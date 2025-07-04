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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text

object SwipeToRevealBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
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
                    Modifier.fillMaxWidth().semantics { contentDescription = CONTENT_DESCRIPTION },
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
                    Text("This Button has two actions", modifier = Modifier.fillMaxSize())
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val swipeToReveal = device.findObject(By.desc(CONTENT_DESCRIPTION))
            // Setting a gesture margin is important otherwise gesture nav is triggered.
            swipeToReveal.setGestureMargin(device.displayWidth / 5)
            repeat(3) {
                swipeToReveal.swipe(Direction.LEFT, 1f, 500)
                device.waitForIdle()
                SystemClock.sleep(500)
                swipeToReveal.swipe(Direction.RIGHT, 1f, 500)
                device.waitForIdle()
                SystemClock.sleep(500)
            }
            swipeToReveal.swipe(Direction.LEFT, 1f, 500)
            device.waitForIdle()
            SystemClock.sleep(500)
            swipeToReveal.swipe(Direction.LEFT, 1f, 500)
            device.waitForIdle()
            SystemClock.sleep(600)
        }
}
