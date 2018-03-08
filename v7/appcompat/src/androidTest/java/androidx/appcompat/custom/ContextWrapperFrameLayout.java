/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.appcompat.custom;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * An AppCompatImageView which manually wraps its Context
 */
public class ContextWrapperFrameLayout extends FrameLayout {

    public ContextWrapperFrameLayout(Context context) {
        super(new CustomContextWrapper(context));
    }

    public ContextWrapperFrameLayout(Context context, AttributeSet attrs) {
        super(new CustomContextWrapper(context), attrs);
    }

    public static class CustomContextWrapper extends ContextWrapper {
        private final Resources mResources;

        public CustomContextWrapper(Context base) {
            super(base);
            mResources = new CustomResources(base.getResources());
        }

        @Override
        public Resources getResources() {
            return mResources;
        }
    }

    public static class CustomResources extends Resources {
        public CustomResources(Resources res) {
            super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
        }
    }
}
