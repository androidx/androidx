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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/**
 * Temporary class to contain hint constants for slices to be used.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
public class SliceHints {

    /**
     * Subtype indicating that this content is the minimum value for a range.
     */
    public static final String SUBTYPE_MIN = "min";

    /**
     * The meta-data key that allows an activity to easily be linked directly to a slice.
     * <p>
     * An activity can be statically linked to a slice uri by including a meta-data item
     * for this key that contains a valid slice uri for the same application declaring
     * the activity.
     */
    public static final String SLICE_METADATA_KEY = "android.metadata.SLICE_URI";

    /**
     * Subtype to tag an item as representing a time in milliseconds since midnight,
     * January 1, 1970 UTC.
     */
    public static final String SUBTYPE_MILLIS = "millis";

    /**
     * Hint indicating that the action/slice tagged with this will launch an activity.
     */
    public static final String HINT_ACTIVITY = "activity";

    /**
     * Hint indicating that this slice was parsed from a serialized format.
     */
    public static final String HINT_CACHED = "cached";

    /**
     * Subtype indicating that this slice represents a selection. The options will be included as
     * sub-slices.
     */
    public static final String SUBTYPE_SELECTION = "selection";

    /**
     * Subtype indicating that this slice represents the key passed back to the application when the
     * user selects this option. The grandparent of this slice must be of subtype
     * {@link #SUBTYPE_SELECTION}.
     *
     * Expected to be an item of format {@link androidx.slice.SliceItem@FORMAT_TEXT}.
     */
    public static final String SUBTYPE_SELECTION_OPTION_KEY = "selection_option_key";

    /**
     * Hint indicating that this slice represents the text displayed to the user for this option.
     * The grandparent of this slice must be of subtype {@link #SUBTYPE_SELECTION}.
     *
     * Expected to be an item of format {@link androidx.slice.SliceItem@FORMAT_TEXT}.
     */
    public static final String HINT_SELECTION_OPTION_VALUE = "selection_option_value";

    @IntDef({
            LARGE_IMAGE, SMALL_IMAGE, ICON_IMAGE, UNKNOWN_IMAGE
    })
    @Retention(SOURCE)
    public @interface ImageMode{}

    /**
     * Indicates that an image should be presented as an icon and it can be tinted.
     */
    public static final int ICON_IMAGE = 0;
    /**
     * Indicates that an image should be presented in a smaller size and it shouldn't be tinted.
     */
    public static final int SMALL_IMAGE = 1;
    /**
     * Indicates that an image presented in a larger size and it shouldn't be tinted.
     */
    public static final int LARGE_IMAGE = 2;
    /**
     * Indicates that an image mode is unknown.
     */
    public static final int UNKNOWN_IMAGE = 3;

    /**
     * Constant representing infinity.
     */
    public static final long INFINITY = -1;

    private SliceHints() {
    }
}
