// Renders the app icon (three neon books on a dark tile) into every file the PWA needs.
// Run `npm run icons` after changing the artwork below; the PNGs in public/ are committed.
//
// - favicon.svg                     rounded tile, scalable (browser tab)
// - icon-192.png / icon-512.png     rounded tile with transparent corners ("any": desktop shortcuts, install prompts)
// - icon-maskable-*.png             full-bleed tile, books inside the safe zone ("maskable": Android home screen,
//                                   which cuts its own circle/squircle — without this Chrome pads the icon on white)
// - apple-touch-icon.png            full-bleed 180px; iOS rounds the corners itself
import { chromium } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const outDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../public');

/**
 * @param {{ rounded: boolean, scale: number }} options
 *   rounded – corner radius on the tile (false = full-bleed square for masked icons)
 *   scale   – size of the books relative to the tile; maskable icons keep them inside the central 80% circle
 */
function iconSvg({ rounded, scale }) {
  const radius = rounded ? 14 : 0;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64" role="img" aria-label="KMA Library">
  <defs>
    <radialGradient id="bg" cx="50%" cy="42%" r="70%">
      <stop offset="0" stop-color="#241b3d"/>
      <stop offset="0.55" stop-color="#14111f"/>
      <stop offset="1" stop-color="#0b0a12"/>
    </radialGradient>
    <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
      <feGaussianBlur in="SourceGraphic" stdDeviation="1.2" result="inner"/>
      <feGaussianBlur in="SourceGraphic" stdDeviation="3.5" result="outer"/>
      <feMerge>
        <feMergeNode in="outer"/><feMergeNode in="outer"/><feMergeNode in="inner"/><feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>
  <rect width="64" height="64" rx="${radius}" fill="url(#bg)"/>${
    rounded
      ? `
  <rect x="1" y="1" width="62" height="62" rx="${radius - 1}" fill="none" stroke="#e879f9" stroke-opacity="0.3" stroke-width="1"/>`
      : ''
  }
  <g transform="translate(32 32) scale(${scale}) translate(-32 -32)">
    <g filter="url(#glow)">
      <rect x="11" y="14" width="11" height="36" rx="2" fill="#e879f9"/>
      <rect x="26" y="10" width="11" height="40" rx="2" fill="#22d3ee"/>
      <rect x="41" y="16" width="11" height="34" rx="2" fill="#4ade80" transform="rotate(8 46.5 33)"/>
    </g>
    <rect x="13.5" y="19" width="6" height="2.5" fill="#160b1f" opacity="0.6"/>
    <rect x="28.5" y="15" width="6" height="2.5" fill="#160b1f" opacity="0.6"/>
  </g>
</svg>
`;
}

const outputs = [
  { file: 'favicon.svg', rounded: true, scale: 1 },
  { file: 'icon-192.png', size: 192, rounded: true, scale: 1 },
  { file: 'icon-512.png', size: 512, rounded: true, scale: 1 },
  { file: 'icon-maskable-192.png', size: 192, rounded: false, scale: 0.78 },
  { file: 'icon-maskable-512.png', size: 512, rounded: false, scale: 0.78 },
  { file: 'apple-touch-icon.png', size: 180, rounded: false, scale: 0.9 },
];

await mkdir(outDir, { recursive: true });
// CHROMIUM_PATH lets an environment with a preinstalled browser skip `playwright install`
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
try {
  for (const output of outputs) {
    const svg = iconSvg(output);
    const target = path.join(outDir, output.file);
    if (!output.size) {
      await writeFile(target, svg);
    } else {
      const page = await browser.newPage({ viewport: { width: output.size, height: output.size }, deviceScaleFactor: 1 });
      await page.setContent(
        `<!doctype html><html><body style="margin:0;background:transparent">${svg.replace(
          'width="64" height="64"',
          `width="${output.size}" height="${output.size}"`,
        )}</body></html>`,
      );
      await page.screenshot({ path: target, omitBackground: true, type: 'png' });
      await page.close();
    }
    console.log(`wrote ${path.relative(process.cwd(), target)}`);
  }
} finally {
  await browser.close();
}
