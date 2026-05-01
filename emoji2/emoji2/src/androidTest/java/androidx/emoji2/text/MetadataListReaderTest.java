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
package androidx.emoji2.text;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class MetadataListReaderTest {

    @Test
    public void testReadByteBuffer_withNegativeSkip_throwsException() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        // sfnt version
        buffer.putInt(0);
        // tableCount
        buffer.putShort((short) 1);
        // padding (3 * 2 = 6 bytes)
        buffer.position(buffer.position() + 6);
        // tag = 'meta'
        buffer.putInt('m' << 24 | 'e' << 16 | 't' << 8 | 'a');
        // checksum
        buffer.putInt(0);
        // offset = 0 (this will cause negative skip because current position is around 24)
        buffer.putInt(0);
        // length
        buffer.putInt(0);
        buffer.position(0);

        assertThrows(IOException.class, () -> {
            MetadataListReader.read(buffer);
        });
    }

    @Test
    public void testReadInputStream_withNegativeSkip_throwsException() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putInt(0);
        buffer.putShort((short) 1);
        buffer.position(buffer.position() + 6);
        buffer.putInt('m' << 24 | 'e' << 16 | 't' << 8 | 'a');
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);

        ByteArrayInputStream is = new ByteArrayInputStream(buffer.array());

        assertThrows(IOException.class, () -> {
            MetadataListReader.read(is);
        });
    }

    @Test
    public void testReadByteBuffer_withOutOfBoundsStartOffset_throwsIllegalArgumentException() {
        ByteBuffer buffer = ByteBuffer.allocate(100);
        buffer.putInt(0);
        buffer.putShort((short) 1);
        buffer.position(buffer.position() + 6);
        buffer.putInt('m' << 24 | 'e' << 16 | 't' << 8 | 'a');
        buffer.putInt(0);
        // offset to meta table (valid positive skip)
        buffer.putInt(30);
        buffer.putInt(0);

        // pad to position 30
        buffer.position(30);
        // minorVersion
        buffer.putShort((short) 0);
        // majorVersion
        buffer.putShort((short) 0);
        // flags
        buffer.putInt(0);
        // reserved
        buffer.putInt(0);
        // mapsCount = 1
        buffer.putInt(1);
        // tag = 'Emji'
        buffer.putInt('E' << 24 | 'm' << 16 | 'j' << 8 | 'i');
        // dataOffset = 200 (out of bounds)
        buffer.putInt(200);
        // dataLength
        buffer.putInt(0);

        buffer.position(0);

        assertThrows(IllegalArgumentException.class, () -> {
            MetadataListReader.read(buffer);
        });
    }
}
