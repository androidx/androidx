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
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.State
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
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat

internal actual fun findDisplayCutouts(placementScope: Placeable.PlacementScope): List<RectRulers> {
    var node = placementScope.coordinates?.findRootCoordinates() as? NodeCoordinator
    while (node != null) {
        node.visitNodes(Nodes.Traversable) { traversableNode ->
            if (traversableNode.traverseKey === RulerKey) {
                return (traversableNode as WindowInsetsRulerProvider)
                    .insetsProvider
                    .displayCutoutBoundsRulers
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
                    .insetsProvider
                    .findWindowInsetsAnimation(windowInsetsRulers) ?: NoWindowInsetsAnimation
            }
        }
        node = node.wrapped
    }
    return NoWindowInsetsAnimation // nothing set
}

internal const val RulerKey = "androidx.compose.ui.layout.WindowInsetsRulers"

internal class WindowInsetsRulersProvider(val insetsWatcher: WindowInsetsWatcher) {
    val currentInsets: WindowInsetsCompat?
        get() = insetsWatcher.currentInsets

    private var _displayCutoutBoundsRulers = mutableObjectListOf<RectRulers>()
    val displayCutoutBoundsRulers: List<RectRulers>
        @SuppressLint("AsCollectionCall")
        get() {
            val displayCutout = currentInsets?.displayCutout
            if (displayCutout == null) {
                _displayCutoutBoundsRulers.clear()
            } else {
                val boundingRects = displayCutout.boundingRects
                if (_displayCutoutBoundsRulers.size > boundingRects.size) {
                    _displayCutoutBoundsRulers.removeRange(
                        boundingRects.size,
                        _displayCutoutBoundsRulers.size,
                    )
                } else if (_displayCutoutBoundsRulers.size < boundingRects.size) {
                    val cutoutRulers = AllDisplayCutoutBoundsRectRulers
                    for (i in
                        _displayCutoutBoundsRulers.size until
                            maxOf(cutoutRulers.size, boundingRects.size)) {
                        _displayCutoutBoundsRulers += cutoutRulers[i]
                    }
                }
            }
            return _displayCutoutBoundsRulers.asList()
        }

    private var waterfallAnimation: WindowInsetsAnimation? = null

    private val windowInsetsAnimationValues = mutableIntObjectMapOf<WindowInsetsAnimationValues>()

    fun findWindowInsetsAnimation(windowInsetsRulers: WindowInsetsRulers): WindowInsetsAnimation? {
        if (windowInsetsRulers === Waterfall) {
            return waterfallAnimation ?: WaterfallAnimation().also { waterfallAnimation = it }
        }
        return findWindowInsetsAnimationValue(windowInsetsRulers)
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
                        2 -> insetsWatcher.findAnimationPositions(type).value?.source
                        // 3 == animation target
                        3 -> insetsWatcher.findAnimationPositions(type).value?.target
                        else -> null
                    }
                if (insets != null) {
                    rulerScope.provideInsetsValue(rectRulers, insets)
                }
            }
        }
    }

    private fun findWindowInsetsAnimationValue(
        windowInsetsRulers: WindowInsetsRulers
    ): WindowInsetsAnimationValues? {
        val type = typeOf(windowInsetsRulers)
        if (type == -1) {
            return null
        }
        return windowInsetsAnimationValues.getOrPut(type) {
            WindowInsetsAnimationValues(type, insetsWatcher.findAnimation(type))
        }
    }

    private fun typeOf(windowInsetsRulers: WindowInsetsRulers): Int =
        when (windowInsetsRulers) {
            CaptionBar -> WindowInsetsCompat.Type.captionBar()
            DisplayCutout -> WindowInsetsCompat.Type.displayCutout()
            Ime -> WindowInsetsCompat.Type.ime()
            MandatorySystemGestures -> WindowInsetsCompat.Type.mandatorySystemGestures()
            NavigationBars -> WindowInsetsCompat.Type.navigationBars()
            StatusBars -> WindowInsetsCompat.Type.statusBars()
            SystemGestures -> WindowInsetsCompat.Type.systemGestures()
            TappableElement -> WindowInsetsCompat.Type.tappableElement()
            else -> -1
        }

    fun isRulerProvided(ruler: Ruler): Boolean {
        var found = false
        findWindowInsetsRuler(ruler) { _, _, _, _ -> found = true }
        return found
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
        block:
            (WindowInsetsRulers?, rectRulers: RectRulers, whichRectRulers: Int, type: Int) -> Unit,
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

    /** Provide all Ruler values for [insets] */
    private fun RulerScope.provideInsetsValue(rectRulers: RectRulers, insets: Insets) {
        val size = coordinates.size
        rectRulers.left provides insets.left.toFloat()
        rectRulers.top provides insets.top.toFloat()
        rectRulers.right provides (size.width - insets.right).toFloat()
        rectRulers.bottom provides (size.height - insets.bottom).toFloat()
    }

    /**
     * Finds which WindowInsetsRulers that [ruler] is part of and passes it to [block]. The
     * parameters to [block] are the [WindowInsetsRulers] that the Ruler is part of (if any), which
     * ruler it is (0 = current, 1 = maximum, 2 = source animation, 3 = target animation), the
     * position in the RectRulers (0 = left, 1 = top, 2 = right, 3 = bottom), and the type of the
     * windowInsetsRulers. If [ruler] is part of the display cutout bounds, `windowInsetsRulers` is
     * `null`, `whichRectRulers` is the index of the displayCutoutBounds, and `type` is `null`.
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
        // The creation of a hashtable is a relatively expensive startup cost, so I've eliminated
        // it.
        // WindowInsetsRulers aren't used often yet, so this is the better performance trade-off.
        WindowInsetsTypeMap.forEach { type, windowInsetsRulers ->
            if (checkWindowInsetsRuler(ruler, windowInsetsRulers, type, block)) {
                return
            }
        }
        if (checkWindowInsetsRuler(ruler, Waterfall, -1, block)) {
            return
        }
        AllDisplayCutoutBoundsRectRulers.forEachIndexed { index, boundsRectRulers ->
            if (ruler.isIn(boundsRectRulers)) {
                block(null, boundsRectRulers, index, -1)
                return
            }
        }
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

    inner class WindowInsetsAnimationValues(
        val type: Int,
        val animation: State<WindowInsetsAnimationCompat?>,
    ) : PlatformWindowInsetsAnimation {
        override val source: RectRulers
            get() = WindowInsetsAnimationSources[type]!!

        override val target: RectRulers
            get() = WindowInsetsAnimationTargets[type]!!

        override val isVisible: Boolean
            get() = currentInsets?.isVisible(type) ?: false

        override val isAnimating: Boolean
            get() = animation.value != null

        override val fraction: Float
            get() = animation.value?.interpolatedFraction ?: 0f

        override val durationMillis: Long
            get() = animation.value?.durationMillis ?: 0L

        override val alpha: Float
            get() = animation.value?.alpha ?: 1f
    }

    companion object {
        private val AllDisplayCutoutBoundsRectRulers = Array(4) { RectRulers() }

        private val WindowInsetsAnimationSources: IntObjectMap<RectRulers> =
            MutableIntObjectMap<RectRulers>(8).also { map ->
                map[WindowInsetsCompat.Type.statusBars()] = RectRulers("status bars source")
                map[WindowInsetsCompat.Type.navigationBars()] = RectRulers("navigation bars source")
                map[WindowInsetsCompat.Type.captionBar()] = RectRulers("caption bar source")
                map[WindowInsetsCompat.Type.ime()] = RectRulers("IME source")
                map[WindowInsetsCompat.Type.systemGestures()] = RectRulers("system gestures source")
                map[WindowInsetsCompat.Type.mandatorySystemGestures()] =
                    RectRulers("mandatory system gestures source")
                map[WindowInsetsCompat.Type.tappableElement()] =
                    RectRulers("tappable element source")
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
                map[WindowInsetsCompat.Type.tappableElement()] =
                    RectRulers("tappable element target")
                map[WindowInsetsCompat.Type.displayCutout()] = RectRulers("display cutout target")
            }

        /**
         * Mapping the [WindowInsetsCompat.Type] to the [RectRulers] for all single insets types.
         */
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
    }
}
