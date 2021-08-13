package com.example.walkingalarm;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmListAdapter extends
        RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmItems;

    public AlarmListAdapter(List<AlarmItem> alarmItems) {
        this.alarmItems = alarmItems;
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


        holder.itemView.setOnClickListener(l -> {
            alarmItem.setExpanded(!alarmItem.isExpanded());
            notifyItemChanged(position);
        });
        holder.alarmActiveSwitch.setOnClickListener(l -> {
            alarmItem.setActive(!alarmItem.isActive());
            notifyItemChanged(position);
        });
    }


    @Override
    public int getItemCount() {
        return alarmItems.size();
    }


    public class AlarmViewHolder extends RecyclerView.ViewHolder {

        public TextView alarmNameTextView;
        public SwitchCompat alarmActiveSwitch;
        public LinearLayout subItem;


        public AlarmViewHolder(View itemView) {

            super(itemView);

            alarmNameTextView = (TextView) itemView.findViewById(R.id.alarm_name);
            alarmActiveSwitch = (SwitchCompat) itemView.findViewById(R.id.alarm_active_switch);
            subItem = (LinearLayout) itemView.findViewById(R.id.sub_alarm_info);

        }

        private void bind(AlarmItem alarmItem){

            alarmNameTextView.setText(alarmItem.getAlarmName());

            boolean expanded = alarmItem.isExpanded();
            subItem.setVisibility(expanded ? View.VISIBLE : View.GONE);
            boolean active = alarmItem.isActive();
            alarmActiveSwitch.setChecked(active);




        }
    }
}