// 侧边栏相关（仅 index 页面存在相关元素，不报错）
const menuBtn = document.getElementById('menu-btn');
const sidebar = document.getElementById('sidebar-drawer');
const overlay = document.getElementById('sidebar-overlay');
const contextMenu = document.getElementById('context-menu');

function openSidebar() {
    if (!sidebar || !overlay) return;
    sidebar.classList.remove('-translate-x-full', 'invisible', 'opacity-0');
    sidebar.classList.add('opacity-100');
    overlay.classList.remove('opacity-0', 'pointer-events-none');
    overlay.classList.add('opacity-100');
}

function closeSidebar() {
    if (!sidebar || !overlay) return;
    sidebar.classList.add('-translate-x-full', 'opacity-0');
    setTimeout(() => {
        if (sidebar.classList.contains('-translate-x-full')) {
            sidebar.classList.add('invisible');
        }
    }, 300);
    overlay.classList.remove('opacity-100');
    overlay.classList.add('opacity-0', 'pointer-events-none');
    hideContextMenu();
}

function showContextMenu(e, element) {
    if (!contextMenu) return;
    e.preventDefault();
    e.stopPropagation();
    const x = e.clientX;
    const y = e.clientY;
    contextMenu.style.left = `${x}px`;
    contextMenu.style.top = `${y}px`;
    contextMenu.classList.remove('hidden');
    document.querySelectorAll('.history-item').forEach(el => el.classList.remove('bg-cogno-bg'));
    element.classList.add('bg-cogno-bg');
}

function hideContextMenu() {
    if (contextMenu) contextMenu.classList.add('hidden');
}

if (menuBtn) menuBtn.addEventListener('click', openSidebar);
if (overlay) overlay.addEventListener('click', closeSidebar);
document.addEventListener('click', hideContextMenu);
if (sidebar) sidebar.addEventListener('scroll', hideContextMenu);

// 语音遮罩（首页）
function toggleVoice() {
    const overlayVoice = document.getElementById('voice-overlay');
    if (!overlayVoice) return;
    const isHidden = overlayVoice.classList.contains('opacity-0');
    if (isHidden) {
        overlayVoice.classList.remove('opacity-0', 'pointer-events-none');
        overlayVoice.classList.add('opacity-100');
    } else {
        overlayVoice.classList.remove('opacity-100');
        overlayVoice.classList.add('opacity-0', 'pointer-events-none');
    }
}

// 深色模式（设置页和全局初始加载）
function applyDarkMode(isDark) {
    const shell = document.getElementById('app-shell') || document.querySelector('.phone-shell');
    if (!shell) return;
    if (isDark) shell.classList.add('dark');
    else shell.classList.remove('dark');
    localStorage.setItem('cogno-dark-mode', isDark);
}

// 修改原有的 loadTheme 函数，在应用主题后同步状态栏颜色
function loadTheme() {
    const isDark = localStorage.getItem('cogno-dark-mode') === 'true';
    applyDarkMode(isDark);
    // 新增：根据深色模式设置状态栏文字颜色
    setStatusBarColorForTheme(isDark);
}
document.addEventListener('DOMContentLoaded', loadTheme);

// 调用原生方法设置状态栏图标颜色（根据是否是深色模式）
function setStatusBarColorForTheme(isDarkMode) {
    if (window.Android && window.Android.setStatusBarDarkMode) {
        window.Android.setStatusBarDarkMode(isDarkMode);
    }
}