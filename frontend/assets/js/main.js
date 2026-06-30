/**
 * VMS Main JavaScript file
 * Handles Form Validations, Dynamic Fields, and OTP Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    const otpInputs = document.querySelectorAll('.otp-box');
    if (otpInputs.length > 0) {
        otpInputs.forEach((input, index) => {
            input.addEventListener('input', (e) => {
                e.target.value = e.target.value.replace(/[^0-9]/g, '');
                
                if (e.target.value.length === 1 && index < otpInputs.length - 1) {
                    otpInputs[index + 1].focus();
                }
            });

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Backspace' && e.target.value === '' && index > 0) {
                    otpInputs[index - 1].focus();
                }
            });
            
            input.addEventListener('paste', (e) => {
                e.preventDefault();
                const pastedData = e.clipboardData.getData('text').replace(/[^0-9]/g, '').substring(0, 6);
                for(let i=0; i<pastedData.length; i++) {
                    if(i < otpInputs.length) {
                        otpInputs[i].value = pastedData[i];
                    }
                }
                if(pastedData.length > 0) {
                    const focusIndex = Math.min(pastedData.length, otpInputs.length - 1);
                    otpInputs[focusIndex].focus();
                }
            });
        });
    }

    const categorySelect = document.getElementById('visitorCategory');
    const dynamicFieldsContainer = document.getElementById('dynamicFieldsContainer');

    if (categorySelect && dynamicFieldsContainer) {
        categorySelect.addEventListener('change', (e) => {
            const category = e.target.value;
            let html = '';
            
            if (category === 'VENDOR' || category === 'CLIENT') {
                html = `
                    <div class="col-md-6 mb-3 animate-slide-up">
                        <label class="form-label-premium">Company Name *</label>
                        <input type="text" class="form-control form-control-premium" required placeholder="Enter company name">
                    </div>
                `;
            } else if (category === 'INTERVIEW') {
                html = `
                    <div class="col-md-6 mb-3 animate-slide-up">
                        <label class="form-label-premium">Position Applied For *</label>
                        <input type="text" class="form-control form-control-premium" required placeholder="e.g. Software Engineer">
                    </div>
                `;
            } else if (category === 'DELIVERY') {
                html = `
                    <div class="col-md-6 mb-3 animate-slide-up">
                        <label class="form-label-premium">Courier Service *</label>
                        <input type="text" class="form-control form-control-premium" required placeholder="e.g. FedEx, BlueDart">
                    </div>
                    <div class="col-md-6 mb-3 animate-slide-up">
                        <label class="form-label-premium">Tracking Number</label>
                        <input type="text" class="form-control form-control-premium" placeholder="Optional">
                    </div>
                `;
            }
            
            dynamicFieldsContainer.innerHTML = html;
        });
    }

    const handleFilePreview = (inputId, previewId) => {
        const fileInput = document.getElementById(inputId);
        const previewElement = document.getElementById(previewId);
        
        if (fileInput && previewElement) {
            fileInput.addEventListener('change', function() {
                const file = this.files[0];
                if (file) {
                    if (file.size > 5 * 1024 * 1024) { // 5MB limit
                        alert("File size exceeds 5MB limit.");
                        this.value = ""; // Clear input
                        return;
                    }
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        previewElement.src = e.target.result;
                        previewElement.style.display = 'block';
                    }
                    reader.readAsDataURL(file);
                }
            });
        }
    };

    handleFilePreview('photoUpload', 'photoPreview');
    handleFilePreview('idUpload', 'idPreview');

    // --- API Configuration ---
    const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' 
        ? 'http://localhost:8080/api/v1' 
        : 'https://YOUR_RENDER_APP_NAME.onrender.com/api/v1'; // <-- IMPORTANT: Replace with your actual Render URL before deploying frontend!

    const getToken = () => localStorage.getItem('vms_jwt');

    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!loginForm.checkValidity()) return;

            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            try {
                const response = await fetch(`${API_BASE_URL}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    localStorage.setItem('vms_jwt', data.token);
                    localStorage.setItem('vms_user', JSON.stringify(data));
                    window.location.href = 'dashboard.html';
                } else {
                    alert('Invalid credentials. Please try again.');
                }
            } catch (error) {
                console.error('Login error:', error);
                alert('Connection error. Is the backend running?');
            }
        });
    }

    const registrationForm = document.getElementById('registrationForm');
    const btnSendMobileOtp = document.getElementById('btnSendMobileOtp');
    const btnSendEmailOtp = document.getElementById('btnSendEmailOtp');
    const mobileOtpSection = document.getElementById('mobileOtpSection');
    const emailOtpSection = document.getElementById('emailOtpSection');
    
    const mobileVerifiedBadge = document.getElementById('mobileVerifiedBadge');
    const emailVerifiedBadge = document.getElementById('emailVerifiedBadge');
    
    let isMobileVerified = false;
    let isEmailVerified = false;
    let confirmationResult = null; // Store Firebase auth result

    if (btnSendMobileOtp) {
        btnSendMobileOtp.addEventListener('click', async () => {
            const mobileInput = document.getElementById('mobile');
            if (!mobileInput.checkValidity()) {
                alert('Please enter a valid 10-digit mobile number.');
                return;
            }
            const mobile = '+91' + mobileInput.value;
            const apiUrl = `${API_BASE_URL}/security/otp/send-mobile?mobile=${encodeURIComponent(mobile)}`;
            
            try {
                btnSendMobileOtp.innerText = 'Sending...';
                const response = await fetch(apiUrl, { method: 'POST' });

                if (response.ok) {
                    btnSendMobileOtp.innerText = 'Sent!';
                    btnSendMobileOtp.classList.replace('btn-outline-primary', 'btn-success');
                    mobileOtpSection.style.display = 'block';
                    alert('SMS sent! (Note: use 123456 for testing)');
                } else {
                    try {
                        const errorData = await response.json();
                        alert(`Error: ${errorData.error || errorData.message || 'Failed to send SMS OTP.'}`);
                    } catch (e) {
                        alert('Error sending OTP, check backend.');
                    }
                    btnSendMobileOtp.innerText = 'Send SMS Code';
                }
            } catch (error) {
                // console.error('OTP Send error:', error);
                alert('Connection error.');
                btnSendMobileOtp.innerText = 'Send SMS Code';
            }
        });
    }

    const mobileOtpInputs = document.querySelectorAll('.mobile-otp');
    if (mobileOtpInputs.length > 0) {
        const lastBox = mobileOtpInputs[5];
        lastBox.addEventListener('input', async () => {
            if (lastBox.value.length === 1) {
                let otpCode = Array.from(mobileOtpInputs).map(input => input.value).join('');
                const mobileInput = document.getElementById('mobile');
                const mobile = '+91' + mobileInput.value;
                const apiUrl = `${API_BASE_URL}/security/otp/verify-mobile`;
                
                try {
                    const response = await fetch(apiUrl, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ mobile: mobile, otpCode: otpCode })
                    });

                    if (response.ok && await response.json()) {
                        mobileVerifiedBadge.classList.remove('d-none');
                        mobileOtpSection.style.display = 'none';
                        isMobileVerified = true;
                    } else {
                        // console.log("OTP was wrong", response);
                        alert('Invalid SMS Code. Please try again.');
                        mobileOtpInputs.forEach(i => i.value = '');
                        mobileOtpInputs[0].focus();
                        isMobileVerified = false;
                    }
                } catch (error) {
                    console.error(error);
                }
            }
        });
    }

    if (btnSendEmailOtp) {
        btnSendEmailOtp.addEventListener('click', async () => {
            const emailInput = document.getElementById('email');
            if (!emailInput || !emailInput.checkValidity() || emailInput.value.trim() === '') {
                alert('Please enter a valid email address in the Personal Details section first.');
                return;
            }
            
            const apiUrl = `${API_BASE_URL}/security/otp/send-email?email=${encodeURIComponent(emailInput.value)}`;
            try {
                btnSendEmailOtp.innerText = 'Sending...';
                const response = await fetch(apiUrl, { method: 'POST' });

                if (response.ok) {
                    btnSendEmailOtp.innerText = 'Sent!';
                    btnSendEmailOtp.classList.replace('btn-outline-primary', 'btn-success');
                    emailOtpSection.style.display = 'block';
                } else {
                    try {
                        const errorData = await response.json();
                        alert(`Error: ${errorData.error || errorData.message || 'Failed to send Email OTP.'}`);
                    } catch (e) {
                        alert('Failed to send Email OTP. Please check backend logs.');
                    }
                    btnSendEmailOtp.innerText = 'Send Email Code';
                }
            } catch (error) {
                console.error('Email OTP Send error:', error);
                alert('Connection error.');
                btnSendEmailOtp.innerText = 'Send Email Code';
            }
        });
    }

    const emailOtpInputs = document.querySelectorAll('.email-otp');
    if (emailOtpInputs.length > 0) {
        const lastBox = emailOtpInputs[5];
        lastBox.addEventListener('input', async () => {
            if (lastBox.value.length === 1) {
                let otpCode = Array.from(emailOtpInputs).map(input => input.value).join('');
                const emailInput = document.getElementById('email');
                const apiUrl = `${API_BASE_URL}/security/otp/verify-email`;
                
                try {
                    const response = await fetch(apiUrl, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ email: emailInput.value, otpCode: otpCode })
                    });

                    if (response.ok && await response.json()) {
                        emailVerifiedBadge.classList.remove('d-none');
                        emailOtpSection.style.display = 'none';
                        isEmailVerified = true;
                    } else {
                        alert('Invalid Email Code. Please try again.');
                        emailOtpInputs.forEach(i => i.value = '');
                        emailOtpInputs[0].focus();
                        isEmailVerified = false;
                    }
                } catch (error) {
                    console.error('Email OTP Verify error:', error);
                }
            }
        });
    }

    if (registrationForm) {
        fetch(`${API_BASE_URL}/draft`, { credentials: 'include' })
            .then(res => {
                if (res.status === 200) return res.json();
                return null;
            })
            .then(draft => {
                if (draft) {
                    if (draft.name) document.getElementById('fullName').value = draft.name;
                    if (draft.email) document.getElementById('email').value = draft.email;
                    if (draft.mobile) {
                        document.getElementById('mobile').value = draft.mobile.startsWith('+91') ? draft.mobile.substring(3) : draft.mobile;
                    }
                    if (draft.hostId) document.getElementById('hostEmployee').value = draft.hostId;
                    if (draft.categoryCode) document.getElementById('visitorCategory').value = draft.categoryCode;
                    if (draft.expectedDate) document.getElementById('expectedDate').value = draft.expectedDate;
                    if (draft.purpose) document.getElementById('purpose').value = draft.purpose;
                }
            })
            .catch(err => console.error('Failed to load draft:', err));

        registrationForm.addEventListener('change', () => {
            const payload = {
                name: document.getElementById('fullName').value || null,
                email: document.getElementById('email').value || null,
                mobile: document.getElementById('mobile').value ? '+91' + document.getElementById('mobile').value : null,
                hostId: document.getElementById('hostEmployee').value || null,
                categoryCode: document.getElementById('visitorCategory').value || null,
                expectedDate: document.getElementById('expectedDate').value || null,
                purpose: document.getElementById('purpose').value || null
            };
            
            fetch(`${API_BASE_URL}/draft`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify(payload)
            }).catch(err => console.error('Failed to save draft:', err));
        });

        registrationForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!registrationForm.checkValidity()) return;
            
            if (!isMobileVerified || !isEmailVerified) {
                alert('Please verify both your mobile number and email address before submitting.');
                return;
            }

            const payload = {
                name: document.getElementById('fullName').value,
                email: document.getElementById('email').value,
                mobile: '+91' + document.getElementById('mobile').value,
                employeeId: document.getElementById('hostEmployee').value,
                categoryCode: document.getElementById('visitorCategory').value,
                expectedDate: document.getElementById('expectedDate').value,
                purpose: document.getElementById('purpose').value,
                company: "N/A", // From dynamic field if present
                idNumber: "12345" // Mock for now, would typically come from dynamic field or file upload OCR
            };

            try {
                const response = await fetch(`${API_BASE_URL}/visitors/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (response.ok) {
                    const data = await response.json();
                    
                    fetch(`${API_BASE_URL}/draft`, { method: 'DELETE', credentials: 'include' }).catch(() => {});
                    
                    // Hide the form and show the Digital Pass
                    document.getElementById('registrationForm').parentElement.parentElement.style.display = 'none';
                    const passContainer = document.getElementById('passContainer');
                    if (passContainer) {
                        passContainer.style.display = 'block';
                        document.getElementById('passVisitId').innerText = `VMS-${data.id.toString().padStart(4, '0')}`;
                        document.getElementById('passVisitorName').innerText = data.visitorName;
                        document.getElementById('passHostName').innerText = data.hostName;
                        // Generate a dynamic QR code containing the visit ID
                        document.getElementById('passQrCode').src = `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=VMS-VISIT-${data.id}`;
                        // Show popup message
                        alert("Registration successful! This pass has been sent to your email.");
                    } else {
                        alert(`Registration successful! Your Visit ID is ${data.id}. Please check your gmail for the Pass.`);
                        window.location.href = 'index.html'; // Fallback
                    }
                } else {
                    const error = await response.json();
                    alert(`Registration failed: ${JSON.stringify(error)}`);
                }
            } catch (error) {
                console.error('Registration error:', error);
                alert('Connection error.');
            }
        });
    }

    const isDashboard = window.location.pathname.includes('dashboard.html');
    if (isDashboard) {
        const token = getToken();
        if (!token) {
            window.location.href = 'index.html'; // Redirect to login
        }

        const userDataStr = localStorage.getItem('vms_user');
        let userRole = 'ADMIN';
        let userName = 'Admin User';
        if (userDataStr) {
            try {
                const userData = JSON.parse(userDataStr);
                userRole = userData.role || 'ADMIN';
                userName = userData.name || 'Admin User';
            } catch (e) {
                console.error("Failed to parse user data", e);
            }
        }
        window.userRole = userRole;

        const roleTitle = document.getElementById('sidebar-role-title');
        if (roleTitle) roleTitle.textContent = `VMS ${userRole.charAt(0) + userRole.slice(1).toLowerCase()}`;
        document.title = `VMS | ${userRole.charAt(0) + userRole.slice(1).toLowerCase()} Dashboard`;

        const topbarAvatar = document.getElementById('topbar-avatar');
        if (topbarAvatar) {
            topbarAvatar.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}&background=4f46e5&color=fff`;
        }

        const applyRoleBasedUI = () => {
            const sidebarLinks = document.querySelectorAll('.nav-link-premium');
            sidebarLinks.forEach(link => {
                const targetView = link.getAttribute('data-target');
                if (!targetView) return;

                if (userRole === 'ADMIN') {
                    link.parentElement.classList.remove('d-none');
                } else if (userRole === 'RECEPTION') {
                    if (['reports-view', 'settings-view'].includes(targetView)) {
                        link.parentElement.classList.add('d-none');
                    } else {
                        link.parentElement.classList.remove('d-none');
                    }
                } else if (userRole === 'EMPLOYEE') {
                    if (['active-view', 'blacklist-view', 'reports-view', 'settings-view'].includes(targetView)) {
                        link.parentElement.classList.add('d-none');
                    } else {
                        link.parentElement.classList.remove('d-none');
                    }
                }
            });

            // Show scanner button only for RECEPTION
            const btnScanPass = document.getElementById('btnScanPass');
            if (btnScanPass && userRole === 'RECEPTION') {
                btnScanPass.classList.remove('d-none');
            }
        };
        applyRoleBasedUI();

        const topbarTitle = document.getElementById('topbar-title');
        const viewSections = document.querySelectorAll('.view-section');
        const navLinks = document.querySelectorAll('.sidebar .nav-link-premium');

        const showView = (viewId, title) => {
            viewSections.forEach(v => v.classList.add('d-none'));
            const targetView = document.getElementById(viewId);
            if(targetView) {
                targetView.classList.remove('d-none');
            }
            if(topbarTitle) {
                topbarTitle.innerText = title;
            }
            
            navLinks.forEach(link => link.classList.remove('active'));
        };

        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                const text = link.innerText.trim();
                
                if (text === 'Logout') return; // Handled by standard link
                
                e.preventDefault();
                link.classList.add('active');

                switch(text) {
                    case 'Dashboard':
                        showView('dashboard-view', 'Live Dashboard');
                        fetchActiveVisitors();
                        break;
                    case 'Active Visitors':
                        showView('dashboard-view', 'Live Dashboard');
                        fetchActiveVisitors();
                        break;
                    case 'Visit History':
                        showView('history-view', 'Visit History');
                        fetchHistory();
                        break;
                    case 'Approvals':
                        showView('approvals-view', 'Pending Approvals');
                        fetchApprovals();
                        break;
                    case 'Blacklist':
                        showView('blacklist-view', 'Blacklist Management');
                        fetchBlacklist();
                        break;
                    case 'Reports':
                        showView('reports-view', 'Analytics & Reports');
                        fetchReports();
                        break;
                    case 'Settings':
                        showView('settings-view', 'System Settings');
                        break;
                    default:
                        showView('dashboard-view', 'Live Dashboard');
                }
            });
        });

        const fetchActiveVisitors = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/visitors/active`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });

                if (response.ok) {
                    const data = await response.json();
                    renderVisitorsTable(data.content);
                }
            } catch (error) {
                console.error("Failed to fetch active visitors", error);
            }
        };

        const renderVisitorsTable = (visitors) => {
            const tbody = document.querySelector('#dashboard-view table tbody');
            if (!tbody) return;
            
            if (visitors.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No active visitors right now</td></tr>';
                return;
            }

            let html = '';
            visitors.forEach(v => {
                const avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(v.visitorName)}&background=random`;
                html += `
                    <tr>
                        <td>
                            <div class="d-flex align-items-center">
                                <img src="${avatar}" class="rounded me-3" width="40" height="40">
                                <div>
                                    <div class="fw-bold">${v.visitorName}</div>
                                    <div class="small text-muted">${v.visitorMobile}</div>
                                </div>
                            </div>
                        </td>
                        <td><span class="badge bg-primary-subtle text-primary px-2 py-1 rounded-pill">${v.categoryDisplayName}</span></td>
                        <td><div class="fw-medium">${v.hostName}</div></td>
                        <td>${new Date(v.expectedDate).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
                        <td class="text-end">
                            ${window.userRole !== 'EMPLOYEE' ? (v.status === 'CHECKED_IN' ? `<button onclick="checkoutVisitor(${v.id})" class="btn btn-sm btn-light border hover-lift"><i class="bi bi-box-arrow-right text-danger"></i> Check Out</button>` : `<button onclick="checkinVisitor(${v.id})" class="btn btn-sm btn-light border hover-lift"><i class="bi bi-box-arrow-in-right text-success"></i> Check In</button>`) : ''}
                            ${window.userRole !== 'EMPLOYEE' ? `<button onclick="blockVisitorQuick('${v.visitorMobile}', '${v.visitorName.replace(/'/g, "\\'")}')" class="btn btn-sm btn-outline-danger ms-2"><i class="bi bi-slash-circle"></i> Block</button>` : ''}
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        };

        window.checkoutVisitor = async (visitId) => {
            if (!confirm('Are you sure you want to check out this visitor?')) return;
            try {
                const response = await fetch(`${API_BASE_URL}/visitors/${visitId}/checkout`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    fetchActiveVisitors();
                } else {
                    alert('Failed to checkout. Ensure visitor is checked in.');
                }
            } catch(e) {
                console.error(e);
            }
        };

        window.checkinVisitor = async (visitId) => {
            if (!confirm('Are you sure you want to check in this visitor?')) return;
            try {
                const response = await fetch(`${API_BASE_URL}/visitors/${visitId}/checkin`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    fetchActiveVisitors();
                } else {
                    alert('Failed to checkin. Ensure visitor is approved.');
                }
            } catch(e) {
                console.error(e);
            }
        };

        window.blockVisitorQuick = async (mobile, name) => {
            const reason = prompt(`Reason for blocking ${name} (${mobile}):`);
            if (!reason) return;
            try {
                const res = await fetch(`${API_BASE_URL}/blacklist`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                    body: JSON.stringify({ mobileNumber: mobile, idNumber: 'N/A', reason: reason })
                });
                if(res.ok) {
                    alert('Visitor added to blacklist successfully.');
                    if(document.getElementById('blacklist-view').classList.contains('d-none') === false) {
                        fetchBlacklist();
                    }
                } else {
                    const err = await res.text();
                    alert('Error adding to blacklist: ' + err);
                }
            } catch (err) {
                console.error(err);
            }
        };

        const fetchHistory = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/visitors/history?size=50`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    renderHistoryTable(data.content);
                }
            } catch (error) {
                console.error(error);
            }
        };

        const renderHistoryTable = (visitors) => {
            const tbody = document.getElementById('history-table-body');
            if (!tbody) return;
            if (visitors.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4">No history records found</td></tr>';
                return;
            }
            let html = '';
            visitors.forEach(v => {
                const avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(v.visitorName)}&background=random`;
                html += `
                    <tr>
                        <td>
                            <div class="d-flex align-items-center">
                                <img src="${avatar}" class="rounded me-3" width="30" height="30">
                                <div><div class="fw-bold">${v.visitorName}</div></div>
                            </div>
                        </td>
                        <td>${v.visitorMobile}</td>
                        <td><span class="badge bg-secondary-subtle text-secondary px-2 py-1 rounded-pill">${v.categoryDisplayName}</span></td>
                        <td>${v.hostName}</td>
                        <td><span class="badge bg-info-subtle text-info px-2 py-1">${v.status}</span></td>
                        <td class="text-end">${new Date(v.expectedDate).toLocaleString([], {dateStyle:'short', timeStyle:'short'})}</td>
                        <td class="text-end">
                            ${window.userRole !== 'EMPLOYEE' ? `<button onclick="blockVisitorQuick('${v.visitorMobile}', '${v.visitorName.replace(/'/g, "\\'")}')" class="btn btn-sm btn-outline-danger ms-2"><i class="bi bi-slash-circle"></i> Block</button>` : ''}
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        };

        const fetchApprovals = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/visitors/approvals/pending`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    renderApprovalsTable(data.content);
                }
            } catch (error) {
                console.error(error);
            }
        };

        const renderApprovalsTable = (visitors) => {
            const tbody = document.getElementById('approvals-table-body');
            if (!tbody) return;
            if (visitors.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4">No pending approvals</td></tr>';
                return;
            }
            let html = '';
            visitors.forEach(v => {
                const avatar = `https://ui-avatars.com/api/?name=${encodeURIComponent(v.visitorName)}&background=random`;
                html += `
                    <tr>
                        <td>
                            <div class="d-flex align-items-center">
                                <img src="${avatar}" class="rounded me-3" width="30" height="30">
                                <div><div class="fw-bold">${v.visitorName}</div></div>
                            </div>
                        </td>
                        <td>${v.visitorMobile}</td>
                        <td><span class="badge bg-warning-subtle text-warning px-2 py-1 rounded-pill">${v.categoryDisplayName}</span></td>
                        <td>${new Date(v.expectedDate).toLocaleString([], {dateStyle:'short', timeStyle:'short'})}</td>
                        <td class="text-end">
                            <button onclick="approveVisit(${v.id})" class="btn btn-sm btn-success me-1">Approve</button>
                            <button onclick="rejectVisit(${v.id})" class="btn btn-sm btn-danger">Reject</button>
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        };

        window.approveVisit = async (id) => {
            if(!confirm('Approve this visit?')) return;
            await fetch(`${API_BASE_URL}/visitors/${id}/approve`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
            fetchApprovals();
        };

        window.rejectVisit = async (id) => {
            if(!confirm('Reject this visit?')) return;
            await fetch(`${API_BASE_URL}/visitors/${id}/reject`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
            fetchApprovals();
        };

        const fetchBlacklist = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/blacklist`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    renderBlacklistTable(data.content);
                }
            } catch (error) {
                console.error(error);
            }
        };

        const renderBlacklistTable = (items) => {
            const tbody = document.getElementById('blacklist-table-body');
            if (!tbody) return;
            if (items.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4">No blacklisted entries</td></tr>';
                return;
            }
            let html = '';
            items.forEach(b => {
                if(!b.isActive) return;
                html += `
                    <tr>
                        <td>${b.mobileNumber || '-'}</td>
                        <td>${b.idNumber || '-'}</td>
                        <td>${b.reason}</td>
                        <td>${new Date(b.createdAt).toLocaleDateString()}</td>
                        <td class="text-end">
                            <button onclick="removeFromBlacklist(${b.id})" class="btn btn-sm btn-outline-danger">Remove</button>
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        };

        window.removeFromBlacklist = async (id) => {
            if(!confirm('Remove from blacklist?')) return;
            await fetch(`${API_BASE_URL}/blacklist/${id}`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` }});
            fetchBlacklist();
        };

        const blacklistForm = document.getElementById('blacklist-form');
        if(blacklistForm) {
            blacklistForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const mobile = document.getElementById('blacklist-mobile').value;
                const idNum = document.getElementById('blacklist-id').value;
                const reason = document.getElementById('blacklist-reason').value;

                if (!mobile && !idNum) {
                    alert('Please provide either Mobile Number or ID Number');
                    return;
                }

                try {
                    const res = await fetch(`${API_BASE_URL}/blacklist`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                        body: JSON.stringify({ mobileNumber: mobile, idNumber: idNum, reason })
                    });
                    if(res.ok) {
                        alert('Added to blacklist');
                        blacklistForm.reset();
                        fetchBlacklist();
                    } else {
                        alert('Error adding to blacklist');
                    }
                } catch (err) {
                    console.error(err);
                }
            });
        }

        let reportChartInstance = null;
        let dashboardChartInstance = null;
        const fetchReports = async () => {
            try {
                const response = await fetch(`${API_BASE_URL}/reports/dashboard-stats`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const stats = await response.json();
                    
                    const labels = [...stats.labels].reverse();
                    const data = [...stats.data].reverse();

                    const chartConfig = {
                        type: 'bar',
                        data: {
                            labels: labels,
                            datasets: [{
                                label: 'Visitors',
                                data: data,
                                backgroundColor: '#4f46e5',
                                borderRadius: 6,
                                barThickness: 20
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: { legend: { display: false } },
                            scales: {
                                y: { beginAtZero: true, grid: { borderDash: [2, 4] }, ticks: { precision: 0 } },
                                x: { grid: { display: false } }
                            }
                        }
                    };

                    const mainCtx = document.getElementById('mainReportChart');
                    if (mainCtx) {
                        if (reportChartInstance) {
                            reportChartInstance.destroy();
                        }
                        reportChartInstance = new Chart(mainCtx.getContext('2d'), chartConfig);
                    }

                    const dashCtx = document.getElementById('visitorChart');
                    if (dashCtx) {
                        if (dashboardChartInstance) {
                            dashboardChartInstance.destroy();
                        }
                        dashboardChartInstance = new Chart(dashCtx.getContext('2d'), chartConfig);
                    }
                    const statValues = document.querySelectorAll('.stat-value');
                    if(statValues.length >= 4) {
                        statValues[0].innerText = stats.insidePremises;
                        statValues[1].innerText = stats.expectedToday;
                        statValues[2].innerText = stats.pendingApprovals;
                        statValues[3].innerText = stats.blacklistHits;
                    }
                }
            } catch (error) {
                console.error(error);
            }
        };

        fetchActiveVisitors();
        fetchReports();
        
        setInterval(() => {
            if(!document.getElementById('dashboard-view').classList.contains('d-none')) {
                fetchActiveVisitors();
                fetchReports(); // to keep dashboard numbers fresh
            }
        }, 30000);
    }
});
