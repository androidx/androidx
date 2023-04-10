/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.core.widgets;

import androidx.constraintlayout.core.widgets.analyzer.Grouping;
import androidx.constraintlayout.core.widgets.analyzer.WidgetGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * HelperWidget class
 */
public class HelperWidget extends ConstraintWidget implements Helper {
    public ConstraintWidget[] mWidgets = new ConstraintWidget[4];
    public int mWidgetsCount = 0;

    @Override
    public void updateConstraints(ConstraintWidgetContainer container) {
        // nothing here
    }

    /**
     * Add a widget to the helper
     *
     * @param widget a widget
     */
    @Override
    public void add(ConstraintWidget widget) {
        if (widget == this || widget == null) {
            return;
        }
        if (mWidgetsCount + 1 > mWidgets.length) {
            mWidgets = Arrays.copyOf(mWidgets, mWidgets.length * 2);
        }
        mWidgets[mWidgetsCount] = widget;
        mWidgetsCount++;
    }

    @Override
    public void copy(ConstraintWidget src, HashMap<ConstraintWidget, ConstraintWidget> map) {
        super.copy(src, map);
        HelperWidget srcHelper = (HelperWidget) src;
        mWidgetsCount = 0;
        final int count = srcHelper.mWidgetsCount;
        for (int i = 0; i < count; i++) {
            add(map.get(srcHelper.mWidgets[i]));
        }
    }

    /**
     * Reset the widgets list contained by this helper
     */
    @Override
    public void removeAllIds() {
        mWidgetsCount = 0;
        Arrays.fill(mWidgets, null);
    }

    // @TODO: add description
    public void addDependents(ArrayList<WidgetGroup> dependencyLists,
            int orientation,
            WidgetGroup group) {
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            group.add(widget);
        }
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            Grouping.findDependents(widget, orientation, dependencyLists, group);
        }
    }

    // @TODO: add description
    public int findGroupInDependents(int orientation) {
        for (int i = 0; i < mWidgetsCount; i++) {
            ConstraintWidget widget = mWidgets[i];
            if (orientation == HORIZONTAL && widget.horizontalGroup != -1) {
                return widget.horizontalGroup;
            }
            if (orientation == VERTICAL && widget.verticalGroup != -1) {
                return widget.verticalGroup;
            }
        }
        return -1;
    }
}
