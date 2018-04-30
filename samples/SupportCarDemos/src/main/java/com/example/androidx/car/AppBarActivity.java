/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.androidx.car;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.TextListItem;

/**
 * Demo activity for creating {@code App Bar} with {@link Toolbar}.
 */
public class AppBarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_bar);
        Toolbar toolbar = findViewById(R.id.car_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Title");
        getSupportActionBar().setSubtitle("Subtitle");

        setUpList();
    }

    private void setUpList() {
        PagedListView list = findViewById(R.id.list);
        list.setAdapter(new ListItemAdapter(this, new SampleProvider(this, 50)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.demo_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private static class SampleProvider extends ListItemProvider {

        private int mCount;
        private Context mContext;

        SampleProvider(Context context, int count) {
            mContext = context;
            mCount = count;
        }

        @Override
        public ListItem get(int position) {
            if (position < 0 || position >= mCount) {
                throw new IndexOutOfBoundsException();
            }
            TextListItem item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("title");
            return item;
        }

        @Override
        public int size() {
            return mCount;
        }
    }
}
