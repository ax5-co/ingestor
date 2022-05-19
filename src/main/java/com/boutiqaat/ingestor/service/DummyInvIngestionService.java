package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.CountryModel;
import com.boutiqaat.ingestor.model.InventoryModel;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.model.WarehouseModel;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service purpose is to ingest dummy KSA inventory & prices into multi_stocks.
 *
 * The ingestion is applied through CRS's upsert prices API: POST /price-info & upsert inventory API: POST /v2/stock,
 * respecting the validations and accurate processing of the API.
 */
@Deprecated
@Slf4j(topic = "DUMMY INVENTORY INGESTION")
@Service
@RequiredArgsConstructor
public class DummyInvIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final CrsClient crsClient;
    private final ObjectMapper mapper;
    private static final int QUANTITY_FROM = 10;
    private static final int QUANTITY_TO = 100000;

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
                List<CatalogProductEntity> fetchedProducts = paged.getContent();
                if (lastPage == Integer.MAX_VALUE) {
                    lastPage = paged.getTotalPages();
                }
                log.info("Fetched page {} from CatalogProductEntity of size {} of total pages {}", page, pageSize, lastPage);

                int quantity = (int) Math.floor(Math.random()*(QUANTITY_TO-QUANTITY_FROM+1)+QUANTITY_FROM);
                WarehouseModel ksa = WarehouseModel.builder()
                        .warehouseId(2)
                        .country(CountryModel.builder()
                                .countryCode("SA")
                                .build())
                        .build();
                List<InventoryModel> dummyModels = fetchedProducts.stream()
                        .map(product -> SkuModel
                                .builder()
                                .sku(product.getSku())
                                .productId(product.getProductId())
                                .description("")
                                .type(product.getType().toUpperCase())
                                .build())
                        .map(model -> new InventoryModel(model, quantity, ksa))
                        .collect(Collectors.toList());

                if (!dummyModels.isEmpty()) {
                    log.info("Calling POST /inventory on productIds {}", dummyModels.stream()
                            .map(InventoryModel::getProductId)
                            .collect(Collectors.toList()));
                    try {
                        crsClient.upsertStocks(dummyModels);
                        if (showSuccess) {
                            dummyModels.forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /inventory on models of ids {}, attempting one-by-one calls",
                                dummyModels.stream().map(InventoryModel::getProductId).collect(Collectors.toList()));
                        dummyModels.forEach(model -> {
                            try {
                                crsClient.upsertStocks(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /inventory on model {}", model.toString(), fe);
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
