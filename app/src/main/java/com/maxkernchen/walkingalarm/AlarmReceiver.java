package com.maxkernchen.walkingalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import androidx.core.app.NotificationManagerCompat;

/**
 * AlarmReceiver class which listens to broadcast coming from AlarmService
 * and then sends them to classes with more UI exposure to change elements in real time.
 * Also is used to trigger the alarm service on phone reboot.
 * @version 1.4
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
                context.startForegroundService(new Intent(context, AlarmService.class));
                break;
            }
            // action for triggered the full screen alarm notification.
            case AlarmFullScreen.FULL_SCREEN_ACTION_ALARM: {
                int steps =
                        intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                String alarmName =
                        intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                String alarmChannelID =
                        intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_CHANNEL_ID);

                AlarmFullScreen.createFullScreenNotification(context, alarmName,
                        alarmChannelID, steps);

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
