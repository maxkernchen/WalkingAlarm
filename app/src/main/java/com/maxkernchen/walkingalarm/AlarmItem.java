package com.maxkernchen.walkingalarm;
import android.media.RingtoneManager;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
/**
 * AlarmItem class holds the object representation of each row in the app.
 * List of this class is stored in AlarmListAdapter.
 *
 * @version 1.4
 * @author Max Kernchen
 */

public class AlarmItem {

    // Calendar for storing when alarm should be triggered, we only care about hour/minute
    private Calendar alarmDate;
    // for tracking if the use has expanded the item in the list.
    private boolean expanded;
    // if the alarm is active, connected to switch button.
    private boolean active;
    // which days of week the alarm is active for, connected toggle buttons.
    private HashSet<DayOfWeek> daysOfWeek;
    /// the name of the alarm sound that should be played when it is triggered.
    private String alarmSoundName;
    // static string for displaying the default alarm sound option.
    private final static String DEFAULT_ALARM_SOUND_NAME = "Default Alarm Sound";
    // Uri for when the user changes from the default alarm sound.
    private String alarmSoundUri;
    // if the alarm has been triggered
    private boolean alarmTriggered;

    /**
     * Constructor for Alarm Item, will take hour/minute in to be set for Alarm Item alarm time.
     * Assumes hour is in 24 hour time, due to being sent from TimePicker android dialog.
     * @param hour hour of the alarm, assumed to be 24 hour time
     * @param minute minute of the alarm.
     */
    public AlarmItem(int hour, int minute){
        this.expanded = false;
        this.active = true;
        this.alarmDate = Calendar.getInstance();
        alarmDate.set(Calendar.HOUR_OF_DAY, hour);
        alarmDate.set(Calendar.MINUTE, minute);
        alarmDate.set(Calendar.SECOND, 0);
        alarmDate.set(Calendar.MILLISECOND, 0);
        //according to javadocs above set methods only take effect after calling getter.
        //so a call to getTime is needed here.
        alarmDate.getTime();
        this.daysOfWeek = new HashSet<>();
        // add current day of week
        daysOfWeek.add(LocalDate.now().getDayOfWeek());
        this.alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        this.alarmSoundName = DEFAULT_ALARM_SOUND_NAME;
    }

    /**
     * gets a String representation of the time the Alarm will be triggered
     * @return - returns the string representation of the Alarm Time, will be in either 24 hour or
     * 12 hour time based on system settings.
     */
    public String getAlarmName(){
        String alarmName;
        if(MainActivity.is24HourTime){
            alarmName = new SimpleDateFormat("HH:mm", Locale.ENGLISH).
                    format(alarmDate.getTime()).toString();
        }
        else{
            alarmName = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).
                    format(alarmDate.getTime()).toString();
        }
        return alarmName;
    }

    /**
     * Set the expanded boolean, changed whenever use clicks on blank area of each alarm item.
     * @param setExpansion the boolean to set expansion to.
     */
    public void setExpanded(boolean setExpansion){
        this.expanded = setExpansion;
    }

    /**
     *
     * @return the expanded boolean
     */
    public boolean isExpanded(){
        return this.expanded;
    }

    /**
     *
     * @return if the alarm is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set the active boolean
     * @param active boolean to set the active to.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     *
     * @return Calendar alarmDate.
     */
    public Calendar getAlarmDate() {
        return alarmDate;
    }

    /**
     * Remove a day of the week
     * @param toRemove the DayOfWeek to remove
     */
    public void removeDayOfWeek(DayOfWeek toRemove){
        daysOfWeek.remove(toRemove);
    }

    /**
     * Add a day of the week
     * @param toAdd the DayOfWeek to add.
     */
    public void addDayOfWeek(DayOfWeek toAdd){
        daysOfWeek.add(toAdd);
    }

    /**
     * @return daysOfWeek Hashset.
     */
    public HashSet<DayOfWeek> getDaysOfWeek(){
        return daysOfWeek;
    }

    /**
     *
     * @return if the alarm has been triggered. alarmTriggered boolean
     */
    public boolean isAlarmTriggered() {
        return alarmTriggered;
    }

    /**
     *
     * @param alarmTriggered boolean to set alarmTriggered to.
     */
    public void setAlarmTriggered(boolean alarmTriggered) {
        this.alarmTriggered = alarmTriggered;
    }

    /**
     * Update the alarm sound and name
     * @param newAlarmSound the sound of the alarm in Uri format
     * @param newAlarmName the title of the alarm
     */
    public void updateAlarmSound(String newAlarmSound, String newAlarmName){
        this.alarmSoundUri = newAlarmSound;
        this.alarmSoundName = newAlarmName;
    }

    /**
     *
     * @return String alarm sound name
     */
    public String getAlarmSoundName() {
        return alarmSoundName;
    }
    /**
     *
     * @return String alarmoundUri
     */
    public String getAlarmSoundUri() {
        return alarmSoundUri;
    }

    /**
     * Overridden equals which consider an alarm item with the same alarm time to be equal.
     * Used to prevent duplicate adds
     * @param obj AlarmItem object to compare to this object
     * @return if the two objects are equal.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof AlarmItem))
            return false;

        AlarmItem objAlarm = (AlarmItem) obj;
        return (this.getAlarmDate().get(Calendar.HOUR_OF_DAY) ==
                objAlarm.getAlarmDate().get(Calendar.HOUR_OF_DAY) &&
                this.getAlarmDate().get(Calendar.MINUTE) ==
                        objAlarm.getAlarmDate().get(Calendar.MINUTE));
    }

}
