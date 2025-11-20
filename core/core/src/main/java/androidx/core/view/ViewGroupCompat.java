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
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.R;
import androidx.core.view.ViewCompat.ScrollAxis;

import org.jspecify.annotations.NonNull;

/**
 * Helper for accessing features in {@link ViewGroup}.
 */
public final class ViewGroupCompat {

    /**
     * This constant is a {@link #setLayoutMode(ViewGroup, int) layoutMode}.
     * Clip bounds are the raw values of {@link View#getLeft() left},
     * {@link View#getTop() top},
     * {@link View#getRight() right} and {@link View#getBottom() bottom}.
     */
    public static final int LAYOUT_MODE_CLIP_BOUNDS = 0;

    /**
     * This constant is a {@link #setLayoutMode(ViewGroup, int) layoutMode}.
     * Optical bounds describe where a widget appears to be. They sit inside the clip
     * bounds which need to cover a larger area to allow other effects,
     * such as shadows and glows, to be drawn.
     */
    public static final int LAYOUT_MODE_OPTICAL_BOUNDS = 1;

    private static final WindowInsets CONSUMED = WindowInsetsCompat.CONSUMED.toWindowInsets();

    static boolean sCompatInsetsDispatchInstalled = false;

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
    @androidx.annotation.ReplaceWith(expression = "group.onRequestSendAccessibilityEvent(child, event)")
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
    @androidx.annotation.ReplaceWith(expression = "group.setMotionEventSplittingEnabled(split)")
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
     * @deprecated Call {@link ViewGroup#getLayoutMode()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "group.getLayoutMode()")
    public static int getLayoutMode(@NonNull ViewGroup group) {
        return group.getLayoutMode();
    }

    /**
     * Sets the basis of alignment during the layout of this ViewGroup.
     * Valid values are either {@link #LAYOUT_MODE_CLIP_BOUNDS} or
     * {@link #LAYOUT_MODE_OPTICAL_BOUNDS}.
     *
     * @param group ViewGroup for which to set the mode.
     * @param mode the layout mode to use during layout operations
     *
     * @see #getLayoutMode(ViewGroup)
     * @deprecated Call {@link ViewGroup#setLayoutMode()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "group.setLayoutMode(mode)")
    public static void setLayoutMode(@NonNull ViewGroup group, int mode) {
        group.setLayoutMode(mode);
    }

    /**
     * Changes whether or not this ViewGroup should be treated as a single entity during
     * Activity Transitions.
     * @param group ViewGroup for which to set the mode.
     * @param isTransitionGroup Whether or not the ViewGroup should be treated as a unit
     *                          in Activity transitions. If false, the ViewGroup won't transition,
     *                          only its children. If true, the entire ViewGroup will transition
     *                          together.
     */
    public static void setTransitionGroup(@NonNull ViewGroup group, boolean isTransitionGroup) {
        group.setTransitionGroup(isTransitionGroup);
    }

    /**
     * Returns true if this ViewGroup should be considered as a single entity for removal
     * when executing an Activity transition. If this is false, child elements will move
     * individually during the transition.
     */
    public static boolean isTransitionGroup(@NonNull ViewGroup group) {
        return group.isTransitionGroup();
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
        return group.getNestedScrollAxes();
    }

    /**
     * Installs a custom {@link View.OnApplyWindowInsetsListener} which dispatches WindowInsets to
     * the given root and its descendants in a way compatible with Android R+ that consuming or
     * modifying insets will only affect the descendants.
     * <P>
     * Note: When using this method, ensure that ViewCompat.setOnApplyWindowInsetsListener() is used
     * instead of the platform call.
     *
     * @param root The root view that the custom listener is installed on. Note: the listener will
     *             consume the insets, so make sure the given root is the root view of the window,
     *             or its siblings might not be able to get insets dispatched.
     */
    public static void installCompatInsetsDispatch(@NonNull View root) {
        if (Build.VERSION.SDK_INT >= 30) {
            return;
        }
        final View.OnApplyWindowInsetsListener listener = (view, windowInsets) -> {
            dispatchApplyWindowInsets(view, windowInsets);

            // The insets have been dispatched to descendants of the given view. Here returns the
            // consumed insets to prevent redundant dispatching by the framework.
            return CONSUMED;
        };
        root.setTag(R.id.tag_compat_insets_dispatch, listener);
        root.setOnApplyWindowInsetsListener(listener);
        sCompatInsetsDispatchInstalled = true;
    }

    @NonNull
    static WindowInsets dispatchApplyWindowInsets(View view, WindowInsets windowInsets) {
        final Object wrappedUserListener = view.getTag(R.id.tag_on_apply_window_listener);
        final Object animCallback = view.getTag(R.id.tag_window_insets_animation_callback);
        final View.OnApplyWindowInsetsListener listener =
                (wrappedUserListener instanceof View.OnApplyWindowInsetsListener)
                        ? (View.OnApplyWindowInsetsListener) wrappedUserListener
                        : (animCallback instanceof View.OnApplyWindowInsetsListener)
                                ? (View.OnApplyWindowInsetsListener) animCallback
                                : null;

        // Don't call View#onApplyWindowInsets directly, but via View#dispatchApplyWindowInsets.
        // Otherwise, the view won't get PFLAG3_APPLYING_INSETS and it will dispatch insets on its
        // own.
        final WindowInsets[] outInsets = {CONSUMED};
        view.setOnApplyWindowInsetsListener((v, w) -> {
            outInsets[0] = listener != null
                    ? listener.onApplyWindowInsets(v, w)
                    : v.onApplyWindowInsets(w);

            // Only apply window insets to this view.
            return CONSUMED;
        });
        // If our OnApplyWindowInsetsListener doesn't get called, it means the view has its own
        // dispatching logic, the outInsets will remain CONSUMED, and we don't have to dispatch
        // insets to its child views.
        view.dispatchApplyWindowInsets(windowInsets);

        // Restore the listener.
        final Object compatInsetsDispatch = view.getTag(R.id.tag_compat_insets_dispatch);
        view.setOnApplyWindowInsetsListener(
                compatInsetsDispatch instanceof View.OnApplyWindowInsetsListener
                        ? (View.OnApplyWindowInsetsListener) compatInsetsDispatch
                        : listener);

        if (outInsets[0] != null && !outInsets[0].isConsumed() && view instanceof ViewGroup) {
            final ViewGroup parent = (ViewGroup) view;
            final int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                dispatchApplyWindowInsets(parent.getChildAt(i), outInsets[0]);
            }
        }
        return outInsets[0] != null ? outInsets[0] : CONSUMED;
    }
}
