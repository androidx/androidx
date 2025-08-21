/*
 * Copyright (C) 2024 The Android Open Source Project
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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.remote.player.view.platform.AndroidRemoteContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteStringTest {

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(
            AndroidxPlatformServices(),
            density = 1f,
            Size(1f, 1f),
            CoreDocument.DOCUMENT_API_LEVEL,
            Operations.PROFILE_ANDROIDX,
        )
    val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 12.0f)
    val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 12)

    @Test
    fun toRemoteStringWithPostfix() {
        val percentage = RemoteFloat(45.5f)
        val percentageString = percentage.toRemoteString(2, 1) + RemoteString("%")
        val percentageStringId = percentageString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(percentageStringId)).isEqualTo("45.5%")
    }

    @Test
    fun floatIfLessThanTrue() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLT(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(4, 0),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessThanFalse() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfLT(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(4, 0),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfLessThanTrue() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLT(
                v,
                RemoteInt(10000),
                v.toRemoteString(4),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessThanFalse() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfLT(
                v,
                RemoteInt(10000),
                v.toRemoteString(4),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfLessEqualTrue() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLE(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(4, 0),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessEqualFalse() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfLE(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(4, 0),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfLessEqualTrue() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLE(
                v,
                RemoteInt(10000),
                v.toRemoteString(4),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessEqualFalse() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfLE(
                v,
                RemoteInt(10000),
                v.toRemoteString(4),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun floatIfGreaterThanFalse() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGT(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
                v.toRemoteString(4, 0),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterThanTrue() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfGT(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
                v.toRemoteString(4, 0),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfGreaterThanFalse() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGT(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
                v.toRemoteString(4),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterThanTrue() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfGT(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
                v.toRemoteString(4),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfGreaterEqualFalse() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGE(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
                v.toRemoteString(4, 0),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterEqualTrue() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfGE(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(2, 0) + RemoteString("K"),
                v.toRemoteString(4, 0),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfGreaterEqualFalse() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGE(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
                v.toRemoteString(4),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterEqualTrue() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfGE(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(2) + RemoteString("K"),
                v.toRemoteString(4),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun lengthConstString() {
        val str = RemoteString("12345")
        val len = str.length
        val lenId = len.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(lenId)).isEqualTo(5)
    }

    @Test
    fun lengthDynamicString() {
        val str = RemoteString("12345") + RemoteString("678")
        val len = str.length
        val lenId = len.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(lenId)).isEqualTo(8)
    }

    @Test
    fun isEmpty() {
        val a = RemoteString("12345")
        val b = RemoteString("")
        val isAEmpty = a.isEmpty
        val isBEmpty = b.isEmpty
        val isAEmptyStr = isAEmpty.select(RemoteString("true"), RemoteString("false"))
        val isBEmptyStr = isBEmpty.select(RemoteString("true"), RemoteString("false"))
        val isAEmptyStrId = isAEmptyStr.getIdForCreationState(creationState)
        val isBEmptyStrId = isBEmptyStr.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(isAEmptyStrId)).isEqualTo("true")
        assertThat(context.getText(isBEmptyStrId)).isEqualTo("false")
    }

    @Test
    fun isNotEmpty() {
        val a = RemoteString("12345")
        val b = RemoteString("")
        val isANotEmpty = a.isNotEmpty
        val isBNotEmpty = b.isNotEmpty
        val isANotEmptyStr = isANotEmpty.select(RemoteString("true"), RemoteString("false"))
        val isBNotEmptyStr = isBNotEmpty.select(RemoteString("true"), RemoteString("false"))
        val isANotEmptyStrId = isANotEmptyStr.getIdForCreationState(creationState)
        val isBNotEmptyStrId = isBNotEmptyStr.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(isANotEmptyStrId)).isEqualTo("false")
        assertThat(context.getText(isBNotEmptyStrId)).isEqualTo("true")
    }

    @Test
    fun hasConstantValue_true() {
        assertThat(RemoteString("ABC").hasConstantValue).isTrue()
        assertThat(RemoteString("A").plus(RemoteString("B")).hasConstantValue).isTrue()
        assertThat(
                selectIfGT(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
                    .hasConstantValue
            )
            .isTrue()
        assertThat(RemoteString("A").isNotEmpty.hasConstantValue).isTrue()
        assertThat(RemoteString("A").isEmpty.hasConstantValue).isTrue()
        assertThat(RemoteString("A").length.hasConstantValue).isTrue()
    }

    @Test
    fun hasConstantValue_false() {
        val c = creationState
        assertThat(
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC).toRemoteString(2).hasConstantValue
            )
            .isFalse()
        assertThat(
                selectIfGT(
                        RemoteFloat(10f),
                        RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                        RemoteString("A"),
                        RemoteString("B"),
                    )
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun namedRemoteString_initialValue() {
        val namedRemoteString = RemoteString.createNamedRemoteString("testString", "initial")
        val result = namedRemoteString + RemoteString("!")
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("initial!")
    }

    @Test
    fun namedRemoteString_overriddenValue() {
        val namedRemoteString = RemoteString.createNamedRemoteString("testString", "initial")
        val result = namedRemoteString + RemoteString("!")
        val resultId = result.getIdForCreationState(creationState)

        makeAndUpdateCoreDocument { context.setNamedStringOverride("testString", "override") }

        assertThat(context.getText(resultId)).isEqualTo("override!")
    }

    @Test
    fun updateMutableRemoteString() {
        val mutableString = MutableRemoteString(mutableStateOf("hi"))
        mutableString.value = "updated"
        val mutableStringId = mutableString.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(mutableStringId)).isEqualTo("updated")
    }

    @Test
    fun substring() {
        val s = RemoteString("Hello world")
        val result = s.substring(6)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("world")
    }

    @Test
    fun substringWithEnd() {
        val s = RemoteString("Hello world")
        val result = s.substring(1, 5)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("ello")
    }

    @Test
    fun dynamicSubstring() {
        val s = RemoteString("Hello world")
        val result = s.substring(s.length - 4)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("orld")
    }

    @Test
    fun dynamicSubstringWithEnd() {
        val s = RemoteString("Hello world")
        val start = s.length - 6
        val end = s.length - 2
        val result = s.substring(start, end)
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo(" wor")
    }

    @Test
    fun dynamicSubstringWithEnd2() {
        val s = RemoteString("Hello world")
        val result = s.substring(0, RemoteInt(5))
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("Hello")
    }

    @Test
    fun computeRequiredCodePointSet_constantString() {
        val s = RemoteString("Hello")

        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("H", "e", "l", "o")
    }

    @Test
    fun computeRequiredCodePointSet_constantStringUnicode() {
        val s = RemoteString("Hi© Thère®")

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("H", "i", "T", "h", "©", "®", "e", "è", "r", " ")
    }

    @Test
    fun computeRequiredCodePointSet_constantStringAddition() {
        val s = RemoteString("Hello") + RemoteString("World")

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("H", "e", "l", "o", "W", "r", "d")
    }

    @Test
    fun computeRequiredCodePointSet_intToString_padSpace() {
        val s = namedRemoteInt.toRemoteString(2, TextFromFloat.PAD_PRE_SPACE)
        val s2 = RemoteFloat(2f).toRemoteString(2, TextFromFloat.PAD_PRE_SPACE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2", " ")
    }

    @Test
    fun computeRequiredCodePointSet_intToString_padNone() {
        val s = namedRemoteInt.toRemoteString(2, TextFromFloat.PAD_PRE_NONE)
        val s2 = RemoteInt(2).toRemoteString(2, TextFromFloat.PAD_PRE_NONE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padSpace_zeroAfter() {
        val s = namedRemoteFloat.toRemoteString(2, 0, TextFromFloat.PAD_PRE_SPACE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padNone_zeroAfter() {
        val s = namedRemoteFloat.toRemoteString(2, 0, TextFromFloat.PAD_PRE_NONE)
        val s2 = RemoteFloat(2f).toRemoteString(2, 0, TextFromFloat.PAD_PRE_SPACE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly(" ", "2")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padSpace_twoAfter() {
        val s =
            namedRemoteFloat.toRemoteString(
                2,
                2,
                TextFromFloat.PAD_PRE_SPACE + TextFromFloat.PAD_AFTER_SPACE,
            )
        val s2 = RemoteFloat(2f).toRemoteString(2, TextFromFloat.PAD_PRE_SPACE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ", ".")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly(" ", "2")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padNone_twoAfter() {
        val s =
            namedRemoteFloat.toRemoteString(
                2,
                2,
                TextFromFloat.PAD_PRE_NONE + TextFromFloat.PAD_AFTER_NONE,
            )
        val s2 =
            RemoteFloat(2f)
                .toRemoteString(2, 2, TextFromFloat.PAD_PRE_NONE + TextFromFloat.PAD_AFTER_NONE)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2", ".")
    }

    @Test
    fun computeRequiredCodePointSet_intToString_plus_constant() {
        val s = namedRemoteInt.toRemoteString(2, TextFromFloat.PAD_PRE_NONE) + RemoteString("K")
        val s2 = RemoteInt(20).toRemoteString(2, TextFromFloat.PAD_PRE_NONE) + RemoteString("K")

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "K")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2", "0", "K")
    }

    @Test
    fun computeRequiredCodePointSet_namedRemoteString() {
        val namedRemoteString = RemoteString.createNamedRemoteString("testString", "initial")

        assertThat(namedRemoteString.computeRequiredCodePointSet(creationState)).isNull()
    }

    @Test
    fun computeRequiredCodePointSet_namedRemoteString_plus_constant() {
        val namedRemoteString = RemoteString.createNamedRemoteString("testString", "initial")
        val s = namedRemoteString + RemoteString("!")

        assertThat(s.computeRequiredCodePointSet(creationState)).isNull()
    }

    private fun makeAndPaintCoreDocument() =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            paint(context, 0)
        }

    private fun makeAndUpdateCoreDocument(runAfterInit: () -> Unit) =
        CoreDocument().apply {
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
            initializeContext(context)

            runAfterInit()

            for (op in operations) {
                if (op is VariableSupport) {
                    op.updateVariables(context)
                }
                op.apply(context)
            }
        }
}
