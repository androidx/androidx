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
                val provider = traversableNode as WindowInsetsRulerProvider
                return provider.insetsProvider?.displayCutoutBoundsRulers ?: emptyList()
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
                val provider = traversableNode as WindowInsetsRulerProvider
                return provider.insetsProvider?.findWindowInsetsAnimation(windowInsetsRulers)
                    ?: NoWindowInsetsAnimation
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
                            minOf(cutoutRulers.size, boundingRects.size)) {
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
        val packed = RulerMap[ruler]
        if (packed < 0) return
        val index = unpackIndex(packed)
        val whichRectRulers = unpackWhichRectRulers(packed)
        when (index) {
            CutoutBoundsIndex -> {
                val currentInsets = currentInsets ?: return
                val cutout = currentInsets.displayCutout ?: return
                val boundingRects = cutout.boundingRects
                if (whichRectRulers >= boundingRects.size) return
                val rect = boundingRects[whichRectRulers]
                val rectRulers = AllDisplayCutoutBoundsRectRulers[whichRectRulers]
                with(rulerScope) {
                    rectRulers.left provides rect.left.toFloat()
                    rectRulers.top provides rect.top.toFloat()
                    rectRulers.right provides rect.right.toFloat()
                    rectRulers.bottom provides rect.bottom.toFloat()
                }
            }
            WaterfallIndex -> {
                // Need special handling for Waterfall rulers because they don't use getInsets()
                val currentInsets = currentInsets ?: return
                val waterfall = currentInsets.displayCutout?.waterfallInsets ?: Insets.NONE
                val rectRulers =
                    if (whichRectRulers == CurrentRulers) Waterfall.current else Waterfall.maximum
                rulerScope.provideInsetsValue(rectRulers, waterfall)
            }
            else -> {
                val type = WindowInsetsTypes[index]
                val rectRulers: RectRulers
                val insets: Insets?
                when (whichRectRulers) {
                    CurrentRulers -> {
                        rectRulers = WindowInsetsRulersByIndex[index].current
                        insets = currentInsets?.getInsets(type)
                    }
                    MaximumRulers -> {
                        rectRulers = WindowInsetsRulersByIndex[index].maximum
                        insets = currentInsets?.getInsetsIgnoringVisibility(type)
                    }
                    SourceRulers -> {
                        rectRulers = AnimationSourceRulers[index]
                        insets = insetsWatcher.findAnimationPositions(type).value?.source
                    }
                    else -> {
                        rectRulers = AnimationTargetRulers[index]
                        insets = insetsWatcher.findAnimationPositions(type).value?.target
                    }
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
        val typeIndex = indexOf(windowInsetsRulers)
        if (typeIndex == -1) {
            return null
        }
        val type = WindowInsetsTypes[typeIndex]
        return windowInsetsAnimationValues.getOrPut(type) {
            WindowInsetsAnimationValues(typeIndex, type, insetsWatcher.findAnimation(type))
        }
    }

    private fun indexOf(windowInsetsRulers: WindowInsetsRulers): Int =
        when (windowInsetsRulers) {
            StatusBars -> 0
            NavigationBars -> 1
            CaptionBar -> 2
            Ime -> 3
            SystemGestures -> 4
            MandatorySystemGestures -> 5
            TappableElement -> 6
            DisplayCutout -> 7
            else -> -1
        }

    fun isRulerProvided(ruler: Ruler): Boolean = ruler in RulerMap

    /** Provide all Ruler values for [insets] */
    private fun RulerScope.provideInsetsValue(rectRulers: RectRulers, insets: Insets) {
        val size = coordinates.size
        rectRulers.left provides insets.left.toFloat()
        rectRulers.top provides insets.top.toFloat()
        rectRulers.right provides (size.width - insets.right).toFloat()
        rectRulers.bottom provides (size.height - insets.bottom).toFloat()
    }

    inner class WaterfallAnimation : PlatformWindowInsetsAnimation {
        override val source: RectRulers
            get() = NeverProvidedRectRulers

        override val target: RectRulers
            get() = NeverProvidedRectRulers

        override val isVisible: Boolean
            get() = currentInsets?.displayCutout?.let { it.waterfallInsets != Insets.NONE } == true

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
        val typeIndex: Int,
        val type: Int,
        val animation: State<WindowInsetsAnimationCompat?>,
    ) : PlatformWindowInsetsAnimation {
        override val source: RectRulers
            get() = AnimationSourceRulers[typeIndex]

        override val target: RectRulers
            get() = AnimationTargetRulers[typeIndex]

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
        /**
         * Sentinel returned by [RulerMap.get] and [RulerMap.findSlot] when a [Ruler] is not
         * supported by [WindowInsetsRulersProvider]. All valid packed entries and slot indices are
         * non-negative.
         */
        private const val NotFound = -1

        /**
         * Synthetic inset indices for rulers not backed by a single [WindowInsetsCompat.Type] entry
         * in [WindowInsetsTypes] (which uses indices `0..7`). Must fit in the low 4 bits (`0..15`)
         * of [packRulerInfo].
         */
        private const val WaterfallIndex = 8
        private const val CutoutBoundsIndex = 9

        /**
         * Identifies which [RectRulers] group a [Ruler] belongs to for standard and waterfall
         * insets. For [CutoutBoundsIndex], the `whichRectRulers` field instead stores the index
         * (`0..3`) into [AllDisplayCutoutBoundsRectRulers].
         */
        private const val CurrentRulers = 0
        private const val MaximumRulers = 1
        private const val SourceRulers = 2
        private const val TargetRulers = 3

        /**
         * Packs an inset `index` (`0..9`, stored in bits `0..3`) and `whichRectRulers` (`0..3`,
         * stored in bits `4..5`) into a single non-negative `Int` so [RulerMap] can store metadata
         * in a primitive [IntArray] without allocating wrapper objects.
         */
        private inline fun packRulerInfo(index: Int, whichRectRulers: Int): Int =
            index or (whichRectRulers shl 4)

        private inline fun unpackIndex(packed: Int): Int = packed and 0xF

        private inline fun unpackWhichRectRulers(packed: Int): Int = packed ushr 4

        /**
         * Pre-allocated [RectRulers] for up to 4 display cutout bounding rects (left, top, right,
         * bottom edges of the display).
         */
        @JvmField internal val AllDisplayCutoutBoundsRectRulers = Array(4) { RectRulers() }

        /**
         * Parallel 8-element lookup tables indexed by inset type index (`0..7`):
         * - [WindowInsetsTypes]: corresponding [WindowInsetsCompat.Type] bitmask.
         * - [WindowInsetsRulersByIndex]: public [WindowInsetsRulers] (provides `current` and
         *   `maximum` [RectRulers]).
         * - [AnimationSourceRulers]: animation `source` [RectRulers] for each inset type.
         * - [AnimationTargetRulers]: animation `target` [RectRulers] for each inset type.
         *
         * All four arrays must remain in identical inset-type order (matching [indexOf]).
         */
        @JvmField
        internal val WindowInsetsTypes: IntArray =
            intArrayOf(
                WindowInsetsCompat.Type.statusBars(),
                WindowInsetsCompat.Type.navigationBars(),
                WindowInsetsCompat.Type.captionBar(),
                WindowInsetsCompat.Type.ime(),
                WindowInsetsCompat.Type.systemGestures(),
                WindowInsetsCompat.Type.mandatorySystemGestures(),
                WindowInsetsCompat.Type.tappableElement(),
                WindowInsetsCompat.Type.displayCutout(),
            )

        @JvmField
        internal val WindowInsetsRulersByIndex: Array<WindowInsetsRulers> =
            arrayOf(
                StatusBars,
                NavigationBars,
                CaptionBar,
                Ime,
                SystemGestures,
                MandatorySystemGestures,
                TappableElement,
                DisplayCutout,
            )

        @JvmField
        internal val AnimationSourceRulers: Array<RectRulers> =
            arrayOf(
                RectRulers("status bars source"),
                RectRulers("navigation bars source"),
                RectRulers("caption bar source"),
                RectRulers("IME source"),
                RectRulers("system gestures source"),
                RectRulers("mandatory system gestures source"),
                RectRulers("tappable element source"),
                RectRulers("display cutout source"),
            )

        @JvmField
        internal val AnimationTargetRulers: Array<RectRulers> =
            arrayOf(
                RectRulers("status bars target"),
                RectRulers("navigation bars target"),
                RectRulers("caption bar target"),
                RectRulers("IME target"),
                RectRulers("system gestures target"),
                RectRulers("mandatory system gestures target"),
                RectRulers("tappable element target"),
                RectRulers("display cutout target"),
            )
    }

    /**
     * Minimal immutable open-addressed identity hash table mapping [Ruler] instances to packed
     * integer metadata ([packRulerInfo]).
     *
     * ### Storage Layout (`keys` and `values`)
     * Entries are stored across two parallel arrays of length [RulerMapCapacity], indexed by the
     * same hash-table slot:
     * - [keys] (`Array<Ruler?>`): holds the singleton [Ruler] at `slot`, or `null` when the slot is
     *   unoccupied. Because entries are never removed, `null` slots act directly as probe
     *   terminators without needing tombstones or a separate metadata bitset.
     * - [values] (`IntArray`): holds the packed `(index, whichRectRulers)` integer for the [Ruler]
     *   at the same `slot` in [keys], avoiding per-entry boxed wrapper allocations.
     *
     * Initialized lazily by JVM/ART class loading the first time [RulerMap] is referenced, so apps
     * that never query a [Ruler] do not allocate these arrays and concurrent callers cannot race to
     * instantiate multiple maps.
     */
    internal object RulerMap {
        // Declared before `keys` so `values` is allocated before `createKeys(values)` populates it.
        private val values = IntArray(RulerMapCapacity)
        private val keys = createKeys(values)

        /**
         * Allocates and populates the [Ruler] key table alongside [values] (of length
         * [RulerMapCapacity]). Allocating `keys` as a local variable lets ART prove its exact
         * runtime array type and length, eliminating per-insert `aput-object` covariance and bounds
         * checks. Exposed as `internal` for benchmarking initialization cost.
         */
        internal fun createKeys(values: IntArray): Array<Ruler?> {
            val keys = arrayOfNulls<Ruler>(RulerMapCapacity)
            if (values.size < RulerMapCapacity) return keys
            putWindowInsetsTypeRulers(keys, values)
            putWaterfallRulers(keys, values)
            putDisplayCutoutBoundsRulers(keys, values)
            return keys
        }

        /**
         * Returns the packed metadata ([packRulerInfo]) for [ruler], or [NotFound] (`-1`) if
         * [ruler] is not provided by [WindowInsetsRulersProvider].
         */
        operator fun get(ruler: Ruler): Int {
            val slot = findSlot(ruler)
            return if (slot >= 0) values[slot] else NotFound
        }

        /** Returns `true` if [ruler] is one of the singleton rulers in [RulerMap]. */
        operator fun contains(ruler: Ruler): Boolean = findSlot(ruler) >= 0

        /**
         * Linearly probes [keys] starting at [initialSlot] until either:
         * - `key === ruler` (hit: returns `slot`), or
         * - `keys[slot] == null` (miss: returns [NotFound]; guaranteed to terminate because
         *   [RulerMapCapacity] is strictly greater than the number of inserted rulers).
         */
        private inline fun findSlot(ruler: Ruler): Int {
            val keys = keys
            var slot = initialSlot(ruler)
            while (true) {
                val key = keys[slot] ?: return NotFound
                if (key === ruler) return slot
                slot = nextSlot(slot)
            }
        }

        /**
         * Computes the starting slot in `0 until RulerMapCapacity` using Fibonacci multiplicative
         * hashing on [Ruler]'s identity hash code:
         * 1. `ruler.hashCode()` returns `System.identityHashCode(ruler)` ([Ruler] does not override
         *    `hashCode`). Raw identity hash codes can have uneven low-bit distribution.
         * 2. Multiplying by [FibonacciHashMultiplier] mixes low-bit entropy into the upper bits and
         *    spreads clustered hash codes evenly across the 32-bit unsigned range.
         * 3. Unsigned right-shifting (`ushr`) by [RulerMapHashShift] extracts the top
         *    [RulerMapCapacityBits] bits—the best-mixed bits of the product—yielding an index in
         *    `0..255` without needing a separate mask or sign check.
         */
        private inline fun initialSlot(ruler: Ruler): Int =
            (ruler.hashCode() * FibonacciHashMultiplier) ushr RulerMapHashShift

        /**
         * Advances `slot` by 1 modulo [RulerMapCapacity] using a bitmask (`RulerMapCapacity` is a
         * power of 2).
         */
        private inline fun nextSlot(slot: Int): Int = (slot + 1) and RulerMapMask

        private inline fun putWindowInsetsTypeRulers(keys: Array<Ruler?>, values: IntArray) {
            val rulersByIndex = WindowInsetsRulersByIndex
            val sources = AnimationSourceRulers
            val targets = AnimationTargetRulers
            for (i in rulersByIndex.indices) {
                val rulers = rulersByIndex[i]
                putRectRulers(keys, values, rulers.current, packRulerInfo(i, CurrentRulers))
                // Android never provides maximum insets for IME, so omit Ime.maximum.
                if (rulers !== Ime) {
                    putRectRulers(keys, values, rulers.maximum, packRulerInfo(i, MaximumRulers))
                }
                putRectRulers(keys, values, sources[i], packRulerInfo(i, SourceRulers))
                putRectRulers(keys, values, targets[i], packRulerInfo(i, TargetRulers))
            }
        }

        private inline fun putWaterfallRulers(keys: Array<Ruler?>, values: IntArray) {
            // Waterfall insets never animate, so they have only current and maximum RectRulers.
            putRectRulers(
                keys,
                values,
                Waterfall.current,
                packRulerInfo(WaterfallIndex, CurrentRulers),
            )
            putRectRulers(
                keys,
                values,
                Waterfall.maximum,
                packRulerInfo(WaterfallIndex, MaximumRulers),
            )
        }

        private inline fun putDisplayCutoutBoundsRulers(keys: Array<Ruler?>, values: IntArray) {
            val cutoutBounds = AllDisplayCutoutBoundsRectRulers
            for (i in cutoutBounds.indices) {
                putRectRulers(keys, values, cutoutBounds[i], packRulerInfo(CutoutBoundsIndex, i))
            }
        }

        private inline fun putRectRulers(
            keys: Array<Ruler?>,
            values: IntArray,
            rectRulers: RectRulers,
            packedInfo: Int,
        ) {
            // Each singleton Ruler is distinct, so insertion only needs to scan to the first empty
            // (`null`) slot without checking for duplicate keys.
            putRuler(keys, values, rectRulers.left, packedInfo)
            putRuler(keys, values, rectRulers.top, packedInfo)
            putRuler(keys, values, rectRulers.right, packedInfo)
            putRuler(keys, values, rectRulers.bottom, packedInfo)
        }

        private inline fun putRuler(
            keys: Array<Ruler?>,
            values: IntArray,
            ruler: Ruler,
            packedInfo: Int,
        ) {
            var slot = initialSlot(ruler)
            while (keys[slot] != null) {
                slot = nextSlot(slot)
            }
            keys[slot] = ruler
            values[slot] = packedInfo
        }
    }
}

/**
 * Log2 of [RulerMapCapacity] (`2^8 = 256` slots).
 *
 * [WindowInsetsRulersProvider] currently registers **148** [Ruler]s (37 [RectRulers] * 4 edges):
 * - 7 non-IME [WindowInsetsCompat.Type]s * 4 [RectRulers] (`current`, `maximum`, `source`,
 *   `target`) = 28 [RectRulers] (112 [Ruler]s)
 * - [Ime] * 3 [RectRulers] (`current`, `source`, `target`; `Ime.maximum` is omitted) = 3
 *   [RectRulers] (12 [Ruler]s)
 * - [Waterfall] * 2 [RectRulers] (`current`, `maximum`) = 2 [RectRulers] (8 [Ruler]s)
 * - 4 [WindowInsetsRulersProvider.AllDisplayCutoutBoundsRectRulers] = 4 [RectRulers] (16 [Ruler]s)
 *
 * Open-addressed linear probing requires a power-of-2 table size for bitmask wrap-around
 * ([RulerMapMask]) and top-bit Fibonacci hash extraction ([RulerMapHashShift]), plus enough empty
 * (`null`) slots to terminate misses quickly without clustering. `256` is the smallest power of 2
 * above 148, giving a ~58% load factor (108 `null` slots).
 *
 * **IMPORTANT**: If new [WindowInsetsRulers] or display cutout bounds are added such that the total
 * number of registered [Ruler]s exceeds ~75% of [RulerMapCapacity] (~192 [Ruler]s for capacity 256,
 * and strictly before reaching 256 since at least one `null` slot is required for lookups to
 * terminate), increment [RulerMapCapacityBits] (for example, to `9` for 512 slots).
 * [RulerMapCapacity], [RulerMapMask], and [RulerMapHashShift] derive from this constant
 * automatically.
 */
private const val RulerMapCapacityBits = 8

/**
 * Total slot count (`256`) of the `keys` and `values` arrays in
 * [WindowInsetsRulersProvider.RulerMap]. Must remain a power of 2 (`1 shl RulerMapCapacityBits`).
 */
internal const val RulerMapCapacity = 1 shl RulerMapCapacityBits

/**
 * Bitmask (`0xFF` for capacity 256) used by `nextSlot` to wrap linear-probing indices modulo
 * [RulerMapCapacity] without a division/modulo instruction (`(slot + 1) and RulerMapMask`). Valid
 * only because [RulerMapCapacity] is a power of 2.
 */
private const val RulerMapMask = RulerMapCapacity - 1

/**
 * Number of bits (`32 - 8 = 24`) to unsigned-right-shift a 32-bit Fibonacci hash product so its top
 * [RulerMapCapacityBits] bits land in `0 until RulerMapCapacity` (`0..255`).
 */
private const val RulerMapHashShift = Int.SIZE_BITS - RulerMapCapacityBits

/**
 * 32-bit Fibonacci hashing multiplier (Knuth, *The Art of Computer Programming*, Vol. 3, §6.4):
 * `floor(2^32 / phi) = 2,654,435,769 = 0x9E3779B9` (where `phi = (1 + sqrt(5)) / 2` is the golden
 * ratio), which equals `-1,640,531,527` (`-0x61c88647`) as a signed 32-bit `Int`.
 *
 * Multiplying a 32-bit hash code by this odd constant (coprime to `2^32`) scrambles low-order bits
 * into the high-order bits and scatters sequential or clustered `identityHashCode` values across
 * the 32-bit space before `ushr RulerMapHashShift` selects the top [RulerMapCapacityBits] bits.
 */
private const val FibonacciHashMultiplier: Int = 0x9E3779B9.toInt()
