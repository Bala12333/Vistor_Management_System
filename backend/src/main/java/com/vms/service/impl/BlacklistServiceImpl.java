package com.vms.service.impl;

import com.vms.dto.BlacklistRequest;
import com.vms.entity.Blacklist;
import com.vms.repository.BlacklistRepository;
import com.vms.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistRepository blacklistRepository;

    @Override
    @Transactional
    public Blacklist addToBlacklist(BlacklistRequest request) {
        if (!StringUtils.hasText(request.getMobileNumber()) && !StringUtils.hasText(request.getIdNumber())) {
            throw new IllegalArgumentException("Either mobile number or ID number must be provided");
        }

        Blacklist blacklist = new Blacklist();
        blacklist.setMobileNumber(request.getMobileNumber());
        blacklist.setIdNumber(request.getIdNumber());
        blacklist.setReason(request.getReason());
        blacklist.setIsActive(true);

        return blacklistRepository.save(blacklist);
    }

    @Override
    @Transactional
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = blacklistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blacklist record not found"));
        blacklist.setIsActive(false);
        blacklistRepository.save(blacklist);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blacklist> getAllBlacklisted(Pageable pageable) {
        // Find all for history, or we could add a query to find only active ones
        return blacklistRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String mobileNumber, String idNumber) {
        if (StringUtils.hasText(mobileNumber)) {
            if (blacklistRepository.findByMobileNumberAndIsActiveTrue(mobileNumber).isPresent()) {
                return true;
            }
        }
        if (StringUtils.hasText(idNumber)) {
            if (blacklistRepository.findByIdNumberAndIsActiveTrue(idNumber).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
