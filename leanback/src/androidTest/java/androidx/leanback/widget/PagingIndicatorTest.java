/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link PagingIndicator}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PagingIndicatorTest {
    private PagingIndicator mIndicator;

    @Before
    public void setup() throws Exception {
        mIndicator = new PagingIndicator(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void testDotPosition() {
        mIndicator.setPageCount(3);
        assertDotPosition();
        mIndicator.setPageCount(6);
        assertDotPosition();
        mIndicator.setPageCount(9);
        assertDotPosition();
    }

    private void assertDotPosition() {
        assertSymmetry();
        assertDistance();
    }

    private void assertSymmetry() {
        int pageCount = mIndicator.getPageCount();
        int mid = pageCount / 2;
        int[] selectedX = mIndicator.getDotSelectedX();
        int sum = selectedX[0] + selectedX[pageCount - 1];
        for (int i = 1; i <= mid; ++i) {
            assertEquals("Selected dots are not symmetric", sum,
                    selectedX[i] + selectedX[pageCount - i - 1]);
        }
        int[] leftX = mIndicator.getDotSelectedLeftX();
        int[] rightX = mIndicator.getDotSelectedRightX();
        sum = leftX[0] + rightX[pageCount - 1];
        for (int i = 1; i < pageCount - 1; ++i) {
            assertEquals("Deselected dots are not symmetric", sum,
                    leftX[i] + rightX[pageCount - i - 1]);
        }
    }

    private void assertDistance() {
        int pageCount = mIndicator.getPageCount();
        int[] selectedX = mIndicator.getDotSelectedX();
        int[] leftX = mIndicator.getDotSelectedLeftX();
        int[] rightX = mIndicator.getDotSelectedRightX();
        int distance = selectedX[1] - selectedX[0];
        for (int i = 2; i < pageCount; ++i) {
            assertEquals("Gaps between selected dots are not even", distance,
                    selectedX[i] - selectedX[i - 1]);
        }
        distance = leftX[1] - leftX[0];
        for (int i = 2; i < pageCount - 1; ++i) {
            assertEquals("Gaps between left dots are not even", distance,
                    leftX[i] - leftX[i - 1]);
        }
        distance = rightX[2] - rightX[1];
        for (int i = 3; i < pageCount; ++i) {
            assertEquals("Gaps between right dots are not even", distance,
                    rightX[i] - rightX[i - 1]);
        }
    }
}
