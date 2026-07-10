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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.projected.ProjectedDisplayController.Companion.create
import androidx.xr.projected.ProjectedDisplayController.PresentationMode.Companion.VISUALS_ON
import androidx.xr.projected.binding.ProjectedServiceConnection
import androidx.xr.projected.binding.ProjectedServiceConnection.ProjectedIntentAction.Companion.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedService
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * Controller for the Projected device display.
 *
 * Use [create] to create an instance of this class. Use [close] to clear the instance.
 */
public class ProjectedDisplayController
private constructor(
    private val connection: ProjectedServiceConnection,
    private val projectedService: IProjectedService,
    private val engagementModeClient: EngagementModeClient,
) : AutoCloseable {

    /**
     * Map to convert the provided listener to the one passed to the [EngagementModeClient]. This
     * should be accessed through the synchronizedPresentationListeners.
     */
    @ExperimentalProjectedApi
    private val presentationModeListeners =
        mutableMapOf<Consumer<PresentationModeFlags>, Consumer<Int>>()

    @ExperimentalProjectedApi
    private val synchronizedPresentationListeners =
        Collections.synchronizedMap(presentationModeListeners)

    /**
     * Convenience function to set the flag bits as as per the
     * [android.view.WindowManager.LayoutParams] flags. Flags will be cleared automatically after
     * the app stops.
     *
     * Supported flags:
     * - [android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
     *     - Note: Keeping the device's screen on can drain the battery quickly. Ordinarily, you
     *       should let the device turn the screen off if the user is not interacting with it. If
     *       you do need to keep the screen on, do so for as short a time as possible.
     *
     * If an unsupported flag is passed, this method does nothing.
     */
    @ExperimentalProjectedApi
    public fun addLayoutParamsFlags(@ProjectedLayoutParamsFlags flags: Int) {
        projectedService.addWindowFlags(flags)
    }

    /**
     * Convenience function to remove the flag bits as as per the
     * [android.view.WindowManager.LayoutParams] flags.
     *
     * Supported flags:
     * - [android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
     *
     * If an unsupported flag is passed, this method does nothing.
     */
    @ExperimentalProjectedApi
    public fun removeLayoutParamsFlags(@ProjectedLayoutParamsFlags flags: Int) {
        projectedService.clearWindowFlags(flags)
    }

    /**
     * The PresentationMode represents how an app is currently able to present content to the user
     * on a projected device.
     */
    @ExperimentalProjectedApi
    public class PresentationMode internal constructor(private val id: Int) {
        override fun toString(): String =
            when (id) {
                0 -> "VISUALS_ON"
                1 -> "AUDIO_ON"
                else -> "UNKNOWN($id)"
            }

        override fun equals(other: Any?): Boolean =
            (other is PresentationMode) && this.id == other.id

        override fun hashCode(): Int = id.hashCode()

        public companion object {
            /**
             * Indicates the presentation mode includes a visual presentation. When the visual
             * presentation mode is on, the display on the Projected device is on. When VISUALS_ON
             * is present, an exercise app could draw UI that indicates how far the user has reached
             * in their current run. when VISUALS_ON is not present, the app's attempt to draw will
             * not work.
             */
            @JvmField public val VISUALS_ON: PresentationMode = PresentationMode(0)

            /**
             * Indicates the presentation mode includes an audio presentation. This can be active
             * with or without [VISUALS_ON]. When the audio presentation mode is on, the speakers on
             * the Projected device are on. When AUDIO_ON is present, an exercise app could play
             * audio that communicates how far the user has reached in their current run. when
             * AUDIO_ON is not present, the app's attempt to play audio will not work.
             */
            @JvmField public val AUDIO_ON: PresentationMode = PresentationMode(1)
        }
    }

    /**
     * The PresentationModeFlags represents a collection of PresentationMode values that are
     * currently active.
     */
    @ExperimentalProjectedApi
    public class PresentationModeFlags
    internal constructor(private val presentationModes: Set<PresentationMode>) {

        override fun equals(other: Any?): Boolean =
            (other is PresentationModeFlags) && this.presentationModes == other.presentationModes

        override fun hashCode(): Int = presentationModes.hashCode()

        /**
         * Checks if the provided PresentationMode is present in the PresentationModeFlags.
         *
         * @param presentationMode The PresentationMode value to check.
         * @return `true` if the specified PresentationMode is present, `false` otherwise.
         */
        public fun hasPresentationMode(presentationMode: PresentationMode): Boolean =
            presentationModes.contains(presentationMode)

        /**
         * Checks if all the provided PresentationModes are present in the PresentationModeFlags.
         *
         * @param presentationModes The Set of PresentationMode values to check.
         * @return `true` if all the specified PresentationModes are present, `false` otherwise.
         */
        public fun hasPresentationMode(presentationModes: Set<PresentationMode>): Boolean =
            presentationModes.containsAll(presentationModes)
    }

    /**
     * Adds a callback to listen for updates to the PresentationModeFlags.
     *
     * The PresentationMode represents how a content is being presented to a projected application
     * (e.g. are visuals on). The callback will be called with the current state as soon as it is
     * available. A listener cannot be added a second time without first being removed.
     */
    @ExperimentalProjectedApi
    @JvmOverloads
    @RequiresApi(Build.VERSION_CODES.N)
    public fun addPresentationModeChangedListener(
        executor: Executor = Dispatchers.Main.asExecutor(),
        listener: Consumer<PresentationModeFlags>,
    ) {
        val convertedListener = Consumer { PresentationModes: Int ->
            listener.accept(convertToPresentationModeFlags(PresentationModes))
        }
        synchronizedPresentationListeners[listener] = convertedListener
        engagementModeClient.addUpdateCallback(executor, convertedListener)
    }

    /**
     * Remove a listener to stop consuming [PresentationModeFlags] values. If the listener has
     * already been removed then this is a no-op.
     */
    @ExperimentalProjectedApi
    public fun removePresentationModeChangedListener(listener: Consumer<PresentationModeFlags>) {
        this.synchronizedPresentationListeners.remove(listener)?.let { convertedListener ->
            engagementModeClient.removeUpdateCallback(convertedListener)
        }
    }

    /**
     * Disconnects from the service providing features for Projected devices. Methods from the
     * [ProjectedDisplayController] shouldn't be called after this.
     *
     * This method should be called in [android.app.Activity.onDestroy].
     */
    override fun close() {
        connection.disconnect()
    }

    @ExperimentalProjectedApi
    private fun convertToPresentationModeFlags(engagementModes: Int): PresentationModeFlags {
        val presentationModeSet: MutableSet<PresentationMode> = mutableSetOf()
        if (engagementModes and EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON != 0) {
            presentationModeSet.add(PresentationMode.VISUALS_ON)
        }
        if (engagementModes and EngagementModeClient.ENGAGEMENT_MODE_FLAG_AUDIO_ON != 0) {
            presentationModeSet.add(PresentationMode.AUDIO_ON)
        }
        return PresentationModeFlags(presentationModeSet)
    }

    public companion object {
        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedDisplayController] when the connection is established.
         *
         * @param activity The [Activity] running on a Projected device.
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @throws IllegalArgumentException if provided Activity is not running on a Projected
         *   device.
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        @ExperimentalProjectedApi
        public suspend fun create(activity: Activity): ProjectedDisplayController {
            require(
                ProjectedContext.isProjectedDeviceContext(activity),
                { "Provided Activity is not running on a Projected device." },
            )
            val serviceConnection = ProjectedServiceConnection(activity, ACTION_BIND)

            return ProjectedDisplayController(
                serviceConnection,
                projectedService = serviceConnection.connect(),
                EngagementModeClient(activity, Handler.createAsync(Looper.getMainLooper())),
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true, // Indicates these constants can be combined using bitwise ops.
        value = [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON],
    )
    public annotation class ProjectedLayoutParamsFlags
}
