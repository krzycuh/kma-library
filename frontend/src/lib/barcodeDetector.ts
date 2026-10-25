/**
 * EAN-13 detector: the browser's own BarcodeDetector where it exists (Chrome on Android),
 * otherwise the ZXing (WebAssembly) ponyfill – loaded only when the scanner is first opened.
 * The ponyfill fetches its .wasm from a CDN by default; we serve it ourselves (CSP: connect-src 'self').
 */
export interface DetectedCode {
  rawValue: string;
}

export interface CodeDetector {
  detect(source: HTMLVideoElement | ImageBitmap): Promise<DetectedCode[]>;
}

interface NativeDetectorClass {
  new (options: { formats: string[] }): CodeDetector;
  getSupportedFormats(): Promise<string[]>;
}

const FORMATS = ['ean_13'];

let detectorPromise: Promise<CodeDetector> | null = null;

async function nativeDetector(): Promise<CodeDetector | null> {
  const Native = (globalThis as { BarcodeDetector?: NativeDetectorClass }).BarcodeDetector;
  if (!Native) return null;
  try {
    const supported = await Native.getSupportedFormats();
    return supported.includes('ean_13') ? new Native({ formats: FORMATS }) : null;
  } catch {
    return null;
  }
}

async function zxingDetector(): Promise<CodeDetector> {
  const [{ BarcodeDetector, prepareZXingModule }, { default: wasmUrl }] = await Promise.all([
    import('barcode-detector/ponyfill'),
    import('zxing-wasm/reader/zxing_reader.wasm?url'),
  ]);
  prepareZXingModule({
    overrides: {
      locateFile: (path: string, prefix: string) => (path.endsWith('.wasm') ? wasmUrl : prefix + path),
    },
  });
  return new BarcodeDetector({ formats: ['ean_13'] });
}

export function getCodeDetector(): Promise<CodeDetector> {
  detectorPromise ??= (async () => (await nativeDetector()) ?? zxingDetector())().catch((error) => {
    detectorPromise = null; // allow a retry, e.g. after a network hiccup while loading the wasm
    throw error;
  });
  return detectorPromise;
}
