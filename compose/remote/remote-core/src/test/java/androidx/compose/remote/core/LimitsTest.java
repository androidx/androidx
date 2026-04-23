/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import androidx.compose.remote.core.operations.BitmapData;

import org.junit.Test;

import java.util.ArrayList;

public class LimitsTest {

    @Test
    public void testMaxOpCount() {
        int original = Limits.MAX_OP_COUNT;
        try {
            Limits.MAX_OP_COUNT = 100;
            assertEquals(100, Limits.MAX_OP_COUNT);
        } finally {
            Limits.MAX_OP_COUNT = original;
        }
    }

    @Test
    public void testMaxImageDimension() {
        int original = Limits.MAX_IMAGE_DIMENSION;
        try {
            Limits.MAX_IMAGE_DIMENSION = 100;
            assertEquals(100, Limits.MAX_IMAGE_DIMENSION);

            // Test that BitmapData.read respects the limit
            WireBuffer buffer = new WireBuffer();
            buffer.start(Operations.DATA_BITMAP);
            buffer.writeInt(1); // imageId
            buffer.writeInt(101); // width > limit
            buffer.writeInt(50); // height
            buffer.writeBuffer(new byte[10]);
            buffer.setIndex(0);
            buffer.readByte(); // consume OP_CODE

            assertThrows(
                    RuntimeException.class,
                    () -> {
                        BitmapData.read(buffer, new ArrayList<>());
                    });

            // Test within limit
            WireBuffer buffer2 = new WireBuffer();
            buffer2.start(Operations.DATA_BITMAP);
            buffer2.writeInt(2); // imageId
            buffer2.writeInt(100); // width == limit
            buffer2.writeInt(50); // height
            buffer2.writeBuffer(new byte[10]);
            buffer2.setIndex(0);
            buffer2.readByte(); // consume OP_CODE

            BitmapData.read(buffer2, new ArrayList<>()); // Should not throw
        } finally {
            Limits.MAX_IMAGE_DIMENSION = original;
        }
    }
}
