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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

/**
 * Evaluates a [WireComplicationData] with
 * [androidx.wear.protolayout.expression.DynamicBuilders.DynamicType] within its fields.
 *
 * Due to [WireComplicationData]'s shallow copy strategy the input is modified in-place.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComplicationDataExpressionEvaluator(
    private val stateStore: ObservableStateStore = ObservableStateStore(emptyMap()),
    private val sensorGateway: SensorGateway? = null,
    private val keepExpression: Boolean = false,
) {
    /**
     * Returns a [Flow] that provides the evaluated [WireComplicationData].
     *
     * The expression is evaluated _separately_ on each flow collection.
     */
    fun evaluate(unevaluatedData: WireComplicationData) =
        flow<WireComplicationData> {
            val state: MutableStateFlow<State> = unevaluatedData.buildState()
            state.value.use {
                val evaluatedData: Flow<WireComplicationData> =
                    state
                        .mapNotNull {
                            when {
                                // Emitting INVALID_DATA if there's an invalid receiver.
                                it.invalidReceivers.isNotEmpty() -> INVALID_DATA
                                // Emitting the data if all pending receivers are done and all
                                // pre-updates are satisfied.
                                it.pendingReceivers.isEmpty() && it.preUpdateCount == 0 -> it.data
                                // Skipping states that are not ready for be emitted.
                                else -> null
                            }
                        }
                        .distinctUntilChanged()
                emitAll(evaluatedData)
            }
        }

    private suspend fun WireComplicationData.buildState() =
        MutableStateFlow(State(this)).apply {
            if (hasRangedValueExpression()) {
                addReceiver(
                    rangedValueExpression,
                    expressionTrimmer = { setRangedValueExpression(null) },
                    setter = { setRangedValue(it) },
                )
            }
            if (hasLongText()) addReceiver(longText) { setLongText(it) }
            if (hasLongTitle()) addReceiver(longTitle) { setLongTitle(it) }
            if (hasShortText()) addReceiver(shortText) { setShortText(it) }
            if (hasShortTitle()) addReceiver(shortTitle) { setShortTitle(it) }
            if (hasContentDescription()) {
                addReceiver(contentDescription) { setContentDescription(it) }
            }
            // Add all the receivers before we start binding them because binding can synchronously
            // trigger the receiver, which would update the data before all the fields are
            // evaluated.
            value.initEvaluation()
        }

    private suspend fun MutableStateFlow<State>.addReceiver(
        expression: DynamicFloat?,
        expressionTrimmer: WireComplicationData.Builder.() -> WireComplicationData.Builder,
        setter: WireComplicationData.Builder.(Float) -> WireComplicationData.Builder,
    ) {
        expression ?: return
        val executor = currentCoroutineContext().asExecutor()
        update { state ->
            state.withPendingReceiver(
                ComplicationEvaluationResultReceiver<Float>(
                    this,
                    setter = { value ->
                        if (!keepExpression) expressionTrimmer(this)
                        setter(this, value)
                    },
                    binder = { receiver -> value.evaluator.bind(expression, executor, receiver) },
                )
            )
        }
    }

    private suspend fun MutableStateFlow<State>.addReceiver(
        text: WireComplicationText?,
        setter: WireComplicationData.Builder.(WireComplicationText) -> WireComplicationData.Builder,
    ) {
        val expression = text?.expression ?: return
        val executor = currentCoroutineContext().asExecutor()
        update {
            it.withPendingReceiver(
                ComplicationEvaluationResultReceiver<String>(
                    this,
                    setter = { value ->
                        setter(
                            if (keepExpression) {
                                WireComplicationText(value, expression)
                            } else {
                                WireComplicationText(value)
                            }
                        )
                    },
                    binder = { receiver ->
                        value.evaluator.bind(expression, ULocale.getDefault(), executor, receiver)
                    },
                )
            )
        }
    }

    /**
     * Holds the state of the continuously evaluated [WireComplicationData] and the various
     * [ComplicationEvaluationResultReceiver] that are evaluating it.
     */
    private inner class State(
        val data: WireComplicationData,
        val pendingReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val invalidReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val completeReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val preUpdateCount: Int = 0,
    ) : AutoCloseable {
        lateinit var evaluator: DynamicTypeEvaluator

        init {
            require(preUpdateCount >= 0) {
                "DynamicTypeValueReceiver invoked onData() more times than onPreUpdate()."
            }
        }

        fun withPendingReceiver(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            copy(pendingReceivers = pendingReceivers + receiver)

        fun withPreUpdate() = copy(preUpdateCount = preUpdateCount + 1)

        fun withInvalidReceiver(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            copy(
                pendingReceivers = pendingReceivers - receiver,
                invalidReceivers = invalidReceivers + receiver,
                completeReceivers = completeReceivers - receiver,
                preUpdateCount = preUpdateCount - 1,
            )

        fun withUpdatedData(
            data: WireComplicationData,
            receiver: ComplicationEvaluationResultReceiver<out Any>,
        ) =
            copy(
                data,
                pendingReceivers = pendingReceivers - receiver,
                invalidReceivers = invalidReceivers - receiver,
                completeReceivers = completeReceivers + receiver,
                preUpdateCount = preUpdateCount - 1,
            )

        /**
         * Initializes the internal [DynamicTypeEvaluator] if there are pending receivers.
         *
         * Should be called after all receivers were added.
         */
        fun initEvaluation() {
            if (pendingReceivers.isEmpty()) return
            require(!this::evaluator.isInitialized) { "initEvaluator must be called exactly once." }
            evaluator =
                DynamicTypeEvaluator(
                    /* platformDataSourcesInitiallyEnabled = */ true,
                    stateStore,
                    sensorGateway,
                )
            for (receiver in pendingReceivers) receiver.bind()
            for (receiver in pendingReceivers) receiver.startEvaluation()
            evaluator.enablePlatformDataSources()
        }

        override fun close() {
            for (receiver in pendingReceivers + invalidReceivers + completeReceivers) {
                receiver.close()
            }
            if (this::evaluator.isInitialized) evaluator.close()
        }

        private fun copy(
            data: WireComplicationData = this.data,
            pendingReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.pendingReceivers,
            invalidReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.invalidReceivers,
            completeReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.completeReceivers,
            preUpdateCount: Int = this.preUpdateCount,
        ) =
            State(
                data = data,
                pendingReceivers = pendingReceivers,
                invalidReceivers = invalidReceivers,
                completeReceivers = completeReceivers,
                preUpdateCount = preUpdateCount,
            )
    }

    private inner class ComplicationEvaluationResultReceiver<T : Any>(
        private val state: MutableStateFlow<State>,
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

        override fun onPreUpdate() {
            state.update { it.withPreUpdate() }
        }

        override fun onData(newData: T) {
            state.update {
                it.withUpdatedData(
                    setter(WireComplicationData.Builder(it.data), newData).build(),
                    this
                )
            }
        }

        override fun onInvalidated() {
            state.update { it.withInvalidReceiver(this) }
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

/**
 * Replacement for CoroutineDispatcher.asExecutor extension due to
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
