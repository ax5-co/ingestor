package com.boutiqaat.ingestor.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "CDCService", url = "${cdc.base.url}")
public interface CdcClient {

    @Async
    @GetMapping("/rest/V1/realtime/{productList}")
    String indexProducts(@PathVariable("productList") String productString);

    @Async
    @GetMapping(value = "/rest/V1/realtime/celebrity/{celebritylist}")
    String indexCelebrities(@PathVariable("celebritylist") String celebrityList);

    //TODO -- add brand enriching, category enriching, for suggestions index ??
}
