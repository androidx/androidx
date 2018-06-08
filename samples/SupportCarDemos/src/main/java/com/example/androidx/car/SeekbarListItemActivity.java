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
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.SeekbarListItem;
import androidx.car.widget.SubheaderListItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link ListItem}.
 */
public class SeekbarListItemActivity extends Activity {

    PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        mPagedListView = findViewById(R.id.paged_list_view);

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

            SubheaderListItem subheaderItem;
            SeekbarListItem item;

            // Slider only.
            subheaderItem = new SubheaderListItem(mContext, "Slider Only");
            mItems.add(subheaderItem);

            item = initSeekbarListItem();
            item.setText(null);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("one line text");
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText(longText);
            mItems.add(item);


            // Start icon.
            subheaderItem = new SubheaderListItem(mContext, "With Primary Action");
            mItems.add(subheaderItem);
            // Only slider. No text.
            item = initSeekbarListItem();
            item.setText(null);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // One line text.
            item = initSeekbarListItem();
            item.setText("one line text");
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // Long text.
            item = initSeekbarListItem();
            item.setText(longText);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            mItems.add(item);

            // Clickable PrimaryActionIcon.
            item = initSeekbarListItem();
            item.setText("with clickable Primary icon");
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
            item.setPrimaryActionIconListener(v -> Toast.makeText(mContext,
                    "Primary icon clicked!", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            // End icon with divider.
            subheaderItem = new SubheaderListItem(mContext, "With Supplemental Action");
            mItems.add(subheaderItem);

            item = initSeekbarListItem();
            item.setText(null);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText(longText);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("with clickable icon");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            item.setSupplementalIconListener(v -> Toast.makeText(mContext,
                    "Supplemental icon clicked!", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            // Empty end icon with divider.
            subheaderItem = new SubheaderListItem(mContext, "With Empty Icon");
            mItems.add(subheaderItem);

            item = initSeekbarListItem();
            item.setText(null);
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("one line text");
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText(longText);
            item.setSupplementalEmptyIcon(true);
            mItems.add(item);

            // End icon without divider.
            subheaderItem = new SubheaderListItem(mContext, "Without Supplemental Action Divider");
            mItems.add(subheaderItem);

            item = initSeekbarListItem();
            item.setText(null);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText(longText);
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            // Empty end icon without divider.
            subheaderItem = new SubheaderListItem(mContext, "With Empty Icon No Divider");
            mItems.add(subheaderItem);

            item = initSeekbarListItem();
            item.setText(null);
            item.setSupplementalEmptyIcon(false);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText("one line text");
            item.setSupplementalEmptyIcon(false);
            mItems.add(item);

            item = initSeekbarListItem();
            item.setText(longText);
            item.setSupplementalEmptyIcon(false);
            mItems.add(item);

            // Secondary progress.
            subheaderItem = new SubheaderListItem(mContext, "Secondary Progress");
            mItems.add(subheaderItem);

            item = new SeekbarListItem(mContext);
            item.setMax(100);
            item.setProgress(0);
            item.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekBar.setSecondaryProgress(progress + 10);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            mItems.add(item);

            mListProvider = new ListItemProvider.ListProvider(mItems);
        }

        private SeekbarListItem initSeekbarListItem() {
            SeekbarListItem item = new SeekbarListItem(mContext);
            item.setMax(100);
            item.setProgress(0);
            item.setOnSeekBarChangeListener(mListener);
            return item;
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
