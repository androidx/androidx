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

import androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX
import androidx.compose.remote.core.operations.TextFromFloat.GROUPING_NONE
import androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_ROUNDING
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE
import androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD
import androidx.compose.remote.core.operations.TextTransform
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt.Companion.createNamedRemoteInt
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStringTest {

    val context = TestRemoteContext()
    val creationState = testCreationState(profiles = PROFILE_ANDROIDX)
    val namedRemoteFloat = createNamedRemoteFloat("testFloat", 12.0f)
    val namedRemoteInt = createNamedRemoteInt("testInt", 12)

    @Test
    fun toRemoteStringWithPostfix() {
        val percentage = RemoteFloat(45.5f)
        val percentageString = percentage.toRemoteStringOptions(2, 1, 0) + RemoteString("%")
        val percentageStringId = percentageString.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(percentageStringId)).isEqualTo("45.5%")
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
        assertThat(RemoteTimeVariables.continuousSeconds.toRemoteString().hasConstantValue)
            .isFalse()
        assertThat(
                selectIfGt(
                        RemoteFloat(10f),
                        RemoteTimeVariables.continuousSeconds,
                        RemoteString("A"),
                        RemoteString("B"),
                    )
                    .hasConstantValue
            )
            .isFalse()
    }

    @Test
    fun namedRemoteString_initialValue() {
        val namedRemoteString = createNamedRemoteString("testString", "initial")
        val result = namedRemoteString + RemoteString("!")
        val resultId = result.getIdForCreationState(creationState)

        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("initial!")
    }

    @Test
    fun namedRemoteString_overriddenValue() {
        val namedRemoteString = createNamedRemoteString("testString", "initial")
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
    fun uppercase_nop_elidesTextTransform() {
        val percentage = RemoteFloat(45.5f)
        val source = percentage.toRemoteStringOptions(2, 1, 0) + RemoteString("%")
        val result = source.uppercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("45.5%")
        assertThat(resultId).isEqualTo(source.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).isEmpty()
    }

    @Test
    fun uppercase_dynamic_withLowerLetters_emitsTextTransform() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("abc"), RemoteString("def"))
        val result = s.uppercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(context.getText(resultId)).isEqualTo("DEF")
        assertThat(doc.operations.filterIsInstance<TextTransform>()).hasSize(1)
    }

    @Test
    fun uppercase_nested_elidesSecondTransform() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("abc"), RemoteString("def"))
        val upper1 = s.uppercase()
        val upper2 = upper1.uppercase()
        val resultId = upper2.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(context.getText(resultId)).isEqualTo("DEF")
        assertThat(resultId).isEqualTo(upper1.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).hasSize(1)
    }

    @Test
    fun uppercase_namedString_emitsTextTransform() {
        val named = RemoteString.createNamedRemoteString("str", "123%")
        val upper = named.uppercase()
        upper.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(doc.operations.filterIsInstance<TextTransform>()).hasSize(1)
    }

    @Test
    fun uppercase_nonEnglishUncased_elidesTextTransform() {
        val s =
            selectIfLt(
                namedRemoteFloat,
                RemoteFloat(0f),
                RemoteString("こんにちは"),
                RemoteString("مرحبا"),
            )
        val result = s.uppercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(context.getText(resultId)).isEqualTo("مرحبا")
        assertThat(resultId).isEqualTo(s.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).isEmpty()
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
    fun lowercase_nop_elidesTextTransform() {
        val percentage = RemoteFloat(45.5f)
        val source = percentage.toRemoteStringOptions(2, 1, 0) + RemoteString("%")
        val result = source.lowercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("45.5%")
        assertThat(resultId).isEqualTo(source.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).isEmpty()
    }

    @Test
    fun lowercase_dynamic_withUpperLetters_emitsTextTransform() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("ABC"), RemoteString("DEF"))
        val result = s.lowercase()
        val resultId = result.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(context.getText(resultId)).isEqualTo("def")
        assertThat(doc.operations.filterIsInstance<TextTransform>()).hasSize(1)
    }

    @Test
    fun lowercase_nested_elidesSecondTransform() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("ABC"), RemoteString("DEF"))
        val lower1 = s.lowercase()
        val lower2 = lower1.lowercase()
        val resultId = lower2.getIdForCreationState(creationState)

        val doc = makeAndUpdateCoreDocument {}

        assertThat(context.getText(resultId)).isEqualTo("def")
        assertThat(resultId).isEqualTo(lower1.getIdForCreationState(creationState))
        assertThat(doc.operations.filterIsInstance<TextTransform>()).hasSize(1)
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

        val named = createNamedRemoteString("test", "")
        assertThat(named.cacheKey).isEqualTo(RemoteNamedCacheKey(RemoteState.Domain.User, "test"))

        val op = named.substring(0.ri, 2.ri)
        val key = op.cacheKey as RemoteOperationCacheKey
        assertThat(key.op.name).isEqualTo("Substring")
        assertThat(key.args).isEqualTo(listOf(named.cacheKey, 0.ri.cacheKey, 2.ri.cacheKey))
    }

    @Test
    fun mutableRemoteString_smokeTest() {
        val mutableStr = MutableRemoteString("test")
        val result = mutableStr + RemoteString("!")
        val resultId = result.getIdForCreationState(creationState)
        makeAndPaintCoreDocument()

        assertThat(context.getText(resultId)).isEqualTo("test!")
    }

    @Test
    fun computeRequiredCodePointSet_constantString() {
        val s = RemoteString("Hello")
        val s2 = RemoteString("Hi© Thère®")

        assertThat(s.computeRequiredCodePointSet(creationState)).containsExactly("H", "e", "l", "o")
        assertThat(s2.computeRequiredCodePointSet(creationState))
            .containsExactly("H", "i", "T", "h", "©", "®", "e", "è", "r", " ")
    }

    @Test
    fun computeRequiredCodePointSet_constantStringConcatenation() {
        val s = RemoteString("Hello") + RemoteString("World")

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("H", "e", "l", "o", "W", "r", "d")
    }

    @Test
    fun computeRequiredCodePointSet_intAndFloatToString() {
        val padSpaceFlags =
            SEPARATOR_COMMA_PERIOD or
                GROUPING_NONE or
                OPTIONS_ROUNDING or
                PAD_AFTER_NONE or
                PAD_PRE_SPACE
        val padNoneFlags =
            SEPARATOR_COMMA_PERIOD or
                GROUPING_NONE or
                OPTIONS_ROUNDING or
                PAD_AFTER_NONE or
                PAD_PRE_NONE
        val padAfterZeroFlags =
            SEPARATOR_COMMA_PERIOD or
                GROUPING_NONE or
                OPTIONS_ROUNDING or
                PAD_AFTER_ZERO or
                PAD_PRE_NONE

        val s1 =
            namedRemoteInt
                .toRemoteFloat()
                .toRemoteStringWithPadding(255, 0, padSpaceFlags, 1, 10, ' ')
        val s2 = namedRemoteInt.toRemoteFloat().toRemoteStringWithPadding(255, 0, padNoneFlags, 1)
        val s3 = namedRemoteFloat.toRemoteStringWithPadding(255, 1, padSpaceFlags, 1, 10, ' ')
        val s4 = namedRemoteFloat.toRemoteStringWithPadding(255, 2, padAfterZeroFlags, 1)

        assertThat(s1.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", " ")
        assertThat(s2.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertThat(s3.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", " ")
        assertThat(s4.computeRequiredCodePointSet(creationState))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".")
    }

    @Test
    fun computeRequiredCodePointSet_namedRemoteString() {
        val namedRemoteString = createNamedRemoteString("testString", "initial")
        val s2 = namedRemoteString + RemoteString("!")

        assertThat(namedRemoteString.computeRequiredCodePointSet(creationState)).isNull()
        assertThat(s2.computeRequiredCodePointSet(creationState)).isNull()
    }

    @Test
    fun computeRequiredCodePointSet_uppercase() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("abc"), RemoteString("def"))
                .uppercase()

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("A", "B", "C", "D", "E", "F")
    }

    @Test
    fun computeRequiredCodePointSet_lowercase() {
        val s =
            selectIfLt(namedRemoteFloat, RemoteFloat(0f), RemoteString("ABC"), RemoteString("DEF"))
                .lowercase()

        assertThat(s.computeRequiredCodePointSet(creationState))
            .containsExactly("a", "b", "c", "d", "e", "f")
    }

    @Test
    fun computeRequiredCodePointSet_select() {
        assertThat(
                selectIfLt(RemoteFloat(10f), RemoteFloat(20f), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A")
        assertThat(
                selectIfLt(RemoteInt(20), RemoteInt(10), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("B")
        assertThat(
                selectIfLe(RemoteFloat(10f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A")
        assertThat(
                selectIfLe(RemoteInt(20), RemoteInt(10), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("B")
        assertThat(
                selectIfGt(RemoteFloat(20f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A")
        assertThat(
                selectIfGt(RemoteInt(10), RemoteInt(20), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("B")
        assertThat(
                selectIfGe(RemoteFloat(10f), RemoteFloat(10f), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A")
        assertThat(
                selectIfGe(RemoteInt(10), RemoteInt(20), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("B")
        assertThat(
                selectIfLt(namedRemoteInt, RemoteInt(20), RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A", "B")
        assertThat(
                selectIfGt(namedRemoteFloat, RemoteFloat(20f), RemoteString("C"), RemoteString("D"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("C", "D")
    }

    @Test
    fun computeRequiredCodePointSet_booleanSelect() {
        assertThat(
                RemoteBoolean(true)
                    .select(RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A")
        assertThat(
                RemoteBoolean(false)
                    .select(RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("B")
        assertThat(
                RemoteBoolean.createNamedRemoteBoolean("test", true)
                    .select(RemoteString("A"), RemoteString("B"))
                    .computeRequiredCodePointSet(creationState)
            )
            .containsExactly("A", "B")
    }

    @Test
    fun toDebugString_concatenation() {
        val s = createNamedRemoteString("s", "hi")
        val expr = s + RemoteString(" there")
        assertThat(expr.toDebugString()).isEqualTo("""user:s + " there"""")
    }

    @Test
    fun toDebugString_select() {
        val x = createNamedRemoteFloat("x", 1f)
        val y = createNamedRemoteFloat("y", 2f)
        val sel = selectIfLt(x, y, RemoteString("A"), RemoteString("B"))
        assertThat(sel.toDebugString()).isEqualTo("""user:x < user:y ? "A" : "B"""")
    }

    @Test
    fun toDebugString_contextVariable() {
        val continuousSecFloat = RemoteTimeVariables.continuousSeconds
        val strExpr = continuousSecFloat.toRemoteString()
        assertThat(strExpr.toDebugString()).isEqualTo("context:continuous_sec.toRemoteString()")
    }

    @Test
    fun toDebugString_constant() {
        val rs = RemoteString("hello")
        assertThat(rs.toDebugString()).isEqualTo("\"hello\"")
    }

    private fun makeAndPaintCoreDocument() = context.loadDocument(creationState)

    private fun makeAndUpdateCoreDocument(runAfterInit: () -> Unit) =
        context.loadDocument(creationState, runAfterInit)
}
