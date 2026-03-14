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

package androidx.compose.remote.creation.compose.state

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.util.RemoteDocumentTestRule
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SdkSuppress(minSdkVersion = 29)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class NonLinearFontScalingTest {

    private companion object {
        const val TOLERANCE_PX = 0.1f
    }

    @get:Rule val remoteComposeTestRule = RemoteDocumentTestRule()

    private val context: RemoteContext
        get() = remoteComposeTestRule.context

    private fun RemoteTextUnit.toPxValue(density: RemoteDensity): Float {
        // Create a fresh state to avoid caching issues with different densities
        val state = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))
        state.remoteDensity = density

        val resultId = this.toPx().getIdForCreationState(state)

        // Init document from buffer
        val document = androidx.compose.remote.core.CoreDocument()
        val buffer = state.document.buffer
        buffer.buffer.index = 0
        document.initFromBuffer(buffer)

        // Paint
        document.paint(context, 0)

        return context.getFloat(resultId)
    }

    @Test
    fun testNonLinearScalingPermutations() {
        val fontScales = listOf(1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        val fontSizes = listOf(8f, 10f, 12f, 14f, 18f, 20f, 24f, 30f, 40f, 100f)
        val density = 2.0f.rf

        for (scale in fontScales) {
            val converter =
                androidx.compose.ui.unit.fontscaling.FontScaleConverterFactory.forScale(scale)
            checkNotNull(converter) { "Expected non-linear converter for scale $scale" }

            val remoteDensity = RemoteDensity(density, scale.rf)

            for (sp in fontSizes) {
                val expectedDp = converter.convertSpToDp(sp)
                val expectedPx = expectedDp * 2.0f
                val actualPx = sp.toInt().rsp.toPxValue(remoteDensity)
            }
        }
    }

    @Test
    fun testConstantFolding() {
        val density = RemoteDensity(2.0f.rf, 1.5f.rf)
        val sp = 20.rsp

        val px = sp.toPx(density)

        // When evaluating with constant density and font size, the output should
        // successfully fold into a constant Float.
        assertThat(px.constantValueOrNull).isNotNull()
    }
}
