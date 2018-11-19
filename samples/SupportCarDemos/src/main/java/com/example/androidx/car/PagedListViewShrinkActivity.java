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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.car.widget.CarToolbar;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for scroll bar not shown when there is no space in PagedListView.
 */
public class PagedListViewShrinkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_shrink_view);

        Resources res = this.getResources();
        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setTitle(R.string.paged_list_view_title);
        toolbar.setNavigationIconOnClickListener(v -> finish());

        PagedListView pagedListView = findViewById(R.id.paged_list_view);
        pagedListView.setAdapter(new DemoAdapter());

        RadioGroup radioGroup = findViewById(R.id.radiogroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                ViewGroup.LayoutParams params = pagedListView.getLayoutParams();

                switch(checkedId) {
                    case R.id.show_nothing:
                        params.height = res.getDimensionPixelSize(
                                androidx.car.R.dimen.car_padding_5);
                        break;
                    case R.id.show_buttons:
                        params.height = 2 * res.getDimensionPixelSize(
                                androidx.car.R.dimen.car_single_line_list_item_height);
                        break;
                    case R.id.show_everything:
                        params.height = 2 * res.getDimensionPixelSize(
                                androidx.car.R.dimen.car_keyline_4);
                        break;
                    default:
                        break;
                }
                pagedListView.requestLayout();
            }
        });
    }

    /**
     * Adapter that populates a number of items which include EditText, so that the soft keyboard
     * can come up when user edit them.
     */
    private static class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {
        private static final int SIZE = 80;
        private final List<String> mItems = new ArrayList<>(SIZE);

        private DemoAdapter() {
            for (int i = 0; i < SIZE; i++) {
                mItems.add("Item " + i);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.paged_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        /**
         * ViewHolder for DemoAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mTextView;

            ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.text);
            }
        }
    }
}
