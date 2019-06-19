/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.media2.common.SessionPlayer.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE;
import static androidx.media2.common.SessionPlayer.REPEAT_MODE_NONE;
import static androidx.media2.common.SessionPlayer.SHUFFLE_MODE_NONE;
import static androidx.media2.common.SessionPlayer.UNKNOWN_TIME;

import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.RepeatMode;
import androidx.media2.common.SessionPlayer.ShuffleMode;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Allows an app to interact with an active {@link MediaSession} or a
 * {@link MediaSessionService} which would provide {@link MediaSession}. Media buttons and other
 * commands can be sent to the session.
 * <p>
 * MediaController objects are thread-safe.
 * <p>
 * Topic covered here:
 * <ol>
 * <li><a href="#ControllerLifeCycle">Controller Lifecycle</a>
 * </ol>
 * <a name="ControllerLifeCycle"></a>
 * <h3>Controller Lifecycle</h3>
 * <p>
 * When a controller is created with the {@link SessionToken} for a {@link MediaSession} (i.e.
 * session token type is {@link SessionToken#TYPE_SESSION}), the controller will connect to the
 * specific session.
 * <p>
 * When a controller is created with the {@link SessionToken} for a {@link MediaSessionService}
 * (i.e. session token type is {@link SessionToken#TYPE_SESSION_SERVICE} or
 * {@link SessionToken#TYPE_LIBRARY_SERVICE}), the controller binds to the service for connecting
 * to a {@link MediaSession} in it. {@link MediaSessionService} will provide a session to connect.
 * <p>
 * When a controller connects to a session,
 * {@link MediaSession.SessionCallback#onConnect(MediaSession, MediaSession.ControllerInfo)}
 * will be called to either accept or reject the connection. Wait
 * {@link ControllerCallback#onConnected(MediaController, SessionCommandGroup)} or
 * {@link ControllerCallback#onDisconnected(MediaController)} for the result.
 * <p>
 * When the connected session is closed, the controller will receive
 * {@link ControllerCallback#onDisconnected(MediaController)}.
 * <p>
 * When you're done, use {@link #close()} to clean up resources. This also helps session service
 * to be destroyed when there's no controller associated with it.
 *
 * @see MediaSession
 * @see MediaSessionService
 */
public class MediaController implements AutoCloseable {
    private static final String TAG = "MediaController";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({AudioManager.ADJUST_LOWER, AudioManager.ADJUST_RAISE, AudioManager.ADJUST_SAME,
            AudioManager.ADJUST_MUTE, AudioManager.ADJUST_UNMUTE, AudioManager.ADJUST_TOGGLE_MUTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeDirection {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(value = {AudioManager.FLAG_SHOW_UI, AudioManager.FLAG_ALLOW_RINGER_MODES,
            AudioManager.FLAG_PLAY_SOUND, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
            AudioManager.FLAG_VIBRATE}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeFlags {}

    final Object mLock = new Object();
    @GuardedBy("mLock")
    MediaControllerImpl mImpl;
    @GuardedBy("mLock")
    boolean mClosed;

    final ControllerCallback mCallback;
    final Executor mCallbackExecutor;

    @GuardedBy("mLock")
    private final List<Pair<ControllerCallback, Executor>> mExtraCallbacks = new ArrayList<>();

    // For testing.
    Long mTimeDiff;

    /**
     * Create a {@link MediaController} from the {@link SessionToken}.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    MediaController(@NonNull final Context context, @NonNull final SessionToken token,
            @Nullable Bundle connectionHints, @Nullable Executor executor,
            @Nullable ControllerCallback callback) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (token == null) {
            throw new NullPointerException("token shouldn't be null");
        }
        mCallback = callback;
        mCallbackExecutor = executor;
        synchronized (mLock) {
            mImpl = createImpl(context, token, connectionHints);
        }
    }

    /**
     * Create a {@link MediaController} from the {@link MediaSessionCompat.Token}.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    MediaController(@NonNull final Context context, @NonNull final MediaSessionCompat.Token token,
            @Nullable final Bundle connectionHints, @Nullable final Executor executor,
            @Nullable final ControllerCallback callback) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (token == null) {
            throw new NullPointerException("token shouldn't be null");
        }
        mCallback = callback;
        mCallbackExecutor = executor;
        SessionToken.createSessionToken(context, token, executor,
                new SessionToken.OnSessionTokenCreatedListener() {
                    @Override
                    public void onSessionTokenCreated(MediaSessionCompat.Token token,
                            SessionToken token2) {
                        synchronized (mLock) {
                            if (!mClosed) {
                                mImpl = createImpl(context, token2, connectionHints);
                            } else {
                                notifyControllerCallback(new ControllerCallbackRunnable() {
                                    @Override
                                    public void run(@NonNull ControllerCallback callback) {
                                        callback.onDisconnected(MediaController.this);
                                    }
                                });
                            }
                        }
                    }
                });
    }

    MediaControllerImpl createImpl(@NonNull Context context, @NonNull SessionToken token,
            @Nullable Bundle connectionHints) {
        if (token.isLegacySession()) {
            return new MediaControllerImplLegacy(context, this, token);
        } else {
            return new MediaControllerImplBase(context, this, token, connectionHints);
        }
    }

    MediaControllerImpl getImpl() {
        synchronized (mLock) {
            return mImpl;
        }
    }

    /**
     * Release this object, and disconnect from the session. After this, callbacks wouldn't be
     * received.
     */
    @Override
    public void close() {
        try {
            MediaControllerImpl impl;
            synchronized (mLock) {
                if (mClosed) {
                    return;
                }
                mClosed = true;
                impl = mImpl;
            }
            if (impl != null) {
                impl.close();
            }
        } catch (Exception e) {
            // Should not be here.
        }
    }

    /**
     * Returns {@link SessionToken} of the connected session.
     * If it is not connected yet, it returns {@code null}.
     * <p>
     * This may differ with the {@link SessionToken} from the constructor. For example, if the
     * controller is created with the token for {@link MediaSessionService}, this would return
     * token for the {@link MediaSession} in the service.
     *
     * @return SessionToken of the connected session, or {@code null} if not connected
     */
    @Nullable
    public SessionToken getConnectedToken() {
        return isConnected() ? getImpl().getConnectedToken() : null;
    }

    /**
     * Returns whether this class is connected to active {@link MediaSession} or not.
     */
    public boolean isConnected() {
        MediaControllerImpl impl = getImpl();
        return impl != null && impl.isConnected();
    }

    /**
     * Requests that the player starts or resumes playback.
     * <p>
     * If the player state is {@link SessionPlayer#PLAYER_STATE_IDLE}, the session would also call
     * {@link SessionPlayer#prepare} and then {@link SessionPlayer#play} to start playback. If you
     * want to have finer grained control of the playback start call {@link #prepare} manually
     * before this. Calling {@link #prepare} in advance would help this method to start playback
     * faster and also help to take audio focus at the last moment.
     *
     * @see #prepare
     */
    @NonNull
    public ListenableFuture<SessionResult> play() {
        if (isConnected()) {
            return getImpl().play();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player pauses playback.
     * <p>
     * This would transfer the player state from {@link SessionPlayer#PLAYER_STATE_PLAYING} to
     * {@link SessionPlayer#PLAYER_STATE_PAUSED}.
     */
    @NonNull
    public ListenableFuture<SessionResult> pause() {
        if (isConnected()) {
            return getImpl().pause();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player prepares the media items for playback.
     * <p>
     * This would transfer the player state from {@link SessionPlayer#PLAYER_STATE_IDLE} to
     * {@link SessionPlayer#PLAYER_STATE_PAUSED}.
     * <p>
     * Playback can be started without this. But this provides finer grained control of playback
     * start. See {@link #play} for details.
     *
     * @see #play
     */
    @NonNull
    public ListenableFuture<SessionResult> prepare() {
        if (isConnected()) {
            return getImpl().prepare();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to increase the playback speed.
     *
     * @see MediaSession.SessionCallback#onFastForward(MediaSession, MediaSession.ControllerInfo)
     */
    @NonNull
    public ListenableFuture<SessionResult> fastForward() {
        if (isConnected()) {
            return getImpl().fastForward();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to decrease the playback speed.
     *
     * @see MediaSession.SessionCallback#onRewind(MediaSession, MediaSession.ControllerInfo)
     */
    @NonNull
    public ListenableFuture<SessionResult> rewind() {
        if (isConnected()) {
            return getImpl().rewind();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to skip backward within the current media item.
     *
     * @see MediaSession.SessionCallback#onSkipForward(MediaSession, MediaSession.ControllerInfo)
     */
    @NonNull
    public ListenableFuture<SessionResult> skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
        if (isConnected()) {
            return getImpl().skipForward();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to skip forward within the current media item.
     *
     * @see MediaSession.SessionCallback#onSkipBackward(MediaSession, MediaSession.ControllerInfo)
     */
    @NonNull
    public ListenableFuture<SessionResult> skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
        if (isConnected()) {
            return getImpl().skipBackward();
        }
        return createDisconnectedFuture();
    }

    /**
     * Move to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    @NonNull
    public ListenableFuture<SessionResult> seekTo(long pos) {
        if (isConnected()) {
            return getImpl().seekTo(pos);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player start playback for a specific media id.
     *
     * @param mediaId The non-empty media id
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> playFromMediaId(@NonNull String mediaId,
            @Nullable Bundle extras) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().playFromMediaId(mediaId, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player start playback for a specific search query.
     *
     * @param query The non-empty search query
     * @param extras Optional extras that can include extra information about the query.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> playFromSearch(@NonNull String query,
            @Nullable Bundle extras) {
        if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().playFromSearch(query, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player start playback for a specific {@link Uri}.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> playFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        if (uri == null) {
            throw new NullPointerException("uri shouldn't be null");
        }
        if (isConnected()) {
            return getImpl().playFromUri(uri, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player prepare a media item with the media id for playback.
     * In other words, other sessions can continue to play during the preparation of this session.
     * This method can be used to speed up the start of the playback.
     * Once the prepare is done, the session will change its playback state to
     * {@link SessionPlayer#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromMediaId} can be directly called
     * without this method.
     *
     * @param mediaId The non-empty media id
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> prepareFromMediaId(@NonNull String mediaId,
            @Nullable Bundle extras) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().prepareFromMediaId(mediaId, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player prepare a media item with the specific search query for playback.
     * In other words, other sessions can continue to play during the preparation of this session.
     * This method can be used to speed up the start of the playback.
     * Once the prepare is done, the session will change its playback state to
     * {@link SessionPlayer#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromSearch} can be directly called
     * without this method.
     *
     * @param query The non-empty search query
     * @param extras Optional extras that can include extra information about the query.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> prepareFromSearch(@NonNull String query,
            @Nullable Bundle extras) {
        if (TextUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().prepareFromSearch(query, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player prepare a media item with the specific {@link Uri} for playback.
     * In other words, other sessions can continue to play during the preparation of this session.
     * This method can be used to speed up the start of the playback.
     * Once the prepare is done, the session will change its playback state to
     * {@link SessionPlayer#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromUri} can be directly called
     * without this method.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @NonNull
    public ListenableFuture<SessionResult> prepareFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        if (uri == null) {
            throw new NullPointerException("uri shouldn't be null");
        }
        if (isConnected()) {
            return getImpl().prepareFromUri(uri, extras);
        }
        return createDisconnectedFuture();
    }

    /**
     * Set the volume of the output this session is playing on. The command will be ignored if it
     * does not support {@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}.
     * <p>
     * If the session is local playback, this changes the device's volume with the stream that
     * session's player is using. Flags will be specified for the {@link AudioManager}.
     * <p>
     * If the session is remote player (i.e. session has set volume provider), its volume provider
     * will receive this request instead.
     *
     * @see #getPlaybackInfo()
     * @param value The value to set it to, between 0 and the reported max.
     * @param flags flags from {@link AudioManager} to include with the volume request for local
     *              playback
     */
    @NonNull
    public ListenableFuture<SessionResult> setVolumeTo(int value, @VolumeFlags int flags) {
        if (isConnected()) {
            return getImpl().setVolumeTo(value, flags);
        }
        return createDisconnectedFuture();
    }

    /**
     * Adjust the volume of the output this session is playing on. The direction
     * must be one of {@link AudioManager#ADJUST_LOWER},
     * {@link AudioManager#ADJUST_RAISE}, or {@link AudioManager#ADJUST_SAME}.
     * <p>
     * The command will be ignored if the session does not support
     * {@link VolumeProviderCompat#VOLUME_CONTROL_RELATIVE} or
     * {@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}.
     * <p>
     * If the session is local playback, this changes the device's volume with the stream that
     * session's player is using. Flags will be specified for the {@link AudioManager}.
     * <p>
     * If the session is remote player (i.e. session has set volume provider), its volume provider
     * will receive this request instead.
     *
     * @see #getPlaybackInfo()
     * @param direction The direction to adjust the volume in.
     * @param flags flags from {@link AudioManager} to include with the volume request for local
     *              playback
     */
    @NonNull
    public ListenableFuture<SessionResult> adjustVolume(@VolumeDirection int direction,
            @VolumeFlags int flags) {
        if (isConnected()) {
            return getImpl().adjustVolume(direction, flags);
        }
        return createDisconnectedFuture();
    }

    /**
     * Get an intent for launching UI associated with this session if one exists.
     * If it is not connected yet, it returns {@code null}.
     *
     * @return A {@link PendingIntent} to launch UI or null
     */
    @Nullable
    public PendingIntent getSessionActivity() {
        return isConnected() ? getImpl().getSessionActivity() : null;
    }

    /**
     * Get the lastly cached player state from
     * {@link ControllerCallback#onPlayerStateChanged(MediaController, int)}.
     * If it is not connected yet, it returns {@link SessionPlayer#PLAYER_STATE_IDLE}.
     *
     * @return player state
     */
    public int getPlayerState() {
        return isConnected() ? getImpl().getPlayerState() : PLAYER_STATE_IDLE;
    }

    /**
     * Gets the duration of the current media item, or {@link SessionPlayer#UNKNOWN_TIME} if
     * unknown or not connected.
     *
     * @return the duration in ms, or {@link SessionPlayer#UNKNOWN_TIME}
     */
    public long getDuration() {
        return isConnected() ? getImpl().getDuration() : UNKNOWN_TIME;
    }

    /**
     * Gets the current playback position.
     * <p>
     * This returns the calculated value of the position, based on the difference between the
     * update time and current time.
     *
     * @return the current playback position in ms, or {@link SessionPlayer#UNKNOWN_TIME}
     *         if unknown or not connected
     */
    public long getCurrentPosition() {
        return isConnected() ? getImpl().getCurrentPosition() : UNKNOWN_TIME;
    }

    /**
     * Get the lastly cached playback speed from
     * {@link ControllerCallback#onPlaybackSpeedChanged(MediaController, float)}.
     *
     * @return speed the lastly cached playback speed, or 0f if unknown or not connected
     */
    public float getPlaybackSpeed() {
        return isConnected() ? getImpl().getPlaybackSpeed() : 0f;
    }

    /**
     * Sets the playback speed. A value of {@code 1.0f} is the default playback value,
     * and a negative value indicates reverse playback. {@code 0.0f} is not allowed.
     *
     * @throws IllegalArgumentException if the {@code speed} is equal to zero.
     */
    @NonNull
    public ListenableFuture<SessionResult> setPlaybackSpeed(float speed) {
        if (speed == 0.0f) {
            throw new IllegalArgumentException("speed must not be zero");
        }
        if (isConnected()) {
            return getImpl().setPlaybackSpeed(speed);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     *
     * @return the buffering state, or {@link SessionPlayer#BUFFERING_STATE_UNKNOWN}
     *         if unknown or not connected
     */
    @SessionPlayer.BuffState
    public int getBufferingState() {
        return isConnected() ? getImpl().getBufferingState() : BUFFERING_STATE_UNKNOWN;
    }

    /**
     * Gets the lastly cached buffered position from the session when
     * {@link ControllerCallback#onBufferingStateChanged(MediaController, MediaItem, int)} is
     * called.
     *
     * @return buffering position in millis, or {@link SessionPlayer#UNKNOWN_TIME} if
     *         unknown or not connected
     */
    public long getBufferedPosition() {
        return isConnected() ? getImpl().getBufferedPosition() : UNKNOWN_TIME;
    }

    /**
     * Get the current playback info for this session.
     * If it is not connected yet, it returns {@code null}.
     *
     * @return The current playback info or null
     */
    @Nullable
    public PlaybackInfo getPlaybackInfo() {
        return isConnected() ? getImpl().getPlaybackInfo() : null;
    }

    /**
     * Rate the media. This will cause the rating to be set for the current user.
     * The rating style must follow the user rating style from the session.
     * You can get the rating style from the session through the
     * {@link MediaMetadata#getRating(String)} with the key
     * {@link MediaMetadata#METADATA_KEY_USER_RATING}.
     * <p>
     * If the user rating was {@code null}, the media item does not accept setting user rating.
     *
     * @param mediaId The non-empty media id
     * @param rating The rating to set
     */
    @NonNull
    public ListenableFuture<SessionResult> setRating(@NonNull String mediaId,
            @NonNull Rating rating) {
        if (mediaId == null) {
            throw new NullPointerException("mediaId shouldn't be null");
        } else if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (rating == null) {
            throw new NullPointerException("rating shouldn't be null");
        }
        if (isConnected()) {
            return getImpl().setRating(mediaId, rating);
        }
        return createDisconnectedFuture();
    }

    /**
     * Send custom command to the session
     * <p>
     * Interoperability: When connected to
     * {@link android.support.v4.media.session.MediaSessionCompat},
     * {@link SessionResult#getResultCode()} will return the custom result code from the
     * {@link ResultReceiver#onReceiveResult(int, Bundle)} instead of the standard result codes
     * defined in the {@link SessionResult}.
     * <p>
     * A command is not accepted if it is not a custom command.
     *
     * @param command custom command
     * @param args optional argument
     */
    @NonNull
    public ListenableFuture<SessionResult> sendCustomCommand(@NonNull SessionCommand command,
            @Nullable Bundle args) {
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
        }
        if (command.getCommandCode() != SessionCommand.COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command should be a custom command");
        }
        if (isConnected()) {
            return getImpl().sendCustomCommand(command, args);
        }
        return createDisconnectedFuture();
    }

    /**
     * Returns the cached playlist from {@link ControllerCallback#onPlaylistChanged}. Can be
     * {@code null} if the playlist hasn't been set or it's reset by {@link #setMediaItem}.
     * <p>
     * This list may differ with the list that was specified with
     * {@link #setPlaylist(List, MediaMetadata)} depending on the {@link SessionPlayer}
     * implementation. Use media items returned here for other playlist agent APIs such as
     * {@link SessionPlayer#skipToPlaylistItem}.
     *
     * @return playlist, or {@code null} if the playlist hasn't been set, controller isn't
     *         connected, or it doesn't have enough permission.
     * @see SessionCommand#COMMAND_CODE_PLAYER_GET_PLAYLIST
     */
    @Nullable
    public List<MediaItem> getPlaylist() {
        return isConnected() ? getImpl().getPlaylist() : null;
    }

    /**
     * Sets the playlist with the list of media IDs. Use this or {@link #setMediaItem} to specify
     * which items to play.
     * <p>
     * All media IDs in the list shouldn't be an empty string.
     * <p>
     * The {@link ControllerCallback#onPlaylistChanged} and
     * {@link ControllerCallback#onCurrentMediaItemChanged} would be called when it's completed.
     * The current item would be the first item in the playlist.
     *
     * @param list list of media id. Shouldn't contain an empty id.
     * @param metadata metadata of the playlist
     * @see #setMediaItem
     * @see ControllerCallback#onCurrentMediaItemChanged
     * @see ControllerCallback#onPlaylistChanged
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     * @throws IllegalArgumentException if the list is {@code null} or contains any empty string.
     */
    @NonNull
    public ListenableFuture<SessionResult> setPlaylist(@NonNull List<String> list,
            @Nullable MediaMetadata metadata) {
        if (list == null) {
            throw new NullPointerException("list shouldn't be null");
        }
        for (int i = 0; i < list.size(); i++) {
            if (TextUtils.isEmpty(list.get(i))) {
                throw new IllegalArgumentException("list shouldn't contain empty id, index=" + i);
            }
        }
        if (isConnected()) {
            return getImpl().setPlaylist(list, metadata);
        }
        return createDisconnectedFuture();
    }

    /**
     * Sets a {@link MediaItem} for playback with the media ID. Use this or {@link #setPlaylist}
     * to specify which items to play. If you want to change current item in the playlist, use one
     * of {@link #skipToPlaylistItem}, {@link #skipToNextPlaylistItem}, or
     * {@link #skipToPreviousPlaylistItem} instead of this method.
     * <p>
     * The {@link ControllerCallback#onPlaylistChanged} and
     * {@link ControllerCallback#onCurrentMediaItemChanged} would be called when it's completed.
     * The current item would be the item given here.
     *
     * @param mediaId The non-empty media id of the item to play
     * @see #setPlaylist
     * @see ControllerCallback#onCurrentMediaItemChanged
     * @see ControllerCallback#onPlaylistChanged
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<SessionResult> setMediaItem(@NonNull String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            getImpl().setMediaItem(mediaId);
        }
        return createDisconnectedFuture();
    }

    /**
     * Updates the playlist metadata
     *
     * @param metadata metadata of the playlist
     */
    @NonNull
    public ListenableFuture<SessionResult> updatePlaylistMetadata(
            @Nullable MediaMetadata metadata) {
        if (isConnected()) {
            return getImpl().updatePlaylistMetadata(metadata);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the lastly cached playlist metadata either from
     * {@link ControllerCallback#onPlaylistMetadataChanged} or
     * {@link ControllerCallback#onPlaylistChanged}.
     *
     * @return metadata metadata of the playlist, or null if none is set or the controller is not
     *         connected
     */
    @Nullable
    public MediaMetadata getPlaylistMetadata() {
        return isConnected() ? getImpl().getPlaylistMetadata() : null;
    }

    /**
     * Adds the media item to the playlist at the index with the media ID. Index equals or greater
     * than the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end
     * of the playlist.
     * <p>
     * This will not change the currently playing media item.
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist will be incremented correspondingly.
     *
     * @param index the index you want to add
     * @param mediaId The non-empty media id of the new item
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<SessionResult> addPlaylistItem(@IntRange(from = 0) int index,
            @NonNull String mediaId) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().addPlaylistItem(index, mediaId);
        }
        return createDisconnectedFuture();
    }

    /**
     * Removes the media item at index in the playlist.
     * <p>
     * If the item is the currently playing item of the playlist, current playback
     * will be stopped and playback moves to next source in the list.
     *
     * @param index the media item you want to add
     */
    @NonNull
    public ListenableFuture<SessionResult> removePlaylistItem(@IntRange(from = 0) int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (isConnected()) {
            return getImpl().removePlaylistItem(index);
        }
        return createDisconnectedFuture();
    }

    /**
     * Replaces the media item at index in the playlist with the media ID.
     *
     * @param index the index of the item to replace
     * @param mediaId The non-empty media id of the new item
     * @see MediaMetadata#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<SessionResult> replacePlaylistItem(@IntRange(from = 0) int index,
            @NonNull String mediaId) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (isConnected()) {
            return getImpl().replacePlaylistItem(index, mediaId);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the lastly cached current item from
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController, MediaItem)}.
     *
     * @return the currently playing item, or null if unknown or not connected
     * @see #setMediaItem
     * @see #setPlaylist
     */
    @Nullable
    public MediaItem getCurrentMediaItem() {
        return isConnected() ? getImpl().getCurrentMediaItem() : null;
    }

    /**
     * Gets the current item index in the playlist. The returned value can be outdated after
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController, MediaItem)} or
     * {@link ControllerCallback#onPlaylistChanged(MediaController, List, MediaMetadata)} is called.
     *
     * @return the index of current item in playlist, or -1 if current media item does not exist or
     * playlist hasn't been set.
     */
    public int getCurrentMediaItemIndex() {
        return isConnected() ? getImpl().getCurrentMediaItemIndex() : -1;
    }

    /**
     * Gets the previous item index in the playlist. The returned value can be outdated after
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController, MediaItem)} or
     * {@link ControllerCallback#onPlaylistChanged(MediaController, List, MediaMetadata)} is called.
     * <p>
     * Interoperability: When connected to
     * {@link android.support.v4.media.session.MediaSessionCompat}, this will always return
     * {@code -1}.
     *
     * @return the index of previous item in playlist, or -1 if previous media item does not exist
     * or playlist hasn't been set.
     */
    public int getPreviousMediaItemIndex() {
        return isConnected() ? getImpl().getPreviousMediaItemIndex() : -1;
    }

    /**
     * Gets the next item index in the playlist. The returned value can be outdated after
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController, MediaItem)} or
     * {@link ControllerCallback#onPlaylistChanged(MediaController, List, MediaMetadata)} is called.
     * <p>
     * Interoperability: When connected to
     * {@link android.support.v4.media.session.MediaSessionCompat}, this will always return
     * {@code -1}.
     *
     * @return the index of next item in playlist, or -1 if next media item does not exist or
     * playlist hasn't been set.
     */
    public int getNextMediaItemIndex() {
        return isConnected() ? getImpl().getNextMediaItemIndex() : -1;
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * This calls {@link SessionPlayer#skipToPreviousPlaylistItem()}.
     */
    @NonNull
    public ListenableFuture<SessionResult> skipToPreviousPlaylistItem() {
        if (isConnected()) {
            return getImpl().skipToPreviousItem();
        }
        return createDisconnectedFuture();
    }

    /**
     * Skips to the next item in the playlist.
     * <p>
     * This calls {@link SessionPlayer#skipToNextPlaylistItem()}.
     */
    @NonNull
    public ListenableFuture<SessionResult> skipToNextPlaylistItem() {
        if (isConnected()) {
            return getImpl().skipToNextItem();
        }
        return createDisconnectedFuture();
    }

    /**
     * Skips to the item in the playlist at the index.
     * <p>
     * This calls {@link SessionPlayer#skipToPlaylistItem(int)}.
     *
     * @param index The item in the playlist you want to play
     */
    @NonNull
    public ListenableFuture<SessionResult> skipToPlaylistItem(@IntRange(from = 0) int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        if (isConnected()) {
            return getImpl().skipToPlaylistItem(index);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the cached repeat mode from the {@link ControllerCallback#onRepeatModeChanged}.
     * If it is not connected yet, it returns {@link SessionPlayer#REPEAT_MODE_NONE}.
     *
     * @return repeat mode
     * @see SessionPlayer#REPEAT_MODE_NONE
     * @see SessionPlayer#REPEAT_MODE_ONE
     * @see SessionPlayer#REPEAT_MODE_ALL
     * @see SessionPlayer#REPEAT_MODE_GROUP
     */
    @RepeatMode
    public int getRepeatMode() {
        return isConnected() ? getImpl().getRepeatMode() : REPEAT_MODE_NONE;
    }

    /**
     * Sets the repeat mode.
     *
     * @param repeatMode repeat mode
     * @see SessionPlayer#REPEAT_MODE_NONE
     * @see SessionPlayer#REPEAT_MODE_ONE
     * @see SessionPlayer#REPEAT_MODE_ALL
     * @see SessionPlayer#REPEAT_MODE_GROUP
     */
    @NonNull
    public ListenableFuture<SessionResult> setRepeatMode(@RepeatMode int repeatMode) {
        if (isConnected()) {
            return getImpl().setRepeatMode(repeatMode);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the cached shuffle mode from the {@link ControllerCallback#onShuffleModeChanged}.
     * If it is not connected yet, it returns {@link SessionPlayer#SHUFFLE_MODE_NONE}.
     *
     * @return The shuffle mode
     * @see SessionPlayer#SHUFFLE_MODE_NONE
     * @see SessionPlayer#SHUFFLE_MODE_ALL
     * @see SessionPlayer#SHUFFLE_MODE_GROUP
     */
    @ShuffleMode
    public int getShuffleMode() {
        return isConnected() ? getImpl().getShuffleMode() : SHUFFLE_MODE_NONE;
    }

    /**
     * Sets the shuffle mode.
     *
     * @param shuffleMode The shuffle mode
     * @see SessionPlayer#SHUFFLE_MODE_NONE
     * @see SessionPlayer#SHUFFLE_MODE_ALL
     * @see SessionPlayer#SHUFFLE_MODE_GROUP
     */
    @NonNull
    public ListenableFuture<SessionResult> setShuffleMode(@ShuffleMode int shuffleMode) {
        if (isConnected()) {
            return getImpl().setShuffleMode(shuffleMode);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the cached video size from the {@link ControllerCallback#onVideoSizeChanged}.
     * If it is not connected yet, it returns {@code new VideoSize(0, 0)}.
     *
     * @return The video size
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public VideoSize getVideoSize() {
        return isConnected() ? getImpl().getVideoSize() : new VideoSize(0, 0);
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of the media.
     * <p>
     * This calls {@link SessionPlayer#setSurfaceInternal(Surface)}.
     *
     * @param surface The {@link Surface} to be used for the video portion of the media.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<SessionResult> setSurface(@Nullable Surface surface) {
        if (isConnected()) {
            return getImpl().setSurface(surface);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the cached track info list from the
     * {@link ControllerCallback#onTrackInfoChanged(MediaController, List)}.
     * The types of tracks supported may vary based on player implementation.
     * If it is not connected yet, it returns null.
     *
     * @return List of tracks. The total number of tracks is the size of the list. If empty,
     *         the implementation should return a empty list instead of {@code null}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public List<TrackInfo> getTrackInfo() {
        return isConnected() ? getImpl().getTrackInfo() : null;
    }

    /**
     * Selects the {@link TrackInfo} for the current media item.
     * The types of tracks supported may vary based on player implementation.
     *
     * @param trackInfo track to be selected.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<SessionResult> selectTrack(@NonNull TrackInfo trackInfo) {
        if (trackInfo == null) {
            throw new NullPointerException("TrackInfo shouldn't be null");
        }
        return isConnected() ? getImpl().selectTrack(trackInfo) : createDisconnectedFuture();
    }

    /**
     * Deselects the {@link TrackInfo} for the current media item.
     * The types of tracks supported may vary based on player implementation.
     *
     * @param trackInfo track to be deselected.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public ListenableFuture<SessionResult> deselectTrack(@NonNull TrackInfo trackInfo) {
        if (trackInfo == null) {
            throw new NullPointerException("TrackInfo shouldn't be null");
        }
        return isConnected() ? getImpl().deselectTrack(trackInfo) : createDisconnectedFuture();
    }

    /**
     * Gets the currently selected track for the given {@link TrackInfo.MediaTrackType}. The return
     * value is an element in the list returned by {@link #getTrackInfo()} and supported track types
     * may vary based on the player implementation.
     *
     * The returned value can be outdated after
     * {@link ControllerCallback#onTrackInfoChanged(MediaController, List)},
     * {@link ControllerCallback#onTrackSelected(MediaController, TrackInfo)},
     * or {@link ControllerCallback#onTrackDeselected(MediaController, TrackInfo)} is called.
     *
     * If it is not connected yet, it returns null.
     *
     * @param trackType type of selected track
     * @return selected track info
     *
     * @hide
     */
    // TODO: revise the method document once subtitle track support is re-enabled. (b/130312596)
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    public TrackInfo getSelectedTrack(@TrackInfo.MediaTrackType int trackType) {
        return isConnected() ? getImpl().getSelectedTrack(trackType) : null;
    }

    /**
     * Sets the time diff forcefully when calculating current position.
     * @param timeDiff {@code null} for reset.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setTimeDiff(Long timeDiff) {
        mTimeDiff = timeDiff;
    }

    /**
     * Registers an extra {@link ControllerCallback}.
     * @param executor a callback executor
     * @param callback a ControllerCallback
     * @see #unregisterExtraCallback(ControllerCallback)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void registerExtraCallback(@NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull ControllerCallback callback) {
        if (executor == null) {
            throw new NullPointerException("executor shouldn't be null");
        }
        if (callback == null) {
            throw new NullPointerException("callback shouldn't be null");
        }
        boolean found = false;
        synchronized (mLock) {
            for (Pair<ControllerCallback, Executor> pair : mExtraCallbacks) {
                if (pair.first == callback) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mExtraCallbacks.add(new Pair<>(callback, executor));
            }
        }
        if (found) {
            Log.w(TAG, "registerExtraCallback: the callback already exists");
        }
    }

    /**
     * Unregisters an {@link ControllerCallback} that has been registered by
     * {@link #registerExtraCallback(Executor, ControllerCallback)}.
     * The callback passed to {@link Builder#setControllerCallback(Executor, ControllerCallback)}
     * can not be unregistered by this method.
     * @param callback a ControllerCallback
     * @see #registerExtraCallback(Executor, ControllerCallback)
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void unregisterExtraCallback(@NonNull ControllerCallback callback) {
        if (callback == null) {
            throw new NullPointerException("callback shouldn't be null");
        }
        boolean found = false;
        synchronized (mLock) {
            for (int i = mExtraCallbacks.size() - 1; i >= 0; i--) {
                if (mExtraCallbacks.get(i).first == callback) {
                    found = true;
                    mExtraCallbacks.remove(i);
                    break;
                }
            }
        }
        if (!found) {
            Log.w(TAG, "unregisterExtraCallback: no such callback found");
        }
    }

    /** @hide */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @RestrictTo(LIBRARY_GROUP) // TODO: LIBRARY_GROUP -> LIBRARY (b/131782509)
    @NonNull
    public List<Pair<ControllerCallback, Executor>> getExtraCallbacks() {
        List<Pair<ControllerCallback, Executor>> extraCallbacks;
        synchronized (mLock) {
            extraCallbacks = new ArrayList<>(mExtraCallbacks);
        }
        return extraCallbacks;
    }

    /**
     * Gets the cached allowed commands from {@link ControllerCallback#onAllowedCommandsChanged}.
     * If it is not connected yet, it returns {@code null}.
     *
     * @return the allowed commands
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    public SessionCommandGroup getAllowedCommands() {
        if (!isConnected()) {
            return null;
        }
        return getImpl().getAllowedCommands();
    }

    private static ListenableFuture<SessionResult> createDisconnectedFuture() {
        return SessionResult.createFutureWithResult(
                SessionResult.RESULT_ERROR_SESSION_DISCONNECTED);
    }

    /** @hide */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @RestrictTo(LIBRARY_GROUP) // TODO: LIBRARY_GROUP -> LIBRARY (b/131782509)
    public void notifyControllerCallback(final ControllerCallbackRunnable callbackRunnable) {
        if (mCallback != null && mCallbackExecutor != null) {
            mCallbackExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    callbackRunnable.run(mCallback);
                }
            });
        }

        for (Pair<ControllerCallback, Executor> pair : getExtraCallbacks()) {
            final ControllerCallback callback = pair.first;
            final Executor executor = pair.second;
            if (callback == null) {
                Log.e(TAG, "notifyControllerCallback: mExtraCallbacks contains a null "
                        + "ControllerCallback! Ignoring...");
                continue;
            }
            if (executor == null) {
                Log.e(TAG, "notifyControllerCallback: mExtraCallbacks contains a null "
                        + "Executor! Ignoring...");
                continue;
            }
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callbackRunnable.run(callback);
                }
            });
        }
    }

    /** @hide */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @RestrictTo(LIBRARY_GROUP) // TODO: LIBRARY_GROUP -> LIBRARY (b/131782509)
    public interface ControllerCallbackRunnable {
        void run(@NonNull ControllerCallback callback);
    }

    interface MediaControllerImpl extends AutoCloseable {
        @Nullable SessionToken getConnectedToken();
        boolean isConnected();
        ListenableFuture<SessionResult> play();
        ListenableFuture<SessionResult> pause();
        ListenableFuture<SessionResult> prepare();
        ListenableFuture<SessionResult> fastForward();
        ListenableFuture<SessionResult> rewind();
        ListenableFuture<SessionResult> seekTo(long pos);
        ListenableFuture<SessionResult> skipForward();
        ListenableFuture<SessionResult> skipBackward();
        ListenableFuture<SessionResult> playFromMediaId(@NonNull String mediaId,
                @Nullable Bundle extras);
        ListenableFuture<SessionResult> playFromSearch(@NonNull String query,
                @Nullable Bundle extras);
        ListenableFuture<SessionResult> playFromUri(@NonNull Uri uri, @Nullable Bundle extras);
        ListenableFuture<SessionResult> prepareFromMediaId(@NonNull String mediaId,
                @Nullable Bundle extras);
        ListenableFuture<SessionResult> prepareFromSearch(@NonNull String query,
                @Nullable Bundle extras);
        ListenableFuture<SessionResult> prepareFromUri(@NonNull Uri uri,
                @Nullable Bundle extras);
        ListenableFuture<SessionResult> setVolumeTo(int value, @VolumeFlags int flags);
        ListenableFuture<SessionResult> adjustVolume(@VolumeDirection int direction,
                @VolumeFlags int flags);
        @Nullable
        PendingIntent getSessionActivity();
        int getPlayerState();
        long getDuration();
        long getCurrentPosition();
        float getPlaybackSpeed();
        ListenableFuture<SessionResult> setPlaybackSpeed(float speed);
        @SessionPlayer.BuffState
        int getBufferingState();
        long getBufferedPosition();
        @Nullable
        PlaybackInfo getPlaybackInfo();
        ListenableFuture<SessionResult> setRating(@NonNull String mediaId,
                @NonNull Rating rating);
        ListenableFuture<SessionResult> sendCustomCommand(@NonNull SessionCommand command,
                @Nullable Bundle args);
        @Nullable
        List<MediaItem> getPlaylist();
        ListenableFuture<SessionResult> setPlaylist(@NonNull List<String> list,
                @Nullable MediaMetadata metadata);
        ListenableFuture<SessionResult> setMediaItem(@NonNull String mediaId);
        ListenableFuture<SessionResult> updatePlaylistMetadata(
                @Nullable MediaMetadata metadata);
        @Nullable MediaMetadata getPlaylistMetadata();
        ListenableFuture<SessionResult> addPlaylistItem(int index, @NonNull String mediaId);
        ListenableFuture<SessionResult> removePlaylistItem(int index);
        ListenableFuture<SessionResult> replacePlaylistItem(int index,
                @NonNull String mediaId);
        MediaItem getCurrentMediaItem();
        int getCurrentMediaItemIndex();
        int getPreviousMediaItemIndex();
        int getNextMediaItemIndex();
        ListenableFuture<SessionResult> skipToPreviousItem();
        ListenableFuture<SessionResult> skipToNextItem();
        ListenableFuture<SessionResult> skipToPlaylistItem(int index);
        @RepeatMode
        int getRepeatMode();
        ListenableFuture<SessionResult> setRepeatMode(@RepeatMode int repeatMode);
        @ShuffleMode
        int getShuffleMode();
        ListenableFuture<SessionResult> setShuffleMode(@ShuffleMode int shuffleMode);
        @NonNull
        VideoSize getVideoSize();
        ListenableFuture<SessionResult> setSurface(@Nullable Surface surface);
        @Nullable
        List<TrackInfo> getTrackInfo();
        ListenableFuture<SessionResult> selectTrack(TrackInfo trackInfo);
        ListenableFuture<SessionResult> deselectTrack(TrackInfo trackInfo);
        @Nullable
        TrackInfo getSelectedTrack(@TrackInfo.MediaTrackType int trackType);
        @Nullable
        SessionCommandGroup getAllowedCommands();

        // Internally used methods
        @NonNull
        Context getContext();
        @Nullable
        MediaBrowserCompat getBrowserCompat();
    }


    /**
     * Builder for {@link MediaController}.
     * <p>
     * To set the token of the session for the controller to connect to, one of the
     * {@link #setSessionToken(SessionToken)} or
     * {@link #setSessionCompatToken(MediaSessionCompat.Token)} should be called.
     * Otherwise, the {@link #build()} will throw an {@link IllegalArgumentException}.
     * <p>
     * Any incoming event from the {@link MediaSession} will be handled on the callback
     * executor.
     */
    public static final class Builder extends BuilderBase<MediaController, Builder,
            ControllerCallback> {
        public Builder(@NonNull Context context) {
            super(context);
        }

        @Override
        @NonNull
        public Builder setSessionToken(@NonNull SessionToken token) {
            return super.setSessionToken(token);
        }

        @Override
        @NonNull
        public Builder setSessionCompatToken(@NonNull MediaSessionCompat.Token compatToken) {
            return super.setSessionCompatToken(compatToken);
        }

        @Override
        @NonNull
        public Builder setControllerCallback(@NonNull Executor executor,
                @NonNull ControllerCallback callback) {
            return super.setControllerCallback(executor, callback);
        }

        /**
         * Build {@link MediaController}.
         * <p>
         * It will throw an {@link IllegalArgumentException} if both {@link SessionToken} and
         * {@link MediaSessionCompat.Token} are not set.
         *
         * @return a new controller
         */
        @Override
        @NonNull
        public MediaController build() {
            if (mToken == null && mCompatToken == null) {
                throw new IllegalArgumentException("token and compat token shouldn't be both null");
            }
            if (mToken != null) {
                return new MediaController(mContext, mToken, mConnectionHints,
                        mCallbackExecutor, mCallback);
            } else {
                return new MediaController(mContext, mCompatToken, mConnectionHints,
                        mCallbackExecutor, mCallback);
            }
        }
    }

    /**
     * Base builder class for MediaController and its subclass. Any change in this class should be
     * also applied to the subclasses {@link MediaController.Builder} and
     * {@link MediaBrowser.Builder}.
     * <p>
     * APIs here should be package private, but should have documentations for developers.
     * Otherwise, javadoc will generate documentation with the generic types such as follows.
     * <pre>U extends BuilderBase<T, U, C> setControllerCallback(Executor executor,
     * C callback)</pre>
     * <p>
     * This class is hidden to prevent from generating test stub, which fails with
     * 'unexpected bound' because it tries to auto generate stub class as follows.
     * <pre>abstract static class BuilderBase<
     *      T extends androidx.media2.MediaController,
     *      U extends androidx.media2.MediaController.BuilderBase<
     *              T, U, C extends androidx.media2.MediaController.ControllerCallback>, C></pre>
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    abstract static class BuilderBase<T extends MediaController, U extends BuilderBase<T, U, C>,
            C extends ControllerCallback> {
        final Context mContext;
        SessionToken mToken;
        MediaSessionCompat.Token mCompatToken;
        Bundle mConnectionHints;
        Executor mCallbackExecutor;
        ControllerCallback mCallback;

        /**
         * Creates a builder for {@link MediaController}.
         *
         * @param context context
         */
        BuilderBase(@NonNull Context context) {
            if (context == null) {
                throw new NullPointerException("context shouldn't be null");
            }
            mContext = context;
        }

        /**
         * Set the {@link SessionToken} for the controller to connect to.
         * <p>
         * When this method is called, the {@link MediaSessionCompat.Token} which was set by calling
         * {@link #setSessionCompatToken} is removed.
         * <p>
         * Detailed behavior of the {@link MediaController} differs according to the type of the
         * token as follows.
         * <p>
         * <ol>
         * <li>Connected to a {@link SessionToken#TYPE_SESSION} token
         * <p>
         * The controller connects to the specified session directly. It's recommended when you're
         * sure which session to control, or a you've got token directly from the session app.
         * <p>
         * This can be used only when the session for the token is running. Once the session is
         * closed, the token becomes unusable.
         * </li>
         * <li>Connected to a {@link SessionToken#TYPE_SESSION_SERVICE} or
         * {@link SessionToken#TYPE_LIBRARY_SERVICE}
         * <p>
         * The controller connects to the session provided by the
         * {@link MediaSessionService#onGetSession(ControllerInfo)}.
         * It's up to the service's decision which session would be returned for the connection.
         * Use the {@link #getConnectedSessionToken()} to know the connected session.
         * <p>
         * This can be used regardless of the session app is running or not. The controller would
         * bind to the service while connected to wake up and keep the service process running.
         * </li>
         * </ol>
         *
         * @param token token to connect to
         * @return The Builder to allow chaining
         * @see MediaSessionService#onGetSession(ControllerInfo)
         * @see #getConnectedSessionToken()
         * @see #setConnectionHints(Bundle)
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public U setSessionToken(@NonNull SessionToken token) {
            if (token == null) {
                throw new NullPointerException("token shouldn't be null");
            }
            mToken = token;
            mCompatToken = null;
            return (U) this;
        }

        /**
         * Set the {@link MediaSessionCompat.Token} for the controller to connect to.
         * <p>
         * When this method is called, the {@link SessionToken} which was set by calling
         * {@link #setSessionToken(SessionToken)} is removed.
         *
         * @param compatToken token to connect to
         * @return The Builder to allow chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public U setSessionCompatToken(@NonNull MediaSessionCompat.Token compatToken) {
            if (compatToken == null) {
                throw new NullPointerException("compatToken shouldn't be null");
            }
            mCompatToken = compatToken;
            mToken = null;
            return (U) this;
        }

        /**
         * Set the connection hints for the controller.
         * <p>
         * {@code connectionHints} is a session-specific argument to send to the session when
         * connecting. The contents of this bundle may affect the connection result.
         * <p>
         * The hints specified here are only used when when connecting to the {@link MediaSession}.
         * They will be ignored when connecting to {@link MediaSessionCompat}.
         *
         * @param connectionHints a bundle which contains the connection hints
         * @return The Builder to allow chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public U setConnectionHints(@NonNull Bundle connectionHints) {
            if (connectionHints == null) {
                throw new NullPointerException("connectionHints shouldn't be null");
            }
            mConnectionHints = new Bundle(connectionHints);
            return (U) this;
        }

        /**
         * Set callback for the controller and its executor.
         *
         * @param executor callback executor
         * @param callback controller callback.
         * @return The Builder to allow chaining
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public U setControllerCallback(@NonNull Executor executor, @NonNull C callback) {
            if (executor == null) {
                throw new NullPointerException("executor shouldn't be null");
            }
            if (callback == null) {
                throw new NullPointerException("callback shouldn't be null");
            }
            mCallbackExecutor = executor;
            mCallback = callback;
            return (U) this;
        }

        @NonNull
        abstract T build();
    }

    /**
     * Interface for listening to change in activeness of the {@link MediaSession}.  It's
     * active if and only if it has set a player.
     */
    public abstract static class ControllerCallback {
        /**
         * Called when the controller is successfully connected to the session. The controller
         * becomes available afterwards.
         *
         * @param controller the controller for this event
         * @param allowedCommands commands that's allowed by the session.
         */
        public void onConnected(@NonNull MediaController controller,
                @NonNull SessionCommandGroup allowedCommands) {}

        /**
         * Called when the session refuses the controller or the controller is disconnected from
         * the session. The controller becomes unavailable afterwards and the callback wouldn't
         * be called.
         * <p>
         * It will be also called after the {@link #close()}, so you can put clean up code here.
         * You don't need to call {@link #close()} after this.
         *
         * @param controller the controller for this event
         */
        public void onDisconnected(@NonNull MediaController controller) {}

        /**
         * Called when the session set the custom layout through the
         * {@link MediaSession#setCustomLayout(MediaSession.ControllerInfo, List)}.
         * <p>
         * Can be called before {@link #onConnected(MediaController, SessionCommandGroup)}
         * is called.
         * <p>
         * Default implementation returns {@link SessionResult#RESULT_ERROR_NOT_SUPPORTED}.
         *
         * @param controller the controller for this event
         * @param layout
         */
        @SessionResult.ResultCode
        public int onSetCustomLayout(
                @NonNull MediaController controller, @NonNull List<CommandButton> layout) {
            return SessionResult.RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when the session has changed anything related with the {@link PlaybackInfo}.
         * <p>
         * Interoperability: When connected to
         * {@link android.support.v4.media.session.MediaSessionCompat}, this may be called when the
         * session changes playback info by calling
         * {@link android.support.v4.media.session.MediaSessionCompat#setPlaybackToLocal(int)} or
         * {@link android.support.v4.media.session.MediaSessionCompat#setPlaybackToRemote(
         * VolumeProviderCompat)}}. Specifically:
         * <ul>
         * <li> Prior to API 21, this will always be called whenever any of those two methods is
         *      called.
         * <li> From API 21 to 22, this is called only when the playback type is changed from local
         *      to remote (i.e. not from remote to local).
         * <li> From API 23, this is called only when the playback type is changed.
         * </ul>
         *
         * @param controller the controller for this event
         * @param info new playback info
         */
        public void onPlaybackInfoChanged(@NonNull MediaController controller,
                @NonNull PlaybackInfo info) {}

        /**
         * Called when the allowed commands are changed by session.
         *
         * @param controller the controller for this event
         * @param commands newly allowed commands
         */
        public void onAllowedCommandsChanged(@NonNull MediaController controller,
                @NonNull SessionCommandGroup commands) {}

        /**
         * Called when the session sent a custom command. Returns a {@link SessionResult} for
         * session to get notification back. If the {@code null} is returned,
         * {@link SessionResult#RESULT_ERROR_UNKNOWN} will be returned.
         * <p>
         * Default implementation returns {@link SessionResult#RESULT_ERROR_NOT_SUPPORTED}.
         *
         * @param controller the controller for this event
         * @param command
         * @param args
         * @return result of handling custom command
         */
        @NonNull
        public SessionResult onCustomCommand(@NonNull MediaController controller,
                @NonNull SessionCommand command, @Nullable Bundle args) {
            return new SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED);
        }

        /**
         * Called when the player state is changed.
         *
         * @param controller the controller for this event
         * @param state the new player state
         */
        public void onPlayerStateChanged(@NonNull MediaController controller,
                @SessionPlayer.PlayerState int state) {}

        /**
         * Called when playback speed is changed.
         *
         * @param controller the controller for this event
         * @param speed speed
         */
        public void onPlaybackSpeedChanged(@NonNull MediaController controller,
                float speed) {}

        /**
         * Called to report buffering events for a media item.
         * <p>
         * Use {@link #getBufferedPosition()} for current buffering position.
         *
         * @param controller the controller for this event
         * @param item the media item for which buffering is happening.
         * @param state the new buffering state.
         */
        public void onBufferingStateChanged(@NonNull MediaController controller,
                @NonNull MediaItem item, @SessionPlayer.BuffState int state) {}

        /**
         * Called to indicate that seeking is completed.
         *
         * @param controller the controller for this event.
         * @param position the previous seeking request.
         */
        public void onSeekCompleted(@NonNull MediaController controller, long position) {}

        /**
         * Called when the player's current item is changed. It's also called after
         * {@link #setPlaylist} or {@link #setMediaItem}.
         * Also called when {@link MediaItem#setMetadata(MediaMetadata)} is called on the current
         * media item.
         * <p>
         * When it's called, you should invalidate previous playback information and wait for later
         * callbacks. Also, current, previous, and next media item indices may need to be updated.
         *
         * @param controller the controller for this event
         * @param item new current media item
         * @see #getPlaylist()
         * @see #getPlaylistMetadata()
         */
        public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                @Nullable MediaItem item) {}

        /**
         * Called when a playlist is changed. It's also called after {@link #setPlaylist} or
         * {@link #setMediaItem}.
         * Also called when {@link MediaItem#setMetadata(MediaMetadata)} is called on a media item
         * that is contained in the current playlist.
         * <p>
         * When it's called, current, previous, and next media item indices may need to be updated.
         *
         * @param controller the controller for this event
         * @param list new playlist
         * @param metadata new metadata
         * @see #getPlaylist()
         * @see #getPlaylistMetadata()
         */
        public void onPlaylistChanged(@NonNull MediaController controller,
                @Nullable List<MediaItem> list, @Nullable MediaMetadata metadata) {}

        /**
         * Called when a playlist metadata is changed.
         *
         * @param controller the controller for this event
         * @param metadata new metadata
         */
        public void onPlaylistMetadataChanged(@NonNull MediaController controller,
                @Nullable MediaMetadata metadata) {}

        /**
         * Called when the shuffle mode is changed.
         *
         * @param controller the controller for this event
         * @param shuffleMode repeat mode
         * @see SessionPlayer#SHUFFLE_MODE_NONE
         * @see SessionPlayer#SHUFFLE_MODE_ALL
         * @see SessionPlayer#SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaController controller,
                @SessionPlayer.ShuffleMode int shuffleMode) {}

        /**
         * Called when the repeat mode is changed.
         *
         * @param controller the controller for this event
         * @param repeatMode repeat mode
         * @see SessionPlayer#REPEAT_MODE_NONE
         * @see SessionPlayer#REPEAT_MODE_ONE
         * @see SessionPlayer#REPEAT_MODE_ALL
         * @see SessionPlayer#REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaController controller,
                @SessionPlayer.RepeatMode int repeatMode) {}

        /**
         * Called when the playback is completed.
         *
         * @param controller the controller for this event
         */
        public void onPlaybackCompleted(@NonNull MediaController controller) {}

        /**
         * Called when video size is changed.
         *
         * @param controller the controller for this event
         * @param item the media item for which the video size changed
         * @param videoSize the size of video
         *
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onVideoSizeChanged(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {}

        /**
         * Called when the tracks are first retrieved after media is prepared or when new tracks are
         * found during playback.
         * <p>
         * When it's called, you should invalidate previous track information and use the new
         * tracks to call {@link #selectTrack(TrackInfo)} or
         * {@link #deselectTrack(TrackInfo)}.
         * <p>
         * The types of tracks supported may vary based on player implementation.
         *
         * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
         * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
         * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
         * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
         *
         * @param controller the controller for this event
         * @param trackInfos the list of track infos
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onTrackInfoChanged(@NonNull MediaController controller,
                @NonNull List<TrackInfo> trackInfos) {}

        /**
         * Called when a track is selected.
         * <p>
         * The types of tracks supported may vary based on player implementation, but generally
         * one track will be selected for each track type.
         *
         * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
         * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
         * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
         * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
         *
         * @param controller the controller for this event
         * @param trackInfo the selected track
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onTrackSelected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {}

        /**
         * Called when a track is deselected.
         * <p>
         * The types of tracks supported may vary based on player implementation, but generally
         * a track should already be selected in order to be deselected and audio and video tracks
         * should not be deselected.
         *
         * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
         * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
         * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
         * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
         *
         * @param controller the controller for this event
         * @param trackInfo the deselected track
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onTrackDeselected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {}

        /**
         * Called when the subtitle track has new subtitle data available.
         * @param controller the controller for this event
         * @param item the MediaItem of this media item
         * @param track the track that has the subtitle data
         * @param data the subtitle data
         *
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onSubtitleData(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {}
    }

    /**
     * Holds information about the the way volume is handled for this session.
     */
    // The same as MediaController.PlaybackInfo
    @VersionedParcelize
    public static final class PlaybackInfo implements VersionedParcelable {
        @ParcelField(1)
        int mPlaybackType;
        @ParcelField(2)
        int mControlType;
        @ParcelField(3)
        int mMaxVolume;
        @ParcelField(4)
        int mCurrentVolume;
        @ParcelField(5)
        AudioAttributesCompat mAudioAttrsCompat;

        /**
         * The session uses local playback.
         */
        public static final int PLAYBACK_TYPE_LOCAL = 1;
        /**
         * The session uses remote playback.
         */
        public static final int PLAYBACK_TYPE_REMOTE = 2;

        /**
         * Used for VersionedParcelable
         */
        PlaybackInfo() {
        }

        PlaybackInfo(int playbackType, AudioAttributesCompat attrs, int controlType, int max,
                int current) {
            mPlaybackType = playbackType;
            mAudioAttrsCompat = attrs;
            mControlType = controlType;
            mMaxVolume = max;
            mCurrentVolume = current;
        }

        /**
         * Get the type of playback which affects volume handling. One of:
         * <ul>
         * <li>{@link #PLAYBACK_TYPE_LOCAL}</li>
         * <li>{@link #PLAYBACK_TYPE_REMOTE}</li>
         * </ul>
         *
         * @return The type of playback this session is using
         */
        public int getPlaybackType() {
            return mPlaybackType;
        }

        /**
         * Get the audio attributes for this session. The attributes will affect
         * volume handling for the session. When the volume type is
         * {@link #PLAYBACK_TYPE_REMOTE} these may be ignored by the
         * remote volume handler.
         *
         * @return The attributes for this session
         */
        @Nullable
        public AudioAttributesCompat getAudioAttributes() {
            return mAudioAttrsCompat;
        }

        /**
         * Get the type of volume control that can be used. One of:
         * <ul>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_ABSOLUTE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_RELATIVE}</li>
         * <li>{@link VolumeProviderCompat#VOLUME_CONTROL_FIXED}</li>
         * </ul>
         *
         * @return The type of volume control that may be used with this session
         */
        public int getControlType() {
            return mControlType;
        }

        /**
         * Get the maximum volume that may be set for this session.
         * <p>
         * This is only meaningful when the playback type is {@link #PLAYBACK_TYPE_REMOTE}.
         *
         * @return The maximum allowed volume where this session is playing
         */
        public int getMaxVolume() {
            return mMaxVolume;
        }

        /**
         * Get the current volume for this session.
         * <p>
         * This is only meaningful when the playback type is {@link #PLAYBACK_TYPE_REMOTE}.
         *
         * @return The current volume where this session is playing
         */
        public int getCurrentVolume() {
            return mCurrentVolume;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(
                    mPlaybackType, mControlType, mMaxVolume, mCurrentVolume, mAudioAttrsCompat);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof PlaybackInfo)) {
                return false;
            }
            PlaybackInfo other = (PlaybackInfo) obj;
            return mPlaybackType == other.mPlaybackType
                    && mControlType == other.mControlType
                    && mMaxVolume == other.mMaxVolume
                    && mCurrentVolume == other.mCurrentVolume
                    && ObjectsCompat.equals(mAudioAttrsCompat, other.mAudioAttrsCompat);
        }

        static PlaybackInfo createPlaybackInfo(int playbackType, AudioAttributesCompat attrs,
                int controlType, int max, int current) {
            return new PlaybackInfo(playbackType, attrs, controlType, max, current);
        }
    }
}
