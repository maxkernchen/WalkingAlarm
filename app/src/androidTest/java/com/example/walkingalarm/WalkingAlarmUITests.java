package com.example.walkingalarm;

import static android.service.autofill.Validators.not;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;


import android.widget.TimePicker;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;

import java.util.Calendar;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WalkingAlarmUITests {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule
            = new ActivityScenarioRule<>(MainActivity.class);


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
                assert MainActivity.getAlarmListAdapterInstance().getItemCount() == 1;
            }

        }

        onView(withId(R.id.alarm_delete_button)).perform(click());

        assert MainActivity.getAlarmListAdapterInstance().getItemCount() == 0;

    }

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

        assert MainActivity.getAlarmListAdapterInstance().getItemCount() == 1;

        onView(withId(R.id.alarm_delete_button)).perform(click());

        assert MainActivity.getAlarmListAdapterInstance().getItemCount() == 0;

    }


}
