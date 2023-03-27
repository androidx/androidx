/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * A class providing options for storing output to MediaStore.
 *
 * <p>Example:
 *
 * <pre>{@code
 *
 * ContentValues contentValues = new ContentValues();
 * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_VIDEO");
 * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
 *
 * MediaStoreOutputOptions options =
 *         new MediaStoreOutputOptions.Builder(
 *             contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
 *         .setContentValues(contentValues)
 *         .build();
 *
 * }</pre>
 *
 * <p>The output {@link Uri} can be obtained via {@link OutputResults#getOutputUri()} from
 * {@link VideoRecordEvent.Finalize#getOutputResults()}.
 *
 * <p>For more information about setting collections {@link Uri} and {@link ContentValues}, read
 * the <a href="https://developer.android.com/training/data-storage/shared/media">
 *     Access media files from shared storage</a> and
 * <a href="https://developer.android.com/reference/android/provider/MediaStore">MediaStore</a>
 * developer guide.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class MediaStoreOutputOptions extends OutputOptions {

    /**
     * An empty {@link ContentValues}.
     */
    @NonNull
    public static final ContentValues EMPTY_CONTENT_VALUES = new ContentValues();

    private final MediaStoreOutputOptionsInternal mMediaStoreOutputOptionsInternal;

    MediaStoreOutputOptions(
            @NonNull MediaStoreOutputOptionsInternal mediaStoreOutputOptionsInternal) {
        super(mediaStoreOutputOptionsInternal);
        mMediaStoreOutputOptionsInternal = mediaStoreOutputOptionsInternal;
    }

    /**
     * Gets the ContentResolver instance.
     *
     * @see Builder#Builder(ContentResolver, Uri)
     */
    @NonNull
    public ContentResolver getContentResolver() {
        return mMediaStoreOutputOptionsInternal.getContentResolver();
    }

    /**
     * Gets the URI of the collection to insert into.
     *
     * @see Builder#Builder(ContentResolver, Uri)
     */
    @NonNull
    public Uri getCollectionUri() {
        return mMediaStoreOutputOptionsInternal.getCollectionUri();
    }

    /**
     * Gets the content values to be included in the created video row.
     *
     * @see Builder#setContentValues(ContentValues)
     */
    @NonNull
    public ContentValues getContentValues() {
        return mMediaStoreOutputOptionsInternal.getContentValues();
    }

    @Override
    @NonNull
    public String toString() {
        // Don't use Class.getSimpleName(), class name will be changed by proguard obfuscation.
        return mMediaStoreOutputOptionsInternal.toString().replaceFirst(
                "MediaStoreOutputOptionsInternal", "MediaStoreOutputOptions");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaStoreOutputOptions)) {
            return false;
        }
        return mMediaStoreOutputOptionsInternal.equals(
                ((MediaStoreOutputOptions) o).mMediaStoreOutputOptionsInternal);
    }

    @Override
    public int hashCode() {
        return mMediaStoreOutputOptionsInternal.hashCode();
    }

    /** The builder of the {@link MediaStoreOutputOptions} object. */
    public static final class Builder extends
            OutputOptions.Builder<MediaStoreOutputOptions, Builder> {

        private final MediaStoreOutputOptionsInternal.Builder mInternalBuilder;

        /**
         * Creates a builder of the {@link MediaStoreOutputOptions} with media store options.
         *
         * <p>The ContentResolver can be obtained by app {@link Context#getContentResolver()
         * context} and is used to access to MediaStore.
         *
         * <p>{@link MediaStore} class provides APIs to obtain the collection URI. A collection
         * URI corresponds to a storage volume on the device shared storage. A common collection
         * URI used to access the primary external storage is
         * {@link MediaStore.Video.Media#EXTERNAL_CONTENT_URI}.
         * {@link MediaStore.Video.Media#getContentUri} can also be used to query different
         * storage volumes. For more information, read
         * <a href="https://developer.android.com/training/data-storage/shared/media">
         *     Access media files from shared storage</a> developer guide.
         *
         * <p>When recording a video, a corresponding video row will be created in the input
         * collection, and the content values set by {@link #setContentValues} will also be
         * written to this row.
         *
         * @param contentResolver the ContentResolver instance.
         * @param collectionUri the URI of the collection to insert into.
         */
        public Builder(@NonNull ContentResolver contentResolver, @NonNull Uri collectionUri) {
            super(new AutoValue_MediaStoreOutputOptions_MediaStoreOutputOptionsInternal.Builder());
            Preconditions.checkNotNull(contentResolver, "Content resolver can't be null.");
            Preconditions.checkNotNull(collectionUri, "Collection Uri can't be null.");
            mInternalBuilder = (MediaStoreOutputOptionsInternal.Builder) mRootInternalBuilder;
            mInternalBuilder.setContentResolver(contentResolver)
                    .setCollectionUri(collectionUri)
                    .setContentValues(EMPTY_CONTENT_VALUES);
        }

        /**
         * Sets the content values to be included in the created video row.
         *
         * <p>The content values is a set of key/value paris used to store the metadata of a
         * video item. The keys are defined in {@link MediaStore.MediaColumns} and
         * {@link MediaStore.Video.VideoColumns}.
         * When recording a video, a corresponding video row will be created in the input
         * collection, and this content values will also be written to this row. If a key is not
         * defined in the MediaStore, the corresponding value will be ignored.
         *
         * <p>If not set, defaults to {@link #EMPTY_CONTENT_VALUES}.
         *
         * @param contentValues the content values to be inserted.
         */
        @NonNull
        public Builder setContentValues(@NonNull ContentValues contentValues) {
            Preconditions.checkNotNull(contentValues, "Content values can't be null.");
            mInternalBuilder.setContentValues(contentValues);
            return this;
        }

        /** Builds the {@link MediaStoreOutputOptions} instance. */
        @Override
        @NonNull
        public MediaStoreOutputOptions build() {
            return new MediaStoreOutputOptions(mInternalBuilder.build());
        }
    }

    @AutoValue
    abstract static class MediaStoreOutputOptionsInternal extends OutputOptionsInternal {
        @NonNull
        abstract ContentResolver getContentResolver();
        @NonNull
        abstract Uri getCollectionUri();
        @NonNull
        abstract ContentValues getContentValues();

        @SuppressWarnings("NullableProblems") // Nullable problem in AutoValue generated class
        @AutoValue.Builder
        abstract static class Builder extends OutputOptionsInternal.Builder<Builder> {
            @NonNull
            abstract Builder setContentResolver(@NonNull ContentResolver contentResolver);
            @NonNull
            abstract Builder setCollectionUri(@NonNull Uri collectionUri);
            @NonNull
            abstract Builder setContentValues(@NonNull ContentValues contentValues);
            @Override
            @NonNull
            abstract MediaStoreOutputOptionsInternal build();
        }
    }
}
