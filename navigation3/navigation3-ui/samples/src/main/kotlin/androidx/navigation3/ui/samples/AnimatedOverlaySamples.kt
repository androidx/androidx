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

package androidx.navigation3.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable private object Landing : NavKey

@Serializable private object AnimatedBottomSheet : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Sampled
fun AnimatedBottomSheetSample() {
    val backStack = rememberNavBackStack(Landing)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategy = AnimatedBottomSheetSceneStrategy(),
        entryProvider =
            entryProvider {
                entry<Landing> {
                    Box(Modifier.fillMaxSize().background(Color.Green)) {
                        Button(onClick = { backStack.add(AnimatedBottomSheet) }) {
                            Text("Click to open bottom sheet")
                        }
                    }
                }
                entry<AnimatedBottomSheet>(
                    metadata = AnimatedBottomSheetSceneStrategy.bottomSheet()
                ) {
                    Column {
                        Text("BottomSheet")
                        Button(onClick = { backStack.removeLastOrNull() }) {
                            Text("Close BottomSheet")
                        }
                    }
                }
            },
    )
}

/** An [SceneStrategy] that renders a [NavEntry] within a [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
private class AnimatedBottomSheetSceneStrategy() : SceneStrategy<Any> {

    override fun SceneStrategyScope<Any>.calculateScene(entries: List<NavEntry<Any>>): Scene<Any>? {
        val entry = entries.lastOrNull() ?: return null
        entry.metadata[MetadataKey] ?: return null

        return object : OverlayScene<Any> {
            override val key = entry.contentKey
            override val entries = listOf(entry)
            override val previousEntries = entries.dropLast(1)
            override val overlaidEntries = previousEntries.takeLast(1)

            lateinit var sheetState: SheetState

            override val content: @Composable (() -> Unit) = {
                sheetState = rememberModalBottomSheetState()
                val minHeight = LocalWindowInfo.current.containerSize.height * 0.2 // 50% height
                ModalBottomSheet(
                    onDismissRequest = onBack,
                    containerColor = Color.Blue,
                    sheetState = sheetState,
                    modifier = Modifier.heightIn(min = minHeight.dp),
                ) {
                    entry.Content()
                }
            }

            override suspend fun onRemove() {
                // run hide animations when this bottom sheet is popped from the backStack
                sheetState.hide()
            }
        }
    }

    companion object {
        object MetadataKey : NavMetadataKey<ModalBottomSheetProperties>

        fun bottomSheet(
            sheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties()
        ) = metadata { put(MetadataKey, sheetProperties) }
    }
}
