import { setVolume } from './sounds.js';

const settingLink = document.getElementById('settingLink');
const settingPopup = document.getElementById('settingPopup');
const settingContainer = document.getElementById('settingContainer');
const settingClose = document.getElementById('settingClose');

// --- CÁC HÀM ÁP DỤNG CÀI ĐẶT ---

function applyBoardTheme(theme) {
    const board = document.getElementById('chessBoard');
    if (!board) return;
    
    // Xóa các class theme cũ
    board.classList.remove('theme-wood', 'theme-dark', 'theme-light');

    if (theme !== 'default') {
        board.classList.add(`theme-${theme}`);
    }
    console.log(`Applied theme: ${theme}`);
}

function applySoundVolume(volume) {
    setVolume(volume / 100);
}

// --- LƯU & TẢI CÀI ĐẶT TỪ LOCALSTORAGE ---

function saveSettings() {
    const theme = document.getElementById('theme-select').value;
    const sound = document.getElementById('sound-slider').value;
    const language = document.getElementById('language-select').value;

    const settings = { theme, sound, language };
    localStorage.setItem('chessGameSettings', JSON.stringify(settings));

    // Áp dụng ngay
    applyBoardTheme(theme);
    applySoundVolume(sound);

    alert('Đã lưu cài đặt!');
}

function loadSettings() {
    const savedSettings = localStorage.getItem('chessGameSettings');
    if (savedSettings) {
        const settings = JSON.parse(savedSettings);
        
        // Áp dụng cài đặt đã lưu
        applyBoardTheme(settings.theme);
        applySoundVolume(settings.sound);

        // Cập nhật UI trong popup (nếu popup đang mở)
        const themeSelect = document.getElementById('theme-select');
        const soundSlider = document.getElementById('sound-slider');
        const languageSelect = document.getElementById('language-select');
        const soundValue = document.getElementById('sound-value');

        if (themeSelect) themeSelect.value = settings.theme;
        if (soundSlider) soundSlider.value = settings.sound;
        if (languageSelect) languageSelect.value = settings.language;
        if (soundValue) soundValue.textContent = settings.sound;
    }
}

// --- XỬ LÝ POPUP ---

async function loadSettingContent() {
    try {
        const response = await fetch('Setting.html');
        if (!response.ok) {
            throw new Error('Failed to load settings content');
        }
        const content = await response.text();
        settingContainer.innerHTML = content;

        // Sau khi tải xong, gán listener và load giá trị đã lưu
        document.getElementById('save-settings-btn').addEventListener('click', saveSettings);
        
        const soundSlider = document.getElementById('sound-slider');
        const soundValue = document.getElementById('sound-value');
        soundSlider.addEventListener('input', () => {
            soundValue.textContent = soundSlider.value;
            applySoundVolume(soundSlider.value); // Cập nhật âm thanh ngay khi kéo
        });

        loadSettings(); // Tải giá trị vào các control

    } catch (error) {
        console.error('[Setting.js] Error loading content:', error);
        settingContainer.innerHTML = '<p style="color: #e74c3c;">Không thể tải nội dung cài đặt.</p>';
    }
}

if (settingLink) {
    settingLink.addEventListener('click', (e) => {
        e.preventDefault();
        settingPopup.style.display = 'flex';
        settingContainer.innerHTML = 'Đang tải...';
        loadSettingContent();
    });

    settingClose.addEventListener('click', () => {
        settingPopup.style.display = 'none';
    });
}

// Tải cài đặt ngay khi trang được load lần đầu
document.addEventListener('DOMContentLoaded', loadSettings);