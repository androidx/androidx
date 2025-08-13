/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.util;

import android.util.SizeF;

import org.jspecify.annotations.NonNull;

/**
 * Immutable class for describing width and height dimensions in some arbitrary unit. Width and
 * height are finite values stored as a floating point representation.
 * <p>
 * This is a backward-compatible version of {@link SizeF}.
 */
public final class SizeFCompat {

    private final float mWidth;
    private final float mHeight;

    public SizeFCompat(float width, float height) {
        mWidth = Preconditions.checkArgumentFinite(width, "width");
        mHeight = Preconditions.checkArgumentFinite(height, "height");
    }

    /**
     * Get the width of the size (as an arbitrary unit).
     * @return width
     */
    public float getWidth() {
        return mWidth;
    }

    /**
     * Get the height of the size (as an arbitrary unit).
     * @return height
     */
    public float getHeight() {
        return mHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeFCompat)) return false;
        SizeFCompat that = (SizeFCompat) o;
        return that.mWidth == mWidth && that.mHeight == mHeight;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(mWidth) ^ Float.floatToIntBits(mHeight);
    }

    @Override
    public @NonNull String toString() {
        return mWidth + "x" + mHeight;
    }

    /** Converts this {@link SizeFCompat} into a {@link SizeF}. */
    public @NonNull SizeF toSizeF() {
        return new SizeF(getWidth(), getHeight());
    }

    /** Converts this {@link SizeF} into a {@link SizeFCompat}. */
    public static @NonNull SizeFCompat toSizeFCompat(@NonNull SizeF size) {
        return new SizeFCompat(size.getWidth(), size.getHeight());
    }
}
