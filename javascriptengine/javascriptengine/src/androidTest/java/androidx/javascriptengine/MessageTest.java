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

package androidx.javascriptengine;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Base64;

/**
 * Tests for the {@link Message} class.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MessageTest {
    private static final String TEST_STRING = "message";
    private static final byte[] TEST_BYTE_ARRAY = new byte[]{0, 1, 2, 3, 4};

    @Test
    public void testCreateMessage_nullString_throws() throws Throwable {
        Assert.assertThrows(
                NullPointerException.class,
                () -> Message.createStringMessage(null));
    }

    @Test
    public void testCreateMessage_nullArrayBuffer_throws() throws Throwable {
        Assert.assertThrows(
                NullPointerException.class,
                () -> Message.createArrayBufferMessage(null));
    }

    @Test
    public void testGetString_fromArrayBuffer_MessageTypeMismatchAccess() throws Throwable {
        Message message = Message.createArrayBufferMessage(TEST_BYTE_ARRAY);
        Assert.assertThrows(
                MessageTypeMismatchException.class,
                message::getString);
    }

    @Test
    public void testGetString_fromString_matches() throws Throwable {
        Message message = Message.createStringMessage(TEST_STRING);
        Assert.assertEquals(Message.TYPE_STRING, message.getType());
        Assert.assertEquals(TEST_STRING, message.getString());
    }

    @Test
    public void testGetArrayBuffer_fromString_MessageTypeMismatchAccess() throws Throwable {
        Message message = Message.createStringMessage(TEST_STRING);
        Assert.assertThrows(
                MessageTypeMismatchException.class,
                message::getArrayBuffer);
    }

    @Test
    public void testGetArrayBuffer_fromArrayBuffer_matches() throws Throwable {
        Message message = Message.createArrayBufferMessage(TEST_BYTE_ARRAY);
        Assert.assertEquals(Message.TYPE_ARRAY_BUFFER, message.getType());
        Assert.assertEquals(TEST_BYTE_ARRAY, message.getArrayBuffer());
    }

    @Test
    public void testToString_stringMessage_expectedOutput() throws Throwable {
        Message message = Message.createStringMessage("abcd\r\n0123");
        String string = message.toString();
        Assert.assertTrue("toString did not match expectation. Was: `" + string + "`",
                string.matches("androidx.javascriptengine.Message@\\p{XDigit}{1,8}\\("
                        + "string="
                        + "abcd\r\n0123"
                        + "\\)"));
    }

    @Test
    public void testToString_arrayBufferMessage_expectedOutput() throws Throwable {
        // Has to be long to ensure we cover whether there are line breaks in the encoding.
        byte[] largeBuffer = Base64.getDecoder().decode(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                            + "AA==");
        Message message = Message.createArrayBufferMessage(largeBuffer);
        String string = message.toString();
        Assert.assertTrue("toString did not match expectation. Was: `" + string + "`",
                string.matches(
                "androidx.javascriptengine.Message@\\p{XDigit}{1,8}\\("
                        + "arrayBuffer="
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\\+/"
                        + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\\+/"
                        + "AA=="
                        + "\\)"));
    }
}
