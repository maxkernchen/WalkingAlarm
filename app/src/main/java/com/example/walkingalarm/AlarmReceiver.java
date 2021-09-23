package com.example.walkingalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i("AlarmReciever", "Started Service");
                    context.startForegroundService(new Intent(context, AlarmService.class));
                } else {
                    context.startService(new Intent(context, AlarmService.class));
                }
                break;
            }
            case AlarmFullScreen.FULL_SCREEN_ACTION_ALARM: {
                Log.i("AlarmReciever", "Started FullScreenAlarm");
                int steps =
                        intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                String alarmName =
                        intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                AlarmFullScreen.createFullScreenNotification(context, alarmName, steps);
                Log.i("AlarmReciever", "Done FullScreenAlarm");
                AlarmService.notificationTriggered = true;

                break;
            }
            case AlarmFullScreen.WALK_ACTION: {

                Intent in = new Intent(AlarmFullScreen.WALK_ACTION);
                int steps = intent.getIntExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, -1);
                String alarmTime =  intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);

                in.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS, steps);
                context.sendBroadcast(in);


                break;
            }
            case AlarmFullScreen.DISMISS_ALARM_ACTION: {
                Log.i("AlarmReciever", "Started Dismiss");

                String alarmName = intent.getStringExtra(
                        AlarmFullScreen.INTENT_EXTRA_ALARM_NAME);
                if(AlarmFullScreen.isCreated) {
                    Intent in = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION);
                    in.putExtra(AlarmFullScreen.INTENT_EXTRA_ALARM_NAME, alarmName);
                    context.sendBroadcast(in);
                }
                else{
                    NotificationManagerCompat notificationManager =
                            NotificationManagerCompat.from(context);
                    notificationManager.cancel(AlarmFullScreen.NOTIFICATION_ID_ALARM);
                    notificationManager.deleteNotificationChannel
                            (AlarmFullScreen.CHANNEL_ID + alarmName);
                }

                break;
            }
            case MainActivity.ALARM_SOUND_PICK_ACTION: {
                Log.i("AlarmReciever", "Started Alarm Sound Pick");
                Intent in = new Intent(MainActivity.ALARM_SOUND_PICK_ACTION);

                in.putExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM,
                        intent.getIntExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM, -1));
                context.sendBroadcast(in);
                break;

            }
            case AlarmService.TOAST_MESSAGE_FROM_SERVICE_ACTION: {
                Toast.makeText(context, intent.getStringExtra(AlarmService.TOAST_EXTRA_ALARM_SERVICE
                ), Toast.LENGTH_LONG).show();

                break;
            }
        }
    }
}
