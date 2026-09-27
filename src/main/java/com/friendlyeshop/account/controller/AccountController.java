package com.friendlyeshop.account.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @GetMapping
    public Map<String, Object> accounts() {
        return Map.of("service", "account-api", "status", "ready", "accounts", List.of());
    }
}
