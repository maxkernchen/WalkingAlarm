package com.example.walkingalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class AlarmListAdapter extends
        RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmItems;

    private SharedPreferences prefs;

    public AlarmListAdapter(SharedPreferences prefs) {
        this.prefs = prefs;
        this.alarmItems = getAlarmItems();


    }
    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View alarmListView = inflater.inflate(R.layout.alarm_list, parent, false);

        AlarmViewHolder alarmViewHolder = new AlarmViewHolder(alarmListView);

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

    private List<AlarmItem> getAlarmItems(){
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


    public class AlarmViewHolder extends RecyclerView.ViewHolder {

        public TextView alarmNameTextView;
        public TextView alarmNameSubTextView;
        public SwitchCompat alarmActiveSwitch;
        public LinearLayout subItem;


        public AlarmViewHolder(View itemView) {

            super(itemView);

            alarmNameTextView = (TextView) itemView.findViewById(R.id.alarm_name);
            alarmActiveSwitch = (SwitchCompat) itemView.findViewById(R.id.alarm_active_switch);
            subItem = (LinearLayout) itemView.findViewById(R.id.sub_alarm_info);
            alarmNameSubTextView = (TextView) itemView.findViewById(R.id.alarm_sub_text);

        }

        private void bind(AlarmItem alarmItem){

            alarmNameTextView.setText(alarmItem.getAlarmName());

            alarmNameSubTextView.setText(alarmItem.alarmDateToString());

            boolean expanded = alarmItem.isExpanded();
            subItem.setVisibility(expanded ? View.VISIBLE : View.GONE);
            boolean active = alarmItem.isActive();
            alarmActiveSwitch.setChecked(active);




        }
    }
}