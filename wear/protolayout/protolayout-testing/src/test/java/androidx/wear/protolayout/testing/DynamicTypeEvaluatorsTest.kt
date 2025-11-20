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

package androidx.wear.protolayout.testing

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.PlatformEventSources
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.PlatformHealthSources.Keys
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.mapTo
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class DynamicTypeEvaluatorsTest {
    @Test
    fun dynamicStringEvaluation_singleAppState() {
        val testValue1 = 23
        val testValue2 = 2
        val stateKey = AppDataKey<DynamicInt32>("testKey")
        val testDynamicString = DynamicInt32.from(stateKey).times(testValue2).format()
        val expectedValue = (testValue1 * testValue2).toString()

        assertThat(testDynamicString.evaluate(dynamicDataMapOf(stateKey mapTo testValue1)))
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicStringEvaluation_multipleAppState() {
        val testValue1 = "Hello "
        val testValue2 = "World!"
        val stateKey1 = AppDataKey<DynamicString>("testKey1")
        val stateKey2 = AppDataKey<DynamicString>("testKey2")
        val testDynamicString = DynamicString.from(stateKey1).concat(DynamicString.from(stateKey2))
        val expectedValue = testValue1 + testValue2

        assertThat(
                testDynamicString.evaluate(
                    dynamicDataMapOf(stateKey1 mapTo testValue1, stateKey2 mapTo testValue2)
                )
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicStringEvaluation_singlePlatformData() {
        val testValue1 = 97F
        val testValue2 = 10F
        val testDynamicString = PlatformHealthSources.heartRateBpm().minus(testValue2).format()
        val expectedValue = (testValue1 - testValue2).toInt().toString()

        assertThat(
                testDynamicString.evaluate(dynamicDataMapOf(Keys.HEART_RATE_BPM mapTo testValue1))
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicStringEvaluation_multiplePlatformData() {
        val testValue1 = 97
        val testValue2 = 10F
        val testDynamicString =
            PlatformHealthSources.dailySteps().minus(PlatformHealthSources.heartRateBpm()).format()
        val expectedValue = (testValue1 - testValue2).toInt().toString()

        assertThat(
                testDynamicString.evaluate(
                    dynamicDataMapOf(
                        Keys.DAILY_STEPS mapTo testValue1,
                        Keys.HEART_RATE_BPM mapTo testValue2,
                    )
                )
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicStringEvaluation_mixedAppStateAndPlatformData() {
        val testValue1 = 97F
        val testValue2 = 10F
        val stateKey = AppDataKey<DynamicFloat>("testKey")
        val testDynamicString =
            PlatformHealthSources.heartRateBpm().div(DynamicFloat.from(stateKey)).format()
        val expectedValue = (testValue1 / testValue2).toString()

        assertThat(
                testDynamicString.evaluate(
                    dynamicDataMapOf(
                        Keys.HEART_RATE_BPM mapTo testValue1,
                        stateKey mapTo testValue2,
                    )
                )
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicFloatEvaluation() {
        val testValue1 = 97F
        val testValue2 = 10F
        val testValue3 = 1.7F
        val stateKey = AppDataKey<DynamicFloat>("testKey")
        val testDynamicFloat =
            PlatformHealthSources.heartRateBpm().div(DynamicFloat.from(stateKey)).minus(testValue3)
        val expectedValue = testValue1 / testValue2 - testValue3

        assertThat(
                testDynamicFloat.evaluate(
                    dynamicDataMapOf(
                        Keys.HEART_RATE_BPM mapTo testValue1,
                        stateKey mapTo testValue2,
                    )
                )
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicInt32Evaluation() {
        val testValue1 = 97
        val testValue2 = 10
        val stateKey = AppDataKey<DynamicInt32>("testKey")
        val testDynamicInt = PlatformHealthSources.dailySteps().minus(DynamicInt32.from(stateKey))
        val expectedValue = testValue1 - testValue2

        assertThat(
                testDynamicInt.evaluate(
                    dynamicDataMapOf(Keys.DAILY_STEPS mapTo testValue1, stateKey mapTo testValue2)
                )
            )
            .isEqualTo(expectedValue)
    }

    @Test
    fun dynamicInt32Evaluation_withAnimation() {
        val startValue = 97
        val endValue = 10
        val testDynamicInt32 = DynamicInt32.animate(startValue, endValue)

        // AnimatableNode will assign the end value directly, as the node is considered not visible
        // by default
        assertThat(testDynamicInt32.evaluate()).isEqualTo(endValue)
    }

    @Test
    fun dynamicBooleanEvaluation() {
        val testValue1 = 97F
        val testValue2 = 80F
        val stateKey = AppDataKey<DynamicFloat>("testKey")
        val injectedData =
            dynamicDataMapOf(Keys.HEART_RATE_BPM mapTo testValue1, stateKey mapTo testValue2)

        assertThat(
                PlatformHealthSources.heartRateBpm()
                    .gt(DynamicFloat.from(stateKey))
                    .evaluate(injectedData)
            )
            .isEqualTo(testValue1 > testValue2)

        assertThat(
                PlatformHealthSources.heartRateBpm()
                    .lt(DynamicFloat.from(stateKey))
                    .evaluate(injectedData)
            )
            .isEqualTo(testValue1 < testValue2)
    }

    @Test
    fun dynamicColorEvaluation() {
        val colorValue1 = Color.valueOf(10)
        val colorValue2 = Color.valueOf(20)
        val colorStateKey = AppDataKey<DynamicColor>("colorTestKey")

        assertThat(
                DynamicColor.from(colorStateKey)
                    .evaluate(dynamicDataMapOf(colorStateKey mapTo colorValue1))
            )
            .isEqualTo(colorValue1)

        val testDynamicColor =
            DynamicColor.onCondition(PlatformEventSources.isLayoutVisible())
                .use(colorValue1.toArgb())
                .elseUse(colorValue2.toArgb())

        assertThat(
                testDynamicColor.evaluate(
                    dynamicDataMapOf(PlatformEventSources.Keys.LAYOUT_VISIBILITY mapTo true)
                )
            )
            .isEqualTo(colorValue1)

        assertThat(
                testDynamicColor.evaluate(
                    dynamicDataMapOf(PlatformEventSources.Keys.LAYOUT_VISIBILITY mapTo false)
                )
            )
            .isEqualTo(colorValue2)
    }

    @Test
    fun dynamicDurationEvaluation() {
        val duration1 = Duration.ofMinutes(12)
        val duration2 = Duration.ofSeconds(2345)
        val stateKey = AppDataKey<DynamicDuration>("testKey")
        val testDynamicDuration = DynamicDuration.from(stateKey)

        assertThat(testDynamicDuration.evaluate(dynamicDataMapOf(stateKey mapTo duration1)))
            .isEqualTo(duration1)
        assertThat(testDynamicDuration.evaluate(dynamicDataMapOf(stateKey mapTo duration2)))
            .isEqualTo(duration2)

        assertThat(DynamicDuration.withSecondsPrecision(duration2).evaluate()).isEqualTo(duration2)
    }

    @Test
    fun dynamicInstantEvaluation() {
        val instant1 = Instant.ofEpochSecond(1234)
        val instant2 = Instant.ofEpochMilli(4321)
        val stateKey = AppDataKey<DynamicInstant>("testKey")
        val testDynamicInstant = DynamicInstant.from(stateKey)

        assertThat(
                testDynamicInstant.evaluate(dynamicDataMapOf(stateKey mapTo instant1))!!.epochSecond
            )
            .isEqualTo(instant1.epochSecond)
        assertThat(
                testDynamicInstant.evaluate(dynamicDataMapOf(stateKey mapTo instant2))!!.epochSecond
            )
            .isEqualTo(instant2.epochSecond)

        assertThat(DynamicInstant.withSecondsPrecision(instant1).evaluate()!!.epochSecond)
            .isEqualTo(instant1.epochSecond)
    }
}
