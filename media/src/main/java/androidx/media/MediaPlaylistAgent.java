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

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.SimpleArrayMap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * MediaPlaylistAgent is the abstract class an application needs to derive from to pass an object
 * to a MediaSession2 that will override default playlist handling behaviors. It contains a set of
 * notify methods to signal MediaSession2 that playlist-related state has changed.
 * <p>
 * Playlists are composed of one or multiple {@link MediaItem2} instances, which combine metadata
 * and data sources (as {@link DataSourceDesc})
 * Used by {@link MediaSession2} and {@link MediaController2}.
 */
// This class only includes methods that contain {@link MediaItem2}.
public abstract class MediaPlaylistAgent {
    private static final String TAG = "MediaPlaylistAgent";

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({REPEAT_MODE_NONE, REPEAT_MODE_ONE, REPEAT_MODE_ALL,
            REPEAT_MODE_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode {}

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
    public @interface ShuffleMode {}

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
    private final SimpleArrayMap<PlaylistEventCallback, Executor> mCallbacks =
            new SimpleArrayMap<>();

    /**
     * Register {@link PlaylistEventCallback} to listen changes in the underlying
     * {@link MediaPlaylistAgent}.
     *
     * @param executor a callback Executor
     * @param callback a PlaylistEventCallback
     * @throws IllegalArgumentException if executor or callback is {@code null}.
     */
    public final void registerPlaylistEventCallback(
            @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull PlaylistEventCallback callback) {
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
     * Unregister the previously registered {@link PlaylistEventCallback}.
     *
     * @param callback the callback to be removed
     * @throws IllegalArgumentException if the callback is {@code null}.
     */
    public final void unregisterPlaylistEventCallback(@NonNull PlaylistEventCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        synchronized (mLock) {
            mCallbacks.remove(callback);
        }
    }

    /**
     * Notifies the current playlist and playlist metadata. Call this API when the playlist is
     * changed.
     * <p>
     * Registered {@link PlaylistEventCallback} would receive this event through the
     * {@link PlaylistEventCallback#onPlaylistChanged(MediaPlaylistAgent, List, MediaMetadata2)}.
     */
    public final void notifyPlaylistChanged() {
        SimpleArrayMap<PlaylistEventCallback, Executor> callbacks = getCallbacks();
        final List<MediaItem2> playlist = getPlaylist();
        final MediaMetadata2 metadata = getPlaylistMetadata();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlaylistEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistChanged(
                            MediaPlaylistAgent.this, playlist, metadata);
                }
            });
        }
    }

    /**
     * Notifies the current playlist metadata. Call this API when the playlist metadata is changed.
     * <p>
     * Registered {@link PlaylistEventCallback} would receive this event through the
     * {@link PlaylistEventCallback#onPlaylistMetadataChanged(MediaPlaylistAgent, MediaMetadata2)}.
     */
    public final void notifyPlaylistMetadataChanged() {
        SimpleArrayMap<PlaylistEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlaylistEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistMetadataChanged(
                            MediaPlaylistAgent.this, MediaPlaylistAgent.this.getPlaylistMetadata());
                }
            });
        }
    }

    /**
     * Notifies the current shuffle mode. Call this API when the shuffle mode is changed.
     * <p>
     * Registered {@link PlaylistEventCallback} would receive this event through the
     * {@link PlaylistEventCallback#onShuffleModeChanged(MediaPlaylistAgent, int)}.
     */
    public final void notifyShuffleModeChanged() {
        SimpleArrayMap<PlaylistEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlaylistEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onShuffleModeChanged(
                            MediaPlaylistAgent.this, MediaPlaylistAgent.this.getShuffleMode());
                }
            });
        }
    }

    /**
     * Notifies the current repeat mode. Call this API when the repeat mode is changed.
     * <p>
     * Registered {@link PlaylistEventCallback} would receive this event through the
     * {@link PlaylistEventCallback#onRepeatModeChanged(MediaPlaylistAgent, int)}.
     */
    public final void notifyRepeatModeChanged() {
        SimpleArrayMap<PlaylistEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlaylistEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onRepeatModeChanged(
                            MediaPlaylistAgent.this, MediaPlaylistAgent.this.getRepeatMode());
                }
            });
        }
    }

    /**
     * Returns the playlist
     *
     * @return playlist, or null if none is set.
     */
    public abstract @Nullable List<MediaItem2> getPlaylist();

    /**
     * Sets the playlist with the metadata.
     * <p>
     * When the playlist is changed, call {@link #notifyPlaylistChanged()} to notify changes to the
     * registered callbacks.
     *
     * @param list playlist
     * @param metadata metadata of the playlist
     * @see #notifyPlaylistChanged()
     */
    public abstract void setPlaylist(@NonNull List<MediaItem2> list,
            @Nullable MediaMetadata2 metadata);

    /**
     * Returns the playlist metadata
     *
     * @return metadata metadata of the playlist, or null if none is set
     */
    public abstract @Nullable MediaMetadata2 getPlaylistMetadata();

    /**
     * Updates the playlist metadata.
     * <p>
     * When the playlist metadata is changed, call {@link #notifyPlaylistMetadataChanged()} to
     * notify changes to the registered callbacks.
     *
     * @param metadata metadata of the playlist
     * @see #notifyPlaylistMetadataChanged()
     */
    public abstract void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata);

    /**
     * Returns currently playing media item.
     */
    public abstract MediaItem2 getCurrentMediaItem();

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
    public abstract void addPlaylistItem(int index, @NonNull MediaItem2 item);

    /**
     * Removes the media item from the playlist
     *
     * @param item media item to remove
     */
    public abstract void removePlaylistItem(@NonNull MediaItem2 item);

    /**
     * Replace the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     *
     * @param index the index of the item to replace
     * @param item the new item
     */
    public abstract void replacePlaylistItem(int index, @NonNull MediaItem2 item);

    /**
     * Skips to the the media item, and plays from it.
     *
     * @param item media item to start playing from
     */
    public abstract void skipToPlaylistItem(@NonNull MediaItem2 item);

    /**
     * Skips to the previous item in the playlist.
     */
    public abstract void skipToPreviousItem();

    /**
     * Skips to the next item in the playlist.
     */
    public abstract void skipToNextItem();

    /**
     * Gets the repeat mode
     *
     * @return repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     */
    public abstract @RepeatMode int getRepeatMode();

    /**
     * Sets the repeat mode.
     * <p>
     * When the repeat mode is changed, call {@link #notifyRepeatModeChanged()} to notify changes
     * to the registered callbacks.
     *
     * @param repeatMode repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     * @see #notifyRepeatModeChanged()
     */
    public abstract void setRepeatMode(@RepeatMode int repeatMode);

    /**
     * Gets the shuffle mode
     *
     * @return The shuffle mode
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     */
    public abstract @ShuffleMode int getShuffleMode();

    /**
     * Sets the shuffle mode.
     * <p>
     * When the shuffle mode is changed, call {@link #notifyShuffleModeChanged()} to notify changes
     * to the registered callbacks.
     *
     * @param shuffleMode The shuffle mode
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see #notifyShuffleModeChanged()
     */
    public abstract void setShuffleMode(@ShuffleMode int shuffleMode);

    /**
     * Called by {@link MediaSession2} when it wants to translate {@link DataSourceDesc} from the
     * {@link MediaPlayerInterface.PlayerEventCallback} to the {@link MediaItem2}. Override this
     * method if you want to create {@link DataSourceDesc}s dynamically, instead of specifying them
     * with {@link #setPlaylist(List, MediaMetadata2)}.
     * <p>
     * Session would throw an exception if this returns {@code null} for the dsd from the
     * {@link MediaPlayerInterface.PlayerEventCallback}.
     * <p>
     * Default implementation calls the {@link #getPlaylist()} and searches the {@link MediaItem2}
     * with the {@param dsd}.
     *
     * @param dsd The dsd to query
     * @return A {@link MediaItem2} object in the playlist that matches given {@code dsd}.
     * @throws IllegalArgumentException if {@code dsd} is null
     */
    public @Nullable MediaItem2 getMediaItem(@NonNull DataSourceDesc dsd) {
        if (dsd == null) {
            throw new IllegalArgumentException("dsd shouldn't be null");
        }
        List<MediaItem2> itemList = getPlaylist();
        if (itemList == null) {
            return null;
        }
        for (int i = 0; i < itemList.size(); i++) {
            MediaItem2 item = itemList.get(i);
            if (item != null && item.getDataSourceDesc().equals(dsd)) {
                return item;
            }
        }
        return null;
    }

    private SimpleArrayMap<PlaylistEventCallback, Executor> getCallbacks() {
        SimpleArrayMap<PlaylistEventCallback, Executor> callbacks = new SimpleArrayMap<>();
        synchronized (mLock) {
            callbacks.putAll(mCallbacks);
        }
        return callbacks;
    }

    /**
     * A callback class to receive notifications for events on the media player. See
     * {@link MediaPlaylistAgent#registerPlaylistEventCallback(Executor, PlaylistEventCallback)}
     * to register this callback.
     */
    public abstract static class PlaylistEventCallback {
        /**
         * Called when a playlist is changed.
         *
         * @param playlistAgent playlist agent for this event
         * @param list new playlist
         * @param metadata new metadata
         */
        public void onPlaylistChanged(@NonNull MediaPlaylistAgent playlistAgent,
                @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when a playlist metadata is changed.
         *
         * @param playlistAgent playlist agent for this event
         * @param metadata new metadata
         */
        public void onPlaylistMetadataChanged(@NonNull MediaPlaylistAgent playlistAgent,
                @Nullable MediaMetadata2 metadata) { }

        /**
         * Called when the shuffle mode is changed.
         *
         * @param playlistAgent playlist agent for this event
         * @param shuffleMode repeat mode
         * @see #SHUFFLE_MODE_NONE
         * @see #SHUFFLE_MODE_ALL
         * @see #SHUFFLE_MODE_GROUP
         */
        public void onShuffleModeChanged(@NonNull MediaPlaylistAgent playlistAgent,
                @ShuffleMode int shuffleMode) { }

        /**
         * Called when the repeat mode is changed.
         *
         * @param playlistAgent playlist agent for this event
         * @param repeatMode repeat mode
         * @see #REPEAT_MODE_NONE
         * @see #REPEAT_MODE_ONE
         * @see #REPEAT_MODE_ALL
         * @see #REPEAT_MODE_GROUP
         */
        public void onRepeatModeChanged(@NonNull MediaPlaylistAgent playlistAgent,
                @RepeatMode int repeatMode) { }
    }
}
