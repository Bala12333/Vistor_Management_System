# Product Requirements Document (PRD) v4.0
**Project:** Visitor Management System (VMS)
**Document ID:** VMS-PRD-004
**Version:** v4.0
**Date:** June 16, 2025
**Architecture:** Final — Spring Boot + OTP + Blacklist + WhatsApp + WebSocket + Redis + Signed QR + Duplicate Detection + Scheduled Reports
**Status:** FINAL — Backend locked to Spring Boot

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Product Vision](#3-product-vision)
4. [Objectives](#4-objectives)
5. [Target Users](#5-target-users)
6. [Visitor Categories and Routing](#6-visitor-categories-and-routing)
7. [How the System Works — End to End](#7-how-the-system-works--end-to-end)
8. [Core Features — Full Detail](#8-core-features--full-detail)
9. [Badge Design Specification](#9-badge-design-specification)
10. [Notification Templates](#10-notification-templates)
11. [Session and Draft Management](#11-session-and-draft-management)
12. [Duplicate Visit Detection](#12-duplicate-visit-detection)
13. [Signed QR Code Security](#13-signed-qr-code-security)
14. [Scheduled Report Engine](#14-scheduled-report-engine)
15. [Non-Functional Requirements](#15-non-functional-requirements)
16. [Technology Stack](#16-technology-stack)
17. [Spring Boot Project Structure](#17-spring-boot-project-structure)
18. [User Stories](#18-user-stories)
19. [Integration Points](#19-integration-points)
20. [Risks and Mitigations](#20-risks-and-mitigations)
21. [Out of Scope](#21-out-of-scope)
22. [20-Day Timeline](#22-20-day-timeline)

---

## 1. Executive Summary

The Visitor Management System (VMS) v4.0 is the final, complete specification for a web-based security and visitor lifecycle platform, built on **Java 17 and Spring Boot 3.2**, as part of a 20-day internship program. It replaces all paper-based visitor logs with a fully digital, verified, and auditable system.

This version locks the backend technology decision to Spring Boot across every document, every code sample, and every architectural diagram — removing the dual Flask/Spring Boot ambiguity present in earlier versions. All implementation guidance, library choices, and setup instructions are Spring Boot-specific.

The feature set remains unchanged from v3.0:

- **OTP-based phone verification** via Twilio Verify
- **Blacklist engine** with Redis hot-path caching
- **WhatsApp/SMS approval workflow** via Twilio
- **WebSocket-powered real-time dashboard** via Spring WebSocket + STOMP
- **Visitor category routing** across 6 defined categories
- **Signed QR codes** using HMAC-SHA256
- **Duplicate visit detection**
- **Scheduled daily report emails** via Spring `@Scheduled`

---

## 2. Problem Statement

### 2.1 Original problems (solved by v1.0)

| Problem | Solution |
|---------|---------|
| Paper visitor logs | Digital registration and storage |
| No audit trail | Database records for every action |
| Manual badge writing | System-generated printed badge |
| No visitor history | Queryable visitor and visit records |

### 2.2 v1.0 gaps (solved by v2.0)

| Problem | Solution |
|---------|---------|
| Fake phone registrations | OTP verification via Twilio Verify |
| No threat prevention | Blacklist engine with Redis hot path |
| Email-only slow approvals | WhatsApp primary, email fallback |
| Stale admin dashboard | WebSocket real-time event push |

### 2.3 v2.0 gaps (solved by v3.0)

| Problem | Solution |
|---------|---------|
| No visitor category rules | 6 categories with routing per category |
| Unsigned QR — forgeable | HMAC-SHA256 signed QR payload |
| Duplicate registrations possible | Same-day duplicate detection |
| Admin must manually pull reports | Scheduled daily report email at 9 AM |
| No draft save on registration form | Redis-based form draft with 24-hour TTL |
| Notification content undefined | Full templates for every message type |

### 2.4 v3.0 gap (solved by v4.0)

| Problem | Solution |
|---------|---------|
| Dual Flask/Spring Boot ambiguity in every spec | Backend locked to Spring Boot 3.2 everywhere — one stack, one set of code samples, one setup path |

---

## 3. Product Vision

> Build a smart, secure, push-based Visitor Management System on Spring Boot, where every visitor is verified before entry, every threat is blocked automatically, every approval happens in seconds, every admin sees the building's occupancy in real time, every badge is printed in under 60 seconds, and every manager receives yesterday's visitor summary before they start their morning.

---

## 4. Objectives

1. Verify visitor phone identity via OTP before any approval request is sent
2. Cross-check every visitor's phone and government ID against the blacklist before approval
3. Detect and block duplicate visit registrations for the same visitor on the same day
4. Deliver employee approval requests via WhatsApp (primary) and email (fallback)
5. Issue cryptographically signed QR passes that cannot be forged or replicated
6. Push live visitor events to the admin dashboard via Spring WebSocket + STOMP
7. Store all ephemeral data (OTP, sessions, drafts, blacklist cache) in Redis via Spring Data Redis
8. Email a daily visitor summary report via Spring `@Scheduled` every morning at 9 AM
9. Reduce pre-registered visitor check-in time to under 60 seconds at reception
10. Route visitors to the correct check-in point based on their visit category
11. Build the entire backend on a single, locked technology stack — Spring Boot 3.2 — with zero ambiguity for the development team

---

## 5. Target Users

| Role | Who They Are | What They Need | Key Screen |
|------|-------------|---------------|------------|
| Visitor | External individual visiting | Fast registration, QR pass, clear instructions | Public registration form |
| Receptionist | Front-desk staff | Speed, search, badge print, walk-in creation | Reception check-in dashboard |
| Employee (Host) | Company employee receiving visitor | One-tap approve/reject, minimal interruption | WhatsApp message + approval screen |
| Admin | IT or operations administrator | Full visibility, user management, reports, blacklist | Admin dashboard |
| Security | Gate guard at entry | QR verification, allow/deny decision | QR scanner screen |

### 5.1 Role Permissions Matrix

| Feature | Visitor | Receptionist | Employee | Admin | Security |
|---------|---------|-------------|---------|-------|---------|
| Register visit | ✓ | ✓ (walk-in) | — | — | — |
| Approve/reject visits | — | — | Own only | All | — |
| Check in visitor | — | ✓ | — | — | — |
| Check out visitor | — | ✓ | — | — | — |
| Scan QR at gate | — | — | — | — | ✓ |
| View all visitors | — | Today only | Own only | All time | — |
| Manage blacklist | — | — | — | ✓ | — |
| Generate reports | — | — | — | ✓ | — |
| Manage users | — | — | — | ✓ | — |
| View audit log | — | — | — | ✓ | — |
| System settings | — | — | — | ✓ | — |

---

## 6. Visitor Categories and Routing

### 6.1 The Six Visitor Categories

Every visitor selects one of six categories during registration. This determines the approval flow, badge colour, and routing destination. These category rules are stored in a `visitor_categories` table and read by the Spring Boot service layer at runtime — they are not hardcoded in Java, so an admin can adjust routing without a redeploy.

#### Category 1 — Client / Business Meeting
- **Approval required:** Yes | **OTP required:** Yes | **Blacklist check:** Yes
- **Badge colour:** Blue header | **Routing:** Main reception → escort by host
- **Max duration:** 8 hours | **ID required:** Yes

#### Category 2 — Interview Candidate
- **Approval required:** Yes (HR) | **OTP required:** Yes | **Blacklist check:** Yes
- **Badge colour:** Green header | **Routing:** Main reception → HR waiting area
- **Max duration:** 4 hours | **ID required:** Yes
- **Special rule:** Waiting-area pass only, no floor access

#### Category 3 — Vendor / Supplier
- **Approval required:** Yes | **OTP required:** Yes | **Blacklist check:** Yes
- **Badge colour:** Yellow header | **Routing:** Goods entry / delivery bay
- **Max duration:** 4 hours | **ID required:** Yes

#### Category 4 — Delivery Personnel
- **Approval required:** No | **OTP required:** No | **Blacklist check:** Yes (always)
- **Badge colour:** Orange header | **Routing:** Delivery bay only
- **Max duration:** 30 minutes | **ID required:** Yes
- **Special rule:** No access beyond delivery bay without escort

#### Category 5 — Service / Maintenance
- **Approval required:** Yes | **OTP required:** Yes | **Blacklist check:** Yes
- **Badge colour:** Grey header | **Routing:** Service entrance → assigned work area
- **Max duration:** 8 hours | **ID required:** Government + company service ID

#### Category 6 — Personal Guest
- **Approval required:** Yes | **OTP required:** Yes | **Blacklist check:** Yes
- **Badge colour:** Purple header | **Routing:** Main reception → employee's floor
- **Max duration:** 2 hours | **ID required:** Government ID

### 6.2 Routing Summary

| Category | Entry Point | Can Access | Escort Required |
|---------|------------|-----------|----------------|
| Client | Main reception | Meeting rooms | Optional |
| Interview | Main reception | HR waiting area only | No |
| Vendor | Main reception | Meeting rooms, work areas | Yes |
| Delivery | Delivery bay | Delivery bay only | Yes if going further |
| Service | Service entrance | Assigned work area only | Yes |
| Guest | Main reception | Employee's floor | Yes |

---

## 7. How the System Works — End to End

### 7.1 Flow 1 — Pre-Registration by Visitor (Standard)

**Step 1 — Registration link delivery.** Host employee generates a link from their dashboard via `POST /api/invites`, handled by `InviteController` in Spring Boot. A UUID token is persisted via `InviteTokenRepository` (Spring Data JPA). Link format: `https://yourdomain.com/register?host=EMP001&token=abc123`.

**Step 2 — Visitor opens the form.** Three steps: Details → Upload → Confirm. Pre-filled host comes from the token lookup.

**Step 3 — Visitor fills Step 1.** Name, mobile, email (optional), company, purpose, category, host employee, expected date/time.

**Step 4 — OTP verification.** Frontend calls `POST /api/otp/send`, handled by `OtpController`, which delegates to `TwilioVerifyService` (wraps the Twilio Java SDK). The Verification SID is cached via `RedisTemplate` with a 300-second TTL. On code entry, `POST /api/otp/verify` is checked against Twilio, and `otp_verified:{mobile}` is set in Redis for 600 seconds.

**Step 5 — Visitor fills Step 2.** Photo and government ID uploaded via `MultipartFile` handling in `VisitorController`. Files saved with UUID names under `/uploads/visitors/{yyyy}/{MM}/{dd}/`.

**Step 6 — Visitor fills Step 3.** Read-only summary, confirmation checkbox, submit.

**Step 7 — Duplicate check.** `VisitService.checkDuplicate()` runs a Spring Data JPA query before any entity is persisted.

**Step 8 — Blacklist check.** `BlacklistService.check()` queries Redis first via `RedisTemplate.opsForValue().get()`, falls back to `BlacklistRepository` (JPA) on a cache miss.

**Step 9 — Visit record created.** `Visitor` and `Visit` JPA entities persisted via their respective repositories, wrapped in a `@Transactional` service method so both succeed or both roll back together.

**Step 10 — Host employee notified.** `NotificationService` calls `TwilioWhatsAppService` and `SendGridEmailService` in the same transaction boundary (fire-and-forget via Spring's `@Async`, so the visitor's HTTP response isn't held up waiting on Twilio/SendGrid).

**Step 11 — Employee approves via WhatsApp.** Twilio POSTs to `WhatsAppWebhookController` at `/api/webhooks/twilio/whatsapp`. The controller validates the `X-Twilio-Signature` header using a custom `TwilioSignatureValidator` component before processing the body.

**Step 12 — QR pass generated and delivered.** `QrCodeService` (wrapping the ZXing library) builds the signed payload via `HmacUtil.sign()`, generates the PNG, and `SendGridEmailService` emails the visitor pass.

**Step 13 — Visitor arrives.** Reception opens the check-in screen, searches via `GET /api/visits/search`.

**Step 14 — Reception checks in.** `POST /api/checkin/{visitId}` persists a `CheckInOut` entity and publishes a Spring application event, which the `DashboardWebSocketHandler` picks up and broadcasts over STOMP to `/topic/admin`.

**Step 15 — Security scans at gate.** `POST /api/qr/verify` recomputes the HMAC and validates visit status.

**Step 16 — Visitor checks out.** `POST /api/checkout/{visitId}` updates the `CheckInOut` entity and triggers another WebSocket broadcast.

### 7.2 Flow 2 — Walk-In Visitor

Reception opens the dedicated Walk-In screen (separate Vue/HTML page hitting `POST /api/visits/walkin`). No OTP step is called. Blacklist check still runs via the same `BlacklistService` used in pre-registration — there is no separate code path for this, ensuring the rule can never accidentally be skipped. Receptionist ticks "Host confirmed verbally," which sets `walkinHostConfirmed=true` on the `CheckInOut` entity. Visit is created directly with `status=APPROVED`.

### 7.3 Flow 3 — Employee Rejection

Employee replies "REJECT {token}" on WhatsApp. `WhatsAppWebhookController` detects the REJECT keyword, creates a `PendingWhatsAppReply` entity with a 10-minute expiry, and sends a follow-up WhatsApp message asking for a reason. The next inbound webhook call from that employee's number is checked against any open `PendingWhatsAppReply` records before being treated as a new command.

### 7.4 Flow 4 — Blacklist Hit During Registration

On a blacklist hit, `BlacklistService` sets `Visit.status=BLOCKED`, publishes a `BlacklistHitEvent` (Spring's `ApplicationEventPublisher`), which two separate `@EventListener` methods react to independently: one sends the admin email via `SendGridEmailService`, the other broadcasts the WebSocket alert via `DashboardWebSocketHandler`. Decoupling these via Spring events means adding a third reaction (e.g., Slack notification) later requires zero changes to `BlacklistService` itself.

### 7.5 Flow 5 — QR Forgery Attempt

`QrVerificationService.verify()` recomputes the HMAC using `MessageDigest`-backed constant-time comparison (`java.security.MessageDigest.isEqual()`) to prevent timing attacks — the Java equivalent of Python's `hmac.compare_digest()`. On mismatch, an `AuditLogService.record()` call logs `QR_FORGERY_ATTEMPT`.

### 7.6 Flow 6 — Scheduled Daily Report Email

A `@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")` method in `DailyReportScheduler` triggers `ReportService.generateDailySummary()`, which runs a Spring Data JPA aggregate query, renders an HTML template via Thymeleaf, and sends it through `SendGridEmailService` to every active admin.

### 7.7 Flow 7 — Duplicate Visit Detection

`VisitService.checkDuplicate(mobile, expectedDate)` runs a derived JPA query method:
```java
Optional<Visit> findByVisitor_MobileAndExpectedDateAndStatusIn(
    String mobile, LocalDate date, List<VisitStatus> statuses);
```
This single line replaces what would otherwise be a hand-written SQL query — Spring Data JPA generates the query from the method name.

---

## 8. Core Features — Full Detail

### 8.1 Visitor Registration Form

Public endpoint, no Spring Security filter chain applied (configured via `SecurityFilterChain` with `.permitAll()` on `/api/visitors/register`, `/api/otp/**`).

**Invite token mechanism:** `InviteToken` JPA entity with `@Column(unique = true)` on the token field. Single-use enforced at the service layer — `InviteTokenService.consume()` checks `usedAt == null` before marking it used, inside a `@Transactional` block to prevent race conditions if two requests hit simultaneously.

**Form fields — Step 1, 2, 3:** unchanged from v3.0 specification (see Section 7.1 above for the full field list — categories, validation rules, and upload constraints are identical regardless of backend framework).

### 8.2 OTP Verification — Spring Boot Implementation

```java
@Service
public class TwilioVerifyService {

    @Value("${twilio.verify.service-sid}")
    private String verifyServiceSid;

    private final RedisTemplate<String, String> redisTemplate;

    public void sendOtp(String mobile) {
        String countKey = "otp_send_count:" + mobile;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count == 1) {
            redisTemplate.expire(countKey, Duration.ofHours(1));
        }
        if (count > 3) {
            throw new OtpLimitExceededException(mobile);
        }

        Verification verification = Verification.creator(
            verifyServiceSid, mobile, "sms"
        ).create();

        redisTemplate.opsForValue().set(
            "otp:" + mobile, verification.getSid(), Duration.ofMinutes(5)
        );
    }

    public boolean verifyOtp(String mobile, String code) {
        String attemptsKey = "otp_attempts:" + mobile;
        if (Boolean.TRUE.equals(redisTemplate.hasKey("otp_locked:" + mobile))) {
            throw new OtpLockedException(mobile);
        }

        VerificationCheck check = VerificationCheck.creator(verifyServiceSid)
            .setTo(mobile)
            .setCode(code)
            .create();

        if ("approved".equals(check.getStatus())) {
            redisTemplate.opsForValue().set(
                "otp_verified:" + mobile, "1", Duration.ofMinutes(10)
            );
            return true;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == 1) redisTemplate.expire(attemptsKey, Duration.ofMinutes(15));
        if (attempts >= 3) {
            redisTemplate.opsForValue().set(
                "otp_locked:" + mobile, "1", Duration.ofMinutes(15)
            );
        }
        return false;
    }
}
```

### 8.3 Blacklist Engine — Spring Boot Implementation

```java
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final BlacklistRepository blacklistRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BlacklistCheckResult check(String mobile, String idNumber) {
        String mobileKey = "blacklist_mobile:" + mobile;
        String idKey = "blacklist_id:" + idNumber;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(mobileKey))
            || Boolean.TRUE.equals(redisTemplate.hasKey(idKey))) {
            return BlacklistCheckResult.hit();
        }

        Optional<Blacklist> match = blacklistRepository
            .findFirstByMobileNumberOrIdNumberAndIsActiveTrue(mobile, idNumber);

        if (match.isPresent()) {
            redisTemplate.opsForValue().set(mobileKey,
                match.get().getId().toString(), Duration.ofHours(1));
            eventPublisher.publishEvent(new BlacklistHitEvent(match.get(), mobile));
            return BlacklistCheckResult.hit();
        }

        return BlacklistCheckResult.clear();
    }
}
```

### 8.4 WhatsApp Approval — Spring Boot Implementation

```java
@RestController
@RequestMapping("/api/webhooks/twilio")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final TwilioSignatureValidator signatureValidator;
    private final ApprovalService approvalService;

    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleReply(
            HttpServletRequest request,
            @RequestParam("From") String from,
            @RequestParam("Body") String body) {

        if (!signatureValidator.isValid(request)) {
            return ResponseEntity.status(403).build();
        }

        approvalService.processWhatsAppReply(from, body);
        return ResponseEntity.ok("<Response></Response>");
    }
}
```

### 8.5 WebSocket Dashboard — Spring Boot Implementation

**Configuration:**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/admin")
            .setAllowedOrigins("${app.frontend-url}")
            .withSockJS();
    }
}
```

**Broadcasting an event:**
```java
@Service
@RequiredArgsConstructor
public class DashboardNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyCheckedIn(Visit visit) {
        messagingTemplate.convertAndSend("/topic/admin",
            new CheckInEvent(visit.getId(), visit.getVisitor().getName(),
                visit.getEmployee().getName(), LocalDateTime.now()));
    }
}
```

**Frontend connects using STOMP over SockJS:**
```javascript
const socket = new SockJS('/ws/admin');
const stompClient = Stomp.over(socket);
stompClient.connect({ Authorization: 'Bearer ' + jwtToken }, () => {
    stompClient.subscribe('/topic/admin', (message) => {
        const event = JSON.parse(message.body);
        updateDashboard(event);
    });
});
```

### 8.6 Redis Caching Layer — Spring Boot Implementation

**Configuration (`application.yml`):**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 200ms
      lettuce:
        pool:
          max-active: 10
```

**Health check used by the fallback logic:**
```java
@Component
@RequiredArgsConstructor
public class RedisHealthChecker {

    private final RedisConnectionFactory connectionFactory;

    public boolean isAvailable() {
        try {
            connectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis unavailable — falling back to MySQL", e);
            return false;
        }
    }
}
```

---

## 9. Badge Design Specification

Unchanged from v3.0. Badge HTML is rendered server-side using **Thymeleaf** (Spring Boot's default templating engine) at `GET /api/badge/{visitId}`, returning a print-ready HTML page styled per the category colour table below.

### 9.1 Badge Header Colours by Category

| Category | Header Colour | Text Colour |
|---------|-------------|------------|
| Client / Business | #1B3F6E (Dark blue) | White |
| Interview Candidate | #1E6641 (Dark green) | White |
| Vendor / Supplier | #B45309 (Amber) | White |
| Delivery Personnel | #C05621 (Orange) | White |
| Service / Maintenance | #374151 (Dark grey) | White |
| Personal Guest | #5B21B6 (Purple) | White |

### 9.2 Badge Content Rules

- Visitor's full name printed bold, 18pt
- Government ID number never printed — only ID type shown
- Valid time = check-in time + category max duration
- QR code on badge is the same signed QR from the visitor pass
- Badge ID printed in monospace font
- "Must be worn visibly at all times" on every badge

---

## 10. Notification Templates

All templates unchanged from v3.0 — content is framework-agnostic. Rendered in Spring Boot via Thymeleaf email templates, passed to `SendGridEmailService`.

### 10.1–10.8 Templates

See Section 10 of VMS-PRD-003 for full template text (OTP SMS, WhatsApp approval request, email approval fallback, QR pass email, rejection email, expiry email, blacklist alert email, daily summary email). All templates apply unchanged — only the rendering engine (Thymeleaf instead of Python f-strings) differs.

---

## 11. Session and Draft Management

### 11.1 The Problem

Same as v3.0 — visitors lose form progress if they close the browser mid-registration.

### 11.2 Spring Boot Implementation

```java
@Service
@RequiredArgsConstructor
public class DraftService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveDraft(String sessionId, RegistrationDraftDto draft) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(draft);
        redisTemplate.opsForValue().set("draft:" + sessionId, json, Duration.ofHours(24));
    }

    public Optional<RegistrationDraftDto> getDraft(String sessionId) throws JsonProcessingException {
        String json = redisTemplate.opsForValue().get("draft:" + sessionId);
        if (json == null) return Optional.empty();
        return Optional.of(objectMapper.readValue(json, RegistrationDraftDto.class));
    }
}
```

The `sessionId` is set as an HttpOnly cookie via `ResponseCookie` in the controller layer:
```java
ResponseCookie cookie = ResponseCookie.from("vms_session", sessionId)
    .httpOnly(true).secure(true).sameSite("Strict")
    .maxAge(Duration.ofHours(24)).build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

---

## 12. Duplicate Visit Detection

### 12.1 Spring Data JPA Query

```java
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT v FROM Visit v WHERE v.visitor.mobile = :mobile " +
           "AND CAST(v.expectedDate AS date) = :date " +
           "AND v.status IN ('PENDING', 'APPROVED')")
    Optional<Visit> findDuplicateVisit(
        @Param("mobile") String mobile,
        @Param("date") LocalDate date
    );
}
```

### 12.2 Service Logic

```java
public VisitCreationResult registerVisit(VisitorRegistrationDto dto) {
    Optional<Visit> duplicate = visitRepository.findDuplicateVisit(
        dto.getMobile(), dto.getExpectedDate());

    if (duplicate.isPresent()) {
        return VisitCreationResult.duplicate(duplicate.get());
    }

    BlacklistCheckResult blacklistResult = blacklistService.check(
        dto.getMobile(), dto.getIdNumber());

    if (blacklistResult.isHit()) {
        return createBlockedVisit(dto);
    }

    return createNewVisit(dto);
}
```

### 12.3 Walk-In Duplicate Warning

Same `findDuplicateVisit()` method is reused in `WalkInController` — there is exactly one duplicate-check code path in the entire application, called from both the pre-registration and walk-in flows.

---

## 13. Signed QR Code Security

### 13.1 HMAC Signing — Java Implementation

```java
@Component
public class QrSigner {

    @Value("${app.qr-secret-key}")
    private String secretKey;

    public String sign(Long visitId, Long visitorId, LocalDate expectedDate) {
        String message = visitId + "|" + visitorId + "|" + expectedDate;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes());
            String hexHash = HexFormat.of().formatHex(hash);
            return message + "|" + hexHash;
        } catch (Exception e) {
            throw new QrSigningException(e);
        }
    }

    public boolean verify(String qrPayload) {
        String[] parts = qrPayload.split("\\|");
        if (parts.length != 4) return false;

        String message = parts[0] + "|" + parts[1] + "|" + parts[2];
        String receivedHash = parts[3];
        String expectedSigned = sign(Long.parseLong(parts[0]),
            Long.parseLong(parts[1]), LocalDate.parse(parts[2]));
        String expectedHash = expectedSigned.split("\\|")[3];

        // Constant-time comparison — prevents timing attacks
        return MessageDigest.isEqual(
            receivedHash.getBytes(), expectedHash.getBytes());
    }
}
```

### 13.2 QR Image Generation with ZXing

```java
@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrSigner qrSigner;

    public byte[] generateQrImage(Long visitId, Long visitorId, LocalDate date) throws Exception {
        String payload = qrSigner.sign(visitId, visitorId, date);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 400, 400);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}
```

### 13.3 What Happens on Failed Verification

Identical behaviour to v3.0 — `AuditLogService.record("QR_FORGERY_ATTEMPT", ...)` and a WebSocket `qr_invalid` broadcast via `DashboardNotifier`.

---

## 14. Scheduled Report Engine

### 14.1 Spring Boot `@Scheduled` Setup

```java
@Component
@RequiredArgsConstructor
public class DailyReportScheduler {

    private final ReportService reportService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendDailyReport() {
        reportService.generateAndSendDailyReport();
    }
}
```

Enabling the scheduler requires one annotation on the main application class:
```java
@SpringBootApplication
@EnableScheduling
public class VmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(VmsApplication.class, args);
    }
}
```

### 14.2 Report Query — Spring Data JPA

```java
@Query("SELECT new com.vms.dto.DailySummaryDto(" +
       "COUNT(v), " +
       "SUM(CASE WHEN c.checkOutTime IS NOT NULL THEN 1 ELSE 0 END), " +
       "SUM(CASE WHEN v.status = 'BLOCKED' THEN 1 ELSE 0 END), " +
       "SUM(CASE WHEN v.visitType = 'WALKIN' THEN 1 ELSE 0 END), " +
       "AVG(c.duration)) " +
       "FROM Visit v LEFT JOIN v.checkInOut c " +
       "WHERE v.expectedDate = :yesterday")
DailySummaryDto getDailySummary(@Param("yesterday") LocalDate yesterday);
```

### 14.3 Delivery

Rendered via Thymeleaf HTML template, sent through `SendGridEmailService` to all `User` entities where `role = ADMIN` and `isActive = true`. Retry logic implemented with Spring Retry (`@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 300000))`).

---

## 15. Non-Functional Requirements

| Category | Requirement | Detail |
|---------|-------------|--------|
| Performance | Page load | < 3 seconds on standard broadband |
| Performance | API response | < 500ms for 95% of requests |
| Performance | OTP delivery | SMS within 10 seconds for 95% of sends |
| Performance | WhatsApp delivery | Within 30 seconds for 95% of sends |
| Performance | WebSocket event | Received within 500ms of trigger |
| Performance | Blacklist lookup | < 10ms Redis hit, < 100ms DB fallback |
| Scalability | Concurrent users | 500 concurrent users without degradation |
| Security | Passwords | BCrypt via Spring Security, strength 12 |
| Security | API auth | JWT required on all endpoints except public form and Twilio webhook |
| Security | SQL injection | Spring Data JPA parameterized queries by default |
| Security | XSS | Input sanitized via Spring's built-in validation (`@Valid`, Hibernate Validator) |
| Security | File uploads | MIME type and extension validated, UUID filenames |
| Security | Transport | HTTPS required in production |
| Security | Twilio webhook | HMAC-SHA1 signature validation via custom `TwilioSignatureValidator` |
| Security | QR codes | HMAC-SHA256 signed payload, `MessageDigest.isEqual()` for constant-time comparison |
| Security | Redis | requirepass auth, localhost binding only |
| Reliability | Twilio down | System operates with email-only approvals |
| Reliability | Redis down | `RedisHealthChecker` triggers MySQL fallback path |
| Reliability | WebSocket down | Dashboard falls back to 30-second polling via `setInterval` |
| Maintainability | API docs | springdoc-openapi (Swagger UI) auto-generated from controller annotations |
| Maintainability | Code style | Lombok for boilerplate reduction, Spring Boot Actuator for health monitoring |

---

## 16. Technology Stack

| Layer | Technology | Why This Choice |
|-------|------------|----------------|
| Backend Framework | Spring Boot 3.2 | Locked decision — enterprise-grade, strong typing, mature ecosystem |
| Language | Java 17 | LTS version, required by Spring Boot 3.x |
| Build Tool | Maven | More tutorials and examples than Gradle for beginners |
| ORM | Spring Data JPA (Hibernate) | Eliminates hand-written SQL, derived query methods |
| Security | Spring Security 6 + JJWT | Industry standard, built-in BCrypt support |
| WebSocket | Spring WebSocket + STOMP + SockJS | Native Spring integration, automatic fallback to polling |
| Scheduler | Spring `@Scheduled` | Zero extra infrastructure, built into Spring context |
| Database | MySQL 8.0 | ACID compliance, relational integrity |
| Cache | Redis 7.0 via Spring Data Redis | Sub-millisecond reads, automatic TTL |
| OTP + WhatsApp | Twilio Java SDK | Official SDK handles request signing |
| Email | SendGrid Java SDK | Reliable deliverability, free tier |
| QR Code | ZXing (Zebra Crossing) | Most widely used Java QR library |
| Templating | Thymeleaf | Spring Boot's default template engine, used for badges and emails |
| API Docs | springdoc-openapi | Auto-generates Swagger UI from controller code |
| Boilerplate reduction | Lombok | `@Data`, `@RequiredArgsConstructor` reduce verbose Java code |
| Frontend | HTML5, Bootstrap 5, Vanilla JS | No build pipeline overhead within 20-day timeline |
| Version Control | GitHub + GitHub Actions CI/CD | Automated Maven build and test on push |

---

## 17. Spring Boot Project Structure

This is the exact folder layout the team should create on Day 11 before writing any business logic.

```
vms-backend/
├── src/main/java/com/vms/
│   ├── VmsApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── RedisConfig.java
│   │   └── CorsConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── OtpController.java
│   │   ├── VisitorController.java
│   │   ├── VisitController.java
│   │   ├── WalkInController.java
│   │   ├── CheckInController.java
│   │   ├── QrController.java
│   │   ├── BlacklistController.java
│   │   ├── AdminController.java
│   │   ├── ReportController.java
│   │   ├── InviteController.java
│   │   └── WhatsAppWebhookController.java
│   ├── service/
│   │   ├── TwilioVerifyService.java
│   │   ├── TwilioWhatsAppService.java
│   │   ├── SendGridEmailService.java
│   │   ├── BlacklistService.java
│   │   ├── VisitService.java
│   │   ├── QrCodeService.java
│   │   ├── QrSigner.java
│   │   ├── DraftService.java
│   │   ├── ReportService.java
│   │   ├── AuditLogService.java
│   │   └── DashboardNotifier.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── EmployeeRepository.java
│   │   ├── VisitorRepository.java
│   │   ├── VisitRepository.java
│   │   ├── BlacklistRepository.java
│   │   ├── CheckInOutRepository.java
│   │   ├── InviteTokenRepository.java
│   │   └── AuditLogRepository.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Employee.java
│   │   ├── Visitor.java
│   │   ├── Visit.java
│   │   ├── Approval.java
│   │   ├── CheckInOut.java
│   │   ├── Blacklist.java
│   │   ├── BlacklistCheckLog.java
│   │   ├── InviteToken.java
│   │   ├── PendingWhatsAppReply.java
│   │   └── AuditLog.java
│   ├── dto/
│   │   ├── VisitorRegistrationDto.java
│   │   ├── WalkInDto.java
│   │   ├── DailySummaryDto.java
│   │   └── ...
│   ├── exception/
│   │   ├── OtpLimitExceededException.java
│   │   ├── OtpLockedException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ...
│   └── scheduler/
│       └── DailyReportScheduler.java
├── src/main/resources/
│   ├── application.yml
│   ├── templates/          (Thymeleaf — badges, emails)
│   └── db/migration/       (Flyway scripts)
├── src/test/java/com/vms/  (JUnit + Mockito tests)
└── pom.xml
```

**Day 11 setup checklist:**
1. Generate project via `start.spring.io` with dependencies: Spring Web, Spring Data JPA, MySQL Driver, Spring Security, Spring Data Redis, Spring WebSocket, Validation, Lombok
2. Create the folder structure above
3. Configure `application.yml` with placeholder DB/Redis credentials
4. Write one dummy `GET /api/health` endpoint and confirm the app starts
5. Do not write business logic until the skeleton runs cleanly

---

## 18. User Stories

| ID | Role | Story | Priority |
|----|------|-------|---------|
| US-01 | Visitor | Register my visit online with OTP phone verification before arriving | HIGH |
| US-02 | Visitor | Get a signed QR pass on my email that cannot be forged | HIGH |
| US-03 | Visitor | Return to my half-filled form and continue from where I left off | MEDIUM |
| US-04 | Visitor | Be told clearly if I already have a visit for today instead of creating a duplicate | MEDIUM |
| US-05 | Employee | Approve a visitor by replying to a WhatsApp message in one tap | HIGH |
| US-06 | Employee | Reject a visitor and provide a reason via WhatsApp | HIGH |
| US-07 | Employee | Generate a registration invite link to send to my guest | MEDIUM |
| US-08 | Receptionist | Check in a pre-registered visitor in under 60 seconds | HIGH |
| US-09 | Receptionist | Create a walk-in visit and be warned if blacklisted | HIGH |
| US-10 | Admin | See every visitor check-in on my dashboard in real time | HIGH |
| US-11 | Admin | Add a person to the blacklist with a reason | HIGH |
| US-12 | Admin | Receive a daily 9 AM email with yesterday's visitor summary | MEDIUM |
| US-13 | Security | Scan a visitor's QR code and be told instantly if valid or forged | HIGH |

---

## 19. Integration Points

### 19.1 Twilio Verify — Java SDK

```xml
<dependency>
  <groupId>com.twilio.sdk</groupId>
  <artifactId>twilio</artifactId>
  <version>10.4.1</version>
</dependency>
```

Initialization in a `@PostConstruct` method or `@Configuration` bean:
```java
@Configuration
public class TwilioConfig {
    @Value("${twilio.account-sid}") private String accountSid;
    @Value("${twilio.auth-token}") private String authToken;

    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
    }
}
```

### 19.2 Twilio WhatsApp — Java SDK

```java
Message.creator(
    new PhoneNumber("whatsapp:" + employeeMobile),
    new PhoneNumber("whatsapp:+14155238886"),
    messageBody
).create();
```

### 19.3 SendGrid — Java SDK

```xml
<dependency>
  <groupId>com.sendgrid</groupId>
  <artifactId>sendgrid-java</artifactId>
  <version>4.10.1</version>
</dependency>
```

```java
Email from = new Email("noreply@yourdomain.com");
Email to = new Email(visitorEmail);
Content content = new Content("text/html", htmlBody);
Mail mail = new Mail(from, subject, to, content);
SendGrid sg = new SendGrid(sendGridApiKey);
sg.api(new Request() {{
    setMethod(Method.POST);
    setEndpoint("mail/send");
    setBody(mail.build());
}});
```

### 19.4 Redis — Spring Data Redis

Already shown in Section 8.6. Connection pooling handled automatically via Lettuce (Spring Boot's default Redis client).

---

## 20. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Spring Boot setup overhead eats into Day 11-12 | High | High | Use Spring Initializr, follow the locked project structure in Section 17, do not deviate |
| Spring WebSocket/STOMP configuration confusion | Medium | High | Budget all of Day 16 exclusively for this; do not mix with other feature work |
| Team unfamiliar with Spring Data JPA query derivation | Medium | Medium | Use simple derived query methods (Section 12.1) before reaching for `@Query` |
| Twilio trial credit exhausted | Medium | High | Monitor credit daily; ~300 OTPs on $15 credit |
| Employee does not have WhatsApp | Medium | Medium | Email fallback always sent via `@Async` in the same service call |
| QR_SECRET_KEY exposed in code | Low | Critical | Store only in `application.yml` via env var substitution, never commit `.env` |
| Redis single node failure | Low | High | `RedisHealthChecker` + MySQL fallback tables |
| Maven dependency version conflicts | Low | Medium | Use Spring Boot's managed dependency versions — do not manually pin library versions unless necessary |
| Scope creep past Day 10 | High | High | Freeze scope after this document is signed |

---

## 21. Out of Scope

| Feature | Why deferred |
|---------|-------------|
| Mobile native app (iOS/Android) | Separate development effort |
| Biometric verification | Hardware dependency |
| Multi-branch / multi-tenant | Architectural change |
| Physical access control integration | Hardware dependency |
| WhatsApp interactive buttons | Requires Meta Business verification |
| Facial recognition ID matching | AI/ML component |
| Microservices split | Single Spring Boot monolith is correct for this scope and timeline |
| Kubernetes / container orchestration | Unnecessary infrastructure for a 20-day internship deployment |

---

## 22. 20-Day Timeline

| Day | Phase | Task | Deliverable |
|-----|-------|------|-------------|
| 1 | Planning | Project understanding | Day 1 report |
| 2 | Planning | Requirement gathering, BRD | BRD, Day 2 report |
| 3 | Planning | Use case + user flow diagrams | UML diagrams |
| 4 | Planning | ERD, data dictionary | ERD |
| 5 | Planning | PRD v4, SRS v4 finalised | This document |
| 6 | Design | Wireframes for all screens | Wireframe doc |
| 7-9 | Design | HTML/CSS for all screens | Screen HTML files |
| 10 | Design | MySQL schema, Redis setup | Database ready |
| 11 | Backend | Spring Initializr setup, project skeleton, Spring Security config | Auth module + health check |
| 12 | Backend | Visitor registration API, file upload, Twilio OTP integration | Registration API |
| 13 | Backend | Blacklist engine, duplicate detection (JPA) | Blacklist module |
| 14 | Backend | Approval workflow, Twilio WhatsApp webhook | Approval module |
| 15 | Backend | QR pass generation (ZXing + HMAC), SendGrid email | QR + email module |
| 16 | Backend | Spring WebSocket + STOMP setup — dedicated day | Real-time dashboard |
| 17 | Backend | Check-in/out APIs, walk-in module | Check-in module |
| 18 | Backend | Reports, `@Scheduled` daily email, admin APIs | Reports + scheduler |
| 19 | Testing | End-to-end testing of all flows | Test report |
| 20 | Delivery | Bug fixes, documentation, final presentation | Final presentation |

---

*VMS-PRD-004 | v4.0 | June 16, 2025 | 20-Day Software Development Internship Program*
*This document supersedes VMS-PRD-001, VMS-PRD-002, and VMS-PRD-003. Backend locked to Spring Boot.*
