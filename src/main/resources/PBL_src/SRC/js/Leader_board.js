// File: Leaderboard.js
const leaderboardLink = document.getElementById('leaderboardLink');
const leaderboardPopup = document.getElementById('leaderboardPopup');
const leaderboardContainer = document.getElementById('leaderboardContainer');
const leaderboardClose = document.getElementById('leaderboardClose');

const API_URL = "http://localhost:8910/api"; // Sửa lại đường dẫn API

function renderLeaderboard(list) {
    if (!list || list.length === 0) {
        leaderboardContainer.innerHTML = "<p>Bảng xếp hạng hiện đang trống.</p>";
        return;
    }

    leaderboardContainer.innerHTML = list.map((player, index) => {
        const rankColor = index === 0 ? '#FFD700' : index === 1 ? '#C0C0C0' : index === 2 ? '#CD7F32' : 'transparent';
        
        // Xử lý dữ liệu trả về từ API (khớp với user entity)
        const name = player.userName || player.playerName || "Unknown";
        const elo = player.eloRating || player.elo || 1200;
        const wins = player.winCount || player.wins || 0;

        return `
            <div class="leaderboard-item" style="
              background:rgba(0, 0, 0, 0.6);padding:12px;border-radius:6px;margin:10px 0;
              display: flex; justify-content: space-between; align-items: center;
              color: #eee; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
              border-left: 4px solid ${rankColor};
            ">
                <span class="rank" style="font-weight: bold; width: 30px;">#${index + 1}</span>
                <strong style="flex-grow: 1;">${name}</strong> 
                <span class="rating" style="color: #88ff88; font-weight: bold;">ELO: ${elo}</span>
                <span class="wins" style="margin-left: 15px; color: #bbb;">${wins} Thắng</span>
            </div>
        `;
    }).join("");
}

function handleLeaderboardData(data) {
    // API trả về { success: true, data: [...] }
    if (data.success && Array.isArray(data.data)) {
        renderLeaderboard(data.data);
    } else {
        leaderboardContainer.innerHTML = "<p>Không thể tải dữ liệu bảng xếp hạng.</p>";
    }
}

// Hàm fetch dữ liệu từ API
async function fetchLeaderboardData() {
    try {
        const response = await fetch(`${API_URL}/leaderboard`);
        const data = await response.json();
        
        if (response.ok) {
            handleLeaderboardData(data);
        } else {
            throw new Error(data.message || "Lỗi khi tải bảng xếp hạng.");
        }
    } catch (error) {
        console.error("[Leaderboard.js] Lỗi Fetch:", error);
        leaderboardContainer.innerHTML = `<p>Lỗi kết nối hoặc dữ liệu: ${error.message}</p>`;
    }
}

// --- Listener (Sử dụng fetch thay vì sendMessage) ---
if (leaderboardLink) {
    leaderboardLink.addEventListener("click", (e) => {
        e.preventDefault();
        leaderboardPopup.style.display = "flex";
        leaderboardContainer.innerHTML = "Đang tải...";
        
        fetchLeaderboardData();
    });

    leaderboardClose.addEventListener("click", () => {
        leaderboardPopup.style.display = "none";
    });
}