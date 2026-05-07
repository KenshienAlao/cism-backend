package com.cism.backend.repository.stalls;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cism.backend.model.stalls.ItemVariationsModel;

@Repository
public interface ItemvariationsRepository extends JpaRepository<ItemVariationsModel, Long> {

    List<ItemVariationsModel> findByStallitem_Id(Long itemId);
}
