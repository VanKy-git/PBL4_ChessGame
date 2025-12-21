const accountLink = document.getElementById("account-link");
const accountPopup = document.getElementById("accountPopup");
const accountContainer = document.getElementById("accountContainer");
const accountClose = document.getElementById("accountClose");

const API_URL = "http://localhost:8910";

const avatarPresets = [
    '../../PBL4_imgs/icon/man.png',
    '../../PBL4_imgs/icon/boy1.png',
    '../../PBL4_imgs/icon/boy2.png',
    '../../PBL4_imgs/icon/boy3.png',
    '../../PBL4_imgs/icon/cat.png',
    '../../PBL4_imgs/icon/dog.png',
    '../../PBL4_imgs/icon/girl.png',
    '../../PBL4_imgs/icon/girl1.png',
    '../../PBL4_imgs/icon/girl2.png',
    '../../PBL4_imgs/icon/user.png'
];

let selectedAvatarUrl = null;

// --- HÀM TẢI NỘI DUNG VÀO POPUP ---
async function loadAccountContent() {
    try {
        const response = await fetch('Account_setting.html');
        if (!response.ok) {
            throw new Error('Failed to load account settings content');
        }
        const content = await response.text();
        accountContainer.innerHTML = content;
        
        // Gắn các listener sau khi nội dung được tải
        attachAccountEventListeners();
        // Tải dữ liệu tài khoản
        fetchAndPopulateAccountData();

    } catch (error) {
        console.error('[Account_setting.js] Error loading content:', error);
        accountContainer.innerHTML = '<p style="color: #e74c3c;">Không thể tải nội dung tài khoản.</p>';
    }
}

// --- LISTENER CHO LINK VÀ NÚT ĐÓNG ---
if (accountLink) {
    accountLink.addEventListener('click', (e) => {
        e.preventDefault();
        accountPopup.style.display = 'flex';
        accountContainer.innerHTML = 'Đang tải...';
        loadAccountContent();
    });
}

if(accountClose) {
    accountClose.addEventListener('click', () => {
        accountPopup.style.display = 'none';
    });
}

// --- CÁC HÀM LOGIC ---

async function fetchAndPopulateAccountData() {
    const currentUserId = localStorage.getItem("playerId") || "unknown";

    try {
        const response = await fetch(`${API_URL}/api/account?playerId=${currentUserId}`);
        const data = await response.json();

        if (response.ok && data.success) {
            const userData = data.data;

            document.getElementById('id_player').textContent = `ID:#${userData.userId || userData.playerId || currentUserId}`;
            document.getElementById('username').value = userData.userName || userData.username || '';
            document.getElementById('email').value = userData.email || '';
            document.getElementById('elo').value = userData.elo || userData.eloRating || 1500;
            document.getElementById('winCount').value = userData.winCount || 0;
            document.getElementById('lossCount').value = userData.lossCount || 0;
            document.getElementById('createdAt').value = userData.createdAt || userData.createAt || '';

            const avatarImg = document.getElementById('currentAvatar');
            if (avatarImg && userData.avatarUrl) {
                avatarImg.src = userData.avatarUrl;
            }
        }
    } catch (error) {
        console.error('Lỗi tải dữ liệu:', error);
    }
}

async function saveAllChanges() {
    const playerId = localStorage.getItem("playerId");
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();

    if (!username || !email) {
        alert('Vui lòng nhập đầy đủ Tên người chơi và Email!');
        return;
    }

    const updateData = { playerId, username, email };
    if (selectedAvatarUrl) updateData.avatarUrl = selectedAvatarUrl;

    try {
        const res = await fetch(`${API_URL}/api/account/update`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updateData)
        });
        const result = await res.json();

        if (result.success) {
            alert('Lưu thông tin thành công!');
            localStorage.setItem("playerName", username);
            if (selectedAvatarUrl) localStorage.setItem("avatarUrl", selectedAvatarUrl);
            
            // Cập nhật avatar hiển thị ngay lập tức
            const avatarImg = document.getElementById('currentAvatar');
            if (avatarImg && selectedAvatarUrl) {
                avatarImg.src = selectedAvatarUrl;
            }
            selectedAvatarUrl = null; // Reset
        } else {
            alert('Lưu thông tin thất bại: ' + (result.error || 'Lỗi không xác định'));
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// --- AVATAR MODAL ---
function generateAvatarGrid() {
    const grid = document.getElementById('avatarGrid');
    if (!grid) return;
    grid.innerHTML = '';

    avatarPresets.forEach(url => {
        const img = document.createElement('img');
        img.src = url;
        img.className = 'avatar-option';
        img.onclick = () => {
            document.querySelectorAll('.avatar-option').forEach(i => i.classList.remove('selected'));
            img.classList.add('selected');
            selectedAvatarUrl = url;
        };
        grid.appendChild(img);
    });
}

function openAvatarModal() {
    generateAvatarGrid();
    const currentSrc = document.getElementById('currentAvatar').src;
    const currentOption = Array.from(document.querySelectorAll('.avatar-option')).find(img => img.src === currentSrc);
    if (currentOption) {
        currentOption.classList.add('selected');
        selectedAvatarUrl = currentSrc;
    }
    document.getElementById('avatarModal').classList.add('active');
}

function closeAvatarModal() {
    document.getElementById('avatarModal').classList.remove('active');
    selectedAvatarUrl = null;
}

function confirmAvatarSelection() {
    if (selectedAvatarUrl) {
        // Chỉ cập nhật biến tạm, chưa lưu lên server cho đến khi bấm "Lưu thay đổi"
        // Hoặc có thể lưu ngay nếu muốn (như code cũ)
        // Ở đây tôi chọn cách lưu ngay để giống logic cũ
        saveAvatarOnly();
    } else {
        alert('Vui lòng chọn một ảnh đại diện!');
    }
}

async function saveAvatarOnly() {
    if (!selectedAvatarUrl) return;

    const playerId = localStorage.getItem("playerId");
    const updateData = { playerId, avatarUrl: selectedAvatarUrl };

    try {
        const res = await fetch(`${API_URL}/api/account/update`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updateData)
        });
        const result = await res.json();

        if (result.success) {
            alert('Đổi ảnh đại diện thành công!');
            document.getElementById('currentAvatar').src = selectedAvatarUrl;
            localStorage.setItem("avatarUrl", selectedAvatarUrl);
            closeAvatarModal();
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// --- ĐỔI MẬT KHẨU ---
async function handlePasswordChange(e) {
    e.preventDefault();
    const oldPassword = document.getElementById('oldPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (newPassword !== confirmPassword) {
        alert('Mật khẩu xác nhận không khớp!');
        return;
    }
    if (newPassword.length < 6) {
        alert('Mật khẩu phải có ít nhất 6 ký tự!');
        return;
    }

    const playerId = localStorage.getItem("playerId");
    try {
        const res = await fetch(`${API_URL}/api/account/change-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerId, oldPassword, newPassword })
        });
        const data = await res.json();
        if (data.success) {
            alert('Đổi mật khẩu thành công!');
            document.getElementById('passwordModal').classList.remove('active');
            document.getElementById('passwordForm').reset();
        } else {
            alert(data.error || 'Đổi mật khẩu thất bại');
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// --- GẮN SỰ KIỆN ---
function attachAccountEventListeners() {
    const currentAvatar = document.getElementById('currentAvatar');
    if (currentAvatar) {
        currentAvatar.style.cursor = 'pointer';
        currentAvatar.onclick = openAvatarModal;
    }

    document.getElementById('btnSaveAvatar')?.addEventListener('click', confirmAvatarSelection);
    document.getElementById('btnCancelAvatar')?.addEventListener('click', closeAvatarModal);
    
    document.getElementById('btnSaveChanges')?.addEventListener('click', saveAllChanges);
    
    document.getElementById('btnChangePassword')?.addEventListener('click', () => {
        document.getElementById('passwordModal').classList.add('active');
    });
    document.getElementById('btnCancelPassword')?.addEventListener('click', () => {
        document.getElementById('passwordModal').classList.remove('active');
        document.getElementById('passwordForm').reset();
    });
    document.getElementById('passwordForm')?.addEventListener('submit', handlePasswordChange);
}