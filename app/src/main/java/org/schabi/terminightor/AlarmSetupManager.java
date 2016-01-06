package org.schabi.terminightor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;

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
    public static final String ALARM_ID = "alarm_id";
    public static final String ALARM_LABEL = "alarm_label";
    public static final String ALARM_NFC_ID = "alarm_nfc_id";
    public static final String ALARM_REPEAT = "alarm_repeat";
    public static final String ALARM_TONE= "alarm_tone";
    public static final String ALARM_VIBRATE = "alarm_vibrate";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            readAndSetupAlarms(context);
            setupRenewTimer(context);
        } else if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
            Log.d(TAG, "Shutdown received");
            cancelAllAlarms(context);
            cancelRenewAlarm(context);
        } else if(ACTION_RENEW_ALARMS.equals(intent.getAction())) {
            Log.d(TAG, "renew alarms");
            readAndSetupAlarms(context);
            setupRenewTimer(context);
        }
    }

    public static void readAndSetupAlarms(Context context) {
        setupAlarmsByCursor(context,
                AlarmDBOpenHelper.getAlarmDBOpenHelper(context).query());
    }

    public static void setupAlarm(Context context, long id) {
        AlarmDBOpenHelper db = AlarmDBOpenHelper.getAlarmDBOpenHelper(context);
        setupAlarmsByCursor(context, db.getValueOf(id));
    }

    private static void setupAlarmsByCursor(Context context, Cursor cursor) {
        cursor.moveToFirst();

        AlarmDBOpenHelper.Index index = AlarmDBOpenHelper.getIndex(cursor);

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

        while(!cursor.isAfterLast()) {
            if(cursor.getInt(index.ciAlarmEnabled) >= 1) {
                int alarmTimeMin = cursor.getInt(index.ciAlarmTime);
                long alarmDate = midnightToday.getTimeInMillis()
                        + (alarmTimeMin * 60 * 1000);

                // If alarm has already gone off today, set it up for tomorrow.
                // So all alarms between now and the 24 hours till the next
                // alarm initialization are covered.
                if (alarmDate < now.getTimeInMillis()) {
                    alarmDate = midnightTomorrow.getTimeInMillis()
                            + (alarmTimeMin * 60 * 1000);
                }

                // check if alarm is allowed to trigger on this day
                // if repeating is disabled, let it through once, and
                // that disable the alarm once and for all
                int alarmDays = cursor.getInt(index.ciEnabledDays);
                Calendar alarmTestDate = Calendar.getInstance();
                alarmTestDate.setTimeInMillis(alarmDate);
                // subtract MONDAY, since the week of Calendar begins with day 2
                // but we need monday being 0.
                int alarmDayOfWeek = alarmTestDate.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY;
                if (!TimeConverter.isRepeatingEnabled(alarmDays) ||
                        TimeConverter.isDayEnabled(alarmDays, alarmDayOfWeek)) {

                    Intent alarmIntent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
                    alarmIntent.putExtra(ALARM_ID, cursor.getLong(index.ciId));
                    alarmIntent.putExtra(ALARM_LABEL, cursor.getString(index.ciName));
                    alarmIntent.putExtra(ALARM_NFC_ID, cursor.getBlob(index.ciAlarmNfcId));
                    alarmIntent.putExtra(ALARM_REPEAT, TimeConverter.isRepeatingEnabled(alarmDays));
                    alarmIntent.putExtra(ALARM_TONE, cursor.getString(index.ciAlarmTone));
                    alarmIntent.putExtra(ALARM_VIBRATE, cursor.getInt(index.ciVibrate) == 1);
                    PendingIntent alarmPendingIntent =
                            PendingIntent.getBroadcast(context, cursor.getInt(index.ciId),
                                    alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);


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

        if(!renewAlarmIsUp(context)) {
            Log.d(TAG, "set renew alarm");
            setupRenewTimer(context);
        }
    }

    public static void setupDebugAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar ad = Calendar.getInstance();
        ad.add(Calendar.SECOND, 1);
        long alarmDate = ad.getTimeInMillis();

        Intent alarmIntent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
        alarmIntent.putExtra(ALARM_ID, 42l);
        alarmIntent.putExtra(ALARM_LABEL, "debug alarm");
        alarmIntent.putExtra(ALARM_NFC_ID, new byte[]{-21, 54, 63, 124});
        alarmIntent.putExtra(ALARM_REPEAT, false);
        alarmIntent.putExtra(ALARM_TONE, "content://media/internal/audio/media/10");
        alarmIntent.putExtra(ALARM_VIBRATE, true);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                context, 42, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if(Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmDate, alarmPendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDate, alarmPendingIntent);
        }
        Log.d(TAG, "setup alarm");

        if(!renewAlarmIsUp(context)) {
            Log.d(TAG, "set renew alarm");
            setupRenewTimer(context);
        }
    }

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

}
