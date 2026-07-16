/* ============================================================
   views.jsx — all screens
   ============================================================ */

/* ----------------------------------------------------------------
   HOME / NEW TAB
   ---------------------------------------------------------------- */
function HomeClock() {
  const [now, setNow] = useState(new Date());
  useEffect(function () {
    const id = setInterval(function () { setNow(new Date()); }, 1000);
    return function () { clearInterval(id); };
  }, []);
  const h = now.getHours();
  const greet = h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : h < 21 ? 'Good evening' : 'Good night';
  const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
  function p(n) { return (n < 10 ? '0' : '') + n; }
  const time = p(h) + ':' + p(now.getMinutes());
  const date = days[now.getDay()] + ', ' + now.getDate() + ' ' + months[now.getMonth()];
  return (
    <div className="home-hero">
      <div className="hh-time">{time}</div>
      <div className="hh-greet">{greet}</div>
      <div className="hh-date">{date}</div>
    </div>
  );
}

function HomeView(props) {
  const [q, setQ] = useState('');
  function submit() {
    const v = q.trim();
    if (v) props.onNavigate(v.indexOf('.') > -1 && v.indexOf(' ') === -1 ? v : ('google.com/search?q=' + encodeURIComponent(v)),
      v.indexOf(' ') > -1 ? ('“' + v + '” — Google Search') : v);
  }
  return (
    <div className="home">
      <div className="home-brand">
        <div className="mark"></div>
        <div className="word">PEBB<b>L</b>E</div>
      </div>

      <HomeClock />

      <div className="searchbar focusable"
        data-autofocus="true"
        tabIndex={0}
        onClick={function (e) { const inp = e.currentTarget.querySelector('input'); if (inp) inp.focus(); }}>
        <Icon name="search" className="lead" />
        <input
          value={q}
          placeholder="Search Google or type a URL"
          onChange={function (e) { setQ(e.target.value); }}
          onKeyDown={function (e) { if (e.key === 'Enter') submit(); }}
        />
        <div className="mic"><Icon name="mic" /></div>
      </div>

      <div className="mv-wrap">
        <div className="mv-label">Most visited</div>
        <div className="mv-grid">
          {MOST_VISITED.map(function (s, i) {
            return (
              <button key={i} className="mv-tile focusable focus-lift"
                onClick={function () { props.onNavigate(s.url, s.name, s.fav); }}>
                <Fav fav={s.fav} />
                <span className="mv-name">{s.name}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   BROWSER CHROME + PAGE
   ---------------------------------------------------------------- */
function BrowserView(props) {
  const tab = props.tab;
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(tab.url);
  const inputRef = useRef(null);

  useEffect(function () { setDraft(tab.url); setEditing(false); }, [tab.id, tab.url]);
  useEffect(function () { if (editing && inputRef.current) inputRef.current.select(); }, [editing]);

  function commit() {
    const v = draft.trim();
    if (v) props.onNavigate(v.indexOf(' ') > -1 ? ('google.com/search?q=' + encodeURIComponent(v)) : v,
      v.indexOf(' ') > -1 ? ('“' + v + '” — Google Search') : v);
    setEditing(false);
  }

  const scheme = tab.secure ? 'https://' : 'http://';
  return (
    <div className="view" style={{ padding: 0 }}>
      <div className="chrome">
        <button className="icon-btn focusable" onClick={props.onBack}><Icon name="arrow_back" /></button>
        <button className="icon-btn focusable" data-disabled="true"><Icon name="arrow_forward" /></button>
        <button className="icon-btn focusable" onClick={props.onReload}><Icon name="refresh" /></button>

        <div className="address focusable" tabIndex={0}
          onClick={function () { setEditing(true); requestAnimationFrame(function(){ inputRef.current && inputRef.current.focus(); }); }}>
          <Icon name={tab.secure ? 'lock' : 'info'} className="lock" />
          {editing
            ? <input ref={inputRef} value={draft}
                onChange={function (e) { setDraft(e.target.value); }}
                onKeyDown={function (e) { if (e.key === 'Enter') commit(); if (e.key === 'Escape') { setEditing(false); setDraft(tab.url); } }}
                onBlur={function () { setEditing(false); setDraft(tab.url); }} />
            : <div className="url"><span className="scheme">{scheme}</span>{tab.url}</div>}
          <div className="modechip"><Icon name={tab.mode === 'mobile' ? 'smartphone' : 'desktop_windows'} style={{ fontSize: 17 }} />{tab.mode === 'mobile' ? 'MOBILE' : 'DESKTOP'}</div>
        </div>

        <button className="icon-btn focusable" onClick={props.onToggleMode} title="Toggle desktop / mobile">
          <Icon name={tab.mode === 'mobile' ? 'desktop_windows' : 'smartphone'} />
        </button>
        <button className={'icon-btn focusable' + (props.bookmarked ? ' fill' : '')} onClick={props.onBookmark}>
          <Icon name="star" fill={props.bookmarked} style={props.bookmarked ? { color: 'var(--primary-strong)' } : null} />
        </button>
        <button className="icon-btn tabcount focusable" onClick={function () { props.onNav('tabs'); }}>
          <Icon name="filter_none" style={{ fontSize: 24 }} />
          <span className="badge">{props.tabCount}</span>
        </button>
        <button className="icon-btn focusable" onClick={props.onMenu}><Icon name="more_vert" /></button>
      </div>

      <div className="page-area">
        <div className="page-scroll">
          <MockPage mode={tab.mode} />
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   TAB SWITCHER
   ---------------------------------------------------------------- */
function TabsView(props) {
  return (
    <div className="view">
      <div className="view-head">
        <div>
          <div className="view-title">Tabs</div>
          <div className="view-sub">{props.tabs.length} open</div>
        </div>
        <div className="spacer"></div>
        <button className="pill-btn tonal focusable" data-autofocus="true" onClick={props.onNew}>
          <Icon name="add" /> New tab
        </button>
        <button className="pill-btn focusable" onClick={props.onCloseAll}>
          <Icon name="close" /> Close all
        </button>
      </div>
      <div className="view-body">
        <div className="view-scroll" style={{ overflowY: 'auto' }}>
          <div className="tabs-grid">
            {props.tabs.map(function (t) {
              const active = t.id === props.activeId;
              return (
                <div key={t.id} className={'tab-card focusable focus-lift' + (active ? ' active' : '')}
                  tabIndex={0}
                  onClick={function () { props.onPick(t.id); }}>
                  <div className="t-head">
                    {t.kind === 'home'
                      ? <div className="fav" style={{ width: 34, height: 34, borderRadius: 9, background: 'var(--surface-3)', color: 'var(--primary-strong)', fontSize: 15 }}><Icon name="home" style={{ fontSize: 20 }} /></div>
                      : <Fav fav={t.fav} style={{ width: 34, height: 34, borderRadius: 9, fontSize: 15 }} />}
                    <span className="t-title">{t.title}</span>
                    <span className="t-close" onClick={function (e) { e.stopPropagation(); props.onClose(t.id); }}><Icon name="close" style={{ fontSize: 20 }} /></span>
                  </div>
                  <div className="t-thumb">
                    {t.kind === 'home'
                      ? <div style={{ position: 'absolute', inset: 0, display: 'grid', placeItems: 'center', color: 'var(--on-surface-faint)' }}><Icon name="search" style={{ fontSize: 36 }} /></div>
                      : <MiniWeb fav={t.fav} />}
                  </div>
                </div>
              );
            })}
            <div className="tab-card newt focusable focus-lift" tabIndex={0} onClick={props.onNew}>
              <Icon name="add" />
              <span>New tab</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   BOOKMARKS
   ---------------------------------------------------------------- */
function BookmarksView(props) {
  const folders = {};
  BOOKMARKS.forEach(function (b) { (folders[b.folder] = folders[b.folder] || []).push(b); });
  const order = Object.keys(folders);
  return (
    <div className="view">
      <div className="view-head">
        <div>
          <div className="view-title">Bookmarks</div>
          <div className="view-sub">{BOOKMARKS.length} saved across {order.length} folders</div>
        </div>
        <div className="spacer"></div>
        <div className="seg">
          <div className="opt on"><Icon name="grid_view" /> Grid</div>
          <div className="opt"><Icon name="list" /> List</div>
        </div>
      </div>
      <div className="view-body">
        <div className="view-scroll" style={{ overflowY: 'auto' }}>
          {order.map(function (folder, fi) {
            return (
              <div key={folder}>
                <div className="group-label"><Icon name="folder" style={{ fontSize: 16, verticalAlign: '-3px', marginRight: 6 }} />{folder}</div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
                  {folders[folder].map(function (b, i) {
                    return (
                      <button key={i} className="row focusable"
                        data-autofocus={fi === 0 && i === 0 ? 'true' : undefined}
                        style={{ textAlign: 'left' }}
                        onClick={function () { props.onOpen(b.url, b.title, b.fav); }}>
                        <Fav fav={b.fav} />
                        <div className="r-main">
                          <div className="r-title">{b.title}</div>
                          <div className="r-sub">{b.url}</div>
                        </div>
                        <span className="r-act"><Icon name="more_vert" /></span>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   HISTORY
   ---------------------------------------------------------------- */
function HistoryView(props) {
  return (
    <div className="view">
      <div className="view-head">
        <div>
          <div className="view-title">History</div>
          <div className="view-sub">Recent browsing on this device</div>
        </div>
        <div className="spacer"></div>
        <button className="pill-btn focusable" onClick={function () { props.onToast('History cleared', 'delete'); }}>
          <Icon name="delete" /> Clear browsing data
        </button>
      </div>
      <div className="view-body">
        <div className="view-scroll" style={{ overflowY: 'auto' }}>
          {HISTORY.map(function (g, gi) {
            return (
              <div key={g.group}>
                <div className="group-label">{g.group}</div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {g.items.map(function (it, i) {
                    return (
                      <button key={i} className="row focusable" style={{ textAlign: 'left' }}
                        data-autofocus={gi === 0 && i === 0 ? 'true' : undefined}
                        onClick={function () { props.onOpen(it.url, it.title, it.fav); }}>
                        <Fav fav={it.fav} />
                        <div className="r-main">
                          <div className="r-title">{it.title}</div>
                          <div className="r-sub">{it.url}</div>
                        </div>
                        <span className="r-time">{it.time}</span>
                        <span className="r-act"><Icon name="close" /></span>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   DOWNLOADS
   ---------------------------------------------------------------- */
function DownloadsView(props) {
  const active = DOWNLOADS.filter(function (d) { return d.status === 'active' || d.status === 'paused'; });
  const past = DOWNLOADS.filter(function (d) { return d.status === 'done' || d.status === 'failed'; });

  function statusColor(s) {
    return s === 'done' ? 'var(--good)' : s === 'failed' ? 'var(--danger)' : s === 'paused' ? 'var(--warn)' : 'var(--primary)';
  }

  function DLRow(d, key, af) {
    const activeKind = d.status === 'active' || d.status === 'paused';
    return (
      <button key={key} className="dl-card focusable" style={{ textAlign: 'left' }}
        data-autofocus={af ? 'true' : undefined}
        onClick={function () { props.onToast(d.status === 'done' ? 'Opening ' + d.name : d.name, d.status === 'done' ? 'open_in_new' : 'download'); }}>
        <div className="dl-ico" style={d.status === 'failed' ? { color: 'var(--danger)' } : null}><Icon name={d.icon} /></div>
        <div className="dl-main">
          <div className="dl-name">{d.name}</div>
          <div className="dl-meta">
            {d.status === 'active' && (d.got + ' of ' + d.size + ' · 2.4 MB/s · 14s left')}
            {d.status === 'paused' && ('Paused · ' + d.size)}
            {d.status === 'done' && (d.size + ' · ' + d.when)}
            {d.status === 'failed' && ('Failed · ' + d.when + ' · network error')}
          </div>
          {activeKind && <div className="dl-bar"><span style={{ width: d.pct + '%', background: statusColor(d.status) }}></span></div>}
        </div>
        <div className="dl-act">
          {d.status === 'active' && <><span className="dl-pct">{d.pct}%</span><span className="r-act"><Icon name="pause" /></span><span className="r-act"><Icon name="close" /></span></>}
          {d.status === 'paused' && <span className="r-act"><Icon name="play_arrow" /></span>}
          {d.status === 'done' && <><span className="r-act"><Icon name="folder_open" /></span><span className="r-act"><Icon name="more_vert" /></span></>}
          {d.status === 'failed' && <span className="r-act"><Icon name="refresh" /></span>}
        </div>
      </button>
    );
  }

  return (
    <div className="view">
      <div className="view-head">
        <div>
          <div className="view-title">Downloads</div>
          <div className="view-sub">{active.length} active · {past.length} completed</div>
        </div>
        <div className="spacer"></div>
        <button className="pill-btn focusable" onClick={function () { props.onToast('Opened download folder', 'folder_open'); }}>
          <Icon name="folder_open" /> Open folder
        </button>
      </div>
      <div className="view-body">
        <div className="view-scroll" style={{ overflowY: 'auto' }}>
          <div className="path-card focusable" tabIndex={0} data-autofocus="true"
            onClick={function () { props.onToast('Choose download location', 'edit'); }}>
            <Icon name="folder" />
            <div className="p-main">
              <div className="p-label">Default download location</div>
              <div className="p-val">{props.downloadPath}</div>
            </div>
            <span className="pill-btn" style={{ pointerEvents: 'none' }}><Icon name="edit" /> Change</span>
          </div>

          {active.length > 0 && <div className="group-label">In progress</div>}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {active.map(function (d, i) { return DLRow(d, 'a' + i, false); })}
          </div>

          <div className="group-label">Completed</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {past.map(function (d, i) { return DLRow(d, 'p' + i, false); })}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ----------------------------------------------------------------
   SETTINGS
   ---------------------------------------------------------------- */
function SettingsView(props) {
  const s = props.settings;
  function Toggle(on) { return <div className={'switch' + (on ? ' on' : '')}><div className="knob"></div></div>; }
  return (
    <div className="view">
      <div className="view-head" style={{ flexDirection: 'column', alignItems: 'center', textAlign: 'center', gap: 4 }}>
        <div className="view-title">Settings</div>
        <div className="view-sub">Pebble · version 1.0</div>
      </div>
      <div className="view-body">
        <div className="view-scroll" style={{ overflowY: 'auto' }}>
          <div className="set-col">
          <div className="group-label">Page rendering</div>
          <div className="set-list">
            <button className="set-row focusable" data-autofocus="true" style={{ textAlign: 'left' }}
              onClick={function () { props.onSet('defaultDesktop', !s.defaultDesktop); }}>
              <div className="s-ico"><Icon name="desktop_windows" /></div>
              <div className="s-main">
                <div className="s-title">Request desktop site by default</div>
                <div className="s-sub">New tabs open pages in {s.defaultDesktop ? 'desktop' : 'mobile'} layout</div>
              </div>
              {Toggle(s.defaultDesktop)}
            </button>
            <button className="set-row focusable" style={{ textAlign: 'left' }}
              onClick={function () { props.onSet('blockPopups', !s.blockPopups); }}>
              <div className="s-ico"><Icon name="block" /></div>
              <div className="s-main">
                <div className="s-title">Block pop-ups & redirects</div>
                <div className="s-sub">Recommended for a cleaner experience</div>
              </div>
              {Toggle(s.blockPopups)}
            </button>
          </div>

          <div className="group-label">Downloads</div>
          <div className="set-list">
            <button className="set-row focusable" style={{ textAlign: 'left' }}
              onClick={function () { props.onToast('Choose download location', 'edit'); }}>
              <div className="s-ico"><Icon name="folder" /></div>
              <div className="s-main">
                <div className="s-title">Download location</div>
                <div className="s-sub" style={{ fontFamily: 'Roboto Mono, monospace' }}>{props.downloadPath}</div>
              </div>
              <span className="pill-btn" style={{ pointerEvents: 'none' }}><Icon name="edit" /> Change</span>
            </button>
            <button className="set-row focusable" style={{ textAlign: 'left' }}
              onClick={function () { props.onSet('askWhere', !s.askWhere); }}>
              <div className="s-ico"><Icon name="help" /></div>
              <div className="s-main">
                <div className="s-title">Ask where to save each file</div>
                <div className="s-sub">Prompt for a location before every download</div>
              </div>
              {Toggle(s.askWhere)}
            </button>
          </div>

          <div className="group-label">Privacy</div>
          <div className="set-list">
            <button className="set-row focusable" style={{ textAlign: 'left' }}
              onClick={function () { props.onSet('dnt', !s.dnt); }}>
              <div className="s-ico"><Icon name="visibility_off" /></div>
              <div className="s-main">
                <div className="s-title">Send “Do Not Track” request</div>
                <div className="s-sub">Ask sites not to track your browsing</div>
              </div>
              {Toggle(s.dnt)}
            </button>
            <button className="set-row focusable" style={{ textAlign: 'left' }}
              onClick={function () { props.onToast('Browsing data cleared', 'delete'); }}>
              <div className="s-ico"><Icon name="delete" /></div>
              <div className="s-main">
                <div className="s-title">Clear browsing data</div>
                <div className="s-sub">History, cookies and cached files</div>
              </div>
              <span className="r-act"><Icon name="chevron_right" /></span>
            </button>
          </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { HomeView, BrowserView, TabsView, BookmarksView, HistoryView, DownloadsView, SettingsView });
