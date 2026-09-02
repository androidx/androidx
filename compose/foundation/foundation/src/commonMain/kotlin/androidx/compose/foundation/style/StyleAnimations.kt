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

import androidx.collection.ScatterMap
import androidx.collection.ScatterSet
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.platform.SynchronizedObject
import androidx.compose.foundation.platform.makeSynchronizedObject
import androidx.compose.foundation.platform.synchronized
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val DefaultSpringSpec = spring<Float>()

/**
 * This is a class that is specifically made to handle animated transitions between [Style]
 * properties on a [StyleResolver] which are using the StyleScope::animate API.
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

    internal inner class Entry(var spec: AnimationSpec<Float>, var start: Any?, var end: Any?) {
        var animation = Animatable(0f)
        var state = EntryState.Inserted
        var job: Job? = null

        fun animate(coroutineScope: CoroutineScope) {
            job?.cancel()
            job = coroutineScope.launch {
                synchronized(lock) {
                    if (state == EntryState.Interrupted) state = EntryState.Unchanged
                }
                try {
                    val velocity = animation.velocity
                    animation.snapTo(0f)
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

    internal var entries = SnapshotStateMap<StyleProperty<*>, Entry>()

    fun isEmpty() = synchronized(lock) { entries.isEmpty() }

    fun isNotEmpty() = synchronized(lock) { entries.isNotEmpty() }

    inline fun <T> animatedValueOrElse(property: StyleProperty<T>, defaultValue: () -> T): T =
        synchronized(lock) {
            val entry = this.entries[property] ?: return defaultValue()
            val t = if (entry.state == EntryState.Interrupted) 0f else entry.animation.value
            @Suppress("UNCHECKED_CAST") property.lerp(entry.start as T, entry.end as T, t)
        }

    fun animating(property: StyleProperty<*>) = property in entries

    /**
     * Record all animations.
     *
     * @param animating the properties that have were defined to have an animation.
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
        animating: ScatterSet<StyleProperty<*>>,
        changes: ScatterSet<StyleProperty<*>>,
        toSpecs: ScatterMap<StyleProperty<*>, AnimationSpec<Float>>?,
        fromSpecs: ScatterMap<StyleProperty<*>, AnimationSpec<Float>>?,
        previousFromSpecs: ScatterMap<StyleProperty<*>, AnimationSpec<Float>>?,
        startValues: StyleProperties,
        endValues: StyleProperties,
        node: StyleResolverNode,
    ) {
        synchronized(lock) {
            preRecordLocked()
            if (animating.isNotEmpty()) {
                // For all the target properties, record an animation
                animating.forEach { property ->
                    val spec =
                        toSpecs?.get(property)
                            ?: previousFromSpecs?.get(property)
                            ?: fromSpecs?.get(property)
                            ?: DefaultSpringSpec
                    recordLocked(property, property in changes, spec, startValues, endValues)
                }
            }
            postRecordLocked(node)
        }
    }

    private fun recordLocked(
        property: StyleProperty<*>,
        changed: Boolean,
        spec: AnimationSpec<Float>,
        startValues: StyleProperties,
        endValues: StyleProperties,
    ): Boolean {
        val entry = entries[property]
        return if (entry != null) {
            if (changed || entry.spec != spec) {
                entry.spec = spec
                entry.state = EntryState.Changed
                @Suppress("UNCHECKED_CAST")
                entry.start =
                    animatedValueOrElse(property as StyleProperty<Any?>) { startValues[property] }
                entry.end = endValues[property]
                true
            } else {
                entry.state = EntryState.Unchanged
                false
            }
        } else {
            if (changed) entries[property] = Entry(spec, startValues[property], endValues[property])
            true
        }
    }

    fun preRecordLocked() =
        entries.values.forEach { entry ->
            when (entry.state) {
                EntryState.Inserted,
                EntryState.Unchanged,
                EntryState.Changed -> entry.state = EntryState.Untouched
                else -> {}
            }
        }

    fun postRecordLocked(node: StyleResolverNode) =
        entries.values.forEach { entry ->
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
            entries.values.forEach { entry -> entry.close() }
            entries.clear()
        }
    }

    /**
     * This is called when exit animations finish, and it removes any entries from the list which
     * are marked as "removing" but have their animations "not running".
     */
    private fun cleanupAnimations() =
        synchronized(lock) {
            entries.entries.removeAll { (_, entry) ->
                entry.state != EntryState.Interrupted && !entry.animation.isRunning
            }
        }
}
