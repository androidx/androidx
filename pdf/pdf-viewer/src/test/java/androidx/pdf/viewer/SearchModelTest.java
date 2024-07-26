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

import static androidx.pdf.util.CycleRange.Direction.FORWARDS;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;

import androidx.pdf.find.MatchCount;
import androidx.pdf.models.MatchRects;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("deprecation")
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class SearchModelTest {

    @Mock
    private PdfLoader mPdfLoader;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSearch() {
        SearchModel searchModel = new SearchModel(mPdfLoader);
        searchModel.setNumPages(3);

        searchModel.setQuery("query", /*viewingPage=*/ 1);
        assertThat(searchModel.selectedMatch().get()).isNull();
        assertThat(searchModel.matchCount().get()).isNull();

        searchModel.updateMatches("query", 1, createMatchRects(2));
        assertThat(searchModel.selectedMatch().get())
                .isEqualTo(new SelectedMatch("query", 1, createMatchRects(2), 0));
        assertThat(searchModel.matchCount().get()).isEqualTo(new MatchCount(0, 2, false));

        searchModel.updateMatches("query", 2, createMatchRects(3));
        assertThat(searchModel.matchCount().get()).isEqualTo(new MatchCount(0, 5, false));

        searchModel.updateMatches("query", 0, createMatchRects(2));
        assertThat(searchModel.matchCount().get()).isEqualTo(new MatchCount(2, 7, true));

        searchModel.selectNextMatch(FORWARDS, 1);
        assertThat(searchModel.selectedMatch().get())
                .isEqualTo(new SelectedMatch("query", 1, createMatchRects(2), 1));
        assertThat(searchModel.matchCount().get()).isEqualTo(new MatchCount(3, 7, true));
    }

    private static MatchRects createMatchRects(int numRects) {
        List<Rect> rects = new ArrayList<Rect>();
        List<Integer> matchToRect = new ArrayList<Integer>();
        List<Integer> charIndexes = new ArrayList<Integer>();
        for (int i = 0; i < numRects; i++) {
            rects.add(new Rect(i * 100, i * 100, i * 101, i * 101));
            matchToRect.add(i);
            charIndexes.add(i * 10);
        }

        return new MatchRects(rects, matchToRect, charIndexes);
    }
}
