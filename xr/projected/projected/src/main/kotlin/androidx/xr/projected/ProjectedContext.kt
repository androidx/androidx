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

import android.app.ActivityOptions
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting

/**
 * Helper for accessing Projected device [Context] and its features.
 *
 * Projected device is an XR device connected to an Android device (host). Host can project the
 * application content to the Projected device and let users interact with it.
 *
 * The Projected device context will ensure Projected device system services are returned, when
 * queried for system services from this object.
 *
 * Note: The application context's deviceId can switch between the Projected and host deviceId
 * depending on which activity was most recently in the foreground. Prefer using the Activity
 * context to minimize the risk of running into this problem.
 */
public object ProjectedContext {

    private const val TAG = "ProjectedContext"

    @VisibleForTesting internal const val PROJECTED_DEVICE_NAME = "ProjectionDevice"

    @VisibleForTesting
    internal const val REQUIRED_LAUNCH_FLAGS =
        (Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
            Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /**
     * Explicitly create the Projected device context from any context object. It returns null if
     * the projected device was not found.
     */
    @JvmStatic
    public fun createProjectedDeviceContext(context: Context): Context {
        val deviceId =
            getProjectedDeviceId(context)
                ?: throw IllegalStateException("Projected device not found.")
        return context.createDeviceContext(deviceId)
    }

    /**
     * Explicitly create the host device context from any context object. The host is the device
     * that connects to a Projected device.
     *
     * If an application is using a Projected device context and it wants to use system services
     * from the host (e.g. phone), it needs to use the host device context.
     */
    @JvmStatic
    public fun createHostDeviceContext(context: Context): Context =
        context.createDeviceContext(Context.DEVICE_ID_DEFAULT)

    /**
     * Returns the name of the Projected device or null if either virtual device wasn't found or the
     * name of the virtual device wasn't set.
     *
     * @throws IllegalArgumentException If another context is used (e.g. the host context).
     */
    @JvmStatic
    public fun getProjectedDeviceName(context: Context): String? =
        // TODO: b/424812882 - Turn this into a lint check with an annotation.
        if (isProjectedDeviceContext(context)) {
            getVirtualDevice(context)?.name
        } else {
            throw IllegalArgumentException(
                "Provided context is not the Projected device context. Can't get the device name."
            )
        }

    /** Returns whether the provided context is the Projected device context. */
    @JvmStatic
    public fun isProjectedDeviceContext(context: Context): Boolean =
        getVirtualDevice(context)?.name?.startsWith(PROJECTED_DEVICE_NAME) == true

    /**
     * Takes an [Intent] with the description of the activity to start and returns it with added
     * flags to start the activity on the Projected device.
     *
     * @param intent The description of the activity to start.
     */
    @JvmStatic
    public fun addProjectedFlags(intent: Intent): Intent = intent.addFlags(REQUIRED_LAUNCH_FLAGS)

    /**
     * Creates [ActivityOptions] that should be used to start an activity on the Projected device.
     *
     * If the Projected device have more than one associated display, the activity will be started
     * on the first one.
     *
     * @param context The Projected device context.
     */
    @JvmStatic
    public fun createProjectedActivityOptions(context: Context): ActivityOptions {
        // TODO: b/424812882 - Turn this into a lint check with an annotation.
        if (!isProjectedDeviceContext(context)) {
            throw IllegalArgumentException("Provided context is not the Projected device context.")
        }

        val displayIds = getProjectedDisplayIds(context)

        if (displayIds.isEmpty()) {
            throw IllegalStateException("No projected display found.")
        }

        if (displayIds.size > 1) {
            // TODO: b/424812731 - Add support for multiple display IDs.
            Log.w(TAG, "More than one projected display found. Selecting the first one.")
        }

        return ActivityOptions.makeBasic().setLaunchDisplayId(displayIds.first())
    }

    private fun getProjectedDeviceId(context: Context) =
        context
            .getSystemService(VirtualDeviceManager::class.java)
            .virtualDevices
            // TODO: b/424824481 - Replace the name matching with a better method.
            .find { it.name?.startsWith(PROJECTED_DEVICE_NAME) ?: false }
            ?.deviceId

    private fun getVirtualDevice(context: Context) =
        context.getSystemService(VirtualDeviceManager::class.java).virtualDevices.find {
            it.deviceId == context.deviceId
        }

    private fun getProjectedDisplayIds(context: Context) =
        getVirtualDevice(context)?.displayIds ?: IntArray(size = 0)
}
