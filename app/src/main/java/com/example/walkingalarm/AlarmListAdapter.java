package com.example.walkingalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AlarmListAdapter extends
        RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmItems;

    private SharedPreferences prefs;


    public AlarmListAdapter(SharedPreferences prefs) {
        this.prefs = prefs;
        this.alarmItems = getAlarmItems(prefs);

    }
    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View alarmListView = inflater.inflate(R.layout.alarm_list, parent, false);
        AlarmViewHolder alarmViewHolder = new AlarmViewHolder(alarmListView);

        return alarmViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarmItem = alarmItems.get(position);
        holder.bind(alarmItem);

        this.saveAlarmItems();
    }



    @Override
    public int getItemCount() {
        return alarmItems.size();
    }

    private void saveAlarmItems(){
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        int i = 0;
        for (AlarmItem item : alarmItems) {
            prefsEditor.putString(String.valueOf(i), gson.toJson(item));
            i++;
        }
        prefsEditor.apply();
    }

    protected static List<AlarmItem> getAlarmItems(SharedPreferences prefs){
        Gson gson = new Gson();
        int i = 0;
        List<AlarmItem> items = new ArrayList<>();
        String jsonItem = prefs.getString(String.valueOf(i), "");
        while(!jsonItem.isEmpty()){
            AlarmItem item = gson.fromJson(jsonItem, AlarmItem.class);
            items.add(item);
            i++;
            jsonItem = prefs.getString(String.valueOf(i), "");
        }

        return items;

    }

    public void addAlarmItem(AlarmItem item){
        alarmItems.add(item);
        // new item is expanded for day of week selection
        alarmItems.get(alarmItems.size() - 1).setExpanded(true);
        notifyItemChanged(alarmItems.size());
    }

    public void deleteAlarmItem(int index){
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.clear();
        prefsEditor.apply();
        alarmItems.remove(index);
        notifyItemRemoved(index);
        //resave to align items.
        saveAlarmItems();

    }

    public boolean triggerAlarm(){
        Calendar now = Calendar.getInstance();
        AlarmItem activeAlarmItem = null;
        boolean alarmTime = false;
        for(AlarmItem item : alarmItems){
            Calendar tempCal = item.getAlarmDate();

            alarmTime = (tempCal.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                    && tempCal.get(Calendar.MINUTE) == now.get(Calendar.MINUTE));

            if(alarmTime && item.isAlarmTriggered()) {
                continue;
            }
            else if(alarmTime && !item.isAlarmTriggered() && item.isActive()){
                item.setAlarmTriggered(true);
                return true;
            }
            else if(!alarmTime && item.isAlarmTriggered()){
                item.setAlarmTriggered(false);
            }
        }
        return false;
    }



    public class AlarmViewHolder extends RecyclerView.ViewHolder {

        private TextView alarmNameTextView;
        private TextView alarmNameSubTextView;
        private SwitchCompat alarmActiveSwitch;
        private LinearLayout subItem;
        private ArrayList<ToggleButton> toggleButtons;
        private View itemView;


        public AlarmViewHolder(View itemView) {

            super(itemView);
            this.itemView = itemView;
            alarmNameTextView = (TextView) itemView.findViewById(R.id.alarm_name);
            alarmActiveSwitch = (SwitchCompat) itemView.findViewById(R.id.alarm_active_switch);
            subItem = (LinearLayout) itemView.findViewById(R.id.sub_alarm_info);
            toggleButtons = this.getToggleDayOfWeeks(itemView);
            this.setUpListeners(this);

        }

        private void bind(AlarmItem alarmItem){

            alarmNameTextView.setText(alarmItem.getAlarmName());


            boolean expanded = alarmItem.isExpanded();
            subItem.setVisibility(expanded ? View.VISIBLE : View.GONE);
            boolean active = alarmItem.isActive();
            alarmActiveSwitch.setChecked(active);
            bindDayOfWeekToggle(alarmItem);


        }

        private ArrayList<ToggleButton> getToggleDayOfWeeks(View itemview){
            ArrayList<ToggleButton> toggleButtons = new ArrayList<>();
            // these are added in same order as java 1.8 DayOfWeek enum starting Monday
            // so we can just directly get the correct toggle.
            toggleButtons.add(itemview.findViewById(R.id.monday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.tuesday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.wednesday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.thursday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.friday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.saturday_toggle));
            toggleButtons.add(itemview.findViewById(R.id.sunday_toggle));

            return toggleButtons;
        }

        private void bindDayOfWeekToggle(AlarmItem alarmItem) {
            HashSet<DayOfWeek> daysOfWeekHashSet = alarmItem.getDaysOfWeek();
            for (int i = 0; i < toggleButtons.size(); i++) {
                toggleButtons.get(i).setChecked(daysOfWeekHashSet.contains(DayOfWeek.of(i + 1)));
            }
        }
        private void setUpListeners(AlarmViewHolder alarmViewHolder){
            MaterialButton btn = (MaterialButton) alarmViewHolder.
                    itemView.findViewById(R.id.alarm_delete_button);
            btn.setOnClickListener(l -> {
                System.out.println(String.valueOf(alarmViewHolder.getAdapterPosition()));
                deleteAlarmItem(alarmViewHolder.getAdapterPosition());
            });

            alarmViewHolder.itemView.setOnClickListener(l -> {
                int position = alarmViewHolder.getAdapterPosition();
                AlarmItem alarmItem = alarmItems.get(position);
                alarmItem.setExpanded(!alarmItem.isExpanded());
                notifyItemChanged(position);
            });
            alarmViewHolder.alarmActiveSwitch.setOnClickListener(l -> {
                int position = alarmViewHolder.getAdapterPosition();
                AlarmItem alarmItem = alarmItems.get(position);
                alarmItem.setActive(!alarmItem.isActive());
                notifyItemChanged(position);
            });
            int i = 1;
            for (ToggleButton toggleButton : toggleButtons){
                final int finalIndex = i;
                toggleButton.setOnClickListener(l -> {
                    int position = alarmViewHolder.getAdapterPosition();
                    AlarmItem alarmItem = alarmItems.get(position);
                    if(toggleButton.isChecked()){
                        alarmItem.addDayOfWeek(DayOfWeek.of(finalIndex));
                    }
                    else{
                        alarmItem.removeDayOfWeek(DayOfWeek.of(finalIndex));
                    }
                    notifyItemChanged(position);
                });
                i++;
            }
        }
    }
}