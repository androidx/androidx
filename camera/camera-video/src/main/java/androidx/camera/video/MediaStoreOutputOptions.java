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
 */
@AutoValue
public abstract class MediaStoreOutputOptions extends OutputOptions {

    MediaStoreOutputOptions() {
        super(OPTIONS_TYPE_MEDIA_STORE);
    }

    /**
     * Returns a builder for this MediaStoreOutputOptions.
     */
    @NonNull
    public static Builder builder() {
        return new AutoValue_MediaStoreOutputOptions.Builder()
                .setFileSizeLimit(FILE_SIZE_UNLIMITED);
    }

    /**
     * Gets the ContentResolver instance in order to convert Uri to a file path.
     */
    @NonNull
    public abstract ContentResolver getContentResolver();

    /**
     * Gets the URL of the table to insert into.
     */
    @NonNull
    public abstract Uri getCollection();

    /**
     * Gets the content values to be included in the created file.
     */
    @NonNull
    public abstract ContentValues getContentValues();

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public abstract long getFileSizeLimit();

    /** The builder of the {@link MediaStoreOutputOptions}. */
    @AutoValue.Builder
    @SuppressWarnings("StaticFinalBuilder")
    public abstract static class Builder {
        Builder() {
        }

        /** Sets the ContentResolver instance. */
        @NonNull
        public abstract Builder setContentResolver(@NonNull ContentResolver contentResolver);

        /** Sets the URL of the table to insert into. */
        @NonNull
        public abstract Builder setCollection(@NonNull Uri collectionUri);

        /** Sets the content values to be included in the created file. */
        @NonNull
        public abstract Builder setContentValues(@NonNull ContentValues contentValues);

        /**
         * Sets the limit for the file length in bytes. Zero or negative values are considered
         * unlimited.
         *
         * <p>If not set, defaults to {@link #FILE_SIZE_UNLIMITED}.
         */
        @NonNull
        public abstract Builder setFileSizeLimit(long bytes);

        /** Builds the MediaStoreOutputOptions instance. */
        @NonNull
        public abstract MediaStoreOutputOptions build();
    }
}
