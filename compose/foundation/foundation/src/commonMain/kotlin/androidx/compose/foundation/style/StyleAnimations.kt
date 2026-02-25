/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.unit.Density
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val FlagUntouched = 1
private const val FlagUnchanged = 2
private const val FlagInserted = 3
private const val FlagRemoving = 4

/**
 * This is a class that is specifically made to handle animated transitions between [Style]
 * properties on a [StyleOuterNode] which are using the StyleScope::animate API.
 *
 * These animations are declarative in the sense that the developer is just specifying the styles
 * they want in specific states, and then the Style system is figuring out how to animate it from
 * the current style to the desired one.
 *
 * We want these animations to be as performant as possible, and importantly we have designed this
 * system such that it can cause only the necessary phases to be invalidated as we are animating,
 * and also making it so that these animated values are only allocated when they are needed, and no
 * sooner, so initial compositions shouldn't have to deal with allocating these animations.
 */
internal class StyleAnimations(val node: StyleOuterNode) {
    /**
     * There is a single Entry per `animate` block in a Style lambda. This entry is created when the
     * block becomes active, and then is disposed when the block becomes inactive and the "exit"
     * animation is complete.
     */
    inner class Entry(
        val key: Int,
        var style: Style,
        var toSpec: AnimationSpec<Float>,
        var fromSpec: AnimationSpec<Float>,
    ) {
        val anim = Animatable(0f)
        val styleScope = ResolvedStyle()

        /**
         * This is the current "state" of this entry. These states are used to understand what needs
         * to be done before/after/during resolving a style. We have the following states:
         *
         *      - Untouched.
         *        This means that during the resolve step, this animation did NOT get recorded. This
         *        is an indication that the corresponding Style is no longer active, which means
         *        that we need to transition into the "Removing" state and call "animateOut".
         *      - Unchanged.
         *        This means that during the resolve step, this animation was "recorded", but
         *        nothing has changed. So if we were animating, keep animating. If we were done
         *        animating, then do nothing.
         *      - Inserted.
         *        This style was added during the most recent resolve. This means that we have yet
         *        to call the "animateIn" API to start this animation.
         *      - Removing.
         *        The style is "animating out". So the style is no longer active but we need to keep
         *        it in the list so that we see the exit animation.
         */
        var state: Int = FlagInserted
        var job: Job? = null

        fun animateIn(coroutineScope: CoroutineScope) {
            job?.cancel()
            job = coroutineScope.launch { anim.animateTo(1f, animationSpec = toSpec) }
        }

        fun snapIn(coroutineScope: CoroutineScope) {
            job?.cancel()
            job = coroutineScope.launch { anim.snapTo(1f) }
        }

        fun animateOut(coroutineScope: CoroutineScope) {
            job?.cancel()
            job =
                coroutineScope.launch {
                    try {
                        anim.animateTo(0f, animationSpec = fromSpec)
                    } finally {
                        // When we finish animation, we call cleanup, which is what finally removes
                        // this entry from the list of entries
                        cleanupAnimations()
                    }
                }
        }

        fun snapOut(coroutineScope: CoroutineScope) {
            job?.cancel()
            job =
                coroutineScope.launch {
                    try {
                        anim.snapTo(0f)
                    } finally {
                        cleanupAnimations()
                    }
                }
        }
    }

    // Since we only allocate a StyleAnimations instance in StyleOuterNode when an actual animation
    // is first triggered, we can be pretty certain that if we are creating one of these we are
    // going to have at least one Entry and be animating that Entry. As a result, we should go
    // ahead and allocate a ResolvedStyle for this class.
    private val currentStyle: ResolvedStyle = ResolvedStyle()
    // Defaulting to an array of size 2. There might be a more optimal value but this seems like a
    // nice conservative number to start with. Most elements won't have many active animations going
    // on at one time I don't think.
    private var values: Array<Entry?> = arrayOfNulls(2)
    private var size: Int = 0

    fun isNotEmpty() = size > 0

    private fun find(key: Int): Entry? {
        val values = values
        val size = size
        @Suppress("EmptyRange")
        for (i in 0 until size) {
            val entry = values[i]
            if (entry?.key == key) {
                return entry
            }
        }
        return null
    }

    /** Ensures internal arrays have at least the required capacity. */
    private fun ensureCapacity(requiredCapacity: Int) {
        val current = values.size
        if (requiredCapacity > current) {
            values = values.copyOf(max(current * 2, 2))
        }
    }

    fun record(
        key: Int,
        style: Style,
        toSpec: AnimationSpec<Float>,
        fromSpec: AnimationSpec<Float>,
    ) {
        val animation = find(key)
        if (animation != null) {
            animation.style = style
            val state = animation.state
            // Key exists, update value and state
            // Update diff state: If it was untouched before, mark as written.
            // If it was already written or added during this block, keep that state.
            if (state == FlagUntouched) {
                animation.state = FlagUnchanged
            } else if (state == FlagRemoving) {
                // if it was marked as removing, then the exit animation is still in progress, but
                // we mark it as "inserted" so we do the enter animation, even if we aren't actually
                // inserting anything into the map
                animation.state = FlagInserted
            }
        } else {
            // Key doesn't exist, add new entry
            ensureCapacity(size + 1)
            val insertIndex = size
            values[insertIndex] = Entry(key, style, toSpec, fromSpec)
            size++
        }
    }

    // TODO: We need to rethink the strategy for applying animated styles since the blocks will get
    //  executed "late", even if there are higher-priority style blocks
    fun withAnimations(
        density: Density,
        staticStyle: ResolvedStyle,
        node: StyleOuterNode,
        forChanges: Int,
    ): ResolvedStyle {
        val result = currentStyle
        staticStyle.copyInto(result)
        applyAnimationsTo(result, density, node, forChanges)
        return result
    }

    /**
     * This is the method where the majority of the work gets done in this class.
     *
     * The algorithm is roughly:
     *
     *      for each Entry/animation that is currently running
     *          target into entry ResolvedStyle, "reference"
     *          resolve the style into reference
     *          if any of the changes match the [forChanges] flag
     *              lerp from target -> reference with the given entry's animated value
     */
    fun applyAnimationsTo(
        target: ResolvedStyle,
        density: Density,
        node: StyleOuterNode,
        forChanges: Int,
    ) {
        forEach { animation ->
            target.copyInto(animation.styleScope)
            // TODO: this could be done as one step, where resolve returns the same thing that
            //  result.diff(...) returns right now.
            // TODO: do we still need to be doing this?
            //  or is resolvedStyle already populated enough?
            // TODO: instead of calling diff(...), we could reset the flags on the style scope and'
            //  just check those after resolving
            animation.styleScope.resolve(animation.style, node, density, true)

            val changed = target.diff(animation.styleScope, forChanges)
            if (changed and forChanges != 0) {
                lerp(target, animation.styleScope, animation.anim.value, forChanges, target)
            }
        }
    }

    /**
     * Updates states for the next cycle. This is expected to be called right before a call to
     * "resolve" a style.
     *
     * Items added or modified previously now become the baseline 'present'.
     */
    fun preResolve() {
        forEach {
            it.state =
                when (it.state) {
                    FlagUnchanged -> FlagUntouched
                    FlagInserted -> FlagUntouched
                    else -> it.state
                }
        }
    }

    /**
     * We need to run this after a call to resolve(). This goes through the entry list and uses the
     * state of each entry to call animateIn() on the newly inserted entries and animateOut() on the
     * newly "removed" entries.
     *
     * This method will end up calling `resolve` on the animate Style lambdas, which means that it
     * is possible that a state object gets read. As a result, this should be called from inside an
     * observation scope.
     *
     * The returned value is the "flags" of the phases which will be affected by newly animating
     * entries. This can be used to understand better which phases need to be invalidated to reflect
     * these animations to the user.
     */
    fun postResolve(node: StyleOuterNode, density: Density, triggerAnimations: Boolean): Int {
        var changedFlags = 0
        forEach {
            if (it.state == FlagInserted) {
                it.styleScope.resolve(it.style, node, density, true)
                changedFlags = changedFlags or it.styleScope.flags
                if (triggerAnimations) it.animateIn(node.node.coroutineScope)
                else it.snapIn(node.node.coroutineScope)
            } else if (it.state == FlagUntouched) {
                it.state = FlagRemoving
                it.styleScope.clear()
                it.styleScope.resolve(it.style, node, density, true)
                changedFlags = changedFlags or it.styleScope.flags
                if (triggerAnimations) it.animateOut(node.node.coroutineScope)
                else it.snapOut(node.node.coroutineScope)
            }
        }
        return changedFlags
    }

    /**
     * This is called when exit animations finish, and it removes any entries from the list which
     * are marked as "removing" but have their animations "not running".
     */
    private fun cleanupAnimations() {
        val values = values
        val size = size
        var i = 0
        var j = 0
        while (j < size) {
            // this is the next non-null value
            val value = values[j] ?: break
            if (value.state == FlagRemoving && !value.anim.isRunning) {
                values[j] = null
                j++
            } else {
                if (i != j) {
                    values[i] = value
                    values[j] = null
                }
                i++
                j++
            }
        }
        this.size = i
        if (size != i) {
            // It might be debatable if we need this. If we don't call this, then the current UI
            // reflects a version of these animations which is still "interpolated", even if the
            // interpolated value might be effectively 0.
            recursionGuard {
                // This may be called recursively. If we are already cleaning up there is no need
                // to do it recursively.
                node.resolveStyleAndInvalidate()
            }
        }
    }

    private inline fun forEach(action: (value: Entry) -> Unit) {
        val values = values
        val size = size
        for (i in 0 until size) {
            val entry = values[i]
            if (entry != null) action(entry)
        }
    }

    private var inGuard = false

    private inline fun recursionGuard(block: () -> Unit) {
        if (inGuard) return
        inGuard = true
        try {
            block()
        } finally {
            inGuard = false
        }
    }
}
