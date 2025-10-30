// Khai báo biến cần thiết
const accountLink = document.getElementById("account-link");
const centerContent = document.querySelector(".center"); // Đây là DIV chứa bàn cờ
let originalBoardContent = ''; // Biến để lưu HTML gốc của bàn cờ

// Lưu nội dung bàn cờ gốc khi trang load
window.addEventListener('DOMContentLoaded', () => {
    // Lưu HTML của cả 3 thanh: player-bar top, board-wrap, player-bar bottom
    if (centerContent) {
        originalBoardContent = centerContent.innerHTML;
        console.log('Nội dung bàn cờ gốc đã được lưu.');
    }
});

// Hàm hiển thị giao diện Tài khoản (thay thế bàn cờ)
function loadAccountSettings() {
    // Nội dung HTML của khung tài khoản (dùng Fallback HTML)
    const accountHTML = `
        <div class="account-setting-container">
            <h1 class="account-title">Cài Đặt Tài Khoản</h1>
            <div class="account-card">
                <div class="avatar-info">
                    <img src="../../PBL4_imgs/icon/man.png" alt="Avatar" class="profile-avatar">
                    <div class="user-details">
                        <div class="user-id">ID: **#1234**</div>
                        <button class="btn btn-change-avatar">Đổi ảnh đại diện</button>
                    </div>
                </div>
                <div class="setting-group">
                    <label for="username-input">Tên người chơi</label>
                    <input type="text" id="username-input" value="Tên người chơi" class="input-field">
                </div>
                <div class="setting-group">
                    <label for="email-input">Email</label>
                    <input type="email" id="email-input" value="abc@email.com" class="input-field" readonly> 
                </div>
                <div class="setting-group">
                    <label for="password-input">Đổi mật khẩu</label>
                    <input type="password" id="password-input" placeholder="Nhập mật khẩu mới" class="input-field">
                </div>
                <div class="action-buttons">
                    <button class="btn btn-save">Lưu thay đổi</button>
                    <button class="btn btn-cancel">Quay lại</button>
                </div>
            </div>
        </div>
    `;

    // 1. Thay thế nội dung
    centerContent.innerHTML = accountHTML;
    // 2. Thêm class để CSS có thể điều chỉnh layout nếu cần
    centerContent.classList.add('account-view'); 
    
    // 3. Gắn sự kiện cho nút "Quay lại" mới được tạo
    const cancelBtn = centerContent.querySelector('.btn-cancel');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', showBoard);
    }
    
    // Tùy chọn: Gắn sự kiện cho nút "Lưu" và "Đổi ảnh" ở đây...
    
    console.log('Hiển thị giao diện tài khoản thành công.');
}

// Hàm khôi phục bàn cờ
function showBoard() {
    if (originalBoardContent) {
        // 1. Khôi phục nội dung bàn cờ gốc
        centerContent.innerHTML = originalBoardContent;
        // 2. Xóa class đã thêm
        centerContent.classList.remove('account-view');
        console.log('Đã khôi phục bàn cờ.');
    } else {
        console.error('Lỗi: Không tìm thấy nội dung bàn cờ gốc để khôi phục.');
    }
}

// Sự kiện click vào nút Tài khoản ở thanh menu
if (accountLink && centerContent) {
    accountLink.addEventListener('click', function(e) {
        e.preventDefault();
        loadAccountSettings();
    });
}