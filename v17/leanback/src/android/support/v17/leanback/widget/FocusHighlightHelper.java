/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.graphics.drawable.TransitionDrawable;
import android.support.v17.leanback.R;
import android.view.View;
import android.content.res.Resources;

/**
 * Setup the behavior how to highlight when a item gains focus.
 */
public class FocusHighlightHelper {

    private static class BrowseItemFocusHighlight implements FocusHighlight {
        private static final int DURATION_MS = 150;

        private int[] mZoomSizeResourceId = new int[] { R.dimen.lb_browse_item_zoom_height,
                R.dimen.lb_browse_item_zoom_width };
        private float[] mZoomSizeCache;

        public BrowseItemFocusHighlight() {
            mZoomSizeCache = new float[mZoomSizeResourceId.length];
        }

        private float getScale(View view) {
            final int index = (view.getHeight() > view.getWidth() ? 0 : 1);
            if (mZoomSizeCache[index] == 0f) {
                mZoomSizeCache[index] = view.getResources()
                        .getDimension(mZoomSizeResourceId[index]);
            }
            if (index == 0) {
                final float height = view.getHeight();
                return (height + mZoomSizeCache[index]) / height;
            } else {
                final float width = view.getWidth();
                return (width + mZoomSizeCache[index]) / width;
            }
        }

        private void viewFocused(View view, boolean hasFocus) {
            if (hasFocus) {
                final float scale = getScale(view);
                view.animate().scaleX(scale).scaleY(scale).setDuration(DURATION_MS);
            } else {
                view.animate().scaleX(1f).scaleY(1f).setDuration(DURATION_MS);
            }
        }

        @Override
        public void onItemFocused(View view, Object item, boolean hasFocus) {
            viewFocused(view, hasFocus);
        }
    }

    private static BrowseItemFocusHighlight sBrowseItemFocusHighlight =
            new BrowseItemFocusHighlight();

    private static HeaderItemFocusHighlight sHeaderItemFocusHighlight =
            new HeaderItemFocusHighlight();

    private static ActionItemFocusHighlight sActionItemFocusHighlight =
            new ActionItemFocusHighlight();

    /**
     * Setup the focus highlight behavior of a focused item in browse list row.
     * @param adapter  adapter of the list row.
     */
    public static void setupBrowseItemFocusHighlight(ItemBridgeAdapter adapter) {
        adapter.setFocusHighlight(sBrowseItemFocusHighlight);
    }

    /**
     * Setup the focus highlight behavior of a focused item in header list.
     * @param adapter  adapter of the header list.
     */
    public static void setupHeaderItemFocusHighlight(ItemBridgeAdapter adapter) {
        adapter.setFocusHighlight(sHeaderItemFocusHighlight);
    }

    /**
     * Setup the focus highlight behavior of a focused item in an action list.
     * @param adapter  adapter of the action list.
     */
    public static void setupActionItemFocusHighlight(ItemBridgeAdapter adapter) {
        adapter.setFocusHighlight(sActionItemFocusHighlight);
    }

    private static class HeaderItemFocusHighlight implements FocusHighlight {
        private boolean mInitialized;
        private float mSelectScale;
        private float mUnselectAlpha;
        private int mDuration;

        private void initializeDimensions(Resources res) {
            if (!mInitialized) {
                mSelectScale =
                        Float.parseFloat(res.getString(R.dimen.lb_browse_header_select_scale));
                mUnselectAlpha =
                        Float.parseFloat(res.getString(R.dimen.lb_browse_header_unselect_alpha));
                mDuration =
                        Integer.parseInt(res.getString(R.dimen.lb_browse_header_select_duration));
                mInitialized = true;
            }
        }

        private void viewFocused(View view, boolean hasFocus) {
            initializeDimensions(view.getResources());
            if (hasFocus) {
                view.animate().scaleX(mSelectScale).scaleY(mSelectScale)
                        .alpha(1f)
                        .setDuration(mDuration);
            } else {
                view.animate().scaleX(1f).scaleY(1f)
                        .alpha(mUnselectAlpha)
                        .setDuration(mDuration);
            }
        }

        @Override
        public void onItemFocused(View view, Object item, boolean hasFocus) {
            viewFocused(view, hasFocus);
        }
    }

    private static class ActionItemFocusHighlight implements FocusHighlight {
        private boolean mInitialized;
        private int mDuration;

        private void initializeDimensions(Resources res) {
            if (!mInitialized) {
                mDuration = Integer.parseInt(res.getString(R.dimen.lb_details_overview_action_select_duration));
            }
        }

        @Override
        public void onItemFocused(View view, Object item, boolean hasFocus) {
            initializeDimensions(view.getResources());
            TransitionDrawable td = (TransitionDrawable) view.getBackground();
            if (hasFocus) {
                td.startTransition(mDuration);
            } else {
                td.reverseTransition(mDuration);
            }
        }
    }
}
