package com.cism.backend.controller.stall;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.stall.RequestItemDto;
import com.cism.backend.dto.stall.ResponseItemDto;
import com.cism.backend.service.stalls.ItemStallService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/owner/stall/item")
public class ItemStallController {

    @Autowired
    private ItemStallService itemStallService;

    @PostMapping("/create-new-item")
    public ResponseEntity<Api<ResponseItemDto>> createNewItem(@ModelAttribute RequestItemDto entity) throws Exception {

        System.out.println("Rendered: " + entity);

        ResponseItemDto success = itemStallService.createNewMealService(entity);

        return ResponseEntity.ok(Api.ok("Create new item success", "CREATE_ITEM_SUCCESS", success));
    }

    @PutMapping("/update-item/{id}")
    public ResponseEntity<Api<ResponseItemDto>> updateItem(@PathVariable Long id, @ModelAttribute RequestItemDto entity)
            throws Exception {
        ResponseItemDto success = itemStallService.updateMealService(id, entity);
        return ResponseEntity.ok(Api.ok("Update item success", "UPDATE_ITEM_SUCCESS", success));

    }

    @DeleteMapping("/delete-item/{id}")
    public ResponseEntity<Api<String>> deleteItem(@PathVariable Long id) throws Exception {
        itemStallService.deleteMealService(id);
        return ResponseEntity.ok(Api.ok("Item successfully deleted", "SUCCESS_DELETED", null));
    }

}
