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

package com.example.androidx.preference;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity that displays and handles launching the demo preference activities with a ListView.
 */
@SuppressWarnings("deprecation")
public class MainActivity extends android.app.ListActivity {

    private static final String INTENT = "intent";
    private static final String NAME = "name";

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SimpleAdapter adapter = new SimpleAdapter(this, getActivityList(),
                android.R.layout.simple_list_item_1, new String[]{NAME},
                new int[]{android.R.id.text1});
        setListAdapter(adapter);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        Map<String, Object> map = (Map<String, Object>) l.getItemAtPosition(position);
        Intent intent = (Intent) map.get(INTENT);
        startActivity(intent);
    }

    @NonNull
    protected List<Map<String, Object>> getActivityList() {
        List<Map<String, Object>> activityList = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory("com.example.androidx.preference.SAMPLE_CODE");

        PackageManager pm = getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            String label = info.loadLabel(pm).toString();
            addItem(activityList, label, getIntent(info));
        }
        return activityList;
    }

    private Intent getIntent(ResolveInfo info) {
        Intent result = new Intent();
        result.setClassName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
        return result;
    }

    private void addItem(List<Map<String, Object>> list, String name, Intent intent) {
        Map<String, Object> temp = new HashMap<>();
        temp.put(NAME, name);
        temp.put(INTENT, intent);
        list.add(temp);
    }
}
