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

window.onclick = function () {
    const menu = document.getElementById('model-menu');
    if (menu.classList.contains('menu-show')) {
        menu.classList.add('menu-hide');
        menu.classList.remove('menu-show');
    }
};

function toggleDarkMode() {
    const shell = document.getElementById('app-shell');
    const icon = document.getElementById('theme-toggle-icon');
    if (!shell || !icon) return;
    const next = !shell.classList.contains('dark');
    applyDarkMode(next);

    if (next) {
        icon.classList.remove('fa-toggle-off');
        icon.classList.add('fa-toggle-on');
    } else {
        icon.classList.remove('fa-toggle-on');
        icon.classList.add('fa-toggle-off');
    }

    if (typeof setStatusBarColorForTheme === 'function') {
        setStatusBarColorForTheme(next);
    }
}

/** 语言弹窗候选项（仅前端 UI；持久 key：cogno-ui-language） */
const LANGUAGE_OPTIONS = [
    { id: 'zh-Hans', label: '简体中文' },
    { id: 'zh-Hant', label: '繁體中文' },
    { id: 'en', label: 'English' },
    { id: 'ja', label: '日本語' },
    { id: 'ko', label: '한국어' },
    { id: 'fr', label: 'Français' },
    { id: 'de', label: 'Deutsch' },
    { id: 'es', label: 'Español' }
];

let languageTempSelectionId = 'zh-Hans';

function getSavedLanguageId() {
    return localStorage.getItem('cogno-ui-language') || 'zh-Hans';
}

function labelForLanguageId(id) {
    const o = LANGUAGE_OPTIONS.find((x) => x.id === id);
    return o ? o.label : '简体中文';
}

function renderLanguageOptionList() {
    const host = document.getElementById('language-option-list');
    if (!host) return;
    host.innerHTML = '';
    LANGUAGE_OPTIONS.forEach((opt) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.setAttribute('data-lang-id', opt.id);
        const selected = languageTempSelectionId === opt.id;
        btn.className =
            'language-option-btn w-full rounded-xl px-3 py-3 text-left text-[15px] font-medium ' +
            (selected
                ? 'bg-cogno-primary/12 text-cogno-primary dark:bg-cogno-darkPrimary/15 dark:text-cogno-darkPrimary'
                : 'text-cogno-text dark:text-cogno-darkText hover:bg-cogno-bg dark:hover:bg-zinc-800');
        btn.textContent = opt.label;
        btn.addEventListener('click', () => {
            languageTempSelectionId = opt.id;
            renderLanguageOptionList();
        });
        host.appendChild(btn);
    });
}

function openLanguageSheet() {
    const sheet = document.getElementById('language-sheet');
    if (!sheet) return;
    languageTempSelectionId = getSavedLanguageId();
    renderLanguageOptionList();
    sheet.classList.add('is-open');
    sheet.setAttribute('aria-hidden', 'false');
}

function closeLanguageSheet() {
    const sheet = document.getElementById('language-sheet');
    if (!sheet) return;
    sheet.classList.remove('is-open');
    sheet.setAttribute('aria-hidden', 'true');
}

function confirmLanguageSheet() {
    localStorage.setItem('cogno-ui-language', languageTempSelectionId);
    const lab = document.getElementById('settings-language-label');
    if (lab) lab.textContent = labelForLanguageId(languageTempSelectionId);
    closeLanguageSheet();
}

function cancelLanguageSheet() {
    languageTempSelectionId = getSavedLanguageId();
    closeLanguageSheet();
}

/** 「清理本地缓存」：短暂底色反馈后立即恢复（无 btn-tap、无 sticky :hover） */
function flashClearCacheRow() {
    const row = document.getElementById('settings-cache-clear-row');
    if (!row) return;
    row.classList.add('settings-cache-flash-active');
    setTimeout(() => row.classList.remove('settings-cache-flash-active'), 160);
}

function initLanguageSheet() {
    const sheet = document.getElementById('language-sheet');
    const panel = document.getElementById('language-sheet-panel');
    const cancel = document.getElementById('language-sheet-cancel');
    const confirm = document.getElementById('language-sheet-confirm');
    if (!sheet || !panel || !cancel || !confirm) return;
    cancel.addEventListener('click', (e) => {
        e.stopPropagation();
        cancelLanguageSheet();
    });
    confirm.addEventListener('click', (e) => {
        e.stopPropagation();
        confirmLanguageSheet();
    });
    sheet.addEventListener('click', (e) => {
        if (e.target === sheet) cancelLanguageSheet();
    });
    panel.addEventListener('click', (e) => e.stopPropagation());
}

function syncLanguageLabelFromStorage() {
    const lab = document.getElementById('settings-language-label');
    if (lab) lab.textContent = labelForLanguageId(getSavedLanguageId());
    languageTempSelectionId = getSavedLanguageId();
}

document.addEventListener('DOMContentLoaded', () => {
    syncLanguageLabelFromStorage();
    initLanguageSheet();
    // 主题已由 common.js 的 loadTheme / applyDarkMode 应用到 app-shell 与 documentElement；此处仅同步开关图标
    if (localStorage.getItem('cogno-dark-mode') === 'true') {
        const icon = document.getElementById('theme-toggle-icon');
        if (icon) {
            icon.classList.remove('fa-toggle-off');
            icon.classList.add('fa-toggle-on');
        }
    }
});
