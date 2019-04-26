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

package androidx.media2.player;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.media.AudioTrack;
import android.os.Build;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Structure for common playback params.
 *
 * Used by {@link MediaPlayer} {@link MediaPlayer#getPlaybackParams()} and
 * {@link MediaPlayer#setPlaybackParams(PlaybackParams)}
 * to control playback behavior.
 * <p>
 * PlaybackParams returned by {@link MediaPlayer#getPlaybackParams()} will always have values.
 * In case of {@link MediaPlayer#setPlaybackParams}, the player will not update the param if the
 * value is not set. For example, if pitch is set while speed is not set, only pitch will be
 * updated.
 * <p>
 * Note that the speed value does not change the player state. For example, if
 * {@link MediaPlayer#getPlaybackParams()} is called with the speed of 2.0f in
 * {@link MediaPlayer#PLAYER_STATE_PAUSED}, the player will just update internal property and stay
 * paused. Once {@link MediaPlayer#play()} is called afterwards, the player will start
 * playback with the given speed. Calling this with zero speed is not allowed.
 * <p>
 * <strong>audio fallback mode:</strong> select out-of-range parameter handling.
 * <ul>
 * <li> {@link PlaybackParams#AUDIO_FALLBACK_MODE_DEFAULT}:
 *   System will determine best handling. </li>
 * <li> {@link PlaybackParams#AUDIO_FALLBACK_MODE_MUTE}:
 *   Play silence for params normally out of range.</li>
 * <li> {@link PlaybackParams#AUDIO_FALLBACK_MODE_FAIL}:
 *   Return {@link java.lang.IllegalArgumentException} from
 *   <code>AudioTrack.setPlaybackParams(PlaybackParams)</code>.</li>
 * </ul>
 * <p> <strong>pitch:</strong> increases or decreases the tonal frequency of the audio content.
 * It is expressed as a multiplicative factor, where normal pitch is 1.0f.
 * <p> <strong>speed:</strong> increases or decreases the time to
 * play back a set of audio or video frames.
 * It is expressed as a multiplicative factor, where normal speed is 1.0f.
 * <p> Different combinations of speed and pitch may be used for audio playback;
 * some common ones:
 * <ul>
 * <li> <em>Pitch equals 1.0f.</em> Speed change will be done with pitch preserved,
 * often called <em>timestretching</em>.</li>
 * <li> <em>Pitch equals speed.</em> Speed change will be done by <em>resampling</em>,
 * similar to {@link AudioTrack#setPlaybackRate(int)}.</li>
 * </ul>
 */
public final class PlaybackParams {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                    AUDIO_FALLBACK_MODE_DEFAULT,
                    AUDIO_FALLBACK_MODE_MUTE,
                    AUDIO_FALLBACK_MODE_FAIL,
            }
    )
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioFallbackMode {}
    public static final int AUDIO_FALLBACK_MODE_DEFAULT = 0;
    public static final int AUDIO_FALLBACK_MODE_MUTE = 1;
    public static final int AUDIO_FALLBACK_MODE_FAIL = 2;

    // params
    private Integer mAudioFallbackMode;
    private Float mPitch;
    private Float mSpeed;
    private android.media.PlaybackParams mPlaybackParams;

    PlaybackParams(Integer audioFallbackMode, Float pitch, Float speed) {
        mAudioFallbackMode = audioFallbackMode;
        mPitch = pitch;
        mSpeed = speed;
    }

    @RequiresApi(23)
    PlaybackParams(android.media.PlaybackParams playbackParams) {
        mPlaybackParams = playbackParams;
    }

    /**
     * Returns the audio fallback mode. {@code null} if a value is not set.
     */
    public @AudioFallbackMode @Nullable Integer getAudioFallbackMode() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                return mPlaybackParams.getAudioFallbackMode();
            } catch (IllegalStateException e) {
                return null;
            }
        } else {
            return mAudioFallbackMode;
        }
    }

    /**
     * Returns the pitch factor. {@code null} if a value is not set.
     */
    public @Nullable Float getPitch() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                return mPlaybackParams.getPitch();
            } catch (IllegalStateException e) {
                return null;
            }
        } else {
            return mPitch;
        }
    }

    /**
     * Returns the speed factor. {@code null} if a value is not set.
     */
    public @Nullable Float getSpeed() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                return mPlaybackParams.getSpeed();
            } catch (IllegalStateException e) {
                return null;
            }
        } else {
            return mSpeed;
        }
    }

    /**
     * Returns the underlying framework {@link android.media.PlaybackParams} object. {@code null}
     * if it is not available.
     * <p>
     * This method is only supported on {@link android.os.Build.VERSION_CODES#M} and later.
     * </p>
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @RequiresApi(23)
    public android.media.PlaybackParams getPlaybackParams() {
        if (Build.VERSION.SDK_INT >= 23) {
            return mPlaybackParams;
        } else {
            return null;
        }
    }

    /**
     * The builder class that makes it easy to chain setters to create a {@link PlaybackParams}
     * object.
     */
    public static final class Builder {
        private Integer mAudioFallbackMode;
        private Float mPitch;
        private Float mSpeed;
        private android.media.PlaybackParams mPlaybackParams;

        /**
         * Default constructor
         */
        public Builder() {
            if (Build.VERSION.SDK_INT >= 23) {
                mPlaybackParams = new android.media.PlaybackParams();
            }
        }

        /** @hide */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @RequiresApi(23)
        public Builder(android.media.PlaybackParams playbackParams) {
            mPlaybackParams = playbackParams;
        }

        /**
         * Constructs a new PlaybackParams builder using data from {@code playbackParams}.
         *
         * @param playbackParams the non-null instance to initialize from.
         */
        public Builder(@NonNull PlaybackParams playbackParams) {
            if (playbackParams == null) {
                throw new NullPointerException("playbakcParams shouldn't be null");
            }
            if (Build.VERSION.SDK_INT >= 23) {
                mPlaybackParams = playbackParams.getPlaybackParams();
            } else {
                mAudioFallbackMode = playbackParams.getAudioFallbackMode();
                mPitch = playbackParams.getPitch();
                mSpeed = playbackParams.getSpeed();
            }
        }

        /**
         * Sets the audio fallback mode.
         *
         * @return this <code>Builder</code> instance.
         */
        public @NonNull Builder setAudioFallbackMode(@AudioFallbackMode int audioFallbackMode) {
            if (Build.VERSION.SDK_INT >= 23) {
                mPlaybackParams.setAudioFallbackMode(audioFallbackMode);
            } else {
                mAudioFallbackMode = audioFallbackMode;
            }
            return this;
        }

        /**
         * Sets the pitch factor.
         *
         * @return this <code>Builder</code> instance.
         * @throws IllegalArgumentException if the pitch is negative.
         */
        public @NonNull Builder setPitch(
                @FloatRange(from = 0.0f, to = Float.MAX_VALUE) float pitch) {
            if (pitch < 0.f) {
                throw new IllegalArgumentException("pitch must not be negative");
            }
            if (Build.VERSION.SDK_INT >= 23) {
                mPlaybackParams.setPitch(pitch);
            } else {
                mPitch = pitch;
            }
            return this;
        }

        /**
         * Sets the speed factor.
         *
         * @return this <code>Builder</code> instance.
         */
        public @NonNull Builder setSpeed(
                @FloatRange(from = 0.0f, to = Float.MAX_VALUE, fromInclusive = false) float speed) {
            if (speed == 0.f) {
                throw new IllegalArgumentException("0 speed is not allowed");
            }
            if (speed < 0.f) {
                throw new IllegalArgumentException("negative speed is not supported");
            }
            if (Build.VERSION.SDK_INT >= 23) {
                mPlaybackParams.setSpeed(speed);
            } else {
                mSpeed = speed;
            }
            return this;
        }

        /**
         * Takes the values of the Builder object and creates a PlaybackParams object.
         *
         * @return PlaybackParams object with values from the Builder.
         */
        public @NonNull PlaybackParams build() {
            if (Build.VERSION.SDK_INT >= 23) {
                return new PlaybackParams(mPlaybackParams);
            } else {
                return new PlaybackParams(mAudioFallbackMode, mPitch, mSpeed);
            }
        }
    }
}
