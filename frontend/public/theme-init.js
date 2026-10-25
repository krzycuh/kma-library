// Applies the saved color theme before the app renders, so a dark-theme user never sees a white flash.
// A plain file (not inline) because the CSP allows only same-origin scripts. Mirrors src/theme/theme.ts.
(function () {
  var preference = 'system';
  try {
    var stored = localStorage.getItem('theme');
    if (stored === 'light' || stored === 'dark') preference = stored;
  } catch (e) {
    // Storage blocked: follow the system
  }
  var dark =
    preference === 'dark' ||
    (preference === 'system' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  if (dark) document.documentElement.classList.add('dark');
  var meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute('content', dark ? '#12101c' : '#1d4ed8');
})();
