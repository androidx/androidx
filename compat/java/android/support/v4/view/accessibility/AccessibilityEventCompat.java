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

import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityRecord;

/**
 * Helper for accessing features in {@link AccessibilityEvent}.
 */
public final class AccessibilityEventCompat {
    /**
     * Represents the event of a hover enter over a {@link android.view.View}.
     * @deprecated Use {@link  AccessibilityEvent#TYPE_VIEW_HOVER_ENTER} directly.
     */
    @Deprecated
    public static final int TYPE_VIEW_HOVER_ENTER = AccessibilityEvent.TYPE_VIEW_HOVER_ENTER;

    /**
     * Represents the event of a hover exit over a {@link android.view.View}.
     * @deprecated Use {@link  AccessibilityEvent#TYPE_VIEW_HOVER_EXIT} directly.
     */
    @Deprecated
    public static final int TYPE_VIEW_HOVER_EXIT = AccessibilityEvent.TYPE_VIEW_HOVER_EXIT;

    /**
     * Represents the event of starting a touch exploration gesture.
     * @deprecated Use {@link  AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_START} directly.
     */
    @Deprecated
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START =
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START;

    /**
     * Represents the event of ending a touch exploration gesture.
     * @deprecated Use {@link AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_END} directly.
     */
    @Deprecated
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END =
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END;

    /**
     * Represents the event of changing the content of a window.
     * @deprecated Use {@link AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED} directly.
     */
    @Deprecated
    public static final int TYPE_WINDOW_CONTENT_CHANGED =
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

    /**
     * Represents the event of scrolling a view.
     * @deprecated Use {@link AccessibilityEvent#TYPE_VIEW_SCROLLED} directly.
     */
    @Deprecated
    public static final int TYPE_VIEW_SCROLLED = AccessibilityEvent.TYPE_VIEW_SCROLLED;

    /**
     * Represents the event of changing the selection in an {@link android.widget.EditText}.
     * @deprecated Use {@link AccessibilityEvent#TYPE_VIEW_TEXT_SELECTION_CHANGED} directly.
     */
    @Deprecated
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED =
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;

    /**
     * Represents the event of an application making an announcement.
     */
    public static final int TYPE_ANNOUNCEMENT = 0x00004000;

    /**
     * Represents the event of gaining accessibility focus.
     */
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUSED = 0x00008000;

    /**
     * Represents the event of clearing accessibility focus.
     */
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED = 0x00010000;

    /**
     * Represents the event of traversing the text of a view at a given movement granularity.
     */
    public static final int TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY = 0x00020000;

    /**
     * Represents the event of beginning gesture detection.
     */
    public static final int TYPE_GESTURE_DETECTION_START = 0x00040000;

    /**
     * Represents the event of ending gesture detection.
     */
    public static final int TYPE_GESTURE_DETECTION_END = 0x00080000;

    /**
     * Represents the event of the user starting to touch the screen.
     */
    public static final int TYPE_TOUCH_INTERACTION_START = 0x00100000;

    /**
     * Represents the event of the user ending to touch the screen.
     */
    public static final int TYPE_TOUCH_INTERACTION_END = 0x00200000;

    /**
     * Represents the event change in the windows shown on the screen.
     */
    public static final int TYPE_WINDOWS_CHANGED = 0x00400000;

    /**
     * Represents the event of a context click on a {@link android.view.View}.
     */
    public static final int TYPE_VIEW_CONTEXT_CLICKED = 0x00800000;

    /**
     * Represents the event of the assistant currently reading the users screen context.
     */
    public static final int TYPE_ASSIST_READING_CONTEXT = 0x01000000;

    /**
     * Change type for {@link #TYPE_WINDOW_CONTENT_CHANGED} event:
     * The type of change is not defined.
     */
    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0x00000000;

    /**
     * Change type for {@link #TYPE_WINDOW_CONTENT_CHANGED} event:
     * A node in the subtree rooted at the source node was added or removed.
     */
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 0x00000001;

    /**
     * Change type for {@link #TYPE_WINDOW_CONTENT_CHANGED} event:
     * The node's text changed.
     */
    public static final int CONTENT_CHANGE_TYPE_TEXT = 0x00000002;

    /**
     * Change type for {@link #TYPE_WINDOW_CONTENT_CHANGED} event:
     * The node's content description changed.
     */
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 0x00000004;

    /**
     * Mask for {@link AccessibilityEvent} all types.
     *
     * @see AccessibilityEvent#TYPE_VIEW_CLICKED
     * @see AccessibilityEvent#TYPE_VIEW_LONG_CLICKED
     * @see AccessibilityEvent#TYPE_VIEW_SELECTED
     * @see AccessibilityEvent#TYPE_VIEW_FOCUSED
     * @see AccessibilityEvent#TYPE_VIEW_TEXT_CHANGED
     * @see AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED
     * @see AccessibilityEvent#TYPE_NOTIFICATION_STATE_CHANGED
     * @see AccessibilityEvent#TYPE_VIEW_HOVER_ENTER
     * @see AccessibilityEvent#TYPE_VIEW_HOVER_EXIT
     * @see AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_START
     * @see AccessibilityEvent#TYPE_TOUCH_EXPLORATION_GESTURE_END
     * @see AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED
     * @see AccessibilityEvent#TYPE_VIEW_SCROLLED
     * @see AccessibilityEvent#TYPE_VIEW_TEXT_SELECTION_CHANGED
     * @see #TYPE_ANNOUNCEMENT
     * @see #TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
     * @see #TYPE_GESTURE_DETECTION_START
     * @see #TYPE_GESTURE_DETECTION_END
     * @see #TYPE_TOUCH_INTERACTION_START
     * @see #TYPE_TOUCH_INTERACTION_END
     * @see #TYPE_WINDOWS_CHANGED
     * @see #TYPE_VIEW_CONTEXT_CLICKED
     * @see #TYPE_ASSIST_READING_CONTEXT
     */
    public static final int TYPES_ALL_MASK = 0xFFFFFFFF;

    /*
     * Hide constructor from clients.
     */
    private AccessibilityEventCompat() {

    }

    /**
     * Gets the number of records contained in the event.
     *
     * @return The number of records.
     *
     * @deprecated Use {@link AccessibilityEvent#getRecordCount()} directly.
     */
    @Deprecated
    public static int getRecordCount(AccessibilityEvent event) {
        return event.getRecordCount();
    }

    /**
     * Appends an {@link android.view.accessibility.AccessibilityRecord} to the end of
     * event records.
     *
     * @param record The record to append.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     *
     * @deprecated Use {@link AccessibilityEvent#appendRecord(AccessibilityRecord)} directly.
     */
    @Deprecated
    public static void appendRecord(AccessibilityEvent event, AccessibilityRecordCompat record) {
        event.appendRecord((AccessibilityRecord) record.getImpl());
    }

    /**
     * Gets the record at a given index.
     *
     * @param index The index.
     * @return The record at the specified index.
     *
     * @deprecated Use {@link AccessibilityEvent#getRecord(int)} directly.
     */
    @Deprecated
    public static AccessibilityRecordCompat getRecord(AccessibilityEvent event, int index) {
        return new AccessibilityRecordCompat(event.getRecord(index));
    }

    /**
     * Creates an {@link AccessibilityRecordCompat} from an {@link AccessibilityEvent}
     * that can be used to manipulate the event properties defined in
     * {@link android.view.accessibility.AccessibilityRecord}.
     * <p>
     * <strong>Note:</strong> Do not call {@link AccessibilityRecordCompat#recycle()} on the
     * returned {@link AccessibilityRecordCompat}. Call {@link AccessibilityEvent#recycle()}
     * in case you want to recycle the event.
     * </p>
     *
     * @param event The from which to create a record.
     * @return An {@link AccessibilityRecordCompat}.
     *
     * @deprecated Use the {@link AccessibilityEvent} directly as {@link AccessibilityRecord}.
     */
    @Deprecated
    public static AccessibilityRecordCompat asRecord(AccessibilityEvent event) {
        return new AccessibilityRecordCompat(event);
    }

    /**
     * Sets the bit mask of node tree changes signaled by an
     * {@link #TYPE_WINDOW_CONTENT_CHANGED} event.
     *
     * @param changeTypes The bit mask of change types.
     * @throws IllegalStateException If called from an AccessibilityService.
     * @see #getContentChangeTypes(AccessibilityEvent)
     */
    public static void setContentChangeTypes(AccessibilityEvent event, int changeTypes) {
        if (Build.VERSION.SDK_INT >= 19) {
            event.setContentChangeTypes(changeTypes);
        }
    }

    /**
     * Gets the bit mask of change types signaled by an
     * {@link #TYPE_WINDOW_CONTENT_CHANGED} event. A single event may represent
     * multiple change types.
     *
     * @return The bit mask of change types. One or more of:
     *         <ul>
     *         <li>{@link AccessibilityEvent#CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION}
     *         <li>{@link AccessibilityEvent#CONTENT_CHANGE_TYPE_SUBTREE}
     *         <li>{@link AccessibilityEvent#CONTENT_CHANGE_TYPE_TEXT}
     *         <li>{@link AccessibilityEvent#CONTENT_CHANGE_TYPE_UNDEFINED}
     *         </ul>
     */
    public static int getContentChangeTypes(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= 19) {
            return event.getContentChangeTypes();
        } else {
            return 0;
        }
    }

    /**
     * Sets the movement granularity that was traversed.
     *
     * @param granularity The granularity.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public static void setMovementGranularity(AccessibilityEvent event, int granularity) {
        if (Build.VERSION.SDK_INT >= 16) {
            event.setMovementGranularity(granularity);
        }
    }

    /**
     * Gets the movement granularity that was traversed.
     *
     * @return The granularity.
     */
    public static int getMovementGranularity(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= 16) {
            return event.getMovementGranularity();
        } else {
            return 0;
        }
    }

    /**
     * Sets the performed action that triggered this event.
     * <p>
     * Valid actions are defined in {@link AccessibilityNodeInfoCompat}:
     * <ul>
     * <li>{@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS}
     * <li>{@link AccessibilityNodeInfoCompat#ACTION_CLEAR_ACCESSIBILITY_FOCUS}
     * <li>{@link AccessibilityNodeInfoCompat#ACTION_CLEAR_FOCUS}
     * <li>{@link AccessibilityNodeInfoCompat#ACTION_CLEAR_SELECTION}
     * <li>{@link AccessibilityNodeInfoCompat#ACTION_CLICK}
     * <li>etc.
     * </ul>
     *
     * @param action The action.
     * @throws IllegalStateException If called from an AccessibilityService.
     * @see AccessibilityNodeInfoCompat#performAction(int)
     */
    public static void setAction(AccessibilityEvent event, int action) {
        if (Build.VERSION.SDK_INT >= 16) {
            event.setAction(action);
        }
    }

    /**
     * Gets the performed action that triggered this event.
     *
     * @return The action.
     */
    public static int getAction(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= 16) {
            return event.getAction();
        } else {
            return 0;
        }
    }
}
