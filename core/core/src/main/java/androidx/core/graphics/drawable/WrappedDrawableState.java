/*
 * Copyright 2018 The Android Open Source Project
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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class WrappedDrawableState extends Drawable.ConstantState {
    int mChangingConfigurations;
    Drawable.ConstantState mDrawableState;

    ColorStateList mTint = null;
    PorterDuff.Mode mTintMode = WrappedDrawableApi14.DEFAULT_TINT_MODE;

    WrappedDrawableState(@Nullable WrappedDrawableState orig) {
        if (orig != null) {
            mChangingConfigurations = orig.mChangingConfigurations;
            mDrawableState = orig.mDrawableState;
            mTint = orig.mTint;
            mTintMode = orig.mTintMode;
        }
    }

    @Override
    public @NonNull Drawable newDrawable() {
        return newDrawable(null);
    }

    @Override
    public @NonNull Drawable newDrawable(@Nullable Resources res) {
        return new WrappedDrawableApi21(this, res);
    }

    @Override
    public int getChangingConfigurations() {
        return mChangingConfigurations
                | (mDrawableState != null ? mDrawableState.getChangingConfigurations() : 0);
    }

    boolean canConstantState() {
        return mDrawableState != null;
    }
}
