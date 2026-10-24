package com.syndeo.backend.controller;

import com.syndeo.backend.dto.TaskCreateRequest;
import com.syndeo.backend.dto.TaskResponse;
import com.syndeo.backend.dto.TaskUpdateRequest;
import com.syndeo.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> listTasks(
            Authentication authentication,
            @PathVariable long projectId) {
        return taskService.listTasks(userId(authentication), projectId);
    }

    @PostMapping
    public TaskResponse createTask(
            Authentication authentication,
            @PathVariable long projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(userId(authentication), projectId, request);
    }

    @PatchMapping("/{taskId}")
    public TaskResponse updateTask(
            Authentication authentication,
            @PathVariable long projectId,
            @PathVariable long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(
                userId(authentication), projectId, taskId, request);
    }

    private long userId(Authentication authentication) {
        return ((Number) authentication.getPrincipal()).longValue();
    }
}
