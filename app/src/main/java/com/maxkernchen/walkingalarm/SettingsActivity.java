package com.maxkernchen.walkingalarm;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


/**
 * SettingsActivity which holds all our settings, right now there are 4:
 *
 * Number of steps required to dismiss alarm
 * Dark theme toggle
 * Vibration on alarm toggle
 * Maximum time in seconds to wait for one step before dismissing the alarm.
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

        SharedPreferences.OnSharedPreferenceChangeListener listenerSharedPress =
                createSharedPrefsListener();
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

        /**
         * Key for the max seconds to wait setting
         */
        public final static String MAX_SECS_TO_WAIT_KEY = "max_secs_to_wait_key";
        // steps to dismiss edit text is stored here for usage on the create listeners and
        // onCreatePreferences
        private EditTextPreference stepsToDismissEditText;
        // seconds to wait edit text is stored here for usage on the create listeners and
        // onCreatePreferences
        private EditTextPreference secondsToWaitForStepEditText;
        // static int which represents the maximum length of the steps to dismiss input field
        // (2 digits)
        private static final int MAX_STEPS_LENGTH = 2;
        // static int which represents the maximum length of the seconds to dismiss setting
        // (3 digits)
        private static final int MAX_SECS_LENGTH = 3;
        // static public int which defines the min amount of steps the steps to dismiss setting
        // should be set to.
        public final static int MINIMUM_STEPS_TO_DISMISS = 5;
        // static public int which defines the maximum amount of time we will wait for at least one
        // step to be walked. This is used to help with delays from Google Fit
        // updating the current steps walked
        public final static int MIN_MAX_SECS_TO_WAIT_FOR_STEPS = 15;
        // The max value of the seconds to wait for steps setting is 120 seconds (2 minutes).
        public final static int MAX_MAX_SECS_TO_WAIT_FOR_STEPS = 120;

        /**
         * Overridden method onCreatePreferences, in this we find the UI elements required
         * and set up our Shared Preference listener
         * @param savedInstanceState bundle not used
         * @param rootKey key for us to create our preferences layout.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            stepsToDismissEditText = findPreference(STEPS_TO_DISMISS_KEY);
            secondsToWaitForStepEditText = findPreference(MAX_SECS_TO_WAIT_KEY);
            // add filter to limit the length of steps to dismiss to 2 digits.
            // and numbers only.
            if(stepsToDismissEditText != null) {
                stepsToDismissEditText.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.selectAll();
                    editText.setFilters(new InputFilter[]{
                            new InputFilter.LengthFilter(MAX_STEPS_LENGTH)});
                });
            }
            if(secondsToWaitForStepEditText != null){
                secondsToWaitForStepEditText.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.selectAll();
                    editText.setFilters(new InputFilter[]{
                            new InputFilter.LengthFilter(MAX_SECS_LENGTH)});
                });
            }

            // create our shared preferences listener and assign it to default shared prefs.

            FragmentActivity activity = getActivity();
            if(activity != null) {
                PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext())
                        .registerOnSharedPreferenceChangeListener(listenerSharedPress);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            FragmentActivity activity = getActivity();
            if(activity != null) {
                PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext())
                        .unregisterOnSharedPreferenceChangeListener(listenerSharedPress);
            }

        }

        /**
         * Helper method to create a shared preference listener.
         * Will check if the value inputted into Steps to Dismiss is at least 5, if its not 5
         * set it to 5. Will also apply dark or light theme immediately on toggle change.
         * @return the OnSharedPreferenceChangeListener to be assigned to SharedPrefs.
         */
        private android.content.SharedPreferences.OnSharedPreferenceChangeListener
        createSharedPrefsListener() {

            SharedPreferences.OnSharedPreferenceChangeListener listenerSharedPrefs
                    = (sharedPreferences, key) -> {

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
                                AppCompatDelegate.setDefaultNightMode
                                        (AppCompatDelegate.MODE_NIGHT_YES);
                            else
                                AppCompatDelegate.setDefaultNightMode
                                        (AppCompatDelegate.MODE_NIGHT_NO);
                        }
                        else if(key.equals(MAX_SECS_TO_WAIT_KEY)){
                            String stringSteps = sharedPreferences.getString(key,
                                    String.valueOf(MIN_MAX_SECS_TO_WAIT_FOR_STEPS));
                            int secsToWaitForSteps = MIN_MAX_SECS_TO_WAIT_FOR_STEPS;
                            try {
                                secsToWaitForSteps = Integer.parseInt(stringSteps);
                            } catch (NumberFormatException nfe) {
                                // set value to 15 in case any invalid options.
                                secondsToWaitForStepEditText.setText(
                                        String.valueOf(MIN_MAX_SECS_TO_WAIT_FOR_STEPS));
                            }
                            // force a minimum of 15 or max of 120 seconds
                            if (secsToWaitForSteps < 15) {
                                secondsToWaitForStepEditText.setText(
                                        String.valueOf(MIN_MAX_SECS_TO_WAIT_FOR_STEPS));
                            }
                            else if(secsToWaitForSteps > 120){
                                secondsToWaitForStepEditText.setText(
                                        String.valueOf(MAX_MAX_SECS_TO_WAIT_FOR_STEPS));
                            }

                        }
                    };
            return listenerSharedPrefs;
        }
    }
}