# Business Requirement Document (BRD)

## 1. Project Overview
The Visitor Management System (VMS) aims to digitize and secure the process of tracking visitors entering the corporate office. The system will automate registration, verify identities, coordinate host approvals, and provide actionable analytics.

## 2. Target Audience / Users
The system identifies five primary user roles:
1. **Visitor:** The external individual entering the premises (e.g., Client, Vendor, Interview Candidate, Delivery Personnel).
2. **Receptionist:** Front desk staff managing walk-ins, checking in/out visitors, and printing physical badges.
3. **Employee (Host):** The corporate staff member expecting a visitor. They receive approval requests and manage their own visitor history.
4. **Security:** Gate personnel responsible for scanning digital QR passes to allow or deny entry.
5. **Admin:** System administrators who manage users, configure the blacklist, and view global reports and real-time dashboards.

## 3. Key Features
- **Visitor Registration (Pre-registration & Walk-in):** Captures name, mobile, email, company, purpose, photo, and ID proof.
- **OTP Identity Verification:** Ensures the mobile number belongs to the registering visitor.
- **Categorized Workflows:** Distinguishes between different visitor types (Client vs. Delivery) to apply specific security protocols.
- **Automated Approvals:** Notifies host employees via WhatsApp/Email to approve or reject the visit.
- **Secure Digital Passes:** Generates cryptographically signed QR codes sent via email upon approval.
- **Check-In/Check-Out Management:** Reception dashboard to log exact entry and exit times.
- **Blacklist & Security Engine:** Automatically screens visitors against a blocked list.
- **Analytics & Reporting:** Daily, weekly, and monthly insights into visitor traffic and department loads.

## 4. High-Level Workflows
### 4.1 Pre-Registered Workflow
1. Employee generates an invite link and sends it to the visitor.
2. Visitor fills out the form, verifies phone via OTP, and uploads documents.
3. System runs a silent blacklist check.
4. System pings the host employee for approval (WhatsApp/Email).
5. Upon approval, visitor receives a secure QR pass.
6. Visitor presents the QR pass at reception for quick scan and check-in.
7. Upon leaving, reception checks them out.

### 4.2 Walk-In Workflow
1. Visitor arrives at reception without prior registration.
2. Receptionist enters the visitor's details into the system.
3. System instantly checks the blacklist.
4. Receptionist verbally confirms the visit with the host employee via phone/intercom.
5. Receptionist checks the visitor in and prints a physical badge.
6. Upon leaving, reception checks them out.
