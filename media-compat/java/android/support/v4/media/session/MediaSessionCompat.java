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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.Rating;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
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
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about building your media application, read the
 * <a href="{@docRoot}guide/topics/media-apps/index.html">Media Apps</a> developer guide.</p>
 * </div>
 */
public class MediaSessionCompat {
    static final String TAG = "MediaSessionCompat";

    private final MediaSessionImpl mImpl;
    private final MediaControllerCompat mController;
    private final ArrayList<OnActiveChangeListener> mActiveListeners = new ArrayList<>();

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef(flag=true, value={
            FLAG_HANDLES_MEDIA_BUTTONS,
            FLAG_HANDLES_TRANSPORT_CONTROLS,
            FLAG_HANDLES_QUEUE_COMMANDS })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SessionFlags {}

    /**
     * Sets this flag on the session to indicate that it can handle media button
     * events.
     */
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1 << 0;

    /**
     * Sets this flag on the session to indicate that it handles transport
     * control commands through its {@link Callback}.
     */
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 1 << 1;

    /**
     * Sets this flag on the session to indicate that it handles queue
     * management commands through its {@link Callback}.
     */
    public static final int FLAG_HANDLES_QUEUE_COMMANDS = 1 << 2;

    /**
     * Predefined custom action to flag the media that is currently playing as inappropriate.
     *
     * @see Callback#onCustomAction
     */
    public static final String ACTION_FLAG_AS_INAPPROPRIATE =
            "android.support.v4.media.session.action.FLAG_AS_INAPPROPRIATE";

    /**
     * Predefined custom action to skip the advertisement that is currently playing.
     *
     * @see Callback#onCustomAction
     */
    public static final String ACTION_SKIP_AD = "android.support.v4.media.session.action.SKIP_AD";

    /**
     * Predefined custom action to follow an artist, album, or playlist. The extra bundle must have
     * {@link #ARGUMENT_MEDIA_ATTRIBUTE} to indicate the type of the follow action. The
     * bundle can also have an optional string argument,
     * {@link #ARGUMENT_MEDIA_ATTRIBUTE_VALUE}, to specify the target to follow (e.g., the
     * name of the artist to follow). If this argument is omitted, the currently playing media will
     * be the target of the action. Thus, the session must perform the follow action with the
     * current metadata. If there's no specified attribute in the current metadata, the controller
     * must not omit this argument.
     *
     * @see #ARGUMENT_MEDIA_ATTRIBUTE
     * @see #ARGUMENT_MEDIA_ATTRIBUTE_VALUE
     * @see Callback#onCustomAction
     */
    public static final String ACTION_FOLLOW = "android.support.v4.media.session.action.FOLLOW";

    /**
     * Predefined custom action to unfollow an artist, album, or playlist. The extra bundle must
     * have {@link #ARGUMENT_MEDIA_ATTRIBUTE} to indicate the type of the unfollow action.
     * The bundle can also have an optional string argument,
     * {@link #ARGUMENT_MEDIA_ATTRIBUTE_VALUE}, to specify the target to unfollow (e.g., the
     * name of the artist to unfollow). If this argument is omitted, the currently playing media
     * will be the target of the action. Thus, the session must perform the unfollow action with the
     * current metadata. If there's no specified attribute in the current metadata, the controller
     * must not omit this argument.
     *
     * @see #ARGUMENT_MEDIA_ATTRIBUTE
     * @see #ARGUMENT_MEDIA_ATTRIBUTE_VALUE
     * @see Callback#onCustomAction
     */
    public static final String ACTION_UNFOLLOW = "android.support.v4.media.session.action.UNFOLLOW";

    /**
     * Argument to indicate the media attribute. It should be one of the following:
     * <ul>
     * <li>{@link #MEDIA_ATTRIBUTE_ARTIST}</li>
     * <li>{@link #MEDIA_ATTRIBUTE_PLAYLIST}</li>
     * <li>{@link #MEDIA_ATTRIBUTE_ALBUM}</li>
     * </ul>
     */
    public static final String ARGUMENT_MEDIA_ATTRIBUTE =
            "android.support.v4.media.session.ARGUMENT_MEDIA_ATTRIBUTE";

    /**
     * String argument to indicate the value of the media attribute (e.g., the name of the artist).
     */
    public static final String ARGUMENT_MEDIA_ATTRIBUTE_VALUE =
            "android.support.v4.media.session.ARGUMENT_MEDIA_ATTRIBUTE_VALUE";

    /**
     * The value of {@link #ARGUMENT_MEDIA_ATTRIBUTE} indicating the artist.
     *
     * @see #ARGUMENT_MEDIA_ATTRIBUTE
     */
    public static final int MEDIA_ATTRIBUTE_ARTIST = 0;

    /**
     * The value of {@link #ARGUMENT_MEDIA_ATTRIBUTE} indicating the album.
     *
     * @see #ARGUMENT_MEDIA_ATTRIBUTE
     */
    public static final int MEDIA_ATTRIBUTE_ALBUM = 1;

    /**
     * The value of {@link #ARGUMENT_MEDIA_ATTRIBUTE} indicating the playlist.
     *
     * @see #ARGUMENT_MEDIA_ATTRIBUTE
     */
    public static final int MEDIA_ATTRIBUTE_PLAYLIST = 2;

    /**
     * Custom action to invoke playFromUri() for the forward compatibility.
     */
    static final String ACTION_PLAY_FROM_URI =
            "android.support.v4.media.session.action.PLAY_FROM_URI";

    /**
     * Custom action to invoke prepare() for the forward compatibility.
     */
    static final String ACTION_PREPARE = "android.support.v4.media.session.action.PREPARE";

    /**
     * Custom action to invoke prepareFromMediaId() for the forward compatibility.
     */
    static final String ACTION_PREPARE_FROM_MEDIA_ID =
            "android.support.v4.media.session.action.PREPARE_FROM_MEDIA_ID";

    /**
     * Custom action to invoke prepareFromSearch() for the forward compatibility.
     */
    static final String ACTION_PREPARE_FROM_SEARCH =
            "android.support.v4.media.session.action.PREPARE_FROM_SEARCH";

    /**
     * Custom action to invoke prepareFromUri() for the forward compatibility.
     */
    static final String ACTION_PREPARE_FROM_URI =
            "android.support.v4.media.session.action.PREPARE_FROM_URI";

    /**
     * Custom action to invoke setCaptioningEnabled() for the forward compatibility.
     */
    static final String ACTION_SET_CAPTIONING_ENABLED =
            "android.support.v4.media.session.action.SET_CAPTIONING_ENABLED";

    /**
     * Custom action to invoke setRepeatMode() for the forward compatibility.
     */
    static final String ACTION_SET_REPEAT_MODE =
            "android.support.v4.media.session.action.SET_REPEAT_MODE";

    /**
     * Custom action to invoke setShuffleModeEnabled() for the forward compatibility.
     */
    static final String ACTION_SET_SHUFFLE_MODE_ENABLED =
            "android.support.v4.media.session.action.SET_SHUFFLE_MODE_ENABLED";

    /**
     * Custom action to invoke setShuffleMode() for the forward compatibility.
     */
    static final String ACTION_SET_SHUFFLE_MODE =
            "android.support.v4.media.session.action.SET_SHUFFLE_MODE";

    /**
     * Custom action to invoke setRating() with extra fields.
     */
    static final String ACTION_SET_RATING =
            "android.support.v4.media.session.action.SET_RATING";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_MEDIA_ID} indicating media id to play.
     */
    static final String ACTION_ARGUMENT_MEDIA_ID =
            "android.support.v4.media.session.action.ARGUMENT_MEDIA_ID";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_SEARCH} indicating search query.
     */
    static final String ACTION_ARGUMENT_QUERY =
            "android.support.v4.media.session.action.ARGUMENT_QUERY";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_URI} and {@link #ACTION_PLAY_FROM_URI}
     * indicating URI to play.
     */
    static final String ACTION_ARGUMENT_URI =
            "android.support.v4.media.session.action.ARGUMENT_URI";

    /**
     * Argument for use with {@link #ACTION_SET_RATING} indicating the rate to be set.
     */
    static final String ACTION_ARGUMENT_RATING =
            "android.support.v4.media.session.action.ARGUMENT_RATING";

    /**
     * Argument for use with various actions indicating extra bundle.
     */
    static final String ACTION_ARGUMENT_EXTRAS =
            "android.support.v4.media.session.action.ARGUMENT_EXTRAS";

    /**
     * Argument for use with {@link #ACTION_SET_CAPTIONING_ENABLED} indicating whether captioning is
     * enabled.
     */
    static final String ACTION_ARGUMENT_CAPTIONING_ENABLED =
            "android.support.v4.media.session.action.ARGUMENT_CAPTIONING_ENABLED";

    /**
     * Argument for use with {@link #ACTION_SET_REPEAT_MODE} indicating repeat mode.
     */
    static final String ACTION_ARGUMENT_REPEAT_MODE =
            "android.support.v4.media.session.action.ARGUMENT_REPEAT_MODE";

    /**
     * Argument for use with {@link #ACTION_SET_SHUFFLE_MODE_ENABLED} indicating that shuffle mode
     * is enabled.
     */
    static final String ACTION_ARGUMENT_SHUFFLE_MODE_ENABLED =
            "android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE_ENABLED";

    /**
     * Argument for use with {@link #ACTION_SET_SHUFFLE_MODE} indicating shuffle mode.
     */
    static final String ACTION_ARGUMENT_SHUFFLE_MODE =
            "android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE";

    static final String EXTRA_BINDER = "android.support.v4.media.session.EXTRA_BINDER";

    // Maximum size of the bitmap in dp.
    private static final int MAX_BITMAP_SIZE_IN_DP = 320;

    // Maximum size of the bitmap in px. It shouldn't be changed.
    static int sMaxBitmapSize;

    /**
     * Creates a new session. You must call {@link #release()} when finished with the session.
     * <p>
     * The session will automatically be registered with the system but will not be published
     * until {@link #setActive(boolean) setActive(true)} is called.
     * </p><p>
     * For API 20 or earlier, note that a media button receiver is required for handling
     * {@link Intent#ACTION_MEDIA_BUTTON}. This constructor will attempt to find an appropriate
     * {@link BroadcastReceiver} from your manifest. See {@link MediaButtonReceiver} for more
     * details.
     * </p>
     * @param context The context to use to create the session.
     * @param tag A short name for debugging purposes.
     */
    public MediaSessionCompat(Context context, String tag) {
        this(context, tag, null, null);
    }

    /**
     * Creates a new session with a specified media button receiver (a component name and/or
     * a pending intent). You must call {@link #release()} when finished with the session.
     * <p>
     * The session will automatically be registered with the system but will not be published
     * until {@link #setActive(boolean) setActive(true)} is called.
     * </p><p>
     * For API 20 or earlier, note that a media button receiver is required for handling
     * {@link Intent#ACTION_MEDIA_BUTTON}. This constructor will attempt to find an appropriate
     * {@link BroadcastReceiver} from your manifest if it's not specified. See
     * {@link MediaButtonReceiver} for more details.
     * </p>
     * @param context The context to use to create the session.
     * @param tag A short name for debugging purposes.
     * @param mbrComponent The component name for your media button receiver.
     * @param mbrIntent The PendingIntent for your receiver component that handles
     *            media button events. This is optional and will be used on between
     *            {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2} and
     *            {@link android.os.Build.VERSION_CODES#KITKAT_WATCH} instead of the
     *            component name.
     */
    public MediaSessionCompat(Context context, String tag, ComponentName mbrComponent,
            PendingIntent mbrIntent) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("tag must not be null or empty");
        }

        if (mbrComponent == null) {
            mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(context);
            if (mbrComponent == null) {
                Log.w(TAG, "Couldn't find a unique registered media button receiver in the "
                        + "given context.");
            }
        }
        if (mbrComponent != null && mbrIntent == null) {
            // construct a PendingIntent for the media button
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            // the associated intent will be handled by the component being registered
            mediaButtonIntent.setComponent(mbrComponent);
            mbrIntent = PendingIntent.getBroadcast(context,
                    0/* requestCode, ignored */, mediaButtonIntent, 0/* flags */);
        }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaSessionImplApi21(context, tag);
            // Set default callback to respond to controllers' extra binder requests.
            setCallback(new Callback() {});
            mImpl.setMediaButtonReceiver(mbrIntent);
        } else if (android.os.Build.VERSION.SDK_INT >= 19) {
            mImpl = new MediaSessionImplApi19(context, tag, mbrComponent, mbrIntent);
        } else if (android.os.Build.VERSION.SDK_INT >= 18) {
            mImpl = new MediaSessionImplApi18(context, tag, mbrComponent, mbrIntent);
        } else {
            mImpl = new MediaSessionImplBase(context, tag, mbrComponent, mbrIntent);
        }
        mController = new MediaControllerCompat(context, this);

        if (sMaxBitmapSize == 0) {
            sMaxBitmapSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    MAX_BITMAP_SIZE_IN_DP, context.getResources().getDisplayMetrics());
        }
    }

    private MediaSessionCompat(Context context, MediaSessionImpl impl) {
        mImpl = impl;
        if (android.os.Build.VERSION.SDK_INT >= 21
                && !MediaSessionCompatApi21.hasCallback(impl.getMediaSession())) {
            // Set default callback to respond to controllers' extra binder requests.
            setCallback(new Callback() {});
        }
        mController = new MediaControllerCompat(context, this);
    }

    /**
     * Adds a callback to receive updates on for the MediaSession. This includes
     * media button and volume events. The caller's thread will be used to post
     * events.
     *
     * @param callback The callback object
     */
    public void setCallback(Callback callback) {
        setCallback(callback, null);
    }

    /**
     * Sets the callback to receive updates for the MediaSession. This includes
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
     * Sets an intent for launching UI for this Session. This can be used as a
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
     * Sets a pending intent for your media button receiver to allow restarting
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
     * Sets any flags for the session.
     *
     * @param flags The flags to set for this session.
     */
    public void setFlags(@SessionFlags int flags) {
        mImpl.setFlags(flags);
    }

    /**
     * Sets the stream this session is playing on. This will affect the system's
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
     * Configures this session to use remote volume handling. This must be called
     * to receive volume button events, otherwise the system will adjust the
     * current stream volume for this session. If {@link #setPlaybackToLocal}
     * was previously called that stream will stop receiving volume changes for
     * this session.
     * <p>
     * On platforms earlier than {@link android.os.Build.VERSION_CODES#LOLLIPOP}
     * this will only allow an app to handle volume commands sent directly to
     * the session by a {@link MediaControllerCompat}. System routing of volume
     * keys will not use the volume provider.
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
     * Sets if this session is currently active and ready to receive commands. If
     * set to false your session's controller may not be discoverable. You must
     * set the session to active before it can start receiving media button
     * events or transport commands.
     * <p>
     * On platforms earlier than
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP},
     * a media button event receiver should be set via the constructor to
     * receive media button events.
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
     * Gets the current active state of this session.
     *
     * @return True if the session is active, false otherwise.
     */
    public boolean isActive() {
        return mImpl.isActive();
    }

    /**
     * Sends a proprietary event to all MediaControllers listening to this
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
     * Retrieves a token object that can be used by apps to create a
     * {@link MediaControllerCompat} for interacting with this session. The
     * owner of the session is responsible for deciding how to distribute these
     * tokens.
     * <p>
     * On platform versions before
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} this token may only be
     * used within your app as there is no way to guarantee other apps are using
     * the same version of the support library.
     *
     * @return A token that can be used to create a media controller for this
     *         session.
     */
    public Token getSessionToken() {
        return mImpl.getSessionToken();
    }

    /**
     * Gets a controller for this session. This is a convenience method to avoid
     * having to cache your own controller in process.
     *
     * @return A controller for this session.
     */
    public MediaControllerCompat getController() {
        return mController;
    }

    /**
     * Updates the current playback state.
     *
     * @param state The current state of playback
     */
    public void setPlaybackState(PlaybackStateCompat state) {
        mImpl.setPlaybackState(state);
    }

    /**
     * Updates the current metadata. New metadata can be created using
     * {@link android.support.v4.media.MediaMetadataCompat.Builder}. This operation may take time
     * proportional to the size of the bitmap to replace large bitmaps with a scaled down copy.
     *
     * @param metadata The new metadata
     * @see android.support.v4.media.MediaMetadataCompat.Builder#putBitmap
     */
    public void setMetadata(MediaMetadataCompat metadata) {
        mImpl.setMetadata(metadata);
    }

    /**
     * Updates the list of items in the play queue. It is an ordered list and
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
     * Sets the title of the play queue. The UI should display this title along
     * with the play queue itself. e.g. "Play Queue", "Now Playing", or an album
     * name.
     *
     * @param title The title of the play queue.
     */
    public void setQueueTitle(CharSequence title) {
        mImpl.setQueueTitle(title);
    }

    /**
     * Sets the style of rating used by this session. Apps trying to set the
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
    public void setRatingType(@RatingCompat.Style int type) {
        mImpl.setRatingType(type);
    }

    /**
     * Enables/disables captioning for this session.
     *
     * @param enabled {@code true} to enable captioning, {@code false} to disable.
     */
    public void setCaptioningEnabled(boolean enabled) {
        mImpl.setCaptioningEnabled(enabled);
    }

    /**
     * Sets the repeat mode for this session.
     * <p>
     * Note that if this method is not called before, {@link MediaControllerCompat#getRepeatMode}
     * will return {@link PlaybackStateCompat#REPEAT_MODE_NONE}.
     *
     * @param repeatMode The repeat mode. Must be one of the followings:
     *            {@link PlaybackStateCompat#REPEAT_MODE_NONE},
     *            {@link PlaybackStateCompat#REPEAT_MODE_ONE},
     *            {@link PlaybackStateCompat#REPEAT_MODE_ALL},
     *            {@link PlaybackStateCompat#REPEAT_MODE_GROUP}
     */
    public void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
        mImpl.setRepeatMode(repeatMode);
    }

    /**
     * Sets the shuffle mode for this session.
     * <p>
     * Note that if this method is not called before,
     * {@link MediaControllerCompat#isShuffleModeEnabled} will return {@code false}.
     *
     * @param enabled {@code true} to enable the shuffle mode, {@code false} to disable.
     * @deprecated Use {@link #setShuffleMode} instead.
     */
    @Deprecated
    public void setShuffleModeEnabled(boolean enabled) {
        mImpl.setShuffleModeEnabled(enabled);
    }

    /**
     * Sets the shuffle mode for this session.
     * <p>
     * Note that if this method is not called before, {@link MediaControllerCompat#getShuffleMode}
     * will return {@link PlaybackStateCompat#SHUFFLE_MODE_NONE}.
     *
     * @param shuffleMode The shuffle mode. Must be one of the followings:
     *                    {@link PlaybackStateCompat#SHUFFLE_MODE_NONE},
     *                    {@link PlaybackStateCompat#SHUFFLE_MODE_ALL},
     *                    {@link PlaybackStateCompat#SHUFFLE_MODE_GROUP}
     */
    public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
        mImpl.setShuffleMode(shuffleMode);
    }

    /**
     * Sets some extras that can be associated with the
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
     * Returns the name of the package that sent the last media button, transport control, or
     * command from controllers and the system. This is only valid while in a request callback, such
     * as {@link Callback#onPlay}. This method is not available and returns null on pre-N devices.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public String getCallingPackage() {
        return mImpl.getCallingPackage();
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
     * Creates an instance from a framework {@link android.media.session.MediaSession} object.
     * <p>
     * This method is only supported on API 21+. On API 20 and below, it returns null.
     * </p>
     *
     * @param context The context to use to create the session.
     * @param mediaSession A {@link android.media.session.MediaSession} object.
     * @return An equivalent {@link MediaSessionCompat} object, or null if none.
     */
    public static MediaSessionCompat fromMediaSession(Context context, Object mediaSession) {
        if (context != null && mediaSession != null && Build.VERSION.SDK_INT >= 21) {
            return new MediaSessionCompat(context, new MediaSessionImplApi21(mediaSession));
        }
        return null;
    }

    private static PlaybackStateCompat getStateWithUpdatedPosition(
            PlaybackStateCompat state, MediaMetadataCompat metadata) {
        if (state == null || state.getPosition() == PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN) {
            return state;
        }

        if (state.getState() == PlaybackStateCompat.STATE_PLAYING
                || state.getState() == PlaybackStateCompat.STATE_FAST_FORWARDING
                || state.getState() == PlaybackStateCompat.STATE_REWINDING) {
            long updateTime = state.getLastPositionUpdateTime();
            if (updateTime > 0) {
                long currentTime = SystemClock.elapsedRealtime();
                long position = (long) (state.getPlaybackSpeed() * (currentTime - updateTime))
                        + state.getPosition();
                long duration = -1;
                if (metadata != null && metadata.containsKey(
                        MediaMetadataCompat.METADATA_KEY_DURATION)) {
                    duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                }

                if (duration >= 0 && position > duration) {
                    position = duration;
                } else if (position < 0) {
                    position = 0;
                }
                return new PlaybackStateCompat.Builder(state)
                        .setState(state.getState(), position, state.getPlaybackSpeed(), currentTime)
                        .build();
            }
        }
        return state;
    }

    /**
     * Receives transport controls, media buttons, and commands from controllers
     * and the system. The callback may be set using {@link #setCallback}.
     */
    public abstract static class Callback {
        final Object mCallbackObj;
        private WeakReference<MediaSessionImpl> mSessionImpl;
        private CallbackHandler mCallbackHandler = null;
        private boolean mMediaPlayPauseKeyPending;

        public Callback() {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                mCallbackObj = MediaSessionCompatApi24.createCallback(new StubApi24());
            } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                mCallbackObj = MediaSessionCompatApi23.createCallback(new StubApi23());
            } else if (android.os.Build.VERSION.SDK_INT >= 21) {
                mCallbackObj = MediaSessionCompatApi21.createCallback(new StubApi21());
            } else {
                mCallbackObj = null;
            }
        }

        private void setSessionImpl(MediaSessionImpl impl, Handler handler) {
            mSessionImpl = new WeakReference<MediaSessionImpl>(impl);
            if (mCallbackHandler != null) {
                mCallbackHandler.removeCallbacksAndMessages(null);
            }
            mCallbackHandler = new CallbackHandler(handler.getLooper());
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
         * <p>
         * The double tap of {@link KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE} or {@link
         * KeyEvent#KEYCODE_HEADSETHOOK} will call the {@link #onSkipToNext} by default.
         *
         * @param mediaButtonEvent The media button event intent.
         * @return True if the event was handled, false otherwise.
         */
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            MediaSessionImpl impl = mSessionImpl.get();
            if (impl == null || mCallbackHandler == null) {
                return false;
            }
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            int keyCode = keyEvent.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (keyEvent.getRepeatCount() > 0) {
                        // Consider long-press as a single tap.
                        handleMediaPlayPauseKeySingleTapIfPending();
                    } else if (mMediaPlayPauseKeyPending) {
                        mCallbackHandler.removeMessages(
                                CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
                        mMediaPlayPauseKeyPending = false;
                        PlaybackStateCompat state = impl.getPlaybackState();
                        long validActions = state == null ? 0 : state.getActions();
                        // Consider double tap as the next.
                        if ((validActions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
                            onSkipToNext();
                        }
                    } else {
                        mMediaPlayPauseKeyPending = true;
                        mCallbackHandler.sendEmptyMessageDelayed(
                                CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT,
                                ViewConfiguration.getDoubleTapTimeout());
                    }
                    return true;
                default:
                    // If another key is pressed within double tap timeout, consider the pending
                    // pending play/pause as a single tap to handle media keys in order.
                    handleMediaPlayPauseKeySingleTapIfPending();
                    break;
            }
            return false;
        }

        private void handleMediaPlayPauseKeySingleTapIfPending() {
            if (!mMediaPlayPauseKeyPending) {
                return;
            }
            mMediaPlayPauseKeyPending = false;
            mCallbackHandler.removeMessages(
                    CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
            MediaSessionImpl impl = mSessionImpl.get();
            if (impl == null) {
                return;
            }
            PlaybackStateCompat state = impl.getPlaybackState();
            long validActions = state == null ? 0 : state.getActions();
            boolean isPlaying = state != null
                    && state.getState() == PlaybackStateCompat.STATE_PLAYING;
            boolean canPlay = (validActions & (PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_PLAY)) != 0;
            boolean canPause = (validActions & (PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_PAUSE)) != 0;
            if (isPlaying && canPause) {
                onPause();
            } else if (!isPlaying && canPlay) {
                onPlay();
            }
        }

        /**
         * Override to handle requests to prepare playback. Override {@link #onPlay} to handle
         * requests for starting playback.
         */
        public void onPrepare() {
        }

        /**
         * Override to handle requests to prepare for playing a specific mediaId that was provided
         * by your app. Override {@link #onPlayFromMediaId} to handle requests for starting
         * playback.
         */
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
        }

        /**
         * Override to handle requests to prepare playback from a search query. An
         * empty query indicates that the app may prepare any music. The
         * implementation should attempt to make a smart choice about what to play.
         * Override {@link #onPlayFromSearch} to handle requests
         * for starting playback.
         */
        public void onPrepareFromSearch(String query, Bundle extras) {
        }

        /**
         * Override to handle requests to prepare a specific media item represented by a URI.
         * Override {@link #onPlayFromUri} to handle requests
         * for starting playback.
         */
        public void onPrepareFromUri(Uri uri, Bundle extras) {
        }

        /**
         * Override to handle requests to begin playback.
         */
        public void onPlay() {
        }

        /**
         * Override to handle requests to play a specific mediaId that was
         * provided by your app.
         */
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
        }

        /**
         * Override to handle requests to begin playback from a search query. An
         * empty query indicates that the app may play any music. The
         * implementation should attempt to make a smart choice about what to
         * play.
         */
        public void onPlayFromSearch(String query, Bundle extras) {
        }

        /**
         * Override to handle requests to play a specific media item represented by a URI.
         */
        public void onPlayFromUri(Uri uri, Bundle extras) {
        }

        /**
         * Override to handle requests to play an item with a given id from the
         * play queue.
         */
        public void onSkipToQueueItem(long id) {
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
         * @param rating The rating being set.
         */
        public void onSetRating(RatingCompat rating) {
        }

        /**
         * Override to handle the item being rated.
         *
         * @param rating The rating being set.
         * @param extras The extras can include information about the media item being rated.
         */
        public void onSetRating(RatingCompat rating, Bundle extras) {
        }

        /**
         * Override to handle requests to enable/disable captioning.
         *
         * @param enabled {@code true} to enable captioning, {@code false} to disable.
         */
        public void onSetCaptioningEnabled(boolean enabled) {
        }

        /**
         * Override to handle the setting of the repeat mode.
         * <p>
         * You should call {@link #setRepeatMode} before end of this method in order to notify
         * the change to the {@link MediaControllerCompat}, or
         * {@link MediaControllerCompat#getRepeatMode} could return an invalid value.
         *
         * @param repeatMode The repeat mode which is one of followings:
         *            {@link PlaybackStateCompat#REPEAT_MODE_NONE},
         *            {@link PlaybackStateCompat#REPEAT_MODE_ONE},
         *            {@link PlaybackStateCompat#REPEAT_MODE_ALL},
         *            {@link PlaybackStateCompat#REPEAT_MODE_GROUP}
         */
        public void onSetRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
        }

        /**
         * Override to handle the setting of the shuffle mode.
         * <p>
         * You should call {@link #setShuffleModeEnabled} before the end of this method in order to
         * notify the change to the {@link MediaControllerCompat}, or
         * {@link MediaControllerCompat#isShuffleModeEnabled} could return an invalid value.
         *
         * @param enabled true when the shuffle mode is enabled, false otherwise.
         * @deprecated Use {@link #onSetShuffleMode} instead.
         */
        @Deprecated
        public void onSetShuffleModeEnabled(boolean enabled) {
        }

        /**
         * Override to handle the setting of the shuffle mode.
         * <p>
         * You should call {@link #setShuffleMode} before the end of this method in order to
         * notify the change to the {@link MediaControllerCompat}, or
         * {@link MediaControllerCompat#getShuffleMode} could return an invalid value.
         *
         * @param shuffleMode The shuffle mode which is one of followings:
         *                    {@link PlaybackStateCompat#SHUFFLE_MODE_NONE},
         *                    {@link PlaybackStateCompat#SHUFFLE_MODE_ALL},
         *                    {@link PlaybackStateCompat#SHUFFLE_MODE_GROUP}
         */
        public void onSetShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
        }

        /**
         * Called when a {@link MediaControllerCompat} wants a
         * {@link PlaybackStateCompat.CustomAction} to be performed.
         *
         * @param action The action that was originally sent in the
         *            {@link PlaybackStateCompat.CustomAction}.
         * @param extras Optional extras specified by the
         *            {@link MediaControllerCompat}.
         * @see #ACTION_FLAG_AS_INAPPROPRIATE
         * @see #ACTION_SKIP_AD
         * @see #ACTION_FOLLOW
         * @see #ACTION_UNFOLLOW
         */
        public void onCustomAction(String action, Bundle extras) {
        }

        /**
         * Called when a {@link MediaControllerCompat} wants to add a {@link QueueItem}
         * with the given {@link MediaDescriptionCompat description} at the end of the play queue.
         *
         * @param description The {@link MediaDescriptionCompat} for creating the {@link QueueItem}
         *            to be inserted.
         */
        public void onAddQueueItem(MediaDescriptionCompat description) {
        }

        /**
         * Called when a {@link MediaControllerCompat} wants to add a {@link QueueItem}
         * with the given {@link MediaDescriptionCompat description} at the specified position
         * in the play queue.
         *
         * @param description The {@link MediaDescriptionCompat} for creating the {@link QueueItem}
         *            to be inserted.
         * @param index The index at which the created {@link QueueItem} is to be inserted.
         */
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
        }

        /**
         * Called when a {@link MediaControllerCompat} wants to remove the first occurrence of the
         * specified {@link QueueItem} with the given {@link MediaDescriptionCompat description}
         * in the play queue.
         *
         * @param description The {@link MediaDescriptionCompat} for denoting the {@link QueueItem}
         *            to be removed.
         */
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
        }

        /**
         * Called when a {@link MediaControllerCompat} wants to remove a {@link QueueItem} at the
         * specified position in the play queue.
         *
         * @param index The index of the element to be removed.
         * @deprecated {@link #onRemoveQueueItem} will be called instead.
         */
        @Deprecated
        public void onRemoveQueueItemAt(int index) {
        }

        private class CallbackHandler extends Handler {
            private static final int MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT = 1;

            CallbackHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT) {
                    handleMediaPlayPauseKeySingleTapIfPending();
                }
            }
        }

        @RequiresApi(21)
        private class StubApi21 implements MediaSessionCompatApi21.Callback {

            StubApi21() {
            }

            @Override
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                try {
                    if (command.equals(MediaControllerCompat.COMMAND_GET_EXTRA_BINDER)) {
                        MediaSessionImplApi21 impl = (MediaSessionImplApi21) mSessionImpl.get();
                        if (impl != null) {
                            Bundle result = new Bundle();
                            IMediaSession extraBinder = impl.getSessionToken().getExtraBinder();
                            BundleCompat.putBinder(result, EXTRA_BINDER,
                                    extraBinder == null ? null : extraBinder.asBinder());
                            cb.send(0, result);
                        }
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onAddQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM_AT)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onAddQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION),
                                extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM)) {
                        extras.setClassLoader(MediaDescriptionCompat.class.getClassLoader());
                        Callback.this.onRemoveQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM_AT)) {
                        MediaSessionImplApi21 impl = (MediaSessionImplApi21) mSessionImpl.get();
                        if (impl != null && impl.mQueue != null) {
                            int index =
                                    extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX, -1);
                            QueueItem item = (index >= 0 && index < impl.mQueue.size())
                                    ? impl.mQueue.get(index) : null;
                            if (item != null) {
                                Callback.this.onRemoveQueueItem(item.getDescription());
                            }
                        }
                    } else {
                        Callback.this.onCommand(command, extras, cb);
                    }
                } catch (BadParcelableException e) {
                    // Do not print the exception here, since it is already done by the Parcel
                    // class.
                    Log.e(TAG, "Could not unparcel the extra data.");
                }
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
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                Callback.this.onPlayFromMediaId(mediaId, extras);
            }

            @Override
            public void onPlayFromSearch(String search, Bundle extras) {
                Callback.this.onPlayFromSearch(search, extras);
            }

            @Override
            public void onSkipToQueueItem(long id) {
                Callback.this.onSkipToQueueItem(id);
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

            @Override
            public void onSetRating(Object ratingObj, Bundle extras) {
                Callback.this.onSetRating(RatingCompat.fromRating(ratingObj), extras);
            }

            @Override
            public void onCustomAction(String action, Bundle extras) {
                if (action.equals(ACTION_PLAY_FROM_URI)) {
                    Uri uri = extras.getParcelable(ACTION_ARGUMENT_URI);
                    Bundle bundle = extras.getParcelable(ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPlayFromUri(uri, bundle);
                } else if (action.equals(ACTION_PREPARE)) {
                    Callback.this.onPrepare();
                } else if (action.equals(ACTION_PREPARE_FROM_MEDIA_ID)) {
                    String mediaId = extras.getString(ACTION_ARGUMENT_MEDIA_ID);
                    Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromMediaId(mediaId, bundle);
                } else if (action.equals(ACTION_PREPARE_FROM_SEARCH)) {
                    String query = extras.getString(ACTION_ARGUMENT_QUERY);
                    Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromSearch(query, bundle);
                } else if (action.equals(ACTION_PREPARE_FROM_URI)) {
                    Uri uri = extras.getParcelable(ACTION_ARGUMENT_URI);
                    Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onPrepareFromUri(uri, bundle);
                } else if (action.equals(ACTION_SET_CAPTIONING_ENABLED)) {
                    boolean enabled = extras.getBoolean(ACTION_ARGUMENT_CAPTIONING_ENABLED);
                    Callback.this.onSetCaptioningEnabled(enabled);
                } else if (action.equals(ACTION_SET_REPEAT_MODE)) {
                    int repeatMode = extras.getInt(ACTION_ARGUMENT_REPEAT_MODE);
                    Callback.this.onSetRepeatMode(repeatMode);
                } else if (action.equals(ACTION_SET_SHUFFLE_MODE_ENABLED)) {
                    boolean enabled = extras.getBoolean(ACTION_ARGUMENT_SHUFFLE_MODE_ENABLED);
                    Callback.this.onSetShuffleModeEnabled(enabled);
                } else if (action.equals(ACTION_SET_SHUFFLE_MODE)) {
                    int shuffleMode = extras.getInt(ACTION_ARGUMENT_SHUFFLE_MODE);
                    Callback.this.onSetShuffleMode(shuffleMode);
                } else if (action.equals(ACTION_SET_RATING)) {
                    extras.setClassLoader(RatingCompat.class.getClassLoader());
                    RatingCompat rating = extras.getParcelable(ACTION_ARGUMENT_RATING);
                    Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                    Callback.this.onSetRating(rating, bundle);
                } else {
                    Callback.this.onCustomAction(action, extras);
                }
            }
        }

        @RequiresApi(23)
        private class StubApi23 extends StubApi21 implements MediaSessionCompatApi23.Callback {

            StubApi23() {
            }

            @Override
            public void onPlayFromUri(Uri uri, Bundle extras) {
                Callback.this.onPlayFromUri(uri, extras);
            }
        }

        @RequiresApi(24)
        private class StubApi24 extends StubApi23 implements MediaSessionCompatApi24.Callback {

            StubApi24() {
            }

            @Override
            public void onPrepare() {
                Callback.this.onPrepare();
            }

            @Override
            public void onPrepareFromMediaId(String mediaId, Bundle extras) {
                Callback.this.onPrepareFromMediaId(mediaId, extras);
            }

            @Override
            public void onPrepareFromSearch(String query, Bundle extras) {
                Callback.this.onPrepareFromSearch(query, extras);
            }

            @Override
            public void onPrepareFromUri(Uri uri, Bundle extras) {
                Callback.this.onPrepareFromUri(uri, extras);
            }
        }
    }

    /**
     * Represents an ongoing session. This may be passed to apps by the session
     * owner to allow them to create a {@link MediaControllerCompat} to communicate with
     * the session.
     */
    public static final class Token implements Parcelable {
        private final Object mInner;
        private final IMediaSession mExtraBinder;

        Token(Object inner) {
            this(inner, null);
        }

        Token(Object inner, IMediaSession extraBinder) {
            mInner = inner;
            mExtraBinder = extraBinder;
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
            return fromToken(token, null);
        }

        /**
         * Creates a compat Token from a framework
         * {@link android.media.session.MediaSession.Token} object, and the extra binder.
         * <p>
         * This method is only supported on
         * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and later.
         * </p>
         *
         * @param token The framework token object.
         * @param extraBinder The extra binder.
         * @return A compat Token for use with {@link MediaControllerCompat}.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public static Token fromToken(Object token, IMediaSession extraBinder) {
            if (token != null && android.os.Build.VERSION.SDK_INT >= 21) {
                return new Token(MediaSessionCompatApi21.verifyToken(token), extraBinder);
            }
            return null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                dest.writeParcelable((Parcelable) mInner, flags);
            } else {
                dest.writeStrongBinder((IBinder) mInner);
            }
        }

        @Override
        public int hashCode() {
            if (mInner == null) {
                return 0;
            }
            return mInner.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Token)) {
                return false;
            }

            Token other = (Token) obj;
            if (mInner == null) {
                return other.mInner == null;
            }
            if (other.mInner == null) {
                return false;
            }
            return mInner.equals(other.mInner);
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

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public IMediaSession getExtraBinder() {
            return mExtraBinder;
        }

        public static final Parcelable.Creator<Token> CREATOR
                = new Parcelable.Creator<Token>() {
                    @Override
                    public Token createFromParcel(Parcel in) {
                        Object inner;
                        if (android.os.Build.VERSION.SDK_INT >= 21) {
                            inner = in.readParcelable(null);
                        } else {
                            inner = in.readStrongBinder();
                        }
                        return new Token(inner);
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
         * This id is reserved. No items can be explicitly assigned this id.
         */
        public static final int UNKNOWN_ID = -1;

        private final MediaDescriptionCompat mDescription;
        private final long mId;

        private Object mItem;

        /**
         * Creates a new {@link MediaSessionCompat.QueueItem}.
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

        QueueItem(Parcel in) {
            mDescription = MediaDescriptionCompat.CREATOR.createFromParcel(in);
            mId = in.readLong();
        }

        /**
         * Gets the description for this item.
         */
        public MediaDescriptionCompat getDescription() {
            return mDescription;
        }

        /**
         * Gets the queue id for this item.
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
         * Gets the underlying
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
         * Creates an instance from a framework {@link android.media.session.MediaSession.QueueItem}
         * object.
         * <p>
         * This method is only supported on API 21+. On API 20 and below, it returns null.
         * </p>
         *
         * @param queueItem A {@link android.media.session.MediaSession.QueueItem} object.
         * @return An equivalent {@link QueueItem} object, or null if none.
         */
        public static QueueItem fromQueueItem(Object queueItem) {
            if (queueItem == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            Object descriptionObj = MediaSessionCompatApi21.QueueItem.getDescription(queueItem);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(
                    descriptionObj);
            long id = MediaSessionCompatApi21.QueueItem.getQueueId(queueItem);
            return new QueueItem(queueItem, description, id);
        }

        /**
         * Creates a list of {@link QueueItem} objects from a framework
         * {@link android.media.session.MediaSession.QueueItem} object list.
         * <p>
         * This method is only supported on API 21+. On API 20 and below, it returns null.
         * </p>
         *
         * @param itemList A list of {@link android.media.session.MediaSession.QueueItem} objects.
         * @return An equivalent list of {@link QueueItem} objects, or null if none.
         */
        public static List<QueueItem> fromQueueItemList(List<?> itemList) {
            if (itemList == null || Build.VERSION.SDK_INT < 21) {
                return null;
            }
            List<QueueItem> items = new ArrayList<>();
            for (Object itemObj : itemList) {
                items.add(fromQueueItem(itemObj));
            }
            return items;
        }

        public static final Creator<MediaSessionCompat.QueueItem> CREATOR
                = new Creator<MediaSessionCompat.QueueItem>() {

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

    /**
     * This is a wrapper for {@link ResultReceiver} for sending over aidl
     * interfaces. The framework version was not exposed to aidls until
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP}.
     */
    static final class ResultReceiverWrapper implements Parcelable {
        private ResultReceiver mResultReceiver;

        public ResultReceiverWrapper(ResultReceiver resultReceiver) {
            mResultReceiver = resultReceiver;
        }

        ResultReceiverWrapper(Parcel in) {
            mResultReceiver = ResultReceiver.CREATOR.createFromParcel(in);
        }

        public static final Creator<ResultReceiverWrapper>
                CREATOR = new Creator<ResultReceiverWrapper>() {
            @Override
            public ResultReceiverWrapper createFromParcel(Parcel p) {
                return new ResultReceiverWrapper(p);
            }

            @Override
            public ResultReceiverWrapper[] newArray(int size) {
                return new ResultReceiverWrapper[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            mResultReceiver.writeToParcel(dest, flags);
        }
    }

    public interface OnActiveChangeListener {
        void onActiveChanged();
    }

    interface MediaSessionImpl {
        void setCallback(Callback callback, Handler handler);
        void setFlags(@SessionFlags int flags);
        void setPlaybackToLocal(int stream);
        void setPlaybackToRemote(VolumeProviderCompat volumeProvider);
        void setActive(boolean active);
        boolean isActive();
        void sendSessionEvent(String event, Bundle extras);
        void release();
        Token getSessionToken();
        void setPlaybackState(PlaybackStateCompat state);
        PlaybackStateCompat getPlaybackState();
        void setMetadata(MediaMetadataCompat metadata);

        void setSessionActivity(PendingIntent pi);

        void setMediaButtonReceiver(PendingIntent mbr);
        void setQueue(List<QueueItem> queue);
        void setQueueTitle(CharSequence title);

        void setRatingType(@RatingCompat.Style int type);
        void setCaptioningEnabled(boolean enabled);
        void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode);
        void setShuffleModeEnabled(boolean enabled);
        void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode);
        void setExtras(Bundle extras);

        Object getMediaSession();

        Object getRemoteControlClient();

        String getCallingPackage();
    }

    static class MediaSessionImplBase implements MediaSessionImpl {
        /***** RemoteControlClient States, we only need none as the others were public *******/
        static final int RCC_PLAYSTATE_NONE = 0;

        private final Context mContext;
        private final ComponentName mMediaButtonReceiverComponentName;
        private final PendingIntent mMediaButtonReceiverIntent;
        private final MediaSessionStub mStub;
        private final Token mToken;
        final String mPackageName;
        final String mTag;
        final AudioManager mAudioManager;
        final RemoteControlClient mRcc;

        final Object mLock = new Object();
        final RemoteCallbackList<IMediaControllerCallback> mControllerCallbacks
                = new RemoteCallbackList<>();

        private MessageHandler mHandler;
        boolean mDestroyed = false;
        boolean mIsActive = false;
        private boolean mIsMbrRegistered = false;
        private boolean mIsRccRegistered = false;
        volatile Callback mCallback;

        @SessionFlags int mFlags;

        MediaMetadataCompat mMetadata;
        PlaybackStateCompat mState;
        PendingIntent mSessionActivity;
        List<QueueItem> mQueue;
        CharSequence mQueueTitle;
        @RatingCompat.Style int mRatingType;
        boolean mCaptioningEnabled;
        @PlaybackStateCompat.RepeatMode int mRepeatMode;
        @PlaybackStateCompat.ShuffleMode int mShuffleMode;
        boolean mShuffleModeEnabled;
        Bundle mExtras;

        int mVolumeType;
        int mLocalStream;
        VolumeProviderCompat mVolumeProvider;

        private VolumeProviderCompat.Callback mVolumeCallback
                = new VolumeProviderCompat.Callback() {
            @Override
            public void onVolumeChanged(VolumeProviderCompat volumeProvider) {
                if (mVolumeProvider != volumeProvider) {
                    return;
                }
                ParcelableVolumeInfo info = new ParcelableVolumeInfo(mVolumeType, mLocalStream,
                        volumeProvider.getVolumeControl(), volumeProvider.getMaxVolume(),
                        volumeProvider.getCurrentVolume());
                sendVolumeInfoChanged(info);
            }
        };

        public MediaSessionImplBase(Context context, String tag, ComponentName mbrComponent,
                PendingIntent mbrIntent) {
            if (mbrComponent == null) {
                throw new IllegalArgumentException(
                        "MediaButtonReceiver component may not be null.");
            }
            mContext = context;
            mPackageName = context.getPackageName();
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mTag = tag;
            mMediaButtonReceiverComponentName = mbrComponent;
            mMediaButtonReceiverIntent = mbrIntent;
            mStub = new MediaSessionStub();
            mToken = new Token(mStub);

            mRatingType = RatingCompat.RATING_NONE;
            mVolumeType = MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL;
            mLocalStream = AudioManager.STREAM_MUSIC;
            mRcc = new RemoteControlClient(mbrIntent);
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            mCallback = callback;
            if (callback != null) {
                if (handler == null) {
                    handler = new Handler();
                }
                synchronized (mLock) {
                    if (mHandler != null) {
                        mHandler.removeCallbacksAndMessages(null);
                    }
                    mHandler = new MessageHandler(handler.getLooper());
                    mCallback.setSessionImpl(this, handler);
                }
            }
        }

        void postToHandler(int what) {
            postToHandler(what, null);
        }

        void postToHandler(int what, int arg1) {
            postToHandler(what, null, arg1);
        }

        void postToHandler(int what, Object obj) {
            postToHandler(what, obj, null);
        }

        void postToHandler(int what, Object obj, int arg1) {
            synchronized (mLock) {
                if (mHandler != null) {
                    mHandler.post(what, obj, arg1);
                }
            }
        }

        void postToHandler(int what, Object obj, Bundle extras) {
            synchronized (mLock) {
                if (mHandler != null) {
                    mHandler.post(what, obj, extras);
                }
            }
        }

        @Override
        public void setFlags(@SessionFlags int flags) {
            synchronized (mLock) {
                mFlags = flags;
            }
            update();
        }

        @Override
        public void setPlaybackToLocal(int stream) {
            if (mVolumeProvider != null) {
                mVolumeProvider.setCallback(null);
            }
            mVolumeType = MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_LOCAL;
            ParcelableVolumeInfo info = new ParcelableVolumeInfo(mVolumeType, mLocalStream,
                    VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE,
                    mAudioManager.getStreamMaxVolume(mLocalStream),
                    mAudioManager.getStreamVolume(mLocalStream));
            sendVolumeInfoChanged(info);
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            if (volumeProvider == null) {
                throw new IllegalArgumentException("volumeProvider may not be null");
            }
            if (mVolumeProvider != null) {
                mVolumeProvider.setCallback(null);
            }
            mVolumeType = MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE;
            mVolumeProvider = volumeProvider;
            ParcelableVolumeInfo info = new ParcelableVolumeInfo(mVolumeType, mLocalStream,
                    mVolumeProvider.getVolumeControl(), mVolumeProvider.getMaxVolume(),
                    mVolumeProvider.getCurrentVolume());
            sendVolumeInfoChanged(info);

            volumeProvider.setCallback(mVolumeCallback);
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
            sendEvent(event, extras);
        }

        @Override
        public void release() {
            mIsActive = false;
            mDestroyed = true;
            update();
            sendSessionDestroyed();
        }

        @Override
        public Token getSessionToken() {
            return mToken;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            synchronized (mLock) {
                mState = state;
            }
            sendState(state);
            if (!mIsActive) {
                // Don't set the state until after the RCC is registered
                return;
            }
            if (state == null) {
                mRcc.setPlaybackState(0);
                mRcc.setTransportControlFlags(0);
            } else {
                // Set state
                setRccState(state);

                // Set transport control flags
                mRcc.setTransportControlFlags(
                        getRccTransportControlFlagsFromActions(state.getActions()));
            }
        }

        @Override
        public PlaybackStateCompat getPlaybackState() {
            synchronized (mLock) {
                return mState;
            }
        }

        void setRccState(PlaybackStateCompat state) {
            mRcc.setPlaybackState(getRccStateFromState(state.getState()));
        }

        int getRccStateFromState(int state) {
            switch (state) {
                case PlaybackStateCompat.STATE_CONNECTING:
                case PlaybackStateCompat.STATE_BUFFERING:
                    return RemoteControlClient.PLAYSTATE_BUFFERING;
                case PlaybackStateCompat.STATE_ERROR:
                    return RemoteControlClient.PLAYSTATE_ERROR;
                case PlaybackStateCompat.STATE_FAST_FORWARDING:
                    return RemoteControlClient.PLAYSTATE_FAST_FORWARDING;
                case PlaybackStateCompat.STATE_NONE:
                    return RCC_PLAYSTATE_NONE;
                case PlaybackStateCompat.STATE_PAUSED:
                    return RemoteControlClient.PLAYSTATE_PAUSED;
                case PlaybackStateCompat.STATE_PLAYING:
                    return RemoteControlClient.PLAYSTATE_PLAYING;
                case PlaybackStateCompat.STATE_REWINDING:
                    return RemoteControlClient.PLAYSTATE_REWINDING;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    return RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS;
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                    return RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS;
                case PlaybackStateCompat.STATE_STOPPED:
                    return RemoteControlClient.PLAYSTATE_STOPPED;
                default:
                    return -1;
            }
        }

        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = 0;
            if ((actions & PlaybackStateCompat.ACTION_STOP) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_STOP;
            }
            if ((actions & PlaybackStateCompat.ACTION_PAUSE) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_PAUSE;
            }
            if ((actions & PlaybackStateCompat.ACTION_PLAY) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_PLAY;
            }
            if ((actions & PlaybackStateCompat.ACTION_REWIND) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_REWIND;
            }
            if ((actions & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS;
            }
            if ((actions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_NEXT;
            }
            if ((actions & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD;
            }
            if ((actions & PlaybackStateCompat.ACTION_PLAY_PAUSE) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE;
            }
            return transportControlFlags;
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            if (metadata != null) {
                // Clones {@link MediaMetadataCompat} and scales down bitmaps if they are large.
                metadata = new MediaMetadataCompat.Builder(metadata, sMaxBitmapSize).build();
            }

            synchronized (mLock) {
                mMetadata = metadata;
            }
            sendMetadata(metadata);
            if (!mIsActive) {
                // Don't set metadata until after the rcc has been registered
                return;
            }
            RemoteControlClient.MetadataEditor editor = buildRccMetadata(
                    metadata == null ? null : metadata.getBundle());
            editor.apply();
        }

        RemoteControlClient.MetadataEditor buildRccMetadata(Bundle metadata) {
            RemoteControlClient.MetadataEditor editor = mRcc.editMetadata(true);
            if (metadata == null) {
                return editor;
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_ART)) {
                Bitmap art = metadata.getParcelable(MediaMetadataCompat.METADATA_KEY_ART);
                if (art != null) {
                    // Clone the bitmap to prevent it from being recycled by RCC.
                    art = art.copy(art.getConfig(), false);
                }
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, art);
            } else if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)) {
                // Fall back to album art if the track art wasn't available
                Bitmap art = metadata.getParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
                if (art != null) {
                    // Clone the bitmap to prevent it from being recycled by RCC.
                    art = art.copy(art.getConfig(), false);
                }
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, art);
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_ALBUM)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_ARTIST)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_AUTHOR)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_AUTHOR,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_AUTHOR));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_COMPILATION)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_COMPILATION,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_COMPILATION));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_COMPOSER)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_COMPOSER,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_COMPOSER));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_DATE)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_DATE,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_DATE));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER)) {
                editor.putLong(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER,
                        metadata.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                editor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,
                        metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_GENRE)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_GENRE,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_TITLE)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)) {
                editor.putLong(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                        metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_WRITER)) {
                editor.putString(MediaMetadataRetriever.METADATA_KEY_WRITER,
                        metadata.getString(MediaMetadataCompat.METADATA_KEY_WRITER));
            }
            return editor;
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            synchronized (mLock) {
                mSessionActivity = pi;
            }
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            // Do nothing, changing this is not supported before API 21.
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            mQueue = queue;
            sendQueue(queue);
        }

        @Override
        public void setQueueTitle(CharSequence title) {
            mQueueTitle = title;
            sendQueueTitle(title);
        }

        @Override
        public Object getMediaSession() {
            return null;
        }

        @Override
        public Object getRemoteControlClient() {
            return null;
        }

        @Override
        public String getCallingPackage() {
            return null;
        }

        @Override
        public void setRatingType(@RatingCompat.Style int type) {
            mRatingType = type;
        }

        @Override
        public void setCaptioningEnabled(boolean enabled) {
            if (mCaptioningEnabled != enabled) {
                mCaptioningEnabled = enabled;
                sendCaptioningEnabled(enabled);
            }
        }

        @Override
        public void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
            if (mRepeatMode != repeatMode) {
                mRepeatMode = repeatMode;
                sendRepeatMode(repeatMode);
            }
        }

        @Override
        public void setShuffleModeEnabled(boolean enabled) {
            if (mShuffleModeEnabled != enabled) {
                mShuffleModeEnabled = enabled;
                sendShuffleModeEnabled(enabled);
            }
        }

        @Override
        public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
            if (mShuffleMode != shuffleMode) {
                mShuffleMode = shuffleMode;
                sendShuffleMode(shuffleMode);
            }
        }

        @Override
        public void setExtras(Bundle extras) {
            mExtras = extras;
            sendExtras(extras);
        }

        // Registers/unregisters components as needed.
        boolean update() {
            boolean registeredRcc = false;
            if (mIsActive) {
                // Register a MBR if it's supported, unregister it if support was removed.
                if (!mIsMbrRegistered && (mFlags & FLAG_HANDLES_MEDIA_BUTTONS) != 0) {
                    registerMediaButtonEventReceiver(mMediaButtonReceiverIntent,
                            mMediaButtonReceiverComponentName);
                    mIsMbrRegistered = true;
                } else if (mIsMbrRegistered && (mFlags & FLAG_HANDLES_MEDIA_BUTTONS) == 0) {
                    unregisterMediaButtonEventReceiver(mMediaButtonReceiverIntent,
                            mMediaButtonReceiverComponentName);
                    mIsMbrRegistered = false;
                }
                // Register a RCC if it's supported, unregister it if support was removed.
                if (!mIsRccRegistered && (mFlags & FLAG_HANDLES_TRANSPORT_CONTROLS) != 0) {
                    mAudioManager.registerRemoteControlClient(mRcc);
                    mIsRccRegistered = true;
                    registeredRcc = true;
                } else if (mIsRccRegistered
                        && (mFlags & FLAG_HANDLES_TRANSPORT_CONTROLS) == 0) {
                    // RCC keeps the state while the system resets its state internally when
                    // we register RCC. Reset the state so that the states in RCC and the system
                    // are in sync when we re-register the RCC.
                    mRcc.setPlaybackState(0);
                    mAudioManager.unregisterRemoteControlClient(mRcc);
                    mIsRccRegistered = false;
                }
            } else {
                // When inactive remove any registered components.
                if (mIsMbrRegistered) {
                    unregisterMediaButtonEventReceiver(mMediaButtonReceiverIntent,
                            mMediaButtonReceiverComponentName);
                    mIsMbrRegistered = false;
                }
                if (mIsRccRegistered) {
                    // RCC keeps the state while the system resets its state internally when
                    // we register RCC. Reset the state so that the states in RCC and the system
                    // are in sync when we re-register the RCC.
                    mRcc.setPlaybackState(0);
                    mAudioManager.unregisterRemoteControlClient(mRcc);
                    mIsRccRegistered = false;
                }
            }
            return registeredRcc;
        }

        void registerMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            mAudioManager.registerMediaButtonEventReceiver(mbrComponent);
        }

        void unregisterMediaButtonEventReceiver(PendingIntent mbrIntent,
                ComponentName mbrComponent) {
            mAudioManager.unregisterMediaButtonEventReceiver(mbrComponent);
        }

        void adjustVolume(int direction, int flags) {
            if (mVolumeType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                if (mVolumeProvider != null) {
                    mVolumeProvider.onAdjustVolume(direction);
                }
            } else {
                mAudioManager.adjustStreamVolume(mLocalStream, direction, flags);
            }
        }

        void setVolumeTo(int value, int flags) {
            if (mVolumeType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                if (mVolumeProvider != null) {
                    mVolumeProvider.onSetVolumeTo(value);
                }
            } else {
                mAudioManager.setStreamVolume(mLocalStream, value, flags);
            }
        }

        void sendVolumeInfoChanged(ParcelableVolumeInfo info) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onVolumeInfoChanged(info);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendSessionDestroyed() {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onSessionDestroyed();
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
            mControllerCallbacks.kill();
        }

        private void sendEvent(String event, Bundle extras) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onEvent(event, extras);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendState(PlaybackStateCompat state) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onPlaybackStateChanged(state);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendMetadata(MediaMetadataCompat metadata) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onMetadataChanged(metadata);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendQueue(List<QueueItem> queue) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onQueueChanged(queue);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendQueueTitle(CharSequence queueTitle) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onQueueTitleChanged(queueTitle);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendCaptioningEnabled(boolean enabled) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onCaptioningEnabledChanged(enabled);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendRepeatMode(int repeatMode) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onRepeatModeChanged(repeatMode);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendShuffleModeEnabled(boolean enabled) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onShuffleModeChangedDeprecated(enabled);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendShuffleMode(int shuffleMode) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onShuffleModeChanged(shuffleMode);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        private void sendExtras(Bundle extras) {
            int size = mControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onExtrasChanged(extras);
                } catch (RemoteException e) {
                }
            }
            mControllerCallbacks.finishBroadcast();
        }

        class MediaSessionStub extends IMediaSession.Stub {
            @Override
            public void sendCommand(String command, Bundle args, ResultReceiverWrapper cb) {
                postToHandler(MessageHandler.MSG_COMMAND,
                        new Command(command, args, cb.mResultReceiver));
            }

            @Override
            public boolean sendMediaButton(KeyEvent mediaButton) {
                boolean handlesMediaButtons =
                        (mFlags & MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS) != 0;
                if (handlesMediaButtons) {
                    postToHandler(MessageHandler.MSG_MEDIA_BUTTON, mediaButton);
                }
                return handlesMediaButtons;
            }

            @Override
            public void registerCallbackListener(IMediaControllerCallback cb) {
                // If this session is already destroyed tell the caller and
                // don't add them.
                if (mDestroyed) {
                    try {
                        cb.onSessionDestroyed();
                    } catch (Exception e) {
                        // ignored
                    }
                    return;
                }
                mControllerCallbacks.register(cb);
            }

            @Override
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                mControllerCallbacks.unregister(cb);
            }

            @Override
            public String getPackageName() {
                // mPackageName is final so doesn't need synchronize block
                return mPackageName;
            }

            @Override
            public String getTag() {
                // mTag is final so doesn't need synchronize block
                return mTag;
            }

            @Override
            public PendingIntent getLaunchPendingIntent() {
                synchronized (mLock) {
                    return mSessionActivity;
                }
            }

            @Override
            @SessionFlags
            public long getFlags() {
                synchronized (mLock) {
                    return mFlags;
                }
            }

            @Override
            public ParcelableVolumeInfo getVolumeAttributes() {
                int controlType;
                int max;
                int current;
                int stream;
                int volumeType;
                synchronized (mLock) {
                    volumeType = mVolumeType;
                    stream = mLocalStream;
                    VolumeProviderCompat vp = mVolumeProvider;
                    if (volumeType == MediaControllerCompat.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                        controlType = vp.getVolumeControl();
                        max = vp.getMaxVolume();
                        current = vp.getCurrentVolume();
                    } else {
                        controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
                        max = mAudioManager.getStreamMaxVolume(stream);
                        current = mAudioManager.getStreamVolume(stream);
                    }
                }
                return new ParcelableVolumeInfo(volumeType, stream, controlType, max, current);
            }

            @Override
            public void adjustVolume(int direction, int flags, String packageName) {
                MediaSessionImplBase.this.adjustVolume(direction, flags);
            }

            @Override
            public void setVolumeTo(int value, int flags, String packageName) {
                MediaSessionImplBase.this.setVolumeTo(value, flags);
            }

            @Override
            public void prepare() throws RemoteException {
                postToHandler(MessageHandler.MSG_PREPARE);
            }

            @Override
            public void prepareFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PREPARE_MEDIA_ID, mediaId, extras);
            }

            @Override
            public void prepareFromSearch(String query, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PREPARE_SEARCH, query, extras);
            }

            @Override
            public void prepareFromUri(Uri uri, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PREPARE_URI, uri, extras);
            }

            @Override
            public void play() throws RemoteException {
                postToHandler(MessageHandler.MSG_PLAY);
            }

            @Override
            public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PLAY_MEDIA_ID, mediaId, extras);
            }

            @Override
            public void playFromSearch(String query, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PLAY_SEARCH, query, extras);
            }

            @Override
            public void playFromUri(Uri uri, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_PLAY_URI, uri, extras);
            }

            @Override
            public void skipToQueueItem(long id) {
                postToHandler(MessageHandler.MSG_SKIP_TO_ITEM, id);
            }

            @Override
            public void pause() throws RemoteException {
                postToHandler(MessageHandler.MSG_PAUSE);
            }

            @Override
            public void stop() throws RemoteException {
                postToHandler(MessageHandler.MSG_STOP);
            }

            @Override
            public void next() throws RemoteException {
                postToHandler(MessageHandler.MSG_NEXT);
            }

            @Override
            public void previous() throws RemoteException {
                postToHandler(MessageHandler.MSG_PREVIOUS);
            }

            @Override
            public void fastForward() throws RemoteException {
                postToHandler(MessageHandler.MSG_FAST_FORWARD);
            }

            @Override
            public void rewind() throws RemoteException {
                postToHandler(MessageHandler.MSG_REWIND);
            }

            @Override
            public void seekTo(long pos) throws RemoteException {
                postToHandler(MessageHandler.MSG_SEEK_TO, pos);
            }

            @Override
            public void rate(RatingCompat rating) throws RemoteException {
                postToHandler(MessageHandler.MSG_RATE, rating);
            }

            @Override
            public void rateWithExtras(RatingCompat rating, Bundle extras) throws RemoteException {
                postToHandler(MessageHandler.MSG_RATE_EXTRA, rating, extras);
            }

            @Override
            public void setCaptioningEnabled(boolean enabled) throws RemoteException {
                postToHandler(MessageHandler.MSG_SET_CAPTIONING_ENABLED, enabled);
            }

            @Override
            public void setRepeatMode(int repeatMode) throws RemoteException {
                postToHandler(MessageHandler.MSG_SET_REPEAT_MODE, repeatMode);
            }

            @Override
            public void setShuffleModeEnabledDeprecated(boolean enabled) throws RemoteException {
                postToHandler(MessageHandler.MSG_SET_SHUFFLE_MODE_ENABLED, enabled);
            }

            @Override
            public void setShuffleMode(int shuffleMode) throws RemoteException {
                postToHandler(MessageHandler.MSG_SET_SHUFFLE_MODE, shuffleMode);
            }

            @Override
            public void sendCustomAction(String action, Bundle args)
                    throws RemoteException {
                postToHandler(MessageHandler.MSG_CUSTOM_ACTION, action, args);
            }

            @Override
            public MediaMetadataCompat getMetadata() {
                return mMetadata;
            }

            @Override
            public PlaybackStateCompat getPlaybackState() {
                PlaybackStateCompat state;
                MediaMetadataCompat metadata;
                synchronized (mLock) {
                    state = mState;
                    metadata = mMetadata;
                }
                return getStateWithUpdatedPosition(state, metadata);
            }

            @Override
            public List<QueueItem> getQueue() {
                synchronized (mLock) {
                    return mQueue;
                }
            }

            @Override
            public void addQueueItem(MediaDescriptionCompat description) {
                postToHandler(MessageHandler.MSG_ADD_QUEUE_ITEM, description);
            }

            @Override
            public void addQueueItemAt(MediaDescriptionCompat description, int index) {
                postToHandler(MessageHandler.MSG_ADD_QUEUE_ITEM_AT, description, index);
            }

            @Override
            public void removeQueueItem(MediaDescriptionCompat description) {
                postToHandler(MessageHandler.MSG_REMOVE_QUEUE_ITEM, description);
            }

            @Override
            public void removeQueueItemAt(int index) {
                postToHandler(MessageHandler.MSG_REMOVE_QUEUE_ITEM_AT, index);
            }

            @Override
            public CharSequence getQueueTitle() {
                return mQueueTitle;
            }

            @Override
            public Bundle getExtras() {
                synchronized (mLock) {
                    return mExtras;
                }
            }

            @Override
            @RatingCompat.Style
            public int getRatingType() {
                return mRatingType;
            }

            @Override
            public boolean isCaptioningEnabled() {
                return mCaptioningEnabled;
            }

            @Override
            @PlaybackStateCompat.RepeatMode
            public int getRepeatMode() {
                return mRepeatMode;
            }

            @Override
            public boolean isShuffleModeEnabledDeprecated() {
                return mShuffleModeEnabled;
            }

            @Override
            @PlaybackStateCompat.ShuffleMode
            public int getShuffleMode() {
                return mShuffleMode;
            }

            @Override
            public boolean isTransportControlEnabled() {
                return (mFlags & FLAG_HANDLES_TRANSPORT_CONTROLS) != 0;
            }
        }

        private static final class Command {
            public final String command;
            public final Bundle extras;
            public final ResultReceiver stub;

            public Command(String command, Bundle extras, ResultReceiver stub) {
                this.command = command;
                this.extras = extras;
                this.stub = stub;
            }
        }

        class MessageHandler extends Handler {

            private static final int MSG_COMMAND = 1;
            private static final int MSG_ADJUST_VOLUME = 2;
            private static final int MSG_PREPARE = 3;
            private static final int MSG_PREPARE_MEDIA_ID = 4;
            private static final int MSG_PREPARE_SEARCH = 5;
            private static final int MSG_PREPARE_URI = 6;
            private static final int MSG_PLAY = 7;
            private static final int MSG_PLAY_MEDIA_ID = 8;
            private static final int MSG_PLAY_SEARCH = 9;
            private static final int MSG_PLAY_URI = 10;
            private static final int MSG_SKIP_TO_ITEM = 11;
            private static final int MSG_PAUSE = 12;
            private static final int MSG_STOP = 13;
            private static final int MSG_NEXT = 14;
            private static final int MSG_PREVIOUS = 15;
            private static final int MSG_FAST_FORWARD = 16;
            private static final int MSG_REWIND = 17;
            private static final int MSG_SEEK_TO = 18;
            private static final int MSG_RATE = 19;
            private static final int MSG_RATE_EXTRA = 31;
            private static final int MSG_CUSTOM_ACTION = 20;
            private static final int MSG_MEDIA_BUTTON = 21;
            private static final int MSG_SET_VOLUME = 22;
            private static final int MSG_SET_REPEAT_MODE = 23;
            private static final int MSG_SET_SHUFFLE_MODE_ENABLED = 24;
            private static final int MSG_ADD_QUEUE_ITEM = 25;
            private static final int MSG_ADD_QUEUE_ITEM_AT = 26;
            private static final int MSG_REMOVE_QUEUE_ITEM = 27;
            private static final int MSG_REMOVE_QUEUE_ITEM_AT = 28;
            private static final int MSG_SET_CAPTIONING_ENABLED = 29;
            private static final int MSG_SET_SHUFFLE_MODE = 30;

            // KeyEvent constants only available on API 11+
            private static final int KEYCODE_MEDIA_PAUSE = 127;
            private static final int KEYCODE_MEDIA_PLAY = 126;

            public MessageHandler(Looper looper) {
                super(looper);
            }

            public void post(int what, Object obj, Bundle bundle) {
                Message msg = obtainMessage(what, obj);
                msg.setData(bundle);
                msg.sendToTarget();
            }

            public void post(int what, Object obj) {
                obtainMessage(what, obj).sendToTarget();
            }

            public void post(int what) {
                post(what, null);
            }

            public void post(int what, Object obj, int arg1) {
                obtainMessage(what, arg1, 0, obj).sendToTarget();
            }

            @Override
            public void handleMessage(Message msg) {
                MediaSessionCompat.Callback cb = mCallback;
                if (cb == null) {
                    return;
                }
                switch (msg.what) {
                    case MSG_COMMAND:
                        Command cmd = (Command) msg.obj;
                        cb.onCommand(cmd.command, cmd.extras, cmd.stub);
                        break;
                    case MSG_MEDIA_BUTTON:
                        KeyEvent keyEvent = (KeyEvent) msg.obj;
                        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                        // Let the Callback handle events first before using the default behavior
                        if (!cb.onMediaButtonEvent(intent)) {
                            onMediaButtonEvent(keyEvent, cb);
                        }
                        break;
                    case MSG_PREPARE:
                        cb.onPrepare();
                        break;
                    case MSG_PREPARE_MEDIA_ID:
                        cb.onPrepareFromMediaId((String) msg.obj, msg.getData());
                        break;
                    case MSG_PREPARE_SEARCH:
                        cb.onPrepareFromSearch((String) msg.obj, msg.getData());
                        break;
                    case MSG_PREPARE_URI:
                        cb.onPrepareFromUri((Uri) msg.obj, msg.getData());
                        break;
                    case MSG_PLAY:
                        cb.onPlay();
                        break;
                    case MSG_PLAY_MEDIA_ID:
                        cb.onPlayFromMediaId((String) msg.obj, msg.getData());
                        break;
                    case MSG_PLAY_SEARCH:
                        cb.onPlayFromSearch((String) msg.obj, msg.getData());
                        break;
                    case MSG_PLAY_URI:
                        cb.onPlayFromUri((Uri) msg.obj, msg.getData());
                        break;
                    case MSG_SKIP_TO_ITEM:
                        cb.onSkipToQueueItem((Long) msg.obj);
                        break;
                    case MSG_PAUSE:
                        cb.onPause();
                        break;
                    case MSG_STOP:
                        cb.onStop();
                        break;
                    case MSG_NEXT:
                        cb.onSkipToNext();
                        break;
                    case MSG_PREVIOUS:
                        cb.onSkipToPrevious();
                        break;
                    case MSG_FAST_FORWARD:
                        cb.onFastForward();
                        break;
                    case MSG_REWIND:
                        cb.onRewind();
                        break;
                    case MSG_SEEK_TO:
                        cb.onSeekTo((Long) msg.obj);
                        break;
                    case MSG_RATE:
                        cb.onSetRating((RatingCompat) msg.obj);
                        break;
                    case MSG_RATE_EXTRA:
                        cb.onSetRating((RatingCompat) msg.obj, msg.getData());
                        break;
                    case MSG_CUSTOM_ACTION:
                        cb.onCustomAction((String) msg.obj, msg.getData());
                        break;
                    case MSG_ADD_QUEUE_ITEM:
                        cb.onAddQueueItem((MediaDescriptionCompat) msg.obj);
                        break;
                    case MSG_ADD_QUEUE_ITEM_AT:
                        cb.onAddQueueItem((MediaDescriptionCompat) msg.obj, msg.arg1);
                        break;
                    case MSG_REMOVE_QUEUE_ITEM:
                        cb.onRemoveQueueItem((MediaDescriptionCompat) msg.obj);
                        break;
                    case MSG_REMOVE_QUEUE_ITEM_AT:
                        if (mQueue != null) {
                            QueueItem item = (msg.arg1 >= 0 && msg.arg1 < mQueue.size())
                                    ? mQueue.get(msg.arg1) : null;
                            if (item != null) {
                                cb.onRemoveQueueItem(item.getDescription());
                            }
                        }
                        break;
                    case MSG_ADJUST_VOLUME:
                        adjustVolume(msg.arg1, 0);
                        break;
                    case MSG_SET_VOLUME:
                        setVolumeTo(msg.arg1, 0);
                        break;
                    case MSG_SET_CAPTIONING_ENABLED:
                        cb.onSetCaptioningEnabled((boolean) msg.obj);
                        break;
                    case MSG_SET_REPEAT_MODE:
                        cb.onSetRepeatMode(msg.arg1);
                        break;
                    case MSG_SET_SHUFFLE_MODE_ENABLED:
                        cb.onSetShuffleModeEnabled((boolean) msg.obj);
                        break;
                    case MSG_SET_SHUFFLE_MODE:
                        cb.onSetShuffleMode(msg.arg1);
                        break;
                }
            }

            private void onMediaButtonEvent(KeyEvent ke, MediaSessionCompat.Callback cb) {
                if (ke == null || ke.getAction() != KeyEvent.ACTION_DOWN) {
                    return;
                }
                long validActions = mState == null ? 0 : mState.getActions();
                switch (ke.getKeyCode()) {
                    // Note KeyEvent.KEYCODE_MEDIA_PLAY is API 11+
                    case KEYCODE_MEDIA_PLAY:
                        if ((validActions & PlaybackStateCompat.ACTION_PLAY) != 0) {
                            cb.onPlay();
                        }
                        break;
                    // Note KeyEvent.KEYCODE_MEDIA_PAUSE is API 11+
                    case KEYCODE_MEDIA_PAUSE:
                        if ((validActions & PlaybackStateCompat.ACTION_PAUSE) != 0) {
                            cb.onPause();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        if ((validActions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
                            cb.onSkipToNext();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        if ((validActions & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
                            cb.onSkipToPrevious();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        if ((validActions & PlaybackStateCompat.ACTION_STOP) != 0) {
                            cb.onStop();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                        if ((validActions & PlaybackStateCompat.ACTION_FAST_FORWARD) != 0) {
                            cb.onFastForward();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_REWIND:
                        if ((validActions & PlaybackStateCompat.ACTION_REWIND) != 0) {
                            cb.onRewind();
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        Log.w(TAG, "KEYCODE_MEDIA_PLAY_PAUSE and KEYCODE_HEADSETHOOK are handled"
                                + " already");
                        break;
                }
            }
        }
    }

    @RequiresApi(18)
    static class MediaSessionImplApi18 extends MediaSessionImplBase {
        private static boolean sIsMbrPendingIntentSupported = true;

        MediaSessionImplApi18(Context context, String tag, ComponentName mbrComponent,
                PendingIntent mbrIntent) {
            super(context, tag, mbrComponent, mbrIntent);
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            super.setCallback(callback, handler);
            if (callback == null) {
                mRcc.setPlaybackPositionUpdateListener(null);
            } else {
                RemoteControlClient.OnPlaybackPositionUpdateListener listener =
                        new RemoteControlClient.OnPlaybackPositionUpdateListener() {
                            @Override
                            public void onPlaybackPositionUpdate(long newPositionMs) {
                                postToHandler(MessageHandler.MSG_SEEK_TO, newPositionMs);
                            }
                        };
                mRcc.setPlaybackPositionUpdateListener(listener);
            }
        }

        @Override
        void setRccState(PlaybackStateCompat state) {
            long position = state.getPosition();
            float speed = state.getPlaybackSpeed();
            long updateTime = state.getLastPositionUpdateTime();
            long currTime = SystemClock.elapsedRealtime();
            if (state.getState() == PlaybackStateCompat.STATE_PLAYING && position > 0) {
                long diff = 0;
                if (updateTime > 0) {
                    diff = currTime - updateTime;
                    if (speed > 0 && speed != 1f) {
                        diff = (long) (diff * speed);
                    }
                }
                position += diff;
            }
            mRcc.setPlaybackState(getRccStateFromState(state.getState()), position, speed);
        }

        @Override
        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = super.getRccTransportControlFlagsFromActions(actions);
            if ((actions & PlaybackStateCompat.ACTION_SEEK_TO) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_POSITION_UPDATE;
            }
            return transportControlFlags;
        }

        @Override
        void registerMediaButtonEventReceiver(PendingIntent mbrIntent, ComponentName mbrComponent) {
            // Some Android implementations are not able to register a media button event receiver
            // using a PendingIntent but need a ComponentName instead. These will raise a
            // NullPointerException.
            if (sIsMbrPendingIntentSupported) {
                try {
                    mAudioManager.registerMediaButtonEventReceiver(mbrIntent);
                } catch (NullPointerException e) {
                    Log.w(TAG, "Unable to register media button event receiver with "
                            + "PendingIntent, falling back to ComponentName.");
                    sIsMbrPendingIntentSupported = false;
                }
            }

            if (!sIsMbrPendingIntentSupported) {
                super.registerMediaButtonEventReceiver(mbrIntent, mbrComponent);
            }
        }

        @Override
        void unregisterMediaButtonEventReceiver(PendingIntent mbrIntent,
                ComponentName mbrComponent) {
            if (sIsMbrPendingIntentSupported) {
                mAudioManager.unregisterMediaButtonEventReceiver(mbrIntent);
            } else {
                super.unregisterMediaButtonEventReceiver(mbrIntent, mbrComponent);
            }
        }
    }

    @RequiresApi(19)
    static class MediaSessionImplApi19 extends MediaSessionImplApi18 {
        MediaSessionImplApi19(Context context, String tag, ComponentName mbrComponent,
                PendingIntent mbrIntent) {
            super(context, tag, mbrComponent, mbrIntent);
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            super.setCallback(callback, handler);
            if (callback == null) {
                mRcc.setMetadataUpdateListener(null);
            } else {
                RemoteControlClient.OnMetadataUpdateListener listener =
                        new RemoteControlClient.OnMetadataUpdateListener() {
                            @Override
                            public void onMetadataUpdate(int key, Object newValue) {
                                if (key == MediaMetadataEditor.RATING_KEY_BY_USER
                                        && newValue instanceof Rating) {
                                    postToHandler(MessageHandler.MSG_RATE,
                                            RatingCompat.fromRating(newValue));
                                }
                            }
                        };
                mRcc.setMetadataUpdateListener(listener);
            }
        }

        @Override
        int getRccTransportControlFlagsFromActions(long actions) {
            int transportControlFlags = super.getRccTransportControlFlagsFromActions(actions);
            if ((actions & PlaybackStateCompat.ACTION_SET_RATING) != 0) {
                transportControlFlags |= RemoteControlClient.FLAG_KEY_MEDIA_RATING;
            }
            return transportControlFlags;
        }

        @Override
        RemoteControlClient.MetadataEditor buildRccMetadata(Bundle metadata) {
            RemoteControlClient.MetadataEditor editor = super.buildRccMetadata(metadata);
            long actions = mState == null ? 0 : mState.getActions();
            if ((actions & PlaybackStateCompat.ACTION_SET_RATING) != 0) {
                editor.addEditableKey(RemoteControlClient.MetadataEditor.RATING_KEY_BY_USER);
            }

            if (metadata == null) {
                return editor;
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_YEAR)) {
                editor.putLong(MediaMetadataRetriever.METADATA_KEY_YEAR,
                        metadata.getLong(MediaMetadataCompat.METADATA_KEY_YEAR));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_RATING)) {
                // Do not remove casting here. Without this, a crash will happen in API 19.
                ((MediaMetadataEditor) editor).putObject(MediaMetadataEditor.RATING_KEY_BY_OTHERS,
                        metadata.getParcelable(MediaMetadataCompat.METADATA_KEY_RATING));
            }
            if (metadata.containsKey(MediaMetadataCompat.METADATA_KEY_USER_RATING)) {
                // Do not remove casting here. Without this, a crash will happen in API 19.
                ((MediaMetadataEditor) editor).putObject(MediaMetadataEditor.RATING_KEY_BY_USER,
                        metadata.getParcelable(MediaMetadataCompat.METADATA_KEY_USER_RATING));
            }
            return editor;
        }
    }

    @RequiresApi(21)
    static class MediaSessionImplApi21 implements MediaSessionImpl {
        private final Object mSessionObj;
        private final Token mToken;

        private boolean mDestroyed = false;
        private final RemoteCallbackList<IMediaControllerCallback> mExtraControllerCallbacks =
                new RemoteCallbackList<>();

        private PlaybackStateCompat mPlaybackState;
        private List<QueueItem> mQueue;
        private MediaMetadataCompat mMetadata;
        @RatingCompat.Style int mRatingType;
        boolean mCaptioningEnabled;
        @PlaybackStateCompat.RepeatMode int mRepeatMode;
        boolean mShuffleModeEnabled;
        @PlaybackStateCompat.ShuffleMode int mShuffleMode;

        public MediaSessionImplApi21(Context context, String tag) {
            mSessionObj = MediaSessionCompatApi21.createSession(context, tag);
            mToken = new Token(MediaSessionCompatApi21.getSessionToken(mSessionObj),
                    new ExtraSession());
        }

        public MediaSessionImplApi21(Object mediaSession) {
            mSessionObj = MediaSessionCompatApi21.verifySession(mediaSession);
            mToken = new Token(MediaSessionCompatApi21.getSessionToken(mSessionObj),
                    new ExtraSession());
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            MediaSessionCompatApi21.setCallback(mSessionObj,
                    callback == null ? null : callback.mCallbackObj, handler);
            if (callback != null) {
                callback.setSessionImpl(this, handler);
            }
        }

        @Override
        public void setFlags(@SessionFlags int flags) {
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
            if (android.os.Build.VERSION.SDK_INT < 23) {
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onEvent(event, extras);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
            }
            MediaSessionCompatApi21.sendSessionEvent(mSessionObj, event, extras);
        }

        @Override
        public void release() {
            mDestroyed = true;
            MediaSessionCompatApi21.release(mSessionObj);
        }

        @Override
        public Token getSessionToken() {
            return mToken;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            mPlaybackState = state;
            int size = mExtraControllerCallbacks.beginBroadcast();
            for (int i = size - 1; i >= 0; i--) {
                IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                try {
                    cb.onPlaybackStateChanged(state);
                } catch (RemoteException e) {
                }
            }
            mExtraControllerCallbacks.finishBroadcast();
            MediaSessionCompatApi21.setPlaybackState(mSessionObj,
                    state == null ? null : state.getPlaybackState());
        }

        @Override
        public PlaybackStateCompat getPlaybackState() {
            return mPlaybackState;
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            MediaSessionCompatApi21.setMetadata(mSessionObj,
                    metadata == null ? null : metadata.getMediaMetadata());
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            MediaSessionCompatApi21.setSessionActivity(mSessionObj, pi);
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            MediaSessionCompatApi21.setMediaButtonReceiver(mSessionObj, mbr);
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            mQueue = queue;
            List<Object> queueObjs = null;
            if (queue != null) {
                queueObjs = new ArrayList<>();
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
        public void setRatingType(@RatingCompat.Style int type) {
            if (android.os.Build.VERSION.SDK_INT < 22) {
                mRatingType = type;
            } else {
                MediaSessionCompatApi22.setRatingType(mSessionObj, type);
            }
        }

        @Override
        public void setCaptioningEnabled(boolean enabled) {
            if (mCaptioningEnabled != enabled) {
                mCaptioningEnabled = enabled;
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onCaptioningEnabledChanged(enabled);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override
        public void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
            if (mRepeatMode != repeatMode) {
                mRepeatMode = repeatMode;
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onRepeatModeChanged(repeatMode);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override
        public void setShuffleModeEnabled(boolean enabled) {
            if (mShuffleModeEnabled != enabled) {
                mShuffleModeEnabled = enabled;
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onShuffleModeChangedDeprecated(enabled);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
            }
        }

        @Override
        public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
            if (mShuffleMode != shuffleMode) {
                mShuffleMode = shuffleMode;
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onShuffleModeChanged(shuffleMode);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
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

        @Override
        public String getCallingPackage() {
            if (android.os.Build.VERSION.SDK_INT < 24) {
                return null;
            } else {
                return MediaSessionCompatApi24.getCallingPackage(mSessionObj);
            }
        }

        class ExtraSession extends IMediaSession.Stub {
            @Override
            public void sendCommand(String command, Bundle args, ResultReceiverWrapper cb) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public boolean sendMediaButton(KeyEvent mediaButton) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void registerCallbackListener(IMediaControllerCallback cb) {
                if (!mDestroyed) {
                    mExtraControllerCallbacks.register(cb);
                }
            }

            @Override
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                mExtraControllerCallbacks.unregister(cb);
            }

            @Override
            public String getPackageName() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public String getTag() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public PendingIntent getLaunchPendingIntent() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            @SessionFlags
            public long getFlags() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public ParcelableVolumeInfo getVolumeAttributes() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void adjustVolume(int direction, int flags, String packageName) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setVolumeTo(int value, int flags, String packageName) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepare() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepareFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepareFromSearch(String query, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepareFromUri(Uri uri, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void play() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromMediaId(String mediaId, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromSearch(String query, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromUri(Uri uri, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void skipToQueueItem(long id) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void pause() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void stop() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void next() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void previous() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void fastForward() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rewind() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void seekTo(long pos) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rate(RatingCompat rating) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rateWithExtras(RatingCompat rating, Bundle extras) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setCaptioningEnabled(boolean enabled) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setRepeatMode(int repeatMode) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setShuffleModeEnabledDeprecated(boolean enabled) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setShuffleMode(int shuffleMode) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void sendCustomAction(String action, Bundle args) throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public MediaMetadataCompat getMetadata() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public PlaybackStateCompat getPlaybackState() {
                return getStateWithUpdatedPosition(mPlaybackState, mMetadata);
            }

            @Override
            public List<QueueItem> getQueue() {
                // Will not be called.
                return null;
            }

            @Override
            public void addQueueItem(MediaDescriptionCompat descriptionCompat) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void addQueueItemAt(MediaDescriptionCompat descriptionCompat, int index) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void removeQueueItem(MediaDescriptionCompat description) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void removeQueueItemAt(int index) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public CharSequence getQueueTitle() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public Bundle getExtras() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            @RatingCompat.Style
            public int getRatingType() {
                return mRatingType;
            }

            @Override
            public boolean isCaptioningEnabled() {
                return mCaptioningEnabled;
            }

            @Override
            @PlaybackStateCompat.RepeatMode
            public int getRepeatMode() {
                return mRepeatMode;
            }

            @Override
            public boolean isShuffleModeEnabledDeprecated() {
                return mShuffleModeEnabled;
            }

            @Override
            @PlaybackStateCompat.ShuffleMode
            public int getShuffleMode() {
                return mShuffleMode;
            }

            @Override
            public boolean isTransportControlEnabled() {
                // Will not be called.
                throw new AssertionError();
            }
        }
    }
}
