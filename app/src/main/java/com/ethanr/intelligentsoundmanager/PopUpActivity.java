package com.ethanr.intelligentsoundmanager;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;

public class PopUpActivity extends Activity {
    private static final String TAG = "intelli_sound:" + PopUpActivity.class.getSimpleName();

    /**
     * Asks the user to set a time using a TimePickerDialog and then enables a single profile.
     * This method is called when a profile is not currently active.
     *
     * @param context The application context.
     * @param profileId The ID of the profile to enable.
     */
    private void widget_ask_time_and_enable(Context context, int profileId) {
        final Calendar currentCalendar = Calendar.getInstance();
        int currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = currentCalendar.get(Calendar.MINUTE);

        Log.d(TAG, "widget_ask_time_and_enable() current time " + currentHour + ":" + currentMinute);

        TimePickerDialog timePickerDialog = new TimePickerDialog(PopUpActivity.this,
                (view, selectedHour, selectedMinute) -> {
                    // Save the selected end time to preferences
                    Utils.save_profile_preference(context, ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, Utils.PERIOD_END_TIME_HOUR, selectedHour);
                    Utils.save_profile_preference(context, ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, Utils.PERIOD_END_TIME_MINUTE, selectedMinute);

                    // Set various sound and volume preferences
                    Utils.save_profile_preference(context, profileId, Utils.NEW_RINGER_VOLUME, 0);
                    Utils.save_profile_preference(context, profileId, Utils.NEW_NOTIFICATION_VOLUME, 0);
                    Utils.save_profile_preference(context, profileId, Utils.NEW_MEDIA_VOLUME, 0);
                    Utils.save_preference(context, Utils.FAVORITES_VOLUME, Utils.FAVORITES_VOLUME_LEVEL);
                    Utils.save_profile_preference(context, profileId, Utils.ENABLE_FAVORITES_INCOMING_CALL, Utils.ENABLE_FAVORITES);

                    // Clean initial sound and set new sound based on preferences
                    Utils.clean_initial_and_set_new_sound(context, profileId);
                    Utils.start_period_worker(context, profileId);

                    // Update the widget to reflect the new state
                    int endHour = Utils.read_profile_preference(context, profileId, Utils.PERIOD_END_TIME_HOUR);
                    int endMinute = Utils.read_profile_preference(context, profileId, Utils.PERIOD_END_TIME_MINUTE);
                    String time = Utils.time_to_ampm(endHour, endMinute);
                    Utils.update_widget_enable(context, true, time);

                    Log.i(TAG, "widget_ask_time_and_enable onTimeSet() setting PERIOD_PROFILE_SINGLE " + selectedHour + ":" + selectedMinute);

                    // Close the activity after the time has been set
                    PopUpActivity.this.finish();
                }, currentHour, currentMinute, false);

        // Handle the cancel button click
        timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                PopUpActivity.this.finish();
                Log.d(TAG, "widget_ask_time_and_enable onClick() cancel is clicked");
            }
        });

        timePickerDialog.show();
    }

    /**
     * Disables the active profile, restores the sound settings, and updates the widget.
     * This method is called when a profile is already active.
     *
     * @param context The application context.
     * @param profileId The ID of the profile to disable.
     */
    private void widget_disable(Context context, int profileId) {
        Utils.restore_sound_and_cancel_period_and_update_widget(context, profileId);
        Log.d(TAG, "widget_disable()");

        // Close the activity after disabling the profile
        PopUpActivity.this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make the activity transparent to act as a pop-up dialog
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        Context context = getApplicationContext();
        int profileId = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE;

        Log.d(TAG, "onCreate()");

        // Check if a worker is already active
        boolean isWorkerActive = Utils.period_profile_worker_active(context, profileId) != Utils.PREFERENCE_UNSET;

        if (!isWorkerActive) {
            // No worker active, so ask for time and enable
            widget_ask_time_and_enable(context, profileId);
        } else {
            // Worker is active, so disable it
            widget_disable(context, profileId);
        }
    }
}
