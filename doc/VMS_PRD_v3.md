# Product Requirements Document (PRD) v3.0
**Project:** Visitor Management System (VMS)
**Document ID:** VMS-PRD-003
**Version:** v3.0
**Date:** June 12, 2025
**Architecture:** Final — OTP + Blacklist + WhatsApp + WebSocket + Redis + Signed QR + Duplicate Detection + Scheduled Reports
**Status:** FINAL — Complete with all edge cases, flows, and implementation detail

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
17. [User Stories](#17-user-stories)
18. [Integration Points](#18-integration-points)
19. [Risks and Mitigations](#19-risks-and-mitigations)
20. [Out of Scope](#20-out-of-scope)
21. [20-Day Timeline](#21-20-day-timeline)

---

## 1. Executive Summary

The Visitor Management System (VMS) v3.0 is the final, complete specification for a web-based security and visitor lifecycle platform built as part of a 20-day internship program. It replaces all paper-based visitor logs with a fully digital, verified, and auditable system.

Version 3.0 builds on the v2.0 architecture (OTP verification, blacklist engine, WhatsApp approvals, WebSocket dashboard, Redis cache) and adds the following missing pieces identified during document review:

- **Visitor category definitions** with category-specific routing rules
- **Complete end-to-end flow documentation** for every scenario
- **Badge design specification** defining exactly what prints at reception
- **Full notification templates** for every email and WhatsApp message the system sends
- **Session and draft management** for the public registration form
- **Duplicate visit detection** to prevent the same visitor registering twice for the same day
- **Signed QR codes** using HMAC-SHA256 to prevent forgery
- **Scheduled report engine** with daily 9 AM summary emails to admin
- **CORS policy**, **database index definitions**, **pagination specs**, **file storage security**, and **Redis health check** — all previously missing

This document is the single source of truth for everything that will be built.

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
| Duplicate registrations possible | Same-day duplicate detection before record creation |
| Admin must manually pull reports | Scheduled daily report email at 9 AM |
| No draft save on registration form | Redis-based form draft with 24-hour TTL |
| Notification content undefined | Full templates for every message type |
| Badge content undefined | Exact badge layout spec per category |
| Registration link delivery undefined | Host employee generates and shares link |
| No delivery routing for visitor types | Routing rules per visitor category |

---

## 3. Product Vision

> Build a smart, secure, push-based Visitor Management System where every visitor is verified before entry, every threat is blocked automatically, every approval happens in seconds, every admin sees the building's occupancy in real time, every badge is printed in under 60 seconds, and every manager receives yesterday's visitor summary before they start their morning.

---

## 4. Objectives

1. Verify visitor phone identity via OTP before any approval request is sent
2. Cross-check every visitor's phone and government ID against the blacklist before approval
3. Detect and block duplicate visit registrations for the same visitor on the same day
4. Deliver employee approval requests via WhatsApp (primary) and email (fallback)
5. Issue cryptographically signed QR passes that cannot be forged or replicated
6. Push live visitor events to the admin dashboard via WebSocket in real time
7. Store all ephemeral data (OTP, sessions, drafts, blacklist cache) in Redis
8. Email a daily visitor summary report to all admin users every morning at 9 AM
9. Reduce pre-registered visitor check-in time to under 60 seconds at reception
10. Route visitors to the correct check-in point based on their visit category

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

Every visitor must select one of these six categories during registration. The category determines the approval flow, badge colour, and routing destination.

#### Category 1 — Client / Business Meeting

- **Who:** External business contacts, customers, partners coming for a meeting
- **Approval required:** Yes — host employee must approve
- **OTP required:** Yes
- **Blacklist check:** Yes
- **Badge colour:** Blue header
- **Routing:** Main reception → escort by host employee
- **Max visit duration:** 8 hours
- **ID required:** Yes

#### Category 2 — Interview Candidate

- **Who:** Job applicants attending an interview
- **Approval required:** Yes — HR employee must approve
- **OTP required:** Yes
- **Blacklist check:** Yes
- **Badge colour:** Green header
- **Routing:** Main reception → HR waiting area
- **Max visit duration:** 4 hours
- **ID required:** Yes
- **Special rule:** Interview candidates get a waiting area pass, not a floor access pass

#### Category 3 — Vendor / Supplier

- **Who:** External vendors delivering goods or services on a contract basis
- **Approval required:** Yes — procurement or operations employee must approve
- **OTP required:** Yes
- **Blacklist check:** Yes
- **Badge colour:** Yellow header
- **Routing:** Goods entry / delivery bay — not main reception
- **Max visit duration:** 4 hours
- **ID required:** Yes

#### Category 4 — Delivery Personnel

- **Who:** Courier, parcel, or goods delivery staff
- **Approval required:** No — reception can approve directly for standard deliveries
- **OTP required:** No — delivery personnel change daily; OTP impractical
- **Blacklist check:** Yes — still runs
- **Badge colour:** Orange header
- **Routing:** Delivery bay / goods receipt area
- **Max visit duration:** 30 minutes
- **ID required:** Yes (company delivery ID or government ID)
- **Special rule:** Delivery personnel do not proceed beyond the delivery bay without escort

#### Category 5 — Service / Maintenance

- **Who:** IT support, electricians, plumbers, AC technicians, security contractors
- **Approval required:** Yes — facilities or IT manager must approve
- **OTP required:** Yes
- **Blacklist check:** Yes
- **Badge colour:** Grey header
- **Routing:** Service entrance → accompanied to work area by facilities staff
- **Max visit duration:** 8 hours
- **ID required:** Yes — company service ID required in addition to government ID
- **Special rule:** Must sign in to the specific area they are working in

#### Category 6 — Personal Guest

- **Who:** Family members, personal guests of an employee
- **Approval required:** Yes — the employee they are visiting must approve
- **OTP required:** Yes
- **Blacklist check:** Yes
- **Badge colour:** Purple header
- **Routing:** Main reception → escorted by employee
- **Max visit duration:** 2 hours
- **ID required:** Government ID

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

This section describes every scenario from the first action to the last. Read this section before building anything.

### 7.1 Flow 1 — Pre-Registration by Visitor (Standard)

This is the primary and preferred flow. The visitor registers before arriving.

**Step 1 — Registration link delivery**
The host employee generates a registration link from their dashboard by clicking "Invite Visitor". The system generates a unique pre-filled URL: `https://yourdomain.com/register?host=EMP001&token=abc123`. The employee copies this link and sends it to the visitor via any channel (email, WhatsApp, SMS — outside the system).

**Step 2 — Visitor opens the form**
The visitor opens the link on their phone or laptop. The host employee name and department are pre-filled from the URL token. The form shows three steps: Details → Upload → Confirm.

**Step 3 — Visitor fills Step 1 (Details)**
Visitor enters: full name, mobile number, email (optional), company name, purpose of visit, visitor category, expected date and time. Host employee is pre-filled but can be changed.

**Step 4 — OTP verification**
Visitor clicks "Send OTP". System calls Twilio Verify. Visitor enters 6-digit code. System verifies against Twilio. On success, phone is marked verified in Redis for 10 minutes. The form advances to Step 2.

**Step 5 — Visitor fills Step 2 (Upload)**
Visitor uploads their photo (or uses webcam) and government ID document. Files are validated for type and size client-side before upload. On upload, files are saved to the server's `uploads/visitors/{date}/` directory with a UUID filename. The original filename is never used (security measure). File paths are stored temporarily in the user's session.

**Step 6 — Visitor fills Step 3 (Confirm)**
Visitor sees a summary of all entered details. Photo shows as a circle preview. ID shows as filename with file size. All details are read-only here. A checkbox: "I confirm the above details are correct and accurate." Submit button is inactive until checkbox is ticked.

**Step 7 — Duplicate check**
Before saving anything, the system checks: does a visit record exist for this mobile number with status PENDING or APPROVED on this calendar date? If yes — the system shows a message: "You already have a visit scheduled for today. Please check your email for your existing QR pass." No new record is created.

**Step 8 — Blacklist check**
If no duplicate — system runs blacklist check against mobile number and ID number. Redis is checked first (sub-millisecond). If Redis misses, MySQL is queried. If a match is found — visit is saved as BLOCKED, visitor receives a generic message, admin receives a WebSocket alert and email.

**Step 9 — Visit record created**
If blacklist is clear — Visitor record is saved to the Visitors table. Visit record is saved to the Visits table with status PENDING. The pre-registration invite token is marked as used.

**Step 10 — Host employee notified**
System calls Twilio WhatsApp API and sends an approval message to the host employee's mobile. Simultaneously sends a fallback email via SendGrid. Both contain the visitor's name, company, category, purpose, expected date/time, and an approval token.

**Step 11 — Employee approves via WhatsApp**
Employee replies "APPROVE {token}" on WhatsApp. Twilio delivers the reply to the VMS webhook endpoint. VMS validates the HMAC-SHA1 signature. VMS validates the token. Visit status updated to APPROVED.

**Step 12 — QR pass generated and delivered**
System generates a QR pass. The QR payload is: `{visitId}|{visitorId}|{date}|{hmac_signature}`. The HMAC is computed using `HMAC-SHA256(secret_key, visitId + visitorId + date)`. The QR code image is generated using the python-qrcode or qrcode.js library. The pass is rendered as an HTML email with the QR image, visitor photo, visit details, and pass ID. Email is sent via SendGrid to the visitor.

**Step 13 — Visitor arrives**
Visitor arrives and shows QR pass to receptionist or security. Receptionist opens the check-in screen and scans or searches by pass ID. System loads the visitor card.

**Step 14 — Reception checks in**
Receptionist verifies the visitor's face against the photo on screen. Clicks "Check In". System records check-in timestamp in CheckInOut table. WebSocket emits `visitor_checked_in` event to all admin sessions. Badge is available to print.

**Step 15 — Security scans at gate (optional)**
If the building has a security gate, the security officer scans the QR using the QR scanner screen. System validates the QR signature. Confirms visit is APPROVED and CHECKED_IN. Shows visitor photo, name, host, and clearance status. Officer clicks "Allow Entry" or "Deny".

**Step 16 — Visitor checks out**
On departure, receptionist searches visitor and clicks "Check Out". System records check-out timestamp. Duration is calculated. Visit status updated. WebSocket emits `visitor_checked_out` event.

---

### 7.2 Flow 2 — Walk-In Visitor (No Prior Registration)

Used when a visitor arrives without a pre-registered appointment.

**Step 1 — Visitor arrives at reception**
Receptionist opens the Walk-In screen from the sidebar. This screen is separate from the pre-registration check-in screen.

**Step 2 — Receptionist fills visitor details**
Receptionist enters: visitor full name, mobile number, company, purpose of visit, visitor category. Selects host employee from the dropdown.

**Step 3 — Blacklist check runs automatically**
As soon as the receptionist enters the mobile number and tabs away from the field, the system silently queries the blacklist. If a hit is found, a red alert banner appears immediately: "This visitor is on the security blacklist. Do not proceed. Contact admin." The form is disabled.

**Step 4 — Receptionist calls host employee**
Receptionist calls the host employee by internal phone to confirm they are expecting this visitor. Once confirmed, receptionist ticks the "Host confirmed verbally" checkbox. The checkbox label shows the employee name: "Priya Suresh (Engineering) confirmed verbally".

**Step 5 — System creates visit record**
System creates a Visitor record and a Visit record with status APPROVED and type WALKIN. No OTP verification is done — the receptionist's physical presence serves as identity confirmation. No WhatsApp notification is sent for walk-ins.

**Step 6 — Check-in and badge print**
Receptionist clicks "Check In and Print Badge". System records check-in, generates a badge, and opens the print dialog. Walk-in visitors do not get a QR pass via email.

---

### 7.3 Flow 3 — Employee Rejection

**Step 1** — Employee receives WhatsApp message with visit request.

**Step 2** — Employee replies "REJECT {token}".

**Step 3** — VMS webhook receives and validates the reply. VMS sends a follow-up WhatsApp message to the employee: "Please reply with the reason for rejection (e.g. 'Not expecting this visitor' or 'Wrong date')."

**Step 4** — Employee sends the reason as a free-text reply. VMS captures this as the rejection reason. If no reply within 10 minutes, reason is stored as "No reason provided".

**Step 5** — Visit status set to REJECTED. Visitor receives an email: "Your visit request has been declined. Please contact {HostEmployeeName} at {HostEmployeeEmail} to reschedule."

---

### 7.4 Flow 4 — Blacklist Hit During Registration

**Step 1** — Visitor completes registration form including OTP verification.

**Step 2** — On form submit, system runs blacklist check. Match found.

**Step 3** — Visit record saved with status BLOCKED. Visitor receives: "Your request has been received. You will be notified by the organisation." (Generic — does not reveal they are blacklisted.)

**Step 4** — Admin receives an immediate WebSocket alert on the dashboard: red banner — "Security Alert — Blacklist hit. {VisitorName} attempted registration at {time}."

**Step 5** — Admin also receives an email alert with full details: visitor name, mobile (masked), ID type, matched blacklist entry reason.

**Step 6** — Event is logged to AuditLog with action `BLACKLIST_HIT`.

---

### 7.5 Flow 5 — QR Forgery Attempt

**Step 1** — Someone attempts to use a forged or expired QR code at the security gate or reception.

**Step 2** — System decodes the QR payload: `{visitId}|{visitorId}|{date}|{hmac}`.

**Step 3** — System recomputes `HMAC-SHA256(secret_key, visitId + visitorId + date)` and compares it to the `hmac` in the payload.

**Step 4** — If the HMAC does not match — QR is invalid. Screen shows: "Invalid pass — signature verification failed." Security officer denies entry. Event logged to AuditLog as `QR_FORGERY_ATTEMPT`.

**Step 5** — If HMAC matches but visit is EXPIRED or CHECKED_OUT — screen shows: "Pass has expired." Entry denied.

**Step 6** — If HMAC matches and visit is APPROVED but not yet CHECKED_IN — entry allowed.

---

### 7.6 Flow 6 — Scheduled Daily Report Email

**Step 1** — Every day at 9:00 AM, the system's scheduled job (APScheduler for Flask / Spring @Scheduled for Spring Boot) triggers the daily report generator.

**Step 2** — System queries: all visits from the previous calendar day (yesterday 00:00 to 23:59).

**Step 3** — Report is compiled: total visitors, checked-in count, checked-out count, pending count, blocked count, average visit duration, busiest hour, department breakdown.

**Step 4** — Report is rendered as an HTML email using a predefined template.

**Step 5** — Email is sent via SendGrid to all active admin users.

**Step 6** — If no visits occurred yesterday, the email still sends with "No visitors recorded yesterday."

---

### 7.7 Flow 7 — Duplicate Visit Detection

**Step 1** — Visitor submits registration form (post OTP, post blacklist clearance).

**Step 2** — System queries: `SELECT VisitID FROM Visits WHERE VisitorID IN (SELECT VisitorID FROM Visitors WHERE Mobile = ?) AND DATE(ExpectedDate) = DATE(?) AND Status IN ('PENDING', 'APPROVED')`.

**Step 3** — If a record is found — system does not create a new visit. Returns HTTP 200 with: "You already have a visit scheduled for today (Visit ID: VMS-2025-XXXX). Please check your email for your existing QR pass. If you need to change the time, contact your host."

**Step 4** — If no record found — proceed with visit creation normally.

---

## 8. Core Features — Full Detail

### 8.1 Visitor Registration Form

**URL:** `https://yourdomain.com/register` (public, no login required)

**Pre-filled URL format:** `https://yourdomain.com/register?host={employeeId}&token={inviteToken}`

**The invite token is:**
- Generated by the host employee from their dashboard
- Stored in an `InviteTokens` table with: token (UUID), EmployeeID, CreatedAt, UsedAt, ExpiresAt (48 hours)
- Single-use — marked as used when the registration form is submitted
- If the token is expired or already used, the form loads without the pre-filled host but still works

**Form fields — Step 1 (Details):**

| Field | Type | Required | Validation |
|-------|------|---------|-----------|
| Full Name | Text | Yes | 2–100 characters, letters and spaces only |
| Mobile Number | Tel | Yes | E.164 format, OTP verified before proceeding |
| Email Address | Email | No | Valid email format if provided |
| Company / Organisation | Text | No | Max 150 characters |
| Visitor Category | Select | Yes | One of 6 defined categories |
| Purpose of Visit | Text | Yes | 10–255 characters, free text |
| Host Employee | Searchable dropdown | Yes | Must select from active employees |
| Expected Date | Date picker | Yes | Must be today or a future date |
| Expected Time | Time picker | Yes | Must be within business hours (8 AM–8 PM) |

**Form fields — Step 2 (Upload):**

| Field | Type | Required | Validation |
|-------|------|---------|-----------|
| Visitor Photo | File / Webcam | Yes | JPEG or PNG, max 2MB, min 200×200px |
| Government ID | File | Yes | JPEG, PNG, or PDF, max 5MB |
| ID Type | Select | Yes | Aadhaar, Passport, Driver License, Company ID |

**Form fields — Step 3 (Confirm):**
- Read-only summary of all entered details
- Photo shown as circle thumbnail
- Confirmation checkbox (required)
- Submit button

---

### 8.2 OTP Verification

**How it works — step by step:**

1. Visitor enters mobile number and clicks "Send OTP"
2. Frontend sends `POST /api/otp/send` with `{mobile: "+919876543210"}`
3. Backend checks Redis key `otp_send_count:{mobile}` — if >= 3, return 429 error
4. Backend calls Twilio: `POST https://verify.twilio.com/v2/Services/{VERIFY_SID}/Verifications` with `To=+919876543210&Channel=sms`
5. Twilio sends a 6-digit code to the visitor's phone — message reads: "Your VMS verification code is 483912. Valid for 5 minutes."
6. Backend stores the Twilio Verification SID in Redis: `SET otp:{mobile} {sid} EX 300`
7. Frontend shows the 6-box OTP input and starts a 5-minute countdown timer
8. Visitor enters digits — each box auto-advances to the next on input
9. On 6th digit entered, frontend auto-calls `POST /api/otp/verify` with `{mobile, code}`
10. Backend calls Twilio: `POST https://verify.twilio.com/v2/Services/{VERIFY_SID}/VerificationChecks` with `VerificationSid={sid}&Code=483912`
11. If Twilio returns `status: approved` — backend sets `SET otp_verified:{mobile} 1 EX 600` — frontend shows green checkmark and advances to next field
12. If Twilio returns `status: pending` — backend increments `otp_attempts:{mobile}`, returns error, frontend shows "Incorrect code. X attempts remaining."
13. After 3 failures — backend sets `SET otp_locked:{mobile} 1 EX 900` — frontend shows "Too many attempts. Try again in 15 minutes."

---

### 8.3 Blacklist Engine

**How it works — step by step:**

1. Visitor submits the registration form
2. Backend extracts `mobile` and `idNumber` from the form data
3. Backend checks Redis: `GET blacklist_mobile:{mobile}` — if exists, it is a HIT
4. If Redis miss — backend queries MySQL: `SELECT BlacklistID, Reason FROM Blacklist WHERE (MobileNumber = ? OR IDNumber = ?) AND IsActive = TRUE LIMIT 1`
5. If MySQL returns a row — backend sets `SET blacklist_mobile:{mobile} {BlacklistID} EX 3600` for future cache hits
6. **On HIT:** backend saves visit with status BLOCKED, returns generic 200 to visitor, sends WebSocket event `blacklist_hit` to admin room, sends email alert to all admins
7. **On MISS:** backend proceeds to create Visitor and Visit records normally

**How admin adds to blacklist:**

1. Admin goes to Blacklist Management screen
2. Enters mobile number (required) or ID number (required — at least one)
3. Enters visitor name (optional — for reference)
4. Enters reason (required — e.g. "Security incident June 5, 2025 — damaged property")
5. Clicks "Add to Blacklist"
6. Backend saves to Blacklist table, immediately sets Redis keys `blacklist_mobile:{mobile}` and `blacklist_id:{idNumber}`
7. AuditLog records: action=BLACKLIST_ADD, performedBy=AdminUserID, details=reason

**How admin deactivates a blacklist entry:**

1. Admin finds the entry in the blacklist table
2. Clicks "Deactivate" — a confirmation dialog appears: "Are you sure? This will allow {name/mobile} to register again."
3. Admin confirms — backend sets `IsActive=FALSE` in Blacklist table, sets `DeactivatedAt=NOW()`
4. Backend immediately deletes Redis keys: `DEL blacklist_mobile:{mobile}`, `DEL blacklist_id:{idNumber}`
5. AuditLog records: action=BLACKLIST_DEACTIVATE

---

### 8.4 WhatsApp Approval — How It Works

**How the approval message is sent:**

1. Visit record created with status PENDING
2. Backend generates a unique approval token: `UUID4()` stored in `Visits.ApprovalToken`
3. Backend constructs WhatsApp message body:
```
VMS Visit Request

Visitor: Rajesh Kumar (TCS Ltd.)
Category: Client Meeting
Purpose: Quarterly review discussion
Date: Jun 10, 2025 at 11:00 AM

To APPROVE reply: APPROVE abc123xyz
To REJECT reply: REJECT abc123xyz

This request expires in 2 hours.
```
4. Backend calls Twilio: `POST https://api.twilio.com/2010-04-01/Accounts/{SID}/Messages` with `From=whatsapp:+14155238886&To=whatsapp:{employeeMobile}&Body={message}`
5. Simultaneously backend calls SendGrid to send fallback email with same information and a clickable link

**How the employee reply is received:**

1. Employee replies "APPROVE abc123xyz" on WhatsApp
2. Twilio sends HTTP POST to `https://yourdomain.com/api/webhooks/twilio/whatsapp` with form data including `Body` and `From`
3. VMS receives the POST — first validates the Twilio signature:
   - Gets `X-Twilio-Signature` header
   - Computes `HMAC-SHA1(TWILIO_AUTH_TOKEN, full_url + sorted_params)`
   - Compares computed value against header — if no match, return 403
4. If signature valid — parses `Body` field
5. Extracts token from body using regex: `r'(APPROVE|REJECT)\s+([a-zA-Z0-9]+)'`
6. Queries Visits table: `SELECT * FROM Visits WHERE ApprovalToken = ? AND Status = 'PENDING'`
7. If found — updates status to APPROVED, records approvedAt timestamp, records channel=WHATSAPP
8. Triggers QR pass generation and email delivery

---

### 8.5 WebSocket Real-Time Dashboard

**How it works — step by step:**

1. Admin logs in. The browser loads the dashboard page
2. Dashboard JavaScript connects to the WebSocket server: `const socket = io('https://yourdomain.com', {auth: {token: jwt_token}})`
3. Server receives the connection — validates the JWT from the auth object. If invalid, emits error and disconnects
4. If valid — server joins the socket to the `admin_room`: `socket.join('admin_room')` (Flask-SocketIO) or subscription to `/topic/admin` (Spring Boot STOMP)
5. Every time a significant event happens (check-in, check-out, blacklist hit, approval), the backend function that handles that event also emits to the room:
   ```python
   # Flask-SocketIO example
   socketio.emit('visitor_checked_in', {
       'visitId': visit.id,
       'visitorName': visitor.name,
       'hostName': employee.name,
       'checkInTime': datetime.now().isoformat(),
       'department': employee.department
   }, room='admin_room')
   ```
6. The admin dashboard JavaScript listens for these events and updates the UI without a page reload:
   ```javascript
   socket.on('visitor_checked_in', (data) => {
       updateActiveCount(+1);
       addRowToLiveTable(data);
       addEventToFeed('check-in', data);
   });
   ```
7. If the connection drops — Socket.IO client automatically attempts reconnection with backoff: 1s, 2s, 4s, 8s, 16s, 30s (cap), up to 10 attempts
8. On reconnection — client requests a full state refresh: `socket.emit('request_full_state')` — server responds with current dashboard KPIs

---

### 8.6 Redis Caching Layer

**How Redis is set up:**

Redis is installed on the same server as the application (localhost). It is not exposed to external networks. Authentication is enabled with `requirepass`.

**Connection in Flask:**
```python
import redis
r = redis.Redis.from_url(os.environ['REDIS_URL'], decode_responses=True,
                         socket_connect_timeout=2, socket_timeout=0.2)
```

**Health check — how the system detects Redis failure:**
```python
def redis_health_check():
    try:
        r.ping()
        return True
    except redis.ConnectionError:
        logger.warning("Redis unavailable — falling back to MySQL")
        return False
```

This check runs at the start of every OTP and blacklist operation. If it returns False, the system routes to the MySQL fallback path automatically.

**What happens when Redis is down:**

| Operation | Normal path | Fallback path |
|---------|------------|--------------|
| OTP store/verify | Redis `otp:{mobile}` | MySQL `OTPFallback` table |
| Session | Redis `session:{hash}` | Server memory store |
| Blacklist check | Redis `blacklist_mobile:{mobile}` | MySQL `Blacklist` table directly |
| Form draft | Redis `draft:{sessionId}` | Not saved — form state lost |

---

## 9. Badge Design Specification

Every check-in produces a printable visitor badge. The badge is A6 size (148mm × 105mm), rendered as an HTML page and printed via the browser's print dialog.

### 9.1 Badge Layout

```
┌─────────────────────────────────────┐
│  [CATEGORY COLOUR HEADER]           │
│  ACME CORP — VISITOR               │
│  [Category name]                    │
├────────────┬────────────────────────┤
│            │  VISITOR NAME          │
│  [PHOTO]   │  Company Name          │
│  80×80px   │                        │
│            │  Host: Priya Suresh    │
│            │  Dept: Engineering     │
├────────────┴────────────────────────┤
│  Date: Jun 10, 2025                 │
│  Valid: 11:00 AM – 07:00 PM         │
│  ID: VMS-2025-0610-00847            │
├─────────────────────────────────────┤
│  [QR CODE 60×60px]  [BARCODE]       │
│                                     │
│  ⚠ Must be worn visibly at all times│
└─────────────────────────────────────┘
```

### 9.2 Badge Header Colours by Category

| Category | Header Colour | Text Colour |
|---------|-------------|------------|
| Client / Business | #1B3F6E (Dark blue) | White |
| Interview Candidate | #1E6641 (Dark green) | White |
| Vendor / Supplier | #B45309 (Amber) | White |
| Delivery Personnel | #C05621 (Orange) | White |
| Service / Maintenance | #374151 (Dark grey) | White |
| Personal Guest | #5B21B6 (Purple) | White |

### 9.3 Badge Content Rules

- Visitor's full name is printed in bold, 18pt font
- Government ID number is **never** printed on the badge — only ID type (e.g. "Aadhaar verified")
- Valid time is calculated as: check-in time + category max duration (e.g. Client = 8 hours)
- QR code on badge is the same signed QR code from the visitor pass
- Badge ID (VMS-YYYY-MMDD-NNNNN) is printed in monospace font at the bottom
- If visitor has no photo on file — a silhouette placeholder is used
- "Must be worn visibly at all times" is printed on every badge regardless of category

---

## 10. Notification Templates

Every message the system sends is defined here. No other message content should be used.

### 10.1 OTP SMS (Twilio Verify — auto-generated)

Twilio Verify sends its own OTP message. The Verify service friendly name should be set to "VMS". The visitor will receive:
```
Your VMS verification code is 483912. This code expires in 5 minutes.
```

### 10.2 WhatsApp Approval Request to Employee

```
VMS Visit Request — Action Required

Visitor: {visitor_name}
Company: {visitor_company}
Category: {visitor_category}
Purpose: {visit_purpose}
Expected: {expected_date} at {expected_time}

To APPROVE reply: APPROVE {token}
To REJECT reply: REJECT {token}

This request expires in 2 hours.
— Visitor Management System
```

### 10.3 Email Approval Request to Employee (Fallback)

**Subject:** `[VMS] Visit Request — {visitor_name} on {expected_date}`

**Body:**
```
Hello {employee_name},

{visitor_name} from {visitor_company} has requested a visit.

Category: {visitor_category}
Purpose: {visit_purpose}
Date: {expected_date}
Time: {expected_time}

[APPROVE VISIT]     [REJECT VISIT]
(clickable buttons linking to /approve/{token} and /reject/{token})

This request expires in 2 hours. If you do not respond, the request will be automatically declined.

— Visitor Management System
```

### 10.4 QR Pass Email to Visitor (On Approval)

**Subject:** `Your VMS Visitor Pass — {company_name} on {expected_date}`

**Body:**
```
Hello {visitor_name},

Your visit to {company_name} has been approved.

Visit details:
Host: {employee_name} ({department})
Date: {expected_date}
Time: {expected_time}
Purpose: {visit_purpose}

[QR CODE IMAGE — 200×200px]

Pass ID: VMS-{year}-{date}-{sequence}

Please present this pass at reception on arrival.
This pass is valid for {expected_date} only.

— Visitor Management System
```

### 10.5 Rejection Email to Visitor

**Subject:** `[VMS] Your visit request has been declined`

**Body:**
```
Hello {visitor_name},

Your request to visit {company_name} on {expected_date} has been declined.

{if rejection_reason}
Reason: {rejection_reason}
{endif}

Please contact {employee_name} at {employee_email} to reschedule.

— Visitor Management System
```

### 10.6 Visit Expiry Email to Visitor

**Subject:** `[VMS] Your visit request has expired`

**Body:**
```
Hello {visitor_name},

Your visit request to {company_name} on {expected_date} has expired because no response was received within 2 hours.

Please contact {employee_name} to request a new visit.

— Visitor Management System
```

### 10.7 Blacklist Alert Email to Admin

**Subject:** `[VMS SECURITY ALERT] Blacklist hit — {visitor_name}`

**Body:**
```
Security Alert — Blacklist Match Detected

A visitor registration was blocked by the blacklist engine.

Visitor name: {visitor_name}
Mobile: {mobile_masked} (last 4 digits shown)
ID type: {id_type}
Attempted at: {timestamp}
Matched blacklist reason: {blacklist_reason}

The visitor received a generic "request received" message and does not know they are blacklisted.

View full details in the admin audit log: [LINK]

— VMS Security System
```

### 10.8 Daily Summary Email to Admin

**Subject:** `[VMS] Daily Report — {yesterday_date}`

**Body:**
```
Good morning,

Here is your visitor summary for {yesterday_date}.

Total visitors:       {total}
Completed visits:     {checked_out}
Active at day end:    {still_inside}
Pending/no-show:      {pending}
Blocked (blacklist):  {blocked}
Walk-in visitors:     {walkin_count}
Average duration:     {avg_duration}
Busiest hour:         {peak_hour}

Top departments by visitor count:
1. {dept1} — {count1} visitors
2. {dept2} — {count2} visitors
3. {dept3} — {count3} visitors

[VIEW FULL REPORT]

— Visitor Management System
```

---

## 11. Session and Draft Management

### 11.1 The Problem

The public registration form has three steps. If a visitor completes Steps 1 and 2 and then closes the browser — all their data is lost. They have to start again. For mobile users on slow connections this is a real friction point.

### 11.2 The Solution — Redis Draft Storage

When a visitor completes Step 1 and OTP verification succeeds, the system saves a form draft to Redis:

```
Key: draft:{sessionId}
Value: JSON of all Step 1 fields
TTL: 86400 seconds (24 hours)
```

The `sessionId` is a UUID stored in the visitor's browser as a cookie (HttpOnly, Secure, SameSite=Strict, max-age=86400).

When the visitor returns and opens the form again within 24 hours — the system checks for a draft cookie, finds the Redis key, and pre-populates Step 1 fields. A banner shows: "We saved your progress. Continue where you left off?"

The draft is deleted from Redis when:
- The visitor successfully submits the form
- The TTL expires (24 hours)
- The visitor clicks "Start over"

### 11.3 What Is and Is Not Saved in the Draft

**Saved in draft:** All Step 1 text fields (name, email, company, purpose, category, host, date, time). OTP verification status (so they don't have to re-verify if they return within 10 minutes).

**Not saved in draft:** Uploaded files (photo and ID). The visitor must re-upload on return. Storing file paths in a public-facing cookie or Redis key without authentication would be a security risk.

---

## 12. Duplicate Visit Detection

### 12.1 The Problem

Without duplicate detection, the same person could:
- Register twice for the same day (by accident or by sharing the link)
- Walk in at reception AND have a pre-registered appointment on the same day
- Create multiple PENDING visits that all notify the same host employee

### 12.2 Detection Logic

The system runs this check after blacklist clearance and before creating any visit record:

```sql
SELECT v.VisitID, v.Status, v.ExpectedDate
FROM Visits v
JOIN Visitors vis ON v.VisitorID = vis.VisitorID
WHERE vis.Mobile = :mobile
  AND DATE(v.ExpectedDate) = DATE(:expectedDate)
  AND v.Status IN ('PENDING', 'APPROVED')
LIMIT 1
```

If a row is returned — duplicate detected.

### 12.3 What the System Does on Duplicate

- Does not create a new visit record
- Does not call Twilio
- Returns HTTP 200 to the visitor with: "You already have a visit scheduled for today. Your pass was sent to {email}. If you did not receive it, click Resend."
- A "Resend Pass" button triggers a re-delivery of the existing QR pass email

### 12.4 Walk-In Duplicate Check

When a receptionist creates a walk-in record, the system also runs the duplicate check against the mobile number entered. If a pre-registered appointment exists for today, the screen shows a warning: "This visitor already has a pre-registered appointment today (Visit ID: VMS-2025-0610-00841, Status: APPROVED). Check them in against the existing record instead of creating a new one." The receptionist can choose to proceed with the walk-in or switch to the pre-registered record.

---

## 13. Signed QR Code Security

### 13.1 The Problem

An unsigned QR code can be forged by anyone who knows the payload format. If the QR just encodes `{visitId: 841, date: 2025-06-10}` — anyone can generate that QR code themselves and walk in without a legitimate approval.

### 13.2 How Signing Works

The QR payload is a pipe-delimited string:

```
{visitId}|{visitorId}|{expectedDate}|{hmacSignature}
```

Example:
```
841|291|2025-06-10|a3f9c8d2e1b4a7f6c5d2e8b1a4c7f3d2
```

The HMAC signature is computed as:

```python
import hmac, hashlib

def sign_qr_payload(visit_id, visitor_id, expected_date):
    message = f"{visit_id}|{visitor_id}|{expected_date}"
    signature = hmac.new(
        key=QR_SECRET_KEY.encode('utf-8'),
        msg=message.encode('utf-8'),
        digestmod=hashlib.sha256
    ).hexdigest()
    return f"{message}|{signature}"
```

`QR_SECRET_KEY` is a 32-character random string stored only as an environment variable. It is never exposed in the frontend, never logged, and never stored in the database.

### 13.3 How Verification Works

At the security scanner or reception check-in screen:

```python
def verify_qr_payload(qr_string):
    parts = qr_string.split('|')
    if len(parts) != 4:
        return False, "Invalid format"
    
    visit_id, visitor_id, date, received_hmac = parts
    expected_hmac = sign_qr_payload(visit_id, visitor_id, date)
    
    # Constant-time comparison to prevent timing attacks
    if not hmac.compare_digest(expected_hmac, received_hmac):
        return False, "Signature invalid — possible forgery"
    
    # Check visit is still valid
    visit = get_visit(visit_id)
    if visit.status not in ['APPROVED', 'CHECKED_IN']:
        return False, f"Visit status is {visit.status}"
    if visit.expected_date != date:
        return False, "Date mismatch"
    
    return True, visit
```

### 13.4 What Happens on Failed Verification

- Screen shows: "Invalid pass — do not allow entry"
- AuditLog records: `QR_FORGERY_ATTEMPT` with the raw QR string (truncated to 100 chars), IP address of the scanner device
- WebSocket event sent to admin room: `qr_invalid` with timestamp and scanner location

---

## 14. Scheduled Report Engine

### 14.1 How It Works

The daily report runs as a background scheduled job. In Flask this uses APScheduler. In Spring Boot this uses the `@Scheduled` annotation.

**Flask setup:**
```python
from apscheduler.schedulers.background import BackgroundScheduler

scheduler = BackgroundScheduler()
scheduler.add_job(
    func=send_daily_report,
    trigger='cron',
    hour=9,
    minute=0,
    timezone='Asia/Kolkata'
)
scheduler.start()
```

**Spring Boot setup:**
```java
@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
public void sendDailyReport() {
    reportService.generateAndSendDailyReport();
}
```

### 14.2 What the Report Contains

The report queries the previous calendar day:

```sql
SELECT 
    COUNT(*) as total_visits,
    SUM(CASE WHEN c.CheckOutTime IS NOT NULL THEN 1 ELSE 0 END) as completed,
    SUM(CASE WHEN v.Status = 'BLOCKED' THEN 1 ELSE 0 END) as blocked,
    SUM(CASE WHEN v.VisitType = 'WALKIN' THEN 1 ELSE 0 END) as walkins,
    AVG(c.Duration) as avg_duration,
    HOUR(c.CheckInTime) as peak_hour
FROM Visits v
LEFT JOIN CheckInOut c ON v.VisitID = c.VisitID
WHERE DATE(v.ExpectedDate) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
```

### 14.3 Delivery

- Email rendered as HTML using the template in Section 10.8
- Sent via SendGrid to all users with Role = ADMIN and IsActive = TRUE
- If SendGrid fails — retry 3 times with 5-minute intervals
- Report generation and send status logged to AuditLog

---

## 15. Non-Functional Requirements

| Category | Requirement | Detail |
|---------|-------------|--------|
| Performance | Page load | < 3 seconds on standard broadband |
| Performance | API response | < 500ms for 95% of requests |
| Performance | OTP delivery | SMS within 10 seconds for 95% of sends |
| Performance | WhatsApp delivery | Within 30 seconds for 95% of sends |
| Performance | WebSocket event | Received by admin client within 500ms of trigger |
| Performance | Blacklist lookup | < 10ms Redis hit, < 100ms DB fallback |
| Performance | Redis operations | < 10ms for 99% of operations |
| Performance | Report generation | < 5 seconds for 1 month of data |
| Scalability | Concurrent users | 500 concurrent users without degradation |
| Security | Passwords | bcrypt, minimum cost factor 12, never plaintext |
| Security | API auth | JWT required on all endpoints except public form and Twilio webhook |
| Security | SQL injection | Parameterized queries on all DB operations |
| Security | XSS | All user inputs sanitized before storage and display |
| Security | File uploads | MIME type and extension validated, UUID filenames used |
| Security | Transport | HTTPS required in production |
| Security | Twilio webhook | HMAC-SHA1 signature validation on every incoming webhook |
| Security | QR codes | HMAC-SHA256 signed payload, constant-time comparison on verify |
| Security | Redis | requirepass auth, localhost binding only |
| Security | ID numbers | Last 4 digits only displayed on any screen |
| Security | OTP | Max 3 attempts, 15-min lockout, 3 sends/hour |
| Reliability | Twilio down | System operates with email-only approvals and manual check |
| Reliability | Redis down | System falls back to MySQL for OTP and sessions |
| Reliability | WebSocket down | Dashboard falls back to 30-second polling |
| Usability | Mobile | Registration form fully functional on 320px width screens |
| Usability | Check-in time | Pre-registered visitor checked in within 60 seconds |
| Usability | Error messages | Every error message names the problem and the fix |
| Usability | Accessibility | WCAG AA contrast, keyboard navigation, screen reader labels |
| Maintainability | API docs | All endpoints documented in Postman |
| Maintainability | Code comments | All non-trivial functions have inline comments |
| Maintainability | Config | All config in .env files — nothing hardcoded |
| Data | Visitor records | Retained for 1 year after visit |
| Data | OTP logs | Retained for 90 days |
| Data | Audit log | Retained for 2 years |
| Data | Uploaded files | Deleted 90 days after check-out |
| Data | Blacklist entries | Retained indefinitely even when deactivated |

---

## 16. Technology Stack

| Layer | Technology | Why This Choice |
|-------|------------|----------------|
| Frontend | HTML5, Bootstrap 5, Vanilla JavaScript | Fast to build, no build pipeline needed in 20 days |
| WebSocket client | Socket.IO client (CDN) | Auto-reconnect, fallback to polling, works with Flask-SocketIO |
| Backend | Python Flask + Flask-SocketIO | Fastest to set up for a 20-day project, large ecosystem |
| Backend alt. | Java Spring Boot + Spring WebSocket | If team is Java-focused — both are valid |
| Database | MySQL 8.0 | ACID compliance, relational integrity, widely understood |
| Cache | Redis 7.0 | Sub-millisecond reads, automatic TTL expiry, industry standard |
| OTP | Twilio Verify API | Handles code generation, delivery, and check in one API |
| WhatsApp | Twilio WhatsApp Business API | Official WhatsApp integration, webhook support |
| Email | SendGrid | Reliable deliverability, free tier covers internship volume |
| Scheduler | APScheduler (Flask) / @Scheduled (Spring) | Built-in, no extra infrastructure needed |
| QR Code | python-qrcode (Flask) / qrcode.js (frontend) | Lightweight, well-documented |
| File Storage | Server filesystem — `/uploads/visitors/{year}/{month}/{uuid}.ext` | Simple for internship scope |
| Auth | JWT (PyJWT for Flask / jjwt for Spring) | Stateless, scalable |
| Version Control | GitHub | Collaboration, PR review, history |
| CI/CD | GitHub Actions | Automated lint and test on every push |

---

## 17. User Stories

| ID | Role | Story | Priority | Status |
|----|------|-------|---------|--------|
| US-01 | Visitor | Register my visit online with OTP phone verification before arriving | HIGH | v2 |
| US-02 | Visitor | Get a signed QR pass on my email that cannot be forged | HIGH | v3 |
| US-03 | Visitor | Return to my half-filled form and continue from where I left off | MEDIUM | v3 |
| US-04 | Visitor | Be told clearly if I already have a visit for today instead of creating a duplicate | MEDIUM | v3 |
| US-05 | Employee | Approve a visitor by replying to a WhatsApp message in one tap | HIGH | v2 |
| US-06 | Employee | Reject a visitor and provide a reason via WhatsApp | HIGH | v2 |
| US-07 | Employee | Generate a registration invite link to send to my guest | MEDIUM | v3 |
| US-08 | Receptionist | Check in a pre-registered visitor in under 60 seconds by searching or scanning | HIGH | v2 |
| US-09 | Receptionist | Create a walk-in visit and be warned if the visitor is on the blacklist | HIGH | v2 |
| US-10 | Receptionist | See a walk-in duplicate warning if the visitor already has an appointment today | MEDIUM | v3 |
| US-11 | Receptionist | Print a colour-coded badge that reflects the visitor's category | MEDIUM | v3 |
| US-12 | Admin | See every visitor check-in on my dashboard in real time without refreshing | HIGH | v2 |
| US-13 | Admin | Add a person to the blacklist with a reason so they are blocked automatically | HIGH | v2 |
| US-14 | Admin | Receive a daily 9 AM email with yesterday's visitor summary | MEDIUM | v3 |
| US-15 | Admin | See a report of all blacklist hits this month | MEDIUM | v2 |
| US-16 | Admin | Export any report as PDF or Excel | MEDIUM | v1 |
| US-17 | Security | Scan a visitor's QR code and be told instantly if it is valid or forged | HIGH | v3 |
| US-18 | Security | See the visitor's photo on screen when I scan the QR so I can verify identity | HIGH | v1 |

---

## 18. Integration Points

### 18.1 Twilio Verify — OTP

- **API base URL:** `https://verify.twilio.com/v2/Services/{VERIFY_SID}`
- **Auth:** HTTP Basic — username: TWILIO_ACCOUNT_SID, password: TWILIO_AUTH_TOKEN
- **Trigger OTP:** POST `/Verifications` with `To={mobile}&Channel=sms`
- **Verify OTP:** POST `/VerificationChecks` with `To={mobile}&Code={code}`
- **Twilio response on success:** `{"status": "approved", "valid": true}`
- **Twilio response on failure:** `{"status": "pending", "valid": false}`
- **Cost:** ~$0.05 per OTP on paid tier. Trial credit of $15 covers ~300 verifications

### 18.2 Twilio WhatsApp — Approval

- **API base URL:** `https://api.twilio.com/2010-04-01/Accounts/{SID}/Messages`
- **Auth:** HTTP Basic — username: TWILIO_ACCOUNT_SID, password: TWILIO_AUTH_TOKEN
- **From:** `whatsapp:+14155238886` (Twilio sandbox number)
- **To:** `whatsapp:+91{employeeMobile}`
- **Webhook endpoint:** `POST /api/webhooks/twilio/whatsapp`
- **Sandbox requirement:** Each employee must WhatsApp the sandbox number with the join code once before receiving messages

### 18.3 SendGrid — Email

- **API base URL:** `https://api.sendgrid.com/v3/mail/send`
- **Auth:** Bearer token — SENDGRID_API_KEY
- **From address:** `noreply@yourdomain.com` (must be verified in SendGrid)
- **Free tier:** 100 emails/day — sufficient for internship
- **Templates:** Rendered server-side as HTML strings — not using SendGrid dynamic templates

### 18.4 Redis

- **Connection:** `redis://:{password}@localhost:6379/0`
- **Database 0:** All VMS keys
- **Max memory policy:** `allkeys-lru` — evicts least recently used keys if memory is full
- **Persistence:** `appendonly no` — Redis is a cache, not a primary store; data loss on restart is acceptable

---

## 19. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Twilio trial credit exhausted | Medium | High | Monitor credit daily; ~300 OTPs on $15 credit. Disable OTP in settings if needed |
| Employee does not have WhatsApp | Medium | Medium | Email fallback always sent simultaneously |
| QR_SECRET_KEY exposed in code | Low | Critical | Never commit .env to Git. Add .env to .gitignore on Day 1 |
| Redis single node failure | Low | High | MySQL fallback for all Redis operations. Redis data is ephemeral — loss is acceptable |
| Twilio sandbox employee join requirement | High | Medium | Document setup steps clearly. All test employees must join sandbox before testing |
| Database grows too large for free hosting | Low | Medium | Implement pagination on all list endpoints; add indexes on Day 6 |
| File storage fills server disk | Low | Medium | Enforce 5MB limit per file, delete files 90 days after check-out |
| Duplicate QR forgery via replay attack | Low | High | QR is one-time: after CHECKED_IN status, QR becomes invalid for re-entry |
| APScheduler not firing in production | Low | Medium | Log every scheduled job execution; add a manual "Send today's report" button as backup |
| Scope creep past Day 10 | High | High | Freeze scope after this document is signed. Any new feature goes to v4 backlog |

---

## 20. Out of Scope

The following are **not** built in this version. They are documented here so they are not forgotten for future versions.

| Feature | Why deferred |
|---------|-------------|
| Mobile native app (iOS/Android) | Requires separate development — 20-day constraint |
| Biometric verification | Requires hardware integration |
| Multi-branch / multi-tenant | Significant architectural change |
| Physical access control integration | Hardware dependency |
| WhatsApp interactive buttons | Requires Meta Business verification |
| Facial recognition ID matching | AI/ML component, out of internship scope |
| Automated court-order data feed | Legal/external API dependency |
| Visitor self-service kiosk | Hardware dependency |
| Pre-visit document collection | Document management system dependency |
| Multi-language registration form | i18n complexity |

---

## 21. 20-Day Timeline

| Day | Phase | Task | Deliverable |
|-----|-------|------|-------------|
| 1 | Planning | Project understanding, program study | Day 1 report |
| 2 | Planning | Requirement gathering, BRD, stakeholder analysis | BRD, Day 2 report |
| 3 | Planning | Use case diagrams, user flow diagrams | UML diagrams |
| 4 | Planning | Entity Relationship Diagram, data dictionary | ERD, data dictionary |
| 5 | Planning | PRD v3 finalised, SRS v3 finalised | PRD, SRS (this document) |
| 6 | Design | Wireframes for all 8 screens | Wireframe document |
| 7 | Design | HTML/CSS for visitor registration form | Registration form HTML |
| 8 | Design | HTML/CSS for reception, admin dashboard | Dashboard HTML |
| 9 | Design | HTML/CSS for approval, QR pass, blacklist screens | All screen HTML |
| 10 | Design | MySQL schema creation, Redis setup, test data | Database ready |
| 11 | Backend | Flask/Spring Boot project setup, JWT auth | Auth module |
| 12 | Backend | Visitor registration API, file upload, OTP integration | Registration API |
| 13 | Backend | Blacklist engine, duplicate detection | Blacklist module |
| 14 | Backend | Approval workflow, Twilio WhatsApp webhook | Approval module |
| 15 | Backend | QR pass generation (signed), email delivery | QR + email module |
| 16 | Backend | Check-in/out APIs, walk-in module, WebSocket server | Check-in module |
| 17 | Backend | Reports module, APScheduler daily email | Reports module |
| 18 | Backend | Admin dashboard APIs, user management | Admin module |
| 19 | Testing | Full end-to-end testing of all flows | Test report |
| 20 | Delivery | Bug fixes, documentation review, final presentation | Final presentation |

---

*VMS-PRD-003 | v3.0 | June 12, 2025 | 20-Day Software Development Internship Program*
*This document supersedes VMS-PRD-001 and VMS-PRD-002*
