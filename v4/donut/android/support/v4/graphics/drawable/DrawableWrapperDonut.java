/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

/**
 * Drawable which delegates all calls to it's wrapped {@link android.graphics.drawable.Drawable}.
 * <p>
 * Also allows backward compatible tinting via a color or {@link ColorStateList}.
 * This functionality is accessed via static methods in {@code DrawableCompat}.
 */
class DrawableWrapperDonut extends Drawable implements Drawable.Callback, DrawableWrapper {

    static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private ColorStateList mTintList;
    private PorterDuff.Mode mTintMode = DEFAULT_MODE;

    private int mCurrentColor;
    private PorterDuff.Mode mCurrentMode;
    private boolean mColorFilterSet;

    Drawable mDrawable;

    DrawableWrapperDonut(Drawable drawable) {
        setWrappedDrawable(drawable);
    }

    @Override
    public void draw(Canvas canvas) {
        mDrawable.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mDrawable.setBounds(bounds);
    }

    @Override
    public void setChangingConfigurations(int configs) {
        mDrawable.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return mDrawable.getChangingConfigurations();
    }

    @Override
    public void setDither(boolean dither) {
        mDrawable.setDither(dither);
    }

    @Override
    public void setFilterBitmap(boolean filter) {
        mDrawable.setFilterBitmap(filter);
    }

    @Override
    public void setAlpha(int alpha) {
        mDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mDrawable.setColorFilter(cf);
    }

    @Override
    public boolean isStateful() {
        final ColorStateList tintList = isCompatTintEnabled() ? mTintList : null;
        return (tintList != null && tintList.isStateful()) || mDrawable.isStateful();
    }

    @Override
    public boolean setState(final int[] stateSet) {
        boolean handled = mDrawable.setState(stateSet);
        handled = updateTint(stateSet) || handled;
        return handled;
    }

    @Override
    public int[] getState() {
        return mDrawable.getState();
    }

    @Override
    public Drawable getCurrent() {
        return mDrawable.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return super.setVisible(visible, restart) || mDrawable.setVisible(visible, restart);
    }

    @Override
    public int getOpacity() {
        return mDrawable.getOpacity();
    }

    @Override
    public Region getTransparentRegion() {
        return mDrawable.getTransparentRegion();
    }

    @Override
    public int getIntrinsicWidth() {
        return mDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mDrawable.getIntrinsicHeight();
    }

    @Override
    public int getMinimumWidth() {
        return mDrawable.getMinimumWidth();
    }

    @Override
    public int getMinimumHeight() {
        return mDrawable.getMinimumHeight();
    }

    @Override
    public boolean getPadding(Rect padding) {
        return mDrawable.getPadding(padding);
    }

    @Override
    public Drawable mutate() {
        Drawable wrapped = mDrawable;
        Drawable mutated = wrapped.mutate();
        if (mutated != wrapped) {
            // If mutate() returned a new instance, update our reference
            setWrappedDrawable(mutated);
        }
        // We return ourselves, since only the wrapped drawable needs to mutate
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    /**
     * {@inheritDoc}
     */
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mDrawable.setLevel(level);
    }

    @Override
    public void setCompatTint(int tint) {
        setCompatTintList(ColorStateList.valueOf(tint));
    }

    @Override
    public void setCompatTintList(ColorStateList tint) {
        if (mTintList != tint) {
            mTintList = tint;
            updateTint(getState());
        }
    }

    @Override
    public void setCompatTintMode(PorterDuff.Mode tintMode) {
        if (mTintMode != tintMode) {
            mTintMode = tintMode;
            updateTint(getState());
        }
    }

    private boolean updateTint(int[] state) {
        if (!isCompatTintEnabled()) {
            // If compat tinting is not enabled, fail fast
            return false;
        }

        if (mTintList != null && mTintMode != null) {
            final int color = mTintList.getColorForState(state, mTintList.getDefaultColor());
            if (!mColorFilterSet || color != mCurrentColor || mTintMode != mCurrentMode) {
                setColorFilter(color, mTintMode);
                mCurrentColor = color;
                mCurrentMode = mTintMode;
                mColorFilterSet = true;
                return true;
            }
        } else {
            mColorFilterSet = false;
            clearColorFilter();
        }
        return false;
    }

    /**
     * Returns the wrapped {@link Drawable}
     */
    public Drawable getWrappedDrawable() {
        return mDrawable;
    }

    /**
     * Sets the current wrapped {@link Drawable}
     */
    public void setWrappedDrawable(Drawable drawable) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }
        mDrawable = null;

        if (drawable != null) {
            // Copy over the bounds from the drawable
            setBounds(drawable.getBounds());
            // Set ourselves as the callback for invalidations
            drawable.setCallback(this);
        } else {
            // Clear our bounds
            setBounds(0, 0, 0, 0);
        }

        mDrawable = drawable;

        // Invalidate ourselves
        invalidateSelf();
    }

    protected boolean isCompatTintEnabled() {
        // It's enabled by default on Donut
        return true;
    }
}
