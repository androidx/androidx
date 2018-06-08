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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import java.util.List;
import java.util.Random;

public class CardPresenter extends Presenter {

    // String constant
    private static final String TAG = "CardPresenter";
    public static final String IMAGE = "ImageResourceId";
    public static final String TITLE = "Title";
    public static final String CONTENT = "Content";

    private static final int IMAGE_HEIGHT_DP = 120;

    private static Random sRand = new Random();
    private int mRowHeight = 0;
    private int mExpandedRowHeight = 0;

    private int mCardThemeResId;
    private Context mContextThemeWrapper;

    public CardPresenter(int cardThemeResId) {
        mCardThemeResId = cardThemeResId;
    }

    public CardPresenter() {
        mCardThemeResId = 0;
    }

    private void setupRowHeights(Context context) {
        if (mRowHeight == 0) {
            float density = context.getResources().getDisplayMetrics().density;
            int height = (int) (IMAGE_HEIGHT_DP * density + 0.5f);

            ImageCardView v = new ImageCardView(context);
            v.setMainImageDimensions(LayoutParams.WRAP_CONTENT, height);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            mRowHeight = v.getMeasuredHeight();
            v.setActivated(true);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            mExpandedRowHeight = v.getMeasuredHeight();
        }
    }

    public int getRowHeight(Context context) {
        setupRowHeights(context);
        return mRowHeight;
    }

    public int getExpandedRowHeight(Context context) {
        setupRowHeights(context);
        return mExpandedRowHeight;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        Context context = parent.getContext();
        if (mCardThemeResId != 0) {
            if (mContextThemeWrapper == null) {
                mContextThemeWrapper = new ContextThemeWrapper(context, mCardThemeResId);
            }
            context = mContextThemeWrapper;
        }
        ImageCardView v = new ImageCardView(context);
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
        final Context context = viewHolder.view.getContext();
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(),
                photoItem.getImageResourceId(), context.getTheme());
        ((ImageCardView) viewHolder.view).setMainImage(drawable);
        ((ImageCardView) viewHolder.view).setTitleText(photoItem.getTitle());
        if (!TextUtils.isEmpty(photoItem.getContent())) {
            ((ImageCardView) viewHolder.view).setContentText(photoItem.getContent());
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item, List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(viewHolder, item, payloads);
        } else {
            PhotoItem photoItem = (PhotoItem) item;
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals(IMAGE)) {
                    final Context context = viewHolder.view.getContext();
                    Drawable drawable = ResourcesCompat.getDrawable(context.getResources(),
                            photoItem.getImageResourceId(), context.getTheme());
                    ((ImageCardView) viewHolder.view).setMainImage(drawable);
                }
                if (key.equals(CONTENT)) {
                    ((ImageCardView) viewHolder.view).setContentText(photoItem.getContent());
                }
                if (key.equals(TITLE)) {
                    ((ImageCardView) viewHolder.view).setTitleText(photoItem.getTitle());
                }
            }
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}
