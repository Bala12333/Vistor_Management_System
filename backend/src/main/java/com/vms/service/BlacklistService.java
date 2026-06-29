package com.vms.service;

import com.vms.dto.BlacklistRequest;
import com.vms.entity.Blacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BlacklistService {
    Blacklist addToBlacklist(BlacklistRequest request);
    void removeFromBlacklist(Long id);
    Page<Blacklist> getAllBlacklisted(Pageable pageable);
    boolean isBlacklisted(String mobileNumber, String idNumber);
}
