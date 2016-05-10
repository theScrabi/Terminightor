package org.schabi.terminightor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by the-scrabi on 27.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * NightKillerService.java is part of Terminightor.
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

public class NightKillerService extends Service {

    private static final String TAG = NightKillerService.class.toString();
    public static final String ACTION_KILL_ALARM = "org.schabi.Terminightor.NightKillerService.ACTION_KILL_ALARM";
    private static final int NOTIFICATION_ID = 0;

    private Intent alarmActivityIntent;

    private final IBinder mBinder = new ServiceBinder();
    MediaPlayer mediaPlayer = null;
    Vibrator vibrator = null;
    boolean gotDisabled = false;

    Alarm alarm;
    AlarmIndicator indicator = new AlarmIndicator(this);

    NotificationManager nm;

    private BroadcastReceiver disableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_KILL_ALARM.equals(intent.getAction())) {
                Log.d(TAG, "shutdown alarm");
                try {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(alarm.isVibrate()) {
                    vibrator.cancel();
                }
                gotDisabled = true;
                NightKillerService.this.stopSelf();
            }
        }
    };

    public class ServiceBinder extends Binder {
        NightKillerService getService() {
            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        alarmActivityIntent = new Intent(this, NightKillerActivity.class);
        LocalBroadcastManager.getInstance(this).registerReceiver(disableReceiver,
                new IntentFilter(ACTION_KILL_ALARM));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            Log.d(TAG, "get data from intent");
            try {
                alarm = Alarm.getFromCursorItem(AlarmDBOpenHelper.getAlarmDBOpenHelper(this).getReadableItem(
                        intent.getLongExtra(Alarm.ID, -1)));
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(!alarm.isRepeatEnabled()) {
                alarm.setEnabled(false);
                AlarmDBOpenHelper.getAlarmDBOpenHelper(this).update(alarm);
            }


            indicator.saveAlarm(alarm);
        } else {
            Log.d(TAG, "get data from indicator");
            try {
                alarm = indicator.restoreAlarm(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {1000, 200, 200, 200};
        if (alarm.isVibrate()) {
            vibrator.vibrate(pattern, 0);
        }

        mediaPlayer = setupNewMediaPlayer(alarm);
        mediaPlayer.start();

        alarmActivityIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_FROM_BACKGROUND);
        alarmActivityIntent.putExtra(Alarm.ID, alarm.getId());
        alarmActivityIntent.putExtra(Alarm.NAME, alarm.getName());
        alarmActivityIntent.putExtra(Alarm.NFC_TAG_ID, alarm.getNfcTagId());
        this.startActivity(alarmActivityIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.alarm))
                .setContentText(alarm.getName())
                .setSmallIcon(R.drawable.terminightor_notify_small)
                .setOngoing(true)
                .build();
        n.contentIntent = PendingIntent.getActivity(this.getApplicationContext(),
                SpecialPendingIds.OPEN_ALARM_ACTIVITY, alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        nm.notify(NOTIFICATION_ID, n);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        try {
            mediaPlayer.stop();
            mediaPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "///////destory service//////");
        if(gotDisabled) {
            indicator.removeIndicator();
        } else {
            Intent restartIntent = new Intent(this, NightKillerService.class);
            restartIntent.putExtra(Alarm.ID, alarm.getId());
            startService(restartIntent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MediaPlayer setupNewMediaPlayer(Alarm alarm) {
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(alarm.getAlarmTone()));
            boolean overrideVolume = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(getString(R.string.overrideAlarmVolume), false);
            if(overrideVolume) {
                mediaPlayer.setVolume(1.0f, 1.0f);
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mediaPlayer;
    }
}
