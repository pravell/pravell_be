package com.pravell.plan.domain.model;

import com.pravell.common.domain.BaseEntity;
import com.pravell.common.exception.AccessDeniedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(
        name = "plan_users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"plan_id", "user_id"})
        }
)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class PlanUsers extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID planId;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private PlanUserStatus planUserStatus;

    public static PlanUsers createOwnerForPlan(UUID userId, UUID planId) {
        return PlanUsers.builder()
                .userId(userId)
                .planId(planId)
                .planUserStatus(PlanUserStatus.OWNER)
                .build();
    }

    public void updateToMember() {
        if (this.planUserStatus.equals(PlanUserStatus.BLOCKED)) {
            log.info("해당 유저는 MEMBER로 변경이 불가능합니다. User Id : {}", this.userId);
            throw new AccessDeniedException("해당 유저는 MEMBER로 변경이 불가능합니다.");
        }
        this.planUserStatus = PlanUserStatus.MEMBER;
    }

    public void updateToWithdrawn() {
        if (!this.planUserStatus.equals(PlanUserStatus.MEMBER)){
            log.info("해당 유저는 WITHDRAWN으로 변경이 불가능합니다. User Id : {}", this.userId);
            throw new AccessDeniedException("해당 유저는 WITHDRAWN로 변경이 불가능합니다.");
        }
        this.planUserStatus = PlanUserStatus.WITHDRAWN;
    }


    public void updateToKicked() {
        if (!this.planUserStatus.equals(PlanUserStatus.MEMBER)){
            log.info("해당 유저는 KICKED로 변경이 불가능합니다. User Id : {}", this.userId);
            throw new AccessDeniedException("해당 유저는 KICKED로 변경이 불가능합니다.");
        }
        this.planUserStatus = PlanUserStatus.KICKED;
    }
}
