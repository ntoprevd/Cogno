function toggleModelMenu(e) {
    e.stopPropagation();
    const menu = document.getElementById('model-menu');
    menu.classList.toggle('menu-show');
    menu.classList.toggle('menu-hide');
}

function selectModel(name) {
    document.getElementById('current-model-name').innerText = name;
    const panel = document.getElementById('custom-api-panel');
    
    // 关键：选择“自定义 API”时显示面板，否则隐藏
    if (name === '自定义 API') {
        panel.classList.add('show');
    } else {
        panel.classList.remove('show');
    }
    
    // 关闭菜单
    const menu = document.getElementById('model-menu');
    menu.classList.add('menu-hide');
    menu.classList.remove('menu-show');
}

window.onclick = function() {
    const menu = document.getElementById('model-menu');
    if (menu.classList.contains('menu-show')) {
        menu.classList.add('menu-hide');
        menu.classList.remove('menu-show');
    }
};

function toggleDarkMode() {
    const shell = document.getElementById('app-shell');
    const icon = document.getElementById('theme-toggle-icon');
    shell.classList.toggle('dark');
    const isDark = shell.classList.contains('dark');
    localStorage.setItem('cogno-dark-mode', isDark);

    // 更新开关图标
    if (isDark) {
        icon.classList.remove('fa-toggle-off');
        icon.classList.add('fa-toggle-on');
    } else {
        icon.classList.remove('fa-toggle-on');
        icon.classList.add('fa-toggle-off');
    }

    // 关键：通知原生层改变状态栏文字颜色
    if (typeof setStatusBarColorForTheme === 'function') {
        setStatusBarColorForTheme(isDark);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (localStorage.getItem('cogno-dark-mode') === 'true') {
        document.getElementById('app-shell').classList.add('dark');
        const icon = document.getElementById('theme-toggle-icon');
        if (icon) {
            icon.classList.remove('fa-toggle-off');
            icon.classList.add('fa-toggle-on');
        }
    }
});