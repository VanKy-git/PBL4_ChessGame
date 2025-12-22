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
            window.dispatchEvent(new CustomEvent('userInfoUpdated', {
                detail: { avatarUrl: selectedAvatarUrl }
            }));
            localStorage.setItem("avatarUrl", selectedAvatarUrl);
            selectedAvatarUrl = null;
            closeAvatarModal();
        }
    } catch (err) {
        alert('Lỗi kết nối!');
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
            const finalUsername = username;
            const finalAvatarUrl = selectedAvatarUrl || document.getElementById('currentAvatar').src;
            document.getElementById('currentAvatar').src = finalAvatarUrl;
            window.dispatchEvent(new CustomEvent('userInfoUpdated', {
                detail: {
                    username: finalUsername,
                    avatarUrl: finalAvatarUrl
                }
            }));
            localStorage.setItem("playerName", finalUsername);
            if (finalAvatarUrl) localStorage.setItem("avatarUrl", finalAvatarUrl);
            selectedAvatarUrl = null;
            await fetchAndPopulateAccountData();
        }
    } catch (err) {
        alert('Lỗi kết nối!');
    }
}

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
    document.querySelectorAll('.avatar-option').forEach(i => i.classList.remove('selected'));
}

function confirmAvatarSelection() {
    if (selectedAvatarUrl) {
        saveAvatarOnly();
    } else {
        alert('Vui lòng chọn một ảnh đại diện!');
    }
}

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

function attachEventListeners() {
    const avatarImg = document.getElementById('currentAvatar');
    if (avatarImg) {
        avatarImg.style.cursor = 'pointer';
        avatarImg.title = 'Click để đổi ảnh đại diện';
        avatarImg.onclick = openAvatarModal;
    }

    document.getElementById('btnSaveAvatar')?.addEventListener('click', confirmAvatarSelection);
    document.getElementById('btnCancelAvatar')?.addEventListener('click', closeAvatarModal);
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

        if (!document.querySelector('link[href*="Account_setting.css"]')) {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = '../css/Account_setting.css';
            document.head.appendChild(link);
        }

        attachEventListeners();
        await fetchAndPopulateAccountData();

    } catch (error) {
        console.error('Lỗi load trang:', error);
        alert('Lỗi tải trang tài khoản: ' + error.message);
    }
}

if (accountLink && centerContent) {
    originalBoardContent = centerContent.innerHTML;
    accountLink.addEventListener('click', e => {
        e.preventDefault();
        loadAccountSettings();
    });
    console.log('Account feature ready!');
}