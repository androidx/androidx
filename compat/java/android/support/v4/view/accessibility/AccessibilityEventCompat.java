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

/**
 * Helper for accessing features in {@link AccessibilityEvent}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class AccessibilityEventCompat {

    static interface AccessibilityEventVersionImpl {
        int getRecordCount(AccessibilityEvent event);
        void appendRecord(AccessibilityEvent event, Object record);
        Object getRecord(AccessibilityEvent event, int index);
        void setContentChangeTypes(AccessibilityEvent event, int types);
        int getContentChangeTypes(AccessibilityEvent event);
    }

    static class AccessibilityEventStubImpl implements AccessibilityEventVersionImpl {

        @Override
        public void appendRecord(AccessibilityEvent event, Object record) {

        }

        @Override
        public Object getRecord(AccessibilityEvent event, int index) {
            return null;
        }

        @Override
        public void setContentChangeTypes(AccessibilityEvent event, int types) {

        }

        @Override
        public int getRecordCount(AccessibilityEvent event) {
            return 0;
        }

        @Override
        public int getContentChangeTypes(AccessibilityEvent event) {
            return 0;
        }
    }

    static class AccessibilityEventIcsImpl extends AccessibilityEventStubImpl {

        @Override
        public void appendRecord(AccessibilityEvent event, Object record) {
            AccessibilityEventCompatIcs.appendRecord(event, record);
        }

        @Override
        public Object getRecord(AccessibilityEvent event, int index) {
            return AccessibilityEventCompatIcs.getRecord(event, index);
        }

        @Override
        public int getRecordCount(AccessibilityEvent event) {
            return AccessibilityEventCompatIcs.getRecordCount(event);
        }
    }

    static class AccessibilityEventKitKatImpl extends AccessibilityEventIcsImpl {

        @Override
        public void setContentChangeTypes(AccessibilityEvent event, int types) {
            AccessibilityEventCompatKitKat.setContentChangeTypes(event, types);
        }

        @Override
        public int getContentChangeTypes(AccessibilityEvent event) {
            return AccessibilityEventCompatKitKat.getContentChangeTypes(event);
        }
    }

    private final static AccessibilityEventVersionImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 19) { // KitKat
            IMPL = new AccessibilityEventKitKatImpl();
        } else if (Build.VERSION.SDK_INT >= 14) { // ICS
            IMPL = new AccessibilityEventIcsImpl();
        } else {
            IMPL = new AccessibilityEventStubImpl();
        }
    }

    /**
     * Represents the event of a hover enter over a {@link android.view.View}.
     */
    public static final int TYPE_VIEW_HOVER_ENTER = 0x00000080;

    /**
     * Represents the event of a hover exit over a {@link android.view.View}.
     */
    public static final int TYPE_VIEW_HOVER_EXIT = 0x00000100;

    /**
     * Represents the event of starting a touch exploration gesture.
     */
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START = 0x00000200;

    /**
     * Represents the event of ending a touch exploration gesture.
     */
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END = 0x00000400;

    /**
     * Represents the event of changing the content of a window.
     */
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 0x00000800;

    /**
     * Represents the event of scrolling a view.
     */
    public static final int TYPE_VIEW_SCROLLED = 0x00001000;

    /**
     * Represents the event of changing the selection in an {@link android.widget.EditText}.
     */
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 0x00002000;

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
     * @see #TYPE_VIEW_HOVER_ENTER
     * @see #TYPE_VIEW_HOVER_EXIT
     * @see #TYPE_TOUCH_EXPLORATION_GESTURE_START
     * @see #TYPE_TOUCH_EXPLORATION_GESTURE_END
     * @see #TYPE_WINDOW_CONTENT_CHANGED
     * @see #TYPE_VIEW_SCROLLED
     * @see #TYPE_VIEW_TEXT_SELECTION_CHANGED
     * @see #TYPE_ANNOUNCEMENT
     * @see #TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
     * @see #TYPE_GESTURE_DETECTION_START
     * @see #TYPE_GESTURE_DETECTION_END
     * @see #TYPE_TOUCH_INTERACTION_START
     * @see #TYPE_TOUCH_INTERACTION_END
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
     */
    public static int getRecordCount(AccessibilityEvent event) {
        return IMPL.getRecordCount(event);
    }

    /**
     * Appends an {@link android.view.accessibility.AccessibilityRecord} to the end of
     * event records.
     *
     * @param record The record to append.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     */
    public static void appendRecord(AccessibilityEvent event, AccessibilityRecordCompat record) {
        IMPL.appendRecord(event, record.getImpl());
    }

    /**
     * Gets the record at a given index.
     *
     * @param index The index.
     * @return The record at the specified index.
     */
    public static AccessibilityRecordCompat getRecord(AccessibilityEvent event, int index) {
        return new AccessibilityRecordCompat(IMPL.getRecord(event, index));
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
     */
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
        IMPL.setContentChangeTypes(event, changeTypes);
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
        return IMPL.getContentChangeTypes(event);
    }

}
