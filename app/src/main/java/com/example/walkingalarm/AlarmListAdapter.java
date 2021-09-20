package com.example.walkingalarm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    public static final String INTENT_EXTRA_INDEX_ITEM = "ExtraAlarmItemIndex";


    public AlarmListAdapter(SharedPreferences prefs) {
        this.prefs = prefs;
        this.alarmItems = getAlarmItemsStatic(prefs);

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
        // get current items in shared prefs, this is because the service changes shared prefs
        // statically, and we need to persist the isAlarmTriggered bool between UI changes.
        List<AlarmItem> currentItemSharedPrefs = getAlarmItemsStatic(prefs);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();

        for (int i = 0; i < alarmItems.size(); i++) {
            AlarmItem tempItem = alarmItems.get(i);
            int indexPrefs = currentItemSharedPrefs.indexOf(tempItem);
            if(indexPrefs >= 0)
                tempItem.setAlarmTriggered(currentItemSharedPrefs.get(indexPrefs).isAlarmTriggered());
            prefsEditor.putString(String.valueOf(i), gson.toJson(tempItem));

        }
        prefsEditor.apply();
    }


    private static void saveAlarmItemsStatic(List<AlarmItem> itemsStatic, SharedPreferences prefs){
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        int i = 0;
        for (AlarmItem item : itemsStatic) {
            prefsEditor.putString(String.valueOf(i), gson.toJson(item));
            i++;
        }
        prefsEditor.apply();
    }

    protected static List<AlarmItem> getAlarmItemsStatic(SharedPreferences prefs){
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

    public void updateAlarmSound(int index, Uri alarmUri, String alarmName){
        AlarmItem item = alarmItems.get(index);
        item.updateAlarmSound(alarmUri.toString(), alarmName);
        notifyItemChanged(index);
    }

    public boolean addAlarmItem(AlarmItem item){
        if(checkForDuplicateAlarmItem(item))
            return false;

        alarmItems.add(item);
        // new item is expanded for day of week selection
        alarmItems.get(alarmItems.size() - 1).setExpanded(true);
        notifyItemChanged(alarmItems.size());

        return true;
    }
    private boolean checkForDuplicateAlarmItem(AlarmItem item){
        for (AlarmItem itemLoop: this.alarmItems) {
            if(itemLoop.equals(item))
                return true;
        }
        return false;
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



    public static AlarmItem triggerAlarmStatic(List<AlarmItem> staticItems, SharedPreferences prefs){
        Calendar now = Calendar.getInstance();
        LocalDate nowDate = LocalDate.now();
        boolean alarmTime = false;
        for(AlarmItem item : staticItems){
            Calendar tempCal = item.getAlarmDate();
            HashSet<DayOfWeek> daysOfWeek = item.getDaysOfWeek();


            alarmTime = (tempCal.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                    && tempCal.get(Calendar.MINUTE) == now.get(Calendar.MINUTE) &&
                    daysOfWeek.contains(nowDate.getDayOfWeek()));

          if(alarmTime && !item.isAlarmTriggered() && item.isActive()){
                item.setAlarmTriggered(true);
                AlarmListAdapter.saveAlarmItemsStatic(staticItems, prefs);
                return item;
            }
            else if(!alarmTime && item.isAlarmTriggered()){
                item.setAlarmTriggered(false);
                AlarmListAdapter.saveAlarmItemsStatic(staticItems, prefs);

          }
        }
        return null;
    }






    public class AlarmViewHolder extends RecyclerView.ViewHolder {

        private TextView alarmNameTextView;
        private TextView alarmNameSubTextView;
        private SwitchCompat alarmActiveSwitch;
        private LinearLayout subItem;
        private ArrayList<ToggleButton> toggleButtons;
        private View itemView;
        private Button alarmSoundPicker;


        public AlarmViewHolder(View itemView) {

            super(itemView);
            this.itemView = itemView;
            alarmNameTextView = (TextView) itemView.findViewById(R.id.alarm_name);
            alarmActiveSwitch = (SwitchCompat) itemView.findViewById(R.id.alarm_active_switch);
            subItem = (LinearLayout) itemView.findViewById(R.id.sub_alarm_info);
            toggleButtons = this.getToggleDayOfWeeks(itemView);
            alarmSoundPicker = (Button) itemView.findViewById(R.id.alarm_select_sound);
            this.setUpListeners(this);

        }

        private void bind(AlarmItem alarmItem){

            alarmNameTextView.setText(alarmItem.getAlarmName());
            boolean expanded = alarmItem.isExpanded();
            subItem.setVisibility(expanded ? View.VISIBLE : View.GONE);
            boolean active = alarmItem.isActive();
            alarmActiveSwitch.setChecked(active);
            bindDayOfWeekToggle(alarmItem);
            alarmSoundPicker.setText(alarmItem.getAlarmSoundName());


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

            alarmSoundPicker.setOnClickListener(l -> {
                Intent intent = new Intent(MainActivity.ALARM_SOUND_PICK_ACTION, null, itemView.getContext(),
                        AlarmReceiver.class);
                int position = alarmViewHolder.getAdapterPosition();
                intent.putExtra(AlarmListAdapter.INTENT_EXTRA_INDEX_ITEM, position);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(itemView.getContext(), 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            });
        }


    }
}