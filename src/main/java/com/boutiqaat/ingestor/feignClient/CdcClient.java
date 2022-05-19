package com.boutiqaat.ingestor.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CDCService", url = "${cdc.base.url}")
public interface CdcClient {

    @GetMapping("/rest/V1/realtime/{productList}")
    ResponseEntity<String> indexProducts(@PathVariable("productList") String productString);
}
