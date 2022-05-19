package com.boutiqaat.ingestor.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class WarehouseModel implements Serializable {
    private int warehouseId;
    private CountryModel country;

    public static WarehouseModel createDefault(){
        return WarehouseModel.builder().warehouseId(1).country(CountryModel.createDefault()).build();
    }
}
