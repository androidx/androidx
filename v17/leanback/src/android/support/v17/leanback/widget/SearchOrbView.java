/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v17.leanback.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * <p>A widget that draws a search affordance, represented by a round background and an icon.</p>
 *
 * Background color and icon can be customized
 */
public class SearchOrbView extends FrameLayout implements View.OnClickListener {
    private OnClickListener mListener;
    private View mSearchOrbView;
    private ImageView mIcon;
    private Drawable mIconDrawable;
    private int mSearchOrbColor, mSearchOrbColorBright;
    private final float mFocusedZoom;
    private final float mBrightnessAlpha;
    private final int mPulseDurationMs;
    private final int mScaleDownDurationMs;
    private ValueAnimator mColorAnimator;

    private final ArgbEvaluator mColorEvaluator = new ArgbEvaluator();

    private final ValueAnimator.AnimatorUpdateListener mUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            Integer color = (Integer) animator.getAnimatedValue();
            setOrbViewColor(color.intValue());
        }
    };

    private ValueAnimator mShadowFocusAnimator;

    private final ValueAnimator.AnimatorUpdateListener mFocusUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            ShadowHelper.getInstance().setZ(mSearchOrbView, animation.getAnimatedFraction());
        }
    };

    public SearchOrbView(Context context) {
        this(context, null);
    }

    public SearchOrbView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.searchOrbViewStyle);
    }

    public SearchOrbView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources res = context.getResources();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.lb_search_orb, this, true);
        mSearchOrbView = root.findViewById(R.id.search_orb);
        mIcon = (ImageView)root.findViewById(R.id.icon);

        mFocusedZoom = context.getResources().getFraction(
                R.fraction.lb_search_orb_focused_zoom, 1, 1);
        mBrightnessAlpha = context.getResources().getFraction(
                R.fraction.lb_search_orb_brightness_alpha, 1, 1);
        mPulseDurationMs = context.getResources().getInteger(
                R.integer.lb_search_orb_pulse_duration_ms);
        mScaleDownDurationMs = context.getResources().getInteger(
                R.integer.lb_search_orb_scale_down_duration_ms);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbSearchOrbView,
                defStyleAttr, 0);

        Drawable img = a.getDrawable(R.styleable.lbSearchOrbView_searchOrbIcon);
        if (img == null) {
            img = res.getDrawable(R.drawable.lb_ic_in_app_search);
        }
        setOrbIcon(img);

        int defColor = res.getColor(R.color.lb_default_search_color);
        int color = a.getColor(R.styleable.lbSearchOrbView_searchOrbColor, defColor);
        int brightColor = a.getColor(
                R.styleable.lbSearchOrbView_searchOrbBrightColor, getBrightColor(color));
        setOrbColor(color, brightColor);
        a.recycle();

        setFocusable(true);
        setClipChildren(false);
        setOnClickListener(this);

        ShadowHelper.getInstance().setZ(mSearchOrbView, 0f);
        // Icon has no background, but must be on top of the search orb view
        ShadowHelper.getInstance().setZ(mIcon, 1f);
    }

    @Override
    public void onClick(View view) {
        if (null != mListener) {
            mListener.onClick(view);
        }
    }

    private void startShadowFocusAnimation(boolean gainFocus, int duration) {
        if (mShadowFocusAnimator == null) {
            mShadowFocusAnimator = ValueAnimator.ofFloat(0f, 1f);
            mShadowFocusAnimator.addUpdateListener(mFocusUpdateListener);
        }
        if (gainFocus) {
            mShadowFocusAnimator.start();
        } else {
            mShadowFocusAnimator.reverse();
        }
        mShadowFocusAnimator.setDuration(duration);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        final float zoom = gainFocus ? mFocusedZoom : 1f;
        final int duration = gainFocus ? mPulseDurationMs : mScaleDownDurationMs;
        mSearchOrbView.animate().scaleX(zoom).scaleY(zoom).setDuration(duration).start();
        startShadowFocusAnimation(gainFocus, duration);
        enableOrbColorAnimation(gainFocus);
    }

    /**
     * Set the orb icon
     * @param icon the drawable to be used as the icon
     */
    public void setOrbIcon(Drawable icon) {
        mIconDrawable = icon;
        mIcon.setImageDrawable(mIconDrawable);
    }

    /**
     * Returns the orb icon
     * @return the drawable used as the icon
     */
    public Drawable getOrbIcon() {
        return mIconDrawable;
    }

    /**
     * Set the on click listener for the orb
     * @param listener The listener.
     */
    public void setOnOrbClickedListener(OnClickListener listener) {
        mListener = listener;
        if (null != listener) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Set the background color of the search orb.
     * @param color the RGBA color
     */
    public void setOrbColor(int color) {
        setOrbColor(color, getBrightColor(color));
    }

    public void setOrbColor(int color, int brightColor) {
        mSearchOrbColor = color;
        mSearchOrbColorBright = brightColor;

        if (mColorAnimator == null) {
            setOrbViewColor(color);
        } else {
            enableOrbColorAnimation(true);
        }
    }

    /**
     * Returns the orb color
     * @return the RGBA color
     */
    public int getOrbColor() {
        return mSearchOrbColor;
    }

    private int getBrightColor(int color) {
        final float brightnessValue = 0xff * mBrightnessAlpha;
        int red = (int)(Color.red(color) * (1 - mBrightnessAlpha) + brightnessValue);
        int green = (int)(Color.green(color) * (1 - mBrightnessAlpha) + brightnessValue);
        int blue = (int)(Color.blue(color) * (1 - mBrightnessAlpha) + brightnessValue);
        int alpha = (int)(Color.alpha(color) * (1 - mBrightnessAlpha) + brightnessValue);
        return Color.argb(alpha, red, green, blue);
    }

    private void enableOrbColorAnimation(boolean enable) {
        if (mColorAnimator != null) {
            mColorAnimator.end();
            mColorAnimator = null;
        }
        if (enable) {
            // TODO: set interpolator (material if available)
            mColorAnimator = ValueAnimator.ofObject(mColorEvaluator,
                    mSearchOrbColor, mSearchOrbColorBright, mSearchOrbColor);
            mColorAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mColorAnimator.setDuration(mPulseDurationMs * 2);
            mColorAnimator.addUpdateListener(mUpdateListener);
            mColorAnimator.start();
        }
    }

    private void setOrbViewColor(int color) {
        if (mSearchOrbView.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) mSearchOrbView.getBackground()).setColor(color);
        }
    }

}
