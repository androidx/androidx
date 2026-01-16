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

package androidx.core.pip

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.app.PictureInPictureParamsCompat
import androidx.core.app.PictureInPictureProvider
import androidx.core.app.PictureInPictureUiStateCompat
import androidx.core.util.Consumer
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

/**
 * A delegate class to help set up PiP (Picture-in-Picture) functionalities on behalf of the given
 * [PictureInPictureProvider] instance.
 *
 * It's highly recommended to choose one of the implementations: [BasicPictureInPicture],
 * [VideoPlaybackPictureInPicture] instead of using this class directly.
 *
 * @param pictureInPictureProvider [PictureInPictureProvider] instance that this delegate will call
 *   into for actual Picture-in-Picture functionalities.
 */
public open class PictureInPictureDelegate(pictureInPictureProvider: PictureInPictureProvider) {

    private var pictureInPictureProviderRef: WeakReference<PictureInPictureProvider> =
        WeakReference(pictureInPictureProvider)

    private var pictureInPictureParamsCompat = PictureInPictureParamsCompat(isEnabled = false)

    /**
     * Explicitly calls [PictureInPictureProvider.enterPictureInPictureMode] on behalf of
     * [pictureInPictureProviderRef] if it's in PiP-able state and autoEnter is not available.
     */
    private val onUserLeaveHint: Runnable = Runnable {
        if (pictureInPictureParamsCompat.isEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // autoEnter is available, skip onUserLeaveHint callback.
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // autoEnter is not available, enter PiP with params.
                pictureInPictureProviderRef
                    .get()
                    ?.enterPictureInPictureMode(pictureInPictureParamsCompat)
            }
        }
    }

    private var lastReportedIsTransitioningToPip: Boolean = false
    private var lastReportedIsStashed: Boolean = false

    private val onPictureInPictureModeChanged: Consumer<PictureInPictureModeChangedInfo> =
        Consumer { pictureInPictureModeChangedInfo ->
            // Clear the internal trackers any time the Activity fully enters or exits PiP mode.
            lastReportedIsTransitioningToPip = false
            lastReportedIsStashed = false
            val event =
                if (pictureInPictureModeChangedInfo.isInPictureInPictureMode) Event.ENTERED
                else Event.EXITED
            val config: Configuration? =
                if (Build.VERSION.SDK_INT >= 26) pictureInPictureModeChangedInfo.newConfig else null
            for ((listener, executor) in mOnPictureInPictureEventListeners) {
                executor.execute { listener.onPictureInPictureEvent(event, config) }
            }
        }

    private val mOnPictureInPictureUiEventChanged: Consumer<PictureInPictureUiStateCompat> =
        Consumer { pictureInPictureUiStateCompat ->
            var event: Event? = null
            if (lastReportedIsStashed && !pictureInPictureUiStateCompat.isStashed) {
                event = Event.UNSTASHED
            } else if (!lastReportedIsStashed && pictureInPictureUiStateCompat.isStashed) {
                event = Event.STASHED
            } else if (
                lastReportedIsTransitioningToPip &&
                    !pictureInPictureUiStateCompat.isTransitioningToPip
            ) {
                event = Event.ENTER_ANIMATION_END
            } else if (
                !lastReportedIsTransitioningToPip &&
                    pictureInPictureUiStateCompat.isTransitioningToPip
            ) {
                event = Event.ENTER_ANIMATION_START
            }
            if (event != null) {
                for ((listener, executor) in mOnPictureInPictureEventListeners) {
                    executor.execute { listener.onPictureInPictureEvent(event, null) }
                }
            }
            lastReportedIsStashed = pictureInPictureUiStateCompat.isStashed
            lastReportedIsTransitioningToPip = pictureInPictureUiStateCompat.isTransitioningToPip
        }

    private val mOnPictureInPictureEventListeners =
        mutableMapOf<OnPictureInPictureEventListener, Executor>()

    init {
        pictureInPictureProvider.addOnUserLeaveHintListener(onUserLeaveHint)
        pictureInPictureProvider.addOnPictureInPictureModeChangedListener(
            onPictureInPictureModeChanged
        )
        pictureInPictureProvider.addOnPictureInPictureUiStateChangedListener(
            mOnPictureInPictureUiEventChanged
        )
    }

    /**
     * Sets the [PictureInPictureParamsCompat] instance for PiP.
     *
     * @param pictureInPictureParamsCompat [PictureInPictureParamsCompat] instance to set, and it's
     *   subjected to be changed. For instance, the aspectRatio would be capped in between the
     *   minimal and maximum allowed aspectRatio; and the sourceRectHint would be center cropped to
     *   match the aspectRatio.
     */
    public fun setPictureInPictureParams(
        pictureInPictureParamsCompat: PictureInPictureParamsCompat
    ) {
        val validatedParams = PictureInPictureParamsValidator.validate(pictureInPictureParamsCompat)
        this@PictureInPictureDelegate.pictureInPictureParamsCompat = validatedParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureProviderRef.get()?.setPictureInPictureParams(validatedParams)
        }
    }

    /** Adds [OnPictureInPictureEventListener] for events sent from system. */
    public fun addOnPictureInPictureEventListener(
        executor: Executor,
        listener: OnPictureInPictureEventListener,
    ) {
        mOnPictureInPictureEventListeners[listener] = executor
    }

    /** Removes [OnPictureInPictureEventListener] for events sent from system. */
    public fun removeOnPictureInPictureEventListener(listener: OnPictureInPictureEventListener) {
        mOnPictureInPictureEventListeners.remove(listener)
    }

    /**
     * Unified listener interface for [Activity.onPictureInPictureModeChanged] and
     * [Activity.onPictureInPictureUiStateChanged] callbacks.
     */
    public interface OnPictureInPictureEventListener {
        /**
         * Received an event emitted from the system.
         *
         * @param event [Event] instance indicates what's happening in the system.
         * @param config [Configuration] instance which is available since API 26 for
         *   [Event.ENTERED] and [Event.EXITED], it is `null` otherwise.
         */
        public fun onPictureInPictureEvent(event: Event, config: Configuration?)
    }

    /** Represents the PiP event emitted from the system. */
    public class Event private constructor(private val description: String) {
        override fun toString(): String {
            return description
        }

        public companion object {
            /**
             * Entering PiP animation starts, this offers an app the chance to hide UI elements such
             * as the overlays upon the video player. Compatibility notes: this event is available
             * since API 35, see also [PictureInPictureUiStateCompat.isTransitioningToPip].
             */
            @JvmField public val ENTER_ANIMATION_START: Event = Event("Enter PiP animation starts")
            /**
             * Entering PiP animation ends, apps usually do not need to take actions for this state.
             * Compatibility notes: this event is available since API 35, see also
             * [PictureInPictureUiStateCompat.isTransitioningToPip]
             */
            @JvmField public val ENTER_ANIMATION_END: Event = Event("Enter PiP animation ends")
            /**
             * App has fully settled in PiP mode, app usually needs to switch its Activity layout to
             * PiP mode. Compatibility notes: this event is available since API 24, see also
             * [PictureInPictureModeChangedInfo.isInPictureInPictureMode].
             */
            @JvmField public val ENTERED: Event = Event("App has entered PiP mode")
            /**
             * PiP window is stashed. A stashed PiP means it is only partially visible to the user,
             * with some parts of it being off-screen. Compatibility notes: this event is available
             * since API 31, see also [PictureInPictureUiStateCompat.isStashed].
             */
            @JvmField public val STASHED: Event = Event("PiP window is stashed")
            /**
             * PiP window is un-stashed. A stashed PiP means it is only partially visible to the
             * user, with some parts of it being off-screen. Compatibility notes: this event is
             * available since API 31, see also [PictureInPictureUiStateCompat.isStashed].
             */
            @JvmField public val UNSTASHED: Event = Event("PiP window is unstashed")
            /**
             * App has exited PiP mode, app usually needs to switch its Activity layout to
             * full-screen mode. Compatibility notes: this event is available since API 24, see also
             * [PictureInPictureModeChangedInfo.isInPictureInPictureMode].
             */
            @JvmField public val EXITED: Event = Event("App has exited PiP mode")
        }
    }
}
