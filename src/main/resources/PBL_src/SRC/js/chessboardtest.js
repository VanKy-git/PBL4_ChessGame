// CẤU HÌNH KẾT NỐI
const WS_URL = "ws://localhost:8080"; // Thay đổi nếu máy chủ của bạn ở địa chỉ khác

// ==========================
// TRẠNG THÁI GAME CỤC BỘ
// ==========================
const initialBoard = [
    ["bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"],
    ["bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"],
    [null, null, null, null, null, null, null, null],
    [null, null, null, null, null, null, null, null],
    [null, null, null, null, null, null, null, null],
    [null, null, null, null, null, null, null, null],
    ["wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"],
    ["wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"]
];
let boardState = initialBoard.map(row => [...row]); // Sao chép trạng thái ban đầu

let websocket = null;
let selected = null;
let highlightedSquares = [];
let currentTurn = 'w'; // Lượt đi hiện tại (Client chỉ hiển thị, Server kiểm soát)
let yourColor = 'w'; // Màu quân của người chơi hiện tại (cần được Server gửi)
let roomId = 'testroom'; // ID phòng (cần được Server gán)

// ==========================
// PHẦN TỬ GIAO DIỆN
// ==========================
const boardElement = document.getElementById("chessBoard");
// Thêm một element để hiển thị trạng thái và nút kết nối nếu cần thiết
// Ví dụ: const statusElement = document.getElementById("status");

// ==========================
// CÁC HÀM TIỆN ÍCH CHUYỂN ĐỔI TỌA ĐỘ
// Server sử dụng ký hiệu Đại số (e2, e4), Client sử dụng chỉ mục (row, col)
// ==========================

function coordToAlg(r, c) {
    const file = String.fromCharCode('a'.charCodeAt(0) + c);
    const rank = 8 - r;
    return file + rank;
}

function algToCoord(alg) {
    if (alg.length !== 2) return null;
    const c = alg.charCodeAt(0) - 'a'.charCodeAt(0);
    const r = 8 - parseInt(alg.charAt(1), 10);
    if (r < 0 || r > 7 || c < 0 || c > 7) return null;
    return { r, c };
}

// ==========================
// CÁC HÀM XỬ LÝ GIAO DIỆN (DOM)
// ==========================

function renderBoard() {
    boardElement.innerHTML = "";
    for (let row = 0; row < 8; row++) {
        for (let col = 0; col < 8; col++) {
            const square = document.createElement("div");
            square.classList.add("square");
            square.dataset.row = row;
            square.dataset.col = col;
            if ((row + col) % 2 === 0) square.classList.add("white");
            else square.classList.add("black");

            const piece = boardState[row][col];
            if (piece) {
                const img = document.createElement("img");
                // Giả định đường dẫn hình ảnh của bạn vẫn hoạt động
                img.src = `../../PBL4_imgs/image/${piece}.png`; 
                img.alt = piece;
                img.draggable = true;
                square.appendChild(img);
            }

            boardElement.appendChild(square);
        }
    }
}

function clearHighlights() {
    highlightedSquares.forEach(sq => sq.classList.remove("valid-move", "capture-move"));
    const currentSelectedSquare = document.querySelector(".highlight");
    if (currentSelectedSquare) {
        currentSelectedSquare.classList.remove("highlight");
    }
    highlightedSquares = [];
}

// KHÔNG CẦN HÀM highlightValidMoves VÌ CHÚNG TA KHÔNG XÁC THỰC LUẬT CỤC BỘ
// Tuy nhiên, để UX tốt hơn, ta sẽ giữ lại hàm highlight, nhưng chỉ dùng cho các ô đã được chọn
function highlightSquare(r, c) {
    const square = document.querySelector(`[data-row="${r}"][data-col="${c}"]`);
    if (square) {
        square.classList.add("highlight");
    }
}

/**
 * Thực hiện di chuyển trên UI (chỉ được gọi sau khi Server xác nhận hợp lệ)
 * @param {string} fromAlg - Vị trí bắt đầu (e2)
 * @param {string} toAlg - Vị trí kết thúc (e4)
 */
function applyMove(fromAlg, toAlg) {
    const fromCoord = algToCoord(fromAlg);
    const toCoord = algToCoord(toAlg);

    if (!fromCoord || !toCoord) return false;

    const piece = boardState[fromCoord.r][fromCoord.c];
    if (!piece) return false;

    // Cập nhật trạng thái bàn cờ cục bộ
    boardState[toCoord.r][toCoord.c] = piece;
    boardState[fromCoord.r][fromCoord.c] = null;
    
    // Vẽ lại giao diện
    renderBoard();
    clearHighlights();
    selected = null;
    return true;
}

// ==========================
// XỬ LÝ MẠNG VÀ TIN NHẮN TỪ SERVER
// ==========================

function connectWebSocket() {
    if (websocket && websocket.readyState === WebSocket.OPEN) return;

    websocket = new WebSocket(WS_URL);

    websocket.onopen = () => {
        console.log("Đã kết nối tới Server.");
        // Gửi thông tin kết nối và yêu cầu tham gia phòng
        websocket.send(JSON.stringify({
            type: "connect",
            playerName: "PlayerClient" // Bạn cần lấy tên người chơi thực tế
        }));
        // Tạm thời, giả định bạn luôn tham gia phòng 'testroom'
        // websocket.send(JSON.stringify({ type: "join_room", roomId: "testroom" })); 
        // statusElement.textContent = "Đã kết nối, đang chờ thông tin phòng...";
    };

    websocket.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        handleMessage(msg);
    };

    websocket.onclose = () => {
        console.log("Mất kết nối với Server.");
        // statusElement.textContent = "Mất kết nối.";
    };

    websocket.onerror = (error) => {
        console.error("WebSocket Error:", error);
    };
}

function handleMessage(msg) {
    switch (msg.type) {
        case 'player_info':
            // Nhận PlayerID, sẵn sàng tham gia phòng
            console.log("Player ID:", msg.playerId);
            // Gửi yêu cầu tham gia phòng ngay sau khi kết nối
            websocket.send(JSON.stringify({ type: "join_room", roomId: "testroom" })); 
            break;
            
        case 'color':
            yourColor = (msg.color === 'white' ? 'w' : 'b'); // Chuyển đổi màu từ Java sang ký hiệu Client
            console.log("Màu quân của bạn:", yourColor);
            break;

        case 'game_start':
            // Nhận trạng thái bàn cờ mới (nếu cần), lượt đi
            currentTurn = msg.currentTurn === 'white' ? 'w' : 'b'; // Đồng bộ lượt
            console.log("Game bắt đầu! Lượt:", currentTurn);
            break;

        case 'turn_change':
            currentTurn = msg.currentTurn === 'white' ? 'w' : 'b';
            // statusElement.textContent = `Lượt: ${currentTurn === 'w' ? 'Trắng' : 'Đen'}`;
            console.log("Đổi lượt. Lượt hiện tại:", currentTurn);
            break;

        case 'move_result':
            // Phản hồi từ Server về nước đi của chính Client
            if (msg.result === true) {
                // Server đã xác nhận, thực hiện di chuyển
                applyMove(msg.from, msg.to); 
                // Server sẽ gửi tiếp tin nhắn 'player_move' cho đối thủ
            } else {
                alert(`Nước đi không hợp lệ: ${msg.message}`);
                clearHighlights();
                selected = null;
                renderBoard(); // Đảm bảo UI khớp với trạng thái Server (nếu có lỗi)
            }
            break;

        case 'player_move':
            // Nhận nước đi của đối thủ (hoặc của chính mình)
            applyMove(msg.from, msg.to);
            break;

        case 'end_game':
            alert(`Trò chơi kết thúc! Người thắng: ${msg.winner}`);
            // Xử lý logic kết thúc game
            break;

        case 'error':
            alert('Lỗi từ Server: ' + msg.message);
            break;
            
        default:
            console.log('Tin nhắn không xác định:', msg);
    }
}

// ==========================
// LOGIC XỬ LÝ INPUT (Click & Drag/Drop)
// KHÔNG CHỨA LUẬT CHƠI, CHỈ GỬI YÊU CẦU ĐẾN SERVER
// ==========================

// **XÓA BỎ HOÀN TOÀN HÀM getValidMoves() VÀ movePiece() CŨ**

function sendMoveRequest(fromRow, fromCol, toRow, toCol) {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        const fromAlg = coordToAlg(fromRow, fromCol);
        const toAlg = coordToAlg(toRow, toCol);

        websocket.send(JSON.stringify({
            type: 'move_request',
            from: fromAlg,
            to: toAlg,
            roomId: roomId,
            color: yourColor === 'w' ? 'white' : 'black' // Gửi màu cho Server xác thực
        }));
    } else {
        alert("Chưa kết nối đến Server!");
    }
}

// ------------------------------------
// XỬ LÝ CLICK
// ------------------------------------
boardElement.addEventListener("click", (e) => {
    const square = e.target.closest(".square");
    if (!square || currentTurn !== yourColor) return; // Không phải lượt mình thì không làm gì

    const row = parseInt(square.dataset.row);
    const col = parseInt(square.dataset.col);
    const piece = boardState[row][col];

    // BƯỚC 1: Chọn quân
    if (piece && piece[0] === yourColor) { 
        clearHighlights();
        selected = { row, col, piece };
        highlightSquare(row, col);
    } 
    // BƯỚC 2: Di chuyển/Ăn quân sau khi đã chọn
    else if (selected) {
        // Thay vì kiểm tra luật, ta GỬI YÊU CẦU
        sendMoveRequest(selected.row, selected.col, row, col);
        
        // Tạm thời highlight ô đích để UX tốt hơn (Có thể xóa nếu muốn UX nghiêm ngặt hơn)
        highlightSquare(row, col); 
        // KHÔNG clearHighlights/selected ở đây. Chờ Server phản hồi!
    }
});

// ------------------------------------
// XỬ LÝ DRAG & DROP
// ------------------------------------
let draggedPiece = null;
let fromRow, fromCol;

boardElement.addEventListener("dragstart", (e) => {
    if (e.target.tagName === "IMG") {
        draggedPiece = e.target;
        
        // Kiểm tra lượt đi và màu quân
        if (draggedPiece.alt[0] !== yourColor || currentTurn !== yourColor) {
            draggedPiece = null;
            e.preventDefault();
            return;
        }

        const parentSquare = draggedPiece.parentElement;
        fromRow = parseInt(parentSquare.dataset.row);
        fromCol = parseInt(parentSquare.dataset.col);
        
        clearHighlights();
        selected = { row: fromRow, col: fromCol, piece: draggedPiece.alt };
        highlightSquare(fromRow, fromCol);

        setTimeout(() => { e.target.style.display = "none"; }, 0);
    }
});

boardElement.addEventListener("dragend", (e) => {
    // Luôn reset trạng thái Drag & Drop
    if (draggedPiece) {
        draggedPiece.style.display = "block";
    }
    draggedPiece = null;
    // Lưu ý: KHÔNG clearHighlights/selected ở đây. Chờ Server phản hồi.
});

boardElement.addEventListener("dragover", (e) => e.preventDefault());

boardElement.addEventListener("drop", (e) => {
    e.preventDefault();
    const square = e.target.closest(".square");
    if (!square || !draggedPiece) return;

    const toRow = parseInt(square.dataset.row);
    const toCol = parseInt(square.dataset.col);
    
    // GỬI YÊU CẦU (Không kiểm tra luật cục bộ)
    sendMoveRequest(fromRow, fromCol, toRow, toCol);
    
    // Tạm thời highlight ô đích
    highlightSquare(toRow, toCol);
});

// ==========================
// KHỞI TẠO GAME
// ==========================
// Gán sự kiện kết nối vào một nút
// document.getElementById("connectBtn").addEventListener('click', connectWebSocket);
connectWebSocket(); // Tự động kết nối khi tải trang
renderBoard();