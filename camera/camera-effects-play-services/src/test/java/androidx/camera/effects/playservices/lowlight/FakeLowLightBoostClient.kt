/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import com.google.android.gms.cameralowlight.LowLightBoostCallback
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.cameralowlight.LowLightBoostOptions
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

/** A fake [LowLightBoostClient] whose results are controlled by tests. */
internal class FakeLowLightBoostClient : LowLightBoostClient {
    /** Starts the task returned by [isDeviceSupported]. */
    var deviceSupportedTask: () -> Task<Boolean> = { Tasks.forResult(true) }

    /** Starts the task returned by [isCameraSupported]. */
    var cameraSupportedTask: (cameraId: String) -> Task<Boolean> = { Tasks.forResult(true) }

    /** Starts the task returned by [isModuleInstalled]. */
    var moduleInstalledTask: () -> Task<Boolean> = { Tasks.forResult(true) }

    /** Starts the task returned by [installModule]. */
    var installModuleTask: () -> Task<Boolean> = { Tasks.forResult(true) }

    /** If set, [createSession] fails with this error. */
    var createSessionError: Exception? = null

    /** If set, [createSession] throws this error synchronously instead of returning a task. */
    var createSessionThrowable: RuntimeException? = null

    /** Called by each [createSession] call after it is recorded, e.g. to make it take time. */
    var onCreateSession: () -> Unit = {}

    var deviceSupportedCount = 0
        private set

    var cameraSupportedCount = 0
        private set

    /** The callback of each [createSession] call, in order. */
    val callbacks = mutableListOf<LowLightBoostCallback>()

    /** The options of each [createSession] call, in order. */
    val createSessionOptions = mutableListOf<LowLightBoostOptions>()

    /** Completes the task returned by each [createSession] call, in order. */
    val createSessionResults = mutableListOf<TaskCompletionSource<LowLightBoostSession>>()

    /** The callback of each [installModule] call, in order. */
    val installCallbacks = mutableListOf<LowLightBoostClient.InstallStatusCallback?>()

    val createSessionCount: Int
        get() = callbacks.size

    override fun isDeviceSupported(): Task<Boolean> {
        deviceSupportedCount++
        return deviceSupportedTask()
    }

    override fun isCameraSupported(cameraId: String): Task<Boolean> {
        cameraSupportedCount++
        return cameraSupportedTask(cameraId)
    }

    override fun isModuleInstalled(): Task<Boolean> = moduleInstalledTask()

    override fun installModule(
        callback: LowLightBoostClient.InstallStatusCallback?
    ): Task<Boolean> {
        installCallbacks.add(callback)
        return installModuleTask()
    }

    override fun releaseModule(): Task<Void> = Tasks.forResult(null)

    override fun createSession(
        options: LowLightBoostOptions,
        callback: LowLightBoostCallback,
    ): Task<LowLightBoostSession> {
        callbacks.add(callback)
        createSessionOptions.add(options)
        onCreateSession()
        createSessionThrowable?.let { throw it }
        val source = TaskCompletionSource<LowLightBoostSession>()
        createSessionResults.add(source)
        createSessionError?.let {
            return Tasks.forException(it)
        }
        return source.task
    }

    override fun getApiKey(): ApiKey<Api.ApiOptions.NoOptions> =
        throw UnsupportedOperationException()

    override fun getOptionalFeatures(): Array<Feature> = emptyArray()
}
