package org.schabi.terminightor;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;

/**
 * Created by the-scrabi on 10.10.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * AlarmIndicator.java is part of Terminightor.
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

public class AlarmIndicator {

    private static final String TAG = AlarmIndicator.class.getName();
    private static final String FILE_NAME = "alarm.indicator";

    private long id;
    private Context context;

    public AlarmIndicator(Context context) {
        this.context = context;
    }

    public void saveAlarm(Alarm alarm) {

        String content;
        content = Long.toString(alarm.getId());
        try {
            FileOutputStream iFile = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            OutputStreamWriter out = new OutputStreamWriter(iFile);
            out.write(content);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Alarm restoreAlarm(Context context) throws Exception {

        FileInputStream iFile = context.openFileInput(FILE_NAME);
        BufferedReader in = new BufferedReader(new InputStreamReader(iFile));
        id = Long.parseLong(in.readLine());
        in.close();

        return Alarm.getFromCursorItem(
                AlarmDBOpenHelper.getAlarmDBOpenHelper(context).getReadableItem(id));
    }

    public void removeIndicator() {
        try {
            Log.d(TAG, "Delete file");
            context.deleteFile(FILE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
