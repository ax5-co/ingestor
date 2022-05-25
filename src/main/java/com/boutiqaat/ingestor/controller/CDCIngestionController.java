package com.boutiqaat.ingestor.controller;

import com.boutiqaat.ingestor.Constants;
import com.boutiqaat.ingestor.service.CdcCelebIndexingServiceImpl;
import com.boutiqaat.ingestor.service.CdcProdIndexingServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/cdc")
@RequiredArgsConstructor
public class CDCIngestionController {
    private final CdcProdIndexingServiceImpl productsService;
    private final CdcCelebIndexingServiceImpl celebrityService;

    @PostMapping(value = "/products", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Void> indexProducts(@RequestParam(defaultValue = "0") int startId,
                                              @RequestParam (defaultValue = "500") int pageSize) {

        return productsService.index(startId, pageSize, Constants.CDC_PRODUCTS).contains(Constants.FAILURE)
                ? ResponseEntity.internalServerError().build()
                : ResponseEntity.ok().build();
    }

    @PostMapping(value = "/celebrities", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Void> indexCelebrities(@RequestParam(defaultValue = "0") int startId,
                                                 @RequestParam (defaultValue = "500") int pageSize) {
        return celebrityService.index(startId, pageSize, Constants.CDC_CELEBRITIES).contains(Constants.FAILURE)
                ? ResponseEntity.internalServerError().build()
                : ResponseEntity.ok().build();
    }
}
