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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Base interface for all media players that want media session
 *
 * @hide
 */
// TODO(jaewan): Unhide
// Previously MediaSessionCompat.Callback.
// Subclasses can be implemented with ExoPlayer, MediaPlayer2, RemoteClient, etc.
// Preferably it can be interface, but API guideline requires to use abstract class.
@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(LIBRARY)
public abstract class SessionPlayer2 implements AutoCloseable {
    private static final String TAG = "SessionPlayer2";

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
            BUFFERING_STATE_BUFFERING_COMPLETE})
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
    public static final int BUFFERING_STATE_BUFFERING_COMPLETE = 3;


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

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Map<PlayerCallback, Executor> mCallbacks = new HashMap<>();

    // APIs from the MediaPlayerConnector
    public abstract @NonNull Future<CommandResult2> play();

    public abstract @NonNull Future<CommandResult2> pause();

    public abstract @NonNull Future<CommandResult2> prepare();

    public abstract @NonNull Future<CommandResult2> seekTo(long position);

    public abstract @NonNull Future<CommandResult2> setPlaybackSpeed(float playbackSpeed);

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the media.
     * You must call this method in {@link #PLAYER_STATE_IDLE} in order for the audio attributes to
     * become effective thereafter.
     *
     * @param attributes non-null <code>AudioAttributes</code>.
     */
    public abstract @NonNull Future<CommandResult2> setAudioAttributes(
            @NonNull AudioAttributesCompat attributes);

    public abstract @PlayerState int getPlayerState();

    public abstract long getCurrentPosition();

    public abstract long getDuration();

    public abstract long getBufferedPosition();

    public abstract @BuffState int getBufferingState();

    public abstract float getPlaybackSpeed();

    // APIs from the MediaPlaylistAgent
    public abstract @NonNull Future<CommandResult2> setPlaylist(List<DataSourceDesc2> list,
            MediaMetadata2 metadata);

    /**
     * Gets the {@link AudioAttributesCompat} that media player has.
     */
    public abstract @Nullable AudioAttributesCompat getAudioAttributes();

    /**
     * Sets a {@link DataSourceDesc2} for playback. This is helper method for
     * {@link #setPlaylist(List, MediaMetadata2)} to set playlist without creating a {@link List}
     * and doesn't specify playlist metadata.
     *
     * @param item
     * @return
     */
    public abstract @NonNull Future<CommandResult2> setMediaItem(DataSourceDesc2 item);

    public abstract @NonNull Future<CommandResult2> addPlaylistItem(int index,
            @NonNull DataSourceDesc2 item);

    public abstract @NonNull Future<CommandResult2> removePlaylistItem(
            @NonNull DataSourceDesc2 item);

    public abstract @NonNull Future<CommandResult2> replacePlaylistItem(int index,
            @NonNull DataSourceDesc2 item);

    public abstract @NonNull Future<CommandResult2> skipToPreviousItem();

    public abstract @NonNull Future<CommandResult2> skipToNextItem();

    public abstract @NonNull Future<CommandResult2> skipToPlaylistItem(
            @NonNull DataSourceDesc2 item);

    public abstract @NonNull Future<CommandResult2> updatePlaylistMetadata(
            @Nullable MediaMetadata2 metadata);

    public abstract @NonNull Future<CommandResult2> setRepeatMode(
            @RepeatMode int repeatMode);

    public abstract @NonNull Future<CommandResult2> setShuffleMode(
            @ShuffleMode int shuffleMode);

    public abstract @Nullable List<DataSourceDesc2> getPlaylist();

    public abstract @Nullable MediaMetadata2 getPlaylistMetadata();

    public abstract @RepeatMode int getRepeatMode();

    public abstract @ShuffleMode int getShuffleMode();

    // APIs previously in the grey area (neither player connector / playlist agent)
    public abstract @Nullable DataSourceDesc2 getCurrentMediaItem();

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
            if (mCallbacks.get(callback) != null) {
                Log.w(TAG, "callback is already added. Ignoring.");
                return;
            }
            mCallbacks.put(callback, executor);
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
            mCallbacks.remove(callback);
        }
    }

    // For subclasses that wants to extend Callbacks
    protected final Map<PlayerCallback, Executor> getCallbacks() {
        Map<PlayerCallback, Executor> map = new HashMap<>();
        synchronized (mLock) {
            map.putAll(mCallbacks);
        }
        return map;
    }

    public static abstract class PlayerCallback {
        // Callbacks from MediaPlayerConnector
        public void onPlayerStateChanged(@NonNull SessionPlayer2 player,
                @PlayerState int playerState) {
        }

        public void onBufferingStateChanged(@NonNull SessionPlayer2 player,
                @Nullable DataSourceDesc2 desc, @BuffState int buffState) {
        }

        public void onPlaybackSpeedChanged(@NonNull SessionPlayer2 player,
                float playbackSpeed) {
        }

        /**
         * Called when players {@link AudioAttributesCompat} has been changed
         *
         * @param player player
         * @param attributes new attributes
         */
        public void onAudioAttributeChanged(@NonNull SessionPlayer2 player,
                @NonNull AudioAttributesCompat attributes) {
        }

        public void onSeekCompleted(@NonNull SessionPlayer2 player, long position) {
        }

        // Callbacks from MediaPlaylistAgent
        public void onPlaylistChanged(@NonNull SessionPlayer2 player, List<DataSourceDesc2> list,
                @Nullable MediaMetadata2 metadata) {
        }

        public void onPlaylistMetadataChanged(@NonNull SessionPlayer2 player,
                @Nullable MediaMetadata2 metadata) {
        }

        public void onShuffleModeChanged(@NonNull SessionPlayer2 player,
                @ShuffleMode int shuffleMode) {
        }

        public void onRepeatModeChanged(@NonNull SessionPlayer2 player,
                @RepeatMode int repeatMode) {
        }

        // Callbacks in the middle
        public void onCurrentMediaItemChanged(@NonNull SessionPlayer2 player,
                @Nullable DataSourceDesc2 item) {
        }
    }
}
