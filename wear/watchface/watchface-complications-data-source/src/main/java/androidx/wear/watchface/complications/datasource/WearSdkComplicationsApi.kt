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

package androidx.wear.watchface.complications.datasource

import android.content.ComponentName
import android.content.Context
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.OpenForTesting
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ComplicationData as AndroidXComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.ComplicationDataRequester
import com.google.wear.Sdk
import com.google.wear.services.complications.ActiveComplicationConfig
import com.google.wear.services.complications.ComplicationData
import com.google.wear.services.complications.ComplicationsManager
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wrapper class for the Wear SDK Complications API.
 *
 * This class facilitates testing via mocks which is otherwise not possible due to only stubs being
 * accessible in unit tests.
 */
@RequiresApi(36)
internal interface WearSdkComplicationsApi {
    /**
     * Calls [ComplicationsManager.getActiveComplicationConfigsAsync] and returns a set of
     * [WearSdkActiveComplicationConfig]s.
     */
    suspend fun getActiveConfigs(executor: Executor): Set<WearSdkActiveComplicationConfig>

    /** Calls [ComplicationsManager.updateComplication]. */
    suspend fun updateComplication(id: Int, data: WearSdkComplicationData, executor: Executor)

    fun create(context: Context) = Impl(context)

    @RequiresApi(36)
    class Impl(private val manager: ComplicationsManager) : WearSdkComplicationsApi {
        internal constructor(
            context: Context
        ) : this(Sdk.getWearManager(context, ComplicationsManager::class.java))

        override suspend fun getActiveConfigs(
            executor: Executor
        ): Set<WearSdkActiveComplicationConfig> =
            suspendForOutcomeReceiver { outcomeReceiver ->
                    manager.getActiveComplicationConfigsAsync(executor, outcomeReceiver)
                }
                .map { WearSdkActiveComplicationConfig(it) }
                .toSet()

        override suspend fun updateComplication(
            id: Int,
            data: WearSdkComplicationData,
            executor: Executor,
        ) {
            try {
                suspendForOutcomeReceiver<Void> { outcomeReceiver ->
                    manager.updateComplication(id, data.data, executor, outcomeReceiver)
                }
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to update complication= $error")
            }
        }

        suspend fun <T> suspendForOutcomeReceiver(block: (OutcomeReceiver<T, Throwable>) -> Unit) =
            suspendCancellableCoroutine<T> { continuation ->
                block(suspendOutcomeReceiver(continuation))
            }

        fun <T> suspendOutcomeReceiver(
            continuation: Continuation<T>
        ): OutcomeReceiver<T, Throwable> =
            object : OutcomeReceiver<T, Throwable> {
                override fun onResult(result: T?) {
                    result?.let { continuation.resume(it) }
                }

                override fun onError(error: Throwable) {
                    continuation.resumeWithException(error)
                }
            }

        internal companion object {
            internal const val TAG = "WearSdkComplicationsApi"
        }
    }
}

/**
 * Wraps an [ActiveComplicationConfig] object and exposes fields that are required by for a
 * complication update.
 */
@RequiresApi(36)
internal class WearSdkActiveComplicationConfig(
    val providerComponent: ComponentName,
    val id: Int,
    val dataType: ComplicationType,
    val targetWatchFace: Int,
) {
    internal constructor(
        config: ActiveComplicationConfig
    ) : this(
        config.config.providerComponent,
        config.config.id,
        ComplicationType.fromWireType(config.config.dataType),
        config.targetWatchFaceSafety,
    )

    /**
     * Converts the config into a pair of a ComplicationRequest and the component name of the data
     * source to which the request will be made.
     */
    fun toComplicationRequestPair(
        immediateResponseRequired: Boolean
    ): Pair<ComponentName, ComplicationRequest> =
        Pair(
            providerComponent,
            ComplicationRequest(id, dataType, immediateResponseRequired, targetWatchFace),
        )
}

/** A wrapper for [ComplicationData] to allow mocking in tests. */
@OpenForTesting
@RequiresApi(36)
internal open class WearSdkComplicationData(val data: ComplicationData)

/**
 * An implementation of [ComplicationDataSourceService.ComplicationRequestListener] which validates
 * incoming [ComplicationData] and converts it to
 * [com.google.wear.services.complications.ComplicationData] to resume the provided [Continuation].
 */
@RequiresApi(36)
internal open class WearSdkComplicationRequestListener
internal constructor(
    val id: Int,
    val dataType: Int,
    val continuation: Continuation<Pair<Int, WearSdkComplicationData>>,
) : ComplicationDataSourceService.ComplicationRequestListener {
    internal constructor(
        request: ComplicationRequest,
        continuation: Continuation<Pair<Int, WearSdkComplicationData>>,
    ) : this(
        request.complicationInstanceId,
        request.complicationType.toWireComplicationType(),
        continuation,
    )

    override fun onComplicationData(complicationData: AndroidXComplicationData?) {
        // The result will be null when there is no update required.
        complicationData?.let {
            try {
                validateReceivedData(it)
                continuation.resume(
                    Pair(id, WearSdkComplicationData(it.asWearSdkComplicationData()))
                )
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    override fun onComplicationDataTimeline(complicationDataTimeline: ComplicationDataTimeline?) {
        // The result will be null when there is no update required.
        complicationDataTimeline?.let {
            try {
                it.validate()
                // This can be run on an arbitrary thread, but that's OK.
                validateReceivedData(it.defaultComplicationData)
                it.timelineEntries.forEach { entry -> validateReceivedData(entry.complicationData) }
                continuation.resume(
                    Pair(id, WearSdkComplicationData(it.asWearSdkComplicationData()))
                )
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    private fun validateReceivedData(complicationData: AndroidXComplicationData?) {
        complicationData?.validate()
        // This can be run on an arbitrary thread, but that's OK.
        val receivedDataType = complicationData?.type ?: ComplicationType.NO_DATA
        val expectedDataType = ComplicationType.fromWireType(dataType)
        require(
            receivedDataType != ComplicationType.NOT_CONFIGURED &&
                receivedDataType != ComplicationType.EMPTY
        ) {
            "Cannot send data of TYPE_NOT_CONFIGURED or TYPE_EMPTY. Use TYPE_NO_DATA" + " instead."
        }
        require(
            receivedDataType == ComplicationType.NO_DATA || receivedDataType == expectedDataType
        ) {
            "Complication data should match the requested type. Expected " +
                "$expectedDataType got $receivedDataType."
        }
        if (complicationData is NoDataComplicationData) {
            complicationData.placeholder?.let {
                require(it.type == expectedDataType) {
                    "Placeholder type must match the requested type. Expected " +
                        "$expectedDataType got ${it.type}."
                }
            }
        }
    }
}

@RequiresApi(36)
/** Interface to create a new [ComplicationDataRequester]. */
internal interface WearSdkComplicationDataRequester {
    /**
     * A suspending function which calls the [ComplicationDataRequester]'s `onComplicationRequest`
     * method using a CancellableCoroutine to unwrap the `ComplicationRequestListener`.
     */
    suspend fun requestData(request: ComplicationRequest): Pair<Int, WearSdkComplicationData>

    /**
     * Implementation of [WearSdkComplicationDataRequester] which creates a
     * [WearSdkComplicationRequestListener] to receive the result of the request.
     */
    class Impl(val requester: ComplicationDataRequester) : WearSdkComplicationDataRequester {
        override suspend fun requestData(
            request: ComplicationRequest
        ): Pair<Int, WearSdkComplicationData> = suspendCancellableCoroutine { continuation ->
            requester.onComplicationRequest(
                request,
                WearSdkComplicationRequestListener(request, continuation),
            )
        }
    }
}
