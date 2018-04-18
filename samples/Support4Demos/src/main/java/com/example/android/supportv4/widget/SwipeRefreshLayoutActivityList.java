/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.android.supportv4.R;


public class SwipeRefreshLayoutActivityList extends BaseSwipeRefreshLayoutActivity {

    private ListView mList;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mList = (ListView) findViewById(R.id.content);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, TITLES);
        mList.setAdapter(arrayAdapter);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.swipe_refresh_widget_listview;
    }
}
