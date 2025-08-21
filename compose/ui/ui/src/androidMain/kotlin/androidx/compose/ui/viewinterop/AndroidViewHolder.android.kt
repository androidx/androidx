/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.graphics.Rect as AndroidRect
import android.graphics.Region
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.composeToViewOffset
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastRoundToInt
import androidx.core.graphics.Insets
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.launch

/**
 * A base class used to host a [View] inside Compose. This API is not designed to be used directly,
 * but rather using the [AndroidView] and `AndroidViewBinding` APIs, which are built on top of
 * [AndroidViewHolder].
 *
 * @param view The view hosted by this holder.
 * @param owner The [Owner] of the composition that this holder lives in.
 */
internal open class AndroidViewHolder(
    context: Context,
    parentContext: CompositionContext?,
    private val compositeKeyHash: Int,
    private val dispatcher: NestedScrollDispatcher,
    val view: View,
    private val owner: Owner,
) :
    ViewGroup(context),
    NestedScrollingParent3,
    ComposeNodeLifecycleCallback,
    OwnerScope,
    OnApplyWindowInsetsListener {

    init {
        // Any [Abstract]ComposeViews that are descendants of this view will host
        // subcompositions of the host composition.
        // UiApplier doesn't supply this, only AndroidView.
        parentContext?.let { compositionContext = it }
        // We save state ourselves, depending on composition.
        isSaveFromParentEnabled = false

        @Suppress("LeakingThis") addView(view)
        ViewCompat.setWindowInsetsAnimationCallback(
            this,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat = insetBounds(bounds)

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat = insetToLayoutPosition(insets)
            },
        )
        ViewCompat.setOnApplyWindowInsetsListener(this, this)
    }

    // Keep nullable to match the `expect` declaration of InteropViewFactoryHolder
    @Suppress("RedundantNullableReturnType") fun getInteropView(): InteropView? = view

    /** The update logic of the [View]. */
    var update: () -> Unit = {}
        protected set(value) {
            field = value
            hasUpdateBlock = true
            runUpdate()
        }

    private var hasUpdateBlock = false

    var reset: () -> Unit = {}
        protected set

    var release: () -> Unit = {}
        protected set

    /** The modifier of the `LayoutNode` corresponding to this [View]. */
    var modifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke(value)
            }
        }

    internal var onModifierChanged: ((Modifier) -> Unit)? = null

    /** The screen density of the layout. */
    var density: Density = Density(1f)
        set(value) {
            if (value !== field) {
                field = value
                onDensityChanged?.invoke(value)
            }
        }

    internal var onDensityChanged: ((Density) -> Unit)? = null

    /** Sets the ViewTreeLifecycleOwner for this view. */
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            if (value !== field) {
                field = value
                setViewTreeLifecycleOwner(value)
            }
        }

    /** Sets the ViewTreeSavedStateRegistryOwner for this view. */
    var savedStateRegistryOwner: SavedStateRegistryOwner? = null
        set(value) {
            if (value !== field) {
                field = value
                setViewTreeSavedStateRegistryOwner(value)
            }
        }

    private val position = IntArray(2)
    private var size = IntSize.Zero
    private var insets: WindowInsetsCompat? = null
    private var bringIntoViewRequester: BringIntoViewRequester? = null

    /**
     * The [OwnerSnapshotObserver] of this holder's [Owner]. Will be null when this view is not
     * attached, since the observer is not valid unless the view is attached.
     */
    private val snapshotObserver: OwnerSnapshotObserver
        get() {
            checkPrecondition(isAttachedToWindow) {
                "Expected AndroidViewHolder to be attached when observing reads."
            }
            return owner.snapshotObserver
        }

    private val runUpdate: () -> Unit = {
        // If we're not attached, the observer isn't started, so don't bother running it.
        // onAttachedToWindow will run an update the next time the view is attached.
        // Also, the view will have no parent when the node is deactivated. when the node will
        // be reactivated the update block will be re-executed.
        if (hasUpdateBlock && isAttachedToWindow && view.parent === this) {
            snapshotObserver.observeReads(this, OnCommitAffectingUpdate, update)
        }
    }

    private val runInvalidate: () -> Unit = { layoutNode.invalidateLayer() }

    internal var onRequestDisallowInterceptTouchEvent: ((Boolean) -> Unit)? = null

    private val location = IntArray(2)

    private var lastWidthMeasureSpec: Int = Unmeasured
    private var lastHeightMeasureSpec: Int = Unmeasured

    private val nestedScrollingParentHelper: NestedScrollingParentHelper =
        NestedScrollingParentHelper(this)

    private var isDrawing = false

    override val isValidOwnerScope: Boolean
        get() = isAttachedToWindow

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    override fun onReuse() {
        // We reset at the same time we remove the view. So if the view was removed, we can just
        // re-add it and it's ready to go. If it's already attached, we didn't reset it and need
        // to do so for it to be reused correctly.
        if (view.parent !== this) {
            addView(view)
        } else {
            reset()
        }
    }

    override fun onDeactivate() {
        reset()
        removeAllViewsInLayout()
    }

    override fun onRelease() {
        release()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (view.parent !== this) {
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec),
            )
            return
        }
        if (view.visibility == GONE) {
            setMeasuredDimension(0, 0)
            return
        }

        view.measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(view.measuredWidth, view.measuredHeight)
        lastWidthMeasureSpec = widthMeasureSpec
        lastHeightMeasureSpec = heightMeasureSpec
    }

    fun remeasure() {
        if (lastWidthMeasureSpec == Unmeasured || lastHeightMeasureSpec == Unmeasured) {
            // This should never happen: it means that the views handler was measured without
            // the AndroidComposeView having been measured.
            return
        }
        measure(lastWidthMeasureSpec, lastHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        view.layout(0, 0, r - l, b - t)
    }

    override fun getLayoutParams(): LayoutParams? {
        return view.layoutParams
            ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        onRequestDisallowInterceptTouchEvent?.invoke(disallowIntercept)
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        runUpdate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // remove all observations:
        snapshotObserver.clear(this)
    }

    // When there is no hardware acceleration invalidates are intercepted using this method,
    // otherwise using onDescendantInvalidated. Return null to avoid invalidating the
    // AndroidComposeView or the handler.
    @Suppress("OVERRIDE_DEPRECATION", "Deprecation")
    override fun invalidateChildInParent(location: IntArray?, dirty: AndroidRect?): ViewParent? {
        super.invalidateChildInParent(location, dirty)
        invalidateOrDefer()
        return null
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        // We need to call super here in order to correctly update the dirty flags of the holder.
        super.onDescendantInvalidated(child, target)
        invalidateOrDefer()
    }

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: AndroidRect?,
        immediate: Boolean,
    ): Boolean {
        bringIntoViewRequester?.invoke(rectangle?.toComposeRect())
        // Compose doesn't tell us whether this request caused a scroll. Return true in any case.
        return true
    }

    fun invalidateOrDefer() {
        if (isDrawing) {
            // If an invalidation occurs while drawing invalidate until next frame to avoid
            // redrawing multiple times during the same frame the same content.
            view.postOnAnimation(runInvalidate)
        } else {
            // when not drawing, we can invalidate any time and not risk multiple draws, we don't
            // defer to avoid waiting a full frame to draw content.
            layoutNode.invalidateLayer()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // On Lollipop, when the Window becomes visible, child Views need to be explicitly
        // invalidated for some reason.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && visibility == View.VISIBLE) {
            layoutNode.invalidateLayer()
        }
    }

    // Always mark the region of the View to not be transparent to disable an optimisation which
    // would otherwise cause certain buggy drawing scenarios. For example, Compose drawing on top
    // of SurfaceViews included in Compose would sometimes not be displayed, as the drawing is
    // not done by Views, therefore the area is not known as non-transparent to the View system.
    override fun gatherTransparentRegion(region: Region?): Boolean {
        if (region == null) return true
        getLocationInWindow(location)
        region.op(
            location[0],
            location[1],
            location[0] + width,
            location[1] + height,
            Region.Op.DIFFERENCE,
        )
        return true
    }

    /**
     * A [LayoutNode] tree representation for this Android [View] holder. The [LayoutNode] will
     * proxy the Compose core calls to the [View].
     */
    val layoutNode: LayoutNode = run {
        // Prepare layout node that proxies measure and layout passes to the View.
        val layoutNode = LayoutNode()
        @OptIn(InternalComposeUiApi::class)
        layoutNode.interopViewFactoryHolder = this@AndroidViewHolder

        val coreModifier =
            Modifier.nestedScroll(NoOpScrollConnection, dispatcher)
                .semantics(true) {}
                .pointerInteropFilter(this)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        if (view.visibility != GONE) {
                            isDrawing = true
                            (layoutNode.owner as? AndroidComposeView)?.drawAndroidView(
                                this@AndroidViewHolder,
                                canvas.nativeCanvas,
                            )
                            isDrawing = false
                        }
                    }
                }
                .onGloballyPositioned {
                    // The global position of this LayoutNode can change with it being replaced. For
                    // these cases, we need to inform the View.
                    layoutAccordingTo(layoutNode)
                    @OptIn(InternalComposeUiApi::class) owner.onInteropViewLayoutChange(this)
                    val previousX = position[0]
                    val previousY = position[1]
                    view.getLocationOnScreen(position)
                    val oldSize = size
                    size = it.size
                    val previouslyDispatchedInsets = insets
                    if (previouslyDispatchedInsets != null) {
                        if (
                            previousX != position[0] || previousY != position[1] || oldSize != size
                        ) {
                            // If we have previously been dispatched insets (no parents consumed
                            // the insets already), we need to dispatch the insets again when the
                            // view is moved as this could cause the insets (once we account for
                            // the layout position) to change.
                            insetToLayoutPosition(previouslyDispatchedInsets)
                                .toWindowInsets()
                                ?.let { translatedInsets ->
                                    // Re-dispatch the insets - we do this instead of calling
                                    // requestApplyInsets() as that schedules a full traversal of
                                    // the
                                    // view hierarchy - there is no need to do that when only the
                                    // AndroidView moved. Other changes that cause insets to change
                                    // will be dispatched as normal.
                                    view.dispatchApplyWindowInsets(translatedInsets)
                                }
                        }
                    }
                }
                .then(BringIntoViewElement { bringIntoViewRequester = it })
        layoutNode.compositeKeyHash = compositeKeyHash
        layoutNode.modifier = modifier.then(coreModifier)
        onModifierChanged = { layoutNode.modifier = it.then(coreModifier) }

        layoutNode.density = density
        onDensityChanged = { layoutNode.density = it }

        layoutNode.onAttach = { owner ->
            (owner as? AndroidComposeView)?.addAndroidView(this, layoutNode)
            if (view.parent !== this) addView(view)
        }
        layoutNode.onDetach = { owner ->
            @OptIn(ExperimentalComposeUiApi::class)
            if (ComposeUiFlags.isViewFocusFixEnabled && hasFocus()) {
                owner.focusOwner.clearFocus(true)
            }
            (owner as? AndroidComposeView)?.removeAndroidView(this)
            removeAllViewsInLayout()
        }

        layoutNode.measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    if (childCount == 0) {
                        return layout(constraints.minWidth, constraints.minHeight) {}
                    }

                    if (constraints.minWidth != 0) {
                        getChildAt(0).minimumWidth = constraints.minWidth
                    }
                    if (constraints.minHeight != 0) {
                        getChildAt(0).minimumHeight = constraints.minHeight
                    }

                    measure(
                        obtainMeasureSpec(
                            constraints.minWidth,
                            constraints.maxWidth,
                            layoutParams!!.width,
                        ),
                        obtainMeasureSpec(
                            constraints.minHeight,
                            constraints.maxHeight,
                            layoutParams!!.height,
                        ),
                    )
                    return layout(measuredWidth, measuredHeight) { layoutAccordingTo(layoutNode) }
                }

                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ) = intrinsicWidth(height)

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ) = intrinsicWidth(height)

                private fun intrinsicWidth(height: Int): Int {
                    measure(
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        obtainMeasureSpec(0, height, layoutParams!!.height),
                    )
                    return measuredWidth
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ) = intrinsicHeight(width)

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ) = intrinsicHeight(width)

                private fun intrinsicHeight(width: Int): Int {
                    measure(
                        obtainMeasureSpec(0, width, layoutParams!!.width),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    )
                    return measuredHeight
                }
            }
        layoutNode
    }

    /**
     * Intersects [Constraints] and [View] LayoutParams to obtain the suitable [View.MeasureSpec]
     * for measuring the [View].
     */
    private fun obtainMeasureSpec(min: Int, max: Int, preferred: Int): Int =
        when {
            preferred >= 0 || min == max -> {
                // Fixed size due to fixed size layout param or fixed constraints.
                MeasureSpec.makeMeasureSpec(preferred.coerceIn(min, max), MeasureSpec.EXACTLY)
            }
            preferred == LayoutParams.WRAP_CONTENT && max != Constraints.Infinity -> {
                // Wrap content layout param with finite max constraint. If max constraint is
                // infinite,
                // we will measure the child with UNSPECIFIED.
                MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST)
            }
            preferred == LayoutParams.MATCH_PARENT && max != Constraints.Infinity -> {
                // Match parent layout param, so we force the child to fill the available space.
                MeasureSpec.makeMeasureSpec(max, MeasureSpec.EXACTLY)
            }
            else -> {
                // max constraint is infinite and layout param is WRAP_CONTENT or MATCH_PARENT.
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            }
        }

    // TODO: b/203141462 - consume whether the AndroidView() is inside a scrollable container, and
    //  use that to set this. In the meantime set true as the defensive default.
    override fun shouldDelayChildPressedState(): Boolean = true

    // NestedScrollingParent3
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0 ||
            (axes and ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0
    }

    override fun getNestedScrollAxes(): Int {
        return nestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollingParentHelper.onStopNestedScroll(target, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray,
    ) {
        if (!isNestedScrollingEnabled) return
        val consumedByParent =
            dispatcher.dispatchPostScroll(
                consumed = Offset(dxConsumed.toComposeOffset(), dyConsumed.toComposeOffset()),
                available = Offset(dxUnconsumed.toComposeOffset(), dyUnconsumed.toComposeOffset()),
                source = toNestedScrollSource(type),
            )
        consumed[0] = composeToViewOffset(consumedByParent.x)
        consumed[1] = composeToViewOffset(consumedByParent.y)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
    ) {
        if (!isNestedScrollingEnabled) return
        dispatcher.dispatchPostScroll(
            consumed = Offset(dxConsumed.toComposeOffset(), dyConsumed.toComposeOffset()),
            available = Offset(dxUnconsumed.toComposeOffset(), dyUnconsumed.toComposeOffset()),
            source = toNestedScrollSource(type),
        )
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!isNestedScrollingEnabled) return
        val consumedByParent =
            dispatcher.dispatchPreScroll(
                available = Offset(dx.toComposeOffset(), dy.toComposeOffset()),
                source = toNestedScrollSource(type),
            )
        consumed[0] = composeToViewOffset(consumedByParent.x)
        consumed[1] = composeToViewOffset(consumedByParent.y)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean,
    ): Boolean {
        if (!isNestedScrollingEnabled) return false
        val viewVelocity = Velocity(velocityX.toComposeVelocity(), velocityY.toComposeVelocity())
        dispatcher.coroutineScope.launch {
            if (!consumed) {
                dispatcher.dispatchPostFling(consumed = Velocity.Zero, available = viewVelocity)
            } else {
                dispatcher.dispatchPostFling(consumed = viewVelocity, available = Velocity.Zero)
            }
        }
        return false
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        if (!isNestedScrollingEnabled) return false
        val toBeConsumed = Velocity(velocityX.toComposeVelocity(), velocityY.toComposeVelocity())
        dispatcher.coroutineScope.launch { dispatcher.dispatchPreFling(toBeConsumed) }
        return false
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return view.isNestedScrollingEnabled
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Cache a copy of the last known insets
        this.insets = WindowInsetsCompat(insets)
        return insetToLayoutPosition(insets)
    }

    private fun insetToLayoutPosition(insets: WindowInsetsCompat): WindowInsetsCompat {
        if (!insets.hasInsets()) {
            return insets
        }
        return insetValue(insets) { l, t, r, b -> insets.inset(l, t, r, b) }
    }

    private fun insetBounds(bounds: BoundsCompat): BoundsCompat =
        insetValue(bounds) { l, t, r, b ->
            BoundsCompat(bounds.lowerBound.inset(l, t, r, b), bounds.upperBound.inset(l, t, r, b))
        }

    private inline fun <T> insetValue(value: T, block: (l: Int, t: Int, r: Int, b: Int) -> T): T {
        val coordinates = layoutNode.innerCoordinator
        if (!coordinates.isAttached) {
            return value
        }
        val topLeft = coordinates.positionInRoot().round()
        val left = topLeft.x.fastCoerceAtLeast(0)
        val top = topLeft.y.fastCoerceAtLeast(0)
        val (rootWidth, rootHeight) = coordinates.findRootCoordinates().size
        val (width, height) = coordinates.size
        val bottomRight = coordinates.localToRoot(Offset(width.toFloat(), height.toFloat())).round()
        val right = (rootWidth - bottomRight.x).fastCoerceAtLeast(0)
        val bottom = (rootHeight - bottomRight.y).fastCoerceAtLeast(0)

        return if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            value
        } else {
            block(left, top, right, bottom)
        }
    }

    private fun Insets.inset(left: Int, top: Int, right: Int, bottom: Int): Insets {
        return Insets.of(
            (this.left - left).fastCoerceAtLeast(0),
            (this.top - top).fastCoerceAtLeast(0),
            (this.right - right).fastCoerceAtLeast(0),
            (this.bottom - bottom).fastCoerceAtLeast(0),
        )
    }

    companion object {
        private val OnCommitAffectingUpdate: (AndroidViewHolder) -> Unit = {
            it.handler.post(it.runUpdate)
        }
    }
}

private fun View.layoutAccordingTo(layoutNode: LayoutNode) {
    val position = layoutNode.coordinates.positionInRoot()
    val x = position.x.fastRoundToInt()
    val y = position.y.fastRoundToInt()
    layout(x, y, x + measuredWidth, y + measuredHeight)
}

private const val Unmeasured = Int.MIN_VALUE

/**
 * No-op Connection required by nested scroll modifier. This is No-op because we don't want to
 * influence nested scrolling with it and it is required by [Modifier.nestedScroll].
 */
private val NoOpScrollConnection = object : NestedScrollConnection {}

private fun Int.toComposeOffset() = toFloat() * -1

private fun Float.toComposeVelocity(): Float = this * -1f

private fun toNestedScrollSource(type: Int): NestedScrollSource =
    when (type) {
        ViewCompat.TYPE_TOUCH -> NestedScrollSource.UserInput
        else -> NestedScrollSource.SideEffect
    }

private typealias BringIntoViewRequester = (Rect?) -> Unit

private typealias OnRequesterReady = (BringIntoViewRequester?) -> Unit

private class BringIntoViewElement(val onRequesterReady: OnRequesterReady) :
    ModifierNodeElement<BringIntoViewNode>() {
    override fun create(): BringIntoViewNode {
        return BringIntoViewNode(onRequesterReady)
    }

    override fun update(node: BringIntoViewNode) {
        node.update(onRequesterReady)
    }

    override fun hashCode(): Int {
        return onRequesterReady.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) ||
            (other is BringIntoViewElement) && (onRequesterReady === other.onRequesterReady)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "requestRectangleBringIntoViewBridge"
    }
}

private class BringIntoViewNode(var onRequesterReady: OnRequesterReady) : Modifier.Node() {
    val requester: BringIntoViewRequester = { rect ->
        if (isAttached) {
            coroutineScope.launch { bringIntoView { rect } }
        }
    }

    override fun onAttach() {
        onRequesterReady(requester)
    }

    override fun onDetach() {
        onRequesterReady.invoke(null)
    }

    fun update(onRequesterReady: OnRequesterReady) {
        this.onRequesterReady = onRequesterReady
        if (isAttached) {
            onRequesterReady(requester)
        }
    }
}
