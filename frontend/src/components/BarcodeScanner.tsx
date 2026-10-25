import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { getCodeDetector, type CodeDetector } from '../lib/barcodeDetector';
import { isbnFromBarcode } from '../utils/barcode';
import { t } from '../i18n';

interface BarcodeScannerProps {
  open: boolean;
  onClose: () => void;
  /** Called once with a valid ISBN-13; the camera is already stopped. */
  onDetected: (isbn: string) => void;
}

type ScannerError = 'insecure' | 'denied' | 'noCamera' | 'busy' | 'generic';

const errorMessages: Record<ScannerError, string> = {
  insecure: t.scanner.errorInsecure,
  denied: t.scanner.errorDenied,
  noCamera: t.scanner.errorNoCamera,
  busy: t.scanner.errorBusy,
  generic: t.scanner.errorGeneric,
};

const SCAN_INTERVAL_MS = 150;
const NOT_ISBN_HINT_MS = 2500;

function classifyCameraError(error: unknown): ScannerError {
  const name = (error as { name?: string })?.name;
  if (name === 'NotAllowedError' || name === 'SecurityError') return 'denied';
  if (name === 'NotFoundError' || name === 'OverconstrainedError') return 'noCamera';
  if (name === 'NotReadableError' || name === 'AbortError') return 'busy';
  return 'generic';
}

// Torch and continuous focus are not in the standard TS DOM types yet
type ExtendedCapabilities = MediaTrackCapabilities & { torch?: boolean; focusMode?: string[] };

/**
 * Full-screen camera preview that reads a book's EAN-13 (ISBN) barcode.
 * Falls back to taking a photo of the code when the live camera is not available.
 */
export function BarcodeScanner({ open, onClose, onDetected }: BarcodeScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const trackRef = useRef<MediaStreamTrack | null>(null);
  const onDetectedRef = useRef(onDetected);
  const [attempt, setAttempt] = useState(0);
  const [status, setStatus] = useState<'starting' | 'scanning' | 'error'>('starting');
  const [error, setError] = useState<ScannerError | null>(null);
  const [hint, setHint] = useState<string | null>(null);
  const [torchAvailable, setTorchAvailable] = useState(false);
  const [torchOn, setTorchOn] = useState(false);
  const [photoBusy, setPhotoBusy] = useState(false);

  useEffect(() => {
    onDetectedRef.current = onDetected;
  }, [onDetected]);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    let stream: MediaStream | null = null;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let hintTimer: ReturnType<typeof setTimeout> | undefined;
    let attachedVideo: HTMLVideoElement | null = null;

    const stopCamera = () => {
      stream?.getTracks().forEach((track) => track.stop());
      stream = null;
      trackRef.current = null;
    };

    const fail = (kind: ScannerError) => {
      stopCamera();
      if (cancelled) return;
      setError(kind);
      setStatus('error');
    };

    setStatus('starting');
    setError(null);
    setHint(null);
    setTorchAvailable(false);
    setTorchOn(false);

    (async () => {
      if (!window.isSecureContext) return fail('insecure');
      if (!navigator.mediaDevices?.getUserMedia) return fail('noCamera');

      // Load the decoder while the camera starts
      const detectorPromise = getCodeDetector();
      let detector: CodeDetector;
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
        });
      } catch (err) {
        detectorPromise.catch(() => undefined);
        return fail(classifyCameraError(err));
      }
      if (cancelled) return stopCamera();
      try {
        detector = await detectorPromise;
      } catch {
        return fail('generic');
      }
      if (cancelled) return stopCamera();

      const track = stream.getVideoTracks()[0];
      trackRef.current = track ?? null;
      const capabilities = (track?.getCapabilities?.() ?? {}) as ExtendedCapabilities;
      if (capabilities.focusMode?.includes('continuous')) {
        track.applyConstraints({ advanced: [{ focusMode: 'continuous' } as MediaTrackConstraintSet] }).catch(() => undefined);
      }
      setTorchAvailable(capabilities.torch === true);

      const video = videoRef.current;
      if (!video) return fail('generic');
      video.srcObject = stream;
      attachedVideo = video;
      try {
        await video.play();
      } catch {
        return fail('generic');
      }
      if (cancelled) return stopCamera();
      setStatus('scanning');

      const scan = async () => {
        if (cancelled) return;
        if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
          try {
            const codes = await detector.detect(video);
            if (cancelled) return;
            const isbn = codes.map((code) => isbnFromBarcode(code.rawValue)).find((value) => value !== null);
            if (isbn) {
              navigator.vibrate?.(80);
              stopCamera();
              onDetectedRef.current(isbn);
              return;
            }
            if (codes.length > 0) {
              setHint(t.scanner.notIsbn);
              clearTimeout(hintTimer);
              hintTimer = setTimeout(() => setHint(null), NOT_ISBN_HINT_MS);
            }
          } catch {
            // A single frame failing to decode is normal; keep scanning
          }
        }
        timer = setTimeout(scan, SCAN_INTERVAL_MS);
      };
      scan();
    })();

    return () => {
      cancelled = true;
      clearTimeout(timer);
      clearTimeout(hintTimer);
      stopCamera();
      if (attachedVideo) attachedVideo.srcObject = null;
    };
  }, [open, attempt]);

  const toggleTorch = async () => {
    const track = trackRef.current;
    if (!track) return;
    try {
      await track.applyConstraints({ advanced: [{ torch: !torchOn } as MediaTrackConstraintSet] });
      setTorchOn(!torchOn);
    } catch {
      setTorchAvailable(false);
    }
  };

  const onPhoto = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setPhotoBusy(true);
    setHint(null);
    try {
      const [detector, bitmap] = await Promise.all([getCodeDetector(), createImageBitmap(file)]);
      const codes = await detector.detect(bitmap);
      bitmap.close();
      const isbn = codes.map((code) => isbnFromBarcode(code.rawValue)).find((value) => value !== null);
      if (isbn) {
        trackRef.current?.stop();
        onDetectedRef.current(isbn);
      } else {
        setHint(t.scanner.photoFailed);
      }
    } catch {
      setHint(t.scanner.photoFailed);
    } finally {
      setPhotoBusy(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} className="relative z-50">
      <DialogPanel className="fixed inset-0 flex flex-col bg-scanner-bg text-scanner-fg">
        <div className="flex items-center justify-between px-4 py-3">
          <DialogTitle className="text-base font-semibold">{t.scanner.title}</DialogTitle>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md px-3 py-2 text-sm font-medium text-scanner-fg hover:bg-scanner-fg/10 focus:outline-none focus:ring-2 focus:ring-scanner-fg"
          >
            {t.scanner.close}
          </button>
        </div>

        <div className="relative flex-1 overflow-hidden">
          <video
            ref={videoRef}
            className={`h-full w-full object-cover ${status === 'scanning' ? '' : 'invisible'}`}
            playsInline
            muted
            aria-hidden="true"
          />
          {status === 'scanning' && (
            <div className="pointer-events-none absolute inset-0 flex items-center justify-center" aria-hidden="true">
              <div className="h-32 w-4/5 max-w-sm rounded-lg border-4 border-scanner-fg/90 shadow-[0_0_0_9999px_rgba(0,0,0,0.45)]" />
            </div>
          )}
          {status === 'starting' && (
            <p className="absolute inset-0 flex items-center justify-center px-6 text-center text-sm text-scanner-muted">
              {t.scanner.starting}
            </p>
          )}
          {status === 'error' && error && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 px-6 text-center" role="alert">
              <p className="text-sm text-scanner-muted">{errorMessages[error]}</p>
              {error !== 'insecure' && (
                <button
                  type="button"
                  onClick={() => setAttempt((n) => n + 1)}
                  className="rounded-lg border border-scanner-fg/70 px-4 py-2 text-sm font-semibold text-scanner-fg hover:bg-scanner-fg/10 focus:outline-none focus:ring-2 focus:ring-scanner-fg"
                >
                  {t.scanner.retry}
                </button>
              )}
            </div>
          )}
        </div>

        <div className="space-y-3 px-4 pb-6 pt-3">
          <p className="min-h-[2.5rem] text-center text-sm text-scanner-muted" aria-live="polite">
            {photoBusy ? t.scanner.readingPhoto : hint ?? (status === 'scanning' ? t.scanner.hint : '')}
          </p>
          <div className="flex gap-2">
            {torchAvailable && (
              <button
                type="button"
                onClick={toggleTorch}
                aria-pressed={torchOn}
                className="flex-1 rounded-lg border border-scanner-fg/70 px-4 py-3 text-sm font-semibold text-scanner-fg hover:bg-scanner-fg/10 focus:outline-none focus:ring-2 focus:ring-scanner-fg"
              >
                {torchOn ? t.scanner.torchOff : t.scanner.torchOn}
              </button>
            )}
            <label className="flex flex-1 cursor-pointer items-center justify-center rounded-lg bg-scanner-fg px-4 py-3 text-sm font-semibold text-scanner-ink hover:bg-scanner-muted focus-within:ring-2 focus-within:ring-scanner-fg">
              {t.scanner.takePhoto}
              <input type="file" accept="image/*" capture="environment" className="sr-only" onChange={onPhoto} disabled={photoBusy} />
            </label>
          </div>
        </div>
      </DialogPanel>
    </Dialog>
  );
}
