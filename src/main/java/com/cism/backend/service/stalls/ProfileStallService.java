package com.cism.backend.service.stalls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.stall.OwnerStallDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.exception.UnauthorizedException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.StallDrinksModel;
import com.cism.backend.model.stalls.StallIncomesModel;
import com.cism.backend.model.stalls.StallMealsModel;
import com.cism.backend.model.stalls.StallSnacksModel;
import com.cism.backend.model.stalls.StallUsersModel;
import com.cism.backend.repository.admin.CreateStallRepository;

import jakarta.transaction.Transactional;

@Service
public class ProfileStallService {
    @Autowired
    CreateStallRepository createStallRepository;




    @Transactional
    public OwnerStallDto getUserService(){
        String licence = getCurrentUserLicence();

        StallModel stall = createStallRepository.findByLicence(licence).orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        return new OwnerStallDto(
            stall.getId(),
            stall.getUserList().stream().findFirst().map(this::mapUser).orElse(null),      
            stall.getMealList().stream().map(this::mapMeal).toList(),
            stall.getSnackList().stream().map(this::mapSnacks).toList(),
            stall.getDrinkList().stream().map(this::mapDrinks).toList(),
            stall.getIncomeList().stream().findFirst().map(this::mapIncomes).orElse(null)

        ); 


    }

    private OwnerStallDto.UserModel mapUser(StallUsersModel u){
        return new OwnerStallDto.UserModel(
            u.getId(),
            u.getStall().getId(),
            u.getName(),
            u.getDescription(),
            u.getImage(),
            u.getStatus(),
            u.getOpenAt(),
            u.getCloseAt(),
            u.getCreatedAt(),
            u.getUpdatedAt()
        );
    }

    private OwnerStallDto.MealsModel mapMeal(StallMealsModel m){
        return new OwnerStallDto.MealsModel(
            m.getId(),
            m.getStall().getId(),
            m.getPrice(),
            m.getName(),
            m.getImage(),
            m.getStocks(),
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }

    private OwnerStallDto.SnacksModel mapSnacks(StallSnacksModel s) {
        return new OwnerStallDto.SnacksModel(
            s.getId(),
            s.getStall().getId(),
            s.getPrice(),
            s.getName(),
            s.getImage(),
            s.getStocks(),
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }

    private OwnerStallDto.DrinksModel mapDrinks(StallDrinksModel d) {
        return new OwnerStallDto.DrinksModel(
            d.getId(),
            d.getStall().getId(),
            d.getPrice(),
            d.getName(),
            d.getImage(),
            d.getStocks(),
            d.getCreatedAt(),
            d.getUpdatedAt()
        );
    }

    private OwnerStallDto.IncomesModel mapIncomes(StallIncomesModel i) {
        return new OwnerStallDto.IncomesModel(
            i.getId(),
            i.getStall().getId(),
            i.getIncome(),
            i.getEarnedAt(),
            i.getCreatedAt()
        );
    }

    public String getCurrentUserLicence(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Stall owner not authenticated", "STALL_OWNER_NOT_AUTHENTICATED");
        }
        return auth.getName();
    }

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}




// update, delete, 