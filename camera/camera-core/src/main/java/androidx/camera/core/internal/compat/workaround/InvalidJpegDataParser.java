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
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.internal.compat.quirk.DeviceQuirks;
import androidx.camera.core.internal.compat.quirk.LargeJpegImageQuirk;

/**
 * Workaround to check whether the captured JPEG image contains redundant 0's padding data.
 *
 * @see LargeJpegImageQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class InvalidJpegDataParser {
    private final boolean mHasQuirk = DeviceQuirks.get(LargeJpegImageQuirk.class) != null;

    /**
     * Returns the valid data length of the input JPEG byte data array which is determined by the
     * JFIF EOI byte.
     *
     * <p>Returns the original byte array length when quirk doesn't exist or EOI can't be found.
     */
    public int getValidDataLength(@NonNull byte[] bytes) {
        if (!mHasQuirk) {
            return bytes.length;
        }

        int jfifEoiMarkEndPosition = getJfifEoiMarkEndPosition(bytes);

        return jfifEoiMarkEndPosition != -1 ? jfifEoiMarkEndPosition : bytes.length;
    }

    /**
     * Returns the end position of JFIF EOI mark. Returns -1 while JFIF EOI mark can't be found
     * in the provided byte array.
     */
    @VisibleForTesting
    public static int getJfifEoiMarkEndPosition(@NonNull byte[] bytes) {
        // Parses the JFIF segments from the start of the JPEG image data
        int markPosition = 0x2;
        while (true) {
            // Breaks the while-loop and return null if the mark byte can't be correctly found.
            if (markPosition + 4 > bytes.length || bytes[markPosition] != ((byte) 0xff)) {
                return -1;
            }

            int segmentLength =
                    ((bytes[markPosition + 2] & 0xff) << 8) | (bytes[markPosition + 3] & 0xff);

            // Breaks the while-loop when finding the SOS (FF DA) mark
            if (bytes[markPosition] == ((byte) 0xff) && bytes[markPosition + 1] == ((byte) 0xda)) {
                break;
            }
            markPosition += segmentLength + 2;
        }

        // Finds the EOI (FF D9) mark to know the end position of the valid compressed image data
        int eoiPosition = markPosition + 2;

        while (true) {
            // Breaks the while-loop and return null if EOI mark can't be found
            if (eoiPosition + 2 > bytes.length) {
                return -1;
            }

            if (bytes[eoiPosition] == ((byte) 0xff) && bytes[eoiPosition + 1] == ((byte) 0xd9)) {
                break;
            }
            eoiPosition++;
        }

        return eoiPosition + 2;
    }
}
