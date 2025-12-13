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

            trimMessages('bottom');
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

            trimMessages('top');
        } catch (e) {
            console.error('Failed loading newer messages', e);
        } finally {
            loading = false;
        }
    }

    function trimMessages(side) {
        const nodes = Array.from(container.querySelectorAll('[data-message-id]'));
        if (nodes.length <= MAX_MESSAGES) return;

        const removeCount = nodes.length - MAX_MESSAGES;

        const readCursor = (el) => {
            if (!el) return null;
            return el.dataset.cursor;
        };

        if (side === 'top') {
            // remove from the top
            for (let i = 0; i < removeCount; i++) {
                const node = nodes[i];
                if (node) node.remove();
            }

            // update topCursor und topHasMore
            const remaining = Array.from(container.querySelectorAll('[data-message-id]'));
            const first = remaining[0];
            topCursor = readCursor(first);
            topHasMore = true;
            container.dataset.topCursor = topCursor;
            container.dataset.topHasMore = String(topHasMore);
        } else {
            // remove from the bottom
            for (let i = 0; i < removeCount; i++) {
                const node = nodes[nodes.length - 1 - i];
                if (node) node.remove();
            }

            // update bottomCursor und bottomHasMore
            const remaining = Array.from(container.querySelectorAll('[data-message-id]'));
            const last = remaining[remaining.length - 1];
            bottomCursor = last.dataset.cursor;
            bottomHasMore = true;
            container.dataset.bottomCursor = bottomCursor;
            container.dataset.bottomHasMore = String(bottomHasMore);
        }
    }

})();
