package com.ethanr.intelligentsoundmanager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class DebugActivity extends AppCompatActivity {
    private Context context;
    private static final String TAG = "intelli_sound:" + DebugActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_debug);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        context = getApplicationContext();

        dump_all_preferences(context);
    }


    private void dump_all_preferences(Context context) {
        int i;
        TextView t = findViewById(R.id.debug_view_id);

        t.setText("Per-Profile Preferences");

        for (i = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE; i < ConfigPeriodActivity.NPROFILES; i++) {
            t.append("\nprofile_id = " + i);
            t.append("\nPERIOD_WORKER_ACTIVE = " + Utils.read_profile_preference(context, i, Utils.PERIOD_WORKER_ACTIVE));
            t.append("\nNEW_RINGER_VOLUME = " + Utils.read_profile_preference(context, i, Utils.NEW_RINGER_VOLUME));
            t.append("\nNEW_MEDIA_VOLUME = " + Utils.read_profile_preference(context, i, Utils.NEW_MEDIA_VOLUME));
            if (i > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
                t.append("\nPERIOD_START_TIME_HOUR = " + Utils.read_profile_preference(context, i, Utils.PERIOD_START_TIME_HOUR));
                t.append("\nPERIOD_START_TIME_MINUTE = " + Utils.read_profile_preference(context, i, Utils.PERIOD_START_TIME_MINUTE));
            }
            t.append("\nPERIOD_END_TIME_HOUR = " + Utils.read_profile_preference(context, i, Utils.PERIOD_END_TIME_HOUR));
            t.append("\nPERIOD_END_TIME_MINUTE = " + Utils.read_profile_preference(context, i, Utils.PERIOD_END_TIME_MINUTE));
            t.append("\nNEW_VOLUME_ACTIVE = " + Utils.read_profile_preference(context, i, Utils.NEW_VOLUME_ACTIVE));
            t.append("\nENABLE_FAVORITES_INCOMING_CALL = " + Utils.read_profile_preference(context, i, Utils.ENABLE_FAVORITES_INCOMING_CALL));
        }

        t.append("\n\nCommon Preferences");
        t.append("\nINITIAL_RINGER_MODE = " + Utils.read_preference(context, Utils.INITIAL_RINGER_MODE));
        t.append("\nINITIAL_RINGER_VOLUME = " + Utils.read_preference(context, Utils.INITIAL_RINGER_VOLUME));
        t.append("\nINITIAL_MEDIA_VOLUME = " + Utils.read_preference(context, Utils.INITIAL_MEDIA_VOLUME));
        t.append("\nFAVORITES_VOLUME = " + Utils.read_preference(context, Utils.FAVORITES_VOLUME));
        t.append("\nFAVORITES_INITIAL_RINGER_MODE = " + Utils.read_preference(context, Utils.FAVORITES_INITIAL_RINGER_MODE));
        t.append("\nFAVORITES_FIRST_INCOMING_CALL_TS = " + Utils.read_preference_long(context, Utils.FAVORITES_FIRST_INCOMING_CALL_TS));
        t.append("\nnext timer time = " + Utils.next_alarm_time(context));

        t.append("\n\nBrightness Preferences");
        int brightness_threshold = Utils.read_preference(context, Utils.BRIGHTNESS_THRESHOLD_PERCENTAGE);
        String brightness_threshold_string = (brightness_threshold == Utils.PREFERENCE_UNSET) ?
                String.valueOf(BrightnessControlService.BRIGHTNESS_THRESHOLD_PERCENTAGE) :
                String.valueOf(brightness_threshold);
        t.append("\nBRIGHTNESS_THRESHOLD_PERCENTAGE = " + brightness_threshold_string);
        t.append("\nCurrent LUX = " + BrightnessControlService.get_current_lux());

        t.append("\n\nCPU Preferences");
        int numberOfCores = CPUFrequencyReader.getNumberOfCores();
        t.append("\nNumber of cores = " + numberOfCores);
        for (i = 0; i < numberOfCores; i++) {
            t.append("\nCore " + i + " frequency = " + CPUFrequencyReader.getCpuFrequency(i));
        }
    }
}