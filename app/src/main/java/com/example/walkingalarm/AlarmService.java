package com.example.walkingalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnSuccessListener;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AlarmService extends Service {
    private SharedPreferences prefs;
    private SharedPreferences settingsPref;
    private int currentSteps;
    private static final String logTag = "AlarmService";
    private boolean errorFoundDuringAlarm = false;
    private int startingSteps = 0;
    private Calendar alarmStartMonitor;
    private static final int SECONDS_TO_WAIT_FOR_STEPS = 30;
    public static String TOAST_EXTRA_ALARM_SERVICE = "ToastExtraMainActivity";
    public final static String TOAST_MESSAGE_FROM_SERVICE_ACTION = "ToastMessageServiceAction";

    private String currentAlarmName = "";

    public static boolean notificationTriggered = false;
    private boolean foundNotifcation = false;

    private NotificationChannel notificationChannelAlarm;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification notification = setUpNotificationChannelsAndroidO();
            startBackGroundThread();

            startForeground(2, notification);

        } else {

           Notification notification =  setupNotification();
            startBackGroundThread();
            startForeground(1, notification);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private void startBackGroundThread(){
        new Thread(new Runnable() {
            public void run() {

                while(true){

                    List<AlarmItem> items = AlarmListAdapter.getAlarmItemsStatic(prefs);
                    AlarmItem isAlarm = AlarmListAdapter.triggerAlarmStatic(items, prefs);

                    if(isAlarm != null){
                        currentAlarmName = isAlarm.getAlarmName();
                        Log.i("AlarmService", "ALARM TRIGGERED!");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            createAlarmChannelSoundAndroidO(isAlarm.getSoundUri(),
                                    isAlarm.getAlarmName());
                        }
                        else{
                            //TODO older api call.
                        }
                        toFullScreenAlarm(getStepsToDimiss());
                        // wait some time for notification to reach user.
                        sleepMainThread(3000);
                        startingSteps = getCurrentSteps();
                        while(stepsRemainingToDismiss() && !errorFoundDuringAlarm) {
                            sleepMainThread(500);
                        }
                        Log.d(logTag,
                                "Init Steps: " + startingSteps + " Final: " + getCurrentSteps()
                                );
                        // only dismiss for non-errors, error message will pause before exiting.
                        if(!errorFoundDuringAlarm){
                            if(!AlarmFullScreen.isCreated){
                                stepsDismissedToast(getString(R.string.alarm_dismissed_toast));
                            }
                            dismissAlarm();
                        }
                        errorFoundDuringAlarm = false;
                        currentAlarmName = "";
                        notificationTriggered = false;
                        foundNotifcation = false;


                    }
                    Log.i("AlarmService", LocalDateTime.now().toString());
                    sleepMainThread(3000);
                }
            }
        }).start();
    }

    private Notification setUpNotificationChannelsAndroidO() {
            Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "AlarmService";
            String channelName = "Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName,
                    NotificationManager.IMPORTANCE_NONE);

            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                    this, NOTIFICATION_CHANNEL_ID);
            notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();


            this.prefs = getSharedPreferences(getString(R.string.shared_prefs_key),
                    Context.MODE_PRIVATE);
            this.settingsPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            AlarmService temp = this;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            /*if (notificationManager.getNotificationChannel(AlarmFullScreen.CHANNEL_ID) == null) {



                NotificationChannel channel = new NotificationChannel(AlarmFullScreen.CHANNEL_ID,
                        "channel_name", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("channel_description");
                channel.enableVibration(false);


                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();


                Uri alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

                channel.setSound(alarmTone, audioAttributes);

                this.notificationChannelAlarm = channel;
                notificationManager.createNotificationChannel(channel);


            }*/
        }

        return notification;
    }
        private Notification setupNotification(){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("WalkingAlarmServiceRunning")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            Uri alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            builder.setSound(alarmTone);
            Notification notification = builder.build();
            return notification;
        }

    private void toFullScreenAlarm( int steps){
        Intent intent = new Intent(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, currentAlarmName);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
        }

    }

    private void dismissAlarm(){
        Intent intent = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, currentAlarmName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void findCurrentSteps(){

        CountDownLatch latch = new CountDownLatch(1);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
           errorMessageToast(getString(R.string.could_not_find_account_error));
        }
        else {
            Fitness.getHistoryClient(getApplicationContext(), account)
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                        @Override
                        public void onSuccess(DataSet dataSet) {

                            final int stepsInner = dataSet.getDataPoints().get(0).
                                    getValue(Field.FIELD_STEPS).asInt();
                            setCurrentSteps(stepsInner);
                            latch.countDown();

                        }
                    })
                    .addOnFailureListener(e -> {
                    });

            boolean timedOut = false;
            try {
                timedOut = !latch.await(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(timedOut){
                errorMessageToast(getString(R.string.could_not_find_steps_error));
            }

        }

    }

    private int getStepsToDimiss(){
        int stepsToDismiss = SettingsActivity.SettingsFragment.MINIMUM_STEPS_TO_DISMISS;
        String stepsString = settingsPref.
                getString(SettingsActivity.SettingsFragment.STEPS_TO_DISMISS_KEY, "");
        try{
            stepsToDismiss = Integer.parseInt(stepsString);
        }
        catch(NumberFormatException nfe){
            // do nothing here as value is set to default of 5.
        }

        return stepsToDismiss;
    }


    private void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
    }
    public int getCurrentSteps() {
        findCurrentSteps();
        return this.currentSteps;
    }
    public boolean stepsRemainingToDismiss(){
        int stepsToDismiss = getStepsToDimiss();
        int stepsRemaining = stepsToDismiss - (getCurrentSteps() - startingSteps);
        stepsRemaining = Math.max(stepsRemaining, 0);
        updateStepCountAlarmFullScreen(stepsRemaining);

        if(notificationTriggered){
            alarmStartMonitor = Calendar.getInstance();
            alarmStartMonitor.add(Calendar.SECOND, SECONDS_TO_WAIT_FOR_STEPS);
            notificationTriggered = false;
            foundNotifcation = true;
            Log.i(logTag, "Notification Triggered bool true");
        }
        Calendar now = Calendar.getInstance();
        // if after 30 seconds of the notification reaching the user we still
        // have not detected any steps, just dismiss the alarm.
        if(foundNotifcation && now.after(alarmStartMonitor) && stepsRemaining == stepsToDismiss){
            errorMessageToast(getString(R.string.could_not_find_steps_error));
            return false;
        }
        return stepsRemaining > 0;

    }

    private void updateStepCountAlarmFullScreen(int steps){

        Intent intent = new Intent(AlarmFullScreen.WALK_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, currentAlarmName);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void createAlarmChannelSoundAndroidO(String alarmUri, String alarmName){

       final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            NotificationChannel channel = new NotificationChannel(AlarmFullScreen.CHANNEL_ID
                    + alarmName,
                    "channel_name", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("channel_description");
            channel.enableVibration(isVibrationEnabled());
            if(isVibrationEnabled())
                channel.setVibrationPattern(new long[] {500, 1000, 500, 1000, 500, 1000});

            Uri alarmTone = Uri.parse(alarmUri);

            channel.setSound(alarmTone, audioAttributes);

            notificationManager.createNotificationChannel(channel);

        }
    }

    private boolean isVibrationEnabled(){
        return settingsPref.getBoolean(SettingsActivity.SettingsFragment.VIBRATE_KEY,
                false);
    }

    private void sleepMainThread(int milliseconds){
        try{
            Thread.sleep(milliseconds);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

    }
    private void errorMessageToast(String toastText) {

        Intent intent = new Intent(AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE, toastText);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        dismissAlarm();
        this.errorFoundDuringAlarm = true;


    }

    private void stepsDismissedToast(String toastText) {

        Intent intent = new Intent(AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE, toastText);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }



    }

}
