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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.media.MediaSessionManager.RemoteUserInfo.LEGACY_CONTROLLER;
import static androidx.media.MediaSessionManager.RemoteUserInfo.UNKNOWN_PID;
import static androidx.media.MediaSessionManager.RemoteUserInfo.UNKNOWN_UID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.media.MediaSessionManager;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media.VolumeProviderCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows interaction with media controllers, volume keys, media buttons, and transport controls.
 *
 * <p>A MediaSession should be created when an app wants to publish media playback information or
 * handle media keys. In general an app only needs one session for all playback, though multiple
 * sessions can be created to provide finer grain controls of media.
 *
 * <p>Once a session is created the owner of the session may pass its {@link #getSessionToken()
 * session token} to other processes to allow them to create a {@link MediaControllerCompat} to
 * interact with the session.
 *
 * <p>To receive commands, media keys, and other events a {@link Callback} must be set with {@link
 * #setCallback(Callback)}.
 *
 * <p>When an app is finished performing playback it must call {@link #release()} to clean up the
 * session and notify any controllers.
 *
 * <p>MediaSessionCompat objects are not thread safe and all calls should be made from the same
 * thread.
 *
 * <p>This is a helper for accessing features in {@link android.media.session.MediaSession}
 * introduced after API level 4 in a backwards compatible fashion.
 *
 * <p><div class="special reference">
 *
 * <h3>Developer Guides</h3>
 *
 * <p>For information about building your media application, read the <a
 * href="{@docRoot}guide/topics/media-apps/index.html">Media Apps</a> developer guide. </div>
 *
 * @deprecated androidx.media is deprecated. Please migrate to <a
 *     href="https://developer.android.com/media/media3">androidx.media3</a>.
 */
@Deprecated
public class MediaSessionCompat {
    static final String TAG = "MediaSessionCompat";

    private final MediaSessionImpl mImpl;
    private final MediaControllerCompat mController;
    private final ArrayList<OnActiveChangeListener> mActiveListeners = new ArrayList<>();

    @IntDef(flag=true, value={
            FLAG_HANDLES_MEDIA_BUTTONS,
            FLAG_HANDLES_TRANSPORT_CONTROLS,
            FLAG_HANDLES_QUEUE_COMMANDS })
    @Retention(RetentionPolicy.SOURCE)
    private @interface SessionFlags {}

    /**
     * Sets this flag on the session to indicate that it can handle media button
     * events.
     *
     * @deprecated This flag is no longer used. All media sessions are expected to handle media
     * button events now. For backward compatibility, this flag will be always set.
     */
    @Deprecated
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1 << 0;

    /**
     * Sets this flag on the session to indicate that it handles transport
     * control commands through its {@link Callback}.
     *
     * @deprecated This flag is no longer used. All media sessions are expected to handle transport
     * controls now. For backward compatibility, this flag will be always set.
     */
    @Deprecated
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
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_PLAY_FROM_URI =
            "android.support.v4.media.session.action.PLAY_FROM_URI";

    /**
     * Custom action to invoke prepare() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_PREPARE = "android.support.v4.media.session.action.PREPARE";

    /**
     * Custom action to invoke prepareFromMediaId() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_PREPARE_FROM_MEDIA_ID =
            "android.support.v4.media.session.action.PREPARE_FROM_MEDIA_ID";

    /**
     * Custom action to invoke prepareFromSearch() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_PREPARE_FROM_SEARCH =
            "android.support.v4.media.session.action.PREPARE_FROM_SEARCH";

    /**
     * Custom action to invoke prepareFromUri() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_PREPARE_FROM_URI =
            "android.support.v4.media.session.action.PREPARE_FROM_URI";

    /**
     * Custom action to invoke setCaptioningEnabled() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_SET_CAPTIONING_ENABLED =
            "android.support.v4.media.session.action.SET_CAPTIONING_ENABLED";

    /**
     * Custom action to invoke setRepeatMode() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_SET_REPEAT_MODE =
            "android.support.v4.media.session.action.SET_REPEAT_MODE";

    /**
     * Custom action to invoke setShuffleMode() for the forward compatibility.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_SET_SHUFFLE_MODE =
            "android.support.v4.media.session.action.SET_SHUFFLE_MODE";

    /**
     * Custom action to invoke setRating() with extra fields.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_SET_RATING =
            "android.support.v4.media.session.action.SET_RATING";

    /**
     * Custom action to invoke setPlaybackSpeed() with extra fields.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_SET_PLAYBACK_SPEED =
            "android.support.v4.media.session.action.SET_PLAYBACK_SPEED";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_MEDIA_ID} indicating media id to play.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_MEDIA_ID =
            "android.support.v4.media.session.action.ARGUMENT_MEDIA_ID";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_SEARCH} indicating search query.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_QUERY =
            "android.support.v4.media.session.action.ARGUMENT_QUERY";

    /**
     * Argument for use with {@link #ACTION_PREPARE_FROM_URI} and {@link #ACTION_PLAY_FROM_URI}
     * indicating URI to play.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_URI =
            "android.support.v4.media.session.action.ARGUMENT_URI";

    /**
     * Argument for use with {@link #ACTION_SET_RATING} indicating the rate to be set.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_RATING =
            "android.support.v4.media.session.action.ARGUMENT_RATING";

    /**
     * Argument for use with {@link #ACTION_SET_PLAYBACK_SPEED} indicating the speed to be set.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_PLAYBACK_SPEED =
            "android.support.v4.media.session.action.ARGUMENT_PLAYBACK_SPEED";

    /**
     * Argument for use with various actions indicating extra bundle.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_EXTRAS =
            "android.support.v4.media.session.action.ARGUMENT_EXTRAS";

    /**
     * Argument for use with {@link #ACTION_SET_CAPTIONING_ENABLED} indicating whether captioning is
     * enabled.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_CAPTIONING_ENABLED =
            "android.support.v4.media.session.action.ARGUMENT_CAPTIONING_ENABLED";

    /**
     * Argument for use with {@link #ACTION_SET_REPEAT_MODE} indicating repeat mode.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_REPEAT_MODE =
            "android.support.v4.media.session.action.ARGUMENT_REPEAT_MODE";

    /**
     * Argument for use with {@link #ACTION_SET_SHUFFLE_MODE} indicating shuffle mode.
     *
     */
    @RestrictTo(LIBRARY)
    public static final String ACTION_ARGUMENT_SHUFFLE_MODE =
            "android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE";

    /**
     */
    @RestrictTo(LIBRARY)
    public static final String KEY_TOKEN = "android.support.v4.media.session.TOKEN";

    /**
     */
    @RestrictTo(LIBRARY)
    public static final String KEY_EXTRA_BINDER =
            "android.support.v4.media.session.EXTRA_BINDER";

    /**
     */
    @RestrictTo(LIBRARY)
    public static final String KEY_SESSION2_TOKEN =
            "android.support.v4.media.session.SESSION_TOKEN2";

    // Bitmap dimension limit in pixels
    private static class MediaMetadataBitmapMaxSizeHolder {
        static final int value = getMediaMetadataBitmapMaxSize();
    }

    @SuppressWarnings("DiscouragedApi") // Using Resources.getIdentifier() is less efficient
    private static int getMediaMetadataBitmapMaxSize() {
        Resources res = Resources.getSystem();
        int limit = res.getDisplayMetrics().widthPixels;
        try {
            int id = res.getIdentifier("config_mediaMetadataBitmapMaxSize", "dimen", "android");
            limit = res.getDimensionPixelSize(id);
        } catch (Resources.NotFoundException e) {
            // do nothing
        }
        return limit;
    }

    /**
     * Gets the bitmap dimension limit in pixels. Bitmaps with width or height larger than this
     * will be scaled down to fit within the limit.
     *
     * Apps are recommended to use bitmaps whose width or height is no larger than this limit.
     * Otherwise, for example, when setting a bitmap in MediaSession metadata {@link
     * MediaSession.setMetadata()}, a downscaling will be performed, which incurs performance
     * cost as well as an intermediate scaled down bitmap.
     *
     * Example usage:
     *
     * <pre>{@code
     *   int width = calculateRequestBitmapWidthPx();
     *   int height = calculateRequestBitmapHeightPx();
     *   final int limit = mediaSession.getBitmapDimensionLimit();
     *   if (width >= limit || height >= limit) {
     *     float scale = Math.max((float)(width) / limit,
     *                            (float)(height) / limit);
     *     if (scale < THRESHOLD) {
     *       width = (int)(width * scale);
     *       height = (int)(height * scale);
     *     }
     *   }
     *
     *   Bitmap bitmap = BitmapLoader.load(uri, width, height);
     * }</pre>
     */
    public static int getBitmapDimensionLimit() {
        return MediaMetadataBitmapMaxSizeHolder.value;
    }

    /**
     * Scales down the given bitmap if its width or height exceeds the limit returned by
     * {@link #getBitmapDimensionLimit()}. The bitmap will be scaled to fit within the limit
     * while maintaining its aspect ratio. On API 31+, the returned bitmap will be a shared
     * bitmap.
     *
     * @param bitmap The bitmap to potentially scale.
     * @return The original bitmap if it's within the dimension limit, or a scaled-down version.
     * @see #getBitmapDimensionLimit()
     */
    public static Bitmap getScaledBitmap(Bitmap bitmap) {
        return getScaledBitmap(bitmap, getBitmapDimensionLimit());
    }

    /**
     * Scales down the given bitmap if its width or height exceeds either the provided
     * {@code dimensionLimit} or the limit returned by {@link #getBitmapDimensionLimit()}.
     * The bitmap will be scaled to fit within that limit while maintaining its aspect ratio.
     * On API 31+, the returned bitmap will be a shared bitmap.
     *
     * @param bitmap The bitmap to potentially scale.
     * @param dimensionLimit The maximum allowed width or height for the bitmap.
     * @return The original bitmap if it's within the dimension limit, or a scaled-down version.
     */
    public static Bitmap getScaledBitmap(Bitmap bitmap, int dimensionLimit) {
        int limit = Math.min(getBitmapDimensionLimit(), dimensionLimit);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > limit || height > limit) {
            float scale = Math.min((float) limit / width, (float) limit / height);
            width = (int)(width * scale);
            height = (int)(height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, /* filter= */ true);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            return bitmap.asShared();
        }
        return bitmap;
    }

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
    public MediaSessionCompat(@NonNull Context context, @NonNull String tag) {
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
    public MediaSessionCompat(@NonNull Context context, @NonNull String tag,
            @Nullable ComponentName mbrComponent, @Nullable PendingIntent mbrIntent) {
        this(context, tag, mbrComponent, mbrIntent, null);
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
     * The {@code sessionInfo} can include additional unchanging information about this session.
     * For example, it can include the version of the application, or other app-specific
     * unchanging information.
     *
     * @param context The context to use to create the session.
     * @param tag A short name for debugging purposes.
     * @param mbrComponent The component name for your media button receiver.
     * @param mbrIntent The PendingIntent for your receiver component that handles
     *            media button events. This is optional and will be used on between
     *            {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR2} and
     *            {@link android.os.Build.VERSION_CODES#KITKAT_WATCH} instead of the
     *            component name.
     * @param sessionInfo A bundle for additional information about this session,
     *                    or {@link Bundle#EMPTY} if none. Controllers can get this information
     *                    by calling {@link MediaControllerCompat#getSessionInfo()}. An
     *                    {@link IllegalArgumentException} will be thrown if this contains any
     *                    non-framework Parcelable objects.
     */
    public MediaSessionCompat(@NonNull Context context, @NonNull String tag,
            @Nullable ComponentName mbrComponent, @Nullable PendingIntent mbrIntent,
            @Nullable Bundle sessionInfo) {
        this(context, tag, mbrComponent, mbrIntent, sessionInfo, null /* session2Token */);
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
    public MediaSessionCompat(@NonNull Context context, @NonNull String tag,
            @Nullable ComponentName mbrComponent, @Nullable PendingIntent mbrIntent,
            @Nullable Bundle sessionInfo, @Nullable VersionedParcelable session2Token) {
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
                    0/* requestCode, ignored */, mediaButtonIntent,
                    Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        }

        if (Build.VERSION.SDK_INT >= 29) {
            mImpl = new MediaSessionImplApi29(context, tag, session2Token, sessionInfo);
        } else if (Build.VERSION.SDK_INT >= 28) {
            mImpl = new MediaSessionImplApi28(context, tag, session2Token, sessionInfo);
        } else if (Build.VERSION.SDK_INT >= 22) {
            mImpl = new MediaSessionImplApi22(context, tag, session2Token, sessionInfo);
        } else {
            mImpl = new MediaSessionImplApi21(context, tag, session2Token, sessionInfo);
        }
        // Set default callback to respond to controllers' extra binder requests.
        Handler handler = new Handler(Looper.myLooper() != null
                ? Looper.myLooper() : Looper.getMainLooper());
        setCallback(new Callback() {}, handler);
        mImpl.setMediaButtonReceiver(mbrIntent);
        mController = new MediaControllerCompat(context, this);
    }

    private MediaSessionCompat(Context context, MediaSessionImpl impl) {
        mImpl = impl;
        mController = new MediaControllerCompat(context, this);
    }

    /**
     * Adds a callback to receive updates on for the MediaSession. This includes
     * media button and volume events. The caller's thread will be used to post
     * events. Set the callback to null to stop receiving events.
     * <p>
     * Don't reuse the callback among the sessions. Callbacks keep internal reference to the
     * session when it's set, so it may misbehave.
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
     * <p>
     * Don't reuse the callback among the sessions. Callbacks keep internal reference to the
     * session when it's set, so it may misbehave.
     *
     * @param callback The callback to receive updates on.
     * @param handler The handler that events should be posted on.
     */
    @SuppressWarnings("deprecation")
    public void setCallback(Callback callback, Handler handler) {
        if (callback == null) {
            mImpl.setCallback(null, null);
        } else {
            mImpl.setCallback(callback, handler != null ? handler : new Handler());
        }
    }

    /**
     * Sets the {@link RegistrationCallback}.
     *
     * @param callback callback to listener callback registration. Can be null to stop.
     * @param handler handler
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media3-session
    public void setRegistrationCallback(
            @Nullable RegistrationCallback callback, @NonNull Handler handler) {
        mImpl.setRegistrationCallback(callback, handler);
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
     * exist. The id of each item should be unique within the play queue.
     * Specify null if there is no current play queue.
     * <p>
     * The queue should be of reasonable size. If the play queue is unbounded
     * within your app, it is better to send a reasonable amount in a sliding
     * window instead.
     *
     * @param queue A list of items in the play queue.
     */
    public void setQueue(List<QueueItem> queue) {
        if (queue != null) {
            Set<Long> set = new HashSet<>();
            for (QueueItem item : queue) {
                if (item == null) {
                    throw new IllegalArgumentException("queue shouldn't have null items");
                }
                if (set.contains(item.getQueueId())) {
                    Log.e(TAG, "Found duplicate queue id: " + item.getQueueId(),
                            new IllegalArgumentException("id of each queue item should be unique"));
                }
                set.add(item.getQueueId());
            }
        }
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
     * Gets the controller information who sent the current request.
     * <p>
     * Note: This is only valid while in a request callback, such as {@link Callback#onPlay}.
     * <p>
     * Note: From API 21 to 23, this method returns a fake {@link RemoteUserInfo} which has
     * following values:
     * <ul>
     *     <li>Package name is {@link MediaSessionManager.RemoteUserInfo#LEGACY_CONTROLLER}.</li>
     *     <li>PID and UID will have negative values.</li>
     * </ul>
     * <p>
     * Note: From API 24 to 27, the {@link RemoteUserInfo} returned from this method will have
     * negative uid and pid. Most of the cases it will have the correct package name, but sometimes
     * it will fail to get the right one.
     *
     * @see MediaSessionManager.RemoteUserInfo#LEGACY_CONTROLLER
     * @see MediaSessionManager#isTrustedForMediaControl(RemoteUserInfo)
     */
    public final @NonNull RemoteUserInfo getCurrentControllerInfo() {
        return mImpl.getCurrentControllerInfo();
    }

    /**
     * Returns the name of the package that sent the last media button, transport control, or
     * command from controllers and the system. This is only valid while in a request callback, such
     * as {@link Callback#onPlay}. This method is not available and returns null on pre-N devices.
     *
     */
    @RestrictTo(LIBRARY)
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
     * <p>
     * Note: A {@link MediaSessionCompat} object returned from this method may not provide the full
     * functionality of {@link MediaSessionCompat} until setting a new
     * {@link MediaSessionCompat.Callback}. To avoid this, when both a {@link MediaSessionCompat}
     * and a framework {@link android.media.session.MediaSession} are needed, it is recommended
     * to create a {@link MediaSessionCompat} first and get the framework session through
     * {@link #getMediaSession()}.
     *
     * @param context The context to use to create the session.
     * @param mediaSession A {@link android.media.session.MediaSession} object.
     * @return An equivalent {@link MediaSessionCompat} object, or null if none.
     */
    public static MediaSessionCompat fromMediaSession(Context context, Object mediaSession) {
        if (context == null || mediaSession == null) {
            return null;
        }
        MediaSessionImpl impl;
        if (Build.VERSION.SDK_INT >= 29) {
            impl = new MediaSessionImplApi29(mediaSession);
        } else if (Build.VERSION.SDK_INT >= 28) {
            impl = new MediaSessionImplApi28(mediaSession);
        } else {
            // API 21+
            impl = new MediaSessionImplApi21(mediaSession);
        }
        return new MediaSessionCompat(context, impl);
    }

    /**
     * A helper method for setting the application class loader to the given {@link Bundle}.
     *
     */
    @RestrictTo(LIBRARY)
    public static void ensureClassLoader(@Nullable Bundle bundle) {
        if (bundle != null) {
            bundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
        }
    }

    /**
     * Tries to unparcel the given {@link Bundle} with the application class loader and
     * returns {@code null} if a {@link BadParcelableException} is thrown while unparcelling,
     * otherwise the given bundle in which the application class loader is set.
     *
     */
    @RestrictTo(LIBRARY)
    public static @Nullable Bundle unparcelWithClassLoader(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        ensureClassLoader(bundle);
        try {
            bundle.isEmpty(); // to call unparcel()
            return bundle;
        } catch (BadParcelableException e) {
            // The exception details will be logged by Parcel class.
            Log.e(TAG, "Could not unparcel the data.");
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static PlaybackStateCompat getStateWithUpdatedPosition(
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
     * Receives transport controls, media buttons, and commands from controllers and the system. The
     * callback may be set using {@link #setCallback}.
     *
     * <p>Don't reuse the callback among the sessions. Callbacks keep internal reference to the
     * session when it's set, so it may misbehave.
     *
     * @deprecated androidx.media is deprecated. Please migrate to <a
     *     href="https://developer.android.com/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public abstract static class Callback {
        final Object mLock = new Object();
        final MediaSession.Callback mCallbackFwk;
        private boolean mMediaPlayPausePendingOnHandler;

        @GuardedBy("mLock")
        WeakReference<MediaSessionImpl> mSessionImpl;
        @GuardedBy("mLock")
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        CallbackHandler mCallbackHandler;

        public Callback() {
            mCallbackFwk = new MediaSessionCallbackApi21();
            mSessionImpl = new WeakReference<>(null);
        }

        void setSessionImpl(MediaSessionImpl impl, Handler handler) {
            synchronized (mLock) {
                mSessionImpl = new WeakReference<MediaSessionImpl>(impl);
                if (mCallbackHandler != null) {
                    mCallbackHandler.removeCallbacksAndMessages(null);
                }
                mCallbackHandler = impl == null || handler == null ? null :
                        new CallbackHandler(handler.getLooper());
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
         * <p>
         * The double tap of {@link KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE} or {@link
         * KeyEvent#KEYCODE_HEADSETHOOK} will call the {@link #onSkipToNext} by default. If the
         * current SDK level is 27 or higher, the default double tap handling is done by framework
         * so this method would do nothing for it.
         *
         * @param mediaButtonEvent The media button event intent.
         * @return True if the event was handled, false otherwise.
         */
        @SuppressWarnings("deprecation")
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (android.os.Build.VERSION.SDK_INT >= 27) {
                // Double tap of play/pause as skipping to next is already handled by framework,
                // so we don't need to repeat again here.
                // Note: Double tap would be handled twice for OC-DR1 whose SDK version 26 and
                //       framework handles the double tap.
                return false;
            }
            MediaSessionImpl impl;
            Handler callbackHandler;
            synchronized (mLock) {
                impl = mSessionImpl.get();
                callbackHandler = mCallbackHandler;
            }
            if (impl == null || callbackHandler == null) {
                return false;
            }
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            RemoteUserInfo remoteUserInfo = impl.getCurrentControllerInfo();
            int keyCode = keyEvent.getKeyCode();
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (keyEvent.getRepeatCount() == 0) {
                        if (mMediaPlayPausePendingOnHandler) {
                            callbackHandler.removeMessages(
                                    CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
                            mMediaPlayPausePendingOnHandler = false;
                            PlaybackStateCompat state = impl.getPlaybackState();
                            long validActions = state == null ? 0 : state.getActions();
                            // Consider double tap as the next.
                            if ((validActions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
                                onSkipToNext();
                            }
                        } else {
                            mMediaPlayPausePendingOnHandler = true;
                            callbackHandler.sendMessageDelayed(callbackHandler.obtainMessage(
                                    CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT,
                                    remoteUserInfo),
                                    ViewConfiguration.getDoubleTapTimeout());
                        }
                    } else {
                        // Consider long-press as a single tap.
                        handleMediaPlayPauseIfPendingOnHandler(impl, callbackHandler);
                    }
                    return true;
                default:
                    // If another key is pressed within double tap timeout, consider the pending
                    // pending play/pause as a single tap to handle media keys in order.
                    handleMediaPlayPauseIfPendingOnHandler(impl, callbackHandler);
                    break;
            }
            return false;
        }

        void handleMediaPlayPauseIfPendingOnHandler(MediaSessionImpl impl,
                Handler callbackHandler) {
            if (!mMediaPlayPausePendingOnHandler) {
                return;
            }
            mMediaPlayPausePendingOnHandler = false;
            callbackHandler.removeMessages(
                    CallbackHandler.MSG_MEDIA_PLAY_PAUSE_KEY_DOUBLE_TAP_TIMEOUT);
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
         * Override to handle the playback speed change.
         * To update the new playback speed, create a new {@link PlaybackStateCompat} by using
         * {@link PlaybackStateCompat.Builder#setState(int, long, float)}, and set it with
         * {@link #setPlaybackState(PlaybackStateCompat)}.
         * <p>
         * A value of {@code 1.0f} is the default playback value, and a negative value indicates
         * reverse playback. The {@code speed} will not be equal to zero.
         *
         * @param speed the playback speed
         * @see #setPlaybackState(PlaybackStateCompat)
         * @see PlaybackStateCompat.Builder#setState(int, long, float)
         */
        public void onSetPlaybackSpeed(float speed) {
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
                    // Here we manually set the caller info, since this is not directly called from
                    // the session callback. This is triggered by timeout.
                    MediaSessionImpl impl;
                    Handler callbackHandler;
                    synchronized (mLock) {
                        impl = mSessionImpl.get();
                        callbackHandler = mCallbackHandler;
                    }
                    if (impl == null
                            || MediaSessionCompat.Callback.this != impl.getCallback()
                            || callbackHandler == null) {
                        return;
                    }
                    RemoteUserInfo info = (RemoteUserInfo) msg.obj;
                    impl.setCurrentControllerInfo(info);
                    handleMediaPlayPauseIfPendingOnHandler(impl, callbackHandler);
                    impl.setCurrentControllerInfo(null);
                }
            }
        }

        private class MediaSessionCallbackApi21 extends MediaSession.Callback {
            MediaSessionCallbackApi21() {
            }

            @Override
            @SuppressWarnings("deprecation")
            public void onCommand(String command, Bundle extras, ResultReceiver cb) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                try {
                    if (command.equals(MediaControllerCompat.COMMAND_GET_EXTRA_BINDER)) {
                        Bundle result = new Bundle();
                        Token token = sessionImpl.getSessionToken();
                        IMediaSession extraBinder = token.getExtraBinder();
                        result.putBinder(KEY_EXTRA_BINDER,
                                extraBinder == null ? null : extraBinder.asBinder());
                        ParcelUtils.putVersionedParcelable(result,
                                KEY_SESSION2_TOKEN, token.getSession2Token());
                        cb.send(0, result);
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM)) {
                        Callback.this.onAddQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_ADD_QUEUE_ITEM_AT)) {
                        Callback.this.onAddQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION),
                                extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM)) {
                        Callback.this.onRemoveQueueItem(
                                (MediaDescriptionCompat) extras.getParcelable(
                                        MediaControllerCompat.COMMAND_ARGUMENT_MEDIA_DESCRIPTION));
                    } else if (command.equals(MediaControllerCompat.COMMAND_REMOVE_QUEUE_ITEM_AT)) {
                        if (sessionImpl.mQueue != null) {
                            int index =
                                    extras.getInt(MediaControllerCompat.COMMAND_ARGUMENT_INDEX, -1);
                            QueueItem item = (index >= 0 && index < sessionImpl.mQueue.size())
                                    ? sessionImpl.mQueue.get(index) : null;
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
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return false;
                }
                setCurrentControllerInfo(sessionImpl);
                boolean result = Callback.this.onMediaButtonEvent(mediaButtonIntent);
                clearCurrentControllerInfo(sessionImpl);
                return result || super.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onPlay() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPlay();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onPlayFromMediaId(String mediaId, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPlayFromMediaId(mediaId, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onPlayFromSearch(String search, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPlayFromSearch(search, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(23)
            @Override
            public void onPlayFromUri(Uri uri, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPlayFromUri(uri, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onSkipToQueueItem(long id) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSkipToQueueItem(id);
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onPause() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPause();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onSkipToNext() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSkipToNext();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onSkipToPrevious() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSkipToPrevious();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onFastForward() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onFastForward();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onRewind() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onRewind();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onStop() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onStop();
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onSeekTo(long pos) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSeekTo(pos);
                clearCurrentControllerInfo(sessionImpl);
            }

            @Override
            public void onSetRating(Rating ratingFwk) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSetRating(RatingCompat.fromRating(ratingFwk));
                clearCurrentControllerInfo(sessionImpl);
            }
            @Override
            @SuppressWarnings("deprecation")
            public void onCustomAction(String action, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);

                try {
                    if (action.equals(ACTION_PLAY_FROM_URI)) {
                        Uri uri = extras.getParcelable(ACTION_ARGUMENT_URI);
                        Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                        ensureClassLoader(bundle);
                        Callback.this.onPlayFromUri(uri, bundle);
                    } else if (action.equals(ACTION_PREPARE)) {
                        Callback.this.onPrepare();
                    } else if (action.equals(ACTION_PREPARE_FROM_MEDIA_ID)) {
                        String mediaId = extras.getString(ACTION_ARGUMENT_MEDIA_ID);
                        Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                        ensureClassLoader(bundle);
                        Callback.this.onPrepareFromMediaId(mediaId, bundle);
                    } else if (action.equals(ACTION_PREPARE_FROM_SEARCH)) {
                        String query = extras.getString(ACTION_ARGUMENT_QUERY);
                        Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                        ensureClassLoader(bundle);
                        Callback.this.onPrepareFromSearch(query, bundle);
                    } else if (action.equals(ACTION_PREPARE_FROM_URI)) {
                        Uri uri = extras.getParcelable(ACTION_ARGUMENT_URI);
                        Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                        ensureClassLoader(bundle);
                        Callback.this.onPrepareFromUri(uri, bundle);
                    } else if (action.equals(ACTION_SET_CAPTIONING_ENABLED)) {
                        boolean enabled = extras.getBoolean(ACTION_ARGUMENT_CAPTIONING_ENABLED);
                        Callback.this.onSetCaptioningEnabled(enabled);
                    } else if (action.equals(ACTION_SET_REPEAT_MODE)) {
                        int repeatMode = extras.getInt(ACTION_ARGUMENT_REPEAT_MODE);
                        Callback.this.onSetRepeatMode(repeatMode);
                    } else if (action.equals(ACTION_SET_SHUFFLE_MODE)) {
                        int shuffleMode = extras.getInt(ACTION_ARGUMENT_SHUFFLE_MODE);
                        Callback.this.onSetShuffleMode(shuffleMode);
                    } else if (action.equals(ACTION_SET_RATING)) {
                        RatingCompat rating = extras.getParcelable(ACTION_ARGUMENT_RATING);
                        Bundle bundle = extras.getBundle(ACTION_ARGUMENT_EXTRAS);
                        ensureClassLoader(bundle);
                        Callback.this.onSetRating(rating, bundle);
                    } else if (action.equals(ACTION_SET_PLAYBACK_SPEED)) {
                        float speed = extras.getFloat(ACTION_ARGUMENT_PLAYBACK_SPEED, 1.0f);
                        Callback.this.onSetPlaybackSpeed(speed);
                    } else {
                        Callback.this.onCustomAction(action, extras);
                    }
                } catch (BadParcelableException e) {
                    // The exception details will be logged by Parcel class.
                    Log.e(TAG, "Could not unparcel the data.");
                }
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(24)
            @Override
            public void onPrepare() {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPrepare();
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(24)
            @Override
            public void onPrepareFromMediaId(String mediaId, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPrepareFromMediaId(mediaId, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(24)
            @Override
            public void onPrepareFromSearch(String query, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPrepareFromSearch(query, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(24)
            @Override
            public void onPrepareFromUri(Uri uri, Bundle extras) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                ensureClassLoader(extras);
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onPrepareFromUri(uri, extras);
                clearCurrentControllerInfo(sessionImpl);
            }

            @RequiresApi(29)
            @Override
            public void onSetPlaybackSpeed(float speed) {
                MediaSessionImplApi21 sessionImpl = getSessionImplIfCallbackIsSet();
                if (sessionImpl == null) {
                    return;
                }
                setCurrentControllerInfo(sessionImpl);
                Callback.this.onSetPlaybackSpeed(speed);
                clearCurrentControllerInfo(sessionImpl);
            }

            private void setCurrentControllerInfo(MediaSessionImpl sessionImpl) {
                if (Build.VERSION.SDK_INT >= 28) {
                    // From API 28, this method has no effect since
                    // MediaSessionImplApi28#getCurrentControllerInfo() returns controller info from
                    // framework.
                    return;
                }
                String packageName = sessionImpl.getCallingPackage();
                if (TextUtils.isEmpty(packageName)) {
                    packageName = LEGACY_CONTROLLER;
                }
                sessionImpl.setCurrentControllerInfo(new RemoteUserInfo(
                        packageName, UNKNOWN_PID, UNKNOWN_UID));
            }

            private void clearCurrentControllerInfo(MediaSessionImpl sessionImpl) {
                sessionImpl.setCurrentControllerInfo(null);
            }

            // Returns the MediaSessionImplApi21 if this callback is still set by the session.
            // This prevent callback methods to be called after session is release() or
            // callback is changed.
            private MediaSessionImplApi21 getSessionImplIfCallbackIsSet() {
                MediaSessionImplApi21 sessionImpl;
                synchronized (mLock) {
                    sessionImpl = (MediaSessionImplApi21) mSessionImpl.get();
                }
                return sessionImpl != null
                        && MediaSessionCompat.Callback.this == sessionImpl.getCallback()
                        ? sessionImpl : null;
            }
        }
    }

    /**
     * Callback to be called when a controller has registered or unregistered controller callback.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
    public interface RegistrationCallback {
        /**
         * Called when a {@link MediaControllerCompat} registered callback.
         *
         * @param callingPid PID from Binder#getCallingPid()
         * @param callingUid UID from Binder#getCallingUid()
         */
        void onCallbackRegistered(int callingPid, int callingUid);

        /**
         * Called when a {@link MediaControllerCompat} unregistered callback.
         *
         * @param callingPid PID from Binder#getCallingPid()
         * @param callingUid UID from Binder#getCallingUid()
         */
        void onCallbackUnregistered(int callingPid, int callingUid);
    }

    /**
     * Represents an ongoing session. This may be passed to apps by the session owner to allow them
     * to create a {@link MediaControllerCompat} to communicate with the session.
     *
     * @deprecated androidx.media is deprecated. Please migrate to <a
     *     href="https://developer.android.com/media/media3">androidx.media3</a>.
     */
    @Deprecated
    @SuppressLint("BanParcelableUsage")
    public static final class Token implements Parcelable {
        private final Object mLock = new Object();
        private final Object mInner;

        @GuardedBy("mLock")
        private IMediaSession mExtraBinder;
        @GuardedBy("mLock")
        private VersionedParcelable mSession2Token;

        Token(Object inner) {
            this(inner, null, null);
        }

        Token(Object inner, IMediaSession extraBinder) {
            this(inner, extraBinder, null);
        }

        Token(Object inner, IMediaSession extraBinder, VersionedParcelable session2Token) {
            mInner = inner;
            mExtraBinder = extraBinder;
            mSession2Token = session2Token;
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
         */
        @RestrictTo(LIBRARY)
        public static Token fromToken(Object token, IMediaSession extraBinder) {
            if (token != null) {
                if (!(token instanceof MediaSession.Token)) {
                    throw new IllegalArgumentException(
                            "token is not a valid MediaSession.Token object");
                }
                return new Token(token, extraBinder);
            }
            return null;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable((Parcelable) mInner, flags);
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
         */
        @RestrictTo(LIBRARY)
        public IMediaSession getExtraBinder() {
            synchronized (mLock) {
                return mExtraBinder;
            }
        }

        /**
         */
        @RestrictTo(LIBRARY)
        public void setExtraBinder(IMediaSession extraBinder) {
            synchronized (mLock) {
                mExtraBinder = extraBinder;
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
        public VersionedParcelable getSession2Token() {
            synchronized (mLock) {
                return mSession2Token;
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
        public void setSession2Token(VersionedParcelable session2Token) {
            synchronized (mLock) {
                mSession2Token = session2Token;
            }
        }

        /**
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_TOKEN, this);
            synchronized (mLock) {
                if (mExtraBinder != null) {
                    bundle.putBinder(KEY_EXTRA_BINDER, mExtraBinder.asBinder());
                }
                if (mSession2Token != null) {
                    ParcelUtils.putVersionedParcelable(bundle, KEY_SESSION2_TOKEN, mSession2Token);
                }
            }
            return bundle;
        }

        /**
         * Creates a compat Token from a bundle object.
         *
         * @param tokenBundle
         * @return A compat Token for use with {@link MediaControllerCompat}.
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX) // accessed by media2-session
        @SuppressWarnings("deprecation")
        public static Token fromBundle(Bundle tokenBundle) {
            if (tokenBundle == null) {
                return null;
            }
            tokenBundle.setClassLoader(Token.class.getClassLoader());
            IMediaSession extraSession = IMediaSession.Stub.asInterface(
                    tokenBundle.getBinder(KEY_EXTRA_BINDER));
            VersionedParcelable session2Token = ParcelUtils.getVersionedParcelable(tokenBundle,
                    KEY_SESSION2_TOKEN);
            Token token = tokenBundle.getParcelable(KEY_TOKEN);
            return token == null ? null : new Token(token.mInner, extraSession, session2Token);
        }

        public static final Parcelable.Creator<Token> CREATOR
                = new Parcelable.Creator<Token>() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public Token createFromParcel(Parcel in) {
                        Object inner;
                        inner = in.readParcelable(null);
                        return new Token(inner);
                    }

                    @Override
                    public Token[] newArray(int size) {
                        return new Token[size];
                    }
        };
    }

    /**
     * A single item that is part of the play queue. It contains a description of the item and its
     * id in the queue.
     *
     * @deprecated androidx.media is deprecated. Please migrate to <a
     *     href="https://developer.android.com/media/media3">androidx.media3</a>.
     */
    @Deprecated
    @SuppressLint("BanParcelableUsage")
    public static final class QueueItem implements Parcelable {
        /**
         * This id is reserved. No items can be explicitly assigned this id.
         */
        public static final int UNKNOWN_ID = -1;

        private final MediaDescriptionCompat mDescription;
        private final long mId;

        private MediaSession.QueueItem mItemFwk;

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

        private QueueItem(
                MediaSession.QueueItem queueItem,
                MediaDescriptionCompat description,
                long id) {
            if (description == null) {
                throw new IllegalArgumentException("Description cannot be null");
            }
            if (id == UNKNOWN_ID) {
                throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");
            }
            mDescription = description;
            mId = id;
            mItemFwk = queueItem;
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
            if (mItemFwk != null) {
                return mItemFwk;
            }
            mItemFwk = Api21Impl.createQueueItem(
                    (MediaDescription) mDescription.getMediaDescription(),
                    mId);
            return mItemFwk;
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
            if (queueItem == null) {
                return null;
            }
            MediaSession.QueueItem queueItemObj = (MediaSession.QueueItem) queueItem;
            Object descriptionObj = Api21Impl.getDescription(queueItemObj);
            MediaDescriptionCompat description = MediaDescriptionCompat.fromMediaDescription(
                    descriptionObj);
            long id = Api21Impl.getQueueId(queueItemObj);
            return new QueueItem(queueItemObj, description, id);
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
            if (itemList == null) {
                return null;
            }
            List<QueueItem> items = new ArrayList<>(itemList.size());
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

        private static class Api21Impl {
            private Api21Impl() {}

            static MediaSession.QueueItem createQueueItem(MediaDescription description, long id) {
                return new MediaSession.QueueItem(description, id);
            }

            static MediaDescription getDescription(MediaSession.QueueItem queueItem) {
                return queueItem.getDescription();
            }

            static long getQueueId(MediaSession.QueueItem queueItem) {
                return queueItem.getQueueId();
            }
        }
    }

    /**
     * This is a wrapper for {@link ResultReceiver} for sending over aidl
     * interfaces. The framework version was not exposed to aidls until
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP}.
     */
    @SuppressLint("BanParcelableUsage")
    /* package */ static final class ResultReceiverWrapper implements Parcelable {
        ResultReceiver mResultReceiver;

        public ResultReceiverWrapper(@NonNull ResultReceiver resultReceiver) {
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

    /**
     * @deprecated androidx.media is deprecated. Please migrate to <a
     *     href="https://developer.android.com/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public interface OnActiveChangeListener {
        void onActiveChanged();
    }

    interface MediaSessionImpl {
        void setCallback(Callback callback, Handler handler);
        void setRegistrationCallback(
                @Nullable RegistrationCallback callback, @NonNull Handler handler);
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
        void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode);
        void setExtras(Bundle extras);

        Object getMediaSession();

        Object getRemoteControlClient();

        String getCallingPackage();
        RemoteUserInfo getCurrentControllerInfo();

        // Internal only method
        void setCurrentControllerInfo(RemoteUserInfo remoteUserInfo);
        Callback getCallback();
    }

    static class MediaSessionImplApi21 implements MediaSessionImpl {
        final MediaSession mSessionFwk;
        final ExtraSession mExtraSession;
        final Token mToken;
        final Object mLock = new Object();
        Bundle mSessionInfo;

        boolean mDestroyed = false;
        final RemoteCallbackList<IMediaControllerCallback> mExtraControllerCallbacks =
                new RemoteCallbackList<>();

        PlaybackStateCompat mPlaybackState;
        List<QueueItem> mQueue;
        MediaMetadataCompat mMetadata;
        @RatingCompat.Style int mRatingType;
        boolean mCaptioningEnabled;
        @PlaybackStateCompat.RepeatMode int mRepeatMode;
        @PlaybackStateCompat.ShuffleMode int mShuffleMode;

        @GuardedBy("mLock")
        Callback mCallback;
        @GuardedBy("mLock")
        RegistrationCallbackHandler mRegistrationCallbackHandler;
        @GuardedBy("mLock")
        RemoteUserInfo mRemoteUserInfo;

        MediaSessionImplApi21(Context context, String tag, VersionedParcelable session2Token,
                Bundle sessionInfo) {
            mSessionFwk = createFwkMediaSession(context, tag, sessionInfo);
            mExtraSession = new ExtraSession(/* mediaSessionImpl= */ this);
            mToken = new Token(mSessionFwk.getSessionToken(), mExtraSession, session2Token);
            mSessionInfo = sessionInfo;
            // For backward compatibility, these flags are always set.
            setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        MediaSessionImplApi21(Object mediaSession) {
            if (!(mediaSession instanceof MediaSession)) {
                throw new IllegalArgumentException(
                        "mediaSession is not a valid MediaSession object");
            }
            mSessionFwk = (MediaSession) mediaSession;
            mExtraSession = new ExtraSession(/* mediaSessionImpl= */ this);
            mToken = new Token(mSessionFwk.getSessionToken(), mExtraSession);
            mSessionInfo = null;
            // For backward compatibility, these flags are always set.
            setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        public MediaSession createFwkMediaSession(Context context, String tag, Bundle sessionInfo) {
            return new MediaSession(context, tag);
        }

        @Override
        public void setCallback(Callback callback, Handler handler) {
            synchronized (mLock) {
                mCallback = callback;
                mSessionFwk.setCallback(callback == null ? null : callback.mCallbackFwk, handler);
                if (callback != null) {
                    callback.setSessionImpl(this, handler);
                }
            }
        }

        @Override
        public void setRegistrationCallback(
                @Nullable RegistrationCallback callback, @NonNull Handler handler) {
            synchronized (mLock) {
                if (mRegistrationCallbackHandler != null) {
                    mRegistrationCallbackHandler.removeCallbacksAndMessages(null);
                }
                if (callback != null) {
                    mRegistrationCallbackHandler = new RegistrationCallbackHandler(
                            handler.getLooper(), callback);
                } else {
                    mRegistrationCallbackHandler = null;
                }
            }
        }

        @SuppressLint("WrongConstant")
        @Override
        public void setFlags(@SessionFlags int flags) {
            // For backward compatibility, always set these deprecated flags.
            mSessionFwk.setFlags(
                    flags | FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
        }

        @Override
        public void setPlaybackToLocal(int stream) {
            // TODO update APIs to use support version of AudioAttributes
            AudioAttributes.Builder bob = new AudioAttributes.Builder();
            bob.setLegacyStreamType(stream);
            mSessionFwk.setPlaybackToLocal(bob.build());
        }

        @Override
        public void setPlaybackToRemote(VolumeProviderCompat volumeProvider) {
            mSessionFwk.setPlaybackToRemote((VolumeProvider) volumeProvider.getVolumeProvider());
        }

        @Override
        public void setActive(boolean active) {
            mSessionFwk.setActive(active);
        }

        @Override
        public boolean isActive() {
            return mSessionFwk.isActive();
        }

        @Override
        public void sendSessionEvent(String event, Bundle extras) {
            if (android.os.Build.VERSION.SDK_INT < 23) {
                synchronized (mLock) {
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
            }
            mSessionFwk.sendSessionEvent(event, extras);
        }

        @Override
        public void release() {
            mDestroyed = true;
            mExtraControllerCallbacks.kill();
            if (Build.VERSION.SDK_INT == 27) {
                // This is a workaround for framework MediaSession's bug in API 27.
                try {
                    Field callback = mSessionFwk.getClass().getDeclaredField("mCallback");
                    callback.setAccessible(true);
                    Handler handler = (Handler) callback.get(mSessionFwk);
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Exception happened while accessing MediaSession.mCallback.",
                            e);
                }
            }
            // Prevent from receiving callbacks from released session.
            mSessionFwk.setCallback(null);
            mExtraSession.release();
            mSessionFwk.release();
        }

        @Override
        public Token getSessionToken() {
            return mToken;
        }

        @Override
        public void setPlaybackState(PlaybackStateCompat state) {
            mPlaybackState = state;
            synchronized (mLock) {
                int size = mExtraControllerCallbacks.beginBroadcast();
                for (int i = size - 1; i >= 0; i--) {
                    IMediaControllerCallback cb = mExtraControllerCallbacks.getBroadcastItem(i);
                    try {
                        cb.onPlaybackStateChanged(state);
                    } catch (RemoteException e) {
                    }
                }
                mExtraControllerCallbacks.finishBroadcast();
            }
            mSessionFwk.setPlaybackState(
                    state == null ? null : (PlaybackState) state.getPlaybackState());
        }

        @Override
        public PlaybackStateCompat getPlaybackState() {
            return mPlaybackState;
        }

        @Override
        public void setMetadata(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            mSessionFwk.setMetadata(
                    metadata == null ? null : (MediaMetadata) metadata.getMediaMetadata());
        }

        @Override
        public void setSessionActivity(PendingIntent pi) {
            mSessionFwk.setSessionActivity(pi);
        }

        @Override
        public void setMediaButtonReceiver(PendingIntent mbr) {
            mSessionFwk.setMediaButtonReceiver(mbr);
        }

        @Override
        public void setQueue(List<QueueItem> queue) {
            mQueue = queue;
            if (queue == null) {
                mSessionFwk.setQueue(null);
                return;
            }
            ArrayList<MediaSession.QueueItem> queueItemFwks = new ArrayList<>(queue.size());
            for (QueueItem item : queue) {
                queueItemFwks.add((MediaSession.QueueItem) item.getQueueItem());
            }
            mSessionFwk.setQueue(queueItemFwks);
        }

        @Override
        public void setQueueTitle(CharSequence title) {
            mSessionFwk.setQueueTitle(title);
        }

        @Override
        public void setRatingType(@RatingCompat.Style int type) {
            mRatingType = type;
        }

        @Override
        public void setCaptioningEnabled(boolean enabled) {
            if (mCaptioningEnabled != enabled) {
                mCaptioningEnabled = enabled;
                synchronized (mLock) {
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
        }

        @Override
        public void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
            if (mRepeatMode != repeatMode) {
                mRepeatMode = repeatMode;
                synchronized (mLock) {
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
        }

        @Override
        public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
            if (mShuffleMode != shuffleMode) {
                mShuffleMode = shuffleMode;
                synchronized (mLock) {
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
        }

        @Override
        public void setExtras(Bundle extras) {
            mSessionFwk.setExtras(extras);
        }

        @Override
        public Object getMediaSession() {
            return mSessionFwk;
        }

        @Override
        public Object getRemoteControlClient() {
            // Note: When this returns somthing, {@link MediaSessionCompatCallbackTest} and
            //       {@link #setCurrentUserInfoOverride} should be also updated.
            return null;
        }

        @Override
        public void setCurrentControllerInfo(RemoteUserInfo remoteUserInfo) {
            synchronized (mLock) {
                mRemoteUserInfo = remoteUserInfo;
            }
        }

        @Override
        public String getCallingPackage() {
            if (android.os.Build.VERSION.SDK_INT < 24) {
                return null;
            } else {
                try {
                    Method getCallingPackageMethod = mSessionFwk.getClass().getMethod(
                            "getCallingPackage");
                    return (String) getCallingPackageMethod.invoke(mSessionFwk);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot execute MediaSession.getCallingPackage()", e);
                }
                return null;
            }
        }

        @Override
        public RemoteUserInfo getCurrentControllerInfo() {
            synchronized (mLock) {
                return mRemoteUserInfo;
            }
        }

        @Override
        public Callback getCallback() {
            synchronized (mLock) {
                return mCallback;
            }
        }

        private static class ExtraSession extends IMediaSession.Stub {

            private final AtomicReference<MediaSessionImplApi21> mMediaSessionImplRef;

            ExtraSession(@NonNull MediaSessionImplApi21 mediaSessionImpl) {
                mMediaSessionImplRef = new AtomicReference<>(mediaSessionImpl);
            }

            /**
             * Clears the reference to the containing component in order to enable garbage
             * collection.
             */
            public void release() {
                mMediaSessionImplRef.set(null);
            }

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
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                if (mediaSessionImpl == null) {
                    return;
                }
                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                RemoteUserInfo info = new RemoteUserInfo(
                        RemoteUserInfo.LEGACY_CONTROLLER, callingPid, callingUid);
                mediaSessionImpl.mExtraControllerCallbacks.register(cb, info);
                synchronized (mediaSessionImpl.mLock) {
                    if (mediaSessionImpl.mRegistrationCallbackHandler != null) {
                        mediaSessionImpl.mRegistrationCallbackHandler.postCallbackRegistered(
                                callingPid, callingUid);
                    }
                }
            }

            @Override
            public void unregisterCallbackListener(IMediaControllerCallback cb) {
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                if (mediaSessionImpl == null) {
                    return;
                }
                mediaSessionImpl.mExtraControllerCallbacks.unregister(cb);

                int callingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                synchronized (mediaSessionImpl.mLock) {
                    if (mediaSessionImpl.mRegistrationCallbackHandler != null) {
                        mediaSessionImpl.mRegistrationCallbackHandler.postCallbackUnregistered(
                                callingPid, callingUid);
                    }
                }
            }

            @Override
            public String getPackageName() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public Bundle getSessionInfo() {
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                return mediaSessionImpl != null && mediaSessionImpl.mSessionInfo != null
                        ? new Bundle(mediaSessionImpl.mSessionInfo)
                        : null;
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
            public void prepareFromMediaId(String mediaId, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepareFromSearch(String query, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void prepareFromUri(Uri uri, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void play() throws RemoteException {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromMediaId(String mediaId, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromSearch(String query, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void playFromUri(Uri uri, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void skipToQueueItem(long id) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void pause() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void stop() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void next() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void previous() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void fastForward() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rewind() {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void seekTo(long pos) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rate(RatingCompat rating) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void rateWithExtras(RatingCompat rating, Bundle extras) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setPlaybackSpeed(float speed) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setCaptioningEnabled(boolean enabled) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setRepeatMode(int repeatMode) {
                // Will not be called.
                throw new AssertionError();
            }

            @Override
            public void setShuffleModeEnabledRemoved(boolean enabled) {
                // Do nothing.
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
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                if (mediaSessionImpl != null) {
                    return getStateWithUpdatedPosition(
                            mediaSessionImpl.mPlaybackState, mediaSessionImpl.mMetadata);
                } else {
                    return null;
                }
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
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                return mediaSessionImpl != null
                        ? mediaSessionImpl.mRatingType
                        : RatingCompat.RATING_NONE;
            }

            @Override
            public boolean isCaptioningEnabled() {
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                return mediaSessionImpl != null && mediaSessionImpl.mCaptioningEnabled;
            }

            @Override
            @PlaybackStateCompat.RepeatMode
            public int getRepeatMode() {
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                return mediaSessionImpl != null
                        ? mediaSessionImpl.mRepeatMode
                        : PlaybackStateCompat.REPEAT_MODE_INVALID;
            }

            @Override
            public boolean isShuffleModeEnabledRemoved() {
                return false;
            }

            @Override
            @PlaybackStateCompat.ShuffleMode
            public int getShuffleMode() {
                MediaSessionImplApi21 mediaSessionImpl = mMediaSessionImplRef.get();
                return mediaSessionImpl != null
                        ? mediaSessionImpl.mShuffleMode
                        : PlaybackStateCompat.SHUFFLE_MODE_INVALID;
            }

            @Override
            public boolean isTransportControlEnabled() {
                // Will not be called.
                throw new AssertionError();
            }
        }
    }

    @RequiresApi(22)
    static class MediaSessionImplApi22 extends MediaSessionImplApi21 {
        MediaSessionImplApi22(Context context, String tag, VersionedParcelable session2Token,
                Bundle sessionInfo) {
            super(context, tag, session2Token, sessionInfo);
        }

        MediaSessionImplApi22(Object mediaSession) {
            super(mediaSession);
        }

        @Override
        public void setRatingType(@RatingCompat.Style int type) {
            mSessionFwk.setRatingType(type);
        }
    }

    @RequiresApi(28)
    static class MediaSessionImplApi28 extends MediaSessionImplApi22 {
        MediaSessionImplApi28(Context context, String tag, VersionedParcelable session2Token,
                Bundle sessionInfo) {
            super(context, tag, session2Token, sessionInfo);
        }

        MediaSessionImplApi28(Object mediaSession) {
            super(mediaSession);
        }

        @Override
        public void setCurrentControllerInfo(RemoteUserInfo remoteUserInfo) {
            // No-op. {@link MediaSession#getCurrentControllerInfo} would work.
        }

        @Override
        public final @NonNull RemoteUserInfo getCurrentControllerInfo() {
            android.media.session.MediaSessionManager.RemoteUserInfo info =
                    ((MediaSession) mSessionFwk).getCurrentControllerInfo();
            return new RemoteUserInfo(info);
        }
    }

    @RequiresApi(29)
    static class MediaSessionImplApi29 extends MediaSessionImplApi28 {
        MediaSessionImplApi29(Context context, String tag, VersionedParcelable session2Token,
                Bundle sessionInfo) {
            super(context, tag, session2Token, sessionInfo);
        }

        MediaSessionImplApi29(Object mediaSession) {
            super(mediaSession);
            mSessionInfo = ((MediaSession) mediaSession).getController().getSessionInfo();
        }

        @Override
        public MediaSession createFwkMediaSession(Context context, String tag, Bundle sessionInfo) {
            return new MediaSession(context, tag, sessionInfo);
        }
    }

    static final class RegistrationCallbackHandler extends Handler {
        private static final int MSG_CALLBACK_REGISTERED = 1001;
        private static final int MSG_CALLBACK_UNREGISTERED = 1002;

        private final RegistrationCallback mCallback;

        RegistrationCallbackHandler(
                @NonNull Looper looper,
                @NonNull RegistrationCallback callback) {
            super(looper);
            mCallback = callback;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_CALLBACK_REGISTERED:
                    mCallback.onCallbackRegistered(msg.arg1, msg.arg2);
                    break;
                case MSG_CALLBACK_UNREGISTERED:
                    mCallback.onCallbackUnregistered(msg.arg1, msg.arg2);
                    break;
            }
        }

        public void postCallbackRegistered(int callingPid, int callingUid) {
            obtainMessage(MSG_CALLBACK_REGISTERED, callingPid, callingUid).sendToTarget();
        }

        public void postCallbackUnregistered(int callingPid, int callingUid) {
            obtainMessage(MSG_CALLBACK_UNREGISTERED, callingPid, callingUid).sendToTarget();
        }
    }
}
