package com.maxkernchen.walkingalarm;

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
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AlarmService class runs continuously  in the background to check for new alarms to be triggered.
 * Once they are triggered it calls google cloud API for current google fit steps.
 * Requires wake lock when testing with Google Pixel 4a, foreground service does eventually
 * not execute due to doze mode. Wake Lock keeps the service running every polling interval.
 * @version 1.4
 * @author Max Kernchen
 */
public class AlarmService extends Service {

    // shared preferences used to store AlarmItems
    private SharedPreferences prefs;
    // shared preference used to store settings.
    private SharedPreferences settingsPref;
    // current steps that the user has walked.
    private int currentSteps;
    // bool to check if any error occurred during the current alarm.
    private boolean errorFoundDuringAlarm = false;
    // int to store the amount of steps right when the alarm is triggered.
    private int startingSteps = 0;
    // Calendar used to check if the we find no steps after a certain amount of time
    private Calendar alarmStartMonitor;
    // Calendar used to check if we find some steps but not all to dismiss.
    // This is used to eventually dismiss the alarm so it does not hang forever.
    private Calendar alarmTimeOutMonitor;
    // seconds to wait before dismissing alarm due to no steps found.
    private static final int SECONDS_TO_WAIT_FOR_STEPS = 45;
    // how often we check if a new alarm needs to be triggered in milliseconds
    private static final int POLLING_FREQUENCY_MS = 3000;
    // how long to wait in ms for GOOGLE FIT API call to complete
    private static final int GOOGLE_FIT_FETCH_TIMEOUT = 10000;
    // how often we call the google fit API to check for current steps.
    private static final int STEPS_POLLING_FREQUENCY_MS = 500;
    // Extra static string for the extra to see a toast to the AlarmReceiver.
    public static final String TOAST_EXTRA_ALARM_SERVICE = "ToastExtraMainActivity";
    // Action which will display toast message from AlarmReceiver.
    public final static String TOAST_MESSAGE_FROM_SERVICE_ACTION = "ToastMessageServiceAction";
    // Constant string which is for the required notification that a service is running.
    public final static String WALKING_ALARM_RUNNING = "Walking Alarm is Running in Background";
    // log tag for logging.
    private static final String logTag = "AlarmService";
    // current name of the alarm.
    private String currentAlarmName = "";
    // current alarm sound to be played
    private String currentAlarmSoundUri = "";
    // public boolean which checks if we have started the notification.
    public static boolean notificationTriggered = false;
    // private boolean that is used if we found the notification is running but has not timed
    // out yet (no steps found after SECONDS_TO_WAIT_FOR_STEPS).
    private boolean foundNotification = false;
    // static bool that is used to check if service is running
    public static boolean isRunning = false;
    // private wake lock to prevent service from being slept during doze mode.
    private PowerManager.WakeLock wakeLock;
    // wake lock tag for AlarmService
    private static final String WAKE_LOCK_TAG_ALARM_SERVICE = "AlarmService:WakeLock";
    // notification channel id for the service notification
    private static final String NOTIFICATION_CHANNEL_ID = "AlarmServiceNotificationChannel";
    // notification channel name for the service notification
    private static final String NOTIFICATION_CHANNEL_NAME = "AlarmServiceNotificationName";
    // current unique id we will use for notification channel id, this will be a GUID
    // to allow us to change the sound/vibration attributes when creating the notification
    private String currentAlarmChannelID;

    /**
     * On start of the service make sure we start based upon API level.
     * Newer API levels require a notification to show the app is running.
     * @param intent - intent to start, from Alarm Receiver or Main Activity
     * @param flags any flags sent
     * @param startId the unique id for this start request
     * @return START_STICKY - meaning the service will be restarted if it was stopped due to low
     * memory. Once additional memory has been found.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        this.prefs = getSharedPreferences(getString(R.string.shared_prefs_key),
                Context.MODE_PRIVATE);
        this.settingsPref = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG_ALARM_SERVICE);
        wakeLock.acquire();

        Notification notification = setUpNotificationChannels();
        startForeground(2, notification);
        startBackGroundThread();
        return START_STICKY;
    }

    /**
     * onBind is not implemented as instead of creating comm channels, I send broadcast intents
     * to AlarmReceiver. Also the service does not require any external class calls once it is
     * started.
     * @param intent intent on binding
     * @return nothing currently
     */
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Overridden onDestroy is only used for API < 26.
     * As those API levels require us to stop a service to full dismiss a notification.
     * Service is started about again about 5 seconds later.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        NotificationManager nMgr = (NotificationManager)this.
                getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(AlarmFullScreen.NOTIFICATION_ID_ALARM);
        isRunning = false;
    }

    /**
     * Helper method which will start our background thread that continuously checks for
     * any alarm that need to be triggered. Currently checks every 3 seconds for a new alarm, to
     * prevent battery drain.
     */
    private void startBackGroundThread(){
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (wakeLock.isHeld()) {
                        // get the alarm items using static preferences
                        List<AlarmItem> items = AlarmListAdapter.getAlarmItemsStatic(prefs);
                        AlarmItem isAlarm = AlarmListAdapter.triggerAlarmStatic(items, prefs);

                        if (isAlarm != null) {
                            currentAlarmName = isAlarm.getAlarmName();
                            // use random UUID for channel id, this allows us to
                            // apply changes to a repeating alarm if the sound or vibration settings
                            // were changed. 
                            currentAlarmChannelID = java.util.UUID.randomUUID().toString();
                            currentAlarmSoundUri = isAlarm.getAlarmSoundUri();
                            createAlarmChannelSound();
                            toFullScreenAlarm(getStepsToDismiss());
                            // wait some time for notification to reach user.
                            sleepMainThread(POLLING_FREQUENCY_MS);

                            startingSteps = getCurrentSteps();
                            while (stepsRemainingToDismiss() && !errorFoundDuringAlarm) {
                                sleepMainThread(STEPS_POLLING_FREQUENCY_MS);
                            }

                            // only dismiss for non-errors, error message will pause before exiting.
                            if (!errorFoundDuringAlarm) {
                                // if full screen activity has not been created, then just
                                // print a toast to let user know the alarm has been dismissed.
                                if (!AlarmFullScreen.isCreated) {
                                    stepsDismissedToast(getString(R.string.alarm_dismissed_toast));
                                }
                                dismissAlarm();
                            }
                            errorFoundDuringAlarm = false;
                            currentAlarmName = "";
                            notificationTriggered = false;
                            foundNotification = false;

                        }
                        sleepMainThread(POLLING_FREQUENCY_MS);

                    }
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();
                        run();
                    }
                } catch (Exception e) {
                    if (!wakeLock.isHeld()) {
                        wakeLock.acquire();
                    }
                    run();
                }
            }
        }).start();
    }

    /**
     * Setup a notification channel, this is only needed for API >= 26.
     * This channel is for the always active notification that is required when running a foreground
     * service
     * @return the notification to be set to the service.
     */
    private Notification setUpNotificationChannels() {
        Notification notification;

        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, NOTIFICATION_CHANNEL_ID);

        notification = notificationBuilder.setOngoing(true)
                .setContentTitle(WALKING_ALARM_RUNNING)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        return notification;
    }

    /**
     * Create a notification channel for API >= 26 which also has a sound linked to it.
     * This sound is pulled from AlarmItem and is set globally once an Alarm is triggered.
     * Will also assign vibration to the channel if allowed in settings.
     */
    private void createAlarmChannelSound(){

        final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationChannel channel = new NotificationChannel(AlarmFullScreen.CHANNEL_ID
                + currentAlarmChannelID,
                AlarmFullScreen.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(AlarmFullScreen.CHANNEL_DESCRIPTION);
        channel.enableVibration(isVibrationEnabled());

        if(isVibrationEnabled())
            channel.setVibrationPattern(AlarmFullScreen.VIBRATION_PATTERN);

        Uri alarmTone = Uri.parse(currentAlarmSoundUri);
        channel.setSound(alarmTone, audioAttributes);
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Will send intent to broadcast receiver to trigger the full screen notification
     * @param steps steps which are required to dismiss the alarm, added to notification text
     */
    private void toFullScreenAlarm(int steps){
        Intent intent = new Intent(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM, null,
                this, AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, currentAlarmName);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_CHANNEL_ID, currentAlarmChannelID);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
        // sound URI and vibrate is only used for API < 26 calls.
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_SOUND_URI, currentAlarmSoundUri);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_VIBRATE_BOOL, isVibrationEnabled());

        int pendingFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        else {
            pendingFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                pendingFlag);

        // need to use alarm manager to send intent, else it might be ignore if device is in
        // deep sleep
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setAndAllowWhileIdle
                    (AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
        }


    }

    /**
     * Helper method which will dismiss the alarm by sending an intent to the AlarmReceiver.
     * AlarmReceiver will cancel any notification channels or service based on API level.
     *
     */
    private void dismissAlarm(){
        Intent intent = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_CHANNEL_ID, currentAlarmChannelID);
        int pendingFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        else {
            pendingFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                pendingFlag);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method which finds the current steps from Google Fit Api.
     * Is called every 500ms. CountDownLatch to await response from Google Fit async call.
     * Usually this call is very fast, but if there is no await this method could finish before
     * current steps is set.
     */
    private void findCurrentSteps(){
        CountDownLatch latch = new CountDownLatch(1);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account == null){
           errorMessageToast(getString(R.string.could_not_find_account_error));
        }
        else {
            Fitness.getHistoryClient(getApplicationContext(), account)
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(dataSet -> {
                        // get just current steps for today we will compare to previous fetch.
                        if(dataSet.getDataPoints().size() > 0) {
                            final int stepsInner = dataSet.getDataPoints().get(0).
                                    getValue(Field.FIELD_STEPS).asInt();
                            setCurrentSteps(stepsInner);
                            //TODO remove this
                            Toast.makeText(this, "steps walked " + stepsInner, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            // a specific scenario is that the user has not moved their phone
                            // since midnight. In that case the first alarm of the day
                            // would have no steps. So we instead of quitting, we assign zero
                            // steps. If this always stays at zero, eventually we should reach
                            // SECONDS_TO_WAIT_FOR_STEPS which will dismiss the alarm.
                            setCurrentSteps(0);
                        }
                        // latch is now okay to release and method can finish.
                        latch.countDown();

                    })
                    .addOnFailureListener(e -> {
                        // do nothing allow for latch to timeout which will print error message
                        // and dismiss alarm
                        //TODO remove this
                        e.printStackTrace();
                    });

            boolean timedOut = false;
            try {
                timedOut = !latch.await(GOOGLE_FIT_FETCH_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                errorMessageToast(getString(R.string.could_not_find_steps_error));
            }
            if(timedOut){
                errorMessageToast(getString(R.string.could_not_find_steps_error));
            }
        }
    }

    /**
     * Get the number of steps we need to dismiss the alarm, default value of 5.
     * @return the number of steps to dismiss from settings or default of 5.
     */
    private int getStepsToDismiss(){
        int stepsToDismiss = SettingsActivity.SettingsFragment.MINIMUM_STEPS_TO_DISMISS;
        String stepsString = settingsPref.
                getString(SettingsActivity.SettingsFragment.STEPS_TO_DISMISS_KEY, "5");
        try{
            stepsToDismiss = Integer.parseInt(stepsString);
        }
        catch(NumberFormatException nfe){
            // do nothing here as value is set to default of 5.
        }
        return stepsToDismiss;
    }

    /**
     * set current steps, helper method for findCurrentSteps as we cannot set the value within
     * a lambda function.
     * @param currentSteps current steps to set to global variable.
     */
    private void setCurrentSteps(int currentSteps) {
        this.currentSteps = currentSteps;
    }

    /**
     * get current steps by calling find current steps first and then returning the global variable
     * @return current steps as found by google fit API.
     */
    private int getCurrentSteps() {
        findCurrentSteps();
        return this.currentSteps;
    }

    /**
     * Find if there are any steps remaining to dismiss, will only start checking if
     * the notification has reached the user, this usually takes a few seconds.
     * If the user has seen the notification but after SECONDS_TO_WAIT_FOR_STEPS
     * seconds no steps have been detected,
     * we will dismiss the alarm.
     * @return true if steps are left to dismiss alarm, false if no more steps are needed.
     */
    private boolean stepsRemainingToDismiss(){
        int stepsToDismiss = getStepsToDismiss();
        int stepsRemaining = stepsToDismiss - (getCurrentSteps() - startingSteps);
        stepsRemaining = Math.max(stepsRemaining, 0);
        updateStepCountAlarmFullScreen(stepsRemaining);

        if(notificationTriggered){
            alarmStartMonitor = Calendar.getInstance();
            alarmTimeOutMonitor = Calendar.getInstance();
            alarmStartMonitor.add(Calendar.SECOND, SECONDS_TO_WAIT_FOR_STEPS);
            // wait double to amount of time to dismiss if we find some steps but not all
            alarmTimeOutMonitor.add(Calendar.SECOND, SECONDS_TO_WAIT_FOR_STEPS * 2);
            notificationTriggered = false;
            foundNotification = true;
        }
        Calendar now = Calendar.getInstance();
        // if after SECONDS_TO_WAIT_FOR_STEPS seconds of the notification reaching the user we still
        // have not detected any steps, just dismiss the alarm.
        if(foundNotification && now.after(alarmStartMonitor) && stepsRemaining == stepsToDismiss){
            errorMessageToast(getString(R.string.could_not_find_steps_error));
            return false;
        }
        // if we walked some steps but not all also dismiss the alarm eventually, else it will
        // stick around forever
        else if(foundNotification && now.after(alarmTimeOutMonitor)){
            errorMessageToast(getString(R.string.alarm_not_dismissed_in_time));
            return false;
        }
        return stepsRemaining > 0;
    }

    /**
     * Send an intent to the AlarmReceiver with the steps remaining to dismiss the alarm.
     * This will allow us to update the AlarmFullScreen activity in real time with remaining steps
     * the user has to walk.
     * @param steps the steps remaining to dismiss the alarm
     */
    private void updateStepCountAlarmFullScreen(int steps){

        Intent intent = new Intent(AlarmFullScreen.WALK_ACTION, null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
        intent.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, currentAlarmName);
        int pendingFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        else {
            pendingFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                pendingFlag);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method which gets the vibration setting from the SettingsActivity
     * @return true or false if vibration is enabled.
     */
    private boolean isVibrationEnabled(){
        return settingsPref.getBoolean(SettingsActivity.SettingsFragment.VIBRATE_KEY,
                false);
    }

    /**
     * Helper method which sleeps main thread, used in main loop for service
     * @param milliseconds how long to sleep
     */
    private void sleepMainThread(int milliseconds){
        try{
            Thread.sleep(milliseconds);
        } catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    /**
     * Helper method which will toast an input string, and then also dismiss the alarm.
     * Any calls to this method are assumed to be errors.
     * @param toastText the message to toast
     */
    private void errorMessageToast(String toastText) {

        Intent intent = new Intent(AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION, null,
                this, AlarmReceiver.class);
        intent.putExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE, toastText);
        int pendingFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        else {
            pendingFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                pendingFlag);

        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

        // wait a little for the notification to reach user, before dismissing alarm
        sleepMainThread(POLLING_FREQUENCY_MS);
        dismissAlarm();
        this.errorFoundDuringAlarm = true;

    }

    /**
     * Helper method which will send a toast message to the user that they walked enough steps
     * to dismiss the alarm. This is only used if the user did not tap the notification, as a
     * indicator that the alarm is dismissed. This is not to be used for error messages.
     * @param toastText the message we will toast to the user.
     */
    private void stepsDismissedToast(String toastText) {
        Intent intent = new Intent(AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION,
                null, this,
                AlarmReceiver.class);
        intent.putExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE, toastText);

        int pendingFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            pendingFlag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        }
        else {
            pendingFlag =  PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                pendingFlag);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

    }

}
