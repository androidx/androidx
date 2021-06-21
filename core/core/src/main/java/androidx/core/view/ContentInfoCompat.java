/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.view;

import android.content.ClipData;
import android.content.ClipDescription;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all the relevant data for a request to {@link OnReceiveContentListener}.
 */
// This class has the "Compat" suffix because it will integrate with (ie, wrap) the SDK API once
// that is available.
public final class ContentInfoCompat {

    /**
     * Specifies the UI through which content is being inserted. Future versions of Android may
     * support additional values.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {SOURCE_APP, SOURCE_CLIPBOARD, SOURCE_INPUT_METHOD, SOURCE_DRAG_AND_DROP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Source {
    }

    /**
     * Specifies that the operation was triggered by the app that contains the target view.
     */
    public static final int SOURCE_APP = 0;

    /**
     * Specifies that the operation was triggered by a paste from the clipboard (e.g. "Paste" or
     * "Paste as plain text" action in the insertion/selection menu).
     */
    public static final int SOURCE_CLIPBOARD = 1;

    /**
     * Specifies that the operation was triggered from the soft keyboard (also known as input
     * method editor or IME). See https://developer.android.com/guide/topics/text/image-keyboard
     * for more info.
     */
    public static final int SOURCE_INPUT_METHOD = 2;

    /**
     * Specifies that the operation was triggered by the drag/drop framework. See
     * https://developer.android.com/guide/topics/ui/drag-drop for more info.
     */
    public static final int SOURCE_DRAG_AND_DROP = 3;

    /**
     * Returns the symbolic name of the given source.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    static String sourceToString(@Source int source) {
        switch (source) {
            case SOURCE_APP: return "SOURCE_APP";
            case SOURCE_CLIPBOARD: return "SOURCE_CLIPBOARD";
            case SOURCE_INPUT_METHOD: return "SOURCE_INPUT_METHOD";
            case SOURCE_DRAG_AND_DROP: return "SOURCE_DRAG_AND_DROP";
        }
        return String.valueOf(source);
    }

    /**
     * Flags to configure the insertion behavior.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true, value = {FLAG_CONVERT_TO_PLAIN_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    /**
     * Flag requesting that the content should be converted to plain text prior to inserting.
     */
    public static final int FLAG_CONVERT_TO_PLAIN_TEXT = 1 << 0;

    /**
     * Returns the symbolic names of the set flags or {@code "0"} if no flags are set.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    static String flagsToString(@Flags int flags) {
        if ((flags & FLAG_CONVERT_TO_PLAIN_TEXT) != 0) {
            return "FLAG_CONVERT_TO_PLAIN_TEXT";
        }
        return String.valueOf(flags);
    }

    @NonNull
    final ClipData mClip;
    @Source
    final int mSource;
    @Flags
    final int mFlags;
    @Nullable
    final Uri mLinkUri;
    @Nullable
    final Bundle mExtras;

    ContentInfoCompat(Builder b) {
        this.mClip = Preconditions.checkNotNull(b.mClip);
        this.mSource = Preconditions.checkArgumentInRange(b.mSource, 0, SOURCE_DRAG_AND_DROP,
                "source");
        this.mFlags = Preconditions.checkFlagsArgument(b.mFlags, FLAG_CONVERT_TO_PLAIN_TEXT);
        this.mLinkUri = b.mLinkUri;
        this.mExtras = b.mExtras;
    }

    @NonNull
    @Override
    public String toString() {
        return "ContentInfoCompat{"
                + "clip=" + mClip.getDescription()
                + ", source=" + sourceToString(mSource)
                + ", flags=" + flagsToString(mFlags)
                + (mLinkUri == null ? "" : ", hasLinkUri(" + mLinkUri.toString().length() + ")")
                + (mExtras == null ? "" : ", hasExtras")
                + "}";
    }

    /**
     * The data to be inserted.
     */
    @NonNull
    public ClipData getClip() {
        return mClip;
    }

    /**
     * The source of the operation. See {@code SOURCE_} constants. Future versions of Android
     * may pass additional values.
     */
    @Source
    public int getSource() {
        return mSource;
    }

    /**
     * Optional flags that control the insertion behavior. See {@code FLAG_} constants.
     */
    @Flags
    public int getFlags() {
        return mFlags;
    }

    /**
     * Optional http/https URI for the content that may be provided by the IME. This is only
     * populated if the source is {@link #SOURCE_INPUT_METHOD} and if a non-empty
     * {@link android.view.inputmethod.InputContentInfo#getLinkUri linkUri} was passed by the
     * IME.
     */
    @Nullable
    public Uri getLinkUri() {
        return mLinkUri;
    }

    /**
     * Optional additional metadata. If the source is {@link #SOURCE_INPUT_METHOD}, this will
     * include the {@link android.view.inputmethod.InputConnection#commitContent opts} passed by
     * the IME.
     */
    @Nullable
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Partitions this content based on the given predicate.
     *
     * <p>This function classifies the content and organizes it into a pair, grouping the items
     * that matched vs didn't match the predicate.
     *
     * <p>Except for the {@link ClipData} items, the returned objects will contain all the same
     * metadata as this {@link ContentInfoCompat}.
     *
     * @param itemPredicate The predicate to test each {@link ClipData.Item} to determine which
     *                      partition to place it into.
     * @return A pair containing the partitioned content. The pair's first object will have the
     * content that matched the predicate, or null if none of the items matched. The pair's
     * second object will have the content that didn't match the predicate, or null if all of
     * the items matched.
     */
    @NonNull
    public Pair<ContentInfoCompat, ContentInfoCompat> partition(
            @NonNull androidx.core.util.Predicate<ClipData.Item> itemPredicate) {
        if (mClip.getItemCount() == 1) {
            boolean matched = itemPredicate.test(mClip.getItemAt(0));
            return Pair.create(matched ? this : null, matched ? null : this);
        }
        ArrayList<ClipData.Item> acceptedItems = new ArrayList<>();
        ArrayList<ClipData.Item> remainingItems = new ArrayList<>();
        for (int i = 0; i < mClip.getItemCount(); i++) {
            ClipData.Item item = mClip.getItemAt(i);
            if (itemPredicate.test(item)) {
                acceptedItems.add(item);
            } else {
                remainingItems.add(item);
            }
        }
        if (acceptedItems.isEmpty()) {
            return Pair.create(null, this);
        }
        if (remainingItems.isEmpty()) {
            return Pair.create(this, null);
        }
        ContentInfoCompat accepted = new Builder(this)
                .setClip(buildClipData(mClip.getDescription(), acceptedItems))
                .build();
        ContentInfoCompat remaining = new Builder(this)
                .setClip(buildClipData(mClip.getDescription(), remainingItems))
                .build();
        return Pair.create(accepted, remaining);
    }

    private static ClipData buildClipData(ClipDescription description,
            List<ClipData.Item> items) {
        ClipData clip = new ClipData(new ClipDescription(description), items.get(0));
        for (int i = 1; i < items.size(); i++) {
            clip.addItem(items.get(i));
        }
        return clip;
    }

    /**
     * Builder for {@link ContentInfoCompat}.
     */
    public static final class Builder {
        @NonNull
        ClipData mClip;
        @Source
        int mSource;
        @Flags
        int mFlags;
        @Nullable
        Uri mLinkUri;
        @Nullable
        Bundle mExtras;

        /**
         * Creates a new builder initialized with the data from the given builder.
         */
        public Builder(@NonNull ContentInfoCompat other) {
            mClip = other.mClip;
            mSource = other.mSource;
            mFlags = other.mFlags;
            mLinkUri = other.mLinkUri;
            mExtras = other.mExtras;
        }

        /**
         * Creates a new builder.
         *
         * @param clip   The data to insert.
         * @param source The source of the operation. See {@code SOURCE_} constants.
         */
        public Builder(@NonNull ClipData clip, @Source int source) {
            mClip = clip;
            mSource = source;
        }

        /**
         * Sets the data to be inserted.
         *
         * @param clip The data to insert.
         * @return this builder
         */
        @NonNull
        public Builder setClip(@NonNull ClipData clip) {
            mClip = clip;
            return this;
        }

        /**
         * Sets the source of the operation.
         *
         * @param source The source of the operation. See {@code SOURCE_} constants.
         * @return this builder
         */
        @NonNull
        public Builder setSource(@Source int source) {
            mSource = source;
            return this;
        }

        /**
         * Sets flags that control content insertion behavior.
         *
         * @param flags Optional flags to configure the insertion behavior. Use 0 for default
         *              behavior. See {@code FLAG_} constants.
         * @return this builder
         */
        @NonNull
        public Builder setFlags(@Flags int flags) {
            mFlags = flags;
            return this;
        }

        /**
         * Sets the http/https URI for the content. See
         * {@link android.view.inputmethod.InputContentInfo#getLinkUri} for more info.
         *
         * @param linkUri Optional http/https URI for the content.
         * @return this builder
         */
        @NonNull
        public Builder setLinkUri(@Nullable Uri linkUri) {
            mLinkUri = linkUri;
            return this;
        }

        /**
         * Sets additional metadata.
         *
         * @param extras Optional bundle with additional metadata.
         * @return this builder
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * @return A new {@link ContentInfoCompat} instance with the data from this builder.
         */
        @NonNull
        public ContentInfoCompat build() {
            return new ContentInfoCompat(this);
        }
    }
}
