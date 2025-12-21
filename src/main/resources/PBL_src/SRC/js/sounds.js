// --- SOUND MANAGEMENT ---
const sounds = {
    move: new Audio('../sounds/move.mp3'),
    capture: new Audio('../sounds/capture.mp3'),
    check: new Audio('../sounds/check.mp3'),
    gameEnd: new Audio('../sounds/game-end.mp3')
};

let isSoundEnabled = true;
let volume = 0.5; // 50% volume

export function playSound(soundName) {
    if (!isSoundEnabled || !sounds[soundName]) return;
    
    const sound = sounds[soundName];
    sound.volume = volume;
    sound.currentTime = 0; // Play from the start
    sound.play().catch(e => console.error("Sound play failed:", e));
}

export function setVolume(newVolume) {
    volume = Math.max(0, Math.min(1, newVolume)); // Clamp between 0 and 1
    console.log(`Volume set to ${Math.round(volume * 100)}%`);
}

export function enableSound(enable) {
    isSoundEnabled = enable;
}

// --- INITIALIZE FROM SETTINGS ---
document.addEventListener('DOMContentLoaded', () => {
    const savedSettings = localStorage.getItem('chessGameSettings');
    if (savedSettings) {
        const settings = JSON.parse(savedSettings);
        setVolume(settings.sound / 100);
    }
});