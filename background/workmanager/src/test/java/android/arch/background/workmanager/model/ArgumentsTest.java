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
        assertThat(restoredArgs.getIntArray(KEY1), is(equalTo(expectedValue1)));
        assertThat(restoredArgs.getIntArray(KEY2), is(equalTo(expectedValue2)));
    }

    @Test
    public void testMerge_basicMerge() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String key4 = "key4";

        Arguments destination = new Arguments.Builder()
                .putBoolean(key1, false)
                .putBoolean(key2, true)
                .build();
        Arguments source = new Arguments.Builder()
                .putBoolean(key3, true)
                .putBoolean(key4, false)
                .build();
        destination.merge(source);

        assertThat(destination.size(), is(4));
        assertThat(destination.getBoolean(key1, true), is(false));
        assertThat(destination.getBoolean(key2, false), is(true));
        assertThat(destination.getBoolean(key3, false), is(true));
        assertThat(destination.getBoolean(key4, true), is(false));
    }

    @Test
    public void testMerge_clobbersExistingKeys() {
        String key = "key";
        String originalValue = "original_value";
        String defaultValue = "no_value_found";
        String clobberedValue = "clobbered_value";

        Arguments destination = new Arguments.Builder().putString(key, originalValue).build();
        Arguments source = new Arguments.Builder().putString(key, clobberedValue).build();
        destination.merge(source);

        assertThat(destination.size(), is(1));
        assertThat(destination.getString(key, defaultValue), is(clobberedValue));
    }

    @Test
    public void testMerge_clobbersExistingKeyTypes() {
        String key = "key";
        String originalValue = "original_value";
        String defaultValue = "no_value_found";
        int clobberedValue = 7;

        Arguments destination = new Arguments.Builder().putString(key, originalValue).build();
        Arguments source = new Arguments.Builder().putInt(key, clobberedValue).build();
        destination.merge(source);

        assertThat(destination.size(), is(1));
        assertThat(destination.getString(key, defaultValue), is(defaultValue));
        assertThat(destination.getInt(key, 0), is(clobberedValue));
    }
}
