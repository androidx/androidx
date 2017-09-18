/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;

import java.io.IOException;

@SmallTest
public class ArgumentsTest {
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    @Test
    public void empty() throws IOException, ClassNotFoundException {
        Arguments args = new Arguments();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertNotNull(restoredArgs);
        assertEquals(0, restoredArgs.size());
    }

    @Test
    public void serializeString() throws IOException, ClassNotFoundException {
        Arguments args = new Arguments();
        String expectedValue1 = "value1";
        String expectedValue2 = "value2";
        args.putString(KEY1, expectedValue1);
        args.putString(KEY2, expectedValue2);

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertNotNull(restoredArgs);
        assertEquals(2, restoredArgs.size());

        String actualValue1 = restoredArgs.getString(KEY1, null);
        assertNotNull(actualValue1);
        assertEquals(expectedValue1, actualValue1);

        String actualValue2 = restoredArgs.getString(KEY2, null);
        assertNotNull(actualValue2);
        assertEquals(expectedValue2, actualValue2);
    }

    @Test
    public void serializeIntArray() throws IOException, ClassNotFoundException {
        Arguments args = new Arguments();
        int[] expectedValue1 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] expectedValue2 = new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        args.putIntArray(KEY1, expectedValue1);
        args.putIntArray(KEY2, expectedValue2);

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertNotNull(restoredArgs);
        assertEquals(2, restoredArgs.size());

        int[] actualValue1 = restoredArgs.getIntArray(KEY1);
        assertArrayEquals(expectedValue1, actualValue1);

        int[] actualValue2 = restoredArgs.getIntArray(KEY2);
        assertArrayEquals(expectedValue2, actualValue2);
    }
}
