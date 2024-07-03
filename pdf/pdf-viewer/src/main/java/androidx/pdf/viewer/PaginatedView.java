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
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.Preconditions;
import androidx.pdf.viewer.PageViewFactory.PageView;

import java.util.AbstractList;
import java.util.List;

/**
 * View to display the PDF pages assembled in a vertical strip by {@code #model}.
 *
 * <p>{@code #model} may hold many more pages than actually fit at any time on the screen. This view
 * will operate by having just a handful of pages actually instantiated into Views at any time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("WrongCall")
public class PaginatedView extends AbstractPaginatedView {

    private static final String TAG = PaginatedView.class.getSimpleName();

    /** Maps the current child views to pages. */
    private final SparseArray<PageView> mPageViews = new SparseArray<>();

    private PdfSelectionModel mSelectionModel;

    private SearchModel mSearchModel;

    public PaginatedView(@NonNull Context context) {
        super(context);
    }

    public PaginatedView(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    public PaginatedView(@NonNull Context context, @NonNull AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @NonNull
    public PdfSelectionModel getSelectionModel() {
        return mSelectionModel;
    }

    public void setSelectionModel(
            @NonNull PdfSelectionModel selectionModel) {
        mSelectionModel = selectionModel;
    }

    @NonNull
    public SearchModel getSearchModel() {
        return mSearchModel;
    }

    public void setSearchModel(@NonNull SearchModel searchModel) {
        mSearchModel = searchModel;
    }

    /** Instantiate a page of this pageView into a child pageView. */
    public void addView(@NonNull PageView pageView) {
        int pageNum = pageView.getPageNum();
        Preconditions.checkState(pageNum < getModel().getSize(),
                "Can't add pageView for unknown page");
        mPageViews.put(pageNum, pageView);
        View view = pageView.asView();
        if (mPageViews.size() == 1) {
            super.addView(view);
        } else {
            int index = mPageViews.indexOfKey(pageNum);
            if (index < mPageViews.size() - 1) {
                super.addView(view, index);
            } else {
                super.addView(view);
            }
        }
    }

    @Override
    public void addView(View child) {
        throw new UnsupportedOperationException("Not supported - Use addPage instead");
    }

    @Override
    public void addView(View child, int width, int height) {
        throw new UnsupportedOperationException("Not supported - Use addPage instead");
    }

    @Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("Not supported - Use addPage instead");
    }

    /** Return the view of the given page number. */
    @Nullable
    public PageView getViewAt(int pageNum) {
        return mPageViews.get(pageNum);
    }

    /**
     * Returns an unmodifiable list with pages that are currently instantiated as Views.
     *
     * <p>The list is backed by this view and will likely change soon, so is only suitable for
     * immediate iteration.
     */
    @NonNull
    public List<PageMosaicView> getChildViews() {
        return new AbstractList<PageMosaicView>() {

            @Override
            public PageMosaicView get(int index) {
                return ((PageView) getChildAt(index)).getPageView();
            }

            @Override
            public int size() {
                return getChildCount();
            }
        };
    }

    /** Can only clear first or last view. */
    @Override
    public void removeViewAt(int pageNum) {
        int index = mPageViews.indexOfKey(pageNum);
        if (index < 0) {
            return;
        }

        PageView page = getViewAt(pageNum);
        if (page != null) {
            mPageViews.delete(pageNum);
            removeView(page.asView());
            page.clearAll();
        }
    }

    @Override
    public void removeAllViews() {
        for (int i = 0; i < mPageViews.size(); i++) {
            mPageViews.valueAt(i).clearAll();
        }
        super.removeAllViews();
        mPageViews.clear();
    }

    /**
     * Issues the {@link #layout} call for one child view.
     *
     * <p>The child view for page X will be laid out in bound's that match page X's size in the
     * {@link
     * PaginationModel} exactly. The child will be positioned according to {@link
     * PaginationModel#getPageLocation} which positions the page:
     *
     * <ul>
     *   <li>vertically at the given <code>top</code> coordinates (between top and top + height),
     *   <li>horizontally between <code>0</code> and {@link PaginationModel#getWidth()} if possible,
     *       in a way that maximizes the portion of that view that is visible on the screen
     * </ul>
     *
     * @param index the index of the child view in this ViewGroup
     */
    @Override
    protected void layoutChild(int index) {
        int pageNum = mPageViews.keyAt(index);
        Rect pageCoordinates = getModel().getPageLocation(pageNum);

        PageView child = (PageView) getChildAt(index);
        child
                .asView()
                .layout(
                        pageCoordinates.left,
                        pageCoordinates.top,
                        pageCoordinates.right,
                        pageCoordinates.bottom);

        Rect viewArea = getModel().getViewArea();
        child
                .getPageView()
                .setViewArea(
                        viewArea.left - pageCoordinates.left,
                        viewArea.top - pageCoordinates.top,
                        viewArea.right - pageCoordinates.left,
                        viewArea.bottom - pageCoordinates.top);
    }

    /** Clear all highlight overlays. */
    public void clearAllOverlays() {
        for (PageMosaicView view : getChildViews()) {
            view.setOverlay(null);
        }
    }

    /** Perform a layout when the viewArea of the {@code model} has changed. */
    @Override
    public void onViewAreaChanged() {
        // We can't wait for the next layout pass, the pages will be drawn before.
        // We could still optimize to skip the next layoutChild() calls for the pages that have been
        // laid out already for this viewArea.
        onLayout(false, getLeft(), getTop(), getRight(), getBottom());
    }
}
