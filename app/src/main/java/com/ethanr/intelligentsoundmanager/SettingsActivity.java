package com.ethanr.intelligentsoundmanager;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.preference.SeekBarPreference;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_activity);

        // With edge-to-edge display mandatory on modern Android, the toolbar would
        // otherwise be drawn underneath the status bar (and the content underneath
        // the gesture nav bar), leaving the back arrow behind the status bar where
        // taps don't land on it. Pad the root by the system bar insets instead.
        View root = findViewById(R.id.settings_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(getString(R.string.brightness_header));
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }
    
    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        private static final String TAG = "intelli_sound:" + SettingsFragment.class.getSimpleName();

        @Override
        public void onResume() {
            super.onResume();
            // If the user just granted the "Modify system settings" permission from the
            // system screen and came straight back here, start the service right away
            // instead of waiting for MainActivity to notice. This never launches any
            // screen itself, so it can't create a back-navigation loop.
            SwitchPreferenceCompat enablePreference = findPreference(getString(R.string.pref_key_enable_feature));
            if (enablePreference != null && enablePreference.isChecked()) {
                boolean canWrite = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || Settings.System.canWrite(requireContext());
                Intent serviceIntent = new Intent(requireContext(), BrightnessControlService.class);
                if (canWrite) {
                    ContextCompat.startForegroundService(requireContext(), serviceIntent);
                } else {
                    requireContext().stopService(serviceIntent);
                }
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreferenceCompat enablePreference = findPreference(getString(R.string.pref_key_enable_feature));
            if (enablePreference != null) {
                enablePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (boolean) newValue;
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(requireContext())) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    }
                    Intent serviceIntent = new Intent(requireContext(), BrightnessControlService.class);
                    if (enabled && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(requireContext()))) {
                        ContextCompat.startForegroundService(requireContext(), serviceIntent);
                    } else if (!enabled) {
                        requireContext().stopService(serviceIntent);
                    }
                    return true;
                });
            }

            SeekBarPreference brightnessPreference = findPreference("pref_key_percentage_value");

            if (brightnessPreference != null) {
                brightnessPreference.setOnPreferenceChangeListener(this);
                Log.d(TAG, "Listener for brightness preference set successfully.");

                int initialValue = Utils.read_brightness_threshold(requireContext());
                if (initialValue == Utils.PREFERENCE_UNSET) {
                    initialValue = BrightnessControlService.BRIGHTNESS_THRESHOLD_PERCENTAGE;
                }
                updateBrightnessSummary(brightnessPreference, initialValue);
            } else {
                Log.e(TAG, "SeekBarPreference with key 'pref_key_percentage_value' not found. Check your XML file!");
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (preference.getKey()) {
                case "pref_key_percentage_value":
                    int selectedValue = (int) newValue;

                    // Round to the nearest 10
                    int roundedValue = Math.round(selectedValue / 10.0f) * 10;

                    // If the rounded value is 0, make it 10 to avoid 0% brightness
                    if (roundedValue == 0) {
                        roundedValue = 10;
                    }

                    Log.d(TAG, "onPreferenceChange() Brightness threshold changed to: " + roundedValue);
                    Utils.save_brightness_threshold(requireContext(), roundedValue);

                    // Update the summary with the new value
                    updateBrightnessSummary(preference, roundedValue);
                    return true;
                default:
                    return false;
            }
        }

        private void updateBrightnessSummary(Preference preference, int value) {
            preference.setSummary("Current Threshold: " + value + "%");
        }
    }
}