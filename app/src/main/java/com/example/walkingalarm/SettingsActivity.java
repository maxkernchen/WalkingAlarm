package com.example.walkingalarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        SharedPreferences.OnSharedPreferenceChangeListener listnerSharedPrefs
                = SettingsFragment.createSharedPrefsListener();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(listnerSharedPrefs);

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        private static SharedPreferences.OnSharedPreferenceChangeListener listnerSharedPrefs;
        public final static String STEPS_TO_DISMISS_KEY = "number_of_steps";
        private static EditTextPreference stepsToDismissEditText;
        private final static int MAX_STEPS_LENGTH = 2;
        public final static int MINIMUM_STEPS_TO_DISMISS = 5;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            stepsToDismissEditText = findPreference(STEPS_TO_DISMISS_KEY);
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

            listnerSharedPrefs = createSharedPrefsListener();
        }
        private static android.content.SharedPreferences.OnSharedPreferenceChangeListener
        createSharedPrefsListener() {
            SharedPreferences.OnSharedPreferenceChangeListener listnerSharedPrefs
                    = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key) {
                    if(key.equals(STEPS_TO_DISMISS_KEY)) {
                        String stringSteps = sharedPreferences.getString(key, "");
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

                }
            };

            return listnerSharedPrefs;
        }



    }
}