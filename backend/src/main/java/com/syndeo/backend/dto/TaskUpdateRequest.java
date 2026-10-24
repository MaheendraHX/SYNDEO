package com.syndeo.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class TaskUpdateRequest {

    @Size(max = 24, message = "Task status must not exceed 24 characters.")
    private String status;

    @Positive(message = "Assignee ID must be a positive number.")
    private Long assigneeUserId;

    private boolean assigneeSpecified;

    public TaskUpdateRequest() {
    }

    public TaskUpdateRequest(String status, Long assigneeUserId) {
        this.status = status;
        this.assigneeUserId = assigneeUserId;
        this.assigneeSpecified = assigneeUserId != null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    @JsonSetter("assigneeUserId")
    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeSpecified = true;
        this.assigneeUserId = assigneeUserId;
    }

    @JsonIgnore
    public boolean isAssigneeSpecified() {
        return assigneeSpecified;
    }
}
