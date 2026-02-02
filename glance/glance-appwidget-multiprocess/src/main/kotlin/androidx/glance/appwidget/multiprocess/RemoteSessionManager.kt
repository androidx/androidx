/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.multiprocess

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.await
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.AppWidgetSession
import androidx.glance.appwidget.SizeMode
import androidx.glance.session.SessionManager
import androidx.glance.session.SessionManagerImpl
import androidx.glance.session.SessionWorker
import androidx.glance.session.WorkManagerProxy
import androidx.glance.state.ConfigManager
import androidx.glance.state.GlanceState
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.multiprocess.RemoteListenableWorker
import androidx.work.multiprocess.RemoteWorkManager
import androidx.work.workDataOf

///
/// Private APIs: these are just versions of the normal session classes (Session, SessionManager,
/// SessionWorker) that use RemoteWorkManager instead of the normal WorkManager.
///

private interface RemoteSession {
    val remoteWorkService: ComponentName
}

internal class RemoteAppWidgetSession(
    private val widget: MultiProcessGlanceAppWidget,
    override val remoteWorkService: ComponentName,
    id: AppWidgetId,
    initialOptions: Bundle? = null,
    configManager: ConfigManager = GlanceState,
    lambdaReceiver: ComponentName? = null,
    sizeMode: SizeMode = widget.sizeMode,
    shouldPublish: Boolean = true,
    initialGlanceState: Any? = null,
) :
    AppWidgetSession(
        widget,
        id,
        initialOptions,
        configManager,
        lambdaReceiver,
        sizeMode,
        shouldPublish,
        initialGlanceState
    ),
    RemoteSession

internal object RemoteSessionManager :
    SessionManager by SessionManagerImpl(
        workerClass = RemoteSessionWorker::class.java,
        workManagerProxy = RemoteWorkManagerProxy,
        inputDataFactory = { session ->
            val className = (session as? RemoteSession)?.remoteWorkService?.className
            val packageName = (session as? RemoteSession)?.remoteWorkService?.packageName
            workDataOf(
                keyParam to session.key,
                RemoteListenableWorker.ARGUMENT_CLASS_NAME to className,
                RemoteListenableWorker.ARGUMENT_PACKAGE_NAME to packageName,
            )
        }
    )

private object RemoteWorkManagerProxy : WorkManagerProxy {
    override suspend fun enqueueUniqueWork(
        context: Context,
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        workRequest: OneTimeWorkRequest,
    ) {
        RemoteWorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, workRequest)
            .await()
    }

    override suspend fun workerIsRunningOrEnqueued(
        context: Context,
        uniqueWorkName: String
    ): Boolean =
        RemoteWorkManager.getInstance(context)
            .getWorkInfos(WorkQuery.Builder.fromUniqueWorkNames(listOf(uniqueWorkName)).build())
            .await()
            .any { it.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED) }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RemoteSessionWorker(
    private val context: Context,
    private val parameters: WorkerParameters
) : RemoteCoroutineWorker(context, parameters) {
    override suspend fun doRemoteWork(): Result =
        SessionWorker(context, parameters, RemoteSessionManager).doWork()
}
