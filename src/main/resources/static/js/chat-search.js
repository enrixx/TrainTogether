(function () {
    const input = document.getElementById('chatSearch');
    let timer;
    function filter() {
        const q = input.value.trim().toLowerCase();
        document.querySelectorAll('ul.list-unstyled > li').forEach(li => {
            const nameEl = li.querySelector('.fw-bold');
            const name = nameEl ? nameEl.textContent.trim().toLowerCase() : '';
            li.style.display = q === '' || name.includes(q) ? '' : 'none';
        });
    }
    input.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(filter, 200);
    });
})();