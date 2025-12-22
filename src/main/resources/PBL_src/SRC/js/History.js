// File: History.js
const historyLink = document.getElementById('historyLink');
const historyPopup = document.getElementById('historyPopup');
const historyContainer = document.getElementById('historyContainer');
const historyClose = document.getElementById('historyClose');

const API_URL = "http://localhost:8910/api"; // Đảm bảo đúng địa chỉ server

function renderHistoryList(list) {
    if (!list || list.length === 0) {
        historyContainer.innerHTML = "<p style='color:white'>Chưa có trận đấu nào.</p>";
        return;
    }

    historyContainer.innerHTML = list.map(match => {
        // API trả về đối tượng match lồng nhau, ta truy cập userName qua player1 và player2
        const player1Name = match.player1 ? match.player1.userName : "Người chơi 1";
        const player2Name = match.player2 ? match.player2.userName : "Người chơi 2";
        
        let winnerText;
        if (match.matchStatus === "Finished") {
             // Quy ước: player1 là Trắng, player2 là Đen. pgnNotation lưu kết quả chuẩn.
             if (match.pgnNotation && match.pgnNotation.includes("1-0")) {
                 winnerText = player1Name;
             } else if (match.pgnNotation && match.pgnNotation.includes("0-1")) {
                 winnerText = player2Name;
             } else if (match.pgnNotation && match.pgnNotation.includes("1/2-1/2")) {
                 winnerText = "Hòa";
             } else {
                 winnerText = "Đã kết thúc"; // Fallback
             }
        } else {
            winnerText = "Đang diễn ra";
        }

        return `
        <div class="match-item" style="
            background: rgba(0, 0, 0, 0.6);
            padding: 12px 15px;
            border-radius: 8px;
            margin: 10px 0;
            box-shadow: 0 2px 8px rgba(0,0,0,0.5);
            text-align: left;
            color: #eee;
            border-left: 4px solid #ff9800;
        ">
            <div style="font-size: 1.1em; margin-bottom: 5px; display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <span style="color: #f0f0f0; font-weight: bold;">${player1Name} (Trắng)</span> 
                    <span style="color: #aaa;"> vs </span> 
                    <span style="color: #f0f0f0; font-weight: bold;">${player2Name} (Đen)</span>
                </div>
                <span style="font-size: 0.8em; color: #aaa;">${new Date(match.startTime).toLocaleDateString()}</span>
            </div>
            <div style="font-size: 0.9em; color: #ccc;">
                <span>🏆 Kết quả: <b style="color: #4CAF50;">${winnerText}</b></span>
            </div>
        </div>
    `}).join("");
}

// Xử lý dữ liệu nhận được
function handleHistoryData(data) {
    console.log("[History.js] Dữ liệu nhận:", data);
    
    if (data && Array.isArray(data.history)) {
        renderHistoryList(data.history);
    } else {
        console.error("[History.js] Dữ liệu lịch sử nhận được không hợp lệ.", data);
        historyContainer.innerHTML = "<p style='color:red'>Lỗi: Không thể tải danh sách lịch sử.</p>";
    }
}

// Hàm fetch dữ liệu từ API
async function fetchHistoryData() {
    const currentUserId = localStorage.getItem("playerId");
    
    if (!currentUserId || currentUserId === "unknown" || currentUserId.startsWith("guest_")) {
        historyContainer.innerHTML = "<p style='color:white'>Vui lòng đăng nhập để xem lịch sử đấu.</p>";
        return;
    }

    try {
        // Gửi yêu cầu HTTP GET
        const response = await fetch(`${API_URL}/history?playerId=${currentUserId}`); 
        const data = await response.json();
        
        if (response.ok && data.success) {
            handleHistoryData(data);
        } else {
            throw new Error(data.error || "Lỗi khi tải lịch sử.");
        }
    } catch (error) {
        console.error("[History.js] Lỗi Fetch:", error);
        historyContainer.innerHTML = `<p style='color:red'>Lỗi kết nối hoặc dữ liệu: ${error.message}</p>`;
    }
}

// --- Listener ---
if (historyLink) {
    historyLink.addEventListener("click", (e) => {
        e.preventDefault();
        
        historyPopup.style.display = "flex";
        historyContainer.innerHTML = "<p style='color:white'>Đang tải...</p>";
        
        fetchHistoryData();
    });
}

if (historyClose) {
    historyClose.addEventListener("click", () => {
        historyPopup.style.display = "none";
    });
}