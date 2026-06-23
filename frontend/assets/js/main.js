/**
 * VMS Main JavaScript file
 * Handles Form Validations, Dynamic Fields, and OTP Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    
    // 1. Form Validation Initialization
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

    // 2. OTP Auto-Advance Logic
    const otpInputs = document.querySelectorAll('.otp-box');
    if (otpInputs.length > 0) {
        otpInputs.forEach((input, index) => {
            input.addEventListener('input', (e) => {
                // Ensure only numbers are entered
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
            
            // Handle paste event for OTP
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

    // 3. Dynamic Field Toggling based on Visitor Category
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

    // 4. File Upload Preview (ID & Photo)
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

    // ==========================================
    // API INTEGRATION LOGIC (Day 18)
    // ==========================================
    const API_BASE_URL = 'http://localhost:8080/api/v1';

    // Helper: Get JWT Token
    const getToken = () => localStorage.getItem('vms_jwt');

    // 5. Login Form Handling
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

    // 6. Registration Flow & OTP Handling
    const registrationForm = document.getElementById('registrationForm');
    const btnSendOtp = document.getElementById('btnSendOtp');
    const otpSection = document.getElementById('otpSection');
    const otpSuccessMsg = document.getElementById('otpSuccessMsg');
    let isOtpVerified = false;

    if (btnSendOtp) {
        btnSendOtp.addEventListener('click', async () => {
            const mobileInput = document.getElementById('mobile');
            if (!mobileInput.checkValidity()) {
                alert('Please enter a valid 10-digit mobile number.');
                return;
            }

            const mobile = '+91' + mobileInput.value;
            try {
                const response = await fetch(`${API_BASE_URL}/security/otp/send?mobile=${encodeURIComponent(mobile)}`, {
                    method: 'POST'
                });

                if (response.ok) {
                    btnSendOtp.innerText = 'Sent!';
                    btnSendOtp.classList.replace('btn-primary', 'btn-success');
                    otpSection.style.display = 'block';
                } else {
                    alert('Failed to send OTP. Please try again.');
                }
            } catch (error) {
                console.error('OTP Send error:', error);
                alert('Connection error.');
            }
        });
    }

    if (otpInputs.length > 0) {
        const lastBox = otpInputs[5];
        lastBox.addEventListener('input', async () => {
            if (lastBox.value.length === 1) {
                // Collect full OTP
                let otpCode = Array.from(otpInputs).map(input => input.value).join('');
                const mobile = '+91' + document.getElementById('mobile').value;

                try {
                    const response = await fetch(`${API_BASE_URL}/security/otp/verify`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ mobile, otpCode })
                    });

                    if (response.ok) {
                        const isValid = await response.json();
                        if (isValid) {
                            otpSuccessMsg.classList.remove('d-none');
                            otpSuccessMsg.innerHTML = '<i class="bi bi-check-circle"></i> Mobile Verified';
                            otpSuccessMsg.classList.add('text-success');
                            otpSuccessMsg.classList.remove('text-danger');
                            isOtpVerified = true;
                        } else {
                            otpSuccessMsg.classList.remove('d-none');
                            otpSuccessMsg.innerHTML = '<i class="bi bi-x-circle"></i> Invalid OTP';
                            otpSuccessMsg.classList.remove('text-success');
                            otpSuccessMsg.classList.add('text-danger');
                            isOtpVerified = false;
                        }
                    }
                } catch (error) {
                    console.error('OTP Verify error:', error);
                }
            }
        });
    }

    if (registrationForm) {
        registrationForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!registrationForm.checkValidity()) return;
            
            if (!isOtpVerified) {
                alert('Please verify your mobile number with OTP first.');
                return;
            }

            // Gather Registration Data
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
                    alert(`Registration successful! Your Visit ID is ${data.id}. Please check your phone for the Pass.`);
                    window.location.href = 'index.html'; // Redirect to home
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

});
