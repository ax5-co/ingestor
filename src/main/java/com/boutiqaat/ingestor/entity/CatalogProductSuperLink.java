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
@Table(name = "catalog_product_super_link", schema = "boutiqaat_v2")
public class CatalogProductSuperLink {
    @Id
    @Column(name = "link_id")
    private Long id;

    @Column(name = "product_id")
    private Long childId;

    @Column(name = "parent_id")
    private Long parentId;
}
