let isEditMode = false;

function toggleEditMode() {
    const icon = document.getElementById('mode-icon');
    isEditMode = !isEditMode;
    if (isEditMode) {
        icon.className = 'fa-solid fa-eye text-[17px]';
        console.log("进入编辑模式");
    } else {
        icon.className = 'fa-solid fa-pencil text-[17px]';
        console.log("进入查看模式");
    }
}

function toggleShareMenu() {
    const menu = document.getElementById('share-menu');
    menu.classList.toggle('menu-hide');
    menu.classList.toggle('menu-show');
}

document.addEventListener('click', (e) => {
    const menu = document.getElementById('share-menu');
    const shareBtn = e.target.closest('button');
    if (menu.classList.contains('menu-show') && !menu.contains(e.target) && (!shareBtn || !shareBtn.innerHTML.includes('fa-arrow-up-right-from-square'))) {
        menu.classList.add('menu-hide');
        menu.classList.remove('menu-show');
    }
});