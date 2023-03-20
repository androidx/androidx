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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataExpressionEvaluatorTest {
    @get:Rule val expect = Expect.create()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    @Test
    fun evaluate_noExpression_returnsUnevaluated() = runBlocking {
        val evaluator = ComplicationDataExpressionEvaluator()

        assertThat(evaluator.evaluate(DATA_WITH_NO_EXPRESSION).firstOrNull())
            .isEqualTo(DATA_WITH_NO_EXPRESSION)
    }

    /**
     * Scenarios for testing data with expressions.
     *
     * Each scenario describes the expressed data, the flow of states, and the flow of the evaluated
     * data output.
     */
    enum class DataWithExpressionScenario(
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
                        .setLongText(WireComplicationText("Long Text"))
                        .setLongTitle(WireComplicationText("Long Title"))
                        .setShortText(WireComplicationText("Short Text"))
                        .setShortTitle(WireComplicationText("Short Title"))
                        .setContentDescription(WireComplicationText("Description"))
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
                        .setLongText(WireComplicationText("Long Text"))
                        .setLongTitle(WireComplicationText("Long Title"))
                        .setShortText(WireComplicationText("Short Text"))
                        .setShortTitle(WireComplicationText("Short Title"))
                        .setContentDescription(WireComplicationText("Description"))
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
                        .setShortTitle(WireComplicationText("Valid"))
                        .setShortText(WireComplicationText("Valid"))
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
                        .setShortTitle(WireComplicationText("Valid"))
                        .setShortText(WireComplicationText("Valid"))
                        .build(),
                    INVALID_DATA, // After it was invalidated.
                ),
        ),
    }

    @Test
    fun evaluate_withExpression_returnsEvaluated() = runBlocking {
        for (scenario in DataWithExpressionScenario.values()) {
            // Defensive copy due to in-place evaluation.
            val expressed = WireComplicationData.Builder(scenario.expressed).build()
            val stateStore = ObservableStateStore(mapOf())
            val evaluator = ComplicationDataExpressionEvaluator(stateStore)
            val allEvaluations =
                evaluator
                    .evaluate(expressed)
                    .shareIn(
                        CoroutineScope(Dispatchers.Main.immediate),
                        SharingStarted.Eagerly,
                        replay = 10,
                    )

            for (state in scenario.states) {
                stateStore.setStateEntryValues(state)
            }

            expect
                .withMessage(scenario.name)
                .that(allEvaluations.replayCache)
                .isEqualTo(scenario.evaluated)
        }
    }

    @Test
    fun evaluate_keepExpression_doesNotTrimUnevaluatedExpression() = runBlocking {
        val expressed =
            WireComplicationData.Builder(WireComplicationData.TYPE_NO_DATA)
                .setRangedValueExpression(DynamicFloat.constant(1f))
                .setLongText(WireComplicationText(DynamicString.constant("Long Text")))
                .setLongTitle(WireComplicationText(DynamicString.constant("Long Title")))
                .setShortText(WireComplicationText(DynamicString.constant("Short Text")))
                .setShortTitle(WireComplicationText(DynamicString.constant("Short Title")))
                .setContentDescription(WireComplicationText(DynamicString.constant("Description")))
                .build()
        val evaluator = ComplicationDataExpressionEvaluator(keepExpression = true)

        assertThat(evaluator.evaluate(expressed).firstOrNull())
            .isEqualTo(
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
                        WireComplicationText("Short Title", DynamicString.constant("Short Title"))
                    )
                    .setContentDescription(
                        WireComplicationText("Description", DynamicString.constant("Description"))
                    )
                    .build()
            )
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
