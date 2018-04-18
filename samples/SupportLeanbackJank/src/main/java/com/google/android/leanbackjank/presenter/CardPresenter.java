/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.google.android.leanbackjank.presenter;

import android.net.Uri;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.leanbackjank.R;
import com.google.android.leanbackjank.model.VideoInfo;

public class CardPresenter extends Presenter {
    private int mSelectedBackgroundColor = -1;
    private int mDefaultBackgroundColor = -1;
    private int mCardWidth;
    private int mCardHeight;

    public CardPresenter(int width, int height) {
        mCardWidth = width;
        mCardHeight = height;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mDefaultBackgroundColor =
                ResourcesCompat.getColor(parent.getResources(), R.color.jank_blue, null);
        mSelectedBackgroundColor =
                ResourcesCompat.getColor(parent.getResources(), R.color.jank_red, null);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                findViewById(R.id.info_field).setBackgroundColor(
                        selected ? mSelectedBackgroundColor : mDefaultBackgroundColor);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        VideoInfo videoInfo = (VideoInfo) item;

        final ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setTitleText(videoInfo.getTitle());
        cardView.setContentText(videoInfo.getStudio());
        cardView.setMainImageDimensions(mCardWidth, mCardHeight);
        cardView.setBackgroundColor(mDefaultBackgroundColor);

        Glide.with(cardView.getContext())
                .load(videoInfo.getImageUri())
                .listener(new RequestListener<Uri, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, Uri uri, Target<GlideDrawable> target,
                            boolean b) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable glideDrawable, Uri uri,
                            Target<GlideDrawable> target, boolean b, boolean b1) {
                        // Remove the background color to reduce overdraw.
                        cardView.setBackground(null);
                        return false;
                    }
                })
                .into(cardView.getMainImageView());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
