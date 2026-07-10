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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.Presenter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
        tv.setClickable(true);
        tv.setOnHoverListener((view, event) -> {
            BaseGridView gridView = null;
            View child = view;
            ViewParent p = child.getParent();
            while (p != null) {
                if (p instanceof BaseGridView) {
                    gridView = (BaseGridView) p;
                    break;
                }
                child = (View) p;
                p = (ViewParent) child.getParent();
            }
            if (gridView == null) {
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                gridView.setSelectedPositionToUnalignedChild(child);
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                gridView.setSelectedPositionToAlignedChild();
            }
            return true;
        });
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, @Nullable Object item) {
        Log.d(TAG, "onBindViewHolder for " + item);
        String text = item == null ? null : item.toString();
        ((TextView) viewHolder.view).setText(text);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}
