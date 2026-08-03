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

package androidx.compose.ui.test.failure

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.printToString
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.test.espresso.util.HumanReadables
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.OutputStreamWriter
import java.io.PrintWriter

@Suppress("VisibleForTests")
internal interface UiHierarchyHandler {
    fun export(fileName: String, roots: Set<ViewRootForTest>)
}

/**
 * Implementation of [UiHierarchyHandler] that generates a human-readable text dump of the current
 * UI state.
 *
 * The output file contains an interleaved mixture of the Android View hierarchy and Compose
 * Semantics trees. Starting from the root windows, as each View is traversed and printed (via
 * Espresso's [HumanReadables]), any Compose Semantics trees hosted by that View are indented and
 * printed directly as children in the hierarchy.
 */
@Suppress("VisibleForTests")
internal class AndroidUiHierarchyHandler : UiHierarchyHandler {
    override fun export(fileName: String, roots: Set<ViewRootForTest>) {
        val storage = PlatformTestStorageRegistry.getInstance()

        storage.openOutputFile(fileName).use { stream ->
            PrintWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                val uniqueWindows = roots.map { getRootParent(it.view) }.fastDistinctBy { it }

                if (uniqueWindows.isEmpty()) {
                    writer.println("====================================================")
                    writer.println("--- No UI hierarchy found ---")
                    writer.println("====================================================")
                    writer.println()
                } else {
                    writer.println("====================================================")
                    writer.println("--- View and Compose Hierarchy ---")
                    writer.println("====================================================")
                    val rootsByView = roots.groupBy { it.view }
                    val visitedRoots = mutableSetOf<ViewRootForTest>()

                    uniqueWindows.fastForEachIndexed { index, window ->
                        writer.println("Window (index = $index)")
                        writer.println()
                        try {
                            dumpViewHierarchy(writer, window, rootsByView, visitedRoots, depth = 0)
                        } catch (t: Throwable) {
                            writer.println("Failed to dump UI hierarchy: ${t.message}")
                        }
                        writer.println()
                    }

                    val unvisited = roots - visitedRoots
                    if (unvisited.isNotEmpty()) {
                        writer.println("--- Unattached Compose Roots ---")
                        unvisited.forEachIndexed { index, root ->
                            writer.println("--- Unattached Root $index ---")
                            dumpComposeSemantics(writer, root, depth = 0)
                            writer.println()
                        }
                    }
                }
            }
        }
    }

    private fun dumpViewHierarchy(
        writer: PrintWriter,
        view: View,
        rootsByView: Map<View, List<ViewRootForTest>>,
        visitedRoots: MutableSet<ViewRootForTest>,
        depth: Int,
    ) {
        if (depth > 0) {
            writer.println("|")
        }
        writer.println("${getPrefix(depth)}${HumanReadables.describe(view)}")

        rootsByView[view]?.let { viewRoots ->
            viewRoots.fastForEach { root ->
                visitedRoots.add(root)
                writer.println("|")
                dumpComposeSemantics(writer, root, depth + 1)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                dumpViewHierarchy(writer, child, rootsByView, visitedRoots, depth + 1)
            }
        }
    }

    private fun dumpComposeSemantics(writer: PrintWriter, root: ViewRootForTest, depth: Int) {
        try {
            val rootNode = root.semanticsOwner.rootSemanticsNode
            val dump = listOf(rootNode).printToString(maxDepth = Int.MAX_VALUE)
            val lines = dump.lines()
            val prefix = getPrefix(depth)
            val indent = " ".repeat(prefix.length)
            lines.fastForEachIndexed { index, line ->
                if (index == lines.lastIndex && line.isEmpty()) return@fastForEachIndexed
                if (index == 0) {
                    writer.println("$prefix$line")
                } else {
                    writer.println("$indent$line")
                }
            }
        } catch (t: Throwable) {
            writer.println("${getPrefix(depth)}Failed to dump semantics: ${t.message}")
        }
    }

    private fun getPrefix(depth: Int): String = "+" + "-".repeat(depth) + ">"

    private fun getRootParent(view: View): View {
        var current = view
        while (current.parent is View) {
            current = current.parent as View
        }
        return current
    }
}
