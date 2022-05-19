package com.boutiqaat.ingestor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "catalog_product_bundle_selection", schema = "boutiqaat_v2")
public class CatalogProductBundleSelection implements Serializable {
    @EmbeddedId
    private BundleId id;

    @Column(name = "product_id")
    private Long childProductId;

    @Column(name = "selection_qty")
    private double selectionQty;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CatalogProductBundleSelection that = (CatalogProductBundleSelection) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
