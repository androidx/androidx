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

@file:JvmName("WorkerParametersExtensions")

package androidx.work

import android.content.ComponentName
import androidx.annotation.RestrictTo
import androidx.work.impl.utils.ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME
import androidx.work.impl.utils.ARGUMENT_SERVICE_CLASS_NAME
import androidx.work.impl.utils.ARGUMENT_SERVICE_PACKAGE_NAME

/**
 * @return `true` if and only if the instance of [WorkerParameters] corresponds to a [WorkRequest]
 *   that runs in a remote process.
 */
fun WorkerParameters.isRemoteWorkRequest(): Boolean {
    return inputData.isRemoteWorkRequest()
}

/**
 * Returns a new instance of [WorkerParameters] representing a [WorkRequest] that can run in a
 * process corresponding to the provided [ComponentName].
 *
 * @param T The [ListenableWorker] to delegate to.
 * @param componentName The [ComponentName] that identifies the `RemoteService` that hosts the
 *   [WorkRequest].
 * @return A new instance of [WorkerParameters]
 */
inline fun <reified T : ListenableWorker> WorkerParameters.usingRemoteService(
    componentName: ComponentName
): WorkerParameters {
    return usingRemoteService(T::class.java.name, componentName)
}

/**
 * Returns a new instance of [WorkerParameters] representing a [WorkRequest] that can run in a
 * process corresponding to the provided [ComponentName].
 *
 * @param workerClassName The fully qualified class name of the [ListenableWorker] to delegate to
 * @param componentName The [ComponentName] that identifies the `RemoteService` that hosts the
 *   [WorkRequest].
 * @return A new instance of [WorkerParameters]
 */
fun WorkerParameters.usingRemoteService(
    workerClassName: String,
    componentName: ComponentName
): WorkerParameters {
    return WorkerParameters(
        id,
        buildDelegatedRemoteRequestData(workerClassName, componentName, inputData),
        tags,
        runtimeExtras,
        runAttemptCount,
        generation,
        backgroundExecutor,
        workerContext,
        taskExecutor,
        workerFactory,
        progressUpdater,
        foregroundUpdater
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun buildDelegatedRemoteRequestData(
    delegatedWorkerName: String,
    componentName: ComponentName,
    inputData: Data
): Data {
    val builder = Data.Builder()
    builder
        .putAll(data = inputData)
        .putString(ARGUMENT_SERVICE_PACKAGE_NAME, componentName.packageName)
        .putString(ARGUMENT_SERVICE_CLASS_NAME, componentName.className)
        .putString(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME, delegatedWorkerName)
    return builder.build()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Data.isRemoteWorkRequest(): Boolean {
    return hasKey<String>(ARGUMENT_SERVICE_PACKAGE_NAME) &&
        hasKey<String>(ARGUMENT_SERVICE_CLASS_NAME) &&
        hasKey<String>(ARGUMENT_REMOTE_LISTENABLE_WORKER_NAME)
}
