package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.BasicEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IndexPaginationRepo<U extends BasicEntity> {
    List<U> findAllByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
