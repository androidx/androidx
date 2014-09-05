/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.media.session;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.text.TextUtils;
import android.view.KeyEvent;

/**
 * Allows an app to interact with an ongoing media session. Media buttons and
 * other commands can be sent to the session. A callback may be registered to
 * receive updates from the session, such as metadata and play state changes.
 * <p>
 * A MediaController can be created if you have a {@link MediaSessionCompat.Token}
 * from the session owner.
 * <p>
 * MediaController objects are thread-safe.
 * <p>
 * This is a helper for accessing features in {@link android.media.session.MediaSession}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class MediaControllerCompat {
    private final MediaControllerImpl mImpl;

    /**
     * Creates a media controller from a session.
     *
     * @param session The session to be controlled.
     */
    public MediaControllerCompat(Context context, MediaSessionCompat session) {
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaControllerImplApi21(context, session);
        } else {
            mImpl = new MediaControllerImplBase();
        }
    }

    /**
     * Creates a media controller from a session token which may have
     * been obtained from another process.
     *
     * @param sessionToken The token of the session to be controlled.
     * @throws RemoteException if the session is not accessible.
     */
    public MediaControllerCompat(Context context, MediaSessionCompat.Token sessionToken)
            throws RemoteException {
        if (sessionToken == null) {
            throw new IllegalArgumentException("sessionToken must not be null");
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaControllerImplApi21(context, sessionToken);
        } else {
            mImpl = new MediaControllerImplBase();
        }
    }

    /**
     * Get a {@link TransportControls} instance for this session.
     *
     * @return A controls instance
     */
    public TransportControls getTransportControls() {
        return mImpl.getTransportControls();
    }

    /**
     * Send the specified media button event to the session. Only media keys can
     * be sent by this method, other keys will be ignored.
     *
     * @param keyEvent The media button event to dispatch.
     * @return true if the event was sent to the session, false otherwise.
     */
    public boolean dispatchMediaButtonEvent(KeyEvent keyEvent) {
        if (keyEvent == null) {
            throw new IllegalArgumentException("KeyEvent may not be null");
        }
        return mImpl.dispatchMediaButtonEvent(keyEvent);
    }

    /**
     * Get the current playback state for this session.
     *
     * @return The current PlaybackState or null
     */
    public PlaybackStateCompat getPlaybackState() {
        return mImpl.getPlaybackState();
    }

    /**
     * Get the current metadata for this session.
     *
     * @return The current MediaMetadata or null.
     */
    public MediaMetadataCompat getMetadata() {
        return mImpl.getMetadata();
    }

    /**
     * Get the rating type supported by the session. One of:
     * <ul>
     * <li>{@link RatingCompat#RATING_NONE}</li>
     * <li>{@link RatingCompat#RATING_HEART}</li>
     * <li>{@link RatingCompat#RATING_THUMB_UP_DOWN}</li>
     * <li>{@link RatingCompat#RATING_3_STARS}</li>
     * <li>{@link RatingCompat#RATING_4_STARS}</li>
     * <li>{@link RatingCompat#RATING_5_STARS}</li>
     * <li>{@link RatingCompat#RATING_PERCENTAGE}</li>
     * </ul>
     *
     * @return The supported rating type
     */
    public int getRatingType() {
        return mImpl.getRatingType();
    }

    /**
     * Get the current playback info for this session.
     *
     * @return The current playback info or null.
     */
    public PlaybackInfo getPlaybackInfo() {
        return mImpl.getPlaybackInfo();
    }

    /**
     * Adds a callback to receive updates from the Session. Updates will be
     * posted on the caller's thread.
     *
     * @param callback The callback object, must not be null.
     */
    public void registerCallback(Callback callback) {
        registerCallback(callback, null);
    }

    /**
     * Adds a callback to receive updates from the session. Updates will be
     * posted on the specified handler's thread.
     *
     * @param callback The callback object, must not be null.
     * @param handler The handler to post updates on. If null the callers thread
     *            will be used.
     */
    public void registerCallback(Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (handler == null) {
            handler = new Handler();
        }
        mImpl.registerCallback(callback, handler);
    }

    /**
     * Stop receiving updates on the specified callback. If an update has
     * already been posted you may still receive it after calling this method.
     *
     * @param callback The callback to remove
     */
    public void unregisterCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        mImpl.unregisterCallback(callback);
    }

    /**
     * Sends a generic command to the session. It is up to the session creator
     * to decide what commands and parameters they will support. As such,
     * commands should only be sent to sessions that the controller owns.
     *
     * @param command The command to send
     * @param params Any parameters to include with the command
     * @param cb The callback to receive the result on
     */
    public void sendCommand(String command, Bundle params, ResultReceiver cb) {
        if (TextUtils.isEmpty(command)) {
            throw new IllegalArgumentException("command cannot be null or empty");
        }
        mImpl.sendCommand(command, params, cb);
    }

    /**
     * Gets the underlying framework {@link android.media.session.MediaController} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @return The underlying {@link android.media.session.MediaController} object,
     * or null if none.
     */
    public Object getMediaController() {
        return mImpl.getMediaController();
    }

    /**
     * Callback for receiving updates on from the session. A Callback can be
     * registered using {@link #registerCallback}
     */
    public static abstract class Callback {
        final Object mCallbackObj;

        public Callback() {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mCallbackObj = MediaControllerCompatApi21.createCallback(new StubApi21());
            } else {
                mCallbackObj = null;
            }
        }

        /**
         * Override to handle the session being destroyed. The session is no
         * longer valid after this call and calls to it will be ignored.
         */
        public void onSessionDestroyed() {
        }

        /**
         * Override to handle custom events sent by the session owner without a
         * specified interface. Controllers should only handle these for
         * sessions they own.
         *
         * @param event The event from the session.
         * @param extras Optional parameters for the event.
         */
        public void onSessionEvent(String event, Bundle extras) {
        }

        /**
         * Override to handle changes in playback state.
         *
         * @param state The new playback state of the session
         */
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
        }

        /**
         * Override to handle changes to the current metadata.
         *
         * @param metadata The current metadata for the session or null if none.
         * @see MediaMetadata
         */
        public void onMetadataChanged(MediaMetadataCompat metadata) {
        }

        private class StubApi21 implements MediaControllerCompatApi21.Callback {
            @Override
            public void onSessionDestroyed() {
                Callback.this.onSessionDestroyed();
            }

            @Override
            public void onSessionEvent(String event, Bundle extras) {
                Callback.this.onSessionEvent(event, extras);
            }

            @Override
            public void onPlaybackStateChanged(Object stateObj) {
                Callback.this.onPlaybackStateChanged(
                        PlaybackStateCompat.fromPlaybackState(stateObj));
            }

            @Override
            public void onMetadataChanged(Object metadataObj) {
                Callback.this.onMetadataChanged(
                        MediaMetadataCompat.fromMediaMetadata(metadataObj));
            }
        }
    }

    /**
     * Interface for controlling media playback on a session. This allows an app
     * to send media transport commands to the session.
     */
    public static abstract class TransportControls {
        TransportControls() {
        }

        /**
         * Request that the player start its playback at its current position.
         */
        public abstract void play();

        /**
         * Request that the player pause its playback and stay at its current
         * position.
         */
        public abstract void pause();

        /**
         * Request that the player stop its playback; it may clear its state in
         * whatever way is appropriate.
         */
        public abstract void stop();

        /**
         * Move to a new location in the media stream.
         *
         * @param pos Position to move to, in milliseconds.
         */
        public abstract void seekTo(long pos);

        /**
         * Start fast forwarding. If playback is already fast forwarding this
         * may increase the rate.
         */
        public abstract void fastForward();

        /**
         * Skip to the next item.
         */
        public abstract void skipToNext();

        /**
         * Start rewinding. If playback is already rewinding this may increase
         * the rate.
         */
        public abstract void rewind();

        /**
         * Skip to the previous item.
         */
        public abstract void skipToPrevious();

        /**
         * Rate the current content. This will cause the rating to be set for
         * the current user. The Rating type must match the type returned by
         * {@link #getRatingType()}.
         *
         * @param rating The rating to set for the current content
         */
        public abstract void setRating(RatingCompat rating);
    }

    /**
     * Holds information about the way volume is handled for this session.
     */
    public static final class PlaybackInfo {
        /**
         * The session uses local playback.
         */
        public static final int PLAYBACK_TYPE_LOCAL = 1;
        /**
         * The session uses remote playback.
         */
        public static final int PLAYBACK_TYPE_REMOTE = 2;

        private final int mPlaybackType;
        // TODO update audio stream with AudioAttributes support version
        private final int mAudioStream;
        private final int mVolumeControl;
        private final int mMaxVolume;
        private final int mCurrentVolume;

        PlaybackInfo(int type, int stream, int control, int max, int current) {
            mPlaybackType = type;
            mAudioStream = stream;
            mVolumeControl = control;
            mMaxVolume = max;
            mCurrentVolume = current;
        }

        /**
         * Get the type of volume handling, either local or remote. One of:
         * <ul>
         * <li>{@link PlaybackInfo#PLAYBACK_TYPE_LOCAL}</li>
         * <li>{@link PlaybackInfo#PLAYBACK_TYPE_REMOTE}</li>
         * </ul>
         *
         * @return The type of volume handling this session is using.
         */
        public int getPlaybackType() {
            return mPlaybackType;
        }

        /**
         * Get the stream this is currently controlling volume on. When the volume
         * type is {@link PlaybackInfo#PLAYBACK_TYPE_REMOTE} this value does not
         * have meaning and should be ignored.
         *
         * @return The stream this session is playing on.
         */
        public int getAudioStream() {
            return mAudioStream;
        }

        /**
         * Get the type of volume control that can be used. One of:
         * <ul>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_RELATIVE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_FIXED}</li>
         * </ul>
         *
         * @return The type of volume control that may be used with this
         *         session.
         */
        public int getVolumeControl() {
            return mVolumeControl;
        }

        /**
         * Get the maximum volume that may be set for this session.
         *
         * @return The maximum allowed volume where this session is playing.
         */
        public int getMaxVolume() {
            return mMaxVolume;
        }

        /**
         * Get the current volume for this session.
         *
         * @return The current volume where this session is playing.
         */
        public int getCurrentVolume() {
            return mCurrentVolume;
        }
    }

    interface MediaControllerImpl {
        void registerCallback(Callback callback, Handler handler);

        void unregisterCallback(Callback callback);
        boolean dispatchMediaButtonEvent(KeyEvent keyEvent);
        TransportControls getTransportControls();
        PlaybackStateCompat getPlaybackState();
        MediaMetadataCompat getMetadata();
        int getRatingType();
        PlaybackInfo getPlaybackInfo();
        void sendCommand(String command, Bundle params, ResultReceiver cb);
        Object getMediaController();
    }

    // TODO: compatibility implementation
    static class MediaControllerImplBase implements MediaControllerImpl {
        @Override
        public void registerCallback(Callback callback, Handler handler) {
        }

        @Override
        public void unregisterCallback(Callback callback) {
        }

        @Override
        public boolean dispatchMediaButtonEvent(KeyEvent event) {
            return false;
        }

        @Override
        public TransportControls getTransportControls() {
            return null;
        }

        @Override
        public PlaybackStateCompat getPlaybackState() {
            return null;
        }

        @Override
        public MediaMetadataCompat getMetadata() {
            return null;
        }

        @Override
        public int getRatingType() {
            return 0;
        }

        @Override
        public PlaybackInfo getPlaybackInfo() {
            return null;
        }

        @Override
        public void sendCommand(String command, Bundle params, ResultReceiver cb) {
        }

        @Override
        public Object getMediaController() {
            return null;
        }
    }

    static class MediaControllerImplApi21 implements MediaControllerImpl {
        private final Object mControllerObj;

        public MediaControllerImplApi21(Context context, MediaSessionCompat session) {
            mControllerObj = MediaControllerCompatApi21.fromToken(context,
                    session.getSessionToken().getToken());
        }

        public MediaControllerImplApi21(Context context, MediaSessionCompat.Token sessionToken)
                throws RemoteException {
            // TODO: refactor framework implementation
            mControllerObj = MediaControllerCompatApi21.fromToken(context,
                    sessionToken.getToken());
            if (mControllerObj == null) throw new RemoteException();
        }

        @Override
        public void registerCallback(Callback callback, Handler handler) {
            MediaControllerCompatApi21.registerCallback(mControllerObj, callback.mCallbackObj, handler);
        }

        @Override
        public void unregisterCallback(Callback callback) {
            MediaControllerCompatApi21.unregisterCallback(mControllerObj, callback.mCallbackObj);
        }

        @Override
        public boolean dispatchMediaButtonEvent(KeyEvent event) {
            return MediaControllerCompatApi21.dispatchMediaButtonEvent(mControllerObj, event);
        }

        @Override
        public TransportControls getTransportControls() {
            Object controlsObj = MediaControllerCompatApi21.getTransportControls(mControllerObj);
            return controlsObj != null ? new TransportControlsApi21(controlsObj) : null;
        }

        @Override
        public PlaybackStateCompat getPlaybackState() {
            Object stateObj = MediaControllerCompatApi21.getPlaybackState(mControllerObj);
            return stateObj != null ? PlaybackStateCompat.fromPlaybackState(stateObj) : null;
        }

        @Override
        public MediaMetadataCompat getMetadata() {
            Object metadataObj = MediaControllerCompatApi21.getMetadata(mControllerObj);
            return metadataObj != null ? MediaMetadataCompat.fromMediaMetadata(metadataObj) : null;
        }

        @Override
        public int getRatingType() {
            return MediaControllerCompatApi21.getRatingType(mControllerObj);
        }

        @Override
        public PlaybackInfo getPlaybackInfo() {
            Object volumeInfoObj = MediaControllerCompatApi21.getPlaybackInfo(mControllerObj);
            return volumeInfoObj != null ? new PlaybackInfo(
                    MediaControllerCompatApi21.PlaybackInfo.getPlaybackType(volumeInfoObj),
                    MediaControllerCompatApi21.PlaybackInfo.getLegacyAudioStream(volumeInfoObj),
                    MediaControllerCompatApi21.PlaybackInfo.getVolumeControl(volumeInfoObj),
                    MediaControllerCompatApi21.PlaybackInfo.getMaxVolume(volumeInfoObj),
                    MediaControllerCompatApi21.PlaybackInfo.getCurrentVolume(volumeInfoObj)) : null;
        }

        @Override
        public void sendCommand(String command, Bundle params, ResultReceiver cb) {
            MediaControllerCompatApi21.sendCommand(mControllerObj, command, params, cb);
        }

        @Override
        public Object getMediaController() {
            return mControllerObj;
        }
    }

    static class TransportControlsApi21 extends TransportControls {
        private final Object mControlsObj;

        public TransportControlsApi21(Object controlsObj) {
            mControlsObj = controlsObj;
        }

        @Override
        public void play() {
            MediaControllerCompatApi21.TransportControls.play(mControlsObj);
        }

        @Override
        public void pause() {
            MediaControllerCompatApi21.TransportControls.pause(mControlsObj);
        }

        @Override
        public void stop() {
            MediaControllerCompatApi21.TransportControls.stop(mControlsObj);
        }

        @Override
        public void seekTo(long pos) {
            MediaControllerCompatApi21.TransportControls.seekTo(mControlsObj, pos);
        }

        @Override
        public void fastForward() {
            MediaControllerCompatApi21.TransportControls.fastForward(mControlsObj);
        }

        @Override
        public void rewind() {
            MediaControllerCompatApi21.TransportControls.rewind(mControlsObj);
        }

        @Override
        public void skipToNext() {
            MediaControllerCompatApi21.TransportControls.skipToNext(mControlsObj);
        }

        @Override
        public void skipToPrevious() {
            MediaControllerCompatApi21.TransportControls.skipToPrevious(mControlsObj);
        }

        @Override
        public void setRating(RatingCompat rating) {
            MediaControllerCompatApi21.TransportControls.setRating(mControlsObj,
                    rating != null ? rating.getRating() : null);
        }
    }
}
