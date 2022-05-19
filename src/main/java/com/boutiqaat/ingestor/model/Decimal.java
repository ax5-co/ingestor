package com.boutiqaat.ingestor.model;

import java.util.HashSet;
import java.util.Set;

public enum Decimal {
    PRICE(75), SPECIAL_PRICE(76);

    private final int attributeId;

    Decimal(int attributeId) {
        this.attributeId = attributeId;
    }

    public static Decimal getByAttributeId(int attributeId) {
        for (Decimal decimal: Decimal.values()){
            if (decimal.attributeId == attributeId) {
                return decimal;
            }
        }
        return null;
    }

    public static Set<Integer> supportedAttributes() {
        Set<Integer> attributes = new HashSet<>();
        for (Decimal decimal: Decimal.values()) {
            attributes.add(decimal.attributeId);
        }
        return attributes;
    }
}
