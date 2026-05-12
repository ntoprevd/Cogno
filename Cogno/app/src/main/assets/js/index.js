const randomQuotes = [
    "Stay hungry, stay foolish. 🍎\n-- Steve Jobs",
    "海内存知己，天涯若比邻。🌏\nAI 亦是你的知音。",
    "此时相望不相闻，愿逐月华流照君。🌙",
    "The journey of a thousand miles begins with a single step. 👣",
    "万物皆有裂痕，那是光照进来的地方。✨",
    "每一个不曾起舞的日子，都是对生命的辜负。💃",
    "Be kind, for everyone you meet is fighting a hard battle. 🌈"
];

const cuteEmojis = ["✨", "🧸", "🌈", "🍭", "🐙", "🚀", "🎨", "🍀", "🐱", "🐥", "🌙"];
let typewriterTimeout;

function runWelcomeLoop() {
    const textElement = document.getElementById('typewriter-text');
    const welcomeView = document.getElementById('welcome-view');
    if (welcomeView.style.display === 'none') return;
    const quote = randomQuotes[Math.floor(Math.random() * randomQuotes.length)];
    textElement.innerHTML = '<span id="typing-content"></span><span class="cursor-blink"></span>';
    const contentSpan = document.getElementById('typing-content');
    let index = 0;
    function type() {
        if (welcomeView.style.display === 'none') return;
        if (index < quote.length) {
            const char = quote.charAt(index);
            if (char === '\n') contentSpan.innerHTML += '<br>';
            else contentSpan.innerHTML += char;
            index++;
            typewriterTimeout = setTimeout(type, 120 + Math.random() * 130);
        } else {
            typewriterTimeout = setTimeout(runWelcomeLoop, 5000);
        }
    }
    setTimeout(type, 1000);
}

function startEmojiSwitch() {
    const emojiElement = document.getElementById('bouncing-emoji');
    const welcomeView = document.getElementById('welcome-view');
    setInterval(() => {
        if (welcomeView.style.display !== 'none') {
            setTimeout(() => {
                const randomEmoji = cuteEmojis[Math.floor(Math.random() * cuteEmojis.length)];
                emojiElement.style.transform = 'scale(0.8)';
                setTimeout(() => {
                    emojiElement.innerText = randomEmoji;
                    emojiElement.style.transform = 'scale(1)';
                }, 100);
            }, 1200);
        }
    }, 2400);
}

document.addEventListener('DOMContentLoaded', () => {
    runWelcomeLoop();
    startEmojiSwitch();
    const sendBtn = document.querySelector('button.bg-cogno-primary');
    if (sendBtn) {
        sendBtn.addEventListener('click', () => {
            const welcomeView = document.getElementById('welcome-view');
            const messageList = document.getElementById('message-list');
            clearTimeout(typewriterTimeout);
            welcomeView.classList.add('welcome-fade-out');
            setTimeout(() => {
                welcomeView.style.display = 'none';
                messageList.classList.remove('hidden');
            }, 800);
        });
    }
});