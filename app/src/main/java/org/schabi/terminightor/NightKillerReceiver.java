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

            context.startService(alarmServiceIntent);
            AlarmSetupManager.setupNextAlarm(context);
        }
    }
}
