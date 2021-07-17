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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

/**
 * A class provides a option for storing output to MediaStore.
 *
 * <p> The result could be saved to a shared storage. The results will remain on the device after
 * the app is uninstalled.
 *
 * Example:
 *
 * <pre>{@code
 *
 * ContentValues contentValues = new ContentValues();
 * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_VIDEO");
 * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
 *
 * MediaStoreOutputOptions options = MediaStoreOutputOptions.builder()
 *         .setContentResolver(contentResolver)
 *         .setCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
 *         .setContentValues(contentValues)
 *         .build();
 *
 * }</pre>
 *
 * @hide
 */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public final class MediaStoreOutputOptions extends OutputOptions {

    /**
     * An empty {@link ContentValues}.
     */
    public static final ContentValues EMPTY_CONTENT_VALUES = new ContentValues();

    private final MediaStoreOutputOptionsInternal mMediaStoreOutputOptionsInternal;

    MediaStoreOutputOptions(
            @NonNull MediaStoreOutputOptionsInternal mediaStoreOutputOptionsInternal) {
        Preconditions.checkNotNull(mediaStoreOutputOptionsInternal,
                "MediaStoreOutputOptionsInternal can't be null.");
        mMediaStoreOutputOptionsInternal = mediaStoreOutputOptionsInternal;
    }

    /**
     * Gets the ContentResolver instance in order to convert URI to a file path.
     */
    @NonNull
    public ContentResolver getContentResolver() {
        return mMediaStoreOutputOptionsInternal.getContentResolver();
    }

    /**
     * Gets the URL of the table to insert into.
     */
    @NonNull
    public Uri getCollection() {
        return mMediaStoreOutputOptionsInternal.getCollection();
    }

    /**
     * Gets the content values to be included in the created file.
     */
    @NonNull
    public ContentValues getContentValues() {
        return mMediaStoreOutputOptionsInternal.getContentValues();
    }

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public long getFileSizeLimit() {
        return mMediaStoreOutputOptionsInternal.getFileSizeLimit();
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

    /** The builder of the {@link MediaStoreOutputOptions}. */
    public static final class Builder implements
            OutputOptions.Builder<MediaStoreOutputOptions, Builder> {
        private final MediaStoreOutputOptionsInternal.Builder mInternalBuilder =
                new AutoValue_MediaStoreOutputOptions_MediaStoreOutputOptionsInternal.Builder()
                        .setContentValues(EMPTY_CONTENT_VALUES)
                        .setFileSizeLimit(FILE_SIZE_UNLIMITED);

        /**
         * Creates a builder of the {@link MediaStoreOutputOptions} with media store options.
         *
         * @param contentResolver the content resolver instance.
         * @param collectionUri the URI of the table to insert into.
         */
        public Builder(@NonNull ContentResolver contentResolver, @NonNull Uri collectionUri) {
            Preconditions.checkNotNull(contentResolver, "Content resolver can't be null.");
            Preconditions.checkNotNull(collectionUri, "Collection Uri can't be null.");
            mInternalBuilder.setContentResolver(contentResolver).setCollection(collectionUri);
        }

        /**
         * Sets the content values to be included in the created file.
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

        /**
         * Sets the limit for the file length in bytes. Zero or negative values are considered
         * unlimited.
         *
         * <p>When used to
         * {@link Recorder#prepareRecording(android.content.Context, MediaStoreOutputOptions)
         * generate} recording, if the specified file size limit is reached while the recording
         * is being recorded, the recording will be finalized with
         * {@link VideoRecordEvent.Finalize#ERROR_FILE_SIZE_LIMIT_REACHED}.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @Override
        @NonNull
        public Builder setFileSizeLimit(long fileSizeLimitBytes) {
            mInternalBuilder.setFileSizeLimit(fileSizeLimitBytes);
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
    abstract static class MediaStoreOutputOptionsInternal {
        @NonNull
        abstract ContentResolver getContentResolver();
        @NonNull
        abstract Uri getCollection();
        @NonNull
        abstract ContentValues getContentValues();
        abstract long getFileSizeLimit();

        @AutoValue.Builder
        abstract static class Builder {
            @NonNull
            abstract Builder setContentResolver(@NonNull ContentResolver contentResolver);
            @NonNull
            abstract Builder setCollection(@NonNull Uri collectionUri);
            @NonNull
            abstract Builder setContentValues(@NonNull ContentValues contentValues);
            @NonNull
            abstract Builder setFileSizeLimit(long fileSizeLimitBytes);
            @NonNull
            abstract MediaStoreOutputOptionsInternal build();
        }
    }
}
