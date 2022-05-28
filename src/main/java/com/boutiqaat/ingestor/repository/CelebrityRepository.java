package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.CelebrityMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CelebrityRepository extends JpaRepository<CelebrityMaster, Long>,
        IndexPaginationRepo<CelebrityMaster> {

    List<CelebrityMaster> findAllByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

}
