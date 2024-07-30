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

import android.graphics.Point;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.models.SelectionBoundary;
import androidx.pdf.util.GestureTracker;
import androidx.pdf.viewer.loader.PdfLoader;

/** Gesture listener for PageView's handling of tap and long press. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PageTouchListener extends GestureTracker.GestureHandler {

    private final PageViewFactory.PageView mPageView;

    private final PdfLoader mPdfLoader;

    private final FindInFileView mFindInFileView;

    private final SingleTapHandler mSingleTapHandler;

    PageTouchListener(@NonNull PageViewFactory.PageView pageView,
            @NonNull PdfLoader pdfLoader,
            @NonNull SingleTapHandler singleTapHandler,
            @NonNull FindInFileView findInFileView) {
        this.mPageView = pageView;
        this.mPdfLoader = pdfLoader;
        this.mSingleTapHandler = singleTapHandler;
        this.mFindInFileView = findInFileView;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        mSingleTapHandler.handleSingleTapConfirmedEventOnPage(e, mPageView.getPageView());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        mFindInFileView.resetFindInFile();
        SelectionBoundary boundary =
                SelectionBoundary.atPoint(new Point((int) e.getX(), (int) e.getY()));
        mPdfLoader.selectPageText(mPageView.getPageNum(), boundary, boundary);
    }
}
