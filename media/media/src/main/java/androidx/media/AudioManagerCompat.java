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

package androidx.media;

import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat.StreamType;

import org.jspecify.annotations.NonNull;

/**
 * Compatibility library for {@link AudioManager} with fallbacks for older platforms.
 *
 * @deprecated androidx.media is deprecated. Please migrate to <a
 *     href="https://developer.android.com/media/media3">androidx.media3</a>.
 */
@Deprecated
public final class AudioManagerCompat {

    private static final String TAG = "AudioManCompat";

    /**
     * Used to indicate a gain of audio focus, or a request of audio focus, of unknown duration.
     *
     * @see AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     * @see #requestAudioFocus(AudioManager, AudioFocusRequestCompat)
     */
    public static final int AUDIOFOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN;
    /**
     * Used to indicate a temporary gain or request of audio focus, anticipated to last a short
     * amount of time. Examples of temporary changes are the playback of driving directions, or an
     * event notification.
     *
     * @see AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     * @see #requestAudioFocus(AudioManager, AudioFocusRequestCompat)
     */
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
    /**
     * Used to indicate a temporary request of audio focus, anticipated to last a short amount of
     * time, and where it is acceptable for other audio applications to keep playing after having
     * lowered their output level (also referred to as "ducking"). Examples of temporary changes are
     * the playback of driving directions where playback of music in the background is acceptable.
     *
     * @see AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     * @see #requestAudioFocus(AudioManager, AudioFocusRequestCompat)
     */
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK =
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
    /**
     * Used to indicate a temporary request of audio focus, anticipated to last a short amount of
     * time, during which no other applications, or system components, should play anything.
     * Examples of exclusive and transient audio focus requests are voice memo recording and speech
     * recognition, during which the system shouldn't play any notifications, and media playback
     * should have paused.
     *
     * @see #requestAudioFocus(AudioManager, AudioFocusRequestCompat)
     */
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE =
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;

    /**
     * Requests audio focus. See the {@link AudioFocusRequestCompat} for information about the
     * options available to configure your request, and notification of focus gain and loss.
     *
     * @param focusRequest an {@link AudioFocusRequestCompat} instance used to configure how focus
     *     is requested.
     * @return {@link AudioManager#AUDIOFOCUS_REQUEST_FAILED} or {@link
     *     AudioManager#AUDIOFOCUS_REQUEST_GRANTED}.
     * @throws NullPointerException if passed a null argument
     */
    @SuppressWarnings("deprecation")
    public static int requestAudioFocus(
            @NonNull AudioManager audioManager, @NonNull AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Api26Impl.requestAudioFocus(audioManager, focusRequest.getAudioFocusRequest());
        } else {
            return audioManager.requestAudioFocus(
                    focusRequest.getOnAudioFocusChangeListener(),
                    focusRequest.getAudioAttributesCompat().getLegacyStreamType(),
                    focusRequest.getFocusGain());
        }
    }

    /**
     * Abandon audio focus. Causes the previous focus owner, if any, to receive focus.
     *
     * @param focusRequest the {@link AudioFocusRequestCompat} that was used when requesting focus
     *     with {@link #requestAudioFocus(AudioManager, AudioFocusRequestCompat)}.
     * @return {@link AudioManager#AUDIOFOCUS_REQUEST_FAILED} or {@link
     *     AudioManager#AUDIOFOCUS_REQUEST_GRANTED}
     * @throws IllegalArgumentException if passed a null argument
     */
    @SuppressWarnings("deprecation")
    public static int abandonAudioFocusRequest(
            @NonNull AudioManager audioManager, @NonNull AudioFocusRequestCompat focusRequest) {
        if (audioManager == null) {
            throw new IllegalArgumentException("AudioManager must not be null");
        }
        if (focusRequest == null) {
            throw new IllegalArgumentException("AudioFocusRequestCompat must not be null");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Api26Impl.abandonAudioFocusRequest(audioManager,
                    focusRequest.getAudioFocusRequest());
        } else {
            return audioManager.abandonAudioFocus(focusRequest.getOnAudioFocusChangeListener());
        }
    }

    /**
     * Returns the maximum volume index for a particular stream.
     *
     * @param streamType The stream type whose maximum volume index is returned.
     * @return The maximum valid volume index for the stream.
     */
    @IntRange(from = 0)
    public static int getStreamMaxVolume(@NonNull AudioManager audioManager,
            @StreamType int streamType) {
        return audioManager.getStreamMaxVolume(streamType);
    }

    /**
     * Returns the minimum volume index for a particular stream.
     *
     * @param streamType The stream type whose minimum volume index is returned.
     * @return The minimum valid volume index for the stream.
     */
    @IntRange(from = 0)
    public static int getStreamMinVolume(@NonNull AudioManager audioManager,
            @StreamType int streamType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Api28Impl.getStreamMinVolume(audioManager, streamType);
        } else {
            return 0;
        }
    }

    /**
     * Indicates if the device implements a fixed volume policy.
     *
     * <p>Some devices may not have volume control and may operate at a fixed volume, and may not
     * enable muting or changing the volume of audio streams. This method will return {@code true}
     * on such devices.
     *
     * <p>Compatibility: It returns {@code false} on API level below 21 even if the device has
     * fixed volume.
     */
    public static boolean isVolumeFixed(@NonNull AudioManager audioManager) {
        return audioManager.isVolumeFixed();
    }

    private AudioManagerCompat() {}

    @RequiresApi(26)
    private static class Api26Impl {

        static int abandonAudioFocusRequest(AudioManager audioManager,
                AudioFocusRequest focusRequest) {
            return audioManager.abandonAudioFocusRequest(focusRequest);
        }

        static int requestAudioFocus(AudioManager audioManager, AudioFocusRequest focusRequest) {
            return audioManager.requestAudioFocus(focusRequest);
        }

        private Api26Impl() {}
    }

    @RequiresApi(28)
    private static class Api28Impl {

        static int getStreamMinVolume(AudioManager audioManager, int streamType) {
            return audioManager.getStreamMinVolume(streamType);
        }

        private Api28Impl() {}
    }
}
