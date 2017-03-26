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

        long time = System.currentTimeMillis();

        if(alarm != null) {
            PendingIntent alarmPendingIntent =
                    PendingIntent.getBroadcast(context, SpecialPendingIds.NEXT_ALARM,
                            alarm.getAlarmIntent(), PendingIntent.FLAG_CANCEL_CURRENT);

            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        alarm.getNextAlarmDate().getTimeInMillis(), alarmPendingIntent);
                Log.e(TAG, Long.toString(time) + "    " + Long.toString(alarm.getNextAlarmDate().getTimeInMillis()));
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
}
