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
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class AlarmIndicator {

    private static final String TAG = AlarmIndicator.class.getName();
    private static final String FILE_NAME = "alarm.indicator";

    private long id;
    private String label;
    private byte[] nfcId;
    private String alarmTonePath;
    private boolean vibrate;
    private Context context;

    public AlarmIndicator(Context context) {
        this.context = context;
    }

    public void saveAlarm(long id, String label, byte[] nfcId,
                          String alarmTonePath, boolean vibrate) {

        this.id = id;
        this.label = label;
        this.nfcId = nfcId;
        this.alarmTonePath = alarmTonePath;
        this.vibrate = vibrate;

        String content;
        content = Long.toString(id) + "\n";
        content += label + "\n";
        content += Arrays.toString(nfcId) + "\n";
        content += alarmTonePath + "\n";
        content += vibrate ? "1" : "0";
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

    public void restoreAlarm() {
        try {
            FileInputStream iFile = context.openFileInput(FILE_NAME);
            BufferedReader in = new BufferedReader(new InputStreamReader(iFile));
            id = Long.parseLong(in.readLine());
            label = in.readLine();
            String[] nfcIdRaw = in.readLine().replace("[", "")
                    .replace("]", "")
                    .replace(" ", "")
                    .split(",");
            nfcId = new byte[nfcIdRaw.length];
            for(int i = 0; i < nfcIdRaw.length; i++) {
                nfcId[i] = Byte.parseByte(nfcIdRaw[i]);
            }
            alarmTonePath = in.readLine();
            vibrate = in.readLine().equals("1");
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeIndicator() {
        try {
            Log.d(TAG, "Delete file");
            context.deleteFile(FILE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public byte[] getNfcId() {
        return nfcId;
    }

    public String getAlarmTone() {
        return alarmTonePath;
    }

    public boolean isVibrateEnabled() {
        return vibrate;
    }
}
