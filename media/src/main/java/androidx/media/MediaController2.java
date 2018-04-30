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
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media.MediaPlaylistAgent.RepeatMode;
import androidx.media.MediaPlaylistAgent.ShuffleMode;
import androidx.media.MediaSession2.CommandButton;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.MediaSession2.ErrorCode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Allows an app to interact with an active {@link MediaSession2} or a
 * {@link MediaSessionService2} in any status. Media buttons and other commands can be sent to
 * the session.
 * <p>
 * When you're done, use {@link #close()} to clean up resources. This also helps session service
 * to be destroyed when there's no controller associated with it.
 * <p>
 * When controlling {@link MediaSession2}, the controller will be available immediately after
 * the creation.
 * <p>
 * When controlling {@link MediaSessionService2}, the {@link MediaController2} would be
 * available only if the session service allows this controller by
 * {@link MediaSession2.SessionCallback#onConnect(MediaSession2, ControllerInfo)} for the service.
 * Wait {@link ControllerCallback#onConnected(MediaController2, SessionCommandGroup2)} or
 * {@link ControllerCallback#onDisconnected(MediaController2)} for the result.
 * <p>
 * MediaController2 objects are thread-safe.
 * <p>
 * @see MediaSession2
 * @see MediaSessionService2
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
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

    private final SupportLibraryImpl mImpl;
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
    public MediaController2(@NonNull Context context, @NonNull SessionToken2 token,
            @NonNull Executor executor, @NonNull ControllerCallback callback) {
        mImpl = new MediaController2ImplBase(context, token, executor, callback);
        mImpl.setInstance(this);
    }

    /**
     * Release this object, and disconnect from the session. After this, callbacks wouldn't be
     * received.
     */
    @Override
    public void close() {
        try {
            mImpl.close();
        } catch (Exception e) {
            // Should not be here.
        }
    }

    /**
     * @return token
     */
    public @NonNull SessionToken2 getSessionToken() {
        return mImpl.getSessionToken();
    }

    /**
     * Returns whether this class is connected to active {@link MediaSession2} or not.
     */
    public boolean isConnected() {
        return mImpl.isConnected();
    }

    /**
     * Requests that the player starts or resumes playback.
     */
    public void play() {
        mImpl.play();
    }

    /**
     * Requests that the player pauses playback.
     */
    public void pause() {
        mImpl.pause();
    }

    /**
     * Requests that the player be reset to its uninitialized state.
     */
    public void reset() {
        mImpl.reset();
    }

    /**
     * Request that the player prepare its playback. In other words, other sessions can continue
     * to play during the preparation of this session. This method can be used to speed up the
     * start of the playback. Once the preparation is done, the session will change its playback
     * state to {@link MediaPlayerInterface#PLAYER_STATE_PAUSED}. Afterwards, {@link #play} can be
     * called to start playback.
     */
    public void prepare() {
        mImpl.prepare();
    }

    /**
     * Start fast forwarding. If playback is already fast forwarding this
     * may increase the rate.
     */
    public void fastForward() {
        mImpl.fastForward();
    }

    /**
     * Start rewinding. If playback is already rewinding this may increase
     * the rate.
     */
    public void rewind() {
        mImpl.rewind();
    }

    /**
     * Move to a new location in the media stream.
     *
     * @param pos Position to move to, in milliseconds.
     */
    public void seekTo(long pos) {
        mImpl.seekTo(pos);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipForward() {
        // To match with KEYCODE_MEDIA_SKIP_FORWARD
        mImpl.skipForward();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void skipBackward() {
        // To match with KEYCODE_MEDIA_SKIP_BACKWARD
        mImpl.skipBackward();
    }

    /**
     * Request that the player start playback for a specific media id.
     *
     * @param mediaId The id of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     */
    public void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        mImpl.playFromMediaId(mediaId, extras);
    }

    /**
     * Request that the player start playback for a specific search query.
     *
     * @param query The search query. Should not be an empty string.
     * @param extras Optional extras that can include extra information about the query.
     */
    public void playFromSearch(@NonNull String query, @Nullable Bundle extras) {
        mImpl.playFromSearch(query, extras);
    }

    /**
     * Request that the player start playback for a specific {@link Uri}.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be played.
     */
    public void playFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        mImpl.playFromUri(uri, extras);
    }

    /**
     * Request that the player prepare playback for a specific media id. In other words, other
     * sessions can continue to play during the preparation of this session. This method can be
     * used to speed up the start of the playback. Once the preparation is done, the session
     * will change its playback state to {@link MediaPlayerInterface#PLAYER_STATE_PAUSED}.
     * Afterwards, {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromMediaId} can be directly called without this method.
     *
     * @param mediaId The id of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     */
    public void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras) {
        mImpl.prepareFromMediaId(mediaId, extras);
    }

    /**
     * Request that the player prepare playback for a specific search query.
     * In other words, other sessions can continue to play during the preparation of this session.
     * This method can be used to speed up the start of the playback.
     * Once the preparation is done, the session will change its playback state to
     * {@link MediaPlayerInterface#PLAYER_STATE_PAUSED}. Afterwards,
     * {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromSearch} can be directly called without this method.
     *
     * @param query The search query. Should not be an empty string.
     * @param extras Optional extras that can include extra information about the query.
     */
    public void prepareFromSearch(@NonNull String query, @Nullable Bundle extras) {
        mImpl.prepareFromSearch(query, extras);
    }

    /**
     * Request that the player prepare playback for a specific {@link Uri}. In other words,
     * other sessions can continue to play during the preparation of this session. This method
     * can be used to speed up the start of the playback. Once the preparation is done, the
     * session will change its playback state to {@link MediaPlayerInterface#PLAYER_STATE_PAUSED}.
     * Afterwards, {@link #play} can be called to start playback. If the preparation is not needed,
     * {@link #playFromUri} can be directly called without this method.
     *
     * @param uri The URI of the requested media.
     * @param extras Optional extras that can include extra information about the media item
     *               to be prepared.
     */
    public void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras) {
        mImpl.prepareFromUri(uri, extras);
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
    public void setVolumeTo(int value, @VolumeFlags int flags) {
        mImpl.setVolumeTo(value, flags);
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
    public void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags) {
        mImpl.adjustVolume(direction, flags);
    }

    /**
     * Get an intent for launching UI associated with this session if one exists.
     *
     * @return A {@link PendingIntent} to launch UI or null.
     */
    public @Nullable PendingIntent getSessionActivity() {
        return mImpl.getSessionActivity();
    }

    /**
     * Get the lastly cached player state from
     * {@link ControllerCallback#onPlayerStateChanged(MediaController2, int)}.
     *
     * @return player state
     */
    public int getPlayerState() {
        return mImpl.getPlayerState();
    }

    /**
     * Gets the duration of the current media item, or {@link MediaPlayerInterface#UNKNOWN_TIME} if
     * unknown.
     * @return the duration in ms, or {@link MediaPlayerInterface#UNKNOWN_TIME}.
     */
    public long getDuration() {
        return mImpl.getDuration();
    }

    /**
     * Gets the current playback position.
     * <p>
     * This returns the calculated value of the position, based on the difference between the
     * update time and current time.
     *
     * @return position
     */
    public long getCurrentPosition() {
        return mImpl.getCurrentPosition();
    }

    /**
     * Get the lastly cached playback speed from
     * {@link ControllerCallback#onPlaybackSpeedChanged(MediaController2, float)}.
     *
     * @return speed the lastly cached playback speed, or 0.0f if unknown.
     */
    public float getPlaybackSpeed() {
        return mImpl.getPlaybackSpeed();
    }

    /**
     * Set the playback speed.
     */
    public void setPlaybackSpeed(float speed) {
        mImpl.setPlaybackSpeed(speed);
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     * @return the buffering state.
     */
    public @MediaPlayerInterface.BuffState int getBufferingState() {
        return mImpl.getBufferingState();
    }

    /**
     * Gets the lastly cached buffered position from the session when
     * {@link ControllerCallback#onBufferingStateChanged(MediaController2, MediaItem2, int)} is
     * called.
     *
     * @return buffering position in millis, or {@link MediaPlayerInterface#UNKNOWN_TIME} if
     * unknown.
     */
    public long getBufferedPosition() {
        return mImpl.getBufferedPosition();
    }

    /**
     * Get the current playback info for this session.
     *
     * @return The current playback info or null.
     */
    public @Nullable PlaybackInfo getPlaybackInfo() {
        return mImpl.getPlaybackInfo();
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
     * @param mediaId The id of the media
     * @param rating The rating to set
     */
    public void setRating(@NonNull String mediaId, @NonNull Rating2 rating) {
        mImpl.setRating(mediaId, rating);
    }

    /**
     * Send custom command to the session
     *
     * @param command custom command
     * @param args optional argument
     * @param cb optional result receiver
     */
    public void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
            @Nullable ResultReceiver cb) {
        mImpl.sendCustomCommand(command, args, cb);
    }

    /**
     * Returns the cached playlist from {@link ControllerCallback#onPlaylistChanged}.
     * <p>
     * This list may differ with the list that was specified with
     * {@link #setPlaylist(List, MediaMetadata2)} depending on the {@link MediaPlaylistAgent}
     * implementation. Use media items returned here for other playlist agent APIs such as
     * {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)}.
     *
     * @return playlist. Can be {@code null} if the playlist hasn't set nor controller doesn't have
     *      enough permission.
     * @see SessionCommand2#COMMAND_CODE_PLAYLIST_GET_LIST
     */
    public @Nullable List<MediaItem2> getPlaylist() {
        return mImpl.getPlaylist();
    }

    /**
     * Sets the playlist.
     * <p>
     * Even when the playlist is successfully set, use the playlist returned from
     * {@link #getPlaylist()} for playlist APIs such as {@link #skipToPlaylistItem(MediaItem2)}.
     * Otherwise the session in the remote process can't distinguish between media items.
     *
     * @param list playlist
     * @param metadata metadata of the playlist
     * @see #getPlaylist()
     * @see ControllerCallback#onPlaylistChanged
     */
    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        mImpl.setPlaylist(list, metadata);
    }

    /**
     * Updates the playlist metadata
     *
     * @param metadata metadata of the playlist
     */
    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        mImpl.updatePlaylistMetadata(metadata);
    }

    /**
     * Gets the lastly cached playlist playlist metadata either from
     * {@link ControllerCallback#onPlaylistMetadataChanged or
     * {@link ControllerCallback#onPlaylistChanged}.
     *
     * @return metadata metadata of the playlist, or null if none is set
     */
    public @Nullable MediaMetadata2 getPlaylistMetadata() {
        return mImpl.getPlaylistMetadata();
    }

    /**
     * Adds the media item to the playlist at position index. Index equals or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * This will not change the currently playing media item.
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist will be incremented correspondingly.
     *
     * @param index the index you want to add
     * @param item the media item you want to add
     */
    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        mImpl.addPlaylistItem(index, item);
    }

    /**
     * Removes the media item at index in the playlist.
     *<p>
     * If the item is the currently playing item of the playlist, current playback
     * will be stopped and playback moves to next source in the list.
     *
     * @param item the media item you want to add
     */
    public void removePlaylistItem(@NonNull MediaItem2 item) {
        mImpl.removePlaylistItem(item);
    }

    /**
     * Replace the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     *
     * @param index the index of the item to replace
     * @param item the new item
     */
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        mImpl.replacePlaylistItem(index, item);
    }

    /**
     * Get the lastly cached current item from
     * {@link ControllerCallback#onCurrentMediaItemChanged(MediaController2, MediaItem2)}.
     *
     * @return the currently playing item, or null if unknown.
     */
    public MediaItem2 getCurrentMediaItem() {
        return mImpl.getCurrentMediaItem();
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPreviousItem()}.
     */
    public void skipToPreviousItem() {
        mImpl.skipToPreviousItem();
    }

    /**
     * Skips to the next item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToNextItem()}.
     */
    public void skipToNextItem() {
        mImpl.skipToNextItem();
    }

    /**
     * Skips to the item in the playlist.
     * <p>
     * This calls {@link MediaPlaylistAgent#skipToPlaylistItem(MediaItem2)}.
     *
     * @param item The item in the playlist you want to play
     */
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        mImpl.skipToPlaylistItem(item);
    }

    /**
     * Gets the cached repeat mode from the {@link ControllerCallback#onRepeatModeChanged}.
     *
     * @return repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    public @RepeatMode int getRepeatMode() {
        return mImpl.getRepeatMode();
    }

    /**
     * Sets the repeat mode.
     *
     * @param repeatMode repeat mode
     * @see MediaPlaylistAgent#REPEAT_MODE_NONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ONE
     * @see MediaPlaylistAgent#REPEAT_MODE_ALL
     * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
     */
    public void setRepeatMode(@RepeatMode int repeatMode) {
        mImpl.setRepeatMode(repeatMode);
    }

    /**
     * Gets the cached shuffle mode from the {@link ControllerCallback#onShuffleModeChanged}.
     *
     * @return The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    public @ShuffleMode int getShuffleMode() {
        return mImpl.getShuffleMode();
    }

    /**
     * Sets the shuffle mode.
     *
     * @param shuffleMode The shuffle mode
     * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
     * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
     * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
     */
    public void setShuffleMode(@ShuffleMode int shuffleMode) {
        mImpl.setShuffleMode(shuffleMode);
    }

    /**
     * Queries for information about the routes currently known.
     */
    public void subscribeRoutesInfo() {
        mImpl.subscribeRoutesInfo();
    }

    /**
     * Unsubscribes for changes to the routes.
     * <p>
     * The {@link ControllerCallback#onRoutesInfoChanged callback} will no longer be invoked for
     * the routes once this method returns.
     * </p>
     */
    public void unsubscribeRoutesInfo() {
        mImpl.unsubscribeRoutesInfo();
    }

    /**
     * Selects the specified route.
     *
     * @param route The route to select.
     */
    public void selectRoute(@NonNull Bundle route) {
        mImpl.selectRoute(route);
    }

    @NonNull Context getContext() {
        return mImpl.getContext();
    }

    @NonNull ControllerCallback getCallback() {
        return mImpl.getCallback();
    }

    @NonNull Executor getCallbackExecutor() {
        return mImpl.getCallbackExecutor();
    }

    @Nullable MediaBrowserCompat getBrowserCompat() {
        return mImpl.getBrowserCompat();
    }

    /**
     * Sets the time diff forcefully when calculating current position.
     * @param timeDiff {@code null} for reset.
     */
    @VisibleForTesting
    void setTimeDiff(Long timeDiff) {
        mTimeDiff = timeDiff;
    }

    interface SupportLibraryImpl extends AutoCloseable {
        void setInstance(MediaController2 controller);
        SessionToken2 getSessionToken();
        boolean isConnected();
        void play();
        void pause();
        void reset();
        void prepare();
        void fastForward();
        void rewind();
        void seekTo(long pos);
        void skipForward();
        void skipBackward();
        void playFromMediaId(@NonNull String mediaId, @Nullable Bundle extras);
        void playFromSearch(@NonNull String query, @Nullable Bundle extras);
        void playFromUri(@NonNull Uri uri, @Nullable Bundle extras);
        void prepareFromMediaId(@NonNull String mediaId, @Nullable Bundle extras);
        void prepareFromSearch(@NonNull String query, @Nullable Bundle extras);
        void prepareFromUri(@NonNull Uri uri, @Nullable Bundle extras);
        void setVolumeTo(int value, @VolumeFlags int flags);
        void adjustVolume(@VolumeDirection int direction, @VolumeFlags int flags);
        @Nullable PendingIntent getSessionActivity();
        int getPlayerState();
        long getDuration();
        long getCurrentPosition();
        float getPlaybackSpeed();
        void setPlaybackSpeed(float speed);
        @MediaPlayerInterface.BuffState int getBufferingState();
        long getBufferedPosition();
        @Nullable PlaybackInfo getPlaybackInfo();
        void setRating(@NonNull String mediaId, @NonNull Rating2 rating);
        void sendCustomCommand(@NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver cb);
        @Nullable List<MediaItem2> getPlaylist();
        void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata);
        void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata);
        @Nullable MediaMetadata2 getPlaylistMetadata();
        void addPlaylistItem(int index, @NonNull MediaItem2 item);
        void removePlaylistItem(@NonNull MediaItem2 item);
        void replacePlaylistItem(int index, @NonNull MediaItem2 item);
        MediaItem2 getCurrentMediaItem();
        void skipToPreviousItem();
        void skipToNextItem();
        void skipToPlaylistItem(@NonNull MediaItem2 item);
        @RepeatMode int getRepeatMode();
        void setRepeatMode(@RepeatMode int repeatMode);
        @ShuffleMode int getShuffleMode();
        void setShuffleMode(@ShuffleMode int shuffleMode);
        void subscribeRoutesInfo();
        void unsubscribeRoutesInfo();
        void selectRoute(@NonNull Bundle route);

        // For MediaBrowser2
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
         *
         * @param controller the controller for this event
         * @param layout
         */
        public void onCustomLayoutChanged(@NonNull MediaController2 controller,
                @NonNull List<CommandButton> layout) { }

        /**
         * Called when the session has changed anything related with the {@link PlaybackInfo}.
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
         * Called when the session sent a custom command.
         *
         * @param controller the controller for this event
         * @param command
         * @param args
         * @param receiver
         */
        public void onCustomCommand(@NonNull MediaController2 controller,
                @NonNull SessionCommand2 command, @Nullable Bundle args,
                @Nullable ResultReceiver receiver) { }

        /**
         * Called when the player state is changed.
         *
         * @param controller the controller for this event
         * @param state
         */
        public void onPlayerStateChanged(@NonNull MediaController2 controller, int state) { }

        /**
         * Called when playback speed is changed.
         *
         * @param controller the controller for this event
         * @param speed speed
         */
        public void onPlaybackSpeedChanged(@NonNull MediaController2 controller,
                float speed) { }

        /**
         * Called to report buffering events for a data source.
         * <p>
         * Use {@link #getBufferedPosition()} for current buffering position.
         *
         * @param controller the controller for this event
         * @param item the media item for which buffering is happening.
         * @param state the new buffering state.
         */
        public void onBufferingStateChanged(@NonNull MediaController2 controller,
                @NonNull MediaItem2 item, @MediaPlayerInterface.BuffState int state) { }

        /**
         * Called to indicate that seeking is completed.
         *
         * @param controller the controller for this event.
         * @param position the previous seeking request.
         */
        public void onSeekCompleted(@NonNull MediaController2 controller, long position) { }

        /**
         * Called when a error from
         *
         * @param controller the controller for this event
         * @param errorCode error code
         * @param extras extra information
         */
        public void onError(@NonNull MediaController2 controller, @ErrorCode int errorCode,
                @Nullable Bundle extras) { }

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
                @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) { }

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
         * @see MediaPlaylistAgent#SHUFFLE_MODE_NONE
         * @see MediaPlaylistAgent#SHUFFLE_MODE_ALL
         * @see MediaPlaylistAgent#SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaController2 controller,
                @MediaPlaylistAgent.ShuffleMode int shuffleMode) { }

        /**
         * Called when the repeat mode is changed.
         *
         * @param controller the controller for this event
         * @param repeatMode repeat mode
         * @see MediaPlaylistAgent#REPEAT_MODE_NONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ONE
         * @see MediaPlaylistAgent#REPEAT_MODE_ALL
         * @see MediaPlaylistAgent#REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaController2 controller,
                @MediaPlaylistAgent.RepeatMode int repeatMode) { }

        /**
         * Called when a property of the indicated media route has changed.
         *
         * @param controller the controller for this event
         * @param routes The list of Bundle from MediaRouteDescriptor.asBundle().
         *              See MediaRouteDescriptor.fromBundle(Bundle bundle) to get
         *              MediaRouteDescriptor object from the {@code routes}
         */
        public void onRoutesInfoChanged(@NonNull MediaController2 controller,
                @Nullable List<Bundle> routes) { }
    }

    /**
     * Holds information about the the way volume is handled for this session.
     */
    // The same as MediaController.PlaybackInfo
    public static final class PlaybackInfo {
        private static final String KEY_PLAYBACK_TYPE = "android.media.audio_info.playback_type";
        private static final String KEY_CONTROL_TYPE = "android.media.audio_info.control_type";
        private static final String KEY_MAX_VOLUME = "android.media.audio_info.max_volume";
        private static final String KEY_CURRENT_VOLUME = "android.media.audio_info.current_volume";
        private static final String KEY_AUDIO_ATTRIBUTES = "android.media.audio_info.audio_attrs";

        private final int mPlaybackType;
        private final int mControlType;
        private final int mMaxVolume;
        private final int mCurrentVolume;
        private final AudioAttributesCompat mAudioAttrsCompat;

        /**
         * The session uses remote playback.
         */
        public static final int PLAYBACK_TYPE_REMOTE = 2;
        /**
         * The session uses local playback.
         */
        public static final int PLAYBACK_TYPE_LOCAL = 1;

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
         * @return The type of playback this session is using.
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
         * @return The attributes for this session.
         */
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
         * @return The type of volume control that may be used with this session.
         */
        public int getControlType() {
            return mControlType;
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

        Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_PLAYBACK_TYPE, mPlaybackType);
            bundle.putInt(KEY_CONTROL_TYPE, mControlType);
            bundle.putInt(KEY_MAX_VOLUME, mMaxVolume);
            bundle.putInt(KEY_CURRENT_VOLUME, mCurrentVolume);
            if (mAudioAttrsCompat != null) {
                bundle.putBundle(KEY_AUDIO_ATTRIBUTES, mAudioAttrsCompat.toBundle());
            }
            return bundle;
        }

        static PlaybackInfo createPlaybackInfo(int playbackType, AudioAttributesCompat attrs,
                int controlType, int max, int current) {
            return new PlaybackInfo(playbackType, attrs, controlType, max, current);
        }

        static PlaybackInfo fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            final int volumeType = bundle.getInt(KEY_PLAYBACK_TYPE);
            final int volumeControl = bundle.getInt(KEY_CONTROL_TYPE);
            final int maxVolume = bundle.getInt(KEY_MAX_VOLUME);
            final int currentVolume = bundle.getInt(KEY_CURRENT_VOLUME);
            final AudioAttributesCompat attrs = AudioAttributesCompat.fromBundle(
                    bundle.getBundle(KEY_AUDIO_ATTRIBUTES));
            return createPlaybackInfo(volumeType, attrs, volumeControl, maxVolume,
                    currentVolume);
        }
    }
}
