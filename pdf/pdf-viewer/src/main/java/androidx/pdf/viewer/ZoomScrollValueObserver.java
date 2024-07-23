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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.ViewState;
import androidx.pdf.data.Range;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.widget.FastScrollView;
import androidx.pdf.widget.ZoomView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ZoomScrollValueObserver implements ObservableValue.ValueObserver<ZoomView.ZoomScroll> {
    private final PaginatedView mPaginatedView;
    private final ZoomView mZoomView;
    private final LayoutHandler mLayoutHandler;
    private final FloatingActionButton mAnnotationButton;
    private final FindInFileView mFindInFileView;
    private final PageIndicator mPageIndicator;
    private final FastScrollView mFastScrollView;
    private boolean mIsAnnotationIntentResolvable;
    private final ObservableValue<ViewState> mViewState;

    public ZoomScrollValueObserver(@NonNull ZoomView zoomView, @NonNull PaginatedView paginatedView,
            @NonNull LayoutHandler layoutHandler,
            @Nullable FloatingActionButton annotationButton,
            @Nullable FindInFileView findInFileView,
            @NonNull PageIndicator pageIndicator, @NonNull FastScrollView fastScrollView,
            boolean isAnnotationIntentResolvable,
            @NonNull ObservableValue<ViewState> viewState) {
        mZoomView = zoomView;
        mPaginatedView = paginatedView;
        mLayoutHandler = layoutHandler;
        mAnnotationButton = annotationButton;
        mFindInFileView = findInFileView;
        mPageIndicator = pageIndicator;
        mFastScrollView = fastScrollView;
        mIsAnnotationIntentResolvable = isAnnotationIntentResolvable;
        mViewState = viewState;
    }

    @Override
    public void onChange(@Nullable ZoomView.ZoomScroll oldPosition,
            @Nullable ZoomView.ZoomScroll position) {
        loadPageAssets(position);
        Range visiblePageRange = mPaginatedView.getPageRangeHandler()
                .computeVisibleRange(position.scrollY, position.zoom,
                        mZoomView.getHeight(), false);
        if (mPageIndicator.setRangeAndZoom(visiblePageRange, position.zoom,
                position.stable)) {
            showFastScrollView();
        }

        if (mIsAnnotationIntentResolvable) {
            if (position.scrollY > 0) {
                mAnnotationButton.setVisibility(View.GONE);
            } else if (position.scrollY == 0
                    && mAnnotationButton.getVisibility() == View.GONE
                    && mFindInFileView.getVisibility() == View.GONE) {
                mAnnotationButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadPageAssets(ZoomView.ZoomScroll position) {
        // Change the resolution of the bitmaps only when a gesture is not in progress.
        if (position.stable || mZoomView.getStableZoom() == 0) {
            mZoomView.setStableZoom(position.zoom);
        }

        mPaginatedView.getPaginationModel().setViewArea(mZoomView.getVisibleAreaInContentCoords());
        mPaginatedView.refreshPageRangeInVisibleArea(position, mZoomView.getHeight());
        mPaginatedView.handleGonePages(/* clearViews= */ false);
        mPaginatedView.loadInvisibleNearPageRange(mZoomView.getStableZoom());

        // The step (4) below requires page Views to be created and laid out. So we create them here
        // and set this flag if that operation needs to wait for a layout pass.
        boolean requiresLayoutPass = mPaginatedView.createPageViewsForVisiblePageRange();

        // 4. Refresh tiles and/or full pages.
        if (position.stable) {
            // Perform a full refresh on all visible pages
            mPaginatedView.refreshVisiblePages(requiresLayoutPass, mViewState.get(),
                    mZoomView.getStableZoom());
            mPaginatedView.handleGonePages(/* clearViews= */ true);
        } else if (mZoomView.getStableZoom() == position.zoom) {
            // Just load a few more tiles in case of tile-scroll
            mPaginatedView.refreshVisibleTiles(requiresLayoutPass, mViewState.get());
        }

        if (mPaginatedView.getPageRangeHandler().getVisiblePages() != null) {
            mLayoutHandler.maybeLayoutPages(
                    mPaginatedView.getPageRangeHandler().getVisiblePages().getLast());
        }
    }

    private void showFastScrollView() {
        if (mFastScrollView != null) {
            mFastScrollView.setVisible();
        }
    }

    public void setAnnotationIntentResolvable(boolean annotationIntentResolvable) {
        mIsAnnotationIntentResolvable = annotationIntentResolvable;
    }
}
