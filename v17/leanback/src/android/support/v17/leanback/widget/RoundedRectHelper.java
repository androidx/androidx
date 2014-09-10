/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;

/**
 * Helper for setting rounded rectangle backgrounds on a view.
 */
final class RoundedRectHelper {

    private final static RoundedRectHelper sInstance = new RoundedRectHelper();
    private Impl mImpl;

    /**
     * Returns an instance of the helper.
     */
    public static RoundedRectHelper getInstance() {
        return sInstance;
    }

    /**
     * Sets a rounded rectangle background on the given view, and clips the view to the
     * background.  If clipping isn't supported on the android runtime, a simple rectangle
     * background is set instead.
     *
     * @param view The view to be modified
     * @param color The color of the background
     */
    public void setRoundedRectBackground(View view, int color) {
        mImpl.setRoundedRectBackground(view, color);
    }

    /**
     * Clears the background of the view to transparent.
     *
     * @param view The view to be modified
     */
    public void clearBackground(View view) {
        mImpl.clearBackground(view);
    }

    static interface Impl {
        public void setRoundedRectBackground(View view, int color);
        public void clearBackground(View view);
    }

    /**
     * Implementation used prior to L.
     */
    private static final class StubImpl implements Impl {
        @Override
        public void setRoundedRectBackground(View view, int color) {
            // We could set a rounded rect background, but we don't
            // because we can't do setClipToOutline.
            // So just set a regular rectangle.
            view.setBackgroundColor(color);
        }

        @Override
        public void clearBackground(View view) {
            view.setBackground(null);
        }
    }

    /**
     * Implementation used on api 21 (and above).
     */
    private static final class Api21Impl implements Impl {
        @Override
        public void setRoundedRectBackground(View view, int color) {
            RoundedRectHelperApi21.setRoundedRectBackground(view, color);
        }

        @Override
        public void clearBackground(View view) {
            RoundedRectHelperApi21.clearBackground(view);
        }
    }

    private RoundedRectHelper() {
        if (Build.VERSION.SDK_INT >= 21) {
            mImpl = new Api21Impl();
        } else {
            mImpl = new StubImpl();
        }
    }
}
