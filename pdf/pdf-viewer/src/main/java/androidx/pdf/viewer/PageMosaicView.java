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
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.LinkRects;
import androidx.pdf.util.Accessibility;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.widget.MosaicView;

/**
 * Renders one Page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("UnusedVariable")
public class PageMosaicView extends MosaicView implements PageViewFactory.PageView {

    private static final String SEARCH_OVERLAY_KEY = "SearchOverlayKey";

    private final int mPageNum;
    private String mPageText;
    private LinkRects mUrlLinks;

    public PageMosaicView(
            Context context,
            int pageNum,
            Dimensions pageSize,
            BitmapSource bitmapSource,
            BitmapRecycler bitmapRecycler) {
        super(context);
        this.mPageNum = pageNum;
        init(pageSize, bitmapRecycler, bitmapSource);
        setId(pageNum);
        setPageText(null);
        setFocusableInTouchMode(true);
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

    /** Set page text and content description. */
    public void setPageText(@Nullable String pageText) {
        this.mPageText = pageText;
        String description =
                (pageText != null)
                        ? pageText
                        : getContext()
                                .getString(androidx.pdf.R.string.desc_page, (mPageNum + 1));
        setContentDescription(description);
    }

    /** Returns true if we have data about any links on the page. */
    public boolean hasPageUrlLinks() {
        return mUrlLinks != null;
    }

    /** Return the URL corresponding to the given point. */
    @Nullable
    public String getLinkUrl(Point p) {
        return (mUrlLinks != null) ? mUrlLinks.getUrlAtPoint(p.x, p.y) : null;
    }

    @Override
    public void clearAll() {
        clearAllBitmaps();
        setOverlay(null);
        setPageText(null);
    }

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
    public View asView() {
        return this;
    }
}
