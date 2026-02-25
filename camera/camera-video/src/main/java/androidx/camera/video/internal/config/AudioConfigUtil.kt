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
package androidx.camera.video.internal.config

import android.media.MediaCodecInfo
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_VORBIS
import android.util.Rational
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.video.AudioSpec
import androidx.camera.video.MediaSpec
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import androidx.camera.video.MediaSpec.OutputFormat
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import androidx.camera.video.internal.audio.AudioSettings
import androidx.camera.video.internal.audio.AudioSource
import androidx.camera.video.internal.encoder.AudioEncoderConfig
import androidx.camera.video.internal.encoder.EncoderConfig
import kotlin.math.abs
import kotlin.math.sign

/** A collection of utilities used for resolving and debugging audio configurations. */
public object AudioConfigUtil {
    private const val TAG = "AudioConfigUtil"

    // Default to 44100 for now as it's guaranteed supported on devices.
    public const val AUDIO_SAMPLE_RATE_DEFAULT: Int = 44100

    // Default to mono since that should be supported on the most devices.
    public const val AUDIO_CHANNEL_COUNT_DEFAULT: Int = AudioSpec.CHANNEL_COUNT_MONO

    // Defaults to PCM_16BIT as it's guaranteed supported on devices.
    public const val AUDIO_SOURCE_FORMAT_DEFAULT: Int = AudioSpec.SOURCE_FORMAT_PCM_16BIT

    // Defaults to Camcorder as this should be the source closest to the camera
    public const val AUDIO_SOURCE_DEFAULT: Int = AudioSpec.SOURCE_CAMCORDER

    private const val AAC_DEFAULT_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC

    private const val AUDIO_ENCODER_MIME_MPEG4_DEFAULT = MIMETYPE_AUDIO_AAC

    private const val AUDIO_ENCODER_MIME_WEBM_DEFAULT = MIMETYPE_AUDIO_VORBIS

    /**
     * Resolves a compatible [AudioProfileProxy] from a list based on the provided MIME type.
     *
     * This method attempts to find the first profile in the provided list that matches the
     * requested [audioMime] and its corresponding codec profile. If the [audioMime] is set to
     * [AudioSpec.MIME_TYPE_UNSPECIFIED], it will return the first available profile in the list.
     *
     * @param audioMime The desired audio MIME type.
     * @param audioProfiles A list of available [AudioProfileProxy]s.
     * @return The first matching [AudioProfileProxy], or `null` if no compatible profile is found.
     */
    public fun resolveCompatibleAudioProfile(
        audioMime: String,
        audioProfiles: List<AudioProfileProxy>,
    ): AudioProfileProxy? {
        val audioCodecProfile = audioMimeToAudioProfile(audioMime)
        return audioProfiles.firstOrNull {
            audioMime == AudioSpec.MIME_TYPE_UNSPECIFIED ||
                (it.mediaType == audioMime && it.profile == audioCodecProfile)
        }
    }

    /**
     * Maps a given [OutputFormat] to its default audio MIME type.
     *
     * @param outputFormat The video recording output format.
     * @return The default audio MIME type string associated with the output format.
     */
    public fun outputFormatToAudioMime(@OutputFormat outputFormat: Int): String {
        return when (outputFormat) {
            OUTPUT_FORMAT_WEBM -> AUDIO_ENCODER_MIME_WEBM_DEFAULT
            else -> AUDIO_ENCODER_MIME_MPEG4_DEFAULT
        }
    }

    /**
     * Maps an audio MIME type to its corresponding standard [MediaCodecInfo.CodecProfileLevel].
     *
     * @param audioMime The audio MIME type.
     * @return The codec profile.
     */
    public fun audioMimeToAudioProfile(audioMime: String): Int =
        when (audioMime) {
            MIMETYPE_AUDIO_AAC -> AAC_DEFAULT_PROFILE
            else -> EncoderConfig.CODEC_PROFILE_NONE
        }

    /**
     * Resolves the audio mime information into a [AudioMimeInfo].
     *
     * @param mediaSpec the media spec to resolve the mime info.
     * @param encoderProfiles the encoder profiles to resolve the mime info. It can be null if there
     *   is no relevant encoder profiles.
     * @return the audio MimeInfo.
     */
    @JvmStatic
    public fun resolveAudioMimeInfo(
        mediaSpec: MediaSpec,
        encoderProfiles: VideoValidatedEncoderProfilesProxy?,
    ): AudioMimeInfo {
        val mediaSpecAudioMime = MediaSpec.outputFormatToAudioMime(mediaSpec.outputFormat)
        val mediaSpecAudioProfile = MediaSpec.outputFormatToAudioProfile(mediaSpec.outputFormat)
        var resolvedAudioMime = mediaSpecAudioMime
        var resolvedAudioProfile = mediaSpecAudioProfile
        var compatibleAudioProfile: AudioProfileProxy? = null
        encoderProfiles?.defaultAudioProfile?.let { audioProfile ->
            val encoderProfileAudioMime = audioProfile.mediaType
            val encoderProfileAudioProfile = audioProfile.profile
            if (encoderProfileAudioMime == AudioProfileProxy.MEDIA_TYPE_NONE) {
                Logger.d(
                    TAG,
                    "EncoderProfiles contains undefined AUDIO mime type so cannot be " +
                        "used. May rely on fallback defaults to derive settings [chosen mime " +
                        "type: $resolvedAudioMime(profile: $resolvedAudioProfile)]",
                )
            } else if (mediaSpec.outputFormat == MediaSpec.OUTPUT_FORMAT_UNSPECIFIED) {
                compatibleAudioProfile = audioProfile
                resolvedAudioMime = encoderProfileAudioMime
                resolvedAudioProfile = encoderProfileAudioProfile
                Logger.d(
                    TAG,
                    "MediaSpec contains OUTPUT_FORMAT_UNSPECIFIED. Using EncoderProfiles " +
                        "to derive AUDIO settings [mime type: $resolvedAudioMime(profile: " +
                        "$resolvedAudioProfile)]",
                )
            } else if (
                mediaSpecAudioMime == encoderProfileAudioMime &&
                    mediaSpecAudioProfile == encoderProfileAudioProfile
            ) {
                compatibleAudioProfile = audioProfile
                resolvedAudioMime = encoderProfileAudioMime
                Logger.d(
                    TAG,
                    "MediaSpec audio mime/profile matches EncoderProfiles. " +
                        "Using EncoderProfiles to derive AUDIO settings [mime type: " +
                        "$resolvedAudioMime(profile: $resolvedAudioProfile)]",
                )
            } else {
                Logger.d(
                    TAG,
                    "MediaSpec audio mime or profile does not match EncoderProfiles, so " +
                        "EncoderProfiles settings cannot be used. May rely on fallback defaults" +
                        " to derive AUDIO settings [EncoderProfiles mime type: " +
                        "$encoderProfileAudioMime(profile: $encoderProfileAudioProfile), " +
                        "chosen mime type: $resolvedAudioMime(profile: $resolvedAudioProfile)]",
                )
            }
        }
        return AudioMimeInfo(
            mimeType = resolvedAudioMime,
            profile = resolvedAudioProfile,
            compatibleAudioProfile = compatibleAudioProfile,
        )
    }

    /**
     * Resolves the audio source settings into an [AudioSettings].
     *
     * @param audioMimeInfo the audio mime info.
     * @param audioSpec the audio spec.
     * @param captureToEncodeRatio the capture to encode sample rate ratio.
     * @return an AudioSettings.
     */
    @JvmStatic
    public fun resolveAudioSettings(
        audioMimeInfo: AudioMimeInfo,
        audioSpec: AudioSpec,
        captureToEncodeRatio: Rational?,
    ): AudioSettings {
        val settingsSupplier =
            if (audioMimeInfo.compatibleAudioProfile != null) {
                AudioSettingsAudioProfileResolver(
                    audioSpec = audioSpec,
                    audioProfile = audioMimeInfo.compatibleAudioProfile,
                    captureToEncodeRatio = captureToEncodeRatio,
                )
            } else {
                AudioSettingsDefaultResolver(
                    audioSpec = audioSpec,
                    captureToEncodeRatio = captureToEncodeRatio,
                )
            }
        return settingsSupplier.get()
    }

    /**
     * Resolves video related information into a [AudioEncoderConfig].
     *
     * @param audioMimeInfo the audio mime info.
     * @param inputTimebase the timebase of the input frame.
     * @param audioSettings the audio settings.
     * @param audioSpec the audio spec.
     * @return a AudioEncoderConfig.
     */
    @JvmStatic
    public fun resolveAudioEncoderConfig(
        audioMimeInfo: AudioMimeInfo,
        inputTimebase: Timebase,
        audioSettings: AudioSettings,
        audioSpec: AudioSpec,
    ): AudioEncoderConfig {
        val configSupplier =
            if (audioMimeInfo.compatibleAudioProfile != null) {
                AudioEncoderConfigAudioProfileResolver(
                    mimeType = audioMimeInfo.mimeType,
                    audioProfile = audioMimeInfo.profile,
                    inputTimebase = inputTimebase,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                    audioProfileProxy = audioMimeInfo.compatibleAudioProfile,
                )
            } else {
                AudioEncoderConfigDefaultResolver(
                    mimeType = audioMimeInfo.mimeType,
                    audioProfile = audioMimeInfo.profile,
                    inputTimeBase = inputTimebase,
                    audioSpec = audioSpec,
                    audioSettings = audioSettings,
                )
            }
        return configSupplier.get()
    }

    public fun resolveAudioSource(audioSpec: AudioSpec): Int {
        var resolvedAudioSource = audioSpec.source
        if (resolvedAudioSource == AudioSpec.SOURCE_UNSPECIFIED) {
            resolvedAudioSource = AUDIO_SOURCE_DEFAULT
            Logger.d(TAG, "Using default AUDIO source: $resolvedAudioSource")
        } else {
            Logger.d(TAG, "Using provided AUDIO source: $resolvedAudioSource")
        }
        return resolvedAudioSource
    }

    public fun resolveAudioSourceFormat(audioSpec: AudioSpec): Int {
        var resolvedAudioSourceFormat = audioSpec.sourceFormat
        if (resolvedAudioSourceFormat == AudioSpec.SOURCE_FORMAT_UNSPECIFIED) {
            // TODO: This should come from a priority list and may need to be combined with
            //  AudioSource.isSettingsSupported.
            resolvedAudioSourceFormat = AUDIO_SOURCE_FORMAT_DEFAULT
            Logger.d(TAG, "Using default AUDIO source format: $resolvedAudioSourceFormat")
        } else {
            Logger.d(TAG, "Using provided AUDIO source format: $resolvedAudioSourceFormat")
        }
        return resolvedAudioSourceFormat
    }

    public fun selectSampleRateOrNearestSupported(
        channelCount: Int,
        sourceFormat: Int,
        initialTargetSampleRate: Int,
    ): Int {
        var selectedSampleRate = initialTargetSampleRate
        // Sample rates sorted by proximity to initial target.
        var sortedCommonSampleRates: List<Int>? = null
        var i = 0
        do {
            if (AudioSource.isSettingsSupported(selectedSampleRate, channelCount, sourceFormat)) {
                return selectedSampleRate
            } else {
                Logger.d(
                    TAG,
                    "Sample rate $selectedSampleRate Hz is not supported by " +
                        "audio source with channel count $channelCount and source " +
                        "format $sourceFormat",
                )
            }

            // If the initial target isn't supported, sort the array of published common sample
            // rates by closeness to target  and step through until we've found one that is
            // supported.
            if (sortedCommonSampleRates == null) {
                Logger.d(
                    TAG,
                    "Trying common sample rates in proximity order to target " +
                        "$initialTargetSampleRate Hz",
                )
                sortedCommonSampleRates = ArrayList(AudioSettings.COMMON_SAMPLE_RATES)
                // If the relative difference is zero, i.e., the target is halfway
                // between the two, always prefer the larger sample rate for quality.
                sortedCommonSampleRates.sortWith { x: Int, y: Int ->
                    val relativeDifference =
                        abs(x - initialTargetSampleRate) - abs(y - initialTargetSampleRate)
                    // If the relative difference is zero, i.e., the target is halfway
                    // between the two, always prefer the larger sample rate for quality.
                    if (relativeDifference == 0) {
                        (x - y).sign
                    } else {
                        relativeDifference.sign
                    }
                }
            }
            if (i < sortedCommonSampleRates.size) {
                selectedSampleRate = sortedCommonSampleRates[i++]
            } else {
                break
            }
        } while (true)

        // No supported sample rate found. The default sample rate should work on most devices. May
        // consider throw an exception or have other way to notify users that the specified
        // sample rate can not be satisfied.
        Logger.d(
            TAG,
            "No sample rate found or supported by audio source. Falling" +
                " back to default sample rate of $AUDIO_SAMPLE_RATE_DEFAULT Hz",
        )
        return AUDIO_SAMPLE_RATE_DEFAULT
    }

    public fun scaleBitrate(
        baseBitrate: Int,
        actualChannelCount: Int,
        baseChannelCount: Int,
        actualSampleRate: Int,
        baseSampleRate: Int,
    ): Int {
        // Scale bitrate based on source number of channels relative to base channel count.
        val channelCountRatio = Rational(actualChannelCount, baseChannelCount)
        // Scale bitrate based on source sample rate relative to profile sample rate.
        val sampleRateRatio = Rational(actualSampleRate, baseSampleRate)
        val resolvedBitrate =
            (baseBitrate * channelCountRatio.toDouble() * sampleRateRatio.toDouble()).toInt()
        var debugString = ""
        if (Logger.isDebugEnabled(TAG)) {
            debugString =
                "Base Bitrate(${baseBitrate}bps) * Channel Count Ratio($actualChannelCount / " +
                    "$baseChannelCount) * Sample Rate Ratio($actualSampleRate / $baseSampleRate) " +
                    "= $resolvedBitrate"
        }
        Logger.d(TAG, debugString)
        return resolvedBitrate
    }

    internal fun resolveSampleRates(
        targetEncodeSampleRate: Int,
        channelCount: Int,
        sourceFormat: Int,
        captureToEncodeRatio: Rational?,
    ): CaptureEncodeRates {
        val resolvedCaptureSampleRate: Int
        val resolvedEncodeSampleRate: Int
        if (captureToEncodeRatio == null) {
            resolvedCaptureSampleRate =
                selectSampleRateOrNearestSupported(
                    channelCount,
                    sourceFormat,
                    targetEncodeSampleRate,
                )
            resolvedEncodeSampleRate = resolvedCaptureSampleRate
        } else {
            val scaledInitialTargetEncodeSampleRate =
                toCaptureRate(
                    encodeRate = targetEncodeSampleRate,
                    captureToEncodeRatio = captureToEncodeRatio,
                )
            resolvedCaptureSampleRate =
                selectSampleRateOrNearestSupported(
                    channelCount,
                    sourceFormat,
                    scaledInitialTargetEncodeSampleRate,
                )
            resolvedEncodeSampleRate =
                toEncodeRate(
                    captureRate = resolvedCaptureSampleRate,
                    captureToEncodeRatio = captureToEncodeRatio,
                )
        }
        Logger.d(
            TAG,
            "Resolved capture/encode sample rate ${resolvedCaptureSampleRate}Hz/" +
                "${resolvedEncodeSampleRate}Hz, [target sample rate: $targetEncodeSampleRate, " +
                "channel count: $channelCount, source format: $sourceFormat, " +
                "capture to encode sample rate ratio: $captureToEncodeRatio]",
        )
        return CaptureEncodeRates(resolvedCaptureSampleRate, resolvedEncodeSampleRate)
    }
}
