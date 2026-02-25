# Module root

androidx.core core-pip

# Package androidx.core.pip

The PiP Jetpack library addresses several challenges in Android's Picture-in-Picture (PiP) mode:
- OS Fragmentation: The library handles differences in PiP API calls across Android versions, such as enterPictureInPictureMode before Android S and isAutoEnterEnabled after.
- Incorrect PiP Parameters: It provides a unified solution for setting correct PiP parameters, especially for playback, to ensure smooth animations (e.g., source rect hint).
- Unified PiP State Callbacks: The library consolidates onPictureInPictureModeChanged and onPictureInPictureUiStateChanged into a single, unified callback interface for simplified state management.
- Boilerplate Code: It reduces boilerplate by offering predefined RemoteAction sets for common use cases like playback and video calls.

Furthermore, all new PiP features will be delivered through the Jetpack library, ensuring that library adopters can access these features with minimal to no effort.

# Usage of the library

This library depends on the latest `androidx.core` library, and it's recommended to use the ComponentActivity from the latest `androidx.activity` as well

- androidx.core 1.18.0-alpha01
- androidx.activity 1.13.0-alpha01 (optional, highly recommended)

The code snippets below would assume the application references both.

## Navigation and Video Call applications

For these usages

- Application does not need to specify the sourceRectHint
- The seamlessResize flag is set to false
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
- Application can specify the player view, and the library can continuously track the view bounds as sourceRectHint
- The seamlessResize flag is set to true
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
