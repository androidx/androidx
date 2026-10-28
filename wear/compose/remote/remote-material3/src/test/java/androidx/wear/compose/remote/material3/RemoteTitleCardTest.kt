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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteTitleCardTest {
    @get:Rule val painterRule = DocumentPainterRule(canvasSize = 400f)

    @Test
    fun button_withCallerDrawWithContent_preservesContainerBackground() {
        val painter = painterRule.paintDocument {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteButton(
                    onClick = RemoteAction.Empty,
                    modifier = RemoteModifier.drawWithContent { drawContent() },
                    colors = RemoteButtonDefaults.buttonColors(containerColor = Color.Red.rc),
                ) {
                    RemoteText("Primary label".rs)
                }
            }
        }
        val redPixels = painter.countPixels { it.red > 0.7f && it.green < 0.2f && it.blue < 0.2f }
        assertWithMessage(
                "Expected RemoteButton red container to draw when caller uses drawWithContent"
            )
            .that(redPixels)
            .isGreaterThan(100)
    }

    @Test
    fun titleCard_withCallerDrawWithContent_preservesContainerAndPostContentOverlay() {
        val painter = painterRule.paintDocument {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteTitleCard(
                    onClick = RemoteAction.Empty,
                    title = { RemoteText("Title".rs) },
                    modifier =
                        RemoteModifier.drawWithContent {
                            drawContent()
                            drawRect(
                                paint = RemotePaint { color = Color.Green.rc },
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(width, 8f.rf),
                            )
                        },
                    colors = RemoteCardDefaults.cardColors(containerColor = Color.Red.rc),
                ) {
                    RemoteText("Body".rs)
                }
            }
        }
        val redPixels = painter.countPixels { it.red > 0.7f && it.green < 0.2f && it.blue < 0.2f }
        val greenPixels = painter.countPixels { it.green > 0.7f && it.red < 0.2f && it.blue < 0.2f }
        assertWithMessage("Expected RemoteTitleCard red container to draw")
            .that(redPixels)
            .isGreaterThan(100)
        assertWithMessage("Expected caller post-content green overlay strip to draw")
            .that(greenPixels)
            .isGreaterThan(50)
    }
}
