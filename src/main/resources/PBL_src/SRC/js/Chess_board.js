// File: chessboard_render.js
// Nhiệm vụ: Chỉ hiển thị bàn cờ (rendering) và chuyển giao input (clicks/drops).

// ==========================
// THIẾT LẬP & HẰNG SỐ
// ==========================

const boardElement = document.getElementById("chessBoard");

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

    window.renderChessBoard.currentBoardState = boardArray;
    
    boardElement.innerHTML = '';
    const { selected, lastMove, flipped, currentTurn, isCheck } = state;
    const currentValidMoves = window.validMoveSquares || [];
    if (flipped) boardElement.classList.add('flipped');
    else boardElement.classList.remove('flipped');

    for (let r = 0; r < 8; r++) {
        for (let c = 0; c < 8; c++) {
            const rr = flipped ? 7 - r : r;
            const cc = flipped ? 7 - c : c;
            const squareAlg = window.coordToAlg(rr, cc);
            const piece = boardArray[rr][cc];

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
            if (currentValidMoves.includes(squareAlg)) {
                const targetPiece = boardArray[rr][cc];
                if (targetPiece && targetPiece !== '') {
                    square.classList.add('capture-move');
                } else {
                    square.classList.add('valid-move');
                }
            }
            const isWhiteKing = piece === 'K';
            const isBlackKing = piece === 'k';
            if (isCheck &&
                ((currentTurn === 'white' && isWhiteKing) ||
                    (currentTurn === 'black' && isBlackKing)))
            {
                square.classList.add('in-check');
            }

            // Thêm tọa độ
            if (c === 0) {
                const rank = document.createElement('div');
                rank.classList.add('coordinate', 'rank');
                rank.textContent = flipped ? r + 1 : 8 - r;
                square.appendChild(rank);
            }
            if (r === 7) {
                const file = document.createElement('div');
                file.classList.add('coordinate', 'file');
                file.textContent = String.fromCharCode('a'.charCodeAt(0) + (flipped ? 7 - c : c));
                square.appendChild(file);
            }

            if (piece && piece !== '') {
                let fileName = '';
                if (piece === piece.toUpperCase()) {
                    fileName = piece; 
                } else {
                    fileName = 'b' + piece.toUpperCase();
                }
                const img = document.createElement("img");
                img.src = `../../PBL4_imgs/image/${fileName}.png`; 
                img.alt = piece;
                img.draggable = true;
                img.dataset.piece = piece;
                square.appendChild(img);
            }

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
        if (parentSquare && parentSquare.dataset.r !== undefined && parentSquare.dataset.c !== undefined) {
            dragStartPos = { r: parseInt(parentSquare.dataset.r), c: parseInt(parentSquare.dataset.c) };
            
            const clone = img.cloneNode(true);
            const computedStyle = window.getComputedStyle(img);
            clone.style.width = computedStyle.width;
            clone.style.height = computedStyle.height;
            clone.style.position = "absolute";
            clone.style.left = "-9999px";
            clone.style.opacity = 0.7;
            document.body.appendChild(clone);
            e.dataTransfer.setDragImage(clone, img.offsetWidth / 2, img.offsetHeight / 2);
            setTimeout(() => {
                if (clone.parentNode === document.body) {
                    document.body.removeChild(clone);
                }
            }, 0);
            setTimeout(() => { if(img) img.style.opacity = 0.5; }, 0);
        } else {
            dragStartPos = null;
            e.preventDefault();
        }
    } else {
        dragStartPos = null;
        e.preventDefault();
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
