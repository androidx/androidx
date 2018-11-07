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

import static androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_ERROR;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_IDLE;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_PAUSED;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_PLAYING;

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
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.media.AudioAttributesCompat;

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
    private static final boolean DEBUG = true;

    interface AudioFocusHandlerImpl {
        boolean onPlayRequested();
        void onPauseRequested();
        void close();
        void sendIntent(Intent intent);
    }

    private final AudioFocusHandlerImpl mImpl;

    AudioFocusHandler(Context context, MediaPlayer player) {
        mImpl = new AudioFocusHandlerImplBase(context, player);
    }

    /**
     * Should be called when the {@link MediaSession2#play()} is called. Returns whether the play()
     * can be proceed.
     * <p>
     * This matches with the Session.Callback#onPlay() written in the guideline.
     *
     * @return {@code true} if it's OK to start playback because audio focus was successfully
     *         granted or audio focus isn't needed for starting playback. {@code false} otherwise.
     *         (i.e. Audio focus is needed for starting playback but failed)
     */
    public boolean onPlayRequested() {
        return mImpl.onPlayRequested();
    }

    /**
     * Should be called when the {@link MediaSession2#pause()} is called. Returns whether the
     * pause() can be proceed.
     */
    public void onPauseRequested() {
        mImpl.onPauseRequested();
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

    private static class AudioFocusHandlerImplBase extends SessionPlayer2.PlayerCallback
            implements AudioFocusHandlerImpl {
        // Value is from the {@link AudioFocusRequest} as follows
        // 'A typical attenuation by the “ducked” application is a factor of 0.2f (or -14dB), that
        // can for instance be applied with MediaPlayer.setVolume(0.2f) when using this class for
        // playback.'
        private static final float VOLUME_DUCK_FACTOR = 0.2f;
        private final BroadcastReceiver mBecomingNoisyIntentReceiver = new NoisyIntentReceiver();
        private final IntentFilter mIntentFilter =
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        private final OnAudioFocusChangeListener mAudioFocusListener = new AudioFocusListener();
        final Object mLock = new Object();
        private final Context mContext;
        final MediaPlayer mPlayer;
        private final AudioManager mAudioManager;

        @GuardedBy("mLock")
        AudioAttributesCompat mAudioAttributes;
        @GuardedBy("mLock")
        private int mCurrentFocusGainType;
        @GuardedBy("mLock")
        boolean mResumeWhenAudioFocusGain;
        @GuardedBy("mLock")
        boolean mHasRegisteredReceiver;

        AudioFocusHandlerImplBase(Context context, MediaPlayer player) {
            mContext = context;
            mPlayer = player;
            mPlayer.registerPlayerCallback(ContextCompat.getMainExecutor(context), this);

            // Cannot use session.getContext() because session's impl isn't initialized at this
            // moment.
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public boolean onPlayRequested() {
            final AudioAttributesCompat attrs = mPlayer.getAudioAttributes();
            boolean result = true;
            synchronized (mLock) {
                if (attrs == null) {
                    mAudioAttributes = null;
                    abandonAudioFocusLocked();
                } else {
                    // Keep cache of the audio attributes. Otherwise audio attributes may be changed
                    // between the audio focus request and audio focus change, resulting the
                    // unexpected situation.
                    mAudioAttributes = attrs;
                    result = requestAudioFocusLocked();
                }
            }
            if (attrs == null) {
                mPlayer.setPlayerVolume(0);
            }
            return result;
        }

        @Override
        public void onPauseRequested() {
            synchronized (mLock) {
                mResumeWhenAudioFocusGain = false;
            }
        }

        /**
         * Check we need to abandon/request audio focus when playback state becomes playing. It's
         * needed to handle following cases.
         *   1. Audio attribute has changed between {@link SessionPlayer2#play} and actual
         *      start of playback. Note that {@link MediaPlayer2} only allows changing audio
         *      attributes in IDLE state, so such issue wouldn't happen.
         *   2. Or, playback is started without MediaSession2#play().
         * <p>
         * If there's no huge issue, then register noisy intent receiver here.
         */
        private void onPlayingNotLocked() {
            final AudioAttributesCompat attrs = mPlayer.getAudioAttributes();
            final int expectedFocusGain;
            boolean pauseNeeded = false;
            synchronized (mLock) {
                expectedFocusGain = convertAudioAttributesToFocusGain(attrs);
                if (ObjectsCompat.equals(mAudioAttributes, attrs)
                        && expectedFocusGain == mCurrentFocusGainType) {
                    // No change in audio attributes, and audio focus is granted as expected.
                    // Register noisy intent if it has an audio attribute (i.e. has sound).
                    if (attrs != null) {
                        registerReceiverLocked();
                    }
                    return;
                }
                Log.w(TAG, "Expected " + mAudioAttributes + " and audioFocusGainType="
                        + mCurrentFocusGainType + " when playback is started, but actually "
                        + attrs
                        + " and audioFocusGainType=" + mCurrentFocusGainType + ". Use"
                        + " MediaSession2#play() for starting playback.");
                mAudioAttributes = attrs;
                if (mCurrentFocusGainType != expectedFocusGain) {
                    // Note: Calling AudioManager#requestAudioFocus() again with the same
                    //       listener but different focus gain type only updates the focus gain
                    //       type.
                    if (expectedFocusGain == AudioManager.AUDIOFOCUS_NONE) {
                        abandonAudioFocusLocked();
                    } else {
                        if (requestAudioFocusLocked()) {
                            registerReceiverLocked();
                        } else {
                            Log.e(TAG, "Playback is started without audio focus, and requesting"
                                    + " audio focus is failed. Forcefully pausing playback");
                            pauseNeeded = true;
                        }
                    }
                }
            }
            if (attrs == null) {
                // If attributes becomes null (i.e. no sound)
                mPlayer.setPlayerVolume(0);
            }
            if (pauseNeeded) {
                mPlayer.pause();
            }
        }

        @Override
        public void onPlayerStateChanged(SessionPlayer2 player, int playerState) {
            switch (playerState) {
                case PLAYER_STATE_IDLE: {
                    synchronized (mLock) {
                        abandonAudioFocusLocked();
                    }
                    break;
                }
                case PLAYER_STATE_PAUSED: {
                    synchronized (mLock) {
                        unregisterReceiverLocked();
                    }
                    break;
                }
                case PLAYER_STATE_PLAYING: {
                    onPlayingNotLocked();
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
            mPlayer.unregisterPlayerCallback(this);
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
         * @return {@code true} if audio focus is granted or isn't needed.
         *         {@code false} only when the attempt to request audio focus was failed.
         */
        @GuardedBy("mLock")
        private boolean requestAudioFocusLocked() {
            int focusGain = convertAudioAttributesToFocusGain(mAudioAttributes);
            if (focusGain == AudioManager.AUDIOFOCUS_NONE) {
                if (mAudioAttributes == null && DEBUG) {
                    // If audio attributes is null, it should be handled outside to set volume
                    // to zero without holding an lock.
                    Log.e(TAG, "requestAudioFocusLocked() shouldn't be called when AudioAttributes"
                            + " is null");
                }
                return true;
            }
            // Note: This API is deprecated from the API level 26, but there's not much reason to
            // use the new API for now.
            int audioFocusRequestResult = mAudioManager.requestAudioFocus(mAudioFocusListener,
                    mAudioAttributes.getVolumeControlStream(), focusGain);
            if (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mCurrentFocusGainType = focusGain;
            } else {
                Log.w(TAG, "requestAudioFocus(" + focusGain + ") failed (return="
                        + audioFocusRequestResult + ") playback wouldn't start.");
                mCurrentFocusGainType = AudioManager.AUDIOFOCUS_NONE;
            }
            if (DEBUG) {
                Log.d(TAG, "requestAudioFocus(" + focusGain + "), result="
                        + (audioFocusRequestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED));
            }
            mResumeWhenAudioFocusGain = false;
            return mCurrentFocusGainType != AudioManager.AUDIOFOCUS_NONE;
        }

        /**
         * Abandons audio focus if it has granted.
         */
        @GuardedBy("mLock")
        private void abandonAudioFocusLocked() {
            if (mCurrentFocusGainType == AudioManager.AUDIOFOCUS_NONE) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "abandoningAudioFocusLocked, currently=" + mCurrentFocusGainType);
            }
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
            mCurrentFocusGainType = AudioManager.AUDIOFOCUS_NONE;
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
        // Note: Any change here should also reflects public Javadoc of {@link MediaSession2}.
        private static int convertAudioAttributesToFocusGain(
                final AudioAttributesCompat audioAttributesCompat) {

            if (audioAttributesCompat == null) {
                // Don't handle audio focus. It may be either video only contents or developers
                // want to have more finer grained control. (e.g. adding audio focus listener)
                return AudioManager.AUDIOFOCUS_NONE;
            }
            // Javadoc here means 'The different types of focus reuqests' written in the
            // {@link AudioFocusRequest}.
            switch (audioAttributesCompat.getUsage()) {
                // USAGE_VOICE_COMMUNICATION_SIGNALLING is for DTMF that may happen multiple times
                // during the phone call when AUDIOFOCUS_GAIN_TRANSIENT is requested for that.
                // Don't request audio focus here.
                case AudioAttributesCompat.USAGE_VOICE_COMMUNICATION_SIGNALLING:
                    return AudioManager.AUDIOFOCUS_NONE;

                // Javadoc says 'AUDIOFOCUS_GAIN: Examples of uses of this focus gain are for music
                // playback, for a game or a video player'
                case AudioAttributesCompat.USAGE_GAME:
                case AudioAttributesCompat.USAGE_MEDIA:
                    return AudioManager.AUDIOFOCUS_GAIN;

                // Special usages: USAGE_UNKNOWN shouldn't be used. Request audio focus to prevent
                // multiple media playback happen at the same time.
                case AudioAttributesCompat.USAGE_UNKNOWN:
                    Log.w(TAG, "Specify a proper usage in the audio attributes for audio focus"
                            + " handling. Using AUDIOFOCUS_GAIN by default.");
                    return AudioManager.AUDIOFOCUS_GAIN;

                // Javadoc says 'AUDIOFOCUS_GAIN_TRANSIENT: An example is for playing an alarm, or
                // during a VoIP call'
                case AudioAttributesCompat.USAGE_ALARM:
                case AudioAttributesCompat.USAGE_VOICE_COMMUNICATION:
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

                // Javadoc says 'AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK: Examples are when playing
                // driving directions or notifications'
                case AudioAttributesCompat.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                case AudioAttributesCompat.USAGE_ASSISTANCE_SONIFICATION:
                case AudioAttributesCompat.USAGE_NOTIFICATION:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                case AudioAttributesCompat.USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                case AudioAttributesCompat.USAGE_NOTIFICATION_EVENT:
                case AudioAttributesCompat.USAGE_NOTIFICATION_RINGTONE:
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

                // Javadoc says 'AUDIOFOCUS_GAIN_EXCLUSIVE: This is typically used if you are doing
                // audio recording or speech recognition'.
                // Assistant is considered as both recording and notifying developer
                case AudioAttributesCompat.USAGE_ASSISTANT:
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;

                // Special usages:
                case AudioAttributesCompat.USAGE_ASSISTANCE_ACCESSIBILITY:
                    if (audioAttributesCompat.getContentType()
                            == AudioAttributesCompat.CONTENT_TYPE_SPEECH) {
                        // Voice shouldn't be interrupted by other playback.
                        return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
                    }
                    return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
            }
            Log.w(TAG, "Unidentified AudioAttribute " + audioAttributesCompat);
            return AudioManager.AUDIOFOCUS_NONE;
        }

        private class NoisyIntentReceiver extends BroadcastReceiver {
            NoisyIntentReceiver() {
            }

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
                            mPlayer.pause();
                            break;
                        case AudioAttributesCompat.USAGE_GAME:
                            // Noisy intent guide says 'For gaming apps, you may choose to
                            // significantly lower the volume instead'.
                            mPlayer.setPlayerVolume(mPlayer.getPlayerVolume() * VOLUME_DUCK_FACTOR);
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

            AudioFocusListener() {
            }

            // This is the thread where the AudioManager was originally instantiated.
            // see: b/78617702
            @Override
            public void onAudioFocusChange(int focusGain) {
                switch (focusGain) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // Regains focus after the LOSS_TRANSIENT or LOSS_TRANSIENT_CAN_DUCK.
                        if (mPlayer.getPlayerState() == PLAYER_STATE_PAUSED) {
                            // Note: onPlayRequested() will be called again with this.
                            synchronized (mLock) {
                                if (!mResumeWhenAudioFocusGain) {
                                    break;
                                }
                            }
                            mPlayer.play();
                        } else {
                            // Resets the volume if the user didn't change it.
                            final float currentVolume = mPlayer.getPlayerVolume();
                            final float volumeBeforeDucking;
                            synchronized (mLock) {
                                if (currentVolume != mPlayerDuckingVolume) {
                                    // User manually changed the volume meanwhile. Don't reset.
                                    break;
                                }
                                volumeBeforeDucking = mPlayerVolumeBeforeDucking;
                            }
                            mPlayer.setPlayerVolume(volumeBeforeDucking);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // Audio-focus developer guide says 'Your app should pause playback
                        // immediately, as it won't ever receive an AUDIOFOCUS_GAIN callback'.
                        mPlayer.pause();
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
                            mPlayer.pause();
                        } else {
                            // Lower the volume by the factor
                            final float currentVolume = mPlayer.getPlayerVolume();
                            final float duckingVolume = currentVolume * VOLUME_DUCK_FACTOR;
                            synchronized (mLock) {
                                mPlayerVolumeBeforeDucking = currentVolume;
                                mPlayerDuckingVolume = duckingVolume;
                            }
                            mPlayer.setPlayerVolume(duckingVolume);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        mPlayer.pause();
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
