/*
 * Copyright 2017 The Android Open Source Project
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
package com.example.android.supportv7.widget.selection.fancy;

import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

/**
 * Access to details of an item associated with a {@link MotionEvent} instance.
 */
final class FancyDetailsLookup extends ItemDetailsLookup<Uri> {

    private final RecyclerView mRecView;

    FancyDetailsLookup(RecyclerView view) {
        mRecView = view;
    }

    @Override
    public ItemDetails<Uri> getItemDetails(MotionEvent e) {
        @Nullable View view = mRecView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            ViewHolder holder = mRecView.getChildViewHolder(view);
            if (holder instanceof FancyHolder) {
                return ((FancyHolder) holder).getItemDetails();
            }
        }
        return null;
    }
}
