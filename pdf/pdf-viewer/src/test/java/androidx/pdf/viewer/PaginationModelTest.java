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

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;

import androidx.pdf.data.Range;
import androidx.pdf.models.Dimensions;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PaginationModelTest {

    private static final Dimensions ONE_HUNDRED_BY_TWO_HUNDRED = new Dimensions(100, 200);
    private static final Dimensions THREE_HUNDRED_BY_THREE_HUNDRED = new Dimensions(300, 300);

    private Context mContext;
    private PaginationModel mPaginationModel;

    @Before
    public void init() {
        mContext = ApplicationProvider.getApplicationContext();
        PdfViewer.setScreenForTest(mContext);
        mPaginationModel = new PaginationModel(mContext);
    }

    /** Test that the model can be initialized to a positive number of pages. */
    @Test
    public void initialize() {
        assertThat(mPaginationModel.isInitialized()).isFalse();
        mPaginationModel.initialize(1);
        assertThat(mPaginationModel.isInitialized()).isTrue();
    }

    /**
     * Test that model cannot be initialized to 0 or negative pages and that exceptions are thrown
     * when this is attempted.
     */
    @Test
    public void initialize_invalidCountThrowsException() {
        Assert.assertThrows(IllegalArgumentException.class, () -> mPaginationModel.initialize(-1));
    }

    @Test
    public void addPage() {
        mPaginationModel.initialize(1);
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);
        Dimensions result = mPaginationModel.getPageSize(0);
        assertThat(result).isEqualTo(ONE_HUNDRED_BY_TWO_HUNDRED);
        assertThat(mPaginationModel.getSize()).isEqualTo(1);
    }

    @Test
    public void getEstimatedFullHeight() {
        mPaginationModel.initialize(3);
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);
        mPaginationModel.addPage(1, ONE_HUNDRED_BY_TWO_HUNDRED);
        // known page heights (400) + estimated height unrendered page based on running average
        // (200) + spacing * 6 (24) = 624
        assertThat(mPaginationModel.getEstimatedFullHeight()).isEqualTo(624);
    }

    @Test
    public void getAccurateFullHeightWhenKnown() {
        mPaginationModel.initialize(5);
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);
        mPaginationModel.addPage(1, ONE_HUNDRED_BY_TWO_HUNDRED);
        // known page heights (400) + estimated height unrendered pages based on running average
        // (600) + spacing * 10 (40) = 1040
        Assert.assertEquals(1040, mPaginationModel.getEstimatedFullHeight());

        mPaginationModel.addPage(2, THREE_HUNDRED_BY_THREE_HUNDRED);
        mPaginationModel.addPage(3, THREE_HUNDRED_BY_THREE_HUNDRED);
        mPaginationModel.addPage(4, THREE_HUNDRED_BY_THREE_HUNDRED);
        // known page heights (1300) + spacing * 10 (40) = 1340
        assertThat(mPaginationModel.getEstimatedFullHeight()).isEqualTo(1340);
    }

    @Test
    public void getPagesInWindow() {
        mPaginationModel.initialize(3);
        // Dimensions height is 200px per page + 4px spacing before and after.
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);
        mPaginationModel.addPage(1, ONE_HUNDRED_BY_TWO_HUNDRED);
        mPaginationModel.addPage(2, ONE_HUNDRED_BY_TWO_HUNDRED);

        // First page not covered by window.
        Range range = mPaginationModel.getPagesInWindow(new Range(205, 500), true);
        assertThat(range.getFirst()).isEqualTo(1);
        assertThat(range.getLast()).isEqualTo(2);
    }

    @Test
    public void getPagesInWindow_noPageInViewport() {
        mPaginationModel.initialize(3);
        // Dimensions height is 200px per page + 4px spacing before and after.
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);

        // No page in view port - return empty range.
        Range range = mPaginationModel.getPagesInWindow(new Range(205, 500), false);
        assertThat(range).isEmpty();
    }

    @Test
    public void getPagesInWindow_onlyPartialPageInViewport() {
        mPaginationModel.initialize(3);
        // Dimensions height is 200px per page + 4px spacing before and after.
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);

        // Only page is partially in view port - return partial page, even if includePartial =
        // false.
        Range range = mPaginationModel.getPagesInWindow(new Range(204, 500), false);
        assertThat(range.getFirst()).isEqualTo(0);
        assertThat(range.getLast()).isEqualTo(0);
    }

    @Test
    public void getPagesInWindow_includePartial() {
        mPaginationModel.initialize(3);
        // Dimensions height is 200px per page + 4px spacing before and after.
        mPaginationModel.addPage(0, ONE_HUNDRED_BY_TWO_HUNDRED);
        mPaginationModel.addPage(1, ONE_HUNDRED_BY_TWO_HUNDRED);

        // Include partial = false, don't include partial second page.
        Range range = mPaginationModel.getPagesInWindow(new Range(0, 212), false);
        assertThat(range.getFirst()).isEqualTo(0);
        assertThat(range.getLast()).isEqualTo(0);

        // Include partial = true, include partial second page.
        range = mPaginationModel.getPagesInWindow(new Range(0, 212), true);
        assertThat(range.getFirst()).isEqualTo(0);
        assertThat(range.getLast()).isEqualTo(1);
    }

    /**
     * Add 3 pages of differing sizes to the model. Set the visible area to cover the whole model.
     * Largest page should span (0, model width). Smaller pages should be placed in the middle of
     * the model horizontally. Pages should get consistent vertical spacing.
     */
    @Test
    public void getPageLocation_viewAreaCoversFullModel() {
        Dimensions smDimensions = new Dimensions(200, 100);
        Dimensions mdDimensions = new Dimensions(400, 200);
        Dimensions lgDimensions = new Dimensions(800, 400);

        mPaginationModel.initialize(3);
        mPaginationModel.addPage(0, smDimensions);
        mPaginationModel.addPage(1, mdDimensions);
        mPaginationModel.addPage(2, lgDimensions);
        // Setting viewArea large enough to accommodate the entire model.
        mPaginationModel.setViewArea(new Rect(0, 0, 800, 800));

        Rect expectedSmLocation = new Rect(300, 0, 500, 100);
        assertThat(mPaginationModel.getPageLocation(0)).isEqualTo(expectedSmLocation);

        Rect expectedMdLocation =
                new Rect(200, 100 + getSpacingAbovePage(1), 600, 300 + getSpacingAbovePage(1));
        assertThat(mPaginationModel.getPageLocation(1)).isEqualTo(expectedMdLocation);

        Rect expectedLgLocation =
                new Rect(0, 300 + getSpacingAbovePage(2), 800, 700 + getSpacingAbovePage(2));
        Assert.assertEquals(expectedLgLocation, mPaginationModel.getPageLocation(2));
        assertThat(mPaginationModel.getPageLocation(2)).isEqualTo(expectedLgLocation);
    }

    /**
     * {@link PaginationModel#getPageLocation(int)} should try to fit as much of each page into the
     * viewable area as possible. Dimensions do not change vertically but pages that are smaller
     * than {@link PaginationModel#getWidth()} can be moved horizontally to make this happen.
     *
     * <p>Page 1's width is smaller than {@link PaginationModel#getWidth()} so it will be placed in
     * the middle when the visible area covers the whole model {@see #testGetPageLocation}. When the
     * visible area is set to a width smaller than page 1's width, it should move to the left or
     * right to the visible area. This test focuses on the bottom-left corner so page 1 should be
     * moved all the way to the left. It's vertical coordinates should not change.
     */
    @Test
    public void getPageLocation_horizontalLocationMoves() {
        Dimensions smDimensions = new Dimensions(200, 100);
        Dimensions mdDimensions = new Dimensions(400, 200);
        Dimensions lgDimensions = new Dimensions(800, 400);

        mPaginationModel.initialize(3);
        mPaginationModel.addPage(0, smDimensions);
        mPaginationModel.addPage(1, mdDimensions);
        mPaginationModel.addPage(2, lgDimensions);

        // Setting viewArea to a 300x200 section in the bottom-left corner of the model.
        mPaginationModel.setViewArea(new Rect(0, 500, 200, 800));

        Rect expectedMdLocation =
                new Rect(0, 100 + getSpacingAbovePage(1), 400, 300 + getSpacingAbovePage(1));
        assertThat(mPaginationModel.getPageLocation(1)).isEqualTo(expectedMdLocation);
    }

    /**
     * Helper method. Spacing between pages is in DP so can change based on device. This method
     * obtains the value for the current {@link #mPaginationModel}. Pages are zero-indexed so
     * page <code>X</code> will have <code>X</code> full page gaps above it.
     */
    private int getSpacingAbovePage(int pageNum) {
        return mPaginationModel.getPageSpacingPx() * 2 * pageNum;
    }
}
