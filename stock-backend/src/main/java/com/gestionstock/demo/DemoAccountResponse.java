package com.gestionstock.demo;

public record DemoAccountResponse(
        String organisation,
        String scenario,
        String adminEmail,
        String userEmail,
        String password
) {
}
