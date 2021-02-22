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

package com.example.androidx.webkit;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;

/**
 * A {@link ListView} which serves as a menu of elements firing {@link Intent}s to other Activities.
 */
public class MenuListView extends ListView {
    private final Context mContext;
    public MenuListView(Context context) {
        super(context);
        mContext = context;
    }
    public MenuListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public MenuListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }
    @RequiresApi(21)
    public MenuListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    /**
     * An item in the {@link MenuListView}.
     */
    public static class MenuItem {
        private String mName;
        private Intent mIntent;

        public MenuItem(String name, Intent intentToLaunch) {
            mName = name;
            mIntent = intentToLaunch;
        }

        @Override
        public String toString() {
            return mName;
        }

        /**
         * Starts the {@link Intent} for this MenuItem. This accepts the {@link Context} for the
         * current Activity on the stack, which will be used as the {@code this} argument to call
         * {@link Context#startActivity(Intent)}.
         *
         * @param activityContext the Activity Context of the current Activity on the stack.
         */
        public void start(Context activityContext) {
            activityContext.startActivity(mIntent);
        }
    }

    /**
     * Sets the menu items for this {@link MenuListView}.
     */
    public void setItems(MenuItem[] items) {
        ArrayAdapter<MenuItem> featureArrayAdapter =
                new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, items);
        setAdapter(featureArrayAdapter);
        setOnItemClickListener((parent, view, position, id) ->
                featureArrayAdapter.getItem(position).start(mContext));
    }
}
