package com.cism.backend.controller.system;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.system.review.ReviewRequest;
import com.cism.backend.dto.system.review.ReviewResponse;
import com.cism.backend.service.system.ReviewService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/client/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/review-get-all")
    public ResponseEntity<Api<List<ReviewResponse>>> reviewGetAll() throws Exception {
        List<ReviewResponse> success = reviewService.getAllReview();
        return ResponseEntity.ok(Api.ok("Review get all success", "REVIEW_GET_ALL_SUCCESS", success));
    }

    @PostMapping("/review-item")
    public ResponseEntity<Api<ReviewResponse>> reviewItem(@RequestBody ReviewRequest entity) throws Exception {
        System.out.println("Review : " + entity);
        ReviewResponse success = reviewService.createReviewService(entity);
        return ResponseEntity.ok(Api.ok("Review item success", "REVIEW_ITEM_SUCCESS", success));
    }

    @DeleteMapping("/review-delete/{userId}/{reviewId}")
    public ResponseEntity<Api<ReviewResponse>> reviewDelete(@PathVariable Long userId, @PathVariable Long reviewId)
            throws Exception {
        ReviewResponse success = reviewService.deleteReviewService(userId, reviewId);
        return ResponseEntity.ok(Api.ok("Review delete success", "REVIEW_DELETE_SUCCESS", success));
    }
}
