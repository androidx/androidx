/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.slice.widget;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;

/**
 * Utility class to track view based on relative location to the parent.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LocationBasedViewTracker implements Runnable, View.OnLayoutChangeListener {

    private static final SelectionLogic INPUT_FOCUS = new SelectionLogic() {
        @Override
        public void selectView(View view) {
            view.requestFocus();
        }
    };

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static final SelectionLogic A11Y_FOCUS = new SelectionLogic() {
        @Override
        public void selectView(View view) {
            view.performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
        }
    };

    private final Rect mFocusRect = new Rect();
    private final ViewGroup mParent;
    private final SelectionLogic mSelectionLogic;

    private LocationBasedViewTracker(ViewGroup parent, View selected,
            SelectionLogic selectionLogic) {
        mParent = parent;
        mSelectionLogic = selectionLogic;

        selected.getDrawingRect(mFocusRect);
        parent.offsetDescendantRectToMyCoords(selected, mFocusRect);

        // Request a layout pass immediately after storing the view position. This ensure that we
        // get onLayoutChange before the selected view can change as a result of user interaction.
        mParent.addOnLayoutChangeListener(this);
        mParent.requestLayout();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        mParent.removeOnLayoutChangeListener(this);
        mParent.post(this);
    }

    @Override
    public void run() {
        ArrayList<View> views = new ArrayList<>();
        mParent.addFocusables(views, View.FOCUS_FORWARD, View.FOCUSABLES_ALL);

        Rect temp = new Rect();
        int oldCloseness = Integer.MAX_VALUE;
        View closestView = null;

        for (View v : views) {
            v.getDrawingRect(temp);
            mParent.offsetDescendantRectToMyCoords(v, temp);
            if (!mFocusRect.intersect(temp)) {
                continue;
            }

            // Find closeness
            int closeness = Math.abs(mFocusRect.left - temp.left)
                    + Math.abs(mFocusRect.right - temp.right)
                    + Math.abs(mFocusRect.top - temp.top)
                    + Math.abs(mFocusRect.bottom - temp.bottom);
            if (oldCloseness > closeness) {
                oldCloseness = closeness;
                closestView = v;
            }
        }
        if (closestView != null) {
            mSelectionLogic.selectView(closestView);
        }
    }

    /**
     * Tries to preserve the input focus after the next content change
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void trackInputFocused(ViewGroup parent) {
        View focused = parent.findFocus();
        if (focused != null) {
            new LocationBasedViewTracker(parent, focused, INPUT_FOCUS);
        }
    }

    /**
     * Tries to preserve the accessibility focus after the next content change
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void trackA11yFocus(ViewGroup parent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        if (!((AccessibilityManager) parent.getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE)).isTouchExplorationEnabled()) {
            return;
        }
        ArrayList<View> children = new ArrayList<>();
        parent.addFocusables(children, View.FOCUS_FORWARD, View.FOCUSABLES_ALL);
        View focused = null;
        for (View child : children) {
            if (child.isAccessibilityFocused()) {
                focused = child;
                break;
            }
        }
        if (focused != null) {
            new LocationBasedViewTracker(parent, focused, A11Y_FOCUS);
        }
    }

    /**
     * Interface to control how a view is selected
     */
    private interface SelectionLogic {

        void selectView(View view);
    }
}
