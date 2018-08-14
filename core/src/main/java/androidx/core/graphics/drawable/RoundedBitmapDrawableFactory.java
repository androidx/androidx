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

package androidx.core.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.BitmapCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import java.io.InputStream;

/**
 * Constructs {@link RoundedBitmapDrawable RoundedBitmapDrawable} objects,
 * either from Bitmaps directly, or from streams and files.
 */
public final class RoundedBitmapDrawableFactory {
    private static final String TAG = "RoundedBitmapDrawableFa";

    private static class DefaultRoundedBitmapDrawable extends RoundedBitmapDrawable {
        DefaultRoundedBitmapDrawable(Resources res, Bitmap bitmap) {
            super(res, bitmap);
        }

        @Override
        public void setMipMap(boolean mipMap) {
            if (mBitmap != null) {
                BitmapCompat.setHasMipMap(mBitmap, mipMap);
                invalidateSelf();
            }
        }

        @Override
        public boolean hasMipMap() {
            return mBitmap != null && BitmapCompat.hasMipMap(mBitmap);
        }

        @Override
        void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight,
                Rect bounds, Rect outRect) {
            GravityCompat.apply(gravity, bitmapWidth, bitmapHeight,
                    bounds, outRect, ViewCompat.LAYOUT_DIRECTION_LTR);
        }
    }

    /**
     * Returns a new drawable by creating it from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     */
    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @Nullable Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 21) {
            return new RoundedBitmapDrawable21(res, bitmap);
        }
        return new DefaultRoundedBitmapDrawable(res, bitmap);
    }

    /**
     * Returns a new drawable, creating it by opening a given file path and decoding the bitmap.
     */
    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @NonNull String filepath) {
        final RoundedBitmapDrawable drawable = create(res, BitmapFactory.decodeFile(filepath));
        if (drawable.getBitmap() == null) {
            Log.w(TAG, "RoundedBitmapDrawable cannot decode " + filepath);
        }
        return drawable;
    }


    /**
     * Returns a new drawable, creating it by decoding a bitmap from the given input stream.
     */
    @NonNull
    public static RoundedBitmapDrawable create(@NonNull Resources res, @NonNull InputStream is) {
        final RoundedBitmapDrawable drawable = create(res, BitmapFactory.decodeStream(is));
        if (drawable.getBitmap() == null) {
            Log.w(TAG, "RoundedBitmapDrawable cannot decode " + is);
        }
        return drawable;
    }

    private RoundedBitmapDrawableFactory() {}

}
