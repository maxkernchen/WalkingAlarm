package com.example.walkingalarm;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


import static org.junit.Assert.assertEquals;

import android.widget.TimePicker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import java.util.Calendar;

/**
 * Small Instrumentation Test class, will create an alarm then delete it.
 * Also will test for a duplicate alarm being added.
 *
 * @version 1.0
 * @author Max Kernchen
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WalkingAlarmUITests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule
            = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Add a duplicate alarm and make sure only one alarm ends up being added.
     */
    @Test
    public void createDuplicateAlarm() {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        for(int i = 0; i < 2; i++) {

            onView(withId(R.id.fab)).perform(click());

            onView(isAssignableFrom(TimePicker.class)).perform(
                    PickerActions.setTime(
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE)
                    )
            );


            onView(withText("OK")).perform(click());
            // should not have added another alarm.
            if(i == 1){
                assertEquals(1, MainActivity.getAlarmListAdapterInstance().getItemCount());
            }

        }

        onView(withId(R.id.alarm_delete_button)).perform(click());

        assertEquals(0, MainActivity.getAlarmListAdapterInstance().getItemCount());

    }

    /**
     * Simple test which will just add an alarm and make sure it ends up in alarm items.
     */
   @Test
    public void createAlarmAndDelete() {
        onView(withId(R.id.fab)).perform(click());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        onView(isAssignableFrom(TimePicker.class)).perform(
                PickerActions.setTime(
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                )
        );


        onView(withText("OK")).perform(click());

        assertEquals(1, MainActivity.getAlarmListAdapterInstance().getItemCount());

        onView(withId(R.id.alarm_delete_button)).perform(click());

        assertEquals(0, MainActivity.getAlarmListAdapterInstance().getItemCount());

    }


}
