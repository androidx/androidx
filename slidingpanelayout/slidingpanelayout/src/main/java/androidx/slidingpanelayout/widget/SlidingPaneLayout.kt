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

package androidx.slidingpanelayout.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.getChildMeasureSpec
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.HandlerCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.Openable
import androidx.customview.widget.ViewDragHelper
import androidx.slidingpanelayout.R
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

private const val TAG = "SlidingPaneLayout"

/** Minimum velocity that will be detected as a fling */
private const val MIN_FLING_VELOCITY = 400 // dips per second

private const val MIN_TOUCH_TARGET_SIZE = 48 // dp

/** Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage. */
private const val ACCESSIBILITY_CLASS_NAME = "androidx.slidingpanelayout.widget.SlidingPaneLayout"

private val edgeSizeUsingSystemGestureInsets = Build.VERSION.SDK_INT >= 29

private fun getChildHeightMeasureSpec(
    child: View,
    skippedFirstPass: Boolean,
    spec: Int,
    padding: Int
): Int {
    val lp = child.layoutParams
    return if (skippedFirstPass) {
        // This was skipped the first time; figure out a real height spec.
        getChildMeasureSpec(spec, padding, lp.height)
    } else {
        MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY)
    }
}

private inline val SlidingPaneLayout.LayoutParams.canInfluenceParentSize: Boolean
    get() = (width != MATCH_PARENT && width != 0) || (height != MATCH_PARENT && height != 0)

private inline val SlidingPaneLayout.LayoutParams.weightOnlyWidth: Boolean
    get() = width == 0 && weight > 0

private inline val SlidingPaneLayout.LayoutParams.canExpandWidth: Boolean
    get() = width == MATCH_PARENT || weight > 0

/**
 * Utility for calculating layout positioning of child views relative to a [FoldingFeature]. This
 * class is not thread-safe.
 */
private class FoldBoundsCalculator {

    private val tmpIntArray = IntArray(2)
    private val splitViewPositionsTmpRect = Rect()
    private val getFoldBoundsInViewTmpRect = Rect()

    /**
     * Returns `true` if there is a split and [outLeftRect] and [outRightRect] contain the split
     * positions; false if there is not a compatible split available, [outLeftRect] and
     * [outRightRect] will remain unmodified.
     */
    fun splitViewPositions(
        foldingFeature: FoldingFeature?,
        parentView: View,
        outLeftRect: Rect,
        outRightRect: Rect,
    ): Boolean {
        if (foldingFeature == null) return false
        if (!foldingFeature.isSeparating) return false

        // Don't support horizontal fold in list-detail view layout
        if (foldingFeature.bounds.left == 0) return false

        // vertical split
        val splitPosition = splitViewPositionsTmpRect
        if (
            foldingFeature.bounds.top == 0 &&
                getFoldBoundsInView(foldingFeature, parentView, splitPosition)
        ) {
            outLeftRect.set(
                parentView.paddingLeft,
                parentView.paddingTop,
                max(parentView.paddingLeft, splitPosition.left),
                parentView.height - parentView.paddingBottom
            )
            val rightBound = parentView.width - parentView.paddingRight
            outRightRect.set(
                min(rightBound, splitPosition.right),
                parentView.paddingTop,
                rightBound,
                parentView.height - parentView.paddingBottom
            )
            return true
        }
        return false
    }

    /**
     * Returns `true` if [foldingFeature] overlaps with [view] and writes the bounds to [outRect].
     */
    private fun getFoldBoundsInView(
        foldingFeature: FoldingFeature,
        view: View,
        outRect: Rect
    ): Boolean {
        val viewLocationInWindow = tmpIntArray
        view.getLocationInWindow(viewLocationInWindow)
        val x = viewLocationInWindow[0]
        val y = viewLocationInWindow[1]
        val viewRect =
            getFoldBoundsInViewTmpRect.apply { set(x, y, x + view.width, y + view.width) }
        val foldRectInView = outRect.apply { set(foldingFeature.bounds) }
        // Translate coordinate space of split from window coordinate space to current view
        // position in window
        val intersects = foldRectInView.intersect(viewRect)
        // Check if the split is overlapped with the view
        if (foldRectInView.width() == 0 && foldRectInView.height() == 0 || !intersects) {
            return false
        }
        foldRectInView.offset(-x, -y)
        return true
    }
}

/**
 * Pulls the string interpolation and exception throwing bytecode out of the inlined
 * [spLayoutParams] property at each call site
 */
private fun layoutParamsError(childView: View, layoutParams: LayoutParams?): Nothing {
    error("SlidingPaneLayout child $childView had unexpected LayoutParams $layoutParams")
}

private inline val View.spLayoutParams: SlidingPaneLayout.LayoutParams
    get() = layoutParams as SlidingPaneLayout.LayoutParams

/**
 * SlidingPaneLayout provides a horizontal, multi-pane layout for use at the top level of a UI. A
 * left (or start) pane is treated as a content list or browser, subordinate to a primary detail
 * view for displaying content.
 *
 * Child views overlap if their combined width exceeds the available width in the SlidingPaneLayout.
 * Each of child views is expand out to fill the available width in the SlidingPaneLayout. When this
 * occurs, the user may slide the topmost view out of the way by dragging it, and dragging back it
 * from the very edge.
 *
 * Thanks to this sliding behavior, SlidingPaneLayout may be suitable for creating layouts that can
 * smoothly adapt across many different screen sizes, expanding out fully on larger screens and
 * collapsing on smaller screens.
 *
 * SlidingPaneLayout is distinct from a navigation drawer as described in the design guide and
 * should not be used in the same scenarios. SlidingPaneLayout should be thought of only as a way to
 * allow a two-pane layout normally used on larger screens to adapt to smaller screens in a natural
 * way. The interaction patterns expressed by SlidingPaneLayout imply a physicality and direct
 * information hierarchy between panes that does not necessarily exist in a scenario where a
 * navigation drawer should be used instead.
 *
 * Appropriate uses of SlidingPaneLayout include pairings of panes such as a contact list and
 * subordinate interactions with those contacts, or an email thread list with the content pane
 * displaying the contents of the selected thread. Inappropriate uses of SlidingPaneLayout include
 * switching between disparate functions of your app, such as jumping from a social stream view to a
 * view of your personal profile - cases such as this should use the navigation drawer pattern
 * instead. ([DrawerLayout][androidx.drawerlayout.widget.DrawerLayout] implements this pattern.)
 *
 * Like [LinearLayout][android.widget.LinearLayout], SlidingPaneLayout supports the use of the
 * layout parameter `layout_weight` on child views to determine how to divide leftover space after
 * measurement is complete. It is only relevant for width. When views do not overlap weight behaves
 * as it does in a LinearLayout.
 */
@Suppress("LeakingThis")
open class SlidingPaneLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ViewGroup(context, attrs, defStyle), Openable {

    /**
     * The ARGB-packed color value used to fade the sliding pane. This property is no longer used.
     */
    @get:Deprecated("This field is no longer populated by SlidingPaneLayout.")
    @get:ColorInt
    @set:Deprecated("SlidingPaneLayout no longer uses this field.")
    open var sliderFadeColor: Int
        get() = 0
        set(@Suppress("UNUSED_PARAMETER") value) {}

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane will become
     * fully covered in the closed state. This value is no longer used.
     */
    @get:Deprecated("This field is no longer populated by SlidingPaneLayout")
    @get:ColorInt
    @set:Deprecated("SlidingPaneLayout no longer uses this field.")
    open var coveredFadeColor: Int
        get() = 0
        set(@Suppress("UNUSED_PARAMETER") value) {}

    /** Drawable used to draw the shadow between panes by default. */
    private var shadowDrawableLeft: Drawable? = null

    /** Drawable used to draw the shadow between panes to support RTL (right to left language). */
    private var shadowDrawableRight: Drawable? = null

    /**
     * Check if both the list and detail view panes in this layout can fully fit side-by-side. If
     * not, the content pane has the capability to slide back and forth. Note that the lock mode is
     * not taken into account in this method. This method is typically used to determine whether the
     * layout is showing two-pane or single-pane.
     */
    open val isSlideable: Boolean
        get() = _isSlideable

    // When converting from java, isSlideable() was open and had no setter;
    // kotlin doesn't allow `open var` with a `private set`.
    private var _isSlideable = false

    /** The child view that can slide, if any. */
    private var slideableView: View? = null

    /**
     * How far the panel is offset from its usual position. range [0, 1] where 0 = open, 1 = closed.
     */
    private var currentSlideOffset = 1f

    /**
     * How far the non-sliding panel is parallaxed from its usual position when open. range [0, 1]
     */
    private var currentParallaxOffset = 0f

    /** How far in pixels the slideable panel may move. */
    private var slideRange = 0

    private val touchTargetMin =
        (context.resources.displayMetrics.density * MIN_TOUCH_TARGET_SIZE).roundToInt()

    private val overlappingPaneHandler = OverlappingPaneHandler()
    private val draggableDividerHandler = DraggableDividerHandler()

    private val cancelEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
    private var activeTouchHandler: TouchHandler? = null
        set(value) {
            if (field != value) {
                // Send a cancel event to the outgoing handler to reset it for later
                field?.onTouchEvent(cancelEvent)
                field = value
            }
        }

    /**
     * Stores whether or not the pane was open the last time it was slideable. If open/close
     * operations are invoked this state is modified. Used by instance state save/restore.
     */
    private var preservedOpenState = false
    private var awaitingFirstLayout = true
    private val tmpRect = Rect()
    private val tmpRect2 = Rect()
    private val foldBoundsCalculator = FoldBoundsCalculator()

    /** The lock mode that controls how the user can swipe between the panes. */
    @get:LockMode @LockMode var lockMode = 0

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_OPEN, LOCK_MODE_LOCKED_CLOSED, LOCK_MODE_LOCKED)
    internal annotation class LockMode

    private var foldingFeature: FoldingFeature? = null
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    /**
     * [Job] that tracks the last launched coroutine running [whileAttachedToVisibleWindow]. This is
     * never set to `null`; the last job is always [joined][Job.join] prior to invoking
     * [whileAttachedToVisibleWindow].
     */
    private var whileAttachedToVisibleWindowJob: Job? = null

    /**
     * Distance to parallax the lower pane by when the upper pane is in its fully closed state, in
     * pixels. The lower pane will scroll between this position and its fully open state.
     */
    @get:Px
    open var parallaxDistance: Int = 0
        /** The distance the lower pane will parallax by when the upper pane is fully closed. */
        set(@Px parallaxBy) {
            field = parallaxBy
            requestLayout()
        }

    /**
     * When set, if sufficient space is not available to present child panes side by side while
     * respecting the child pane's [LayoutParams.width] or [minimum width][View.getMinimumWidth],
     * the [SlidingPaneLayout] may allow the child panes to overlap. When child panes overlap
     * [lockMode] determines whether the user can drag the top pane to one side to make the lower
     * pane available for viewing or interaction.
     *
     * Defaults to `true`.
     */
    var isOverlappingEnabled: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    private val systemGestureInsets: Insets?
        // Get system gesture insets when SDK version is larger than 29. Otherwise, return null.
        get() {
            var gestureInsets: Insets? = null
            if (edgeSizeUsingSystemGestureInsets) {
                val rootInsetsCompat = ViewCompat.getRootWindowInsets(this)
                if (rootInsetsCompat != null) {
                    @Suppress("DEPRECATION")
                    gestureInsets = rootInsetsCompat.systemGestureInsets
                }
            }
            return gestureInsets
        }

    private val isLayoutRtl: Boolean
        get() = layoutDirection == LAYOUT_DIRECTION_RTL

    private val windowInfoTracker = WindowInfoTracker.getOrCreate(context)

    private var userResizingDividerDrawable: Drawable? = null

    // Reused/preallocated gesture exclusion data
    private val computedDividerExclusionRect = Rect()
    private val dividerGestureExclusionRect = Rect()
    private val gestureExclusionRectsList = listOf(dividerGestureExclusionRect)

    /**
     * Set a [Drawable] to display when [isUserResizingEnabled] is `true` and multiple panes are
     * visible without overlapping. This forms the visual touch target for dragging. This may also
     * be set from the `userResizingDividerDrawable` XML attribute during view inflation.
     */
    fun setUserResizingDividerDrawable(drawable: Drawable?) {
        val old = userResizingDividerDrawable
        if (drawable !== old) {
            if (old != null) {
                old.callback = null
                unscheduleDrawable(old)
            }
            userResizingDividerDrawable = drawable
            if (drawable != null) {
                drawable.callback = this
                if (drawable.isStateful) {
                    drawable.setState(createUserResizingDividerDrawableState(drawableState))
                }
                drawable.setVisible(visibility == VISIBLE, false)
            }
            // don't just invalidate; layout performs some extra state computation for the divider
            requestLayout()
        }
    }

    /**
     * Set a [Drawable] by resource id to display when [isUserResizingEnabled] is `true` and
     * multiple panes are visible without overlapping. This forms the visual touch target for
     * dragging. This may also be set from the `userResizingDividerDrawable` XML attribute during
     * view inflation.
     */
    fun setUserResizingDividerDrawable(@DrawableRes resId: Int) {
        setUserResizingDividerDrawable(ContextCompat.getDrawable(context, resId))
    }

    /** `true` if the user is currently dragging the [user resizing divider][isUserResizable] */
    val isDividerDragging: Boolean
        get() = draggableDividerHandler.isDragging

    /**
     * Position of the division between split panes when [isSlideable] is `false`. When the value is
     * < 0 it should be one of the `SPLIT_DIVIDER_POSITION_*` constants, e.g.
     * [SPLIT_DIVIDER_POSITION_AUTO]. When the value is >= 0 it represents a value in pixels between
     * 0 and [getWidth]. The default value is [SPLIT_DIVIDER_POSITION_AUTO].
     *
     * Changing this property will result in a [requestLayout] and relayout of the contents of the
     * [SlidingPaneLayout].
     *
     * This can be controlled by the user when [isUserResizable] and the setting will be preserved
     * as part of `savedInstanceState`. The value may be adapted across relayouts or configuration
     * changes to account for differences in pane sizing constraints.
     */
    var splitDividerPosition: Int = SPLIT_DIVIDER_POSITION_AUTO
        set(value) {
            if (field != value) {
                field = value
                if (!isSlideable) {
                    requestLayout()
                }
            }
        }

    /**
     * The center position in the X dimension of the visual divider indicator between panes. If
     * [isUserResizable] would return `false` this property will return < 0. If the user is actively
     * dragging the divider this will reflect its current drag position. If not, it will reflect
     * [splitDividerPosition] if [splitDividerPosition] would return >= 0, or the automatically
     * determined divider position if [splitDividerPosition] would return
     * [SPLIT_DIVIDER_POSITION_AUTO].
     */
    val visualDividerPosition: Int
        get() =
            when {
                !isUserResizable -> -1
                isDividerDragging -> draggableDividerHandler.dragPositionX
                splitDividerPosition >= 0 -> splitDividerPosition
                else -> {
                    val leftChild: View
                    val rightChild: View
                    if (isLayoutRtl) {
                        leftChild = getChildAt(1)
                        rightChild = getChildAt(0)
                    } else {
                        leftChild = getChildAt(0)
                        rightChild = getChildAt(1)
                    }
                    (leftChild.right + leftChild.spLayoutParams.rightMargin + rightChild.left -
                        rightChild.spLayoutParams.leftMargin) / 2
                }
            }

    private fun createUserResizingDividerDrawableState(viewState: IntArray): IntArray {
        if (android.R.attr.state_pressed !in viewState && !isDividerDragging) {
            return viewState
        }

        return if (isDividerDragging) {
            // Add the pressed state for the divider drawable
            viewState.copyOf(viewState.size + 1).also { stateArray ->
                stateArray[stateArray.lastIndex] = android.R.attr.state_pressed
            }
        } else {
            var foundPressed = false
            IntArray(viewState.size - 1) { index ->
                if (viewState[index] == android.R.attr.state_pressed) foundPressed = true
                viewState[if (foundPressed) index + 1 else index]
            }
        }
    }

    /**
     * Set to `true` to enable user resizing of side by side panes through gestures or other inputs.
     * This may also be set from the `isUserResizingEnabled` XML attribute during view inflation. A
     * divider drawable must be provided; see [setUserResizingDividerDrawable] and
     * [isUserResizable].
     */
    var isUserResizingEnabled: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                requestLayout()
            }
        }

    /**
     * `true` if user resizing of side-by-side panes is currently available. This means that:
     * - [isSlideable] is `false` (otherwise panes are overlapping, not side-by-side)
     * - [isUserResizingEnabled] is `true`
     * - A divider drawable has been [set][setUserResizingDividerDrawable]
     *
     * and not necessarily that the user themselves can change in size.
     */
    val isUserResizable: Boolean
        get() = !isSlideable && isUserResizingEnabled && userResizingDividerDrawable != null

    /** `true` if child views are clipped to [visualDividerPosition]. */
    var isChildClippingToResizeDividerEnabled: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    private var onUserResizingDividerClickListener: OnClickListener? = null

    /**
     * Set a [View.OnClickListener] that will be invoked if the user clicks/taps on the resizing
     * divider. The divider is only available to be clicked if [isUserResizable].
     */
    fun setOnUserResizingDividerClickListener(listener: OnClickListener?) {
        onUserResizingDividerClickListener = listener
    }

    private var userResizeBehavior = USER_RESIZE_RELAYOUT_WHEN_COMPLETE

    /**
     * Configure the [UserResizeBehavior] that will be used to adjust the [splitDividerPosition]
     * when [isUserResizable] and the user drags the divider from side to side.
     *
     * The default is [USER_RESIZE_RELAYOUT_WHEN_COMPLETE], which will adjust the position to a
     * freeform position respecting the minimum width of each pane when the user lets go of the
     * divider. [USER_RESIZE_RELAYOUT_WHEN_MOVED] will resize both panes live as the user drags,
     * though for complex layouts this can carry negative performance implications.
     *
     * This property can be set from layout xml as the `userResizeBehavior` attribute, using
     * `relayoutWhenComplete` or `relayoutWhenMoved` to set [USER_RESIZE_RELAYOUT_WHEN_COMPLETE] or
     * [USER_RESIZE_RELAYOUT_WHEN_MOVED], respectively.
     */
    fun setUserResizeBehavior(userResizeBehavior: UserResizeBehavior) {
        this.userResizeBehavior = userResizeBehavior
    }

    init {
        setWillNotDraw(false)
        ViewCompat.setAccessibilityDelegate(this, AccessibilityDelegate())
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)

        context.withStyledAttributes(
            attrs,
            R.styleable.SlidingPaneLayout,
            defStyleRes = R.style.Widget_SlidingPaneLayout
        ) {
            isOverlappingEnabled =
                getBoolean(R.styleable.SlidingPaneLayout_isOverlappingEnabled, true)
            isUserResizingEnabled =
                getBoolean(R.styleable.SlidingPaneLayout_isUserResizingEnabled, false)
            userResizingDividerDrawable =
                getDrawable(R.styleable.SlidingPaneLayout_userResizingDividerDrawable)
            isChildClippingToResizeDividerEnabled =
                getBoolean(
                    R.styleable.SlidingPaneLayout_isChildClippingToResizeDividerEnabled,
                    true
                )
            // Constants used in this `when` are defined in attrs.xml
            userResizeBehavior =
                when (
                    val behaviorConstant =
                        getInt(R.styleable.SlidingPaneLayout_userResizeBehavior, 0)
                ) {
                    // relayoutWhenComplete
                    0 -> USER_RESIZE_RELAYOUT_WHEN_COMPLETE
                    // relayoutWhenMoved
                    1 -> USER_RESIZE_RELAYOUT_WHEN_MOVED
                    else -> error("$behaviorConstant is not a valid userResizeBehavior value")
                }
        }
    }

    private fun computeDividerTargetRect(outRect: Rect, dividerPositionX: Int): Rect {
        val divider = userResizingDividerDrawable
        if (divider == null) {
            outRect.setEmpty()
            return outRect
        }

        val touchTargetMin = touchTargetMin
        val dividerWidth = divider.intrinsicWidth
        val dividerHeight = divider.intrinsicHeight
        val width = max(dividerWidth, touchTargetMin)
        val height = max(dividerHeight, touchTargetMin)
        val left = dividerPositionX - width / 2
        val right = left + width
        val top = (this.height - paddingTop - paddingBottom) / 2 + paddingTop - height / 2
        val bottom = top + height
        outRect.set(left, top, right, bottom)
        return outRect
    }

    /**
     * Set a listener to be notified of panel slide events. Note that this method is deprecated and
     * you should use [addPanelSlideListener] to add a listener and [removePanelSlideListener] to
     * remove a registered listener.
     *
     * @param listener Listener to notify when drawer events occur
     * @see PanelSlideListener
     * @see addPanelSlideListener
     * @see removePanelSlideListener
     */
    @Deprecated("Use {@link #addPanelSlideListener(PanelSlideListener)}")
    open fun setPanelSlideListener(listener: PanelSlideListener?) {
        overlappingPaneHandler.setPanelSlideListener(listener)
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of sliding state
     * events.
     *
     * @param listener Listener to notify when sliding state events occur.
     * @see removeSlideableStateListener
     */
    open fun addSlideableStateListener(listener: SlideableStateListener) {
        overlappingPaneHandler.addSlideableStateListener(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of sliding
     * state events.
     *
     * @param listener Listener to notify when sliding state events occur
     */
    open fun removeSlideableStateListener(listener: SlideableStateListener) {
        overlappingPaneHandler.removeSlideableStateListener(listener)
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of panel slide
     * events.
     *
     * @param listener Listener to notify when panel slide events occur.
     * @see removePanelSlideListener
     */
    open fun addPanelSlideListener(listener: PanelSlideListener) {
        overlappingPaneHandler.addPanelSlideListener(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of panel
     * slide events.
     *
     * @param listener Listener to remove from being notified of panel slide events
     * @see addPanelSlideListener
     */
    open fun removePanelSlideListener(listener: PanelSlideListener) {
        overlappingPaneHandler.removePanelSlideListener(listener)
    }

    private fun dispatchOnPanelSlide(panel: View) {
        overlappingPaneHandler.dispatchOnPanelSlide(panel, currentSlideOffset)
    }

    private fun updateObscuredViewsVisibility(panel: View?) {
        val isLayoutRtl = isLayoutRtl
        val startBound = if (isLayoutRtl) width - paddingRight else paddingLeft
        val endBound = if (isLayoutRtl) paddingLeft else width - paddingRight
        val topBound = paddingTop
        val bottomBound = height - paddingBottom
        val left: Int
        val right: Int
        val top: Int
        val bottom: Int
        if (panel != null && panel.isOpaque) {
            left = panel.left
            right = panel.right
            top = panel.top
            bottom = panel.bottom
        } else {
            left = 0
            top = 0
            right = 0
            bottom = 0
        }
        forEach { child ->
            if (child === panel) {
                // There are still more children above the panel but they won't be affected.
                return
            }
            if (child.visibility != GONE) {
                val clampedChildLeft =
                    (if (isLayoutRtl) endBound else startBound).coerceAtLeast(child.left)
                val clampedChildTop = topBound.coerceAtLeast(child.top)
                val clampedChildRight =
                    (if (isLayoutRtl) startBound else endBound).coerceAtMost(child.right)
                val clampedChildBottom = bottomBound.coerceAtMost(child.bottom)
                child.visibility =
                    if (
                        clampedChildLeft >= left &&
                            clampedChildTop >= top &&
                            clampedChildRight <= right &&
                            clampedChildBottom <= bottom
                    )
                        INVISIBLE
                    else VISIBLE
            }
        }
    }

    private fun setAllChildrenVisible() {
        forEach { child ->
            if (child.visibility == INVISIBLE) {
                child.visibility = VISIBLE
            }
        }
    }

    private fun updateDividerDrawableBounds(dividerPositionX: Int) {
        // only set the divider up if we have a width/height for the layout
        if (width > 0 && height > 0)
            userResizingDividerDrawable?.apply {
                val layoutCenterY = (height - paddingTop - paddingBottom) / 2 + paddingTop
                val dividerLeft = dividerPositionX - intrinsicWidth / 2
                val dividerTop = layoutCenterY - intrinsicHeight / 2
                setBounds(
                    dividerLeft,
                    dividerTop,
                    dividerLeft + intrinsicWidth,
                    dividerTop + intrinsicHeight
                )
            }
    }

    private fun updateGestureExclusion(dividerPositionX: Int) {
        if (dividerPositionX < 0) {
            computedDividerExclusionRect.setEmpty()
        } else {
            computeDividerTargetRect(computedDividerExclusionRect, dividerPositionX)
        }

        // Setting gesture exclusion rects makes the framework do some work; avoid it if we can.
        if (computedDividerExclusionRect != dividerGestureExclusionRect) {
            if (computedDividerExclusionRect.isEmpty) {
                ViewCompat.setSystemGestureExclusionRects(this, emptyList())
            } else {
                dividerGestureExclusionRect.set(computedDividerExclusionRect)
                // dividerGestureExclusionRect is already in gestureExclusionRectsList
                ViewCompat.setSystemGestureExclusionRects(this, gestureExclusionRectsList)
            }
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()

        userResizingDividerDrawable?.apply {
            if (isStateful && setState(createUserResizingDividerDrawableState(drawableState))) {
                invalidateDrawable(this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)

        userResizingDividerDrawable?.let { DrawableCompat.setHotspot(it, x, y) }
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        super.verifyDrawable(who) || who === userResizingDividerDrawable

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        userResizingDividerDrawable?.jumpToCurrentState()
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount == 1) {
            // Wrap detail view inside a touch blocker container
            val detailView: View = TouchBlocker(child)
            super.addView(detailView, index, params)
            return
        }
        super.addView(child, index, params)
    }

    override fun removeView(view: View) {
        if (view.parent is TouchBlocker) {
            super.removeView(view.parent as View)
            return
        }
        super.removeView(view)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        awaitingFirstLayout = true
        whileAttachedToVisibleWindowJob?.cancel()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        val toJoin = whileAttachedToVisibleWindowJob?.apply { cancel() }
        whileAttachedToVisibleWindowJob =
            if (visibility != VISIBLE) null
            else {
                CoroutineScope(HandlerCompat.createAsync(handler.looper).asCoroutineDispatcher())
                    .launch(start = CoroutineStart.UNDISPATCHED) {
                        // Don't let two copies of this run concurrently
                        toJoin?.join()
                        whileAttachedToVisibleWindow()
                    }
            }
    }

    private suspend fun whileAttachedToVisibleWindow() {
        windowInfoTracker
            .windowLayoutInfo(context)
            .mapNotNull { info ->
                info.displayFeatures.firstOrNull { it is FoldingFeature } as? FoldingFeature
            }
            .distinctUntilChanged()
            .collect { nextFeature -> foldingFeature = nextFeature }
    }

    override fun onDetachedFromWindow() {
        whileAttachedToVisibleWindowJob?.cancel()
        awaitingFirstLayout = true
        super.onDetachedFromWindow()
    }

    private fun getMinimumChildWidth(child: View): Int {
        return if (child is TouchBlocker) {
            child.getChildAt(0).minimumWidth
        } else child.minimumWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var layoutHeight = 0
        var maxLayoutHeight = 0
        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                maxLayoutHeight = heightSize - paddingTop - paddingBottom
                layoutHeight = maxLayoutHeight
            }
            MeasureSpec.AT_MOST -> maxLayoutHeight = heightSize - paddingTop - paddingBottom
        }
        var weightSum = 0f
        var canSlide = false
        val isLayoutRtl = isLayoutRtl
        val widthAvailable = (widthSize - paddingLeft - paddingRight).coerceAtLeast(0)
        var widthRemaining = widthAvailable
        val childCount = childCount
        if (childCount > 2) {
            error("SlidingPaneLayout: More than two child views are not supported.")
        }

        // We'll find the current one below.
        slideableView = null

        // Make kotlinc happy that this can't change while we run measurement
        val allowOverlappingPanes = isOverlappingEnabled

        var skippedChildMeasure = false

        // First pass. Measure based on child LayoutParams width/height.
        // Weight will incur a second pass; distributing leftover space in the event of overlap
        // behaves similar to weight and will also incur a second pass.
        forEachIndexed { i, child ->
            val lp = child.spLayoutParams
            if (child.visibility == GONE) {
                lp.dimWhenOffset = false
                return@forEachIndexed
            }
            if (lp.weight > 0) {
                weightSum += lp.weight

                // If we have no width, weight is the only contributor to the final size.
                // Measure this view on the weight pass only.
                // If we do have width, then we need to measure this child to see how much space
                // is left for other children.
                if (lp.width == 0) {
                    skippedChildMeasure = true
                    return@forEachIndexed
                }
            }
            val horizontalMargin = lp.leftMargin + lp.rightMargin
            val widthAvailableToChild =
                if (allowOverlappingPanes) widthAvailable else widthRemaining
            // When the parent width spec is UNSPECIFIED, measure each of child to get its
            // desired width.
            val childWidthSpec =
                when (lp.width) {
                    WRAP_CONTENT ->
                        MeasureSpec.makeMeasureSpec(
                            (widthAvailableToChild - horizontalMargin).coerceAtLeast(0),
                            if (widthMode == MeasureSpec.UNSPECIFIED) widthMode
                            else MeasureSpec.AT_MOST
                        )
                    MATCH_PARENT ->
                        MeasureSpec.makeMeasureSpec(
                            (widthAvailableToChild - horizontalMargin).coerceAtLeast(0),
                            widthMode
                        )
                    else -> MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
                }
            val childWidthSize = MeasureSpec.getSize(childWidthSpec)
            val childHeightSpec =
                getChildMeasureSpec(
                    heightMeasureSpec,
                    paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin,
                    lp.height
                )
            if (
                allowOverlappingPanes ||
                    lp.canInfluenceParentSize ||
                    MeasureSpec.getMode(childWidthSpec) != MeasureSpec.EXACTLY
            ) {
                child.measure(childWidthSpec, childHeightSpec)
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                if (childHeight > layoutHeight) {
                    if (heightMode == MeasureSpec.AT_MOST) {
                        layoutHeight = childHeight.coerceAtMost(maxLayoutHeight)
                    } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                        layoutHeight = childHeight
                    }
                }
                widthRemaining -= childWidth
            } else {
                // Skip actually measuring, but record how much width it will consume
                widthRemaining -= childWidthSize
                skippedChildMeasure = true
            }
            // Skip first child (list pane), the list pane is always a non-sliding pane.
            if (i > 0) {
                lp.slideable = allowOverlappingPanes && widthRemaining < 0
                canSlide = canSlide or lp.slideable
                if (lp.slideable) {
                    slideableView = child
                }
            }
        }

        // Second pass. Resolve weight. This can still affect the size of the SlidingPaneLayout.
        // Ideally we only measure any given child view once, but if a child has both nonzero
        // lp.width and weight, we have to do both.
        // If overlapping is permitted by [allowOverlappingPanes], child views overlap when
        // the combined width of child views cannot fit into the available width.
        // Each of child views is sized to fill all available space. If there is no overlap,
        // distribute the extra width proportionally to weight.
        if (canSlide || weightSum > 0 || skippedChildMeasure) {
            var totalMeasuredWidth = 0
            forEachIndexed { index, child ->
                if (child.visibility == GONE) return@forEachIndexed
                val lp = child.spLayoutParams
                val skippedFirstPass = !lp.canInfluenceParentSize || lp.weightOnlyWidth
                val measuredWidth = if (skippedFirstPass) 0 else child.measuredWidth
                val newWidth =
                    when {
                        // Child view consumes available space if the combined width cannot fit into
                        // the layout available width.
                        canSlide -> widthAvailable - lp.horizontalMargin
                        lp.weight > 0 -> {
                            val dividerPos = splitDividerPosition
                            if (canSlide || dividerPos == SPLIT_DIVIDER_POSITION_AUTO) {
                                // Distribute the extra width proportionally similar to LinearLayout
                                val widthToDistribute = widthRemaining.coerceAtLeast(0)
                                val addedWidth =
                                    (lp.weight * widthToDistribute / weightSum).roundToInt()
                                measuredWidth + addedWidth
                            } else { // Explicit dividing line is defined
                                val clampedPos =
                                    dividerPos
                                        .coerceAtMost(width - paddingRight)
                                        .coerceAtLeast(paddingLeft)
                                val availableWidthDivider = clampedPos - paddingLeft
                                if ((index == 0) xor isLayoutRtl) {
                                    availableWidthDivider - lp.horizontalMargin
                                } else {
                                    // padding accounted for in widthAvailable;
                                    // dividerPos includes left padding
                                    widthAvailable - lp.horizontalMargin - availableWidthDivider
                                }
                            }
                        }
                        lp.width == MATCH_PARENT -> {
                            widthAvailable - lp.horizontalMargin - totalMeasuredWidth
                        }
                        lp.width > 0 -> lp.width
                        else -> measuredWidth
                    }
                if (skippedFirstPass || measuredWidth != newWidth) {
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
                    val childHeightSpec =
                        getChildHeightMeasureSpec(
                            child,
                            skippedFirstPass,
                            heightMeasureSpec,
                            paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin
                        )
                    child.measure(childWidthSpec, childHeightSpec)
                    val childHeight = child.measuredHeight
                    if (childHeight > layoutHeight) {
                        if (heightMode == MeasureSpec.AT_MOST) {
                            layoutHeight = childHeight.coerceAtMost(maxLayoutHeight)
                        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                            layoutHeight = childHeight
                        }
                    }
                }
                totalMeasuredWidth += newWidth + lp.leftMargin + lp.rightMargin
            }
        }

        // All children have been measured at least once.

        // By this point we know the parent size and whether we have any sliding panes.
        // Set the parent size now and notify observers.
        val measuredHeight = layoutHeight + paddingTop + paddingBottom
        setMeasuredDimension(widthSize, measuredHeight)
        if (canSlide != isSlideable) {
            _isSlideable = canSlide
            overlappingPaneHandler.dispatchSlideableState(canSlide)
        }
        if (!overlappingPaneHandler.isIdle && !canSlide) {
            // Cancel scrolling in progress, it's no longer relevant.
            overlappingPaneHandler.abort()
        }
    }

    /** Returns `true` if any measurement was performed, `false` otherwise */
    private fun remeasureForFoldingFeature(foldingFeature: FoldingFeature): Boolean {
        // At this point, all child views have been measured. Calculate the device fold position
        // in the view. Update the split position to where the fold when it exists.
        val leftSplitBounds = tmpRect
        val rightSplitBounds = tmpRect2
        val hasFold =
            foldBoundsCalculator.splitViewPositions(
                foldingFeature,
                this,
                leftSplitBounds,
                rightSplitBounds
            )
        if (hasFold) {
            // Determine if child configuration would prevent following the fold position;
            // if so, make no changes.
            forEachIndexed { i, child ->
                if (child.visibility == GONE) return@forEachIndexed
                val splitView =
                    when (i) {
                        0 -> leftSplitBounds
                        1 -> rightSplitBounds
                        else -> error("too many children to split")
                    }
                val lp = child.spLayoutParams
                // If the child has been given exact width settings, don't make changes
                if (!lp.canExpandWidth) return false
                // minimumWidth will always be >= 0 so this coerceAtLeast is safe
                val minChildWidth = getMinimumChildWidth(child).coerceAtLeast(lp.width)
                // If the child has a minimum size larger than the area left by the fold's division,
                // don't make changes
                if (minChildWidth + lp.horizontalMargin > splitView.width()) return false
            }

            forEachIndexed { i, child ->
                if (child.visibility == GONE) return@forEachIndexed
                val splitView =
                    when (i) {
                        0 -> leftSplitBounds
                        1 -> rightSplitBounds
                        else -> error("too many children to split")
                    }
                val lp = child.spLayoutParams

                val childWidthSpec =
                    MeasureSpec.makeMeasureSpec(
                        (splitView.width() - lp.horizontalMargin).coerceAtLeast(0),
                        MeasureSpec.EXACTLY
                    )

                // Use the child's existing height; all children have been measured once since
                // their last layout
                val childHeightSpec =
                    MeasureSpec.makeMeasureSpec(child.measuredHeight, MeasureSpec.EXACTLY)

                child.measure(childWidthSpec, childHeightSpec)
            }
        }
        return hasFold
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val foldingFeature = foldingFeature
        if (
            !isSlideable &&
                splitDividerPosition == SPLIT_DIVIDER_POSITION_AUTO &&
                foldingFeature != null
        ) {
            // We can't determine the position of the screen fold relative to the placed
            // SlidingPaneLayout until the SlidingPaneLayout is placed, which is complete
            // when onLayout is called.
            remeasureForFoldingFeature(foldingFeature)
        }

        val isLayoutRtl = isLayoutRtl
        val width = r - l
        val paddingStart = if (isLayoutRtl) paddingRight else paddingLeft
        val paddingEnd = if (isLayoutRtl) paddingLeft else paddingRight
        val paddingTop = paddingTop
        val childCount = childCount
        var xStart = paddingStart
        var nextXStart = xStart
        if (awaitingFirstLayout) {
            currentSlideOffset = if (isSlideable && preservedOpenState) 0f else 1f
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }
            val lp = child.spLayoutParams
            val childWidth = child.measuredWidth
            var offset = 0
            if (lp.slideable) {
                val margin = lp.leftMargin + lp.rightMargin
                val range = nextXStart.coerceAtMost(width - paddingEnd) - xStart - margin
                slideRange = range
                val lpMargin = if (isLayoutRtl) lp.rightMargin else lp.leftMargin
                lp.dimWhenOffset = xStart + lpMargin + range + childWidth / 2 > width - paddingEnd
                val pos = (range * currentSlideOffset).toInt()
                xStart += pos + lpMargin
                currentSlideOffset = pos.toFloat() / slideRange
            } else if (isSlideable && parallaxDistance != 0) {
                offset = ((1 - currentSlideOffset) * parallaxDistance).toInt()
                xStart = nextXStart
            } else {
                xStart = nextXStart
            }
            val childRight: Int
            val childLeft: Int
            if (isLayoutRtl) {
                childRight = width - xStart + offset
                childLeft = childRight - childWidth
            } else {
                childLeft = xStart - offset
                childRight = childLeft + childWidth
            }
            val childBottom = paddingTop + child.measuredHeight
            child.layout(childLeft, paddingTop, childRight, childBottom)

            // If a folding feature separates the content, we use its width as the extra
            // offset for the next child, in order to avoid rendering the content under it.
            var nextXOffset = 0
            if (
                foldingFeature != null &&
                    foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL &&
                    foldingFeature.isSeparating
            ) {
                nextXOffset = foldingFeature.bounds.width()
            }
            nextXStart += child.width + abs(nextXOffset)
        }
        if (isUserResizable) {
            updateGestureExclusion(visualDividerPosition)
            // Force the divider to update and draw
            invalidate()
        } else {
            updateGestureExclusion(-1)
        }
        if (awaitingFirstLayout) {
            if (isSlideable) {
                if (parallaxDistance != 0) {
                    parallaxOtherViews(currentSlideOffset)
                }
            }
            updateObscuredViewsVisibility(slideableView)
        }
        awaitingFirstLayout = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Recalculate sliding panes and their details
        if (w != oldw) {
            awaitingFirstLayout = true
        }
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        super.requestChildFocus(child, focused)
        if (!isInTouchMode && !isSlideable) {
            preservedOpenState = child === slideableView
        }
    }

    private fun selectActiveTouchHandler(): TouchHandler? {
        activeTouchHandler =
            if (isSlideable) {
                overlappingPaneHandler
            } else if (isUserResizable) {
                draggableDividerHandler
            } else null
        return activeTouchHandler
    }

    override fun onInterceptTouchEvent(
        @Suppress("InvalidNullabilityOverride") ev: MotionEvent
    ): Boolean {
        return selectActiveTouchHandler()?.onInterceptTouchEvent(ev) ?: false
    }

    override fun onTouchEvent(@Suppress("InvalidNullabilityOverride") ev: MotionEvent): Boolean {
        return selectActiveTouchHandler()?.onTouchEvent(ev) ?: false
    }

    private fun closePane(initialVelocity: Int): Boolean {
        if (!isSlideable) {
            preservedOpenState = false
        }
        if (awaitingFirstLayout || smoothSlideTo(1f, initialVelocity)) {
            preservedOpenState = false
            return true
        }
        return false
    }

    private fun openPane(initialVelocity: Int): Boolean {
        if (!isSlideable) {
            preservedOpenState = true
        }
        if (awaitingFirstLayout || smoothSlideTo(0f, initialVelocity)) {
            preservedOpenState = true
            return true
        }
        return false
    }

    @Deprecated(
        "Renamed to {@link #openPane()} - this method is going away soon!",
        ReplaceWith("openPane()")
    )
    open fun smoothSlideOpen() {
        openPane()
    }

    /**
     * Open the detail view if it is currently slideable. If first layout has already completed this
     * will animate.
     */
    override fun open() {
        openPane()
    }

    /**
     * Open the detail view if it is currently slideable. If first layout has already completed this
     * will animate.
     *
     * @return true if the pane was slideable and is now open/in the process of opening
     */
    open fun openPane(): Boolean {
        return openPane(0)
    }

    /** @return true if content in this layout can be slid open and closed */
    @Deprecated(
        "Renamed to {@link #isSlideable()} - this method is going away soon!",
        ReplaceWith("isSlideable")
    )
    open fun canSlide(): Boolean {
        return isSlideable
    }

    @Deprecated(
        "Renamed to {@link #closePane()} - this method is going away soon!",
        ReplaceWith("closePane()")
    )
    open fun smoothSlideClosed() {
        closePane()
    }

    /**
     * Close the detail view if it is currently slideable. If first layout has already completed
     * this will animate.
     */
    override fun close() {
        closePane()
    }

    /**
     * Close the detail view if it is currently slideable. If first layout has already completed
     * this will animate.
     *
     * @return true if the pane was slideable and is now closed/in the process of closing
     */
    open fun closePane(): Boolean {
        return closePane(0)
    }

    /**
     * Check if the detail view is completely open. It can be open either because the slider itself
     * is open revealing the detail view, or if all content fits without sliding.
     *
     * @return true if the detail view is completely open
     */
    override fun isOpen(): Boolean {
        return !isSlideable || currentSlideOffset == 0f
    }

    private fun onPanelDragged(newLeft: Int) {
        val slideableView = slideableView
        if (slideableView == null) {
            // This can happen if we're aborting motion during layout because everything now fits.
            currentSlideOffset = 0f
            return
        }
        val isLayoutRtl = isLayoutRtl
        val lp = slideableView.spLayoutParams
        val childWidth = slideableView.width
        val newStart = if (isLayoutRtl) width - newLeft - childWidth else newLeft
        val paddingStart = if (isLayoutRtl) paddingRight else paddingLeft
        val lpMargin = if (isLayoutRtl) lp.rightMargin else lp.leftMargin
        val startBound = paddingStart + lpMargin
        currentSlideOffset = (newStart - startBound).toFloat() / slideRange
        if (parallaxDistance != 0) {
            parallaxOtherViews(currentSlideOffset)
        }
        dispatchOnPanelSlide(slideableView)
    }

    override fun drawChild(
        @Suppress("InvalidNullabilityOverride") canvas: Canvas,
        @Suppress("InvalidNullabilityOverride") child: View,
        drawingTime: Long
    ): Boolean {
        if (isSlideable) {
            val gestureInsets = systemGestureInsets
            if (isLayoutRtl xor isOpen) {
                overlappingPaneHandler.setEdgeTrackingEnabled(
                    ViewDragHelper.EDGE_LEFT,
                    gestureInsets?.left ?: 0
                )
            } else {
                overlappingPaneHandler.setEdgeTrackingEnabled(
                    ViewDragHelper.EDGE_RIGHT,
                    gestureInsets?.right ?: 0
                )
            }
        } else {
            overlappingPaneHandler.disableEdgeTracking()
        }
        val lp = child.spLayoutParams
        val save = canvas.save()
        if (isSlideable && !lp.slideable && slideableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(tmpRect)
            if (isLayoutRtl) {
                tmpRect.left = max(tmpRect.left, slideableView!!.right)
            } else {
                tmpRect.right = min(tmpRect.right, slideableView!!.left)
            }
            canvas.clipRect(tmpRect)
        }
        if (!isSlideable && isChildClippingToResizeDividerEnabled) {
            val visualDividerPosition = visualDividerPosition
            if (visualDividerPosition >= 0) {
                with(tmpRect) {
                    if (isLayoutRtl xor (child === getChildAt(0))) {
                        // left child
                        left = paddingLeft
                        right = visualDividerPosition
                    } else {
                        // right child
                        left = visualDividerPosition
                        right = width - paddingRight
                    }
                    top = paddingTop
                    bottom = height - paddingBottom
                }
                canvas.clipRect(tmpRect)
            }
        }
        return super.drawChild(canvas, child, drawingTime).also { canvas.restoreToCount(save) }
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity initial velocity in case of fling, or 0.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun smoothSlideTo(slideOffset: Float, velocity: Int): Boolean {
        if (!isSlideable) {
            // Nothing to do.
            return false
        }
        val slideableView = slideableView ?: return false
        val isLayoutRtl = isLayoutRtl
        val lp = slideableView.spLayoutParams
        val x: Int =
            if (isLayoutRtl) {
                val startBound = paddingRight + lp.rightMargin
                val childWidth = slideableView.width
                (width - (startBound + slideOffset * slideRange + childWidth)).toInt()
            } else {
                val startBound = paddingLeft + lp.leftMargin
                (startBound + slideOffset * slideRange).toInt()
            }
        if (overlappingPaneHandler.smoothSlideViewTo(slideableView, x, slideableView.top)) {
            setAllChildrenVisible()
            postInvalidateOnAnimation()
            return true
        }
        return false
    }

    override fun computeScroll() {
        overlappingPaneHandler.onComputeScroll()
    }

    /** Set a drawable to use as a shadow. */
    @Deprecated(
        """Renamed to {@link #setShadowDrawableLeft(Drawable d)} to support LTR (left to
      right language) and {@link #setShadowDrawableRight(Drawable d)} to support RTL (right to left
      language) during opening/closing.""",
        ReplaceWith("setShadowDrawableLeft(d)")
    )
    open fun setShadowDrawable(drawable: Drawable?) {
        setShadowDrawableLeft(drawable)
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane during
     * opening/closing.
     */
    open fun setShadowDrawableLeft(drawable: Drawable?) {
        shadowDrawableLeft = drawable
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane during
     * opening/closing to support right to left language.
     */
    open fun setShadowDrawableRight(drawable: Drawable?) {
        shadowDrawableRight = drawable
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane during
     * opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     */
    @Deprecated(
        """Renamed to {@link #setShadowResourceLeft(int)} to support LTR (left to
      right language) and {@link #setShadowResourceRight(int)} to support RTL (right to left
      language) during opening/closing.""",
        ReplaceWith("setShadowResourceLeft(resId)")
    )
    open fun setShadowResource(@DrawableRes resId: Int) {
        setShadowResourceLeft(resId)
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane during
     * opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     */
    open fun setShadowResourceLeft(@DrawableRes resId: Int) {
        setShadowDrawableLeft(ContextCompat.getDrawable(context, resId))
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane during
     * opening/closing to support right to left language.
     *
     * @param resId Resource ID of a drawable to use
     */
    open fun setShadowResourceRight(@DrawableRes resId: Int) {
        setShadowDrawableRight(ContextCompat.getDrawable(context, resId))
    }

    override fun draw(c: Canvas) {
        super.draw(c)
        val isLayoutRtl = isLayoutRtl
        val shadowDrawable: Drawable? =
            if (isLayoutRtl) {
                shadowDrawableRight
            } else {
                shadowDrawableLeft
            }
        val shadowView = if (childCount > 1) getChildAt(1) else null
        if (shadowView != null && shadowDrawable != null) {
            val top = shadowView.top
            val bottom = shadowView.bottom
            val shadowWidth = shadowDrawable.intrinsicWidth
            val left: Int
            val right: Int
            if (this.isLayoutRtl) {
                left = shadowView.right
                right = left + shadowWidth
            } else {
                right = shadowView.left
                left = right - shadowWidth
            }
            shadowDrawable.setBounds(left, top, right, bottom)
            shadowDrawable.draw(c)
        }

        userResizingDividerDrawable
            ?.takeIf { isUserResizable }
            ?.let { divider ->
                updateDividerDrawableBounds(visualDividerPosition)
                divider.draw(c)
            }
    }

    private fun parallaxOtherViews(slideOffset: Float) {
        val isLayoutRtl = isLayoutRtl
        val childCount = childCount
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (v === slideableView) continue
            val oldOffset = ((1 - currentParallaxOffset) * parallaxDistance).toInt()
            currentParallaxOffset = slideOffset
            val newOffset = ((1 - slideOffset) * parallaxDistance).toInt()
            val dx = oldOffset - newOffset
            v.offsetLeftAndRight(if (isLayoutRtl) -dx else dx)
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true), or
     *   just its children (false).
     * @param dx Delta scrolled in pixels
     * @param x X coordinate of the active touch point
     * @param y Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected open fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (v is ViewGroup) {
            val scrollX = v.getScrollX()
            val scrollY = v.getScrollY()
            val count = v.childCount
            // Count backwards - let topmost views consume scroll distance first.
            for (i in count - 1 downTo 0) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                val child = v.getChildAt(i)
                if (
                    x + scrollX >= child.left &&
                        x + scrollX < child.right &&
                        y + scrollY >= child.top &&
                        y + scrollY < child.bottom &&
                        canScroll(
                            child,
                            true,
                            dx,
                            x + scrollX - child.left,
                            y + scrollY - child.top
                        )
                ) {
                    return true
                }
            }
        }
        return checkV && v.canScrollHorizontally(if (isLayoutRtl) dx else -dx)
    }

    private fun isDimmed(child: View?): Boolean {
        if (child == null) {
            return false
        }
        val lp = child.spLayoutParams
        return isSlideable && lp.dimWhenOffset && currentSlideOffset > 0
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams()
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return if (p is MarginLayoutParams) LayoutParams(p) else LayoutParams(p)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams && super.checkLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = SavedState(superState)
        state.isOpen = if (isSlideable) isOpen else preservedOpenState
        state.lockMode = lockMode
        state.splitDividerPosition = splitDividerPosition
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.isOpen) {
            openPane()
        } else {
            closePane()
        }
        preservedOpenState = state.isOpen
        lockMode = state.lockMode
        splitDividerPosition = state.splitDividerPosition
    }

    /**
     * Interceptor for touch events and generic motion events that will accept those event streams
     * in the case where the pane view(s) do not. This prevents touch events from passing through
     * overlapping panes to covered panes below.
     *
     * Ideally SlidingPaneLayout would override dispatchTouchEventForChild instead, but that's not
     * public API. This somewhat breaks the structural contract of child view behavior, but it's
     * been in place for some time as part of the previous implementation prior to the port to
     * Kotlin.
     */
    private inner class TouchBlocker(view: View) : ViewGroup(view.context) {
        init {
            addView(view)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val child = getChildAt(0)
            child.measure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(child.measuredWidth, child.measuredHeight)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            getChildAt(0).layout(0, 0, r - l, b - t)
        }

        override fun getLayoutParams(): LayoutParams = getChildAt(0).layoutParams

        override fun setLayoutParams(lp: LayoutParams) {
            getChildAt(0).layoutParams = lp
        }

        override fun checkLayoutParams(p: LayoutParams?): Boolean =
            this@SlidingPaneLayout.checkLayoutParams(p)

        override fun generateDefaultLayoutParams(): LayoutParams =
            this@SlidingPaneLayout.generateDefaultLayoutParams()

        override fun generateLayoutParams(p: LayoutParams?): LayoutParams =
            this@SlidingPaneLayout.generateLayoutParams(p)

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return isSlideable
        }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
            return isSlideable
        }
    }

    open class LayoutParams : MarginLayoutParams {
        /**
         * The weighted proportion of how much of the leftover space this child should consume after
         * measurement.
         */
        @JvmField var weight = 0f

        /** True if this pane is the slideable pane in the layout. */
        @JvmField internal var slideable = false

        /**
         * True if this view should be drawn dimmed when it's been offset from its default position.
         */
        @JvmField internal var dimWhenOffset = false

        internal inline val horizontalMargin: Int
            get() = leftMargin + rightMargin

        constructor() : super(MATCH_PARENT, MATCH_PARENT)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: MarginLayoutParams) : super(source)

        constructor(source: LayoutParams) : super(source) {
            weight = source.weight
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            context.withStyledAttributes(attrs, R.styleable.SlidingPaneLayout_Layout) {
                weight = getFloat(R.styleable.SlidingPaneLayout_Layout_android_layout_weight, 0f)
            }
        }
    }

    internal class SavedState : AbsSavedState {
        var isOpen = false

        @LockMode var lockMode = 0

        /**
         * This saves the raw pixel position of the split, or the AUTO constant. Using raw pixel
         * position will bias toward the list pane in a list/detail arrangement remaining stable in
         * size even if the window size changes across configurations. This does NOT (yet) elegantly
         * handle density changes, or customization of biasing the resize divider point toward one
         * pane or the other based on a different developer intent.
         */
        var splitDividerPosition: Int = SPLIT_DIVIDER_POSITION_AUTO

        constructor(superState: Parcelable?) : super(superState!!)

        constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
            isOpen = parcel.readInt() != 0
            lockMode = parcel.readInt()
            splitDividerPosition = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (isOpen) 1 else 0)
            out.writeInt(lockMode)
            out.writeInt(splitDividerPosition)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object : ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(parcel: Parcel, loader: ClassLoader): SavedState {
                        return SavedState(parcel, null)
                    }

                    override fun createFromParcel(parcel: Parcel): SavedState {
                        return SavedState(parcel, null)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }

    internal inner class AccessibilityDelegate : AccessibilityDelegateCompat() {
        private val tmpRect = Rect()

        override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfoCompat
        ) {
            val superNode = AccessibilityNodeInfoCompat.obtain(info)
            super.onInitializeAccessibilityNodeInfo(host, superNode)
            copyNodeInfoNoChildren(info, superNode)
            @Suppress("Deprecation") superNode.recycle()
            info.className = ACCESSIBILITY_CLASS_NAME
            info.setSource(host)
            val parent = host.getParentForAccessibility()
            if (parent is View) {
                info.setParent(parent as View)
            }

            // This is a best-approximation of addChildrenForAccessibility()
            // that accounts for filtering.
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (!filter(child) && child.visibility == VISIBLE) {
                    // Force importance to "yes" since we can't read the value.
                    child.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
                    info.addChild(child)
                }
            }
        }

        override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
            super.onInitializeAccessibilityEvent(host, event)
            event.className = ACCESSIBILITY_CLASS_NAME
        }

        override fun onRequestSendAccessibilityEvent(
            host: ViewGroup,
            child: View,
            event: AccessibilityEvent
        ): Boolean {
            return if (!filter(child)) {
                super.onRequestSendAccessibilityEvent(host, child, event)
            } else false
        }

        fun filter(child: View?): Boolean {
            return isDimmed(child)
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately seem to be
         * a few elements that are not easily cloneable using the underlying API. Leave it private
         * here as it's not general-purpose useful.
         */
        private fun copyNodeInfoNoChildren(
            dest: AccessibilityNodeInfoCompat,
            src: AccessibilityNodeInfoCompat
        ) {
            val rect = tmpRect
            src.getBoundsInScreen(rect)
            dest.setBoundsInScreen(rect)
            dest.isVisibleToUser = src.isVisibleToUser
            dest.packageName = src.packageName
            dest.className = src.className
            dest.contentDescription = src.contentDescription
            dest.isEnabled = src.isEnabled
            dest.isClickable = src.isClickable
            dest.isFocusable = src.isFocusable
            dest.isFocused = src.isFocused
            dest.isAccessibilityFocused = src.isAccessibilityFocused
            dest.isSelected = src.isSelected
            dest.isLongClickable = src.isLongClickable
            @Suppress("Deprecation") dest.addAction(src.actions)
            dest.movementGranularities = src.movementGranularities
        }
    }

    /** Listener to whether the SlidingPaneLayout is slideable or is a fixed width. */
    fun interface SlideableStateListener {
        /**
         * Called when onMeasure has measured out the total width of the added layouts within
         * SlidingPaneLayout
         *
         * @param isSlideable Returns true if the current SlidingPaneLayout has the ability to
         *   slide, returns false if the SlidingPaneLayout is a fixed width.
         */
        fun onSlideableStateChanged(isSlideable: Boolean)
    }

    /** Listener for monitoring events about sliding panes. */
    interface PanelSlideListener {
        /**
         * Called when a detail view's position changes.
         *
         * @param panel The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        fun onPanelSlide(panel: View, slideOffset: Float)

        /**
         * Called when a detail view becomes slid completely open.
         *
         * @param panel The detail view that was slid to an open position
         */
        fun onPanelOpened(panel: View)

        /**
         * Called when a detail view becomes slid completely closed.
         *
         * @param panel The detail view that was slid to a closed position
         */
        fun onPanelClosed(panel: View)
    }

    /**
     * No-op stubs for [PanelSlideListener]. If you only want to implement a subset of the listener
     * methods you can extend this instead of implement the full interface.
     */
    open class SimplePanelSlideListener : PanelSlideListener {
        override fun onPanelSlide(panel: View, slideOffset: Float) {}

        override fun onPanelOpened(panel: View) {}

        override fun onPanelClosed(panel: View) {}
    }

    /** Used to switch gesture handling modes */
    internal interface TouchHandler {
        fun onInterceptTouchEvent(ev: MotionEvent): Boolean

        fun onTouchEvent(ev: MotionEvent): Boolean
    }

    private inner class OverlappingPaneHandler : ViewDragHelper.Callback(), TouchHandler {
        /**
         * A panel view is locked into internal scrolling or another condition that is preventing a
         * drag.
         */
        private var isUnableToDrag = false

        private var initialMotionX = 0f
        private var initialMotionY = 0f
        private val slideableStateListeners: MutableList<SlideableStateListener> =
            CopyOnWriteArrayList()
        private val panelSlideListeners: MutableList<PanelSlideListener> = CopyOnWriteArrayList()
        private var singlePanelSlideListener: PanelSlideListener? = null
        private val dragHelper =
            ViewDragHelper.create(this@SlidingPaneLayout, 0.5f, this).apply {
                minVelocity = MIN_FLING_VELOCITY * context.resources.displayMetrics.density
            }

        val isIdle: Boolean
            get() = dragHelper.viewDragState == ViewDragHelper.STATE_IDLE

        fun abort() = dragHelper.abort()

        fun onComputeScroll() {
            if (dragHelper.continueSettling(true)) {
                if (!isSlideable) {
                    dragHelper.abort()
                    return
                }
                postInvalidateOnAnimation()
            }
        }

        fun smoothSlideViewTo(view: View, left: Int, top: Int): Boolean =
            dragHelper.smoothSlideViewTo(view, left, top)

        fun setPanelSlideListener(listener: PanelSlideListener?) {
            // The logic in this method emulates what we had before support for multiple
            // registered listeners.
            singlePanelSlideListener?.let { removePanelSlideListener(it) }
            listener?.let { addPanelSlideListener(it) }
            // Update the deprecated field so that we can remove the passed listener the next
            // time we're called
            singlePanelSlideListener = listener
        }

        fun addSlideableStateListener(listener: SlideableStateListener) {
            slideableStateListeners.add(listener)
        }

        fun removeSlideableStateListener(listener: SlideableStateListener) {
            slideableStateListeners.remove(listener)
        }

        fun dispatchSlideableState(isSlideable: Boolean) {
            for (listener in slideableStateListeners) {
                listener.onSlideableStateChanged(isSlideable)
            }
        }

        fun addPanelSlideListener(listener: PanelSlideListener) {
            panelSlideListeners.add(listener)
        }

        fun removePanelSlideListener(listener: PanelSlideListener) {
            panelSlideListeners.remove(listener)
        }

        fun dispatchOnPanelSlide(panel: View, slideOffset: Float) {
            for (listener in panelSlideListeners) {
                listener.onPanelSlide(panel, slideOffset)
            }
        }

        fun dispatchOnPanelOpened(panel: View?) {
            if (panel != null) {
                for (listener in panelSlideListeners) {
                    listener.onPanelOpened(panel)
                }
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            }
        }

        fun dispatchOnPanelClosed(panel: View?) {
            if (panel != null) {
                for (listener in panelSlideListeners) {
                    listener.onPanelClosed(panel)
                }
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            }
        }

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return if (!isDraggable) {
                false
            } else child.spLayoutParams.slideable
        }

        override fun onViewDragStateChanged(state: Int) {
            if (dragHelper.viewDragState == ViewDragHelper.STATE_IDLE) {
                preservedOpenState =
                    if (currentSlideOffset == 1f) {
                        updateObscuredViewsVisibility(slideableView)
                        dispatchOnPanelClosed(slideableView)
                        false
                    } else {
                        dispatchOnPanelOpened(slideableView)
                        true
                    }
            }
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            // Make all child views visible in preparation for sliding things around
            setAllChildrenVisible()
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            onPanelDragged(left)
            invalidate()
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val lp = releasedChild.spLayoutParams
            var left: Int
            if (isLayoutRtl) {
                var startToRight = paddingRight + lp.rightMargin
                if (xvel < 0 || xvel == 0f && currentSlideOffset > 0.5f) {
                    startToRight += slideRange
                }
                val childWidth = slideableView!!.width
                left = width - startToRight - childWidth
            } else {
                left = paddingLeft + lp.leftMargin
                if (xvel > 0 || xvel == 0f && currentSlideOffset > 0.5f) {
                    left += slideRange
                }
            }
            dragHelper.settleCapturedViewAt(left, releasedChild.top)
            invalidate()
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return slideRange
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            var newLeft = left
            val slideableView = checkNotNull(slideableView)
            val lp = slideableView.spLayoutParams
            newLeft =
                if (isLayoutRtl) {
                    val startBound = (width - (paddingRight + lp.rightMargin + slideableView.width))
                    val endBound = startBound - slideRange
                    newLeft.coerceIn(endBound, startBound)
                } else {
                    val startBound = paddingLeft + lp.leftMargin
                    val endBound = startBound + slideRange
                    newLeft.coerceIn(startBound, endBound)
                }
            return newLeft
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            // Make sure we never move views vertically.
            // This could happen if the child has less height than its parent.
            return child.top
        }

        override fun onEdgeTouched(edgeFlags: Int, pointerId: Int) {
            if (!isDraggable) {
                return
            }
            dragHelper.captureChildView(slideableView!!, pointerId)
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            if (!isDraggable) {
                return
            }
            dragHelper.captureChildView(slideableView!!, pointerId)
        }

        val isDraggable: Boolean
            get() {
                if (isUnableToDrag) return false
                if (lockMode == LOCK_MODE_LOCKED) return false
                if (isOpen && lockMode == LOCK_MODE_LOCKED_OPEN) return false
                return !(!isOpen && lockMode == LOCK_MODE_LOCKED_CLOSED)
            }

        fun setEdgeTrackingEnabled(edgeFlags: Int, size: Int) {
            dragHelper.setEdgeTrackingEnabled(edgeFlags)
            dragHelper.edgeSize = size.coerceAtLeast(dragHelper.defaultEdgeSize)
        }

        fun disableEdgeTracking() {
            dragHelper.setEdgeTrackingEnabled(0)
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            val action = ev.actionMasked

            // Preserve the open state based on the last view that was touched.
            if (!isSlideable && action == MotionEvent.ACTION_DOWN && childCount > 1) {
                // After the first things will be slideable.
                val secondChild = getChildAt(1)
                if (secondChild != null) {
                    preservedOpenState =
                        dragHelper.isViewUnder(secondChild, ev.x.toInt(), ev.y.toInt())
                }
            }
            if (!isSlideable || isUnableToDrag && action != MotionEvent.ACTION_DOWN) {
                dragHelper.cancel()
                return super@SlidingPaneLayout.onInterceptTouchEvent(ev)
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                dragHelper.cancel()
                return false
            }
            var interceptTap = false
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    isUnableToDrag = false
                    val x = ev.x
                    val y = ev.y
                    initialMotionX = x
                    initialMotionY = y
                    if (
                        dragHelper.isViewUnder(slideableView, x.toInt(), y.toInt()) &&
                            isDimmed(slideableView)
                    ) {
                        interceptTap = true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = ev.x
                    val y = ev.y
                    val adx = abs(x - initialMotionX)
                    val ady = abs(y - initialMotionY)
                    val slop = dragHelper.touchSlop
                    if (adx > slop && ady > adx) {
                        dragHelper.cancel()
                        isUnableToDrag = true
                        return false
                    }
                }
            }
            val interceptForDrag = dragHelper.shouldInterceptTouchEvent(ev)
            return interceptForDrag || interceptTap
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (!isSlideable) {
                return super@SlidingPaneLayout.onTouchEvent(ev)
            }
            dragHelper.processTouchEvent(ev)
            val wantTouchEvents = true
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val x = ev.x
                    val y = ev.y
                    initialMotionX = x
                    initialMotionY = y
                }
                MotionEvent.ACTION_UP -> {
                    if (isDimmed(slideableView)) {
                        val x = ev.x
                        val y = ev.y
                        val dx = x - initialMotionX
                        val dy = y - initialMotionY
                        val slop = dragHelper.touchSlop
                        if (
                            dx * dx + dy * dy < slop * slop &&
                                dragHelper.isViewUnder(slideableView, x.toInt(), y.toInt())
                        ) {
                            // Taps close a dimmed open pane.
                            closePane(0)
                        }
                    }
                }
            }
            return wantTouchEvents
        }
    }

    private inner class DraggableDividerHandler :
        AbsDraggableDividerHandler(touchSlop = ViewConfiguration.get(context).scaledTouchSlop) {
        private val tmpTargetRect = Rect()

        // Implementation note: this doesn't use the drawable bounds directly since drawing
        // is what configures the bounds; this function may be checked prior to that update step
        override fun dividerBoundsContains(x: Int, y: Int): Boolean =
            computeDividerTargetRect(tmpTargetRect, visualDividerPosition).contains(x, y)

        override fun clampDraggingDividerPosition(proposedPositionX: Int): Int {
            val leftChild: View
            val rightChild: View
            if (isLayoutRtl) {
                leftChild = getChildAt(1)
                rightChild = getChildAt(0)
            } else {
                leftChild = getChildAt(0)
                rightChild = getChildAt(1)
            }
            return proposedPositionX.coerceIn(
                paddingLeft +
                    leftChild.spLayoutParams.horizontalMargin +
                    getMinimumChildWidth(leftChild),
                width -
                    paddingRight -
                    rightChild.spLayoutParams.horizontalMargin -
                    getMinimumChildWidth(rightChild)
            )
        }

        override fun onUserResizeStarted() {
            userResizeBehavior.onUserResizeStarted(this@SlidingPaneLayout, dragPositionX)
            drawableStateChanged()
        }

        override fun onUserResizeProgress() {
            userResizeBehavior.onUserResizeProgress(this@SlidingPaneLayout, dragPositionX)
            invalidate()
        }

        override fun onUserResizeComplete(wasCancelled: Boolean) {
            if (wasCancelled) {
                userResizeBehavior.onUserResizeCancelled(this@SlidingPaneLayout, dragPositionX)
            } else {
                userResizeBehavior.onUserResizeComplete(this@SlidingPaneLayout, dragPositionX)
            }
            invalidate()
        }

        override fun onDividerClicked() {
            onUserResizingDividerClickListener?.onClick(this@SlidingPaneLayout)
        }
    }

    /** The state machine for working with divider dragging user input */
    internal abstract class AbsDraggableDividerHandler(private val touchSlop: Int) : TouchHandler {

        private var xDown = Float.NaN

        /** `true` if the user is actively dragging */
        var isDragging: Boolean = false
            private set

        /** X position of a drag in progress or -1 if no drag in progress */
        var dragPositionX: Int = -1
            private set

        /** returns `true` if the divider's visual bounds contain the point `(x, y)` */
        abstract fun dividerBoundsContains(x: Int, y: Int): Boolean

        open fun clampDraggingDividerPosition(proposedPositionX: Int): Int = proposedPositionX

        /** Called when a user resize begins; [isDragging] has changed from false to true */
        open fun onUserResizeStarted() {}

        /** Called when [dragPositionX] has changed as a result of user resize */
        open fun onUserResizeProgress() {}

        /** Called when user resizing has ended; [dragPositionX] represents the end position */
        open fun onUserResizeComplete(wasCancelled: Boolean) {}

        /** Called when the divider is touched and released without crossing [touchSlop] */
        open fun onDividerClicked() {}

        private fun commonActionDown(ev: MotionEvent): Boolean =
            if (dividerBoundsContains(ev.x.roundToInt(), ev.y.roundToInt())) {
                xDown = ev.x
                if (touchSlop == 0) {
                    isDragging = true
                    dragPositionX = clampDraggingDividerPosition(ev.x.roundToInt())
                    onUserResizeStarted()
                }
                true
            } else false

        private fun commonActionMove(ev: MotionEvent): Boolean =
            if (!xDown.isNaN()) {
                var startedDrag = false
                if (!isDragging) {
                    val dx = ev.x - xDown
                    if (abs(dx) >= touchSlop) {
                        isDragging = true
                        startedDrag = true
                    }
                }
                // Second if instead of else because isDragging can change above
                if (isDragging) {
                    val newPosition = clampDraggingDividerPosition(ev.x.roundToInt())
                    dragPositionX = newPosition
                    if (startedDrag) onUserResizeStarted()
                    onUserResizeProgress()
                }
                true
            } else false

        final override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> if (commonActionDown(ev) && isDragging) return true
                MotionEvent.ACTION_MOVE -> if (commonActionMove(ev) && isDragging) return true
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) xDown = Float.NaN
                }
            }
            return false
        }

        final override fun onTouchEvent(ev: MotionEvent): Boolean =
            when (val action = ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> commonActionDown(ev)
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL ->
                    if (!xDown.isNaN()) {
                        xDown = Float.NaN
                        if (isDragging) {
                            isDragging = false
                            onUserResizeComplete(wasCancelled = action == MotionEvent.ACTION_CANCEL)
                            dragPositionX = -1
                        } else if (
                            action == MotionEvent.ACTION_UP &&
                                dividerBoundsContains(ev.x.roundToInt(), ev.y.roundToInt())
                        ) {
                            onDividerClicked()
                        }
                        true
                    } else false
                // Moves are only valid if we got the initial down event
                MotionEvent.ACTION_MOVE -> commonActionMove(ev)
                else -> false
            }
    }

    /**
     * Policy implementation for user resizing. See [USER_RESIZE_RELAYOUT_WHEN_COMPLETE] or
     * [USER_RESIZE_RELAYOUT_WHEN_MOVED] for default implementations, or this interface may be
     * implemented externally to apply additional behaviors such as snapping to predefined
     * breakpoints.
     */
    interface UserResizeBehavior {
        /**
         * Called when a user resize begins and the user is now dragging the divider.
         *
         * @param slidingPaneLayout the layout being manipulated in case of stateless behaviors
         * @param dividerPositionX the X coordinate of the divider being dragged in pixels
         */
        fun onUserResizeStarted(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int)

        /**
         * Called when a user resize has progressed to a new divider position.
         *
         * @param slidingPaneLayout the layout being manipulated in case of stateless behaviors
         * @param dividerPositionX the X coordinate of the divider being dragged in pixels
         */
        fun onUserResizeProgress(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int)

        /**
         * Called when a user resize completed successfully; the user let go of the divider with
         * intent to reposition it.
         *
         * @param slidingPaneLayout the layout being manipulated in case of stateless behaviors
         * @param dividerPositionX the X coordinate of the divider being dragged in pixels
         */
        fun onUserResizeComplete(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int)

        /**
         * Called when a user resize has been cancelled; typically another ancestor view has
         * intercepted the touch event stream for the gesture.
         *
         * @param slidingPaneLayout the layout being manipulated in case of stateless behaviors
         * @param dividerPositionX the X coordinate of the divider being dragged in pixels
         */
        fun onUserResizeCancelled(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int)
    }

    companion object {
        /** User can freely swipe between list and detail panes. */
        const val LOCK_MODE_UNLOCKED = 0

        /**
         * The detail pane is locked in an open position. The user cannot swipe to close the detail
         * pane, but the app can close the detail pane programmatically.
         */
        const val LOCK_MODE_LOCKED_OPEN = 1

        /**
         * The detail pane is locked in a closed position. The user cannot swipe to open the detail
         * pane, but the app can open the detail pane programmatically.
         */
        const val LOCK_MODE_LOCKED_CLOSED = 2

        /**
         * The user cannot swipe between list and detail panes, though the app can open or close the
         * detail pane programmatically.
         */
        const val LOCK_MODE_LOCKED = 3

        /**
         * Value for [splitDividerPosition] indicating that the position should be automatically
         * determined by other layout policy (e.g. [LayoutParams.weight]) rather than set to a
         * specific pixel value. [visualDividerPosition] will continue to reflect the currently
         * displayed position of the divider.
         */
        const val SPLIT_DIVIDER_POSITION_AUTO = -1

        /**
         * [UserResizeBehavior] where the divider can be released at any position respecting the
         * minimum sizes of each pane view. Relayout occurs only when the divider is released.
         *
         * See [setUserResizeBehavior].
         */
        @JvmField
        val USER_RESIZE_RELAYOUT_WHEN_COMPLETE: UserResizeBehavior =
            object : UserResizeBehavior {
                override fun onUserResizeStarted(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }

                override fun onUserResizeProgress(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }

                override fun onUserResizeComplete(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    slidingPaneLayout.splitDividerPosition = dividerPositionX
                }

                override fun onUserResizeCancelled(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }
            }

        /**
         * [UserResizeBehavior] where the divider can be released at any position respecting the
         * minimum sizes of each pane view, but relayout will occur on each frame when the divider
         * is moved. This setting can have significant performance implications on complex layouts.
         *
         * See [setUserResizeBehavior].
         */
        @JvmField
        val USER_RESIZE_RELAYOUT_WHEN_MOVED: UserResizeBehavior =
            object : UserResizeBehavior {
                override fun onUserResizeStarted(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }

                override fun onUserResizeProgress(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    slidingPaneLayout.splitDividerPosition = dividerPositionX
                }

                override fun onUserResizeComplete(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }

                override fun onUserResizeCancelled(
                    slidingPaneLayout: SlidingPaneLayout,
                    dividerPositionX: Int
                ) {
                    // Do nothing
                }
            }
    }
}
