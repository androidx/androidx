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

import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.accessibilityservice.AccessibilityServiceInfoCompat;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeInfo}
 * introduced after API level 4 in a backwards compatible fashion.
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

        private final Object mAction;

        /**
         * Creates a new instance.
         *
         * @param actionId The action id.
         * @param label The action label.
         */
        public AccessibilityActionCompat(int actionId, CharSequence label) {
            this(IMPL.newAccessibilityAction(actionId, label));
        }

        private AccessibilityActionCompat(Object action) {
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
         * @return An instance.
         */
        public static CollectionInfoCompat obtain(int rowCount, int columnCount,
                boolean hierarchical, int selectionMode) {
            return new CollectionInfoCompat(IMPL.obtainCollectionInfo(rowCount, columnCount,
                    hierarchical, selectionMode));
        }

        private CollectionInfoCompat(Object info) {
            mInfo = info;
        }

        public int getColumnCount() {
            return IMPL.getCollectionInfoColumnCount(mInfo);
        }

        public int getRowCount() {
            return IMPL.getCollectionInfoRowCount(mInfo);
        }

        public boolean isHierarchical() {
            return IMPL.isCollectionInfoHierarchical(mInfo);
        }
    }

    public static class CollectionItemInfoCompat {

        private final Object mInfo;

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @return An instance.
         */
        public static CollectionItemInfoCompat obtain(int rowIndex, int rowSpan,
                int columnIndex, int columnSpan, boolean heading, boolean selected) {
            return new CollectionItemInfoCompat(IMPL.obtainCollectionItemInfo(rowIndex, rowSpan,
                    columnIndex, columnSpan, heading, selected));
        }

        private CollectionItemInfoCompat(Object info) {
            mInfo = info;
        }

        public int getColumnIndex() {
            return IMPL.getCollectionItemColumnIndex(mInfo);
        }

        public int getColumnSpan() {
            return IMPL.getCollectionItemColumnSpan(mInfo);
        }

        public int getRowIndex() {
            return IMPL.getCollectionItemRowIndex(mInfo);
        }

        public int getRowSpan() {
            return IMPL.getCollectionItemRowSpan(mInfo);
        }

        public boolean isHeading() {
            return IMPL.isCollectionItemHeading(mInfo);
        }

        public boolean isSelected() {
            return IMPL.isCollectionItemSelected(mInfo);
        }
    }

    public static class RangeInfoCompat {
        /** Range type: integer. */
        public static final int RANGE_TYPE_INT = 0;
        /** Range type: float. */
        public static final int RANGE_TYPE_FLOAT = 1;
        /** Range type: percent with values from zero to one.*/
        public static final int RANGE_TYPE_PERCENT = 2;

        private final Object mInfo;

        private RangeInfoCompat(Object info) {
            mInfo = info;
        }

        public float getCurrent() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getCurrent(mInfo);
        }

        public float getMax() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getMax(mInfo);
        }

        public float getMin() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getMin(mInfo);
        }

        public int getType() {
            return AccessibilityNodeInfoCompatKitKat.RangeInfo.getType(mInfo);
        }
    }

    static interface AccessibilityNodeInfoImpl {
        public Object newAccessibilityAction(int actionId, CharSequence label);
        public Object obtain();
        public Object obtain(View source);
        public Object obtain(Object info);
        public Object obtain(View root, int virtualDescendantId);
        public void setSource(Object info, View source);
        public void setSource(Object info, View root, int virtualDescendantId);
        public Object findFocus(Object info, int focus);
        public Object focusSearch(Object info, int direction);
        public int getWindowId(Object info);
        public int getChildCount(Object info);
        public Object getChild(Object info, int index);
        public void addChild(Object info, View child);
        public void addChild(Object info, View child, int virtualDescendantId);
        public boolean removeChild(Object info, View child);
        public boolean removeChild(Object info, View root, int virtualDescendantId);
        public int getActions(Object info);
        public void addAction(Object info, int action);
        public void addAction(Object info, Object action);
        public boolean removeAction(Object info, Object action);
        public int getAccessibilityActionId(Object action);
        public CharSequence getAccessibilityActionLabel(Object action);
        public boolean performAction(Object info, int action);
        public boolean performAction(Object info, int action, Bundle arguments);
        public void setMovementGranularities(Object info, int granularities);
        public int getMovementGranularities(Object info);
        public List<Object> findAccessibilityNodeInfosByText(Object info, String text);
        public Object getParent(Object info);
        public void setParent(Object info, View root, int virtualDescendantId);
        public void setParent(Object info, View parent);
        public void getBoundsInParent(Object info, Rect outBounds);
        public void setBoundsInParent(Object info, Rect bounds);
        public void getBoundsInScreen(Object info, Rect outBounds);
        public void setBoundsInScreen(Object info, Rect bounds);
        public boolean isCheckable(Object info);
        public void setCheckable(Object info, boolean checkable);
        public boolean isChecked(Object info);
        public void setChecked(Object info, boolean checked);
        public boolean isFocusable(Object info);
        public void setFocusable(Object info, boolean focusable);
        public boolean isFocused(Object info);
        public void setFocused(Object info, boolean focused);
        public boolean isVisibleToUser(Object info);
        public void setVisibleToUser(Object info, boolean visibleToUser);
        public boolean isAccessibilityFocused(Object info);
        public void setAccessibilityFocused(Object info, boolean focused);
        public boolean isSelected(Object info);
        public void setSelected(Object info, boolean selected);
        public boolean isClickable(Object info);
        public void setClickable(Object info, boolean clickable);
        public boolean isLongClickable(Object info);
        public void setLongClickable(Object info, boolean longClickable);
        public boolean isEnabled(Object info);
        public void setEnabled(Object info, boolean enabled);
        public boolean isPassword(Object info);
        public void setPassword(Object info, boolean password);
        public boolean isScrollable(Object info);
        public void setScrollable(Object info, boolean scrollable);
        public CharSequence getPackageName(Object info);
        public void setPackageName(Object info, CharSequence packageName);
        public CharSequence getClassName(Object info);
        public void setClassName(Object info, CharSequence className);
        public CharSequence getText(Object info);
        public void setText(Object info, CharSequence text);
        public CharSequence getContentDescription(Object info);
        public void setContentDescription(Object info, CharSequence contentDescription);
        public void recycle(Object info);
        public String getViewIdResourceName(Object info);
        public void setViewIdResourceName(Object info, String viewId);
        public int getLiveRegion(Object info);
        public void setLiveRegion(Object info, int mode);
        public Object getCollectionInfo(Object info);
        public void setCollectionInfo(Object info, Object collectionInfo);
        public Object getCollectionItemInfo(Object info);
        public void setCollectionItemInfo(Object info, Object collectionItemInfo);
        public Object getRangeInfo(Object info);
        public void setRangeInfo(Object info, Object rangeInfo);
        public List<Object> getActionList(Object info);
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode);
        public int getCollectionInfoColumnCount(Object info);
        public int getCollectionInfoRowCount(Object info);
        public boolean isCollectionInfoHierarchical(Object info);
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected);
        public int getCollectionItemColumnIndex(Object info);
        public int getCollectionItemColumnSpan(Object info);
        public int getCollectionItemRowIndex(Object info);
        public int getCollectionItemRowSpan(Object info);
        public boolean isCollectionItemHeading(Object info);
        public boolean isCollectionItemSelected(Object info);
        public Object getTraversalBefore(Object info);
        public void setTraversalBefore(Object info, View view);
        public void setTraversalBefore(Object info, View root, int virtualDescendantId);
        public Object getTraversalAfter(Object info);
        public void setTraversalAfter(Object info, View view);
        public void setTraversalAfter(Object info, View root, int virtualDescendantId);
        public void setContentInvalid(Object info, boolean contentInvalid);
        public boolean isContentInvalid(Object info);
        public void setError(Object info, CharSequence error);
        public CharSequence getError(Object info);
        public void setLabelFor(Object info, View labeled);
        public void setLabelFor(Object info, View root, int virtualDescendantId);
        public Object getLabelFor(Object info);
        public void setLabeledBy(Object info, View labeled);
        public void setLabeledBy(Object info, View root, int virtualDescendantId);
        public Object getLabeledBy(Object info);
        public boolean canOpenPopup(Object info);
        public void setCanOpenPopup(Object info, boolean opensPopup);
        public List<Object> findAccessibilityNodeInfosByViewId(Object info, String viewId);
        public Bundle getExtras(Object info);
        public int getInputType(Object info);
        public void setInputType(Object info, int inputType);
        public void setMaxTextLength(Object info, int max);
        public int getMaxTextLength(Object info);
        public void setTextSelection(Object info, int start, int end);
        public int getTextSelectionStart(Object info);
        public int getTextSelectionEnd(Object info);
        public Object getWindow(Object info);
        public boolean isDismissable(Object info);
        public void setDismissable(Object info, boolean dismissable);
        public boolean isEditable(Object info);
        public void setEditable(Object info, boolean editable);
        public boolean isMultiLine(Object info);
        public void setMultiLine(Object info, boolean multiLine);
        public boolean refresh(Object info);
    }

    static class AccessibilityNodeInfoStubImpl implements AccessibilityNodeInfoImpl {
        @Override
        public Object newAccessibilityAction(int actionId, CharSequence label) {
            return null;
        }

        @Override
        public Object obtain() {
            return null;
        }

        @Override
        public Object obtain(View source) {
            return null;
        }

        @Override
        public Object obtain(View root, int virtualDescendantId) {
            return null;
        }

        @Override
        public Object obtain(Object info) {
            return null;
        }

        @Override
        public void addAction(Object info, int action) {

        }

        @Override
        public void addAction(Object info, Object action) {

        }

        @Override
        public boolean removeAction(Object info, Object action) {
            return false;
        }

        @Override
        public int getAccessibilityActionId(Object action) {
            return 0;
        }

        @Override
        public CharSequence getAccessibilityActionLabel(Object action) {
            return null;
        }

        @Override
        public void addChild(Object info, View child) {

        }

        @Override
        public void addChild(Object info, View child, int virtualDescendantId) {

        }

        @Override
        public boolean removeChild(Object info, View child) {
            return false;
        }

        @Override
        public boolean removeChild(Object info, View root, int virtualDescendantId) {
            return false;
        }

        @Override
        public List<Object> findAccessibilityNodeInfosByText(Object info, String text) {
            return Collections.emptyList();
        }

        @Override
        public int getActions(Object info) {
            return 0;
        }

        @Override
        public void getBoundsInParent(Object info, Rect outBounds) {

        }

        @Override
        public void getBoundsInScreen(Object info, Rect outBounds) {

        }

        @Override
        public Object getChild(Object info, int index) {
            return null;
        }

        @Override
        public int getChildCount(Object info) {
            return 0;
        }

        @Override
        public CharSequence getClassName(Object info) {
            return null;
        }

        @Override
        public CharSequence getContentDescription(Object info) {
            return null;
        }

        @Override
        public CharSequence getPackageName(Object info) {
            return null;
        }

        @Override
        public Object getParent(Object info) {
            return null;
        }

        @Override
        public CharSequence getText(Object info) {
            return null;
        }

        @Override
        public int getWindowId(Object info) {
            return 0;
        }

        @Override
        public boolean isCheckable(Object info) {
            return false;
        }

        @Override
        public boolean isChecked(Object info) {
            return false;
        }

        @Override
        public boolean isClickable(Object info) {
            return false;
        }

        @Override
        public boolean isEnabled(Object info) {
            return false;
        }

        @Override
        public boolean isFocusable(Object info) {
            return false;
        }

        @Override
        public boolean isFocused(Object info) {
            return false;
        }

        @Override
        public boolean isVisibleToUser(Object info) {
            return false;
        }

        @Override
        public boolean isAccessibilityFocused(Object info) {
            return false;
        }

        @Override
        public boolean isLongClickable(Object info) {
            return false;
        }

        @Override
        public boolean isPassword(Object info) {
            return false;
        }

        @Override
        public boolean isScrollable(Object info) {
            return false;
        }

        @Override
        public boolean isSelected(Object info) {
            return false;
        }

        @Override
        public boolean performAction(Object info, int action) {
            return false;
        }

        @Override
        public boolean performAction(Object info, int action, Bundle arguments) {
            return false;
        }

        @Override
        public void setMovementGranularities(Object info, int granularities) {

        }

        @Override
        public int getMovementGranularities(Object info) {
            return 0;
        }

        @Override
        public void setBoundsInParent(Object info, Rect bounds) {

        }

        @Override
        public void setBoundsInScreen(Object info, Rect bounds) {

        }

        @Override
        public void setCheckable(Object info, boolean checkable) {

        }

        @Override
        public void setChecked(Object info, boolean checked) {

        }

        @Override
        public void setClassName(Object info, CharSequence className) {

        }

        @Override
        public void setClickable(Object info, boolean clickable) {

        }

        @Override
        public void setContentDescription(Object info, CharSequence contentDescription) {

        }

        @Override
        public void setEnabled(Object info, boolean enabled) {

        }

        @Override
        public void setFocusable(Object info, boolean focusable) {

        }

        @Override
        public void setFocused(Object info, boolean focused) {

        }

        @Override
        public void setVisibleToUser(Object info, boolean visibleToUser) {

        }

        @Override
        public void setAccessibilityFocused(Object info, boolean focused) {

        }

        @Override
        public void setLongClickable(Object info, boolean longClickable) {

        }

        @Override
        public void setPackageName(Object info, CharSequence packageName) {

        }

        @Override
        public void setParent(Object info, View parent) {

        }

        @Override
        public void setPassword(Object info, boolean password) {

        }

        @Override
        public void setScrollable(Object info, boolean scrollable) {

        }

        @Override
        public void setSelected(Object info, boolean selected) {

        }

        @Override
        public void setSource(Object info, View source) {

        }

        @Override
        public void setSource(Object info, View root, int virtualDescendantId) {

        }

        @Override
        public Object findFocus(Object info, int focus) {
            return null;
        }

        @Override
        public Object focusSearch(Object info, int direction) {
            return null;
        }

        @Override
        public void setText(Object info, CharSequence text) {

        }

        @Override
        public void recycle(Object info) {

        }

        @Override
        public void setParent(Object info, View root, int virtualDescendantId) {

        }

        @Override
        public String getViewIdResourceName(Object info) {
            return null;
        }

        @Override
        public void setViewIdResourceName(Object info, String viewId) {

        }

        @Override
        public int getLiveRegion(Object info) {
            return ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE;
        }

        @Override
        public void setLiveRegion(Object info, int mode) {
            // No-op
        }

        @Override
        public Object getCollectionInfo(Object info) {
            return null;
        }

        @Override
        public void setCollectionInfo(Object info, Object collectionInfo) {
        }

        @Override
        public Object getCollectionItemInfo(Object info) {
            return null;
        }

        @Override
        public void setCollectionItemInfo(Object info, Object collectionItemInfo) {
        }

        @Override
        public Object getRangeInfo(Object info) {
            return null;
        }

        @Override
        public void setRangeInfo(Object info, Object rangeInfo) {
        }

        @Override
        public List<Object> getActionList(Object info) {
            return null;
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode) {
            return null;
        }

        @Override
        public int getCollectionInfoColumnCount(Object info) {
            return 0;
        }

        @Override
        public int getCollectionInfoRowCount(Object info) {
            return 0;
        }

        @Override
        public boolean isCollectionInfoHierarchical(Object info) {
            return false;
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return null;
        }

        @Override
        public int getCollectionItemColumnIndex(Object info) {
            return 0;
        }

        @Override
        public int getCollectionItemColumnSpan(Object info) {
            return 0;
        }

        @Override
        public int getCollectionItemRowIndex(Object info) {
            return 0;
        }

        @Override
        public int getCollectionItemRowSpan(Object info) {
            return 0;
        }

        @Override
        public boolean isCollectionItemHeading(Object info) {
            return false;
        }

        @Override
        public boolean isCollectionItemSelected(Object info) {
            return false;
        }

        @Override
        public Object getTraversalBefore(Object info) {
            return null;
        }

        @Override
        public void setTraversalBefore(Object info, View view) {
        }

        @Override
        public void setTraversalBefore(Object info, View root, int virtualDescendantId) {
        }

        @Override
        public Object getTraversalAfter(Object info) {
            return null;
        }

        @Override
        public void setTraversalAfter(Object info, View view) {
        }

        @Override
        public void setTraversalAfter(Object info, View root, int virtualDescendantId) {
        }

        @Override
        public void setContentInvalid(Object info, boolean contentInvalid) {
        }

        @Override
        public boolean isContentInvalid(Object info) {
            return false;
        }

        @Override
        public void setError(Object info, CharSequence error) {
        }

        @Override
        public CharSequence getError(Object info) {
            return null;
        }

        @Override
        public void setLabelFor(Object info, View labeled) {
        }

        @Override
        public void setLabelFor(Object info, View root, int virtualDescendantId) {
        }

        @Override
        public Object getLabelFor(Object info) {
            return null;
        }

        @Override
        public void setLabeledBy(Object info, View labeled) {
        }

        @Override
        public void setLabeledBy(Object info, View root, int virtualDescendantId) {
        }

        @Override
        public Object getLabeledBy(Object info){
            return null;
        }

        @Override
        public boolean canOpenPopup(Object info) {
            return false;
        }

        @Override
        public void setCanOpenPopup(Object info, boolean opensPopup) {
        }

        @Override
        public List<Object> findAccessibilityNodeInfosByViewId(Object info, String viewId) {
            return  Collections.emptyList();
        }

        @Override
        public Bundle getExtras(Object info) {
            return new Bundle();
        }

        @Override
        public int getInputType(Object info) {
            return InputType.TYPE_NULL;
        }

        @Override
        public void setInputType(Object info, int inputType) {
        }

        @Override
        public void setMaxTextLength(Object info, int max) {
        }

        @Override
        public int getMaxTextLength(Object info) {
            return -1;
        }

        @Override
        public void setTextSelection(Object info, int start, int end) {
        }

        @Override
        public int getTextSelectionStart(Object info) {
            return -1;
        }

        @Override
        public int getTextSelectionEnd(Object info) {
            return -1;
        }

        @Override
        public Object getWindow(Object info) {
            return null;
        }

        @Override
        public boolean isDismissable(Object info) {
            return false;
        }

        @Override
        public void setDismissable(Object info, boolean dismissable) {
        }

        @Override
        public boolean isEditable(Object info) {
            return false;
        }

        @Override
        public void setEditable(Object info, boolean editable) {
        }

        @Override
        public boolean isMultiLine(Object info) {
            return false;
        }

        @Override
        public void setMultiLine(Object info, boolean multiLine) {
        }

        @Override
        public boolean refresh(Object info) {
            return false;
        }
    }

    static class AccessibilityNodeInfoIcsImpl extends AccessibilityNodeInfoStubImpl {
        @Override
        public Object obtain() {
            return AccessibilityNodeInfoCompatIcs.obtain();
        }

        @Override
        public Object obtain(View source) {
            return AccessibilityNodeInfoCompatIcs.obtain(source);
        }

        @Override
        public Object obtain(Object info) {
            return AccessibilityNodeInfoCompatIcs.obtain(info);
        }

        @Override
        public void addAction(Object info, int action) {
            AccessibilityNodeInfoCompatIcs.addAction(info, action);
        }

        @Override
        public void addChild(Object info, View child) {
            AccessibilityNodeInfoCompatIcs.addChild(info, child);
        }

        @Override
        public List<Object> findAccessibilityNodeInfosByText(Object info, String text) {
            return AccessibilityNodeInfoCompatIcs.findAccessibilityNodeInfosByText(info, text);
        }

        @Override
        public int getActions(Object info) {
            return AccessibilityNodeInfoCompatIcs.getActions(info);
        }

        @Override
        public void getBoundsInParent(Object info, Rect outBounds) {
            AccessibilityNodeInfoCompatIcs.getBoundsInParent(info, outBounds);
        }

        @Override
        public void getBoundsInScreen(Object info, Rect outBounds) {
            AccessibilityNodeInfoCompatIcs.getBoundsInScreen(info, outBounds);
        }

        @Override
        public Object getChild(Object info, int index) {
            return AccessibilityNodeInfoCompatIcs.getChild(info, index);
        }

        @Override
        public int getChildCount(Object info) {
            return AccessibilityNodeInfoCompatIcs.getChildCount(info);
        }

        @Override
        public CharSequence getClassName(Object info) {
            return AccessibilityNodeInfoCompatIcs.getClassName(info);
        }

        @Override
        public CharSequence getContentDescription(Object info) {
            return AccessibilityNodeInfoCompatIcs.getContentDescription(info);
        }

        @Override
        public CharSequence getPackageName(Object info) {
            return AccessibilityNodeInfoCompatIcs.getPackageName(info);
        }

        @Override
        public Object getParent(Object info) {
            return AccessibilityNodeInfoCompatIcs.getParent(info);
        }

        @Override
        public CharSequence getText(Object info) {
            return AccessibilityNodeInfoCompatIcs.getText(info);
        }

        @Override
        public int getWindowId(Object info) {
            return AccessibilityNodeInfoCompatIcs.getWindowId(info);
        }

        @Override
        public boolean isCheckable(Object info) {
            return AccessibilityNodeInfoCompatIcs.isCheckable(info);
        }

        @Override
        public boolean isChecked(Object info) {
            return AccessibilityNodeInfoCompatIcs.isChecked(info);
        }

        @Override
        public boolean isClickable(Object info) {
            return AccessibilityNodeInfoCompatIcs.isClickable(info);
        }

        @Override
        public boolean isEnabled(Object info) {
            return AccessibilityNodeInfoCompatIcs.isEnabled(info);
        }

        @Override
        public boolean isFocusable(Object info) {
            return AccessibilityNodeInfoCompatIcs.isFocusable(info);
        }

        @Override
        public boolean isFocused(Object info) {
            return AccessibilityNodeInfoCompatIcs.isFocused(info);
        }

        @Override
        public boolean isLongClickable(Object info) {
            return AccessibilityNodeInfoCompatIcs.isLongClickable(info);
        }

        @Override
        public boolean isPassword(Object info) {
            return AccessibilityNodeInfoCompatIcs.isPassword(info);
        }

        @Override
        public boolean isScrollable(Object info) {
            return AccessibilityNodeInfoCompatIcs.isScrollable(info);
        }

        @Override
        public boolean isSelected(Object info) {
            return AccessibilityNodeInfoCompatIcs.isSelected(info);
        }

        @Override
        public boolean performAction(Object info, int action) {
            return AccessibilityNodeInfoCompatIcs.performAction(info, action);
        }

        @Override
        public void setBoundsInParent(Object info, Rect bounds) {
            AccessibilityNodeInfoCompatIcs.setBoundsInParent(info, bounds);
        }

        @Override
        public void setBoundsInScreen(Object info, Rect bounds) {
            AccessibilityNodeInfoCompatIcs.setBoundsInScreen(info, bounds);
        }

        @Override
        public void setCheckable(Object info, boolean checkable) {
            AccessibilityNodeInfoCompatIcs.setCheckable(info, checkable);
        }

        @Override
        public void setChecked(Object info, boolean checked) {
            AccessibilityNodeInfoCompatIcs.setChecked(info, checked);
        }

        @Override
        public void setClassName(Object info, CharSequence className) {
            AccessibilityNodeInfoCompatIcs.setClassName(info, className);
        }

        @Override
        public void setClickable(Object info, boolean clickable) {
            AccessibilityNodeInfoCompatIcs.setClickable(info, clickable);
        }

        @Override
        public void setContentDescription(Object info, CharSequence contentDescription) {
            AccessibilityNodeInfoCompatIcs.setContentDescription(info, contentDescription);
        }

        @Override
        public void setEnabled(Object info, boolean enabled) {
            AccessibilityNodeInfoCompatIcs.setEnabled(info, enabled);
        }

        @Override
        public void setFocusable(Object info, boolean focusable) {
            AccessibilityNodeInfoCompatIcs.setFocusable(info, focusable);
        }

        @Override
        public void setFocused(Object info, boolean focused) {
            AccessibilityNodeInfoCompatIcs.setFocused(info, focused);
        }

        @Override
        public void setLongClickable(Object info, boolean longClickable) {
            AccessibilityNodeInfoCompatIcs.setLongClickable(info, longClickable);
        }

        @Override
        public void setPackageName(Object info, CharSequence packageName) {
            AccessibilityNodeInfoCompatIcs.setPackageName(info, packageName);
        }

        @Override
        public void setParent(Object info, View parent) {
            AccessibilityNodeInfoCompatIcs.setParent(info, parent);
        }

        @Override
        public void setPassword(Object info, boolean password) {
            AccessibilityNodeInfoCompatIcs.setPassword(info, password);
        }

        @Override
        public void setScrollable(Object info, boolean scrollable) {
            AccessibilityNodeInfoCompatIcs.setScrollable(info, scrollable);
        }

        @Override
        public void setSelected(Object info, boolean selected) {
            AccessibilityNodeInfoCompatIcs.setSelected(info, selected);
        }

        @Override
        public void setSource(Object info, View source) {
            AccessibilityNodeInfoCompatIcs.setSource(info, source);
        }

        @Override
        public void setText(Object info, CharSequence text) {
            AccessibilityNodeInfoCompatIcs.setText(info, text);
        }

        @Override
        public void recycle(Object info) {
            AccessibilityNodeInfoCompatIcs.recycle(info);
        }
    }

    static class AccessibilityNodeInfoJellybeanImpl extends AccessibilityNodeInfoIcsImpl {
        @Override
        public Object obtain(View root, int virtualDescendantId) {
            return AccessibilityNodeInfoCompatJellyBean.obtain(root, virtualDescendantId);
        }

        @Override
        public Object findFocus(Object info, int focus) {
            return AccessibilityNodeInfoCompatJellyBean.findFocus(info, focus);
        }

        @Override
        public Object focusSearch(Object info, int direction) {
            return AccessibilityNodeInfoCompatJellyBean.focusSearch(info, direction);
        }

        @Override
        public void addChild(Object info, View child, int virtualDescendantId) {
            AccessibilityNodeInfoCompatJellyBean.addChild(info, child, virtualDescendantId);
        }

        @Override
        public void setSource(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatJellyBean.setSource(info, root, virtualDescendantId);
        }

        @Override
        public boolean isVisibleToUser(Object info) {
            return AccessibilityNodeInfoCompatJellyBean.isVisibleToUser(info);
        }

        @Override
        public void setVisibleToUser(Object info, boolean visibleToUser) {
            AccessibilityNodeInfoCompatJellyBean.setVisibleToUser(info, visibleToUser);
        }

        @Override
        public boolean isAccessibilityFocused(Object info) {
            return AccessibilityNodeInfoCompatJellyBean.isAccessibilityFocused(info);
        }

        @Override
        public void setAccessibilityFocused(Object info, boolean focused) {
            AccessibilityNodeInfoCompatJellyBean.setAccesibilityFocused(info, focused);
        }

        @Override
        public boolean performAction(Object info, int action, Bundle arguments) {
            return AccessibilityNodeInfoCompatJellyBean.performAction(info, action, arguments);
        }

        @Override
        public void setMovementGranularities(Object info, int granularities) {
            AccessibilityNodeInfoCompatJellyBean.setMovementGranularities(info, granularities);
        }

        @Override
        public int getMovementGranularities(Object info) {
            return AccessibilityNodeInfoCompatJellyBean.getMovementGranularities(info);
        }

        @Override
        public void setParent(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatJellyBean.setParent(info, root, virtualDescendantId);
        }
    }

    static class AccessibilityNodeInfoJellybeanMr1Impl extends AccessibilityNodeInfoJellybeanImpl {

        @Override
        public void setLabelFor(Object info, View labeled) {
            AccessibilityNodeInfoCompatJellybeanMr1.setLabelFor(info, labeled);
        }

        @Override
        public void setLabelFor(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatJellybeanMr1.setLabelFor(info, root, virtualDescendantId);
        }

        @Override
        public Object getLabelFor(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr1.getLabelFor(info);
        }

        @Override
        public void setLabeledBy(Object info, View labeled) {
            AccessibilityNodeInfoCompatJellybeanMr1.setLabeledBy(info, labeled);
        }

        @Override
        public void setLabeledBy(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatJellybeanMr1.setLabeledBy(info, root, virtualDescendantId);
        }

        @Override
        public Object getLabeledBy(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr1.getLabeledBy(info);
        }
    }

    static class AccessibilityNodeInfoJellybeanMr2Impl extends
            AccessibilityNodeInfoJellybeanMr1Impl {

        @Override
        public String getViewIdResourceName(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr2.getViewIdResourceName(info);
        }

        @Override
        public void setViewIdResourceName(Object info, String viewId) {
            AccessibilityNodeInfoCompatJellybeanMr2.setViewIdResourceName(info, viewId);
        }

        @Override
        public List<Object> findAccessibilityNodeInfosByViewId(Object info, String viewId) {
            return AccessibilityNodeInfoCompatJellybeanMr2.findAccessibilityNodeInfosByViewId(info,
                    viewId);
        }

        @Override
        public void setTextSelection(Object info, int start, int end) {
            AccessibilityNodeInfoCompatJellybeanMr2.setTextSelection(info, start, end);
        }

        @Override
        public int getTextSelectionStart(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr2.getTextSelectionStart(info);
        }

        @Override
        public int getTextSelectionEnd(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr2.getTextSelectionEnd(info);
        }

        @Override
        public boolean isEditable(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr2.isEditable(info);
        }

        @Override
        public void setEditable(Object info, boolean editable) {
            AccessibilityNodeInfoCompatJellybeanMr2.setEditable(info, editable);
        }

        @Override
        public boolean refresh(Object info) {
            return AccessibilityNodeInfoCompatJellybeanMr2.refresh(info);
        }
    }

    static class AccessibilityNodeInfoKitKatImpl extends AccessibilityNodeInfoJellybeanMr2Impl {
        @Override
        public int getLiveRegion(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getLiveRegion(info);
        }

        @Override
        public void setLiveRegion(Object info, int mode) {
            AccessibilityNodeInfoCompatKitKat.setLiveRegion(info, mode);
        }

        @Override
        public Object getCollectionInfo(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getCollectionInfo(info);
        }

        @Override
        public void setCollectionInfo(Object info, Object collectionInfo) {
            AccessibilityNodeInfoCompatKitKat.setCollectionInfo(info, collectionInfo);
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount,
                boolean hierarchical, int selectionMode) {
            return AccessibilityNodeInfoCompatKitKat.obtainCollectionInfo(rowCount, columnCount,
                    hierarchical, selectionMode);
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return AccessibilityNodeInfoCompatKitKat
                    .obtainCollectionItemInfo(rowIndex, rowSpan, columnIndex, columnSpan, heading);
        }

        @Override
        public int getCollectionInfoColumnCount(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionInfo.getColumnCount(info);
        }

        @Override
        public int getCollectionInfoRowCount(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionInfo.getRowCount(info);
        }

        @Override
        public boolean isCollectionInfoHierarchical(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionInfo.isHierarchical(info);
        }

        @Override
        public Object getCollectionItemInfo(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getCollectionItemInfo(info);
        }

        @Override
        public Object getRangeInfo(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getRangeInfo(info);
        }

        @Override
        public void setRangeInfo(Object info, Object rangeInfo) {
            AccessibilityNodeInfoCompatKitKat.setRangeInfo(info, rangeInfo);
        }

        @Override
        public int getCollectionItemColumnIndex(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionItemInfo.getColumnIndex(info);
        }

        @Override
        public int getCollectionItemColumnSpan(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionItemInfo.getColumnSpan(info);
        }

        @Override
        public int getCollectionItemRowIndex(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionItemInfo.getRowIndex(info);
        }

        @Override
        public int getCollectionItemRowSpan(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionItemInfo.getRowSpan(info);
        }

        @Override
        public boolean isCollectionItemHeading(Object info) {
            return AccessibilityNodeInfoCompatKitKat.CollectionItemInfo.isHeading(info);
        }

        @Override
        public void setCollectionItemInfo(Object info, Object collectionItemInfo) {
            AccessibilityNodeInfoCompatKitKat.setCollectionItemInfo(info, collectionItemInfo);
        }

        @Override
        public void setContentInvalid(Object info, boolean contentInvalid) {
            AccessibilityNodeInfoCompatKitKat.setContentInvalid(info, contentInvalid);
        }

        @Override
        public boolean isContentInvalid(Object info) {
            return AccessibilityNodeInfoCompatKitKat.isContentInvalid(info);
        }

        @Override
        public boolean canOpenPopup(Object info) {
            return AccessibilityNodeInfoCompatKitKat.canOpenPopup(info);
        }

        @Override
        public void setCanOpenPopup(Object info, boolean opensPopup) {
            AccessibilityNodeInfoCompatKitKat.setCanOpenPopup(info, opensPopup);
        }

        @Override
        public Bundle getExtras(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getExtras(info);
        }

        @Override
        public int getInputType(Object info) {
            return AccessibilityNodeInfoCompatKitKat.getInputType(info);
        }

        @Override
        public void setInputType(Object info, int inputType) {
            AccessibilityNodeInfoCompatKitKat.setInputType(info, inputType);
        }

        @Override
        public boolean isDismissable(Object info) {
            return AccessibilityNodeInfoCompatKitKat.isDismissable(info);
        }

        @Override
        public void setDismissable(Object info, boolean dismissable) {
            AccessibilityNodeInfoCompatKitKat.setDismissable(info, dismissable);
        }

        @Override
        public boolean isMultiLine(Object info) {
            return AccessibilityNodeInfoCompatKitKat.isMultiLine(info);
        }

        @Override
        public void setMultiLine(Object info, boolean multiLine) {
            AccessibilityNodeInfoCompatKitKat.setMultiLine(info, multiLine);
        }
    }

    static class AccessibilityNodeInfoApi21Impl extends AccessibilityNodeInfoKitKatImpl {
        @Override
        public Object newAccessibilityAction(int actionId, CharSequence label) {
            return AccessibilityNodeInfoCompatApi21.newAccessibilityAction(actionId, label);
        }

        @Override
        public List<Object> getActionList(Object info) {
            return AccessibilityNodeInfoCompatApi21.getActionList(info);
        }

        @Override
        public Object obtainCollectionInfo(int rowCount, int columnCount, boolean hierarchical,
                int selectionMode) {
            return AccessibilityNodeInfoCompatApi21.obtainCollectionInfo(rowCount, columnCount,
                    hierarchical, selectionMode);
        }

        @Override
        public void addAction(Object info, Object action) {
            AccessibilityNodeInfoCompatApi21.addAction(info, action);
        }

        @Override
        public boolean removeAction(Object info, Object action) {
            return AccessibilityNodeInfoCompatApi21.removeAction(info, action);
        }

        @Override
        public int getAccessibilityActionId(Object action) {
            return AccessibilityNodeInfoCompatApi21.getAccessibilityActionId(action);
        }

        @Override
        public CharSequence getAccessibilityActionLabel(Object action) {
            return AccessibilityNodeInfoCompatApi21.getAccessibilityActionLabel(action);
        }

        @Override
        public Object obtainCollectionItemInfo(int rowIndex, int rowSpan, int columnIndex,
                int columnSpan, boolean heading, boolean selected) {
            return AccessibilityNodeInfoCompatApi21.obtainCollectionItemInfo(rowIndex, rowSpan,
                    columnIndex, columnSpan, heading, selected);
        }

        @Override
        public boolean isCollectionItemSelected(Object info) {
            return AccessibilityNodeInfoCompatApi21.CollectionItemInfo.isSelected(info);
        }

        @Override
        public CharSequence getError(Object info) {
            return AccessibilityNodeInfoCompatApi21.getError(info);
        }

        @Override
        public void setError(Object info, CharSequence error) {
            AccessibilityNodeInfoCompatApi21.setError(info, error);
        }

        @Override
        public void setMaxTextLength(Object info, int max) {
            AccessibilityNodeInfoCompatApi21.setMaxTextLength(info, max);
        }

        @Override
        public int getMaxTextLength(Object info) {
            return AccessibilityNodeInfoCompatApi21.getMaxTextLength(info);
        }

        @Override
        public Object getWindow(Object info) {
            return AccessibilityNodeInfoCompatApi21.getWindow(info);
        }

        @Override
        public boolean removeChild(Object info, View child) {
            return AccessibilityNodeInfoCompatApi21.removeChild(info, child);
        }

        @Override
        public boolean removeChild(Object info, View root, int virtualDescendantId) {
            return AccessibilityNodeInfoCompatApi21.removeChild(info, root, virtualDescendantId);
        }
    }

    static class AccessibilityNodeInfoApi22Impl extends AccessibilityNodeInfoApi21Impl {
        @Override
        public Object getTraversalBefore(Object info) {
            return AccessibilityNodeInfoCompatApi22.getTraversalBefore(info);
        }

        @Override
        public void setTraversalBefore(Object info, View view) {
            AccessibilityNodeInfoCompatApi22.setTraversalBefore(info, view);
        }

        @Override
        public void setTraversalBefore(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatApi22.setTraversalBefore(info, root, virtualDescendantId);
        }

        @Override
        public Object getTraversalAfter(Object info) {
            return AccessibilityNodeInfoCompatApi22.getTraversalAfter(info);
        }

        @Override
        public void setTraversalAfter(Object info, View view) {
            AccessibilityNodeInfoCompatApi22.setTraversalAfter(info, view);
        }

        @Override
        public void setTraversalAfter(Object info, View root, int virtualDescendantId) {
            AccessibilityNodeInfoCompatApi22.setTraversalAfter(info, root, virtualDescendantId);
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 22) {
            IMPL = new AccessibilityNodeInfoApi22Impl();
        } else if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new AccessibilityNodeInfoApi21Impl();
        } else if (Build.VERSION.SDK_INT >= 19) { // KitKat
            IMPL = new AccessibilityNodeInfoKitKatImpl();
        } else if (Build.VERSION.SDK_INT >= 18) { // JellyBean MR2
            IMPL = new AccessibilityNodeInfoJellybeanMr2Impl();
        } else if (Build.VERSION.SDK_INT >= 17) { // JellyBean MR1
            IMPL = new AccessibilityNodeInfoJellybeanMr1Impl();
        } else if (Build.VERSION.SDK_INT >= 16) { // JellyBean
            IMPL = new AccessibilityNodeInfoJellybeanImpl();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityNodeInfoIcsImpl();
        } else {
            IMPL = new AccessibilityNodeInfoStubImpl();
        }
    }

    private static final AccessibilityNodeInfoImpl IMPL;

    private final Object mInfo;

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
     */
    public AccessibilityNodeInfoCompat(Object info) {
        mInfo = info;
    }

    /**
     * @return The wrapped {@link android.view.accessibility.AccessibilityNodeInfo}.
     */
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
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.obtain(source));
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
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.obtain());
    }

    /**
     * Returns a cached instance if such is available or a new one is create.
     * The returned instance is initialized from the given <code>info</code>.
     *
     * @param info The other info.
     * @return An instance.
     */
    public static AccessibilityNodeInfoCompat obtain(AccessibilityNodeInfoCompat info) {
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.obtain(info.mInfo));
    }

    /**
     * Sets the source.
     *
     * @param source The info source.
     */
    public void setSource(View source) {
        IMPL.setSource(mInfo, source);
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
        return IMPL.getWindowId(mInfo);
    }

    /**
     * Gets the number of children.
     *
     * @return The child count.
     */
    public int getChildCount() {
        return IMPL.getChildCount(mInfo);
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
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getChild(mInfo, index));
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
        IMPL.addChild(mInfo, child);
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
        return IMPL.getActions(mInfo);
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
        IMPL.addAction(mInfo, action);
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
        return IMPL.performAction(mInfo, action);
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
        List<Object> infos = IMPL.findAccessibilityNodeInfosByText(mInfo, text);
        final int infoCount = infos.size();
        for (int i = 0; i < infoCount; i++) {
            Object info = infos.get(i);
            result.add(new AccessibilityNodeInfoCompat(info));
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
        return AccessibilityNodeInfoCompat.wrapNonNullInstance(IMPL.getParent(mInfo));
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
        IMPL.setParent(mInfo, parent);
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
        IMPL.setParent(mInfo, root, virtualDescendantId);
    }

    /**
     * Gets the node bounds in parent coordinates.
     *
     * @param outBounds The output node bounds.
     */
    public void getBoundsInParent(Rect outBounds) {
        IMPL.getBoundsInParent(mInfo, outBounds);
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
     *@throws IllegalStateException If called from an AccessibilityService.
     */
    public void setBoundsInParent(Rect bounds) {
        IMPL.setBoundsInParent(mInfo, bounds);
    }

    /**
     * Gets the node bounds in screen coordinates.
     *
     * @param outBounds The output node bounds.
     */
    public void getBoundsInScreen(Rect outBounds) {
        IMPL.getBoundsInScreen(mInfo, outBounds);
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
        IMPL.setBoundsInScreen(mInfo, bounds);
    }

    /**
     * Gets whether this node is checkable.
     *
     * @return True if the node is checkable.
     */
    public boolean isCheckable() {
        return IMPL.isCheckable(mInfo);
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
        IMPL.setCheckable(mInfo, checkable);
    }

    /**
     * Gets whether this node is checked.
     *
     * @return True if the node is checked.
     */
    public boolean isChecked() {
        return IMPL.isChecked(mInfo);
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
        IMPL.setChecked(mInfo, checked);
    }

    /**
     * Gets whether this node is focusable.
     *
     * @return True if the node is focusable.
     */
    public boolean isFocusable() {
        return IMPL.isFocusable(mInfo);
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
        IMPL.setFocusable(mInfo, focusable);
    }

    /**
     * Gets whether this node is focused.
     *
     * @return True if the node is focused.
     */
    public boolean isFocused() {
        return IMPL.isFocused(mInfo);
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
        IMPL.setFocused(mInfo, focused);
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
        return IMPL.isSelected(mInfo);
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
        IMPL.setSelected(mInfo, selected);
    }

    /**
     * Gets whether this node is clickable.
     *
     * @return True if the node is clickable.
     */
    public boolean isClickable() {
        return IMPL.isClickable(mInfo);
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
        IMPL.setClickable(mInfo, clickable);
    }

    /**
     * Gets whether this node is long clickable.
     *
     * @return True if the node is long clickable.
     */
    public boolean isLongClickable() {
        return IMPL.isLongClickable(mInfo);
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
        IMPL.setLongClickable(mInfo, longClickable);
    }

    /**
     * Gets whether this node is enabled.
     *
     * @return True if the node is enabled.
     */
    public boolean isEnabled() {
        return IMPL.isEnabled(mInfo);
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
        IMPL.setEnabled(mInfo, enabled);
    }

    /**
     * Gets whether this node is a password.
     *
     * @return True if the node is a password.
     */
    public boolean isPassword() {
        return IMPL.isPassword(mInfo);
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
        IMPL.setPassword(mInfo, password);
    }

    /**
     * Gets if the node is scrollable.
     *
     * @return True if the node is scrollable, false otherwise.
     */
    public boolean isScrollable() {
        return IMPL.isScrollable(mInfo);
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
        IMPL.setScrollable(mInfo, scrollable);
    }

    /**
     * Gets the package this node comes from.
     *
     * @return The package name.
     */
    public CharSequence getPackageName() {
        return IMPL.getPackageName(mInfo);
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
        IMPL.setPackageName(mInfo, packageName);
    }

    /**
     * Gets the class this node comes from.
     *
     * @return The class name.
     */
    public CharSequence getClassName() {
        return IMPL.getClassName(mInfo);
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
        IMPL.setClassName(mInfo, className);
    }

    /**
     * Gets the text of this node.
     *
     * @return The text.
     */
    public CharSequence getText() {
        return IMPL.getText(mInfo);
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
        IMPL.setText(mInfo, text);
    }

    /**
     * Gets the content description of this node.
     *
     * @return The content description.
     */
    public CharSequence getContentDescription() {
        return IMPL.getContentDescription(mInfo);
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
        IMPL.setContentDescription(mInfo, contentDescription);
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this function.
     *
     * @throws IllegalStateException If the info is already recycled.
     */
    public void recycle() {
        IMPL.recycle(mInfo);
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
        List<Object> nodes = IMPL.findAccessibilityNodeInfosByViewId(mInfo, viewId);
        if (nodes != null) {
            List<AccessibilityNodeInfoCompat> result = new ArrayList<AccessibilityNodeInfoCompat>();
            for (Object node : nodes) {
                result.add(new AccessibilityNodeInfoCompat(node));
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
