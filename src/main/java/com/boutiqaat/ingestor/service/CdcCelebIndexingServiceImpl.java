package com.boutiqaat.ingestor.service;

import com.boutiqaat.ingestor.entity.CelebrityMaster;
import com.boutiqaat.ingestor.repository.CelebrityRepository;
import org.springframework.stereotype.Service;

@Service
public class CdcCelebIndexingServiceImpl extends CdcIndexingService <CelebrityRepository, CelebrityMaster> {

    public CdcCelebIndexingServiceImpl(AsyncCallService service, CelebrityRepository repository) {
        super(service, repository);
        this.LOG_TAG = "Celebrities";
    }
}
