package com.example.walkingalarm;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmItem {

    private String alarmName;

    private Date alarmDate;
    private boolean expanded;
    private boolean active;

    public AlarmItem(String alarmName){
        this.alarmName = alarmName;
        this.expanded = false;
        this.active = true;
        this.alarmDate =  convertToDate(alarmName);
    }

    private Date convertToDate(String alarmName){
        Date converted = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
        try {
             converted = sdf.parse(alarmName);
        } catch (ParseException e) {
            //TODO how to handle this, dont think this exception will ever happen.
            e.printStackTrace();
        }

        return converted;
    }

    public String getAlarmName(){
        return new SimpleDateFormat("K:mm a").format(alarmDate).toString();
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

    public Date getAlarmDate() {
        return alarmDate;
    }


    public String alarmDateToString() {
        return alarmDate.toString();
    }
}
