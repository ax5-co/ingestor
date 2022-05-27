package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import com.boutiqaat.ingestor.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class CdcProdIndexingServiceImpl extends CdcIndexingService <ProductRepository, CatalogProductEntity> {

    public CdcProdIndexingServiceImpl(AsyncCallService service, ProductRepository repository) {
        super(service, repository);
        this.LOG_TAG = "Products";
    }
}
