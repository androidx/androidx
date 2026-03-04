# Module root

androidx.core core-pip

# Package androidx.core.pip

The PiP Jetpack library addresses several challenges in Android's Picture-in-Picture (PiP) mode:
- OS Fragmentation: The library handles differences in PiP API calls across Android versions, such as [`Activity#enterPictureInPictureMode`](https://developer.android.com/reference/android/app/Activity#enterPictureInPictureMode(android.app.PictureInPictureParams)) before Android S and [`PictureInPictureParams#isAutoEnterEnabled`](https://developer.android.com/reference/android/app/PictureInPictureParams) after.
- Incorrect PiP Parameters: It provides a unified solution for setting correct `PictureInPictureParams`, especially for playback, to ensure smooth animations (e.g., source rect hint).
- Unified PiP State Callbacks: The library consolidates [`Activity#onPictureInPictureModeChanged`](https://developer.android.com/reference/android/app/Activity.html#onPictureInPictureModeChanged(kotlin.Boolean)) and [`Activity#onPictureInPictureUiStateChanged`](https://developer.android.com/reference/android/app/Activity#onPictureInPictureUiStateChanged(android.app.PictureInPictureUiState)) into a single, unified callback interface via `PictureInPictureDelegate.OnPictureInPictureEventListener` for simplified state management.

Furthermore, all new PiP features will be delivered through the Jetpack library, ensuring that library adopters can access these features with minimal to no effort.

# Usage of the library

This library depends on the latest `androidx.core` library, and it's recommended to use the `ComponentActivity` from the latest `androidx.activity` as well

- androidx.core:1.18.0-rc01+
- androidx.activity:1.13.0-rc01+ (optional, highly recommended)

The code snippets below would assume the application references both.

## Navigation and Video Call applications

For these usages

- Application does not need to specify the [`sourceRectHint`](https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setSourceRectHint(android.graphics.Rect))
- The [`seamlessResizeEnabled`](https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setSeamlessResizeEnabled(boolean)) flag is set to `FALSE` for non-video playback content; for smoother crossfading animation.
- Typically, does not listen on `ENTER_ANIMATION_START` and `ENTER_ANIMATION_END` events

```
// Pseudo code in Kotlin
class NavigationActivity :
        ComponentActivity(), PictureInPictureDelegate.OnPictureInPictureEventListener {

    private lateinit var pictureInPictureImpl: BasicPictureInPicture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pictureInPictureImpl = BasicPictureInPicture(this)
        pictureInPictureImpl.addOnPictureInPictureEventListener(getMainExecutor(), this)
    }

    override fun onPictureInPictureEvent(
        event: PictureInPictureDelegate.Event,
        newConfig: Configuration?
    ) {
        when (event) {
            PictureInPictureDelegate.Event.ENTERED -> {
                /* Change to PiP layout*/
            }
            PictureInPictureDelegate.Event.STASHED -> {
                /* Optional: PiP is now in stashed state */
            }
            PictureInPictureDelegate.Event.UNSTASHED -> {
                /* Optional: PiP is now in unstashed state */
            }
            PictureInPictureDelegate.Event.EXITED -> {
                /* Change to full-screen layout*/
            }
        }
    }

    private fun onNavigationStateChanged(isInActiveNavigation: Boolean) {
        pictureInPictureImpl.apply {
            setEnabled(isInActiveNavigation)
            setAspectRatio(desiredAspectRatio)
            setActions(actions)
        }
    }
}
```

## Video Playback applications

For the video playback usage
- Application can specify the player view, and the library can continuously track the view bounds as [`sourceRectHint`](https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setSourceRectHint(android.graphics.Rect))
- The [`seamlessResizeEnabled`](https://developer.android.com/reference/android/app/PictureInPictureParams.Builder#setSeamlessResizeEnabled(boolean)) flag is set to `TRUE`
- It's highly recommended to listen on `ENTER_ANIMATION_START` event to hide the overlays upon the video to achieve a cleaner entering PiP animation


```
// Pseudo code in Kotlin
class VideoPlaybackActivity :
        ComponentActivity(), PictureInPictureDelegate.OnPictureInPictureEventListener {

    private lateinit var pictureInPictureImpl: VideoPlaybackPictureInPicture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pictureInPictureImpl = VideoPlaybackPictureInPicture(this)
        pictureInPictureImpl.addOnPictureInPictureEventListener(getMainExecutor(), this)
    }

    override fun onDestroy() {
        super.onDestroy()
        pictureInPictureImpl.close()
    }

    override fun onPictureInPictureEvent(
        event: PictureInPictureDelegate.Event,
        newConfig: Configuration?
    ) {
        when (event) {
            PictureInPictureDelegate.Event.ENTER_ANIMATION_START -> {
                /* Optional: hide overlays that are hidden in PiP mode. */
            }
            PictureInPictureDelegate.Event.ENTER_ANIMATION_END -> {
                /* Optional: the animation to enter PiP ends */
            }
            PictureInPictureDelegate.Event.ENTERED -> {
                /* Change to PiP layout*/
            }
            PictureInPictureDelegate.Event.STASHED -> {
                /* Optional: PiP is now in stashed state */
            }
            PictureInPictureDelegate.Event.UNSTASHED -> {
                /* Optional: PiP is now in unstashed state */
            }
            PictureInPictureDelegate.Event.EXITED -> {
                /* Change to full-screen layout*/
            }
        }
    }

    private fun onPlaybackStateChanged(isPlaying: Boolean) {
        pictureInPictureImpl.apply {
            setEnabled(isPlaying)
            setPlayerView(if (isPlaying) playerView else null)
            setAspectRatio(videoAspectRatio)
            setActions(actions)
        }
    }
}
```
# Migration from platform APIs

1. Keep the AndroidManifest requirements on the activity.

    Example
    ```
    <activity android:name="VideoActivity"
        android:supportsPictureInPicture="true"
        android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation">
    </activity>
    ```
2. Include the required dependencies.

    Example
    ```
    dependencies {
        implementation("androidx.core:core:1.18.0-rc01")
        implementation("androidx.activity:activity:1.13.0-rc01")
        implementation("androidx.core:core-pip:1.0.0-beta01")
    }

    ```
3. Select the template that best fits your use case
    - BasicPictureInPicture: Best for Navigation and Video Call apps where
        seamless resize is not typically supported, and no source rect hint is
        needed.
    - VideoPlaybackPictureInPicture: Designed for video playback. It automatically
        tracks player view bounds for the source rect hint and enables seamless
        resize by default.
4. Entering PiP
   - Legacy implementation
        - Apps must differentiate by API level: Manually call
        `enterPictureInPicture` in `OnUserLeaveHint` (< Android S) or use
        `setAutoEnabled(boolean)` (>= Android S).
    - Jetpack implementation
        - Developers no longer need to manage API-specific logic for PiP. You
        can eliminate previous `onUserLeaveHint` code and instead trigger
       `setEnabled(boolean)` on your template instance whenever the PiP eligibility status changes.
5. Receiving PiP Callbacks
    - Legacy implementation
        - Relies on separate callbacks: `onPictureInPictureModeChanged` for
        layout toggling and `onPictureInPictureUiStateChanged` for
        animation/stashing states
    - Jetpack implementation
        - Uses a unified event-based callback via
        `addOnPictureInPictureEventListener`, covering `ENTERED`, `EXITED`,
        `STASHED`, `UNSTASHED` and etc. in one place.
6. Providing actions in the PiP menu
    - Legacy implementation
        - Updating `PictureInPictureParams` manually whenever actions change.
    - Jetpack implementation
        - Updating via `setActions(list)` on your template instance whenever
        actions change.


