// File: History.js
const historyLink = document.getElementById('historyLink');
const historyPopup = document.getElementById('historyPopup');
const historyContainer = document.getElementById('historyContainer');
const historyClose = document.getElementById('historyClose');

const API_URL = "http://localhost:8910/api"; // Đảm bảo đúng địa chỉ server

function renderHistoryList(list) {
    if (!list || list.length === 0) {
        historyContainer.innerHTML = "<p>Chưa có trận đấu nào.</p>";
        return;
    }

    historyContainer.innerHTML = list.map(match => {
        // --- 1. LẤY DỮ LIỆU CẦN THIẾT ---
        
        // Trận đấu của bạn có các đối tượng player1 và player2
        const player1Name = match.player1 ? match.player1.userName : 'Unknown Player 1';
        const player2Name = match.player2 ? match.player2.userName : 'Unknown Player 2';
        
        const status = match.matchStatus; // Trạng thái: "waiting", "finished", v.v.
        
        // Giả sử trường thời gian là created_at nằm trong đối tượng player1 (hoặc tìm trường rõ ràng hơn)
        const matchTimeField = match.player1.createdAt || 'Invalid Date';
        const formattedDate = new Date(matchTimeField).toLocaleString();

        let winnerDisplay = '';
        let statusColor = '#fff';

        // --- 2. XÁC ĐỊNH NGƯỜI THẮNG DỰA TRÊN TRẠNG THÁI VÀ winnerId (Nếu có) ---
        
        if (status === 'finished' && match.winnerId) {
            // Logic này sẽ hoạt động khi Backend sửa lại và cung cấp winnerId
            const winnerId = match.winnerId;
            const winnerName = (winnerId === match.player1.user_id) ? player1Name : player2Name;
            
            winnerDisplay = `<span>🏆 Người thắng: <b>${winnerName}</b></span>`;
            
            // Xác định thắng/thua cho người dùng hiện tại
            if (winnerId === CURRENT_USER_ID) {
                statusColor = '#00ff00'; // Thắng
            } else {
                statusColor = '#ff0000'; // Thua
            }
        } else if (status === 'waiting') {
            winnerDisplay = `<span style="color:#ffff00;">Trạng thái: Đang chờ</span>`;
            statusColor = '#ffff00';
        } else {
             winnerDisplay = `<span>Trạng thái: ${status}</span>`;
        }

        // --- 3. RENDER HTML ---
        return `
            <div class="match-item" style="
            background:rgba(0, 0, 0, 0.7);padding:10px;border-radius:8px;margin:8px 0;
            box-shadow: 0 4px 14px rgba(255, 140, 0, 0.5);
            text-align:left;
            ">
            <strong style="color: ${statusColor};">${player1Name} vs ${player2Name}</strong><br>
            ${winnerDisplay}<br>
            <span class="muted">${formattedDate}</span>
            </div>
        `;
    }).join("");
}

// Xử lý dữ liệu nhận được
function handleHistoryData(data) {
    console.log("[History.js] Dữ liệu nhận:", data);
    
    if (data && Array.isArray(data.history)) {
        renderHistoryList(data.history);
    } else {
        console.error("[History.js] Dữ liệu lịch sử nhận được không hợp lệ.", data);
        historyContainer.innerHTML = "<p>Lỗi: Không thể tải danh sách lịch sử.</p>";
    }
}

// Hàm fetch dữ liệu từ API
async function fetchHistoryData() {
    const currentUserId = localStorage.getItem("playerId") || "unknown";
    
    try {
        // Gửi yêu cầu HTTP GET
        const response = await fetch(`${API_URL}/history?playerId=${currentUserId}`); 
        const data = await response.json();
        
        if (response.ok) {
            handleHistoryData(data);
        } else {
            throw new Error(data.message || "Lỗi khi tải lịch sử.");
        }
    } catch (error) {
        console.error("[History.js] Lỗi Fetch:", error);
        historyContainer.innerHTML = `<p>Lỗi kết nối hoặc dữ liệu: ${error.message}</p>`;
    }
}

// --- Listener (Sử dụng fetch thay vì sendMessage) ---
if (historyLink) {
    historyLink.addEventListener("click", (e) => {
        e.preventDefault();
        
        historyPopup.style.display = "flex";
        historyContainer.innerHTML = "Đang tải...";
        
        fetchHistoryData();
    });
}

if (historyClose) {
    historyClose.addEventListener("click", () => {
        historyPopup.style.display = "none";
    });
}