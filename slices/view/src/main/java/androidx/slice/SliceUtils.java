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

package androidx.slice;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.SliceMetadata.LOADED_ALL;
import static androidx.slice.SliceMetadata.LOADED_NONE;
import static androidx.slice.SliceMetadata.LOADED_PARTIAL;
import static androidx.slice.core.SliceHints.HINT_KEYWORDS;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.core.SliceQuery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for dealing with slices.
 */
public class SliceUtils {

    private SliceUtils() {
    }

    /**
     * Serialize a slice to an OutputStream.
     * <p>
     * The slice can later be read into slice form again with {@link #parseSlice}.
     * Some slice types cannot be serialized, their handling is controlled by
     * {@link SerializeOptions}.
     *
     * @param s The slice to serialize.
     * @param context Context used to load any resources in the slice.
     * @param output The output of the serialization.
     * @param encoding The encoding to use for serialization.
     * @param options Options defining how to handle non-serializable items.
     * @throws IllegalArgumentException if the slice cannot be serialized using the given options.
     */
    public static void serializeSlice(@NonNull Slice s, @NonNull Context context,
            @NonNull OutputStream output, @NonNull String encoding,
            @NonNull SerializeOptions options) throws IOException, IllegalArgumentException {
        SliceXml.serializeSlice(s, context, output, encoding, options);
    }

    /**
     * Parse a slice that has been previously serialized.
     * <p>
     * Parses a slice that was serialized with {@link #serializeSlice}.
     * <p>
     * Note: Slices returned by this cannot be passed to {@link SliceConvert#unwrap(Slice)}.
     *
     * @param input The input stream to read from.
     * @param encoding The encoding to read as.
     * @param listener Listener used to handle actions when reconstructing the slice.
     * @throws SliceParseException if the InputStream cannot be parsed.
     */
    public static @NonNull Slice parseSlice(@NonNull Context context, @NonNull InputStream input,
            @NonNull String encoding, @NonNull SliceActionListener listener)
            throws IOException, SliceParseException {
        return SliceXml.parseSlice(context, input, encoding, listener);
    }

    /**
     * Holds options for how to handle SliceItems that cannot be serialized.
     */
    public static class SerializeOptions {
        /**
         * Constant indicating that the an {@link IllegalArgumentException} should be thrown
         * when this format is encountered.
         */
        public static final int MODE_THROW = 0;
        /**
         * Constant indicating that the SliceItem should be removed when this format is encountered.
         */
        public static final int MODE_REMOVE = 1;
        /**
         * Constant indicating that the SliceItem should be serialized as much as possible.
         * <p>
         * For images this means they will be attempted to be serialized. For actions, the
         * action will be removed but the content of the action will be serialized. The action
         * may be triggered later on a de-serialized slice by binding the slice again and activating
         * a pending-intent at the same location as the serialized action.
         */
        public static final int MODE_CONVERT = 2;

        @IntDef({MODE_THROW, MODE_REMOVE, MODE_CONVERT})
        @Retention(SOURCE)
        @interface FormatMode {
        }

        private int mActionMode = MODE_THROW;
        private int mImageMode = MODE_THROW;
        private int mMaxWidth = 1000;
        private int mMaxHeight = 1000;

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void checkThrow(String format) {
            switch (format) {
                case FORMAT_ACTION:
                case FORMAT_REMOTE_INPUT:
                    if (mActionMode != MODE_THROW) return;
                    break;
                case FORMAT_IMAGE:
                    if (mImageMode != MODE_THROW) return;
                    break;
                default:
                    return;
            }
            throw new IllegalArgumentException(format + " cannot be serialized");
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getActionMode() {
            return mActionMode;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getImageMode() {
            return mImageMode;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getMaxWidth() {
            return mMaxWidth;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getMaxHeight() {
            return mMaxHeight;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_ACTION} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setActionMode(@FormatMode int mode) {
            mActionMode = mode;
            return this;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_IMAGE} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setImageMode(@FormatMode int mode) {
            mImageMode = mode;
            return this;
        }

        /**
         * Set the maximum width of an image to use when serializing.
         * <p>
         * Will only be used if the {@link #setImageMode(int)} is set to {@link #MODE_CONVERT}.
         * Any images larger than the maximum size will be scaled down to fit within that size.
         * The default value is 1000.
         */
        public SerializeOptions setMaxImageWidth(int width) {
            mMaxWidth = width;
            return this;
        }

        /**
         * Set the maximum height of an image to use when serializing.
         * <p>
         * Will only be used if the {@link #setImageMode(int)} is set to {@link #MODE_CONVERT}.
         * Any images larger than the maximum size will be scaled down to fit within that size.
         * The default value is 1000.
         */
        public SerializeOptions setMaxImageHeight(int height) {
            mMaxHeight = height;
            return this;
        }
    }

    /**
     * Indicates this slice is empty and waiting for content to be loaded.
     *
     * @deprecated TO BE REMOVED: use {@link SliceMetadata#LOADED_NONE}
     */
    @Deprecated
    public static final int LOADING_ALL = 0;
    /**
     * Indicates this slice has some content but is waiting for other content to be loaded.
     *
     * @deprecated TO BE REMOVED: use {@link SliceMetadata#LOADED_PARTIAL}
     */
    @Deprecated
    public static final int LOADING_PARTIAL = 1;
    /**
     * Indicates this slice has fully loaded and is not waiting for other content.
     *
     * @deprecated TO BE REMOVED: use {@link SliceMetadata#LOADED_ALL}
     */
    @Deprecated
    public static final int LOADING_COMPLETE = 2;

    /**
     * @return the current loading state of the provided {@link Slice}.
     *
     * @deprecated TO BE REMOVED: use {@link SliceMetadata#getLoadingState()}
     */
    @Deprecated
    public static int getLoadingState(@NonNull Slice slice) {
        // Check loading state
        boolean hasHintPartial =
                SliceQuery.find(slice, null, HINT_PARTIAL, null) != null;
        if (slice.getItems().size() == 0) {
            // Empty slice
            return LOADED_NONE;
        } else if (hasHintPartial) {
            // Slice with specific content to load
            return LOADED_PARTIAL;
        } else {
            // Full slice
            return LOADED_ALL;
        }
    }

    /**
     * @return the group of actions associated with the provided slice, if they exist.
     *
     * @deprecated TO BE REMOVED; use {@link SliceMetadata#getSliceActions()}
     */
    @Deprecated
    @Nullable
    public static List<SliceItem> getSliceActions(@NonNull Slice slice) {
        SliceItem actionGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_ACTIONS, null);
        String[] hints = new String[] {HINT_ACTIONS, HINT_SHORTCUT};
        return (actionGroup != null)
                ? SliceQuery.findAll(actionGroup, FORMAT_SLICE, hints, null)
                : null;
    }

    /**
     * @return the list of keywords associated with the provided slice, null if no keywords were
     * specified or an empty list if the slice was specified to have no keywords.
     *
     * @deprecated TO BE REMOVED; use {@link SliceMetadata#getSliceKeywords()}
     */
    @Deprecated
    @Nullable
    public static List<String> getSliceKeywords(@NonNull Slice slice) {
        SliceItem keywordGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_KEYWORDS, null);
        if (keywordGroup != null) {
            List<SliceItem> itemList = SliceQuery.findAll(keywordGroup, FORMAT_TEXT);
            if (itemList != null) {
                ArrayList<String> stringList = new ArrayList<>();
                for (int i = 0; i < itemList.size(); i++) {
                    String keyword = (String) itemList.get(i).getText();
                    if (!TextUtils.isEmpty(keyword)) {
                        stringList.add(keyword);
                    }
                }
                return stringList;
            }
        }
        return null;
    }

    /**
     * A listener used to receive events on slices parsed with
     * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
     */
    public interface SliceActionListener {
        /**
         * Called when an action is triggered on a slice parsed with
         * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
         * @param actionUri The uri of the action selected.
         */
        void onSliceAction(Uri actionUri);
    }

    /**
     * Exception thrown during
     * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
     */
    public static class SliceParseException extends Exception {
        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SliceParseException(String s, Throwable e) {
            super(s, e);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SliceParseException(String s) {
            super(s);
        }
    }
}
