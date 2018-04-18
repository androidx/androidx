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

package com.example.android.supportv7.widget.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple RecyclerView adapter that displays every string passed in a constructor as an item.
 */
public class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.ViewHolder> {

    private int mBackground;

    private List<String> mValues;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public String mBoundString;
        public TextView mTextView;

        public ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText();
        }
    }

    public String getValueAt(int position) {
        return mValues.get(position);
    }

    public SimpleStringAdapter(Context context, String[] strings) {
        TypedValue val = new TypedValue();
        if (context.getTheme() != null) {
            context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, val, true);
        }
        mBackground = val.resourceId;
        mValues = new ArrayList<String>();
        Collections.addAll(mValues, strings);
    }

    public void swap(int pos1, int pos2) {
        String tmp = mValues.get(pos1);
        mValues.set(pos1, mValues.get(pos2));
        mValues.set(pos2, tmp);
        notifyItemRemoved(pos1);
        notifyItemInserted(pos2);
    }

    @Override
    public SimpleStringAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final ViewHolder h = new ViewHolder(new TextView(parent.getContext()));
        h.mTextView.setMinimumHeight(128);
        h.mTextView.setPadding(20, 0, 20, 0);
        h.mTextView.setFocusable(true);
        h.mTextView.setBackgroundResource(mBackground);
        RecyclerView.LayoutParams lp = getLayoutParams();
        h.mTextView.setLayoutParams(lp);
        return h;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mBoundString = mValues.get(position);
        holder.mTextView.setText(position + ":" + mValues.get(position));
        holder.mTextView.setMinHeight((200 + mValues.get(position).length() * 10));
        holder.mTextView.setBackgroundColor(getBackgroundColor(position));
    }


    /**
     * Returns LayoutParams to be used for each item in this adapter. It can be overridden
     * to provide different LayoutParams.
     * @return LayoutParams to be used for each item in this adapter.
     */
    public RecyclerView.LayoutParams getLayoutParams() {
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 10;
        lp.rightMargin = 5;
        lp.topMargin = 20;
        lp.bottomMargin = 15;
        return lp;
    }

    private int getBackgroundColor(int position) {
        switch (position % 4) {
            case 0: return Color.BLACK;
            case 1: return Color.RED;
            case 2: return Color.DKGRAY;
            case 3: return Color.BLUE;
        }
        return Color.TRANSPARENT;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public List<String> getValues() {
        return mValues;
    }

    public void setValues(List<String> values) {
        mValues = values;
    }
}
