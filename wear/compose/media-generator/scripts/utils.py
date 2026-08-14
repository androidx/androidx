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

from __future__ import annotations

import argparse
from contextlib import contextmanager
from dataclasses import dataclass
import os
import shlex
import subprocess
import sys
import time
from typing import Optional, Tuple

# Third-party dependencies with graceful fallback check
try:
    import cv2
    import numpy as np
except ImportError:
    print("❌ Required Python packages 'opencv-python' or 'numpy' are missing.")
    print("Please run: pip install opencv-python numpy")
    sys.exit(1)


# ==============================================================================
# SECTION 1: Constants, Configuration & Data Classes
# ==============================================================================

# Shared Package & Activity Constants
APP_PKG = "androidx.wear.compose.integration.media"
VIDEO_ACTIVITY = ".VideoActivity"
SCREENSHOT_ACTIVITY = ".ScreenshotActivity"

# Shared Intent Actions
INTENT_RESTART_ANIMATION = "androidx.wear.compose.integration.media.RESTART_ANIMATION"
INTENT_PERFORM_FORWARD_FLICK = "androidx.wear.compose.integration.media.PERFORM_FORWARD_FLICK"
INTENT_NEXT_SAMPLE = "androidx.wear.compose.integration.media.NEXT_SAMPLE"

# System Settling & Timing Constants
SCREENRECORD_FLUSH_DELAY = 3.5       # Delay allowing toybox killall to flush MP4 moov atom headers
APK_INSTALL_SETTLE_TIME = 3.0        # Time allowed for emulators to settle after Gradle installDebug
RECORDER_INIT_DELAY = 2.0            # Pause between starting screenrecord and triggering gestures
SAMPLE_LAUNCH_SETTLING_TIME = 4.0   # Time allowed for app cold-launch splash screen to settle
POST_ANIMATION_SETTLING_TIME = 3.0   # Pause after animation completes before stopping recording

# FFmpeg Compositing Freeze-Frame Constant
COMPOSITE_END_FREEZE_PAUSE = 3.0     # Tail freeze-frame duration (seconds) appended by FFmpeg tpad

# Paths & Repositories
REPO_ROOT_RELATIVE_PATH = "../../../../"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MOCKUP_IMG = os.path.join(SCRIPT_DIR, "template", "regular_and_large_watch_frames.png")

# Module-level device variables initialized during setup_environment()
REGULAR_WATCH: Optional[Device] = None
LARGE_WATCH: Optional[Device] = None


@dataclass
class Device:
    serial: str
    w: int
    h: int

@dataclass
class SamplePaths:
    out_dir: str
    raw_reg: str
    raw_large: str
    local_reg: str
    local_large: str
    local_composite: str

# ==============================================================================
# SECTION 2: Environment & Device Management
# ==============================================================================


def configure_process_limits():
    """
    Increases the open file descriptor limit (RLIMIT_NOFILE) to the system maximum.

    Why this is needed:
    macOS has a very low default soft limit (often 256). During long batch runs
    across all samples, many ADB commands and FFmpeg processes open and close.
    Because the OS takes time to clean up closed file descriptors, raising this
    limit prevents 'Too many open files' crashes during batch runs.
    """
    try:
        import resource
        soft, hard = resource.getrlimit(resource.RLIMIT_NOFILE)
        if soft < hard:
            resource.setrlimit(resource.RLIMIT_NOFILE, (hard, hard))
    except Exception:
        pass


def reset_adb_daemon():
    """Restarts the background ADB server daemon to clear hung socket connections or zombie processes."""
    try:
        subprocess.run(["adb", "kill-server"], capture_output=True, text=True)
        # Give the newly spawned ADB server daemon 5s to re-discover and handshake with running emulators
        time.sleep(5)
    except Exception:
        pass


def sync_system_clocks(epoch_ms: int = 1769337015000) -> None:
    """
    Sets the system clock on both emulators to the exact same starting UTC timestamp
    (Jan 25, 2026 10:30:15 UTC) so TimeText and pickers show matching times across recordings.

    Note: Time is not frozen; both watch clocks continue ticking normally from this synchronized start time.
    """
    print(f"⏰ Synchronizing system clocks in lockstep to epoch timestamp {epoch_ms} (Jan 25, 2026 10:30:15 UTC)...")
    # Prevent Android network time sync from overriding our custom timestamp
    run_command_on_both_emulators("settings put global auto_time 0")
    run_command_on_both_emulators("settings put global auto_time_zone 0")
    # Set both emulator clocks to the same starting time simultaneously via alarm manager
    run_adb_shell_clock_synced(f"cmd alarm set-time {epoch_ms}")


def setup_environment() -> Tuple[Device, Device]:
    """
    Verifies system prerequisites (like ffmpeg) and discovers connected watch emulators (454x454 & 480x480),
    populating module-level device globals REGULAR_WATCH and LARGE_WATCH.
    """
    # Configure file descriptor process limits to prevent socket exhaustion during long batch runs
    configure_process_limits()

    print("=" * 50)
    print("Wear OS Animated Media Generator")
    print("=" * 50)
    print("Checking prerequisites...")
    try:
        run_cmd("ffmpeg -version", silent=True)
        print("✅ ffmpeg is installed.")
    except Exception:
        print("❌ ffmpeg is not installed. Please install it first.")
        sys.exit(1)

    def discover_devices() -> Tuple[Optional[Device], Optional[Device]]:
        res = subprocess.run(["adb", "devices"], capture_output=True, text=True)
        lines = res.stdout.strip().split('\n')[1:]
        serials = [parts[0] for line in lines if len(parts := line.split('\t')) == 2 and parts[1] == "device"]

        def get_resolution(serial):
            p = subprocess.run(f"adb -s {serial} shell wm size", shell=True, capture_output=True, text=True)
            out = p.stdout.strip()
            if "Physical size:" in out:
                return int(out.split(" ")[-1].split("x")[0])
            return 0

        d1, d2 = None, None
        for s in serials:
            r = get_resolution(s)
            if r == 454:
                d1 = Device(s, 454, 454)
            elif r == 480:
                d2 = Device(s, 480, 480)
        return d1, d2

    # Try discovering devices on the current running ADB server first
    d1, d2 = discover_devices()

    # Fallback: If devices were not detected, reset ADB daemon once and retry discovery
    if not d1 or not d2:
        print("    - Emulators not detected, resetting ADB daemon fallback...")
        reset_adb_daemon()
        d1, d2 = discover_devices()

    if not d1 or not d2:
        print("❌ ERROR: Could not find both a REGULAR_WATCH (454x454) and LARGE_WATCH (480x480).")
        print("Please start Pixel_4_Regular and Pixel_4_Large from Android Studio.")
        sys.exit(1)

    global REGULAR_WATCH, LARGE_WATCH
    REGULAR_WATCH = d1
    LARGE_WATCH = d2

    print(f"✅ REGULAR_WATCH (454) mapped to {REGULAR_WATCH.serial}, LARGE_WATCH (480) mapped to {LARGE_WATCH.serial}.")
    print("=" * 50 + "\n")
    sync_system_clocks()
    return REGULAR_WATCH, LARGE_WATCH


def install_media_generator_apk():
    """Navigates to repository root, compiles, and installs the media-generator APK on connected emulators."""
    print("⚙️  Building and installing media-generator APK (this may take a moment)...")
    repo_root = os.path.abspath(os.path.join(SCRIPT_DIR, REPO_ROOT_RELATIVE_PATH))
    try:
        run_cmd(f"cd {shlex.quote(repo_root)} && ALLOW_PUBLIC_REPOS=true ./gradlew :wear:compose:media-generator:installDebug")
        print(f"    - Waiting {int(APK_INSTALL_SETTLE_TIME)}s for emulators to settle after APK installation...")
        time.sleep(APK_INSTALL_SETTLE_TIME)
    except subprocess.CalledProcessError as e:
        err_msg = ((e.stderr or "") + (e.stdout or "")).lower()
        # Detect expired credentials or authentication issues with the build cache
        if any(keyword in err_msg for keyword in ["gcp credential", "credential", "gcloud", "unauthorized", "auth failed"]):
            print("\n❌ Build failed: Authentication or credentials issue detected for the AndroidX build cache.")
            print("👉 Please check/renew your GCP credentials and try again.\n")
        raise


def reset_device_state(paths: SamplePaths):
    """
    Resets emulator state before recording a sample by force-stopping the app
    to clear lingering process state and deleting old temporary MP4s from /sdcard/.
    """
    print("    - Cleaning up existing apps and videos...")
    run_command_on_both_emulators(f"am force-stop {APP_PKG}")
    run_command_on_both_emulators(f"rm -f {paths.raw_reg}", f"rm -f {paths.raw_large}")


def launch_sample(sample_name: str, activity: str = VIDEO_ACTIVITY):
    """
    Cold-launches the target sample Activity on both emulators (force-stopping the app process first)
    and waits for the splash screen and initial UI layout to settle.
    """
    print(f"    - Launching Sample App with {sample_name}...")
    run_command_on_both_emulators(f"am start -S -n {APP_PKG}/{activity} -e sample_name {sample_name}")
    print(f"    - Waiting {int(SAMPLE_LAUNCH_SETTLING_TIME)}s for sample to stabilize (splash screen gone)...")
    time.sleep(SAMPLE_LAUNCH_SETTLING_TIME)


def parse_arguments(description: str) -> argparse.Namespace:
    """
    Parses CLI flags (e.g. --output_dir, --sample) and configures the automatic terminal --help menu.
    Returns an argparse.Namespace object containing the parsed options.
    """
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument(
        "--output_dir",
        type=str,
        required=True,
        help="Destination directory path where generated media will be saved (e.g. ~/Documents/Videos or /Users/<username>/Videos).",
    )
    parser.add_argument(
        "--sample",
        type=str,
        default=None,
        help="Optional specific sample name to generate instead of running the full suite.",
    )
    return parser.parse_args()


# ==============================================================================
# SECTION 3: ADB Command Dispatchers & Synchronized Touch Helpers
# ==============================================================================

# --- Low-level ADB Command Dispatchers & Clock Synchronization ---

def run_cmd(cmd: str, silent: bool = True):
    """
    Executes a shell command synchronously and blocks until completion.
    Includes automatic ADB daemon recovery: if an ADB command drops connection mid-run,
    it restarts the daemon, waits for port 5037 to free up, and retries the command up to 3 times.
    """
    if not silent:
        print(f"Running: {cmd}")

    for attempt in range(1, 4):
        try:
            subprocess.run(cmd, shell=True, check=True, capture_output=True, text=True)
            return
        except subprocess.CalledProcessError as e:
            err_msg = (e.stderr or "") + (e.stdout or "")
            is_adb_daemon_error = any(
                keyword in err_msg for keyword in [
                    "cannot connect to daemon",
                    "daemon not running",
                    "device offline",
                    "device not found",
                    "Address already in use",
                    "ADB server didn't ACK",
                    "failed to start daemon",
                    "could not install *smartsocket* listener",
                ]
            )

            # Automatically recover ADB daemon and retry command if an ADB socket connection drops mid-run
            if "adb" in cmd and is_adb_daemon_error and attempt < 3:
                print(f"      ⚠️  ADB daemon connection dropped (attempt {attempt}/3). Resetting ADB server...")
                reset_adb_daemon()
                print(f"      🔄 Retrying ADB command (attempt {attempt + 1}/3)...")
                continue

            if not silent:
                print(f"Command failed: {cmd}\nStderr: {e.stderr}")
            raise


def run_cmd_async(cmd: str, silent: bool = True) -> subprocess.Popen:
    """Spawns a shell command asynchronously in the background (non-blocking) and returns its process handle."""
    return subprocess.Popen(cmd, shell=True, stdout=subprocess.DEVNULL if silent else None, stderr=subprocess.DEVNULL if silent else None)


def run_command_on_both_emulators(cmd1: str, cmd2: Optional[str] = None) -> None:
    """Executes an ADB shell command serially on both connected watch emulators."""
    cmd2 = cmd2 or cmd1
    run_cmd(f"adb -s {REGULAR_WATCH.serial} shell \"{cmd1}\"")
    run_cmd(f"adb -s {LARGE_WATCH.serial} shell \"{cmd2}\"")


def run_adb_shell_clock_synced(cmd1: str, cmd2: Optional[str] = None, delay_ms: int = 1000) -> None:
    """
    Executes ADB shell commands on both watches at the exact same physical millisecond.

    How lockstep synchronization works:
    1. Reads current kernel uptime (/proc/uptime) from both watches in parallel.
    2. Calculates a future target uptime for each watch (current uptime + 1000ms delay buffer).
    3. Sends a shell command to each watch telling it to wait locally until its target uptime hits.

    Because both watches pause locally before running, the 1-second delay buffer absorbs
    ADB transmission lag, ensuring both commands fire at the exact same physical moment.
    """
    cmd2 = cmd2 or cmd1

    for attempt in range(1, 4):
        try:
            # 1. Read current kernel uptime on both watches simultaneously
            p_reg = subprocess.Popen(["adb", "-s", REGULAR_WATCH.serial, "shell", "cat /proc/uptime"], stdout=subprocess.PIPE, text=True)
            p_large = subprocess.Popen(["adb", "-s", LARGE_WATCH.serial, "shell", "cat /proc/uptime"], stdout=subprocess.PIPE, text=True)

            out_reg = p_reg.communicate()[0].strip()
            out_large = p_large.communicate()[0].strip()

            if not out_reg or not out_large:
                raise ValueError("Empty uptime output from ADB")

            u1_ms = int(float(out_reg.split()[0]) * 1000)
            u2_ms = int(float(out_large.split()[0]) * 1000)

            # 2. Calculate future target uptime for each watch (delay buffer)
            target1_ms = u1_ms + delay_ms
            target2_ms = u2_ms + delay_ms

            # 3. Helper script that pauses locally on the watch until target_u_ms is reached
            def create_synced_script(target_u_ms: int, cmd: str) -> str:
                return (
                    f"curr=$(awk '{{print int($1 * 1000)}}' /proc/uptime); "
                    f"rem=$(( {target_u_ms} - curr )); "
                    f"[ $rem -gt 0 ] && sleep $(printf '%d.%03d' $((rem / 1000)) $((rem % 1000))); "
                    f"{cmd}"
                )

            # 4. Dispatch wait-and-execute scripts to both watches in parallel
            p1 = subprocess.Popen(["adb", "-s", REGULAR_WATCH.serial, "shell", create_synced_script(target1_ms, cmd1)])
            p2 = subprocess.Popen(["adb", "-s", LARGE_WATCH.serial, "shell", create_synced_script(target2_ms, cmd2)])
            p1.wait()
            p2.wait()
            return

        except Exception as e:
            # If an ADB connection drops while querying uptimes or dispatching scripts, reset daemon and retry
            if attempt < 3:
                print(f"      ⚠️  Synced ADB command dropped (attempt {attempt}/3: {e}). Resetting ADB server...")
                reset_adb_daemon()
                time.sleep(1.0)
                print(f"      🔄 Retrying ADB synced command (attempt {attempt + 1}/3)...")
                continue
            raise


# --- High-level Synchronized Touch Gesture Helpers ---

def format_adb_swipe_cmd(w: int, h: int, pct_x1: float, pct_y1: float, pct_x2: float, pct_y2: float, duration_ms: int) -> str:
    """Formats an ADB 'input swipe' shell command string scaled to device resolution percentages."""
    x1, y1 = int(w * pct_x1), int(h * pct_y1)
    x2, y2 = int(w * pct_x2), int(h * pct_y2)
    return f"input swipe {x1} {y1} {x2} {y2} {duration_ms}"


def perform_synced_swipe(sx_pct: float, sy_pct: float, ex_pct: float, ey_pct: float, duration_ms: int):
    """Executes a touch swipe on both watch emulators at the exact same millisecond."""
    run_adb_shell_clock_synced(
        format_adb_swipe_cmd(REGULAR_WATCH.w, REGULAR_WATCH.h, sx_pct, sy_pct, ex_pct, ey_pct, duration_ms),
        format_adb_swipe_cmd(LARGE_WATCH.w, LARGE_WATCH.h, sx_pct, sy_pct, ex_pct, ey_pct, duration_ms)
    )


def perform_synced_tap(pct_x: float, pct_y: float, hold_ms: int = 300):
    """Executes a touch tap on both watch emulators at the exact same millisecond."""
    perform_synced_swipe(pct_x, pct_y, pct_x, pct_y, hold_ms)


def perform_synced_scroll_down(times: int, sy_pct: float, pause: float):
    """Executes consecutive downward scroll swipes on both watches with inter-swipe pauses."""
    for i in range(times):
        perform_synced_swipe(0.5, sy_pct, 0.5, 0.2, 800)
        is_last = (i == times - 1)
        time.sleep((pause + 0.5) if is_last else pause)


# ==============================================================================
# SECTION 4: Media Recording & Compositing
# ==============================================================================

def build_sample_paths(sample_name: str, out_dir_root: str, ext: str = "mp4") -> SamplePaths:
    """Constructs local host destination paths and remote emulator /sdcard/ file paths for a sample."""
    out_dir = os.path.join(out_dir_root, sample_name)
    return SamplePaths(
        out_dir=out_dir,
        raw_reg=f"/sdcard/raw_regular_{sample_name}.{ext}",
        raw_large=f"/sdcard/raw_large_{sample_name}.{ext}",
        local_reg=os.path.join(out_dir, f"WearComposeM3_{sample_name}_Regular.{ext}"),
        local_large=os.path.join(out_dir, f"WearComposeM3_{sample_name}_Large.{ext}"),
        local_composite=os.path.join(out_dir, f"WearComposeM3_{sample_name}_CompositeImage.{ext}"),
    )

@contextmanager
def record_screens(paths: SamplePaths):
    """
    Context manager that launches background 'adb shell screenrecord' processes on both watches,
    waits for gesture execution, and sends SIGINT (-2) to cleanly flush MP4 headers upon exit.
    """
    print("    - Starting background screen recording...")
    p1 = run_cmd_async(f"adb -s {shlex.quote(REGULAR_WATCH.serial)} shell screenrecord --size {REGULAR_WATCH.w}x{REGULAR_WATCH.h} --time-limit 180 {shlex.quote(paths.raw_reg)}")
    p2 = run_cmd_async(f"adb -s {shlex.quote(LARGE_WATCH.serial)} shell screenrecord --size {LARGE_WATCH.w}x{LARGE_WATCH.h} --time-limit 180 {shlex.quote(paths.raw_large)}")

    time.sleep(RECORDER_INIT_DELAY)
    try:
        yield
        """
        Pause to allow UI animation to fully settle.
        Note: This pause will NOT extend the resting freeze-frame in the raw MP4 video because once the
        screen becomes 100% static, Android's display compositor stops emitting new frame buffers to screenrecord.
        This is why FFmpeg's `tpad` filter is required in generate_video_composite() to manually clone the resting freeze-frame.
        """
        time.sleep(POST_ANIMATION_SETTLING_TIME)
    finally:
        print("    - Stopping screen recording...")
        # Send SIGINT (-2) via toybox killall to cleanly flush MP4 moov atom header on device
        run_command_on_both_emulators("killall -2 screenrecord || true")
        time.sleep(SCREENRECORD_FLUSH_DELAY)
        # Ensure host ADB subprocesses are reaped cleanly without hanging
        for p in (p1, p2):
            try:
                if p:
                    p.wait(timeout=5.0)
            except subprocess.TimeoutExpired:
                p.kill()

def get_true_start_time(video_path: str) -> float:
    """
    Analyzes an autoplay or OHG video recording using OpenCV and returns the exact start time
    of the first sample UI frame after the 100ms black screen reset.

    How it works:
    1. Reads each frame along with its true display timestamp from the MP4 recording.
    2. Finds all black screen reset frames (mean pixel brightness < 0.01).
    3. Takes the LAST black frame in the 100ms reset window.
    4. Returns the timestamp 2 frames after the last black frame (last_black_idx + 2) to skip past the transition frame.
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"      ⚠️  Could not open {video_path} for frame analysis.")
        return 0.0

    try:
        fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
        max_frames = int(fps * 5)  # Analyze only the first 5 seconds of footage
        frames = []
        timestamps = []

        # Read initial frames and their exact display timestamps from MP4 header
        while len(frames) < max_frames:
            ts = cap.get(cv2.CAP_PROP_POS_MSEC) / 1000.0
            ret, frame = cap.read()
            if not ret:
                break
            frames.append(frame)
            timestamps.append(ts)

        if not frames:
            return 0.0

        # Step 1: Find all pure black reset frames (mean brightness < 0.01)
        black_indices = [i for i, f in enumerate(frames) if np.mean(f) < 0.01]
        if not black_indices:
            return 0.0

        # Step 2: Locate the last black frame of the 100ms reset window
        last_black_idx = black_indices[-1]

        # Step 3: Select frame (last_black_idx + 2) to completely skip the fade-in/transition frame
        first_ui_idx = last_black_idx + 2
        if first_ui_idx < len(timestamps):
            return timestamps[first_ui_idx]
        elif (last_black_idx + 1) < len(timestamps):
            return timestamps[last_black_idx + 1]
        return 0.0
    finally:
        cap.release()


def pull_videos_from_emulators(paths: SamplePaths):
    """Pulls raw recorded MP4 videos from emulator /sdcard/ storage to local host paths and deletes remote files."""
    print(f"    - Pulling videos to {paths.out_dir}...")
    run_cmd(f"adb -s {REGULAR_WATCH.serial} pull {shlex.quote(paths.raw_reg)} {shlex.quote(paths.local_reg)}")
    run_cmd(f"adb -s {LARGE_WATCH.serial} pull {shlex.quote(paths.raw_large)} {shlex.quote(paths.local_large)}")

    # Clean up device storage
    run_cmd(f"adb -s {REGULAR_WATCH.serial} shell rm -f {shlex.quote(paths.raw_reg)}")
    run_cmd(f"adb -s {LARGE_WATCH.serial} shell rm -f {shlex.quote(paths.raw_large)}")


def take_screenshots(paths: SamplePaths):
    """Captures static PNG screenshots from both watch emulators using 'adb exec-out screencap'."""
    run_cmd(f'adb -s {REGULAR_WATCH.serial} exec-out screencap -p > "{paths.local_reg}"')
    run_cmd(f'adb -s {LARGE_WATCH.serial} exec-out screencap -p > "{paths.local_large}"')


def generate_video_composite(paths: SamplePaths, trim_reg: float = 0.0, trim_large: float = 0.0):
    """
    Renders side-by-side composite MP4 video using FFmpeg.
    Pre-filters VFR inputs to 60fps CFR to eliminate timestamp jitter, trims pre-roll footage,
    appends a few-second tail freeze-frame (tpad),
    and overlays both watch feeds under the Pixel 4 mockup frame.
    """
    print("    - Compositing final video with FFmpeg...")
    trim_filter = ""
    v0_label = "0:v"
    v1_label = "1:v"

    # Pre-filter VFR inputs to 60fps CFR to eliminate overlay timestamp jitter at t=0
    if trim_reg > 0 or trim_large > 0:
        trim_filter = (
            f"[{v0_label}]fps=fps=60,trim=start={trim_reg},setpts=PTS-STARTPTS[vt0];"
            f"[{v1_label}]fps=fps=60,trim=start={trim_large},setpts=PTS-STARTPTS[vt1];"
        )
        v0_label = "vt0"
        v1_label = "vt1"
    else:
        trim_filter = (
            f"[{v0_label}]fps=fps=60[vt0];"
            f"[{v1_label}]fps=fps=60[vt1];"
        )
        v0_label = "vt0"
        v1_label = "vt1"

    composite_cmd = f'''ffmpeg -y \
      -i "{paths.local_reg}" \
      -i "{paths.local_large}" \
      -f image2 -loop 1 -i "{MOCKUP_IMG}" \
      -filter_complex \
      "{trim_filter} \
       [{v0_label}]scale=434:434:flags=lanczos,tpad=stop_mode=clone:stop_duration={COMPOSITE_END_FREEZE_PAUSE}[vstd]; \
       [{v1_label}]scale=484:484:flags=lanczos,tpad=stop_mode=clone:stop_duration={COMPOSITE_END_FREEZE_PAUSE}[vlarge]; \
       color=s=2048x720:c=0xFDFDFD:r=60[bg]; \
       [bg][vstd]overlay=391:143:shortest=1[bg2]; \
       [bg2][vlarge]overlay=1170:118:shortest=1[bg3]; \
       [bg3][2:v]overlay=0:0:shortest=1[out]" \
      -map "[out]" \
      -c:v libx264 -crf 22 -pix_fmt yuv420p \
      "{paths.local_composite}"'''
    run_cmd(composite_cmd)


def generate_image_composite(paths: SamplePaths):
    """Renders side-by-side composite static PNG screenshot using FFmpeg under the Pixel 4 mockup frame."""
    print("    - Compositing final image with FFmpeg...")
    composite_cmd = f'''ffmpeg -y \
      -i "{paths.local_reg}" \
      -i "{paths.local_large}" \
      -i "{MOCKUP_IMG}" \
      -filter_complex \
      "[0:v]scale=434:434:flags=lanczos[vstd]; \
       [1:v]scale=484:484:flags=lanczos[vlarge]; \
       color=s=2048x720:c=0xFDFDFD[bg]; \
       [bg][vstd]overlay=391:143[bg2]; \
       [bg2][vlarge]overlay=1170:118[bg3]; \
       [bg3][2:v]overlay=0:0[out]" \
      -map "[out]" \
      -frames:v 1 \
      "{paths.local_composite}"'''
    run_cmd(composite_cmd)


def render_final_composite_video(sample_name: str, paths: SamplePaths, triggers_on_load: bool):
    """Orchestrates OpenCV pre-roll start time detection (for autoplay/OHG) and FFmpeg video compositing."""
    trim_reg, trim_large = 0.0, 0.0

    if triggers_on_load:
        print("    - Analyzing frames to find exact start of animation...")
        trim_reg = get_true_start_time(paths.local_reg)
        trim_large = get_true_start_time(paths.local_large)

    generate_video_composite(paths, trim_reg=trim_reg, trim_large=trim_large)
    print(f"    ✅ Done! Saved to: {paths.local_composite}")
