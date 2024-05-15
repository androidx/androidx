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

package androidx.pdf.data;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Unit tests for {@link Range}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class RangeTest {

    @Test
    public void testEmptyRange() {
        Range empty = new Range();
        Range alsoEmpty = new Range();
        assertThat(alsoEmpty).isEqualTo(empty);
        assertThat(empty.equals(alsoEmpty)).isTrue();
        assertThat(alsoEmpty.equals(empty)).isTrue();
        assertThat(alsoEmpty.hashCode()).isEqualTo(empty.hashCode());
        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.length()).isEqualTo(0);
        assertThat(empty.contains(alsoEmpty)).isTrue();
        assertThat(empty.contains(0)).isFalse();
        assertThat(empty.contains(-1)).isFalse();
    }

    @Test
    public void testRangeSingle() {
        Range range = new Range(5, 5);
        assertThat(range.isEmpty()).isFalse();
        assertThat(range.length()).isEqualTo(1);
        assertThat(range.contains(5)).isTrue();
        assertThat(range.contains(4)).isFalse();
        assertThat(range.contains(new Range())).isTrue();
    }

    @Test
    public void testRangeMany() {
        Range range = new Range(2, 5);
        assertThat(range.isEmpty()).isFalse();
        assertThat(range.length()).isEqualTo(4);
        assertThat(range.contains(2)).isTrue();
        assertThat(range.contains(3)).isTrue();
        assertThat(range.contains(4)).isTrue();
        assertThat(range.contains(5)).isTrue();
        assertThat(range.contains(0)).isFalse();
        assertThat(range.contains(1)).isFalse();
        assertThat(range.contains(6)).isFalse();
        assertThat(range.contains(new Range())).isTrue();
        assertThat(range.contains(range)).isTrue();
        assertThat(range.contains(new Range(2, 4))).isTrue();
    }

    @Test
    public void testContains() {
        Range range = new Range(-10, -2);
        assertThat(range.contains(-10)).isTrue();
        assertThat(range.contains(-2)).isTrue();
        assertThat(range.contains(-11)).isFalse();
        assertThat(range.contains(-1)).isFalse();
        assertThat(range.contains(0)).isFalse();

        assertThat(range.contains(new Range())).isTrue();
        assertThat(range.contains(new Range(-2, -2))).isTrue();
        assertThat(range.contains(new Range(-10, -2))).isTrue();
        assertThat(range.contains(new Range(-10, -9))).isTrue();
        assertThat(range.contains(new Range(-20, -9))).isFalse();
        assertThat(range.contains(new Range(-2, 0))).isFalse();
        assertThat(range.contains(new Range(0, 0))).isFalse();
    }

    @Test
    public void testUnionSingle() {
        Range range = new Range().union(5);
        assertThat(range.isEmpty()).isFalse();
        assertThat(range.length()).isEqualTo(1);
        assertThat(range.contains(5)).isTrue();
        assertThat(range.contains(4)).isFalse();
        assertThat(range.contains(new Range())).isTrue();
    }

    @Test
    public void testUnionMany() {
        Range range = new Range(2, 3).union(5);
        assertThat(range.isEmpty()).isFalse();
        assertThat(range.length()).isEqualTo(4);
        assertThat(range.contains(2)).isTrue();
        assertThat(range.contains(3)).isTrue();
        assertThat(range.contains(4)).isTrue();
        assertThat(range.contains(5)).isTrue();
        assertThat(range.contains(0)).isFalse();
        assertThat(range.contains(1)).isFalse();
        assertThat(range.contains(6)).isFalse();
        assertThat(range.contains(new Range())).isTrue();
        assertThat(range.contains(range)).isTrue();
        assertThat(range.contains(new Range(2, 4))).isTrue();
    }

    @Test
    public void testMinusNoChange() {
        Range range = new Range(2, 8);
        Range otherRange = new Range(9, 12);

        Range[] diffRanges = range.minus(otherRange);
        assertThat(diffRanges.length).isEqualTo(1);
        assertThat(diffRanges[0]).isEqualTo(range);

        diffRanges = otherRange.minus(range);
        assertThat(diffRanges.length).isEqualTo(1);
        assertThat(diffRanges[0]).isEqualTo(otherRange);
    }

    @Test
    public void testMinusEmpty() {
        Range range = new Range(2, 8);
        Range otherRange = new Range(2, 12);
        Range[] diffRanges = range.minus(otherRange);
        assertThat(diffRanges.length).isEqualTo(0);
    }

    @Test
    public void testMinusSplit() {
        Range range = new Range(2, 8);
        Range otherRange = new Range(3, 4);
        Range[] diffRanges = range.minus(otherRange);
        assertThat(diffRanges.length).isEqualTo(2);
        assertThat(diffRanges[0].length()).isEqualTo(1);
        assertThat(diffRanges[1].length()).isEqualTo(4);
        assertThat(diffRanges[0]).isEqualTo(new Range(2, 2));
        assertThat(diffRanges[1]).isEqualTo(new Range(5, 8));
    }

    @Test
    public void testMinusEatsLeft() {
        Range range = new Range(2, 8);
        Range otherRange = new Range(1, 7);
        Range[] diffRanges = range.minus(otherRange);
        assertThat(diffRanges.length).isEqualTo(1);
        assertThat(diffRanges[0].length()).isEqualTo(1);
        assertThat(diffRanges[0]).isEqualTo(new Range(8, 8));
    }

    @Test
    public void testMinusEatsRight() {
        Range range = new Range(2, 8);
        Range otherRange = new Range(8, 8);
        Range[] diffRanges = range.minus(otherRange);
        assertThat(diffRanges.length).isEqualTo(1);
        assertThat(diffRanges[0].length()).isEqualTo(6);
        assertThat(diffRanges[0]).isEqualTo(new Range(2, 7));
    }

    @Test
    public void testExpand() {
        Range range = new Range(5, 8);
        Range bounds = new Range(0, 300);
        Range exp = range.expand(1, bounds);
        assertThat(exp.getFirst()).isEqualTo(4);
        assertThat(exp.getLast()).isEqualTo(9);
    }

    @Test
    public void testExpandBounded() {
        Range range = new Range(2, 8);
        Range bounds = new Range(0, 300);
        Range exp = range.expand(4, bounds);
        assertThat(exp.getFirst()).isEqualTo(0);
        assertThat(exp.getLast()).isEqualTo(12);
    }

    @Test
    public void testIterateEmpty() {
        Range empty = new Range();
        assertThat(empty.iterator().hasNext()).isFalse();
    }

    @Test
    public void testIterate() {
        Range range = new Range(-1, 3);
        int[] expected = {-1, 0, 1, 2, 3};
        int count = 0;
        for (int i : range) {
            assertThat(i).isEqualTo(expected[count++]);
        }
    }
}
