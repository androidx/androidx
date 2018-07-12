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

package androidx.mediarouter.media;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;
import androidx.media2.BaseRemoteMediaPlayerConnector;
import androidx.media2.DataSourceDesc2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaPlayerConnector.PlayerEventCallback;
import androidx.media2.MediaSession2;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import java.util.concurrent.Executor;

/**
 * RouteMediaPlayerConnector is the abstract class representing playback happens on the outside of
 * the device through {@link MediaRouter}
 * <p>
 * If you use this to the {@link MediaSession2} followings would happen.
 * <ul>
 *     <li>Session wouldn't handle audio focus</li>
 *     <li>Session would dispatch volume change event to the player instead of changing device
 *         volume</li>
 * </ul>
 */
public abstract class RouteMediaPlayerConnector extends BaseRemoteMediaPlayerConnector {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<PlayerEventCallback, Executor> mCallbacks = new ArrayMap<>();

    @GuardedBy("mLock")
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    RouteInfo mRoute;

    /**
     * Updates routes info. If the same route has already set, it only notifies volume changes.
     *
     * @param route route
     */
    public final void updateRouteInfo(@Nullable RouteInfo route) {
        synchronized (mLock) {
            if (mRoute != route) {
                mHandler.removeCallbacksAndMessages(null);
                mRoute = route;
            } else {
                notifyPlayerVolumeChanged();
            }
        }
    }

    @Override
    public final float getMaxPlayerVolume() {
        synchronized (mLock) {
            if (mRoute != null) {
                return mRoute.getVolumeMax();
            }
        }
        return 1.0f;
    }

    @Override
    public final float getPlayerVolume() {
        synchronized (mLock) {
            if (mRoute != null) {
                return mRoute.getVolume();
            }
        }
        return 1.0f;
    }

    @Override
    public final void adjustPlayerVolume(final int direction) {
        synchronized (mLock) {
            if (mRoute != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            if (mRoute != null) {
                                mRoute.requestUpdateVolume(direction);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public final void setPlayerVolume(final float volume) {
        synchronized (mLock) {
            if (mRoute != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mLock) {
                            if (mRoute != null) {
                                mRoute.requestSetVolume((int) (volume + 0.5f));
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public final @VolumeControlType int getVolumeControlType() {
        synchronized (mLock) {
            if (mRoute != null) {
                switch (mRoute.getVolumeHandling()) {
                    case RouteInfo.PLAYBACK_VOLUME_FIXED:
                        return VOLUME_CONTROL_FIXED;
                    case RouteInfo.PLAYBACK_VOLUME_VARIABLE:
                        return VOLUME_CONTROL_ABSOLUTE;
                }
            }
        }
        return VOLUME_CONTROL_FIXED;
    }

    /**
     * Adds a callback to be notified of events for this player.
     *
     * @param executor the {@link Executor} to be used for the events.
     * @param callback the callback to receive the events.
     */
    @Override
    public final void registerPlayerEventCallback(@NonNull Executor executor,
            @NonNull PlayerEventCallback callback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        synchronized (mLock) {
            mCallbacks.put(callback, executor);
        }
    }

    /**
     * Removes a previously registered callback for player events.
     *
     * @param callback the callback to remove
     */
    @Override
    public final void unregisterPlayerEventCallback(@NonNull PlayerEventCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback shouldn't be null");
        }
        synchronized (mLock) {
            mCallbacks.remove(callback);
        }
    }


    /**
     * Notifies the current data source. Call this API when the current data soruce is changed.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onCurrentDataSourceChanged(MediaPlayerConnector, DataSourceDesc2)}
     * .
     */
    public final void notifyCurrentDataSourceChanged() {
        notifyCurrentDataSourceChanged(getCurrentDataSource());
    }

    /**
     * Notifies that the playback is completed. Call this API when no other source is about to be
     * played next (i.e. playback reached the end of the list of sources to play).
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onCurrentDataSourceChanged(MediaPlayerConnector, DataSourceDesc2)}
     * with {@code null} {@link DataSourceDesc2}.
     */
    public final void notifyPlaybackCompleted() {
        notifyCurrentDataSourceChanged(null);
    }

    private void notifyCurrentDataSourceChanged(final DataSourceDesc2 dsd) {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onCurrentDataSourceChanged(RouteMediaPlayerConnector.this, dsd);
                }
            });
        }
    }

    /**
     * Notifies that a data source is prepared.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onCurrentDataSourceChanged(MediaPlayerConnector, DataSourceDesc2)}
     * with {@code null} {@link DataSourceDesc2}.
     *
     * @param dsd prepared dsd
     */
    public final void notifyMediaPrepared(final DataSourceDesc2 dsd) {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onMediaPrepared(RouteMediaPlayerConnector.this, dsd);
                }
            });
        }
    }

    /**
     * Notifies the current player state. Call this API when the current player state is changed.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onPlayerStateChanged(MediaPlayerConnector, int)}.
     */
    public final void notifyPlayerStateChanged() {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        final @PlayerState int state = getPlayerState();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStateChanged(RouteMediaPlayerConnector.this, state);
                }
            });
        }
    }

    /**
     * Notifies that buffering state of a data source is changed.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onBufferingStateChanged(MediaPlayerConnector, DataSourceDesc2,
     * int)}.
     *
     * @param dsd dsd to notify
     * @param state new buffering state
     */
    public final void notifyBufferingStateChanged(final DataSourceDesc2 dsd,
            final @BuffState int state) {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onBufferingStateChanged(RouteMediaPlayerConnector.this, dsd, state);
                }
            });
        }
    }

    /**
     * Notifies the current playback speed. Call this API when the current playback speed is
     * changed.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onPlaybackSpeedChanged(MediaPlayerConnector, float)}.
     */
    public final void notifyPlaybackSpeedChanged() {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        final float speed = getPlaybackSpeed();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackSpeedChanged(RouteMediaPlayerConnector.this, speed);
                }
            });
        }
    }

    /**
     * Notifies that buffering state of a data source is changed.
     * <p>
     * Registered {@link PlayerEventCallback} would receive this event through the
     * {@link PlayerEventCallback#onSeekCompleted(MediaPlayerConnector, long)}
     *
     * @param position seek position. May be differ with the position specified by
     *                 {@link #seekTo(long)}.
     */
    public final void notifySeekCompleted(final long position) {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        for (int i = 0; i < callbacks.size(); i++) {
            final PlayerEventCallback callback = callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSeekCompleted(RouteMediaPlayerConnector.this, position);
                }
            });
        }
    }

    /**
     * Notifies the current player volume. Call this API when the current playback volume is
     * changed.
     */
    public final void notifyPlayerVolumeChanged() {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = getCallbacks();
        final float volume = getPlayerVolume();
        for (int i = 0; i < callbacks.size(); i++) {
            if (!(callbacks.keyAt(i) instanceof RemotePlayerEventCallback)) {
                continue;
            }
            final RemotePlayerEventCallback callback =
                    (RemotePlayerEventCallback) callbacks.keyAt(i);
            final Executor executor = callbacks.valueAt(i);
            if (callback instanceof RemotePlayerEventCallback) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onPlayerVolumeChanged(RouteMediaPlayerConnector.this, volume);
                    }
                });
            }
        }
    }

    private SimpleArrayMap<PlayerEventCallback, Executor> getCallbacks() {
        SimpleArrayMap<PlayerEventCallback, Executor> callbacks = new SimpleArrayMap<>();
        synchronized (mLock) {
            callbacks.putAll(mCallbacks);
        }
        return callbacks;
    }
}
