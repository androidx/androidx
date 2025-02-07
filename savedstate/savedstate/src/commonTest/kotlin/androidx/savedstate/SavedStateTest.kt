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
import kotlin.test.Test

internal class SavedStateTest : RobolectricTest() {

    @Test
    fun factory_withMap_hasInitialState() {
        val oldState = createDefaultSavedState().read { toMap() }
        val newState = savedState(oldState).read { toMap() }

        assertThat(newState).isEqualTo(oldState)
    }

    @Test
    fun factory_withSavedState_hasInitialState() {
        val oldState = createDefaultSavedState()
        val newState = savedState(oldState)

        assertThat(oldState.read { contentDeepEquals(newState) }).isTrue()
    }

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
        val state1 = createDefaultSavedState()
        val state2 = createDefaultSavedState()

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isTrue()
    }

    @Test
    fun contentDeepEquals_withMissingKey_returnsFalse() {
        val sharedState1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putIntArray(KEY_3, intArrayOf(1, 2, 3))
        }
        val sharedState2 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putIntArray(KEY_3, intArrayOf(1, 2, 3))
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState1)
        }
        val state2 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState2)
        }

        val contentDeepEquals = state1.read { contentDeepEquals(state2) }

        assertThat(contentDeepEquals).isFalse()
    }

    @Test
    fun contentDeepEquals_withDifferentContent_returnsFalse() {
        val sharedState1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putIntArray(KEY_3, intArrayOf(1, 2, 3))
        }
        val sharedState2 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putIntArray(KEY_3, intArrayOf(1, 2, 3))
        }
        val state1 = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putInt(KEY_2, Int.MAX_VALUE)
            putSavedState(KEY_3, sharedState1)
        }
        val state2 = savedState {
            putFloat(KEY_1, Float.MAX_VALUE)
            putFloat(KEY_2, Float.MAX_VALUE)
            putSavedState(KEY_3, sharedState2)
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

    @Test
    fun contentDeepHashCode_isConsistentForSameInstance() {
        val state = createDefaultSavedState()

        val hashCode1 = state.read { contentDeepHashCode() }
        val hashCode2 = state.read { contentDeepHashCode() }

        assertThat(hashCode1).isEqualTo(hashCode2)
    }

    @Test
    fun contentDeepHashCode_isEqualForSameContent() {
        val state1 = createDefaultSavedState()
        val state2 = createDefaultSavedState()

        val hashCode1 = state1.read { contentDeepHashCode() }
        val hashCode2 = state2.read { contentDeepHashCode() }

        assertThat(hashCode1).isEqualTo(hashCode2)
    }

    @Test
    fun contentDeepHashCode_isDifferentForDifferentContent() {
        val state1 = savedState { putInt("id", 1) }
        val state2 = savedState { putInt("id", 2) }

        val hashCode1 = state1.read { contentDeepHashCode() }
        val hashCode2 = state2.read { contentDeepHashCode() }

        assertThat(hashCode1).isNotEqualTo(hashCode2)
    }

    @Test
    fun contentDeepHashCode_generatesUniqueValues() {
        val states = List(size = 1000) { idx -> savedState { putInt("id", idx) } }

        // Calculate the hash code, of each element, and remove any possible duplicate.
        val hashCodes = states.map { state -> state.read { contentDeepHashCode() } }.toSet()

        // Ensure that each hash code is unique.
        assertThat(hashCodes.size).isEqualTo(states.size)
    }

    @Test
    fun toMap() {
        val sharedState = savedState {
            putInt(KEY_1, Int.MIN_VALUE)
            putNull(KEY_2)
        }
        val parentState = savedState {
            putInt(KEY_1, Int.MAX_VALUE)
            putNull(KEY_2)
            putSavedState(KEY_3, sharedState)
        }

        val actual = parentState.read { toMap() }

        val expected = mapOf(KEY_1 to Int.MAX_VALUE, KEY_2 to null, KEY_3 to sharedState)
        assertThat(actual).containsExactlyEntriesIn(expected)
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
        assertThrows<IllegalArgumentException> { savedState().read { getBoolean(KEY_1) } }
    }

    @Test
    fun getBoolean_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getBoolean(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_BOOLEAN)
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
        assertThrows<IllegalArgumentException> { savedState().read { getChar(KEY_1) } }
    }

    @Test
    fun getChar_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MIN_VALUE) }
        val actual = underTest.read { getChar(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_CHAR)
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
    fun getCharSequence_whenSet_returns() {
        val underTest = savedState { putCharSequence(KEY_1, CHAR_SEQUENCE_VALUE_1) }
        val actual = underTest.read { getCharSequence(KEY_1) }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_VALUE_1)
    }

    @Test
    fun getCharSequence_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getCharSequence(KEY_1) } }
    }

    @Test
    fun getCharSequence_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> { underTest.read { getString(KEY_1) } }
    }

    @Test
    fun getCharSequenceOrElse_whenSet_returns() {
        val underTest = savedState { putCharSequence(KEY_1, CHAR_SEQUENCE_VALUE_1) }
        val actual = underTest.read { getCharSequenceOrElse(KEY_1) { CHAR_SEQUENCE_VALUE_2 } }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_VALUE_1)
    }

    @Test
    fun getCharSequenceOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getCharSequenceOrElse(KEY_1) { CHAR_SEQUENCE_VALUE_2 } }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_VALUE_2)
    }

    @Test
    fun getCharSequenceOrElse_whenSet_differentType_returnsElse() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getCharSequenceOrElse(KEY_1) { CHAR_SEQUENCE_VALUE_2 } }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_VALUE_2)
    }

    @Test
    fun getDouble_whenSet_returns() {
        val underTest = savedState { putDouble(KEY_1, Double.MAX_VALUE) }
        val actual = underTest.read { getDouble(KEY_1) }

        assertThat(actual).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun getDouble_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getDouble(KEY_1) } }
    }

    @Test
    fun getDouble_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getDouble(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_DOUBLE)
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
        assertThrows<IllegalArgumentException> { savedState().read { getFloat(KEY_1) } }
    }

    @Test
    fun getFloat_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getFloat(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_FLOAT)
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
        assertThrows<IllegalArgumentException> { savedState().read { getInt(KEY_1) } }
    }

    @Test
    fun getInt_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getInt(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_INT)
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
        assertThrows<IllegalArgumentException> { savedState().read { getLong(KEY_1) } }
    }

    @Test
    fun getLong_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        val actual = underTest.read { getLong(KEY_1) }

        assertThat(actual).isEqualTo(DEFAULT_LONG)
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
        assertThrows<IllegalArgumentException> { savedState().read { getString(KEY_1) } }
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
        assertThrows<IllegalArgumentException> { savedState().read { getIntList(KEY_1) } }
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
    fun getCharSequenceList_whenSet_returns() {
        val underTest = savedState { putCharSequenceList(KEY_1, CHAR_SEQUENCE_LIST) }
        val actual = underTest.read { getCharSequenceList(KEY_1) }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_LIST)
    }

    @Test
    fun getCharSequenceList_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getCharSequenceList(KEY_1) } }
    }

    @Test
    fun getCharSequenceList_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getCharSequenceList(KEY_1) } }
    }

    @Test
    fun getCharSequenceListOrElse_whenSet_returns() {
        val underTest = savedState { putCharSequenceList(KEY_1, CHAR_SEQUENCE_LIST) }
        val actual = underTest.read { getCharSequenceListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_LIST)
    }

    @Test
    fun getCharSequenceListOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getCharSequenceListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<CharSequence>())
    }

    @Test
    fun getCharSequenceListOrElse_whenSet_differentType_returnsElse() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }
        val actual = underTest.read { getCharSequenceListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<CharSequence>())
    }

    @Test
    fun getSavedStateList_whenSet_returns() {
        val expected = List(size = 5) { savedState() }

        val underTest = savedState { putSavedStateList(KEY_1, expected) }
        val actual = underTest.read { getSavedStateList(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateList_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getSavedStateList(KEY_1) } }
    }

    @Test
    fun getSavedStateList_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> { underTest.read { getSavedStateList(KEY_1) } }
    }

    @Test
    fun getSavedStateListOrElse_whenSet_returns() {
        val expected = List(size = 5) { savedState() }

        val underTest = savedState { putSavedStateList(KEY_1, expected) }
        val actual = underTest.read { getSavedStateListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateListOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getSavedStateListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<SavedState>())
    }

    @Test
    fun getSavedStateListOrElse_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getSavedStateListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<SavedState>())
    }

    @Test
    fun getStringList_whenSet_returns() {
        val underTest = savedState { putStringList(KEY_1, LIST_STRING_VALUE) }
        val actual = underTest.read { getStringList(KEY_1) }

        assertThat(actual).isEqualTo(LIST_STRING_VALUE)
    }

    @Test
    fun getStringList_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getStringList(KEY_1) } }
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
    fun getBooleanArray_whenSet_returns() {
        val expected = BooleanArray(size = 5) { idx -> idx % 2 == 0 }

        val underTest = savedState { putBooleanArray(KEY_1, expected) }
        val actual = underTest.read { getBooleanArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getBooleanArray(KEY_1) } }
    }

    @Test
    fun getBooleanArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getBooleanArray(KEY_1) } }
    }

    @Test
    fun getBooleanArrayOrElse_whenSet_returns() {
        val expected = BooleanArray(size = 5) { idx -> idx % 2 == 0 }

        val underTest = savedState { putBooleanArray(KEY_1, expected) }
        val actual = underTest.read { getBooleanArrayOrElse(KEY_1) { BooleanArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanArrayOrElse_whenNotSet_returnsElse() {
        val expected = BooleanArray(size = 0)

        val actual = savedState().read { getBooleanArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = booleanArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getBooleanArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharArray_whenSet_returns() {
        val expected = CharArray(size = 5) { idx -> idx.toChar() }

        val underTest = savedState { putCharArray(KEY_1, expected) }
        val actual = underTest.read { getCharArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getCharArray(KEY_1) } }
    }

    @Test
    fun getCharArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getCharArray(KEY_1) } }
    }

    @Test
    fun getCharArrayOrElse_whenSet_returns() {
        val expected = CharArray(size = 5) { idx -> idx.toChar() }

        val underTest = savedState { putCharArray(KEY_1, expected) }
        val actual = underTest.read { getCharArrayOrElse(KEY_1) { CharArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharArrayOrElse_whenNotSet_returnsElse() {
        val expected = CharArray(size = 0)

        val actual = savedState().read { getCharArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = charArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getCharArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharSequenceArray_whenSet_returns() {
        val expected = Array<CharSequence>(size = 5) { idx -> idx.toString() }

        val underTest = savedState { putCharSequenceArray(KEY_1, expected) }
        val actual = underTest.read { getCharSequenceArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharSequenceArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getCharSequenceArray(KEY_1) } }
    }

    @Test
    fun getCharSequenceArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getCharSequenceArray(KEY_1) } }
    }

    @Test
    fun getCharSequenceArrayOrElse_whenSet_returns() {
        val expected = CHAR_SEQUENCE_ARRAY

        val underTest = savedState { putCharSequenceArray(KEY_1, expected) }
        val actual = underTest.read { getCharSequenceArrayOrElse(KEY_1) { emptyArray() } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharSequenceArrayOrElse_whenNotSet_returnsElse() {
        val expected = CHAR_SEQUENCE_ARRAY

        val actual = savedState().read { getCharSequenceArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharSequenceArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = CHAR_SEQUENCE_ARRAY

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getCharSequenceArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getDoubleArray_whenSet_returns() {
        val expected = DoubleArray(size = 5) { idx -> idx.toDouble() }

        val underTest = savedState { putDoubleArray(KEY_1, expected) }
        val actual = underTest.read { getDoubleArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getDoubleArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getDoubleArray(KEY_1) } }
    }

    @Test
    fun getDoubleArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getDoubleArray(KEY_1) } }
    }

    @Test
    fun getDoubleArrayOrElse_whenSet_returns() {
        val expected = DoubleArray(size = 5) { idx -> idx.toDouble() }

        val underTest = savedState { putDoubleArray(KEY_1, expected) }
        val actual = underTest.read { getDoubleArrayOrElse(KEY_1) { DoubleArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getDoubleArrayOrElse_whenNotSet_returnsElse() {
        val expected = DoubleArray(size = 0)

        val actual = savedState().read { getDoubleArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getDoubleArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = doubleArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getDoubleArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getFloatArray_whenSet_returns() {
        val expected = FloatArray(size = 5) { idx -> idx.toFloat() }

        val underTest = savedState { putFloatArray(KEY_1, expected) }
        val actual = underTest.read { getFloatArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getFloatArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getFloatArray(KEY_1) } }
    }

    @Test
    fun getFloatArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getFloatArray(KEY_1) } }
    }

    @Test
    fun getFloatArrayOrElse_whenSet_returns() {
        val expected = FloatArray(size = 5) { idx -> idx.toFloat() }

        val underTest = savedState { putFloatArray(KEY_1, expected) }
        val actual = underTest.read { getFloatArrayOrElse(KEY_1) { FloatArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getFloatArrayOrElse_whenNotSet_returnsElse() {
        val expected = FloatArray(size = 0)

        val actual = savedState().read { getFloatArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getFloatArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = floatArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getFloatArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntArray_whenSet_returns() {
        val expected = IntArray(size = 5) { idx -> idx }

        val underTest = savedState { putIntArray(KEY_1, expected) }
        val actual = underTest.read { getIntArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getIntArray(KEY_1) } }
    }

    @Test
    fun getIntArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getIntArray(KEY_1) } }
    }

    @Test
    fun getIntArrayOrElse_whenSet_returns() {
        val expected = IntArray(size = 5) { idx -> idx }

        val underTest = savedState { putIntArray(KEY_1, expected) }
        val actual = underTest.read { getIntArrayOrElse(KEY_1) { IntArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntArrayOrElse_whenNotSet_returnsElse() {
        val expected = IntArray(size = 0)

        val actual = savedState().read { getIntArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = intArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getIntArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getLongArray_whenSet_returns() {
        val expected = LongArray(size = 5) { idx -> idx.toLong() }

        val underTest = savedState { putLongArray(KEY_1, expected) }
        val actual = underTest.read { getLongArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getLongArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getLongArray(KEY_1) } }
    }

    @Test
    fun getLongArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getLongArray(KEY_1) } }
    }

    @Test
    fun getLongArrayOrElse_whenSet_returns() {
        val expected = LongArray(size = 5) { idx -> idx.toLong() }

        val underTest = savedState { putLongArray(KEY_1, expected) }
        val actual = underTest.read { getLongArrayOrElse(KEY_1) { LongArray(size = 0) } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getLongArrayOrElse_whenNotSet_returnsElse() {
        val expected = LongArray(size = 0)

        val actual = savedState().read { getLongArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getLongArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = longArrayOf()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getLongArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateArray_whenSet_returns() {
        val expected = Array(size = 5) { savedState() }

        val underTest = savedState { putSavedStateArray(KEY_1, expected) }
        val actual = underTest.read { getSavedStateArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateArray_ofParcelable_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getSavedStateArray(KEY_1) } }
    }

    @Test
    fun getSavedStateArray_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> { underTest.read { getSavedStateArray(KEY_1) } }
    }

    @Test
    fun getSavedStateArrayOrElse_whenSet_returns() {
        val expected = Array(size = 5) { savedState() }

        val underTest = savedState { putSavedStateArray(KEY_1, expected) }
        val actual = underTest.read { getSavedStateArrayOrElse(KEY_1) { emptyArray() } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateArrayOrElse_ofParcelable_whenNotSet_returnsElse() {
        val actual = savedState().read { getSavedStateArrayOrElse(KEY_1) { emptyArray() } }

        assertThat(actual).isEqualTo(emptyArray<SavedState>())
    }

    @Test
    fun getSavedStateArrayOrElse_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getSavedStateArrayOrElse(KEY_1) { emptyArray() } }

        assertThat(actual).isEqualTo(emptyArray<SavedState>())
    }

    @Test
    fun getStringArray_whenSet_returns() {
        val expected = Array(size = 5) { idx -> idx.toString() }

        val underTest = savedState { putStringArray(KEY_1, expected) }
        val actual = underTest.read { getStringArray(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getStringArray_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getStringArray(KEY_1) } }
    }

    @Test
    fun getStringArray_whenSet_differentType_throws() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThrows<IllegalStateException> { underTest.read { getStringArray(KEY_1) } }
    }

    @Test
    fun getStringArrayOrElse_whenSet_returns() {
        val expected = Array(size = 5) { idx -> idx.toString() }

        val underTest = savedState { putStringArray(KEY_1, expected) }
        val actual = underTest.read { getStringArrayOrElse(KEY_1) { arrayOf() } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getStringArrayOrElse_whenNotSet_returnsElse() {
        val expected = arrayOf<String>()

        val actual = savedState().read { getStringArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getStringArrayOrElse_whenSet_differentType_returnsElse() {
        val expected = arrayOf<String>()

        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getStringArrayOrElse(KEY_1) { expected } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedState_whenSet_returns() {
        val underTest = savedState { putSavedState(KEY_1, SAVED_STATE_VALUE) }
        val actual = underTest.read { getSavedState(KEY_1) }

        assertThat(actual).isEqualTo(SAVED_STATE_VALUE)
    }

    @Test
    fun getSavedState_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getSavedState(KEY_1) } }
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

    private companion object TestUtils {
        const val KEY_1 = "KEY_1"
        const val KEY_2 = "KEY_2"
        const val KEY_3 = "KEY_3"
        const val STRING_VALUE = "string-value"
        val LIST_INT_VALUE = List(size = 5) { idx -> idx }
        val LIST_STRING_VALUE = List(size = 5) { idx -> "index=$idx" }
        val SAVED_STATE_VALUE = savedState()
        val CHAR_SEQUENCE_VALUE_1: CharSequence = Int.MIN_VALUE.toString()
        val CHAR_SEQUENCE_VALUE_2: CharSequence = Int.MAX_VALUE.toString()
        val CHAR_SEQUENCE_ARRAY = Array<CharSequence>(size = 5) { idx -> "index=$idx" }
        val CHAR_SEQUENCE_LIST = List<CharSequence>(size = 5) { idx -> "index=$idx" }

        private fun createDefaultSavedState(): SavedState {
            var key = 0
            val savedState = savedState {
                putBoolean(key = "KEY_${++key}", value = true)
                putBooleanArray(key = "KEY_${++key}", value = booleanArrayOf(true, false))
                putChar(key = "KEY_${++key}", value = Char.MAX_VALUE)
                putCharArray(
                    key = "KEY_${++key}",
                    value = charArrayOf(Char.MIN_VALUE, Char.MAX_VALUE)
                )
                putDouble(key = "KEY_${++key}", value = Double.MAX_VALUE)
                putDoubleArray(
                    key = "KEY_${++key}",
                    value = doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE)
                )
                putFloat(key = "KEY_${++key}", value = Float.MAX_VALUE)
                putFloatArray(
                    key = "KEY_${++key}",
                    value = floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE)
                )
                putInt(key = "KEY_${++key}", value = Int.MAX_VALUE)
                putIntArray(key = "KEY_${++key}", value = intArrayOf(1, 2, 3))
                putIntList(key = "KEY_${++key}", value = listOf(Int.MIN_VALUE, Int.MAX_VALUE))
                putLong(key = "KEY_${++key}", value = Long.MAX_VALUE)
                putLongArray(
                    key = "KEY_${++key}",
                    value = longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE)
                )
                putNull(key = "KEY_${++key}")
                putString(key = "KEY_${++key}", value = "Text")
                putStringArray(key = "KEY_${++key}", value = arrayOf("Text3", "text4"))
                putStringList(key = "KEY_${++key}", value = listOf("Text1", "text2"))
            }
            return savedState {
                putAll(savedState)
                putSavedState(key = "KEY_${++key}", savedState)
            }
        }
    }
}
