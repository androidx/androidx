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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.state.RemoteColor.Companion.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val referenceColor =
    RemoteColor.rgb(red = 0.25f.rf, green = 0.5f.rf, blue = 0.75f.rf, alpha = 0.1f.rf)

private val referenceHsvColor =
    RemoteColor.hsv(hue = 0.75f.rf, saturation = 0.5f.rf, value = 0.25f.rf)

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue

private const val COLOR_RED = 0xFFFF0000.toInt()
private const val COLOR_GREEN = 0xFF00FF00.toInt()
private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
private const val COLOR_BLACK = 0xFF000000.toInt()
private const val COLOR_TRANSPARENT = 0x00000000

@RunWith(JUnit4::class)
class RemoteColorTest {
    val context = TestRemoteContext()
    val creationState = testCreationState()

    @Test
    fun fromHSV_constant() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.hsv(hue = h, saturation = s, value = v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val expectedColorInt = argb(255, 48, 32, 64)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(1f)
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(48f / 255f)
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(32f / 255f)
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(64f / 255f)
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

        val expectedColorInt = argb(255, 48, 32, 64)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(context.getFloat(alphaId)).isWithin(1f / 255f).of(1f)
        assertThat(context.getFloat(redId)).isWithin(1f / 255f).of(48f / 255f)
        assertThat(context.getFloat(greenId)).isWithin(1f / 255f).of(32f / 255f)
        assertThat(context.getFloat(blueId)).isWithin(1f / 255f).of(64f / 255f)
    }

    @Test
    fun fromAHSV() {
        val h = 0.75f.rf
        val s = 0.5f.rf
        val v = 0.25f.rf
        val result = RemoteColor.fromAHSV(alpha = 127, hue = h, saturation = s, value = v)
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        val expectedColorInt = argb(127, 48, 32, 64)
        assertThat(context.getColor(resultId)).isEqualTo(expectedColorInt)
        assertThat(result.alpha.constantValue).isWithin(1f / 255f).of(127f / 255f)
        assertThat(result.red.constantValue).isWithin(1f / 255f).of(48f / 255f)
        assertThat(result.green.constantValue).isWithin(1f / 255f).of(32f / 255f)
        assertThat(result.blue.constantValue).isWithin(1f / 255f).of(64f / 255f)
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

        assertThat(context.getColor(resultId)).isEqualTo(argb(255, 191, 128, 64))
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
        val red = RemoteColor(COLOR_RED)
        val green = RemoteColor(COLOR_GREEN)
        val result = lerp(red, green, RemoteFloat(0f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(COLOR_RED)
    }

    @Test
    fun tweenTwice() {
        var redCreated = 0

        val color = COLOR_RED
        val red =
            RemoteColor(cacheKey = RemoteStateInstanceKey()) { creationState ->
                // This should only be created once
                redCreated++
                creationState.document.addNamedColor("USER:OnPrimaryColor", color)
            }
        val green = RemoteColor(COLOR_GREEN)

        val result0 = lerp(green, red, RemoteFloat(0.26f))
        result0.getIdForCreationState(creationState)

        val result = lerp(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(argb(255, 223, 135, 0))
        assertThat(redCreated).isEqualTo(1)
    }

    @Test
    fun tween0_25() {
        val red = RemoteColor(COLOR_RED)
        val green = RemoteColor(COLOR_GREEN)
        val result = lerp(red, green, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(argb(255, 223, 135, 0))
    }

    @Test
    fun tween0_75() {
        val red = RemoteColor(COLOR_RED)
        val green = RemoteColor(COLOR_GREEN)
        val result = lerp(red, green, RemoteFloat(0.75f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(argb(255, 135, 223, 0))
    }

    @Test
    fun tween1() {
        val red = RemoteColor(COLOR_RED)
        val green = RemoteColor(COLOR_GREEN)
        val result = lerp(red, green, RemoteFloat(1f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(COLOR_GREEN)
    }

    @Test
    fun tweenColorInt() {
        val result = lerp(COLOR_RED, COLOR_GREEN, RemoteFloat(0.25f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(resultId)).isEqualTo(argb(255, 223, 135, 0))
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

        assertThat(context.getColor(resultId)).isEqualTo(argb(204, 153, 102, 51))
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

        assertThat(context.getColor(resultIdAlpha)).isEqualTo(argb(0, 255, 255, 255))
        assertThat(context.getColor(resultIdRed)).isEqualTo(argb(255, 0, 255, 255))
        assertThat(context.getColor(resultIdGreen)).isEqualTo(argb(255, 255, 0, 255))
        assertThat(context.getColor(resultIdBlue)).isEqualTo(argb(255, 255, 255, 0))
    }

    @Test
    fun constantValue_rgb_constant() {
        val r = RemoteFloat(1.0f)
        val g = RemoteFloat(0.5f)
        val b = RemoteFloat(0.0f)
        val a = RemoteFloat(1.0f)
        val color = RemoteColor.rgb(red = r, green = g, blue = b, alpha = a)

        assertThat(color.hasConstantValue).isTrue()
        assertThat(color.constantValue).isEqualTo(argb(255, 255, 128, 0))
    }

    @Test
    fun constantValue_rgb_notConstant() {
        val r = RemoteTimeVariables.continuousSeconds
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
        assertThat(color.constantValue).isEqualTo(COLOR_RED)
    }

    @Test
    fun constantValue_hsv_notConstant() {
        val h = RemoteTimeVariables.continuousSeconds
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
        assertThat(color.constantValue).isEqualTo(COLOR_RED)
    }

    @Test
    fun constantValue_fromAHSV_notConstant() {
        val h = RemoteTimeVariables.continuousSeconds
        val s = RemoteFloat(1.0f)
        val v = RemoteFloat(1.0f)
        val color = RemoteColor.fromAHSV(alpha = 255, hue = h, saturation = s, value = v)

        assertThat(color.hasConstantValue).isFalse()
    }

    @Test
    fun components_constantPropagation() {
        val colorInt = argb(102, 26, 51, 77)
        val remote = RemoteColor(colorInt)

        assertThat(remote.red.constantValue).isWithin(0.01f).of(0.1f)
        assertThat(remote.green.constantValue).isWithin(0.01f).of(0.2f)
        assertThat(remote.blue.constantValue).isWithin(0.01f).of(0.3f)
        assertThat(remote.alpha.constantValue).isWithin(0.01f).of(0.4f)
        assertThat(remote.hue.constantValue).isWithin(0.001f).of(Utils.getHue(colorInt))
        assertThat(remote.saturation.constantValue)
            .isWithin(0.001f)
            .of(Utils.getSaturation(colorInt))
        assertThat(remote.brightness.constantValue)
            .isWithin(0.001f)
            .of(Utils.getBrightness(colorInt))
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

        assertThat(result.constantValue).isEqualTo(argb(204, 153, 102, 51))
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
                red = RemoteTimeVariables.continuousSeconds,
                green = RemoteFloat(0.5f),
                blue = RemoteFloat(0.4f),
                alpha = RemoteFloat(0.8f),
            )
        val result = a * b

        assertThat(result.constantValueOrNull).isNull()
    }

    @Test
    fun remoteColorIsCached() {
        val red = createNamedRemoteColor("red", COLOR_RED)
        val red2 = createNamedRemoteColor("red", COLOR_RED)
        val green = createNamedRemoteColor("green", COLOR_GREEN)

        val redId = red.getIdForCreationState(creationState)
        val red2Id = red2.getIdForCreationState(creationState)
        val greenId = green.getIdForCreationState(creationState)

        assertThat(redId).isEqualTo(red2Id)
        assertThat(redId).isNotEqualTo(greenId)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteColor(COLOR_RED)
        assertThat(constant.cacheKey).isEqualTo(RemoteColorConstantCacheKey(COLOR_RED))
    }

    @Test
    fun colorConstantCacheKeyDoesNotCollideWithInt() {
        val red = COLOR_RED
        val color = RemoteColor(red)
        val intVal = RemoteInt(red)
        assertThat(color.cacheKey).isNotEqualTo(intVal.cacheKey)
        assertThat(color.cacheKey).isInstanceOf(RemoteColorConstantCacheKey::class.java)
        assertThat(intVal.cacheKey).isInstanceOf(RemoteConstantCacheKey::class.java)
    }

    @Test
    fun toDebugString_variable() {
        val c = createNamedRemoteColor("col", COLOR_RED)
        assertThat(c.toDebugString()).isEqualTo("user:col")
    }

    @Test
    fun toDebugString_fromHSV() {
        val x = createNamedRemoteFloat("h", 0.5f)
        val y = createNamedRemoteFloat("s", 0.2f)
        val z = createNamedRemoteFloat("v", 0.1f)
        val hsv = RemoteColor.hsv(hue = x, saturation = y, value = z)
        assertThat(hsv.toDebugString()).isEqualTo("fromHSV(user:h, user:s, user:v)")
    }

    @Test
    fun toDebugString_contextVariable() {
        val continuousSecFloat = RemoteTimeVariables.continuousSeconds
        val colorExpr = RemoteColor.hsv(continuousSecFloat, 1f.rf, 1f.rf)
        assertThat(colorExpr.toDebugString()).isEqualTo("fromHSV(context:continuous_sec, 1.0, 1.0)")
    }

    @Test
    fun toDebugString_constant() {
        val color = RemoteColor(COLOR_RED)
        assertThat(color.toDebugString()).isEqualTo("FFFF0000")
    }

    @Test
    fun times_white_elided() {
        val color = createNamedRemoteColor("color", COLOR_RED)
        val white = RemoteColor(COLOR_WHITE)

        assertThat(color * white).isSameInstanceAs(color)
        assertThat(white * color).isSameInstanceAs(color)
    }

    @Test
    fun times_transparent_elided() {
        val color = createNamedRemoteColor("color", COLOR_RED)
        val transparent = RemoteColor(COLOR_TRANSPARENT)

        assertThat(color * transparent).isSameInstanceAs(transparent)
        assertThat(transparent * color).isSameInstanceAs(transparent)
    }

    @Test
    fun times_black_opaque_elided() {
        val opaqueColor =
            RemoteColor.rgb(red = 0.5f.rf, green = 0.5f.rf, blue = 0.5f.rf, alpha = 1f.rf)
        val black = RemoteColor(COLOR_BLACK)

        assertThat(opaqueColor * black).isSameInstanceAs(black)
        assertThat(black * opaqueColor).isSameInstanceAs(black)
    }

    @Test
    fun times_black_transparent_not_elided() {
        val dynamicAlpha = createNamedRemoteFloat("alpha", 0.5f)
        val transparentColor =
            RemoteColor.rgb(red = 0.5f.rf, green = 0.5f.rf, blue = 0.5f.rf, alpha = dynamicAlpha)
        val black = RemoteColor(COLOR_BLACK)

        val result = transparentColor * black
        assertThat(result).isNotSameInstanceAs(black)
        assertThat(result.alpha).isSameInstanceAs(dynamicAlpha)
        assertThat(result.red.constantValueOrNull).isEqualTo(0f)
        assertThat(result.green.constantValueOrNull).isEqualTo(0f)
        assertThat(result.blue.constantValueOrNull).isEqualTo(0f)
    }

    @Test
    fun tween_same_from_to_elided() {
        val color = createNamedRemoteColor("color", COLOR_RED)
        val tweenFactor = 0.5f.rf

        val tweened = lerp(color, color, tweenFactor)
        assertThat(tweened).isSameInstanceAs(color)
    }

    @Test
    fun tweenInt_same_from_to_elided() {
        val colorInt = COLOR_RED
        val tweenFactor = 0.5f.rf

        val tweened = lerp(colorInt, colorInt, tweenFactor)
        assertThat(tweened.constantValueOrNull).isEqualTo(COLOR_RED)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
