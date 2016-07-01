/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.media;

import android.os.SystemClock;
import android.view.KeyEvent;

/**
 * Implemented by the playback side of the media system, to respond to
 * requests to perform actions and to retrieve its current state.  These
 * requests may either come from key events dispatched directly to your UI, or
 * events sent over a media button event receiver that this class keeps active
 * while your window is in focus.
 */
public abstract class TransportPerformer {
    /**
     * Request to start playback on the media, resuming from whatever current state
     * (position etc) it is in.
     */
    public abstract void onStart();

    /**
     * Request to pause playback of the media, staying at the current playback position
     * and other state so a later call to {@link #onStart()} will resume at the same place.
     */
    public abstract void onPause();

    /**
     * Request to completely stop playback of the media, clearing whatever state the
     * player thinks is appropriate.
     */
    public abstract void onStop();

    /**
     * Request to return the duration of the current media, in milliseconds.
     */
    public abstract long onGetDuration();

    /**
     * Request to return the current playback position, in milliseconds.
     */
    public abstract long onGetCurrentPosition();

    /**
     * Request to move the current playback position.
     * @param pos New position to move to, in milliseconds.
     */
    public abstract void onSeekTo(long pos);

    /**
     * Request to find out whether the player is currently playing its media.
     */
    public abstract boolean onIsPlaying();

    /**
     * Request to find out how much of the media has been buffered on the local device.
     * @return Return a percentage (0-100) indicating how much of the total data
     * has been buffered.  The default implementation returns 100, meaning the content
     * is always on the local device.
     */
    public int onGetBufferPercentage() {
        return 100;
    }

    /**
     * Retrieves the flags for the media transport control buttons that this transport supports.
     * Result is a combination of the following flags:
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PREVIOUS},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_REWIND},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PAUSE},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_STOP},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_FAST_FORWARD},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_NEXT}
     *
     * <p>The default implementation returns:
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PAUSE}, and
     *      {@link TransportMediator#FLAG_KEY_MEDIA_STOP}</p>
     */
    public int onGetTransportControlFlags() {
        return TransportMediator.FLAG_KEY_MEDIA_PLAY
                | TransportMediator.FLAG_KEY_MEDIA_PLAY_PAUSE
                | TransportMediator.FLAG_KEY_MEDIA_PAUSE
                | TransportMediator.FLAG_KEY_MEDIA_STOP;
    }

    /**
     * Report that a media button has been pressed.  This is like
     * {@link android.view.KeyEvent.Callback#onKeyDown(int, android.view.KeyEvent)} but
     * will only deliver media keys.  The default implementation handles these keys:
     * <ul>
     *     <li>KEYCODE_MEDIA_PLAY: call {@link #onStart}</li>
     *     <li>KEYCODE_MEDIA_PAUSE: call {@link #onPause}</li>
     *     <li>KEYCODE_MEDIA_STOP: call {@link #onStop}</li>
     *     <li>KEYCODE_MEDIA_PLAY_PAUSE and KEYCODE_HEADSETHOOK: call {@link #onPause}
     *          if {@link #onIsPlaying()} returns true, otherwise call {@link #onStart}</li>
     * </ul>
     * @param keyCode The code of the media key.
     * @param event The full key event.
     * @return Indicate whether the key has been consumed.  The default
     * implementation always returns true.  This only matters for keys
     * being dispatched here from
     * {@link TransportMediator#dispatchKeyEvent(android.view.KeyEvent)
     * TransportController.dispatchKeyEvent}, and determines whether the key
     * continues on to its default key handling (which for media keys means
     * being delivered to the current media remote control, which should
     * be us).
     */
    public boolean onMediaButtonDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case TransportMediator.KEYCODE_MEDIA_PLAY:
                onStart();
                return true;
            case TransportMediator.KEYCODE_MEDIA_PAUSE:
                onPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                onStop();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (onIsPlaying()) {
                    onPause();
                } else {
                    onStart();
                }
        }
        return true;
    }

    /**
     * Report that a media button has been released.  This is like
     * {@link KeyEvent.Callback#onKeyUp(int, android.view.KeyEvent)} but
     * will only deliver media keys.  The default implementation does nothing.
     * @param keyCode The code of the media key.
     * @param event The full key event.
     * @return Indicate whether the key has been consumed.  The default
     * implementation always returns true.  This only matters for keys
     * being dispatched here from
     * {@link TransportMediator#dispatchKeyEvent(android.view.KeyEvent)
     * TransportController.dispatchKeyEvent}, and determines whether the key
     * continues on to its default key handling (which for media keys means
     * being delivered to the current media remote control, which should
     * be us).
     */
    public boolean onMediaButtonUp(int keyCode, KeyEvent event) {
        return true;
    }

    // Copy constants from framework since we can't link to them.
    static final int AUDIOFOCUS_GAIN = 1;
    static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    static final int AUDIOFOCUS_LOSS = -1 * AUDIOFOCUS_GAIN;
    static final int AUDIOFOCUS_LOSS_TRANSIENT = -1 * AUDIOFOCUS_GAIN_TRANSIENT;
    static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK =
            -1 * AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

    /**
     * Report that audio focus has changed on the app.  This only happens if
     * you have indicated you have started playing with
     * {@link TransportMediator#startPlaying TransportController.startPlaying},
     * which takes audio focus for you.
     * @param focusChange The type of focus change, as per
     * {@link android.media.AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     * OnAudioFocusChangeListener.onAudioFocusChange}.  The default implementation will
     * deliver a {@link KeyEvent#KEYCODE_MEDIA_STOP}
     * when receiving {@link android.media.AudioManager#AUDIOFOCUS_LOSS}.
     */
    public void onAudioFocusChange(int focusChange) {
        int keyCode = 0;
        switch (focusChange) {
            case AUDIOFOCUS_LOSS:
                // This will cause us to stop playback, which means we drop audio focus
                // so we will not get any further audio focus gain.
                keyCode = TransportMediator.KEYCODE_MEDIA_PAUSE;
                break;
        }
        if (keyCode != 0) {
            final long now = SystemClock.uptimeMillis();
            onMediaButtonDown(keyCode, new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            onMediaButtonUp(keyCode, new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
        }
    }
}
