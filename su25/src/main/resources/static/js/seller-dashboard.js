/* Seller Dashboard client script
   - Progressive enhancement: runs after DOM ready
   - No external deps except Chart.js (loaded via CDN in template)
   - Features: count-up anim, chart render, simple pagination for tables */

;(function() {
  function onReady(fn){ if(document.readyState!=='loading'){ fn(); } else { document.addEventListener('DOMContentLoaded', fn); } }

  function easeOutCubic(t){ return 1 - Math.pow(1 - t, 3); }

  function animateCount(el){
    const targetAttr = el.getAttribute('data-count');
    if (!targetAttr) return;
    const target = Number(targetAttr.toString().replace(/,/g,'')) || 0;
    const dur = 900; // ms
    const start = performance.now();
    function tick(now){
      const p = Math.min(1, (now - start)/dur);
      const v = Math.round(target * easeOutCubic(p));
      el.textContent = v.toLocaleString('en-US');
      if (p < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
  }

  function readCSVAttr(el, name){
    const s = el.getAttribute(name) || '';
    // Thymeleaf provides comma-separated strings already escaped
    return s.split(',').map(x => x.trim()).filter(Boolean);
  }

  function initChart(){
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
          x: { display: true, grid: { display:false }, ticks: { color: '#a8b0d3', maxTicksLimit: 8 } },
          y: { display: true, grid: { color: 'rgba(255,255,255,0.06)' }, ticks: { color: '#a8b0d3', callback: v => `$${Number(v).toLocaleString('en-US')}` } }
        },
        elements: { line: { cubicInterpolationMode: 'monotone' } }
      }
    });
  }

  function paginateTable(tbody, pager, pageSize){
    const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.querySelector('td.footer-note'));
    if (rows.length <= pageSize) { pager.innerHTML = ''; return; }
    const total = Math.ceil(rows.length / pageSize);
    let current = 1;

    function render(){
      rows.forEach((tr, idx) => {
        const p = Math.floor(idx / pageSize) + 1;
        tr.style.display = (p === current) ? '' : 'none';
      });
      // Build controls
      pager.innerHTML = '';
      const mkBtn = (label, page, disabled, ariaCurrent) => {
        const b = document.createElement('button');
        b.type = 'button';
        b.className = 'btn';
        b.textContent = label;
        if (ariaCurrent) b.setAttribute('aria-current', 'page');
        b.disabled = !!disabled;
        b.setAttribute('aria-label', `Trang ${page}`);
        b.addEventListener('click', () => { current = page; render(); pager.scrollIntoView({behavior:'smooth', block:'nearest'}); });
        return b;
      };
      pager.appendChild(mkBtn('«', Math.max(1, current - 1), current === 1, false));
      for (let i = 1; i <= total; i++) {
        pager.appendChild(mkBtn(String(i), i, false, i === current));
      }
      pager.appendChild(mkBtn('»', Math.min(total, current + 1), current === total, false));
    }
    render();
  }

  function applyTheme(theme){
    const root = document.documentElement;
    root.classList.remove('theme-dark','theme-light');
    if (theme === 'dark') root.classList.add('theme-dark');
    else if (theme === 'light') root.classList.add('theme-light');
    // update theme-color meta for browser UI
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'light' ? '#f6f7fb' : '#0b1020');
    // toggle icon
    const btn = document.getElementById('themeToggle');
    if (btn){
      btn.innerHTML = theme === 'light' ? '<i class="ti ti-moon"></i>' : '<i class="ti ti-sun"></i>';
      btn.setAttribute('aria-label', 'Chuyển giao diện');
      btn.title = 'Chuyển giao diện';
    }
  }

  onReady(function(){
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
    if (recentOrdersTbody && recentOrdersPager) paginateTable(recentOrdersTbody, recentOrdersPager, 8);

    // Theme toggle with system default
    const userPref = localStorage.getItem('theme');
    if (userPref === 'light' || userPref === 'dark') applyTheme(userPref);
    else applyTheme(window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');

    const btn = document.getElementById('themeToggle');
    if (btn){
      btn.addEventListener('click', () => {
        const isLight = document.documentElement.classList.contains('theme-light');
        const next = isLight ? 'dark' : 'light';
        localStorage.setItem('theme', next);
        applyTheme(next);
      });
    }
  });
})();
