/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.savedstate.internal.SavedStateUtils
import kotlin.test.Test

internal class SavedStateTest : RobolectricTest() {

    @Test
    fun contains_whenHasKey_returnsTrue() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { contains(KEY_1) }).isTrue()
    }

    @Test
    fun contains_whenDoesNotHaveKey_returnsFalse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { contains(KEY_2) }).isFalse()
    }

    @Test
    fun isEmpty_whenEmpty_returnTrue() {
        val underTest = savedState()

        assertThat(underTest.read { isEmpty() }).isTrue()
    }

    @Test
    fun isEmpty_whenNotEmpty_returnFalse() {
        val underTest = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }

        assertThat(underTest.read { isEmpty() }).isFalse()
    }

    @Test
    fun size() {
        val underTest = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }

        assertThat(underTest.read { size() }).isEqualTo(expected = 2)
    }

    @Test
    fun remove() {
        val underTest = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }

        underTest.read {
            assertThat(contains(KEY_1)).isTrue()
            assertThat(contains(KEY_2)).isTrue()
        }

        underTest.write { remove(KEY_1) }

        underTest.read {
            assertThat(contains(KEY_1)).isFalse()
            assertThat(contains(KEY_2)).isTrue()
        }
    }

    @Test
    fun clear() {
        val underTest = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }
        underTest.write { clear() }

        assertThat(underTest.read { isEmpty() }).isTrue()
    }

    @Test
    fun contentDeepEquals_withEqualContent_returnsTrue() {
        val sharedState = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }
        val state2 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isTrue()
    }

    @Test
    fun contentDeepEquals_withMissingKey_returnsFalse() {
        val sharedState = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }
        val state2 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isFalse()
    }

    @Test
    fun contentDeepEquals_withDifferentContent_returnsFalse() {
        val sharedState = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }
        val state2 = savedState {
            putFloat(KEY_1, Float.MAX_VALUE)
            putFloat(KEY_2, Float.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isFalse()
    }

    @Test
    fun contentDeepEquals_withEmptyContent_returnsFalse() {
        val sharedState = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState)
        }
        val state2 = savedState()

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isFalse()
    }

    // region getters and setters
    @Test
    fun getBoolean_whenSet_returns() {
        val expected = true

        val underTest = savedState { putBoolean(KEY_1, expected) }
        val actual = underTest.read { getBoolean(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBoolean_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getBoolean(KEY_1) } }
    }

    @Test
    fun getBoolean_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getBoolean(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_BOOLEAN)
    }

    @Test
    fun getBooleanOrElse_whenSet_returns() {
        val expected = true

        val underTest = savedState { putBoolean(KEY_1, expected) }
        val actual = underTest.read { getBooleanOrElse(KEY_1) { false } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getBooleanOrElse(KEY_1) { true } }

        assertThat(actual).isTrue()
    }

    @Test
    fun getBooleanOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getBooleanOrElse(KEY_1) { true } }

        assertThat(actual).isTrue()
    }

    @Test
    fun getChar_whenSet_returns() {
        val underTest = savedState { putChar(KEY_1, Char.MAX_VALUE) }
        val actual = underTest.read { getChar(KEY_1) }

        assertThat(actual).isEqualTo(Char.MAX_VALUE)
    }

    @Test
    fun getChar_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getChar(KEY_1) } }
    }

    @Test
    fun getChar_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MIN_VALUE) }
        val actual = underTest.read { getChar(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_CHAR)
    }

    @Test
    fun getCharOrElse_whenSet_returns() {
        val underTest = savedState { putChar(KEY_1, Char.MAX_VALUE) }
        val actual = underTest.read { getCharOrElse(KEY_1) { Char.MIN_VALUE } }

        assertThat(actual).isEqualTo(Char.MAX_VALUE)
    }

    @Test
    fun getCharOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getCharOrElse(KEY_1) { Char.MIN_VALUE } }

        assertThat(actual).isEqualTo(Char.MIN_VALUE)
    }

    @Test
    fun getCharOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getCharOrElse(KEY_1) { Char.MAX_VALUE } }

        assertThat(actual).isEqualTo(Char.MAX_VALUE)
    }

    @Test
    fun getDouble_whenSet_returns() {
        val underTest = savedState { putDouble(KEY_1, Double.MAX_VALUE) }
        val actual = underTest.read { getDouble(KEY_1) }

        assertThat(actual).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun getDouble_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getDouble(KEY_1) } }
    }

    @Test
    fun getDouble_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getDouble(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_DOUBLE)
    }

    @Test
    fun getDoubleOrElse_whenSet_returns() {
        val underTest = savedState { putDouble(KEY_1, Double.MAX_VALUE) }
        val actual = underTest.read { getDoubleOrElse(KEY_1) { Double.MIN_VALUE } }

        assertThat(actual).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun getDoubleOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getDoubleOrElse(KEY_1) { Double.MIN_VALUE } }

        assertThat(actual).isEqualTo(Double.MIN_VALUE)
    }

    @Test
    fun getDoubleOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getDoubleOrElse(KEY_1) { Double.MIN_VALUE } }

        assertThat(actual).isEqualTo(Double.MIN_VALUE)
    }

    @Test
    fun getFloat_whenSet_returns() {
        val underTest = savedState { putFloat(KEY_1, Float.MAX_VALUE) }
        val actual = underTest.read { getFloat(KEY_1) }

        assertThat(actual).isEqualTo(Float.MAX_VALUE)
    }

    @Test
    fun getFloat_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getFloat(KEY_1) } }
    }

    @Test
    fun getFloat_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getFloat(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_FLOAT)
    }

    @Test
    fun getFloatOrElse_whenSet_returns() {
        val underTest = savedState { putFloat(KEY_1, Float.MAX_VALUE) }
        val actual = underTest.read { getFloatOrElse(KEY_1) { Float.MIN_VALUE } }

        assertThat(actual).isEqualTo(Float.MAX_VALUE)
    }

    @Test
    fun getFloatOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getFloatOrElse(KEY_1) { Float.MIN_VALUE } }

        assertThat(actual).isEqualTo(Float.MIN_VALUE)
    }

    @Test
    fun getFloatOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getFloatOrElse(KEY_1) { Float.MIN_VALUE } }

        assertThat(actual).isEqualTo(Float.MIN_VALUE)
    }

    @Test
    fun getInt_whenSet_returns() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getInt(KEY_1) }

        assertThat(actual).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun getInt_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getInt(KEY_1) } }
    }

    @Test
    fun getInt_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getInt(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_INT)
    }

    @Test
    fun getIntOrElse_whenSet_returns() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getIntOrElse(KEY_1) { Int.MIN_VALUE } }

        assertThat(actual).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun getIntOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getIntOrElse(KEY_1) { Int.MIN_VALUE } }

        assertThat(actual).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun getIntOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getIntOrElse(KEY_1) { Int.MIN_VALUE } }

        assertThat(actual).isEqualTo(Int.MIN_VALUE)
    }

    @Test
    fun getLong_whenSet_returns() {
        val underTest = savedState { putLong(KEY_1, Long.MAX_VALUE) }
        val actual = underTest.read { getLong(KEY_1) }

        assertThat(actual).isEqualTo(Long.MAX_VALUE)
    }

    @Test
    fun getLong_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getLong(KEY_1) } }
    }

    @Test
    fun getLong_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getLong(KEY_1) }

        assertThat(actual).isEqualTo(SavedStateUtils.DEFAULT_LONG)
    }

    @Test
    fun getLongOrElse_whenSet_returns() {
        val underTest = savedState { putLong(KEY_1, Long.MAX_VALUE) }
        val actual = underTest.read { getLongOrElse(KEY_1) { Long.MIN_VALUE } }

        assertThat(actual).isEqualTo(Long.MAX_VALUE)
    }

    @Test
    fun getLongOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getLongOrElse(KEY_1) { Long.MIN_VALUE } }

        assertThat(actual).isEqualTo(Long.MIN_VALUE)
    }

    @Test
    fun getLongOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getLongOrElse(KEY_1) { Long.MIN_VALUE } }

        assertThat(actual).isEqualTo(Long.MIN_VALUE)
    }

    @Test
    fun putNull_whenSet_returnsTrue() {
        val underTest = savedState { putNull(KEY_1) }
        val actual = underTest.read { isNull(KEY_1) }

        assertThat(actual).isTrue()
    }

    @Test
    fun getNull_whenSet_nonNull_returnsFalse() {
        val underTest = savedState { putBoolean(KEY_1, true) }
        val actual = underTest.read { isNull(KEY_1) }

        assertThat(actual).isFalse()
    }

    @Test
    fun putNull_whenNotSet_returnsFalse() {
        val underTest = savedState()
        val actual = underTest.read { isNull(KEY_1) }

        assertThat(actual).isFalse()
    }

    @Test
    fun getString_whenSet_returns() {
        val underTest = savedState { putString(KEY_1, STRING_VALUE) }
        val actual = underTest.read { getString(KEY_1) }

        assertThat(actual).isEqualTo(STRING_VALUE)
    }

    @Test
    fun getString_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getString(KEY_1) } }
    }

    @Test
    fun getString_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> { underTest.read { getString(KEY_1) } }
    }

    @Test
    fun getStringOrElse_whenSet_returns() {
        val underTest = savedState { putString(KEY_1, STRING_VALUE) }
        val actual = underTest.read { getString(KEY_1) }

        assertThat(actual).isEqualTo(STRING_VALUE)
    }

    @Test
    fun getStringOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getStringOrElse(KEY_1) { STRING_VALUE } }

        assertThat(actual).isEqualTo(STRING_VALUE)
    }

    @Test
    fun getStringOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getStringOrElse(KEY_1) { STRING_VALUE } }

        assertThat(actual).isEqualTo(STRING_VALUE)
    }

    @Test
    fun getIntList_whenSet_returns() {
        val expected = List(size = 5) { idx -> idx }

        val underTest = savedState { putIntList(KEY_1, expected) }
        val actual = underTest.read { getIntList(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntList_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getIntList(KEY_1) } }
    }

    @Test
    fun getIntList_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getIntList(KEY_1) } }
    }

    @Test
    fun getIntListOrElse_whenSet_returns() {
        val underTest = savedState { putIntList(KEY_1, LIST_INT_VALUE) }
        val actual = underTest.read { getIntListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(LIST_INT_VALUE)
    }

    @Test
    fun getIntListOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getIntListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<Int>())
    }

    @Test
    fun getIntOrElseList_whenSet_differentType_returnsElse() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }
        val actual = underTest.read { getIntListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<Int>())
    }

    @Test
    fun getStringList_whenSet_returns() {
        val underTest = savedState { putStringList(KEY_1, LIST_STRING_VALUE) }
        val actual = underTest.read { getStringList(KEY_1) }

        assertThat(actual).isEqualTo(LIST_STRING_VALUE)
    }

    @Test
    fun getStringList_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getStringList(KEY_1) } }
    }

    @Test
    fun getStringList_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getStringList(KEY_1) } }
    }

    @Test
    fun getStringListOrElse_whenSet_returns() {
        val underTest = savedState { putStringList(KEY_1, LIST_STRING_VALUE) }
        val actual = underTest.read { getStringListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(LIST_STRING_VALUE)
    }

    @Test
    fun getStringListOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getStringListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<String>())
    }

    @Test
    fun getStringListOrElse_whenSet_differentType_returnsElse() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }
        val actual = underTest.read { getStringListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<String>())
    }

    @Test
    fun getSavedState_whenSet_returns() {
        val underTest = savedState { putSavedState(KEY_1, SAVED_STATE_VALUE) }
        val actual = underTest.read { getSavedState(KEY_1) }

        assertThat(actual).isEqualTo(SAVED_STATE_VALUE)
    }

    @Test
    fun getSavedState_whenNotSet_throws() {
        assertThrows<IllegalStateException> { savedState().read { getSavedState(KEY_1) } }
    }

    @Test
    fun getSavedState_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> { underTest.read { getSavedState(KEY_1) } }
    }

    @Test
    fun getSavedStateOrElse_whenSet_returns() {
        val underTest = savedState { putSavedState(KEY_1, SAVED_STATE_VALUE) }
        val actual = underTest.read { getSavedStateOrElse(KEY_1) { savedState() } }

        assertThat(actual).isEqualTo(SAVED_STATE_VALUE)
    }

    @Test
    fun getSavedStateOrElse_whenNotSet_returnsElse() {
        val expected = savedState()

        val actual = savedState().read { getSavedStateOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateOrElse_whenSet_differentType_returnsElse() {
        val expected = savedState()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getSavedStateOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun putAll() {
        val previousState = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val underTest = savedState { putAll(previousState) }
        val actual = underTest.read { getInt(KEY_1) }

        assertThat(actual).isEqualTo(Int.MAX_VALUE)
    }

    // endregion

    private companion object {
        const val KEY_1 = "KEY_1"
        const val KEY_2 = "KEY_2"
        const val KEY_3 = "KEY_3"
        const val STRING_VALUE = "string-value"
        val LIST_INT_VALUE = List(size = 5) { idx -> idx }
        val LIST_STRING_VALUE = List(size = 5) { idx -> "index=$idx" }
        val SET_INT_VALUE = LIST_INT_VALUE.toSet()
        val SET_STRING_VALUE = LIST_STRING_VALUE.toSet()
        val SAVED_STATE_VALUE = savedState()
    }
}
