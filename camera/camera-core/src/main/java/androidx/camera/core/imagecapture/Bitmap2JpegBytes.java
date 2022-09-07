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

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.processing.Packet;
import androidx.camera.core.processing.Processor;

import com.google.auto.value.AutoValue;

import java.io.ByteArrayOutputStream;

/**
 * Compresses a {@link Bitmap} to JPEG bytes.
 *
 * <p>The {@link Bitmap} will be recycled and should not be used after the processing.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class Bitmap2JpegBytes implements Processor<Bitmap2JpegBytes.In, Packet<byte[]>> {

    @NonNull
    @Override
    public Packet<byte[]> process(@NonNull In in) throws ImageCaptureException {
        Packet<Bitmap> packet = in.getPacket();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        packet.getData().compress(Bitmap.CompressFormat.JPEG, in.getJpegQuality(), outputStream);
        packet.getData().recycle();
        return Packet.of(outputStream.toByteArray(),
                requireNonNull(packet.getExif()),
                ImageFormat.JPEG,
                packet.getSize(),
                packet.getCropRect(),
                packet.getRotationDegrees(),
                packet.getSensorToBufferTransform());
    }

    /**
     * Input of {@link Bitmap2JpegBytes} processor.
     */
    @AutoValue
    abstract static class In {

        abstract Packet<Bitmap> getPacket();

        abstract int getJpegQuality();

        @NonNull
        static In of(@NonNull Packet<Bitmap> imagePacket, int jpegQuality) {
            return new AutoValue_Bitmap2JpegBytes_In(imagePacket, jpegQuality);
        }
    }
}

