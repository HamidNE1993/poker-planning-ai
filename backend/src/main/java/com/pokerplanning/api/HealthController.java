package com.pokerplanning.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Système & Santé", description = "Vérification de l'état opérationnel du serveur")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Vérifier la disponibilité de l'API", description = "Retourne le statut UP et le nom de l'application.")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "Poker Planning AI");
    }
}
