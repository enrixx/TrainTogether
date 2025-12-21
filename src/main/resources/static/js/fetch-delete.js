const getCsrf = () => {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    return {
        header: headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN',
        token: tokenMeta ? tokenMeta.getAttribute('content') : null
    };
};

const fetchDelete = async (url, confirmMessage, removeTargetId) => {
    if (!confirm(confirmMessage)) return false;

    const { header, token } = getCsrf();

    try {
        const res = await fetch(url, {
            method: 'DELETE',
            headers: token ? { [header]: token } : {},
            credentials: 'same-origin'
        });

        if (res.ok) {
            if (removeTargetId) {
                document.getElementById(removeTargetId)?.remove();
            }
            return true;
        }
    } catch (err) {
        return false;
    }
};

// Expose for inline calls from templates
window.fetchDelete = fetchDelete;
