package com.cism.backend.service.stalls;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.stall.RequestItemDto;
import com.cism.backend.dto.stall.ResponseItemDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.stalls.StallItemRepository;
import com.cism.backend.util.CurrentUserLicence;
import com.cism.backend.util.IsBlack;

import jakarta.transaction.Transactional;

@Service
public class ItemStallService {

    @Autowired
    private CreateStallRepository createStallRepository;

    @Autowired
    private StallItemRepository stallItemRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private IsBlack isBlack;

    @Transactional
    public ResponseItemDto createNewMealService(RequestItemDto entity) {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        if (isBlack.isBlank(entity.name()) || isBlack.isBlankBigDecimal(entity.price())) {
            throw new BadrequestException("Please fill in all the fields", "FILL_ALL_FIELDS");
        }

        StallItemModel item = StallItemModel.builder()
                .stall(stall)
                .name(entity.name())
                .price(entity.price())
                .image(entity.image())
                .category(entity.category())
                .stocks(entity.stocks())
                .sold(0)
                .previousSold(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StallItemModel savedItem = stallItemRepository.save(item);

        return mapToResponseDto(savedItem);
    }

    @Transactional
    public ResponseItemDto updateMealService(Long id, RequestItemDto entity) throws Exception {
        StallItemModel item = helperByIdAndEntity(id, entity);

        item.setName(entity.name());
        item.setPrice(entity.price());
        item.setImage(entity.image());
        item.setCategory(entity.category());
        item.setStocks(entity.stocks());
        item.setUpdatedAt(Instant.now());

        StallItemModel updatedItem = stallItemRepository.save(item);

        return mapToResponseDto(updatedItem);
    }

    @Transactional
    public ResponseItemDto deleteMealService(Long id) throws Exception {
        StallItemModel item = helperById(id);
        stallItemRepository.deleteById(id);
        return mapToResponseDto(item);
    }

    private StallItemModel helperById(Long id) throws Exception {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        StallItemModel item = stallItemRepository.findById(id)
                .orElseThrow(() -> new BadrequestException("Item not found", "ITEM_NOT_FOUND"));

        if (item.getStall().getId() != stall.getId()) {
            throw new BadrequestException("You do not have permission to update this item", "UNAUTHORIZED");
        }
        return item;
    }

    private StallItemModel helperByIdAndEntity(Long id, RequestItemDto entity) throws Exception {
        StallItemModel item = helperById(id);

        if (isBlack.isBlank(entity.name()) || isBlack.isBlankBigDecimal(entity.price())) {
            throw new BadrequestException("Please fill in all the fields", "FILL_ALL_FIELDS");
        }
        return item;
    }

    private ResponseItemDto mapToResponseDto(StallItemModel entity) {
        return new ResponseItemDto(
                entity.getId(),
                entity.getStall().getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getImage(),
                entity.getCategory(),
                entity.getStocks(),
                entity.getSold(),
                entity.getPreviousSold(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
