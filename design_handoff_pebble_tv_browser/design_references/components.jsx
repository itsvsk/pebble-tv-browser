/* ============================================================
   components.jsx — shared UI primitives
   ============================================================ */

function Icon(props) {
  return React.createElement('span', {
    className: 'material-symbols-outlined' + (props.fill ? ' fill' : '') + (props.className ? ' ' + props.className : ''),
    style: props.style
  }, props.name);
}

function Fav(props) {
  const f = props.fav;
  if (!f) {
    return <div className="fav" style={{ background: 'var(--surface-3)', color: 'var(--on-surface-faint)' }}>
      <Icon name="public" />
    </div>;
  }
  return <div className="fav" style={Object.assign({ background: f.bg, color: f.fg }, props.style)}>{f.ch}</div>;
}

/* ---------------- Navigation rail ---------------- */
function NavRail(props) {
  const items = [
    { id: 'home', icon: 'home', label: 'Home' },
    { id: 'tabs', icon: 'tab', label: 'Tabs' },
    { id: 'bookmarks', icon: 'bookmark', label: 'Saved' },
    { id: 'history', icon: 'history', label: 'History' },
    { id: 'downloads', icon: 'download', label: 'Files' }
  ];
  return (
    <div className="rail">
      <div className="rail-logo"><div className="dot"></div></div>
      {items.map(function (it) {
        const active = props.view === it.id ||
          (it.id === 'home' && props.view === 'browser');
        return (
          <button key={it.id}
            className={'rail-item focusable focus-soft' + (active ? ' active' : '')}
            onClick={function () { props.onNav(it.id); }}>
            {active ? <span className="pillbg"></span> : null}
            <Icon name={it.icon} fill={active} />
            <span className="rlabel">{it.label}</span>
          </button>
        );
      })}
      <div className="rail-spacer"></div>
      <button className={'rail-item focusable focus-soft' + (props.view === 'settings' ? ' active' : '')}
        onClick={function () { props.onNav('settings'); }}>
        {props.view === 'settings' ? <span className="pillbg"></span> : null}
        <Icon name="settings" fill={props.view === 'settings'} />
        <span className="rlabel">Settings</span>
      </button>
    </div>
  );
}

/* ---------------- Key hints ---------------- */
function KeyHints(props) {
  const hints = props.hints || [
    { k: '\u25B2\u25BC\u25C0\u25B6', label: 'Navigate' },
    { k: 'OK', label: 'Select' },
    { k: 'Back', label: 'Return' }
  ];
  return (
    <div className="keyhints">
      {hints.map(function (h, i) {
        return <div className="kh" key={i}><span className="key">{h.k}</span><span>{h.label}</span></div>;
      })}
    </div>
  );
}

/* ---------------- Toast ---------------- */
function Toast(props) {
  if (!props.msg) return null;
  return <div className="toast"><Icon name={props.icon || 'check'} style={{ fontSize: 20, verticalAlign: '-4px', marginRight: 8 }} />{props.msg}</div>;
}

/* ============================================================
   Mock rendered web page (sells desktop / mobile modes)
   ============================================================ */
function MockPage(props) {
  const mobile = props.mode === 'mobile';
  const imgH = mobile ? 200 : 360;
  return (
    <div className={'page-canvas mock ' + (mobile ? 'mobile' : 'desktop')}>
      <div className="m-nav">
        <div className="m-brand">NASA</div>
        {!mobile && <>
          <div className="m-link">Missions</div>
          <div className="m-link">Universe</div>
          <div className="m-link">Earth</div>
          <div className="m-link">Multimedia</div>
        </>}
        <div style={{ flex: 1 }}></div>
        <Icon name={mobile ? 'menu' : 'search'} style={{ color: '#5f6368' }} />
      </div>
      <div className="m-hero">
        <div className="m-kicker">Solar System · Voyager Program</div>
        <div className="m-h1" style={mobile ? { fontSize: 30 } : null}>The Pale Blue Dot: a mote of dust suspended in a sunbeam</div>
        <div className="m-meta">By NASA Science Editorial Team · Updated 14 May 2026 · 6 min read</div>
      </div>
      <div style={{ padding: mobile ? '0 24px 24px' : '0 40px 32px' }}>
        <div className="m-img" style={{ height: imgH }}>[ photograph — Earth from 6 billion km ]</div>
      </div>
      <div className="m-body" style={mobile ? { padding: '8px 24px 40px' } : null}>
        <p>On 14 February 1990, the Voyager 1 spacecraft turned its camera back toward home and captured the Earth as a single, almost invisible point of light against the vastness of space.</p>
        <p>From a distance of about six billion kilometres, our entire world occupied less than a single pixel — a finding that reframed how a generation thought about scale, fragility, and our place among the planets.</p>
        {!mobile && <p>The image, and the reflection it inspired, remains one of the most widely reproduced photographs in the history of spaceflight, and a touchstone for conversations about stewardship of the planet.</p>}
      </div>
      <div className="m-grid" style={{ gridTemplateColumns: mobile ? '1fr' : '1fr 1fr 1fr', padding: mobile ? '0 24px 40px' : '0 40px 56px' }}>
        {[['Missions', 'Voyager 1 & 2 — the grand tour of the outer planets'],
          ['Universe', 'How distance and time shape what telescopes can see'],
          ['Earth', 'Monitoring a changing climate from orbit']].map(function (c, i) {
          return (
            <div className="m-card" key={i}>
              <div className="m-img" style={{ height: 120, borderRadius: 0 }}>[ image ]</div>
              <div className="c-body"><h3>{c[0]}</h3><p>{c[1]}</p></div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* tiny abstract page used in tab thumbnails */
function MiniWeb(props) {
  const f = props.fav;
  return (
    <div className="miniweb" style={{ background: '#f5f6f7', padding: '10px 12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
        <div style={{ width: 14, height: 14, borderRadius: 4, background: f ? f.bg : '#dadce0' }}></div>
        <div style={{ height: 8, width: '40%', borderRadius: 4, background: '#dadce0' }}></div>
      </div>
      <div style={{ height: 46, borderRadius: 6, background: 'repeating-linear-gradient(135deg,#e3e6ea 0 8px,#eceef1 8px 16px)', marginBottom: 10 }}></div>
      {[80, 95, 70].map(function (w, i) {
        return <div key={i} style={{ height: 6, width: w + '%', borderRadius: 3, background: '#e2e4e8', marginBottom: 7 }}></div>;
      })}
    </div>
  );
}

Object.assign(window, { Icon, Fav, NavRail, KeyHints, Toast, MockPage, MiniWeb });
