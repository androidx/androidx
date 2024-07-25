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

import android.content.Context;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.PaginationUtils;
import androidx.pdf.widget.ZoomView;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectedMatchValueObserver implements ObservableValue.ValueObserver<SelectedMatch> {
    private final PaginatedView mPaginatedView;
    private final PaginationModel mPaginationModel;
    private final PageViewFactory mPageViewFactory;
    private final LayoutHandler mLayoutHandler;
    private final ZoomView mZoomView;
    private final Context mContext;

    public SelectedMatchValueObserver(@NonNull PaginatedView paginatedView,
            @NonNull PaginationModel paginationModel, @NonNull PageViewFactory pageViewFactory,
            @NonNull ZoomView zoomView, @NonNull LayoutHandler layoutHandler,
            @NonNull Context context) {
        mPaginatedView = paginatedView;
        mPaginationModel = paginationModel;
        mPageViewFactory = pageViewFactory;
        mZoomView = zoomView;
        mLayoutHandler = layoutHandler;
        mContext = context;
    }

    @Override
    public void onChange(
            @Nullable SelectedMatch oldSelection,
            @Nullable SelectedMatch newSelection) {
        if (newSelection == null) {
            mPaginatedView.clearAllOverlays();
            return;
        }
        if (oldSelection != null && isPageCreated(oldSelection.getPage())) {
            // Selected match has moved onto a new page - update the overlay on
            // the old page.
            mPaginatedView.getViewAt(oldSelection.getPage())
                    .getPageView()
                    .setOverlay(
                            new PdfHighlightOverlay(oldSelection.getPageMatches()));
        }
        lookAtSelection(newSelection);
    }

    private boolean isPageCreated(int pageNum) {
        return pageNum < mPaginationModel.getSize() && mPaginatedView.getViewAt(pageNum) != null;
    }

    private void lookAtSelection(SelectedMatch selection) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        if (selection.getPage() >= mPaginationModel.getSize()) {
            mLayoutHandler.layoutPages(selection.getPage() + 1);
            return;
        }
        Rect rect = selection.getPageMatches().getFirstRect(selection.getSelected());
        int x = mPaginationModel.getLookAtX(selection.getPage(), rect.centerX());
        int y = mPaginationModel.getLookAtY(selection.getPage(), rect.centerY());
        mZoomView.centerAt(x, y);

        PageMosaicView pageView = (PageMosaicView) mPageViewFactory.getOrCreatePageView(
                selection.getPage(),
                PaginationUtils.getPageElevationInPixels(mContext),
                mPaginationModel.getPageSize(selection.getPage()));
        pageView.setOverlay(selection.getOverlay());
    }
}
