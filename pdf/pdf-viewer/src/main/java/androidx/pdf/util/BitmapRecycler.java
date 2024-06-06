/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds on to wasted bitmaps for a while in the hope they can be reused. Works very well if we are
 * to use lots of bitmaps of the same size. Isn't useful if sizes differ.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BitmapRecycler {
    private static final String TAG = BitmapRecycler.class.getSimpleName();

    private final List<WeakReference<Bitmap>> mPool = new ArrayList<>();

    /**
     * Obtains a {@link Bitmap} of the given dimensions, either by reusing a compatible wasted
     * one, or
     * creating a new one.
     *
     * @param dimensions The required dimensions of the bitmap (in px)
     * @return A recycled or new {@link Bitmap}, or null if there's not enough memory to create one.
     */
    @Nullable
    public Bitmap obtainBitmap(@NonNull Dimensions dimensions) {
        Bitmap bitmap;
        Iterator<WeakReference<Bitmap>> iterator = null;
        synchronized (mPool) {
            iterator = mPool.iterator();
            while (iterator.hasNext()) {
                bitmap = iterator.next().get();
                if (bitmap == null) {
                    iterator.remove();
                } else if (bitmap.getWidth() == dimensions.getWidth()
                        && bitmap.getHeight() == dimensions.getHeight()) {
                    iterator.remove();
                    return bitmap;
                }
            }
        }

        // We're about to need a big chunk of memory, so let the GC reclaim as much as it can...
        bitmap = null;
        iterator = null;
        return createBitmap(dimensions);
    }

    /** Discards a no-longer-useful {@link Bitmap} so it can be recycled later. */
    public void discardBitmap(@Nullable Bitmap bitmap) {
        synchronized (mPool) {
            if (bitmap != null) {
                mPool.add(new WeakReference<>(bitmap));
            }
        }

    }

    @Nullable
    private Bitmap createBitmap(Dimensions dimensions) {
        try {
            Bitmap bitmap =
                    Bitmap.createBitmap(dimensions.getWidth(), dimensions.getHeight(),
                            Bitmap.Config.ARGB_8888);
            dump();
            return bitmap;
        } catch (OutOfMemoryError e) {
            dump();
            return null;
        }
    }

    /** Returns the memory usage of a {@link Bitmap} (in kb). */
    public static int getMemSizeKb(@NonNull Bitmap bitmap) {
        return bitmap.getByteCount() / 1024; // could use getAllocationByteCount() for API >= 19
    }

    private void dump() {
        StringBuilder sb = new StringBuilder("BitmapRecycler " + mPool.size() + " (");
        int mem = 0;

        for (WeakReference<Bitmap> bitmapRef : mPool) {
            Bitmap bitmap = bitmapRef.get();
            if (bitmap != null) {
                mem += getMemSizeKb(bitmap);
                sb.append(toString(bitmap)).append(",");
            } else {
                sb.append("(null ref)");
            }
        }

        sb.append(") /mem = " + mem);
    }

    @SuppressWarnings("ObjectToString")
    private static String toString(Bitmap bitmap) {
        // TODO: Bitmap does not implement toString() in bitmap
        return String.format("%s : %s x %s", bitmap, bitmap.getWidth(), bitmap.getHeight());
    }
}
