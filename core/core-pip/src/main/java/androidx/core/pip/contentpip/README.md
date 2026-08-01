# Dynamic Task PiP

This package provides a Content PiP solution for Android applications that need to maintain Picture-in-Picture (PiP) functionality when the system's native [Auto-PiP](https://developer.android.com/reference/android/app/PictureInPictureParams#isAutoEnterEnabled()) is suppressed or unavailable.

## Overview

In certain scenarios, such as when a user uses the "quick switch" gesture (swiping horizontally on the navigation bar) to navigate away from a PiP-eligible Activity, native [Auto-PiP](https://developer.android.com/reference/android/app/PictureInPictureParams#isAutoEnterEnabled()) may be suppressed by the system. The Content PiP solution addresses this by:

1.  **Isolated Task**: Starting an internal, translucent Activity in a dedicated task (`taskAffinity="androidx.core.pip.contentpip.isolated"`).
2.  **Payload Handoff**: Providing a callback mechanism (`ContentPipCallback`) to move content (e.g., a Media Player) from the main Activity to the PiP container Activity.
3.  **Lifecycle Management**: Automatically managing the PiP task's lifecycle, including "pulling back" the content to the main Activity when the user returns to the app.

## Key Components

*   **`ContentPip.enableAppOnAppSwitch(Activity, ContentPipCallback)`**: The entry point to enable the fallback pipeline for a `ComponentActivity`.
*   **`ContentPipCallback`**: The interface developers implement to manage their content during the handoff:
    *   `onInitContentPip()`: Return `true` if the app should enter PiP (e.g., video is playing).
    *   `onPrepareContentPip()`: Prepare the main Activity for handoff (e.g., detaching a player from its view).
    *   `onAttachContentPip(pipActivity)`: Attach the content to the new PiP container Activity.
    *   `onFinishContentPip(isStopped)`: Clean up when PiP ends or the user returns to the main app.

## Best Practices

*   **UI Continuity**: Use the provided lifecycle hooks to ensure the transition between the main task and the PiP task is seamless.
*   **Media Management**: Ensure your media player (like Media3 ExoPlayer) is managed in a way that allows it to be detached and re-attached to different `PlayerView` instances without resetting the playback state.
*   **Resource Cleanup**: Always handle `onFinishContentPip` to release resources and stop playback if the PiP task is dismissed.
