// File: Friends.js
const friendsLink = document.getElementById('friendsLink');
const friendsPopup = document.getElementById('friendsPopup');
const friendsContainer = document.getElementById('friendsContainer');
const friendsClose = document.getElementById('friendsClose');

const API_URL = "http://localhost:8910/api";

function renderFriendsList(list) {
    console.log("🔍 [DEBUG] Rendering list:", list); // ✅ Log để xem dữ liệu
    
    if (!list || list.length === 0) {
        friendsContainer.innerHTML = "<p>Chưa có ai trong danh sách bạn bè.</p>";
        return;
    }

    friendsContainer.innerHTML = list.map((friend, index) => {
        console.log(`🔍 [DEBUG] Friend ${index}:`, friend); // ✅ Log từng phần tử
        
        // ✅ Kiểm tra an toàn từng field
        const playerName = friend.playerName || friend.friendName || friend.username || "Không rõ tên";
        const status = friend.status || 'offline';
        const score = friend.score || 0;
        
        return `
            <div class="friend-item" style="
              background:rgba(0, 0, 0, 0.6);padding:10px;border-radius:6px;margin:8px 0;
              display: flex; justify-content: space-between; align-items: center;
              color: #eee; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
            ">
                <strong>${playerName}</strong> 
                <span>${status === 'online' ? '<b style="color:#00ff00;">Online</b>' : 'Offline'}</span>
                <span class="score">Điểm: ${score}</span>
            </div>
        `;
    }).join("");
}

function handleFriendsData(data) {
    // Kiểm tra cấu trúc data trước khi render
    if (data && Array.isArray(data.data)) {
        renderFriendsList(data.friends);
    } else {
        console.warn("[Friends.js] Cấu trúc dữ liệu nhận được không hợp lệ:", data);
        friendsContainer.innerHTML = `<p>Lỗi: Dữ liệu bạn bè không đúng định dạng.</p>`;
    }
}


// Hàm fetch dữ liệu từ API
async function fetchFriendsData() {
    const currentUserId = localStorage.getItem("playerId") || "unknown";
    console.log("🔍 [DEBUG] Current userId:", currentUserId); // ✅ Log

    try {
        const url = `${API_URL}/friends?playerId=${currentUserId}`;
        console.log("🔍 [DEBUG] Fetching URL:", url); // ✅ Log
        
        const response = await fetch(url);
        const data = await response.json();
        
        console.log("🔍 [DEBUG] Response data:", data); // ✅ Log
        
        if (response.ok) {
            handleFriendsData(data);
        } else {
            throw new Error(data.message || "Lỗi khi tải danh sách bạn bè.");
        }
    } catch (error) {
        console.error("[Friends.js] Lỗi Fetch:", error);
        friendsContainer.innerHTML = `<p>Lỗi kết nối: ${error.message}</p>`;
    }
}

// --- Listener (Sử dụng fetch thay vì sendMessage) ---
if (friendsLink) {
    friendsLink.addEventListener("click", (e) => {
        e.preventDefault();
        friendsPopup.style.display = "flex";
        friendsContainer.innerHTML = "Đang tải...";
        
        fetchFriendsData();
    });

    friendsClose.addEventListener("click", () => {
        friendsPopup.style.display = "none";
    });
}