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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.pdf.data.Range;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.PageSelection;
import androidx.pdf.select.SelectionActionMode;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PageSelectionValueObserverTest {
    private final PaginatedView mMockPaginatedView = mock(PaginatedView.class);
    private final PaginationModel mMockPaginationModel = mock(PaginationModel.class);
    private final PageViewFactory mMockPageViewFactory = mock(PageViewFactory.class);
    private final PageSelection mMockOldPageSelection = mock(PageSelection.class);
    private final PageSelection mMockNewPageSelection = mock(PageSelection.class);
    private final PageViewFactory.PageView mMockOldPageView = mock(PageViewFactory.PageView.class);
    private final PageViewFactory.PageView mMockNewPageView = mock(PageViewFactory.PageView.class);
    private final PageMosaicView mMockOldPageMosaicView = mock(PageMosaicView.class);
    private final PageMosaicView mMockNewPageMosaicView = mock(PageMosaicView.class);
    private final PageRangeHandler mMockPageRangeHandler = mock(PageRangeHandler.class);
    private final SelectionActionMode mMockSelectionActionMode = mock(SelectionActionMode.class);
    private final Context mContext = ApplicationProvider.getApplicationContext();


    @Test
    public void onChange_setOverlay() {
        when(mMockOldPageSelection.getPage()).thenReturn(1);
        when(mMockNewPageSelection.getPage()).thenReturn(2);
        when(mMockPaginationModel.getSize()).thenReturn(3);
        when(mMockPaginationModel.getPageSize(2)).thenReturn(new Dimensions(100, 100));
        when(mMockPaginatedView.getViewAt(1)).thenReturn(mMockOldPageView);
        when(mMockPaginatedView.getViewAt(2)).thenReturn(mMockNewPageView);
        when(mMockOldPageView.getPageView()).thenReturn(mMockOldPageMosaicView);
        when(mMockPageViewFactory.getOrCreatePageView(2, 2, new Dimensions(100, 100))).thenReturn(
                mMockNewPageMosaicView);
        when(mMockPaginatedView.getPageRangeHandler()).thenReturn(mMockPageRangeHandler);
        when(mMockPageRangeHandler.getVisiblePages()).thenReturn(new Range(1, 2));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.density = 1f;
        mContext.getResources().getDisplayMetrics().setTo(displayMetrics);
        PdfViewer.setScreenForTest(mContext);

        PageSelectionValueObserver pageSelectionValueObserver =
                new PageSelectionValueObserver(mMockPaginatedView, mMockPaginationModel,
                        mMockPageViewFactory, mContext, mMockSelectionActionMode);
        pageSelectionValueObserver.onChange(mMockOldPageSelection, mMockNewPageSelection);

        verify(mMockOldPageMosaicView).setOverlay(null);
        verify(mMockNewPageMosaicView).setOverlay(any());
    }
}
