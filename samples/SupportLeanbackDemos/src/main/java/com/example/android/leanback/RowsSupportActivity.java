// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from RowsActivity.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.TitleHelper;
import androidx.leanback.widget.TitleView;

public class RowsSupportActivity extends FragmentActivity
{
    private RowsSupportFragment mRowsSupportFragment;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rows_support);

        mRowsSupportFragment = (RowsSupportFragment) getSupportFragmentManager().findFragmentById(
                R.id.main_rows_fragment);

        setupTitleFragment();
    }

    private void setupTitleFragment() {
        TitleView titleView = findViewById(R.id.title);
        titleView.setTitle("RowsSupportFragment");
        titleView.setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RowsSupportActivity.this, SearchSupportActivity.class);
                startActivity(intent);
            }
        });

        BrowseFrameLayout frameLayout = findViewById(R.id.rows_frame);
        TitleHelper titleHelper = new TitleHelper(frameLayout, titleView);
        frameLayout.setOnFocusSearchListener(titleHelper.getOnFocusSearchListener());
        mRowsSupportFragment.setTitleHelper(titleHelper);
    }
}
