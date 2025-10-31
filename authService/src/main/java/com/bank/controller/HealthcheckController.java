package com.bank.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class HealthcheckController {
    @GetMapping("/healthcheck")
    public String healthcheck() {
        return "OK";
    }
}
