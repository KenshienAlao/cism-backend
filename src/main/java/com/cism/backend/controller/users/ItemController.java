package com.cism.backend.controller.users;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.users.AllStallDto;

import org.springframework.web.bind.annotation.GetMapping;
import com.cism.backend.service.users.ItemService;

@RestController
@RequestMapping("/api/user/stall")
public class ItemController {
    @Autowired
    private ItemService itemService;

    @GetMapping("/get-all-item")
    public ResponseEntity<Api<List<AllStallDto>>> getAllItem() {
        List<AllStallDto> success = itemService.getAllItem();
        return ResponseEntity.ok(Api.ok("Success get all items", "SUCCESS_GET_ALL_ITEMS", success));
    }
}
