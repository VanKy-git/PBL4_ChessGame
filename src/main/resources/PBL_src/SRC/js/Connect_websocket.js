let mainSocket = null;
const SOCKET_URL = "ws://localhost:8080";
const messageHandlers = {};
let pendingMessages = [];

export function connectMainSocket(token, playerId) {
    if (mainSocket && mainSocket.readyState === WebSocket.OPEN) {
        return mainSocket;
    }
    if (!mainSocket || mainSocket.readyState === WebSocket.CLOSED) {
        mainSocket = new WebSocket(SOCKET_URL);
    }

    mainSocket.onopen = () => {
        console.log(" Đã kết nối server chính.");
        mainSocket.send(JSON.stringify({
            type: "auth",
            token: token,
            playerId: playerId
        }));
        if (pendingMessages.length > 0) {
            console.log(` Gửi ${pendingMessages.length} tin nhắn chờ...`);
            pendingMessages.forEach(msg => mainSocket.send(JSON.stringify(msg)));
            pendingMessages = [];
        }
    };

    mainSocket.onmessage = (event) => {
        try {
            const msg = JSON.parse(event.data);
            if (msg.type === 'ping') {
                sendMessage({ type: 'pong' });
                return;
            }
            console.log('Received:', msg);
            handleMessage(msg);
        } catch (e) {
            console.error("[Socket] Lỗi phân tích tin nhắn WebSocket:", e);
        }
    };

    mainSocket.onclose = () => {
        console.log("🔌 Đã ngắt kết nối WebSocket");
        mainSocket = null;
    };
    mainSocket.onerror = (e) => console.error("[Socket] Lỗi socket:", e);
    return mainSocket;
}

export function sendMessage(messageObject) {
    if (mainSocket && mainSocket.readyState === WebSocket.OPEN) {
        mainSocket.send(JSON.stringify(messageObject));
        if (messageObject.type !== 'pong') {
             console.log(" Gửi tới server:", messageObject);
        }
        return true;
    }
    if (!mainSocket || mainSocket.readyState === WebSocket.CLOSED) {
        connectMainSocket();
    }
    console.warn(" Socket chưa mở, thêm vào hàng chờ:", messageObject);
    pendingMessages.push(messageObject);
    return false;
}

export function registerHandler(type, handlerFunction) {
    // Nếu chưa có mảng handler cho type này, tạo một mảng mới
    if (!messageHandlers[type]) {
        messageHandlers[type] = [];
    }
    // Thêm handler mới vào mảng
    messageHandlers[type].push(handlerFunction);
    console.log(`[Socket] Registered handler for type: ${type}. Total handlers: ${messageHandlers[type].length}`);
}

function handleMessage(msg) {
    // Nếu có mảng handler cho type này
    if (msg.type && messageHandlers[msg.type]) {
        console.log(`[Socket] Executing ${messageHandlers[msg.type].length} handlers for type: ${msg.type}`);
        // Gọi tất cả các handler trong mảng
        messageHandlers[msg.type].forEach(handler => {
            try {
                handler(msg);
            } catch (e) {
                console.error(`[Socket] Lỗi khi thực thi handler cho type ${msg.type}:`, e);
            }
        });
    } else {
        console.warn(`[Socket] Không tìm thấy hàm xử lý cho tin nhắn loại: ${msg.type}`);
    }
}
