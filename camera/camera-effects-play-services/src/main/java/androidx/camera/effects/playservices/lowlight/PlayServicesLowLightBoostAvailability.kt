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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraInfo
import androidx.camera.core.Logger
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import java.util.Collections
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Queries and caches whether the Google Play Services Low Light Boost effect is supported on the
 * device and on a given camera.
 *
 * Availability checks invoke [LowLightBoostClient.isDeviceSupported] and
 * [LowLightBoostClient.isCameraSupported] asynchronously with a timeout. Results reported by Google
 * Play Services are cached for the lifetime of the process, keyed by the camera ID for camera
 * support, so repeated queries return immediately. A timeout or a failed query resolves to `false`
 * and isn't cached, so a later query tries again.
 */
internal object PlayServicesLowLightBoostAvailability {

    private const val TAG = "PlayServicesLowLightBoostAvailability"

    /** How long to wait for Google Play Services to answer a support query. */
    private const val DEFAULT_TIMEOUT_MS = 5_000L

    @Volatile private var deviceSupported: Boolean? = null
    private val cameraSupported: MutableMap<String, Boolean> =
        Collections.synchronizedMap(HashMap())

    /** Overrides the [LowLightBoostClient] used for queries in tests. */
    @VisibleForTesting
    @Volatile
    internal var clientFactoryForTesting: ((Context) -> LowLightBoostClient)? = null

    /** The timeout of each Google Play Services query, overridable in tests. */
    @VisibleForTesting @Volatile internal var timeoutMs: Long = DEFAULT_TIMEOUT_MS

    /**
     * Enforces query timeouts on a background thread, so a caller that blocks the main thread on
     * the returned future still gets a result. The thread exits when no timeout is pending.
     */
    private val timeoutExecutor by lazy {
        ScheduledThreadPoolExecutor(1) { runnable ->
                Thread(runnable, "CameraX-LLB-Availability").apply { isDaemon = true }
            }
            .apply {
                setKeepAliveTime(1, TimeUnit.SECONDS)
                allowCoreThreadTimeOut(true)
                removeOnCancelPolicy = true
            }
    }

    /** Returns the cached device support result, or `null` if it is not known yet. */
    internal fun getCachedDeviceSupported(): Boolean? = deviceSupported

    /** Caches whether the device supports Low Light Boost, as reported by Google Play Services. */
    internal fun setDeviceSupported(supported: Boolean) {
        deviceSupported = supported
    }

    /** Returns the cached support result for [cameraId], or `null` if it is not known yet. */
    internal fun getCachedCameraSupported(cameraId: String): Boolean? = cameraSupported[cameraId]

    /** Caches whether [cameraId] supports Low Light Boost, as reported by Google Play Services. */
    internal fun setCameraSupported(cameraId: String, supported: Boolean) {
        cameraSupported[cameraId] = supported
    }

    /** Clears all cached results and test overrides. */
    @VisibleForTesting
    internal fun reset() {
        deviceSupported = null
        cameraSupported.clear()
        clientFactoryForTesting = null
        timeoutMs = DEFAULT_TIMEOUT_MS
    }

    /**
     * Asynchronously checks whether the Play Services Low Light Boost effect is supported on the
     * device and the specified [cameraInfo].
     *
     * Checks cached results first. Otherwise, queries [LowLightBoostClient.isDeviceSupported] and
     * then [LowLightBoostClient.isCameraSupported], and caches their results.
     *
     * @param context The context used to query Play Services availability.
     * @param cameraInfo The [CameraInfo] of the camera to check.
     * @return A [ListenableFuture] resolving to `true` if supported, or `false` otherwise. `false`
     *   is also returned if Google Play Services can't be reached within the timeout; such a result
     *   isn't cached.
     */
    fun isSupportedAsync(context: Context, cameraInfo: CameraInfo): ListenableFuture<Boolean> {
        if (cameraInfo !is CameraInfoInternal) {
            return Futures.immediateFuture(false)
        }
        return isSupportedAsync(context, cameraInfo.implementation.cameraId)
    }

    /**
     * Asynchronously checks whether the Play Services Low Light Boost effect is supported on the
     * device and the camera [cameraId]. See [isSupportedAsync].
     */
    internal fun isSupportedAsync(context: Context, cameraId: String): ListenableFuture<Boolean> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Futures.immediateFuture(false)
        }
        val appContext = ContextUtil.getPersistentApplicationContext(context)
        return Futures.transformAsync(
            isDeviceSupportedAsync(appContext),
            { deviceSupported ->
                if (deviceSupported == true) {
                    isCameraSupportedAsync(appContext, cameraId)
                } else {
                    Futures.immediateFuture(false)
                }
            },
            CameraXExecutors.directExecutor(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isDeviceSupportedAsync(appContext: Context): ListenableFuture<Boolean> {
        deviceSupported?.let {
            return Futures.immediateFuture(it)
        }
        return query(
            description = "isDeviceSupported",
            startTask = { getClient(appContext).isDeviceSupported() },
            onResult = { deviceSupported = it },
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isCameraSupportedAsync(
        appContext: Context,
        cameraId: String,
    ): ListenableFuture<Boolean> {
        cameraSupported[cameraId]?.let {
            return Futures.immediateFuture(it)
        }
        return query(
            description = "isCameraSupported($cameraId)",
            startTask = { getClient(appContext).isCameraSupported(cameraId) },
            onResult = { cameraSupported[cameraId] = it },
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getClient(appContext: Context): LowLightBoostClient =
        clientFactoryForTesting?.invoke(appContext) ?: LowLightBoost.getClient(appContext)

    /**
     * Runs the Google Play Services query started by [startTask] and resolves to its result, or to
     * `false` if it fails or doesn't complete within [timeoutMs]. [onResult] is called with a
     * successful result, even if it arrives after the timeout.
     */
    private fun query(
        description: String,
        startTask: () -> Task<Boolean>,
        onResult: (Boolean) -> Unit,
    ): ListenableFuture<Boolean> {
        val resultFuture =
            try {
                startTask().toFuture(description, onResult)
            } catch (e: Exception) {
                Logger.w(TAG, "Low Light Boost $description failed", e)
                return Futures.immediateFuture(false)
            }
        val timeoutFuture =
            Futures.makeTimeoutFuture(
                timeoutMs,
                timeoutExecutor,
                false,
                /* cancelInputAtTimeout= */ false,
                resultFuture,
            )
        timeoutFuture.addListener(
            {
                // Don't log a timeout when the caller cancelled the future.
                if (!resultFuture.isDone && !timeoutFuture.isCancelled) {
                    Logger.w(TAG, "Low Light Boost $description timed out after ${timeoutMs}ms")
                }
            },
            CameraXExecutors.directExecutor(),
        )
        return timeoutFuture
    }

    private fun Task<Boolean>.toFuture(
        description: String,
        onResult: (Boolean) -> Unit,
    ): ListenableFuture<Boolean> = CallbackToFutureAdapter.getFuture { completer ->
        addOnCompleteListener(CameraXExecutors.directExecutor()) { task ->
            if (task.isSuccessful) {
                val supported = task.result == true
                onResult(supported)
                completer.set(supported)
            } else {
                val error = task.exception
                if (error != null) {
                    Logger.w(TAG, "Low Light Boost $description failed", error)
                } else {
                    Logger.w(TAG, "Low Light Boost $description failed")
                }
                completer.set(false)
            }
        }
        description
    }
}
