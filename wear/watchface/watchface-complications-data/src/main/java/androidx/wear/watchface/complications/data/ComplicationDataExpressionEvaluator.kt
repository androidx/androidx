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
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Evaluates a [WireComplicationData] with [androidx.wear.remote.expr.DynamicBuilders.DynamicType]
 * within its fields.
 *
 * Due to [WireComplicationData]'s shallow copy strategy the input is modified in-place.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ComplicationDataExpressionEvaluator(
    val unevaluatedData: WireComplicationData,
) : AutoCloseable {
    /**
     * Java compatibility class for [ComplicationDataExpressionEvaluator].
     *
     * Unlike [data], [listener] is not invoked until there is a value (until [data] is non-null).
     */
    class Compat(
        val unevaluatedData: WireComplicationData,
        private val executor: Executor,
        private val listener: Consumer<WireComplicationData>,
    ) : AutoCloseable {
        private val evaluator = ComplicationDataExpressionEvaluator(unevaluatedData)

        /** @see ComplicationDataExpressionEvaluator.init */
        fun init() {
            evaluator.init()
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

    /** Parses the expression and starts async evaluation. */
    fun init() {
        // TODO(b/260065006): Use real implementation.
        _data.value = unevaluatedData
    }

    /**
     * Stops evaluation.
     *
     * [data] will not change after this is called.
     */
    override fun close() {
        // TODO(b/260065006): Use real implementation.
    }
}
