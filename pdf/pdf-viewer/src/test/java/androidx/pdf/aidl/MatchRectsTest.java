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

import static org.junit.Assert.assertTrue;

import android.graphics.Rect;

import androidx.pdf.models.MatchRects;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
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
    public void testGetCharIndex_returnsIndexCorrespondingToMatch() {
        assertThat(mMatchRects.getCharIndex(0)).isEqualTo(0);
        assertThat(mMatchRects.getCharIndex(1)).isEqualTo(10);
        assertThat(mMatchRects.getCharIndex(2)).isEqualTo(20);
    }

    @Test
    public void testGetMatchNearestCharIndex_returnsMatchIndexCorrespondingToCharIndex() {
        assertThat(mMatchRects.getMatchNearestCharIndex(0)).isEqualTo(0);
        assertThat(mMatchRects.getMatchNearestCharIndex(10)).isEqualTo(1);
        assertThat(mMatchRects.getMatchNearestCharIndex(20)).isEqualTo(2);
    }

    @Test
    public void testGetFirstRect_returnsFirstRectForMatch() {
        assertThat(mMatchRects.getFirstRect(0)).isEqualTo(new Rect(0, 0, 0, 0));
        assertThat(mMatchRects.getFirstRect(1)).isEqualTo(new Rect(200, 200, 202, 202));
        assertThat(mMatchRects.getFirstRect(2)).isEqualTo(new Rect(300, 300, 303, 303));
    }

    @Test
    public void testFlatten() {
        List<Rect> rects = mMatchRects.flatten();
        assertThat(rects.size()).isEqualTo(5);

        List<Rect> rectsExcludingMatchOne = Arrays.asList(
                rects.get(0), rects.get(1), rects.get(3), rects.get(4));
        assertThat(mMatchRects.flattenExcludingMatch(1)).isEqualTo(rectsExcludingMatchOne);
    }

    @Test
    public void testRectsExcludingMatchOne_returnsFlatListOfRectsForAllMatchesExceptGivenMatch() {
        List<Rect> rects = mMatchRects.flatten();
        List<Rect> rectsExcludingMatchOne = Arrays.asList(rects.get(0), rects.get(1), rects.get(3),
                rects.get(4));
        assertThat(mMatchRects.flattenExcludingMatch(1)).isEqualTo(rectsExcludingMatchOne);
    }

    @Test
    public void testClassFields_flagsFieldModification() {
        List<String> fields = new ArrayList<>();
        fields.add("NO_MATCHES");
        fields.add("CREATOR");
        fields.add("mRects");
        fields.add("mMatchToRect");
        fields.add("mCharIndexes");

        List<String> declaredFields = new ArrayList<>();
        for (Field field : MatchRects.class.getDeclaredFields()) {
            declaredFields.add(field.getName());
        }

        assertTrue(fields.containsAll(declaredFields));
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
