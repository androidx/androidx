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
import android.os.Build;

import androidx.pdf.models.Dimensions;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.viewer.PageViewFactory.PageView;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.MosaicView.BitmapSource;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests for {@link PaginatedView}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PaginatedViewTest {

    PaginatedView mPaginatedView;
    PaginationModel mPaginationModel;
    Context mContext;
    Dimensions mDimensions;

    @Mock
    BitmapSource mMockBitmapSource;
    @Mock
    BitmapRecycler mMockBitmapRecycler;
    @Mock
    PdfLoader mMockPdfLoader;
    @Mock
    PdfSelectionModel mPdfSelectionModel;
    @Mock
    SearchModel mSearchModel;
    @Mock
    PdfSelectionHandles mSelectionHandles;

    PageView mTestPageView0;
    PageView mTestPageView1;

    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mDimensions = new Dimensions(100, 200);

        PdfViewer.setScreenForTest(mContext);
        // Setting uninitialized model.
        mPaginatedView = new PaginatedView(mContext);
        mPaginationModel = new PaginationModel(mContext);

        mPaginatedView.setModel(mPaginationModel);
        mTestPageView0 = new PageMosaicView(mContext, 0, mDimensions, mMockBitmapSource,
                mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel, mSearchModel,
                mSelectionHandles);
        mTestPageView1 = new PageMosaicView(mContext, 1, mDimensions, mMockBitmapSource,
                mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel, mSearchModel,
                mSelectionHandles);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void testAddAndRemoveViews() {
        mPaginationModel.initialize(3);
        mPaginationModel.addPage(0, mDimensions);
        mPaginationModel.addPage(1, mDimensions);

        mPaginatedView.addView(mTestPageView0);
        mPaginatedView.addView(mTestPageView1);
        Assert.assertEquals(mTestPageView0, mPaginatedView.getViewAt(0));
        Assert.assertEquals(mTestPageView1, mPaginatedView.getViewAt(1));
        List<PageMosaicView> childViews = mPaginatedView.getChildViews();
        Assert.assertEquals(2, childViews.size());
        Assert.assertEquals(mTestPageView0, childViews.get(0));
        Assert.assertEquals(mTestPageView1, childViews.get(1));

        mPaginationModel.addPage(2, mDimensions);
        PageView testPageView3 = new PageMosaicView(mContext, 2, mDimensions, mMockBitmapSource,
                mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel, mSearchModel,
                mSelectionHandles);
        mPaginatedView.addView(testPageView3);

        mPaginatedView.removeViewAt(1);
        Assert.assertEquals(null, mPaginatedView.getViewAt(1));
    }

    @Test
    public void testRemoveAllViews() {
        mPaginationModel.initialize(3);
        mPaginationModel.addPage(0, mDimensions);
        mPaginationModel.addPage(1, mDimensions);

        mPaginatedView.addView(mTestPageView0);
        mPaginatedView.addView(mTestPageView1);

        mPaginatedView.removeAllViews();
        assertThat(mPaginatedView.getChildViews()).isEmpty();
    }
}
