package com.example.walkingalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlarmReciever", "Received Broadcast for action: " + intent.getAction());
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.i("AlarmReciever", "Started Service");
                context.startForegroundService(new Intent(context, AlarmService.class));
            } else {
                context.startService(new Intent(context, AlarmService.class));
            }
        }
        else if(intent.getAction().equals(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM)){
            Log.i("AlarmReciever", "Started FullScreenAlarm");

            AlarmFullScreen.createFullScreenNotification(context, (String)
                    intent.getExtras().get("AlarmTime"));
        }
        else if(intent.getAction().equals(AlarmFullScreen.WALK_ACTION)){
            Log.i("AlarmReciever", "Started StepsFullScreen");

            Intent in = new Intent(AlarmFullScreen.WALK_ACTION);
            in.putExtra(AlarmFullScreen.INTENT_EXTRA_STEPS,
                    intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_STEPS));
            context.sendBroadcast(in);
        }
        else if (intent.getAction().equals(AlarmFullScreen.DISMISS_ALARM_ACTION)){
            Log.i("AlarmReciever", "Started Dismiss");

            Intent in = new Intent(AlarmFullScreen.DISMISS_ALARM_ACTION);
            context.sendBroadcast(in);

        }
        else if(intent.getAction().equals(AlarmFullScreen.ERROR_TOAST_ACTION)){
            Log.i("AlarmReciever", "Started Error Toast");

            Intent in = new Intent(AlarmFullScreen.ERROR_TOAST_ACTION);
            in.putExtra(AlarmFullScreen.INTENT_EXTRA_TOAST_ERROR,
                    intent.getStringExtra(AlarmFullScreen.INTENT_EXTRA_TOAST_ERROR));
            context.sendBroadcast(in);
        }
    }
}
