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

package com.example.android.support.design.widget;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.android.support.design.R;

import java.util.ArrayList;
import java.util.Collections;

public class SimpleStringRecyclerViewAdapter
        extends RecyclerView.Adapter<SimpleStringRecyclerViewAdapter.ViewHolder> {

    private int mBackground;

    private ArrayList<String> mValues;

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

    public SimpleStringRecyclerViewAdapter(Context context, String[] strings) {
        TypedValue val = new TypedValue();
        if (context.getTheme() != null) {
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, val, true);
        }
        mBackground = val.resourceId;
        mValues = new ArrayList<>();
        Collections.addAll(mValues, strings);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        textView.setBackgroundResource(mBackground);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mBoundString = mValues.get(position);
        holder.mTextView.setText(position + ": " + mValues.get(position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }
}
