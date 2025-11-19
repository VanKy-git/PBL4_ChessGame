const friendsLink = document.getElementById('friendsLink');
const friendsPopup = document.getElementById('friendsPopup');
const friendsContainer = document.getElementById('friendsContainer');
const friendsClose = document.getElementById('friendsClose');

const API_URL = "http://localhost:8910/api";

// Hàm xử lý gọi API DELETE để xóa bạn bè
async function deleteFriend(friendshipId) {
    if (!friendshipId) {
        console.error("Thiếu friendshipId để xóa.");
        return;
    }

    // Hiển thị thông báo đang xóa
    friendsContainer.innerHTML = `<p style="color: #ffaa00; text-align: center;">Đang xóa bạn bè (ID: ${friendshipId})...</p>`;

    try {
        // Gửi friendshipId qua query parameter cho phương thức DELETE
        const url = `${API_URL}/friends?friendshipId=${friendshipId}`;
        
        const response = await fetch(url, {
            method: 'DELETE',
        });
        
        // Kiểm tra xem dữ liệu có phải JSON không, nếu không thì lấy text
        const contentType = response.headers.get("content-type");
        const data = contentType && contentType.includes("application/json") ? await response.json() : await response.text();
        
        if (response.ok && (typeof data === 'string' || data.success)) {
            console.log(`✅ Đã xóa thành công friendship ID: ${friendshipId}`);
            // Sau khi xóa thành công, tải lại danh sách
            fetchFriendsData(); 
        } else {
            const errorMessage = (data.message || data || 'Lỗi không xác định');
            console.error(`❌ Lỗi xóa bạn bè: ${errorMessage}`);
            friendsContainer.innerHTML = `<p style="color: red; text-align: center;">Lỗi xóa: ${errorMessage}</p>`;
            // Tải lại danh sách sau 3 giây nếu lỗi
            setTimeout(fetchFriendsData, 3000); 
        }
    } catch (error) {
        console.error("[Friends.js] Lỗi Fetch DELETE:", error);
        friendsContainer.innerHTML = `<p style="color: red; text-align: center;">Lỗi kết nối khi xóa: ${error.message}</p>`;
    }
}


function renderFriendsList(list, currentNumericUserId) {
    console.log("🔍 [DEBUG] Rendering list:", list); 
    
    if (!list || list.length === 0) {
        friendsContainer.innerHTML = "<p>Chưa có ai trong danh sách bạn bè.</p>";
        return;
    }

    const htmlContent = list.map((friend, index) => {
        
        // 1. Xác định đối tượng nào là 'người bạn'
        let friendObject = null;
        if (friend.user1 && friend.user1.user_id !== currentNumericUserId) {
            friendObject = friend.user1;
        } else if (friend.user2 && friend.user2.user_id !== currentNumericUserId) {
            friendObject = friend.user2;
        }

        // Lấy ID quan hệ (quan trọng cho chức năng xóa)
        const friendshipId = friend.friendshipId; 
        
        // 2. Lấy tên, trạng thái và điểm từ đối tượng người bạn
        const playerName = friendObject ? friendObject.userName : "Không rõ tên";
        const status = friend.status; 
        const score = friendObject ? friendObject.eloRating : 0;

        return `
            <div class="friend-item" data-friendship-id="${friendshipId}" style="
              background:rgba(0, 0, 0, 0.6);padding:10px;border-radius:6px;margin:8px 0;
              display: flex; justify-content: space-between; align-items: center;
              color: #eee; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
            ">
                <div style="flex-grow: 1;">
                    <strong>${playerName}</strong> 
                    <span>${status === 'online' ? '<b style="color:#00ff00;">Online</b>' : 'Offline'}</span>
                    <span class="score">Điểm: ${score}</span>
                </div>
                <!-- NÚT XÓA -->
                <button 
                    class="delete-friend-btn" 
                    data-id="${friendshipId}"
                    style="
                        background: #cc0000; color: white; border: none; padding: 5px 10px;
                        border-radius: 4px; cursor: pointer; font-size: 0.8em; margin-left: 10px;
                        transition: background 0.2s;
                    "
                    onmouseover="this.style.background='#ff3333'"
                    onmouseout="this.style.background='#cc0000'"
                >
                    Xóa
                </button>
            </div>
        `;
    }).join("");

    friendsContainer.innerHTML = htmlContent;
    
    // 3. GẮN EVENT LISTENER SAU KHI RENDER
    document.querySelectorAll('.delete-friend-btn').forEach(button => {
        button.addEventListener('click', (e) => {
            // Lấy ID quan hệ từ data-id của nút
            const idToDelete = e.target.getAttribute('data-id');
            // Sử dụng confirm vì đây là ứng dụng độc lập, nhưng trong dự án lớn nên dùng modal custom
            if (idToDelete && window.confirm(`Bạn có chắc chắn muốn xóa bạn bè này không? (ID quan hệ: ${idToDelete})`)) {
                 deleteFriend(idToDelete);
            }
        });
    });
}

function handleFriendsData(data, currentUserId) { 
    if (data && Array.isArray(data.data)) {
        // Chuyển ID sang số để so sánh nghiêm ngặt trong render list
        const currentNumericUserId = parseInt(currentUserId); 
        renderFriendsList(data.data, currentNumericUserId);
    } else {
        console.warn("[Friends.js] Cấu trúc dữ liệu nhận được không hợp lệ:", data);
        friendsContainer.innerHTML = `<p>Lỗi: Dữ liệu bạn bè không đúng định dạng.</p>`;
    }
}


// Hàm fetch dữ liệu từ API
async function fetchFriendsData() {
    const currentUserId = localStorage.getItem("playerId") || "unknown"; 

    try {
        const url = `${API_URL}/friends?playerId=${currentUserId}`;
        
        const response = await fetch(url);
        const data = await response.json();
        
        if (response.ok) {
            handleFriendsData(data, currentUserId); 
        } else {
            throw new Error(data.message || "Lỗi khi tải danh sách bạn bè.");
        }
    } catch (error) {
        console.error("[Friends.js] Lỗi Fetch:", error);
        friendsContainer.innerHTML = `<p>Lỗi kết nối: ${error.message}</p>`;
    }
}

// --- Listener ---
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