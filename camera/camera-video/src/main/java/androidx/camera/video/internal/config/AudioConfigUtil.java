/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.video.internal.config;

import android.util.Range;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.MediaSpec;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.camera.video.internal.audio.AudioSource;
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.core.util.Supplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A collection of utilities used for resolving and debugging audio configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioConfigUtil {
    private static final String TAG = "AudioConfigUtil";

    // Default to 44100 for now as it's guaranteed supported on devices.
    static final int AUDIO_SAMPLE_RATE_DEFAULT = 44100;
    // Default to mono since that should be supported on the most devices.
    static final int AUDIO_CHANNEL_COUNT_DEFAULT = AudioSpec.CHANNEL_COUNT_MONO;
    // Defaults to PCM_16BIT as it's guaranteed supported on devices.
    static final int AUDIO_SOURCE_FORMAT_DEFAULT = AudioSpec.SOURCE_FORMAT_PCM_16BIT;
    // Defaults to Camcorder as this should be the source closest to the camera
    static final int AUDIO_SOURCE_DEFAULT = AudioSpec.SOURCE_CAMCORDER;

    // Should not be instantiated.
    private AudioConfigUtil() {
    }

    /**
     * Resolves the audio mime information into a {@link AudioMimeInfo}.
     *
     * @param mediaSpec        the media spec to resolve the mime info.
     * @param encoderProfiles  the encoder profiles to resolve the mime info. It can be null if
     *                         there is no relevant encoder profiles.
     * @return the audio MimeInfo.
     */
    @NonNull
    public static AudioMimeInfo resolveAudioMimeInfo(@NonNull MediaSpec mediaSpec,
            @Nullable VideoValidatedEncoderProfilesProxy encoderProfiles) {
        String mediaSpecAudioMime = MediaSpec.outputFormatToAudioMime(mediaSpec.getOutputFormat());
        int mediaSpecAudioProfile =
                MediaSpec.outputFormatToAudioProfile(mediaSpec.getOutputFormat());
        String resolvedAudioMime = mediaSpecAudioMime;
        int resolvedAudioProfile = mediaSpecAudioProfile;
        AudioProfileProxy compatibleAudioProfile = null;
        if (encoderProfiles != null && encoderProfiles.getDefaultAudioProfile() != null) {
            AudioProfileProxy audioProfile = encoderProfiles.getDefaultAudioProfile();
            String encoderProfileAudioMime = audioProfile.getMediaType();
            int encoderProfileAudioProfile = audioProfile.getProfile();

            if (Objects.equals(encoderProfileAudioMime, AudioProfileProxy.MEDIA_TYPE_NONE)) {
                Logger.d(TAG, "EncoderProfiles contains undefined AUDIO mime type so cannot be "
                        + "used. May rely on fallback defaults to derive settings [chosen mime "
                        + "type: "
                        + resolvedAudioMime + "(profile: " + resolvedAudioProfile + ")]");
            } else if (mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO) {
                compatibleAudioProfile = audioProfile;
                resolvedAudioMime = encoderProfileAudioMime;
                resolvedAudioProfile = encoderProfileAudioProfile;
                Logger.d(TAG, "MediaSpec contains OUTPUT_FORMAT_AUTO. Using EncoderProfiles "
                        + "to derive AUDIO settings [mime type: "
                        + resolvedAudioMime + "(profile: " + resolvedAudioProfile + ")]");
            } else if (Objects.equals(mediaSpecAudioMime, encoderProfileAudioMime)
                    && mediaSpecAudioProfile == encoderProfileAudioProfile) {
                compatibleAudioProfile = audioProfile;
                resolvedAudioMime = encoderProfileAudioMime;
                Logger.d(TAG, "MediaSpec audio mime/profile matches EncoderProfiles. "
                        + "Using EncoderProfiles to derive AUDIO settings [mime type: "
                        + resolvedAudioMime + "(profile: " + resolvedAudioProfile + ")]");
            } else {
                Logger.d(TAG, "MediaSpec audio mime or profile does not match EncoderProfiles, so "
                        + "EncoderProfiles settings cannot be used. May rely on fallback defaults"
                        + " to derive AUDIO settings [EncoderProfiles mime type: "
                        + encoderProfileAudioMime + "(profile: " + encoderProfileAudioProfile
                        + "), chosen mime type: "
                        + resolvedAudioMime + "(profile: " + resolvedAudioProfile + ")]");
            }
        }

        AudioMimeInfo.Builder mimeInfoBuilder = AudioMimeInfo.builder(resolvedAudioMime)
                .setProfile(resolvedAudioProfile);
        if (compatibleAudioProfile != null) {
            mimeInfoBuilder.setCompatibleAudioProfile(compatibleAudioProfile);
        }

        return mimeInfoBuilder.build();
    }

    /**
     * Resolves the audio source settings into an {@link AudioSettings}.
     *
     * @param audioMimeInfo the audio mime info.
     * @param audioSpec     the audio spec.
     * @return an AudioSettings.
     */
    @NonNull
    public static AudioSettings resolveAudioSettings(@NonNull AudioMimeInfo audioMimeInfo,
            @NonNull AudioSpec audioSpec) {
        Supplier<AudioSettings> settingsSupplier;
        AudioProfileProxy compatibleAudioProfile = audioMimeInfo.getCompatibleAudioProfile();
        if (compatibleAudioProfile != null) {
            settingsSupplier = new AudioSettingsAudioProfileResolver(audioSpec,
                    compatibleAudioProfile);
        } else {
            settingsSupplier = new AudioSettingsDefaultResolver(audioSpec);
        }

        return settingsSupplier.get();
    }

    /**
     * Resolves video related information into a {@link AudioEncoderConfig}.
     *
     * @param audioMimeInfo       the audio mime info.
     * @param inputTimebase       the timebase of the input frame.
     * @param audioSettings       the audio settings.
     * @param audioSpec           the audio spec.
     * @return a AudioEncoderConfig.
     */
    @NonNull
    public static AudioEncoderConfig resolveAudioEncoderConfig(@NonNull AudioMimeInfo audioMimeInfo,
            @NonNull Timebase inputTimebase, @NonNull AudioSettings audioSettings,
            @NonNull AudioSpec audioSpec) {
        Supplier<AudioEncoderConfig> configSupplier;
        AudioProfileProxy compatibleAudioProfile = audioMimeInfo.getCompatibleAudioProfile();
        if (compatibleAudioProfile != null) {
            configSupplier = new AudioEncoderConfigAudioProfileResolver(
                    audioMimeInfo.getMimeType(), audioMimeInfo.getProfile(), inputTimebase,
                    audioSpec, audioSettings, compatibleAudioProfile);
        } else {
            configSupplier = new AudioEncoderConfigDefaultResolver(audioMimeInfo.getMimeType(),
                    audioMimeInfo.getProfile(), inputTimebase, audioSpec, audioSettings);
        }

        return configSupplier.get();
    }

    static int resolveAudioSource(@NonNull AudioSpec audioSpec) {
        int resolvedAudioSource = audioSpec.getSource();
        if (resolvedAudioSource == AudioSpec.SOURCE_AUTO) {
            resolvedAudioSource = AUDIO_SOURCE_DEFAULT;
            Logger.d(TAG, "Using default AUDIO source: " + resolvedAudioSource);
        } else {
            Logger.d(TAG, "Using provided AUDIO source: " + resolvedAudioSource);
        }

        return resolvedAudioSource;
    }

    static int resolveAudioSourceFormat(@NonNull AudioSpec audioSpec) {
        int resolvedAudioSourceFormat = audioSpec.getSourceFormat();
        if (resolvedAudioSourceFormat == AudioSpec.SOURCE_FORMAT_AUTO) {
            // TODO: This should come from a priority list and may need to be combined with
            //  AudioSource.isSettingsSupported.
            resolvedAudioSourceFormat = AUDIO_SOURCE_FORMAT_DEFAULT;
            Logger.d(TAG, "Using default AUDIO source format: " + resolvedAudioSourceFormat);
        } else {
            Logger.d(
                    TAG, "Using provided AUDIO source format: " + resolvedAudioSourceFormat);
        }

        return resolvedAudioSourceFormat;
    }

    static int selectSampleRateOrNearestSupported(@NonNull Range<Integer> targetRange,
            int channelCount, int sourceFormat, int initialTargetSampleRate) {
        int selectedSampleRate = initialTargetSampleRate;
        // Sample rates sorted by proximity to initial target.
        List<Integer> sortedCommonSampleRates = null;
        int i = 0;
        do {
            if (targetRange.contains(selectedSampleRate)) {
                if (AudioSource.isSettingsSupported(selectedSampleRate, channelCount,
                        sourceFormat)) {
                    return selectedSampleRate;
                } else {
                    Logger.d(TAG, "Sample rate " + selectedSampleRate + "Hz is not supported by "
                            + "audio source with channel count " + channelCount + " and source "
                            + "format " + sourceFormat);
                }
            } else {
                Logger.d(TAG, "Sample rate " + selectedSampleRate + "Hz is not in target range "
                        + targetRange);
            }

            // If the initial target isn't supported, sort the array of published common sample
            // rates by closeness to target  and step through until we've found one that is
            // supported.
            if (sortedCommonSampleRates == null) {
                Logger.d(TAG,
                        "Trying common sample rates in proximity order to target "
                                + initialTargetSampleRate + "Hz");
                sortedCommonSampleRates = new ArrayList<>(AudioSettings.COMMON_SAMPLE_RATES);
                Collections.sort(sortedCommonSampleRates, (x, y) -> {
                    int relativeDifference = Math.abs(x - initialTargetSampleRate) - Math.abs(
                            y - initialTargetSampleRate);
                    // If the relative difference is zero, i.e., the target is halfway
                    // between the two, always prefer the larger sample rate for quality.
                    if (relativeDifference == 0) {
                        return (int) Math.signum(x - y);
                    }

                    return (int) Math.signum(relativeDifference);
                });
            }

            if (i < sortedCommonSampleRates.size()) {
                selectedSampleRate = sortedCommonSampleRates.get(i++);
            } else {
                break;
            }
        } while (true);

        // No supported sample rate found. The default sample rate should work on most devices. May
        // consider throw an exception or have other way to notify users that the specified
        // sample rate can not be satisfied.
        Logger.d(TAG, "No sample rate found in target range or supported by audio source. Falling"
                + " back to default sample rate of " + AUDIO_SAMPLE_RATE_DEFAULT + "Hz");
        return AUDIO_SAMPLE_RATE_DEFAULT;
    }

    static int scaleAndClampBitrate(int baseBitrate,
            int actualChannelCount, int baseChannelCount,
            int actualSampleRate, int baseSampleRate,
            Range<Integer> clampedRange) {
        // Scale bitrate based on source number of channels relative to base channel count.
        Rational channelCountRatio = new Rational(actualChannelCount, baseChannelCount);
        // Scale bitrate based on source sample rate relative to profile sample rate.
        Rational sampleRateRatio = new Rational(actualSampleRate, baseSampleRate);

        int resolvedBitrate = (int) (baseBitrate * channelCountRatio.doubleValue()
                * sampleRateRatio.doubleValue());

        String debugString = "";
        if (Logger.isDebugEnabled(TAG)) {
            debugString = String.format("Base Bitrate(%dbps) * Channel Count Ratio(%d / %d) * "
                            + "Sample Rate Ratio(%d / %d) = %d", baseBitrate, actualChannelCount,
                    baseChannelCount, actualSampleRate, baseSampleRate, resolvedBitrate);
        }

        if (!AudioSpec.BITRATE_RANGE_AUTO.equals(clampedRange)) {
            resolvedBitrate = clampedRange.clamp(resolvedBitrate);
            if (Logger.isDebugEnabled(TAG)) {
                debugString += String.format("\nClamped to range %s -> %dbps", clampedRange,
                        resolvedBitrate);
            }
        }
        Logger.d(TAG, debugString);
        return resolvedBitrate;
    }
}
