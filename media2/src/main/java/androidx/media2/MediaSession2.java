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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.SessionPlayer2.BuffState;
import androidx.media2.SessionPlayer2.PlayerState;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
 * A MediaSession2 should be created when an app wants to publish media playback information or
 * handle media keys. In general an app only needs one session for all playback, though multiple
 * sessions can be created to provide finer grain controls of media.
 * <p>
 * If you want to support background playback, {@link MediaSessionService2} is preferred
 * instead. With it, your playback can be revived even after playback is finished. See
 * {@link MediaSessionService2} for details.
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
 * to other processes to allow them to create a {@link MediaController2} to interact with the
 * session.
 * <p>
 * When a session receive transport control commands, the session sends the commands directly to
 * the the underlying media player set by {@link Builder} or {@link #updatePlayer}.
 * <p>
 * When an app is finished performing playback it must call {@link #close()} to clean up the session
 * and notify any controllers.
 * <p>
 * <a name="Thread"></a>
 * <h3>Thread</h3>
 * <p>
 * {@link MediaSession2} objects are thread safe, but should be used on the thread on the looper.
 * <a name="KeyEvents"></a>
 * <h3>Media key events mapping</h3>
 * <p>
 * Here's the table of per key event.
 * <table>
 * <tr><th>Key code</th><th>{@link MediaSession2} API</th></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PLAY}</td>
 *     <td>{@link SessionPlayer2#play()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PAUSE}</td>
 *     <td>{@link SessionPlayer2#pause()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_NEXT}</td>
 *     <td>{@link SessionPlayer2#skipToNextPlaylistItem()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_PREVIOUS}</td>
 *     <td>{@link SessionPlayer2#skipToPreviousPlaylistItem()}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_STOP}</td>
 *     <td>{@link SessionPlayer2#pause()} and then
 *         {@link SessionPlayer2#seekTo(long)} with 0</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_FAST_FORWARD}</td>
 *     <td>{@link SessionCallback#onFastForward}</td></tr>
 * <tr><td>{@link KeyEvent#KEYCODE_MEDIA_REWIND}</td>
 *     <td>{@link SessionCallback#onRewind}</td></tr>
 * <tr><td><ul><li>{@link KeyEvent#KEYCODE_MEDIA_PLAY_PAUSE}</li>
 *             <li>{@link KeyEvent#KEYCODE_HEADSETHOOK}</li></ul></td>
 *     <td><ul><li>For a single tap
 *             <ul><li>{@link SessionPlayer2#pause()} if
 *             {@link SessionPlayer2#PLAYER_STATE_PLAYING}</li>
 *             <li>{@link SessionPlayer2#play()} otherwise</li></ul>
 *             <li>For a double tap, {@link SessionPlayer2#skipToNextPlaylistItem()}</li></ul></td>
 *     </td>
 * </table>
 * @see MediaSessionService2
 */
@TargetApi(Build.VERSION_CODES.P)
public class MediaSession2 implements AutoCloseable {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({ERROR_CODE_UNKNOWN_ERROR, ERROR_CODE_APP_ERROR, ERROR_CODE_NOT_SUPPORTED,
            ERROR_CODE_AUTHENTICATION_EXPIRED, ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED,
            ERROR_CODE_CONCURRENT_STREAM_LIMIT, ERROR_CODE_PARENTAL_CONTROL_RESTRICTED,
            ERROR_CODE_NOT_AVAILABLE_IN_REGION, ERROR_CODE_CONTENT_ALREADY_PLAYING,
            ERROR_CODE_SKIP_LIMIT_REACHED, ERROR_CODE_ACTION_ABORTED, ERROR_CODE_END_OF_QUEUE,
            ERROR_CODE_SETUP_REQUIRED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ErrorCode {}

    /**
     * This is the default error code and indicates that none of the other error codes applies.
     */
    public static final int ERROR_CODE_UNKNOWN_ERROR = 0;

    /**
     * Error code when the application state is invalid to fulfill the request.
     */
    public static final int ERROR_CODE_APP_ERROR = 1;

    /**
     * Error code when the request is not supported by the application.
     */
    public static final int ERROR_CODE_NOT_SUPPORTED = 2;

    /**
     * Error code when the request cannot be performed because authentication has expired.
     */
    public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = 3;

    /**
     * Error code when a premium account is required for the request to succeed.
     */
    public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = 4;

    /**
     * Error code when too many concurrent streams are detected.
     */
    public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = 5;

    /**
     * Error code when the content is blocked due to parental controls.
     */
    public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = 6;

    /**
     * Error code when the content is blocked due to being regionally unavailable.
     */
    public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = 7;

    /**
     * Error code when the requested content is already playing.
     */
    public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = 8;

    /**
     * Error code when the application cannot skip any more songs because skip limit is reached.
     */
    public static final int ERROR_CODE_SKIP_LIMIT_REACHED = 9;

    /**
     * Error code when the action is interrupted due to some external event.
     */
    public static final int ERROR_CODE_ACTION_ABORTED = 10;

    /**
     * Error code when the playback navigation (previous, next) is not possible because the queue
     * was exhausted.
     */
    public static final int ERROR_CODE_END_OF_QUEUE = 11;

    /**
     * Error code when the session needs user's manual intervention.
     */
    public static final int ERROR_CODE_SETUP_REQUIRED = 12;

    static final String TAG = "MediaSession2";

    private final MediaSession2Impl mImpl;

    MediaSession2(Context context, String id, SessionPlayer2 player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        mImpl = createImpl(context, id, player, sessionActivity, callbackExecutor,
                callback);
    }

    MediaSession2Impl createImpl(Context context, String id, SessionPlayer2 player,
            PendingIntent sessionActivity, Executor callbackExecutor, SessionCallback callback) {
        return new MediaSession2ImplBase(this, context, id, player, sessionActivity,
                callbackExecutor, callback);
    }

    /**
     * Should be only used by subclass.
     */
    MediaSession2Impl getImpl() {
        return mImpl;
    }

    /**
     * Updates the underlying {@link SessionPlayer2} for this session to dispatch incoming event to.
     *
     * @param player a player that handles actual media playback in your app
     */
    public void updatePlayer(@NonNull SessionPlayer2 player) {
        mImpl.updatePlayer(player);
    }

    @Override
    public void close() {
        try {
            mImpl.close();
        } catch (Exception e) {
            // Should not be here.
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isClosed() {
        return mImpl.isClosed();
    }

    /**
     * Gets the underlying {@link SessionPlayer2}.
     * <p>
     * When the session is closed, it returns the lastly set player.
     *
     * @return player.
     */
    public @NonNull SessionPlayer2 getPlayer() {
        return mImpl.getPlayer();
    }

    /**
     * Gets the session ID
     *
     * @return
     */
    public @NonNull String getId() {
        return mImpl.getId();
    }

    /**
     * Returns the {@link SessionToken2} for creating {@link MediaController2}.
     */
    public @NonNull SessionToken2 getToken() {
        return mImpl.getToken();
    }

    @NonNull Context getContext() {
        return mImpl.getContext();
    }

    @NonNull Executor getCallbackExecutor() {
        return mImpl.getCallbackExecutor();
    }

    @NonNull SessionCallback getCallback() {
        return mImpl.getCallback();
    }

    /**
     * Returns the list of connected controller.
     *
     * @return list of {@link ControllerInfo}
     */
    public @NonNull List<ControllerInfo> getConnectedControllers() {
        return mImpl.getConnectedControllers();
    }

    /**
     * Sets ordered list of {@link CommandButton} for controllers to build UI with it.
     * <p>
     * It's up to controller's decision how to represent the layout in its own UI.
     * Here's the same way
     * (layout[i] means a CommandButton at index i in the given list)
     * For 5 icons row
     *      layout[3] layout[1] layout[0] layout[2] layout[4]
     * For 3 icons row
     *      layout[1] layout[0] layout[2]
     * For 5 icons row with overflow icon (can show +5 extra buttons with overflow button)
     *      expanded row:   layout[5] layout[6] layout[7] layout[8] layout[9]
     *      main row:       layout[3] layout[1] layout[0] layout[2] layout[4]
     * <p>
     * This API can be called in the
     * {@link SessionCallback#onConnect(MediaSession2, ControllerInfo)}.
     *
     * @param controller controller to specify layout.
     * @param layout ordered list of layout.
     */
    public void setCustomLayout(@NonNull ControllerInfo controller,
            @NonNull List<CommandButton> layout) {
        mImpl.setCustomLayout(controller, layout);
    }

    /**
     * Set the new allowed command group for the controller
     *
     * @param controller controller to change allowed commands
     * @param commands new allowed commands
     */
    public void setAllowedCommands(@NonNull ControllerInfo controller,
            @NonNull SessionCommandGroup2 commands) {
        mImpl.setAllowedCommands(controller, commands);
    }

    /**
     * Send custom command to all connected controllers.
     *
     * @param command a command
     * @param args optional argument
     */
    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args) {
        mImpl.sendCustomCommand(command, args);
    }

    /**
     * Send custom command to a specific controller.
     *
     * @param command a command
     * @param args optional argument
     * @param receiver result receiver for the session
     */
    public void sendCustomCommand(@NonNull ControllerInfo controller,
            @NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver receiver) {
        mImpl.sendCustomCommand(controller, command, args, receiver);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipForward() {
        mImpl.skipForward();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipBackward() {
        mImpl.skipBackward();
    }

    /**
     * Notify errors to the connected controllers
     *
     * @param errorCode error code
     * @param extras extras
     */
    public void notifyError(@ErrorCode int errorCode, @Nullable Bundle extras) {
        mImpl.notifyError(errorCode, extras);
    }

    /**
     * Notify routes information to a connected controller
     *
     * @param controller controller information
     * @param routes The routes information. Each bundle should be from {@link
     *               androidx.mediarouter.media.MediaRouter.RouteInfo#getUniqueRouteDescriptorBundle
     *               RouteInfo}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
            @Nullable List<Bundle> routes) {
        mImpl.notifyRoutesInfoChanged(controller, routes);
    }

    /**
     * @hide
     * @return Bundle
     */
    @RestrictTo(LIBRARY_GROUP)
    public MediaSessionCompat getSessionCompat() {
        return mImpl.getSessionCompat();
    }

    /**
     * Handles the controller's connection request from {@link MediaSessionService2}.
     *
     * @param controller controller aidl
     * @param packageName controller package name
     * @param pid controller pid
     * @param uid controller uid
     */
    void handleControllerConnectionFromService(IMediaController2 controller, String packageName,
            int pid, int uid) {
        mImpl.connectFromService(controller, packageName, pid, uid);
    }

    IBinder getLegacyBrowerServiceBinder() {
        return mImpl.getLegacyBrowserServiceBinder();
    }

    /**
     * Callback to be called for all incoming commands from {@link MediaController2}s.
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
         * You can reject the connection by return {@code null}. In that case, controller receives
         * {@link MediaController2.ControllerCallback#onDisconnected(MediaController2)} and cannot
         * be usable.
         *
         * @param session the session for this event
         * @param controller controller information.
         * @return allowed commands. Can be {@code null} to reject connection.
         */
        public @Nullable SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) {
            SessionCommandGroup2 commands = new SessionCommandGroup2.Builder()
                    .addAllPredefinedCommands(SessionCommand2.COMMAND_VERSION_1)
                    .build();
            return commands;
        }

        /**
         * Called when a controller is disconnected
         *
         * @param session the session for this event
         * @param controller controller information
         */
        public void onDisconnected(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) { }

        /**
         * Called when a controller sent a command which will be sent directly to one of the
         * following:
         * <ul>
         *  <li> {@link SessionPlayer2} </li>
         *  <li> {@link SessionPlayer2} </li>
         *  <li> {@link android.media.AudioManager}</li>
         * </ul>
         * Return {@code false} here to reject the request and stop sending command.
         *
         * @param session the session for this event
         * @param controller controller information.
         * @param command a command. This method will be called for every single command.
         * @return {@code true} if you want to accept incoming command. {@code false} otherwise.
         * @see SessionCommand2#COMMAND_CODE_PLAYER_PLAY
         * @see SessionCommand2#COMMAND_CODE_PLAYER_PAUSE
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_PREFETCH
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SEEK_TO
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SET_REPEAT_MODE
         * @see SessionCommand2#COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYER_GET_PLAYLIST
         * @see SessionCommand2#COMMAND_CODE_PLAYER_SET_PLAYLIST
         * @see SessionCommand2#COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA
         * @see SessionCommand2#COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA
         * @see SessionCommand2#COMMAND_CODE_VOLUME_SET_VOLUME
         * @see SessionCommand2#COMMAND_CODE_VOLUME_ADJUST_VOLUME
         */
        public boolean onCommandRequest(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand2 command) {
            return true;
        }

        /**
         * Called when a controller has sent a command with a {@link MediaItem2} to add a new media
         * item to this session. Being specific, this will be called for following APIs.
         * <ol>
         * <li>{@link MediaController2#addPlaylistItem(int, MediaItem2)}
         * <li>{@link MediaController2#replacePlaylistItem(int, MediaItem2)}
         * <li>{@link MediaController2#setPlaylist(List, MediaMetadata2)}
         * <li>{@link MediaController2#setMediaItem(MediaItem2)}
         * </ol>
         * Override this to translate incoming media items to be understood by your player. It's
         * highly recommended to create and return a new media item here. Otherwise session player
         * may reject the command.
         * <p>
         * For example, a player may only understand {@link FileMediaItem2}, {@link UriMediaItem2},
         * and {@link CallbackMediaItem2} while the media item from the controller would be
         * sanitized not to contain subclass information. In that case you need to override this
         * method to create a media item for player from given media item.
         * <p>
         * You may return {@code null} if the given item is invalid. Here's the behavior when it
         * happens.
         * <table border="0" cellspacing="0" cellpadding="0">
         * <tr><th>Controller command</th> <th>Behavior when {@code null} is returned</th></tr>
         * <tr><td>addPlaylistItem</td> <td>Ignore</td></tr>
         * <tr><td>replacePlaylistItem</td> <td>Ignore</td></tr>
         * <tr><td>setPlaylist</td>
         *     <td>Ignore {@code null} items, and build a list with non-{@code null} items. Call
         *         {@link SessionPlayer2#setPlaylist(List, MediaMetadata2)} with the list</td></tr>
         * <tr><td>setMediaItem</td> <td>Ignore</td></tr>
         * </table>
         * <p>
         * This will be called on the same thread where {@link #onCommandRequest} and commands with
         * the media controller will be executed.
         * <p>
         * Default implementation simply returns the media item from the controller although the
         * player may ignore or throw an exception.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param item incoming media item from the controller
         * @return translated media item for player. Can be {@code null} to ignore
         */
        public @Nullable MediaItem2 onCreateMediaItem(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull MediaItem2 item) {
            return item;
        }

        /**
         * Called when a controller set rating of a media item through
         * {@link MediaController2#setRating(String, Rating2)}.
         * <p>
         * To allow setting user rating for a {@link MediaItem2}, the media item's metadata
         * should have {@link Rating2} with the key {@link MediaMetadata2#METADATA_KEY_USER_RATING},
         * in order to provide possible rating style for controller. Controller will follow the
         * rating style.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId media id from the controller
         * @param rating new rating from the controller
         * @see SessionCommand2#COMMAND_CODE_SESSION_SET_RATING
         */
        public void onSetRating(@NonNull MediaSession2 session, @NonNull ControllerInfo controller,
                @NonNull String mediaId, @NonNull Rating2 rating) { }

        /**
         * Called when a controller sent a custom command through
         * {@link MediaController2#sendCustomCommand(SessionCommand2, Bundle, ResultReceiver)}.
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
         * @param cb optional result receiver
         * @see SessionCommand2#COMMAND_CODE_CUSTOM
         */
        public void onCustomCommand(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand2 customCommand,
                @Nullable Bundle args, @Nullable ResultReceiver cb) { }

        /**
         * Called when a controller requested to play a specific mediaId through
         * {@link MediaController2#playFromMediaId(String, Bundle)}.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId media id
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID
         */
        public void onPlayFromMediaId(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to begin playback from a search query through
         * {@link MediaController2#playFromSearch(String, Bundle)}
         * <p>
         * An empty query indicates that the app may play any music. The implementation should
         * attempt to make a smart choice about what to play.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param query query string. Can be empty to indicate any suggested media
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PLAY_FROM_SEARCH
         */
        public void onPlayFromSearch(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String query,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to play a specific media item represented by a URI
         * through {@link MediaController2#playFromUri(Uri, Bundle)}
         *
         * @param session the session for this event
         * @param controller controller information
         * @param uri uri
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PLAY_FROM_URI
         */
        public void onPlayFromUri(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull Uri uri,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to prefetch for playing a specific mediaId through
         * {@link MediaController2#prefetchFromMediaId(String, Bundle)}.
         * <p>
         * During the prefetch, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer2#PLAYER_STATE_PAUSED} after the prefetch is done.
         * <p>
         * The playback of the prefetched content should start in the later calls of
         * {@link SessionPlayer2#play()}.
         * <p>
         * Override {@link #onPlayFromMediaId} to handle requests for starting
         * playback without preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId media id to prefetch
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREFETCH_FROM_MEDIA_ID
         */
        public void onPrefetchFromMediaId(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to prefetch playback from a search query through
         * {@link MediaController2#prefetchFromSearch(String, Bundle)}.
         * <p>
         * An empty query indicates that the app may prefetch any music. The implementation should
         * attempt to make a smart choice about what to play.
         * <p>
         * During the prefetch, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer2#PLAYER_STATE_PAUSED} after the prefetch is done.
         * <p>
         * The playback of the prefetched content should start in the later calls of
         * {@link SessionPlayer2#play()}.
         * <p>
         * Override {@link #onPlayFromSearch} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param query query string. Can be empty to indicate any suggested media
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREFETCH_FROM_SEARCH
         */
        public void onPrefetchFromSearch(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String query,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to prefetch a specific media item represented by a URI
         * through {@link MediaController2#prefetchFromUri(Uri, Bundle)}.
         * <p>
         * During the prefetch, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link SessionPlayer2#PLAYER_STATE_PAUSED} after the prefetch is done.
         * <p>
         * The playback of the prefetched content should start in the later calls of
         * {@link SessionPlayer2#play()}.
         * <p>
         * Override {@link #onPlayFromUri} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param uri uri
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREFETCH_FROM_URI
         */
        public void onPrefetchFromUri(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull Uri uri, @Nullable Bundle extras) { }

        /**
         * Called when a controller called {@link MediaController2#fastForward()}
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand2#COMMAND_CODE_SESSION_FAST_FORWARD
         */
        public void onFastForward(@NonNull MediaSession2 session, ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#rewind()}
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand2#COMMAND_CODE_SESSION_REWIND
         */
        public void onRewind(@NonNull MediaSession2 session, ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#subscribeRoutesInfo()}
         * Session app should notify the routes information by calling
         * {@link MediaSession2#notifyRoutesInfoChanged(ControllerInfo, List)}.
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand2#COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onSubscribeRoutesInfo(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#unsubscribeRoutesInfo()}
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand2#COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onUnsubscribeRoutesInfo(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#selectRoute(Bundle)}.
         * @param session the session for this event
         * @param controller controller information
         * @param route The route bundle from {@link
         *              androidx.mediarouter.media.MediaRouter.RouteInfo
         *              #getUniqueRouteDescriptorBundle RouteInfo}
         * @see SessionCommand2#COMMAND_CODE_SESSION_SELECT_ROUTE
         * @see androidx.mediarouter.media.MediaRouter.RouteInfo#getUniqueRouteDescriptorBundle
         * @see androidx.mediarouter.media.MediaRouter#getRoute
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onSelectRoute(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull Bundle route) { }

        /**
         * Called when the player state is changed. Used internally for setting the
         * {@link MediaSessionService2} as foreground/background.
         */
        final void onPlayerStateChanged(MediaSession2 session, @PlayerState int state) {
            if (mForegroundServiceEventCallback != null) {
                mForegroundServiceEventCallback.onPlayerStateChanged(session, state);
            }
        }

        final void onSessionClosed(MediaSession2 session) {
            if (mForegroundServiceEventCallback != null) {
                mForegroundServiceEventCallback.onSessionClosed(session);
            }
        }

        void setForegroundServiceEventCallback(ForegroundServiceEventCallback callback) {
            mForegroundServiceEventCallback = callback;
        }

        abstract static class ForegroundServiceEventCallback {
            public void onPlayerStateChanged(MediaSession2 session, @PlayerState int state) { }
            public void onSessionClosed(MediaSession2 session) { }
        }
    }

    /**
     * Builder for {@link MediaSession2}.
     * <p>
     * Any incoming event from the {@link MediaController2} will be handled on the thread
     * that created session with the {@link Builder#build()}.
     */
    public static final class Builder extends BuilderBase<MediaSession2, Builder, SessionCallback> {
        public Builder(@NonNull Context context, @NonNull SessionPlayer2 player) {
            super(context, player);
        }

        @Override
        public @NonNull Builder setSessionActivity(@Nullable PendingIntent pi) {
            return super.setSessionActivity(pi);
        }

        @Override
        public @NonNull Builder setId(@NonNull String id) {
            return super.setId(id);
        }

        @Override
        public @NonNull Builder setSessionCallback(@NonNull Executor executor,
                @NonNull SessionCallback callback) {
            return super.setSessionCallback(executor, callback);
        }

        @Override
        public @NonNull MediaSession2 build() {
            if (mCallbackExecutor == null) {
                mCallbackExecutor = ContextCompat.getMainExecutor(mContext);
            }
            if (mCallback == null) {
                mCallback = new SessionCallback() {};
            }
            return new MediaSession2(mContext, mId, mPlayer, mSessionActivity,
                    mCallbackExecutor, mCallback);
        }
    }

    /**
     * Information of a controller.
     */
    public static final class ControllerInfo {
        private final RemoteUserInfo mRemoteUserInfo;
        private final boolean mIsTrusted;
        private final ControllerCb mControllerCb;

        /**
         * @param remoteUserInfo remote user info
         * @param trusted {@code true} if trusted, {@code false} otherwise
         * @param cb ControllerCb. Can be {@code null} only when a MediaBrowserCompat connects to
         *           MediaSessionService2 and ControllerInfo is needed for
         *           SessionCallback#onConnected().
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        ControllerInfo(@NonNull RemoteUserInfo remoteUserInfo, boolean trusted,
                @Nullable ControllerCb cb) {
            mRemoteUserInfo = remoteUserInfo;
            mIsTrusted = trusted;
            mControllerCb = cb;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public @NonNull RemoteUserInfo getRemoteUserInfo() {
            return mRemoteUserInfo;
        }

        /**
         * @return package name of the controller. Can be
         *         {@link androidx.media.MediaSessionManager.RemoteUserInfo#LEGACY_CONTROLLER} if
         *         the package name cannot be obtained.
         */
        public @NonNull String getPackageName() {
            return mRemoteUserInfo.getPackageName();
        }

        /**
         * @return uid of the controller. Can be a negative value if the uid cannot be obtained.
         */
        public int getUid() {
            return mRemoteUserInfo.getUid();
        }

        /**
         * Return if the controller has granted {@code android.permission.MEDIA_CONTENT_CONTROL} or
         * has a enabled notification listener so can be trusted to accept connection and incoming
         * command request.
         *
         * @return {@code true} if the controller is trusted.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public boolean isTrusted() {
            return mIsTrusted;
        }

        @Override
        public int hashCode() {
            return mControllerCb != null ? mControllerCb.hashCode() : 0;
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
     * Button for a {@link SessionCommand2} that will be shown by the controller.
     * <p>
     * It's up to the controller's decision to respect or ignore this customization request.
     */
    @VersionedParcelize
    public static final class CommandButton implements VersionedParcelable {
        private static final String KEY_COMMAND = "android.media.session2.command_button.command";
        private static final String KEY_ICON_RES_ID =
                "android.media.session2.command_button.icon_res_id";
        private static final String KEY_DISPLAY_NAME =
                "android.media.session2.command_button.display_name";
        private static final String KEY_EXTRAS = "android.media.session2.command_button.extras";
        private static final String KEY_ENABLED = "android.media.session2.command_button.enabled";

        @ParcelField(1)
        SessionCommand2 mCommand;
        @ParcelField(2)
        int mIconResId;
        @ParcelField(3)
        String mDisplayName;
        @ParcelField(4)
        Bundle mExtras;
        @ParcelField(5)
        boolean mEnabled;

        /**
         * Used for VersionedParcelable
         */
        CommandButton() {
        }

        CommandButton(@Nullable SessionCommand2 command, int iconResId,
                @Nullable String displayName, Bundle extras, boolean enabled) {
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
        public @Nullable SessionCommand2 getCommand() {
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
        public @Nullable String getDisplayName() {
            return mDisplayName;
        }

        /**
         * Extra information of the button. It's private information between session and controller.
         *
         * @return
         */
        public @Nullable Bundle getExtras() {
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
         * @hide
         * @return Bundle
         */
        @RestrictTo(LIBRARY_GROUP)
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putBundle(KEY_COMMAND, mCommand.toBundle());
            bundle.putInt(KEY_ICON_RES_ID, mIconResId);
            bundle.putString(KEY_DISPLAY_NAME, mDisplayName);
            bundle.putBundle(KEY_EXTRAS, mExtras);
            bundle.putBoolean(KEY_ENABLED, mEnabled);
            return bundle;
        }

        /**
         * @hide
         * @return CommandButton
         */
        @RestrictTo(LIBRARY_GROUP)
        public static @Nullable CommandButton fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            CommandButton.Builder builder = new CommandButton.Builder();
            builder.setCommand(SessionCommand2.fromBundle(bundle.getBundle(KEY_COMMAND)));
            builder.setIconResId(bundle.getInt(KEY_ICON_RES_ID, 0));
            builder.setDisplayName(bundle.getString(KEY_DISPLAY_NAME));
            builder.setExtras(bundle.getBundle(KEY_EXTRAS));
            builder.setEnabled(bundle.getBoolean(KEY_ENABLED));
            try {
                return builder.build();
            } catch (IllegalStateException e) {
                // Malformed or version mismatch. Return null for now.
                return null;
            }
        }

        /**
         * Builder for {@link CommandButton}.
         */
        public static final class Builder {
            private SessionCommand2 mCommand;
            private int mIconResId;
            private String mDisplayName;
            private Bundle mExtras;
            private boolean mEnabled;

            /**
             * Sets the {@link SessionCommand2} that would be sent to the session when the button
             * is clicked.
             *
             * @param command session command
             */
            public @NonNull Builder setCommand(@Nullable SessionCommand2 command) {
                mCommand = command;
                return this;
            }

            /**
             * Sets the bitmap-type (e.g. PNG) icon resource id of the button.
             * <p>
             * None bitmap type (e.g. VectorDrawabale) may cause unexpected behavior when it's sent
             * to {@link MediaController2} app, so please avoid using it especially for the older
             * platform (API < 21).
             *
             * @param resId resource id of the button
             */
            public @NonNull Builder setIconResId(int resId) {
                mIconResId = resId;
                return this;
            }

            /**
             * Sets the display name of the button.
             *
             * @param displayName display name of the button
             */
            public @NonNull Builder setDisplayName(@Nullable String displayName) {
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
            public @NonNull Builder setEnabled(boolean enabled) {
                mEnabled = enabled;
                return this;
            }

            /**
             * Sets the extras of the button.
             *
             * @param extras extras information of the button
             */
            public @NonNull Builder setExtras(@Nullable Bundle extras) {
                mExtras = extras;
                return this;
            }

            /**
             * Builds the {@link CommandButton}.
             *
             * @return a new {@link CommandButton}
             */
            public @NonNull CommandButton build() {
                return new CommandButton(mCommand, mIconResId, mDisplayName, mExtras, mEnabled);
            }
        }
    }

    abstract static class ControllerCb {
        // Mostly matched with the methods in MediaController2.ControllerCallback
        abstract void onCustomLayoutChanged(@NonNull List<CommandButton> layout)
                throws RemoteException;
        abstract void onPlaybackInfoChanged(@NonNull PlaybackInfo info) throws RemoteException;
        abstract void onAllowedCommandsChanged(@NonNull SessionCommandGroup2 commands)
                throws RemoteException;
        abstract void onCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver) throws RemoteException;
        abstract void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException;
        abstract void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException;
        abstract void onBufferingStateChanged(@NonNull MediaItem2 item,
                @BuffState int bufferingState, long bufferedPositionMs) throws RemoteException;
        abstract void onSeekCompleted(long eventTimeMs, long positionMs, long position)
                throws RemoteException;
        abstract void onError(@ErrorCode int errorCode, @Nullable Bundle extras)
                throws RemoteException;
        abstract void onCurrentMediaItemChanged(@Nullable MediaItem2 item) throws RemoteException;
        abstract void onPlaylistChanged(@NonNull List<MediaItem2> playlist,
                @Nullable MediaMetadata2 metadata) throws RemoteException;
        abstract void onPlaylistMetadataChanged(@Nullable MediaMetadata2 metadata)
                throws RemoteException;
        abstract void onShuffleModeChanged(@SessionPlayer2.ShuffleMode int shuffleMode)
                throws RemoteException;
        abstract void onRepeatModeChanged(@SessionPlayer2.RepeatMode int repeatMode)
                throws RemoteException;
        abstract void onPlaybackCompleted() throws RemoteException;
        abstract void onRoutesInfoChanged(@Nullable List<Bundle> routes) throws RemoteException;
        abstract void onDisconnected() throws RemoteException;

        // Mostly matched with the methods in MediaBrowser2.BrowserCallback.
        abstract void onGetLibraryRootDone(@Nullable Bundle rootHints, @Nullable String rootMediaId,
                @Nullable Bundle rootExtra) throws RemoteException;
        abstract void onChildrenChanged(@NonNull String parentId, int itemCount,
                @Nullable Bundle extras) throws RemoteException;
        abstract void onGetChildrenDone(@NonNull String parentId, int page, int pageSize,
                @Nullable List<MediaItem2> result, @Nullable Bundle extras) throws RemoteException;
        abstract void onGetItemDone(@NonNull String mediaId, @Nullable MediaItem2 result)
                throws RemoteException;
        abstract void onSearchResultChanged(@NonNull String query, int itemCount,
                @Nullable Bundle extras) throws RemoteException;
        abstract void onGetSearchResultDone(@NonNull String query, int page, int pageSize,
                @Nullable List<MediaItem2> result, @Nullable Bundle extras) throws RemoteException;
    }

    interface MediaSession2Impl extends MediaInterface2.SessionPlayer, AutoCloseable {
        void updatePlayer(@NonNull SessionPlayer2 player,
                @Nullable SessionPlayer2 playlistAgent);
        void updatePlayer(@NonNull SessionPlayer2 player);
        @NonNull SessionPlayer2 getPlayer();
        @NonNull String getId();
        @NonNull SessionToken2 getToken();
        @NonNull List<ControllerInfo> getConnectedControllers();
        boolean isConnected(@NonNull ControllerInfo controller);

        void setCustomLayout(@NonNull ControllerInfo controller,
                @NonNull List<CommandButton> layout);
        void setAllowedCommands(@NonNull ControllerInfo controller,
                @NonNull SessionCommandGroup2 commands);
        void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args);
        void sendCustomCommand(@NonNull ControllerInfo controller,
                @NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver);
        void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
                @Nullable List<Bundle> routes);

        // Internally used methods
        MediaSession2 getInstance();
        MediaSessionCompat getSessionCompat();
        Context getContext();
        Executor getCallbackExecutor();
        SessionCallback getCallback();
        boolean isClosed();
        PlaybackStateCompat createPlaybackStateCompat();
        PlaybackInfo getPlaybackInfo();
        PendingIntent getSessionActivity();
        IBinder getLegacyBrowserServiceBinder();
        void connectFromService(IMediaController2 caller, String packageName, int pid, int uid);
    }

    /**
     * Base builder class for MediaSession2 and its subclass. Any change in this class should be
     * also applied to the subclasses {@link MediaSession2.Builder} and
     * {@link MediaLibraryService2.MediaLibrarySession.Builder}.
     * <p>
     * APIs here should be package private, but should have documentations for developers.
     * Otherwise, javadoc will generate documentation with the generic types such as follows.
     * <pre>U extends BuilderBase<T, U, C> setSessionCallback(Executor executor, C callback)</pre>
     * <p>
     * This class is hidden to prevent from generating test stub, which fails with
     * 'unexpected bound' because it tries to auto generate stub class as follows.
     * <pre>abstract static class BuilderBase<
     *      T extends android.media.MediaSession2,
     *      U extends android.media.MediaSession2.BuilderBase<
     *              T, U, C extends android.media.MediaSession2.SessionCallback>, C></pre>
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    abstract static class BuilderBase
            <T extends MediaSession2, U extends BuilderBase<T, U, C>, C extends SessionCallback> {
        final Context mContext;
        SessionPlayer2 mPlayer;
        String mId;
        Executor mCallbackExecutor;
        C mCallback;
        PendingIntent mSessionActivity;

        BuilderBase(@NonNull Context context, @NonNull SessionPlayer2 player) {
            if (context == null) {
                throw new IllegalArgumentException("context shouldn't be null");
            }
            if (player == null) {
                throw new IllegalArgumentException("player shouldn't be null");
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
        @NonNull U setSessionActivity(@Nullable PendingIntent pi) {
            mSessionActivity = pi;
            return (U) this;
        }

        /**
         * Set ID of the session. If it's not set, an empty string with used to create a session.
         * <p>
         * Use this if and only if your app supports multiple playback at the same time and also
         * wants to provide external apps to have finer controls of them.
         *
         * @param id id of the session. Must be unique per package.
         * @throws IllegalArgumentException if id is {@code null}
         * @return
         */
        @NonNull U setId(@NonNull String id) {
            if (id == null) {
                throw new IllegalArgumentException("id shouldn't be null");
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
        @NonNull U setSessionCallback(@NonNull Executor executor, @NonNull C callback) {
            if (executor == null) {
                throw new IllegalArgumentException("executor shouldn't be null");
            }
            if (callback == null) {
                throw new IllegalArgumentException("callback shouldn't be null");
            }
            mCallbackExecutor = executor;
            mCallback = callback;
            return (U) this;
        }

        /**
         * Build {@link MediaSession2}.
         *
         * @return a new session
         * @throws IllegalStateException if the session with the same id is already exists for the
         *      package.
         */
        @NonNull abstract T build();
    }
}
