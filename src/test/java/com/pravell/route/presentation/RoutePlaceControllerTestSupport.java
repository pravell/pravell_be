package com.pravell.route.presentation;

import com.pravell.ControllerTestSupport;
import com.pravell.place.domain.model.PinPlace;
import com.pravell.place.domain.repository.PinPlaceRepository;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanUserStatus;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanRepository;
import com.pravell.plan.domain.repository.PlanUsersRepository;
import com.pravell.route.domain.model.Route;
import com.pravell.route.domain.model.RoutePlace;
import com.pravell.route.domain.repository.RoutePlaceRepository;
import com.pravell.route.domain.repository.RouteRepository;
import com.pravell.user.domain.model.User;
import com.pravell.user.domain.model.UserStatus;
import com.pravell.user.domain.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RoutePlaceControllerTestSupport extends ControllerTestSupport {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PlanRepository planRepository;

    @Autowired
    protected PlanUsersRepository planUsersRepository;

    @Autowired
    protected RouteRepository routeRepository;

    @Autowired
    protected RoutePlaceRepository routePlaceRepository;

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

    protected Route getRoute(UUID planId, boolean isDeleted) {
        return Route.builder()
                .id(UUID.randomUUID())
                .planId(planId)
                .name("플랜 이름" + UUID.randomUUID())
                .description("플랜 설명" + UUID.randomUUID())
                .isDeleted(isDeleted)
                .build();
    }

    protected PinPlace getPinPlace(UUID planId) {
        return getPinPlace(planId, "장소 이름", "장소 주소", "장소 도로명 주소", "12345667", "09876", "#123456");
    }

    protected PinPlace getPinPlace(UUID planId, String title, String address, String roadAddress, String mapx,
                                   String mapy, String pinColor) {
        return PinPlace.builder()
                .placeId("PlaceeeIDDdd")
                .nickname("nicknameee")
                .title(title)
                .address(address)
                .roadAddress(roadAddress)
                .mapx(mapx)
                .mapy(mapy)
                .pinColor(pinColor)
                .planId(planId)
                .savedUser(UUID.randomUUID())
                .lastRefreshedAt(LocalDateTime.now())
                .description("장소 설명")
                .hours("정보 없음")
                .latitude(new BigDecimal("123.4567"))
                .longitude(new BigDecimal("23.45678"))
                .build();
    }

    protected RoutePlace getRoutePlace(UUID routeId, Long pinPlaceId, Long sequence, String description,
                                       String nickname, LocalDate date) {
        return RoutePlace.builder()
                .routeId(routeId)
                .pinPlaceId(pinPlaceId)
                .sequence(sequence)
                .description(description)
                .nickname(nickname)
                .date(date)
                .build();
    }

}
