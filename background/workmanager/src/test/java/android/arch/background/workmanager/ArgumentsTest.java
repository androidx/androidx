/*
 * Copyright 2017 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;

import java.io.IOException;

@SmallTest
public class ArgumentsTest {
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";

    @Test
    public void testSize_noArguments() {
        Arguments args = new Arguments.Builder().build();
        assertThat(args.size(), is(0));
    }

    @Test
    public void testSize_hasArguments() {
        Arguments args = new Arguments.Builder().putBoolean(KEY1, true).build();
        assertThat(args.size(), is(1));
    }

    @Test
    public void testSerializeEmpty() throws IOException, ClassNotFoundException {
        Arguments args = Arguments.EMPTY;

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(args));
    }

    @Test
    public void testSerializeString() throws IOException, ClassNotFoundException {
        String expectedValue1 = "value1";
        String expectedValue2 = "value2";
        Arguments args = new Arguments.Builder()
                .putString(KEY1, expectedValue1)
                .putString(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(args));
    }

    @Test
    public void testSerializeIntArray() throws IOException, ClassNotFoundException {
        Integer[] expectedValue1 = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Integer[] expectedValue2 = new Integer[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        Arguments args = new Arguments.Builder()
                .putIntArray(KEY1, expectedValue1)
                .putIntArray(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(notNullValue()));
        assertThat(restoredArgs.size(), is(2));
        assertThat(restoredArgs.getIntArray(KEY1), is(equalTo(expectedValue1)));
        assertThat(restoredArgs.getIntArray(KEY2), is(equalTo(expectedValue2)));
    }
}
