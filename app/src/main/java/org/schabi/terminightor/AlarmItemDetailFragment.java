package org.schabi.terminightor;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

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
    private int time = 0;
    private byte[] tagId = null;

    private AlarmDBOpenHelper dbHandler;
    private long itemId;
    private boolean isEnabled = false;
    private String alarmTonePath = "";

    private boolean use24Hours = false;

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

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            Log.d(TAG, getArguments().getString(ARG_ITEM_ID));
            try {
                itemId = Long.parseLong(getArguments().getString(ARG_ITEM_ID));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "ERROR: no item id given.");
            getActivity().finish();
        }
        timePickerDialog = new TimePickerDialog(getActivity(), R.style.AppTheme_Timepicker,
                new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setAlarmTimeView.setText(TimeConverter.toString(hourOfDay, minute, use24Hours));
                setAlarmAMPMView.setText(TimeConverter.getAMPMSuffix(hourOfDay, use24Hours));
                time = TimeConverter.getRawTime(hourOfDay, minute);
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
        restoreItem();

        setAlarmToneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                        getResources().getString(R.string.selectAlarmToneTitle));
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
                if(!alarmTonePath.isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(alarmTonePath));
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "result");
        switch(requestCode) {
            case READ_NFC_ID:
                if (resultCode == SetTagActivity.ID_RECEIVED) {
                    tagId = data.getByteArrayExtra(SetTagActivity.NFC_ID);
                    nfcTagIdView.setText(Arrays.toString(tagId));
                    nfcTagLabelView.setVisibility(View.VISIBLE);
                }
                break;
            case SET_ALARM_TONE:
                if (data != null) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        alarmTonePath = uri.toString();
                        setAlarmToneButton.setText(RingtoneManager.getRingtone(getActivity(), uri)
                                .getTitle(getActivity()));
                    } else {
                        alarmTonePath = "";
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
        if (itemId < 0) {
            alarmDBOpenHelper.insert(alarmLabelBox.getText().toString(),
                    isEnabled,
                    time,
                    chooseDateView.getEnabledDays(),
                    alarmTonePath,
                    vibrateCheckBox.isChecked(),
                    tagId);
        } else {
            alarmDBOpenHelper.update(itemId,
                    alarmLabelBox.getText().toString(),
                    isEnabled,
                    time,
                    chooseDateView.getEnabledDays(),
                    alarmTonePath,
                    vibrateCheckBox.isChecked(),
                    tagId);
        }
        if (isEnabled) {
            AlarmSetupManager.cancelAllAlarms(getActivity());
            AlarmSetupManager.readAndSetupAlarms(getActivity());
        } else {
            AlarmSetupManager.cancelAlarm(getActivity(), itemId);
        }
    }

    public void restoreItem() {
        if(itemId >= 0) {
            AlarmDBOpenHelper alarmDBOpenHelper =
                    AlarmDBOpenHelper.getAlarmDBOpenHelper(getActivity());
            time = alarmDBOpenHelper.getAlarmTime(itemId);
            alarmTonePath = alarmDBOpenHelper.getAlarmTone(itemId);
            setAlarmTimeView.setText(TimeConverter.toString(time, use24Hours));
            timePickerDialog.updateTime(TimeConverter.getHours(time),
                    TimeConverter.getMinutes(time));
            setAlarmAMPMView.setText(TimeConverter.getAMPMSuffixByRaw(time, use24Hours));
            chooseDateView.setEnabledDays(alarmDBOpenHelper.getAlarmDays(itemId));
            repeatCheckBox.setChecked(chooseDateView.isRepeatEnabled());
            if (repeatCheckBox.isChecked()) {
                chooseDateView.setVisibility(View.VISIBLE);
            }
            alarmLabelBox.setText(alarmDBOpenHelper.getName(itemId));
            setAlarmToneButton.setText(RingtoneManager
                    .getRingtone(getActivity(), Uri.parse(alarmTonePath)).getTitle(getActivity()));
            vibrateCheckBox.setChecked(alarmDBOpenHelper.isVibrateEnabled(itemId));
            isEnabled = alarmDBOpenHelper.isAlarmEnabled(itemId);
            tagId = alarmDBOpenHelper.getNfcTagId(itemId);
            if (tagId.length != 0) {
                nfcTagLabelView.setVisibility(View.VISIBLE);
                nfcTagIdView.setText(Arrays.toString(tagId));
            }
        } else {
            repeatCheckBox.setChecked(false);
            isEnabled = true;
            setAlarmTimeView.setText("--:--");
            setAlarmAMPMView.setText("");
            Uri alarmToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            alarmTonePath = alarmToneUri.toString();
            setAlarmToneButton.setText(RingtoneManager.getRingtone(getActivity(), alarmToneUri)
                    .getTitle(getActivity()));
            vibrateCheckBox.setChecked(false);
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
        if(itemId >= 0) {
            dbHandler.delete(itemId);
            AlarmSetupManager.cancelAlarm(getActivity(), itemId);
        }
        if(getActivity().getClass() == AlarmItemDetailActivity.class) {
            getActivity().finish();
        }
    }

    private int getTimeBySpinnerSelection(int selection) {
        switch(selection)  {
            case 0: return 0;
            case 1: return 2;
            case 2: return 5;
            case 3: return 15;
            case 4: return 30;
            default:
                Log.e(TAG, "You unlocked a secret mushroom.");
        }
        return -1;
    }

    private int getSpinnerSelectionByTime(int time) {
        switch(time) {
            case 0: return 0;
            case 2: return 1;
            case 5: return 2;
            case 15: return 3;
            case 30: return 4;
            default:
                Log.e(TAG, "This is a dead end. Go back and try another door: " +
                        Integer.toString(time));
        }
        return 0;
    }

    public boolean hasTag() {
        return tagId != null;
    }
}
