package com.vms.service;

import com.vms.dto.RegistrationDraftDto;
import java.util.Optional;

public interface DraftService {
    void saveDraft(String sessionId, RegistrationDraftDto draft);
    Optional<RegistrationDraftDto> getDraft(String sessionId);
    void deleteDraft(String sessionId);
}
