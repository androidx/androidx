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

import android.icu.util.ULocale
import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.support.wearable.complications.ComplicationData.Companion.TYPE_NO_DATA
import android.support.wearable.complications.ComplicationText as WireComplicationText
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.PlatformDataKey
import androidx.wear.protolayout.expression.pipeline.BoundDynamicType
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider
import androidx.wear.protolayout.expression.pipeline.PlatformTimeUpdateNotifier
import androidx.wear.protolayout.expression.pipeline.StateStore
import java.time.Instant
import java.util.concurrent.Executor
import java.util.function.Supplier
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

/**
 * Evaluates a [WireComplicationData] with
 * [androidx.wear.protolayout.expression.DynamicBuilders.DynamicType] within its fields.
 *
 * All constructor parameters are forwarded to [DynamicTypeEvaluator.Config.Builder].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComplicationDataEvaluator
@VisibleForTesting
constructor(
    private val stateStore: StateStore? = StateStore(emptyMap()),
    private val platformTimeUpdateNotifier: PlatformTimeUpdateNotifier? = null,
    private val platformDataProviders: Map<PlatformDataProvider, Set<PlatformDataKey<*>>> = mapOf(),
    private val keepDynamicValues: Boolean = false,
    private val clock: Supplier<Instant>? = null,
) {
    constructor(
        stateStore: StateStore? = StateStore(emptyMap()),
        platformTimeUpdateNotifier: PlatformTimeUpdateNotifier? = null,
        platformDataProviders: Map<PlatformDataProvider, Set<PlatformDataKey<*>>> = mapOf(),
        keepDynamicValues: Boolean = false,
    ) : this(
        stateStore,
        platformTimeUpdateNotifier,
        platformDataProviders,
        keepDynamicValues,
        clock = null,
    )

    private val evaluator =
        DynamicTypeEvaluator(
            DynamicTypeEvaluator.Config.Builder()
                .apply { stateStore?.let { setStateStore(it) } }
                .apply { platformTimeUpdateNotifier?.let { setPlatformTimeUpdateNotifier(it) } }
                .apply {
                    for ((platformDataProvider, dataKeys) in platformDataProviders) {
                        addPlatformDataProvider(platformDataProvider, dataKeys)
                    }
                }
                .apply { clock?.let { @Suppress("VisibleForTests") setClock(it) } }
                .build()
        )

    /**
     * Returns a [Flow] that provides the evaluated [WireComplicationData].
     *
     * The dynamic values are evaluated _separately_ on each flow collection.
     */
    fun evaluate(unevaluatedData: WireComplicationData): Flow<WireComplicationData> =
        evaluateTopLevelFields(unevaluatedData)
            // Combining with fields that are made of WireComplicationData.
            .combineWithDataList(unevaluatedData.timelineEntries) { entries ->
                // Timeline entries are set on the built WireComplicationData.
                WireComplicationData.Builder(
                    this@combineWithDataList.build().apply { setTimelineEntryCollection(entries) }
                )
            }
            .combineWithDataList(unevaluatedData.listEntries) { setListEntryCollection(it) }
            // Must be last, as it overwrites INVALID_DATA.
            .combineWithEvaluatedPlaceholder(unevaluatedData.placeholder)
            .distinctUntilChanged()

    /** Evaluates "local" fields, excluding fields of type [WireComplicationData]. */
    private fun evaluateTopLevelFields(
        unevaluatedData: WireComplicationData
    ): Flow<WireComplicationData> {
        // Combine setter flows into one flow...
        return combine(
            unevaluatedData.topLevelSetterFlows().ifEmpty {
                return flowOf(unevaluatedData) // If no field needs evaluation, don't combine.
            }
        ) { setters ->
            // ... that builds the data from all the setters.
            setters
                .fold(WireComplicationData.Builder(unevaluatedData)) { builder, setter ->
                    setter(builder) ?: return@combine INVALID_DATA
                }
                .build()
        }
    }

    /**
     * Returns list of [Flow]s describing how to build the [WireComplicationData] based on dynamic
     * values in "local" fields, excluding fields of type [WireComplicationData].
     *
     * When evaluation is triggered, the [Flow] emits a method that sets field(s) in the provided
     * [WireComplicationData.Builder].
     *
     * Each `bindX` call returns a [Flow] of [WireComplicationDataSetter] that sets the provided
     * fields based on the type (e.g. [Float] vs [String]), and potentially trims the dynamic value
     * (based on [keepDynamicValues]).
     */
    private fun WireComplicationData.topLevelSetterFlows(): List<Flow<WireComplicationDataSetter>> =
        buildList {
            if (hasRangedDynamicValue() && rangedDynamicValue != null) {
                add(
                    rangedDynamicValue!!
                        .evaluate()
                        .toDataSetter(
                            setter = { setRangedValue(it) },
                            dynamicValueTrimmer = { setRangedDynamicValue(null) },
                        )
                )
            }
            if (hasLongText() && longText?.dynamicValue != null) {
                add(longText!!.dynamicValue!!.evaluateToTextSetter { setLongText(it) })
            }
            if (hasLongTitle() && longTitle?.dynamicValue != null) {
                add(longTitle!!.dynamicValue!!.evaluateToTextSetter { setLongTitle(it) })
            }
            if (hasShortText() && shortText?.dynamicValue != null) {
                add(shortText!!.dynamicValue!!.evaluateToTextSetter { setShortText(it) })
            }
            if (hasShortTitle() && shortTitle?.dynamicValue != null) {
                add(shortTitle!!.dynamicValue!!.evaluateToTextSetter { setShortTitle(it) })
            }
            if (hasContentDescription() && contentDescription?.dynamicValue != null) {
                add(
                    contentDescription!!.dynamicValue!!.evaluateToTextSetter {
                        setContentDescription(it)
                    }
                )
            }
        }

    /**
     * Combines the receiver with the evaluated version of the provided list.
     *
     * If the receiver [Flow] emits [INVALID_DATA] or the input list is null or empty, this does not
     * mutate the flow and does not wait for the entries to finish evaluating.
     *
     * If even one [WireComplicationData] within the provided list is evaluated to [INVALID_DATA],
     * the output [Flow] becomes [INVALID_DATA] (the receiver [Flow] is ignored).
     */
    private fun Flow<WireComplicationData>.combineWithDataList(
        unevaluatedEntries: List<WireComplicationData>?,
        setter:
            WireComplicationData.Builder.(
                List<WireComplicationData>
            ) -> WireComplicationData.Builder,
    ): Flow<WireComplicationData> {
        if (unevaluatedEntries.isNullOrEmpty()) return this
        val evaluatedEntriesFlow: Flow<Array<WireComplicationData>> =
            combine(unevaluatedEntries.map { evaluate(it) }) { it }

        return this.combine(evaluatedEntriesFlow) {
            data: WireComplicationData,
            evaluatedEntries: Array<WireComplicationData> ->

            // Not mutating if invalid.
            if (data === INVALID_DATA) return@combine data
            // An entry is invalid, emitting invalid.
            if (evaluatedEntries.any { it === INVALID_DATA }) return@combine INVALID_DATA
            // All is well, mutating the input.
            return@combine WireComplicationData.Builder(data)
                .setter(evaluatedEntries.toList())
                .build()
        }
    }

    /**
     * Same as [combineWithDataList], but sets the evaluated placeholder ONLY when the receiver
     * [Flow] emits [TYPE_NO_DATA], or [keepDynamicValues] is true, otherwise clears it and does not
     * wait for the placeholder to finish evaluating.
     *
     * If the placeholder is not required (per the above paragraph), this doesn't wait for it.
     */
    private fun Flow<WireComplicationData>.combineWithEvaluatedPlaceholder(
        unevaluatedPlaceholder: WireComplicationData?
    ): Flow<WireComplicationData> {
        if (unevaluatedPlaceholder == null) return this
        val evaluatedPlaceholderFlow: Flow<WireComplicationData> = evaluate(unevaluatedPlaceholder)

        return this.combine(evaluatedPlaceholderFlow) {
            data: WireComplicationData,
            evaluatedPlaceholder: WireComplicationData ->
            if (!keepDynamicValues && data.type != TYPE_NO_DATA) {
                // Clearing the placeholder when data is not TYPE_NO_DATA (it was meant as an
                // dynamic value fallback).
                return@combine WireComplicationData.Builder(data).setPlaceholder(null).build()
            }
            // Placeholder required but invalid, emitting invalid.
            if (evaluatedPlaceholder === INVALID_DATA) return@combine INVALID_DATA
            // All is well, mutating the input.
            return@combine WireComplicationData.Builder(data)
                .setPlaceholder(evaluatedPlaceholder)
                .build()
        }
    }

    /** Converts a generic flow to a [WireComplicationDataSetter]. */
    private fun <T : Any> Flow<T?>.toDataSetter(
        setter: WireComplicationData.Builder.(T) -> WireComplicationData.Builder,
        dynamicValueTrimmer: WireComplicationData.Builder.() -> WireComplicationData.Builder,
    ): Flow<WireComplicationDataSetter> {
        return map { value ->
            if (value == null) return@map { null } // emit invalidating setter.
            { builder ->
                var newBuilder = setter(builder, value)
                if (!keepDynamicValues) newBuilder = dynamicValueTrimmer(newBuilder)
                newBuilder
            }
        }
    }

    /**
     * Evaluates a [DynamicString] and converts it to a [WireComplicationDataSetter].
     *
     * This combines [DynamicString.evaluate] and [toDataSetter] because the trimming requires the
     * evaluated [DynamicString], so combining it avoids mentioning it twice, i.e.:
     * ```
     * dynamicString.evaluate().toTextSetter(dynamicString) { ... }
     * ```
     */
    private fun DynamicString.evaluateToTextSetter(
        setter: WireComplicationData.Builder.(WireComplicationText) -> WireComplicationData.Builder,
    ): Flow<WireComplicationDataSetter> =
        evaluate()
            .toDataSetter(
                setter = { value ->
                    if (keepDynamicValues) {
                        setter(WireComplicationText(value, this@evaluateToTextSetter))
                    } else {
                        setter(WireComplicationText(value))
                    }
                },
                dynamicValueTrimmer = { this }, // Trimming is done in setter.
            )

    /**
     * Binds a [DynamicFloat], returning a [Flow] of [Float] or `null` if the binding is
     * invalidated.
     */
    private fun DynamicFloat.evaluate(): Flow<Float?> {
        return evaluateDynamicType(
            bindingRequest = { executor, receiver ->
                DynamicTypeBindingRequest.forDynamicFloat(this@evaluate, executor, receiver)
            }
        )
    }

    /**
     * Binds a [DynamicString], returning a [Flow] of [String] or `null` if the binding is
     * invalidated.
     */
    private fun DynamicString.evaluate(): Flow<String?> {
        return evaluateDynamicType(
            bindingRequest = { executor, receiver ->
                DynamicTypeBindingRequest.forDynamicString(
                    this@evaluate,
                    ULocale.getDefault(),
                    executor,
                    receiver,
                )
            }
        )
    }

    /**
     * Converts [DynamicTypeEvaluator.bind] to [Flow], emitting `null` when the binding is
     * invalidated.
     *
     * The actual implementation of [DynamicTypeValueReceiver] is separated to the helper class
     * [DynamicTypeValueReceiverToChannel].
     */
    private fun <T : Any> evaluateDynamicType(
        bindingRequest: (Executor, DynamicTypeValueReceiver<T>) -> DynamicTypeBindingRequest,
    ): Flow<T?> =
        callbackFlow {
                // Binding DynamicTypeEvaluator to the provided binding request.
                val boundDynamicType: BoundDynamicType =
                    evaluator.bind(
                        bindingRequest(
                            currentCoroutineContext().asExecutor(),
                            // Emitting values to the callbackFlow's channel.
                            DynamicTypeValueReceiverToChannel(channel)
                        )
                    )
                // Start evaluation.
                // TODO(b/267599473): Remove dispatches when DynamicTypeEvaluator is thread safe.
                Dispatchers.Main.immediate { boundDynamicType.startEvaluation() }
                awaitClose {
                    // Stop evaluation when the Flow (created by callbackFlow) is closed.
                    CoroutineScope(Dispatchers.Main.immediate).launch { boundDynamicType.close() }
                }
            }
            .conflate() // We only care about the latest data for each field.

    /**
     * Converts [DynamicTypeValueReceiver] into a [SendChannel] (from a [callbackFlow]).
     *
     * [onData] emits the value, [onInvalidated] emits `null`.
     */
    private class DynamicTypeValueReceiverToChannel<T : Any>(
        private val channel: SendChannel<T?>,
    ) : DynamicTypeValueReceiver<T> {
        override fun onData(newData: T) {
            channel
                .trySend(newData)
                // Shouldn't fail for overflow as we conflate the flow.
                .onFailure { e -> Log.e(TAG, "Failed sending dynamic update.", e) }
        }

        override fun onInvalidated() {
            channel
                .trySend(null)
                // Shouldn't fail for overflow as we conflate the flow.
                .onFailure { e -> Log.e(TAG, "Failed sending dynamic update.", e) }
        }
    }

    companion object {
        private const val TAG = "ComplicationDataEvaluator"

        val INVALID_DATA: WireComplicationData = NoDataComplicationData().asWireComplicationData()
    }
}

/**
 * Describes a method that sets values on the [WireComplicationData.Builder]. When field is
 * invalidated, the method should return `null`.
 */
private typealias WireComplicationDataSetter =
    (WireComplicationData.Builder) -> WireComplicationData.Builder?

/**
 * Replacement for [kotlinx.coroutines.asExecutor] extension due to
 * https://github.com/Kotlin/kotlinx.coroutines/pull/3683.
 */
internal fun CoroutineContext.asExecutor() = Executor { runnable ->
    val dispatcher = this[ContinuationInterceptor] as CoroutineDispatcher
    if (dispatcher.isDispatchNeeded(this)) {
        dispatcher.dispatch(this, runnable)
    } else {
        runnable.run()
    }
}
