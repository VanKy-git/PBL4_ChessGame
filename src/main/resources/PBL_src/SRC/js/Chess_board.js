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

function renderBoard() {
  boardElement.innerHTML = ''; 

  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      const square = document.createElement("div");
      square.classList.add("square");
      square.dataset.row = row;   
      square.dataset.col = col;
      if ((row + col) % 2 === 0) {
        square.classList.add("white");
      } else {
        square.classList.add("black");
      }
      const piece = initialBoard[row][col];
      if (piece) {
        const img = document.createElement("img");
        img.src = `../../PBL4_imgs/image/${piece}.png`;      
        img.alt = piece;
        img.draggable = true; 
        square.appendChild(img);

        img.addEventListener("click", () => handlePieceClick(row, col, piece, img));
      }

      boardElement.appendChild(square);
    }
  }
}

let selectedPosition = null;
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

  if (selectedPosition.row === row && selectedPosition.col === col) {
    square.classList.remove("highlight");
    selectedPosition = null;
    return;
  }
  const { row: fromRow, col: fromCol } = selectedPosition;
  const movingPiece = initialBoard[fromRow][fromCol];

  initialBoard[row][col] = movingPiece;
  initialBoard[fromRow][fromCol] = null;

  selectedPosition = null;
  renderBoard();
});


let draggedPiece = null;
boardElement.addEventListener("dragstart", (e) => {
  if (e.target.tagName === "IMG") {
    draggedPiece = e.target;
    setTimeout(() => {
      e.target.style.display = "none";
    }, 0);
  }
});
boardElement.addEventListener("dragend", (e) => {
  if (draggedPiece) {
    draggedPiece.style.display = "block";
    draggedPiece = null;
  }
});
boardElement.addEventListener("dragover", (e) => {
  e.preventDefault();
});
boardElement.addEventListener("drop", (e) => {
  e.preventDefault();
  if (e.target.classList.contains("square") && draggedPiece) {
    if (e.target.children.length === 0 || e.target.children[0].tagName === "IMG") {
      e.target.appendChild(draggedPiece);
    }
  }
});

renderBoard();
