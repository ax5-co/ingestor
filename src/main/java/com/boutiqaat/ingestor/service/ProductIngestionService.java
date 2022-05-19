package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service purpose is to ingest products info [product_id-sku-type] from magento DB into multi_stocks.sku table.
 *
 * The ingestion is applied through CRS's upsert skus API: POST /sku, respecting the validations and accurate
 * processing of the API.
 */
@Slf4j(topic = "SKU INGESTION")
@Service
@RequiredArgsConstructor
public class ProductIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final CrsClient crsClient;
    private final ObjectMapper mapper;


    @Transactional(readOnly = true)
    @Override
    public void ingest(OutputStream outputStream, int minProductId, int pageSize, boolean showSuccess, boolean showFailure) {
        long minId = Math.max(minProductId, 0);
        Pageable pageable = PageRequest.of(0, pageSize);
        try (PrintWriter output = new PrintWriter(outputStream, true)) {
            output.println(opening);
            AtomicLong successes = new AtomicLong();
            AtomicLong failures = new AtomicLong();
            while (true) {
                List<CatalogProductEntity> fetchedProducts =
                        productRepository.findAllByProductIdGreaterThanOrderByProductIdAsc(minId, pageable);
                if (fetchedProducts.isEmpty()) {
                    break;
                }
                log.info("Fetched {} records from CatalogProductEntity starting at productId {} ", pageSize, minId);
                final Map<Long, SkuModel> toBeUpserted = fetchedProducts.stream()
                        .map(product -> SkuModel
                                .builder()
                                .sku(product.getSku())
                                .productId(product.getProductId())
                                .description("")
                                .type(product.getType().toUpperCase())
                                .build())
                        .collect(Collectors.toMap(SkuModel::getProductId, skuModel -> skuModel));

                if (!toBeUpserted.values().isEmpty()) {
                    try {
                        crsClient.upsertProducts(new ArrayList<>(toBeUpserted.values()));
                        log.info("Called POST /sku on {} productIds", toBeUpserted.keySet().size());
                        if (showSuccess) {
                            toBeUpserted.values().forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /sku on models of ids {}, attempting one-by-one calls",
                                toBeUpserted.keySet());
                        toBeUpserted.values().forEach(model -> {
                            try {
                                crsClient.upsertProducts(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /sku on model {}", model.toString(), fe);
                                if (showFailure) {
                                    failures.getAndIncrement();
                                    writeFailure(output, mapper, log, model, failures);
                                }
                            }
                        });
                    }
                }
                minId = fetchedProducts.get(fetchedProducts.size() - 1).getProductId();
            }
            output.println(closing);
        }
    }
}
