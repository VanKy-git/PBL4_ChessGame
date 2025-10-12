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

const boardElement = document.getElementById("chessBoard");

// ==========================
// THIẾT LẬP TRẠNG THÁI GAME
// ==========================
let selected = null;
let highlightedSquares = [];
let currentTurn = 'w'; // Lượt đi hiện tại (Trắng bắt đầu)

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

            const piece = initialBoard[row][col];
            if (piece) {
                const img = document.createElement("img");
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

function highlightValidMoves(row, col, piece) {
    const moves = getValidMoves(row, col, piece); 
    moves.forEach(([r, c, isCapture]) => {
        const square = document.querySelector(`[data-row="${r}"][data-col="${c}"]`);
        if (square) {
            square.classList.add("valid-move");
            if (isCapture) {
                square.classList.add("capture-move"); 
            }
            highlightedSquares.push(square);
        }
    });
}

function movePiece(fromRow, fromCol, toRow, toCol) {
    const piece = initialBoard[fromRow][fromCol];
    initialBoard[toRow][toCol] = piece;
    initialBoard[fromRow][fromCol] = null;
    currentTurn = currentTurn === 'w' ? 'b' : 'w';
    renderBoard();
}

boardElement.addEventListener("click", (e) => {
    const square = e.target.closest(".square");
    if (!square) return;

    const row = parseInt(square.dataset.row);
    const col = parseInt(square.dataset.col);
    const piece = initialBoard[row][col];

    if (selected && selected.row === row && selected.col === col) {
        clearHighlights();
        selected = null;
        return;
    }
    if (piece && piece[0] === currentTurn) { 
        clearHighlights();
        selected = { row, col, piece };
        highlightValidMoves(row, col, piece);
        square.classList.add("highlight");
    } 
    else if (selected && square.classList.contains("valid-move")) {
        movePiece(selected.row, selected.col, row, col);
        clearHighlights();
        selected = null;
    }
    else {
        clearHighlights();
        selected = null;
    }
});

// ==========================
// LOGIC DI CHUYỂN CƠ BẢN
// ==========================
function getValidMoves(row, col, piece) {
    // moves: Mảng các [row, col, isCapture (boolean)]
    const moves = []; 
    const color = piece[0];
    const type = piece[1];

    const directions = {
        N: [-1, 0], S: [1, 0], E: [0, 1], W: [0, -1],
        NE: [-1, 1], NW: [-1, -1], SE: [1, 1], SW: [1, -1]
    };

    // Hàm tiện ích cho các quân đi đường dài (Xe, Tượng, Hậu)
    function addDir(dr, dc, repeat = true) {
        let r = row + dr, c = col + dc;
        while (r >= 0 && r < 8 && c >= 0 && c < 8) {
            const target = initialBoard[r][c];
            if (target) {
                // Gặp quân
                if (target[0] !== color) moves.push([r, c, true]); // Ăn quân đối thủ
                break; // Dừng lại khi gặp quân đầu tiên
            }
            moves.push([r, c, false]); // Ô trống
            if (!repeat) break; // Chỉ đi 1 bước (cho Vua)
            r += dr; c += dc;
        }
    }

    switch (type) {
        case "P": // Tốt 
            const dir = color === "w" ? -1 : 1; 
            const oneStepRow = row + dir;

            // 1. Di chuyển 1 ô thẳng
            if (oneStepRow >= 0 && oneStepRow < 8 && !initialBoard[oneStepRow][col]) {
                moves.push([oneStepRow, col, false]);

                // 2. Di chuyển 2 ô 
                const twoStepRow = row + dir * 2;
                const initialRow = color === "w" ? 6 : 1;
                if (row === initialRow && twoStepRow >= 0 && twoStepRow < 8 && !initialBoard[twoStepRow][col]) {
                    moves.push([twoStepRow, col, false]);
                }
            }

            // 3. Ăn chéo
            for (const dc of [-1, 1]) {
                const targetRow = row + dir;
                const targetCol = col + dc;
                if (targetRow >= 0 && targetRow < 8 && targetCol >= 0 && targetCol < 8) {
                    const target = initialBoard[targetRow][targetCol];
                    if (target && target[0] !== color) moves.push([targetRow, targetCol, true]); 
                }
            }
            break;

        case "R": // Xe
            addDir(directions.N[0], directions.N[1]); addDir(directions.S[0], directions.S[1]);
            addDir(directions.E[0], directions.E[1]); addDir(directions.W[0], directions.W[1]);
            break;
            
        case "B": // Tượng
            addDir(directions.NE[0], directions.NE[1]); addDir(directions.NW[0], directions.NW[1]);
            addDir(directions.SE[0], directions.SE[1]); addDir(directions.SW[0], directions.SW[1]);
            break;
            
        case "Q": // Hậu
            Object.values(directions).forEach(([dr, dc]) => addDir(dr, dc));
            break;
            
        case "N": // Mã
            const knightMoves = [
                [-2, -1], [-2, 1], [2, -1], [2, 1],
                [-1, -2], [-1, 2], [1, -2], [1, 2]
            ];
            knightMoves.forEach(([dr, dc]) => {
                const r = row + dr, c = col + dc;
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    const target = initialBoard[r][c];
                    const isCapture = target && target[0] !== color;
                    if (!target || isCapture) moves.push([r, c, isCapture]);
                }
            });
            break;
            
        case "K": // Vua
            Object.values(directions).forEach(([dr, dc]) => {
                const r = row + dr, c = col + dc;
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    const target = initialBoard[r][c];
                    const isCapture = target && target[0] !== color;
                    if (!target || isCapture) moves.push([r, c, isCapture]);
                }
            });
            break;
    }

    return moves;
}

// ==========================
// DRAG & DROP (Tùy chọn - Đã sửa lỗi và tích hợp logic lượt đi/highlight)
// ==========================
let draggedPiece = null;
let fromRow, fromCol;

boardElement.addEventListener("dragstart", (e) => {
    if (e.target.tagName === "IMG") {
        draggedPiece = e.target;
        
        // Kiểm tra xem quân cờ có thuộc lượt đi hiện tại không
        if (draggedPiece.alt[0] !== currentTurn) {
            draggedPiece = null;
            e.preventDefault();
            return;
        }

        const parentSquare = draggedPiece.parentElement;
        fromRow = parseInt(parentSquare.dataset.row);
        fromCol = parseInt(parentSquare.dataset.col);
        
        // Highlight nước đi hợp lệ khi bắt đầu kéo
        clearHighlights();
        selected = { row: fromRow, col: fromCol, piece: draggedPiece.alt };
        highlightValidMoves(fromRow, fromCol, draggedPiece.alt);
        parentSquare.classList.add("highlight");

        setTimeout(() => { e.target.style.display = "none"; }, 0);
    }
});

boardElement.addEventListener("dragend", (e) => {
    if (draggedPiece) {
        draggedPiece.style.display = "block";
        draggedPiece = null;
        clearHighlights(); 
        selected = null;
    }
});

boardElement.addEventListener("dragover", (e) => e.preventDefault());

boardElement.addEventListener("drop", (e) => {
    e.preventDefault();
    const square = e.target.closest(".square");
    if (!square || !draggedPiece) return;

    const toRow = parseInt(square.dataset.row);
    const toCol = parseInt(square.dataset.col);

    // Kiểm tra nước đi có hợp lệ (đã được highlight)
    if (square.classList.contains("valid-move")) {
        movePiece(fromRow, fromCol, toRow, toCol);
    } 
    
    // reset trạng thái
    draggedPiece.style.display = "block";
    draggedPiece = null;
    clearHighlights();
    selected = null;
});

// ==========================
// KHỞI TẠO GAME
// ==========================
renderBoard();