// ====== LẤY CÁC PHẦN TỬ TRONG DOM ======
const exitRoomBtn = document.getElementById("exitroom-btn");
const drawBtn = document.getElementById("draw-btn");
const resignBtn = document.getElementById("resign-btn");

// Popup thoát phòng
const exitConfirm = document.getElementById("exitConfirm");
const stayBtn = document.getElementById("stay-btn");
const exitConfirmBtn = document.getElementById("exitconfirm-btn");

// ====== ẨN POPUP KHI VỪA TẢI TRANG ======
exitConfirm.classList.add("modal-hidden");

// ====== SỰ KIỆN NÚT THOÁT PHÒNG ======
exitRoomBtn.addEventListener("click", () => {
    exitConfirm.classList.remove("modal-hidden");
    exitConfirm.classList.add("modal-show");
});

// ====== SỰ KIỆN NÚT Ở LẠI ======
stayBtn.addEventListener("click", () => {
    exitConfirm.classList.remove("modal-show");
    exitConfirm.classList.add("modal-hidden");
});

exitConfirmBtn.addEventListener("click", () => {
    console.log("Người chơi đã thoát phòng!");
    // TODO: Gửi socket báo thoát hoặc chuyển trang
     window.location.href = "Home_page.html"; // Chuyển về trang HomePage
});

// ====== SỰ KIỆN NÚT CẦU HÒA ======
drawBtn.addEventListener("click", () => {
    console.log("Người chơi yêu cầu cầu hòa");
    // TODO: Gửi socket yêu cầu hòa, hiển thị thông báo cho đối thủ
});

// ====== SỰ KIỆN NÚT XIN THUA ======
resignBtn.addEventListener("click", () => {
    console.log("Người chơi xin thua!");
    // TODO: Gửi socket báo thua, hiện thông báo kết thúc trận
});
