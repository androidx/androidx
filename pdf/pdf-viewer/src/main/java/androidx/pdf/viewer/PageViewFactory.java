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
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.metrics.EventCallback;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.util.Accessibility;
import androidx.pdf.util.GestureTracker;
import androidx.pdf.util.TileBoard;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.MosaicView;
import androidx.pdf.widget.ZoomView;

import java.util.List;

/**
 * Factory to create the appropriate {@link PageView}, determined by whether TalkBack is on or off.
 *
 * <p>Returns a {@link PageLinksView} if TalkBack is on, otherwise returns a {@link PageMosaicView}.
 * NOTE: This was done as a performance improvement, since View mandates that the {@link
 * PageMosaicView} itself cannot have virtual view links as children, we need a container and
 * another view to put them in, but we didn't want users without TalkBack enabled to take a
 * performance hit by inserting another view into the hierarchy if they will never use it. Since the
 * determination of which view to use is done once at the time the document is initially rendered,
 * accessible link functionality will not work if TalkBack is turned on during a Pico session.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PageViewFactory {
    private final Context mContext;
    private final PdfLoader mPdfLoader;
    private final PaginatedView mPaginatedView;
    private final ZoomView mZoomView;
    private final SingleTapHandler mSingleTapHandler;
    private final FindInFileView mFindInFileView;
    private final EventCallback mEventCallback;

    public PageViewFactory(@NonNull Context context,
            @NonNull PdfLoader pdfLoader,
            @NonNull PaginatedView paginatedView,
            @NonNull ZoomView zoomView,
            @NonNull SingleTapHandler singleTapHandler,
            @NonNull FindInFileView findInFileView,
            @Nullable EventCallback eventCallback) {
        this.mContext = context;
        this.mPdfLoader = pdfLoader;
        this.mPaginatedView = paginatedView;
        this.mZoomView = zoomView;
        this.mSingleTapHandler = singleTapHandler;
        this.mFindInFileView = findInFileView;
        this.mEventCallback = eventCallback;
    }

    /**
     * Interface encapsulating a single page, and accessibility view if necessary.
     *
     * <p>NOTE: Meant to be extended by a {@link View} that returns itself in {@link #asView()}.
     */
    public interface PageView {

        /** Returns the {@link PageMosaicView} associated with this PageView. */
        @NonNull
        PageMosaicView getPageView();

        /** Return page number. */
        int getPageNum();

        /** Set page URL links. */
        void setPageUrlLinks(@Nullable LinkRects links);

        /** Set page goto links. */
        void setPageGotoLinks(@Nullable List<GotoLink> links);

        /**
         * Returns the base view that implements this interface.
         *
         * <p>NOTE: This is the view that should be added to the view hierarchy. May return the same
         * object as {@link #getPageView()}, e.g. for the {@link PageMosaicView} implementation.
         */
        @NonNull
        View asView();

        /** Clear all bitmaps and reset the view overlay. */
        void clearAll();
    }

    /**
     * Returns an instance of {@link PageView}. If the view is already created and added to the
     * {@link PaginatedView} then it will be returned from that list else a new instance will be
     * created.
     */
    @NonNull
    public PageMosaicView getOrCreatePageView(int pageNum,
            int pageElevationInPixels,
            @NonNull Dimensions pageDimensions) {
        PageView pageView = mPaginatedView.getViewAt(pageNum);
        if (pageView == null) {
            pageView = createAndSetupPageView(pageNum, pageElevationInPixels, pageDimensions);
        }

        return pageView.getPageView();
    }

    /**
     * Returns a {@link PageMosaicView}, bundled together with a {@link PageLinksView} and
     * optionally a {@link AccessibilityPageWrapper} if TalkBack is on, otherwise returns
     * a {@link PageMosaicView}.
     */
    @NonNull
    protected PageView createPageView(
            int pageNum,
            @NonNull Dimensions pageSize) {
        final MosaicView.BitmapSource bitmapSource = createBitmapSource(pageNum);
        final PageMosaicView pageMosaicView =
                new PageMosaicView(mContext, pageNum, pageSize, bitmapSource,
                        TileBoard.DEFAULT_RECYCLER, mPdfLoader, mPaginatedView.getSelectionModel(),
                        mPaginatedView.getSearchModel(), mPaginatedView.getSelectionHandles());
        if (isTouchExplorationEnabled(mContext)) {
            final PageLinksView pageLinksView = new PageLinksView(mContext, mZoomView.zoomScroll());

            return new AccessibilityPageWrapper(
                    mContext, pageNum, pageMosaicView, pageLinksView);
        } else {
            return pageMosaicView;
        }
    }

    @VisibleForTesting
    protected boolean isTouchExplorationEnabled(@NonNull Context context) {
        return Accessibility.get().isTouchExplorationEnabled(context);
    }

    @NonNull
    protected MosaicView.BitmapSource createBitmapSource(int pageNum) {
        return new MosaicView.BitmapSource() {

            @Override
            public void requestPageBitmap(@NonNull Dimensions pageSize,
                    boolean alsoRequestingTiles) {
                if (!alsoRequestingTiles) {
                    if (mEventCallback != null) {
                        mEventCallback.onPageBitmapOnlyRequested(pageNum);
                    }
                }
                mPdfLoader.loadPageBitmap(pageNum, pageSize);
            }

            @Override
            public void requestNewTiles(@NonNull Dimensions pageSize,
                    @NonNull Iterable<TileBoard.TileInfo> tiles) {
                if (mEventCallback != null) {
                    mEventCallback.onPageTilesRequested(pageNum, tiles);
                }
                mPdfLoader.loadTileBitmaps(pageNum, pageSize, tiles);
            }

            @Override
            public void cancelTiles(@NonNull Iterable<Integer> tileIds) {
                if (mEventCallback != null) {
                    mEventCallback.onPageTilesCleared(pageNum);
                }
                mPdfLoader.cancelTileBitmaps(pageNum, tileIds);
            }
        };
    }

    @NonNull
    protected PageView createAndSetupPageView(int pageNum,
            int pageElevationInPixels,
            @NonNull Dimensions pageDimensions) {
        PageView pageView =
                createPageView(
                        pageNum,
                        pageDimensions
                );
        mPaginatedView.addView(pageView);

        GestureTracker gestureTracker = new GestureTracker(mContext);
        gestureTracker.setDelegateHandler(new PageTouchListener(pageView, mPdfLoader,
                mSingleTapHandler, mFindInFileView));
        pageView.asView().setOnTouchListener(gestureTracker);

        PageMosaicView pageMosaicView = pageView.getPageView();
        // Setting Elevation only works if there is a background color.
        pageMosaicView.setBackgroundColor(Color.WHITE);
        pageMosaicView.setElevation(pageElevationInPixels);
        return pageView;
    }
}
