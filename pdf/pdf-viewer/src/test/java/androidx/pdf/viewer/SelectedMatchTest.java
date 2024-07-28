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

package androidx.pdf.viewer;

import static androidx.pdf.util.CycleRange.Direction.BACKWARDS;
import static androidx.pdf.util.CycleRange.Direction.FORWARDS;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;

import androidx.pdf.models.MatchRects;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class SelectedMatchTest {

    @Test
    public void testSelectNextAndPrevious() {
        MatchRects matchRects = createMatchRects(5, 0, 2, 3);
        assertThat(matchRects.size()).isEqualTo(3);

        SelectedMatch selectedMatch = new SelectedMatch("query", 1, matchRects, 0);
        assertThat(selectedMatch.getSelected()).isEqualTo(0);

        selectedMatch = selectedMatch.selectNextMatchOnPage(FORWARDS);
        assertThat(selectedMatch).isNotNull();
        selectedMatch = selectedMatch.selectNextMatchOnPage(FORWARDS);
        assertThat(selectedMatch).isNotNull();
        assertThat(selectedMatch.getSelected()).isEqualTo(2);

        assertThat(selectedMatch.selectNextMatchOnPage(FORWARDS)).isNull();

        selectedMatch = selectedMatch.selectNextMatchOnPage(BACKWARDS);
        assertThat(selectedMatch).isNotNull();
        selectedMatch = selectedMatch.selectNextMatchOnPage(BACKWARDS);
        assertThat(selectedMatch).isNotNull();
        assertThat(selectedMatch.getSelected()).isEqualTo(0);

        assertThat(selectedMatch.selectNextMatchOnPage(BACKWARDS)).isNull();
    }

    private static MatchRects createMatchRects(int numRects, Integer... matchToRect) {
        List<Rect> rects = new ArrayList<Rect>();
        List<Integer> charIndexes = new ArrayList<Integer>();
        for (int i = 0; i < numRects; i++) {
            rects.add(new Rect(i * 100, i * 100, i * 101, i * 101));
            charIndexes.add(i * 10);
        }

        return new MatchRects(rects, Arrays.<Integer>asList(matchToRect), charIndexes);
    }
}
