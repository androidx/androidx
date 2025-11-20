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

import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.QuirkSettings;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads all video specific quirks required for the current device.
 */
public class DeviceQuirksLoader {

    private DeviceQuirksLoader() {
    }

    /**
     * Goes through all defined video related quirks, and returns those that should be loaded
     * on the current device.
     */
    static @NonNull List<Quirk> loadQuirks(@NonNull QuirkSettings quirkSettings) {
        final List<Quirk> quirks = new ArrayList<>();

        // Load all video specific quirks
        if (quirkSettings.shouldEnableQuirk(
                MediaCodecInfoReportIncorrectInfoQuirk.class,
                MediaCodecInfoReportIncorrectInfoQuirk.load())) {
            quirks.add(new MediaCodecInfoReportIncorrectInfoQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                CameraUseInconsistentTimebaseQuirk.class,
                CameraUseInconsistentTimebaseQuirk.load())) {
            quirks.add(new CameraUseInconsistentTimebaseQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ReportedVideoQualityNotSupportedQuirk.class,
                ReportedVideoQualityNotSupportedQuirk.load())) {
            quirks.add(new ReportedVideoQualityNotSupportedQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                VideoEncoderCrashQuirk.class,
                VideoEncoderCrashQuirk.load())) {
            quirks.add(new VideoEncoderCrashQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExcludeStretchedVideoQualityQuirk.class,
                ExcludeStretchedVideoQualityQuirk.load())) {
            quirks.add(new ExcludeStretchedVideoQualityQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                MediaStoreVideoCannotWrite.class,
                MediaStoreVideoCannotWrite.load())) {
            quirks.add(new MediaStoreVideoCannotWrite());
        }
        if (quirkSettings.shouldEnableQuirk(
                AudioEncoderIgnoresInputTimestampQuirk.class,
                AudioEncoderIgnoresInputTimestampQuirk.load())) {
            quirks.add(new AudioEncoderIgnoresInputTimestampQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk.class,
                VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk.load())) {
            quirks.add(new VideoEncoderSuspendDoesNotIncludeSuspendTimeQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                NegativeLatLongSavesIncorrectlyQuirk.class,
                NegativeLatLongSavesIncorrectlyQuirk.load())) {
            quirks.add(new NegativeLatLongSavesIncorrectlyQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                AudioTimestampFramePositionIncorrectQuirk.class,
                AudioTimestampFramePositionIncorrectQuirk.load())) {
            quirks.add(new AudioTimestampFramePositionIncorrectQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExtraSupportedResolutionQuirk.class,
                ExtraSupportedResolutionQuirk.load())) {
            quirks.add(new ExtraSupportedResolutionQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                StretchedVideoResolutionQuirk.class,
                StretchedVideoResolutionQuirk.load())) {
            quirks.add(new StretchedVideoResolutionQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                CodecStuckOnFlushQuirk.class,
                CodecStuckOnFlushQuirk.load())) {
            quirks.add(new CodecStuckOnFlushQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                StopCodecAfterSurfaceRemovalCrashMediaServerQuirk.class,
                StopCodecAfterSurfaceRemovalCrashMediaServerQuirk.load())) {
            quirks.add(new StopCodecAfterSurfaceRemovalCrashMediaServerQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExtraSupportedQualityQuirk.class,
                ExtraSupportedQualityQuirk.load())) {
            quirks.add(new ExtraSupportedQualityQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                SignalEosOutputBufferNotComeQuirk.class,
                SignalEosOutputBufferNotComeQuirk.load())) {
            quirks.add(new SignalEosOutputBufferNotComeQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                SizeCannotEncodeVideoQuirk.class,
                SizeCannotEncodeVideoQuirk.load())) {
            quirks.add(new SizeCannotEncodeVideoQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewBlackScreenQuirk.class,
                PreviewBlackScreenQuirk.load())) {
            quirks.add(new PreviewBlackScreenQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PrematureEndOfStreamVideoQuirk.class,
                PrematureEndOfStreamVideoQuirk.load())) {
            quirks.add(PrematureEndOfStreamVideoQuirk.INSTANCE);
        }
        if (quirkSettings.shouldEnableQuirk(
                MediaCodecDefaultDataSpaceQuirk.class,
                MediaCodecDefaultDataSpaceQuirk.load())) {
            quirks.add(new MediaCodecDefaultDataSpaceQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                HdrRepeatingRequestFailureQuirk.class,
                HdrRepeatingRequestFailureQuirk.load())) {
            quirks.add(new HdrRepeatingRequestFailureQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewFreezeAfterHighSpeedRecordingQuirk.class,
                PreviewFreezeAfterHighSpeedRecordingQuirk.load())) {
            quirks.add(PreviewFreezeAfterHighSpeedRecordingQuirk.INSTANCE);
        }
        if (quirkSettings.shouldEnableQuirk(
                GLProcessingStuckOnCodecFlushQuirk.class,
                GLProcessingStuckOnCodecFlushQuirk.load())) {
            quirks.add(GLProcessingStuckOnCodecFlushQuirk.INSTANCE);
        }
        if (quirkSettings.shouldEnableQuirk(
                VideoInterlacingQuirk.class,
                VideoInterlacingQuirk.load())) {
            quirks.add(VideoInterlacingQuirk.INSTANCE);
        }
        return quirks;
    }
}
