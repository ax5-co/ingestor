package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CdcClient;
import com.boutiqaat.ingestor.repository.ProductRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "Async Service")
@Service
@RequiredArgsConstructor
public class AsyncService {

    private final ProductRepository productRepository;
    private final CdcClient cdcClient;

    @Async
    public CompletableFuture<List<CatalogProductEntity>> findAllProducts(long minProductId, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> productRepository.findAllByProductIdGreaterThanOrderByProductIdAsc(minProductId, pageable));
    }

    @Async
    public CompletableFuture<ResponseEntity<?>> esIndex(String toBeIndexed) {
        try {
            return CompletableFuture.supplyAsync(() -> cdcClient.indexProducts(toBeIndexed));
        } catch (FeignException e) {
            log.error("EXCEPTION in GET /realtime on productIds {}", toBeIndexed, e);
            return null;
        }
    }
}
