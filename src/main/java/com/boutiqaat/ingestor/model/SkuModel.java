package com.boutiqaat.ingestor.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SkuModel implements Serializable {
    private String sku;
    private Long productId;
    private String description;
    private String type;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkuModel skuModel = (SkuModel) o;
        return sku.equalsIgnoreCase(skuModel.sku) && productId.equals(skuModel.productId) && type.equalsIgnoreCase(skuModel.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, productId, type);
    }
}
