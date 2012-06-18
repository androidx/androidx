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
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeInfo}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class AccessibilityNodeInfoCompat {

    static interface AccessibilityNodeInfoImpl {
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
        public int getActions(Object info);
        public void addAction(Object info, int action);
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
    }

    static class AccessibilityNodeInfoStubImpl implements AccessibilityNodeInfoImpl {
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
        public void addChild(Object info, View child) {

        }

        @Override
        public void addChild(Object info, View child, int virtualDescendantId) {

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

    static {
        if (Build.VERSION.SDK_INT >= 16) { // JellyBean
            IMPL = new AccessibilityNodeInfoJellybeanImpl();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityNodeInfoIcsImpl();
        } else {
            IMPL = new AccessibilityNodeInfoStubImpl();
        }
    }

    private static final AccessibilityNodeInfoImpl IMPL;

    private final Object mInfo;

    // Actions.

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
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
     *           AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
     *   info.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
     * </code></pre></p>
     * </p>
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
     * <strong>Arguments:</strong> {@link #ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT}<br>
     * <strong>Example:</strong>
     * <code><pre><p>
     *   Bundle arguments = new Bundle();
     *   arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
     *           AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
     *   info.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
     *           arguments);
     * </code></pre></p>
     * </p>
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
}
