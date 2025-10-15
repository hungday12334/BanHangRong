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
          label: 'Revenue',
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
  toast.innerHTML = `<span class="icon"><i class="${icon}"></i></span><div class="msg">${message}</div><div class="act"><button class="close" aria-label="Close">✕</button></div>`;
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
          b.setAttribute('aria-label', `Page ${page}`);
          b.addEventListener('click', () => { current = page; render(); /* keep pager fixed: avoid scrollIntoView */ });
          return b;
        };
        pager.appendChild(mkBtn('«', Math.max(1, current - 1), current === 1, false));
        // Sliding window: show up to 10 consecutive pages.
        const windowSize = 10;
        let start = 1;
        if (totalPages <= windowSize) {
          start = 1;
        } else {
          if (current <= windowSize) {
            start = 1;
          } else {
            start = current - (windowSize - 1); // e.g. current=11 -> start=2 (2..11)
            // ensure window doesn't overflow at high end
            if (start > totalPages - windowSize + 1) start = totalPages - windowSize + 1;
          }
        }
        const end = Math.min(totalPages, start + windowSize - 1);
        for (let i = start; i <= end; i++) {
          const btn = mkBtn(String(i), i, false, i === current);
          btn.classList.add('page-btn');
          if (i === current) btn.classList.add('active');
          pager.appendChild(btn);
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

  // Logo fallback: if the image fails to load or is fully transparent -> show text fallback
  function initLogoFallback() {
    const fig = document.querySelector('.app-logo');
    if (!fig) return;
    const img = fig.querySelector('img');
    if (!img) return;
    let done = false;
    function toFallback(reason) {
      if (done) return; done = true;
      fig.classList.add('fallback');
      fig.innerHTML = '<span>BR</span>'; // viết tắt Bán Rong
      if (reason) console.warn('Logo fallback:', reason);
    }
    img.addEventListener('error', () => toFallback('error'));
    // Detect blank (all white) or fully transparent by sampling once it loads
    img.addEventListener('load', () => {
      try {
        const canvas = document.createElement('canvas');
        const w = canvas.width = img.naturalWidth;
        const h = canvas.height = img.naturalHeight;
        if (!w || !h) { toFallback('zero-size'); return; }
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        const data = ctx.getImageData(0,0,w,h).data;
        let sum = 0, opaque = 0;
        for (let i=0;i<data.length;i+=4){
          const r=data[i], g=data[i+1], b=data[i+2], a=data[i+3];
            if (a>12) opaque++;
            sum += r+g+b;
        }
        const avg = sum / ( (data.length/4) * 3 );
        if (opaque < (data.length/4)*0.05 || avg > 250) {
          toFallback('blank/transparent');
        }
      } catch(e){ console.debug('Logo analysis skipped', e); }
    }, { once:true });
  }

  function applyTheme(theme) {
    const root = document.documentElement;
    // Add temporary transition class for smoother theme swap
    root.classList.add('theme-transition');
    // Prepare radial wipe overlay (capture current background color before class change)
    let oldBg = getComputedStyle(root).getPropertyValue('--bg').trim();
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
      btn.setAttribute('aria-label', 'Toggle theme');
      btn.title = 'Toggle theme';
      btn.dataset.mode = theme;
    }
    // swap logo variant
    const logoImg = document.querySelector('.app-logo-img');
    if (logoImg) {
      const lightSrc = logoImg.getAttribute('data-logo-light');
      const darkSrc = logoImg.getAttribute('data-logo-dark');
      if (theme === 'dark' && darkSrc) {
        logoImg.src = darkSrc;
      } else if (lightSrc) {
        logoImg.src = lightSrc;
      }
    }
    // loader nucleus logo swap
    const loaderLogo = document.querySelector('#appLoader .loader-logo');
    if (loaderLogo) {
      const lightSrc = loaderLogo.getAttribute('data-logo-light');
      const darkSrc = loaderLogo.getAttribute('data-logo-dark');
      if (theme === 'dark' && darkSrc) loaderLogo.src = darkSrc; else if (lightSrc) loaderLogo.src = lightSrc;
    }
    // Create cross-fade overlay + subtle body pop
    try {
      const layer = document.createElement('div');
      layer.className = 'theme-switch-layer';
      document.body.appendChild(layer);
      document.body.classList.add('theme-switching');
      setTimeout(()=> { layer.remove(); document.body.classList.remove('theme-switching'); }, 620);
    } catch(_) {}
    // Remove transition class after a short delay
    setTimeout(() => root.classList.remove('theme-transition'), 600);
  }

  onReady(function () {
    initLogoFallback();
    const loadStarted = performance.now();
    const MIN_LOAD = 1400; // ms (slightly longer to showcase enhanced animation)
    const appLoader = document.getElementById('appLoader');
    const progressBar = appLoader?.querySelector('[data-loader-progress]');
    const loadTextEl = appLoader?.querySelector('[data-loader-text]');
    const tipEl = appLoader?.querySelector('[data-loader-tip]');
    const TIPS = [
      'Tip: You can switch panels quickly using the URL #hash.',
      'Tip: Click the sun/moon icon to toggle theme.',
      'Tip: Use filters to narrow down results.',
      'Info: Metrics will refresh periodically.',
      'Tip: Scroll to the bottom to load more data (if available).'
    ];
    let tipIndex = 0;
    function cycleTip() {
      if (!tipEl) return;
      tipEl.textContent = TIPS[tipIndex % TIPS.length];
      tipIndex++;
    }
    cycleTip();
    const tipTimer = setInterval(cycleTip, 6500);

    let simulated = 0;
    let done = false;
    function tickProgress() {
      if (done) return;
      // accelerate slower after 70%
      const inc = simulated < 70 ? (4 + Math.random()*6) : (1 + Math.random()*3);
      simulated = Math.min(simulated + inc, 94); // stop at 94% until finish
      if (progressBar) progressBar.style.width = simulated + '%';
      if (loadTextEl) {
    if (simulated < 30) loadTextEl.textContent = 'Initializing...';
    else if (simulated < 55) loadTextEl.textContent = 'Loading data...';
    else if (simulated < 80) loadTextEl.textContent = 'Processing metrics...';
    else loadTextEl.textContent = 'Preparing view...';
      }
      setTimeout(tickProgress, 260 + Math.random()*240);
    }
    tickProgress();

    function finishGlobalLoad() {
      const elapsed = performance.now() - loadStarted;
      const remain = Math.max(0, MIN_LOAD - elapsed);
      setTimeout(() => {
        done = true;
        if (progressBar) progressBar.style.width = '100%';
          if (loadTextEl) loadTextEl.textContent = 'Finished!';
        document.body.classList.remove('loading');
        document.body.classList.add('ready');
        if (appLoader) {
          appLoader.style.opacity = '0';
          setTimeout(()=> { clearInterval(tipTimer); appLoader.remove(); }, 600);
        }
        // Start animations AFTER loader removed
        document.querySelectorAll('[data-count]').forEach(animateCount);
        initChart();
        document.querySelectorAll('.progress span').forEach(span => {
          const w = span.getAttribute('data-target-width') || span.style.width || '0%';
          span.style.width = '0%'; requestAnimationFrame(()=> span.style.width = w);
        });
      }, remain);
    }

    // Delay KPI + chart start until finishGlobalLoad
    // Replace initial progress width capture
    document.querySelectorAll('.progress span').forEach(span => {
      span.setAttribute('data-target-width', span.style.width || '0%');
      span.style.width = '0%';
    });

    // Expose for debugging
    window.__finishGlobalLoad = finishGlobalLoad;

    // Panel loading helper
    function withPanelLoading(panelEl, task, fallbackMsg) {
      if (!panelEl) return;
      let overlay = panelEl.querySelector(':scope > .panel-loading-overlay');
      if (!overlay) {
  overlay = document.createElement('div');
  overlay.className = 'panel-loading-overlay';
  overlay.innerHTML = '<div class="mini-spinner"></div><div>Loading...</div>';
        panelEl.appendChild(overlay);
      }
      // Ensure overlay is visible and blocks interaction while loading
      overlay.hidden = false;
      overlay.style.pointerEvents = 'auto';
      overlay.style.transition = overlay.style.transition || 'opacity .28s ease';
      // force reflow then set opacity to 1
      void overlay.offsetWidth; overlay.style.opacity = '1';
      const MIN_PANEL = 500;
      const started = performance.now();

      function cleanupOverlay() {
        try {
          // remove overlay from DOM to avoid any accidental blocking
          if (overlay && overlay.parentElement) overlay.parentElement.removeChild(overlay);
        } catch (_) {}
      }

      Promise.resolve().then(task).catch(err => {
        console.error(err);
        if (fallbackMsg) panelEl.querySelectorAll('tbody').forEach(tb => tb.innerHTML = `<tr><td colspan="10" class="empty-state">${fallbackMsg}</td></tr>`);
      }).finally(() => {
        const elapsed = performance.now() - started;
        const wait = Math.max(0, MIN_PANEL - elapsed);
        setTimeout(() => {
          // fade out and then remove from DOM
          overlay.style.opacity = '0';
          overlay.style.pointerEvents = 'none';
          const onFinish = () => { try { cleanupOverlay(); } catch(_){} };
          // If transition finishes, remove then; otherwise fallback timeout
          const removeAfter = 360;
          let fired = false;
          const handler = () => { if (fired) return; fired = true; onFinish(); };
          overlay.addEventListener('transitionend', handler, { once: true });
          setTimeout(() => { handler(); }, removeAfter);
        }, wait);
      });
    }

    // Remove old hash handler (merged into showPanelByHash later)

    // Defer finish until next frame to allow initial layout
    requestAnimationFrame(finishGlobalLoad);
    // (Animations moved to finishGlobalLoad)

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
          showToast(next === 'light' ? 'Switched to light theme' : 'Switched to dark theme', 'info', { duration: 1500 });
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

  // === Product snapshot & helpers (exposed for reuse) ===
  let __originalProduct = null; // snapshot of the product being edited
    function normalizeProductObj(p) {
      return {
        name: (p.name ?? '').trim(),
        price: p.price != null ? Number(p.price) : null,
        salePrice: p.salePrice != null ? Number(p.salePrice) : null,
        quantity: p.quantity != null ? Number(p.quantity) : 0,
        downloadUrl: (p.downloadUrl ?? '').trim(),
        description: (p.description ?? '').trim(),
        status: (typeof p.status === 'string' && p.status) ? p.status : 'pending'
      };
    }
    function collectFormProduct() {
      return normalizeProductObj({
        name: document.getElementById('pm_name')?.value,
        price: document.getElementById('pm_price')?.value ? Number(document.getElementById('pm_price').value) : null,
        salePrice: document.getElementById('pm_salePrice')?.value ? Number(document.getElementById('pm_salePrice').value) : null,
        quantity: document.getElementById('pm_quantity')?.value ? Number(document.getElementById('pm_quantity').value) : 0,
        downloadUrl: document.getElementById('pm_downloadUrl')?.value || '',
        description: document.getElementById('pm_description')?.value || '',
  status: (document.getElementById('pm_status')?.dataset.status || '').trim()
      });
    }
    function productChanged() {
      if (!__originalProduct) return true; // new product coi như có thay đổi
      const now = collectFormProduct();
    return Object.keys(__originalProduct).some(k => __originalProduct[k] !== now[k] || (k === 'status' && now[k] === 'pending'));
    }
    async function loadProduct(id) {
      const res = await fetch(`/api/products/${id}`);
  if (!res.ok) { showToast('Failed to load product details', 'error'); return; }
      const p = await res.json();
        document.getElementById('pm_productId').value = p.productId || '';
      document.getElementById('pm_name').value = p.name ?? '';
      document.getElementById('pm_price').value = p.price ?? '';
      document.getElementById('pm_salePrice').value = p.salePrice ?? '';
      document.getElementById('pm_quantity').value = p.quantity ?? 0;
      document.getElementById('pm_downloadUrl').value = p.downloadUrl ?? '';
      document.getElementById('pm_description').value = p.description ?? '';
      const st = document.getElementById('pm_status');
      if (st) {
        const stVal = (p.status || '').toString().toLowerCase();
        let statusText = 'Pending';
        let badgeClass = 'badge';
        if (stVal === 'public') { statusText = 'Public'; badgeClass = 'badge pill good'; }
        else if (stVal === 'hidden') { statusText = 'Hidden'; badgeClass = 'badge'; }
        st.textContent = statusText;
        st.className = badgeClass;
        st.dataset.status = stVal;
      }
      __originalProduct = normalizeProductObj(p);
    }

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
  const userIdEl = document.getElementById('userId');
  const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);

      // (định nghĩa đã đưa ra ngoài khối if)

      // Row clicks open modal
      document.querySelectorAll('[data-product-id]').forEach(row => {
        row.addEventListener('click', () => { const id = row.getAttribute('data-product-id'); loadProduct(id).then(() => openModal(productModal)); });
      });

      // Add product
      document.getElementById('btnAddProduct')?.addEventListener('click', () => {
        // Clear form for new product
        document.getElementById('pm_productId').value = '';
        document.getElementById('pm_name').value = '';
        document.getElementById('pm_price').value = '';
        document.getElementById('pm_salePrice').value = '';
        document.getElementById('pm_quantity').value = 0;
        document.getElementById('pm_downloadUrl').value = '';
        document.getElementById('pm_description').value = '';
  // Default: Public (do not hide automatically if user hasn't changed anything)
  const st = document.getElementById('pm_status');
  st.textContent = 'Pending';
  st.className = 'badge';
  st.dataset.status = 'pending';
  __originalProduct = null; // new product -> always treat as create
        openModal(productModal);
      });

      // Save product (create/update)
      document.getElementById('productForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('pm_productId').value;
  // If editing & no changes, skip API call to avoid backend switching status to Hidden
        if (id && !productChanged()) {
          showToast('No changes to save', 'info');
          closeModal(productModal);
          return;
        }
        const payload = {
          sellerId: sellerId,
          name: document.getElementById('pm_name').value,
          price: document.getElementById('pm_price').value ? Number(document.getElementById('pm_price').value) : null,
          salePrice: document.getElementById('pm_salePrice').value ? Number(document.getElementById('pm_salePrice').value) : null,
          quantity: document.getElementById('pm_quantity').value ? Number(document.getElementById('pm_quantity').value) : 0,
          downloadUrl: document.getElementById('pm_downloadUrl').value || null,
          description: document.getElementById('pm_description').value || null,
          status: document.getElementById('pm_status').dataset.status
        };
        const res = await fetch(id ? `/api/products/${id}` : '/api/products', {
          method: id ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
  if (res.ok) { closeModal(productModal); showToast(id ? 'Product saved' : 'Product created', 'success'); setTimeout(() => refreshMyProducts(), 350); }
    else { showToast('Failed to save product', 'error'); }
      });

      // Publish / gửi duyệt: Seller-only app -> luôn gửi duyệt (pending nếu không phải public)
      document.getElementById('pm_publish')?.addEventListener('click', async () => {
        const id = document.getElementById('pm_productId').value;
        if (!id) return;
        const statusText = document.getElementById('pm_status').textContent;
        // Nếu sản phẩm đang ở trạng thái Public và không có thay đổi nào -> bấm duyệt sẽ KHÔNG thay đổi trạng thái
        if (statusText === 'Public' && !productChanged()) {
          // Seller: nếu sản phẩm đang Public và không thay đổi gì -> không làm gì cả
          showToast('Product is already Public (no changes)', 'info');
          closeModal(productModal);
          return; // No-op
        }
        // Seller-only: gửi publish=false để tránh tự public
        const res = await fetch(`/api/products/${id}/approval?publish=${statusText !== 'Public'}`, {
          method: 'POST',
          headers: { 'X-User-Type': 'SELLER' }
        });
        if (res.ok) {
          closeModal(productModal);
          showToast('Approval request sent (status = pending)', 'success');
          setTimeout(() => refreshMyProducts(), 350);
        } else {
          showToast('Approval/publish action failed', 'error');
        }
      });

      // Delete product
      document.getElementById('pm_delete')?.addEventListener('click', async () => {
        const id = document.getElementById('pm_productId').value;
        if (!id) { closeModal(productModal); return; }
    if (!confirm('Delete this product?')) return;
        const res = await fetch(`/api/products/${id}`, { method: 'DELETE' });
  if (res.ok) { closeModal(productModal); showToast('Product deleted', 'success'); setTimeout(() => refreshMyProducts(), 350); }
    else { showToast('Failed to delete product', 'error'); }
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
        // Prefer seller-scoped endpoint when sellerId is available to avoid leaking other sellers' items
        const sidEl = document.getElementById('sellerId');
        const sid = sidEl ? Number(sidEl.textContent.trim()) : null;
        const url = sid ? `/api/seller/${sid}/orders/${id}` : `/api/orders/${id}`;
        const res = await fetch(url);
    if (!res.ok) { showToast('Failed to load order details', 'error'); return; }
        const data = await res.json();
        const o = data.order || {};
        const user = data.user || {};
        document.getElementById('om_orderId').textContent = o.orderId;
        document.getElementById('om_userId').textContent = user.username || (user.userId ? `User #${user.userId}` : '');
        // In seller view, show sellerAmount if present; fallback to totalAmount
        const amtVal = (o.sellerAmount != null ? o.sellerAmount : o.totalAmount);
        const amt = (amtVal == null) ? '' : Number(amtVal).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        document.getElementById('om_totalAmount').textContent = amt;
        document.getElementById('om_createdAt').textContent = o.createdAt ?? '';
        const items = Array.isArray(data.items) ? data.items : (data.items && data.items.content ? data.items.content : []);
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

    // Load "My Products" list (reusable for refresh after CRUD)
    async function refreshMyProducts(showToastMsg = true) {
  const sellerIdEl = document.getElementById('sellerId');
  const userIdEl = document.getElementById('userId');
  const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
      if (!sellerId) return; // require seller
      const res = await fetch(`/api/products?sellerId=${sellerId}`);
      if (!res.ok) { showToast('Failed to load your product list', 'error'); return; }
      const list = await res.json();
      const tbody = document.getElementById('tbMyProducts');
      const counter = document.getElementById('myProductsCount');
      if (!tbody) return;
      tbody.innerHTML = '';
      list.forEach(p => {
        const tr = document.createElement('tr');
        tr.className = 'clickable';
        tr.setAttribute('data-product-id', p.productId);
  const stVal = (p.status || '').toString().toLowerCase();
  let statusHtml = '<span class="badge">Pending</span>';
  if (stVal === 'public') statusHtml = '<span class="pill good">Public</span>';
  else if (stVal === 'hidden') statusHtml = '<span class="badge">Hidden</span>';
  const price = (p.price ?? 0).toLocaleString('en-US');
  tr.innerHTML = `<td>${p.productId}</td><td>${p.name ?? ''}</td><td>$${price}</td><td class="hide-md">${p.quantity ?? 0}</td><td>${statusHtml}</td>`;
        tbody.appendChild(tr);
      });
      if (counter) counter.textContent = list.length;
      // rebind row click to open product modal
      document.querySelectorAll('#tbMyProducts [data-product-id]').forEach(row => {
        row.addEventListener('click', () => {
          const id = row.getAttribute('data-product-id');
          loadProduct(id).then(() => openModal(productModal));
        });
      });
      const pager = document.getElementById('pgMyProducts');
      if (pager) paginateTable(tbody, pager, 5);
      if (showToastMsg) showToast(`Loaded ${list.length} of your products`, 'info', { duration: 2000 });
    }

    // initial load
    refreshMyProducts(false);

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
      // ALSO hide other sidebar panels (orders, keys, productpanel) to prevent residual content
      ['ordersPanel','keysPanel','profileSettingsPanel','productsPanel'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.hidden = true; el.style.display = 'none'; }
      });
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
      '#products': 'productsPanel',
      '#gen-keys': 'generateKeysPanel',
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
      // NEW: tự động load dữ liệu khi chuyển panel
      try {
        if (hash === '#orders' && typeof loadSellerOrders === 'function') {
          loadSellerOrders(true);
        } else if (hash === '#keys' && typeof loadSellerKeys === 'function') {
          loadSellerKeys(true);
        } else if (hash === '#gen-keys' && typeof initGenerateKeys === 'function') {
          initGenerateKeys();
        } else if (hash === '#products' && typeof loadProductsPanel === 'function') {
          loadProductsPanel(true);
        }
      } catch (_) { /* ignore */ }
    }
    // Intercept sidebar anchor clicks
    document.querySelectorAll('.menu a[href^="#"]').forEach(a => {
      a.addEventListener('click', (e) => { e.preventDefault(); const h = a.getAttribute('href'); showPanelByHash(h); /* load handled inside showPanelByHash */ });
    });
    window.addEventListener('hashchange', () => showPanelByHash(window.location.hash));
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
        if (!res.ok) { showToast('Failed to load user info', 'error'); return; }
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
  if (!res.ok) { showToast('Failed to update profile', 'error'); return; }
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
  showToast('Profile updated', 'success');
      });
    }

    // === WebSocket realtime order notifications ===
    (function initOrderSocket() {
      const proto = (location.protocol === 'https:') ? 'wss:' : 'ws:';
      const url = proto + '//' + location.host + '/ws/orders';
      let ws;
      let retry = 0;
      const maxRetry = 6;
      function connect() {
        ws = new WebSocket(url);
  ws.onopen = () => { retry = 0; showToast('Connected to realtime orders', 'info', { duration: 1500 }); };
        ws.onmessage = (ev) => {
          try {
            const data = JSON.parse(ev.data);
            if (data && data.type === 'new-order' && data.data) {
              const id = data.data.orderId;
              const amt = data.data.totalAmount;
              const formatted = (amt == null) ? '' : ('$' + Number(amt).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
              showToast(`New order #${id} ${formatted}`, 'success');
              // Optionally: refresh recent orders list (lightweight approach: reload after short delay)
              // Could implement incremental prepend instead of reload; keep simple first.
              setTimeout(() => { try { refreshMyProducts(); } catch (e) { } }, 1200);
            }
          } catch (_) { /* ignore parse errors */ }
        };
        ws.onclose = () => {
          if (retry < maxRetry) {
            const delay = Math.min(1000 * Math.pow(2, retry), 10000);
            retry++;
            setTimeout(connect, delay);
          } else {
            showToast('Realtime connection lost', 'error', { duration: 4000 });
          }
        };
        ws.onerror = () => { try { ws.close(); } catch (_) { } };
      }
      // Only init on dashboard main view (avoid duplicate when showing profile panels)
      if (document.getElementById('dashboardContent')) {
        connect();
      }
    })();

    // ================= Seller Orders Panel (dynamic load) =================
  const sellerIdEl = document.getElementById('sellerId');
  const userIdEl = document.getElementById('userId');
  // Prefer explicit userId when available (new behavior). Fall back to sellerId for backward compatibility.
  const sellerIdVal = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
    const ordersTbody = document.getElementById('tbSellerOrders');
    const ordersPager = document.getElementById('pgSellerOrders');
    let ordersPageState = { page: 0, size: 10, totalPages: 0 };

    async function loadSellerOrders(resetPage = false) {
      if (!sellerIdVal || !ordersTbody) return;
      if (resetPage) ordersPageState.page = 0;
      const params = new URLSearchParams();
      params.set('page', ordersPageState.page);
      params.set('size', ordersPageState.size);
      const s = document.getElementById('ord_search')?.value.trim();
      const from = document.getElementById('ord_from')?.value;
      const to = document.getElementById('ord_to')?.value;
      if (s) params.set('search', s);
      if (from) params.set('from', from);
      if (to) params.set('to', to);
  const res = await fetch(`/api/seller/${sellerIdVal}/orders?` + params.toString());
  if (!res.ok) { showToast('Failed to load orders', 'error'); return; }
      const data = await res.json();
      ordersPageState.totalPages = data.totalPages;
      ordersTbody.innerHTML = '';
      data.content.forEach(o => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${o.orderId}</td>` +
          `<td>${o.createdAt ? o.createdAt.replace('T',' ') : ''}</td>` +
          `<td>${o.buyerUsername ? o.buyerUsername : (o.buyerUserId ? ('User #' + o.buyerUserId) : '')}</td>` +
          `<td>${o.sellerItems ?? 0}</td>` +
          `<td>$${(o.sellerAmount ?? 0).toLocaleString('en-US', {minimumFractionDigits:2, maximumFractionDigits:2})}</td>`;
        tr.className = 'clickable';
        tr.addEventListener('click', () => { // open seller-scoped order detail
          const id = o.orderId;
          const orderModal = document.getElementById('orderModal');
          if (!orderModal) return;
          (async () => {
            const res = await fetch(`/api/seller/${sellerIdVal}/orders/${id}`);
            if (!res.ok) { showToast('Failed to load order details', 'error'); return; }
            const data = await res.json();
            const ord = data.order || {};
            const user = data.user || {};
            document.getElementById('om_orderId').textContent = ord.orderId;
            document.getElementById('om_userId').textContent = user.username || (user.userId ? ('User #' + user.userId) : '');
            // Use sellerAmount for seller view total
            const amtVal = ord.sellerAmount; const amt = (amtVal == null) ? '' : Number(amtVal).toLocaleString('en-US', {minimumFractionDigits:2, maximumFractionDigits:2});
            document.getElementById('om_totalAmount').textContent = amt;
            document.getElementById('om_createdAt').textContent = ord.createdAt ?? '';
            const items = data.items || []; const tb = document.getElementById('om_items'); tb.innerHTML='';
            for (const it of items) { const r=document.createElement('tr'); r.innerHTML=`<td>${it.productName||('#'+it.productId)}</td><td>${it.quantity}</td><td>${it.priceAtTime}</td>`; tb.appendChild(r); }
            const overlay = document.getElementById('modalOverlay');
            if (overlay) { overlay.hidden=false; overlay.classList.add('visible'); }
            if (typeof orderModal.showModal === 'function') { try { orderModal.showModal(); } catch (_) { orderModal.setAttribute('open',''); } }
            requestAnimationFrame(()=> orderModal.classList.add('is-open'));
          })();
        });
        ordersTbody.appendChild(tr);
      });
      paginateTable(ordersTbody, ordersPager, ordersPageState.size); // reuse for pager skeleton
      // Override pager to hook page changes via API (not just client slicing)
      if (ordersPager) {
        ordersPager.innerHTML='';
        const total = ordersPageState.totalPages;
        if (total > 1) {
          const mk = (label, page, disabled, current) => {
            const b=document.createElement('button'); b.type='button'; b.className='btn'; b.textContent=label; b.disabled=disabled; if (current) b.setAttribute('aria-current','page');
            b.addEventListener('click', () => { ordersPageState.page = page; loadSellerOrders(false); }); return b; };
          ordersPager.appendChild(mk('«', Math.max(0, ordersPageState.page-1), ordersPageState.page===0,false));
          // sliding window of up to 10 pages (1-based labels, page is 0-based)
          const wSize = 10;
          let startIdx = 0;
          if (total <= wSize) startIdx = 0; else {
            if (ordersPageState.page <= wSize - 1) startIdx = 0; else startIdx = ordersPageState.page - (wSize - 1);
            if (startIdx > total - wSize) startIdx = total - wSize;
          }
          const endIdx = Math.min(total - 1, startIdx + wSize - 1);
          for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i+1), i, false, i===ordersPageState.page); btn.classList.add('page-btn'); if (i===ordersPageState.page) btn.classList.add('active'); ordersPager.appendChild(btn); }
          ordersPager.appendChild(mk('»', Math.min(total-1, ordersPageState.page+1), ordersPageState.page===total-1,false));
        }
      }
    }

    document.getElementById('ord_btnFilter')?.addEventListener('click', () => loadSellerOrders(true));
    document.getElementById('ord_btnReset')?.addEventListener('click', () => {
      const f = document.getElementById('ord_from'); if (f) f.value='';
      const t = document.getElementById('ord_to'); if (t) t.value='';
      const s = document.getElementById('ord_search'); if (s) s.value='';
      loadSellerOrders(true);
    });
    document.getElementById('ord_search')?.addEventListener('keydown', e => { if (e.key==='Enter') { e.preventDefault(); loadSellerOrders(true);} });
    document.getElementById('ord_btnExport')?.addEventListener('click', () => {
      if (!ordersTbody) return; const rows = [['OrderId','CreatedAt','User','SellerItems','SellerAmount']];
      ordersTbody.querySelectorAll('tr').forEach(tr => { const cols=[...tr.children].map(td=> td.textContent.replace(/\s+/g,' ').trim()); if (cols.length>=5) rows.push(cols.slice(0,5)); });
      const csv = rows.map(r=> r.map(c => '"'+c.replace(/"/g,'""')+'"').join(',')).join('\r\n');
      const blob = new Blob([csv], {type:'text/csv;charset=utf-8;'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='seller_orders.csv'; a.click(); setTimeout(()=>URL.revokeObjectURL(a.href),500);
    });

    // Auto load when panel hash activated
    if (window.location.hash === '#orders') setTimeout(() => loadSellerOrders(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#orders') loadSellerOrders(false); });

    // ================= License Keys Panel =================
    const keysTbody = document.getElementById('tbSellerKeys');
    const keysPager = document.getElementById('pgSellerKeys');
    let keysPageState = { page:0, size:10, totalPages:0 };

    async function loadSellerKeys(resetPage=false) {
      if (!sellerIdVal || !keysTbody) return;
      if (resetPage) keysPageState.page = 0;
      const params = new URLSearchParams();
      params.set('page', keysPageState.page); params.set('size', keysPageState.size);
      const prod = document.getElementById('key_product')?.value; if (prod) params.set('productId', prod);
      const act = document.getElementById('key_active')?.value; if (act) params.set('active', act);
      const s = document.getElementById('key_search')?.value.trim(); if (s) params.set('search', s);
  const res = await fetch(`/api/seller/${sellerIdVal}/licenses?` + params.toString());
  if (!res.ok) { showToast('Failed to load keys', 'error'); return; }
      const data = await res.json(); keysPageState.totalPages = data.totalPages;
      keysTbody.innerHTML='';
      data.content.forEach(l => {
        const tr = document.createElement('tr');
        const activeBadge = l.isActive
          ? '<button type="button" class="pill good" data-toggle-lic="'+l.licenseId+'" title="Click to disable">ON</button>'
          : '<button type="button" class="badge" data-toggle-lic="'+l.licenseId+'" title="Click to enable">OFF</button>';
        const actDate = l.activationDate ? l.activationDate.replace('T',' ') : '';
        const deviceText = (l.deviceIdentifier && l.deviceIdentifier.trim().length)
          ? l.deviceIdentifier
          : 'unused';
        tr.innerHTML = `<td>${l.licenseId}</td>
                        <td style="font-family:monospace;">${l.licenseKey}</td>
                        <td>${l.productName||('#'+l.productId)}</td>
                        <td>${l.orderId||''}</td>
                        <td>${activeBadge}</td>
                        <td>${actDate}</td>
                        <td>${deviceText}</td>`;
        keysTbody.appendChild(tr);
      });
        if (data.content.length === 0) {
          const tr = document.createElement('tr');
          const colSpan = 7;
          tr.innerHTML = `<td colspan="${colSpan}" class="footer-note">No keys for the current seller or sellerId is incorrect.</td>`;
          keysTbody.appendChild(tr);
        }
      paginateTable(keysTbody, keysPager, keysPageState.size);
      if (keysPager) {
        keysPager.innerHTML=''; const total = keysPageState.totalPages;
        if (total>1) {
          const mk=(label,page,disabled,current)=>{ const b=document.createElement('button'); b.type='button'; b.className='btn'; b.textContent=label; b.disabled=disabled; if(current) b.setAttribute('aria-current','page'); b.addEventListener('click',()=>{ keysPageState.page=page; loadSellerKeys(false); }); return b; };
          keysPager.appendChild(mk('«', Math.max(0, keysPageState.page-1), keysPageState.page===0,false));
            {
              const wSize = 10; let startIdx = 0; if (total <= wSize) startIdx = 0; else { if (keysPageState.page <= wSize - 1) startIdx = 0; else startIdx = keysPageState.page - (wSize - 1); if (startIdx > total - wSize) startIdx = total - wSize; }
              const endIdx = Math.min(total - 1, startIdx + wSize - 1);
              for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i+1), i, false, i===keysPageState.page); btn.classList.add('page-btn'); if (i===keysPageState.page) btn.classList.add('active'); keysPager.appendChild(btn); }
            }
            keysPager.appendChild(mk('»', Math.min(total-1, keysPageState.page+1), keysPageState.page===total-1,false));
        }
      }
    }

    document.getElementById('key_btnFilter')?.addEventListener('click', () => loadSellerKeys(true));
    document.getElementById('key_btnReset')?.addEventListener('click', () => {
      const p=document.getElementById('key_product'); if (p) p.value='';
      const a=document.getElementById('key_active'); if (a) a.value='';
      const s=document.getElementById('key_search'); if (s) s.value='';
      loadSellerKeys(true);
    });
    document.getElementById('key_search')?.addEventListener('keydown', e => { if (e.key==='Enter') { e.preventDefault(); loadSellerKeys(true); } });

    // Toggle active by clicking the ON/OFF badge
    keysTbody?.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-toggle-lic]'); if (!btn) return;
      const id = btn.getAttribute('data-toggle-lic');
      // Determine current state by class name
      const isOn = btn.classList.contains('pill') && btn.classList.contains('good');
      const next = !isOn;
      const res = await fetch(`/api/seller/${sellerIdVal}/licenses/${id}`, {
        method: 'PATCH',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ isActive: next })
      });
      if (res.ok) {
        showToast(next? 'Key enabled':'Key disabled','success');
        loadSellerKeys(false);
      } else {
        showToast('Failed to update key','error');
      }
    });

    // Populate product filter select (reuse my products API)
    (async function populateProductsForKeys(){
      if (!sellerIdVal) return; const sel = document.getElementById('key_product'); if (!sel) return;
      try { const res = await fetch(`/api/products?sellerId=${sellerIdVal}`); if (!res.ok) return; const list = await res.json();
        list.forEach(p => { const o=document.createElement('option'); o.value=p.productId; o.textContent=p.name || ('#'+p.productId); sel.appendChild(o); });
      } catch (_) {}
    })();

    if (window.location.hash === '#keys') setTimeout(() => loadSellerKeys(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#keys') loadSellerKeys(false); });

    // ================= Products Panel (list + filters in-place) =================
  const productsGrid = document.getElementById('prdGrid');
  const productsPager = document.getElementById('pgProducts');
  const prdCategorySel = document.getElementById('prd_category');
  let productsPageState = { page:0, size:18, totalPages:0 };

    async function populateCategoriesOnce() {
      if (!prdCategorySel || prdCategorySel.getAttribute('data-loaded') === '1') return;
      try {
        const res = await fetch('/api/categories');
        if (!res.ok) return;
        const cats = await res.json();
        cats.forEach(c => { const o=document.createElement('option'); o.value=c.categoryId; o.textContent=c.name; prdCategorySel.appendChild(o); });
        prdCategorySel.setAttribute('data-loaded','1');
      } catch(_){}
    }

    async function loadProductsPanel(resetPage=false) {
      if (!productsGrid) return;
      if (resetPage) productsPageState.page = 0;
      await populateCategoriesOnce();

      // Compose params and a human-readable filter description
      const params = new URLSearchParams();
      params.set('page', productsPageState.page);
      params.set('size', productsPageState.size);
      const parts = [];
    const s = document.getElementById('prd_search')?.value.trim(); if (s) { params.set('search', s); parts.push(`keyword "${s}"`); }
    const cat = prdCategorySel?.value; if (cat) { params.set('categoryId', cat); const opt=prdCategorySel.options[prdCategorySel.selectedIndex]; if (opt && opt.text) parts.push(`category "${opt.text}"`); }
  const rating = document.getElementById('prd_rating')?.value; if (rating) { params.set('minRating', rating); parts.push(`rating ≥ ${rating}`); }
  const dl = document.getElementById('prd_downloads')?.value; if (dl) { params.set('minDownloads', dl); parts.push(`sold ≥ ${dl}`); }
      const statusEl = document.getElementById('prd_status');
      const statusRaw = statusEl?.value;
      if (statusRaw) {
        const statusNorm = statusRaw.toString().trim().toLowerCase();
        params.set('status', statusNorm);
  // Show friendly label in toast (capitalize first letter)
  const label = statusNorm.charAt(0).toUpperCase() + statusNorm.slice(1);
  parts.push(`status "${label}"`);
      }

      // Show loading feedback (toast) and overlay on panel
      try { if (typeof showToast === 'function') showToast('Loading products' + (parts.length? ' by ' + parts.join(', ') : ''), 'info', { duration: 1200 }); } catch(_){ }
      const panelEl = document.getElementById('productsPanel');
      const task = async () => {
        const res = await fetch('/api/products/search?' + params.toString());
        if (!res.ok) { showToast('Failed to load products', 'error'); return; }
        const data = await res.json();
        productsPageState.totalPages = data.totalPages || 1;
        productsGrid.innerHTML = '';
        (data.content || []).forEach(p => {
          const card = document.createElement('div');
          card.className = 'product-card clickable';
          card.setAttribute('data-product-id', p.productId);
          const st = (p.status||'').toLowerCase();
          let statusHtml = '<span class="badge">Pending</span>';
          if (st === 'public') statusHtml = '<span class="pill good">Public</span>';
          else if (st === 'hidden') statusHtml = '<span class="badge">Hidden</span>';
          const price = (p.price ?? 0).toLocaleString('en-US');
          const rating = (p.averageRating != null) ? Number(p.averageRating).toFixed(1) : '-';
          const totalSales = (p.totalSales != null) ? p.totalSales : 0;
          const img = (p.imageUrl && p.imageUrl.trim().length) ? p.imageUrl : '/img/no-image.png';
          card.innerHTML = `
            <div class="thumb" style="width:100%;aspect-ratio:4/3;overflow:hidden;border-radius:10px;background:#0e1430;display:flex;align-items:center;justify-content:center;">
              <img src="${img}" alt="${p.name ?? ''}" onerror="this.style.display='none'" style="width:100%;height:100%;object-fit:cover;" />
            </div>
            <div class="meta" style="padding:8px 2px;display:flex;flex-direction:column;gap:6px;">
              <div class="line" style="display:flex;justify-content:space-between;gap:8px;align-items:center;">
                <div class="name" title="${p.name ?? ''}" style="font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${p.name ?? ''}</div>
                <div class="price" style="color:#7c9eff;font-weight:700;">$${price}</div>
              </div>
              <div class="sub" style="display:flex;gap:10px;font-size:12px;color:#a8b0d3;">
                  <span title="Sold">🛒 ${totalSales}</span>
                  <span title="Rating">⭐ ${rating}</span>
                  <span>${statusHtml}</span>
                </div>
            </div>`;
          card.addEventListener('click', () => { const id=p.productId; loadProduct(id).then(()=> openModal(productModal)); });
          productsGrid.appendChild(card);
        });
        if (productsPager) {
          productsPager.innerHTML=''; const total = productsPageState.totalPages;
          if (total>1) {
            const mk=(label,page,disabled,current)=>{ const b=document.createElement('button'); b.type='button'; b.className='btn'; b.textContent=label; b.disabled=disabled; if(current) b.setAttribute('aria-current','page'); b.addEventListener('click',()=>{ productsPageState.page=page; loadProductsPanel(false); }); return b; };
            productsPager.appendChild(mk('«', Math.max(0, productsPageState.page-1), productsPageState.page===0,false));
            {
              const wSize = 10; let startIdx = 0; if (total <= wSize) startIdx = 0; else { if (productsPageState.page <= wSize - 1) startIdx = 0; else startIdx = productsPageState.page - (wSize - 1); if (startIdx > total - wSize) startIdx = total - wSize; }
              const endIdx = Math.min(total - 1, startIdx + wSize - 1);
              for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i+1), i, false, i===productsPageState.page); btn.classList.add('page-btn'); if (i===productsPageState.page) btn.classList.add('active'); productsPager.appendChild(btn); }
            }
            productsPager.appendChild(mk('»', Math.min(total-1, productsPageState.page+1), productsPageState.page===total-1,false));
          }
        }
        try { showToast(`Loaded ${data.content ? data.content.length : 0} products`, 'info', { duration: 1200 }); } catch(_){ }
      };
      if (typeof withPanelLoading === 'function' && panelEl) {
        withPanelLoading(panelEl, task, 'Failed to load products');
      } else {
        // Fallback: no overlay
        task();
      }
    }

    document.getElementById('prd_btnFilter')?.addEventListener('click', () => loadProductsPanel(true));
    document.getElementById('prd_btnReset')?.addEventListener('click', () => {
      const s=document.getElementById('prd_search'); if (s) s.value='';
      if (prdCategorySel) prdCategorySel.value='';
      const r=document.getElementById('prd_rating'); if (r) r.value='';
      const d=document.getElementById('prd_downloads'); if (d) d.value='';
      const st=document.getElementById('prd_status'); if (st) st.value='';
      loadProductsPanel(true);
    });
    document.getElementById('prd_search')?.addEventListener('keydown', e => { if (e.key==='Enter') { e.preventDefault(); loadProductsPanel(true);} });
    // Auto apply when changing status
    document.getElementById('prd_status')?.addEventListener('change', () => loadProductsPanel(true));
    if (window.location.hash === '#products') setTimeout(() => loadProductsPanel(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#products') loadProductsPanel(false); });

    // ================= Generate Keys Panel =================
    function onlyPublicProducts(list) {
      return Array.isArray(list) ? list.filter(p => (p.status||'').toLowerCase()==='public') : [];
    }
    async function populatePublicProductsForGen() {
      const sel = document.getElementById('gk_product'); if (!sel) return;
      if (sel.getAttribute('data-loaded')==='1') return;
      try {
        const sellerIdEl = document.getElementById('sellerId');
        const userIdEl = document.getElementById('userId');
        const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
        if (!sellerId) return;
        const res = await fetch(`/api/products?sellerId=${sellerId}`);
        if (!res.ok) return;
        const list = await res.json();
        onlyPublicProducts(list).forEach(p => { const o=document.createElement('option'); o.value=p.productId; o.textContent=`#${p.productId} • ${p.name}`; o.dataset.qty = p.quantity ?? 0; sel.appendChild(o); });
        sel.setAttribute('data-loaded','1');
      } catch(_){}
    }

    // ==== Generate Keys Panel: Custom Product Table ====
    async function renderProductTableForGenKey() {
      const table = document.getElementById('gk_product_table');
      const tbody = table?.querySelector('tbody');
      const selectedDiv = document.getElementById('gk_product_selected');
      const input = document.getElementById('gk_product');
      if (!tbody || !input) return;
      tbody.innerHTML = '<tr><td colspan="4">Đang tải sản phẩm...</td></tr>';
      selectedDiv.textContent = '';
      input.value = '';
      try {
        const sellerIdEl = document.getElementById('sellerId');
        const userIdEl = document.getElementById('userId');
        const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
        if (!sellerId) return;
        const res = await fetch(`/api/products?sellerId=${sellerId}`);
        if (!res.ok) { tbody.innerHTML = '<tr><td colspan="4">Không thể tải sản phẩm</td></tr>'; return; }
        const list = await res.json();
        const products = Array.isArray(list) ? list.filter(p => (p.status||'').toLowerCase()==='public') : [];
        if (!products.length) { tbody.innerHTML = '<tr><td colspan="4">Không có sản phẩm PUBLIC</td></tr>'; return; }
        tbody.innerHTML = '';
        products.forEach(p => {
          const tr = document.createElement('tr');
          tr.className = 'clickable';
          tr.innerHTML = `<td>${p.productId}</td><td>${p.name}</td><td>${p.categoryName||''}</td><td>${p.status}</td>`;
          tr.addEventListener('click', () => {
            input.value = p.productId;
            selectedDiv.textContent = `Đã chọn: #${p.productId} • ${p.name}`;
            // Highlight row
            tbody.querySelectorAll('tr').forEach(row => row.classList.remove('selected'));
            tr.classList.add('selected');
          });
          tbody.appendChild(tr);
        });
      } catch (e) {
        tbody.innerHTML = '<tr><td colspan="4">Lỗi tải sản phẩm</td></tr>';
      }
    }

    // ==== Generate Keys Panel: User selection with search + pagination ====
    const gkUsersState = { page: 0, size: 8, totalPages: 0, q: '', type: '' };

    async function loadGenUsers(resetPage=false) {
      const tbody = document.querySelector('#gk_user_table tbody');
      const pager = document.getElementById('pgGenUsers');
      const selDiv = document.getElementById('gk_user_selected');
      const hidden = document.getElementById('gk_user');
      if (!tbody || !pager) return;
      if (resetPage) gkUsersState.page = 0;
      const params = new URLSearchParams();
      params.set('page', gkUsersState.page);
      params.set('size', gkUsersState.size);
      if (gkUsersState.q) params.set('q', gkUsersState.q);
      if (gkUsersState.type) params.set('type', gkUsersState.type);
      tbody.innerHTML = '<tr><td colspan="4">Loading users...</td></tr>';
      try {
        const res = await fetch('/api/users/search?' + params.toString());
        if (!res.ok) { tbody.innerHTML = '<tr><td colspan="4">Failed to load users</td></tr>'; return; }
        const data = await res.json();
        gkUsersState.totalPages = data.totalPages || 1;
        const list = Array.isArray(data.content) ? data.content : [];
        tbody.innerHTML = '';
        if (!list.length) tbody.innerHTML = '<tr><td colspan="4">No users</td></tr>';
        list.forEach(u => {
          const tr = document.createElement('tr');
          tr.className = 'clickable';
          const uname = u.username ?? '';
          const email = u.email ?? '';
          const type = u.userType ?? '';
          tr.innerHTML = `<td>${u.userId}</td><td>${uname}</td><td>${email}</td><td>${type}</td>`;
          tr.addEventListener('click', () => {
            if (hidden) hidden.value = u.userId;
            if (selDiv) selDiv.textContent = `Assign to: #${u.userId} • ${uname} • ${email}`;
            tbody.querySelectorAll('tr').forEach(r => r.classList.remove('selected'));
            tr.classList.add('selected');
          });
          tbody.appendChild(tr);
        });
        // Build server pager
        pager.innerHTML = '';
        if (gkUsersState.totalPages > 1) {
          const mk=(label,page,disabled,current)=>{ const b=document.createElement('button'); b.type='button'; b.className='btn'; b.textContent=label; b.disabled=disabled; if(current) b.setAttribute('aria-current','page'); b.addEventListener('click',()=>{ gkUsersState.page=page; loadGenUsers(false); }); return b; };
          pager.appendChild(mk('«', Math.max(0, gkUsersState.page-1), gkUsersState.page===0,false));
          // Server pager for generate-keys users: sliding window (7 pages only)
          const totalG = gkUsersState.totalPages;
          const wSizeG = 7;
          let startG = 0;
          if (totalG <= wSizeG) startG = 0; else { if (gkUsersState.page <= wSizeG - 1) startG = 0; else startG = gkUsersState.page - (wSizeG - 1); if (startG > totalG - wSizeG) startG = totalG - wSizeG; }
          const endG = Math.min(totalG - 1, startG + wSizeG - 1);
          for (let i = startG; i <= endG; i++) { const b = mk(String(i+1), i, false, i===gkUsersState.page); b.classList.add('page-btn'); if (i===gkUsersState.page) b.classList.add('active'); pager.appendChild(b); }
          pager.appendChild(mk('»', Math.min(gkUsersState.totalPages-1, gkUsersState.page+1), gkUsersState.page===gkUsersState.totalPages-1,false));
        }
      } catch (_) {
        tbody.innerHTML = '<tr><td colspan="4">Error loading users</td></tr>';
      }
    }

    function initGenUserSearchHandlers() {
      const q = document.getElementById('gk_user_q');
      const t = document.getElementById('gk_user_type');
      const btn = document.getElementById('gk_user_search');
      if (q) q.addEventListener('keydown', e => { if (e.key==='Enter') { e.preventDefault(); gkUsersState.q = (q.value||'').trim(); loadGenUsers(true); } });
      if (t) t.addEventListener('change', () => { gkUsersState.type = (t.value||'').trim(); loadGenUsers(true); });
      if (btn) btn.addEventListener('click', () => { gkUsersState.q = (q?.value||'').trim(); gkUsersState.type = (t?.value||'').trim(); loadGenUsers(true); });
    }

    async function initGenerateKeys() {
      await renderProductTableForGenKey();
      initGenUserSearchHandlers();
      await loadGenUsers(true);
    }

    document.getElementById('genKeyForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const prodInput = document.getElementById('gk_product');
      const pid = prodInput?.value ? Number(prodInput.value) : null;
      const exp = document.getElementById('gk_expire')?.value || '';
      let qty = document.getElementById('gk_qty')?.value ? parseInt(document.getElementById('gk_qty').value,10) : 0;
      if (!pid) { showToast('Vui lòng chọn sản phẩm PUBLIC', 'error'); return; }
      if (!qty || qty <= 0) { showToast('Số lượng phải > 0', 'error'); return; }
      try {
        const res = await fetch('/api/seller/' + (document.getElementById('userId')?.textContent?.trim()||document.getElementById('sellerId')?.textContent?.trim()) + '/licenses/generate', {
          method: 'POST', headers: {'Content-Type':'application/json'},
          body: JSON.stringify({
            productId: pid,
            expireDate: exp,
            quantity: qty,
            userId: (document.getElementById('gk_user')?.value ? Number(document.getElementById('gk_user').value) : undefined)
          })
        });
        if (!res.ok) { const t = await res.text(); showToast(t || 'Failed to generate keys', 'error'); return; }
        const data = await res.json();
        showToast(`Đã tạo ${data.generated} key (còn lại ${data.remaining})`, 'success');
      } catch (e) { showToast('Lỗi tạo key', 'error'); }
    });

    if (window.location.hash === '#gen-keys') setTimeout(() => initGenerateKeys(), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#gen-keys') initGenerateKeys(); });
  });
})();
