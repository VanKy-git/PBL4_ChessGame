document.addEventListener('DOMContentLoaded', () => {
    const leaderboardLink = document.getElementById('leaderboardLink');
    const leaderboardPopup = document.getElementById('leaderboardPopup');
    const leaderboardClose = document.getElementById('leaderboardClose');
    const leaderboardContainer = document.getElementById('leaderboardContainer');
    const API_URL = 'http://localhost:8910';

    leaderboardLink.addEventListener('click', async (e) => {
        e.preventDefault();
        leaderboardPopup.style.display = 'flex';
        await fetchLeaderboard();
    });

    leaderboardClose.addEventListener('click', () => {
        leaderboardPopup.style.display = 'none';
    });

    async function fetchLeaderboard() {
        leaderboardContainer.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_URL}/api/leaderboard`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            const result = await response.json();
            if (result.success) {
                renderLeaderboard(result.data);
            } else {
                leaderboardContainer.innerHTML = `<p class="error-message">Lỗi: ${result.error}</p>`;
            }
        } catch (error) {
            leaderboardContainer.innerHTML = `<p class="error-message">Không thể tải bảng xếp hạng. Vui lòng thử lại sau.</p>`;
            console.error('Error fetching leaderboard:', error);
        }
    }

    function renderLeaderboard(users) {
        if (!users || users.length === 0) {
            leaderboardContainer.innerHTML = '<p>Chưa có ai trên bảng xếp hạng.</p>';
            return;
        }

        let tableHtml = `
            <table class="leaderboard-table">
                <thead>
                    <tr>
                        <th>Hạng</th>
                        <th>Người chơi</th>
                        <th>Elo</th>
                    </tr>
                </thead>
                <tbody>
        `;

        users.forEach((user, index) => {
            const rank = index + 1;
            let rankClass = '';
            if (rank === 1) rankClass = 'rank-gold';
            if (rank === 2) rankClass = 'rank-silver';
            if (rank === 3) rankClass = 'rank-bronze';

            tableHtml += `
                <tr>
                    <td class="rank-cell ${rankClass}">${rank}</td>
                    <td class="player-cell">
                        <img src="${user.avatarUrl || '../../PBL4_imgs/icon/logo.png'}" alt="avatar" class="player-avatar-small">
                        <span>${user.userName}</span>
                    </td>
                    <td class="elo-cell">${user.eloRating}</td>
                </tr>
            `;
        });

        tableHtml += `
                </tbody>
            </table>
        `;

        leaderboardContainer.innerHTML = tableHtml;
    }
});
