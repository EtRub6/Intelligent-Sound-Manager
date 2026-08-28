package com.ethanr.intelligentsoundmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Utils {

    private static final String TAG = "intelli_sound:" + Utils.class.getSimpleName();
    private static final String TIME_FORMAT = "hh:mma";
    final static int PREFERENCE_UNSET = -1;
    final static long PREFERENCE_UNSET_LONG = -1;
    final static String PREFERENCE_UNSET_STRING = "PREFERENCE_UNSET";
    public final static String INITIAL_RINGER_MODE = "INITIAL_RINGER_MODE";
    public final static String INITIAL_RINGER_VOLUME = "INITIAL_RINGER_VOLUME";
    public final static String INITIAL_NOTIFICATION_VOLUME = "INITIAL_NOTIFICATION_VOLUME";
    public final static String INITIAL_MEDIA_VOLUME = "INITIAL_MEDIA_VOLUME";
    public static final String INTELLI_SOUND_PREFS = "intelli_sound_prefs";
    public final static String NEW_RINGER_VOLUME = "NEW_RINGER_VOLUME";
    public final static String NEW_NOTIFICATION_VOLUME = "NEW_NOTIFICATION_VOLUME";
    public final static String NEW_MEDIA_VOLUME = "NEW_MEDIA_VOLUME";
    final static String NEW_VOLUME_ACTIVE = "NEW_VOLUME_ACTIVE";
    public static final String PERIOD_START_TIME_HOUR = "PERIODIC_START_TIME_HOUR";
    public static final String PERIOD_START_TIME_MINUTE = "PERIODIC_START_TIME_MINUTE";
    public static final String PERIOD_END_TIME_HOUR = "PERIODIC_END_TIME_HOUR";
    public static final String PERIOD_END_TIME_MINUTE = "PERIODIC_END_TIME_MINUTE";
    final static String PERIOD_WORKER_ACTIVE = "PERIOD_WORKER_ACTIVE";
    final static int FAVORITE_MAX_PHONE_NUMBER = 10;
    final static String FAVORITE_PHONE_NUMBER = "FAVORITE_PHONE_NUMBER";
    final static int FAVORITES_VOLUME_LEVEL = 1;
    final static int FAVORITES_VOLUME_LEVEL_INCREASE = 2;
    final static String FAVORITES_VOLUME = "FAVORITES_VOLUME";
    final static String FAVORITES_INITIAL_RINGER_MODE = "FAVORITES_INITIAL_RINGER_MODE";
    public final static String FAVORITES_INITIAL_RINGER_VOLUME = "FAVORITES_INITIAL_RINGER_VOLUME";
    public final static String FAVORITES_FIRST_INCOMING_CALL_TS = "FAVORITES_FIRST_INCOMING_CALL_TS";
    private final static int FAVORITES_INCOMING_CALL_DELTA_MINS = 15;
    final static String ENABLE_FAVORITES_INCOMING_CALL = "ENABLE_FAVORITES_INCOMING_CALL";
    final static int ENABLE_FAVORITES = 1;
    final static int DISABLE_FAVORITES = 0;
    public final static String BRIGHTNESS_THRESHOLD_PERCENTAGE = "BRIGHTNESS_THRESHOLD_PERCENTAGE";

    public static void clear_preferences(Context context) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.clear();
        editor.apply();
        editor.commit();

        Log.d(TAG, "clear_preferences()");
    }

    public static void clearDefaultPreferences(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        editor.commit();
    }

    public static void commit_preferences(Context context) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.commit();

        Log.d(TAG, "commit_preferences()");
    }

    public static void save_preference(Context context, String name, int val) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt(name, val);
        editor.apply();
        editor.commit();
    }

    private static void save_preference(Context context, String name, long val) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putLong(name, val);
        editor.apply();
        editor.commit();
    }

    private static void save_preference(Context context, String name, String s) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(name, s);
        editor.apply();
        editor.commit();
    }


    public static int read_preference(Context context, String name) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        return sharedPref.getInt(name, PREFERENCE_UNSET);
    }

    public static long read_preference_long(Context context, String name) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        return sharedPref.getLong(name, PREFERENCE_UNSET_LONG);
    }

    private static String read_preference_string(Context context, String name) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        return sharedPref.getString(name, PREFERENCE_UNSET_STRING);
    }

    private static void remove_preference(Context context, String name) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences sharedPref = deviceContext.getSharedPreferences(INTELLI_SOUND_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.remove(name);
        editor.commit();
    }

    public static void remove_profile_preference(Context context, int profile_id, String name) {
        remove_preference(context, name + "_" + profile_id);
    }

    public static void save_profile_preference(Context context, int profile_id, String name, int val) {
        save_preference(context, name + "_" + profile_id, val);
    }

    public static void save_profile_preference(Context context, int profile_id, String name, long val) {
        save_preference(context, name + "_" + profile_id, val);
    }

    public static void save_profile_preference(Context context, int profile_id, String name, String s) {
        save_preference(context, name + "_" + profile_id, s);
    }


    public static int read_profile_preference(Context context, int profile_id, String name) {
        return read_preference(context, name + "_" + profile_id);
    }

    public static long read_profile_preference_long(Context context, int profile_id, String name) {
        return read_preference_long(context, name + "_" + profile_id);
    }

    public static String read_profile_preference_string(Context context, int profile_id, String name) {
        return read_preference_string(context, name + "_" + profile_id);
    }

    public static void save_brightness_threshold(Context context, int percentage) {
        save_preference(context, BRIGHTNESS_THRESHOLD_PERCENTAGE, percentage);
        Log.d(TAG, "save_brightness_threshold() saved percentage = " + percentage);
    }

    public static int read_brightness_threshold(Context context) {
        return read_preference(context, BRIGHTNESS_THRESHOLD_PERCENTAGE);
    }

    public static String time_to_ampm(int hour, int minute){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        SimpleDateFormat time_formatted = new SimpleDateFormat(TIME_FORMAT, Locale.US);
        return time_formatted.format(c.getTime()).toLowerCase();
    }

    public static int get_ringer_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "get_ringer_volume() mAudioManager is null");
            return -1;
        }
        return mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
    }

    public static void set_ringer_volume(Context context, int volume) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
    }

    public static int get_ringer_max_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "get_ringer_max_volume() mAudioManager is null");
            return -1;
        }
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    public static int get_notification_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "get_notification_volume() mAudioManager is null");
            return -1;
        }
        return mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
    }

    public static void set_notification_volume(Context context, int volume) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
    }

    public static int get_notification_max_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "get_notification_max_volume() mAudioManager is null");
            return -1;
        }
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
    }

    private static int get_ringer_mode(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "get_ringer_mode() mAudioManager is null");
            return -1;
        }
        return mAudioManager.getRingerMode();
    }

    private static void save_ringer_mode(Context context, String preference) {
        int mode = get_ringer_mode(context);
        if (mode == -1)
            return;

        Log.d(TAG, "save_ringer_mode " + preference + " = " + mode);
        save_preference(context, preference, mode);
    }

    private static void save_ringer_volume(Context context, String preference) {
        int volume = get_ringer_volume(context);
        if (volume == -1)
            return;

        Log.d(TAG, "save_ringer_mode " + preference + " = " + volume);
        save_preference(context, preference, volume);
    }

    private static void save_notification_volume(Context context, String preference) {
        int volume = get_notification_volume(context);
        if (volume == -1)
            return;

        Log.d(TAG, "save_notification_volume " + preference + " = " + volume);
        save_preference(context, preference, volume);
    }

    public static void save_media_volume(Context context, String preference) {
        int volume = get_media_volume(context);
        if (volume == -1)
            return;
        save_preference(context, preference, volume);
    }

    private static void restore_ringer_mode(Context context, String preference) {
        int mode = read_preference(context, preference);
        if (mode == PREFERENCE_UNSET) {
            Log.e(TAG, "restore_ringer_mode() PREFERENCE_UNSET, probably already restored");
            return;
        }
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setRingerMode(mode);
        Log.i(TAG, "restore_ringer_mode() mode = " + mode);
    }


    public static void restore_initial_ringer_volume(Context context) {
        int volume = read_preference(context, INITIAL_RINGER_VOLUME);
        if (volume == PREFERENCE_UNSET) {
            Log.e(TAG, "restore_initial_ringer_volume() PREFERENCE_UNSET, probably already restored");
            return;
        }
        set_ringer_volume(context, volume);
        Log.i(TAG, "restore_initial_ringer_volume() volume = " + volume);
    }

    public static void restore_initial_notification_volume(Context context) {
        int volume = read_preference(context, INITIAL_NOTIFICATION_VOLUME);
        if (volume == PREFERENCE_UNSET) {
            Log.e(TAG, "restore_initial_notification_volume() PREFERENCE_UNSET, probably already restored");
            return;
        }
        set_notification_volume(context, volume);
        Log.i(TAG, "restore_initial_notification_volume() volume = " + volume);
    }

    private static void restore_initial_media_volume(Context context) {
        int volume = read_preference(context, INITIAL_MEDIA_VOLUME);
        if (volume == PREFERENCE_UNSET) {
            Log.d(TAG, "restore_initial_media_volume() PREFERENCE_UNSET, probably already restored");
            return;
        }
        set_media_volume(context, volume);
        Log.i(TAG, "restore_initial_media_volume() volume = " + volume);
    }

    private static void set_ringer_mode_silent(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            Log.e(TAG, "set_ringer_mode_silent() mAudioManager is null");
            return;
        }
        try {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } catch (SecurityException e) {
            Log.e(TAG, "silence_ringer_vibration() " + e);
        }
    }

    public static int get_media_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null)
            return -1;
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static int get_media_max_volume(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null)
            return -1;
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static void set_media_volume(Context context, int volume) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null)
            return;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public static void save_initial_sound_preferences(Context context) {
        save_ringer_mode(context, INITIAL_RINGER_MODE);
        save_ringer_volume(context, INITIAL_RINGER_VOLUME);
        save_notification_volume(context, INITIAL_NOTIFICATION_VOLUME);
        save_media_volume(context, INITIAL_MEDIA_VOLUME);
        save_preference(context, FAVORITES_VOLUME, FAVORITES_VOLUME_LEVEL);
    }

    public static void restore_initial_sound(Context context, int profile_id) {
        if (read_profile_preference(context, profile_id, NEW_RINGER_VOLUME) == 0)
            restore_ringer_mode(context, INITIAL_RINGER_MODE);

        restore_initial_ringer_volume(context);
        restore_initial_notification_volume(context);
        restore_initial_media_volume(context);
    }

    public static void cleanup_sound_preferences(Context context) {
        remove_preference(context, INITIAL_RINGER_MODE);
        remove_preference(context, INITIAL_RINGER_VOLUME);
        remove_preference(context, INITIAL_NOTIFICATION_VOLUME);
        remove_preference(context, INITIAL_MEDIA_VOLUME);
        Log.d(TAG, "cleanup_sound_preferences()");
    }

    public static void set_new_volume(Context context, int profile_id) {
        int new_ringer_volume = read_profile_preference(context, profile_id, NEW_RINGER_VOLUME);
        int new_notification_volume = read_profile_preference(context, profile_id, NEW_NOTIFICATION_VOLUME);
        int new_media_volume = read_profile_preference(context, profile_id, NEW_MEDIA_VOLUME);

        Log.i(TAG, "set_new_volume() new volume ringer/notification/media " + new_ringer_volume + "/" + new_notification_volume + "/" + new_media_volume);

        if (new_ringer_volume == 0) {
            set_ringer_mode_silent(context);
            set_notification_volume(context, 0);
            set_media_volume(context, 0);
        } else {
            set_ringer_volume(context, new_ringer_volume);
            set_notification_volume(context, new_notification_volume);
            set_media_volume(context, new_media_volume);
        }

        set_new_volume_active_preference(context, profile_id);

        Log.i(TAG, "set_new_volume() new media volume " + new_ringer_volume);
    }

    public static boolean periodic_start_immediately(Context context, int profile_id) {
        int hour = read_profile_preference(context, profile_id, PERIOD_START_TIME_HOUR);
        if (hour == PREFERENCE_UNSET) { Log.e(TAG, "periodic_start_immediately() start hour = " + hour); return false; }
        int minute = read_profile_preference(context, profile_id, PERIOD_START_TIME_MINUTE);
        if (minute == PREFERENCE_UNSET) { Log.e(TAG, "periodic_start_immediately() start minute = " + minute); return false; }
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, hour);
        start.set(Calendar.MINUTE, minute);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        hour = read_profile_preference(context, profile_id, PERIOD_END_TIME_HOUR);
        if (hour == PREFERENCE_UNSET) { Log.e(TAG, "periodic_start_immediately() end hour = " + hour); return false; }
        minute = read_profile_preference(context, profile_id, PERIOD_END_TIME_MINUTE);
        if (minute == PREFERENCE_UNSET) { Log.e(TAG, "periodic_start_immediately() end minute = " + minute); return false; }
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, hour);
        end.set(Calendar.MINUTE, minute);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();

        if (start.before(end)) {
            if (now.before(start))
                return false;
            else return now.before(end);
        }
        else if (now.before(end))
            return true;

        return start.before(now);
    }

    private static long calculate_ms_from_now(int hour, int minute) {
        Calendar future = Calendar.getInstance();
        future.set(Calendar.HOUR_OF_DAY, hour);
        future.set(Calendar.MINUTE, minute);
        future.set(Calendar.SECOND, 0);
        future.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();

        if (future.before(now))
            future.add(Calendar.HOUR_OF_DAY, 24);

        return future.getTimeInMillis() - now.getTimeInMillis();
    }

    private static long calculate_ms_from_now_end_now(int hour, int minute) {
        Calendar future = Calendar.getInstance();
        future.set(Calendar.HOUR_OF_DAY, hour);
        future.set(Calendar.MINUTE, minute);
        future.set(Calendar.SECOND, 0);
        future.set(Calendar.MILLISECOND, 0);
        future.add(Calendar.HOUR_OF_DAY, 24);

        return future.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }


    private static int get_broadcast_request_code(int profile_id, int sound_request) {
        return profile_id * 10 + sound_request;
    }

    public static PendingIntent get_period_pending_intent(Context context, int profile_id, int sound_request) {
        Intent intent = new Intent(context, PeriodManager.class);
        intent.putExtra(PeriodManager.PROFILE_ID_REQUEST, profile_id);
        intent.putExtra(PeriodManager.SOUND_REQUEST, sound_request);
        intent.setAction(PeriodManager.ACTION_PERIOD_MANAGER);

        return PendingIntent.getBroadcast(context, get_broadcast_request_code(profile_id, sound_request), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule_exact_timer(Context context, int profile_id, long ms, int sound_request) {
        PendingIntent pending_intent = get_period_pending_intent(context, profile_id, sound_request);
        if (pending_intent == null) {
            Log.e(TAG, "schedule_exact_timer() pending_intent is null");
            return;
        }

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager.canScheduleExactAlarms())
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pending_intent);
        else
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ms, pending_intent);

        Log.d(TAG, "schedule_exact_timer() alarm for profile = " + profile_id + " ms = " + ms);
    }

    public static void start_period_worker_start_time(Context context, int profile_id) {
        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            Log.e(TAG, "start_period_worker_start_time() wrong profile_id = " + profile_id);
            return;
        }

        int start_hour = read_profile_preference(context, profile_id, PERIOD_START_TIME_HOUR);
        if (start_hour == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_start_time() start_hour = " + start_hour); return; }
        int start_minute = read_profile_preference(context, profile_id, PERIOD_START_TIME_MINUTE);
        if (start_minute == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_start_time() start_minute = " + start_minute); return; }

        long new_sound_ms = calculate_ms_from_now(start_hour, start_minute);
        schedule_exact_timer(context, profile_id, new_sound_ms, PeriodManager.NEW_SOUND_REQUEST);
        Log.i(TAG, "start_period_worker_start_time() set alarm to new sound at " + start_hour + ":" + start_minute);
    }

    public static void start_period_worker_end_time(Context context, int profile_id) {
        int end_hour = read_profile_preference(context, profile_id, PERIOD_END_TIME_HOUR);
        if (end_hour == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_end_time() end_hour = " + end_hour); return; }
        int end_minute = read_profile_preference(context, profile_id, PERIOD_END_TIME_MINUTE);
        if (end_minute == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_end_time() end_minute = " + end_minute); return; }


        long revert_ms = calculate_ms_from_now(end_hour, end_minute);
        schedule_exact_timer(context, profile_id, revert_ms, PeriodManager.RESTORE_SOUND_REQUEST);
        Log.i(TAG, "start_period_worker_end_time() set alarm to restore sound at " + end_hour + ":" + end_minute);
    }

    // triggered if stop now button called only
    public static void start_period_worker_end_now_end_time(Context context, int profile_id) {
        int end_hour = read_profile_preference(context, profile_id, PERIOD_END_TIME_HOUR);
        if (end_hour == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_end_time() end_hour = " + end_hour); return; }
        int end_minute = read_profile_preference(context, profile_id, PERIOD_END_TIME_MINUTE);
        if (end_minute == PREFERENCE_UNSET) { Log.e(TAG, "start_period_worker_end_time() end_minute = " + end_minute); return; }


        // RESTORE
        long revert_ms = calculate_ms_from_now_end_now(end_hour, end_minute);
        schedule_exact_timer(context, profile_id, revert_ms, PeriodManager.RESTORE_SOUND_REQUEST);
        Log.i(TAG, "start_period_worker_end_time() set alarm to restore sound at " + end_hour + ":" + end_minute);
    }

    public static void start_period_worker(Context context, int profile_id) {
        start_period_worker_end_time(context, profile_id);

        if (profile_id != ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
            start_period_worker_start_time(context, profile_id);

        set_period_worker_active_preference(context, profile_id);
    }


      public static void cancel_period_sound_request(Context context, int profile_id, int sound_request) {
        // Cancel restore sound
        PendingIntent pending_intent = get_period_pending_intent(context, profile_id, sound_request);
        if (pending_intent == null) {
            Log.e(TAG, "cancel_period_sound_request() pending_intent is null");
            return;
        }

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pending_intent);

        Log.i(TAG, "cancel_period_sound_request() cancelled alarm for profile_id = " + profile_id);
    }

    public static long next_alarm_time(Context context) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo next_clock = alarmManager.getNextAlarmClock();

        if (next_clock == null)
            return -1;

        long t = next_clock.getTriggerTime();

        Log.d(TAG, "next_alarm_time() t = " + t);

        return t;
    }

    public static void cancel_period(Context context, int profile_id) {
        cancel_period_sound_request(context, profile_id, PeriodManager.RESTORE_SOUND_REQUEST);
        Log.d(TAG, "cancel_period() RESTORE_SOUND_REQUEST");

        if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            cancel_period_sound_request(context, profile_id, PeriodManager.NEW_SOUND_REQUEST);
            Log.d(TAG, "cancel_period() NEW_SOUND_REQUEST");
        }

        update_favorite_contacts(context);
    }

    private static void set_new_volume_active_preference(Context context, int profile_id) {
        save_profile_preference(context, profile_id, NEW_VOLUME_ACTIVE, 1);
        Log.d(TAG, "set_new_volume_active_preference() profile_id " + profile_id);
    }

    public static int new_volume_active(Context context, int profile_id) {
        return read_profile_preference(context, profile_id, NEW_VOLUME_ACTIVE);
    }

    public static void set_new_volume_inactive_preference(Context context, int profile_id) {
        remove_profile_preference(context, profile_id, NEW_VOLUME_ACTIVE);
        Log.d(TAG, "set_new_volume_inactive_preference() profile_id " + profile_id);
    }

    private static void set_period_worker_active_preference(Context context, int profile_id) {
        save_profile_preference(context, profile_id, PERIOD_WORKER_ACTIVE, 1);
        Log.d(TAG, "set_period_worker_active_preference() profile_id " + profile_id);
    }

    public static int period_profile_worker_active(Context context, int profile_id) {
        return read_profile_preference(context, profile_id, PERIOD_WORKER_ACTIVE);
    }

    public static void set_period_worker_inactive_preference(Context context, int profile_id) {
        remove_profile_preference(context, profile_id, PERIOD_WORKER_ACTIVE);
        Log.d(TAG, "set_period_worker_inactive_preference() profile_id " + profile_id);
    }

    public static int get_active_profile(Context context) {
        int i;

        for (i = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE; i < ConfigPeriodActivity.NPROFILES; i++) {
            if (read_profile_preference(context, i, NEW_VOLUME_ACTIVE) != PREFERENCE_UNSET)
                return i;
        }

        return PREFERENCE_UNSET;
    }

    private static void remove_time_preferences(Context context, int profile_id) {
        remove_profile_preference(context, profile_id, PERIOD_END_TIME_HOUR);
        remove_profile_preference(context, profile_id, PERIOD_END_TIME_MINUTE);

        if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            remove_profile_preference(context, profile_id, PERIOD_START_TIME_HOUR);
            remove_profile_preference(context, profile_id, PERIOD_START_TIME_MINUTE);
        }

        Log.d(TAG, "remove_time_preferences() profile " + profile_id);
    }

    public static void update_activities(Context context, int profile_id, int type) {
        Log.d(TAG, "update_activities() profile " + profile_id);
        Intent intent = new Intent(context.getPackageName());
        intent.putExtra(PeriodManager.PROFILE_ID_REQUEST, profile_id);
        intent.putExtra(PeriodManager.SOUND_REQUEST, type);
        context.sendBroadcast(intent);
    }

    public static void update_widget_enable(Context context, boolean enable, String time) {
        Log.d(TAG, "update_widget_enable() enable = " + enable);

        // The action is set here.
        Intent intent = new Intent(SinglePeriodManagerWidget.WIDGET_BACKGROUND_ACTION);
        // Set the component, telling the system where to send the broadcast.
        intent.setComponent(new ComponentName(context, SinglePeriodManagerWidget.class));

        intent.putExtra(SinglePeriodManagerWidget.WIDGET_ENABLE, enable);
        intent.putExtra(SinglePeriodManagerWidget.WIDGET_TIME, time);
        context.sendBroadcast(intent);
    }

    public static void restore_sound(Context context, int profile_id) {
        Log.i(TAG, "restore_sound() profile " + profile_id);
        restore_initial_sound(context, profile_id);
        cleanup_sound_preferences(context);
    }

    public static void restore_sound_and_update_main_activity(Context context, int profile_id) {
        if (profile_id < ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            Log.e(TAG, "restore_sound_and_update_main_activity() wrong profile_id = " + profile_id);
            return;
        }

        restore_sound(context, profile_id);

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
            set_period_worker_inactive_preference(context, profile_id);

        set_new_volume_inactive_preference(context, profile_id);
        update_activities(context, profile_id, PeriodManager.RESTORE_SOUND_REQUEST);
        update_widget_enable(context, false, context.getString(R.string.dont_disturb_description));

        Log.i(TAG, "restore_sound_and_update_main_activity() profile " + profile_id);
    }

    public static void restore_sound_and_cancel_period_and_update_widget(Context context, int profile_id) {
        restore_initial_sound(context, profile_id);

        set_period_worker_inactive_preference(context, profile_id);
        set_new_volume_inactive_preference(context, profile_id);
        remove_time_preferences(context, profile_id);

        cancel_period(context, profile_id);

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
            update_widget_enable(context, false, context.getString(R.string.dont_disturb_description));
    }

    public static void clean_initial_and_set_new_sound(Context context, int profile_id) {
        cleanup_sound_preferences(context);
        save_initial_sound_preferences(context);
        set_new_volume(context, profile_id);
        Log.i(TAG, "clean_initial_and_set_new_sound() profile " + profile_id);
    }

    public static void new_sound_and_update_activities(Context context, int profile_id) {
        clean_initial_and_set_new_sound(context, profile_id);
        update_activities(context, profile_id, PeriodManager.NEW_SOUND_REQUEST);

        Log.i(TAG, "new_sound_and_update_main_activity() profile " + profile_id);
    }

    public static void save_favorite_contact_preference(Context context, int favorite_num, String name, String s) {
        save_preference(context, name + "_" + favorite_num, s);
    }

    public static String read_favorite_contact_preference(Context context, int favorite_num, String name) {
        return read_preference_string(context, name + "_" + favorite_num);
    }

    public static void remove_favorite_contact_preference(Context context, int favorite_num, String name) {
        remove_preference(context, name + "_" + favorite_num);
    }

    public static String removeCountryCode(String phoneNumber, String countryCode) {
        if (phoneNumber == null || phoneNumber.isEmpty() || countryCode == null || countryCode.isEmpty()) {
            return phoneNumber; // Or throw an exception, depending on requirements
        }

        if (phoneNumber.startsWith(countryCode)) {
            return phoneNumber.substring(countryCode.length());
        } else if (phoneNumber.startsWith("+" + countryCode)) {
            return phoneNumber.substring(countryCode.length() + 1);
        }

        return phoneNumber; // If country code not found, return original number
    }


    public static void update_favorite_contacts(Context context) {
        int favorites = 0;

        // wipe out all favorite contacts first then add
        delete_favorite_contacts(context);

        ContentResolver contentResolver = context.getContentResolver();

        // Use try-with-resources for the main cursor
        try (Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int column = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    if (column < 0) {
                        Log.e(TAG, "update_favorite_contacts() HAS_PHONE_NUMBER wrong column = " + column);
                        return; // Exit here, cursor is closed automatically
                    }

                    if (cursor.getInt(column) == 1) {
                        column = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                        if (column < 0) {
                            Log.e(TAG, "update_favorite_contacts() _ID wrong column = " + column);
                            return; // Exit here
                        }

                        String id = String.valueOf(cursor.getLong(column));

                        // Use nested try-with-resources for the phone cursor
                        try (Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null)) {

                            if (phoneCursor != null) {
                                while (phoneCursor.moveToNext()) {
                                    column = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
                                    if (column < 0) {
                                        Log.e(TAG, "update_favorite_contacts() STARRED wrong column = " + column);
                                        return; // Exit here
                                    }

                                    if (phoneCursor.getInt(column) == 1) {
                                        column = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                        if (column < 0) {
                                            Log.e(TAG, "update_favorite_contacts() NUMBER wrong column = " + column);
                                            return; // Exit here
                                        }

                                        String phone_num = phoneCursor.getString(column);
                                        if (!phone_num.isEmpty()) {
                                            String formatted_phone_num = removeCountryCode(PhoneNumberUtils.extractNetworkPortion(phone_num), "+1");
                                            if (!is_favorite_contact(context, formatted_phone_num)) {
                                                favorites++;
                                                save_favorite_contact_preference(context, favorites, FAVORITE_PHONE_NUMBER, formatted_phone_num);
                                                Log.d(TAG, "update_favorite_contacts() " + favorites + " " + formatted_phone_num);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        }
    }

    public static void delete_favorite_contacts(Context context) {
        int i;

        for (i = 0; i < FAVORITE_MAX_PHONE_NUMBER; i++) {
            if (read_favorite_contact_preference(context, i + 1, FAVORITE_PHONE_NUMBER).equals(PREFERENCE_UNSET_STRING))
                break;
            remove_favorite_contact_preference(context, i + 1, FAVORITE_PHONE_NUMBER);
        }

        if (i > 0)
            Log.d(TAG, "delete_favorite_contacts() deleted favorites " + i);
    }

    public static boolean is_favorite_contact(Context context, String incoming_phone_num) {
        int i;
        String favorite_contact;

        for (i = 0; i < FAVORITE_MAX_PHONE_NUMBER; i++) {
            favorite_contact = read_favorite_contact_preference(context, i + 1, FAVORITE_PHONE_NUMBER);
            if (favorite_contact.equals(PREFERENCE_UNSET_STRING))
                return false;

            //Log.d(TAG, "is_favorite_contact() incoming/favorite " + incoming_phone_num + "/" + favorite_contact);

            if (favorite_contact.equals(incoming_phone_num))
                return true;
        }

        Log.d(TAG, "is_favorite_contact() not match found for " + incoming_phone_num);
        return false;
    }

    private static void set_ringer_mode_normal(Context context) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        try {
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        } catch (SecurityException e) {
            Log.e(TAG, "set_ringer_mode_normal() " + e);
        }
    }

    public static void set_favorites_sound(Context context, int profile_id) {
        int enable_favorites = Utils.read_profile_preference(context, profile_id, Utils.ENABLE_FAVORITES_INCOMING_CALL);
        if (enable_favorites == Utils.PREFERENCE_UNSET) {
            Log.e(TAG, "set_favorites_sound() ERROR ENABLE_FAVORITES_INCOMING_CALL is supposed to be initialized");
            return;
        }

        if (enable_favorites == Utils.DISABLE_FAVORITES) {
            Log.d(TAG, "set_favorites_sound() DISABLE_FAVORITES, nothing to do");
            return;
        }

        boolean recent_call = update_favorites_incoming_call_ts(context);
        int volume;

        if (recent_call) {
            volume = read_preference(context, FAVORITES_VOLUME);
            if (volume == PREFERENCE_UNSET) {
                Log.d(TAG, "set_favorites_sound() PREFERENCE_UNSET");
                return;
            }
        }
        else volume = FAVORITES_VOLUME_LEVEL;

        int new_volume = read_profile_preference(context, profile_id, NEW_RINGER_VOLUME);
        if (new_volume == PREFERENCE_UNSET) {
            Log.e(TAG, "set_favorites_sound() profile_id " + profile_id + " NEW_RINGER_VOLUME unset");
            return;
        }
        volume = Math.max(volume, new_volume);


        save_ringer_mode(context, FAVORITES_INITIAL_RINGER_MODE);
        save_ringer_volume(context, FAVORITES_INITIAL_RINGER_VOLUME);

        set_ringer_mode_normal(context);
        set_ringer_volume(context, volume);

        int volume_increase = recent_call ? FAVORITES_VOLUME_LEVEL_INCREASE : 0;
        save_preference(context, FAVORITES_VOLUME, Math.min(volume + volume_increase, get_ringer_max_volume(context)));

        Log.d(TAG, "set_favorites_sound() volume/increase volume = " + volume + "/" + volume_increase + " recent call = " + recent_call);
    }

    public static void restore_favorites_sound(Context context) {
        int mode = read_preference(context, FAVORITES_INITIAL_RINGER_MODE);
        if (mode == PREFERENCE_UNSET) return;

        if (mode == 0)
            restore_ringer_mode(context, FAVORITES_INITIAL_RINGER_MODE);

        Log.d(TAG, "restore_favorites_sound() mode = " + mode);

        remove_preference(context, FAVORITES_INITIAL_RINGER_MODE);
        remove_preference(context, FAVORITES_INITIAL_RINGER_VOLUME);
    }

    private static boolean update_favorites_incoming_call_ts(Context context) {
        long ts = read_preference_long(context, FAVORITES_FIRST_INCOMING_CALL_TS);
        long ts_now = Calendar.getInstance().getTimeInMillis();

        if (ts == PREFERENCE_UNSET)
            save_preference(context, FAVORITES_FIRST_INCOMING_CALL_TS, ts_now);
        else if ((ts_now - ts) > FAVORITES_INCOMING_CALL_DELTA_MINS * 1000 * 60) {
            remove_preference(context, FAVORITES_FIRST_INCOMING_CALL_TS);
            return false;
        }

        return true; // recent call, within FAVORITES_FIRST_INCOMING_CALL_TS mins range
    }
}