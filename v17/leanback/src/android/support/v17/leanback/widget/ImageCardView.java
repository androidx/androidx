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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

/**
 * A subclass of {@link BaseCardView} with an {@link ImageView} as its main region.
 */
public class ImageCardView extends BaseCardView {

    private ImageView mImageView;
    private View mInfoArea;
    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mBadgeImage;
    private boolean mAttachedToWindow;

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

        if (mInfoArea != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbImageCardView,
                    defStyle, 0);
            try {
                setInfoAreaBackground(
                        a.getDrawable(R.styleable.lbImageCardView_infoAreaBackground));
            } finally {
                a.recycle();
            }
        }
    }

    /**
     * Returns the main image view.
     */
    public final ImageView getMainImageView() {
        return mImageView;
    }

    /**
     * Enables or disables adjustment of view bounds on the main image.
     */
    public void setMainImageAdjustViewBounds(boolean adjustViewBounds) {
        if (mImageView != null) {
            mImageView.setAdjustViewBounds(adjustViewBounds);
        }
    }

    /**
     * Sets the ScaleType of the main image.
     */
    public void setMainImageScaleType(ScaleType scaleType) {
        if (mImageView != null) {
            mImageView.setScaleType(scaleType);
        }
    }

    /**
     * Sets the image drawable with fade-in animation.
     */
    public void setMainImage(Drawable drawable) {
        setMainImage(drawable, true);
    }

    /**
     * Sets the image drawable with optional fade-in animation.
     */
    public void setMainImage(Drawable drawable, boolean fade) {
        if (mImageView == null) {
            return;
        }

        mImageView.setImageDrawable(drawable);
        if (drawable == null) {
            mImageView.animate().cancel();
            mImageView.setAlpha(1f);
            mImageView.setVisibility(View.INVISIBLE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            if (fade) {
                fadeIn();
            } else {
                mImageView.animate().cancel();
                mImageView.setAlpha(1f);
            }
        }
    }

    /**
     * Sets the layout dimensions of the ImageView.
     */
    public void setMainImageDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.width = width;
        lp.height = height;
        mImageView.setLayoutParams(lp);
    }

    /**
     * Returns the ImageView drawable.
     */
    public Drawable getMainImage() {
        if (mImageView == null) {
            return null;
        }

        return mImageView.getDrawable();
    }

    /**
     * Returns the info area background drawable.
     */
    public Drawable getInfoAreaBackground() {
        if (mInfoArea != null) {
            return mInfoArea.getBackground();
        }
        return null;
    }

    /**
     * Sets the info area background drawable.
     */
    public void setInfoAreaBackground(Drawable drawable) {
        if (mInfoArea != null) {
            mInfoArea.setBackground(drawable);
        }
    }

    /**
     * Sets the info area background color.
     */    
    public void setInfoAreaBackgroundColor(@ColorInt int color) {
        if (mInfoArea != null) {
            mInfoArea.setBackgroundColor(color);
        }
    }

    /**
     * Sets the title text.
     */
    public void setTitleText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }

        mTitleView.setText(text);
    }

    /**
     * Returns the title text.
     */
    public CharSequence getTitleText() {
        if (mTitleView == null) {
            return null;
        }

        return mTitleView.getText();
    }

    /**
     * Sets the content text.
     */
    public void setContentText(CharSequence text) {
        if (mContentView == null) {
            return;
        }

        mContentView.setText(text);
    }

    /**
     * Returns the content text.
     */
    public CharSequence getContentText() {
        if (mContentView == null) {
            return null;
        }

        return mContentView.getText();
    }

    /**
     * Sets the badge image drawable.
     */
    public void setBadgeImage(Drawable drawable) {
        if (mBadgeImage == null) {
            return;
        }
        mBadgeImage.setImageDrawable(drawable);
        if (drawable != null && mContentView!= null && mContentView.getVisibility() != GONE) {
            mBadgeImage.setVisibility(View.VISIBLE);
        } else {
            mBadgeImage.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the badge image drawable.
     */
    public Drawable getBadgeImage() {
        if (mBadgeImage == null) {
            return null;
        }

        return mBadgeImage.getDrawable();
    }

    private void fadeIn() {
        mImageView.setAlpha(0f);
        if (mAttachedToWindow) {
            mImageView.animate().alpha(1f).setDuration(mImageView.getResources().getInteger(
                    android.R.integer.config_shortAnimTime));
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mImageView.getAlpha() == 0) {
            fadeIn();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mImageView.animate().cancel();
        mImageView.setAlpha(1f);
        super.onDetachedFromWindow();
    }
}
