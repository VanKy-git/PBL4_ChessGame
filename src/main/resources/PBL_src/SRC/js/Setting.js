// File: Setting.js

// Các biến cấu hình mặc định
const DEFAULT_THEME = 'classic';

// Các theme màu sắc (CSS variables hoặc class)
const THEMES = {
    classic: { white: '#f0d9b5', black: '#b58863' },
    green: { white: '#eeeed2', black: '#769656' },
    blue: { white: '#dee3e6', black: '#8ca2ad' },
    wood: { white: '#e8c39e', black: '#a67c52' }
};

// Hàm áp dụng theme
export function applyTheme(themeName) {
    const theme = THEMES[themeName] || THEMES.classic;
    const root = document.documentElement;
    
    // Cập nhật biến CSS toàn cục (nếu dùng CSS variables)
    root.style.setProperty('--white-square', theme.white);
    root.style.setProperty('--black-square', theme.black);
    
    // Cập nhật trực tiếp style cho các ô cờ
    let styleEl = document.getElementById('dynamic-theme-style');
    if (!styleEl) {
        styleEl = document.createElement('style');
        styleEl.id = 'dynamic-theme-style';
        document.head.appendChild(styleEl);
    }
    // Sử dụng !important để ghi đè CSS mặc định
    styleEl.textContent = `
        .white { background-color: ${theme.white} !important; }
        .black { background-color: ${theme.black} !important; }
        .white .coordinate { color: ${theme.black} !important; }
        .black .coordinate { color: ${theme.white} !important; }
    `;
    
    localStorage.setItem('chessTheme', themeName);
}

// Hàm load giao diện cài đặt
export async function loadSettings() {
    const settingPopup = document.getElementById('settingPopup');
    const settingContainer = document.getElementById('settingContainer');
    const settingCloseBtn = document.getElementById('settingClose');

    if (!settingPopup || !settingContainer) {
        console.error("Không tìm thấy popup cài đặt!");
        return;
    }
    
    try {
        // Load nội dung từ file HTML nếu chưa load
        if (!settingContainer.innerHTML || settingContainer.innerHTML === 'Đang tải...') {
            const response = await fetch('Setting.html');
            if (!response.ok) throw new Error('Cannot load Setting.html');
            const html = await response.text();
            settingContainer.innerHTML = html;
            
            // Load CSS
            if (!document.querySelector('link[href*="../css/Setting.css"]')) {
                const link = document.createElement('link');
                link.rel = 'stylesheet';
                link.href = '../css/Setting.css';
                document.head.appendChild(link);
            }
        }
        
        // Hiển thị popup
        settingPopup.style.display = 'flex';
        
        // Khởi tạo sự kiện
        initSettingsEvents(settingContainer);
        
        // Sự kiện đóng popup
        if (settingCloseBtn) {
            // Xóa listener cũ để tránh trùng lặp
            const newCloseBtn = settingCloseBtn.cloneNode(true);
            settingCloseBtn.parentNode.replaceChild(newCloseBtn, settingCloseBtn);
            
            newCloseBtn.addEventListener('click', () => {
                settingPopup.style.display = 'none';
            });
        }
        
    } catch (error) {
        console.error('Error loading settings:', error);
        settingContainer.innerHTML = '<p>Lỗi tải cài đặt.</p>';
    }
}

// Khởi tạo các sự kiện trong trang cài đặt
function initSettingsEvents(container) {
    // 1. Xử lý chọn Theme
    const themeOptions = container.querySelectorAll('.theme-option');
    const currentTheme = localStorage.getItem('chessTheme') || DEFAULT_THEME;
    
    themeOptions.forEach(option => {
        // Reset trạng thái selected
        option.classList.remove('selected');
        if (option.dataset.theme === currentTheme) {
            option.classList.add('selected');
        }
        
        // Clone node để xóa listener cũ nếu có
        const newOption = option.cloneNode(true);
        option.parentNode.replaceChild(newOption, option);
        
        newOption.addEventListener('click', () => {
            // Xóa selected ở các option khác
            container.querySelectorAll('.theme-option').forEach(opt => opt.classList.remove('selected'));
            newOption.classList.add('selected');
            applyTheme(newOption.dataset.theme);
        });
    });
}

// Gắn sự kiện vào menu Cài đặt
document.addEventListener('DOMContentLoaded', () => {
    const settingLink = document.getElementById('settingLink');
    
    if (settingLink) {
        settingLink.addEventListener('click', (e) => {
            e.preventDefault();
            loadSettings();
        });
    } else {
        // Fallback nếu chưa có ID
        const links = document.querySelectorAll('nav.menu a');
        links.forEach(link => {
            if (link.textContent.includes('Cài đặt')) {
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                    loadSettings();
                });
            }
        });
    }
});