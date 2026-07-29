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
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.printToString
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
 * The output file contains two distinct sections:
 * 1. The View hierarchy, generated via Espresso's `HumanReadables`.
 * 2. The Compose Semantics trees for all provided Compose roots.
 *
 * The combined output is written directly to the
 * [androidx.test.platform.io.PlatformTestStorageRegistry].
 */
@Suppress("VisibleForTests", "UnsafeOptInUsageError")
internal class AndroidUiHierarchyHandler : UiHierarchyHandler {
    @Suppress("ListIterator")
    override fun export(fileName: String, roots: Set<ViewRootForTest>) {
        val storage = PlatformTestStorageRegistry.getInstance()

        storage.openOutputFile(fileName).use { stream ->
            PrintWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                val uniqueWindows = roots.map { getRootParent(it.view) }.distinct()

                if (uniqueWindows.isEmpty()) {
                    writer.println("====================================================")
                    writer.println("--- No Android View hierarchy found ---")
                    writer.println("====================================================")
                    writer.println()
                } else {
                    writer.println("====================================================")
                    writer.println("--- Android View Hierarchy ---")
                    writer.println("====================================================")
                    uniqueWindows.forEachIndexed { index, window ->
                        try {
                            writer.println(
                                HumanReadables.getViewHierarchyErrorMessage(
                                    window,
                                    null,
                                    "Window (index = $index)",
                                    null,
                                    Int.MAX_VALUE,
                                )
                            )
                        } catch (t: Throwable) {
                            writer.println("Failed to dump View hierarchy: ${t.message}")
                        }
                        writer.println()
                    }
                }

                if (roots.isEmpty()) {
                    writer.println("====================================================")
                    writer.println("--- No Compose roots found ---")
                    writer.println("====================================================")
                    writer.println()
                } else {
                    writer.println("====================================================")
                    writer.println("--- Compose Semantics Trees ---")
                    writer.println("====================================================")
                    roots.forEachIndexed { index, root ->
                        writer.println("--- Compose Root $index ---")
                        try {
                            val rootNode = root.semanticsOwner.rootSemanticsNode
                            writer.println(listOf(rootNode).printToString(maxDepth = Int.MAX_VALUE))
                        } catch (t: Throwable) {
                            writer.println("Failed to dump semantics: ${t.message}")
                        }
                        writer.println()
                    }
                }
            }
        }
    }

    private fun getRootParent(view: View): View {
        var current = view
        while (current.parent is View) {
            current = current.parent as View
        }
        return current
    }
}
