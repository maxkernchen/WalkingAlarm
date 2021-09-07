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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDateTime;
import java.util.List;

public class AlarmService extends Service {
    private SharedPreferences prefs;
    public AlarmService(){

    }

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
                        Log.i("AlarmService", "ALARM TRIGGERED!");
                        toFullScreenAlarm(isAlarm.getAlarmName());

                    }
                    Log.i("AlarmService", LocalDateTime.now().toString());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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

            Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();






            this.prefs = getSharedPreferences(getString(R.string.shared_prefs_key),
                    Context.MODE_PRIVATE);
            AlarmService temp = this;

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            if (notificationManager.getNotificationChannel(AlarmFullScreen.CHANNEL_ID) == null) {



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
                notificationManager.createNotificationChannel(channel);


               /* Intent intent = new Intent(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM,
                        null, this, AlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                        0);

                NotificationCompat.Builder alarmBuilder = new NotificationCompat.Builder(
                        this,AlarmFullScreen.CHANNEL_ID );
                notification = notificationBuilder.setOngoing(true)
                        .setContentTitle("Alarm Triggered!")
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setContentIntent(pendingIntent)
                        .build();*/

            }
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

    private void toFullScreenAlarm(String alarmTime){
        Intent intent = new Intent(AlarmFullScreen.FULL_SCREEN_ACTION_ALARM, null, this,
                AlarmReceiver.class);
        intent.putExtra("AlarmTime", alarmTime);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
        }


    }

}
