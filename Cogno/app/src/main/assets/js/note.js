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