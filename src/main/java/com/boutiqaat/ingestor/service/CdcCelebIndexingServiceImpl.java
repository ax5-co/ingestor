package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CelebrityMaster;
import com.boutiqaat.ingestor.feignClient.CdcClient;
import com.boutiqaat.ingestor.repository.CelebrityRepository;

public class CdcCelebIndexingServiceImpl extends CdcIndexingService <CelebrityRepository, CelebrityMaster> {

    public CdcCelebIndexingServiceImpl(CdcClient cdcClient, CelebrityRepository repository) {
        super(cdcClient, repository);
        this.LOG_TAG = "Celebrities";
    }
}
