package com.example.walkingalarm;

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
        this.alarmDate =  new Date();
    }

    public String getAlarmName(){
        return this.alarmName;
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
