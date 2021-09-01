package com.example.walkingalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Received Reboot event", Toast.LENGTH_LONG).show();
        Log.i("AlarmReciever", "Boot completed");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i("AlarmReciever", "Started Service");

            context.startForegroundService(new Intent(context, AlarmService.class));
        } else {
            context.startService(new Intent(context, AlarmService.class));
        }
    }
}
