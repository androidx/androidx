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
Wear OS Compose Material 3 Video Generator.

Records animation samples on Wear OS emulators and renders side-by-side mockup videos.
"""

import os
import sys
import gestures
import utils


# 6-stage automated pipeline: paths, launch, record, choreography, pull, composite.
def generate_sample_video(sample_name: str, out_dir_root: str) -> None:
    print(f"\n[▶] Generating videos for: {sample_name}")

    # Stage 1: Build local and emulator file paths
    paths = utils.build_sample_paths(sample_name, out_dir_root)
    os.makedirs(paths.out_dir, exist_ok=True)

    # Stage 2: Reset emulator state and launch target sample
    utils.reset_device_state(paths)
    utils.launch_sample(sample_name)

    handler = gestures.get_media_capture_gesture_handler(sample_name)

    # Stage 3 & 4: Record screens while triggering gestures choreography or autoplay animation
    with utils.record_screens(paths):
        print(f"    - {handler.description}")
        handler.trigger_animation(sample_name)

    # Stage 5: Transfer recorded MP4s from emulators to host
    utils.pull_videos_from_emulators(paths)

    # Stage 6: Analyze pre-roll and render final side-by-side mockup video
    utils.render_final_composite_video(sample_name, paths, handler.triggers_on_load)


# Master entry point: verifies emulators, installs media-generator APK, and loops across samples.
def main() -> None:
    """
    Main entry point: verifies connected watch emulators, installs the
    media-generator APK, and processes all samples sequentially.
    """
    # Parses CLI arguments and sets up the automatic terminal --help documentation
    args = utils.parse_arguments("Wear OS Compose Material 3 Video Generator")

    # Performs prerequisite checks and binds active watch emulators to utils device globals
    utils.setup_environment()

    out_dir_root = os.path.abspath(args.output_dir)
    print(f"📂 Output directory configured to: {out_dir_root}")

    # Build and install media-generator APK on both emulators
    utils.install_media_generator_apk()

    if args.sample:
        if args.sample not in gestures.SAMPLE_GESTURES_MAP:
            print(f"❌ ERROR: Sample '{args.sample}' is not registered in gestures.py.")
            sys.exit(1)
        target_samples = [args.sample]
    else:
        target_samples = gestures.SAMPLE_GESTURES_MAP.keys()

    # Sequential execution loop across target samples
    for sample_name in target_samples:
        generate_sample_video(sample_name, out_dir_root)


if __name__ == '__main__':
    main()
