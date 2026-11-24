package com.vaibhavs.hospital_agent.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/check")
    public String homeRoute() {
        return "Welcome this is a spring boot server.";
    }
}
