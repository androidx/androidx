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
import static android.app.slice.Slice.HINT_ERROR;
import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_KEYWORDS;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LAST_UPDATED;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_PERMISSION_REQUEST;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.HINT_TTL;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.SliceConvert.unwrap;
import static androidx.slice.core.SliceHints.HINT_ACTIVITY;
import static androidx.slice.core.SliceHints.HINT_CACHED;
import static androidx.slice.core.SliceHints.HINT_END_OF_SECTION;
import static androidx.slice.core.SliceHints.HINT_OVERLAY;
import static androidx.slice.core.SliceHints.HINT_RAW;
import static androidx.slice.core.SliceHints.HINT_SELECTION_OPTION;
import static androidx.slice.core.SliceHints.HINT_SHOW_LABEL;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.slice.SliceManager;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.StringDef;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.util.Preconditions;
import androidx.slice.compat.SliceProviderCompat;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * A slice is a piece of app content and actions that can be surfaced outside of the app. A slice
 * is identified by a Uri and served via a {@link SliceProvider}.
 *
 * <p>Slices are constructed using {@link androidx.slice.builders.TemplateSliceBuilder}s
 * in a tree structure that provides the OS some information about how the content should be
 * displayed.
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@VersionedParcelize(allowSerialization = true, isCustom = true)
@Deprecated
public final class Slice extends CustomVersionedParcelable implements VersionedParcelable {

    /**
     * Key to retrieve an extra added to an intent when an item in a selection is selected.
     */
    public static final String EXTRA_SELECTION = "android.app.slice.extra.SELECTION";

    /**
     * Subtype to tag an item as representing the progress bar mode for a
     * {@link android.app.slice.Slice#SUBTYPE_RANGE}
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final String SUBTYPE_RANGE_MODE = "range_mode";

    private static final String HINTS = "hints";
    private static final String ITEMS = "items";
    private static final String URI = "uri";
    private static final String SPEC_TYPE = "type";
    private static final String SPEC_REVISION = "revision";

    static final String[] NO_HINTS = new String[0];
    static final SliceItem[] NO_ITEMS = new SliceItem[0];

    /**
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
            HINT_ERROR,
            HINT_ACTIVITY,
            HINT_CACHED,
            HINT_END_OF_SECTION,
            HINT_SELECTION_OPTION,
            HINT_RAW,
            HINT_OVERLAY,
            HINT_SHOW_LABEL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceHint { }

    @ParcelField(value = 1, defaultValue = "null")
    SliceSpec mSpec = null;

    @ParcelField(value = 2, defaultValue = "androidx.slice.Slice.NO_ITEMS")
    SliceItem[] mItems = NO_ITEMS;
    @ParcelField(value = 3, defaultValue = "androidx.slice.Slice.NO_HINTS")
    @SliceHint
    String[] mHints = NO_HINTS;
    @ParcelField(value = 4, defaultValue = "null")
    String mUri = null;

    /**
     */
    @RestrictTo(Scope.LIBRARY)
    Slice(ArrayList<SliceItem> items, @SliceHint String[] hints, Uri uri,
            SliceSpec spec) {
        mHints = hints;
        mItems = items.toArray(new SliceItem[items.size()]);
        mUri = uri.toString();
        mSpec = spec;
    }

    /**
     * Used for VersionedParcelable
     */
    @RestrictTo(Scope.LIBRARY)
    public Slice() {
    }

    /**
     */
    @RestrictTo(Scope.LIBRARY)
    @SuppressWarnings("deprecation")
    public Slice(@NonNull Bundle in) {
        mHints = in.getStringArray(HINTS);
        Parcelable[] items = in.getParcelableArray(ITEMS);
        mItems = new SliceItem[items.length];
        for (int i = 0; i < mItems.length; i++) {
            if (items[i] instanceof Bundle) {
                mItems[i] = new SliceItem((Bundle) items[i]);
            }
        }
        mUri = in.getParcelable(URI).toString();
        mSpec = in.containsKey(SPEC_TYPE)
                ? new SliceSpec(in.getString(SPEC_TYPE), in.getInt(SPEC_REVISION))
                : null;
    }

    /**
     */
    @RestrictTo(Scope.LIBRARY)
    public @NonNull Bundle toBundle() {
        Bundle b = new Bundle();
        b.putStringArray(HINTS, mHints);
        Parcelable[] p = new Parcelable[mItems.length];
        for (int i = 0; i < mItems.length; i++) {
            p[i] = mItems[i].toBundle();
        }
        b.putParcelableArray(ITEMS, p);
        b.putParcelable(URI, Uri.parse(mUri));
        if (mSpec != null) {
            b.putString(SPEC_TYPE, mSpec.getType());
            b.putInt(SPEC_REVISION, mSpec.getRevision());
        }
        return b;
    }

    /**
     * @return The spec for this slice
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public @Nullable SliceSpec getSpec() {
        return mSpec;
    }

    /**
     * @return The Uri that this Slice represents.
     */
    public @NonNull Uri getUri() {
        return Uri.parse(mUri);
    }

    /**
     * @return All child {@link SliceItem}s that this Slice contains.
     */
    public @NonNull List<SliceItem> getItems() {
        return Arrays.asList(mItems);
    }

    /**
     * @return
     */
    @RestrictTo(LIBRARY)
    public @NonNull SliceItem[] getItemArray() {
        return mItems;
    }

    /**
     * @return All hints associated with this Slice.
     */
    public @NonNull @SliceHint List<String> getHints() {
        return Arrays.asList(mHints);
    }

    /**
     */
    @RestrictTo(LIBRARY)
    public @NonNull @SliceHint String[] getHintArray() {
        return mHints;
    }

    /**
     */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public boolean hasHint(@NonNull @SliceHint String hint) {
        return ArrayUtils.contains(mHints, hint);
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public void onPreParceling(boolean isStream) {
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public void onPostParceling() {
        for (int i = mItems.length - 1; i >= 0; i--) {
            if (mItems[i].mObj == null) {
                mItems = ArrayUtils.removeElement(SliceItem.class, mItems, mItems[i]);
                if (mItems == null) {
                    mItems = new SliceItem[0];
                }
            }
        }
    }

    /**
     * A Builder used to construct {@link Slice}s
     */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public static class Builder {

        private final Uri mUri;
        private ArrayList<SliceItem> mItems = new ArrayList<>();
        private @SliceHint ArrayList<String> mHints = new ArrayList<>();
        private SliceSpec mSpec;
        private int mChildId;

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
        public Builder(@NonNull Builder parent) {
            mUri = parent.getChildUri();
        }

        private Uri getChildUri() {
            return mUri.buildUpon().appendPath("_gen")
                    .appendPath(String.valueOf(mChildId++)).build();
        }

        /**
         * Add the spec for this slice.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public @NonNull Builder setSpec(@Nullable SliceSpec spec) {
            mSpec = spec;
            return this;
        }

        /**
         * Add hints to the Slice being constructed
         */
        public @NonNull Builder addHints(@NonNull @SliceHint String... hints) {
            mHints.addAll(Arrays.asList(hints));
            return this;
        }

        /**
         * Add hints to the Slice being constructed
         */
        public @NonNull Builder addHints(@NonNull @SliceHint List<String> hints) {
            return addHints(hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a sub-slice to the slice being constructed
         */
        public @NonNull Builder addSubSlice(@NonNull Slice slice) {
            Preconditions.checkNotNull(slice);
            return addSubSlice(slice, null);
        }

        /**
         * Add a sub-slice to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addSubSlice(@NonNull Slice slice, @Nullable String subType) {
            Preconditions.checkNotNull(slice);
            mItems.add(new SliceItem(slice, FORMAT_SLICE, subType, slice.getHintArray()));
            return this;
        }

        /**
         * Add an action to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addAction(@NonNull PendingIntent action,
                @NonNull Slice s, @Nullable String subType) {
            Preconditions.checkNotNull(action);
            Preconditions.checkNotNull(s);
            @SliceHint String[] hints = s.getHintArray();
            mItems.add(new SliceItem(action, s, FORMAT_ACTION, subType, hints));
            return this;
        }

        /**
         * Add an action to the slice being constructed
         * @param subType Optional template-specific type information
         * @param action Callback to be triggered when a pending intent would normally be fired.
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addAction(@NonNull Slice s, @Nullable String subType,
                @NonNull SliceItem.ActionHandler action) {
            Preconditions.checkNotNull(s);
            @SliceHint String[] hints = s.getHintArray();
            mItems.add(new SliceItem(action, s, FORMAT_ACTION, subType, hints));
            return this;
        }

        /**
         * Add text to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addText(@Nullable CharSequence text, @Nullable String subType,
                @NonNull @SliceHint String... hints) {
            mItems.add(new SliceItem(text, FORMAT_TEXT, subType, hints));
            return this;
        }

        /**
         * Add text to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addText(@Nullable CharSequence text, @Nullable String subType,
                @NonNull @SliceHint List<String> hints) {
            return addText(text, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add an image to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addIcon(@NonNull IconCompat icon, @Nullable String subType,
                @NonNull @SliceHint String... hints) {
            Preconditions.checkNotNull(icon);
            if (isValidIcon(icon)) {
                mItems.add(new SliceItem(icon, FORMAT_IMAGE, subType, hints));
            }
            return this;
        }

        /**
         * Add an image to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addIcon(@NonNull IconCompat icon, @Nullable String subType,
                @NonNull @SliceHint List<String> hints) {
            Preconditions.checkNotNull(icon);
            if (isValidIcon(icon)) {
                return addIcon(icon, subType, hints.toArray(new String[hints.size()]));
            }
            return this;
        }

        /**
         * Add remote input to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public @NonNull Builder addRemoteInput(@NonNull RemoteInput remoteInput,
                @Nullable String subType, @NonNull @SliceHint List<String> hints) {
            Preconditions.checkNotNull(remoteInput);
            return addRemoteInput(remoteInput, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add remote input to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public @NonNull Builder addRemoteInput(@NonNull RemoteInput remoteInput,
                @Nullable String subType, @NonNull @SliceHint String... hints) {
            Preconditions.checkNotNull(remoteInput);
            mItems.add(new SliceItem(remoteInput, FORMAT_REMOTE_INPUT, subType, hints));
            return this;
        }

        /**
         * Add a int to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addInt(int value, @Nullable String subType,
                @NonNull @SliceHint String... hints) {
            mItems.add(new SliceItem(value, FORMAT_INT, subType, hints));
            return this;
        }

        /**
         * Add a int to the slice being constructed
         *
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addInt(int value, @Nullable String subType,
                @NonNull @SliceHint List<String> hints) {
            return addInt(value, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a long to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addLong(long time, @Nullable String subType,
                @NonNull @SliceHint String... hints) {
            mItems.add(new SliceItem(time, FORMAT_LONG, subType, hints));
            return this;
        }

        /**
         * Add a long to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addLong(long time, @Nullable String subType,
                @NonNull @SliceHint List<String> hints) {
            return addLong(time, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a timestamp to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         * @deprecated TO BE REMOVED
         */
        @Deprecated
        public Builder addTimestamp(long time, @Nullable String subType,
                @SliceHint String... hints) {
            mItems.add(new SliceItem(time, FORMAT_LONG, subType, hints));
            return this;
        }

        /**
         * Add a timestamp to the slice being constructed
         * @param subType Optional template-specific type information
         * @see SliceItem#getSubType()
         */
        public @NonNull Builder addTimestamp(long time, @Nullable String subType,
                @NonNull @SliceHint List<String> hints) {
            return addTimestamp(time, subType, hints.toArray(new String[hints.size()]));
        }

        /**
         * Add a SliceItem to the slice being constructed.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull Builder addItem(@NonNull SliceItem item) {
            mItems.add(item);
            return this;
        }

        /**
         * Construct the slice.
         */
        public @NonNull Slice build() {
            return new Slice(mItems, mHints.toArray(new String[mHints.size()]), mUri, mSpec);
        }
    }

    /**
     * @return A string representation of this slice.
     */
    @NonNull
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * @return A string representation of this slice.
     */
    @NonNull
    @RestrictTo(Scope.LIBRARY)
    public String toString(@NonNull String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append("Slice ");
        if (mHints.length > 0) {
            appendHints(sb, mHints);
            sb.append(' ');
        }
        sb.append('[');
        sb.append(mUri);
        sb.append("] {\n");
        final String nextIndent = indent + "  ";
        for (int i = 0; i < mItems.length; i++) {
            SliceItem item = mItems[i];
            sb.append(item.toString(nextIndent));
        }
        sb.append(indent);
        sb.append('}');
        return sb.toString();
    }

    /**
     */
    @RestrictTo(Scope.LIBRARY)
    public static void appendHints(@NonNull StringBuilder sb, @Nullable String[] hints) {
        if (hints == null || hints.length == 0) return;

        sb.append('(');
        int end = hints.length - 1;
        for (int i = 0; i < end; i++) {
            sb.append(hints[i]);
            sb.append(", ");
        }
        sb.append(hints[end]);
        sb.append(")");
    }

    /**
     * Turns a slice Uri into slice content.
     *
     * @param context Context to be used.
     * @param uri The URI to a slice provider
     * @return The Slice provided by the app or null if none is given.
     * @see Slice
     */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    @Nullable
    public static Slice bindSlice(@NonNull Context context, @NonNull Uri uri,
            @Nullable Set<SliceSpec> supportedSpecs) {
        if (Build.VERSION.SDK_INT >= 28) {
            return callBindSlice(context, uri, supportedSpecs);
        } else {
            return SliceProviderCompat.bindSlice(context, uri, supportedSpecs);
        }
    }

    @RequiresApi(28)
    private static Slice callBindSlice(Context context, Uri uri,
            Set<SliceSpec> supportedSpecs) {
        SliceManager sliceManager = Api23Impl.getSystemService(context, SliceManager.class);
        return SliceConvert.wrap(Api28Impl.bindSlice(sliceManager, uri, unwrap(supportedSpecs)),
                context);
    }

    /**
     */
    @RestrictTo(LIBRARY)
    static boolean isValidIcon(IconCompat icon) {
        if (icon == null) {
            return false;
        }
        if (icon.mType == Icon.TYPE_RESOURCE && icon.getResId() == 0) {
            throw new IllegalArgumentException("Failed to add icon, invalid resource id: "
                    + icon.getResId());
        }
        return true;
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static <T> T getSystemService(Context context, Class<T> serviceClass) {
            return context.getSystemService(serviceClass);
        }

    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static android.app.slice.Slice bindSlice(SliceManager sliceManager, Uri uri,
                Set<android.app.slice.SliceSpec> supportedSpecs) {
            return sliceManager.bindSlice(uri, supportedSpecs);
        }
    }
}
