package com.cism.backend.service.stalls;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.stall.ItemVariationsRequest;
import com.cism.backend.dto.stall.ItemVariationsResponse;
import com.cism.backend.dto.stall.RequestItemDto;
import com.cism.backend.dto.stall.RequestItemVariationDto;
import com.cism.backend.dto.stall.ResponseItemDto;
import com.cism.backend.service.users.FileStorageService;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.ItemVariationsModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.stalls.ItemvariationsRepository;
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
    private ItemvariationsRepository itemvariationsRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private IsBlack isBlack;

    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public ResponseItemDto createNewMealService(RequestItemDto entity) {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        boolean hasVariations = entity.variations() != null && !entity.variations().isEmpty();

        if (isBlack.isBlank(entity.name())) {
            throw new BadrequestException("Name is required", "NAME_REQUIRED");
        }

        if (!hasVariations) {
            if (isBlack.isBlankBigDecimal(entity.price()) || entity.stocks() == null) {
                throw new BadrequestException("Price and stocks are required when no variations are added",
                        "PRICE_STOCKS_REQUIRED");
            }
        }

        String imageUrl = null;
        try {
            if (entity.imageFile() != null && !entity.imageFile().isEmpty()) {
                imageUrl = fileStorageService.stallItemImage(entity.imageFile());
            } else if (entity.image() != null && !entity.image().isBlank()) {
                imageUrl = entity.image();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StallItemModel item = StallItemModel.builder()
                .stall(stall)
                .name(entity.name())
                .price(entity.price())
                .image(imageUrl)
                .category(entity.category())
                .stocks(entity.stocks())
                .sold(0)
                .previousSold(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        StallItemModel savedItem = stallItemRepository.save(item);

        if (hasVariations) {
            for (RequestItemVariationDto varDto : entity.variations()) {
                String varImageUrl = null;
                try {
                    if (varDto.imageFile() != null && !varDto.imageFile().isEmpty()) {
                        varImageUrl = fileStorageService.stallItemImage(varDto.imageFile());
                    } else if (varDto.image() != null && !varDto.image().isBlank()) {
                        varImageUrl = varDto.image();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ItemVariationsModel variation = ItemVariationsModel.builder()
                        .stallitem(savedItem)
                        .name(varDto.name())
                        .price(varDto.price())
                        .stock(varDto.stocks())
                        .image(varImageUrl)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                savedItem.getItemVariations().add(variation);
            }
            stallItemRepository.save(savedItem);
        }

        return mapToResponseDto(savedItem);
    }

    @Transactional
    public ResponseItemDto updateMealService(Long id, RequestItemDto entity) throws Exception {
        StallItemModel item = helperByIdAndEntity(id, entity);

        item.setName(entity.name());
        item.setPrice(entity.price());

        if (entity.imageFile() != null && !entity.imageFile().isEmpty()) {
            try {
                item.setImage(fileStorageService.stallItemImage(entity.imageFile()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (entity.image() != null && !entity.image().isBlank()) {
            item.setImage(entity.image());
        }

        item.setCategory(entity.category());
        item.setStocks(entity.stocks());
        item.setUpdatedAt(Instant.now());

        if (entity.variations() != null) {
            item.getItemVariations().clear();

            for (RequestItemVariationDto varDto : entity.variations()) {
                String varImageUrl = null;
                try {
                    if (varDto.imageFile() != null && !varDto.imageFile().isEmpty()) {
                        varImageUrl = fileStorageService.stallItemImage(varDto.imageFile());
                    } else if (varDto.image() != null && !varDto.image().isBlank()) {
                        varImageUrl = varDto.image();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ItemVariationsModel variation = ItemVariationsModel.builder()
                        .stallitem(item)
                        .name(varDto.name())
                        .price(varDto.price())
                        .stock(varDto.stocks())
                        .image(varImageUrl)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                item.getItemVariations().add(variation);
            }
        }

        StallItemModel updatedItem = stallItemRepository.save(item);

        return mapToResponseDto(updatedItem);
    }

    @Transactional
    public ResponseItemDto deleteMealService(Long id) throws Exception {
        StallItemModel item = helperById(id);
        stallItemRepository.deleteById(id);
        return mapToResponseDto(item);
    }

    // item variation

    @Transactional
    public ItemVariationsResponse addVariationsService(Long itemId, ItemVariationsRequest entity) throws Exception {
        String stall = currentUserLicence.getCurrentUserLicence();
        StallModel stallModel = createStallRepository.findByLicence(stall)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        StallItemModel item = stallItemRepository.findById(itemId)
                .orElseThrow(() -> new BadrequestException("Item not found", "ITEM_NOT_FOUND"));
        if (item.getStall().getId() != stallModel.getId()) {
            throw new BadrequestException("You do not have permission to update this item", "UNAUTHORIZED");
        }

        String imageUrl = null;
        if (entity.imageFile() != null && !entity.imageFile().isEmpty()) {
            try {
                imageUrl = fileStorageService.stallItemImage(entity.imageFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (entity.image() != null && !entity.image().isBlank()) {
            imageUrl = entity.image();
        }

        ItemVariationsModel addVariation = ItemVariationsModel.builder()
                .stallitem(item)
                .name(entity.name())
                .price(entity.price())
                .stock(entity.stocks())
                .image(imageUrl)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ItemVariationsModel savedVariation = itemvariationsRepository.save(addVariation);

        // Sync total stock to parent item
        item.getItemVariations().add(savedVariation);
        int totalStock = item.getItemVariations().stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();
        item.setStocks(totalStock);
        stallItemRepository.save(item);

        return mapToVariationResponseDto(savedVariation);
    }

    @Transactional
    public ItemVariationsResponse updateVariationService(Long variationId, ItemVariationsRequest entity)
            throws Exception {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stallModel = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        ItemVariationsModel variation = itemvariationsRepository.findById(variationId)
                .orElseThrow(() -> new BadrequestException("Variation not found", "VARIATION_NOT_FOUND"));

        if (variation.getStallitem().getStall().getId() != stallModel.getId()) {
            throw new BadrequestException("You do not have permission to update this variation", "UNAUTHORIZED");
        }

        String imageUrl = variation.getImage(); // keep existing by default
        if (entity.imageFile() != null && !entity.imageFile().isEmpty()) {
            try {
                imageUrl = fileStorageService.stallItemImage(entity.imageFile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (entity.image() != null && !entity.image().isBlank()) {
            imageUrl = entity.image();
        }

        variation.setName(entity.name());
        variation.setPrice(entity.price());
        variation.setStock(entity.stocks());
        variation.setImage(imageUrl);
        variation.setUpdatedAt(Instant.now());

        ItemVariationsModel updated = itemvariationsRepository.save(variation);

        // Sync total stock to parent item
        StallItemModel item = updated.getStallitem();
        int totalStock = item.getItemVariations().stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();
        item.setStocks(totalStock);
        stallItemRepository.save(item);

        return mapToVariationResponseDto(updated);
    }

    @Transactional
    public ItemVariationsResponse deleteVariationService(Long variationId) throws Exception {
        String licence = currentUserLicence.getCurrentUserLicence();
        StallModel stallModel = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        ItemVariationsModel variation = itemvariationsRepository.findById(variationId)
                .orElseThrow(() -> new BadrequestException("Variation not found", "VARIATION_NOT_FOUND"));

        if (variation.getStallitem().getStall().getId() != stallModel.getId()) {
            throw new BadrequestException("You do not have permission to delete this variation", "UNAUTHORIZED");
        }

        StallItemModel item = variation.getStallitem();
        item.getItemVariations().remove(variation);
        itemvariationsRepository.deleteById(variationId);

        // Sync total stock to parent item
        int totalStock = item.getItemVariations().stream()
                .mapToInt(v -> v.getStock() != null ? v.getStock() : 0)
                .sum();
        item.setStocks(totalStock);
        stallItemRepository.save(item);

        return mapToVariationResponseDto(variation);
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

        boolean hasVariations = entity.variations() != null && !entity.variations().isEmpty();

        if (isBlack.isBlank(entity.name())) {
            throw new BadrequestException("Name is required", "NAME_REQUIRED");
        }

        if (!hasVariations) {
            if (isBlack.isBlankBigDecimal(entity.price()) || entity.stocks() == null) {
                throw new BadrequestException("Price and stocks are required when no variations are added",
                        "PRICE_STOCKS_REQUIRED");
            }
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
                entity.getUpdatedAt(),
                entity.getItemVariations() != null
                        ? entity.getItemVariations().stream().map(this::mapToVariationResponseDto).toList()
                        : java.util.List.of());
    }

    private ItemVariationsResponse mapToVariationResponseDto(ItemVariationsModel entity) {
        return new ItemVariationsResponse(
                entity.getId(),
                entity.getName(),
                entity.getStock(),
                entity.getPrice(),
                entity.getImage());
    }
}
