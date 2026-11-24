/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.compose.remote.core.operations.BitmapData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RemoteBitmapDecoderTest {

    private final BitmapLoader mEmptyLoader = (url) -> null;

    @Test
    public void testDecodeInline_raw8888_highBitSetBytes() {
        // 4 pixels in 4x1 bitmap testing high-bit-set (negative) bytes in each channel:
        // Pixel 0: 0x00800000 (A=0x00, R=0x80, G=0x00, B=0x00) -> R must not sign-extend into A
        // Pixel 1: 0xFF008000 (A=0xFF, R=0x00, G=0x80, B=0x00) -> G must not sign-extend into R
        // Pixel 2: 0xFF000080 (A=0xFF, R=0x00, G=0x00, B=0x80) -> B must not sign-extend into R,G
        // Pixel 3: 0x80FFFFFF (A=0x80, R=0xFF, G=0xFF, B=0xFF) -> R,G,B must not sign-extend into A
        byte[] rawData =
                new byte[] {
                    0x00,
                    (byte) 0x80,
                    0x00,
                    0x00,
                    (byte) 0xFF,
                    0x00,
                    (byte) 0x80,
                    0x00,
                    (byte) 0xFF,
                    0x00,
                    0x00,
                    (byte) 0x80,
                    (byte) 0x80,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                };

        Bitmap bitmap =
                RemoteBitmapDecoder.decodeBitmap(
                        1,
                        BitmapData.ENCODING_INLINE,
                        BitmapData.TYPE_RAW8888,
                        4,
                        1,
                        rawData,
                        mEmptyLoader);
        assertNotNull(bitmap);
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)));
        assertEquals(0xFF008000, bitmap.getPixel(1, 0));
        assertEquals(0xFF000080, bitmap.getPixel(2, 0));
        assertEquals(0x80FFFFFF, bitmap.getPixel(3, 0));
    }
}
