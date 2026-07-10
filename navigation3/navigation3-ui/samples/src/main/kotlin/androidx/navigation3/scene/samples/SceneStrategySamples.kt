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

package androidx.navigation3.scene.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

@Suppress("FunctionName", "unused")
@Sampled
fun SceneStrategyOnBackSample() {
    /** Class that shows a [NavEntry] as a [Dialog]. */
    class SimpleDialogScene<T : Any>(
        private val entry: NavEntry<T>,
        override val previousEntries: List<NavEntry<T>>,
        onBack: () -> Unit,
    ) : OverlayScene<T> {
        override val key = entry.contentKey
        override val entries = listOf(entry)
        override val overlaidEntries = previousEntries

        override val content: @Composable () -> Unit = {
            Dialog(onDismissRequest = onBack) { entry.Content() }
        }
    }

    /**
     * A [SceneStrategy] that shows every screen above the first one as a dialog.
     *
     * In a real example, you'd have developers opt into displaying their entry as a dialog by
     * providing a companion method that provides [NavEntry.metadata].
     *
     * @see androidx.navigation3.scene.DialogSceneStrategy
     */
    class SimpleDialogSceneStrategy<T : Any> : SceneStrategy<T> {
        override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
            val entry = entries.lastOrNull() ?: return null
            val previousEntries = entries.dropLast(1)
            // Only show this as a dialog if there is something 'underneath' the dialog
            return if (previousEntries.isNotEmpty()) {
                SimpleDialogScene(entry, previousEntries, onBack)
            } else {
                null
            }
        }
    }
}
