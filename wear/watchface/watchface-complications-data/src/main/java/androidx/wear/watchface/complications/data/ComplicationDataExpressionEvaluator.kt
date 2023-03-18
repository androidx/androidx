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
    val unevaluatedData: WireComplicationData,
    private val stateStore: ObservableStateStore = ObservableStateStore(emptyMap()),
    private val sensorGateway: SensorGateway? = null,
    private val keepExpression: Boolean = false,
) : AutoCloseable {
    @Volatile // In case init() and close() are called on different threads.
    private lateinit var evaluator: DynamicTypeEvaluator
    private val state = MutableStateFlow(State(unevaluatedData))

    /**
     * Returns a [Flow] that provides the evaluated [WireComplicationData].
     *
     * Must be called and collected exactly once.
     */
    fun evaluate() =
        flow<WireComplicationData> {
            // Add all the receivers before we start binding them because binding can synchronously
            // trigger the receiver, which would update the data before all the fields are
            // evaluated.
            initStateReceivers()
            initEvaluator()
            val evaluatedData: Flow<WireComplicationData> =
                state
                    .mapNotNull {
                        when {
                            // Emitting INVALID_DATA if there's an invalid receiver.
                            it.invalid.isNotEmpty() -> INVALID_DATA
                            // Emitting the data if all pending receivers are done and all
                            // pre-updates are satisfied.
                            it.pending.isEmpty() && it.preUpdateCount == 0 -> it.data
                            // Skipping states that are not ready for be emitted.
                            else -> null
                        }
                    }
                    .distinctUntilChanged()
            emitAll(evaluatedData)
        }

    /**
     * Stops evaluation.
     *
     * The [Flow] returned by [evaluate] will not emit after this is called.
     */
    override fun close() {
        for (receiver in state.value.all) receiver.close()
        if (this::evaluator.isInitialized) evaluator.close()
    }

    /** Adds [ComplicationEvaluationResultReceiver]s to [state]. */
    private suspend fun initStateReceivers() {
        val receivers = mutableSetOf<ComplicationEvaluationResultReceiver<out Any>>()

        if (unevaluatedData.hasRangedValueExpression()) {
            unevaluatedData.rangedValueExpression
                ?.buildReceiver(
                    expressionTrimmer = { setRangedValueExpression(null) },
                    setter = { setRangedValue(it) },
                )
                ?.let { receivers += it }
        }
        if (unevaluatedData.hasLongText()) {
            unevaluatedData.longText?.buildReceiver { setLongText(it) }?.let { receivers += it }
        }
        if (unevaluatedData.hasLongTitle()) {
            unevaluatedData.longTitle?.buildReceiver { setLongTitle(it) }?.let { receivers += it }
        }
        if (unevaluatedData.hasShortText()) {
            unevaluatedData.shortText?.buildReceiver { setShortText(it) }?.let { receivers += it }
        }
        if (unevaluatedData.hasShortTitle()) {
            unevaluatedData.shortTitle?.buildReceiver { setShortTitle(it) }?.let { receivers += it }
        }
        if (unevaluatedData.hasContentDescription()) {
            unevaluatedData.contentDescription
                ?.buildReceiver { setContentDescription(it) }
                ?.let { receivers += it }
        }

        state.value = State(unevaluatedData, receivers)
    }

    private suspend fun DynamicFloat.buildReceiver(
        expressionTrimmer: WireComplicationData.Builder.() -> WireComplicationData.Builder,
        setter: WireComplicationData.Builder.(Float) -> WireComplicationData.Builder,
    ): ComplicationEvaluationResultReceiver<Float> {
        val executor = currentCoroutineContext().asExecutor()
        return ComplicationEvaluationResultReceiver(
            setter = {
                if (!keepExpression) expressionTrimmer(this)
                setter(this, it)
            },
            binder = { evaluator.bind(this@buildReceiver, executor, it) },
        )
    }

    private suspend fun WireComplicationText.buildReceiver(
        setter: WireComplicationData.Builder.(WireComplicationText) -> WireComplicationData.Builder,
    ): ComplicationEvaluationResultReceiver<String>? {
        val executor = currentCoroutineContext().asExecutor()
        return expression?.let { expression ->
            ComplicationEvaluationResultReceiver(
                setter = {
                    setter(
                        if (keepExpression) {
                            WireComplicationText(it, expression)
                        } else {
                            WireComplicationText(it)
                        }
                    )
                },
                binder = { evaluator.bind(expression, ULocale.getDefault(), executor, it) },
            )
        }
    }

    /** Initializes the internal [DynamicTypeEvaluator] if there are pending receivers. */
    private fun initEvaluator() {
        if (state.value.pending.isEmpty()) return
        evaluator =
            DynamicTypeEvaluator(
                /* platformDataSourcesInitiallyEnabled = */ true,
                stateStore,
                sensorGateway,
            )
        for (receiver in state.value.pending) receiver.bind()
        for (receiver in state.value.pending) receiver.startEvaluation()
        evaluator.enablePlatformDataSources()
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
        val preUpdateCount: Int = 0,
    ) {
        val all = pending + invalid + complete

        init {
            require(preUpdateCount >= 0) {
                "DynamicTypeValueReceiver invoked onData() more times than onPreUpdate()."
            }
        }

        fun withPreUpdate() =
            State(
                data,
                pending = pending,
                invalid = invalid,
                complete = complete,
                preUpdateCount + 1,
            )

        fun withInvalid(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            State(
                data,
                pending = pending - receiver,
                invalid = invalid + receiver,
                complete = complete - receiver,
                preUpdateCount - 1,
            )

        fun withUpdated(
            data: WireComplicationData,
            receiver: ComplicationEvaluationResultReceiver<out Any>,
        ) =
            State(
                data,
                pending = pending - receiver,
                invalid = invalid - receiver,
                complete = complete + receiver,
                preUpdateCount - 1,
            )
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

        override fun onPreUpdate() {
            state.update { it.withPreUpdate() }
        }

        override fun onData(newData: T) {
            state.update {
                it.withUpdated(setter(WireComplicationData.Builder(it.data), newData).build(), this)
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

/**
 * Replacement for CoroutineDispatcher.asExecutor extension due to
 * https://github.com/Kotlin/kotlinx.coroutines/pull/3683.
 */
internal fun CoroutineContext.asExecutor() = Executor { runnable ->
    val dispatcher = get(ContinuationInterceptor) as CoroutineDispatcher
    if (dispatcher.isDispatchNeeded(this)) {
        dispatcher.dispatch(this, runnable)
    } else {
        runnable.run()
    }
}
