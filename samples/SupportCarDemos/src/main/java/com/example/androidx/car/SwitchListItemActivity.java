/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.car.widget.CarToolbar;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.SwitchListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link androidx.car.widget.SwitchListItem}.
 */
public class SwitchListItemActivity extends Activity {

    private PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setTitle(R.string.switch_list_item_title);
        toolbar.setNavigationIconOnClickListener(v -> finish());

        mPagedListView = findViewById(R.id.paged_list_view);

        SampleProvider provider = new SampleProvider(this);
        ListItemAdapter adapter = new ListItemAdapter(this, provider);

        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);

        SwitchListItem item = new SwitchListItem(this);
        item.setTitle("Clicking me to set checked state of item above");
        item.setSwitchOnCheckedChangeListener((buttonView, isChecked) -> {
            int size = adapter.getItemCount();
            // -2 to get second to last item (the one above).
            ((SwitchListItem) provider.mItems.get(size - 2)).setChecked(isChecked);
            adapter.notifyDataSetChanged();
        });
        provider.mItems.add(item);
    }

    private static class SampleProvider extends ListItemProvider {
        private Context mContext;

        private final List<ListItem> mItems = new ArrayList<>();
        private final ListItemProvider.ListProvider mListProvider =
                new ListItemProvider.ListProvider(mItems);
        private final CompoundButton.OnCheckedChangeListener mListener = (button, isChecked) ->
                Toast.makeText(mContext,
                        "Switch is " + (isChecked ? "checked" : "unchecked"),
                        Toast.LENGTH_SHORT).show();

        SampleProvider(Context context) {
            mContext = context;

            String longText = mContext.getString(R.string.long_text);

            SwitchListItem item;

            item = new SwitchListItem(mContext);
            item.setTitle("Title - show divider");
            item.setShowSwitchDivider(true);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setBody("Body text");
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setTitle("Long body text");
            item.setBody(longText);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    SwitchListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("Switch with Icon");
            item.setBody(longText);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setTitle("Switch with Drawable");
            item.setPrimaryActionIcon(
                    mContext.getDrawable(android.R.drawable.sym_def_app_icon),
                    SwitchListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setBody(longText);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setTitle("Clicking item toggles switch");
            item.setClickable(true);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);

            item = new SwitchListItem(mContext);
            item.setTitle("Disabled item");
            item.setEnabled(false);
            item.setSwitchOnCheckedChangeListener(mListener);
            mItems.add(item);
        }

        @Override
        public ListItem get(int position) {
            return mListProvider.get(position);
        }

        @Override
        public int size() {
            return mListProvider.size();
        }
    }
}

