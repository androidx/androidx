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
import android.graphics.Color;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.SeekbarListItem;
import androidx.car.widget.TextListItem;

/**
 * Demo activity for {@link ListItem}.
 */
public class SeekbarListItemActivity extends Activity {

    PagedListView mPagedListView;

    private static int pixelToDip(Context context, int pixels) {
        return (int) (pixels / context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        mPagedListView = findViewById(R.id.paged_list_view);
        mPagedListView.setBackgroundColor(Color.BLUE);

        ListItemAdapter adapter = new ListItemAdapter(this,
                new SampleProvider(this), ListItemAdapter.BackgroundStyle.PANEL);
        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
    }

    private static class SampleProvider extends ListItemProvider {

        private Context mContext;
        SeekBar.OnSeekBarChangeListener mListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Toast.makeText(mContext, "" + progress, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        private List<ListItem> mItems;

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;
            mItems = new ArrayList<>();

            String longText = mContext.getString(R.string.long_text);

            TextListItem textListItem;
            SeekbarListItem item;

            // Slider only.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle("Slider Only");
            mItems.add(textListItem);

            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            mItems.add(item);


            // Start icon.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle("With Primary Action");
            mItems.add(textListItem);
            // Only slider. No text.
            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // One line text.
            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // Long text.
            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // End icon with divider.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle(
                    "With Supplemental Action");
            mItems.add(textListItem);
            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            // Empty end icon with divider.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle("With Empty Icon");
            mItems.add(textListItem);
            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            // End icon without divider.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle(
                    "Without Supplemental Action Divider");
            mItems.add(textListItem);
            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            // Empty end icon without divider.
            textListItem = new TextListItem(mContext);
            textListItem.setTitle(
                    "With Empty Icon No Divider");
            mItems.add(textListItem);
            item = new SeekbarListItem(mContext, 100, 0, mListener, null);
            item.setSupplementalEmptyIcon(false);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, "one line text");
            item.setSupplementalEmptyIcon(false);
            mItems.add(item);

            item = new SeekbarListItem(mContext, 100, 0, mListener, longText);
            item.setSupplementalEmptyIcon(false);
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
