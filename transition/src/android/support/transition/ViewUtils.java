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

package android.support.transition;

import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Compatibility utilities for platform features of {@link View}.
 */
class ViewUtils {

    private static final ViewUtilsImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 18) {
            IMPL = new ViewUtilsApi18();
        } else {
            IMPL = new ViewUtilsApi14();
        }
    }

    /**
     * Backward-compatible {@link View#getOverlay()}.
     */
    static ViewOverlayImpl getOverlay(@NonNull View view) {
        return IMPL.getOverlay(view);
    }

    /**
     * Backward-compatible {@link View#getWindowId()}.
     */
    static WindowIdImpl getWindowId(@NonNull View view) {
        return IMPL.getWindowId(view);
    }

}
