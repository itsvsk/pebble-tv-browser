/* ============================================================
   nav.jsx — Spatial D-pad navigation
   Real DOM focus on `.focusable` nodes. Arrow keys move focus
   to the nearest neighbour in that direction; Enter clicks the
   focused node; Backspace bubbles a "back" event.
   ============================================================ */
const { useEffect, useRef, useState, useCallback, useLayoutEffect } = React;

const SpatialNav = (function () {
  function focusables() {
    return Array.prototype.slice
      .call(document.querySelectorAll('.focusable'))
      .filter(function (el) {
        return el.offsetParent !== null && !el.hasAttribute('data-skip');
      });
  }
  function center(el) {
    const r = el.getBoundingClientRect();
    return { x: r.left + r.width / 2, y: r.top + r.height / 2, r: r };
  }
  function move(dir) {
    const cur = document.activeElement;
    const list = focusables();
    if (!list.length) return false;
    if (!cur || !cur.classList || !cur.classList.contains('focusable')) {
      list[0].focus();
      return true;
    }
    const c = center(cur);
    let best = null, bestScore = Infinity;
    for (let i = 0; i < list.length; i++) {
      const el = list[i];
      if (el === cur) continue;
      const t = center(el);
      const dx = t.x - c.x, dy = t.y - c.y;
      let valid = false, primary = 0, cross = 0;
      if (dir === 'left')  { valid = dx < -2; primary = -dx; cross = Math.abs(dy); }
      if (dir === 'right') { valid = dx > 2;  primary = dx;  cross = Math.abs(dy); }
      if (dir === 'up')    { valid = dy < -2; primary = -dy; cross = Math.abs(dx); }
      if (dir === 'down')  { valid = dy > 2;  primary = dy;  cross = Math.abs(dx); }
      if (!valid) continue;
      // strongly prefer alignment on the cross axis
      const score = primary + cross * 2.4;
      if (score < bestScore) { bestScore = score; best = el; }
    }
    if (best) {
      best.focus();
      best.scrollIntoView ? scrollWithin(best) : null;
      return true;
    }
    return false;
  }
  // keep focused element visible inside its nearest .view-scroll / .page-scroll
  function scrollWithin(el) {
    const scrollers = ['.view-scroll', '.page-scroll', '.tabs-scroll'];
    let p = el.parentElement;
    while (p) {
      if (p.scrollHeight > p.clientHeight + 4 &&
          /auto|scroll|hidden/.test(getComputedStyle(p).overflowY)) {
        const er = el.getBoundingClientRect();
        const pr = p.getBoundingClientRect();
        const pad = 40;
        if (er.top < pr.top + pad) p.scrollTop -= (pr.top + pad - er.top);
        else if (er.bottom > pr.bottom - pad) p.scrollTop += (er.bottom - (pr.bottom - pad));
        break;
      }
      p = p.parentElement;
    }
  }
  return { focusables, move, scrollWithin };
})();

// Focus the first focusable inside a container (by selector) after render.
function useAutoFocus(depKey) {
  useEffect(function () {
    const id = requestAnimationFrame(function () {
      const list = SpatialNav.focusables();
      // Prefer an element marked data-autofocus, else the first
      const pref = document.querySelector('.focusable[data-autofocus]');
      const target = (pref && pref.offsetParent !== null) ? pref : list[0];
      if (target) target.focus();
    });
    return function () { cancelAnimationFrame(id); };
  }, [depKey]);
}

// Global key handler. onBack fires on Backspace/Escape.
function useGlobalKeys(onBack) {
  useEffect(function () {
    function onKey(e) {
      const a = document.activeElement;
      const isInput = a && (a.tagName === 'INPUT' || a.tagName === 'TEXTAREA');
      const k = e.key;
      if (k === 'ArrowUp' || k === 'ArrowDown' || k === 'ArrowLeft' || k === 'ArrowRight') {
        // let text inputs use left/right for cursor
        if (isInput && (k === 'ArrowLeft' || k === 'ArrowRight')) return;
        e.preventDefault();
        const map = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right' };
        SpatialNav.move(map[k]);
      } else if (k === 'Enter') {
        if (a && a.classList && a.classList.contains('focusable') && a.tagName !== 'INPUT') {
          e.preventDefault();
          a.click();
        }
      } else if (k === 'Backspace' || k === 'Escape') {
        if (isInput && k === 'Backspace') return; // editing
        e.preventDefault();
        if (onBack) onBack();
      }
    }
    document.addEventListener('keydown', onKey);
    return function () { document.removeEventListener('keydown', onKey); };
  }, [onBack]);
}

Object.assign(window, { SpatialNav, useAutoFocus, useGlobalKeys,
  useEffect, useRef, useState, useCallback, useLayoutEffect });
