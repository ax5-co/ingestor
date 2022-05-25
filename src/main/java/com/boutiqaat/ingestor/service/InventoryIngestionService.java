package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.InventoryModel;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
 * Service purpose is ingest inventories of bundle & configurable skus by triggering CRS's API POST /v2/stock Upsert
 * for bundles & configurables.
 *
 * Notice that no quantities are required at all, since inventory of bundle/config is calculation-based within CRS.
 * The calculation requires that inventory of each child being present in multi_stocks.stock table
 * which is a task of NAV system.
 *
 * The ingestion is applied through CRS's upsert stocks API: POST v2/stock, respecting the validations and accurate
 * processing of the API.
 *
 */
@Deprecated
@Slf4j(topic = "INVENTORY INGESTION")
@Service
@RequiredArgsConstructor
public class InventoryIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final CrsClient crsClient;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    @Override
    public void ingest(OutputStream outputStream, int startPage, int pageSize, boolean showSuccess, boolean showFailure) {
        int lastPage = Integer.MAX_VALUE;
        Pageable pageable;
        int page = Math.max(startPage, 0);
        try (PrintWriter output = new PrintWriter(outputStream, true)) {
            output.println(opening);
            AtomicLong successes = new AtomicLong();
            AtomicLong failures = new AtomicLong();
            while (page <= lastPage) {
                pageable = PageRequest.of(page, pageSize);
                Page<CatalogProductEntity> paged = productRepository.findAll(pageable);
                if (lastPage == Integer.MAX_VALUE) {
                    lastPage = paged.getTotalPages();
                }
                List<CatalogProductEntity> fetchedProducts = paged.getContent();
                log.info("Fetched page {} from CatalogProductEntity of size {} of total pages {}", page, pageSize, lastPage);
                final Map<Long, InventoryModel> toBeUpserted = fetchedProducts.stream()
                        .filter(product -> (product.getType().equalsIgnoreCase("bundle")
                                || product.getType().equalsIgnoreCase("configurable")))
                        .map(product -> new InventoryModel(SkuModel
                                .builder()
                                .sku(product.getSku())
                                .productId(product.getId())
                                .description("")
                                .type(product.getType().toUpperCase())
                                .build()))
                        .collect(Collectors.toMap(InventoryModel::getProductId, model -> model));

                if(!toBeUpserted.isEmpty()) {
                    log.debug("Calling POST /stock on productIds {}", toBeUpserted.keySet());
                    try {
                        crsClient.upsertStocks(new ArrayList<>(toBeUpserted.values()));
                        if (showSuccess) {
                            toBeUpserted.values().forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /stock on models of ids {}, attempting one-by-one calls",
                                toBeUpserted.keySet());
                        toBeUpserted.values().forEach( model -> {
                            try {
                                crsClient.upsertStocks(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /stock on model {}", model, fe);
                                if (showFailure) {
                                    failures.getAndIncrement();
                                    writeFailure(output, mapper, log, model, failures);
                                }
                            }
                        });
                    }
                }
                page++;
            }
            output.println(closing);
        }
    }
}
