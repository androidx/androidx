/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import android.graphics.Color
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.style.UserStyleSetting
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(WatchFaceTestRunner::class)
public class RenderParametersTest {
    @Test
    public fun interactive_base_toWireFormatRoundTrip() {
        val renderParameters = RenderParameters(
            RenderParameters(DrawMode.INTERACTIVE, setOf(WatchFaceLayer.BASE)).toWireFormat()
        )
        assertThat(renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
        assertThat(renderParameters.watchFaceLayers).containsExactly(WatchFaceLayer.BASE)
        assertThat(renderParameters.highlightLayer).isNull()
    }

    @Test
    public fun ambient_twoLayers_toWireFormatRoundTrip() {
        val renderParameters = RenderParameters(
            RenderParameters(
                DrawMode.AMBIENT,
                setOf(
                    WatchFaceLayer.COMPLICATIONS,
                    WatchFaceLayer.COMPLICATIONS_OVERLAY
                )
            ).toWireFormat()
        )
        assertThat(renderParameters.drawMode).isEqualTo(DrawMode.AMBIENT)
        assertThat(renderParameters.watchFaceLayers).containsExactly(
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        assertThat(renderParameters.highlightLayer).isNull()
    }

    @Test
    public fun highlightLayer_AllComplications_toWireFormatRoundTrip() {
        val renderParameters = RenderParameters(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplications,
                    Color.RED,
                    Color.BLACK
                )
            ).toWireFormat()
        )
        assertThat(renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
        assertThat(renderParameters.watchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        val highlightLayer = renderParameters.highlightLayer
        assertThat(highlightLayer!!.highlightTint).isEqualTo(Color.RED)
        assertThat(highlightLayer.backgroundTint).isEqualTo(Color.BLACK)
        assertThat(highlightLayer.highlightedElement).isInstanceOf(
            RenderParameters.HighlightedElement.AllComplications::class.java
        )
    }

    @Test
    public fun highlightLayer_Complication_toWireFormatRoundTrip() {
        val renderParameters = RenderParameters(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.Complication(123),
                    Color.RED,
                    Color.BLACK
                )
            ).toWireFormat()
        )
        assertThat(renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
        assertThat(renderParameters.watchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        val highlightLayer = renderParameters.highlightLayer
        assertThat(highlightLayer!!.highlightTint).isEqualTo(Color.RED)
        assertThat(highlightLayer.backgroundTint).isEqualTo(Color.BLACK)
        assertThat(highlightLayer.highlightedElement).isInstanceOf(
            RenderParameters.HighlightedElement.Complication::class.java
        )
        val complication = highlightLayer.highlightedElement as
            RenderParameters.HighlightedElement.Complication
        assertThat(complication.id).isEqualTo(123)
    }

    @Test
    public fun highlightLayer_UserStyle_toWireFormatRoundTrip() {
        val renderParameters = RenderParameters(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.UserStyle(UserStyleSetting.Id("id-1")),
                    Color.RED,
                    Color.BLACK
                )
            ).toWireFormat()
        )
        assertThat(renderParameters.drawMode).isEqualTo(DrawMode.INTERACTIVE)
        assertThat(renderParameters.watchFaceLayers).containsExactly(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
        val highlightLayer = renderParameters.highlightLayer
        assertThat(highlightLayer!!.highlightTint).isEqualTo(Color.RED)
        assertThat(highlightLayer.backgroundTint).isEqualTo(Color.BLACK)
        assertThat(highlightLayer.highlightedElement).isInstanceOf(
            RenderParameters.HighlightedElement.UserStyle::class.java
        )
        val userStyle = highlightLayer.highlightedElement as
            RenderParameters.HighlightedElement.UserStyle
        assertThat(userStyle.id.value).isEqualTo("id-1")
    }
}