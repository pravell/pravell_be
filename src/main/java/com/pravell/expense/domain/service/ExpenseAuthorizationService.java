package com.pravell.expense.domain.service;

import com.pravell.expense.domain.model.PlanMember;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ExpenseAuthorizationService {

    public boolean isOwnerOrMember(UUID userId, List<PlanMember> planMembers) {
        return planMembers.stream()
                .anyMatch(pm -> pm.getMemberId().equals(userId));
    }

}
