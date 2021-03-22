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

import static androidx.camera.video.OutputOptions.Type.MEDIA_STORE;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

/**
 * A class provides a option for storing output to MediaStore.
 *
 * <p> The result could be saved to a shared storage. The results will remain on the device after
 * the app is uninstalled.
 */
@AutoValue
public abstract class MediaStoreOutputOptions extends OutputOptions {

    MediaStoreOutputOptions() {
        super(MEDIA_STORE);
    }

    /** Returns a builder for this MediaStoreOutputOptions. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_MediaStoreOutputOptions.Builder();
    }

    /**
     * Gets the ContentResolver instance in order to convert Uri to a file path.
     */
    @NonNull
    public abstract ContentResolver getContentResolver();

    /**
     * Gets the limit for the file length in bytes.
     */
    @Override
    public abstract int getFileSizeLimit();

    /** Gets the Uri instance */
    @NonNull
    public abstract Uri getUri();

    /** The builder of the {@link MediaStoreOutputOptions}. */
    @AutoValue.Builder
    public abstract static class Builder {
        Builder() {
        }

        /** Sets the ContentResolver instance. */
        @NonNull
        public abstract Builder setContentResolver(@NonNull ContentResolver contentResolver);

        /** Defines how to store the result. */
        @NonNull
        public abstract Builder setUri(@NonNull Uri uri);

        /** Sets the limit for the file length in bytes. */
        @NonNull
        public abstract Builder setFileSizeLimit(int bytes);

        /** Builds the MediaStoreOutputOptions instance. */
        @NonNull
        public abstract MediaStoreOutputOptions build();
    }
}
