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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.android.support.design.Cheeses;
import com.example.android.support.design.R;

import java.util.ArrayList;
import java.util.Random;

public class BottomSheetDynamicContent extends AppCompatActivity {

    private DynamicAdapter mAdapter;

    private BottomSheetBehavior<RecyclerView> mBehavior;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.add:
                    if (mAdapter != null) {
                        mAdapter.add();
                        mAdapter.notifyDataSetChanged();
                        if (mBehavior != null) {
                            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    }
                    break;
                case R.id.remove:
                    if (mAdapter != null) {
                        mAdapter.remove();
                        mAdapter.notifyDataSetChanged();
                        if (mBehavior != null) {
                            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                    }
                    break;
                case R.id.expand:
                    if (mBehavior != null) {
                        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                    break;
                case R.id.collapse:
                    if (mBehavior != null) {
                        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_bottom_sheet_dynamic);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DynamicAdapter();
        for (int i = 0; i < 5; i++) {
            mAdapter.add();
        }
        list.setAdapter(mAdapter);
        mBehavior = BottomSheetBehavior.from(list);

        Button add = (Button) findViewById(R.id.add);
        if (add != null) {
            add.setOnClickListener(mOnClickListener);
        }
        Button remove = (Button) findViewById(R.id.remove);
        if (remove != null) {
            remove.setOnClickListener(mOnClickListener);
        }
        Button expand = (Button) findViewById(R.id.expand);
        if (expand != null) {
            expand.setOnClickListener(mOnClickListener);
        }
        Button collapse = (Button) findViewById(R.id.collapse);
        if (collapse != null) {
            collapse.setOnClickListener(mOnClickListener);
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(android.R.layout.simple_list_item_1, parent, false));
            text = (TextView) itemView.findViewById(android.R.id.text1);
        }

    }

    private static class DynamicAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final ArrayList<String> mCheeses = new ArrayList<>();
        private final Random mRandom = new Random(System.currentTimeMillis());

        void add() {
            mCheeses.add(Cheeses.sCheeseStrings[mRandom.nextInt(Cheeses.sCheeseStrings.length)]);
        }

        void remove() {
            mCheeses.remove(mCheeses.size() - 1);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(mCheeses.get(position));
        }

        @Override
        public int getItemCount() {
            return mCheeses.size();
        }

    }

}
