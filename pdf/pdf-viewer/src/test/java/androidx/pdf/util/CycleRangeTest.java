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

import static androidx.pdf.util.CycleRange.Direction.BACKWARDS;
import static androidx.pdf.util.CycleRange.Direction.FORWARDS;
import static androidx.pdf.util.CycleRange.Direction.OUTWARDS;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class CycleRangeTest {

    @Test
    public void testCycleRange() {
        assertThat(toList(CycleRange.of(0, 5, FORWARDS))).isEqualTo(toList(0, 1, 2, 3, 4));
        assertThat(toList(CycleRange.of(0, 5, BACKWARDS))).isEqualTo(toList(0, 4, 3, 2, 1));
        assertThat(toList(CycleRange.of(0, 5, OUTWARDS))).isEqualTo(toList(0, 1, 4, 2, 3));

        assertThat(toList(CycleRange.of(2, 5, FORWARDS))).isEqualTo(toList(2, 3, 4, 0, 1));
        assertThat(toList(CycleRange.of(2, 5, BACKWARDS))).isEqualTo(toList(2, 1, 0, 4, 3));
        assertThat(toList(CycleRange.of(2, 5, OUTWARDS))).isEqualTo(toList(2, 3, 1, 4, 0));

        assertThat(toList(CycleRange.of(-1, 5, BACKWARDS))).isEqualTo(toList(4, 3, 2, 1, 0));
        assertThat(toList(CycleRange.of(7, 5, FORWARDS))).isEqualTo(toList(2, 3, 4, 0, 1));
    }

    private List<Integer> toList(Integer... input) {
        return Arrays.asList(input);
    }

    private List<Integer> toList(Iterable<Integer> input) {
        List<Integer> result = new ArrayList<Integer>();
        for (Integer i : input) {
            result.add(i);
        }
        return result;
    }
}
