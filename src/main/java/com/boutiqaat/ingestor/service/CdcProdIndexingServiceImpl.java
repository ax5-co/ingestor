package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.feignClient.CdcClient;
import com.boutiqaat.ingestor.repository.ProductRepository;

public class CdcProdIndexingServiceImpl extends CdcIndexingService <ProductRepository, CatalogProductEntity> {

    public CdcProdIndexingServiceImpl(CdcClient cdcClient, ProductRepository repository) {
        super(cdcClient, repository);
        this.LOG_TAG = "Products";
    }
}
