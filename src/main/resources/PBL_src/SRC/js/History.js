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

    historyContainer.innerHTML = list.map(match => `
        <div class="match-item" style="
        background:rgba(0, 0, 0, 0.7);padding:10px;border-radius:8px;margin:8px 0;
        box-shadow: 0 4px 14px rgba(255, 140, 0, 0.5);
        text-align:left;
        ">
        <strong>${match.playerX}</strong> vs <strong>${match.playerO}</strong><br>
        <span>🏆 Người thắng: <b>${match.winner}</b></span><br>
        <span class="muted">${new Date(match.date).toLocaleString()}</span>
        </div>
    `).join("");
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