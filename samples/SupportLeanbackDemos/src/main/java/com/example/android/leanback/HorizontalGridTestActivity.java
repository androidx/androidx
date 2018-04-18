/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.leanback;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalGridTestActivity extends Activity {
    private static final String TAG = "HorizontalGridTestActivity";
    private static final boolean DEBUG = true;
    private static final String SELECT_ACTION = "android.test.leanback.widget.SELECT";
    private static final int NUM_ITEMS = 100;
    private static final boolean STAGGERED = true;

    private HorizontalGridView mHorizontalGridView;

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (DEBUG) {
                final String[] stateNames = { "IDLE", "DRAGGING", "SETTLING" };
                Log.v(TAG, "onScrollStateChanged "
                        + (newState < stateNames.length ? stateNames[newState] : newState));
            }
        }
    };

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.horizontal_grid, null, false);
        mHorizontalGridView = (HorizontalGridView) view.findViewById(R.id.gridview);

        mHorizontalGridView.setWindowAlignment(HorizontalGridView.WINDOW_ALIGN_BOTH_EDGE);
        mHorizontalGridView.setWindowAlignmentOffsetPercent(35);
        mHorizontalGridView.setOnChildViewHolderSelectedListener(
                new OnChildViewHolderSelectedListener() {
                    @Override
                    public void onChildViewHolderSelected(RecyclerView parent,
                                                          RecyclerView.ViewHolder child,
                                                          int position, int subposition) {
                        if (DEBUG) Log.d(TAG, "onChildSelected position=" + position);
                    }

                });
        return view;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DEBUG) Log.v(TAG, "onCreate");

        RecyclerView.Adapter adapter = new MyAdapter();

        View view = createView();

        mHorizontalGridView.setAdapter(new MyAdapter());
        setContentView(view);

        mHorizontalGridView.addOnScrollListener(mScrollListener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.v(TAG, "onNewIntent " + intent);
        if (intent.getAction().equals(SELECT_ACTION)) {
            int position = intent.getIntExtra("SELECT_POSITION", -1);
            if (position >= 0) {
                mHorizontalGridView.setSelectedPosition(position);
            }
        }
        super.onNewIntent(intent);
    }

    private OnFocusChangeListener mItemFocusChangeListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                v.setBackgroundColor(Color.YELLOW);
            } else {
                v.setBackgroundColor(Color.LTGRAY);
            }
        }
    };

    private OnClickListener mItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mHorizontalGridView.getAdapter().notifyDataSetChanged();
        }
    };

    class MyAdapter extends RecyclerView.Adapter {

        private int[] mItemLengths;

        MyAdapter() {
            mItemLengths = new int[NUM_ITEMS];
            for (int i = 0; i < mItemLengths.length; i++) {
                mItemLengths[i] = STAGGERED ? (int)(Math.random() * 180) + 180 : 240;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (DEBUG) Log.v(TAG, "createViewHolder " + viewType);
            TextView textView = new TextView(parent.getContext());
            textView.setTextColor(Color.BLACK);
            textView.setFocusable(true);
            textView.setFocusableInTouchMode(true);
            textView.setOnFocusChangeListener(mItemFocusChangeListener);
            textView.setOnClickListener(mItemClickListener);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder baseHolder, int position) {
            if (DEBUG) Log.v(TAG, "bindViewHolder " + position + " " + baseHolder);
            ViewHolder holder = (ViewHolder) baseHolder;
            ((TextView) holder.itemView).setText("Item "+position);
            holder.itemView.setBackgroundColor(Color.LTGRAY);
            holder.itemView.setLayoutParams(new ViewGroup.MarginLayoutParams(mItemLengths[position],
                    80));
        }

        @Override
        public int getItemCount() {
            return mItemLengths.length;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }
}
