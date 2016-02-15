package org.schabi.terminightor;

import android.util.Log;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by the-scrabi on 07.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * TimeConverter.java is part of Terminightor.
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

public class TimeConverter {

    private static final String TAG = TimeConverter.class.toString();

    private static int toTwelfHours(int tfHour) {
        return (tfHour % 12 == 0 ? 12 : (tfHour % 12));
    }

    public static int getHours(int rawTime) {
        int hours = rawTime/60;
        if(hours > 23) {
            Log.e(TAG, "Time lies beyond a day :P");
        }
        return hours;
    }

    public static int getMinutes(int rawTime) {
        return rawTime % 60;
    }

    public static String toString(int hours, int minutes, boolean use24) {
        hours = use24 ? hours : toTwelfHours(hours);
        return (hours < 10 ? " " + Integer.toString(hours)
                : Integer.toString(hours))
                + ":" + (minutes < 10 ? "0" + Integer.toString(minutes) : Integer.toString(minutes));
    }

    public static String toString(int rawTime, boolean use24) {
        return toString(getHours(rawTime), getMinutes(rawTime), use24);
    }
}
