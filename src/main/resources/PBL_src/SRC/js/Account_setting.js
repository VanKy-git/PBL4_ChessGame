// ========== CHỨC NĂNG LOAD GIAO DIỆN TÀI KHOẢN ==========
const accountLink = document.getElementById("account-link");
const centerContent = document.querySelector(".center");

if (accountLink && centerContent) {
  console.log('Tìm thấy #account-link và .center');
  // Lưu nội dung bàn cờ gốc
  const originalBoardContent = centerContent.innerHTML;

  // Hàm load nội dung tài khoản
  async function loadAccountSettings() {
    try {
      console.log('Đang load Account_setting.html...');
      const response = await fetch('Account_setting.html');
      if (!response.ok) throw new Error('Không thể load trang tài khoản');
      
      const html = await response.text();
      console.log('Đã load thành công!');
      
      // Tạo một div tạm để parse HTML
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = html;
      
      // Lấy nội dung body hoặc phần settings-page
      const accountContent = tempDiv.querySelector('.settings-page');
      
      if (accountContent) {
        // Thay thế nội dung center
        centerContent.innerHTML = accountContent.outerHTML;
        centerContent.classList.add('account-view');
        
        // Load CSS cho account settings nếu chưa có
        if (!document.querySelector('link[href*="../css/Account_setting.css"]')) {
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.href = '../css/Account_setting.css';
          document.head.appendChild(link);
        }
        
        // Gắn sự kiện cho nút "Quay lại"
        const cancelBtn = centerContent.querySelector('.cancel');
        if (cancelBtn) {
          cancelBtn.addEventListener('click', showBoard);
        }
        
        console.log('Hiển thị tài khoản thành công!');
      } else {
        throw new Error('Không tìm thấy .settings-page');
      }
      
    } catch (error) {
      console.error('Lỗi khi load trang tài khoản:', error);
      // Fallback: hiển thị nội dung trực tiếp
      centerContent.innerHTML = `
        <div class="settings-page">
          <h1 class="title">Cài Đặt Tài Khoản</h1>
          <div class="account-section">
            <div class="avatar">
              <img src="../../PBL4_imgs/icon/man.png" alt="Avatar">
              <button class="btn change-avatar">Đổi ảnh</button>
            </div>
            <div id="id_player">ID:#1234</div>
            <label>Tên người chơi</label>
            <input type="text" value="Tên người chơi">
            <label>Email</label>
            <input type="email" value="abc@email.com">
            <label>Đổi mật khẩu</label>
            <input type="password" placeholder="Nhập mật khẩu mới">
            <div class="actions">
              <button class="btn save">Lưu thay đổi</button>
              <button class="btn cancel">Quay lại</button>
            </div>
          </div>
        </div>
      `;
      centerContent.classList.add('account-view');
      
      // Load CSS
      if (!document.querySelector('link[href*="Account_setting.css"]')) {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = '../css/Account_setting.css';
        document.head.appendChild(link);
      }
      
      // Gắn sự kiện cho nút Quay lại trong fallback
      const cancelBtn = centerContent.querySelector('.cancel');
      if (cancelBtn) {
        cancelBtn.addEventListener('click', showBoard);
      }
    }
  }

  // Hàm hiển thị bàn cờ
  function showBoard() {
    centerContent.innerHTML = originalBoardContent;
    centerContent.classList.remove('account-view');
    // Áp dụng lại theme sau khi load lại bàn cờ
    applyTheme(currentTheme);
    console.log('Đã quay lại bàn cờ!');
  }

  // Sự kiện click vào nút Tài khoản
  accountLink.addEventListener('click', function(e) {
    e.preventDefault();
    console.log('Click vào nút Tài khoản!');
    loadAccountSettings();
  });
  
  console.log(' Tính năng tài khoản đã sẵn sàng!');
} else {
  console.error(' Không tìm thấy #account-link hoặc .center');
}
