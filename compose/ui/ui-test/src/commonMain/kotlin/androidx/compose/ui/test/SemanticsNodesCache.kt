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

package androidx.compose.ui.test

import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode

/**
 * Caches the [SemanticsNode]s collected from compose roots, so that repeated node queries don't
 * have to walk the semantics trees every time.
 *
 * Cached nodes are discarded when the time of the [clock] changes, because advancing the clock can
 * recompose and thus change the semantics trees. Call [clear] when the semantics trees can change
 * in other ways.
 *
 * Only use this while the UI is expected to be stable, for example during
 * [ComposeUiTest.runWithoutImplicitWait].
 */
internal class SemanticsNodesCache(private val clock: MainTestClock) {
    private val cachedNodes = mutableMapOf<Key, List<SemanticsNode>>()

    /** The [clock] time at which the cache was last cleared. */
    private var cacheTime = clock.currentTime

    /**
     * Returns the cached semantics nodes of the given [roots], or collects and caches them if they
     * are not cached yet.
     *
     * @see collectSemanticsNodes
     */
    fun getOrCollect(
        roots: Set<RootForTest>,
        useUnmergedTree: Boolean,
        skipDeactivatedNodes: Boolean,
    ): List<SemanticsNode> {
        if (clock.currentTime != cacheTime) {
            clear()
        }
        return cachedNodes.getOrPut(Key(roots, useUnmergedTree, skipDeactivatedNodes)) {
            collectSemanticsNodes(roots, useUnmergedTree, skipDeactivatedNodes)
        }
    }

    /** Discards all cached semantics nodes. */
    fun clear() {
        cachedNodes.clear()
        cacheTime = clock.currentTime
    }

    private class Key(
        val roots: Set<RootForTest>,
        val useUnmergedTree: Boolean,
        val skipDeactivatedNodes: Boolean,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key) return false
            if (roots != other.roots) return false
            if (useUnmergedTree != other.useUnmergedTree) return false
            if (skipDeactivatedNodes != other.skipDeactivatedNodes) return false
            return true
        }

        override fun hashCode(): Int {
            var result = roots.hashCode()
            result = 31 * result + useUnmergedTree.hashCode()
            result = 31 * result + skipDeactivatedNodes.hashCode()
            return result
        }
    }
}
