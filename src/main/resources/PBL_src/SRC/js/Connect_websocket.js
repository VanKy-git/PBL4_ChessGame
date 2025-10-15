// ../js/Connect_websocket.js

let mainSocket = null;
const SOCKET_URL = "ws://localhost:8080";
const messageHandlers = {};
let pendingMessages = []; // ✅ hàng đợi tin nhắn chờ gửi khi socket chưa mở

/**
 * Kết nối tới WebSocket server hoặc trả về socket đã mở.
 */
export function connectMainSocket() {
    if (mainSocket && mainSocket.readyState === WebSocket.OPEN) {
        return mainSocket;
    }

    // Nếu socket chưa tồn tại hoặc đã đóng thì tạo mới
    if (!mainSocket || mainSocket.readyState === WebSocket.CLOSED) {
        mainSocket = new WebSocket(SOCKET_URL);
    }

    mainSocket.onopen = () => {
        console.log(" Đã kết nối server chính.");
        // Gửi tất cả tin nhắn đang chờ
        if (pendingMessages.length > 0) {
            console.log(` Gửi ${pendingMessages.length} tin nhắn chờ...`);
            pendingMessages.forEach(msg => mainSocket.send(JSON.stringify(msg)));
            pendingMessages = [];
        }
    };

    mainSocket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            const handler = messageHandlers[data.type];
            console.log(" Nhận từ server:", data);

            if (handler) {
                handler(data);
            } else {
                console.warn(`[Socket] Không có handler được đăng ký cho loại: ${data.type}`);
            }
        } catch (e) {
            console.error("[Socket] Lỗi phân tích tin nhắn WebSocket:", e);
        }
    };

    mainSocket.onclose = () => console.log("🔌 Đã ngắt kết nối WebSocket");
    mainSocket.onerror = (e) => console.error("[Socket] Lỗi socket:", e);

    return mainSocket;
}

/**
 * Gửi đối tượng JavaScript dưới dạng chuỗi JSON qua WebSocket.
 */
export function sendMessage(messageObject) {
    if (mainSocket && mainSocket.readyState === WebSocket.OPEN) {
        mainSocket.send(JSON.stringify(messageObject));
        console.log(" Gửi tới server:", messageObject);
        return true;
    }

    if (!mainSocket || mainSocket.readyState === WebSocket.CLOSED) {
        connectMainSocket();
    }

    console.warn(" Socket chưa mở, thêm vào hàng chờ:", messageObject);
    pendingMessages.push(messageObject);
    return false;
}

/**
 * Đăng ký hàm xử lý cho một loại tin nhắn cụ thể.
 */
export function registerHandler(type, handlerFunction) {
    messageHandlers[type] = handlerFunction;
}
