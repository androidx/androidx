/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Checkable;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.test.uiautomator.util.Traces;
import androidx.test.uiautomator.util.Traces.Section;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a UI element, and exposes methods for performing gestures (clicks, swipes) or
 * searching through its children.
 *
 * <p>Unlike {@link UiObject}, {@link UiObject2} is bound to a particular view instance and can
 * become stale if the underlying view object is destroyed. As a result, it may be necessary
 * to call {@link UiDevice#findObject(BySelector)} to obtain a new {@link UiObject2} instance if the
 * UI changes significantly.
 */
public class UiObject2 implements Searchable {

    private static final String TAG = UiObject2.class.getSimpleName();

    // default percentage of margins for gestures.
    private static final float DEFAULT_GESTURE_MARGIN_PERCENT = 0.1f;

    // default percentage of each scroll in scrollUntil().
    private static final float DEFAULT_SCROLL_UNTIL_PERCENT = 0.8f;

    // Default gesture speeds and timeouts.
    private static final int DEFAULT_SWIPE_SPEED = 5_000; // dp/s
    private static final int DEFAULT_SCROLL_SPEED = 1_500; // dp/s
    private static final int DEFAULT_FLING_SPEED = 7_500; // dp/s
    private static final int DEFAULT_DRAG_SPEED = 2_500; // dp/s
    private static final int DEFAULT_PINCH_SPEED = 1_000; // dp/s
    // Retry if scrollFinished has null result
    private static final int MAX_NULL_SCROLL_RETRY = 2;
    private static final long SCROLL_TIMEOUT = 1_000; // ms
    private static final long FLING_TIMEOUT = 5_000; // ms; longer as motion may continue.

    private final UiDevice mDevice;
    private final BySelector mSelector;
    private final GestureController mGestureController;
    private final WaitMixin<UiObject2> mWaitMixin = new WaitMixin<>(this);
    private final int mDisplayId;
    private final float mDisplayDensity;
    private AccessibilityNodeInfo mCachedNode;
    private Margins mMargins = new PercentMargins(DEFAULT_GESTURE_MARGIN_PERCENT,
            DEFAULT_GESTURE_MARGIN_PERCENT,
            DEFAULT_GESTURE_MARGIN_PERCENT,
            DEFAULT_GESTURE_MARGIN_PERCENT);

    private UiObject2(UiDevice device, BySelector selector, AccessibilityNodeInfo cachedNode) {
        mDevice = device;
        mSelector = selector;
        mCachedNode = cachedNode;
        mGestureController = GestureController.getInstance(device);

        // Fetch and cache display information. This is safe as moving the underlying view to
        // another display would invalidate the cached node and require recreating this UiObject2.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AccessibilityWindowInfo window = cachedNode.getWindow();
            mDisplayId = window == null ? Display.DEFAULT_DISPLAY : Api30Impl.getDisplayId(window);
        } else {
            mDisplayId = Display.DEFAULT_DISPLAY;
        }
        Context uiContext = device.getUiContext(mDisplayId);
        int densityDpi = uiContext.getResources().getConfiguration().densityDpi;
        mDisplayDensity = (float) densityDpi / DisplayMetrics.DENSITY_DEFAULT;
    }

    @Nullable
    static UiObject2 create(@NonNull UiDevice device, @NonNull BySelector selector,
            @NonNull AccessibilityNodeInfo cachedNode) {
        try {
            return new UiObject2(device, selector, cachedNode);
        } catch (RuntimeException e) {
            Log.w(TAG, String.format("Failed to create UiObject2 for node %s.", cachedNode), e);
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UiObject2)) {
            return false;
        }
        try {
            UiObject2 other = (UiObject2) object;
            return getAccessibilityNodeInfo().equals(other.getAccessibilityNodeInfo());
        } catch (StaleObjectException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            return getAccessibilityNodeInfo().hashCode();
        } catch (StaleObjectException e) {
            return mCachedNode.hashCode();
        }
    }

    /** Recycle this object. */
    public void recycle() {
        mCachedNode.recycle();
        mCachedNode = null;
    }

    // Settings

    /**
     * Sets the percentage of gestures' margins to avoid touching too close to the edges, e.g.
     * when scrolling up, phone open quick settings instead if gesture is close to the top.
     * The percentage is based on the object's visible size, e.g. to set 20% margins:
     * <pre>mUiObject2.setGestureMarginPercentage(0.2f);</pre>
     *
     * @Param percent Float between [0, 0.5] for four margins: left, top, right, and bottom.
     */
    public void setGestureMarginPercentage(@FloatRange(from = 0f, to = 0.5f) float percent) {
        setGestureMarginsPercentage(percent, percent, percent, percent);
    }

    /**
     * Sets the percentage of gestures' margins to avoid touching too close to the edges, e.g.
     * when scrolling up, phone open quick settings instead if gesture is close to the top.
     * The percentage is based on the object's visible size, e.g. to set 20% bottom margin only:
     * <pre>mUiObject2.setGestureMarginsPercentage(0f, 0f, 0f, 0.2f);</pre>
     *
     * @Param left Float between [0, 1] for left margin
     * @Param top Float between [0, 1] for top margin
     * @Param right Float between [0, 1] for right margin
     * @Param bottom Float between [0, 1] for bottom margin
     */
    public void setGestureMarginsPercentage(@FloatRange(from = 0f, to = 1f) float left,
            @FloatRange(from = 0f, to = 1f) float top,
            @FloatRange(from = 0f, to = 1f) float right,
            @FloatRange(from = 0f, to = 1f) float bottom) {
        mMargins = new PercentMargins(left, top, right, bottom);
    }

    /** Sets the margins used for gestures in pixels. */
    public void setGestureMargin(int margin) {
        setGestureMargins(margin, margin, margin, margin);
    }

    /** Sets the margins used for gestures in pixels. */
    public void setGestureMargins(int left, int top, int right, int bottom) {
        mMargins = new SimpleMargins(left, top, right, bottom);
    }

    // Wait functions

    /**
     * Waits for a {@code condition} to be met.
     *
     * @param condition The {@link UiObject2Condition} to wait for.
     * @param timeout   The maximum time in milliseconds to wait for.
     * @return The final result returned by the {@code condition}, or {@code null} if the {@code
     * condition} was not met before the {@code timeout}.
     */
    public <U> U wait(@NonNull UiObject2Condition<U> condition, long timeout) {
        return wait((Condition<? super UiObject2, U>) condition, timeout);
    }

    /**
     * Waits for a {@code condition} to be met.
     *
     * @param condition The {@link SearchCondition} to evaluate.
     * @param timeout   The maximum time in milliseconds to wait for.
     * @return The final result returned by the {@code condition}, or {@code null} if the {@code
     * condition} was not met before the {@code timeout}.
     */
    public <U> U wait(@NonNull SearchCondition<U> condition, long timeout) {
        return wait((Condition<? super UiObject2, U>) condition, timeout);
    }


    /**
     * Waits for a {@code condition} to be met.
     *
     * @param condition The {@link Condition} to evaluate.
     * @param timeout   The maximum time in milliseconds to wait for.
     * @return The final result returned by the {@code condition}, or {@code null} if the {@code
     * condition} was not met before the {@code timeout}.
     */
    public <U> U wait(@NonNull Condition<? super UiObject2, U> condition, long timeout) {
        try (Section ignored = Traces.trace("UiObject2#wait")) {
            Log.d(TAG, String.format("Waiting %dms for %s.", timeout, condition));
            return mWaitMixin.wait(condition, timeout);
        }
    }

    // Search functions

    /** Returns this object's parent, or {@code null} if it has no parent. */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public UiObject2 getParent() {
        AccessibilityNodeInfo parent = getAccessibilityNodeInfo().getParent();
        return parent != null ? UiObject2.create(getDevice(), mSelector, parent) : null;
    }

    /** Returns the number of child elements directly under this object. */
    public int getChildCount() {
        return getAccessibilityNodeInfo().getChildCount();
    }

    /** Returns a collection of the child elements directly under this object. */
    @NonNull
    public List<UiObject2> getChildren() {
        return findObjects(By.depth(1));
    }

    /** Returns {@code true} if there is a nested element which matches the {@code selector}. */
    @Override
    public boolean hasObject(@NonNull BySelector selector) {
        Log.d(TAG, String.format("Searching for node with selector: %s.", selector));
        AccessibilityNodeInfo node =
                ByMatcher.findMatch(getDevice(), selector, getAccessibilityNodeInfo());
        if (node != null) {
            node.recycle();
            return true;
        }
        return false;
    }

    /**
     * Searches all elements under this object and returns the first one to match the {@code
     * selector}, or {@code null} if no matching objects are found.
     */
    @Override
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public UiObject2 findObject(@NonNull BySelector selector) {
        Log.d(TAG, String.format("Retrieving node with selector: %s.", selector));
        AccessibilityNodeInfo node =
                ByMatcher.findMatch(getDevice(), selector, getAccessibilityNodeInfo());
        if (node == null) {
            Log.d(TAG, String.format("Node not found with selector: %s.", selector));
            return null;
        }
        return UiObject2.create(getDevice(), selector, node);
    }

    /**
     * Searches all elements under this object and returns those that match the {@code selector}.
     */
    @Override
    @NonNull
    public List<UiObject2> findObjects(@NonNull BySelector selector) {
        Log.d(TAG, String.format("Retrieving nodes with selector: %s.", selector));
        List<UiObject2> ret = new ArrayList<>();
        for (AccessibilityNodeInfo node :
                ByMatcher.findMatches(getDevice(), selector, getAccessibilityNodeInfo())) {
            UiObject2 object = UiObject2.create(getDevice(), selector, node);
            if (object != null) {
                ret.add(object);
            }
        }
        return ret;
    }

    // Attribute accessors

    /** Returns the ID of the display containing this object. */
    public int getDisplayId() {
        return mDisplayId;
    }

    /** Returns this object's visible bounds. */
    @NonNull
    public Rect getVisibleBounds() {
        return getVisibleBounds(getAccessibilityNodeInfo());
    }

    /** Returns the visible bounds of a {@code node}. */
    private Rect getVisibleBounds(AccessibilityNodeInfo node) {
        //  The display may not be accessible because it can be a private display, for example.
        final boolean isDisplayAccessible = getDevice().getDisplayById(getDisplayId()) != null;
        Rect screen = null;
        if (isDisplayAccessible) {
            Point displaySize = getDevice().getDisplaySize(getDisplayId());
            screen = new Rect(0, 0, displaySize.x, displaySize.y);
        }
        return AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(node, screen, true);
    }

    /** Returns a point in the center of this object's visible bounds. */
    @NonNull
    public Point getVisibleCenter() {
        return getVisibleCenter(getAccessibilityNodeInfo());
    }

    /** Returns a point in the center of the {@code node}'s visible bounds. */
    @NonNull
    private Point getVisibleCenter(AccessibilityNodeInfo node) {
        Rect bounds = getVisibleBounds(node);
        return new Point(bounds.centerX(), bounds.centerY());
    }

    /** Returns this object's visible bounds with the margins removed. */
    private Rect getVisibleBoundsForGestures() {
        return getVisibleBoundsForGestures(getAccessibilityNodeInfo());
    }

    /** Returns the {@code node}'s visible bounds with the margins removed. */
    private Rect getVisibleBoundsForGestures(AccessibilityNodeInfo node) {
        Rect ret = getVisibleBounds(node);
        return mMargins.apply(ret);
    }

    /** Updates a {@code point} to ensure it is within the {@code node}'s visible bounds. */
    private boolean clipToGestureBounds(Point point, AccessibilityNodeInfo node) {
        final Rect bounds = getVisibleBoundsForGestures(node);
        if (bounds.contains(point.x, point.y)) {
            return true;
        }
        Log.d(TAG, String.format("Clipping out-of-bound (%d, %d) into %s.", point.x, point.y,
                bounds));
        point.x = Math.max(bounds.left, Math.min(point.x, bounds.right));
        point.y = Math.max(bounds.top, Math.min(point.y, bounds.bottom));
        return false;
    }

    /** Returns the class name of this object's underlying {@link View}. */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getClassName() {
        CharSequence chars = getAccessibilityNodeInfo().getClassName();
        return chars != null ? chars.toString() : null;
    }

    /**
     * Returns this object's content description.
     *
     * @see View#getContentDescription()
     */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getContentDescription() {
        CharSequence chars = getAccessibilityNodeInfo().getContentDescription();
        return chars != null ? chars.toString() : null;
    }

    /** Returns the package name of the app that this object belongs to. */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getApplicationPackage() {
        CharSequence chars = getAccessibilityNodeInfo().getPackageName();
        return chars != null ? chars.toString() : null;
    }

    /** Returns the fully qualified resource name for this object's ID. */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getResourceName() {
        CharSequence chars = getAccessibilityNodeInfo().getViewIdResourceName();
        return chars != null ? chars.toString() : null;
    }

    /**
     * Returns this object's text content.
     *
     * @see TextView#getText()
     */
    @SuppressLint("UnknownNullness") // Avoid unnecessary null checks from nullable testing APIs.
    public String getText() {
        CharSequence chars = getAccessibilityNodeInfo().getText();
        return chars != null ? chars.toString() : null;
    }

    /**
     * Returns the hint text of this object, or null if hint text is not preset.
     * <p>Hint text is displayed when there's no user input text.
     *
     * @see TextView#getHint()
     */
    @RequiresApi(26)
    @Nullable
    public String getHint() {
        return Api26Impl.getHintText(getAccessibilityNodeInfo());
    }

    /**
     * Returns {@code true} if this object is checkable.
     *
     * @see Checkable
     */
    public boolean isCheckable() {
        return getAccessibilityNodeInfo().isCheckable();
    }

    /**
     * Returns {@code true} if this object is checked.
     *
     * @see Checkable#isChecked()
     */
    public boolean isChecked() {
        return getAccessibilityNodeInfo().isChecked();
    }

    /**
     * Returns {@code true} if this object is clickable.
     *
     * @see View#isClickable()
     */
    public boolean isClickable() {
        return getAccessibilityNodeInfo().isClickable();
    }

    /**
     * Returns {@code true} if this object is enabled.
     *
     * @see TextView#isEnabled()
     */
    public boolean isEnabled() {
        return getAccessibilityNodeInfo().isEnabled();
    }

    /**
     * Returns {@code true} if this object is focusable.
     *
     * @see View#isFocusable()
     */
    public boolean isFocusable() {
        return getAccessibilityNodeInfo().isFocusable();
    }

    /**
     * Returns {@code true} if this object is focused.
     *
     * @see View#isFocused()
     */
    public boolean isFocused() {
        return getAccessibilityNodeInfo().isFocused();
    }

    /**
     * Returns {@code true} if this object is long clickable.
     *
     * @see View#isLongClickable()
     */
    public boolean isLongClickable() {
        return getAccessibilityNodeInfo().isLongClickable();
    }

    /** Returns {@code true} if this object is scrollable. */
    public boolean isScrollable() {
        return getAccessibilityNodeInfo().isScrollable();
    }

    /**
     * Returns {@code true} if this object is selected.
     *
     * @see View#isSelected()
     */
    public boolean isSelected() {
        return getAccessibilityNodeInfo().isSelected();
    }

    /**
     * Returns the drawing order (z-index) of this object relative to its siblings. Higher values
     * are drawn last (i.e. above their siblings).
     * <p>In some cases, the drawing order is essentially simultaneous, so it is possible for two
     * siblings to return the same value. It is also possible that values will be skipped.
     *
     * @return The drawing order of this object relative to its siblings.
     */
    @RequiresApi(24)
    public int getDrawingOrder() {
        return Api24Impl.getDrawingOrder(getAccessibilityNodeInfo());
    }

    // Actions

    /** Clears this object's text content if it is an editable field. */
    public void clear() {
        setText("");
    }

    /** Clicks on this object's center. */
    public void click() {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Point center = getVisibleCenter(node);
        Log.d(TAG, String.format("Clicking on (%d, %d).", center.x, center.y));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        mGestureController.performGesture(Gestures.click(center, getDisplayId()));
    }

    /**
     * Clicks on a {@code point} within this object's visible bounds.
     *
     * @param point The point to click (clipped to ensure it is within the visible bounds).
     */
    public void click(@NonNull Point point) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        clipToGestureBounds(point, node);
        Log.d(TAG, String.format("Clicking on (%d, %d).", point.x, point.y));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        mGestureController.performGesture(Gestures.click(point, getDisplayId()));
    }

    /** Clicks on this object's center for {@code duration} milliseconds. */
    public void click(long duration) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Point center = getVisibleCenter(node);
        Log.d(TAG, String.format("Clicking on (%d, %d) for %dms.", center.x, center.y, duration));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        mGestureController.performGesture(Gestures.click(center, duration, getDisplayId()));
    }

    /**
     * Clicks on a {@code point} within this object's visible bounds.
     *
     * @param point    The point to click (clipped to ensure it is within the visible bounds).
     * @param duration The click duration in milliseconds.
     */
    public void click(@NonNull Point point, long duration) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        clipToGestureBounds(point, node);
        Log.d(TAG, String.format("Clicking on (%d, %d) for %dms.", point.x, point.y, duration));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        mGestureController.performGesture(Gestures.click(point, duration, getDisplayId()));
    }

    /**
     * Clicks on this object's center, and waits for a {@code condition} to be met.
     *
     * @param condition The {@link EventCondition} to wait for.
     * @param timeout   The maximum time in milliseconds to wait for.
     */
    public <U> U clickAndWait(@NonNull EventCondition<U> condition, long timeout) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Point center = getVisibleCenter(node);
        Log.d(TAG, String.format("Clicking on (%d, %d) and waiting %dms for %s.", center.x,
                center.y, timeout, condition));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        return mGestureController.performGestureAndWait(condition, timeout,
                Gestures.click(center, getDisplayId()));
    }

    /**
     * Clicks on a {@code point} within this object's visible bounds, and waits for a {@code
     * condition} to be met.
     *
     * @param point     The point to click (clipped to ensure it is within the visible bounds).
     * @param condition The {@link EventCondition} to wait for.
     * @param timeout   The maximum time in milliseconds to wait for.
     */
    public <U> U clickAndWait(@NonNull Point point, @NonNull EventCondition<U> condition,
            long timeout) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        clipToGestureBounds(point, node);
        Log.d(TAG, String.format("Clicking on (%d, %d) and waiting %dms for %s.", point.x,
                point.y, timeout, condition));
        if (!node.isClickable()) {
            Log.w(TAG, String.format("Clicking on non-clickable object: %s", node));
        }
        return mGestureController.performGestureAndWait(
                condition, timeout, Gestures.click(point, getDisplayId()));
    }

    /**
     * Drags this object to the specified point.
     *
     * @param dest The end point to drag this object to.
     */
    public void drag(@NonNull Point dest) {
        drag(dest, (int) (DEFAULT_DRAG_SPEED * mDisplayDensity));
    }

    /**
     * Drags this object to the specified point.
     *
     * @param dest  The end point to drag this object to.
     * @param speed The speed at which to perform this gesture in pixels per second.
     */
    public void drag(@NonNull Point dest, int speed) {
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        Point center = getVisibleCenter();
        Log.d(TAG, String.format("Dragging from (%d, %d) to (%d, %d) at %dpx/s.", center.x,
                center.y, dest.x, dest.y, speed));
        mGestureController.performGesture(Gestures.drag(center, dest, speed, getDisplayId()));
    }

    /** Performs a long click on this object's center. */
    public void longClick() {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Point center = getVisibleCenter(node);
        Log.d(TAG, String.format("Long-clicking on (%d, %d).", center.x, center.y));
        if (!node.isLongClickable()) {
            Log.w(TAG, String.format("Long-clicking on non-long-clickable object: %s", node));
        }
        mGestureController.performGesture(Gestures.longClick(center, getDisplayId()));
    }

    /**
     * Performs a pinch close gesture on this object.
     *
     * @param percent The size of the pinch as a percentage of this object's size.
     */
    public void pinchClose(float percent) {
        pinchClose(percent, (int) (DEFAULT_PINCH_SPEED * mDisplayDensity));
    }

    /**
     * Performs a pinch close gesture on this object.
     *
     * @param percent The size of the pinch as a percentage of this object's size.
     * @param speed   The speed at which to perform this gesture in pixels per second.
     */
    public void pinchClose(float percent, int speed) {
        if (percent < 0.0f || percent > 1.0f) {
            throw new IllegalArgumentException("Percent must be between 0.0f and 1.0f");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        Rect bounds = getVisibleBoundsForGestures();
        Log.d(TAG, String.format("Pinching close (bounds=%s, percent=%f) at %dpx/s.", bounds,
                percent, speed));
        mGestureController.performGesture(
                Gestures.pinchClose(bounds, percent, speed, getDisplayId()));
    }

    /**
     * Performs a pinch open gesture on this object.
     *
     * @param percent The size of the pinch as a percentage of this object's size.
     */
    public void pinchOpen(float percent) {
        pinchOpen(percent, (int) (DEFAULT_PINCH_SPEED * mDisplayDensity));
    }

    /**
     * Performs a pinch open gesture on this object.
     *
     * @param percent The size of the pinch as a percentage of this object's size.
     * @param speed   The speed at which to perform this gesture in pixels per second.
     */
    public void pinchOpen(float percent, int speed) {
        if (percent < 0.0f || percent > 1.0f) {
            throw new IllegalArgumentException("Percent must be between 0.0f and 1.0f");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        Rect bounds = getVisibleBoundsForGestures();
        Log.d(TAG, String.format("Pinching open (bounds=%s, percent=%f) at %dpx/s.", bounds,
                percent, speed));
        mGestureController.performGesture(
                Gestures.pinchOpen(bounds, percent, speed, getDisplayId()));
    }

    /**
     * Performs a swipe gesture on this object.
     *
     * @param direction The direction in which to swipe.
     * @param percent   The length of the swipe as a percentage of this object's size.
     */
    public void swipe(@NonNull Direction direction, float percent) {
        swipe(direction, percent, (int) (DEFAULT_SWIPE_SPEED * mDisplayDensity));
    }

    /**
     * Performs a swipe gesture on this object.
     *
     * @param direction The direction in which to swipe.
     * @param percent   The length of the swipe as a percentage of this object's size.
     * @param speed     The speed at which to perform this gesture in pixels per second.
     */
    public void swipe(@NonNull Direction direction, float percent, int speed) {
        if (percent < 0.0f || percent > 1.0f) {
            throw new IllegalArgumentException("Percent must be between 0.0f and 1.0f");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }
        Rect bounds = getVisibleBoundsForGestures();
        Log.d(TAG, String.format("Swiping %s (bounds=%s, percent=%f) at %dpx/s.",
                direction.name().toLowerCase(), bounds, percent, speed));
        mGestureController.performGesture(
                Gestures.swipeRect(bounds, direction, percent, speed, getDisplayId()));
    }

    /**
     * Performs a scroll gesture on this object.
     *
     * @param direction The direction in which to scroll.
     * @param percent   The distance to scroll as a percentage of this object's visible size.
     * @return {@code true} if the object can still scroll in the given direction.
     */
    public boolean scroll(@NonNull Direction direction, final float percent) {
        return scroll(direction, percent, (int) (DEFAULT_SCROLL_SPEED * mDisplayDensity));
    }

    /**
     * Performs a scroll gesture on this object.
     *
     * @param direction The direction in which to scroll.
     * @param percent   The distance to scroll as a percentage of this object's visible size.
     * @param speed     The speed at which to perform this gesture in pixels per second.
     * @return {@code true} if the object can still scroll in the given direction.
     */
    public boolean scroll(@NonNull Direction direction, float percent, final int speed) {
        if (percent < 0.0f) {
            throw new IllegalArgumentException("Percent must be greater than 0.0f");
        }
        if (speed < 0) {
            throw new IllegalArgumentException("Speed cannot be negative");
        }

        // To scroll, we swipe in the opposite direction
        final Direction swipeDirection = Direction.reverse(direction);

        // Scroll by performing repeated swipes
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Rect bounds = getVisibleBoundsForGestures(node);
        Log.d(TAG, String.format("Scrolling %s (bounds=%s, percent=%f) at %dpx/s.",
                direction.name().toLowerCase(), bounds, percent, speed));
        if (!node.isScrollable()) {
            Log.w(TAG, String.format("Scrolling on non-scrollable object: %s", node));
        }
        for (; percent > 0.0f; percent -= 1.0f) {
            float segment = Math.min(percent, 1.0f);
            PointerGesture swipe = Gestures.swipeRect(
                    bounds, swipeDirection, segment, speed, getDisplayId()).pause(250);

            // Perform the gesture and return early if we reached the end
            Boolean scrollFinishedResult = mGestureController.performGestureAndWait(
                    Until.scrollFinished(direction), SCROLL_TIMEOUT, swipe);
            if (!Boolean.FALSE.equals(scrollFinishedResult)) {
                if (scrollFinishedResult == null) {
                    Log.i(TAG, "No scroll event received after scroll.");
                }
                return false;
            }
        }
        // We never reached the end
        return true;
    }

    /**
     * Perform scroll actions in certain direction until a {@code condition} is satisfied or scroll
     * has finished, e.g. to scroll until an object contain certain text is found:
     * <pre> mScrollableUiObject2.scrollUntil(Direction.DOWN, Until.findObject(By.textContains
     * ("sometext"))); </pre>
     *
     * @param direction The direction in which to scroll.
     * @param condition The {@link Condition} to evaluate.
     * @return If the condition is satisfied.
     */
    public <U> U scrollUntil(@NonNull Direction direction,
            @NonNull Condition<? super UiObject2, U> condition) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Rect bounds = getVisibleBoundsForGestures(node);
        int speed = (int) (DEFAULT_SCROLL_SPEED * mDisplayDensity);
        int nullScrollRetryCount = 0;

        if (!node.isScrollable()) {
            Log.w(TAG, String.format("Scrolling on non-scrollable object: %s", node));
        }

        // To scroll, we swipe in the opposite direction
        final Direction swipeDirection = Direction.reverse(direction);
        while (true) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                // b/267804786: clearing cache on API 28 before applying the condition.
                clearCache();
            }
            U result = condition.apply(this);
            if (result != null && !Boolean.FALSE.equals(result)) {
                // given condition is satisfied.
                return result;
            }
            PointerGesture swipe = Gestures.swipeRect(bounds, swipeDirection,
                    DEFAULT_SCROLL_UNTIL_PERCENT, speed, getDisplayId()).pause(250);
            EventCondition<Boolean> scrollFinished = Until.scrollFinished(direction);
            Boolean scrollFinishedResult =
                    mGestureController.performGestureAndWait(scrollFinished, SCROLL_TIMEOUT, swipe);
            if (Boolean.TRUE.equals(scrollFinishedResult)) {
                // Scroll has finished.
                Log.i(TAG, "scrollUntil reached the end.");
                break;
            } else if (scrollFinishedResult == null) {
                // Couldn't determine whether scroll finished after retries.
                if (nullScrollRetryCount++ >= MAX_NULL_SCROLL_RETRY) {
                    Log.i(TAG, "scrollUntil reached max retries for null events.");
                    break;
                }
                Log.i(TAG, String.format("Couldn't determine whether scroll was finished, "
                        + "retrying: count %d", nullScrollRetryCount - 1));
            }
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // b/267804786: clearing cache on API 28 before applying the condition.
            clearCache();
        }
        return condition.apply(this);
    }

    /**
     * Perform scroll actions in certain direction until a {@code condition} is satisfied or scroll
     * has finished, e.g. to scroll until a new window has appeared:
     * <pre> mScrollableUiObject2.scrollUntil(Direction.DOWN, Until.newWindow()); </pre>
     *
     * @param direction The direction in which to scroll.
     * @param condition The {@link EventCondition} to wait for.
     * @return The value obtained after applying the condition.
     */
    public <U> U scrollUntil(@NonNull Direction direction, @NonNull EventCondition<U> condition) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();
        Rect bounds = getVisibleBoundsForGestures(node);
        int speed = (int) (DEFAULT_SCROLL_SPEED * mDisplayDensity);
        int nullScrollRetryCount = 0;

        if (!node.isScrollable()) {
            Log.w(TAG, String.format("Scrolling on non-scrollable object: %s", node));
        }

        // To scroll, we swipe in the opposite direction
        final Direction swipeDirection = Direction.reverse(direction);
        while (true) {
            // combine the input condition with scroll finished condition.
            EventCondition<Boolean> scrollFinished = Until.scrollFinished(direction);
            EventCondition<Boolean> combinedEventCondition = new EventCondition<Boolean>() {
                @Override
                public Boolean getResult() {
                    if (Boolean.TRUE.equals(scrollFinished.getResult())) {
                        // scroll has finished.
                        return true;
                    }
                    U result = condition.getResult();
                    return result != null && !Boolean.FALSE.equals(result);
                }

                @Override
                public boolean accept(AccessibilityEvent event) {
                    return condition.accept(event) || scrollFinished.accept(event);
                }

                @NonNull
                @Override
                public String toString() {
                    return condition + " || " + scrollFinished;
                }
            };
            PointerGesture swipe = Gestures.swipeRect(bounds, swipeDirection,
                    DEFAULT_SCROLL_UNTIL_PERCENT, speed, getDisplayId()).pause(250);
            if (mGestureController.performGestureAndWait(combinedEventCondition, SCROLL_TIMEOUT,
                    swipe)) {
                if (Boolean.TRUE.equals(scrollFinished.getResult())) {
                    Log.i(TAG, "scrollUntil reached the end.");
                }
                // Either scroll has finished or the accessibility event has appeared.
                break;
            } else if (scrollFinished.getResult() == null) {
                // Couldn't determine whether scroll finished after retries.
                if (nullScrollRetryCount++ >= MAX_NULL_SCROLL_RETRY) {
                    Log.i(TAG, "scrollUntil reached max retries for null events.");
                    break;
                }
                Log.i(TAG, String.format("Couldn't determine whether scroll was finished, "
                        + "retrying: count %d", nullScrollRetryCount - 1));
            }
        }
        return condition.getResult();
    }

    /**
     * Performs a fling gesture on this object.
     *
     * @param direction The direction in which to fling.
     * @return {@code true} if the object can still scroll in the given direction.
     */
    public boolean fling(@NonNull Direction direction) {
        return fling(direction, (int) (DEFAULT_FLING_SPEED * mDisplayDensity));
    }

    /**
     * Performs a fling gesture on this object.
     *
     * @param direction The direction in which to fling.
     * @param speed     The speed at which to perform this gesture in pixels per second.
     * @return {@code true} if the object can still scroll in the given direction.
     */
    public boolean fling(@NonNull Direction direction, final int speed) {
        ViewConfiguration vc = ViewConfiguration.get(getDevice().getUiContext(getDisplayId()));
        if (speed < vc.getScaledMinimumFlingVelocity()) {
            throw new IllegalArgumentException("Speed is less than the minimum fling velocity");
        }

        // To fling, we swipe in the opposite direction
        final Direction swipeDirection = Direction.reverse(direction);

        Rect bounds = getVisibleBoundsForGestures();
        PointerGesture swipe = Gestures.swipeRect(
                bounds, swipeDirection, 1.0f, speed, getDisplayId());

        // Perform the gesture and return true if we did not reach the end
        Log.d(TAG, String.format("Flinging %s (bounds=%s) at %dpx/s.",
                direction.name().toLowerCase(), bounds, speed));
        Boolean scrollFinishedResult = mGestureController.performGestureAndWait(
                Until.scrollFinished(direction), FLING_TIMEOUT, swipe);
        if (scrollFinishedResult == null) {
            Log.i(TAG, "No scroll event received after fling.");
        }
        return Boolean.FALSE.equals(scrollFinishedResult);
    }

    /** Sets this object's text content if it is an editable field. */
    public void setText(@Nullable String text) {
        AccessibilityNodeInfo node = getAccessibilityNodeInfo();

        // Per framework convention, setText(null) means clearing it
        if (text == null) {
            text = "";
        }

        Log.d(TAG, String.format("Setting text to '%s'.", text));
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            Log.w(TAG, "AccessibilityNodeInfo#performAction(ACTION_SET_TEXT) failed");
        }
    }

    /**
     * Returns an up-to-date {@link AccessibilityNodeInfo} corresponding to this object's
     * underlying {@link View}. Note that this method can be expensive as it wait for the device to
     * be idle and tries multiple time to refresh the {@link AccessibilityNodeInfo}.
     */
    private AccessibilityNodeInfo getAccessibilityNodeInfo() {
        if (mCachedNode == null) {
            throw new IllegalStateException("This object has already been recycled.");
        }

        getDevice().waitForIdle();
        if (!mCachedNode.refresh()) {
            Log.w(TAG, "Failed to refresh AccessibilityNodeInfo. Retrying.");
            getDevice().runWatchers();

            if (!mCachedNode.refresh()) {
                throw new StaleObjectException();
            }
        }
        return mCachedNode;
    }

    /**
     * Clear the a11y cache.
     * @throws Exception
     */
    @SuppressLint("SoonBlockedPrivateApi") // Only used in API 28
    private void clearCache() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, String.format("clearCache() reflection is not available on API >= 33,"
                    + " current API: %d", Build.VERSION.SDK_INT));
            return;
        }
        try {
            Class<?> clazz = Class.forName(
                    "android.view.accessibility.AccessibilityInteractionClient");
            Method getInstance = clazz.getDeclaredMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance != null) {
                Method clearCache = instance.getClass().getDeclaredMethod("clearCache");
                clearCache.invoke(instance);
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to call AccessibilityInteractionClient#clearCache() reflection", e);
        }

    }

    UiDevice getDevice() {
        return mDevice;
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
        }

        static int getDrawingOrder(AccessibilityNodeInfo accessibilityNodeInfo) {
            return accessibilityNodeInfo.getDrawingOrder();
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
        }

        static String getHintText(AccessibilityNodeInfo accessibilityNodeInfo) {
            CharSequence chars = accessibilityNodeInfo.getHintText();
            return chars != null ? chars.toString() : null;
        }
    }

    @RequiresApi(30)
    static class Api30Impl {
        private Api30Impl() {
        }

        static int getDisplayId(AccessibilityWindowInfo accessibilityWindowInfo) {
            return accessibilityWindowInfo.getDisplayId();
        }
    }

    private interface Margins {
        Rect apply(Rect bounds);
    }

    private static class SimpleMargins implements Margins {
        int mLeft, mTop, mRight, mBottom;
        SimpleMargins(int left, int top, int right, int bottom) {
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
        }

        @Override
        public Rect apply(Rect bounds) {
            return new Rect(bounds.left + mLeft,
                    bounds.top + mTop,
                    bounds.right - mRight,
                    bounds.bottom - mBottom);
        }
    }

    private static class PercentMargins implements Margins {
        float mLeft, mTop, mRight, mBottom;
        PercentMargins(float left, float top, float right, float bottom) {
            mLeft = left;
            mTop = top;
            mRight = right;
            mBottom = bottom;
        }

        @Override
        public Rect apply(Rect bounds) {
            return new Rect(bounds.left + (int) (bounds.width() * mLeft),
                    bounds.top + (int) (bounds.height() * mTop),
                    bounds.right - (int) (bounds.width() * mRight),
                    bounds.bottom - (int) (bounds.height() * mBottom));
        }
    }
}
