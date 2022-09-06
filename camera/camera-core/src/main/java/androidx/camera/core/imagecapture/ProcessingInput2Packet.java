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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.Exif;
import androidx.camera.core.processing.Packet;
import androidx.camera.core.processing.Processor;

import java.io.IOException;

/**
 * Converts {@link ProcessingNode} input to a {@link Packet}.
 *
 * <p>This is we fix the metadata of the image, such as rotation and crop rect.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class ProcessingInput2Packet implements
        Processor<ProcessingNode.InputPacket, Packet<ImageProxy>> {

    @NonNull
    @Override
    public Packet<ImageProxy> process(@NonNull ProcessingNode.InputPacket inputPacket)
            throws ImageCaptureException {
        ImageProxy image = inputPacket.getImageProxy();
        ProcessingRequest request = inputPacket.getProcessingRequest();

        Exif exif;
        try {
            exif = Exif.createFromImageProxy(image);
            // Rewind the buffer after reading.
            image.getPlanes()[0].getBuffer().rewind();
        } catch (IOException e) {
            throw new ImageCaptureException(ERROR_FILE_IO, "Failed to extract EXIF data.", e);
        }

        // TODO(b/242536202): update size, crop, rotation and transform based on
        //  EXIF_ROTATION_AVAILABILITY

        return Packet.of(
                image,
                exif,
                request.getCropRect(),
                request.getRotationDegrees(),
                request.getSensorToBufferTransform());
    }
}
