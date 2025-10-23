const boardEl = document.getElementById('chessBoard');
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

applyTheme(currentTheme);

document.addEventListener('DOMContentLoaded', function () {
  const rightPanel = document.querySelector('.right-panel');
  const onlineMode = document.querySelector('.mode[data-mode="online"]');
  const originalHTML = rightPanel.innerHTML;

  onlineMode.addEventListener('click', function () {
      // Thay toàn bộ phần bên phải
      rightPanel.innerHTML = `
      <div class="online-wrapper" style="position: relative; display: flex; flex-direction: column; gap: 12px; padding: 20px;">
  <button id="backToModes">←</button>
  <div style="font-weight:700; font-size:18px; text-align:center; margin-bottom:10px;">Chơi trực tuyến</div>
  <div class="muted" style="text-align:center; margin-bottom:20px;">Kết nối với đối thủ khác</div>
  <button id="createRoom" class="btnn">Tạo phòng</button>
  <input id="roomCode" class="input" placeholder="Nhập mã phòng...">
  <button id="joinRoom" class="btnn">Tham gia phòng</button>
  <button id="matchmaking" class="btnn">Ghép trận ngẫu nhiên</button>
  
</div>
      `;

      // Khi nhấn nút ← thì quay lại giao diện cũ
      const backBtn = document.getElementById('backToModes');
      backBtn.addEventListener('click', function () {
          rightPanel.innerHTML = originalHTML;
      });
  });
});







