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

package androidx.media2.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * Subclasses may have extra methods to reset the player state to {@link #PLAYER_STATE_IDLE} from
 * other states. Take a look at documentations of specific subclass that you're interested in.
 * <p>
 *
 * <h3 id="InvalidStates">Invalid method calls</h3>
 * The only method you safely call from the {@link #PLAYER_STATE_ERROR} is {@link #close()}.
 * Any other methods might return meaningless data.
 * <p>
 * Subclasses of the SessionPlayer may have extra methods that are safe to be called in the error
 * state and/or provide a method to recover from the error state. Take a look at documentations of
 * specific subclass that you're interested in.
 * <p>
 * Most methods can be called from any non-Error state. In case they're called in invalid state,
 * the implementation should ignore and would return {@link PlayerResult} with
 * {@link PlayerResult#RESULT_ERROR_INVALID_STATE}. The following table lists the methods that
 * aren't guaranteed to successfully running if they're called from the associated invalid states.
 * <p>
 * <table>
 * <tr><th>Method Name</th> <th>Invalid States</th></tr>
 * <tr><td>setAudioAttributes</td> <td>{Paused, Playing}</td></tr>
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
public abstract class SessionPlayer implements Closeable {
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

    /**
     * Value indicating the time is unknown
     */
    public static final long UNKNOWN_TIME = Long.MIN_VALUE;

    /**
     * Media item index is invalid. This value will be returned when the corresponding media item
     * does not exist.
     */
    public static final int INVALID_ITEM_INDEX = -1;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final List<Pair<PlayerCallback, Executor>> mCallbacks = new ArrayList<>();

    /**
     * Starts or resumes playback.
     * <p>
     * On success, this transfers the player state to {@link #PLAYER_STATE_PLAYING} and
     * a {@link PlayerResult} should be returned with the current media item when the command
     * was completed. If it is called in {@link #PLAYER_STATE_IDLE} or {@link #PLAYER_STATE_ERROR},
     * it should be ignored and a {@link PlayerResult} should be returned with
     * {@link PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> play();

    /**
     * Pauses playback.
     * <p>
     * On success, this transfers the player state to {@link #PLAYER_STATE_PAUSED} and
     * a {@link PlayerResult} should be returned with the current media item when the command
     * was completed. If it is called in {@link #PLAYER_STATE_IDLE} or {@link #PLAYER_STATE_ERROR},
     * it should be ignored and a {@link PlayerResult} should be returned with
     * {@link PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> pause();

    /**
     * Prepares the media items for playback. During this time, the player may allocate resources
     * required to play, such as audio and video decoders. Before calling this API, set media
     * item(s) through either {@link #setMediaItem} or {@link #setPlaylist}.
     * <p>
     * On success, this transfers the player state from {@link #PLAYER_STATE_IDLE} to
     * {@link #PLAYER_STATE_PAUSED} and a {@link PlayerResult} should be returned with the prepared
     * media item when the command completed. If it's not called in {@link #PLAYER_STATE_IDLE},
     * it should be ignored and {@link PlayerResult} should be returned with
     * {@link PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> prepare();

    /**
     * Seeks to the specified position.
     * <p>
     * The position is the relative position based on the {@link MediaItem#getStartPosition()}. So
     * calling {@link #seekTo(long)} with {@code 0} means the seek to the start position.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed. If it's called in {@link #PLAYER_STATE_IDLE}, it is ignored and
     * a {@link PlayerResult} should be returned with
     * {@link PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @param position the new playback position in ms. The value should be in the range of start
     * and end positions defined in {@link MediaItem}.
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> seekTo(long position);

    /**
     * Sets the playback speed. The default playback speed is {@code 1.0f}, and negative values
     * indicate reverse playback and {@code 0.0f} is not allowed.
     * <p>
     * The supported playback speed range depends on the underlying player implementation, so it is
     * recommended to query the actual speed of the player via {@link #getPlaybackSpeed()} after the
     * operation completes. In particular, please note that player implementations may not support
     * reverse playback.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param playbackSpeed the requested playback speed
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see #getPlaybackSpeed()
     * @see PlayerCallback#onPlaybackSpeedChanged(SessionPlayer, float)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setPlaybackSpeed(float playbackSpeed);

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the media.
     * <p>
     * You must call this method in {@link #PLAYER_STATE_IDLE} in order for the audio attributes to
     * become effective thereafter. Otherwise, the call would be ignored and {@link PlayerResult}
     * should be returned with {@link PlayerResult#RESULT_ERROR_INVALID_STATE}.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param attributes non-null <code>AudioAttributes</code>.
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setAudioAttributes(
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
    @PlayerState
    public abstract int getPlayerState();

    /**
     * Gets the current playback position.
     * <p>
     * The position is the relative position based on the {@link MediaItem#getStartPosition()}.
     * So the position {@code 0} means the start position of the {@link MediaItem}.
     *
     * @return the current playback position in ms, or {@link #UNKNOWN_TIME} if unknown
     */
    public abstract long getCurrentPosition();

    /**
     * Gets the duration of the current media item, or {@link #UNKNOWN_TIME} if unknown. If the
     * current {@link MediaItem} has either start or end position, then duration would be adjusted
     * accordingly instead of returning the whole size of the {@link MediaItem}.
     *
     * @return the duration in ms, or {@link #UNKNOWN_TIME} if unknown
     */
    public abstract long getDuration();

    /**
     * Gets the position for how much has been buffered, or {@link #UNKNOWN_TIME} if unknown.
     * <p>
     * The position is the relative position based on the {@link MediaItem#getStartPosition()}.
     * So the position {@code 0} means the start position of the {@link MediaItem}.
     *
     * @return the buffered position in ms, or {@link #UNKNOWN_TIME} if unknown
     */
    public abstract long getBufferedPosition();

    /**
     * Returns the current buffering state of the player.
     * <p>
     * During the buffering, see {@link #getBufferedPosition()} for the quantifying the amount
     * already buffered.
     *
     * @return the buffering state, or {@link #BUFFERING_STATE_UNKNOWN} if unknown
     * @see #getBufferedPosition()
     */
    @BuffState
    public abstract int getBufferingState();

    /**
     * Gets the actual playback speed to be used by the player when playing. A value of {@code 1.0f}
     * is the default playback value, and a negative value indicates reverse playback.
     * <p>
     * Note that it may differ from the speed set in {@link #setPlaybackSpeed(float)}.
     *
     * @return the actual playback speed
     */
    public abstract float getPlaybackSpeed();

    /**
     * Gets the size of the video.
     *
     * @return the size of the video. The width and height of size could be 0 if there is no video
     *         or the size has not been determined yet.
     * @see PlayerCallback#onVideoSizeChanged(SessionPlayer, VideoSize)
     */
    @NonNull
    public VideoSize getVideoSize() {
        throw new UnsupportedOperationException("getVideoSize is not implemented");
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of the media.
     * <p>
     * A null surface will reset any Surface and result in only the audio track being played.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param surface the {@link Surface} to be used for the video portion of the media
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     */
    @NonNull
    public ListenableFuture<PlayerResult> setSurface(@Nullable Surface surface) {
        throw new UnsupportedOperationException("setSurface is not implemented");
    }

    /**
     * Sets a list of {@link MediaItem} with metadata. Use this or {@link #setMediaItem} to specify
     * which items to play.
     * <p>
     * This can be called multiple times in any states other than {@link #PLAYER_STATE_ERROR}. This
     * would override previous {@link #setMediaItem} or {@link #setPlaylist} calls.
     * <p>
     * Ensure uniqueness of each {@link MediaItem} in the playlist so the session can uniquely
     * identity individual items. All {@link MediaItem}s shouldn't be {@code null} as well.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in each {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged} and {@link PlayerCallback#onCurrentMediaItemChanged}
     * when it's completed. The current media item would be the first item in the playlist.
     * <p>
     * The implementation must close the {@link ParcelFileDescriptor} in the {@link FileMediaItem}
     * when a media item in the playlist is a {@link FileMediaItem}.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the first media item of the
     * playlist when the command completed.
     *
     * @param list a list of {@link MediaItem} objects to set as a play list
     * @throws IllegalArgumentException if the given list is {@code null} or empty, or has
     *         duplicated media items.
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see #setMediaItem
     * @see PlayerCallback#onPlaylistChanged
     * @see PlayerCallback#onCurrentMediaItemChanged
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setPlaylist(
            @NonNull List<MediaItem> list, @Nullable MediaMetadata metadata);

    /**
     * Gets the {@link AudioAttributesCompat} that media player has.
     */
    @Nullable
    public abstract AudioAttributesCompat getAudioAttributes();

    /**
     * Sets a {@link MediaItem} for playback. Use this or {@link #setPlaylist} to specify which
     * items to play. If you want to change current item in the playlist, use one of
     * {@link #skipToPlaylistItem}, {@link #skipToNextPlaylistItem}, or
     * {@link #skipToPreviousPlaylistItem} instead of this method.
     * <p>
     * This can be called multiple times in any states other than {@link #PLAYER_STATE_ERROR}. This
     * would override previous {@link #setMediaItem} or {@link #setPlaylist} calls.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged} and {@link PlayerCallback#onCurrentMediaItemChanged}
     * when it's completed. The current item would be the item given here.
     * <p>
     * The implementation must close the {@link ParcelFileDescriptor} in the {@link FileMediaItem}
     * if the given media item is a {@link FileMediaItem}.
     * <p>
     * On success, a {@link PlayerResult} should be returned with {@code item} set.
     *
     * @param item the descriptor of media item you want to play
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see #setPlaylist
     * @see PlayerCallback#onPlaylistChanged
     * @see PlayerCallback#onCurrentMediaItemChanged
     * @throws IllegalArgumentException if the given item is {@code null}.
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setMediaItem(
            @NonNull MediaItem item);

    /**
     * Adds the media item to the playlist at the index. Index equals to or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist should be increased correspondingly.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     * <p>
     * The implementation must close the {@link ParcelFileDescriptor} in the {@link FileMediaItem}
     * if the given media item is a {@link FileMediaItem}.
     * <p>
     * On success, a {@link PlayerResult} should be returned with {@code item} added.
     *
     * @param index the index of the item you want to add in the playlist
     * @param item the media item you want to add
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> addPlaylistItem(int index,
            @NonNull MediaItem item);

    /**
     * Removes the media item from the playlist
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with {@code item} removed.
     * <p>
     * If the last item is removed, the player should be moved to {@link #PLAYER_STATE_IDLE}.
     *
     * @param index the index of the item you want to remove in the playlist
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> removePlaylistItem(
            @IntRange(from = 0) int index);

    /**
     * Replaces the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     * <p>
     * The implementation must close the {@link ParcelFileDescriptor} in the {@link FileMediaItem}
     * if the given media item is a {@link FileMediaItem}.
     * <p>
     * On success, a {@link PlayerResult} should be returned with {@code item} set.
     *
     * @param index the index of the item to replace in the playlist
     * @param item the new item
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> replacePlaylistItem(int index,
            @NonNull MediaItem item);

    /**
     * Moves the media item at {@code fromIdx} to {@code toIdx} in the playlist.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with {@code item} set.
     *
     * @param fromIndex the media item's initial index in the playlist
     * @param toIndex the media item's target index in the playlist
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @NonNull
    public ListenableFuture<PlayerResult> movePlaylistItem(
            @IntRange(from = 0) int fromIndex, @IntRange(from = 0) int toIndex) {
        throw new UnsupportedOperationException("movePlaylistItem is not implemented");
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> skipToPreviousPlaylistItem();

    /**
     * Skips to the next item in the playlist.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> skipToNextPlaylistItem();

    /**
     * Skips to the item in the playlist at the index.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param index the index of the item you want to play in the playlist
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> skipToPlaylistItem(
            @IntRange(from = 0) int index);

    /**
     * Updates the playlist metadata while keeping the playlist as-is.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)} when it's
     * completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param metadata metadata of the playlist
     * @see PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> updatePlaylistMetadata(
            @Nullable MediaMetadata metadata);

    /**
     * Sets the repeat mode.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onRepeatModeChanged(SessionPlayer, int)} when it's completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param repeatMode repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     * @see PlayerCallback#onRepeatModeChanged(SessionPlayer, int)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setRepeatMode(
            @RepeatMode int repeatMode);

    /**
     * Sets the shuffle mode.
     * <p>
     * The implementation must notify registered callbacks with
     * {@link PlayerCallback#onShuffleModeChanged(SessionPlayer, int)} when it's completed.
     * <p>
     * On success, a {@link PlayerResult} should be returned with the current media item when the
     * command completed.
     *
     * @param shuffleMode the shuffle mode
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see PlayerCallback#onShuffleModeChanged(SessionPlayer, int)
     */
    @NonNull
    public abstract ListenableFuture<PlayerResult> setShuffleMode(
            @ShuffleMode int shuffleMode);

    /**
     * Gets the playlist. It can be {@code null} if the playlist hasn't been set or it's reset by
     * {@link #setMediaItem}.
     *
     * @return playlist, or {@code null}
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @Nullable
    public abstract List<MediaItem> getPlaylist();

    /**
     * Gets the playlist metadata.
     *
     * @return metadata metadata of the playlist, or null if none is set
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     * @see PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)
     */
    @Nullable
    public abstract MediaMetadata getPlaylistMetadata();

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
    @RepeatMode
    public abstract int getRepeatMode();

    /**
     * Gets the shuffle mode.
     *
     * @return the shuffle mode
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see PlayerCallback#onShuffleModeChanged(SessionPlayer, int)
     */
    @ShuffleMode
    public abstract int getShuffleMode();

    /**
     * Gets the current media item, which is currently playing or would be played with later
     * {@link #play}. This value may be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is
     * called.
     *
     * @return the current media item. Can be {@code null} only when the player is in
     * {@link #PLAYER_STATE_IDLE} and a media item or playlist hasn't been set.
     * @see #setMediaItem
     * @see #setPlaylist
     */
    @Nullable
    public abstract MediaItem getCurrentMediaItem();

    /**
     * Gets the index of current media item in playlist. This value should be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of current media item. Can be {@link #INVALID_ITEM_INDEX} when current
     *         media item is null or not in the playlist, and when the playlist hasn't been set.
     */
    @IntRange(from = INVALID_ITEM_INDEX)
    public abstract int getCurrentMediaItemIndex();

    /**
     * Gets the previous item index in the playlist. This value should be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of previous media item. Can be {@link #INVALID_ITEM_INDEX} only when
     *         previous media item does not exist or playlist hasn't been set.
     */
    @IntRange(from = INVALID_ITEM_INDEX)
    public abstract int getPreviousMediaItemIndex();

    /**
     * Gets the next item index in the playlist. This value should be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of next media item. Can be {@link #INVALID_ITEM_INDEX} only when next media
     *         item does not exist or playlist hasn't been set.
     */
    @IntRange(from = INVALID_ITEM_INDEX)
    public abstract int getNextMediaItemIndex();

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
            throw new NullPointerException("executor shouldn't be null");
        }
        if (callback == null) {
            throw new NullPointerException("callback shouldn't be null");
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
            throw new NullPointerException("callback shouldn't be null");
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
    @NonNull
    protected final List<Pair<PlayerCallback, Executor>> getCallbacks() {
        List<Pair<PlayerCallback, Executor>> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mCallbacks);
        }
        return list;
    }

    /**
     * Gets the full list of selected and unselected tracks that the media contains. The order of
     * the list is irrelevant as different players expose tracks in different ways, but the tracks
     * will generally be ordered based on track type.
     * <p>
     * The types of tracks supported may vary based on player implementation.
     *
     * @return list of tracks. The total number of tracks is the size of the list. If empty,
     *         the implementation should return an empty list instead of {@code null}.
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     */
    @NonNull
    public List<TrackInfo> getTracks() {
        throw new UnsupportedOperationException("getTracks is not implemented");
    }

    /**
     * Selects the {@link TrackInfo} for the current media item.
     * <p>
     * Generally one track will be selected for each track type.
     * <p>
     * The types of tracks supported may vary based on player implementation.
     * <p>
     * Note: {@link #getTracks()} returns the list of tracks that can be selected, but the
     * list may be invalidated when {@link PlayerCallback#onTracksChanged(SessionPlayer, List)}
     * is called.
     *
     * @param trackInfo track to be selected
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     * @see PlayerCallback#onTrackSelected(SessionPlayer, TrackInfo)
     */
    @NonNull
    public ListenableFuture<PlayerResult> selectTrack(@NonNull TrackInfo trackInfo) {
        throw new UnsupportedOperationException("selectTrack is not implemented");
    }

    /**
     * Deselects the {@link TrackInfo} for the current media item.
     * <p>
     * Generally, a track should already be selected in order to be deselected, and audio and video
     * tracks should not be deselected.
     * <p>
     * The types of tracks supported may vary based on player implementation.
     * <p>
     * Note: {@link #getSelectedTrack(int)} returns the currently selected track per track type that
     * can be deselected, but the list may be invalidated when
     * {@link PlayerCallback#onTracksChanged(SessionPlayer, List)} is called.
     *
     * @param trackInfo track to be deselected
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     * @see PlayerCallback#onTrackDeselected(SessionPlayer, TrackInfo)
     */
    @NonNull
    public ListenableFuture<PlayerResult> deselectTrack(@NonNull TrackInfo trackInfo) {
        throw new UnsupportedOperationException("deselectTrack is not implemented");
    }

    /**
     * Gets currently selected track's {@link TrackInfo} for the given track type.
     * <p>
     * The returned value can be outdated after
     * {@link PlayerCallback#onTracksChanged(SessionPlayer, List)},
     * {@link PlayerCallback#onTrackSelected(SessionPlayer, TrackInfo)},
     * or {@link PlayerCallback#onTrackDeselected(SessionPlayer, TrackInfo)} is called.
     *
     * @param trackType type of selected track
     * @return selected track info
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     */
    @Nullable
    public TrackInfo getSelectedTrack(@TrackInfo.MediaTrackType int trackType) {
        throw new UnsupportedOperationException(
                "getSelectedTrack is not implemented");
    }

    /**
     * Removes all existing references to callbacks and executors.
     *
     * Note: Sub classes of {@link SessionPlayer} that override this API should call this super
     * method.
     */
    @CallSuper
    @Override
    public void close() {
        synchronized (mLock) {
            mCallbacks.clear();
        }
    }

    /**
     * Class for the player to return each audio/video/subtitle track's metadata.
     *
     * Note: TrackInfo holds a MediaFormat instance, but only the following key-values will be
     * supported when sending it over different processes:
     * <ul>
     * <li>{@link MediaFormat#KEY_LANGUAGE}</li>
     * <li>{@link MediaFormat#KEY_MIME}</li>
     * <li>{@link MediaFormat#KEY_IS_FORCED_SUBTITLE}</li>
     * <li>{@link MediaFormat#KEY_IS_AUTOSELECT}</li>
     * <li>{@link MediaFormat#KEY_IS_DEFAULT}</li>
     * </ul>
     *
     * @see #getTracks
     */
    @VersionedParcelize(isCustom = true)
    public static class TrackInfo extends CustomVersionedParcelable {
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;

        private static final String KEY_IS_FORMAT_NULL =
                "androidx.media2.common.SessionPlayer.TrackInfo.KEY_IS_FORMAT_NULL";
        private static final String KEY_IS_SELECTABLE =
                "androidx.media2.common.SessionPlayer.TrackInfo.KEY_IS_SELECTABLE";

        /**
         * @hide
         */
        @IntDef(flag = false, /*prefix = "MEDIA_TRACK_TYPE",*/ value = {
                MEDIA_TRACK_TYPE_UNKNOWN,
                MEDIA_TRACK_TYPE_VIDEO,
                MEDIA_TRACK_TYPE_AUDIO,
                MEDIA_TRACK_TYPE_SUBTITLE,
                MEDIA_TRACK_TYPE_METADATA,
        })
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface MediaTrackType {}

        @ParcelField(1)
        int mId;

        // Removed @ParcelField(2)

        @ParcelField(3)
        int mTrackType;

        // Parceled via mParcelableExtras.
        @NonParcelField
        @Nullable
        MediaFormat mFormat;
        // Parceled via mParcelableExtras.
        @NonParcelField
        boolean mIsSelectable;
        // For extra information containing MediaFormat and isSelectable data. Should only be used
        // by onPreParceling() and onPostParceling().
        @ParcelField(4)
        Bundle mParcelableExtras;
        @NonParcelField
        private final Object mLock = new Object();

        // WARNING: Adding a new ParcelField may break old library users (b/152830728)

        /**
         * Used for VersionedParcelable
         */
        TrackInfo() {
            // no-op
        }

        /**
         * Constructor to create a TrackInfo instance.
         *
         * Note: The default value for {@link #isSelectable()} is false.
         *
         * @param id id of track unique across {@link MediaItem}s
         * @param type type of track. Can be video, audio or subtitle
         * @param format format of track
         */
        public TrackInfo(int id, int type, @Nullable MediaFormat format) {
            this(id, type, format, /* isSelectable= */ false);
        }

        /**
         * Constructor to create a TrackInfo instance.
         *
         * @param id id of track unique across {@link MediaItem}s
         * @param type type of track. Can be video, audio or subtitle
         * @param format format of track
         * @param isSelectable whether track can be selected via
         * {@link SessionPlayer#selectTrack(TrackInfo)}.
         */
        public TrackInfo(int id, int type, @Nullable MediaFormat format, boolean isSelectable) {
            mId = id;
            mTrackType = type;
            mFormat = format;
            mIsSelectable = isSelectable;
        }

        /**
         * Gets the track type.
         * @return MediaTrackType which indicates if the track is video, audio or subtitle
         */
        @MediaTrackType
        public int getTrackType() {
            return mTrackType;
        }

        /**
         * Gets the language code of the track.
         * @return {@link Locale} which includes the language information
         */
        @NonNull
        public Locale getLanguage() {
            String language = mFormat != null ? mFormat.getString(MediaFormat.KEY_LANGUAGE) : null;
            if (language == null) {
                language = "und";
            }
            return new Locale(language);
        }

        /**
         * Gets the {@link MediaFormat} of the track.  If the format is
         * unknown or could not be determined, null is returned.
         */
        @Nullable
        public MediaFormat getFormat() {
            return mFormat;
        }

        /**
         * Gets the id of the track.
         * The id is used by {@link #selectTrack(TrackInfo)} and {@link #deselectTrack(TrackInfo)}
         * to identify the track to be (de)selected.
         * So, it's highly recommended to ensure that the id of each track is unique across
         * {@link MediaItem}s to avoid potential mis-selection when a stale {@link TrackInfo} is
         * used.
         *
         * @return id of the track
         */
        public int getId() {
            return mId;
        }

        /**
         * Whether the current track can be selected via {@link #selectTrack(TrackInfo)} or not.
         *
         * @return true if the current track can be selected; false if otherwise.
         */
        public boolean isSelectable() {
            return mIsSelectable;
        }

        @Override
        @NonNull
        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('#').append(mId);
            out.append('{');
            switch (mTrackType) {
                case MEDIA_TRACK_TYPE_VIDEO:
                    out.append("VIDEO");
                    break;
                case MEDIA_TRACK_TYPE_AUDIO:
                    out.append("AUDIO");
                    break;
                case MEDIA_TRACK_TYPE_SUBTITLE:
                    out.append("SUBTITLE");
                    break;
                case MEDIA_TRACK_TYPE_METADATA:
                    out.append("METADATA");
                    break;
                default:
                    out.append("UNKNOWN");
                    break;
            }
            out.append(", ").append(mFormat);
            out.append(", isSelectable=").append(mIsSelectable);
            out.append("}");
            return out.toString();
        }

        @Override
        public int hashCode() {
            return mId;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TrackInfo)) {
                return false;
            }
            TrackInfo other = (TrackInfo) obj;
            return mId == other.mId;
        }

        /**
         * @hide
         * @param isStream
         */
        @RestrictTo(LIBRARY)
        @Override
        public void onPreParceling(boolean isStream) {
            synchronized (mLock) {
                mParcelableExtras = new Bundle();

                mParcelableExtras.putBoolean(KEY_IS_FORMAT_NULL, mFormat == null);
                if (mFormat != null) {
                    putStringValueToBundle(MediaFormat.KEY_LANGUAGE, mFormat, mParcelableExtras);
                    putStringValueToBundle(MediaFormat.KEY_MIME, mFormat, mParcelableExtras);
                    putIntValueToBundle(MediaFormat.KEY_IS_FORCED_SUBTITLE, mFormat,
                            mParcelableExtras);
                    putIntValueToBundle(MediaFormat.KEY_IS_AUTOSELECT, mFormat, mParcelableExtras);
                    putIntValueToBundle(MediaFormat.KEY_IS_DEFAULT, mFormat, mParcelableExtras);
                }

                mParcelableExtras.putBoolean(KEY_IS_SELECTABLE, mIsSelectable);
            }
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Override
        public void onPostParceling() {
            if (mParcelableExtras != null && !mParcelableExtras.getBoolean(KEY_IS_FORMAT_NULL)) {
                mFormat = new MediaFormat();
                setStringValueToMediaFormat(MediaFormat.KEY_LANGUAGE, mFormat, mParcelableExtras);
                setStringValueToMediaFormat(MediaFormat.KEY_MIME, mFormat, mParcelableExtras);
                setIntValueToMediaFormat(MediaFormat.KEY_IS_FORCED_SUBTITLE, mFormat,
                        mParcelableExtras);
                setIntValueToMediaFormat(MediaFormat.KEY_IS_AUTOSELECT, mFormat, mParcelableExtras);
                setIntValueToMediaFormat(MediaFormat.KEY_IS_DEFAULT, mFormat, mParcelableExtras);
            }

            if (mParcelableExtras == null || !mParcelableExtras.containsKey(KEY_IS_SELECTABLE)) {
                mIsSelectable = mTrackType != MEDIA_TRACK_TYPE_VIDEO;
            } else {
                mIsSelectable = mParcelableExtras.getBoolean(KEY_IS_SELECTABLE);
            }
        }

        private static void putIntValueToBundle(
                String intValueKey, MediaFormat mediaFormat, Bundle bundle) {
            if (mediaFormat.containsKey(intValueKey)) {
                bundle.putInt(intValueKey, mediaFormat.getInteger(intValueKey));
            }
        }

        private static void putStringValueToBundle(
                String stringValueKey, MediaFormat mediaFormat, Bundle bundle) {
            if (mediaFormat.containsKey(stringValueKey)) {
                bundle.putString(stringValueKey, mediaFormat.getString(stringValueKey));
            }
        }

        private static void setIntValueToMediaFormat(
                String intValueKey, MediaFormat mediaFormat, Bundle bundle) {
            if (bundle.containsKey(intValueKey)) {
                mediaFormat.setInteger(intValueKey, bundle.getInt(intValueKey));
            }
        }

        private static void setStringValueToMediaFormat(
                String stringValueKey, MediaFormat mediaFormat, Bundle bundle) {
            if (bundle.containsKey(stringValueKey)) {
                mediaFormat.setString(stringValueKey, bundle.getString(stringValueKey));
            }
        }
    }

    /**
     * A callback class to receive notifications for events on the session player. See
     * {@link #registerPlayerCallback(Executor, PlayerCallback)} to register this callback.
     */
    public abstract static class PlayerCallback {
        /**
         * Called when the state of the player has changed.
         *
         * @param player the player whose state has changed
         * @param playerState the new state of the player
         * @see #getPlayerState()
         */
        public void onPlayerStateChanged(@NonNull SessionPlayer player,
                @PlayerState int playerState) {
        }

        /**
         * Called when a buffering events for a media item happened.
         *
         * @param player the player that is buffering
         * @param item the media item for which buffering is happening
         * @param buffState the new buffering state
         * @see #getBufferingState()
         */
        public void onBufferingStateChanged(@NonNull SessionPlayer player,
                @Nullable MediaItem item, @BuffState int buffState) {
        }

        /**
         * Called when the playback speed has changed.
         *
         * @param player the player that has changed the playback speed
         * @param playbackSpeed the new playback speed
         * @see #getPlaybackSpeed()
         */
        public void onPlaybackSpeedChanged(@NonNull SessionPlayer player,
                float playbackSpeed) {
        }

        /**
         * Called when {@link #seekTo(long)} is completed.
         *
         * @param player the player that has completed seeking
         * @param position the previous seeking request
         * @see #getCurrentPosition()
         */
        public void onSeekCompleted(@NonNull SessionPlayer player, long position) {
        }

        /**
         * Called when a playlist is changed. It's also called after {@link #setPlaylist} or
         * {@link #setMediaItem}.
         *
         * @param player the player that has changed the playlist and playlist metadata
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
         * @param player the player that has changed the playlist metadata
         * @param metadata new metadata
         * @see #getPlaylistMetadata()
         */
        public void onPlaylistMetadataChanged(@NonNull SessionPlayer player,
                @Nullable MediaMetadata metadata) {
        }

        /**
         * Called when the shuffle mode is changed.
         * <p>
         * {@link SessionPlayer#getPreviousMediaItemIndex()} and
         * {@link SessionPlayer#getNextMediaItemIndex()} values can be outdated when this callback
         * is called if the current media item is the first or last item in the playlist.
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
         * <p>
         * {@link SessionPlayer#getPreviousMediaItemIndex()} and
         * {@link SessionPlayer#getNextMediaItemIndex()} values can be outdated when this callback
         * is called if the current media item is the first or last item in the playlist.
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
         * Called when the player's current media item has changed. Generally called after a new
         * media item is set through {@link #setPlaylist} or {@link #setMediaItem}, or after
         * skipping to a different item in a given playlist.
         *
         * @param player the player whose media item changed
         * @param item the new current media item. This can be {@code null} when the state of
         * the player becomes {@link #PLAYER_STATE_IDLE}.
         * @see #getCurrentMediaItem()
         */
        public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                @Nullable MediaItem item) {
        }

        /**
         * Called when the player finished playing. Playback state would be also set
         * {@link #PLAYER_STATE_PAUSED} with it.
         * <p>
         * This will be called only when the repeat mode is set to {@link #REPEAT_MODE_NONE}.
         *
         * @param player the player whose playback is completed
         * @see #REPEAT_MODE_NONE
         */
        public void onPlaybackCompleted(@NonNull SessionPlayer player) {
        }

        /**
         * Called when the player's current audio attributes are changed.
         *
         * @param player the player whose audio attributes are changed
         * @param attributes the new current audio attributes
         * @see #getAudioAttributes()
         */
        public void onAudioAttributesChanged(@NonNull SessionPlayer player,
                @Nullable AudioAttributesCompat attributes) {
        }

        /**
         * Called to indicate the video size
         * <p>
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         * <p>
         * This callback is generally called when player updates video size, but will also be
         * called when {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)}
         * is called.
         *
         * @param player the player associated with this callback
         * @param size the size of the video
         * @see #getVideoSize()
         */
        public void onVideoSizeChanged(@NonNull SessionPlayer player, @NonNull VideoSize size) {
        }

        /**
         * Called when the player's subtitle track has new subtitle data available.
         * @param player the player that reports the new subtitle data
         * @param item the MediaItem of this media item
         * @param track the track that has the subtitle data
         * @param data the subtitle data
         */
        public void onSubtitleData(@NonNull SessionPlayer player, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
        }

        /**
         * Called when the tracks of the current media item is changed such as
         * 1) when tracks of a media item become available,
         * 2) when new tracks are found during playback, or
         * 3) when the current media item is changed.
         * <p>
         * When it's called, you should invalidate previous track information and use the new
         * tracks to call {@link #selectTrack(TrackInfo)} or
         * {@link #deselectTrack(TrackInfo)}.
         *
         * @param player the player associated with this callback
         * @param tracks the list of tracks. It can be empty
         * @see #getTracks()
         */
        public void onTracksChanged(@NonNull SessionPlayer player,
                @NonNull List<TrackInfo> tracks) {
        }

        /**
         * Called when a track is selected.
         *
         * @param player the player associated with this callback
         * @param trackInfo the selected track
         * @see #selectTrack(TrackInfo)
         */
        public void onTrackSelected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
        }

        /**
         * Called when a track is deselected.
         * <p>
         * This callback will generally be called only after calling
         * {@link #deselectTrack(TrackInfo)}.
         *
         * @param player the player associated with this callback
         * @param trackInfo the deselected track
         * @see #deselectTrack(TrackInfo)
         */
        public void onTrackDeselected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
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
    @SuppressWarnings("HiddenSuperclass")
    public static class PlayerResult implements BaseResult {
        /**
         * @hide
         */
        @IntDef(flag = false, /*prefix = "RESULT",*/ value = {
                RESULT_SUCCESS,
                RESULT_ERROR_UNKNOWN,
                RESULT_ERROR_INVALID_STATE,
                RESULT_ERROR_BAD_VALUE,
                RESULT_ERROR_PERMISSION_DENIED,
                RESULT_ERROR_IO,
                RESULT_ERROR_NOT_SUPPORTED,
                RESULT_INFO_SKIPPED})
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY)
        public @interface ResultCode {}

        private final int mResultCode;
        private final long mCompletionTime;
        private final MediaItem mItem;

        /**
         * Constructor that uses the current system clock as the completion time.
         *
         * @param resultCode result code. Recommends to use the standard code defined here.
         * @param item media item when the command completed
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


        /**
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        @NonNull
        public static ListenableFuture<PlayerResult> createFuture(int resultCode) {
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
         * @return result code
         * @see #RESULT_ERROR_UNKNOWN
         * @see #RESULT_ERROR_INVALID_STATE
         * @see #RESULT_ERROR_BAD_VALUE
         * @see #RESULT_ERROR_PERMISSION_DENIED
         * @see #RESULT_ERROR_IO
         * @see #RESULT_ERROR_NOT_SUPPORTED
         * @see #RESULT_INFO_SKIPPED
         */
        @Override
        @ResultCode
        public int getResultCode() {
            return mResultCode;
        }

        /**
         * Gets the completion time of the command. Being more specific, it's the same as
         * {@link android.os.SystemClock#elapsedRealtime()} when the command completed.
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
         * the command completed.
         *
         * @return media item when the command completed. Can be {@code null} for an error, or
         *         the current media item was {@code null}
         */
        @Override
        @Nullable
        public MediaItem getMediaItem() {
            return mItem;
        }
    }
}
