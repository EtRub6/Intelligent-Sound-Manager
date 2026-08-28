package com.ethanr.intelligentsoundmanager;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "intelli_sound:" + MainActivity.class.getSimpleName();
    private Context context;
    private BroadcastReceiver alarm_receiver;
    private static final int ConfigPeriod_REQUEST_ID = 1;
    private static final int ConfigPeriod_Screening_REQUEST_ID = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Edge-to-edge is mandatory on modern Android; without this the toolbar and
        // its overflow menu would be drawn under the status bar and be hard to tap.
        View root = findViewById(R.id.main_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // The app name is presented as the main heading below the toolbar.
        // Suppress the action bar's own title through the ActionBar API itself
        // (not just by blanking the Toolbar's text) so it can't get re-applied
        // by AppCompat and show the app name a second time.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle("");

        // On tablet/desktop-windowing configurations Android can draw its own
        // system caption bar (icon + app name) above the app's content. Hide it
        // here so it doesn't duplicate the heading this screen already shows.
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.hide(WindowInsetsCompat.Type.captionBar());

        context = getApplicationContext();

        request_permissions();


        load_single_active_profile();
        load_periodic_active_profiles();

        register_alarm_receiver();

        Utils.update_favorite_contacts(context);

        requestDoNotDisturbPermissionOrSetDoNotDisturbApi23AndUp();

        sync_brightness_service();

        Log.d(TAG, "onCreate()");
    }


    void register_alarm_receiver() {
        alarm_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int profile_id = intent.getIntExtra(PeriodManager.PROFILE_ID_REQUEST, -1);
                if (profile_id == -1) {
                    Log.e(TAG, "MainActivity.register_alarm_receiver onReceive() unsupported profile_id = " + profile_id);
                    return;
                }

                int update_type = intent.getIntExtra(PeriodManager.SOUND_REQUEST, -1);
                switch (update_type) {
                    case PeriodManager.NEW_SOUND_REQUEST:
                        set_profile_icon(profile_id,false);
                        set_clickable_inactive_profiles(context, profile_id, false);
                        break;
                    case PeriodManager.RESTORE_SOUND_REQUEST:
                        cleanup_completed_worker(profile_id);
                        set_profile_icon(profile_id,true);
                        set_clickable_inactive_profiles(context, profile_id, true);
                        break;
                    default:
                        Log.e(TAG, "onReceive() unsupported update_type = " + update_type);
                }

                Log.d(TAG, "onReceive() update_type = " + update_type);
            }
        };
        registerReceiver(alarm_receiver, new IntentFilter(getPackageName()), RECEIVER_NOT_EXPORTED);
    }

    private void set_clickable_inactive_profiles(Context context, int profile_id, boolean clickable) {
        CardView card;

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            card = findViewById(R.id.periodic_cardView1);
            card.setClickable(clickable); // TODO support more than CARD1
        }
        else if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            card = findViewById(R.id.single_cardView);
            card.setClickable(clickable);
        }
    }

    private void load_single_active_profile() {

        if (Utils.period_profile_worker_active(context, ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) != Utils.PREFERENCE_UNSET) {
            int hour = Utils.read_profile_preference(context, ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, Utils.PERIOD_END_TIME_HOUR);
            int minute = Utils.read_profile_preference(context, ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, Utils.PERIOD_END_TIME_MINUTE);
            String s = Utils.time_to_ampm(hour, minute);
            set_profile_time_text(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, s);

            init_single_active_profile();

            Log.d(TAG, "load_single_profile() is active");
        }
        else
            init_single_profile();

        set_profile_ring_volume_text(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE);
        set_profile_notification_volume_text(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE);
        set_profile_media_volume_text(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE);
    }

    private void init_single_active_profile() {
        set_profile_checkbox_clickable(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, true);
        set_profile_checkbox_checked(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE,true);
        set_profile_icon(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE,false);

        int profile_id = Utils.get_active_profile(context);
        if (profile_id > ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
            set_clickable_inactive_profiles(context, profile_id, false);
    }

    private void init_single_profile() {
        set_profile_checkbox_clickable(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, false);
        set_profile_checkbox_checked(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE, false);
        set_profile_icon(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE,true);
    }


    private void load_and_display_saved_time(int profile_id) {
        int hour, minute;

        if (profile_id <= ConfigPeriodActivity.PERIOD_PROFILE_SINGLE || profile_id >= ConfigPeriodActivity.NPROFILES) {
            Log.e(TAG, "load_and_display_saved_time() unsupported profile = " + profile_id);
            return;
        }

        hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_HOUR);
        if (hour != Utils.PREFERENCE_UNSET) {
            minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_MINUTE);
            String formatted = Utils.time_to_ampm(hour, minute);

            hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_HOUR);
            minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_MINUTE);

            set_profile_time_text(profile_id, formatted + " - " + Utils.time_to_ampm(hour, minute));
        }
        else Log.e(TAG, "load_and_display_saved_time() start time is supposed to be set");
    }

    private void load_periodic_active_profiles() {
        int i;

        for (i = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE + 1; i < ConfigPeriodActivity.NPROFILES; i++) {
            set_profile_checkbox_clickable(i, Utils.period_profile_worker_active(context, i) != Utils.PREFERENCE_UNSET);
            set_profile_checkbox_checked(i,Utils.period_profile_worker_active(context, i) != Utils.PREFERENCE_UNSET);

            if (Utils.period_profile_worker_active(context, i) == Utils.PREFERENCE_UNSET) {
                set_profile_icon(i,true);
                Log.d(TAG, "load_periodic_active_profiles() not active profile_id = " + i);
            }
            else {
                load_and_display_saved_time(i);
                set_profile_icon(i, Utils.new_volume_active(context, i) == Utils.PREFERENCE_UNSET);
                set_clickable_inactive_profiles(context, i, false);
                Log.d(TAG, "load_periodic_active_profiles() active profile_id = " + i);
            }

            set_profile_ring_volume_text(i);
            set_profile_notification_volume_text(i);
            set_profile_media_volume_text(i);
        }

    }

    private void set_profile_checkbox_clickable(int profile_id, boolean status) {
        CheckBox check_box;

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                check_box = findViewById(R.id.single_checkBox);
                break;
            case 1:
                check_box = findViewById(R.id.periodic_checkBox1);
                break;
            default:
                Log.e(TAG, "set_profile_checkbox_clickable() unsupported profile = " + profile_id);
                return;
        }

        if (check_box != null)
            check_box.setClickable(status);
    }

    private void set_profile_icon(int profile_id, boolean status) {
        ImageView profile_icon;

        Log.d(TAG, "set_profile_icon() profile_id = " + profile_id + " status = " + status);

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                profile_icon = findViewById(R.id.single_icon);
                break;
            case ConfigPeriodActivity.PERIOD_PROFILE_1:
                profile_icon = findViewById(R.id.periodic1_icon);
                break;
            default:
                Log.e(TAG, "set_profile_icon() unsupported profile = " + profile_id);
                return;
        }

        if (profile_icon != null)
            profile_icon.setImageResource(status ? R.drawable.ring_on : R.drawable.ring_off);
    }

    private void set_profile_checkbox_checked(int profile_id, boolean status) {
        CheckBox check_box;

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                check_box = findViewById(R.id.single_checkBox);
                break;
            case ConfigPeriodActivity.PERIOD_PROFILE_1:
                check_box = findViewById(R.id.periodic_checkBox1);
                break;
            default:
                Log.e(TAG, "set_profile_checkbox_checked() unsupported profile = " + profile_id);
                return;
        }

        check_box.setChecked(status);
    }

    private void requestDoNotDisturbPermissionOrSetDoNotDisturbApi23AndUp() {
        NotificationManager n = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (!n.isNotificationPolicyAccessGranted()) {
            // Ask the user to grant access
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    private void request_permissions() {
        java.util.ArrayList<String> missingPermissions = new java.util.ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_CONTACTS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_CALL_LOG);
        }

        if (!missingPermissions.isEmpty()) {
            requestPermissions(missingPermissions.toArray(new String[0]), 100);
        }
    }

    private boolean isBrightnessControlEnabled() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_enable_feature), false);
    }

    private void sync_brightness_service() {
        if (!isBrightnessControlEnabled()) {
            stopService(new Intent(this, BrightnessControlService.class));
            return;
        }

        // Note: if the "Modify system settings" permission hasn't been granted yet,
        // we deliberately do NOT jump to the system permission screen from here.
        // That request already happens once, explicitly, when the user turns the
        // feature on from Settings. Re-launching it every time this activity resumes
        // (e.g. right after the user presses back out of that very screen) used to
        // throw the user straight back into it, making the back button look broken.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            Log.w(TAG, "sync_brightness_service() feature enabled but WRITE_SETTINGS not granted; " +
                    "not starting the service. Grant permission from Settings to enable it.");
            return;
        }

        Intent serviceIntent = new Intent(this, BrightnessControlService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (context != null) {
            sync_brightness_service();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (alarm_receiver != null)
            unregisterReceiver(alarm_receiver);

        Utils.commit_preferences(context);
        Log.d(TAG, "MainActivity onDestroy()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void schedule_period_worker(int profile_id) {
        Utils.start_period_worker(context, profile_id);

        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            int end_hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_HOUR);
            int end_minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_MINUTE);
            String time = Utils.time_to_ampm(end_hour, end_minute);
            Utils.update_widget_enable(context, true, time);
        }
    }


    private void cleanup_completed_worker(int profile_id) {
        // no need to cleanup if periodic profile (by definition), only if cancelled
        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE)
            cleanup_single_completed(profile_id);
    }

    private void cleanup_single_completed(int profile_id) {
        if (profile_id != ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            Log.e(TAG, "cleanup_single_completed unsupported profile_id = " + profile_id);
            return;
        }

        init_single_profile();
        set_profile_time_text(profile_id, "");
    }

    private void start_period(int profile_id) {
        if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
            Utils.clean_initial_and_set_new_sound(context, profile_id);
            set_profile_icon(profile_id, false); // only for single profile
        }
        else if (Utils.periodic_start_immediately(context, profile_id))
            Utils.clean_initial_and_set_new_sound(context, profile_id);

        Log.i(TAG, "start_period() starting period worker for profile = " + profile_id);
        schedule_period_worker(profile_id);
    }

    private void cancel_period(int profile_id) {
        cleanup_completed_worker(profile_id);

        if (Utils.period_profile_worker_active(context, profile_id) != Utils.PREFERENCE_UNSET) {
            set_profile_icon(profile_id, true);
            set_profile_time_text(profile_id, "");
        }

        Utils.restore_sound_and_cancel_period_and_update_widget(context, profile_id);
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox)view).isChecked();

        switch (view.getId()) {
            case R.id.single_checkBox:
                if (checked)
                    start_period(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE);
                else
                    cancel_period(ConfigPeriodActivity.PERIOD_PROFILE_SINGLE);
                break;
            case R.id.periodic_checkBox1:
                if (checked)
                    start_period(ConfigPeriodActivity.PERIOD_PROFILE_1);
                else
                    cancel_period(ConfigPeriodActivity.PERIOD_PROFILE_1);
                break;
            default:
                Log.e(TAG, "onCheckboxClicked() Invalid checkbox");
                break;
        }
    }

    void show_error_dialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(message).setTitle(title);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void clear_preferences_menu() {
        for (int i = 0; i < ConfigPeriodActivity.NPROFILES; i++) {
            if (Utils.period_profile_worker_active(context, i) != Utils.PREFERENCE_UNSET) {
                cancel_period(i);
                Log.d(TAG, "clear_preferences_menu() found active alarm for RESTORE_SOUND_REQUEST, profile_id = " + i);
            }
        }

        Utils.clear_preferences(context);
        Utils.clearDefaultPreferences(context);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings_id) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_debug_id) {
            Intent intent = new Intent(MainActivity.this, DebugActivity.class);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.clear_preferences_id) {
                clear_preferences_menu();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void openConfigPeriodActivity(View v) {
        int profile_id;
        int id = v.getId();

        if (id == R.id.single_cardView) {
            profile_id = ConfigPeriodActivity.PERIOD_PROFILE_SINGLE;
        }
        else if (id == R.id.periodic_cardView1) {
            profile_id = ConfigPeriodActivity.PERIOD_PROFILE_1;
        }
        else {
            Log.e(TAG, "openConfigPeriodActivity() unsupported profile_id");
            return;
        }

        Intent intent = new Intent(this, ConfigPeriodActivity.class);
        intent.putExtra(ConfigPeriodActivity.PERIOD_PROFILE_ID_REQUEST, profile_id);
        startActivityForResult(intent, ConfigPeriod_REQUEST_ID);
    }

    private void set_profile_time_text(int profile_id, String s) {
        TextView view;

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                view = findViewById(R.id.single_textView);
                break;
            case 1:
                view = findViewById(R.id.periodic_textView1);
                break;
            default:
                return;
        }

        if (view != null) {
            view.setText(s);
            Log.d(TAG, "set_profile_time() " + s);
        }
        else
            Log.e(TAG, "set_profile_time_id() view is null");
    }

    private void set_profile_ring_volume_text(int profile_id) {
        TextView view;
        int volume;

        volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_RINGER_VOLUME);
        if (volume == Utils.PREFERENCE_UNSET)
            volume = Utils.get_ringer_volume(context);

        String s = String.valueOf(volume);

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                view = findViewById(R.id.single_ring_volume);
                break;
            case 1:
                view = findViewById(R.id.periodic_ring_volume1);
                break;
            default:
                return;
        }

        if (view != null)
            view.setText(s);
        else
            Log.e(TAG, "set_profile_ring_volume() view is null");
    }

    private void set_profile_notification_volume_text(int profile_id) {
        TextView view;
        int volume;

        volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_NOTIFICATION_VOLUME);
        if (volume == Utils.PREFERENCE_UNSET)
            volume = Utils.get_notification_volume(context);

        String s = String.valueOf(volume);

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                view = findViewById(R.id.single_notification_volume);
                break;
            case 1:
                view = findViewById(R.id.periodic_notification_volume1);
                break;
            default:
                return;
        }

        if (view != null)
            view.setText(s);
        else
            Log.e(TAG, "set_profile_notification_volume() view is null");
    }

    private void set_profile_media_volume_text(int profile_id) {
        TextView view;
        int volume;

        volume = Utils.read_profile_preference(context, profile_id, Utils.NEW_MEDIA_VOLUME);
        if (volume == Utils.PREFERENCE_UNSET)
            volume = Utils.get_media_volume(context);

        String s = String.valueOf(volume);

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                view = findViewById(R.id.single_media_volume);
                break;
            case 1:
                view = findViewById(R.id.periodic_media_volume1);
                break;
            default:
                return;
        }

        if (view != null)
            view.setText(s);
        else
            Log.e(TAG, "set_profile_media_volume() view is null");
    }

    private void schedule_period_worker_if_checked(int profile_id) {
        CheckBox check_box;

        switch (profile_id) {
            case ConfigPeriodActivity.PERIOD_PROFILE_SINGLE:
                check_box = findViewById(R.id.single_checkBox);
                break;
            case 1:
                check_box = findViewById(R.id.periodic_checkBox1);
                break;
            default:
                return;
        }

        if (check_box.isChecked()) {
            Utils.cancel_period(context, profile_id);
            Utils.restore_sound(context, profile_id);
            Utils.set_period_worker_inactive_preference(context, profile_id);

            start_period(profile_id);
            Log.d(TAG, "schedule_period_worker_if_checked() is_checked");
        }
    }

    // This method is called when the second activity finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ConfigPeriod_REQUEST_ID) {
            if (resultCode == RESULT_OK) {
                int profile_id = data.getIntExtra(ConfigPeriodActivity.PERIOD_PROFILE_ID_REQUEST, 0);
                if (profile_id < ConfigPeriodActivity.PERIOD_PROFILE_SINGLE || profile_id >= ConfigPeriodActivity.NPROFILES) {
                    Log.e(TAG, "onActivityResult() unsupported profile_id");
                    return;
                }

                set_profile_ring_volume_text(profile_id);
                set_profile_notification_volume_text(profile_id);
                set_profile_media_volume_text(profile_id);

                int end_hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_HOUR);
                int end_minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_END_TIME_MINUTE);
                if (end_hour == Utils.PREFERENCE_UNSET) {
                    set_profile_checkbox_clickable(profile_id, false);
                    Log.e(TAG, "onActivityResult() end hour must be set, set end hour to proceed further, profile_id = " + profile_id);
                    return;
                }

                if (profile_id == ConfigPeriodActivity.PERIOD_PROFILE_SINGLE) {
                    set_profile_checkbox_clickable(profile_id, true);
                    set_profile_time_text(profile_id, Utils.time_to_ampm(end_hour, end_minute));
                    schedule_period_worker_if_checked(profile_id);
                }
                else {
                    int start_hour = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_HOUR);
                    int start_minute = Utils.read_profile_preference(context, profile_id, Utils.PERIOD_START_TIME_MINUTE);
                    if (start_hour == Utils.PREFERENCE_UNSET) {
                        set_profile_checkbox_clickable(profile_id, false);
                        Log.e(TAG, "onActivityResult() start hour must be set, set start hour to proceed further, profile_id = " + profile_id);
                        return;
                    }
                    set_profile_time_text(profile_id, Utils.time_to_ampm(start_hour, start_minute) + " - " + Utils.time_to_ampm(end_hour, end_minute));
                    set_profile_checkbox_clickable(profile_id, true);

                    schedule_period_worker_if_checked(profile_id);
                }
            }
            else if (resultCode == RESULT_CANCELED)
                Log.d(TAG, "onActivityResult() REQUEST RESULT_CANCELED");
            else Log.e(TAG, "onActivityResult() REQUEST neither RESULT_OK nor RESULT_CANCELED = " + requestCode);
        }
        else if (requestCode == ConfigPeriod_Screening_REQUEST_ID) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult() app is now the call screening app");
            } else {
                Log.d(TAG, "onActivityResult() is not the call screening app");
            }
        }
    }


}
