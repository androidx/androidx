/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.support.appnavigation.app;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Home activity for app navigation code samples.
 */
public class AppNavHomeActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new SampleAdapter(querySampleActivities()));
    }

    @Override
    protected void onListItemClick(ListView lv, View v, int pos, long id) {
        SampleInfo info = (SampleInfo) getListAdapter().getItem(pos);
        startActivity(info.intent);
    }

    protected List<SampleInfo> querySampleActivities() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setPackage(getPackageName());
        intent.addCategory(Intent.CATEGORY_SAMPLE_CODE);

        PackageManager pm = getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

        ArrayList<SampleInfo> samples = new ArrayList<SampleInfo>();

        final int count = infos.size();
        for (int i = 0; i < count; i++) {
            final ResolveInfo info = infos.get(i);
            final CharSequence labelSeq = info.loadLabel(pm);
            String label = labelSeq != null ? labelSeq.toString() : info.activityInfo.name;

            Intent target = new Intent();
            target.setClassName(info.activityInfo.applicationInfo.packageName,
                    info.activityInfo.name);
            SampleInfo sample = new SampleInfo(label, target);
            samples.add(sample);
        }

        return samples;
    }

    static class SampleInfo {
        String name;
        Intent intent;

        SampleInfo(String name, Intent intent) {
            this.name = name;
            this.intent = intent;
        }
    }

    class SampleAdapter extends BaseAdapter {
        private List<SampleInfo> mItems;

        public SampleAdapter(List<SampleInfo> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1,
                        parent, false);
                convertView.setTag(convertView.findViewById(android.R.id.text1));
            }
            TextView tv = (TextView) convertView.getTag();
            tv.setText(mItems.get(position).name);
            return convertView;
        }

    }
}
