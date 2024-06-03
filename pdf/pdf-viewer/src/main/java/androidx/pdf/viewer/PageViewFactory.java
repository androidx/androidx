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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.util.Accessibility;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.ObservableValue;
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
    private PageViewFactory() {
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
     * Returns a {@link PageMosaicView}, bundled together with a {@link PageLinksView} and
     * optionally a {@link FormAccessibilityView} in a {@link AccessibilityPageWrapper} if TalkBack
     * is on, otherwise returns a {@link PageMosaicView}.
     */
    @NonNull
    public static PageView createPageView(
            @NonNull Context context,
            int pageNum,
            @NonNull Dimensions pageSize,
            @NonNull MosaicView.BitmapSource bitmapSource,
            @Nullable BitmapRecycler bitmapRecycler,
            @NonNull ObservableValue<ZoomView.ZoomScroll> zoomScroll) {
        final PageMosaicView pageMosaicView =
                new PageMosaicView(context, pageNum, pageSize, bitmapSource, bitmapRecycler);
        if (Accessibility.get().isTouchExplorationEnabled(context)) {
            final PageLinksView pageLinksView = new PageLinksView(context, zoomScroll);

            return new AccessibilityPageWrapper(
                    context, pageNum, pageMosaicView, pageLinksView);
        } else {
            return pageMosaicView;
        }
    }
}
