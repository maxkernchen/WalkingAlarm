package com.maxkernchen.walkingalarm;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.maxkernchen.walkingalarm.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Task;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.text.format.DateFormat;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxkernchen.walkingalarm.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

/**
 * MainActivity for our app, will init our AlarmListAdapter and listeners for the floating action
 * button. Will also trigger a google sign in first install of the app.
 *
 * @version 1.0
 * @author Max Kernchen
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Singleton AlarmListAdapter which can be called from other classes, and
     * is central location for storing our list of Alarms.
     */
    private static AlarmListAdapter singletonAlarmListAdapter;
    /**
     * Request code for our google sign in intent
     */
    private final static int GOOGLE_SIGN_IN_REQUEST_CODE = 1;
    /**
     * global var for storing which item has clicked the sound picker.
     */
    public static int currentItemIndexSoundPick = -1;
    /**
     * static bool for check if the system is currently in 24hour time.
     */
    public static boolean is24HourTime = false;
    /**
     * ActivityResultLauncher for the result of the the RingtoneManager intent.
     * This is static to allow for the AlarmListAdapter to call it.
     */
    public static ActivityResultLauncher<Intent> alarmPickerResultLauncher;

    /**
     * OnCreate which will inflate our layout and assign any listeners.
     * At the end will also trigger a google sign intent if the application has been installed
     * for the first time
     * @param savedInstanceState no saved bundles used here
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding =
                ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        // find shared prefs to assign to new AlarmListAdapter
        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.shared_prefs_key),
                Context.MODE_PRIVATE);

        RecyclerView rvAlarmListView = (RecyclerView) findViewById(R.id.alarmListView);
        // disable animations on changes, else it will flash toggle buttons for every update.
        rvAlarmListView.getItemAnimator().setChangeDuration(0);

        AlarmListAdapter alarmListAdapter = new AlarmListAdapter(
                prefs);
        rvAlarmListView.setAdapter(alarmListAdapter);
        rvAlarmListView.setLayoutManager(new LinearLayoutManager(this));


        is24HourTime = DateFormat.is24HourFormat(this);
        // add listener to floating action button which will pop up a TimePickerDialog.
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar cldr = Calendar.getInstance();
                int hour = is24HourTime ? cldr.get(Calendar.HOUR_OF_DAY) : cldr.get(Calendar.HOUR);
                int minutes = cldr.get(Calendar.MINUTE);

                final TimePickerDialog picker = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker tp, int sHour, int sMinute) {
                                boolean addSucceeded = alarmListAdapter.addAlarmItem(
                                        new AlarmItem(sHour, sMinute));
                                if(!addSucceeded){
                                    Toast.makeText(MainActivity.this,
                                            R.string.duplicate_alarm_time_error,
                                            Toast.LENGTH_LONG).show();
                                }

                            }
                        }, hour, minutes, is24HourTime);
                picker.setCanceledOnTouchOutside(false);
                picker.show();
            }
        });
        // handle response from sound picker intent.
        alarmPickerResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            final Uri uri = result.getData().getParcelableExtra
                                    (RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                            updateAlarmSoundForCurrentItem(uri);
                        }
                    }
                });

        singletonAlarmListAdapter = alarmListAdapter;

        // set theme, start service, and sign into google
        this.setThemeOnStart();
        if(!AlarmService.isRunning) {
            Intent myService = new Intent(this, AlarmService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(myService);
            } else {
                startService(myService);
            }
        }
        googleSignIn();
    }

    /**
     * The Uri we found from the RingtoneManager intent result, to be assign to an alarm item
     * @param alarmUri the Uri to assign to a alarm item.
     */
    private void updateAlarmSoundForCurrentItem (Uri alarmUri){
        if(alarmUri != null) {
            Ringtone ringtone = RingtoneManager.getRingtone(this, alarmUri);
            String alarmName = ringtone.getTitle(this);
            singletonAlarmListAdapter.updateAlarmSound(currentItemIndexSoundPick, alarmUri,
                    alarmName);
        }
    }

    /**
     * inflates the menu for settings
     * @param menu Menu to inflate
     * @return - true to display menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * when an item is select from the menu, in this case there is also one item.
     * So it will send us to the SettingsActivity
     * @param item the menu selected
     * @return true to consume event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(MainActivity.this,
                    SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * helper method which is public and static, will allow any class to get the
     * singletonAlarmListAdapter.
     * @return singletonAlarmListAdapter
     */
    public static AlarmListAdapter getAlarmListAdapterInstance(){
        return singletonAlarmListAdapter;
    }

    /**
     * Sign into google fit, we only need read access to the google fit.
     * This will
     */
    private void googleSignIn(){
        final FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this,
                fitnessOptions);

        boolean permissions = GoogleSignIn.hasPermissions(account, fitnessOptions);
        if(!permissions) {
            GoogleSignIn.requestPermissions(this, GOOGLE_SIGN_IN_REQUEST_CODE,
                    account, fitnessOptions);
        }
    }


    /**
     * Set dark or light theme on start up based upon settings.
     */
    private void setThemeOnStart(){
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isDarkMode =
                prefs.getBoolean(SettingsActivity.SettingsFragment.DARK_MODE_KEY, false);
        if(isDarkMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

    }

    /**
     * Overridden onActivityResult which will handle the result from the google sign
     * @param requestCode request code from google sign in
     * @param resultCode result code, not used here
     * @param data data used to get the account from the intent result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GOOGLE_SIGN_IN_REQUEST_CODE){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
            } catch (ApiException e) {
                Toast.makeText(this, R.string.could_not_find_account_error,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}