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
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Base interface for all media players that want media session.
 * <p>
 * APIs that return {@link ListenableFuture} should be the asynchronous calls and shouldn't block
 * the calling thread. This guarantees the APIs are safe to be called on the main thread.
 *
 * <p>Topics covered here are:
 * <ol>
 * <li><a href="#BestPractices">Best practices</a>
 * <li><a href="#PlayerStates">Player states</a>
 * <li><a href="#InvalidStates">Invalid method calls</a>
 * </ol>
 *
 * <h3 id="BestPractices">Best practices</h3>
 *
 * Here are best practices when implementing/using SessionPlayer:
 *
 * <ul>
 * <li>When updating UI, you should respond to {@link PlayerCallback} invocations instead of
 * {@link PlayerResult} objects since the player can be controlled by others.
 * <li>When a SessionPlayer object is no longer being used, call {@link #close()} as soon as
 * possible to release the resources used by the internal player engine associated with the
 * SessionPlayer. For example, if a player uses hardware decoder, other player instances may
 * fallback to software decoders or fail to play. You cannot use SessionPlayer instance after
 * you call {@link #close()}. There is no way to reuse the instance.
 * <li>The current playback position can be retrieved with a call to {@link #getCurrentPosition()},
 * which is helpful for applications such as a music player that need to keep track of the playback
 * progress.
 * <li>The playback position can be adjusted with a call to {@link #seekTo(long)}. Although the
 * asynchronous {@link #seekTo} call returns right away, the actual seek operation may take a
 * while to finish, especially for audio/video being streamed.
 * <li>You can call {@link #seekTo(long)} from the {@link #PLAYER_STATE_PAUSED}. In these cases, if
 * you are playing a video stream and the requested position is valid, one video frame may be
 * displayed.
 * </ul>
 *
 * <h3 id="PlayerStates">Player states</h3>
 * The playback control of audio/video files is managed as a state machine. The SessionPlayer
 * defines four states:
 * <ol>
 *     <li>{@link #PLAYER_STATE_IDLE}: Initial state after the instantiation.
 *         <p>
 *         While in this state, you should call {@link #setMediaItem(MediaItem)} or
 *         {@link #setPlaylist(List, MediaMetadata)}. Check returned {@link ListenableFuture} for
 *         potential error.
 *         <p>
 *         Calling {@link #prepare()} transfers this object to {@link #PLAYER_STATE_PAUSED}.
 *
 *     <li>{@link #PLAYER_STATE_PAUSED}: State when the audio/video playback is paused.
 *         <p>
 *         Call {@link #play()} to resume or start playback from the position where it paused.
 *
 *     <li>{@link #PLAYER_STATE_PLAYING}: State when the player plays the media item.
 *         <p>
 *         In this state, {@link PlayerCallback#onBufferingStateChanged(
 *         SessionPlayer, MediaItem, int)} will be called regularly to tell the buffering status.
 *         <p>
 *         Playback state would remain {@link #PLAYER_STATE_PLAYING} when the currently playing
 *         media item is changed.
 *         <p>
 *         When the playback reaches the end of stream, the behavior depends on repeat mode, set by
 *         {@link #setRepeatMode(int)}. If the repeat mode was set to {@link #REPEAT_MODE_NONE},
 *         the player will transfer to the {@link #PLAYER_STATE_PAUSED}. Otherwise, the
 *         SessionPlayer object remains in the {@link #PLAYER_STATE_PLAYING} and playback will be
 *         ongoing.
 *
 *     <li>{@link #PLAYER_STATE_ERROR}: State when the playback failed and player cannot be
 *         recovered by itself.
 *         <p>
 *         In general, playback might fail due to various reasons such as unsupported audio/video
 *         format, poorly interleaved audio/video, resolution too high, streaming timeout, and
 *         others. In addition, due to programming errors, a playback control operation might be
 *         performed from an <a href="#InvalidStates">invalid state</a>. In these cases the player
 *         may transition to this state.
 * </ol>
 * <p>
 *
 * <h3 id="InvalidStates">Invalid method calls</h3>
 * The only method you safely call from the {@link #PLAYER_STATE_ERROR} is {@link #close()}.
 * Any other methods might throw an exception or return meaningless data.
 * <p>
 * Subclasses of the SessionPlayer may have extra methods that are safe to be called in the error
 * state and/or provide a method to recover from the error state. Take a look at documentations of
 * specific class that you're interested in.
 * <p>
 * Most methods can be called from any non-Error state. They will either perform their work or
 * silently have no effect. The following table lists the methods that aren't guaranteed to
 * successfully running if they're called from the associated invalid states.
 * <p>
 * <table>
 * <tr><th>Method Name</th> <th>Invalid States</th></tr>
 * <tr><td>setMediaItem</td> <td>{Paused, Playing}</td></tr>
 * <tr><td>setPlaylist</td> <td>{Paused, Playing}</td></tr>
 * <tr><td>prepare</td> <td>{Paused, Playing}</td></tr>
 * <tr><td>play</td> <td>{Idle}</td></tr>
 * <tr><td>pause</td> <td>{Idle}</td></tr>
 * <tr><td>seekTo</td> <td>{Idle}</td></tr>
 * </table>
 */
// Previously MediaSessionCompat.Callback.
// Players can extend this directly (e.g. MediaPlayer) or create wrapper and control underlying
// player.
// Preferably it can be interface, but API guideline requires to use abstract class.
@TargetApi(Build.VERSION_CODES.P)
public abstract class SessionPlayer implements AutoCloseable {
    private static final String TAG = "SessionPlayer";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            PLAYER_STATE_IDLE,
            PLAYER_STATE_PAUSED,
            PLAYER_STATE_PLAYING,
            PLAYER_STATE_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({
            BUFFERING_STATE_UNKNOWN,
            BUFFERING_STATE_BUFFERING_AND_PLAYABLE,
            BUFFERING_STATE_BUFFERING_AND_STARVED,
            BUFFERING_STATE_COMPLETE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BuffState {
    }

    /**
     * State when the player is idle, and needs configuration to start playback.
     */
    public static final int PLAYER_STATE_IDLE = 0;

    /**
     * State when the player's playback is paused
     */
    public static final int PLAYER_STATE_PAUSED = 1;

    /**
     * State when the player's playback is ongoing
     */
    public static final int PLAYER_STATE_PLAYING = 2;

    /**
     * State when the player is in error state and cannot be recovered self.
     */
    public static final int PLAYER_STATE_ERROR = 3;

    /**
     * Buffering state is unknown.
     */
    public static final int BUFFERING_STATE_UNKNOWN = 0;

    /**
     * Buffering state indicating the player is buffering but enough has been buffered
     * for this player to be able to play the content.
     * See {@link #getBufferedPosition()} for how far is buffered already.
     */
    public static final int BUFFERING_STATE_BUFFERING_AND_PLAYABLE = 1;

    /**
     * Buffering state indicating the player is buffering, but the player is currently starved
     * for data, and cannot play.
     */
    public static final int BUFFERING_STATE_BUFFERING_AND_STARVED = 2;

    /**
     * Buffering state indicating the player is done buffering, and the remainder of the content is
     * available for playback.
     */
    public static final int BUFFERING_STATE_COMPLETE = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({REPEAT_MODE_NONE, REPEAT_MODE_ONE, REPEAT_MODE_ALL,
            REPEAT_MODE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {
    }

    /**
     * Playback will be stopped at the end of the playing media list.
     */
    public static final int REPEAT_MODE_NONE = 0;

    /**
     * Playback of the current playing media item will be repeated.
     */
    public static final int REPEAT_MODE_ONE = 1;

    /**
     * Playing media list will be repeated.
     */
    public static final int REPEAT_MODE_ALL = 2;

    /**
     * Playback of the playing media group will be repeated.
     * A group is a logical block of media items which is specified in the section 5.7 of the
     * Bluetooth AVRCP 1.6. An example of a group is the playlist.
     */
    public static final int REPEAT_MODE_GROUP = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({SHUFFLE_MODE_NONE, SHUFFLE_MODE_ALL, SHUFFLE_MODE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShuffleMode {
    }

    /**
     * Media list will be played in order.
     */
    public static final int SHUFFLE_MODE_NONE = 0;

    /**
     * Media list will be played in shuffled order.
     */
    public static final int SHUFFLE_MODE_ALL = 1;

    /**
     * Media group will be played in shuffled order.
     * A group is a logical block of media items which is specified in the section 5.7 of the
     * Bluetooth AVRCP 1.6. An example of a group is the playlist.
     */
    public static final int SHUFFLE_MODE_GROUP = 2;

    public static final long UNKNOWN_TIME = -1;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final List<Pair<PlayerCallback, Executor>> mCallbacks = new ArrayList<>();

    /**
     * Plays the playback.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> play();

    /**
     * Pauses playback.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> pause();

    /**
     * Prepares the media items for playback. During this time, the player may allocate resources
     * required to play, such as audio and video decoders.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> prepare();

    /**
     * Seeks to the specified position. Moves the playback head to the specified position.
     *
     * @param position the new playback position in ms. The value should be in the range of start
     * and end positions defined in {@link MediaItem}.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> seekTo(long position);

    /**
     * Sets the playback speed. A value of {@code 1.0f} is the default playback value.
     * <p>
     * After changing the playback speed, it is recommended to query the actual speed supported
     * by the player, see {@link #getPlaybackSpeed()}.
     *
     * @param playbackSpeed playback speed
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setPlaybackSpeed(float playbackSpeed);

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the media.
     * <p>
     * You must call this method in {@link #PLAYER_STATE_IDLE} in order for the audio attributes to
     * become effective thereafter.
     *
     * @param attributes non-null <code>AudioAttributes</code>.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setAudioAttributes(
            @NonNull AudioAttributesCompat attributes);

    /**
     * Gets the current player state.
     *
     * @return the current player state
     * @see PlayerCallback#onPlayerStateChanged(SessionPlayer, int)
     * @see #PLAYER_STATE_IDLE
     * @see #PLAYER_STATE_PAUSED
     * @see #PLAYER_STATE_PLAYING
     * @see #PLAYER_STATE_ERROR
     */
    public abstract @PlayerState int getPlayerState();

    /**
     * Gets the current playback head position.
     *
     * @return the current playback position in ms, or {@link #UNKNOWN_TIME} if unknown.
     */
    public abstract long getCurrentPosition();

    /**
     * Gets the duration of the current media item, or {@link #UNKNOWN_TIME} if unknown.
     *
     * @return the duration in ms, or {@link #UNKNOWN_TIME}.
     */
    public abstract long getDuration();

    /**
     * Gets the buffered position of current playback, or {@link #UNKNOWN_TIME} if unknown.
     * @return the buffered position in ms, or {@link #UNKNOWN_TIME}.
     */
    public abstract long getBufferedPosition();

    /**
     * Returns the current buffering state of the player.
     * During the buffering, see {@link #getBufferedPosition()} for the quantifying the amount
     * already buffered.
     *
     * @return the buffering state.
     * @see #getBufferedPosition()
     */
    public abstract @BuffState int getBufferingState();

    /**
     * Gets the actual playback speed to be used by the player when playing.
     * <p>
     * Note that it may differ from the speed set in {@link #setPlaybackSpeed(float)}.
     *
     * @return the actual playback speed
     */
    public abstract float getPlaybackSpeed();

    /**
     * Sets a list of {@link MediaItem} with metadata. Ensure uniqueness of each {@link MediaItem}
     * in the playlist so the session can uniquely identity individual items. All
     * {@link MediaItem}s shouldn't be {@code null} as well.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in each {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     *
     * @param list A list of {@link MediaItem} objects to set as a play list.
     * @throws IllegalArgumentException if the given list is {@code null} or empty, or has
     *         duplicated media items.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setPlaylist(
            @NonNull List<MediaItem> list, @Nullable MediaMetadata metadata);

    /**
     * Gets the {@link AudioAttributesCompat} that media player has.
     */
    public abstract @Nullable AudioAttributesCompat getAudioAttributes();

    /**
     * Sets a {@link MediaItem} for playback.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in each {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     *
     * @param item the descriptor of media item you want to play
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * @throws IllegalArgumentException if the given item is {@code null}.
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setMediaItem(
            @NonNull MediaItem item);

    /**
     * Adds the media item to the playlist at position index. Index equals or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * The implementation may not change the currently playing media item.
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist will be increased correspondingly.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     *
     * @param index the index of the item you want to add in the playlist
     * @param item the media item you want to add
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> addPlaylistItem(int index,
            @NonNull MediaItem item);

    /**
     * Removes the media item from the playlist
     * <p>
     * The implementation may not change the currently playing media item even when it's removed.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     *
     * @param index the index of the item you want to remove in the playlist
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> removePlaylistItem(
            @IntRange(from = 0) int index);

    /**
     * Replaces the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     *
     * @param index the index of the item to replace in the playlist
     * @param item the new item
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> replacePlaylistItem(int index,
            @NonNull MediaItem item);

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     *
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> skipToPreviousPlaylistItem();

    /**
     * Skips to the next item in the playlist.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     *
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> skipToNextPlaylistItem();

    /**
     * Skips to the the media item.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     *
     * @param index The index of the item you want to play in the playlist
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> skipToPlaylistItem(
            @IntRange(from = 0) int index);

    /**
     * Updates the playlist metadata while keeping the playlist as-is.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)} when it's
     * completed.
     *
     * @param metadata metadata of the playlist
     * @see PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> updatePlaylistMetadata(
            @Nullable MediaMetadata metadata);

    /**
     * Sets the repeat mode.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onRepeatModeChanged(SessionPlayer, int)} when it's completed.
     *
     * @param repeatMode repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     * @see PlayerCallback#onRepeatModeChanged(SessionPlayer, int)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setRepeatMode(
            @RepeatMode int repeatMode);

    /**
     * Sets the shuffle mode.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onShuffleModeChanged(SessionPlayer, int)} when it's completed.
     *
     * @param shuffleMode The shuffle mode
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see PlayerCallback#onShuffleModeChanged(SessionPlayer, int)
     */
    public abstract @NonNull ListenableFuture<PlayerResult> setShuffleMode(
            @ShuffleMode int shuffleMode);

    /**
     * Gets the playlist.
     *
     * @return playlist, or null if none is set.
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    public abstract @Nullable List<MediaItem> getPlaylist();

    /**
     * Gets the playlist metadata.
     *
     * @return metadata metadata of the playlist, or null if none is set
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     * @see PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)
     */
    public abstract @Nullable MediaMetadata getPlaylistMetadata();

    /**
     * Gets the repeat mode.
     *
     * @return repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     * @see PlayerCallback#onRepeatModeChanged(SessionPlayer, int)
     */
    public abstract @RepeatMode int getRepeatMode();

    /**
     * Gets the shuffle mode.
     *
     * @return The shuffle mode
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see PlayerCallback#onShuffleModeChanged(SessionPlayer, int)
     */
    public abstract @ShuffleMode int getShuffleMode();

    /**
     * Gets the current media item.
     *
     * @return the current media item. Can be {@code null} only when media item nor playlist hasn't
     *         set.
     */
    public abstract @Nullable MediaItem getCurrentMediaItem();

    // Listeners / Callback related
    // Intentionally final not to allow developers to change the behavior
    /**
     * Register {@link PlayerCallback} to listen changes.
     *
     * @param executor a callback Executor
     * @param callback a PlayerCallback
     * @throws IllegalArgumentException if executor or callback is {@code null}.
     */
    public final void registerPlayerCallback(
            @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull PlayerCallback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }

        synchronized (mLock) {
            for (Pair<PlayerCallback, Executor> pair : mCallbacks) {
                if (pair.first == callback && pair.second != null) {
                    Log.w(TAG, "callback is already added. Ignoring.");
                    return;
                }
            }
            mCallbacks.add(new Pair<>(callback, executor));
        }
    }

    /**
     * Unregister the previously registered {@link PlayerCallback}.
     *
     * @param callback the callback to be removed
     * @throws IllegalArgumentException if the callback is {@code null}.
     */
    public final void unregisterPlayerCallback(@NonNull PlayerCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        synchronized (mLock) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                if (mCallbacks.get(i).first == callback) {
                    mCallbacks.remove(i);
                }
            }
        }
    }

    /**
     * Gets the callbacks with executors for subclasses to notify player events.
     *
     * @return map of callbacks and its executors
     */
    protected final @NonNull List<Pair<PlayerCallback, Executor>> getCallbacks() {
        List<Pair<PlayerCallback, Executor>> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mCallbacks);
        }
        return list;
    }

    /**
     * A callback class to receive notifications for events on the session player. See
     * {@link #registerPlayerCallback(Executor, PlayerCallback)} to register this callback.
     */
    public abstract static class PlayerCallback {
        /**
         * Called when the state of the player has changed.
         *
         * @param player the player whose state has changed.
         * @param playerState the new state of the player.
         * @see #getPlayerState() ()
         */
        public void onPlayerStateChanged(@NonNull SessionPlayer player,
                @PlayerState int playerState) {
        }

        /**
         * Called when a buffering events for a media item happened.
         *
         * @param player the player that is buffering
         * @param item the media item for which buffering is happening.
         * @param buffState the new buffering state.
         * @see #getBufferingState()
         */
        public void onBufferingStateChanged(@NonNull SessionPlayer player,
                @Nullable MediaItem item, @BuffState int buffState) {
        }

        /**
         * Called when the playback speed has changed.
         *
         * @param player the player that has changed the playback speed.
         * @param playbackSpeed the new playback speed.
         * @see #getPlaybackSpeed()
         */
        public void onPlaybackSpeedChanged(@NonNull SessionPlayer player,
                float playbackSpeed) {
        }

        /**
         * Called when {@link #seekTo(long)} is completed.
         *
         * @param player the player that has completed seeking.
         * @param position the previous seeking request.
         * @see #getCurrentPosition()
         */
        public void onSeekCompleted(@NonNull SessionPlayer player, long position) {
        }

        /**
         * Called when a playlist is changed.
         *
         * @param player the player that has changed the playlist and playlist metadata.
         * @param list new playlist
         * @param metadata new metadata
         * @see #getPlaylist()
         * @see #getPlaylistMetadata()
         */
        public void onPlaylistChanged(@NonNull SessionPlayer player,
                @Nullable List<MediaItem> list, @Nullable MediaMetadata metadata) {
        }

        /**
         * Called when a playlist metadata is changed.
         *
         * @param player the player that has changed the playlist metadata.
         * @param metadata new metadata
         * @see #getPlaylistMetadata()
         */
        public void onPlaylistMetadataChanged(@NonNull SessionPlayer player,
                @Nullable MediaMetadata metadata) {
        }

        /**
         * Called when the shuffle mode is changed.
         *
         * @param player playlist agent for this event
         * @param shuffleMode shuffle mode
         * @see #SHUFFLE_MODE_NONE
         * @see #SHUFFLE_MODE_ALL
         * @see #SHUFFLE_MODE_GROUP
         * @see #getShuffleMode()
         */
        public void onShuffleModeChanged(@NonNull SessionPlayer player,
                @ShuffleMode int shuffleMode) {
        }

        /**
         * Called when the repeat mode is changed.
         *
         * @param player player for this event
         * @param repeatMode repeat mode
         * @see #REPEAT_MODE_NONE
         * @see #REPEAT_MODE_ONE
         * @see #REPEAT_MODE_ALL
         * @see #REPEAT_MODE_GROUP
         * @see #getRepeatMode()
         */
        public void onRepeatModeChanged(@NonNull SessionPlayer player,
                @RepeatMode int repeatMode) {
        }

        /**
         * Called when the player's current media item has changed.
         *
         * @param player the player whose media item changed.
         * @param item the new current media item.
         * @see #getCurrentMediaItem()
         */
        public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                @NonNull MediaItem item) {
        }

        /**
         * Called when the player finished playing. Playback state would be also set
         * {@link #PLAYER_STATE_PAUSED} with it.
         * <p>
         * This will be called only when the repeat mode is set to {@link #REPEAT_MODE_NONE}.
         *
         * @param player the player whose playback is completed.
         * @see #REPEAT_MODE_NONE
         */
        public void onPlaybackCompleted(@NonNull SessionPlayer player) {
        }

        /**
         * Called when the player's current audio attributes are changed.
         *
         * @param player the player whose audio attributes are changed.
         * @param attributes the new current audio attributes
         * @see #getAudioAttributes()
         */
        public void onAudioAttributesChanged(@NonNull SessionPlayer player,
                @Nullable AudioAttributesCompat attributes) {
        }
    }

    /**
     * Result class of the asynchronous APIs.
     * <p>
     * Subclass may extend this class for providing more result and/or custom result code. For the
     * custom result code, follow the convention below to avoid potential code duplication.
     * <p>
     * <ul>
     * <li>Predefined error code: Negative integers greater than -100. (i.e. -100 < code < 0)
     * <li>Custom error code: Negative integers equal to or less than -1000. (i.e. code < -1000)
     * <li>Predefined info code: Positive integers less than 100. (i.e. 0 < code < 100)
     * <li>Custom Info code: Positive integers equal to or greater than 1000. (i.e. code > +1000)
     * </ul>
     */
    public static class PlayerResult implements BaseResult {
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
                RESULT_CODE_SKIPPED})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface ResultCode {}

        private final int mResultCode;
        private final long mCompletionTime;
        private final MediaItem mItem;

        /**
         * Constructor that uses the current system clock as the completion time.
         *
         * @param resultCode result code. Recommends to use the standard code defined here.
         * @param item media item when the command is completed
         */
        // Note: resultCode is intentionally not annotated for subclass to return extra error codes.
        public PlayerResult(int resultCode, @Nullable MediaItem item) {
            this(resultCode, item, SystemClock.elapsedRealtime());
        }

        // Note: resultCode is intentionally not annotated for subclass to return extra error codes.
        private PlayerResult(int resultCode, @Nullable MediaItem item, long completionTime) {
            mResultCode = resultCode;
            mItem = item;
            mCompletionTime = completionTime;
        }

        static ListenableFuture<PlayerResult> createFuture(int resultCode) {
            ResolvableFuture<PlayerResult> result = ResolvableFuture.create();
            result.set(new PlayerResult(resultCode, null));
            return result;
        }

        /**
         * Gets the result code.
         * <p>
         * Subclass of the {@link SessionPlayer} may have defined customized extra code other than
         * codes defined here. Check the documentation of the class that you're interested in.
         *
         * @return result code.
         * @see #RESULT_CODE_UNKNOWN_ERROR
         * @see #RESULT_CODE_INVALID_STATE
         * @see #RESULT_CODE_BAD_VALUE
         * @see #RESULT_CODE_PERMISSION_DENIED
         * @see #RESULT_CODE_IO_ERROR
         * @see #RESULT_CODE_SKIPPED
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
         * Gets the {@link MediaItem} for which the command was executed. In other words, this is
         * the item sent as an argument of the command if any, otherwise the current media item when
         * the command was completed.
         *
         * @return media item when the command is completed. Can be {@code null} for an error, or
         *         the current media item was {@code null}.
         */
        @Override
        public @Nullable MediaItem getMediaItem() {
            return mItem;
        }
    }
}
