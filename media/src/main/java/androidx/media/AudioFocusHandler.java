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

import static androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_ERROR;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_IDLE;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_PAUSED;
import static androidx.media.BaseMediaPlayer.PLAYER_STATE_PLAYING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

/**
 * Handles audio focus and noisy intent depending on the {@link AudioAttributesCompat}.
 * <p>
 * This follows our developer's guideline, and does nothing if the audio attribute hasn't set.
 *
 * @see {@docRoot}guide/topics/media-apps/audio-app/mediasession-callbacks.html
 * @see {@docRoot}guide/topics/media-apps/video-app/mediasession-callbacks.html
 * @see {@docRoot}guide/topics/media-apps/audio-focus.html
 * @see {@docRoot}guide/topics/media-apps/volume-and-earphones.html
 * @hide
 */
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
@RestrictTo(Scope.LIBRARY)
public class AudioFocusHandler {
    private static final String TAG = "AudioFocusHandler";
    private static final boolean DEBUG = false;

    interface AudioFocusHandlerImpl {
        boolean onPlayRequested();
        boolean onPauseRequested();
        void onPlayerStateChanged(int playerState);
        void close();
        void sendIntent(Intent intent);
    }

    private final AudioFocusHandlerImpl mImpl;

    AudioFocusHandler(Context context, MediaSession2 session) {
        mImpl = new AudioFocusHandlerImplBase(context, session);
    }

    /**
     * Should be called when the {@link MediaSession2#play()} is called. Returns whether the play()
     * can be proceed.
     * <p>
     * This matches with the Session.Callback#onPlay() written in the guideline.
     *
     * @return {@code true} if we don't need to handle audio focus or audio focus was granted.
     *          {@code false} otherwise (i.e. attempt to request audio focus was failed).
     */
    public boolean onPlayRequested() {
        return mImpl.onPlayRequested();
    }

    /**
     * Should be called when the {@link MediaSession2#pause()} is called. Returns whether the
     * pause() can be proceed.
     * <p>
     * This matches with the Session.Callback#onPlay() written in the guideline.
     *
     * @return {@code true} if we don't need to handle audio focus or audio focus was granted.
     *          {@code false} otherwise (i.e. attempt to request audio focus was failed).
     */
    public boolean onPauseRequested() {
        return mImpl.onPauseRequested();
    }

    /**
     * Should be called when the player state is changed.
     * <p>
     * This is to implement the guideline for media session callback.
     */
    public void onPlayerStateChanged(int playerState) {
        mImpl.onPlayerStateChanged(playerState);
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     */
    public void close() {
        mImpl.close();
    }

    /**
     * Testing purpose.
     *
     * @param intent
     */
    public void sendIntent(Intent intent) {
        mImpl.sendIntent(intent);
    }

    private static class AudioFocusHandlerImplBase implements AudioFocusHandlerImpl {
        // Value is from the {@link AudioFocusRequest} as follows
        // 'A typical attenuation by the “ducked” application is a factor of 0.2f (or -14dB), that
        // can for instance be applied with MediaPlayer.setVolume(0.2f) when using this class for
        // playback.'
        private static final float VOLUME_DUCK_FACTOR = 0.2f;
        private final BroadcastReceiver mBecomingNoisyIntentReceiver = new NoisyIntentReceiver();
        private final IntentFilter mIntentFilter =
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        private final OnAudioFocusChangeListener mAudioFocusListener = new AudioFocusListener();
        private final Object mLock = new Object();
        private final Context mContext;
        private final MediaSession2 mSession;
        private final AudioManager mAudioManager;

        @GuardedBy("mLock")
        private AudioAttributesCompat mAudioAttributes;
        @GuardedBy("mLock")
        private boolean mHasAudioFocus;
        @GuardedBy("mLock")
        private boolean mResumeWhenAudioFocusGain;
        @GuardedBy("mLock")
        private boolean mHasRegisteredReceiver;

        AudioFocusHandlerImplBase(Context context, MediaSession2 session) {
            mContext = context;
            mSession = session;

            // Cannot use session.getContext() because session's impl isn't initialized at this
            // moment.
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        private AudioAttributesCompat getAudioAttributesNotLocked() {
            if (mSession.getVolumeProvider() != null) {
                // Remote session. Ignore audio attributes.
                return null;
            }
            BaseMediaPlayer player = mSession.getPlayer();
            return player == null ? null : player.getAudioAttributes();
        }

        @GuardedBy("mLock")
        private void updateAudioAttributesIfNeededLocked(AudioAttributesCompat attributes) {
            if (ObjectsCompat.equals(attributes, mAudioAttributes)) {
                // It's the same.
                return;
            }
            // Keep cache of the audio attributes. Otherwise audio attributes may be changed
            // between the audio focus request and audio focus change, resulting the unexpected
            // situation.
            mAudioAttributes = attributes;
            if (mHasAudioFocus) {
                mHasAudioFocus = requestAudioFocusLocked();
                if (!mHasAudioFocus) {
                    Log.w(TAG, "Failed to regain audio focus.");
                }
            }
        }

        @Override
        public boolean onPlayRequested() {
            // Instead of registering a listener for audio attribute changes, grabs the new one
            // here.
            final AudioAttributesCompat attr = getAudioAttributesNotLocked();
            synchronized (mLock) {
                updateAudioAttributesIfNeededLocked(attr);
                // Try getting audio focus.
                if (!requestAudioFocusLocked()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean onPauseRequested() {
            synchronized (mLock) {
                mResumeWhenAudioFocusGain = false;
            }
            return true;
        }

        @Override
        public void onPlayerStateChanged(int playerState) {
            switch (playerState) {
                case PLAYER_STATE_IDLE: {
                    synchronized (mLock) {
                        abandonAudioFocusLocked();
                    }
                    break;
                }
                case PLAYER_STATE_PAUSED: {
                    final AudioAttributesCompat attr = getAudioAttributesNotLocked();
                    synchronized (mLock) {
                        updateAudioAttributesIfNeededLocked(attr);
                        unregisterReceiverLocked();
                    }
                    break;
                }
                case PLAYER_STATE_PLAYING: {
                    final AudioAttributesCompat attr = getAudioAttributesNotLocked();
                    synchronized (mLock) {
                        updateAudioAttributesIfNeededLocked(attr);
                        registerReceiverLocked();
                    }
                    break;
                }
                case PLAYER_STATE_ERROR: {
                    close();
                    break;
                }
            }
        }

        @Override
        public void close() {
            synchronized (mLock) {
                unregisterReceiverLocked();
                abandonAudioFocusLocked();
            }
        }

        @Override
        public void sendIntent(Intent intent) {
            mBecomingNoisyIntentReceiver.onReceive(mContext, intent);
        }

        /**
         * Requests audio focus. This may regain audio focus.
         *
         * @return {@code true} if we don't need to handle audio focus nor audio focus was granted.
         *      {@code false} only when the attempt to request audio focus was failed.
         */
        @GuardedBy("mLock")
        private boolean requestAudioFocusLocked() {
            int focusGain = convertAudioAttributesToFocusGainLocked();
            if (focusGain == AudioManager.AUDIOFOCUS_NONE) {
                // Developer hasn't set audio focus request. Let the developer handle by themselves.
                return true;
            }
            // Note: This API is deprecated from the API level 26, but there's not much reason to
            // use the new API for now.
            int audioFocusRequestResult = mAudioManager.requestAudioFocus(mAudioFocusListener,
                    mAudioAttributes.getVolumeControlStream(), focusGain);
            if (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mHasAudioFocus = true;
            } else {
                Log.w(TAG, "requestAudioFocus(" + focusGain + ") failed (return="
                        + audioFocusRequestResult + ") playback wouldn't start.");
                mHasAudioFocus = false;
            }
            if (DEBUG) {
                Log.d(TAG, "requestAudioFocus(" + focusGain + "), result=" + mHasAudioFocus);
            }
            mResumeWhenAudioFocusGain = false;
            return mHasAudioFocus;
        }

        /**
         * Abandons audio focus if it has granted.
         */
        @GuardedBy("mLock")
        private void abandonAudioFocusLocked() {
            if (!mHasAudioFocus) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "abandonAudioFocus, result=" + mHasAudioFocus);
            }
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
            mHasAudioFocus = false;
            mResumeWhenAudioFocusGain = false;
        }

        @GuardedBy("mLock")
        private void registerReceiverLocked() {
            if (mHasRegisteredReceiver) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "registering noisy intent");
            }
            // Registering the receiver multiple-times may not be allowed for newer platform.
            // Register only when it's not registered.
            mContext.registerReceiver(mBecomingNoisyIntentReceiver, mIntentFilter);
            mHasRegisteredReceiver = true;
        }

        @GuardedBy("mLock")
        private void unregisterReceiverLocked() {
            if (!mHasRegisteredReceiver) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "unregistering noisy intent");
            }
            mContext.unregisterReceiver(mBecomingNoisyIntentReceiver);
            mHasRegisteredReceiver = false;
        }

        // Converts {@link AudioAttributesCompat} to one of the audio focus request. This follows
        // the class Javadoc of {@link AudioFocusRequest}.
        // Note: Implementation may not be the perfect match with the Javadoc because there's NO
        // clear documentation for audio focus handling with the specific usage and content type.
        @GuardedBy("mLock")
        private int convertAudioAttributesToFocusGainLocked() {
            AudioAttributesCompat audioAttributesCompat = mAudioAttributes;

            if (audioAttributesCompat == null) {
                return AudioManager.AUDIOFOCUS_NONE;
            }
            // Javadoc here means 'The different types of focus reuqests' written in the
            // {@link AudioFocusRequest}.
            switch (audioAttributesCompat.getUsage()) {
                // Javadoc says 'AUDIOFOCUS_GAIN: Examples of uses of this focus gain are for music
                // playback, for a game or a video player'
                case AudioAttributesCompat.USAGE_GAME:
                case AudioAttributesCompat.USAGE_MEDIA:
                    return AudioManager.AUDIOFOCUS_GAIN;

                // Javadoc says 'AUDIOFOCUS_GAIN_TRANSIENT: An example is for playing an alarm, or
                // during a VoIP call'
                case AudioAttributesCompat.USAGE_ALARM:
                case AudioAttributesCompat.USAGE_VOICE_COMMUNICATION:
                case AudioAttributesCompat.USAGE_VOICE_COMMUNICATION_SIGNALLING:
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

                // Javadoc says 'AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK: Examples are when playing
                // driving directions or notifications'
                case AudioAttributesCompat.USAGE_ASSISTANCE_ACCESSIBILITY:
                case AudioAttributesCompat.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                case AudioAttributesCompat.USAGE_ASSISTANCE_SONIFICATION:
                case AudioAttributesCompat.USAGE_ASSISTANT:
                case AudioAttributesCompat.USAGE_NOTIFICATION:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                case AudioAttributesCompat.USAGE_NOTIFICATION_EVENT:
                case AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE:
                case AudioAttributesCompat.USAGE_UNKNOWN:
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
            }
            // Javadoc also mentioned about AUDIOFOCUS_GAIN_EXCLUSIVE that 'This is typically used
            // if you are doing audio recording or speech recognition', but there's no way to
            // distinguish playback vs recording only with the AudioAttributesCompat, and using
            // media session for recording doesn't seem like a good use case. Don't handle audio
            // focus, so developer can can decide more finer grained control.
            return AudioManager.AUDIOFOCUS_NONE;
        }

        private class NoisyIntentReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DEBUG) {
                    Log.d(TAG, "Received noisy intent " + intent);
                }
                // This is always the main thread, except for the test.
                synchronized (mLock) {
                    if (!mHasRegisteredReceiver) {
                        return;
                    }
                }
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    final int usage;
                    synchronized (mLock) {
                        if (mAudioAttributes == null) {
                            return;
                        }
                        usage = mAudioAttributes.getUsage();
                    }
                    switch (usage) {
                        case AudioAttributesCompat.USAGE_MEDIA:
                            // Noisy intent guide says 'In the case of music players, users
                            // typically expect the playback to be paused.'
                            mSession.pause();
                            break;
                        case AudioAttributesCompat.USAGE_GAME:
                            // Noisy intent guide says 'For gaming apps, you may choose to
                            // significantly lower the volume instead'.
                            BaseMediaPlayer player = mSession.getPlayer();
                            if (player != null) {
                                player.setPlayerVolume(player.getPlayerVolume()
                                        * VOLUME_DUCK_FACTOR);
                            }
                            break;
                        default:
                            // Noisy intent guide didn't say anything more for this. No-op for now.
                            break;
                    }
                }
            }
        }

        private class AudioFocusListener implements OnAudioFocusChangeListener {
            private float mPlayerVolumeBeforeDucking;
            private float mPlayerDuckingVolume;

            // This is the thread where the AudioManager was originally instantiated.
            // see: b/78617702
            @Override
            public void onAudioFocusChange(int focusGain) {
                switch (focusGain) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Regains focus after the LOSS_TRANSIENT or LOSS_TRANSIENT_CAN_DUCK.
                        if (mSession.getPlayerState() == PLAYER_STATE_PAUSED) {
                            // Note: onPlayRequested() will be called again with this.
                            synchronized (mLock) {
                                if (!mResumeWhenAudioFocusGain) {
                                    break;
                                }
                            }
                            mSession.play();
                        } else {
                            BaseMediaPlayer player = mSession.getPlayer();
                            if (player != null) {
                                // Resets the volume if the user didn't change it.
                                final float currentVolume = player.getPlayerVolume();
                                final float volumeBeforeDucking;
                                synchronized (mLock) {
                                    if (currentVolume != mPlayerDuckingVolume) {
                                        // User manually changed the volume meanwhile. Don't reset.
                                        break;
                                    }
                                    volumeBeforeDucking = mPlayerVolumeBeforeDucking;
                                }
                                player.setPlayerVolume(volumeBeforeDucking);
                            }
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Audio-focus developer guide says 'Your app should pause playback
                        // immediately, as it won't ever receive an AUDIOFOCUS_GAIN callback'.
                        mSession.pause();
                        // Don't resume even after you regain the audio focus.
                        synchronized (mLock) {
                            mResumeWhenAudioFocusGain = false;
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        final boolean pause;
                        synchronized (mLock) {
                            if (mAudioAttributes == null) {
                                // This shouldn't happen. Just ignoring for now.
                                break;
                            }
                            pause = (mAudioAttributes.getContentType()
                                    == AudioAttributesCompat.CONTENT_TYPE_SPEECH);
                        }
                        if (pause) {
                            mSession.pause();
                        } else {
                            BaseMediaPlayer player = mSession.getPlayer();
                            if (player != null) {
                                // Lower the volume by the factor
                                final float currentVolume = player.getPlayerVolume();
                                final float duckingVolume = currentVolume * VOLUME_DUCK_FACTOR;
                                synchronized (mLock) {
                                    mPlayerVolumeBeforeDucking = currentVolume;
                                    mPlayerDuckingVolume = duckingVolume;
                                }
                                player.setPlayerVolume(duckingVolume);
                            }
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        mSession.pause();
                        // Resume after regaining the audio focus.
                        synchronized (mLock) {
                            mResumeWhenAudioFocusGain = true;
                        }
                        break;
                }
            }
        };
    }
}
