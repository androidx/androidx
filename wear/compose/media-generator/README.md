# Wear Compose Media Generator

This module is an internal developer tool for generating sample screenshots and videos for **Wear Compose Material 3** documentation on developer.android.com (DAC). It creates side-by-side composite media showcasing components rendered across standard regular and large Wear OS round display profiles.

It is cross-platform and can be run on both macOS and Linux.

Samples from other modules can also be added for media generation using this framework.

## Requirements

Before running the scripts, you need:

1. **FFmpeg**: Must be installed (`brew install ffmpeg` on macOS or `sudo apt-get install ffmpeg` on Linux).
2. **Python 3.8+ & Libraries**: Python 3 with OpenCV and NumPy installed:
   * **macOS:** `pip3 install opencv-python numpy`
   * **Linux:** `sudo apt-get install python3-opencv python3-numpy` (or `pip3 install opencv-python numpy`)
3. **Two Emulators Running**: Create and launch both emulators in Android Studio (**Open Android Studio -> Device Manager -> (+) Add new Device -> Form Factor: Wear OS**):
   * **Regular Round**: 454x454 px / 213dp, 320 dpi (e.g. "Wear OS Large Round" profile)
   * **Large Round**: 480x480 px / 240dp, 320 dpi (e.g. "Wear OS XL Round" profile)
   * **CLI Alternative**: If you prefer launching pre-created emulators from the command line without opening Android Studio:
     ```bash
     $ANDROID_HOME/emulator/emulator -avd <regular_watch_avd_name> &
     $ANDROID_HOME/emulator/emulator -avd <large_watch_avd_name> &
     ```

> **Note:**
> - Emulator AVD names can be anything; the scripts automatically discover connected devices by querying screen resolutions via `adb shell wm size`.
> - Both emulators must be running simultaneously before executing the scripts (`setup_environment()` will verify their resolutions).
> - Don't click or interact with the emulators while scripts are running to avoid recording artifacts.

## Generating Videos

From the root of the AndroidX repository (`frameworks/support/`), run:

```bash
# Generate all registered sample videos:
python3 wear/compose/media-generator/scripts/generate_all_videos.py --output_dir /absolute/path/to/save/videos

# Or generate a single target sample quickly:
python3 wear/compose/media-generator/scripts/generate_all_videos.py --output_dir /absolute/path/to/save/videos --sample ButtonGroupSample
```

### Adding New Video Samples
1. **Register in Kotlin**: Add your `@Composable` sample function to `src/main/java/.../VideoRegistry.kt`:
   ```kotlin
   // Example:
   "MyNewSample" to { MyNewSample() },
   ```
2. **Register in Python**: Map your sample name to its gesture handler in `SAMPLE_GESTURES_MAP` inside `scripts/gestures.py`:
   ```python
   # Example:
   "MyNewSample": AUTOPLAY_HANDLER,  # (or DOUBLE_TAP_CENTER_HANDLER, etc.)
   ```

*(Note: The main pipeline automatically handles all screen recording, pre-roll/post-roll pause timers, OpenCV animation synchronization, and FFmpeg side-by-side compositing!)*

## Generating Screenshots

Screenshots use a simple, static rendering system without gesture choreography.

From the root of the AndroidX repository (`frameworks/support/`), run:

```bash
python3 wear/compose/media-generator/scripts/generate_all_screenshots.py --output_dir /absolute/path/to/save/screenshots
```

### Adding New Screenshot Samples

1. **Register the sample**: Add your sample to either `tlcScreenshotRegistry` (for standard components like buttons and cards) or `boxScreenshotRegistry` (for full-screen components like pickers and progress indicators) inside `src/main/java/.../ScreenshotRegistry.kt`. For example:
```kotlin
val boxScreenshotRegistry: Map<String, @Composable () -> Unit> =
    mapOf(
        "DatePickerSample" to { DatePickerSample() },
        "TimePickerSample" to { TimePickerSample() }
    )
```
