// 侧边栏相关（仅 index 页面存在相关元素，不报错）
const menuBtn = document.getElementById('menu-btn');
const sidebar = document.getElementById('sidebar-drawer');
const overlay = document.getElementById('sidebar-overlay');
const contextMenu = document.getElementById('context-menu');

/** 当前长按/右键打开菜单所对应的对话行（重命名、置顶、删除共用） */
let contextMenuTargetItem = null;
/** 菜单刚打开的时间戳：用于忽略手指抬起触发的「伪点击」，避免菜单瞬间被关掉 */
let contextMenuOpenedAt = 0;
/** 重命名弹窗打开时锁定的行（关闭菜单后仍保留引用） */
let renameTargetItem = null;

const LONG_PRESS_MS = 520;
let pressTimer = null;
let pressStartEl = null;

function openSidebar() {
    ensureSidebarPanInit();
    setPanElementsTransition(true);
    applyDrawerProgress(1);
    window.setTimeout(() => setPanElementsTransition(false), 340);
}

function closeSidebar() {
    ensureSidebarPanInit();
    setPanElementsTransition(true);
    applyDrawerProgress(0);
    window.setTimeout(() => {
        setPanElementsTransition(false);
        hideContextMenu();
    }, 340);
}

/** 侧栏是否视为「打开」（与 _drawerProgress 一致，供长按菜单等逻辑使用） */
function isSidebarOpen() {
    return _drawerProgress > 0.99;
}

/** 抽屉开合进度 0=全关 1=全开（仅 index：存在 #sidebar-drawer 时初始化） */
let _drawerProgress = 0;
let _sidebarPanListenersBound = false;
let _mainChromeEls = [];

function getSidebarDrawerWidthPx() {
    if (!sidebar) return window.innerWidth * 0.85;
    return sidebar.offsetWidth || window.innerWidth * 0.85;
}

function setPanElementsTransition(enabled) {
    const curve = 'transform 0.32s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.32s cubic-bezier(0.32, 0.72, 0, 1)';
    const t = enabled ? curve : 'none';
    if (sidebar) sidebar.style.transition = t;
    if (overlay) overlay.style.transition = t;
    _mainChromeEls.forEach((el) => {
        el.style.transition = t;
    });
}

/**
 * 根据进度 p 同步抽屉、主壳（顶栏+聊天+底栏）与遮罩；全程用 transform/opacity，避免跟手时触发布局抖动。
 * 不修改侧栏 DOM 结构，仅运行时改 style / class。
 */
function applyDrawerProgress(p) {
    if (!sidebar || !overlay) return;
    const width = getSidebarDrawerWidthPx();
    const clamped = Math.max(0, Math.min(1, p));
    _drawerProgress = clamped;

    const translateDrawer = -(1 - clamped) * width;
    sidebar.style.transform = `translate3d(${translateDrawer}px,0,0)`;

    const translateMain = clamped * width;
    _mainChromeEls.forEach((el) => {
        el.style.transform = `translate3d(${translateMain}px,0,0)`;
    });

    overlay.style.opacity = String(0.45 * clamped);
    overlay.style.pointerEvents = clamped > 0.03 ? 'auto' : 'none';

    if (clamped > 0.001) {
        sidebar.classList.remove('invisible', 'opacity-0');
        sidebar.classList.add('opacity-100');
    } else {
        sidebar.classList.add('invisible', 'opacity-0');
        sidebar.classList.remove('opacity-100');
    }
}

/**
 * ChatGPT 式跟手侧栏：在 .phone-shell 上监听 touch，主界面与抽屉位移与手指水平位移成正比；
 * 松手按行程（屏宽约 30%）与速度阈值决定吸附开/关；松手后 cubic-bezier 与 setPanElementsTransition 一致。
 */
function ensureSidebarPanInit() {
    if (_sidebarPanListenersBound || !sidebar || !overlay) return;
    const shell = document.querySelector('.phone-shell');
    if (!shell) return;

    _sidebarPanListenersBound = true;

    sidebar.classList.remove('-translate-x-full', 'transition-all', 'duration-300');
    overlay.classList.remove('transition-opacity', 'duration-300', 'opacity-0', 'opacity-100');

    const hdr = shell.querySelector(':scope > header');
    const mainEl = document.getElementById('chat-container');
    const ft = shell.querySelector(':scope > footer');
    _mainChromeEls = [hdr, mainEl, ft].filter(Boolean);
    _mainChromeEls.forEach((el) => {
        el.style.willChange = 'transform';
    });
    sidebar.style.willChange = 'transform';

    applyDrawerProgress(0);

    const OPEN_FRAC = 0.3; // 最小打开/关闭行程：屏宽的 30%（与任务说明一致）
    const V_OPEN = 0.55;
    const V_CLOSE = -0.55;

    let pan = null;

    function voiceBlocksPan() {
        const v = document.getElementById('voice-overlay');
        return v && !v.classList.contains('opacity-0');
    }

    function renameBlocksPan() {
        const d = document.getElementById('rename-dialog');
        return d && !d.classList.contains('hidden');
    }

    function targetInSidebar(target) {
        return sidebar.contains(target);
    }

    function targetInMainChrome(target) {
        return _mainChromeEls.some((el) => el && el.contains(target));
    }

    shell.addEventListener(
        'touchstart',
        (e) => {
            if (e.touches.length !== 1) return;
            if (voiceBlocksPan() || renameBlocksPan()) return;

            const t = e.touches[0];
            const x = t.clientX;
            const y = t.clientY;
            const target = e.target;

            if (!isSidebarOpen()) {
                if (!targetInMainChrome(target)) return;
                pan = {
                    mode: 'opening',
                    startX: x,
                    startY: y,
                    startP: 0,
                    lastX: x,
                    lastT: e.timeStamp,
                    vx: 0,
                    locked: false
                };
            } else {
                if (targetInMainChrome(target) || overlay.contains(target) || targetInSidebar(target)) {
                    pan = {
                        mode: 'closing',
                        startX: x,
                        startY: y,
                        startP: _drawerProgress,
                        lastX: x,
                        lastT: e.timeStamp,
                        vx: 0,
                        locked: false
                    };
                }
            }
        },
        { passive: false }
    );

    shell.addEventListener(
        'touchmove',
        (e) => {
            if (!pan || e.touches.length !== 1) return;
            const t = e.touches[0];
            const dx = t.clientX - pan.startX;
            const dy = t.clientY - pan.startY;

            if (!pan.locked) {
                if (Math.abs(dx) < 10 && Math.abs(dy) < 10) return;
                if (Math.abs(dy) > Math.abs(dx) * 1.08) {
                    pan = null;
                    return;
                }
                pan.locked = true;
                setPanElementsTransition(false);
            }

            e.preventDefault();

            const w = getSidebarDrawerWidthPx();
            const p = Math.max(0, Math.min(1, pan.startP + (t.clientX - pan.startX) / w));
            applyDrawerProgress(p);

            const dt = e.timeStamp - pan.lastT;
            if (dt > 0) {
                pan.vx = (t.clientX - pan.lastX) / dt;
            }
            pan.lastX = t.clientX;
            pan.lastT = e.timeStamp;
        },
        { passive: false }
    );

    function finishPan(e) {
        if (!pan) return;
        const wasLocked = pan.locked;
        const endPan = pan;
        pan = null;
        if (!wasLocked) return;

        const w = getSidebarDrawerWidthPx();
        const vx = endPan.vx;
        const p = _drawerProgress;
        const screenW = window.innerWidth;
        const openDistPx = OPEN_FRAC * screenW;
        let target = p;

        if (endPan.mode === 'opening') {
            if (p * w >= openDistPx || vx >= V_OPEN) target = 1;
            else target = 0;
        } else {
            if ((1 - p) * w >= openDistPx || vx <= V_CLOSE) target = 0;
            else target = 1;
        }

        setPanElementsTransition(true);
        applyDrawerProgress(target);
        window.setTimeout(() => setPanElementsTransition(false), 340);

        if (target < 0.05) hideContextMenu();
    }

    shell.addEventListener('touchend', finishPan, { passive: true });
    shell.addEventListener('touchcancel', finishPan, { passive: true });
}

function positionHistoryContextMenu(anchorEl) {
    const menu = document.getElementById('context-menu');
    if (!menu || !anchorEl) return;
    menu.classList.remove('hidden');
    menu.style.visibility = 'hidden';
    const gap = 8;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const rect = anchorEl.getBoundingClientRect();
    const mw = menu.offsetWidth || 240;
    const mh = menu.offsetHeight || 150;
    menu.style.visibility = '';
    let left = rect.left;
    let top = rect.bottom + gap;
    if (left + mw > vw - 8) left = Math.max(8, vw - mw - 8);
    if (left < 8) left = 8;
    if (top + mh > vh - 12) top = Math.max(8, rect.top - mh - gap);
    if (top < 8) top = 8;
    menu.style.left = `${left}px`;
    menu.style.top = `${top}px`;
}

function refreshContextMenuPinLabel() {
    const lbl = document.getElementById('ctx-pin-label');
    const item = contextMenuTargetItem;
    if (!lbl || !item) return;
    lbl.textContent = item.getAttribute('data-pinned') === 'true' ? '取消置顶' : '置顶';
}

/** 根据 data-pinned 更新左侧会话图标 solid/regular、右侧图钉显隐（颜色交给 common.css，避免运行时拼 Tailwind 类被 CDN 摇树掉） */
function updateHistoryRowChrome(item) {
    if (!item) return;
    const pinned = item.getAttribute('data-pinned') === 'true';
    const typeIcon = item.querySelector('.history-item-type-icon');
    const pinMark = item.querySelector('.history-pin-icon');
    if (typeIcon) {
        typeIcon.classList.toggle('fa-solid', pinned);
        typeIcon.classList.toggle('fa-regular', !pinned);
    }
    if (pinMark) {
        pinMark.classList.toggle('invisible', !pinned);
    }
}

function historyTogglePin(item) {
    const root = document.getElementById('history-items-root');
    if (!item || !root) return;
    const pinned = item.getAttribute('data-pinned') === 'true';
    if (pinned) {
        item.setAttribute('data-pinned', 'false');
        root.appendChild(item);
    } else {
        item.setAttribute('data-pinned', 'true');
        root.prepend(item);
    }
    updateHistoryRowChrome(item);
}

function historyDeleteItem(item) {
    const root = document.getElementById('history-items-root');
    const emptyHint = document.getElementById('history-empty-hint');
    if (!item || !root) return;
    item.remove();
    if (root.children.length && emptyHint) emptyHint.classList.add('hidden');
    if (!root.children.length && emptyHint) emptyHint.classList.remove('hidden');
}

function onDocumentClickForContextMenu(e) {
    const menu = document.getElementById('context-menu');
    if (!menu || menu.classList.contains('hidden')) return;
    if (e.target.closest('#context-menu')) return;
    // 长按松手后的 click 仍落在条目上：短窗内忽略，防止菜单刚弹出就被关掉
    if (Date.now() - contextMenuOpenedAt < 420 && e.target.closest('.history-item')) return;
    hideContextMenu();
}

function showContextMenu(e, element) {
    if (!contextMenu || !element) return;
    e.preventDefault();
    e.stopPropagation();
    contextMenuTargetItem = element;
    refreshContextMenuPinLabel();
    positionHistoryContextMenu(element);
    contextMenu.classList.remove('hidden');
    contextMenuOpenedAt = Date.now();
    document.querySelectorAll('.history-item').forEach((el) => el.classList.remove('context-open'));
    element.classList.add('context-open');
}

function hideContextMenu() {
    if (contextMenu) contextMenu.classList.add('hidden');
    document.querySelectorAll('.history-item.context-open').forEach((el) => el.classList.remove('context-open'));
    contextMenuTargetItem = null;
}

function initSidebarHistory() {
    const root = document.getElementById('history-items-root');
    const menu = document.getElementById('context-menu');
    const scrollArea = document.getElementById('history-scroll-area');
    if (!root || !menu) return;

    root.querySelectorAll('.history-item').forEach((el) => updateHistoryRowChrome(el));

    const cancelPress = () => {
        if (pressTimer) {
            clearTimeout(pressTimer);
            pressTimer = null;
        }
        pressStartEl = null;
    };

    const openMenuForItem = (el) => {
        if (navigator.vibrate) {
            try {
                navigator.vibrate(12);
            } catch (_) {}
        }
        contextMenuTargetItem = el;
        refreshContextMenuPinLabel();
        positionHistoryContextMenu(el);
        menu.classList.remove('hidden');
        contextMenuOpenedAt = Date.now();
        document.querySelectorAll('.history-item').forEach((n) => n.classList.remove('context-open'));
        el.classList.add('context-open');
    };

    const startPress = (el, clientX, clientY) => {
        cancelPress();
        pressStartEl = el;
        pressTimer = setTimeout(() => {
            pressTimer = null;
            if (pressStartEl === el) openMenuForItem(el);
        }, LONG_PRESS_MS);
    };

    root.querySelectorAll('.history-item').forEach((el) => {
        el.addEventListener(
            'touchstart',
            (e) => {
                if (e.touches.length !== 1) return;
                startPress(el, e.touches[0].clientX, e.touches[0].clientY);
            },
            { passive: true }
        );
        el.addEventListener(
            'touchmove',
            (e) => {
                if (!pressTimer) return;
                const t = e.touches[0];
                const r = el.getBoundingClientRect();
                const pad = 16;
                if (
                    t.clientX < r.left - pad ||
                    t.clientX > r.right + pad ||
                    t.clientY < r.top - pad ||
                    t.clientY > r.bottom + pad
                ) {
                    cancelPress();
                }
            },
            { passive: true }
        );
        el.addEventListener('touchend', cancelPress);
        el.addEventListener('touchcancel', cancelPress);

        el.addEventListener('mousedown', (e) => {
            if (e.button !== 0) return;
            startPress(el, e.clientX, e.clientY);
        });
        el.addEventListener('mouseup', cancelPress);
        el.addEventListener('mouseleave', cancelPress);

        el.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            openMenuForItem(el);
        });
    });

    if (scrollArea) scrollArea.addEventListener('scroll', hideContextMenu);

    const renameBtn = document.getElementById('ctx-rename');
    const pinBtn = document.getElementById('ctx-pin');
    const delBtn = document.getElementById('ctx-delete');
    if (renameBtn) {
        renameBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            renameTargetItem = contextMenuTargetItem;
            hideContextMenu();
            openRenameHistoryDialog();
        });
    }
    if (pinBtn) {
        pinBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const t = contextMenuTargetItem;
            historyTogglePin(t);
            hideContextMenu();
        });
    }
    if (delBtn) {
        delBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            historyDeleteItem(contextMenuTargetItem);
            hideContextMenu();
        });
    }
}

function openRenameHistoryDialog() {
    const overlay = document.getElementById('rename-dialog');
    const input = document.getElementById('rename-dialog-input');
    if (!overlay || !input || !renameTargetItem) return;
    const title = renameTargetItem.querySelector('.history-item-title');
    input.value = title ? title.textContent.trim() : '';
    overlay.classList.remove('hidden');
    overlay.setAttribute('aria-hidden', 'false');
    setTimeout(() => input.focus(), 60);
}

function closeRenameHistoryDialog() {
    const overlay = document.getElementById('rename-dialog');
    if (!overlay) return;
    overlay.classList.add('hidden');
    overlay.setAttribute('aria-hidden', 'true');
    const input = document.getElementById('rename-dialog-input');
    if (input) input.value = '';
    renameTargetItem = null;
}

function confirmRenameHistoryDialog() {
    const input = document.getElementById('rename-dialog-input');
    if (!renameTargetItem || !input) return;
    const title = renameTargetItem.querySelector('.history-item-title');
    const v = input.value.trim();
    if (v && title) title.textContent = v;
    closeRenameHistoryDialog();
}

function initRenameDialog() {
    const overlay = document.getElementById('rename-dialog');
    const cancelBtn = document.getElementById('rename-dialog-cancel');
    const confirmBtn = document.getElementById('rename-dialog-confirm');
    const panel = document.getElementById('rename-dialog-panel');
    if (!overlay || !cancelBtn || !confirmBtn) return;

    cancelBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        closeRenameHistoryDialog();
    });
    confirmBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        confirmRenameHistoryDialog();
    });
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeRenameHistoryDialog();
    });
    if (panel) {
        panel.addEventListener('click', (e) => e.stopPropagation());
    }

    const input = document.getElementById('rename-dialog-input');
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') confirmRenameHistoryDialog();
            if (e.key === 'Escape') closeRenameHistoryDialog();
        });
    }
}

if (menuBtn) menuBtn.addEventListener('click', openSidebar);
if (overlay) overlay.addEventListener('click', closeSidebar);
document.addEventListener('click', onDocumentClickForContextMenu);
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
    if (isDark) {
        shell.classList.add('dark');
        document.documentElement.classList.add('dark');
    } else {
        shell.classList.remove('dark');
        document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('cogno-dark-mode', isDark);
}

function loadTheme() {
    const isDark = localStorage.getItem('cogno-dark-mode') === 'true';
    applyDarkMode(isDark);
    setStatusBarColorForTheme(isDark);
}

function onDomContentLoaded() {
    loadTheme();
    initSidebarHistory();
    initRenameDialog();
    ensureSidebarPanInit();
}
document.addEventListener('DOMContentLoaded', onDomContentLoaded);

function setStatusBarColorForTheme(isDarkMode) {
    if (window.Android && window.Android.setNavigationBarColor) {
        try {
            window.Android.setNavigationBarColor(0);
        } catch (_) {}
    }
    if (window.Android && window.Android.setStatusBarDarkMode) {
        window.Android.setStatusBarDarkMode(isDarkMode);
    }
}