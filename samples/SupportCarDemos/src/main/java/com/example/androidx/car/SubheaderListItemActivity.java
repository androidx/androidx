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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.SubheaderListItem;
import androidx.car.widget.TextListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link SubheaderListItem}.
 */
public class SubheaderListItemActivity extends Activity {

    PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        mPagedListView = findViewById(R.id.paged_list_view);

        ListItemAdapter adapter = new ListItemAdapter(this,
                new SampleProvider(this), ListItemAdapter.BackgroundStyle.SOLID);
        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
        mPagedListView.setDividerVisibilityManager(adapter);
    }

    private static class SampleProvider extends ListItemProvider {

        private Context mContext;
        private List<ListItem> mItems;

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;
            mItems = new ArrayList<>();

            SubheaderListItem subheaderItem;
            TextListItem item;

            subheaderItem = new SubheaderListItem(mContext,
                    "subheader matching items without start margin");
            subheaderItem.setTextStartMarginType(SubheaderListItem.TEXT_START_MARGIN_TYPE_NONE);
            subheaderItem.setHideDivider(true);
            mItems.add(subheaderItem);

            item = new TextListItem(mContext);
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("item - hides following divider");
            item.setHideDivider(true);
            mItems.add(item);

            // ========================

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("Header");
            item.setBody("header text with more words");
            item.setHideDivider(true);
            mItems.add(item);

            subheaderItem = new SubheaderListItem(mContext,
                    "subheader matching items with no icon");
            subheaderItem.setTextStartMarginType(
                    SubheaderListItem.TEXT_START_MARGIN_TYPE_SMALL);
            subheaderItem.setHideDivider(true);
            mItems.add(subheaderItem);

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("item - hides following divider");
            item.setHideDivider(true);
            mItems.add(item);

            // ========================

            subheaderItem = new SubheaderListItem(mContext,
                    "subheader matching items with small icons");
            subheaderItem.setTextStartMarginType(
                    SubheaderListItem.TEXT_START_MARGIN_TYPE_SMALL);
            mItems.add(subheaderItem);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("item");
            mItems.add(item);

            // ========================

            subheaderItem = new SubheaderListItem(mContext,
                    "subheader matching items with large icons");
            subheaderItem.setTextStartMarginType(
                    SubheaderListItem.TEXT_START_MARGIN_TYPE_LARGE);
            mItems.add(subheaderItem);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
            item.setTitle("item");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
            item.setTitle("item");
            mItems.add(item);

            mListProvider = new ListItemProvider.ListProvider(mItems);
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
