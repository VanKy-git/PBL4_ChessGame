    // File: game_controller.js
    // Bộ điều khiển chính cho WebSocket, FEN, và Quản lý View trên Home_page.html

    // ==========================
    // 1. TRẠNG THÁI VÀ HẰNG SỐ
    // ==========================
    let socket = null;
    let connected = false;
    const SOCKET_URL = "ws://localhost:8080";

    // Trạng thái Game
    let currentFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"; 
    let playerId = localStorage.getItem("playerId");
    let playerName = localStorage.getItem("playerName") || "Guest";
    let yourColor = null; 
    let roomId = null;
    let gameActive = false;
    let currentTurn = 'white'; 

    // Trạng thái Giao diện và Input
    let selectedSquare = null; 
    let lastMove = null; 
    let flipped = false; 

    // Thông tin Người chơi
    let player1Info = null; // Đối thủ (Thanh trên)
    let player2Info = null; // Mình (Thanh dưới)

    // ==========================
    // 2. DOM ELEMENTS & HTML TEMPLATES
    // ==========================
    const rightPanel = document.querySelector('aside.right-panel');
    let initialModesHTML; 

    // Biến DOM cho Game Controls (Sẽ được gán lại khi showGameControlsView chạy)
    let statusEl, roomInfoEl, colorInfoEl, chatMessagesEl, chatInputEl, chatSendBtnEl;

    // Hàm Phân tích FEN (Đã được viết đầy đủ)
    function decodeFEN(fen) {
        const parts = fen.split(' ');
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
        return { board: boardArray, turn: turn };
    }

    // Hàm Trợ giúp: Kiểm tra màu quân
    function isPieceOurColor(piece) {
        if (!piece || !yourColor) return false;
        const pieceColor = piece === piece.toUpperCase() ? 'white' : 'black';
        return pieceColor === yourColor;
    }

    // HTML cho Lobby View (State 2) - BẠN PHẢI TỰ ĐỊNH NGHĨA CSS CHO CÁC CLASS NÀY
    function getLobbyHTML() {
        return `
        <div class="online-wrapper">
            <button id="backToModes" class="btn-back">←</button>
            <div style="font-weight:700; font-size:18px; text-align:center; margin-bottom:10px;">Chơi trực tuyến</div>
            <div class="muted" style="text-align:center; margin-bottom:20px;">Kết nối với đối thủ khác</div>
            
            <button id="createRoomBtn" class="btnn" disable>Tạo phòng</button>
            <input id="joinRoomIdInput" class="input" placeholder="Nhập mã phòng...">
            <button id="joinRoomBtn" class="btnn" disable>Tham gia phòng</button>
            <button id="matchmakingBtn" class="btnn" disable>Ghép trận ngẫu nhiên</button>
            
            <div id="lobbyStatus" class="status-lobby">Đang chờ kết nối...</div>
        </div>`;
    }

    // HTML cho Game Controls View (State 3) - BẠN PHẢI TỰ ĐỊNH NGHĨA CSS CHO CÁC CLASS NÀY
    function getGameControlsHTML() {
        return `
        <div class="game-controls-wrapper">
            <div class="status" id="gameStatus">Đang chờ đối thủ...</div>
            <div id="playerInfoBar">
                <div>Phòng: <span id="roomInfoEl">${roomId || '-'}</span></div>
                <div>Màu: <span id="colorInfoEl">${yourColor || '-'}</span></div>
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
                <button id="drawRequestBtn" class="btn-action">Cầu hòa</button>
                <button id="resignBtn" class="btn-action btn-warning">Đầu hàng</button>
                <button id="exitRoomBtn" class="btn-action btn-danger">Thoát phòng</button>
            </div>
        </div>`;
    }

    // Hàm GameOver Modal (Tùy chọn: cần HTML của Modal)
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

    function showModesView() {
        rightPanel.innerHTML = initialModesHTML;
        const onlineModeBtn = document.querySelector('[data-mode="online"]');
        if (onlineModeBtn) {
            onlineModeBtn.addEventListener('click', showLobbyView);
        }
        if (socket && socket.readyState === WebSocket.OPEN) socket.close();
        // Giữ lại playerID/Name, reset trạng thái game
        roomId = null; yourColor = null; gameActive = false; player1Info = null; selectedSquare = null;
        renderGame(); 
    }

    function showLobbyView() {
        if (!rightPanel) return;
        rightPanel.innerHTML = getLobbyHTML(); 
        
        // Gán Listeners cho Lobby
        document.getElementById('backToModes').addEventListener('click', showModesView);
        document.getElementById('createRoomBtn').addEventListener('click', createRoom);
        document.getElementById('joinRoomBtn').addEventListener('click', joinRoom);
        document.getElementById('matchmakingBtn').addEventListener('click', findMatchmaking);

        connectSocket(document.getElementById('lobbyStatus'));
        
        // Khởi tạo thông tin người chơi hiện tại (Player 2)
        player2Info = { id: playerId, name: playerName, elo: 1200 };
        updatePlayerBars(); 
    }

    function showGameControlsView() {
        rightPanel.innerHTML = getGameControlsHTML(); 

        // Khai báo lại DOM cho Game Controls (Quan trọng!)
        statusEl = document.getElementById('gameStatus');
        roomInfoEl = document.getElementById('roomInfoEl');
        colorInfoEl = document.getElementById('colorInfoEl');
        chatMessagesEl = document.getElementById('chatMessagesEl');
        chatInputEl = document.getElementById('chatInputEl');
        chatSendBtnEl = document.getElementById('chatSendBtnEl');
        
        // Gắn Listeners cho Game
        document.getElementById('exitRoomBtn').addEventListener('click', leaveRoom);
        document.getElementById('drawRequestBtn').addEventListener('click', requestDraw);
        document.getElementById('resignBtn').addEventListener('click', resignGame);

        if (chatSendBtnEl) chatSendBtnEl.addEventListener('click', sendChat);
        if (chatInputEl) chatInputEl.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendChat();
        });

        renderGame(); 
    }


    // ==========================
    // 4. WEBSOCKET VÀ MESSAGE HANDLING
    // ==========================

    function connectSocket(statusEl) {
        if (socket && socket.readyState === WebSocket.OPEN) return;

        if (statusEl) statusEl.textContent = "Đang kết nối...";
        socket = new WebSocket(SOCKET_URL);
        
        socket.onopen = () => {
            connected = true;
            if (statusEl) statusEl.textContent = "Đã kết nối.";
            socket.send(JSON.stringify({ type: "connect", playerName: playerName, playerId: playerId }));
        };

        socket.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            handleMessage(msg);
        };

        socket.onclose = () => {
            connected = false;
            if (statusEl) statusEl.textContent = "Mất kết nối!";
            if (gameActive) alert("Mất kết nối với Server! Game kết thúc.");
            showModesView();
        };
    }

    // Các hàm gửi yêu cầu Server
    function createRoom() {
        console.log("createRoom pressed, connected =", connected);
        if (socket && connected) socket.send(JSON.stringify({ type: "create_room" })); 
    }
    function joinRoom() { 
        const id = document.getElementById('joinRoomIdInput').value.trim();
        if (id && socket && connected) socket.send(JSON.stringify({ type: "join_room", roomId: id }));
    }
    function findMatchmaking() { if (socket && connected) socket.send(JSON.stringify({ type: "find_match" })); }
    function requestDraw() { if (gameActive && socket) socket.send(JSON.stringify({ type: "draw_request", roomId: roomId })); }
    function resignGame() { if (gameActive && socket) socket.send(JSON.stringify({ type: "resign", roomId: roomId })); }

    function sendChat() { 
        const text = chatInputEl.value.trim();
        if (text && socket && connected && roomId) {
            socket.send(JSON.stringify({ type: "chat", roomId: roomId, message: text }));
            chatInputEl.value = '';
            // Tùy chọn: Thêm tin nhắn của mình ngay lập tức vào khung chat
            addChatMessage(playerName, text); 
        }
    }

    function leaveRoom() { 
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: "leave_room", roomId: roomId }));
        }
        showModesView();
    }

    function enableLobbyButtons() {
        // Chỉ bật các nút nếu chúng đã được tải vào DOM (trong Lobby View)
        const createBtn = document.getElementById('createRoomBtn');
        const joinBtn = document.getElementById('joinRoomBtn');
        const matchBtn = document.getElementById('matchmakingBtn');
        const lobbyStatusEl = document.getElementById('lobbyStatus');
        
        if (createBtn) createBtn.disabled = false;
        if (joinBtn) joinBtn.disabled = false;
        if (matchBtn) matchBtn.disabled = false;
        
        if (lobbyStatusEl) lobbyStatusEl.textContent = "Đã kết nối, sẵn sàng tạo phòng.";
    }

    function handleMessage(msg) {
        switch (msg.type) {
            case 'player_info':
                playerId = msg.playerId;
                playerName = msg.playerName;
                localStorage.setItem("playerId", playerId);
                player2Info = { id: playerId, name: playerName, elo: msg.elo || 1200 };
                updatePlayerBars();
                enableLobbyButtons();
                break;
                
            case 'room_created':
            case 'room_joined':
                roomId = msg.roomId;
                yourColor = msg.color;
                showGameControlsView(); 
                break;

            case 'game_start':
                gameActive = true;
                // PHẦN BỊ COMMENT TRƯỚC ĐÓ: Logic lấy thông tin đối thủ từ Server
                if (msg.playerWhite && msg.playerBlack) { 
                    const whitePlayer = msg.playerWhite;
                    const blackPlayer = msg.playerBlack;
                    
                    if (whitePlayer.id === playerId) {
                        player2Info = whitePlayer; player1Info = blackPlayer; yourColor = 'white';
                    } else {
                        player2Info = blackPlayer; player1Info = whitePlayer; yourColor = 'black';
                    }
                }
                // Fall through
            case 'turn_update':
                currentFEN = msg.fen;
                if (msg.lastMove) lastMove = { from: window.algToCoord(msg.lastMove.from), to: window.algToCoord(msg.lastMove.to) };
                renderGame();
                break;
                
            case 'chat':
                addChatMessage(msg.playerName, msg.message);
                break;

            case 'end_game':
                const isWinner = msg.winnerId === playerId;
                const title = isWinner ? '🎉 Chiến thắng!' : ' Thất bại!';
                showGameOverModal(title, msg.reason || "Trận đấu kết thúc.");
                gameActive = false;
                break;
                
            case 'error': alert('Lỗi: ' + msg.message); break;
        }
    }

    // ==========================
    // 5. LOGIC RENDER VÀ INPUT
    // ==========================

    function updatePlayerBars() {
        const p1Bar = document.getElementById('player1Bar');
        const p2Bar = document.getElementById('player2Bar');
        if (!p1Bar || !p2Bar) return;
        
        // --- Cập nhật Player 2 (Mình) ---
        if (player2Info) {
            p2Bar.querySelector('.player-name').textContent = player2Info.name;
            p2Bar.querySelector('.player-elo').textContent = player2Info.elo || '1200';
            p2Bar.classList.remove('hidden-player');
        } else {
            p2Bar.classList.add('hidden-player');
        }
        
        // --- Cập nhật Player 1 (Đối thủ) ---
        if (player1Info) {
            p1Bar.querySelector('.player-name').textContent = player1Info.name;
            p1Bar.querySelector('.player-elo').textContent = player1Info.elo || '1200';
            p1Bar.classList.remove('hidden-player');
        } else {
            p1Bar.classList.add('hidden-player'); 
        }
    }

    function addChatMessage(sender, text) {
        if (chatMessagesEl) {
            const div = document.createElement('div');
            div.textContent = `[${sender}]: ${text}`;
            chatMessagesEl.appendChild(div);
            chatMessagesEl.scrollTop = chatMessagesEl.scrollHeight; // Cuộn xuống dưới
        }
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
        
        if (!gameActive || currentTurn !== yourColor) { selectedSquare = null; renderGame(); return; }
        
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

    window.sendMove = function(fromAlg, toAlg) {
        if (socket && socket.readyState === WebSocket.OPEN && roomId) {
            socket.send(JSON.stringify({
                type: 'move_request',
                from: fromAlg,
                to: toAlg,
                roomId: roomId
            }));
        }
    };

    // Khởi tạo ban đầu
    document.addEventListener('DOMContentLoaded', () => {
        const initialRightPanel = document.querySelector('aside.right');
        if (initialRightPanel) {
            window.initialModesHTML = initialRightPanel.innerHTML;
            const onlineModeBtn = document.querySelector('[data-mode="online"]');
            if (onlineModeBtn) {
                onlineModeBtn.addEventListener('click', showLobbyView);
            }
        }
        renderGame(); 
    });
