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
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.ViewHolder> {

    private int mBackground;

    private ArrayList<String> mValues;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public ViewHolder(TextView v) {
            super(v);
            textView = v;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + textView.getText();
        }
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
        h.textView.setMinimumHeight(128);
        h.textView.setPadding(20, 0, 20, 0);
        h.textView.setFocusable(true);
        h.textView.setBackgroundResource(mBackground);
        return h;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(position + ":" + mValues.get(position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }
}
