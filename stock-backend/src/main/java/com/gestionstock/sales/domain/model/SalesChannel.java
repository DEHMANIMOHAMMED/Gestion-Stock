package com.gestionstock.sales.domain.model;

public enum SalesChannel {
    STORE,
    WEB,
    PHONE,
    B2B,
    OTHER;

    public static SalesChannel from(String value) {
        try {
            return SalesChannel.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid sales channel");
        }
    }
}
