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

import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteTextMeasurerTest {

    private val density = RemoteDensity(2.0f.rf, 1.0f.rf)

    @Test
    fun constructor_initializesDensity() {
        val measurer = RemoteTextMeasurer(density = density)

        assertThat(measurer.density).isEqualTo(density)
    }

    @Test
    fun measure_returnsRemoteTextLayoutResult() {
        val measurer = RemoteTextMeasurer(density = density)
        val result = measurer.measure("Hello".rs)

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
        assertThat(result.width).isNotNull()
        assertThat(result.height).isNotNull()
    }

    @Test
    fun measureWidth_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val width = measurer.measureWidth("Hello".rs)

        assertThat(width).isNotNull()
    }

    @Test
    fun measureHeight_returnsRemoteFloat() {
        val measurer = RemoteTextMeasurer(density = density)
        val height = measurer.measureHeight("Hello".rs)

        assertThat(height).isNotNull()
    }

    @Test
    fun measure_withCustomStyle() {
        val customStyle =
            RemoteTextStyle(
                fontSize = 24.rsp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
        val measurer = RemoteTextMeasurer(density = density)

        val result =
            measurer.measure(
                text = "Hello World".rs,
                style = customStyle,
                fontSize = 20.rsp,
            )

        assertThat(result.size.width).isNotNull()
        assertThat(result.size.height).isNotNull()
        assertThat(result.width).isNotNull()
        assertThat(result.height).isNotNull()
    }

    @Test
    fun measure_withStyleFontSize() {
        val measurer = RemoteTextMeasurer(density = density)
        val style = RemoteTextStyle(fontSize = 18.rsp)

        val result = measurer.measure("Hello".rs, style = style)

        assertThat(result.width).isNotNull()
        assertThat(result.height).isNotNull()
    }

    @Test
    fun measure_withFontSizeOverride_takesPrecedence() {
        val measurer = RemoteTextMeasurer(density = density)
        val style = RemoteTextStyle(fontSize = 18.rsp)

        val result = measurer.measure("Hello".rs, style = style, fontSize = 24.rsp)

        assertThat(result.width).isNotNull()
        assertThat(result.height).isNotNull()
    }
}
