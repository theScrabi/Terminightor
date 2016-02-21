package org.schabi.terminightor;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageButton;


/**
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * AlarmItemListActivity.java is part of Terminightor.
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

public class AlarmItemListActivity extends FragmentActivity
        implements AlarmItemListFragment.Callbacks {

    private static final String TAG = AlarmItemListFragment.class.toString();

    AlarmItemListFragment listFragment;
    AlarmItemDetailFragment detailFragment = null;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarmitem_list);

        listFragment = (AlarmItemListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.alarmitem_list);

        if (findViewById(R.id.alarmitem_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((AlarmItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.alarmitem_list))
                    .setActivateOnItemClick(true);
        }

        FloatingActionButton addAlarmButton = (FloatingActionButton) findViewById(R.id.addAlarmButton);
        addAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemSelected(Long.toString(-1));
            }
        });

        ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlarmItemListActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        // will (cancel and) setup the next alarm
        AlarmSetupManager.setupNextAlarm(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Callback method from {@link AlarmItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if(detailFragment != null) {
                detailFragment.updateItem();
                listFragment.updateList();
            }
            Bundle arguments = new Bundle();
            arguments.putString(AlarmItemDetailFragment.ARG_ITEM_ID, id);
            AlarmItemDetailFragment fragment = new AlarmItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.alarmitem_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, AlarmItemDetailActivity.class);
            detailIntent.putExtra(AlarmItemDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
