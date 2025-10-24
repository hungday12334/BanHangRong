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
  const MIN_LOAD = 800;

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

    // Keep instance for dynamic updates
    if (window.revenueChartInstance && window.revenueChartInstance.destroy) {
      window.revenueChartInstance.destroy();
      window.revenueChartInstance = null;
    }
    window.revenueChartInstance = new Chart(ctx, {
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

  // Fetch revenue data for seller and update chart
  async function loadAndUpdateRevenue(days) {
    const sellerEl = document.getElementById('sellerId');
    const sellerId = sellerEl ? sellerEl.textContent.trim() : null;
    if (!sellerId) return;
    try {
      const resp = await fetch(`/api/seller/${encodeURIComponent(sellerId)}/analytics/revenue?days=${encodeURIComponent(days)}`);
      if (!resp.ok) throw new Error('Failed to load revenue');
      const json = await resp.json();
      const labels = Array.from(json.labels || []);
      const data = Array.from(json.data || []).map(Number);
      const canvas = document.getElementById('revenueChart');
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (window.revenueChartInstance) {
        window.revenueChartInstance.data.labels = labels;
        if (window.revenueChartInstance.data.datasets && window.revenueChartInstance.data.datasets[0]) {
          window.revenueChartInstance.data.datasets[0].data = data;
        }
        window.revenueChartInstance.update();
      } else {
        // set data attributes then init
        canvas.setAttribute('data-labels', labels.join(','));
        canvas.setAttribute('data-data', data.join(','));
        initChart();
      }
    } catch (e) {
      console.error('loadAndUpdateRevenue error', e);
      showToast('Could not load revenue data', 'error');
    }
  }

  // Wire up range selector for revenue chart
  function initRevenueRangeControls() {
    const sel = document.getElementById('revenueRange');
    const custom = document.getElementById('revenueRangeCustom');
    if (!sel) return;
    sel.addEventListener('change', () => {
      if (sel.value === 'custom') {
        custom.style.display = '';
        custom.focus();
      } else {
        custom.style.display = 'none';
        loadAndUpdateRevenue(Number(sel.value));
      }
    });
    custom.addEventListener('change', () => {
      const v = Number(custom.value) || 15;
      loadAndUpdateRevenue(v);
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
        const data = ctx.getImageData(0, 0, w, h).data;
        let sum = 0, opaque = 0;
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i], g = data[i + 1], b = data[i + 2], a = data[i + 3];
          if (a > 12) opaque++;
          sum += r + g + b;
        }
        const avg = sum / ((data.length / 4) * 3);
        if (opaque < (data.length / 4) * 0.05 || avg > 250) {
          toFallback('blank/transparent');
        }
      } catch (e) { console.debug('Logo analysis skipped', e); }
    }, { once: true });
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
      setTimeout(() => { layer.remove(); document.body.classList.remove('theme-switching'); }, 620);
    } catch (_) { }
    // Remove transition class after a short delay
    setTimeout(() => root.classList.remove('theme-transition'), 600);
  }


  // Initialize charts and revenue controls after DOM ready
  onReady(function () {
    initLogoFallback();
    const loadStarted = performance.now();
    // Ensure appLoader element is available in this scope for loader progress handling
    const appLoader = document.getElementById('appLoader');
    initLogoFallback();
    initChart();
    initRevenueRangeControls();
    // load default range from selector (if present) to refresh chart data
    const sel = document.getElementById('revenueRange');
    if (sel) {
      const initial = sel.value === 'custom' ? (document.getElementById('revenueRangeCustom')?.value || '15') : sel.value;
      loadAndUpdateRevenue(Number(initial));
    }
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
      const inc = simulated < 70 ? (4 + Math.random() * 6) : (1 + Math.random() * 3);
      simulated = Math.min(simulated + inc, 94); // stop at 94% until finish
      if (progressBar) progressBar.style.width = simulated + '%';
      if (loadTextEl) {
        if (simulated < 30) loadTextEl.textContent = 'Initializing...';
        else if (simulated < 55) loadTextEl.textContent = 'Loading data...';
        else if (simulated < 80) loadTextEl.textContent = 'Processing metrics...';
        else loadTextEl.textContent = 'Preparing view...';
      }
      setTimeout(tickProgress, 260 + Math.random() * 240);
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
          setTimeout(() => { clearInterval(tipTimer); appLoader.remove(); }, 600);
        }
        // Start animations AFTER loader removed
        document.querySelectorAll('[data-count]').forEach(animateCount);
        initChart();
        document.querySelectorAll('.progress span').forEach(span => {
          const w = span.getAttribute('data-target-width') || span.style.width || '0%';
          span.style.width = '0%'; requestAnimationFrame(() => span.style.width = w);
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
        } catch (_) { }
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
          const onFinish = () => { try { cleanupOverlay(); } catch (_) { } };
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

      // Upload image -> autofill URL
      (function initImageGenerate() {
        const btn = document.getElementById('pm_genImage');
        const fileInput = document.getElementById('pm_imageFile');
        const urlInput = document.getElementById('pm_imageUrl');
        const previewWrap = document.getElementById('pm_imagePreview');
        const previewImg = previewWrap ? previewWrap.querySelector('img') : null;
        if (!btn || !fileInput || !urlInput) return;
        btn.addEventListener('click', () => { fileInput.click(); });
        fileInput.addEventListener('change', async () => {
          const f = fileInput.files && fileInput.files[0];
          if (!f) return;
          try {
            const fd = new FormData(); fd.append('file', f);
            // Optional: allow passing expiration via data-expiration on Generate button
            const exp = btn && btn.dataset ? (btn.dataset.expiration || '') : '';
            if (exp && /^\d+$/.test(exp)) { fd.append('expiration', exp); }
            const res = await fetch('/api/uploads/image', { method: 'POST', body: fd });
            if (!res.ok) {
              let msg = 'Upload failed';
              try { msg = (await res.text()) || msg; } catch (_) { }
              showToast(msg, 'error');
              return;
            }
            const data = await res.json();
            const url = data && data.url ? data.url : null;
            if (!url) { showToast('Invalid upload response', 'error'); return; }
            urlInput.value = url;
            if (previewWrap && previewImg) {
              previewImg.src = url; previewWrap.style.display = '';
            }
            // Mark as changed so Save will persist
            __originalProduct = null;
            showToast('Image uploaded', 'success', { duration: 1500 });
          } catch (e) {
            showToast('Upload error', 'error');
          } finally {
            fileInput.value = '';
          }
        });
      })();

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
        if (res.ok) {
          // Try to set primary image if URL present
          try {
            const saved = await res.json().catch(() => null);
            const pid = id || (saved && (saved.productId || saved.id));
            const url = (document.getElementById('pm_imageUrl').value || '').trim();
            if (pid && url) {
              await fetch(`/api/products/${pid}/primary-image`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url })
              });
            }
          } catch (_) { /* non-blocking */ }
          closeModal(productModal);
          showToast(id ? 'Product saved' : 'Product created', 'success');
          setTimeout(() => refreshMyProducts(), 350);
        } else {
          showToast('Failed to save product', 'error');
        }
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

    // Sort state for My Products table
    let myProductsSort = { key: 'productId', dir: 'asc' };

    // Attach click handlers to sortable headers in My Products card
    function initMyProductsSorting() {
      const panel = document.getElementById('sectionMyProducts');
      if (!panel) return;
      panel.querySelectorAll('th.sortable').forEach(th => {
        th.style.cursor = 'pointer';
        th.addEventListener('click', () => {
          const key = th.getAttribute('data-sort');
          if (!key) return;
          if (myProductsSort.key === key) {
            myProductsSort.dir = (myProductsSort.dir === 'asc') ? 'desc' : 'asc';
          } else {
            myProductsSort.key = key;
            myProductsSort.dir = 'asc';
          }
          // update UI indicators
          panel.querySelectorAll('th.sortable .sort-indicator').forEach(si => si.textContent = '');
          const ind = th.querySelector('.sort-indicator');
          if (ind) ind.textContent = myProductsSort.dir === 'asc' ? ' ▲' : ' ▼';
          // re-fetch/render with new sort
          withPanelLoading(panel.querySelector('.card') || panel, () => refreshMyProducts(false), 'Failed to sort products');
        });
      });
    }

    // Load "My Products" list (reusable for refresh after CRUD)
    async function refreshMyProducts(showToastMsg = true) {
      const sellerIdEl = document.getElementById('sellerId');
      const userIdEl = document.getElementById('userId');
      const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
      if (!sellerId) return; // require seller
      const res = await fetch(`/api/products?sellerId=${sellerId}`);
      if (!res.ok) { showToast('Failed to load your product list', 'error'); return; }
      let list = await res.json();
      // Apply status filter from UI if present
      try {
        const statusSel = document.getElementById('myProductsStatusFilter');
        const statusFilter = statusSel ? (statusSel.value || 'all') : 'all';
        if (statusFilter && statusFilter !== 'all') {
          list = list.filter(p => ((p.status || '').toString().toLowerCase() === statusFilter));
        }
      } catch (e) { /* non-blocking */ }

      // Apply sorting (client-side)
      try {
        const key = myProductsSort.key;
        const dir = myProductsSort.dir === 'asc' ? 1 : -1;
        if (key) {
          list.sort((a, b) => {
            const va = (a[key] == null) ? '' : a[key];
            const vb = (b[key] == null) ? '' : b[key];
            // numeric compare for known numeric fields
            if (key === 'productId' || key === 'quantity' || key === 'price') {
              const na = Number(va) || 0;
              const nb = Number(vb) || 0;
              return (na - nb) * dir;
            }
            // fallback string compare
            return String(va).toLowerCase().localeCompare(String(vb).toLowerCase()) * dir;
          });
        }
      } catch (e) { /* non-blocking */ }
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

    // initialize sorting after DOM is ready for the panel
    initMyProductsSorting();

      // Wire status filter change to refresh list
      const myProductsFilter = document.getElementById('myProductsStatusFilter');
      if (myProductsFilter) {
        myProductsFilter.addEventListener('change', () => {
          const panel = document.getElementById('sectionMyProducts');
          // prefer to attach overlay to the inner .card so it only covers the card area
          const card = panel ? panel.querySelector('.card') : null;
          const target = card || panel;
          if (target && typeof withPanelLoading === 'function') {
            withPanelLoading(target, () => refreshMyProducts(false), 'Failed to load products');
          } else {
            refreshMyProducts(false);
          }
        });
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
        try { history.replaceState({}, '', '#profile'); } catch (e) { }
      }
      // optional: scroll to top of profile panel
      profilePanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function hideProfile() {
      if (!profilePanel || !dashboardContent) return;
      profilePanel.hidden = true;
      profilePanel.style.display = 'none';
      dashboardContent.style.display = '';
      // ALSO hide other sidebar panels (orders, keys, products, vouchers) to prevent residual content
      ['ordersPanel', 'keysPanel', 'profileSettingsPanel', 'productsPanel', 'vouchersPanel'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.hidden = true; el.style.display = 'none'; }
      });
      // restore active class to dashboard link
      document.querySelectorAll('.menu a').forEach(a => a.classList.remove('active'));
      const dash = Array.from(document.querySelectorAll('.menu a')).find(a => a.getAttribute('href') === '/seller/dashboard');
      if (dash) dash.classList.add('active');
      try { history.replaceState({}, '', (location.pathname || '/seller/dashboard') + (location.search || '')); } catch (e) { }
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
      '#vouchers': 'vouchersPanel',
      '#withdraw': 'withdrawPanel',
      '#profile-settings': 'profileSettingsPanel'
    };
    function showPanelByHash(hash) {
      // default: show dashboard
      if (!hash || !panelMap[hash]) {
        hideProfile();
        // also hide any other panels
        Object.values(panelMap).forEach(id => { const el = document.getElementById(id); if (el) { el.hidden = true; el.style.display = 'none'; } });
        // ensure vouchers panel remains hidden and flagged aria-hidden for a11y
        const vp = document.getElementById('vouchersPanel');
        if (vp) { vp.hidden = true; vp.style.display = 'none'; vp.setAttribute('aria-hidden', 'true'); }
        // close product modal if somehow left open and prevent its inline form from being visible
        const pm = document.getElementById('productModal');
        if (pm && pm.hasAttribute('open')) { try { pm.close(); } catch (_) { pm.removeAttribute('open'); pm.style.display = 'none'; } }
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
      try { history.replaceState({}, '', hash); } catch (e) { }
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
        } else if (hash === '#vouchers' && typeof loadVouchersPanel === 'function') {
          loadVouchersPanel(true);
        } else if (hash === '#withdraw') {
          if (typeof loadWithdrawPanel === 'function') loadWithdrawPanel();
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
    // ===== Vouchers panel logic =====
    // ===== Withdraw panel logic =====
    // Common Vietnam bank list (code + display name)
    const BANKS = [
      { code: 'VCB', name: 'Vietcombank' },
      { code: 'CTG', name: 'VietinBank' },
      { code: 'BIDV', name: 'BIDV' },
      { code: 'TCB', name: 'Techcombank' },
      { code: 'MB', name: 'MB Bank' },
      { code: 'AGRIBANK', name: 'Agribank' },
      { code: 'ACB', name: 'ACB' },
      { code: 'VPB', name: 'VPBank' },
      { code: 'STB', name: 'Sacombank' },
      { code: 'HDB', name: 'HDBank' },
      { code: 'SCB', name: 'SCB' },
      { code: 'OCB', name: 'OCB' },
      { code: 'VIB', name: 'VIB' },
      { code: 'SHB', name: 'SHB' },
      { code: 'MSB', name: 'MSB' },
      { code: 'EIB', name: 'Eximbank' },
      { code: 'TPB', name: 'TPBank' },
      { code: 'HSBC', name: 'HSBC Vietnam' }
    ];
    async function loadWithdrawSummary() {
      const res = await fetch('/seller/withdraw/summary');
      if (!res.ok) throw new Error('Cannot load withdraw summary');
      return await res.json();
    }
    function buildWithdrawSearchURL(params) {
      const url = new URL('/seller/withdraw/search', window.location.origin);
      Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && String(v).trim() !== '') url.searchParams.set(k, v); });
      return url.toString();
    }
    async function searchWithdrawals({ status, fromDate, toDate, minAmount, maxAmount, page = 0, size = 10 }) {
      const url = buildWithdrawSearchURL({ status, fromDate, toDate, minAmount, maxAmount, page, size });
      const res = await fetch(url);
      if (!res.ok) throw new Error('Cannot load withdraw list');
      return await res.json();
    }
    async function createWithdrawal(payload) {
      const res = await fetch('/seller/withdraw', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (!res.ok) throw new Error(await res.text());
      return await res.json();
    }
    async function addBankAccount(payload) {
      const res = await fetch('/seller/withdraw/bank-account', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (!res.ok) throw new Error(await res.text());
      return await res.json();
    }
    function renderWithdrawUI(data) {
      const bankSel = document.getElementById('wd_bank');
      const histBody = document.getElementById('wd_history');
      const pager = document.getElementById('wd_pager');
      const fromEl = document.getElementById('wd_from');
      const toEl = document.getElementById('wd_to');
      const minEl = document.getElementById('wd_min');
      const maxEl = document.getElementById('wd_max');
      const statusEl = document.getElementById('wd_status');
      const applyBtn = document.getElementById('wd_apply_filters');
      const amountWrap = document.getElementById('wd_amount_wrap');
      const allChk = document.getElementById('wd_all');
      const summary = document.getElementById('wd_summary');
      const netPreview = document.getElementById('wd_net_preview');
      const addBankBtn = document.getElementById('wd_add_bank');
      if (!bankSel || !histBody) return;
      summary.textContent = `Balance: ${Number(data.balance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2 })} • Fee: ${data.feePercent}%`;
      bankSel.innerHTML = '';
      (data.accounts || []).forEach(b => {
        const opt = document.createElement('option');
        opt.value = b.bankAccountId; opt.textContent = `${b.bankName} - ${b.accountNumber}`; bankSel.appendChild(opt);
      });
      // wire add bank button (bind once)
      if (addBankBtn && !addBankBtn.dataset.bound) {
        addBankBtn.addEventListener('click', () => openBankAccountModal());
        addBankBtn.dataset.bound = '1';
      }
      async function applyFilters(page = 0) {
        const status = statusEl?.value || '';
        const fromDate = fromEl?.value || '';
        const toDate = toEl?.value || '';
        const minAmount = minEl?.value || '';
        const maxAmount = maxEl?.value || '';
        const resp = await searchWithdrawals({ status, fromDate, toDate, minAmount, maxAmount, page, size: 10 });
        const content = Array.isArray(resp.content) ? resp.content : (Array.isArray(resp) ? resp : []);
        histBody.innerHTML = '';
        content.forEach(w => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${w.withdrawalId}</td><td>${w.createdAt ? new Date(w.createdAt).toLocaleString() : ''}</td><td>${w.amount}</td><td>${w.feeAmount}</td><td>${w.netAmount}</td><td><span class="chip">${w.status}</span></td>`;
          histBody.appendChild(tr);
        });
        // render pager
        if (pager) {
          pager.innerHTML = '';
          const totalPages = typeof resp.totalPages === 'number' ? resp.totalPages : 1;
          const number = typeof resp.number === 'number' ? resp.number : 0;
          if (totalPages > 1) {
            const mk = (label, p, disabled, active) => {
              const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = !!disabled; if (active) b.classList.add('active');
              b.addEventListener('click', () => applyFilters(p)); return b;
            };
            pager.appendChild(mk('Prev', Math.max(0, number - 1), number <= 0));
            for (let i = 0; i < totalPages; i++) { pager.appendChild(mk(String(i + 1), i, false, i === number)); }
            pager.appendChild(mk('Next', Math.min(totalPages - 1, number + 1), number >= totalPages - 1));
          }
        }
      }
      applyBtn?.addEventListener('click', () => applyFilters(0));
      // initial load with no filters shows recent page 0
      applyFilters(0);
      if (!data.accounts || data.accounts.length === 0) {
        // inline hint when no accounts
        const wrap = document.createElement('div');
        wrap.className = 'footer-note';
        wrap.style.marginTop = '8px';
        wrap.textContent = 'No bank accounts yet. Click “+” to add one.';
        bankSel.parentElement.appendChild(wrap);
      }
      function toggleAmount() { amountWrap.style.display = allChk.checked ? 'none' : 'block'; updateNetPreview(); }
      allChk?.addEventListener('change', toggleAmount); toggleAmount();
      function updateNetPreview() {
        if (!netPreview) return;
        const feePct = Number(data.feePercent || 0);
        const withdrawAll = allChk?.checked;
        let amountVal = 0;
        if (withdrawAll) amountVal = Number(data.balance || 0);
        else {
          const v = Number(document.getElementById('wd_amount')?.value || 0);
          amountVal = isNaN(v) ? 0 : v;
        }
        const fee = amountVal * (feePct / 100);
        const net = Math.max(0, amountVal - fee);
        netPreview.textContent = `Net after ${feePct}% fee: ${net.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
      }
      document.getElementById('wd_amount')?.addEventListener('input', updateNetPreview);
      updateNetPreview();
    }
    async function loadWithdrawPanel() {
      const panel = document.getElementById('withdrawPanel');
      await withPanelLoading(panel, async () => {
        const data = await loadWithdrawSummary();
        renderWithdrawUI(data);
      }, 'Cannot load withdraw data');
    }
    document.getElementById('wd_form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const bankAccountId = Number(document.getElementById('wd_bank').value);
        const withdrawAll = document.getElementById('wd_all').checked;
        const amountEl = document.getElementById('wd_amount');
        const amount = withdrawAll ? null : Number(amountEl.value);
        const res = await createWithdrawal({ bankAccountId, amount, withdrawAll });
        showToast(`Created. Fee: ${res.fee}, Net: ${res.net}`, 'success');
        const data = await loadWithdrawSummary();
        renderWithdrawUI(data);
      } catch (err) { showToast(String(err), 'error'); }
    });

    // ===== Bank account modal =====
    function openBankAccountModal() {
      const dlg = document.getElementById('bankAccountModal');
      if (!dlg) return;
      const form = dlg.querySelector('#bankAccountForm');
      // reset fields
      form.reset?.();
      // populate bank select once
      const sel = dlg.querySelector('#ba_bankSelect');
      const codeInput = dlg.querySelector('#ba_bankCode');
      const logoWrap = dlg.querySelector('#ba_bankLogo');
      const logoImg = logoWrap ? logoWrap.querySelector('img') : null;
      const logoFallback = logoWrap ? logoWrap.querySelector('.fallback') : null;
      const browseBtn = dlg.querySelector('#ba_browseBanks');
      const listBox = dlg.querySelector('#ba_bankList');
      function updateBankPreview(code) {
        if (!logoWrap || !logoImg || !logoFallback) return;
        if (!code) {
          logoImg.style.display = 'none';
          logoFallback.style.display = '';
          logoFallback.textContent = '';
          return;
        }
        const urlPng = `/img/banks/${code}.png`;
        const urlSvg = `/img/banks/${code}.svg`;
        let triedSvg = false;
        function toFallback() {
          logoImg.style.display = 'none';
          logoFallback.style.display = '';
          logoFallback.textContent = code.slice(0, 3).toUpperCase();
        }
        logoImg.onerror = () => {
          if (!triedSvg) { triedSvg = true; logoImg.src = urlSvg; }
          else { toFallback(); }
        };
        logoImg.onload = () => { logoImg.style.display = ''; logoFallback.style.display = 'none'; };
        logoImg.src = urlPng;
      }
      if (sel && !sel.dataset.bound) {
        // clear current options except first placeholder
        while (sel.options.length > 1) sel.remove(1);
        BANKS.forEach(b => {
          const opt = document.createElement('option');
          opt.value = b.code; opt.textContent = `${b.name} (${b.code})`;
          sel.appendChild(opt);
        });
        sel.addEventListener('change', () => {
          codeInput.value = sel.value || '';
          updateBankPreview(sel.value || '');
        });
        sel.dataset.bound = '1';
      } else if (sel) {
        sel.value = '';
      }
      if (codeInput) codeInput.value = '';
      updateBankPreview('');
      // Image-rich browse dropdown
      function closeBankList() { if (listBox) { listBox.hidden = true; dlg.__bankListOpen = false; browseBtn?.setAttribute('aria-expanded', 'false'); } }
      function openBankList() {
        if (!listBox) return;
        listBox.innerHTML = '';
        BANKS.forEach(b => {
          const row = document.createElement('div');
          row.className = 'bank-option';
          row.setAttribute('role', 'option');
          row.innerHTML = `<span class="logo"><img alt="${b.name}" onerror="this.style.display='none';this.nextElementSibling.style.display='';" /><span class="fallback" style="display:none">${b.code}</span></span><span class="name">${b.name}</span><span class="code">${b.code}</span>`;
          const img = row.querySelector('img');
          if (img) img.src = `/img/banks/${b.code}.png`;
          row.addEventListener('click', () => {
            if (sel) sel.value = b.code;
            if (codeInput) codeInput.value = b.code;
            updateBankPreview(b.code);
            closeBankList();
          });
          listBox.appendChild(row);
        });
        listBox.hidden = false;
        dlg.__bankListOpen = true;
        browseBtn?.setAttribute('aria-expanded', 'true');
      }
      if (browseBtn && !browseBtn.dataset.bound) {
        browseBtn.addEventListener('click', () => {
          if (dlg.__bankListOpen) closeBankList(); else openBankList();
        });
        browseBtn.dataset.bound = '1';
      }
      // close when clicking outside list
      const onDocClick = (ev) => {
        if (!dlg.__bankListOpen) return;
        if (!listBox.contains(ev.target) && !browseBtn.contains(ev.target)) closeBankList();
      };
      document.addEventListener('click', onDocClick, { once: true });
      dlg.querySelector('#ba_accountNumber').value = '';
      dlg.querySelector('#ba_holderName').value = '';
      dlg.querySelector('#ba_branch').value = '';
      const def = dlg.querySelector('#ba_default');
      if (def) def.checked = true;

      dlg.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(dlg); }, { once: true });
      dlg.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(dlg), { once: true }));
      form.onsubmit = async (e) => {
        e.preventDefault();
        const bankCode = (dlg.querySelector('#ba_bankSelect').value || '').trim();
        if (!bankCode) { showToast('Please select a bank', 'error'); return; }
        const bank = BANKS.find(b => b.code === bankCode);
        const bankName = bank ? bank.name : bankCode;
        const accountNumber = dlg.querySelector('#ba_accountNumber').value.trim();
        const accountHolderName = dlg.querySelector('#ba_holderName').value.trim();
        const branch = dlg.querySelector('#ba_branch').value.trim();
        const makeDefault = !!dlg.querySelector('#ba_default').checked;
        if (!accountNumber || !accountHolderName) {
          showToast('Please fill required fields', 'error');
          return;
        }
        try {
          await addBankAccount({ bankName, bankCode, accountNumber, accountHolderName, branch, makeDefault });
          closeModal(dlg);
          showToast('Bank saved', 'success');
          const data2 = await loadWithdrawSummary();
          renderWithdrawUI(data2);
        } catch (e) {
          showToast(String(e), 'error');
        }
      };
      openModal(dlg);
    }
    async function fetchSellerProductsLite(sellerId) {
      const res = await fetch(`/api/products-lite?sellerId=${sellerId}`);
      if (!res.ok) return [];
      return await res.json();
    }

    async function fetchVouchers(sellerId, productId, q) {
      const url = new URL(`/api/seller/${sellerId}/products/${productId}/vouchers`, window.location.origin);
      if (q && q.trim()) url.searchParams.set('q', q.trim());
      const res = await fetch(url);
      if (!res.ok) return [];
      return await res.json();
    }

    async function saveVoucher(sellerId, productId, payload, voucherId) {
      const url = `/api/seller/${sellerId}/products/${productId}/vouchers` + (voucherId ? `/${voucherId}` : '');
      const method = voucherId ? 'PUT' : 'POST';
      const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      return res;
    }

    async function deleteVoucher(sellerId, productId, voucherId) {
      const url = `/api/seller/${sellerId}/products/${productId}/vouchers/${voucherId}`;
      return await fetch(url, { method: 'DELETE' });
    }

    async function loadVouchersPanel(forceReload) {
      const panel = document.getElementById('vouchersPanel');
      if (!panel) return;
      const sellerIdEl = document.getElementById('sellerId');
      const userIdEl = document.getElementById('userId');
      const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
      if (!sellerId) { showToast('Seller not identified', 'error'); return; }

      const selProduct = document.getElementById('vc_product');
      const selStatus = document.getElementById('vc_status');
      const inpSearch = document.getElementById('vc_search');
      const tb = document.getElementById('tbVouchers');
      const btnNew = document.getElementById('vc_btnNew');
      const btnRefresh = document.getElementById('vc_btnRefresh');

      // load product list once
      if (!selProduct.dataset.loaded || forceReload) {
        selProduct.innerHTML = '<option value="">Select product</option>';
        const products = await fetchSellerProductsLite(sellerId);
        for (const p of products) {
          const opt = document.createElement('option');
          opt.value = p.productId;
          opt.textContent = `#${p.productId} — ${p.name}`;
          selProduct.appendChild(opt);
        }
        selProduct.dataset.loaded = '1';
      }

      let lastReq = 0;
      async function render() {
        const pid = Number(selProduct.value || '0');
        tb.innerHTML = '';
        const reqId = ++lastReq;

        let list = [];
        if (!pid) {
          // No product selected: fetch vouchers for all products and merge
          const prods = Array.from(selProduct.options).filter(o => o.value).map(o => Number(o.value));
          const promises = prods.map(p => fetchVouchers(sellerId, p, inpSearch?.value || '').catch(() => []));
          const results = await Promise.all(promises);
          if (reqId !== lastReq) return; // ignore out-of-order
          // flatten and deduplicate by code (case-insensitive), keep first occurrence
          const combined = results.flat();
          const byCode = {};
          for (const v of combined) {
            const key = (v.code || '').toString().trim().toUpperCase();
            if (!byCode[key]) byCode[key] = v;
          }
          list = Object.values(byCode);
        } else {
          const reqIdLocal = reqId;
          list = await fetchVouchers(sellerId, pid, inpSearch?.value || '');
          if (reqIdLocal !== lastReq) return; // ignore out-of-order responses (typing fast)
        }

        const statusFilter = (selStatus.value || '').toLowerCase();
        const filtered = list.filter(v => !statusFilter || (v.status || '').toLowerCase() === statusFilter);
        if (!filtered.length) {
          tb.innerHTML = '<tr class="footer-note"><td colspan="7">No vouchers found.</td></tr>';
          return;
        }
        for (const v of filtered) {
          const tr = document.createElement('tr');
          const period = [v.startAt ? new Date(v.startAt).toLocaleString() : '-', v.endAt ? new Date(v.endAt).toLocaleString() : '-'].join(' → ');
          const uses = `${v.usedCount ?? 0}${v.maxUses ? ' / ' + v.maxUses : ''}`;
          const val = (v.discountType === 'AMOUNT' ? ('$' + Number(v.discountValue || 0).toLocaleString('en-US')) : (Number(v.discountValue || 0) + '%'));
          tr.innerHTML = `
            <td><b>${v.code}</b></td>
            <td>${v.discountType}</td>
            <td>${val}</td>
            <td class="hide-md">${period}</td>
            <td>${uses}</td>
            <td><span class="badge">${(v.status || '').toUpperCase()}</span></td>
            <td style="text-align:right;white-space:nowrap">
              <button class="btn" data-act="usage">Usage</button>
              <button class="btn" data-act="edit">Edit</button>
              <button class="btn danger" data-act="del">Delete</button>
            </td>`;
          // actions
          tr.querySelector('[data-act="edit"]').addEventListener('click', () => openVoucherModal(pid, v));
          tr.querySelector('[data-act="usage"]').addEventListener('click', () => openUsageModal(pid, v));
          tr.querySelector('[data-act="del"]').addEventListener('click', async () => {
            if (!confirm('Delete this voucher?')) return;
            const res = await deleteVoucher(sellerId, pid, v.voucherId);
            if (res.ok) { showToast('Voucher deleted', 'success'); render(); } else showToast('Delete failed', 'error');
          });
          tb.appendChild(tr);
        }
      }

      selProduct.addEventListener('change', render);
      selStatus.addEventListener('change', render);
      btnRefresh?.addEventListener('click', render);
      // live search with debounce 250ms, DB-backed
      let tmr;
      inpSearch?.addEventListener('input', () => { clearTimeout(tmr); tmr = setTimeout(render, 250); });

      function openVoucherModal(productId, voucher) {
        const dlg = document.getElementById('voucherModal');
        if (!dlg) return;
        dlg.querySelector('#vc_voucherId').value = voucher?.voucherId ?? '';
        dlg.querySelector('#vc_code').value = voucher?.code ?? '';
        dlg.querySelector('#vc_type').value = voucher?.discountType ?? 'PERCENT';
        dlg.querySelector('#vc_value').value = voucher?.discountValue ?? '';
        dlg.querySelector('#vc_min').value = voucher?.minOrder ?? '';
        dlg.querySelector('#vc_start').value = voucher?.startAt ? new Date(voucher.startAt).toISOString().slice(0, 16) : '';
        dlg.querySelector('#vc_end').value = voucher?.endAt ? new Date(voucher.endAt).toISOString().slice(0, 16) : '';
        dlg.querySelector('#vc_maxUses').value = voucher?.maxUses ?? '';
        dlg.querySelector('#vc_maxPerUser').value = voucher?.maxUsesPerUser ?? '';
        dlg.querySelector('#vc_status').value = voucher?.status ?? 'active';

        dlg.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(dlg); }, { once: true });
        dlg.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(dlg), { once: true }));
        dlg.querySelector('#voucherForm').onsubmit = async (e) => {
          e.preventDefault();
          const payload = {
            code: dlg.querySelector('#vc_code').value.trim(),
            discountType: dlg.querySelector('#vc_type').value,
            discountValue: Number(dlg.querySelector('#vc_value').value || '0'),
            minOrder: dlg.querySelector('#vc_min').value ? Number(dlg.querySelector('#vc_min').value) : null,
            startAt: dlg.querySelector('#vc_start').value ? new Date(dlg.querySelector('#vc_start').value).toISOString() : null,
            endAt: dlg.querySelector('#vc_end').value ? new Date(dlg.querySelector('#vc_end').value).toISOString() : null,
            maxUses: dlg.querySelector('#vc_maxUses').value ? Number(dlg.querySelector('#vc_maxUses').value) : null,
            maxUsesPerUser: dlg.querySelector('#vc_maxPerUser').value ? Number(dlg.querySelector('#vc_maxPerUser').value) : null,
            status: dlg.querySelector('#vc_status').value
          };
          const vid = dlg.querySelector('#vc_voucherId').value || null;
          const res = await saveVoucher(sellerId, productId, payload, vid);
          if (res.ok) { closeModal(dlg); showToast('Voucher saved', 'success'); render(); }
          else { const msg = await res.text(); showToast('Save failed: ' + msg, 'error'); }
        };
        openModal(dlg);
      }

      async function openUsageModal(productId, voucher) {
        const dlg = document.getElementById('voucherUsageModal');
        if (!dlg) return;
        const codeEl = dlg.querySelector('#vu_code');
        const usedEl = dlg.querySelector('#vu_used');
        const maxEl = dlg.querySelector('#vu_max');
        const leftEl = dlg.querySelector('#vu_left');
        const table = dlg.querySelector('#vu_table');
        codeEl.textContent = voucher.code;
        usedEl.textContent = voucher.usedCount ?? 0;
        maxEl.textContent = voucher.maxUses ?? '∞';
        leftEl.textContent = voucher.maxUses ? Math.max(0, voucher.maxUses - (voucher.usedCount ?? 0)) : '∞';
        table.innerHTML = '<tr class="footer-note"><td colspan="5">Loading...</td></tr>';
        const res = await fetch(`/api/seller/${sellerId}/products/${productId}/vouchers/${voucher.voucherId}/usage`);
        if (res.ok) {
          const list = await res.json();
          table.innerHTML = '';
          if (!list.length) table.innerHTML = '<tr class="footer-note"><td colspan="5">No usage yet.</td></tr>';
          list.forEach((r, idx) => {
            const trr = document.createElement('tr');
            trr.innerHTML = `<td>${idx + 1}</td><td>#${r.orderId || '-'}</td><td>#${r.userId || '-'}</td><td>$${Number(r.discountAmount || 0).toFixed(2)}</td><td>${r.createdAt ? new Date(r.createdAt).toLocaleString() : '-'}</td>`;
            table.appendChild(trr);
          });
        } else {
          table.innerHTML = '<tr class="footer-note"><td colspan="5">Failed to load usage.</td></tr>';
        }
        dlg.addEventListener('cancel', (e) => { e.preventDefault(); closeModal(dlg); }, { once: true });
        dlg.querySelectorAll('[data-close]').forEach(x => x.addEventListener('click', () => closeModal(dlg), { once: true }));
        openModal(dlg);
      }

      btnNew?.addEventListener('click', () => {
        const pid = Number(selProduct.value || '0');
        if (!pid) { showToast('Select a product first', 'error'); return; }
        openVoucherModal(pid, null);
      });

      // initial render if product already chosen
      render();
    }
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
          `<td>${o.createdAt ? o.createdAt.replace('T', ' ') : ''}</td>` +
          `<td>${o.buyerUsername ? o.buyerUsername : (o.buyerUserId ? ('User #' + o.buyerUserId) : '')}</td>` +
          `<td>${o.sellerItems ?? 0}</td>` +
          `<td>$${(o.sellerAmount ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>`;
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
            const amtVal = ord.sellerAmount; const amt = (amtVal == null) ? '' : Number(amtVal).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            document.getElementById('om_totalAmount').textContent = amt;
            document.getElementById('om_createdAt').textContent = ord.createdAt ?? '';
            const items = data.items || []; const tb = document.getElementById('om_items'); tb.innerHTML = '';
            for (const it of items) { const r = document.createElement('tr'); r.innerHTML = `<td>${it.productName || ('#' + it.productId)}</td><td>${it.quantity}</td><td>${it.priceAtTime}</td>`; tb.appendChild(r); }
            const overlay = document.getElementById('modalOverlay');
            if (overlay) { overlay.hidden = false; overlay.classList.add('visible'); }
            if (typeof orderModal.showModal === 'function') { try { orderModal.showModal(); } catch (_) { orderModal.setAttribute('open', ''); } }
            requestAnimationFrame(() => orderModal.classList.add('is-open'));
          })();
        });
        ordersTbody.appendChild(tr);
      });
      paginateTable(ordersTbody, ordersPager, ordersPageState.size); // reuse for pager skeleton
      // Override pager to hook page changes via API (not just client slicing)
      if (ordersPager) {
        ordersPager.innerHTML = '';
        const total = ordersPageState.totalPages;
        if (total > 1) {
          const mk = (label, page, disabled, current) => {
            const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = disabled; if (current) b.setAttribute('aria-current', 'page');
            b.addEventListener('click', () => { ordersPageState.page = page; loadSellerOrders(false); }); return b;
          };
          ordersPager.appendChild(mk('«', Math.max(0, ordersPageState.page - 1), ordersPageState.page === 0, false));
          // sliding window of up to 10 pages (1-based labels, page is 0-based)
          const wSize = 10;
          let startIdx = 0;
          if (total <= wSize) startIdx = 0; else {
            if (ordersPageState.page <= wSize - 1) startIdx = 0; else startIdx = ordersPageState.page - (wSize - 1);
            if (startIdx > total - wSize) startIdx = total - wSize;
          }
          const endIdx = Math.min(total - 1, startIdx + wSize - 1);
          for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i + 1), i, false, i === ordersPageState.page); btn.classList.add('page-btn'); if (i === ordersPageState.page) btn.classList.add('active'); ordersPager.appendChild(btn); }
          ordersPager.appendChild(mk('»', Math.min(total - 1, ordersPageState.page + 1), ordersPageState.page === total - 1, false));
        }
      }
    }

    document.getElementById('ord_btnFilter')?.addEventListener('click', () => loadSellerOrders(true));
    document.getElementById('ord_btnReset')?.addEventListener('click', () => {
      const f = document.getElementById('ord_from'); if (f) f.value = '';
      const t = document.getElementById('ord_to'); if (t) t.value = '';
      const s = document.getElementById('ord_search'); if (s) s.value = '';
      loadSellerOrders(true);
    });
    document.getElementById('ord_search')?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); loadSellerOrders(true); } });
    document.getElementById('ord_btnExport')?.addEventListener('click', () => {
      if (!ordersTbody) return; const rows = [['OrderId', 'CreatedAt', 'User', 'SellerItems', 'SellerAmount']];
      ordersTbody.querySelectorAll('tr').forEach(tr => { const cols = [...tr.children].map(td => td.textContent.replace(/\s+/g, ' ').trim()); if (cols.length >= 5) rows.push(cols.slice(0, 5)); });
      const csv = rows.map(r => r.map(c => '"' + c.replace(/"/g, '""') + '"').join(',')).join('\r\n');
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' }); const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'seller_orders.csv'; a.click(); setTimeout(() => URL.revokeObjectURL(a.href), 500);
    });

    // Auto load when panel hash activated
    if (window.location.hash === '#orders') setTimeout(() => loadSellerOrders(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#orders') loadSellerOrders(false); });

    // ================= License Keys Panel =================
    const keysTbody = document.getElementById('tbSellerKeys');
    const keysPager = document.getElementById('pgSellerKeys');
    let keysPageState = { page: 0, size: 10, totalPages: 0 };

    async function loadSellerKeys(resetPage = false) {
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
      keysTbody.innerHTML = '';
      data.content.forEach(l => {
        const tr = document.createElement('tr');
        const activeBadge = l.isActive
          ? '<button type="button" class="pill good" data-toggle-lic="' + l.licenseId + '" title="Click to disable">ON</button>'
          : '<button type="button" class="badge" data-toggle-lic="' + l.licenseId + '" title="Click to enable">OFF</button>';
        const actDate = l.activationDate ? l.activationDate.replace('T', ' ') : '';
        const deviceText = (l.deviceIdentifier && l.deviceIdentifier.trim().length)
          ? l.deviceIdentifier
          : 'unused';
        // Try to extract expire date from licenseKey format: PRD<productId>-yyyyMMdd-<random>
        let expireText = '';
        try {
          const parts = (l.licenseKey || '').split('-');
          if (parts.length >= 3) {
            const maybe = parts[1];
            if (/^\d{8}$/.test(maybe)) {
              // format yyyyMMdd -> yyyy-MM-dd for readability
              expireText = maybe.slice(0, 4) + '-' + maybe.slice(4, 6) + '-' + maybe.slice(6, 8);
            }
          }
        } catch (e) { expireText = ''; }
        tr.innerHTML = `<td>${l.licenseId}</td>
                        <td style="font-family:monospace;">${l.licenseKey}</td>
                        <td>${l.productName || ('#' + l.productId)}</td>
                        <td>${l.orderId || ''}</td>
                        <td>${activeBadge}</td>
                        <td>${expireText}</td>
                        <td>${actDate}</td>
                        <td>${deviceText}</td>`;
        keysTbody.appendChild(tr);
      });
      if (data.content.length === 0) {
        const tr = document.createElement('tr');
        const colSpan = 8;
        tr.innerHTML = `<td colspan="${colSpan}" class="footer-note">No keys for the current seller or sellerId is incorrect.</td>`;
        keysTbody.appendChild(tr);
      }
      paginateTable(keysTbody, keysPager, keysPageState.size);
      if (keysPager) {
        keysPager.innerHTML = ''; const total = keysPageState.totalPages;
        if (total > 1) {
          const mk = (label, page, disabled, current) => { const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = disabled; if (current) b.setAttribute('aria-current', 'page'); b.addEventListener('click', () => { keysPageState.page = page; loadSellerKeys(false); }); return b; };
          keysPager.appendChild(mk('«', Math.max(0, keysPageState.page - 1), keysPageState.page === 0, false));
          {
            const wSize = 10; let startIdx = 0; if (total <= wSize) startIdx = 0; else { if (keysPageState.page <= wSize - 1) startIdx = 0; else startIdx = keysPageState.page - (wSize - 1); if (startIdx > total - wSize) startIdx = total - wSize; }
            const endIdx = Math.min(total - 1, startIdx + wSize - 1);
            for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i + 1), i, false, i === keysPageState.page); btn.classList.add('page-btn'); if (i === keysPageState.page) btn.classList.add('active'); keysPager.appendChild(btn); }
          }
          keysPager.appendChild(mk('»', Math.min(total - 1, keysPageState.page + 1), keysPageState.page === total - 1, false));
        }
      }
    }

    document.getElementById('key_btnFilter')?.addEventListener('click', () => loadSellerKeys(true));
    document.getElementById('key_btnReset')?.addEventListener('click', () => {
      const p = document.getElementById('key_product'); if (p) p.value = '';
      const a = document.getElementById('key_active'); if (a) a.value = '';
      const s = document.getElementById('key_search'); if (s) s.value = '';
      loadSellerKeys(true);
    });
    document.getElementById('key_search')?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); loadSellerKeys(true); } });

    // Toggle active by clicking the ON/OFF badge
    keysTbody?.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-toggle-lic]'); if (!btn) return;
      const id = btn.getAttribute('data-toggle-lic');
      // Determine current state by class name
      const isOn = btn.classList.contains('pill') && btn.classList.contains('good');
      const next = !isOn;
      const res = await fetch(`/api/seller/${sellerIdVal}/licenses/${id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isActive: next })
      });
      if (res.ok) {
        showToast(next ? 'Key enabled' : 'Key disabled', 'success');
        loadSellerKeys(false);
      } else {
        showToast('Failed to update key', 'error');
      }
    });

    // Populate product filter select (reuse my products API)
    (async function populateProductsForKeys() {
      if (!sellerIdVal) return; const sel = document.getElementById('key_product'); if (!sel) return;
      try {
        const res = await fetch(`/api/products?sellerId=${sellerIdVal}`); if (!res.ok) return; const list = await res.json();
        list.forEach(p => { const o = document.createElement('option'); o.value = p.productId; o.textContent = p.name || ('#' + p.productId); sel.appendChild(o); });
      } catch (_) { }
    })();

    if (window.location.hash === '#keys') setTimeout(() => loadSellerKeys(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#keys') loadSellerKeys(false); });

    // ================= Products Panel (list + filters in-place) =================
    const productsGrid = document.getElementById('prdGrid');
    const productsPager = document.getElementById('pgProducts');
    const prdCategorySel = document.getElementById('prd_category');
    let productsPageState = { page: 0, size: 18, totalPages: 0 };

    async function populateCategoriesOnce() {
      if (!prdCategorySel || prdCategorySel.getAttribute('data-loaded') === '1') return;
      try {
        const res = await fetch('/api/categories');
        if (!res.ok) return;
        const cats = await res.json();
        cats.forEach(c => { const o = document.createElement('option'); o.value = c.categoryId; o.textContent = c.name; prdCategorySel.appendChild(o); });
        prdCategorySel.setAttribute('data-loaded', '1');
      } catch (_) { }
    }

    async function loadProductsPanel(resetPage = false) {
      if (!productsGrid) return;
      if (resetPage) productsPageState.page = 0;
      await populateCategoriesOnce();

      // Compose params and a human-readable filter description
      const params = new URLSearchParams();
      params.set('page', productsPageState.page);
      params.set('size', productsPageState.size);
      // Restrict to current seller by default
      try {
        const sellerIdEl = document.getElementById('sellerId');
        const userIdEl = document.getElementById('userId');
        const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
        if (sellerId) params.set('sellerId', String(sellerId));
      } catch (_) { }
      const parts = [];
      const s = document.getElementById('prd_search')?.value.trim(); if (s) { params.set('search', s); parts.push(`keyword "${s}"`); }
      const cat = prdCategorySel?.value; if (cat) { params.set('categoryId', cat); const opt = prdCategorySel.options[prdCategorySel.selectedIndex]; if (opt && opt.text) parts.push(`category "${opt.text}"`); }
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
      try { if (typeof showToast === 'function') showToast('Loading products' + (parts.length ? ' by ' + parts.join(', ') : ''), 'info', { duration: 1200 }); } catch (_) { }
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
          const st = (p.status || '').toLowerCase();
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
          card.addEventListener('click', () => { const id = p.productId; loadProduct(id).then(() => openModal(productModal)); });
          productsGrid.appendChild(card);
        });
        if (productsPager) {
          productsPager.innerHTML = ''; const total = productsPageState.totalPages;
          if (total > 1) {
            const mk = (label, page, disabled, current) => { const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = disabled; if (current) b.setAttribute('aria-current', 'page'); b.addEventListener('click', () => { productsPageState.page = page; loadProductsPanel(false); }); return b; };
            productsPager.appendChild(mk('«', Math.max(0, productsPageState.page - 1), productsPageState.page === 0, false));
            {
              const wSize = 10; let startIdx = 0; if (total <= wSize) startIdx = 0; else { if (productsPageState.page <= wSize - 1) startIdx = 0; else startIdx = productsPageState.page - (wSize - 1); if (startIdx > total - wSize) startIdx = total - wSize; }
              const endIdx = Math.min(total - 1, startIdx + wSize - 1);
              for (let i = startIdx; i <= endIdx; i++) { const btn = mk(String(i + 1), i, false, i === productsPageState.page); btn.classList.add('page-btn'); if (i === productsPageState.page) btn.classList.add('active'); productsPager.appendChild(btn); }
            }
            productsPager.appendChild(mk('»', Math.min(total - 1, productsPageState.page + 1), productsPageState.page === total - 1, false));
          }
        }
        try { showToast(`Loaded ${data.content ? data.content.length : 0} products`, 'info', { duration: 1200 }); } catch (_) { }
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
      const s = document.getElementById('prd_search'); if (s) s.value = '';
      if (prdCategorySel) prdCategorySel.value = '';
      const r = document.getElementById('prd_rating'); if (r) r.value = '';
      const d = document.getElementById('prd_downloads'); if (d) d.value = '';
      const st = document.getElementById('prd_status'); if (st) st.value = '';
      loadProductsPanel(true);
    });
    document.getElementById('prd_search')?.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); loadProductsPanel(true); } });
    // Auto apply when changing status
    document.getElementById('prd_status')?.addEventListener('change', () => loadProductsPanel(true));
    if (window.location.hash === '#products') setTimeout(() => loadProductsPanel(true), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#products') loadProductsPanel(false); });

    // ================= Generate Keys Panel =================
    function onlyPublicProducts(list) {
      return Array.isArray(list) ? list.filter(p => (p.status || '').toLowerCase() === 'public') : [];
    }
    async function populatePublicProductsForGen() {
      const sel = document.getElementById('gk_product'); if (!sel) return;
      if (sel.getAttribute('data-loaded') === '1') return;
      try {
        const sellerIdEl = document.getElementById('sellerId');
        const userIdEl = document.getElementById('userId');
        const sellerId = (userIdEl && userIdEl.textContent && userIdEl.textContent.trim()) ? Number(userIdEl.textContent.trim()) : (sellerIdEl ? Number(sellerIdEl.textContent.trim()) : null);
        if (!sellerId) return;
        const res = await fetch(`/api/products?sellerId=${sellerId}`);
        if (!res.ok) return;
        const list = await res.json();
        onlyPublicProducts(list).forEach(p => { const o = document.createElement('option'); o.value = p.productId; o.textContent = `#${p.productId} • ${p.name}`; o.dataset.qty = p.quantity ?? 0; sel.appendChild(o); });
        sel.setAttribute('data-loaded', '1');
      } catch (_) { }
    }

    // ==== Generate Keys Panel: Custom Product Table ====
    const gkProductsState = { all: [], filtered: [], page: 0, size: 5, q: '' };

    async function renderProductTableForGenKey() {
      const table = document.getElementById('gk_product_table');
      const tbody = table?.querySelector('tbody');
      const selectedDiv = document.getElementById('gk_product_selected');
      const input = document.getElementById('gk_product');
      const pager = document.getElementById('pgGenProducts');
      const qInput = document.getElementById('gk_product_q');
      const qBtn = document.getElementById('gk_product_search');
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
        gkProductsState.all = Array.isArray(list) ? list.filter(p => (p.status || '').toLowerCase() === 'public') : [];
        const applyFilter = () => {
          const q = (gkProductsState.q || '').toLowerCase();
          gkProductsState.filtered = gkProductsState.all.filter(p => !q || (p.name || '').toLowerCase().includes(q));
          gkProductsState.page = 0;
          renderPage();
        };
        const renderPage = () => {
          tbody.innerHTML = '';
          if (!gkProductsState.filtered.length) { tbody.innerHTML = '<tr><td colspan="4">Không có sản phẩm PUBLIC</td></tr>'; if (pager) pager.innerHTML = ''; return; }
          const start = gkProductsState.page * gkProductsState.size;
          const end = Math.min(start + gkProductsState.size, gkProductsState.filtered.length);
          const pageItems = gkProductsState.filtered.slice(start, end);
          pageItems.forEach(p => {
            const tr = document.createElement('tr');
            tr.className = 'clickable';
            tr.innerHTML = `<td>${p.productId}</td><td>${p.name}</td><td>${p.categoryName || ''}</td><td>${p.status}</td>`;
            tr.addEventListener('click', () => {
              input.value = p.productId;
              selectedDiv.textContent = `Đã chọn: #${p.productId} • ${p.name}`;
              tbody.querySelectorAll('tr').forEach(row => row.classList.remove('selected'));
              tr.classList.add('selected');
            });
            tbody.appendChild(tr);
          });
          if (pager) {
            pager.innerHTML = '';
            const totalPages = Math.max(1, Math.ceil(gkProductsState.filtered.length / gkProductsState.size));
            if (totalPages > 1) {
              const mk = (label, p, disabled, active) => { const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = !!disabled; if (active) b.classList.add('active'); b.addEventListener('click', () => { gkProductsState.page = p; renderPage(); }); return b; };
              pager.appendChild(mk('«', Math.max(0, gkProductsState.page - 1), gkProductsState.page === 0, false));
              for (let i = 0; i < totalPages; i++) { pager.appendChild(mk(String(i + 1), i, false, i === gkProductsState.page)); }
              pager.appendChild(mk('»', Math.min(totalPages - 1, gkProductsState.page + 1), gkProductsState.page >= totalPages - 1, false));
            }
          }
        };
        // Hook search events
        if (qBtn) qBtn.addEventListener('click', () => { gkProductsState.q = (qInput?.value || '').trim(); applyFilter(); });
        if (qInput) qInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); gkProductsState.q = (qInput.value || '').trim(); applyFilter(); } });
        // First render
        applyFilter();
      } catch (e) {
        tbody.innerHTML = '<tr><td colspan="4">Lỗi tải sản phẩm</td></tr>';
      }
    }

    // ==== Generate Keys Panel: User selection with search + pagination ====
    const gkUsersState = { page: 0, size: 8, totalPages: 0, q: '', type: '' };

    async function loadGenUsers(resetPage = false) {
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
          const mk = (label, page, disabled, current) => { const b = document.createElement('button'); b.type = 'button'; b.className = 'btn'; b.textContent = label; b.disabled = disabled; if (current) b.setAttribute('aria-current', 'page'); b.addEventListener('click', () => { gkUsersState.page = page; loadGenUsers(false); }); return b; };
          pager.appendChild(mk('«', Math.max(0, gkUsersState.page - 1), gkUsersState.page === 0, false));
          // Server pager for generate-keys users: sliding window (7 pages only)
          const totalG = gkUsersState.totalPages;
          const wSizeG = 7;
          let startG = 0;
          if (totalG <= wSizeG) startG = 0; else { if (gkUsersState.page <= wSizeG - 1) startG = 0; else startG = gkUsersState.page - (wSizeG - 1); if (startG > totalG - wSizeG) startG = totalG - wSizeG; }
          const endG = Math.min(totalG - 1, startG + wSizeG - 1);
          for (let i = startG; i <= endG; i++) { const b = mk(String(i + 1), i, false, i === gkUsersState.page); b.classList.add('page-btn'); if (i === gkUsersState.page) b.classList.add('active'); pager.appendChild(b); }
          pager.appendChild(mk('»', Math.min(gkUsersState.totalPages - 1, gkUsersState.page + 1), gkUsersState.page === gkUsersState.totalPages - 1, false));
        }
      } catch (_) {
        tbody.innerHTML = '<tr><td colspan="4">Error loading users</td></tr>';
      }
    }

    function initGenUserSearchHandlers() {
      const q = document.getElementById('gk_user_q');
      const t = document.getElementById('gk_user_type');
      const btn = document.getElementById('gk_user_search');
      if (q) q.addEventListener('keydown', e => { if (e.key === 'Enter') { e.preventDefault(); gkUsersState.q = (q.value || '').trim(); loadGenUsers(true); } });
      if (t) t.addEventListener('change', () => { gkUsersState.type = (t.value || '').trim(); loadGenUsers(true); });
      if (btn) btn.addEventListener('click', () => { gkUsersState.q = (q?.value || '').trim(); gkUsersState.type = (t?.value || '').trim(); loadGenUsers(true); });
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
      let qty = document.getElementById('gk_qty')?.value ? parseInt(document.getElementById('gk_qty').value, 10) : 0;
      const selUser = document.getElementById('gk_user')?.value ? Number(document.getElementById('gk_user').value) : undefined;
      const orderItemInput = document.getElementById('gk_order_item')?.value ? Number(document.getElementById('gk_order_item').value) : undefined;
      if (!pid) { showToast('Vui lòng chọn sản phẩm PUBLIC', 'error'); return; }
      if (!qty || qty <= 0) { showToast('Số lượng phải > 0', 'error'); return; }
      // If no order item is provided, require a user so backend can auto-create order
      if (!orderItemInput && !selUser) {
        showToast('Không có Order Item ID: hãy chọn User để hệ thống tự tạo đơn hàng', 'error');
        return;
      }
      try {
        const res = await fetch('/api/seller/' + (document.getElementById('userId')?.textContent?.trim() || document.getElementById('sellerId')?.textContent?.trim()) + '/licenses/generate', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            productId: pid,
            expireDate: exp,
            quantity: qty,
            userId: selUser,
            orderItemId: orderItemInput
          })
        });
        if (!res.ok) { const t = await res.text(); showToast(t || 'Failed to generate keys', 'error'); return; }
        const data = await res.json();
        showToast(`Đã tạo ${data.generated} key (còn lại ${data.remaining})`, 'success');
        // Reload keys list and navigate to Keys panel so new keys are visible immediately
        try { if (typeof loadSellerKeys === 'function') loadSellerKeys(true); } catch (e) { /* ignore */ }
        try { if (typeof showPanelByHash === 'function') showPanelByHash('#keys'); } catch (e) { /* ignore */ }
      } catch (e) { showToast('Lỗi tạo key', 'error'); }
    });

    if (window.location.hash === '#gen-keys') setTimeout(() => initGenerateKeys(), 120);
    window.addEventListener('hashchange', () => { if (window.location.hash === '#gen-keys') initGenerateKeys(); });

    // Initialize collapsible menu groups: turns .menu-group > .menu-group-title into toggles
    (function initMenuGroupToggles() {
      try {
        const groups = Array.from(document.querySelectorAll('.menu-group'));
        groups.forEach(g => {
          const title = g.querySelector('.menu-group-title');
          if (!title) return;
          // wrap links into container for easier hide/show
          let items = g.querySelector('.menu-items');
          if (!items) {
            items = document.createElement('div');
            items.className = 'menu-items';
            // move all anchor.item children into items
            Array.from(g.querySelectorAll('a.item')).forEach(a => items.appendChild(a));
            g.appendChild(items);
          }
          // make title button-like and add aria attributes
          title.setAttribute('role', 'button');
          title.setAttribute('tabindex', '0');
          title.setAttribute('aria-expanded', String(true));
          title.classList.add('menu-group-toggle');

          const toggle = () => {
            const expanded = title.getAttribute('aria-expanded') === 'true';
            title.setAttribute('aria-expanded', String(!expanded));
            items.style.display = expanded ? 'none' : '';
          };
          title.addEventListener('click', toggle);
          title.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); } });
        });
      } catch (e) { console.debug('menu-group init error', e); }
    })();
  });
})();
