package com.cism.backend.controller.admin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.admin.CreateStallDto;
import com.cism.backend.dto.admin.CreateUserDto;
import com.cism.backend.dto.admin.StallListResponse;
import com.cism.backend.dto.common.Api;
import com.cism.backend.service.admin.CreateStallService;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.cism.backend.model.admin.StallModel;
import com.cism.backend.dto.admin.StallResponseDto;
import com.cism.backend.dto.admin.StallUserResponse;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
public class StallsController {

    @Autowired
    private CreateStallService createStallService;

    @PostMapping("/create-stall")
    public ResponseEntity<Api<StallResponseDto>> createStall(@ModelAttribute CreateUserDto entity) throws IOException {

        CreateStallService.StallCreationResult result = createStallService.createStall();
        StallModel stall = result.stall();
        CreateUserDto details = createStallService.createUserStall(entity, stall);

        CreateStallDto account = new CreateStallDto(result.plainPassword(), stall.getLicence());
        StallResponseDto response = new StallResponseDto(account, details);

        return ResponseEntity.ok(Api.ok("Stall created", "STALL_CREATED", response));
    }

    @PutMapping("/reset-password/{id}")
    public ResponseEntity<Api<CreateStallDto>> resetPassword(@PathVariable Long id) {
        CreateStallService.StallCreationResult result = createStallService.resetPassword(id);
        CreateStallDto response = new CreateStallDto(result.plainPassword(), result.stall().getLicence());
        return ResponseEntity.ok(Api.ok("Password reset successfully", "PASSWORD_RESET", response));
    }

    @GetMapping("/get-stalls")
    public ResponseEntity<Api<List<StallListResponse>>> getStalls() {

        List<StallListResponse> stallList = createStallService.getAllStalls();

        return ResponseEntity.ok(Api.ok("Stalls fetched", "STALLS_FETCHED", stallList));
    }

    @PutMapping("/update-stall/{id}")
    public ResponseEntity<Api<StallUserResponse>> updateStall(@PathVariable Long id,
            @ModelAttribute CreateUserDto entity) throws IOException {
        StallUserResponse success = createStallService.updateUserStall(id, entity);
        return ResponseEntity.ok(Api.ok("Stall updated", "STALL_UPDATED", success));
    }

    @DeleteMapping("/delete-stall/{id}")
    public ResponseEntity<Api<String>> deleteStall(@PathVariable Long id) {
        String succss = createStallService.deleteStall(id);
        return ResponseEntity.ok(Api.ok(succss, "STALL_DELETED", succss));
    }

}
