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
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;

import static androidx.slice.core.SliceHints.SUBTYPE_MILLIS;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceHints;
import androidx.slice.core.SliceQuery;
import androidx.slice.widget.ListContent;
import androidx.slice.widget.SliceView;
import androidx.slice.widget.SliceView.SliceMode;
import androidx.versionedparcelable.ParcelUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Utilities for dealing with slices.
 */
@RequiresApi(19)
public class SliceUtils {

    private SliceUtils() {
    }

    /**
     * Attempt to remove any extra content from the slice.
     * <p>
     * This is meant to be used alongside {@link #serializeSlice} to reduce
     * the size of a slice by only including data for the slice to be displayed
     * in a specific mode.
     *
     * @param s            The slice to strip.
     * @param mode         The mode that will be used with {@link SliceView#setMode} to
     *                     display the slice.
     * @param isScrollable The value that will be used with {@link SliceView#setScrollable} to
     *                     display the slice.
     * @return returns A new smaller slice if stripping can be done, or the
     * original slice if no content can be removed.
     */
    @NonNull
    public static Slice stripSlice(@NonNull Slice s, @SliceMode int mode, boolean isScrollable) {
        ListContent listContent = new ListContent(s);
        if (listContent.isValid()) {
            Slice.Builder builder = copyMetadata(s);
            switch (mode) {
                case SliceView.MODE_SHORTCUT:
                    // TODO -- passing context in here will ensure we always have shortcut / can
                    // fall back to appInfo
                    SliceAction shortcutAction = listContent.getShortcut(null);
                    if (shortcutAction != null) {
                        return ((SliceActionImpl) shortcutAction).buildSlice(builder);
                    } else {
                        return s;
                    }
                case SliceView.MODE_SMALL:
                    builder.addItem(listContent.getHeader().getSliceItem());
                    List<SliceAction> actions = listContent.getSliceActions();
                    if (actions != null && actions.size() > 0) {
                        Slice.Builder sb = new Slice.Builder(builder);
                        for (SliceAction action : actions) {
                            Slice.Builder b = new Slice.Builder(sb).addHints(HINT_ACTIONS);
                            sb.addSubSlice(((SliceActionImpl) action).buildSlice(b));
                        }
                        builder.addSubSlice(sb.addHints(HINT_ACTIONS).build());
                    }
                    return builder.build();
                case SliceView.MODE_LARGE:
                    // TODO: Implement stripping for large mode
                default:
                    return s;
            }
        }
        return s;
    }

    /**
     * @return A slice builder that contains slice metadata from top-level items.
     */
    private static Slice.Builder copyMetadata(@NonNull Slice s) {
        Slice.Builder slice = new Slice.Builder(s.getUri());

        // Adds TTL item into new slice builder.
        SliceItem ttlItem = SliceQuery.findTopLevelItem(s, FORMAT_LONG, HINT_TTL, null, null);
        if (ttlItem != null) {
            slice.addLong(ttlItem.getLong(), SUBTYPE_MILLIS, HINT_TTL);
        }

        // Adds last updated item into new slice builder.
        SliceItem updatedItem = SliceQuery.findTopLevelItem(
                s, FORMAT_LONG, HINT_LAST_UPDATED, null, null);
        if (updatedItem != null) {
            slice.addLong(updatedItem.getLong(), SUBTYPE_MILLIS, HINT_LAST_UPDATED);
        }

        // Adds color item into new slice builder.
        SliceItem colorItem = SliceQuery.findTopLevelItem(s, FORMAT_INT, SUBTYPE_COLOR, null,
                null);
        if (colorItem != null) {
            slice.addInt(colorItem.getInt(), SUBTYPE_COLOR);
        }

        // Adds layout direction item into new slice builder.
        SliceItem layoutDirItem = SliceQuery.findTopLevelItem(
                s, FORMAT_INT, SUBTYPE_LAYOUT_DIRECTION, null, null);
        if (layoutDirItem != null) {
            slice.addInt(layoutDirItem.getInt(), SUBTYPE_LAYOUT_DIRECTION);
        }

        // Adds key words item into new slice builder.
        List<String> keyWords = SliceMetadata.from(null, s).getSliceKeywords();
        if (keyWords != null && keyWords.size() > 0) {
            Slice.Builder sb = new Slice.Builder(slice);
            for (String keyword : keyWords) {
                sb.addText(keyword, null);
            }
            slice.addSubSlice(sb.addHints(HINT_KEYWORDS).build());
        }

        // Adds top-level hints into new slice builder.
        List<String> hints = s.getHints();
        if (hints.size() > 0) {
            slice.addHints(hints);
        }
        return slice;
    }

    /**
     * Serialize a slice to an OutputStream.
     * <p>
     * The serialized slice can later be read into slice form again with {@link #parseSlice}.
     * Some slice types cannot be serialized, their handling is controlled by
     * {@link SerializeOptions}.
     *
     * @param s       The slice to serialize.
     * @param context Context used to load any resources in the slice.
     * @param output  The output of the serialization.
     * @param options Options defining how to handle non-serializable items.
     * @throws IllegalArgumentException if the slice cannot be serialized using the given options.
     */
    public static void serializeSlice(@NonNull Slice s, @NonNull final Context context,
            @NonNull OutputStream output,
            @NonNull final SerializeOptions options) throws IllegalArgumentException {
        synchronized (SliceItemHolder.sSerializeLock) {
            SliceItemHolder.sHandler = new SliceItemHolder.HolderHandler() {
                @Override
                public void handle(SliceItemHolder holder, String format) {
                    handleOptions(context, holder, format, options);
                }
            };
            ParcelUtils.toOutputStream(s, output);
            SliceItemHolder.sHandler = null;
        }
    }

    static void handleOptions(Context context, SliceItemHolder holder, String format,
            SerializeOptions options) {
        switch (format) {
            case FORMAT_IMAGE:
                switch (options.getImageMode()) {
                    case SerializeOptions.MODE_THROW:
                        throw new IllegalArgumentException("Cannot serialize icon");
                    case SerializeOptions.MODE_REMOVE:
                        // Remove the icon.
                        holder.mVersionedParcelable = null;
                        break;
                    case SerializeOptions.MODE_CONVERT:
                        holder.mVersionedParcelable = convert(context,
                                (IconCompat) holder.mVersionedParcelable, options);
                        break;
                }
                break;
            case FORMAT_REMOTE_INPUT:
                if (options.getActionMode() == SerializeOptions.MODE_THROW) {
                    throw new IllegalArgumentException("Cannot serialize action");
                } else {
                    holder.mParcelable = null;
                }
                break;
            case FORMAT_ACTION:
                switch (options.getActionMode()) {
                    case SerializeOptions.MODE_THROW:
                        throw new IllegalArgumentException("Cannot serialize action");
                    case SerializeOptions.MODE_REMOVE:
                        holder.mVersionedParcelable = null;
                        break;
                    case SerializeOptions.MODE_CONVERT:
                        holder.mParcelable = null;
                        break;
                }
                break;
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static IconCompat convert(Context context, IconCompat icon, SerializeOptions options) {
        switch (icon.getType()) {
            case Icon.TYPE_RESOURCE:
                return icon;
            default:
                byte[] data = SliceXml.convertToBytes(icon, context, options);
                return IconCompat.createWithData(data, 0, data.length);
        }
    }

    /**
     * Parse a slice that has been previously serialized.
     * <p>
     * Parses a slice that was serialized with {@link #serializeSlice}.
     * <p>
     * Note: Slices returned by this cannot be passed to {@link SliceConvert#unwrap(Slice)}.
     *
     * @param input    The input stream to read from.
     * @param encoding The encoding to read as.
     * @param listener Listener used to handle actions when reconstructing the slice.
     * @throws SliceParseException if the InputStream cannot be parsed.
     */
    public static @NonNull Slice parseSlice(@NonNull final Context context,
            @NonNull InputStream input, @NonNull String encoding,
            @NonNull final SliceActionListener listener) throws IOException, SliceParseException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(input);
        String parcelName = Slice.class.getName();

        bufferedInputStream.mark(parcelName.length() + 4);
        boolean usesParcel = doesStreamStartWith(parcelName, bufferedInputStream);
        bufferedInputStream.reset();
        if (usesParcel) {
            Slice slice;
            final SliceItem.ActionHandler handler = new SliceItem.ActionHandler() {
                @Override
                public void onAction(SliceItem item, Context context, Intent intent) {
                    listener.onSliceAction(item.getSlice().getUri(), context, intent);
                }
            };
            synchronized (SliceItemHolder.sSerializeLock) {
                SliceItemHolder.sHandler = new SliceItemHolder.HolderHandler() {
                    @Override
                    public void handle(SliceItemHolder holder, String format) {
                        setActionsAndUpdateIcons(holder, handler, context, format);
                    }
                };
                slice = ParcelUtils.fromInputStream(bufferedInputStream);
                slice.mHints = ArrayUtils.appendElement(String.class, slice.mHints,
                        SliceHints.HINT_CACHED);
                SliceItemHolder.sHandler = null;
            }
            return slice;
        }
        Slice s = SliceXml.parseSlice(context, bufferedInputStream, encoding, listener);
        s.mHints = ArrayUtils.appendElement(String.class, s.mHints, SliceHints.HINT_CACHED);
        return s;
    }

    static void setActionsAndUpdateIcons(SliceItemHolder holder,
            SliceItem.ActionHandler listener,
            Context context, String format) {
        switch (format) {
            case FORMAT_IMAGE:
                if (holder.mVersionedParcelable instanceof IconCompat) {
                    ((IconCompat) holder.mVersionedParcelable).checkResource(context);
                }
                break;
            case FORMAT_ACTION:
                holder.mCallback = listener;
                break;
        }
    }

    private static boolean doesStreamStartWith(String parcelName, BufferedInputStream inputStream) {
        byte[] data = parcelName.getBytes(Charset.forName("UTF-16"));
        byte[] buf = new byte[data.length];
        try {
            // Read out the int size of the string.
            if (inputStream.read(buf, 0, 4) < 0) {
                return false;
            }
            if (inputStream.read(buf, 0, buf.length) < 0) {
                return false;
            }
            return parcelName.equals(new String(buf, "UTF-16"));
        } catch (IOException e) {
            return false;
        }
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

        private Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.PNG;
        private int mQuality = 100;

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
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public Bitmap.CompressFormat getFormat() {
            return mFormat;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public int getQuality() {
            return mQuality;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_ACTION} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         *
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
         *
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

        /**
         * Sets the options to use when converting icons to be serialized. Only used if
         * the image mode is set to {@link #MODE_CONVERT}.
         *
         * @param format  The format to encode images with, default is
         *                {@link android.graphics.Bitmap.CompressFormat#PNG}.
         * @param quality The quality to use when encoding images.
         */
        public SerializeOptions setImageConversionFormat(Bitmap.CompressFormat format,
                int quality) {
            mFormat = format;
            mQuality = quality;
            return this;
        }
    }

    /**
     * A listener used to receive events on slices parsed with
     * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
     */
    public interface SliceActionListener {
        /**
         * Called when an action is triggered on a slice parsed with
         * {@link #parseSlice(Context, InputStream, String, SliceActionListener)}.
         *
         * @param actionUri The uri of the action selected.
         * @param context   The context passed to {@link SliceItem#fireAction(Context, Intent)}
         * @param intent    The intent passed to {@link SliceItem#fireAction(Context, Intent)}
         */
        void onSliceAction(Uri actionUri, Context context, Intent intent);
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
