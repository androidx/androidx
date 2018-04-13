/*
 * Copyright (C) 2017 The Android Open Source Project
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
import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.SliceConvert.unwrap;
import static androidx.slice.core.SliceHints.HINT_KEYWORDS;
import static androidx.slice.core.SliceHints.HINT_LAST_UPDATED;
import static androidx.slice.core.SliceHints.HINT_PERMISSION_REQUEST;
import static androidx.slice.core.SliceHints.HINT_TTL;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.slice.SliceManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.BuildCompat;
import androidx.core.util.Consumer;
import androidx.slice.compat.SliceProviderCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A slice is a piece of app content and actions that can be surfaced outside of the app.
 *
 * <p>They are constructed using {@link androidx.slice.builders.TemplateSliceBuilder}s
 * in a tree structure that provides the OS some information about how the content should be
 * displayed.
 */
public final class Slice {

    private static final String HINTS = "hints";
    private static final String ITEMS = "items";
    private static final String URI = "uri";
    private static final String SPEC_TYPE = "type";
    private static final String SPEC_REVISION = "revision";
    private final SliceSpec mSpec;

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @StringDef({
            HINT_TITLE,
            HINT_LIST,
            HINT_LIST_ITEM,
            HINT_LARGE,
            HINT_ACTIONS,
            HINT_SELECTED,
            HINT_HORIZONTAL,
            HINT_NO_TINT,
            HINT_PARTIAL,
            HINT_SUMMARY,
            HINT_SEE_MORE,
            HINT_SHORTCUT,
            HINT_KEYWORDS,
            HINT_TTL,
            HINT_LAST_UPDATED,
            HINT_PERMISSION_REQUEST,
    })
    public @interface SliceHint{ }

    private final SliceItem[] mItems;
    private final @SliceHint String[] mHints;
    private Uri mUri;

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    Slice(ArrayList<SliceItem> items, @SliceHint String[] hints, Uri uri,
            SliceSpec spec) {
        mHints = hints;
        mItems = items.toArray(new SliceItem[items.size()]);
        mUri = uri;
        mSpec = spec;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public Slice(Bundle in) {
        mHints = in.getStringArray(HINTS);
        Parcelable[] items = in.getParcelableArray(ITEMS);
        mItems = new SliceItem[items.length];
        for (int i = 0; i < mItems.length; i++) {
            if (items[i] instanceof Bundle) {
                mItems[i] = new SliceItem((Bundle) items[i]);
            }
        }
        mUri = in.getParcelable(URI);
        mSpec = in.containsKey(SPEC_TYPE)
                ? new SliceSpec(in.getString(SPEC_TYPE), in.getInt(SPEC_REVISION))
                : null;
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray(HINTS, mHints);
        Parcelable[] p = new Parcelable[mItems.length];
        for (int i = 0; i < mItems.length; i++) {
            p[i] = mItems[i].toBundle();
        }
        b.putParcelableArray(ITEMS, p);
        b.putParcelable(URI, mUri);
        if (mSpec != null) {
            b.putString(SPEC_TYPE, mSpec.getType());
            b.putInt(SPEC_REVISION, mSpec.getRevision());
        }
        return b;
    }

    /**
     * @return The spec for this slice
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable SliceSpec getSpec() {
        return mSpec;
    }

    /**
     * @return The Uri that this Slice represents.
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * @return All child {@link SliceItem}s that this Slice contains.
     */
    public List<SliceItem> getItems() {
        return Arrays.asList(mItems);
    }

    /**
     * @return All hints associated with this Slice.
     */
    public @SliceHint List<String> getHints() {
        return Arrays.asList(mHints);
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public boolean hasHint(@SliceHint String hint) {
        return ArrayUtils.contains(mHints, hint);
    }

    /**
     * A Builder used to construct {@link Slice}s
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static class Builder {

        private final Uri mUri;
        private ArrayList<SliceItem> mItems = new ArrayList<>();
        private @SliceHint ArrayList<String> mHints = new ArrayList<>();
        private SliceSpec mSpec;

        /**
         * Create a builder which will construct a {@link Slice} for the Given Uri.
         * @param uri Uri to tag for this slice.
         */
        public Builder(@NonNull Uri uri) {
            mUri = uri;
        }

        /**
         * Create a builder for a {@link Slice} that is a sub-slice of the slice
         * being constructed by the provided builder.
         * @param parent The builder constructing the parent slice
         */
        public Builder(@NonNull Slice.Builder parent) {
            mUri = parent.mUri.buildUpon().appendPath("_gen")
                    .appendPath(String.valueOf(mItems.size())).build();
        }

        /**
         * Add the spec for this slice.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Builder setSpec(SliceSpec spec) {
            mSpec = spec;
            return this;
        }

        /**
         * Add hints to the Slice being constructed
         */
        public Builder addHints(@SliceHint String... hints) {
            mHints.addAll(Arrays.asList(hints));
            return this;
        }

        /**
         * Add hints to the Slice being constructed
         */
        public Builder addHints(@SliceHint List<String> hints) {
            return addHints(hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a sub-slice to the slice being constructed
         */
        public Builder addSubSlice(@NonNull Slice slice) {
            return addSubSlice(slice, null);
        }

        /**
         * Add a sub-slice to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addSubSlice(@NonNull Slice slice, String subType) {
            mItems.add(new SliceItem(slice, FORMAT_SLICE, subType, slice.getHints().toArray(
                    new String[slice.getHints().size()])));
            return this;
        }

        /**
         * Add an action to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Slice.Builder addAction(@NonNull PendingIntent action,
                @NonNull Slice s, @Nullable String subType) {
            @SliceHint String[] hints = s != null
                    ? s.getHints().toArray(new String[s.getHints().size()]) : new String[0];
            mItems.add(new SliceItem(action, s, FORMAT_ACTION, subType, hints));
            return this;
        }

        /**
         * Add an action to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Slice.Builder addAction(@NonNull Consumer<Uri> action,
                @NonNull Slice s, @Nullable String subType) {
            @SliceHint String[] hints = s != null
                    ? s.getHints().toArray(new String[s.getHints().size()]) : new String[0];
            mItems.add(new SliceItem(action, s, FORMAT_ACTION, subType, hints));
            return this;
        }

        /**
         * Add text to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addText(CharSequence text, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(text, FORMAT_TEXT, subType, hints));
            return this;
        }

        /**
         * Add text to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addText(CharSequence text, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addText(text, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add an image to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addIcon(IconCompat icon, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(icon, FORMAT_IMAGE, subType, hints));
            return this;
        }

        /**
         * Add an image to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addIcon(IconCompat icon, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addIcon(icon, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add remote input to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Slice.Builder addRemoteInput(RemoteInput remoteInput, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addRemoteInput(remoteInput, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add remote input to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public Slice.Builder addRemoteInput(RemoteInput remoteInput, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(remoteInput, FORMAT_REMOTE_INPUT, subType, hints));
            return this;
        }

        /**
         * Add a int to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addInt(int value, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(value, FORMAT_INT, subType, hints));
            return this;
        }

        /**
         * Add a int to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Builder addInt(int value, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addInt(value, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a long to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Slice.Builder addLong(long time, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(time, FORMAT_LONG, subType, hints));
            return this;
        }

        /**
         * Add a long to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Slice.Builder addLong(long time, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addLong(time, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a timestamp to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        public Slice.Builder addTimestamp(long time, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(time, FORMAT_LONG, subType, hints));
            return this;
        }

        /**
         * Add a timestamp to the slice being constructed
         * @param subType Optional template-specific type information
         * @see {@link SliceItem#getSubType()}
         */
        public Slice.Builder addTimestamp(long time, @Nullable String subType,
                @SliceHint List<String> hints) {
            return addTimestamp(time, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a SliceItem to the slice being constructed.
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        public Slice.Builder addItem(SliceItem item) {
            mItems.add(item);
            return this;
        }

        /**
         * Construct the slice.
         */
        public Slice build() {
            return new Slice(mItems, mHints.toArray(new String[mHints.size()]), mUri, mSpec);
        }
    }

    /**
     * @return A string representation of this slice.
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * @return A string representation of this slice.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append("slice ");
        addHints(sb, mHints);
        sb.append("{\n");
        String nextIndent = indent + "  ";
        for (int i = 0; i < mItems.length; i++) {
            SliceItem item = mItems[i];
            sb.append(item.toString(nextIndent));
        }
        sb.append(indent);
        sb.append("}");
        return sb.toString();
    }

    /**
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static void addHints(StringBuilder sb, String[] hints) {
        if (hints == null || hints.length == 0) return;

        sb.append("(");
        int end = hints.length - 1;
        for (int i = 0; i < end; i++) {
            sb.append(hints[i]);
            sb.append(", ");
        }
        sb.append(hints[end]);
        sb.append(") ");
    }

    /**
     * Turns a slice Uri into slice content.
     *
     * @hide
     * @param context Context to be used.
     * @param uri The URI to a slice provider
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @SuppressWarnings("NewApi") // Lint doesn't understand BuildCompat.
    @Nullable
    public static Slice bindSlice(Context context, @NonNull Uri uri,
            Set<SliceSpec> supportedSpecs) {
        if (BuildCompat.isAtLeastP()) {
            return callBindSlice(context, uri, supportedSpecs);
        } else {
            return SliceProviderCompat.bindSlice(context, uri, supportedSpecs);
        }
    }

    @RequiresApi(28)
    private static Slice callBindSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        return SliceConvert.wrap(context.getSystemService(SliceManager.class)
                .bindSlice(uri, unwrap(supportedSpecs)));
    }
}
