/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.FlushCoroutineDispatcher
import androidx.compose.ui.util.trace
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The scheduler for performing recomposition and applying updates to one or more [Composition]s.
 *
 * The main difference from [Recomposer] is separate dispatchers for LaunchEffect and other
 * recompositions that allows more precise status checking.
 *
 * @param coroutineContext The coroutine context to use for the compositor.
 * @param elements Additional coroutine context elements to include in context.
 */
internal class ComposeSceneRecomposer(
    coroutineContext: CoroutineContext,
    vararg elements: CoroutineContext.Element
) {
    private val job = Job()
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    /**
     * We use [FlushCoroutineDispatcher] not because we need [flush] for
     * LaunchEffect tasks, but because we need to know if it is idle (hasn't scheduled tasks)
     */
    private val effectDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposeDispatcher = FlushCoroutineDispatcher(coroutineScope)
    private val recomposer = Recomposer(coroutineContext + job + effectDispatcher)

    /**
     * `true` if there is any pending work scheduled, regardless of whether it is currently running.
     */
    val hasPendingWork: Boolean
        get() = recomposer.hasPendingWork ||
        effectDispatcher.hasTasks() ||
        recomposeDispatcher.hasTasks()

    val compositionContext: CompositionContext
        get() = recomposer

    init {
        var context: CoroutineContext = recomposeDispatcher
        for (element in elements) {
            context += element
        }
        coroutineScope.launch(context,
            start = CoroutineStart.UNDISPATCHED
        ) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    /**
     * Perform all scheduled tasks and wait for the tasks which are already
     * performing in the recomposition scope.
     */
    fun performScheduledTasks() = trace("ComposeSceneRecomposer:performScheduledTasks") {
        recomposeDispatcher.flush()
    }

    /**
     * Perform all scheduled effects.
     */
    fun performScheduledEffects() = trace("ComposeSceneRecomposer:performScheduledEffects") {
        effectDispatcher.flush()
    }

    /**
     * Permanently shut down this [Recomposer] for future use.
     *
     * @see Recomposer.cancel
     */
    fun cancel() {
        recomposer.cancel()
        job.cancel()
    }
}