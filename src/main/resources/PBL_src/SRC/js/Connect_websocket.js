// ../js/Connect_websocket.js


let mainSocket = null;
const SOCKET_URL = "ws://10.10.30.103:8080";
const messageHandlers = {}; // ✅ Dùng cái này
let pendingMessages = [];

/**
 * Kết nối tới WebSocket server hoặc trả về socket đã mở.
 */
export function connectMainSocket() {
    if (mainSocket && mainSocket.readyState === WebSocket.OPEN) {
        return mainSocket;
    }
    if (!mainSocket || mainSocket.readyState === WebSocket.CLOSED) {
        mainSocket = new WebSocket(SOCKET_URL);
    }

    mainSocket.onopen = () => {
        console.log("✅ Đã kết nối server chính.");
        if (pendingMessages.length > 0) {
            console.log(` Gửi ${pendingMessages.length} tin nhắn chờ...`);
            pendingMessages.forEach(msg => mainSocket.send(JSON.stringify(msg)));
            pendingMessages = [];
        }
    };

    mainSocket.onmessage = (event) => {
        try {
            const msg = JSON.parse(event.data);
            console.log('Received:', msg);
            // ✅ Gọi hàm nội bộ để điều phối
            handleMessage(msg);
        } catch (e) {
            console.error("[Socket] Lỗi phân tích tin nhắn WebSocket:", e);
        }
    };

    mainSocket.onclose = () => {
        console.log("🔌 Đã ngắt kết nối WebSocket");
        mainSocket = null; // ✅ Reset socket
    };
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
    if (messageHandlers[type]) {
        console.warn(`[Socket] Ghi đè handler cho type: ${type}`);
    }
    messageHandlers[type] = handlerFunction;
}

/**
 * ✅ HÀM NỘI BỘ: Điều phối tin nhắn
 * (Hàm này mà bạn đã thiếu ở lượt trước)
 */
function handleMessage(msg) {
    if (msg.type && messageHandlers[msg.type]) {
        messageHandlers[msg.type](msg);
    } else {
        console.warn(`[Socket] Không tìm thấy hàm xử lý cho tin nhắn loại: ${msg.type}`);
    }
}