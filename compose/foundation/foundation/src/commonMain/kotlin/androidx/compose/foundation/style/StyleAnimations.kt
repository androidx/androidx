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
        Interrupted,
        Removing,
    }

    private inner class Entry(var spec: AnimationSpec<Float>) {
        var animation = Animatable(0f)
        var state = EntryState.Inserted
        var job: Job? = null

        fun animate(coroutineScope: CoroutineScope) {
            job?.cancel()
            job =
                coroutineScope.launch {
                    synchronized(lock) {
                        if (state == EntryState.Interrupted) state = EntryState.Unchanged
                    }
                    try {
                        val velocity = animation.velocity
                        animation = Animatable(0f)
                        animation.animateTo(1f, animationSpec = spec, initialVelocity = velocity)
                    } finally {
                        cleanupAnimations()
                    }
                }
        }

        fun interrupted(coroutineScope: CoroutineScope) {
            animate(coroutineScope)
        }

        fun close() {
            job?.cancel()
            job = null
        }
    }

    private var entries: MutableIntObjectMap<Entry> = mutableIntObjectMapOf()

    fun phaseFlags(): Int {
        var primitivesSet = 0L
        var objectsSet = 0
        entries.forEach { it, _ ->
            if (it < PrimitivePropertyCount) primitivesSet = primitivesSet.withId(it.toByte())
            else objectsSet = objectsSet.withId(it)
        }
        return objectPhaseFlagsOf(objectsSet) or primitivePhaseFlagsOf(primitivesSet)
    }

    fun isEmpty() = synchronized(lock) { entries.isEmpty() }

    @Suppress("NOTHING_TO_INLINE") inline fun timeOf(propertyId: Byte) = timeOf(propertyId.toInt())

    fun timeOf(propertyId: Int): Float =
        synchronized(lock) {
            val entry = this.entries[propertyId] ?: return 0f
            if (entry.state == EntryState.Interrupted) 0f else entry.animation.value
        }

    fun inFlight(): Long =
        synchronized(lock) {
            var inFlight = 0L

            entries.forEach { id, entry ->
                if (entry.state == EntryState.Interrupted || entry.animation.isRunning) {
                    inFlight = inFlight or (1L shl id)
                }
            }

            inFlight
        }

    /**
     * Record all animations.
     *
     * @param animating the properties that have been were defined to have an animation.
     * @param changes the set of properties that have changed
     * @param toSpecs the map of properties to the to animation specs. If a property is in
     *   [animating] and not in this map then the spec is assumed to be [DefaultSpringSpec].
     * @param fromSpecs the map of properties to from animation specs. If the property is in
     *   [animating] an is not in this map then it is assumed to be the [toSpecs] value.
     * @param previousFromSpecs the previous [fromSpecs].
     * @return the set of animations that have been scheduled to be started or restarted.
     */
    @Suppress("UNCHECKED_CAST")
    fun recordAnimations(
        animating: Long,
        changes: Long,
        toSpecs: IntObjectMap<AnimationSpec<Float>>?,
        fromSpecs: IntObjectMap<AnimationSpec<Float>>?,
        previousFromSpecs: IntObjectMap<AnimationSpec<Float>>?,
        node: StyleOuterNode,
    ): Long {
        var started = 0L
        synchronized(lock) {
            preRecordLocked()
            if (animating != 0L) {
                // For all the target properties, record an animation
                forEachBitOf(animating) { id ->
                    val spec =
                        toSpecs?.get(id)
                            ?: previousFromSpecs?.get(id)
                            ?: fromSpecs?.get(id)
                            ?: DefaultSpringSpec
                    val changed = changes.hasAnimationId(id)
                    if (recordLocked(id, changed, spec)) started = started.withId(id)
                }
            }
            postRecordLocked(node)
        }
        return started
    }

    private fun recordLocked(id: Int, changed: Boolean, spec: AnimationSpec<Float>): Boolean {
        val entry = entries[id]
        return if (entry != null) {
            if (changed || entry.spec != spec) {
                entry.spec = spec
                entry.state = EntryState.Changed
                true
            } else {
                entry.state = EntryState.Unchanged
                false
            }
        } else {
            if (changed) entries[id] = Entry(spec)
            true
        }
    }

    fun preRecordLocked() =
        entries.forEach { _, entry ->
            when (entry.state) {
                EntryState.Inserted,
                EntryState.Unchanged,
                EntryState.Changed -> entry.state = EntryState.Untouched
                else -> {}
            }
        }

    fun postRecordLocked(node: StyleOuterNode) =
        entries.forEach { _, entry ->
            when (entry.state) {
                EntryState.Inserted -> {
                    entry.animate(node.animationScope)
                }
                EntryState.Untouched -> {
                    entry.state = EntryState.Removing
                }
                EntryState.Changed -> {
                    entry.state = EntryState.Interrupted
                    entry.interrupted(node.animationScope)
                }
                else -> {}
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
                entry.state != EntryState.Interrupted && !entry.animation.isRunning
            }
        }
}

@ExperimentalFoundationStyleApi
internal fun StyleAnimations?.isNullOrEmpty() = this == null || this.isEmpty()

private inline fun forEachBitOf(flags: Long, block: (value: Int) -> Unit) {
    var current = flags
    while (current != 0L) {
        val index = current.countTrailingZeroBits()
        block(index)
        current = current xor (1L shl index)
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.hasAnimationId(id: Int) = this and (1L shl id) != 0L

@Suppress("NOTHING_TO_INLINE") private inline fun Long.withId(id: Int) = this or (1L shl id)

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.toPrimitivesSet() = this and ((1L shl PrimitivePropertyCount + 1) - 1L)

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.toObjectsSet() = (this shr PrimitivePropertyCount).toInt()

@Suppress("NOTHING_TO_INLINE")
internal fun propertySetsToAnimationSet(primitivesSet: Long, objectsSet: Int) =
    primitivesSet or (objectsSet.toLong() shl FirstObjectProperty)
