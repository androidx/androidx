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
import androidx.compose.remote.core.operations.Utils
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
import org.robolectric.annotation.Config

private val referenceColor =
    RemoteColor.rgb(red = 0.25f.rf, green = 0.5f.rf, blue = 0.75f.rf, alpha = 0.1f.rf)

private val referenceHsvColor =
    RemoteColor.hsv(hue = 0.75f.rf, saturation = 0.5f.rf, value = 0.25f.rf)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@SdkSuppress(minSdkVersion = 29)
class RemoteColorTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState = RemoteComposeCreationState(AndroidxRcPlatformServices(), Size(1f, 1f))

    @Test
    fun fromHSV_constant() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.hsv(hue = h, saturation = s, value = v)
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
    fun fromHSV_references() {
        val h = 0.75f.rf.createReference()
        val s = 0.5f.rf.createReference()
        val v = 0.25f.rf.createReference()
        val result = RemoteColor.hsv(hue = h, saturation = s, value = v)
        val resultId = result.getIdForCreationState(creationState)

        val alphaId = result.alpha.getIdForCreationState(creationState)
        val redId = result.red.getIdForCreationState(creationState)
        val greenId = result.green.getIdForCreationState(creationState)
        val blueId = result.blue.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        val expectedColorInt = AndroidColor.argb(255, 48, 32, 64)
        val expectedColor = AndroidColor.valueOf(expectedColorInt)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(context.getFloat(alphaId)).isWithin(1f / 255f).of(expectedColor.alpha())
        assertThat(context.getFloat(redId)).isWithin(1f / 255f).of(expectedColor.red())
        assertThat(context.getFloat(greenId)).isWithin(1f / 255f).of(expectedColor.green())
        assertThat(context.getFloat(blueId)).isWithin(1f / 255f).of(expectedColor.blue())
    }

    @Test
    fun fromAHSV() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.fromAHSV(alpha = 127, hue = h, saturation = s, value = v)
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
        val result = RemoteColor.rgb(red = r, green = g, blue = b, alpha = a)
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
                cacheKey = RemoteStateInstanceKey(),
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
            RemoteColor.rgb(
                red = RemoteFloat(0.8f),
                green = RemoteFloat(0.8f),
                blue = RemoteFloat(0.5f),
                alpha = RemoteFloat(1f),
            )
        val b =
            RemoteColor.rgb(
                red = RemoteFloat(0.75f),
                green = RemoteFloat(0.5f),
                blue = RemoteFloat(0.4f),
                alpha = RemoteFloat(0.8f),
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
        val a = RemoteColor.rgb(red = 1f.rf, green = 1f.rf, blue = 1f.rf, alpha = 1f.rf)
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
    fun constantValue_rgb_constant() {
        val r = RemoteFloat(1.0f)
        val g = RemoteFloat(0.5f)
        val b = RemoteFloat(0.0f)
        val a = RemoteFloat(1.0f)
        val color = RemoteColor.rgb(red = r, green = g, blue = b, alpha = a)

        assertThat(color.hasConstantValue).isTrue()
        assertThat(color.constantValue).isEqualTo(Color(1.0f, 0.5f, 0.0f, 1.0f))
    }

    @Test
    fun constantValue_rgb_notConstant() {
        val r = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
        val g = RemoteFloat(0.5f)
        val b = RemoteFloat(0.0f)
        val a = RemoteFloat(1.0f)
        val color = RemoteColor.rgb(red = r, green = g, blue = b, alpha = a)

        assertThat(color.hasConstantValue).isFalse()
    }

    @Test
    fun constantValue_hsv_constant() {
        val h = RemoteFloat(0.0f) // Red
        val s = RemoteFloat(1.0f)
        val v = RemoteFloat(1.0f)
        val color = RemoteColor.hsv(hue = h, saturation = s, value = v)

        assertThat(color.hasConstantValue).isTrue()
        assertThat(color.constantValue).isEqualTo(Color.Red)
    }

    @Test
    fun constantValue_hsv_notConstant() {
        val h = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
        val s = RemoteFloat(1.0f)
        val v = RemoteFloat(1.0f)
        val color = RemoteColor.hsv(hue = h, saturation = s, value = v)

        assertThat(color.hasConstantValue).isFalse()
    }

    @Test
    fun constantValue_fromAHSV_constant() {
        val h = RemoteFloat(0.0f)
        val s = RemoteFloat(1.0f)
        val v = RemoteFloat(1.0f)
        val color = RemoteColor.fromAHSV(alpha = 255, hue = h, saturation = s, value = v)

        assertThat(color.hasConstantValue).isTrue()
        assertThat(color.constantValue).isEqualTo(Color.Red)
    }

    @Test
    fun constantValue_fromAHSV_notConstant() {
        val h = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
        val s = RemoteFloat(1.0f)
        val v = RemoteFloat(1.0f)
        val color = RemoteColor.fromAHSV(alpha = 255, hue = h, saturation = s, value = v)

        assertThat(color.hasConstantValue).isFalse()
    }

    @Test
    fun components_constantPropagation() {
        val color = Color(0.1f, 0.2f, 0.3f, 0.4f)
        val remote = RemoteColor(color)

        assertThat(remote.red.constantValue).isWithin(0.01f).of(0.1f)
        assertThat(remote.green.constantValue).isWithin(0.01f).of(0.2f)
        assertThat(remote.blue.constantValue).isWithin(0.01f).of(0.3f)
        assertThat(remote.alpha.constantValue).isWithin(0.01f).of(0.4f)
        assertThat(remote.hue.constantValue).isWithin(0.001f).of(Utils.getHue(color.toArgb()))
        assertThat(remote.saturation.constantValue)
            .isWithin(0.001f)
            .of(Utils.getSaturation(color.toArgb()))
        assertThat(remote.brightness.constantValue)
            .isWithin(0.001f)
            .of(Utils.getBrightness(color.toArgb()))
    }

    @Test
    fun constantValue_constant() {
        val a =
            RemoteColor.rgb(
                red = RemoteFloat(0.8f),
                green = RemoteFloat(0.8f),
                blue = RemoteFloat(0.5f),
                alpha = RemoteFloat(1f),
            )
        val b =
            RemoteColor.rgb(
                red = RemoteFloat(0.75f),
                green = RemoteFloat(0.5f),
                blue = RemoteFloat(0.4f),
                alpha = RemoteFloat(0.8f),
            )
        val result = a * b

        assertThat(result.constantValue.toArgb()).isEqualTo(AndroidColor.argb(204, 153, 102, 51))
    }

    @Test
    fun constantValue_constant2() {
        val a = RemoteFloat(0.5f)
        val b = RemoteColor(a, RemoteFloat(1f), RemoteFloat(1f), RemoteFloat(1f))

        assertThat(b.hasConstantValue).isTrue()
    }

    @Test
    fun constantValue_notConstant() {
        val a =
            RemoteColor.rgb(
                red = RemoteFloat(0.8f),
                green = RemoteFloat(0.8f),
                blue = RemoteFloat(0.5f),
                alpha = RemoteFloat(1f),
            )
        val b =
            RemoteColor.rgb(
                red = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                green = RemoteFloat(0.5f),
                blue = RemoteFloat(0.4f),
                alpha = RemoteFloat(0.8f),
            )
        val result = a * b

        assertThat(result.constantValueOrNull).isNull()
    }

    @Test
    fun remoteColorIsCached() {
        val red =
            creationState.getOrCreateNamedState(
                RemoteColor::class.java,
                "red",
                RemoteState.Domain.User,
            ) {
                RemoteColor(Color(AndroidColor.RED))
            }
        val red2 =
            creationState.getOrCreateNamedState(
                RemoteColor::class.java,
                "red",
                RemoteState.Domain.User,
            ) {
                RemoteColor(Color(AndroidColor.RED))
            }
        val green =
            creationState.getOrCreateNamedState(
                RemoteColor::class.java,
                "green",
                RemoteState.Domain.User,
            ) {
                RemoteColor(Color(AndroidColor.GREEN))
            }

        assertThat(red).isSameInstanceAs(red2)
        assertThat(red).isNotSameInstanceAs(green)
    }

    @Test
    fun extensionFunctionMatches() {
        assertThat(Color.Black.rc.constantValue.toArgb()).isEqualTo(AndroidColor.BLACK)
        assertThat(Color.Transparent.rc.constantValue.toArgb()).isEqualTo(AndroidColor.TRANSPARENT)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteColor(Color.Red)
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey(Color.Red.toArgb()))
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
