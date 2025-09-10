package com.pravell.place.presentation;

import com.pravell.ControllerTestSupport;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PlaceControllerTestSupport extends ControllerTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PlanRepository planRepository;

    @Autowired
    protected PlanUsersRepository planUsersRepository;

    @Autowired
    protected PinPlaceRepository pinPlaceRepository;

    protected User getUser(UserStatus status) {
        return User.builder()
                .id(UUID.randomUUID())
                .userId("userId" + UUID.randomUUID())
                .nickname("nickname" + UUID.randomUUID())
                .password("passworddd")
                .status(status)
                .build();
    }

    protected Plan getPlan(boolean isDeleted) {
        return getPlan(isDeleted, true);
    }

    protected Plan getPlan(boolean isDeleted, boolean isPublic) {
        return Plan.builder()
                .id(UUID.randomUUID())
                .name("name")
                .isDeleted(isDeleted)
                .isPublic(isPublic)
                .startDate(LocalDate.parse("2025-09-29"))
                .endDate(LocalDate.parse("2025-09-30"))
                .build();
    }

    protected PlanUsers getPlanUsers(UUID planId, UUID userId, PlanUserStatus status) {
        return PlanUsers.builder()
                .planId(planId)
                .userId(userId)
                .planUserStatus(status)
                .build();
    }

    protected PinPlace getPinPlace(UUID planId) {
        return getPinPlace("addressss", planId);
    }

    protected PinPlace getPinPlace(String address, UUID planId) {
        return PinPlace.builder()
                .placeId(UUID.randomUUID().toString())
                .nickname("nickname")
                .title("title")
                .address(address)
                .roadAddress(address + " road")
                .hours("[\"Monday: 10:00 AM – 9:00 PM\",\"Tuesday: 10:00 AM – 9:00 PM\",\"Wednesday: 10:00 AM – 9:00 PM\",\"Thursday: 10:00 AM – 9:00 PM\",\"Friday: 10:00 AM – 9:00 PM\",\"Saturday: 10:00 AM – 9:00 PM\",\"Sunday: 10:00 AM – 9:00 PM\"]")
                .mapy("12345")
                .mapy("123456")
                .pinColor("#F54927")
                .planId(planId)
                .savedUser(UUID.randomUUID())
                .description("description")
                .lastRefreshedAt(LocalDateTime.now())
                .latitude(new BigDecimal("123.44567"))
                .longitude(new BigDecimal("123.23456"))
                .build();
    }

}
