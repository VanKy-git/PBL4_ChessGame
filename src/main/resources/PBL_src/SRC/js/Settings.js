document.addEventListener('DOMContentLoaded', () => {
    const settingsLink = document.getElementById('settingsLink');
    const settingsPopup = document.getElementById('settingsPopup');
    const saveSettingsBtn = document.getElementById('saveSettingsBtn');
    const cancelSettingsBtn = document.getElementById('cancelSettingsBtn');

    const lightSquareColorInput = document.getElementById('lightSquareColor');
    const darkSquareColorInput = document.getElementById('darkSquareColor');
    const boardThemeOptions = document.querySelector('.board-theme-options');

    const themes = {
        default: { '--white-square-bg': '#f0d9b5', '--black-square-bg': '#b58863' },
        green:   { '--white-square-bg': '#eeeed2', '--black-square-bg': '#769656' },
        blue:    { '--white-square-bg': '#dee3e6', '--black-square-bg': '#8ca2ad' },
        wood:    { '--white-square-bg': '#e8cba0', '--black-square-bg': '#a67c52' },
    };

    // 1. Mở/Đóng Popup
    if (settingsLink) {
        settingsLink.addEventListener('click', (e) => {
            e.preventDefault();
            loadSettingsToUI();
            settingsPopup.style.display = 'flex';
        });
    }

    if (cancelSettingsBtn) {
        cancelSettingsBtn.addEventListener('click', () => {
            settingsPopup.style.display = 'none';
            applySettings(getSavedSettings()); // Khôi phục cài đặt đã lưu
        });
    }

    if (saveSettingsBtn) {
        saveSettingsBtn.addEventListener('click', () => {
            const currentTheme = document.querySelector('.theme-option.selected')?.dataset.theme;
            const settingsToSave = currentTheme 
                ? { theme: currentTheme }
                : { 
                    '--white-square-bg': lightSquareColorInput.value,
                    '--black-square-bg': darkSquareColorInput.value
                  };
            
            saveSettings(settingsToSave);
            applySettings(settingsToSave);
            settingsPopup.style.display = 'none';
        });
    }

    // 2. Xử lý chọn Theme
    boardThemeOptions.addEventListener('click', (e) => {
        const target = e.target.closest('.theme-option');
        if (!target) return;

        // Bỏ chọn tất cả
        boardThemeOptions.querySelectorAll('.theme-option').forEach(opt => opt.classList.remove('selected'));
        
        // Chọn theme mới
        target.classList.add('selected');
        const themeName = target.dataset.theme;
        const themeColors = themes[themeName];

        // Áp dụng màu ngay lập tức và cập nhật color picker
        applySettings(themeColors);
        lightSquareColorInput.value = themeColors['--white-square-bg'];
        darkSquareColorInput.value = themeColors['--black-square-bg'];
    });


    // 3. Xử lý thay đổi màu thủ công
    lightSquareColorInput.addEventListener('input', () => {
        document.documentElement.style.setProperty('--white-square-bg', lightSquareColorInput.value);
        deselectAllThemes();
    });

    darkSquareColorInput.addEventListener('input', () => {
        document.documentElement.style.setProperty('--black-square-bg', darkSquareColorInput.value);
        deselectAllThemes();
    });

    function deselectAllThemes() {
        boardThemeOptions.querySelectorAll('.theme-option').forEach(opt => opt.classList.remove('selected'));
    }

    // 4. Logic Lưu và Tải cài đặt
    function saveSettings(settings) {
        localStorage.setItem('boardSettings', JSON.stringify(settings));
    }

    function getSavedSettings() {
        const saved = localStorage.getItem('boardSettings');
        if (saved) {
            const parsed = JSON.parse(saved);
            // Nếu lưu theo theme, trả về màu của theme đó
            if (parsed.theme && themes[parsed.theme]) {
                return themes[parsed.theme];
            }
            // Nếu lưu màu tùy chỉnh
            return parsed;
        }
        return themes.default; // Mặc định
    }

    function applySettings(settings) {
        const settingsToApply = settings.theme && themes[settings.theme] ? themes[settings.theme] : settings;
        for (const [key, value] of Object.entries(settingsToApply)) {
            if (key.startsWith('--')) {
                document.documentElement.style.setProperty(key, value);
            }
        }
    }

    function loadSettingsToUI() {
        const saved = JSON.parse(localStorage.getItem('boardSettings') || '{}');
        const settings = getSavedSettings();
        
        lightSquareColorInput.value = settings['--white-square-bg'];
        darkSquareColorInput.value = settings['--black-square-bg'];

        deselectAllThemes();
        if (saved.theme) {
            const selectedThemeEl = boardThemeOptions.querySelector(`[data-theme="${saved.theme}"]`);
            if (selectedThemeEl) {
                selectedThemeEl.classList.add('selected');
            }
        }
    }

    // 5. Áp dụng cài đặt đã lưu khi tải trang
    applySettings(getSavedSettings());
});
