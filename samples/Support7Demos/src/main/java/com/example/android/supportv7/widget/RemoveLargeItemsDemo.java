/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Test activity that displays behavior when all or some visible items are removed from a
 * LinearLayoutManager.
 */
public class RemoveLargeItemsDemo extends Activity {

    RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    MyAdapter mAdapter;
    private int mNumItemsAdded = 0;
    ArrayList<Item> mItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remove_large_items_demo);

        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        for (int i = 0; i < 6; ++i) {
            mItems.add(new Item("Item #" + i));
        }
        mAdapter = new MyAdapter(mItems);
        mRecyclerView.setAdapter(mAdapter);

        ((ViewGroup) findViewById(R.id.container)).addView(mRecyclerView);

        CheckBox reverseLayout = findViewById(R.id.reverse);
        reverseLayout.setOnCheckedChangeListener(
                (buttonView, isChecked) -> mLinearLayoutManager.setReverseLayout(isChecked)
        );

        CheckBox enableStackFromEnd = findViewById(R.id.enableStackFromEnd);
        enableStackFromEnd.setOnCheckedChangeListener(
                (buttonView, isChecked) -> mLinearLayoutManager.setStackFromEnd(isChecked)
        );
    }

    /**
     * Called by xml when a check box is checked.
     */
    public void checkboxClicked(@NonNull View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        boolean selected = ((CheckBox) view).isChecked();
        MyViewHolder holder = (MyViewHolder) mRecyclerView.getChildViewHolder(parent);
        mAdapter.selectItem(holder, selected);
    }

    /**
     * Called by xml when a item is clicked.
     */
    public void itemClicked(@NonNull View view) {
        ViewGroup parent = (ViewGroup) view;
        MyViewHolder holder = (MyViewHolder) mRecyclerView.getChildViewHolder(parent);
        final int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        mAdapter.toggleExpanded(holder);
        mAdapter.notifyItemChanged(position);
    }

    /**
     * Called by xml onClick to delete items that have been checked.
     */
    public void deleteSelectedItems(@NonNull View view) {
        int numItems = mItems.size();
        if (numItems > 0) {
            for (int i = numItems - 1; i >= 0; --i) {
                final Item item = mItems.get(i);
                //noinspection ConstantConditions
                Boolean selected = mAdapter.mSelected.get(item);
                if (selected != null && selected) {
                    removeAtPosition(i);
                }
            }
        }
    }

    private void removeAtPosition(int position) {
        if (position < mItems.size()) {
            mItems.remove(position);
            mAdapter.notifyItemRemoved(position);
        }
    }

    private void addAtPosition(String text) {
        int position = 3;
        if (position > mItems.size()) {
            position = mItems.size();
        }
        Item item = new Item(text);
        mItems.add(position, item);
        mAdapter.mSelected.put(item, Boolean.FALSE);
        mAdapter.mExpanded.put(item, Boolean.FALSE);
        mAdapter.notifyItemInserted(position);
    }

    /**
     * Animates an item in.
     */
    public void addItem(@NonNull View view) {
        addAtPosition("Added Item #" + mNumItemsAdded++);
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private int mBackground;
        List<Item> mData;
        ArrayMap<Item, Boolean> mSelected = new ArrayMap<>();
        ArrayMap<Item, Boolean> mExpanded = new ArrayMap<>();

        MyAdapter(List<Item> data) {
            TypedValue val = new TypedValue();
            RemoveLargeItemsDemo.this.getTheme().resolveAttribute(
                    androidx.appcompat.R.attr.selectableItemBackground, val, true);
            mBackground = val.resourceId;
            mData = data;
        }

        @NotNull
        @Override
        public MyViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
            MyViewHolder h =
                    new MyViewHolder(getLayoutInflater().inflate(
                            R.layout.remove_large_items_demo_item,
                            mRecyclerView, false));
            h.textView.setMinimumHeight(128);
            h.textView.setFocusable(true);
            h.textView.setBackgroundResource(mBackground);
            return h;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NotNull MyViewHolder myViewHolder, int position) {
            Item item = mData.get(position);
            myViewHolder.boundItem = item;
            myViewHolder.textView.setText(item.mString);
            Boolean selected = mSelected.get(item);
            if (selected == null) {
                selected = false;
            }
            myViewHolder.checkBox.setChecked(selected);
            Boolean expanded = mExpanded.get(item);
            if (Boolean.TRUE.equals(expanded)) {
                myViewHolder.textView.setText("More text for the expanded version");
            } else {
                myViewHolder.textView.setText(item.mString);
                myViewHolder.container.setBackgroundColor(item.mColor);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void selectItem(MyViewHolder holder, boolean selected) {
            mSelected.put(holder.boundItem, selected);
        }

        public void toggleExpanded(MyViewHolder holder) {
            Boolean expanded = mExpanded.get(holder.boundItem);
            if (expanded == null) {
                expanded = false;
            }
            mExpanded.put(holder.boundItem, !expanded);
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public CheckBox checkBox;
        public View container;
        public Item boundItem;

        MyViewHolder(View v) {
            super(v);
            container = v;
            textView = v.findViewById(R.id.text);
            checkBox = v.findViewById(R.id.selected);
        }

        @Override
        @NonNull
        public String toString() {
            return super.toString() + " \"" + textView.getText() + "\"";
        }
    }

    static class Item {
        public String mString;
        public int mColor;

        Item(String string) {
            mString = string;
            Random rnd = new Random();
            mColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        }
    }
}
