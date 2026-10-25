package com.tvbrowser.core.webview

import java.net.URI

/** Keeps D-pad traversal through Google results from opening query suggestions. */
internal object GoogleResultsFocusGuard {
    private val googleHost = Regex("^(?:[a-z0-9-]+\\.)?google\\.(?:com|co\\.[a-z]{2}|[a-z]{2,3})$")

    fun appliesTo(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme in listOf("http", "https") &&
            googleHost.matches(uri.host.orEmpty().lowercase()) &&
            uri.path == "/search"
    }.getOrDefault(false)

    // A clicked query remains editable. Directional focus skips it and cannot trigger Google's
    // suggestions while the user is navigating result links with the remote.
    const val script = """
        (() => {
          if (window.__pebbleGoogleQueryGuard) return;
          window.__pebbleGoogleQueryGuard = true;
          const selector = 'textarea[name="q"], input[name="q"]';
          let editingByClick = false;
          const isQuery = target => target instanceof Element && !!target.closest(selector);
          document.addEventListener('pointerdown', event => {
            editingByClick = isQuery(event.target);
          }, true);
          document.addEventListener('focusin', event => {
            if (isQuery(event.target) && !editingByClick) event.target.blur();
          }, true);
          document.addEventListener('focusout', event => {
            if (isQuery(event.target)) editingByClick = false;
          }, true);
          const protect = () => {
            document.querySelectorAll(selector).forEach(field => {
              if (field.getAttribute('tabindex') !== '-1') field.setAttribute('tabindex', '-1');
            });
          };
          protect();
          new MutationObserver(protect).observe(document.documentElement, {
            childList: true, subtree: true, attributes: true, attributeFilter: ['tabindex']
          });
        })();
    """
}
