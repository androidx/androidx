/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.internal.compat.quirk.IncorrectJpegMetadataQuirk;

import java.nio.ByteBuffer;

/**
 * Workaround to correct the JPEG metadata for specific problematic devices.
 *
 * @see IncorrectJpegMetadataQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class JpegMetadataCorrector {
    private final IncorrectJpegMetadataQuirk mQuirk;

    /**
     * Constructor
     */
    public JpegMetadataCorrector(@NonNull Quirks quirks) {
        mQuirk = quirks.get(IncorrectJpegMetadataQuirk.class);
    }

    /**
     * Returns whether JPEG file metadata needs to be corrected or not.
     */
    public boolean needCorrectJpegMetadata() {
        return mQuirk != null;
    }

    /**
     * Converts the image proxy to the byte array with correct JPEG metadata.
     */
    @NonNull
    public byte[] jpegImageToJpegByteArray(@NonNull ImageProxy image) {
        if (mQuirk == null) {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] data = new byte[buffer.capacity()];
            buffer.rewind();
            buffer.get(data);
            return data;
        } else {
            return mQuirk.jpegImageToJpegByteArray(image);
        }
    }
}
