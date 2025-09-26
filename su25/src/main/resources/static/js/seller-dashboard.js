// Minimal helper for animating counters
function animateNumber(el, to, duration=900){
  const start = 0; const diff = to - start; const t0 = performance.now();
  function step(t){
    const p = Math.min(1,(t-t0)/duration); const val = start + diff * (1 - Math.pow(1-p,3));
    el.textContent = Intl.NumberFormat().format(val.toFixed(0));
    if(p<1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

window.addEventListener('DOMContentLoaded', () => {
  // Parallax: update CSS vars based on scroll
  const parallax = ()=>{
    const y = window.scrollY||0;
    document.documentElement.style.setProperty('--parallaxY1', `${y*.08}px`);
    document.documentElement.style.setProperty('--parallaxY2', `${-y*.06}px`);
  };
  parallax();
  window.addEventListener('scroll', parallax, {passive:true});

  // Reveal on scroll: IntersectionObserver
  const io = ('IntersectionObserver' in window) ? new IntersectionObserver((entries)=>{
    entries.forEach(e=>{ if(e.isIntersecting){ e.target.classList.add('reveal-visible'); io.unobserve(e.target);} });
  }, { rootMargin: '0px 0px -10% 0px', threshold: .1 }) : null;
  document.querySelectorAll('.card, .table, .title').forEach(el=>{
    el.classList.add('reveal'); if(io) io.observe(el); else el.classList.add('reveal-visible');
  });

  // Page transition: add class on link clicks for a quick fade
  document.body.addEventListener('click', (e)=>{
    const a = e.target.closest('a');
    if(!a) return; const href = a.getAttribute('href');
    if(!href || href.startsWith('#') || a.target==='_blank' || href.startsWith('javascript:')) return;
    document.body.classList.add('page-leave');
    setTimeout(()=>{ window.location.href = href; }, 120);
    e.preventDefault();
  }, {capture:true});
  document.querySelectorAll('[data-count]')?.forEach(el => {
    const v = Number(el.getAttribute('data-count')||'0');
    animateNumber(el, v);
  });

  // Build chart if canvas exists
  const ctx = document.getElementById('revenueChart');
  if (ctx && window.Chart) {
    const labels = (ctx.dataset.labels||'').split(',');
    const data = (ctx.dataset.data||'').split(',').map(Number);
    new Chart(ctx.getContext('2d'), {
      type: 'line',
      data: { labels, datasets: [{
        label: 'Revenue', data, tension: .35, fill: true,
        borderColor: '#6c8cff', backgroundColor: (c) => {
          const {ctx, chartArea} = c.chart; if(!chartArea) return 'rgba(108,140,255,.35)';
          const g = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          g.addColorStop(0, 'rgba(108,140,255,.35)');
          g.addColorStop(1, 'rgba(108,140,255,0)');
          return g; },
        pointRadius: 0, borderWidth: 2.2
      }]},
      options: {
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            mode: 'index', intersect: false,
            titleFont: { size: 13 }, bodyFont: { size: 13 }, footerFont: { size: 12 },
            callbacks: { label: (c)=> ` $${c.parsed.y.toFixed(2)}` }
          }
        },
        scales: {
          x: { ticks: { color: '#96a0bf', font:{ size: 12 } }, grid: { color: 'rgba(255,255,255,.06)' } },
          y: { ticks: { color: '#96a0bf', font:{ size: 12 }, callback:(v)=>'$'+v }, grid: { color: 'rgba(255,255,255,.06)' } }
        },
        interaction: { mode: 'index', intersect: false },
      }
    });
  }
  // Simple client-side pagination for 3 tables
  function paginate(tbodyId, pagerId, pageSize=5){
    const tbody = document.getElementById(tbodyId);
    const pager = document.getElementById(pagerId);
    if(!tbody || !pager) return;
    const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.querySelector('.footer-note'));
    let page = 1; const total = Math.max(1, Math.ceil(rows.length / pageSize));
    function render(){
      rows.forEach((r,i)=>{ r.style.display = (Math.ceil((i+1)/pageSize) === page) ? '' : 'none'; });
      const footerRow = tbody.querySelector('tr .footer-note')?.closest('tr') || null;
      if(rows.length === 0){ if(footerRow) footerRow.style.display=''; } else { if(footerRow) footerRow.style.display='none'; }
      pager.innerHTML = '';
      if(total > 1){
        const info = document.createElement('span'); info.className='page-info'; info.textContent = `Trang ${page}/${total}`; pager.appendChild(info);
        const prev = document.createElement('button'); prev.textContent='Trước'; prev.disabled = page<=1; prev.onclick=()=>{ page=Math.max(1,page-1); render(); }; pager.appendChild(prev);
        const next = document.createElement('button'); next.textContent='Sau'; next.disabled = page>=total; next.onclick=()=>{ page=Math.min(total,page+1); render(); }; pager.appendChild(next);
      }

      // Add invisible filler rows so that each page renders with equal number of rows,
      // keeping the card/table height stable without scrollbars or big gaps.
      // First remove existing fillers
      tbody.querySelectorAll('tr.filler').forEach(n=>n.remove());
  const visibleCount = rows.filter((r,i)=>Math.ceil((i+1)/pageSize)===page).length + ((rows.length===0 && footerRow)?1:0);
      const need = Math.max(0, pageSize - visibleCount);
      for(let i=0;i<need;i++){
        const filler = document.createElement('tr');
        filler.className = 'filler';
        // Create the same number of cells as the first visible row to maintain width
        const refRow = rows[0];
        const headerRow = tbody.parentElement.querySelector('thead tr');
        const colCount = refRow ? refRow.children.length : (headerRow?.children.length || 1);
        for(let c=0;c<colCount;c++){
          const td = document.createElement('td');
          // Copy responsive classes like 'hide-md' to keep columns aligned on mobile
          const refCell = refRow?.children?.[c] || headerRow?.children?.[c];
          if(refCell && refCell.className){ td.className = refCell.className; }
          td.innerHTML = '&nbsp;';
          filler.appendChild(td);
        }
        tbody.appendChild(filler);
      }
    }
    render();
  }
  paginate('tbTopProducts','pgTopProducts',5);
  paginate('tbRecentOrders','pgRecentOrders',5);
  paginate('tbLowStock','pgLowStock',5);
});
