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
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

class DrawableWrapperLollipop extends DrawableWrapperKitKat {

    private final boolean mUseCompatTinting;

    DrawableWrapperLollipop(Drawable drawable) {
        this(drawable, false);
    }

    DrawableWrapperLollipop(Drawable drawable, boolean useCompatTinting) {
        super(drawable);
        mUseCompatTinting = useCompatTinting;
    }

    @Override
    public void setHotspot(float x, float y) {
        mDrawable.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        mDrawable.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getOutline(Outline outline) {
        mDrawable.getOutline(outline);
    }

    @Override
    public void applyTheme(Resources.Theme t) {
        mDrawable.applyTheme(t);
    }

    @Override
    public boolean canApplyTheme() {
        return mDrawable.canApplyTheme();
    }

    @Override
    public Rect getDirtyBounds() {
        return mDrawable.getDirtyBounds();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (mUseCompatTinting) {
            setCompatTintList(tint);
        } else {
            mDrawable.setTintList(tint);
        }
    }

    @Override
    public void setTint(int tintColor) {
        if (mUseCompatTinting) {
            setCompatTint(tintColor);
        } else {
            mDrawable.setTint(tintColor);
        }
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        if (mUseCompatTinting) {
            setCompatTintMode(tintMode);
        } else {
            mDrawable.setTintMode(tintMode);
        }
    }

    @Override
    public boolean setState(int[] stateSet) {
        if (super.setState(stateSet)) {
            // Manually invalidate because the framework doesn't currently force an invalidation
            // on a state change
            invalidateSelf();
            return true;
        }
        return false;
    }

    @Override
    protected boolean isCompatTintEnabled() {
        return mUseCompatTinting;
    }
}
