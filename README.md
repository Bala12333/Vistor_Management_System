# 🛡️ Visitor Management System (VMS) v3.0

> A smart, secure, push-based digital security and visitor lifecycle platform. VMS replaces outdated paper logs with verified phone identity, instant WhatsApp approvals, cryptographically signed unforgeable QR passes, and real-time dashboard monitoring.

Developed as the culmination of a 20-day software engineering internship, **VMS** represents the final, complete product specification and architecture, integrating robust security mechanisms, failover protocols, and premium design patterns using **Java Spring Boot** as the backend framework.

---

## 📂 Project Documentation Index

All primary documentation resides in the [doc/](doc) directory:

1. **[Product Requirements Document (PRD) v3.0](doc/VMS_PRD_v3.md)**: Product goals, detailed feature descriptions, role permissions, notification templates, and user stories.
2. **[Software Requirements Specification (SRS) v3.0](doc/VMS_SRS_v3.md)**: Detailed technical specifications, database schema definitions, API reference, pagination standards, file storage rules, and test cases.
3. **[Architecture & Complete Workflow Guide](doc/VMS_Architecture_Workflow.md)**: Sequential UML/Mermaid flows detailing pre-registration, walk-ins, OTP validation, blacklist checks, webhook validations, and failure mitigations.


---

## 🏛️ System Architecture & Tech Stack

VMS is structured as a multi-tier web application built on modern, lightweight, and resilient technologies.

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer"]
        BROWSER["🌐 Browser Client<br/>(HTML5 / Bootstrap 5 / Vanilla JS)"]
    end
    subgraph App["Application Layer (Spring Boot)"]
        AUTH["🔑 Auth Service"]
        VISITOR["👤 Visitor & Visit Service"]
        BLACKLIST["🛡️ Blacklist Engine"]
        APPROVAL["💬 Approval Webhook Service"]
        QR["🔍 QR Code Generator & Validator"]
        WS["📡 WebSocket Server (Spring WebSocket/STOMP)"]
        SCHED["⏰ Scheduler (@Scheduled)"]
    end
    subgraph Data["Data Layer"]
        MYSQL["🗄️ MySQL 8.0<br/>(Permanent Relational Storage)"]
        REDIS["⚡ Redis 7.0<br/>(OTP, Drafts, Hot-Path Blacklist)"]
        FILES["📁 Local Filesystem<br/>(Restricted Access Storage)"]
    end
    subgraph Services["External Integrations"]
        TWILIO_V["📱 Twilio Verify (OTP SMS)"]
        TWILIO_W["💬 Twilio WhatsApp API"]
        SENDGRID["✉️ SendGrid Email API"]
    end

    BROWSER <--> AUTH & VISITOR & APPROVAL
    BROWSER -. WebSocket .-> WS
    AUTH & VISITOR & APPROVAL & QR & SCHED & WS --> MYSQL & REDIS
    VISITOR --> FILES
    VISITOR --> TWILIO_V
    APPROVAL --> TWILIO_W & SENDGRID
```

### 🛠️ Technology Stack Breakdown
* **Frontend:** Responsive HTML5, Bootstrap 5, Vanilla JavaScript, and SockJS/STOMP client.
* **Backend:** Java Spring Boot (with Spring WebSocket STOMP).
* **Database:** MySQL 8.0 (ACID compliance, relational integrity, customized indexes).
* **Cache:** Redis 7.0 (ephemeral data, hot-path lookup, form draft persistence with TTL).
* **Services:** Twilio Verify (OTP SMS), Twilio WhatsApp Business API (Approvals), SendGrid (Transactional emails & Daily reports).

---

## 🌟 Core Features & Capability Matrix

### 1. The Six Visitor Categories & Routing Rules
Every visitor belongs to a category which determines their verification requirements, routing destination, badge styling, and max duration.

| Category | Description | OTP | Approval | Badge Color | Routing & Escalation | Max Duration |
| :--- | :--- | :---: | :---: | :---: | :--- | :--- |
| **Client / Business** | Corporate partners, customers | Yes | Host Employee | **Blue** (#1B3F6E) | Main reception $\rightarrow$ escorted by host | 8 Hours |
| **Interview Candidate**| Job applicants | Yes | HR Employee | **Green** (#1E6641) | Main reception $\rightarrow$ HR waiting area | 4 Hours |
| **Vendor / Supplier** | Contract service providers | Yes | Operations / Proc | **Amber** (#B45309) | Goods entry / delivery bay | 4 Hours |
| **Delivery Personnel**| Courier, package delivery | No | Receptionist | **Orange** (#C05621)| Goods entry (does not proceed past bay) | 30 Mins |
| **Service / Tech** | IT, electricians, plumbing | Yes | Facilities Manager | **Grey** (#374151) | Service entrance $\rightarrow$ escort to work area | 8 Hours |
| **Personal Guest** | Family, friends of employees | Yes | Host Employee | **Purple** (#5B21B6)| Main reception $\rightarrow$ escorted by host | 2 Hours |

> [!NOTE]
> **Delivery Personnel Bypass:** Since couriers change daily, the system automatically skips OTP SMS verification and employee approvals for them. They are auto-approved upon passing the blacklist check and routed directly to the delivery bay.

---

### 2. Identity Verification & Draft Management
* **Twilio Verify OTP:** Enforces E.164 phone validation. Features a 5-minute expiry, a maximum of 3 code requests per hour, and locking of the mobile number for 15 minutes after 3 consecutive verification failures to prevent brute-forcing.
* **Redis Form Drafts:** Multi-step registration forms can be slow on mobile data. When a visitor completes Step 1 (Details) and verifies their phone, VMS saves a progress draft to Redis (`draft:{sessionId}`) with a **24-hour TTL**. If they return within 24 hours, their progress is restored.
  > [!WARNING]
  > File uploads (Visitor Photo and Government ID documents) are **never** cached in drafts due to data security guidelines; they must be re-uploaded on form resumption.

---

### 3. Smart Security Blacklist Engine
Protects the facilities from known security risks using a multi-level cache fallback.
* **Redis Hot-Path Check:** Checks visitor mobile number and ID document number against Redis (`blacklist_mobile:{mobile}`) in under 10ms.
* **Database Fallback:** If Redis is down, VMS queries the SQL `Blacklist` table directly (under 100ms).
* **Silent Blocking:** When a blacklisted individual registers, the system displays a generic "request received" message to prevent alerting them. Internally, the visit status is marked as `BLOCKED`.
* **Instant Alerts:** Triggers a real-time WebSocket push to the admin dashboard (displaying a red banner) and sends an immediate email containing the masked visitor details and blacklist hit reason to all administrators.

---

### 4. Duplicate Visit Detection
Before any visit record is created, the system queries for existing `PENDING` or `APPROVED` visits for that mobile number on the expected date:
* **Pre-Registration Flow:** Returns `HTTP 200` with the existing ticket ID and an option to resend the QR pass email. No duplicate records are created.
* **Walk-In Flow:** Displays a warning to the receptionist showing the active record. The receptionist can choose to check them in under the existing record or proceed with a new record.

---

### 5. Cryptographically Signed QR Passes
VMS uses digital signatures to prevent pass forgery or modification:
1. **Creation:** When a visit is approved, a pipe-delimited payload is created: `visitId|visitorId|expectedDate|hmac`.
2. **Signature:** The HMAC is computed as: `HMAC-SHA256(QR_SECRET_KEY, visitId + "|" + visitorId + "|" + expectedDate)`.
3. **Verification:** Upon scan at the gate, the server splits the QR, recomputes the HMAC, and uses `hmac.compare_digest()` (constant-time verification to prevent timing attacks).
4. **Validation:** Checks if the visit date matches today and status is `APPROVED`. A checked-in/out pass is rejected if scanned again (preventing replay attacks). Invalid scans log a `QR_FORGERY_ATTEMPT` event.

---

### 6. WhatsApp & Email Approvals
* **Twilio WhatsApp Webhook:** Sends an approval template directly to the host employee's mobile. The employee replies with `APPROVE {token}` or `REJECT {token}`. The VMS webhook intercepts the incoming POST, verifies the `X-Twilio-Signature` header using HMAC-SHA1 to prevent spoofing, and updates the visit status.
* **Two-Way Rejection Flow:** If the employee rejects, the system follows up via WhatsApp asking for the reason. If they reply within 10 minutes, the comment is appended as the rejection remark.
* **Email Fallback:** A parallel HTML email is sent via SendGrid with secure action links, ensuring approvals proceed even if WhatsApp is offline.
* **Auto-Expiry:** If no action is taken within 2 hours, the visit status changes to `EXPIRED`.

---

### 7. Real-Time Admin Dashboard & Daily Reporting
* **Real-Time WebSockets:** Uses a long-lived JWT-authenticated WebSocket connection at `/ws/admin` to push live visitor counts, active check-ins, and security logs to the dashboard.
* **Long-Polling Fallback:** If WebSockets become unreachable, the dashboard automatically shifts to standard REST API polling every 30 seconds and shows a "Live updates paused" indicator.
* **Automated Daily Reports:** At **9:00 AM IST**, a scheduled cron job aggregates yesterday's metrics (total visitors, check-ins, walk-ins, blacklist hits, busy hours, and top departments) and emails a summary to all active admins.

---

## 🗄️ Database Schema Summary

The system database requires structured tables containing customized indexes for performant lookups:

| Table | Purpose | Core Fields |
| :--- | :--- | :--- |
| **`Users`** | System actors & credentials | `UserID`, `Email`, `PasswordHash` (bcrypt), `Role`, `FailedLoginCount` |
| **`Departments`** | Department definitions | `DepartmentID` (PK), `DepartmentName` |
| **`Employees`** | Host information | `EmployeeID` (FK Users), `DepartmentID` (FK Departments), `Designation`, `Contact` (WhatsApp) |
| **`InviteTokens`** | pre-registration links | `Token` (UUID), `EmployeeID`, `ExpiresAt`, `UsedAt` |
| **`VisitorCategories`** | Category configurations | `CategoryCode` (PK), `DisplayName`, `RequiresApproval`, `BadgeColour` |
| **`Visitors`** | Master visitor directory | `VisitorID`, `Name`, `Contact` (Indexed), `IDNumber` (Indexed), `PhotoPath` |
| **`Visits`** | Visit records | `VisitID`, `VisitorID`, `EmployeeID`, `CategoryCode`, `VisitDate`, `Status` |
| **`Approvals`** | Approval logs | `ApprovalID`, `VisitID`, `ApprovedBy`, `Channel` (WhatsApp/Email), `Remarks` |
| **`CheckInOut`** | Check-in metrics | `CheckID`, `VisitID`, `CheckInTime`, `CheckOutTime`, `WalkinHostConfirmed` |
| **`Blacklist`** | Blacklisted entities | `MobileNumber` (Indexed), `IDNumber` (Indexed), `Reason`, `IsActive` |
| **`BlacklistCheckLog`** | Security audit check logs | `VisitID`, `CheckedMobile`, `Result` (HIT/MISS), `CheckedAt` |
| **`PendingWhatsAppReply`** | Multi-step WhatsApp states | `VisitID`, `EmployeeMobile`, `AwaitingStep`, `ExpiresAt` |
| **`AuditLog`** | System-wide audit log | `Action` (Indexed), `PerformedBy`, `Details`, `IPAddress`, `CreatedAt` |
| **`OTPFallback`** | MySQL OTP cache | `Mobile` (Indexed), `VerificationSID`, `Verified`, `ExpiresAt` (Used if Redis is down) |

---

## 🔒 Security & Data Compliance Standards

* **Credential Security:** All user passwords must be hashed using `bcrypt` with a cost factor of 12 or higher.
* **PII Masking:** Visitor government ID numbers are never displayed in full on any UI screen or sent in plain REST payloads. They are masked to show only the last 4 digits (e.g., `****9012`).
* **Secure File Handling:** Uploaded visitor photos and IDs are stored on disk using UUID filenames. Access to these files is restricted via an authenticated endpoint (`GET /api/files/{uuid}`) checking the requester's RBAC role. Files are deleted automatically 90 days after check-out.
* **Redis Reliability Fallback:** If `PING` checks to Redis fail, the system diverts cache workflows:
  * OTP verification falls back to the `OTPFallback` SQL table.
  * System session tokens fall back to server-side memory.
  * Blacklist checks bypass Redis and query the SQL `Blacklist` table directly.

---

## 🚀 Getting Started

### 📋 Prerequisites
* Java 17+ and Maven 3.9+
* MySQL 8.0+
* Redis 7.0+
* A Twilio account (with Verify Service and WhatsApp Sandbox enabled)
* A SendGrid API Key (with verified Sender email)

### ⚙️ Environment Configuration (`.env`)
Create a `.env` file in the root directory and populate the variables. Never commit this file to version control.

```env
# Application Settings
SECRET_KEY=your_jwt_signing_key_here
QR_SECRET_KEY=your_32_character_qr_secret_key
TIMEZONE=Asia/Kolkata
FRONTEND_URL=http://localhost:3000
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Databases
DATABASE_URL=mysql+pymysql://user:password@localhost:3306/vms_db
REDIS_URL=redis://:redis_password@localhost:6379/0

# Twilio Configuration
TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
TWILIO_AUTH_TOKEN=your_twilio_auth_token_here
TWILIO_VERIFY_SID=VAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886

# Email Configuration
SENDGRID_API_KEY=SG.XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

### 🏃 Setup Commands
1. Clone the repository and navigate to the project directory.
2. Build the project using Maven:
   ```bash
   mvn clean install
   ```
3. Run the database migration scripts to initialize the tables and compile indexes.
4. Start the Redis cache and MySQL server locally.
5. Launch the application server:
   ```bash
   mvn spring-boot:run
   ```

---

## 🚀 Future Enhancements / Production Rollout

### 📱 SMS OTP Integration (Twilio)
During the internship and development phases, real SMS sending via **Twilio** is temporarily disabled to avoid accumulating billing costs on trial accounts. 

Currently, the system uses a **Mock OTP (`123456`)** backed by Redis for all mobile number verification flows. 

**Production Rollout Plan:**
1. **Enable Twilio Verify API:** Once the project is moved to full production, the `SecurityServiceImpl.java` will be updated to utilize the Twilio Verify REST API.
2. **Remove Mock Logic:** The static `123456` Redis assignment in `sendMobileOtp` will be replaced with the Twilio Verify SDK trigger.
3. **Environment Variables:** Production `.env` files will require active `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_VERIFY_SID` keys (already stubbed in configuration).
4. **WhatsApp Fallback:** After SMS OTP stabilizes, we will introduce WhatsApp-based OTP delivery using the Twilio WhatsApp Sandbox infrastructure.