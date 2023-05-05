package com.maxkernchen.walkingalarm;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


/**
 * SettingsActivity which holds all our settings, right now there are 3:
 *
 * Number of steps required to dismiss alarm
 * Dark theme toggle
 * Vibration on alarm toggle
 *
 *  @version 1.4
 *  @author Max Kernchen
 */
public class SettingsActivity extends AppCompatActivity {

    /**
     * Overridden onCreate which will find shared preferences and create the layout.
     * We also a assign a shared preferences listener to validate and change the app on the fly.
     * @param savedInstanceState bundle for storing previous states.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


    }

    /**
     * Overridden onOptionsItemSelected when an item in our settings menu is selected.
     * Only change here is to add back button listener
     * @param item the item selected
     * @return true to consume action here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            // allow us to go back to main activity from settings activity
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    /**
     * Inner static SettingsFragment class which will store our settings and create a SharedPrefs
     * Listener to validate any setting changes or apply them on the fly (dark mode).
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        /**
         * Key for the steps to dismiss setting
         */
        public final static String STEPS_TO_DISMISS_KEY = "number_of_steps_setting_key";
        /**
         * Key for the dark mode setting
         */
        public final static String DARK_MODE_KEY = "dark_mode_setting_key";
        /**
         * Key for the Vibration setting
         */
        public final static String VIBRATE_KEY = "vibration_setting_key";
        // steps to dismiss edit text is stored here for usage on the create listeners and
        // onCreatePreferences
        private EditTextPreference stepsToDismissEditText;
        // static int which represents the maximum length of the steps to dismiss input field
        // (2 digits)
        private static final int MAX_STEPS_LENGTH = 2;
        // static public int which defines the min amount of steps the steps to dismiss setting
        // should be set to.
        public final static int MINIMUM_STEPS_TO_DISMISS = 5;

        /**
         * Overrideen method onCreatePreferences, in this we find the UI elements required
         * and set up our Shared Preference listener
         * @param savedInstanceState bundle not used
         * @param rootKey key for us to create our preferences layout.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            stepsToDismissEditText = findPreference(STEPS_TO_DISMISS_KEY);
            // add filter to limit the length of steps to dismiss to 2 digits.
            // and numbers only.
            stepsToDismissEditText.setOnBindEditTextListener(new EditTextPreference.
                    OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.selectAll();
                    editText.setFilters(new InputFilter[]{
                            new InputFilter.LengthFilter(MAX_STEPS_LENGTH)});
                }
            });

            // create our shared preferences listener and assign it to default shared prefs.
            SharedPreferences.OnSharedPreferenceChangeListener listnerSharedPrefs =
                    createSharedPrefsListener();

            PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
                    .registerOnSharedPreferenceChangeListener(listnerSharedPrefs);

        }

        /**
         * Helper method to create a shared preference listener.
         * Will check if the value inputted into Steps to Dismiss is at least 5, if its not 5
         * set it to 5. Will also apply dark or light theme immediately on toggle change.
         * @return the OnSharedPreferenceChangeListener to be assigned to SharedPrefs.
         */
        private android.content.SharedPreferences.OnSharedPreferenceChangeListener
        createSharedPrefsListener() {

            SharedPreferences.OnSharedPreferenceChangeListener listnerSharedPrefs
                    = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key) {

                    if(key.equals(STEPS_TO_DISMISS_KEY)) {
                        String stringSteps = sharedPreferences.getString(key,
                                String.valueOf(MINIMUM_STEPS_TO_DISMISS));
                        int steps = MINIMUM_STEPS_TO_DISMISS;
                        try {
                            steps = Integer.parseInt(stringSteps);
                        } catch (NumberFormatException nfe) {
                            // set value to 5 in case any invalid options.
                            stepsToDismissEditText.setText(
                                    String.valueOf(MINIMUM_STEPS_TO_DISMISS));
                        }
                        // force a minimum of 5.
                        if (steps < 5) {
                            stepsToDismissEditText.setText(
                                    String.valueOf(MINIMUM_STEPS_TO_DISMISS));
                        }
                    }
                    else if(key.equals(DARK_MODE_KEY)){
                        boolean isDarkMode = sharedPreferences.getBoolean(key, false);
                        if(isDarkMode)
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        else
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                }
            };
            return listnerSharedPrefs;
        }


    }
}