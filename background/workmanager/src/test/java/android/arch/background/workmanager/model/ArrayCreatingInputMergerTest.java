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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

@SmallTest
public class ArrayCreatingInputMergerTest {

    private static final String KEY = "key";

    private static final Integer[] VALUE_INT_ARRAY = { 0, 1, 2 };
    private static final Integer VALUE_INT = 3;
    private static final Long VALUE_LONG = Long.MAX_VALUE;

    ArrayCreatingInputMerger mArrayCreatingInputMerger;
    Arguments mArgumentsWithIntArray;
    Arguments mArgumentsWithInt;
    Arguments mArgumentsWithLong;

    @Before
    public void setUp() {
        mArrayCreatingInputMerger = new ArrayCreatingInputMerger();
        mArgumentsWithIntArray = new Arguments.Builder().putIntArray(KEY, VALUE_INT_ARRAY).build();
        mArgumentsWithInt = new Arguments.Builder().putInt(KEY, VALUE_INT).build();
        mArgumentsWithLong = new Arguments.Builder().putLong(KEY, VALUE_LONG).build();
    }

    @Test
    public void testMerge_singleArgument() {
        Arguments output = getOutputFor(mArgumentsWithInt);
        assertThat(output.size(), is(1));
        Integer[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(1));
        assertThat(outputArray[0], is(VALUE_INT));
    }

    @Test
    public void testMerge_concatenatesNonArrays() {
        Arguments output = getOutputFor(mArgumentsWithInt, mArgumentsWithInt);
        assertThat(output.size(), is(1));
        Integer[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(2));
        assertThat(outputArray[0], is(VALUE_INT));
        assertThat(outputArray[1], is(VALUE_INT));
    }

    @Test
    public void testMerge_concatenatesArrays() {
        Arguments output = getOutputFor(mArgumentsWithIntArray, mArgumentsWithIntArray);
        assertThat(output.size(), is(1));
        Integer[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(VALUE_INT_ARRAY.length * 2));
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < VALUE_INT_ARRAY.length; ++j) {
                assertThat(outputArray[i * VALUE_INT_ARRAY.length + j], is(VALUE_INT_ARRAY[j]));
            }
        }
    }

    @Test
    public void testMerge_concatenatesArrayAndPrimitive() {
        Arguments output = getOutputFor(mArgumentsWithIntArray, mArgumentsWithInt);
        assertThat(output.size(), is(1));
        Integer[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(VALUE_INT_ARRAY.length + 1));
        for (int i = 0; i < VALUE_INT_ARRAY.length; ++i) {
            assertThat(outputArray[i], is(VALUE_INT_ARRAY[i]));
        }
        assertThat(outputArray[VALUE_INT_ARRAY.length], is(VALUE_INT));
    }

    @Test
    public void testMerge_throwsIllegalStateExceptionOnDifferentTypes() {
        Throwable throwable = null;
        try {
            Arguments output = getOutputFor(mArgumentsWithInt, mArgumentsWithLong);
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
    }

    private Arguments getOutputFor(Arguments... arguments) {
        return mArrayCreatingInputMerger.merge(Arrays.asList(arguments));
    }
}
