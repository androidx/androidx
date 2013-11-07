/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.supportv4.widget;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.supportv4.R;
import com.example.android.supportv4.Shakespeare;

import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;

/**
 * Example of using the SwipeRefreshLayout.
 */
public class SwipeRefreshLayoutActivity extends Activity implements OnRefreshListener {
    public static final String[] TITLES =
    {
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear"
    };
    // Try a SUPER quick refresh to make sure we don't get extra refreshes
    // while the user's finger is still down.
    private static final boolean SUPER_QUICK_REFRESH = false;
    private View mContent;
    private SwipeRefreshLayout mSwipeRefreshWidget;
    private ListView mList;
    private Handler mHandler = new Handler();
    private final Runnable mRefreshDone = new Runnable() {

        @Override
        public void run() {
            mSwipeRefreshWidget.setRefreshing(false);
        }

    };
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.swipe_refresh_widget_sample);
        mSwipeRefreshWidget = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_widget);
        mSwipeRefreshWidget.setColorScheme(R.color.color1, R.color.color2, R.color.color3,
                R.color.color4);
        mList = (ListView) findViewById(R.id.content);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, TITLES);
        mList.setAdapter(arrayAdapter);
        mSwipeRefreshWidget.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        refresh();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.swipe_refresh_menu, menu);
        return true;
    }

    /**
     * Click handler for the menu item to force a refresh.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch(id) {
            case R.id.force_refresh:
                mSwipeRefreshWidget.setRefreshing(true);
                refresh();
                return true;
        }
        return false;
    }

    private void refresh() {
        mHandler.removeCallbacks(mRefreshDone);
        mHandler.postDelayed(mRefreshDone, 1000);
    }
}