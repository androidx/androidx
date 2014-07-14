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
package android.support.v4.graphics;

import android.graphics.Bitmap;

/**
 * Helper for accessing features in {@link android.graphics.Bitmap}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class BitmapCompat {
    /**
     * Interface for the full API.
     */
    interface BitmapImpl {
        public boolean hasMipMap(Bitmap bitmap);
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap);
        public int getAllocationByteCount(Bitmap bitmap);
    }

    static class BaseBitmapImpl implements BitmapImpl {
        @Override
        public boolean hasMipMap(Bitmap bitmap) {
            return false;
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
        }

        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }

    static class HcMr1BitmapCompatImpl extends BaseBitmapImpl {
        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return BitmapCompatHoneycombMr1.getAllocationByteCount(bitmap);
        }
    }

    static class JbMr2BitmapCompatImpl extends HcMr1BitmapCompatImpl {
        @Override
        public boolean hasMipMap(Bitmap bitmap){
            return BitmapCompatJellybeanMR2.hasMipMap(bitmap);
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
            BitmapCompatJellybeanMR2.setHasMipMap(bitmap, hasMipMap);
        }
    }

    static class KitKatBitmapCompatImpl extends JbMr2BitmapCompatImpl {
        @Override
        public int getAllocationByteCount(Bitmap bitmap) {
            return BitmapCompatKitKat.getAllocationByteCount(bitmap);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final BitmapImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 19) {
            IMPL = new KitKatBitmapCompatImpl();
        } else if (version >= 18) {
            IMPL = new JbMr2BitmapCompatImpl();
        } else if (version >= 12) {
            IMPL = new HcMr1BitmapCompatImpl();
        } else {
            IMPL = new BaseBitmapImpl();
        }
    }

    public static boolean hasMipMap(Bitmap bitmap) {
        return IMPL.hasMipMap(bitmap);
    }

    public static void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
        IMPL.setHasMipMap(bitmap, hasMipMap);
    }

    /**
     * Returns the size of the allocated memory used to store this bitmap's pixels in a backwards
     * compatible way.
     *
     * @param bitmap the bitmap in which to return it's allocation size
     * @return the allocation size in bytes
     */
    public static int getAllocationByteCount(Bitmap bitmap) {
        return IMPL.getAllocationByteCount(bitmap);
    }
}