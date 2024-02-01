/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads all video specific quirks required for the current device.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class DeviceQuirksLoader {

    private DeviceQuirksLoader() {
    }

    /**
     * Goes through all defined video related quirks, and returns those that should be loaded
     * on the current device.
     */
    @NonNull
    static List<Quirk> loadQuirks() {
        final List<Quirk> quirks = new ArrayList<>();

        // Load all video specific quirks
        if (MediaFormatMustNotUseFrameRateToFindEncoderQuirk.load()) {
            quirks.add(new MediaFormatMustNotUseFrameRateToFindEncoderQuirk());
        }
        if (MediaCodecInfoReportIncorrectInfoQuirk.load()) {
            quirks.add(new MediaCodecInfoReportIncorrectInfoQuirk());
        }
        if (DeactivateEncoderSurfaceBeforeStopEncoderQuirk.load()) {
            quirks.add(new DeactivateEncoderSurfaceBeforeStopEncoderQuirk());
        }
        if (CameraUseInconsistentTimebaseQuirk.load()) {
            quirks.add(new CameraUseInconsistentTimebaseQuirk());
        }
        if (ReportedVideoQualityNotSupportedQuirk.load()) {
            quirks.add(new ReportedVideoQualityNotSupportedQuirk());
        }
        if (EncoderNotUsePersistentInputSurfaceQuirk.load()) {
            quirks.add(new EncoderNotUsePersistentInputSurfaceQuirk());
        }
        if (VideoEncoderCrashQuirk.load()) {
            quirks.add(new VideoEncoderCrashQuirk());
        }
        if (ExcludeStretchedVideoQualityQuirk.load()) {
            quirks.add(new ExcludeStretchedVideoQualityQuirk());
        }
        if (MediaStoreVideoCannotWrite.load()) {
            quirks.add(new MediaStoreVideoCannotWrite());
        }
        if (AudioEncoderIgnoresInputTimestampQuirk.load()) {
            quirks.add(new AudioEncoderIgnoresInputTimestampQuirk());
        }
        if (VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk.load()) {
            quirks.add(new VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk());
        }
        if (NegativeLatLongSavesIncorrectlyQuirk.load()) {
            quirks.add(new NegativeLatLongSavesIncorrectlyQuirk());
        }
        if (PreviewStretchWhenVideoCaptureIsBoundQuirk.load()) {
            quirks.add(new PreviewStretchWhenVideoCaptureIsBoundQuirk());
        }
        if (PreviewDelayWhenVideoCaptureIsBoundQuirk.load()) {
            quirks.add(new PreviewDelayWhenVideoCaptureIsBoundQuirk());
        }
        if (AudioTimestampFramePositionIncorrectQuirk.load()) {
            quirks.add(new AudioTimestampFramePositionIncorrectQuirk());
        }
        if (ImageCaptureFailedWhenVideoCaptureIsBoundQuirk.load()) {
            quirks.add(new ImageCaptureFailedWhenVideoCaptureIsBoundQuirk());
        }
        if (ExtraSupportedResolutionQuirk.load()) {
            quirks.add(new ExtraSupportedResolutionQuirk());
        }
        if (StretchedVideoResolutionQuirk.load()) {
            quirks.add(new StretchedVideoResolutionQuirk());
        }
        if (CodecStuckOnFlushQuirk.load()) {
            quirks.add(new CodecStuckOnFlushQuirk());
        }
        if (StopCodecAfterSurfaceRemovalCrashMediaServerQuirk.load()) {
            quirks.add(new StopCodecAfterSurfaceRemovalCrashMediaServerQuirk());
        }
        if (ExtraSupportedQualityQuirk.load()) {
            quirks.add(new ExtraSupportedQualityQuirk());
        }
        if (SignalEosOutputBufferNotComeQuirk.load()) {
            quirks.add(new SignalEosOutputBufferNotComeQuirk());
        }

        return quirks;
    }
}
