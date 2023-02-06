/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications.data

import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.support.wearable.complications.ComplicationText as WireComplicationText
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataExpressionEvaluatorTest {
    @get:Rule
    val expect = Expect.create()

    private val listener = mock<Consumer<WireComplicationData>>()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    @Test
    fun data_notInitialized_setToNull() {
        ComplicationDataExpressionEvaluator(DATA_WITH_NO_EXPRESSION).use { evaluator ->
            assertThat(evaluator.data.value).isNull()
        }
    }

    @Test
    fun data_noExpression_setToUnevaluated() {
        ComplicationDataExpressionEvaluator(DATA_WITH_NO_EXPRESSION).use { evaluator ->
            evaluator.init()
            runUiThreadTasks()

            assertThat(evaluator.data.value).isEqualTo(DATA_WITH_NO_EXPRESSION)
        }
    }

    /**
     * Scenarios for testing per-field static expressions.
     *
     * Each scenario describes how to set the expression in the [WireComplicationData] and how to
     * set the evaluated value.
     *
     * Note that evaluated data retains the expression.
     */
    enum class StaticExpressionScenario(
        val expressed: WireComplicationData.Builder.() -> WireComplicationData.Builder,
        val evaluated: WireComplicationData.Builder.() -> WireComplicationData.Builder,
    ) {
        RANGED_VALUE(
            expressed = { setRangedValueExpression(DynamicFloat.constant(10f)) },
            evaluated = {
                setRangedValue(10f).setRangedValueExpression(DynamicFloat.constant(10f))
            },
        ),
        LONG_TEXT(
            expressed = { setLongText(WireComplicationText(DynamicString.constant("hello"))) },
            evaluated = {
                setLongText(WireComplicationText("hello", DynamicString.constant("hello")))
            },
        ),
        LONG_TITLE(
            expressed = { setLongTitle(WireComplicationText(DynamicString.constant("hello"))) },
            evaluated = {
                setLongTitle(WireComplicationText("hello", DynamicString.constant("hello")))
            },
        ),
        SHORT_TEXT(
            expressed = { setShortText(WireComplicationText(DynamicString.constant("hello"))) },
            evaluated = {
                setShortText(WireComplicationText("hello", DynamicString.constant("hello")))
            },
        ),
        SHORT_TITLE(
            expressed = { setShortTitle(WireComplicationText(DynamicString.constant("hello"))) },
            evaluated = {
                setShortTitle(WireComplicationText("hello", DynamicString.constant("hello")))
            },
        ),
        CONTENT_DESCRIPTION(
            expressed = {
                setContentDescription(WireComplicationText(DynamicString.constant("hello")))
            },
            evaluated = {
                setContentDescription(
                    WireComplicationText("hello", DynamicString.constant("hello"))
                )
            },
        ),
    }

    @Test
    fun data_staticExpression_setToEvaluated() {
        for (scenario in StaticExpressionScenario.values()) {
            val base = WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA).build()
            val expressed = scenario.expressed(WireComplicationData.Builder(base)).build()
            val evaluated =
                scenario.evaluated(WireComplicationData.Builder(base)).build()

            ComplicationDataExpressionEvaluator(expressed).use { evaluator ->
                evaluator.init()
                runUiThreadTasks()

                expect
                    .withMessage(scenario.name)
                    .that(evaluator.data.value)
                    .isEqualTo(evaluated)
            }
        }
    }

    @Test
    fun compat_notInitialized_listenerNotInvoked() {
        ComplicationDataExpressionEvaluator.Compat(
            DATA_WITH_NO_EXPRESSION,
            ContextCompat.getMainExecutor(getApplicationContext()),
            listener,
        ).use {
            runUiThreadTasks()

            verify(listener, never()).accept(any())
        }
    }

    @Test
    fun compat_noExpression_listenerInvokedWithData() {
        ComplicationDataExpressionEvaluator.Compat(
            DATA_WITH_NO_EXPRESSION,
            ContextCompat.getMainExecutor(getApplicationContext()),
            listener,
        ).use { evaluator ->
            evaluator.init()
            runUiThreadTasks()

            verify(listener, times(1)).accept(DATA_WITH_NO_EXPRESSION)
        }
    }

    private companion object {
        val DATA_WITH_NO_EXPRESSION =
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setRangedValue(10f)
                .build()
    }
}
