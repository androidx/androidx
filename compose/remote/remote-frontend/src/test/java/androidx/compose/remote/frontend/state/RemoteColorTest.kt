/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.frontend.state

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val referenceColor =
    RemoteColor.fromARGB(
        RemoteFloat(0.1f),
        RemoteFloat(0.25f),
        RemoteFloat(0.5f),
        RemoteFloat(0.75f),
    )

private val referenceHsvColor =
    RemoteColor.fromHSV(RemoteFloat(0.75f), RemoteFloat(0.5f), RemoteFloat(0.25f))

@RunWith(RobolectricTestRunner::class)
class RemoteColorTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    @Test
    fun fromHSV() {
        val h = RemoteFloat(0.75f)
        val s = RemoteFloat(0.5f)
        val v = RemoteFloat(0.25f)
        val result = RemoteColor.fromHSV(h, s, v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 48, 32, 64))
    }

    @Test
    fun fromAHSV() {
        val h = RemoteFloat(0.75f)
        val s = RemoteFloat(0.5f)
        val v = RemoteFloat(0.25f)
        val result = RemoteColor.fromAHSV(127, h, s, v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(127, 48, 32, 64))
    }

    @Test
    fun fromARGB() {
        val a = RemoteFloat(1f)
        val r = RemoteFloat(0.75f)
        val g = RemoteFloat(0.5f)
        val b = RemoteFloat(0.25f)
        val result = RemoteColor.fromARGB(a, r, g, b)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 191, 128, 64))
    }

    @Test
    fun red() {
        val result = referenceColor.red
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.25f)
    }

    @Test
    fun green() {
        val result = referenceColor.green
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.5f)
    }

    @Test
    fun blue() {
        val result = referenceColor.blue
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.75f)
    }

    @Test
    fun alpha() {
        val result = referenceColor.alpha
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.1f)
    }

    @Test
    fun hue() {
        val result = referenceHsvColor.hue
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.75f)
    }

    @Test
    fun saturation() {
        val result = referenceHsvColor.saturation
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.5f)
    }

    @Test
    fun brightness() {
        val result = referenceHsvColor.brightness
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isWithin(1f / 255f).of(0.25f)
    }

    @Test
    fun tween() {
        val red = RemoteColor(Color.RED)
        val green = RemoteColor(Color.GREEN)
        val result = tween(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 223, 135, 0))
    }

    @Test
    fun tweenColorInt() {
        val result = tween(Color.RED, Color.GREEN, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 223, 135, 0))
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
