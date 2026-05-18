package com.gestionstock.stock.domain.model;

public enum MovementType {
    IN,
    OUT,
    ADJUST;

    public static MovementType from(String value) {
        try {
            return MovementType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid movement type: " + value);
        }
    }
}
