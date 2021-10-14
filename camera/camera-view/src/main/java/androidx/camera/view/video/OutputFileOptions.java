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

package androidx.camera.view.video;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.VideoCapture;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.io.File;

/**
 * Options for saving newly captured video.
 *
 * <p> this class is used to configure save location and metadata. Save location can be
 * either a {@link File}, {@link MediaStore}. The metadata will be
 * stored with the saved video.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@ExperimentalVideo
@AutoValue
public abstract class OutputFileOptions {

    // Empty metadata object used as a placeholder for no user-supplied metadata.
    // Should be initialized to all default values.
    private static final Metadata EMPTY_METADATA = Metadata.builder().build();

    // Restrict constructor to same package
    OutputFileOptions() {
    }

    /**
     * Creates options to write captured video to a {@link File}.
     *
     * @param file save location of the video.
     */
    @NonNull
    public static Builder builder(@NonNull File file) {
        return new AutoValue_OutputFileOptions.Builder().setMetadata(EMPTY_METADATA).setFile(file);
    }

    /**
     * Creates options to write captured video to a {@link ParcelFileDescriptor}.
     *
     * <p>Using a ParcelFileDescriptor to record a video is only supported for Android 8.0 or
     * above.
     *
     * @param fileDescriptor to save the video.
     * @throws IllegalArgumentException when the device is not running Android 8.0 or above.
     */
    @NonNull
    public static Builder builder(@NonNull ParcelFileDescriptor fileDescriptor) {
        Preconditions.checkArgument(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
                "Using a ParcelFileDescriptor to record a video is only supported for Android 8"
                        + ".0 or above.");

        return new AutoValue_OutputFileOptions.Builder().setMetadata(
                EMPTY_METADATA).setFileDescriptor(fileDescriptor);
    }

    /**
     * Creates options to write captured video to {@link MediaStore}.
     *
     * Example:
     *
     * <pre>{@code
     *
     * ContentValues contentValues = new ContentValues();
     * contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_VIDEO");
     * contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
     *
     * OutputFileOptions options = OutputFileOptions.builder(
     *         getContentResolver(),
     *         MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
     *         contentValues).build();
     *
     * }</pre>
     *
     * @param contentResolver to access {@link MediaStore}
     * @param saveCollection  The URI of the table to insert into.
     * @param contentValues   to be included in the created video file.
     */
    @NonNull
    public static Builder builder(@NonNull ContentResolver contentResolver,
            @NonNull Uri saveCollection,
            @NonNull ContentValues contentValues) {
        return new AutoValue_OutputFileOptions.Builder()
                .setMetadata(EMPTY_METADATA)
                .setContentResolver(contentResolver)
                .setSaveCollection(saveCollection).setContentValues(contentValues);
    }

    /**
     * Returns the File object which is set by the {@link OutputFileOptions.Builder}.
     */
    @Nullable
    abstract File getFile();

    /**
     * Returns the ParcelFileDescriptor object which is set by the
     * {@link OutputFileOptions.Builder}.
     */
    @Nullable
    abstract ParcelFileDescriptor getFileDescriptor();

    /**
     * Returns the content resolver which is set by the {@link OutputFileOptions.Builder}.
     */
    @Nullable
    abstract ContentResolver getContentResolver();

    /**
     * Returns the URI which is set by the {@link OutputFileOptions.Builder}.
     */
    @Nullable
    abstract Uri getSaveCollection();

    /**
     * Returns the content values which is set by the {@link OutputFileOptions.Builder}.
     */
    @Nullable
    abstract ContentValues getContentValues();

    /** Returns the metadata which is set by the {@link OutputFileOptions.Builder}. */
    @NonNull
    public abstract Metadata getMetadata();

    /**
     * Checking the caller wants to save video to MediaStore.
     */
    private boolean isSavingToMediaStore() {
        return getSaveCollection() != null && getContentResolver() != null
                && getContentValues() != null;
    }

    /**
     * Checking the caller wants to save video to a File.
     */
    private boolean isSavingToFile() {
        return getFile() != null;
    }

    /**
     * Checking the caller wants to save video to a ParcelFileDescriptor.
     */
    private boolean isSavingToFileDescriptor() {
        return getFileDescriptor() != null;
    }

    /**
     * Converts to a {@link VideoCapture.OutputFileOptions}.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public VideoCapture.OutputFileOptions toVideoCaptureOutputFileOptions() {
        VideoCapture.OutputFileOptions.Builder internalOutputFileOptionsBuilder;
        if (isSavingToFile()) {
            internalOutputFileOptionsBuilder =
                    new VideoCapture.OutputFileOptions.Builder(
                            Preconditions.checkNotNull(getFile()));
        } else if (isSavingToFileDescriptor()) {
            internalOutputFileOptionsBuilder =
                    new VideoCapture.OutputFileOptions.Builder(
                            Preconditions.checkNotNull(getFileDescriptor()).getFileDescriptor());
        } else {
            Preconditions.checkState(isSavingToMediaStore());
            internalOutputFileOptionsBuilder =
                    new VideoCapture.OutputFileOptions.Builder(
                            Preconditions.checkNotNull(getContentResolver()),
                            Preconditions.checkNotNull(getSaveCollection()),
                            Preconditions.checkNotNull(getContentValues()));
        }

        VideoCapture.Metadata internalMetadata = new VideoCapture.Metadata();
        internalMetadata.location = getMetadata().getLocation();
        internalOutputFileOptionsBuilder.setMetadata(internalMetadata);

        return internalOutputFileOptionsBuilder.build();
    }

    /**
     * Builder class for {@link OutputFileOptions}.
     */
    @AutoValue.Builder
    @SuppressWarnings("StaticFinalBuilder")
    public abstract static class Builder {

        // Restrict construction to same package
        Builder() {
        }

        abstract Builder setFile(@Nullable File file);

        abstract Builder setFileDescriptor(@Nullable ParcelFileDescriptor fileDescriptor);

        abstract Builder setContentResolver(@Nullable ContentResolver contentResolver);

        abstract Builder setSaveCollection(@Nullable Uri uri);

        abstract Builder setContentValues(@Nullable ContentValues contentValues);

        /**
         * Sets the metadata to be stored with the saved video.
         *
         * @param metadata Metadata to be stored with the saved video.
         */
        @NonNull
        public abstract Builder setMetadata(@NonNull Metadata metadata);

        /**
         * Builds {@link OutputFileOptions}.
         */
        @NonNull
        public abstract OutputFileOptions build();
    }
}
