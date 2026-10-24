
package com.syndeo.backend.controller;

import com.syndeo.backend.dto.UserProfileResponse;
import com.syndeo.backend.dto.UserProfileUpdateRequest;
import com.syndeo.backend.service.UserProfileService;
import com.syndeo.backend.service.UserProfileUpdateService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileUpdateService userProfileUpdateService;

    public UserProfileController(
            UserProfileService userProfileService,
            UserProfileUpdateService userProfileUpdateService
    ) {
        this.userProfileService = userProfileService;
        this.userProfileUpdateService = userProfileUpdateService;
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(Authentication authentication) {
        long userId = ((Number) authentication.getPrincipal()).longValue();
        return userProfileService.getProfile(userId);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        long userId = ((Number) authentication.getPrincipal()).longValue();

        userProfileUpdateService.updateProfile(userId, request);

        return userProfileService.getProfile(userId);
    }
}
