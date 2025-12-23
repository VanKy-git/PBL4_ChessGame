// File: Home_page.js
import {connectMainSocket, sendMessage, registerHandler} from "./Connect_websocket.js";

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
// === THÊM ELEMENT CHO POPUP CHỌN THỜI GIAN ===
const timeControlOverlay = document.getElementById('time-control-overlay');
const timeOptionsContainer = timeControlOverlay?.querySelector('.time-options');
const cancelTimeSelectionBtn = document.getElementById('cancelTimeSelectionBtn');
// 1. Lấy các Element
const aiOverlay = document.getElementById('ai-setup-overlay');
const startAiBtn = document.getElementById('startAiGameBtn');
const cancelAiBtn = document.getElementById('cancelAiSetupBtn');
const eloBtns = document.querySelectorAll('.elo-btn');
const aiTimeBtns = document.querySelectorAll('.time-btn-ai');
const colorBtns = document.querySelectorAll('.color-btn');

// Biến lưu cấu hình đang chọn
let selectedElo = 1350;
let selectedAiTime = 600000;
let selectedColor = 'random'; // Thêm biến chọn màu
let matchmakingIntervalId = null; // ID để dừng setInterval
let matchmakingStartTime = 0;   // Thời điểm bắt đầu tìm trận

// Biến lưu trữ HTML gốc
let originalModesHTML = '';
const rightPanel = document.querySelector('.right-panel');

// === CÁC BIẾN CHO TẠO PHÒNG MỚI ===
const createRoomPopup = document.getElementById('create-room-popup');
const confirmCreateRoomBtn = document.getElementById('confirmCreateRoomBtn');
const cancelCreateRoomBtn = document.getElementById('cancelCreateRoomBtn');
const waitingRoomOverlay = document.getElementById('waiting-room-overlay');
const waitingRoomIdDisplay = document.getElementById('waitingRoomIdDisplay');
const cancelWaitingRoomBtn = document.getElementById('cancelWaitingRoomBtn');
const roomListPopup = document.getElementById('room-list-popup');
const roomListLink = document.getElementById('roomListLink');
const closeRoomListBtn = document.getElementById('closeRoomListBtn');
const roomListTabContent = document.getElementById('roomListTabContent');
const roomListTabs = document.querySelectorAll('#room-list-popup .tab-btn');

let selectedCreateTime = 300000; // Default 5 phút
let selectedVisibility = 'public'; // Default public
let currentRoomListTab = 'waiting'; // 'waiting' or 'playing'
let allRooms = []; // Lưu trữ tất cả các phòng để filter


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

// Hàm này giờ sẽ cập nhật dữ liệu và gọi render
function updateRoomList(rooms) {
    allRooms = rooms || [];
    renderRoomList(); // Render cho popup bên trái
}

// Hàm mới để render danh sách phòng vào popup bên trái
function renderRoomList() {
    if (!roomListTabContent) return;

    const filteredRooms = allRooms.filter(room => {
        if (currentRoomListTab === 'waiting') return room.status === 'waiting';
        if (currentRoomListTab === 'playing') return room.status === 'playing';
        return false;
    });

    if (filteredRooms.length === 0) {
        roomListTabContent.innerHTML = '<p class="muted" style="text-align: center; padding: 20px;">Không có phòng nào.</p>';
        return;
    }

    roomListTabContent.innerHTML = `
        <ul class="room-list">
            ${filteredRooms.map(room => `
                <li class="room-item">
                    <div class="room-info">
                        <span class="room-id">Phòng #${room.roomId}</span>
                        <span class="room-time"><i class="fa-regular fa-clock"></i> ${room.timeControl / 60000} phút</span>
                        <span class="room-players"><i class="fa-solid fa-user"></i> ${room.players ? room.players.join(' vs ') : (room.playerCount + '/2')}</span>
                    </div>
                    <div class="room-actions">
                        ${room.status === 'waiting' ? `<button class="btn-join-list" data-roomid="${room.roomId}">Vào chơi</button>` : ''}
                        ${room.status === 'playing' ? `<button class="btn-watch-list" data-roomid="${room.roomId}">Xem</button>` : ''}
                    </div>
                </li>
            `).join('')}
        </ul>
    `;
}


// 3. Sự kiện đóng Popup
if (cancelAiBtn) {
    cancelAiBtn.addEventListener('click', () => {
        aiOverlay.classList.add('hidden');
        aiOverlay.style.display = 'none';
    });
}

// 4. Logic chọn Elo (Highlight nút được chọn)
eloBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
        // Xóa class selected ở tất cả nút
        eloBtns.forEach(b => b.classList.remove('selected'));
        // Thêm vào nút vừa bấm
        e.target.classList.add('selected');
        // Lưu giá trị
        selectedElo = parseInt(e.target.dataset.elo);
    });
});

// 5. Logic chọn Thời gian (Highlight nút được chọn)
aiTimeBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
        aiTimeBtns.forEach(b => b.classList.remove('selected'));
        e.target.classList.add('selected');
        selectedAiTime = parseInt(e.target.dataset.time);
    });
});

// Logic chọn màu quân
colorBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
        // SỬA LỖI: Dùng e.currentTarget để đảm bảo lấy đúng nút (kể cả khi click vào icon bên trong)
        colorBtns.forEach(b => b.classList.remove('selected'));
        e.currentTarget.classList.add('selected');
        selectedColor = e.currentTarget.dataset.color;
    });
});

// 6. Gửi lệnh tạo game xuống Server
if (startAiBtn) {
    startAiBtn.addEventListener('click', () => {
        console.log(`Creating AI Game: Elo ${selectedElo}, Time ${selectedAiTime}, Color: ${selectedColor}`);

        sendMessage({
            type: "create_ai_game",
            elo: selectedElo,
            timeControl: selectedAiTime,
            color: selectedColor // Gửi màu đã chọn
        });

        // Ẩn popup
        aiOverlay.classList.add('hidden');
        aiOverlay.style.display = 'none';

        // Ẩn giao diện Lobby (nếu có), hiện bàn cờ
        window.showGameControlsView(true); // true để báo là game AI
    });
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

// *** HÀM GLOBAL ĐỂ ẨN TẤT CẢ OVERLAY CHỜ ***
window.hideWaitingOverlays = function() {
    if (waitingRoomOverlay) waitingRoomOverlay.classList.add('hidden');
    if (matchmakingOverlay) matchmakingOverlay.classList.add('hidden');
    stopMatchmakingTimer(); // Cũng dừng timer nếu có
};


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

function getGameControlsHTML(isAiGame = false) {
    // Nút Cầu hòa sẽ được ẩn bằng CSS nếu là game AI
    const aiButtons = isAiGame ? `
        <button id="takeBackBtn" class="btn-action">Đi lại</button>
    ` : '';

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
            ${aiButtons}
            <button id="drawRequestBtn" class="btn-action">Cầu hòa</button>
            <button id="resignBtn" class="btn-action btn-warning">Đầu hàng</button>
            <button id="exitRoomBtn" class="btn-action btn-danger">Thoát phòng</button>
        </div>
    </div>`;
}

function showModesView() {
    if (rightPanel) rightPanel.innerHTML = originalModesHTML;
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

window.showGameControlsView = function(isAiGame = false) {
    if (rightPanel) {
        rightPanel.innerHTML = getGameControlsHTML(isAiGame);
        if (isAiGame) {
            rightPanel.classList.add('ai-game-view');
        } else {
            rightPanel.classList.remove('ai-game-view');
        }
    }

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

// === SIDEBAR USER INFO + LOGOUT (THÊM VÀO ĐÂY) ===
async function loadSidebarUserInfo() {
    const usernameEl = document.getElementById('sidebarUsername');
    const playerIdEl = document.getElementById('sidebarPlayerId');
    const avatarEl   = document.getElementById('sidebarAvatar');

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
        console.error("Failed to load user data from DB:", err);
        // Nếu API lỗi → mới dùng cache
        const cached = localStorage.getItem("playerName");
        usernameEl.textContent = cached ? cached : "Guest";
        playerIdEl.textContent = `ID: #${playerId}`;
    }
}

async function logout() {
    // HIỆN POPUP XÁC NHẬN
    const confirmed = await showConfirmationPopup(
        "Xác nhận đăng xuất",
        "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này?"
    );

    if (!confirmed) return;

    const playerId = localStorage.getItem("playerId");
    const token = localStorage.getItem("token");

    // ✅ CẬP NHẬT STATUS THÀNH "OFFLINE"
    if (playerId) {
        try {
            const requestBody = { userId: parseInt(playerId), status: "Offline" };
            await fetch(`http://localhost:8910/api/updateStatus`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": token ? `Bearer ${token}` : ""
                },
                body: JSON.stringify(requestBody)
            });
        } catch (e) {
            console.error("❌ [LOGOUT] Error calling updateStatus API:", e);
        }
    }

    // ✅ XÓA SẠCH DỮ LIỆU
    localStorage.removeItem("token");
    localStorage.removeItem("playerId");
    localStorage.removeItem("playerName");
    localStorage.removeItem("avatarUrl");
    localStorage.removeItem("userData");
    localStorage.removeItem("googleAuthMode");

    // ✅ Reset UI
    const usernameEl = document.getElementById('sidebarUsername');
    if (usernameEl) usernameEl.textContent = "Guest";

    // ✅ CHUYỂN TRANG
    window.location.href = "../html/MainLogin.html";
}

// === LOGIC CHO CÁC POPUP MỚI ===

// 1. Logic cho popup tạo phòng
if (createRoomPopup) {
    const timeBtns = createRoomPopup.querySelectorAll('.time-btn-create');
    timeBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            timeBtns.forEach(b => b.classList.remove('selected'));
            e.currentTarget.classList.add('selected');
            selectedCreateTime = parseInt(e.currentTarget.dataset.time);
        });
    });

    const visBtns = createRoomPopup.querySelectorAll('.visibility-btn');
    visBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            visBtns.forEach(b => b.classList.remove('selected'));
            e.currentTarget.classList.add('selected');
            selectedVisibility = e.currentTarget.dataset.vis;
        });
    });

    if (confirmCreateRoomBtn) {
        confirmCreateRoomBtn.addEventListener('click', () => {
            sendMessage({
                type: "create_room",
                timeControl: selectedCreateTime,
                visibility: selectedVisibility
            });
            createRoomPopup.classList.add('hidden');
            if (waitingRoomOverlay) {
                waitingRoomOverlay.classList.remove('hidden');
                if (waitingRoomIdDisplay) waitingRoomIdDisplay.textContent = "Đang tạo...";
            }
        });
    }

    if (cancelCreateRoomBtn) {
        cancelCreateRoomBtn.addEventListener('click', () => {
            createRoomPopup.classList.add('hidden');
        });
    }
}

// 2. Logic cho overlay chờ
if (cancelWaitingRoomBtn) {
    cancelWaitingRoomBtn.addEventListener('click', () => {
        const roomId = cancelWaitingRoomBtn.dataset.roomid;
        if (roomId) {
            sendMessage({ type: "cancel_waiting_room", roomId: roomId });
        }
        waitingRoomOverlay.classList.add('hidden');
    });
}

// 3. Logic cho popup danh sách phòng (bên trái)
if (roomListLink) {
    roomListLink.addEventListener('click', (e) => {
        e.preventDefault();
        if (roomListPopup) {
            roomListPopup.classList.remove('hidden');
            sendMessage({ type: "get_rooms" }); // Lấy danh sách mới nhất
        }
    });
}

if (closeRoomListBtn) {
    closeRoomListBtn.addEventListener('click', () => {
        if (roomListPopup) roomListPopup.classList.add('hidden');
    });
}

if (roomListTabs) {
    roomListTabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            roomListTabs.forEach(t => t.classList.remove('active'));
            e.currentTarget.classList.add('active');
            currentRoomListTab = e.currentTarget.dataset.tab;
            renderRoomList(); // Render lại list với tab mới
        });
    });
}

// Ủy quyền sự kiện cho các nút trong danh sách phòng
if (roomListTabContent) {
    roomListTabContent.addEventListener('click', (e) => {
        const joinBtn = e.target.closest('.btn-join-list');
        if (joinBtn && window.joinRoom) {
            const roomId = joinBtn.dataset.roomid;
            if(rightPanel.querySelector('#joinRoomIdInput')) {
                 rightPanel.querySelector('#joinRoomIdInput').value = roomId;
                 window.joinRoom();
            } else {
                showLobbyView();
                setTimeout(() => {
                    if(rightPanel.querySelector('#joinRoomIdInput')) {
                        rightPanel.querySelector('#joinRoomIdInput').value = roomId;
                        window.joinRoom();
                    }
                }, 100);
            }
            if (roomListPopup) roomListPopup.classList.add('hidden');
        }

        const watchBtn = e.target.closest('.btn-watch-list');
        if (watchBtn && window.watchRoom) {
            const roomId = watchBtn.dataset.roomid;
            window.watchRoom(roomId);
            if (roomListPopup) roomListPopup.classList.add('hidden');
        }
    });
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
    });
    // Lưu lại HTML ban đầu
    originalModesHTML = rightPanel.innerHTML;
    const token = localStorage.getItem("token");
    const playerId = localStorage.getItem("playerId");
    connectMainSocket(token, playerId);

    loadSidebarUserInfo();

    // ĐĂNG XUẤT – BẮT RIÊNG VÌ NÚT NẰM Ở SIDEBAR
    const logoutButton = document.getElementById('logoutBtn');
    if (logoutButton) {
        logoutButton.addEventListener('click', logout);
    }

    // LẮNG NGHE SỰ KIỆN CẬP NHẬT AVATAR TỪ BẤT KỲ TRANG NÀO
    window.addEventListener('userInfoUpdated', (e) => {
        const { username, avatarUrl } = e.detail || {};
        if (username) {
            const el = document.getElementById('sidebarUsername');
            if (el) el.textContent = username;
            playerName = username;
        }
        if (avatarUrl) {
            const img = document.getElementById('sidebarAvatar');
            if (img) img.src = avatarUrl + '?t=' + Date.now();
        }
    });

    
    // *** CHỈ ĐĂNG KÝ CÁC HANDLER LIÊN QUAN ĐẾN UI Ở ĐÂY ***
    registerHandler('room_list', (msg) => updateRoomList(msg.rooms));
    registerHandler('room_update', (msg) => updateRoomList(msg.rooms));

    // Handler cho room_created, chỉ để cập nhật UI
    registerHandler('room_created', (msg) => {
        console.log("[Home_page] Received room_created, updating UI.");
        if (waitingRoomIdDisplay) {
            waitingRoomIdDisplay.textContent = msg.roomId;
        }
        if (cancelWaitingRoomBtn) {
            cancelWaitingRoomBtn.dataset.roomid = msg.roomId;
        }
    });

    // Handler cho game_start, chỉ để ẩn các overlay
    registerHandler('game_start', (msg) => {
        console.log("[Home_page] Received game_start, hiding overlays.");
        if (window.hideWaitingOverlays) {
            window.hideWaitingOverlays();
        }
    });

    // Sử dụng Ủy quyền sự kiện (Event Delegation)
    rightPanel.addEventListener('click', async function (event) {

        // 1. Click "Chơi trực tuyến"
        const onlineModeBtn = event.target.closest('.mode[data-mode="online"]');
        if (onlineModeBtn) {
            showLobbyView();
        }

        // SỬA LỖI: Xử lý sự kiện click cho nút "Chơi với AI" bằng Event Delegation
        const aiModeBtn = event.target.closest('.mode[data-mode="ai"]');
        if (aiModeBtn) {
            if (aiOverlay) {
                aiOverlay.classList.remove('hidden');
                aiOverlay.style.display = 'flex';
            }
            return;
        }

        // 2. Click "Back"
        const backBtn = event.target.closest('#backToModes');
        if (backBtn) {
            showModesView();
            return;
        }

        // 3. Click "Tạo phòng" -> Sửa để mở popup
        const createRoomBtn = event.target.closest('#createRoomBtn');
        if (createRoomBtn) {
            if (createRoomPopup) {
                createRoomPopup.classList.remove('hidden');
            }
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
            const selectedTime = await selectTimeControl();
            if (selectedTime !== null) {
                showMatchmakingPopup();
                matchmakingBtn.disabled = true;
                matchmakingBtn.textContent = "Đang tìm...";
                sendMessage({
                    type: "join",
                    playerName: playerName,
                    playerId: localStorage.getItem("playerId"),
                    timeControl: selectedTime
                });
            }
            return;
        }
        // 6. Click "Cầu hòa"
        const drawBtn = event.target.closest('#drawRequestBtn');
        if (drawBtn && window.requestDraw) {
            const confirmed = await showConfirmationPopup(
                "Xác nhận Cầu hòa",
                "Bạn có chắc chắn muốn gửi lời đề nghị hòa đến đối thủ?"
            );
            if (confirmed) {
                window.requestDraw();
            }
            return;
        }

        // 7. Click "Đầu hàng"
        const resignBtn = event.target.closest('#resignBtn');
        if (resignBtn && window.resignGame) {
            const confirmed = await showConfirmationPopup(
                "Xác nhận Đầu hàng",
                "Bạn có chắc chắn muốn đầu hàng trận đấu này không?"
            );
            if (confirmed) {
                window.resignGame();
            }
            return;
        }

        // 8. Click "Thoát phòng"
        const exitBtn = event.target.closest('#exitRoomBtn');
        if (exitBtn) {
            const confirmed = await showConfirmationPopup(
                "Xác nhận Thoát phòng",
                "Bạn có chắc chắn muốn thoát khỏi phòng? (Nếu đang chơi, bạn sẽ bị xử thua)."
            );
            if (confirmed) {
                if (window.leaveRoom) {
                    window.leaveRoom();
                }
                showModesView();
            }
            return;
        }
        
        // 11. Click "Đi lại" (Take Back) trong game AI
        const takeBackBtn = event.target.closest('#takeBackBtn');
        if (takeBackBtn && window.requestTakeBack) {
            window.requestTakeBack();
        }
    });
});
