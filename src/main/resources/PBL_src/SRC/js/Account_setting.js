document.addEventListener('DOMContentLoaded', () => {
    const accountLink = document.getElementById('account-link');
    const accountPopup = document.getElementById('accountPopup');
    const accountClose = document.getElementById('accountClose');
    const accountContainer = document.getElementById('accountContainer');
    
    // Change Password Elements
    const changePasswordPopup = document.getElementById('changePasswordPopup');
    const submitChangePasswordBtn = document.getElementById('submitChangePasswordBtn');
    const cancelChangePasswordBtn = document.getElementById('cancelChangePasswordBtn');
    const oldPasswordInput = document.getElementById('oldPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmNewPasswordInput = document.getElementById('confirmNewPassword');

    const API_URL = 'http://localhost:8910';

    if (accountLink) {
        accountLink.addEventListener('click', async (e) => {
            e.preventDefault();
            accountPopup.style.display = 'flex';
            await fetchAndRenderAccountInfo();
        });
    }

    if (accountClose) {
        accountClose.addEventListener('click', () => {
            accountPopup.style.display = 'none';
        });
    }

    // --- Change Password Logic ---
    if (cancelChangePasswordBtn) {
        cancelChangePasswordBtn.addEventListener('click', () => {
            changePasswordPopup.style.display = 'none';
            clearPasswordInputs();
        });
    }

    if (submitChangePasswordBtn) {
        submitChangePasswordBtn.addEventListener('click', async () => {
            const oldPassword = oldPasswordInput.value;
            const newPassword = newPasswordInput.value;
            const confirmNewPassword = confirmNewPasswordInput.value;

            if (!oldPassword || !newPassword || !confirmNewPassword) {
                alert("Vui lòng nhập đầy đủ thông tin.");
                return;
            }

            if (newPassword !== confirmNewPassword) {
                alert("Mật khẩu mới không khớp.");
                return;
            }
            
            if (newPassword.length < 6) {
                alert("Mật khẩu mới phải có ít nhất 6 ký tự.");
                return;
            }

            try {
                const token = localStorage.getItem('token');
                const response = await fetch(`${API_URL}/api/user/change-password`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        oldPassword: oldPassword,
                        newPassword: newPassword
                    })
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    alert("Đổi mật khẩu thành công!");
                    changePasswordPopup.style.display = 'none';
                    clearPasswordInputs();
                } else {
                    alert("Lỗi: " + (result.error || "Đổi mật khẩu thất bại."));
                }
            } catch (error) {
                console.error("Error changing password:", error);
                alert("Đã xảy ra lỗi khi đổi mật khẩu.");
            }
        });
    }

    function clearPasswordInputs() {
        oldPasswordInput.value = '';
        newPasswordInput.value = '';
        confirmNewPasswordInput.value = '';
    }

    async function fetchAndRenderAccountInfo() {
        accountContainer.innerHTML = '<div class="loading-spinner"></div>';
        const token = localStorage.getItem('token');
        const playerName = localStorage.getItem('playerName');
        const playerId = localStorage.getItem('playerId');
        const isGuest = !playerId || playerId.startsWith('guest_');

        if (isGuest) {
            renderGuestInfo(playerName);
            return;
        }

        if (!token) {
            renderGuestInfo(playerName); // Fallback for safety
            return;
        }

        try {
            const response = await fetch(`${API_URL}/api/user/me`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    // Token invalid, logout
                    logout();
                }
                throw new Error('Failed to fetch user data');
            }

            const result = await response.json();
            if (result.success) {
                renderUserInfo(result.data);
            } else {
                accountContainer.innerHTML = `<p class="error-message">Lỗi: ${result.error}</p>`;
            }

        } catch (error) {
            console.error('Error fetching account info:', error);
            accountContainer.innerHTML = `<p class="error-message">Không thể tải thông tin tài khoản.</p>`;
        }
    }

    function renderGuestInfo(playerName) {
        accountContainer.innerHTML = `
            <div class="account-info-section">
                <p><strong>Tên:</strong> ${playerName || 'Guest'}</p>
                <p>Bạn đang chơi với tư cách khách.</p>
            </div>
            <div class="account-actions">
                <button id="logoutBtn" class="btn-action btn-danger">Đăng xuất</button>
            </div>
        `;
        addLogoutListener();
    }

    function renderUserInfo(user) {
        accountContainer.innerHTML = `
            <img src="${user.avatarUrl || '../../PBL4_imgs/icon/logo.png'}" alt="Avatar" class="account-avatar-large">
            <div class="account-info-section">
                <p><strong>ID:</strong> ${user.userId}</p>
                <p><strong>Tên người dùng:</strong> ${user.userName}</p>
                <p><strong>Email:</strong> ${user.email || 'Chưa cập nhật'}</p>
                <p><strong>Elo:</strong> ${user.eloRating}</p>
            </div>
            <div class="account-actions">
                <button id="changePasswordBtn" class="btn-action">Đổi mật khẩu</button>
                <button id="logoutBtn" class="btn-action btn-danger">Đăng xuất</button>
            </div>
        `;
        addLogoutListener();
        
        const changePasswordBtn = document.getElementById('changePasswordBtn');
        if(changePasswordBtn) {
            changePasswordBtn.addEventListener('click', () => {
                // Ẩn popup tài khoản, hiện popup đổi mật khẩu
                // accountPopup.style.display = 'none'; // Tùy chọn: có thể giữ popup tài khoản ở dưới
                changePasswordPopup.style.display = 'flex';
            });
        }
    }

    function addLogoutListener() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', logout);
        }
    }

    function logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('playerName');
        localStorage.removeItem('playerId');
        window.location.href = 'MainLogin.html';
    }
});
