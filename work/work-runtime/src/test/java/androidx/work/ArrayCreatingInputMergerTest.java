/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class ArrayCreatingInputMergerTest {

    private static final String KEY = "key";

    private static final int[] VALUE_INT_ARRAY = { 0, 1, 2 };
    private static final int VALUE_INT = 3;
    private static final Long VALUE_LONG = Long.MAX_VALUE;

    ArrayCreatingInputMerger mArrayCreatingInputMerger;
    Data mDataWithIntArray;
    Data mDataWithInt;
    Data mDataWithLong;

    @Before
    public void setUp() {
        mArrayCreatingInputMerger = new ArrayCreatingInputMerger();
        mDataWithIntArray = new Data.Builder().putIntArray(KEY, VALUE_INT_ARRAY).build();
        mDataWithInt = new Data.Builder().putInt(KEY, VALUE_INT).build();
        mDataWithLong = new Data.Builder().putLong(KEY, VALUE_LONG).build();
    }

    @Test
    public void testMerge_singleArgument() {
        Data output = getOutputFor(mDataWithInt);
        assertThat(output.size(), is(1));
        int[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(1));
        assertThat(outputArray[0], is(VALUE_INT));
    }

    @Test
    public void testMerge_concatenatesNonArrays() {
        Data output = getOutputFor(mDataWithInt, mDataWithInt);
        assertThat(output.size(), is(1));
        int[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(2));
        assertThat(outputArray[0], is(VALUE_INT));
        assertThat(outputArray[1], is(VALUE_INT));
    }

    @Test
    public void testMerge_concatenatesArrays() {
        Data output = getOutputFor(mDataWithIntArray, mDataWithIntArray);
        assertThat(output.size(), is(1));
        int[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(VALUE_INT_ARRAY.length * 2));
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < VALUE_INT_ARRAY.length; ++j) {
                assertThat(outputArray[i * VALUE_INT_ARRAY.length + j], is(VALUE_INT_ARRAY[j]));
            }
        }
    }

    @Test
    public void testMerge_concatenatesArrayAndPrimitive() {
        Data output = getOutputFor(mDataWithIntArray, mDataWithInt);
        assertThat(output.size(), is(1));
        int[] outputArray = output.getIntArray(KEY);
        assertThat(outputArray.length, is(VALUE_INT_ARRAY.length + 1));
        for (int i = 0; i < VALUE_INT_ARRAY.length; ++i) {
            assertThat(outputArray[i], is(VALUE_INT_ARRAY[i]));
        }
        assertThat(outputArray[VALUE_INT_ARRAY.length], is(VALUE_INT));
    }

    @Test
    public void testMerge_throwsIllegalArgumentExceptionOnDifferentTypes() {
        Throwable throwable = null;
        try {
            Data output = getOutputFor(mDataWithInt, mDataWithLong);
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalArgumentException.class));
    }

    private Data getOutputFor(Data... inputs) {
        return mArrayCreatingInputMerger.merge(Arrays.asList(inputs));
    }
}
