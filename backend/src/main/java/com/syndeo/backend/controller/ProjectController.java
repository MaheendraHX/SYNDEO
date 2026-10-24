package com.syndeo.backend.controller;

import com.syndeo.backend.dto.ProjectCreateRequest;
import com.syndeo.backend.dto.ProjectResponse;
import com.syndeo.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects(Authentication authentication) {
        return projectService.listProjects(userId(authentication));
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(
            Authentication authentication,
            @PathVariable long projectId) {
        return projectService.getProject(userId(authentication), projectId);
    }

    @PostMapping
    public ProjectResponse createProject(
            Authentication authentication,
            @Valid @RequestBody ProjectCreateRequest request) {
        return projectService.createProject(userId(authentication), request);
    }

    @PostMapping("/{projectId}/join")
    public ProjectResponse joinProject(
            Authentication authentication,
            @PathVariable long projectId) {
        return projectService.joinProject(userId(authentication), projectId);
    }

    @DeleteMapping("/{projectId}/membership")
    public ProjectResponse leaveProject(
            Authentication authentication,
            @PathVariable long projectId) {
        return projectService.leaveProject(userId(authentication), projectId);
    }

    private long userId(Authentication authentication) {
        return ((Number) authentication.getPrincipal()).longValue();
    }
}
