package com.pravell.plan.application;

import com.pravell.common.exception.AccessDeniedException;
import com.pravell.plan.domain.model.Plan;
import com.pravell.plan.domain.model.PlanInviteCode;
import com.pravell.plan.domain.model.PlanUsers;
import com.pravell.plan.domain.repository.PlanInviteCodeRepository;
import com.pravell.plan.domain.service.PlanAuthorizationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateInviteCodeService {

    private final PlanInviteCodeRepository planInviteCodeRepository;
    private final PlanAuthorizationService planAuthorizationService;

    @Value("${invite-code.characters}")
    private String CHARACTERS;

    @Value("${invite-code.length}")
    private int CODE_LENGTH;

    @Value("${invite-code.expires}")
    private int CODE_EXPIRES;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public String create(Plan plan, List<PlanUsers> planUsers, UUID userId) {
        validateMemberOrOwnerForInviteCode(planUsers, userId);

        String code = generateCode();

        planInviteCodeRepository.save(PlanInviteCode.builder()
                        .planId(plan.getId())
                        .code(code)
                        .createdBy(userId)
                        .expiresAt(LocalDateTime.now().plusDays(CODE_EXPIRES))
                .build());

        return code;
    }

    private void validateMemberOrOwnerForInviteCode(List<PlanUsers> planUsers, UUID userId) {
        if (!planAuthorizationService.isOwnerOrMember(userId, planUsers)){
            throw new AccessDeniedException("해당 플랜의 초대코드를 생성 할 권한이 없습니다.");
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

}
