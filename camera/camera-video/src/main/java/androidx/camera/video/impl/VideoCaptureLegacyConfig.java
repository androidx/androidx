/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.video.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.ThreadConfig;
import androidx.camera.video.VideoCaptureLegacy;

/**
 * Config for a video capture use case.
 *
 * <p>In the earlier stage, the VideoCaptureLegacy is deprioritized.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoCaptureLegacyConfig
        implements UseCaseConfig<VideoCaptureLegacy>,
        ImageOutputConfig,
        ThreadConfig {

    // Option Declarations:
    // *********************************************************************************************

    public static final Option<Integer> OPTION_VIDEO_FRAME_RATE =
            Option.create("camerax.video.videoCaptureLegacy.recordingFrameRate", int.class);
    public static final Option<Integer> OPTION_BIT_RATE =
            Option.create("camerax.video.videoCaptureLegacy.bitRate", int.class);
    public static final Option<Integer> OPTION_INTRA_FRAME_INTERVAL =
            Option.create("camerax.video.videoCaptureLegacy.intraFrameInterval", int.class);
    public static final Option<Integer> OPTION_AUDIO_BIT_RATE =
            Option.create("camerax.video.videoCaptureLegacy.audioBitRate", int.class);
    public static final Option<Integer> OPTION_AUDIO_SAMPLE_RATE =
            Option.create("camerax.video.videoCaptureLegacy.audioSampleRate", int.class);
    public static final Option<Integer> OPTION_AUDIO_CHANNEL_COUNT =
            Option.create("camerax.video.videoCaptureLegacy.audioChannelCount", int.class);
    public static final Option<Integer> OPTION_AUDIO_RECORD_SOURCE =
            Option.create("camerax.video.videoCaptureLegacy.audioRecordSource", int.class);
    public static final Option<Integer> OPTION_AUDIO_MIN_BUFFER_SIZE =
            Option.create("camerax.video.videoCaptureLegacy.audioMinBufferSize", int.class);

    // *********************************************************************************************

    private final OptionsBundle mConfig;

    public VideoCaptureLegacyConfig(@NonNull OptionsBundle config) {
        mConfig = config;
    }

    /**
     * Returns the recording frames per second.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getVideoFrameRate(int valueIfMissing) {
        return retrieveOption(OPTION_VIDEO_FRAME_RATE, valueIfMissing);
    }

    /**
     * Returns the recording frames per second.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getVideoFrameRate() {
        return retrieveOption(OPTION_VIDEO_FRAME_RATE);
    }

    /**
     * Returns the encoding bit rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getBitRate(int valueIfMissing) {
        return retrieveOption(OPTION_BIT_RATE, valueIfMissing);
    }

    /**
     * Returns the encoding bit rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getBitRate() {
        return retrieveOption(OPTION_BIT_RATE);
    }

    /**
     * Returns the number of seconds between each key frame.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getIFrameInterval(int valueIfMissing) {
        return retrieveOption(OPTION_INTRA_FRAME_INTERVAL, valueIfMissing);
    }

    /**
     * Returns the number of seconds between each key frame.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getIFrameInterval() {
        return retrieveOption(OPTION_INTRA_FRAME_INTERVAL);
    }

    /**
     * Returns the audio encoding bit rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getAudioBitRate(int valueIfMissing) {
        return retrieveOption(OPTION_AUDIO_BIT_RATE, valueIfMissing);
    }

    /**
     * Returns the audio encoding bit rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getAudioBitRate() {
        return retrieveOption(OPTION_AUDIO_BIT_RATE);
    }

    /**
     * Returns the audio sample rate.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getAudioSampleRate(int valueIfMissing) {
        return retrieveOption(OPTION_AUDIO_SAMPLE_RATE, valueIfMissing);
    }

    /**
     * Returns the audio sample rate.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getAudioSampleRate() {
        return retrieveOption(OPTION_AUDIO_SAMPLE_RATE);
    }

    /**
     * Returns the audio channel count.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getAudioChannelCount(int valueIfMissing) {
        return retrieveOption(OPTION_AUDIO_CHANNEL_COUNT, valueIfMissing);
    }

    /**
     * Returns the audio channel count.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getAudioChannelCount() {
        return retrieveOption(OPTION_AUDIO_CHANNEL_COUNT);
    }

    /**
     * Returns the audio recording source.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getAudioRecordSource(int valueIfMissing) {
        return retrieveOption(OPTION_AUDIO_RECORD_SOURCE, valueIfMissing);
    }

    /**
     * Returns the audio recording source.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getAudioRecordSource() {
        return retrieveOption(OPTION_AUDIO_RECORD_SOURCE);
    }

    /**
     * Returns the audio minimum buffer size, in bytes.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    public int getAudioMinBufferSize(int valueIfMissing) {
        return retrieveOption(OPTION_AUDIO_MIN_BUFFER_SIZE, valueIfMissing);
    }

    /**
     * Returns the audio minimum buffer size, in bytes.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    public int getAudioMinBufferSize() {
        return retrieveOption(OPTION_AUDIO_MIN_BUFFER_SIZE);
    }

    /**
     * Retrieves the format of the image that is fed as input.
     *
     * <p>This should always be PRIVATE for VideoCaptureLegacy.
     */
    @Override
    public int getInputFormat() {
        return ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
    }

    @NonNull
    @Override
    public Config getConfig() {
        return mConfig;
    }
}
