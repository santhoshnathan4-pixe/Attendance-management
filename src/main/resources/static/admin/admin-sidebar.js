/* GROWBYTEE admin sidebar - one shared file for every admin page.
   Usage: <script src="admin-sidebar.js" defer></script> (old <aside class="sidebar"> is hidden automatically)
   Always-visible dark icon rail; hover to see names. */
/* ===== waiting banner: if the server is slow (sleeping), tell the user instead of showing a blank page ===== */
(function () {
  if (window.__waitBanner || !window.fetch) return;
  window.__waitBanner = true;
  var pending = 0, timer = null, el = null, orig = window.fetch;
  function show() {
    if (!el) {
      el = document.createElement('div');
      el.id = 'waitBanner';
      el.style.cssText = 'position:fixed;left:50%;top:14px;transform:translateX(-50%);z-index:100050;background:#0F172A;color:#fff;padding:10px 16px;border-radius:12px;font:600 13px/1.4 "Segoe UI",system-ui,sans-serif;box-shadow:0 10px 30px rgba(0,0,0,.25);display:flex;align-items:center;gap:10px;max-width:92vw';
      el.innerHTML = '<span style="width:14px;height:14px;border:2px solid #fff;border-top-color:transparent;border-radius:50%;display:inline-block;animation:wbspin .8s linear infinite;flex-shrink:0"></span><span>Server is waking up. Please wait, loading your data (can take up to 1 minute)...</span><style>@keyframes wbspin{to{transform:rotate(360deg)}}</style>';
      document.body.appendChild(el);
    }
    el.style.display = 'flex';
  }
  function hide() { if (el) el.style.display = 'none'; }
  window.fetch = function () {
    pending++;
    if (pending === 1) { clearTimeout(timer); timer = setTimeout(function () { if (pending > 0) show(); }, 3000); }
    var p = orig.apply(this, arguments);
    var done = function () { pending = Math.max(0, pending - 1); if (pending === 0) { clearTimeout(timer); hide(); } };
    p.then(done, done);
    return p;
  };
})();
(function () {
  var P = {
    dash: '<rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/>',
    users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
    leave: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18M9 16l2 2 4-4"/>',
    wallet: '<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>',
    chart: '<path d="M3 3v18h18M18 17V9M13 17V5M8 17v-3"/>',
    day: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
    clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    branch: '<rect x="4" y="2" width="16" height="20" rx="2"/><path d="M9 22v-4h6v4M8 6h.01M16 6h.01M12 6h.01M8 10h.01M16 10h.01M12 10h.01M8 14h.01M16 14h.01M12 14h.01"/>',
    ai: '<path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/><path d="M19 17v4M17 19h4"/>',
    hist: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5M12 7v5l4 2"/>',
    out: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
    shield: '<path d="M12 3l8 3v6c0 4.5-3.2 8.3-8 9-4.8-.7-8-4.5-8-9V6z"/><path d="m9 12 2 2 4-4"/>',
    pin: '<path d="M12 17v5M9 3h6l-1 7 3 3H7l3-3z"/>'
  };
  var MENU = [
    ['Main', [['Dashboard', '/admin-dashboard.html', 'dash'], ['Employees', '/admin/employees.html', 'users'], ['Leave & Attendance', '/admin/leave.html', 'leave'], ['Salary', '/admin/salary.html', 'wallet'], ['Monthly Report', '/admin/monthly-report.html', 'chart']]],
    ['Setup', [['Working Days', '/admin/working-day-settings.html', 'day'], ['Shifts', '/admin/shift-settings.html', 'clock'], ['Branches & Rules', '/admin/branch-management.html', 'branch']]],
    /* AI Assistant paused - may be enabled later with Gemini. To re-enable add: ['AI Assistant', '/admin/ai-assistant.html', 'ai'], */
    ['More', [['History', '/admin/history.html', 'hist'], ['Admin Access', '/admin/admin-access.html', 'shield']]]
  ];

  /* ---- who is signed in: OWNER (everything), HR (everything except Admin Access), MANAGER (limited) ----
     NOTE: this hides menu items and redirects pages in the browser. It makes the screens simple for each
     person; real protection of the server needs login tokens (planned next). */
  var ROLE = 'OWNER';
  try { ROLE = (localStorage.getItem('adminRole') || 'OWNER').toUpperCase(); } catch (e) {}
  if (ROLE !== 'HR' && ROLE !== 'MANAGER') ROLE = 'OWNER'; /* old or unknown role = full access */
  var ACCESS_LABEL = ROLE === 'OWNER' ? 'Admin Access' : 'My Account';
  var MANAGER_PAGES = ['admin-dashboard.html', 'leave.html', 'monthly-report.html', 'admin-access.html'];
  function allowed(href) {
    var page = href.split('/').pop().toLowerCase();
    if (ROLE === 'OWNER') return true;
    if (page === 'admin-access.html') return true; /* everyone may change their own password there */
    if (ROLE === 'HR') return true;
    return MANAGER_PAGES.indexOf(page) !== -1;
  }
  (function guard() {
    var page = location.pathname.split('/').pop().toLowerCase();
    var known = /\.html$/.test(page) && page !== 'admin-login.html' && page !== 'login.html';
    if (known && !allowed(page)) location.replace('/admin-dashboard.html');
  })();

  /* ---- shared colourful theme for every admin page (one place, so all pages look the same) ---- */
  var THEME = '' +
  ':root{--bg:#F3F6FB;--blue:#2563EB;--blue-l:#E6EEFF;--amber:#D97706;--amber-l:#FEF1DC;--violet:#7C3AED;--violet-l:#EFE7FE;--teal:#0891B2;--teal-l:#DDF3F8;--red-l:#FDE8E8;--ink:#0F172A;--soft:#64748B;--edge:#E6ECF4}' +
  'body{background:var(--bg)!important;color:var(--ink)}' +
  '.topbar{background:#fff!important;border-bottom:1px solid var(--edge)!important;box-shadow:0 1px 3px rgba(15,23,42,.04)}' +
  '.topbar h2{font-size:18px!important;font-weight:800!important;color:var(--ink);display:flex;align-items:center;gap:10px}' +
  '.topbar h2::before{content:"";width:5px;height:20px;border-radius:3px;background:linear-gradient(180deg,#22C55E,#2563EB)}' +
  '.avatar{background:linear-gradient(135deg,#16A34A,#2563EB)!important}' +
  '.table-card,.card,.calendar-card,.setting-calendar-card,.summary-card,.weekly-off-summary,.modal-box{border:1px solid var(--edge)!important;border-radius:16px!important;box-shadow:0 1px 3px rgba(15,23,42,.05)}' +
  '.table-card:hover,.card:hover{box-shadow:0 6px 18px rgba(15,23,42,.07)}' +
  'th{background:#EEF3FA!important;color:#475569!important;font-weight:700}' +
  'tbody tr:hover td{background:#F5F9FF}' +
  '.btn{border-radius:10px!important;font-weight:600;transition:transform .12s,box-shadow .15s,background .15s}.btn:hover{transform:translateY(-1px)}' +
  '.btn-primary,.btn-success{background:linear-gradient(135deg,#16A34A,#0F7A35)!important;color:#fff!important;border-color:transparent!important;box-shadow:0 2px 8px rgba(22,163,74,.3)}' +
  '.btn-dark{background:linear-gradient(135deg,#2563EB,#1D4ED8)!important;color:#fff!important;border-color:transparent!important;box-shadow:0 2px 8px rgba(37,99,235,.3)}' +
  '.btn-warning{background:linear-gradient(135deg,#F59E0B,#D97706)!important;color:#fff!important;border-color:transparent!important;box-shadow:0 2px 8px rgba(217,119,6,.3)}' +
  '.btn-danger{background:linear-gradient(135deg,#EF4444,#DC2626)!important;color:#fff!important;border-color:transparent!important;box-shadow:0 2px 8px rgba(220,38,38,.28)}' +
  '.btn-secondary,.btn-ghost{background:#fff!important;color:#1E293B!important;border:1.5px solid #D5DEEA!important}.btn-secondary:hover,.btn-ghost:hover{background:var(--blue-l)!important;border-color:#B6CBF8!important;color:var(--blue)!important}' +
  '.count-pill,.chip{background:var(--blue-l)!important;color:var(--blue)!important;border-radius:99px;font-weight:700}' +
  '.badge,.status-chip,.status-pill,.setting-badge,.verification-status{border-radius:99px;font-weight:700}' +
  '.late-chip{background:var(--amber-l)!important;color:#B45309!important;border-radius:99px;font-weight:700}' +
  '.admin-badge{background:var(--violet-l)!important;color:var(--violet)!important;border-radius:99px;font-weight:700}' +
  '.summary-grid .summary-card:nth-child(4n+1){border-top:4px solid #2563EB!important}.summary-grid .summary-card:nth-child(4n+2){border-top:4px solid #16A34A!important}.summary-grid .summary-card:nth-child(4n+3){border-top:4px solid #D97706!important}.summary-grid .summary-card:nth-child(4n+4){border-top:4px solid #7C3AED!important}' +
  '.summary-icon{border-radius:12px}' +
  'input,select,textarea{border-radius:10px}input:focus,select:focus,textarea:focus{border-color:#2563EB!important;box-shadow:0 0 0 4px rgba(37,99,235,.14)!important;outline:0}' +
  '.modal-header h2,.modal-header h3{color:var(--ink);font-weight:800}' +
  '.close-btn{background:#EEF3FA!important;border-radius:9px!important}.close-btn:hover{background:var(--red-l)!important;color:#DC2626}' +
  '.tab,.tabs button,[data-tab]{border-radius:10px}[data-tab].active,.tab.active{background:var(--blue-l)!important;color:var(--blue)!important;border-color:var(--blue)!important}' +
  '.message.success{background:#E3F6EA;border-color:#BFE6CC;color:#0B5A2B}.message.error{background:var(--red-l);border-color:#F9C9C9;color:#991B1B}';

  /* ---- density + polish (Zoho / HCL style): compact tables, no cut-off buttons, calm spacing ---- */
  THEME += '' +
  'body{font-family:"Segoe UI",Inter,system-ui,-apple-system,Arial,sans-serif!important;-webkit-font-smoothing:antialiased}' +
  '.content,.page,.container{max-width:1500px}' +
  '.table-wrapper,.gwrap,.details-table-wrapper{overflow-x:auto;-webkit-overflow-scrolling:touch}' +
  'table{border-collapse:separate;border-spacing:0;width:100%}' +
  'th{padding:11px 9px!important;font-size:11px!important;letter-spacing:.06em;white-space:nowrap}' +
  'td{padding:11px 9px!important;font-size:13px;border-bottom:1px solid #EEF2F7;vertical-align:middle}' +
  'tbody tr:last-child td{border-bottom:0}' +
  'td:last-child{white-space:nowrap}' +
  'td .btn+.btn{margin-left:6px}' +
  '.btn-small,.btn-sm{padding:6px 12px!important;font-size:12px!important;border-radius:8px!important}' +
  'td .btn-primary{background:#E6EEFF!important;color:#1D4ED8!important;box-shadow:none!important;border:1px solid #C9D9FB!important}' +
  'td .btn-primary:hover{background:#2563EB!important;color:#fff!important}' +
  'td .btn-danger{background:#FDECEC!important;color:#B91C1C!important;box-shadow:none!important;border:1px solid #F7CFCF!important}' +
  'td .btn-danger:hover{background:#DC2626!important;color:#fff!important}' +
  '.empty-row,.empty-state{color:var(--soft);padding:32px 12px!important;text-align:center}' +
  '.badge-blue{background:#E6EEFF!important;color:#1D4ED8!important;border-radius:8px!important;padding:4px 9px;font-size:11.5px;font-weight:700}' +
  '.dept-chip{background:#EFE7FE!important;color:#6D28D9!important;border-radius:6px;padding:2px 7px;font-size:10.5px;font-weight:700;margin-left:6px}' +
  '.top-bar{background:#fff;border:1px solid var(--edge);border-radius:14px;padding:12px 14px;gap:10px}' +
  '.main>.content,.main>section{padding-top:20px}' +
  '@media(max-width:760px){th,td{padding:9px 10px!important}.content{padding:14px!important}.topbar{padding-left:62px!important}}';

  THEME += '' +
  '.table-wrapper table th:last-child{position:sticky;right:0;background:#EEF3FA!important;box-shadow:-8px 0 8px -8px rgba(15,23,42,.18);z-index:2}' +
  '.table-wrapper table td:last-child{position:sticky;right:0;background:#fff;box-shadow:-8px 0 8px -8px rgba(15,23,42,.12)}' +
  '.table-wrapper tbody tr:hover td:last-child{background:#F5F9FF}';
  var RAIL = 76, WIDE = 232;
  function lbl(m) { return m[1].indexOf('admin-access.html') !== -1 ? ACCESS_LABEL : m[0]; }
  function svg(k) { return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + P[k] + '</svg>'; }
  function esc(t) { return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;'); }

  /* Always-visible dark icon rail. Hover (or tap on a phone) opens it to show names. */
  var css = '' +
  '.sidebar{display:none!important}' +
  ':root{--asb-w:' + RAIL + 'px}' +
  '.main{margin-left:var(--asb-w)!important;width:calc(100% - var(--asb-w))!important}' +
  '#asb{position:fixed;inset:0 auto 0 0;width:' + RAIL + 'px;background:#111513;color:#fff;z-index:90;display:flex;flex-direction:column;overflow:hidden;transition:width .2s ease,box-shadow .2s;font-family:"Segoe UI",system-ui,Arial,sans-serif;font-size:14px}' +
  '#asb.open{width:' + WIDE + 'px;box-shadow:10px 0 30px rgba(0,0,0,.3)}' +
  '#asb .hd{height:76px;display:flex;align-items:center;gap:12px;padding:0 18px;flex-shrink:0;white-space:nowrap}' +
  '#asb .lg{width:40px;height:40px;border-radius:12px;background:#16A34A;display:grid;place-items:center;font-weight:800;font-size:18px;flex-shrink:0}' +
  '#asb .nm{font-weight:800;letter-spacing:.8px;opacity:0;transition:opacity .15s}#asb.open .nm{opacity:1}' +
  '#asb nav{flex:1;overflow-y:auto;overflow-x:hidden;padding:6px 14px}#asb nav::-webkit-scrollbar{width:0}' +
  '#asb .sec{height:1px;background:rgba(255,255,255,.1);margin:12px 6px}#asb .sec span{display:none}' +
  '#asb.open .sec{height:24px;background:none;display:flex;align-items:center;margin:10px 0 2px;padding:0 14px}' +
  '#asb.open .sec span{display:block;font-size:11px;letter-spacing:.9px;text-transform:uppercase;color:#7E9186;font-weight:700}' +
  '#asb a,#asb .lo{position:relative;height:46px;display:flex;align-items:center;gap:16px;padding:0 14px;margin:4px 0;border-radius:12px;color:#B7C4BC;text-decoration:none;white-space:nowrap;cursor:pointer;border:0;background:transparent;width:100%;font:inherit;transition:background .15s,color .15s}' +
  '#asb svg{width:22px;height:22px;flex-shrink:0}' +
  '#asb a span,#asb .lo span{opacity:0;transition:opacity .15s}#asb.open a span,#asb.open .lo span{opacity:1}' +
  '#asb a:hover{background:rgba(255,255,255,.08);color:#fff}' +
  '#asb a.on{background:#232A26;color:#fff;font-weight:600}' +
  '#asb a.on::before{content:"";position:absolute;left:-14px;top:8px;bottom:8px;width:4px;border-radius:0 4px 4px 0;background:#22C55E}' +
  '#asb a.on svg{color:#4ADE80}' +
  '#asb .ft{padding:12px 14px 16px;display:flex;flex-direction:column;gap:10px;align-items:stretch}' +
  '#asb .me{display:flex;align-items:center;gap:12px;padding:0 4px;white-space:nowrap}' +
  '#asb .av{width:44px;height:44px;border-radius:50%;background:#fff;color:#111513;display:grid;place-items:center;font-weight:800;font-size:18px;flex-shrink:0}' +
  '#asb .who{opacity:0;line-height:1.25;overflow:hidden;transition:opacity .15s}#asb.open .who{opacity:1}' +
  '#asb .who b{display:block;font-size:13px;max-width:140px;overflow:hidden;text-overflow:ellipsis}#asb .who small{color:#7E9186;font-size:11px;letter-spacing:.5px}' +
  '#asb .lo{height:44px;justify-content:flex-start;background:rgba(220,38,38,.12);color:#F87171;font-weight:600}' +
  '#asb .lo:hover{background:#DC2626;color:#fff}' +
  '#asbm{display:none;position:fixed;top:10px;left:10px;z-index:91;width:42px;height:42px;border-radius:12px;border:0;background:#111513;color:#fff;place-items:center;cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,.25)}#asbm svg{width:22px;height:22px}' +
  '@media(max-width:760px){:root{--asb-w:0px}#asb{width:' + WIDE + 'px;transform:translateX(-100%);transition:transform .2s}#asb.open{transform:none}#asb .nm,#asb a span,#asb .lo span,#asb .who{opacity:1}#asb .sec{height:24px;background:none;display:flex;align-items:center;margin:10px 0 2px;padding:0 14px}#asb .sec span{display:block;font-size:11px;letter-spacing:.9px;text-transform:uppercase;color:#7E9186;font-weight:700}#asbm{display:grid}.main{padding-top:54px}}';

  function init() {
    var st = document.createElement('style'); st.textContent = css + THEME; document.head.appendChild(st);
    var cur = location.pathname.toLowerCase().split('/').pop();
    var uname = 'Admin';
    try { uname = localStorage.getItem('adminName') || 'Admin'; } catch (e) {}
    var h = '<div class="hd"><div class="lg">G</div><div class="nm">GROWBYTEE</div></div><nav>';
    MENU.forEach(function (s, i) {
      var items = s[1].filter(function (m) { return allowed(m[1]); });
      if (!items.length) return;
      if (i > 0) h += '<div class="sec"><span>' + s[0] + '</span></div>';
      items.forEach(function (m) {
        h += '<a href="' + m[1] + '" class="' + (cur === m[1].split('/').pop().toLowerCase() ? 'on' : '') + '" title="' + esc(lbl(m)) + '">' + svg(m[2]) + '<span>' + esc(lbl(m)) + '</span></a>';
      });
    });
    h += '</nav><div class="ft"><div class="me"><div class="av">' + esc(uname.trim().charAt(0).toUpperCase() || 'A') + '</div><div class="who"><b>' + esc(uname) + '</b><small>' + esc(ROLE) + '</small></div></div>' +
         '<button class="lo" id="asbOut" title="Logout">' + svg('out') + '<span>Logout</span></button></div>';
    var el = document.createElement('aside'); el.id = 'asb'; el.innerHTML = h; document.body.appendChild(el);
    var mb = document.createElement('button'); mb.id = 'asbm'; mb.setAttribute('aria-label', 'Menu'); mb.innerHTML = svg('dash'); document.body.appendChild(mb);
    var t = null;
    function wide() { return window.innerWidth > 760; }
    var armed = false; /* open only after the mouse really moves, so a stationary pointer never keeps it open */
    window.addEventListener('mousemove', function () { armed = true; }, { once: true });
    el.addEventListener('mousemove', function () { if (armed && wide() && !el.classList.contains('open')) { clearTimeout(t); el.classList.add('open'); } });
    el.addEventListener('mouseenter', function () { if (armed && wide()) { clearTimeout(t); el.classList.add('open'); } });
    el.addEventListener('click', function (e) { if (e.target.closest && e.target.closest('a')) el.classList.remove('open'); });
    el.addEventListener('mouseleave', function () { if (wide()) { t = setTimeout(function () { el.classList.remove('open'); }, 150); } });
    mb.onclick = function (e) { e.stopPropagation(); el.classList.toggle('open'); };
    document.addEventListener('click', function (e) { if (!wide() && !el.contains(e.target)) el.classList.remove('open'); });
    document.getElementById('asbOut').onclick = function () {
      if (!confirm('Are you sure you want to logout?')) return;
      try { localStorage.removeItem('adminEmail'); localStorage.removeItem('adminName'); localStorage.removeItem('adminRole'); } catch (e) {}
      location.href = '/admin/admin-login.html';
    };
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
