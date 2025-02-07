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

package androidx.wear.protolayout.expression

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicDataMapTest {

    @Test
    fun size_returnsCorrectValue() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(map.size).isEqualTo(2)
    }

    @Test
    fun mutableMap_size_returnsCorrectValue() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)
        map[FLOAT_KEY] = FLOAT_VALUE

        assertThat(map.size).isEqualTo(3)
    }

    @Test
    fun contains_availableKey_returnsTrue() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(INT_KEY in map).isTrue()
    }

    @Test
    fun mutableMap_availableKey_returnsTrue() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(INT_KEY in map).isTrue()
    }

    @Test
    fun contains_unAvailableKey_returnsFalse() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(FLOAT_KEY in map).isFalse()
    }

    @Test
    fun mutableMap_unAvailableKey_returnsFalse() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(FLOAT_KEY in map).isFalse()
    }

    @Test
    fun contains_wrongType_returnsFalse() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(floatAppDataKey(INT_KEY.key) in map).isFalse()
    }

    @Test
    fun mutableMap_wrongType_returnsFalse() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE)

        assertThat(floatAppDataKey(INT_KEY.key) in map).isFalse()
    }

    @Test
    fun get_availableKeys_returnsValue() {
        val map =
            dynamicDataMapOf(
                BOOL_KEY mapTo BOOL_VALUE,
                COLOR_KEY mapTo COLOR_VALUE,
                DURATION_KEY mapTo DURATION_VALUE,
                FLOAT_KEY mapTo FLOAT_VALUE,
                INSTANT_KEY mapTo INSTANT_VALUE,
                INT_KEY mapTo INT_VALUE,
                STRING_KEY mapTo STRING_VALUE
            )

        assertThat(map[BOOL_KEY]).isEqualTo(BOOL_VALUE)
        assertThat(map[COLOR_KEY]).isEqualTo(COLOR_VALUE)
        assertThat(map[DURATION_KEY]).isEqualTo(DURATION_VALUE)
        assertThat(map[FLOAT_KEY]).isEqualTo(FLOAT_VALUE)
        assertThat(map[INSTANT_KEY]).isEqualTo(INSTANT_VALUE)
        assertThat(map[INT_KEY]).isEqualTo(INT_VALUE)
        assertThat(map[STRING_KEY]).isEqualTo(STRING_VALUE)
    }

    @Test
    fun mutableMap_get_availableKeys_returnsValue() {
        val map =
            mutableDynamicDataMapOf(
                BOOL_KEY mapTo BOOL_VALUE,
                COLOR_KEY mapTo COLOR_VALUE,
                DURATION_KEY mapTo DURATION_VALUE,
                FLOAT_KEY mapTo FLOAT_VALUE,
                INSTANT_KEY mapTo INSTANT_VALUE,
                INT_KEY mapTo INT_VALUE,
                STRING_KEY mapTo STRING_VALUE
            )

        assertThat(map[BOOL_KEY]).isEqualTo(BOOL_VALUE)
        assertThat(map[COLOR_KEY]).isEqualTo(COLOR_VALUE)
        assertThat(map[DURATION_KEY]).isEqualTo(DURATION_VALUE)
        assertThat(map[FLOAT_KEY]).isEqualTo(FLOAT_VALUE)
        assertThat(map[INSTANT_KEY]).isEqualTo(INSTANT_VALUE)
        assertThat(map[INT_KEY]).isEqualTo(INT_VALUE)
        assertThat(map[STRING_KEY]).isEqualTo(STRING_VALUE)
    }

    @Test
    fun get_unAvailableKey_returnsNull() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE)

        assertThat(map[STRING_KEY]).isNull()
    }

    @Test
    fun mutableMap_get_unAvailableKey_returnsNull() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE)

        assertThat(map[STRING_KEY]).isNull()
    }

    @Test
    fun get_wrongType_throws() {
        val map = dynamicDataMapOf(INT_KEY mapTo INT_VALUE)

        assertFailsWith<IllegalStateException> { map[stringAppDataKey(INT_KEY.key)] }
    }

    @Test
    fun mutableMap_get_wrongType_throws() {
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE)

        assertFailsWith<IllegalStateException> { map[stringAppDataKey(INT_KEY.key)] }
    }

    @Test
    fun plusAnotherMap_returnsNewReadOnlyConcatenatedMap() {
        val map =
            dynamicDataMapOf(INT_KEY mapTo INT_VALUE) +
                dynamicDataMapOf(STRING_KEY mapTo STRING_VALUE)

        assertThat(map)
            .isEqualTo(dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE))
    }

    @Test
    fun mutableMap_plusAnotherMap_returnsNewReadOnlyConcatenatedMap() {
        val map =
            mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE) +
                mutableDynamicDataMapOf(STRING_KEY mapTo STRING_VALUE)

        assertThat(map)
            .isEqualTo(dynamicDataMapOf(INT_KEY mapTo INT_VALUE, STRING_KEY mapTo STRING_VALUE))
    }

    @Test
    fun mutableMap_set_addsAssociation() {
        val map = mutableDynamicDataMapOf()
        map[BOOL_KEY] = BOOL_VALUE
        map[COLOR_KEY] = COLOR_VALUE
        map[DURATION_KEY] = DURATION_VALUE
        map[FLOAT_KEY] = FLOAT_VALUE
        map[INSTANT_KEY] = INSTANT_VALUE
        map[INT_KEY] = INT_VALUE
        map[STRING_KEY] = STRING_VALUE

        assertThat(map[BOOL_KEY]).isEqualTo(BOOL_VALUE)
        assertThat(map[COLOR_KEY]).isEqualTo(COLOR_VALUE)
        assertThat(map[DURATION_KEY]).isEqualTo(DURATION_VALUE)
        assertThat(map[FLOAT_KEY]).isEqualTo(FLOAT_VALUE)
        assertThat(map[INSTANT_KEY]).isEqualTo(INSTANT_VALUE)
        assertThat(map[INT_KEY]).isEqualTo(INT_VALUE)
        assertThat(map[STRING_KEY]).isEqualTo(STRING_VALUE)
    }

    @Test
    fun mutableMap_set_existingKey_overwrites() {
        val stringKeyWithIntKeyName = stringAppDataKey(INT_KEY.key)
        val map = mutableDynamicDataMapOf(INT_KEY mapTo INT_VALUE)
        map[stringKeyWithIntKeyName] = STRING_VALUE

        assertThat(map[stringKeyWithIntKeyName]).isEqualTo(STRING_VALUE)
    }

    private companion object {
        val BOOL_KEY = boolAppDataKey("bool")
        const val BOOL_VALUE = true
        val COLOR_KEY = colorAppDataKey("color")
        val COLOR_VALUE = Color.valueOf(Color.RED)
        val DURATION_KEY = durationAppDataKey("duration")
        val DURATION_VALUE: Duration = Duration.ofHours(42)
        val FLOAT_KEY = floatAppDataKey("float")
        const val FLOAT_VALUE = 3.14f
        val INSTANT_KEY = instantAppDataKey("instant")
        val INSTANT_VALUE: Instant = Instant.ofEpochSecond(123456)
        val INT_KEY = intAppDataKey("int")
        const val INT_VALUE = 42
        val STRING_KEY = stringAppDataKey("string")
        const val STRING_VALUE = "42"
    }
}
