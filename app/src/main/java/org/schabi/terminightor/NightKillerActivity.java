package org.schabi.terminightor;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

/**
 * Created by the-scrabi on 26.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * NightKillerActivity.java is part of Terminightor.
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

public class NightKillerActivity extends Activity {

    private static final String TAG = NightKillerActivity.class.toString();

    private TextView alarmLabelView;
    private ImageView innerWave;
    private ImageView outerWave;
    private boolean hasStopped = false;
    private boolean ignoreNfcTagId = false;

    private NfcAdapter nfcAdapter;
    private long alarmId = -1;
    private String alarmLabel = "";
    private byte[] expectedNfcId = null;
    private PendingIntent pendingIntent;

    Runnable animRunnable = new Runnable() {
        Handler handler = new Handler();
        private int animCounter = 0;

        @Override
        public void run() {
            switch (animCounter) {
                case 0:
                    break;
                case 1:
                    innerWave.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    innerWave.setVisibility(View.INVISIBLE);
                    outerWave.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    outerWave.setVisibility(View.INVISIBLE);
                    break;
                case 4:
                    break;
                case 5:
                    animCounter = 0;
                    break;
                default:
                    Log.d(TAG, "Call the cops the programmer broke it.");
            }
            animCounter++;
            if (!hasStopped) {
                handler.postDelayed(this, 500);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nightkiller);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Bundle a = getIntent().getExtras();
        alarmId = a.getLong(Alarm.ID);
        alarmLabel = a.getString(Alarm.NAME);
        expectedNfcId = a.getByteArray(Alarm.NFC_TAG_ID);

        innerWave = (ImageView) findViewById(R.id.inner_wave_alarm);
        outerWave = (ImageView) findViewById(R.id.outer_wave_alarm);
        alarmLabelView = (TextView) findViewById(R.id.alarm_label_alarm);
        alarmLabelView.setText(alarmLabel);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        ignoreNfcTagId = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(this.getString(R.string.ignoreNfcId), false);

        Log.d(TAG, Long.toString(alarmId));
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, this.getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        pendingIntent = PendingIntent.getActivity(this,
                SpecialPendingIds.NIGHT_KILLER_READ_NFC_TAG_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        IntentFilter[] intentFilters = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);

        Handler handler = new Handler();
        handler.postDelayed(animRunnable, 500);
        hasStopped = false;

        super.onResume();
    }

    @Override
    public void onPause() {
        nfcAdapter.disableForegroundDispatch(this);
        hasStopped = true;
        pendingIntent.cancel();
        super.onPause();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        byte[] nfcId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
        if(nfcId != null) {
            if (Arrays.equals(nfcId, expectedNfcId) || ignoreNfcTagId) {
                Log.d(TAG, "Send kill alarm");
                Intent killAlarmIntent = new Intent(NightKillerService.ACTION_KILL_ALARM);
                LocalBroadcastManager.getInstance(this).sendBroadcast(killAlarmIntent);
                finish();
            } else {
                Toast.makeText(this, R.string.wrongTagToast, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Expected: " + Arrays.toString(expectedNfcId));
                Log.d(TAG, "Given:    " + Arrays.toString(nfcId));
            }
        }
    }
}
