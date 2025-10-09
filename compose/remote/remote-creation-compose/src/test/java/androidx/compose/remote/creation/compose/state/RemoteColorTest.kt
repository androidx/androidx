/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.test.filters.SdkSuppress
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
@SdkSuppress(minSdkVersion = 26)
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

        val expectedColorInt = Color.argb(255, 48, 32, 64)
        val expectedColor = Color.valueOf(expectedColorInt)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isEqualTo(expectedColor.alpha())
        assertThat(result.red.constantValue).isEqualTo(expectedColor.red())
        assertThat(result.green.constantValue).isEqualTo(expectedColor.green())
        assertThat(result.blue.constantValue).isEqualTo(expectedColor.blue())
    }

    @Test
    fun fromAHSV() {
        val h = RemoteFloat(0.75f)
        val s = RemoteFloat(0.5f)
        val v = RemoteFloat(0.25f)
        val result = RemoteColor.fromAHSV(127, h, s, v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val expectedColorInt = Color.argb(127, 48, 32, 64)
        val expectedColor = Color.valueOf(expectedColorInt)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isEqualTo(expectedColor.alpha())
        assertThat(result.red.constantValue).isEqualTo(expectedColor.red())
        assertThat(result.green.constantValue).isEqualTo(expectedColor.green())
        assertThat(result.blue.constantValue).isEqualTo(expectedColor.blue())
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
        assertThat(result.alpha.constantValue).isEqualTo(1f)
        assertThat(result.red.constantValue).isEqualTo(0.75f)
        assertThat(result.green.constantValue).isEqualTo(0.5f)
        assertThat(result.blue.constantValue).isEqualTo(0.25f)
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
    fun tween0() {
        val red = RemoteColor(Color.RED)
        val green = RemoteColor(Color.GREEN)
        val result = tween(red, green, RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.RED)
    }

    @Test
    fun tweenTwice() {
        var redCreated = 0

        val color = Color.RED
        val red =
            RemoteColor(
                constantValue = null,
                RemoteFloat(Color.alpha(color).toFloat() / 255f),
                RemoteFloat(Color.red(color).toFloat() / 255f),
                RemoteFloat(Color.green(color).toFloat() / 255f),
                RemoteFloat(Color.blue(color).toFloat() / 255f),
            ) { creationState ->
                // This should only be created once
                redCreated++
                creationState.document.addNamedColor("USER:OnPrimaryColor", color)
            }
        val green = RemoteColor(Color.GREEN)

        val result0 = tween(green, red, RemoteFloat(0.26f))
        val result0Id = result0.getIdForCreationState(creationState)

        val result = tween(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 223, 135, 0))
        assertThat(redCreated).isEqualTo(1)
    }

    @Test
    fun tween0_25() {
        val red = RemoteColor(Color.RED)
        val green = RemoteColor(Color.GREEN)
        val result = tween(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 223, 135, 0))
    }

    @Test
    fun tween0_75() {
        val red = RemoteColor(Color.RED)
        val green = RemoteColor(Color.GREEN)
        val result = tween(red, green, RemoteFloat(0.75f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 135, 223, 0))
    }

    @Test
    fun tween1() {
        val red = RemoteColor(Color.RED)
        val green = RemoteColor(Color.GREEN)
        val result = tween(red, green, RemoteFloat(1f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.GREEN)
    }

    @Test
    fun tweenColorInt() {
        val result = tween(Color.RED, Color.GREEN, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(255, 223, 135, 0))
    }

    @Test
    fun multiply() {
        val a =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(0.8f),
                RemoteFloat(0.5f),
            )
        val b =
            RemoteColor.fromARGB(
                RemoteFloat(0.8f),
                RemoteFloat(0.75f),
                RemoteFloat(0.5f),
                RemoteFloat(0.4f),
            )
        val result = a * b
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(Color.argb(204, 153, 102, 51))
        assertThat(result.alpha.constantValue).isEqualTo(0.8f)
        assertThat(result.red.constantValue).isEqualTo(0.6f)
        assertThat(result.green.constantValue).isEqualTo(0.4f)
        assertThat(result.blue.constantValue).isEqualTo(0.2f)
    }

    @Test
    fun copy() {
        val a = RemoteColor.fromARGB(1f.rf, 1f.rf, 1f.rf, 1f.rf)
        val aAlpha = a.copy(alpha = 0f.rf)
        val aRed = a.copy(red = 0f.rf)
        val aGreen = a.copy(green = 0f.rf)
        val aBlue = a.copy(blue = 0f.rf)
        val resultIdAlpha = aAlpha.getIdForCreationState(creationState)
        val resultIdRed = aRed.getIdForCreationState(creationState)
        val resultIdGreen = aGreen.getIdForCreationState(creationState)
        val resultIdBlue = aBlue.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultIdAlpha)).isEqualTo(Color.argb(0, 255, 255, 255))
        assertThat(context.getColor(resultIdRed)).isEqualTo(Color.argb(255, 0, 255, 255))
        assertThat(context.getColor(resultIdGreen)).isEqualTo(Color.argb(255, 255, 0, 255))
        assertThat(context.getColor(resultIdBlue)).isEqualTo(Color.argb(255, 255, 255, 0))
    }

    @Test
    fun constantValue_constant() {
        val a =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(0.8f),
                RemoteFloat(0.5f),
            )
        val b =
            RemoteColor.fromARGB(
                RemoteFloat(0.8f),
                RemoteFloat(0.75f),
                RemoteFloat(0.5f),
                RemoteFloat(0.4f),
            )
        val result = a * b

        assertThat(result.constantValue?.toArgb()).isEqualTo(Color.argb(204, 153, 102, 51))
    }

    @Test
    fun constantValue_notConstant() {
        val a =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(0.8f),
                RemoteFloat(0.5f),
            )
        val b =
            RemoteColor.fromARGB(
                RemoteFloat(0.8f),
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                RemoteFloat(0.5f),
                RemoteFloat(0.4f),
            )
        val result = a * b

        assertThat(result.constantValue).isNull()
    }

    @Test
    fun remoteColorIsCached() {
        val red =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "red", "USER") {
                RemoteColor(Color.valueOf(Color.RED))
            }
        val red2 =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "red", "USER") {
                RemoteColor(Color.valueOf(Color.RED))
            }
        val green =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "green", "USER") {
                RemoteColor(Color.valueOf(Color.GREEN))
            }

        assertThat(red).isSameInstanceAs(red2)
        assertThat(red).isNotSameInstanceAs(green)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
