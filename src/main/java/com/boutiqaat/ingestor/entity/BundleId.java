package com.boutiqaat.ingestor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class BundleId implements Serializable {
    private static final long serialVersionUID = 4026252333294496217L;

    @Column(name = "selection_id")
    private Long selectionId;

    @Column(name = "parent_product_id")
    private Long parentProductId;
}
