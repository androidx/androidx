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

package androidx.pdf.widget;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.R;
import androidx.pdf.data.Range;
import androidx.pdf.util.Accessibility;

import java.util.Objects;

/**
 * A Toast-like overlay that surfaces details about the current view (mostly page number, but also
 * zoom factor) to the user. Input is accepted in raw format (e.g. pages in 0-based indexing), but
 * presented to the user in human dialect, i.e. page numbers in 1-based indexing, zoom factor in
 * percent.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PageIndicator extends ReusableToast {
    private static final int AUTO_HIDE_DELAY_MS = 1300;
    private static final String DEFAULT_PAGE_TEXT = "";

    private final Context mContext;
    private final TextView mPageNumberView;
    private final Accessibility mAccessibility;

    private int mNumPages;

    private Range mCurrentRange;

    private float mCurrentZoom;

    PageIndicator(Context context, ViewGroup container) {
        this(context, inflateView(context, container), Accessibility.get());
    }

    @VisibleForTesting
    PageIndicator(Context context, TextView pageNumberView, Accessibility accessibility) {
        super(pageNumberView);
        this.mContext = context;
        this.mAccessibility = accessibility;
        setAutoHideDelayMs(AUTO_HIDE_DELAY_MS);
        hide();
        this.mPageNumberView = pageNumberView;
    }

    public void setNumPages(int numPages) {
        this.mNumPages = numPages;
    }

    /**
     * Sets details about the current view: page range and zoom. If TalkBack is on, those details
     * will be announced.
     *
     * @param range  the page range, in usual 0-based indexing.
     * @param zoom   the zoom factor, as a number between 0 and 1.
     * @param stable indicates whether the position in the document is stable.
     * @return whether this method resulted in the pageIndicator being shown.
     */
    public boolean setRangeAndZoom(@NonNull Range range, float zoom, boolean stable) {
        boolean shown = false;

        String announceStr = null;
        if (!Objects.equals(mCurrentRange, range)) {
            String desc = getDescription(range);
            announceStr = desc;
            mPageNumberView.setText(getLabel(range));
            mPageNumberView.setContentDescription(desc);
            if (mCurrentRange != null) {
                // Do not show on the first time, only when updating
                show();
                shown = true;
            }

            mCurrentRange = range;
        }

        if (zoom != mCurrentZoom && stable) {
            // Override announcement with zoom info.
            announceStr = (announceStr != null) ? announceStr + getZoomDescription(zoom) :
                    getZoomDescription(zoom);
            mCurrentZoom = zoom;
        }
        if (announceStr != null && mAccessibility.isAccessibilityEnabled(mContext)) {
            mAccessibility.announce(mContext, mPageNumberView, announceStr);
        }

        return shown;
    }

    @NonNull
    public TextView getTextView() {
        return mPageNumberView;
    }

    private static TextView inflateView(Context context, ViewGroup container) {
        LayoutInflater.from(context).inflate(R.layout.page_indicator, container);
        return (TextView) container.findViewById(R.id.pdf_page_num);
    }

    private String getLabel(Range range) {
        Resources res = mContext.getResources();
        switch (range.length()) {
            case 0:
                return res.getString(R.string.label_page_single, range.getLast(), mNumPages);
            case 1:
                return res.getString(R.string.label_page_single, range.getFirst() + 1, mNumPages);
            default:
                return res.getString(R.string.label_page_range, range.getFirst() + 1,
                        range.getLast() + 1,
                        mNumPages);
        }
    }

    private String getDescription(Range range) {
        Resources res = mContext.getResources();
        switch (range.length()) {
            case 0:
                return res.getString(R.string.desc_page_single, range.getLast() + 1, mNumPages);
            case 1:
                return res.getString(R.string.desc_page_single, range.getFirst() + 1, mNumPages);
            default:
                return res.getString(R.string.desc_page_range, range.getFirst() + 1,
                        range.getLast() + 1,
                        mNumPages);
        }
    }

    private String getZoomDescription(float zoom) {
        Resources res = mContext.getResources();
        return res.getString(R.string.desc_zoom, Math.round(zoom * 100));
    }

    /**
     * Resets the PageIndicator to its initial state. This includes clearing the
     * displayed page range and zoom information, hiding the indicator, and clearing
     * the text content of the TextView.
     */
    public void reset() {
        // Clear current range and zoom
        mCurrentRange = null;
        mCurrentZoom = 0;

        // Hide the page indicator
        hide();

        // Clear the text content of the TextView
        mPageNumberView.setText(DEFAULT_PAGE_TEXT);
    }
}
