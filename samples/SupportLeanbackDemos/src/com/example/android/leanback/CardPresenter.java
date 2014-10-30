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
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import java.util.Random;

public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static final int IMAGE_HEIGHT_DP = 120;

    private static Random sRand = new Random();
    private static int sRowHeight = 0;
    private static int sExpandedRowHeight = 0;

    private static void setupRowHeights(Context context) {
        if (sRowHeight == 0) {
            float density = context.getResources().getDisplayMetrics().density;
            int height = (int) (IMAGE_HEIGHT_DP * density + 0.5f);

            ImageCardView v = new ImageCardView(context);
            v.setMainImageDimensions(LayoutParams.WRAP_CONTENT, height);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            sRowHeight = v.getMeasuredHeight();
            v.setActivated(true);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            sExpandedRowHeight = v.getMeasuredHeight();
        }
    }

    public static int getRowHeight(Context context) {
        setupRowHeights(context);
        return sRowHeight;
    }

    public static int getExpandedRowHeight(Context context) {
        setupRowHeights(context);
        return sExpandedRowHeight;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        ImageCardView v = new ImageCardView(parent.getContext());
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        // Randomly makes image view crop as a square or just stretch to original
        // aspect ratio.
        if (sRand.nextBoolean()) {
            v.setMainImageAdjustViewBounds(false);
            v.setMainImageDimensions(getRowHeight(parent.getContext()),
                    getRowHeight(parent.getContext()));
        } else {
            v.setMainImageAdjustViewBounds(true);
            v.setMainImageDimensions(LayoutParams.WRAP_CONTENT,
                    getRowHeight(parent.getContext()));
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Log.d(TAG, "onBindViewHolder for " + item.toString());
        PhotoItem photoItem = (PhotoItem) item;
        Drawable drawable =  viewHolder.view.getContext().getResources()
                .getDrawable(photoItem.getImageResourceId());
        ((ImageCardView) viewHolder.view).setMainImage(drawable);
        ((ImageCardView) viewHolder.view).setTitleText(photoItem.getTitle());
        if (!TextUtils.isEmpty(photoItem.getContent())) {
            ((ImageCardView) viewHolder.view).setContentText(photoItem.getContent());
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}
