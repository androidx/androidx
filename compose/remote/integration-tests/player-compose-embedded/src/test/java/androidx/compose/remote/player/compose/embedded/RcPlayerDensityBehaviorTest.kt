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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the embedded player honors the document's `Header.DOC_DENSITY_BEHAVIOR`.
 *
 * The document declares a 16-unit padding (stored as a raw float). Rendered at a display density of
 * 2:
 * - DENSITY_BEHAVIOR_DP interprets the value as dp → 16dp inset.
 * - DENSITY_BEHAVIOR_PIXELS / LEGACY interpret it as pixels → 16px = 8dp inset.
 *
 * So the padding inset (inner-left minus outer-left, in dp) is 2× larger under DP than under
 * PIXELS/LEGACY. Density behavior is invisible at density 1 (×density == ÷density), hence the
 * explicit density-2 override.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerDensityBehaviorTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val paddingUnits = 16f
    private val renderDensity = 2f

    /** A 100×100 outer box padded by [paddingUnits], wrapping a 20×20 inner box. */
    private fun documentWith(behavior: Int): CoreDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc =
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(100.rdp)
                                .semantics { contentDescription = "outer".rs }
                                .padding(paddingUnits.rf)
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(20.rdp).semantics {
                                    contentDescription = "inner".rs
                                }
                        )
                    }
                },
            )
        doc.apply { setDensityBehavior(behavior) }
    }

    /** Renders [document] at [renderDensity] and returns the inner box's left inset, in dp. */
    private fun paddingInsetDp(document: CoreDocument): Float {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(renderDensity, 1f)) {
                Box(modifier = Modifier) { RcPlayer(document = document, autoUpdate = false) }
            }
        }
        rule.waitForIdle()
        val outerLeft =
            rule.onNodeWithContentDescription("outer").getUnclippedBoundsInRoot().left.value
        val innerLeft =
            rule.onNodeWithContentDescription("inner").getUnclippedBoundsInRoot().left.value
        return innerLeft - outerLeft
    }

    @Test
    fun dpBehaviorScalesPaddingByDensity() {
        // DP: 16 interpreted as dp → 16dp inset.
        val inset = paddingInsetDp(documentWith(CoreDocument.DENSITY_BEHAVIOR_DP))
        assert(abs(inset - paddingUnits) < 1f) {
            "DP behavior should inset by ${paddingUnits}dp, got ${inset}dp"
        }
    }

    @Test
    fun pixelsBehaviorTreatsPaddingAsPixels() {
        // PIXELS: 16 interpreted as px → 16/2 = 8dp inset at density 2.
        val inset = paddingInsetDp(documentWith(CoreDocument.DENSITY_BEHAVIOR_PIXELS))
        assert(abs(inset - paddingUnits / renderDensity) < 1f) {
            "PIXELS behavior should inset by ${paddingUnits / renderDensity}dp, got ${inset}dp"
        }
    }

    @Test
    fun legacyBehaviorTreatsPaddingAsPixels() {
        // LEGACY (the creation-compose default): padding is px → 8dp inset at density 2
        // (unchanged).
        val inset = paddingInsetDp(documentWith(CoreDocument.DENSITY_BEHAVIOR_LEGACY))
        assert(abs(inset - paddingUnits / renderDensity) < 1f) {
            "LEGACY behavior should inset by ${paddingUnits / renderDensity}dp, got ${inset}dp"
        }
    }

    // --- Spacing: a raw-read value (mSpacedBy) the player scales itself per density behavior. ---

    /**
     * A column with two 20×20 boxes ("a", "b") separated by a raw [paddingUnits] `spacedBy` gap.
     */
    private fun columnWith(behavior: Int): CoreDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc =
            captureRule.captureDocument(
                context = context,
                content = {
                    RemoteColumn(
                        modifier = RemoteModifier.size(100.rdp),
                        verticalArrangement = RemoteArrangement.spacedBy(paddingUnits.rf),
                    ) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(20.rdp).semantics {
                                    contentDescription = "a".rs
                                }
                        )
                        RemoteBox(
                            modifier =
                                RemoteModifier.size(20.rdp).semantics {
                                    contentDescription = "b".rs
                                }
                        )
                    }
                },
            )
        doc.apply { setDensityBehavior(behavior) }
    }

    /** Renders [document] at [renderDensity] and returns the gap between the two boxes, in dp. */
    private fun spacingGapDp(document: CoreDocument): Float {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(renderDensity, 1f)) {
                Box(modifier = Modifier) { RcPlayer(document = document, autoUpdate = false) }
            }
        }
        rule.waitForIdle()
        val aBottom = rule.onNodeWithContentDescription("a").getUnclippedBoundsInRoot().bottom.value
        val bTop = rule.onNodeWithContentDescription("b").getUnclippedBoundsInRoot().top.value
        return bTop - aBottom
    }

    @Test
    fun dpBehaviorScalesSpacingByDensity() {
        // DP: 16 interpreted as dp → 16dp gap.
        val gap = spacingGapDp(columnWith(CoreDocument.DENSITY_BEHAVIOR_DP))
        assert(abs(gap - paddingUnits) < 1f) {
            "DP behavior should space by ${paddingUnits}dp, got ${gap}dp"
        }
    }

    /**
     * Density independence across displays: a `40.rdp` box authored at a creation density of 3 must
     * still render as 40dp when played back at a display density of 2 — i.e. dp resolves at the
     * *playback* density, not the *creation* density.
     */
    @Test
    fun dpResolvesAtPlaybackDensityNotCreationDensity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(300, 300, 480, 1f),
                content = {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(40.rdp).semantics { contentDescription = "box".rs }
                    )
                },
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(renderDensity, 1f)) {
                Box(modifier = Modifier) { RcPlayer(document = document, autoUpdate = false) }
            }
        }
        rule.waitForIdle()

        val size =
            rule.onNodeWithContentDescription("box").getUnclippedBoundsInRoot().let {
                it.right.value - it.left.value
            }
        assert(abs(size - 40f) < 1f) {
            "A 40.rdp box must render as 40dp at any display density (density-independent), got ${size}dp"
        }
    }

    @Test
    fun legacyBehaviorTreatsSpacingAsPixels() {
        // LEGACY: spacing is px → 16/2 = 8dp gap at density 2.
        val gap = spacingGapDp(columnWith(CoreDocument.DENSITY_BEHAVIOR_LEGACY))
        assert(abs(gap - paddingUnits / renderDensity) < 1f) {
            "LEGACY behavior should space by ${paddingUnits / renderDensity}dp, got ${gap}dp"
        }
    }
}
