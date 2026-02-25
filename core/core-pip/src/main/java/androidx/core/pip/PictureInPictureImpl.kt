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

/**
 * Basic Picture-in-Picture implementation.
 *
 * Configures PiP with a specific aspect ratio, custom actions, and controls enter behavior.
 * Seamless resize is disabled and no sourceRectHint is used.
 */
public open class BasicPictureInPicture(pictureInPictureProvider: PictureInPictureProvider) :
    PictureInPictureDelegate(pictureInPictureProvider) {
    protected val pictureInPictureParamsBuilder: PictureInPictureParamsCompat.Builder =
        PictureInPictureParamsCompat.Builder()

    init {
        pictureInPictureParamsBuilder.setSeamlessResizeEnabled(false)
        setPictureInPictureParams(pictureInPictureParamsBuilder.build())
    }

    /**
     * Sets the desired aspect ratio for the Picture-in-Picture window.
     *
     * @param aspectRatio The desired width/height ratio.
     * @return This implementation instance for chaining.
     */
    public fun setAspectRatio(aspectRatio: Rational): BasicPictureInPicture {
        pictureInPictureParamsBuilder.setAspectRatio(aspectRatio)
        setPictureInPictureParams(pictureInPictureParamsBuilder.build())
        return this
    }

    /**
     * Sets whether the activity should automatically enter Picture-in-Picture mode when eligible
     * (e.g., when swiping to home). This indicates the "willingness to enter PiP".
     *
     * @param enabled True if the Activity is PiP-able, false otherwise.
     * @return This implementation instance for chaining.
     */
    public fun setEnabled(enabled: Boolean): BasicPictureInPicture {
        pictureInPictureParamsBuilder.setEnabled(enabled)
        setPictureInPictureParams(pictureInPictureParamsBuilder.build())
        return this
    }

    /**
     * Sets the custom actions to be available in the Picture-in-Picture menu.
     *
     * @param actions A list of RemoteActions.
     * @return This implementation instance for chaining.
     */
    public fun setActions(actions: List<RemoteAction>): BasicPictureInPicture {
        pictureInPictureParamsBuilder.setActions(actions)
        setPictureInPictureParams(pictureInPictureParamsBuilder.build())
        return this
    }
}

/**
 * Picture-in-Picture implementation optimized for Video Playback applications.
 *
 * Enables seamless resize and allows tracking a View to automatically update the source rectangle
 * hint for smooth animations using the package's ViewBoundsTracker.
 */
public class VideoPlaybackPictureInPicture(provider: PictureInPictureProvider) :
    BasicPictureInPicture(provider), AutoCloseable {

    private val viewBoundsChangedListener: ViewBoundsTracker.OnViewBoundsChangedListener =
        object : ViewBoundsTracker.OnViewBoundsChangedListener {
            override fun onViewBoundsChanged(view: View, newBounds: Rect) {
                pictureInPictureParamsBuilder.setSourceRectHint(newBounds)
                setPictureInPictureParams(pictureInPictureParamsBuilder.build())
            }
        }

    private var viewBoundsTracker: ViewBoundsTracker? = null

    init {
        pictureInPictureParamsBuilder.setSeamlessResizeEnabled(true)
        setPictureInPictureParams(pictureInPictureParamsBuilder.build())
    }

    /**
     * Sets the View to be tracked for updating the source rectangle hint. The bounds of this View
     * (e.g., the player view) will be used to ensure smooth entry/exit animations into/out of
     * Picture-in-Picture.
     *
     * @param view The View to track, or null to stop tracking and clear the hint.
     * @return This implementation instance for chaining.
     */
    public fun setPlayerView(view: View?): VideoPlaybackPictureInPicture {
        // Close any previous tracker
        close()

        if (view != null) {
            viewBoundsTracker =
                ViewBoundsTracker(view).apply { addListener(viewBoundsChangedListener) }
            val initialBounds = Rect()
            if (view.getGlobalVisibleRect(initialBounds)) {
                pictureInPictureParamsBuilder.setSourceRectHint(initialBounds)
                setPictureInPictureParams(pictureInPictureParamsBuilder.build())
            }
        } else {
            // Clear hint if view is removed
            pictureInPictureParamsBuilder.setSourceRectHint(null)
            setPictureInPictureParams(pictureInPictureParamsBuilder.build())
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
