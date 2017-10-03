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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeProviderCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.WeakHashMap;

/**
 * Helper for accessing features in {@link View}.
 */
public class ViewCompat {
    private static final String TAG = "ViewCompat";

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({View.FOCUS_LEFT, View.FOCUS_UP, View.FOCUS_RIGHT, View.FOCUS_DOWN,
            View.FOCUS_FORWARD, View.FOCUS_BACKWARD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusDirection {}

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({View.FOCUS_LEFT, View.FOCUS_UP, View.FOCUS_RIGHT, View.FOCUS_DOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRealDirection {}

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({View.FOCUS_FORWARD, View.FOCUS_BACKWARD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRelativeDirection {}

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

    @TargetApi(Build.VERSION_CODES.O)
    @IntDef({
            View.IMPORTANT_FOR_AUTOFILL_AUTO,
            View.IMPORTANT_FOR_AUTOFILL_YES,
            View.IMPORTANT_FOR_AUTOFILL_NO,
            View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS,
            View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface AutofillImportance {}

    @IntDef({
            IMPORTANT_FOR_ACCESSIBILITY_AUTO,
            IMPORTANT_FOR_ACCESSIBILITY_YES,
            IMPORTANT_FOR_ACCESSIBILITY_NO,
            IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ImportantForAccessibility {}

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

    /**
     * The view is not important for accessibility, nor are any of its
     * descendant views.
     */
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
     */
    public static final int LAYOUT_DIRECTION_LTR = 0;

    /**
     * Horizontal layout direction of this view is from Right to Left.
     */
    public static final int LAYOUT_DIRECTION_RTL = 1;

    /**
     * Horizontal layout direction of this view is inherited from its parent.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_INHERIT = 2;

    /**
     * Horizontal layout direction of this view is from deduced from the default language
     * script for the locale. Use with {@link #setLayoutDirection}.
     */
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
     * @hide
     */
    @IntDef(value = {SCROLL_AXIS_NONE, SCROLL_AXIS_HORIZONTAL, SCROLL_AXIS_VERTICAL}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface ScrollAxis {}

    /**
     * Indicates no axis of view scrolling.
     */
    public static final int SCROLL_AXIS_NONE = 0;

    /**
     * Indicates scrolling along the horizontal axis.
     */
    public static final int SCROLL_AXIS_HORIZONTAL = 1 << 0;

    /**
     * Indicates scrolling along the vertical axis.
     */
    public static final int SCROLL_AXIS_VERTICAL = 1 << 1;

    /**
     * @hide
     */
    @IntDef({TYPE_TOUCH, TYPE_NON_TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
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

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
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

    static class ViewCompatBaseImpl {
        private static Field sMinWidthField;
        private static boolean sMinWidthFieldFetched;
        private static Field sMinHeightField;
        private static boolean sMinHeightFieldFetched;
        private static WeakHashMap<View, String> sTransitionNameMap;
        private Method mDispatchStartTemporaryDetach;
        private Method mDispatchFinishTemporaryDetach;
        private boolean mTempDetachBound;
        WeakHashMap<View, ViewPropertyAnimatorCompat> mViewPropertyAnimatorCompatMap = null;
        private static Method sChildrenDrawingOrderMethod;
        static Field sAccessibilityDelegateField;
        static boolean sAccessibilityDelegateCheckFailed = false;

        public void setAutofillHints(@NonNull View v, @Nullable String... autofillHints) {
            // no-op
        }

        public void setAccessibilityDelegate(View v,
                @Nullable AccessibilityDelegateCompat delegate) {
            v.setAccessibilityDelegate(delegate == null ? null : delegate.getBridge());
        }

        public boolean hasAccessibilityDelegate(View v) {
            if (sAccessibilityDelegateCheckFailed) {
                return false; // View implementation might have changed.
            }
            if (sAccessibilityDelegateField == null) {
                try {
                    sAccessibilityDelegateField = View.class
                            .getDeclaredField("mAccessibilityDelegate");
                    sAccessibilityDelegateField.setAccessible(true);
                } catch (Throwable t) {
                    sAccessibilityDelegateCheckFailed = true;
                    return false;
                }
            }
            try {
                return sAccessibilityDelegateField.get(v) != null;
            } catch (Throwable t) {
                sAccessibilityDelegateCheckFailed = true;
                return false;
            }
        }

        public void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info) {
            v.onInitializeAccessibilityNodeInfo(info.unwrap());
        }

        @SuppressWarnings("deprecation")
        public boolean startDragAndDrop(View v, ClipData data, View.DragShadowBuilder shadowBuilder,
                Object localState, int flags) {
            return v.startDrag(data, shadowBuilder, localState, flags);
        }

        public void cancelDragAndDrop(View v) {
            // no-op
        }

        public void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder) {
            // no-op
        }

        public boolean hasTransientState(View view) {
            // A view can't have transient state if transient state wasn't supported.
            return false;
        }

        public void setHasTransientState(View view, boolean hasTransientState) {
            // Do nothing; API doesn't exist
        }

        public void postInvalidateOnAnimation(View view) {
            view.postInvalidate();
        }

        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            view.postInvalidate(left, top, right, bottom);
        }

        public void postOnAnimation(View view, Runnable action) {
            view.postDelayed(action, getFrameTime());
        }

        public void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
            view.postDelayed(action, getFrameTime() + delayMillis);
        }

        long getFrameTime() {
            return ValueAnimator.getFrameDelay();
        }

        public int getImportantForAccessibility(View view) {
            return 0;
        }

        public void setImportantForAccessibility(View view, int mode) {
        }

        public boolean isImportantForAccessibility(View view) {
            return true;
        }

        public boolean performAccessibilityAction(View view, int action, Bundle arguments) {
            return false;
        }

        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
            return null;
        }

        public int getLabelFor(View view) {
            return 0;
        }

        public void setLabelFor(View view, int id) {
        }

        public void setLayerPaint(View view, Paint paint) {
            // Make sure the paint is correct; this will be cheap if it's the same
            // instance as was used to call setLayerType earlier.
            view.setLayerType(view.getLayerType(), paint);
            // This is expensive, but the only way to accomplish this before JB-MR1.
            view.invalidate();
        }

        public int getLayoutDirection(View view) {
            return LAYOUT_DIRECTION_LTR;
        }

        public void setLayoutDirection(View view, int layoutDirection) {
            // No-op
        }

        public ViewParent getParentForAccessibility(View view) {
            return view.getParent();
        }

        public int getAccessibilityLiveRegion(View view) {
            return ACCESSIBILITY_LIVE_REGION_NONE;
        }

        public void setAccessibilityLiveRegion(View view, int mode) {
            // No-op
        }

        public int getPaddingStart(View view) {
            return view.getPaddingLeft();
        }

        public int getPaddingEnd(View view) {
            return view.getPaddingRight();
        }

        public void setPaddingRelative(View view, int start, int top, int end, int bottom) {
            view.setPadding(start, top, end, bottom);
        }

        public void dispatchStartTemporaryDetach(View view) {
            if (!mTempDetachBound) {
                bindTempDetach();
            }
            if (mDispatchStartTemporaryDetach != null) {
                try {
                    mDispatchStartTemporaryDetach.invoke(view);
                } catch (Exception e) {
                    Log.d(TAG, "Error calling dispatchStartTemporaryDetach", e);
                }
            } else {
                // Try this instead
                view.onStartTemporaryDetach();
            }
        }

        public void dispatchFinishTemporaryDetach(View view) {
            if (!mTempDetachBound) {
                bindTempDetach();
            }
            if (mDispatchFinishTemporaryDetach != null) {
                try {
                    mDispatchFinishTemporaryDetach.invoke(view);
                } catch (Exception e) {
                    Log.d(TAG, "Error calling dispatchFinishTemporaryDetach", e);
                }
            } else {
                // Try this instead
                view.onFinishTemporaryDetach();
            }
        }

        public boolean hasOverlappingRendering(View view) {
            return true;
        }

        private void bindTempDetach() {
            try {
                mDispatchStartTemporaryDetach = View.class.getDeclaredMethod(
                        "dispatchStartTemporaryDetach");
                mDispatchFinishTemporaryDetach = View.class.getDeclaredMethod(
                        "dispatchFinishTemporaryDetach");
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Couldn't find method", e);
            }
            mTempDetachBound = true;
        }

        public int getMinimumWidth(View view) {
            if (!sMinWidthFieldFetched) {
                try {
                    sMinWidthField = View.class.getDeclaredField("mMinWidth");
                    sMinWidthField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // Couldn't find the field. Abort!
                }
                sMinWidthFieldFetched = true;
            }

            if (sMinWidthField != null) {
                try {
                    return (int) sMinWidthField.get(view);
                } catch (Exception e) {
                    // Field get failed. Oh well...
                }
            }

            // We failed, return 0
            return 0;
        }

        public int getMinimumHeight(View view) {
            if (!sMinHeightFieldFetched) {
                try {
                    sMinHeightField = View.class.getDeclaredField("mMinHeight");
                    sMinHeightField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // Couldn't find the field. Abort!
                }
                sMinHeightFieldFetched = true;
            }

            if (sMinHeightField != null) {
                try {
                    return (int) sMinHeightField.get(view);
                } catch (Exception e) {
                    // Field get failed. Oh well...
                }
            }

            // We failed, return 0
            return 0;
        }

        public ViewPropertyAnimatorCompat animate(View view) {
            if (mViewPropertyAnimatorCompatMap == null) {
                mViewPropertyAnimatorCompatMap = new WeakHashMap<>();
            }
            ViewPropertyAnimatorCompat vpa = mViewPropertyAnimatorCompatMap.get(view);
            if (vpa == null) {
                vpa = new ViewPropertyAnimatorCompat(view);
                mViewPropertyAnimatorCompatMap.put(view, vpa);
            }
            return vpa;
        }

        public void setTransitionName(View view, String transitionName) {
            if (sTransitionNameMap == null) {
                sTransitionNameMap = new WeakHashMap<>();
            }
            sTransitionNameMap.put(view, transitionName);
        }

        public String getTransitionName(View view) {
            if (sTransitionNameMap == null) {
                return null;
            }
            return sTransitionNameMap.get(view);
        }

        public int getWindowSystemUiVisibility(View view) {
            return 0;
        }

        public void requestApplyInsets(View view) {
        }

        public void setElevation(View view, float elevation) {
        }

        public float getElevation(View view) {
            return 0f;
        }

        public void setTranslationZ(View view, float translationZ) {
        }

        public float getTranslationZ(View view) {
            return 0f;
        }

        public void setClipBounds(View view, Rect clipBounds) {
        }

        public Rect getClipBounds(View view) {
            return null;
        }

        public void setChildrenDrawingOrderEnabled(ViewGroup viewGroup, boolean enabled) {
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

        public boolean getFitsSystemWindows(View view) {
            return false;
        }

        public void setOnApplyWindowInsetsListener(View view,
                OnApplyWindowInsetsListener listener) {
            // noop
        }

        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            return insets;
        }

        public WindowInsetsCompat dispatchApplyWindowInsets(View v, WindowInsetsCompat insets) {
            return insets;
        }

        public boolean isPaddingRelative(View view) {
            return false;
        }

        public void setNestedScrollingEnabled(View view, boolean enabled) {
            if (view instanceof NestedScrollingChild) {
                ((NestedScrollingChild) view).setNestedScrollingEnabled(enabled);
            }
        }

        public boolean isNestedScrollingEnabled(View view) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).isNestedScrollingEnabled();
            }
            return false;
        }

        public void setBackground(View view, Drawable background) {
            view.setBackgroundDrawable(background);
        }

        public ColorStateList getBackgroundTintList(View view) {
            return (view instanceof TintableBackgroundView)
                    ? ((TintableBackgroundView) view).getSupportBackgroundTintList()
                    : null;
        }

        public void setBackgroundTintList(View view, ColorStateList tintList) {
            if (view instanceof TintableBackgroundView) {
                ((TintableBackgroundView) view).setSupportBackgroundTintList(tintList);
            }
        }

        public void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
            if (view instanceof TintableBackgroundView) {
                ((TintableBackgroundView) view).setSupportBackgroundTintMode(mode);
            }
        }

        public PorterDuff.Mode getBackgroundTintMode(View view) {
            return (view instanceof TintableBackgroundView)
                    ? ((TintableBackgroundView) view).getSupportBackgroundTintMode()
                    : null;
        }

        public boolean startNestedScroll(View view, int axes) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).startNestedScroll(axes);
            }
            return false;
        }

        public void stopNestedScroll(View view) {
            if (view instanceof NestedScrollingChild) {
                ((NestedScrollingChild) view).stopNestedScroll();
            }
        }

        public boolean hasNestedScrollingParent(View view) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).hasNestedScrollingParent();
            }
            return false;
        }

        public boolean dispatchNestedScroll(View view, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).dispatchNestedScroll(dxConsumed, dyConsumed,
                        dxUnconsumed, dyUnconsumed, offsetInWindow);
            }
            return false;
        }

        public boolean dispatchNestedPreScroll(View view, int dx, int dy,
                int[] consumed, int[] offsetInWindow) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).dispatchNestedPreScroll(dx, dy, consumed,
                        offsetInWindow);
            }
            return false;
        }

        public boolean dispatchNestedFling(View view, float velocityX, float velocityY,
                boolean consumed) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).dispatchNestedFling(velocityX, velocityY,
                        consumed);
            }
            return false;
        }

        public boolean dispatchNestedPreFling(View view, float velocityX, float velocityY) {
            if (view instanceof NestedScrollingChild) {
                return ((NestedScrollingChild) view).dispatchNestedPreFling(velocityX, velocityY);
            }
            return false;
        }

        public boolean isInLayout(View view) {
            return false;
        }

        public boolean isLaidOut(View view) {
            return view.getWidth() > 0 && view.getHeight() > 0;
        }

        public boolean isLayoutDirectionResolved(View view) {
            return false;
        }

        public float getZ(View view) {
            return getTranslationZ(view) + getElevation(view);
        }

        public void setZ(View view, float z) {
            // no-op
        }

        public boolean isAttachedToWindow(View view) {
            return view.getWindowToken() != null;
        }

        public boolean hasOnClickListeners(View view) {
            return false;
        }

        public int getScrollIndicators(View view) {
            return 0;
        }

        public void setScrollIndicators(View view, int indicators) {
            // no-op
        }

        public void setScrollIndicators(View view, int indicators, int mask) {
            // no-op
        }

        public void offsetLeftAndRight(View view, int offset) {
            view.offsetLeftAndRight(offset);
            if (view.getVisibility() == View.VISIBLE) {
                tickleInvalidationFlag(view);

                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    tickleInvalidationFlag((View) parent);
                }
            }
        }

        public void offsetTopAndBottom(View view, int offset) {
            view.offsetTopAndBottom(offset);
            if (view.getVisibility() == View.VISIBLE) {
                tickleInvalidationFlag(view);

                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    tickleInvalidationFlag((View) parent);
                }
            }
        }

        private static void tickleInvalidationFlag(View view) {
            final float y = view.getTranslationY();
            view.setTranslationY(y + 1);
            view.setTranslationY(y);
        }

        public void setPointerIcon(View view, PointerIconCompat pointerIcon) {
            // no-op
        }

        public Display getDisplay(View view) {
            if (isAttachedToWindow(view)) {
                final WindowManager wm = (WindowManager) view.getContext().getSystemService(
                        Context.WINDOW_SERVICE);
                return wm.getDefaultDisplay();
            }
            return null;
        }

        public void setTooltipText(View view, CharSequence tooltipText) {
        }

        public int getNextClusterForwardId(@NonNull View view) {
            return View.NO_ID;
        }

        public void setNextClusterForwardId(@NonNull View view, int nextClusterForwardId) {
            // no-op
        }

        public boolean isKeyboardNavigationCluster(@NonNull View view) {
            return false;
        }

        public void setKeyboardNavigationCluster(@NonNull View view, boolean isCluster) {
            // no-op
        }

        public boolean isFocusedByDefault(@NonNull View view) {
            return false;
        }

        public void setFocusedByDefault(@NonNull View view, boolean isFocusedByDefault) {
            // no-op
        }

        public View keyboardNavigationClusterSearch(@NonNull View view, View currentCluster,
                @FocusDirection int direction) {
            return null;
        }

        public void addKeyboardNavigationClusters(@NonNull View view,
                @NonNull Collection<View> views, int direction) {
            // no-op
        }

        public boolean restoreDefaultFocus(@NonNull View view) {
            return view.requestFocus();
        }

        public boolean hasExplicitFocusable(@NonNull View view) {
            return view.hasFocusable();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public @AutofillImportance int getImportantForAutofill(@NonNull View v) {
            return View.IMPORTANT_FOR_AUTOFILL_AUTO;
        }

        public void setImportantForAutofill(@NonNull View v, @AutofillImportance int mode) {
            // no-op
        }

        public boolean isImportantForAutofill(@NonNull View v) {
            return true;
        }
    }

    @RequiresApi(15)
    static class ViewCompatApi15Impl extends ViewCompatBaseImpl {
        @Override
        public boolean hasOnClickListeners(View view) {
            return view.hasOnClickListeners();
        }
    }

    @RequiresApi(16)
    static class ViewCompatApi16Impl extends ViewCompatApi15Impl {
        @Override
        public boolean hasTransientState(View view) {
            return view.hasTransientState();
        }
        @Override
        public void setHasTransientState(View view, boolean hasTransientState) {
            view.setHasTransientState(hasTransientState);
        }
        @Override
        public void postInvalidateOnAnimation(View view) {
            view.postInvalidateOnAnimation();
        }
        @Override
        public void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
            view.postInvalidateOnAnimation(left, top, right, bottom);
        }
        @Override
        public void postOnAnimation(View view, Runnable action) {
            view.postOnAnimation(action);
        }
        @Override
        public void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
            view.postOnAnimationDelayed(action, delayMillis);
        }
        @Override
        public int getImportantForAccessibility(View view) {
            return view.getImportantForAccessibility();
        }
        @Override
        public void setImportantForAccessibility(View view, int mode) {
            // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS is not available
            // on this platform so replace with IMPORTANT_FOR_ACCESSIBILITY_NO
            // which is closer semantically.
            if (mode == IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS) {
                mode = IMPORTANT_FOR_ACCESSIBILITY_NO;
            }
            //noinspection WrongConstant
            view.setImportantForAccessibility(mode);
        }
        @Override
        public boolean performAccessibilityAction(View view, int action, Bundle arguments) {
            return view.performAccessibilityAction(action, arguments);
        }
        @Override
        public AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            if (provider != null) {
                return new AccessibilityNodeProviderCompat(provider);
            }
            return null;
        }

        @Override
        public ViewParent getParentForAccessibility(View view) {
            return view.getParentForAccessibility();
        }

        @Override
        public int getMinimumWidth(View view) {
            return view.getMinimumWidth();
        }

        @Override
        public int getMinimumHeight(View view) {
            return view.getMinimumHeight();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void requestApplyInsets(View view) {
            view.requestFitSystemWindows();
        }

        @Override
        public boolean getFitsSystemWindows(View view) {
            return view.getFitsSystemWindows();
        }

        @Override
        public boolean hasOverlappingRendering(View view) {
            return view.hasOverlappingRendering();
        }

        @Override
        public void setBackground(View view, Drawable background) {
            view.setBackground(background);
        }
    }

    @RequiresApi(17)
    static class ViewCompatApi17Impl extends ViewCompatApi16Impl {

        @Override
        public int getLabelFor(View view) {
            return view.getLabelFor();
        }

        @Override
        public void setLabelFor(View view, int id) {
            view.setLabelFor(id);
        }

        @Override
        public void setLayerPaint(View view, Paint paint) {
            view.setLayerPaint(paint);
        }

        @Override
        public int getLayoutDirection(View view) {
            return view.getLayoutDirection();
        }

        @Override
        public void setLayoutDirection(View view, int layoutDirection) {
            view.setLayoutDirection(layoutDirection);
        }

        @Override
        public int getPaddingStart(View view) {
            return view.getPaddingStart();
        }

        @Override
        public int getPaddingEnd(View view) {
            return view.getPaddingEnd();
        }

        @Override
        public void setPaddingRelative(View view, int start, int top, int end, int bottom) {
            view.setPaddingRelative(start, top, end, bottom);
        }

        @Override
        public int getWindowSystemUiVisibility(View view) {
            return view.getWindowSystemUiVisibility();
        }

        @Override
        public boolean isPaddingRelative(View view) {
            return view.isPaddingRelative();
        }

        @Override
        public Display getDisplay(View view) {
            return view.getDisplay();
        }
    }

    @RequiresApi(18)
    static class ViewCompatApi18Impl extends ViewCompatApi17Impl {
        @Override
        public void setClipBounds(View view, Rect clipBounds) {
            view.setClipBounds(clipBounds);
        }

        @Override
        public Rect getClipBounds(View view) {
            return view.getClipBounds();
        }

        @Override
        public boolean isInLayout(View view) {
            return view.isInLayout();
        }
    }

    @RequiresApi(19)
    static class ViewCompatApi19Impl extends ViewCompatApi18Impl {
        @Override
        public int getAccessibilityLiveRegion(View view) {
            return view.getAccessibilityLiveRegion();
        }

        @Override
        public void setAccessibilityLiveRegion(View view, int mode) {
            view.setAccessibilityLiveRegion(mode);
        }

        @Override
        public void setImportantForAccessibility(View view, int mode) {
            view.setImportantForAccessibility(mode);
        }

        @Override
        public boolean isLaidOut(View view) {
            return view.isLaidOut();
        }

        @Override
        public boolean isLayoutDirectionResolved(View view) {
            return view.isLayoutDirectionResolved();
        }

        @Override
        public boolean isAttachedToWindow(View view) {
            return view.isAttachedToWindow();
        }
    }

    @RequiresApi(21)
    static class ViewCompatApi21Impl extends ViewCompatApi19Impl {
        private static ThreadLocal<Rect> sThreadLocalRect;

        @Override
        public void setTransitionName(View view, String transitionName) {
            view.setTransitionName(transitionName);
        }

        @Override
        public String getTransitionName(View view) {
            return view.getTransitionName();
        }

        @Override
        public void requestApplyInsets(View view) {
            view.requestApplyInsets();
        }

        @Override
        public void setElevation(View view, float elevation) {
            view.setElevation(elevation);
        }

        @Override
        public float getElevation(View view) {
            return view.getElevation();
        }

        @Override
        public void setTranslationZ(View view, float translationZ) {
            view.setTranslationZ(translationZ);
        }

        @Override
        public float getTranslationZ(View view) {
            return view.getTranslationZ();
        }

        @Override
        public void setOnApplyWindowInsetsListener(View view,
                final OnApplyWindowInsetsListener listener) {
            if (listener == null) {
                view.setOnApplyWindowInsetsListener(null);
                return;
            }

            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    WindowInsetsCompat compatInsets = WindowInsetsCompat.wrap(insets);
                    compatInsets = listener.onApplyWindowInsets(view, compatInsets);
                    return (WindowInsets) WindowInsetsCompat.unwrap(compatInsets);
                }
            });
        }

        @Override
        public void setNestedScrollingEnabled(View view, boolean enabled) {
            view.setNestedScrollingEnabled(enabled);
        }

        @Override
        public boolean isNestedScrollingEnabled(View view) {
            return view.isNestedScrollingEnabled();
        }

        @Override
        public boolean startNestedScroll(View view, int axes) {
            return view.startNestedScroll(axes);
        }

        @Override
        public void stopNestedScroll(View view) {
            view.stopNestedScroll();
        }

        @Override
        public boolean hasNestedScrollingParent(View view) {
            return view.hasNestedScrollingParent();
        }

        @Override
        public boolean dispatchNestedScroll(View view, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
            return view.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    offsetInWindow);
        }

        @Override
        public boolean dispatchNestedPreScroll(View view, int dx, int dy,
                int[] consumed, int[] offsetInWindow) {
            return view.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        }

        @Override
        public boolean dispatchNestedFling(View view, float velocityX, float velocityY,
                boolean consumed) {
            return view.dispatchNestedFling(velocityX, velocityY, consumed);
        }

        @Override
        public boolean dispatchNestedPreFling(View view, float velocityX, float velocityY) {
            return view.dispatchNestedPreFling(velocityX, velocityY);
        }

        @Override
        public boolean isImportantForAccessibility(View view) {
            return view.isImportantForAccessibility();
        }

        @Override
        public ColorStateList getBackgroundTintList(View view) {
            return view.getBackgroundTintList();
        }

        @Override
        public void setBackgroundTintList(View view, ColorStateList tintList) {
            view.setBackgroundTintList(tintList);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the background
                // after applying the tint
                Drawable background = view.getBackground();
                boolean hasTint = (view.getBackgroundTintList() != null)
                        && (view.getBackgroundTintMode() != null);
                if ((background != null) && hasTint) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        }

        @Override
        public void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
            view.setBackgroundTintMode(mode);

            if (Build.VERSION.SDK_INT == 21) {
                // Work around a bug in L that did not update the state of the background
                // after applying the tint
                Drawable background = view.getBackground();
                boolean hasTint = (view.getBackgroundTintList() != null)
                        && (view.getBackgroundTintMode() != null);
                if ((background != null) && hasTint) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        }

        @Override
        public PorterDuff.Mode getBackgroundTintMode(View view) {
            return view.getBackgroundTintMode();
        }

        @Override
        public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            WindowInsets unwrapped = (WindowInsets)  WindowInsetsCompat.unwrap(insets);
            WindowInsets result = v.onApplyWindowInsets(unwrapped);
            if (result != unwrapped) {
                unwrapped = new WindowInsets(result);
            }
            return WindowInsetsCompat.wrap(unwrapped);
        }

        @Override
        public WindowInsetsCompat dispatchApplyWindowInsets(View v, WindowInsetsCompat insets) {
            WindowInsets unwrapped = (WindowInsets) WindowInsetsCompat.unwrap(insets);
            WindowInsets result = v.dispatchApplyWindowInsets(unwrapped);
            if (result != unwrapped) {
                unwrapped = new WindowInsets(result);
            }
            return WindowInsetsCompat.wrap(unwrapped);
        }

        @Override
        public float getZ(View view) {
            return view.getZ();
        }

        @Override
        public void setZ(View view, float z) {
            view.setZ(z);
        }

        @Override
        public void offsetLeftAndRight(View view, int offset) {
            final Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;

            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                // If the view currently does not currently intersect the parent (and is therefore
                // not displayed) we may need need to invalidate
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                        view.getRight(), view.getBottom());
            }

            // Now offset, invoking the API 11+ implementation (which contains its own workarounds)
            super.offsetLeftAndRight(view, offset);

            // The view has now been offset, so let's intersect the Rect and invalidate where
            // the View is now displayed
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        }

        @Override
        public void offsetTopAndBottom(View view, int offset) {
            final Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;

            final ViewParent parent = view.getParent();
            if (parent instanceof View) {
                final View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                // If the view currently does not currently intersect the parent (and is therefore
                // not displayed) we may need need to invalidate
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(),
                        view.getRight(), view.getBottom());
            }

            // Now offset, invoking the API 11+ implementation (which contains its own workarounds)
            super.offsetTopAndBottom(view, offset);

            // The view has now been offset, so let's intersect the Rect and invalidate where
            // the View is now displayed
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        }

        private static Rect getEmptyTempRect() {
            if (sThreadLocalRect == null) {
                sThreadLocalRect = new ThreadLocal<>();
            }
            Rect rect = sThreadLocalRect.get();
            if (rect == null) {
                rect = new Rect();
                sThreadLocalRect.set(rect);
            }
            rect.setEmpty();
            return rect;
        }
    }

    @RequiresApi(23)
    static class ViewCompatApi23Impl extends ViewCompatApi21Impl {
        @Override
        public void setScrollIndicators(View view, int indicators) {
            view.setScrollIndicators(indicators);
        }

        @Override
        public void setScrollIndicators(View view, int indicators, int mask) {
            view.setScrollIndicators(indicators, mask);
        }

        @Override
        public int getScrollIndicators(View view) {
            return view.getScrollIndicators();
        }


        @Override
        public void offsetLeftAndRight(View view, int offset) {
            view.offsetLeftAndRight(offset);
        }

        @Override
        public void offsetTopAndBottom(View view, int offset) {
            view.offsetTopAndBottom(offset);
        }
    }

    @RequiresApi(24)
    static class ViewCompatApi24Impl extends ViewCompatApi23Impl {
        @Override
        public void dispatchStartTemporaryDetach(View view) {
            view.dispatchStartTemporaryDetach();
        }

        @Override
        public void dispatchFinishTemporaryDetach(View view) {
            view.dispatchFinishTemporaryDetach();
        }

        @Override
        public void setPointerIcon(View view, PointerIconCompat pointerIconCompat) {
            view.setPointerIcon((PointerIcon) (pointerIconCompat != null
                    ? pointerIconCompat.getPointerIcon() : null));
        }

        @Override
        public boolean startDragAndDrop(View view, ClipData data,
                View.DragShadowBuilder shadowBuilder, Object localState, int flags) {
            return view.startDragAndDrop(data, shadowBuilder, localState, flags);
        }

        @Override
        public void cancelDragAndDrop(View view) {
            view.cancelDragAndDrop();
        }

        @Override
        public void updateDragShadow(View view, View.DragShadowBuilder shadowBuilder) {
            view.updateDragShadow(shadowBuilder);
        }
    }

    @RequiresApi(26)
    static class ViewCompatApi26Impl extends ViewCompatApi24Impl {

        @Override
        public void setAutofillHints(@NonNull View v, @Nullable String... autofillHints) {
            v.setAutofillHints(autofillHints);
        }

        @Override
        public @AutofillImportance int getImportantForAutofill(@NonNull View v) {
            return v.getImportantForAutofill();
        }

        @Override
        public void setImportantForAutofill(@NonNull View v, @AutofillImportance int mode) {
            v.setImportantForAutofill(mode);
        }

        @Override
        public boolean isImportantForAutofill(@NonNull View v) {
            return v.isImportantForAutofill();
        }

        @Override
        public void setTooltipText(View view, CharSequence tooltipText) {
            view.setTooltipText(tooltipText);
        }

        @Override
        public int getNextClusterForwardId(@NonNull View view) {
            return view.getNextClusterForwardId();
        }

        @Override
        public void setNextClusterForwardId(@NonNull View view, int nextClusterForwardId) {
            view.setNextClusterForwardId(nextClusterForwardId);
        }

        @Override
        public boolean isKeyboardNavigationCluster(@NonNull View view) {
            return view.isKeyboardNavigationCluster();
        }

        @Override
        public void setKeyboardNavigationCluster(@NonNull View view, boolean isCluster) {
            view.setKeyboardNavigationCluster(isCluster);
        }

        @Override
        public boolean isFocusedByDefault(@NonNull View view) {
            return view.isFocusedByDefault();
        }

        @Override
        public void setFocusedByDefault(@NonNull View view, boolean isFocusedByDefault) {
            view.setFocusedByDefault(isFocusedByDefault);
        }

        @Override
        public View keyboardNavigationClusterSearch(@NonNull View view, View currentCluster,
                @FocusDirection int direction) {
            return view.keyboardNavigationClusterSearch(currentCluster, direction);
        }

        @Override
        public void addKeyboardNavigationClusters(@NonNull View view,
                @NonNull Collection<View> views, int direction) {
            view.addKeyboardNavigationClusters(views, direction);
        }

        @Override
        public boolean restoreDefaultFocus(@NonNull View view) {
            return view.restoreDefaultFocus();
        }

        @Override
        public boolean hasExplicitFocusable(@NonNull View view) {
            return view.hasExplicitFocusable();
        }
    }

    static final ViewCompatBaseImpl IMPL;
    static {
        if (Build.VERSION.SDK_INT >= 26) {
            IMPL = new ViewCompatApi26Impl();
        } else if (Build.VERSION.SDK_INT >= 24) {
            IMPL = new ViewCompatApi24Impl();
        } else if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new ViewCompatApi23Impl();
        } else if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new ViewCompatApi21Impl();
        } else if (Build.VERSION.SDK_INT >= 19) {
            IMPL = new ViewCompatApi19Impl();
        } else if (Build.VERSION.SDK_INT >= 18) {
            IMPL = new ViewCompatApi18Impl();
        } else if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new ViewCompatApi17Impl();
        } else if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new ViewCompatApi16Impl();
        } else if (Build.VERSION.SDK_INT >= 15) {
            IMPL = new ViewCompatApi15Impl();
        } else {
            IMPL = new ViewCompatBaseImpl();
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
     *
     * @deprecated Use {@link View#canScrollVertically(int)} directly.
     */
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
     * @param v The View against which to invoke the method.
     * @return This view's over-scroll mode.
     * @deprecated Call {@link View#getOverScrollMode()} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    @OverScroll
    public static int getOverScrollMode(View v) {
        //noinspection ResourceType
        return v.getOverScrollMode();
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
     * @deprecated Call {@link View#setOverScrollMode(int)} directly. This method will be
     * removed in a future release.
     */
    @Deprecated
    public static void setOverScrollMode(View v, @OverScroll int overScrollMode) {
        v.setOverScrollMode(overScrollMode);
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
     */
    public static void onInitializeAccessibilityNodeInfo(View v, AccessibilityNodeInfoCompat info) {
        IMPL.onInitializeAccessibilityNodeInfo(v, info);
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
     *
     * @param delegate the object to which accessibility method calls should be
     *                 delegated
     * @see AccessibilityDelegateCompat
     */
    public static void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        IMPL.setAccessibilityDelegate(v, delegate);
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
     * @param autofillHints The autofill hints to set. If the array is emtpy, {@code null} is set.
     * @attr ref android.R.styleable#View_autofillHints
     */
    public static void setAutofillHints(@NonNull View v, @Nullable String... autofillHints) {
        IMPL.setAutofillHints(v, autofillHints);
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
     * @return {@link View#IMPORTANT_FOR_AUTOFILL_AUTO} by default, or value passed to
     * {@link #setImportantForAutofill(View, int)}.
     *
     * @attr ref android.R.styleable#View_importantForAutofill
     */
    public static @AutofillImportance int getImportantForAutofill(@NonNull View v) {
        return IMPL.getImportantForAutofill(v);
    }

    /**
     * Sets the mode for determining whether this view is considered important for autofill.
     *
     * <p>The platform determines the importance for autofill automatically but you
     * can use this method to customize the behavior. For example:
     *
     * <ol>
     *   <li>When the view contents is irrelevant for autofill (for example, a text field used in a
     *       "Captcha" challenge), it should be {@link View#IMPORTANT_FOR_AUTOFILL_NO}.
     *   <li>When both the view and its children are irrelevant for autofill (for example, the root
     *       view of an activity containing a spreadhseet editor), it should be
     *       {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS}.
     *   <li>When the view content is relevant for autofill but its children aren't (for example,
     *       a credit card expiration date represented by a custom view that overrides the proper
     *       autofill methods and has 2 children representing the month and year), it should
     *       be {@link View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS}.
     * </ol>
     *
     * <p><b>NOTE:</strong> setting the mode as does {@link View#IMPORTANT_FOR_AUTOFILL_NO} or
     * {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS} does not guarantee the view (and
     * its children) will be always be considered not important; for example, when the user
     * explicitly makes an autofill request, all views are considered important. See
     * {@link #isImportantForAutofill(View)} for more details about how the View's importance for
     * autofill is used.
     *
     * <p>This method is only supported on API >= 26.
     * On API 25 and below, it is a no-op</p>
     *
     *
     * @param mode {@link View#IMPORTANT_FOR_AUTOFILL_AUTO},
     * {@link View#IMPORTANT_FOR_AUTOFILL_YES},
     * {@link View#IMPORTANT_FOR_AUTOFILL_NO},
     * {@link View#IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS},
     * or {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS}.
     *
     * @attr ref android.R.styleable#View_importantForAutofill
     */
    public static void setImportantForAutofill(@NonNull View v, @AutofillImportance int mode) {
        IMPL.setImportantForAutofill(v, mode);
    }

    /**
     * Hints the Android System whether the {@link android.app.assist.AssistStructure.ViewNode}
     * associated with this view is considered important for autofill purposes.
     *
     * <p>Generally speaking, a view is important for autofill if:
     * <ol>
     * <li>The view can be autofilled by an {@link android.service.autofill.AutofillService}.
     * <li>The view contents can help an {@link android.service.autofill.AutofillService}
     *     determine how other views can be autofilled.
     * <ol>
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
     *       then it returns {@code true}
     *   <li>if it returns {@link View#IMPORTANT_FOR_AUTOFILL_NO} or
     *       {@link View#IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS},
     *       then it returns {@code false}
     *   <li>if it returns {@link View#IMPORTANT_FOR_AUTOFILL_AUTO},
     *   then it uses some simple heuristics that can return {@code true}
     *   in some cases (like a container with a resource id), but {@code false} in most.
     *   <li>otherwise, it returns {@code false}.
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
        return IMPL.isImportantForAutofill(v);
    }

    /**
     * Checks whether provided View has an accessibility delegate attached to it.
     *
     * @param v The View instance to check
     * @return True if the View has an accessibility delegate
     */
    public static boolean hasAccessibilityDelegate(View v) {
        return IMPL.hasAccessibilityDelegate(v);
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
     * @see #IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
     * @see #IMPORTANT_FOR_ACCESSIBILITY_AUTO
     */
    @ImportantForAccessibility
    public static int getImportantForAccessibility(View view) {
        //noinspection ResourceType
        return IMPL.getImportantForAccessibility(view);
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
     */
    public static void setImportantForAccessibility(View view,
            @ImportantForAccessibility int mode) {
        IMPL.setImportantForAccessibility(view, mode);
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
     * </code>
     * <li>{@link #IMPORTANT_FOR_ACCESSIBILITY_YES}, return <code>true</code>
     * <li>{@link #IMPORTANT_FOR_ACCESSIBILITY_AUTO}, return <code>true</code> if
     * view satisfies any of the following:
     * <ul>
     * <li>Is actionable, e.g. {@link View#isClickable()},
     * {@link View#isLongClickable()}, or {@link View#isFocusable()}
     * <li>Has an {@link AccessibilityDelegateCompat}
     * <li>Has an interaction listener, e.g. {@link View.OnTouchListener},
     * {@link View.OnKeyListener}, etc.
     * <li>Is an accessibility live region, e.g.
     * {@link #getAccessibilityLiveRegion(View)} is not
     * {@link #ACCESSIBILITY_LIVE_REGION_NONE}.
     * </ul>
     * </ol>
     * <p>
     * <em>Note:</em> Prior to API 21, this method will always return {@code true}.
     *
     * @return Whether the view is exposed for accessibility.
     * @see #setImportantForAccessibility(View, int)
     * @see #getImportantForAccessibility(View)
     */
    public static boolean isImportantForAccessibility(View view) {
        return IMPL.isImportantForAccessibility(view);
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
     *
     * @param action The action to perform.
     * @param arguments Optional action arguments.
     * @return Whether the action was performed.
     */
    public static boolean performAccessibilityAction(View view, int action, Bundle arguments) {
        return IMPL.performAccessibilityAction(view, action, arguments);
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
    public static AccessibilityNodeProviderCompat getAccessibilityNodeProvider(View view) {
        return IMPL.getAccessibilityNodeProvider(view);
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
     */
    public static int getLabelFor(View view) {
        return IMPL.getLabelFor(view);
    }

    /**
     * Sets the id of a view for which a given view serves as a label for
     * accessibility purposes.
     *
     * @param view The view on which to invoke the corresponding method.
     * @param labeledId The labeled view id.
     */
    public static void setLabelFor(View view, @IdRes int labeledId) {
        IMPL.setLabelFor(view, labeledId);
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
     */
    public static void setLayerPaint(View view, Paint paint) {
        IMPL.setLayerPaint(view, paint);
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
     */
    @ResolvedLayoutDirectionMode
    public static int getLayoutDirection(View view) {
        //noinspection ResourceType
        return IMPL.getLayoutDirection(view);
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
     */
    public static void setLayoutDirection(View view, @LayoutDirectionMode int layoutDirection) {
        IMPL.setLayoutDirection(view, layoutDirection);
    }

    /**
     * Gets the parent for accessibility purposes. Note that the parent for
     * accessibility is not necessary the immediate parent. It is the first
     * predecessor that is important for accessibility.
     *
     * @param view View to retrieve parent for
     * @return The parent for use in accessibility inspection
     */
    public static ViewParent getParentForAccessibility(View view) {
        return IMPL.getParentForAccessibility(view);
    }

    /**
     * Indicates whether this View is opaque. An opaque View guarantees that it will
     * draw all the pixels overlapping its bounds using a fully opaque color.
     *
     * @return True if this View is guaranteed to be fully opaque, false otherwise.
     * @deprecated Use {@link View#isOpaque()} directly. This method will be
     * removed in a future release.
     */
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
     */
    @AccessibilityLiveRegion
    public static int getAccessibilityLiveRegion(View view) {
        //noinspection ResourceType
        return IMPL.getAccessibilityLiveRegion(view);
    }

    /**
     * Sets the live region mode for the specified view. This indicates to
     * accessibility services whether they should automatically notify the user
     * about changes to the view's content description or text, or to the
     * content descriptions or text of the view's children (where applicable).
     * <p>
     * For example, in a login screen with a TextView that displays an "incorrect
     * password" notification, that view should be marked as a live region with
     * mode {@link #ACCESSIBILITY_LIVE_REGION_POLITE}.
     * <p>
     * To disable change notifications for this view, use
     * {@link #ACCESSIBILITY_LIVE_REGION_NONE}. This is the default live region
     * mode for most views.
     * <p>
     * To indicate that the user should be notified of changes, use
     * {@link #ACCESSIBILITY_LIVE_REGION_POLITE}.
     * <p>
     * If the view's changes should interrupt ongoing speech and notify the user
     * immediately, use {@link #ACCESSIBILITY_LIVE_REGION_ASSERTIVE}.
     *
     * @param view The view on which to set the live region mode
     * @param mode The live region mode for this view, one of:
     *        <ul>
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_NONE}
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_POLITE}
     *        <li>{@link #ACCESSIBILITY_LIVE_REGION_ASSERTIVE}
     *        </ul>
     */
    public static void setAccessibilityLiveRegion(View view, @AccessibilityLiveRegion int mode) {
        IMPL.setAccessibilityLiveRegion(view, mode);
    }

    /**
     * Returns the start padding of the specified view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @param view The view to get padding for
     * @return the start padding in pixels
     */
    public static int getPaddingStart(View view) {
        return IMPL.getPaddingStart(view);
    }

    /**
     * Returns the end padding of the specified view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @param view The view to get padding for
     * @return the end padding in pixels
     */
    public static int getPaddingEnd(View view) {
        return IMPL.getPaddingEnd(view);
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
     */
    public static void setPaddingRelative(View view, int start, int top, int end, int bottom) {
        IMPL.setPaddingRelative(view, start, top, end, bottom);
    }

    /**
     * Notify a view that it is being temporarily detached.
     */
    public static void dispatchStartTemporaryDetach(View view) {
        IMPL.dispatchStartTemporaryDetach(view);
    }

    /**
     * Notify a view that its temporary detach has ended; the view is now reattached.
     */
    public static void dispatchFinishTemporaryDetach(View view) {
        IMPL.dispatchFinishTemporaryDetach(view);
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
    @Deprecated
    @Nullable
    public static Matrix getMatrix(View view) {
        return view.getMatrix();
    }

    /**
     * Returns the minimum width of the view.
     *
     * <p>Prior to API 16, this method may return 0 on some platforms.</p>
     *
     * @return the minimum width the view will try to be.
     */
    public static int getMinimumWidth(View view) {
        return IMPL.getMinimumWidth(view);
    }

    /**
     * Returns the minimum height of the view.
     *
     * <p>Prior to API 16, this method may return 0 on some platforms.</p>
     *
     * @return the minimum height the view will try to be.
     */
    public static int getMinimumHeight(View view) {
        return IMPL.getMinimumHeight(view);
    }

    /**
     * This method returns a ViewPropertyAnimator object, which can be used to animate
     * specific properties on this View.
     *
     * @return ViewPropertyAnimator The ViewPropertyAnimator associated with this View.
     */
    public static ViewPropertyAnimatorCompat animate(View view) {
        return IMPL.animate(view);
    }

    /**
     * Sets the horizontal location of this view relative to its left position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @param value The horizontal position of this view relative to its left position,
     * in pixels.
     *
     * @deprecated Use {@link View#setTranslationX(float)} directly.
     */
    @Deprecated
    public static void setTranslationX(View view, float value) {
        view.setTranslationX(value);
    }

    /**
     * Sets the vertical location of this view relative to its top position.
     * This effectively positions the object post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @param value The vertical position of this view relative to its top position,
     * in pixels.
     *
     * @attr name android:translationY
     *
     * @deprecated Use {@link View#setTranslationY(float)} directly.
     */
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
     * @param value The opacity of the view.
     *
     * @deprecated Use {@link View#setAlpha(float)} directly.
     */
    @Deprecated
    public static void setAlpha(View view, @FloatRange(from=0.0, to=1.0) float value) {
        view.setAlpha(value);
    }

    /**
     * Sets the visual x position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationX(View, float) translationX} property to be the difference between
     * the x value passed in and the current left property of the view as determined
     * by the layout bounds.
     *
     * @param value The visual x position of this view, in pixels.
     *
     * @deprecated Use {@link View#setX(float)} directly.
     */
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
     * @param value The visual y position of this view, in pixels.
     *
     * @deprecated Use {@link View#setY(float)} directly.
     */
    @Deprecated
    public static void setY(View view, float value) {
        view.setY(value);
    }

    /**
     * Sets the degrees that the view is rotated around the pivot point. Increasing values
     * result in clockwise rotation.
     *
     * @param value The degrees of rotation.
     *
     * @deprecated Use {@link View#setRotation(float)} directly.
     */
    @Deprecated
    public static void setRotation(View view, float value) {
        view.setRotation(value);
    }

    /**
     * Sets the degrees that the view is rotated around the horizontal axis through the pivot point.
     * Increasing values result in clockwise rotation from the viewpoint of looking down the
     * x axis.
     *
     * @param value The degrees of X rotation.
     *
     * @deprecated Use {@link View#setRotationX(float)} directly.
     */
    @Deprecated
    public static void setRotationX(View view, float value) {
        view.setRotationX(value);
    }

    /**
     * Sets the degrees that the view is rotated around the vertical axis through the pivot point.
     * Increasing values result in counter-clockwise rotation from the viewpoint of looking
     * down the y axis.
     *
     * @param value The degrees of Y rotation.
     *
     * @deprecated Use {@link View#setRotationY(float)} directly.
     */
    @Deprecated
    public static void setRotationY(View view, float value) {
        view.setRotationY(value);
    }

    /**
     * Sets the amount that the view is scaled in x around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param value The scaling factor.
     *
     * @deprecated Use {@link View#setScaleX(float)} directly.
     */
    @Deprecated
    public static void setScaleX(View view, float value) {
        view.setScaleX(value);
    }

    /**
     * Sets the amount that the view is scaled in Y around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1 means that no scaling is applied.
     *
     * @param value The scaling factor.
     *
     * @deprecated Use {@link View#setScaleY(float)} directly.
     */
    @Deprecated
    public static void setScaleY(View view, float value) {
        view.setScaleY(value);
    }

    /**
     * The x location of the point around which the view is
     * {@link #setRotation(View, float) rotated} and {@link #setScaleX(View, float) scaled}.
     *
     * @deprecated Use {@link View#getPivotX()} directly.
     */
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
     * @param value The x location of the pivot point.
     *
     * @deprecated Use {@link View#setPivotX(float)} directly.
     */
    @Deprecated
    public static void setPivotX(View view, float value) {
        view.setPivotX(value);
    }

    /**
     * The y location of the point around which the view is {@link #setRotation(View,
     * float) rotated} and {@link #setScaleY(View, float) scaled}.
     *
     * @return The y location of the pivot point.
     *
     * @deprecated Use {@link View#getPivotY()} directly.
     */
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
     * @param value The y location of the pivot point.
     *
     * @deprecated Use {@link View#setPivotX(float)} directly.
     */
    @Deprecated
    public static void setPivotY(View view, float value) {
        view.setPivotY(value);
    }

    /**
     * @deprecated Use {@link View#getRotation()} directly.
     */
    @Deprecated
    public static float getRotation(View view) {
        return view.getRotation();
    }

    /**
     * @deprecated Use {@link View#getRotationX()} directly.
     */
    @Deprecated
    public static float getRotationX(View view) {
        return view.getRotationX();
    }

    /**
     * @deprecated Use {@link View#getRotationY()} directly.
     */
    @Deprecated
    public static float getRotationY(View view) {
        return view.getRotationY();
    }

    /**
     * @deprecated Use {@link View#getScaleX()} directly.
     */
    @Deprecated
    public static float getScaleX(View view) {
        return view.getScaleX();
    }

    /**
     * @deprecated Use {@link View#getScaleY()} directly.
     */
    @Deprecated
    public static float getScaleY(View view) {
        return view.getScaleY();
    }

    /**
     * @deprecated Use {@link View#getX()} directly.
     */
    @Deprecated
    public static float getX(View view) {
        return view.getX();
    }

    /**
     * @deprecated Use {@link View#getY()} directly.
     */
    @Deprecated
    public static float getY(View view) {
        return view.getY();
    }

    /**
     * Sets the base elevation of this view, in pixels.
     */
    public static void setElevation(View view, float elevation) {
        IMPL.setElevation(view, elevation);
    }

    /**
     * The base elevation of this view relative to its parent, in pixels.
     *
     * @return The base depth position of the view, in pixels.
     */
    public static float getElevation(View view) {
        return IMPL.getElevation(view);
    }

    /**
     * Sets the depth location of this view relative to its {@link #getElevation(View) elevation}.
     */
    public static void setTranslationZ(View view, float translationZ) {
        IMPL.setTranslationZ(view, translationZ);
    }

    /**
     * The depth location of this view relative to its {@link #getElevation(View) elevation}.
     *
     * @return The depth of this view relative to its elevation.
     */
    public static float getTranslationZ(View view) {
        return IMPL.getTranslationZ(view);
    }

    /**
     * Sets the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * @param view The View against which to invoke the method.
     * @param transitionName The name of the View to uniquely identify it for Transitions.
     */
    public static void setTransitionName(View view, String transitionName) {
        IMPL.setTransitionName(view, transitionName);
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
    public static String getTransitionName(View view) {
        return IMPL.getTransitionName(view);
    }

    /**
     * Returns the current system UI visibility that is currently set for the entire window.
     */
    public static int getWindowSystemUiVisibility(View view) {
        return IMPL.getWindowSystemUiVisibility(view);
    }

    /**
     * Ask that a new dispatch of {@code View.onApplyWindowInsets(WindowInsets)} be performed. This
     * falls back to {@code View.requestFitSystemWindows()} where available.
     */
    public static void requestApplyInsets(View view) {
        IMPL.requestApplyInsets(view);
    }

    /**
     * Tells the ViewGroup whether to draw its children in the order defined by the method
     * {@code ViewGroup.getChildDrawingOrder(int, int)}.
     *
     * @param enabled true if the order of the children when drawing is determined by
     *        {@link ViewGroup#getChildDrawingOrder(int, int)}, false otherwise
     *
     * <p>Prior to API 7 this will have no effect.</p>
     *
     * @deprecated Use {@link ViewGroup#setChildrenDrawingOrderEnabled(boolean)} directly.
     */
    @Deprecated
    public static void setChildrenDrawingOrderEnabled(ViewGroup viewGroup, boolean enabled) {
       IMPL.setChildrenDrawingOrderEnabled(viewGroup, enabled);
    }

    /**
     * Returns true if this view should adapt to fit system window insets. This method will always
     * return false before API 16 (Jellybean).
     */
    public static boolean getFitsSystemWindows(View v) {
        return IMPL.getFitsSystemWindows(v);
    }

    /**
     * Sets whether or not this view should account for system screen decorations
     * such as the status bar and inset its content; that is, controlling whether
     * the default implementation of {@link View#fitSystemWindows(Rect)} will be
     * executed. See that method for more details.
     *
     * @deprecated Use {@link View#setFitsSystemWindows(boolean)} directly.
     */
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
     * @deprecated Use {@link View#jumpDrawablesToCurrentState()} directly.
     */
    @Deprecated
    public static void jumpDrawablesToCurrentState(View v) {
        v.jumpDrawablesToCurrentState();
    }

    /**
     * Set an {@link OnApplyWindowInsetsListener} to take over the policy for applying
     * window insets to this view. This will only take effect on devices with API 21 or above.
     */
    public static void setOnApplyWindowInsetsListener(View v,
            OnApplyWindowInsetsListener listener) {
        IMPL.setOnApplyWindowInsetsListener(v, listener);
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
    public static WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
        return IMPL.onApplyWindowInsets(view, insets);
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
     * @param insets Insets to apply
     * @return The provided insets minus the insets that were consumed
     */
    public static WindowInsetsCompat dispatchApplyWindowInsets(View view,
            WindowInsetsCompat insets) {
        return IMPL.dispatchApplyWindowInsets(view, insets);
    }

    /**
     * Controls whether the entire hierarchy under this view will save its
     * state when a state saving traversal occurs from its parent.
     *
     * @param enabled Set to false to <em>disable</em> state saving, or true
     * (the default) to allow it.
     *
     * @deprecated Use {@link View#setSaveFromParentEnabled(boolean)} directly.
     */
    @Deprecated
    public static void setSaveFromParentEnabled(View v, boolean enabled) {
        v.setSaveFromParentEnabled(enabled);
    }

    /**
     * Changes the activated state of this view. A view can be activated or not.
     * Note that activation is not the same as selection.  Selection is
     * a transient property, representing the view (hierarchy) the user is
     * currently interacting with.  Activation is a longer-term state that the
     * user can move views in and out of.
     *
     * @param activated true if the view must be activated, false otherwise
     *
     * @deprecated Use {@link View#setActivated(boolean)} directly.
     */
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
     * @return true if the content in this view might overlap, false otherwise.
     */
    public static boolean hasOverlappingRendering(View view) {
        return IMPL.hasOverlappingRendering(view);
    }

    /**
     * Return if the padding as been set through relative values
     * {@code View.setPaddingRelative(int, int, int, int)} or thru
     *
     * @return true if the padding is relative or false if it is not.
     */
    public static boolean isPaddingRelative(View view) {
        return IMPL.isPaddingRelative(view);
    }

    /**
     * Set the background of the {@code view} to a given Drawable, or remove the background. If the
     * background has padding, {@code view}'s padding is set to the background's padding. However,
     * when a background is removed, this View's padding isn't touched. If setting the padding is
     * desired, please use{@code setPadding(int, int, int, int)}.
     */
    public static void setBackground(View view, Drawable background) {
        IMPL.setBackground(view, background);
    }

    /**
     * Return the tint applied to the background drawable, if specified.
     * <p>
     * Only returns meaningful info when running on API v21 or newer, or if {@code view}
     * implements the {@code TintableBackgroundView} interface.
     */
    public static ColorStateList getBackgroundTintList(View view) {
        return IMPL.getBackgroundTintList(view);
    }

    /**
     * Applies a tint to the background drawable.
     * <p>
     * This will always take effect when running on API v21 or newer. When running on platforms
     * previous to API v21, it will only take effect if {@code view} implements the
     * {@code TintableBackgroundView} interface.
     */
    public static void setBackgroundTintList(View view, ColorStateList tintList) {
        IMPL.setBackgroundTintList(view, tintList);
    }

    /**
     * Return the blending mode used to apply the tint to the background
     * drawable, if specified.
     * <p>
     * Only returns meaningful info when running on API v21 or newer, or if {@code view}
     * implements the {@code TintableBackgroundView} interface.
     */
    public static PorterDuff.Mode getBackgroundTintMode(View view) {
        return IMPL.getBackgroundTintMode(view);
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setBackgroundTintList(android.view.View, android.content.res.ColorStateList)} to
     * the background drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
     * <p>
     * This will always take effect when running on API v21 or newer. When running on platforms
     * previous to API v21, it will only take effect if {@code view} implement the
     * {@code TintableBackgroundView} interface.
     */
    public static void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
        IMPL.setBackgroundTintMode(view, mode);
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
     * @param enabled true to enable nested scrolling, false to disable
     *
     * @see #isNestedScrollingEnabled(View)
     */
    public static void setNestedScrollingEnabled(@NonNull View view, boolean enabled) {
        IMPL.setNestedScrollingEnabled(view, enabled);
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
        return IMPL.isNestedScrollingEnabled(view);
    }

    /**
     * Begin a nestable scroll operation along the given axes.
     *
     * <p>This version of the method just calls {@link #startNestedScroll(View, int, int)} using
     * the touch input type.</p>
     *
     * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
     *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     *         the current gesture.
     */
    public static boolean startNestedScroll(@NonNull View view, @ScrollAxis int axes) {
        return IMPL.startNestedScroll(view, axes);
    }

    /**
     * Stop a nested scroll in progress.
     *
     * <p>This version of the method just calls {@link #stopNestedScroll(View, int)} using the
     * touch input type.</p>
     *
     * @see #startNestedScroll(View, int)
     */
    public static void stopNestedScroll(@NonNull View view) {
        IMPL.stopNestedScroll(view);
    }

    /**
     * Returns true if this view has a nested scrolling parent.
     *
     * <p>This version of the method just calls {@link #hasNestedScrollingParent(View, int)}
     * using the touch input type.</p>
     *
     * @return whether this view has a nested scrolling parent
     */
    public static boolean hasNestedScrollingParent(@NonNull View view) {
        return IMPL.hasNestedScrollingParent(view);
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>This version of the method just calls
     * {@link #dispatchNestedScroll(View, int, int, int, int, int[], int)} using the touch input
     * type.</p>
     *
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
            int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
        return IMPL.dispatchNestedScroll(view, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>This version of the method just calls
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[], int)} using the touch input
     * type.</p>
     *
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
    public static boolean dispatchNestedPreScroll(@NonNull View view, int dx, int dy,
            @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
        return IMPL.dispatchNestedPreScroll(view, dx, dy, consumed, offsetInWindow);
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
            return IMPL.startNestedScroll(view, axes);
        }
        return false;
    }

    /**
     * Stop a nested scroll in progress.
     *
     * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
     *
     * @param type the type of input which cause this scroll event
     * @see #startNestedScroll(View, int)
     */
    public static void stopNestedScroll(@NonNull View view, @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            ((NestedScrollingChild2) view).stopNestedScroll(type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            IMPL.stopNestedScroll(view);
        }
    }

    /**
     * Returns true if this view has a nested scrolling parent.
     *
     * <p>The presence of a nested scrolling parent indicates that this view has initiated
     * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
     *
     * @param type the type of input which cause this scroll event
     * @return whether this view has a nested scrolling parent
     */
    public static boolean hasNestedScrollingParent(@NonNull View view, @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            ((NestedScrollingChild2) view).hasNestedScrollingParent(type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return IMPL.hasNestedScrollingParent(view);
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
     * {@link #dispatchNestedPreScroll(View, int, int, int[], int[]) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.</p>
     *
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @param type the type of input which cause this scroll event
     * @return true if the event was dispatched, false if it could not be dispatched.
     * @see #dispatchNestedPreScroll(View, int, int, int[], int[])
     */
    public static boolean dispatchNestedScroll(@NonNull View view, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow,
            @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            return ((NestedScrollingChild2) view).dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return IMPL.dispatchNestedScroll(view, dxConsumed, dyConsumed, dxUnconsumed,
                    dyUnconsumed, offsetInWindow);
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
            @Nullable int[] consumed, @Nullable int[] offsetInWindow, @NestedScrollType int type) {
        if (view instanceof NestedScrollingChild2) {
            return ((NestedScrollingChild2) view).dispatchNestedPreScroll(dx, dy, consumed,
                    offsetInWindow, type);
        } else if (type == ViewCompat.TYPE_TOUCH) {
            return IMPL.dispatchNestedPreScroll(view, dx, dy, consumed, offsetInWindow);
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
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @param consumed true if the child consumed the fling, false otherwise
     * @return true if the nested scrolling parent consumed or otherwise reacted to the fling
     */
    public static boolean dispatchNestedFling(@NonNull View view, float velocityX, float velocityY,
            boolean consumed) {
        return IMPL.dispatchNestedFling(view, velocityX, velocityY, consumed);
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
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @return true if a nested scrolling parent consumed the fling
     */
    public static boolean dispatchNestedPreFling(@NonNull View view, float velocityX,
            float velocityY) {
        return IMPL.dispatchNestedPreFling(view, velocityX, velocityY);
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
     */
    public static boolean isInLayout(View view) {
        return IMPL.isInLayout(view);
    }

    /**
     * Returns true if {@code view} has been through at least one layout since it
     * was last attached to or detached from a window.
     */
    public static boolean isLaidOut(View view) {
        return IMPL.isLaidOut(view);
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
     */
    public static boolean isLayoutDirectionResolved(View view) {
        return IMPL.isLayoutDirectionResolved(view);
    }

    /**
     * The visual z position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationZ(View, float) translationZ} property plus the current
     * {@link #getElevation(View) elevation} property.
     *
     * @return The visual z position of this view, in pixels.
     */
    public static float getZ(View view) {
        return IMPL.getZ(view);
    }

    /**
     * Sets the visual z position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationZ(View, float) translationZ} property to be the difference between
     * the x value passed in and the current {@link #getElevation(View) elevation} property.
     * <p>
     * Compatibility:
     * <ul>
     *     <li>API &lt; 21: No-op
     * </ul>
     *
     * @param z The visual z position of this view, in pixels.
     */
    public static void setZ(View view, float z) {
        IMPL.setZ(view, z);
    }

    /**
     * Offset this view's vertical location by the specified number of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetTopAndBottom(View view, int offset) {
        IMPL.offsetTopAndBottom(view, offset);
    }

    /**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public static void offsetLeftAndRight(View view, int offset) {
        IMPL.offsetLeftAndRight(view, offset);
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
     */
    public static void setClipBounds(View view, Rect clipBounds) {
        IMPL.setClipBounds(view, clipBounds);
    }

    /**
     * Returns a copy of the current {@link #setClipBounds(View, Rect)}.
     *
     * <p>Prior to API 18 this will return null.</p>
     *
     * @return A copy of the current clip bounds if clip bounds are set,
     * otherwise null.
     */
    public static Rect getClipBounds(View view) {
        return IMPL.getClipBounds(view);
    }

    /**
     * Returns true if the provided view is currently attached to a window.
     */
    public static boolean isAttachedToWindow(View view) {
        return IMPL.isAttachedToWindow(view);
    }

    /**
     * Returns whether the provided view has an attached {@link View.OnClickListener}.
     *
     * @return true if there is a listener, false if there is none.
     */
    public static boolean hasOnClickListeners(View view) {
        return IMPL.hasOnClickListeners(view);
    }

    /**
     * Sets the state of all scroll indicators.
     * <p>
     * See {@link #setScrollIndicators(View, int, int)} for usage information.
     *
     * @param indicators a bitmask of indicators that should be enabled, or
     *                   {@code 0} to disable all indicators
     *
     * @see #setScrollIndicators(View, int, int)
     * @see #getScrollIndicators(View)
     */
    public static void setScrollIndicators(@NonNull View view, @ScrollIndicators int indicators) {
        IMPL.setScrollIndicators(view, indicators);
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
     *
     * @see #setScrollIndicators(View, int)
     * @see #getScrollIndicators(View)
     */
    public static void setScrollIndicators(@NonNull View view, @ScrollIndicators int indicators,
            @ScrollIndicators int mask) {
        IMPL.setScrollIndicators(view, indicators, mask);
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
     * @return a bitmask representing the enabled scroll indicators
     */
    public static int getScrollIndicators(@NonNull View view) {
        return IMPL.getScrollIndicators(view);
    }

    /**
     * Set the pointer icon for the current view.
     * @param pointerIcon A PointerIconCompat instance which will be shown when the mouse hovers.
     */
    public static void setPointerIcon(@NonNull View view, PointerIconCompat pointerIcon) {
        IMPL.setPointerIcon(view, pointerIcon);
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
     */
    public static Display getDisplay(@NonNull View view) {
        return IMPL.getDisplay(view);
    }

    /**
     * Sets the tooltip for the view.
     *
     * <p>Prior to API 26 this does nothing. Use TooltipCompat class from v7 appcompat library
     * for a compatible tooltip implementation.</p>
     *
     * @param tooltipText the tooltip text
     */
    public static void setTooltipText(@NonNull View view, @Nullable CharSequence tooltipText) {
        IMPL.setTooltipText(view, tooltipText);
    }

    /**
     * Start the drag and drop operation.
     */
    public static boolean startDragAndDrop(View v, ClipData data,
            View.DragShadowBuilder shadowBuilder, Object localState, int flags) {
        return IMPL.startDragAndDrop(v, data, shadowBuilder, localState, flags);
    }

    /**
     * Cancel the drag and drop operation.
     */
    public static void cancelDragAndDrop(View v) {
        IMPL.cancelDragAndDrop(v);
    }

    /**
     * Update the drag shadow while drag and drop is in progress.
     */
    public static void updateDragShadow(View v, View.DragShadowBuilder shadowBuilder) {
        IMPL.updateDragShadow(v, shadowBuilder);
    }

    /**
     * Gets the ID of the next keyboard navigation cluster root.
     *
     * @return the next keyboard navigation cluster ID, or {@link View#NO_ID} if the framework
     *         should decide automatically or API < 26.
     */
    public static int getNextClusterForwardId(@NonNull View view) {
        return IMPL.getNextClusterForwardId(view);
    }

    /**
     * Sets the ID of the next keyboard navigation cluster root view. Does nothing if {@code view}
     * is not a keyboard navigation cluster or if API < 26.
     *
     * @param nextClusterForwardId next cluster ID, or {@link View#NO_ID} if the framework
     *                             should decide automatically.
     */
    public static void setNextClusterForwardId(@NonNull View view, int nextClusterForwardId) {
        IMPL.setNextClusterForwardId(view, nextClusterForwardId);
    }

    /**
     * Returns whether {@code view} is a root of a keyboard navigation cluster. Always returns
     * {@code false} on API < 26.
     *
     * @return {@code true} if this view is a root of a cluster, or {@code false} otherwise.
     */
    public static boolean isKeyboardNavigationCluster(@NonNull View view) {
        return IMPL.isKeyboardNavigationCluster(view);
    }

    /**
     * Set whether {@code view} is a root of a keyboard navigation cluster. Does nothing if
     * API < 26.
     *
     * @param isCluster {@code true} to mark {@code view} as the root of a cluster, {@code false}
     *                  to unmark.
     */
    public static void setKeyboardNavigationCluster(@NonNull View view, boolean isCluster) {
        IMPL.setKeyboardNavigationCluster(view, isCluster);
    }

    /**
     * Returns whether {@code view} should receive focus when the focus is restored for the view
     * hierarchy containing it. Returns {@code false} on API < 26.
     * <p>
     * Focus gets restored for a view hierarchy when the root of the hierarchy gets added to a
     * window or serves as a target of cluster navigation.
     *
     * @return {@code true} if {@code view} is the default-focus view, {@code false} otherwise.
     */
    public static boolean isFocusedByDefault(@NonNull View view) {
        return IMPL.isFocusedByDefault(view);
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
     * @param isFocusedByDefault {@code true} to set {@code view} as the default-focus view,
     *                           {@code false} otherwise.
     */
    public static void setFocusedByDefault(@NonNull View view, boolean isFocusedByDefault) {
        IMPL.setFocusedByDefault(view, isFocusedByDefault);
    }

    /**
     * Find the nearest keyboard navigation cluster in the specified direction.
     * This does not actually give focus to that cluster.
     *
     * @param currentCluster The starting point of the search. {@code null} means the current
     *                       cluster is not found yet.
     * @param direction Direction to look.
     *
     * @return the nearest keyboard navigation cluster in the specified direction, or {@code null}
     *         if one can't be found or if API < 26.
     */
    public static View keyboardNavigationClusterSearch(@NonNull View view, View currentCluster,
            @FocusDirection int direction) {
        return IMPL.keyboardNavigationClusterSearch(view, currentCluster, direction);
    }

    /**
     * Adds any keyboard navigation cluster roots that are descendants of {@code view} (
     * including {@code view} if it is a cluster root itself) to {@code views}. Does nothing
     * on API < 26.
     *
     * @param views collection of keyboard navigation cluster roots found so far.
     * @param direction direction to look.
     */
    public static void addKeyboardNavigationClusters(@NonNull View view,
            @NonNull Collection<View> views, int direction) {
        IMPL.addKeyboardNavigationClusters(view, views, direction);
    }

    /**
     * Gives focus to the default-focus view in the view hierarchy rooted at {@code view}.
     * If the default-focus view cannot be found or if API < 26, this falls back to calling
     * {@link View#requestFocus(int)}.
     *
     * @return {@code true} if {@code view} or one of its descendants took focus, {@code false}
     *         otherwise.
     */
    public static boolean restoreDefaultFocus(@NonNull View view) {
        return IMPL.restoreDefaultFocus(view);
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
     * @return {@code true} if the view is focusable or if the view contains a focusable
     *         view, {@code false} otherwise
     */
    public static boolean hasExplicitFocusable(@NonNull View view) {
        return IMPL.hasExplicitFocusable(view);
    }

    protected ViewCompat() {}
}
