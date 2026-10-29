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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies that the layer used to draw a component's content with a group effect (DRAW_CONTENT
 * under a paint alpha, color filter or blend mode) is only allocated when that effect is drawn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerContentLayerTest {
    @get:Rule val rule = RcPlayerTestRule()

    private class CountingGraphicsContext(private val delegate: GraphicsContext) : GraphicsContext {
        var created = 0
        var released = 0

        override fun createGraphicsLayer(): GraphicsLayer =
            delegate.createGraphicsLayer().also { created++ }

        override fun releaseGraphicsLayer(layer: GraphicsLayer) {
            released++
            delegate.releaseGraphicsLayer(layer)
        }
    }

    private fun drawContentDocument(groupAlpha: Float?): CoreDocument {
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        writer.root {
            writer.startBox(
                RecordingModifier().size(100, 100).background(AndroidColor.BLACK).drawContent()
            )
            writer.startCanvasOperations()
            writer.getRcPaint().setColor(AndroidColor.RED).commit()
            writer.drawRect(0f, 0f, 100f, 100f)
            if (groupAlpha != null) writer.getRcPaint().setAlpha(groupAlpha).commit()
            writer.drawComponentContent()
            writer.endCanvasOperations()
            writer.endBox()
        }
        val bytes = writer.buffer()
        return CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(bytes, 0, writer.bufferSize()).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    private fun renderAndRemove(document: CoreDocument): CountingGraphicsContext {
        lateinit var counting: CountingGraphicsContext
        var show by mutableStateOf(true)
        rule.setContent {
            val graphicsContext = LocalGraphicsContext.current
            counting = remember(graphicsContext) { CountingGraphicsContext(graphicsContext) }
            CompositionLocalProvider(LocalGraphicsContext provides counting) {
                if (show) {
                    Box(modifier = Modifier.size(100.dp).testTag("player")) {
                        RcPlayer(document = document)
                    }
                }
            }
        }
        rule.waitForIdle()
        // Force a draw pass.
        rule.onNodeWithTag("player").captureToImage()
        assertThat(counting.released).isEqualTo(0)
        val createdWhileShown = counting.created
        show = false
        rule.waitForIdle()
        assertThat(counting.created).isEqualTo(createdWhileShown)
        return counting
    }

    @Test
    fun drawContentWithoutGroupEffect_allocatesNoLayer() {
        val counting = renderAndRemove(drawContentDocument(groupAlpha = null))

        assertThat(counting.created).isEqualTo(0)
    }

    @Test
    fun drawContentWithGroupAlpha_allocatesLayerAndReleasesIt() {
        val counting = renderAndRemove(drawContentDocument(groupAlpha = 0.5f))

        assertThat(counting.created).isEqualTo(1)
        assertThat(counting.released).isEqualTo(1)
    }
}
