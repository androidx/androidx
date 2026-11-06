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

package androidx.glance.appwidget

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.core.os.bundleOf
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver.Companion.ExtraParameters
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver.Companion.getParameterExtras
import androidx.glance.appwidget.proto.LayoutProto.AsyncRequest
import androidx.glance.appwidget.protobuf.ByteString
import androidx.glance.appwidget.updatestrategy.BroadcastRequest
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

/**
 * This class handles requests from a GlanceAppWidgetReceiver to do update, resize, delete, and
 * lambda actions. This is to avoid calling goAsync on devices where it may cause a crash.
 *
 * This is a workaround for issues with certain OEMs, but goAsync is the preferred method of
 * launching asynchronous tasks from a BroadcastReceiver. See
 * https://issuetracker.google.com/257513022 for more info.
 */
@RestrictTo(LIBRARY_GROUP)
public class AsyncRequestWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    @Deprecated("Deprecated by super class, replacement in progress, see b/245353737")
    override val coroutineContext: CoroutineDispatcher = Dispatchers.Main
    private val request = AsyncRequest.parseFrom(params.inputData.getByteArray("request"))

    override suspend fun doWork(): Result = coroutineScope {
        request.toBroadcastRequest()?.execute(applicationContext)
        Result.success()
    }

    internal companion object {
        /** Enqueues a one-time [AsyncRequestWorker] in [WorkManager] to process [request]. */
        fun launch(context: Context, request: BroadcastRequest) {
            val proto = request.toProto()
            WorkManager.getInstance(context).run {
                enqueue(
                    OneTimeWorkRequestBuilder<AsyncRequestWorker>()
                        .setInputData(
                            Data.Builder().putByteArray("request", proto.toByteArray()).build()
                        )
                        .build()
                )
                enqueueDelayedWorker()
            }
        }

        /** Workaround worker to fix b/119920965 */
        private fun WorkManager.enqueueDelayedWorker() {
            enqueueUniqueWork(
                "updateRequestWorkerKeepEnabled",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<AsyncRequestWorker>()
                    .setInitialDelay(10 * 365, TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .build(),
            )
        }

        private fun BroadcastRequest.toProto(): AsyncRequest =
            AsyncRequest.newBuilder()
                .apply {
                    when (val req = this@toProto) {
                        is BroadcastRequest.Update ->
                            update =
                                AsyncRequest.Update.newBuilder()
                                    .apply {
                                        receiver = req.widgetReceiver::class.java.canonicalName
                                        addAllAppWidgetIds(req.appWidgetIds.toList())
                                    }
                                    .build()
                        is BroadcastRequest.OptionsChanged ->
                            optionsChanged =
                                AsyncRequest.OptionsChanged.newBuilder()
                                    .apply {
                                        receiver = req.widgetReceiver::class.java.canonicalName
                                        appWidgetId = req.appWidgetId
                                        bundle = ByteString.copyFrom(req.newOptions.toBytes())
                                    }
                                    .build()
                        is BroadcastRequest.Delete ->
                            delete =
                                AsyncRequest.Delete.newBuilder()
                                    .apply {
                                        receiver = req.widgetReceiver::class.java.canonicalName
                                        addAllAppWidgetIds(req.appWidgetIds.toList())
                                    }
                                    .build()
                        is BroadcastRequest.Lambda ->
                            lambda =
                                AsyncRequest.Lambda.newBuilder()
                                    .apply {
                                        receiver = req.widgetReceiver::class.java.canonicalName
                                        appWidgetId = req.appWidgetId
                                        actionKey = req.actionKey
                                    }
                                    .build()
                        is BroadcastRequest.RunCallback ->
                            runCallback =
                                AsyncRequest.RunCallback.newBuilder()
                                    .apply {
                                        className = req.className
                                        appWidgetId = req.glanceId.appWidgetId
                                        actionParameters =
                                            ByteString.copyFrom(req.parameters.toBytes())
                                    }
                                    .build()
                    }
                }
                .build()

        private fun AsyncRequest.toBroadcastRequest(): BroadcastRequest? =
            when {
                hasUpdate() ->
                    createReceiver(update.receiver)?.let {
                        BroadcastRequest.Update(
                            widgetReceiver = it,
                            appWidgetIds = update.appWidgetIdsList.toIntArray(),
                        )
                    }
                hasDelete() ->
                    createReceiver(delete.receiver)?.let {
                        BroadcastRequest.Delete(
                            widgetReceiver = it,
                            appWidgetIds = delete.appWidgetIdsList.toIntArray(),
                        )
                    }
                hasLambda() ->
                    createReceiver(lambda.receiver)?.let {
                        BroadcastRequest.Lambda(
                            widgetReceiver = it,
                            appWidgetId = lambda.appWidgetId,
                            actionKey = lambda.actionKey,
                        )
                    }
                hasRunCallback() ->
                    BroadcastRequest.RunCallback(
                        receiver = null,
                        widgetOwner = null,
                        className = runCallback.className,
                        glanceId = AppWidgetId(runCallback.appWidgetId),
                        parameters =
                            actionParametersFromBytes(runCallback.actionParameters.toByteArray()),
                    )
                hasOptionsChanged() ->
                    createReceiver(optionsChanged.receiver)?.let {
                        BroadcastRequest.OptionsChanged(
                            widgetReceiver = it,
                            appWidgetId = optionsChanged.appWidgetId,
                            newOptions = bundleFromBytes(optionsChanged.bundle.toByteArray()),
                        )
                    }
                else -> null
            }

        private fun createReceiver(className: String): GlanceAppWidgetReceiver? {
            return className.let { Class.forName(it) }.getDeclaredConstructor().newInstance()
                as? GlanceAppWidgetReceiver
        }

        private fun ActionParameters.toBytes(): ByteArray {
            val typedArray = asMap().map { (key, value) -> key.name to value }.toTypedArray()

            return Bundle()
                .apply {
                    @Suppress("DEPRECATION") // bundleOf is deprecated
                    putBundle(ExtraParameters, bundleOf(*typedArray))
                }
                .toBytes()
        }

        private fun actionParametersFromBytes(bytes: ByteArray): ActionParameters =
            getParameterExtras(bundleFromBytes(bytes))

        private fun Bundle.toBytes(): ByteArray =
            Parcel.obtain().let { parcel ->
                writeToParcel(parcel, 0)
                parcel.marshall().also { parcel.recycle() }
            }

        private fun bundleFromBytes(bytes: ByteArray): Bundle =
            Parcel.obtain().let { parcel ->
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                Bundle.CREATOR.createFromParcel(parcel).also { parcel.recycle() }
            }
    }
}
