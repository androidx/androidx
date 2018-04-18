/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.Presenter;

public class StringPresenter extends Presenter {
    private static final String TAG = "StringPresenter";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        final Context context = parent.getContext();
        TextView tv = new TextView(context);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.text_bg,
                context.getTheme()));
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Log.d(TAG, "onBindViewHolder for " + item.toString());
        ((TextView) viewHolder.view).setText(item.toString());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}
