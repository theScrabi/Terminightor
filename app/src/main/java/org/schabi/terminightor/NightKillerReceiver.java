package org.schabi.terminightor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by the-scrabi on 26.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * NightKillerReceiver.java is part of Terminightor.
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

public class NightKillerReceiver extends BroadcastReceiver {
    private static final String TAG = NightKillerReceiver.class.toString();

    public static final String ACTION_FIRE_ALARM = "org.schabi.Terminightor.NightKillerReceiver.ACTION_FIRE_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_FIRE_ALARM)) {

            Log.d(TAG, "Gonna kill your night");
            Intent alarmServiceIntent = intent;
            intent.setClass(context, NightKillerService.class);
            /*
            Intent alarmServiceIntent = new Intent(context, NightKillerService.class);

            alarmServiceIntent.putExtra(Alarm.ID,
                    intent.getLongExtra(AlarmSetupManager.ALARM_ID, -1));
            alarmServiceIntent.putExtra(AlarmSetupManager.ALARM_LABEL,
                    intent.getStringExtra(AlarmSetupManager.ALARM_LABEL));
            alarmServiceIntent.putExtra(AlarmSetupManager.ALARM_NFC_ID,
                    intent.getByteArrayExtra(AlarmSetupManager.ALARM_NFC_ID));
            alarmServiceIntent.putExtra(AlarmSetupManager.ALARM_TONE,
                    intent.getStringExtra(AlarmSetupManager.ALARM_TONE));
            alarmServiceIntent.putExtra(AlarmSetupManager.ALARM_VIBRATE,
                    intent.getBooleanExtra(AlarmSetupManager.ALARM_VIBRATE, false));
                    */
            context.startService(alarmServiceIntent);

            if(!intent.getBooleanExtra(Alarm.ALARM_REPEAT, false)) {
                long id = intent.getLongExtra(Alarm.ID, -1);
                Alarm alarm = null;
                try {
                    alarm = Alarm.getFromCursorItem(AlarmDBOpenHelper.
                            getAlarmDBOpenHelper(context).getReadableItem(id));
                } catch(Exception e) {
                    e.printStackTrace();
                }
                alarm.setEnabled(false);
                AlarmDBOpenHelper.getAlarmDBOpenHelper(context).update(alarm);
            }
        }
    }
}
