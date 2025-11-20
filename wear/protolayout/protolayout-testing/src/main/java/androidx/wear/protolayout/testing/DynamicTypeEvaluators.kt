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

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.util.ULocale
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataMap
import androidx.wear.protolayout.expression.PlatformDataValues
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver
import androidx.wear.protolayout.expression.pipeline.StateStore
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executor

/**
 * Evaluates the dynamic string with the injected app state and/or platform data values.
 *
 * @return the last evaluated string value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
internal fun DynamicString.evaluate(injectedData: DynamicDataMap? = null): String? {
    val result = mutableListOf<String>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicString(
            this,
            ULocale.getDefault(),
            { it.run() },
            AddToListCallback(result),
        ),
        injectedData,
    )
    return result.lastOrNull()
}

/**
 * Evaluate the dynamic Float data with the injected app state and/or platform data values.
 *
 * @return the last evaluated float value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
@Suppress("AutoBoxing")
internal fun DynamicFloat.evaluate(injectedData: DynamicDataMap? = null): Float? {
    val result = mutableListOf<Float>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicFloat(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()
}

/**
 * Evaluate the dynamic integer data with the injected app state and/or platform data values.
 *
 * @return the last evaluated integer value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
@Suppress("AutoBoxing")
internal fun DynamicInt32.evaluate(injectedData: DynamicDataMap? = null): Int? {
    val result = mutableListOf<Int>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicInt32(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()
}

/**
 * Evaluates the dynamic boolean with the injected app state and/or platform data values.
 *
 * @return the last evaluated boolean value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
internal fun DynamicBool.evaluate(injectedData: DynamicDataMap? = null): Boolean? {
    val result = mutableListOf<Boolean>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicBool(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()
}

/**
 * Evaluates the dynamic color with the injected app state and/or platform data values.
 *
 * @return the last evaluated boolean value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
internal fun DynamicColor.evaluate(injectedData: DynamicDataMap? = null): Color? {
    val result = mutableListOf<Int>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicColor(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()?.let { Color.valueOf(it) }
}

/**
 * Evaluates the dynamic duration with the injected app state and/or platform data values.
 *
 * @return the last evaluated boolean value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
internal fun DynamicDuration.evaluate(injectedData: DynamicDataMap? = null): Duration? {
    val result = mutableListOf<Duration>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicDuration(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()
}

/**
 * Evaluates the dynamic instant with the injected app state and/or platform data values.
 *
 * @return the last evaluated boolean value with the data injected to the pipeline into. Null return
 *   indicates that there is no data received in the pipeline.
 */
internal fun DynamicInstant.evaluate(injectedData: DynamicDataMap? = null): Instant? {
    val result = mutableListOf<Instant>()
    evaluate(
        DynamicTypeBindingRequest.forDynamicInstant(this, { it.run() }, AddToListCallback(result)),
        injectedData,
    )
    return result.lastOrNull()
}

private fun evaluate(bindingRequest: DynamicTypeBindingRequest, injectedData: DynamicDataMap?) {
    if (injectedData == null || injectedData.entries.isEmpty()) {
        DynamicTypeEvaluator(DynamicTypeEvaluator.Config.Builder().build())
            .bind(bindingRequest)
            .startEvaluation()
        return
    }

    val appStateData =
        injectedData.entries
            .filter { entry -> entry.key is AppDataKey }
            .associate { entry -> entry.key as AppDataKey to entry.value }

    val stateStore = StateStore(emptyMap())
    val configBuilder = DynamicTypeEvaluator.Config.Builder().setStateStore(stateStore)

    var platformDataReceiver: PlatformDataReceiver? = null
    val platformData = PlatformDataValues.Builder().putAll(injectedData).build()
    if (platformData.all.isNotEmpty()) {
        configBuilder.addPlatformDataProvider(
            object : PlatformDataProvider {
                @SuppressLint("ProtoLayoutMinSchema")
                override fun setReceiver(executor: Executor, receiver: PlatformDataReceiver) {
                    platformDataReceiver = receiver
                }

                override fun clearReceiver() {}
            },
            platformData.all.keys,
        )
    }

    DynamicTypeEvaluator(configBuilder.build()).bind(bindingRequest).startEvaluation()

    if (appStateData.isNotEmpty()) {
        stateStore.setAppStateEntryValues(appStateData)
    }

    platformDataReceiver?.onData(platformData)
}

private class AddToListCallback<T : Any>(private val listToUpdate: MutableList<T>) :
    DynamicTypeValueReceiver<T> {
    override fun onData(newData: T) {
        listToUpdate.add(newData)
    }

    override fun onInvalidated() {}
}
