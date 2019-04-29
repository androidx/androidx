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

package androidx.viewpager2.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

import android.animation.LayoutTransition;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Class used to detect if there are gaps between pages and if any of the pages contain a running
 * change-transition in case we detected an illegal state in the {@link ScrollEventAdapter}.
 *
 * This is an approximation of the detection and could potentially lead to misleading advice. If we
 * hit problems with it, remove the detection and replace with a suggestive error message instead,
 * like "Negative page offset encountered. Did you setAnimateParentHierarchy(false) to all your
 * LayoutTransitions?".
 */
final class AnimateLayoutChangeDetector {
    private static final ViewGroup.MarginLayoutParams ZERO_MARGIN_LAYOUT_PARAMS;

    static {
        ZERO_MARGIN_LAYOUT_PARAMS = new ViewGroup.MarginLayoutParams(MATCH_PARENT, MATCH_PARENT);
        ZERO_MARGIN_LAYOUT_PARAMS.setMargins(0, 0, 0, 0);
    }

    private LinearLayoutManager mLayoutManager;

    AnimateLayoutChangeDetector(@NonNull LinearLayoutManager llm) {
        mLayoutManager = llm;
    }

    boolean mayHaveInterferingAnimations() {
        // Two conditions need to be satisfied:
        // 1) the pages are not laid out contiguously (i.e., there are gaps between them)
        // 2) there is a ViewGroup with a LayoutTransition that isChangingLayout()
        return (!arePagesLaidOutContiguously() || mLayoutManager.getChildCount() <= 1)
                && hasRunningChangingLayoutTransition();
    }

    private boolean arePagesLaidOutContiguously() {
        // Collect view positions
        int childCount = mLayoutManager.getChildCount();
        if (childCount == 0) {
            return true;
        }

        boolean isHorizontal = mLayoutManager.getOrientation() == ORIENTATION_HORIZONTAL;
        int[][] bounds = new int[childCount][2];
        for (int i = 0; i < childCount; i++) {
            View view = mLayoutManager.getChildAt(i);
            if (view == null) {
                throw new IllegalStateException("null view contained in the view hierarchy");
            }
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            ViewGroup.MarginLayoutParams margin;
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                margin = (ViewGroup.MarginLayoutParams) layoutParams;
            } else {
                margin = ZERO_MARGIN_LAYOUT_PARAMS;
            }
            bounds[i][0] = isHorizontal
                    ? view.getLeft() - margin.leftMargin
                    : view.getTop() - margin.topMargin;
            bounds[i][1] = isHorizontal
                    ? view.getRight() + margin.rightMargin
                    : view.getBottom() + margin.bottomMargin;
        }

        // Sort them
        Arrays.sort(bounds, new Comparator<int[]>() {
            @Override
            public int compare(int[] lhs, int[] rhs) {
                return lhs[0] - rhs[0];
            }
        });

        // Check for inconsistencies
        for (int i = 1; i < childCount; i++) {
            if (bounds[i - 1][1] != bounds[i][0]) {
                return false;
            }
        }

        // Check that the pages fill the whole screen
        int pageSize = bounds[0][1] - bounds[0][0];
        if (bounds[0][0] > 0 || bounds[childCount - 1][1] < pageSize) {
            return false;
        }
        return true;
    }

    private boolean hasRunningChangingLayoutTransition() {
        int childCount = mLayoutManager.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (hasRunningChangingLayoutTransition(mLayoutManager.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRunningChangingLayoutTransition(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            LayoutTransition layoutTransition = viewGroup.getLayoutTransition();
            if (layoutTransition != null && layoutTransition.isChangingLayout()) {
                return true;
            }
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (hasRunningChangingLayoutTransition(viewGroup.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
