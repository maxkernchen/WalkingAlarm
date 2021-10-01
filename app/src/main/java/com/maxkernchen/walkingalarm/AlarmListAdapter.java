package com.maxkernchen.walkingalarm;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.maxkernchen.walkingalarm.R;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/**
 * AlarmListAdapter class which extends RecyclerView.Adapter.
 * Link to alarm_list layout, holds a list of alarmItems and saves/updates
 * them to SharedPreferences.
 *
 * @version 1.0
 * @author Max Kernchen
 */


public class AlarmListAdapter extends
        RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {
    // list of alarmItems which represent each alarm.
    private List<AlarmItem> alarmItems;
    //SharedPreferences for storing alarm items using GSON.
    private SharedPreferences prefs;
    // static string for the index of the alarm item we are updating.
    public static final String INTENT_EXTRA_INDEX_ITEM = "ExtraAlarmItemIndex";

    /**
     * Constructor for AlarmListAdapter will set the shared prefs and get current items stored.
     * @param prefs SharedPreferences that we use to store alarm items.
     */
    public AlarmListAdapter(SharedPreferences prefs) {
        this.prefs = prefs;
        this.alarmItems = getAlarmItemsStatic(prefs);
    }

    /**
     * onCreateViewHolder Overridden method, will assign layout as R.layout.alarm_list
     * and bind alarmitems to AlarmViewHolder inner class.
     * @param parent parent view group
     * @param viewType viewType int.
     * @return
     */
    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View alarmListView = inflater.inflate(R.layout.alarm_list, parent, false);
        AlarmViewHolder alarmViewHolder = new AlarmViewHolder(alarmListView);

        return alarmViewHolder;
    }

    /**
     * onBindViewHolder overridden, will bind the current alarm item and save all changes.
     * @param holder the holder class which we will bind the alarm item to.
     * @param position the position which was changed.
     */
    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarmItem = alarmItems.get(position);
        holder.bind(alarmItem);
        this.saveAlarmItems();
    }

    /**
     * Use alarmItems to get the amount of items in the RecycleView.
     * @return size of alarmItems array
     */
    @Override
    public int getItemCount() {
        return alarmItems.size();
    }

    /**
     * Save all alarm items by using shared preferences and GSON.
     */
    private void saveAlarmItems(){
        // get current items in shared prefs, this is because the service changes shared prefs
        // statically, and we need to persist the isAlarmTriggered bool between UI changes.
        List<AlarmItem> currentItemSharedPrefs = getAlarmItemsStatic(prefs);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        Gson gson = new Gson();
        for (int i = 0; i < alarmItems.size(); i++) {
            AlarmItem tempItem = alarmItems.get(i);
            int indexPrefs = currentItemSharedPrefs.indexOf(tempItem);
            // set alarm triggered to what it should be currently stored in the static shared prefs.
            if(indexPrefs >= 0)
                tempItem.setAlarmTriggered(currentItemSharedPrefs.get(indexPrefs).
                        isAlarmTriggered());
            prefsEditor.putString(String.valueOf(i), gson.toJson(tempItem));

        }
        prefsEditor.apply();
    }

    /**
     * save alarm items statically, this is called by the AlarmService class.
     * @param itemsStatic a list of items to save
     * @param prefs the shared preferences to save the items to.
     */
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

    /**
     * Get alarm items from a passed in SharedPreferences statically
     * @param prefs the preferences to get the alarm items from
     * @return List of Alarm Items from shared preferences.
     */
    public static List<AlarmItem> getAlarmItemsStatic(SharedPreferences prefs){
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

    /**
     * Update the alarm sound and make sure changes are bound.
     * @param index index of alarm item to update
     * @param alarmUri Uri SoundUri we need to play
     * @param alarmName String alarmName of the sound to play
     */
    public void updateAlarmSound(int index, Uri alarmUri, String alarmName){
        AlarmItem item = alarmItems.get(index);
        item.updateAlarmSound(alarmUri.toString(), alarmName);
        notifyItemChanged(index);
    }

    /**
     * Add a new alarm item, check for duplicates
     * @param item AlarmItem to add to the list
     * @return false if the alarm item is a duplicate, else true.
     */
    public boolean addAlarmItem(AlarmItem item){
        if(checkForDuplicateAlarmItem(item))
            return false;

        alarmItems.add(item);
        // new item is expanded for day of week selection
        alarmItems.get(alarmItems.size() - 1).setExpanded(true);
        notifyItemChanged(alarmItems.size());

        return true;
    }

    /**
     * Helper method to check for any duplicate alarm items before adding.
     * @param item alarm item we want to add.
     * @return True if passed in AlarmItem is a duplicate, else false.
     */
    private boolean checkForDuplicateAlarmItem(AlarmItem item){
        for (AlarmItem itemLoop: this.alarmItems) {
            if(itemLoop.equals(item))
                return true;
        }
        return false;
    }

    /**
     * Delete an alarm item, also removes it from shared preferences.
     * @param index index of AlarmItem to remove.
     */
    public void deleteAlarmItem(int index){
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.clear();
        prefsEditor.apply();
        alarmItems.remove(index);
        notifyItemRemoved(index);
        //resave to align items.
        saveAlarmItems();

    }

    /**
     * Method called by alarm service to check if any alarm are ready to be triggered.
     * If one is found, return it.
     * @param staticItems items to check for if any are triggered
     * @param prefs preferences to save changes when an alarm is triggered
     * @return AlarmItem if Alarm is ready to be triggered, else null.
     */
    public static AlarmItem triggerAlarmStatic(List<AlarmItem> staticItems, SharedPreferences prefs)
    {
        // get current time and day of week.
        Calendar now = Calendar.getInstance();
        LocalDate nowDate = LocalDate.now();
        boolean alarmTime = false;

        for(AlarmItem item : staticItems){
            Calendar tempCal = item.getAlarmDate();
            HashSet<DayOfWeek> daysOfWeek = item.getDaysOfWeek();
            // check if alarm is matching current hour/minute and day of week.
            alarmTime = (tempCal.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY)
                    && tempCal.get(Calendar.MINUTE) == now.get(Calendar.MINUTE) &&
                    daysOfWeek.contains(nowDate.getDayOfWeek()));
            // we need to make sure the alarm was not already triggered and it's active.
            if(alarmTime && !item.isAlarmTriggered() && item.isActive()){
                item.setAlarmTriggered(true);
                AlarmListAdapter.saveAlarmItemsStatic(staticItems, prefs);
                return item;
            }
            // if the alarm was triggered last time set it to false.
            else if(!alarmTime && item.isAlarmTriggered()){
                item.setAlarmTriggered(false);
                AlarmListAdapter.saveAlarmItemsStatic(staticItems, prefs);
          }
        }
        // okay to return null as there is null check on alarm service side.
        return null;
    }


    /**
     * Inner class which helps us bind the AlarmItems to UI elements.
     */
    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        // all local variables for our UI elements that represent one AlarmItem.
        private TextView alarmNameTextView;
        private TextView alarmNameSubTextView;
        private SwitchCompat alarmActiveSwitch;
        private LinearLayout subItem;
        private ArrayList<ToggleButton> toggleButtons;
        private View itemView;
        private Button alarmSoundPicker;

        /**
         * Constructor for one AlarmItem, will find all relevant UI view ids and adds listeners
         * @param itemView itemView we use to fetch UI elements.
         */
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

        /**
         * bind an alarm item, so the object representation matches what the UI shows.
         * @param alarmItem
         */
        private void bind(AlarmItem alarmItem){
            alarmNameTextView.setText(alarmItem.getAlarmName());
            boolean expanded = alarmItem.isExpanded();
            subItem.setVisibility(expanded ? View.VISIBLE : View.GONE);
            boolean active = alarmItem.isActive();
            alarmActiveSwitch.setChecked(active);
            bindDayOfWeekToggle(alarmItem);
            alarmSoundPicker.setText(alarmItem.getAlarmSoundName());
        }

        /**
         * Helper method to get list of all toggle buttons used for day of week toggle.
         * @param itemview view we use to find the needed UI elements
         * @return list of toggle buttons.
         */
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

        /**
         * Bind each day of week toggle to an AlarmItem
         * @param alarmItem AlarmItem to bind the toggle buttons to.
         */
        private void bindDayOfWeekToggle(AlarmItem alarmItem) {
            HashSet<DayOfWeek> daysOfWeekHashSet = alarmItem.getDaysOfWeek();
            for (int i = 0; i < toggleButtons.size(); i++) {
                toggleButtons.get(i).setChecked(daysOfWeekHashSet.contains(DayOfWeek.of(i + 1)));
            }
        }

        /**
         * add listeners to any UI elements that need them.
         * @param alarmViewHolder AlarmViewHolder used to assign listeners to UI elements.
         */
        private void setUpListeners(AlarmViewHolder alarmViewHolder){

            // assign listeners to buttons.
            MaterialButton btn = (MaterialButton) alarmViewHolder.
                    itemView.findViewById(R.id.alarm_delete_button);
            btn.setOnClickListener(l -> {
                deleteAlarmItem(alarmViewHolder.getAdapterPosition());
            });
            // set expanded listener
            alarmViewHolder.itemView.setOnClickListener(l -> {
                int position = alarmViewHolder.getAdapterPosition();
                AlarmItem alarmItem = alarmItems.get(position);
                alarmItem.setExpanded(!alarmItem.isExpanded());
                notifyItemChanged(position);
            });
            // set active switch
            alarmViewHolder.alarmActiveSwitch.setOnClickListener(l -> {
                int position = alarmViewHolder.getAdapterPosition();
                AlarmItem alarmItem = alarmItems.get(position);
                alarmItem.setActive(!alarmItem.isActive());
                notifyItemChanged(position);
            });

            // assign toggle button listeners for each DayOfWeek.
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
            // add listener for alarm sound picker.
            // As we have to trigger this intent from the Main Activity, we send a broadcast
            // to MainActivity's broadcast receivers.
            alarmSoundPicker.setOnClickListener(l -> {
                
                MainActivity.currentItemIndexSoundPick = alarmViewHolder.getAdapterPosition();

                Intent alarmSoundIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                alarmSoundIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

                MainActivity.alarmPickerResultLauncher.launch(alarmSoundIntent);


            });
        }


    }
}