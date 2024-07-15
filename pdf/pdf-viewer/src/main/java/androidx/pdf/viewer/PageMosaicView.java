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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.GotoLinkDestination;
import androidx.pdf.models.LinkRects;
import androidx.pdf.util.Accessibility;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.MosaicView;

import java.util.List;

/**
 * Renders one Page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PageMosaicView extends MosaicView implements PageViewFactory.PageView {

    @VisibleForTesting
    public static final String SEARCH_OVERLAY_KEY = "SearchOverlayKey";

    private final int mPageNum;
    private String mPageText;
    private LinkRects mUrlLinks;
    private List<GotoLink> mGotoLinks;
    private final PdfLoader mPdfLoader;
    private final PdfSelectionModel mSelectionModel;
    private final SearchModel mSearchModel;
    private final PdfSelectionHandles mSelectionHandles;

    public PageMosaicView(
            @NonNull Context context,
            int pageNum,
            @NonNull Dimensions pageSize,
            @NonNull BitmapSource bitmapSource,
            @Nullable BitmapRecycler bitmapRecycler,
            @NonNull PdfLoader pdfLoader,
            @NonNull PdfSelectionModel selectionModel,
            @NonNull SearchModel searchModel,
            @NonNull PdfSelectionHandles selectionHandles) {
        super(context);
        this.mPageNum = pageNum;
        init(pageSize, bitmapRecycler, bitmapSource);
        setId(pageNum);
        setPageText(null);
        setFocusableInTouchMode(true);
        this.mPdfLoader = pdfLoader;
        this.mSelectionModel = selectionModel;
        this.mSearchModel = searchModel;
        this.mSelectionHandles = selectionHandles;
    }

    /** Set the given overlay. */
    public void setOverlay(@Nullable Drawable overlay) {
        if (overlay != null) {
            super.addOverlay(SEARCH_OVERLAY_KEY, overlay);
        } else {
            super.removeOverlay(SEARCH_OVERLAY_KEY);
        }
    }

    /** Check if SEARCH_OVERLAY_KEY has an overlay. */
    public boolean hasOverlay() {
        return super.hasOverlay(SEARCH_OVERLAY_KEY);
    }

    /** Check if page text is needed. */
    public boolean needsPageText() {
        return mPageText == null && Accessibility.get().isAccessibilityEnabled(getContext());
    }

    @Nullable
    public String getPageText() {
        return this.mPageText;
    }

    /** Set page text and content description. */
    public void setPageText(@Nullable String pageText) {
        this.mPageText = pageText;
        setContentDescription(buildContentDescription(pageText, mPageNum));
    }

    @NonNull
    protected String buildContentDescription(@Nullable String pageText, int pageNum) {
        return (pageText != null)
                ? pageText
                : getContext()
                        .getString(androidx.pdf.R.string.desc_page, (mPageNum + 1));
    }

    /** Returns true if we have data about any links on the page. */
    public boolean hasPageUrlLinks() {
        return mUrlLinks != null;
    }

    /** Returns true if we have data about any internal links on the page. */
    public boolean hasPageGotoLinks() {
        return mGotoLinks != null && !mGotoLinks.isEmpty();
    }

    /** Return the URL corresponding to the given point. */
    @Nullable
    public String getLinkUrl(@NonNull Point p) {
        return (mUrlLinks != null) ? mUrlLinks.getUrlAtPoint(p.x, p.y) : null;
    }

    /** Return the goto link corresponding to the given point. */
    @Nullable
    public GotoLinkDestination getGotoDestination(@NonNull Point p) {
        if (mGotoLinks != null) {
            for (GotoLink link : mGotoLinks) {
                if (link.getBounds() != null) {
                    // TODO: Add list handling instead of taking its first element
                    Rect rect = link.getBounds().get(0);
                    if (rect.contains(p.x, p.y)) {
                        return link.getDestination();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void clearAll() {
        clearAllBitmaps();
        setOverlay(null);
        setPageText(null);
    }

    @NonNull
    @Override
    public PageMosaicView getPageView() {
        return this;
    }

    @Override
    public int getPageNum() {
        return mPageNum;
    }

    @Override
    public void setPageUrlLinks(@Nullable LinkRects links) {
        this.mUrlLinks = links;
    }

    @Override
    public void setPageGotoLinks(@Nullable List<GotoLink> links) {
        mGotoLinks = links;
    }

    @NonNull
    @Override
    public View asView() {
        return this;
    }

    /**
     * Loads the page content like page text, external urls and goto links and also resets the
     * overlays from selection and search
     */
    public void refreshPageContentAndOverlays() {
        loadPageComponents();
        resetOverlays();
    }

    /** Loads the page text, external links and the goto links for the page */
    private void loadPageComponents() {
        if (needsPageText()) {
            mPdfLoader.loadPageText(mPageNum);
        }
        if (!hasPageUrlLinks()) {
            mPdfLoader.loadPageUrlLinks(mPageNum);
        }
        if (!hasPageGotoLinks()) {
            mPdfLoader.loadPageGotoLinks(mPageNum);
        }
    }

    private void resetOverlays() {
        if (getPageNum() == mSelectionModel.getPage()) {
            setOverlay(new PdfHighlightOverlay(mSelectionModel.selection().get()));
            mSelectionHandles.updateHandles();
        } else if (mSearchModel.query().get() != null) {
            if (!hasOverlay()) {
                mPdfLoader.searchPageText(getPageNum(), mSearchModel.query().get());
            }
        } else {
            setOverlay(null);
        }
    }
}
