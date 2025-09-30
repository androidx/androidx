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

package androidx.compose.ui.inspection.recompositions

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.compose.runtime.Composer
import androidx.compose.ui.inspection.LOG_TAG
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.inspection.ArtTooling

private const val START_RESTART_GROUP = "startRestartGroup(I)Landroidx/compose/runtime/Composer;"
private const val SKIP_TO_GROUP_END = "skipToGroupEnd()V"

/** Detection of recompose counts and skips from installing runtime hooks. */
open class RecompositionHandler<T : RecompositionData>(
    protected val artTooling: ArtTooling,
    protected val anchorMap: AnchorMap,
    protected val createRecompositionData: () -> T,
) {
    protected val lock = Any()

    // True, if currently collecting recomposition counts.
    @GuardedBy("lock") private var collectingRecompositionCounts = false

    // The data collected for recomposition counts and state reads.
    @GuardedBy("lock") protected val counts = mutableMapOf<Any, T>()

    init {
        currentHandler = this
        if (artTooling.javaClass.simpleName != "DefaultArtTooling") {
            // The ArtTooling used in tests: DefaultArtTooling is backed by a global instance
            // of ArtToolingImpl which is unable to undo bytecode manipulations.
            // For tests: install the hooks once and do not call DefaultArtTooling.unregisterHooks.
            //
            // The ArtTooling used in production comes from Studio and behaves differently.
            // For production: install the hooks everytime we create the compose inspector.
            hooksInstalled = false
        }
    }

    open fun dispose() {
        counts.clear()
        currentHandler = null
    }

    fun changeCollectionMode(startCollecting: Boolean, keepCounts: Boolean) {
        synchronized(lock) {
            if (startCollecting != collectingRecompositionCounts) {
                if (!hooksInstalled) {
                    installHooks(artTooling)
                }
                collectingRecompositionCounts = startCollecting
            }
            if (!keepCounts) {
                counts.clear()
            }
        }
    }

    // Return the recomposition counts and skips
    fun getCounts(anchorHash: Int): RecompositionData? {
        synchronized(lock) {
            return anchorMap[anchorHash]?.let { counts[it] }
        }
    }

    // Increment the recomposition count.
    // Adjust the state reads based on the max number of recompositions with state reads
    // the agent is supposed to maintain.
    open fun incrementRecompositionCount(anchor: Any): T? {
        synchronized(lock) {
            if (collectingRecompositionCounts) {
                val data = counts.getOrPut(anchor, createRecompositionData)
                data.incrementCount()
                return data
            }
            return null
        }
    }

    // The last recomposition was skipped.
    fun incrementRecompositionSkip(anchor: Any) {
        synchronized(lock) {
            if (collectingRecompositionCounts) {
                counts[anchor]?.incrementSkip()
            }
        }
    }
}

private var hooksInstalled = false
private var currentHandler: RecompositionHandler<*>? = null

/**
 * We install 2 hooks:
 * - exit hook for ComposerImpl.startRestartGroup gives us the anchor of the composable
 * - entry hook for ComposerImpl.skipToGroupEnd converts a recompose count to a skip count.
 */
private fun installHooks(artTooling: ArtTooling) {
    if (hooksInstalled) {
        return
    }
    hooksInstalled = true

    composerImplementationClasses().forEach { composer ->
        artTooling.registerExitHook(composer, START_RESTART_GROUP) { composer: Composer ->
            composer.recomposeScopeIdentity?.let { anchor ->
                currentHandler?.incrementRecompositionCount(anchor)
            }
            composer
        }

        artTooling.registerEntryHook(composer, SKIP_TO_GROUP_END) { obj, _ ->
            val composer = obj as? Composer
            composer?.recomposeScopeIdentity?.let { anchor ->
                currentHandler?.incrementRecompositionSkip(anchor)
            }
        }
    }
}

private fun composerImplementationClasses(): List<Class<*>> {
    val baseName = Composer::class.java.name.let { it.substring(0..it.lastIndexOf('.')) }
    val classes = mutableListOf<Class<*>>()
    try {
        classes.add(Class.forName(baseName + "ComposerImpl"))
    } catch (_: Throwable) {}
    try {
        classes.add(Class.forName(baseName + "GapComposer"))
    } catch (_: Throwable) {}
    try {
        classes.add(Class.forName(baseName + "LinkComposer"))
    } catch (_: Throwable) {}
    if (classes.isEmpty()) {
        Log.w(LOG_TAG, "Could not install recomposition hooks")
    }
    return classes
}
