// Bàn cờ 8x8, mỗi phần tử là quân cờ hoặc null
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

for (let row = 0; row < 8; row++) {
  for (let col = 0; col < 8; col++) {
    const square = document.createElement("div");
    square.classList.add("square");

    // Xen kẽ màu trắng/đen
    if ((row + col) % 2 === 0) {
      square.classList.add("white");
    } else {
      square.classList.add("black");
    }

    // Nếu có quân thì thêm ảnh
    const piece = initialBoard[row][col];
    if (piece) {
      const img = document.createElement("img");
      console.log(piece);
      img.src = `../../PBL4_imgs/image/${piece}.png`;      
      img.alt = piece;
      square.appendChild(img);
    }

    boardElement.appendChild(square);
  }
}

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
const images = boardElement.querySelectorAll("img");
images.forEach(img => {
  img.setAttribute("draggable", true);
});

