package com.example.walkingalarm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationManagerCompat;

/**
 * AlarmReceiver class which listens to broadcast coming from AlarmService
 * and then sends them to classes with more UI exposure to change elements in real time.
 * Also is used to trigger the alarm service on phone reboot.
 * @version 1.0
 * @author Max Kernchen
 */


public class AlarmReceiver extends BroadcastReceiver {

    private static final String logTag = "AlarmReceiver";

    /**
     * Overridden onReceive which allows us to call specific methods based on Intent Actions
     * @param context Context which is passed along to other classes
     * @param intent intent from the calling class
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            // once the phone is rebooted we want to start the alarm service.
            case Intent.ACTION_BOOT_COMPLETED: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, AlarmService.class));
                } else {
                    context.startService(new Intent(context, AlarmService.class));
                }
                break;
            }
            // action for triggered the full screen alarm notification.
            case AlarmFullScreen.FULL_SCREEN_ACTION_ALARM: {
                int steps =
                        intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                String alarmName =
                        intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                String soundUri = intent.getStringExtra(AlarmFullScreen.
                        INTENT_EXTRA_ALARM_SOUND_URI);
                boolean vibrate = intent.getBooleanExtra(AlarmFullScreen.
                        INTENT_EXTRA_ALARM_VIBRATE_BOOL, false);
                // based on build version there are two ways to create a full screen notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AlarmFullScreen.createFullScreenNotificationAndroidO(context, alarmName, steps);
                }
                else{
                    AlarmFullScreen.createFullScreenNotification(context, alarmName, soundUri,
                            steps, vibrate);
                }
                AlarmService.notificationTriggered = true;
                break;
            }
            // action which update the steps remaining to walk in the full screen activity
            case AlarmFullScreen.WALK_ACTION: {
                Intent in = new Intent(AlarmFullScreen.WALK_ACTION);
                int steps = intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                in.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
                context.sendBroadcast(in);
                break;
            }
            // Action to dismiss the alarm. If the AlarmFullScreen activity has not yet been
            // created, then just cancel it here rather than in the AlarmFullScreen class.
            case AlarmFullScreen.DISMISS_ALARM_ACTION: {
                String alarmName = intent.getStringExtra(
                        AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (AlarmFullScreen.isCreated) {
                        Intent in = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION);
                        in.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, alarmName);
                        context.sendBroadcast(in);
                    }
                    else {
                        NotificationManagerCompat notificationManager =
                                NotificationManagerCompat.from(context);
                        notificationManager.cancel(AlarmFullScreen.NOTIFICATION_ID_ALARM);
                        notificationManager.deleteNotificationChannel
                                (AlarmFullScreen.CHANNEL_ID + alarmName);
                    }
                }
                else {
                    // wait a little bit for service to stop, this is needed in API < 26
                    // to dismiss notifications triggered by a Service class.
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (AlarmFullScreen.isCreated) {
                        Intent in = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION);
                        in.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, alarmName);
                        context.sendBroadcast(in);
                    }
                    else{
                        // start service again API < 26 needs to stop service to cancel
                        // notifications.

                        if(!AlarmService.isRunning)
                            context.startService(new Intent(context, AlarmService.class));
                    }


                }

                break;
            }
            // action for picking the alarm sound from the main activity.
            case MainActivity.ALARM_SOUND_PICK_ACTION: {
                Intent in = new Intent(MainActivity.ALARM_SOUND_PICK_ACTION);
                in.putExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM,
                        intent.getIntExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM, -1));
                context.sendBroadcast(in);
                break;
            }
            // this allows us to send a toast from the service class, usually for error messages.
            case AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION: {
                Toast.makeText(context, intent.getStringExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE
                ), Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
}
