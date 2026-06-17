# Use Case Diagram & User Flows

## 1. Use Case Diagram
The following Mermaid diagram illustrates the primary interactions between the actors and the VMS.

```mermaid
usecaseDiagram
    actor Visitor
    actor Receptionist
    actor Employee
    actor Admin
    actor Security

    package "Visitor Management System" {
        usecase "Register Details" as UC1
        usecase "Verify OTP" as UC2
        usecase "Upload ID/Photo" as UC3
        usecase "Approve/Reject Visit" as UC4
        usecase "Generate QR Pass" as UC5
        usecase "Check-In / Check-Out" as UC6
        usecase "Scan QR Code" as UC7
        usecase "Manage Blacklist" as UC8
        usecase "View Real-time Dashboard" as UC9
        usecase "Generate Reports" as UC10
    }

    Visitor --> UC1
    Visitor --> UC2
    Visitor --> UC3

    Employee --> UC4

    Receptionist --> UC6
    
    Security --> UC7

    Admin --> UC8
    Admin --> UC9
    Admin --> UC10

    UC4 ..> UC5 : <<include>>
    UC1 ..> UC8 : <<include>>
```

## 2. User Flow: Pre-Registration
```mermaid
flowchart TD
    A[Visitor receives invite link] --> B[Fills Registration Form]
    B --> C{OTP Verification}
    C -->|Fails| D[Registration Locked]
    C -->|Passes| E[Upload Photo & ID]
    E --> F[System Blacklist Check]
    F -->|Hit| G[Silently Blocked & Alert Admin]
    F -->|Clear| H[Approval Request to Host]
    H -->|Rejected| I[Visitor Notified of Rejection]
    H -->|Approved| J[System Generates QR Pass]
    J --> K[Emailed to Visitor]
```

## 3. User Flow: Reception Walk-In
```mermaid
flowchart TD
    A[Visitor Arrives at Desk] --> B[Receptionist Enters Details]
    B --> C[Instant Blacklist Check]
    C -->|Hit| D[Alert Displayed, Entry Denied]
    C -->|Clear| E[Receptionist Calls Host]
    E -->|Approved| F[Check In Visitor]
    F --> G[Print Badge]
    G --> H[Visitor Enters]
    H --> I[Visitor Leaves]
    I --> J[Receptionist Checks Out]
```
