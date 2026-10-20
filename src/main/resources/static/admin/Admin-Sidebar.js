/* GROWBYTEE admin sidebar - one shared file for every admin page.
   Usage: <script src="admin-sidebar.js" defer></script> (old <aside class="sidebar"> is hidden automatically)
   Collapsed icon rail; hover to open; pin button keeps it open. */
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
    pin: '<path d="M12 17v5M9 3h6l-1 7 3 3H7l3-3z"/>'
  };
  var MENU = [
    ['Main', [['Dashboard', '/admin-dashboard.html', 'dash'], ['Employees', '/admin/employees.html', 'users'], ['Leave & Attendance', '/admin/leave.html', 'leave'], ['Salary', '/admin/salary.html', 'wallet'], ['Monthly Report', '/admin/monthly-report.html', 'chart']]],
    ['Setup', [['Working Days', '/admin/working-day-settings.html', 'day'], ['Shifts', '/admin/shift-settings.html', 'clock'], ['Branches & Rules', '/admin/branch-management.html', 'branch']]],
    /* AI Assistant paused - may be enabled later with Gemini. To re-enable add: ['AI Assistant', '/admin/ai-assistant.html', 'ai'], */
    ['More', [['History', '/admin/history.html', 'hist']]]
  ];
  var W0 = 68, W1 = 236, pinned = false;
  try { pinned = localStorage.getItem('asbPinned') === '1'; } catch (e) {}

  function svg(k) { return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' + P[k] + '</svg>'; }

  var css = '' +
  '.sidebar{display:none!important}' +
  ':root{--asb-w:0px}body.asb-pin{--asb-w:' + W1 + 'px}' +
  '.main{margin-left:var(--asb-w)!important;width:calc(100% - var(--asb-w))!important;transition:margin .2s,width .2s}' +
  '#asb{position:fixed;inset:0 auto 0 0;width:' + W1 + 'px;background:#0F1A14;color:#fff;z-index:90;display:flex;flex-direction:column;overflow:hidden;transform:translateX(-100%);transition:transform .22s ease,box-shadow .22s;font-family:"Segoe UI",system-ui,Arial,sans-serif;font-size:14px}' +
  '#asb.open,body.asb-pin #asb{transform:none}#asb.open:not(.pin){box-shadow:8px 0 28px rgba(0,0,0,.28)}' +
  '#asbhot{position:fixed;left:0;top:0;bottom:0;width:10px;z-index:999}' +
  '#asbtab{position:fixed;left:0;top:50%;margin-top:-28px;width:14px;height:56px;border-radius:0 8px 8px 0;background:#16A34A;z-index:999;display:grid;place-items:center;color:#fff;opacity:.85;transition:opacity .2s}' +
  '#asbtab svg{width:10px;height:10px}body.asb-pin #asbhot,body.asb-pin #asbtab,#asb.open~#asbtab{opacity:0;pointer-events:none}' +
  '#asb .hd{height:60px;display:flex;align-items:center;gap:12px;padding:0 15px;border-bottom:1px solid rgba(255,255,255,.08);flex-shrink:0;white-space:nowrap}' +
  '#asb .lg{width:38px;height:38px;border-radius:10px;background:#16A34A;display:grid;place-items:center;font-weight:800;font-size:17px;flex-shrink:0}' +
  '#asb .nm{font-weight:700;letter-spacing:.6px;flex:1}' +
  '#asb .pn{width:28px;height:28px;border:0;border-radius:6px;background:transparent;color:#8FA399;cursor:pointer;display:grid;place-items:center;flex-shrink:0}' +
  '#asb .pn svg{width:16px;height:16px}#asb .pn:hover{background:rgba(255,255,255,.1);color:#fff}body.asb-pin #asb .pn{color:#4ADE80}' +
  '#asb nav{flex:1;overflow-y:auto;overflow-x:hidden;padding:10px 12px}#asb nav::-webkit-scrollbar{width:0}' +
  '#asb .sec{height:26px;display:flex;align-items:center;padding:0 12px;font-size:11px;letter-spacing:.8px;text-transform:uppercase;color:#6F8478;margin-top:6px}' +
  '#asb a,#asb .lo{height:42px;display:flex;align-items:center;gap:14px;padding:0 12px;margin:2px 0;border-radius:9px;color:#C9D6CE;text-decoration:none;white-space:nowrap;cursor:pointer;border:0;background:transparent;width:100%;font:inherit;transition:background .15s,color .15s}' +
  '#asb svg{width:20px;height:20px;flex-shrink:0}' +
  '#asb a:hover,#asb .lo:hover{background:rgba(255,255,255,.08);color:#fff}#asb a.on{background:#16A34A;color:#fff;font-weight:600}' +
  '#asb .ft{padding:10px 12px 14px;border-top:1px solid rgba(255,255,255,.08)}#asb .lo:hover{background:rgba(220,38,38,.18);color:#FCA5A5}' +
  '#asbm{display:none;position:fixed;top:10px;left:10px;z-index:91;width:40px;height:40px;border-radius:10px;border:0;background:#0F1A14;color:#fff;place-items:center;cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,.25)}#asbm svg{width:20px;height:20px}' +
  '@media(max-width:760px){body.asb-pin{--asb-w:0px}#asbhot,#asbtab{display:none}#asbm{display:grid}.main{padding-top:52px}#asb .pn{display:none}}';

  function init() {
    var st = document.createElement('style'); st.textContent = css; document.head.appendChild(st);
    var cur = location.pathname.toLowerCase();
    var h = '<div class="hd"><div class="lg">G</div><div class="nm">GROWBYTEE</div><button class="pn" id="asbPn" title="Keep open">' + svg('pin') + '</button></div><nav>';
    MENU.forEach(function (s) {
      h += '<div class="sec">' + s[0] + '</div>';
      s[1].forEach(function (m) {
        h += '<a href="' + m[1] + '" class="' + (cur === m[1].split('/').pop() ? 'on' : '') + '" title="' + m[0] + '">' + svg(m[2]) + '<span>' + m[0].replace('&', '&amp;') + '</span></a>';
      });
    });
    h += '</nav><div class="ft"><button class="lo" id="asbOut" title="Logout">' + svg('out') + '<span>Logout</span></button></div>';
    var el = document.createElement('aside'); el.id = 'asb'; el.innerHTML = h; document.body.appendChild(el);
    var hot = document.createElement('div'); hot.id = 'asbhot'; document.body.appendChild(hot);
    var tab = document.createElement('div'); tab.id = 'asbtab'; tab.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="m9 6 6 6-6 6"/></svg>'; document.body.appendChild(tab);
    var t = null;
    function openSb() { clearTimeout(t); el.classList.add('open'); }
    function closeSb() { clearTimeout(t); t = setTimeout(function () { el.classList.remove('open'); }, 250); }
    hot.addEventListener('mouseenter', openSb); tab.addEventListener('mouseenter', openSb);
    el.addEventListener('mouseenter', openSb); el.addEventListener('mouseleave', closeSb);
    hot.addEventListener('mouseleave', closeSb);
    var mb = document.createElement('button'); mb.id = 'asbm'; mb.innerHTML = svg('dash'); document.body.appendChild(mb);
    mb.onclick = function () { el.classList.toggle('open'); };
    document.addEventListener('click', function (e) { if (!el.contains(e.target) && e.target !== mb && !mb.contains(e.target)) el.classList.remove('open'); });
    function setPin(v) { pinned = v; document.body.classList.toggle('asb-pin', v); el.classList.toggle('pin', v); if (v) el.classList.remove('open'); try { localStorage.setItem('asbPinned', v ? '1' : '0'); } catch (e) {} }
    setPin(pinned);
    document.getElementById('asbPn').onclick = function () { setPin(!pinned); };
    document.getElementById('asbOut').onclick = function () {
      if (!confirm('Are you sure you want to logout?')) return;
      try { localStorage.removeItem('adminEmail'); localStorage.removeItem('adminName'); } catch (e) {}
      location.href = '/admin/admin-login.html';
    };
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();