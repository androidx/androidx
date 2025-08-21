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

@RunWith(RobolectricTestRunner::class)
class RemoteBooleanTest {
    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }

    val creationState =
        RemoteComposeCreationState(AndroidxPlatformServices(), density = 1f, Size(1f, 1f))

    @Test
    fun stringSelect_true() {
        val bool = RemoteBoolean(true)
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun stringSelect_false() {
        val bool = RemoteBoolean(false)
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun floatSelect_true() {
        val bool = RemoteBoolean(true)
        val result = bool.select(RemoteFloat(10f), RemoteFloat(20f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(10f)
        assertThat(result.arrayProvider(creationState).size).isEqualTo(1)
        assertThat(result.arrayProvider(creationState)[0]).isEqualTo(10f)
    }

    @Test
    fun floatSelect_false() {
        val bool = RemoteBoolean(false)
        val result = bool.select(RemoteFloat(10f), RemoteFloat(20f))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getFloat(resultId)).isEqualTo(20f)
        assertThat(result.arrayProvider(creationState).size).isEqualTo(1)
        assertThat(result.arrayProvider(creationState)[0]).isEqualTo(20f)
    }

    @Test
    fun intSelect_true() {
        val bool = RemoteBoolean(true)
        val result = bool.select(RemoteInt(10), RemoteInt(20))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(10)
        assertThat(result.arrayProvider(creationState).size).isEqualTo(1)
        assertThat(result.arrayProvider(creationState)[0]).isEqualTo(10)
    }

    @Test
    fun intSelect_false() {
        val bool = RemoteBoolean(false)
        val result = bool.select(RemoteInt(10), RemoteInt(20))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(20)
        assertThat(result.arrayProvider(creationState).size).isEqualTo(1)
        assertThat(result.arrayProvider(creationState)[0]).isEqualTo(20)
    }

    @Test
    fun booleanSelect_true() {
        val bool = RemoteBoolean(true)
        val result = bool.select(RemoteBoolean(false), RemoteBoolean(true))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(0)
    }

    @Test
    fun booleanSelect_false() {
        val bool = RemoteBoolean(false)
        val result = bool.select(RemoteBoolean(false), RemoteBoolean(true))
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(resultId)).isEqualTo(1)
    }

    @Test
    fun intEqual_true() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(10)
        val bool = v1 eq v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun intEqual_false() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(9)
        val v3 = RemoteInt(11)
        val bool1 = v1 eq v2
        val bool2 = v3 eq v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun intNotEqual_true() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(10)
        val bool = v1 ne v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun intNotEqual_false() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(9)
        val v3 = RemoteInt(11)
        val bool1 = v1 ne v2
        val bool2 = v3 ne v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun intLessThanSelect_true() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(20)
        val bool = v1 lt v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun intLessThanSelect_false() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(20)
        val bool1 = v2 lt v1
        val bool2 = v2 lt v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun intLessThanOrEqualSelect_true() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(20)
        val bool1 = v1 le v2
        val bool2 = v2 le v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun intLessThanOrEqualSelect_false() {
        val v1 = RemoteInt(10)
        val v2 = RemoteInt(20)
        val bool = v2 le v1
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun intGreaterThanSelect_true() {
        val v1 = RemoteInt(20)
        val v2 = RemoteInt(10)
        val bool = v1 gt v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun intGreaterThanSelect_false() {
        val v1 = RemoteInt(20)
        val v2 = RemoteInt(10)
        val bool1 = v2 gt v1
        val bool2 = v2 gt v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun intGreaterThanOrEqualSelect_true() {
        val v1 = RemoteInt(20)
        val v2 = RemoteInt(10)
        val bool1 = v1 ge v2
        val bool2 = v2 ge v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun intGreaterThanOrEqualSelect_false() {
        val v1 = RemoteInt(20)
        val v2 = RemoteInt(10)
        val bool = v2 ge v1
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun floatEqual_true() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(10f)
        val bool = v1 eq v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun floatEqual_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(9f)
        val v3 = RemoteFloat(11f)
        val bool1 = v1 eq v2
        val bool2 = v3 eq v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun floatNotEqual_true() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(10f)
        val bool = v1 ne v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun floatNotEqual_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(9f)
        val v3 = RemoteFloat(11f)
        val bool1 = v1 ne v2
        val bool2 = v3 ne v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun floatLessThanSelect_true() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool = v1 lt v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun floatLessThanSelect_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool1 = v2 lt v1
        val bool2 = v2 lt v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun floatLessThanOrEqualSelect_true() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool1 = v1 le v2
        val bool2 = v2 le v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun floatLessThanOrEqualSelect_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool = v2 le v1
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun floatGreaterThanSelect_true() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool = v1 gt v2
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("true")
    }

    @Test
    fun floatGreaterThanSelect_false() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool1 = v2 gt v1
        val bool2 = v2 gt v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("false")
        assertThat(context.getText(str2Id)).isEqualTo("false")
    }

    @Test
    fun floatGreaterThanOrEqualSelect_true() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool1 = v1 ge v2
        val bool2 = v2 ge v2
        val str1 = bool1.select(RemoteString("true"), RemoteString("false"))
        val str2 = bool2.select(RemoteString("true"), RemoteString("false"))
        val str1Id = str1.getIdForCreationState(creationState)
        val str2Id = str2.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(str1Id)).isEqualTo("true")
        assertThat(context.getText(str2Id)).isEqualTo("true")
    }

    @Test
    fun floatGreaterThanOrEqualSelect_false() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool = v2 ge v1
        val str = bool.select(RemoteString("true"), RemoteString("false"))
        val strId = str.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(strId)).isEqualTo("false")
    }

    @Test
    fun colorIntSelect_true() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool = v1 ge v2
        val color = bool.select(Color.RED, Color.GREEN)
        val colorId = color.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(colorId)).isEqualTo(Color.RED)
    }

    @Test
    fun colorIntSelect_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool = v1 ge v2
        val color = bool.select(Color.RED, Color.GREEN)
        val colorId = color.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(colorId)).isEqualTo(Color.GREEN)
    }

    @Test
    fun colorSelect_true() {
        val v1 = RemoteFloat(20f)
        val v2 = RemoteFloat(10f)
        val bool = v1 ge v2
        val color = bool.select(RemoteColor(Color.RED), RemoteColor(Color.GREEN))
        val colorId = color.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(colorId)).isEqualTo(Color.RED)
    }

    @Test
    fun colorSelect_false() {
        val v1 = RemoteFloat(10f)
        val v2 = RemoteFloat(20f)
        val bool = v1 ge v2
        val color = bool.select(RemoteColor(Color.RED), RemoteColor(Color.GREEN))
        val colorId = color.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getColor(colorId)).isEqualTo(Color.GREEN)
    }

    @Test
    fun or() {
        val b00 = RemoteBoolean(false) or RemoteBoolean(false)
        val b01 = RemoteBoolean(false) or RemoteBoolean(true)
        val b10 = RemoteBoolean(true) or RemoteBoolean(false)
        val b11 = RemoteBoolean(true) or RemoteBoolean(true)
        val result00 = b00.select(RemoteString("true"), RemoteString("false"))
        val result01 = b01.select(RemoteString("true"), RemoteString("false"))
        val result10 = b10.select(RemoteString("true"), RemoteString("false"))
        val result11 = b11.select(RemoteString("true"), RemoteString("false"))
        val result00Id = result00.getIdForCreationState(creationState)
        val result01Id = result01.getIdForCreationState(creationState)
        val result10Id = result10.getIdForCreationState(creationState)
        val result11Id = result11.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result00Id)).isEqualTo("false")
        assertThat(context.getText(result01Id)).isEqualTo("true")
        assertThat(context.getText(result10Id)).isEqualTo("true")
        assertThat(context.getText(result11Id)).isEqualTo("true")
    }

    @Test
    fun and() {
        val b00 = RemoteBoolean(false) and RemoteBoolean(false)
        val b01 = RemoteBoolean(false) and RemoteBoolean(true)
        val b10 = RemoteBoolean(true) and RemoteBoolean(false)
        val b11 = RemoteBoolean(true) and RemoteBoolean(true)
        val result00 = b00.select(RemoteString("true"), RemoteString("false"))
        val result01 = b01.select(RemoteString("true"), RemoteString("false"))
        val result10 = b10.select(RemoteString("true"), RemoteString("false"))
        val result11 = b11.select(RemoteString("true"), RemoteString("false"))
        val result00Id = result00.getIdForCreationState(creationState)
        val result01Id = result01.getIdForCreationState(creationState)
        val result10Id = result10.getIdForCreationState(creationState)
        val result11Id = result11.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result00Id)).isEqualTo("false")
        assertThat(context.getText(result01Id)).isEqualTo("false")
        assertThat(context.getText(result10Id)).isEqualTo("false")
        assertThat(context.getText(result11Id)).isEqualTo("true")
    }

    @Test
    fun xor() {
        val b00 = RemoteBoolean(false) xor RemoteBoolean(false)
        val b01 = RemoteBoolean(false) xor RemoteBoolean(true)
        val b10 = RemoteBoolean(true) xor RemoteBoolean(false)
        val b11 = RemoteBoolean(true) xor RemoteBoolean(true)
        val result00 = b00.select(RemoteString("true"), RemoteString("false"))
        val result01 = b01.select(RemoteString("true"), RemoteString("false"))
        val result10 = b10.select(RemoteString("true"), RemoteString("false"))
        val result11 = b11.select(RemoteString("true"), RemoteString("false"))
        val result00Id = result00.getIdForCreationState(creationState)
        val result01Id = result01.getIdForCreationState(creationState)
        val result10Id = result10.getIdForCreationState(creationState)
        val result11Id = result11.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result00Id)).isEqualTo("false")
        assertThat(context.getText(result01Id)).isEqualTo("true")
        assertThat(context.getText(result10Id)).isEqualTo("true")
        assertThat(context.getText(result11Id)).isEqualTo("false")
    }

    @Test
    fun eq() {
        val b00 = RemoteBoolean(false) eq RemoteBoolean(false)
        val b01 = RemoteBoolean(false) eq RemoteBoolean(true)
        val b10 = RemoteBoolean(true) eq RemoteBoolean(false)
        val b11 = RemoteBoolean(true) eq RemoteBoolean(true)
        val result00 = b00.select(RemoteString("true"), RemoteString("false"))
        val result01 = b01.select(RemoteString("true"), RemoteString("false"))
        val result10 = b10.select(RemoteString("true"), RemoteString("false"))
        val result11 = b11.select(RemoteString("true"), RemoteString("false"))
        val result00Id = result00.getIdForCreationState(creationState)
        val result01Id = result01.getIdForCreationState(creationState)
        val result10Id = result10.getIdForCreationState(creationState)
        val result11Id = result11.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result00Id)).isEqualTo("true")
        assertThat(context.getText(result01Id)).isEqualTo("false")
        assertThat(context.getText(result10Id)).isEqualTo("false")
        assertThat(context.getText(result11Id)).isEqualTo("true")
    }

    @Test
    fun ne() {
        val b00 = RemoteBoolean(false) ne RemoteBoolean(false)
        val b01 = RemoteBoolean(false) ne RemoteBoolean(true)
        val b10 = RemoteBoolean(true) ne RemoteBoolean(false)
        val b11 = RemoteBoolean(true) ne RemoteBoolean(true)
        val result00 = b00.select(RemoteString("true"), RemoteString("false"))
        val result01 = b01.select(RemoteString("true"), RemoteString("false"))
        val result10 = b10.select(RemoteString("true"), RemoteString("false"))
        val result11 = b11.select(RemoteString("true"), RemoteString("false"))
        val result00Id = result00.getIdForCreationState(creationState)
        val result01Id = result01.getIdForCreationState(creationState)
        val result10Id = result10.getIdForCreationState(creationState)
        val result11Id = result11.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result00Id)).isEqualTo("false")
        assertThat(context.getText(result01Id)).isEqualTo("true")
        assertThat(context.getText(result10Id)).isEqualTo("true")
        assertThat(context.getText(result11Id)).isEqualTo("false")
    }

    @Test
    fun not() {
        val b0 = !RemoteBoolean(false)
        val b1 = !RemoteBoolean(true)
        val result0 = b0.select(RemoteString("true"), RemoteString("false"))
        val result1 = b1.select(RemoteString("true"), RemoteString("false"))
        val result0Id = result0.getIdForCreationState(creationState)
        val result2Id = result1.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(result0Id)).isEqualTo("true")
        assertThat(context.getText(result2Id)).isEqualTo("false")
    }

    @Test
    fun toRemoteInt() {
        val b0 = RemoteBoolean(true)
        val b1 = RemoteBoolean(false)
        val i0 = b0.toRemoteInt()
        val i1 = b1.toRemoteInt()
        val i0Id = i0.getIdForCreationState(creationState)
        val i1Id = i1.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(i0Id)).isEqualTo(1)
        assertThat(context.getInteger(i1Id)).isEqualTo(0)
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }
}
