import { connectMainSocket, sendMessage, registerHandler } from './Connect_websocket.js';

// GIẢ ĐỊNH: Các phần tử này có ID:
const friendsLink = document.getElementById('friendsLink');
const friendsPopup = document.getElementById('friendsPopup');
const friendsContainer = document.getElementById('friendsContainer');
const friendsClose = document.getElementById('friendsClose');

// --- 1. Hàm Render ---

function renderFriendsList(list) {
    if (!list || list.length === 0) {
        friendsContainer.innerHTML = "<p>Chưa có ai trong danh sách bạn bè.</p>";
        return;
    }

    friendsContainer.innerHTML = list.map(friend => `
        <div class="friend-item" style="
          background:rgba(0, 0, 0, 0.6);padding:10px;border-radius:6px;margin:8px 0;
          display: flex; justify-content: space-between; align-items: center;
          color: #eee; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
        ">
            <strong>${friend.playerName}</strong> 
            <span>${friend.status === 'online' ? '<b style="color:#00ff00;">Online</b>' : 'Offline'}</span>
            <span class="score">${friend.score ? `Điểm: ${friend.score}` : ''}</span>
        </div>
    `).join("");
}

// --- 2. Đăng ký Handler ---

function handleFriendsData(data) {
    renderFriendsList(data.friends);
}

registerHandler("friends_list", handleFriendsData);

// --- 3. Listener ---

if (friendsLink) {
    friendsLink.addEventListener("click", (e) => {
        e.preventDefault();
        friendsPopup.style.display = "flex";
        friendsContainer.innerHTML = "Đang tải...";
        
        connectMainSocket();
        sendMessage({ type: "get_friends" });
    });

    friendsClose.addEventListener("click", () => {
        friendsPopup.style.display = "none";
    });
}