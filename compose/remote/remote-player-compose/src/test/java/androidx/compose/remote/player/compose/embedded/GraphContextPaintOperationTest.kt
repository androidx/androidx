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

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.core.operations.ColorAttribute
import androidx.compose.remote.core.operations.ImageAttribute
import androidx.compose.remote.core.operations.layout.CanvasOperations
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.runtime.mutableFloatStateOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GraphContextPaintOperationTest {

    @Test
    fun colorAttributeEvaluatesThroughPaintContextInGraphContext() {
        val state = SnapshotRemoteComposeState()
        val sourceColorId = 1
        // ARGB: Alpha=255, Red=255, Green=128, Blue=0
        state.overrideColor(sourceColorId, 0xFFFF8000.toInt())

        val redOp = ColorAttribute(10, sourceColorId, ColorAttribute.COLOR_RED)
        val greenOp = ColorAttribute(11, sourceColorId, ColorAttribute.COLOR_GREEN)
        val blueOp = ColorAttribute(12, sourceColorId, ColorAttribute.COLOR_BLUE)
        val alphaOp = ColorAttribute(13, sourceColorId, ColorAttribute.COLOR_ALPHA)

        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(10, redOp)
                put(11, greenOp)
                put(12, blueOp)
                put(13, alphaOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        assertThat(graphContext.getFloat(10)).isWithin(0.01f).of(1.0f)
        assertThat(graphContext.getFloat(11)).isWithin(0.01f).of(128f / 255f)
        assertThat(graphContext.getFloat(12)).isWithin(0.01f).of(0.0f)
        assertThat(graphContext.getFloat(13)).isWithin(0.01f).of(1.0f)
    }

    @Test
    fun imageAttributeEvaluatesBitmapDataDimensions() {
        val state = SnapshotRemoteComposeState()
        val imageId = 7
        val bitmapData =
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG,
                320,
                BitmapData.ENCODING_URL,
                240,
                "camera/current".toByteArray(),
            )
        state.updateObject(imageId, bitmapData)

        val widthOp = ImageAttribute(30, imageId, ImageAttribute.IMAGE_WIDTH, null)
        val heightOp = ImageAttribute(31, imageId, ImageAttribute.IMAGE_HEIGHT, null)

        val computedOps =
            mutableIntObjectMapOf<Operation>().apply {
                put(30, widthOp)
                put(31, heightOp)
            }

        val graphContext =
            GraphContext(
                realState = state,
                computedOps = computedOps,
                timeMillis = mutableFloatStateOf(0f),
                clock = RemoteClock.SYSTEM,
            )

        assertThat(graphContext.getFloat(30)).isEqualTo(320f)
        assertThat(graphContext.getFloat(31)).isEqualTo(240f)
    }

    @Test
    fun resolveBitmapReturnsNullOnUnresolvableUrlWithoutThrowing() {
        val ctx = AndroidRemoteContext()
        val state = SnapshotRemoteComposeState()
        ctx.mRemoteComposeState = state
        val imageId = 9
        val badUrlBitmap =
            BitmapData(
                imageId,
                BitmapData.TYPE_PNG,
                100,
                BitmapData.ENCODING_URL,
                80,
                "camera/current".toByteArray(),
            )
        ctx.putObject(imageId, badUrlBitmap)

        val resolved = resolveBitmap(ctx, imageId)
        assertThat(resolved).isNull()
        // Declared metadata remains intact in the object store.
        assertThat(state.getObject(imageId)).isSameInstanceAs(badUrlBitmap)
    }

    @Test
    fun canvasBitmapDataIsRegisteredDuringApplyOperationsWithoutBitmaps() {
        val buffer = RemoteComposeBuffer()
        val wire = buffer.buffer
        BoxLayout.apply(wire, 1, 0, 0, 0)
        CanvasOperations.apply(wire)
        BitmapData.apply(
            wire,
            42,
            BitmapData.TYPE_PNG_8888,
            64,
            BitmapData.ENCODING_INLINE,
            48,
            ByteArray(0),
        )
        buffer.addContainerEnd()
        buffer.addContainerEnd()

        val document = CoreDocument()
        document.initFromBuffer(buffer)
        val preprocessed = preprocessDocument(document)

        val ctx = initializePlayerRemoteContext(document, RemoteClock.SYSTEM, preprocessed)
        val registered = ctx.mRemoteComposeState.getObject(42) as? BitmapData
        assertThat(registered).isNotNull()
        assertThat(registered!!.width).isEqualTo(64)
        assertThat(registered.height).isEqualTo(48)
    }
}
