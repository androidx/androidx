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

package androidx.xr.projected

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.xr.projected.ProjectedActivityCompat.Companion.create
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.binding.ProjectedServiceConnection.ProjectedIntentAction.Companion.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedInputEventListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedInputEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Class providing Projected device-specific features for Activity, like listening to Projected
 * input events.
 *
 * Use [create] to create an instance of this class. Use [close] to clear the instance.
 */
@ExperimentalProjectedApi
public class ProjectedActivityCompat
private constructor(
    private val connection: ProjectedServiceConnection,
    private val projectedService: IProjectedService,
) : AutoCloseable {

    /** Flow providing a stream of Projected input events. */
    public val projectedInputEvents: Flow<androidx.xr.projected.ProjectedInputEvent> =
        callbackFlow {
            val projectedActionsListener: IProjectedInputEventListener =
                object : IProjectedInputEventListener.Stub() {
                    override fun onProjectedInputEvent(inputEvent: ProjectedInputEvent) {
                        try {
                            val projectedInputAction =
                                androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
                                    .fromCode(inputEvent.action)
                            trySend(ProjectedInputEvent(projectedInputAction))
                        } catch (_: Exception) {}
                    }

                    override fun getInterfaceVersion(): Int = VERSION
                }

            val job = launch {
                connection.isServiceConnected.collect { isConnected ->
                    if (!isConnected) {
                        channel.close()
                    }
                }
            }

            projectedService.registerProjectedInputEventListener(projectedActionsListener)

            awaitClose {
                projectedService.unregisterProjectedInputEventListener(projectedActionsListener)
                job.cancel()
            }
        }

    override fun close() {
        connection.disconnect()
    }

    public companion object {
        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedActivityCompat] when the connection is established.
         *
         * @param context The context to use for binding to the service.
         * @return A [ProjectedActivityCompat] instance.
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @deprecated Use [ProjectedActivityCompat.create(activity: Activity)] instead
         *
         * TODO(b/479214966): Remove this once all clients have been migrated.
         */
        @JvmStatic
        @Deprecated("Use create(activity: Activity) instead")
        public suspend fun create(context: Context): ProjectedActivityCompat {
            val serviceConnection = ProjectedServiceConnection(context, ACTION_BIND)
            return ProjectedActivityCompat(
                serviceConnection,
                projectedService = serviceConnection.connect(),
            )
        }

        /**
         * Connects to the service providing features for Projected devices and returns the
         * ProjectedActivityCompat when the connection is established.
         *
         * @param activity The [Activity] running on a Projected device
         * @return A [ProjectedActivityCompat] instance
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @throws IllegalArgumentException if provided Activity is not running on a Projected
         *   device
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        public suspend fun create(activity: Activity): ProjectedActivityCompat {
            require(
                ProjectedContext.isProjectedDeviceContext(activity),
                { "Provided Activity is not running on a Projected device." },
            )
            val serviceConnection = ProjectedServiceConnection(activity, ACTION_BIND)
            return ProjectedActivityCompat(
                serviceConnection,
                projectedService = serviceConnection.connect(),
            )
        }
    }
}
