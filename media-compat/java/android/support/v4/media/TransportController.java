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

/**
 * Base interface to controlling a media transport.  This is the
 * interface for implementing things like on-screen controls: it
 * allows them to request changes in playback, retrieve the current
 * playback state, and monitor for changes to the playback state.
 */
public abstract class TransportController {
    /**
     * Start listening to changes in playback state.
     */
    public abstract void registerStateListener(TransportStateListener listener);

    /**
     * Stop listening to changes in playback state.
     */
    public abstract void unregisterStateListener(TransportStateListener listener);

    /**
     * Request that the player start its playback at its current position.
     */
    public abstract void startPlaying();

    /**
     * Request that the player pause its playback and stay at its current position.
     */
    public abstract void pausePlaying();

    /**
     * Request that the player stop its playback; it may clear its state in whatever
     * way is appropriate.
     */
    public abstract void stopPlaying();

    /**
     * Retrieve the total duration of the media stream, in milliseconds.
     */
    public abstract long getDuration();

    /**
     * Retrieve the current playback location in the media stream, in milliseconds.
     */
    public abstract long getCurrentPosition();

    /**
     * Move to a new location in the media stream.
     * @param pos Position to move to, in milliseconds.
     */
    public abstract void seekTo(long pos);

    /**
     * Return whether the player is currently playing its stream.
     */
    public abstract boolean isPlaying();

    /**
     * Retrieve amount, in percentage (0-100), that the media stream has been buffered
     * on to the local device.  Return 100 if the stream is always local.
     */
    public abstract int getBufferPercentage();

    /**
     * Retrieve the flags for the media transport control buttons that this transport supports.
     * Result is a combination of the following flags:
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PREVIOUS},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_REWIND},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PLAY_PAUSE},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_PAUSE},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_STOP},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_FAST_FORWARD},
     *      {@link TransportMediator#FLAG_KEY_MEDIA_NEXT}
     */
    public abstract int getTransportControlFlags();
}
