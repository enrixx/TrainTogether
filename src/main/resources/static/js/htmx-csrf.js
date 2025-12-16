document.addEventListener('DOMContentLoaded', function () {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) return;

    const token = tokenMeta.getAttribute('content');
    const header = headerMeta.getAttribute('content');

    document.body.addEventListener('htmx:configRequest', function (event) {
        if (!event.detail.headers) event.detail.headers = {};
        event.detail.headers[header] = token;
    });
});