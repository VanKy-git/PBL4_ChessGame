// match.js
// Connects to ChessServer (Java WebSocket), handles room join/create via previous flow,
// sends move_request with algebraic notation, handles responses (move_result, turn_change, end_game).
// Assumes chess_board.js is loaded and exposes applyMoveFromAlgebraic, coordToAlg, etc.

// global roomId variable — read from localStorage (set when player joined/created room)
let socket = null;
window.roomId = localStorage.getItem("roomId") || null; // expose as window.roomId for chess_board
let playerColor = null;
let currentTurn = "white";

// DOM
const exitRoomBtn = document.getElementById("exitroom-btn");
const drawBtn = document.getElementById("draw-btn");
const resignBtn = document.getElementById("resign-btn");
const exitConfirm = document.getElementById("exitConfirm");
const stayBtn = document.getElementById("stay-btn");
const exitConfirmBtn = document.getElementById("exitconfirm-btn");

// initial hide popup
if (exitConfirm) exitConfirm.classList.add("modal-hidden");

// popup handlers
if (exitRoomBtn) exitRoomBtn.addEventListener("click", () => exitConfirm.classList.remove("modal-hidden"));
if (stayBtn) stayBtn.addEventListener("click", () => exitConfirm.classList.add("modal-hidden"));
if (exitConfirmBtn) exitConfirmBtn.addEventListener("click", () => {
  // send leave_room to server if connected
  if (socket && socket.readyState === WebSocket.OPEN && window.roomId) {
    socket.send(JSON.stringify({ type: "leave_room", roomId: window.roomId }));
  }
  // clear stored room id
  localStorage.removeItem("roomId");
  window.roomId = null;
  window.location.href = "Home_page.html";
});
if (resignBtn) resignBtn.addEventListener("click", () => {
  if (socket && socket.readyState === WebSocket.OPEN && window.roomId) {
    socket.send(JSON.stringify({ type: "resign", roomId: window.roomId }));
  }
  alert("Bạn đã xin thua.");
  localStorage.removeItem("roomId");
  window.location.href = "Home_page.html";
});
if (drawBtn) drawBtn.addEventListener("click", () => {
  if (socket && socket.readyState === WebSocket.OPEN && window.roomId) {
    socket.send(JSON.stringify({ type: "draw_request", roomId: window.roomId }));
    alert("Đã gửi yêu cầu hòa.");
  } else {
    alert("Không thể gửi yêu cầu hòa (chưa kết nối).");
  }
});

// WebSocket connection
function connect() {
    const name = playerNameInput.value.trim();
    if (!name) {
        alert('Vui lòng nhập tên!');
        return;
    }

    playerName = name;
    const url = "ws://localhost:8080";

    try {
        websocket = new WebSocket(url);

        websocket.onopen = () => {
            updateConnectionStatus(true);
            websocket.send(JSON.stringify({
                type: "connect",
                playerName: playerName
            }));
        };

        websocket.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            console.log('Received:', msg);
            handleMessage(msg);
        };

        websocket.onclose = () => {
            updateConnectionStatus(false);
            resetGameState();
        };

        websocket.onerror = (error) => {
            console.error('WebSocket error:', error);
            updateConnectionStatus(false);
        };

    } catch (error) {
        console.error('Connection error:', error);
        alert('Không thể kết nối tới server!');
    }
}

// helper to convert coord to algebraic (mirror of chess_board)
function coordToAlg(r, c) {
  const file = String.fromCharCode('a'.charCodeAt(0) + c);
  const rank = 8 - r;
  return file + rank;
}

// Exposed function used by chess_board.js to request a move
// It takes algebraic strings "e2","e4"
function sendMove(fromAlg, toAlg) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    console.warn("WebSocket not connected, cannot send move");
    return;
  }
  if (!window.roomId) {
    console.warn("No roomId — playing local only");
    return;
  }

  socket.send(JSON.stringify({
    type: "move_request",
    roomId: window.roomId,
    from: fromAlg,
    to: toAlg,
    piece: null // optional - server can determine piece from its gameState/validator
  }));
}

// expose globally so chess_board can call sendMove
window.sendMove = sendMove;
window.coordToAlg = coordToAlg;

// connect on load
connectWebSocket();
