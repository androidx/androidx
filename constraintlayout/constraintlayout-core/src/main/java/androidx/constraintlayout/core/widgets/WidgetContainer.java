/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.constraintlayout.core.Cache;

import java.util.ArrayList;

/**
 * A container of ConstraintWidget
 */
public class WidgetContainer extends ConstraintWidget {
    public ArrayList<ConstraintWidget> mChildren = new ArrayList<>();

    /*-----------------------------------------------------------------------*/
    // Construction
    /*-----------------------------------------------------------------------*/

    /**
     * Default constructor
     */
    public WidgetContainer() {
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public WidgetContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public WidgetContainer(int width, int height) {
        super(width, height);
    }

    @Override
    public void reset() {
        mChildren.clear();
        super.reset();
    }

    /**
     * Add a child widget
     *
     * @param widget to add
     */
    public void add(ConstraintWidget widget) {
        mChildren.add(widget);
        if (widget.getParent() != null) {
            WidgetContainer container = (WidgetContainer) widget.getParent();
            container.remove(widget);
        }
        widget.setParent(this);
    }

    /**
     * Add multiple child widgets.
     *
     * @param widgets to add
     */
    public void add(ConstraintWidget... widgets) {
        final int count = widgets.length;
        for (int i = 0; i < count; i++) {
            add(widgets[i]);
        }
    }

    /**
     * Remove a child widget
     *
     * @param widget to remove
     */
    public void remove(ConstraintWidget widget) {
        mChildren.remove(widget);
        widget.reset();
    }

    /**
     * Access the children
     *
     * @return the array of children
     */
    public ArrayList<ConstraintWidget> getChildren() {
        return mChildren;
    }

    /**
     * Return the top-level ConstraintWidgetContainer
     *
     * @return top-level ConstraintWidgetContainer
     */
    public ConstraintWidgetContainer getRootConstraintContainer() {
        ConstraintWidget item = this;
        ConstraintWidget parent = item.getParent();
        ConstraintWidgetContainer container = null;
        if (item instanceof ConstraintWidgetContainer) {
            container = (ConstraintWidgetContainer) this;
        }
        while (parent != null) {
            item = parent;
            parent = item.getParent();
            if (item instanceof ConstraintWidgetContainer) {
                container = (ConstraintWidgetContainer) item;
            }
        }
        return container;
    }

    /*-----------------------------------------------------------------------*/
    // Overloaded methods from ConstraintWidget
    /*-----------------------------------------------------------------------*/

    /**
     * Set the offset of this widget relative to the root widget.
     * We then set the offset of our children as well.
     *
     * @param x horizontal offset
     * @param y vertical offset
     */
    @Override
    public void setOffset(int x, int y) {
        super.setOffset(x, y);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.setOffset(getRootX(), getRootY());
        }
    }

    /**
     * Function implemented by ConstraintWidgetContainer
     */
    public void layout() {
        if (mChildren == null) {
            return;
        }
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof WidgetContainer) {
                ((WidgetContainer) widget).layout();
            }
        }
    }

    @Override
    public void resetSolverVariables(Cache cache) {
        super.resetSolverVariables(cache);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.resetSolverVariables(cache);
        }
    }

    // @TODO: add description
    public void removeAllChildren() {
        mChildren.clear();
    }
}
