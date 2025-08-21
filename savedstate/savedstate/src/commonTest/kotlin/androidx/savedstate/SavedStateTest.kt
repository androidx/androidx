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

@IgnoreWebTarget
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
    fun contentDeepToString_isEmpty_returnsEmptyBrackets() {
        val savedState = savedState()
        assertThat(savedState.read { contentDeepToString() }).isEqualTo("[]")
    }

    @Test
    fun contentDeepToString_hasPrimitiveValues_returnsFormattedString() {
        val savedState = savedState {
            putInt("intKey", 42)
            putString("stringKey", "Hello")
            putBoolean("boolKey", true)
        }
        val expected = "[intKey=42, stringKey=Hello, boolKey=true]"
        assertThat(savedState.read { contentDeepToString() }).isEqualTo(expected)
    }

    @Test
    fun contentDeepToString_hasArrayValues_returnsFormattedArrayString() {
        val savedState = savedState {
            putStringArray("stringArray", arrayOf("A", "B", "C"))
            putIntArray("intArray", intArrayOf(1, 2, 3))
        }
        val expected = "[stringArray=[A, B, C], intArray=[1, 2, 3]]"
        assertThat(savedState.read { contentDeepToString() }).isEqualTo(expected)
    }

    @Test
    fun contentDeepToString_hasNestedSavedState_returnsFormattedNestedString() {
        val savedState = savedState {
            putSavedState("nested", savedState { putString("nestedKey", "NestedValue") })
        }
        val expected = "[nested=[nestedKey=NestedValue]]"
        assertThat(savedState.read { contentDeepToString() }).isEqualTo(expected)
    }

    @Test
    fun contentDeepToString_hasNullValues_returnsStringWithNull() {
        val savedState = savedState { putNull("nullKey") }
        val expected = "[nullKey=null]"
        assertThat(savedState.read { contentDeepToString() }).isEqualTo(expected)
    }

    @Test
    fun contentDeepToString_hasSameContent_returnsEqualStrings() {
        val state1 = createDefaultSavedState()
        val state2 = createDefaultSavedState()

        val toString1 = state1.read { contentDeepToString() }
        val toString2 = state2.read { contentDeepToString() }

        assertThat(toString1).isEqualTo(toString2)
    }

    @Test
    fun contentDeepToString_hasDifferentContent_returnsDifferentStrings() {
        val state1 = savedState(createDefaultSavedState()) { putInt("id", 1) }
        val state2 = savedState(createDefaultSavedState()) { putInt("id", 2) }

        val toString1 = state1.read { contentDeepToString() }
        val toString2 = state2.read { contentDeepToString() }

        assertThat(toString1).isNotEqualTo(toString2)
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
    fun getBoolean_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThrows<IllegalArgumentException> { underTest.read { getBoolean(KEY_1) } }
    }

    @Test
    fun getBooleanOrNull_whenSet_returns() {
        val expected = true

        val underTest = savedState { putBoolean(KEY_1, expected) }
        val actual = underTest.read { getBooleanOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getBooleanOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getBooleanOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getBooleanOrNull(KEY_1) })
    }

    @Test
    fun getBooleanOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThat(underTest.read { getBooleanOrNull(KEY_1) })
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
    fun getChar_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MIN_VALUE) }
        assertThrows<IllegalArgumentException> { underTest.read { getChar(KEY_1) } }
    }

    @Test
    fun getCharOrNull_whenSet_returns() {
        val underTest = savedState { putChar(KEY_1, Char.MAX_VALUE) }
        val actual = underTest.read { getCharOrNull(KEY_1) }

        assertThat(actual).isEqualTo(Char.MAX_VALUE)
    }

    @Test
    fun getCharOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getCharOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getCharOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MIN_VALUE) }
        assertThat(underTest.read { getCharOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getString(KEY_1) } }
    }

    @Test
    fun getCharSequenceOrNull_whenSet_returns() {
        val underTest = savedState { putCharSequence(KEY_1, CHAR_SEQUENCE_VALUE_1) }
        val actual = underTest.read { getCharSequenceOrNull(KEY_1) }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_VALUE_1)
    }

    @Test
    fun getCharSequenceOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getCharSequenceOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getCharSequenceOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getCharSequenceOrNull(KEY_1) }).isNull()
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
    fun getDouble_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThrows<IllegalArgumentException> { underTest.read { getDouble(KEY_1) } }
    }

    @Test
    fun getDoubleOrNull_whenSet_returns() {
        val underTest = savedState { putDouble(KEY_1, Double.MAX_VALUE) }
        val actual = underTest.read { getDoubleOrNull(KEY_1) }

        assertThat(actual).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun getDoubleOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getDoubleOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getDoubleOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getDoubleOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getDoubleOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThat(underTest.read { getDoubleOrNull(KEY_1) }).isNull()
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
    fun getFloat_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThrows<IllegalArgumentException> { underTest.read { getFloat(KEY_1) } }
    }

    @Test
    fun getFloatOrNull_whenSet_returns() {
        val underTest = savedState { putFloat(KEY_1, Float.MAX_VALUE) }
        val actual = underTest.read { getFloatOrNull(KEY_1) }

        assertThat(actual).isEqualTo(Float.MAX_VALUE)
    }

    @Test
    fun getFloatOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getFloatOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getFloatOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getFloatOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getFloatOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        assertThat(underTest.read { getFloatOrNull(KEY_1) }).isNull()
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
    fun getInt_whenSet_differentType_throws() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        assertThrows<IllegalArgumentException> { underTest.read { getInt(KEY_1) } }
    }

    @Test
    fun getIntOrNull_whenSet_returns() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getIntOrNull(KEY_1) }

        assertThat(actual).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun getIntOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getIntOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getIntOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        assertThat(underTest.read { getIntOrNull(KEY_1) }).isNull()
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
        assertThrows<IllegalArgumentException> { underTest.read { getLong(KEY_1) } }
    }

    @Test
    fun getLongOrNull_whenSet_returns() {
        val underTest = savedState { putLong(KEY_1, Long.MAX_VALUE) }
        val actual = underTest.read { getLongOrNull(KEY_1) }

        assertThat(actual).isEqualTo(Long.MAX_VALUE)
    }

    @Test
    fun getLongOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getLongOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getLongOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getLongOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getLongOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putBoolean(KEY_1, false) }
        assertThat(underTest.read { getLongOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getString(KEY_1) } }
    }

    @Test
    fun getStringOrNull_whenSet_returns() {
        val underTest = savedState { putString(KEY_1, STRING_VALUE) }
        val actual = underTest.read { getStringOrNull(KEY_1) }

        assertThat(actual).isEqualTo(STRING_VALUE)
    }

    @Test
    fun getStringOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getStringOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getStringOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getStringOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getIntList(KEY_1) } }
    }

    @Test
    fun getIntListOrNull_whenSet_returns() {
        val expected = List(size = 5) { idx -> idx }

        val underTest = savedState { putIntList(KEY_1, expected) }
        val actual = underTest.read { getIntListOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntListOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getIntListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntListOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getIntListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntListOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getIntListOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getCharSequenceList(KEY_1) } }
    }

    @Test
    fun getCharSequenceListOrNull_whenSet_returns() {
        val underTest = savedState { putCharSequenceList(KEY_1, CHAR_SEQUENCE_LIST) }
        val actual = underTest.read { getCharSequenceListOrNull(KEY_1) }

        assertThat(actual).isEqualTo(CHAR_SEQUENCE_LIST)
    }

    @Test
    fun getCharSequenceListOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getCharSequenceListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceListOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getCharSequenceListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceListOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getCharSequenceListOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getSavedStateList(KEY_1) } }
    }

    @Test
    fun getSavedStateListOrNull_whenSet_returns() {
        val expected = List(size = 5) { savedState() }

        val underTest = savedState { putSavedStateList(KEY_1, expected) }
        val actual = underTest.read { getSavedStateListOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateListOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSavedStateListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateListOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getSavedStateListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateListOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getSavedStateListOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getStringList(KEY_1) } }
    }

    @Test
    fun getStringListOrNull_whenSet_returns() {
        val underTest = savedState { putStringList(KEY_1, LIST_STRING_VALUE) }
        val actual = underTest.read { getStringListOrNull(KEY_1) }

        assertThat(actual).isEqualTo(LIST_STRING_VALUE)
    }

    @Test
    fun getStringListOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getStringListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringListOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getStringListOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringListOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getStringListOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getBooleanArray(KEY_1) } }
    }

    @Test
    fun getBooleanArrayOrNull_whenSet_returns() {
        val expected = BooleanArray(size = 5) { idx -> idx % 2 == 0 }

        val underTest = savedState { putBooleanArray(KEY_1, expected) }
        val actual = underTest.read { getBooleanArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getBooleanArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getBooleanArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getBooleanArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getBooleanArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getBooleanArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getBooleanArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getCharArray(KEY_1) } }
    }

    @Test
    fun getCharArrayOrNull_whenSet_returns() {
        val expected = CharArray(size = 5) { idx -> idx.toChar() }

        val underTest = savedState { putCharArray(KEY_1, expected) }
        val actual = underTest.read { getCharArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCharArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getCharArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getCharArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getCharArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getCharSequenceArray(KEY_1) } }
    }

    @Test
    fun getCharSequenceArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getCharSequenceArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getCharSequenceArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getCharSequenceArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getCharSequenceArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getDoubleArray(KEY_1) } }
    }

    @Test
    fun getDoubleArrayOrNull_whenSet_returns() {
        val expected = DoubleArray(size = 5) { idx -> idx.toDouble() }

        val underTest = savedState { putDoubleArray(KEY_1, expected) }
        val actual = underTest.read { getDoubleArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getDoubleArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getDoubleArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getDoubleArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getDoubleArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getDoubleArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getDoubleArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getFloatArray(KEY_1) } }
    }

    @Test
    fun getFloatArrayOrNull_whenSet_returns() {
        val expected = FloatArray(size = 5) { idx -> idx.toFloat() }

        val underTest = savedState { putFloatArray(KEY_1, expected) }
        val actual = underTest.read { getFloatArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getFloatArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getFloatArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getFloatArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getFloatArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getFloatArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getFloatArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getIntArray(KEY_1) } }
    }

    @Test
    fun getIntArrayOrNull_whenSet_returns() {
        val expected = IntArray(size = 5) { idx -> idx }

        val underTest = savedState { putIntArray(KEY_1, expected) }
        val actual = underTest.read { getIntArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getIntArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getIntArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getIntArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getIntArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getIntArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getLongArray(KEY_1) } }
    }

    @Test
    fun getLongArrayOrNull_whenSet_returns() {
        val expected = LongArray(size = 5) { idx -> idx.toLong() }

        val underTest = savedState { putLongArray(KEY_1, expected) }
        val actual = underTest.read { getLongArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getLongArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getLongArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getLongArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getLongArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getLongArrayOrNull_whenSet_differentType_returnsNull() {
        val expected = Int.MAX_VALUE

        val underTest = savedState { putInt(KEY_1, expected) }

        assertThat(underTest.read { getLongArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getSavedStateArray(KEY_1) } }
    }

    @Test
    fun getSavedStateArrayOrNull_whenSet_returns() {
        val expected = Array(size = 5) { savedState() }

        val underTest = savedState { putSavedStateArray(KEY_1, expected) }
        val actual = underTest.read { getSavedStateArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSavedStateArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSavedStateArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getSavedStateArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateArrayOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getSavedStateArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getStringArray(KEY_1) } }
    }

    @Test
    fun getStringArrayOrNull_whenSet_returns() {
        val expected = Array(size = 5) { idx -> idx.toString() }

        val underTest = savedState { putStringArray(KEY_1, expected) }
        val actual = underTest.read { getStringArrayOrNull(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getStringArrayOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getStringArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringArrayOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getStringArrayOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getStringArrayOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getStringArrayOrNull(KEY_1) }).isNull()
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

        assertThrows<IllegalArgumentException> { underTest.read { getSavedState(KEY_1) } }
    }

    @Test
    fun getSavedStateOrNull_whenSet_returns() {
        val underTest = savedState { putSavedState(KEY_1, SAVED_STATE_VALUE) }
        val actual = underTest.read { getSavedStateOrNull(KEY_1) }

        assertThat(actual).isEqualTo(SAVED_STATE_VALUE)
    }

    @Test
    fun getSavedStateOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSavedStateOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getSavedStateOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSavedStateOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getSavedStateOrNull(KEY_1) }).isNull()
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
        val LIST_STRING_VALUE = List(size = 5) { idx -> "index=$idx" }
        val SAVED_STATE_VALUE = savedState()
        val CHAR_SEQUENCE_VALUE_1: CharSequence = Int.MIN_VALUE.toString()
        val CHAR_SEQUENCE_LIST = List<CharSequence>(size = 5) { idx -> "index=$idx" }

        private fun createDefaultSavedState(): SavedState {
            var key = 0
            val savedState = savedState {
                putBoolean(key = "KEY_${++key}", value = true)
                putBooleanArray(key = "KEY_${++key}", value = booleanArrayOf(true, false))
                putChar(key = "KEY_${++key}", value = Char.MAX_VALUE)
                putCharArray(
                    key = "KEY_${++key}",
                    value = charArrayOf(Char.MIN_VALUE, Char.MAX_VALUE),
                )
                putDouble(key = "KEY_${++key}", value = Double.MAX_VALUE)
                putDoubleArray(
                    key = "KEY_${++key}",
                    value = doubleArrayOf(Double.MIN_VALUE, Double.MAX_VALUE),
                )
                putFloat(key = "KEY_${++key}", value = Float.MAX_VALUE)
                putFloatArray(
                    key = "KEY_${++key}",
                    value = floatArrayOf(Float.MIN_VALUE, Float.MAX_VALUE),
                )
                putInt(key = "KEY_${++key}", value = Int.MAX_VALUE)
                putIntArray(key = "KEY_${++key}", value = intArrayOf(1, 2, 3))
                putIntList(key = "KEY_${++key}", value = listOf(Int.MIN_VALUE, Int.MAX_VALUE))
                putLong(key = "KEY_${++key}", value = Long.MAX_VALUE)
                putLongArray(
                    key = "KEY_${++key}",
                    value = longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE),
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
