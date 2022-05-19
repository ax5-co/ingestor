package com.boutiqaat.ingestor.feignClient;

import com.boutiqaat.ingestor.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "CrsService", url = "${crs.base.url}")
public interface CrsClient {

    @PostMapping("/atom/v1/sku")
    void upsertProducts(@RequestBody List<SkuModel> models);

    @PostMapping("/atom/v1/bundle")
    void upsertBundles(@RequestBody List<BundleModel> models);

    @PostMapping("/atom/v1/configurable")
    void upsertConfigurables(@RequestBody List<ConfigModel> models);

    @GetMapping("/atom/v1/bundle")
    void fetchBundles(@RequestBody List<BundleModel> models);

    @GetMapping("/atom/v1/configurable")
    void fetchConfigurables(@RequestBody List<ConfigModel> models);

    @PostMapping("/atom/v1/price-info")
    void upsertPrices(@RequestBody List<PriceModel> models);

    @PostMapping("/atom/v2/stock")
    void upsertStocks(@RequestBody List<InventoryModel> models);
}
