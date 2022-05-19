package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.entity.CatalogProductSuperLink;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.ConfigModel;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.repository.ConfigRepository;
import com.boutiqaat.ingestor.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service purpose is to ingest configurables' [parent-child] mappings from magento DB into multi_stocks.configurable
 * table.
 *
 * The ingestion is applied through CRS's upsert configs API: POST /configurable, respecting the validations and
 * accurate processing of the API.
 */
@Slf4j(topic = "CONFIG INGESTION")
@Service
@RequiredArgsConstructor
public class ConfigIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final ConfigRepository configRepository;
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
                Page<CatalogProductSuperLink> paged = configRepository.findAll(pageable);
                List<CatalogProductSuperLink> fetchedRelations = paged.getContent();
                if (lastPage == Integer.MAX_VALUE) {
                    lastPage = paged.getTotalPages();
                }
                log.info("Fetched page {} from CatalogProductSuperLink of size {} of total pages {}", page, pageSize, lastPage);
                Set<Long> ids = new HashSet<>();
                fetchedRelations.forEach(config -> {
                    ids.add(config.getChildId());
                    ids.add(config.getParentId());
                });

                //CRS SkuController does not provide a filter API on product_id, & we need sku codes
                Map<Long, SkuModel> products = productRepository.findAllById(ids)
                        .stream()
                        .collect(Collectors.toMap(CatalogProductEntity::getProductId, entity -> SkuModel
                                .builder()
                                .sku(entity.getSku())
                                .productId(entity.getProductId())
                                .type(entity.getType().toUpperCase())
                                .description("")
                                .build()));

                log.debug("Assembled SkuModels of product_ids {} ", products.keySet());
                List<ConfigModel> toBeUpserted = fetchedRelations.stream().map(config -> ConfigModel
                                .builder()
                                .parent(products.get(config.getParentId()))
                                .child(products.get(config.getChildId()))
                                .build())
                        .distinct()
                        .filter(config -> config.getParent() != null && config.getChild() != null)
                        .collect(Collectors.toList());

                if (!toBeUpserted.isEmpty()) {
                    log.info("Calling POST /configurable on parent->child pairs {}",
                            toBeUpserted.stream().map(model -> Pair.of(model.getParent().getProductId(),
                                            model.getChild().getProductId()))
                                    .collect(Collectors.toList()));
                    try {
                        crsClient.upsertConfigurables(toBeUpserted);
                        if (showSuccess) {
                            toBeUpserted.forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /configurable on models of ids {}, attempting one-by-one calls",
                                toBeUpserted
                                        .stream()
                                        .map(config -> Pair.of(config.getParent().getProductId(),
                                                config.getChild().getProductId()))
                                        .collect(Collectors.toList()));
                        toBeUpserted.forEach(model -> {
                            try {
                                crsClient.upsertConfigurables(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /configurable on model {}", model, fe);
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
