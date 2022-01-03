# Walking Alarm 

  

Walking Alarm is an Android application which tracks a user's steps to dismiss an alarm. 

To track steps it uses the [Google Fit Api](https://developers.google.com/fit/android)  

which requires a user to OAuth and give the application read permissions to their fitness data. 

This application will not function without an active Google Fit account and step tracking. 

  

Walking Alarm has been published on the Play Store at the below link: 

  

[https://play.google.com/store/apps/details?id=com.maxkernchen.walkingalarm](https://play.google.com/store/apps/details?id=com.maxkernchen.walkingalarm) 

  

  

Features Include: 

* Dark Mode 

* Vibration Toggle 

* Ability to Pick Own Alarm Sound 

* Ability to Set Steps Required to Dismiss 

  

  

![screenshot 1 Walking Alarm](https://i.imgur.com/AOet0k5.jpg)  ![screenshot 2 Walking Alarm](https://i.imgur.com/rOZQlMm.jpg)  

  

  

Development Discussion: 

  

* One challenge in making this application was understanding the Google API authentication process.  
There is a decent amount of setup required in the Google Cloud Console, but also some research was required on how to call each API. 
In this case I only call one endpoint to retreive a read only data-set of steps walked for the day. These requests are also async so I had to use a Countdown latch to block 
until a timeout period or a response had returned.  

* I gained a lot of experience in broadcast receivers during this application's development, I have learned this are great for updating UI elements programmatically. 
A broadcast can be sent from a boss service, which sends it to receivers that are listening within each activity. This works great for updating the full screen activity which 
displays how many steps the user has remaining to walk to dismiss the alarm. 

* Another challenge with development was that randomly the application would not dismiss alarms. After adding lots of logging and reviewing logs written, I noticed that  
the application would randomly sleep overnight. I had assumed that a foreground service would remain running even if the phone was in deep sleep. This turned out  
not the be the case, and I had to implement a wake lock to keep the service active. I was concerned this may affect battery life, but after many days testing I noticed the application 
does not show up anywhere near the top battery usage on my device. 

  

  

 

 
