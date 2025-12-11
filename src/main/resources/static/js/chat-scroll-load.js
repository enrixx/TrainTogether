(function () {
    const MAX_MESSAGES = 120;
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    let loading = false;
    let topCursor = container.dataset.topCursor
    let topHasMore = container.dataset.topHasMore === 'true'
    let bottomCursor = container.dataset.bottomCursor
    let bottomHasMore = container.dataset.bottomHasMore === 'true'
    let pageSize = parseInt(container.dataset.pageSize, 10);

    let topSentinel = container.querySelector('#top-sentinel');
    let bottomSentinel = container.querySelector('#bottom-sentinel');

    // ---------------- Observers ----------------
    const topObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting && !loading && topHasMore) {
                loadOlder().then();
            }
        });
    }, {root: container, threshold: 0.1});
    topObserver.observe(topSentinel);

    const bottomObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting && !loading && bottomHasMore) {
                loadNewer().then();
            }
        });
    }, {root: container, threshold: 0.1});
    bottomObserver.observe(bottomSentinel);

    // ---------------- Loaders ----------------
    async function loadOlder() {
        if (loading) return;
        loading = true;

        const roomId = container.dataset.roomId;
        const url = new URL(`/chat/${roomId}/messages/fragment`, window.location.origin);
        url.searchParams.set('direction', 'up');
        if (topCursor) url.searchParams.set('cursor', topCursor);
        url.searchParams.set('pageSize', String(pageSize));

        //save Scroll-Position
        const prevScrollHeight = container.scrollHeight;

        try {
            const res = await fetch(url.toString(), {credentials: 'same-origin'});
            const page = await res.json();

            const html = page.html || '';
            if (html.trim()) {
                // parse and insert fragment right after top sentinel while preserving order
                const temp = document.createElement('div');
                temp.innerHTML = html;
                const frag = document.createDocumentFragment();
                while (temp.firstChild) {
                    frag.appendChild(temp.firstChild);
                }
                container.insertBefore(frag, topSentinel.nextSibling);
            }

            // adjust scroll so user's viewport stays at same messages
            const newScrollHeight = container.scrollHeight;
            container.scrollTop += (newScrollHeight - prevScrollHeight);

            // update cursor/hasMore and dataset attributes
            topCursor = page.cursor;
            topHasMore = !!page.hasMore;
            container.dataset.topCursor = topCursor;
            container.dataset.topHasMore = String(topHasMore);

            //TODO: trimMessages();
        } catch (e) {
            console.error('Failed loading older messages', e);
        } finally {
            loading = false;
        }
    }

    async function loadNewer() {
        if (loading) return;
        loading = true;

        const roomId = container.dataset.roomId;
        const url = new URL(`/chat/${roomId}/messages/fragment`, window.location.origin);
        url.searchParams.set('direction', 'down');
        if (bottomCursor) url.searchParams.set('cursor', bottomCursor);
        url.searchParams.set('pageSize', String(pageSize));

        try {
            const res = await fetch(url.toString(), {credentials: 'same-origin'});
            const page = await res.json();

            const html = page.html || '';
            if (html.trim()) {
                const temp = document.createElement('div');
                temp.innerHTML = html;
                const frag = document.createDocumentFragment();
                while (temp.firstChild) {
                    frag.appendChild(temp.firstChild);
                }
                // insert new messages right before bottom sentinel
                container.insertBefore(frag, bottomSentinel);
            }

            // update cursor/hasMore and dataset attributes
            bottomCursor = page.cursor || null;
            bottomHasMore = !!page.hasMore;
            container.dataset.bottomCursor = bottomCursor || '';
            container.dataset.bottomHasMore = String(bottomHasMore);

            //TODO: trimMessages();
        } catch (e) {
            console.error('Failed loading newer messages', e);
        } finally {
            loading = false;
        }
    }
})();
