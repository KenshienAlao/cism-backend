package com.cism.backend.service.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.system.review.ReviewRequest;
import com.cism.backend.dto.system.review.ReviewResponse;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.system.review.ReviewModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.stalls.StallItemRepository;
import com.cism.backend.repository.system.ReviewRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;
import com.cism.backend.util.IsBlack;

import java.time.Instant;
import java.util.List;

import jakarta.transaction.Transactional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private IsBlack isBlack;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository createStallRepository;

    @Autowired
    private StallItemRepository stallItemRepository;

    @Transactional
    public List<ReviewResponse> getAllReview() throws Exception {
        return reviewRepository.findAll().stream().map(this::mapToResponseDto).toList();
    }

    @Transactional
    public ReviewResponse createReviewService(ReviewRequest entity) throws Exception {
        String email = currentUserLicence.getCurrentUserEmail();
        StallModel stall = createStallRepository.findById(entity.stallId())
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        AuthModel user = registerRepository.findByEmail(email)
                .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));

        if (!user.getEmail().equals(email)) {
            throw new BadrequestException("This user not register ", "UNREGISTER");
        }

        if (isBlack.isBlankLong(entity.itemId())) {
            throw new BadrequestException("Item not found", "ITEM_NOT_FOUND");
        }

        if (isBlack.isBlank(entity.comment()) || isBlack.isBlankInteger(entity.star())) {
            throw new BadrequestException("Invalid review data", "INVALID_REVIEW_DATA");
        }

        if (reviewRepository.findByStallitemIdAndStallIdAndUsersId(entity.itemId(), entity.stallId(), user.getId())
                .isPresent()) {
            throw new BadrequestException("You have already reviewed this item", "ALREADY_REVIEWED");
        }

        StallItemModel item = stallItemRepository.findById(entity.itemId())
                .orElseThrow(() -> new BadrequestException("Item not found", "ITEM_NOT_FOUND"));

        ReviewModel review = ReviewModel.builder()
                .stall(stall)
                .users(user)
                .stallitem(item)
                .star(entity.star())
                .comment(entity.comment())
                .createAt(Instant.now())
                .build();

        ReviewModel savedReview = reviewRepository.save(review);
        return mapToResponseDto(savedReview);
    }

    @Transactional
    public ReviewResponse deleteReviewService(Long userId, Long reviewId) throws Exception {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        ReviewModel review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BadrequestException("Review not found", "REVIEW_NOT_FOUND"));

        if (!stall.getId().equals(review.getStall().getId())) {
            throw new BadrequestException("Unauthorized", "UNAUTHORIZED");
        }

        if (!userId.equals(review.getUsers().getId())) {
            throw new BadrequestException("Unauthorized", "UNAUTHORIZED");
        }

        reviewRepository.deleteById(review.getId());
        return mapToResponseDto(review);
    }

    private ReviewResponse mapToResponseDto(ReviewModel entity) {
        var user = entity.getUsers();
        return new ReviewResponse(
                new ReviewResponse.User(user.getClientName(), user.getAvatar(), user.getRole()),
                entity.getComment(),
                entity.getStar(),
                entity.getStallitem().getId(),
                entity.getCreateAt());
    }

}
