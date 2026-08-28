package com.ethanr.intelligentsoundmanager;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;


public class PeriodManager extends BroadcastReceiver {
    private static final String TAG = "intelli_sound:" + PeriodManager.class.getSimpleName();

    public final static String PROFILE_ID_REQUEST = "PROFILE_ID_REQUEST";
    public final static String SOUND_REQUEST = "SOUND_REQUEST";
    public final static int NEW_SOUND_REQUEST = 1;
    public final static int RESTORE_SOUND_REQUEST = 2;
    static final String ACTION_PERIOD_MANAGER = "PERIOD_MANAGER";


    private void cleanup_after_boot(Context context, Intent intent) {
        int profile_id;

        Log.d(TAG, "onReceive() intent = " + intent.getAction() + " looking for active profiles");

        for (profile_id = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE; profile_id < ConfigPeriodActivity.NPROFILES; profile_id++) {
            if (Utils.period_profile_worker_active(context, profile_id) != Utils.PREFERENCE_UNSET) {
                Log.i(TAG, "onReceive() found active profile - " + profile_id + ", restoring sound regardless of time");
                Utils.restore_sound_and_update_main_activity(context, profile_id);
            }
        }
    }


    private void sound_request(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Log.e(TAG, "sound_request() action is null");
            return;
        }

        if (action.equals(ACTION_PERIOD_MANAGER)) {
            int profile_id = intent.getIntExtra(PROFILE_ID_REQUEST, -1);
            if (profile_id == -1) {
                Log.e(TAG, "sound_request() unsupported profile_id = " + profile_id);
                return;
            }

            int request = intent.getIntExtra(SOUND_REQUEST, -1);
            switch (request) {
                case NEW_SOUND_REQUEST:
                    Log.d(TAG, "sound_request() NEW_SOUND_REQUEST");
                    Utils.new_sound_and_update_activities(context, profile_id);
                    // android stopped supporting repeat alarms, doing it manually
                    Utils.schedule_exact_timer(context, profile_id, AlarmManager.INTERVAL_DAY, NEW_SOUND_REQUEST);
                    break;
                case RESTORE_SOUND_REQUEST:
                    Log.d(TAG, "sound_request() RESTORE_SOUND_REQUEST");
                    Utils.restore_sound_and_update_main_activity(context, profile_id);
                    if (profile_id != ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
                        Utils.schedule_exact_timer(context, profile_id, AlarmManager.INTERVAL_DAY, RESTORE_SOUND_REQUEST);
                    break;
                default:
                    Log.e(TAG, "sound_request() unsupported request = " + request);
                    return;
            }

            // update favorite contacts if they changed during sound change
            Utils.update_favorite_contacts(context);
        }
    }

    private void handle_boot(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Log.e(TAG, "handle_boot() action is null");
            return;
        }

        if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            cleanup_after_boot(context, intent);
            // update favorite contacts if they changed since last boot
            Utils.update_favorite_contacts(context);
        }

    }

    private void detect_phone(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            Log.e(TAG, "detect_phone() action is null");
            return;
        }

        if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String extra_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (extra_state == null) {
                Log.e(TAG, "detect_phone() extra_state is null");
                return;
            }


            String incoming_phone_num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(TAG, "detect_phone() extra_state = " + extra_state + " incoming_phone_num = " + incoming_phone_num);
            if (incoming_phone_num != null) {
                if (Utils.is_favorite_contact(context, incoming_phone_num)) {
                    int profile_id = Utils.get_active_profile(context);
                    if (profile_id != Utils.PREFERENCE_UNSET) {
                        Log.d(TAG, "detect_phone() new volume is active for profile_id = " + profile_id);
                        if (extra_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                            Log.d(TAG, "detect_phone() EXTRA_STATE_OFFHOOK");
                        } else if (extra_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                            Log.i(TAG, "detect_phone() EXTRA_STATE_IDLE");
                            Utils.restore_favorites_sound(context);
                        } else if (extra_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                            Log.i(TAG, "detect_phone() EXTRA_STATE_RINGING " + incoming_phone_num);
                            Utils.set_favorites_sound(context, profile_id);
                        }
                    }
                    else Log.d(TAG, "detect_phone() new volume is not active for any profile");
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "PeriodManager:onReceive() intent = " + intent.getAction());

        handle_boot(context, intent);

        sound_request(context, intent);

        detect_phone(context, intent);
    }

}
