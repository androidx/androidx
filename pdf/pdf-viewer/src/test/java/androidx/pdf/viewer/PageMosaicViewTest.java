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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.MockDrawable;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.MosaicView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Unit tests for {@link PageMosaicView}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class PageMosaicViewTest {
    @Mock
    private MosaicView.BitmapSource mMockBitmapSource;

    @Mock
    private BitmapRecycler mMockBitmapRecycler;

    @Mock
    private PdfLoader mMockPdfLoader;

    @Mock
    private PdfSelectionModel mPdfSelectionModel;

    @Mock
    private SearchModel mSearchModel;

    PdfSelectionHandles mSelectionHandles = mock(PdfSelectionHandles.class);

    @Before
    public void setup() {

    }

    @Test
    public void newPageMosaicView_pageTextIsNull() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel,
                mSearchModel, mSelectionHandles);

        assertThat(pageMosaicView.getPageText() == null).isTrue();
    }

    @Test
    public void setOverlay_nonNull_addsNewOverlayForSearchKey() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel,
                mSearchModel, mSelectionHandles);

        pageMosaicView.setOverlay(new MockDrawable());
        assertThat(pageMosaicView.hasOverlay()).isTrue();
    }

    @Test
    public void setOverlay_nullOverlay_removesOverlayForSearchKey() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel,
                mSearchModel, mSelectionHandles);

        pageMosaicView.setOverlay(new MockDrawable());
        assertThat(pageMosaicView.hasOverlay()).isTrue();

        pageMosaicView.setOverlay(null);
        assertThat(pageMosaicView.hasOverlay()).isFalse();
    }

    @Test
    public void setPageText_nonNullPageText_setsPageTextAndContentDescription() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel,
                mSearchModel, mSelectionHandles) {
            @Override
            @NonNull
            protected String buildContentDescription(@Nullable String pageText, int pageNum) {
                return (pageText != null) ? pageText : "dummyString";
            }
        };

        String pageText = "dummyPageText";
        pageMosaicView.setPageText(pageText);
        assertThat(pageMosaicView.getPageText()).isEqualTo(pageText);
        assertThat(pageMosaicView.getContentDescription()).isEqualTo(pageText);
    }

    @Test
    public void setPageText_nullPageText_nullPageTextAndNonNullContentDescription() {
        String dummyContentDescription = "dummyContentDescription";
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, mMockPdfLoader, mPdfSelectionModel,
                mSearchModel, mSelectionHandles) {
            @Override
            @NonNull
            protected String buildContentDescription(@Nullable String pageText, int pageNum) {
                return (pageText != null) ? pageText : dummyContentDescription;
            }
        };

        pageMosaicView.setPageText(null);
        assertThat(pageMosaicView.getPageText() == null).isTrue();
        assertThat(pageMosaicView.getContentDescription()).isEqualTo(dummyContentDescription);
    }

    @Test
    public void refreshPageContentAndOverlays_needsPageText_callsLoadPageTextRemovesOverlay() {
        PdfLoader dummyPdfLoader = mock(PdfLoader.class);
        PdfSelectionModel dummyPdfSelectionModel = mock(PdfSelectionModel.class);
        SearchModel dummySearchModel = mock(SearchModel.class);
        when(dummyPdfSelectionModel.getPage()).thenReturn(1);
        when(dummySearchModel.query()).thenReturn(new ObservableValue<String>() {
            @Nullable
            @Override
            public String get() {
                return null;
            }

            @NonNull
            @Override
            public Object addObserver(ValueObserver<String> observer) {
                return null;
            }

            @Override
            public void removeObserver(@NonNull Object observerKey) {

            }
        });

        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, dummyPdfLoader, dummyPdfSelectionModel,
                dummySearchModel, mSelectionHandles) {
            @Override
            public boolean needsPageText() {
                return true;
            }
        };

        pageMosaicView.refreshPageContentAndOverlays();

        verify(dummyPdfLoader).loadPageText(any(Integer.class));
        verify(dummyPdfLoader).loadPageUrlLinks(any(Integer.class));
        verify(dummyPdfLoader).loadPageGotoLinks(any(Integer.class));
        assertFalse(pageMosaicView.hasOverlay());
    }

    @Test
    public void refreshPageContentAndOverlays_sameSelectionPage_setsHighlightOverlay() {
        List<Rect> boundingBoxes = List.of(new Rect(10, 10, 10, 10));
        PdfLoader dummyPdfLoader = mock(PdfLoader.class);
        PdfSelectionModel dummyPdfSelectionModel = mock(PdfSelectionModel.class);
        SearchModel dummySearchModel = mock(SearchModel.class);
        when(dummyPdfSelectionModel.getPage()).thenReturn(0);
        when(dummyPdfSelectionModel.selection()).thenReturn(new ObservableValue<PageSelection>() {

            @NonNull
            @Override
            public Object addObserver(ValueObserver<PageSelection> observer) {
                return null;
            }

            @Override
            public void removeObserver(@NonNull Object observerKey) {

            }

            @Nullable
            @Override
            public PageSelection get() {
                return new PageSelection(0, mock(SelectionBoundary.class),
                        mock(SelectionBoundary.class), boundingBoxes, "");
            }
        });

        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, dummyPdfLoader, dummyPdfSelectionModel,
                dummySearchModel, mSelectionHandles) {
            @Override
            public boolean needsPageText() {
                return false;
            }
        };

        pageMosaicView.refreshPageContentAndOverlays();

        assertTrue(pageMosaicView.hasOverlay());
    }

    @Test
    public void refreshPageContentAndOverlays_nonNullSearchQuery_callsSearchPageText() {
        PdfLoader dummyPdfLoader = mock(PdfLoader.class);
        PdfSelectionModel dummyPdfSelectionModel = mock(PdfSelectionModel.class);
        SearchModel dummySearchModel = mock(SearchModel.class);
        when(dummyPdfSelectionModel.getPage()).thenReturn(1);
        when(dummySearchModel.query()).thenReturn(new ObservableValue<String>() {
            @Nullable
            @Override
            public String get() {
                return "placeholder";
            }

            @NonNull
            @Override
            public Object addObserver(ValueObserver<String> observer) {
                return null;
            }

            @Override
            public void removeObserver(@NonNull Object observerKey) {

            }
        });

        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler, dummyPdfLoader, dummyPdfSelectionModel,
                dummySearchModel, mSelectionHandles) {
            @Override
            public boolean needsPageText() {
                return false;
            }
        };

        pageMosaicView.refreshPageContentAndOverlays();

        verify(dummyPdfLoader).searchPageText(any(Integer.class), any(String.class));
    }
}
