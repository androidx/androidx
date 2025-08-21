/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.layout

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectList
import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.CaptionBar
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.DisplayCutout
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Ime
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.MandatorySystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SystemGestures
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.TappableElement
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.Waterfall
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requestRemeasure
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat

internal class WindowWindowInsetsAnimationValues(name: String) : PlatformWindowInsetsAnimation {
    override var isVisible: Boolean by mutableStateOf(true)
    override var isAnimating: Boolean by mutableStateOf(false)
    override var fraction: Float by mutableFloatStateOf(0f)
    override var durationMillis: Long by mutableLongStateOf(0L)
    override var alpha: Float by mutableFloatStateOf(1f)
    override val source: RectRulers = RectRulers("$name source")
    override val target: RectRulers = RectRulers("$name target")

    /** The current Window Insets values. */
    var current = UnsetValueInsets

    /**
     * The value of the Window Insets when they are visible. [WindowInsetsRulers.Ime] never provides
     * this value.
     */
    var maximum = UnsetValueInsets

    /** The starting insets value of the animation when [isAnimating] is `true`. */
    var sourceValueInsets = UnsetValueInsets

    /** The ending insets value of the animation when [isAnimating] is `true`. */
    var targetValueInsets = UnsetValueInsets
}

internal actual fun findDisplayCutouts(placementScope: Placeable.PlacementScope): List<RectRulers> {
    var node = placementScope.coordinates?.findRootCoordinates() as? NodeCoordinator
    while (node != null) {
        node.visitNodes(Nodes.Traversable) { traversableNode ->
            if (traversableNode.traverseKey === RulerKey) {
                return (traversableNode as RulerProviderModifierNode).cutoutRulers
            }
        }
        node = node.wrapped
    }
    return emptyList() // it hasn't been set on the root node
}

internal actual fun findInsetsAnimationProperties(
    placementScope: Placeable.PlacementScope,
    windowInsetsRulers: WindowInsetsRulers,
): WindowInsetsAnimation {
    var node = placementScope.coordinates?.findRootCoordinates() as? NodeCoordinator
    while (node != null) {
        node.visitNodes(Nodes.Traversable) { traversableNode ->
            if (traversableNode.traverseKey === RulerKey) {
                return (traversableNode as RulerProviderModifierNode)
                    .insetsValues[windowInsetsRulers] ?: NoWindowInsetsAnimation
            }
        }
        node = node.wrapped
    }
    return NoWindowInsetsAnimation // nothing set
}

/** Applies the rulers for window insets. */
internal fun Modifier.applyWindowInsetsRulers(insetsListener: InsetsListener) =
    this.then(RulerProviderModifierElement(insetsListener))

/** [ModifierNodeElement] that provides all [RectRulers] except for [Ime]. */
@SuppressLint("ModifierNodeInspectableProperties")
private class RulerProviderModifierElement(val insetsListener: InsetsListener) :
    ModifierNodeElement<RulerProviderModifierNode>() {
    override fun create(): RulerProviderModifierNode = RulerProviderModifierNode(insetsListener)

    override fun hashCode(): Int = insetsListener.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return (other as? RulerProviderModifierElement)?.insetsListener === insetsListener
    }

    override fun update(node: RulerProviderModifierNode) {
        node.insetsListener = insetsListener
    }
}

private const val RulerKey = "androidx.compose.ui.layout.WindowInsetsRulers"

/**
 * [Modifier.Node] that provides all [RectRulers] except for [Ime] and other Rulers animated with
 * the IME. The private [WindowWindowInsetsAnimationValues] are provided as a [TraversableNode].
 */
private class RulerProviderModifierNode(insetsListener: InsetsListener) :
    Modifier.Node(), LayoutModifierNode, TraversableNode {
    val insetsValues: ScatterMap<Any, WindowWindowInsetsAnimationValues>
        get() = insetsListener.insetsValues

    val generation: MutableIntState
        get() = insetsListener.generation

    var previousGeneration = -1

    val cutoutRects: MutableObjectList<MutableState<Rect>>
        get() = insetsListener.displayCutouts

    val cutoutRulers: List<RectRulers>
        get() = insetsListener.displayCutoutRulers

    var insetsListener: InsetsListener = insetsListener
        set(value) {
            if (field !== value) {
                field = value
                requestRemeasure()
            }
        }

    val rulerLambda: RulerScope.() -> Unit = {
        previousGeneration = generation.intValue // just read the value so it is observed
        // When generation is 0, no updateInsets() has been called yet, so we don't need to
        // provide any insets.
        if (previousGeneration > 0) {
            val size = coordinates.size
            val insetsValues = insetsListener.insetsValues
            val (width, height) = size
            AnimatableInsetsRulers.forEach { rulers ->
                val values = insetsValues[rulers]!!
                provideInsetsValues(rulers.current, values.current, width, height)
                if (values.isAnimating) {
                    provideInsetsValues(values.source, values.sourceValueInsets, width, height)
                    provideInsetsValues(values.target, values.targetValueInsets, width, height)
                }
                provideInsetsValues(rulers.maximum, values.maximum, width, height)
            }
            if (cutoutRects.isNotEmpty()) {
                cutoutRects.forEachIndexed { index, rectState ->
                    val rulers = cutoutRulers[index]
                    val rect = rectState.value
                    rulers.left provides rect.left.toFloat()
                    rulers.top provides rect.top.toFloat()
                    rulers.right provides rect.right.toFloat()
                    rulers.bottom provides rect.bottom.toFloat()
                }
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val width = placeable.width
        val height = placeable.height
        return layout(width, height, rulers = rulerLambda) { placeable.place(0, 0) }
    }

    override val traverseKey: Any
        get() = RulerKey
}

/** Provide values for a [RectRulers]. */
private fun RulerScope.provideInsetsValues(
    rulers: RectRulers,
    insets: ValueInsets,
    width: Int,
    height: Int,
) {
    if (insets != UnsetValueInsets) {
        val left = insets.left.toFloat()
        val top = insets.top.toFloat()
        val right = (width - insets.right).toFloat()
        val bottom = (height - insets.bottom).toFloat()

        rulers.left provides left
        rulers.top provides top
        rulers.right provides right
        rulers.bottom provides bottom
    }
}

/**
 * A listener for WindowInsets changes. This updates the [insetsValues] values whenever values
 * change.
 */
internal class InsetsListener(val composeView: AndroidComposeView) :
    WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE),
    Runnable,
    OnApplyWindowInsetsListener,
    OnAttachStateChangeListener {
    /**
     * When [android.view.WindowInsetsController.controlWindowInsetsAnimation] is called, the
     * [onApplyWindowInsets] is called after [onPrepare] with the target size. We don't want to
     * report the target size, we want to always report the current size, so we must ignore those
     * calls. However, the animation may be canceled before it progresses. On R, it won't make any
     * callbacks, so we have to figure out whether the [onApplyWindowInsets] is from a canceled
     * animation or if it is from the controlled animation. When [prepared] is `true` on R, we post
     * a callback to set the [onApplyWindowInsets] insets value.
     */
    private var prepared = false

    /** `true` if there is an animation in progress. */
    private var runningAnimationMask = 0

    private var savedInsets: WindowInsetsCompat? = null

    /**
     * A mapping of [RectRulers] to the actual values [WindowWindowInsetsAnimationValues] that back
     * them. Each [AndroidComposeView] will have different values.
     */
    val insetsValues: ScatterMap<Any, WindowWindowInsetsAnimationValues> =
        MutableScatterMap<Any, WindowWindowInsetsAnimationValues>(9).also {
            it[CaptionBar] = WindowWindowInsetsAnimationValues("caption bar")
            it[DisplayCutout] = WindowWindowInsetsAnimationValues("display cutout")
            it[Ime] = WindowWindowInsetsAnimationValues("ime")
            it[MandatorySystemGestures] =
                WindowWindowInsetsAnimationValues("mandatory system gestures")
            it[NavigationBars] = WindowWindowInsetsAnimationValues("navigation bars")
            it[StatusBars] = WindowWindowInsetsAnimationValues("status bars")
            it[SystemGestures] = WindowWindowInsetsAnimationValues("system gestures")
            it[TappableElement] = WindowWindowInsetsAnimationValues("tappable element")
            it[Waterfall] = WindowWindowInsetsAnimationValues("waterfall")
        }

    val generation = mutableIntStateOf(0)

    val displayCutouts = MutableObjectList<MutableState<Rect>>(4)
    val displayCutoutRulers = mutableStateListOf<RectRulers>()

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        prepared = true
        super.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: BoundsCompat,
    ): BoundsCompat {
        val insets = savedInsets
        prepared = false
        savedInsets = null

        if (animation.durationMillis > 0L && insets != null) {
            val type = animation.typeMask
            runningAnimationMask = runningAnimationMask or type
            // This is the animation's target value
            val rulers = WindowInsetsTypeMap[type]
            if (rulers != null) {
                val insetsValue = insetsValues[rulers]!!
                val target = ValueInsets(insets.getInsets(type))
                val current = insetsValue.current
                if (target != current) {
                    // It is really animating. The target is different from the current value
                    insetsValue.sourceValueInsets = current
                    insetsValue.targetValueInsets = target
                    insetsValue.isAnimating = true
                    updateInsetAnimationInfo(insetsValue, animation)
                    generation.intValue++
                    Snapshot.sendApplyNotifications()
                }
            }
        }

        return super.onStart(animation, bounds)
    }

    private fun updateInsetAnimationInfo(
        insetsValue: WindowWindowInsetsAnimationValues,
        animation: WindowInsetsAnimationCompat,
    ) {
        insetsValue.fraction = animation.interpolatedFraction
        insetsValue.alpha = animation.alpha
        insetsValue.durationMillis = animation.durationMillis
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        runningAnimations.fastForEach { animation ->
            val typeMask = animation.typeMask
            val rulers = WindowInsetsTypeMap[typeMask]
            if (rulers != null) {
                val insetsValue = insetsValues[rulers]!!
                if (insetsValue.isAnimating) {
                    // It is really animating. It could be animating to the same value, so there
                    // is no need to pretend that it is animating.
                    updateInsetAnimationInfo(insetsValue, animation)
                }
            }
        }
        updateInsets(insets)
        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        prepared = false
        val type = animation.typeMask
        runningAnimationMask = runningAnimationMask and type.inv()
        savedInsets = null
        val rulers = WindowInsetsTypeMap[type]
        if (rulers != null) {
            val insetsValue = insetsValues[rulers]!!
            insetsValue.fraction = 0f
            insetsValue.alpha = 1f
            insetsValue.durationMillis = 0L
            insetsValue.fraction = 0f
            stopAnimationForRuler(insetsValue)
            generation.intValue++
            Snapshot.sendApplyNotifications()
        }
        super.onEnd(animation)
    }

    private fun stopAnimationForRuler(insetsValue: WindowWindowInsetsAnimationValues) {
        insetsValue.isAnimating = false
        insetsValue.sourceValueInsets = UnsetValueInsets
        insetsValue.targetValueInsets = UnsetValueInsets
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Keep track of the most recent insets we've seen, to ensure onEnd will always use the
        // most recently acquired insets
        if (prepared) {
            savedInsets = insets // save for onStart()

            // There may be no callback on R if the animation is canceled after onPrepare(),
            // so we won't know if the onPrepare() was canceled or if this is an
            // onApplyWindowInsets() after the cancellation. We'll just post the value
            // and if it is still preparing then we just use the value.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                view.post(this)
            }
        } else if (runningAnimationMask == 0) {
            // If an animation is running, rely on onProgress() to update the insets
            // On APIs less than 30 where the IME animation is backported, this avoids reporting
            // the final insets for a frame while the animation is running.
            updateInsets(insets)
        }
        return insets
    }

    private fun updateInsets(insets: WindowInsetsCompat) {
        var changed = false
        var hasInsets = false
        WindowInsetsTypeMap.forEach { type, rulers ->
            val insetsValue = ValueInsets(insets.getInsets(type))
            val values = insetsValues[rulers]!!
            if (insetsValue != values.current) {
                values.current = insetsValue
                changed = true
                if (insetsValue != ZeroValueInsets) {
                    hasInsets = true
                }
            }
            if (type != WindowInsetsCompat.Type.ime()) {
                val insetsValue = ValueInsets(insets.getInsetsIgnoringVisibility(type))
                if (values.maximum != insetsValue) {
                    values.maximum = insetsValue
                    changed = true
                    if (insetsValue != ZeroValueInsets) {
                        hasInsets = true
                    }
                }
            }
            values.isVisible = insets.isVisible(type)
        }
        val cutout = insets.displayCutout
        val waterfall =
            if (cutout == null) {
                ZeroValueInsets
            } else {
                ValueInsets(cutout.waterfallInsets)
            }
        val waterfallInsets = insetsValues[Waterfall]!!
        waterfallInsets.isVisible = waterfall != ZeroValueInsets
        if (waterfallInsets.current != waterfall) {
            waterfallInsets.current = waterfall
            waterfallInsets.maximum = waterfall
            changed = true
            if (waterfall != ZeroValueInsets) {
                hasInsets = true
            }
        }
        if (cutout == null) {
            if (displayCutouts.size > 0) {
                displayCutouts.clear()
                displayCutoutRulers.clear()
                changed = true
            }
        } else {
            val boundingRects = cutout.boundingRects
            if (boundingRects.size < displayCutouts.size) {
                displayCutouts.removeRange(boundingRects.size, displayCutouts.size)
                displayCutoutRulers.removeRange(boundingRects.size, displayCutoutRulers.size)
                changed = true
            } else {
                repeat(boundingRects.size - displayCutouts.size) {
                    displayCutouts += mutableStateOf(boundingRects[displayCutouts.size])
                    displayCutoutRulers += RectRulers("display cutout rect ${displayCutouts.size}")
                    changed = true
                }
            }

            boundingRects.fastForEachIndexed { index, rect ->
                val cutout = displayCutouts[index]
                if (cutout.value != rect) {
                    cutout.value = rect
                    changed = true
                }
            }
            if (boundingRects.isNotEmpty()) {
                hasInsets = true
            }
        }
        // Don't invalidate the rulers if there have never been insets or if there isn't a change
        if ((hasInsets || generation.intValue != 0) && changed) {
            generation.intValue++
            Snapshot.sendApplyNotifications()
        }
    }

    /**
     * On [R], we don't receive the [onEnd] call when an animation is canceled, so we post the value
     * received in [onApplyWindowInsets] immediately after [onPrepare]. If [onProgress] or [onEnd]
     * is received before the runnable executes then the value won't be used. Otherwise, the
     * [onApplyWindowInsets] value will be used. It may have a janky frame, but it is the best we
     * can do.
     */
    override fun run() {
        if (prepared) {
            runningAnimationMask = 0
            prepared = false
            savedInsets?.let {
                updateInsets(it)
                savedInsets = null
            }
        }
    }

    override fun onViewAttachedToWindow(view: View) {
        // Until merging the foundation layout implementation and this implementation, we'll
        // listen on the ComposeView containing the AndroidComposeView so that there isn't
        // a collision
        val listenerView = view.parent as? View ?: view
        ViewCompat.setOnApplyWindowInsetsListener(listenerView, this)
        ViewCompat.setWindowInsetsAnimationCallback(listenerView, this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        // Until merging the foundation layout implementation and this implementation, we'll
        // listen on the ComposeView containing the AndroidComposeView so that there isn't
        // a collision
        val listenerView = view.parent as? View ?: view
        ViewCompat.setOnApplyWindowInsetsListener(listenerView, null)
        ViewCompat.setWindowInsetsAnimationCallback(listenerView, null)
    }
}

/** Mapping the [WindowInsetsCompat.Type] to the [RectRulers] for all single insets types. */
private val WindowInsetsTypeMap: IntObjectMap<WindowInsetsRulers> =
    MutableIntObjectMap<WindowInsetsRulers>(8).also {
        it[WindowInsetsCompat.Type.statusBars()] = StatusBars
        it[WindowInsetsCompat.Type.navigationBars()] = NavigationBars
        it[WindowInsetsCompat.Type.captionBar()] = CaptionBar
        it[WindowInsetsCompat.Type.ime()] = Ime
        it[WindowInsetsCompat.Type.systemGestures()] = SystemGestures
        it[WindowInsetsCompat.Type.mandatorySystemGestures()] = MandatorySystemGestures
        it[WindowInsetsCompat.Type.tappableElement()] = TappableElement
        it[WindowInsetsCompat.Type.displayCutout()] = DisplayCutout
    }

/** Rulers that can animate, but don't always animate with the IME */
private val AnimatableInsetsRulers =
    arrayOf(
        StatusBars,
        NavigationBars,
        CaptionBar,
        TappableElement,
        SystemGestures,
        MandatorySystemGestures,
        Ime,
        Waterfall,
        DisplayCutout,
    )
