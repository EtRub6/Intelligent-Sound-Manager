package com.ethanr.intelligentsoundmanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

public class BrightnessControlService extends Service implements SensorEventListener {
    private static final String TAG = "intelli_sound:" + BrightnessControlService.class.getSimpleName();
    private static final String CHANNEL_ID = "BrightnessControlServiceChannel";

    // --- Configuration Constants ---
    private static final int MAX_BRIGHTNESS = 255;
    private int brightnessThresholdValue;
    private static final float LUX_THRESHOLD = 2000f;
    public static final int BRIGHTNESS_THRESHOLD_PERCENTAGE = 80;
    private static final int CUSTOM_DELAY_MICROSECONDS = 10 * 1000 * 1000;

    // ADDED: Constant for the minimum delay between brightness changes (5 seconds)
    private static final long MIN_DELAY_MILLIS = 5000L;

    // --- State and Sensor Variables ---
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private static float currentLux;
    private boolean isOverridden = false;
    private boolean sensorRegistered = false;

    // ADDED: Variable to track the time of the last successful brightness change
    private long lastChangeTimeMillis = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_enable_feature), false)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Adjusting Brightness")
                .setContentText("Intelligent brightness control is active.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }

        updateBrightnessThresholdValue();
        Log.d(TAG, "Brightness threshold set to " + this.brightnessThresholdValue + " (" + (int)(this.brightnessThresholdValue / (float)MAX_BRIGHTNESS * 100) + "%)");

        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        if (sensorManager == null) {
            Log.e(TAG, "SensorManager unavailable; stopping brightness service.");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (lightSensor == null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (lightSensor != null && !sensorRegistered) {
            sensorRegistered = sensorManager.registerListener(this, lightSensor, CUSTOM_DELAY_MICROSECONDS);
            if (!sensorRegistered) {
                Log.e(TAG, "Unable to register the ambient-light sensor.");
            }
        }

        return START_STICKY;
    }

    public static float get_current_lux() {
        return currentLux;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            currentLux = event.values[0];
            long currentTime = System.currentTimeMillis();

            // Check if enough time has passed since the last change
            if (currentTime - lastChangeTimeMillis < MIN_DELAY_MILLIS) {
                // If not, do nothing and return early
                return;
            }

            if (currentLux > LUX_THRESHOLD && !isOverridden) {
                updateBrightnessThresholdValue();
                if (setSystemBrightness(this.brightnessThresholdValue, true)) {
                    isOverridden = true;
                    Log.d(TAG, "Lux " + currentLux + " > " + LUX_THRESHOLD + ". Overriding to " + this.brightnessThresholdValue);
                    lastChangeTimeMillis = currentTime;
                }
            }
            else if (currentLux <= LUX_THRESHOLD && isOverridden) {
                if (setSystemBrightness(0, false)) {
                    isOverridden = false;
                    Log.d(TAG, "Lux " + currentLux + " <= " + LUX_THRESHOLD + ". Reverting to auto mode.");
                    lastChangeTimeMillis = currentTime;
                }
            }
        }
    }

    private void updateBrightnessThresholdValue() {
        int brightnessPercentage = Utils.read_brightness_threshold(this);
        if (brightnessPercentage == Utils.PREFERENCE_UNSET) {
            brightnessPercentage = BRIGHTNESS_THRESHOLD_PERCENTAGE;
        }
        brightnessPercentage = Math.max(10, Math.min(100, brightnessPercentage));
        this.brightnessThresholdValue = (int)(MAX_BRIGHTNESS * (brightnessPercentage / 100.0f));
    }

    private boolean setSystemBrightness(int brightnessValue, boolean manualMode) {
        if (Settings.System.canWrite(this)) {
            ContentResolver contentResolver = getContentResolver();

            try {
                int currentMode = Settings.System.getInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                int currentBrightness = Settings.System.getInt(contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS);

                if (manualMode) {
                    if (currentMode != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL || currentBrightness != brightnessValue) {
                        Settings.System.putInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                        Settings.System.putInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
                    }
                } else {
                    if (currentMode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        Settings.System.putInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    }
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Permission to write settings denied or other error.", e);
            }
        } else {
            Log.w(TAG, "WRITE_SETTINGS permission not granted.");
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorRegistered = false;
        }
        if (isOverridden) {
            setSystemBrightness(0, false);
            isOverridden = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Brightness Control Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}