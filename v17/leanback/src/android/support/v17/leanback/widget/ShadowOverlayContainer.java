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
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * ShadowOverlayContainer Provides a SDK version independent wrapper container
 * to take care of shadow and/or color overlay.
 * <p>
 * Shadow and color dimmer overlay are both optional.  When shadow is used,  it's
 * user's responsibility to properly call setClipChildren(false) on parent views if
 * the shadow can appear outside bounds of parent views.
 * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
 * before using shadow.  Depending on sdk version, optical bounds might be applied
 * to parent.
 * </p>
 * <p>
 * {@link #initialize(boolean, boolean)} must be first called on the container to initialize
 * shadows and/or color overlay.  Then call {@link #wrap(View)} to insert wrapped view
 * into container.
 * </p>
 * <p>
 * Call {@link #setShadowFocusLevel(float)} to control shadow alpha.
 * </p>
 * <p>
 * Call {@link #setColorOverlayColor(int)} to control overlay color.
 * </p>
 */
public class ShadowOverlayContainer extends FrameLayout {

    private boolean mInitialized;
    private View mColorDimOverlay;
    private Object mShadowImpl;

    public ShadowOverlayContainer(Context context) {
        this(context, null, 0);
    }

    public ShadowOverlayContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowOverlayContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Return true if the platform sdk supports shadow.
     */
    public static boolean supportsShadow() {
        return ShadowHelper.getInstance().supportsShadow();
    }

    /**
     * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
     * before using shadow.  Depending on sdk version, optical bounds might be applied
     * to parent.
     */
    public static void prepareParentForShadow(ViewGroup parent) {
        ShadowHelper.getInstance().prepareParent(parent);
    }

    /**
     * Initialize shadows and/or color overlay.  Both are optional.
     */
    public void initialize(boolean hasShadow, boolean hasColorDimOverlay) {
        if (mInitialized) {
            throw new IllegalStateException();
        }
        mInitialized = true;
        if (hasShadow) {
            mShadowImpl = ShadowHelper.getInstance().addShadow(this);
        }
        if (hasColorDimOverlay) {
            mColorDimOverlay = LayoutInflater.from(getContext())
                    .inflate(R.layout.lb_card_color_overlay, this, false);
            addView(mColorDimOverlay);
        }
    }

    /**
     * Set shadow focus level (0 to 1). 0 for unfocused, 1f for fully focused.
     */
    public void setShadowFocusLevel(float level) {
        if (mShadowImpl != null) {
            if (level < 0f) {
                level = 0f;
            } else if (level > 1f) {
                level = 1f;
            }
            ShadowHelper.getInstance().setShadowFocusLevel(mShadowImpl, level);
        }
    }

    /**
     * Set color (with alpha) of the overlay.
     */
    public void setOverlayColor(int overlayColor) {
        mColorDimOverlay.setBackgroundColor(overlayColor);
    }

    /**
     * Inserts view into the wrapper.
     */
    public void wrap(View view) {
        if (!mInitialized) {
            throw new IllegalStateException();
        }
        if (mColorDimOverlay != null) {
            addView(view, indexOfChild(mColorDimOverlay));
        } else {
            addView(view);
        }
    }

}
