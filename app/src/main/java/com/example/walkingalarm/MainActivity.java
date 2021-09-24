package com.example.walkingalarm;

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
import androidx.navigation.ui.AppBarConfiguration;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.walkingalarm.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static AlarmListAdapter singletonAlarmListAdapter;

    private final static String LOG_TAG = "MainActivity";
    private final static int GOOGLE_SIGN_IN_REQUEST_CODE = 1;
    public final static String ALARM_SOUND_PICK_ACTION = "AlarmSoundPickAction";



    private int currentItemIndexSoundPick = -1;
    public static boolean is24HourTime = false;

    ActivityResultLauncher<Intent> alarmPickerResultLauncher = registerForActivityResult(
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


    public static GoogleSignInAccount account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        SharedPreferences prefs = this.getSharedPreferences(getString(R.string.shared_prefs_key),
                Context.MODE_PRIVATE);


        RecyclerView rvAlarmListView = (RecyclerView) findViewById(R.id.alarmListView);
        // disable animations on changes, else it will flash toggle buttons for every update.
        rvAlarmListView.getItemAnimator().setChangeDuration(0);

        AlarmListAdapter alarmListAdapter = new AlarmListAdapter(
                prefs);

        rvAlarmListView.setAdapter(alarmListAdapter);
        rvAlarmListView.setLayoutManager(new LinearLayoutManager(this));


        registerReceiver();
        // time picker dialog

        is24HourTime = DateFormat.is24HourFormat(this);


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

        singletonAlarmListAdapter = alarmListAdapter;
        this.setThemeOnStart();
        Intent myService = new Intent(this, AlarmService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           startForegroundService(myService);
        } else {
            startService(myService);
        }
        googleSignIn();

    }

    private void updateAlarmSoundForCurrentItem (Uri alarmUri){
        Ringtone ringtone = RingtoneManager.getRingtone(this, alarmUri);
        String alarmName = ringtone.getTitle(this);
        singletonAlarmListAdapter.updateAlarmSound(currentItemIndexSoundPick, alarmUri,
                alarmName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static AlarmListAdapter getAlarmListAdapterInstance(){
        return singletonAlarmListAdapter;
    }


    public void googleSignIn(){

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

    private void registerReceiver() {
        BroadcastReceiver mainActivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MainActivity.ALARM_SOUND_PICK_ACTION)) {

                    int itemIndex =
                            intent.getIntExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM,
                                    -1);
                    if (itemIndex >= 0) {

                        final Intent ringtone = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                        ringtone.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                        currentItemIndexSoundPick = itemIndex;

                        alarmPickerResultLauncher.launch(ringtone);
                    }

                }


            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ALARM_SOUND_PICK_ACTION);
        registerReceiver(mainActivityReceiver, filter);
    }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GOOGLE_SIGN_IN_REQUEST_CODE){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                account = task.getResult(ApiException.class);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

}