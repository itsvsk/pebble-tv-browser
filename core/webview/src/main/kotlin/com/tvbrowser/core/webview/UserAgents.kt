package com.tvbrowser.core.webview

object UserAgents {
    const val DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val VIEWPORT_DESKTOP_JS = """
        (function() {
          var m = document.querySelector('meta[name=viewport]');
          if (!m) { m = document.createElement('meta'); m.name = 'viewport'; document.head.appendChild(m); }
          m.content = 'width=1280';
        })();
    """
}
