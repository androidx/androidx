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

            // Slider only.
            mItems.add(new TextListItem.Builder(mContext).withTitle("Slider Only").build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .build());


            // Start icon.
            mItems.add(new TextListItem.Builder(mContext).withTitle("With Primary Action").build());
            // Only slider. No text.
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon)
                    .build());

            // One line text.
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon)
                    .build());

            // Long text.
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon)
                    .build());

            // End icon with divider.
            mItems.add(new TextListItem.Builder(mContext).withTitle(
                    "With Supplemental Action").build());
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                    .build());

            // Empty end icon with divider.
            mItems.add(new TextListItem.Builder(mContext).withTitle("With Empty Icon").build());
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .withSupplementalEmptyIcon(true)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .withSupplementalEmptyIcon(true)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .withSupplementalEmptyIcon(true)
                    .build());

            // End icon without divider.
            mItems.add(new TextListItem.Builder(mContext).withTitle(
                    "Without Supplemental Action Divider").build());
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false)
                    .build());

            // Empty end icon without divider.
            mItems.add(new TextListItem.Builder(mContext).withTitle(
                    "With Empty Icon No Divider").build());
            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, null)
                    .withSupplementalEmptyIcon(false)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, "one line text")
                    .withSupplementalEmptyIcon(false)
                    .build());

            mItems.add(new SeekbarListItem.Builder(mContext, 100, 0, mListener, longText)
                    .withSupplementalEmptyIcon(false)
                    .build());

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
