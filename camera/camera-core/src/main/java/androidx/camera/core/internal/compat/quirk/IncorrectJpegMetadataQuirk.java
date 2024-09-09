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

package androidx.camera.core.internal.compat.quirk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.Quirk;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>QuirkSummary
 *     Bug Id: 309005680, 356428987
 *     Description: Quirk required to check whether the captured JPEG image has incorrect metadata.
 *                  For example, Samsung A24 device has the problem and result in the captured
 *                  image can't be parsed and saved successfully. Samsung S10e and S10+ devices are
 *                  also reported to have the similar issue.
 *     Device(s): Samsung Galaxy A24, S10e, S10+ device.
 */
public final class IncorrectJpegMetadataQuirk implements Quirk {

    private static final Set<String> SAMSUNG_DEVICES = new HashSet<>(Arrays.asList(
            "A24", // Samsung Galaxy A24 series devices
            "BEYOND0", // Samsung Galaxy S10e series devices
            "BEYOND2" // Samsung Galaxy S10+ series devices
    ));

    static boolean load() {
        return isSamsungProblematicDevice();
    }

    private static boolean isSamsungProblematicDevice() {
        return "Samsung".equalsIgnoreCase(Build.BRAND) && SAMSUNG_DEVICES.contains(
                Build.DEVICE.toUpperCase(Locale.US));
    }

    /**
     * Converts the image proxy to the byte array with correct JPEG metadata.
     *
     * <p>Some unexpected data exists in the head of the problematic JPEG images captured from
     * the Samsung A24 device. Removing those data can fix the JPEG images.
     */
    @NonNull
    public byte[] jpegImageToJpegByteArray(@NonNull ImageProxy imageProxy) {
        ByteBuffer byteBuffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.rewind();
        byteBuffer.get(bytes);

        int copyStartPos = 0;

        // Applies the solution only when the original JPEG data can't be correctly parsed to
        // find the SOS marker position.
        if (!canParseSosMarker(bytes)) {
            int secondFfd8Position = findSecondFfd8Position(bytes);
            if (secondFfd8Position != -1) {
                copyStartPos = secondFfd8Position;
            } else {
                return bytes;
            }
        }

        return Arrays.copyOfRange(bytes, copyStartPos, byteBuffer.limit());
    }

    /**
     * Returns whether the JFIF SOS marker can be correctly parsed from the input JPEG byte data.
     */
    private boolean canParseSosMarker(@NonNull byte[] bytes) {
        // Parses the JFIF segments from the start of the JPEG image data
        int markPosition = 0x2;
        while (true) {
            // Breaks the while-loop and return false if the mark byte can't be correctly found.
            if (markPosition + 4 > bytes.length || bytes[markPosition] != ((byte) 0xff)) {
                return false;
            }
            // Breaks the while-loop when finding the SOS (FF DA) mark
            if (bytes[markPosition] == ((byte) 0xff) && bytes[markPosition + 1] == ((byte) 0xda)) {
                return true;
            }
            int segmentLength =
                    ((bytes[markPosition + 2] & 0xff) << 8) | (bytes[markPosition + 3] & 0xff);
            markPosition += segmentLength + 2;
        }
    }

    /**
     * Returns the second FFD8 position.
     *
     * @param bytes the JPEG byte array data.
     * @return the second FFD8 position if it can be found. Otherwise, returns -1.
     */
    private int findSecondFfd8Position(@NonNull byte[] bytes) {
        // Starts from the position 2 to skip the first FFD8
        int position = 2;

        while (true) {
            if (position + 1 > bytes.length) {
                break;
            }
            // Find and return the second FFD8 position
            if (bytes[position] == ((byte) 0xff)
                    && bytes[position + 1] == ((byte) 0xd8)) {
                return position;
            }
            position++;
        }

        return -1;
    }
}
