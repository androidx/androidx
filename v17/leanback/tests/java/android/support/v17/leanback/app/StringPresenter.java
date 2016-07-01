/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.graphics.Color;
import android.support.v17.leanback.widget.Presenter;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @hide from javadoc
 */
public class StringPresenter extends Presenter {
    private static final boolean DEBUG = false;
    private static final String TAG = "StringPresenter";

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        if (DEBUG) Log.d(TAG, "onCreateViewHolder");
        TextView tv = new TextView(parent.getContext());
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 200));
        tv.setBackgroundColor(Color.CYAN);
        tv.setTextColor(Color.BLACK);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        if (DEBUG) Log.d(TAG, "onBindViewHolder for " + item.toString());
        ((TextView) viewHolder.view).setText(item.toString());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        if (DEBUG) Log.d(TAG, "onUnbindViewHolder");
    }
}
