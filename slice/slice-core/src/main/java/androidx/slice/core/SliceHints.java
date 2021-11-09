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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/**
 * Temporary class to contain hint constants for slices to be used.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@RequiresApi(19)
public class SliceHints {

    /**
     * Subtype indicating that this content is the minimum value for a range.
     */
    public static final String SUBTYPE_MIN = "min";

    /**
     * Indicates that the content is the determinate mode for a range.
     */
    public static final int DETERMINATE_RANGE = 0;

    /**
     * Indicates that the content is the indeterminate mode for a range.
     */
    public static final int INDETERMINATE_RANGE = 1;

    /**
     * Indicates that the content is the star rating mode for a range.
     */
    public static final int STAR_RATING = 2;

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
     * Hint indicating that this slice is the end of section and may need some form of visual
     * separation.
     */
    public static final String HINT_END_OF_SECTION = "end_of_section";

    /**
     * Hint indicating that this slice was parsed from a serialized format.
     */
    public static final String HINT_CACHED = "cached";

    /**
     * Hint indicating that the content in this slice should be left unaltered as much as possible.
     */
    public static final String HINT_RAW = "raw";

    /**
     * Hint indicating that the text in this slice should be used to overlay an image.
     */
    public static final String HINT_OVERLAY = "overlay";

    /**
     * Hint indicating that the button in this slice should be shown as text button.
     */
    public static final String HINT_SHOW_LABEL = "show_label";

    /**
     * Subtype indicating that this slice represents a selection. The options will be included as
     * sub-slices.
     */
    public static final String SUBTYPE_SELECTION = "selection";

    /**
     * Subtype indicating that this slice represents a Date Picker.
     */
    public static final String SUBTYPE_DATE_PICKER = "date_picker";
    /**
     * Subtype indicating that this slice represents a Time Picker.
     */
    public static final String SUBTYPE_TIME_PICKER = "time_picker";

    /**
     * Hint indicating that this slice represents an option selectable in a selection slice.
     * The parent of this slice must be of subtype {@link #SUBTYPE_SELECTION}.
     */
    public static final String HINT_SELECTION_OPTION = "selection_option";

    /**
     * Subtype indicating that this slice represents the key passed back to the application when the
     * user selects this option. The parent of this slice must have hint
     * {@link #HINT_SELECTION_OPTION}.
     *
     * Expected to be an item of format {@link androidx.slice.SliceItem@FORMAT_TEXT}.
     */
    public static final String SUBTYPE_SELECTION_OPTION_KEY = "selection_option_key";

    /**
     * Subtype indicating that this slice represents the text displayed to the user for this option.
     * The parent of this slice must have hint {@link #HINT_SELECTION_OPTION}.
     *
     * Expected to be an item of format {@link androidx.slice.SliceItem@FORMAT_TEXT}.
     */
    public static final String SUBTYPE_SELECTION_OPTION_VALUE = "selection_option_value";

    public static final String SUBTYPE_HOST_EXTRAS = "host_extras";

    public static final String SUBTYPE_ACTION_KEY = "action_key";

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
     * Indicates that an image should be presented in its intrinsic size and shouldn't be tinted.
     * If SliceView in the call-site doesn't support RAW_IMAGE, fallback to SMALL_IMAGE instead.
     */
    public static final int RAW_IMAGE_SMALL = 3;
    /**
     * Indicates that an image should be presented in its intrinsic size and shouldn't be tinted.
     * If SliceView in the call-site doesn't support RAW_IMAGE, fallback to LARGE_IMAGE instead.
     */
    public static final int RAW_IMAGE_LARGE = 4;
    /**
     * Indicates that an image mode is unknown.
     */
    public static final int UNKNOWN_IMAGE = 5;
    /**
     * Indicates that an action with label.
     */
    public static final int ACTION_WITH_LABEL = 6;

    @IntDef({
            LARGE_IMAGE, SMALL_IMAGE, ICON_IMAGE, RAW_IMAGE_SMALL, RAW_IMAGE_LARGE, UNKNOWN_IMAGE,
            ACTION_WITH_LABEL
    })
    @Retention(SOURCE)
    public @interface ImageMode {
    }

    /**
     * Constant representing infinity.
     */
    public static final long INFINITY = -1;

    private SliceHints() {
    }
}
