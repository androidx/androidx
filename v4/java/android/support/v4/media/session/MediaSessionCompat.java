
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
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.text.TextUtils;

/**
 * Allows interaction with media controllers, volume keys, media buttons, and
 * transport controls.
 * <p>
 * A MediaSession should be created when an app wants to publish media playback
 * information or handle media keys. In general an app only needs one session
 * for all playback, though multiple sessions can be created to provide finer
 * grain controls of media.
 * <p>
 * Once a session is created the owner of the session may pass its
 * {@link #getSessionToken() session token} to other processes to allow them to
 * create a {@link MediaControllerCompat} to interact with the session.
 * <p>
 * To receive commands, media keys, and other events a {@link Callback} must be
 * set with {@link #addCallback(Callback)}. To receive transport control
 * commands a {@link TransportControlsCallback} must be set with
 * {@link #addTransportControlsCallback}.
 * <p>
 * When an app is finished performing playback it must call {@link #release()}
 * to clean up the session and notify any controllers.
 * <p>
 * MediaSession objects are thread safe.
 * <p>
 * This is a helper for accessing features in {@link android.media.session.MediaSession}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class MediaSessionCompat {
    private final MediaSessionImpl mImpl;

    /**
     * Set this flag on the session to indicate that it can handle media button
     * events.
     */
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1 << 0;

    /**
     * Set this flag on the session to indicate that it handles transport
     * control commands through a {@link TransportControlsCallback}.
     * The callback can be retrieved by calling {@link #addTransportControlsCallback}.
     */
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 1 << 1;

    /**
     * The session uses local playback.
     */
    public static final int VOLUME_TYPE_LOCAL = 1;

    /**
     * The session uses remote playback.
     */
    public static final int VOLUME_TYPE_REMOTE = 2;

    /**
     * Creates a new session.
     *
     * @param context The context.
     * @param tag A short name for debugging purposes.
     */
    public MediaSessionCompat(Context context, String tag) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag must not be null or empty");
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaSessionImplApi21(context, tag);
        } else {
            mImpl = new MediaSessionImplBase();
        }
    }

    /**
     * Add a callback to receive updates on for the MediaSession. This includes
     * media button and volume events. The caller's thread will be used to post
     * events.
     *
     * @param callback The callback object
     */
    public void addCallback(Callback callback) {
        addCallback(callback, null);
    }

    /**
     * Add a callback to receive updates for the MediaSession. This includes
     * media button and volume events.
     *
     * @param callback The callback to receive updates on.
     * @param handler The handler that events should be posted on.
     */
    public void addCallback(Callback callback, Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        mImpl.addCallback(callback, handler != null ? handler : new Handler());
    }

    /**
     * Remove a callback. It will no longer receive updates.
     *
     * @param callback The callback to remove.
     */
    public void removeCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        mImpl.removeCallback(callback);
    }

    /**
     * Set any flags for the session.
     *
     * @param flags The flags to set for this session.
     */
    public void setFlags(int flags) {
        mImpl.setFlags(flags);
    }

    /**
     * Set the stream this session is playing on. This will affect the system's
     * volume handling for this session. If {@link #setPlaybackToRemote} was
     * previously called it will stop receiving volume commands and the system
     * will begin sending volume changes to the appropriate stream.
     * <p>
     * By default sessions are on {@link AudioManager#STREAM_MUSIC}.
     *
     * @param stream The {@link AudioManager} stream this session is playing on.
     */
    public void setPlaybackToLocal(int stream) {
        mImpl.setPlaybackToLocal(stream);
    }

    /**
     * Configure this session to use remote volume handling. This must be called
     * to receive volume button events, otherwise the system will adjust the
     * current stream volume for this session. If {@link #setPlaybackToLocal}
     * was previously called that stream will stop receiving volume changes for
     * this session.
     *
     * @param volumeProvider The provider that will handle volume changes. May
     *            not be null.
     */
    public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
        if (volumeProvider == null) {
            throw new IllegalArgumentException("volumeProvider may not be null!");
        }
        mImpl.setPlaybackToRemote(volumeProvider);
    }

    /**
     * Set if this session is currently active and ready to receive commands. If
     * set to false your session's controller may not be discoverable. You must
     * set the session to active before it can start receiving media button
     * events or transport commands.
     *
     * @param active Whether this session is active or not.
     */
    public void setActive(boolean active) {
        mImpl.setActive(active);
    }

    /**
     * Get the current active state of this session.
     *
     * @return True if the session is active, false otherwise.
     */
    public boolean isActive() {
        return mImpl.isActive();
    }

    /**
     * Send a proprietary event to all MediaControllers listening to this
     * Session. It's up to the Controller/Session owner to determine the meaning
     * of any events.
     *
     * @param event The name of the event to send
     * @param extras Any extras included with the event
     */
    public void sendSessionEvent(String event, Bundle extras) {
        if (TextUtils.isEmpty(event)) {
            throw new IllegalArgumentException("event cannot be null or empty");
        }
        mImpl.sendSessionEvent(event, extras);
    }

    /**
     * This must be called when an app has finished performing playback. If
     * playback is expected to start again shortly the session can be left open,
     * but it must be released if your activity or service is being destroyed.
     */
    public void release() {
        mImpl.release();
    }

    /**
     * Retrieve a token object that can be used by apps to create a
     * {@link MediaControllerCompat} for interacting with this session. The owner of
     * the session is responsible for deciding how to distribute these tokens.
     *
     * @return A token that can be used to create a MediaController for this
     *         session.
     */
    public Token getSessionToken() {
        return mImpl.getSessionToken();
    }

    /**
     * Add a callback to receive transport controls on, such as play, rewind, or
     * fast forward.
     *
     * @param callback The callback object.
     */
    public void addTransportControlsCallback(TransportControlsCallback callback) {
        addTransportControlsCallback(callback, null);
    }

    /**
     * Add a callback to receive transport controls on, such as play, rewind, or
     * fast forward. The updates will be posted to the specified handler. If no
     * handler is provided they will be posted to the caller's thread.
     *
     * @param callback The callback to receive updates on.
     * @param handler The handler to post the updates on.
     */
    public void addTransportControlsCallback(TransportControlsCallback callback,
            Handler handler) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        mImpl.addTransportControlsCallback(callback, handler != null ? handler : new Handler());
    }

    /**
     * Stop receiving transport controls on the specified callback. If an update
     * has already been posted you may still receive it after this call returns.
     *
     * @param callback The callback to stop receiving updates on.
     */
    public void removeTransportControlsCallback(TransportControlsCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback cannot be null");
        }
        mImpl.removeTransportControlsCallback(callback);
    }

    /**
     * Update the current playback state.
     *
     * @param state The current state of playback
     */
    public void setPlaybackState(PlaybackStateCompat state) {
        mImpl.setPlaybackState(state);
    }

    /**
     * Update the current metadata. New metadata can be created using
     * {@link android.media.MediaMetadata.Builder}.
     *
     * @param metadata The new metadata
     */
    public void setMetadata(MediaMetadataCompat metadata) {
        mImpl.setMetadata(metadata);
    }

    /**
     * Gets the underlying framework {@link android.media.session.MediaSession} object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @return The underlying {@link android.media.session.MediaSession} object,
     * or null if none.
     */
    public Object getMediaSession() {
        return mImpl.getMediaSession();
    }

    /**
     * Receives generic commands or updates from controllers and the system.
     * Callbacks may be registered using {@link #addCallback}.
     */
    public abstract static class Callback {
        final Object mCallbackObj;

        public Callback() {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mCallbackObj = MediaSessionCompatApi21.createCallback(new StubApi21());
            } else {
                mCallbackObj = null;
            }
        }

        /**
         * Called when a media button is pressed and this session has the
         * highest priority or a controller sends a media button event to the
         * session. TODO determine if using Intents identical to the ones
         * RemoteControlClient receives is useful
         * <p>
         * The intent will be of type {@link Intent#ACTION_MEDIA_BUTTON} with a
         * KeyEvent in {@link Intent#EXTRA_KEY_EVENT}
         *
         * @param mediaButtonIntent an intent containing the KeyEvent as an
         *            extra
         */
        public void onMediaButtonEvent(Intent mediaButtonIntent) {
        }

        /**
         * Called when a controller has sent a custom command to this session.
         * The owner of the session may handle custom commands but is not
         * required to.
         *
         * @param command The command name.
         * @param extras Optional parameters for the command, may be null.
         * @param cb A result receiver to which a result may be sent by the command, may be null.
         */
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
        }

        private class StubApi21 implements MediaSessionCompatApi21.Callback {
            @Override
            public void onMediaButtonEvent(Intent mediaButtonIntent) {
                Callback.this.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                Callback.this.onCommand(command, extras, cb);
            }
        }
    }

    /**
     * Receives transport control commands. Callbacks may be registered using
     * {@link #addTransportControlsCallback}.
     */
    public static abstract class TransportControlsCallback {
        final Object mCallbackObj;

        public TransportControlsCallback() {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mCallbackObj = MediaSessionCompatApi21.createTransportControlsCallback(
                        new StubApi21());
            } else {
                mCallbackObj = null;
            }
        }

        /**
         * Override to handle requests to begin playback.
         */
        public void onPlay() {
        }

        /**
         * Override to handle requests to pause playback.
         */
        public void onPause() {
        }

        /**
         * Override to handle requests to skip to the next media item.
         */
        public void onSkipToNext() {
        }

        /**
         * Override to handle requests to skip to the previous media item.
         */
        public void onSkipToPrevious() {
        }

        /**
         * Override to handle requests to fast forward.
         */
        public void onFastForward() {
        }

        /**
         * Override to handle requests to rewind.
         */
        public void onRewind() {
        }

        /**
         * Override to handle requests to stop playback.
         */
        public void onStop() {
        }

        /**
         * Override to handle requests to seek to a specific position in ms.
         *
         * @param pos New position to move to, in milliseconds.
         */
        public void onSeekTo(long pos) {
        }

        /**
         * Override to handle the item being rated.
         *
         * @param rating
         */
        public void onSetRating(RatingCompat rating) {
        }

        private class StubApi21 implements MediaSessionCompatApi21.TransportControlsCallback {
            @Override
            public void onPlay() {
                TransportControlsCallback.this.onPlay();
            }

            @Override
            public void onPause() {
                TransportControlsCallback.this.onPause();
            }

            @Override
            public void onSkipToNext() {
                TransportControlsCallback.this.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                TransportControlsCallback.this.onSkipToPrevious();
            }

            @Override
            public void onFastForward() {
                TransportControlsCallback.this.onFastForward();
            }

            @Override
            public void onRewind() {
                TransportControlsCallback.this.onRewind();
            }

            @Override
            public void onStop() {
                TransportControlsCallback.this.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                TransportControlsCallback.this.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Object ratingObj) {
                TransportControlsCallback.this.onSetRating(RatingCompat.fromRating(ratingObj));
            }
        }
    }

    /**
     * Represents an ongoing session. This may be passed to apps by the session
     * owner to allow them to create a {@link MediaControllerCompat} to communicate with
     * the session.
     */
    public static final class Token implements Parcelable {
        private final Parcelable mInner;

        Token(Parcelable inner) {
            mInner = inner;
        }

        @Override
        public int describeContents() {
            return mInner.describeContents();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(mInner, flags);
        }

        /**
         * Gets the underlying framework {@link android.media.session.MediaSession.Token} object.
         * <p>
         * This method is only supported on API 21+.
         * </p>
         *
         * @return The underlying {@link android.media.session.MediaSession.Token} object,
         * or null if none.
         */
        public Object getToken() {
            return mInner;
        }

        public static final Parcelable.Creator<Token> CREATOR
                = new Parcelable.Creator<Token>() {
            @Override
            public Token createFromParcel(Parcel in) {
                return new Token(in.readParcelable(null));
            }

            @Override
            public Token[] newArray(int size) {
                return new Token[size];
            }
        };
    }

    interface MediaSessionImpl {
        void addCallback(Callback callback, Handler handler);
        void removeCallback(Callback callback);
        void setFlags(int flags);
        void setPlaybackToLocal(int stream);
        void setPlaybackToRemote(VolumeProviderCompat volumeProvider);
        void setActive(boolean active);
        boolean isActive();
        void sendSessionEvent(String event, Bundle extras);
        void release();
        Token getSessionToken();
        void addTransportControlsCallback(TransportControlsCallback callback, Handler handler);
        void removeTransportControlsCallback(TransportControlsCallback callback);
        void setPlaybackState(PlaybackStateCompat state);
        void setMetadata(MediaMetadataCompat metadata);
        Object getMediaSession();
    }

    // TODO: compatibility implementation
    static class MediaSessionImplBase implements MediaSessionImpl {
        @Override
        public void addCallback(Callback callback, Handler handler) {
        }

        @Override
        public void removeCallback(Callback callback) {
        }

        @Override
        public void setFlags(int flags) {
        }

        @Override
        public void setPlaybackToLocal(int stream) {
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
        }

        @Override
        public void setActive(boolean active) {
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
        }

        @Override
        public void release() {
        }

        @Override
        public Token getSessionToken() {
            return null;
        }

        @Override
        public void addTransportControlsCallback(TransportControlsCallback callback,
                Handler handler) {
        }

        @Override
        public void removeTransportControlsCallback(TransportControlsCallback callback) {
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
        }

        @Override
        public Object getMediaSession() {
            return null;
        }
    }

    static class MediaSessionImplApi21 implements MediaSessionImpl {
        private final Object mSessionObj;
        private final Token mToken;

        public MediaSessionImplApi21(Context context, String tag) {
            mSessionObj = MediaSessionCompatApi21.createSession(context, tag);
            mToken = new Token(MediaSessionCompatApi21.getSessionToken(mSessionObj));
        }

        @Override
        public void addCallback(Callback callback, Handler handler) {
            MediaSessionCompatApi21.addCallback(mSessionObj, callback.mCallbackObj, handler);
        }

        @Override
        public void removeCallback(Callback callback) {
            MediaSessionCompatApi21.removeCallback(mSessionObj, callback.mCallbackObj);
        }

        @Override
        public void setFlags(int flags) {
            MediaSessionCompatApi21.setFlags(mSessionObj, flags);
        }

        @Override
        public void setPlaybackToLocal(int stream) {
            MediaSessionCompatApi21.setPlaybackToLocal(mSessionObj, stream);
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            MediaSessionCompatApi21.setPlaybackToRemote(mSessionObj,
                    volumeProvider.getVolumeProvider());
        }

        @Override
        public void setActive(boolean active) {
            MediaSessionCompatApi21.setActive(mSessionObj, active);
        }

        @Override
        public boolean isActive() {
            return MediaSessionCompatApi21.isActive(mSessionObj);
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
            MediaSessionCompatApi21.sendSessionEvent(mSessionObj, event, extras);
        }

        @Override
        public void release() {
            MediaSessionCompatApi21.release(mSessionObj);
        }

        @Override
        public Token getSessionToken() {
            return mToken;
        }

        @Override
        public void addTransportControlsCallback(TransportControlsCallback callback,
                Handler handler) {
            MediaSessionCompatApi21.addTransportControlsCallback(
                    mSessionObj, callback.mCallbackObj, handler);
        }

        @Override
        public void removeTransportControlsCallback(TransportControlsCallback callback) {
            MediaSessionCompatApi21.removeTransportControlsCallback(
                    mSessionObj, callback.mCallbackObj);
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            MediaSessionCompatApi21.setPlaybackState(mSessionObj, state.getPlaybackState());
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            MediaSessionCompatApi21.setMetadata(mSessionObj, metadata.getMediaMetadata());
        }

        @Override
        public Object getMediaSession() {
            return mSessionObj;
        }
    }
}
