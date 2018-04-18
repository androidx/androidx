/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.view;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.core.R;
import androidx.core.view.ViewCompat.ScrollAxis;

/**
 * Helper for accessing features in {@link ViewGroup}.
 */
public final class ViewGroupCompat {

    /**
     * This constant is a {@link #setLayoutMode(ViewGroup, int) layoutMode}.
     * Clip bounds are the raw values of {@link android.view.View#getLeft() left},
     * {@link android.view.View#getTop() top},
     * {@link android.view.View#getRight() right} and {@link android.view.View#getBottom() bottom}.
     */
    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0;

    /**
     * This constant is a {@link #setLayoutMode(ViewGroup, int) layoutMode}.
     * Optical bounds describe where a widget appears to be. They sit inside the clip
     * bounds which need to cover a larger area to allow other effects,
     * such as shadows and glows, to be drawn.
     */
    public static final int LAYOUT_MODE_OPTICAL_BOUNDS = 1;

    /*
     * Hide the constructor.
     */
    private ViewGroupCompat() {}

    /**
     * Called when a child has requested sending an {@link AccessibilityEvent} and
     * gives an opportunity to its parent to augment the event.
     * <p>
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link ViewCompat#setAccessibilityDelegate(View, AccessibilityDelegateCompat)} its
     * {@link AccessibilityDelegateCompat#onRequestSendAccessibilityEvent(ViewGroup, View,
     * AccessibilityEvent)} is responsible for handling this call.
     * </p>
     *
     * @param group The group whose method to invoke.
     * @param child The child which requests sending the event.
     * @param event The event to be sent.
     * @return True if the event should be sent.
     *
     * @deprecated Use {@link ViewGroup#onRequestSendAccessibilityEvent(View, AccessibilityEvent)}
     * directly.
     */
    @Deprecated
    public static boolean onRequestSendAccessibilityEvent(ViewGroup group, View child,
            AccessibilityEvent event) {
        return group.onRequestSendAccessibilityEvent(child, event);
    }

    /**
     * Enable or disable the splitting of MotionEvents to multiple children during touch event
     * dispatch. This behavior is enabled by default for applications that target an
     * SDK version of 11 (Honeycomb) or newer. On earlier platform versions this feature
     * was not supported and this method is a no-op.
     *
     * <p>When this option is enabled MotionEvents may be split and dispatched to different child
     * views depending on where each pointer initially went down. This allows for user interactions
     * such as scrolling two panes of content independently, chording of buttons, and performing
     * independent gestures on different pieces of content.
     *
     * @param group ViewGroup to modify
     * @param split <code>true</code> to allow MotionEvents to be split and dispatched to multiple
     *              child views. <code>false</code> to only allow one child view to be the target of
     *              any MotionEvent received by this ViewGroup.
     *
     * @deprecated Use {@link ViewGroup#setMotionEventSplittingEnabled(boolean)} directly.
     */
    @Deprecated
    public static void setMotionEventSplittingEnabled(ViewGroup group, boolean split) {
        group.setMotionEventSplittingEnabled(split);
    }

    /**
     * Returns the basis of alignment during layout operations on this ViewGroup:
     * either {@link #LAYOUT_MODE_CLIP_BOUNDS} or {@link #LAYOUT_MODE_OPTICAL_BOUNDS}.
     * <p>
     * If no layoutMode was explicitly set, either programmatically or in an XML resource,
     * the method returns the layoutMode of the view's parent ViewGroup if such a parent exists,
     * otherwise the method returns a default value of {@link #LAYOUT_MODE_CLIP_BOUNDS}.
     *
     * @return the layout mode to use during layout operations
     *
     * @see #setLayoutMode(ViewGroup, int)
     */
    public static int getLayoutMode(@NonNull ViewGroup group) {
        if (Build.VERSION.SDK_INT >= 18) {
            return group.getLayoutMode();
        }
        return LAYOUT_MODE_CLIP_BOUNDS;
    }

    /**
     * Sets the basis of alignment during the layout of this ViewGroup.
     * Valid values are either {@link #LAYOUT_MODE_CLIP_BOUNDS} or
     * {@link #LAYOUT_MODE_OPTICAL_BOUNDS}.
     *
     * @param mode the layout mode to use during layout operations
     *
     * @see #getLayoutMode(ViewGroup)
     */
    public static void setLayoutMode(@NonNull ViewGroup group, int mode) {
        if (Build.VERSION.SDK_INT >= 18) {
            group.setLayoutMode(mode);
        }
    }

    /**
     * Changes whether or not this ViewGroup should be treated as a single entity during
     * Activity Transitions.
     * @param isTransitionGroup Whether or not the ViewGroup should be treated as a unit
     *                          in Activity transitions. If false, the ViewGroup won't transition,
     *                          only its children. If true, the entire ViewGroup will transition
     *                          together.
     */
    public static void setTransitionGroup(@NonNull ViewGroup group, boolean isTransitionGroup) {
        if (Build.VERSION.SDK_INT >= 21) {
            group.setTransitionGroup(isTransitionGroup);
        } else {
            group.setTag(R.id.tag_transition_group, isTransitionGroup);
        }
    }

    /**
     * Returns true if this ViewGroup should be considered as a single entity for removal
     * when executing an Activity transition. If this is false, child elements will move
     * individually during the transition.
     */
    public static boolean isTransitionGroup(@NonNull ViewGroup group) {
        if (Build.VERSION.SDK_INT >= 21) {
            return group.isTransitionGroup();
        }
        Boolean explicit = (Boolean) group.getTag(R.id.tag_transition_group);
        return (explicit != null && explicit)
                || group.getBackground() != null
                || ViewCompat.getTransitionName(group) != null;
    }

    /**
     * Return the current axes of nested scrolling for this ViewGroup.
     *
     * <p>A ViewGroup returning something other than {@link ViewCompat#SCROLL_AXIS_NONE} is
     * currently acting as a nested scrolling parent for one or more descendant views in
     * the hierarchy.</p>
     *
     * @return Flags indicating the current axes of nested scrolling
     * @see ViewCompat#SCROLL_AXIS_HORIZONTAL
     * @see ViewCompat#SCROLL_AXIS_VERTICAL
     * @see ViewCompat#SCROLL_AXIS_NONE
     */
    @ScrollAxis
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int getNestedScrollAxes(@NonNull ViewGroup group) {
        if (Build.VERSION.SDK_INT >= 21) {
            return group.getNestedScrollAxes();
        }
        if (group instanceof NestedScrollingParent) {
            return ((NestedScrollingParent) group).getNestedScrollAxes();
        }
        return ViewCompat.SCROLL_AXIS_NONE;
    }
}
