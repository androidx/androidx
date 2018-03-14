/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.example.android.supportv4.R;

import java.util.ArrayList;
import java.util.List;

/**
 * This example shows how to use the {@link ExploreByTouchHelper} class in the
 * Android support library to add accessibility support to a custom view that
 * represents multiple logical items.
 * <p>
 * The {@link ExploreByTouchHelper} class wraps
 * {@link AccessibilityNodeProviderCompat} and simplifies exposing information
 * about a custom view's logical structure to accessibility services.
 * <p>
 * The custom view in this example is responsible for:
 * <ul>
 * <li>Creating a helper class that extends {@link ExploreByTouchHelper}
 * <li>Setting the helper as the accessibility delegate using
 * {@link ViewCompat#setAccessibilityDelegate}
 * <li>Dispatching hover events to the helper in {@link View#dispatchHoverEvent}
 * </ul>
 * <p>
 * The helper class implementation in this example is responsible for:
 * <ul>
 * <li>Mapping hover event coordinates to logical items
 * <li>Exposing information about logical items to accessibility services
 * <li>Handling accessibility actions
 * <ul>
 */
public class ExploreByTouchHelperActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore_by_touch_helper);

        final CustomView customView = findViewById(R.id.custom_view);

        // Adds an item at the top-left quarter of the custom view.
        customView.addItem(getString(R.string.sample_item_a), 0, 0, 0.5f, 0.5f);

        // Adds an item at the bottom-right quarter of the custom view.
        CustomView.CustomItem itemB =
                customView.addItem(getString(R.string.sample_item_b), 0.5f, 0.5f, 1, 1);

        // Add an item at the bottom quarter of Item B.
        CustomView.CustomItem itemC =
                customView.addItem(getString(R.string.sample_item_c), 0, 0.75f, 1, 1);
        customView.setParentItem(itemC, itemB);

        // Add an item at the left quarter of Item C.
        CustomView.CustomItem itemD =
                customView.addItem(getString(R.string.sample_item_d), 0, 0f, 0.25f, 1);
        customView.setParentItem(itemD, itemC);

        customView.layoutItems();
    }

    /**
     * Simple custom view that draws rectangular items to the screen. Each item
     * has a checked state that may be toggled by tapping on the item.
     */
    public static class CustomView extends View {
        private static final int NO_ITEM = -1;

        private final Paint mPaint = new Paint();
        private final Rect mTempBounds = new Rect();
        private final List<CustomItem> mItems = new ArrayList<CustomItem>();
        private CustomViewTouchHelper mTouchHelper;

        public CustomView(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Set up accessibility helper class.
            mTouchHelper = new CustomViewTouchHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mTouchHelper);
        }

        @Override
        public boolean dispatchHoverEvent(MotionEvent event) {
            // Always attempt to dispatch hover events to accessibility first.
            if (mTouchHelper.dispatchHoverEvent(event)) {
                return true;
            }

            return super.dispatchHoverEvent(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    final int itemIndex = getItemIndexUnder(event.getX(), event.getY());
                    if (itemIndex >= 0) {
                        onItemClicked(itemIndex);
                    }
                    return true;
            }

            return super.onTouchEvent(event);
        }

        /**
         * Adds an item to the custom view. The item is positioned relative to
         * the custom view bounds and its descriptions is drawn at its center.
         *
         * @param description The item's description.
         * @param top Top coordinate as a fraction of the parent height, range
         *            is [0,1].
         * @param left Left coordinate as a fraction of the parent width, range
         *            is [0,1].
         * @param bottom Bottom coordinate as a fraction of the parent height,
         *            range is [0,1].
         * @param right Right coordinate as a fraction of the parent width,
         *            range is [0,1].
         */
        public CustomItem addItem(String description, float left, float top, float right,
                                  float bottom) {
            final CustomItem item = new CustomItem();
            item.mId = mItems.size();
            item.mBounds = new RectF(left, top, right, bottom);
            item.mDescription = description;
            item.mChecked = false;
            mItems.add(item);
            return item;
        }

        /**
         * Sets the parent of an CustomItem.  This adjusts the bounds so that they are relative to
         * the specified view, and initializes the parent and child info to point to each either.
         * @param item CustomItem that will become a child node.
         * @param parent CustomItem that will become the parent node.
         */
        public void setParentItem(CustomItem item, CustomItem parent) {
            item.mParent = parent;
            parent.mChildren.add(item.mId);
        }

        /**
         * Walk the view hierarchy of each item and calculate mBoundsInRoot.
         */
        public void layoutItems() {
            for (CustomItem item : mItems) {
                layoutItem(item);
            }
        }

        void layoutItem(CustomItem item) {
            item.mBoundsInRoot = new RectF(item.mBounds);
            CustomItem parent = item.mParent;
            while (parent != null) {
                RectF bounds = item.mBoundsInRoot;
                item.mBoundsInRoot.set(parent.mBounds.left + bounds.left * parent.mBounds.width(),
                        parent.mBounds.top + bounds.top * parent.mBounds.height(),
                        parent.mBounds.left + bounds.right * parent.mBounds.width(),
                        parent.mBounds.top + bounds.bottom * parent.mBounds.height());
                parent = parent.mParent;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            final Paint paint = mPaint;
            final Rect bounds = mTempBounds;
            final int height = getHeight();
            final int width = getWidth();

            for (CustomItem item : mItems) {
                if (item.mParent == null) {
                    paint.setColor(item.mChecked ? Color.RED : Color.BLUE);
                } else {
                    paint.setColor(item.mChecked ? Color.MAGENTA : Color.GREEN);
                }
                paint.setStyle(Style.FILL);
                scaleRectF(item.mBoundsInRoot, bounds, width, height);
                canvas.drawRect(bounds, paint);
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Align.CENTER);
                canvas.drawText(item.mDescription, bounds.centerX(), bounds.centerY(), paint);
            }
        }

        protected boolean onItemClicked(int index) {
            final CustomItem item = getItem(index);
            if (item == null) {
                return false;
            }

            item.mChecked = !item.mChecked;
            invalidate();

            // Since the item's checked state is exposed to accessibility
            // services through its AccessibilityNodeInfo, we need to invalidate
            // the item's virtual view. At some point in the future, the
            // framework will obtain an updated version of the virtual view.
            mTouchHelper.invalidateVirtualView(index);

            // We also need to let the framework know what type of event
            // happened. Accessibility services may use this event to provide
            // appropriate feedback to the user.
            mTouchHelper.sendEventForVirtualView(index, AccessibilityEvent.TYPE_VIEW_CLICKED);

            return true;
        }

        protected int getItemIndexUnder(float x, float y) {
            final float scaledX = (x / getWidth());
            final float scaledY = (y / getHeight());
            final int n = mItems.size();

            // Search in reverse order, so that topmost items are selected first.
            for (int i = n - 1; i >= 0; i--) {
                final CustomItem item = mItems.get(i);
                if (item.mBoundsInRoot.contains(scaledX, scaledY)) {
                    return i;
                }
            }

            return NO_ITEM;
        }

        protected CustomItem getItem(int index) {
            if ((index < 0) || (index >= mItems.size())) {
                return null;
            }

            return mItems.get(index);
        }

        protected static void scaleRectF(RectF in, Rect out, int width, int height) {
            out.top = (int) (in.top * height);
            out.bottom = (int) (in.bottom * height);
            out.left = (int) (in.left * width);
            out.right = (int) (in.right * width);
        }

        private class CustomViewTouchHelper extends ExploreByTouchHelper {
            private final Rect mTempRect = new Rect();

            public CustomViewTouchHelper(View forView) {
                super(forView);
            }

            @Override
            protected int getVirtualViewAt(float x, float y) {
                // We also perform hit detection in onTouchEvent(), and we can
                // reuse that logic here. This will ensure consistency whether
                // accessibility is on or off.
                final int index = getItemIndexUnder(x, y);
                if (index == NO_ITEM) {
                    return ExploreByTouchHelper.INVALID_ID;
                }

                return index;
            }

            @Override
            protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
                // Since every item should be visible, and since we're mapping
                // directly from item index to virtual view id, we can add
                // the index of every view that doesn't have a parent.
                final int n = mItems.size();
                for (int i = 0; i < n; i++) {
                    if (mItems.get(i).mParent == null) {
                        virtualViewIds.add(i);
                    }
                }
            }

            @Override
            protected void onPopulateEventForVirtualView(
                    int virtualViewId, AccessibilityEvent event) {
                final CustomItem item = getItem(virtualViewId);
                if (item == null) {
                    throw new IllegalArgumentException("Invalid virtual view id");
                }

                // The event must be populated with text, either using
                // getText().add() or setContentDescription(). Since the item's
                // description is displayed visually, we'll add it to the event
                // text. If it was only used for accessibility, we would use
                // setContentDescription().
                event.getText().add(item.mDescription);
            }

            @Override
            protected void onPopulateNodeForVirtualView(
                    int virtualViewId, AccessibilityNodeInfoCompat node) {
                final CustomItem item = getItem(virtualViewId);
                if (item == null) {
                    throw new IllegalArgumentException("Invalid virtual view id");
                }

                // Node and event text and content descriptions are usually
                // identical, so we'll use the exact same string as before.
                node.setText(item.mDescription);

                // Reported bounds should be consistent with those used to draw
                // the item in onDraw(). They should also be consistent with the
                // hit detection performed in getVirtualViewAt() and
                // onTouchEvent().
                final Rect bounds = mTempRect;
                int height = getHeight();
                int width = getWidth();
                if (item.mParent != null) {
                    width = (int) (width * item.mParent.mBoundsInRoot.width());
                    height = (int) (height * item.mParent.mBoundsInRoot.height());
                }
                scaleRectF(item.mBounds, bounds, width, height);
                node.setBoundsInParent(bounds);

                // Since the user can tap an item, add the CLICK action. We'll
                // need to handle this later in onPerformActionForVirtualView.
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

                // This item has a checked state.
                node.setCheckable(true);
                node.setChecked(item.mChecked);

                // Setup the hierarchy.
                if (item.mParent != null) {
                    node.setParent(CustomView.this, item.mParent.mId);
                }
                for (Integer id : item.mChildren) {
                    node.addChild(CustomView.this, id);
                }
            }

            @Override
            protected boolean onPerformActionForVirtualView(
                    int virtualViewId, int action, Bundle arguments) {
                switch (action) {
                    case AccessibilityNodeInfoCompat.ACTION_CLICK:
                        // Click handling should be consistent with
                        // onTouchEvent(). This ensures that the view works the
                        // same whether accessibility is turned on or off.
                        return onItemClicked(virtualViewId);
                }

                return false;
            }

        }

        public static class CustomItem {
            private int mId;
            private CustomItem mParent;
            private List<Integer> mChildren = new ArrayList<>();
            private String mDescription;
            private RectF mBounds;
            private RectF mBoundsInRoot;
            private boolean mChecked;
        }
    }
}
