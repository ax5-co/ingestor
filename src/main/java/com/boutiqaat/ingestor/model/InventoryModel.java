package com.boutiqaat.ingestor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InventoryModel implements Serializable {
        @JsonProperty("sku")
        private SkuModel product;
        private long productId;
        private boolean stockOut;
        private boolean forcedStockOut;
        private String forcedStockOutReason;
        private int total;
        private int available;
        private int bundle;
        private int vip;
        private int gwp;
        private WarehouseModel warehouse;

        public InventoryModel(SkuModel product) {
                this.product = product;
                this.productId = product.getProductId();
                this.stockOut = false;
                this.forcedStockOut = false;
                this.forcedStockOutReason = null;
                this.total = 0;
                this.available = 0;
                this.bundle = 0;
                this.vip = 0;
                this.gwp = 0;
                this.warehouse = WarehouseModel.createDefault();
        }

        public InventoryModel(SkuModel product, int quantity, WarehouseModel warehouse) {
                this.product = product;
                this.productId = product.getProductId();
                this.stockOut = false;
                this.forcedStockOut = false;
                this.forcedStockOutReason = null;
                this.total = quantity;
                this.available = quantity;
                this.bundle = 0;
                this.vip = 0;
                this.gwp = 0;
                this.warehouse = warehouse;
        }
}
