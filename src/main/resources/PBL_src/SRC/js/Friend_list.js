import { sendMessage, registerHandler } from './Connect_websocket.js';

const friendsLink = document.getElementById('friendsLink');
const friendsPopup = document.getElementById('friendsPopup');
const friendsTabContent = document.getElementById('friendsTabContent');
const friendsClose = document.getElementById('friendsClose');
const tabs = document.querySelectorAll('.tab-btn');

let currentTab = 'list';
let pendingInvite = null;

// --- RENDER FUNCTIONS ---

function renderFriendsList(friends) {
    if (!friends || friends.length === 0) {
        friendsTabContent.innerHTML = "<p class='empty-msg'>Chưa có bạn bè.</p>";
        return;
    }
    friendsTabContent.innerHTML = friends.map(friend => {
        const friendStatus = friend.friend_status;
        let actionButton;

        if (friendStatus === 'In Game') {
            actionButton = `
                <button class="btn-action spectate-btn" data-id="${friend.friend_id}">
                    Xem trận
                </button>`;
        } else {
            const isOnline = friendStatus === 'Online';
            actionButton = `
                <button class="btn-action invite-btn" 
                    id="btn-invite-${friend.friend_id}"
                    data-id="${friend.friend_id}" 
                    data-name="${friend.friend_name}" 
                    ${!isOnline ? 'disabled' : ''}>
                    ${isOnline ? "Mời đấu" : "Offline"}
                </button>`;
        }

        return `
            <div class="friend-item" data-id="${friend.friend_id}">
                <img src="${friend.avatar_url || '../../PBL4_imgs/icon/default_avatar.png'}" alt="Avatar" class="user-avatar-small">
                <div class="friend-info">
                    <strong>${friend.friend_name}</strong>
                    <span class="status-badge ${friendStatus === 'Online' || friendStatus === 'In Game' ? 'online' : 'offline'}">
                        ${friendStatus || 'Offline'}
                    </span>
                </div>
                ${actionButton}
            </div>
        `;
    }).join("");
}
//
// function renderSearchResults(users) {
//     friendsTabContent.innerHTML = `
//         <div class="search-bar">
//             <input type="text" id="searchInput" class="input" placeholder="Nhập tên người dùng..."/>
//             <button id="searchBtn" class="btn-action">Tìm kiếm</button>
//         </div>
//         <div id="searchResults" class="results-list">
//             ${users && users.length > 0 ? users.map(user => `
//                 <div class="user-item" data-id="${user.userId}">
//                     <img src="${user.avatarUrl || '../../PBL4_imgs/icon/default_avatar.png'}" alt="Avatar" class="user-avatar-small">
//                     <div class="user-info">
//                         <strong>${user.userName}</strong>
//                         <span class="elo-rating">Elo: ${user.elo}</span>
//                     </div>
//                     <button class="btn-action add-friend-btn" data-id="${user.userId}">Kết bạn</button>
//                 </div>
//             `).join("") : (users ? "<p class='empty-msg'>Không tìm thấy người dùng.</p>" : "")}
//         </div>
//     `;
// }

    // thay mới

function renderSearchResults(users) {
    friendsTabContent.innerHTML = `
        <div class="search-bar">
            <input type="text" id="searchInput" class="input" placeholder="Nhập tên người dùng..."/>
            <button id="searchBtn" class="btn-action">Tìm kiếm</button>
        </div>
        <div id="searchResults" class="results-list">
            ${users && users.length > 0 ? users.map(user => {

        // Logic hiển thị nút bấm dựa trên quan hệ
        let buttonHtml;

        if (user.relationship === 'friend') {
            // Trạng thái: Đã là bạn bè -> Disable nút
            buttonHtml = `<button class="btn-action" disabled style="background-color: #ccc; cursor: not-allowed;">Đã kết bạn</button>`;
        }
        else if (user.relationship === 'pending') {
            // Trạng thái: Đang chờ (đã gửi lời mời hoặc đang có lời mời)
            buttonHtml = `<button class="btn-action" disabled style="background-color: #ccc; cursor: not-allowed;">Đang chờ</button>`;
        }
        else {
            // Trạng thái: Chưa kết bạn -> Hiện nút Kết bạn bình thường
            buttonHtml = `<button class="btn-action add-friend-btn" data-id="${user.userId}">Kết bạn</button>`;
        }

        return `
                <div class="user-item" data-id="${user.userId}">
                    <img src="${user.avatarUrl || user.avatar_url || '../../PBL4_imgs/icon/default_avatar.png'}" 
                         alt="Avatar" class="user-avatar-small"
                         onerror="this.onerror=null;this.src='../../PBL4_imgs/icon/man.png';">
                    
                    <div class="user-info">
                        <strong>${user.userName}</strong>
                        <span class="elo-rating">Elo: ${user.elo}</span>
                    </div>
                    
                    ${buttonHtml}
                </div>
            `}).join("") : (users ? "<p class='empty-msg'>Không tìm thấy người dùng.</p>" : "")}
        </div>
    `;

    // Gán lại sự kiện click cho nút Search mới được render
    document.getElementById('searchBtn').addEventListener('click', () => {
        const keyword = document.getElementById('searchInput').value;
        if (keyword.trim()) {
            sendMessage({ type: 'search_users', keyword: keyword });
        }
    });
}


function renderFriendRequests(requests) {
    if (!requests || requests.length === 0) {
        friendsTabContent.innerHTML = "<p class='empty-msg'>Không có lời mời kết bạn nào.</p>";
        return;
    }
    friendsTabContent.innerHTML = requests.map(req => `
        <div class="request-item" data-id="${req.friendship_id}">
            <img src="${req.avatar_url || '../../PBL4_imgs/icon/default_avatar.png'}" alt="Avatar" class="user-avatar-small">
            <div class="request-info">
                <strong>${req.friend_name}</strong>
                <span class="request-msg">muốn kết bạn với bạn</span>
            </div>
            <div class="request-actions">
                <button class="btn-action confirm accept-friend-btn" data-id="${req.friendship_id}">Chấp nhận</button>
                <button class="btn-action cancel reject-friend-btn" data-id="${req.friendship_id}">Từ chối</button>
            </div>
        </div>
    `).join("");
}

// --- TAB HANDLING ---

function showTab(tabName) {
    currentTab = tabName;
    tabs.forEach(tab => {
        tab.classList.toggle('active', tab.dataset.tab === tabName);
    });

    friendsTabContent.innerHTML = "<div class='loading-spinner'></div>";
    
    if (tabName === 'list') {
        sendMessage({ type: 'get_friends' });
    } else if (tabName === 'search') {
        renderSearchResults(null);
    } else if (tabName === 'requests') {
        sendMessage({ type: 'get_friends' });
    }
}

// --- EVENT LISTENERS ---

if (friendsLink) {
    friendsLink.addEventListener('click', (e) => {
        e.preventDefault();
        friendsPopup.style.display = 'flex';
        showTab('list');
    });
}

if (friendsClose) {
    friendsClose.addEventListener('click', () => {
        friendsPopup.style.display = 'none';
    });
}

tabs.forEach(tab => {
    tab.addEventListener('click', () => showTab(tab.dataset.tab));
});

if (friendsTabContent) {
    friendsTabContent.addEventListener('click', (e) => {
        const target = e.target;
        
        // Search
        if (target.id === 'searchBtn') {
            const keyword = document.getElementById('searchInput').value;
            if (keyword.trim()) {
                sendMessage({ type: 'search_users', keyword: keyword });
            }
        } 
        // Add Friend
        else if (target.classList.contains('add-friend-btn')) {
            const receiverId = target.dataset.id;
            sendMessage({ type: 'friend_request', receiverId: parseInt(receiverId) });
            target.disabled = true;
            target.textContent = "Đã gửi";
        } 
        // Accept Friend
        else if (target.classList.contains('accept-friend-btn')) {
            const friendshipId = target.dataset.id;
            sendMessage({ type: 'accept_friend', friendshipId: parseInt(friendshipId) });
        } 
        // Reject Friend
        else if (target.classList.contains('reject-friend-btn')) {
            const friendshipId = target.dataset.id;
            sendMessage({ type: 'reject_friend', friendshipId: parseInt(friendshipId) });
        } 
        // Invite Game
        else if (target.classList.contains('invite-btn')) {
            const friendId = target.dataset.id;
            const friendName = target.dataset.name;
            pendingInvite = { friendId, friendName };
            
            const inviteNameEl = document.getElementById('invite-friend-name');
            if(inviteNameEl) inviteNameEl.textContent = friendName;
            
            const invitePopup = document.getElementById('invite-popup');
            if(invitePopup) invitePopup.classList.remove('hidden');
        }
        // Spectate Game
        else if (target.classList.contains('spectate-btn')) {
            const friendId = target.dataset.id;
            sendMessage({ type: 'spectate_game', friendId: parseInt(friendId) });
            friendsPopup.style.display = 'none';
        }
    });
}

// Invite Popup Listeners
const cancelInviteBtn = document.getElementById('cancelInviteBtn');
if (cancelInviteBtn) {
    cancelInviteBtn.addEventListener('click', () => {
        document.getElementById('invite-popup').classList.add('hidden');
        pendingInvite = null;
    });
}

document.querySelectorAll('#invite-popup .time-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        if (pendingInvite) {
            const timeControl = parseInt(btn.dataset.time);
            sendMessage({ type: 'invite_friend', friendId: pendingInvite.friendId, timeControl: timeControl });
            
            // Cập nhật nút thành "Đã mời" và disable
            const inviteBtn = document.getElementById(`btn-invite-${pendingInvite.friendId}`);
            if (inviteBtn) {
                inviteBtn.textContent = "Đã mời";
                inviteBtn.disabled = true;
            }

            document.getElementById('invite-popup').classList.add('hidden');
            // Không set pendingInvite = null ngay, để giữ ref nếu cần xử lý thêm, 
            // nhưng ở đây ta dùng ID để tìm lại nút nên có thể null cũng được.
            // pendingInvite = null; 
        }
    });
});

// Game Invite Response Listeners
let pendingGameInvite = null;
const acceptInviteBtn = document.getElementById('acceptInviteBtn');
if (acceptInviteBtn) {
    acceptInviteBtn.addEventListener('click', () => {
        if (pendingGameInvite) {
            // Nếu người chơi đang trong hàng đợi tìm trận, hủy nó đi
            if (typeof handleCancelMatchmaking === 'function') {
                handleCancelMatchmaking();
            }
            sendMessage({ type: 'invite_response', accepted: true, opponentId: pendingGameInvite.fromPlayerId });
            document.getElementById('game-invite-popup').classList.add('hidden');
            pendingGameInvite = null;
        }
    });
}

const declineInviteBtn = document.getElementById('declineInviteBtn');
if (declineInviteBtn) {
    declineInviteBtn.addEventListener('click', () => {
        if (pendingGameInvite) {
            sendMessage({ type: 'invite_response', accepted: false, opponentId: pendingGameInvite.fromPlayerId });
            document.getElementById('game-invite-popup').classList.add('hidden');
            pendingGameInvite = null;
        }
    });
}


// --- WEBSOCKET HANDLERS ---

// registerHandler('friends_list', (data) => {
//     const myId = parseInt(localStorage.getItem("playerId"));
//
//     if (currentTab === 'list') {
//         // Lọc bạn bè đã chấp nhận (status = 'accepted')
//         const acceptedFriends = data.friends.filter(f => f.status && f.status.toLowerCase() === 'accepted');
//         renderFriendsList(acceptedFriends);
//     } else if (currentTab === 'requests') {
//         // Lọc lời mời kết bạn ĐANG CHỜ (status = 'pending')
//         // VÀ người gửi (sender_id) KHÁC mình (tức là mình là người nhận)
//         const pendingRequests = data.friends.filter(f =>
//             f.status && f.status.toLowerCase() === 'pending' &&
//             f.sender_id !== myId
//         );
//         renderFriendRequests(pendingRequests);
//     }
// });

registerHandler('friends_list', (data) => {
    // 1. Lấy ID của bản thân (Ép kiểu về số nguyên để so sánh chuẩn)
    const myId = parseInt(localStorage.getItem("playerId"));

    console.log("🔥 DEBUG FRIEND LIST:");
    console.log("My ID:", myId);

    if (currentTab === 'list') {
        const acceptedFriends = data.friends.filter(f =>
            f.status && f.status.toLowerCase() === 'accepted'
        );
        renderFriendsList(acceptedFriends);
    }
    else if (currentTab === 'requests') {
        // 2. Lọc danh sách lời mời
        const receivedRequests = data.friends.filter(f => {
            // Debug từng dòng xem tại sao nó không ẩn
            const isPending = f.status && f.status.toLowerCase() === 'pending';
            const isNotSender = f.sender_id !== myId;

            // In ra console để kiểm tra
            if (isPending && !isNotSender) {
                console.log(`Ẩn lời mời gửi tới ${f.friend_name} vì mình là người gửi (SenderID: ${f.sender_id})`);
            }

            // Giữ lại nếu: Là Pending VÀ Mình KHÔNG phải người gửi
            return isPending && isNotSender;
        });

        console.log("Danh sách hiển thị sau khi lọc:", receivedRequests);
        renderFriendRequests(receivedRequests);
    }
});

registerHandler('search_results', (data) => {
    renderSearchResults(data.users);
});

registerHandler('friend_request_sent', () => {
    // alert('Đã gửi lời mời kết bạn!');
});

registerHandler('friend_request_accepted', () => {
    if (currentTab === 'requests') {
        sendMessage({ type: 'get_friends' });
    }
});

registerHandler('friend_request_rejected', () => {
    if (currentTab === 'requests') {
        sendMessage({ type: 'get_friends' });
    }
});

registerHandler('game_invite', (data) => {
    pendingGameInvite = data;
    const inviterNameEl = document.getElementById('inviter-name');
    if(inviterNameEl) inviterNameEl.textContent = data.fromPlayerName;
    
    const gameInvitePopup = document.getElementById('game-invite-popup');
    if(gameInvitePopup) gameInvitePopup.classList.remove('hidden');
});

registerHandler('invite_sent', () => {
    // Không alert nữa, vì nút đã đổi trạng thái
});

registerHandler('invite_rejected', (data) => {
    alert('Người chơi đã từ chối lời mời.');
    
    // Tìm nút mời của người chơi này và enable lại
    // data.fromPlayerId là ID của người TỪ CHỐI (người mình đã mời)
    // Nhưng server gửi về: type: invite_rejected, fromPlayerId: <ID người từ chối>
    
    // Cần đảm bảo server gửi đúng ID người từ chối về
    if (data.fromPlayerId) {
        const inviteBtn = document.getElementById(`btn-invite-${data.fromPlayerId}`);
        if (inviteBtn) {
            inviteBtn.textContent = "Mời đấu";
            inviteBtn.disabled = false;
        }
    }
});

// --- INIT ---
// connectMainSocket();
