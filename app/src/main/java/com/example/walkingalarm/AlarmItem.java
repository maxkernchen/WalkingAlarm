package com.example.walkingalarm;


import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.HashSet;

public class AlarmItem {

    private String alarmName;

    private Calendar alarmDate;
    private boolean expanded;
    private boolean active;
    private HashSet<DayOfWeek> daysOfWeek;
    private String alarmSoundName;



    private String soundUri;




    private boolean alarmTriggered;



    public AlarmItem(int hour, int minute){
        this.expanded = false;
        this.active = true;
        this.alarmDate =  Calendar.getInstance();
        alarmDate.set(Calendar.HOUR_OF_DAY, hour);
        alarmDate.set(Calendar.MINUTE, minute);
        alarmDate.set(Calendar.SECOND, 0);
        alarmDate.set(Calendar.MILLISECOND, 0);
        //according to javadocs above set methods only take affect after calling getter.
        //so a call to getTime is needed here.
        alarmDate.getTime();
        // TODO: add logic that if alarm created is exactly current time, trigger it the next
        // TODO: day. Similar to android default clock app

        this.daysOfWeek = new HashSet<>();
        this.alarmName = this.getAlarmName();
        // add current day of week
        daysOfWeek.add(LocalDate.now().getDayOfWeek());
        this.soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();

        this.alarmSoundName = "Default Alarm Sound";
    }

    private LocalTime convertToDate(int hour, int minute){
        return LocalTime.of(hour,minute);
    }

    public String getAlarmName(){
        return new SimpleDateFormat("K:mm a").format(alarmDate.getTime()).toString();
    }

    public void setExpanded(boolean setExpansion){
        this.expanded = setExpansion;
    }
    public boolean isExpanded(){
        return this.expanded;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
;
    public Calendar getAlarmDate() {
        return alarmDate;
    }


    public String alarmDateToString() {
        return alarmDate.toString();
    }

    public void removeDayOfWeek(DayOfWeek toRemove){
        daysOfWeek.remove(toRemove);
    }
    public void addDayOfWeek(DayOfWeek toAdd){
        daysOfWeek.add(toAdd);
    }
    public HashSet<DayOfWeek> getDaysOfWeek(){
        return daysOfWeek;
    }

    public boolean isAlarmTriggered() {
        return alarmTriggered;
    }

    public void setAlarmTriggered(boolean alarmTriggered) {
        this.alarmTriggered = alarmTriggered;
    }

    public void updateAlarmSound(String newAlarmSound, String newAlarmName){
        this.soundUri = newAlarmSound;
        this.alarmSoundName = newAlarmName;
    }

    public String getAlarmSoundName() {
        return alarmSoundName;
    }

    public String getSoundUri() {
        return soundUri;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null)
            return false;
        AlarmItem objAlarm = (AlarmItem) obj;
        return (this.getAlarmDate().get(Calendar.HOUR_OF_DAY) ==
                objAlarm.getAlarmDate().get(Calendar.HOUR_OF_DAY) &&
                this.getAlarmDate().get(Calendar.MINUTE) ==
                        objAlarm.getAlarmDate().get(Calendar.MINUTE));
    }

}
