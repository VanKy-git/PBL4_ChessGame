// File: Home_page.js
import {sendMessage} from "./Connect_websocket.js";

// Lấy playerName từ localStorage
let playerName = localStorage.getItem("playerName") || "Guest";

// Các hàm đổi Theme (Giữ nguyên)
const boardEl = document.getElementById('chessBoard');


//Lấy popup ghép trận
const matchmakingOverlay = document.getElementById('matchmaking-overlay');
const cancelMatchmakingBtn = document.getElementById('cancelMatchmakingBtn');
const matchmakingTimerEl = document.getElementById('matchmaking-timer');
const gameOverOverlay = document.getElementById('game-over-overlay');
const gameOverTitleEl = document.getElementById('gameOverTitle');
const gameOverReasonEl = document.getElementById('gameOverReason');
const findNewBtn = document.getElementById('gameOverFindNewBtn');
const rematchBtn = document.getElementById('gameOverRematchBtn');
const leaveBtn = document.getElementById('gameOverLeaveBtn');
const confirmationOverlay = document.getElementById('confirmation-overlay');
const confirmTitleEl = document.getElementById('confirmTitle');
const confirmMessageEl = document.getElementById('confirmMessage');
const confirmBtnYes = document.getElementById('confirmBtnYes');
const confirmBtnNo = document.getElementById('confirmBtnNo');

let matchmakingIntervalId = null; // ID để dừng setInterval
let matchmakingStartTime = 0;   // Thời điểm bắt đầu tìm trận

// Biến lưu trữ HTML gốc
let originalModesHTML = '';
const rightPanel = document.querySelector('.right-panel');


// == CÁC HÀM TẠO VIEW (LẤY TỪ game_controller.js) ==

function getLobbyHTML() {
    return `
    <div class="online-wrapper">
        <button id="backToModes" class="btn-back">←</button>
        <div style="font-weight:700; font-size:18px; text-align:center; margin-bottom:10px;">Chơi trực tuyến</div>
        <div class="muted" style="text-align:center; margin-bottom:20px;">Kết nối với đối thủ khác</div>
        
        <button id="createRoomBtn" class="btnn" disabled>Tạo phòng</button>
        <input id="joinRoomIdInput" class="input" placeholder="Nhập mã phòng...">
        <button id="joinRoomBtn" class="btnn" disabled>Tham gia phòng</button>
        <button id="matchmakingBtn" class="btnn" disabled>Ghép trận ngẫu nhiên</button>
        
        <div id="lobbyStatus" class="status-lobby">Đang chờ kết nối...</div>
    </div>`;
}

// Hàm hiển thị popup
function showMatchmakingPopup() {
    if (matchmakingOverlay) {
        matchmakingOverlay.classList.remove('hidden');
    }
    startMatchmakingTimer();
    // Gắn sự kiện Hủy (chỉ gắn 1 lần)
    if (cancelMatchmakingBtn) {
        // Xóa listener cũ trước khi thêm mới (đề phòng)
        cancelMatchmakingBtn.removeEventListener('click', handleCancelMatchmaking);
        cancelMatchmakingBtn.addEventListener('click', handleCancelMatchmaking, { once: true }); // Chỉ chạy 1 lần
    }
}

// Hàm ẩn popup
function hideMatchmakingPopup() {
    if (matchmakingOverlay) {
        matchmakingOverlay.classList.add('hidden');
    }
    stopMatchmakingTimer();
}
// Gắn vào window để game_controller có thể gọi
window.hideMatchmakingPopup = hideMatchmakingPopup;

// Hàm xử lý khi nhấn nút Hủy
function handleCancelMatchmaking() {
    hideMatchmakingPopup();
    stopMatchmakingTimer();
    // Gửi tin nhắn hủy lên server (BẠN CẦN THÊM LOGIC XỬ LÝ Ở SERVER)
    sendMessage({ type: "cancel_matchmaking" });

    // Có thể cần kích hoạt lại nút "Ghép trận" trong Lobby View
    const matchmakingBtnInLobby = rightPanel.querySelector('#matchmakingBtn');
    if (matchmakingBtnInLobby) {
        matchmakingBtnInLobby.disabled = false;
        matchmakingBtnInLobby.textContent = "Ghép trận ngẫu nhiên";
    }
}

function getGameControlsHTML() {
    return `
    <div class="game-controls-wrapper">
        <div class="status" id="gameStatus">Đang chờ đối thủ...</div>
        <div id="playerInfoBar">
            <div>Phòng: <span id="roomInfoEl">-</span></div>
            <div>Màu: <span id="colorInfoEl">-</span></div>
        </div>
        <div id="chatSection" class="chat-section">
            <div style="font-weight:700; margin-bottom:5px;">Trò chuyện</div>
            <div id="chatMessagesEl" class="chat-messages"></div>
            <div class="chat-input-area">
                <input id="chatInputEl" class="input" placeholder="Nhắn tin...">
                <button id="chatSendBtnEl" class="btn-chat">Gửi</button>
            </div>
        </div>
        <div id="moveListContainer" class="move-list-section">
            <div style="font-weight:600; margin-bottom: 5px;">Lịch sử nước đi:</div>
            <ul id="moveList"></ul>
        </div>
        <div class="game-actions">
            <button id="drawRequestBtn" class="btn-action">Cầu hòa</button>
            <button id="resignBtn" class="btn-action btn-warning">Đầu hàng</button>
            <button id="exitRoomBtn" class="btn-action btn-danger">Thoát phòng</button>
        </div>
    </div>`;
}

// == CÁC HÀM HIỂN THỊ VIEW ==

function showModesView() {
    if (rightPanel) rightPanel.innerHTML = originalModesHTML;
    // Tùy chọn: Gửi tin nhắn ngắt kết nối
    // sendMessage({ type: "disconnect" });
}

function showConfirmationPopup(title, message) {
    return new Promise((resolve) => {
        if (!confirmationOverlay || !confirmTitleEl || !confirmMessageEl || !confirmBtnYes || !confirmBtnNo) {
            // Fallback dùng confirm() nếu element bị thiếu
            resolve(confirm(`${title}\n${message}`));
            return;
        }

        // Cập nhật nội dung popup
        confirmTitleEl.textContent = title;
        confirmMessageEl.textContent = message;

        // Xóa listener cũ trước khi gán mới (quan trọng!)
        confirmBtnYes.onclick = null;
        confirmBtnNo.onclick = null;

        // Gán sự kiện cho nút
        confirmBtnYes.onclick = () => {
            confirmationOverlay.classList.add('hidden');
            resolve(true); // Trả về true khi đồng ý
        };
        confirmBtnNo.onclick = () => {
            confirmationOverlay.classList.add('hidden');
            resolve(false); // Trả về false khi hủy
        };

        // Hiển thị popup
        confirmationOverlay.classList.remove('hidden');
    });
}

function showLobbyView() {
    if (rightPanel) rightPanel.innerHTML = getLobbyHTML();
    // Kích hoạt kết nối
    sendMessage({
        type: "connect",
        playerName: playerName,
        playerId: localStorage.getItem("playerId") // Gửi cả ID (nếu có)
    });
}

window.showGameOverPopup = function(result, reason) {
    if (!gameOverOverlay || !gameOverTitleEl || !gameOverReasonEl || !findNewBtn || !rematchBtn || !leaveBtn) {
        console.error("Không tìm thấy các element của popup Game Over!");
        // Fallback dùng alert
        alert(`Kết quả: ${result} - Lý do: ${reason || 'Kết thúc trận'}`);
        return;
    }

    // --- Cập nhật nội dung ---
    switch (result) {
        case 'win':
            gameOverTitleEl.textContent = '🎉 Chiến thắng!';
            gameOverTitleEl.style.color = '#4CAF50'; // Màu xanh lá
            break;
        case 'loss':
            gameOverTitleEl.textContent = 'Thất bại!';
            gameOverTitleEl.style.color = '#F44336'; // Màu đỏ
            break;
        case 'draw':
        default:
            gameOverTitleEl.textContent = 'Hòa cờ!';
            gameOverTitleEl.style.color = '#FFC107'; // Màu vàng
            break;
    }
    gameOverReasonEl.textContent = reason || ''; // Hiển thị lý do hoặc để trống

    // --- Gắn sự kiện cho các nút ---

    // Nút Tìm trận mới: Gọi hàm findNewGame từ game_controller
    findNewBtn.onclick = () => {
        gameOverOverlay.classList.add('hidden'); // Ẩn popup
        if (window.findNewGame) {
            window.findNewGame(); // Gọi hàm tìm trận mới
            // Chuyển về màn hình Lobby (quan trọng)
            showLobbyView();
        } else {
            console.error("Không tìm thấy hàm window.findNewGame!");
        }
    };

    // Nút Tái đấu: Gửi yêu cầu lên server
    rematchBtn.onclick = () => {
        gameOverOverlay.classList.add('hidden'); // Ẩn popup
        if (window.requestRematch) {
            window.requestRematch(); // Gọi hàm gửi yêu cầu tái đấu
            // Có thể hiển thị thông báo "Đã gửi yêu cầu tái đấu"
            alert("Đã gửi yêu cầu tái đấu!");
        } else {
            console.error("Chưa có hàm window.requestRematch!");
            // Tạm thời chỉ quay lại lobby
            showLobbyView();
        }
    };

    // Nút Thoát: Quay về màn hình chọn chế độ
    leaveBtn.onclick = () => {
        gameOverOverlay.classList.add('hidden'); // Ẩn popup
        showModesView(); // Quay về màn hình chọn chế độ chơi
    };

    // --- Hiển thị popup ---
    gameOverOverlay.classList.remove('hidden');
}

// ✅ Gắn hàm này vào 'window' để game_controller có thể gọi nó
window.showGameControlsView = function() {
    if (rightPanel) rightPanel.innerHTML = getGameControlsHTML();

    const chatSendBtn = document.getElementById('chatSendBtnEl');
    if (chatSendBtn && window.sendChat) chatSendBtn.addEventListener('click', window.sendChat);

    const chatInput = document.getElementById('chatInputEl');
    if (chatInput && window.sendChat) chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') window.sendChat();
    });
}

function startMatchmakingTimer() {
    matchmakingStartTime = Date.now(); // Ghi lại thời điểm bắt đầu
    if (matchmakingTimerEl) matchmakingTimerEl.textContent = '00:00'; // Reset hiển thị

    // Dừng interval cũ nếu có
    if (matchmakingIntervalId) clearInterval(matchmakingIntervalId);

    // Cập nhật đồng hồ mỗi giây
    matchmakingIntervalId = setInterval(() => {
        const elapsedTime = Math.floor((Date.now() - matchmakingStartTime) / 1000); // Giây đã trôi qua
        const minutes = String(Math.floor(elapsedTime / 60)).padStart(2, '0');
        const seconds = String(elapsedTime % 60).padStart(2, '0');
        if (matchmakingTimerEl) {
            matchmakingTimerEl.textContent = `${minutes}:${seconds}`;
        }
    }, 1000);
}

// ✅ HÀM DỪNG ĐỒNG HỒ
function stopMatchmakingTimer() {
    if (matchmakingIntervalId) {
        clearInterval(matchmakingIntervalId); // Dừng cập nhật
        matchmakingIntervalId = null;
    }
    // Có thể reset text hoặc để nguyên giá trị cuối
    // if (matchmakingTimerEl) matchmakingTimerEl.textContent = '00:00';
}

// == KHỞI TẠO ==
document.addEventListener('DOMContentLoaded', function () {
    if (!rightPanel) {
        console.error("Không tìm thấy '.right-panel'");
        return;
    }
    anime({
        targets: '.ml1 .letter', // Chọn tất cả các chữ cái
        opacity: [0, 1],         // Chuyển từ mờ (0) sang rõ (1)
        translateY: [20, 0],     // Di chuyển từ dưới lên (20px -> 0px)
        easing: "easeOutExpo",   // Kiểu chuyển động mượt mà
        duration: 1400,          // Tổng thời gian animation
        delay: anime.stagger(100) // Mỗi chữ cái trễ 100ms so với chữ trước
        // delay: (el, i) => 70 * i // Cách viết delay khác, tương tự stagger
    });
    // Lưu lại HTML ban đầu
    originalModesHTML = rightPanel.innerHTML;

    // Sử dụng Ủy quyền sự kiện (Event Delegation)
    rightPanel.addEventListener('click', async function (event) {

        // 1. Click "Chơi trực tuyến"
        const onlineModeBtn = event.target.closest('.mode[data-mode="online"]');
        if (onlineModeBtn) {
            showLobbyView();
            return;
        }

        // 2. Click "Back"
        const backBtn = event.target.closest('#backToModes');
        if (backBtn) {
            showModesView();
            return;
        }

        // 3. Click "Tạo phòng"
        const createRoomBtn = event.target.closest('#createRoomBtn');
        if (createRoomBtn && window.createRoom) {
            window.createRoom(); // Gọi hàm từ game_controller
            return;
        }

        // 4. Click "Tham gia phòng"
        const joinRoomBtn = event.target.closest('#joinRoomBtn');
        if (joinRoomBtn && window.joinRoom) {
            window.joinRoom(); // Gọi hàm từ game_controller
            return;
        }

        // 5. Click "Ghép trận"
        const matchmakingBtn = event.target.closest('#matchmakingBtn');
        if (matchmakingBtn && window.findNewGame) {

            // ✅ HIỂN THỊ POPUP TRƯỚC KHI GỬI YÊU CẦU
            showMatchmakingPopup();

            // Vô hiệu hóa nút và đổi text (có thể làm trong showMatchmakingPopup)
            matchmakingBtn.disabled = true;

            matchmakingBtn.textContent = "Đang tìm...";

            window.findNewGame(); // Gọi hàm gửi yêu cầu ghép trận
            return;
        }
        // 6. Click "Cầu hòa" (#drawRequestBtn trong Game View)
        const drawBtn = event.target.closest('#drawRequestBtn');
        if (drawBtn && window.requestDraw) {
            // ✅ HIỂN THỊ POPUP XÁC NHẬN TÙY CHỈNH
            const confirmed = await showConfirmationPopup(
                "Xác nhận Cầu hòa",
                "Bạn có chắc chắn muốn gửi lời đề nghị hòa đến đối thủ?"
            );
            if (confirmed) {
                console.log("Đã gửi cầu hòa");
                window.requestDraw(); // Chỉ gọi nếu nhấn Đồng ý
            }
            return;
        }

        // 7. Click "Đầu hàng" (#resignBtn trong Game View)
        const resignBtn = event.target.closest('#resignBtn');
        if (resignBtn && window.resignGame) {
            // ✅ HIỂN THỊ POPUP XÁC NHẬN TÙY CHỈNH
            const confirmed = await showConfirmationPopup(
                "Xác nhận Đầu hàng",
                "Bạn có chắc chắn muốn đầu hàng trận đấu này không?"
            );
            if (confirmed) {
                window.resignGame(); // Chỉ gọi nếu nhấn Đồng ý
            }
            return;
        }

        // 8. Click "Thoát phòng" (#exitRoomBtn trong Game View)
        const exitBtn = event.target.closest('#exitRoomBtn');
        if (exitBtn) {
            // ✅ HIỂN THỊ POPUP XÁC NHẬN TÙY CHỈNH
            const confirmed = await showConfirmationPopup(
                "Xác nhận Thoát phòng",
                "Bạn có chắc chắn muốn thoát khỏi phòng? (Nếu đang chơi, bạn sẽ bị xử thua)."
            );
            if (confirmed) {
                if (window.leaveRoom) {
                    window.leaveRoom(); // Gửi tin nhắn rời phòng
                }
                showModesView(); // Quay về màn hình chọn chế độ
            }
            return;
        }
    });
});