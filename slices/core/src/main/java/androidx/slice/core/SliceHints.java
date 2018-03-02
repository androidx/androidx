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

package androidx.slice.core;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.support.annotation.RestrictTo;

/**
 * Temporary class to contain hint constants for slices to be used.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SliceHints {
    /**
     * Subtype to range an item representing a range.
     */
    public static final String SUBTYPE_RANGE = "range";

    /**
     * Subtype indicating that this content is the maximum value for a range.
     */
    public static final String SUBTYPE_MAX = "max";

    /**
     * Subtype indicating that this content is the current value for a range.
     */
    public static final String SUBTYPE_VALUE = "value";

    /**
     * Key to retrieve an extra added to an intent when the value of an input range has changed.
     */
    public static final String EXTRA_RANGE_VALUE = "android.app.slice.extra.RANGE_VALUE";

    /**
     * The meta-data key that allows an activity to easily be linked directly to a slice.
     * <p>
     * An activity can be statically linked to a slice uri by including a meta-data item
     * for this key that contains a valid slice uri for the same application declaring
     * the activity.
     */
    public static final String SLICE_METADATA_KEY = "android.metadata.SLICE_URI";
}
