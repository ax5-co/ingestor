package com.boutiqaat.ingestor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "catalog_product_entity_decimal", schema = "boutiqaat_v2")
public class CatalogProductEntityDecimal {
    @Id
    @Column(name = "value_id")
    private Long id;

    @Column(name = "row_id")
    private Long productId;

    @Column(name = "attribute_id")
    private int attributeId;

    @Column(name = "value")
    private BigDecimal amount;
}
