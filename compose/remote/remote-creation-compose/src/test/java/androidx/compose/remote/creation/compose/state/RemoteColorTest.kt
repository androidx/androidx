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
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val referenceColor = RemoteColor.fromARGB(0.1f.rf, 0.25f.rf, 0.5f.rf, 0.75f.rf)

private val referenceHsvColor = RemoteColor.fromHSV(0.75f.rf, 0.5f.rf, 0.25f.rf)

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@SdkSuppress(minSdkVersion = 29)
class RemoteColorTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    @Test
    fun fromHSV() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.fromHSV(h, s, v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val expectedColorInt = AndroidColor.argb(255, 48, 32, 64)
        val expectedColor = AndroidColor.valueOf(expectedColorInt)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(expectedColor.alpha())
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(expectedColor.red())
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(expectedColor.green())
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(expectedColor.blue())
    }

    @Test
    fun fromAHSV() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.fromAHSV(127, h, s, v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val expectedColorInt = AndroidColor.argb(127, 48, 32, 64)
        val expectedColor = AndroidColor.valueOf(expectedColorInt)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(expectedColor.alpha())
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(expectedColor.red())
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(expectedColor.green())
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(expectedColor.blue())
    }

    @Test
    fun fromARGB() {
        val a = 1f.rf
        val r = 0.75f.rf
        val g = 0.5f.rf
        val b = 0.25f.rf
        val result = RemoteColor.fromARGB(a, r, g, b)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(255, 191, 128, 64))
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(1f)
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(0.75f)
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(0.5f)
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(0.25f)
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
        val red = RemoteColor(AndroidColor.RED)
        val green = RemoteColor(AndroidColor.GREEN)
        val result = tween(red, green, RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.RED)
    }

    @Test
    fun tweenTwice() {
        var redCreated = 0

        val color = AndroidColor.RED
        val red =
            RemoteColor(
                constantValueOrNull = null,
                alpha = RemoteFloat(AndroidColor.alpha(color).toFloat() / 255f),
                red = RemoteFloat(AndroidColor.red(color).toFloat() / 255f),
                green = RemoteFloat(AndroidColor.green(color).toFloat() / 255f),
                blue = RemoteFloat(AndroidColor.blue(color).toFloat() / 255f),
            ) { creationState ->
                // This should only be created once
                redCreated++
                creationState.document.addNamedColor("USER:OnPrimaryColor", color)
            }
        val green = RemoteColor(AndroidColor.GREEN)

        val result0 = tween(green, red, RemoteFloat(0.26f))
        val result0Id = result0.getIdForCreationState(creationState)

        val result = tween(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(255, 223, 135, 0))
        assertThat(redCreated).isEqualTo(1)
    }

    @Test
    fun tween0_25() {
        val red = RemoteColor(AndroidColor.RED)
        val green = RemoteColor(AndroidColor.GREEN)
        val result = tween(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(255, 223, 135, 0))
    }

    @Test
    fun tween0_75() {
        val red = RemoteColor(AndroidColor.RED)
        val green = RemoteColor(AndroidColor.GREEN)
        val result = tween(red, green, RemoteFloat(0.75f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(255, 135, 223, 0))
    }

    @Test
    fun tween1() {
        val red = RemoteColor(AndroidColor.RED)
        val green = RemoteColor(AndroidColor.GREEN)
        val result = tween(red, green, RemoteFloat(1f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.GREEN)
    }

    @Test
    fun tweenColorInt() {
        val result = tween(AndroidColor.RED, AndroidColor.GREEN, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(255, 223, 135, 0))
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

        assertThat(context.getColor(resultId)).isEqualTo(AndroidColor.argb(204, 153, 102, 51))
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(0.8f)
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(0.6f)
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(0.4f)
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(0.2f)
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

        assertThat(context.getColor(resultIdAlpha)).isEqualTo(AndroidColor.argb(0, 255, 255, 255))
        assertThat(context.getColor(resultIdRed)).isEqualTo(AndroidColor.argb(255, 0, 255, 255))
        assertThat(context.getColor(resultIdGreen)).isEqualTo(AndroidColor.argb(255, 255, 0, 255))
        assertThat(context.getColor(resultIdBlue)).isEqualTo(AndroidColor.argb(255, 255, 255, 0))
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

        assertThat(result.constantValue.toArgb()).isEqualTo(AndroidColor.argb(204, 153, 102, 51))
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

        assertThat(result.constantValueOrNull).isNull()
    }

    @Test
    fun remoteColorIsCached() {
        val red =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "red", "USER") {
                RemoteColor(Color(AndroidColor.RED))
            }
        val red2 =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "red", "USER") {
                RemoteColor(Color(AndroidColor.RED))
            }
        val green =
            creationState.getOrCreateNamedState(RemoteColor::class.java, "green", "USER") {
                RemoteColor(Color(AndroidColor.GREEN))
            }

        assertThat(red).isSameInstanceAs(red2)
        assertThat(red).isNotSameInstanceAs(green)
    }

    @Test
    fun extensionFunctionMatches() {
        assertThat(Color.Black.rc.constantValue).isEqualTo(Color.Black)
        assertThat(Color.Transparent.rc.constantValue).isEqualTo(Color.Transparent)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
