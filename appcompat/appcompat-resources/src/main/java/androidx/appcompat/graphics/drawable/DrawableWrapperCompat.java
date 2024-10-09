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

package androidx.appcompat.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.graphics.drawable.DrawableCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Drawable which delegates all calls to its wrapped {@link Drawable}.
 * <p>
 * The wrapped {@link Drawable} <em>must</em> be fully released from any {@link View}
 * before wrapping, otherwise internal {@link Callback} may be dropped.
 * <p>
 * Adapted from platform class, altered with API level checks as necessary.
 */
public class DrawableWrapperCompat extends Drawable implements Drawable.Callback {

    private Drawable mDrawable;

    /**
     * Creates a new wrapper around the specified drawable.
     */
    public DrawableWrapperCompat(Drawable drawable) {
        setDrawable(drawable);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
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

    @SuppressWarnings("deprecation")
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
        return mDrawable.isStateful();
    }

    @Override
    public boolean setState(final int[] stateSet) {
        return mDrawable.setState(stateSet);
    }

    @Override
    public int[] getState() {
        return mDrawable.getState();
    }

    @Override
    public void jumpToCurrentState() {
        mDrawable.jumpToCurrentState();
    }

    @Override
    public Drawable getCurrent() {
        return mDrawable.getCurrent();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        return super.setVisible(visible, restart) || mDrawable.setVisible(visible, restart);
    }

    @SuppressWarnings("deprecation")
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mDrawable.setLevel(level);
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        DrawableCompat.setAutoMirrored(mDrawable, mirrored);
    }

    @Override
    public boolean isAutoMirrored() {
        return DrawableCompat.isAutoMirrored(mDrawable);
    }

    @Override
    public void setTint(int tint) {
        DrawableCompat.setTint(mDrawable, tint);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        DrawableCompat.setTintList(mDrawable, tint);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        DrawableCompat.setTintMode(mDrawable, tintMode);
    }

    @Override
    public void setHotspot(float x, float y) {
        DrawableCompat.setHotspot(mDrawable, x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        DrawableCompat.setHotspotBounds(mDrawable, left, top, right, bottom);
    }

    /**
     * @return the wrapped drawable
     */
    public @Nullable Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * Sets the wrapped drawable.
     *
     * @param drawable the wrapped drawable
     */
    public void setDrawable(@Nullable Drawable drawable) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }

        mDrawable = drawable;

        if (drawable != null) {
            drawable.setCallback(this);
        }
    }
}
