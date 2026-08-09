# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Wear OS Compose Material 3 Screenshot Generator.

Captures static screenshots across Wear OS emulators and generates side-by-side composite images.
"""

import os
import subprocess
import time
import utils

# Takes raw PNG screenshots from both emulators sequentially (using exec-out to stream directly
# to Mac disk without saving on watch) and merges them into a side-by-side mockup frame.
def process_screenshot(sample_name: str, out_dir_root: str) -> None:
    paths = utils.build_sample_paths(sample_name, out_dir_root, ext="png")
    os.makedirs(paths.out_dir, exist_ok=True)

    print("    - Taking raw screenshots...")
    utils.take_screenshots(paths)

    print("    - Compositing final image with FFmpeg...")
    utils.generate_image_composite(paths)
    print(f"    ✅ Saved screenshot composite to: {paths.local_composite}")

def main():
    # Parses CLI arguments and sets up the automatic terminal --help documentation
    args = utils.parse_arguments("Wear OS Compose Material 3 Screenshot Generator")

    # Performs prerequisite checks and binds active watch emulators to utils device globals
    utils.setup_environment()

    out_dir_root = os.path.abspath(args.output_dir)
    print(f"📂 Output directory configured to: {out_dir_root}")
    if not os.path.exists(out_dir_root):
        os.makedirs(out_dir_root)

    # Compile and install the latest APK onto BOTH connected emulators simultaneously via Gradle.
    utils.install_media_generator_apk()

    # Force stop the app on both emulators to kill any lingering background threads or memory states from older runs.
    utils.run_command_on_both_emulators(f"am force-stop {utils.APP_PKG}")

    # Clear old journal history so we don't accidentally read stale SAMPLE_READY signals from previous test runs.
    utils.run_cmd(f"adb -s {utils.REGULAR_WATCH.serial} logcat -c")

    # Sleep 2s to allow the watch's internal Linux log daemon (logd) to finish freeing up memory buffers.
    print("    - Waiting for logd to finish clearing buffers...")
    time.sleep(2.0)

    print("📡 Tailing Logcat for SAMPLE_READY signals...")

    # Launch adb logcat as a live background pipe. We only need to listen to REGULAR_WATCH as our master timing
    # conductor because both watches run identical code in parallel lockstep. Listening to one prevents duplicate signals.
    process = subprocess.Popen(["adb", "-s", utils.REGULAR_WATCH.serial, "logcat"], stdout=subprocess.PIPE, text=True, bufsize=1)

    # Launch ScreenshotActivity on both emulators to begin static sample rendering.
    print("🚀 Launching ScreenshotActivity...")
    utils.run_command_on_both_emulators(f"am start -S -n {utils.APP_PKG}/{utils.SCREENSHOT_ACTIVITY}")

    try:
        for line in process.stdout:
            decoded_line = line.strip()
            if "ScreenshotSystem" in decoded_line:
                print(f"[LOGCAT] {decoded_line}")

            # When the watch signals that the UI has settled for 3 seconds, capture photos on both watches.
            if "SAMPLE_READY:" in decoded_line:
                sample_name = decoded_line.split("SAMPLE_READY:")[1].strip()
                print(f"\n[📸] Processing: {sample_name}")

                process_screenshot(sample_name, out_dir_root)

                # Broadcast NEXT_SAMPLE to both watches simultaneously to flip to the next sample without relaunching the app.
                print("    - Advancing to next sample...")
                utils.run_command_on_both_emulators(f"am broadcast -a {utils.INTENT_NEXT_SAMPLE}")
            elif "FINISHED" in decoded_line and "ScreenshotSystem" in decoded_line:
                print("\n✅ Reached end of samples. Exiting.")
                break
    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        process.terminate()


if __name__ == '__main__':
    main()
