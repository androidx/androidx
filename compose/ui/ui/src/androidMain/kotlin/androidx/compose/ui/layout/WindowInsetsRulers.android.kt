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
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableScatterMap
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
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
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat

internal actual fun findDisplayCutouts(placementScope: Placeable.PlacementScope): List<RectRulers> {
    var node = placementScope.coordinates?.findRootCoordinates() as? NodeCoordinator
    while (node != null) {
        node.visitNodes(Nodes.Traversable) { traversableNode ->
            if (traversableNode.traverseKey === RulerKey) {
                return (traversableNode as WindowInsetsRulerProvider)
                    .insetsListener
                    .displayCutoutRulers
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
                return (traversableNode as WindowInsetsRulerProvider)
                    .insetsListener
                    .findWindowInsetsAnimation(windowInsetsRulers) ?: NoWindowInsetsAnimation
            }
        }
        node = node.wrapped
    }
    return NoWindowInsetsAnimation // nothing set
}

internal const val RulerKey = "androidx.compose.ui.layout.WindowInsetsRulers"

internal interface WindowInsetsRulerProvider {
    val insetsListener: InsetsListener
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
     * A mapping of [RectRulers] to the actual values [WindowInsetsAnimationValues] that back them.
     * Each [AndroidComposeView] will have different values.
     */
    private var insetsValues: MutableScatterMap<WindowInsetsRulers, WindowInsetsAnimationValues>? =
        null

    private val _displayCutoutRulers = mutableObjectListOf<RectRulers>()
    val displayCutoutRulers: List<RectRulers>
        @SuppressLint("AsCollectionCall")
        get() {
            val displayCutout = currentInsets?.displayCutout
            if (displayCutout == null) {
                _displayCutoutRulers.clear()
            } else {
                val boundingRects = displayCutout.boundingRects
                if (_displayCutoutRulers.size > boundingRects.size) {
                    _displayCutoutRulers.removeRange(boundingRects.size, _displayCutoutRulers.size)
                } else if (_displayCutoutRulers.size < boundingRects.size) {
                    for (i in
                        _displayCutoutRulers.size until
                            maxOf(AllDisplayCutoutRulers.size, boundingRects.size)) {
                        _displayCutoutRulers += AllDisplayCutoutRulers[i]
                    }
                }
            }
            return _displayCutoutRulers.asList()
        }

    private var currentInsets by mutableStateOf<WindowInsetsCompat?>(null)

    private var waterfallAnimation: WindowInsetsAnimation? = null

    fun findWindowInsetsAnimation(windowInsetsRulers: WindowInsetsRulers): WindowInsetsAnimation? {
        if (windowInsetsRulers === Waterfall) {
            return waterfallAnimation ?: WaterfallAnimation().also { waterfallAnimation = it }
        }
        return findWindowInsetsAnimationValue(windowInsetsRulers)
    }

    fun findWindowInsetsAnimationValue(
        windowInsetsRulers: WindowInsetsRulers
    ): WindowInsetsAnimationValues? {
        val insetsValues =
            insetsValues
                ?: MutableScatterMap<WindowInsetsRulers, WindowInsetsAnimationValues>(10).also {
                    insetsValues = it
                    WindowInsetsTypeMap.forEach { type, windowInsetsRulers ->
                        it[windowInsetsRulers] = WindowInsetsAnimationValues(type)
                    }
                }
        return insetsValues[windowInsetsRulers]
    }

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
                val insetsValue = findWindowInsetsAnimationValue(rulers)!!
                val target = insets.getInsets(type)
                val current = currentInsets?.getInsets(type)
                if (target != current) {
                    // It is really animating. The target is different from the current value
                    insetsValue.sourceValueInsets = current
                    insetsValue.targetValueInsets = target
                    insetsValue.windowInsetsAnimationValues = animation
                    Snapshot.sendApplyNotifications()
                }
            }
        }

        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        runningAnimations.fastForEach { animation ->
            val typeMask = animation.typeMask
            val rulers = WindowInsetsTypeMap[typeMask]
            if (rulers != null) {
                val insetsValue = findWindowInsetsAnimationValue(rulers)!!
                if (insetsValue.isAnimating) {
                    // It is really animating. It could be animating to the same value, so there
                    // is no need to pretend that it is animating.
                    insetsValue.windowInsetsAnimationValues = animation
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
            val insetsValue = findWindowInsetsAnimationValue(rulers)!!
            insetsValue.sourceValueInsets = null
            insetsValue.targetValueInsets = null
            insetsValue.windowInsetsAnimationValues = null
            Snapshot.sendApplyNotifications()
        }
        super.onEnd(animation)
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
        if (currentInsets == null) {
            // if we're setting insets with no values, we treat this as not setting any insets
            var hasValue = false
            WindowInsetsTypeMap.forEachKey { type ->
                if (type == WindowInsetsCompat.Type.ime()) {
                    if (insets.getInsets(type) != Insets.NONE) {
                        hasValue = true
                        return@forEachKey
                    }
                } else if (insets.getInsetsIgnoringVisibility(type) != Insets.NONE) {
                    hasValue = true
                    return@forEachKey
                }
            }
            if (!hasValue) {
                return
            }
        }
        currentInsets = insets
        Snapshot.sendApplyNotifications()
    }

    /**
     * Provides the value for [ruler], if possible, along with all other Rulers in the same
     * [RectRulers].
     */
    fun provideInset(rulerScope: RulerScope, ruler: Ruler) {
        findWindowInsetsRuler(ruler) { windowInsetsRulers, rectRulers, whichRectRulers, type ->
            if (windowInsetsRulers == null) {
                // Display cutout bounds rulers
                val currentInsets = currentInsets ?: return
                val cutout = currentInsets.displayCutout ?: return
                val boundingRects = cutout.boundingRects
                val rect = boundingRects[whichRectRulers]
                with(rulerScope) {
                    rectRulers.left provides rect.left.toFloat()
                    rectRulers.top provides rect.top.toFloat()
                    rectRulers.right provides rect.right.toFloat()
                    rectRulers.bottom provides rect.bottom.toFloat()
                }
            } else if (windowInsetsRulers === Waterfall) {
                // Need special handling for Waterfall rulers because they don't use getInsets()
                val currentInsets = currentInsets ?: return
                val waterfall = currentInsets.displayCutout?.waterfallInsets ?: Insets.NONE
                rulerScope.provideInsetsValue(rectRulers, waterfall)
            } else {
                val insets =
                    when (whichRectRulers) {
                        // 0 is current value
                        0 -> currentInsets?.getInsets(type)
                        // 1 is maximum value
                        1 ->
                            if (windowInsetsRulers === Ime) {
                                null
                            } else {
                                currentInsets?.getInsetsIgnoringVisibility(type)
                            }
                        // 2 == animation source
                        // 3 == animation target
                        else -> {
                            val animation = insetsValues?.get(windowInsetsRulers)
                            if (animation?.isAnimating != true) {
                                null
                            } else if (whichRectRulers == 2) {
                                animation.sourceValueInsets
                            } else {
                                animation.targetValueInsets
                            }
                        }
                    }
                if (insets != null) {
                    rulerScope.provideInsetsValue(rectRulers, insets)
                }
            }
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

    inner class WindowInsetsAnimationValues(val type: Int) : PlatformWindowInsetsAnimation {
        var windowInsetsAnimationValues by mutableStateOf<WindowInsetsAnimationCompat?>(null)
        override val source: RectRulers
            get() = WindowInsetsAnimationSources[type]!!

        override val target: RectRulers
            get() = WindowInsetsAnimationTargets[type]!!

        override val isVisible: Boolean
            get() = currentInsets?.isVisible(type) ?: false

        override val isAnimating: Boolean
            get() = windowInsetsAnimationValues != null

        override val fraction: Float
            get() = windowInsetsAnimationValues?.interpolatedFraction ?: 0f

        override val durationMillis: Long
            get() = windowInsetsAnimationValues?.durationMillis ?: 0L

        override val alpha: Float
            get() = windowInsetsAnimationValues?.alpha ?: 1f

        /** The starting insets value of the animation when [isAnimating] is `true`. */
        var sourceValueInsets by mutableStateOf<Insets?>(null)

        /** The ending insets value of the animation when [isAnimating] is `true`. */
        var targetValueInsets by mutableStateOf<Insets?>(null)
    }

    inner class WaterfallAnimation : PlatformWindowInsetsAnimation {
        override val source: RectRulers
            get() = NeverProvidedRectRulers

        override val target: RectRulers
            get() = NeverProvidedRectRulers

        override val isVisible: Boolean
            get() = currentInsets?.displayCutout?.waterfallInsets?.equals(Insets.NONE) == false

        override val isAnimating: Boolean
            get() = false

        override val fraction: Float
            get() = 0f

        override val durationMillis: Long
            get() = 0L

        override val alpha: Float
            get() = 1f
    }
}

// We define all display cutout rulers in advance. It is unlikely that there will be more than
// 4 holes in the screen.
private val AllDisplayCutoutRulers = Array(4) { RectRulers("display cutout rect ${it + 1}") }

private val WindowInsetsAnimationSources: IntObjectMap<RectRulers> =
    MutableIntObjectMap<RectRulers>(8).also { map ->
        map[WindowInsetsCompat.Type.statusBars()] = RectRulers("status bars source")
        map[WindowInsetsCompat.Type.navigationBars()] = RectRulers("navigation bars source")
        map[WindowInsetsCompat.Type.captionBar()] = RectRulers("caption bar source")
        map[WindowInsetsCompat.Type.ime()] = RectRulers("IME source")
        map[WindowInsetsCompat.Type.systemGestures()] = RectRulers("system gestures source")
        map[WindowInsetsCompat.Type.mandatorySystemGestures()] =
            RectRulers("mandatory system gestures source")
        map[WindowInsetsCompat.Type.tappableElement()] = RectRulers("tappable element source")
        map[WindowInsetsCompat.Type.displayCutout()] = RectRulers("display cutout source")
    }

private val WindowInsetsAnimationTargets: IntObjectMap<RectRulers> =
    MutableIntObjectMap<RectRulers>(8).also { map ->
        map[WindowInsetsCompat.Type.statusBars()] = RectRulers("status bars target")
        map[WindowInsetsCompat.Type.navigationBars()] = RectRulers("navigation bars target")
        map[WindowInsetsCompat.Type.captionBar()] = RectRulers("caption bar target")
        map[WindowInsetsCompat.Type.ime()] = RectRulers("IME target")
        map[WindowInsetsCompat.Type.systemGestures()] = RectRulers("system gestures target")
        map[WindowInsetsCompat.Type.mandatorySystemGestures()] =
            RectRulers("mandatory system gestures target")
        map[WindowInsetsCompat.Type.tappableElement()] = RectRulers("tappable element target")
        map[WindowInsetsCompat.Type.displayCutout()] = RectRulers("display cutout target")
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

internal val AllWindowInsetsRulersLookup: (Ruler) -> Boolean = { ruler ->
    var found = false
    findWindowInsetsRuler(ruler) { _, _, _, _ -> found = true }
    found
}

/** Provide all Ruler values for [insets] */
private fun RulerScope.provideInsetsValue(rectRulers: RectRulers, insets: Insets) {
    val size = coordinates.size
    rectRulers.left provides insets.left.toFloat()
    rectRulers.top provides insets.top.toFloat()
    rectRulers.right provides (size.width - insets.right).toFloat()
    rectRulers.bottom provides (size.height - insets.bottom).toFloat()
}

/**
 * If this Ruler is the left, top, right, or bottom Ruler in [rectRuler], then `true` will be
 * returned. Otherwise, `false` is returned.
 */
private fun Ruler.isIn(rectRuler: RectRulers): Boolean =
    this === rectRuler.left ||
        this === rectRuler.top ||
        this === rectRuler.right ||
        this === rectRuler.bottom

private inline fun checkWindowInsetsRuler(
    ruler: Ruler,
    windowInsetsRulers: WindowInsetsRulers,
    type: Int,
    block: (WindowInsetsRulers?, rectRulers: RectRulers, whichRectRulers: Int, type: Int) -> Unit,
): Boolean {
    var found = true
    if (ruler.isIn(windowInsetsRulers.current)) {
        block(windowInsetsRulers, windowInsetsRulers.current, 0, type)
    } else if (ruler.isIn(windowInsetsRulers.maximum)) {
        block(windowInsetsRulers, windowInsetsRulers.maximum, 1, type)
    } else if (type == -1) {
        // Waterfall never animates
        found = false
    } else {
        val source = WindowInsetsAnimationSources[type] ?: return false
        if (ruler.isIn(source)) {
            block(windowInsetsRulers, source, 2, type)
        } else {
            val target = WindowInsetsAnimationTargets[type] ?: return false
            if (ruler.isIn(target)) {
                block(windowInsetsRulers, target, 3, type)
            } else {
                found = false
            }
        }
    }
    return found
}

/**
 * Finds which WindowInsetsRulers that [ruler] is part of and passes it to [block]. The parameters
 * to [block] are the [WindowInsetsRulers] that the Ruler is part of (if any), which ruler it is (0
 * = current, 1 = maximum, 2 = source animation, 3 = target animation), the position in the
 * RectRulers (0 = left, 1 = top, 2 = right, 3 = bottom), and the type of the windowInsetsRulers. If
 * [ruler] is part of the display cutout bounds, `windowInsetsRulers` is `null`, `whichRectRulers`
 * is the index of the displayCutoutBounds, and `type` is `null`.
 */
private inline fun findWindowInsetsRuler(
    ruler: Ruler,
    block:
        (
            windowInsetsRulers: WindowInsetsRulers?,
            rectRulers: RectRulers,
            whichRectRulers: Int,
            type: Int,
        ) -> Unit,
) {
    // This is a linear lookup rather than a hashtable lookup and is slower.
    // The creation of a hashtable is a relatively expensive startup cost, so I've eliminated it.
    // WindowInsetsRulers aren't used often yet, so this is the better performance trade-off.
    WindowInsetsTypeMap.forEach { type, windowInsetsRulers ->
        if (checkWindowInsetsRuler(ruler, windowInsetsRulers, type, block)) {
            return
        }
    }
    if (checkWindowInsetsRuler(ruler, Waterfall, -1, block)) {
        return
    }
    AllDisplayCutoutRulers.forEachIndexed { index, boundsRectRulers ->
        if (ruler.isIn(boundsRectRulers)) {
            block(null, boundsRectRulers, index, -1)
            return
        }
    }
}
