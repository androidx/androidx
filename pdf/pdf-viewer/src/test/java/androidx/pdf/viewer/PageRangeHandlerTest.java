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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.pdf.data.Range;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PageRangeHandlerTest {
    @Test
    public void getVisiblePage_nullVisiblePageRange_returnsZero() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        int expectedResult = 0;

        int result = adapter.getVisiblePage();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getVisiblePage_nonNullRange_returnsMidOfRange() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        Range dummyVisiblePageRange = new Range(1, 4);
        int expectedResult = 2;

        adapter.setVisiblePages(dummyVisiblePageRange);
        int result = adapter.getVisiblePage();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void adjustMaxPageToUpperVisibleRange_nullVisiblePageRange_noChangeInMaxPage() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        int expectedResult = -1;

        adapter.adjustMaxPageToUpperVisibleRange();
        int result = adapter.getMaxPage();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void adjustMaxPageToUpperVisibleRange_lowerUpperVisibleRange_noChangeInMaxPage() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        Range dummyVisiblePageRange = new Range(1, 4);
        int dummyMaxPage = 10;

        adapter.setVisiblePages(dummyVisiblePageRange);
        adapter.setMaxPage(dummyMaxPage);
        adapter.adjustMaxPageToUpperVisibleRange();
        int result = adapter.getMaxPage();

        assertThat(result).isEqualTo(dummyMaxPage);
    }

    @Test
    public void adjustMaxPageToUpperVisibleRange_higherUpperVisibleRange_updatedMaxPage() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        Range dummyVisiblePageRange = new Range(1, 10);
        int dummyMaxPage = 4;
        int expectedResult = 10;

        adapter.setVisiblePages(dummyVisiblePageRange);
        adapter.setMaxPage(dummyMaxPage);
        adapter.adjustMaxPageToUpperVisibleRange();
        int result = adapter.getMaxPage();

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void computeVisibleRange_zeroZoom_throwsIllegalArgumentException() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        int dummyScrollY = 10;
        float dummyZoom = 0;
        int dummyViewHeight = 100;

        assertThrows(IllegalArgumentException.class,
                () -> adapter.computeVisibleRange(dummyScrollY, dummyZoom, dummyViewHeight, true));
    }

    @Test
    public void computeVisibleRange_overlappingRange_returnsOverlappedRange() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        when(mockPaginationModel.getPagesInWindow(any(Range.class), any(Boolean.class))).thenAnswer(
                new Answer<Range>() {
                    private final Range mDefaultRange = new Range(3, 5);

                    @Override
                    public Range answer(InvocationOnMock invocation) throws Throwable {
                        Range intervalPx = invocation.getArgument(0);
                        boolean includePartial = invocation.getArgument(1);

                        // Condition has been added so that we can capture a change to the value
                        if (includePartial) {
                            return new Range(
                                    Math.min(intervalPx.getFirst(), mDefaultRange.getFirst()),
                                    Math.max(intervalPx.getLast(), mDefaultRange.getLast())
                            );
                        }
                        return mDefaultRange;

                    }
                });
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        int dummyScrollY = 2;
        float dummyZoom = 2;
        int dummyViewHeight = 10;
        Range expectedResult = new Range(1, 6);

        Range result = adapter.computeVisibleRange(dummyScrollY, dummyZoom, dummyViewHeight, true);

        assertEquals(result, expectedResult);
    }

    @Test
    public void refreshVisiblePageRange_overlappingRange_updatesVisibleRangeWithOverlappedRange() {
        PaginationModel mockPaginationModel = Mockito.mock(PaginationModel.class);
        when(mockPaginationModel.getPagesInWindow(any(Range.class), any(Boolean.class))).thenAnswer(
                new Answer<Range>() {
                    private final Range mDefaultRange = new Range(3, 5);

                    @Override
                    public Range answer(InvocationOnMock invocation) throws Throwable {
                        Range intervalPx = invocation.getArgument(0);
                        boolean includePartial = invocation.getArgument(1);

                        // Condition has been added so that we can capture a change to the value
                        if (includePartial) {
                            return new Range(
                                    Math.min(intervalPx.getFirst(), mDefaultRange.getFirst()),
                                    Math.max(intervalPx.getLast(), mDefaultRange.getLast())
                            );
                        }
                        return mDefaultRange;

                    }
                });
        PageRangeHandler adapter = new PageRangeHandler(mockPaginationModel);
        int dummyScrollY = 2;
        float dummyZoom = 2;
        int dummyViewHeight = 10;
        Range expectedResult = new Range(1, 6);

        adapter.refreshVisiblePageRange(dummyScrollY, dummyZoom, dummyViewHeight);
        Range result = adapter.getVisiblePages();

        assertEquals(result, expectedResult);
    }
}
