const accountLink = document.getElementById("account-link");
const centerContent = document.querySelector(".center");
let originalBoardContent = null;

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

function showBoard() {
    if (centerContent && originalBoardContent) {
        centerContent.innerHTML = originalBoardContent;
        centerContent.classList.remove('account-view');
    }
}

// ========== LẤY DỮ LIỆU TÀI KHOẢN ==========
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

            // Cập nhật avatar hiện tại
            const avatarImg = document.getElementById('currentAvatar');
            if (avatarImg && userData.avatarUrl) {
                avatarImg.src = userData.avatarUrl;
            }
        }
    } catch (error) {
        console.error('Lỗi tải dữ liệu:', error);
    }
}

// ========== LƯU CHỈ AVATAR (khi bấm Xác nhận trong modal) ==========
// 1. Chỉ đổi avatar (khi bấm "Xác nhận" trong modal)
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

            // Cập nhật ngay avatar ở trang Cài đặt
            document.getElementById('currentAvatar').src = selectedAvatarUrl;

            // PHÁT SỰ KIỆN CHO SIDEBAR
            window.dispatchEvent(new CustomEvent('userInfoUpdated', {
                detail: { avatarUrl: selectedAvatarUrl }
            }));

            // Cập nhật localStorage
            localStorage.setItem("avatarUrl", selectedAvatarUrl);

            // Reset + đóng modal
            selectedAvatarUrl = null;
            closeAvatarModal();
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// 2. Lưu username + email + avatar (nếu có)
async function saveAllChanges() {
    const playerId = localStorage.getItem("playerId");
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();

    if (!username || !email) {
        alert('Vui lòng nhập đầy đủ Tên người chơi và Email!');
        return;
    }

    const updateData = { playerId, username, email };
    if (selectedAvatarUrl) updateData.avatarUrl = selectedAvatarUrl; // thêm nếu có

    try {
        const res = await fetch(`${API_URL}/api/account/update`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updateData)
        });
        const result = await res.json();

        if (result.success) {
            alert('Lưu thông tin thành công!');

            const finalUsername = username;
            const finalAvatarUrl = selectedAvatarUrl || document.getElementById('currentAvatar').src;

            // Cập nhật giao diện trang Cài đặt
            document.getElementById('currentAvatar').src = finalAvatarUrl;

            // PHÁT SỰ KIỆN CHO SIDEBAR
            window.dispatchEvent(new CustomEvent('userInfoUpdated', {
                detail: {
                    username: finalUsername,
                    avatarUrl: finalAvatarUrl
                }
            }));

            // Cập nhật localStorage
            localStorage.setItem("playerName", finalUsername);
            if (finalAvatarUrl) localStorage.setItem("avatarUrl", finalAvatarUrl);

            selectedAvatarUrl = null; // reset để lần sau đổi avatar vẫn được
            await fetchAndPopulateAccountData();
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// ========== AVATAR MODAL ==========
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
    // Highlight avatar hiện tại nếu nó nằm trong danh sách preset
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
    document.querySelectorAll('.avatar-option').forEach(i => i.classList.remove('selected'));
}

// Khi bấm "Xác nhận" trong modal → lưu avatar ngay
function confirmAvatarSelection() {
    if (selectedAvatarUrl) {
        saveAvatarOnly(); // Lưu luôn, không cần chờ "Lưu thay đổi"
    } else {
        alert('Vui lòng chọn một ảnh đại diện!');
    }
}

// ========== ĐỔI MẬT KHẨU ==========
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
            alert(data.message || 'Đổi mật khẩu thất bại');
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

// ========== GẮN SỰ KIỆN ==========
function attachEventListeners() {
    // Click avatar để mở modal
    const avatarImg = document.getElementById('currentAvatar');
    if (avatarImg) {
        avatarImg.style.cursor = 'pointer';
        avatarImg.title = 'Click để đổi ảnh đại diện';
        avatarImg.onclick = openAvatarModal;
    }

    // Các nút trong modal
    document.getElementById('btnSaveAvatar')?.addEventListener('click', confirmAvatarSelection);
    document.getElementById('btnCancelAvatar')?.addEventListener('click', closeAvatarModal);

    // Nút chính
    document.getElementById('btnCancel')?.addEventListener('click', showBoard);
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

// ========== LOAD TRANG ==========
async function loadAccountSettings() {
    try {
        const response = await fetch('Account_setting.html');
        const html = await response.text();
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;

        const accountContent = tempDiv.querySelector('.settings-page');
        const avatarModal = tempDiv.querySelector('#avatarModal');
        const passwordModal = tempDiv.querySelector('#passwordModal');

        if (!accountContent) throw new Error('Không tìm thấy .settings-page');

        centerContent.innerHTML = accountContent.outerHTML;
        centerContent.classList.add('account-view');
        if (avatarModal) centerContent.appendChild(avatarModal);
        if (passwordModal) centerContent.appendChild(passwordModal);

        // Load CSS
        if (!document.querySelector('link[href*="Account_setting.css"]')) {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = '../css/Account_setting.css';
            document.head.appendChild(link);
        }

        attachEventListeners();        // Gắn event NGAY, đặc biệt là click avatar
        await fetchAndPopulateAccountData();

    } catch (error) {
        console.error('Lỗi load trang:', error);
        alert('Lỗi tải trang tài khoản: ' + error.message);
    }
}

// ========== KHỞI TẠO ==========
if (accountLink && centerContent) {
    originalBoardContent = centerContent.innerHTML;
    accountLink.addEventListener('click', e => {
        e.preventDefault();
        loadAccountSettings();
    });
    console.log('Account feature ready!');
}