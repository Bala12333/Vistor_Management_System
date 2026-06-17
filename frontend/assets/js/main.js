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
});
