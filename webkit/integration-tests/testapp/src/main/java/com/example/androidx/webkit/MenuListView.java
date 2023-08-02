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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A {@link ListView} which serves as a menu of elements firing {@link Intent}s to other Activities.
 *
 * Not overriding {@link ListView#ListView(Context, AttributeSet, int, int)} since it only
 * applies to API version 21, and we compile this apk for 19.
 */
public class MenuListView extends ListView {
    public MenuListView(@NonNull Context context) {
        super(context);
    }
    public MenuListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public MenuListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * An item in the {@link MenuListView}.
     */
    public static class MenuItem {
        private String mName;
        private Intent mIntent;

        public MenuItem(@NonNull String name, @NonNull Intent intentToLaunch) {
            mName = name;
            mIntent = intentToLaunch;
        }

        @Override
        @NonNull
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
        public void start(@NonNull Context activityContext) {
            activityContext.startActivity(mIntent);
        }
    }

    /**
     * Sets the menu items for this {@link MenuListView}.
     */
    public void setItems(@NonNull MenuItem[] items) {
        final Context context = getContext();
        ArrayAdapter<MenuItem> featureArrayAdapter =
                new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        setAdapter(featureArrayAdapter);
        setOnItemClickListener((parent, view, position, id) ->
                featureArrayAdapter.getItem(position).start(context));
    }
}
