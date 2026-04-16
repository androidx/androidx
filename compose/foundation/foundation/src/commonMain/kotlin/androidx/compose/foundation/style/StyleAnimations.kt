/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.platform.SynchronizedObject
import androidx.compose.foundation.platform.makeSynchronizedObject
import androidx.compose.foundation.platform.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * This is a class that is specifically made to handle animated transitions between [Style]
 * properties on a [StyleOuterNode] which are using the StyleScope::animate API.
 *
 * These animations are declarative in the sense that the developer is just specifying the styles
 * they want in specific states.The Style system is figuring out how to animate it from the current
 * style to the desired one.
 */
@ExperimentalFoundationStyleApi
internal class StyleAnimations {
    private val lock: SynchronizedObject = makeSynchronizedObject()

    enum class EntryState {
        Untouched,
        Unchanged,
        Changed,
        Inserted,
        Removing,
    }

    private inner class Entry(
        var toSpec: AnimationSpec<Float>,
        var fromSpec: AnimationSpec<Float>,
    ) {
        val animation = Animatable(0f)
        var state = EntryState.Inserted
        var animatingIn: Boolean = true
        var job: Job? = null

        fun animateIn(coroutineScope: CoroutineScope) {
            animatingIn = true
            job?.cancel()
            job = coroutineScope.launch { animation.animateTo(1f, animationSpec = toSpec) }
        }

        fun animateOut(coroutineScope: CoroutineScope) {
            animatingIn = false
            job?.cancel()
            job =
                coroutineScope.launch {
                    try {
                        animation.animateTo(0f, animationSpec = fromSpec)
                    } finally {
                        // When we finish animation, we call cleanup, which is what finally removes
                        // this entry from the list of entries
                        cleanupAnimations()
                    }
                }
        }

        fun interrupted(coroutineScope: CoroutineScope) {
            if (animatingIn) animateIn(coroutineScope) else animateOut(coroutineScope)
        }

        fun close() {
            job?.cancel()
            job = null
        }
    }

    private var entries: MutableIntObjectMap<Entry> = mutableIntObjectMapOf()

    fun phaseFlags(): Int {
        var primitivesSet: Long = 0
        var objectsSet: Int = 0
        entries.forEach { it, _ ->
            if (it < PrimitivePropertyCount) primitivesSet = primitivesSet.withId(it.toByte())
            else objectsSet = objectsSet.withId(it)
        }
        return objectPhaseFlagsOf(objectsSet) or primitivePhaseFlagsOf(primitivesSet)
    }

    fun isEmpty() = synchronized(lock) { entries.isEmpty() }

    @Suppress("NOTHING_TO_INLINE") inline fun timeOf(propertyId: Byte) = timeOf(propertyId.toInt())

    fun timeOf(propertyId: Int): Float {
        synchronized(lock) {
            val entry = this.entries[propertyId] ?: return 0f
            return entry.animation.value
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun recordAnimations(
        base: StyleProperties?,
        target: StyleProperties?,
        fromSpecs: IntObjectMap<AnimationSpec<Float>>?,
        toSpecs: IntObjectMap<AnimationSpec<Float>>?,
        node: StyleOuterNode,
    ) {
        synchronized(lock) {
            preRecordLocked()
            if (base != null && target != null) {
                // The color and brush properties form one logical property so if either base
                // or target sets a brush the brush version must be animated. This fixes the
                // bit sets to give priority to the brush over a color
                val objectsSet = target.objectsSet or (base.objectsSet and BrushPropertiesMask)
                val primitivesSet = removeColorForBrushProperties(target.primitivesSet, objectsSet)

                // For all the target properties, record an animation
                forEachSetPropertyId(primitivesSet, objectsSet) { id ->
                    val fromSpec = fromSpecs?.get(id) ?: DefaultSpringSpec
                    val toSpec = toSpecs?.get(id) ?: DefaultSpringSpec
                    record(id, fromSpec, toSpec)
                }
            }
            postRecordLocked(node)
        }
    }

    private fun record(id: Int, toSpec: AnimationSpec<Float>, fromSpec: AnimationSpec<Float>) {
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST") val entry = entries[id]
            if (entry != null) {
                if (entry.toSpec != toSpec || entry.fromSpec != fromSpec) {
                    entry.fromSpec = fromSpec
                    entry.toSpec = toSpec
                    entry.state = EntryState.Changed
                } else {
                    entry.state = EntryState.Unchanged
                }
            } else {
                entries[id] = Entry(toSpec, fromSpec)
            }
        }
    }

    fun preRecordLocked() =
        synchronized(lock) {
            entries.forEach { _, entry ->
                when (entry.state) {
                    EntryState.Inserted,
                    EntryState.Unchanged,
                    EntryState.Changed -> entry.state = EntryState.Untouched
                    else -> {}
                }
            }
        }

    fun postRecordLocked(node: StyleOuterNode) =
        synchronized(lock) {
            entries.forEach { _, entry ->
                when (entry.state) {
                    EntryState.Inserted -> {
                        entry.animateIn(node.animationScope)
                    }

                    EntryState.Untouched -> {
                        entry.state = EntryState.Removing
                        entry.animateOut(node.animationScope)
                    }

                    EntryState.Changed -> {
                        entry.interrupted(node.animationScope)
                    }
                    else -> {}
                }
            }
        }

    fun close() {
        synchronized(lock) {
            entries.forEach { _, entry -> entry.close() }
            entries.clear()
        }
    }

    /**
     * This is called when exit animations finish, and it removes any entries from the list which
     * are marked as "removing" but have their animations "not running".
     */
    private fun cleanupAnimations() =
        synchronized(lock) {
            entries.removeIf { _, entry ->
                entry.state == EntryState.Removing && !entry.animation.isRunning
            }
        }
}

private inline fun forEachBitOf(flags: Long, block: (value: Int) -> Unit) {
    var current = flags
    while (current != 0L) {
        val index = current.countTrailingZeroBits()
        block(index)
        current = current xor (1L shl index)
    }
}

private inline fun forEachBitOf(flags: Int, block: (value: Int) -> Unit) {
    var current = flags
    while (current != 0) {
        val index = current.countTrailingZeroBits()
        block(index)
        current = current xor (1 shl index)
    }
}

private inline fun forEachSetPropertyId(
    primitivesSet: Long,
    objectsSet: Int,
    block: (id: Int) -> Unit,
) {
    forEachBitOf(primitivesSet, block)
    forEachBitOf(objectsSet) { block(PrimitivePropertyCount + it) }
}
