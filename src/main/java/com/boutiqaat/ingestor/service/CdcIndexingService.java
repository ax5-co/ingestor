package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.Constants;
import com.boutiqaat.ingestor.entity.BasicEntity;
import com.boutiqaat.ingestor.repository.IndexPaginationRepo;
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
public abstract class CdcIndexingService <R extends IndexPaginationRepo<E>, E extends BasicEntity> {
    private final AsyncCallService apiService;
    private final R repository;
    protected String LOG_TAG = "";


    @Transactional(readOnly = true)
    public String index(int startProductId, int pageSize, String type) {
        long minId = Math.max(startProductId, 0);
        Pageable pageable = PageRequest.of(0, pageSize);
        List<CompletableFuture<String>> cdcCalls = new ArrayList<>();

        while (true) {
            List<String> fetchedIds =
                    repository.findAllByIdGreaterThanOrderByIdAsc(minId, pageable)
                            .stream()
                            .map(E::getId)
                            .map(String::valueOf)
                            .collect(Collectors.toList());
            if (fetchedIds.isEmpty()) {
                break;
            }
            log.info("{} Fetched {} records starting at Id {} till Id {}",
                    LOG_TAG, pageSize, fetchedIds.get(0), fetchedIds.get(fetchedIds.size()-1));
            cdcCalls.add(apiService.callCdcApi(type, fetchedIds)
                    .exceptionally(e -> {
                        log.error("Failure in API call for {}, with Exception {}", LOG_TAG, e.getMessage());
                        return Constants.FAILURE;
                    }));
            minId = Long.parseLong(fetchedIds.get(fetchedIds.size() -1));
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
