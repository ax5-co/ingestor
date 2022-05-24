package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.Constants;
import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CdcClient;
import com.boutiqaat.ingestor.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service purpose is to run real-time ES ingestion for scanned products through CDC real-time API.
 *
 */
@Slf4j(topic = "ES Ingestion")
@Service
@RequiredArgsConstructor
public class CdcIndexingService {
    private final ProductRepository productRepository;
    private final CdcClient cdcClient;

    @Transactional(readOnly = true)
    public String indexProducts(int minProductId, int pageSize) {
        long minId = Math.max(minProductId, 0);
        Pageable pageable = PageRequest.of(0, pageSize);
        List<CompletableFuture<String>> cdcCalls = new ArrayList<>();

        while (true) {
            List<String> fetchedProductIds =
                    productRepository.findAllByProductIdGreaterThanOrderByProductIdAsc(minId, pageable)
                            .stream()
                            .map(CatalogProductEntity::getProductId)
                            .map(String::valueOf)
                            .collect(Collectors.toList());
            if (fetchedProductIds.isEmpty()) {
                break;
            }
            log.info("Fetched {} records from CatalogProductEntity starting at productId {} till productId {}",
                    pageSize, fetchedProductIds.get(0), fetchedProductIds.get(fetchedProductIds.size()-1));
            cdcCalls.add(CompletableFuture
                    .supplyAsync(() -> cdcClient.indexProducts(String.join(",", fetchedProductIds)))
                    .exceptionally(e -> {
                        log.error("Failure in GET /realtime, with Exception {}", e.getMessage());
                        return Constants.FAILURE;
                    }));
            minId = Long.parseLong(fetchedProductIds.get(fetchedProductIds.size() -1));
        }
        CompletableFuture<Void> allResult =
                CompletableFuture.allOf(cdcCalls.toArray(new CompletableFuture[0]));
        try {
            return allResult.thenApply(v ->
                    cdcCalls.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.joining("::"))
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failure in joining CompletableFutures with Exception ", e);
            e.printStackTrace();
        }
        return Constants.FAILURE;
    }
}
