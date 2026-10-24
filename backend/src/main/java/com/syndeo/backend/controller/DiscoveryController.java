package com.syndeo.backend.controller;

import com.syndeo.backend.dto.DiscoveryProfileResponse;
import com.syndeo.backend.service.DiscoveryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public List<DiscoveryProfileResponse> searchProfiles(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String interest) {
        long userId = ((Number) authentication.getPrincipal()).longValue();
        return discoveryService.searchProfiles(userId, search, skill, interest);
    }
}
