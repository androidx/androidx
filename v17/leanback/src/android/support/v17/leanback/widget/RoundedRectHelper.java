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
     * Sets or removes a rounded rectangle outline on the given view.
     */
    public void setClipToRoundedOutline(View view, boolean clip) {
        mImpl.setClipToRoundedOutline(view, clip);
    }

    static interface Impl {
        public void setClipToRoundedOutline(View view, boolean clip);
    }

    /**
     * Implementation used prior to L.
     */
    private static final class StubImpl implements Impl {
        @Override
        public void setClipToRoundedOutline(View view, boolean clip) {
            // Not supported
        }
    }

    /**
     * Implementation used on api 21 (and above).
     */
    private static final class Api21Impl implements Impl {
        @Override
        public void setClipToRoundedOutline(View view, boolean clip) {
            RoundedRectHelperApi21.setClipToRoundedOutline(view, clip);
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
