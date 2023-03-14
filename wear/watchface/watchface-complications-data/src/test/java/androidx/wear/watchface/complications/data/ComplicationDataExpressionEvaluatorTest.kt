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
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.StateEntryBuilders.StateEntryValue
import androidx.wear.protolayout.expression.pipeline.ObservableStateStore
import androidx.wear.watchface.complications.data.ComplicationDataExpressionEvaluator.Companion.INVALID_DATA
import androidx.wear.watchface.complications.data.ComplicationDataExpressionEvaluator.Companion.hasExpression
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataExpressionEvaluatorTest {
    @get:Rule val expect = Expect.create()

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
     * Scenarios for testing expressions.
     *
     * Each scenario describes the expressed data, the flow of states, and the flow of the evaluated
     * data output.
     */
    enum class DataExpressionScenario(
        val expressed: WireComplicationData,
        val states: List<Map<String, StateEntryValue>>,
        val evaluated: List<WireComplicationData>,
    ) {
        SET_IMMEDIATELY_WHEN_ALL_DATA_AVAILABLE(
            expressed =
                WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                    .setRangedValueExpression(DynamicFloat.constant(1f))
                    .setLongText(WireComplicationText(DynamicString.constant("Long Text")))
                    .setLongTitle(WireComplicationText(DynamicString.constant("Long Title")))
                    .setShortText(WireComplicationText(DynamicString.constant("Short Text")))
                    .setShortTitle(WireComplicationText(DynamicString.constant("Short Title")))
                    .setContentDescription(
                        WireComplicationText(DynamicString.constant("Description"))
                    )
                    .build(),
            states = listOf(),
            evaluated =
                listOf(
                    WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                        .setRangedValue(1f)
                        .setRangedValueExpression(DynamicFloat.constant(1f))
                        .setLongText(
                            WireComplicationText("Long Text", DynamicString.constant("Long Text"))
                        )
                        .setLongTitle(
                            WireComplicationText("Long Title", DynamicString.constant("Long Title"))
                        )
                        .setShortText(
                            WireComplicationText("Short Text", DynamicString.constant("Short Text"))
                        )
                        .setShortTitle(
                            WireComplicationText(
                                "Short Title",
                                DynamicString.constant("Short Title")
                            )
                        )
                        .setContentDescription(
                            WireComplicationText(
                                "Description",
                                DynamicString.constant("Description")
                            )
                        )
                        .build()
                ),
        ),
        SET_ONLY_AFTER_ALL_FIELDS_EVALUATED(
            expressed =
                WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                    .setRangedValueExpression(DynamicFloat.fromState("ranged_value"))
                    .setLongText(WireComplicationText(DynamicString.fromState("long_text")))
                    .setLongTitle(WireComplicationText(DynamicString.fromState("long_title")))
                    .setShortText(WireComplicationText(DynamicString.fromState("short_text")))
                    .setShortTitle(WireComplicationText(DynamicString.fromState("short_title")))
                    .setContentDescription(
                        WireComplicationText(DynamicString.fromState("description"))
                    )
                    .build(),
            states =
                aggregate(
                    // Each map piles on top of the previous ones.
                    mapOf("ranged_value" to StateEntryValue.fromFloat(1f)),
                    mapOf("long_text" to StateEntryValue.fromString("Long Text")),
                    mapOf("long_title" to StateEntryValue.fromString("Long Title")),
                    mapOf("short_text" to StateEntryValue.fromString("Short Text")),
                    mapOf("short_title" to StateEntryValue.fromString("Short Title")),
                    // Only the last one will trigger an evaluated data.
                    mapOf("description" to StateEntryValue.fromString("Description")),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // Before state is available.
                    WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                        .setRangedValue(1f)
                        .setRangedValueExpression(DynamicFloat.fromState("ranged_value"))
                        .setLongText(
                            WireComplicationText("Long Text", DynamicString.fromState("long_text"))
                        )
                        .setLongTitle(
                            WireComplicationText(
                                "Long Title",
                                DynamicString.fromState("long_title")
                            )
                        )
                        .setShortText(
                            WireComplicationText(
                                "Short Text",
                                DynamicString.fromState("short_text")
                            )
                        )
                        .setShortTitle(
                            WireComplicationText(
                                "Short Title",
                                DynamicString.fromState("short_title")
                            )
                        )
                        .setContentDescription(
                            WireComplicationText(
                                "Description",
                                DynamicString.fromState("description")
                            )
                        )
                        .build()
                ),
        ),
        SET_TO_EVALUATED_IF_ALL_FIELDS_VALID(
            expressed =
                WireComplicationData.Builder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.fromState("valid")))
                    .setShortText(WireComplicationText(DynamicString.fromState("valid")))
                    .build(),
            states =
                listOf(
                    mapOf("valid" to StateEntryValue.fromString("Valid")),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // Before state is available.
                    WireComplicationData.Builder(WireComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(
                            WireComplicationText("Valid", DynamicString.fromState("valid"))
                        )
                        .setShortText(
                            WireComplicationText("Valid", DynamicString.fromState("valid"))
                        )
                        .build(),
                ),
        ),
        SET_TO_NO_DATA_IF_FIRST_STATE_IS_INVALID(
            expressed =
                WireComplicationData.Builder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.fromState("valid")))
                    .setShortText(WireComplicationText(DynamicString.fromState("invalid")))
                    .build(),
            states =
                listOf(
                    mapOf(),
                    mapOf("valid" to StateEntryValue.fromString("Valid")),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // States invalid after one field changed to valid.
                ),
        ),
        SET_TO_NO_DATA_IF_LAST_STATE_IS_INVALID(
            expressed =
                WireComplicationData.Builder(WireComplicationData.TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.fromState("valid")))
                    .setShortText(WireComplicationText(DynamicString.fromState("invalid")))
                    .build(),
            states =
                listOf(
                    mapOf(
                        "valid" to StateEntryValue.fromString("Valid"),
                        "invalid" to StateEntryValue.fromString("Valid"),
                    ),
                    mapOf("valid" to StateEntryValue.fromString("Valid")),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // Before state is available.
                    WireComplicationData.Builder(WireComplicationData.TYPE_SHORT_TEXT)
                        .setShortTitle(
                            WireComplicationText("Valid", DynamicString.fromState("valid"))
                        )
                        .setShortText(
                            WireComplicationText("Valid", DynamicString.fromState("invalid"))
                        )
                        .build(),
                    INVALID_DATA, // After it was invalidated.
                ),
        ),
    }

    @Test
    fun data_expression_setToEvaluated() {
        for (scenario in DataExpressionScenario.values()) {
            // Defensive copy due to in-place evaluation.
            val expressed = WireComplicationData.Builder(scenario.expressed).build()
            val stateStore = ObservableStateStore(mapOf())
            ComplicationDataExpressionEvaluator(expressed, stateStore).use { evaluator ->
                val allEvaluations =
                    evaluator.data
                        .filterNotNull()
                        .shareIn(
                            CoroutineScope(Dispatchers.Main),
                            SharingStarted.Eagerly,
                            replay = 10,
                        )
                evaluator.init()
                runUiThreadTasks() // Ensures data sharing started.

                for (state in scenario.states) {
                    stateStore.setStateEntryValues(state)
                    runUiThreadTasks() // Ensures data sharing ended.
                }

                expect
                    .withMessage(scenario.name)
                    .that(allEvaluations.replayCache)
                    .isEqualTo(scenario.evaluated)
            }
        }
    }

    enum class HasExpressionDataWithExpressionScenario(val data: WireComplicationData) {
        RANGED_VALUE(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setRangedValueExpression(DynamicFloat.constant(1f))
                .build()
        ),
        LONG_TEXT(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setLongText(WireComplicationText(DynamicString.constant("Long Text")))
                .build()
        ),
        LONG_TITLE(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setLongTitle(WireComplicationText(DynamicString.constant("Long Title")))
                .build()
        ),
        SHORT_TEXT(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setShortText(WireComplicationText(DynamicString.constant("Short Text")))
                .build()
        ),
        SHORT_TITLE(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setShortTitle(WireComplicationText(DynamicString.constant("Short Title")))
                .build()
        ),
        CONTENT_DESCRIPTION(
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setContentDescription(WireComplicationText(DynamicString.constant("Description")))
                .build()
        ),
    }

    @Test
    fun hasExpression_dataWithExpression_returnsTrue() {
        for (scenario in HasExpressionDataWithExpressionScenario.values()) {
            expect.withMessage(scenario.name).that(hasExpression(scenario.data)).isTrue()
        }
    }

    @Test
    fun hasExpression_dataWithoutExpression_returnsFalse() {
        assertThat(hasExpression(DATA_WITH_NO_EXPRESSION)).isFalse()
    }

    @Test
    fun compat_notInitialized_listenerNotInvoked() {
        ComplicationDataExpressionEvaluator.Compat(DATA_WITH_NO_EXPRESSION, listener).use {
            runUiThreadTasks()

            verify(listener, never()).accept(any())
        }
    }

    @Test
    fun compat_noExpression_listenerInvokedWithData() {
        ComplicationDataExpressionEvaluator.Compat(DATA_WITH_NO_EXPRESSION, listener).use {
            evaluator ->
            evaluator.init(ContextCompat.getMainExecutor(getApplicationContext()))
            runUiThreadTasks()

            verify(listener, times(1)).accept(DATA_WITH_NO_EXPRESSION)
        }
    }

    private companion object {
        val DATA_WITH_NO_EXPRESSION =
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setRangedValue(10f)
                .build()

        /** Converts `[{a: A}, {b: B}, {c: C}]` to `[{a: A}, {a: A, b: B}, {a: A, b: B, c: C}]`. */
        fun <K, V> aggregate(vararg maps: Map<K, V>): List<Map<K, V>> =
            maps.fold(listOf()) { acc, map -> acc + ((acc.lastOrNull() ?: mapOf()) + map) }
    }
}
