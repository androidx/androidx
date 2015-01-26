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

package android.support.v7.graphics.drawable;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

/**
 * A {@link DrawableWrapper} which updates it's color filter using a {@link ColorStateList}.
 */
public class TintDrawableWrapper extends DrawableWrapper {

    static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private ColorStateList mTintList;
    private PorterDuff.Mode mTintMode;

    private int mCurrentColor;

    /**
     * Create a drawable wrapper which tints it's content {@link Drawable} based on the given
     * {@link android.content.res.ColorStateList}.
     */
    public TintDrawableWrapper(Drawable drawable, ColorStateList tint, PorterDuff.Mode tintMode) {
        super(drawable);
        mTintList = tint;
        mTintMode = tintMode;

        if (tint != null) {
            // If we have a tint, set the initial tint color
            mCurrentColor = mTintList.getColorForState(getState(), mTintList.getDefaultColor());
            setColorFilter(mCurrentColor, mTintMode != null ? mTintMode : DEFAULT_MODE);
        }
    }

    /**
     * Specifies a tint for this drawable.
     *
     * @param tint Color to use for tinting this drawable
     * @see #setTintMode(PorterDuff.Mode)
     */
    public void setSupportTint(int tint) {
        setTintList(ColorStateList.valueOf(tint));
    }

    /**
     * Specifies a tint for this drawable as a color state list.
     *
     * @param tint Color state list to use for tinting this drawable, or null to
     *            clear the tint
     * @see #setTintMode(PorterDuff.Mode)
     */
    public void setSupportTintList(ColorStateList tint) {
        mTintList = tint;
        updateTint(getState());
    }

    /**
     * Specifies a tint blending mode for this drawable.
     *
     * @param tintMode A Porter-Duff blending mode
     */
    public void setSupportTintMode(PorterDuff.Mode tintMode) {
        mTintMode = tintMode;
        updateTint(getState());
    }

    @Override
    public boolean isStateful() {
        return (mTintList != null && mTintList.isStateful()) || super.isStateful();
    }

    @Override
    public boolean setState(int[] stateSet) {
        boolean handled = super.setState(stateSet);
        handled = updateTint(stateSet) || handled;
        return handled;
    }

    private boolean updateTint(int[] state) {
        if (mTintList != null) {
            final int color = mTintList.getColorForState(state, mTintList.getDefaultColor());
            if (color != mCurrentColor) {
                setColorFilter(color, mTintMode != null ? mTintMode : DEFAULT_MODE);
                mCurrentColor = color;
                return true;
            }
        }
        return false;
    }
}
