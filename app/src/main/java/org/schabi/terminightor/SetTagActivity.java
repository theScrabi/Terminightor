package org.schabi.terminightor;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.os.Handler;
import android.widget.ImageView;

/**
 * Created by the-scrabi on 24.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * SetTagActivity.java is part of Terminightor.
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

public class SetTagActivity extends Activity {

    private static final String TAG = SetTagActivity.class.getName();

    // result codes
    public static final int CANCELED = 0;
    public static final int ID_RECEIVED = 1;

    // intent data tags
    public static final String NFC_ID = "nfc_id";

    private Button tapToCancelButton;
    private ImageView innerWave;
    private ImageView outerWave;

    private boolean hasStopped = false;

    private NfcAdapter nfcAdapter;

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

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_set_tag);

        tapToCancelButton = (Button) findViewById(R.id.tapToCancelButton);
        innerWave = (ImageView) findViewById(R.id.inner_wave);
        outerWave = (ImageView) findViewById(R.id.outer_wave);

        tapToCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(CANCELED);
                finish();
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(this, this.getClass());
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                SpecialPendingIds.SET_TAG_READ_NFC_TAG_ID, intent, 0);
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
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

        Intent resultIntent = new Intent();
        setResult(ID_RECEIVED, resultIntent);
        resultIntent.putExtra(NFC_ID, id);
        finish();
    }
}
