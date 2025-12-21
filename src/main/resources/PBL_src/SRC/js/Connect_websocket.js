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
            // Xử lý ping/pong trước khi log và handle
            if (msg.type === 'ping') {
                sendMessage({ type: 'pong' });
                return; // Không cần xử lý thêm
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
        if (messageObject.type !== 'pong') { // Không log pong để tránh nhiễu
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
    if (messageHandlers[type]) {
        console.warn(`[Socket] Ghi đè handler cho type: ${type}`);
    }
    messageHandlers[type] = handlerFunction;
}

function handleMessage(msg) {
    if (msg.type && messageHandlers[msg.type]) {
        messageHandlers[msg.type](msg);
    } else {
        console.warn(`[Socket] Không tìm thấy hàm xử lý cho tin nhắn loại: ${msg.type}`);
    }
}
