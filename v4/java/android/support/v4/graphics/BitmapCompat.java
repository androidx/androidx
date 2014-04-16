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
    }

    /**
     * Interface implementation that doesn't use anything about v4 APIs.
     */
    static class BaseBitmapImpl implements BitmapImpl {
        @Override
        public boolean hasMipMap(Bitmap bitmap) {
            return false;
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
        }
    }

    static class JbMr2BitmapCompatImpl extends BaseBitmapImpl {
        @Override
        public boolean hasMipMap(Bitmap bitmap){
            return BitmapCompatJellybeanMR2.hasMipMap(bitmap);
        }

        @Override
        public void setHasMipMap(Bitmap bitmap, boolean hasMipMap) {
            BitmapCompatJellybeanMR2.setHasMipMap(bitmap, hasMipMap);
        }
    }

    /**
     * Select the correct implementation to use for the current platform.
     */
    static final BitmapImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 18) {
            IMPL = new JbMr2BitmapCompatImpl();
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
}