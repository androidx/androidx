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

package androidx.compose.remote.player.compose.embedded

import androidx.collection.IntSet
import androidx.collection.emptyIntSet
import androidx.collection.intSetOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.PaintOperation
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.serialize.MapSerializer
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.doc.getOperationsList
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperationPolicy
import androidx.compose.remote.player.compose.embedded.draw.UnknownOperations
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [RcPlayerFeature] dispatch, lifecycle and profile coverage. */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerFeatureTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun featurePaintsAnOperationTheBuiltInLoopDoesNotHandle() {
        val document = documentWithFeatureOperation()

        rule.setContent { Player(document, RcPlayerFeatureSet(FillRedFeature())) }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

        assertRed(bitmap, 50, 50)
    }

    @Test
    fun operationIsSkippedWithoutItsFeature() {
        // An operation no feature handles is left to the unknown-operation policy.
        UnknownOperations.policy = UnknownOperationPolicy.Log
        val document = documentWithFeatureOperation()

        rule.setContent { Player(document, RcPlayerFeatureSet.Empty) }
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()

        assertNotRed(bitmap, 50, 50)
    }

    @Test
    fun laterFeatureWithTheSameKeyReplacesTheEarlierOne() {
        val default = FillRedFeature()
        val override = FillRedFeature()
        val other = RecordingFeature()

        val set = RcPlayerFeatureSet(default, other) + RcPlayerFeatureSet(override)

        assertThat(set.features).containsExactly(other, override).inOrder()
    }

    @Test
    fun featureSetsWithTheSameFeaturesAreEqual() {
        val red = FillRedFeature()
        val recording = RecordingFeature()

        assertThat(RcPlayerFeatureSet(red, recording)).isEqualTo(RcPlayerFeatureSet(red, recording))
        assertThat(RcPlayerFeatureSet(red, recording).hashCode())
            .isEqualTo(RcPlayerFeatureSet(red, recording).hashCode())
        assertThat(RcPlayerFeatureSet(red, recording)).isNotEqualTo(RcPlayerFeatureSet(red))
        assertThat(RcPlayerFeatureSet(red)).isNotEqualTo(RcPlayerFeatureSet(FillRedFeature()))
        assertThat(RcPlayerFeatureSet(red, recording))
            .isNotEqualTo(RcPlayerFeatureSet(recording, red))
    }

    @Test
    fun installRunsWhileThePlayerIsComposed() {
        val feature = RecordingFeature()
        var showPlayer by mutableStateOf(true)
        val document = documentWithFeatureOperation()

        rule.setContent { if (showPlayer) Player(document, RcPlayerFeatureSet(feature)) }
        rule.waitForIdle()

        assertThat(feature.installed).isTrue()
        assertThat(feature.sawFeatureOperation).isTrue()

        showPlayer = false
        rule.waitForIdle()

        assertThat(feature.installed).isFalse()
    }

    @Test
    fun builtInProfilesNeedOnlyTheMatrixFeature() {
        val matrix = RcPlayerFeatureSet(MatrixFeature)

        assertThat(matrix.unsupportedOpcodes(RcProfiles.PROFILE_BASELINE)).isEqualTo(emptyIntSet())
        assertThat(matrix.unsupportedOpcodes(RcProfiles.PROFILE_ANDROIDX)).isEqualTo(emptyIntSet())
        // Matrix ops are in the baseline profile, but left to MatrixFeature.
        assertThat(RcPlayerFeatureSet.Empty.unsupportedOpcodes(RcProfiles.PROFILE_ANDROIDX))
            .isEqualTo(MatrixFeature.opcodes)
    }

    @Test
    fun experimentalProfileNeedsFeaturesForItsOperations() {
        val experimental = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL

        val withoutFeatures = RcPlayerFeatureSet.Empty.unsupportedOpcodes(experimental)
        val withMesh =
            RcPlayerFeatureSet(ClaimingFeature(intSetOf(Operations.DRAW_MESH_2D)))
                .unsupportedOpcodes(experimental)

        assertThat(Operations.DRAW_MESH_2D in withoutFeatures).isTrue()
        assertThat(Operations.PLAY_SOUND in withoutFeatures).isTrue()
        // Experimental operations the player still executes built in are never reported.
        assertThat(Operations.LAYOUT_FLOW in withoutFeatures).isFalse()
        assertThat(Operations.DRAW_MESH_2D in withMesh).isFalse()
        assertThat(Operations.PLAY_SOUND in withMesh).isTrue()
    }

    @Test
    fun featureSetSupportsTheProfilesOfItsFeatures() {
        val set =
            RcPlayerFeatureSet(
                ClaimingFeature(emptyIntSet(), profiles = RcProfiles.PROFILE_EXPERIMENTAL)
            )

        assertThat(RcPlayerFeatureSet.Empty.profiles).isEqualTo(RcPlayerFeatureSet.BuiltInProfiles)
        assertThat(set.profiles)
            .isEqualTo(RcPlayerFeatureSet.BuiltInProfiles or RcProfiles.PROFILE_EXPERIMENTAL)
    }

    @Composable
    private fun Player(document: CoreDocument, features: RcPlayerFeatureSet) {
        Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document, features = features) }
    }

    private fun documentWithFeatureOperation(): CoreDocument {
        val document = buildDocument { writer.drawCircle(1f, 1f, 1f) }
        document.getOperationsList().add(FeatureOperation())
        return document
    }

    /** A paint operation only a feature knows how to execute. */
    private class FeatureOperation : PaintOperation() {
        override fun paint(context: PaintContext) {}

        override fun write(buffer: WireBuffer) {}

        override fun deepToString(indent: String): String = "FeatureOperation"

        override fun serialize(serializer: MapSerializer) {}
    }

    /** Fills the canvas red for each [FeatureOperation]. */
    private class FillRedFeature : RcPlayerFeature(KEY) {
        override val opcodes: IntSet = emptyIntSet()

        override fun paint(op: Operation, scope: RcFeatureDrawScope): Boolean {
            if (op !is FeatureOperation) return false
            scope.drawScope.drawRect(Color.Red)
            return true
        }

        companion object {
            val KEY = Key("FillRed")
        }
    }

    /** Records its [Install] lifecycle. */
    private class RecordingFeature : RcPlayerFeature(KEY) {
        override val opcodes: IntSet = emptyIntSet()
        var installed = false
        var sawFeatureOperation = false

        @Composable
        override fun Install(scope: RcFeatureDocumentScope) {
            DisposableEffect(scope) {
                installed = true
                sawFeatureOperation = scope.document.containsOperation { it is FeatureOperation }
                onDispose { installed = false }
            }
        }

        companion object {
            val KEY = Key("Recording")
        }
    }

    private class ClaimingFeature(
        override val opcodes: IntSet,
        override val profiles: Int = RcProfiles.PROFILE_BASELINE,
    ) : RcPlayerFeature(Key("Claiming"))
}
