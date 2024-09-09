/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.core.imagecapture;

import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.imagecapture.FileUtil.createTempFile;
import static androidx.camera.core.imagecapture.FileUtil.moveFileToTarget;
import static androidx.camera.core.imagecapture.FileUtil.updateFileExif;

import static java.util.Objects.requireNonNull;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.internal.compat.workaround.InvalidJpegDataParser;
import androidx.camera.core.processing.Operation;
import androidx.camera.core.processing.Packet;

import com.google.auto.value.AutoValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Saves JPEG bytes to disk.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class JpegBytes2Disk implements
        Operation<JpegBytes2Disk.In, ImageCapture.OutputFileResults> {

    @NonNull
    @Override
    public ImageCapture.OutputFileResults apply(@NonNull In in) throws ImageCaptureException {
        Packet<byte[]> packet = in.getPacket();
        ImageCapture.OutputFileOptions options = in.getOutputFileOptions();
        File tempFile = createTempFile(options);
        writeBytesToFile(tempFile, packet.getData());
        updateFileExif(tempFile, requireNonNull(packet.getExif()), options,
                packet.getRotationDegrees());
        Uri uri = moveFileToTarget(tempFile, options);
        return new ImageCapture.OutputFileResults(uri);
    }

    /**
     * Writes byte array to the given {@link File}.
     */
    static void writeBytesToFile(
            @NonNull File tempFile, @NonNull byte[] bytes) throws ImageCaptureException {
        try (FileOutputStream output = new FileOutputStream(tempFile)) {
            InvalidJpegDataParser invalidJpegDataParser = new InvalidJpegDataParser();
            output.write(bytes, 0, invalidJpegDataParser.getValidDataLength(bytes));
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to write to temp file", e);
        }
    }

    /**
     * Input packet.
     */
    @AutoValue
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract static class In {

        @NonNull
        abstract Packet<byte[]> getPacket();

        @NonNull
        abstract ImageCapture.OutputFileOptions getOutputFileOptions();

        @NonNull
        public static In of(@NonNull Packet<byte[]> jpegBytes,
                @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
            return new AutoValue_JpegBytes2Disk_In(jpegBytes, outputFileOptions);
        }
    }
}
