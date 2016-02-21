package org.schabi.terminightor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by the-scrabi on 25.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * AlarmSetupManager.java is part of Terminightor.
 *
 * Terminightor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terminightor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terminightor.  If not, see <http://www.gnu.org/licenses/>.
 */

public class AlarmSetupManager extends BroadcastReceiver {

    private static final String ACTION_RENEW_ALARMS =
            "org.schabi.Terminightor.AlarmSetupManager.ACTION_RENEW_ALARMS";

    private static final String TAG = AlarmSetupManager.class.toString();
    /*
    public static final String ALARM_ID = "alarm_id";
    public static final String ALARM_LABEL = "alarm_label";
    public static final String ALARM_NFC_ID = "alarm_nfc_id";
    public static final String ALARM_REPEAT = "alarm_repeat";
    public static final String ALARM_TONE= "alarm_tone";
    public static final String ALARM_VIBRATE = "alarm_vibrate"; */

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            setupNextAlarm(context);
        } else if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
            Log.d(TAG, "Shutdown received");
            cancelNextAlarm(context);
        } else if(ACTION_RENEW_ALARMS.equals(intent.getAction())) {
            Log.d(TAG, "renew alarm");
            setupNextAlarm(context);
        }
    }

    /*
    public static void readAndSetupAlarms(Context context) {
        setupAlarmsByCursor(context,
                AlarmDBOpenHelper.getAlarmDBOpenHelper(context).query());
    }

    public static void setupAlarm(Context context, long id) {
        AlarmDBOpenHelper db = AlarmDBOpenHelper.getAlarmDBOpenHelper(context);
        setupAlarmsByCursor(context, db.getValueOf(id));
    }
    */

    /*
    // initalize all alarms
    private static void setupAlarmsByCursor(Context context, Cursor cursor) {


        cursor.moveToFirst();

        Calendar now = Calendar.getInstance();
        Calendar midnightToday = Calendar.getInstance();
        midnightToday.set(Calendar.HOUR, 0);
        midnightToday.set(Calendar.AM_PM, 0);
        midnightToday.set(Calendar.MINUTE, 0);
        midnightToday.set(Calendar.HOUR_OF_DAY, 0);
        midnightToday.set(Calendar.SECOND, 0);
        midnightToday.set(Calendar.MILLISECOND, 0);

        Calendar midnightTomorrow = (Calendar) midnightToday.clone();
        midnightTomorrow.add(Calendar.DATE, 1);

        // initialize alarm all 24 hours.
        Calendar nextInitialization = Calendar.getInstance();
        nextInitialization.add(Calendar.DATE, 1);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        while (!cursor.isAfterLast()) {
            Alarm alarm = null;
            try {
                alarm = Alarm.getFromCursorItem(cursor);
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (alarm.isEnabled()) {
                long alarmDate = midnightToday.getTimeInMillis()
                        + alarm.getAlarmTimeInMillis();

                // If alarm has already gone off today, set it up for tomorrow.
                // So all alarms between now and the 24 hours till the next
                // alarm initialization are covered.
                if (alarmDate < now.getTimeInMillis()) {
                    alarmDate = midnightTomorrow.getTimeInMillis()
                            + alarm.getAlarmTimeInMillis();
                }

                // check if alarm is allowed to trigger on this day
                // if repeating is disabled, let it through once, and
                // that disable the alarm once and for all

                Calendar alarmTestDate = Calendar.getInstance();
                alarmTestDate.setTimeInMillis(alarmDate);

                if (!alarm.isRepeatEnabled()
                        || alarm.isDayEnabled(alarmTestDate.get(Calendar.DAY_OF_WEEK))) {

                    PendingIntent alarmPendingIntent =
                            PendingIntent.getBroadcast(context, (int) alarm.getId(),
                                    alarm.getAlarmIntent(), PendingIntent.FLAG_CANCEL_CURRENT);

                    if (Build.VERSION.SDK_INT >= 19) {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                                alarmDate, alarmPendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDate, alarmPendingIntent);
                    }
                }
            }
            cursor.moveToNext();
        }

        if (!renewAlarmIsUp(context)) {
            Log.d(TAG, "set renew alarm");
            setupRenewTimer(context);
        }
    }
    */

    public static Alarm getNextAlarm(Context context) throws Exception {
        Cursor cursor = AlarmDBOpenHelper.getAlarmDBOpenHelper(context).query();
        cursor.moveToFirst();

        Calendar nextAlarmTime = null;
        Alarm nextAlarm = null;

        while(!cursor.isAfterLast()) {
            Alarm alarm = Alarm.getFromCursorItem(cursor);

            //will return null if alarm is not enabled
            Calendar dateOfAlarm = alarm.getNextAlarmDate();

            if(dateOfAlarm != null
                    &&(nextAlarmTime == null || dateOfAlarm.before(nextAlarmTime))) {
                nextAlarmTime = dateOfAlarm;
                nextAlarm = alarm;
            }

            cursor.moveToNext();
        }
        return nextAlarm;
    }

    public static void cancelNextAlarm(Context context) {
        // Does only cancel the pending intent, it does not write to the database.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context,
                SpecialPendingIds.NEXT_ALARM, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(alarmPendingIntent);
    }

    public static void setupNextAlarm(Context context) {
        cancelNextAlarm(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Alarm alarm = null;
        try {
            alarm = getNextAlarm(context);
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(alarm != null) {
            PendingIntent alarmPendingIntent =
                    PendingIntent.getBroadcast(context, SpecialPendingIds.NEXT_ALARM,
                            alarm.getAlarmIntent(), PendingIntent.FLAG_CANCEL_CURRENT);

            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        alarm.getNextAlarmDate().getTimeInMillis(), alarmPendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        alarm.getNextAlarmDate().getTimeInMillis(), alarmPendingIntent);
            }
        }
    }

    public static void setAlarmEnabledById(Context context, long id, boolean enabled) throws Exception {
        // cancel in database
        AlarmDBOpenHelper db = AlarmDBOpenHelper.getAlarmDBOpenHelper(context);
        Alarm alarm = Alarm.getFromCursorItem(db.getReadableItem(id));
        alarm.setEnabled(enabled);
        db.update(alarm);

        // reset intent
        setupNextAlarm(context);
    }

    /*

    public static boolean renewAlarmIsUp(Context context) {
        Intent alarmIntent = new Intent(ACTION_RENEW_ALARMS);
        PendingIntent alarmPendingIntent =
                PendingIntent.getBroadcast(context,
                        SpecialPendingIds.RENEW_ALARM, alarmIntent, PendingIntent.FLAG_NO_CREATE);
        return alarmPendingIntent != null;
    }

    public static boolean alarmIsUp(Context context, long id) {
        Intent alarmIntent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
        PendingIntent alarmPendingIntent =
                PendingIntent.getBroadcast(context, (int)id,
                        alarmIntent, PendingIntent.FLAG_NO_CREATE);
        return alarmPendingIntent != null;
    }

    public static void cancelAlarm(Context context, long id) {
        // Does only cancel the pending intent, it does not write to the database.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context,
                (int)id, alarmIntent, 0);
        alarmManager.cancel(alarmPendingIntent);
    }

    public static void cancelAllAlarms(Context context) {
        // Does only cancel the pending intent, it does not write to the database.
        Cursor cursor = AlarmDBOpenHelper.getAlarmDBOpenHelper(context).query();
        cursor.moveToFirst();
        AlarmDBOpenHelper.Index index = AlarmDBOpenHelper.getIndex(cursor);
        while(!cursor.isAfterLast()) {
            cancelAlarm(context, cursor.getLong(index.ciId));
            cursor.moveToNext();
        }
    }

    public static void cancelRenewAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(ACTION_RENEW_ALARMS);
        PendingIntent alarmPendingIntent =
                PendingIntent.getBroadcast(context, SpecialPendingIds.RENEW_ALARM,
                        alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(alarmPendingIntent);
    }

    public static void setupRenewTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar setupDate = Calendar.getInstance();
        setupDate.add(Calendar.DATE, 1);
        Intent renewIntent = new Intent(ACTION_RENEW_ALARMS);
        PendingIntent pendingSetupIntent =
                PendingIntent.getBroadcast(context, SpecialPendingIds.RENEW_ALARM,
                        renewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, setupDate.getTimeInMillis(), pendingSetupIntent);
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP, setupDate.getTimeInMillis(), pendingSetupIntent);
        }
    }
    */
}
