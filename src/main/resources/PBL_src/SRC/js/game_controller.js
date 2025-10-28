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


// ==========================
// 3. QUẢN LÝ VIEW VÀ VIEW ACTIONS
// ==========================

// ❌ XÓA: Toàn bộ các hàm showModesView, showLobbyView, showGameControlsView
// Sẽ được xử lý bởi Home_page.js

// ==========================
// 4. WEBSOCKET VÀ MESSAGE HANDLING
// ==========================


function createRoom() {
    console.log("createRoom pressed");
    sendMessage({ type: "create_room" });
}
function joinRoom() {
    const id = document.getElementById('joinRoomIdInput').value.trim();
    if (id) sendMessage({ type: "join_room", roomId: id });
}
function findNewGame() {
    // ✅ SỬA: Dùng sendMessage và các biến toàn cục
    gameActive = false;
    currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    renderGame();
    sendMessage({type: "join", playerName: playerName});
    // ✅ SỬA: Cập nhật UI
}
function requestDraw() { if (gameActive) sendMessage({ type: "draw_request", roomId: roomId }); }
function resignGame() { if (gameActive) sendMessage({ type: "resign", roomId: roomId }); }

function sendChat() {
    // ✅ TÌM ELEMENT TRỰC TIẾP Ở ĐÂY
    const inputElement = document.getElementById('chatInputEl');

    // ✅ Kiểm tra xem có tìm thấy element không
    if (!inputElement) {
        console.error("Lỗi: Không tìm thấy ô nhập chat (#chatInputEl)!");
        return; // Thoát nếu không tìm thấy
    }

    // ✅ Sử dụng element vừa tìm được
    const text = inputElement.value.trim();
    if (text && roomId) {
        sendMessage({ type: "chat", roomId: roomId, message: text });
        inputElement.value = ''; // Xóa nội dung trong element vừa tìm
    }
}

function leaveRoom() {
    sendMessage({ type: "leave_room", roomId: roomId });
}

function onPlayerLeft(msg)
{
    onEndGame(msg);
}

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

// ==========================
// 5. CÁC HÀM XỬ LÝ TIN NHẮN (HANDLER)
// ==========================

// ❌ XÓA: export function handleMessage(msg) { ... }
// Thay vào đó, chúng ta tạo các hàm handler riêng lẻ

function onPlayerInfo(msg) {
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
    // ✅ GỌI HÀM NÀY TỪ Home_page.js (sẽ tạo ở bước 3)
    window.showGameControlsView();
}

function onGameStart(msg) {
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

    if (msg.currentTurn) {
        currentTurn = msg.currentTurn;
    }
    if (window.hideMatchmakingPopup) window.hideMatchmakingPopup();
    // Gọi renderGame() trực tiếp
    updateStatus();
    renderGame();
}

function onMoveResult(msg) {
    console.log("Move Result message received.");
    if (msg.fen && msg.LMfrom && window.algToCoord) { // Kiểm tra cả lastMove

        // 1. Lấy tọa độ nước đi cuối
        const fromCoord = window.algToCoord(msg.LMfrom);
        const toCoord = window.algToCoord(msg.LMto);

        // 2. Lấy bàn cờ TRƯỚC KHI cập nhật FEN
        const oldGameData = decodeFEN(currentFEN);
        const oldBoardArray = oldGameData.board;
        const turnBeforeMove = oldGameData.turn; // Lượt đi trước đó (chính là màu quân vừa đi)

        // 3. Lấy ký tự quân cờ TỪ BÀN CỜ CŨ
        let movedPieceChar = '';
        if (fromCoord && oldBoardArray[fromCoord.r] && oldBoardArray[fromCoord.r][fromCoord.c]) {
            movedPieceChar = oldBoardArray[fromCoord.r][fromCoord.c];
        } else {
            console.warn("Không thể lấy ký tự quân cờ từ tọa độ 'from'");
        }

        // 4. Cập nhật trạng thái mới
        currentFEN = msg.fen;
        const newGameData = decodeFEN(currentFEN);
        currentTurn = newGameData.turn; // Cập nhật lượt đi MỚI

        lastMove = { from: fromCoord, to: toCoord }; // Cập nhật lastMove (dạng {r, c})

        // 5. ✅ GỌI HÀM VỚI ĐỦ THÔNG TIN (bao gồm pieceChar)
        addMoveToHistory(msg.LMfrom, msg.LMto, turnBeforeMove, movedPieceChar);

        // 6. Render và cập nhật status
        renderGame();
        updateStatus();

    } else {
        console.warn("Received move_result without FEN or lastMove:", msg);
        // Xử lý nước đi không hợp lệ (nếu cần)
        if (msg.result === false) {
            alert("Nước đi không hợp lệ: " + (msg.message || ''));
        }
    }
}

function onChat(msg) {
    addChatMessage(msg.playerName, msg.message);
}

function onEndGame(msg) {
    gameActive = false; // Dừng game
    // stopTimer(); // Dừng đồng hồ

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
    } else {
        // Fallback nếu hàm chưa sẵn sàng
        alert(`Kết quả: ${result} - Lý do: ${reason || 'Kết thúc trận'}`);
    }
}

function requestRematch() {
    if (roomId) {
        sendMessage({ type: "rematch_request", roomId: roomId });
        // Client có thể hiển thị "Đang chờ đối thủ chấp nhận..."
    }
}

function addMoveToHistory(fromAlg, toAlg, movedColor, pieceChar) {
    const moveListEl = document.getElementById('moveList');
    if (!moveListEl) return;

    const pieceIcon = PIECES[pieceChar] || ''; // Lấy icon, nếu không có thì để trống

    // Tạo text (Icon + Nước đi)
    const moveText = `${pieceIcon}${fromAlg}->${toAlg}`;

    let listItem;

    if (isWhiteMove) {
        // Bắt đầu một hàng mới cho nước đi của Trắng
        listItem = document.createElement('li');
        listItem.textContent = `${moveText}   `;
        moveListEl.appendChild(listItem);
    } else {
        // Thêm nước đi của Đen vào hàng hiện tại
        listItem = moveListEl.lastElementChild; // Lấy <li> cuối cùng
        if (listItem) {
            listItem.textContent += ` ${moveText}`; // Thêm vào sau nước đi của Trắng
        }
    }

    isWhiteMove = !isWhiteMove; // Đảo lượt cho lần thêm tiếp theo

    // Tự động cuộn xuống dưới
    moveListEl.scrollTop = moveListEl.scrollHeight;
}

function onError(msg) {
    alert('Lỗi: ' + msg.message);
}

function updateStatus() {
    // Tìm element nếu chưa có (làm 1 lần)
    if (!statusEl) statusEl = document.getElementById('gameStatus');
    if (!roomInfoEl) roomInfoEl = document.getElementById('roomInfoEl');
    if (!colorInfoEl) colorInfoEl = document.getElementById('colorInfoEl');

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

// ✅ ĐĂNG KÝ TẤT CẢ HANDLER KHI FILE NÀY TẢI LÊN
registerHandler('player_info', onPlayerInfo);
registerHandler('room_info', onRoomCreatedOrJoined)
registerHandler('room_created', onRoomCreatedOrJoined);
registerHandler('room_joined', onRoomCreatedOrJoined);
registerHandler('game_start', onGameStart);
registerHandler('move_result', onMoveResult);
registerHandler('chat', onChat);
registerHandler('end_game', onEndGame);
registerHandler('error', onError);
registerHandler('color', onRoomCreatedOrJoined);
registerHandler('draw_offer', onDrawOfferReceived);
registerHandler('draw_rejected', onDrawRejected);
registerHandler('player_left', onPlayerLeft);


// ==========================
// 6. LOGIC RENDER VÀ INPUT
// ==========================

function updatePlayerBars() {
    // const p1Bar = document.getElementById('player1Bar');
    // const p2Bar = document.getElementById('player2Bar');
    // if (!p1Bar || !p2Bar) return;
    //
    // // --- Cập nhật Player 2 (Mình) ---
    // if (player2Info) {
    //     p2Bar.querySelector('.player-name').textContent = player2Info.name;
    //     p2Bar.querySelector('.player-elo').textContent = player2Info.elo || '1200';
    //     p2Bar.classList.remove('hidden-player');
    // } else {
    //     p2Bar.classList.add('hidden-player');
    // }
    //
    // // --- Cập nhật Player 1 (Đối thủ) ---
    // if (player1Info) {
    //     p1Bar.querySelector('.player-name').textContent = player1Info.name;
    //     p1Bar.querySelector('.player-elo').textContent = player1Info.elo || '1200';
    //     p1Bar.classList.remove('hidden-player');
    // } else {
    //     p1Bar.classList.add('hidden-player');
    // }
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

    const state = { selected: selectedSquare, lastMove: lastMove, flipped: yourColor === 'black', currentTurn: currentTurn };

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
        } else if (piece && isPieceOurColor(piece)) {
            selectedSquare = { r: fromR, c: fromC }; // Chọn quân mới
        } else if (selectedSquare) {
            // Click vào ô đích khi đã có quân chọn -> MOVE
            const fromAlg = window.coordToAlg(selectedSquare.r, selectedSquare.c);
            const toAlg = window.coordToAlg(fromR, fromC);
            window.sendMove(fromAlg, toAlg);
            selectedSquare = null;
        } else {
            selectedSquare = null;
        }
    }
    // Xử lý Move (Drag/Drop hoặc Click-Click)
    else {
        const fromAlg = window.coordToAlg(fromR, fromC);
        const toAlg = window.coordToAlg(toR, toC);
        window.sendMove(fromAlg, toAlg);
        selectedSquare = null;
    }

    renderGame();
};

// NHƯNG SỬA sendMove ĐỂ DÙNG sendMessage
window.sendMove = function(fromAlg, toAlg) {
    if (roomId) {
        sendMessage({
            type: 'move_request',
            from: fromAlg,
            to: toAlg,
            roomId: roomId
        });
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
