/* ============================================================
   app.jsx — root: state, routing, key wiring
   ============================================================ */

function uid() { return 't' + Math.random().toString(36).slice(2, 8); }

function App() {
  const [tabs, setTabs] = useState(INITIAL_TABS);
  const [activeId, setActiveId] = useState('t1');
  const [view, setView] = useState('browser'); // start mid-browse to show the app in use
  const [toast, setToast] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [bookmarkedUrls, setBookmarkedUrls] = useState(['nasa.gov/pale-blue-dot', 'wikipedia.org']);
  const [settings, setSettings] = useState({
    defaultDesktop: true, blockPopups: true, askWhere: false, dnt: true
  });
  const downloadPath = '/storage/emulated/0/Download';

  const activeTab = tabs.find(function (t) { return t.id === activeId; }) || tabs[0];

  useAutoFocus(view + ':' + activeId + ':' + (activeTab && activeTab.url));

  function flash(msg, icon) {
    setToast({ msg: msg, icon: icon });
    clearTimeout(window.__toastT);
    window.__toastT = setTimeout(function () { setToast(null); }, 2000);
  }

  // ---- navigation actions ----
  function patchTab(id, patch) {
    setTabs(function (ts) { return ts.map(function (t) { return t.id === id ? Object.assign({}, t, patch) : t; }); });
  }

  function navigate(url, title, fav) {
    patchTab(activeId, {
      kind: 'page',
      url: url,
      title: title || url,
      fav: fav || activeTab.fav || FAVS.wikipedia,
      secure: url.indexOf('http://') !== 0,
      mode: settings.defaultDesktop ? (activeTab.mode || 'desktop') : 'mobile'
    });
    setView('browser');
    flash('Loading ' + url, 'public');
  }

  function newTab() {
    const id = uid();
    setTabs(function (ts) { return ts.concat([{ id: id, kind: 'home', title: 'New tab', url: '', fav: null, mode: settings.defaultDesktop ? 'desktop' : 'mobile', secure: true }]); });
    setActiveId(id);
    setView('browser');
  }

  function pickTab(id) {
    setActiveId(id);
    setView('browser');
  }

  function closeTab(id) {
    setTabs(function (ts) {
      const idx = ts.findIndex(function (t) { return t.id === id; });
      const next = ts.filter(function (t) { return t.id !== id; });
      if (next.length === 0) {
        const nid = uid();
        const fresh = [{ id: nid, kind: 'home', title: 'New tab', url: '', fav: null, mode: 'desktop', secure: true }];
        setActiveId(nid);
        return fresh;
      }
      if (id === activeId) setActiveId(next[Math.max(0, idx - 1)].id);
      return next;
    });
  }

  function closeAll() {
    const nid = uid();
    setTabs([{ id: nid, kind: 'home', title: 'New tab', url: '', fav: null, mode: 'desktop', secure: true }]);
    setActiveId(nid);
    flash('All tabs closed', 'tab');
  }

  // Rail "Home": take the current tab to the home page (standard browser home button)
  function goHome() {
    patchTab(activeId, { kind: 'home', title: 'New tab', url: '', fav: null, secure: true });
    setMenuOpen(false);
    setView('browser');
  }

  // Rail navigation dispatcher
  function railNav(v) {
    setMenuOpen(false);
    if (v === 'home') goHome();
    else setView(v);
  }

  function toggleMode() {
    const m = activeTab.mode === 'mobile' ? 'desktop' : 'mobile';
    patchTab(activeId, { mode: m });
    flash('Switched to ' + m + ' site', m === 'mobile' ? 'smartphone' : 'desktop_windows');
  }

  function toggleBookmark() {
    const u = activeTab.url;
    setBookmarkedUrls(function (b) {
      if (b.indexOf(u) > -1) { flash('Removed from bookmarks', 'star_outline'); return b.filter(function (x) { return x !== u; }); }
      flash('Added to bookmarks', 'star'); return b.concat([u]);
    });
  }

  function onBack() {
    if (menuOpen) { setMenuOpen(false); return; }
    if (view !== 'browser') { setView('browser'); return; }
    flash('Back', 'arrow_back');
  }

  useGlobalKeys(onBack);

  function setSetting(k, v) { setSettings(function (s) { return Object.assign({}, s, { [k]: v }); }); }

  // ---- render current view ----
  let body;
  if (view === 'browser') {
    body = activeTab.kind === 'home'
      ? <HomeView onNavigate={navigate} />
      : <BrowserView
          tab={activeTab}
          tabCount={tabs.length}
          bookmarked={bookmarkedUrls.indexOf(activeTab.url) > -1}
          onNavigate={navigate}
          onToggleMode={toggleMode}
          onBookmark={toggleBookmark}
          onReload={function () { flash('Reloading…', 'refresh'); }}
          onBack={onBack}
          onMenu={function () { setMenuOpen(true); }}
          onNav={setView}
        />;
  } else if (view === 'tabs') {
    body = <TabsView tabs={tabs} activeId={activeId} onPick={pickTab} onClose={closeTab} onNew={newTab} onCloseAll={closeAll} />;
  } else if (view === 'bookmarks') {
    body = <BookmarksView onOpen={navigate} />;
  } else if (view === 'history') {
    body = <HistoryView onOpen={navigate} onToast={flash} />;
  } else if (view === 'downloads') {
    body = <DownloadsView downloadPath={downloadPath} onToast={flash} />;
  } else if (view === 'settings') {
    body = <SettingsView settings={settings} downloadPath={downloadPath} onSet={setSetting} onToast={flash} />;
  }
  // safety net: never allow a blank screen on an unhandled view
  if (!body) {
    body = activeTab.kind === 'home'
      ? <HomeView onNavigate={navigate} />
      : <HomeView onNavigate={navigate} />;
  }

  // hint bar contextual
  const hints = view === 'browser' && activeTab.kind === 'page'
    ? [{ k: '\u25B2\u25BC\u25C0\u25B6', label: 'Navigate' }, { k: 'OK', label: 'Open' }, { k: 'Back', label: 'Previous page' }]
    : null;

  return (
    <React.Fragment>
      <NavRail view={view} onNav={railNav} />
      <div className="main">
        {body}
        <KeyHints hints={hints} />
        <Toast msg={toast && toast.msg} icon={toast && toast.icon} />
        {menuOpen && <OverflowMenu
          tab={activeTab}
          bookmarked={bookmarkedUrls.indexOf(activeTab.url) > -1}
          onClose={function () { setMenuOpen(false); }}
          onAction={function (a) {
            setMenuOpen(false);
            if (a === 'tabs') setView('tabs');
            else if (a === 'bookmarks') setView('bookmarks');
            else if (a === 'history') setView('history');
            else if (a === 'downloads') setView('downloads');
            else if (a === 'settings') setView('settings');
            else if (a === 'desktop') toggleMode();
            else if (a === 'star') toggleBookmark();
            else if (a === 'find') flash('Find in page', 'search');
            else if (a === 'share') flash('Share link', 'share');
          }}
        />}
      </div>
    </React.Fragment>
  );
}

/* ---- overflow menu (the more_vert button) ---- */
function OverflowMenu(props) {
  const t = props.tab;
  const items = [
    { id: 'star', icon: t && props.bookmarked ? 'star' : 'star_outline', label: props.bookmarked ? 'Bookmarked' : 'Add bookmark' },
    { id: 'desktop', icon: t && t.mode === 'mobile' ? 'desktop_windows' : 'smartphone', label: t && t.mode === 'mobile' ? 'Desktop site' : 'Mobile site' },
    { id: 'find', icon: 'search', label: 'Find in page' },
    { id: 'share', icon: 'share', label: 'Share' },
    { sep: true },
    { id: 'bookmarks', icon: 'bookmark', label: 'Bookmarks' },
    { id: 'history', icon: 'history', label: 'History' },
    { id: 'downloads', icon: 'download', label: 'Downloads' },
    { id: 'settings', icon: 'settings', label: 'Settings' }
  ];
  useEffect(function () {
    const id = requestAnimationFrame(function () {
      const el = document.querySelector('.menu-sheet .focusable');
      if (el) el.focus();
    });
    return function () { cancelAnimationFrame(id); };
  }, []);
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 50 }}>
      <div style={{ position: 'absolute', inset: 0, background: 'var(--scrim)' }} onClick={props.onClose}></div>
      <div className="menu-sheet" style={{
        position: 'absolute', top: 100, right: 28, width: 340,
        background: 'var(--surface-2)', borderRadius: 'var(--r-lg)',
        border: '1px solid var(--outline-faint)', boxShadow: 'var(--e3)',
        padding: 10, display: 'flex', flexDirection: 'column', gap: 2
      }}>
        {items.map(function (it, i) {
          if (it.sep) return <div key={i} style={{ height: 1, background: 'var(--outline-faint)', margin: '8px 12px' }}></div>;
          return (
            <button key={i} className="focusable focus-soft"
              onClick={function () { props.onAction(it.id); }}
              style={{
                display: 'flex', alignItems: 'center', gap: 16, padding: '14px 16px',
                borderRadius: 'var(--r-md)', background: 'transparent', border: 'none',
                color: 'var(--on-surface)', fontSize: 17, fontFamily: 'inherit', textAlign: 'left'
              }}>
              <Icon name={it.icon} style={{ fontSize: 24, color: 'var(--on-surface-muted)' }} />
              {it.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}

window.App = App;
ReactDOM.createRoot(document.getElementById('app')).render(<App />);
if (window.__fitStage) window.__fitStage();
