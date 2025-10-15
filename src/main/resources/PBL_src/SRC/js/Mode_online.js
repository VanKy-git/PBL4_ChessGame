// ==================== POPUP CHƠI TRỰC TUYẾN ====================
let websocket = null;
let connected = false;

const popup = document.getElementById("onlinePopup");
const popupStatus = document.getElementById("popupStatus");
const popupActions = document.getElementById("popupActions");
const popupClose = document.getElementById("popupClose");
const popupCreateRoom = document.getElementById("popupCreateRoom");
const popupJoinCode = document.getElementById("popupJoinCode");
const popupJoinRoom = document.getElementById("popupJoinRoom");

document.querySelector('[data-mode="online"]').addEventListener('click', () => {
  popup.style.display = "flex";
  connectWebSocket();
});

popupClose.onclick = () => {
  popup.style.display = "none";
  if (websocket && websocket.readyState === WebSocket.OPEN) {
    websocket.close();
  }
  connected = false;
  popupStatus.textContent = "Đang kết nối...";
  popupActions.style.display = "none";
};

function connectWebSocket() {
  try {
    websocket = new WebSocket("ws://localhost:8080");

    websocket.onopen = () => {
      connected = true;
      popupActions.style.display = "block";
      websocket.send(JSON.stringify({ type: "connect", playerName: "Người chơi" }));
    };

    websocket.onclose = () => {
      connected = false;
      popupStatus.textContent = "Mất kết nối tới server.";
      popupActions.style.display = "none";
    };

    websocket.onerror = () => {
      popupStatus.textContent = "Lỗi kết nối!";
      popupActions.style.display = "none";
    };

    websocket.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      console.log("WS:", msg);
      if (msg.type === "room_created") {
        alert("Phòng đã tạo! Mã phòng: " + msg.roomId);
      } else if (msg.type === "room_joined") {
        localStorage.setItem("roomId", msg.roomId); // lưu mã phòng để dùng sau
        popup.style.display = "none"; // ẩn popup đi
        window.location.href = "match.html";
      } else if (msg.type === "error") {
        alert("Lỗi: " + msg.message);
      }
    };
  } catch (err) {
    popupStatus.textContent = "Không thể kết nối!";
  }
}

// Nút tạo phòng
popupCreateRoom.onclick = () => {
  if (connected && websocket) {
    websocket.send(JSON.stringify({ type: "create_room" }));
  }
};

// Nút tham gia phòng
popupJoinRoom.onclick = () => {
  const id = popupJoinCode.value.trim();
  if (!id) {
    alert("Vui lòng nhập mã phòng!");
    return;
  }
  if (connected && websocket) {
    websocket.send(JSON.stringify({ type: "join_room", roomId: id }));
  }
};