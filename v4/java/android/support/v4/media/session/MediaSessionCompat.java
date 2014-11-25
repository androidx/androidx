
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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

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
 * set with {@link #setCallback(Callback)}.
 * <p>
 * When an app is finished performing playback it must call {@link #release()}
 * to clean up the session and notify any controllers.
 * <p>
 * MediaSessionCompat objects are not thread safe and all calls should be made
 * from the same thread.
 * <p>
 * This is a helper for accessing features in
 * {@link android.media.session.MediaSession} introduced after API level 4 in a
 * backwards compatible fashion.
 */
public class MediaSessionCompat {
    private final MediaSessionImpl mImpl;
    private final MediaControllerCompat mController;
    private final ArrayList<OnActiveChangeListener>
            mActiveListeners = new ArrayList<OnActiveChangeListener>();

    /**
     * Set this flag on the session to indicate that it can handle media button
     * events.
     */
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1 << 0;

    /**
     * Set this flag on the session to indicate that it handles transport
     * control commands through its {@link Callback}.
     */
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 1 << 1;

    /**
     * Creates a new session.
     *
     * @param context The context.
     * @param tag A short name for debugging purposes.
     * @param mediaButtonEventReceiver The component name for your receiver.
     *            This must be non-null to support platform versions earlier
     *            than {@link android.os.Build.VERSION_CODES#LOLLIPOP}.
     * @param mbrIntent The PendingIntent for your receiver component that
     *            handles media button events. This is optional and will be used
     *            on {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2} and
     *            later instead of the component name.
     */
    public MediaSessionCompat(Context context, String tag, ComponentName mediaButtonEventReceiver,
            PendingIntent mbrIntent) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag must not be null or empty");
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaSessionImplApi21(context, tag);
            mImpl.setMediaButtonReceiver(mbrIntent);
        } else {
            mImpl = new MediaSessionImplBase(context, mediaButtonEventReceiver, mbrIntent);
        }
        mController = new MediaControllerCompat(context, this);
    }

    private MediaSessionCompat(Context context, MediaSessionImpl impl) {
        mImpl = impl;
        mController = new MediaControllerCompat(context, this);
    }

    /**
     * Add a callback to receive updates on for the MediaSession. This includes
     * media button and volume events. The caller's thread will be used to post
     * events.
     *
     * @param callback The callback object
     */
    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    /**
     * Set the callback to receive updates for the MediaSession. This includes
     * media button and volume events. Set the callback to null to stop
     * receiving events.
     *
     * @param callback The callback to receive updates on.
     * @param handler The handler that events should be posted on.
     */
    public void setCallback(Callback callback, Handler handler) {
        mImpl.setCallback(callback, handler != null ? handler : new Handler());
    }

    /**
     * Set an intent for launching UI for this Session. This can be used as a
     * quick link to an ongoing media screen. The intent should be for an
     * activity that may be started using
     * {@link Activity#startActivity(Intent)}.
     *
     * @param pi The intent to launch to show UI for this Session.
     */
    public void setSessionActivity(PendingIntent pi) {
        mImpl.setSessionActivity(pi);
    }

    /**
     * Set a pending intent for your media button receiver to allow restarting
     * playback after the session has been stopped. If your app is started in
     * this way an {@link Intent#ACTION_MEDIA_BUTTON} intent will be sent via
     * the pending intent.
     * <p>
     * This method will only work on
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and later. Earlier
     * platform versions must include the media button receiver in the
     * constructor.
     *
     * @param mbr The {@link PendingIntent} to send the media button event to.
     */
    public void setMediaButtonReceiver(PendingIntent mbr) {
        mImpl.setMediaButtonReceiver(mbr);
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
     * <p>
     * On platforms earlier than
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP},
     * {@link #setMediaButtonReceiver(PendingIntent)} must be called before
     * setting this to true.
     *
     * @param active Whether this session is active or not.
     */
    public void setActive(boolean active) {
        mImpl.setActive(active);
        for (OnActiveChangeListener listener : mActiveListeners) {
            listener.onActiveChanged();
        }
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
     * Get a controller for this session. This is a convenience method to avoid
     * having to cache your own controller in process.
     *
     * @return A controller for this session.
     */
    public MediaControllerCompat getController() {
        return mController;
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
     * Update the list of items in the play queue. It is an ordered list and
     * should contain the current item, and previous or upcoming items if they
     * exist. Specify null if there is no current play queue.
     * <p>
     * The queue should be of reasonable size. If the play queue is unbounded
     * within your app, it is better to send a reasonable amount in a sliding
     * window instead.
     *
     * @param queue A list of items in the play queue.
     */
    public void setQueue(List<QueueItem> queue) {
        mImpl.setQueue(queue);
    }

    /**
     * Set the title of the play queue. The UI should display this title along
     * with the play queue itself. e.g. "Play Queue", "Now Playing", or an album
     * name.
     *
     * @param title The title of the play queue.
     */
    public void setQueueTitle(CharSequence title) {
        mImpl.setQueueTitle(title);
    }

    /**
     * Set the style of rating used by this session. Apps trying to set the
     * rating should use this style. Must be one of the following:
     * <ul>
     * <li>{@link RatingCompat#RATING_NONE}</li>
     * <li>{@link RatingCompat#RATING_3_STARS}</li>
     * <li>{@link RatingCompat#RATING_4_STARS}</li>
     * <li>{@link RatingCompat#RATING_5_STARS}</li>
     * <li>{@link RatingCompat#RATING_HEART}</li>
     * <li>{@link RatingCompat#RATING_PERCENTAGE}</li>
     * <li>{@link RatingCompat#RATING_THUMB_UP_DOWN}</li>
     * </ul>
     */
    public void setRatingType(int type) {
        mImpl.setRatingType(type);
    }

    /**
     * Set some extras that can be associated with the
     * {@link MediaSessionCompat}. No assumptions should be made as to how a
     * {@link MediaControllerCompat} will handle these extras. Keys should be
     * fully qualified (e.g. com.example.MY_EXTRA) to avoid conflicts.
     *
     * @param extras The extras associated with the session.
     */
    public void setExtras(Bundle extras) {
        mImpl.setExtras(extras);
    }

    /**
     * Gets the underlying framework {@link android.media.session.MediaSession}
     * object.
     * <p>
     * This method is only supported on API 21+.
     * </p>
     *
     * @return The underlying {@link android.media.session.MediaSession} object,
     *         or null if none.
     */
    public Object getMediaSession() {
        return mImpl.getMediaSession();
    }

    /**
     * Gets the underlying framework {@link android.media.RemoteControlClient}
     * object.
     * <p>
     * This method is only supported on APIs 14-20. On API 21+
     * {@link #getMediaSession()} should be used instead.
     *
     * @return The underlying {@link android.media.RemoteControlClient} object,
     *         or null if none.
     */
    public Object getRemoteControlClient() {
        return mImpl.getRemoteControlClient();
    }

    /**
     * Adds a listener to be notified when the active status of this session
     * changes. This is primarily used by the support library and should not be
     * needed by apps.
     *
     * @param listener The listener to add.
     */
    public void addOnActiveChangeListener(OnActiveChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }
        mActiveListeners.add(listener);
    }

    /**
     * Stops the listener from being notified when the active status of this
     * session changes.
     *
     * @param listener The listener to remove.
     */
    public void removeOnActiveChangeListener(OnActiveChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener may not be null");
        }
        mActiveListeners.remove(listener);
    }

    /**
     * Obtain a compat wrapper for an existing MediaSession.
     *
     * @param mediaSession The {@link android.media.session.MediaSession} to
     *            wrap.
     * @return A compat wrapper for the provided session.
     */
    public static MediaSessionCompat obtain(Context context, Object mediaSession) {
        return new MediaSessionCompat(context, new MediaSessionImplApi21(mediaSession));
    }

    /**
     * Receives transport controls, media buttons, and commands from controllers
     * and the system. The callback may be set using {@link #setCallback}.
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

        /**
         * Override to handle media button events.
         *
         * @param mediaButtonEvent The media button event intent.
         * @return True if the event was handled, false otherwise.
         */
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return false;
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

        private class StubApi21 implements MediaSessionCompatApi21.Callback {

            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                Callback.this.onCommand(command, extras, cb);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                return Callback.this.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onPlay() {
                Callback.this.onPlay();
            }

            @Override
            public void onPause() {
                Callback.this.onPause();
            }

            @Override
            public void onSkipToNext() {
                Callback.this.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                Callback.this.onSkipToPrevious();
            }

            @Override
            public void onFastForward() {
                Callback.this.onFastForward();
            }

            @Override
            public void onRewind() {
                Callback.this.onRewind();
            }

            @Override
            public void onStop() {
                Callback.this.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                Callback.this.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Object ratingObj) {
                Callback.this.onSetRating(RatingCompat.fromRating(ratingObj));
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

        /**
         * Creates a compat Token from a framework
         * {@link android.media.session.MediaSession.Token} object.
         * <p>
         * This method is only supported on
         * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and later.
         * </p>
         *
         * @param token The framework token object.
         * @return A compat Token for use with {@link MediaControllerCompat}.
         */
        public static Token fromToken(Object token) {
            if (token == null || android.os.Build.VERSION.SDK_INT < 21) {
                return null;
            }
            return new Token((Parcelable) MediaSessionCompatApi21.verifyToken(token));
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

    /**
     * A single item that is part of the play queue. It contains a description
     * of the item and its id in the queue.
     */
    public static final class QueueItem implements Parcelable {
        /**
         * This id is reserved. No items can be explicitly asigned this id.
         */
        public static final int UNKNOWN_ID = -1;

        private final MediaDescriptionCompat mDescription;
        private final long mId;

        private Object mItem;

        /**
         * Create a new {@link MediaSessionCompat.QueueItem}.
         *
         * @param description The {@link MediaDescriptionCompat} for this item.
         * @param id An identifier for this item. It must be unique within the
         *            play queue and cannot be {@link #UNKNOWN_ID}.
         */
        public QueueItem(MediaDescriptionCompat description, long id) {
            this(null, description, id);
        }

        private QueueItem(Object queueItem, MediaDescriptionCompat description, long id) {
            if (description == null) {
                throw new IllegalArgumentException("Description cannot be null.");
            }
            if (id == UNKNOWN_ID) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            mDescription = description;
            mId = id;
            mItem = queueItem;
        }

        private QueueItem(Parcel in) {
            mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            mId = in.readLong();
        }

        /**
         * Get the description for this item.
         */
        public MediaDescriptionCompat getDescription() {
            return mDescription;
        }

        /**
         * Get the queue id for this item.
         */
        public long getQueueId() {
            return mId;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            mDescription.writeToParcel(dest, flags);
            dest.writeLong(mId);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Get the underlying
         * {@link android.media.session.MediaSession.QueueItem}.
         * <p>
         * On builds before {@link android.os.Build.VERSION_CODES#LOLLIPOP} null
         * is returned.
         *
         * @return The underlying
         *         {@link android.media.session.MediaSession.QueueItem} or null.
         */
        public Object getQueueItem() {
            if (mItem != null || android.os.Build.VERSION.SDK_INT < 21) {
                return mItem;
            }
            mItem = MediaSessionCompatApi21.QueueItem.createItem(mDescription.getMediaDescription(),
                    mId);
            return mItem;
        }

        /**
         * Obtain a compat wrapper for an existing QueueItem.
         *
         * @param queueItem The {@link android.media.session.MediaSession.QueueItem} to
         *            wrap.
         * @return A compat wrapper for the provided item.
         */
        public static QueueItem obtain(Object queueItem) {
            Object descriptionObj = MediaSessionCompatApi21.QueueItem.getDescription(queueItem);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(
                    descriptionObj);
            long id = MediaSessionCompatApi21.QueueItem.getQueueId(queueItem);
            return new QueueItem(queueItem, description, id);
        }

        public static final Creator<MediaSessionCompat.QueueItem>
                CREATOR = new Creator<MediaSessionCompat.QueueItem>() {

                        @Override
                    public MediaSessionCompat.QueueItem createFromParcel(Parcel p) {
                        return new MediaSessionCompat.QueueItem(p);
                    }

                        @Override
                    public MediaSessionCompat.QueueItem[] newArray(int size) {
                        return new MediaSessionCompat.QueueItem[size];
                    }
                };

        @Override
        public String toString() {
            return "MediaSession.QueueItem {" +
                    "Description=" + mDescription +
                    ", Id=" + mId + " }";
        }
    }

    public interface OnActiveChangeListener {
        void onActiveChanged();
    }

    interface MediaSessionImpl {
        void setCallback(Callback callback, Handler handler);
        void setFlags(int flags);
        void setPlaybackToLocal(int stream);
        void setPlaybackToRemote(VolumeProviderCompat volumeProvider);
        void setActive(boolean active);
        boolean isActive();
        void sendSessionEvent(String event, Bundle extras);
        void release();
        Token getSessionToken();
        void setPlaybackState(PlaybackStateCompat state);
        void setMetadata(MediaMetadataCompat metadata);

        void setSessionActivity(PendingIntent pi);

        void setMediaButtonReceiver(PendingIntent mbr);
        void setQueue(List<QueueItem> queue);
        void setQueueTitle(CharSequence title);

        void setRatingType(int type);
        void setExtras(Bundle extras);

        Object getMediaSession();

        Object getRemoteControlClient();
    }

    // TODO: compatibility implementation
    static class MediaSessionImplBase implements MediaSessionImpl {
        private final Context mContext;
        private final ComponentName mComponentName;
        private final PendingIntent mMediaButtonEventReceiver;
        private final Object mRccObj;
        private Object mToken;

        private boolean mIsActive = false;
        private boolean mIsRccRegistered = false;
        private boolean mIsMbrRegistered = false;
        private Callback mCallback;

        private int mFlags;

        private MediaMetadataCompat mMetadata;
        private PlaybackStateCompat mState;

        public MediaSessionImplBase(Context context, ComponentName mbrComponent,
                PendingIntent mbr) {
            if (mbrComponent == null) {
                throw new IllegalArgumentException(
                        "MediaButtonReceiver component may not be null.");
            }
            if (mbr == null) {
                // construct a PendingIntent for the media button
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                // the associated intent will be handled by the component being
                // registered
                mediaButtonIntent.setComponent(mbrComponent);
                mbr = PendingIntent.getBroadcast(context,
                        0/* requestCode, ignored */, mediaButtonIntent, 0/* flags */);
            }
            mContext = context;
            mComponentName = mbrComponent;
            mMediaButtonEventReceiver = mbr;
            if (android.os.Build.VERSION.SDK_INT >= 14) {
                mRccObj = MediaSessionCompatApi14.createRemoteControlClient(mbr);
            } else {
                mRccObj = null;
            }
        }

        @Override
        public void setCallback(final Callback callback, Handler handler) {
            if (callback == mCallback) {
                return;
            }
            if (callback == null || android.os.Build.VERSION.SDK_INT < 18) {
                // There's nothing to register on API < 18 since media buttons
                // all go through the media button receiver
                if (android.os.Build.VERSION.SDK_INT >= 18) {
                    MediaSessionCompatApi18.setOnPlaybackPositionUpdateListener(mRccObj, null);
                }
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    MediaSessionCompatApi19.setOnMetadataUpdateListener(mRccObj, null);
                }
            } else {
                if (handler == null) {
                    handler = new Handler();
                }
                MediaSessionCompatApi14.Callback cb14 = new MediaSessionCompatApi14.Callback() {
                    @Override
                    public void onStop() {
                        callback.onStop();
                    }

                    @Override
                    public void onSkipToPrevious() {
                        callback.onSkipToPrevious();
                    }

                    @Override
                    public void onSkipToNext() {
                        callback.onSkipToNext();
                    }

                    @Override
                    public void onSetRating(Object ratingObj) {
                        callback.onSetRating(RatingCompat.fromRating(ratingObj));
                    }

                    @Override
                    public void onSeekTo(long pos) {
                        callback.onSeekTo(pos);
                    }

                    @Override
                    public void onRewind() {
                        callback.onRewind();
                    }

                    @Override
                    public void onPlay() {
                        callback.onPlay();
                    }

                    @Override
                    public void onPause() {
                        callback.onPause();
                    }

                    @Override
                    public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                        return callback.onMediaButtonEvent(mediaButtonIntent);
                    }

                    @Override
                    public void onFastForward() {
                        callback.onFastForward();
                    }

                    @Override
                    public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                        callback.onCommand(command, extras, cb);
                    }
                };
                if (android.os.Build.VERSION.SDK_INT >= 18) {
                    Object onPositionUpdateObj = MediaSessionCompatApi18
                            .createPlaybackPositionUpdateListener(cb14);
                    MediaSessionCompatApi18.setOnPlaybackPositionUpdateListener(mRccObj,
                            onPositionUpdateObj);
                }
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    Object onMetadataUpdateObj = MediaSessionCompatApi19
                            .createMetadataUpdateListener(cb14);
                    MediaSessionCompatApi19.setOnMetadataUpdateListener(mRccObj,
                            onMetadataUpdateObj);
                }
            }
            mCallback = callback;
        }

        @Override
        public void setFlags(int flags) {
            mFlags = flags;
            update();
        }

        @Override
        public void setPlaybackToLocal(int stream) {
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
        }

        @Override
        public void setActive(boolean active) {
            if (active == mIsActive) {
                return;
            }
            mIsActive = active;
            if (update()) {
                setMetadata(mMetadata);
                setPlaybackState(mState);
            }
        }

        @Override
        public boolean isActive() {
            return mIsActive;
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
        }

        @Override
        public void release() {
            mIsActive = false;
            update();
        }

        @Override
        public Token getSessionToken() {
            return null;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            mState = state;
            if (!mIsActive) {
                // Don't set the state until after the RCC is registered
                return;
            }

            if (state == null) {
                if (android.os.Build.VERSION.SDK_INT >= 14) {
                    MediaSessionCompatApi14.setState(mRccObj, PlaybackStateCompat.STATE_NONE);
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= 18) {
                    MediaSessionCompatApi18.setState(mRccObj, state.getState(), state.getPosition(),
                            state.getPlaybackSpeed(), state.getLastPositionUpdateTime());
                } else if (android.os.Build.VERSION.SDK_INT >= 14) {
                    MediaSessionCompatApi14.setState(mRccObj, state.getState());
                }
            }
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            if (!mIsActive) {
                // Don't set metadata until after the rcc has been registered
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                boolean canRate = mState != null
                        && (mState.getActions() & PlaybackStateCompat.ACTION_SET_RATING) != 0;
                MediaSessionCompatApi19.setMetadata(mRccObj,
                        metadata == null ? null : metadata.getBundle(), canRate);
            } else if (android.os.Build.VERSION.SDK_INT >= 14) {
                MediaSessionCompatApi14.setMetadata(mRccObj,
                        metadata == null ? null : metadata.getBundle());
            }
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            // Do nothing, changing this is not supported before API 21.
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
        }

        @Override
        public void setQueueTitle(CharSequence title) {
        }

        @Override
        public Object getMediaSession() {
            return null;
        }

        @Override
        public Object getRemoteControlClient() {
            return mRccObj;
        }

        @Override
        public void setRatingType(int type) {
        }

        @Override
        public void setExtras(Bundle extras) {
        }

        // Registers/unregisters the RCC and MediaButtonEventReceiver as needed.
        private boolean update() {
            boolean registeredRcc = false;
            if (mIsActive) {
                // On API 8+ register a MBR if it's supported, unregister it
                // if support was removed.
                if (android.os.Build.VERSION.SDK_INT >= 8) {
                    if (!mIsMbrRegistered && (mFlags & FLAG_HANDLES_MEDIA_BUTTONS) != 0) {
                        if (android.os.Build.VERSION.SDK_INT >= 18) {
                            MediaSessionCompatApi18.registerMediaButtonEventReceiver(mContext,
                                    mMediaButtonEventReceiver);
                        } else {
                            MediaSessionCompatApi8.registerMediaButtonEventReceiver(mContext,
                                    mComponentName);
                        }
                        mIsMbrRegistered = true;
                    } else if (mIsMbrRegistered && (mFlags & FLAG_HANDLES_MEDIA_BUTTONS) == 0) {
                        if (android.os.Build.VERSION.SDK_INT >= 18) {
                            MediaSessionCompatApi18.unregisterMediaButtonEventReceiver(mContext,
                                    mMediaButtonEventReceiver);
                        } else {
                            MediaSessionCompatApi8.unregisterMediaButtonEventReceiver(mContext,
                                    mComponentName);
                        }
                        mIsMbrRegistered = false;
                    }
                }
                // On API 14+ register a RCC if it's supported, unregister it if
                // not.
                if (android.os.Build.VERSION.SDK_INT >= 14) {
                    if (!mIsRccRegistered && (mFlags & FLAG_HANDLES_TRANSPORT_CONTROLS) != 0) {
                        MediaSessionCompatApi14.registerRemoteControlClient(mContext, mRccObj);
                        mIsRccRegistered = true;
                        registeredRcc = true;
                    } else if (mIsRccRegistered
                            && (mFlags & FLAG_HANDLES_TRANSPORT_CONTROLS) == 0) {
                        MediaSessionCompatApi14.unregisterRemoteControlClient(mContext, mRccObj);
                        mIsRccRegistered = false;
                    }
                }
            } else {
                // When inactive remove any registered components.
                if (mIsMbrRegistered) {
                    if (android.os.Build.VERSION.SDK_INT >= 18) {
                        MediaSessionCompatApi18.unregisterMediaButtonEventReceiver(mContext,
                                mMediaButtonEventReceiver);
                    } else {
                        MediaSessionCompatApi8.unregisterMediaButtonEventReceiver(mContext,
                                mComponentName);
                    }
                    mIsMbrRegistered = false;
                }
                if (mIsRccRegistered) {
                    MediaSessionCompatApi14.unregisterRemoteControlClient(mContext, mRccObj);
                    mIsRccRegistered = false;
                }
            }
            return registeredRcc;
        }
    }

    static class MediaSessionImplApi21 implements MediaSessionImpl {
        private final Object mSessionObj;
        private final Token mToken;

        private PendingIntent mMediaButtonIntent;

        public MediaSessionImplApi21(Context context, String tag) {
            mSessionObj = MediaSessionCompatApi21.createSession(context, tag);
            mToken = new Token(MediaSessionCompatApi21.getSessionToken(mSessionObj));
        }

        public MediaSessionImplApi21(Object mediaSession) {
            mSessionObj = MediaSessionCompatApi21.verifySession(mediaSession);
            mToken = new Token(MediaSessionCompatApi21.getSessionToken(mSessionObj));
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            MediaSessionCompatApi21.setCallback(mSessionObj, callback.mCallbackObj, handler);
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
        public void setPlaybackState(PlaybackStateCompat state) {
            MediaSessionCompatApi21.setPlaybackState(mSessionObj, state.getPlaybackState());
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            MediaSessionCompatApi21.setMetadata(mSessionObj, metadata.getMediaMetadata());
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            MediaSessionCompatApi21.setSessionActivity(mSessionObj, pi);
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            mMediaButtonIntent = mbr;
            MediaSessionCompatApi21.setMediaButtonReceiver(mSessionObj, mbr);
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            List<Object> queueObjs = null;
            if (queue != null) {
                queueObjs = new ArrayList<Object>();
                for (QueueItem item : queue) {
                    queueObjs.add(item.getQueueItem());
                }
            }
            MediaSessionCompatApi21.setQueue(mSessionObj, queueObjs);
        }

        @Override
        public void setQueueTitle(CharSequence title) {
            MediaSessionCompatApi21.setQueueTitle(mSessionObj, title);
        }

        @Override
        public void setRatingType(int type) {
            if (android.os.Build.VERSION.SDK_INT < 22) {
                // TODO figure out 21 implementation
            } else {
                MediaSessionCompatApi22.setRatingType(mSessionObj, type);
            }
        }

        @Override
        public void setExtras(Bundle extras) {
            MediaSessionCompatApi21.setExtras(mSessionObj, extras);
        }

        @Override
        public Object getMediaSession() {
            return mSessionObj;
        }

        @Override
        public Object getRemoteControlClient() {
            return null;
        }
    }
}
