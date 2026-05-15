let currentMode = 'session';

function toggleMode() {
    const titleNode = document.getElementById('library-title');
    if (currentMode === 'session') {
        currentMode = 'topic';
        titleNode.innerText = "NoteLibrary-Topic";
    } else {
        currentMode = 'session';
        titleNode.innerText = "NoteLibrary-Dialogue";
    }
    console.log("当前模式:", currentMode);
}

function toggleSearch() {
    const wrapper = document.getElementById('search-bar-wrapper');
    const searchInput = document.getElementById('note-search-input');
    if (wrapper.classList.contains('grid-rows-[0fr]')) {
        wrapper.classList.remove('grid-rows-[0fr]');
        wrapper.classList.add('grid-rows-[1fr]');
        setTimeout(() => searchInput.focus(), 150);
    } else {
        wrapper.classList.remove('grid-rows-[1fr]');
        wrapper.classList.add('grid-rows-[0fr]');
        searchInput.blur();
        searchInput.value = "";
    }
}

const NOTE_LONG_PRESS_MS = 520;
let notePressTimer = null;
let notePressStartEl = null;
let noteContextTarget = null;
let noteRenameTarget = null;
let noteContextOpenedAt = 0;

function positionNoteContextMenu(anchorEl) {
    const menu = document.getElementById('note-context-menu');
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

function refreshNotePinLabel() {
    const label = document.getElementById('note-ctx-pin-label');
    if (!label || !noteContextTarget) return;
    label.textContent = noteContextTarget.getAttribute('data-pinned') === 'true' ? '取消置顶' : '置顶';
}

function updateNotePinIcon(item) {
    if (!item) return;
    const icon = item.querySelector('.note-pin-icon');
    const pinned = item.getAttribute('data-pinned') === 'true';
    if (icon) icon.classList.toggle('hidden', !pinned);
}

function openNoteContextMenu(item) {
    if (!item) return;
    if (navigator.vibrate) {
        try {
            navigator.vibrate(12);
        } catch (_) {}
    }
    noteContextTarget = item;
    refreshNotePinLabel();
    positionNoteContextMenu(item);
    noteContextOpenedAt = Date.now();
    document.querySelectorAll('.note-item').forEach((el) => el.classList.remove('context-open'));
    item.classList.add('context-open');
}

function hideNoteContextMenu() {
    const menu = document.getElementById('note-context-menu');
    if (menu) menu.classList.add('hidden');
    document.querySelectorAll('.note-item.context-open').forEach((el) => el.classList.remove('context-open'));
    noteContextTarget = null;
}

function toggleNotePin(item) {
    const list = item ? item.parentElement : null;
    if (!item || !list) return;
    const pinned = item.getAttribute('data-pinned') === 'true';
    item.setAttribute('data-pinned', pinned ? 'false' : 'true');
    updateNotePinIcon(item);
    if (pinned) list.appendChild(item);
    else list.prepend(item);
}

function openNoteRenameDialog() {
    const dialog = document.getElementById('note-rename-dialog');
    const input = document.getElementById('note-rename-dialog-input');
    if (!dialog || !input || !noteRenameTarget) return;
    const title = noteRenameTarget.querySelector('.note-item-title');
    input.value = title ? title.textContent.trim() : '';
    dialog.classList.remove('hidden');
    dialog.setAttribute('aria-hidden', 'false');
    setTimeout(() => input.focus(), 60);
}

function closeNoteRenameDialog() {
    const dialog = document.getElementById('note-rename-dialog');
    if (!dialog) return;
    dialog.classList.add('hidden');
    dialog.setAttribute('aria-hidden', 'true');
    const input = document.getElementById('note-rename-dialog-input');
    if (input) input.value = '';
    noteRenameTarget = null;
}

function confirmNoteRenameDialog() {
    const input = document.getElementById('note-rename-dialog-input');
    if (!input || !noteRenameTarget) return;
    const title = noteRenameTarget.querySelector('.note-item-title');
    const value = input.value.trim();
    if (value && title) title.textContent = value;
    closeNoteRenameDialog();
}

function initNoteContextMenu() {
    const items = Array.from(document.querySelectorAll('.note-item'));
    const menu = document.getElementById('note-context-menu');
    if (!items.length || !menu) return;

    items.forEach(updateNotePinIcon);

    const cancelPress = () => {
        if (notePressTimer) {
            clearTimeout(notePressTimer);
            notePressTimer = null;
        }
        notePressStartEl = null;
    };

    const startPress = (item) => {
        cancelPress();
        notePressStartEl = item;
        notePressTimer = setTimeout(() => {
            notePressTimer = null;
            if (notePressStartEl === item) openNoteContextMenu(item);
        }, NOTE_LONG_PRESS_MS);
    };

    items.forEach((item) => {
        item.addEventListener(
            'touchstart',
            (e) => {
                if (e.touches.length !== 1) return;
                startPress(item);
            },
            { passive: true }
        );
        item.addEventListener(
            'touchmove',
            (e) => {
                if (!notePressTimer) return;
                const t = e.touches[0];
                const r = item.getBoundingClientRect();
                const pad = 16;
                if (t.clientX < r.left - pad || t.clientX > r.right + pad || t.clientY < r.top - pad || t.clientY > r.bottom + pad) {
                    cancelPress();
                }
            },
            { passive: true }
        );
        item.addEventListener('touchend', cancelPress);
        item.addEventListener('touchcancel', cancelPress);
        item.addEventListener('mousedown', (e) => {
            if (e.button !== 0) return;
            startPress(item);
        });
        item.addEventListener('mouseup', cancelPress);
        item.addEventListener('mouseleave', cancelPress);
        item.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            openNoteContextMenu(item);
        });
    });

    document.addEventListener(
        'click',
        (e) => {
            if (Date.now() - noteContextOpenedAt < 520 && e.target.closest('.note-item')) {
                e.preventDefault();
                e.stopImmediatePropagation();
                return;
            }
            if (e.target.closest('#note-context-menu')) return;
            hideNoteContextMenu();
        },
        true
    );

    const renameBtn = document.getElementById('note-ctx-rename');
    const pinBtn = document.getElementById('note-ctx-pin');
    const deleteBtn = document.getElementById('note-ctx-delete');
    if (renameBtn) renameBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        noteRenameTarget = noteContextTarget;
        hideNoteContextMenu();
        openNoteRenameDialog();
    });
    if (pinBtn) pinBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleNotePin(noteContextTarget);
        hideNoteContextMenu();
    });
    if (deleteBtn) deleteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        if (noteContextTarget) noteContextTarget.remove();
        hideNoteContextMenu();
    });

    const dialog = document.getElementById('note-rename-dialog');
    const panel = document.getElementById('note-rename-dialog-panel');
    const cancelBtn = document.getElementById('note-rename-dialog-cancel');
    const confirmBtn = document.getElementById('note-rename-dialog-confirm');
    const input = document.getElementById('note-rename-dialog-input');
    if (cancelBtn) cancelBtn.addEventListener('click', closeNoteRenameDialog);
    if (confirmBtn) confirmBtn.addEventListener('click', confirmNoteRenameDialog);
    if (dialog) dialog.addEventListener('click', (e) => {
        if (e.target === dialog) closeNoteRenameDialog();
    });
    if (panel) panel.addEventListener('click', (e) => e.stopPropagation());
    if (input) input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') confirmNoteRenameDialog();
        if (e.key === 'Escape') closeNoteRenameDialog();
    });
}

document.addEventListener('DOMContentLoaded', initNoteContextMenu);
