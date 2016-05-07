package org.schabi.terminightor;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Christian Schabesberger on 14.02.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * Alarm.java is part of Terminightor.
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

public class Alarm implements Parcelable {

    private static final String TAG = Alarm.class.toString();

    // keys
    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String ALARM_ENABLED = "alarm_enabled";
    public static final String ALARM_TIME = "alarm_time";
    public static final String ALARM_DAYS = "alarm_days";
    public static final String ALARM_TONE = "alarm_tone";
    public static final String VIBRATE = "vibrate";
    public static final String NFC_TAG_ID = "nfc_tag_id";

    // keys only for intent
    public static final String ALARM_REPEAT = "repeat";

    //values
    private long id = -1;
    private String name = "";
    private boolean enabled = true;
    private int alarmHour = 0;
    private int alarmMinute = 0;
    private int enabledDays = 0b0011111;
    private boolean repeatEnabled = false;
    private String alarmTone = "";
    private boolean vibrate = false;
    private byte[] nfcTagId = null;

    public void setHour(int hour) throws Exception {
        if(hour >= 24 || hour < 0) {
            throw new Exception("Wrong hour entered: " + Integer.toString(hour));
        }
        this.alarmHour = hour;
    }

    public void setMinute(int minute) throws Exception {
        if(minute >= 60 || minute < 0) {
            throw new Exception("Wront minute entered: " + Integer.toString(minute));
        }
        this.alarmMinute = minute;
    }

    public void setTerminightorStyledAlarmDays(int alarmDays) {
        repeatEnabled = ((1<<7) & alarmDays) >= 1;
        enabledDays = ~(1<<7) & alarmDays;
    }

    public void setAlarmTone(String alarmTone) {
        this.alarmTone = alarmTone;
    }

    public void setVibrateEnabled(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public void setNfcTagId(byte[] nfcTagId) {
        this.nfcTagId = nfcTagId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static Alarm getFromCursorItem(Cursor cursor) throws Exception {
        AlarmDBOpenHelper.Index index = AlarmDBOpenHelper.getIndex(cursor);
        Alarm alarm = new Alarm();
        alarm.id = cursor.getLong(index.ciId);
        alarm.name = cursor.getString(index.ciName);
        alarm.enabled = cursor.getInt(index.ciAlarmEnabled) >= 1;
        int rawTime = cursor.getInt(index.ciAlarmTime);
        if(rawTime >= 24 * 60 || rawTime < 0) {
            throw new Exception("Invalid time loaded from database.");
        }
        alarm.setHour(rawTime / 60);
        alarm.setMinute(rawTime % 60);
        alarm.enabledDays = ~(1<<7) & cursor.getInt(index.ciEnabledDays);
        alarm.repeatEnabled = ((1<<7) & cursor.getInt(index.ciEnabledDays)) >= 1;
        alarm.alarmTone = cursor.getString(index.ciAlarmTone);
        alarm.vibrate = cursor.getInt(index.ciVibrate) >= 1;
        alarm.nfcTagId = cursor.getBlob(index.ciAlarmNfcId);
        return alarm;
    }

    public int getTerminightorStyledAlarmTime() {
        return alarmHour * 60 + alarmMinute;
    }

    public int getTerminightorStyledAlarmDays() {
        if(repeatEnabled) {
            return enabledDays | (1<<7);
        } else {
            return 0;
        }
    }

    public long getId() {
        return id;
    }

    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(ALARM_ENABLED, enabled ? 1 : 0);
        values.put(ALARM_TIME, getTerminightorStyledAlarmTime());
        values.put(ALARM_DAYS, getTerminightorStyledAlarmDays());
        values.put(ALARM_TONE, alarmTone);
        values.put(VIBRATE, vibrate ? 1 : 0);
        values.put(NFC_TAG_ID, nfcTagId);
        return values;
    }

    public Intent getAlarmIntent() {
        Intent intent = new Intent(NightKillerReceiver.ACTION_FIRE_ALARM);
        intent.putExtra(ID, id);
        return intent;
    }

    public boolean isDayEnabled(int javaCalendarDay) {
        // first convert
        int tDay;
        if(javaCalendarDay == Calendar.SUNDAY) {
            tDay = 6;
        } else {
            tDay = javaCalendarDay - Calendar.MONDAY;
        }

        return ((1<<tDay) & enabledDays) >= 1;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }

    public String getAlarmTone() {
        return alarmTone;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public byte[] getNfcTagId() {
        return nfcTagId;
    }

    /*public int getRawAlarmTime() {
        return alarmHour * 60 + alarmMinute;
    }*/

    public long getAlarmTimeInMillis() {
        return (alarmHour * 60 + alarmMinute) * 60 * 1000;
    }

    public int getMinute() {
        return alarmMinute;
    }

    public int getHour() {
        return alarmHour;
    }

    public String getAMPMSuffix(boolean use24) {
        return use24 ? "" :
                ((alarmHour < 12) ? "AM" : "PM");
    }

    public String getTimeString(boolean use24) {
        int hour = use24 ? alarmHour : toTwelfHours(alarmHour);
        return (hour < 10 ? " " + Integer.toString(hour)
                : Integer.toString(hour))
                + ":" + (alarmMinute < 10 ? "0" + Integer.toString(alarmMinute) : Integer.toString(alarmMinute));
    }

    public Calendar getNextAlarmDate() {
        if(isEnabled()) {
            final Calendar now = Calendar.getInstance();
            Calendar nextAlarm = Calendar.getInstance();
            Calendar midnightToday = (Calendar) now.clone();
            midnightToday.set(Calendar.HOUR, 0);
            midnightToday.set(Calendar.AM_PM, 0);
            midnightToday.set(Calendar.MINUTE, 0);
            midnightToday.set(Calendar.HOUR_OF_DAY, 0);
            midnightToday.set(Calendar.SECOND, 0);
            midnightToday.set(Calendar.MILLISECOND, 0);

            if(isRepeatEnabled()) {
                Calendar helperTime = (Calendar) midnightToday.clone();

                helperTime.add(Calendar.HOUR_OF_DAY, alarmHour);
                helperTime.add(Calendar.MINUTE, alarmMinute);
                if(isDayEnabled(midnightToday.get(Calendar.DAY_OF_WEEK))
                    && helperTime.after(now)) {
                    // if alarm is set for today and did not yet fire, we can set it up
                    // for today else we set it of for the same day of the week, but next week.
                    nextAlarm = helperTime;
                    return nextAlarm;
                } else {
                    // weird for loop ist meant like this:
                    // look for the first alarm between tomorrow and today next week.
                    // tomorrow is +1 today next week is +7
                    // today was already tested by the if clause above
                    for(int daysFromNow = 1; daysFromNow <= 7; daysFromNow++) {
                        helperTime = (Calendar) midnightToday.clone();
                        helperTime.add(Calendar.DAY_OF_MONTH, daysFromNow);
                        if(isDayEnabled(helperTime.get(Calendar.DAY_OF_WEEK))) {
                            helperTime.add(Calendar.HOUR_OF_DAY, alarmHour);
                            helperTime.add(Calendar.MINUTE, alarmMinute);
                            nextAlarm = helperTime;
                            return nextAlarm;
                        }
                    }
                }
            } else {
                nextAlarm = midnightToday;
                nextAlarm.add(Calendar.HOUR_OF_DAY, alarmHour);
                nextAlarm.add(Calendar.MINUTE, alarmMinute);
                if(nextAlarm.before(now)) {
                    nextAlarm.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
            return (Calendar) nextAlarm.clone();
        } else {
            return null;
        }
    }

    public boolean hasTag() {
        return nfcTagId != null;
    }

    private static int toTwelfHours(int tfHour) {
        return (tfHour % 12 == 0 ? 12 : (tfHour % 12));
    }


    //parcelable stuff

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeByte(enabled ? (byte) 1 : (byte) 0);
        dest.writeInt(this.alarmHour);
        dest.writeInt(this.alarmMinute);
        dest.writeInt(this.enabledDays);
        dest.writeByte(repeatEnabled ? (byte) 1 : (byte) 0);
        dest.writeString(this.alarmTone);
        dest.writeByte(vibrate ? (byte) 1 : (byte) 0);
        dest.writeByteArray(this.nfcTagId);
    }

    public Alarm() {
    }

    protected Alarm(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.enabled = in.readByte() != 0;
        this.alarmHour = in.readInt();
        this.alarmMinute = in.readInt();
        this.enabledDays = in.readInt();
        this.repeatEnabled = in.readByte() != 0;
        this.alarmTone = in.readString();
        this.vibrate = in.readByte() != 0;
        this.nfcTagId = in.createByteArray();
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel source) {
            return new Alarm(source);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
}
