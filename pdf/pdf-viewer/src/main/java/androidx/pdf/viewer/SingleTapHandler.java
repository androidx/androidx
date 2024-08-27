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

import static android.view.View.GONE;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.models.GotoLinkDestination;
import androidx.pdf.util.ExternalLinks;
import androidx.pdf.util.ZoomUtils;
import androidx.pdf.widget.ZoomView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SingleTapHandler {
    private final Context mContext;
    private final FloatingActionButton mFloatingActionButton;
    private final PaginatedView mPaginatedView;
    private final FindInFileView mFindInFileView;
    private final ZoomView mZoomView;
    private final PdfSelectionModel mPdfSelectionModel;
    private final PaginationModel mPaginationModel;
    private final LayoutHandler mLayoutHandler;
    private boolean mIsAnnotationIntentResolvable;

    public SingleTapHandler(@NonNull Context context,
            @NonNull FloatingActionButton floatingActionButton,
            @NonNull PaginatedView paginatedView,
            @NonNull FindInFileView findInFileView,
            @NonNull ZoomView zoomView,
            @NonNull PdfSelectionModel pdfSelectionModel,
            @NonNull PaginationModel paginationModel,
            @NonNull LayoutHandler layoutHandler) {
        mContext = context;
        mFloatingActionButton = floatingActionButton;
        mPaginatedView = paginatedView;
        mFindInFileView = findInFileView;
        mZoomView = zoomView;
        mPdfSelectionModel = pdfSelectionModel;
        mPaginationModel = paginationModel;
        mLayoutHandler = layoutHandler;
    }

    public void setAnnotationIntentResolvable(boolean annotationIntentResolvable) {
        mIsAnnotationIntentResolvable = annotationIntentResolvable;
    }

    public void handleSingleTapConfirmedEventOnPage(@NonNull MotionEvent event,
            @NonNull PageMosaicView pageMosaicView) {
        if (mIsAnnotationIntentResolvable) {
            if (mFloatingActionButton.getVisibility() == View.GONE
                    && mFindInFileView.getVisibility() == GONE) {
                mFloatingActionButton.setVisibility(View.VISIBLE);
            } else {
                mFloatingActionButton.setVisibility(View.GONE);
            }
        }

        handleSelection();

        Point point = new Point((int) event.getX(), (int) event.getY());
        handleExternalLink(point, pageMosaicView);

        GotoLinkDestination gotoDest = pageMosaicView.getGotoDestination(point);
        if (gotoDest != null) {
            gotoPageDest(gotoDest);
        }
    }

    private void handleSelection() {
        boolean hadSelection =
                mPdfSelectionModel != null && mPdfSelectionModel.selection().get() != null;
        if (hadSelection) {
            mPdfSelectionModel.setSelection(null);
        }
    }

    private void handleExternalLink(Point point, PageMosaicView pageMosaicView) {
        String linkUrl = pageMosaicView.getLinkUrl(point);
        if (linkUrl != null) {
            ExternalLinks.open(linkUrl, mContext);
        }
    }

    /**  */
    public void gotoPageDest(@NonNull GotoLinkDestination destination) {

        if (destination.getPageNumber() >= mPaginationModel.getSize()) {
            // We have not yet loaded our destination.
            mLayoutHandler.layoutPages(destination.getPageNumber() + 1);
            mLayoutHandler.add(
                    pageNum -> {
                        if (pageNum == destination.getPageNumber()) {
                            gotoPageDest(destination);
                            return false;
                        }
                        return true;
                    });
            return;
        }

        if (destination.getYCoordinate() != null) {
            int pageY = (int) destination.getYCoordinate().floatValue();

            Rect pageRect = mPaginationModel.getPageLocation(destination.getPageNumber(),
                    mPaginatedView.getViewArea());
            int x = pageRect.left + (pageRect.width() / 2);
            int y = mPaginationModel.getLookAtY(destination.getPageNumber(), pageY);
            // Zoom should match the width of the page.
            float zoom =
                    ZoomUtils.calculateZoomToFit(
                            mZoomView.getViewportWidth(), mZoomView.getViewportHeight(),
                            pageRect.width(), 1);

            mZoomView.setZoom(zoom);
            mZoomView.centerAt(x, y);
        } else {
            gotoPage(destination.getPageNumber());
        }
    }

    /** Goes to the {@code pageNum} and fits the page to the current viewport. */
    private void gotoPage(int pageNum) {
        if (pageNum >= mPaginationModel.getSize()) {
            // We have not yet loaded our destination.
            mLayoutHandler.layoutPages(pageNum + 1);
            mLayoutHandler.add(
                    loadedPageNum -> {
                        if (pageNum == loadedPageNum) {
                            gotoPage(pageNum);
                            return false;
                        }
                        return true;
                    });
            return;
        }

        Rect pageRect = mPaginationModel.getPageLocation(pageNum, mPaginatedView.getViewArea());

        int x = pageRect.left + (pageRect.width() / 2);
        int y = pageRect.top + (pageRect.height() / 2);
        float zoom =
                ZoomUtils.calculateZoomToFit(
                        mZoomView.getViewportWidth(),
                        mZoomView.getViewportHeight(),
                        pageRect.width(),
                        pageRect.height());

        mZoomView.setZoom(zoom);
        mZoomView.centerAt(x, y);
    }
}
