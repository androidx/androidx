# Module root

androidx.core core-pip

# Package androidx.core.pip

The PiP Jetpack library addresses several challenges in Android's Picture-in-Picture (PiP) mode:
- OS Fragmentation: The library handles differences in PiP API calls across Android versions, such as [`Activity#enterPictureInPictureMode`](https://developer.android.com/reference/android/app/Activity#enterPictureInPictureMode(android.app.PictureInPictureParams)) before Android S and [`PictureInPictureParams#isAutoEnterEnabled`](https://developer.android.com/reference/android/app/PictureInPictureParams) after.
- Incorrect PiP Parameters: It provides a unified solution for setting correct `PictureInPictureParams`, especially for playback, to ensure smooth animations (e.g., source rect hint).
- Unified PiP State Callbacks: The library consolidates [`Activity#onPictureInPictureModeChanged`](https://developer.android.com/reference/android/app/Activity.html#onPictureInPictureModeChanged(kotlin.Boolean)) and [`Activity#onPictureInPictureUiStateChanged`](https://developer.android.com/reference/android/app/Activity#onPictureInPictureUiStateChanged(android.app.PictureInPictureUiState)) into a single, unified callback interface via `PictureInPictureDelegate.OnPictureInPictureEventListener` for simplified state management.

Furthermore, all new PiP features will be delivered through the Jetpack library, ensuring that library adopters can access these features with minimal to no effort.

For developer guidance, check out the following guide:

https://developer.android.com/develop/ui/views/pip-jetpack