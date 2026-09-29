import { Capacitor } from '@capacitor/core';

/**
 * Initializes and monitors safe area requirements for Android and mobile devices.
 * Ensures the top navbar on full screen pages is never obscured by the user's phone
 * status bar (time, battery, network line).
 */
export function initSafeArea(): void {
  if (typeof window === 'undefined') return;

  const applyStatusHeadroom = () => {
    const isCapacitor = Capacitor.isNativePlatform();
    const platform = Capacitor.getPlatform();
    const isAndroid = platform === 'android' || /Android/i.test(navigator.userAgent);
    const isStandalone =
      window.matchMedia('(display-mode: standalone)').matches ||
      window.matchMedia('(display-mode: fullscreen)').matches ||
      Boolean((navigator as any).standalone);

    if (isCapacitor || (isAndroid && isStandalone)) {
      document.documentElement.classList.add('is-android-app');

      // Check if CSS var --android-status-bar-gap is already provided by native bridge
      const currentGap = getComputedStyle(document.documentElement)
        .getPropertyValue('--android-status-bar-gap')
        .trim();

      if (!currentGap || currentGap === '0px' || currentGap === '0') {
        // Fallback default status bar height for Android devices (usually 28px - 36px)
        document.documentElement.style.setProperty('--android-status-bar-gap', '32px');
      }
    }
  };

  applyStatusHeadroom();

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', applyStatusHeadroom);
  }
  window.addEventListener('resize', applyStatusHeadroom);
}
