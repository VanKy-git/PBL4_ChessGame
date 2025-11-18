// File: Account.js (Logic tải giao diện và dữ liệu tài khoản)
const accountLink = document.getElementById("account-link");
const centerContent = document.querySelector(".center");
// Lưu nội dung bàn cờ gốc
let originalBoardContent = null;

const API_URL = "http://localhost:8910";

// Hàm hiển thị bàn cờ
function showBoard() {
    if (centerContent && originalBoardContent) {
        centerContent.innerHTML = originalBoardContent;
        centerContent.classList.remove('account-view');
        // Giả sử có hàm applyTheme
        // applyTheme(currentTheme); 
        console.log('Đã quay lại bàn cờ!');
    }
}

// Hàm lấy dữ liệu người dùng và cập nhật form
async function fetchAndPopulateAccountData() {
    const currentUserId = localStorage.getItem("playerId") || "unknown";
    
    try {
        const response = await fetch(`${API_URL}/account?playerId=${currentUserId}`);
        const data = await response.json();
        
        if (response.ok && data.success) {
            const userData = data.data;
            
            // Cập nhật các trường trên form (sau khi HTML đã được load)
            const idPlayerEl = document.getElementById('id_player');
            const usernameInput = document.querySelector('.settings-page input[type="text"]');
            const emailInput = document.querySelector('.settings-page input[type="email"]');
            
            if(idPlayerEl) idPlayerEl.textContent = `ID:${userData.playerId}`;
            if(usernameInput) usernameInput.value = userData.username;
            if(emailInput) emailInput.value = userData.email;
            
            console.log('Đã cập nhật dữ liệu tài khoản thành công!');

            // Gắn sự kiện cho nút "Quay lại" (vì nó vừa được load)
            const cancelBtn = centerContent.querySelector('.cancel');
            if (cancelBtn) {
                cancelBtn.addEventListener('click', showBoard);
            }
            
            // TÙY CHỌN: Gắn sự kiện cho nút "Lưu thay đổi"
            const saveBtn = centerContent.querySelector('.save');
            if (saveBtn) {
                saveBtn.addEventListener('click', handleSaveAccountChanges);
            }

        } else {
            throw new Error(data.message || "Lỗi khi tải dữ liệu tài khoản.");
        }
    } catch (error) {
        console.error('Lỗi khi fetch dữ liệu tài khoản:', error);
        alert('Không thể tải dữ liệu tài khoản: ' + error.message);
    }
}

// Xử lý khi click nút Lưu thay đổi (Cần viết thêm API POST/PUT trong MainApiServer)
function handleSaveAccountChanges() {
    // Thu thập dữ liệu từ form
    const newUsername = document.querySelector('.settings-page input[type="text"]').value;
    const newEmail = document.querySelector('.settings-page input[type="email"]').value;
    const newPassword = document.querySelector('.settings-page input[type="password"]').value;
    
    const updatePayload = {
        username: newUsername,
        email: newEmail,
        // Chỉ gửi mật khẩu nếu người dùng nhập
        newPassword: newPassword || undefined 
    };

    console.log("Gửi dữ liệu cập nhật:", updatePayload);
    // Gửi fetch PUT/POST đến /api/account để cập nhật
    // ...
    alert("Chức năng Lưu thay đổi đang được triển khai!");
}

// Hàm load nội dung tài khoản (Load HTML trước, sau đó Fetch Data)
async function loadAccountSettings() {
    // BƯỚC 1: Load HTML
    try {
        console.log('Đang load Account_setting.html...');
        const response = await fetch('Account_setting.html');
        if (!response.ok) throw new Error('Không thể load trang tài khoản');
        
        const html = await response.text();
        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = html;
        const accountContent = tempDiv.querySelector('.settings-page');
        
        if (accountContent) {
            // Thay thế nội dung center
            centerContent.innerHTML = accountContent.outerHTML;
            centerContent.classList.add('account-view');
            
            // Load CSS
            if (!document.querySelector('link[href*="../css/Account_setting.css"]')) {
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = '../css/Account_setting.css';
                document.head.appendChild(link);
            }
            
            // BƯỚC 2: Load dữ liệu từ API và điền vào form
            await fetchAndPopulateAccountData();

        } else {
            throw new Error('Không tìm thấy .settings-page trong Account_setting.html');
        }
        
    } catch (error) {
        console.error('Lỗi khi load trang tài khoản:', error);
        // Fallback: Tải giao diện cơ bản và hiển thị lỗi
        // ... (phần fallback HTML của bạn) ...
        alert("Lỗi tải giao diện hoặc dữ liệu: " + error.message);
    }
}


// --- Listener (Account Link) ---
if (accountLink && centerContent) {
    // Chỉ lưu nội dung gốc khi tìm thấy cả hai element
    originalBoardContent = centerContent.innerHTML;

    accountLink.addEventListener('click', function(e) {
        e.preventDefault();
        console.log('Click vào nút Tài khoản!');
        loadAccountSettings();
    });
    
    console.log('Tính năng tài khoản đã sẵn sàng!');
} else {
    console.error('Không tìm thấy #account-link hoặc .center');
}