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

package android.arch.background.workmanager.model;

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
    public void empty() throws IOException, ClassNotFoundException {
        Arguments args = new Arguments.Builder().build();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(notNullValue()));
        assertThat(restoredArgs.size(), is(0));
    }

    @Test
    public void serializeString() throws IOException, ClassNotFoundException {
        String expectedValue1 = "value1";
        String expectedValue2 = "value2";
        Arguments args = new Arguments.Builder()
                .putString(KEY1, expectedValue1)
                .putString(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(notNullValue()));
        assertThat(restoredArgs.size(), is(2));

        String actualValue1 = restoredArgs.getString(KEY1, null);
        assertThat(actualValue1, is(notNullValue()));
        assertThat(actualValue1, is(expectedValue1));

        String actualValue2 = restoredArgs.getString(KEY2, null);
        assertThat(actualValue2, is(notNullValue()));
        assertThat(actualValue2, is(expectedValue2));
    }

    @Test
    public void serializeIntArray() throws IOException, ClassNotFoundException {
        int[] expectedValue1 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] expectedValue2 = new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        Arguments args = new Arguments.Builder()
                .putIntArray(KEY1, expectedValue1)
                .putIntArray(KEY2, expectedValue2)
                .build();

        byte[] byteArray = Arguments.toByteArray(args);
        Arguments restoredArgs = Arguments.fromByteArray(byteArray);

        assertThat(restoredArgs, is(notNullValue()));
        assertThat(restoredArgs.size(), is(2));

        int[] actualValue1 = restoredArgs.getIntArray(KEY1);
        assertThat(actualValue1, is(equalTo(expectedValue1)));

        int[] actualValue2 = restoredArgs.getIntArray(KEY2);
        assertThat(actualValue2, is(equalTo(expectedValue2)));
    }
}
