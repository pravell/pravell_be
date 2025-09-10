package com.pravell.marker.presentation;

import com.pravell.ControllerTestSupport;
import com.pravell.marker.domain.model.Marker;
import com.pravell.marker.domain.repository.MarkerRepository;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class MarkerControllerTestSupport extends ControllerTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PlanRepository planRepository;

    @Autowired
    protected PlanUsersRepository planUsersRepository;

    @Autowired
    protected MarkerRepository markerRepository;

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

    protected Marker getMarker(UUID planId, String color, String description){
        return Marker.builder()
                .planId(planId)
                .description(description)
                .color(color)
                .build();
    }

}
