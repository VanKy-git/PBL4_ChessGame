const boardEl = document.getElementById('chessBoard');

let flipped = false;

// Kiểm tra xem có color picker không trước khi dùng
const lightPicker = document.getElementById('lightPicker');
const darkPicker = document.getElementById('darkPicker');
const flipBtn = document.getElementById('flipBtn');
const resetBtn = document.getElementById('resetBtn');

function applyColors() {
  if (!boardEl) return;
  const squares = boardEl.querySelectorAll('.square');
  squares.forEach(sq => {
    const r = +sq.dataset.row;
    const c = +sq.dataset.col;
    const isLight = (r + c) % 2 === 0;
    if (lightPicker && darkPicker) {
      sq.style.background = isLight ? lightPicker.value : darkPicker.value;
    }
  });
}

function flipBoard() {
  if (!boardEl) return;
  flipped = !flipped;
  boardEl.style.transform = flipped ? 'rotate(180deg)' : 'none';
  const squares = boardEl.querySelectorAll('.square');
  squares.forEach(sq => sq.style.transform = flipped ? 'rotate(180deg)' : 'none');
}

// gán sự kiện (chỉ khi có các element)
if (lightPicker && darkPicker && flipBtn && resetBtn) {
  lightPicker.addEventListener('input', applyColors);
  darkPicker.addEventListener('input', applyColors);
  flipBtn.addEventListener('click', flipBoard);
  resetBtn.addEventListener('click', () => {
    lightPicker.value = '#f0d9b5';
    darkPicker.value = '#b58863';
    applyColors();
    if (flipped) flipBoard();
  });
}

// chọn chế độ chơi
document.querySelectorAll('.mode').forEach(node => node.addEventListener('click', () => {
  const m = node.dataset.mode;
  alert('Bạn chọn chế độ: ' + m);
}));

const themes = [
  { white: "#f0d9b5", black: "#b58863" }, // mặc định
  { white: "#e0f7fa", black: "#006064" }, // xanh ngọc
  { white: "#f3e5f5", black: "#6a1b9a" }, // tím
  { white: "#eeeeee", black: "#424242" },
  { white: "#fff3e0", black: "#e65100", name: "Hoàng hôn" }, // cam nhạt - cam đậm
  { white: "#e3f2fd", black: "#1565c0", name: "Bầu trời" }, // xanh da trời nhạt - xanh đậm
  { white: "#f1f8e9", black: "#558b2f", name: "Tre xanh" }, // xanh lá nhạt - xanh rêu
  { white: "#fafafa", black: "#37474f", name: "Đơn sắc" }, // trắng xám - xám xanh
  { white: "#fff9c4", black: "#f57f17", name: "Mật ong" }, // vàng nhạt - vàng đậm
  { white: "#fce4ec", black: "#880e4f", name: "Hoa anh đào" }, // hồng nhạt - tím đỏ
  { white: "#e0f2f1", black: "#00695c", name: "Ngọc bích" }, // xanh mint - xanh lục
  { white: "#fff8e1", black: "#ff6f00", name: "Vàng kim" }, // vàng nhạt - cam vàng
  { white: "#ede7f6", black: "#4527a0", name: "Tím đêm" }, // tím nhạt - tím đậm
  { white: "#efebe9", black: "#5d4037", name: "Cà phê sữa" }, // be nhạt - nâu đậm
  { white: "#eceff1", black: "#263238", name: "Kim loại" }  // xám
];

let currentTheme = 0;

function applyTheme(index) {
  const theme = themes[index];
  document.documentElement.style.setProperty("--white-square", theme.white);
  document.documentElement.style.setProperty("--black-square", theme.black);
}

function prevTheme() {
  currentTheme = (currentTheme - 1 + themes.length) % themes.length;
  applyTheme(currentTheme);
}

function nextTheme() {
  currentTheme = (currentTheme + 1) % themes.length;
  applyTheme(currentTheme);
}

// áp dụng theme đầu tiên khi load
applyTheme(currentTheme);

const btn = document.getElementById('menu_banbe');
const friendsList = document.getElementById('friendsList');

function hideFriendsList() {
  if (friendsList) {
    friendsList.style.display = 'none';
  }
}

if (btn && friendsList) {
  btn.addEventListener('click', function(e) {
    e.preventDefault();
    if (friendsList.style.display === 'block') {
      friendsList.style.display = 'none';
    } else {
      friendsList.style.display = 'block';
    }
  });

  // Ẩn khi chuột rời khỏi cả nút và vùng danh sách bạn bè
  btn.addEventListener('mouseleave', () => {
    setTimeout(() => {
      if (!btn.matches(':hover') && !friendsList.matches(':hover')) {
        hideFriendsList();
      }
    }, 100); 
  });

  friendsList.addEventListener('mouseleave', () => {
    setTimeout(() => {
      if (!btn.matches(':hover') && !friendsList.matches(':hover')) {
        hideFriendsList();
      }
    }, 100);
  });
}

// ========== CHỨC NĂNG LOAD GIAO DIỆN TÀI KHOẢN ==========
const accountLink = document.getElementById("account-link");
const centerContent = document.querySelector(".center");

if (accountLink && centerContent) {
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
        
        // // Load CSS cho account settings nếu chưa có
        // if (!document.querySelector('link[href*="Account_setting.css"]')) {
        //   const link = document.createElement('link');
        //   link.rel = 'stylesheet';
        //   link.href = '../css/Account_setting.css';
        //   document.head.appendChild(link);
        // }
        
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
  
  console.log('✅ Tính năng tài khoản đã sẵn sàng!');
} else {
  console.error('❌ Không tìm thấy #account-link hoặc .center');
}