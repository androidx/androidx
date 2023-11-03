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

package androidx.javascriptengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.javascriptengine.common.Utils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class UtilsTest {

    //These are prefix bytes of valid UTF8.
    static final byte[] TRUNCATED_TWO_BYTES_UTF8 = new byte[]{(byte) 0xc3};
    static final byte[] TRUNCATED_THREE_BYTES_UTF8 = new byte[]{(byte) 0xe0, (byte) 0xa0};
    static final byte[] TRUNCATED_FOUR_BYTES_UTF8 =
            new byte[]{(byte) 0xf0, (byte) 0x92, (byte) 0x80};

    static final byte[] VALID_UTF8 = "valid byte".getBytes(StandardCharsets.UTF_8);
    static final byte[] VALID_FOUR_BYTE_UTF8 =
            new byte[]{(byte) 0xf0, (byte) 0x92, (byte) 0x80, (byte) 0x80};

    @Test
    public void testIsValidUTF8FirstByte() {
        assertTrue(Utils.isUTF8ContinuationByte(TRUNCATED_THREE_BYTES_UTF8[1]));
        assertTrue(Utils.isUTF8ContinuationByte(TRUNCATED_FOUR_BYTES_UTF8[1]));
        assertTrue(Utils.isUTF8ContinuationByte(TRUNCATED_FOUR_BYTES_UTF8[2]));

        assertFalse(Utils.isUTF8ContinuationByte("0".getBytes(StandardCharsets.UTF_8)[0]));
        assertFalse(Utils.isUTF8ContinuationByte((byte) 0xff));
        assertFalse(Utils.isUTF8ContinuationByte(VALID_UTF8[0]));
        assertFalse(Utils.isUTF8ContinuationByte(VALID_FOUR_BYTE_UTF8[0]));
        assertFalse(Utils.isUTF8ContinuationByte(TRUNCATED_TWO_BYTES_UTF8[0]));
        assertFalse(Utils.isUTF8ContinuationByte(TRUNCATED_THREE_BYTES_UTF8[0]));
        assertFalse(Utils.isUTF8ContinuationByte(TRUNCATED_FOUR_BYTES_UTF8[0]));

    }

    @Test
    public void testGetValidUTF8PrefixLength() {
        byte[] validPrefixBytes = "valid byte".getBytes(StandardCharsets.UTF_8);
        assertEquals(-1, Utils.getLastUTF8StartingByteIndex("".getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, Utils.getLastUTF8StartingByteIndex("a".getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, Utils.getLastUTF8StartingByteIndex("ab".getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, Utils.getLastUTF8StartingByteIndex("abc".getBytes(StandardCharsets.UTF_8)));
        assertEquals(3,
                Utils.getLastUTF8StartingByteIndex("abcd".getBytes(StandardCharsets.UTF_8)));
        assertEquals(validPrefixBytes.length - 1,
                Utils.getLastUTF8StartingByteIndex(validPrefixBytes));
        assertEquals(validPrefixBytes.length,
                Utils.getLastUTF8StartingByteIndex(
                        concat(validPrefixBytes, TRUNCATED_TWO_BYTES_UTF8)));
        assertEquals(validPrefixBytes.length, Utils.getLastUTF8StartingByteIndex(
                concat(validPrefixBytes, TRUNCATED_THREE_BYTES_UTF8)));
        assertEquals(validPrefixBytes.length, Utils.getLastUTF8StartingByteIndex(
                concat(validPrefixBytes, TRUNCATED_FOUR_BYTES_UTF8)));
        assertEquals(validPrefixBytes.length,
                Utils.getLastUTF8StartingByteIndex(concat(validPrefixBytes, VALID_FOUR_BYTE_UTF8)));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        int lenA = a.length;
        int lenB = b.length;
        byte[] c = Arrays.copyOf(a, lenA + lenB);
        System.arraycopy(b, 0, c, lenA, lenB);
        return c;
    }
}
