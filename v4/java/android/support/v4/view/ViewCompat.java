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

package android.support.v4.view;

import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

/**
 * Helper for accessing features in {@link View} introduced after API
 * level 4 in a backwards compatible fashion.
 */
public class ViewCompat {
    /**
     * Always allow a user to over-scroll this view, provided it is a
     * view that can scroll.
     */
    public static final int OVER_SCROLL_ALWAYS = 0;

    /**
     * Allow a user to over-scroll this view only if the content is large
     * enough to meaningfully scroll, provided it is a view that can scroll.
     */
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

    /**
     * Never allow a user to over-scroll this view.
     */
    public static final int OVER_SCROLL_NEVER = 2;

    private static final long FAKE_FRAME_TIME = 10;

    /**
     * Automatically determine whether a view is important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;

    /**
     * The view is important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;

    /**
     * The view is not important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;

    interface ViewCompatImpl {
        public boolean canScrollHorizontally(View v, int direction);
        public boolean canScrollVertically(View v, int direction);
        public int getOverScrollMode(View v);
        public void setOverScrollMode(View v, int mode);
        public void onInitializeAccessibilityEvent(View v, AccessibilityEvent event);
        public void onPopulateAccessibilityEvent(View v, AccessibilityEvent event);
        public void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info);
        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate);
        public boolean hasTransientState(View view);
        public void setHasTransientState(View view, boolean hasTransientState);
        public void postInvalidateOnAnimation(View view);
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom);
        public void postOnAnimation(View view, Runnable action);
        public void postOnAnimationDelayed(View view, Runnable action, long delayMillis);
        public int getImportantForAccessibility(View view);
        public void setImportantForAccessibility(View view, int mode);
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view);
    }

    static class BaseViewCompatImpl implements ViewCompatImpl {
        public boolean canScrollHorizontally(View v, int direction) {
            return false;
        }
        public boolean canScrollVertically(View v, int direction) {
            return false;
        }
        public int getOverScrollMode(View v) {
            return OVER_SCROLL_NEVER;
        }
        public void setOverScrollMode(View v, int mode) {
            // Do nothing; API doesn't exist
        }
        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
            // Do nothing; API doesn't exist
        }
        public void onPopulateAccessibilityEvent(View v, AccessibilityEvent event) {
            // Do nothing; API doesn't exist
        }
        public void onInitializeAccessibilityEvent(View v, AccessibilityEvent event) {
         // Do nothing; API doesn't exist
        }
        public void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info) {
            // Do nothing; API doesn't exist
        }
        public boolean hasTransientState(View view) {
            // A view can't have transient state if transient state wasn't supported.
            return false;
        }
        public void setHasTransientState(View view, boolean hasTransientState) {
            // Do nothing; API doesn't exist
        }
        public void postInvalidateOnAnimation(View view) {
            view.postInvalidateDelayed(getFrameTime());
        }
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            view.postInvalidateDelayed(getFrameTime(), left, top, right, bottom);
        }
        public void postOnAnimation(View view, Runnable action) {
            view.postDelayed(action, getFrameTime());
        }
        public void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
            view.postDelayed(action, getFrameTime() + delayMillis);
        }
        long getFrameTime() {
            return FAKE_FRAME_TIME;
        }
        public int getImportantForAccessibility(View view) {
            return 0;
        }
        public void setImportantForAccessibility(View view, int mode) {

        }
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
            return null;
        }
    }

    static class GBViewCompatImpl extends BaseViewCompatImpl {
        @Override
        public int getOverScrollMode(View v) {
            return ViewCompatGingerbread.getOverScrollMode(v);
        }
        @Override
        public void setOverScrollMode(View v, int mode) {
            ViewCompatGingerbread.setOverScrollMode(v, mode);
        }
    }

    static class HCViewCompatImpl extends GBViewCompatImpl {
        long getFrameTime() {
            return ViewCompatHC.getFrameTime();
        }
    }

    static class ICSViewCompatImpl extends HCViewCompatImpl {
        @Override
        public boolean canScrollHorizontally(View v, int direction) {
            return ViewCompatICS.canScrollHorizontally(v, direction);
        }
        @Override
        public boolean canScrollVertically(View v, int direction) {
            return ViewCompatICS.canScrollVertically(v, direction);
        }
        @Override
        public void onPopulateAccessibilityEvent(View v, AccessibilityEvent event) {
            ViewCompatICS.onPopulateAccessibilityEvent(v, event);
        }
        @Override
        public void onInitializeAccessibilityEvent(View v, AccessibilityEvent event) {
            ViewCompatICS.onInitializeAccessibilityEvent(v, event);
        }
        @Override
        public void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info) {
            ViewCompatICS.onInitializeAccessibilityNodeInfo(v, info.getInfo());
        }
        @Override
        public void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
            ViewCompatICS.setAccessibilityDelegate(v, delegate.getBridge());
        }
    }

    static class JBViewCompatImpl extends ICSViewCompatImpl {
        @Override
        public boolean hasTransientState(View view) {
            return ViewCompatJB.hasTransientState(view);
        }
        @Override
        public void setHasTransientState(View view, boolean hasTransientState) {
            ViewCompatJB.setHasTransientState(view, hasTransientState);
        }
        @Override
        public void postInvalidateOnAnimation(View view) {
            ViewCompatJB.postInvalidateOnAnimation(view);
        }
        @Override
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            ViewCompatJB.postInvalidateOnAnimation(view, left, top, right, bottom);
        }
        @Override
        public void postOnAnimation(View view, Runnable action) {
            ViewCompatJB.postOnAnimation(view, action);
        }
        @Override
        public void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
            ViewCompatJB.postOnAnimationDelayed(view, action, delayMillis);
        }
        @Override
        public int getImportantForAccessibility(View view) {
            return ViewCompatJB.getImportantForAccessibility(view);
        }
        @Override
        public void setImportantForAccessibility(View view, int mode) {
            ViewCompatJB.setImportantForAccessibility(view, mode);
        }
        @Override
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
            Object compat = ViewCompatJB.getAccessibilityNodeProvider(view);
            if (compat != null) {
                return new AccessibilityNodeProviderCompat(compat);
            }
            return null;
        }
    }

    static final ViewCompatImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 16 || android.os.Build.VERSION.CODENAME.equals("JellyBean")) {
            IMPL = new JBViewCompatImpl();
        } else if (version >= 14) {
            IMPL = new ICSViewCompatImpl();
        } else if (version >= 11) {
            IMPL = new HCViewCompatImpl();
        } else if (version >= 9) {
            IMPL = new GBViewCompatImpl();
        } else {
            IMPL = new BaseViewCompatImpl();
        }
    }

    /**
     * Check if this view can be scrolled horizontally in a certain direction.
     *
     * @param v The View against which to invoke the method.
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public static boolean canScrollHorizontally(View v, int direction) {
        return IMPL.canScrollHorizontally(v, direction);
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param v The View against which to invoke the method.
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public static boolean canScrollVertically(View v, int direction) {
        return IMPL.canScrollVertically(v, direction);
    }

    /**
     * Returns the over-scroll mode for this view. The result will be
     * one of {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * @param v The View against which to invoke the method.
     * @return This view's over-scroll mode.
     */
    public static int getOverScrollMode(View v) {
        return IMPL.getOverScrollMode(v);
    }

    /**
     * Set the over-scroll mode for this view. Valid over-scroll modes are
     * {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * Setting the over-scroll mode of a view will have an effect only if the
     * view is capable of scrolling.
     *
     * @param v The View against which to invoke the method.
     * @param overScrollMode The new over-scroll mode for this view.
     */
    public static void setOverScrollMode(View v, int overScrollMode) {
        IMPL.setOverScrollMode(v, overScrollMode);
    }

    /**
     * Called from {@link View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)}
     * giving a chance to this View to populate the accessibility event with its
     * text content. While this method is free to modify event
     * attributes other than text content, doing so should normally be performed in
     * {@link View#onInitializeAccessibilityEvent(AccessibilityEvent)}.
     * <p>
     * Example: Adding formatted date string to an accessibility event in addition
     *          to the text added by the super implementation:
     * <pre> public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
     *     super.onPopulateAccessibilityEvent(event);
     *     final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY;
     *     String selectedDateUtterance = DateUtils.formatDateTime(mContext,
     *         mCurrentDate.getTimeInMillis(), flags);
     *     event.getText().add(selectedDateUtterance);
     * }</pre>
     * <p>
     * If an {@link android.view.View.AccessibilityDelegate} has been specified via calling
     * {@link View#setAccessibilityDelegate(android.view.View.AccessibilityDelegate)} its
     * {@link android.view.View.AccessibilityDelegate#onPopulateAccessibilityEvent(View,
     *  AccessibilityEvent)}
     * is responsible for handling this call.
     * </p>
     * <p class="note"><strong>Note:</strong> Always call the super implementation before adding
     * information to the event, in case the default implementation has basic information to add.
     * </p>
     *
     * @param v The View against which to invoke the method.
     * @param event The accessibility event which to populate.
     *
     * @see View#sendAccessibilityEvent(int)
     * @see View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     */
    public static void onPopulateAccessibilityEvent(View v, AccessibilityEvent event) {
        IMPL.onPopulateAccessibilityEvent(v, event);
    }

    /**
     * Initializes an {@link AccessibilityEvent} with information about
     * this View which is the event source. In other words, the source of
     * an accessibility event is the view whose state change triggered firing
     * the event.
     * <p>
     * Example: Setting the password property of an event in addition
     *          to properties set by the super implementation:
     * <pre> public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
     *     super.onInitializeAccessibilityEvent(event);
     *     event.setPassword(true);
     * }</pre>
     * <p>
     * If an {@link android.view.View.AccessibilityDelegate} has been specified via calling
     * {@link View#setAccessibilityDelegate(android.view.View.AccessibilityDelegate)} its
     * {@link android.view.View.AccessibilityDelegate#onInitializeAccessibilityEvent(View,
     *  AccessibilityEvent)}
     * is responsible for handling this call.
     * </p>
     * <p class="note"><strong>Note:</strong> Always call the super implementation before adding
     * information to the event, in case the default implementation has basic information to add.
     * </p>
     *
     * @param v The View against which to invoke the method.
     * @param event The event to initialize.
     *
     * @see View#sendAccessibilityEvent(int)
     * @see View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     */
    public static void onInitializeAccessibilityEvent(View v, AccessibilityEvent event) {
        IMPL.onInitializeAccessibilityEvent(v, event);
    }

    /**
     * Initializes an {@link android.view.accessibility.AccessibilityNodeInfo} with information
     * about this view. The base implementation sets:
     * <ul>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setParent(View)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setBoundsInParent(Rect)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setBoundsInScreen(Rect)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setPackageName(CharSequence)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setClassName(CharSequence)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setContentDescription(CharSequence)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setEnabled(boolean)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setClickable(boolean)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setFocusable(boolean)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setFocused(boolean)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setLongClickable(boolean)},</li>
     * <li>{@link android.view.accessibility.AccessibilityNodeInfo#setSelected(boolean)},</li>
     * </ul>
     * <p>
     * Subclasses should override this method, call the super implementation,
     * and set additional attributes.
     * </p>
     * <p>
     * If an {@link android.view.View.AccessibilityDelegate} has been specified via calling
     * {@link View#setAccessibilityDelegate(android.view.View.AccessibilityDelegate)} its
     * {@link android.view.View.AccessibilityDelegate#onInitializeAccessibilityNodeInfo(View,
     *  android.view.accessibility.AccessibilityNodeInfo)}
     * is responsible for handling this call.
     * </p>
     *
     * @param v The View against which to invoke the method.
     * @param info The instance to initialize.
     */
    public static void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info) {
        IMPL.onInitializeAccessibilityNodeInfo(v, info);
    }

    /**
     * Sets a delegate for implementing accessibility support via compositon as
     * opposed to inheritance. The delegate's primary use is for implementing
     * backwards compatible widgets. For more details see
     * {@link android.view.View.AccessibilityDelegate}.
     *
     * @param v The View against which to invoke the method.
     * @param delegate The delegate instance.
     *
     * @see android.view.View.AccessibilityDelegate
     */
    public static void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        IMPL.setAccessibilityDelegate(v, delegate);
    }

    /**
     * Indicates whether the view is currently tracking transient state that the
     * app should not need to concern itself with saving and restoring, but that
     * the framework should take special note to preserve when possible.
     *
     * @param view View to check for transient state
     * @return true if the view has transient state
     */
    public static boolean hasTransientState(View view) {
        return IMPL.hasTransientState(view);
    }

    /**
     * Set whether this view is currently tracking transient state that the
     * framework should attempt to preserve when possible.
     *
     * @param view View tracking transient state
     * @param hasTransientState true if this view has transient state
     */
    public static void setHasTransientState(View view, boolean hasTransientState) {
        IMPL.setHasTransientState(view, hasTransientState);
    }

    /**
     * <p>Cause an invalidate to happen on the next animation time step, typically the
     * next display frame.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view View to invalidate
     */
    public static void postInvalidateOnAnimation(View view) {
        IMPL.postInvalidateOnAnimation(view);
    }

    /**
     * <p>Cause an invalidate of the specified area to happen on the next animation
     * time step, typically the next display frame.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view View to invalidate
     * @param left The left coordinate of the rectangle to invalidate.
     * @param top The top coordinate of the rectangle to invalidate.
     * @param right The right coordinate of the rectangle to invalidate.
     * @param bottom The bottom coordinate of the rectangle to invalidate.
     */
    public static void postInvalidateOnAnimation(View view, int left, int top,
            int right, int bottom) {
        IMPL.postInvalidateOnAnimation(view, left, top, right, bottom);
    }

    /**
     * <p>Causes the Runnable to execute on the next animation time step.
     * The runnable will be run on the user interface thread.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view View to post this Runnable to
     * @param action The Runnable that will be executed.
     */
    public static void postOnAnimation(View view, Runnable action) {
        IMPL.postOnAnimation(view, action);
    }

    /**
     * <p>Causes the Runnable to execute on the next animation time step,
     * after the specified amount of time elapses.
     * The runnable will be run on the user interface thread.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view The view to post this Runnable to
     * @param action The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     */
    public static void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
        IMPL.postOnAnimationDelayed(view, action, delayMillis);
    }

    /**
     * Gets the mode for determining whether this View is important for accessibility
     * which is if it fires accessibility events and if it is reported to
     * accessibility services that query the screen.
     *
     * @param view The view whose property to get.
     * @return The mode for determining whether a View is important for accessibility.
     *
     * @see #IMPORTANT_FOR_ACCESSIBILITY_YES
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO
     * @see #IMPORTANT_FOR_ACCESSIBILITY_AUTO
     */
    public static int getImportantForAccessibility(View view) {
        return IMPL.getImportantForAccessibility(view);
    }

    /**
     * Sets how to determine whether this view is important for accessibility
     * which is if it fires accessibility events and if it is reported to
     * accessibility services that query the screen.
     *
     * @param view The view whose property to set.
     * @param mode How to determine whether this view is important for accessibility.
     *
     * @see #IMPORTANT_FOR_ACCESSIBILITY_YES
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO
     * @see #IMPORTANT_FOR_ACCESSIBILITY_AUTO
     */
    public static void setImportantForAccessibility(View view, int mode) {
        IMPL.setImportantForAccessibility(view, mode);
    }

    /**
     * Gets the provider for managing a virtual view hierarchy rooted at this View
     * and reported to {@link android.accessibilityservice.AccessibilityService}s
     * that explore the window content.
     * <p>
     * If this method returns an instance, this instance is responsible for managing
     * {@link AccessibilityNodeInfoCompat}s describing the virtual sub-tree rooted at
     * this View including the one representing the View itself. Similarly the returned
     * instance is responsible for performing accessibility actions on any virtual
     * view or the root view itself.
     * </p>
     * <p>
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link #setAccessibilityDelegate(View, AccessibilityDelegateCompat) its
     * {@link AccessibilityDelegateCompat#getAccessibilityNodeProvider(View)}
     * is responsible for handling this call.
     * </p>
     *
     * @param view The view whose property to get.
     * @return The provider.
     *
     * @see AccessibilityNodeProviderCompat
     */
    public static AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
        return IMPL.getAccessibilityNodeProvider(view);
    }
}
