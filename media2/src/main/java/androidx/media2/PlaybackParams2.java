/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.os.Build;

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
 * Used by {@link XMediaPlayer} {@link XMediaPlayer#getPlaybackParams()} and
 * {@link XMediaPlayer#setPlaybackParams(PlaybackParams2)}
 * to control playback behavior.
 * <p> <strong>audio fallback mode:</strong>
 * select out-of-range parameter handling.
 * <ul>
 * <li> {@link PlaybackParams2#AUDIO_FALLBACK_MODE_DEFAULT}:
 *   System will determine best handling. </li>
 * <li> {@link PlaybackParams2#AUDIO_FALLBACK_MODE_MUTE}:
 *   Play silence for params normally out of range.</li>
 * <li> {@link PlaybackParams2#AUDIO_FALLBACK_MODE_FAIL}:
 *   Return {@link java.lang.IllegalArgumentException} from
 *   <code>AudioTrack.setPlaybackParams(PlaybackParams2)</code>.</li>
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
public final class PlaybackParams2 {
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
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
    private PlaybackParams mPlaybackParams;

    PlaybackParams2(Integer audioFallbackMode, Float pitch, Float speed) {
        mAudioFallbackMode = audioFallbackMode;
        mPitch = pitch;
        mSpeed = speed;
    }

    @RequiresApi(Build.VERSION_CODES.M)
    PlaybackParams2(PlaybackParams playbackParams) {
        mPlaybackParams = playbackParams;
    }

    /**
     * Returns the audio fallback mode. {@code null} if a value is not set.
     */
    public @AudioFallbackMode @Nullable Integer getAudioFallbackMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
     * Returns the underlying framework {@link PlaybackParams} object. {@code null} if it is not
     * available.
     * <p>
     * This method is only supported on {@link android.os.Build.VERSION_CODES#M} and later.
     * </p>
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @RequiresApi(Build.VERSION_CODES.M)
    public PlaybackParams getPlaybackParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mPlaybackParams;
        } else {
            return null;
        }
    }

    /**
     * The builder class that makes it easy to chain setters to create a {@link PlaybackParams2}
     * object.
     */
    public static final class Builder {
        private Integer mAudioFallbackMode;
        private Float mPitch;
        private Float mSpeed;
        private PlaybackParams mPlaybackParams;

        public Builder() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPlaybackParams = new PlaybackParams();
            }
        }

        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
        @RequiresApi(Build.VERSION_CODES.M)
        public Builder(PlaybackParams playbackParams) {
            mPlaybackParams = playbackParams;
        }

        /**
         * Sets the audio fallback mode.
         *
         * @return this <code>Builder</code> instance.
         */
        public @NonNull Builder setAudioFallbackMode(@AudioFallbackMode int audioFallbackMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        public @NonNull Builder setPitch(float pitch) {
            if (pitch < 0.f) {
                throw new IllegalArgumentException("pitch must not be negative");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        public @NonNull Builder setSpeed(float speed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPlaybackParams.setSpeed(speed);
            } else {
                mSpeed = speed;
            }
            return this;
        }

        /**
         * Takes the values of the Builder object and creates a PlaybackParams2 object.
         * @return PlaybackParams2 object with values from the Builder.
         */
        public @NonNull PlaybackParams2 build() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new PlaybackParams2(mPlaybackParams);
            } else {
                return new PlaybackParams2(mAudioFallbackMode, mPitch, mSpeed);
            }
        }
    }
}
