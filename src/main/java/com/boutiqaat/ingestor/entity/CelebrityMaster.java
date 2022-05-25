package com.boutiqaat.ingestor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "celebrity_master", schema = "boutiqaat_v2")
public class CelebrityMaster extends BasicEntity{
    @Id
    @Column(name = "celebrity_id")
    private long id;
}
