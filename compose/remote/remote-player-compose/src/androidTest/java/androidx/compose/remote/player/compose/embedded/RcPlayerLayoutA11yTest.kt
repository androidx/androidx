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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.rememberRemoteScrollState
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.verticalScroll
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device layout + accessibility tests for the embedded [RcPlayer], driven with Compose UI
 * testing (Espresso-style semantics queries — no screenshots). These complement the Robolectric
 * unit tests by exercising the real device layout/measure + accessibility pipeline.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RcPlayerLayoutA11yTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private fun captureDocument(content: @Composable @RemoteComposable () -> Unit): CoreDocument {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return runBlocking { captureRule.captureDocument(context, content = content) }
    }

    /** The document's root content description must surface as a semantics label on the player. */
    @Test
    fun rootContentDescriptionLabelsThePlayer() {
        val document = captureDocument { RemoteBox(modifier = RemoteModifier.size(100.rdp)) }
        document.setContentDescription("a weather card")

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("a weather card").assertIsDisplayed()
    }

    /** A semantics contentDescription authored on a component is queryable on device. */
    @Test
    fun componentSemanticsAreExposed() {
        val document = captureDocument {
            RemoteBox(
                modifier =
                    RemoteModifier.size(80.rdp).semantics { contentDescription = "inner box".rs }
            )
        }

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("inner box").assertIsDisplayed()
    }

    /**
     * Swiping a scrollable area publishes the live offset to the document's scroll-position
     * variable (a marker whose width is bound to that variable widens) — the device counterpart of
     * the Robolectric scroll-position test.
     */
    @Test
    fun scrollPublishesPositionToBoundVariable() {
        val document = captureDocument {
            val scrollState = rememberRemoteScrollState()
            RemoteColumn(modifier = RemoteModifier.size(200.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(100.rdp).verticalScroll(scrollState).semantics {
                            contentDescription = "scroller".rs
                        }
                ) {
                    RemoteColumn { repeat(5) { RemoteBox(modifier = RemoteModifier.size(80.rdp)) } }
                }
                RemoteBox(
                    modifier =
                        RemoteModifier.width(scrollState.positionState).height(10.rdp).semantics {
                            contentDescription = "marker".rs
                        }
                )
            }
        }

        rule.setContent { Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        fun markerWidth() =
            rule.onNodeWithContentDescription("marker").getUnclippedBoundsInRoot().let {
                it.right.value - it.left.value
            }

        val before = markerWidth()
        rule.onNodeWithContentDescription("scroller").performTouchInput { swipeUp() }
        rule.waitForIdle()
        val after = markerWidth()

        assert(after > before + 1f) {
            "Scrolling should publish the offset to the bound variable; before=$before after=$after"
        }
    }
}
