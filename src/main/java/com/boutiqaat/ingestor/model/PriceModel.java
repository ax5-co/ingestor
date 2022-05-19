package com.boutiqaat.ingestor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PriceModel implements Serializable {
    @JsonProperty("sku")
    @EqualsAndHashCode.Include
    private SkuModel product;

    @EqualsAndHashCode.Include
    private CountryModel country;

    private Long productId;

    private BigDecimal price;

    private BigDecimal specialPrice;

    private BigDecimal discount;

    private BigDecimal taxes = BigDecimal.ZERO;
}
