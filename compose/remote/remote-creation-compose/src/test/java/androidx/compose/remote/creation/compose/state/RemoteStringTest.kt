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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.VariableSupport
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import java.text.DecimalFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteStringTest {

    val context =
        AndroidRemoteContext().apply {
            useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        }
    val creationState =
        RemoteComposeCreationState(
            AndroidxRcPlatformServices(),
            Size(1f, 1f),
            CoreDocument.DOCUMENT_API_LEVEL,
            PROFILE_ANDROIDX,
        )
    val namedRemoteFloat = RemoteFloat.createNamedRemoteFloat("testFloat", 12.0f)
    val namedRemoteInt = RemoteInt.createNamedRemoteInt("testInt", 12)

    @Test
    fun toRemoteStringWithPostfix() {
        val percentage = RemoteFloat(45.5f)
        val percentageString = percentage.toRemoteString(DecimalFormat("#0.0")) + RemoteString("%")
        val percentageStringId = percentageString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(percentageStringId)).isEqualTo("45.5%")
    }

    @Test
    fun floatIfLessThan_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("#0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessThan_equal() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(12345f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfLessThan_greater() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfLt(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfLessThan_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessThan_equal() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(12345),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfLessThan_greater() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfLt(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfLessEqual_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(10000f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessEqual_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(1234f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfLessEqual_greater() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfLe(
                v,
                RemoteFloat(9999f),
                v.toRemoteString(DecimalFormat("###0")),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfLessEqual_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(10000),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessEqual_equal() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(1234),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfLessEqual_greater() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfLe(
                v,
                RemoteInt(9999),
                v.toRemoteString(DecimalFormat("####")),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun floatIfGreaterThan_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterThan_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(1234f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterThan_greater() {
        val v = RemoteFloat(12345f)
        val conditionalString =
            selectIfGt(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun intIfGreaterThan_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterThan_equal() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(1234),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterThan_greater() {
        val v = RemoteInt(12345)
        val conditionalString =
            selectIfGt(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("12K")
    }

    @Test
    fun floatIfGreaterEqual_less() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterEqual_equal() {
        val v = RemoteFloat(1234f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun floatIfGreaterEqual_greater() {
        val v = RemoteFloat(10000f)
        val conditionalString =
            selectIfGe(
                v,
                RemoteFloat(10000f),
                (v / 1000f).toRemoteString(DecimalFormat("#0")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("###0")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfGreaterEqual_less() {
        val v = RemoteInt(1234)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("1234")
    }

    @Test
    fun intIfGreaterEqual_equal() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
            )
        val conditionalStringId = conditionalString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(conditionalStringId)).isEqualTo("10K")
    }

    @Test
    fun intIfGreaterEqual_greater() {
        val v = RemoteInt(10000)
        val conditionalString =
            selectIfGe(
                v,
                RemoteInt(10000),
                (v / 1000).toRemoteString(DecimalFormat("##")) + RemoteString("K"),
                v.toRemoteString(DecimalFormat("####")),
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

        assertThat(context.getText(isAEmptyStrId)).isEqualTo("false")
        assertThat(context.getText(isBEmptyStrId)).isEqualTo("true")
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

        assertThat(context.getText(isANotEmptyStrId)).isEqualTo("true")
        assertThat(context.getText(isBNotEmptyStrId)).isEqualTo("false")
    }

    @Test
    fun hasConstantValue_true() {
        assertThat(RemoteString("ABC").hasConstantValue).isTrue()
        assertThat(RemoteString("A").plus(RemoteString("B")).hasConstantValue).isTrue()
        assertThat(
                selectIfGt(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
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
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC)
                    .toRemoteString(DecimalFormat("#0.00"))
                    .hasConstantValue
            )
            .isFalse()
        assertThat(
                selectIfGt(
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

        makeAndUpdateCoreDocument { context.setNamedStringOverride("USER:testString", "override") }

        assertThat(context.getText(resultId)).isEqualTo("override!")
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
    fun uppercase() {
        val s = RemoteString("Hello world")
        val result = s.uppercase()
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("HELLO WORLD")
    }

    @Test
    fun lowercase() {
        val s = RemoteString("Hello world")
        val result = s.lowercase()
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("hello world")
    }

    @Test
    fun trim() {
        val s = RemoteString(" Hello world ")
        val result = s.trim()
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("Hello world")
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
        val decimalFormat =
            android.icu.text.DecimalFormat("#0").apply {
                formatWidth = 10
                padCharacter = ' '
            }
        val s = namedRemoteInt.toRemoteString(decimalFormat)
        val s2 = RemoteFloat(2f).toRemoteString(decimalFormat)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2", " ")
    }

    @Test
    fun computeRequiredCodePointSet_intToString_padNone() {
        val s = namedRemoteInt.toRemoteString(DecimalFormat("##"))
        val s2 = RemoteInt(2).toRemoteString(DecimalFormat("##"))

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padSpace_zeroAfter() {
        val decimalFormat =
            android.icu.text.DecimalFormat("#0").apply {
                formatWidth = 10
                padCharacter = ' '
            }
        val s = namedRemoteFloat.toRemoteString(decimalFormat)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padNone_zeroAfter() {
        val s = namedRemoteFloat.toRemoteString(DecimalFormat("#0"))
        val s2 = RemoteFloat(2f).toRemoteString(DecimalFormat("#0"))

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padSpace_twoAfter() {
        val decimalFormat =
            android.icu.text.DecimalFormat("#0.0").apply {
                formatWidth = 10
                padCharacter = ' '
            }
        val s = namedRemoteFloat.toRemoteString(decimalFormat)
        val s2 = RemoteFloat(2f).toRemoteString(decimalFormat)

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", " ")
        assertThat(s2.computeRequiredCodePointSet(creationState))
            .containsExactly(" ", "0", "2", ".")
    }

    @Test
    fun computeRequiredCodePointSet_floatToString_padNone_twoAfter() {
        val s = namedRemoteFloat.toRemoteString(DecimalFormat("0.00"))
        val s2 = RemoteFloat(2f).toRemoteString(DecimalFormat("#0.0#"))

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("2", "0", ".")
    }

    @Test
    fun computeRequiredCodePointSet_intToString_plus_constant() {
        val s = namedRemoteInt.toRemoteString(DecimalFormat("##")) + RemoteString("K")
        val s2 = RemoteInt(20).toRemoteString(DecimalFormat("##")) + RemoteString("K")

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

    @Test
    fun computeRequiredCodePointSet_uppercase_dynamic() {
        // Use a conditional to ensure we have a dynamic string with a known set of code points
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("abc"), RemoteString("def"))
        val upper = s.uppercase()

        assertThat(upper.computeRequiredCodePointSet(creationState))
            .containsExactly("A", "B", "C", "D", "E", "F")
    }

    @Test
    fun computeRequiredCodePointSet_lowercase_dynamic() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("ABC"), RemoteString("DEF"))
        val lower = s.lowercase()

        assertThat(lower.computeRequiredCodePointSet(creationState))
            .containsExactly("a", "b", "c", "d", "e", "f")
    }

    @Test
    fun extensionFunctionMatches() {
        assertThat("a".rs.constantValue).isEqualTo("a")
        assertThat("b".rs.constantValue).isEqualTo("b")
    }

    @Test
    fun operatorPlusString() {
        val str = "12345".rs + "678"
        val len = str.length
        val lenId = len.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getInteger(lenId)).isEqualTo(8)
    }

    @Test
    fun cacheKeys() {
        val constant = RemoteString("test")
        assertThat(constant.cacheKey).isEqualTo(RemoteConstantCacheKey("test"))

        val named = RemoteString.createNamedRemoteString("test", "")
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(RemoteState.Domain.User, "test"))

        val op = named.substring(0.ri, 2.ri)
        assertThat(op.cacheKey)
            .isEqualTo(
                RemoteOperationCacheKey.create(
                    RemoteString.OperationKey.Substring,
                    named,
                    0.ri,
                    2.ri,
                )
            )
    }

    @Test
    fun remoteFloat_toRemoteString_caching() {
        val rf = RemoteFloat.createNamedRemoteFloat("testFloat", 1.0f)

        val rs1 = rf.toRemoteString(DecimalFormat("#0.##"))
        val rs2 = rf.toRemoteString(DecimalFormat("#0.##"))
        val rs3 = rf.toRemoteString(DecimalFormat("00.##")) // Different 'before'
        val rs4 = rf.toRemoteString(DecimalFormat("#0.00")) // Different 'after'
        val rs5 = rf.toRemoteString(DecimalFormat("#0.##;(#0.##)")) // Different 'flags'

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)
        val id3 = rs3.getIdForCreationState(creationState)
        val id4 = rs4.getIdForCreationState(creationState)
        val id5 = rs5.getIdForCreationState(creationState)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1).isNotEqualTo(id4)
        assertThat(id1).isNotEqualTo(id5)
        assertThat(ImmutableList.of(id1, id3, id4, id5)).containsNoDuplicates()
    }

    @Test
    fun remoteInt_toRemoteString_caching() {
        val ri = RemoteInt.createNamedRemoteInt("testInt", 1)

        val rs1 = ri.toRemoteString(DecimalFormat("#"))
        val rs2 = ri.toRemoteString(DecimalFormat("#"))
        val rs3 = ri.toRemoteString(DecimalFormat("00")) // Different 'before'
        val rs4 = ri.toRemoteString(DecimalFormat("#;(#)")) // Different 'flags'

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)
        val id3 = rs3.getIdForCreationState(creationState)
        val id4 = rs4.getIdForCreationState(creationState)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1).isNotEqualTo(id4)
    }

    @Test
    fun differentRemoteFloats_differentIds() {
        val rf1 = RemoteFloat.createNamedRemoteFloat("testFloat1", 1.0f)
        val rf2 = RemoteFloat.createNamedRemoteFloat("testFloat2", 2.0f)

        val rs1 = rf1.toRemoteString(DecimalFormat("###0"))
        val rs2 = rf2.toRemoteString(DecimalFormat("###0"))

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun differentRemoteInts_differentIds() {
        val ri1 = RemoteInt.createNamedRemoteInt("testInt1", 1)
        val ri2 = RemoteInt.createNamedRemoteInt("testInt2", 2)

        val rs1 = ri1.toRemoteString(DecimalFormat("#"))
        val rs2 = ri2.toRemoteString(DecimalFormat("#"))

        val id1 = rs1.getIdForCreationState(creationState)
        val id2 = rs2.getIdForCreationState(creationState)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun computeRequiredCodePointSet_selectIfLt_float_constant() {
        val s = selectIfLt(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 =
            selectIfLt(RemoteFloat(20f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfLt_int_constant() {
        val s = selectIfLt(RemoteInt(10), RemoteInt(20), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 = selectIfLt(RemoteInt(20), RemoteInt(10), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfLe_float_constant() {
        val s = selectIfLe(RemoteFloat(10f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 =
            selectIfLe(RemoteFloat(20f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfLe_int_constant() {
        val s = selectIfLe(RemoteInt(10), RemoteInt(10), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 = selectIfLe(RemoteInt(20), RemoteInt(10), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfGt_float_constant() {
        val s = selectIfGt(RemoteFloat(20f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 =
            selectIfGt(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfGt_int_constant() {
        val s = selectIfGt(RemoteInt(20), RemoteInt(10), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 = selectIfGt(RemoteInt(10), RemoteInt(20), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfGe_float_constant() {
        val s = selectIfGe(RemoteFloat(10f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 =
            selectIfGe(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIfGe_int_constant() {
        val s = selectIfGe(RemoteInt(10), RemoteInt(10), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A")

        val s2 = selectIfGe(RemoteInt(10), RemoteInt(20), RemoteString("A"), RemoteString("B"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("B")
    }

    @Test
    fun computeRequiredCodePointSet_selectIf_dynamic() {
        val s = selectIfLt(namedRemoteInt, RemoteInt(20), RemoteString("A"), RemoteString("B"))
        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("A", "B")

        val s2 =
            selectIfGt(namedRemoteFloat, RemoteFloat(20f), RemoteString("C"), RemoteString("D"))
        assertThat(s2.computeRequiredCodePointSet(creationState)).containsExactly("C", "D")
    }

    @Test
    fun mutableRemoteString_smokeTest() {
        val mutableStr = MutableRemoteString("test")
        val result = mutableStr + RemoteString("!")
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("test!")
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
