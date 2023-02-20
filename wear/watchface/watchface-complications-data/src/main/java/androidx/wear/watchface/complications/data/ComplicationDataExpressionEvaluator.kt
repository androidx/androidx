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
import android.support.wearable.complications.ComplicationText as WireComplicationText
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.pipeline.BoundDynamicType
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import androidx.wear.protolayout.expression.pipeline.ObservableStateStore
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway
import java.util.concurrent.Executor
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Evaluates a [WireComplicationData] with
 * [androidx.wear.protolayout.expression.DynamicBuilders.DynamicType] within its fields.
 *
 * Due to [WireComplicationData]'s shallow copy strategy the input is modified in-place.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComplicationDataExpressionEvaluator(
    val unevaluatedData: WireComplicationData,
    private val stateStore: ObservableStateStore = ObservableStateStore(emptyMap()),
    private val sensorGateway: SensorGateway? = null,
) : AutoCloseable {
    /**
     * Java compatibility class for [ComplicationDataExpressionEvaluator].
     *
     * Unlike [data], [listener] is not invoked until there is a value (until [data] is non-null).
     */
    class Compat
    @JvmOverloads
    constructor(
        val unevaluatedData: WireComplicationData,
        private val listener: Consumer<WireComplicationData>,
        stateStore: ObservableStateStore = ObservableStateStore(emptyMap()),
        sensorGateway: SensorGateway? = null,
    ) : AutoCloseable {
        private val evaluator =
            ComplicationDataExpressionEvaluator(unevaluatedData, stateStore, sensorGateway)

        /**
         * @see ComplicationDataExpressionEvaluator.init, [executor] is used in place of
         *   `coroutineScope`.
         */
        fun init(executor: Executor) {
            evaluator.init(CoroutineScope(executor.asCoroutineDispatcher()))
            evaluator.data
                .filterNotNull()
                .onEach(listener::accept)
                .launchIn(CoroutineScope(executor.asCoroutineDispatcher()))
        }

        /** @see ComplicationDataExpressionEvaluator.close */
        override fun close() {
            evaluator.close()
        }
    }

    private val _data = MutableStateFlow<WireComplicationData?>(null)

    /**
     * The evaluated data, or `null` if it wasn't evaluated yet, or [NoDataComplicationData] if it
     * wasn't possible to evaluate the [unevaluatedData].
     */
    val data: StateFlow<WireComplicationData?> = _data.asStateFlow()

    @Volatile // In case init() and close() are called on different threads.
    private lateinit var evaluator: DynamicTypeEvaluator
    private val state = MutableStateFlow(State(unevaluatedData))

    /**
     * Parses the expression and starts blocking evaluation.
     *
     * This needs to be called exactly once.
     *
     * @param coroutineScope used for background evaluation
     */
    fun init(coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        // Add all the receivers before we start binding them because binding can synchronously
        // trigger the receiver, which would update the data before all the fields are evaluated.
        initStateReceivers(coroutineScope)
        initEvaluator()
        monitorState(coroutineScope)
    }

    /**
     * Stops evaluation.
     *
     * [data] will not change after this is called.
     */
    override fun close() {
        for (receiver in state.value.all) receiver.close()
        if (this::evaluator.isInitialized) evaluator.close()
    }

    /** Adds [ComplicationEvaluationResultReceiver]s to [state]. */
    private fun initStateReceivers(coroutineScope: CoroutineScope) {
        val receivers = mutableSetOf<ComplicationEvaluationResultReceiver<out Any>>()

        if (unevaluatedData.hasRangedValueExpression()) {
            unevaluatedData.rangedValueExpression
                ?.buildReceiver(coroutineScope) { setRangedValue(it) }
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasLongText()) {
            unevaluatedData.longText
                ?.buildReceiver(coroutineScope) { setLongText(it) }
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasLongTitle()) {
            unevaluatedData.longTitle
                ?.buildReceiver(coroutineScope) { setLongTitle(it) }
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasShortText()) {
            unevaluatedData.shortText
                ?.buildReceiver(coroutineScope) { setShortText(it) }
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasShortTitle()) {
            unevaluatedData.shortTitle
                ?.buildReceiver(coroutineScope) { setShortTitle(it) }
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasContentDescription()) {
            unevaluatedData.contentDescription
                ?.buildReceiver(coroutineScope) { setContentDescription(it) }
                ?.let { receivers += it }
        }

        state.value = State(unevaluatedData, receivers)
    }

    private fun DynamicFloat.buildReceiver(
        coroutineScope: CoroutineScope,
        setter: WireComplicationData.Builder.(Float) -> WireComplicationData.Builder
    ) =
        ComplicationEvaluationResultReceiver(
            setter,
            binder = { receiver ->
                evaluator.bind(
                    this@buildReceiver,
                    coroutineScope.coroutineContext.asExecutor(),
                    receiver
                )
            },
        )

    private fun WireComplicationText.buildReceiver(
        coroutineScope: CoroutineScope,
        setter: WireComplicationData.Builder.(WireComplicationText) -> WireComplicationData.Builder
    ) =
        expression?.let { expression ->
            ComplicationEvaluationResultReceiver<String>(
                setter = { setter(WireComplicationText(it, expression)) },
                binder = { receiver ->
                    evaluator.bind(
                        expression,
                        ULocale.getDefault(),
                        coroutineScope.coroutineContext.asExecutor(),
                        receiver
                    )
                },
            )
        }

    /** Initializes the internal [DynamicTypeEvaluator] if there are pending receivers. */
    private fun initEvaluator() {
        if (state.value.pending.isEmpty()) return
        evaluator =
            DynamicTypeEvaluator(
                /* platformDataSourcesInitiallyEnabled = */ true,
                sensorGateway,
                stateStore,
            )
        for (receiver in state.value.pending) receiver.bind()
        for (receiver in state.value.pending) receiver.startEvaluation()
        evaluator.enablePlatformDataSources()
    }

    /** Monitors [state] changes and updates [data]. */
    private fun monitorState(coroutineScope: CoroutineScope) {
        state
            .onEach {
                if (it.invalid.isNotEmpty()) _data.value = INVALID_DATA
                else if (it.pending.isEmpty()) _data.value = it.data
            }
            .launchIn(coroutineScope)
    }

    /**
     * Holds the state of the continuously evaluated [WireComplicationData] and the various
     * [ComplicationEvaluationResultReceiver] that are evaluating it.
     */
    private class State(
        val data: WireComplicationData,
        val pending: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val invalid: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val complete: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
    ) {
        val all = pending + invalid + complete

        fun withInvalid(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            State(data, pending - receiver, invalid + receiver, complete - receiver)

        fun withComplete(
            data: WireComplicationData,
            receiver: ComplicationEvaluationResultReceiver<out Any>,
        ) = State(data, pending - receiver, invalid - receiver, complete + receiver)
    }

    private inner class ComplicationEvaluationResultReceiver<T : Any>(
        private val setter: WireComplicationData.Builder.(T) -> WireComplicationData.Builder,
        private val binder: (ComplicationEvaluationResultReceiver<T>) -> BoundDynamicType,
    ) : DynamicTypeValueReceiver<T>, AutoCloseable {
        @Volatile // In case bind() and close() are called on different threads.
        private lateinit var boundDynamicType: BoundDynamicType

        fun bind() {
            boundDynamicType = binder(this)
        }

        fun startEvaluation() {
            boundDynamicType.startEvaluation()
        }

        override fun close() {
            boundDynamicType.close()
        }

        override fun onPreUpdate() {}

        override fun onData(newData: T) {
            state.update {
                it.withComplete(
                    setter(WireComplicationData.Builder(it.data), newData).build(),
                    this
                )
            }
        }

        override fun onInvalidated() {
            state.update { it.withInvalid(this) }
        }
    }

    companion object {
        val INVALID_DATA: WireComplicationData = NoDataComplicationData().asWireComplicationData()

        fun hasExpression(data: WireComplicationData): Boolean =
            data.run {
                (hasRangedValueExpression() && rangedValueExpression != null) ||
                    (hasLongText() && longText?.expression != null) ||
                    (hasLongTitle() && longTitle?.expression != null) ||
                    (hasShortText() && shortText?.expression != null) ||
                    (hasShortTitle() && shortTitle?.expression != null) ||
                    (hasContentDescription() && contentDescription?.expression != null)
            }
    }
}

internal fun CoroutineContext.asExecutor(): Executor =
    (get(ContinuationInterceptor) as CoroutineDispatcher).asExecutor()
