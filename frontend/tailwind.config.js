/** @type {import('tailwindcss').Config} */

// Theme-aware palette: every color family the app uses reads its shades from CSS variables
// (src/theme.css) – the light values are Tailwind's defaults, `html.dark` swaps in the neon theme.
const SHADES = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950];
const themed = (family) =>
  Object.fromEntries(SHADES.map((shade) => [shade, `rgb(var(--c-${family}-${shade}) / <alpha-value>)`]));

export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        white: 'rgb(var(--c-white) / <alpha-value>)',
        gray: themed('gray'),
        blue: themed('blue'),
        red: themed('red'),
        green: themed('green'),
        amber: themed('amber'),
        yellow: themed('yellow'),
        // Rating stars: amber in the light theme, pink in neon (amber is "lent" there)
        star: 'rgb(var(--c-star) / <alpha-value>)',
        // Camera scanner: always black and white, whatever the theme
        scanner: {
          bg: '#000000',
          fg: '#ffffff',
          muted: '#e5e7eb',
          ink: '#111827',
        },
        primary: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#86efac',
          400: '#4ade80',
          500: '#22c55e',
          600: '#16a34a',
          700: '#15803d',
          800: '#166534',
          900: '#14532d',
        },
        profit: {
          light: '#86efac',
          DEFAULT: '#22c55e',
          dark: '#15803d',
        },
        loss: {
          light: '#fca5a5',
          DEFAULT: '#ef4444',
          dark: '#b91c1c',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
      },
    },
  },
  plugins: [],
}
