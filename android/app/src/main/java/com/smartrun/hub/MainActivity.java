package com.smartrun.hub;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    public static final String HIGH_PRIORITY_CHANNEL_ID = "smartrun_order_alerts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Configure WebView to allow immediate audio playback without requiring gesture
        try {
            if (this.bridge != null && this.bridge.getWebView() != null) {
                WebView webView = this.bridge.getWebView();
                WebSettings settings = webView.getSettings();
                settings.setMediaPlaybackRequiresUserGesture(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Ensure Android status bar (time, battery, network line) does not obscure top navbar
        try {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            int statusBarHeightPx = resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
            float density = getResources().getDisplayMetrics().density;
            final int statusBarHeightDp = density > 0 ? Math.round(statusBarHeightPx / density) : 32;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Ensure dark status bar icons on light background so phone time, battery, network line are crisp & visible
                if (getWindow().getInsetsController() != null) {
                    getWindow().getInsetsController().setSystemBarsAppearance(
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.view.View decor = getWindow().getDecorView();
                decor.setSystemUiVisibility(decor.getSystemUiVisibility() | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            if (this.bridge != null && this.bridge.getWebView() != null) {
                this.bridge.getWebView().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String js = "document.documentElement.style.setProperty('--android-status-bar-gap', '" + statusBarHeightDp + "px');" +
                                        "document.documentElement.classList.add('is-android-app');";
                            bridge.getWebView().evaluateJavascript(js, null);
                        } catch (Exception ignored) {}
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Create high priority notification channel with our app's custom loud sound file
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SmartRun Order Alerts";
            String description = "High-priority audible alerts and popup notifications for incoming warehouse orders";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Delete old channel if exists to ensure newly set custom sound takes effect
                try {
                    notificationManager.deleteNotificationChannel(HIGH_PRIORITY_CHANNEL_ID);
                } catch (Exception ignored) {}

                Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.smartrun_order_alert);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build();

                NotificationChannel channel = new NotificationChannel(HIGH_PRIORITY_CHANNEL_ID, name, importance);
                channel.setDescription(description);
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 600, 250, 600, 250, 800});
                channel.setSound(soundUri, audioAttributes);
                channel.setShowBadge(true);
                channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                channel.setBypassDnd(true);

                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
