package org.schabi.terminightor;

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by the-scrabi on 07.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * AlarmAdapter.java is part of Terminightor.
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

public class AlarmAdapter extends CursorAdapter {
    private LayoutInflater inflater;
    private boolean use24Hours;

    public AlarmAdapter(Context context) {
        super(context, null, 0);
        inflater = LayoutInflater.from(context);
        use24Hours = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.use24Hours), false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        Alarm alarm = null;
        try {
            alarm = Alarm.getFromCursorItem(cursor);
        } catch(Exception e) {
            e.printStackTrace();
        }

        final int id = (int) alarm.getId();
        TextView alarmLabel = (TextView) view.findViewById(R.id.alarmLabel);
        TextView alarmTime = (TextView) view.findViewById(R.id.alarmTimeLabel);
        TextView amPmSuffix = (TextView) view.findViewById(R.id.amPmSuffix);
        SwitchCompat enabledSwitch = (SwitchCompat) view.findViewById(R.id.alarmEnabledSwitch);

        alarmLabel.setText(alarm.getName());
        alarmTime.setText(alarm.getTimeString(use24Hours));
        amPmSuffix.setText(alarm.getAMPMSuffix(use24Hours));
        enabledSwitch.setChecked(alarm.isEnabled());

        enabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                /*
                AlarmDBOpenHelper.getAlarmDBOpenHelper(context).setAlarmEnabled(id, isChecked);
                if (isChecked) {
                    AlarmSetupManager.cancelAllAlarms(context);
                    AlarmSetupManager.readAndSetupAlarms(context);
                } else {
                    AlarmSetupManager.cancelAlarm(context, id);
                }
                */
                try {
                    AlarmSetupManager.setAlarmEnabledById(context, id, isChecked);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.alarm_item, null);
    }
}
