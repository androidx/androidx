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

package androidx.core.pip

import android.app.RemoteAction
import android.graphics.Rect
import android.util.Rational
import android.view.View
import androidx.core.app.PictureInPictureParamsCompat
import androidx.core.app.PictureInPictureProvider
import java.util.concurrent.Executor

/**
 * Basic Picture-in-Picture implementation.
 *
 * Configures PiP with a specific aspect ratio, custom actions, and controls enter behavior.
 * Seamless resize is disabled and no sourceRectHint is used.
 *
 * @param pictureInPictureProvider [PictureInPictureProvider] instance that this delegate will call
 *   into for actual Picture-in-Picture functionalities.
 * @param executor The executor to use for applying the Picture-in-Picture parameters. It is
 *   recommended to use a background executor to offload these framework calls from the main thread.
 */
public open class BasicPictureInPicture(
    pictureInPictureProvider: PictureInPictureProvider,
    private val executor: Executor,
) : PictureInPictureDelegate(pictureInPictureProvider) {
    protected val pictureInPictureParamsBuilder: PictureInPictureParamsCompat.Builder =
        PictureInPictureParamsCompat.Builder()

    internal val implementationLock: Any = Any()
    internal var pendingParams: PictureInPictureParamsCompat? = null
    internal var isTaskScheduled: Boolean = false

    init {
        pictureInPictureParamsBuilder.setSeamlessResizeEnabled(false)
    }

    /**
     * Sets the desired aspect ratio for the Picture-in-Picture window.
     *
     * Callers must invoke [commit] to apply the changes.
     *
     * @param aspectRatio The desired width/height ratio.
     * @return This implementation instance for chaining.
     */
    public fun setAspectRatio(aspectRatio: Rational): BasicPictureInPicture {
        synchronized(implementationLock) {
            pictureInPictureParamsBuilder.setAspectRatio(aspectRatio)
        }
        return this
    }

    /**
     * Sets whether the activity should automatically enter Picture-in-Picture mode when eligible
     * (e.g., when swiping to home). This indicates the "willingness to enter PiP".
     *
     * Callers must invoke [commit] to apply the changes.
     *
     * @param enabled True if the Activity is PiP-able, false otherwise.
     * @return This implementation instance for chaining.
     */
    public fun setEnabled(enabled: Boolean): BasicPictureInPicture {
        synchronized(implementationLock) { pictureInPictureParamsBuilder.setEnabled(enabled) }
        return this
    }

    /**
     * Sets the custom actions to be available in the Picture-in-Picture menu.
     *
     * Callers must invoke [commit] to apply the changes.
     *
     * @param actions A list of RemoteActions.
     * @return This implementation instance for chaining.
     */
    public fun setActions(actions: List<RemoteAction>): BasicPictureInPicture {
        synchronized(implementationLock) { pictureInPictureParamsBuilder.setActions(actions) }
        return this
    }

    /**
     * Commits the changes made through the setter methods and applies them to the current
     * Picture-in-Picture session.
     *
     * This method builds the [PictureInPictureParamsCompat] and schedules an update using the
     * executor provided at construction.
     */
    public fun commit() {
        synchronized(implementationLock) {
            val params = pictureInPictureParamsBuilder.build()
            pendingParams = params
            // Update local state immediately so enterPip uses the latest params.
            updateLocalParams(params)
            scheduleParamsUpdate()
        }
    }

    internal fun scheduleParamsUpdate() {
        if (!isTaskScheduled) {
            isTaskScheduled = true
            executor.execute {
                val paramsToApply: PictureInPictureParamsCompat
                synchronized(implementationLock) {
                    paramsToApply = pendingParams!!
                    isTaskScheduled = false
                }
                setPictureInPictureParams(paramsToApply)
            }
        }
    }
}

/**
 * Picture-in-Picture implementation optimized for Video Playback applications.
 *
 * Enables seamless resize and allows tracking a View to automatically update the source rectangle
 * hint for smooth animations using the package's ViewBoundsTracker.
 *
 * @param provider [PictureInPictureProvider] instance that this delegate will call into for actual
 *   Picture-in-Picture functionalities.
 * @param executor The executor to use for applying the Picture-in-Picture parameters. It is
 *   recommended to use a background executor to offload these framework calls from the main thread.
 */
public class VideoPlaybackPictureInPicture(provider: PictureInPictureProvider, executor: Executor) :
    BasicPictureInPicture(provider, executor), AutoCloseable {

    private val viewBoundsChangedListener: ViewBoundsTracker.OnViewBoundsChangedListener =
        object : ViewBoundsTracker.OnViewBoundsChangedListener {
            override fun onViewBoundsChanged(view: View, newBounds: Rect) {
                synchronized(implementationLock) {
                    pictureInPictureParamsBuilder.setSourceRectHint(newBounds)
                    val params = pictureInPictureParamsBuilder.build()
                    pendingParams = params
                    updateLocalParams(params)
                    scheduleParamsUpdate()
                }
            }
        }

    private var viewBoundsTracker: ViewBoundsTracker? = null

    init {
        pictureInPictureParamsBuilder.setSeamlessResizeEnabled(true)
    }

    /**
     * Sets the View to be tracked for updating the source rectangle hint. The bounds of this View
     * (e.g., the player view) will be used to ensure smooth entry/exit animations into/out of
     * Picture-in-Picture.
     *
     * Callers must invoke [commit] to apply the changes.
     *
     * @param view The View to track, or null to stop tracking and clear the hint.
     * @return This implementation instance for chaining.
     */
    public fun setPlayerView(view: View?): VideoPlaybackPictureInPicture {
        // Close any previous tracker
        close()

        synchronized(implementationLock) {
            if (view != null) {
                viewBoundsTracker =
                    ViewBoundsTracker(view).apply { addListener(viewBoundsChangedListener) }
                val initialBounds = Rect()
                if (view.getGlobalVisibleRect(initialBounds)) {
                    pictureInPictureParamsBuilder.setSourceRectHint(initialBounds)
                }
            } else {
                // Clear hint if view is removed
                pictureInPictureParamsBuilder.setSourceRectHint(null)
            }
        }
        return this
    }

    /**
     * Releases resources used by this implementation, such as the ViewBoundsTracker. Call this when
     * the implementation is no longer needed (e.g., in Activity.onDestroy).
     */
    public override fun close() {
        viewBoundsTracker?.release()
        viewBoundsTracker = null
    }
}
