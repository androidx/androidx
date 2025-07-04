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
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DynamicDataPairTest {

    @Test
    fun templatedPair_asPair() {
        val pair: Pair<DynamicDataKey<DynamicInt32>, DynamicDataValue<DynamicInt32>> =
            DynamicDataPair(INT_KEY, DynamicDataValue.fromInt(INT_VALUE)).asPair()

        assertThat(pair.first).isEqualTo(INT_KEY)
        assertThat(pair.second.intValue).isEqualTo(INT_VALUE)
    }

    @Test
    fun bool_infixPair() {
        val pair = BOOL_KEY mapTo BOOL_VALUE

        assertThat(pair.key).isEqualTo(BOOL_KEY)
        assertThat(pair.value.boolValue).isEqualTo(BOOL_VALUE)
    }

    @Test
    fun color_infixPair() {
        val pair = COLOR_KEY mapTo COLOR_VALUE

        assertThat(pair.key).isEqualTo(COLOR_KEY)
        assertThat(Color.valueOf(pair.value.colorValue)).isEqualTo(COLOR_VALUE)
    }

    @Test
    fun duration_infixPair() {
        val pair = DURATION_KEY mapTo DURATION_VALUE

        assertThat(pair.key).isEqualTo(DURATION_KEY)
        assertThat(pair.value.durationValue).isEqualTo(DURATION_VALUE)
    }

    @Test
    fun float_infixPair() {
        val pair = FLOAT_KEY mapTo FLOAT_VALUE

        assertThat(pair.key).isEqualTo(FLOAT_KEY)
        assertThat(pair.value.floatValue).isEqualTo(FLOAT_VALUE)
    }

    @Test
    fun instant_infixPair() {
        val pair = INSTANT_KEY mapTo INSTANT_VALUE

        assertThat(pair.key).isEqualTo(INSTANT_KEY)
        assertThat(pair.value.instantValue).isEqualTo(INSTANT_VALUE)
    }

    @Test
    fun int_infixPair() {
        val pair = INT_KEY mapTo INT_VALUE

        assertThat(pair.key).isEqualTo(INT_KEY)
        assertThat(pair.value.intValue).isEqualTo(INT_VALUE)
    }

    @Test
    fun string_infixPair() {
        val pair = STRING_KEY mapTo STRING_VALUE

        assertThat(pair.key).isEqualTo(STRING_KEY)
        assertThat(pair.value.stringValue).isEqualTo(STRING_VALUE)
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
