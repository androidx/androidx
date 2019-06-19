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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.media2.session.SessionResult.RESULT_ERROR_NOT_SUPPORTED;
import static androidx.media2.session.SessionResult.RESULT_SUCCESS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.common.CallbackMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.BuffState;
import androidx.media2.common.SessionPlayer.PlayerResult;
import androidx.media2.common.SessionPlayer.PlayerState;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.UriMediaItem;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController.PlaybackInfo;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.SessionResult.ResultCode;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Allows a media app to expose its transport controls and playback information in a process to
 * other processes including the Android framework and other apps. Common use cases are as follows.
 * <ul>
 *     <li>Bluetooth/wired headset key events support</li>
 *     <li>Android Auto/Wearable support</li>
 *     <li>Separating UI process and playback process</li>
 * </ul>
 * <p>
 * A MediaSession should be created when an app wants to publish media playback information or
 * handle media keys. In general an app only needs one session for all playback, though multiple
 * sessions can be created to provide finer grain controls of media.
 * <p>
 * If you want to support background playback, {@link MediaSessionService} is preferred
 * instead. With it, your playback can be revived even after playback is finished. See
 * {@link MediaSessionService} for details.
 * <p>
 * Topic covered here:
 * <ol>
 * <li><a href="#SessionLifecycle">Session Lifecycle</a>
 * <li><a href="#AudioFocusAndNoisyIntent">Audio focus and noisy intent</a>
 * <li><a href="#Thread">Thread</a>
 * <li><a href="#KeyEvents">Media key events mapping</a>
 * </ol>
 * <a name="SessionLifecycle"></a>
 * <h3>Session Lifecycle</h3>
 * <p>
 * A session can be obtained by {@link Builder}. The owner of the session may pass its session token
 * to other processes to allow them to create a {@link MediaController} to interact with the
 * session.
 * <p>
 * When a session receive transport control commands, the session sends the commands directly to
 * the the underlying media player set by {@link Builder} or {@link #updatePlayer}.
 * <p>
 * When an app is finished performing playback it must call {@link #close()} to clean up the session
 * and notify any controllers. The app is responsible for closing the underlying player after
 * closing the session.
 * is closed.
 * <p>
 * <a name="Thread"></a>
 * <h3>Thread</h3>
 * <p>
 * {@link MediaSession} objects are thread safe, but should be used on the thread on the looper.
 * <a name="KeyEvents"></a>
 * <h3>Media key events mapping</h3>
 * <p>
 * Here's the table of per key event.
 * <table>
 * <tr><th>Key code</th><th>{@link MediaSession} API</th></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PLAY}</td>
 *     <td>{@link SessionPlayer#play()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PAUSE}</td>
 *     <td>{@link SessionPlayer#pause()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_NEXT}</td>
 *     <td>{@link SessionPlayer#skipToNextPlaylistItem()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PREVIOUS}</td>
 *     <td>{@link SessionPlayer#skipToPreviousPlaylistItem()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_STOP}</td>
 *     <td>{@link SessionPlayer#pause()} and then
 *         {@link SessionPlayer#seekTo(long)} with 0</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_FAST_FORWARD}</td>
 *     <td>{@link SessionCallback#onFastForward}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_REWIND}</td>
 *     <td>{@link SessionCallback#onRewind}</td></tr>
 * <tr><td><ul><li>{@link KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE}</li>
 *             <li>{@link KeyEvent#KEYCODE_HEADSETHOOK}</li></ul></td>
 *     <td><ul><li>For a single tap
 *             <ul><li>{@link SessionPlayer#pause()} if
 *             {@link SessionPlayer#PLAYER_STATE_PLAYING}</li>
 *             <li>{@link SessionPlayer#play()} otherwise</li></ul>
 *             <li>For a double tap, {@link SessionPlayer#skipToNextPlaylistItem()}</li></ul></td>
 *     </tr>
 * </table>
 * @see MediaSessionService
 */
public class MediaSession implements AutoCloseable {
    static final String TAG = "MediaSession";

    // It's better to have private static lock instead of using MediaSession.class because the
    // private lock object isn't exposed.
    private static final Object STATIC_LOCK = new Object();
    // Note: This checks the uniqueness of a session ID only in single process.
    // When the framework becomes able to check the uniqueness, this logic should be removed.
    @GuardedBy("STATIC_LOCK")
    private static final HashMap<String, MediaSession> SESSION_ID_TO_SESSION_MAP = new HashMap<>();

    private final MediaSessionImpl mImpl;

    MediaSession(Context context, String id, SessionPlayer player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback,
            Bundle tokenExtras) {
        synchronized (STATIC_LOCK) {
            if (SESSION_ID_TO_SESSION_MAP.containsKey(id)) {
                throw new IllegalStateException("Session ID must be unique. ID=" + id);
            }
            SESSION_ID_TO_SESSION_MAP.put(id, this);
        }
        mImpl = createImpl(context, id, player, sessionActivity, callbackExecutor, callback,
                tokenExtras);
    }

    MediaSessionImpl createImpl(Context context, String id, SessionPlayer player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback,
            Bundle tokenExtras) {
        return new MediaSessionImplBase(this, context, id, player, sessionActivity,
                callbackExecutor, callback, tokenExtras);
    }

    /**
     * Should be only used by subclass.
     */
    MediaSessionImpl getImpl() {
        return mImpl;
    }

    static MediaSession getSession(Uri sessionUri) {
        synchronized (STATIC_LOCK) {
            for (MediaSession session : SESSION_ID_TO_SESSION_MAP.values()) {
                if (ObjectsCompat.equals(session.getUri(), sessionUri)) {
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * Updates the underlying {@link SessionPlayer} for this session to dispatch incoming event to.
     *
     * @param player a player that handles actual media playback in your app
     */
    public void updatePlayer(@NonNull SessionPlayer player) {
        if (player == null) {
            throw new NullPointerException("player shouldn't be null");
        }
        mImpl.updatePlayer(player);
    }

    @Override
    public void close() {
        try {
            synchronized (STATIC_LOCK) {
                SESSION_ID_TO_SESSION_MAP.remove(mImpl.getId());
            }
            mImpl.close();
        } catch (Exception e) {
            // Should not be here.
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean isClosed() {
        return mImpl.isClosed();
    }

    /**
     * Gets the underlying {@link SessionPlayer}.
     * <p>
     * When the session is closed, it returns the lastly set player.
     *
     * @return player.
     */
    @NonNull
    public SessionPlayer getPlayer() {
        return mImpl.getPlayer();
    }

    /**
     * Gets the session ID
     *
     * @return
     */
    @NonNull
    public String getId() {
        return mImpl.getId();
    }

    /**
     * Returns the {@link SessionToken} for creating {@link MediaController}.
     */
    @NonNull
    public SessionToken getToken() {
        return mImpl.getToken();
    }

    @NonNull
    Context getContext() {
        return mImpl.getContext();
    }

    @NonNull
    Executor getCallbackExecutor() {
        return mImpl.getCallbackExecutor();
    }

    @NonNull
    SessionCallback getCallback() {
        return mImpl.getCallback();
    }

    /**
     * Returns the list of connected controller.
     *
     * @return list of {@link ControllerInfo}
     */
    @NonNull
    public List<ControllerInfo> getConnectedControllers() {
        return mImpl.getConnectedControllers();
    }

    /**
     * Sets ordered list of {@link CommandButton} for controllers to build UI with it.
     * <p>
     * It's up to controller's decision how to represent the layout in its own UI.
     * Here are some examples.
     * <p>
     * Note: <code>layout[i]</code> means a CommandButton at index i in the given list
     * <table>
     * <tr><th>Controller UX layout</th><th>Layout example</th></tr>
     * <tr><td>Row with 3 icons</td>
     *     <td><code>layout[1]</code> <code>layout[0]</code> <code>layout[2]</code></td></tr>
     * <tr><td>Row with 5 icons</td>
     *     <td><code>layout[3]</code> <code>layout[1]</code> <code>layout[0]</code>
     *         <code>layout[2]</code> <code>layout[4]</code></td></tr>
     * <tr><td rowspan=2>Row with 5 icons and an overflow icon, and another expandable row with 5
     *         extra icons</td>
     *     <td><code>layout[3]</code> <code>layout[1]</code> <code>layout[0]</code>
     *         <code>layout[2]</code> <code>layout[4]</code></td></tr>
     * <tr><td><code>layout[3]</code> <code>layout[1]</code> <code>layout[0]</code>
     *         <code>layout[2]</code> <code>layout[4]</code></td></tr>
     * </table>
     * <p>
     * This API can be called in the
     * {@link SessionCallback#onConnect(MediaSession, ControllerInfo)}.
     *
     * @param controller controller to specify layout.
     * @param layout ordered list of layout.
     */
    @NonNull
    public ListenableFuture<SessionResult> setCustomLayout(
            @NonNull ControllerInfo controller, @NonNull List<CommandButton> layout) {
        if (controller == null) {
            throw new NullPointerException("controller shouldn't be null");
        }
        if (layout == null) {
            throw new NullPointerException("layout shouldn't be null");
        }
        return mImpl.setCustomLayout(controller, layout);
    }

    /**
     * Sets the new allowed command group for the controller.
     * <p>
     * This is synchronous call. Changes in the allowed commands take effect immediately regardless
     * of the controller notified about the change through
     * {@link MediaController.ControllerCallback
     * #onAllowedCommandsChanged(MediaController, SessionCommandGroup)}
     *
     * @param controller controller to change allowed commands
     * @param commands new allowed commands
     */
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull SessionCommandGroup commands) {
        if (controller == null) {
            throw new NullPointerException("controller shouldn't be null");
        }
        if (commands == null) {
            throw new NullPointerException("commands shouldn't be null");
        }
        mImpl.setAllowedCommands(controller, commands);
    }

    /**
     * Broadcasts custom command to all connected controllers.
     * <p>
     * This is synchronous call and doesn't wait for result from the controller. Use
     * {@link #sendCustomCommand(ControllerInfo, SessionCommand, Bundle)} for getting the result.
     * <p>
     * A command is not accepted if it is not a custom command.
     *
     * @param command a command
     * @param args optional argument
     * @see #sendCustomCommand(ControllerInfo, SessionCommand, Bundle)
     */
    public void broadcastCustomCommand(@NonNull SessionCommand command, @Nullable Bundle args) {
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
        }
        if (command.getCommandCode() != SessionCommand.COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command should be a custom command");
        }
        mImpl.broadcastCustomCommand(command, args);
    }

    /**
     * Send custom command to a specific controller.
     * <p>
     * A command is not accepted if it is not a custom command.
     *
     * @param command a command
     * @param args optional argument
     * @see #broadcastCustomCommand(SessionCommand, Bundle)
     */
    @NonNull
    public ListenableFuture<SessionResult> sendCustomCommand(
            @NonNull ControllerInfo controller, @NonNull SessionCommand command,
            @Nullable Bundle args) {
        if (controller == null) {
            throw new NullPointerException("controller shouldn't be null");
        }
        if (command == null) {
            throw new NullPointerException("command shouldn't be null");
        }
        if (command.getCommandCode() != SessionCommand.COMMAND_CODE_CUSTOM) {
            throw new IllegalArgumentException("command should be a custom command");
        }
        return mImpl.sendCustomCommand(controller, command, args);
    }

    /**
     * @hide
     * @return Bundle
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public MediaSessionCompat getSessionCompat() {
        return mImpl.getSessionCompat();
    }

    /**
     * Handles the controller's connection request from {@link MediaSessionService}.
     *
     * @param controller controller aidl
     * @param packageName controller package name
     * @param pid controller pid
     * @param uid controller uid
     * @param connectionHints controller connection hints
     */
    void handleControllerConnectionFromService(IMediaController controller, String packageName,
            int pid, int uid, @Nullable Bundle connectionHints) {
        mImpl.connectFromService(controller, packageName, pid, uid, connectionHints);
    }

    IBinder getLegacyBrowerServiceBinder() {
        return mImpl.getLegacyBrowserServiceBinder();
    }

    @NonNull
    private Uri getUri() {
        return mImpl.getUri();
    }

    /**
     * Callback to be called for all incoming commands from {@link MediaController}s.
     * <p>
     * If it's not set, the session will accept all controllers and all incoming commands by
     * default.
     */
    public abstract static class SessionCallback {
        ForegroundServiceEventCallback mForegroundServiceEventCallback;

        /**
         * Called when a controller is created for this session. Return allowed commands for
         * controller. By default it allows all connection requests and commands.
         * <p>
         * You can reject the connection by return {@code null}. In that case, the controller
         * receives {@link MediaController.ControllerCallback#onDisconnected(MediaController)} and
         * cannot be used.
         * <p>
         * The controller hasn't connected yet in this method, so calls to the controller
         * (e.g. {@link #sendCustomCommand}, {@link #setCustomLayout}) would be ignored. Override
         * {@link #onPostConnect} for the custom initialization for the controller instead.
         *
         * @param session the session for this event
         * @param controller controller information.
         * @return allowed commands. Can be {@code null} to reject connection.
         * @see #onPostConnect(MediaSession, ControllerInfo)
         */
        @Nullable
        public SessionCommandGroup onConnect(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            SessionCommandGroup commands = new SessionCommandGroup.Builder()
                    .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_1)
                    .build();
            return commands;
        }

        /**
         * Called immediately after a controller is connected. This is a convenient method to add
         * custom initialization between the session and a controller.
         * <p>
         * Note that calls to the controller (e.g. {@link #sendCustomCommand},
         * {@link #setCustomLayout}) work here but don't work in {@link #onConnect} because the
         * controller hasn't connected yet in {@link #onConnect}.
         *
         * @param session the session for this event
         * @param controller controller information.
         */
        public void onPostConnect(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
        }

        /**
         * Called when a controller is disconnected
         *
         * @param session the session for this event
         * @param controller controller information
         */
        public void onDisconnected(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {}

        /**
         * Called when a controller sent a command which will be sent directly to one of the
         * following:
         * <ul>
         *  <li>{@link SessionPlayer}</li>
         *  <li>{@link android.media.AudioManager}</li>
         * </ul>
         * <p>
         * Return {@link SessionResult#RESULT_SUCCESS} to proceed the command. If something
         * else is returned, command wouldn't be sent and the controller would receive the code with
         * it.
         *
         * @param session the session for this event
         * @param controller controller information.
         * @param command a command. This method will be called for every single command.
         * @return {@code RESULT_SUCCESS} if you want to proceed with incoming command.
         *         Another code for ignore.
         * @see SessionCommand#COMMAND_CODE_PLAYER_PLAY
         * @see SessionCommand#COMMAND_CODE_PLAYER_PAUSE
         * @see SessionCommand#COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_PREPARE
         * @see SessionCommand#COMMAND_CODE_PLAYER_SEEK_TO
         * @see SessionCommand#COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE
         * @see SessionCommand#COMMAND_CODE_PLAYER_SET_REPEAT_MODE
         * @see SessionCommand#COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM
         * @see SessionCommand#COMMAND_CODE_PLAYER_GET_PLAYLIST
         * @see SessionCommand#COMMAND_CODE_PLAYER_SET_PLAYLIST
         * @see SessionCommand#COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA
         * @see SessionCommand#COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA
         * @see SessionCommand#COMMAND_CODE_VOLUME_SET_VOLUME
         * @see SessionCommand#COMMAND_CODE_VOLUME_ADJUST_VOLUME
         */
        @ResultCode
        public int onCommandRequest(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand command) {
            return RESULT_SUCCESS;
        }

        /**
         * Called when a controller has sent a command with a {@link MediaItem} to add a new media
         * item to this session. Being specific, this will be called for following APIs.
         * <ol>
         * <li>{@link MediaController#addPlaylistItem(int, String)}
         * <li>{@link MediaController#replacePlaylistItem(int, String)}
         * <li>{@link MediaController#setPlaylist(List, MediaMetadata)}
         * <li>{@link MediaController#setMediaItem(String)}
         * </ol>
         * Override this to translate incoming {@code mediaId} to a {@link MediaItem} to be
         * understood by your player. For example, a player may only understand
         * {@link androidx.media2.common.FileMediaItem}, {@link UriMediaItem},
         * and {@link CallbackMediaItem}. Check the documentation of the player that you're using.
         * <p>
         * If the given media ID is valid, you should return the media item with the given media ID.
         * If the ID doesn't match, an {@link RuntimeException} will be thrown.
         * You may return {@code null} if the given item is invalid. Here's the behavior when it
         * happens.
         * <table border="0" cellspacing="0" cellpadding="0">
         * <tr><th>Controller command</th> <th>Behavior when {@code null} is returned</th></tr>
         * <tr><td>addPlaylistItem</td> <td>Ignore</td></tr>
         * <tr><td>replacePlaylistItem</td> <td>Ignore</td></tr>
         * <tr><td>setPlaylist</td>
         *     <td>Ignore {@code null} items, and build a list with non-{@code null} items. Call
         *         {@link SessionPlayer#setPlaylist(List, MediaMetadata)} with the list</td></tr>
         * <tr><td>setMediaItem</td> <td>Ignore</td></tr>
         * </table>
         * <p>
         * This will be called on the same thread where {@link #onCommandRequest} and commands with
         * the media controller will be executed.
         * <p>
         * Default implementation returns the {@code null}.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId non-empty media id for creating item with
         * @return translated media item for player with the mediaId. Can be {@code null} to ignore.
         * @see MediaMetadata#METADATA_KEY_MEDIA_ID
         */
        @Nullable
        public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String mediaId) {
            return null;
        }

        /**
         * Called when a controller set rating of a media item through
         * {@link MediaController#setRating(String, Rating)}.
         * <p>
         * To allow setting user rating for a {@link MediaItem}, the media item's metadata
         * should have {@link Rating} with the key {@link MediaMetadata#METADATA_KEY_USER_RATING},
         * in order to provide possible rating style for controller. Controller will follow the
         * rating style.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId non-empty media id
         * @param rating new rating from the controller
         * @see SessionCommand#COMMAND_CODE_SESSION_SET_RATING
         */
        @ResultCode
        public int onSetRating(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @NonNull Rating rating) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller sent a custom command through
         * {@link MediaController#sendCustomCommand(SessionCommand, Bundle)}.
         * <p>
         * Interoperability: This would be also called by {@link
         * android.support.v4.media.MediaBrowserCompat
         * #sendCustomAction(String, Bundle, CustomActionCallback)}. If so, extra from
         * sendCustomAction will be considered as args and customCommand would have null extra.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param customCommand custom command.
         * @param args optional arguments
         * @return result of handling custom command. A runtime exception will be thrown if
         *         {@code null} is returned.
         * @see SessionCommand#COMMAND_CODE_CUSTOM
         */
        @NonNull
        public SessionResult onCustomCommand(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand customCommand,
                @Nullable Bundle args) {
            return new SessionResult(RESULT_ERROR_NOT_SUPPORTED, null);
        }

        /**
         * Called when a controller requested to play a specific mediaId through
         * {@link MediaController#playFromMediaId(String, Bundle)}.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId non-empty media id
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPlayFromMediaId(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller requested to begin playback from a search query through
         * {@link MediaController#playFromSearch(String, Bundle)}
         *
         * @param session the session for this event
         * @param controller controller information
         * @param query non-empty search query.
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PLAY_FROM_SEARCH
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPlayFromSearch(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String query,
                @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller requested to play a specific media item represented by a URI
         * through {@link MediaController#playFromUri(Uri, Bundle)}
         *
         * @param session the session for this event
         * @param controller controller information
         * @param uri uri
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PLAY_FROM_URI
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPlayFromUri(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull Uri uri,
                @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller requested to prepare for playing a specific mediaId through
         * {@link MediaController#prepareFromMediaId(String, Bundle)}.
         * <p>
         * During the prepare, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer#PLAYER_STATE_PAUSED} after the prepare is done.
         * <p>
         * The playback of the prepared content should start in the later calls of
         * {@link SessionPlayer#play()}.
         * <p>
         * Override {@link #onPlayFromMediaId} to handle requests for starting
         * playback without preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId non-empty media id
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPrepareFromMediaId(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller requested to prepare playback from a search query through
         * {@link MediaController#prepareFromSearch(String, Bundle)}.
         * <p>
         * During the prepare, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer#PLAYER_STATE_PAUSED} after the prepare is done.
         * <p>
         * The playback of the prepared content should start in the later calls of
         * {@link SessionPlayer#play()}.
         * <p>
         * Override {@link #onPlayFromSearch} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param query non-empty search query
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPrepareFromSearch(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull String query,
                @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller requested to prepare a specific media item represented by a URI
         * through {@link MediaController#prepareFromUri(Uri, Bundle)}.
         * <p>
         * During the prepare, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer#PLAYER_STATE_PAUSED} after the prepare is done.
         * <p>
         * The playback of the prepared content should start in the later calls of
         * {@link SessionPlayer#play()}.
         * <p>
         * Override {@link #onPlayFromUri} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param uri uri
         * @param extras optional extra bundle
         * @see SessionCommand#COMMAND_CODE_SESSION_PREPARE_FROM_URI
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @ResultCode
        public int onPrepareFromUri(@NonNull MediaSession session,
                @NonNull ControllerInfo controller, @NonNull Uri uri, @Nullable Bundle extras) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller called {@link MediaController#fastForward()}.
         * <p>
         * It's recommended to increase the playback speed when this method is called.
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand#COMMAND_CODE_SESSION_FAST_FORWARD
         */
        @ResultCode
        public int onFastForward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller called {@link MediaController#rewind()}.
         * <p>
         * It's recommended to decrease the playback speed when this method is called.
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand#COMMAND_CODE_SESSION_REWIND
         */
        @ResultCode
        public int onRewind(@NonNull MediaSession session, @NonNull ControllerInfo controller) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller called {@link MediaController#skipForward()}.
         * <p>
         * It's recommended to seek forward within the current media item when this method
         * is called.
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand#COMMAND_CODE_SESSION_SKIP_FORWARD
         */
        @ResultCode
        public int onSkipForward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when a controller called {@link MediaController#skipBackward()}.
         * <p>
         * It's recommended to seek backward within the current media item when this method
         * is called.
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand#COMMAND_CODE_SESSION_SKIP_BACKWARD
         */
        @ResultCode
        public int onSkipBackward(@NonNull MediaSession session,
                @NonNull ControllerInfo controller) {
            return RESULT_ERROR_NOT_SUPPORTED;
        }

        /**
         * Called when the player state is changed. Used internally for setting the
         * {@link MediaSessionService} as foreground/background.
         */
        final void onPlayerStateChanged(MediaSession session, @PlayerState int state) {
            if (mForegroundServiceEventCallback != null) {
                mForegroundServiceEventCallback.onPlayerStateChanged(session, state);
            }
        }

        final void onSessionClosed(MediaSession session) {
            if (mForegroundServiceEventCallback != null) {
                mForegroundServiceEventCallback.onSessionClosed(session);
            }
        }

        void setForegroundServiceEventCallback(ForegroundServiceEventCallback callback) {
            mForegroundServiceEventCallback = callback;
        }

        abstract static class ForegroundServiceEventCallback {
            public void onPlayerStateChanged(MediaSession session, @PlayerState int state) {}
            public void onSessionClosed(MediaSession session) {}
        }
    }

    /**
     * Builder for {@link MediaSession}.
     * <p>
     * Any incoming event from the {@link MediaController} will be handled on the callback executor.
     * If it's not set, {@link ContextCompat#getMainExecutor(Context)} will be used by default.
     */
    public static final class Builder extends BuilderBase<MediaSession, Builder, SessionCallback> {
        public Builder(@NonNull Context context, @NonNull SessionPlayer player) {
            super(context, player);
        }

        @Override
        @NonNull
        public Builder setSessionActivity(@Nullable PendingIntent pi) {
            return super.setSessionActivity(pi);
        }

        @Override
        @NonNull
        public Builder setId(@NonNull String id) {
            return super.setId(id);
        }

        @Override
        @NonNull
        public Builder setSessionCallback(@NonNull Executor executor,
                @NonNull SessionCallback callback) {
            return super.setSessionCallback(executor, callback);
        }

        @Override
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            return super.setExtras(extras);
        }

        @Override
        @NonNull
        public MediaSession build() {
            if (mCallbackExecutor == null) {
                mCallbackExecutor = ContextCompat.getMainExecutor(mContext);
            }
            if (mCallback == null) {
                mCallback = new SessionCallback() {};
            }
            return new MediaSession(mContext, mId, mPlayer, mSessionActivity,
                    mCallbackExecutor, mCallback, mExtras);
        }
    }

    /**
     * Information of a controller.
     */
    public static final class ControllerInfo {
        private final RemoteUserInfo mRemoteUserInfo;
        private final boolean mIsTrusted;
        private final ControllerCb mControllerCb;
        private final Bundle mConnectionHints;

        /**
         * @param remoteUserInfo remote user info
         * @param trusted {@code true} if trusted, {@code false} otherwise
         * @param cb ControllerCb. Can be {@code null} only when a MediaBrowserCompat connects to
         *           MediaSessionService and ControllerInfo is needed for
         *           SessionCallback#onConnected().
         * @param connectionHints a session-specific argument sent from the controller for the
         *                        connection. The contents of this bundle may affect the
         *                        connection result.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        ControllerInfo(@NonNull RemoteUserInfo remoteUserInfo, boolean trusted,
                @Nullable ControllerCb cb, @Nullable Bundle connectionHints) {
            mRemoteUserInfo = remoteUserInfo;
            mIsTrusted = trusted;
            mControllerCb = cb;
            mConnectionHints = connectionHints;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        @NonNull
        public RemoteUserInfo getRemoteUserInfo() {
            return mRemoteUserInfo;
        }

        /**
         * @return package name of the controller. Can be
         *         {@link androidx.media.MediaSessionManager.RemoteUserInfo#LEGACY_CONTROLLER} if
         *         the package name cannot be obtained.
         */
        @NonNull
        public String getPackageName() {
            return mRemoteUserInfo.getPackageName();
        }

        /**
         * @return uid of the controller. Can be a negative value if the uid cannot be obtained.
         */
        public int getUid() {
            return mRemoteUserInfo.getUid();
        }

        /**
         * @return connection hints sent from controller, or {@link Bundle#EMPTY} if none.
         */
        @NonNull
        public Bundle getConnectionHints() {
            return mConnectionHints == null ? Bundle.EMPTY : new Bundle(mConnectionHints);
        }

        /**
         * Return if the controller has granted {@code android.permission.MEDIA_CONTENT_CONTROL} or
         * has a enabled notification listener so can be trusted to accept connection and incoming
         * command request.
         *
         * @return {@code true} if the controller is trusted.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public boolean isTrusted() {
            return mIsTrusted;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mControllerCb, mRemoteUserInfo);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerInfo)) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            ControllerInfo other = (ControllerInfo) obj;
            if (mControllerCb != null || other.mControllerCb != null) {
                return ObjectsCompat.equals(mControllerCb, other.mControllerCb);
            }
            return mRemoteUserInfo.equals(other.mRemoteUserInfo);
        }

        @Override
        public String toString() {
            return "ControllerInfo {pkg=" + mRemoteUserInfo.getPackageName() + ", uid="
                    + mRemoteUserInfo.getUid() + "})";
        }

        @Nullable ControllerCb getControllerCb() {
            return mControllerCb;
        }
    }

    /**
     * Button for a {@link SessionCommand} that will be shown by the controller.
     * <p>
     * It's up to the controller's decision to respect or ignore this customization request.
     */
    @VersionedParcelize
    public static final class CommandButton implements VersionedParcelable {
        @ParcelField(1)
        SessionCommand mCommand;
        @ParcelField(2)
        int mIconResId;
        @ParcelField(3)
        CharSequence mDisplayName;
        @ParcelField(4)
        Bundle mExtras;
        @ParcelField(5)
        boolean mEnabled;

        /**
         * Used for VersionedParcelable
         */
        CommandButton() {
        }

        CommandButton(@Nullable SessionCommand command, int iconResId,
                @Nullable CharSequence displayName, Bundle extras, boolean enabled) {
            mCommand = command;
            mIconResId = iconResId;
            mDisplayName = displayName;
            mExtras = extras;
            mEnabled = enabled;
        }

        /**
         * Get command associated with this button. Can be {@code null} if the button isn't enabled
         * and only providing placeholder.
         *
         * @return command or {@code null}
         */
        @Nullable
        public SessionCommand getCommand() {
            return mCommand;
        }

        /**
         * Resource id of the button in this package. Can be {@code 0} if the command is predefined
         * and custom icon isn't needed.
         *
         * @return resource id of the icon. Can be {@code 0}.
         */
        public int getIconResId() {
            return mIconResId;
        }

        /**
         * Display name of the button. Can be {@code null} or empty if the command is predefined
         * and custom name isn't needed.
         *
         * @return custom display name. Can be {@code null} or empty.
         */
        @Nullable
        public CharSequence getDisplayName() {
            return mDisplayName;
        }

        /**
         * Extra information of the button. It's private information between session and controller.
         *
         * @return
         */
        @Nullable
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Return whether it's enabled.
         *
         * @return {@code true} if enabled. {@code false} otherwise.
         */
        public boolean isEnabled() {
            return mEnabled;
        }

        /**
         * Builder for {@link CommandButton}.
         */
        public static final class Builder {
            private SessionCommand mCommand;
            private int mIconResId;
            private CharSequence mDisplayName;
            private Bundle mExtras;
            private boolean mEnabled;

            /**
             * Sets the {@link SessionCommand} that would be sent to the session when the button
             * is clicked.
             *
             * @param command session command
             */
            @NonNull
            public Builder setCommand(@Nullable SessionCommand command) {
                mCommand = command;
                return this;
            }

            /**
             * Sets the bitmap-type (e.g. PNG) icon resource id of the button.
             * <p>
             * None bitmap type (e.g. VectorDrawabale) may cause unexpected behavior when it's sent
             * to {@link MediaController} app, so please avoid using it especially for the older
             * platform (API < 21).
             *
             * @param resId resource id of the button
             */
            @NonNull
            public Builder setIconResId(int resId) {
                mIconResId = resId;
                return this;
            }

            /**
             * Sets the display name of the button.
             *
             * @param displayName display name of the button
             */
            @NonNull
            public Builder setDisplayName(@Nullable CharSequence displayName) {
                mDisplayName = displayName;
                return this;
            }

            /**
             * Sets whether the button is enabled. Can be {@code false} to indicate that the button
             * should be shown but isn't clickable.
             *
             * @param enabled {@code true} if the button is enabled and ready.
             *          {@code false} otherwise.
             */
            @NonNull
            public Builder setEnabled(boolean enabled) {
                mEnabled = enabled;
                return this;
            }

            /**
             * Sets the extras of the button.
             *
             * @param extras extras information of the button
             */
            @NonNull
            public Builder setExtras(@Nullable Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Builds the {@link CommandButton}.
             *
             * @return a new {@link CommandButton}
             */
            @NonNull
            public CommandButton build() {
                return new CommandButton(mCommand, mIconResId, mDisplayName, mExtras, mEnabled);
            }
        }
    }

    // TODO: Drop 'Cb' from the name.
    abstract static class ControllerCb {
        abstract void onPlayerResult(int seq, PlayerResult result) throws RemoteException;
        abstract void onSessionResult(int seq, SessionResult result) throws RemoteException;
        abstract void onLibraryResult(int seq, LibraryResult result) throws RemoteException;

        // Mostly matched with the methods in MediaController.ControllerCallback
        abstract void setCustomLayout(int seq, @NonNull List<CommandButton> layout)
                throws RemoteException;
        abstract void sendCustomCommand(int seq, @NonNull SessionCommand command,
                @Nullable Bundle args) throws RemoteException;
        abstract void onPlaybackInfoChanged(int seq, @NonNull PlaybackInfo info)
                throws RemoteException;
        abstract void onAllowedCommandsChanged(int seq, @NonNull SessionCommandGroup commands)
                throws RemoteException;
        abstract void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs,
                int playerState) throws RemoteException;
        abstract void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs,
                float speed) throws RemoteException;
        abstract void onBufferingStateChanged(int seq, @NonNull MediaItem item,
                @BuffState int bufferingState, long bufferedPositionMs, long eventTimeMs,
                long positionMs) throws RemoteException;
        abstract void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long position)
                throws RemoteException;
        abstract void onCurrentMediaItemChanged(int seq, @Nullable MediaItem item, int currentIdx,
                int previousIdx, int nextIdx) throws RemoteException;
        abstract void onPlaylistChanged(int seq, @NonNull List<MediaItem> playlist,
                @Nullable MediaMetadata metadata, int currentIdx, int previousIdx,
                int nextIdx) throws RemoteException;
        abstract void onPlaylistMetadataChanged(int seq, @Nullable MediaMetadata metadata)
                throws RemoteException;
        abstract void onShuffleModeChanged(int seq, @SessionPlayer.ShuffleMode int shuffleMode,
                int currentIdx, int previousIdx, int nextIdx) throws RemoteException;
        abstract void onRepeatModeChanged(int seq, @SessionPlayer.RepeatMode int repeatMode,
                int currentIdx, int previousIdx, int nextIdx) throws RemoteException;
        abstract void onPlaybackCompleted(int seq) throws RemoteException;
        abstract void onDisconnected(int seq) throws RemoteException;
        abstract void onVideoSizeChanged(int seq, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) throws RemoteException;
        abstract void onTrackInfoChanged(int seq, List<TrackInfo> trackInfos,
                TrackInfo selectedVideoTrack, TrackInfo selectedAudioTrack,
                TrackInfo selectedSubtitleTrack, TrackInfo selectedMetadataTrack)
                throws RemoteException;
        abstract void onTrackSelected(int seq, TrackInfo trackInfo) throws RemoteException;
        abstract void onTrackDeselected(int seq, TrackInfo trackInfo) throws RemoteException;
        abstract void onSubtitleData(int seq, @NonNull MediaItem item, @NonNull TrackInfo track,
                @NonNull SubtitleData data) throws RemoteException;

        // Mostly matched with the methods in MediaBrowser.BrowserCallback.
        abstract void onChildrenChanged(int seq, @NonNull String parentId, int itemCount,
                @Nullable LibraryParams params) throws RemoteException;
        abstract void onSearchResultChanged(int seq, @NonNull String query, int itemCount,
                @Nullable LibraryParams params) throws RemoteException;
    }

    interface MediaSessionImpl extends MediaInterface.SessionPlayer, AutoCloseable {
        void updatePlayer(@NonNull SessionPlayer player,
                @Nullable SessionPlayer playlistAgent);
        void updatePlayer(@NonNull SessionPlayer player);
        @NonNull
        SessionPlayer getPlayer();
        @NonNull
        String getId();
        @NonNull
        Uri getUri();
        @NonNull
        SessionToken getToken();
        @NonNull
        List<ControllerInfo> getConnectedControllers();
        boolean isConnected(@NonNull ControllerInfo controller);

        ListenableFuture<SessionResult> setCustomLayout(@NonNull ControllerInfo controller,
                @NonNull List<CommandButton> layout);
        void setAllowedCommands(@NonNull ControllerInfo controller,
                @NonNull SessionCommandGroup commands);
        void broadcastCustomCommand(@NonNull SessionCommand command, @Nullable Bundle args);
        ListenableFuture<SessionResult> sendCustomCommand(@NonNull ControllerInfo controller,
                @NonNull SessionCommand command, @Nullable Bundle args);

        // Internally used methods
        MediaSession getInstance();
        MediaSessionCompat getSessionCompat();
        Context getContext();
        Executor getCallbackExecutor();
        SessionCallback getCallback();
        boolean isClosed();
        PlaybackStateCompat createPlaybackStateCompat();
        PlaybackInfo getPlaybackInfo();
        PendingIntent getSessionActivity();
        IBinder getLegacyBrowserServiceBinder();
        void connectFromService(IMediaController caller, String packageName, int pid, int uid,
                @Nullable Bundle connectionHints);
    }

    /**
     * Base builder class for MediaSession and its subclass. Any change in this class should be
     * also applied to the subclasses {@link MediaSession.Builder} and
     * {@link MediaLibraryService.MediaLibrarySession.Builder}.
     * <p>
     * APIs here should be package private, but should have documentations for developers.
     * Otherwise, javadoc will generate documentation with the generic types such as follows.
     * <pre>U extends BuilderBase<T, U, C> setSessionCallback(Executor executor, C callback)</pre>
     * <p>
     * This class is hidden to prevent from generating test stub, which fails with
     * 'unexpected bound' because it tries to auto generate stub class as follows.
     * <pre>abstract static class BuilderBase<
     *      T extends MediaSession,
     *      U extends MediaSession.BuilderBase<
     *              T, U, C extends MediaSession.SessionCallback>, C></pre>
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    abstract static class BuilderBase
            <T extends MediaSession, U extends BuilderBase<T, U, C>, C extends SessionCallback> {
        final Context mContext;
        SessionPlayer mPlayer;
        String mId;
        Executor mCallbackExecutor;
        C mCallback;
        PendingIntent mSessionActivity;
        Bundle mExtras;

        BuilderBase(@NonNull Context context, @NonNull SessionPlayer player) {
            if (context == null) {
                throw new NullPointerException("context shouldn't be null");
            }
            if (player == null) {
                throw new NullPointerException("player shouldn't be null");
            }
            mContext = context;
            mPlayer = player;
            // Ensure non-null id.
            mId = "";
        }

        /**
         * Set an intent for launching UI for this Session. This can be used as a
         * quick link to an ongoing media screen. The intent should be for an
         * activity that may be started using {@link Context#startActivity(Intent)}.
         *
         * @param pi The intent to launch to show UI for this session.
         */
        @SuppressWarnings("unchecked")
        @NonNull
        U setSessionActivity(@Nullable PendingIntent pi) {
            mSessionActivity = pi;
            return (U) this;
        }

        /**
         * Set ID of the session. If it's not set, an empty string will be used to create a session.
         * <p>
         * Use this if and only if your app supports multiple playback at the same time and also
         * wants to provide external apps to have finer controls of them.
         *
         * @param id id of the session. Must be unique per package.
         * @return
         */
        // Note: This ID is not visible to the controllers. ID is introduced in order to prevent
        // apps from creating multiple sessions without any clear reasons. If they create two
        // sessions with the same ID in a process, then an IllegalStateException will be thrown.
        @SuppressWarnings("unchecked")
        @NonNull
        U setId(@NonNull String id) {
            if (id == null) {
                throw new NullPointerException("id shouldn't be null");
            }
            mId = id;
            return (U) this;
        }

        /**
         * Set callback for the session.
         *
         * @param executor callback executor
         * @param callback session callback.
         * @return
         */
        @SuppressWarnings("unchecked")
        @NonNull
        U setSessionCallback(@NonNull Executor executor, @NonNull C callback) {
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

        /**
         * Set extras for the session token.  If not set, {@link SessionToken#getExtras()}
         * will return {@link Bundle#EMPTY}.
         *
         * @return The Builder to allow chaining
         * @see SessionToken#getExtras()
         */
        @NonNull
        @SuppressWarnings("unchecked")
        public U setExtras(@NonNull Bundle extras) {
            if (extras == null) {
                throw new NullPointerException("extras shouldn't be null");
            }
            mExtras = extras;
            return (U) this;
        }

        /**
         * Build {@link MediaSession}.
         *
         * @return a new session
         * @throws IllegalStateException if the session with the same id is already exists for the
         *      package.
         */
        @NonNull abstract T build();
    }
}
