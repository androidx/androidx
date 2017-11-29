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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

@SmallTest
public class OverwritingInputMergerTest {

    private OverwritingInputMerger mOverwritingInputMerger;

    @Before
    public void setUp() {
        mOverwritingInputMerger = new OverwritingInputMerger();
    }

    @Test
    public void testMerge_singleArgument() {
        String key = "key";
        String value = "value";

        Arguments arguments = new Arguments.Builder().putString(key, value).build();
        Arguments output = getOutputFor(arguments);

        assertThat(output.size(), is(1));
        assertThat(output.getString(key, null), is(value));
    }

    @Test
    public void testMerge_multipleArguments() {
        String key1 = "key1";
        String value1 = "value1";
        String value1a = "value1a";
        String key2 = "key2";
        String value2 = "value2";
        String key3 = "key3";
        String value3 = "value3";

        Arguments arguments1 = new Arguments.Builder()
                .putString(key1, value1)
                .putString(key2, value2)
                .build();
        Arguments arguments2 = new Arguments.Builder()
                .putString(key1, value1a)
                .putString(key3, value3)
                .build();

        Arguments output = getOutputFor(arguments1, arguments2);

        assertThat(output.size(), is(3));
        assertThat(output.getString(key1, null), is(value1a));
        assertThat(output.getString(key2, null), is(value2));
        assertThat(output.getString(key3, null), is(value3));
    }

    private Arguments getOutputFor(Arguments... arguments) {
        return mOverwritingInputMerger.merge(Arrays.asList(arguments));
    }
}
