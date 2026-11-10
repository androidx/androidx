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
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.ContentInfo;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Holds all the relevant data for a request to {@link OnReceiveContentListener}. This is a
 * backward-compatible wrapper for the platform class {@link ContentInfo}.
 */
public final class ContentInfoCompat {

    /**
     * Specifies the UI through which content is being inserted. Future versions of Android may
     * support additional values.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {SOURCE_APP, SOURCE_CLIPBOARD, SOURCE_INPUT_METHOD, SOURCE_DRAG_AND_DROP,
            SOURCE_AUTOFILL, SOURCE_PROCESS_TEXT})
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
     * Specifies that the operation was triggered by the autofill framework. See
     * https://developer.android.com/guide/topics/text/autofill for more info.
     */
    public static final int SOURCE_AUTOFILL = 4;

    /**
     * Specifies that the operation was triggered by a result from a
     * {@link android.content.Intent#ACTION_PROCESS_TEXT PROCESS_TEXT} action in the selection
     * menu.
     */
    public static final int SOURCE_PROCESS_TEXT = 5;

    /**
     * Returns the symbolic name of the given source.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    static @NonNull String sourceToString(@Source int source) {
        switch (source) {
            case SOURCE_APP: return "SOURCE_APP";
            case SOURCE_CLIPBOARD: return "SOURCE_CLIPBOARD";
            case SOURCE_INPUT_METHOD: return "SOURCE_INPUT_METHOD";
            case SOURCE_DRAG_AND_DROP: return "SOURCE_DRAG_AND_DROP";
            case SOURCE_AUTOFILL: return "SOURCE_AUTOFILL";
            case SOURCE_PROCESS_TEXT: return "SOURCE_PROCESS_TEXT";
        }
        return String.valueOf(source);
    }

    /**
     * Flags to configure the insertion behavior.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true, value = {FLAG_CONVERT_TO_PLAIN_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    /**
     * Flag requesting that the content should be converted to plain text prior to inserting.
     */
    @SuppressWarnings("PointlessBitwiseExpression")
    public static final int FLAG_CONVERT_TO_PLAIN_TEXT = 1 << 0;

    /**
     * Returns the symbolic names of the set flags or {@code "0"} if no flags are set.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    static @NonNull String flagsToString(@Flags int flags) {
        if ((flags & FLAG_CONVERT_TO_PLAIN_TEXT) != 0) {
            return "FLAG_CONVERT_TO_PLAIN_TEXT";
        }
        return String.valueOf(flags);
    }

    private final @NonNull Compat mCompat;

    private ContentInfoCompat(@NonNull Compat compat) {
        mCompat = compat;
    }

    /**
     * Provides a backward-compatible wrapper for {@link ContentInfo}.
     *
     * <p>This method is not supported on devices running SDK <= 30 since the platform
     * class will not be available.
     *
     * @param platContentInfo platform class to wrap, must not be null
     * @return wrapped class
     */
    @RequiresApi(31)
    public static @NonNull ContentInfoCompat toContentInfoCompat(
            @NonNull ContentInfo platContentInfo) {
        return new ContentInfoCompat(new Compat31Impl(platContentInfo));
    }

    /**
     * Provides the {@link ContentInfo} represented by this object.
     *
     * <p>This method is not supported on devices running SDK <= 30 since the platform
     * class will not be available.
     *
     * @return platform class object
     * @see ContentInfoCompat#toContentInfoCompat
     */
    @RequiresApi(31)
    public @NonNull ContentInfo toContentInfo() {
        return Objects.requireNonNull(mCompat.getWrapped());
    }

    @Override
    public @NonNull String toString() {
        return mCompat.toString();
    }

    /**
     * The data to be inserted.
     */
    public @NonNull ClipData getClip() {
        return mCompat.getClip();
    }

    /**
     * The source of the operation. See {@code SOURCE_} constants. Future versions of Android
     * may pass additional values.
     */
    @Source
    public int getSource() {
        return mCompat.getSource();
    }

    /**
     * Optional flags that control the insertion behavior. See {@code FLAG_} constants.
     */
    @Flags
    public int getFlags() {
        return mCompat.getFlags();
    }

    /**
     * Optional http/https URI for the content that may be provided by the IME. This is only
     * populated if the source is {@link #SOURCE_INPUT_METHOD} and if a non-empty
     * {@link android.view.inputmethod.InputContentInfo#getLinkUri linkUri} was passed by the
     * IME.
     */
    public @Nullable Uri getLinkUri() {
        return mCompat.getLinkUri();
    }

    /**
     * Optional additional metadata. If the source is {@link #SOURCE_INPUT_METHOD}, this will
     * include the {@link android.view.inputmethod.InputConnection#commitContent opts} passed by
     * the IME.
     */
    public @Nullable Bundle getExtras() {
        return mCompat.getExtras();
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
    public @NonNull Pair<ContentInfoCompat, ContentInfoCompat> partition(
            androidx.core.util.@NonNull Predicate<ClipData.Item> itemPredicate) {
        ClipData clip = mCompat.getClip();
        if (clip.getItemCount() == 1) {
            boolean matched = itemPredicate.test(clip.getItemAt(0));
            return Pair.create(matched ? this : null, matched ? null : this);
        }
        Pair<ClipData, ClipData> split = ContentInfoCompat.partition(clip, itemPredicate);
        if (split.first == null) {
            return Pair.create(null, this);
        } else if (split.second == null) {
            return Pair.create(this, null);
        }
        return Pair.create(
                new ContentInfoCompat.Builder(this).setClip(split.first).build(),
                new ContentInfoCompat.Builder(this).setClip(split.second).build());
    }

    static @NonNull Pair<ClipData, ClipData> partition(@NonNull ClipData clip,
            androidx.core.util.@NonNull Predicate<ClipData.Item> itemPredicate) {
        ArrayList<ClipData.Item> acceptedItems = null;
        ArrayList<ClipData.Item> remainingItems = null;
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            if (itemPredicate.test(item)) {
                acceptedItems = (acceptedItems == null) ? new ArrayList<>() : acceptedItems;
                acceptedItems.add(item);
            } else {
                remainingItems = (remainingItems == null) ? new ArrayList<>() : remainingItems;
                remainingItems.add(item);
            }
        }
        if (acceptedItems == null) {
            return Pair.create(null, clip);
        }
        if (remainingItems == null) {
            return Pair.create(clip, null);
        }
        return Pair.create(
                buildClipData(clip.getDescription(), acceptedItems),
                buildClipData(clip.getDescription(), remainingItems));
    }

    static @NonNull ClipData buildClipData(@NonNull ClipDescription description,
            @NonNull List<ClipData.Item> items) {
        ClipData clip = new ClipData(new ClipDescription(description), items.get(0));
        for (int i = 1; i < items.size(); i++) {
            clip.addItem(items.get(i));
        }
        return clip;
    }

    /**
     * Partitions content based on the given predicate.
     *
     * <p>This function classifies the content and organizes it into a pair, grouping the items
     * that matched vs didn't match the predicate.
     *
     * <p>Except for the {@link ClipData} items, the returned objects will contain all the same
     * metadata as the passed-in {@link ContentInfo}.
     *
     * @param payload payload to add to returned pair.
     * @param itemPredicate The predicate to test each {@link ClipData.Item} to determine which
     *                      partition to place it into.
     * @return A pair containing the partitioned content. The pair's first object will have the
     * content that matched the predicate, or null if none of the items matched. The pair's
     * second object will have the content that didn't match the predicate, or null if all of
     * the items matched.
     */
    @RequiresApi(31)
    public static @NonNull Pair<ContentInfo, ContentInfo> partition(@NonNull ContentInfo payload,
            @NonNull Predicate<ClipData.Item> itemPredicate) {
        return Api31Impl.partition(payload, itemPredicate);
    }

    @RequiresApi(31)
    private static final class Api31Impl {
        private Api31Impl() {}

        public static @NonNull Pair<ContentInfo, ContentInfo> partition(
                @NonNull ContentInfo payload, @NonNull Predicate<ClipData.Item> itemPredicate) {
            ClipData clip = payload.getClip();
            if (clip.getItemCount() == 1) {
                boolean matched = itemPredicate.test(clip.getItemAt(0));
                return Pair.create(matched ? payload : null, matched ? null : payload);
            }
            Pair<ClipData, ClipData> split = ContentInfoCompat.partition(clip, itemPredicate::test);
            if (split.first == null) {
                return Pair.create(null, payload);
            } else if (split.second == null) {
                return Pair.create(payload, null);
            }
            return Pair.create(
                    new ContentInfo.Builder(payload).setClip(split.first).build(),
                    new ContentInfo.Builder(payload).setClip(split.second).build());
        }
    }

    private interface Compat {
        @Nullable ContentInfo getWrapped();
        @NonNull ClipData getClip();
        @Source
        int getSource();
        @Flags
        int getFlags();
        @Nullable Uri getLinkUri();
        @Nullable Bundle getExtras();
    }

    private static final class CompatImpl implements Compat {
        private final @NonNull ClipData mClip;
        @Source
        private final int mSource;
        @Flags
        private final int mFlags;
        private final @Nullable Uri mLinkUri;
        private final @Nullable Bundle mExtras;

        CompatImpl(BuilderCompatImpl b) {
            mClip = Preconditions.checkNotNull(b.mClip);
            mSource = Preconditions.checkArgumentInRange(b.mSource, 0, SOURCE_PROCESS_TEXT,
                    "source");
            mFlags = Preconditions.checkFlagsArgument(b.mFlags, FLAG_CONVERT_TO_PLAIN_TEXT);
            mLinkUri = b.mLinkUri;
            mExtras = b.mExtras;
        }

        @Override
        public @Nullable ContentInfo getWrapped() {
            return null;
        }

        @Override
        public @NonNull ClipData getClip() {
            return mClip;
        }

        @Source
        @Override
        public int getSource() {
            return mSource;
        }

        @Flags
        @Override
        public int getFlags() {
            return mFlags;
        }

        @Override
        public @Nullable Uri getLinkUri() {
            return mLinkUri;
        }

        @Override
        public @Nullable Bundle getExtras() {
            return mExtras;
        }

        @Override
        public @NonNull String toString() {
            return "ContentInfoCompat{"
                    + "clip=" + mClip.getDescription()
                    + ", source=" + sourceToString(mSource)
                    + ", flags=" + flagsToString(mFlags)
                    + (mLinkUri == null ? "" : ", hasLinkUri(" + mLinkUri.toString().length() + ")")
                    + (mExtras == null ? "" : ", hasExtras")
                    + "}";
        }
    }

    @RequiresApi(31)
    private static final class Compat31Impl implements Compat {
        private final @NonNull ContentInfo mWrapped;

        Compat31Impl(@NonNull ContentInfo wrapped) {
            mWrapped = Preconditions.checkNotNull(wrapped);
        }

        @Override
        public @NonNull ContentInfo getWrapped() {
            return mWrapped;
        }

        @Override
        public @NonNull ClipData getClip() {
            return mWrapped.getClip();
        }

        @Source
        @Override
        public int getSource() {
            return mWrapped.getSource();
        }

        @Flags
        @Override
        public int getFlags() {
            return mWrapped.getFlags();
        }

        @Override
        public @Nullable Uri getLinkUri() {
            return mWrapped.getLinkUri();
        }

        @Override
        public @Nullable Bundle getExtras() {
            return mWrapped.getExtras();
        }

        @Override
        public @NonNull String toString() {
            return "ContentInfoCompat{" + mWrapped + "}";
        }
    }

    /**
     * Builder for {@link ContentInfoCompat}.
     */
    public static final class Builder {
        private final @NonNull BuilderCompat mBuilderCompat;

        /**
         * Creates a new builder initialized with the data from the given object (shallow copy).
         */
        public Builder(@NonNull ContentInfoCompat other) {
            if (Build.VERSION.SDK_INT >= 31) {
                mBuilderCompat = new BuilderCompat31Impl(other);
            } else {
                mBuilderCompat = new BuilderCompatImpl(other);
            }
        }

        /**
         * Creates a new builder.
         *
         * @param clip   The data to insert.
         * @param source The source of the operation. See {@code SOURCE_} constants.
         */
        public Builder(@NonNull ClipData clip, @Source int source) {
            if (Build.VERSION.SDK_INT >= 31) {
                mBuilderCompat = new BuilderCompat31Impl(clip, source);
            } else {
                mBuilderCompat = new BuilderCompatImpl(clip, source);
            }
        }

        /**
         * Sets the data to be inserted.
         *
         * @param clip The data to insert.
         * @return this builder
         */
        public @NonNull Builder setClip(@NonNull ClipData clip) {
            mBuilderCompat.setClip(clip);
            return this;
        }

        /**
         * Sets the source of the operation.
         *
         * @param source The source of the operation. See {@code SOURCE_} constants.
         * @return this builder
         */
        public @NonNull Builder setSource(@Source int source) {
            mBuilderCompat.setSource(source);
            return this;
        }

        /**
         * Sets flags that control content insertion behavior.
         *
         * @param flags Optional flags to configure the insertion behavior. Use 0 for default
         *              behavior. See {@code FLAG_} constants.
         * @return this builder
         */
        public @NonNull Builder setFlags(@Flags int flags) {
            mBuilderCompat.setFlags(flags);
            return this;
        }

        /**
         * Sets the http/https URI for the content. See
         * {@link android.view.inputmethod.InputContentInfo#getLinkUri} for more info.
         *
         * @param linkUri Optional http/https URI for the content.
         * @return this builder
         */
        public @NonNull Builder setLinkUri(@Nullable Uri linkUri) {
            mBuilderCompat.setLinkUri(linkUri);
            return this;
        }

        /**
         * Sets additional metadata.
         *
         * @param extras Optional bundle with additional metadata.
         * @return this builder
         */
        public @NonNull Builder setExtras(@Nullable Bundle extras) {
            mBuilderCompat.setExtras(extras);
            return this;
        }

        /**
         * @return A new {@link ContentInfoCompat} instance with the data from this builder.
         */
        public @NonNull ContentInfoCompat build() {
            return mBuilderCompat.build();
        }
    }

    private interface BuilderCompat {
        void setClip(@NonNull ClipData clip);
        void setSource(@Source int source);
        void setFlags(@Flags int flags);
        void setLinkUri(@Nullable Uri linkUri);
        void setExtras(@Nullable Bundle extras);
        @NonNull ContentInfoCompat build();
    }

    private static final class BuilderCompatImpl implements BuilderCompat {
        @NonNull ClipData mClip;
        @Source
        int mSource;
        @Flags
        int mFlags;
        @Nullable Uri mLinkUri;
        @Nullable Bundle mExtras;

        BuilderCompatImpl(@NonNull ClipData clip, int source) {
            mClip = clip;
            mSource = source;
        }

        BuilderCompatImpl(@NonNull ContentInfoCompat other) {
            mClip = other.getClip();
            mSource = other.getSource();
            mFlags = other.getFlags();
            mLinkUri = other.getLinkUri();
            mExtras = other.getExtras();
        }

        @Override
        public void setClip(@NonNull ClipData clip) {
            mClip = clip;
        }

        @Override
        public void setSource(@Source int source) {
            mSource = source;
        }

        @Override
        public void setFlags(@Flags int flags) {
            mFlags = flags;
        }

        @Override
        public void setLinkUri(@Nullable Uri linkUri) {
            mLinkUri = linkUri;
        }

        @Override
        public void setExtras(@Nullable Bundle extras) {
            mExtras = extras;
        }

        @Override
        public @NonNull ContentInfoCompat build() {
            return new ContentInfoCompat(new CompatImpl(this));
        }
    }

    @RequiresApi(31)
    private static final class BuilderCompat31Impl implements BuilderCompat {
        private final ContentInfo.@NonNull Builder mPlatformBuilder;

        BuilderCompat31Impl(@NonNull ClipData clip, int source) {
            mPlatformBuilder = new ContentInfo.Builder(clip, source);
        }

        BuilderCompat31Impl(@NonNull ContentInfoCompat other) {
            mPlatformBuilder = new ContentInfo.Builder(other.toContentInfo());
        }

        @Override
        public void setClip(@NonNull ClipData clip) {
            mPlatformBuilder.setClip(clip);
        }

        @Override
        public void setSource(@Source int source) {
            mPlatformBuilder.setSource(source);
        }

        @Override
        public void setFlags(@Flags int flags) {
            mPlatformBuilder.setFlags(flags);
        }

        @Override
        public void setLinkUri(@Nullable Uri linkUri) {
            mPlatformBuilder.setLinkUri(linkUri);
        }

        @Override
        public void setExtras(@Nullable Bundle extras) {
            mPlatformBuilder.setExtras(extras);
        }

        @Override
        public @NonNull ContentInfoCompat build() {
            return new ContentInfoCompat(new Compat31Impl(mPlatformBuilder.build()));
        }
    }
}
