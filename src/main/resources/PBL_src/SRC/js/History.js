    // ../js/History.js

    // Đảm bảo đường dẫn này chính xác (chỉ là tên file, không cần đường dẫn đầy đủ nếu nằm cùng thư mục)
    import { connectMainSocket, sendMessage, registerHandler } from './Connect_websocket.js';

    const historyLink = document.getElementById('historyLink');
    const historyPopup = document.getElementById('historyPopup');
    const historyContainer = document.getElementById('historyContainer');
    const historyClose = document.getElementById('historyClose');

    function renderHistoryList(list) {
        if (!list || list.length === 0) {
            historyContainer.innerHTML = "<p>Chưa có trận đấu nào.</p>";
            return;
        }

        historyContainer.innerHTML = list.map(match => {
            // Xử lý hiển thị người thắng
            let winnerDisplay = match.winner;
            
            // Nếu winner là 'waiting' hoặc null/undefined, có thể trận đấu chưa kết thúc hoặc hòa
            if (!winnerDisplay || winnerDisplay.toLowerCase() === 'waiting') {
                winnerDisplay = "Chưa xác định";
            } else if (winnerDisplay.toLowerCase() === 'draw') {
                winnerDisplay = "Hòa";
            } else {
                // Nếu winner trùng với playerX hoặc playerO thì giữ nguyên,
                // nếu winner là 'white' hoặc 'black' thì map về tên người chơi
                if (winnerDisplay === 'white') {
                    // Giả sử playerX là white (cần server xác nhận, nhưng thường là vậy)
                    // Hoặc server nên trả về tên người thắng luôn.
                    // Tạm thời hiển thị tên winner nếu nó khác 'waiting'
                }
            }

            // Format ngày tháng
            let dateDisplay = "Unknown Date";
            if (match.date) {
                try {
                    dateDisplay = new Date(match.date).toLocaleString();
                } catch (e) {
                    console.error("Lỗi format ngày:", e);
                }
            }

            return `
            <div class="match-item" style="
            background:rgba(0, 0, 0, 0.7);padding:10px;border-radius:8px;margin:8px 0;
            box-shadow: 0 4px 14px rgba(255, 140, 0, 0.5);
            text-align:left;
            ">
            <strong>${match.playerX}</strong> vs <strong>${match.playerO}</strong><br>
            <span>🏆 Kết quả: <b>${winnerDisplay}</b></span><br>
            <span class="muted">${dateDisplay}</span>
            </div>
        `}).join("");
    }

    // --- 2. Đăng ký Handler (Có thêm logging để gỡ lỗi) ---

    function handleHistoryData(data) {
        console.log("[History.js] HANDLER LỊCH SỬ ĐÃ CHẠY. Dữ liệu nhận:", data);
        
        // Kiểm tra tính hợp lệ của dữ liệu trước khi render
        if (data && Array.isArray(data.history)) {
            renderHistoryList(data.history);
        } else {
            console.error("[History.js] Dữ liệu lịch sử nhận được không hợp lệ hoặc thiếu trường 'history'.", data);
            historyContainer.innerHTML = "<p>Lỗi: Không thể tải danh sách lịch sử.</p>";
        }
    }

    // Đăng ký hàm xử lý cho type: "history_list"
    registerHandler("history_list", handleHistoryData);


    // --- 3. Listener ---

    if (historyLink) { // Kiểm tra để tránh lỗi nếu DOM chưa sẵn sàng
        historyLink.addEventListener("click", (e) => {
            e.preventDefault();
            
            // Luôn hiển thị trạng thái "Đang tải..."
            historyPopup.style.display = "flex";
            historyContainer.innerHTML = "Đang tải...";
            
            connectMainSocket(); 
            // Gửi yêu cầu. Nếu socket chưa mở, nó sẽ được xếp hàng trong Connect_websocket.js
            sendMessage({ type: "get_history" });
        });
    }

    if (historyClose) {
        historyClose.addEventListener("click", () => {
            historyPopup.style.display = "none";
        });
    }