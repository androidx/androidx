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

import static android.view.View.VISIBLE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContentInfo;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.AccessibilityRecord;
import android.view.autofill.AutofillId;
import android.view.contentcapture.ContentCaptureSession;
import android.view.inputmethod.InputConnection;

import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SimpleArrayMap;
import androidx.core.R;
import androidx.core.util.Preconditions;
import androidx.core.view.AccessibilityDelegateCompat.AccessibilityDelegateAdapter;
import androidx.core.view.HapticFeedbackConstantsCompat.HapticFeedbackFlags;
import androidx.core.view.HapticFeedbackConstantsCompat.HapticFeedbackType;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.core.view.autofill.AutofillIdCompat;
import androidx.core.view.contentcapture.ContentCaptureSessionCompat;
import androidx.core.viewtree.ViewTree;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper for accessing features in {@link View}.
 */
@SuppressWarnings({"JavadocReference", "DeprecatedIsStillUsed", "JavaDoc", "RedundantSuppression"})
// Unreliable warnings.
@SuppressLint("PrivateConstructorForUtilityClass") // deprecated non-private constructor
public class ViewCompat {
    private static final String TAG = "ViewCompat";

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({View.FOCUS_LEFT, View.FOCUS_UP, View.FOCUS_RIGHT, View.FOCUS_DOWN,
            View.FOCUS_FORWARD, View.FOCUS_BACKWARD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusDirection {}

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({View.FOCUS_LEFT, View.FOCUS_UP, View.FOCUS_RIGHT, View.FOCUS_DOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRealDirection {}

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({View.FOCUS_FORWARD, View.FOCUS_BACKWARD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRelativeDirection {}

    @SuppressWarnings("deprecation")
    @IntDef({OVER_SCROLL_ALWAYS, OVER_SCROLL_IF_CONTENT_SCROLLS, OVER_SCROLL_NEVER})
    @Retention(RetentionPolicy.SOURCE)
    private @interface OverScroll {}

    /**
     * Always allow a user to over-scroll this view, provided it is a
     * view that can scroll.
     * @deprecated Use {@link View#OVER_SCROLL_ALWAYS} directly. This constant will be removed in
     * a future release.
     */
    @Deprecated
    public static final int OVER_SCROLL_ALWAYS = 0;

    /**
     * Allow a user to over-scroll this view only if the content is large
     * enough to meaningfully scroll, provided it is a view that can scroll.
     * @deprecated Use {@link View#OVER_SCROLL_IF_CONTENT_SCROLLS} directly. This constant will be
     * removed in a future release.
     */
    @Deprecated
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

    /**
     * Never allow a user to over-scroll this view.
     * @deprecated Use {@link View#OVER_SCROLL_NEVER} directly. This constant will be removed in
     * a future release.
     */
    @Deprecated
    public static final int OVER_SCROLL_NEVER = 2;

    @RequiresApi(Build.VERSION_CODES.O)
    @IntDef({
            View.IMPORTANT_FOR_AUTOFILL_AUTO,
            View.IMPORTANT_FOR_AUTOFILL_YES,
            View.IMPORTANT_FOR_AUTOFILL_NO,
            View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS,
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface AutofillImportance {}

    @SuppressWarnings("deprecation")
    @IntDef({
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            IMPORTANT_FOR_ACCESSIBILITY_YES,
            IMPORTANT_FOR_ACCESSIBILITY_NO,
            IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ImportantForAccessibility {}

    @IntDef({
            IMPORTANT_FOR_CONTENT_CAPTURE_AUTO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ImportantForContentCapture {}

    /**
     * Automatically determine whether a view is important for content capture.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0x0;

    /**
     * The view is important for content capture, and its children (if any) will be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 0x1;

    /**
     * The view is not important for content capture, but its children (if any) will be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 0x2;

    /**
     * The view is important for content capture, but its children (if any) will not be traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 0x4;

    /**
     * The view is not important for content capture, and its children (if any) will not be
     * traversed.
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 0x8;

    /**
     * Automatically determine whether a view is important for accessibility.
     *
     * @deprecated Use {@link View#IMPORTANT_FOR_ACCESSIBILITY_AUTO} directly.
     */
    @Deprecated
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;

    /**
     * The view is important for accessibility.
     *
     * @deprecated Use {@link View#IMPORTANT_FOR_ACCESSIBILITY_YES} directly.
     */
    @Deprecated
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;

    /**
     * The view is not important for accessibility.
     *
     * @deprecated Use {@link View#IMPORTANT_FOR_ACCESSIBILITY_NO} directly.
     */
    @Deprecated
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;

    /**
     * The view is not important for accessibility, nor are any of its
     * descendant views.
     *
     * @deprecated Use {@link View#IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS} directly.
     */
    @Deprecated
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 0x00000004;

    @IntDef({
            ACCESSIBILITY_LIVE_REGION_NONE,
            ACCESSIBILITY_LIVE_REGION_POLITE,
            ACCESSIBILITY_LIVE_REGION_ASSERTIVE
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface AccessibilityLiveRegion {}

    /**
     * Live region mode specifying that accessibility services should not
     * automatically announce changes to this view. This is the default live
     * region mode for most views.
     * <p>
     * Use with {@link ViewCompat#setAccessibilityLiveRegion(View, int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0x00000000;

    /**
     * Live region mode specifying that accessibility services should announce
     * changes to this view.
     * <p>
     * Use with {@link ViewCompat#setAccessibilityLiveRegion(View, int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 0x00000001;

    /**
     * Live region mode specifying that accessibility services should interrupt
     * ongoing speech to immediately announce changes to this view.
     * <p>
     * Use with {@link ViewCompat#setAccessibilityLiveRegion(View, int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 0x00000002;

    @IntDef({View.LAYER_TYPE_NONE, View.LAYER_TYPE_SOFTWARE, View.LAYER_TYPE_HARDWARE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LayerType {}

    /**
     * Indicates that the view does not have a layer.
     *
     * @deprecated Use {@link View#LAYER_TYPE_NONE} directly.
     */
    @Deprecated
    public static final int LAYER_TYPE_NONE = 0;

    /**
     * <p>Indicates that the view has a software layer. A software layer is backed
     * by a bitmap and causes the view to be rendered using Android's software
     * rendering pipeline, even if hardware acceleration is enabled.</p>
     *
     * <p>Software layers have various usages:</p>
     * <p>When the application is not using hardware acceleration, a software layer
     * is useful to apply a specific color filter and/or blending mode and/or
     * translucency to a view and all its children.</p>
     * <p>When the application is using hardware acceleration, a software layer
     * is useful to render drawing primitives not supported by the hardware
     * accelerated pipeline. It can also be used to cache a complex view tree
     * into a texture and reduce the complexity of drawing operations. For instance,
     * when animating a complex view tree with a translation, a software layer can
     * be used to render the view tree only once.</p>
     * <p>Software layers should be avoided when the affected view tree updates
     * often. Every update will require to re-render the software layer, which can
     * potentially be slow (particularly when hardware acceleration is turned on
     * since the layer will have to be uploaded into a hardware texture after every
     * update.)</p>
     *
     * @deprecated Use {@link View#LAYER_TYPE_SOFTWARE} directly.
     */
    @Deprecated
    public static final int LAYER_TYPE_SOFTWARE = 1;

    /**
     * <p>Indicates that the view has a hardware layer. A hardware layer is backed
     * by a hardware specific texture (generally Frame Buffer Objects or FBO on
     * OpenGL hardware) and causes the view to be rendered using Android's hardware
     * rendering pipeline, but only if hardware acceleration is turned on for the
     * view hierarchy. When hardware acceleration is turned off, hardware layers
     * behave exactly as {@link View#LAYER_TYPE_SOFTWARE software layers}.</p>
     *
     * <p>A hardware layer is useful to apply a specific color filter and/or
     * blending mode and/or translucency to a view and all its children.</p>
     * <p>A hardware layer can be used to cache a complex view tree into a
     * texture and reduce the complexity of drawing operations. For instance,
     * when animating a complex view tree with a translation, a hardware layer can
     * be used to render the view tree only once.</p>
     * <p>A hardware layer can also be used to increase the rendering quality when
     * rotation transformations are applied on a view. It can also be used to
     * prevent potential clipping issues when applying 3D transforms on a view.</p>
     *
     * @deprecated Use {@link View#LAYER_TYPE_HARDWARE} directly.
     */
    @Deprecated
    public static final int LAYER_TYPE_HARDWARE = 2;

    @IntDef({
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface LayoutDirectionMode {}

    @IntDef({
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ResolvedLayoutDirectionMode {}

    /**
     * Horizontal layout direction of this view is from Left to Right.
     *
     * @deprecated Use {@link View#LAYOUT_DIRECTION_LTR} directly.
     */
    @Deprecated
    public static final int LAYOUT_DIRECTION_LTR = 0;

    /**
     * Horizontal layout direction of this view is from Right to Left.
     *
     * @deprecated Use {@link View#LAYOUT_DIRECTION_RTL} directly.
     */
    @Deprecated
    public static final int LAYOUT_DIRECTION_RTL = 1;

    /**
     * Horizontal layout direction of this view is inherited from its parent.
     * Use with {@link #setLayoutDirection}.
     *
     * @deprecated Use {@link View#LAYOUT_DIRECTION_INHERIT} directly.
     */
    @Deprecated
    public static final int LAYOUT_DIRECTION_INHERIT = 2;

    /**
     * Horizontal layout direction of this view is from deduced from the default language
     * script for the locale. Use with {@link #setLayoutDirection}.
     *
     * @deprecated Use {@link View#LAYOUT_DIRECTION_LOCALE} directly.
     */
    @Deprecated
    public static final int LAYOUT_DIRECTION_LOCALE = 3;

    /**
     * Bits of {@link #getMeasuredWidthAndState} and
     * {@link #getMeasuredWidthAndState} that provide the actual measured size.
     *
     * @deprecated Use {@link View#MEASURED_SIZE_MASK} directly.
     */
    @Deprecated
    public static final int MEASURED_SIZE_MASK = 0x00ffffff;

    /**
     * Bits of {@link #getMeasuredWidthAndState} and
     * {@link #getMeasuredWidthAndState} that provide the additional state bits.
     *
     * @deprecated Use {@link View#MEASURED_STATE_MASK} directly.
     */
    @Deprecated
    public static final int MEASURED_STATE_MASK = 0xff000000;

    /**
     * Bit shift of {@link #MEASURED_STATE_MASK} to get to the height bits
     * for functions that combine both width and height into a single int,
     * such as {@link #getMeasuredState} and the childState argument of
     * {@link #resolveSizeAndState(int, int, int)}.
     *
     * @deprecated Use {@link View#MEASURED_HEIGHT_STATE_SHIFT} directly.
     */
    @Deprecated
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;

    /**
     * Bit of {@link #getMeasuredWidthAndState} and
     * {@link #getMeasuredWidthAndState} that indicates the measured size
     * is smaller that the space the view would like to have.
     *
     * @deprecated Use {@link View#MEASURED_STATE_TOO_SMALL} directly.
     */
    @Deprecated
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    /**
     */
    @IntDef(value = {SCROLL_AXIS_NONE, SCROLL_AXIS_HORIZONTAL, SCROLL_AXIS_VERTICAL}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @interface ScrollAxis {}

    /**
     * Indicates no axis of view scrolling.
     */
    public static final int SCROLL_AXIS_NONE = 0;

    /**
     * Indicates scrolling along the horizontal axis.
     */
    public static final int SCROLL_AXIS_HORIZONTAL = 1;

    /**
     * Indicates scrolling along the vertical axis.
     */
    public static final int SCROLL_AXIS_VERTICAL = 1 << 1;

    /**
     */
    @IntDef({TYPE_TOUCH, TYPE_NON_TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public @interface NestedScrollType {}

    /**
     * Indicates that the input type for the gesture is from a user touching the screen.
     */
    public static final int TYPE_TOUCH = 0;

    /**
     * Indicates that the input type for the gesture is caused by something which is not a user
     * touching a screen. This is usually from a fling which is settling.
     */
    public static final int TYPE_NON_TOUCH = 1;

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true,
            value = {
                    SCROLL_INDICATOR_TOP,
                    SCROLL_INDICATOR_BOTTOM,
                    SCROLL_INDICATOR_LEFT,
                    SCROLL_INDICATOR_RIGHT,
                    SCROLL_INDICATOR_START,
                    SCROLL_INDICATOR_END,
            })
    public @interface ScrollIndicators {}

    /**
     * Scroll indicator direction for the top edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_TOP = 0x1;

    /**
     * Scroll indicator direction for the bottom edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_BOTTOM = 0x2;

    /**
     * Scroll indicator direction for the left edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_LEFT = 0x4;

    /**
     * Scroll indicator direction for the right edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_RIGHT = 0x8;

    /**
     * Scroll indicator direction for the starting edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_START = 0x10;

    /**
     * Scroll indicator direction for the ending edge of the view.
     *
     * @see #setScrollIndicators(View, int)
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static final int SCROLL_INDICATOR_END = 0x20;

    private static Method sDispatchStartTemporaryDetach;
    private static Method sDispatchFinishTemporaryDetach;
    private static boolean sTempDetachBound;

    private static WeakHashMap<View, ViewPropertyAnimatorCompat> sViewPropertyAnimatorMap = null;

    private static Method sChildrenDrawingOrderMethod;
    private static Field sAccessibilityDelegateField;
    private static boolean sAccessibilityDelegateCheckFailed = false;

    private static boolean sTryHiddenViewTransformMatrixToGlobal = true;

    /**
     * Stores debugging information about attributes. This should be called in a constructor by
     * every custom {@link View} that uses a custom styleable. If the custom view does not call it,
     * then the custom attributes used by this view will not be visible in layout inspection tools.
     *
     * No-op before API 29.
     *
     * @param view view for which to save the data.
     * @param context Context under which this view is created.
     * @param styleable A reference to styleable array R.styleable.Foo
     * @param attrs AttributeSet used to construct this view.
     * @param t Resolved {@link TypedArray} returned by a call to
     *        {@link android.content.res.Resources#obtainAttributes(AttributeSet, int[])}.
     * @param defStyleAttr Default style attribute passed into the view constructor.
     * @param defStyleRes Default style resource passed into the view constructor.
     */
    public static void saveAttributeDataForStyleable(@NonNull View view,
            @SuppressLint("ContextFirst") @NonNull Context context, int @NonNull [] styleable,
            @Nullable AttributeSet attrs, @NonNull TypedArray t, int defStyleAttr,
            int defStyleRes) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.saveAttributeDataForStyleable(
                    view, context, styleable, attrs, t, defStyleAttr, defStyleRes);
        }
    }

    /**
     * Check if this view can be scrolled horizontally in a certain direction.
     *
     * @param view The View against which to invoke the method.
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     *
     * @deprecated Use {@link View#canScrollHorizontally(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.canScrollHorizontally(direction)")
    @Deprecated
    public static boolean canScrollHorizontally(View view, int direction) {
        return view.canScrollHorizontally(direction);
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param view The View against which to invoke the method.
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     * @deprecated Use {@link View#canScrollVertically(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.canScrollVertically(direction)")
    @Deprecated
    public static boolean canScrollVertically(View view, int direction) {
        return view.canScrollVertically(direction);
    }

    /**
     * Returns the over-scroll mode for this view. The result will be
     * one of {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * @param view The View against which to invoke the method.
     * @return This view's over-scroll mode.
     * @deprecated Call {@link View#getOverScrollMode()} directly. This method will be
     * removed in a future release.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getOverScrollMode()")
    @Deprecated
    @OverScroll
    public static int getOverScrollMode(View view) {
        //noinspection ResourceType
        return view.getOverScrollMode();
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
     * @param view The View against which to invoke the method.
     * @param overScrollMode The new over-scroll mode for this view.
     * @deprecated Call {@link View#setOverScrollMode(int)} directly. This method will be
     * removed in a future release.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setOverScrollMode(overScrollMode)")
    @Deprecated
    public static void setOverScrollMode(View view, @OverScroll int overScrollMode) {
        view.setOverScrollMode(overScrollMode);
    }

    /**
     * Called from {@link View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)}
     * giving a chance to this View to populate the accessibility event with its
     * text content.
     * <p>
     * <b>Note:</b> This method should only be used with {@link AccessibilityRecord#getText()}.
     * Avoid mutating other event state in this method. Instead, follow the practices described in
     * {@link #dispatchPopulateAccessibilityEvent(AccessibilityEvent)}. In general, put UI
     * metadata in the node for services to easily query, than in events.
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
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link ViewCompat#setAccessibilityDelegate(View, AccessibilityDelegateCompat)} its
     * {@link AccessibilityDelegateCompat#onPopulateAccessibilityEvent(View, AccessibilityEvent)}
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
     *
     * @deprecated Call {@link View#onPopulateAccessibilityEvent(AccessibilityEvent)} directly.
     * This method will be removed in a future release.
     */
    @androidx.annotation.ReplaceWith(expression = "v.onPopulateAccessibilityEvent(event)")
    @Deprecated
    public static void onPopulateAccessibilityEvent(View v, AccessibilityEvent event) {
        v.onPopulateAccessibilityEvent(event);
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
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link ViewCompat#setAccessibilityDelegate(View, AccessibilityDelegateCompat)}, its
     * {@link AccessibilityDelegateCompat#onInitializeAccessibilityEvent(View, AccessibilityEvent)}
     * is responsible for handling this call.
     *
     * @param v The View against which to invoke the method.
     * @param event The event to initialize.
     *
     * @see View#sendAccessibilityEvent(int)
     * @see View#dispatchPopulateAccessibilityEvent(AccessibilityEvent)
     *
     * @deprecated Call {@link View#onInitializeAccessibilityEvent(AccessibilityEvent)} directly.
     * This method will be removed in a future release.
     */
    @androidx.annotation.ReplaceWith(expression = "v.onInitializeAccessibilityEvent(event)")
    @Deprecated
    public static void onInitializeAccessibilityEvent(View v, AccessibilityEvent event) {
        v.onInitializeAccessibilityEvent(event);
    }

    /**
     * Initializes an {@link AccessibilityNodeInfoCompat} with information
     * about this view. The base implementation sets:
     * <ul>
     * <li>{@link AccessibilityNodeInfoCompat#setParent(View)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setBoundsInParent(Rect)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setBoundsInScreen(Rect)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setPackageName(CharSequence)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setClassName(CharSequence)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setContentDescription(CharSequence)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setStateDescription(CharSequence)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setEnabled(boolean)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setClickable(boolean)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setFocusable(boolean)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setFocused(boolean)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setLongClickable(boolean)},</li>
     * <li>{@link AccessibilityNodeInfoCompat#setSelected(boolean)},</li>
     * </ul>
     * <p>
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link ViewCompat#setAccessibilityDelegate(View, AccessibilityDelegateCompat)}, its
     * {@link AccessibilityDelegateCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)}
     * method is responsible for handling this call.
     *
     * @param v The View against which to invoke the method.
     * @param info The instance to initialize.
     * @deprecated Call {@link View#onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo)}
     * directly.
     */
    @androidx.annotation.ReplaceWith(expression = "v.onInitializeAccessibilityNodeInfo(info.unwrap())")
    @Deprecated
    public static void onInitializeAccessibilityNodeInfo(@NonNull View v,
            @NonNull AccessibilityNodeInfoCompat info) {
        v.onInitializeAccessibilityNodeInfo(info.unwrap());
    }

    /**
     * Sets a delegate for implementing accessibility support via composition
     * (as opposed to inheritance). For more details, see
     * {@link AccessibilityDelegateCompat}.
     * <p>
     * <strong>Note:</strong> On platform versions prior to
     * {@link android.os.Build.VERSION_CODES#M API 23}, delegate methods on
     * views in the {@code android.widget.*} package are called <i>before</i>
     * host methods. This prevents certain properties such as class name from
     * being modified by overriding
     * {@link AccessibilityDelegateCompat#onInitializeAccessibilityNodeInfo(View, AccessibilityNodeInfoCompat)},
     * as any changes will be overwritten by the host class.
     * <p>
     * Starting in {@link android.os.Build.VERSION_CODES#M API 23}, delegate
     * methods are called <i>after</i> host methods, which all properties to be
     * modified without being overwritten by the host class.
     * <p>
     * If an AccessibilityDelegateCompat is already attached to the view, and this method sets
     * the delegate to null, an empty delegate will be attached to ensure that other compatibility
     * behavior continues to work for this view.
     *
     * @param v view for which to set the delegate.
     * @param delegate the object to which accessibility method calls should be
     *                 delegated
     * @see AccessibilityDelegateCompat
     */
    public static void setAccessibilityDelegate(
            @NonNull View v, @Nullable AccessibilityDelegateCompat delegate) {
        if ((delegate == null)
                && (getAccessibilityDelegateInternal(v) instanceof AccessibilityDelegateAdapter)) {
            delegate = new AccessibilityDelegateCompat();
        }
        setImportantForAccessibilityIfNeeded(v);
        v.setAccessibilityDelegate(delegate == null ? null : delegate.getBridge());
    }

    /**
     * Sets the hints that help an {@link android.service.autofill.AutofillService} determine how
     * to autofill the view with the user's data.
     *
     * <p>Typically, there is only one way to autofill a view, but there could be more than one.
     * For example, if the application accepts either an username or email address to identify
     * an user.
     *
     * <p>These hints are not validated by the Android System, but passed "as is" to the service.
     * Hence, they can have any value, but it's recommended to use the {@code AUTOFILL_HINT_}
     * constants such as:
     * {@link View#AUTOFILL_HINT_USERNAME}, {@link View#AUTOFILL_HINT_PASSWORD},
     * {@link View#AUTOFILL_HINT_EMAIL_ADDRESS},
     * {@link View#AUTOFILL_HINT_NAME},
     * {@link View#AUTOFILL_HINT_PHONE},
     * {@link View#AUTOFILL_HINT_POSTAL_ADDRESS}, {@link View#AUTOFILL_HINT_POSTAL_CODE},
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_NUMBER},
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE},
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE},
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY},
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH} or
     * {@link View#AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}.
     *
     * <p>This method is only supported on API >= 26.
     * On API 25 and below, it is a no-op</p>
     *
     * @param view view for which to set the hints.
     * @param autofillHints The autofill hints to set. If the array is emtpy, {@code null} is set.
     * {@link android.R.attr#autofillHints}
     */
    public static void setAutofillHints(@NonNull View view, String @Nullable ... autofillHints) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setAutofillHints(view, autofillHints);
        }
    }

    /**
     * Gets the mode for determining whether this view is important for autofill.
     *
     * <p>See {@link #setImportantForAutofill(View, int)} and {@link #isImportantForAutofill(View)}
     * for more info about this mode.
     *
     * <p>This method is only supported on API >= 26.
     * On API 25 and below, it will always return {@link View#IMPORTANT_FOR_AUTOFILL_AUTO}.</p>
     *
     * @param v The View against which to invoke the method.
     * @return {@link View#IMPORTANT_FOR_AUTOFILL_AUTO} by default, or value passed to
     * {@link #setImportantForAutofill(View, int)}.
     *
     * {@link android.R.attr#importantForAutofill}
     */
    @SuppressLint("InlinedApi")
    public static @AutofillImportance int getImportantForAutofill(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getImportantForAutofill(v);
        }
        return View.IMPORTANT_FOR_AUTOFILL_AUTO;
    }

    /**
     * Sets the mode for determining whether this view is considered important for autofill.
     *
     * <p>The platform determines the importance for autofill automatically but you
     * can use this method to customize the behavior. For example:
     *
     * <ol>
     *   <li>When the view contents is irrelevant for autofill (for example, a text field used in a
     *       "Captcha" challenge), it should be {@link View#IMPORTANT_FOR_AUTOFILL_NO}.</li>
     *   <li>When both the view and its children are irrelevant for autofill (for example, the root
     *       view of an activity containing a spreadhseet editor), it should be
     *       {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS}.</li>
     *   <li>When the view content is relevant for autofill but its children aren't (for example,
     *       a credit card expiration date represented by a custom view that overrides the proper
     *       autofill methods and has 2 children representing the month and year), it should
     *       be {@link View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS}.</li>
     * </ol>
     *
     * <p><strong>NOTE:</strong> setting the mode as does {@link View#IMPORTANT_FOR_AUTOFILL_NO} or
     * {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS} does not guarantee the view (and
     * its children) will be always be considered not important; for example, when the user
     * explicitly makes an autofill request, all views are considered important. See
     * {@link #isImportantForAutofill(View)} for more details about how the View's importance for
     * autofill is used.
     *
     * <p>This method is only supported on API >= 26.
     * On API 25 and below, it is a no-op</p>
     *
     * @param v The View against which to invoke the method.
     * @param mode {@link View#IMPORTANT_FOR_AUTOFILL_AUTO},
     * {@link View#IMPORTANT_FOR_AUTOFILL_YES},
     * {@link View#IMPORTANT_FOR_AUTOFILL_NO},
     * {@link View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS},
     * or {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS}.
     *
     * {@link android.R.attr#importantForAutofill}
     */
    public static void setImportantForAutofill(@NonNull View v, @AutofillImportance int mode) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setImportantForAutofill(v, mode);
        }
    }

    /**
     * Hints the Android System whether the {@link android.app.assist.AssistStructure.ViewNode}
     * associated with this view is considered important for autofill purposes.
     *
     * <p>Generally speaking, a view is important for autofill if:
     * <ol>
     * <li>The view can be autofilled by an {@link android.service.autofill.AutofillService}.</li>
     * <li>The view contents can help an {@link android.service.autofill.AutofillService}
     *     determine how other views can be autofilled.</li>
     * </ol>
     *
     * <p>For example, view containers should typically return {@code false} for performance reasons
     * (since the important info is provided by their children), but if its properties have relevant
     * information (for example, a resource id called {@code credentials}, it should return
     * {@code true}. On the other hand, views representing labels or editable fields should
     * typically return {@code true}, but in some cases they could return {@code false}
     * (for example, if they're part of a "Captcha" mechanism).
     *
     * <p>The value returned by this method depends on the value returned by
     * {@link #getImportantForAutofill(View)}:
     *
     * <ol>
     *   <li>if it returns {@link View#IMPORTANT_FOR_AUTOFILL_YES} or
     *       {@link View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS},
     *       then it returns {@code true}</li>
     *   <li>if it returns {@link View#IMPORTANT_FOR_AUTOFILL_NO} or
     *       {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS},
     *       then it returns {@code false}</li>
     *   <li>if it returns {@link View#IMPORTANT_FOR_AUTOFILL_AUTO},
     *   then it uses some simple heuristics that can return {@code true}
     *   in some cases (like a container with a resource id), but {@code false} in most.</li>
     *   <li>otherwise, it returns {@code false}.</li>
     * </ol>
     *
     * <p>When a view is considered important for autofill:
     * <ul>
     *   <li>The view might automatically trigger an autofill request when focused on.
     *   <li>The contents of the view are included in the {@link android.view.ViewStructure}
     *   used in an autofill request.
     * </ul>
     *
     * <p>On the other hand, when a view is considered not important for autofill:
     * <ul>
     *   <li>The view never automatically triggers autofill requests, but it can trigger a manual
     *       request through {@link android.view.autofill.AutofillManager#requestAutofill(View)}.
     *   <li>The contents of the view are not included in the {@link android.view.ViewStructure}
     *   used in an autofill request, unless the request has the
     *       {@link View#AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS} flag.
     * </ul>
     *
     * <p>This method is only supported on API >= 26.
     * On API 25 and below, it will always return {@code true}.</p>
     *
     * @param v The View against which to invoke the method.
     * @return whether the view is considered important for autofill.
     *
     * @see #setImportantForAutofill(View, int)
     * @see View#IMPORTANT_FOR_AUTOFILL_AUTO
     * @see View#IMPORTANT_FOR_AUTOFILL_YES
     * @see View#IMPORTANT_FOR_AUTOFILL_NO
     * @see View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS
     * @see View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
     * @see android.view.autofill.AutofillManager#requestAutofill(View)
     */
    public static boolean isImportantForAutofill(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.isImportantForAutofill(v);
        }
        return true;
    }

    /**
     * Gets the unique, logical identifier of this view in the activity, for autofill purposes.
     *
     * <p>The autofill id is created on demand, unless it is explicitly set by
     * {@link #setAutofillId(AutofillId)}.
     *
     * <p>See {@link #setAutofillId(AutofillId)} for more info.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 26 and above, this method matches platform behavior.
     * <li>SDK 25 and below, this method always return null.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return The View's autofill id.
     */
    public static @Nullable AutofillIdCompat getAutofillId(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 26) {
            return AutofillIdCompat.toAutofillIdCompat(Api26Impl.getAutofillId(v));
        }
        return null;
    }

    /**
     * Sets the unique, logical identifier of this view in the activity, for autofill purposes.
     *
     * <p>The autofill id is created on demand, and this method should only be called when a view is
     * reused after {@link #dispatchProvideAutofillStructure(ViewStructure, int)} is called, as
     * that method creates a snapshot of the view that is passed along to the autofill service.
     *
     * <p>This method is typically used when view subtrees are recycled to represent different
     * content* &mdash;in this case, the autofill id can be saved before the view content is swapped
     * out, and restored later when it's swapped back in. For example:
     *
     * <pre>
     * EditText reusableView = ...;
     * ViewGroup parentView = ...;
     * AutofillManager afm = ...;
     *
     * // Swap out the view and change its contents
     * AutofillId oldId = reusableView.getAutofillId();
     * CharSequence oldText = reusableView.getText();
     * parentView.removeView(reusableView);
     * AutofillId newId = afm.getNextAutofillId();
     * reusableView.setText("New I am");
     * reusableView.setAutofillId(newId);
     * parentView.addView(reusableView);
     *
     * // Later, swap the old content back in
     * parentView.removeView(reusableView);
     * reusableView.setAutofillId(oldId);
     * reusableView.setText(oldText);
     * parentView.addView(reusableView);
     * </pre>
     *
     * <p>NOTE: If this view is a descendant of an {@link android.widget.AdapterView}, the system
     * may reset its autofill id when this view is recycled. If the autofill ids need to be stable,
     * they should be set again in
     * {@link android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 28 and above, this method matches platform behavior.
     * <li>SDK 27 and below, this method does nothing.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @param id an autofill ID that is unique in the {@link android.app.Activity} hosting the view,
     * or {@code null} to reset it. Usually it's an id previously allocated to another view (and
     * obtained through {@link #getAutofillId()}), or a new value obtained through
     * {@link AutofillManager#getNextAutofillId()}.
     *
     * @throws IllegalStateException if the view is already {@link #isAttachedToWindow() attached to
     * a window}.
     *
     * @throws IllegalArgumentException if the id is an autofill id associated with a virtual view.
     */
    public static void setAutofillId(@NonNull View v, @Nullable AutofillIdCompat id) {
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.setAutofillId(v, id);
        }
    }

    /**
     * Sets the mode for determining whether this view is considered important for content capture.
     *
     * <p>The platform determines the importance for autofill automatically but you
     * can use this method to customize the behavior. Typically, a view that provides text should
     * be marked as {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES}.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 30 and above, this method matches platform behavior.
     * <li>SDK 29 and below, this method does nothing.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @param mode {@link #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO},
     * {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES}, {@link #IMPORTANT_FOR_CONTENT_CAPTURE_NO},
     * {@link #IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS},
     * or {@link #IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS}.
     *
     * @attr ref android.R.styleable#View_importantForContentCapture
     */
    public static void setImportantForContentCapture(@NonNull View v,
            @ImportantForContentCapture int mode) {
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Impl.setImportantForContentCapture(v, mode);
        }
    }

    /**
     * Gets the mode for determining whether this view is important for content capture.
     *
     * <p>See {@link #setImportantForContentCapture(int)} and
     * {@link #isImportantForContentCapture()} for more info about this mode.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 30 and above, this method matches platform behavior.
     * <li>SDK 29 and below, this method always return {@link #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO}.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return {@link #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO} by default, or value passed to
     * {@link #setImportantForContentCapture(int)}.
     *
     * @attr ref android.R.styleable#View_importantForContentCapture
     */
    public static int getImportantForContentCapture(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.getImportantForContentCapture(v);
        }
        return IMPORTANT_FOR_CONTENT_CAPTURE_AUTO;
    }

    /**
     * Hints the Android System whether this view is considered important for content capture, based
     * on the value explicitly set by {@link #setImportantForContentCapture(int)} and heuristics
     * when it's {@link #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO}.
     *
     * <p>See {@link ContentCaptureManager} for more info about content capture.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 30 and above, this method matches platform behavior.
     * <li>SDK 29 and below, this method always return false.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return whether the view is considered important for content capture.
     *
     * @see #setImportantForContentCapture(int)
     * @see #IMPORTANT_FOR_CONTENT_CAPTURE_AUTO
     * @see #IMPORTANT_FOR_CONTENT_CAPTURE_YES
     * @see #IMPORTANT_FOR_CONTENT_CAPTURE_NO
     * @see #IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS
     * @see #IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
     */
    public static boolean isImportantForContentCapture(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.isImportantForContentCapture(v);
        }
        return false;
    }

    /**
     * Gets the session used to notify content capture events.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method always return null.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @return session explicitly set by {@link #setContentCaptureSession(ContentCaptureSession)},
     * inherited by ancestors, default session or {@code null} if content capture is disabled for
     * this view.
     */
    public static @Nullable ContentCaptureSessionCompat getContentCaptureSession(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentCaptureSession session = Api29Impl.getContentCaptureSession(v);
            if (session == null) {
                return null;
            }
            return ContentCaptureSessionCompat.toContentCaptureSessionCompat(session, v);
        }
        return null;
    }

    /**
     * Sets the (optional) {@link ContentCaptureSession} associated with this view.
     *
     * <p>This method should be called when you need to associate a {@link ContentCaptureContext} to
     * the content capture events associated with this view or its view hierarchy (if it's a
     * {@link ViewGroup}).
     *
     * <p>For example, if your activity is associated with a web domain, first you would need to
     * set the context for the main DOM:
     *
     * <pre>
     *   ContentCaptureSession mainSession = rootView.getContentCaptureSession();
     *   mainSession.setContentCaptureContext(ContentCaptureContext.forLocusId(Uri.parse(myUrl));
     * </pre>
     *
     * <p>Then if the page had an {@code IFRAME}, you would create a new session for it:
     *
     * <pre>
     *   ContentCaptureSession iframeSession = mainSession.createContentCaptureSession(
     *       ContentCaptureContext.forLocusId(Uri.parse(iframeUrl)));
     *   iframeView.setContentCaptureSession(iframeSession);
     * </pre>
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 28 and below, this method does nothing.
     * </ul>
     *
     * @param v The View against which to invoke the method.
     * @param contentCaptureSession a session created by
     * {@link ContentCaptureSession#createContentCaptureSession(
     *        android.view.contentcapture.ContentCaptureContext)}.
     */
    public static void setContentCaptureSession(@NonNull View v,
            @Nullable ContentCaptureSessionCompat contentCaptureSession) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setContentCaptureSession(v, contentCaptureSession);
        }
    }

    /**
     * Checks whether provided View has an accessibility delegate attached to it.
     *
     * @param view The View instance to check
     * @return True if the View has an accessibility delegate
     */
    public static boolean hasAccessibilityDelegate(@NonNull View view) {
        return getAccessibilityDelegateInternal(view) != null;
    }

    /**
     * Get the current accessibility delegate.
     * @see #setAccessibilityDelegate(View, AccessibilityDelegateCompat)
     *
     * @param view The view whose delegate is of interest
     * @return A compat wrapper for the current delegate. If no delegate is attached, you may
     *         still get an object that is being used to provide backward compatibility. Returns
     *         {@code null} if there is no delegate attached.
     */
    public static @Nullable AccessibilityDelegateCompat getAccessibilityDelegate(
            @NonNull View view) {
        final View.AccessibilityDelegate delegate = getAccessibilityDelegateInternal(view);
        if (delegate == null) {
            return null;
        }
        if (delegate instanceof AccessibilityDelegateAdapter) {
            return ((AccessibilityDelegateAdapter) delegate).mCompat;
        }
        return new AccessibilityDelegateCompat(delegate);
    }

    static void ensureAccessibilityDelegateCompat(@NonNull View v) {
        AccessibilityDelegateCompat delegateCompat = getAccessibilityDelegate(v);
        if (delegateCompat == null) {
            delegateCompat = new AccessibilityDelegateCompat();
        }
        setAccessibilityDelegate(v, delegateCompat);
    }

    private static View.@Nullable AccessibilityDelegate getAccessibilityDelegateInternal(
            @NonNull View v) {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.getAccessibilityDelegate(v);
        } else {
            return getAccessibilityDelegateThroughReflection(v);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess") // Private field
    private static View.@Nullable AccessibilityDelegate getAccessibilityDelegateThroughReflection(
            @NonNull View v) {
        if (sAccessibilityDelegateCheckFailed) {
            return null; // View implementation might have changed.
        }
        if (sAccessibilityDelegateField == null) {
            try {
                sAccessibilityDelegateField = View.class
                        .getDeclaredField("mAccessibilityDelegate");
                sAccessibilityDelegateField.setAccessible(true);
            } catch (Throwable t) {
                sAccessibilityDelegateCheckFailed = true;
                return null;
            }
        }
        try {
            Object o = sAccessibilityDelegateField.get(v);
            if (o instanceof View.AccessibilityDelegate) {
                return (View.AccessibilityDelegate) o;
            }
            return null;
        } catch (Throwable t) {
            sAccessibilityDelegateCheckFailed = true;
            return null;
        }
    }

    /**
     * Indicates whether the view is currently tracking transient state that the
     * app should not need to concern itself with saving and restoring, but that
     * the framework should take special note to preserve when possible.
     *
     * @param view View to check for transient state
     * @return true if the view has transient state
     * @deprecated Call {@link View#hasTransientState()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.hasTransientState()")
    @Deprecated
    public static boolean hasTransientState(@NonNull View view) {
        return view.hasTransientState();
    }

    /**
     * Set whether this view is currently tracking transient state that the
     * framework should attempt to preserve when possible.
     *
     * @param view View tracking transient state
     * @param hasTransientState true if this view has transient state
     * @deprecated Call {@link View#setHasTransientState(boolean)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setHasTransientState(hasTransientState)")
    @Deprecated
    public static void setHasTransientState(@NonNull View view, boolean hasTransientState) {
        view.setHasTransientState(hasTransientState);
    }

    /**
     * <p>Cause an invalidate to happen on the next animation time step, typically the
     * next display frame.</p>
     *
     * <p>This method can be invoked from outside of the UI thread
     * only when this View is attached to a window.</p>
     *
     * @param view View to invalidate
     * @deprecated Call {@link View#postInvalidateOnAnimation()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.postInvalidateOnAnimation()")
    @Deprecated
    public static void postInvalidateOnAnimation(@NonNull View view) {
        view.postInvalidateOnAnimation();
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
     * @deprecated Call {@link View#postInvalidateOnAnimation(int, int, int, int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.postInvalidateOnAnimation(left, top, right, bottom)")
    @Deprecated
    public static void postInvalidateOnAnimation(@NonNull View view, int left, int top,
            int right, int bottom) {
        view.postInvalidateOnAnimation(left, top, right, bottom);
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
     * @deprecated Call {@link View#postOnAnimation(Runnable)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.postOnAnimation(action)")
    @Deprecated
    public static void postOnAnimation(@NonNull View view, @NonNull Runnable action) {
        view.postOnAnimation(action);
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
     * @deprecated Call {@link View#postOnAnimationDelayed(Runnable, long)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.postOnAnimationDelayed(action, delayMillis)")
    @Deprecated
    @SuppressLint("LambdaLast")
    public static void postOnAnimationDelayed(@NonNull View view, @NonNull Runnable action,
            long delayMillis) {
        view.postOnAnimationDelayed(action, delayMillis);
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
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
     * @see #IMPORTANT_FOR_ACCESSIBILITY_AUTO
     * @deprecated Call {@link View#getImportantForAccessibility()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getImportantForAccessibility()")
    @Deprecated
    @ImportantForAccessibility
    public static int getImportantForAccessibility(@NonNull View view) {
        return view.getImportantForAccessibility();
    }

    /**
     * Sets how to determine whether this view is important for accessibility
     * which is if it fires accessibility events and if it is reported to
     * accessibility services that query the screen.
     * <p>
     * <em>Note:</em> If the current platform version does not support the
     *  {@link #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS} mode, then
     *  {@link #IMPORTANT_FOR_ACCESSIBILITY_NO} will be used as it is the
     *  closest terms of semantics.
     * </p>
     *
     * @param view The view whose property to set.
     * @param mode How to determine whether this view is important for accessibility.
     *
     * @see #IMPORTANT_FOR_ACCESSIBILITY_YES
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
     * @see #IMPORTANT_FOR_ACCESSIBILITY_AUTO
     * @deprecated Call {@link View#setImportantForAccessibility(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setImportantForAccessibility(mode)")
    @Deprecated
    @UiThread
    public static void setImportantForAccessibility(@NonNull View view,
            @ImportantForAccessibility int mode) {
        view.setImportantForAccessibility(mode);
    }

    /**
     * Computes whether this view should be exposed for accessibility. In
     * general, views that are interactive or provide information are exposed
     * while views that serve only as containers are hidden.
     * <p>
     * If an ancestor of this view has importance
     * {@link #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS}, this method
     * returns <code>false</code>.
     * <p>
     * Otherwise, the value is computed according to the view's
     * {@link #getImportantForAccessibility(View)} value:
     * <ol>
     * <li>{@link #IMPORTANT_FOR_ACCESSIBILITY_NO} or
     * {@link #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS}, return <code>false
     * </code></li>
     * <li>{@link #IMPORTANT_FOR_ACCESSIBILITY_YES}, return <code>true</code></li>
     * <li>{@link #IMPORTANT_FOR_ACCESSIBILITY_AUTO}, return <code>true</code> if
     * view satisfies any of the following:</li>
     * <ul>
     * <li>Is actionable, e.g. {@link View#isClickable()},
     * {@link View#isLongClickable()}, or {@link View#isFocusable()}</li>
     * <li>Has an {@link AccessibilityDelegateCompat}</li>
     * <li>Has an interaction listener, e.g. {@link View.OnTouchListener},
     * {@link View.OnKeyListener}, etc.</li>
     * <li>Is an accessibility live region, e.g.
     * {@link #getAccessibilityLiveRegion(View)} is not
     * {@link #ACCESSIBILITY_LIVE_REGION_NONE}.</li>
     * </ul>
     * </ol>
     * <p>
     * <em>Note:</em> Prior to API 21, this method will always return {@code true}.
     *
     * @param view view for which to check the state.
     * @return Whether the view is exposed for accessibility.
     * @see #setImportantForAccessibility(View, int)
     * @see #getImportantForAccessibility(View)
     */
    public static boolean isImportantForAccessibility(@NonNull View view) {
        return view.isImportantForAccessibility();
    }

    /**
     * Performs the specified accessibility action on the view. For
     * possible accessibility actions look at {@link AccessibilityNodeInfoCompat}.
     * <p>
     * If an {@link AccessibilityDelegateCompat} has been specified via calling
     * {@link #setAccessibilityDelegate(View, AccessibilityDelegateCompat)} its
     * {@link AccessibilityDelegateCompat#performAccessibilityAction(View, int, Bundle)}
     * is responsible for handling this call.
     * </p>
     * <p>
     * <b>Note:</b> Avoid setting accessibility focus with
     * {@link AccessibilityNodeInfoCompat.AccessibilityActionCompat#ACTION_ACCESSIBILITY_FOCUS}.
     * This is intended to be controlled by screen readers. Apps changing focus can confuse
     * screen readers, and the resulting behavior can vary by device and screen reader version.
     *
     * @param view view on which to perform the action.
     * @param action The action to perform.
     * @param arguments Optional action arguments.
     * @return Whether the action was performed.
     * @deprecated Call {@link View#performAccessibilityAction(int, Bundle)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.performAccessibilityAction(action, arguments)")
    @Deprecated
    public static boolean performAccessibilityAction(@NonNull View view, int action,
            @Nullable Bundle arguments) {
        return view.performAccessibilityAction(action, arguments);
    }

    /**
     * Perform a haptic feedback to the user for the view.
     *
     * <p>The framework will provide haptic feedback for some built in actions, such as long
     * presses, but you may wish to provide feedback for your own widget.
     *
     * <p>The feedback will only be performed if {@link android.view.View#isHapticFeedbackEnabled()}
     * is true.
     *
     * <em>Note:</em> Check compatibility support for each feedback constant described at
     * {@link HapticFeedbackConstantsCompat}.
     *
     * @param view             The view.
     * @param feedbackConstant One of the constants defined in {@link HapticFeedbackConstantsCompat}
     * @return Whether the feedback might be performed - generally this result should be ignored
     */
    public static boolean performHapticFeedback(@NonNull View view,
            @HapticFeedbackType int feedbackConstant) {
        feedbackConstant =
                HapticFeedbackConstantsCompat.getFeedbackConstantOrFallback(feedbackConstant);
        if (feedbackConstant == HapticFeedbackConstantsCompat.NO_HAPTICS) {
            // This compat implementation is straightforward.
            return false;
        }
        return view.performHapticFeedback(feedbackConstant);
    }

    /**
     * Perform a haptic feedback to the user for the view.
     *
     * <p>This is similar to {@link #performHapticFeedback(android.view.View, int)}, with
     * additional options.
     *
     * <em>Note:</em> Check compatibility support for each feedback constant described at
     * {@link HapticFeedbackConstantsCompat}.
     *
     * @param view             The view.
     * @param feedbackConstant One of the constants defined in {@link HapticFeedbackConstantsCompat}
     * @param flags            Additional flags as per {@link HapticFeedbackConstantsCompat}
     * @return Whether the feedback might be performed - generally this result should be ignored
     */
    public static boolean performHapticFeedback(@NonNull View view,
            @HapticFeedbackType int feedbackConstant, @HapticFeedbackFlags int flags) {
        feedbackConstant =
                HapticFeedbackConstantsCompat.getFeedbackConstantOrFallback(feedbackConstant);
        if (feedbackConstant == HapticFeedbackConstantsCompat.NO_HAPTICS) {
            // This compat implementation is straightforward.
            return false;
        }
        return view.performHapticFeedback(feedbackConstant, flags);
    }

    /**
     * Adds an accessibility action that can be performed on a node associated with a view.
     * A view can only have 32 actions created with this API.
     *
     * @param view    The view.
     * @param label   The user facing description of the action. If an action with the same label
     *               already exists, it will be replaced.
     * @param command The command performed when the service requests the action.
     * @return The id associated with the action,
     * or {@link View#NO_ID} if the action could not be created.
     * This id can be used to remove the action.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static int addAccessibilityAction(
            @NonNull View view, @NonNull CharSequence label,
            @NonNull AccessibilityViewCommand command) {
        int actionId = getAvailableActionIdFromResources(view, label);
        if (actionId != View.NO_ID) {
            AccessibilityActionCompat action =
                    new AccessibilityActionCompat(actionId, label, command);
            addAccessibilityAction(view, action);
        }
        return actionId;
    }

    private static final int[] ACCESSIBILITY_ACTIONS_RESOURCE_IDS = {
            R.id.accessibility_custom_action_0,
            R.id.accessibility_custom_action_1,
            R.id.accessibility_custom_action_2,
            R.id.accessibility_custom_action_3,
            R.id.accessibility_custom_action_4,
            R.id.accessibility_custom_action_5,
            R.id.accessibility_custom_action_6,
            R.id.accessibility_custom_action_7,
            R.id.accessibility_custom_action_8,
            R.id.accessibility_custom_action_9,
            R.id.accessibility_custom_action_10,
            R.id.accessibility_custom_action_11,
            R.id.accessibility_custom_action_12,
            R.id.accessibility_custom_action_13,
            R.id.accessibility_custom_action_14,
            R.id.accessibility_custom_action_15,
            R.id.accessibility_custom_action_16,
            R.id.accessibility_custom_action_17,
            R.id.accessibility_custom_action_18,
            R.id.accessibility_custom_action_19,
            R.id.accessibility_custom_action_20,
            R.id.accessibility_custom_action_21,
            R.id.accessibility_custom_action_22,
            R.id.accessibility_custom_action_23,
            R.id.accessibility_custom_action_24,
            R.id.accessibility_custom_action_25,
            R.id.accessibility_custom_action_26,
            R.id.accessibility_custom_action_27,
            R.id.accessibility_custom_action_28,
            R.id.accessibility_custom_action_29,
            R.id.accessibility_custom_action_30,
            R.id.accessibility_custom_action_31};

    private static int getAvailableActionIdFromResources(View view, @NonNull CharSequence label) {
        int result = View.NO_ID;
        // Finds the existing custom action id by label.
        List<AccessibilityActionCompat> actions = getActionList(view);
        for (int i = 0; i < actions.size(); i++) {
            if (TextUtils.equals(label, actions.get(i).getLabel())) {
                return actions.get(i).getId();
            }
        }
        // Finds the first available action id from resources.
        for (int i = 0; i < ACCESSIBILITY_ACTIONS_RESOURCE_IDS.length && result == View.NO_ID;
                i++) {
            int id = ACCESSIBILITY_ACTIONS_RESOURCE_IDS[i];
            boolean idAvailable = true;
            for (int j = 0; j < actions.size(); j++) {
                idAvailable &= actions.get(j).getId() != id;
            }
            if (idAvailable) {
                result = id;
            }
        }
        return result;
    }

    /**
     * Replaces an action. This can be used to change the default behavior or label of the action
     * specified. If label and command are both {@code null}, the action will be removed.
     *
     * @param view The view.
     * @param replacedAction The action to be replaced.
     * @param label The user facing description of the action or {@code null}.
     * @param command The command performed when the service requests the action.
     *
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     */
    public static void replaceAccessibilityAction(@NonNull View view,
            @NonNull AccessibilityActionCompat replacedAction, @Nullable CharSequence label,
            @Nullable AccessibilityViewCommand command) {
        if (command == null && label == null) {
            ViewCompat.removeAccessibilityAction(view, replacedAction.getId());
        } else {
            addAccessibilityAction(view, replacedAction.createReplacementAction(label, command));
        }
    }

    private static void addAccessibilityAction(@NonNull View view,
            @NonNull AccessibilityActionCompat action) {
        ensureAccessibilityDelegateCompat(view);
        removeActionWithId(action.getId(), view);
        getActionList(view).add(action);
        notifyViewAccessibilityStateChangedIfNeeded(
                view, AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
    }

    /**
     * Removes an accessibility action that can be performed on a node associated with a view.
     * If the action was not already added to the view, calling this method has no effect.
     *
     * @param view The view
     * @param actionId The actionId of the action to be removed.
     */
    public static void removeAccessibilityAction(@NonNull View view, int actionId) {
        removeActionWithId(actionId, view);
        notifyViewAccessibilityStateChangedIfNeeded(
                view, AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
    }

    private static void removeActionWithId(int actionId, View view) {
        List<AccessibilityActionCompat> actions = getActionList(view);
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getId() == actionId) {
                actions.remove(i);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<AccessibilityActionCompat> getActionList(View view) {
        ArrayList<AccessibilityActionCompat> actions =
                (ArrayList<AccessibilityActionCompat>) view.getTag(R.id.tag_accessibility_actions);
        if (actions == null) {
            actions = new ArrayList<>();
            view.setTag(R.id.tag_accessibility_actions, actions);
        }
        return actions;
    }

    /**
     * Sets the state description of this node.
     * <p>
     *   <strong>Note:</strong> Cannot be called from an
     *   {@link android.accessibilityservice.AccessibilityService}.
     *   This class is made immutable before being delivered to an AccessibilityService.
     * </p>
     * <p>
     * State refers to a frequently changing property of the View, such as an enabled/disabled
     * state of a button or the audio level of a volume slider.
     *
     * <p>
     * This should omit role or content. Role refers to the kind of user-interface element the
     * View is, such as a Button or Checkbox. Content is the meaningful text and graphics that
     * should be described by {@link View#setContentDescription(CharSequence)} or
     * {@code android:contentDescription}. It is expected that a content description mostly
     * remains constant, while a state description updates from time to time.
     *
     * @param view view for which to set the description.
     * @param stateDescription the state description of this node.
     *
     * @throws IllegalStateException If called from an AccessibilityService.
     * @see View#setStateDescription(CharSequence)
     * @see View#setContentDescription(CharSequence)
     */
    @UiThread
    public static void setStateDescription(@NonNull View view,
            @Nullable CharSequence stateDescription) {
        stateDescriptionProperty().set(view, stateDescription);
    }

    /**
     * Returns the {@link View}'s state description.
     * <p>
     * <strong>Note:</strong> Do not override this method, as it will have no
     * effect on the state description presented to accessibility services.
     * You must call {@link #setStateDescription(View, CharSequence)} to modify the
     * state description.
     *
     * @param view view for which to get the description
     * @return the state description
     * @see #setStateDescription(View, CharSequence)
     */
    @UiThread
    public static @Nullable CharSequence getStateDescription(@NonNull View view) {
        return stateDescriptionProperty().get(view);
    }


    /**
     * Allow accessibility services to find and activate clickable spans in the application.
     *
     * <p>
     * {@link android.text.style.ClickableSpan} is automatically supported from
     * API 26. For compatibility back to API 19, this should be enabled.
     * <p>
     * {@link android.text.style.URLSpan}, a subclass of ClickableSpans, is
     * automatically supported and does not need this enabled.
     * <p>
     * Do not put ClickableSpans in {@link View#setContentDescription(CharSequence)} or
     * {@link View#setStateDescription(CharSequence)}.
     * These links are only visible to accessibility services in
     * {@link AccessibilityNodeInfoCompat#getText()}, which should be
     * modifiable using helper methods on UI elements. For example, use
     * {@link android.widget.TextView#setText(CharSequence)} to modify the text of TextViews.
     *
     * @param view The view
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: No-op
     * </ul>
     */
    public static void enableAccessibleClickableSpanSupport(@NonNull View view) {
        ensureAccessibilityDelegateCompat(view);
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
     * {@link #setAccessibilityDelegate(View, AccessibilityDelegateCompat)} its
     * {@link AccessibilityDelegateCompat#getAccessibilityNodeProvider(View)}
     * is responsible for handling this call.
     * </p>
     *
     * @param view The view whose property to get.
     * @return The provider.
     *
     * @see AccessibilityNodeProviderCompat
     */
    public static @Nullable AccessibilityNodeProviderCompat getAccessibilityNodeProvider(
            @NonNull View view) {
        AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
        if (provider != null) {
            return new AccessibilityNodeProviderCompat(provider);
        }
        return null;
    }

    /**
     * The opacity of the view. This is a value from 0 to 1, where 0 means the view is
     * completely transparent and 1 means the view is completely opaque.
     *
     * <p>By default this is 1.0f.
     * @return The opacity of the view.
     *
     * @deprecated Use {@link View#getAlpha()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getAlpha()")
    @Deprecated
    public static float getAlpha(View view) {
        return view.getAlpha();
    }

    /**
     * <p>Specifies the type of layer backing this view. The layer can be
     * {@link View#LAYER_TYPE_NONE disabled}, {@link View#LAYER_TYPE_SOFTWARE software} or
     * {@link View#LAYER_TYPE_HARDWARE hardware}.</p>
     *
     * <p>A layer is associated with an optional {@link android.graphics.Paint}
     * instance that controls how the layer is composed on screen. The following
     * properties of the paint are taken into account when composing the layer:</p>
     * <ul>
     * <li>{@link android.graphics.Paint#getAlpha() Translucency (alpha)}</li>
     * <li>{@link android.graphics.Paint#getXfermode() Blending mode}</li>
     * <li>{@link android.graphics.Paint#getColorFilter() Color filter}</li>
     * </ul>
     *
     * <p>If this view has an alpha value set to < 1.0 by calling
     * setAlpha(float), the alpha value of the layer's paint is replaced by
     * this view's alpha value. Calling setAlpha(float) is therefore
     * equivalent to setting a hardware layer on this view and providing a paint with
     * the desired alpha value.<p>
     *
     * <p>Refer to the documentation of {@link View#LAYER_TYPE_NONE disabled},
     * {@link View#LAYER_TYPE_SOFTWARE software} and {@link View#LAYER_TYPE_HARDWARE hardware}
     * for more information on when and how to use layers.</p>
     *
     * @param view View to set the layer type for
     * @param layerType The type of layer to use with this view, must be one of
     *        {@link View#LAYER_TYPE_NONE}, {@link View#LAYER_TYPE_SOFTWARE} or
     *        {@link View#LAYER_TYPE_HARDWARE}
     * @param paint The paint used to compose the layer. This argument is optional
     *        and can be null. It is ignored when the layer type is
     *        {@link View#LAYER_TYPE_NONE}
     *
     * @deprecated Use {@link View#setLayerType(int, Paint)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setLayerType(layerType, paint)")
    @Deprecated
    public static void setLayerType(View view, @LayerType int layerType, Paint paint) {
        view.setLayerType(layerType, paint);
    }

    /**
     * Indicates what type of layer is currently associated with this view. By default
     * a view does not have a layer, and the layer type is {@link View#LAYER_TYPE_NONE}.
     * Refer to the documentation of
     * {@link #setLayerType(android.view.View, int, android.graphics.Paint)}
     * for more information on the different types of layers.
     *
     * @param view The view to fetch the layer type from
     * @return {@link View#LAYER_TYPE_NONE}, {@link View#LAYER_TYPE_SOFTWARE} or
     *         {@link View#LAYER_TYPE_HARDWARE}
     *
     * @see #setLayerType(android.view.View, int, android.graphics.Paint)
     * @see View#LAYER_TYPE_NONE
     * @see View#LAYER_TYPE_SOFTWARE
     * @see View#LAYER_TYPE_HARDWARE
     *
     * @deprecated Use {@link View#getLayerType()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getLayerType()")
    @Deprecated
    @LayerType
    public static int getLayerType(View view) {
        //noinspection ResourceType
        return view.getLayerType();
    }

    /**
     * Gets the id of a view for which a given view serves as a label for
     * accessibility purposes.
     *
     * @param view The view on which to invoke the corresponding method.
     * @return The labeled view id.
     * @deprecated Call {@link View#getLabelFor()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getLabelFor()")
    @Deprecated
    public static int getLabelFor(@NonNull View view) {
        return view.getLabelFor();
    }

    /**
     * Sets the id of a view for which a given view serves as a label for
     * accessibility purposes.
     *
     * @param view The view on which to invoke the corresponding method.
     * @param labeledId The labeled view id.
     * @deprecated Call {@link View#setLabelFor(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setLabelFor(labeledId)")
    @Deprecated
    public static void setLabelFor(@NonNull View view, @IdRes int labeledId) {
        view.setLabelFor(labeledId);
    }

    /**
     * Updates the {@link Paint} object used with the current layer (used only if the current
     * layer type is not set to {@link View#LAYER_TYPE_NONE}). Changed properties of the Paint
     * provided to {@link #setLayerType(android.view.View, int, android.graphics.Paint)}
     * will be used the next time the View is redrawn, but
     * {@link #setLayerPaint(android.view.View, android.graphics.Paint)}
     * must be called to ensure that the view gets redrawn immediately.
     *
     * <p>A layer is associated with an optional {@link android.graphics.Paint}
     * instance that controls how the layer is composed on screen. The following
     * properties of the paint are taken into account when composing the layer:</p>
     * <ul>
     * <li>{@link android.graphics.Paint#getAlpha() Translucency (alpha)}</li>
     * <li>{@link android.graphics.Paint#getXfermode() Blending mode}</li>
     * <li>{@link android.graphics.Paint#getColorFilter() Color filter}</li>
     * </ul>
     *
     * <p>If this view has an alpha value set to < 1.0 by calling
     * View#setAlpha(float), the alpha value of the layer's paint is replaced by
     * this view's alpha value. Calling View#setAlpha(float) is therefore
     * equivalent to setting a hardware layer on this view and providing a paint with
     * the desired alpha value.</p>
     *
     * @param view View to set a layer paint for
     * @param paint The paint used to compose the layer. This argument is optional
     *        and can be null. It is ignored when the layer type is
     *        {@link View#LAYER_TYPE_NONE}
     *
     * @see #setLayerType(View, int, android.graphics.Paint)
     * @deprecated Call {@link View#setLayerPaint(Paint)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setLayerPaint(paint)")
    @Deprecated
    public static void setLayerPaint(@NonNull View view, @Nullable Paint paint) {
        view.setLayerPaint(paint);
    }

    /**
     * Returns the resolved layout direction for this view.
     *
     * @param view View to get layout direction for
     * @return {@link #LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
     * {@link #LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
     *
     * For compatibility, this will return {@link #LAYOUT_DIRECTION_LTR} if API version
     * is lower than Jellybean MR1 (API 17)
     *
     * @deprecated Call {@link View#getLayoutDirection()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getLayoutDirection()")
    @Deprecated
    @ResolvedLayoutDirectionMode
    public static int getLayoutDirection(@NonNull View view) {
        return view.getLayoutDirection();
    }

    /**
     * Set the layout direction for this view. This will propagate a reset of layout direction
     * resolution to the view's children and resolve layout direction for this view.
     *
     * @param view View to set layout direction for
     * @param layoutDirection the layout direction to set. Should be one of:
     *
     * {@link #LAYOUT_DIRECTION_LTR},
     * {@link #LAYOUT_DIRECTION_RTL},
     * {@link #LAYOUT_DIRECTION_INHERIT},
     * {@link #LAYOUT_DIRECTION_LOCALE}.
     *
     * Resolution will be done if the value is set to LAYOUT_DIRECTION_INHERIT. The resolution
     * proceeds up the parent chain of the view to get the value. If there is no parent, then it
     * will return the default {@link #LAYOUT_DIRECTION_LTR}.
     *
     * @deprecated Call {@link View#setLayoutDirection(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setLayoutDirection(layoutDirection)")
    @Deprecated
    public static void setLayoutDirection(@NonNull View view,
            @LayoutDirectionMode int layoutDirection) {
        view.setLayoutDirection(layoutDirection);
    }

    /**
     * Gets the parent for accessibility purposes. Note that the parent for
     * accessibility is not necessary the immediate parent. It is the first
     * predecessor that is important for accessibility.
     *
     * @param view View to retrieve parent for
     * @return The parent for use in accessibility inspection
     * @deprecated Call {@link View#getParentForAccessibility()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getParentForAccessibility()")
    @Deprecated
    public static @Nullable ViewParent getParentForAccessibility(@NonNull View view) {
        return view.getParentForAccessibility();
    }

    /**
     * Finds the first descendant view with the given ID, the view itself if the ID matches
     * {@link View#getId()}, or throws an IllegalArgumentException if the ID is invalid or there
     * is no matching view in the hierarchy.
     * <p>
     * <strong>Note:</strong> In most cases -- depending on compiler support --
     * the resulting view is automatically cast to the target class type. If
     * the target class type is unconstrained, an explicit cast may be
     * necessary.
     *
     * @param view the view to start the search from.
     * @param id the ID to search for
     * @return a view with given ID
     * @see View#findViewById(int)
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T extends View> @NonNull T requireViewById(@NonNull View view, @IdRes int id) {
        if (Build.VERSION.SDK_INT >= 28) {
            return ViewCompat.Api28Impl.requireViewById(view, id);
        }

        T targetView = view.findViewById(id);
        if (targetView == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this View");
        }
        return targetView;
    }

    /**
     * Indicates whether this View is opaque. An opaque View guarantees that it will
     * draw all the pixels overlapping its bounds using a fully opaque color.
     *
     * @param view view for which to check the state.
     * @return True if this View is guaranteed to be fully opaque, false otherwise.
     * @deprecated Use {@link View#isOpaque()} directly. This method will be
     * removed in a future release.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isOpaque()")
    @Deprecated
    public static boolean isOpaque(View view) {
        return view.isOpaque();
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec.  Will take the desired size, unless a different size
     * is imposed by the constraints.  The returned value is a compound integer,
     * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
     * size is smaller than the size the view wants to be.
     *
     * @param size How big the view wants to be
     * @param measureSpec Constraints imposed by the parent
     * @param childMeasuredState Size information bit mask for the view's children.
     * @return Size information bit mask as defined by
     * {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     *
     * @deprecated Use {@link View#resolveSizeAndState(int, int, int)} directly.
     */
    @Deprecated
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        return View.resolveSizeAndState(size, measureSpec, childMeasuredState);
    }

    /**
     * Return the full width measurement information for this view as computed
     * by the most recent call to {@link android.view.View#measure(int, int)}.
     * This result is a bit mask as defined by {@link #MEASURED_SIZE_MASK} and
     * {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link android.view.View#getWidth()} to see how wide a view is after layout.
     *
     * @return The measured width of this view as a bit mask.
     *
     * @deprecated Use {@link View#getMeasuredWidth()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMeasuredWidthAndState()")
    @Deprecated
    public static int getMeasuredWidthAndState(View view) {
        return view.getMeasuredWidthAndState();
    }

    /**
     * Return the full height measurement information for this view as computed
     * by the most recent call to {@link android.view.View#measure(int, int)}.
     * This result is a bit mask as defined by {@link #MEASURED_SIZE_MASK} and
     * {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link android.view.View#getHeight()} to see how wide a view is after layout.
     *
     * @return The measured width of this view as a bit mask.
     *
     * @deprecated Use {@link View#getMeasuredHeightAndState()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMeasuredHeightAndState()")
    @Deprecated
    public static int getMeasuredHeightAndState(View view) {
        return view.getMeasuredHeightAndState();
    }

    /**
     * Return only the state bits of {@link #getMeasuredWidthAndState}
     * and {@link #getMeasuredHeightAndState}, combined into one integer.
     * The width component is in the regular bits {@link #MEASURED_STATE_MASK}
     * and the height component is at the shifted bits
     * {@link #MEASURED_HEIGHT_STATE_SHIFT}>>{@link #MEASURED_STATE_MASK}.
     *
     * @deprecated Use {@link View#getMeasuredState()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMeasuredState()")
    @Deprecated
    public static int getMeasuredState(View view) {
        return view.getMeasuredState();
    }

    /**
     * Merge two states as returned by {@link #getMeasuredState(View)}.
     * @param curState The current state as returned from a view or the result
     * of combining multiple views.
     * @param newState The new view state to combine.
     * @return Returns a new integer reflecting the combination of the two
     * states.
     *
     * @deprecated Use {@link View#combineMeasuredStates(int, int)} directly.
     */
    @Deprecated
    public static int combineMeasuredStates(int curState, int newState) {
        return View.combineMeasuredStates(curState, newState);
    }

    /**
     * Gets the live region mode for the specified View.
     *
     * @param view The view from which to obtain the live region mode
     * @return The live region mode for the view.
     *
     * @see ViewCompat#setAccessibilityLiveRegion(View, int)
     * @deprecated Call {@link View#getAccessibilityLiveRegion()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getAccessibilityLiveRegion()")
    @Deprecated
    @AccessibilityLiveRegion
    public static int getAccessibilityLiveRegion(@NonNull View view) {
        return view.getAccessibilityLiveRegion();
    }

    /**
     * Sets the live region mode for this view. This indicates to accessibility
     * services whether they should automatically notify the user about changes
     * to the view's content description or text, or to the content descriptions
     * or text of the view's children (where applicable).
     * <p>
     * To indicate that the user should be notified of changes, use
     * {@link #ACCESSIBILITY_LIVE_REGION_POLITE}. Announcements from this region are queued and
     * do not disrupt ongoing speech.
     * <p>
     * For example, selecting an option in a dropdown menu may update a panel below with the updated
     * content. This panel may be marked as a live region with
     * {@link #ACCESSIBILITY_LIVE_REGION_POLITE} to notify users of the change.
     * <p>
     * For notifying users about errors, such as in a login screen with text that displays an
     * "incorrect password" notification, that view should send an AccessibilityEvent of type
     * {@link AccessibilityEvent#CONTENT_CHANGE_TYPE_ERROR} and set
     * {@link AccessibilityNodeInfo#setError(CharSequence)} instead. Custom widgets should expose
     * error-setting methods that support accessibility automatically. For example, instead of
     * explicitly sending this event when using a TextView, use
     * {@link android.widget.TextView#setError(CharSequence)}.
     * <p>
     * To disable change notifications for this view, use
     * {@link #ACCESSIBILITY_LIVE_REGION_NONE}. This is the default live region
     * mode for most views.
     * <p>
     * If the view's changes should interrupt ongoing speech and notify the user
     * immediately, use {@link #ACCESSIBILITY_LIVE_REGION_ASSERTIVE}. This may result in disruptive
     * announcements from an accessibility service, so it should generally be used only to convey
     * information that is time-sensitive or critical for use of the application. Examples may
     * include an incoming call or an emergency alert.
     *
     * @param view The view on which to set the live region mode
     * @param mode The live region mode for this view, one of:
     *        <ul>
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_NONE}
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_POLITE}
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_ASSERTIVE}
     *        </ul>
     * @deprecated Call {@link View#setAccessibilityLiveRegion(int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setAccessibilityLiveRegion(mode)")
    @Deprecated
    public static void setAccessibilityLiveRegion(@NonNull View view,
            @AccessibilityLiveRegion int mode) {
        view.setAccessibilityLiveRegion(mode);
    }

    /**
     * Returns the start padding of the specified view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @param view The view to get padding for
     * @return the start padding in pixels
     * @deprecated Call {@link View#getPaddingStart()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getPaddingStart()")
    @Deprecated
    @Px
    public static int getPaddingStart(@NonNull View view) {
        return view.getPaddingStart();
    }

    /**
     * Returns the end padding of the specified view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @param view The view to get padding for
     * @return the end padding in pixels
     * @deprecated Call {@link View#getPaddingEnd()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getPaddingEnd()")
    @Deprecated
    @Px
    public static int getPaddingEnd(@NonNull View view) {
        return view.getPaddingEnd();
    }

    /**
     * Sets the relative padding. The view may add on the space required to display
     * the scrollbars, depending on the style and visibility of the scrollbars.
     * So the values returned from {@link #getPaddingStart}, {@link View#getPaddingTop},
     * {@link #getPaddingEnd} and {@link View#getPaddingBottom} may be different
     * from the values set in this call.
     *
     * @param view The view on which to set relative padding
     * @param start the start padding in pixels
     * @param top the top padding in pixels
     * @param end the end padding in pixels
     * @param bottom the bottom padding in pixels
     * @deprecated Call {@link View#setPaddingRelative(int, int, int, int)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setPaddingRelative(start, top, end, bottom)")
    @Deprecated
    public static void setPaddingRelative(@NonNull View view, @Px int start, @Px int top,
            @Px int end, @Px int bottom) {
        view.setPaddingRelative(start, top, end, bottom);
    }

    private static void bindTempDetach() {
        try {
            sDispatchStartTemporaryDetach = View.class.getDeclaredMethod(
                    "dispatchStartTemporaryDetach");
            sDispatchFinishTemporaryDetach = View.class.getDeclaredMethod(
                    "dispatchFinishTemporaryDetach");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Couldn't find method", e);
        }
        sTempDetachBound = true;
    }

    /**
     * Notify a view that it is being temporarily detached.
     */
    public static void dispatchStartTemporaryDetach(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.dispatchStartTemporaryDetach(view);
        } else {
            if (!sTempDetachBound) {
                bindTempDetach();
            }
            if (sDispatchStartTemporaryDetach != null) {
                try {
                    sDispatchStartTemporaryDetach.invoke(view);
                } catch (Exception e) {
                    Log.d(TAG, "Error calling dispatchStartTemporaryDetach", e);
                }
            } else {
                // Try this instead
                view.onStartTemporaryDetach();
            }
        }
    }

    /**
     * Notify a view that its temporary detach has ended; the view is now reattached.
     */
    public static void dispatchFinishTemporaryDetach(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.dispatchFinishTemporaryDetach(view);
        } else {
            if (!sTempDetachBound) {
                bindTempDetach();
            }
            if (sDispatchFinishTemporaryDetach != null) {
                try {
                    sDispatchFinishTemporaryDetach.invoke(view);
                } catch (Exception e) {
                    Log.d(TAG, "Error calling dispatchFinishTemporaryDetach", e);
                }
            } else {
                // Try this instead
                view.onFinishTemporaryDetach();
            }
        }
    }

    /**
     * The horizontal location of this view relative to its {@link View#getLeft() left} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The horizontal position of this view relative to its left position, in pixels.
     *
     * @deprecated Use {@link View#getTranslationX()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getTranslationX()")
    @Deprecated
    public static float getTranslationX(View view) {
        return view.getTranslationX();
    }

    /**
     * The vertical location of this view relative to its {@link View#getTop() top} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The vertical position of this view relative to its top position, in pixels.
     *
     * @deprecated Use {@link View#getTranslationY()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getTranslationY()")
    @Deprecated
    public static float getTranslationY(View view) {
        return view.getTranslationY();
    }

    /**
     * The transform matrix of this view, which is calculated based on the current
     * rotation, scale, and pivot properties.
     * <p>
     *
     * @param view The view whose Matrix will be returned
     * @return The current transform matrix for the view
     *
     * @see #getRotation(View)
     * @see #getScaleX(View)
     * @see #getScaleY(View)
     * @see #getPivotX(View)
     * @see #getPivotY(View)
     *
     * @deprecated Use {@link View#getMatrix()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMatrix()")
    @Deprecated
    public static @Nullable Matrix getMatrix(View view) {
        return view.getMatrix();
    }

    /**
     * Returns the minimum width of the view.
     *
     * <p>Prior to API 16, this method may return 0 on some platforms.</p>
     *
     * @return the minimum width the view will try to be.
     * @deprecated Call {@link View#getMinimumWidth()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMinimumWidth()")
    @Deprecated
    @SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
    // Reflective access to private field, unboxing result of reflective get()
    public static int getMinimumWidth(@NonNull View view) {
        return view.getMinimumWidth();
    }

    /**
     * Returns the minimum height of the view.
     *
     * <p>Prior to API 16, this method may return 0 on some platforms.</p>
     *
     * @return the minimum height the view will try to be.
     * @deprecated Call {@link View#getMinimumHeight()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getMinimumHeight()")
    @Deprecated
    @SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
    // Reflective access to private field, unboxing result of reflective get()
    public static int getMinimumHeight(@NonNull View view) {
        return view.getMinimumHeight();
    }

    /**
     * This method returns a ViewPropertyAnimator object, which can be used to animate
     * specific properties on this View.
     *
     * @return ViewPropertyAnimator The ViewPropertyAnimator associated with this View.
     * @deprecated Call {@link View#animate()} directly.
     */
    @Deprecated
    public static @NonNull ViewPropertyAnimatorCompat animate(@NonNull View view) {
        if (sViewPropertyAnimatorMap == null) {
            sViewPropertyAnimatorMap = new WeakHashMap<>();
        }
        ViewPropertyAnimatorCompat vpa = sViewPropertyAnimatorMap.get(view);
        if (vpa == null) {
            vpa = new ViewPropertyAnimatorCompat(view);
            sViewPropertyAnimatorMap.put(view, vpa);
        }
        return vpa;
    }

    /**
     * Sets the horizontal location of this view relative to its left position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @param view view for which to set the translation.
     * @param value The horizontal position of this view relative to its left position,
     * in pixels.
     *
     * @deprecated Use {@link View#setTranslationX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setTranslationX(value)")
    @Deprecated
    public static void setTranslationX(View view, float value) {
        view.setTranslationX(value);
    }

    /**
     * Sets the vertical location of this view relative to its top position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @param view view for which to set the translation.
     * @param value The vertical position of this view relative to its top position,
     * in pixels.
     *
     * @attr name android:translationY
     *
     * @deprecated Use {@link View#setTranslationY(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setTranslationY(value)")
    @Deprecated
    public static void setTranslationY(View view, float value) {
        view.setTranslationY(value);
    }

    /**
     * <p>Sets the opacity of the view. This is a value from 0 to 1, where 0 means the view is
     * completely transparent and 1 means the view is completely opaque.</p>
     *
     * <p> Note that setting alpha to a translucent value (0 < alpha < 1) can have significant
     * performance implications, especially for large views. It is best to use the alpha property
     * sparingly and transiently, as in the case of fading animations.</p>
     *
     * @param view view to set the alpha on.
     * @param value The opacity of the view.
     *
     * @deprecated Use {@link View#setAlpha(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setAlpha(value)")
    @Deprecated
    public static void setAlpha(View view, @FloatRange(from = 0.0, to = 1.0) float value) {
        view.setAlpha(value);
    }

    /**
     * Sets the visual x position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationX(View, float) translationX} property to be the difference between
     * the x value passed in and the current left property of the view as determined
     * by the layout bounds.
     *
     * @param view view to set the position on.
     * @param value The visual x position of this view, in pixels.
     *
     * @deprecated Use {@link View#setX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setX(value)")
    @Deprecated
    public static void setX(View view, float value) {
        view.setX(value);
    }

    /**
     * Sets the visual y position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationY(View, float) translationY} property to be the difference between
     * the y value passed in and the current top property of the view as determined by the
     * layout bounds.
     *
     * @param view view to set the position on.
     * @param value The visual y position of this view, in pixels.
     *
     * @deprecated Use {@link View#setY(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setY(value)")
    @Deprecated
    public static void setY(View view, float value) {
        view.setY(value);
    }

    /**
     * Sets the degrees that the view is rotated around the pivot point. Increasing values
     * result in clockwise rotation.
     *
     * @param view view to set the rotation on.
     * @param value The degrees of rotation.
     *
     * @deprecated Use {@link View#setRotation(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setRotation(value)")
    @Deprecated
    public static void setRotation(View view, float value) {
        view.setRotation(value);
    }

    /**
     * Sets the degrees that the view is rotated around the horizontal axis through the pivot point.
     * Increasing values result in clockwise rotation from the viewpoint of looking down the
     * x axis.
     *
     * @param view view to set the rotation on.
     * @param value The degrees of X rotation.
     *
     * @deprecated Use {@link View#setRotationX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setRotationX(value)")
    @Deprecated
    public static void setRotationX(View view, float value) {
        view.setRotationX(value);
    }

    /**
     * Sets the degrees that the view is rotated around the vertical axis through the pivot point.
     * Increasing values result in counter-clockwise rotation from the viewpoint of looking
     * down the y axis.
     *
     * @param view view to set the rotation on.
     * @param value The degrees of Y rotation.
     *
     * @deprecated Use {@link View#setRotationY(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setRotationY(value)")
    @Deprecated
    public static void setRotationY(View view, float value) {
        view.setRotationY(value);
    }

    /**
     * Sets the amount that the view is scaled in x around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param view view to set the scale on.
     * @param value The scaling factor.
     *
     * @deprecated Use {@link View#setScaleX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setScaleX(value)")
    @Deprecated
    public static void setScaleX(View view, float value) {
        view.setScaleX(value);
    }

    /**
     * Sets the amount that the view is scaled in Y around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param view view to set the scale on.
     * @param value The scaling factor.
     *
     * @deprecated Use {@link View#setScaleY(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setScaleY(value)")
    @Deprecated
    public static void setScaleY(View view, float value) {
        view.setScaleY(value);
    }

    /**
     * The x location of the point around which the view is
     * {@link #setRotation(View, float) rotated} and {@link #setScaleX(View, float) scaled}.
     *
     * @param view view for which to get the pivot.
     * @deprecated Use {@link View#getPivotX()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getPivotX()")
    @Deprecated
    public static float getPivotX(View view) {
        return view.getPivotX();
    }

    /**
     * Sets the x location of the point around which the view is
     * {@link #setRotation(View, float) rotated} and {@link #setScaleX(View, float) scaled}.
     * By default, the pivot point is centered on the object.
     * Setting this property disables this behavior and causes the view to use only the
     * explicitly set pivotX and pivotY values.
     *
     * @param view view for which to set the pivot.
     * @param value The x location of the pivot point.
     *
     * @deprecated Use {@link View#setPivotX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setPivotX(value)")
    @Deprecated
    public static void setPivotX(View view, float value) {
        view.setPivotX(value);
    }

    /**
     * The y location of the point around which the view is {@link #setRotation(View,
     * float) rotated} and {@link #setScaleY(View, float) scaled}.
     *
     * @param view view for which to get the pivot.
     * @return The y location of the pivot point.
     *
     * @deprecated Use {@link View#getPivotY()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getPivotY()")
    @Deprecated
    public static float getPivotY(View view) {
        return view.getPivotY();
    }

    /**
     * Sets the y location of the point around which the view is
     * {@link #setRotation(View, float) rotated} and {@link #setScaleY(View, float) scaled}.
     * By default, the pivot point is centered on the object.
     * Setting this property disables this behavior and causes the view to use only the
     * explicitly set pivotX and pivotY values.
     *
     * @param view view for which to set the pivot.
     * @param value The y location of the pivot point.
     *
     * @deprecated Use {@link View#setPivotX(float)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setPivotY(value)")
    @Deprecated
    public static void setPivotY(View view, float value) {
        view.setPivotY(value);
    }

    /**
     * @param view view for which to get the rotation.
     * @deprecated Use {@link View#getRotation()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getRotation()")
    @Deprecated
    public static float getRotation(View view) {
        return view.getRotation();
    }

    /**
     * @param view view for which to get the rotation.
     * @deprecated Use {@link View#getRotationX()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getRotationX()")
    @Deprecated
    public static float getRotationX(View view) {
        return view.getRotationX();
    }

    /**
     * @param view view for which to get the rotation.
     * @deprecated Use {@link View#getRotationY()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getRotationY()")
    @Deprecated
    public static float getRotationY(View view) {
        return view.getRotationY();
    }

    /**
     * @param view view for which to get the scale.
     * @deprecated Use {@link View#getScaleX()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getScaleX()")
    @Deprecated
    public static float getScaleX(View view) {
        return view.getScaleX();
    }

    /**
     * @param view view for which to get the scale.
     * @deprecated Use {@link View#getScaleY()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getScaleY()")
    @Deprecated
    public static float getScaleY(View view) {
        return view.getScaleY();
    }

    /**
     * @param view view for which to get the X.
     * @deprecated Use {@link View#getX()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getX()")
    @Deprecated
    public static float getX(View view) {
        return view.getX();
    }

    /**
     * @param view view for which to get the Y.
     * @deprecated Use {@link View#getY()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getY()")
    @Deprecated
    public static float getY(View view) {
        return view.getY();
    }

    /**
     * @param view view for which to set the elevation.
     * @param elevation view elevation in pixels.
     * Sets the base elevation of this view, in pixels.
     */
    public static void setElevation(@NonNull View view, float elevation) {
        view.setElevation(elevation);
    }

    /**
     * The base elevation of this view relative to its parent, in pixels.
     *
     * @param view view for which to get the elevation.
     * @return The base depth position of the view, in pixels.
     */
    public static float getElevation(@NonNull View view) {
        return view.getElevation();
    }

    /**
     * Sets the depth location of this view relative to its {@link #getElevation(View) elevation}.
     * @param view view for which to set the translation.
     * @param translationZ the depth of location of this view relative its elevation.
     */
    public static void setTranslationZ(@NonNull View view, float translationZ) {
        view.setTranslationZ(translationZ);
    }

    /**
     * The depth location of this view relative to its {@link #getElevation(View) elevation}.
     *
     * @param view view for which to get the translation.
     * @return The depth of this view relative to its elevation.
     */
    public static float getTranslationZ(@NonNull View view) {
        return view.getTranslationZ();
    }

    /**
     * Sets the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * @param view The View against which to invoke the method.
     * @param transitionName The name of the View to uniquely identify it for Transitions.
     */
    public static void setTransitionName(@NonNull View view, @Nullable String transitionName) {
        view.setTransitionName(transitionName);
    }

    /**
     * Returns the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * <p>This returns null if the View has not been given a name.</p>
     *
     * @param view The View against which to invoke the method.
     * @return The name used of the View to be used to identify Views in Transitions or null
     * if no name has been given.
     */
    public static @Nullable String getTransitionName(@NonNull View view) {
        return view.getTransitionName();
    }

    /**
     * Convenience method to add {@code overlay} to {@code overlayHost}'s
     * {@link View#getOverlay() overlay} and assign the
     * {@link ViewTree#setViewTreeDisjointParent(View, ViewParent) disjointParent} in the
     * overlay hierarchy.
     *
     * @param overlayHost The view to add an overlay to
     * @param overlay The view to overlay onto {@code overlayHost}
     * @see android.view.ViewGroupOverlay#add(View)
     */
    public static void addOverlayView(@NonNull ViewGroup overlayHost, @NonNull View overlay) {
        overlayHost.getOverlay().add(overlay);
        ViewTree.setViewTreeDisjointParent((View) overlay.getParent(), overlayHost);
    }

    /**
     * Returns the current system UI visibility that is currently set for the entire window.
     *
     * @param view view for which to get the visibility.
     *
     * @deprecated SystemUiVisibility flags are deprecated. Use
     * {@link WindowInsetsController} instead.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getWindowSystemUiVisibility()")
    @Deprecated
    public static int getWindowSystemUiVisibility(@NonNull View view) {
        return view.getWindowSystemUiVisibility();
    }

    /**
     * Ask that a new dispatch of {@code View.onApplyWindowInsets(WindowInsets)} be performed. This
     * falls back to {@code View.requestFitSystemWindows()} where available.
     *
     * @param view view for which to send the request.
     */
    public static void requestApplyInsets(@NonNull View view) {
        view.requestApplyInsets();
    }

    /**
     * Tells the ViewGroup whether to draw its children in the order defined by the method
     * {@code ViewGroup.getChildDrawingOrder(int, int)}.
     *
     * @param viewGroup the ViewGroup for which to set the mode.
     * @param enabled true if the order of the children when drawing is determined by
     *        {@link ViewGroup#getChildDrawingOrder(int, int)}, false otherwise
     *
     * <p>Prior to API 7 this will have no effect.</p>
     *
     * @deprecated Use {@link ViewGroup#setChildrenDrawingOrderEnabled(boolean)} directly.
     */
    @SuppressLint("BanUncheckedReflection") // Reflective access to bypass Java visibility
    @Deprecated
    public static void setChildrenDrawingOrderEnabled(ViewGroup viewGroup, boolean enabled) {
        if (sChildrenDrawingOrderMethod == null) {
            try {
                sChildrenDrawingOrderMethod = ViewGroup.class
                        .getDeclaredMethod("setChildrenDrawingOrderEnabled", boolean.class);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Unable to find childrenDrawingOrderEnabled", e);
            }
            sChildrenDrawingOrderMethod.setAccessible(true);
        }
        try {
            sChildrenDrawingOrderMethod.invoke(viewGroup, enabled);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to invoke childrenDrawingOrderEnabled", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to invoke childrenDrawingOrderEnabled", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Unable to invoke childrenDrawingOrderEnabled", e);
        }
    }

    /**
     * Returns true if this view should adapt to fit system window insets. This method will always
     * return false before API 16 (Jellybean).
     *
     * @param view view for which to get the state.
     * @deprecated Call {@link View#getFitsSystemWindows()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getFitsSystemWindows()")
    @Deprecated
    public static boolean getFitsSystemWindows(@NonNull View view) {
        return view.getFitsSystemWindows();
    }

    /**
     * Sets whether or not this view should account for system screen decorations
     * such as the status bar and inset its content; that is, controlling whether
     * the default implementation of {@link View#fitSystemWindows(Rect)} will be
     * executed. See that method for more details.
     *
     * @param view view for which to set the state.
     * @param fitSystemWindows whether or not this view should account for system screen
     *                         decorations.
     *
     * @deprecated Use {@link View#setFitsSystemWindows(boolean)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setFitsSystemWindows(fitSystemWindows)")
    @Deprecated
    public static void setFitsSystemWindows(View view, boolean fitSystemWindows) {
        view.setFitsSystemWindows(fitSystemWindows);
    }

    /**
     * On API 11 devices and above, call <code>Drawable.jumpToCurrentState()</code>
     * on all Drawable objects associated with this view.
     * <p>
     * On API 21 and above, also calls <code>StateListAnimator#jumpToCurrentState()</code>
     * if there is a StateListAnimator attached to this view.
     *
     * @param view view for which to jump the drawable state.
     * @deprecated Use {@link View#jumpDrawablesToCurrentState()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.jumpDrawablesToCurrentState()")
    @Deprecated
    public static void jumpDrawablesToCurrentState(View view) {
        view.jumpDrawablesToCurrentState();
    }

    /**
     * Set an {@link OnApplyWindowInsetsListener} to take over the policy for applying
     * window insets to this view. This will only take effect on devices with API 21 or above.
     *
     * @param view view on which to the listener.
     * @param listener listener for the applied window insets.
     */
    public static void setOnApplyWindowInsetsListener(final @NonNull View view,
            final @Nullable OnApplyWindowInsetsListener listener) {
        Api21Impl.setOnApplyWindowInsetsListener(view, listener);
    }

    /**
     * Called when the view should apply {@link WindowInsetsCompat} according to its internal policy.
     *
     * <p>Clients may supply an {@link OnApplyWindowInsetsListener} to a view. If one is set
     * it will be called during dispatch instead of this method. The listener may optionally
     * call this method from its own implementation if it wishes to apply the view's default
     * insets policy in addition to its own.</p>
     *
     * @param view The View against which to invoke the method.
     * @param insets Insets to apply
     * @return The supplied insets with any applied insets consumed
     */
    public static @NonNull WindowInsetsCompat onApplyWindowInsets(@NonNull View view,
            @NonNull WindowInsetsCompat insets) {
        final WindowInsets unwrapped = insets.toWindowInsets();
        if (unwrapped != null) {
            WindowInsets result = view.onApplyWindowInsets(unwrapped);
            if (!result.equals(unwrapped)) {
                // If the value changed, return a newly wrapped instance
                return WindowInsetsCompat.toWindowInsetsCompat(result, view);
            }
        }
        return insets;
    }

    /**
     * Request to apply the given window insets to this view or another view in its subtree.
     *
     * <p>This method should be called by clients wishing to apply insets corresponding to areas
     * obscured by window decorations or overlays. This can include the status and navigation bars,
     * action bars, input methods and more. New inset categories may be added in the future.
     * The method returns the insets provided minus any that were applied by this view or its
     * children.</p>
     *
     * @param view view for which to dispatch the request.
     * @param insets Insets to apply
     * @return The provided insets minus the insets that were consumed
     */
    public static @NonNull WindowInsetsCompat dispatchApplyWindowInsets(@NonNull View view,
            @NonNull WindowInsetsCompat insets) {
        final WindowInsets unwrapped = insets.toWindowInsets();
        if (unwrapped != null) {
            final WindowInsets result = Build.VERSION.SDK_INT >= 30
                    ? Api30Impl.dispatchApplyWindowInsets(view, unwrapped)
                    : Api20Impl.dispatchApplyWindowInsets(view, unwrapped);
            if (!result.equals(unwrapped)) {
                // If the value changed, return a newly wrapped instance
                return WindowInsetsCompat.toWindowInsetsCompat(result, view);
            }
        }
        return insets;
    }

    /**
     * Sets a list of areas within this view's post-layout coordinate space where the system
     * should not intercept touch or other pointing device gestures. <em>This method should
     * be called by {@link View#onLayout(boolean, int, int, int, int)} or
     * {@link View#onDraw(Canvas)}.</em>
     * <p>
     * On devices running API 28 and below, this method has no effect.
     *
     * @param view view for which to set the exclusion rects.
     * @param rects A list of precision gesture regions that this view needs to function correctly
     * @see View#setSystemGestureExclusionRects
     */
    public static void setSystemGestureExclusionRects(@NonNull View view,
            @NonNull List<Rect> rects) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.setSystemGestureExclusionRects(view, rects);
        }
    }

    /**
     * Retrieve the list of areas within this view's post-layout coordinate space where the system
     * should not intercept touch or other pointing device gestures.
     * <p>
     * On devices running API 28 and below, this method always returns an empty list.
     *
     * @param view view for which to get the exclusion rects.
     *
     * @see View#getSystemGestureExclusionRects
     */
    public static @NonNull List<Rect> getSystemGestureExclusionRects(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.getSystemGestureExclusionRects(view);
        }
        return Collections.emptyList();
    }

    /**
     * Provide original {@link WindowInsetsCompat} that are dispatched to the view hierarchy.
     * The insets are only available if the view is attached.
     * <p>
     * On devices running API 20 and below, this method always returns null.
     *
     * @return WindowInsetsCompat from the top of the view hierarchy or null if View is detached
     */
    public static @Nullable WindowInsetsCompat getRootWindowInsets(@NonNull View view) {
        return Api23Impl.getRootWindowInsets(view);
    }

    /**
     * Compute insets that should be consumed by this view and the ones that should propagate
     * to those under it.
     *
     * @param view view for which insets need to be computed.
     * @param insets Insets currently being processed by this View, likely received as a parameter
     *           to {@link View#onApplyWindowInsets(WindowInsets)}.
     * @param outLocalInsets A Rect that will receive the insets that should be consumed
     *                       by this view
     * @return Insets that should be passed along to views under this one
     */
    public static @NonNull WindowInsetsCompat computeSystemWindowInsets(@NonNull View view,
            @NonNull WindowInsetsCompat insets, @NonNull Rect outLocalInsets) {
        return Api21Impl.computeSystemWindowInsets(view, insets, outLocalInsets);
    }

    /**
     * Retrieves a {@link WindowInsetsControllerCompat} of the window this view is attached to.
     *
     * @return A {@link WindowInsetsControllerCompat} or {@code null} if the view is neither
     * attached to a window nor a view tree with a decor.
     * @see WindowCompat#getInsetsController(Window, View)
     * @deprecated Prefer {@link WindowCompat#getInsetsController(Window, View)} to explicitly
     * specify the window (such as when the view is in a dialog).
     */
    @Deprecated
    public static @Nullable WindowInsetsControllerCompat getWindowInsetsController(
            @NonNull View view) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Api30Impl.getWindowInsetsController(view);
        } else {
            Context context = view.getContext();
            while (context instanceof ContextWrapper) {
                if (context instanceof Activity) {
                    Window window = ((Activity) context).getWindow();
                    return window != null ? WindowCompat.getInsetsController(window, view) : null;
                }
                context = ((ContextWrapper) context).getBaseContext();
            }
            return null;
        }
    }

    /**
     * Sets a {@link WindowInsetsAnimationCompat.Callback} to be notified about animations of
     * windows that cause insets.
     * <p>
     * The callback's {@link WindowInsetsAnimationCompat.Callback#getDispatchMode()
     * dispatch mode} will affect whether animation callbacks are dispatched to the children of
     * this view.
     * <p>
     * Prior to API 30, if an {@link OnApplyWindowInsetsListener} is used on the same
     * view, be sure to always use the {@link ViewCompat} version of
     * {@link #setOnApplyWindowInsetsListener(View, OnApplyWindowInsetsListener)}, otherwise the
     * listener will be overridden by this method.
     * <p>
     * The insets dispatch needs to reach this view for the listener to be called. If any view
     * consumed the insets earlier in the dispatch, this won't be called.
     * <p>
     * Prior to API 21, this method has no effect.
     *
     * @param view view for which to set the callback.
     * @param callback The callback to set, or <code>null</code> to remove the currently installed
     *                 callback
     */
    public static void setWindowInsetsAnimationCallback(@NonNull View view,
            final WindowInsetsAnimationCompat.@Nullable Callback callback) {
        WindowInsetsAnimationCompat.setCallback(view, callback);
    }

    /**
     * Sets the listener to be used to handle insertion of content into the given view.
     *
     * <p>Depending on the type of view, this listener may be invoked for different scenarios. For
     * example, for an {@code AppCompatEditText}, this listener will be invoked for the following
     * scenarios:
     * <ol>
     *     <li>Paste from the clipboard (e.g. "Paste" or "Paste as plain text" action in the
     *     insertion/selection menu)</li>
     *     <li>Content insertion from the keyboard (from {@link InputConnection#commitContent})</li>
     *     <li>Drag and drop (drop events from {@link View#onDragEvent})</li>
     * </ol>
     *
     * <p>When setting a listener, clients must also declare the accepted MIME types.
     * The listener will still be invoked even if the MIME type of the content is not one of the
     * declared MIME types (e.g. if the user pastes content whose type is not one of the declared
     * MIME types).
     * In that case, the listener may reject the content (defer to the default platform behavior)
     * or execute some other fallback logic (e.g. show an appropriate message to the user).
     * The declared MIME types serve as a hint to allow different features to optionally alter
     * their behavior. For example, a soft keyboard may optionally choose to hide its UI for
     * inserting GIFs for a particular input field if the MIME types set here for that field
     * don't include "image/gif" or "image/*".
     *
     * <p>Note: MIME type matching in the Android framework is case-sensitive, unlike formal RFC
     * MIME types. As a result, you should always write your MIME types with lowercase letters,
     * or use {@link android.content.Intent#normalizeMimeType} to ensure that it is converted to
     * lowercase.
     *
     * @param view The target view.
     * @param mimeTypes The MIME types accepted by the given listener. These may use patterns
     *                  such as "image/*", but may not start with a wildcard. This argument must
     *                  not be null or empty if a non-null listener is passed in.
     * @param listener The listener to use. This can be null to reset to the default behavior.
     */
    public static void setOnReceiveContentListener(@NonNull View view,
            String @Nullable [] mimeTypes, @Nullable OnReceiveContentListener listener) {
        if (Build.VERSION.SDK_INT >= 31) {
            Api31Impl.setOnReceiveContentListener(view, mimeTypes, listener);
            return;
        }
        mimeTypes = (mimeTypes == null || mimeTypes.length == 0) ? null : mimeTypes;
        if (listener != null) {
            Preconditions.checkArgument(mimeTypes != null,
                    "When the listener is set, MIME types must also be set");
        }
        if (mimeTypes != null) {
            boolean hasLeadingWildcard = false;
            for (String mimeType : mimeTypes) {
                if (mimeType.startsWith("*")) {
                    hasLeadingWildcard = true;
                    break;
                }
            }
            Preconditions.checkArgument(!hasLeadingWildcard,
                    "A MIME type set here must not start with *: " + Arrays.toString(mimeTypes));
        }
        view.setTag(R.id.tag_on_receive_content_mime_types, mimeTypes);
        view.setTag(R.id.tag_on_receive_content_listener, listener);
    }

    /**
     * Returns the MIME types accepted by the listener configured on the given view via
     * {@link #setOnReceiveContentListener}. By default returns null.
     *
     * <p>Different features (e.g. pasting from the clipboard, inserting stickers from the soft
     * keyboard, etc) may optionally use this metadata to conditionally alter their behavior. For
     * example, a soft keyboard may choose to hide its UI for inserting GIFs for a particular
     * input field if the MIME types returned here for that field don't include "image/gif" or
     * "image/*".
     *
     * <p>Note: Comparisons of MIME types should be performed using utilities such as
     * {@link ClipDescription#compareMimeTypes} rather than simple string equality, in order to
     * correctly handle patterns such as "text/*", "image/*", etc. Note that MIME type matching
     * in the Android framework is case-sensitive, unlike formal RFC MIME types. As a result,
     * you should always write your MIME types with lowercase letters, or use
     * {@link android.content.Intent#normalizeMimeType} to ensure that it is converted to
     * lowercase.
     *
     * @param view The target view.
     *
     * @return The MIME types accepted by the {@link OnReceiveContentListener} for the given view
     * (may include patterns such as "image/*").
     */
    public static String @Nullable [] getOnReceiveContentMimeTypes(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 31) {
            return Api31Impl.getReceiveContentMimeTypes(view);
        }
        return (String[]) view.getTag(R.id.tag_on_receive_content_mime_types);
    }

    /**
     * Receives the given content.
     *
     * <p>If a listener is set, invokes the listener. If the listener returns a non-null result,
     * executes the fallback handling for the portion of the content returned by the listener.
     *
     * <p>If no listener is set, executes the fallback handling.
     *
     * <p>The fallback handling is defined by the target view if the view implements
     * {@link OnReceiveContentViewBehavior}, or is simply a no-op.
     *
     * @param view The target view.
     * @param payload The content to insert and related metadata.
     *
     * @return The portion of the passed-in content that was not handled (may be all, some, or none
     * of the passed-in content).
     */
    public static @Nullable ContentInfoCompat performReceiveContent(@NonNull View view,
            @NonNull ContentInfoCompat payload) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "performReceiveContent: " + payload
                    + ", view=" + view.getClass().getSimpleName() + "[" + view.getId() + "]");
        }
        if (Build.VERSION.SDK_INT >= 31) {
            return Api31Impl.performReceiveContent(view, payload);
        }
        OnReceiveContentListener listener =
                (OnReceiveContentListener) view.getTag(R.id.tag_on_receive_content_listener);
        if (listener != null) {
            ContentInfoCompat remaining = listener.onReceiveContent(view, payload);
            return (remaining == null) ? null : getFallback(view).onReceiveContent(remaining);
        }
        return getFallback(view).onReceiveContent(payload);
    }

    private static OnReceiveContentViewBehavior getFallback(@NonNull View view) {
        if (view instanceof OnReceiveContentViewBehavior) {
            return ((OnReceiveContentViewBehavior) view);
        }
        return NO_OP_ON_RECEIVE_CONTENT_VIEW_BEHAVIOR;
    }

    private static final OnReceiveContentViewBehavior NO_OP_ON_RECEIVE_CONTENT_VIEW_BEHAVIOR =
            payload -> payload;

    @RequiresApi(31)
    private static final class Api31Impl {
        private Api31Impl() {}

        public static void setOnReceiveContentListener(@NonNull View view,
                String @Nullable [] mimeTypes, final @Nullable OnReceiveContentListener listener) {
            if (listener == null) {
                view.setOnReceiveContentListener(mimeTypes, null);
            } else {
                view.setOnReceiveContentListener(mimeTypes,
                        new OnReceiveContentListenerAdapter(listener));
            }
        }

        public static String @Nullable [] getReceiveContentMimeTypes(@NonNull View view) {
            return view.getReceiveContentMimeTypes();
        }

        public static @Nullable ContentInfoCompat performReceiveContent(@NonNull View view,
                @NonNull ContentInfoCompat payload) {
            ContentInfo platPayload = payload.toContentInfo();
            ContentInfo platResult = view.performReceiveContent(platPayload);
            if (platResult == null) {
                return null;
            }
            if (platResult == platPayload) {
                // Avoid unnecessary conversion when returning the original payload unchanged.
                return payload;
            }
            return ContentInfoCompat.toContentInfoCompat(platResult);
        }
    }

    @RequiresApi(31)
    private static final class OnReceiveContentListenerAdapter implements
            android.view.OnReceiveContentListener {

        private final @NonNull OnReceiveContentListener mJetpackListener;

        OnReceiveContentListenerAdapter(@NonNull OnReceiveContentListener jetpackListener) {
            mJetpackListener = jetpackListener;
        }

        @Override
        public @Nullable ContentInfo onReceiveContent(@NonNull View view,
                @NonNull ContentInfo platPayload) {
            ContentInfoCompat payload = ContentInfoCompat.toContentInfoCompat(platPayload);
            ContentInfoCompat result = mJetpackListener.onReceiveContent(view, payload);
            if (result == null) {
                return null;
            }
            if (result == payload) {
                // Avoid unnecessary conversion when returning the original payload unchanged.
                return platPayload;
            }
            return result.toContentInfo();
        }
    }

    /**
     * Controls whether the entire hierarchy under this view will save its
     * state when a state saving traversal occurs from its parent.
     *
     * @param view view for which to set the state.
     * @param enabled Set to false to <em>disable</em> state saving, or true
     * (the default) to allow it.
     *
     * @deprecated Use {@link View#setSaveFromParentEnabled(boolean)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setSaveFromParentEnabled(enabled)")
    @Deprecated
    public static void setSaveFromParentEnabled(View view, boolean enabled) {
        view.setSaveFromParentEnabled(enabled);
    }

    /**
     * Changes the activated state of this view. A view can be activated or not.
     * Note that activation is not the same as selection.  Selection is
     * a transient property, representing the view (hierarchy) the user is
     * currently interacting with.  Activation is a longer-term state that the
     * user can move views in and out of.
     *
     * @param view view for which to set the state.
     * @param activated true if the view must be activated, false otherwise
     *
     * @deprecated Use {@link View#setActivated(boolean)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setActivated(activated)")
    @Deprecated
    public static void setActivated(View view, boolean activated) {
        view.setActivated(activated);
    }

    /**
     * Returns whether this View has content which overlaps.
     *
     * <p>This function, intended to be overridden by specific View types, is an optimization when
     * alpha is set on a view. If rendering overlaps in a view with alpha < 1, that view is drawn to
     * an offscreen buffer and then composited into place, which can be expensive. If the view has
     * no overlapping rendering, the view can draw each primitive with the appropriate alpha value
     * directly. An example of overlapping rendering is a TextView with a background image, such as
     * a Button. An example of non-overlapping rendering is a TextView with no background, or an
     * ImageView with only the foreground image. The default implementation returns true; subclasses
     * should override if they have cases which can be optimized.</p>
     *
     * @param view view for which to get the state.
     * @return true if the content in this view might overlap, false otherwise.
     * @deprecated Call {@link View#hasOverlappingRendering()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.hasOverlappingRendering()")
    @Deprecated
    public static boolean hasOverlappingRendering(@NonNull View view) {
        return view.hasOverlappingRendering();
    }

    /**
     * Return if the padding as been set through relative values
     * {@code View.setPaddingRelative(int, int, int, int)} or thru
     *
     * @param view view for which to get the state.
     * @return true if the padding is relative or false if it is not.
     * @deprecated Call {@link View#isPaddingRelative()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isPaddingRelative()")
    @Deprecated
    public static boolean isPaddingRelative(@NonNull View view) {
        return view.isPaddingRelative();
    }

    /**
     * Set the background of the {@code view} to a given Drawable, or remove the background. If the
     * background has padding, {@code view}'s padding is set to the background's padding. However,
     * when a background is removed, this View's padding isn't touched. If setting the padding is
     * desired, please use {@code setPadding(int, int, int, int)}.
     * @param view view for which to set the background.
     * @param background the drawable to use as view background.
     * @deprecated Call {@link View#setBackground(Drawable)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setBackground(background)")
    @Deprecated
    public static void setBackground(@NonNull View view, @Nullable Drawable background) {
        view.setBackground(background);
    }

    /**
     * Return the tint applied to the background drawable, if specified.
     * <p>
     * Only returns meaningful info when running on API v21 or newer, or if {@code view}
     * implements the {@code TintableBackgroundView} interface.
     */
    public static @Nullable ColorStateList getBackgroundTintList(@NonNull View view) {
        return view.getBackgroundTintList();
    }

    /**
     * Applies a tint to the background drawable.
     */
    public static void setBackgroundTintList(@NonNull View view,
            @Nullable ColorStateList tintList) {
        view.setBackgroundTintList(tintList);
    }

    /**
     * Return the blending mode used to apply the tint to the background
     * drawable, if specified.
     * <p>
     * Only returns meaningful info when running on API v21 or newer, or if {@code view}
     * implements the {@code TintableBackgroundView} interface.
     */
    public static PorterDuff.@Nullable Mode getBackgroundTintMode(@NonNull View view) {
        return view.getBackgroundTintMode();
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setBackgroundTintList(android.view.View, android.content.res.ColorStateList)} to
     * the background drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     */
    public static void setBackgroundTintMode(@NonNull View view, PorterDuff.@Nullable Mode mode) {
        view.setBackgroundTintMode(mode);
    }

    // TODO: getters for various view properties (rotation, etc)

    /**
     * Enable or disable nested scrolling for this view.
     *
     * <p>If this property is set to true the view will be permitted to initiate nested
     * scrolling operations with a compatible parent view in the current hierarchy. If this
     * view does not implement nested scrolling this will have no effect. Disabling nested scrolling
     * while a nested scroll is in progress has the effect of
     * {@link #stopNestedScroll(View) stopping} the nested scroll.</p>
     *
     * @param view view for which to set the state.
     * @param enabled true to enable nested scrolling, false to disable
     *
     * @see #isNestedScrollingEnabled(View)
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static void setNestedScrollingEnabled(@NonNull View view, boolean enabled) {
        view.setNestedScrollingEnabled(enabled);
    }

    /**
     * Returns true if nested scrolling is enabled for this view.
     *
     * <p>If nested scrolling is enabled and this View class implementation supports it,
     * this view will act as a nested scrolling child view when applicable, forwarding data
     * about the scroll operation in progress to a compatible and cooperating nested scrolling
     * parent.</p>
     *
     * @return true if nested scrolling is enabled
     *
     * @see #setNestedScrollingEnabled(View, boolean)
     */
    public static boolean isNestedScrollingEnabled(@NonNull View view) {
        return view.isNestedScrollingEnabled();
    }

    /**
     * Begin a nestable scroll operation along the given axes.
     *
     * <p>This version of the method just calls {@link #startNestedScroll(View, int, int)} using
     * the touch input type.</p>
     *
     * @param view view for which to start the scroll.
     * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
     *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     *         the current gesture.
     */
    public static boolean startNestedScroll(@NonNull View view, @ScrollAxis int axes) {
        return view.startNestedScroll(axes);
    }

    /**
     * Stop a nested scroll in progress.
     *
     * <p>This version of the method just calls {@link #stopNestedScroll(View, int)} using the
     * touch input type.</p>
     *
     * @param view view for which to stop the scroll.
     *
     * @see #startNestedScroll(View, int)
     */
    public static void stopNestedScroll(@NonNull View view) {
        view.stopNestedScroll();
    }

    /**
     * Returns true if this view has a nested scrolling parent.
     *
     * <p>This version of the method just calls {@link #hasNestedScrollingParent(View, int)}
     * using the touch input type.</p>
     *
     * @param view view for which to check the parent.
     * @return whether this view has a nested scrolling parent
     */
    public static boolean hasNestedScrollingParent(@NonNull View view) {
        return view.hasNestedScrollingParent();
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>This version of the method just calls
     * {@link #dispatchNestedScroll(View, int, int, int, int, int[], int)} using the touch input
     * type.</p>
     *
     * @param view view for which to dispatch the scroll.
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @return true if the event was dispatched, false if it could not be dispatched.
     */
    public static boolean dispatchNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int @Nullable [] offsetInWindow) {
        return view.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, offsetInWindow);
    }

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>This version of the method just calls
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[], int)} using the touch input
     * type.</p>
     *
     * @param view view for which to dispatch the scroll.
     * @param dx Horizontal scroll distance in pixels
     * @param dy Vertical scroll distance in pixels
     * @param consumed Output. If not null, consumed[0] will contain the consumed component of dx
     *                 and consumed[1] the consumed dy.
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @return true if the parent consumed some or all of the scroll delta
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static boolean dispatchNestedPreScroll(@NonNull View view, int dx, int dy,
            int @Nullable [] consumed, int @Nullable [] offsetInWindow) {
        return view.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    /**
     * Begin a nestable scroll operation along the given axes.
     *
     * <p>A view starting a nested scroll promises to abide by the following contract:</p>
     *
     * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
     * of a touch scroll this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
     * In the case of touch scrolling the nested scroll will be terminated automatically in
     * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
     * In the event of programmatic scrolling the caller must explicitly call
     * {@link #stopNestedScroll(View)} to indicate the end of the nested scroll.</p>
     *
     * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
     * If it returns false the caller may ignore the rest of this contract until the next scroll.
     * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
     *
     * <p>At each incremental step of the scroll the caller should invoke
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[]) dispatchNestedPreScroll}
     * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
     * parent at least partially consumed the scroll and the caller should adjust the amount it
     * scrolls by.</p>
     *
     * <p>After applying the remainder of the scroll delta the caller should invoke
     * {@link #dispatchNestedScroll(View, int, int, int, int, int[]) dispatchNestedScroll}, passing
     * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
     * these values differently. See
     * {@link NestedScrollingParent#onNestedScroll(View, int, int, int, int)}.
     * </p>
     *
     * @param view view on which to start the scroll.
     * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
     *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
     * @param type the type of input which cause this scroll event
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     *         the current gesture.
     *
     * @see #stopNestedScroll(View)
     * @see #dispatchNestedPreScroll(View, int, int, int[], int[])
     * @see #dispatchNestedScroll(View, int, int, int, int, int[])
     */
    public static boolean startNestedScroll(@NonNull View view, @ScrollAxis int axes,
            @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            return ((NestedScrollingChild2) view).startNestedScroll(axes, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return startNestedScroll(view, axes);
        }
        return false;
    }

    /**
     * Stop a nested scroll in progress.
     *
     * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
     *
     * @param view view for which to stop the scroll.
     * @param type the type of input which cause this scroll event
     * @see #startNestedScroll(View, int)
     */
    public static void stopNestedScroll(@NonNull View view, @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            ((NestedScrollingChild2) view).stopNestedScroll(type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            stopNestedScroll(view);
        }
    }

    /**
     * Returns true if this view has a nested scrolling parent.
     *
     * <p>The presence of a nested scrolling parent indicates that this view has initiated
     * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
     *
     * @param view view for which to check the parent.
     * @param type the type of input which cause this scroll event
     * @return whether this view has a nested scrolling parent
     */
    public static boolean hasNestedScrollingParent(@NonNull View view, @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            ((NestedScrollingChild2) view).hasNestedScrollingParent(type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return hasNestedScrollingParent(view);
        }
        return false;
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled(View) enabled} for this view this method does nothing.</p>
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[], int) dispatchNestedPreScroll}
     * before consuming a component of the scroll event themselves.
     *
     * <p>A non-null <code>consumed</code> int array of length 2 may be passed in to enable nested
     * scrolling parents to report how much of the scroll distance was consumed.  The original
     * caller (where the input event was received to start the scroll) should initialize the values
     * to be 0, in order to tell how much was actually consumed up the hierarchy of scrolling
     * parents.
     *
     * @param view view for which to dispatch the scroll.
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @param consumed Output, If not null, <code>consumed[0]</code> will contain the consumed
     *                 component of dx and <code>consumed[1]</code> the consumed dy.
     */
    public static void dispatchNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int @Nullable [] offsetInWindow,
            @NestedScrollType int type, int @NonNull [] consumed) {
        if (view instanceof NestedScrollingChild3) {
            ((NestedScrollingChild3) view).dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed);
        } else {
            dispatchNestedScroll(view, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    offsetInWindow, type);
        }
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled(View) enabled} for this view this method does nothing.
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[]) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.
     *
     * @param view view for which to dispatch the scroll.
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @return true if the event was dispatched, and therefore the scroll distance was consumed
     * @see #dispatchNestedPreScroll(View, int, int, int[], int[])
     */
    public static boolean dispatchNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int @Nullable [] offsetInWindow,
            @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            return ((NestedScrollingChild2) view).dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return dispatchNestedScroll(view, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    offsetInWindow);
        }
        return false;
    }

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
     * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
     * scrolling operation to consume some or all of the scroll operation before the child view
     * consumes it.</p>
     *
     * @param view view for which to dispatch the scroll.
     * @param dx Horizontal scroll distance in pixels
     * @param dy Vertical scroll distance in pixels
     * @param consumed Output. If not null, consumed[0] will contain the consumed component of dx
     *                 and consumed[1] the consumed dy.
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @return true if the parent consumed some or all of the scroll delta
     * @see #dispatchNestedScroll(View, int, int, int, int, int[])
     */
    public static boolean dispatchNestedPreScroll(@NonNull View view, int dx, int dy,
            int @Nullable [] consumed, int @Nullable [] offsetInWindow,
            @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            return ((NestedScrollingChild2) view).dispatchNestedPreScroll(dx, dy, consumed,
                    offsetInWindow, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return dispatchNestedPreScroll(view, dx, dy, consumed, offsetInWindow);
        }
        return false;
    }

    /**
     * Dispatch a fling to a nested scrolling parent.
     *
     * <p>This method should be used to indicate that a nested scrolling child has detected
     * suitable conditions for a fling. Generally this means that a touch scroll has ended with a
     * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
     * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
     * along a scrollable axis.</p>
     *
     * <p>If a nested scrolling child view would normally fling but it is at the edge of
     * its own content, it can use this method to delegate the fling to its nested scrolling
     * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
     *
     * @param view view for which to dispatch the fling.
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @param consumed true if the child consumed the fling, false otherwise
     * @return true if the nested scrolling parent consumed or otherwise reacted to the fling
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static boolean dispatchNestedFling(@NonNull View view, float velocityX, float velocityY,
            boolean consumed) {
        return view.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    /**
     * Dispatch a fling to a nested scrolling parent before it is processed by this view.
     *
     * <p>Nested pre-fling events are to nested fling events what touch intercept is to touch
     * and what nested pre-scroll is to nested scroll. <code>dispatchNestedPreFling</code>
     * offsets an opportunity for the parent view in a nested fling to fully consume the fling
     * before the child view consumes it. If this method returns <code>true</code>, a nested
     * parent view consumed the fling and this view should not scroll as a result.</p>
     *
     * <p>For a better user experience, only one view in a nested scrolling chain should consume
     * the fling at a time. If a parent view consumed the fling this method will return false.
     * Custom view implementations should account for this in two ways:</p>
     *
     * <ul>
     *     <li>If a custom view is paged and needs to settle to a fixed page-point, do not
     *     call <code>dispatchNestedPreFling</code>; consume the fling and settle to a valid
     *     position regardless.</li>
     *     <li>If a nested parent does consume the fling, this view should not scroll at all,
     *     even to settle back to a valid idle position.</li>
     * </ul>
     *
     * <p>Views should also not offer fling velocities to nested parent views along an axis
     * where scrolling is not currently supported; a {@link android.widget.ScrollView ScrollView}
     * should not offer a horizontal fling velocity to its parents since scrolling along that
     * axis is not permitted and carrying velocity along that motion does not make sense.</p>
     *
     * @param view view for which to dispatch the fling.
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @return true if a nested scrolling parent consumed the fling
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static boolean dispatchNestedPreFling(@NonNull View view, float velocityX,
            float velocityY) {
        return view.dispatchNestedPreFling(velocityX, velocityY);
    }

    /**
     * Returns whether the view hierarchy is currently undergoing a layout pass. This
     * information is useful to avoid situations such as calling {@link View#requestLayout()}
     * during a layout pass.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 18: Always returns {@code false}</li>
     * </ul>
     *
     * @return whether the view hierarchy is currently undergoing a layout pass
     * @deprecated Call {@link View#isInLayout()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isInLayout()")
    @Deprecated
    public static boolean isInLayout(@NonNull View view) {
        return view.isInLayout();
    }

    /**
     * Returns true if {@code view} has been through at least one layout since it
     * was last attached to or detached from a window.
     * @deprecated Call {@link View#isLaidOut()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isLaidOut()")
    @Deprecated
    public static boolean isLaidOut(@NonNull View view) {
        return view.isLaidOut();
    }

    /**
     * Returns whether layout direction has been resolved.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: Always returns {@code false}</li>
     * </ul>
     *
     * @return true if layout direction has been resolved.
     * @deprecated Call {@link View#isLayoutDirectionResolved()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isLayoutDirectionResolved()")
    @Deprecated
    public static boolean isLayoutDirectionResolved(@NonNull View view) {
        return view.isLayoutDirectionResolved();
    }

    /**
     * The visual z position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationZ(View, float) translationZ} property plus the current
     * {@link #getElevation(View) elevation} property.
     *
     * @param view view for which to get the position.
     *
     * @return The visual z position of this view, in pixels.
     */
    public static float getZ(@NonNull View view) {
        return view.getZ();
    }

    /**
     * Sets the visual z position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationZ(View, float) translationZ} property to be the difference between
     * the x value passed in and the current {@link #getElevation(View) elevation} property.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op</li>
     * </ul>
     *
     * @param view view for which to set the position.
     * @param z The visual z position of this view, in pixels.
     */
    public static void setZ(@NonNull View view, float z) {
        view.setZ(z);
    }

    /**
     * Offset this view's vertical location by the specified number of pixels.
     *
     * @param view view that needs to be offset.
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetTopAndBottom(@NonNull View view, int offset) {
        view.offsetTopAndBottom(offset);
    }

    /**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param view view which needs to be offset.
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetLeftAndRight(@NonNull View view, int offset) {
        view.offsetLeftAndRight(offset);
    }

    /**
     * Sets a rectangular area on this view to which the view will be clipped
     * when it is drawn. Setting the value to null will remove the clip bounds
     * and the view will draw normally, using its full bounds.
     *
     * <p>Prior to API 18 this does nothing.</p>
     *
     * @param view       The view to set clipBounds.
     * @param clipBounds The rectangular area, in the local coordinates of
     * this view, to which future drawing operations will be clipped.
     * @deprecated Call {@link View#setClipBounds(Rect)} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.setClipBounds(clipBounds)")
    @Deprecated
    public static void setClipBounds(@NonNull View view, @Nullable Rect clipBounds) {
        view.setClipBounds(clipBounds);
    }

    /**
     * Returns a copy of the current {@link #setClipBounds(View, Rect)}.
     *
     * <p>Prior to API 18 this will return null.</p>
     *
     * @return A copy of the current clip bounds if clip bounds are set,
     * otherwise null.
     * @deprecated Call {@link View#getClipBounds()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getClipBounds()")
    @Deprecated
    public static @Nullable Rect getClipBounds(@NonNull View view) {
        return view.getClipBounds();
    }

    /**
     * Returns true if the provided view is currently attached to a window.
     * @deprecated Call {@link View#isAttachedToWindow()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.isAttachedToWindow()")
    @Deprecated
    public static boolean isAttachedToWindow(@NonNull View view) {
        return view.isAttachedToWindow();
    }

    /**
     * Returns whether the provided view has an attached {@link View.OnClickListener}.
     *
     * @return true if there is a listener, false if there is none.
     * @deprecated Call {@link View#hasOnClickListeners()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.hasOnClickListeners()")
    @Deprecated
    public static boolean hasOnClickListeners(@NonNull View view) {
        return view.hasOnClickListeners();
    }

    /**
     * Sets the state of all scroll indicators.
     * <p>
     * See {@link #setScrollIndicators(View, int, int)} for usage information.
     *
     * @param view view for which to set the state.
     * @param indicators a bitmask of indicators that should be enabled, or
     *                   {@code 0} to disable all indicators
     *
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static void setScrollIndicators(@NonNull View view, @ScrollIndicators int indicators) {
        view.setScrollIndicators(indicators);
    }

    /**
     * Sets the state of the scroll indicators specified by the mask. To change
     * all scroll indicators at once, see {@link #setScrollIndicators(View, int)}.
     * <p>
     * When a scroll indicator is enabled, it will be displayed if the view
     * can scroll in the direction of the indicator.
     * <p>
     * Multiple indicator types may be enabled or disabled by passing the
     * logical OR of the desired types. If multiple types are specified, they
     * will all be set to the same enabled state.
     * <p>
     * For example, to enable the top scroll indicatorExample: {@code setScrollIndicators}
     *
     * @param view view for which to set the state.
     * @param indicators the indicator direction, or the logical OR of multiple
     *             indicator directions. One or more of:
     *             <ul>
     *               <li>{@link #SCROLL_INDICATOR_TOP}</li>
     *               <li>{@link #SCROLL_INDICATOR_BOTTOM}</li>
     *               <li>{@link #SCROLL_INDICATOR_LEFT}</li>
     *               <li>{@link #SCROLL_INDICATOR_RIGHT}</li>
     *               <li>{@link #SCROLL_INDICATOR_START}</li>
     *               <li>{@link #SCROLL_INDICATOR_END}</li>
     *             </ul>
     * @param mask the mask for scroll indicators.
     *
     * @see #setScrollIndicators(View, int)
     * @see #getScrollIndicators(View)
     */
    public static void setScrollIndicators(@NonNull View view, @ScrollIndicators int indicators,
            @ScrollIndicators int mask) {
        view.setScrollIndicators(indicators, mask);
    }

    /**
     * Returns a bitmask representing the enabled scroll indicators.
     * <p>
     * For example, if the top and left scroll indicators are enabled and all
     * other indicators are disabled, the return value will be
     * {@code ViewCompat.SCROLL_INDICATOR_TOP | ViewCompat.SCROLL_INDICATOR_LEFT}.
     * <p>
     * To check whether the bottom scroll indicator is enabled, use the value
     * of {@code (ViewCompat.getScrollIndicators(view) & ViewCompat.SCROLL_INDICATOR_BOTTOM) != 0}.
     *
     * @param view view for which to get the state.
     *
     * @return a bitmask representing the enabled scroll indicators
     */
    public static int getScrollIndicators(@NonNull View view) {
        return view.getScrollIndicators();
    }

    /**
     * Set the pointer icon for the current view.
     * @param view view for which to set the pointer icon.
     * @param pointerIcon A PointerIconCompat instance which will be shown when the mouse hovers.
     */
    public static void setPointerIcon(@NonNull View view, @Nullable PointerIconCompat pointerIcon) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.setPointerIcon(view, (PointerIcon) (pointerIcon != null
                    ? pointerIcon.getPointerIcon() : null));
        }
    }

    /**
     * Gets the logical display to which the view's window has been attached.
     * <p>
     * Compatibility:
     * <ul>
     * <li>API &lt; 17: Returns the default display when the view is attached. Otherwise, null.
     * </ul>
     *
     * @return The logical display, or null if the view is not currently attached to a window.
     * @deprecated Call {@link View#getDisplay()} directly.
     */
    @androidx.annotation.ReplaceWith(expression = "view.getDisplay()")
    @Deprecated
    public static @Nullable Display getDisplay(@NonNull View view) {
        return view.getDisplay();
    }

    /**
     * Sets the tooltip for the view.
     *
     * <p>Prior to API 26 this does nothing. Use TooltipCompat class from v7 appcompat library
     * for a compatible tooltip implementation.</p>
     *
     * @param view view for which to set the tooltip.
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setTooltipText(view, tooltipText);
        }
    }

    /**
     * Start the drag and drop operation.
     */
    @SuppressWarnings("deprecation")
    public static boolean startDragAndDrop(@NonNull View v, @Nullable ClipData data,
            View.@NonNull DragShadowBuilder shadowBuilder, @Nullable Object myLocalState,
            int flags) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.startDragAndDrop(v, data, shadowBuilder, myLocalState, flags);
        } else {
            return v.startDrag(data, shadowBuilder, myLocalState, flags);
        }
    }

    /**
     * Cancel the drag and drop operation.
     */
    public static void cancelDragAndDrop(@NonNull View v) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.cancelDragAndDrop(v);
        }
    }

    /**
     * Update the drag shadow while drag and drop is in progress.
     */
    public static void updateDragShadow(@NonNull View v,
            View.@NonNull DragShadowBuilder shadowBuilder) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.updateDragShadow(v, shadowBuilder);
        }
    }

    /**
     * Gets the ID of the next keyboard navigation cluster root.
     *
     * @return the next keyboard navigation cluster ID, or {@link View#NO_ID} if the framework
     *         should decide automatically or API < 26.
     */
    public static int getNextClusterForwardId(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.getNextClusterForwardId(view);
        }
        return View.NO_ID;
    }

    /**
     * Sets the ID of the next keyboard navigation cluster root view. Does nothing if {@code view}
     * is not a keyboard navigation cluster or if API < 26.
     *
     * @param view view for which to set the ID.
     * @param nextClusterForwardId next cluster ID, or {@link View#NO_ID} if the framework
     *                             should decide automatically.
     */
    public static void setNextClusterForwardId(@NonNull View view, int nextClusterForwardId) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setNextClusterForwardId(view, nextClusterForwardId);
        }
    }

    /**
     * Returns whether {@code view} is a root of a keyboard navigation cluster. Always returns
     * {@code false} on API < 26.
     *
     * @param view view for which to check the cluster.
     * @return {@code true} if this view is a root of a cluster, or {@code false} otherwise.
     */
    public static boolean isKeyboardNavigationCluster(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.isKeyboardNavigationCluster(view);
        }
        return false;
    }

    /**
     * Set whether {@code view} is a root of a keyboard navigation cluster. Does nothing if
     * API < 26.
     *
     * @param view view for which to set the cluster.
     * @param isCluster {@code true} to mark {@code view} as the root of a cluster, {@code false}
     *                  to unmark.
     */
    public static void setKeyboardNavigationCluster(@NonNull View view, boolean isCluster) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setKeyboardNavigationCluster(view, isCluster);
        }
    }

    /**
     * Returns whether {@code view} should receive focus when the focus is restored for the view
     * hierarchy containing it. Returns {@code false} on API < 26.
     * <p>
     * Focus gets restored for a view hierarchy when the root of the hierarchy gets added to a
     * window or serves as a target of cluster navigation.
     *
     * @param view view for which to check the state.
     * @return {@code true} if {@code view} is the default-focus view, {@code false} otherwise.
     */
    public static boolean isFocusedByDefault(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.isFocusedByDefault(view);
        }
        return false;
    }

    /**
     * Sets whether {@code view} should receive focus when the focus is restored for the view
     * hierarchy containing it.
     * <p>
     * Focus gets restored for a view hierarchy when the root of the hierarchy gets added to a
     * window or serves as a target of cluster navigation.
     * <p>
     * Does nothing on API < 26.
     *
     * @param view view for which to set the state.
     * @param isFocusedByDefault {@code true} to set {@code view} as the default-focus view,
     *                           {@code false} otherwise.
     */
    public static void setFocusedByDefault(@NonNull View view, boolean isFocusedByDefault) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setFocusedByDefault(view, isFocusedByDefault);
        }
    }

    /**
     * Find the nearest keyboard navigation cluster in the specified direction.
     * This does not actually give focus to that cluster.
     *
     * @param view view on which to do the search.
     * @param currentCluster The starting point of the search. {@code null} means the current
     *                       cluster is not found yet.
     * @param direction Direction to look.
     *
     * @return the nearest keyboard navigation cluster in the specified direction, or {@code null}
     *         if one can't be found or if API < 26.
     */
    public static @Nullable View keyboardNavigationClusterSearch(@NonNull View view,
            @Nullable View currentCluster, @FocusDirection int direction) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.keyboardNavigationClusterSearch(view, currentCluster, direction);
        }
        return null;
    }

    /**
     * Adds any keyboard navigation cluster roots that are descendants of {@code view} (
     * including {@code view} if it is a cluster root itself) to {@code views}. Does nothing
     * on API < 26.
     *
     * @param view view on which to make the change.
     * @param views collection of keyboard navigation cluster roots found so far.
     * @param direction direction to look.
     */
    public static void addKeyboardNavigationClusters(@NonNull View view,
            @NonNull Collection<View> views, int direction) {
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.addKeyboardNavigationClusters(view, views, direction);
        }
    }

    /**
     * Gives focus to the default-focus view in the view hierarchy rooted at {@code view}.
     * If the default-focus view cannot be found or if API < 26, this falls back to calling
     * {@link View#requestFocus(int)}.
     *
     * @param view view on which to make the change.
     * @return {@code true} if {@code view} or one of its descendants took focus, {@code false}
     *         otherwise.
     */
    public static boolean restoreDefaultFocus(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.restoreDefaultFocus(view);
        }
        return view.requestFocus();
    }

    /**
     * Returns true if this view is focusable or if it contains a reachable View
     * for which {@link View#hasExplicitFocusable()} returns {@code true}.
     * A "reachable hasExplicitFocusable()" is a view whose parents do not block descendants focus.
     * Only {@link View#VISIBLE} views for which {@link View#getFocusable()} would return
     * {@link View#FOCUSABLE} are considered focusable.
     *
     * <p>This method preserves the pre-{@link Build.VERSION_CODES#O} behavior of
     * {@link View#hasFocusable()} in that only views explicitly set focusable will cause
     * this method to return true. A view set to {@link View#FOCUSABLE_AUTO} that resolves
     * to focusable will not.</p>
     *
     * @param view view on which to make the change.
     * @return {@code true} if the view is focusable or if the view contains a focusable
     *         view, {@code false} otherwise
     */
    public static boolean hasExplicitFocusable(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= 26) {
            return Api26Impl.hasExplicitFocusable(view);
        }
        return view.hasFocusable();
    }

    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     * @deprecated Call {@link View#generateViewId()} directly.
     */
    @Deprecated
    public static int generateViewId() {
        return View.generateViewId();
    }

    /**
     * Adds a listener which will receive unhandled {@link KeyEvent}s. This must be called on the
     * UI thread.
     *
     * @param view view on which to add the listener.
     * @param listener a receiver of unhandled {@link KeyEvent}s.
     * @see #removeOnUnhandledKeyEventListener
     */
    public static void addOnUnhandledKeyEventListener(@NonNull View view,
            final @NonNull OnUnhandledKeyEventListenerCompat listener) {
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.addOnUnhandledKeyEventListener(view, listener);
            return;
        }
        @SuppressWarnings("unchecked")
        ArrayList<OnUnhandledKeyEventListenerCompat> viewListeners =
                (ArrayList<OnUnhandledKeyEventListenerCompat>)
                        view.getTag(R.id.tag_unhandled_key_listeners);
        if (viewListeners == null) {
            viewListeners = new ArrayList<>();
            view.setTag(R.id.tag_unhandled_key_listeners, viewListeners);
        }
        viewListeners.add(listener);
        if (viewListeners.size() == 1) {
            UnhandledKeyEventManager.registerListeningView(view);
        }
    }

    /**
     * Removes a listener which will receive unhandled {@link KeyEvent}s. This must be called on the
     * UI thread.
     *
     * @param view view from which to remove the listener.
     * @param listener a receiver of unhandled {@link KeyEvent}s.
     * @see #addOnUnhandledKeyEventListener
     */
    public static void removeOnUnhandledKeyEventListener(@NonNull View view,
            @NonNull OnUnhandledKeyEventListenerCompat listener) {
        if (Build.VERSION.SDK_INT >= 28) {
            Api28Impl.removeOnUnhandledKeyEventListener(view, listener);
            return;
        }
        @SuppressWarnings("unchecked")
        ArrayList<OnUnhandledKeyEventListenerCompat> viewListeners =
                (ArrayList<OnUnhandledKeyEventListenerCompat>)
                        view.getTag(R.id.tag_unhandled_key_listeners);
        if (viewListeners != null) {
            viewListeners.remove(listener);
            if (viewListeners.size() == 0) {
                UnhandledKeyEventManager.unregisterListeningView(view);
            }
        }
    }

    /**
     * @deprecated This is a utility class and it shouldn't be instantiated.
     */
    @Deprecated
    protected ViewCompat() {
    }

    /**
     * Interface definition for a callback to be invoked when a hardware key event hasn't
     * been handled by the view hierarchy.
     */
    @SuppressWarnings("NullableProblems") // Useless warning
    public interface OnUnhandledKeyEventListenerCompat {
        /**
         * Called when a hardware key is dispatched to a view after being unhandled during normal
         * {@link KeyEvent} dispatch.
         *
         * @param v The view the key has been dispatched to.
         * @param event The KeyEvent object containing information about the event.
         * @return {@code true} if the listener has consumed the event, {@code false} otherwise.
         */
        boolean onUnhandledKeyEvent(@NonNull View v, @NonNull KeyEvent event);
    }

    @UiThread
    static boolean dispatchUnhandledKeyEventBeforeHierarchy(View root, KeyEvent evt) {
        if (Build.VERSION.SDK_INT >= 28) {
            return false;
        }
        return UnhandledKeyEventManager.at(root).preDispatch(evt);
    }

    @UiThread
    static boolean dispatchUnhandledKeyEventBeforeCallback(View root, KeyEvent evt) {
        if (Build.VERSION.SDK_INT >= 28) {
            return false;
        }
        return UnhandledKeyEventManager.at(root).dispatch(root, evt);
    }

    /**
     * Modifies the input matrix such that it maps on-screen coordinates to
     * view-local coordinates for the provided view.
     *
     * @param view view to examine
     * @param matrix input matrix to modify
     */
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public static void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.transformMatrixToGlobal(view, matrix);
        } else {
            // The View method in question is available as a public (but hidden) method all the way
            // back to API 21, but we check that it's actually present, since conformance testing
            // does not assert about methods that are not in the public API.
            if (sTryHiddenViewTransformMatrixToGlobal) {
                try {
                    Api29Impl.transformMatrixToGlobal(view, matrix);
                    return;
                } catch (NoSuchMethodError e) {
                    sTryHiddenViewTransformMatrixToGlobal = false;
                }
            }
            fallbackTransformMatrixToGlobal(view, matrix);
        }
    }

    @VisibleForTesting
    static void fallbackTransformMatrixToGlobal(View view, Matrix matrix) {
        ViewParent parent = view.getParent();
        if (parent instanceof View) {
            View parentView = (View) parent;
            fallbackTransformMatrixToGlobal(parentView, matrix);
            matrix.preTranslate(-parentView.getScrollX(), -parentView.getScrollY());
        }
        matrix.preTranslate(view.getLeft(), view.getTop());
        matrix.preConcat(view.getMatrix());
    }

    /**
     * Sets whether this View should be a focusable element for screen readers
     * and include non-focusable Views from its subtree when providing feedback.
     * <p>
     * Note: this is similar to using <a href="#attr_android:focusable">{@code android:focusable},
     * but does not impact input focus behavior.
     * <p>This can be used to
     * <a href="{@docRoot}guide/topics/ui/accessibility/principles#content-groups">group related
     * content.</a>
     * </p>
     *
     * @param view The view whose title should be set
     * @param screenReaderFocusable Whether the view should be treated as a unit by screen reader
     *                              accessibility tools.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: No-op
     * </ul>
     */
    @UiThread
    public static void setScreenReaderFocusable(@NonNull View view, boolean screenReaderFocusable) {
        screenReaderFocusableProperty().set(view, screenReaderFocusable);
    }

    /**
     * Returns whether the view should be treated as a focusable unit by screen reader
     * accessibility tools.
     * @see #setScreenReaderFocusable(View, boolean)
     *
     * @param view The view to check for screen reader focusability.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: Always returns {@code false}</li>
     * </ul>
     *
     * @return Whether the view should be treated as a focusable unit by screen reader.
     */
    @UiThread
    public static boolean isScreenReaderFocusable(@NonNull View view) {
        Boolean result = screenReaderFocusableProperty().get(view);
        return result != null && result;
    }

    private static AccessibilityViewProperty<Boolean> screenReaderFocusableProperty() {
        return new AccessibilityViewProperty<Boolean>(
                R.id.tag_screen_reader_focusable, Boolean.class, 28) {

            @RequiresApi(28)
            @Override
            Boolean frameworkGet(@NonNull View view) {
                return ViewCompat.Api28Impl.isScreenReaderFocusable(view);
            }

            @RequiresApi(28)
            @Override
            void frameworkSet(@NonNull View view, Boolean value) {
                ViewCompat.Api28Impl.setScreenReaderFocusable(view, value);
            }

            @Override
            boolean shouldUpdate(Boolean oldValue, Boolean newValue) {
                return !booleanNullToFalseEquals(oldValue, newValue);
            }
        };
    }

    /**
     * Visually distinct portion of a window with window-like semantics are considered panes for
     * accessibility purposes. One example is the content view of a large fragment that is replaced.
     * In order for accessibility services to understand a pane's window-like behavior, panes
     * should have descriptive titles. Views with pane titles produce
     * {@link AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED}s when they appear, disappear, or change
     * title.
     *
     * <p>
     * When transitioning from one Activity to another, instead of using
     * setAccessibilityPaneTitle(), set a descriptive title for your activity's window by using
     * {@code android:label} for the matching <activity> entry in your application’s manifest  or
     * updating the title at runtime with {@link android.app.Activity#setTitle(CharSequence)}.
     *
     * <p>
     * To set the pane title in xml, use {@code android:accessibilityPaneTitle}.
     * @param view The view whose pane title should be set.
     * @param accessibilityPaneTitle The pane's title. Setting to {@code null} indicates that this
     *                               View is not a pane.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: No-op
     * </ul>
     *
     * {@see AccessibilityNodeInfo#setPaneTitle(CharSequence)}
     */
    @UiThread
    public static void setAccessibilityPaneTitle(@NonNull View view,
            @Nullable CharSequence accessibilityPaneTitle) {
        paneTitleProperty().set(view, accessibilityPaneTitle);
        if (accessibilityPaneTitle != null) {
            sAccessibilityPaneVisibilityManager.addAccessibilityPane(view);
        } else {
            sAccessibilityPaneVisibilityManager.removeAccessibilityPane(view);
        }
    }

    /**
     * Get the title of the pane for purposes of accessibility.
     *
     * @param view The view queried for it's pane title.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 19: Always returns {@code null}</li>
     * </ul>
     *
     * @return The current pane title.
     *
     * {@see #setAccessibilityPaneTitle}.
     */
    @UiThread
    public static @Nullable CharSequence getAccessibilityPaneTitle(@NonNull View view) {
        return paneTitleProperty().get(view);
    }

    private static AccessibilityViewProperty<CharSequence> paneTitleProperty() {
        return new AccessibilityViewProperty<CharSequence>(R.id.tag_accessibility_pane_title,
                CharSequence.class, AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE, 28) {

            @RequiresApi(28)
            @Override
            CharSequence frameworkGet(View view) {
                return ViewCompat.Api28Impl.getAccessibilityPaneTitle(view);
            }

            @RequiresApi(28)
            @Override
            void frameworkSet(View view, CharSequence value) {
                ViewCompat.Api28Impl.setAccessibilityPaneTitle(view, value);
            }

            @Override
            boolean shouldUpdate(CharSequence oldValue, CharSequence newValue) {
                return !TextUtils.equals(oldValue, newValue);
            }
        };
    }

    private static AccessibilityViewProperty<CharSequence> stateDescriptionProperty() {
        return new AccessibilityViewProperty<CharSequence>(R.id.tag_state_description,
                CharSequence.class, AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION, 30) {

            @RequiresApi(30)
            @Override
            CharSequence frameworkGet(View view) {
                return Api30Impl.getStateDescription(view);
            }

            @RequiresApi(30)
            @Override
            void frameworkSet(View view, CharSequence value) {
                Api30Impl.setStateDescription(view, value);
            }

            @Override
            boolean shouldUpdate(CharSequence oldValue, CharSequence newValue) {
                return !TextUtils.equals(oldValue, newValue);
            }
        };
    }

    /**
     * Gets whether this view is a heading for accessibility purposes.
     *
     * @param view The view checked if it is a heading.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 28: Always returns {@code false}</li>
     * </ul>
     *
     * @return {@code true} if the view is a heading, {@code false} otherwise.
     */
    @UiThread
    public static boolean isAccessibilityHeading(@NonNull View view) {
        Boolean result = accessibilityHeadingProperty().get(view);
        return result != null && result;
    }

    /**
     * Set if view is a heading for a section of content for accessibility purposes.
     *
     * @param view The view to set if it is a heading.
     * @param isHeading {@code true} if the view is a heading, {@code false} otherwise.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 28: No-op
     * </ul>
     */
    @UiThread
    public static void setAccessibilityHeading(@NonNull View view, boolean isHeading) {
        accessibilityHeadingProperty().set(view, isHeading);
    }

    private static AccessibilityViewProperty<Boolean> accessibilityHeadingProperty() {
        return new AccessibilityViewProperty<Boolean>(
                R.id.tag_accessibility_heading, Boolean.class, 28) {

            @RequiresApi(28)
            @Override
            Boolean frameworkGet(View view) {
                return ViewCompat.Api28Impl.isAccessibilityHeading(view);
            }

            @RequiresApi(28)
            @Override
            void frameworkSet(View view, Boolean value) {
                ViewCompat.Api28Impl.setAccessibilityHeading(view, value);
            }

            @Override
            boolean shouldUpdate(Boolean oldValue, Boolean newValue) {
                return !booleanNullToFalseEquals(oldValue, newValue);
            }
        };
    }


    abstract static class AccessibilityViewProperty<T> {
        private final int mTagKey;
        private final Class<T> mType;
        private final int mFrameworkMinimumSdk;
        private final int mContentChangeType;

        AccessibilityViewProperty(int tagKey, Class<T> type, int frameworkMinimumSdk) {
            this(tagKey, type,
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED, frameworkMinimumSdk);
        }

        AccessibilityViewProperty(
                int tagKey, Class<T> type, int contentChangeType, int frameworkMinimumSdk) {
            mTagKey = tagKey;
            mType = type;
            mContentChangeType = contentChangeType;
            mFrameworkMinimumSdk = frameworkMinimumSdk;

        }

        void set(View view, T value) {
            if (frameworkAvailable()) {
                frameworkSet(view, value);
            } else if (shouldUpdate(get(view), value)) {
                ensureAccessibilityDelegateCompat(view);
                view.setTag(mTagKey, value);
                // If we're here, we're guaranteed to be on v19+ (see the logic in
                // extrasAvailable), so we can call notifyViewAccessibilityStateChangedIfNeeded
                // which requires 19.
                notifyViewAccessibilityStateChangedIfNeeded(view, mContentChangeType);
            }
        }

        @SuppressWarnings("unchecked")
        T get(View view) {
            if (frameworkAvailable()) {
                return frameworkGet(view);
            } else {
                Object value = view.getTag(mTagKey);
                if (mType.isInstance(value)) {
                    return (T) value;
                }
            }
            return null;
        }

        private boolean frameworkAvailable() {
            return Build.VERSION.SDK_INT >= mFrameworkMinimumSdk;
        }

        boolean shouldUpdate(T oldValue, T newValue) {
            return !newValue.equals(oldValue);
        }

        abstract T frameworkGet(View view);

        abstract void frameworkSet(View view, T value);

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean booleanNullToFalseEquals(Boolean a, Boolean b) {
            boolean aBool = a != null && a;
            boolean bBool = b != null && b;
            return aBool == bBool;
        }
    }

    static void notifyViewAccessibilityStateChangedIfNeeded(View view, int changeType) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                view.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!accessibilityManager.isEnabled()) {
            return;
        }
        boolean isVisibleAccessibilityPane = getAccessibilityPaneTitle(view) != null
                && (view.isShown() && view.getWindowVisibility() == VISIBLE);
        // If this is a live region or accessibilityPane, we should send a subtree change event
        // from this view immediately. Otherwise, we can let it propagate up.
        if ((view.getAccessibilityLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE)
                || isVisibleAccessibilityPane) {
            final AccessibilityEvent event = AccessibilityEvent.obtain();
            event.setEventType(isVisibleAccessibilityPane
                    ? AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    : AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            event.setContentChangeTypes(changeType);
            if (isVisibleAccessibilityPane) {
                event.getText().add(getAccessibilityPaneTitle(view));
                setImportantForAccessibilityIfNeeded(view);
            }
            view.sendAccessibilityEventUnchecked(event);
        } else if (changeType == AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED) {
            final AccessibilityEvent event = AccessibilityEvent.obtain();
            view.onInitializeAccessibilityEvent(event);
            event.setEventType(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            event.setContentChangeTypes(changeType);
            event.setSource(view);
            view.onPopulateAccessibilityEvent(event);
            event.getText().add(getAccessibilityPaneTitle(view));
            accessibilityManager.sendAccessibilityEvent(event);
        } else if (view.getParent() != null) {
            final ViewParent parent = view.getParent();
            try {
                parent.notifySubtreeAccessibilityStateChanged(view, view, changeType);
            } catch (AbstractMethodError e) {
                Log.e(TAG, view.getParent().getClass().getSimpleName()
                        + " does not fully implement ViewParent", e);
            }
        }
    }

    private static void setImportantForAccessibilityIfNeeded(View view) {
        if (view.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    private static final AccessibilityPaneVisibilityManager sAccessibilityPaneVisibilityManager =
            new AccessibilityPaneVisibilityManager();

    static class AccessibilityPaneVisibilityManager
            implements ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener {
        private final WeakHashMap<View, Boolean> mPanesToVisible = new WeakHashMap<>();

        @Override
        public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < 28) {
                for (Map.Entry<View, Boolean> entry : mPanesToVisible.entrySet()) {
                    checkPaneVisibility(entry);
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            // When detached the view loses its viewTreeObserver.
            registerForLayoutCallback(view);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            // Don't do anything.
        }

        void addAccessibilityPane(View pane) {
            mPanesToVisible.put(pane, pane.isShown() && pane.getWindowVisibility() == VISIBLE);
            pane.addOnAttachStateChangeListener(this);
            if (pane.isAttachedToWindow()) {
                registerForLayoutCallback(pane);
            }
        }

        void removeAccessibilityPane(View pane) {
            mPanesToVisible.remove(pane);
            pane.removeOnAttachStateChangeListener(this);
            unregisterForLayoutCallback(pane);
        }

        private void checkPaneVisibility(Map.Entry<View, Boolean> panesToVisibleEntry) {
            View pane = panesToVisibleEntry.getKey();
            boolean oldVisibility = panesToVisibleEntry.getValue();
            boolean newVisibility = pane.isShown() && pane.getWindowVisibility() == VISIBLE;
            if (oldVisibility != newVisibility) {
                int contentChangeType = newVisibility
                        ? AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED
                        : AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED;
                notifyViewAccessibilityStateChangedIfNeeded(pane, contentChangeType);
                panesToVisibleEntry.setValue(newVisibility);
            }
        }

        private void registerForLayoutCallback(View view) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }

        private void unregisterForLayoutCallback(View view) {
            ViewTreeObserver observer = view.getViewTreeObserver();
            observer.removeOnGlobalLayoutListener(this);
        }
    }

    static class UnhandledKeyEventManager {
        // The number of views with listeners is usually much fewer than the number of views.
        // This means it should be faster to only check parent chains of views with listeners than
        // to check every view for listeners.
        private static final ArrayList<WeakReference<View>> sViewsWithListeners = new ArrayList<>();

        // This is a cache (per keypress) of all the views which either have listeners or
        // contain a view with listeners. This is only accessed on the UI thread.
        private @Nullable WeakHashMap<View, Boolean> mViewsContainingListeners = null;

        // Keeps track of which Views have unhandled key focus for which keys. This doesn't
        // include modifiers.
        private SparseArray<WeakReference<View>> mCapturedKeys = null;

        // Set to the last KeyEvent which went through preDispatch. Currently, it's difficult to
        // unify the "earliest" point we can handle a KeyEvent in all code-paths. However, this
        // de-duplicating behavior is left as an implementation detail only since things may
        // become cleaner as more of supportlib moves towards the component model.
        private WeakReference<KeyEvent> mLastDispatchedPreViewKeyEvent = null;

        private SparseArray<WeakReference<View>> getCapturedKeys() {
            if (mCapturedKeys == null) {
                mCapturedKeys = new SparseArray<>();
            }
            return mCapturedKeys;
        }

        static UnhandledKeyEventManager at(View root) {
            UnhandledKeyEventManager manager = (UnhandledKeyEventManager)
                    root.getTag(R.id.tag_unhandled_key_event_manager);
            if (manager == null) {
                manager = new UnhandledKeyEventManager();
                root.setTag(R.id.tag_unhandled_key_event_manager, manager);
            }
            return manager;
        }

        boolean dispatch(View root, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                recalcViewsWithUnhandled();
            }

            View consumer = dispatchInOrder(root, event);

            // If an unhandled listener handles one, then keep track of it so that the consuming
            // view is first to receive its repeats and release as well.
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int keycode = event.getKeyCode();
                if (consumer != null && !KeyEvent.isModifierKey(keycode)) {
                    getCapturedKeys().put(keycode, new WeakReference<>(consumer));
                }
            }
            return consumer != null;
        }

        private @Nullable View dispatchInOrder(View view, KeyEvent event) {
            if (mViewsContainingListeners == null || !mViewsContainingListeners.containsKey(view)) {
                return null;
            }
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                // No access to internal ViewGroup ordering here, so just use child order.
                for (int i = vg.getChildCount() - 1; i >= 0; --i) {
                    View v = vg.getChildAt(i);
                    View consumer = dispatchInOrder(v, event);
                    if (consumer != null) {
                        return consumer;
                    }
                }
            }
            if (onUnhandledKeyEvent(view, event)) {
                return view;
            }
            return null;
        }

        /**
         * Called before the event gets dispatched to the view hierarchy
         * @return {@code true} if an unhandled handler has focus and consumed the event
         */
        boolean preDispatch(KeyEvent event) {
            // De-duplicate calls to preDispatch. See comment on mLastDispatchedPreViewKeyEvent.
            if (mLastDispatchedPreViewKeyEvent != null
                    && mLastDispatchedPreViewKeyEvent.get() == event) {
                return false;
            }
            mLastDispatchedPreViewKeyEvent = new WeakReference<>(event);

            // Always clean-up 'up' events since it's possible for earlier dispatch stages to
            // consume them without consuming the corresponding 'down' event.
            WeakReference<View> currentReceiver = null;
            SparseArray<WeakReference<View>> capturedKeys = getCapturedKeys();
            if (event.getAction() == KeyEvent.ACTION_UP) {
                int idx = capturedKeys.indexOfKey(event.getKeyCode());
                if (idx >= 0) {
                    currentReceiver = capturedKeys.valueAt(idx);
                    capturedKeys.removeAt(idx);
                }
            }
            if (currentReceiver == null) {
                currentReceiver = capturedKeys.get(event.getKeyCode());
            }
            if (currentReceiver != null) {
                View target = currentReceiver.get();
                if (target != null && target.isAttachedToWindow()) {
                    onUnhandledKeyEvent(target, event);
                }
                // consume anyways so that we don't feed uncaptured key events to other views
                return true;
            }
            return false;
        }

        private boolean onUnhandledKeyEvent(@NonNull View v, @NonNull KeyEvent event) {
            @SuppressWarnings("unchecked")
            ArrayList<OnUnhandledKeyEventListenerCompat> viewListeners =
                    (ArrayList<OnUnhandledKeyEventListenerCompat>)
                            v.getTag(R.id.tag_unhandled_key_listeners);
            if (viewListeners != null) {
                for (int i = viewListeners.size() - 1; i >= 0; --i) {
                    if (viewListeners.get(i).onUnhandledKeyEvent(v, event)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Registers that a view has at least one {@link OnUnhandledKeyEventListenerCompat}. Does
         * nothing if the view is already registered.
         */
        static void registerListeningView(View v) {
            synchronized (sViewsWithListeners) {
                for (WeakReference<View> wv : sViewsWithListeners) {
                    if (wv.get() == v) {
                        return;
                    }
                }
                sViewsWithListeners.add(new WeakReference<>(v));
            }
        }

        static void unregisterListeningView(View v) {
            synchronized (sViewsWithListeners) {
                for (int i = 0; i < sViewsWithListeners.size(); ++i) {
                    if (sViewsWithListeners.get(i).get() == v) {
                        sViewsWithListeners.remove(i);
                        return;
                    }
                }
            }
        }

        private void recalcViewsWithUnhandled() {
            if (mViewsContainingListeners != null) {
                mViewsContainingListeners.clear();
            }
            if (sViewsWithListeners.isEmpty()) {
                return;
            }
            synchronized (sViewsWithListeners) {
                if (mViewsContainingListeners == null) {
                    mViewsContainingListeners = new WeakHashMap<>();
                }
                for (int i = sViewsWithListeners.size() - 1; i >= 0; --i) {
                    WeakReference<View> vw = sViewsWithListeners.get(i);
                    View v = vw.get();
                    if (v == null) {
                        sViewsWithListeners.remove(i);
                    } else {
                        mViewsContainingListeners.put(v, Boolean.TRUE);
                        ViewParent nxt = v.getParent();
                        while (nxt instanceof View) {
                            mViewsContainingListeners.put((View) nxt, Boolean.TRUE);
                            nxt = nxt.getParent();
                        }
                    }
                }
            }
        }
    }

    private static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static WindowInsetsCompat computeSystemWindowInsets(@NonNull View v,
                @NonNull WindowInsetsCompat insets, @NonNull Rect outLocalInsets) {
            WindowInsets platformInsets = insets.toWindowInsets();
            if (platformInsets != null) {
                return WindowInsetsCompat.toWindowInsetsCompat(
                        v.computeSystemWindowInsets(platformInsets, outLocalInsets), v);
            } else {
                outLocalInsets.setEmpty();
                return insets;
            }
        }

        static void setOnApplyWindowInsetsListener(final @NonNull View v,
                final @Nullable OnApplyWindowInsetsListener listener) {
            final View.OnApplyWindowInsetsListener wrappedUserListener = listener != null
                    ? new View.OnApplyWindowInsetsListener() {
                        WindowInsetsCompat mLastInsets = null;

                        @Override
                        public WindowInsets onApplyWindowInsets(final View view,
                                final WindowInsets insets) {
                            WindowInsetsCompat compatInsets =
                                    WindowInsetsCompat.toWindowInsetsCompat(insets, view);
                            if (Build.VERSION.SDK_INT < 30) {
                                callCompatInsetAnimationCallback(insets, v);

                                if (compatInsets.equals(mLastInsets)) {
                                    // We got the same insets we just return the previously computed
                                    // insets.
                                    return listener.onApplyWindowInsets(view, compatInsets)
                                            .toWindowInsets();
                                }
                            }
                            mLastInsets = compatInsets;
                            compatInsets = listener.onApplyWindowInsets(view, compatInsets);

                            if (Build.VERSION.SDK_INT >= 30) {
                                return compatInsets.toWindowInsets();
                            }

                            // On API < 30, the visibleInsets, used to built WindowInsetsCompat, are
                            // updated after the insets dispatch so we don't have the updated
                            // visible insets at that point. As a workaround, we re-apply the insets
                            // so we know that we'll have the right value the next time it's called.
                            requestApplyInsets(view);
                            // Keep a copy in case the insets haven't changed on the next call so we
                            // don't need to call the listener again.

                            return compatInsets.toWindowInsets();
                        }
                    }
                    : null;

            // For backward compatibility of WindowInsetsAnimation, we use an
            // OnApplyWindowInsetsListener. We use the view tags to keep track of both listeners
            if (Build.VERSION.SDK_INT < 30) {
                v.setTag(R.id.tag_on_apply_window_listener, wrappedUserListener);
            }

            final Object compatInsetsDispatch = v.getTag(R.id.tag_compat_insets_dispatch);
            if (compatInsetsDispatch != null) {
                // Don't call `v.setOnApplyWindowInsetsListener`. Otherwise, it will overwrite the
                // compat-dispatch listener. The compat-dispatch listener will make sure listeners
                // stored with `tag_on_apply_window_listener` and
                // `tag_window_insets_animation_callback` are get called.
                return;
            }

            if (wrappedUserListener != null) {
                v.setOnApplyWindowInsetsListener(wrappedUserListener);
            } else {
                // If the listener is null, we need to make sure our compat listener, if any, is
                // set in-lieu of the listener being removed.
                final View.OnApplyWindowInsetsListener compatInsetsAnimationCallback =
                        (View.OnApplyWindowInsetsListener) v.getTag(
                                R.id.tag_window_insets_animation_callback);
                v.setOnApplyWindowInsetsListener(compatInsetsAnimationCallback);
            }
        }

        /**
         * The backport of {@link WindowInsetsAnimationCompat.Callback} on API < 30 relies on
         * onApplyWindowInsetsListener, so if this callback is set, we'll call it in this method
         */
        static void callCompatInsetAnimationCallback(final @NonNull WindowInsets insets,
                final @NonNull View v) {
            // In case a WindowInsetsAnimationCompat.Callback is set, make sure to
            // call its compat listener.
            View.OnApplyWindowInsetsListener insetsAnimationCallback =
                    (View.OnApplyWindowInsetsListener) v.getTag(
                            R.id.tag_window_insets_animation_callback);
            if (insetsAnimationCallback != null) {
                insetsAnimationCallback.onApplyWindowInsets(v, insets);
            }
        }

    }

    private static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        public static @Nullable WindowInsetsCompat getRootWindowInsets(@NonNull View v) {
            final WindowInsets wi = v.getRootWindowInsets();
            if (wi == null) return null;

            final WindowInsetsCompat insets = WindowInsetsCompat.toWindowInsetsCompat(wi);
            // This looks strange, but the WindowInsetsCompat instance still needs to know about
            // what the root window insets, and the root view visible bounds are
            insets.setRootWindowInsets(insets);
            insets.copyRootViewBounds(v.getRootView());
            return insets;
        }
    }

    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void saveAttributeDataForStyleable(@NonNull View view,
                @NonNull Context context, int @NonNull [] styleable, @Nullable AttributeSet attrs,
                @NonNull TypedArray t, int defStyleAttr, int defStyleRes) {
            view.saveAttributeDataForStyleable(
                    context, styleable, attrs, t, defStyleAttr, defStyleRes);
        }

        static View.AccessibilityDelegate getAccessibilityDelegate(View view) {
            return view.getAccessibilityDelegate();
        }

        static void setSystemGestureExclusionRects(View view, List<Rect> rects) {
            view.setSystemGestureExclusionRects(rects);
        }

        static List<Rect> getSystemGestureExclusionRects(View view) {
            return view.getSystemGestureExclusionRects();
        }

        static ContentCaptureSession getContentCaptureSession(View view) {
            return view.getContentCaptureSession();
        }

        static void setContentCaptureSession(View view,
                ContentCaptureSessionCompat contentCaptureSession) {
            view.setContentCaptureSession(contentCaptureSession == null
                    ? null : contentCaptureSession.toContentCaptureSession());
        }

        static void transformMatrixToGlobal(View view, Matrix matrix) {
            view.transformMatrixToGlobal(matrix);
        }
    }

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {
            // This class is not instantiable.
        }

        public static @Nullable WindowInsetsControllerCompat getWindowInsetsController(
                @NonNull View view) {
            WindowInsetsController windowInsetsController = view.getWindowInsetsController();
            return windowInsetsController != null
                    ? WindowInsetsControllerCompat.toWindowInsetsControllerCompat(
                    windowInsetsController) : null;
        }

        static void setStateDescription(View view, CharSequence stateDescription) {
            view.setStateDescription(stateDescription);
        }

        static CharSequence getStateDescription(View view) {
            return view.getStateDescription();
        }

        static void setImportantForContentCapture(View view, int mode) {
            view.setImportantForContentCapture(mode);
        }

        static boolean isImportantForContentCapture(View view) {
            return view.isImportantForContentCapture();
        }

        static int getImportantForContentCapture(View view) {
            return view.getImportantForContentCapture();
        }

        static WindowInsets dispatchApplyWindowInsets(View view, WindowInsets insets) {
            return view.dispatchApplyWindowInsets(insets);
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static void setAutofillHints(@NonNull View view, String... autofillHints) {
            view.setAutofillHints(autofillHints);
        }

        static void setTooltipText(@NonNull View view, CharSequence tooltipText) {
            view.setTooltipText(tooltipText);
        }

        static int getNextClusterForwardId(@NonNull View view) {
            return view.getNextClusterForwardId();
        }

        static void setNextClusterForwardId(View view, int nextClusterForwardId) {
            view.setNextClusterForwardId(nextClusterForwardId);
        }

        static boolean isKeyboardNavigationCluster(@NonNull View view) {
            return view.isKeyboardNavigationCluster();
        }

        static void setKeyboardNavigationCluster(@NonNull View view, boolean isCluster) {
            view.setKeyboardNavigationCluster(isCluster);
        }

        static boolean isFocusedByDefault(@NonNull View view) {
            return view.isFocusedByDefault();
        }

        static void setFocusedByDefault(@NonNull View view, boolean isFocusedByDefault) {
            view.setFocusedByDefault(isFocusedByDefault);
        }

        static View keyboardNavigationClusterSearch(@NonNull View view, View currentCluster,
                int direction) {
            return view.keyboardNavigationClusterSearch(currentCluster, direction);
        }

        static void addKeyboardNavigationClusters(@NonNull View view, Collection<View> views,
                int direction) {
            view.addKeyboardNavigationClusters(views, direction);
        }

        static boolean restoreDefaultFocus(@NonNull View view) {
            return view.restoreDefaultFocus();
        }

        static boolean hasExplicitFocusable(@NonNull View view) {
            return view.hasExplicitFocusable();
        }

        static int getImportantForAutofill(View view) {
            return view.getImportantForAutofill();
        }

        static void setImportantForAutofill(View view, int mode) {
            view.setImportantForAutofill(mode);
        }

        static boolean isImportantForAutofill(View view) {
            return view.isImportantForAutofill();
        }

        public static AutofillId getAutofillId(View view) {
            return view.getAutofillId();
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static void setPointerIcon(@NonNull View view, PointerIcon pointerIcon) {
            view.setPointerIcon(pointerIcon);
        }

        static boolean startDragAndDrop(@NonNull View view, @Nullable ClipData data,
                View.@NonNull DragShadowBuilder shadowBuilder, @Nullable Object myLocalState,
                int flags) {
            return view.startDragAndDrop(data, shadowBuilder, myLocalState, flags);
        }

        static void cancelDragAndDrop(@NonNull View view) {
            view.cancelDragAndDrop();
        }

        static void updateDragShadow(@NonNull View view,
                View.@NonNull DragShadowBuilder shadowBuilder) {
            view.updateDragShadow(shadowBuilder);
        }

        static void dispatchStartTemporaryDetach(View view) {
            view.dispatchStartTemporaryDetach();
        }

        static void dispatchFinishTemporaryDetach(View view) {
            view.dispatchFinishTemporaryDetach();
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
        static <T> T requireViewById(View view, int id) {
            return (T) view.requireViewById(id);
        }

        static CharSequence getAccessibilityPaneTitle(View view) {
            return view.getAccessibilityPaneTitle();
        }

        static void setAccessibilityPaneTitle(View view,
                CharSequence accessibilityPaneTitle) {
            view.setAccessibilityPaneTitle(accessibilityPaneTitle);
        }

        static void setAccessibilityHeading(View view, boolean isHeading) {
            view.setAccessibilityHeading(isHeading);
        }

        static boolean isAccessibilityHeading(View view) {
            return view.isAccessibilityHeading();
        }

        static boolean isScreenReaderFocusable(View view) {
            return view.isScreenReaderFocusable();
        }

        static void setScreenReaderFocusable(View view, boolean screenReaderFocusable) {
            view.setScreenReaderFocusable(screenReaderFocusable);
        }

        @SuppressWarnings("unchecked")
        static void addOnUnhandledKeyEventListener(@NonNull View v,
                final @NonNull OnUnhandledKeyEventListenerCompat listener) {
            SimpleArrayMap<OnUnhandledKeyEventListenerCompat, View.OnUnhandledKeyEventListener>
                    viewListeners = (SimpleArrayMap<OnUnhandledKeyEventListenerCompat,
                    View.OnUnhandledKeyEventListener>)
                    v.getTag(R.id.tag_unhandled_key_listeners);
            if (viewListeners == null) {
                viewListeners = new SimpleArrayMap<>();
                v.setTag(R.id.tag_unhandled_key_listeners, viewListeners);
            }

            View.OnUnhandledKeyEventListener fwListener = listener::onUnhandledKeyEvent;

            viewListeners.put(listener, fwListener);
            v.addOnUnhandledKeyEventListener(fwListener);
        }

        @SuppressWarnings("unchecked")
        static void removeOnUnhandledKeyEventListener(@NonNull View v,
                @NonNull OnUnhandledKeyEventListenerCompat listener) {
            SimpleArrayMap<OnUnhandledKeyEventListenerCompat, View.OnUnhandledKeyEventListener>
                    viewListeners = (SimpleArrayMap<OnUnhandledKeyEventListenerCompat,
                    View.OnUnhandledKeyEventListener>)
                    v.getTag(R.id.tag_unhandled_key_listeners);
            if (viewListeners == null) {
                return;
            }
            View.OnUnhandledKeyEventListener fwListener = viewListeners.get(listener);
            if (fwListener != null) {
                v.removeOnUnhandledKeyEventListener(fwListener);
            }
        }

        public static void setAutofillId(View view, AutofillIdCompat id) {
            view.setAutofillId(id == null ? null : id.toAutofillId());
        }
    }

    static class Api20Impl {
        private Api20Impl() {
            // This class is not instantiable.
        }

        static WindowInsets dispatchApplyWindowInsets(View view, WindowInsets insets) {
            return ViewGroupCompat.sCompatInsetsDispatchInstalled
                    // Dispatches insets in a way compatible with API 30+, but ignores
                    // View.OnApplyWindowInsetsListener set by the app. They should use
                    // ViewCompat.OnApplyWindowInsetsListener instead.
                    ? ViewGroupCompat.dispatchApplyWindowInsets(view, insets)
                    // Dispatches insets in the legacy way that a view can consume or modify insets
                    // to be dispatched to its siblings, but View.OnApplyWindowInsetsListener set
                    // by the app will be respected.
                    : view.dispatchApplyWindowInsets(insets);
        }
    }
}
