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

package androidx.test.uiautomator.internal

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.children

/**
 * Internal core function that performs a DFS for all the publicly exposed api. This is called by
 * all the [androidx.test.uiautomator.onElement] and [androidx.test.uiautomator.onElements]
 * functions.
 */
internal fun findElements(
    timeoutMs: Long,
    pollIntervalMs: Long,
    shouldStop: (MutableList<AccessibilityNodeInfo>) -> (Boolean),
    block: AccessibilityNodeInfo.() -> (Boolean),
    rootNodeBlock: () -> (List<AccessibilityNodeInfo>),
): List<AccessibilityNodeInfo> {

    // DFS to find a element matching the given filter
    fun dfs(node: AccessibilityNodeInfo, collected: MutableList<AccessibilityNodeInfo>) {

        // Check if this is the node we're looking for
        if (block(node)) {
            collected.add(node)
            if (shouldStop(collected)) return
        }

        // If not, explore the children.
        node.children().forEach { child -> dfs(child, collected) }
    }

    // Run DFS starting from root produced by the given factory.
    fun findNodes(): List<AccessibilityNodeInfo> {
        val clock = TimeoutClock(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
        while (true) {
            val foundNodes =
                rootNodeBlock()
                    .map {
                        val list = mutableListOf<AccessibilityNodeInfo>()
                        dfs(node = it, collected = list)
                        list
                    }
                    .flatten()
            if (foundNodes.isNotEmpty()) return foundNodes
            if (clock.isTimeoutOrSleep()) return emptyList()

            // Run the ui watchers, in case there is a ui watcher for a dialog to dismiss.
            uiDevice.runWatchers()
        }
    }
    return findNodes()
}
