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
import androidx.glance.appwidget.action.RunCallbackAction
import androidx.glance.appwidget.proto.LayoutProto.AsyncRequest
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
        when {
            request.hasUpdate() -> {
                val update = request.update
                createReceiver(update.receiver)?.run {
                    doUpdate(applicationContext, update.appWidgetIdsList.toIntArray())
                }
            }
            request.hasDelete() -> {
                val delete = request.delete
                createReceiver(delete.receiver)?.run {
                    doDelete(applicationContext, delete.appWidgetIdsList.toIntArray())
                }
            }
            request.hasLambda() -> {
                val lambda = request.lambda
                createReceiver(lambda.receiver)?.run {
                    doLambda(applicationContext, lambda.appWidgetId, lambda.actionKey)
                }
            }
            request.hasRunCallback() -> {
                val runCallback = request.runCallback
                RunCallbackAction.run(
                    applicationContext,
                    runCallback.className,
                    AppWidgetId(runCallback.appWidgetId),
                    actionParametersFromBytes(runCallback.actionParameters.toByteArray()),
                )
            }
            request.hasMyPackageReplaced() -> {
                val manager = GlanceAppWidgetManager(applicationContext)
                manager.cleanReceivers()
            }
            request.hasOptionsChanged() -> {
                val optionsChanged = request.optionsChanged
                createReceiver(optionsChanged.receiver)?.run {
                    doOptionsChanged(
                        applicationContext,
                        optionsChanged.appWidgetId,
                        bundleFromBytes(optionsChanged.bundle.toByteArray()),
                    )
                }
            }
        }
        Result.success()
    }

    internal companion object {
        private fun createReceiver(className: String): GlanceAppWidgetReceiver? {
            return className.let { Class.forName(it) }.getDeclaredConstructor().newInstance()
                as? GlanceAppWidgetReceiver
        }

        fun launchAsyncRequestWorker(context: Context, request: AsyncRequest) {
            WorkManager.getInstance(context).run {
                enqueue(
                    OneTimeWorkRequestBuilder<AsyncRequestWorker>()
                        .setInputData(
                            Data.Builder().putByteArray("request", request.toByteArray()).build()
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

        fun ActionParameters.toBytes(): ByteArray =
            asMap()
                .map { (key, value) -> key.name to value }
                .toTypedArray()
                .let { bundleOf(ExtraParameters to bundleOf(*it)) }
                .toBytes()

        fun actionParametersFromBytes(bytes: ByteArray): ActionParameters =
            getParameterExtras(bundleFromBytes(bytes))

        fun Bundle.toBytes(): ByteArray =
            Parcel.obtain().let { parcel ->
                writeToParcel(parcel, 0)
                parcel.marshall().also { parcel.recycle() }
            }

        fun bundleFromBytes(bytes: ByteArray): Bundle =
            Parcel.obtain().let { parcel ->
                parcel.unmarshall(bytes, 0, bytes.size)
                parcel.setDataPosition(0)
                Bundle.CREATOR.createFromParcel(parcel).also { parcel.recycle() }
            }
    }
}
