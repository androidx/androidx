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
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.pdf.R;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.util.ExternalLinks;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.widget.ZoomView;

import java.util.List;
import java.util.Objects;

/**
 * A transparent container for virtual views representing clickable links within the Page. NOTE:
 * This ViewGroup must not have any views as children. Views cannot have both real and virtual
 * children.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class PageLinksView extends LinearLayout {
    private static final String TAG = PageLinksView.class.getSimpleName();

    private static final Rect DUMMY_RECT = new Rect(0, 0, 1, 1);

    @Nullable
    private LinkRects mUrlLinks;
    private List<GotoLink> mGotoLinks;
    private final ObservableValue<ZoomView.ZoomScroll> mZoomScroll;
    private ExploreByTouchHelper mTouchHelper;

    public PageLinksView(@NonNull Context context,
            @NonNull ObservableValue<ZoomView.ZoomScroll> zoomScroll) {
        super(context);
        setLayoutParams(
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        this.mZoomScroll = zoomScroll;
        setWillNotDraw(true);
        setFocusableInTouchMode(false);
    }

    /** Set page URL links. */
    public void setPageUrlLinks(@Nullable LinkRects links) {
        this.mUrlLinks = links;
        if (links != null && !links.isEmpty() && mTouchHelper == null) {
            this.mTouchHelper = new PageTouchHelper();
            ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        }
    }

    /** Set page goto links. */
    public void setPageGotoLinks(@Nullable List<GotoLink> links) {
        mGotoLinks = links;
        if (links != null && !links.isEmpty() && mTouchHelper == null) {
            this.mTouchHelper = new PageTouchHelper();
            ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        }
    }

    /** Reset page URL links to null. */
    public void clearAll() {
        setPageUrlLinks(null);
        mGotoLinks = null;
    }

    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        if (mTouchHelper != null && mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public void addView(View view) {
        throw new RuntimeException("Child views are not supported - this is a virtual view parent");
    }

    /**
     * This class provides accessible focusable and clickable virtual views of the links on the
     * page,
     * using the data stored in {@link PageLinksView#mUrlLinks} when it is loaded.
     */
    private class PageTouchHelper extends ExploreByTouchHelper {

        PageTouchHelper() {
            super(PageLinksView.this);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            // These co-ordinates are automatically scaled for us according to the scale of the
            // PageView that is being touched, so there is no need to scale them ourselves.
            int linkSize = mUrlLinks != null ? mUrlLinks.size() : 0;
            if (mUrlLinks != null) {
                for (int i = 0; i < linkSize; i++) {
                    for (Rect rect : mUrlLinks.get(i)) {
                        if (rect.contains((int) x, (int) y)) {
                            return i;
                        }
                    }
                }
            }
            return getVirtualViewForGotoLink(x, y, linkSize);
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            int linkSize = mUrlLinks != null ? mUrlLinks.size() : 0;
            if (mUrlLinks != null) {
                for (int i = 0; i < mUrlLinks.size(); i++) {
                    virtualViewIds.add(i);
                }
            }
            if (mGotoLinks != null) {
                for (int i = 0; i < mGotoLinks.size(); i++) {
                    virtualViewIds.add((linkSize - 1) + i);
                }
            }
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action,
                Bundle args) {
            // These virtual views don't directly handle any actions, so this is never called.
            return false;
        }

        @Override
        protected void onPopulateEventForVirtualView(int virtualViewId,
                @NonNull AccessibilityEvent event) {
            if (!isLinkLoaded(virtualViewId)) {
                // This field should always be filled to keep the Accessibility framework happy.
                event.setContentDescription("");
                return;
            }

            event.setContentDescription(getContentDescription(virtualViewId));
        }

        @Override
        protected void onPopulateNodeForVirtualView(
                int virtualViewId, @NonNull AccessibilityNodeInfoCompat node) {
            if (!isLinkLoaded(virtualViewId)) {
                // These fields should always be filled to keep the Accessibility framework happy.
                node.setContentDescription("");
                node.setBoundsInParent(DUMMY_RECT);
                return;
            }

            node.setContentDescription(getContentDescription(virtualViewId));
            node.setFocusable(true);

            int linkSize = mUrlLinks != null ? mUrlLinks.size() : 0;
            int gotoLinksSize = mGotoLinks != null ? mGotoLinks.size() : 0;
            Rect bounds = null;
            if (virtualViewId < linkSize) {
                bounds = new Rect(mUrlLinks.get(virtualViewId).get(0));
            } else if (virtualViewId < linkSize + gotoLinksSize) {
                // TODO: Add list handling instead of taking its first element
                bounds = mGotoLinks.get(virtualViewId - linkSize).getBounds().get(0);
            }

            if (bounds != null) {
                // The AccessibilityNodeInfo isn't automatically scaled by the scaling of the View
                // it is part of, so we have to do that ourselves - in contrast to
                // #getVirtualViewAt.
                float zoom = Objects.requireNonNull(mZoomScroll.get()).zoom;

                // Explicitly cast to int after scaling
                bounds.top = (int) (bounds.top * zoom);
                bounds.bottom = (int) (bounds.bottom * zoom);
                bounds.left = (int) (bounds.left * zoom);
                bounds.right = (int) (bounds.right * zoom);

                node.setBoundsInParent(bounds);
            }
        }

        private boolean isLinkLoaded(int virtualViewId) {
            // Links can be deleted as we unload pages as the user scrolls around - if this
            // happens but an event for the link somehow happens afterward, we should ignore it
            // and try not to crash. Also, the accessibility framework sometimes requests links
            // that don't exist - eg it requests the virtual view at Integer.MAX_VALUE
            int linkSize = mUrlLinks != null ? mUrlLinks.size() : 0;
            int gotoLinksSize = mGotoLinks != null ? mGotoLinks.size() : 0;
            return virtualViewId >= 0 && virtualViewId < linkSize + gotoLinksSize;
        }

        private String getContentDescription(int virtualViewId) {
            int linkSize = mUrlLinks != null ? mUrlLinks.size() : 0;
            int gotoLinksSize = mGotoLinks != null ? mGotoLinks.size() : 0;
            if (virtualViewId < linkSize) {
                return ExternalLinks.getDescription(mUrlLinks.getUrl(virtualViewId), getContext());
            } else if (virtualViewId < linkSize + gotoLinksSize) {
                int pageNum = mGotoLinks.get(
                        virtualViewId - linkSize).getDestination().getPageNumber();
                return getContext().getString(R.string.desc_goto_link, pageNum);
            }
            return "";
        }

        private int getVirtualViewForGotoLink(float x, float y, int linkSize) {
            if (mGotoLinks != null) {
                for (int i = 0; i < mGotoLinks.size(); i++) {
                    GotoLink gotoLink = mGotoLinks.get(i);
                    if (gotoLink.getBounds() != null) {
                        // TODO: Add list handling instead of taking its first element
                        Rect rect = gotoLink.getBounds().get(0);
                        if (rect.contains((int) x, (int) y)) {
                            return (linkSize - 1) + i;
                        }
                    }
                }
            }
            return INVALID_ID;
        }
    }
}
