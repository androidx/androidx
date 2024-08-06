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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.Range;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.PaginationUtils;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PageSelectionValueObserver implements ObservableValue.ValueObserver<PageSelection> {
    private final PaginatedView mPaginatedView;
    private final PaginationModel mPaginationModel;
    private final PageViewFactory mPageViewFactory;
    private Context mContext;

    public PageSelectionValueObserver(@NonNull PaginatedView paginatedView,
            @NonNull PaginationModel paginationModel,
            @NonNull PageViewFactory pageViewFactory, @NonNull Context context) {
        mPaginatedView = paginatedView;
        mPaginationModel = paginationModel;
        mPageViewFactory = pageViewFactory;
        mContext = context;
    }

    @Override
    public void onChange(@Nullable PageSelection oldSelection,
            @Nullable PageSelection newSelection) {
        if (oldSelection != null && isPageCreated(oldSelection.getPage())) {
            getPage(oldSelection.getPage()).getPageView().setOverlay(null);
        }

        Range visiblePageRange =
                mPaginatedView.getPageRangeHandler().getVisiblePages();
        if (newSelection != null && visiblePageRange.contains(
                newSelection.getPage())) {
            mPageViewFactory.getOrCreatePageView(
                            newSelection.getPage(),
                            PaginationUtils.getPageElevationInPixels(mContext),
                            mPaginationModel.getPageSize(newSelection.getPage())).getPageView()
                    .setOverlay(new PdfHighlightOverlay(newSelection));
        }
    }

    private boolean isPageCreated(int pageNum) {
        return pageNum < mPaginationModel.getSize() && mPaginatedView.getViewAt(pageNum) != null;
    }

    private PageViewFactory.PageView getPage(int pageNum) {
        return mPaginatedView.getViewAt(pageNum);
    }
}
