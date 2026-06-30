package com.vms.service.impl;

import com.vms.dto.BlacklistRequest;
import com.vms.entity.Blacklist;
import com.vms.repository.BlacklistRepository;
import com.vms.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final StringRedisTemplate redisTemplate;

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

        Blacklist saved = blacklistRepository.save(blacklist);
        
        if (StringUtils.hasText(saved.getMobileNumber())) {
            redisTemplate.opsForValue().set("blacklist_mobile:" + saved.getMobileNumber(), "1", Duration.ofHours(1));
        }
        if (StringUtils.hasText(saved.getIdNumber())) {
            redisTemplate.opsForValue().set("blacklist_id:" + saved.getIdNumber(), "1", Duration.ofHours(1));
        }

        return saved;
    }

    @Override
    @Transactional
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = blacklistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blacklist record not found"));
        blacklist.setIsActive(false);
        blacklistRepository.save(blacklist);
        
        if (StringUtils.hasText(blacklist.getMobileNumber())) {
            redisTemplate.delete("blacklist_mobile:" + blacklist.getMobileNumber());
        }
        if (StringUtils.hasText(blacklist.getIdNumber())) {
            redisTemplate.delete("blacklist_id:" + blacklist.getIdNumber());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blacklist> getAllBlacklisted(Pageable pageable) {
        return blacklistRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String mobileNumber, String idNumber) {
        if (StringUtils.hasText(mobileNumber)) {
            String redisKey = "blacklist_mobile:" + mobileNumber;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                return true;
            }
            if (blacklistRepository.findByMobileNumberAndIsActiveTrue(mobileNumber).isPresent()) {
                redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(1));
                return true;
            }
        }
        
        if (StringUtils.hasText(idNumber)) {
            String redisKey = "blacklist_id:" + idNumber;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                return true;
            }
            if (blacklistRepository.findByIdNumberAndIsActiveTrue(idNumber).isPresent()) {
                redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(1));
                return true;
            }
        }
        return false;
    }
}
