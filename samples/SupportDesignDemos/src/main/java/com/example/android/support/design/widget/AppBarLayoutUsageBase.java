/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.android.support.design.Cheeses;
import com.example.android.support.design.R;
import com.example.android.support.design.Shakespeare;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;

import java.util.Random;

abstract class AppBarLayoutUsageBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        // Retrieve the Toolbar from our content view, and set it as the action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout)
                findViewById(R.id.collapsing_app_bar);
        if (collapsingToolbarLayout != null) {
            if (displayTitle()) {
                collapsingToolbarLayout.setTitle(getTitle());
            }
            collapsingToolbarLayout.setContentScrimColor(0xFFFF00FF);
        }

        TextView dialog = findViewById(R.id.textview_dialogue);
        if (dialog != null) {
            dialog.setText(TextUtils.concat(Shakespeare.DIALOGUE));
        }

        RecyclerView recyclerView = findViewById(R.id.appbar_recyclerview);
        if (recyclerView != null) {
            setupRecyclerView(recyclerView);
        }

        TabLayout tabLayout = findViewById(R.id.tabs);
        if (tabLayout != null) {
            setupTabs(tabLayout);
        }

        final SwipeRefreshLayout refreshLayout = findViewById(R.id.swiperefresh);
        if (refreshLayout != null) {
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                private final Handler mHandler = new Handler();

                @Override
                public void onRefresh() {
                    // Post a delayed runnable to reset the refreshing state in 2 seconds
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshLayout.setRefreshing(false);
                        }
                    }, 2000);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_expand: {
                AppBarLayout abl = findViewById(R.id.app_bar);
                abl.setExpanded(true);
                return true;
            }
            case R.id.action_toggle_collapse: {
                AppBarLayout abl = findViewById(R.id.app_bar);
                abl.setExpanded(false);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void addRandomTab(TabLayout tabLayout) {
        Random r = new Random();
        String cheese = Cheeses.sCheeseStrings[r.nextInt(Cheeses.sCheeseStrings.length)];
        tabLayout.addTab(tabLayout.newTab().setText(cheese));
    }

    private void setupTabs(TabLayout tabLayout) {
        for (int i = 0; i < 10; i++) {
            addRandomTab(tabLayout);
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new SimpleStringRecyclerViewAdapter(this, Cheeses.sCheeseStrings));
    }

    protected boolean displayTitle() {
        return true;
    }

    protected abstract int getLayoutId();

}
