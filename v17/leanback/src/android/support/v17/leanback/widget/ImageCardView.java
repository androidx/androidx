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
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageCardView extends BaseCardView {

    private ImageView mImageView;
    private View mInfoArea;
    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mBadgeImage;
    private ImageView mBadgeFadeMask;

    public ImageCardView(Context context) {
        this(context, null);
    }

    public ImageCardView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.imageCardViewStyle);
    }

    public ImageCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.lb_image_card_view, this);

        mImageView = (ImageView) v.findViewById(R.id.main_image);
        mImageView.setVisibility(View.INVISIBLE);
        mInfoArea = v.findViewById(R.id.info_field);
        mTitleView = (TextView) v.findViewById(R.id.title_text);
        mContentView = (TextView) v.findViewById(R.id.content_text);
        mBadgeImage = (ImageView) v.findViewById(R.id.extra_badge);
        mBadgeFadeMask = (ImageView) v.findViewById(R.id.fade_mask);
    }

    public void setMainImage(Drawable drawable) {
        if (mImageView == null) {
            return;
        }

        mImageView.setImageDrawable(drawable);
        if (drawable == null) {
            mImageView.setVisibility(View.INVISIBLE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            fadeIn(mImageView);
        }
    }

    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mImageView.setLayoutParams(lp);
    }

    public Drawable getMainImage() {
        if (mImageView == null) {
            return null;
        }

        return mImageView.getDrawable();
    }

    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }

        mTitleView.setText(text);
        setTextMaxLines();
    }

    public CharSequence getTitleText() {
        if (mTitleView == null) {
            return null;
        }

        return mTitleView.getText();
    }

    public void setContentText(CharSequence text) {
        if (mContentView == null) {
            return;
        }

        mContentView.setText(text);
        setTextMaxLines();
    }

    public CharSequence getContentText() {
        if (mContentView == null) {
            return null;
        }

        return mContentView.getText();
    }

    public void setBadgeImage(Drawable drawable) {
        if (mBadgeImage == null) {
            return;
        }

        if (drawable != null) {
            mBadgeImage.setImageDrawable(drawable);
            mBadgeImage.setVisibility(View.VISIBLE);
            mBadgeFadeMask.setVisibility(View.VISIBLE);
        } else {
            mBadgeImage.setVisibility(View.GONE);
            mBadgeFadeMask.setVisibility(View.GONE);
        }
    }

    public Drawable getBadgeImage() {
        if (mBadgeImage == null) {
            return null;
        }

        return mBadgeImage.getDrawable();
    }

    private void fadeIn(View v) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(v.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime)).start();
    }

    private void setTextMaxLines() {
        if (TextUtils.isEmpty(getTitleText())) {
            mContentView.setMaxLines(2);
        } else {
            mContentView.setMaxLines(1);
        }
        if (TextUtils.isEmpty(getContentText())) {
            mTitleView.setMaxLines(2);
        } else {
            mTitleView.setMaxLines(1);
        }
    }
}
