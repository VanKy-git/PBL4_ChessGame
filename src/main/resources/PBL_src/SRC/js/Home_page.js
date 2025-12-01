// File: Home_page.js
import {connectMainSocket, sendMessage} from "./Connect_websocket.js";

// Lấy playerName từ localStorage
let playerName = localStorage.getItem("playerName") || "Guest";

// === SIDEBAR USER INFO + LOGOUT (THÊM VÀO ĐÂY) ===
async function loadSidebarUserInfo() {
    const usernameEl = document.getElementById('sidebarUsername');
    const playerIdEl = document.getElementById('sidebarPlayerId');
    const avatarEl   = document.getElementById('sidebarAvatar');
    const logoutBtn = document.getElementById('logoutBtn');

    if (!usernameEl || !playerIdEl || !avatarEl) {
        setTimeout(loadSidebarUserInfo, 100);
        return;
    }

    const playerId = localStorage.getItem("playerId");
    const token    = localStorage.getItem("token");

    // NẾU KHÔNG CÓ TOKEN HOẶC PLAYERID → GUEST
    if (!playerId || !token) {
        usernameEl.textContent = "Guest";
        playerIdEl.textContent = "ID: #0000";
        avatarEl.src = "../../PBL4_imgs/icon/user.png";
        return;
    }

    // BẮT BUỘC GỌI API MỖI LẦN ĐỂ LẤY DỮ LIỆU MỚI NHẤT TỪ DB
    try {
        const res = await fetch(`http://localhost:8910/api/account?playerId=${playerId}`, {
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (!res.ok) throw new Error("Token lỗi hoặc hết hạn");

        const json = await res.json();
        if (json.success && json.data) {
            const u = json.data;
            // HIỆN ĐÚNG DỮ LIỆU MỚI NHẤT TỪ DB
            usernameEl.textContent = u.userName || "Guest";
            playerIdEl.textContent = `ID: #${u.playerId || playerId}`;
            if (u.avatarUrl) {
                avatarEl.src = u.avatarUrl;
            }

            // CẬP NHẬT LẠI localStorage ĐỂ LẦN SAU NHANH HƠN
            localStorage.setItem("playerName", u.username);
            if (u.avatarUrl) localStorage.setItem("avatarUrl", u.avatarUrl);
        }
    } catch (err) {
        console.warn("Không load được từ DB → dùng cache cũ:", err);
        // Nếu API lỗi → mới dùng cache
        const cached = localStorage.getItem("playerName");
        usernameEl.textContent = cached ? cached + " (offline)" : "Guest";
        playerIdEl.textContent = `ID: #${playerId}`;
    }
}

async function logout() {
    // HIỆN POPUP XÁC NHẬN ĐẸP
    const confirmed = await showConfirmationPopup(
        "Xác nhận đăng xuất",
        "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này?"
    );

    if (!confirmed) return; // Bấm Hủy → thoát hàm

    const playerId = localStorage.getItem("playerId");
    const token = localStorage.getItem("token");

    // Gửi logout lên server (nếu có token)
    if (playerId && token) {
        try {
            await fetch(`${API_URL}/api/auth/logout`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                },
                body: JSON.stringify({ playerId })
            });
        } catch (e) {
            console.warn("Server không phản hồi logout (vẫn ok)", e);
        }
    }

    // XÓA SẠCH DỮ LIỆU ĐĂNG NHẬP
    localStorage.removeItem("token");
    localStorage.removeItem("playerId");
    localStorage.removeItem("playerName");
    localStorage.removeItem("avatarUrl");

    // Reset giao diện về Guest
    document.getElementById('sidebarUsername').textContent = "Guest";
    document.getElementById('sidebarPlayerId').textContent = "ID: #0000";
    document.getElementById('sidebarAvatar').src = "../../PBL4_imgs/icon/user.png";

    // Chuyển về trang login (thay tên file nếu khác)
    window.location.href = "../html/MainLogin.html";
}

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
// === THÊM ELEMENT CHO POPUP CHỌN THỜI GIAN ===
const timeControlOverlay = document.getElementById('time-control-overlay');
const timeOptionsContainer = timeControlOverlay?.querySelector('.time-options');
const cancelTimeSelectionBtn = document.getElementById('cancelTimeSelectionBtn');

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
        
        <button id="createRoomBtn" class="btnn" >Tạo phòng</button>
        <input id="joinRoomIdInput" class="input" placeholder="Nhập mã phòng...">
        <button id="joinRoomBtn" class="btnn" >Tham gia phòng</button>
        <button id="matchmakingBtn" class="btnn" >Ghép trận ngẫu nhiên</button>
        
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

function showModesView() {
    if (rightPanel) rightPanel.innerHTML = originalModesHTML;
    // Tùy chọn: Gửi tin nhắn ngắt kết nối
    // sendMessage({ type: "disconnect" });
}

function showConfirmationPopup(title, message) {
    return new Promise((resolve) => {
        if (!confirmationOverlay || !confirmTitleEl || !confirmMessageEl || !confirmBtnYes || !confirmBtnNo) {
            resolve(confirm(`${title}\n${message}`));
            return;
        }

        confirmTitleEl.textContent = title;
        confirmMessageEl.textContent = message;

        confirmBtnYes.onclick = null;
        confirmBtnNo.onclick = null;

        confirmBtnYes.onclick = () => {
            confirmationOverlay.classList.add('hidden');
            resolve(true); // Trả về true khi đồng ý
        };
        confirmBtnNo.onclick = () => {
            confirmationOverlay.classList.add('hidden');
            resolve(false); // Trả về false khi hủy
        };

        confirmationOverlay.classList.remove('hidden');
    });
}

function selectTimeControl() {
    return new Promise((resolve) => {
        if (!timeControlOverlay || !timeOptionsContainer || !cancelTimeSelectionBtn) {
            console.error("Không tìm thấy element của popup chọn thời gian!");
            resolve(null);
            return;
        }

        timeOptionsContainer.querySelectorAll('.time-btn').forEach(button => {
            const timeMs = parseInt(button.dataset.time);
            const clickHandler = () => {
                timeControlOverlay.classList.add('hidden');
                resolve(timeMs); // Trả về thời gian đã chọn (ms)
            };
            button.replaceWith(button.cloneNode(true)); // Cách đơn giản để xóa mọi listener
            timeControlOverlay.querySelector(`[data-time="${timeMs}"]`).addEventListener('click', clickHandler);
        });

        const cancelHandler = () => {
            timeControlOverlay.classList.add('hidden');
            resolve(null); // Trả về null khi hủy
        };
        cancelTimeSelectionBtn.replaceWith(cancelTimeSelectionBtn.cloneNode(true));
        document.getElementById('cancelTimeSelectionBtn').addEventListener('click', cancelHandler);
        timeControlOverlay.classList.remove('hidden');
    });
}

function showLobbyView(selectedTimeMs = null) {
    if (rightPanel) rightPanel.innerHTML = getLobbyHTML();
}

window.showGameOverPopup = function(result, reason) {
    if (!gameOverOverlay || !gameOverTitleEl || !gameOverReasonEl || !findNewBtn || !rematchBtn || !leaveBtn) {
        console.error("Không tìm thấy các element của popup Game Over!");
        alert(`Kết quả: ${result} - Lý do: ${reason || 'Kết thúc trận'}`);
        return;
    }

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

    findNewBtn.onclick = () => {
        gameOverOverlay.classList.add('hidden'); // Ẩn popup
        showLobbyView();
    };

    rematchBtn.onclick = () => {
        if (window.requestRematch) {
            console.log(window.requestRematch);
            window.requestRematch(); // Gọi hàm gửi yêu cầu
        } else {
            console.error("Chưa có hàm window.requestRematch!");
            gameOverOverlay.classList.add('hidden');
            showLobbyView();
        }
    };

    leaveBtn.onclick = () => {
        gameOverOverlay.classList.add('hidden'); // Ẩn popup
        if (window.leaveRoom) {
            window.leaveRoom(); // Gửi tin nhắn rời phòng
        }
        showModesView(); // Quay về màn hình chọn chế độ chơi
    };

    gameOverOverlay.classList.remove('hidden');
}

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

    if (matchmakingIntervalId) clearInterval(matchmakingIntervalId);

    matchmakingIntervalId = setInterval(() => {
        const elapsedTime = Math.floor((Date.now() - matchmakingStartTime) / 1000); // Giây đã trôi qua
        const minutes = String(Math.floor(elapsedTime / 60)).padStart(2, '0');
        const seconds = String(elapsedTime % 60).padStart(2, '0');
        if (matchmakingTimerEl) {
            matchmakingTimerEl.textContent = `${minutes}:${seconds}`;
        }
    }, 1000);
}

function stopMatchmakingTimer() {
    if (matchmakingIntervalId) {
        clearInterval(matchmakingIntervalId); // Dừng cập nhật
        matchmakingIntervalId = null;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (!rightPanel) {
        console.error("Không tìm thấy '.right-panel'");
        return;
    }
    const chessworldTitle = document.querySelector('.ml1'); // Chọn thẻ h1

    if (chessworldTitle) {
        chessworldTitle.addEventListener('mouseenter', () => {
            anime.remove('.ml1 .letter');
            anime({
                targets: '.ml1 .letter',
                translateY: [0, -10, 0],
                scale: [1, 1.1, 1],
                rotate: [-5, 5, 0],
                duration: 600,
                delay: anime.stagger(50),
                easing: 'easeOutElastic(1, .6)'
            });
        });

        chessworldTitle.addEventListener('mouseleave', () => {
            anime.remove('.ml1 .letter');
            anime({
                targets: '.ml1 .letter',
                translateY: 0,
                scale: 1,
                rotate: 0,
                duration: 300,
                delay: anime.stagger(30),
                easing: 'easeOutQuad'
            });
        });
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
    const token = localStorage.getItem("token");
    const playerId = localStorage.getItem("playerId");
    connectMainSocket(token, playerId);
    loadSidebarUserInfo();

    // Sử dụng Ủy quyền sự kiện (Event Delegation)
    rightPanel.addEventListener('click', async function (event) {

        // 1. Click "Chơi trực tuyến"
        const onlineModeBtn = event.target.closest('.mode[data-mode="online"]');
        if (onlineModeBtn) {
            showLobbyView();
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
            // ✅ HIỂN THỊ LẠI POPUP CHỌN THỜI GIAN
            const selectedTime = await selectTimeControl();
            if (selectedTime !== null) {
                showMatchmakingPopup(); // Hiện popup chờ
                matchmakingBtn.disabled = true;
                matchmakingBtn.textContent = "Đang tìm...";
                // Gửi yêu cầu tìm trận KÈM thời gian
                sendMessage({
                    type: "join", // Hoặc type khác tùy server
                    playerName: playerName,
                    playerId: localStorage.getItem("playerId"),
                    timeControl: selectedTime
                });
            }
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

    // ĐĂNG XUẤT – BẮT RIÊNG VÌ NÚT NẰM Ở SIDEBAR, KHÔNG NẰM TRONG rightPanel
    const logoutButton = document.getElementById('logoutBtn');
    if (logoutButton) {
        logoutButton.addEventListener('click', logout);
    }
    // LẮNG NGHE SỰ KIỆN CẬP NHẬT AVATAR TỪ BẤT KỲ TRANG NÀO (Account_setting.js)
    window.addEventListener('userInfoUpdated', (e) => {
        const { username, avatarUrl } = e.detail || {};

        if (username) {
            const el = document.getElementById('sidebarUsername');
            if (el) el.textContent = username;
            playerName = username; // cập nhật biến toàn cục luôn
        }

        if (avatarUrl) {
            const img = document.getElementById('sidebarAvatar');
            if (img) img.src = avatarUrl + '?t=' + Date.now();
        }
    });
});