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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.ViewState;
import androidx.pdf.util.ThreadUtils;
import androidx.pdf.viewer.loader.PdfLoader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LayoutHandler {
    /** Only interact with Queue on the main thread. */
    private final List<OnDimensCallback> mDimensCallbackQueue;
    private final PdfLoader mPdfLoader;

    /** The number of pages that have been laid out in the document. */
    private int mPageLayoutReach = 0;
    private int mInitialPageLayoutReach = 4;

    public LayoutHandler(@NonNull PdfLoader pdfLoader) {
        mDimensCallbackQueue = new ArrayList<>();
        mPdfLoader = pdfLoader;
    }

    public int getPageLayoutReach() {
        return mPageLayoutReach;
    }

    public void setPageLayoutReach(int pageLayoutReach) {
        mPageLayoutReach = pageLayoutReach;
    }

    public int getInitialPageLayoutReach() {
        return mInitialPageLayoutReach;
    }

    public void setInitialPageLayoutReach(int initialPageLayoutReach) {
        mInitialPageLayoutReach = initialPageLayoutReach;
    }

    public void setInitialPageLayoutReachWithMax(int layoutReach) {
        mInitialPageLayoutReach = Math.max(mInitialPageLayoutReach, layoutReach);
    }

    /**
     * Lay out some pages up to some distant page. Not guaranteed to lay out any pages: maybe all
     * pages, or at least enough pages, are already laid out.
     */
    public void maybeLayoutPages(int current) {
        int peekAhead = Math.min(current + 2, 100);
        int distantPage = Math.max(current + peekAhead, mInitialPageLayoutReach);
        layoutPages(distantPage);
    }

    /**
     * Lays out all the pages until {@code untilPage}, or equivalently so that {@code untilPage}s
     * are laid out. So calling with {@code untilPage = 10} will ensure pages 0-9 are laid out.
     *
     * @param untilPage The upper limit of the range of pages to be laid out. Cropped to the
     *                  number of pages of the document if this number was larger.
     */
    public void layoutPages(int untilPage) {
        if (mPdfLoader == null) {
            return;
        }
        int lastPage = Math.min(untilPage, mPdfLoader.getNumPages());
        int requestLayoutPage = mPageLayoutReach;
        while (requestLayoutPage < lastPage) {
            mPdfLoader.loadPageDimensions(requestLayoutPage);
            requestLayoutPage++;
        }
    }

    /** */
    public void add(@NonNull OnDimensCallback callback) {
        mDimensCallbackQueue.add(callback);
    }

    /** */
    public void processCallbacksInQueue(@NonNull ViewState viewState,
            int pageNum) {
        ThreadUtils.postOnUiThread(
                () -> {
                    if (mDimensCallbackQueue.isEmpty()
                            || viewState == ViewState.NO_VIEW) {
                        return;
                    }

                    Iterator<OnDimensCallback> iterator =
                            mDimensCallbackQueue.iterator();
                    while (iterator.hasNext()) {
                        OnDimensCallback callback = iterator.next();
                        boolean shouldKeep = callback.onDimensLoaded(pageNum);
                        if (!shouldKeep) {
                            iterator.remove();
                        }
                    }
                });
    }

    /** Callback is called everytime dimensions for a page have loaded. */
    public interface OnDimensCallback {
        /** Return true to continue receiving callbacks, else false. */
        boolean onDimensLoaded(int pageNum);
    }
}
