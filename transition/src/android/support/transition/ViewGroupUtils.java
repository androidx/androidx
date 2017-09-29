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
import android.view.ViewGroup;

/**
 * Compatibility utilities for platform features of {@link ViewGroup}.
 */
class ViewGroupUtils {

    private static final ViewGroupUtilsImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 18) {
            IMPL = new ViewGroupUtilsApi18();
        } else {
            IMPL = new ViewGroupUtilsApi14();
        }
    }

    /**
     * Backward-compatible {@link ViewGroup#getOverlay()}.
     */
    static ViewGroupOverlayImpl getOverlay(@NonNull ViewGroup group) {
        return IMPL.getOverlay(group);
    }

    /**
     * Provides access to the hidden ViewGroup#suppressLayout method.
     */
    static void suppressLayout(@NonNull ViewGroup group, boolean suppress) {
        IMPL.suppressLayout(group, suppress);
    }

}
