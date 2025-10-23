// File: chessboard_render.js
// Nhiệm vụ: Chỉ hiển thị bàn cờ (rendering) và chuyển giao input (clicks/drops).

// ==========================
// THIẾT LẬP & HẰNG SỐ
// ==========================

const boardElement = document.getElementById("chessBoard");
let currentBoardState = null; // Lưu trữ mảng 8x8 cuối cùng được render

// Các themes ĐÃ ĐƯỢC ĐIỀN ĐẦY ĐỦ
const themes = [ 
    { white: "#f0d9b5", black: "#b58863" }, // Mặc định
    { white: "#e0f7fa", black: "#006064" }, // Xanh nhạt/Xanh đậm
    { white: "#f3e5f5", black: "#6a1b9a" }, // Tím nhạt/Tím đậm
    { white: "#eeeeee", black: "#424242" }, // Xám sáng/Xám tối
    { white: "#fff3e0", black: "#e65100" }, // Vàng nhạt/Cam
    { white: "#e3f2fd", black: "#1565c0" }, // Xanh dương nhạt/Xanh đậm
    { white: "#f1f8e9", black: "#558b2f" }, // Xanh lá nhạt/Xanh lá đậm
    { white: "#fafafa", black: "#37474f" }, // Trắng/Đen than
    { white: "#fff9c4", black: "#f57f17" }, // Vàng chanh/Cam vàng
    { white: "#fce4ec", black: "#880e4f" }, // Hồng nhạt/Đỏ tía
    { white: "#e0f2f1", black: "#00695c" }, // Xanh ngọc nhạt/Xanh ngọc đậm
    { white: "#fff8e1", black: "#ff6f00" }, // Trắng kem/Cam gắt
    { white: "#ede7f6", black: "#4527a0" }, // Tím Oải Hương/Tím đậm
    { white: "#efebe9", black: "#5d4037" }, // Nâu kem/Nâu đậm
    { white: "#eceff1", black: "#263238" }  // Bạc/Đen
]; 

// ===================== Helper functions (Exposed) =====================

/** Chuyển đổi tọa độ mảng (0-7) thành ký hiệu đại số (A1-H8) */
window.coordToAlg = function(r, c) {
    const file = String.fromCharCode('a'.charCodeAt(0) + c);
    const rank = 8 - r;
    return file + rank;
};

/** Chuyển đổi ký hiệu đại số thành tọa độ mảng */
window.algToCoord = function(alg) {
    if (!alg || alg.length !== 2) return null;
    const c = alg.charCodeAt(0) - 'a'.charCodeAt(0);
    const r = 8 - parseInt(alg.charAt(1), 10);
    return { r, c };
};

// ===================== Hàm Render Chính (API) =====================

window.renderChessBoard = function(boardArray, state) {
    if (!boardElement || !boardArray || !state) return;
    
    currentBoardState = boardArray;
    
    boardElement.innerHTML = '';
    const { selected, lastMove, flipped } = state;
    
    if (flipped) boardElement.classList.add('flipped');
    else boardElement.classList.remove('flipped');

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const rr = flipped ? 7 - r : r;
            const cc = flipped ? 7 - c : c;

            const square = document.createElement("div");
            square.classList.add("square");
            
            square.dataset.r = rr;
            square.dataset.c = cc;
            
            if ((r + c) % 2 === 0) square.classList.add("white");
            else square.classList.add("black");

            // HIGH LIGHTS
            if (selected && selected.r === rr && selected.c === cc) {
                square.classList.add('highlight');
            }
            if (lastMove && lastMove.from && lastMove.to &&
                ((lastMove.from.r === rr && lastMove.from.c === cc) ||
                 (lastMove.to.r === rr && lastMove.to.c === cc))) {
                square.classList.add('last-move');
            }

            // TRONG chessboard_render.js, bên trong hàm window.renderChessBoard, thay thế logic tạo IMG:

const piece = boardArray[rr][cc]; // Ví dụ: 'R' (Trắng) hoặc 'r' (Đen)

if (piece && piece !== '') {
    let fileName = '';

    if (piece === piece.toUpperCase()) {
        fileName = piece; 
        
    } else {fileName = 'b' + piece.toUpperCase();
    }

    const img = document.createElement("img");
    
    // Tạo đường dẫn cuối cùng: [R, K, bN, bR, bQ, etc.].png
    img.src = `../../PBL4_imgs/image/${fileName}.png`; 
    
    img.alt = piece;
    img.draggable = true;
    img.dataset.piece = piece;
    square.appendChild(img);
}

            // Gắn sự kiện click
            square.addEventListener('click', handleInputClick);
            
            boardElement.appendChild(square);
        }
    }
};

// ===================== Xử lý Input (Chuyển giao cho Controller) =====================

function handleInputClick(e) {
    const square = e.currentTarget;
    const r = parseInt(square.dataset.r);
    const c = parseInt(square.dataset.c);
    
    if (typeof window.handleBoardInput === "function") {
        window.handleBoardInput(r, c); 
    }
}

let dragStartPos = null;

boardElement.addEventListener("dragstart", (e) => {
    const img = e.target.closest('img');
    if (img) {
        const parentSquare = img.parentElement;
        dragStartPos = { r: parseInt(parentSquare.dataset.r), c: parseInt(parentSquare.dataset.c) };

        setTimeout(() => { img.style.opacity = 0; }, 0);
        
        if (typeof window.handleBoardInput === "function") {
             window.handleBoardInput(dragStartPos.r, dragStartPos.c); // Báo hiệu chọn quân
        }
    }
});

boardElement.addEventListener("dragend", (e) => {
    const img = e.target.closest('img');
    if (img) img.style.opacity = 1;
});

boardElement.addEventListener("dragover", (e) => e.preventDefault());

boardElement.addEventListener("drop", (e) => {
    e.preventDefault();
    const square = e.target.closest(".square");
    if (!square || !dragStartPos) return;

    const toRow = parseInt(square.dataset.r);
    const toCol = parseInt(square.dataset.c);
    const fromRow = dragStartPos.r;
    const fromCol = dragStartPos.c;

    if (typeof window.handleBoardInput === "function") {
        window.handleBoardInput(fromRow, fromCol, toRow, toCol); 
    }

    dragStartPos = null;
});


// ===================== EXPOSE API =====================
window.renderChessBoard = window.renderChessBoard;
// Các hàm coordToAlg và algToCoord đã được expose dưới dạng window.function