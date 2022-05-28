package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.Constants;
import com.boutiqaat.ingestor.feignClient.CdcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "API Calling Service")
@Service
@RequiredArgsConstructor
public class AsyncCallService {

    private final CdcClient cdcClient;

    @Async
    public CompletableFuture<String> callCdcApi(String type, List<String> fetchedIds) {
        log.info("Initiating CDC API call for {}", type);
        return CompletableFuture.supplyAsync(() -> {
            if (type.equalsIgnoreCase(Constants.CDC_PRODUCTS)) {
                return cdcClient.indexProducts(String.join(",", fetchedIds));
            } else if (type.equalsIgnoreCase(Constants.CDC_CELEBRITIES)) {
                return cdcClient.indexCelebrities(String.join(",", fetchedIds));
            }
            return null;
        }).thenApply(s -> {
            if (s.equalsIgnoreCase(Constants.SUCCESS)) {
                log.info("{} CDC API call for {}", s, type);
                return s;
            }
            log.error("{} CDC API call for {} from id {} till id {}",
                    Constants.FAILURE, type, fetchedIds.get(0), fetchedIds.get(fetchedIds.size()-1));
            return Constants.FAILURE;
        });
    }
}
