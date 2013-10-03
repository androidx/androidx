/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.view;

/**
 * Helper for accessing features in <code>ScaleGestureDetector</code> introduced
 * after API level 19 (KitKat) in a backwards compatible fashion.
 */
public class ScaleGestureDetectorCompat {
    static final ScaleGestureDetectorImpl IMPL;

    interface ScaleGestureDetectorImpl {

        public void setQuickScaleEnabled(Object o, boolean enabled);

        public boolean isQuickScaleEnabled(Object o);
    }

    private static class BaseScaleGestureDetectorImpl implements ScaleGestureDetectorImpl {
        @Override
        public void setQuickScaleEnabled(Object o, boolean enabled) {
            // Intentionally blank
        }

        @Override
        public boolean isQuickScaleEnabled(Object o) {
            return false;
        }
    }

    private static class ScaleGestureDetectorCompatKitKatImpl implements ScaleGestureDetectorImpl {
        @Override
        public void setQuickScaleEnabled(Object o, boolean enabled) {
            ScaleGestureDetectorCompatKitKat.setQuickScaleEnabled(o, enabled);
        }

        @Override
        public boolean isQuickScaleEnabled(Object o) {
            return ScaleGestureDetectorCompatKitKat.isQuickScaleEnabled(o);
        }
    }

    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 19) { // KitKat
            IMPL = new ScaleGestureDetectorCompatKitKatImpl();
        } else {
            IMPL = new BaseScaleGestureDetectorImpl();
        }
    }

    private ScaleGestureDetectorCompat() {}

    /**
     * Set whether the associated <code>OnScaleGestureListener</code> should receive onScale
     * callbacks when the user performs a doubleTap followed by a swipe. Note that this is enabled
     * by default if the app targets API 19 and newer.
     * @param enabled true to enable quick scaling, false to disable
     */
    public static void setQuickScaleEnabled(Object scaleGestureDetector, boolean enabled) {
        IMPL.setQuickScaleEnabled(scaleGestureDetector, enabled);
    }

    /**
     * Return whether the quick scale gesture, in which the user performs a double tap followed by a
     * swipe, should perform scaling. See <code>setQuickScaleEnabled(Object, boolean)<code>.
     */
    public static boolean isQuickScaleEnabled(Object scaleGestureDetector) {
        return IMPL.isQuickScaleEnabled(scaleGestureDetector); // KitKat
    }
}
