import { connectMainSocket, sendMessage, registerHandler } from './Connect_websocket.js';

// GIẢ ĐỊNH: Các phần tử này có ID:
const leaderboardLink = document.getElementById('leaderboardLink');
const leaderboardPopup = document.getElementById('leaderboardPopup');
const leaderboardContainer = document.getElementById('leaderboardContainer');
const leaderboardClose = document.getElementById('leaderboardClose');

// --- 1. Hàm Render ---

function renderLeaderboard(list) {
    if (!list || list.length === 0) {
        leaderboardContainer.innerHTML = "<p>Bảng xếp hạng hiện đang trống.</p>";
        return;
    }

    leaderboardContainer.innerHTML = list.map((player, index) => {
        const rankColor = index === 0 ? '#FFD700' : index === 1 ? '#C0C0C0' : index === 2 ? '#CD7F32' : 'transparent';
        
        // Kiểm tra tên người chơi, nếu không có thì hiển thị "Unknown"
        const playerName = player.playerName || player.username || "Unknown";

        return `
            <div class="leaderboard-item" style="
              background:rgba(0, 0, 0, 0.6);padding:12px;border-radius:6px;margin:10px 0;
              display: flex; justify-content: space-between; align-items: center;
              color: #eee; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
              border-left: 4px solid ${rankColor};
            ">
                <span class="rank" style="font-weight: bold; width: 30px;">#${index + 1}</span>
                <strong style="flex-grow: 1;">${playerName}</strong>
                <span class="rating" style="color: #88ff88; font-weight: bold;">ELO: ${player.elo}</span>
                <span class="wins" style="margin-left: 15px; color: #bbb;">${player.wins} Thắng</span>
            </div>
        `;
    }).join("");
}

// --- 2. Đăng ký Handler ---

function handleLeaderboardData(data) {
    renderLeaderboard(data.leaderboard);
}

registerHandler("leaderboard_data", handleLeaderboardData);

// --- 3. Listener ---

if (leaderboardLink) {
    leaderboardLink.addEventListener("click", (e) => {
        e.preventDefault();
        leaderboardPopup.style.display = "flex";
        leaderboardContainer.innerHTML = "Đang tải...";
        
        connectMainSocket();
        sendMessage({ type: "get_leaderboard" });
    });

    leaderboardClose.addEventListener("click", () => {
        leaderboardPopup.style.display = "none";
    });
}