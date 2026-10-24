package com.syndeo.backend.controller;

import com.syndeo.backend.dto.CircleCreateRequest;
import com.syndeo.backend.dto.CircleResponse;
import com.syndeo.backend.service.CircleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/circles")
public class CircleController {

    private final CircleService circleService;

    public CircleController(CircleService circleService) {
        this.circleService = circleService;
    }

    @GetMapping
    public List<CircleResponse> listCircles(Authentication authentication) {
        return circleService.listCircles(userId(authentication));
    }

    @GetMapping("/{circleId}")
    public CircleResponse getCircle(
            Authentication authentication,
            @PathVariable long circleId) {
        return circleService.getCircle(userId(authentication), circleId);
    }

    @PostMapping
    public CircleResponse createCircle(
            Authentication authentication,
            @Valid @RequestBody CircleCreateRequest request) {
        return circleService.createCircle(userId(authentication), request);
    }

    @PostMapping("/{circleId}/join")
    public CircleResponse joinCircle(
            Authentication authentication,
            @PathVariable long circleId) {
        return circleService.joinCircle(userId(authentication), circleId);
    }

    @PostMapping("/{circleId}/leave")
    public CircleResponse leaveCircle(
            Authentication authentication,
            @PathVariable long circleId) {
        return circleService.leaveCircle(userId(authentication), circleId);
    }

    private long userId(Authentication authentication) {
        return ((Number) authentication.getPrincipal()).longValue();
    }
}
