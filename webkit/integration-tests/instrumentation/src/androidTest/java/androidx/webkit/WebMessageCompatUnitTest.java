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

package androidx.webkit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;

/**
 * Test {@link WebMessageCompat} basic usages.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class WebMessageCompatUnitTest {

    @Test
    public void testArrayBufferUsage() {
        final byte[] bytes = {1, 2, 3};
        final WebMessageCompat message = new WebMessageCompat(bytes);
        Assert.assertEquals(WebMessageCompat.TYPE_ARRAY_BUFFER, message.getType());
        Assert.assertArrayEquals(bytes, message.getArrayBuffer());
        Assert.assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                message.getData();
            }
        });
    }

    @Test
    public void testStringUsage() {
        final String string = "Hello";
        final WebMessageCompat message = new WebMessageCompat(string);
        Assert.assertEquals(WebMessageCompat.TYPE_STRING, message.getType());
        Assert.assertEquals(string, message.getData());
        Assert.assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                message.getArrayBuffer();
            }
        });
    }
}
