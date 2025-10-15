// chess_board.js
// Handles rendering board, theme, click & drag/drop interactions.
// Works in BOTH offline mode and online mode (if a global sendMove(fromAlg, toAlg) exists).

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

// THEMES
const themes = [
  { white: "#f0d9b5", black: "#b58863" }, // mặc định
  { white: "#e0f7fa", black: "#006064" },
  { white: "#f3e5f5", black: "#6a1b9a" },
  { white: "#eeeeee", black: "#424242" },
  { white: "#fff3e0", black: "#e65100" },
  { white: "#e3f2fd", black: "#1565c0" },
  { white: "#f1f8e9", black: "#558b2f" },
  { white: "#fafafa", black: "#37474f" },
  { white: "#fff9c4", black: "#f57f17" },
  { white: "#fce4ec", black: "#880e4f" },
  { white: "#e0f2f1", black: "#00695c" },
  { white: "#fff8e1", black: "#ff6f00" },
  { white: "#ede7f6", black: "#4527a0" },
  { white: "#efebe9", black: "#5d4037" },
  { white: "#eceff1", black: "#263238" }
];

let currentTheme = 0;

function applyTheme(index) {
  const theme = themes[index];
  document.documentElement.style.setProperty("--white-square", theme.white);
  document.documentElement.style.setProperty("--black-square", theme.black);
}

function prevTheme() {
  currentTheme = (currentTheme - 1 + themes.length) % themes.length;
  applyTheme(currentTheme);
}

function nextTheme() {
  currentTheme = (currentTheme + 1) % themes.length;
  applyTheme(currentTheme);
}

applyTheme(currentTheme);

// Helpers: convert between array coords (row 0..7, col 0..7) and algebraic "a1".."h8"
function coordToAlg(r, c) {
  const file = String.fromCharCode('a'.charCodeAt(0) + c);
  const rank = 8 - r;
  return file + rank; // e.g. {r:7,c:4} -> "e1"
}
function algToCoord(alg) {
  if (!alg || alg.length !== 2) return null;
  const c = alg.charCodeAt(0) - 'a'.charCodeAt(0);
  const r = 8 - parseInt(alg.charAt(1), 10);
  return { r, c };
}

// Render board UI
function renderBoard() {
  boardElement.innerHTML = '';

  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      const square = document.createElement("div");
      square.classList.add("square");
      square.dataset.row = row;
      square.dataset.col = col;

      // color classes use CSS variables defined in Match.css/Chess_board.css
      if ((row + col) % 2 === 0) square.classList.add("white");
      else square.classList.add("black");

      const piece = initialBoard[row][col];
      if (piece) {
        const img = document.createElement("img");
        img.src = `../../PBL4_imgs/image/${piece}.png`;
        img.alt = piece;
        img.draggable = true;
        // Add small dataset to image for convenience
        img.dataset.piece = piece;
        square.appendChild(img);
      }

      boardElement.appendChild(square);
    }
  }
}

// selection & drag state
let selectedPosition = null;
let draggedPiece = null;

// Click handler (supports selection -> move). For online: will call global sendMove(fromAlg,toAlg) if present.
// If sendMove exists and we're in online mode (roomId set), do NOT apply locally until server confirms.
boardElement.addEventListener("click", (e) => {
  const square = e.target.closest(".square");
  if (!square) return;
  const row = parseInt(square.dataset.row);
  const col = parseInt(square.dataset.col);
  const piece = initialBoard[row][col];

  if (!selectedPosition) {
    if (piece) {
      selectedPosition = { row, col };
      square.classList.add("highlight");
    }
    return;
  }

  // if clicked same square - deselect
  if (selectedPosition.row === row && selectedPosition.col === col) {
    square.classList.remove("highlight");
    selectedPosition = null;
    return;
  }

  // perform move: either send to server or move locally
  const fromRow = selectedPosition.row;
  const fromCol = selectedPosition.col;
  const toRow = row;
  const toCol = col;

  const fromAlg = coordToAlg(fromRow, fromCol);
  const toAlg = coordToAlg(toRow, toCol);

  // Clean highlight immediately
  clearHighlights();

  if (typeof window.roomId !== "undefined" && window.roomId !== null) {
    // online mode: send to server via global sendMove(fromAlg,toAlg)
    if (typeof window.sendMove === "function") {
      window.sendMove(fromAlg, toAlg);
    } else {
      console.warn("sendMove() not found — cannot send online move");
    }
  } else {
    // offline/local mode — update board immediately
    const movingPiece = initialBoard[fromRow][fromCol];
    initialBoard[toRow][toCol] = movingPiece;
    initialBoard[fromRow][fromCol] = null;
    renderBoard();
  }

  selectedPosition = null;
});

// DRAG & DROP
boardElement.addEventListener("dragstart", (e) => {
  if (e.target.tagName === "IMG") {
    draggedPiece = e.target;
    // hide dragged image (visual)
    setTimeout(() => { if (e.target) e.target.style.display = "none"; }, 0);
  }
});
boardElement.addEventListener("dragend", (e) => {
  if (draggedPiece) {
    draggedPiece.style.display = "block";
    draggedPiece = null;
  }
});
boardElement.addEventListener("dragover", (e) => e.preventDefault());
boardElement.addEventListener("drop", (e) => {
  e.preventDefault();
  const square = e.target.closest(".square");
  if (!square || !draggedPiece) return;

  const fromRow = parseInt(draggedPiece.parentElement.dataset.row);
  const fromCol = parseInt(draggedPiece.parentElement.dataset.col);
  const toRow = parseInt(square.dataset.row);
  const toCol = parseInt(square.dataset.col);

  const fromAlg = coordToAlg(fromRow, fromCol);
  const toAlg = coordToAlg(toRow, toCol);

  // restore image visibility
  draggedPiece.style.display = "block";
  draggedPiece = null;

  // online: send move request and wait server; offline: apply immediately
  if (typeof window.roomId !== "undefined" && window.roomId !== null) {
    if (typeof window.sendMove === "function") {
      window.sendMove(fromAlg, toAlg);
    } else {
      console.warn("sendMove() not found — cannot send online move");
    }
  } else {
    const movingPiece = initialBoard[fromRow][fromCol];
    initialBoard[toRow][toCol] = movingPiece;
    initialBoard[fromRow][fromCol] = null;
    renderBoard();
  }
});

// helper to clear highlights
function clearHighlights() {
  boardElement.querySelectorAll(".highlight").forEach(el => el.classList.remove("highlight"));
}

// Utility: apply a move given algebraic strings (fromAlg, toAlg)
// This will update initialBoard and re-render. Use when server confirms move.
function applyMoveFromAlgebraic(fromAlg, toAlg, pieceOverride = null) {
  const from = algToCoord(fromAlg);
  const to = algToCoord(toAlg);
  if (!from || !to) return;
  const movingPiece = pieceOverride || initialBoard[from.r][from.c];
  initialBoard[to.r][to.c] = movingPiece;
  initialBoard[from.r][from.c] = null;
  clearHighlights();
  renderBoard();
}

// algToCoord (already used)
function algToCoord(alg) {
  if (!alg || alg.length !== 2) return null;
  const c = alg.charCodeAt(0) - 'a'.charCodeAt(0);
  const r = 8 - parseInt(alg.charAt(1), 10);
  return { r, c };
}

// initial render
renderBoard();

// Expose some functions to global so match.js can call them if needed
window.applyMoveFromAlgebraic = applyMoveFromAlgebraic;
window.coordToAlg = coordToAlg;
window.algToCoord = algToCoord;
window.renderBoard = renderBoard;
