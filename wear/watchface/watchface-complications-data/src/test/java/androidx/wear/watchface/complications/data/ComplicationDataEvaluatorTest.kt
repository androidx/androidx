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
import android.support.wearable.complications.ComplicationData.Companion.TYPE_NO_DATA
import android.support.wearable.complications.ComplicationData.Companion.TYPE_SHORT_TEXT
import android.support.wearable.complications.ComplicationText as WireComplicationText
import android.util.Log
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider
import androidx.wear.protolayout.expression.pipeline.StateStore
import androidx.wear.watchface.complications.data.ComplicationDataEvaluator.Companion.INVALID_DATA
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.robolectric.shadows.ShadowLog

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataEvaluatorTest {
    @get:Rule val expect = Expect.create()

    @OptIn(ExperimentalCoroutinesApi::class) // StandardTestDispatcher no longer experimental.
    private val dispatcher: CoroutineDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    @Test
    fun evaluate_noExpression_returnsUnevaluated() = runBlocking {
        val data = WireComplicationData.Builder(TYPE_NO_DATA).setRangedValue(10f).build()

        val evaluator = ComplicationDataEvaluator()

        assertThat(evaluator.evaluate(data).firstOrNull()).isEqualTo(data)
    }

    /**
     * Scenarios for testing data with expressions.
     *
     * Each scenario describes the expressed data, the flow of states, and the flow of the evaluated
     * data output.
     */
    enum class DataWithExpressionScenario(
        val expressed: WireComplicationData,
        val states: List<Map<AppDataKey<*>, DynamicDataValue<*>>>,
        val evaluated: List<WireComplicationData>,
    ) {
        SET_IMMEDIATELY_WHEN_ALL_DATA_AVAILABLE(
            expressed =
                WireComplicationData.Builder(TYPE_NO_DATA)
                    .setRangedDynamicValue(DynamicFloat.constant(1f))
                    .setLongText(WireComplicationText(DynamicString.constant("Long Text")))
                    .setLongTitle(WireComplicationText(DynamicString.constant("Long Title")))
                    .setShortText(WireComplicationText(DynamicString.constant("Short Text")))
                    .setShortTitle(WireComplicationText(DynamicString.constant("Short Title")))
                    .setContentDescription(
                        WireComplicationText(DynamicString.constant("Description"))
                    )
                    .setPlaceholder(constantData("Placeholder"))
                    .setListEntryCollection(listOf(constantData("List")))
                    .build()
                    .also { it.setTimelineEntryCollection(listOf(constantData("Timeline"))) },
            states = listOf(),
            evaluated =
                listOf(
                    WireComplicationData.Builder(TYPE_NO_DATA)
                        .setRangedValue(1f)
                        .setLongText(WireComplicationText("Long Text"))
                        .setLongTitle(WireComplicationText("Long Title"))
                        .setShortText(WireComplicationText("Short Text"))
                        .setShortTitle(WireComplicationText("Short Title"))
                        .setContentDescription(WireComplicationText("Description"))
                        .setPlaceholder(evaluatedData("Placeholder"))
                        .setListEntryCollection(listOf(evaluatedData("List")))
                        .build()
                        .also { it.setTimelineEntryCollection(listOf(evaluatedData("Timeline"))) },
                ),
        ),
        SET_ONLY_AFTER_ALL_FIELDS_EVALUATED(
            expressed =
                WireComplicationData.Builder(TYPE_NO_DATA)
                    .setRangedDynamicValue(DynamicFloat.from(AppDataKey("ranged_value")))
                    .setLongText(WireComplicationText(DynamicString.from(AppDataKey("long_text"))))
                    .setLongTitle(
                        WireComplicationText(DynamicString.from(AppDataKey("long_title")))
                    )
                    .setShortText(
                        WireComplicationText(DynamicString.from(AppDataKey("short_text")))
                    )
                    .setShortTitle(
                        WireComplicationText(DynamicString.from(AppDataKey("short_title")))
                    )
                    .setContentDescription(
                        WireComplicationText(DynamicString.from(AppDataKey("description")))
                    )
                    .setPlaceholder(stateData("placeholder"))
                    .setListEntryCollection(listOf(stateData("list")))
                    .build()
                    .also { it.setTimelineEntryCollection(listOf(stateData("timeline"))) },
            states =
                aggregate(
                    // Each map piles on top of the previous ones.
                    mapOf(
                        AppDataKey<DynamicFloat>("ranged_value") to DynamicDataValue.fromFloat(1f)
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("long_text") to
                            DynamicDataValue.fromString("Long Text")
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("long_title") to
                            DynamicDataValue.fromString("Long Title")
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("short_text") to
                            DynamicDataValue.fromString("Short Text")
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("short_title") to
                            DynamicDataValue.fromString("Short Title")
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("description") to
                            DynamicDataValue.fromString("Description")
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("placeholder") to
                            DynamicDataValue.fromString("Placeholder")
                    ),
                    mapOf(AppDataKey<DynamicString>("list") to DynamicDataValue.fromString("List")),
                    mapOf(
                        AppDataKey<DynamicString>("timeline") to
                            DynamicDataValue.fromString("Timeline")
                    ),
                    // Only the last one will trigger an evaluated data.
                ),
            evaluated =
                listOf(
                    // Before any state is available.
                    INVALID_DATA,
                    // INVALID_DATA with placeholder, after it's available (and others aren't).
                    WireComplicationData.Builder(INVALID_DATA)
                        .setPlaceholder(evaluatedData("Placeholder"))
                        .build(),
                    // Evaluated data with after everything is available.
                    WireComplicationData.Builder(TYPE_NO_DATA)
                        .setRangedValue(1f)
                        .setLongText(WireComplicationText("Long Text"))
                        .setLongTitle(WireComplicationText("Long Title"))
                        .setShortText(WireComplicationText("Short Text"))
                        .setShortTitle(WireComplicationText("Short Title"))
                        .setContentDescription(WireComplicationText("Description"))
                        // Not trimmed for TYPE_NO_DATA.
                        .setPlaceholder(evaluatedData("Placeholder"))
                        .setListEntryCollection(listOf(evaluatedData("List")))
                        .build()
                        .also { it.setTimelineEntryCollection(listOf(evaluatedData("Timeline"))) },
                ),
        ),
        SET_TO_EVALUATED_IF_ALL_FIELDS_VALID(
            expressed =
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.from(AppDataKey("valid"))))
                    .setShortText(WireComplicationText(DynamicString.from(AppDataKey("valid"))))
                    .build(),
            states =
                listOf(
                    mapOf(
                        AppDataKey<DynamicString>("valid") to DynamicDataValue.fromString("Valid")
                    ),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // Before state is available.
                    WireComplicationData.Builder(TYPE_SHORT_TEXT)
                        .setShortTitle(WireComplicationText("Valid"))
                        .setShortText(WireComplicationText("Valid"))
                        .build(),
                ),
        ),
        SET_TO_NO_DATA_IF_FIRST_STATE_IS_INVALID(
            expressed =
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.from(AppDataKey("valid"))))
                    .setShortText(WireComplicationText(DynamicString.from(AppDataKey("invalid"))))
                    .build(),
            states =
                listOf(
                    mapOf(),
                    mapOf(
                        AppDataKey<DynamicString>("valid") to DynamicDataValue.fromString("Valid")
                    ),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // States invalid after one field changed to valid.
                ),
        ),
        SET_TO_NO_DATA_IF_LAST_STATE_IS_INVALID(
            expressed =
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortTitle(WireComplicationText(DynamicString.from(AppDataKey("valid"))))
                    .setShortText(WireComplicationText(DynamicString.from(AppDataKey("invalid"))))
                    .build(),
            states =
                listOf(
                    mapOf(
                        AppDataKey<DynamicString>("valid") to DynamicDataValue.fromString("Valid"),
                        AppDataKey<DynamicString>("invalid") to
                            DynamicDataValue.fromString("Valid"),
                    ),
                    mapOf(
                        AppDataKey<DynamicString>("valid") to DynamicDataValue.fromString("Valid")
                    ),
                ),
            evaluated =
                listOf(
                    INVALID_DATA, // Before state is available.
                    WireComplicationData.Builder(TYPE_SHORT_TEXT)
                        .setShortTitle(WireComplicationText("Valid"))
                        .setShortText(WireComplicationText("Valid"))
                        .build(),
                    INVALID_DATA, // After it was invalidated.
                ),
        ),
        SET_TO_EVALUATED_WITHOUT_PLACEHOLDER_IF_NOT_NO_DATA(
            expressed =
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText("Text"))
                    .setPlaceholder(evaluatedData("Placeholder"))
                    .build(),
            states = listOf(),
            evaluated =
                listOf(
                    // No placeholder.
                    WireComplicationData.Builder(TYPE_SHORT_TEXT)
                        .setShortText(WireComplicationText("Text"))
                        .build(),
                )
        ),
        SET_TO_EVALUATED_WITHOUT_PLACEHOLDER_EVEN_IF_PLACEHOLDER_INVALID_IF_NOT_NO_DATA(
            expressed =
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText("Text"))
                    .setPlaceholder(stateData("placeholder"))
                    .build(),
            states = listOf(), // placeholder state not set.
            evaluated =
                listOf(
                    // No placeholder.
                    WireComplicationData.Builder(TYPE_SHORT_TEXT)
                        .setShortText(WireComplicationText("Text"))
                        .build(),
                )
        ),
    }

    @Test
    fun evaluate_withExpression_returnsEvaluated() = runBlocking {
        for (scenario in DataWithExpressionScenario.values()) {
            // Defensive copy due to in-place evaluation.
            val expressed = WireComplicationData.Builder(scenario.expressed).build()
            val stateStore = StateStore(mapOf())
            val evaluator = ComplicationDataEvaluator(stateStore)
            val allEvaluations =
                evaluator
                    .evaluate(expressed)
                    .shareIn(
                        CoroutineScope(dispatcher),
                        SharingStarted.Eagerly,
                        replay = 10,
                    )

            advanceUntilIdle()
            for (state in scenario.states) {
                stateStore.setAppStateEntryValues(state)
                advanceUntilIdle()
            }

            expect
                .withMessage(scenario.name)
                .that(allEvaluations.replayCache)
                .isEqualTo(scenario.evaluated)
        }
    }

    @Test
    fun evaluate_cancelled_cleansUp() = runBlocking {
        // Arrange
        val expressed =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setRangedDynamicValue(PlatformHealthSources.heartRateBpm())
                .build()

        val provider = mock<PlatformDataProvider>()
        val evaluator = ComplicationDataEvaluator(
            platformDataProviders = mapOf(
                provider to setOf(PlatformHealthSources.Keys.HEART_RATE_BPM)
            )
        )
        val flow = evaluator.evaluate(expressed)

        // Validity check - Platform provider not used until Flow collection.
        verifyNoInteractions(provider)
        val job = launch(dispatcher) { flow.collect {} }
        try {
            advanceUntilIdle()
            // Validity check - Platform provider registered while collection is in progress.
            verify(provider).setReceiver(any(), any())
            verifyNoMoreInteractions(provider)
        } finally {
            // Act
            job.cancel()
            advanceUntilIdle()
        }

        // Assert
        advanceUntilIdle()
        verify(provider).clearReceiver()
        verifyNoMoreInteractions(provider)
    }

    @Test
    fun evaluate_keepExpression_doesNotTrimUnevaluatedExpression() = runBlocking {
        val expressed =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setRangedDynamicValue(DynamicFloat.constant(1f))
                .setLongText(WireComplicationText(DynamicString.constant("Long Text")))
                .setLongTitle(WireComplicationText(DynamicString.constant("Long Title")))
                .setShortText(WireComplicationText(DynamicString.constant("Short Text")))
                .setShortTitle(WireComplicationText(DynamicString.constant("Short Title")))
                .setContentDescription(WireComplicationText(DynamicString.constant("Description")))
                .setPlaceholder(constantData("Placeholder"))
                .setListEntryCollection(listOf(constantData("List")))
                .build()
                .also { it.setTimelineEntryCollection(listOf(constantData("Timeline"))) }
        val evaluator = ComplicationDataEvaluator(keepDynamicValues = true)

        assertThat(evaluator.evaluate(expressed).firstOrNull())
            .isEqualTo(
                WireComplicationData.Builder(TYPE_NO_DATA)
                    .setRangedValue(1f)
                    .setRangedDynamicValue(DynamicFloat.constant(1f))
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
                    .setPlaceholder(evaluatedWithConstantData("Placeholder"))
                    .setListEntryCollection(listOf(evaluatedWithConstantData("List")))
                    .build()
                    .also {
                        it.setTimelineEntryCollection(listOf(evaluatedWithConstantData("Timeline")))
                    },
            )
    }

    @Test
    fun evaluate_keepExpressionNotNoData_doesNotTrimPlaceholder() = runBlocking {
        val expressed =
            WireComplicationData.Builder(TYPE_SHORT_TEXT)
                .setShortText(WireComplicationText("Text"))
                .setPlaceholder(evaluatedData("Placeholder"))
                .build()
        val evaluator = ComplicationDataEvaluator(keepDynamicValues = true)

        assertThat(evaluator.evaluate(expressed).firstOrNull())
            .isEqualTo(
                WireComplicationData.Builder(TYPE_SHORT_TEXT)
                    .setShortText(WireComplicationText("Text"))
                    .setPlaceholder(evaluatedData("Placeholder"))
                    .build()
            )
    }

    private fun advanceUntilIdle() {
        @OptIn(ExperimentalCoroutinesApi::class) // StandardTestDispatcher no longer experimental.
        (dispatcher as TestDispatcher).scheduler.advanceUntilIdle()
    }

    private companion object {
        /** Converts `[{a: A}, {b: B}, {c: C}]` to `[{a: A}, {a: A, b: B}, {a: A, b: B, c: C}]`. */
        fun <K, V> aggregate(vararg maps: Map<K, V>): List<Map<K, V>> =
            maps.fold(listOf()) { acc, map -> acc + ((acc.lastOrNull() ?: mapOf()) + map) }

        fun constantData(value: String) =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setLongText(WireComplicationText(DynamicString.constant(value)))
                .build()

        fun stateData(value: String) =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setLongText(WireComplicationText(DynamicString.from(AppDataKey(value))))
                .build()

        fun evaluatedData(value: String) =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setLongText(WireComplicationText(value))
                .build()

        fun evaluatedWithConstantData(value: String) =
            WireComplicationData.Builder(TYPE_NO_DATA)
                .setLongText(WireComplicationText(value, DynamicString.constant(value)))
                .build()
    }
}
