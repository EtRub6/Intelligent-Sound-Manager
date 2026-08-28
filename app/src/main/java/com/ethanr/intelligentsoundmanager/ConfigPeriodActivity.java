package com.ethanr.intelligentsoundmanager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.TimePickerDialog;
import android.widget.TimePicker;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;

public class ConfigPeriodActivity extends AppCompatActivity {

    private static final String TAG = "intelli_sound:" + ConfigPeriodActivity.class.getSimpleName();
    private static final int CONVENIENCE_MINUTES = 5;
    private Context context;
    private int profile_id;
    private int new_ring_volume, new_notification_volume, new_media_volume;
    private int start_hour, start_minute, end_hour, end_minute;
    private BroadcastReceiver alarm_receiver;

    final static String PERIOD_PROFILE_ID_REQUEST = "PERIODIC_PROFILE_ID_REQUEST";
    final static String PERIOD_PROFILE_ID_REPLY = "PERIODIC_PROFILE_ID_REPLY";
    final static int NPROFILES = 2;
    public static final int PERIOD_PROFILE_SINGLE = 0;
    public static final int PERIOD_PROFILE_1 = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config_period);

        // Keep the OK/Cancel/End Now buttons clear of the status bar and gesture nav.
        View root = findViewById(R.id.config_period_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        profile_id = getIntent().getIntExtra(PERIOD_PROFILE_ID_REQUEST, 0);
        context = getApplicationContext();

        load_and_display_preferences();

        register_alarm_receiver();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                Log.i(TAG, "onBackPressed() handled by dispatcher");
                setResult(RESULT_CANCELED);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        Log.d(TAG, "ConfigPeriodActivity onCreate() profile_id = " + profile_id);
    }

    void register_alarm_receiver() {
        alarm_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int profile_id = intent.getIntExtra(PeriodManager.PROFILE_ID_REQUEST, -1);
                if (profile_id == -1) {
                    Log.e(TAG, "ConfigPeriodActivity register_alarm_receiver() onReceive() unsupported profile_id = " + profile_id);
                    return;
                }

                int update_type = intent.getIntExtra(PeriodManager.SOUND_REQUEST, -1);
                if (update_type == PeriodManager.NEW_SOUND_REQUEST) {
                    enable_end_now(true);
                } else if (update_type == PeriodManager.RESTORE_SOUND_REQUEST) {
                    enable_end_now(false);
                } else {
                    Log.e(TAG, "ConfigPeriodActivity onReceive() unsupported update_type = " + update_type);
                }

                Log.d(TAG, "ConfigPeriodActivity onReceive() update_type = " + update_type);
            }
        };
        registerReceiver(alarm_receiver, new IntentFilter(getPackageName()), RECEIVER_NOT_EXPORTED);
    }

    private void show_title(int profile_id) {
        TextView title = findViewById(R.id.profile_title_id);
        if (title == null) {
            Log.e(TAG, "show_title() title is null");
            return ;
        }

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            title.setText(getString(R.string.profile_name_string, "Single"));
        } else if (profile_id == PERIOD_PROFILE_1) {
            title.setText(getString(R.string.profile_name_string, "Periodic 1"));
        }
    }

    private void load_and_display_preferences() {
        int hour, minute;
        String s;

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            Button start_button = findViewById(R.id.profile_start_time_button);
            start_button.setEnabled(false);
        }

        if (profile_id >= ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_HOUR);
            if (hour != Utils.PREFERENCE_UNSET) {
                minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_MINUTE);
                s = Utils.time_to_ampm(hour, minute);
                set_view_text(R.id.profile_end_time_button, s);
            } else {
                Button ok_button = findViewById(R.id.profile_ok_button);
                ok_button.setEnabled(false);
            }

            if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
                hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_HOUR);
                if (hour != Utils.PREFERENCE_UNSET) {
                    minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_MINUTE);
                    s = Utils.time_to_ampm(hour, minute);
                    set_view_text(R.id.profile_start_time_button, s);
                } else {
                    Button ok_button = findViewById(R.id.profile_ok_button);
                    ok_button.setEnabled(false);
                }
            }
        } else {
            return;
        }

        set_ring_volume_slider();
        set_notification_volume_slider();
        set_media_volume_slider();
        set_profile_favorites_switch();
        init_end_now();

        start_hour = start_minute = end_hour = end_minute = -1;

        show_title(profile_id);
    }

    private void set_ring_volume_slider() {
        Slider slider = findViewById(R.id.profile_ring_volume_slider_id);
        TextView textView = findViewById(R.id.profile_new_ring_volume_id);

        slider.setValueFrom(0);
        slider.setStepSize(1);
        slider.setValueTo(Utils.get_ringer_max_volume(context));


        new_ring_volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_RINGER_VOLUME);
        if (new_ring_volume == Utils.PREFERENCE_UNSET) {
            new_ring_volume = Utils.get_ringer_volume(context);
            // in case if user doesn't change anything
            Utils.save_profile_preference(context, profile_id, Utils.NEW_RINGER_VOLUME, new_ring_volume);
        }
        slider.setValue((float)new_ring_volume);

        textView.setText(getString(R.string.profile_new_ring_volume_string, new_ring_volume));

        slider.addOnChangeListener((@NonNull Slider s, float value, boolean fromUser) -> {
            new_ring_volume = (int)value;
            textView.setText(getString(R.string.profile_new_ring_volume_string, new_ring_volume));
        });
    }

    private void set_notification_volume_slider() {
        Slider slider = findViewById(R.id.profile_notification_volume_slider_id);
        TextView textView = findViewById(R.id.profile_new_notification_volume_id);

        slider.setValueFrom(0);
        slider.setStepSize(1);
        slider.setValueTo(Utils.get_notification_max_volume(context));


        new_notification_volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_NOTIFICATION_VOLUME);
        if (new_notification_volume == Utils.PREFERENCE_UNSET) {
            new_notification_volume = Utils.get_notification_volume(context);
            // in case if user doesn't change anything
            Utils.save_profile_preference(context, profile_id, Utils.NEW_NOTIFICATION_VOLUME, new_notification_volume);
        }
        slider.setValue((float)new_notification_volume);

        textView.setText(getString(R.string.profile_new_notification_volume_string, new_notification_volume));

        slider.addOnChangeListener((@NonNull Slider s, float value, boolean fromUser) -> {
            new_notification_volume = (int)value;
            textView.setText(getString(R.string.profile_new_notification_volume_string, new_notification_volume));
        });
    }

    private void set_media_volume_slider() {
        Slider slider = findViewById(R.id.profile_media_volume_slider_id);
        TextView textView = findViewById(R.id.profile_new_media_volume_id);

        slider.setValueFrom(0);
        slider.setStepSize(1);
        slider.setValueTo(Utils.get_media_max_volume(context));

        new_media_volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_MEDIA_VOLUME);
        if (new_media_volume == Utils.PREFERENCE_UNSET) {
            new_media_volume = Utils.get_media_volume(context);
            // in case if user doesn't change anything
            Utils.save_profile_preference(context, profile_id, Utils.NEW_MEDIA_VOLUME, new_media_volume);
        }
        slider.setValue(new_media_volume);

        textView.setText(getString(R.string.profile_new_media_volume_string, new_media_volume));

        slider.addOnChangeListener((@NonNull Slider s, float value, boolean fromUser) -> {
            new_media_volume = (int)value;
            textView.setText(getString(R.string.profile_new_media_volume_string, new_media_volume));
        });
    }

    private void set_profile_favorites_switch() {
        int enable_favorites = Utils.read_profile_preference(context, profile_id, Utils.ENABLE_FAVORITES_INCOMING_CALL);
        if (enable_favorites == Utils.PREFERENCE_UNSET) {
            enable_favorites = Utils.ENABLE_FAVORITES; // enable by default
            Utils.save_profile_preference(context, profile_id, Utils.ENABLE_FAVORITES_INCOMING_CALL, enable_favorites);
            Log.d(TAG, "set_profile_favorites_switch() unset profile = " + profile_id + " enable_favorites = " + enable_favorites);
        }

        SwitchMaterial favorites_switch = findViewById(R.id.profile_favorites_switch);
        favorites_switch.setChecked(enable_favorites == Utils.ENABLE_FAVORITES);
        Log.d(TAG, "set_profile_favorites_switch() enable_favorites = " + enable_favorites);

        favorites_switch.setOnCheckedChangeListener((buttonView, isChecked) -> Utils.save_profile_preference(context, profile_id, Utils.ENABLE_FAVORITES_INCOMING_CALL, isChecked ? Utils.ENABLE_FAVORITES : Utils.DISABLE_FAVORITES));
    }

    void check_to_enable_ok_button() {
        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            if (end_hour != -1 && end_minute != -1) {
                Button ok_button = findViewById(R.id.profile_ok_button);
                ok_button.setEnabled(true);
            }
        } else if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            if (start_hour != -1 && start_minute != -1 && end_hour != -1 && end_minute != -1) {
                Button ok_button = findViewById(R.id.profile_ok_button);
                ok_button.setEnabled(true);
            }
        }
    }

    private void set_view_text(int id, String s) {
        TextView view;

        if (id == R.id.profile_start_time_button) {
            view = findViewById(R.id.profile_start_time_text_id);
        } else if (id == R.id.profile_end_time_button) {
            view = findViewById(R.id.profile_end_time_text_id);
        } else {
            Log.e(TAG, "set_view_text() Invalid button ID");
            return;
        }

        if (view != null)
            view.setText(s);
    }

    private void save_start_end_time(int id, int hour, int minute) {
        if (id == R.id.profile_start_time_button) {
            start_hour = hour;
            start_minute = minute;
        } else if (id == R.id.profile_end_time_button) {
            end_hour = hour;
            end_minute = minute;
        } else {
            Log.e(TAG, "save_start_end_time() Invalid button ID");
            return;
        }
    }

    public void showPeriodTimePickerDialog(View v) {
        final int button_id = v.getId();

        Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        if (profile_id != ConfigPeriodActivity.PERIOD_PROFILE_SINGLE && button_id == R.id.profile_end_time_button) {
            int start_minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_MINUTE);
            if (start_minute != Utils.PREFERENCE_UNSET && ((start_minute + CONVENIENCE_MINUTES) < 59))
                current_minute = start_minute + CONVENIENCE_MINUTES;
        }

        SmartTimePickerDialog timePickerDialog = new SmartTimePickerDialog(ConfigPeriodActivity.this,
                (view, set_hour, set_minute) -> {
                    String formatted = Utils.time_to_ampm(set_hour, set_minute);
                    save_start_end_time(button_id, set_hour, set_minute);
                    set_view_text(button_id, formatted);
                    check_to_enable_ok_button();
                }, current_hour, current_minute, false);
        timePickerDialog.show();
    }

    private static class SmartTimePickerDialog extends TimePickerDialog {
        private int lastHour;

        public SmartTimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute, boolean is24HourView) {
            super(context, listener, hourOfDay, minute, is24HourView);
            this.lastHour = hourOfDay;
        }

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            super.onTimeChanged(view, hourOfDay, minute);

            // Get selected hour in 1-12 format
            int newHour12 = hourOfDay % 12;
            if (newHour12 == 0) newHour12 = 12;

            // Get previous hour in 1-12 format
            int lastHour12 = lastHour % 12;
            if (lastHour12 == 0) lastHour12 = 12;

            // Only act if the hour number actually changed (ignoring AM/PM change)
            if (newHour12 != lastHour12) {
                boolean isPm = hourOfDay >= 12;
                Calendar now = Calendar.getInstance();
                int nowHour24 = now.get(Calendar.HOUR_OF_DAY);
                int nowHour12 = nowHour24 % 12;
                if (nowHour12 == 0) nowHour12 = 12;
                boolean nowIsPm = nowHour24 >= 12;

                boolean shouldBePm;

                // User's requested logic:
                // If chosen hour is smaller than current hour then change AM -> PM or PM -> AM
                // We treat 12 as the "smallest" hour in 12-hour logic (it comes before 1)
                int normalizedNew = (newHour12 == 12) ? 0 : newHour12;
                int normalizedNow = (nowHour12 == 12) ? 0 : nowHour12;

                if (normalizedNew < normalizedNow) {
                    shouldBePm = !nowIsPm;
                } else {
                    shouldBePm = nowIsPm;
                }

                if (shouldBePm != isPm) {
                    int correctedHour24 = (newHour12 % 12) + (shouldBePm ? 12 : 0);
                    updateTime(correctedHour24, minute);
                    this.lastHour = correctedHour24;
                } else {
                    this.lastHour = hourOfDay;
                }
            } else {
                this.lastHour = hourOfDay;
            }
        }
    }

    private void init_end_now() {
        Log.d(TAG, "init_end_now() " + (Utils.new_volume_active(context, profile_id) != Utils.PREFERENCE_UNSET));
        enable_end_now(Utils.new_volume_active(context, profile_id) != Utils.PREFERENCE_UNSET);
    }

    private void enable_end_now(boolean enable) {
        Button end_now = findViewById(R.id.period_profile_end_now_button);
        end_now.setEnabled(enable);
    }

    public void period_activity_end_now(View v) {
        if (v.getId() == R.id.period_profile_end_now_button) {
            Utils.cancel_period_sound_request(context, profile_id, PeriodManager.RESTORE_SOUND_REQUEST);
            Utils.restore_sound_and_update_main_activity(context, profile_id);
            Utils.start_period_worker_end_now_end_time(context, profile_id);
            Log.d(TAG, "period_activity_stop_now() RESTORE_SOUND_REQUEST");
        } else {
            Log.e(TAG, "period_activity_stop_now() Invalid button ID");
        }
    }

    public void period_activity_ok_cancel(View v) {
        int id = v.getId();
        if (id == R.id.profile_ok_button) {
            Utils.save_profile_preference(context, profile_id, Utils.NEW_RINGER_VOLUME, new_ring_volume);
            Utils.save_profile_preference(context, profile_id, Utils.NEW_NOTIFICATION_VOLUME, new_notification_volume);
            Utils.save_profile_preference(context, profile_id, Utils.NEW_MEDIA_VOLUME, new_media_volume);
            if (profile_id > PERIOD_PROFILE_SINGLE) {
                Utils.save_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_HOUR, start_hour);
                Utils.save_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_MINUTE, start_minute);
            }
            Utils.save_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_HOUR, end_hour);
            Utils.save_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_MINUTE, end_minute);

            Intent intent = getIntent();
            intent.putExtra(ConfigPeriodActivity.PERIOD_PROFILE_ID_REPLY, profile_id);
            setResult(RESULT_OK, intent);
            finish();
        } else if (id == R.id.profile_cancel_button) {
            Log.i(TAG, "period_activity_ok_cancel() CANCEL");
            setResult(RESULT_CANCELED);
            finish();
        } else {
            Log.e(TAG, "period_activity_ok_cancel() Invalid button ID");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (alarm_receiver != null)
            unregisterReceiver(alarm_receiver);

        Log.d(TAG, "ConfigPeriodActivity onDestroy()");
    }
}
