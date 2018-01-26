/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.app.slice.core;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;

/**
 * Temporary class to contain hint constants for slices to be used.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SliceHints {
    /**
     * Subtype indicating that this content is the maximum value for a slider or progress.
     */
    public static final String SUBTYPE_MAX = "max";

    /**
     * Subtype indicating that this content is the current value for a slider or progress.
     */
    public static final String SUBTYPE_PROGRESS = "progress";

    /**
     * Key to retrieve an extra added to an intent when the value of a slider has changed.
     */
    public static final String EXTRA_SLIDER_VALUE = "android.app.slice.extra.SLIDER_VALUE";
}
