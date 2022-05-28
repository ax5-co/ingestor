package com.boutiqaat.ingestor.controller;

import com.boutiqaat.ingestor.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;

/**
 * APIs for development usage only.
 * Purpose is to ingest data from magento DB into multi_stocks DB under multi_stocks project,
 * REF: https://alshayji.atlassian.net/browse/BOUT-812 & https://alshayji.atlassian.net/browse/BOUT-870.
 *
 * Return type is a stream of failures, where the stream elements are the JSON serialization of models that failed to
 * be upserted through corresponding CRS APIs.
 *
 * Timeout of the APIs is significant due to the fact that they scan, in pages, all products in boutiqaat_v2
 * .catlog_product_entity table and, in most cases, scan another table. AsyncConfig defines the timeout duration.
 *
 * Producing output of streams on the server side, i.e., these APIs, occurs asynchronously such that once a failure
 * is caught, it's sent to user.
 *
 * Since output represents JSON models list, the closing square bracket ']' of the list defines the end of output of
 * each API.
 *
 * Order of usage for dependable results:
 * 1. /product ; to upsert product_id-sku-type mappings.
 * 2. /bundle and /config ; to upsert parent-child mappings for each.
 * 3. /price and /inventory ; to upsert prices & inventory info for all products of all types.
 */
@Slf4j
@RestController
@RequestMapping("/crs")
@RequiredArgsConstructor
public class CRSIngestionController {

    private final ProductIngestionService productIngestionService;
    private final BundleIngestionService bundleIngestionService;
    private final ConfigIngestionService configIngestionService;
    private final PricesIngestionService pricesIngestionService;
    private final InventoryIngestionService inventoryIngestionService;
    private final DummyInvIngestionService dummyInventoryService;
    private final DummyPriceIngestionService dummyPricesService;
    private int startPage, pageSize;
    private boolean showSuccess, showFailure;
    private IngestionStreamWriter ingestionStreamWriterService;


    @PostMapping(produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> upsertProducts(@RequestParam (defaultValue = "0") int startPage,
                                                                @RequestParam (defaultValue = "200") int pageSize,
                                                                @RequestParam (defaultValue = "false") boolean showSuccess,
                                                                @RequestParam (defaultValue = "true") boolean showFailure) {

        prepareArgs(startPage, pageSize, showSuccess, showFailure, productIngestionService);
        return ResponseEntity.ok(this::writeTo);
    }

    @PostMapping(value = "/bundle", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> upsertBundles(@RequestParam (defaultValue = "0") int startPage,
                                                               @RequestParam (defaultValue = "200") int pageSize,
                                                               @RequestParam (defaultValue = "false") boolean showSuccess,
                                                               @RequestParam (defaultValue = "true") boolean showFailure) {

        prepareArgs(startPage, pageSize, showSuccess, showFailure, bundleIngestionService);
        return ResponseEntity.ok(this::writeTo);
    }

    @PostMapping(value = "/config",  produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> upsertConfigs(@RequestParam (defaultValue = "0") int startPage,
                                                               @RequestParam (defaultValue = "200") int pageSize,
                                                               @RequestParam (defaultValue = "false") boolean showSuccess,
                                                               @RequestParam (defaultValue = "true") boolean showFailure) {

        prepareArgs(startPage, pageSize, showSuccess, showFailure, configIngestionService);
        return ResponseEntity.ok(this::writeTo);
    }

    @Deprecated
    @PostMapping(value = "/price", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> upsertPrices(@RequestParam (defaultValue = "0") int startPage,
                                                              @RequestParam (defaultValue = "200") int pageSize,
                                                              @RequestParam (defaultValue = "false") boolean showSuccess,
                                                              @RequestParam (defaultValue = "true") boolean showFailure,
                                                              @RequestParam (defaultValue = "false") boolean dummy) {

        if (!dummy) {
            prepareArgs(startPage, pageSize, showSuccess, showFailure, pricesIngestionService);
        } else {
            prepareArgs(startPage, pageSize, showSuccess, showFailure, dummyPricesService);
        }
        return ResponseEntity.ok(this::writeTo);
    }

    @Deprecated
    @PostMapping(value = "/inventory", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> upsertStocks(@RequestParam (defaultValue = "0") int startPage,
                                                              @RequestParam (defaultValue = "200") int pageSize,
                                                              @RequestParam (defaultValue = "false") boolean showSuccess,
                                                              @RequestParam (defaultValue = "true") boolean showFailure,
                                                              @RequestParam (defaultValue = "false") boolean dummy) {

        if (!dummy) {
            prepareArgs(startPage, pageSize, showSuccess, showFailure, inventoryIngestionService);
        } else {
            prepareArgs(startPage, pageSize, showSuccess, showFailure, dummyInventoryService);
        }
        return ResponseEntity.ok(this::writeTo);
    }

    private void prepareArgs(int startPage, int pageSize, boolean showSuccess, boolean showFailure,
                             IngestionStreamWriter service) {
        this.startPage = startPage;
        this.pageSize = pageSize;
        this.showSuccess = showSuccess;
        this.showFailure = showFailure;
        this.ingestionStreamWriterService = service;
    }

    private void writeTo(OutputStream outputStream) {
        ingestionStreamWriterService.ingest(outputStream, startPage, pageSize, showSuccess, showFailure);
    }
}
