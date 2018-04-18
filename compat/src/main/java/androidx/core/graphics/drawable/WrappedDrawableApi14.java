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

package androidx.core.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Drawable which delegates all calls to its wrapped {@link Drawable}.
 * <p/>
 * Also allows backward compatible tinting via a color or {@link ColorStateList}.
 * This functionality is accessed via static methods in {@code DrawableCompat}.
 */
class WrappedDrawableApi14 extends Drawable
        implements Drawable.Callback, WrappedDrawable, TintAwareDrawable {

    static final PorterDuff.Mode DEFAULT_TINT_MODE = PorterDuff.Mode.SRC_IN;

    private int mCurrentColor;
    private PorterDuff.Mode mCurrentMode;
    private boolean mColorFilterSet;

    DrawableWrapperState mState;
    private boolean mMutated;

    Drawable mDrawable;

    WrappedDrawableApi14(@NonNull DrawableWrapperState state, @Nullable Resources res) {
        mState = state;
        updateLocalState(res);
    }

    /**
     * Creates a new wrapper around the specified drawable.
     *
     * @param dr the drawable to wrap
     */
    WrappedDrawableApi14(@Nullable Drawable dr) {
        mState = mutateConstantState();
        // Now set the drawable...
        setWrappedDrawable(dr);
    }

    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void updateLocalState(@Nullable Resources res) {
        if (mState != null && mState.mDrawableState != null) {
            setWrappedDrawable(mState.mDrawableState.newDrawable(res));
        }
    }

    @Override
    public void jumpToCurrentState() {
        mDrawable.jumpToCurrentState();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        mDrawable.draw(canvas);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (mDrawable != null) {
            mDrawable.setBounds(bounds);
        }
    }

    @Override
    public void setChangingConfigurations(int configs) {
        mDrawable.setChangingConfigurations(configs);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations()
                | (mState != null ? mState.getChangingConfigurations() : 0)
                | mDrawable.getChangingConfigurations();
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
        final ColorStateList tintList = (isCompatTintEnabled() && mState != null)
                ? mState.mTint
                : null;
        return (tintList != null && tintList.isStateful()) || mDrawable.isStateful();
    }

    @Override
    public boolean setState(@NonNull int[] stateSet) {
        boolean handled = mDrawable.setState(stateSet);
        handled = updateTint(stateSet) || handled;
        return handled;
    }

    @NonNull
    @Override
    public int[] getState() {
        return mDrawable.getState();
    }

    @NonNull
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
    public boolean getPadding(@NonNull Rect padding) {
        return mDrawable.getPadding(padding);
    }

    @Override
    @RequiresApi(19)
    public void setAutoMirrored(boolean mirrored) {
        mDrawable.setAutoMirrored(mirrored);
    }

    @Override
    @RequiresApi(19)
    public boolean isAutoMirrored() {
        return mDrawable.isAutoMirrored();
    }

    @Override
    @Nullable
    public ConstantState getConstantState() {
        if (mState != null && mState.canConstantState()) {
            mState.mChangingConfigurations = getChangingConfigurations();
            return mState;
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState = mutateConstantState();
            if (mDrawable != null) {
                mDrawable.mutate();
            }
            if (mState != null) {
                mState.mDrawableState = mDrawable != null ? mDrawable.getConstantState() : null;
            }
            mMutated = true;
        }
        return this;
    }

    /**
     * Mutates the constant state and returns the new state.
     * <p>
     * This method should never call the super implementation; it should always
     * mutate and return its own constant state.
     *
     * @return the new state
     */
    @NonNull
    DrawableWrapperState mutateConstantState() {
        return new DrawableWrapperStateBase(mState, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        invalidateSelf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mDrawable.setLevel(level);
    }

    @Override
    public void setTint(int tint) {
        setTintList(ColorStateList.valueOf(tint));
    }

    @Override
    public void setTintList(ColorStateList tint) {
        mState.mTint = tint;
        updateTint(getState());
    }

    @Override
    public void setTintMode(@NonNull PorterDuff.Mode tintMode) {
        mState.mTintMode = tintMode;
        updateTint(getState());
    }

    private boolean updateTint(int[] state) {
        if (!isCompatTintEnabled()) {
            // If compat tinting is not enabled, fail fast
            return false;
        }

        final ColorStateList tintList = mState.mTint;
        final PorterDuff.Mode tintMode = mState.mTintMode;

        if (tintList != null && tintMode != null) {
            final int color = tintList.getColorForState(state, tintList.getDefaultColor());
            if (!mColorFilterSet || color != mCurrentColor || tintMode != mCurrentMode) {
                setColorFilter(color, tintMode);
                mCurrentColor = color;
                mCurrentMode = tintMode;
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
    @Override
    public final Drawable getWrappedDrawable() {
        return mDrawable;
    }

    /**
     * Sets the current wrapped {@link Drawable}
     */
    @Override
    public final void setWrappedDrawable(Drawable dr) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }

        mDrawable = dr;

        if (dr != null) {
            dr.setCallback(this);
            // Only call setters for data that's stored in the base Drawable.
            setVisible(dr.isVisible(), true);
            setState(dr.getState());
            setLevel(dr.getLevel());
            setBounds(dr.getBounds());
            if (mState != null) {
                mState.mDrawableState = dr.getConstantState();
            }
        }

        invalidateSelf();
    }

    protected boolean isCompatTintEnabled() {
        // It's enabled by default on Gingerbread
        return true;
    }

    protected abstract static class DrawableWrapperState extends Drawable.ConstantState {
        int mChangingConfigurations;
        Drawable.ConstantState mDrawableState;

        ColorStateList mTint = null;
        PorterDuff.Mode mTintMode = DEFAULT_TINT_MODE;

        DrawableWrapperState(@Nullable DrawableWrapperState orig, @Nullable Resources res) {
            if (orig != null) {
                mChangingConfigurations = orig.mChangingConfigurations;
                mDrawableState = orig.mDrawableState;
                mTint = orig.mTint;
                mTintMode = orig.mTintMode;
            }
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return newDrawable(null);
        }

        @NonNull
        @Override
        public abstract Drawable newDrawable(@Nullable Resources res);

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations
                    | (mDrawableState != null ? mDrawableState.getChangingConfigurations() : 0);
        }

        boolean canConstantState() {
            return mDrawableState != null;
        }
    }

    private static class DrawableWrapperStateBase extends DrawableWrapperState {
        DrawableWrapperStateBase(
                @Nullable DrawableWrapperState orig, @Nullable Resources res) {
            super(orig, res);
        }

        @NonNull
        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            return new WrappedDrawableApi14(this, res);
        }
    }
}
