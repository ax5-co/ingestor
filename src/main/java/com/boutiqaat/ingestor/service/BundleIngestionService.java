package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductBundleSelection;
import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CrsClient;
import com.boutiqaat.ingestor.model.BundleModel;
import com.boutiqaat.ingestor.model.SkuModel;
import com.boutiqaat.ingestor.repository.BundleRepository;
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
 * Service purpose is to ingest bundles' [parent-child-selectionQty] mappings from magento DB into multi_stocks.bundle
 * table.
 *
 * The ingestion is applied through CRS's upsert bundles API: POST /bundle, respecting the validations and accurate
 * processing of the API.
 */
@Slf4j(topic = "BUNDLE INGESTION")
@Service
@RequiredArgsConstructor
public class BundleIngestionService implements IngestionStreamWriter {
    private final ProductRepository productRepository;
    private final BundleRepository bundleRepository;
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
                Page<CatalogProductBundleSelection> paged = bundleRepository.findAll(pageable);
                List<CatalogProductBundleSelection> fetchedRelations = paged.getContent();
                if (lastPage == Integer.MAX_VALUE) {
                    lastPage = paged.getTotalPages();
                }
                log.info("Fetched page {} from CatalogProductBundleSelection of size {} of total pages {}", page, pageSize, lastPage);
                Set<Long> ids = new HashSet<>();
                fetchedRelations.forEach(bundle -> {
                    ids.add(bundle.getChildProductId());
                    ids.add(bundle.getId().getParentProductId());
                });

                //CRS SkuController does not provide a filter API on product_id, & we need sku codes
                Map<Long, SkuModel> products = productRepository.findAllById(ids)
                        .stream()
                        .collect(Collectors.toMap(CatalogProductEntity::getId, entity -> SkuModel
                                .builder()
                                .sku(entity.getSku())
                                .productId(entity.getId())
                                .type(entity.getType().toUpperCase())
                                .description("")
                                .build()));

                log.info("Assembled SkuModels of product_ids {} ", products.keySet());
                List<BundleModel> toBeUpserted = fetchedRelations.stream().map(bundle -> BundleModel
                                .builder()
                                .parent(products.get(bundle.getId().getParentProductId()))
                                .child(products.get(bundle.getChildProductId()))
                                .selectionQty((int) bundle.getSelectionQty())
                                .build())
                        .distinct()
                        .filter(bundle -> bundle.getParent() != null && bundle.getChild() != null)
                        .collect(Collectors.toList());

                if (!toBeUpserted.isEmpty()) {
                    log.info("Calling POST /bundle on parent->child pairs {}",
                            toBeUpserted.stream().map(model -> Pair.of(model.getParent().getProductId(),
                                            model.getChild().getProductId()))
                                    .collect(Collectors.toList()));
                    try {
                        crsClient.upsertBundles(toBeUpserted);
                        if (showSuccess) {
                            toBeUpserted.forEach(model -> {
                                successes.getAndIncrement();
                                writeSuccess(output, mapper, log, model, successes);
                            });
                        }
                    } catch (FeignException e) {
                        log.error("EXCEPTION in POST /bundle on models of ids {}, attempting one-by-one calls",
                                toBeUpserted
                                        .stream()
                                        .map(bundle -> Pair.of(bundle.getParent().getProductId(),
                                                bundle.getChild().getProductId()))
                                        .collect(Collectors.toList()));
                        toBeUpserted.forEach(model -> {
                            try {
                                crsClient.upsertBundles(Collections.singletonList(model));
                                if (showSuccess) {
                                    successes.getAndIncrement();
                                    writeSuccess(output, mapper, log, model, successes);
                                }
                            } catch (FeignException fe) {
                                log.error("Failure Feign Request Body {}",
                                        new String(fe.request().body(), StandardCharsets.UTF_8));
                                log.error("EXCEPTION in POST /bundle on model {}", model, fe);
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
