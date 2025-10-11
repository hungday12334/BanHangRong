/* Seller Dashboard client script
   - Progressive enhancement: runs after DOM ready
   - No external deps except Chart.js (loaded via CDN in template)
   - Features: count-up anim, chart render, simple pagination for tables */

; (function () {
  function onReady(fn) { if (document.readyState !== 'loading') { fn(); } else { document.addEventListener('DOMContentLoaded', fn); } }

  function easeOutCubic(t) { return 1 - Math.pow(1 - t, 3); }

  function animateCount(el) {
    const targetAttr = el.getAttribute('data-count');
    if (!targetAttr) return;
    const target = Number(targetAttr.toString().replace(/,/g, '')) || 0;
    const dur = 900; // ms
    const start = performance.now();
    function tick(now) {
      const p = Math.min(1, (now - start) / dur);
      const v = Math.round(target * easeOutCubic(p));
      el.textContent = v.toLocaleString('en-US');
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  function readCSVAttr(el, name) {
    const s = el.getAttribute(name) || '';
    // Thymeleaf provides comma-separated strings already escaped
    return s.split(',').map(x => x.trim()).filter(Boolean);
  }

  function initChart() {
    const canvas = document.getElementById('revenueChart');
    if (!canvas || typeof Chart === 'undefined') return;
    const labels = readCSVAttr(canvas, 'data-labels');
    const data = readCSVAttr(canvas, 'data-data').map(Number);

    const ctx = canvas.getContext('2d');
    // Resize height gently on small screens
    if (window.innerWidth < 820) canvas.height = 160;

    new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data,
          label: 'Doanh thu',
          fill: true,
          borderColor: '#7c9eff',
          backgroundColor: 'rgba(124,158,255,0.16)',
          tension: 0.35,
          pointRadius: 0,
          borderWidth: 2,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
        scales: {
          x: { display: true, grid: { display: false }, ticks: { color: '#a8b0d3', maxTicksLimit: 8 } },
          y: { display: true, grid: { color: 'rgba(255,255,255,0.06)' }, ticks: { color: '#a8b0d3', callback: v => `$${Number(v).toLocaleString('en-US')}` } }
        },
        elements: { line: { cubicInterpolationMode: 'monotone' } }
      }
    });
  }

  // Toast utility
  function showToast(message, type = 'info', opts = {}) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? 'ti ti-circle-check' : type === 'error' ? 'ti ti-alert-triangle' : 'ti ti-info-circle';
    toast.innerHTML = `<span class="icon"><i class="${icon}"></i></span><div class="msg">${message}</div><div class="act"><button class="close" aria-label="Đóng">✕</button></div>`;
    container.appendChild(toast);
    // Force reflow to play animation
    void toast.offsetWidth; toast.classList.add('show');
    const close = () => { toast.classList.remove('show'); setTimeout(() => toast.remove(), 200); };
    toast.querySelector('.close').addEventListener('click', close);
    const delay = opts.duration ?? (type === 'error' ? 5000 : 3000);
    if (delay !== 0) setTimeout(close, delay);
  }

  function paginateTable(tbody, pager, pageSize) {
    // Mark container for CSS fallback (keep pager pinned)
    pager?.closest('.body')?.classList.add('has-pager');
    // Always start from a clean state: remove previous filler rows
    tbody.querySelectorAll('tr.filler-row').forEach(fr => fr.remove());
    let rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('filler-row') && !tr.querySelector('td.footer-note'));
    let rowCount = rows.length;
    let totalPages = Math.max(1, Math.ceil(rowCount / pageSize));
    let current = 1;

    let stableHeightSet = false;
    function setStableMinHeight() {
      if (stableHeightSet) return;
      const bodyEl = pager.closest('.body');
      const table = tbody.closest('table');
      const thead = table ? table.querySelector('thead') : null;
      // Measure tallest visible row in current page to avoid clipping when text wraps
      const start = (current - 1) * pageSize;
      const end = Math.min(start + pageSize, rows.length);
      let maxRowH = 0;
      for (let i = start; i < end; i++) {
        const h = rows[i]?.getBoundingClientRect().height || 0;
        if (h > maxRowH) maxRowH = h;
      }
      const rowH = maxRowH || 44;
      const hdrH = (thead?.getBoundingClientRect().height) || 32;
      const pagerH = (pager.getBoundingClientRect().height) || 36;
      const padding = 24; // breathing space
      const minH = Math.ceil(hdrH + rowH * pageSize + pagerH + padding);
      if (bodyEl) bodyEl.style.minHeight = `${minH}px`;
      stableHeightSet = true;
    }

    function render() {
      // Refresh rows and counters in case DOM changed externally
      rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('filler-row') && !tr.querySelector('td.footer-note'));
      rowCount = rows.length;
      totalPages = Math.max(1, Math.ceil(rowCount / pageSize));
      if (current > totalPages) current = totalPages;

      const start = (current - 1) * pageSize;
      const end = Math.min(start + pageSize, rowCount);
      rows.forEach((tr, idx) => {
        tr.style.display = (idx >= start && idx < end) ? '' : 'none';
      });
      // Pad with invisible filler rows so the table keeps constant height per page
      tbody.querySelectorAll('tr.filler-row').forEach(fr => fr.remove());
      const visibleCount = Math.max(0, end - start);
      const need = Math.max(0, pageSize - visibleCount);
      if (need > 0) {
        const colCount = (tbody.closest('table')?.querySelectorAll('thead th').length) || (rows[0]?.children.length) || 1;
        const footerNoteRow = tbody.querySelector('tr td.footer-note')?.parentElement || null;
        for (let i = 0; i < need; i++) {
          const tr = document.createElement('tr');
          tr.className = 'filler-row';
          const td = document.createElement('td');
          td.colSpan = colCount;
          td.innerHTML = '&nbsp;';
          tr.appendChild(td);
          if (footerNoteRow) tbody.insertBefore(tr, footerNoteRow);
          else tbody.appendChild(tr);
        }
      }
      // Build controls
      pager.innerHTML = '';
      if (totalPages > 1) {
        const mkBtn = (label, page, disabled, ariaCurrent) => {
          const b = document.createElement('button');
          b.type = 'button';
          b.className = 'btn';
          b.textContent = label;
          if (ariaCurrent) b.setAttribute('aria-current', 'page');
          b.disabled = !!disabled;
          b.setAttribute('aria-label', `Trang ${page}`);
          b.addEventListener('click', () => { current = page; render(); /* keep pager fixed: avoid scrollIntoView */ });
          return b;
        };
        pager.appendChild(mkBtn('«', Math.max(1, current - 1), current === 1, false));
        for (let i = 1; i <= totalPages; i++) {
          pager.appendChild(mkBtn(String(i), i, false, i === current));
        }
        pager.appendChild(mkBtn('»', Math.min(totalPages, current + 1), current === totalPages, false));
      } else {
        // When a single page, ensure pager is cleared and keep rows visible with fillers if needed
        pager.innerHTML = '';
      }
      // After DOM updates, fix a stable min-height once (applies to both single/multi page)
      requestAnimationFrame(setStableMinHeight);
    }
    render();
  }

  function applyTheme(theme) {
    const root = document.documentElement;
    root.classList.remove('theme-dark', 'theme-light');
    if (theme === 'dark') root.classList.add('theme-dark');
    else if (theme === 'light') root.classList.add('theme-light');
    // update theme-color meta for browser UI
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'light' ? '#f6f7fb' : '#0b1020');
    // toggle icon
    const btn = document.getElementById('themeToggle');
    if (btn) {
      btn.innerHTML = theme === 'light' ? '<i class="ti ti-moon"></i>' : '<i class="ti ti-sun"></i>';
      btn.setAttribute('aria-label', 'Chuyển giao diện');
      btn.title = 'Chuyển giao diện';
    }
  }

  onReady(function () {
    // Ensure global loader is dismissed so the page becomes visible
    document.body.classList.remove('loading');
    document.body.classList.add('ready');
    // Animate KPI counters
    document.querySelectorAll('[data-count]').forEach(animateCount);

    // Progress bar width already set by Thymeleaf inline style; ensure transition applies after a frame
    document.querySelectorAll('.progress span').forEach(span => {
      const w = span.style.width;
      span.style.width = '0%';
      requestAnimationFrame(() => { span.style.width = w || '0%'; });
    });

    initChart();

    // Tables pagination (progressive enhancement)
    const lowStockTbody = document.getElementById('tbLowStock');
    const lowStockPager = document.getElementById('pgLowStock');
    if (lowStockTbody && lowStockPager) paginateTable(lowStockTbody, lowStockPager, 5);

    const topProductsTbody = document.getElementById('tbTopProducts');
    const topProductsPager = document.getElementById('pgTopProducts');
    if (topProductsTbody && topProductsPager) paginateTable(topProductsTbody, topProductsPager, 5);

    const recentOrdersTbody = document.getElementById('tbRecentOrders');
    const recentOrdersPager = document.getElementById('pgRecentOrders');
    if (recentOrdersTbody && recentOrdersPager) paginateTable(recentOrdersTbody, recentOrdersPager, 5);

    // Theme toggle with system default
    const userPref = localStorage.getItem('theme');
    if (userPref === 'light' || userPref === 'dark') applyTheme(userPref);
    else applyTheme(window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');

    const btn = document.getElementById('themeToggle');
    if (btn) {
      btn.addEventListener('click', () => {
        const isLight = document.documentElement.classList.contains('theme-light');
        const next = isLight ? 'dark' : 'light';
        localStorage.setItem('theme', next);
        applyTheme(next);
        if (typeof showToast === 'function') {
          showToast(next === 'light' ? 'Đã chuyển sang giao diện sáng' : 'Đã chuyển sang giao diện tối', 'info', { duration: 1500 });
        }
      });
    }

    // ===== Modals & CRUD =====
    const overlay = document.getElementById('modalOverlay');

    // Scroll lock helpers to avoid jump-to-top when opening dialogs
    function lockScroll() {
      if (document.body.style.position === 'fixed') return; // already locked
      const y = window.scrollY || document.documentElement.scrollTop || 0;
      document.documentElement.setAttribute('data-scroll-y', String(y));
      document.body.style.position = 'fixed';
      document.body.style.top = `-${y}px`;
      document.body.style.left = '0';
      document.body.style.right = '0';
      document.body.style.width = '100%';
    }
    function unlockScroll() {
      const yStr = document.documentElement.getAttribute('data-scroll-y');
      document.body.style.position = '';
      document.body.style.top = '';
      document.body.style.left = '';
      document.body.style.right = '';
      document.body.style.width = '';
      document.documentElement.removeAttribute('data-scroll-y');
      if (yStr) {
        const y = parseInt(yStr, 10);
        if (!Number.isNaN(y)) window.scrollTo(0, y);
      }
    }
    function openModal(dlg) {
      if (!dlg) return;
      if (overlay) { overlay.hidden = false; overlay.classList.add('visible'); }
      // lock background scroll without changing scroll position
      lockScroll();
      // prefer native dialog but still animate via class
      if (typeof dlg.showModal === 'function') {
        try { dlg.showModal(); } catch (e) { dlg.setAttribute('open', ''); }
      } else {
        dlg.setAttribute('open', ''); dlg.style.display = 'block';
      }
      // ensure animation class toggles
      requestAnimationFrame(() => dlg.classList.add('is-open'));
    }

    function closeModal(dlg) {
      if (!dlg) return;
      // play closing animation
      dlg.classList.remove('is-open');
      const finishClose = () => {
        // ensure native close/remove open attribute
        if (typeof dlg.close === 'function') {
          try { dlg.close(); } catch (e) { dlg.removeAttribute('open'); dlg.style.display = 'none'; }
        } else {
          dlg.removeAttribute('open'); dlg.style.display = 'none';
        }

        // Only clear overlay and scroll-lock when no other dialogs remain open
        const remaining = Array.from(document.querySelectorAll('dialog.modal[open]'));
        if (!remaining.length) {
          if (overlay) { overlay.classList.remove('visible'); overlay.hidden = true; }
          // restore scroll position after releasing lock
          unlockScroll();
        }

        dlg.removeEventListener('transitionend', finishClose);
      };
      // if transition exists, wait for it; otherwise close immediately
      const cs = window.getComputedStyle(dlg);
      const hasTransition = cs.transitionDuration && cs.transitionDuration !== '0s';
      if (hasTransition) {
        dlg.addEventListener('transitionend', finishClose);
      } else {
        finishClose();
      }
    }
    // click on overlay closes any open dialog
    overlay?.addEventListener('click', (ev) => {
      const openDialogs = Array.from(document.querySelectorAll('dialog.modal[open]'));
      openDialogs.forEach(d => closeModal(d));
    });

    // Close dialogs on ESC
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' || e.key === 'Esc') {
        e.preventDefault();
        const openDialogs = Array.from(document.querySelectorAll('dialog.modal[open]'));
        if (openDialogs.length) {
          openDialogs.forEach(d => closeModal(d));
        }
      }
    });

    // Page load fade-in
    document.body.classList.remove('page-leaving');
    requestAnimationFrame(() => document.body.classList.add('page-loaded'));

    // Intercept internal link clicks for smooth leave transition
    document.addEventListener('click', (e) => {
      const a = e.target.closest && e.target.closest('a');
      if (!a) return;
      const href = a.getAttribute('href');
      if (!href || href.startsWith('#') || a.target === '_blank' || href.startsWith('mailto:')) return;
      // allow same-origin navigation only
      const url = new URL(href, window.location.href);
      if (url.origin !== window.location.origin) return;
      e.preventDefault();
      document.body.classList.remove('page-loaded');
      document.body.classList.add('page-leaving');
      setTimeout(() => { window.location.href = url.href; }, 260);
    });

    // Product modal handlers
    const productModal = document.getElementById('productModal');
    if (productModal) {
      // ensure any native cancel/close events clear locks
      productModal.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(productModal); });
      productModal.addEventListener('close', () => {
        const remaining = Array.from(document.querySelectorAll('dialog.modal[open]'));
        if (!remaining.length) {
          if (overlay) { overlay.classList.remove('visible'); overlay.hidden = true; }
          unlockScroll();
        }
      });
      productModal.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(productModal)));

      const sellerIdEl = document.getElementById('sellerId');
      const sellerId = sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null;

      // keep a snapshot of original values to detect changes before save
      let original = null;
      async function loadProduct(id) {
        const res = await fetch(`/api/products/${id}`);
        if (!res.ok) { showToast('Không tải được chi tiết sản phẩm', 'error'); return; }
  const p = await res.json();
        document.getElementById('pm_productId').value = p.productId ?? '';
        document.getElementById('pm_name').value = p.name ?? '';
        document.getElementById('pm_price').value = p.price ?? '';
        document.getElementById('pm_salePrice').value = p.salePrice ?? '';
        document.getElementById('pm_quantity').value = p.quantity ?? 0;
        document.getElementById('pm_downloadUrl').value = p.downloadUrl ?? '';
        document.getElementById('pm_description').value = p.description ?? '';
  const st = document.getElementById('pm_status');
  const stat = (p.status || '').toLowerCase();
  let label = 'Hidden', cls = 'badge';
  if (stat === 'public') { label = 'Public'; cls = 'badge pill good'; }
  else if (stat === 'pending') { label = 'Pending'; cls = 'badge'; }
  else if (stat === 'cancelled') { label = 'Cancelled'; cls = 'badge'; }
  else { label = 'Hidden'; cls = 'badge'; }
  st.textContent = label; st.className = cls;
        // snapshot after load
        original = {
          name: p.name ?? '',
          price: p.price ?? '',
          salePrice: p.salePrice ?? '',
          quantity: p.quantity ?? 0,
          downloadUrl: p.downloadUrl ?? '',
          description: p.description ?? ''
        };
      }

      // Row clicks open modal
      document.querySelectorAll('[data-product-id]').forEach(row => {
        row.addEventListener('click', () => { const id = row.getAttribute('data-product-id'); loadProduct(id).then(() => openModal(productModal)); });
      });

      // Add product
      document.getElementById('btnAddProduct')?.addEventListener('click', () => {
        document.getElementById('pm_productId').value = '';
        document.getElementById('pm_name').value = '';
        document.getElementById('pm_price').value = '';
        document.getElementById('pm_salePrice').value = '';
        document.getElementById('pm_quantity').value = 0;
        document.getElementById('pm_downloadUrl').value = '';
        document.getElementById('pm_description').value = '';
        const st = document.getElementById('pm_status'); st.textContent = 'Hidden'; st.className = 'badge';
        openModal(productModal);
      });

      // Save product (create/update)
      document.getElementById('productForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('pm_productId').value;
        const payload = {
          sellerId: sellerId,
          name: document.getElementById('pm_name').value,
          price: document.getElementById('pm_price').value ? Number(document.getElementById('pm_price').value) : null,
          salePrice: document.getElementById('pm_salePrice').value ? Number(document.getElementById('pm_salePrice').value) : null,
          quantity: document.getElementById('pm_quantity').value ? Number(document.getElementById('pm_quantity').value) : 0,
          downloadUrl: document.getElementById('pm_downloadUrl').value || null,
          description: document.getElementById('pm_description').value || null
        };
        // Detect changes vs original snapshot
        const changed = !original ||
          original.name !== (payload.name || '') ||
          String(original.price ?? '') !== String(payload.price ?? '') ||
          String(original.salePrice ?? '') !== String(payload.salePrice ?? '') ||
          Number(original.quantity ?? 0) !== Number(payload.quantity ?? 0) ||
          (original.downloadUrl || '') !== (payload.downloadUrl || '') ||
          (original.description || '') !== (payload.description || '');
        if (id && original && !changed) {
          showToast('Không có thay đổi nào để lưu', 'info');
          return;
        }
        const res = await fetch(id ? `/api/products/${id}` : '/api/products', {
          method: id ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (res.status === 304) { showToast('Không có thay đổi nào để lưu', 'info'); return; }
        if (res.ok) { closeModal(productModal); showToast(id ? 'Đã lưu sản phẩm (đã chuyển sang ẩn)' : 'Đã tạo sản phẩm', 'success'); setTimeout(() => location.reload(), 350); }
        else { showToast('Lưu sản phẩm thất bại', 'error'); }
      });

      // Publish button logic per requirements
      document.getElementById('pm_publish')?.addEventListener('click', async () => {
        const id = document.getElementById('pm_productId').value;
        if (!id) return;
        const stText = (document.getElementById('pm_status')?.textContent || '').trim();
        const st = stText.toLowerCase();
        if (st === 'public') {
          showToast('Sản phẩm đã được duyệt', 'info');
          return;
        }
        if (st === 'pending') {
          showToast('Sản phẩm đang chờ duyệt', 'info');
          return;
        }
        // hidden -> request pending
        const res = await fetch(`/api/products/${id}/pending`, { method: 'POST' });
        if (res.ok) { closeModal(productModal); showToast('Đã gửi duyệt: trạng thái Pending', 'success'); setTimeout(() => location.reload(), 350); }
        else { showToast('Không thể chuyển sang Pending', 'error'); }
      });

      // Delete product
      document.getElementById('pm_delete')?.addEventListener('click', async () => {
        const id = document.getElementById('pm_productId').value;
        if (!id) { closeModal(productModal); return; }
        if (!confirm('Xóa sản phẩm này?')) return;
        const res = await fetch(`/api/products/${id}`, { method: 'DELETE' });
        if (res.ok) { closeModal(productModal); showToast('Đã xóa sản phẩm', 'success'); setTimeout(() => location.reload(), 350); }
        else { showToast('Xóa sản phẩm thất bại', 'error'); }
      });
    }

    // Order modal handlers (view-only)
    const orderModal = document.getElementById('orderModal');
    if (orderModal) {
      orderModal.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(orderModal); });
      orderModal.addEventListener('close', () => {
        const remaining = Array.from(document.querySelectorAll('dialog.modal[open]'));
        if (!remaining.length) {
          if (overlay) { overlay.classList.remove('visible'); overlay.hidden = true; }
          unlockScroll();
        }
      });
      orderModal.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(orderModal)));

      async function loadOrder(id) {
        const sellerIdEl = document.getElementById('sellerId');
        const sellerId = sellerIdEl ? sellerIdEl.textContent.trim() : '';
  const res = await fetch(`/api/seller/orders/${id}` + (sellerId ? `?sellerId=${encodeURIComponent(sellerId)}` : ''));
        if (!res.ok) { showToast('Không tải được chi tiết đơn hàng', 'error'); return; }
        const data = await res.json();
        const o = data.order;
        const user = data.user || {};
        document.getElementById('om_orderId').textContent = o.orderId;
        document.getElementById('om_userId').textContent = user.username || (`User #${o.userId ?? ''}`);
        // Total Amount should reflect database value directly with safe formatting
        const amtVal = o.totalAmount;
        const amt = (amtVal == null) ? '' : Number(amtVal).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        document.getElementById('om_totalAmount').textContent = amt;
        document.getElementById('om_createdAt').textContent = o.createdAt ?? '';
        const items = data.items || [];
        const tb = document.getElementById('om_items');
        tb.innerHTML = '';
        for (const it of items) {
          const tr = document.createElement('tr');
          const name = it.productName || `#${it.productId}`;
          tr.innerHTML = `<td>${name}</td><td>${it.quantity}</td><td>${it.priceAtTime}</td>`;
          tb.appendChild(tr);
        }
      }

      document.querySelectorAll('[data-order-id]').forEach(row => {
        row.addEventListener('click', () => { const id = row.getAttribute('data-order-id'); loadOrder(id).then(() => openModal(orderModal)); });
      });

      // View-only: no save/delete handlers for orders
    }

    // Load "My Products" list
    (async function loadMyProducts() {
      const sellerIdEl = document.getElementById('sellerId');
      const sellerId = sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null;
      if (!sellerId) return; // require seller
  const res = await fetch(`/api/products?sellerId=${sellerId}`);
      if (!res.ok) { showToast('Không tải được danh sách sản phẩm của bạn', 'error'); return; }
      const list = await res.json();
      const tbody = document.getElementById('tbMyProducts');
      const counter = document.getElementById('myProductsCount');
      if (!tbody) return;
      tbody.innerHTML = '';
      list.forEach(p => {
        const tr = document.createElement('tr');
        tr.className = 'clickable';
        tr.setAttribute('data-product-id', p.productId);
        const stat = (p.status || '').toLowerCase();
        let statusHtml = '<span class="badge">Hidden</span>';
        if (stat === 'public') statusHtml = '<span class="pill good">Public</span>';
        else if (stat === 'pending') statusHtml = '<span class="badge">Pending</span>';
        else if (stat === 'cancelled') statusHtml = '<span class="badge">Cancelled</span>';
        const price = (p.price ?? 0).toLocaleString('en-US');
        tr.innerHTML = `<td>${p.productId}</td><td>${p.name ?? ''}</td><td>$${price}</td><td class="hide-md">${p.quantity ?? 0}</td><td>${statusHtml}</td>`;
        tbody.appendChild(tr);
      });
      if (counter) counter.textContent = list.length;
      // rebind row click to open product modal
      document.querySelectorAll('#tbMyProducts [data-product-id]').forEach(row => {
        row.addEventListener('click', () => {
          const id = row.getAttribute('data-product-id');
          (async () => {
            const res = await fetch(`/api/products/${id}`); if (!res.ok) { showToast('Không tải được chi tiết sản phẩm', 'error'); return; } const p = await res.json();
            document.getElementById('pm_productId').value = p.productId ?? '';
            document.getElementById('pm_name').value = p.name ?? '';
            document.getElementById('pm_price').value = p.price ?? '';
            document.getElementById('pm_salePrice').value = p.salePrice ?? '';
            document.getElementById('pm_quantity').value = p.quantity ?? 0;
            document.getElementById('pm_downloadUrl').value = p.downloadUrl ?? '';
            document.getElementById('pm_description').value = p.description ?? '';
            const st = document.getElementById('pm_status');
            const stat = (p.status || '').toLowerCase();
            let label = 'Hidden', cls = 'badge';
            if (stat === 'public') { label = 'Public'; cls = 'badge pill good'; }
            else if (stat === 'pending') { label = 'Pending'; cls = 'badge'; }
            else if (stat === 'cancelled') { label = 'Cancelled'; cls = 'badge'; }
            else { label = 'Hidden'; cls = 'badge'; }
            st.textContent = label; st.className = cls;
            openModal(productModal);
          })();
        });
      });
      const pager = document.getElementById('pgMyProducts');
      if (pager) paginateTable(tbody, pager, 5);
      showToast(`Tải ${list.length} sản phẩm của bạn`, 'info', { duration: 2000 });
    })();

    // === Avatar edit button hover ===
    const avatarWrap = document.querySelector('.avatar-edit-wrap');
    const editAvatarBtn = document.getElementById('btnEditAvatar');
    if (avatarWrap && editAvatarBtn) {
      avatarWrap.addEventListener('mouseenter', () => { editAvatarBtn.style.display = 'block'; });
      avatarWrap.addEventListener('mouseleave', () => { editAvatarBtn.style.display = 'none'; });
      editAvatarBtn.addEventListener('click', () => {
        // Mở modal chỉnh sửa profile (tập trung vào input avatar)
        const btnOpen = document.getElementById('btnOpenEditProfile');
        if (btnOpen) btnOpen.click();
        setTimeout(() => {
          const input = document.getElementById('pf_avatarUrl');
          if (input) input.focus();
        }, 300);
      });
    }

    // === Profile panel toggle (show profile in-place without navigation) ===
    const dashboardContent = document.getElementById('dashboardContent');
    const profilePanel = document.getElementById('profilePanel');
    const profileLink = Array.from(document.querySelectorAll('.menu a')).find(a => a.getAttribute('href') === '#profile');
  const manageLink = Array.from(document.querySelectorAll('.menu a')).find(a => a.getAttribute('href') === '/seller/dashboard');
    const backBtn = document.getElementById('btnBackToDashboard');

    function showProfile(pushState = true) {
      if (!profilePanel || !dashboardContent) return;
      dashboardContent.style.display = 'none';
      profilePanel.hidden = false;
      profilePanel.style.display = '';
      // set active class on menu
      document.querySelectorAll('.menu a').forEach(a => a.classList.remove('active'));
      if (profileLink) profileLink.classList.add('active');
      if (pushState) {
        try { history.replaceState({}, '', '#profile'); } catch (e) {}
      }
      // optional: scroll to top of profile panel
      profilePanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hideProfile() {
      if (!profilePanel || !dashboardContent) return;
      profilePanel.hidden = true;
      profilePanel.style.display = 'none';
      dashboardContent.style.display = '';
      // restore active class to dashboard link
      document.querySelectorAll('.menu a').forEach(a => a.classList.remove('active'));
      const dash = Array.from(document.querySelectorAll('.menu a')).find(a => a.getAttribute('href') === '/seller/dashboard');
      if (dash) dash.classList.add('active');
  try { history.replaceState({}, '', (location.pathname || '/seller/dashboard') + (location.search || '')); } catch (e) {}
      // scroll back to top of dashboard content
      dashboardContent.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    // Bind clicks
    profileLink?.addEventListener('click', (ev) => { ev.preventDefault(); showProfile(true); });
    // Avoid page reload when clicking "Quản lý" if we are already on this page
    manageLink?.addEventListener('click', (ev) => {
      try {
        const curr = location.pathname.replace(/\/?$/, '');
        const target = new URL(manageLink.getAttribute('href'), location.origin).pathname.replace(/\/?$/, '');
        if (curr === target) {
          ev.preventDefault(); ev.stopPropagation();
          hideProfile(); // show dashboard content in-place
        }
      } catch (_) { /* noop */ }
    });
    backBtn?.addEventListener('click', () => { hideProfile(); });

    // Respect initial hash (if user opened with #profile)
    if (window.location.hash === '#profile') {
      // small timeout to allow other onReady tasks to finish
      setTimeout(() => showProfile(false), 50);
    }

    // === Generic sidebar panel switching for in-place tabs ===
    const panelMap = {
      '#profile': 'profilePanel',
      '#orders': 'ordersPanel',
      '#keys': 'keysPanel',
      '#profile-settings': 'profileSettingsPanel'
    };
    function showPanelByHash(hash) {
      // default: show dashboard
      if (!hash || !panelMap[hash]) {
        hideProfile();
        // also hide any other panels
        Object.values(panelMap).forEach(id => { const el = document.getElementById(id); if (el) { el.hidden = true; el.style.display = 'none'; } });
        return;
      }
      // hide dashboard and all panels first
      if (dashboardContent) dashboardContent.style.display = 'none';
      Object.values(panelMap).forEach(id => { const el = document.getElementById(id); if (el) { el.hidden = true; el.style.display = 'none'; } });
      // activate menu item
      document.querySelectorAll('.menu a').forEach(a => a.classList.remove('active'));
      const activeLink = Array.from(document.querySelectorAll('.menu a')).find(a => a.getAttribute('href') === hash);
      if (activeLink) activeLink.classList.add('active');
      // show target panel
      const target = document.getElementById(panelMap[hash]);
      if (target) { target.hidden = false; target.style.display = ''; target.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
      try { history.replaceState({}, '', hash); } catch (e) {}
    }
    // Intercept sidebar anchor clicks
    document.querySelectorAll('.menu a[href^="#"]').forEach(a => {
      a.addEventListener('click', (e) => { e.preventDefault(); const h = a.getAttribute('href'); showPanelByHash(h); });
    });
    // If hash is one of our panels (other than #profile handled earlier), show it on load
    if (window.location.hash && panelMap[window.location.hash] && window.location.hash !== '#profile') {
      setTimeout(() => showPanelByHash(window.location.hash), 50);
    }

    // === Profile edit modal ===
    const profileModal = document.getElementById('profileModal');
    if (profileModal) {
      // reuse existing overlay + lock helpers
      profileModal.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(profileModal); });
      profileModal.addEventListener('close', () => {
        const remaining = Array.from(document.querySelectorAll('dialog.modal[open]'));
        if (!remaining.length) { if (overlay) { overlay.classList.remove('visible'); overlay.hidden = true; } unlockScroll(); }
      });
      profileModal.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(profileModal)));

      const btnOpen = document.getElementById('btnOpenEditProfile');
      const sellerIdEl = document.getElementById('sellerId');
      const sellerId = sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null;

      async function loadProfile(id) {
        if (!id) return;
        const res = await fetch(`/api/users/${id}`);
        if (!res.ok) { showToast('Không tải được thông tin người dùng', 'error'); return; }
        const u = await res.json();
        document.getElementById('pf_userId').value = u.userId ?? '';
        document.getElementById('pf_username').value = u.username ?? '';
        document.getElementById('pf_email').value = u.email ?? '';
        document.getElementById('pf_phone').value = u.phoneNumber ?? '';
        document.getElementById('pf_avatarUrl').value = u.avatarUrl ?? '';
      }

      btnOpen?.addEventListener('click', () => { loadProfile(sellerId).then(() => openModal(profileModal)); });

      document.getElementById('profileForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('pf_userId').value || sellerId;
        const payload = {
          username: document.getElementById('pf_username').value,
          email: document.getElementById('pf_email').value,
          phoneNumber: document.getElementById('pf_phone').value,
          avatarUrl: document.getElementById('pf_avatarUrl').value
        };
        const res = await fetch(`/api/users/${id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        if (!res.ok) { showToast('Cập nhật thất bại', 'error'); return; }
        const u = await res.json();
        // reflect changes in profile panel UI
        const elU = document.getElementById('profile_username'); if (elU) elU.textContent = u.username || '-';
        const elE = document.getElementById('profile_email'); if (elE) elE.textContent = u.email || '-';
        const elP = document.getElementById('profile_phone'); if (elP) elP.textContent = u.phoneNumber || '-';
        // update avatar
        const avatarWrap = document.querySelector('#profilePanel .avatar');
        if (avatarWrap) {
          avatarWrap.innerHTML = '';
          const img = document.createElement('img');
          img.alt = 'Avatar'; img.style.width = '56px'; img.style.height = '56px'; img.style.borderRadius = '50%'; img.style.objectFit = 'cover';
          img.src = (u.avatarUrl && u.avatarUrl.trim().length) ? u.avatarUrl : '/img/avatar_default.jpg';
          avatarWrap.appendChild(img);
        }
        closeModal(profileModal);
        showToast('Đã cập nhật trang cá nhân', 'success');
      });
    }
  });
})();
