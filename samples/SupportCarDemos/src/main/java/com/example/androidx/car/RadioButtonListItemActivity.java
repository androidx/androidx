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

import androidx.car.app.CarListDialog;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.RadioButtonListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo activity for {@link androidx.car.widget.RadioButtonListItem}.
 */
public class RadioButtonListItemActivity extends Activity {

    private PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);


        mPagedListView = findViewById(R.id.paged_list_view);
        RadioButtonSelectionAdapter adapter = new RadioButtonSelectionAdapter(
                this, new ListItemProvider.ListProvider(createItems()),
                /* isSingleSelection= */ true);

        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);

        new CarListDialog.Builder(this)
                .setItems(new String[]{"single selection", "multiple selection"},
                        (dialog, position) -> adapter.setIsSingleSelection(position == 0))
                .create()
                .show();
    }

    private List<? extends ListItem> createItems() {
        List<RadioButtonListItem> items = new ArrayList<>();

        RadioButtonListItem item;

        item = new RadioButtonListItem(this);
        item.setPrimaryActionNoIcon();
        item.setTextStartMargin(R.dimen.car_keyline_3);
        item.setTitle("No icon");
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item.setTitle("Small icon - with action divider");
        item.setShowRadioButtonDivider(true);
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
        item.setTitle("Medium icon - with body text");
        item.setBody("Sample body text");
        item.setShowRadioButtonDivider(true);
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setBody("Only body text - No title, no divider, no icon.");
        items.add(item);

        item = new RadioButtonListItem(this);
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
        item.setTitle("Large icon");
        items.add(item);

        return items;
    }

    private static class RadioButtonSelectionAdapter extends ListItemAdapter {

        abstract static class SelectionController {
            static final int UNCHECKED = -1;

            abstract void setChecked(int position);

            abstract boolean isChecked(int position);
        }

        static class SingleSelectionController extends SelectionController {

            private int mLastCheckedPosition = UNCHECKED;

            @Override
            void setChecked(int position) {
                mLastCheckedPosition = position;
            }

            @Override
            boolean isChecked(int position) {
                return position == mLastCheckedPosition;
            }
        }

        static class MultiSelectionController extends SelectionController {
            private Set<Integer> mChecked = new HashSet<>();

            @Override
            void setChecked(int position) {
                mChecked.add(position);
            }

            @Override
            boolean isChecked(int position) {
                return mChecked.contains(position);
            }
        }

        private SelectionController mSelectionController;

        RadioButtonSelectionAdapter(Context context, ListItemProvider itemProvider,
                boolean isSingleSelection) {
            super(context, itemProvider, ListItemAdapter.BACKGROUND_STYLE_PANEL);
            mSelectionController = createSelectionController(isSingleSelection);
        }

        public SelectionController createSelectionController(boolean isSingleSelection) {
            return isSingleSelection
                    ? new SingleSelectionController()
                    : new MultiSelectionController();
        }

        public void setIsSingleSelection(boolean isSingleSelection) {
            mSelectionController = createSelectionController(isSingleSelection);
        }

        @Override
        public void onBindViewHolder(ListItem.ViewHolder vh, int position) {
            super.onBindViewHolder(vh, position);

            RadioButtonListItem.ViewHolder viewHolder = (RadioButtonListItem.ViewHolder) vh;

            viewHolder.getRadioButton().setChecked(mSelectionController.isChecked(position));
            viewHolder.getRadioButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                mSelectionController.setChecked(position);
                // Refresh other radio button list items.
                notifyDataSetChanged();
            });
        }
    }
}
