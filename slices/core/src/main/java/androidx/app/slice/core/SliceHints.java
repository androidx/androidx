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

import android.app.slice.Slice;
import android.support.annotation.RestrictTo;

/**
 * Temporary class to contain hint constants for slices to be used.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SliceHints {
    /**
     * Subtype to indicate that this content has a toggle action associated with it. To indicate
     * that the toggle is on, use {@link Slice#HINT_SELECTED}. When the toggle state changes, the
     * intent associated with it will be sent along with an extra {@link #EXTRA_TOGGLE_STATE}
     * which can be retrieved to see the new state of the toggle.
     */
    public static final String SUBTYPE_TOGGLE = "toggle";

    /**
     * Key to retrieve an extra added to an intent when a control is changed.
     */
    public static final String EXTRA_TOGGLE_STATE = "android.app.slice.extra.TOGGLE_STATE";

    /**
     * Hint indicating this content should be shown instead of the normal content when the slice
     * is in small format
     */
    public static final String HINT_SUMMARY = "summary";
}
