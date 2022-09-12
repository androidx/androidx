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

import static androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.processing.Packet;
import androidx.camera.core.processing.Processor;

/**
 * Converts a {@link ImageProxy} to JPEG bytes.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class Image2JpegBytes implements Processor<Packet<ImageProxy>, Packet<byte[]>> {

    @NonNull
    @Override
    public Packet<byte[]> process(@NonNull Packet<ImageProxy> inputPacket)
            throws ImageCaptureException {
        // TODO(b/240998060): For YUV input image is YUV, convert it to JPEG bytes.
        byte[] jpegBytes = jpegImageToJpegByteArray(inputPacket.getData());
        inputPacket.getData().close();
        return Packet.of(
                jpegBytes,
                inputPacket.getExif(),
                inputPacket.getFormat(),
                inputPacket.getSize(),
                inputPacket.getCropRect(),
                inputPacket.getRotationDegrees(),
                inputPacket.getSensorToBufferTransform());
    }
}
