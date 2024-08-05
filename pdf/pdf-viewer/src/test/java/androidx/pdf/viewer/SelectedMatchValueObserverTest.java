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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import androidx.pdf.models.Dimensions;
import androidx.pdf.models.MatchRects;
import androidx.pdf.widget.ZoomView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;


@SmallTest
@RunWith(RobolectricTestRunner.class)
public class SelectedMatchValueObserverTest {
    private final PaginatedView mMockPaginatedView = mock(PaginatedView.class);
    private final PaginationModel mMockPaginationModel = mock(PaginationModel.class);
    private final PageViewFactory mMockPageViewFactory = mock(PageViewFactory.class);
    private final ZoomView mMockZoomView = mock(ZoomView.class);
    private final LayoutHandler mMockLayoutHandler = mock(LayoutHandler.class);
    private final SelectedMatch mMockOldSelection = mock(SelectedMatch.class);
    private final SelectedMatch mMockNewSelection = mock(SelectedMatch.class);
    private final PageViewFactory.PageView mMockOldPageView = mock(PageViewFactory.PageView.class);
    private final PageViewFactory.PageView mMockNewPageView = mock(PageViewFactory.PageView.class);
    private final PageMosaicView mMockOldPageMosaicView = mock(PageMosaicView.class);
    private final PageMosaicView mMockNewPageMosaicView = mock(PageMosaicView.class);
    private final MatchRects mMockMatchRects = mock(MatchRects.class);
    private final PdfHighlightOverlay mMockPdfHighlightOverlay = mock(PdfHighlightOverlay.class);
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void onChange_setOverlay() {
        when(mMockOldSelection.getPage()).thenReturn(1);
        when(mMockNewSelection.getPage()).thenReturn(2);
        when(mMockPaginationModel.getSize()).thenReturn(3);
        when(mMockPaginatedView.getViewAt(1)).thenReturn(mMockOldPageView);
        when(mMockPaginatedView.getViewAt(2)).thenReturn(mMockNewPageView);
        when(mMockPaginationModel.getPageSize(2)).thenReturn(new Dimensions(100, 100));
        when(mMockOldPageView.getPageView()).thenReturn(mMockOldPageMosaicView);
        when(mMockPageViewFactory.getOrCreatePageView(2, 2,
                new Dimensions(100, 100))).thenReturn(
                mMockNewPageMosaicView);
        when(mMockPageViewFactory.getOrCreatePageView(2, 2,
                new Dimensions(100, 100)).getPageView()).thenReturn(
                mMockNewPageMosaicView);
        when(mMockOldSelection.getPageMatches()).thenReturn(
                new MatchRects(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        when(mMockNewSelection.getPageMatches()).thenReturn(mMockMatchRects);
        when(mMockNewSelection.getSelected()).thenReturn(1);
        when(mMockNewSelection.getPage()).thenReturn(2);
        when(mMockMatchRects.getFirstRect(1)).thenReturn(new Rect(0, 0, 0, 0));
        when(mMockPaginationModel.getLookAtX(2, 0)).thenReturn(0);
        when(mMockPaginationModel.getLookAtY(2, 0)).thenReturn(0);
        when(mMockNewSelection.getOverlay()).thenReturn(mMockPdfHighlightOverlay);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = 1f;
        mContext.getResources().getDisplayMetrics().setTo(displayMetrics);
        PdfViewer.setScreenForTest(mContext);

        SelectedMatchValueObserver selectedMatchValueObserver = new SelectedMatchValueObserver(
                mMockPaginatedView, mMockPaginationModel, mMockPageViewFactory, mMockZoomView,
                mMockLayoutHandler, mContext);
        selectedMatchValueObserver.onChange(mMockOldSelection, mMockNewSelection);

        verify(mMockZoomView).centerAt(0, 0);
        verify(mMockNewPageMosaicView).setOverlay(mMockPdfHighlightOverlay);
    }
}
