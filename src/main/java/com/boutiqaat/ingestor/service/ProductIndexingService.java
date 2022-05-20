package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service purpose is to run real-time ES ingestion for scanned products through CDC real-time API.
 *
 */
@Slf4j(topic = "ES Ingestion")
@Service
@RequiredArgsConstructor
public class ProductIndexingService implements IngestionStreamWriter {
    private final AsyncService asyncService;
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
                List<CatalogProductEntity> fetchedProducts;
                try {
                    fetchedProducts = asyncService.findAllProducts(pageable).get();
                } catch (ExecutionException | InterruptedException ex) {
                    log.error("Async DB call failed!");
                    continue;
                }
                if (fetchedProducts.isEmpty()) {
                    break;
                }
                log.info("Fetched {} records from CatalogProductEntity starting at productId {} ", pageSize, minId);
                String toBeIndexed = fetchedProducts.stream()
                        .map(model -> String.valueOf(model.getProductId()))
                        .collect(Collectors.joining(","));

                if (!toBeIndexed.isEmpty()) {
                    log.debug("Calling GET /realtime on productIds {}", toBeIndexed);
                    ResponseEntity<?> response;
                    try {
                        response = asyncService.esIndex(toBeIndexed).handle((s, t) -> {
                            if (s != null) return s;

                            if (showFailure) {
                                failures.getAndIncrement();
                                writeFailure(output, mapper, log, toBeIndexed, failures);
                            }
                            return ResponseEntity.badRequest().build();
                        }).get();
                    } catch (ExecutionException | InterruptedException ex) {
                        log.error("Async API call failed!");
                        continue;
                    }
                    log.debug("response status: {}", response.getStatusCodeValue());
                    if (response.getStatusCode().equals(HttpStatus.OK) && showSuccess) {
                        successes.getAndIncrement();
                        writeSuccess(output, mapper, log, toBeIndexed, successes);
                    } else if (!response.getStatusCode().equals(HttpStatus.OK) ||
                            !Objects.requireNonNull(response.getBody()).toString().contains("success")) {
                        log.error("Failure in GET /realtime on productIds {}", toBeIndexed);
                        if (showFailure) {
                            failures.getAndIncrement();
                            writeFailure(output, mapper, log, toBeIndexed, failures);
                        }
                    }
                }
                minId = fetchedProducts.get(fetchedProducts.size() -1).getProductId();
            }
            output.println(closing);
        }
    }
}
