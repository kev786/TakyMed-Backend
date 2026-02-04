package com.takymed.auth_service.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/test")
public class TestController {

    @GetMapping("/all")
    public String publicAccess() {
        return "Contenu Public - Tout le monde peut voir ceci.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Accès ADMIN - Bienvenue Admin !";
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public String doctorAccess() {
        return "Accès DOCTEUR - Bienvenue Docteur !";
    }

}
