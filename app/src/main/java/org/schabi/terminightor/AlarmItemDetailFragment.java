package org.schabi.terminightor;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Arrays;


/**
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * AlarmItemDetailFragment.java is part of Terminightor.
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

public class AlarmItemDetailFragment extends Fragment {

    private static final String TAG = AlarmItemListFragment.class.toString();

    private static final int READ_NFC_ID = 0;    // request codes
    private static final int SET_ALARM_TONE = 1;

    // views
    private TextView setAlarmTimeView;
    private TextView setAlarmAMPMView;
    private CheckBox repeatCheckBox;
    private ChooseDaysView chooseDateView;
    private EditText alarmLabelBox;
    private Button setAlarmToneButton;
    private CheckBox vibrateCheckBox;
    private ImageView nfcTagLabelView;
    private TextView nfcTagIdView;
    private FloatingActionButton addNfcTabButton;
    private TimePickerDialog timePickerDialog;
    private AlarmDBOpenHelper dbHandler;

    private Alarm alarm;
    private boolean use24Hours = false;
    // indicates wether the user already set a time or not.
    // this is used in order to display the --:-- styled view
    // after restoring the fragment.
    private boolean timeValueSet = false;
    private Bundle lastSavedInstaneState = null;

    // saved instance state ids
    private static final String ALARM = "alarm";
    private static final String USE24HOURS = "use24Hours";
    private static final String TIME_VALUE_SET = "timeValueSet";

    // these variables are used to make the app work despide that good damn fragment lifecicle
    private byte[] returnedNfcTagId = null;
    // Will be set to true in the onResume() methode.
    private boolean gotInited = false;

    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AlarmItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        dbHandler = AlarmDBOpenHelper.getAlarmDBOpenHelper(getActivity());

        timePickerDialog = new TimePickerDialog(getActivity(), R.style.AppTheme_Timepicker,
                new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                try {
                    alarm.setHour(hourOfDay);
                    alarm.setMinute(minute);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                setAlarmTimeView.setText(alarm.getTimeString(use24Hours));
                setAlarmAMPMView.setText(alarm.getAMPMSuffix(use24Hours));
                timeValueSet = true;
            }
        }, 0, 0, true);

        use24Hours = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(this.getString(R.string.use24Hours), false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alarmitem_detail, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        lastSavedInstaneState = savedInstanceState;
        Activity a = getActivity();
        setAlarmTimeView = (TextView) a.findViewById(R.id.setAlarmTimeView);
        setAlarmAMPMView = (TextView) a.findViewById(R.id.setAlarmAmPmSuffix);
        repeatCheckBox = (CheckBox) a.findViewById(R.id.setRepeatCheckBox);
        chooseDateView = (ChooseDaysView) a.findViewById(R.id.chooseDateView);
        alarmLabelBox = (EditText) a.findViewById(R.id.setAlarmLabelBox);
        setAlarmToneButton = (Button) a.findViewById(R.id.setAlarmToneButton);
        vibrateCheckBox = (CheckBox) a.findViewById(R.id.vibrateCheckBox);
        nfcTagLabelView = (ImageView) a.findViewById(R.id.nfcTagLabelView);
        nfcTagIdView = (TextView) a.findViewById(R.id.nfcTagIdView);
        addNfcTabButton = (FloatingActionButton) a.findViewById(R.id.addNfcTagButton);

        repeatCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatCheckBox.isChecked()) {
                    chooseDateView.setVisibility(View.VISIBLE);
                } else {
                    chooseDateView.setVisibility(View.GONE);
                }
                chooseDateView.setRepeatEnabled(repeatCheckBox.isChecked());
            }
        });

        setAlarmTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timePickerDialog.show();
            }
        });

        setAlarmToneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                        getResources().getString(R.string.selectAlarmToneTitle));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
                if(!alarm.getAlarmTone().isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(alarm.getAlarmTone()));
                }
                startActivityForResult(intent, SET_ALARM_TONE);
            }
        });

        addNfcTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetTagActivity.class);
                startActivityForResult(intent, READ_NFC_ID);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(lastSavedInstaneState == null) {
            // If this fragment was already existing we don't need to load the arguments again
            // otherwiese we would override already set values.
            if(!gotInited) {
                if (getArguments().containsKey(ARG_ITEM_ID)) {
                    try {
                        restoreItem(Long.parseLong(getArguments().getString(ARG_ITEM_ID)));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "ERROR: no item id given.");
                    getActivity().finish();
                }
            }
        } else {
            use24Hours = lastSavedInstaneState.getBoolean(USE24HOURS);
            alarm = lastSavedInstaneState.getParcelable(ALARM);
            timeValueSet = lastSavedInstaneState.getBoolean(TIME_VALUE_SET);
            restoreAlarm(alarm);
            if(!timeValueSet) {
                displayNoTimeEntered();
            }
        }

        if(returnedNfcTagId != null) {
            alarm.setNfcTagId(returnedNfcTagId);
            returnedNfcTagId = null;
        }
        gotInited = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateAlarm();
        outState.putParcelable(ALARM, alarm);
        outState.putBoolean(USE24HOURS, use24Hours);
        outState.putBoolean(TIME_VALUE_SET, timeValueSet);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case READ_NFC_ID:
                if (resultCode == SetTagActivity.ID_RECEIVED) {
                    byte[] nfcTagId = data.getByteArrayExtra(SetTagActivity.NFC_ID);
                    returnedNfcTagId = nfcTagId;
                    nfcTagIdView.setText(Arrays.toString(nfcTagId));
                    nfcTagLabelView.setVisibility(View.VISIBLE);
                }
                break;
            case SET_ALARM_TONE:
                if (data != null) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        alarm.setAlarmTone(uri.toString());
                        setAlarmToneButton.setText(RingtoneManager.getRingtone(getActivity(), uri)
                                .getTitle(getActivity()));
                    } else {
                        alarm.setAlarmTone("");
                        setAlarmToneButton.setText(getString(R.string.noneAlarmTone));
                    }
                }
                break;
            default:
                Log.e(TAG, "ERROR: request code not known");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_delete) {
            leaveAndDelete();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void updateItem() {
        AlarmDBOpenHelper alarmDBOpenHelper = AlarmDBOpenHelper.getAlarmDBOpenHelper(getActivity());
        updateAlarm();
        if (alarm.getId() < 0) {
            alarmDBOpenHelper.insert(alarm);
        } else {
            alarmDBOpenHelper.update(alarm);
        }
        AlarmSetupManager.setupNextAlarm(getActivity());
        Toast.makeText(getContext(), R.string.alarmSaved, Toast.LENGTH_SHORT).show();
    }

    public void updateAlarm() {
        alarm.setTerminightorStyledAlarmDays(chooseDateView.getEnabledDays());
        alarm.setVibrateEnabled(vibrateCheckBox.isChecked());
        alarm.setName(alarmLabelBox.getText().toString());
    }

    public void restoreItem(long id) {
        Context c = getContext();
        assert c != null;
        if(id>= 0) {
            try {
                alarm = Alarm.getFromCursorItem(
                        AlarmDBOpenHelper.getAlarmDBOpenHelper(c).getReadableItem(id));
            } catch(Exception e) {
                e.printStackTrace();
            }
            restoreAlarm(alarm);
            timeValueSet = true;
        } else {
            alarm = new Alarm();
            repeatCheckBox.setChecked(false);
            chooseDateView.setRepeatEnabled(false);
            displayNoTimeEntered();
            Uri alarmToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            alarm.setAlarmTone(alarmToneUri.toString());
            try {
                setAlarmToneButton.setText(RingtoneManager.getRingtone(c, alarmToneUri)
                        .getTitle(c));
            } catch(Exception e) {
                e.printStackTrace();
                setAlarmToneButton.setText(c.getString(R.string.defaultRingTone));
            }
        }
    }

    private void restoreAlarm(Alarm alarm) {
        Context c = getContext();
        setAlarmTimeView.setText(alarm.getTimeString(use24Hours));
        timePickerDialog.updateTime(alarm.getHour(), alarm.getMinute());
        setAlarmAMPMView.setText(alarm.getAMPMSuffix(use24Hours));
        chooseDateView.setEnabledDays(alarm.getTerminightorStyledAlarmDays());
        repeatCheckBox.setChecked(chooseDateView.isRepeatEnabled());
        if (chooseDateView.isRepeatEnabled()) {
            chooseDateView.setVisibility(View.VISIBLE);
        }
        alarmLabelBox.setText(alarm.getName());
        setAlarmToneButton.setText(RingtoneManager
                .getRingtone(c, Uri.parse(alarm.getAlarmTone())).getTitle(c));
        vibrateCheckBox.setChecked(alarm.isVibrate());
        if (alarm.getNfcTagId() != null && alarm.getNfcTagId().length != 0) {
            nfcTagLabelView.setVisibility(View.VISIBLE);
            nfcTagIdView.setText(Arrays.toString(alarm.getNfcTagId()));
        }
    }

    public void noNfcTagSetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.noNfcTagMessage)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveAndDelete();
                    }
                });
        builder.create().show();
    }

    private void leaveAndDelete() {
        if(alarm.getId() >= 0) {
            dbHandler.delete(alarm.getId());
            AlarmSetupManager.setupNextAlarm(getActivity());
        }
        if(getActivity().getClass() == AlarmItemDetailActivity.class) {
            getActivity().finish();
        }
    }

    private void displayNoTimeEntered() {
        setAlarmTimeView.setText("--:--");
        setAlarmAMPMView.setText("");
    }

    public boolean hasTag() {
        return alarm.hasTag();
    }
}
