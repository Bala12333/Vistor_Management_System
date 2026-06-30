package com.vms.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.RegistrationDraftDto;
import com.vms.service.DraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DraftServiceImpl implements DraftService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void saveDraft(String sessionId, RegistrationDraftDto draft) {
        try {
            String json = objectMapper.writeValueAsString(draft);
            redisTemplate.opsForValue().set("draft:" + sessionId, json, Duration.ofHours(24));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize draft for session {}", sessionId, e);
            throw new RuntimeException("Failed to save draft", e);
        }
    }

    @Override
    public Optional<RegistrationDraftDto> getDraft(String sessionId) {
        String json = redisTemplate.opsForValue().get("draft:" + sessionId);
        if (json != null) {
            try {
                return Optional.of(objectMapper.readValue(json, RegistrationDraftDto.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize draft for session {}", sessionId, e);
            }
        }
        return Optional.empty();
    }

    @Override
    public void deleteDraft(String sessionId) {
        redisTemplate.delete("draft:" + sessionId);
    }
}
