/*
 * Copyright (C) 2015 The Android Open Source Project
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


package com.example.android.supportv7.widget;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.AsyncListUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;

/**
 * A sample Activity to demonstrate capabilities of {@link AsyncListUtil}.
 */
public class AsyncListUtilActivity extends Activity {

    private static final String TAG = "AsyncListUtilActivity";

    private RecyclerView mRecyclerView;

    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRecyclerView = new RecyclerView(this);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mRecyclerView.setLayoutParams(layoutParams);
        mRecyclerView.setAdapter(new AsyncAdapter());
        setContentView(mRecyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("Layout").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mRecyclerView.requestLayout();
        return super.onOptionsItemSelected(item);
    }

    private static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public TextViewHolder(Context context) {
            super(new TextView(context));
            textView = (TextView) itemView;
        }
    }

    private class AsyncAdapter extends RecyclerView.Adapter<TextViewHolder> {

        private AsyncListUtil<String> mAsyncListUtil;

        AsyncAdapter() {
            mAsyncListUtil = new AsyncStringListUtil();
        }

        @Override
        public TextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TextViewHolder(parent.getContext());
        }

        @Override
        public void onBindViewHolder(TextViewHolder holder, int position) {
            final String itemString = mAsyncListUtil.getItem(position);
            if (itemString == null) {
                holder.textView.setText("loading...");
            } else {
                holder.textView.setText(itemString);
            }
        }

        @Override
        public int getItemCount() {
            return mAsyncListUtil.getItemCount();
        }
    }

    private class AsyncStringListUtil extends AsyncListUtil<String> {

        private static final int TILE_SIZE = 5;

        private static final long DELAY_MS = 500;

        public AsyncStringListUtil() {
            super(String.class, TILE_SIZE,
                    new AsyncListUtil.DataCallback<String>() {
                        @Override
                        public int refreshData() {
                            return Cheeses.sCheeseStrings.length;
                        }

                        @Override
                        public void fillData(String[] data, int startPosition, int itemCount) {
                            sleep();
                            for (int i = 0; i < itemCount; i++) {
                                data[i] = Cheeses.sCheeseStrings[startPosition + i];
                            }
                        }

                        private void sleep() {
                            try {
                                Thread.sleep(DELAY_MS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new AsyncListUtil.ViewCallback() {
                        @Override
                        public void getItemRangeInto(int[] outRange) {
                            outRange[0] = mLinearLayoutManager.findFirstVisibleItemPosition();
                            outRange[1] = mLinearLayoutManager.findLastVisibleItemPosition();
                        }

                        @Override
                        public void onDataRefresh() {
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }

                        @Override
                        public void onItemLoaded(int position) {
                            mRecyclerView.getAdapter().notifyItemChanged(position);
                        }
                    });

            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    onRangeChanged();
                }
            });
        }
    }
}
