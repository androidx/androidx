/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.fail;

import androidx.pdf.util.ObservableArray.ArrayObserver;
import androidx.pdf.util.Observables.ExposedArray;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class ExposedArrayTest {

    private ExposedArray<String> mArray;

    @Before
    public void setUp() {
        mArray = Observables.newExposedArray();
    }

    @Test
    public void testIteration() {
        mArray.set(9, "September");
        mArray.set(10, "October");
        mArray.set(11, "November");
        mArray.set(12, "December");

        int count = 0;
        for (int i : observable().keys()) {
            assertThat(observable().get(i)).isNotNull();
            count++;
        }
        assertWithMessage("Wrong number of items in iteration, count = " + count)
                .that(count)
                .isEqualTo(4);
    }

    @Test
    public void testIterationWithNulls() {
        mArray.set(9, "September");
        mArray.set(10, null);
        mArray.set(11, "November");
        mArray.set(12, "December");

        int count = 0;
        boolean gotNull = false;
        for (int i : observable().keys()) {
            if (observable().get(i) == null) {
                gotNull = true;
            }
            count++;
        }
        assertWithMessage("Wrong number of items in iteration, count = " + count)
                .that(count)
                .isEqualTo(4);
        assertWithMessage("Didn't get back the null value").that(gotNull).isTrue();
    }

    @Test
    public void testObserveAdd() {
        final String[] values = new String[13];
        mArray.set(10, "October");
        observable().addObserver(new BaseObserver() {
            @Override
            public void onValueAdded(int index, String addedValue) {
                values[index] = addedValue;
            }
        });

        mArray.set(1, "January");
        mArray.set(6, "May");

        assertThat(values[1]).isEqualTo("January");
        assertThat(values[6]).isEqualTo("May");

        for (int i = 0; i < 13; i++) {
            if (i != 1 && i != 6) {
                assertThat(values[i]).isNull();
            }
        }
    }

    @Test
    public void testObserveRemove() {
        final String[] values = new String[13];
        mArray.set(1, "January");
        mArray.set(3, "Mars");

        observable().addObserver(new BaseObserver() {
            @Override
            public void onValueRemoved(int index, String removedValue) {
                values[index] = removedValue;
            }
        });

        mArray.remove(3);
        mArray.remove(6);

        assertThat(values[3]).isEqualTo("Mars");

        for (int i = 0; i < 13; i++) {
            if (i != 3) {
                assertThat(values[i]).isNull();
            }
        }
    }

    @Test
    public void testObserveReplace() {
        final String[] values = new String[13];
        mArray.set(1, "January");
        mArray.set(3, "Mars");
        observable().addObserver(new BaseObserver() {
            @Override
            public void onValueReplaced(int index, String previousValue, String newValue) {
                values[index] = newValue;
            }
        });

        mArray.set(3, "March");

        assertThat(values[3]).isEqualTo("March");

        for (int i = 0; i < 13; i++) {
            if (i != 3) {
                assertThat(values[i]).isNull();
            }
        }
    }

    private ObservableArray<String> observable() {
        return mArray;
    }

    private static class BaseObserver implements ArrayObserver<String> {
        @Override
        public void onValueReplaced(int index, String previousValue, String newValue) {
            fail(String.format("onValueReplaced at %d with %s, %s", index, previousValue,
                    newValue));
        }

        @Override
        public void onValueRemoved(int index, String removedValue) {
            fail(String.format("onValueRemoved at %d with %s", index, removedValue));
        }

        @Override
        public void onValueAdded(int index, String addedValue) {
            fail(String.format("onValueAdded at %d with %s", index, addedValue));
        }

    }
}
