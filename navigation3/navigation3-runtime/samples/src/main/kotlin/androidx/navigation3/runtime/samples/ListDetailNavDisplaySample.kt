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

package androidx.navigation3.runtime.samples

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.samples.ListDetailNavDisplay.IS_SUPPORTING_PANE

object ListDetailNavDisplay {
    internal const val IS_SUPPORTING_PANE = "isSupportingPane"

    fun isSupportingPane(value: Boolean) = mapOf(IS_SUPPORTING_PANE to value)
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun <T : Any> ListDetailNavDisplay(
    backstack: List<T>,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<*>> = emptyList(),
    onBack: () -> Unit = { if (backstack is MutableList) backstack.removeAt(backstack.size - 1) },
    windowWidthSizeClass: WindowWidthSizeClass =
        calculateWindowSizeClass(LocalActivity.current!!).widthSizeClass,
    entryProvider: (key: T) -> NavEntry<T>,
) {
    val isSinglePaneLayout = (windowWidthSizeClass == WindowWidthSizeClass.Compact)
    BackHandler(isSinglePaneLayout && backstack.size > 1, onBack)
    val entries = rememberDecoratedNavEntries(backstack, entryDecorators, entryProvider)
    val lastEntry = entries.last()
    if (isSinglePaneLayout) {
        Box(modifier = modifier) { lastEntry.Content() }
    } else {
        Row {
            var rightEntry: NavEntry<T>? = null
            val leftEntry: NavEntry<T>?
            val isSupportingPane = lastEntry.metadata[IS_SUPPORTING_PANE]?.equals(true) ?: false
            if (isSupportingPane) {
                // Display the penultimate entry in the left pane
                leftEntry = entries[entries.size - 2]
                // Display the last entry in the right pane
                rightEntry = lastEntry
            } else {
                // Display the last entry in the left pane
                leftEntry = lastEntry
            }
            // Left pane
            Box(modifier = modifier.fillMaxWidth(0.5F)) { leftEntry.Content() }
            // Right pane
            Box(modifier = modifier.fillMaxWidth()) {
                if (rightEntry == null) {
                    Text("Please select an item")
                } else {
                    rightEntry.Content()
                }
            }
        }
    }
}
