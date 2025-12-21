import { setVolume } from './sounds.js';

const settingLink = document.getElementById('settingLink');
const settingPopup = document.getElementById('settingPopup');
const settingContainer = document.getElementById('settingContainer');
const settingClose = document.getElementById('settingClose');

// --- CÁC HÀM ÁP DỤNG CÀI ĐẶT (chạy độc lập) ---

function applyBoardTheme(theme) {
    const board = document.getElementById('chessBoard');
    if (!board) return;
    
    // Xóa tất cả các class theme cũ
    const themes = ['wood', 'dark', 'light', 'ocean', 'forest', 'cherry'];
    themes.forEach(t => board.classList.remove(`theme-${t}`));

    if (theme && theme !== 'default') {
        board.classList.add(`theme-${theme}`);
    }
    console.log(`Applied theme: ${theme}`);
}

function applySoundVolume(volume) {
    setVolume(Number(volume) / 100);
}

// --- HÀM TẢI VÀ ÁP DỤNG CÀI ĐẶT (Định nghĩa ở top-level) ---
function loadAndApplySavedSettings() {
    const savedSettings = localStorage.getItem('chessGameSettings');
    if (savedSettings) {
        const settings = JSON.parse(savedSettings);
        applyBoardTheme(settings.theme || 'default');
        applySoundVolume(settings.sound || 50);
    }
}

// --- LOGIC CHÍNH ---

// 1. Tải và áp dụng cài đặt đã lưu khi trang load lần đầu
document.addEventListener('DOMContentLoaded', loadAndApplySavedSettings);

// 2. Xử lý khi mở popup
async function openAndPrepareSettingsPopup() {
    settingPopup.style.display = 'flex';
    settingContainer.innerHTML = 'Đang tải...';

    try {
        const response = await fetch('Setting.html');
        if (!response.ok) throw new Error('Failed to load settings content');
        
        const content = await response.text();
        settingContainer.innerHTML = content;

        // --- Gắn listener và xử lý logic BÊN TRONG hàm này ---
        const saveBtn = document.getElementById('save-settings-btn');
        const themeSelect = document.getElementById('theme-select');
        const soundSlider = document.getElementById('sound-slider');
        const soundValue = document.getElementById('sound-value');

        // Lấy cài đặt đã lưu và cập nhật UI của popup
        const savedSettings = JSON.parse(localStorage.getItem('chessGameSettings') || '{}');
        if (themeSelect) themeSelect.value = savedSettings.theme || 'default';
        if (soundSlider) soundSlider.value = savedSettings.sound || 50;
        if (soundValue) soundValue.textContent = savedSettings.sound || 50;

        // Listener cho nút Lưu
        if (saveBtn) {
            saveBtn.addEventListener('click', () => {
                const settingsToSave = {
                    theme: themeSelect.value,
                    sound: soundSlider.value
                };
                localStorage.setItem('chessGameSettings', JSON.stringify(settingsToSave));

                // Phản hồi trực quan
                saveBtn.textContent = 'Đã lưu!';
                saveBtn.style.backgroundColor = '#5a9a34';
                setTimeout(() => {
                    saveBtn.textContent = 'Lưu thay đổi';
                    saveBtn.style.backgroundColor = '#81b64c';
                }, 1500);
            });
        }
        
        // Listener cho thanh trượt âm thanh
        if (soundSlider) {
            soundSlider.addEventListener('input', () => {
                if (soundValue) soundValue.textContent = soundSlider.value;
                applySoundVolume(soundSlider.value); // Áp dụng ngay khi kéo
            });
        }
        
        // Listener cho lựa chọn theme
        if (themeSelect) {
            themeSelect.addEventListener('change', () => {
                applyBoardTheme(themeSelect.value); // Áp dụng ngay khi chọn
            });
        }

    } catch (error) {
        console.error('[Setting.js] Error loading content:', error);
        settingContainer.innerHTML = '<p style="color: #e74c3c;">Không thể tải nội dung cài đặt.</p>';
    }
}

// 3. Gắn sự kiện cho các nút chính
if (settingLink) {
    settingLink.addEventListener('click', (e) => {
        e.preventDefault();
        openAndPrepareSettingsPopup();
    });
}

if (settingClose) {
    settingClose.addEventListener('click', () => {
        settingPopup.style.display = 'none';
        // Khi đóng popup, tải lại cài đặt đã lưu để hủy các thay đổi chưa lưu
        loadAndApplySavedSettings(); 
    });
}