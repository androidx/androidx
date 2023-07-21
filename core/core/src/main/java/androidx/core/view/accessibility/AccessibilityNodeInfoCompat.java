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

package androidx.core.view.accessibility;

import static android.view.View.NO_ID;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.util.Collections.emptyList;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.TouchDelegateInfo;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.R;
import androidx.core.accessibilityservice.AccessibilityServiceInfoCompat;
import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand.CommandArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.MoveAtGranularityArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.MoveHtmlArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.MoveWindowArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.ScrollToPositionArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.SetProgressArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.SetSelectionArguments;
import androidx.core.view.accessibility.AccessibilityViewCommand.SetTextArguments;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper for accessing {@link android.view.accessibility.AccessibilityNodeInfo} in a backwards
 * compatible fashion.
 */
public class AccessibilityNodeInfoCompat {

    /**
     * A class defining an action that can be performed on an {@link AccessibilityNodeInfo}.
     * Each action has a unique id and a label.
     * <p>
     * There are three categories of actions:
     * <ul>
     * <li><strong>Standard actions</strong> - These are actions that are reported and
     * handled by the standard UI widgets in the platform. Each standard action is associated with
     * a resource id, e.g. {@link android.R.id#accessibilityActionScrollUp}. Note that actions were
     * formerly associated with static constants defined in this class, e.g.
     * {@link #ACTION_FOCUS}. These actions will have {@code null} labels.
     * </li>
     * <li><strong>Custom actions action</strong> - These are actions that are reported
     * and handled by custom widgets. i.e. ones that are not part of the UI toolkit. For
     * example, an application may define a custom action for clearing the user history.
     * </li>
     * <li><strong>Overriden standard actions</strong> - These are actions that override
     * standard actions to customize them. For example, an app may add a label to the
     * standard {@link #ACTION_CLICK} action to indicate to the user that this action clears
     * browsing history.
     * </ul>
     * </p>
     * <p class="note">
     * <strong>Note:</strong> Views which support these actions should invoke
     * {@link View#setImportantForAccessibility(int)} with
     * {@link View#IMPORTANT_FOR_ACCESSIBILITY_YES} to ensure an
     * {@link android.accessibilityservice.AccessibilityService} can discover the set of supported
     * actions.
     * </p>
     */
    public static class AccessibilityActionCompat {

        private static final String TAG = "A11yActionCompat";

        /**
         * Action that gives input focus to the node.
         */
        public static final AccessibilityActionCompat ACTION_FOCUS =
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_FOCUS, null);

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
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK, null);

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
                        AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, null,
                        MoveAtGranularityArguments.class);

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
                        AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, null,
                        MoveAtGranularityArguments.class);

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
                        AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT, null,
                        MoveHtmlArguments.class);

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
                        AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT, null,
                        MoveHtmlArguments.class);

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
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_COPY, null);

        /**
         * Action to paste the current clipboard content.
         */
        public static final AccessibilityActionCompat ACTION_PASTE =
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_PASTE, null);

        /**
         * Action to cut the current selection and place it to the clipboard.
         */
        public static final AccessibilityActionCompat ACTION_CUT =
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CUT, null);

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
                        AccessibilityNodeInfoCompat.ACTION_SET_SELECTION, null,
                        SetSelectionArguments.class);

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
         *  info.performAction(AccessibilityActionCompat.ACTION_SET_TEXT.getId(), arguments);
         * </code></pre></p>
         */
        public static final AccessibilityActionCompat ACTION_SET_TEXT =
                new AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, null,
                        SetTextArguments.class);

        /**
         * Action that requests the node make its bounding rectangle visible
         * on the screen, scrolling if necessary just enough.
         *
         * @see View#requestRectangleOnScreen(Rect)
         */
        public static final AccessibilityActionCompat ACTION_SHOW_ON_SCREEN =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN : null,
                        android.R.id.accessibilityActionShowOnScreen, null, null, null);

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
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION
                        : null, android.R.id.accessibilityActionScrollToPosition, null, null,
                        ScrollToPositionArguments.class);

        /**
         * Action to scroll the node content up.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_UP =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP : null,
                        android.R.id.accessibilityActionScrollUp, null, null, null);
        /**
         * Action to scroll the node content left.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_LEFT =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT : null,
                        android.R.id.accessibilityActionScrollLeft, null, null, null);

        /**
         * Action to scroll the node content down.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_DOWN =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN : null,
                        android.R.id.accessibilityActionScrollDown, null, null, null);

        /**
         * Action to scroll the node content right.
         */
        public static final AccessibilityActionCompat ACTION_SCROLL_RIGHT =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT : null,
                        android.R.id.accessibilityActionScrollRight, null, null, null);

        /**
         * Action to move to the page above.
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_PAGE_UP =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 29
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP : null,
                        android.R.id.accessibilityActionPageUp, null, null, null);

        /**
         * Action to move to the page below.
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_PAGE_DOWN =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 29
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN : null,
                        android.R.id.accessibilityActionPageDown, null, null, null);

        /**
         * Action to move to the page left.
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_PAGE_LEFT =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 29
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_LEFT : null,
                        android.R.id.accessibilityActionPageLeft, null, null, null);

        /**
         * Action to move to the page right.
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_PAGE_RIGHT =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 29
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_RIGHT : null,
                        android.R.id.accessibilityActionPageRight, null, null, null);

        /**
         * Action that context clicks the node.
         */
        public static final AccessibilityActionCompat ACTION_CONTEXT_CLICK =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 23
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK : null,
                        android.R.id.accessibilityActionContextClick, null, null, null);

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
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 24
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS : null,
                        android.R.id.accessibilityActionSetProgress, null, null,
                        SetProgressArguments.class);

        /**
         * Action to move a window to a new location.
         * <p>
         * <strong>Arguments:</strong>
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVE_WINDOW_X}
         * {@link AccessibilityNodeInfoCompat#ACTION_ARGUMENT_MOVE_WINDOW_Y}
         */
        public static final AccessibilityActionCompat ACTION_MOVE_WINDOW =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 26
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_MOVE_WINDOW : null,
                        android.R.id.accessibilityActionMoveWindow, null, null,
                        MoveWindowArguments.class);

        /**
         * Action to show a tooltip.
         */
        public static final AccessibilityActionCompat ACTION_SHOW_TOOLTIP =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 28
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_TOOLTIP : null,
                        android.R.id.accessibilityActionShowTooltip, null, null, null);

        /**
         * Action to hide a tooltip. A node should expose this action only for views that are
         * currently showing a tooltip.
         */
        public static final AccessibilityActionCompat ACTION_HIDE_TOOLTIP =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 28
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_HIDE_TOOLTIP : null,
                        android.R.id.accessibilityActionHideTooltip, null, null, null);

        /**
         * Action that presses and holds a node.
         * <p>
         * This action is for nodes that have distinct behavior that depends on how long a press is
         * held. Nodes having a single action for long press should use {@link #ACTION_LONG_CLICK}
         *  instead of this action, and nodes should not expose both actions.
         * <p>
         * When calling {@code performAction(ACTION_PRESS_AND_HOLD, bundle}, use
         * {@link #ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT} to specify how long the
         * node is pressed. The first time an accessibility service performs ACTION_PRES_AND_HOLD
         * on a node, it must specify 0 as ACTION_ARGUMENT_PRESS_AND_HOLD, so the application is
         * notified that the held state has started. To ensure reasonable behavior, the values
         * must be increased incrementally and may not exceed 10,000. UIs requested
         * to hold for times outside of this range should ignore the action.
         * <p>
         * The total time the element is held could be specified by an accessibility user up-front,
         * or may depend on what happens on the UI as the user continues to request the hold.
         * <p>
         *   <strong>Note:</strong> The time between dispatching the action and it arriving in the
         *     UI process is not guaranteed. It is possible on a busy system for the time to expire
         *     unexpectedly. For the case of holding down a key for a repeating action, a delayed
         *     arrival should be benign. Please do not use this sort of action in cases where such
         *     delays will lead to unexpected UI behavior.
         * <p>
         */
        @NonNull public static final AccessibilityActionCompat ACTION_PRESS_AND_HOLD =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 30
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_PRESS_AND_HOLD : null,
                        android.R.id.accessibilityActionPressAndHold, null, null, null);

        /**
         * Action to send an ime actionId which is from
         * {@link android.view.inputmethod.EditorInfo#actionId}. This ime actionId sets by
         * {@link android.widget.TextView#setImeActionLabel(CharSequence, int)}, or it would be
         * {@link android.view.inputmethod.EditorInfo#IME_ACTION_UNSPECIFIED} if no specific
         * actionId has set. A node should expose this action only for views that are currently
         * with input focus and editable.
         */
        @NonNull public static final AccessibilityActionCompat ACTION_IME_ENTER =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 30
                        ? AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER : null,
                        android.R.id.accessibilityActionImeEnter, null, null, null);

        /**
         * Action to start a drag.
         * <p>
         * This action initiates a drag & drop within the system. The source's dragged content is
         * prepared before the drag begins. In View, this action should prepare the arguments to
         * {@link View#startDragAndDrop(ClipData, View.DragShadowBuilder, Object, int)}} and then
         * call the method. The equivalent should be performed for other UI toolkits.
         * </p>
         *
         * @see AccessibilityEventCompat#CONTENT_CHANGE_TYPE_DRAG_STARTED
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_DRAG_START =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 32
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_DRAG_START : null,
                        android.R.id.accessibilityActionDragStart, null, null, null);

        /**
         * Action to trigger a drop of the content being dragged.
         * <p>
         * This action is added to potential drop targets if the source started a drag with
         * {@link #ACTION_DRAG_START}. In View, these targets are Views that accepted
         * {@link android.view.DragEvent#ACTION_DRAG_STARTED} and have an
         * {@link View.OnDragListener}.
         * </p>
         *
         * @see AccessibilityEventCompat#CONTENT_CHANGE_TYPE_DRAG_DROPPED
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_DRAG_DROP =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 32
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_DRAG_DROP : null,
                        android.R.id.accessibilityActionDragDrop, null, null, null);

        /**
         * Action to cancel a drag.
         * <p>
         * This action is added to the source that started a drag with {@link #ACTION_DRAG_START}.
         * </p>
         *
         * @see AccessibilityEventCompat#CONTENT_CHANGE_TYPE_DRAG_CANCELLED
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_DRAG_CANCEL =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 32
                        ?  AccessibilityNodeInfo.AccessibilityAction.ACTION_DRAG_CANCEL : null,
                        android.R.id.accessibilityActionDragCancel, null, null, null);

        /**
         * Action to show suggestions for editable text.
         */
        @NonNull
        public static final AccessibilityActionCompat ACTION_SHOW_TEXT_SUGGESTIONS =
                new AccessibilityActionCompat(Build.VERSION.SDK_INT >= 33
                        ?   AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_TEXT_SUGGESTIONS
                        :   null, android.R.id.accessibilityActionShowTextSuggestions, null,
                        null, null);

        final Object mAction;
        private final int mId;
        private final Class<? extends CommandArguments> mViewCommandArgumentClass;

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        protected final AccessibilityViewCommand mCommand;

        /**
         * Creates a new instance.
         *
         * @param actionId The action id.
         * @param label The action label.
         */
        public AccessibilityActionCompat(int actionId, CharSequence label) {
            this(null, actionId, label, null, null);
        }

        /**
         * Creates a new instance.
         *
         * @param actionId The action id.
         * @param label The action label.
         * @param command The command performed when the service requests the action
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public AccessibilityActionCompat(int actionId, CharSequence label,
                AccessibilityViewCommand command) {
            this(null, actionId, label, command, null);
        }

        AccessibilityActionCompat(Object action) {
            this(action, 0, null, null, null);
        }

        private AccessibilityActionCompat(int actionId, CharSequence label,
                Class<? extends CommandArguments> viewCommandArgumentClass) {
            this(null, actionId, label, null, viewCommandArgumentClass);
        }

        AccessibilityActionCompat(Object action, int id, CharSequence label,
                AccessibilityViewCommand command,
                Class<? extends CommandArguments> viewCommandArgumentClass) {
            mId = id;
            mCommand = command;
            if (Build.VERSION.SDK_INT >= 21 && action == null) {
                mAction = new AccessibilityNodeInfo.AccessibilityAction(id, label);
            } else {
                mAction = action;
            }
            mViewCommandArgumentClass = viewCommandArgumentClass;
        }

        /**
         * Gets the id for this action.
         *
         * @return The action id.
         */
        public int getId() {
            if (Build.VERSION.SDK_INT >= 21) {
                return ((AccessibilityNodeInfo.AccessibilityAction) mAction).getId();
            } else {
                return 0;
            }
        }

        /**
         * Gets the label for this action. Its purpose is to describe the
         * action to user.
         *
         * @return The label.
         */
        public CharSequence getLabel() {
            if (Build.VERSION.SDK_INT >= 21) {
                return ((AccessibilityNodeInfo.AccessibilityAction) mAction).getLabel();
            } else {
                return null;
            }
        }

        /**
         * Performs the action.
         * @return If the action was handled.
         * @param view View to act upon.
         * @param arguments Optional action arguments.
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public boolean perform(View view, Bundle arguments) {
            if (mCommand != null) {
                CommandArguments viewCommandArgument = null;
                if (mViewCommandArgumentClass != null) {
                    try {
                        viewCommandArgument =
                                mViewCommandArgumentClass.getDeclaredConstructor().newInstance();
                        viewCommandArgument.setBundle(arguments);
                    } catch (Exception e) {
                        final String className = mViewCommandArgumentClass == null
                                ? "null" : mViewCommandArgumentClass.getName();
                        Log.e(TAG, "Failed to execute command with argument class "
                                + "ViewCommandArgument: " + className, e);
                    }
                }
                return mCommand.perform(view, viewCommandArgument);
            }
            return false;
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public AccessibilityActionCompat createReplacementAction(CharSequence label,
                AccessibilityViewCommand command) {
            return new AccessibilityActionCompat(null, mId, label, command,
                    mViewCommandArgumentClass);
        }

        @Override
        public int hashCode() {
            return mAction != null ? mAction.hashCode() : 0;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof AccessibilityNodeInfoCompat.AccessibilityActionCompat)) {
                return false;
            }
            AccessibilityNodeInfoCompat.AccessibilityActionCompat other =
                    (AccessibilityNodeInfoCompat.AccessibilityActionCompat) obj;
            if (mAction == null) {
                if (other.mAction != null) {
                    return false;
                }
            } else if (!mAction.equals(other.mAction)) {
                return false;
            }
            return true;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("AccessibilityActionCompat: ");
            // Mirror AccessibilityNodeInfoCompat.toString's action string.
            String actionName = getActionSymbolicName(mId);
            if (actionName.equals("ACTION_UNKNOWN") && getLabel() != null) {
                actionName = getLabel().toString();
            }
            builder.append(actionName);
            return builder.toString();
        }
    }

    /**
     * Class with information if a node is a collection.
     * <p>
     * A collection of items has rows and columns and may be marked as hierarchical.
     *
     * <p>
     * For example, a list where the items are placed in a vertical layout is a collection with one
     * column and as many rows as the list items. This collection has 3 rows and 1 column and should
     * not be marked as hierarchical since items do not exist at different levels/ranks and there
     * are no nested collections.
     * <ul>
     *     <li>Item 1</li>
     *     <li>Item 2</li>
     *     <li>Item 3</li>
     * </ul>
     *
     * <p>
     * A table is a collection with several rows and several columns. This collection has 2 rows and
     * 3 columns and is not marked as hierarchical:
     *<table>
     *   <tr>
     *     <td>Item 1</td>
     *     <td>Item 2</td>
     *     <td>Item 3</td>
     *   </tr>
     *   <tr>
     *     <td>Item 4</td>
     *     <td>Item 5</td>
     *     <td>Item 6</td>
     *   </tr>
     * </table>
     *
     * <p>
     * Nested collections could be marked as hierarchical. To add outer and inner collections to the
     * same hierarchy, mark them both as hierarchical.
     *
     * <p> For example, if you have a collection with two lists - this collection has an outer
     * list with 3 rows and 1 column and an inner list within "Item 2" with 2 rows and 1 -
     * you can mark both the outer list and the inner list as hierarchical to make them part of
     * the same hierarchy. If a collection does not have any ancestor or descendant hierarchical
     * collections, it does not need to be marked as hierarchical.
     *  <ul>
     *      <li>Item 1</li>
     *      <li> Item 2
     *          <ul>
     *              <li>Item 2A</li>
     *              <li>Item 2B</li>
     *          </ul>
     *      </li>
     *      <li>Item 3</li>
     *  </ul>
     *
     * <p>
     * To be a valid list, a collection has 1 row and any number of columns or 1 column and any
     * number of rows.
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
            if (Build.VERSION.SDK_INT >= 21) {
                return new CollectionInfoCompat(AccessibilityNodeInfo.CollectionInfo.obtain(
                        rowCount, columnCount, hierarchical, selectionMode));
            } else if (Build.VERSION.SDK_INT >= 19) {
                return new CollectionInfoCompat(AccessibilityNodeInfo.CollectionInfo.obtain(
                        rowCount, columnCount, hierarchical));
            } else {
                return new CollectionInfoCompat(null);
            }
        }

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowCount The number of rows, or -1 if count is unknown.
         * @param columnCount The number of columns , or -1 if count is unknown.
         * @param hierarchical Whether the collection is hierarchical.
         *
         * @return An instance.
         */
        public static CollectionInfoCompat obtain(int rowCount, int columnCount,
                boolean hierarchical) {
            if (Build.VERSION.SDK_INT >= 19) {
                return new CollectionInfoCompat(AccessibilityNodeInfo.CollectionInfo.obtain(
                        rowCount, columnCount, hierarchical));
            } else {
                return new CollectionInfoCompat(null);
            }
        }

        CollectionInfoCompat(Object info) {
            mInfo = info;
        }

        /**
         * Gets the number of columns.
         *
         * @return The column count, or -1 if count is unknown.
         */
        public int getColumnCount() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionInfo) mInfo).getColumnCount();
            } else {
                return -1;
            }
        }

        /**
         * Gets the number of rows.
         *
         * @return The row count, or -1 if count is unknown.
         */
        public int getRowCount() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionInfo) mInfo).getRowCount();
            } else {
                return -1;
            }
        }

        /**
         * Gets if the collection is a hierarchically ordered.
         *
         * @return Whether the collection is hierarchical.
         */
        public boolean isHierarchical() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionInfo) mInfo).isHierarchical();
            } else {
                return false;
            }
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
            if (Build.VERSION.SDK_INT >= 21) {
                return ((AccessibilityNodeInfo.CollectionInfo) mInfo).getSelectionMode();
            } else {
                return 0;
            }
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
         * @param heading Whether the item is a heading. This should be set to false and the newer
         *                {@link AccessibilityNodeInfoCompat#setHeading(boolean)} used to identify
         *                headings.
         * @param selected Whether the item is selected.
         * @return An instance.
         */
        public static CollectionItemInfoCompat obtain(int rowIndex, int rowSpan,
                int columnIndex, int columnSpan, boolean heading, boolean selected) {
            if (Build.VERSION.SDK_INT >= 21) {
                return new CollectionItemInfoCompat(AccessibilityNodeInfo.CollectionItemInfo.obtain(
                        rowIndex, rowSpan, columnIndex, columnSpan, heading, selected));
            } else if (Build.VERSION.SDK_INT >= 19) {
                return new CollectionItemInfoCompat(AccessibilityNodeInfo.CollectionItemInfo.obtain(
                        rowIndex, rowSpan, columnIndex, columnSpan, heading));
            } else {
                return new CollectionItemInfoCompat(null);
            }
        }

        /**
         * Returns a cached instance if such is available otherwise a new one.
         *
         * @param rowIndex The row index at which the item is located.
         * @param rowSpan The number of rows the item spans.
         * @param columnIndex The column index at which the item is located.
         * @param columnSpan The number of columns the item spans.
         * @param heading Whether the item is a heading. This should be set to false and the newer
         *                {@link AccessibilityNodeInfoCompat#setHeading(boolean)} used to identify
         *                headings.
         * @return An instance.
         */
        public static CollectionItemInfoCompat obtain(int rowIndex, int rowSpan,
                int columnIndex, int columnSpan, boolean heading) {
            if (Build.VERSION.SDK_INT >= 19) {
                return new CollectionItemInfoCompat(AccessibilityNodeInfo.CollectionItemInfo.obtain(
                        rowIndex, rowSpan, columnIndex, columnSpan, heading));
            } else {
                return new CollectionItemInfoCompat(null);
            }
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
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).getColumnIndex();
            } else {
                return 0;
            }
        }

        /**
         * Gets the number of columns the item spans.
         *
         * @return The column span.
         */
        public int getColumnSpan() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).getColumnSpan();
            } else {
                return 0;
            }
        }

        /**
         * Gets the row index at which the item is located.
         *
         * @return The row index.
         */
        public int getRowIndex() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).getRowIndex();
            } else {
                return 0;
            }
        }

        /**
         * Gets the number of rows the item spans.
         *
         * @return The row span.
         */
        public int getRowSpan() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).getRowSpan();
            } else {
                return 0;
            }
        }

        /**
         * Gets if the collection item is a heading. For example, section
         * heading, table header, etc.
         *
         * @return If the item is a heading.
         * @deprecated Use {@link AccessibilityNodeInfoCompat#isHeading()}
         */
        @SuppressWarnings("deprecation")
        @Deprecated
        public boolean isHeading() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).isHeading();
            } else {
                return false;
            }
        }

        /**
         * Gets if the collection item is selected.
         *
         * @return If the item is selected.
         */
        public boolean isSelected() {
            if (Build.VERSION.SDK_INT >= 21) {
                return ((AccessibilityNodeInfo.CollectionItemInfo) mInfo).isSelected();
            } else {
                return false;
            }
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
            if (Build.VERSION.SDK_INT >= 19) {
                return new RangeInfoCompat(
                        AccessibilityNodeInfo.RangeInfo.obtain(type, min, max, current));
            } else {
                return new RangeInfoCompat(null);
            }
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
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.RangeInfo) mInfo).getCurrent();
            } else {
                return 0;
            }
        }

        /**
         * Gets the max value.
         *
         * @return The max value.
         */
        public float getMax() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.RangeInfo) mInfo).getMax();
            } else {
                return 0;
            }
        }

        /**
         * Gets the min value.
         *
         * @return The min value.
         */
        public float getMin() {
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.RangeInfo) mInfo).getMin();
            } else {
                return 0;
            }
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
            if (Build.VERSION.SDK_INT >= 19) {
                return ((AccessibilityNodeInfo.RangeInfo) mInfo).getType();
            } else {
                return RANGE_TYPE_INT;
            }
        }
    }

    /**
     * Class with information of touch delegated views and regions.
     */
    public static final class TouchDelegateInfoCompat {
        final TouchDelegateInfo mInfo;

        /**
         * Create a new instance of {@link TouchDelegateInfoCompat}.
         *
         * @param targetMap A map from regions (in view coordinates) to delegated views.
         */
        public TouchDelegateInfoCompat(@NonNull Map<Region, View> targetMap) {
            if (Build.VERSION.SDK_INT >= 29) {
                mInfo = new TouchDelegateInfo(targetMap);
            } else {
                mInfo = null;
            }
        }

        TouchDelegateInfoCompat(@NonNull TouchDelegateInfo info) {
            mInfo = info;
        }

        /**
         * Returns the number of touch delegate target region.
         * <p>
         * Compatibility:
         * <ul>
         *     <li>API &lt; 29: Always returns {@code 0}</li>
         * </ul>
         *
         * @return Number of touch delegate target region.
         */
        public @IntRange(from = 0) int getRegionCount() {
            if (Build.VERSION.SDK_INT >= 29) {
                return mInfo.getRegionCount();
            }
            return 0;
        }

        /**
         * Return the {@link Region} at the given index.
         * <p>
         * Compatibility:
         * <ul>
         *     <li>API &lt; 29: Always returns {@code null}</li>
         * </ul>
         *
         * @param index The desired index, must be between 0 and {@link #getRegionCount()}-1.
         * @return Returns the {@link Region} stored at the given index.
         */
        @Nullable
        public Region getRegionAt(@IntRange(from = 0) int index) {
            if (Build.VERSION.SDK_INT >= 29) {
                return mInfo.getRegionAt(index);
            }
            return null;
        }

        /**
         * Return the target {@link AccessibilityNodeInfoCompat} for the given {@link Region}.
         * <p>
         *   <strong>Note:</strong> This api can only be called from
         *   {@link android.accessibilityservice.AccessibilityService}.
         * </p>
         * <p>
         * Compatibility:
         * <ul>
         *     <li>API &lt; 29: Always returns {@code null}</li>
         * </ul>
         *
         * @param region The region retrieved from {@link #getRegionAt(int)}.
         * @return The target node associates with the given region.
         */
        @Nullable
        public AccessibilityNodeInfoCompat getTargetForRegion(@NonNull Region region) {
            if (Build.VERSION.SDK_INT >= 29) {
                AccessibilityNodeInfo info = mInfo.getTargetForRegion(region);
                if (info != null) {
                    return AccessibilityNodeInfoCompat.wrap(info);
                }
            }
            return null;
        }
    }

    private static final String ROLE_DESCRIPTION_KEY =
            "AccessibilityNodeInfo.roleDescription";

    private static final String PANE_TITLE_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.PANE_TITLE_KEY";

    private static final String TOOLTIP_TEXT_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.TOOLTIP_TEXT_KEY";

    private static final String HINT_TEXT_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.HINT_TEXT_KEY";

    private static final String BOOLEAN_PROPERTY_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.BOOLEAN_PROPERTY_KEY";

    private static final String SPANS_ID_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.SPANS_ID_KEY";

    private static final String SPANS_START_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.SPANS_START_KEY";

    private static final String SPANS_END_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.SPANS_END_KEY";

    private static final String SPANS_FLAGS_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.SPANS_FLAGS_KEY";

    private static final String SPANS_ACTION_ID_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.SPANS_ACTION_ID_KEY";

    private static final String STATE_DESCRIPTION_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.STATE_DESCRIPTION_KEY";

    private static final String UNIQUE_ID_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat.UNIQUE_ID_KEY";

    private static final String MIN_DURATION_BETWEEN_CONTENT_CHANGES_KEY =
            "androidx.view.accessibility.AccessibilityNodeInfoCompat."
                    + "MIN_DURATION_BETWEEN_CONTENT_CHANGES_KEY";

    // These don't line up with the internal framework constants, since they are independent
    // and we might as well get all 32 bits of utility here.
    private static final int BOOLEAN_PROPERTY_SCREEN_READER_FOCUSABLE = 0x00000001;
    private static final int BOOLEAN_PROPERTY_IS_HEADING = 0x00000002;
    private static final int BOOLEAN_PROPERTY_IS_SHOWING_HINT = 0x00000004;
    private static final int BOOLEAN_PROPERTY_IS_TEXT_ENTRY_KEY = 0x00000008;
    private static final int BOOLEAN_PROPERTY_HAS_REQUEST_INITIAL_ACCESSIBILITY_FOCUS = 1 << 5;

    private final AccessibilityNodeInfo mInfo;

    /**
     *  androidx.customview.widget.ExploreByTouchHelper.HOST_ID = -1;
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public int mParentVirtualDescendantId = NO_ID;

    private int mVirtualDescendantId = NO_ID;

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

    /**
     * Argument for specifying the x coordinate to which to move a window.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_MOVE_WINDOW}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_MOVE_WINDOW
     */
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_X =
            "ACTION_ARGUMENT_MOVE_WINDOW_X";

    /**
     * Argument for specifying the y coordinate to which to move a window.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_MOVE_WINDOW}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_MOVE_WINDOW
     */
    public static final String ACTION_ARGUMENT_MOVE_WINDOW_Y =
            "ACTION_ARGUMENT_MOVE_WINDOW_Y";

    /**
     * Argument to represent the duration in milliseconds to press and hold a node.
     * <p>
     * <strong>Type:</strong> int<br>
     * <strong>Actions:</strong>
     * <ul>
     *     <li>{@link AccessibilityActionCompat#ACTION_PRESS_AND_HOLD}</li>
     * </ul>
     *
     * @see AccessibilityActionCompat#ACTION_PRESS_AND_HOLD
     */
    @SuppressLint("ActionValue")
    public static final String ACTION_ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT =
            "android.view.accessibility.action.ARGUMENT_PRESS_AND_HOLD_DURATION_MILLIS_INT";

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
     * Key used to request and locate extra data for text character location. This key requests that
     * an array of {@link android.graphics.RectF}s be added to the extras. This request is made with
     * {@link #refreshWithExtraData(String, Bundle)}. The arguments taken by this request are two
     * integers: {@link #EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX} and
     * {@link #EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH}. The starting index must be valid
     * inside the CharSequence returned by {@link #getText()}, and the length must be positive.
     * <p>
     * The data can be retrieved from the {@code Bundle} returned by {@link #getExtras()} using this
     * string as a key for {@link Bundle#getParcelableArray(String)}. The
     * {@link android.graphics.RectF} will be null for characters that either do not exist or are
     * off the screen.
     *
     * {@see #refreshWithExtraData(String, Bundle)}
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY =
            "android.core.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_KEY";

    /**
     * Integer argument specifying the start index of the requested text location data. Must be
     * valid inside the CharSequence returned by {@link #getText()}.
     *
     * @see #EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX =
            "android.core.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX";

    /**
     * Integer argument specifying the end index of the requested text location data. Must be
     * positive and no larger than {@link #EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH}.
     *
     * @see #EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH =
            "android.core.view.accessibility.extra.DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH";

    /**
     * The maximum allowed length of the requested text location data.
     */
    public static final int EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_MAX_LENGTH = 20000;

    private static int sClickableSpanId = 0;

    /**
     * Creates a wrapper for info implementation.
     *
     * @param object The info to wrap.
     * @return A wrapper for if the object is not null, null otherwise.
     */
    @SuppressWarnings("deprecation")
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
        if (Build.VERSION.SDK_INT >= 16) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(
                    AccessibilityNodeInfo.obtain(root, virtualDescendantId));
        } else {
            return null;
        }
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
        mVirtualDescendantId = NO_ID;

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
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}.
     * This class is made immutable before being delivered to an AccessibilityService.
     * <p>
     * This method is not supported on devices running API level < 16 since the platform did
     * not support virtual descendants of real views.
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setSource(View root, int virtualDescendantId) {
        // Store the ID anyway, since we may need it for equality checks.
        mVirtualDescendantId = virtualDescendantId;

        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.setSource(root, virtualDescendantId);
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.findFocus(focus));
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.focusSearch(direction));
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.addChild(root, virtualDescendantId);
        }
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
        if (Build.VERSION.SDK_INT >= 21) {
            return mInfo.removeChild(child);
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 21) {
            return mInfo.removeChild(root, virtualDescendantId);
        } else {
            return false;
        }
    }

    /**
     * Gets the actions that can be performed on the node.
     *
     * @return The bit mask of with actions.
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_FOCUS
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_CLEAR_FOCUS
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_SELECT
     * @see android.view.accessibility.AccessibilityNodeInfo#ACTION_CLEAR_SELECTION
     *
     * @deprecated Use {@link #getActionList()} instead.
     */
    @Deprecated
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

    private List<Integer> extrasIntList(String key) {
        if (Build.VERSION.SDK_INT < 19) {
            return new ArrayList<Integer>();
        }
        ArrayList<Integer> list = Api19Impl.getExtras(mInfo)
                .getIntegerArrayList(key);
        if (list == null) {
            list = new ArrayList<Integer>();
            Api19Impl.getExtras(mInfo).putIntegerArrayList(key, list);
        }
        return list;
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
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public void addAction(AccessibilityActionCompat action) {
        if (Build.VERSION.SDK_INT >= 21) {
            mInfo.addAction((AccessibilityNodeInfo.AccessibilityAction) action.mAction);
        }
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
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: Always returns {@code false}</li>
     * </ul>
     */
    public boolean removeAction(AccessibilityActionCompat action) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mInfo.removeAction((AccessibilityNodeInfo.AccessibilityAction) action.mAction);
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            return mInfo.performAction(action, arguments);
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.setMovementGranularities(granularities);
        }
    }

    /**
     * Gets the movement granularities for traversing the text of this node.
     *
     * @return The bit mask with granularities.
     */
    public int getMovementGranularities() {
        if (Build.VERSION.SDK_INT >= 16) {
            return mInfo.getMovementGranularities();
        } else {
            return 0;
        }
    }

    /**
     * Finds {@link android.view.accessibility.AccessibilityNodeInfo}s by text. The match
     * is case insensitive containment. The search is relative to this info i.e. this
     * info is the root of the traversed tree.
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
        mParentVirtualDescendantId = NO_ID;

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
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}.
     * This class is made immutable before being delivered to an AccessibilityService.
     * <p>
     * This method is not supported on devices running API level < 16 since the platform did
     * not support virtual descendants of real views.
     *
     * @param root The root of the virtual subtree.
     * @param virtualDescendantId The id of the virtual descendant.
     */
    public void setParent(View root, int virtualDescendantId) {
        // Store the ID anyway, since we may need it for equality checks.
        mParentVirtualDescendantId = virtualDescendantId;

        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.setParent(root, virtualDescendantId);
        }
    }

    /**
     * Gets the node bounds in the viewParent's coordinates.
     * {@link #getParent()} does not represent the source's viewParent.
     * Instead it represents the result of {@link View#getParentForAccessibility()},
     * which returns the closest ancestor where {@link View#isImportantForAccessibility()} is true.
     * So this method is not reliable.
     *
     * @param outBounds The output node bounds.
     *
     * @deprecated Use {@link #getBoundsInScreen(Rect)} instead.
     */
    @Deprecated
    public void getBoundsInParent(Rect outBounds) {
        mInfo.getBoundsInParent(outBounds);
    }

    /**
     * Sets the node bounds in the viewParent's coordinates.
     * {@link #getParent()} does not represent the source's viewParent.
     * Instead it represents the result of {@link View#getParentForAccessibility()},
     * which returns the closest ancestor where {@link View#isImportantForAccessibility()} is true.
     * So this method is not reliable.
     *
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}. This class is
     * made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param bounds The node bounds.
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Accessibility services should not care about these bounds.
     */
    @Deprecated
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
     * Gets whether this node is visible to the user.
     *
     * @return Whether the node is visible to the user.
     */
    public boolean isVisibleToUser() {
        if (Build.VERSION.SDK_INT >= 16) {
            return mInfo.isVisibleToUser();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.setVisibleToUser(visibleToUser);
        }
    }

    /**
     * Gets whether this node is accessibility focused.
     *
     * @return True if the node is accessibility focused.
     */
    public boolean isAccessibilityFocused() {
        if (Build.VERSION.SDK_INT >= 16) {
            return mInfo.isAccessibilityFocused();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 16) {
            mInfo.setAccessibilityFocused(focused);
        }
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
     * Gets if the node has selectable text.
     *
     * <p>
     *     Services should use {@link #ACTION_SET_SELECTION} for selection. Editable text nodes must
     *     also be selectable. But not all UIs will populate this field, so services should consider
     *     'isTextSelectable | isEditable' to ensure they don't miss nodes with selectable text.
     *  Compatibility:
     *  <ul>
     *      <li>Api &lt; 33: Returns false.</li>
     *  </ul>
     * </p>
     *
     * @see #isEditable
     * @return True if the node has selectable text.
     */
    public boolean isTextSelectable() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.isTextSelectable(mInfo);
        } else {
            return false;
        }
    }

    /**
     * Sets if the node has selectable text.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     *  Compatibility:
     *  <ul>
     *      <li>Api &lt; 33: Does not operate.</li>
     *  </ul>
     * </p>
     *
     * @param selectableText True if the node has selectable text, false otherwise.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setTextSelectable(boolean selectableText) {
        if (Build.VERSION.SDK_INT >= 33) {
            Api33Impl.setTextSelectable(mInfo, selectableText);
        }
    }

    /**
     * Gets the minimum time duration between two content change events.
     */
    public long getMinDurationBetweenContentChangesMillis() {
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getLong(MIN_DURATION_BETWEEN_CONTENT_CHANGES_KEY);
        }
        return 0;
    }

    /**
     * Sets the minimum time duration between two content change events, which is used in throttling
     * content change events in accessibility services.
     *
     * <p>
     * Example: An app can set MinDurationBetweenContentChanges as 1 min for a view which sends
     * content change events to accessibility services one event per second.
     * Accessibility service will throttle those content change events and only handle one event
     * per minute for that view.
     * </p>
     *
     * @see AccessibilityEventCompat#getContentChangeTypes for all content change types.
     * @param duration the minimum duration between content change events.
     */
    public void setMinDurationBetweenContentChangesMillis(long duration) {
        if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putLong(MIN_DURATION_BETWEEN_CONTENT_CHANGES_KEY, duration);
        }
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
        if (Build.VERSION.SDK_INT >= 24) {
            return mInfo.isImportantForAccessibility();
        } else {
            return true;
        }
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
        if (Build.VERSION.SDK_INT >= 24) {
            mInfo.setImportantForAccessibility(important);
        }
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
        if (hasSpans()) {
            List<Integer> starts = extrasIntList(SPANS_START_KEY);
            List<Integer> ends = extrasIntList(SPANS_END_KEY);
            List<Integer> flags = extrasIntList(SPANS_FLAGS_KEY);
            List<Integer> ids = extrasIntList(SPANS_ID_KEY);
            Spannable spannable = new SpannableString(TextUtils.substring(mInfo.getText(),
                    0, mInfo.getText().length()));
            for (int i = 0; i < starts.size(); i++) {
                spannable.setSpan(new AccessibilityClickableSpanCompat(ids.get(i), this,
                                getExtras().getInt(SPANS_ACTION_ID_KEY)),
                        starts.get(i), ends.get(i), flags.get(i));
            }
            return spannable;
        } else {
            return mInfo.getText();
        }
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
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void addSpansToExtras(CharSequence text, View view) {
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 26) {
            clearExtrasSpans();
            removeCollectedSpans(view);
            ClickableSpan[] spans = getClickableSpans(text);
            if (spans != null && spans.length > 0) {
                getExtras().putInt(SPANS_ACTION_ID_KEY, R.id.accessibility_action_clickable_span);
                SparseArray<WeakReference<ClickableSpan>> tagSpans =
                        getOrCreateSpansFromViewTags(view);
                for (int i = 0; spans != null && i < spans.length; i++) {
                    int id = idForClickableSpan(spans[i], tagSpans);
                    tagSpans.put(id, new WeakReference<>(spans[i]));
                    addSpanLocationToExtras(spans[i], (Spanned) text, id);
                }
            }
        }
    }

    private SparseArray<WeakReference<ClickableSpan>> getOrCreateSpansFromViewTags(View host) {
        SparseArray<WeakReference<ClickableSpan>> spans = getSpansFromViewTags(host);
        if (spans == null) {
            spans = new SparseArray<>();
            host.setTag(R.id.tag_accessibility_clickable_spans, spans);
        }
        return spans;
    }

    @SuppressWarnings("unchecked")
    private SparseArray<WeakReference<ClickableSpan>> getSpansFromViewTags(View host) {
        return (SparseArray<WeakReference<ClickableSpan>>) host.getTag(
                R.id.tag_accessibility_clickable_spans);
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static ClickableSpan[] getClickableSpans(CharSequence text) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            return spanned.getSpans(0, text.length(), ClickableSpan.class);
        }
        return null;
    }

    private int idForClickableSpan(ClickableSpan span,
            SparseArray<WeakReference<ClickableSpan>> spans) {
        if (spans != null) {
            for (int i = 0; i < spans.size(); i++) {
                ClickableSpan aSpan = spans.valueAt(i).get();
                if (span.equals(aSpan)) {
                    return spans.keyAt(i);
                }
            }
        }
        return sClickableSpanId++;
    }

    private boolean hasSpans() {
        return !extrasIntList(SPANS_START_KEY).isEmpty();
    }

    private void clearExtrasSpans() {
        if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).remove(SPANS_START_KEY);
            Api19Impl.getExtras(mInfo).remove(SPANS_END_KEY);
            Api19Impl.getExtras(mInfo).remove(SPANS_FLAGS_KEY);
            Api19Impl.getExtras(mInfo).remove(SPANS_ID_KEY);
        }
    }

    private void addSpanLocationToExtras(ClickableSpan span, Spanned spanned, int id) {
        extrasIntList(SPANS_START_KEY).add(spanned.getSpanStart(span));
        extrasIntList(SPANS_END_KEY).add(spanned.getSpanEnd(span));
        extrasIntList(SPANS_FLAGS_KEY).add(spanned.getSpanFlags(span));
        extrasIntList(SPANS_ID_KEY).add(id);
    }

    private void removeCollectedSpans(View view) {
        SparseArray<WeakReference<ClickableSpan>> spans = getSpansFromViewTags(view);
        if (spans != null) {
            List<Integer> toBeRemovedIndices = new ArrayList<>();
            for (int i = 0; i < spans.size(); i++) {
                if (spans.valueAt(i).get() == null) {
                    toBeRemovedIndices.add(i);
                }
            }
            for (int i = 0; i < toBeRemovedIndices.size(); i++) {
                spans.remove(toBeRemovedIndices.get(i));
            }
        }
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
     * Gets the state description of this node.
     *
     * @return the state description or null if android version smaller
     * than 19.
     */
    public @Nullable CharSequence getStateDescription() {
        if (BuildCompat.isAtLeastR()) {
            return mInfo.getStateDescription();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getCharSequence(STATE_DESCRIPTION_KEY);
        }
        return null;
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
     * Sets the state description of this node.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param stateDescription the state description of this node.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setStateDescription(@Nullable CharSequence stateDescription) {
        if (BuildCompat.isAtLeastR()) {
            mInfo.setStateDescription(stateDescription);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putCharSequence(STATE_DESCRIPTION_KEY, stateDescription);
        }
    }

    /**
     * Gets the unique id of this node.
     *
     * @return the unique id or null if android version smaller
     * than 19.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    public @Nullable String getUniqueId() {
        if (BuildCompat.isAtLeastT()) {
            return mInfo.getUniqueId();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getString(UNIQUE_ID_KEY);
        }
        return null;
    }

    /**
     * Sets the unique id of this node.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param uniqueId the unique id of this node.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    public void setUniqueId(@Nullable String uniqueId) {
        if (BuildCompat.isAtLeastT()) {
            mInfo.setUniqueId(uniqueId);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putString(UNIQUE_ID_KEY, uniqueId);
        }
    }

    /**
     * Return an instance back to be reused.
     * <p>
     * <strong>Note:</strong> You must not touch the object after calling this function.
     *
     * @throws IllegalStateException If the info is already recycled.
     * @deprecated Accessibility Object recycling is no longer necessary or functional.
     */
    @Deprecated
    public void recycle() { }

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
        if (Build.VERSION.SDK_INT >= 18) {
            mInfo.setViewIdResourceName(viewId);
        }
    }

    /**
     * Gets the fully qualified resource name of the source view's id.
     *
     * <p>
     *   <strong>Note:</strong> The primary usage of this API is for UI test automation
     *   and in order to report the source view id of an {@link AccessibilityNodeInfoCompat}
     *   the client has to set the {@link AccessibilityServiceInfoCompat#FLAG_REPORT_VIEW_IDS}
     *   flag when configuring their {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @return The id resource name.
     */
    public String getViewIdResourceName() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mInfo.getViewIdResourceName();
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.getLiveRegion();
        } else {
            return ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setLiveRegion(mode);
        }
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
        if (Build.VERSION.SDK_INT >= 24) {
            return mInfo.getDrawingOrder();
        } else {
            return 0;
        }
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
        if (Build.VERSION.SDK_INT >= 24) {
            mInfo.setDrawingOrder(drawingOrderInParent);
        }
    }

    /**
     * Gets the collection info if the node is a collection. A collection
     * child is always a collection item.
     *
     * @return The collection info.
     */
    public CollectionInfoCompat getCollectionInfo() {
        if (Build.VERSION.SDK_INT >= 19) {
            AccessibilityNodeInfo.CollectionInfo info = mInfo.getCollectionInfo();
            if (info != null) {
                return new CollectionInfoCompat(info);
            }
        }
        return null;
    }

    public void setCollectionInfo(Object collectionInfo) {
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setCollectionInfo((collectionInfo == null) ? null
                    : (AccessibilityNodeInfo.CollectionInfo) ((CollectionInfoCompat)
                            collectionInfo).mInfo);
        }

    }

    public void setCollectionItemInfo(Object collectionItemInfo) {
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setCollectionItemInfo((collectionItemInfo == null) ? null
                    : (AccessibilityNodeInfo.CollectionItemInfo) ((CollectionItemInfoCompat)
                            collectionItemInfo).mInfo);
        }
    }

    /**
     * Gets the collection item info if the node is a collection item. A collection
     * item is always a child of a collection.
     *
     * @return The collection item info.
     */
    public CollectionItemInfoCompat getCollectionItemInfo() {
        if (Build.VERSION.SDK_INT >= 19) {
            AccessibilityNodeInfo.CollectionItemInfo info = mInfo.getCollectionItemInfo();
            if (info != null) {
                return new CollectionItemInfoCompat(info);
            }
        }
        return null;
    }

    /**
     * Gets the range info if this node is a range.
     *
     * @return The range.
     */
    public RangeInfoCompat getRangeInfo() {
        if (Build.VERSION.SDK_INT >= 19) {
            AccessibilityNodeInfo.RangeInfo info = mInfo.getRangeInfo();
            if (info != null) {
                return new RangeInfoCompat(info);
            }
        }
        return null;
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setRangeInfo((AccessibilityNodeInfo.RangeInfo) rangeInfo.mInfo);
        }
    }

    /**
     * Gets the {@link android.view.accessibility.AccessibilityNodeInfo.ExtraRenderingInfo
     * extra rendering info} if the node is meant to be refreshed with extra data
     * to examine rendering related accessibility issues.
     *
     * @return The {@link android.view.accessibility.AccessibilityNodeInfo.ExtraRenderingInfo
     * extra rendering info}.
     */
    @Nullable
    public AccessibilityNodeInfo.ExtraRenderingInfo getExtraRenderingInfo() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.getExtraRenderingInfo(mInfo);
        } else {
            return null;
        }
    }

    /**
     * Gets the actions that can be performed on the node.
     *
     * @return A list of AccessibilityActions.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: Always returns {@code null}</li>
     * </ul>
     */
    @SuppressWarnings({"unchecked", "MixedMutabilityReturnType"})
    public List<AccessibilityActionCompat> getActionList() {
        List<Object> actions = null;
        if (Build.VERSION.SDK_INT >= 21) {
            actions = (List<Object>) (List<?>) mInfo.getActionList();
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setContentInvalid(contentInvalid);
        }
    }

    /**
     * Gets if the content of this node is invalid. For example,
     * a date is not well-formed.
     *
     * @return If the node content is invalid.
     */
    public boolean isContentInvalid() {
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.isContentInvalid();
        } else {
            return false;
        }
    }

    /**
     * Gets whether this node is context clickable.
     *
     * @return True if the node is context clickable.
     */
    public boolean isContextClickable() {
        if (Build.VERSION.SDK_INT >= 23) {
            return mInfo.isContextClickable();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 23) {
            mInfo.setContextClickable(contextClickable);
        }
    }

    /**
     * Gets the hint text of this node. Only applies to nodes where text can be entered.
     *
     * @return The hint text.
     */
    public @Nullable CharSequence getHintText() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mInfo.getHintText();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getCharSequence(HINT_TEXT_KEY);
        }
        return null;
    }

    /**
     * Sets the hint text of this node. Only applies to nodes where text can be entered.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param hintText The hint text for this mode.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setHintText(@Nullable CharSequence hintText) {
        if (Build.VERSION.SDK_INT >= 26) {
            mInfo.setHintText(hintText);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putCharSequence(HINT_TEXT_KEY, hintText);
        }
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
        if (Build.VERSION.SDK_INT >= 21) {
            mInfo.setError(error);
        }
    }

    /**
     * Gets the error text of this node.
     *
     * @return The error text.
     */
    public CharSequence getError() {
        if (Build.VERSION.SDK_INT >= 21) {
            return mInfo.getError();
        } else {
            return null;
        }
    }

    /**
     * Sets the view for which the view represented by this info serves as a
     * label for accessibility purposes.
     *
     * @param labeled The view for which this info serves as a label.
     */
    public void setLabelFor(View labeled) {
        if (Build.VERSION.SDK_INT >= 17) {
            mInfo.setLabelFor(labeled);
        }
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
        if (Build.VERSION.SDK_INT >= 17) {
            mInfo.setLabelFor(root, virtualDescendantId);
        }
    }

    /**
     * Gets the node info for which the view represented by this info serves as
     * a label for accessibility purposes.
     *
     * @return The labeled info.
     */
    public AccessibilityNodeInfoCompat getLabelFor() {
        if (Build.VERSION.SDK_INT >= 17) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getLabelFor());
        } else {
            return null;
        }
    }

    /**
     * Sets the view which serves as the label of the view represented by
     * this info for accessibility purposes.
     *
     * @param label The view that labels this node's source.
     */
    public void setLabeledBy(View label) {
        if (Build.VERSION.SDK_INT >= 17) {
            mInfo.setLabeledBy(label);
        }
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
        if (Build.VERSION.SDK_INT >= 17) {
            mInfo.setLabeledBy(root, virtualDescendantId);
        }
    }

    /**
     * Gets the node info which serves as the label of the view represented by
     * this info for accessibility purposes.
     *
     * @return The label.
     */
    public AccessibilityNodeInfoCompat getLabeledBy() {
        if (Build.VERSION.SDK_INT >= 17) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getLabeledBy());
        } else {
            return null;
        }
    }

    /**
     * Gets if this node opens a popup or a dialog.
     *
     * @return If the the node opens a popup.
     */
    public boolean canOpenPopup() {
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.canOpenPopup();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setCanOpenPopup(opensPopup);
        }
    }

    /**
     * Finds {@link AccessibilityNodeInfoCompat}s by the fully qualified view id's resource
     * name where a fully qualified id is of the from "package:id/id_resource_name".
     * For example, if the target application's package is "foo.bar" and the id
     * resource name is "baz", the fully qualified resource id is "foo.bar:id/baz".
     *
     * <p>
     *   <strong>Note:</strong> The primary usage of this API is for UI test automation
     *   and in order to report the fully qualified view id if an
     *   {@link AccessibilityNodeInfoCompat} the client has to set the
     *   {@link android.accessibilityservice.AccessibilityServiceInfo#FLAG_REPORT_VIEW_IDS}
     *   flag when configuring their {@link android.accessibilityservice.AccessibilityService}.
     * </p>
     *
     * @param viewId The fully qualified resource name of the view id to find.
     * @return A list of node info.
     */
    @SuppressWarnings("MixedMutabilityReturnType")
    public List<AccessibilityNodeInfoCompat> findAccessibilityNodeInfosByViewId(String viewId) {
        if (Build.VERSION.SDK_INT >= 18) {
            List<AccessibilityNodeInfo> nodes = mInfo.findAccessibilityNodeInfosByViewId(viewId);
            List<AccessibilityNodeInfoCompat> result = new ArrayList<>();
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
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo);
        } else {
            return new Bundle();
        }
    }

    /**
     * Gets the input type of the source as defined by {@link InputType}.
     *
     * @return The input type.
     */
    public int getInputType() {
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.getInputType();
        } else {
            return InputType.TYPE_NULL;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setInputType(inputType);
        }
    }

    /**
     * Get the extra data available for this node.
     * <p>
     * Some data that is useful for some accessibility services is expensive to compute, and would
     * place undue overhead on apps to compute all the time. That data can be requested with
     * {@link #refreshWithExtraData(String, Bundle)}.
     *
     * @return An unmodifiable list of keys corresponding to extra data that can be requested.
     * @see #EXTRA_DATA_RENDERING_INFO_KEY
     * @see #EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
     */
    @NonNull
    public List<String> getAvailableExtraData() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mInfo.getAvailableExtraData();
        } else {
            return emptyList();
        }
    }

    /**
     * Set the extra data available for this node.
     * <p>
     * <strong>Note:</strong> When a {@code View} passes in a non-empty list, it promises that
     * it will populate the node's extras with corresponding pieces of information in
     * {@link View#addExtraDataToAccessibilityNodeInfo(AccessibilityNodeInfo, String, Bundle)}.
     * <p>
     * <strong>Note:</strong> Cannot be called from an
     * {@link android.accessibilityservice.AccessibilityService}.
     * This class is made immutable before being delivered to an AccessibilityService.
     *
     * @param extraDataKeys A list of types of extra data that are available.
     * @see #getAvailableExtraData()
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setAvailableExtraData(@NonNull List<String> extraDataKeys) {
        if (Build.VERSION.SDK_INT >= 26) {
            mInfo.setAvailableExtraData(extraDataKeys);
        }
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
        if (Build.VERSION.SDK_INT >= 21) {
            mInfo.setMaxTextLength(max);
        }
    }

    /**
     * Returns the maximum text length for this node.
     *
     * @return The maximum text length, or -1 for no limit.
     * @see #setMaxTextLength(int)
     */
    public int getMaxTextLength() {
        if (Build.VERSION.SDK_INT >= 21) {
            return mInfo.getMaxTextLength();
        } else {
            return -1;
        }
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
        if (Build.VERSION.SDK_INT >= 18) {
            mInfo.setTextSelection(start, end);
        }
    }

    /**
     * Gets the text selection start.
     *
     * @return The text selection start if there is selection or -1.
     */
    public int getTextSelectionStart() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mInfo.getTextSelectionStart();
        } else {
            return -1;
        }
    }

    /**
     * Gets the text selection end.
     *
     * @return The text selection end if there is selection or -1.
     */
    public int getTextSelectionEnd() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mInfo.getTextSelectionEnd();
        } else {
            return -1;
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getTraversalBefore());
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            mInfo.setTraversalBefore(view);
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            mInfo.setTraversalBefore(root, virtualDescendantId);
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            return AccessibilityNodeInfoCompat.wrapNonNullInstance(mInfo.getTraversalAfter());
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            mInfo.setTraversalAfter(view);
        }
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
        if (Build.VERSION.SDK_INT >= 22) {
            mInfo.setTraversalAfter(root, virtualDescendantId);
        }
    }

    /**
     * Gets the window to which this node belongs.
     *
     * @return The window.
     *
     * @see android.accessibilityservice.AccessibilityService#getWindows()
     */
    public AccessibilityWindowInfoCompat getWindow() {
        if (Build.VERSION.SDK_INT >= 21) {
            return AccessibilityWindowInfoCompat.wrapNonNullInstance(mInfo.getWindow());
        } else {
            return null;
        }
    }

    /**
     * Gets if the node can be dismissed.
     *
     * @return If the node can be dismissed.
     */
    public boolean isDismissable() {
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.isDismissable();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setDismissable(dismissable);
        }
    }

    /**
     * Gets if the node is editable.
     *
     * @return True if the node is editable, false otherwise.
     */
    public boolean isEditable() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mInfo.isEditable();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 18) {
            mInfo.setEditable(editable);
        }
    }

    /**
     * Gets if the node is a multi line editable text.
     *
     * @return True if the node is multi line.
     */
    public boolean isMultiLine() {
        if (Build.VERSION.SDK_INT >= 19) {
            return mInfo.isMultiLine();
        } else {
            return false;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            mInfo.setMultiLine(multiLine);
        }
    }

    /**
     * Gets the tooltip text of this node.
     *
     * @return The tooltip text.
     */
    @Nullable
    public CharSequence getTooltipText() {
        if (Build.VERSION.SDK_INT >= 28) {
            return mInfo.getTooltipText();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getCharSequence(TOOLTIP_TEXT_KEY);
        }
        return null;
    }

    /**
     * Sets the tooltip text of this node.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param tooltipText The tooltip text.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setTooltipText(@Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT >= 28) {
            mInfo.setTooltipText(tooltipText);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putCharSequence(TOOLTIP_TEXT_KEY, tooltipText);
        }
    }

    /**
     * If this node represents a visually distinct region of the screen that may update separately
     * from the rest of the window, it is considered a pane. Set the pane title to indicate that
     * the node is a pane, and to provide a title for it.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     * @param paneTitle The title of the window represented by this node.
     */
    public void setPaneTitle(@Nullable CharSequence paneTitle) {
        if (Build.VERSION.SDK_INT >= 28) {
            mInfo.setPaneTitle(paneTitle);
        } else if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putCharSequence(PANE_TITLE_KEY, paneTitle);
        }
    }

    /**
     * Get the title of the pane represented by this node.
     *
     * @return The title of the pane represented by this node, or {@code null} if this node does
     *         not represent a pane.
     */
    public @Nullable CharSequence getPaneTitle() {
        if (Build.VERSION.SDK_INT >= 28) {
            return mInfo.getPaneTitle();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getCharSequence(PANE_TITLE_KEY);
        }
        return null;
    }

    /**
     * Returns whether the node is explicitly marked as a focusable unit by a screen reader. Note
     * that {@code false} indicates that it is not explicitly marked, not that the node is not
     * a focusable unit. Screen readers should generally use other signals, such as
     * {@link #isFocusable()}, or the presence of text in a node, to determine what should receive
     * focus.
     *
     * @return {@code true} if the node is specifically marked as a focusable unit for screen
     *         readers, {@code false} otherwise.
     */
    public boolean isScreenReaderFocusable() {
        if (Build.VERSION.SDK_INT >= 28) {
            return mInfo.isScreenReaderFocusable();
        }
        return getBooleanProperty(BOOLEAN_PROPERTY_SCREEN_READER_FOCUSABLE);
    }

    /**
     * Sets whether the node should be considered a focusable unit by a screen reader.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param screenReaderFocusable {@code true} if the node is a focusable unit for screen readers,
     *                              {@code false} otherwise.
     */
    public void setScreenReaderFocusable(boolean screenReaderFocusable) {
        if (Build.VERSION.SDK_INT >= 28) {
            mInfo.setScreenReaderFocusable(screenReaderFocusable);
        } else {
            setBooleanProperty(BOOLEAN_PROPERTY_SCREEN_READER_FOCUSABLE, screenReaderFocusable);
        }
    }

    /**
     * Returns whether the node's text represents a hint for the user to enter text. It should only
     * be {@code true} if the node has editable text.
     *
     * @return {@code true} if the text in the node represents a hint to the user, {@code false}
     * otherwise.
     */
    public boolean isShowingHintText() {
        if (Build.VERSION.SDK_INT >= 26) {
            return mInfo.isShowingHintText();
        }
        return getBooleanProperty(BOOLEAN_PROPERTY_IS_SHOWING_HINT);
    }

    /**
     * Sets whether the node's text represents a hint for the user to enter text. It should only
     * be {@code true} if the node has editable text.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param showingHintText {@code true} if the text in the node represents a hint to the user,
     * {@code false} otherwise.
     */
    public void setShowingHintText(boolean showingHintText) {
        if (Build.VERSION.SDK_INT >= 26) {
            mInfo.setShowingHintText(showingHintText);
        } else {
            setBooleanProperty(BOOLEAN_PROPERTY_IS_SHOWING_HINT, showingHintText);
        }
    }

    /**
     * Returns whether node represents a heading.
     * <p><strong>Note:</strong> Returns {@code true} if either {@link #setHeading(boolean)}
     * marks this node as a heading or if the node has a {@link CollectionItemInfoCompat} that marks
     * it as such, to accomodate apps that use the now-deprecated API.</p>
     *
     * @return {@code true} if the node is a heading, {@code false} otherwise.
     */
    @SuppressWarnings("deprecation")
    public boolean isHeading() {
        if (Build.VERSION.SDK_INT >= 28) {
            return mInfo.isHeading();
        }
        if (getBooleanProperty(BOOLEAN_PROPERTY_IS_HEADING)) return true;
        CollectionItemInfoCompat collectionItemInfo = getCollectionItemInfo();
        return (collectionItemInfo != null) && collectionItemInfo.isHeading();
    }

    /**
     * Sets whether the node represents a heading.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param isHeading {@code true} if the node is a heading, {@code false} otherwise.
     */
    public void setHeading(boolean isHeading) {
        if (Build.VERSION.SDK_INT >= 28) {
            mInfo.setHeading(isHeading);
        } else {
            setBooleanProperty(BOOLEAN_PROPERTY_IS_HEADING, isHeading);
        }
    }

    /**
     * Returns whether node represents a text entry key that is part of a keyboard or keypad.
     *
     * @return {@code true} if the node is a text entry key, {@code false} otherwise.
     */
    public boolean isTextEntryKey() {
        if (Build.VERSION.SDK_INT >= 29) {
            return mInfo.isTextEntryKey();
        }
        return getBooleanProperty(BOOLEAN_PROPERTY_IS_TEXT_ENTRY_KEY);
    }

    /**
     * Sets whether the node represents a text entry key that is part of a keyboard or keypad.
     * <p>This method has no effect below API 19</p>
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param isTextEntryKey {@code true} if the node is a text entry key, {@code false} otherwise.
     */
    public void setTextEntryKey(boolean isTextEntryKey) {
        if (Build.VERSION.SDK_INT >= 29) {
            mInfo.setTextEntryKey(isTextEntryKey);
        } else {
            setBooleanProperty(BOOLEAN_PROPERTY_IS_TEXT_ENTRY_KEY, isTextEntryKey);
        }
    }

    /**
     * Gets whether the node has {@link #setRequestInitialAccessibilityFocus}.
     *
     * @return True if the node has requested initial accessibility focus.
     */
    @SuppressLint("KotlinPropertyAccess")
    public boolean hasRequestInitialAccessibilityFocus() {
        return getBooleanProperty(BOOLEAN_PROPERTY_HAS_REQUEST_INITIAL_ACCESSIBILITY_FOCUS);
    }

    /**
     * Sets whether the node has requested initial accessibility focus.
     *
     * <p>
     * If the node {@link #hasRequestInitialAccessibilityFocus}, this node would be one of
     * candidates to be accessibility focused when the window appears.
     * </p>
     *
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     *
     * @param requestInitialAccessibilityFocus True if the node requests to receive initial
     *                                         accessibility focus.
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    @SuppressLint("GetterSetterNames")
    public void setRequestInitialAccessibilityFocus(boolean requestInitialAccessibilityFocus) {
        setBooleanProperty(BOOLEAN_PROPERTY_HAS_REQUEST_INITIAL_ACCESSIBILITY_FOCUS,
                requestInitialAccessibilityFocus);
    }

    /**
     * Refreshes this info with the latest state of the view it represents.
     * <p>
     * <strong>Note:</strong> If this method returns false this info is obsolete
     * since it represents a view that is no longer in the view tree.
     * </p>
     * @return Whether the refresh succeeded.
     */
    public boolean refresh() {
        if (Build.VERSION.SDK_INT >= 18) {
            return mInfo.refresh();
        } else {
            return false;
        }
    }

    /**
     * Gets the custom role description.
     * @return The role description.
     */
    public @Nullable CharSequence getRoleDescription() {
        if (Build.VERSION.SDK_INT >= 19) {
            return Api19Impl.getExtras(mInfo).getCharSequence(ROLE_DESCRIPTION_KEY);
        } else {
            return null;
        }
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
        if (Build.VERSION.SDK_INT >= 19) {
            Api19Impl.getExtras(mInfo).putCharSequence(ROLE_DESCRIPTION_KEY, roleDescription);
        }
    }

    /**
     * Get the {@link TouchDelegateInfoCompat} for touch delegate behavior with the represented
     * view. It is possible for the same node to be pointed to by several regions. Use
     * {@link TouchDelegateInfoCompat#getRegionAt(int)} to get touch delegate target
     * {@link Region}, and {@link TouchDelegateInfoCompat#getTargetForRegion(Region)}
     * for {@link AccessibilityNodeInfoCompat} from the given region.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 29: Always returns {@code null}</li>
     * </ul>
     *
     * @return {@link TouchDelegateInfoCompat} or {@code null} if there are no touch delegates
     * in this node.
     */
    @Nullable
    public TouchDelegateInfoCompat getTouchDelegateInfo() {
        if (Build.VERSION.SDK_INT >= 29) {
            TouchDelegateInfo delegateInfo = mInfo.getTouchDelegateInfo();
            if (delegateInfo != null) {
                return new TouchDelegateInfoCompat(delegateInfo);
            }
        }
        return null;
    }

    /**
     * Set touch delegate info if the represented view has a {@link android.view.TouchDelegate}.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an
     *   AccessibilityService.
     * </p>
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 29: No-op</li>
     * </ul>
     *
     * @param delegatedInfo {@link TouchDelegateInfoCompat}
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public void setTouchDelegateInfo(@NonNull TouchDelegateInfoCompat delegatedInfo) {
        if (Build.VERSION.SDK_INT >= 29) {
            mInfo.setTouchDelegateInfo(delegatedInfo.mInfo);
        }
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
        if (!(obj instanceof AccessibilityNodeInfoCompat)) {
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
        if (mVirtualDescendantId != other.mVirtualDescendantId) {
            return false;
        }
        if (mParentVirtualDescendantId != other.mParentVirtualDescendantId) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @NonNull
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
        builder.append("; uniqueId: ").append(getUniqueId());

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
        if (Build.VERSION.SDK_INT >= 21) {
            List<AccessibilityActionCompat> actions = getActionList();
            for (int i = 0; i < actions.size(); i++) {
                AccessibilityActionCompat action = actions.get(i);
                String actionName = getActionSymbolicName(action.getId());
                if (actionName.equals("ACTION_UNKNOWN") && action.getLabel() != null) {
                    actionName = action.getLabel().toString();
                }
                builder.append(actionName);
                if (i != actions.size() - 1) {
                    builder.append(", ");
                }
            }
        } else {
            for (int actionBits = getActions(); actionBits != 0;) {
                final int action = 1 << Integer.numberOfTrailingZeros(actionBits);
                actionBits &= ~action;
                builder.append(getActionSymbolicName(action));
                if (actionBits != 0) {
                    builder.append(", ");
                }
            }
        }
        builder.append("]");

        return builder.toString();
    }

    private void setBooleanProperty(int property, boolean value) {
        Bundle extras = getExtras();
        if (extras != null) {
            int booleanProperties = extras.getInt(BOOLEAN_PROPERTY_KEY, 0);
            booleanProperties &= ~property;
            booleanProperties |= value ? property : 0;
            extras.putInt(BOOLEAN_PROPERTY_KEY, booleanProperties);
        }
    }

    private boolean getBooleanProperty(int property) {
        Bundle extras = getExtras();
        if (extras == null) return false;
        return (extras.getInt(BOOLEAN_PROPERTY_KEY, 0) & property) == property;
    }

    static String getActionSymbolicName(int action) {
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
            case ACTION_EXPAND:
                return "ACTION_EXPAND";
            case ACTION_COLLAPSE:
                return "ACTION_COLLAPSE";
            case ACTION_SET_TEXT:
                return "ACTION_SET_TEXT";
            case android.R.id.accessibilityActionScrollUp:
                return "ACTION_SCROLL_UP";
            case android.R.id.accessibilityActionScrollLeft:
                return "ACTION_SCROLL_LEFT";
            case android.R.id.accessibilityActionScrollDown:
                return "ACTION_SCROLL_DOWN";
            case android.R.id.accessibilityActionScrollRight:
                return "ACTION_SCROLL_RIGHT";
            case android.R.id.accessibilityActionPageDown:
                return "ACTION_PAGE_DOWN";
            case android.R.id.accessibilityActionPageUp:
                return "ACTION_PAGE_UP";
            case android.R.id.accessibilityActionPageLeft:
                return "ACTION_PAGE_LEFT";
            case android.R.id.accessibilityActionPageRight:
                return "ACTION_PAGE_RIGHT";
            case android.R.id.accessibilityActionShowOnScreen:
                return "ACTION_SHOW_ON_SCREEN";
            case android.R.id.accessibilityActionScrollToPosition:
                return "ACTION_SCROLL_TO_POSITION";
            case android.R.id.accessibilityActionContextClick:
                return "ACTION_CONTEXT_CLICK";
            case android.R.id.accessibilityActionSetProgress:
                return "ACTION_SET_PROGRESS";
            case android.R.id.accessibilityActionMoveWindow:
                return "ACTION_MOVE_WINDOW";
            case android.R.id.accessibilityActionShowTooltip:
                return "ACTION_SHOW_TOOLTIP";
            case android.R.id.accessibilityActionHideTooltip:
                return "ACTION_HIDE_TOOLTIP";
            case android.R.id.accessibilityActionPressAndHold:
                return "ACTION_PRESS_AND_HOLD";
            case android.R.id.accessibilityActionImeEnter:
                return "ACTION_IME_ENTER";
            case android.R.id.accessibilityActionDragStart:
                return "ACTION_DRAG_START";
            case android.R.id.accessibilityActionDragDrop:
                return "ACTION_DRAG_DROP";
            case android.R.id.accessibilityActionDragCancel:
                return "ACTION_DRAG_CANCEL";
            default:
                return "ACTION_UNKNOWN";
        }
    }

    @RequiresApi(33)
    private static class Api33Impl {
        private Api33Impl() {
            // This class is non instantiable.
        }

        @DoNotInline
        public static AccessibilityNodeInfo.ExtraRenderingInfo getExtraRenderingInfo(
                AccessibilityNodeInfo info) {
            return info.getExtraRenderingInfo();
        }

        @DoNotInline
        public static boolean isTextSelectable(AccessibilityNodeInfo info) {
            return info.isTextSelectable();
        }

        @DoNotInline
        public static void setTextSelectable(AccessibilityNodeInfo info, boolean selectable) {
            info.setTextSelectable(selectable);
        }
    }

    @RequiresApi(19)
    private static class Api19Impl {
        private Api19Impl() {
            // This class is non instantiable.
        }

        @DoNotInline
        public static Bundle getExtras(AccessibilityNodeInfo info) {
            return info.getExtras();
        }
    }
}
