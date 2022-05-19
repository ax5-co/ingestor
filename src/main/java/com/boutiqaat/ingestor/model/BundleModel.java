package com.boutiqaat.ingestor.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.io.Serializable;

@Builder
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BundleModel implements Serializable {
    @EqualsAndHashCode.Include
    private SkuModel parent;

    @EqualsAndHashCode.Include
    private SkuModel child;

    private int selectionQty;
}
