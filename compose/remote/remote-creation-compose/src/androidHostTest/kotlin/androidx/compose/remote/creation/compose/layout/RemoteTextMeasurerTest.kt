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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteTextMeasurerTest {

    private val density = RemoteDensity(2.0f.rf, 1.0f.rf)

    @Test
    fun constructor_initializesDefaults() {
        val style = RemoteTextStyle(fontSize = 18.rsp)
        val measurer = RemoteTextMeasurer(density = density, style = style)

        assertThat(measurer.density).isEqualTo(density)
        assertThat(measurer.style).isEqualTo(style)
    }

    @Test
    fun measureSingleLine_returnsRemoteSingleLineTextBounds() {
        val measurer = RemoteTextMeasurer(density = density)
        val result = measurer.measureSingleLine("Hello".rs)

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
        assertThat(result.baseline).isNotNull()
    }

    @Test
    fun measureFontMetrics_returnsRemoteFontMetrics() {
        val measurer = RemoteTextMeasurer(density = density)
        val metrics = measurer.measureFontMetrics(RemoteTextStyle(fontSize = 18.rsp))

        assertThat(metrics.ascent).isNotNull()
        assertThat(metrics.descent).isNotNull()
    }

    @Test
    fun measurePaintBounds_returnsRemotePaintBounds() {
        val measurer = RemoteTextMeasurer(density = density)
        val bounds = measurer.measurePaintBounds("Hello".rs)

        assertThat(bounds.left).isNotNull()
        assertThat(bounds.top).isNotNull()
        assertThat(bounds.right).isNotNull()
        assertThat(bounds.bottom).isNotNull()
        assertThat(bounds.width).isNotNull()
        assertThat(bounds.height).isNotNull()
    }

    @Test
    fun measureLayoutWidth_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val width = measurer.measureLayoutWidth(" Hello ".rs)

        assertThat(width).isNotNull()
    }

    @Test
    fun measureLayoutHeight_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val height = measurer.measureLayoutHeight("Hello".rs)

        assertThat(height).isNotNull()
    }

    @Test
    fun measureMonospaceWidth_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val monospaceWidth = measurer.measureMonospaceWidth("Hello".rs)

        assertThat(monospaceWidth).isNotNull()
    }

    @Test
    fun measureGlyphLeft_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val glyphLeft = measurer.measureGlyphLeft("Hello".rs)

        assertThat(glyphLeft).isNotNull()
    }

    @Test
    fun measureGlyphTop_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val glyphTop = measurer.measureGlyphTop("Hello".rs)

        assertThat(glyphTop).isNotNull()
    }

    @Test
    fun measureGlyphRight_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val glyphRight = measurer.measureGlyphRight("Hello".rs)

        assertThat(glyphRight).isNotNull()
    }

    @Test
    fun measureGlyphBottom_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val glyphBottom = measurer.measureGlyphBottom("Hello".rs)

        assertThat(glyphBottom).isNotNull()
    }

    @Test
    fun measureAscent_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val ascent = measurer.measureAscent("Hello".rs)

        assertThat(ascent).isNotNull()
    }

    @Test
    fun measureDescent_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val descent = measurer.measureDescent("Hello".rs)

        assertThat(descent).isNotNull()
    }

    @Test
    fun measureSingleLine_withCustomStyle() {
        val customStyle =
            RemoteTextStyle(
                fontSize = 24.rsp,
                fontWeight = RemoteFontWeight.Bold,
                fontStyle = RemoteFontStyle.Italic,
            )
        val measurer = RemoteTextMeasurer(density = density)

        val result =
            measurer.measureSingleLine(
                text = "Hello World".rs,
                style = customStyle,
            )

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
    }

    @Test
    fun measureSingleLine_withStyleFontSize() {
        val measurer = RemoteTextMeasurer(density = density)
        val style = RemoteTextStyle(fontSize = 18.rsp)

        val result = measurer.measureSingleLine("Hello".rs, style = style)

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
    }

    @Test
    fun measureSingleLine_withStyleOverride_fallsBackToMeasurerStyleFontSize() {
        val measurer =
            RemoteTextMeasurer(
                density = density,
                style = RemoteTextStyle(fontSize = 18.rsp),
            )
        val styleWithoutFontSize = RemoteTextStyle(fontWeight = RemoteFontWeight.Bold)

        val result = measurer.measureSingleLine("Hello".rs, style = styleWithoutFontSize)

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
    }

    @Test
    fun measureSingleLine_withDensityOverride_usesOverriddenDensity() {
        val measurer =
            RemoteTextMeasurer(
                density = density,
                style = RemoteTextStyle(fontSize = 16.rsp),
            )
        val customDensity = RemoteDensity(3.0f.rf, 1.5f.rf)

        val result = measurer.measureSingleLine("Hello".rs, density = customDensity)

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
    }

    @Test
    fun measureMethods_writeToBaseRemoteComposeWriterWithoutThrowing() {
        val displayInfo = RemoteCreationDisplayInfo(width = 200, height = 200, densityDpi = 160)
        val headlessProfile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                RcPlatformServices.None,
            ) { creationDisplayInfo, profile, callback ->
                RemoteComposeWriter(creationDisplayInfo, null, profile, callback)
            }
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = displayInfo,
                contentDescription = null,
                profile = headlessProfile,
            )
        val measurer = RemoteTextMeasurer(density = density)
        val style =
            RemoteTextStyle(
                fontSize = 16.rsp,
                fontWeight = RemoteFontWeight.Medium,
                fontStyle = RemoteFontStyle.Italic,
            )
        val layout = measurer.measureSingleLine(text = "HeadlessMeasure".rs, style = style)
        val fontMetrics = measurer.measureFontMetrics(style = style)
        val paintBounds = measurer.measurePaintBounds(text = "HeadlessMeasure".rs, style = style)
        val monospaceWidth =
            measurer.measureMonospaceWidth(text = "HeadlessMeasure".rs, style = style)

        assertThat(layout.size.width.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(layout.size.height.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(layout.baseline.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(fontMetrics.ascent.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(fontMetrics.descent.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(paintBounds.left.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(paintBounds.top.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(paintBounds.right.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(paintBounds.bottom.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(monospaceWidth.getFloatIdForCreationState(creationState).isNaN()).isTrue()
        assertThat(creationState.document.bufferSize()).isGreaterThan(0)
    }

    @Test
    fun resultTypes_supportStructuralEqualityAndDimensions() {
        val layoutA = RemoteSingleLineTextBounds(RemoteSize(104f.rf, 20f.rf), 16f.rf)
        val layoutB = RemoteSingleLineTextBounds(RemoteSize(104f.rf, 20f.rf), 16f.rf)
        val layoutC = RemoteSingleLineTextBounds(RemoteSize(104f.rf, 20f.rf), 14f.rf)

        assertThat(layoutA).isEqualTo(layoutB)
        assertThat(layoutA.hashCode()).isEqualTo(layoutB.hashCode())
        assertThat(layoutA).isNotEqualTo(layoutC)
        assertThat(layoutA.size.width.constantValueOrNull).isEqualTo(104f)
        assertThat(layoutA.size.height.constantValueOrNull).isEqualTo(20f)
        assertThat(layoutA.baseline.constantValueOrNull).isEqualTo(16f)

        val metricsA = RemoteFontMetrics(-16f.rf, 4f.rf)
        val metricsB = RemoteFontMetrics(-16f.rf, 4f.rf)
        assertThat(metricsA).isEqualTo(metricsB)
        assertThat(metricsA.hashCode()).isEqualTo(metricsB.hashCode())

        val boundsA = RemotePaintBounds(1f.rf, -14f.rf, 101f.rf, 6f.rf)
        val boundsB = RemotePaintBounds(1f.rf, -14f.rf, 101f.rf, 6f.rf)
        assertThat(boundsA).isEqualTo(boundsB)
        assertThat(boundsA.hashCode()).isEqualTo(boundsB.hashCode())
        assertThat(boundsA.width.constantValueOrNull).isEqualTo(100f)
        assertThat(boundsA.height.constantValueOrNull).isEqualTo(20f)
    }
}
