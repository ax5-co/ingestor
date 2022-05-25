package com.boutiqaat.ingestor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "catalog_product_entity", schema = "boutiqaat_v2")
public class CatalogProductEntity extends BasicEntity{
    @Id
    @Column(name = "row_id")
    private Long id;

    @Column(name = "type_id")
    private String type;

    @Column(name = "sku")
    private String sku;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CatalogProductEntity that = (CatalogProductEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
