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
import static androidx.media2.SessionPlayer2.PLAYER_STATE_PAUSED;

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
        boolean onPlay();
        void onPause();
        void onReset();
        void close();
        void sendIntent(Intent intent);
    }

    private final AudioFocusHandlerImpl mImpl;

    AudioFocusHandler(Context context, MediaPlayer player) {
        mImpl = new AudioFocusHandlerImplBase(context, player);
    }

    /**
     * Should be called when the {@link MediaPlayer#play()} is called. Returns whether the play()
     * can be proceed.
     *
     * @return {@code true} if it's OK to start playback because audio focus was successfully
     * granted or audio focus isn't needed for starting playback. {@code false} otherwise.
     * (i.e. Audio focus is needed for starting playback but failed)
     */
    public boolean onPlay() {
        return mImpl.onPlay();
    }

    /**
     * Called when the {@link MediaPlayer#pause()} is called.
     */
    public void onPause() {
        mImpl.onPause();
    }

    /**
     * Called when the {@link MediaPlayer#reset()} is called.
     */
    public void onReset() {
        mImpl.onReset();
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
        private final BroadcastReceiver mBecomingNoisyReceiver = new BecomingNoisyReceiver();
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
        boolean mBecomingNoisyReceiverRegistered;

        AudioFocusHandlerImplBase(Context context, MediaPlayer player) {
            mContext = context;
            mPlayer = player;

            // Cannot use session.getContext() because session's impl isn't initialized at this
            // moment.
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        @Override
        public boolean onPlay() {
            final AudioAttributesCompat attrs = mPlayer.getAudioAttributes();
            boolean result = true;
            synchronized (mLock) {
                mAudioAttributes = attrs;
                // Checks whether the audio attributes is {@code null}, to check indirectly whether
                // the media item has audio track.
                if (attrs == null) {
                    // No sound.
                    abandonAudioFocusLocked();
                    unregisterBecomingNoisyReceiverLocked();
                } else {
                    result = requestAudioFocusLocked();
                    if (result) {
                        registerBecomingNoisyReceiverLocked();
                    }
                }
            }
            return result;
        }

        @Override
        public void onPause() {
            synchronized (mLock) {
                mResumeWhenAudioFocusGain = false;
                unregisterBecomingNoisyReceiverLocked();
            }
        }

        @Override
        public void onReset() {
            synchronized (mLock) {
                abandonAudioFocusLocked();
                unregisterBecomingNoisyReceiverLocked();
            }
        }

        @Override
        public void close() {
            synchronized (mLock) {
                unregisterBecomingNoisyReceiverLocked();
                abandonAudioFocusLocked();
            }
        }

        @Override
        public void sendIntent(Intent intent) {
            mBecomingNoisyReceiver.onReceive(mContext, intent);
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
        private void registerBecomingNoisyReceiverLocked() {
            if (mBecomingNoisyReceiverRegistered) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "registering becoming noisy receiver");
            }
            // Registering the receiver multiple-times may not be allowed for newer platform.
            // Register only when it's not registered.
            mContext.registerReceiver(mBecomingNoisyReceiver, mIntentFilter);
            mBecomingNoisyReceiverRegistered = true;
        }

        @GuardedBy("mLock")
        private void unregisterBecomingNoisyReceiverLocked() {
            if (!mBecomingNoisyReceiverRegistered) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "unregistering becoming noisy receiver");
            }
            mContext.unregisterReceiver(mBecomingNoisyReceiver);
            mBecomingNoisyReceiverRegistered = false;
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

        private class BecomingNoisyReceiver extends BroadcastReceiver {
            BecomingNoisyReceiver() {
            }

            // Note: This is always the main thread, except for the test.
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    return;
                }
                final int usage;
                synchronized (mLock) {
                    if (DEBUG) {
                        Log.d(TAG, "Received noisy intent, intent=" + intent + ", registered="
                                + mBecomingNoisyReceiverRegistered + ", attr=" + mAudioAttributes);
                    }
                    if (!mBecomingNoisyReceiverRegistered || mAudioAttributes == null) {
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
                            // Note: onPlay() will be called again with this.
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
