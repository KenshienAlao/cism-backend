package com.cism.backend.service.users;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.users.AllStallDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.stalls.StallUsersModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;

import jakarta.transaction.Transactional;

@Service
public class ItemService {

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository createStallRepository;

    @Transactional
    public List<AllStallDto> getAllItem() {
        String email = currentUserLicence.getCurrentUserEmail();
        registerRepository.findByEmail(email)
                .orElseThrow(() -> new BadrequestException("User not found! please login again.", "USER_NOT_FOUND"));

        return createStallRepository.findAll().stream()
                .map(this::mapToAllStallDto)
                .toList();
    }

    private AllStallDto mapToAllStallDto(StallModel stall) {
        List<StallUsersModel> userList = stall.getUserList();
        StallUsersModel profile = (userList != null && !userList.isEmpty()) ? userList.get(0) : null;

        List<StallItemModel> itemList = stall.getItemList();
        List<AllStallDto.Item> items = (itemList != null ? itemList.stream() : Stream.<StallItemModel>empty())
                .map(i -> new AllStallDto.Item(
                        i.getId(),
                        i.getStall().getId(),
                        i.getName(),
                        i.getPrice(),
                        i.getStocks(),
                        i.getImage(),
                        i.getCategory()))
                .toList();

        List<AllStallDto.Review> reviews = (stall.getReviewList() != null ? stall.getReviewList().stream() : java.util.stream.Stream.<com.cism.backend.model.system.review.ReviewModel>empty())
                .map(r -> new AllStallDto.Review(
                        r.getId(),
                        r.getItemId(),
                        r.getStar(),
                        r.getComment(),
                        r.getCreateAt()))
                .toList();

        return new AllStallDto(
                stall.getId(),
                profile != null ? profile.getName() : "Unknown",
                profile != null ? profile.getDescription() : "",
                profile != null ? profile.getImage() : null,
                profile != null ? profile.getOpenAt() : "00:00",
                profile != null ? profile.getCloseAt() : "00:00",
                profile != null ? profile.getRole() : "STALL",
                profile != null ? profile.getStatus() : false,
                reviews,
                items);
    }
}
