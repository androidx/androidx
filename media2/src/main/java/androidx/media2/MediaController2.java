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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.SessionPlayer2.BUFFERING_STATE_UNKNOWN;
import static androidx.media2.SessionPlayer2.PLAYER_STATE_IDLE;
import static androidx.media2.SessionPlayer2.REPEAT_MODE_NONE;
import static androidx.media2.SessionPlayer2.SHUFFLE_MODE_NONE;
import static androidx.media2.SessionPlayer2.UNKNOWN_TIME;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.VolumeProviderCompat;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.SessionResult;
import androidx.media2.SessionPlayer2.RepeatMode;
import androidx.media2.SessionPlayer2.ShuffleMode;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Allows an app to interact with an active {@link MediaSession2} or a
 * {@link MediaSessionService2} which would provide {@link MediaSession2}. Media buttons and other
 * commands can be sent to the session.
 * <p>
 * MediaController2 objects are thread-safe.
 * <p>
 * Topic covered here:
 * <ol>
 * <li><a href="#ControllerLifeCycle">Controller Lifecycle</a>
 * </ol>
 * <a name="ControllerLifeCycle"></a>
 * <h3>Controller Lifecycle</h3>
 * <p>
 * When a controller is created with the {@link SessionToken2} for a {@link MediaSession2} (i.e.
 * session token type is {@link SessionToken2#TYPE_SESSION}), the controller will connect to the
 * specific session.
 * <p>
 * When a controller is created with the {@link SessionToken2} for a {@link MediaSessionService2}
 * (i.e. session token type is {@link SessionToken2#TYPE_SESSION_SERVICE} or
 * {@link SessionToken2#TYPE_LIBRARY_SERVICE}), the controller binds to the service for connecting
 * to a {@link MediaSession2} in it. {@link MediaSessionService2} will provide a session to connect.
 * <p>
 * When a controller connects to a session,
 * {@link MediaSession2.SessionCallback#onConnect(MediaSession2, ControllerInfo)} will be called to
 * either accept or reject the connection. Wait
 * {@link ControllerCallback#onConnected(MediaController2, SessionCommandGroup2)} or
 * {@link ControllerCallback#onDisconnected(MediaController2)} for the result.
 * <p>
 * When the connected session is closed, the controller will receive
 * {@link ControllerCallback#onDisconnected(MediaController2)}.
 * <p>
 * When you're done, use {@link #close()} to clean up resources. This also helps session service
 * to be destroyed when there's no controller associated with it.
 *
 * @see MediaSession2
 * @see MediaSessionService2
 */
@TargetApi(Build.VERSION_CODES.P)
public class MediaController2 implements AutoCloseable {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({AudioManager.ADJUST_LOWER, AudioManager.ADJUST_RAISE, AudioManager.ADJUST_SAME,
            AudioManager.ADJUST_MUTE, AudioManager.ADJUST_UNMUTE, AudioManager.ADJUST_TOGGLE_MUTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeDirection {}

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef(value = {AudioManager.FLAG_SHOW_UI, AudioManager.FLAG_ALLOW_RINGER_MODES,
            AudioManager.FLAG_PLAY_SOUND, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
            AudioManager.FLAG_VIBRATE}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface VolumeFlags {}

    final Object mLock = new Object();
    @GuardedBy("mLock")
    MediaController2Impl mImpl;
    @GuardedBy("mLock")
    boolean mClosed;

    // For testing.
    Long mTimeDiff;

    /**
     * Create a {@link MediaController2} from the {@link SessionToken2}.
     * This connects to the session and may wake up the service if it's not available.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    public MediaController2(@NonNull final Context context, @NonNull final SessionToken2 token,
            @NonNull final Executor executor, @NonNull final ControllerCallback callback) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        synchronized (mLock) {
            mImpl = createImpl(context, token, executor, callback);
        }
    }

    /**
     * Create a {@link MediaController2} from the {@link MediaSessionCompat.Token}.
     * This connects to the session and may wake up the service if it's not available.
     *
     * @param context Context
     * @param token token to connect to
     * @param executor executor to run callbacks on.
     * @param callback controller callback to receive changes in
     */
    public MediaController2(@NonNull final Context context,
            @NonNull final MediaSessionCompat.Token token,
            @NonNull final Executor executor, @NonNull final ControllerCallback callback) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        SessionToken2.createSessionToken2(context, token, executor,
                new SessionToken2.OnSessionToken2CreatedListener() {
                    @Override
                    public void onSessionToken2Created(MediaSessionCompat.Token token,
                            SessionToken2 token2) {
                        synchronized (mLock) {
                            if (!mClosed) {
                                mImpl = createImpl(context, token2, executor, callback);
                            } else {
                                executor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        callback.onDisconnected(MediaController2.this);
                                    }
                                });
                            }
                        }
                    }
                });
    }

    MediaController2Impl createImpl(@NonNull Context context, @NonNull SessionToken2 token,
            @NonNull Executor executor, @NonNull ControllerCallback callback) {
        if (token.isLegacySession()) {
            return new MediaController2ImplLegacy(context, this, token, executor, callback);
        } else {
            return new MediaController2ImplBase(context, this, token, executor, callback);
        }
    }

    MediaController2Impl getImpl() {
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
            MediaController2Impl impl;
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
     * Returns {@link SessionToken2} of the connected session.
     * If it is not connected yet, it returns {@code null}.
     * <p>
     * This may differ with the {@link SessionToken2} from the constructor. For example, if the
     * controller is created with the token for {@link MediaSessionService2}, this would return
     * token for the {@link MediaSession2} in the service.
     *
     * @return SessionToken2 of the connected session, or {@code null} if not connected
     */
    @Nullable
    public SessionToken2 getConnectedSessionToken() {
        return isConnected() ? getImpl().getConnectedSessionToken() : null;
    }

    /**
     * Returns whether this class is connected to active {@link MediaSession2} or not.
     */
    public boolean isConnected() {
        MediaController2Impl impl = getImpl();
        return impl != null && impl.isConnected();
    }

    /**
     * Requests that the player start or resume playback.
     */
    @NonNull
    public ListenableFuture<ControllerResult> play() {
        if (isConnected()) {
            return getImpl().play();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player pause playback.
     */
    @NonNull
    public ListenableFuture<ControllerResult> pause() {
        if (isConnected()) {
            return getImpl().pause();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests that the player prepare the media items for playback. In other words, other
     * sessions can continue to play during the prepare of this session. This method can be used
     * to speed up the start of the playback. Once the prepare is done, the player will change
     * its playback state to {@link SessionPlayer2#PLAYER_STATE_PAUSED}. Afterwards, {@link #play}
     * can be called to start playback.
     */
    @NonNull
    public ListenableFuture<ControllerResult> prepare() {
        if (isConnected()) {
            return getImpl().prepare();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to increase the playback speed.
     *
     * @see MediaSession2.SessionCallback#onFastForward(MediaSession2, ControllerInfo)
     */
    @NonNull
    public ListenableFuture<ControllerResult> fastForward() {
        if (isConnected()) {
            return getImpl().fastForward();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to decrease the playback speed.
     *
     * @see MediaSession2.SessionCallback#onRewind(MediaSession2, ControllerInfo)
     */
    @NonNull
    public ListenableFuture<ControllerResult> rewind() {
        if (isConnected()) {
            return getImpl().rewind();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to skip backward within the current media item.
     *
     * @see MediaSession2.SessionCallback#onSkipForward(MediaSession2, ControllerInfo)
     */
    @NonNull
    public ListenableFuture<ControllerResult> skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
        if (isConnected()) {
            return getImpl().skipForward();
        }
        return createDisconnectedFuture();
    }

    /**
     * Requests session to skip forward within the current media item.
     *
     * @see MediaSession2.SessionCallback#onSkipBackward(MediaSession2, ControllerInfo)
     */
    @NonNull
    public ListenableFuture<ControllerResult> skipBackward() {
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
    public ListenableFuture<ControllerResult> seekTo(long pos) {
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
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> playFromMediaId(@NonNull String mediaId,
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
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> playFromSearch(@NonNull String query,
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
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> playFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        if (uri == null) {
            throw new IllegalArgumentException("uri shouldn't be null");
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
     * {@link SessionPlayer2#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromMediaId} can be directly called
     * without this method.
     *
     * @param mediaId The non-empty media id
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> prepareFromMediaId(@NonNull String mediaId,
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
     * {@link SessionPlayer2#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromSearch} can be directly called
     * without this method.
     *
     * @param query The non-empty search query
     * @param extras Optional extras that can include extra information about the query.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> prepareFromSearch(@NonNull String query,
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
     * {@link SessionPlayer2#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be called to start
     * playback. If the prepare is not needed, {@link #playFromUri} can be directly called
     * without this method.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public ListenableFuture<ControllerResult> prepareFromUri(@NonNull Uri uri,
            @Nullable Bundle extras) {
        if (uri == null) {
            throw new IllegalArgumentException("uri shouldn't be null");
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
    public ListenableFuture<ControllerResult> setVolumeTo(int value, @VolumeFlags int flags) {
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
    public ListenableFuture<ControllerResult> adjustVolume(@VolumeDirection int direction,
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
     * {@link ControllerCallback#onPlayerStateChanged(MediaController2, int)}.
     * If it is not connected yet, it returns {@link SessionPlayer2#PLAYER_STATE_IDLE}.
     *
     * @return player state
     */
    public int getPlayerState() {
        return isConnected() ? getImpl().getPlayerState() : PLAYER_STATE_IDLE;
    }

    /**
     * Gets the duration of the current media item, or {@link SessionPlayer2#UNKNOWN_TIME} if
     * unknown or not connected.
     *
     * @return the duration in ms, or {@link SessionPlayer2#UNKNOWN_TIME}
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
     * @return the current playback position in ms, or {@link SessionPlayer2#UNKNOWN_TIME}
     *         if unknown or not connected
     */
    public long getCurrentPosition() {
        return isConnected() ? getImpl().getCurrentPosition() : UNKNOWN_TIME;
    }

    /**
     * Get the lastly cached playback speed from
     * {@link ControllerCallback#onPlaybackSpeedChanged(MediaController2, float)}.
     *
     * @return speed the lastly cached playback speed, or 0f if unknown or not connected
     */
    public float getPlaybackSpeed() {
        return isConnected() ? getImpl().getPlaybackSpeed() : 0f;
    }

    /**
     * Set the playback speed.
     */
    @NonNull
    public ListenableFuture<ControllerResult> setPlaybackSpeed(float speed) {
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
     * @return the buffering state, or {@link SessionPlayer2#BUFFERING_STATE_UNKNOWN}
     *         if unknown or not connected
     */
    public @SessionPlayer2.BuffState int getBufferingState() {
        return isConnected() ? getImpl().getBufferingState() : BUFFERING_STATE_UNKNOWN;
    }

    /**
     * Gets the lastly cached buffered position from the session when
     * {@link ControllerCallback#onBufferingStateChanged(MediaController2, MediaItem2, int)} is
     * called.
     *
     * @return buffering position in millis, or {@link SessionPlayer2#UNKNOWN_TIME} if
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
     * {@link MediaMetadata2#getRating(String)} with the key
     * {@link MediaMetadata2#METADATA_KEY_USER_RATING}.
     * <p>
     * If the user rating was {@code null}, the media item does not accept setting user rating.
     *
     * @param mediaId The non-empty media id
     * @param rating The rating to set
     */
    @NonNull
    public ListenableFuture<ControllerResult> setRating(@NonNull String mediaId,
            @NonNull Rating2 rating) {
        if (TextUtils.isEmpty(mediaId)) {
            throw new IllegalArgumentException("mediaId shouldn't be empty");
        }
        if (rating == null) {
            throw new IllegalArgumentException("rating shouldn't be null");
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
     * {@link ControllerResult#getResultCode()} will return the custom result code from the
     * {@link ResultReceiver#onReceiveResult(int, Bundle)} instead of the standard result codes
     * defined in the {@link ControllerResult}.
     *
     * @param command custom command
     * @param args optional argument
     */
    @NonNull
    public ListenableFuture<ControllerResult> sendCustomCommand(@NonNull SessionCommand2 command,
            @Nullable Bundle args) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        if (command.getCommandCode() != SessionCommand2.COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command should be a custom command");
        }
        if (isConnected()) {
            return getImpl().sendCustomCommand(command, args);
        }
        return createDisconnectedFuture();
    }

    /**
     * Returns the cached playlist from {@link ControllerCallback#onPlaylistChanged}.
     * <p>
     * This list may differ with the list that was specified with
     * {@link #setPlaylist(List, MediaMetadata2)} depending on the {@link SessionPlayer2}
     * implementation. Use media items returned here for other playlist agent APIs such as
     * {@link SessionPlayer2#skipToPlaylistItem(MediaItem2)}.
     *
     * @return playlist, or {@code null} if the playlist hasn't set, controller isn't connected,
     *         or it doesn't have enough permission
     * @see SessionCommand2#COMMAND_CODE_PLAYER_GET_PLAYLIST
     */
    @Nullable
    public List<MediaItem2> getPlaylist() {
        return isConnected() ? getImpl().getPlaylist() : null;
    }

    /**
     * Sets the playlist with the list of media IDs. All media IDs in the list shouldn't be empty.
     *
     * @param list list of media id. Shouldn't contain an empty id.
     * @param metadata metadata of the playlist
     * @see #getPlaylist()
     * @see ControllerCallback#onPlaylistChanged
     * @see MediaMetadata2#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<ControllerResult> setPlaylist(@NonNull List<String> list,
            @Nullable MediaMetadata2 metadata) {
        if (list == null) {
            throw new IllegalArgumentException("list shouldn't be null");
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
     * Sets a {@link MediaItem2} for playback.
     *
     * @param mediaId The non-empty media id of the item to play
     * @see MediaMetadata2#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<ControllerResult> setMediaItem(@NonNull String mediaId) {
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
    public ListenableFuture<ControllerResult> updatePlaylistMetadata(
            @Nullable MediaMetadata2 metadata) {
        if (isConnected()) {
            return getImpl().updatePlaylistMetadata(metadata);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the lastly cached playlist playlist metadata either from
     * {@link ControllerCallback#onPlaylistMetadataChanged} or
     * {@link ControllerCallback#onPlaylistChanged}.
     *
     * @return metadata metadata of the playlist, or null if none is set or the controller is not
     *         connected
     */
    @Nullable
    public MediaMetadata2 getPlaylistMetadata() {
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
     * @see MediaMetadata2#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<ControllerResult> addPlaylistItem(@IntRange(from = 0) int index,
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
    public ListenableFuture<ControllerResult> removePlaylistItem(@IntRange(from = 0) int index) {
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
     * @see MediaMetadata2#METADATA_KEY_MEDIA_ID
     */
    @NonNull
    public ListenableFuture<ControllerResult> replacePlaylistItem(@IntRange(from = 0) int index,
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
     * Get the lastly cached current item from
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController2, MediaItem2)}.
     *
     * @return the currently playing item, or null if unknown or not connected
     */
    @Nullable
    public MediaItem2 getCurrentMediaItem() {
        return isConnected() ? getImpl().getCurrentMediaItem() : null;
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * This calls {@link SessionPlayer2#skipToPreviousPlaylistItem()}.
     */
    @NonNull
    public ListenableFuture<ControllerResult> skipToPreviousPlaylistItem() {
        if (isConnected()) {
            return getImpl().skipToPreviousItem();
        }
        return createDisconnectedFuture();
    }

    /**
     * Skips to the next item in the playlist.
     * <p>
     * This calls {@link SessionPlayer2#skipToNextPlaylistItem()}.
     */
    @NonNull
    public ListenableFuture<ControllerResult> skipToNextPlaylistItem() {
        if (isConnected()) {
            return getImpl().skipToNextItem();
        }
        return createDisconnectedFuture();
    }

    /**
     * Skips to the item in the playlist at the index.
     * <p>
     * This calls {@link SessionPlayer2#skipToPlaylistItem(MediaItem2)}.
     *
     * @param index The item in the playlist you want to play
     */
    @NonNull
    public ListenableFuture<ControllerResult> skipToPlaylistItem(@IntRange(from = 0) int index) {
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
     * If it is not connected yet, it returns {@link SessionPlayer2#REPEAT_MODE_NONE}.
     *
     * @return repeat mode
     * @see SessionPlayer2#REPEAT_MODE_NONE
     * @see SessionPlayer2#REPEAT_MODE_ONE
     * @see SessionPlayer2#REPEAT_MODE_ALL
     * @see SessionPlayer2#REPEAT_MODE_GROUP
     */
    public @RepeatMode int getRepeatMode() {
        return isConnected() ? getImpl().getRepeatMode() : REPEAT_MODE_NONE;
    }

    /**
     * Sets the repeat mode.
     *
     * @param repeatMode repeat mode
     * @see SessionPlayer2#REPEAT_MODE_NONE
     * @see SessionPlayer2#REPEAT_MODE_ONE
     * @see SessionPlayer2#REPEAT_MODE_ALL
     * @see SessionPlayer2#REPEAT_MODE_GROUP
     */
    @NonNull
    public ListenableFuture<ControllerResult> setRepeatMode(@RepeatMode int repeatMode) {
        if (isConnected()) {
            return getImpl().setRepeatMode(repeatMode);
        }
        return createDisconnectedFuture();
    }

    /**
     * Gets the cached shuffle mode from the {@link ControllerCallback#onShuffleModeChanged}.
     * If it is not connected yet, it returns {@link SessionPlayer2#SHUFFLE_MODE_NONE}.
     *
     * @return The shuffle mode
     * @see SessionPlayer2#SHUFFLE_MODE_NONE
     * @see SessionPlayer2#SHUFFLE_MODE_ALL
     * @see SessionPlayer2#SHUFFLE_MODE_GROUP
     */
    public @ShuffleMode int getShuffleMode() {
        return isConnected() ? getImpl().getShuffleMode() : SHUFFLE_MODE_NONE;
    }

    /**
     * Sets the shuffle mode.
     *
     * @param shuffleMode The shuffle mode
     * @see SessionPlayer2#SHUFFLE_MODE_NONE
     * @see SessionPlayer2#SHUFFLE_MODE_ALL
     * @see SessionPlayer2#SHUFFLE_MODE_GROUP
     */
    @NonNull
    public ListenableFuture<ControllerResult> setShuffleMode(@ShuffleMode int shuffleMode) {
        if (isConnected()) {
            return getImpl().setShuffleMode(shuffleMode);
        }
        return createDisconnectedFuture();
    }

    /**
     * Sets the time diff forcefully when calculating current position.
     * @param timeDiff {@code null} for reset.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setTimeDiff(Long timeDiff) {
        mTimeDiff = timeDiff;
    }

    private static ListenableFuture<ControllerResult> createDisconnectedFuture() {
        return ControllerResult.createFutureWithResult(ControllerResult.RESULT_CODE_DISCONNECTED);
    }

    @NonNull ControllerCallback getCallback() {
        return isConnected() ? getImpl().getCallback() : null;
    }

    @NonNull Executor getCallbackExecutor() {
        return isConnected() ? getImpl().getCallbackExecutor() : null;
    }

    interface MediaController2Impl extends AutoCloseable {
        @Nullable SessionToken2 getConnectedSessionToken();
        boolean isConnected();
        ListenableFuture<ControllerResult> play();
        ListenableFuture<ControllerResult> pause();
        ListenableFuture<ControllerResult> prepare();
        ListenableFuture<ControllerResult> fastForward();
        ListenableFuture<ControllerResult> rewind();
        ListenableFuture<ControllerResult> seekTo(long pos);
        ListenableFuture<ControllerResult> skipForward();
        ListenableFuture<ControllerResult> skipBackward();
        ListenableFuture<ControllerResult> playFromMediaId(@NonNull String mediaId,
                @Nullable Bundle extras);
        ListenableFuture<ControllerResult> playFromSearch(@NonNull String query,
                @Nullable Bundle extras);
        ListenableFuture<ControllerResult> playFromUri(@NonNull Uri uri, @Nullable Bundle extras);
        ListenableFuture<ControllerResult> prepareFromMediaId(@NonNull String mediaId,
                @Nullable Bundle extras);
        ListenableFuture<ControllerResult> prepareFromSearch(@NonNull String query,
                @Nullable Bundle extras);
        ListenableFuture<ControllerResult> prepareFromUri(@NonNull Uri uri,
                @Nullable Bundle extras);
        ListenableFuture<ControllerResult> setVolumeTo(int value, @VolumeFlags int flags);
        ListenableFuture<ControllerResult> adjustVolume(@VolumeDirection int direction,
                @VolumeFlags int flags);
        @Nullable PendingIntent getSessionActivity();
        int getPlayerState();
        long getDuration();
        long getCurrentPosition();
        float getPlaybackSpeed();
        ListenableFuture<ControllerResult> setPlaybackSpeed(float speed);
        @SessionPlayer2.BuffState int getBufferingState();
        long getBufferedPosition();
        @Nullable PlaybackInfo getPlaybackInfo();
        ListenableFuture<ControllerResult> setRating(@NonNull String mediaId,
                @NonNull Rating2 rating);
        ListenableFuture<ControllerResult> sendCustomCommand(@NonNull SessionCommand2 command,
                @Nullable Bundle args);
        @Nullable List<MediaItem2> getPlaylist();
        ListenableFuture<ControllerResult> setPlaylist(@NonNull List<String> list,
                @Nullable MediaMetadata2 metadata);
        ListenableFuture<ControllerResult> setMediaItem(@NonNull String mediaId);
        ListenableFuture<ControllerResult> updatePlaylistMetadata(
                @Nullable MediaMetadata2 metadata);
        @Nullable MediaMetadata2 getPlaylistMetadata();
        ListenableFuture<ControllerResult> addPlaylistItem(int index, @NonNull String mediaId);
        ListenableFuture<ControllerResult> removePlaylistItem(@NonNull int index);
        ListenableFuture<ControllerResult> replacePlaylistItem(int index,
                @NonNull String mediaId);
        MediaItem2 getCurrentMediaItem();
        ListenableFuture<ControllerResult> skipToPreviousItem();
        ListenableFuture<ControllerResult> skipToNextItem();
        ListenableFuture<ControllerResult> skipToPlaylistItem(@NonNull int index);
        @RepeatMode int getRepeatMode();
        ListenableFuture<ControllerResult> setRepeatMode(@RepeatMode int repeatMode);
        @ShuffleMode int getShuffleMode();
        ListenableFuture<ControllerResult> setShuffleMode(@ShuffleMode int shuffleMode);

        // Internally used methods
        @NonNull MediaController2 getInstance();
        @NonNull Context getContext();
        @NonNull ControllerCallback getCallback();
        @NonNull Executor getCallbackExecutor();
        @Nullable MediaBrowserCompat getBrowserCompat();
    }

    /**
     * Interface for listening to change in activeness of the {@link MediaSession2}.  It's
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
        public void onConnected(@NonNull MediaController2 controller,
                @NonNull SessionCommandGroup2 allowedCommands) { }

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
        public void onDisconnected(@NonNull MediaController2 controller) { }

        /**
         * Called when the session set the custom layout through the
         * {@link MediaSession2#setCustomLayout(ControllerInfo, List)}.
         * <p>
         * Can be called before {@link #onConnected(MediaController2, SessionCommandGroup2)}
         * is called.
         * <p>
         * Default implementation returns {@link ControllerResult#RESULT_CODE_NOT_SUPPORTED}.
         *
         * @param controller the controller for this event
         * @param layout
         */
        public @ControllerResult.ResultCode int onSetCustomLayout(
                @NonNull MediaController2 controller, @NonNull List<CommandButton> layout) {
            return ControllerResult.RESULT_CODE_NOT_SUPPORTED;
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
        public void onPlaybackInfoChanged(@NonNull MediaController2 controller,
                @NonNull PlaybackInfo info) { }

        /**
         * Called when the allowed commands are changed by session.
         *
         * @param controller the controller for this event
         * @param commands newly allowed commands
         */
        public void onAllowedCommandsChanged(@NonNull MediaController2 controller,
                @NonNull SessionCommandGroup2 commands) { }

        /**
         * Called when the session sent a custom command. Returns a {@link ControllerResult} for
         * session to get notification back. If the {@code null} is returned,
         * {@link ControllerResult#RESULT_CODE_UNKNOWN_ERROR} will be returned.
         * <p>
         * Default implementation returns {@link ControllerResult#RESULT_CODE_NOT_SUPPORTED}.
         *
         * @param controller the controller for this event
         * @param command
         * @param args
         * @return result of handling custom command
         */
        @NonNull
        public ControllerResult onCustomCommand(@NonNull MediaController2 controller,
                @NonNull SessionCommand2 command, @Nullable Bundle args) {
            return new ControllerResult(ControllerResult.RESULT_CODE_NOT_SUPPORTED);
        }

        /**
         * Called when the player state is changed.
         *
         * @param controller the controller for this event
         * @param state the new player state
         */
        public void onPlayerStateChanged(@NonNull MediaController2 controller,
                @SessionPlayer2.PlayerState int state) { }

        /**
         * Called when playback speed is changed.
         *
         * @param controller the controller for this event
         * @param speed speed
         */
        public void onPlaybackSpeedChanged(@NonNull MediaController2 controller,
                float speed) { }

        /**
         * Called to report buffering events for a media item.
         * <p>
         * Use {@link #getBufferedPosition()} for current buffering position.
         *
         * @param controller the controller for this event
         * @param item the media item for which buffering is happening.
         * @param state the new buffering state.
         */
        public void onBufferingStateChanged(@NonNull MediaController2 controller,
                @NonNull MediaItem2 item, @SessionPlayer2.BuffState int state) { }

        /**
         * Called to indicate that seeking is completed.
         *
         * @param controller the controller for this event.
         * @param position the previous seeking request.
         */
        public void onSeekCompleted(@NonNull MediaController2 controller, long position) { }

        /**
         * Called when the player's currently playing item is changed
         * <p>
         * When it's called, you should invalidate previous playback information and wait for later
         * callbacks.
         *
         * @param controller the controller for this event
         * @param item new item
         * @see #onBufferingStateChanged(MediaController2, MediaItem2, int)
         */
        public void onCurrentMediaItemChanged(@NonNull MediaController2 controller,
                @Nullable MediaItem2 item) { }

        /**
         * Called when a playlist is changed.
         *
         * @param controller the controller for this event
         * @param list new playlist
         * @param metadata new metadata
         */
        public void onPlaylistChanged(@NonNull MediaController2 controller,
                @Nullable List<MediaItem2> list, @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when a playlist metadata is changed.
         *
         * @param controller the controller for this event
         * @param metadata new metadata
         */
        public void onPlaylistMetadataChanged(@NonNull MediaController2 controller,
                @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when the shuffle mode is changed.
         *
         * @param controller the controller for this event
         * @param shuffleMode repeat mode
         * @see SessionPlayer2#SHUFFLE_MODE_NONE
         * @see SessionPlayer2#SHUFFLE_MODE_ALL
         * @see SessionPlayer2#SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaController2 controller,
                @SessionPlayer2.ShuffleMode int shuffleMode) { }

        /**
         * Called when the repeat mode is changed.
         *
         * @param controller the controller for this event
         * @param repeatMode repeat mode
         * @see SessionPlayer2#REPEAT_MODE_NONE
         * @see SessionPlayer2#REPEAT_MODE_ONE
         * @see SessionPlayer2#REPEAT_MODE_ALL
         * @see SessionPlayer2#REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaController2 controller,
                @SessionPlayer2.RepeatMode int repeatMode) { }

        /**
         * Called when the playback is completed.
         *
         * @param controller the controller for this event
         */
        public void onPlaybackCompleted(@NonNull MediaController2 controller) { }
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

    /**
     * Result class to be used with {@link ListenableFuture} for asynchronous calls.
     */
    @VersionedParcelize
    public static class ControllerResult implements RemoteResult2, VersionedParcelable {
        /**
         * Result code representing that the command is successfully completed.
         * <p>
         * Interoperability: When connected to
         * {@link android.support.v4.media.session.MediaSessionCompat}, this can be also used to
         * tell that the command was successfully sent, but the result is unknown.
         */
        // Redefined to override the Javadoc
        public static final int RESULT_CODE_SUCCESS = 0;

        /**
         * @hide
         */
        @IntDef(flag = false, /*prefix = "RESULT_CODE",*/ value = {
                RESULT_CODE_SUCCESS,
                RESULT_CODE_UNKNOWN_ERROR,
                RESULT_CODE_INVALID_STATE,
                RESULT_CODE_BAD_VALUE,
                RESULT_CODE_PERMISSION_DENIED,
                RESULT_CODE_IO_ERROR,
                RESULT_CODE_SKIPPED,
                RESULT_CODE_DISCONNECTED,
                RESULT_CODE_NOT_SUPPORTED,
                RESULT_CODE_AUTHENTICATION_EXPIRED,
                RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED,
                RESULT_CODE_CONCURRENT_STREAM_LIMIT,
                RESULT_CODE_PARENTAL_CONTROL_RESTRICTED,
                RESULT_CODE_NOT_AVAILABLE_IN_REGION,
                RESULT_CODE_SKIP_LIMIT_REACHED,
                RESULT_CODE_SETUP_REQUIRED})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface ResultCode {}

        @ParcelField(1)
        int mResultCode;
        @ParcelField(2)
        long mCompletionTime;
        @ParcelField(3)
        Bundle mCustomCommandResult;
        @ParcelField(4)
        MediaItem2 mItem;

        /**
         * Constructor to be used by
         * {@link ControllerCallback#onCustomCommand(MediaController2, SessionCommand2, Bundle)}.
         *
         * @param resultCode result code
         * @param customCommandResult custom command result
         */
        public ControllerResult(@ResultCode int resultCode, @Nullable Bundle customCommandResult) {
            this(resultCode, customCommandResult, null);
        }

        // For versioned parcelable
        ControllerResult() {
            // no-op
        }

        ControllerResult(@ResultCode int resultCode) {
            this(resultCode, null, null);
        }

        ControllerResult(@ResultCode int resultCode, @Nullable Bundle customCommandResult,
                @Nullable MediaItem2 item) {
            this(resultCode, customCommandResult, item, SystemClock.elapsedRealtime());
        }

        ControllerResult(@ResultCode int resultCode, @Nullable Bundle customCommandResult,
                @Nullable MediaItem2 item, long completionTime) {
            mResultCode = resultCode;
            mCustomCommandResult = customCommandResult;
            mItem = item;
            mCompletionTime = completionTime;
        }

        static ListenableFuture<ControllerResult> createFutureWithResult(
                @ResultCode int resultCode) {
            ResolvableFuture<ControllerResult> result = ResolvableFuture.create();
            result.set(new ControllerResult(resultCode));
            return result;
        }

        static ControllerResult from(@Nullable SessionResult result) {
            if (result == null) {
                return null;
            }
            return new ControllerResult(result.getResultCode(), result.getCustomCommandResult(),
                    result.getMediaItem(), result.getCompletionTime());
        }

        /**
         * Gets the result code.
         *
         * @return result code
         * @see #RESULT_CODE_SUCCESS
         * @see #RESULT_CODE_UNKNOWN_ERROR
         * @see #RESULT_CODE_INVALID_STATE
         * @see #RESULT_CODE_BAD_VALUE
         * @see #RESULT_CODE_PERMISSION_DENIED
         * @see #RESULT_CODE_IO_ERROR
         * @see #RESULT_CODE_SKIPPED
         * @see #RESULT_CODE_DISCONNECTED
         * @see #RESULT_CODE_NOT_SUPPORTED
         * @see #RESULT_CODE_AUTHENTICATION_EXPIRED
         * @see #RESULT_CODE_PREMIUM_ACCOUNT_REQUIRED
         * @see #RESULT_CODE_CONCURRENT_STREAM_LIMIT
         * @see #RESULT_CODE_PARENTAL_CONTROL_RESTRICTED
         * @see #RESULT_CODE_NOT_AVAILABLE_IN_REGION
         * @see #RESULT_CODE_SKIP_LIMIT_REACHED
         * @see #RESULT_CODE_SETUP_REQUIRED
         */
        @Override
        public @ResultCode int getResultCode() {
            return mResultCode;
        }

        /**
         * Gets the completion time of the command. Being more specific, it's the same as
         * {@link android.os.SystemClock#elapsedRealtime()} when the command is completed.
         *
         * @return completion time of the command
         */
        @Override
        public long getCompletionTime() {
            return mCompletionTime;
        }

        /**
         * Gets the result of {@link #sendCustomCommand(SessionCommand2, Bundle)}. This is only
         * valid when it's returned by the {@link #sendCustomCommand(SessionCommand2, Bundle)} and
         * will be {@code null} otherwise.
         *
         * @see #sendCustomCommand(SessionCommand2, Bundle)
         * @return result of send custom command
         */
        @Nullable
        public Bundle getCustomCommandResult() {
            return mCustomCommandResult;
        }

        /**
         * Gets the {@link MediaItem2} for which the command was executed. In other words, this is
         * the current media item when the command was completed.
         * <p>
         * Can be {@code null} for many reasons. For examples,
         * <ul>
         * <li>Error happened.
         * <li>Current media item was {@code null} at that time.
         * <li>Command is irrelevant with the media item (e.g. custom command).
         * </ul>
         *
         * @return media item when the command is completed. Can be {@code null} for an error, the
         *         current media item was {@code null}, or any other reason.
         */
        @Override
        @Nullable
        public MediaItem2 getMediaItem() {
            return mItem;
        }
    }
}
