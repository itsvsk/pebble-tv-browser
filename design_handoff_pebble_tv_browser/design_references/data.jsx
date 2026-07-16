/* ============================================================
   data.jsx — seed content for the prototype
   ============================================================ */

// favicon palette — low-chroma, niche, on dark
const FAVS = {
  wikipedia: { bg: '#dfe3e8', fg: '#1a1c20', ch: 'W' },
  youtube:   { bg: '#c98b8b', fg: '#1a0c0c', ch: '\u25B6' },
  github:    { bg: '#c3c8d0', fg: '#14161b', ch: 'GH' },
  arxiv:     { bg: '#b9a07f', fg: '#1d1709', ch: 'X' },
  nasa:      { bg: '#8aa9cf', fg: '#0c1722', ch: 'N' },
  bbc:       { bg: '#d6d2cb', fg: '#1a1c20', ch: 'B' },
  reddit:    { bg: '#cda07f', fg: '#1d1207', ch: 'r' },
  hn:        { bg: '#cbb08a', fg: '#1d1607', ch: 'Y' },
  maps:      { bg: '#84b59a', fg: '#07140e', ch: '\u25C9' },
  weather:   { bg: '#9fb6cf', fg: '#0c1722', ch: '\u2600' },
  music:     { bg: '#b69fcf', fg: '#150c22', ch: '\u266B' },
  archive:   { bg: '#c8c1b4', fg: '#1a1710', ch: 'A' }
};

const MOST_VISITED = [
  { name: 'Wikipedia',     url: 'wikipedia.org',         fav: FAVS.wikipedia },
  { name: 'YouTube',       url: 'youtube.com',           fav: FAVS.youtube },
  { name: 'GitHub',        url: 'github.com',            fav: FAVS.github },
  { name: 'arXiv',         url: 'arxiv.org',             fav: FAVS.arxiv },
  { name: 'NASA',          url: 'nasa.gov',              fav: FAVS.nasa },
  { name: 'BBC',           url: 'bbc.com',               fav: FAVS.bbc },
  { name: 'Hacker News',   url: 'news.ycombinator.com',  fav: FAVS.hn },
  { name: 'Maps',          url: 'maps.google.com',       fav: FAVS.maps },
  { name: 'Reddit',        url: 'reddit.com',            fav: FAVS.reddit },
  { name: 'Weather',       url: 'weather.com',           fav: FAVS.weather },
  { name: 'Archive',       url: 'archive.org',           fav: FAVS.archive },
  { name: 'Music',         url: 'music.youtube.com',     fav: FAVS.music }
];

const BOOKMARKS = [
  { title: 'Wikipedia — The Free Encyclopedia', url: 'wikipedia.org', fav: FAVS.wikipedia, folder: 'Bookmarks bar' },
  { title: 'NASA Image and Video Library',      url: 'images.nasa.gov', fav: FAVS.nasa, folder: 'Bookmarks bar' },
  { title: 'Hacker News',                       url: 'news.ycombinator.com', fav: FAVS.hn, folder: 'Bookmarks bar' },
  { title: 'GitHub · Build software better',    url: 'github.com', fav: FAVS.github, folder: 'Reading' },
  { title: 'arXiv e-Print archive',             url: 'arxiv.org', fav: FAVS.arxiv, folder: 'Reading' },
  { title: 'BBC — Homepage',                    url: 'bbc.com', fav: FAVS.bbc, folder: 'Reading' },
  { title: 'Internet Archive',                  url: 'archive.org', fav: FAVS.archive, folder: 'Reading' },
  { title: 'YouTube',                           url: 'youtube.com', fav: FAVS.youtube, folder: 'Watch later' }
];

const HISTORY = [
  { group: 'Today', items: [
    { title: 'Voyager 1 — Wikipedia',                 url: 'en.wikipedia.org/wiki/Voyager_1', fav: FAVS.wikipedia, time: '14:22' },
    { title: 'The Pale Blue Dot — NASA',              url: 'nasa.gov/pale-blue-dot', fav: FAVS.nasa, time: '14:05' },
    { title: 'Show HN: A tiny spatial-nav library',   url: 'news.ycombinator.com/item', fav: FAVS.hn, time: '13:48' },
    { title: 'webkit/WebKit — main',                  url: 'github.com/WebKit/WebKit', fav: FAVS.github, time: '11:30' }
  ]},
  { group: 'Yesterday', items: [
    { title: 'Attention Is All You Need',             url: 'arxiv.org/abs/1706.03762', fav: FAVS.arxiv, time: '21:14' },
    { title: 'BBC — Science & Environment',           url: 'bbc.com/science', fav: FAVS.bbc, time: '20:40' },
    { title: 'Weather — 7-day forecast',              url: 'weather.com', fav: FAVS.weather, time: '08:12' }
  ]},
  { group: 'Mon, 25 May', items: [
    { title: 'Internet Archive: Digital Library',     url: 'archive.org', fav: FAVS.archive, time: '19:03' },
    { title: 'r/space — top of all time',             url: 'reddit.com/r/space', fav: FAVS.reddit, time: '18:20' }
  ]}
];

const DOWNLOADS = [
  { name: 'pale-blue-dot-4k.jpg',         size: '8.4 MB',   got: '5.1 MB',  pct: 61, status: 'active', kind: 'image', icon: 'image' },
  { name: 'voyager-mission-report.pdf',   size: '24.2 MB',  got: '6.7 MB',  pct: 28, status: 'active', kind: 'pdf',   icon: 'picture_as_pdf' },
  { name: 'webkit-source.zip',            size: '142 MB',   got: '142 MB',  pct: 100,status: 'paused', kind: 'archive', icon: 'folder_zip' },
  { name: 'lecture-03-transformers.mp4',  size: '512 MB',   got: '512 MB',  pct: 100,status: 'done',   kind: 'video', icon: 'movie',  when: 'Today, 13:51' },
  { name: 'attention-is-all-you-need.pdf',size: '2.1 MB',   got: '2.1 MB',  pct: 100,status: 'done',   kind: 'pdf',   icon: 'picture_as_pdf', when: 'Today, 11:02' },
  { name: 'nasa-wallpaper-pack.zip',      size: '88 MB',    got: '88 MB',   pct: 100,status: 'done',   kind: 'archive', icon: 'folder_zip', when: 'Yesterday, 20:44' },
  { name: 'firmware-update.bin',          size: '36 MB',    got: '12 MB',   pct: 33, status: 'failed', kind: 'file',  icon: 'error', when: 'Yesterday' }
];

const INITIAL_TABS = [
  { id: 't1', kind: 'page', title: 'The Pale Blue Dot — NASA', url: 'nasa.gov/pale-blue-dot', fav: FAVS.nasa, mode: 'desktop', secure: true },
  { id: 't2', kind: 'page', title: 'Voyager 1 — Wikipedia', url: 'en.wikipedia.org/wiki/Voyager_1', fav: FAVS.wikipedia, mode: 'desktop', secure: true },
  { id: 't3', kind: 'home',  title: 'New tab', url: '', fav: null, mode: 'desktop', secure: true }
];

Object.assign(window, { FAVS, MOST_VISITED, BOOKMARKS, HISTORY, DOWNLOADS, INITIAL_TABS });
