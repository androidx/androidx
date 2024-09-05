/*
 * Copyright 2024 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.processing.Operation;

import com.google.auto.value.AutoValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DngImage2Disk implements Operation<DngImage2Disk.In, ImageCapture.OutputFileResults> {

    @NonNull
    private DngCreator mDngCreator;

    public DngImage2Disk(@NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull CaptureResult captureResult) {
        this(new DngCreator(cameraCharacteristics, captureResult));
    }

    @VisibleForTesting
    DngImage2Disk(@NonNull DngCreator dngCreator) {
        mDngCreator = dngCreator;
    }

    @NonNull
    @Override
    public ImageCapture.OutputFileResults apply(@NonNull In in) throws ImageCaptureException {
        ImageCapture.OutputFileOptions options = in.getOutputFileOptions();
        File tempFile = createTempFile(options);
        writeImageToFile(tempFile, in.getImageProxy(), in.getRotationDegrees());
        Uri uri = moveFileToTarget(tempFile, options);
        return new ImageCapture.OutputFileResults(uri);
    }

    /**
     * Writes byte array to the given {@link File}.
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void writeImageToFile(
            @NonNull File tempFile,
            @NonNull ImageProxy imageProxy,
            int rotationDegrees) throws ImageCaptureException {
        try (FileOutputStream output = new FileOutputStream(tempFile)) {
            mDngCreator.setOrientation(computeExifOrientation(rotationDegrees));
            mDngCreator.writeImage(output, imageProxy.getImage());
        } catch (IllegalArgumentException e) {
            throw new ImageCaptureException(ERROR_FILE_IO,
                    "Image with an unsupported format was used", e);
        } catch (IllegalStateException e) {
            throw new ImageCaptureException(ERROR_FILE_IO,
                    "Not enough metadata information has been "
                            + "set to write a well-formatted DNG file", e);
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to write to temp file", e);
        } finally {
            imageProxy.close();
        }
    }

    static int computeExifOrientation(int rotationDegrees) {
        switch (rotationDegrees) {
            case 0:
                return ExifInterface.ORIENTATION_NORMAL;
            case 90:
                return ExifInterface.ORIENTATION_ROTATE_90;
            case 180:
                return ExifInterface.ORIENTATION_ROTATE_180;
            case 270:
                return ExifInterface.ORIENTATION_ROTATE_270;
        }
        return ExifInterface.ORIENTATION_UNDEFINED;
    }

    /**
     * Input packet.
     */
    @AutoValue
    abstract static class In {

        @NonNull
        abstract ImageProxy getImageProxy();

        abstract int getRotationDegrees();

        @NonNull
        abstract ImageCapture.OutputFileOptions getOutputFileOptions();

        @NonNull
        static DngImage2Disk.In of(
                @NonNull ImageProxy imageProxy,
                int rotationDegrees,
                @NonNull ImageCapture.OutputFileOptions outputFileOptions) {
            return new AutoValue_DngImage2Disk_In(imageProxy,
                    rotationDegrees, outputFileOptions);
        }
    }
}
