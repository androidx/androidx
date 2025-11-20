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
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;

class WrappedDrawableApi21 extends WrappedDrawableApi14 {
    private static final String TAG = "WrappedDrawableApi21";
    private static Method sIsProjectedDrawableMethod;

    WrappedDrawableApi21(WrappedDrawableState state, Resources resources) {
        super(state, resources);
        findAndCacheIsProjectedDrawableMethod();
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
    public void getOutline(@NonNull Outline outline) {
        mDrawable.getOutline(outline);
    }

    @Override
    public @NonNull Rect getDirtyBounds() {
        return mDrawable.getDirtyBounds();
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (isCompatTintEnabled()) {
            super.setTintList(tint);
        } else {
            mDrawable.setTintList(tint);
        }
    }

    @Override
    public void setTint(int tintColor) {
        if (isCompatTintEnabled()) {
            super.setTint(tintColor);
        } else {
            mDrawable.setTint(tintColor);
        }
    }

    @Override
    public void setTintMode(PorterDuff.@NonNull Mode tintMode) {
        if (isCompatTintEnabled()) {
            super.setTintMode(tintMode);
        } else {
            mDrawable.setTintMode(tintMode);
        }
    }

    @Override
    public boolean setState(int @NonNull [] stateSet) {
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
        final Drawable drawable = mDrawable;
        return drawable instanceof GradientDrawable
                || drawable instanceof DrawableContainer
                || drawable instanceof InsetDrawable
                || drawable instanceof RippleDrawable;
    }

    /**
     * This method is overriding hidden framework method in {@link Drawable}. It is used by the
     * system and thus it should not be removed.
     */
    @Override
    public boolean isProjected() {
        if (mDrawable != null && sIsProjectedDrawableMethod != null) {
            try {
                return (Boolean) sIsProjectedDrawableMethod.invoke(mDrawable);
            } catch (Exception ex) {
                Log.w(TAG, "Error calling Drawable#isProjected() method", ex);
            }
        }

        return false;
    }

    private void findAndCacheIsProjectedDrawableMethod() {
        if (sIsProjectedDrawableMethod == null) {
            try {
                sIsProjectedDrawableMethod = Drawable.class.getDeclaredMethod("isProjected");
            } catch (Exception ex) {
                Log.w(TAG, "Failed to retrieve Drawable#isProjected() method", ex);
            }
        }
    }
}
