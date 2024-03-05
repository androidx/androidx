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

package androidx.pdf.aidl;


import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class MatchRectsTest {

    private MatchRects mMatchRects = createMatchRects(5, 0, 2, 3);

    @Test
    public void testGetRectsForMatch() {
        assertThat(mMatchRects.size()).isEqualTo(3);

        assertThat(mMatchRects.get(0).size()).isEqualTo(2);
        assertThat(mMatchRects.get(1).size()).isEqualTo(1);
        assertThat(mMatchRects.get(2).size()).isEqualTo(2);

        assertThat(mMatchRects.get(0)).isEqualTo(mMatchRects.flatten().subList(0, 2));
        assertThat(mMatchRects.get(1)).isEqualTo(mMatchRects.flatten().subList(2, 3));
        assertThat(mMatchRects.get(2)).isEqualTo(mMatchRects.flatten().subList(3, 5));
    }

    @Test
    public void testFlatten() {
        List<Rect> rects = mMatchRects.flatten();
        assertThat(rects.size()).isEqualTo(5);

        List<Rect> rectsExcludingMatchOne = Arrays.asList(
                rects.get(0), rects.get(1), rects.get(3), rects.get(4));
        assertThat(mMatchRects.flattenExcludingMatch(1)).isEqualTo(rectsExcludingMatchOne);
    }

    private static MatchRects createMatchRects(int numRects, Integer... matchToRect) {
        List<Rect> rects = new ArrayList<>();
        List<Integer> charIndexes = new ArrayList<>();
        for (int i = 0; i < numRects; i++) {
            rects.add(new Rect(i * 100, i * 100, i * 101, i * 101));
            charIndexes.add(i * 10);
        }

        return new MatchRects(rects, Arrays.asList(matchToRect), charIndexes);
    }
}
