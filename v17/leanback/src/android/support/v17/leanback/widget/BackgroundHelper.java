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
 * limitations under the License
 */
package android.support.v17.leanback.widget;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v17.leanback.widget.BackgroundHelperKitkat;
import android.view.View;


/**
 * Helper for view backgrounds.
 * @hide
 */
public final class BackgroundHelper {

    final static BackgroundHelperVersionImpl sImpl;

    static interface BackgroundHelperVersionImpl {
        public void setBackgroundPreservingAlpha(View view, Drawable drawable);
    }

    private static final class BackgroundHelperStubImpl implements BackgroundHelperVersionImpl {
        @Override
        public void setBackgroundPreservingAlpha(View view, Drawable drawable) {
            // Cannot query drawable alpha
            view.setBackground(drawable);
        }
    }

    private static final class BackgroundHelperKitkatImpl implements BackgroundHelperVersionImpl {
        @Override
        public void setBackgroundPreservingAlpha(View view, Drawable drawable) {
            BackgroundHelperKitkat.setBackgroundPreservingAlpha(view, drawable);
        }
    }

    private BackgroundHelper() {
    }

    static {
        if (Build.VERSION.SDK_INT >= 19) {
            sImpl = new BackgroundHelperKitkatImpl();
        } else {
            sImpl = new BackgroundHelperStubImpl();
        }
    }

    public static void setBackgroundPreservingAlpha(View view, Drawable drawable) {
        sImpl.setBackgroundPreservingAlpha(view, drawable);
    }
}
