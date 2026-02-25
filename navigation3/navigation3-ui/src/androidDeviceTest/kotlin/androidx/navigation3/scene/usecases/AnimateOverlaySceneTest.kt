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

package androidx.navigation3.scene.usecases

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimateOverlaySceneTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    private object First

    private object Second

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun testAnimateOut() {
        lateinit var backStack: MutableList<Any>
        val testTag = "testTag"
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            backStack = remember { mutableStateListOf(First) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                // sheetState and onRemoved implemented here for test readability
                // realistically it should be implemented within the scene
                sceneStrategies =
                    listOf(
                        AnimatedBottomSheetSceneStrategy(
                            sheetState = sheetState,
                            onRemoved = { sheetState.hide() },
                        )
                    ),
                entryProvider =
                    entryProvider {
                        entry(First) { Text("First") }
                        entry(
                            key = Second,
                            metadata = AnimatedBottomSheetSceneStrategy.animatedBottomSheet(),
                        ) {
                            Column {
                                Text("Second")
                                Button(
                                    modifier = Modifier.testTag(testTag),
                                    onClick = { backStack.removeLastOrNull() },
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    },
            )
        }

        composeTestRule.onNodeWithText("First").assertIsDisplayed()

        backStack.add(Second)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Second").assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag(testTag).performClick()

        // make sure overlay is popped
        assertThat(backStack).containsExactly(First)

        repeat(8) { composeTestRule.mainClock.advanceTimeByFrame() }
        // assert that hide animation is running after pop
        composeTestRule.onNodeWithTag(testTag).assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(testTag).assertIsNotDisplayed()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class AnimatedBottomSheetSceneStrategy(
    val sheetState: SheetState,
    val onRemoved: suspend () -> Unit,
) : SceneStrategy<Any> {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun SceneStrategyScope<Any>.calculateScene(entries: List<NavEntry<Any>>): Scene<Any>? {
        val entry = entries.lastOrNull() ?: return null
        entry.metadata[MetadataKey] ?: return null

        return object : OverlayScene<Any> {
            override val key = entry.contentKey
            override val entries = listOf(entry)
            override val previousEntries = entries.dropLast(1)
            override val overlaidEntries = previousEntries.takeLast(1)

            override val content: @Composable (() -> Unit) = {
                val minHeight = LocalWindowInfo.current.containerSize.height * 0.2 // 50% height
                ModalBottomSheet(
                    onDismissRequest = { onBack() },
                    containerColor = Color.Blue,
                    sheetState = sheetState,
                    modifier = Modifier.heightIn(min = minHeight.dp),
                ) {
                    entry.Content()
                }
            }

            override suspend fun onRemove() {
                onRemoved.invoke()
            }
        }
    }

    companion object {
        object MetadataKey : NavMetadataKey<Boolean>

        fun animatedBottomSheet() = metadata { put(MetadataKey, true) }
    }
}
