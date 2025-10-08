let opponentTime = 600;
let selfTime = 600;

let isSelfTurn = true;

const opponentTimerEl = document.getElementById('opponent-timer');
const selfTimerEl = document.getElementById('self-timer');

function formatTime(seconds) {
  const m = Math.floor(seconds / 60).toString().padStart(2, '0');
  const s = (seconds % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

function updateTimers() {
  if (isSelfTurn) {
    selfTime--;
  } else {
    opponentTime--;
  }
  if (selfTime < 0 || opponentTime < 0) {
    clearInterval(timerInterval);
    alert(isSelfTurn ? "Bạn đã hết thời gian! Bạn thua." : "Đối thủ hết thời gian! Bạn thắng.");
    return;
  }
  opponentTimerEl.textContent = formatTime(opponentTime);
  selfTimerEl.textContent = formatTime(selfTime);
}

const timerInterval = setInterval(updateTimers, 1000);

function switchTurn() {
  isSelfTurn = !isSelfTurn;
}

document.getElementById('resign-btn').addEventListener('click', () => {
  if (confirm("Bạn chắc chắn muốn xin thua?")) {
    alert("Bạn đã xin thua.");
    clearInterval(timerInterval);
  }
});

document.getElementById('draw-btn').addEventListener('click', () => {
  if (confirm("Bạn muốn đề nghị hòa?")) {
    alert("Đề nghị hòa đã được gửi đến đối thủ.");
  }
});
