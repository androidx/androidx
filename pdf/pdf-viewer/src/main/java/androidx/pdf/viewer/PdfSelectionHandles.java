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

import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.R;
import androidx.pdf.models.PageSelection;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.select.SelectionActionMode;
import androidx.pdf.select.SelectionModel;
import androidx.pdf.util.Preconditions;
import androidx.pdf.widget.ZoomView;
import androidx.pdf.widget.ZoomableSelectionHandles;

/**
 * Implementation of SelectionHandles for PdfViewer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfSelectionHandles extends ZoomableSelectionHandles<PageSelection> {

    private final SelectionModel<?> mSelectionModel;
    private final PaginatedView mPdfView;

    private SelectionBoundary mFixed;
    private SelectionBoundary mDragging;

    private SelectionActionMode mSelectionActionMode;

    public PdfSelectionHandles(
            @NonNull PdfSelectionModel selectionModel, @NonNull ZoomView zoomView,
            @NonNull PaginatedView pdfView,
            @NonNull SelectionActionMode selectionActionMode) {
        super(
                zoomView, (ViewGroup) zoomView.findViewById(R.id.zoomed_view),
                selectionModel.selection());
        this.mSelectionModel = Preconditions.checkNotNull(selectionModel);
        this.mPdfView = Preconditions.checkNotNull(pdfView);
        this.mSelectionActionMode = selectionActionMode;
    }

    @Override
    protected void updateHandles() {
        if (mSelection == null || mPdfView.getViewAt(mSelection.getPage()) == null) {
            hideHandles();
        } else {
            View pageView = mPdfView.getViewAt(mSelection.getPage()).asView();
            showHandle(mStartHandle, pageView, mSelection.getStart(), false);
            showHandle(mStopHandle, pageView, mSelection.getStop(), true);
        }
    }

    private void showHandle(
            ImageView handle, View pageView, SelectionBoundary boundary, boolean isStop) {
        float rawX = pageView.getX() + boundary.getX();
        float rawY = pageView.getY() + boundary.getY();
        boolean isRight = isStop ^ boundary.isRtl();
        super.showHandle(handle, rawX, rawY, isRight);
    }

    @Override
    protected void onDragHandleDown(boolean isStopHandle) {
        mDragging = isStopHandle ? mSelection.getStop() : mSelection.getStart();
        mFixed = isStopHandle ? mSelection.getStart() : mSelection.getStop();
        mPdfView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
    }

    @Override
    protected void onDragHandleMove(int deltaX, int deltaY) {
        mSelectionActionMode.stopActionMode();
        SelectionBoundary updated = SelectionBoundary.atPoint(mDragging.getX() + deltaX,
                mDragging.getY() + deltaY);
        mSelectionModel.updateSelectionAsync(mFixed, updated);
    }

    @Override
    protected void onDragHandleUp() {
        mSelectionActionMode.resume();
        mPdfView.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE);
    }

    @NonNull
    public ImageView getStartHandle() {
        return mStartHandle;
    }

    @NonNull
    public ImageView getStopHandle() {
        return mStopHandle;
    }
}
