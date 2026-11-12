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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.paint.PaintChangeAdapter
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteTimeTextUnitTest {
    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun defaults_fontVariationSettings_isNull() {
        assertThat(RemoteTimeTextDefaults.FontVariationSettings).isNull()
        assertThat(RemoteTimeTextDefaults.TimeTextStyle.fontVariationSettings).isNull()
    }

    @Test
    fun remoteTimeText_withExplicitFontVariationSettings_capturesSuccessfully() {
        val settings =
            RemoteFontVariation.Settings(
                RemoteFontVariation.weight(700.rf),
                RemoteFontVariation.width(110f.rf),
            )
        val axes = captureTextAxes {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                fontVariationSettings = settings,
            )
        }

        assertThat(axes).containsExactly("wght", 700f, "wdth", 110f, "tnum", 1f).inOrder()
    }

    @Test
    fun remoteTimeText_withStyleFontVariationSettings_capturesSuccessfully() {
        val settings = RemoteFontVariation.Settings(RemoteFontVariation.weight(600.rf))
        val style = RemoteTimeTextDefaults.TimeTextStyle.copy(fontVariationSettings = settings)
        val axes = captureTextAxes {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                style = style,
            )
        }

        assertThat(axes).containsExactly("wght", 600f, "tnum", 1f).inOrder()
    }

    @Test
    fun remoteTimeText_explicitFontVariationSettingsOverridesStyle_capturesSuccessfully() {
        val styleSettings = RemoteFontVariation.Settings(RemoteFontVariation.weight(500.rf))
        val explicitSettings =
            RemoteFontVariation.Settings(
                RemoteFontVariation.weight(800.rf),
                RemoteFontVariation.width(90f.rf),
            )
        val style = RemoteTimeTextDefaults.TimeTextStyle.copy(fontVariationSettings = styleSettings)
        val axes = captureTextAxes {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                style = style,
                fontVariationSettings = explicitSettings,
            )
        }

        assertThat(axes).containsExactly("wght", 800f, "wdth", 90f, "tnum", 1f).inOrder()
    }

    private fun captureTextAxes(
        content: @Composable @RemoteComposable () -> Unit
    ): Map<String, Float> {
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                profile = TestProfiles.androidNativeProfile,
                content = content,
            )
        }
        val remoteContext =
            AndroidRemoteContext().apply {
                useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
            }
        document.initializeContext(remoteContext)

        val operations = document.operations.flatten()
        assertThat(operations.any { it is DrawTextOnCircle }).isTrue()

        val textCircleIndex = operations.indexOfFirst { it is DrawTextOnCircle }
        val paintData =
            operations.subList(0, textCircleIndex).filterIsInstance<PaintData>().lastOrNull()
        val paintContext = checkNotNull(remoteContext.paintContext) { "PaintContext is null" }

        val changes = AxisCapturingPaintChanges()
        paintData?.mPaintData?.applyPaintChange(paintContext, changes)

        val tags = changes.tags ?: return emptyMap()
        val values = changes.values ?: return emptyMap()
        return tags.indices.associate { tags[it] to values[it] }
    }

    private fun List<Operation>.flatten(): List<Operation> = flatMap { operation ->
        if (operation is Container) listOf(operation) + operation.list.flatten()
        else listOf(operation)
    }

    private class AxisCapturingPaintChanges : PaintChangeAdapter() {
        var tags: Array<out String>? = null
        var values: FloatArray? = null

        override fun setFontVariationAxes(tags: Array<out String>, values: FloatArray) {
            this.tags = tags
            this.values = values
        }
    }
}
