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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFocusRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.MediaController2.PlaybackInfo;
import androidx.media.MediaPlayerInterface.BuffState;
import androidx.media.MediaPlayerInterface.PlayerState;
import androidx.media.MediaPlaylistAgent.PlaylistEventCallback;
import androidx.media.MediaPlaylistAgent.RepeatMode;
import androidx.media.MediaPlaylistAgent.ShuffleMode;

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
 * A session can be obtained by {@link Builder}. The owner of the session may pass its session token
 * to other processes to allow them to create a {@link MediaController2} to interact with the
 * session.
 * <p>
 * When a session receive transport control commands, the session sends the commands directly to
 * the the underlying media player set by {@link Builder} or
 * {@link #updatePlayer}.
 * <p>
 * When an app is finished performing playback it must call {@link #close()} to clean up the session
 * and notify any controllers.
 * <p>
 * {@link MediaSession2} objects should be used on the thread on the looper.
 *
 * @see MediaSessionService2
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class MediaSession2 extends MediaInterface2.SessionPlayer implements AutoCloseable {
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

    private final SupportLibraryImpl mImpl;

    MediaSession2(SupportLibraryImpl impl) {
        mImpl = impl;
    }

    SupportLibraryImpl getImpl() {
        return mImpl;
    }

    /**
     * Sets the underlying {@link MediaPlayerInterface} and {@link MediaPlaylistAgent} for this
     * session to dispatch incoming event to.
     * <p>
     * When a {@link MediaPlaylistAgent} is specified here, the playlist agent should manage
     * {@link MediaPlayerInterface} for calling
     * {@link MediaPlayerInterface#setNextDataSources(List)}.
     * <p>
     * If the {@link MediaPlaylistAgent} isn't set, session will recreate the default playlist
     * agent.
     *
     * @param player a {@link MediaPlayerInterface} that handles actual media playback in your app
     * @param playlistAgent a {@link MediaPlaylistAgent} that manages playlist of the {@code player}
     * @param volumeProvider a {@link VolumeProviderCompat}. If {@code null}, system will adjust the
     *                       appropriate stream volume for this session's player.
     */
    public void updatePlayer(@NonNull MediaPlayerInterface player,
            @Nullable MediaPlaylistAgent playlistAgent,
            @Nullable VolumeProviderCompat volumeProvider) {
        mImpl.updatePlayer(player, playlistAgent, volumeProvider);
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
     * @return player
     */
    public @NonNull MediaPlayerInterface getPlayer() {
        return mImpl.getPlayer();
    }

    /**
     * @return playlist agent
     */
    public @NonNull MediaPlaylistAgent getPlaylistAgent() {
        return mImpl.getPlaylistAgent();
    }

    /**
     * @return volume provider
     */
    public @Nullable VolumeProviderCompat getVolumeProvider() {
        return mImpl.getVolumeProvider();
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
     * Set the {@link AudioFocusRequest} to obtain the audio focus
     *
     * @param afr the full request parameters
     */
    public void setAudioFocusRequest(@Nullable AudioFocusRequest afr) {
        mImpl.setAudioFocusRequest(afr);
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
     * Play playback.
     * <p>
     * This calls {@link MediaPlayerInterface#play()}.
     */
    @Override
    public void play() {
        mImpl.play();
    }

    /**
     * Pause playback.
     * <p>
     * This calls {@link MediaPlayerInterface#pause()}.
     */
    @Override
    public void pause() {
        mImpl.pause();
    }

    /**
     * Stop playback, and reset the player to the initial state.
     * <p>
     * This calls {@link MediaPlayerInterface#reset()}.
     */
    @Override
    public void reset() {
        mImpl.reset();
    }

    /**
     * Request that the player prepare its playback. In other words, other sessions can continue
     * to play during the preparation of this session. This method can be used to speed up the
     * start of the playback. Once the preparation is done, the session will change its playback
     * state to {@link MediaPlayerInterface#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be
     * called to start playback.
     * <p>
     * This calls {@link MediaPlayerInterface#reset()}.
     */
    @Override
    public void prepare() {
        mImpl.prepare();
    }

    /**
     * Move to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    @Override
    public void seekTo(long pos) {
        mImpl.seekTo(pos);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void skipForward() {
        mImpl.skipForward();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void skipBackward() {
        mImpl.skipBackward();
    }

    /**
     * Notify errors to the connected controllers
     *
     * @param errorCode error code
     * @param extras extras
     */
    @Override
    public void notifyError(@ErrorCode int errorCode, @Nullable Bundle extras) {
        mImpl.notifyError(errorCode, extras);
    }

    /**
     * Notify routes information to a connected controller
     *
     * @param controller controller information
     * @param routes The routes information. Each bundle should be from
     *              MediaRouteDescritor.asBundle().
     */
    public void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
            @Nullable List<Bundle> routes) {
        mImpl.notifyRoutesInfoChanged(controller, routes);
    }

    /**
     * Gets the current player state.
     *
     * @return the current player state
     */
    @Override
    public @PlayerState int getPlayerState() {
        return mImpl.getPlayerState();
    }

    /**
     * Gets the current position.
     *
     * @return the current playback position in ms, or {@link MediaPlayerInterface#UNKNOWN_TIME} if
     *         unknown.
     */
    @Override
    public long getCurrentPosition() {
        return mImpl.getCurrentPosition();
    }

    /**
     * Gets the duration of the currently playing media item.
     *
     * @return the duration of the current item from {@link MediaPlayerInterface#getDuration()}.
     */
    @Override
    public long getDuration() {
        return mImpl.getDuration();
    }

    /**
     * Gets the buffered position, or {@link MediaPlayerInterface#UNKNOWN_TIME} if unknown.
     *
     * @return the buffered position in ms, or {@link MediaPlayerInterface#UNKNOWN_TIME}.
     */
    @Override
    public long getBufferedPosition() {
        return mImpl.getBufferedPosition();
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     *
     * @return the buffering state.
     */
    @Override
    public @BuffState int getBufferingState() {
        return mImpl.getBufferingState();
    }

    /**
     * Get the playback speed.
     *
     * @return speed
     */
    @Override
    public float getPlaybackSpeed() {
        return mImpl.getPlaybackSpeed();
    }

    /**
     * Set the playback speed.
     */
    @Override
    public void setPlaybackSpeed(float speed) {
        mImpl.setPlaybackSpeed(speed);
    }

    /**
     * Sets the data source missing helper. Helper will be used to provide default implementation of
     * {@link MediaPlaylistAgent} when it isn't set by developer.
     * <p>
     * Default implementation of the {@link MediaPlaylistAgent} will call helper when a
     * {@link MediaItem2} in the playlist doesn't have a {@link DataSourceDesc}. This may happen
     * when
     * <ul>
     *      <li>{@link MediaItem2} specified by {@link #setPlaylist(List, MediaMetadata2)} doesn't
     *          have {@link DataSourceDesc}</li>
     *      <li>{@link MediaController2#addPlaylistItem(int, MediaItem2)} is called and accepted
     *          by {@link SessionCallback#onCommandRequest(
     *          MediaSession2, ControllerInfo, SessionCommand2)}.
     *          In that case, an item would be added automatically without the data source.</li>
     * </ul>
     * <p>
     * If it's not set, playback wouldn't happen for the item without data source descriptor.
     * <p>
     * The helper will be run on the executor that was specified by
     * {@link Builder#setSessionCallback(Executor, SessionCallback)}.
     *
     * @param helper a data source missing helper.
     * @throws IllegalStateException when the helper is set when the playlist agent is set
     * @see #setPlaylist(List, MediaMetadata2)
     * @see SessionCallback#onCommandRequest(MediaSession2, ControllerInfo, SessionCommand2)
     * @see SessionCommand2#COMMAND_CODE_PLAYLIST_ADD_ITEM
     * @see SessionCommand2#COMMAND_CODE_PLAYLIST_REPLACE_ITEM
     */
    @Override
    public void setOnDataSourceMissingHelper(@NonNull OnDataSourceMissingHelper helper) {
        mImpl.setOnDataSourceMissingHelper(helper);
    }

    /**
     * Clears the data source missing helper.
     *
     * @see #setOnDataSourceMissingHelper(OnDataSourceMissingHelper)
     */
    @Override
    public void clearOnDataSourceMissingHelper() {
        mImpl.clearOnDataSourceMissingHelper();
    }

    /**
     * Returns the playlist from the {@link MediaPlaylistAgent}.
     * <p>
     * This list may differ with the list that was specified with
     * {@link #setPlaylist(List, MediaMetadata2)} depending on the {@link MediaPlaylistAgent}
     * implementation. Use media items returned here for other playlist agent APIs such as
     * {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)}.
     *
     * @return playlist
     * @see MediaPlaylistAgent#getPlaylist()
     * @see SessionCallback#onPlaylistChanged(
     *          MediaSession2, MediaPlaylistAgent, List, MediaMetadata2)
     */
    @Override
    public List<MediaItem2> getPlaylist() {
        return mImpl.getPlaylist();
    }

    /**
     * Sets a list of {@link MediaItem2} to the {@link MediaPlaylistAgent}. Ensure uniqueness of
     * each {@link MediaItem2} in the playlist so the session can uniquely identity individual
     * items.
     * <p>
     * This may be an asynchronous call, and {@link MediaPlaylistAgent} may keep the copy of the
     * list. Wait for {@link SessionCallback#onPlaylistChanged(MediaSession2, MediaPlaylistAgent,
     * List, MediaMetadata2)} to know the operation finishes.
     * <p>
     * You may specify a {@link MediaItem2} without {@link DataSourceDesc}. In that case,
     * {@link MediaPlaylistAgent} has responsibility to dynamically query {link DataSourceDesc}
     * when such media item is ready for preparation or play. Default implementation needs
     * {@link OnDataSourceMissingHelper} for such case.
     * <p>
     * It's recommended to fill {@link MediaMetadata2} in each {@link MediaItem2} especially for the
     * duration information with the key {@link MediaMetadata2#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     *
     * @param list A list of {@link MediaItem2} objects to set as a play list.
     * @throws IllegalArgumentException if given list is {@code null}, or has duplicated media
     * items.
     * @see MediaPlaylistAgent#setPlaylist(List, MediaMetadata2)
     * @see SessionCallback#onPlaylistChanged(
     *          MediaSession2, MediaPlaylistAgent, List, MediaMetadata2)
     * @see #setOnDataSourceMissingHelper
     */
    @Override
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        mImpl.setPlaylist(list, metadata);
    }

    /**
     * Skips to the item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)} and the behavior depends
     * on the playlist agent implementation, especially with the shuffle/repeat mode.
     *
     * @param item The item in the playlist you want to play
     * @see #getShuffleMode()
     * @see #getRepeatMode()
     */
    @Override
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        mImpl.skipToPlaylistItem(item);
    }

    /**
     * Skips to the previous item.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPreviousItem()} and the behavior depends on the
     * playlist agent implementation, especially with the shuffle/repeat mode.
     *
     * @see #getShuffleMode()
     * @see #getRepeatMode()
     **/
    @Override
    public void skipToPreviousItem() {
        mImpl.skipToPreviousItem();
    }

    /**
     * Skips to the next item.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToNextItem()} and the behavior depends on the
     * playlist agent implementation, especially with the shuffle/repeat mode.
     *
     * @see #getShuffleMode()
     * @see #getRepeatMode()
     */
    @Override
    public void skipToNextItem() {
        mImpl.skipToNextItem();
    }

    /**
     * Gets the playlist metadata from the {@link MediaPlaylistAgent}.
     *
     * @return the playlist metadata
     */
    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return mImpl.getPlaylistMetadata();
    }

    /**
     * Adds the media item to the playlist at position index. Index equals or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * This will not change the currently playing media item.
     * If index is less than or equal to the current index of the play list,
     * the current index of the play list will be incremented correspondingly.
     *
     * @param index the index you want to add
     * @param item the media item you want to add
     */
    @Override
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        mImpl.addPlaylistItem(index, item);
    }

    /**
     * Removes the media item in the playlist.
     * <p>
     * If the item is the currently playing item of the playlist, current playback
     * will be stopped and playback moves to next source in the list.
     *
     * @param item the media item you want to add
     */
    @Override
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        mImpl.removePlaylistItem(item);
    }

    /**
     * Replaces the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     *
     * @param index the index of the item to replace
     * @param item the new item
     */
    @Override
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        mImpl.replacePlaylistItem(index, item);
    }

    /**
     * Return currently playing media item.
     *
     * @return currently playing media item
     */
    @Override
    public MediaItem2 getCurrentMediaItem() {
        return mImpl.getCurrentMediaItem();
    }

    /**
     * Updates the playlist metadata to the {@link MediaPlaylistAgent}.
     *
     * @param metadata metadata of the playlist
     */
    @Override
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        mImpl.updatePlaylistMetadata(metadata);
    }

    /**
     * Gets the repeat mode from the {@link MediaPlaylistAgent}.
     *
     * @return repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    @Override
    public @RepeatMode int getRepeatMode() {
        return mImpl.getRepeatMode();
    }

    /**
     * Sets the repeat mode to the {@link MediaPlaylistAgent}.
     *
     * @param repeatMode repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    @Override
    public void setRepeatMode(@RepeatMode int repeatMode) {
        mImpl.setRepeatMode(repeatMode);
    }

    /**
     * Gets the shuffle mode from the {@link MediaPlaylistAgent}.
     *
     * @return The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    @Override
    public @ShuffleMode int getShuffleMode() {
        return mImpl.getShuffleMode();
    }

    /**
     * Sets the shuffle mode to the {@link MediaPlaylistAgent}.
     *
     * @param shuffleMode The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    @Override
    public void setShuffleMode(@ShuffleMode int shuffleMode) {
        mImpl.setShuffleMode(shuffleMode);
    }

    /**
     * Interface definition of a callback to be invoked when a {@link MediaItem2} in the playlist
     * didn't have a {@link DataSourceDesc} but it's needed now for preparing or playing it.
     *
     * #see #setOnDataSourceMissingHelper
     */
    public interface OnDataSourceMissingHelper {
        /**
         * Called when a {@link MediaItem2} in the playlist didn't have a {@link DataSourceDesc}
         * but it's needed now for preparing or playing it. Returned data source descriptor will be
         * sent to the player directly to prepare or play the contents.
         * <p>
         * An exception may be thrown if the returned {@link DataSourceDesc} is duplicated in the
         * playlist, so items cannot be differentiated.
         *
         * @param session the session for this event
         * @param item media item from the controller
         * @return a data source descriptor if the media item. Can be {@code null} if the content
         *        isn't available.
         */
        @Nullable DataSourceDesc onDataSourceMissing(@NonNull MediaSession2 session,
                @NonNull MediaItem2 item);
    }

    /**
     * Callback to be called for all incoming commands from {@link MediaController2}s.
     * <p>
     * If it's not set, the session will accept all controllers and all incoming commands by
     * default.
     */
    public abstract static class SessionCallback {
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
            SessionCommandGroup2 commands = new SessionCommandGroup2();
            commands.addAllPredefinedCommands();
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
         *  <li> {@link MediaPlayerInterface} </li>
         *  <li> {@link MediaPlaylistAgent} </li>
         *  <li> {@link android.media.AudioManager} or {@link VolumeProviderCompat} </li>
         * </ul>
         * Return {@code false} here to reject the request and stop sending command.
         *
         * @param session the session for this event
         * @param controller controller information.
         * @param command a command. This method will be called for every single command.
         * @return {@code true} if you want to accept incoming command. {@code false} otherwise.
         * @see SessionCommand2#COMMAND_CODE_PLAYBACK_PLAY
         * @see SessionCommand2#COMMAND_CODE_PLAYBACK_PAUSE
         * @see SessionCommand2#COMMAND_CODE_PLAYBACK_RESET
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SKIP_TO_NEXT_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SKIP_TO_PREV_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYBACK_PREPARE
         * @see SessionCommand2#COMMAND_CODE_PLAYBACK_SEEK_TO
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SKIP_TO_PLAYLIST_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SET_SHUFFLE_MODE
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SET_REPEAT_MODE
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_ADD_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_REMOVE_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_REPLACE_ITEM
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SET_LIST
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST_METADATA
         * @see SessionCommand2#COMMAND_CODE_PLAYLIST_SET_LIST_METADATA
         * @see SessionCommand2#COMMAND_CODE_VOLUME_SET_VOLUME
         * @see SessionCommand2#COMMAND_CODE_VOLUME_ADJUST_VOLUME
         */
        public boolean onCommandRequest(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull SessionCommand2 command) {
            return true;
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
         * Called when a controller requested to prepare for playing a specific mediaId through
         * {@link MediaController2#prepareFromMediaId(String, Bundle)}.
         * <p>
         * During the preparation, a session should not hold audio focus in order to allow other
         * sessions play seamlessly. The state of playback should be updated to
         * {@link MediaPlayerInterface#PLAYER_STATE_PAUSED} after the preparation is done.
         * <p>
         * The playback of the prepared content should start in the later calls of
         * {@link MediaSession2#play()}.
         * <p>
         * Override {@link #onPlayFromMediaId} to handle requests for starting
         * playback without preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param mediaId media id to prepare
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID
         */
        public void onPrepareFromMediaId(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String mediaId,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to prepare playback from a search query through
         * {@link MediaController2#prepareFromSearch(String, Bundle)}.
         * <p>
         * An empty query indicates that the app may prepare any music. The implementation should
         * attempt to make a smart choice about what to play.
         * <p>
         * The state of playback should be updated to
         * {@link MediaPlayerInterface#PLAYER_STATE_PAUSED} after the preparation is done.
         * The playback of the prepared content should start in the
         * later calls of {@link MediaSession2#play()}.
         * <p>
         * Override {@link #onPlayFromSearch} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param query query string. Can be empty to indicate any suggested media
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH
         */
        public void onPrepareFromSearch(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull String query,
                @Nullable Bundle extras) { }

        /**
         * Called when a controller requested to prepare a specific media item represented by a URI
         * through {@link MediaController2#prepareFromUri(Uri, Bundle)}.
         * <p>
         * During the preparation, a session should not hold audio focus in order to allow
         * other sessions play seamlessly. The state of playback should be updated to
         * {@link MediaPlayerInterface#PLAYER_STATE_PAUSED} after the preparation is done.
         * <p>
         * The playback of the prepared content should start in the later calls of
         * {@link MediaSession2#play()}.
         * <p>
         * Override {@link #onPlayFromUri} to handle requests for starting playback without
         * preparation.
         *
         * @param session the session for this event
         * @param controller controller information
         * @param uri uri
         * @param extras optional extra bundle
         * @see SessionCommand2#COMMAND_CODE_SESSION_PREPARE_FROM_URI
         */
        public void onPrepareFromUri(@NonNull MediaSession2 session,
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
         */
        public void onSubscribeRoutesInfo(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#unsubscribeRoutesInfo()}
         *
         * @param session the session for this event
         * @param controller controller information
         * @see SessionCommand2#COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO
         */
        public void onUnsubscribeRoutesInfo(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller) { }

        /**
         * Called when a controller called {@link MediaController2#selectRoute(Bundle)}.
         * @param session the session for this event
         * @param controller controller information
         * @param route The route bundle which may be from MediaRouteDescritor.asBundle().
         * @see SessionCommand2#COMMAND_CODE_SESSION_SELECT_ROUTE
         */
        public void onSelectRoute(@NonNull MediaSession2 session,
                @NonNull ControllerInfo controller, @NonNull Bundle route) { }
        /**
         * Called when the player's current playing item is changed
         * <p>
         * When it's called, you should invalidate previous playback information and wait for later
         * callbacks.
         *
         * @param session the controller for this event
         * @param player the player for this event
         * @param item new item
         */
        public void onCurrentMediaItemChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, @Nullable MediaItem2 item) { }

        /**
         * Called when the player is <i>prepared</i>, i.e. it is ready to play the content
         * referenced by the given data source.
         * @param session the session for this event
         * @param player the player for this event
         * @param item the media item for which buffering is happening
         */
        public void onMediaPrepared(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, @NonNull MediaItem2 item) { }

        /**
         * Called to indicate that the state of the player has changed.
         * See {@link MediaPlayerInterface#getPlayerState()} for polling the player state.
         * @param session the session for this event
         * @param player the player for this event
         * @param state the new state of the player.
         */
        public void onPlayerStateChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, @PlayerState int state) { }

        /**
         * Called to report buffering events for a data source.
         *
         * @param session the session for this event
         * @param player the player for this event
         * @param item the media item for which buffering is happening.
         * @param state the new buffering state.
         */
        public void onBufferingStateChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, @NonNull MediaItem2 item,
                @BuffState int state) { }

        /**
         * Called to indicate that the playback speed has changed.
         * @param session the session for this event
         * @param player the player for this event
         * @param speed the new playback speed.
         */
        public void onPlaybackSpeedChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, float speed) { }

        /**
         * Called to indicate that {@link #seekTo(long)} is completed.
         *
         * @param session the session for this event.
         * @param player the player that has completed seeking.
         * @param position the previous seeking request.
         * @see #seekTo(long)
         */
        public void onSeekCompleted(@NonNull MediaSession2 session,
                @NonNull MediaPlayerInterface player, long position) { }

        /**
         * Called when a playlist is changed from the {@link MediaPlaylistAgent}.
         * <p>
         * This is called when the underlying agent has called
         * {@link PlaylistEventCallback#onPlaylistChanged(MediaPlaylistAgent,
         * List, MediaMetadata2)}.
         *
         * @param session the session for this event
         * @param playlistAgent playlist agent for this event
         * @param list new playlist
         * @param metadata new metadata
         */
        public void onPlaylistChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlaylistAgent playlistAgent, @NonNull List<MediaItem2> list,
                @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when a playlist metadata is changed.
         *
         * @param session the session for this event
         * @param playlistAgent playlist agent for this event
         * @param metadata new metadata
         */
        public void onPlaylistMetadataChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlaylistAgent playlistAgent, @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when the shuffle mode is changed.
         *
         * @param session the session for this event
         * @param playlistAgent playlist agent for this event
         * @param shuffleMode repeat mode
         * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
         * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
         * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlaylistAgent playlistAgent,
                @MediaPlaylistAgent.ShuffleMode int shuffleMode) { }

        /**
         * Called when the repeat mode is changed.
         *
         * @param session the session for this event
         * @param playlistAgent playlist agent for this event
         * @param repeatMode repeat mode
         * @see MediaPlaylistAgent#REPEAT_MODE_NONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ALL
         * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaSession2 session,
                @NonNull MediaPlaylistAgent playlistAgent,
                @MediaPlaylistAgent.RepeatMode int repeatMode) { }
    }

    /**
     * Builder for {@link MediaSession2}.
     * <p>
     * Any incoming event from the {@link MediaController2} will be handled on the thread
     * that created session with the {@link Builder#build()}.
     */
    public static final class Builder extends BuilderBase<MediaSession2, Builder, SessionCallback> {
        private MediaSession2ImplBase.Builder mImpl;

        public Builder(Context context) {
            super(context);
            mImpl = new MediaSession2ImplBase.Builder(context);
            setImpl(mImpl);
        }

        @Override
        public @NonNull Builder setPlayer(@NonNull MediaPlayerInterface player) {
            return super.setPlayer(player);
        }

        @Override
        public @NonNull Builder setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
            return super.setPlaylistAgent(playlistAgent);
        }

        @Override
        public @NonNull Builder setVolumeProvider(@Nullable VolumeProviderCompat volumeProvider) {
            return super.setVolumeProvider(volumeProvider);
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
            return super.build();
        }
    }

    /**
     * Information of a controller.
     */
    public static final class ControllerInfo {
        private final int mUid;
        private final String mPackageName;
        private final boolean mIsTrusted;
        private final ControllerCb mControllerCb;

        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        ControllerInfo(@NonNull String packageName, int pid, int uid, @NonNull ControllerCb cb) {
            mUid = uid;
            mPackageName = packageName;
            mIsTrusted = false;
            mControllerCb = cb;
        }

        /**
         * @return package name of the controller
         */
        public @NonNull String getPackageName() {
            return mPackageName;
        }

        /**
         * @return uid of the controller
         */
        public int getUid() {
            return mUid;
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
            return mControllerCb.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerInfo)) {
                return false;
            }
            ControllerInfo other = (ControllerInfo) obj;
            return mControllerCb.equals(other.mControllerCb);
        }

        @Override
        public String toString() {
            return "ControllerInfo {pkg=" + mPackageName + ", uid=" + mUid + "})";
        }

        @NonNull IBinder getId() {
            return mControllerCb.getId();
        }

        @NonNull ControllerCb getControllerCb() {
            return mControllerCb;
        }
    }

    /**
     * Button for a {@link SessionCommand2} that will be shown by the controller.
     * <p>
     * It's up to the controller's decision to respect or ignore this customization request.
     */
    public static final class CommandButton {
        private static final String KEY_COMMAND =
                "android.media.media_session2.command_button.command";
        private static final String KEY_ICON_RES_ID =
                "android.media.media_session2.command_button.icon_res_id";
        private static final String KEY_DISPLAY_NAME =
                "android.media.media_session2.command_button.display_name";
        private static final String KEY_EXTRAS =
                "android.media.media_session2.command_button.extras";
        private static final String KEY_ENABLED =
                "android.media.media_session2.command_button.enabled";

        private SessionCommand2 mCommand;
        private int mIconResId;
        private String mDisplayName;
        private Bundle mExtras;
        private boolean mEnabled;

        private CommandButton(@Nullable SessionCommand2 command, int iconResId,
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
        @Override
        public int hashCode() {
            return getId().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControllerCb)) {
                return false;
            }
            ControllerCb other = (ControllerCb) obj;
            return getId().equals(other.getId());
        }

        abstract @NonNull IBinder getId();

        // Mostly matched with the methods in MediaController2.ControllerCallback
        abstract void onCustomLayoutChanged(@NonNull List<CommandButton> layout)
                throws RemoteException;
        abstract void onPlaybackInfoChanged(@NonNull PlaybackInfo info) throws RemoteException;
        abstract void onAllowedCommandsChanged(@NonNull SessionCommandGroup2 commands)
                throws RemoteException;
        abstract void onCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver) throws RemoteException;
        abstract void onPlayerStateChanged(int playerState) throws RemoteException;
        abstract void onPlaybackSpeedChanged(float speed) throws RemoteException;
        abstract void onBufferingStateChanged(@NonNull MediaItem2 item,
                @MediaPlayerInterface.BuffState int state) throws RemoteException;
        abstract void onSeekCompleted(long position) throws RemoteException;
        abstract void onError(@ErrorCode int errorCode, @Nullable Bundle extras)
                throws RemoteException;
        abstract void onCurrentMediaItemChanged(@Nullable MediaItem2 item) throws RemoteException;
        abstract void onPlaylistChanged(@NonNull List<MediaItem2> playlist,
                @Nullable MediaMetadata2 metadata) throws RemoteException;
        abstract void onPlaylistMetadataChanged(@Nullable MediaMetadata2 metadata)
                throws RemoteException;
        abstract void onShuffleModeChanged(@MediaPlaylistAgent.ShuffleMode int shuffleMode)
                throws RemoteException;
        abstract void onRepeatModeChanged(@MediaPlaylistAgent.RepeatMode int repeatMode)
                throws RemoteException;
        abstract void onRoutesInfoChanged(@Nullable List<Bundle> routes) throws RemoteException;
        abstract void onChildrenChanged(@NonNull  String parentId, int itemCount,
                @Nullable Bundle extras) throws RemoteException;
        abstract void onSearchResultChanged(@NonNull String query, int itemCount,
                @Nullable Bundle extras) throws RemoteException;
    }

    abstract static class SupportLibraryImpl extends MediaInterface2.SessionPlayer
            implements AutoCloseable {
        abstract void updatePlayer(@NonNull MediaPlayerInterface player,
                @Nullable MediaPlaylistAgent playlistAgent,
                @Nullable VolumeProviderCompat volumeProvider);
        abstract @NonNull MediaPlayerInterface getPlayer();
        abstract @NonNull MediaPlaylistAgent getPlaylistAgent();
        abstract @Nullable VolumeProviderCompat getVolumeProvider();
        abstract @NonNull SessionToken2 getToken();
        abstract @NonNull List<ControllerInfo> getConnectedControllers();

        abstract void setAudioFocusRequest(@Nullable AudioFocusRequest afr);
        abstract void setCustomLayout(@NonNull ControllerInfo controller,
                @NonNull List<CommandButton> layout);
        abstract void setAllowedCommands(@NonNull ControllerInfo controller,
                @NonNull SessionCommandGroup2 commands);
        abstract void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args);
        abstract void sendCustomCommand(@NonNull ControllerInfo controller,
                @NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver);
        abstract void notifyRoutesInfoChanged(@NonNull ControllerInfo controller,
                @Nullable List<Bundle> routes);

        // LibrarySession methods
        abstract void notifyChildrenChanged(@NonNull ControllerInfo controller,
                @NonNull String parentId, int itemCount, @Nullable Bundle extras,
                @NonNull List<MediaSessionManager.RemoteUserInfo> subscribingBrowsers);
        abstract void notifySearchResultChanged(@NonNull ControllerInfo controller,
                @NonNull String query, int itemCount, @Nullable Bundle extras);

        // Internally used methods
        abstract MediaSession2 createInstance();
        abstract MediaSession2 getInstance();
        abstract MediaSessionCompat getSessionCompat();
        abstract Context getContext();
        abstract Executor getCallbackExecutor();
        abstract SessionCallback getCallback();
        abstract boolean isClosed();
        abstract PlaybackStateCompat getPlaybackStateCompat();
        abstract PlaybackInfo getPlaybackInfo();
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
        MediaSession2ImplBase.BuilderBase<T, C> mBaseImpl;
        MediaPlayerInterface mPlayer;
        String mId;
        Executor mCallbackExecutor;
        C mCallback;
        MediaPlaylistAgent mPlaylistAgent;
        VolumeProviderCompat mVolumeProvider;
        PendingIntent mSessionActivity;

        BuilderBase(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("context shouldn't be null");
            }
            mContext = context;
            // Ensure non-null
            mId = "";
        }

        /**
         * Sets the underlying {@link MediaPlayerInterface} for this session to dispatch incoming
         * event to.
         *
         * @param player a {@link MediaPlayerInterface} that handles actual media playback in your
         *               app.
         */
        @NonNull U setPlayer(@NonNull MediaPlayerInterface player) {
            if (player == null) {
                throw new IllegalArgumentException("player shouldn't be null");
            }
            mBaseImpl.setPlayer(player);
            return (U) this;
        }

        /**
         * Sets the {@link MediaPlaylistAgent} for this session to manages playlist of the
         * underlying {@link MediaPlayerInterface}. The playlist agent should manage
         * {@link MediaPlayerInterface} for calling
         * {@link MediaPlayerInterface#setNextDataSources(List)}.
         * <p>
         * If the {@link MediaPlaylistAgent} isn't set, session will create the default playlist
         * agent.
         *
         * @param playlistAgent a {@link MediaPlaylistAgent} that manages playlist of the
         *                      {@code player}
         */
        U setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
            if (playlistAgent == null) {
                throw new IllegalArgumentException("playlistAgent shouldn't be null");
            }
            mBaseImpl.setPlaylistAgent(playlistAgent);
            return (U) this;
        }

        /**
         * Sets the {@link VolumeProviderCompat} for this session to handle volume events. If not
         * set, system will adjust the appropriate stream volume for this session's player.
         *
         * @param volumeProvider The provider that will receive volume button events.
         */
        @NonNull U setVolumeProvider(@Nullable VolumeProviderCompat volumeProvider) {
            mBaseImpl.setVolumeProvider(volumeProvider);
            return (U) this;
        }

        /**
         * Set an intent for launching UI for this Session. This can be used as a
         * quick link to an ongoing media screen. The intent should be for an
         * activity that may be started using {@link Context#startActivity(Intent)}.
         *
         * @param pi The intent to launch to show UI for this session.
         */
        @NonNull U setSessionActivity(@Nullable PendingIntent pi) {
            mBaseImpl.setSessionActivity(pi);
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
            mBaseImpl.setId(id);
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
            mBaseImpl.setSessionCallback(executor, callback);
            return (U) this;
        }

        /**
         * Build {@link MediaSession2}.
         *
         * @return a new session
         * @throws IllegalStateException if the session with the same id is already exists for the
         *      package.
         */
        @NonNull T build() {
            return mBaseImpl.build();
        }

        void setImpl(MediaSession2ImplBase.BuilderBase<T, C> impl) {
            mBaseImpl = impl;
        }
    }
}
