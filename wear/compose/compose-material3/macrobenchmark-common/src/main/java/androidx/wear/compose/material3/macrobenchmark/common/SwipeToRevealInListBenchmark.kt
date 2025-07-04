/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.launch

object SwipeToRevealInListBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val transformationSpec = rememberTransformationSpec()
            BenchmarkingList(15) { index ->
                val buttonText = remember(index) { "Row #$index" }
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = { /* This block is called when the primary action is executed. */
                            },
                            icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                            text = { Text("Delete") },
                        )
                    },
                    onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .semantics { contentDescription = CONTENT_DESCRIPTION + index }
                            .transformedHeight(this@BenchmarkingList, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                                // Is needed to disable clipping.
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                clip = false
                            },
                    secondaryAction = {
                        SecondaryActionButton(
                            onClick = { /* This block is called when the secondary action is executed. */
                            },
                            icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "Options") },
                        )
                    },
                    undoPrimaryAction = {
                        UndoActionButton(
                            onClick = { /* This block is called when the undo primary action is executed. */
                            },
                            text = { Text("Undo Delete") },
                        )
                    },
                ) {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth().semantics {
                                // Use custom actions to make the primary and secondary actions
                                // accessible
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction("Delete") {
                                            /* Add the primary action click handler here */
                                            true
                                        },
                                        CustomAccessibilityAction("Options") {
                                            /* Add the secondary click handler here */
                                            true
                                        },
                                    )
                            },
                        onClick = {},
                    ) {
                        Text(buttonText, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

    /** Place a composable within the conditions of a [TransformingLazyColumn] */
    @Composable
    private fun BenchmarkingList(
        count: Int,
        testContent: @Composable TransformingLazyColumnItemScope.(Int) -> Unit,
    ) {
        val state = rememberTransformingLazyColumnState()
        val coroutineScope = rememberCoroutineScope()
        AppScaffold {
            ScreenScaffold(
                state,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
                edgeButton = {
                    EdgeButton(onClick = { coroutineScope.launch { state.scrollToItem(1) } }) {
                        Text("To top")
                    }
                },
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    modifier =
                        Modifier.background(MaterialTheme.colorScheme.background).semantics {
                            contentDescription = CONTENT_DESCRIPTION
                        },
                ) {
                    items(count) { index ->
                        Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            testContent(index)
                        }
                    }
                }
            }
        }
    }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val column = device.findObject(By.scrollable(true))
            val sleepTime = 500L
            repeat(4) { iteration ->
                val desc = CONTENT_DESCRIPTION + iteration

                val swipeToReveal = column.findObject(By.desc(desc))

                // the distance from the top of the column to the bottom of swipeToReveal as a
                // fraction
                val scrollDistanceFraction =
                    (swipeToReveal.visibleBounds.bottom.toFloat() /
                        column.visibleBounds.height().toFloat())

                // Setting a gesture margin is important otherwise gesture nav is triggered.
                swipeToReveal.setGestureMargin(device.displayWidth / 5)

                // go back and forth on the swipe action
                swipeToReveal.swipe(Direction.LEFT, 1f, 500)

                // Scroll down slightly to ensure we trigger any position-related compose code
                SystemClock.sleep(sleepTime)
                device.waitForIdle()
                column.scroll(Direction.DOWN, scrollDistanceFraction * 0.25f)

                device.waitForIdle()
                swipeToReveal.swipe(Direction.RIGHT, 1f, 500)

                device.waitForIdle()
                swipeToReveal.swipe(Direction.LEFT, 1f, 500)
                // finish the swipe action
                device.waitForIdle()
                swipeToReveal.swipe(Direction.LEFT, 1f, 500)

                SystemClock.sleep(sleepTime)
                device.waitForIdle()
                column.scroll(Direction.DOWN, scrollDistanceFraction * 0.75f)
            }
            column.scrollUntil(Direction.UP, Until.hasObject(By.text(CONTENT_DESCRIPTION + 0)))
        }
}
