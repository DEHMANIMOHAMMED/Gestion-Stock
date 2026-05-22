package com.gestionstock.demo;

import com.gestionstock.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final PermissionService permissionService;

    @GetMapping("/accounts")
    public ResponseEntity<List<DemoAccountResponse>> accounts() {
        permissionService.requireOwner();
        return ResponseEntity.ok(List.of(
                new DemoAccountResponse("Demo Stock", "Generaliste TPE avec risques mixtes", "admin@demo-stock.local", "user@demo-stock.local", "Password123!"),
                new DemoAccountResponse("Garage Atlas", "Garage avec pieces critiques et delais fournisseurs", "admin@garage-atlas.local", "mecano@garage-atlas.local", "Password123!"),
                new DemoAccountResponse("Pharma Nova", "Pharmacie avec ruptures sensibles", "admin@pharma-nova.local", "preparateur@pharma-nova.local", "Password123!"),
                new DemoAccountResponse("Boutique Lumiere", "Retail saisonnier et forte rotation", "admin@boutique-lumiere.local", "vendeur@boutique-lumiere.local", "Password123!"),
                new DemoAccountResponse("Atelier Meca", "Petit fabricant avec composants et lots", "admin@atelier-meca.local", "atelier@atelier-meca.local", "Password123!")
        ));
    }
}
