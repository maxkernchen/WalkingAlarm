package com.maxkernchen.walkingalarm;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.WindowManager;
import android.widget.TextView;

import com.maxkernchen.walkingalarm.databinding.ActivityAlarmFullScreenBinding;

/**
 * Full-Screen activity which will display a users progress towards dismissing the alarm.
 * Half of this class is auto-generated by Android Studio for compatibility between APIs
 * to hide System UI elements.
 *
 * The other half contains a broadcast receiver to dismiss or update the full screen UI elements.
 *
 * @version 1.23
 * @author Max Kernchen
 */
public class AlarmFullScreen extends AppCompatActivity {

    /**
     * public Static String for the Alarm Full Screen Notification Channel
     */
    public static final String CHANNEL_ID = "AlarmFullScreenChannel";
    /**
     * public Static String for the Walking Alarm Notification Channel
     */
    public static final String CHANNEL_NAME = "WalkingAlarmNotificationChannel";
    /**
     * public Static String for the Walking Alarm Notification Channel description.
     */
    public static final String CHANNEL_DESCRIPTION = "WalkingAlarmNotificationChannelDescription";

    /**
     * public Static String for the Alarm Full Screen Action that triggers the notification.
     */
    public static final String FULL_SCREEN_ACTION_ALARM = "AlarmActionFullScreen";
    /**
     * public Static String for the Alarm Full Screen Action that tracks steps walked so far.
     */
    public static final String WALK_ACTION = "AlarmActionWalk";
    /**
     * public Static String for the Alarm Full Screen Action that dismisses the notification.
     */
    public static final String DISMISS_ALARM_ACTION = "AlarmActionDismiss";
    /**
     * public Static String intent extra that stores current steps.
     */
    public static final String INTENT_EXTRA_STEPS = "StepsStringExtra";
    /**
     * public Static String intent extra that stores the alarm name.
     */
    public static final String INTENT_EXTRA_ALARM_NAME = "AlarmNameExtra";
    /**
     * public Static String intent extra that stores the alarm sound uri.
     */
    public static final String INTENT_EXTRA_ALARM_SOUND_URI = "AlarmSoundUriExtra";
    /**
     * public Static String intent extra that stores the alarm sound uri.
     */
    public static final String INTENT_EXTRA_ALARM_VIBRATE_BOOL = "AlarmVibrateBoolExtra";
    /**
     * public Static int intent extra that stores notification id.
     */
    public static final int NOTIFICATION_ID_ALARM = 1;

    /** bool used to monitor when the Full Screen activity is created,
     *  these is always a few second delay between when we find an alarm and the notification
     *  reaches the users. This helps us know when to start monitoring user steps.
     */
    public static boolean isCreated = false;

    /**
     * Static vibration pattern to be used when vibration is enabled.
     */
    public static final long[] VIBRATION_PATTERN = new long[] {500, 1000, 500, 1000, 500, 1000};

    /** BroadcastReceiver for dismissing/updated the full screen activity UI. Called from
     *  AlarmReceiver
     */
    private BroadcastReceiver alarmFullScreenReceiver;

    /**
     * binding for our full screen activity
     */
    private ActivityAlarmFullScreenBinding binding;

    /**
     * Creates activity and also registers broadcast receiver for incoming Intents.
     * Also sets isCreated boolean = true.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAlarmFullScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // set flags so activity is showed when phone is off (on lock screen)
        // and to remove status and action bar.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        registerReceiver();
        isCreated = true;

    }

    @Override
    /**
     * onStop is overridden here to unregister the broadcast receiver.
     */
    public void onStop() {
        super.onStop();
        isCreated = false;
        if (alarmFullScreenReceiver != null) {
            unregisterReceiver(alarmFullScreenReceiver);
        }
    }

    /**
     * define our broadcast receiver, there are three filters.
     *
     * 1. WALK_ACTION -  update the UI with remaining steps to dismiss alarm
     * 2. DISMISS_ALARM_ACTION - dismisses the notification/full screen activity,
     *    goes back to previous activity.
     * 3. FULL_SCREEN_ACTION_ALARM - starts the notification/full screen activity.
     */
    private void registerReceiver() {
        alarmFullScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case AlarmFullScreen.FULL_SCREEN_ACTION_ALARM: {
                        String alarmName = intent.getStringExtra(
                                AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                        int stepsToDismiss = intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS,
                                -1);
                        createFullScreenNotificationAndroidO(context, alarmName, stepsToDismiss);
                        break;
                    }
                    case AlarmFullScreen.WALK_ACTION: {
                        int steps = intent.getIntExtra
                                (AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                        if (steps >= 0) {
                            TextView view = (TextView) findViewById(R.id.fullscreen_content);
                            view.setText(getString(R.string.alarm_steps_remaining, steps));
                        }

                        break;
                    }
                    case AlarmFullScreen.DISMISS_ALARM_ACTION: {
                        String alarmName = intent.getStringExtra(
                                AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                        cancelFullScreenNotification(context, alarmName);
                        break;
                    }
                }

            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmFullScreen.WALK_ACTION);
        filter.addAction(AlarmFullScreen.DISMISS_ALARM_ACTION);
        filter.addAction(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM);
        registerReceiver(alarmFullScreenReceiver, filter);
    }


    /**
     * Cancel the current notification and move the full screen activity to the back.
     * @param context - context sent from AlarmReceiver.
     * @param alarmName - the name of the alarm, used to uniquely make each notification channel
     */
    private void cancelFullScreenNotification(Context context, String alarmName){
        isCreated = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(context);
            notificationManager.cancel(AlarmFullScreen.NOTIFICATION_ID_ALARM);
            notificationManager.deleteNotificationChannel
                    (AlarmFullScreen.CHANNEL_ID + alarmName);
        }
        else {
            // have to start and stop service to remove notification if API < 26
            if(!AlarmService.isRunning)
                context.startService(new Intent(context, AlarmService.class));
        }

        moveTaskToBack(true);
        finish();
    }

    /**
     * Create the notification using context from alarm service.
     * @param context - context from alarm service.
     * @param alarmName - name of alarm to be used on notification text.
     * @param steps steps to dismiss to be used in notification subtext
     */
    public static void createFullScreenNotificationAndroidO(Context context, String alarmName,
                                                            int steps)
    {
        Intent intent = new Intent(context, AlarmFullScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID + alarmName)
                        .setSmallIcon(R.drawable.ic_walk_action_foreground)
                        .setContentTitle(context.getString(R.string.alarm_notification_text,
                                alarmName))
                        .setContentText(context.
                                getString(R.string.alarm_notification_subtext, steps))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setContentIntent(pendingIntent)
                        .setFullScreenIntent(pendingIntent, true)
                        .setAutoCancel(false)
                        .setOngoing(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.
                from(context);
        notificationManager.notify(NOTIFICATION_ID_ALARM, notificationBuilder.build());

    }

    /**
     * createFullScreenNotification similar to above method but for devices with API < 26
     * @param context context from Alarm Receiver
     * @param alarmName String AlarmName to be used in notification text
     * @param soundUri String soundUri to set the notification sound
     * @param steps int steps to be displayed in the notification text
     * @param vibrate boolean to check if vibration should be enabled
     */
    public static void createFullScreenNotification(Context context, String alarmName,
                                                    String soundUri, int steps, boolean vibrate)
    {
        Intent intent = new Intent(context, AlarmFullScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                0);

        Uri alarmSound = Uri.parse(soundUri);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_walk_action_foreground)
                        .setContentTitle(context.getString(R.string.alarm_notification_text,
                                alarmName))
                        .setContentText(context.
                                getString(R.string.alarm_notification_subtext, steps))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setContentIntent(pendingIntent)
                        .setFullScreenIntent(pendingIntent, true)
                        .setSound(alarmSound, AudioManager.STREAM_ALARM)
                        .setOngoing(true);

        if(vibrate)
            notificationBuilder.setVibrate(VIBRATION_PATTERN);

        NotificationManager notificationManager = (NotificationManager)context.
                getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_ALARM, notificationBuilder.build());

    }

}
