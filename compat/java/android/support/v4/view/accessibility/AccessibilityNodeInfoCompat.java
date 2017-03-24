/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.support.v4.view.accessibility;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeInfo} in a backwards
 * compatible fashion.
 */
public class AccessibilityNodeInfoCompat {

    public static class AccessibilityActionCompat {

        /**
         * Action that gives input focus to the node.
         */
        public static final AccessibilityActionCompat ACTION_FOCUS =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_FOCUS, null);

        /**
         * Action that clears input focus of the node.
         */
        public static final AccessibilityActionCompat ACTION_CLEAR_FOCUS =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS, null);

        /**
         *  Action that selects the node.
         */
        public static final AccessibilityActionCompat ACTION_SELECT =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SELECT, null);

        /**
         * Action that deselects the node.
         */
        public static final AccessibilityActionCompat ACTION_CLEAR_SELECTION =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION, null);

        /**
         * Action that clicks on the node info.
         */
        public static final AccessibilityActionCompat ACTION_CLICK =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK, null);

        /**
         * Action that long clicks on the node.
         */
        public static final AccessibilityActionCompat ACTION_LONG_CLICK =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, null);

        /**
         * Action that gives accessibility focus to the node.
         */
        public static final AccessibilityActionCompat ACTION_ACCESSIBILITY_FOCUS =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS, null);

        /**
         * Action that clears accessibility focus of the node.
         */
        public static final AccessibilityActionCompat ACTION_CLEAR_ACCESSIBILITY_FOCUS =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);

        /**
         * Action that requests to go to the next entity in this node's text
         * at a given movement granularity. For example, move to the next character,
         * word, etc.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT},
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN}<br>
         * <strong>Example:</strong> Move to the previous character and do not extend selection.
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
         *           AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER);
         *   arguments.putBoolean(
         *           AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false);
         *   info.performAction(
         *           AccessibilityActionCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY.getId(),
         *           arguments);
         * </code></pre></p>
         * </p>
         *
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *
         * @see AccessibilityNodeInfoCompat#setMovementGranularities(int)
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         * @see AccessibilityNodeInfoCompat#getMovementGranularities()
         *  AccessibilityNodeInfoCompat.getMovementGranularities()
         *
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_CHARACTER
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_WORD
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_LINE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PARAGRAPH
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PAGE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
         */
        public static final AccessibilityActionCompat ACTION_NEXT_AT_MOVEMENT_GRANULARITY =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null);

        /**
         * Action that requests to go to the previous entity in this node's text
         * at a given movement granularity. For example, move to the next character,
         * word, etc.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT},
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN}<br>
         * <strong>Example:</strong> Move to the next character and do not extend selection.
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
         *           AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER);
         *   arguments.putBoolean(
         *           AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false);
         *   info.performAction(
         *           AccessibilityActionCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY.getId(),
         *           arguments);
         * </code></pre></p>
         * </p>
         *
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
         *
         * @see AccessibilityNodeInfoCompat#setMovementGranularities(int)
         *   AccessibilityNodeInfoCompat.setMovementGranularities(int)
         * @see AccessibilityNodeInfoCompat#getMovementGranularities()
         *  AccessibilityNodeInfoCompat.getMovementGranularities()
         *
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_CHARACTER
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_WORD
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_LINE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PARAGRAPH
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
         * @see AccessibilityNodeInfoCompat#MOVEMENT_GRANULARITY_PAGE
         *  AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
         */
        public static final AccessibilityActionCompat ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null);

        /**
         * Action to move to the next HTML element of a given type. For example, move
         * to the BUTTON, INPUT, TABLE, etc.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_HTML_ELEMENT_STRING
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING}<br>
         * <strong>Example:</strong>
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putString(
         *           AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING, "BUTTON");
         *   info.performAction(
         *           AccessibilityActionCompat.ACTION_NEXT_HTML_ELEMENT.getId(), arguments);
         * </code></pre></p>
         * </p>
         */
        public static final AccessibilityActionCompat ACTION_NEXT_HTML_ELEMENT =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT, null);

        /**
         * Action to move to the previous HTML element of a given type. For example, move
         * to the BUTTON, INPUT, TABLE, etc.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_HTML_ELEMENT_STRING
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING}<br>
         * <strong>Example:</strong>
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putString(
         *           AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING, "BUTTON");
         *   info.performAction(
         *           AccessibilityActionCompat.ACTION_PREVIOUS_HTML_ELEMENT.getId(), arguments);
         * </code></pre></p>
         * </p>
         */
        public static final AccessibilityActionCompat ACTION_PREVIOUS_HTML_ELEMENT =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT, null);

        /**
         * Action to scroll the node content forward.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_FORWARD =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, null);

        /**
         * Action to scroll the node content backward.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_BACKWARD =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, null);

        /**
         * Action to copy the current selection to the clipboard.
         */
        public static final AccessibilityActionCompat ACTION_COPY =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_COPY, null);

        /**
         * Action to paste the current clipboard content.
         */
        public static final AccessibilityActionCompat ACTION_PASTE =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_PASTE, null);

        /**
         * Action to cut the current selection and place it to the clipboard.
         */
        public static final AccessibilityActionCompat ACTION_CUT =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CUT, null);

        /**
         * Action to set the selection. Performing this action with no arguments
         * clears the selection.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_SELECTION_START_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT},
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_SELECTION_END_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT}<br>
         * <strong>Example:</strong>
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, 1);
         *   arguments.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, 2);
         *   info.performAction(AccessibilityActionCompat.ACTION_SET_SELECTION.getId(), arguments);
         * </code></pre></p>
         * </p>
         *
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_SELECTION_START_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT
         * @see AccessibilityNodeInfoCompat#ACTION_ARGUMENT_SELECTION_END_INT
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT
         */
        public static final AccessibilityActionCompat ACTION_SET_SELECTION =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, null);

        /**
         * Action to expand an expandable node.
         */
        public static final AccessibilityActionCompat ACTION_EXPAND =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_EXPAND, null);

        /**
         * Action to collapse an expandable node.
         */
        public static final AccessibilityActionCompat ACTION_COLLAPSE =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_COLLAPSE, null);

        /**
         * Action to dismiss a dismissable node.
         */
        public static final AccessibilityActionCompat ACTION_DISMISS =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_DISMISS, null);

        /**
         * Action that sets the text of the node. Performing the action without argument,
         * using <code> null</code> or empty {@link CharSequence} will clear the text. This
         * action will also put the cursor at the end of text.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE
         *  AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE}<br>
         * <strong>Example:</strong>
         * <code><pre><p>
         *   Bundle arguments = new Bundle();
         *   arguments.putCharSequence(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
         *       "android");
         *   info.performAction(AccessibilityActionCompat.ACTION_SET_TEXT.getId(), arguments);
         * </code></pre></p>
         */
        public static final AccessibilityActionCompat ACTION_SET_TEXT =
                new AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_TEXT, null);

        /**
         * Action that requests the node make its bounding rectangle visible
         * on the screen, scrolling if necessary just enough.
         *
         * @see View#requestRectangleOnScreen(Rect)
         */
        public static final AccessibilityActionCompat ACTION_SHOW_ON_SCREEN =
                new AccessibilityActionCompat(IMPL.getActionShowOnScreen());

        /**
         * Action that scrolls the node to make the specified collection
         * position visible on screen.
         * <p>
         * <strong>Arguments:</strong>
         * <ul>
         *     <li>{@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_ROW_INT}</li>
         *     <li>{@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_COLUMN_INT}</li>
         * <ul>
         *
         * @see AccessibilityNodeInfoCompat#getCollectionInfo()
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_TO_POSITION =
                new AccessibilityActionCompat(IMPL.getActionScrollToPosition());

        /**
         * Action to scroll the node content up.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_UP =
                new AccessibilityActionCompat(IMPL.getActionScrollUp());

        /**
         * Action to scroll the node content left.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_LEFT =
                new AccessibilityActionCompat(IMPL.getActionScrollLeft());

        /**
         * Action to scroll the node content down.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_DOWN =
                new AccessibilityActionCompat(IMPL.getActionScrollDown());

        /**
         * Action to scroll the node content right.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_RIGHT =
                new AccessibilityActionCompat(IMPL.getActionScrollRight());

        /**
         * Action that context clicks the node.
         */
        public static final AccessibilityActionCompat ACTION_CONTEXT_CLICK =
                new AccessibilityActionCompat(IMPL.getActionContextClick());

        /**
         * Action that sets progress between {@link  RangeInfoCompat#getMin() RangeInfo.getMin()} and
         * {@link  RangeInfoCompat#getMax() RangeInfo.getMax()}. It should use the same value type as
         * {@link RangeInfoCompat#getType() RangeInfo.getType()}
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_PROGRESS_VALUE}
         *
         * @see RangeInfoCompat
         */
        public static final AccessibilityActionCompat ACTION_SET_PROGRESS =
                new AccessibilityActionCompat(IMPL.getActionSetProgress());

        final Object mAction;

        /**
         * Creates a new instance.
         *
         * @param actionId The action id.
         * @param label The action label.
         */
        public AccessibilityActionCompat(int actionId, CharSequence label) {
            this(IMPL.newAccessibilityAction(actionId, label));
        }

        AccessibilityActionCompat(Object action) {
            mAction = action;
        }

        /**
         * Gets the id for this action.
         *
         * @return The action id.
         */
        public int getId() {
            return IMPL.getAccessibilityActionId(mAction);
        }

        /**
         * Gets the label for this action. Its purpose is to describe the
         * action to user.
         *
         * @return The label.
         */
        public CharSequence getLabel() {
            return IMPL.getAccessibilityActionLabel(mAction);
        }
    }

    /**
     * Class with information if a node is a collection.
     * <p>
     * A collection of items has rows and columns and may be hierarchical.
     * For example, a horizontal list is a collection with one column, as
     * many rows as the list items, and is not hierarchical; A table is a
     * collection with several rows, several columns, and is not hierarchical;
     * A vertical tree is a hierarchical collection with one column and
     * as many rows as the first level children.
     * </p>
     */
    public static class CollectionInfoCompat {
        /** Selection mode where items are not selectable. */
        public static final int SELECTION_MODE_NONE = 0;

        /** Selection mode where a single item may be selected. */
        public static final int SELECTION_MODE_SINGLE = 1;

        /** Selection mode where multiple items may be selected. */
        public static final int SELECTION_MODE_MULTIPLE = 2;

        final Object mInfo;

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowCount The number of rows.
         * @param columnCount The number of columns.
         * @param hierarchical Whether the collection is hierarchical.
         * @param selectionMode The collection's selection mode, one of:
         *            <ul>
         *            <li>{@link #SELECTION_MODE_NONE}
         *            <li>{@link #SELECTION_MODE_SINGLE}
         *            <li>{@link #SELECTION_MODE_MULTIPLE}
         *            </ul>
         *
         * @return An instance.
         */
        public static CollectionInfoCompat obtain(int rowCount, int columnCount,
                boolean hierarchical, int selectionMode) {
            return new CollectionInfoCompat(IMPL.obtainCollectionInfo(rowCount, columnCount,
                    hierarchical, selectionMode));
        }

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowCount The number of rows.
         * @param columnCount The number of columns.
         * @param hierarchical Whether the collection is hierarchical.
         *
         * @return An instance.
         */
        public static CollectionInfoCompat obtain(int rowCount, int columnCount,
                boolean hierarchical) {
            return new CollectionInfoCompat(IMPL.obtainCollectionInfo(rowCount, columnCount,
                    hierarchical));
        }

        CollectionInfoCompat(Object info) {
            mInfo = info;
        }

        /**
         * Gets the number of columns.
         *
         * @return The column count.
         */
        public int getColumnCount() {
            return IMPL.getCollectionInfoColumnCount(mInfo);
        }

        /**
         * Gets the number of rows.
         *
         * @return The row count.
         */
        public int getRowCount() {
            return IMPL.getCollectionInfoRowCount(mInfo);
        }

        /**
         * Gets if the collection is a hierarchically ordered.
         *
         * @return Whether the collection is hierarchical.
         */
        public boolean isHierarchical() {
            return IMPL.isCollectionInfoHierarchical(mInfo);
        }

        /**
         * Gets the collection's selection mode.
         *
         * @return The collection's selection mode, one of:
         *         <ul>
         *         <li>{@link #SELECTION_MODE_NONE}
         *         <li>{@link #SELECTION_MODE_SINGLE}
         *         <li>{@link #SELECTION_MODE_MULTIPLE}
         *         </ul>
         */
        public int getSelectionMode() {
            return IMPL.getCollectionInfoSelectionMode(mInfo);
        }
    }

    /**
     * Class with information if a node is a collection item.
     * <p>
     * A collection item is contained in a collection, it starts at
     * a given row and column in the collection, and spans one or
     * more rows and columns. For example, a header of two related
     * table columns starts at the first row and the first column,
     * spans one row and two columns.
     * </p>
     */
    public static class CollectionItemInfoCompat {

        final Object mInfo;

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowIndex The row index at which the item is located.
         * @param rowSpan The number of rows the item spans.
         * @param columnIndex The column index at which the item is located.
         * @param columnSpan The number of columns the item spans.
         * @param heading Whether the item is a heading.
         * @param selected Whether the item is selected.
         * @return An instance.
         */
        public static CollectionItemInfoCompat obtain(int rowIndex, int rowSpan,
                int columnIndex, int columnSpan, boolean heading, boolean selected) {
            return new CollectionItemInfoCompat(IMPL.obtainCollectionItemInfo(rowIndex, rowSpan,
                    columnIndex, columnSpan, heading, selected));
        }

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowIndex The row index at which the item is located.
         * @param rowSpan The number of rows the item spans.
         * @param columnIndex The column index at which the item is located.
         * @param columnSpan The number of columns the item spans.
         * @param heading Whether the item is a heading.
         * @return An instance.
         */
        public static CollectionItemInfoCompat obtain(int rowIndex, int rowSpan,
                int columnIndex, int columnSpan, boolean heading) {
            return new CollectionItemInfoCompat(IMPL.obtainCollectionItemInfo(rowIndex, rowSpan,
                    columnIndex, columnSpan, heading));
        }

        CollectionItemInfoCompat(Object info) {
            mInfo = info;
        }

        /**
         * Gets the column index at which the item is located.
         *
         * @return The column index.
         */
        public int getColumnIndex() {
            return IMPL.getCollectionItemColumnIndex(mInfo);
        }

        /**
         * Gets the number of columns the item spans.
         *
         * @return The column span.
         */
        public int getColumnSpan() {
            return IMPL.getCollectionItemColumnSpan(mInfo);
        }

        /**
         * Gets the row index at which the item is located.
         *
         * @return The row index.
         */
        public int getRowIndex() {
            return IMPL.getCollectionItemRowIndex(mInfo);
        }

        /**
         * Gets the number of rows the item spans.
         *
         * @return The row span.
         */
        public int getRowSpan() {
            return IMPL.getCollectionItemRowSpan(mInfo);
        }

        /**
         * Gets if the collection item is a heading. For example, section
         * heading, table header, etc.
         *
         * @return If the item is a heading.
         */
        public boolean isHeading() {
            return IMPL.isCollectionItemHeading(mInfo);
        }

        /**
         * Gets if the collection item is selected.
         *
         * @return If the item is selected.
         */
        public boolean isSelected() {
            return IMPL.isCollectionItemSelected(mInfo);
        }
    }

    /**
     * Class with information if a node is a range.
     */
    public static class RangeInfoCompat {
        /** Range type: integer. */
        public static final int RANGE_TYPE_INT = 0;
        /** Range type: float. */
        public static final int RANGE_TYPE_FLOAT = 1;
        /** Range type: percent with values from zero to one.*/
        public static final int RANGE_TYPE_PERCENT = 2;

        /**
         * Obtains a cached instance if such is available otherwise a new one.
         *
         * @param type The type of the range.
         * @param min The min value.
         * @param max The max value.
         * @param current The current value.
         * @return The instance
         */
        public static RangeInfoCompat obtain(int type, float min, float max, float current) {
            return new RangeInfoCompat(IMPL.obtainRangeInfo(type, min, max, current));
        }

        final Object mInfo;

        RangeInfoCompat(Object info) {
            mInfo = info;
        }

        /**
         * Gets the current value.
         *
         * @return The current value.
         */
        public float getCurrent() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getCurrent(mInfo);
        }

        /**
         * Gets the max value.
         *
         * @return The max value.
         */
        public float getMax() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getMax(mInfo);
        }

        /**
         * Gets the min value.
         *
         * @return The min value.
         */
        public float getMin() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getMin(mInfo);
        }

        /**
         * Gets the range type.
         *
         * @return The range type.
         *
         * @see #RANGE_TYPE_INT
         * @see #RANGE_TYPE_FLOAT
         * @see #RANGE_TYPE_PERCENT
         */
        public int getType() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getType(mInfo);
        }
    }

    static class AccessibilityNodeInfoBaseImpl {
        public Object newAccessibilityAction(int actionId, CharSequence label) {
            return null;
        }

        public AccessibilityNodeInfo obtain(View root, int virtualDescendantId) {
            return null;
        }

        public void addAction(AccessibilityNodeInfo info, Object action) {
        }

        public boolean removeAction(AccessibilityNodeInfo info, Object action) {
            return false;
        }

        public int getAccessibilityActionId(Object action) {
            return 0;
        }

        public CharSequence getAccessibilityActionLabel(Object action) {
            return null;
        }

        public void addChild(AccessibilityNodeInfo info, View child, int virtualDescendantId) {
        }

        public boolean removeChild(AccessibilityNodeInfo info, View child) {
            return false;
        }

        public boolean removeChild(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            return false;
        }

        public boolean isVisibleToUser(AccessibilityNodeInfo info) {
            return false;
        }

        public boolean isAccessibilityFocused(AccessibilityNodeInfo info) {
            return false;
        }

        public boolean performAction(AccessibilityNodeInfo info, int action, Bundle arguments) {
            return false;
        }

        public void setMovementGranularities(AccessibilityNodeInfo info, int granularities) {
        }

        public int getMovementGranularities(AccessibilityNodeInfo info) {
            return 0;
        }

        public void setVisibleToUser(AccessibilityNodeInfo info, boolean visibleToUser) {
        }

        public void setAccessibilityFocused(AccessibilityNodeInfo info, boolean focused) {
        }

        public void setSource(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
        }

        public Object findFocus(AccessibilityNodeInfo info, int focus) {
            return null;
        }

        public Object focusSearch(AccessibilityNodeInfo info, int direction) {
            return null;
        }

        public void setParent(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
        }

        public String getViewIdResourceName(AccessibilityNodeInfo info) {
            return null;
        }

        public void setViewIdResourceName(AccessibilityNodeInfo info, String viewId) {
        }

        public int getLiveRegion(AccessibilityNodeInfo info) {
            return ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE;
        }

        public void setLiveRegion(AccessibilityNodeInfo info, int mode) {
            // No-op
        }

        public Object getCollectionInfo(AccessibilityNodeInfo info) {
            return null;
        }

        public void setCollectionInfo(AccessibilityNodeInfo info, Object collectionInfo) {
        }

        public Object getCollectionItemInfo(AccessibilityNodeInfo info) {
            return null;
        }

        public void setCollectionItemInfo(AccessibilityNodeInfo info, Object collectionItemInfo) {
        }

        public Object getRangeInfo(AccessibilityNodeInfo info) {
            return null;
        }

        public void setRangeInfo(AccessibilityNodeInfo info, Object rangeInfo) {
        }

        public List<Object> getActionList(AccessibilityNodeInfo info) {
            return null;
        }

        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode) {
            return null;
        }

        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical) {
            return null;
        }

        public int getCollectionInfoColumnCount(Object info) {
            return 0;
        }

        public int getCollectionInfoRowCount(Object info) {
            return 0;
        }

        public boolean isCollectionInfoHierarchical(Object info) {
            return false;
        }

        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return null;
        }

        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading) {
            return null;
        }

        public int getCollectionItemColumnIndex(Object info) {
            return 0;
        }

        public int getCollectionItemColumnSpan(Object info) {
            return 0;
        }

        public int getCollectionItemRowIndex(Object info) {
            return 0;
        }

        public int getCollectionItemRowSpan(Object info) {
            return 0;
        }

        public boolean isCollectionItemHeading(Object info) {
            return false;
        }

        public boolean isCollectionItemSelected(Object info) {
            return false;
        }

        public Object obtainRangeInfo(int type, float min, float max, float current) {
            return null;
        }

        public Object getTraversalBefore(AccessibilityNodeInfo info) {
            return null;
        }

        public void setTraversalBefore(AccessibilityNodeInfo info, View view) {
        }

        public void setTraversalBefore(AccessibilityNodeInfo info, View root,
                int virtualDescendantId) {
        }

        public Object getTraversalAfter(AccessibilityNodeInfo info) {
            return null;
        }

        public void setTraversalAfter(AccessibilityNodeInfo info, View view) {
        }

        public void setTraversalAfter(AccessibilityNodeInfo info, View root,
                int virtualDescendantId) {
        }

        public void setContentInvalid(AccessibilityNodeInfo info, boolean contentInvalid) {
        }

        public boolean isContentInvalid(AccessibilityNodeInfo info) {
            return false;
        }

        public void setError(AccessibilityNodeInfo info, CharSequence error) {
        }

        public CharSequence getError(AccessibilityNodeInfo info) {
            return null;
        }

        public void setLabelFor(AccessibilityNodeInfo info, View labeled) {
        }

        public void setLabelFor(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
        }

        public Object getLabelFor(AccessibilityNodeInfo info) {
            return null;
        }

        public void setLabeledBy(AccessibilityNodeInfo info, View labeled) {
        }

        public void setLabeledBy(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
        }

        public Object getLabeledBy(AccessibilityNodeInfo info) {
            return null;
        }

        public boolean canOpenPopup(AccessibilityNodeInfo info) {
            return false;
        }

        public void setCanOpenPopup(AccessibilityNodeInfo info, boolean opensPopup) {
        }

        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(
                AccessibilityNodeInfo info, String viewId) {
            return Collections.emptyList();
        }

        public Bundle getExtras(AccessibilityNodeInfo info) {
            return new Bundle();
        }

        public int getInputType(AccessibilityNodeInfo info) {
            return InputType.TYPE_NULL;
        }

        public void setInputType(AccessibilityNodeInfo info, int inputType) {
        }

        public void setMaxTextLength(AccessibilityNodeInfo info, int max) {
        }

        public int getMaxTextLength(AccessibilityNodeInfo info) {
            return -1;
        }

        public void setTextSelection(AccessibilityNodeInfo info, int start, int end) {
        }

        public int getTextSelectionStart(AccessibilityNodeInfo info) {
            return -1;
        }

        public int getTextSelectionEnd(AccessibilityNodeInfo info) {
            return -1;
        }

        public Object getWindow(AccessibilityNodeInfo info) {
            return null;
        }

        public boolean isDismissable(AccessibilityNodeInfo info) {
            return false;
        }

        public void setDismissable(AccessibilityNodeInfo info, boolean dismissable) {
        }

        public boolean isEditable(AccessibilityNodeInfo info) {
            return false;
        }

        public void setEditable(AccessibilityNodeInfo info, boolean editable) {
        }

        public boolean isMultiLine(AccessibilityNodeInfo info) {
            return false;
        }

        public void setMultiLine(AccessibilityNodeInfo info, boolean multiLine) {
        }

        public boolean refresh(AccessibilityNodeInfo info) {
            return false;
        }

        public CharSequence getRoleDescription(AccessibilityNodeInfo info) {
            return null;
        }

        public void setRoleDescription(AccessibilityNodeInfo info, CharSequence roleDescription) {
        }

        public Object getActionScrollToPosition() {
            return null;
        }

        public Object getActionSetProgress() {
            return null;
        }

        public boolean isContextClickable(AccessibilityNodeInfo info) {
            return false;
        }

        public void setContextClickable(AccessibilityNodeInfo info, boolean contextClickable) {
            // Do nothing.
        }

        public Object getActionShowOnScreen() {
            return null;
        }

        public Object getActionScrollUp() {
            return null;
        }

        public Object getActionScrollDown() {
            return null;
        }

        public Object getActionScrollLeft() {
            return null;
        }

        public Object getActionScrollRight() {
            return null;
        }

        public Object getActionContextClick() {
            return null;
        }

        public int getCollectionInfoSelectionMode(Object info) {
            return 0;
        }

        public int getDrawingOrder(AccessibilityNodeInfo info) {
            return 0;
        }

        public void setDrawingOrder(AccessibilityNodeInfo info, int drawingOrderInParent) {
        }

        public boolean isImportantForAccessibility(AccessibilityNodeInfo info) {
            return true;
        }

        public void setImportantForAccessibility(AccessibilityNodeInfo info,
                boolean importantForAccessibility) {
        }
    }

    @RequiresApi(16)
    static class AccessibilityNodeInfoApi16Impl extends AccessibilityNodeInfoBaseImpl {
        @Override
        public AccessibilityNodeInfo obtain(View root, int virtualDescendantId) {
            return AccessibilityNodeInfo.obtain(root, virtualDescendantId);
        }

        @Override
        public Object findFocus(AccessibilityNodeInfo info, int focus) {
            return info.findFocus(focus);
        }

        @Override
        public Object focusSearch(AccessibilityNodeInfo info, int direction) {
            return info.focusSearch(direction);
        }

        @Override
        public void addChild(AccessibilityNodeInfo info, View child, int virtualDescendantId) {
            info.addChild(child, virtualDescendantId);
        }

        @Override
        public void setSource(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            info.setSource(root, virtualDescendantId);
        }

        @Override
        public boolean isVisibleToUser(AccessibilityNodeInfo info) {
            return info.isVisibleToUser();
        }

        @Override
        public void setVisibleToUser(AccessibilityNodeInfo info, boolean visibleToUser) {
            info.setVisibleToUser(visibleToUser);
        }

        @Override
        public boolean isAccessibilityFocused(AccessibilityNodeInfo info) {
            return info.isAccessibilityFocused();
        }

        @Override
        public void setAccessibilityFocused(AccessibilityNodeInfo info, boolean focused) {
            info.setAccessibilityFocused(focused);
        }

        @Override
        public boolean performAction(AccessibilityNodeInfo info, int action, Bundle arguments) {
            return info.performAction(action, arguments);
        }

        @Override
        public void setMovementGranularities(AccessibilityNodeInfo info, int granularities) {
            info.setMovementGranularities(granularities);
        }

        @Override
        public int getMovementGranularities(AccessibilityNodeInfo info) {
            return info.getMovementGranularities();
        }

        @Override
        public void setParent(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            info.setParent(root, virtualDescendantId);
        }
    }

    @RequiresApi(17)
    static class AccessibilityNodeInfoApi17Impl extends AccessibilityNodeInfoApi16Impl {

        @Override
        public void setLabelFor(AccessibilityNodeInfo info, View labeled) {
            info.setLabelFor(labeled);
        }

        @Override
        public void setLabelFor(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            info.setLabelFor(root, virtualDescendantId);
        }

        @Override
        public Object getLabelFor(AccessibilityNodeInfo info) {
            return info.getLabelFor();
        }

        @Override
        public void setLabeledBy(AccessibilityNodeInfo info, View labeled) {
            info.setLabeledBy(labeled);
        }

        @Override
        public void setLabeledBy(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            info.setLabeledBy(root, virtualDescendantId);
        }

        @Override
        public Object getLabeledBy(AccessibilityNodeInfo info) {
            return info.getLabeledBy();
        }
    }

    @RequiresApi(18)
    static class AccessibilityNodeInfoApi18Impl extends AccessibilityNodeInfoApi17Impl {

        @Override
        public String getViewIdResourceName(AccessibilityNodeInfo info) {
            return info.getViewIdResourceName();
        }

        @Override
        public void setViewIdResourceName(AccessibilityNodeInfo info, String viewId) {
            info.setViewIdResourceName(viewId);
        }

        @Override
        public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(
                AccessibilityNodeInfo info, String viewId) {
            return info.findAccessibilityNodeInfosByViewId(viewId);
        }

        @Override
        public void setTextSelection(AccessibilityNodeInfo info, int start, int end) {
            info.setTextSelection(start, end);
        }

        @Override
        public int getTextSelectionStart(AccessibilityNodeInfo info) {
            return info.getTextSelectionStart();
        }

        @Override
        public int getTextSelectionEnd(AccessibilityNodeInfo info) {
            return info.getTextSelectionEnd();
        }

        @Override
        public boolean isEditable(AccessibilityNodeInfo info) {
            return info.isEditable();
        }

        @Override
        public void setEditable(AccessibilityNodeInfo info, boolean editable) {
            info.setEditable(editable);
        }

        @Override
        public boolean refresh(AccessibilityNodeInfo info) {
            return info.refresh();
        }
    }

    @RequiresApi(19)
    static class AccessibilityNodeInfoApi19Impl extends AccessibilityNodeInfoApi18Impl {
        private static final String ROLE_DESCRIPTION_KEY =
                "AccessibilityNodeInfo.roleDescription";

        @Override
        public int getLiveRegion(AccessibilityNodeInfo info) {
            return info.getLiveRegion();
        }

        @Override
        public void setLiveRegion(AccessibilityNodeInfo info, int mode) {
            info.setLiveRegion(mode);
        }

        @Override
        public Object getCollectionInfo(AccessibilityNodeInfo info) {
            return info.getCollectionInfo();
        }

        @Override
        public void setCollectionInfo(AccessibilityNodeInfo info, Object collectionInfo) {
            info.setCollectionInfo((AccessibilityNodeInfo.CollectionInfo) collectionInfo);
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode) {
            return AccessibilityNodeInfo.CollectionInfo.obtain(rowCount, columnCount, hierarchical);
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical) {
            return AccessibilityNodeInfo.CollectionInfo.obtain(rowCount, columnCount, hierarchical);
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return AccessibilityNodeInfo.CollectionItemInfo.obtain(rowIndex, rowSpan, columnIndex,
                    columnSpan, heading);
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading) {
            return AccessibilityNodeInfo.CollectionItemInfo.obtain(rowIndex, rowSpan, columnIndex,
                    columnSpan, heading);
        }

        @Override
        public int getCollectionInfoColumnCount(Object info) {
            return ((AccessibilityNodeInfo.CollectionInfo) info).getColumnCount();
        }

        @Override
        public int getCollectionInfoRowCount(Object info) {
            return ((AccessibilityNodeInfo.CollectionInfo) info).getRowCount();
        }

        @Override
        public boolean isCollectionInfoHierarchical(Object info) {
            return ((AccessibilityNodeInfo.CollectionInfo) info).isHierarchical();
        }

        @Override
        public Object getCollectionItemInfo(AccessibilityNodeInfo info) {
            return info.getCollectionItemInfo();
        }

        @Override
        public Object getRangeInfo(AccessibilityNodeInfo info) {
            return info.getRangeInfo();
        }

        @Override
        public void setRangeInfo(AccessibilityNodeInfo info, Object rangeInfo) {
            info.setRangeInfo((AccessibilityNodeInfo.RangeInfo) rangeInfo);
        }

        @Override
        public int getCollectionItemColumnIndex(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).getColumnIndex();
        }

        @Override
        public int getCollectionItemColumnSpan(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).getColumnSpan();
        }

        @Override
        public int getCollectionItemRowIndex(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).getRowIndex();
        }

        @Override
        public int getCollectionItemRowSpan(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).getRowSpan();
        }

        @Override
        public boolean isCollectionItemHeading(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).isHeading();
        }

        @Override
        public void setCollectionItemInfo(AccessibilityNodeInfo info, Object collectionItemInfo) {
            info.setCollectionItemInfo(
                    (AccessibilityNodeInfo.CollectionItemInfo) collectionItemInfo);
        }

        @Override
        public Object obtainRangeInfo(int type, float min, float max, float current) {
            return AccessibilityNodeInfo.RangeInfo.obtain(type, min, max, current);
        }

        @Override
        public void setContentInvalid(AccessibilityNodeInfo info, boolean contentInvalid) {
            info.setContentInvalid(contentInvalid);
        }

        @Override
        public boolean isContentInvalid(AccessibilityNodeInfo info) {
            return info.isContentInvalid();
        }

        @Override
        public boolean canOpenPopup(AccessibilityNodeInfo info) {
            return info.canOpenPopup();
        }

        @Override
        public void setCanOpenPopup(AccessibilityNodeInfo info, boolean opensPopup) {
            info.setCanOpenPopup(opensPopup);
        }

        @Override
        public Bundle getExtras(AccessibilityNodeInfo info) {
            return info.getExtras();
        }

        @Override
        public int getInputType(AccessibilityNodeInfo info) {
            return info.getInputType();
        }

        @Override
        public void setInputType(AccessibilityNodeInfo info, int inputType) {
            info.setInputType(inputType);
        }

        @Override
        public boolean isDismissable(AccessibilityNodeInfo info) {
            return info.isDismissable();
        }

        @Override
        public void setDismissable(AccessibilityNodeInfo info, boolean dismissable) {
            info.setDismissable(dismissable);
        }

        @Override
        public boolean isMultiLine(AccessibilityNodeInfo info) {
            return info.isMultiLine();
        }

        @Override
        public void setMultiLine(AccessibilityNodeInfo info, boolean multiLine) {
            info.setMultiLine(multiLine);
        }

        @Override
        public CharSequence getRoleDescription(AccessibilityNodeInfo info) {
            Bundle extras = getExtras(info);
            return extras.getCharSequence(ROLE_DESCRIPTION_KEY);
        }

        @Override
        public void setRoleDescription(AccessibilityNodeInfo info, CharSequence roleDescription) {
            Bundle extras = getExtras(info);
            extras.putCharSequence(ROLE_DESCRIPTION_KEY, roleDescription);
        }
    }

    @RequiresApi(21)
    static class AccessibilityNodeInfoApi21Impl extends AccessibilityNodeInfoApi19Impl {
        @Override
        public Object newAccessibilityAction(int actionId, CharSequence label) {
            return new AccessibilityNodeInfo.AccessibilityAction(actionId, label);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<Object> getActionList(AccessibilityNodeInfo info) {
            Object result = info.getActionList();
            return (List<Object>) result;
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode) {
            return AccessibilityNodeInfo.CollectionInfo.obtain(rowCount, columnCount, hierarchical,
                    selectionMode);
        }

        @Override
        public void addAction(AccessibilityNodeInfo info, Object action) {
            info.addAction((AccessibilityNodeInfo.AccessibilityAction) action);
        }

        @Override
        public boolean removeAction(AccessibilityNodeInfo info, Object action) {
            return info.removeAction((AccessibilityNodeInfo.AccessibilityAction) action);
        }

        @Override
        public int getAccessibilityActionId(Object action) {
            return ((AccessibilityNodeInfo.AccessibilityAction) action).getId();
        }

        @Override
        public CharSequence getAccessibilityActionLabel(Object action) {
            return ((AccessibilityNodeInfo.AccessibilityAction) action).getLabel();
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return AccessibilityNodeInfo.CollectionItemInfo.obtain(rowIndex, rowSpan, columnIndex,
                    columnSpan, heading, selected);
        }

        @Override
        public boolean isCollectionItemSelected(Object info) {
            return ((AccessibilityNodeInfo.CollectionItemInfo) info).isSelected();
        }

        @Override
        public CharSequence getError(AccessibilityNodeInfo info) {
            return info.getError();
        }

        @Override
        public void setError(AccessibilityNodeInfo info, CharSequence error) {
            info.setError(error);
        }

        @Override
        public void setMaxTextLength(AccessibilityNodeInfo info, int max) {
            info.setMaxTextLength(max);
        }

        @Override
        public int getMaxTextLength(AccessibilityNodeInfo info) {
            return info.getMaxTextLength();
        }

        @Override
        public Object getWindow(AccessibilityNodeInfo info) {
            return info.getWindow();
        }

        @Override
        public boolean removeChild(AccessibilityNodeInfo info, View child) {
            return info.removeChild(child);
        }

        @Override
        public boolean removeChild(AccessibilityNodeInfo info, View root, int virtualDescendantId) {
            return info.removeChild(root, virtualDescendantId);
        }

        @Override
        public int getCollectionInfoSelectionMode(Object info) {
            return ((AccessibilityNodeInfo.CollectionInfo) info).getSelectionMode();
        }
    }

    @RequiresApi(22)
    static class AccessibilityNodeInfoApi22Impl extends AccessibilityNodeInfoApi21Impl {
        @Override
        public Object getTraversalBefore(AccessibilityNodeInfo info) {
            return info.getTraversalBefore();
        }

        @Override
        public void setTraversalBefore(AccessibilityNodeInfo info, View view) {
            info.setTraversalBefore(view);
        }

        @Override
        public void setTraversalBefore(AccessibilityNodeInfo info, View root,
                int virtualDescendantId) {
            info.setTraversalBefore(root, virtualDescendantId);
        }

        @Override
        public Object getTraversalAfter(AccessibilityNodeInfo info) {
            return info.getTraversalAfter();
        }

        @Override
        public void setTraversalAfter(AccessibilityNodeInfo info, View view) {
            info.setTraversalAfter(view);
        }

        @Override
        public void setTraversalAfter(AccessibilityNodeInfo info, View root,
                int virtualDescendantId) {
            info.setTraversalAfter(root, virtualDescendantId);
        }
    }

    @RequiresApi(23)
    static class AccessibilityNodeInfoApi23Impl extends AccessibilityNodeInfoApi22Impl {
        @Override
        public Object getActionScrollToPosition() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION;
        }

        @Override
        public Object getActionShowOnScreen() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN;
        }

        @Override
        public Object getActionScrollUp() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP;
        }

        @Override
        public Object getActionScrollDown() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN;
        }

        @Override
        public Object getActionScrollLeft() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT;
        }

        @Override
        public Object getActionScrollRight() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT;
        }

        @Override
        public Object getActionContextClick() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK;
        }

        @Override
        public boolean isContextClickable(AccessibilityNodeInfo info) {
            return info.isContextClickable();
        }

        @Override
        public void setContextClickable(AccessibilityNodeInfo info, boolean contextClickable) {
            info.setContextClickable(contextClickable);
        }
    }

    @RequiresApi(24)
    static class AccessibilityNodeInfoApi24Impl extends AccessibilityNodeInfoApi23Impl {
        @Override
        public Object getActionSetProgress() {
            return AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS;
        }

        @Override
        public int getDrawingOrder(AccessibilityNodeInfo info) {
            return info.getDrawingOrder();
        }

        @Override
        public void setDrawingOrder(AccessibilityNodeInfo info, int drawingOrderInParent) {
            info.setDrawingOrder(drawingOrderInParent);
        }

        @Override
        public boolean isImportantForAccessibility(AccessibilityNodeInfo info) {
            return info.isImportantForAccessibility();
        }

        @Override
        public void setImportantForAccessibility(AccessibilityNodeInfo info,
                boolean importantForAccessibility) {
            info.setImportantForAccessibility(importantForAccessibility);
        }

    }

    static {
        if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new AccessibilityNodeInfoApi24Impl();
        } else if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new AccessibilityNodeInfoApi23Impl();
        } else if (Build.VERSION.SDK_INT >= 22) {
            IMPL = new AccessibilityNodeInfoApi22Impl();
        } else if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new AccessibilityNodeInfoApi21Impl();
        } else if (Build.VERSION.SDK_INT >= 19) { // KitKat
            IMPL = new AccessibilityNodeInfoApi19Impl();
        } else if (Build.VERSION.SDK_INT >= 18) { // JellyBean MR2
            IMPL = new AccessibilityNodeInfoApi18Impl();
        } else if (Build.VERSION.SDK_INT >= 17) { // JellyBean MR1
            IMPL = new AccessibilityNodeInfoApi17Impl();
        } else if (Build.VERSION.SDK_INT >= 16) { // JellyBean
            IMPL = new AccessibilityNodeInfoApi16Impl();
        } else {
            IMPL = new AccessibilityNodeInfoBaseImpl();
        }
    }

    static final AccessibilityNodeInfoBaseImpl IMPL;

    private final AccessibilityNodeInfo mInfo;

    /**
     *  android.support.v4.widget.ExploreByTouchHelper.HOST_ID = -1;
     *
     *  @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public int mParentVirtualDescendantId = -1;

    // Actions introduced in IceCreamSandwich

    /**
     * Action that focuses the node.
     */
    public static final int ACTION_FOCUS = 0x00000001;

    /**
     * Action that unfocuses the node.
     */
    public static final int ACTION_CLEAR_FOCUS = 0x00000002;

    /**
     * Action that selects the node.
     */
    public static final int ACTION_SELECT = 0x00000004;

    /**
     * Action that unselects the node.
     */
    public static final int ACTION_CLEAR_SELECTION = 0x00000008;

    /**
     * Action that clicks on the node info.
     */
    public static final int ACTION_CLICK = 0x00000010;

    /**
     * Action that long clicks on the node.
     */
    public static final int ACTION_LONG_CLICK = 0x00000020;

    // Actions introduced in JellyBean

    /**
     * Action that gives accessibility focus to the node.
     */
    public static final int ACTION_ACCESSIBILITY_FOCUS = 0x00000040;

    /**
     * Action that clears accessibility focus of the node.
     */
    public static final int ACTION_CLEAR_ACCESSIBILITY_FOCUS = 0x00000080;

    /**
     * Action that requests to go to the next entity in this node's text
     * at a given movement granularity. For example, move to the next character,
     * word, etc.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT}<,
     * {@link #ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN}<br>
     * <strong>Example:</strong> Move to the previous character and do not extend selection.
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
     *           AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
     *   arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
     *           false);
     *   info.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
     * </code></pre></p>
     * </p>
     *
     * @see #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
     * @see #ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
     *
     * @see #setMovementGranularities(int)
     * @see #getMovementGranularities()
     *
     * @see #MOVEMENT_GRANULARITY_CHARACTER
     * @see #MOVEMENT_GRANULARITY_WORD
     * @see #MOVEMENT_GRANULARITY_LINE
     * @see #MOVEMENT_GRANULARITY_PARAGRAPH
     * @see #MOVEMENT_GRANULARITY_PAGE
     */
    public static final int ACTION_NEXT_AT_MOVEMENT_GRANULARITY = 0x00000100;

    /**
     * Action that requests to go to the previous entity in this node's text
     * at a given movement granularity. For example, move to the next character,
     * word, etc.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT}<,
     * {@link #ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN}<br>
     * <strong>Example:</strong> Move to the next character and do not extend selection.
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
     *           AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
     *   arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
     *           false);
     *   info.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
     *           arguments);
     * </code></pre></p>
     * </p>
     *
     * @see #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT
     * @see #ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN
     *
     * @see #setMovementGranularities(int)
     * @see #getMovementGranularities()
     *
     * @see #MOVEMENT_GRANULARITY_CHARACTER
     * @see #MOVEMENT_GRANULARITY_WORD
     * @see #MOVEMENT_GRANULARITY_LINE
     * @see #MOVEMENT_GRANULARITY_PARAGRAPH
     * @see #MOVEMENT_GRANULARITY_PAGE
     */
    public static final int ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY = 0x00000200;

    /**
     * Action to move to the next HTML element of a given type. For example, move
     * to the BUTTON, INPUT, TABLE, etc.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_HTML_ELEMENT_STRING}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING, "BUTTON");
     *   info.performAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT, arguments);
     * </code></pre></p>
     * </p>
     */
    public static final int ACTION_NEXT_HTML_ELEMENT = 0x00000400;

    /**
     * Action to move to the previous HTML element of a given type. For example, move
     * to the BUTTON, INPUT, TABLE, etc.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_HTML_ELEMENT_STRING}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING, "BUTTON");
     *   info.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT, arguments);
     * </code></pre></p>
     * </p>
     */
    public static final int ACTION_PREVIOUS_HTML_ELEMENT = 0x00000800;

    /**
     * Action to scroll the node content forward.
     */
    public static final int ACTION_SCROLL_FORWARD = 0x00001000;

    /**
     * Action to scroll the node content backward.
     */
    public static final int ACTION_SCROLL_BACKWARD = 0x00002000;

    // Actions introduced in JellyBeanMr2

    /**
     * Action to copy the current selection to the clipboard.
     */
    public static final int ACTION_COPY = 0x00004000;

    /**
     * Action to paste the current clipboard content.
     */
    public static final int ACTION_PASTE = 0x00008000;

    /**
     * Action to cut the current selection and place it to the clipboard.
     */
    public static final int ACTION_CUT = 0x00010000;

    /**
     * Action to set the selection. Performing this action with no arguments
     * clears the selection.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_SELECTION_START_INT},
     * {@link #ACTION_ARGUMENT_SELECTION_END_INT}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 1);
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 2);
     *   info.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
     * </code></pre></p>
     * </p>
     *
     * @see #ACTION_ARGUMENT_SELECTION_START_INT
     * @see #ACTION_ARGUMENT_SELECTION_END_INT
     */
    public static final int ACTION_SET_SELECTION = 0x00020000;

    /**
     * Action to expand an expandable node.
     */
    public static final int ACTION_EXPAND = 0x00040000;

    /**
     * Action to collapse an expandable node.
     */
    public static final int ACTION_COLLAPSE = 0x00080000;

    /**
     * Action to dismiss a dismissable node.
     */
    public static final int ACTION_DISMISS = 0x00100000;

    /**
     * Action that sets the text of the node. Performing the action without argument, using <code>
     * null</code> or empty {@link CharSequence} will clear the text. This action will also put the
     * cursor at the end of text.
     * <p>
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
     *       "android");
     *   info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
     * </code></pre></p>
     */
    public static final int ACTION_SET_TEXT = 0x00200000;

    // Action arguments

    /**
     * Argument for which movement granularity to be used when traversing the node text.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong> {@link #ACTION_NEXT_AT_MOVEMENT_GRANULARITY},
     * {@link #ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY}
     * </p>
     */
    public static final String ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT =
        "ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT";

    /**
     * Argument for which HTML element to get moving to the next/previous HTML element.
     * <p>
     * <strong>Type:</strong> String<br>
     * <strong>Actions:</strong> {@link #ACTION_NEXT_HTML_ELEMENT},
     *         {@link #ACTION_PREVIOUS_HTML_ELEMENT}
     * </p>
     */
    public static final String ACTION_ARGUMENT_HTML_ELEMENT_STRING =
        "ACTION_ARGUMENT_HTML_ELEMENT_STRING";

    /**
     * Argument for whether when moving at granularity to extend the selection
     * or to move it otherwise.
     * <p>
     * <strong>Type:</strong> boolean<br>
     * <strong>Actions:</strong> {@link #ACTION_NEXT_AT_MOVEMENT_GRANULARITY},
     * {@link #ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY}
     * </p>
     *
     * @see #ACTION_NEXT_AT_MOVEMENT_GRANULARITY
     * @see #ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
     */
    public static final String ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN =
            "ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN";

    /**
     * Argument for specifying the selection start.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong> {@link #ACTION_SET_SELECTION}
     * </p>
     *
     * @see #ACTION_SET_SELECTION
     */
    public static final String ACTION_ARGUMENT_SELECTION_START_INT =
            "ACTION_ARGUMENT_SELECTION_START_INT";

    /**
     * Argument for specifying the selection end.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong> {@link #ACTION_SET_SELECTION}
     * </p>
     *
     * @see #ACTION_SET_SELECTION
     */
    public static final String ACTION_ARGUMENT_SELECTION_END_INT =
            "ACTION_ARGUMENT_SELECTION_END_INT";

    /**
     * Argument for specifying the text content to set
     * <p>
     * <strong>Type:</strong> CharSequence<br>
     * <strong>Actions:</strong> {@link #ACTION_SET_TEXT}
     * </p>
     *
     * @see #ACTION_SET_TEXT
     */
    public static final String ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE =
            "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE";

    /**
     * Argument for specifying the collection row to make visible on screen.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_SCROLL_TO_POSITION}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_SCROLL_TO_POSITION
     */
    public static final String ACTION_ARGUMENT_ROW_INT =
            "android.view.accessibility.action.ARGUMENT_ROW_INT";

    /**
     * Argument for specifying the collection column to make visible on screen.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_SCROLL_TO_POSITION}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_SCROLL_TO_POSITION
     */
    public static final String ACTION_ARGUMENT_COLUMN_INT =
            "android.view.accessibility.action.ARGUMENT_COLUMN_INT";

    /**
     * Argument for specifying the progress value to set.
     * <p>
     * <strong>Type:</strong> float<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_SET_PROGRESS}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_SET_PROGRESS
     */
    public static final String ACTION_ARGUMENT_PROGRESS_VALUE =
            "android.view.accessibility.action.ARGUMENT_PROGRESS_VALUE";

    // Focus types

    /**
     * The input focus.
     */
    public static final int FOCUS_INPUT = 1;

    /**
     * The accessibility focus.
     */
    public static final int FOCUS_ACCESSIBILITY = 2;

    // Movement granularities

    /**
     * Movement granularity bit for traversing the text of a node by character.
     */
    public static final int MOVEMENT_GRANULARITY_CHARACTER = 0x00000001;

    /**
     * Movement granularity bit for traversing the text of a node by word.
     */
    public static final int MOVEMENT_GRANULARITY_WORD = 0x00000002;

    /**
     * Movement granularity bit for traversing the text of a node by line.
     */
    public static final int MOVEMENT_GRANULARITY_LINE = 0x00000004;

    /**
     * Movement granularity bit for traversing the text of a node by paragraph.
     */
    public static final int MOVEMENT_GRANULARITY_PARAGRAPH = 0x00000008;

    /**
     * Movement granularity bit for traversing the text of a node by page.
     */
    public static final int MOVEMENT_GRANULARITY_PAGE = 0x00000010;

    /**
     * Creates a wrapper for info implementation.
     *
     * @param object The info to wrap.
     * @return A wrapper for if the object is not null, null otherwise.
     */
    static AccessibilityNodeInfoCompat wrapNonNullInstance(Object object) {
        if (object != null) {
            return new AccessibilityNodeInfoCompat(object);
        }
        return null;
    }

    /**
     * Creates a new instance wrapping an
     * {@link android.view.accessibility.AccessibilityNodeInfo}.
     *
     * @param info The info.
     *
     * @deprecated Use {@link #wrap(AccessibilityNodeInfo)} instead.
     */
    @Deprecated
    public AccessibilityNodeInfoCompat(Object info) {
        mInfo = (AccessibilityNodeInfo) info;
    }

    private AccessibilityNodeInfoCompat(AccessibilityNodeInfo info) {
        mInfo = info;
    }

    /**
     * Creates a new instance wrapping an
     * {@link android.view.accessibility.AccessibilityNodeInfo}.
     *
     * @param info The info.
     */
    public static AccessibilityNodeInfoCompat wrap(@NonNull AccessibilityNodeInfo info) {
        return new AccessibilityNodeInfoCompat(info);
    }

    /**
     * @return The unwrapped {@link android.view.accessibility.AccessibilityNodeInfo}.
     */
    public AccessibilityNodeInfo unwrap() {
        return mInfo;
    }

    /**
     * @return The wrapped {@link android.view.accessibility.AccessibilityNodeInfo}.
     *
     * @deprecated Use {@link #unwrap()} instead.
     */
    @Deprecated
    public Object getInfo() {
        return mInfo;
    }

    /**
     * Returns a cached instance if such is available otherwise a new one and
     * sets the source.
     *
     * @return An instance.
     * @see #setSource(View)
     */
    public static AccessibilityNodeInfoCompat obtain(View source) {
        return AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(source));
    }

    /**
     * Returns a cached instance if such is available otherwise a new one
     * and sets the source.
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     * @return An instance.
     *
     * @see #setSource(View, int)
     */
    public static AccessibilityNodeInfoCompat obtain(View root, int virtualDescendantId) {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                IMPL.obtain(root, virtualDescendantId));
    }

    /**
     * Returns a cached instance if such is available otherwise a new one.
     *
     * @return An instance.
     */
    public static AccessibilityNodeInfoCompat obtain() {
        return AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain());
    }

    /**
     * Returns a cached instance if such is available or a new one is create.
     * The returned instance is initialized from the given <code>info</code>.
     *
     * @param info The other info.
     * @return An instance.
     */
    public static AccessibilityNodeInfoCompat obtain(AccessibilityNodeInfoCompat info) {
        return AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain(info.mInfo));
    }

    /**
     * Sets the source.
     *
     * @param source The info source.
     */
    public void setSource(View source) {
        mInfo.setSource(source);
    }

    /**
     * Sets the source to be a virtual descendant of the given <code>root</code>.
     * If <code>virtualDescendantId</code> is {@link View#NO_ID} the root
     * is set as the source.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report themselves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setSource(View root, int virtualDescendantId) {
        IMPL.setSource(mInfo, root, virtualDescendantId);
    }

    /**
     * Find the view that has the specified focus type. The search starts from
     * the view represented by this node info.
     *
     * @param focus The focus to find. One of {@link #FOCUS_INPUT} or
     *         {@link #FOCUS_ACCESSIBILITY}.
     * @return The node info of the focused view or null.
     *
     * @see #FOCUS_INPUT
     * @see #FOCUS_ACCESSIBILITY
     */
    public AccessibilityNodeInfoCompat findFocus(int focus) {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.findFocus(mInfo, focus));
    }

    /**
     * Searches for the nearest view in the specified direction that can take
     * input focus.
     *
     * @param direction The direction. Can be one of:
     *     {@link View#FOCUS_DOWN},
     *     {@link View#FOCUS_UP},
     *     {@link View#FOCUS_LEFT},
     *     {@link View#FOCUS_RIGHT},
     *     {@link View#FOCUS_FORWARD},
     *     {@link View#FOCUS_BACKWARD}.
     *
     * @return The node info for the view that can take accessibility focus.
     */
    public AccessibilityNodeInfoCompat focusSearch(int direction) {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.focusSearch(mInfo, direction));
    }

    /**
     * Gets the id of the window from which the info comes from.
     *
     * @return The window id.
     */
    public int getWindowId() {
        return mInfo.getWindowId();
    }

    /**
     * Gets the number of children.
     *
     * @return The child count.
     */
    public int getChildCount() {
        return mInfo.getChildCount();
    }

    /**
     * Get the child at given index.
     * <p>
     * <strong>Note:</strong> It is a client responsibility to recycle the
     * received info by calling {@link AccessibilityNodeInfoCompat#recycle()} to
     * avoid creating of multiple instances.
     * </p>
     *
     * @param index The child index.
     * @return The child node.
     * @throws IllegalStateException If called outside of an
     *             AccessibilityService.
     */
    public AccessibilityNodeInfoCompat getChild(int index) {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getChild(index));
    }

    /**
     * Adds a child.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param child The child.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void addChild(View child) {
        mInfo.addChild(child);
    }

    /**
     * Adds a virtual child which is a descendant of the given <code>root</code>.
     * If <code>virtualDescendantId</code> is {@link View#NO_ID} the root
     * is added as a child.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual child.
     */
    public void addChild(View root, int virtualDescendantId) {
        IMPL.addChild(mInfo, root, virtualDescendantId);
    }

    /**
     * Removes a child. If the child was not previously added to the node,
     * calling this method has no effect.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}.
     * This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param child The child.
     * @return true if the child was present
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public boolean removeChild(View child) {
        return IMPL.removeChild(mInfo, child);
    }

    /**
     * Removes a virtual child which is a descendant of the given
     * <code>root</code>. If the child was not previously added to the node,
     * calling this method has no effect.
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual child.
     * @return true if the child was present
     * @see #addChild(View, int)
     */
    public boolean removeChild(View root, int virtualDescendantId) {
        return IMPL.removeChild(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the actions that can be performed on the node.
     *
     * @return The bit mask of with actions.
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_FOCUS
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_CLEAR_FOCUS
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_SELECT
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_CLEAR_SELECTION
     */
    public int getActions() {
        return mInfo.getActions();
    }

    /**
     * Adds an action that can be performed on the node.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param action The action.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void addAction(int action) {
        mInfo.addAction(action);
    }

    /**
     * Adds an action that can be performed on the node.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param action The action.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void addAction(AccessibilityActionCompat action) {
        IMPL.addAction(mInfo, action.mAction);
    }

    /**
     * Removes an action that can be performed on the node. If the action was
     * not already added to the node, calling this method has no effect.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param action The action to be removed.
     * @return The action removed from the list of actions.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public boolean removeAction(AccessibilityActionCompat action) {
        return IMPL.removeAction(mInfo, action.mAction);
    }

    /**
     * Performs an action on the node.
     * <p>
     * <strong>Note:</strong> An action can be performed only if the request is
     * made from an {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @param action The action to perform.
     * @return True if the action was performed.
     * @throws IllegalStateException If called outside of an
     *             AccessibilityService.
     */
    public boolean performAction(int action) {
        return mInfo.performAction(action);
    }

    /**
     * Performs an action on the node.
     * <p>
     *   <strong>Note:</strong> An action can be performed only if the request is made
     *   from an {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @param action The action to perform.
     * @param arguments A bundle with additional arguments.
     * @return True if the action was performed.
     *
     * @throws IllegalStateException If called outside of an AccessibilityService.
     */
    public boolean performAction(int action, Bundle arguments) {
        return IMPL.performAction(mInfo, action, arguments);
    }

    /**
     * Sets the movement granularities for traversing the text of this node.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param granularities The bit mask with granularities.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setMovementGranularities(int granularities) {
        IMPL.setMovementGranularities(mInfo, granularities);
    }

    /**
     * Gets the movement granularities for traversing the text of this node.
     *
     * @return The bit mask with granularities.
     */
    public int getMovementGranularities() {
        return IMPL.getMovementGranularities(mInfo);
    }

    /**
     * Finds {@link android.view.accessibility.AccessibilityNodeInfo}s by text. The match
     * is case insensitive containment. The search is relative to this info i.e. this
     * info is the root of the traversed tree.
     * <p>
     * <strong>Note:</strong> It is a client responsibility to recycle the
     * received info by calling {@link android.view.accessibility.AccessibilityNodeInfo#recycle()}
     * to avoid creating of multiple instances.
     * </p>
     *
     * @param text The searched text.
     * @return A list of node info.
     */
    public List<AccessibilityNodeInfoCompat> findAccessibilityNodeInfosByText(String text) {
        List<AccessibilityNodeInfoCompat> result = new ArrayList<AccessibilityNodeInfoCompat>();
        List<AccessibilityNodeInfo> infos = mInfo.findAccessibilityNodeInfosByText(text);
        final int infoCount = infos.size();
        for (int i = 0; i < infoCount; i++) {
            AccessibilityNodeInfo info = infos.get(i);
            result.add(AccessibilityNodeInfoCompat.wrap(info));
        }
        return result;
    }

    /**
     * Gets the parent.
     * <p>
     * <strong>Note:</strong> It is a client responsibility to recycle the
     * received info by calling {@link android.view.accessibility.AccessibilityNodeInfo#recycle()}
     * to avoid creating of multiple instances.
     * </p>
     *
     * @return The parent.
     */
    public AccessibilityNodeInfoCompat getParent() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getParent());
    }

    /**
     * Sets the parent.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param parent The parent.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setParent(View parent) {
        mInfo.setParent(parent);
    }

    /**
     * Sets the parent to be a virtual descendant of the given <code>root</code>.
     * If <code>virtualDescendantId</code> equals to {@link View#NO_ID} the root
     * is set as the parent.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setParent(View root, int virtualDescendantId) {
        mParentVirtualDescendantId = virtualDescendantId;
        IMPL.setParent(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the node bounds in parent coordinates.
     *
     * @param outBounds The output node bounds.
     */
    public void getBoundsInParent(Rect outBounds) {
        mInfo.getBoundsInParent(outBounds);
    }

    /**
     * Sets the node bounds in parent coordinates.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param bounds The node bounds.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setBoundsInParent(Rect bounds) {
        mInfo.setBoundsInParent(bounds);
    }

    /**
     * Gets the node bounds in screen coordinates.
     *
     * @param outBounds The output node bounds.
     */
    public void getBoundsInScreen(Rect outBounds) {
        mInfo.getBoundsInScreen(outBounds);
    }

    /**
     * Sets the node bounds in screen coordinates.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param bounds The node bounds.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setBoundsInScreen(Rect bounds) {
        mInfo.setBoundsInScreen(bounds);
    }

    /**
     * Gets whether this node is checkable.
     *
     * @return True if the node is checkable.
     */
    public boolean isCheckable() {
        return mInfo.isCheckable();
    }

    /**
     * Sets whether this node is checkable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param checkable True if the node is checkable.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setCheckable(boolean checkable) {
        mInfo.setCheckable(checkable);
    }

    /**
     * Gets whether this node is checked.
     *
     * @return True if the node is checked.
     */
    public boolean isChecked() {
        return mInfo.isChecked();
    }

    /**
     * Sets whether this node is checked.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param checked True if the node is checked.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setChecked(boolean checked) {
        mInfo.setChecked(checked);
    }

    /**
     * Gets whether this node is focusable.
     *
     * @return True if the node is focusable.
     */
    public boolean isFocusable() {
        return mInfo.isFocusable();
    }

    /**
     * Sets whether this node is focusable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param focusable True if the node is focusable.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setFocusable(boolean focusable) {
        mInfo.setFocusable(focusable);
    }

    /**
     * Gets whether this node is focused.
     *
     * @return True if the node is focused.
     */
    public boolean isFocused() {
        return mInfo.isFocused();
    }

    /**
     * Sets whether this node is focused.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param focused True if the node is focused.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setFocused(boolean focused) {
        mInfo.setFocused(focused);
    }

    /**
     * Sets whether this node is visible to the user.
     *
     * @return Whether the node is visible to the user.
     */
    public boolean isVisibleToUser() {
        return IMPL.isVisibleToUser(mInfo);
    }

    /**
     * Sets whether this node is visible to the user.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param visibleToUser Whether the node is visible to the user.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setVisibleToUser(boolean visibleToUser) {
        IMPL.setVisibleToUser(mInfo, visibleToUser);
    }

    /**
     * Gets whether this node is accessibility focused.
     *
     * @return True if the node is accessibility focused.
     */
    public boolean isAccessibilityFocused() {
        return IMPL.isAccessibilityFocused(mInfo);
    }

    /**
     * Sets whether this node is accessibility focused.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param focused True if the node is accessibility focused.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setAccessibilityFocused(boolean focused) {
        IMPL.setAccessibilityFocused(mInfo, focused);
    }

    /**
     * Gets whether this node is selected.
     *
     * @return True if the node is selected.
     */
    public boolean isSelected() {
        return mInfo.isSelected();
    }

    /**
     * Sets whether this node is selected.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param selected True if the node is selected.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setSelected(boolean selected) {
        mInfo.setSelected(selected);
    }

    /**
     * Gets whether this node is clickable.
     *
     * @return True if the node is clickable.
     */
    public boolean isClickable() {
        return mInfo.isClickable();
    }

    /**
     * Sets whether this node is clickable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param clickable True if the node is clickable.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setClickable(boolean clickable) {
        mInfo.setClickable(clickable);
    }

    /**
     * Gets whether this node is long clickable.
     *
     * @return True if the node is long clickable.
     */
    public boolean isLongClickable() {
        return mInfo.isLongClickable();
    }

    /**
     * Sets whether this node is long clickable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param longClickable True if the node is long clickable.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setLongClickable(boolean longClickable) {
        mInfo.setLongClickable(longClickable);
    }

    /**
     * Gets whether this node is enabled.
     *
     * @return True if the node is enabled.
     */
    public boolean isEnabled() {
        return mInfo.isEnabled();
    }

    /**
     * Sets whether this node is enabled.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param enabled True if the node is enabled.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setEnabled(boolean enabled) {
        mInfo.setEnabled(enabled);
    }

    /**
     * Gets whether this node is a password.
     *
     * @return True if the node is a password.
     */
    public boolean isPassword() {
        return mInfo.isPassword();
    }

    /**
     * Sets whether this node is a password.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param password True if the node is a password.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setPassword(boolean password) {
        mInfo.setPassword(password);
    }

    /**
     * Gets if the node is scrollable.
     *
     * @return True if the node is scrollable, false otherwise.
     */
    public boolean isScrollable() {
        return mInfo.isScrollable();
    }

    /**
     * Sets if the node is scrollable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param scrollable True if the node is scrollable, false otherwise.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setScrollable(boolean scrollable) {
        mInfo.setScrollable(scrollable);
    }

    /**
     * Returns whether the node originates from a view considered important for accessibility.
     *
     * @return {@code true} if the node originates from a view considered important for
     *         accessibility, {@code false} otherwise
     *
     * @see View#isImportantForAccessibility()
     */
    public boolean isImportantForAccessibility() {
        return IMPL.isImportantForAccessibility(mInfo);
    }

    /**
     * Sets whether the node is considered important for accessibility.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param important {@code true} if the node is considered important for accessibility,
     *                  {@code false} otherwise
     */
    public void setImportantForAccessibility(boolean important) {
        IMPL.setImportantForAccessibility(mInfo, important);
    }

    /**
     * Gets the package this node comes from.
     *
     * @return The package name.
     */
    public CharSequence getPackageName() {
        return mInfo.getPackageName();
    }

    /**
     * Sets the package this node comes from.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param packageName The package name.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setPackageName(CharSequence packageName) {
        mInfo.setPackageName(packageName);
    }

    /**
     * Gets the class this node comes from.
     *
     * @return The class name.
     */
    public CharSequence getClassName() {
        return mInfo.getClassName();
    }

    /**
     * Sets the class this node comes from.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param className The class name.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setClassName(CharSequence className) {
        mInfo.setClassName(className);
    }

    /**
     * Gets the text of this node.
     *
     * @return The text.
     */
    public CharSequence getText() {
        return mInfo.getText();
    }

    /**
     * Sets the text of this node.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param text The text.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setText(CharSequence text) {
        mInfo.setText(text);
    }

    /**
     * Gets the content description of this node.
     *
     * @return The content description.
     */
    public CharSequence getContentDescription() {
        return mInfo.getContentDescription();
    }

    /**
     * Sets the content description of this node.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param contentDescription The content description.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setContentDescription(CharSequence contentDescription) {
        mInfo.setContentDescription(contentDescription);
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this function.
     *
     * @throws IllegalStateException If the info is already recycled.
     */
    public void recycle() {
        mInfo.recycle();
    }

    /**
     * Sets the fully qualified resource name of the source view's id.
     *
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param viewId The id resource name.
     */
    public void setViewIdResourceName(String viewId) {
        IMPL.setViewIdResourceName(mInfo, viewId);
    }

    /**
     * Gets the fully qualified resource name of the source view's id.
     *
     * <p>
     *   <strong>Note:</strong> The primary usage of this API is for UI test automation
     *   and in order to report the source view id of an {@link AccessibilityNodeInfoCompat}
     *   the client has to set the {@link AccessibilityServiceInfoCompat#FLAG_REPORT_VIEW_IDS}
     *   flag when configuring his {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @return The id resource name.
     */
    public String getViewIdResourceName() {
        return IMPL.getViewIdResourceName(mInfo);
    }

    /**
     * Gets the node's live region mode.
     * <p>
     * A live region is a node that contains information that is important for
     * the user and when it changes the user should be notified. For example,
     * in a login screen with a TextView that displays an "incorrect password"
     * notification, that view should be marked as a live region with mode
     * {@link ViewCompat#ACCESSIBILITY_LIVE_REGION_POLITE}.
     * <p>
     * It is the responsibility of the accessibility service to monitor
     * {@link AccessibilityEventCompat#TYPE_WINDOW_CONTENT_CHANGED} events
     * indicating changes to live region nodes and their children.
     *
     * @return The live region mode, or
     *         {@link ViewCompat#ACCESSIBILITY_LIVE_REGION_NONE} if the view is
     *         not a live region.
     * @see ViewCompat#getAccessibilityLiveRegion(View)
     */
    public int getLiveRegion() {
        return IMPL.getLiveRegion(mInfo);
    }

    /**
     * Sets the node's live region mode.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     *
     * @param mode The live region mode, or
     *        {@link ViewCompat#ACCESSIBILITY_LIVE_REGION_NONE} if the view is
     *        not a live region.
     * @see ViewCompat#setAccessibilityLiveRegion(View, int)
     */
    public void setLiveRegion(int mode) {
        IMPL.setLiveRegion(mInfo, mode);
    }

    /**
     * Get the drawing order of the view corresponding it this node.
     * <p>
     * Drawing order is determined only within the node's parent, so this index is only relative
     * to its siblings.
     * <p>
     * In some cases, the drawing order is essentially simultaneous, so it is possible for two
     * siblings to return the same value. It is also possible that values will be skipped.
     *
     * @return The drawing position of the view corresponding to this node relative to its siblings.
     */
    public int getDrawingOrder() {
        return IMPL.getDrawingOrder(mInfo);
    }

    /**
     * Set the drawing order of the view corresponding it this node.
     *
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     * @param drawingOrderInParent
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setDrawingOrder(int drawingOrderInParent) {
        IMPL.setDrawingOrder(mInfo, drawingOrderInParent);
    }

    /**
     * Gets the collection info if the node is a collection. A collection
     * child is always a collection item.
     *
     * @return The collection info.
     */
    public CollectionInfoCompat getCollectionInfo() {
        Object info = IMPL.getCollectionInfo(mInfo);
        if (info == null) return null;
        return new CollectionInfoCompat(info);
    }

    public void setCollectionInfo(Object collectionInfo) {
        IMPL.setCollectionInfo(mInfo, ((CollectionInfoCompat) collectionInfo).mInfo);
    }

    public void setCollectionItemInfo(Object collectionItemInfo) {
        IMPL.setCollectionItemInfo(mInfo, ((CollectionItemInfoCompat) collectionItemInfo).mInfo);
    }

    /**
     * Gets the collection item info if the node is a collection item. A collection
     * item is always a child of a collection.
     *
     * @return The collection item info.
     */
    public CollectionItemInfoCompat getCollectionItemInfo() {
        Object info = IMPL.getCollectionItemInfo(mInfo);
        if (info == null) return null;
        return new CollectionItemInfoCompat(info);
    }

    /**
     * Gets the range info if this node is a range.
     *
     * @return The range.
     */
    public RangeInfoCompat getRangeInfo() {
        Object info = IMPL.getRangeInfo(mInfo);
        if (info == null) return null;
        return new RangeInfoCompat(info);
    }

    /**
     * Sets the range info if this node is a range.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param rangeInfo The range info.
     */
    public void setRangeInfo(RangeInfoCompat rangeInfo) {
        IMPL.setRangeInfo(mInfo, rangeInfo.mInfo);
    }

    /**
     * Gets the actions that can be performed on the node.
     *
     * @return A list of AccessibilityActions.
     */
    public List<AccessibilityActionCompat> getActionList() {
        List<Object> actions = IMPL.getActionList(mInfo);
        if (actions != null) {
            List<AccessibilityActionCompat> result = new ArrayList<AccessibilityActionCompat>();
            final int actionCount = actions.size();
            for (int i = 0; i < actionCount; i++) {
                Object action = actions.get(i);
                result.add(new AccessibilityActionCompat(action));
            }
            return result;
        } else {
            return Collections.<AccessibilityActionCompat>emptyList();
        }
    }

    /**
     * Sets if the content of this node is invalid. For example,
     * a date is not well-formed.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param contentInvalid If the node content is invalid.
     */
    public void setContentInvalid(boolean contentInvalid) {
        IMPL.setContentInvalid(mInfo, contentInvalid);
    }

    /**
     * Gets if the content of this node is invalid. For example,
     * a date is not well-formed.
     *
     * @return If the node content is invalid.
     */
    public boolean isContentInvalid() {
        return IMPL.isContentInvalid(mInfo);
    }

    /**
     * Gets whether this node is context clickable.
     *
     * @return True if the node is context clickable.
     */
    public boolean isContextClickable() {
        return IMPL.isContextClickable(mInfo);
    }

    /**
     * Sets whether this node is context clickable.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is made immutable
     * before being delivered to an AccessibilityService.
     * </p>
     *
     * @param contextClickable True if the node is context clickable.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setContextClickable(boolean contextClickable) {
        IMPL.setContextClickable(mInfo, contextClickable);
    }

    /**
     * Sets the error text of this node.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param error The error text.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setError(CharSequence error) {
        IMPL.setError(mInfo, error);
    }

    /**
     * Gets the error text of this node.
     *
     * @return The error text.
     */
    public CharSequence getError() {
        return IMPL.getError(mInfo);
    }

    /**
     * Sets the view for which the view represented by this info serves as a
     * label for accessibility purposes.
     *
     * @param labeled The view for which this info serves as a label.
     */
    public void setLabelFor(View labeled) {
        IMPL.setLabelFor(mInfo, labeled);
    }

    /**
     * Sets the view for which the view represented by this info serves as a
     * label for accessibility purposes. If <code>virtualDescendantId</code>
     * is {@link View#NO_ID} the root is set as the labeled.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report themselves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     *
     * @param root The root whose virtual descendant serves as a label.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setLabelFor(View root, int virtualDescendantId) {
        IMPL.setLabelFor(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the node info for which the view represented by this info serves as
     * a label for accessibility purposes.
     * <p>
     *   <strong>Note:</strong> It is a client responsibility to recycle the
     *     received info by calling {@link AccessibilityNodeInfoCompat#recycle()}
     *     to avoid creating of multiple instances.
     * </p>
     *
     * @return The labeled info.
     */
    public AccessibilityNodeInfoCompat getLabelFor() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getLabelFor(mInfo));
    }

    /**
     * Sets the view which serves as the label of the view represented by
     * this info for accessibility purposes.
     *
     * @param label The view that labels this node's source.
     */
    public void setLabeledBy(View label) {
        IMPL.setLabeledBy(mInfo, label);
    }

    /**
     * Sets the view which serves as the label of the view represented by
     * this info for accessibility purposes. If <code>virtualDescendantId</code>
     * is {@link View#NO_ID} the root is set as the label.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report themselves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param root The root whose virtual descendant labels this node's source.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setLabeledBy(View root, int virtualDescendantId) {
        IMPL.setLabeledBy(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the node info which serves as the label of the view represented by
     * this info for accessibility purposes.
     * <p>
     *   <strong>Note:</strong> It is a client responsibility to recycle the
     *     received info by calling {@link AccessibilityNodeInfoCompat#recycle()}
     *     to avoid creating of multiple instances.
     * </p>
     *
     * @return The label.
     */
    public AccessibilityNodeInfoCompat getLabeledBy() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getLabeledBy(mInfo));
    }

    /**
     * Gets if this node opens a popup or a dialog.
     *
     * @return If the the node opens a popup.
     */
    public boolean canOpenPopup() {
        return IMPL.canOpenPopup(mInfo);
    }

    /**
     * Sets if this node opens a popup or a dialog.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param opensPopup If the the node opens a popup.
     */
    public void setCanOpenPopup(boolean opensPopup) {
        IMPL.setCanOpenPopup(mInfo, opensPopup);
    }

    /**
     * Finds {@link AccessibilityNodeInfoCompat}s by the fully qualified view id's resource
     * name where a fully qualified id is of the from "package:id/id_resource_name".
     * For example, if the target application's package is "foo.bar" and the id
     * resource name is "baz", the fully qualified resource id is "foo.bar:id/baz".
     *
     * <p>
     *   <strong>Note:</strong> It is a client responsibility to recycle the
     *     received info by calling {@link AccessibilityNodeInfoCompat#recycle()}
     *     to avoid creating of multiple instances.
     * </p>
     * <p>
     *   <strong>Note:</strong> The primary usage of this API is for UI test automation
     *   and in order to report the fully qualified view id if an
     *   {@link AccessibilityNodeInfoCompat} the client has to set the
     *   {@link android.accessibilityservice.AccessibilityServiceInfo#FLAG_REPORT_VIEW_IDS}
     *   flag when configuring his {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @param viewId The fully qualified resource name of the view id to find.
     * @return A list of node info.
     */
    public List<AccessibilityNodeInfoCompat> findAccessibilityNodeInfosByViewId(String viewId) {
        List<AccessibilityNodeInfo> nodes = IMPL.findAccessibilityNodeInfosByViewId(mInfo, viewId);
        if (nodes != null) {
            List<AccessibilityNodeInfoCompat> result = new ArrayList<AccessibilityNodeInfoCompat>();
            for (AccessibilityNodeInfo node : nodes) {
                result.add(AccessibilityNodeInfoCompat.wrap(node));
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Gets an optional bundle with extra data. The bundle
     * is lazily created and never <code>null</code>.
     * <p>
     * <strong>Note:</strong> It is recommended to use the package
     * name of your application as a prefix for the keys to avoid
     * collisions which may confuse an accessibility service if the
     * same key has different meaning when emitted from different
     * applications.
     * </p>
     *
     * @return The bundle.
     */
    public Bundle getExtras() {
        return IMPL.getExtras(mInfo);
    }

    /**
     * Gets the input type of the source as defined by {@link InputType}.
     *
     * @return The input type.
     */
    public int getInputType() {
        return IMPL.getInputType(mInfo);
    }

    /**
     * Sets the input type of the source as defined by {@link InputType}.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an
     *   AccessibilityService.
     * </p>
     *
     * @param inputType The input type.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setInputType(int inputType) {
        IMPL.setInputType(mInfo, inputType);
    }

    /**
     * Sets the maximum text length, or -1 for no limit.
     * <p>
     * Typically used to indicate that an editable text field has a limit on
     * the number of characters entered.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}.
     * This class is made immutable before being delivered to an AccessibilityService.
     *
     * @param max The maximum text length.
     * @see #getMaxTextLength()
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setMaxTextLength(int max) {
        IMPL.setMaxTextLength(mInfo, max);
    }

    /**
     * Returns the maximum text length for this node.
     *
     * @return The maximum text length, or -1 for no limit.
     * @see #setMaxTextLength(int)
     */
    public int getMaxTextLength() {
        return IMPL.getMaxTextLength(mInfo);
    }

    /**
     * Sets the text selection start and end.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param start The text selection start.
     * @param end The text selection end.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setTextSelection(int start, int end) {
        IMPL.setTextSelection(mInfo, start, end);
    }

    /**
     * Gets the text selection start.
     *
     * @return The text selection start if there is selection or -1.
     */
    public int getTextSelectionStart() {
        return IMPL.getTextSelectionStart(mInfo);
    }

    /**
     * Gets the text selection end.
     *
     * @return The text selection end if there is selection or -1.
     */
    public int getTextSelectionEnd() {
        return IMPL.getTextSelectionEnd(mInfo);
    }

    /**
     * Gets the node before which this one is visited during traversal. A screen-reader
     * must visit the content of this node before the content of the one it precedes.
     *
     * @return The succeeding node if such or <code>null</code>.
     *
     * @see #setTraversalBefore(android.view.View)
     * @see #setTraversalBefore(android.view.View, int)
     */
    public AccessibilityNodeInfoCompat getTraversalBefore() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getTraversalBefore(mInfo));
    }

    /**
     * Sets the view before whose node this one should be visited during traversal. A
     * screen-reader must visit the content of this node before the content of the one
     * it precedes.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param view The view providing the preceding node.
     *
     * @see #getTraversalBefore()
     */
    public void setTraversalBefore(View view) {
        IMPL.setTraversalBefore(mInfo, view);
    }

    /**
     * Sets the node before which this one is visited during traversal. A screen-reader
     * must visit the content of this node before the content of the one it precedes.
     * The successor is a virtual descendant of the given <code>root</code>. If
     * <code>virtualDescendantId</code> equals to {@link View#NO_ID} the root is set
     * as the successor.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setTraversalBefore(View root, int virtualDescendantId) {
        IMPL.setTraversalBefore(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the node after which this one is visited in accessibility traversal.
     * A screen-reader must visit the content of the other node before the content
     * of this one.
     *
     * @return The succeeding node if such or <code>null</code>.
     *
     * @see #setTraversalAfter(android.view.View)
     * @see #setTraversalAfter(android.view.View, int)
     */
    public AccessibilityNodeInfoCompat getTraversalAfter() {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getTraversalAfter(mInfo));
    }

    /**
     * Sets the view whose node is visited after this one in accessibility traversal.
     * A screen-reader must visit the content of the other node before the content
     * of this one.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param view The previous view.
     *
     * @see #getTraversalAfter()
     */
    public void setTraversalAfter(View view) {
        IMPL.setTraversalAfter(mInfo, view);
    }

    /**
     * Sets the node after which this one is visited in accessibility traversal.
     * A screen-reader must visit the content of the other node before the content
     * of this one. If <code>virtualDescendantId</code> equals to {@link View#NO_ID}
     * the root is set as the predecessor.
     * <p>
     * A virtual descendant is an imaginary View that is reported as a part of the view
     * hierarchy for accessibility purposes. This enables custom views that draw complex
     * content to report them selves as a tree of virtual views, thus conveying their
     * logical structure.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setTraversalAfter(View root, int virtualDescendantId) {
        IMPL.setTraversalAfter(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the window to which this node belongs.
     *
     * @return The window.
     *
     * @see android.accessibilityservice.AccessibilityService#getWindows()
     */
    public AccessibilityWindowInfoCompat getWindow() {
        return AccessibilityWindowInfoCompat.wrapNonNullInstance(IMPL.getWindow(mInfo));
    }

    /**
     * Gets if the node can be dismissed.
     *
     * @return If the node can be dismissed.
     */
    public boolean isDismissable() {
        return IMPL.isDismissable(mInfo);
    }

    /**
     * Sets if the node can be dismissed.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param dismissable If the node can be dismissed.
     */
    public void setDismissable(boolean dismissable) {
        IMPL.setDismissable(mInfo, dismissable);
    }

    /**
     * Gets if the node is editable.
     *
     * @return True if the node is editable, false otherwise.
     */
    public boolean isEditable() {
        return IMPL.isEditable(mInfo);
    }

    /**
     * Sets whether this node is editable.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param editable True if the node is editable.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setEditable(boolean editable) {
        IMPL.setEditable(mInfo, editable);
    }

    /**
     * Gets if the node is a multi line editable text.
     *
     * @return True if the node is multi line.
     */
    public boolean isMultiLine() {
        return IMPL.isMultiLine(mInfo);
    }

    /**
     * Sets if the node is a multi line editable text.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param multiLine True if the node is multi line.
     */
    public void setMultiLine(boolean multiLine) {
        IMPL.setMultiLine(mInfo, multiLine);
    }

    /**
     * Refreshes this info with the latest state of the view it represents.
     * <p>
     * <strong>Note:</strong> If this method returns false this info is obsolete
     * since it represents a view that is no longer in the view tree and should
     * be recycled.
     * </p>
     * @return Whether the refresh succeeded.
     */
    public boolean refresh() {
        return IMPL.refresh(mInfo);
    }

    /**
     * Gets the custom role description.
     * @return The role description.
     */
    public @Nullable CharSequence getRoleDescription() {
        return IMPL.getRoleDescription(mInfo);
    }

    /**
     * Sets the custom role description.
     *
     * <p>
     *   The role description allows you to customize the name for the view's semantic
     *   role. For example, if you create a custom subclass of {@link android.view.View}
     *   to display a menu bar, you could assign it the role description of "menu bar".
     * </p>
     * <p>
     *   <strong>Warning:</strong> For consistency with other applications, you should
     *   not use the role description to force accessibility services to describe
     *   standard views (such as buttons or checkboxes) using specific wording. For
     *   example, you should not set a role description of "check box" or "tick box" for
     *   a standard {@link android.widget.CheckBox}. Instead let accessibility services
     *   decide what feedback to provide.
     * </p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param roleDescription The role description.
     */
    public void setRoleDescription(@Nullable CharSequence roleDescription) {
        IMPL.setRoleDescription(mInfo, roleDescription);
    }

    @Override
    public int hashCode() {
        return (mInfo == null) ? 0 : mInfo.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AccessibilityNodeInfoCompat other = (AccessibilityNodeInfoCompat) obj;
        if (mInfo == null) {
            if (other.mInfo != null) {
                return false;
            }
        } else if (!mInfo.equals(other.mInfo)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());

        Rect bounds = new Rect();

        getBoundsInParent(bounds);
        builder.append("; boundsInParent: " + bounds);

        getBoundsInScreen(bounds);
        builder.append("; boundsInScreen: " + bounds);

        builder.append("; packageName: ").append(getPackageName());
        builder.append("; className: ").append(getClassName());
        builder.append("; text: ").append(getText());
        builder.append("; contentDescription: ").append(getContentDescription());
        builder.append("; viewId: ").append(getViewIdResourceName());

        builder.append("; checkable: ").append(isCheckable());
        builder.append("; checked: ").append(isChecked());
        builder.append("; focusable: ").append(isFocusable());
        builder.append("; focused: ").append(isFocused());
        builder.append("; selected: ").append(isSelected());
        builder.append("; clickable: ").append(isClickable());
        builder.append("; longClickable: ").append(isLongClickable());
        builder.append("; enabled: ").append(isEnabled());
        builder.append("; password: ").append(isPassword());
        builder.append("; scrollable: " + isScrollable());

        builder.append("; [");
        for (int actionBits = getActions(); actionBits != 0;) {
            final int action = 1 << Integer.numberOfTrailingZeros(actionBits);
            actionBits &= ~action;
            builder.append(getActionSymbolicName(action));
            if (actionBits != 0) {
                builder.append(", ");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    private static String getActionSymbolicName(int action) {
        switch (action) {
            case ACTION_FOCUS:
                return "ACTION_FOCUS";
            case ACTION_CLEAR_FOCUS:
                return "ACTION_CLEAR_FOCUS";
            case ACTION_SELECT:
                return "ACTION_SELECT";
            case ACTION_CLEAR_SELECTION:
                return "ACTION_CLEAR_SELECTION";
            case ACTION_CLICK:
                return "ACTION_CLICK";
            case ACTION_LONG_CLICK:
                return "ACTION_LONG_CLICK";
            case ACTION_ACCESSIBILITY_FOCUS:
                return "ACTION_ACCESSIBILITY_FOCUS";
            case ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                return "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
            case ACTION_NEXT_AT_MOVEMENT_GRANULARITY:
                return "ACTION_NEXT_AT_MOVEMENT_GRANULARITY";
            case ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY:
                return "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY";
            case ACTION_NEXT_HTML_ELEMENT:
                return "ACTION_NEXT_HTML_ELEMENT";
            case ACTION_PREVIOUS_HTML_ELEMENT:
                return "ACTION_PREVIOUS_HTML_ELEMENT";
            case ACTION_SCROLL_FORWARD:
                return "ACTION_SCROLL_FORWARD";
            case ACTION_SCROLL_BACKWARD:
                return "ACTION_SCROLL_BACKWARD";
            case ACTION_CUT:
                return "ACTION_CUT";
            case ACTION_COPY:
                return "ACTION_COPY";
            case ACTION_PASTE:
                return "ACTION_PASTE";
            case ACTION_SET_SELECTION:
                return "ACTION_SET_SELECTION";
            default:
                return"ACTION_UNKNOWN";
        }
    }
}
