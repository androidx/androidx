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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * End-to-end Robolectric tests creating LTR and RTL documents via `remote-creation-compose` and
 * verifying both layout bounds and text alignment/direction in `RcPlayer` using Compose testing
 * APIs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class LayoutDirectionEmbeddedPlayerTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun displayInfo(
        widthDp: Float = 200f,
        heightDp: Float = 240f,
    ): RemoteCreationDisplayInfo {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        return createRemoteCreationDisplayInfo(
            context = context,
            size = Size(widthDp * density, heightDp * density),
        )
    }

    @Test
    fun creationLtr_playLtr_placesLayoutAndTextLtr() {
        runAndVerifyLtrDocument(playRtl = false)
    }

    @Test
    fun creationLtr_playRtl_placesLayoutAndTextLtr() {
        runAndVerifyLtrDocument(playRtl = true)
    }

    @Test
    fun creationRtl_playLtr_placesLayoutAndTextRtl() {
        runAndVerifyRtlDocument(playRtl = false)
    }

    @Test
    fun creationRtl_playRtl_placesLayoutAndTextRtl() {
        runAndVerifyRtlDocument(playRtl = true)
    }

    @Test
    fun nestedLayoutDirectionOverrides_creationLtr_playRtl() {
        var observedHostDirection: LayoutDirection? = null
        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(widthDp = 200f, heightDp = 180f),
            creationComposableWrapper = ComposableWrappers.noop,
            playComposableWrapper = { content ->
                ComposableWrappers.rtl {
                    observedHostDirection = LocalLayoutDirection.current
                    Box(modifier = Modifier.size(200.dp, 180.dp)) { content() }
                }
            },
        ) {
            NestedLayoutAndTextSample(innerDirection = RemoteLayoutDirection.Rtl)
        }

        // Physical left/right player APIs do not stomp the host's LocalLayoutDirection.
        assertThat(observedHostDirection).isEqualTo(LayoutDirection.Rtl)

        // Outer LTR section: child box and Start text stay on the left.
        val outerBox = rule.onNodeWithContentDescription("outer_box").getUnclippedBoundsInRoot()
        assertThat(outerBox.left.value).isWithin(0.5f).of(0f)
        assertThat(outerBox.right.value).isWithin(0.5f).of(30f)

        val outerText = rule.textLayoutResultOf("Outer Start")
        assertThat(outerText.layoutInput.style.textAlign).isEqualTo(TextAlign.Left)
        assertThat(outerText.layoutInput.style.textDirection).isEqualTo(TextDirection.ContentOrLtr)
        assertThat(outerText.getLineLeft(0)).isLessThan(2f)

        // Inner RTL override section: child box and Start text flip to the right.
        val innerBox = rule.onNodeWithContentDescription("inner_box").getUnclippedBoundsInRoot()
        assertThat(innerBox.left.value).isWithin(0.5f).of(170f)
        assertThat(innerBox.right.value).isWithin(0.5f).of(200f)

        val innerText = rule.textLayoutResultOf("Inner Override")
        assertThat(innerText.layoutInput.style.textAlign).isEqualTo(TextAlign.Right)
        assertThat(innerText.layoutInput.style.textDirection).isEqualTo(TextDirection.ContentOrLtr)
        assertThat(innerText.getLineRight(0)).isWithin(2f).of(innerText.size.width.toFloat())
    }

    @Test
    fun nestedLayoutDirectionOverrides_creationRtl_playLtr() {
        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(widthDp = 200f, heightDp = 180f),
            creationComposableWrapper = ComposableWrappers.rtl,
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(200.dp, 180.dp)) { content() }
            },
        ) {
            NestedLayoutAndTextSample(innerDirection = RemoteLayoutDirection.Ltr)
        }

        // Outer RTL section: child box and Start text stay on the right.
        val outerBox = rule.onNodeWithContentDescription("outer_box").getUnclippedBoundsInRoot()
        assertThat(outerBox.left.value).isWithin(0.5f).of(170f)
        assertThat(outerBox.right.value).isWithin(0.5f).of(200f)

        val outerText = rule.textLayoutResultOf("Outer Start")
        assertThat(outerText.layoutInput.style.textAlign).isEqualTo(TextAlign.Right)
        assertThat(outerText.getLineRight(0)).isWithin(2f).of(outerText.size.width.toFloat())

        // Inner LTR override section: child box and Start text flip back to the left.
        val innerBox = rule.onNodeWithContentDescription("inner_box").getUnclippedBoundsInRoot()
        assertThat(innerBox.left.value).isWithin(0.5f).of(0f)
        assertThat(innerBox.right.value).isWithin(0.5f).of(30f)

        val innerText = rule.textLayoutResultOf("Inner Override")
        assertThat(innerText.layoutInput.style.textAlign).isEqualTo(TextAlign.Left)
        assertThat(innerText.getLineLeft(0)).isLessThan(2f)
    }

    private fun runAndVerifyLtrDocument(playRtl: Boolean) {
        setSampleContent(createRtl = false, playRtl = playRtl)

        // Asymmetric padding (start=16dp, end=4dp) puts content in [16..196].
        val firstBounds = rule.onNodeWithContentDescription("row_first").getUnclippedBoundsInRoot()
        val secondBounds =
            rule.onNodeWithContentDescription("row_second").getUnclippedBoundsInRoot()
        assertThat(firstBounds.left.value).isWithin(0.5f).of(16f)
        assertThat(firstBounds.right.value).isWithin(0.5f).of(56f)
        assertThat(secondBounds.left.value).isWithin(0.5f).of(56f)
        assertThat(secondBounds.right.value).isWithin(0.5f).of(76f)

        // SpacedBy(8dp, Alignment.End) anchors children at the right edge (196dp) in compose order.
        val spacedA = rule.onNodeWithContentDescription("spaced_a").getUnclippedBoundsInRoot()
        val spacedB = rule.onNodeWithContentDescription("spaced_b").getUnclippedBoundsInRoot()
        assertThat(spacedA.left.value).isWithin(0.5f).of(158f)
        assertThat(spacedB.left.value).isWithin(0.5f).of(186f)
        assertThat(spacedB.right.value).isWithin(0.5f).of(196f)

        val boxBounds =
            rule.onNodeWithContentDescription("center_start_box").getUnclippedBoundsInRoot()
        assertThat(boxBounds.left.value).isWithin(0.5f).of(16f)
        assertThat(boxBounds.right.value).isWithin(0.5f).of(46f)

        // Text alignments in an LTR-created document stay aligned to left/right regardless of host.
        assertTextAlignedLeft("Start aligned", TextAlign.Left)
        assertTextAlignedRight("End aligned", TextAlign.Right)
        assertTextAlignedLeft("Left aligned", TextAlign.Left)
        assertTextAlignedRight("Right aligned", TextAlign.Right)
        assertTextAlignedLeft("שלום World!", TextAlign.Left)
    }

    private fun runAndVerifyRtlDocument(playRtl: Boolean) {
        setSampleContent(createRtl = true, playRtl = playRtl)

        // Asymmetric padding (start=16dp, end=4dp) resolves in RTL to left=4dp, right=16dp ->
        // [4..184].
        val firstBounds = rule.onNodeWithContentDescription("row_first").getUnclippedBoundsInRoot()
        val secondBounds =
            rule.onNodeWithContentDescription("row_second").getUnclippedBoundsInRoot()
        assertThat(firstBounds.right.value).isWithin(0.5f).of(184f)
        assertThat(firstBounds.left.value).isWithin(0.5f).of(144f)
        assertThat(secondBounds.right.value).isWithin(0.5f).of(144f)
        assertThat(secondBounds.left.value).isWithin(0.5f).of(124f)

        // SpacedBy(8dp, Alignment.End) in RTL resolves End to physical left edge (4dp), reversing
        // child order.
        val spacedA = rule.onNodeWithContentDescription("spaced_a").getUnclippedBoundsInRoot()
        val spacedB = rule.onNodeWithContentDescription("spaced_b").getUnclippedBoundsInRoot()
        assertThat(spacedB.left.value).isWithin(0.5f).of(4f)
        assertThat(spacedB.right.value).isWithin(0.5f).of(14f)
        assertThat(spacedA.left.value).isWithin(0.5f).of(22f)
        assertThat(spacedA.right.value).isWithin(0.5f).of(42f)

        val boxBounds =
            rule.onNodeWithContentDescription("center_start_box").getUnclippedBoundsInRoot()
        assertThat(boxBounds.right.value).isWithin(0.5f).of(184f)
        assertThat(boxBounds.left.value).isWithin(0.5f).of(154f)

        // Text alignments resolve RemoteTextAlign.Start/End against creation direction, while
        // Left/Right stay physical.
        assertTextAlignedRight("Start aligned", TextAlign.Right)
        assertTextAlignedLeft("End aligned", TextAlign.Left)
        assertTextAlignedLeft("Left aligned", TextAlign.Left)
        assertTextAlignedRight("Right aligned", TextAlign.Right)
        assertTextAlignedRight("שלום World!", TextAlign.Right)
    }

    private fun setSampleContent(createRtl: Boolean, playRtl: Boolean) {
        val display = displayInfo()
        rule.setRemoteContent(
            remoteCreationDisplayInfo = display,
            creationComposableWrapper =
                if (createRtl) ComposableWrappers.rtl else ComposableWrappers.noop,
            playComposableWrapper = { content ->
                val hostWrapper = if (playRtl) ComposableWrappers.rtl else ComposableWrappers.noop
                hostWrapper { Box(modifier = Modifier.size(200.dp, 240.dp)) { content() } }
            },
        ) {
            LayoutAndTextSample()
        }
    }

    private fun assertTextAlignedLeft(text: String, expectedStyleAlign: TextAlign) {
        val result = rule.textLayoutResultOf(text)
        assertThat(result.layoutInput.style.textAlign).isEqualTo(expectedStyleAlign)
        assertThat(result.layoutInput.style.textDirection).isEqualTo(TextDirection.ContentOrLtr)
        assertThat(result.getLineLeft(0)).isLessThan(2f)
    }

    private fun assertTextAlignedRight(text: String, expectedStyleAlign: TextAlign) {
        val result = rule.textLayoutResultOf(text)
        assertThat(result.layoutInput.style.textAlign).isEqualTo(expectedStyleAlign)
        assertThat(result.layoutInput.style.textDirection).isEqualTo(TextDirection.ContentOrLtr)
        assertThat(result.getLineRight(0)).isWithin(2f).of(result.size.width.toFloat())
    }

    private fun ComposeContentTestRule.textLayoutResultOf(text: String): TextLayoutResult {
        val node = onNodeWithText(text).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    @Composable
    @RemoteComposable
    private fun LayoutAndTextSample() {
        RemoteColumn(
            modifier =
                RemoteModifier.fillMaxSize()
                    .padding(start = 16.rdp, top = 8.rdp, end = 4.rdp, bottom = 8.rdp)
        ) {
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth(),
                horizontalArrangement = RemoteArrangement.Start,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(40.rdp, 20.rdp).background(Color.Red.rc).semantics {
                            contentDescription = "row_first".rs
                        }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(20.rdp, 20.rdp).background(Color.Blue.rc).semantics {
                            contentDescription = "row_second".rs
                        }
                )
            }
            RemoteRow(
                modifier = RemoteModifier.fillMaxWidth(),
                horizontalArrangement = RemoteArrangement.spacedBy(8.rdp, RemoteAlignment.End),
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(20.rdp, 20.rdp).background(Color.Yellow.rc).semantics {
                            contentDescription = "spaced_a".rs
                        }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.size(10.rdp, 20.rdp).background(Color.Magenta.rc).semantics {
                            contentDescription = "spaced_b".rs
                        }
                )
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxWidth().height(24.rdp),
                contentAlignment = RemoteAlignment.CenterStart,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(30.rdp, 16.rdp).background(Color.Cyan.rc).semantics {
                            contentDescription = "center_start_box".rs
                        }
                )
            }
            RemoteText(
                text = "Start aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
            RemoteText(
                text = "End aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.End,
            )
            RemoteText(
                text = "Left aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Left,
            )
            RemoteText(
                text = "Right aligned".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Right,
            )
            RemoteText(
                text = "שלום World!".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun NestedLayoutAndTextSample(innerDirection: RemoteLayoutDirection) {
        RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
            RemoteBox(
                modifier = RemoteModifier.fillMaxWidth().height(24.rdp),
                contentAlignment = RemoteAlignment.CenterStart,
            ) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(30.rdp, 16.rdp).background(Color.Red.rc).semantics {
                            contentDescription = "outer_box".rs
                        }
                )
            }
            RemoteText(
                text = "Outer Start".rs,
                modifier = RemoteModifier.fillMaxWidth(),
                fontSize = 14.rsp,
                textAlign = RemoteTextAlign.Start,
            )
            CompositionLocalProvider(LocalRemoteLayoutDirection provides innerDirection) {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(24.rdp),
                    contentAlignment = RemoteAlignment.CenterStart,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(30.rdp, 16.rdp)
                                .background(Color.Blue.rc)
                                .semantics { contentDescription = "inner_box".rs }
                    )
                }
                RemoteText(
                    text = "Inner Override".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    textAlign = RemoteTextAlign.Start,
                )
            }
        }
    }
}
