# Project Understanding Document & Problem Statement

## 1. Introduction
This document outlines the foundational understanding of the Visitor Management System (VMS) project, including the Software Development Life Cycle (SDLC) approach, visitor management concepts, and the core problem statement.

## 2. Problem Statement
The corporate office currently receives hundreds of visitors daily, including clients, vendors, interview candidates, employee guests, delivery personnel, and service providers. The existing system relies on outdated paper logs which pose several significant challenges:
- **Security Risks:** Paper logs can be easily forged, modified, or lost. There is no reliable identity verification.
- **Inefficiency:** Receptionists spend excessive time manually entering details, and employees must physically come down or be called via intercom to approve visitors.
- **Lack of Tracking:** There is no real-time visibility into who is currently in the building, how long they have been there, or when they left.
- **Poor User Experience:** Long queues at reception create a negative first impression for important clients and candidates.

## 3. Project Objective
To design, develop, test, and deploy a complete digital Visitor Management System (VMS) that automates visitor registration, provides robust identity verification, enables instant digital approvals from host employees, issues secure digital passes, and offers real-time tracking and reporting for the administration.

## 4. SDLC Approach
The project follows an Agile-based iterative SDLC, divided into weekly sprints:
- **Week 1: Requirement Gathering & Planning** - Understanding the business process, identifying stakeholders, and designing the database and system architecture.
- **Week 2: UI/UX & Database Development** - Creating wireframes, clickable prototypes, and responsive frontend screens.
- **Week 3: Backend Development** - Building the server-side logic using Java Spring Boot, exposing REST APIs, and integrating database connectivity.
- **Week 4: Advanced Features & Deployment** - Implementing reporting, QR code generation, end-to-end testing, and final system integration.

## 5. Key Visitor Management Concepts
- **Pre-Registration:** Allowing visitors to complete the registration process via a secure link before they arrive.
- **Identity Verification:** Using OTP (One-Time Password) via SMS to verify the visitor's mobile number.
- **Role-Based Workflows:** Different approval and routing rules based on the visitor category (e.g., Deliveries skip approval, Clients require employee approval).
- **Secure Access:** Generating unforgeable, cryptographically signed QR codes for entry.
- **Real-time Monitoring:** Providing security and admin teams with a live dashboard of active check-ins.
