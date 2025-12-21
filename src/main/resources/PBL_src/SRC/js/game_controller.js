// File: game_controller.js
import { sendMessage, registerHandler } from "./Connect_websocket.js";

// ==========================
// 1. TRẠNG THÁI VÀ HẰNG SỐ
// ==========================


// Trạng thái Game
let currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
let playerId = localStorage.getItem("playerId");
let playerName = localStorage.getItem("playerName") || "Guest";
let yourColor = null;
let roomId = null;
let gameActive = false;
let currentTurn = 'white';
let isWhiteMove = true;
let validMoveSquares = [];
let isKingInCheckState = false;
let whiteTimeMs;
let blackTimeMs;
let timerIntervalId = null;
let rematchOfferedByOpponent = false; // Cờ báo đối thủ đã mời tái đấu
let rematchRequestedByMe = false; // Cờ báo mình đã yêu cầu tái đấu
window.validMoveSquares = validMoveSquares;

// Nếu chưa có, hãy thêm vào đầu file này:
const PIECES = {
    'r':'\u265C','n':'\u265E','b':'\u265D','q':'\u265B','k':'\u265A','p':'\u265F', // black
    'R':'\u2656','N':'\u2658','B':'\u2657','Q':'\u2655','K':'\u2654','P':'\u2659'  // white
};

// ... (các biến trạng thái UI và Player Info giữ nguyên) ...
let selectedSquare = null;
let lastMove = null;
let player1Info = null; // Đối thủ (Thanh trên)
let player2Info = null; // Mình (Thanh dưới)

// ✅ Biến cho popup cầu hòa
let drawOfferTimeoutId = null; // ID cho setTimeout tự động từ chối
let drawOfferIntervalId = null; // ID cho setInterval đếm ngược
const DRAW_OFFER_TIMEOUT_SECONDS = 15; // Thời gian chờ

// ✅ Lấy Element cho popup (nên lấy 1 lần)
const drawPopupEl = document.getElementById('draw-offer-popup');
const acceptDrawBtn = document.getElementById('acceptDrawBtn');
const declineDrawBtn = document.getElementById('declineDrawBtn');
const drawCountdownEl = document.getElementById('draw-countdown');

// ✅ Biến cho popup phong cấp
let pendingPromotionMove = null;
const promotionPopupEl = document.getElementById('promotion-overlay');
const cancelPromotionBtn = document.getElementById('cancelPromotionBtn');

// ==========================
// 2. DOM ELEMENTS & HTML TEMPLATES
// ==========================
const rightPanel = document.querySelector('aside.right-panel');

// ... (các biến DOM và hàm HTML (decodeFEN, getLobbyHTML...) giữ nguyên) ...
let statusEl, roomInfoEl, colorInfoEl, chatMessagesEl, chatInputEl, chatSendBtnEl;
function decodeFEN(fen) {        const parts = fen.split(' ');
    const boardPart = parts[0];
    const turn = parts[1] === 'w' ? 'white' : 'black';

    let boardArray = [];
    const ranks = boardPart.split('/');
    ranks.forEach(rank => {
        let row = [];
        for (const char of rank) {
            if (/\d/.test(char)) {
                for (let i = 0; i < parseInt(char); i++) {
                    row.push('');
                }
            } else {
                row.push(char);
            }
        }
        boardArray.push(row);
        // THÊM KIỂM TRA ĐỘ DÀI HÀNG ĐỂ TRÁNH LỖI (Dù FEN chuẩn luôn là 8)
        while (row.length < 8) row.push('');
    });
    return { board: boardArray, turn: turn };}
function isPieceOurColor(piece) {
    if (!piece || !yourColor) return false;
    const pieceColor = piece === piece.toUpperCase() ? 'white' : 'black';
    return pieceColor === yourColor;
}

function showGameOverModal(title, message) {
    const modal = document.getElementById('gameOverModal');
    if (!modal) { alert(`${title} - ${message}`); return; } // Fallback

    document.getElementById('gameOverTitle').textContent = title;
    document.getElementById('gameOverMessage').textContent = message;

    modal.style.display = 'flex';

    // Gắn lại sự kiện cho các nút
    document.getElementById('modalExitBtn').onclick = function() {
        modal.style.display = 'none';
        showModesView();
    };
    document.getElementById('playAgainBtn').onclick = function() {
        modal.style.display = 'none';
        showLobbyView();
    };
}

function createRoom() {
    console.log("createRoom pressed");
    sendMessage({ type: "create_room" });
}
function joinRoom() {
    const id = document.getElementById('joinRoomIdInput').value.trim();
    if (id) sendMessage({ type: "join_room", roomId: id });
}
function findNewGame() {
    gameActive = false;
    currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    renderGame();
    sendMessage({type: "join", playerName: playerName});
}
function requestDraw() { if (gameActive) sendMessage({ type: "draw_request", roomId: roomId }); }
function resignGame() { if (gameActive) sendMessage({ type: "resign", roomId: roomId }); }

function sendChat() {
    const inputElement = document.getElementById('chatInputEl');

    if (!inputElement) {
        console.error("Lỗi: Không tìm thấy ô nhập chat (#chatInputEl)!");
        return; // Thoát nếu không tìm thấy
    }

    const text = inputElement.value.trim();
    if (text && roomId) {
        sendMessage({ type: "chat", roomId: roomId, message: text });
        inputElement.value = ''; // Xóa nội dung trong element vừa tìm
    }
}

function leaveRoom() {
    sendMessage({ type: "leave_room", roomId: roomId });
    resetGameLocalState();
}

function onPlayerLeft(msg)
{
    onEndGame(msg);
}

function requestRematch() {
    if (roomId && !gameActive) {
        sendMessage({ type: "rematch_request", roomId: roomId });
        rematchRequestedByMe = true;
        console.log("Da gui rematch");
        const rematchBtn = document.getElementById('gameOverRematchBtn');
        if(rematchBtn) {
            rematchBtn.disabled = true;
            rematchBtn.textContent = "Đang chờ đối thủ...";
        }
    }
}
window.requestRematch = requestRematch;

function enableLobbyButtons() {
    const createBtn = document.getElementById('createRoomBtn');
    const joinBtn = document.getElementById('joinRoomBtn');
    const matchBtn = document.getElementById('matchmakingBtn');
    const lobbyStatusEl = document.getElementById('lobbyStatus');

    if (createBtn) createBtn.disabled = false;
    if (joinBtn) joinBtn.disabled = false;
    if (matchBtn) matchBtn.disabled = false;
    if (lobbyStatusEl) lobbyStatusEl.textContent = "Đã kết nối, sẵn sàng tạo phòng.";
}

function onPlayerInfo(msg) {
    // window.location.href = "Home_page.html";
    // alert(msg);
    playerId = msg.playerId;
    playerName = msg.playerName;
    localStorage.setItem("playerId", playerId);
    player2Info = { id: playerId, name: playerName, elo: msg.elo || 1200 };
    updatePlayerBars();
    enableLobbyButtons();
}

function onRoomCreatedOrJoined(msg) {
    if(msg.roomId) {
        roomId = msg.roomId;
    }
    if(msg.color)
    yourColor = msg.color;
    if (window.hideMatchmakingPopup) window.hideMatchmakingPopup();
    window.showGameControlsView();
    updateStatus()
    renderGame();
}

function resetGameLocalState() {
    console.log("Resetting local game state..."); // DEBUG
    gameActive = false;
    currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    yourColor = null;
    roomId = null;
    currentTurn = 'white';
    selectedSquare = null;
    lastMove = null;
    player1Info = null;
    stopTimer();
    whiteTimeMs = 60000;
    blackTimeMs = 60000;
    player1Info = null;
    const capturedAreas = document.querySelectorAll('.captured-pieces'); // ✅ Xóa quân ăn được cũ
    capturedAreas.forEach(area => area.innerHTML = '');
    updateTimerDisplay();
    const p1Bar = document.getElementById('player1Bar');
    const p2Bar = document.getElementById('player2Bar');
    if (p1Bar) p1Bar.classList.add('hidden');
    if (p2Bar) p2Bar.classList.add('hidden');
    isKingInCheckState = false;
    validMoveSquares = [];
    window.validMoveSquares = [];
    rematchOfferedByOpponent = false;
    rematchRequestedByMe = false;

    const moveListEl = document.getElementById('moveList');
    if (moveListEl) moveListEl.innerHTML = '';

    const chatMessages = document.getElementById('chatMessagesEl');
    if (chatMessages) chatMessages.innerHTML = '';

    // Reset hiển thị trạng thái (tùy chọn, có thể để onEndGame làm)
    updateStatus();
    renderGame(); // Render lại bàn cờ ban đầu
    updateTimerDisplay();
}

function updateTimerDisplay() {
    // Lấy element mỗi lần để đảm bảo không bị null
    const p1TimeEl = document.querySelector('#player1Bar .player-time');
    const p2TimeEl = document.querySelector('#player2Bar .player-time');
    const p1Bar = document.getElementById('player1Bar');
    const p2Bar = document.getElementById('player2Bar');

    if (!p1TimeEl || !p2TimeEl || !p1Bar || !p2Bar) return;

    const formatTime = (ms) => {
        if (ms < 0) ms = 0;
        const totalSeconds = Math.floor(ms / 1000);
        const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
        const seconds = String(totalSeconds % 60).padStart(2, '0');
        return `${minutes}:${seconds}`;
    };

    // Hiển thị thời gian
    if (yourColor === 'white') {
        p2TimeEl.textContent = formatTime(whiteTimeMs);
        p1TimeEl.textContent = formatTime(blackTimeMs);
        // Thêm/Xóa class low-time
        p2Bar.classList.toggle('low-time', whiteTimeMs < 30000 && whiteTimeMs > 0);
        p1Bar.classList.toggle('low-time', blackTimeMs < 30000 && blackTimeMs > 0);
        // Thêm/Xóa class active-turn
        p2Bar.classList.toggle('active-turn', currentTurn === 'white');
        p1Bar.classList.toggle('active-turn', currentTurn === 'black');
    } else { // yourColor là 'black' hoặc null
        p2TimeEl.textContent = formatTime(blackTimeMs);
        p1TimeEl.textContent = formatTime(whiteTimeMs);
        // Thêm/Xóa class low-time
        p2Bar.classList.toggle('low-time', blackTimeMs < 30000 && blackTimeMs > 0);
        p1Bar.classList.toggle('low-time', whiteTimeMs < 30000 && whiteTimeMs > 0);
        // Thêm/Xóa class active-turn
        p2Bar.classList.toggle('active-turn', currentTurn === 'black');
        p1Bar.classList.toggle('active-turn', currentTurn === 'white');
    }
}

function updateCapturedPieces(capturingColor, capturedPieceChar) {
    if (!capturedPieceChar || capturedPieceChar === '.') return; // Bỏ qua nếu không có quân bị ăn

    let fileName = '';
    if (capturedPieceChar === capturedPieceChar.toUpperCase()) {
        fileName = capturedPieceChar; // Quân trắng
    } else {
        fileName = 'b' + capturedPieceChar.toUpperCase(); // Quân đen
    }

    let targetArea;
    // Nếu Trắng ăn -> thêm vào khu vực của Trắng (player 2)
    if (capturingColor === 'white' && yourColor === 'white') {
        targetArea = document.querySelector('#player2Bar .captured-pieces');
    }
    // Nếu Đen ăn -> thêm vào khu vực của Đen (player 1)
    else if (capturingColor === 'black' && yourColor === 'white') {
        targetArea = document.querySelector('#player1Bar .captured-pieces');
    }
    // Nếu bạn là Đen:
    else if (capturingColor === 'white' && yourColor === 'black') {
        targetArea = document.querySelector('#player1Bar .captured-pieces');
    }
    else if (capturingColor === 'black' && yourColor === 'black') {
        targetArea = document.querySelector('#player2Bar .captured-pieces');
    }

    if (targetArea) {
        const img = document.createElement('img');
        img.src = `../../PBL4_imgs/image/${fileName}.png`;
        img.alt = capturedPieceChar;
        img.classList.add('captured-piece-icon');
        targetArea.appendChild(img);
    }
}

function onGameStart(msg) {
    // Reset chat và lịch sử
    const chatMessages = document.getElementById('chatMessagesEl');
    if (chatMessages) chatMessages.innerHTML = '';
    const moveListEl = document.getElementById('moveList');
    if (moveListEl) moveListEl.innerHTML = '';

    isKingInCheckState = false;
    gameActive = true;
    if (msg.playerWhite && msg.playerBlack) {
        const whitePlayer = msg.playerWhite;
        const blackPlayer = msg.playerBlack;
        if (whitePlayer.id === playerId) {
            player2Info = whitePlayer; player1Info = blackPlayer; yourColor = 'white';
        } else {
            player2Info = blackPlayer; player1Info = whitePlayer; yourColor = 'black';
        }
    }
    if (msg.gameState) {
        currentFEN = msg.gameState;
    } else {
        // Fallback phòng trường hợp server không gửi FEN
        currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }
    if (msg.initialTimeMs) {
        whiteTimeMs = msg.initialTimeMs;
        blackTimeMs = msg.initialTimeMs;
    }
    if (msg.currentTurn) {
        currentTurn = msg.currentTurn;
    }
    if (window.hideMatchmakingPopup) window.hideMatchmakingPopup();
    const gameOverOverlay = document.getElementById('game-over-overlay');
    if (gameOverOverlay) gameOverOverlay.classList.add('hidden');
    // Gọi renderGame() trực tiếp
    startTimer(); // Bắt đầu timer client
    const p1Bar = document.getElementById('player1Bar');
    const p2Bar = document.getElementById('player2Bar');
    if (p1Bar) p1Bar.classList.remove('hidden');
    if (p2Bar) p2Bar.classList.remove('hidden');
    renderGame();   // Vẽ bàn cờ
    updatePlayerBars(); // ✅ Cập nhật tên người chơi sau khi có info
    updateStatus();   // Cập nhật lượt đi
    updateTimerDisplay(); // ✅ Hiển thị thời gian ban đầu
}

function startTimer() {
    stopTimer(); // Dừng interval cũ trước khi bắt đầu cái mới
    if (!gameActive) return; // Không chạy nếu game chưa active

    console.log("Starting client-side timer..."); // DEBUG

    timerIntervalId = setInterval(() => {
        // Chỉ giảm thời gian nếu game đang active
        if (!gameActive) {
            stopTimer();
            return;
        }

        // Giảm thời gian của người đang đến lượt
        if (currentTurn === 'white') {
            whiteTimeMs -= 1000;
        } else if (currentTurn === 'black') { // Thêm else if cho chắc
            blackTimeMs -= 1000;
        }

        // Đảm bảo thời gian không âm
        if (whiteTimeMs < 0) whiteTimeMs = 0;
        if (blackTimeMs < 0) blackTimeMs = 0;

        // Cập nhật hiển thị
        updateTimerDisplay();

        // (Không cần kiểm tra hết giờ ở đây, server sẽ lo việc đó)
        // if (whiteTimeMs <= 0 || blackTimeMs <= 0) {
        //     stopTimer();
        // }
    }, 1000); // Chạy mỗi giây
}

// Xử lý khi nhận màu mới (QUAN TRỌNG CHO TÁI ĐẤU)
function onColorAssigned(msg) {
    if (msg.color) {
        yourColor = msg.color; // Cập nhật màu của mình
        console.log("Color assigned/updated:", yourColor);
        if(colorInfoEl) colorInfoEl.textContent = yourColor === 'white' ? 'Trắng' : 'Đen';
        // Render lại bàn cờ để flip nếu cần
        renderGame();
    }
}


// ✅ Handler khi đối thủ mời tái đấu
function onRematchOffer(msg) {
    console.log("Rematch offer received from:", msg.offeringPlayer);
    rematchOfferedByOpponent = true; // Đánh dấu đối thủ đã mời

    // Hiển thị thông báo trên popup Game Over (nếu đang hiển thị)
    // Hoặc tạo một popup mời tái đấu riêng (nếu muốn)
    const rematchBtn = document.getElementById('gameOverRematchBtn');
    if (rematchBtn && !rematchBtn.disabled) { // Chỉ cập nhật nếu mình chưa yêu cầu
        rematchBtn.textContent = "Chấp nhận Tái đấu!"; // Đổi text nút
        // Có thể thêm hiệu ứng nhấp nháy cho nút
        rematchBtn.classList.add('rematch-offer-pulse'); // Thêm class CSS (cần định nghĩa)
    }
    // TÙY CHỌN: Hiển thị popup mời tái đấu riêng biệt
    // if (window.showRematchOfferPopup) {
    //     window.showRematchOfferPopup(msg.offeringPlayer);
    // } else {
    //     alert(`${msg.offeringPlayer} muốn tái đấu! Nhấn nút "Tái đấu" để chấp nhận.`);
    // }
}

function onRematchUnavailable(msg) {
    console.log("Rematch unavailable:", msg.reason);
    rematchOfferedByOpponent = false; // Reset cờ
    // Vô hiệu hóa và cập nhật nút Tái đấu trên popup Game Over
    const rematchBtn = document.getElementById('gameOverRematchBtn');
    if (rematchBtn) {
        rematchBtn.disabled = true;
        rematchBtn.textContent = "Đối thủ đã rời";
        rematchBtn.classList.remove('rematch-offer-pulse');
    }
    alert(msg.reason || "Đối thủ đã rời, không thể tái đấu.");
}

function stopTimer() {
    if (timerIntervalId) {
        clearInterval(timerIntervalId);
        timerIntervalId = null;
        console.log("Client-side timer stopped."); // DEBUG
    }
}

function onMoveResult(msg) {
    // ✅ SỬA LẠI: Kiểm tra msg.lastMove là object và có from/to
    if (msg.fen && msg.lastMove && typeof msg.lastMove === 'object' && msg.lastMove.from && msg.lastMove.to && window.algToCoord) {

        const fromCoord = window.algToCoord(msg.lastMove.from); // Đọc từ object lastMove
        const toCoord = window.algToCoord(msg.lastMove.to);     // Đọc từ object lastMove

        // Lấy bàn cờ TRƯỚC KHI cập nhật FEN
        const oldGameData = decodeFEN(currentFEN);
        const oldBoardArray = oldGameData.board;
        const turnBeforeMove = oldGameData.turn; // Lượt đi trước đó

        // Lấy ký tự quân cờ TỪ BÀN CỜ CŨ
        let movedPieceChar = '';
        if (fromCoord && oldBoardArray[fromCoord.r] !== undefined && oldBoardArray[fromCoord.r][fromCoord.c] !== undefined) {
            movedPieceChar = oldBoardArray[fromCoord.r][fromCoord.c];
        } else {
            console.warn("Cannot get moved piece char from 'from' coord:", fromCoord);
        }

        // ✅ KIỂM TRA ĂN QUÂN
        let capturedPieceChar = null;
        if (toCoord && oldBoardArray[toCoord.r]?.[toCoord.c]) {
            capturedPieceChar = oldBoardArray[toCoord.r][toCoord.c];
        }
        // Xử lý ăn Tốt qua đường (nếu có)
        const movedPieceLower = movedPieceChar.toLowerCase();
        if (movedPieceLower === 'p' && fromCoord.c !== toCoord.c && capturedPieceChar === '') {
            // Đây có thể là en passant, quân bị ăn là tốt đối phương ở cùng cột đích, khác hàng
            const capturedPawnRow = turnBeforeMove === 'white' ? toCoord.r + 1 : toCoord.r - 1;
            capturedPieceChar = turnBeforeMove === 'white' ? 'p' : 'P'; // Xác định màu tốt bị ăn
        }

        // Cập nhật trạng thái mới
        currentFEN = msg.fen;
        isKingInCheckState = msg.isCheck || false;
        const newGameData = decodeFEN(currentFEN);
        currentTurn = newGameData.turn; // Cập nhật lượt đi MỚI
        lastMove = { from: fromCoord, to: toCoord }; // Lưu dạng {r, c} cho renderGame
        console.log('Updated lastMove variable:', lastMove);

        // Gọi addMoveToHistory với dữ liệu từ msg.lastMove
        addMoveToHistory(msg.lastMove.from, msg.lastMove.to, turnBeforeMove, movedPieceChar);

        if (capturedPieceChar) {
            updateCapturedPieces(turnBeforeMove, capturedPieceChar);
        }
        // Render và cập nhật status
        renderGame();
        updateStatus();

    } else {
        // Log chi tiết hơn nếu thiếu dữ liệu
        console.warn("Received move_result with missing/invalid data (fen, lastMove.from, lastMove.to):", msg);
        if (msg.result === false) {
            alert("Nước đi không hợp lệ: " + (msg.message || ''));
        }
        // Thêm dòng này để render lại trạng thái cũ nếu nước đi không hợp lệ nhưng có fen (hiếm gặp)
        else if (msg.fen) {
            currentFEN = msg.fen; // Cập nhật FEN nếu có, dù lastMove thiếu
            renderGame();
            updateStatus();
        }
    }
}

function onChat(msg) {
    addChatMessage(msg.playerName, msg.message);
}

function onEndGame(msg) {
    gameActive = false; // Dừng game
    stopTimer(); // Dừng đồng hồ

    const winner = msg.winner; // "white", "black", "draw"
    const reason = msg.reason || null; // Lý do từ server

    let result = 'draw'; // Mặc định là hòa
    if (winner === yourColor) {
        result = 'win';
    } else if (winner !== 'draw') {
        result = 'loss';
    }

    // Gọi hàm hiển thị popup từ Home_page.js
    if (window.showGameOverPopup) {
        window.showGameOverPopup(result, reason);
        const rematchBtn = document.getElementById('gameOverRematchBtn');
        if(rematchBtn) {
            rematchBtn.disabled = false;
            rematchBtn.textContent = "Tái đấu";
            rematchBtn.classList.remove('rematch-offer-pulse');
        }
    } else {
        // Fallback nếu hàm chưa sẵn sàng
        alert(`Kết quả: ${result} - Lý do: ${reason || 'Kết thúc trận'}`);
    }
    if (statusEl) statusEl.textContent = "Trận đấu đã kết thúc.";
    // resetGameLocalState();
}

function addMoveToHistory(fromAlg, toAlg, movedColor, pieceChar) {
    const moveListEl = document.getElementById('moveList');
    if (!moveListEl) return;

    const pieceIcon = PIECES[pieceChar] || '';
    const moveText = `${pieceIcon}${toAlg}`;

    let listItem;

    if (movedColor === 'white') {
        // Trắng vừa đi -> Bắt đầu dòng mới
        listItem = document.createElement('li');

        // Tạo span cho nước đi của Trắng
        const whiteMoveSpan = document.createElement('span');
        whiteMoveSpan.className = 'move white-move'; // Thêm class
        whiteMoveSpan.textContent = moveText;

        listItem.appendChild(whiteMoveSpan); // Nối span vào

        moveListEl.appendChild(listItem);

    } else {
        // Đen vừa đi -> Tìm dòng cuối cùng và nối vào
        listItem = moveListEl.lastElementChild;
        if (listItem) {
            // Tạo span cho nước đi của Đen
            const blackMoveSpan = document.createElement('span');
            blackMoveSpan.className = 'move black-move'; // Thêm class
            blackMoveSpan.textContent = moveText;

            // Thêm khoảng trắng trước khi nối span của Đen
            listItem.appendChild(document.createTextNode(' ')); // Thêm space node
            listItem.appendChild(blackMoveSpan); // Nối span vào
        } else {
            // ... (Xử lý lỗi như cũ) ...
            console.error("Lỗi lịch sử: Nước đi của Đen không có dòng để nối vào!");
            listItem = document.createElement('li');
            listItem.textContent = `?. ... ${moveText}`;
            moveListEl.appendChild(listItem);
        }
    }

    moveListEl.scrollTop = moveListEl.scrollHeight;
}

function onError(msg) {
    alert('Lỗi: ' + msg.message);
}

function updateStatus() {
    // Tìm element nếu chưa có (làm 1 lần)
// ✅ LUÔN TÌM LẠI ELEMENT MỖI LẦN CHẠY
    statusEl = document.getElementById('gameStatus');
    roomInfoEl = document.getElementById('roomInfoEl');
    colorInfoEl = document.getElementById('colorInfoEl');

    // Kiểm tra xem các element có tồn tại không (quan trọng sau khi đổi view)
    if (!statusEl || !roomInfoEl || !colorInfoEl) {
        // Có thể không cần báo lỗi, vì có lúc view không phải là game controls
        // console.warn("updateStatus: Không tìm thấy các element status/room/color (có thể đang ở lobby/modes)");
        return;
    }

    // ✅ SỬA LỖI 2 & 3: Kiểm tra tất cả element cần thiết
    if (!statusEl || !roomInfoEl || !colorInfoEl) {
        console.error("Lỗi: Không tìm thấy các element status/room/color!");
        return;
    }

    if (!gameActive) {
        statusEl.textContent = 'Chưa có game';
        roomInfoEl.textContent = '-';
        colorInfoEl.textContent = '-';
        statusEl.style.backgroundColor = ''; // Reset màu nền
        // colorInfoEl.style.color = ''; // Reset màu chữ (tùy chọn)
        return;
    }

    // ✅ SỬA LỖI 3: Thêm kiểm tra null/undefined
    roomInfoEl.textContent = roomId ? roomId : '-';

    // ✅ SỬA LỖI 1 & 4: Dùng style.color và so sánh trực tiếp
    if (yourColor === "white") {
        colorInfoEl.textContent = "Trắng";
        colorInfoEl.style.color = "white"; // Hoặc màu trắng: "#DDDDDD" để nổi bật trên nền tối
    } else if (yourColor === "black") {
        colorInfoEl.textContent = "Đen";
        colorInfoEl.style.color = "black";
    } else {
        colorInfoEl.textContent = '-'; // Trường hợp chưa có màu
        colorInfoEl.style.color = ''; // Reset màu
    }

    // Tạo text hiển thị lượt đi
    const turnText = currentTurn === 'white' ? 'Trắng' : 'Đen';
    const isMyTurn = currentTurn === yourColor;
    statusEl.textContent = `Lượt: ${turnText}${isMyTurn ? ' (Bạn)' : ''}`;

    // Đổi màu nền dựa trên lượt đi
    statusEl.style.backgroundColor = isMyTurn ? '#b3081c' : '#fff3cd';
}

// ✅ Hàm đóng popup và dọn dẹp timer/listener
function closeDrawOfferPopup() {
    if (drawPopupEl) drawPopupEl.classList.remove('show'); // Ẩn popup (dùng class 'show')
    // Dừng các timer
    if (drawOfferTimeoutId) clearTimeout(drawOfferTimeoutId);
    if (drawOfferIntervalId) clearInterval(drawOfferIntervalId);
    drawOfferTimeoutId = null;
    drawOfferIntervalId = null;
    // Xóa listener để tránh gọi nhiều lần (quan trọng)
    if (acceptDrawBtn) acceptDrawBtn.onclick = null;
    if (declineDrawBtn) declineDrawBtn.onclick = null;
}

// ✅ Handler khi nhận lời cầu hòa
function onDrawOfferReceived(msg) {
    if (!gameActive || !drawPopupEl) return; // Chỉ hiển thị khi đang chơi game

    console.log("Draw Offer received.");
    closeDrawOfferPopup(); // Đóng popup cũ nếu có (đề phòng)

    let countdown = DRAW_OFFER_TIMEOUT_SECONDS;
    if (drawCountdownEl) drawCountdownEl.textContent = countdown; // Hiển thị số giây ban đầu

    // Hiển thị popup với hiệu ứng
    drawPopupEl.classList.remove('hidden'); // Bỏ ẩn
    // Dùng setTimeout nhỏ để transition CSS kịp hoạt động
    setTimeout(() => {
        if (drawPopupEl) drawPopupEl.classList.add('show');
    }, 10);

    // Bắt đầu đếm ngược hiển thị
    drawOfferIntervalId = setInterval(() => {
        countdown--;
        if (drawCountdownEl) drawCountdownEl.textContent = countdown;
        if (countdown <= 0) {
            clearInterval(drawOfferIntervalId); // Dừng đếm khi hết giờ
        }
    }, 1000);

    // Tự động từ chối sau X giây
    drawOfferTimeoutId = setTimeout(() => {
        console.log("Draw offer timed out, automatically declining.");
        sendMessage({ type: "draw_response", roomId: roomId, accepted: false });
        closeDrawOfferPopup(); // Đóng popup
    }, DRAW_OFFER_TIMEOUT_SECONDS * 1000);

    // Gắn sự kiện cho nút Đồng ý
    if (acceptDrawBtn) {
        acceptDrawBtn.onclick = () => {
            console.log("Draw accepted by client.");
            sendMessage({ type: "draw_response", roomId: roomId, accepted: true });
            closeDrawOfferPopup(); // Đóng popup
        };
    }

    // Gắn sự kiện cho nút Từ chối
    if (declineDrawBtn) {
        declineDrawBtn.onclick = () => {
            console.log("Draw declined by client.");
            sendMessage({ type: "draw_response", roomId: roomId, accepted: false });
            closeDrawOfferPopup(); // Đóng popup
        };
    }
}

// Handler khi đối thủ từ chối (server gửi 'draw_rejected')
function onDrawRejected(msg) {
    alert("Đối thủ đã từ chối cầu hòa.");
    // Kích hoạt lại nút cầu hòa của mình (nếu bị vô hiệu hóa)
    const drawRequestBtn = document.getElementById('drawRequestBtn');
    if(drawRequestBtn) drawRequestBtn.disabled = false;
}

registerHandler('player_info', onPlayerInfo);
registerHandler('room_info', onRoomCreatedOrJoined)
registerHandler('room_created', onRoomCreatedOrJoined);
registerHandler('room_joined', onRoomCreatedOrJoined);
registerHandler('game_start', onGameStart);
registerHandler('move_result', onMoveResult);
registerHandler('chat', onChat);
registerHandler('end_game', onEndGame);
registerHandler('error', onError);
registerHandler('draw_offer', onDrawOfferReceived);
registerHandler('draw_rejected', onDrawRejected);
registerHandler('player_left', onPlayerLeft);
registerHandler('valid_moves',onValidMovesReceived)
registerHandler('color', onColorAssigned); // Quan trọng cho rematch
registerHandler('rematch_offer', onRematchOffer);
registerHandler('rematch_unavailable', onRematchUnavailable);

//LOGIC RENDER VÀ INPUT

function updatePlayerBars() {
    const p1Bar = document.getElementById('player1Bar');
    const p2Bar = document.getElementById('player2Bar');
    if (!p1Bar || !p2Bar) return;

    // Hàm helper để cập nhật avatar
    const updateAvatar = (avatarEl, avatarUrl) => {
        if (!avatarEl) return;
        avatarEl.innerHTML = ''; // Xóa nội dung cũ
        if (avatarUrl && avatarUrl !== "null" && avatarUrl !== "") {
            const img = document.createElement('img');
            img.src = avatarUrl;
            img.alt = "Avatar";
            avatarEl.appendChild(img);
        } else {
            const icon = document.createElement('i');
            icon.className = "fa-solid fa-user";
            avatarEl.appendChild(icon);
        }
    };

    // Cập nhật Player 2 (Mình)
    const p2NameEl = p2Bar.querySelector('.player-name');
    const p2EloEl = p2Bar.querySelector('.player-elo');
    const p2Avatar = p2Bar.querySelector('.player-avatar');

    if (player2Info) {
        if (p2NameEl) p2NameEl.textContent = player2Info.name || playerName;
        if (p2EloEl) p2EloEl.textContent = `Elo: ${player2Info.elo || 1200}`;
        updateAvatar(p2Avatar, player2Info.avatar);
    } else {
        if (p2NameEl) p2NameEl.textContent = "Bạn";
        if (p2EloEl) p2EloEl.textContent = "Elo: 1200";
        updateAvatar(p2Avatar, null);
    }

    // Cập nhật Player 1 (Đối thủ)
    const p1NameEl = p1Bar.querySelector('.player-name');
    const p1EloEl = p1Bar.querySelector('.player-elo');
    const p1Avatar = p1Bar.querySelector('.player-avatar');

    if (player1Info) {
        if (p1NameEl) p1NameEl.textContent = player1Info.name || "Đối thủ";
        if (p1EloEl) p1EloEl.textContent = `Elo: ${player1Info.elo || 1200}`;
        updateAvatar(p1Avatar, player1Info.avatar);
    } else {
        if (p1NameEl) p1NameEl.textContent = "Đối thủ";
        if (p1EloEl) p1EloEl.textContent = "Elo: ???";
        updateAvatar(p1Avatar, null);
    }

    // Xóa quân cờ bị ăn cũ khi cập nhật (sẽ được thêm lại bởi updateCapturedPieces)
    const capturedAreas = document.querySelectorAll('.captured-pieces');
    capturedAreas.forEach(area => area.innerHTML = '');
}
function addChatMessage(sender, text) {
    // TÌM ELEMENT TRỰC TIẾP Ở ĐÂY
    const messagesContainer = document.getElementById('chatMessagesEl');

    // Kiểm tra xem có tìm thấy container không
    if (!messagesContainer) {
        console.error("Lỗi: Không tìm thấy khung chat (#chatMessagesEl)!");
        return; // Thoát nếu không tìm thấy
    }

    // Tạo phần tử tin nhắn
    const div = document.createElement('div');
    const strong = document.createElement('strong');
    strong.textContent = sender + ": ";
    div.appendChild(strong); // Tên người gửi in đậm
    div.appendChild(document.createTextNode(text)); // Nội dung tin nhắn

    // Thêm class nếu muốn phân biệt (tùy chọn)
    // if (sender === playerName) { div.classList.add('my-message'); }
    // else { div.classList.add('opponent-message'); }

    // Thêm tin nhắn vào container và cuộn xuống
    messagesContainer.appendChild(div);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}
function renderGame() {
    const gameData = decodeFEN(currentFEN);
    const boardArray = gameData.board;
    currentTurn = gameData.turn;

    updatePlayerBars();

    // Cập nhật trạng thái game trên Sidebar
    if (statusEl) statusEl.textContent = `Lượt: ${currentTurn === 'white' ? 'Trắng' : 'Đen'}${currentTurn === yourColor ? ' (Bạn)' : ''}`;
    if (roomInfoEl) roomInfoEl.textContent = roomId;
    if (colorInfoEl) colorInfoEl.textContent = yourColor;

    const state = { selected: selectedSquare, lastMove: lastMove, flipped: yourColor === 'black', currentTurn: currentTurn, isCheck: isKingInCheckState };

    if (window.renderChessBoard) {
        window.renderChessBoard(boardArray, state);
    }
}

// Hàm xử lý input từ chessboard_render.js (ĐÃ VIẾT ĐẦY ĐỦ LOGIC CHỌN/MOVE)
window.handleBoardInput = function(fromR, fromC, toR, toC) {
    const isClickMode = toR === undefined;

    if (!gameActive || currentTurn !== yourColor) {
        selectedSquare = null;
        renderGame();
        return; }

    if (isClickMode) {
        const piece = window.renderChessBoard.currentBoardState[fromR][fromC];
        if (selectedSquare && selectedSquare.r === fromR && selectedSquare.c === fromC) {
            selectedSquare = null; // Bỏ chọn
            window.validMoveSquares = [];
        } else if (piece && isPieceOurColor(piece)) {
            selectedSquare = { r: fromR, c: fromC }; // Chọn quân mới
            window.validMoveSquares = [];
            sendMessage({
                type: "get_valid_moves", // Loại tin nhắn mới
                square: window.coordToAlg(fromR, fromC), // Gửi ô cờ dạng "e2"
                roomId: roomId
            });
            renderGame();
        } else if (selectedSquare) {
            window.validMoveSquares = [];
            // Click vào ô đích khi đã có quân chọn -> MOVE
            const fromAlg = window.coordToAlg(selectedSquare.r, selectedSquare.c);
            const toAlg = window.coordToAlg(fromR, fromC);

            // KIỂM TRA PHONG CẤP
            const movingPiece = window.renderChessBoard.currentBoardState[selectedSquare.r][selectedSquare.c];
            if (checkPromotion(movingPiece, fromR)) { // fromR ở đây là hàng đích
                showPromotionPopup(fromAlg, toAlg);
            } else {
                window.sendMove(fromAlg, toAlg);
            }

            selectedSquare = null;
        } else {
            selectedSquare = null;
            window.validMoveSquares = [];
        }
    }
    // Xử lý Move (Drag/Drop hoặc Click-Click)
    else {
        window.validMoveSquares = [];
        const fromAlg = window.coordToAlg(fromR, fromC);
        const toAlg = window.coordToAlg(toR, toC);

        // KIỂM TRA PHONG CẤP
        const movingPiece = window.renderChessBoard.currentBoardState[fromR][fromC];
        if (checkPromotion(movingPiece, toR)) { // toR là hàng đích
            showPromotionPopup(fromAlg, toAlg);
        } else {
            window.sendMove(fromAlg, toAlg);
        }

        selectedSquare = null;
    }

    renderGame();
};

function checkPromotion(piece, toRow) {
    if (piece === 'P' && toRow === 0) return true;
    if (piece === 'p' && toRow === 7) return true;
    return false;
}

function showPromotionPopup(fromAlg, toAlg) {
    pendingPromotionMove = { from: fromAlg, to: toAlg };

    // Cập nhật hình ảnh quân cờ trong popup dựa trên màu
    const options = document.querySelectorAll('.promo-option img');
    options.forEach(img => {
        const pieceType = img.parentElement.dataset.piece; // q, r, b, n
        const fileName = yourColor === 'white' ? pieceType.toUpperCase() : 'b' + pieceType.toUpperCase();
        img.src = `../../PBL4_imgs/image/${fileName}.png`;
    });

    if (promotionPopupEl) promotionPopupEl.classList.remove('hidden');
}

function hidePromotionPopup() {
    if (promotionPopupEl) promotionPopupEl.classList.add('hidden');
    pendingPromotionMove = null;
}

// Gắn sự kiện cho các lựa chọn phong cấp
document.querySelectorAll('.promo-option').forEach(option => {
    option.addEventListener('click', () => {
        if (pendingPromotionMove) {
            const piece = option.dataset.piece; // q, r, b, n
            window.sendMove(pendingPromotionMove.from, pendingPromotionMove.to, piece);
            hidePromotionPopup();
        }
    });
});

if (cancelPromotionBtn) {
    cancelPromotionBtn.addEventListener('click', hidePromotionPopup);
}

function onValidMovesReceived(msg) {
    // Lưu lại danh sách ô đích hợp lệ
    window.validMoveSquares = msg.moves || [];
    // Gọi renderGame() để vẽ lại highlight cho các ô này
    renderGame();
}

// NHƯNG SỬA sendMove ĐỂ DÙNG sendMessage
window.sendMove = function(fromAlg, toAlg, promotionPiece = null) {
    if (roomId) {
        const msg = {
            type: 'move_request',
            from: fromAlg,
            to: toAlg,
            roomId: roomId
        };
        if (promotionPiece) {
            msg.promotion = promotionPiece;
        }
        sendMessage(msg);
    }
};

// Thêm các dòng này vào CUỐI CÙNG của file game_controller.js

window.createRoom = createRoom;
window.joinRoom = joinRoom;
window.findNewGame = findNewGame;
window.requestDraw = requestDraw;
window.resignGame = resignGame;
window.sendChat = sendChat;
window.leaveRoom = leaveRoom;
window.requestReMatch = requestRematch;

if (true) {
    renderGame();
} else {
    // Nếu Chess_board.js tải chậm hơn, đợi nó
    window.addEventListener('boardLoaded', () => renderGame());
}
