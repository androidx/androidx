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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDrm;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.SettableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaItem2;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaTimestamp2;
import androidx.media2.PlaybackParams2;
import androidx.media2.exoplayer.external.Player;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * An implementation of {@link MediaPlayer2} based on a repackaged version of ExoPlayer.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
public final class ExoPlayerMediaPlayer2Impl extends MediaPlayer2
        implements ExoPlayerWrapper.Listener {

    private static final String TAG = "ExoPlayerMediaPlayer2";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ExoPlayerWrapper mPlayer;

    private final Handler mTaskHandler;
    @GuardedBy("mTaskLock")
    private final ArrayDeque<Task> mPendingTasks;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mTaskLock;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mTaskLock")
    Task mCurrentTask;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mLock;
    @GuardedBy("mLock")
    private ArrayMap<MediaPlayerConnector.PlayerEventCallback, Executor>
            mExecutorByPlayerEventCallback;
    @GuardedBy("mLock")
    private Pair<Executor, EventCallback> mExecutorAndEventCallback;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaPlayerConnector mMediaPlayerConnectorImpl;

    /** Creates a new ExoPlayer wrapper using the specified context. */
    public ExoPlayerMediaPlayer2Impl(@NonNull Context context) {
        mPlayer = new ExoPlayerWrapper(context.getApplicationContext(), /* listener= */ this);
        // Player callbacks will be called on the task handler thread.
        mTaskHandler = new Handler(mPlayer.getLooper());
        mPendingTasks = new ArrayDeque<>();
        mTaskLock = new Object();
        mExecutorByPlayerEventCallback = new ArrayMap<>();
        mLock = new Object();
        resetPlayer();
    }

    // Command queue and events implementation.
    // TODO: Consider refactoring to share implementation with MediaPlayer2Impl.

    @Override
    public void notifyWhenCommandLabelReached(final Object label) {
        _notifyWhenCommandLabelReached(label);
    }

    @Override
    public Object _notifyWhenCommandLabelReached(final Object label) {
        return addTask(new Task(CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED, false) {
            @Override
            void process() {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onCommandLabelReached(ExoPlayerMediaPlayer2Impl.this, label);
                    }
                });
            }
        });
    }

    @Override
    public void clearPendingCommands() {
        synchronized (mTaskLock) {
            mPendingTasks.clear();
        }
    }

    @Override
    public boolean cancel(Object token) {
        synchronized (mTaskLock) {
            return mPendingTasks.remove(token);
        }
    }

    private Object addTask(Task task) {
        synchronized (mTaskLock) {
            if (task.mMediaCallType == MediaPlayer2.CALL_COMPLETED_SEEK_TO) {
                Task previous = mPendingTasks.peekLast();
                if (previous != null && previous.mMediaCallType == task.mMediaCallType) {
                    // Skip the unnecessary previous seek command.
                    previous.mSkip = true;
                }
            }
            mPendingTasks.add(task);
            processPendingTask();
        }
        return task;
    }

    @GuardedBy("mTaskLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void processPendingTask() {
        if (mCurrentTask == null && !mPendingTasks.isEmpty()) {
            Task task = mPendingTasks.removeFirst();
            mCurrentTask = task;
            mTaskHandler.post(task);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void registerPlayerEventCallback(@NonNull Executor executor,
            @NonNull MediaPlayerConnector.PlayerEventCallback callback) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(executor);
        synchronized (mLock) {
            mExecutorByPlayerEventCallback.put(callback, executor);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void unregisterPlayerEventCallback(@NonNull MediaPlayerConnector.PlayerEventCallback callback) {
        Preconditions.checkNotNull(callback);
        synchronized (mLock) {
            mExecutorByPlayerEventCallback.remove(callback);
        }
    }

    @Override
    public void setEventCallback(@NonNull Executor executor, @NonNull EventCallback eventCallback) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(eventCallback);
        synchronized (mLock) {
            mExecutorAndEventCallback = Pair.create(executor, eventCallback);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyMediaPlayer2Event(final Mp2EventNotifier notifier) {
        final Pair<Executor, EventCallback> executorAndEventCallback;
        synchronized (mLock) {
            executorAndEventCallback = mExecutorAndEventCallback;
        }
        if (executorAndEventCallback != null) {
            Executor executor = executorAndEventCallback.first;
            final EventCallback eventCallback = executorAndEventCallback.second;
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.notify(eventCallback);
                    }
                });
            } catch (RejectedExecutionException e) {
                // The given executor is shutting down.
                Log.w(TAG, "The given executor is shutting down. Ignoring the player event.");
            }
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"}) /* synthetic access */
    void notifyPlayerEvent(final PlayerEventNotifier notifier) {
        ArrayMap<MediaPlayerConnector.PlayerEventCallback, Executor> map;
        synchronized (mLock) {
            map = new ArrayMap<>(mExecutorByPlayerEventCallback);
        }
        final int callbackCount = map.size();
        for (int i = 0; i < callbackCount; i++) {
            final Executor executor = map.valueAt(i);
            final MediaPlayerConnector.PlayerEventCallback eventCallback = map.keyAt(i);
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.notify(eventCallback);
                    }
                });
            } catch (RejectedExecutionException e) {
                // The given executor is shutting down.
                Log.w(TAG, "The given executor is shutting down. Ignoring the player event.");
            }
        }
    }

    // Player implementation.

    @Override
    public MediaPlayerConnector getMediaPlayerConnector() {
        synchronized (mLock) {
            if (mMediaPlayerConnectorImpl == null) {
                mMediaPlayerConnectorImpl = new ExoPlayerMediaPlayerConnector();
            }
            return mMediaPlayerConnectorImpl;
        }
    }

    @Override
    public void setMediaItem(final MediaItem2 item) {
        _setMediaItem(item);
    }

    @Override
    public Object _setMediaItem(final MediaItem2 item) {
        return addTask(new Task(CALL_COMPLETED_SET_DATA_SOURCE, false) {
            @Override
            void process() {
                mPlayer.setMediaItem(item);
            }
        });
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return runPlayerCallableBlocking(new Callable<MediaItem2>() {
            @Override
            public MediaItem2 call() throws Exception {
                return mPlayer.getCurrentMediaItem();
            }
        });
    }

    @Override
    public void prepare() {
        _prepare();
    }

    @Override
    public Object _prepare() {
        return addTask(new Task(CALL_COMPLETED_PREPARE, true) {
            @Override
            void process() {
                mPlayer.prepare();
            }
        });
    }

    @Override
    public void play() {
        _play();
    }

    @Override
    public Object _play() {
        return addTask(new Task(CALL_COMPLETED_PLAY, false) {
            @Override
            void process() {
                mPlayer.play();
            }
        });
    }

    @Override
    public void pause() {
        _pause();
    }

    @Override
    public Object _pause() {
        return addTask(new Task(CALL_COMPLETED_PAUSE, false) {
            @Override
            void process() {
                mPlayer.pause();
            }
        });
    }

    @Override
    public void seekTo(long msec, int mode) {
        _seekTo(msec, mode);
    }

    @Override
    public Object _seekTo(final long msec, final int mode) {
        return addTask(new Task(CALL_COMPLETED_SEEK_TO, true) {
            @Override
            void process() {
                mPlayer.seekTo(msec, mode);
            }
        });
    }

    @Override
    public long getCurrentPosition() {
        return runPlayerCallableBlocking(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return mPlayer.getCurrentPosition();
            }
        });
    }

    @Override
    public long getDuration() {
        return runPlayerCallableBlocking(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return mPlayer.getDuration();
            }
        });
    }

    @Override
    public long getBufferedPosition() {
        return runPlayerCallableBlocking(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return mPlayer.getBufferedPosition();
            }
        });
    }

    @Override
    public @MediaPlayer2.MediaPlayer2State int getState() {
        return runPlayerCallableBlocking(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mPlayer.getState();
            }
        });
    }

    @Override
    public void loopCurrent(final boolean loop) {
        _loopCurrent(loop);
    }

    @Override
    public Object _loopCurrent(final boolean loop) {
        return addTask(new Task(CALL_COMPLETED_LOOP_CURRENT, false) {
            @Override
            void process() {
                mPlayer.loopCurrent(loop);
            }
        });
    }

    @Override
    public void setAudioAttributes(final AudioAttributesCompat attributes) {
        _setAudioAttributes(attributes);
    }

    @Override
    public Object _setAudioAttributes(final AudioAttributesCompat attributes) {
        return addTask(new Task(CALL_COMPLETED_SET_AUDIO_ATTRIBUTES, false) {
            @Override
            void process() {
                mPlayer.setAudioAttributes(attributes);
            }
        });
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return runPlayerCallableBlocking(new Callable<AudioAttributesCompat>() {
            @Override
            public AudioAttributesCompat call() throws Exception {
                return mPlayer.getAudioAttributes();
            }
        });
    }

    @Override
    public void attachAuxEffect(int effectId) {
        _attachAuxEffect(effectId);
    }

    @Override
    public Object _attachAuxEffect(final int effectId) {
        return addTask(new Task(CALL_COMPLETED_ATTACH_AUX_EFFECT, false) {
            @Override
            void process() {
                mPlayer.attachAuxEffect(effectId);
            }
        });
    }

    @Override
    public void setAuxEffectSendLevel(float level) {
        _setAuxEffectSendLevel(level);
    }

    @Override
    public Object _setAuxEffectSendLevel(final float auxEffectSendLevel) {
        return addTask(new Task(CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL, false) {
            @Override
            void process() {
                mPlayer.setAuxEffectSendLevel(auxEffectSendLevel);
            }
        });
    }

    @Override
    public void setPlaybackParams(PlaybackParams2 params) {
        _setPlaybackParams(params);
    }

    @Override
    public Object _setPlaybackParams(final PlaybackParams2 params) {
        return addTask(new Task(CALL_COMPLETED_SET_PLAYBACK_PARAMS, false) {
            @Override
            void process() {
                mPlayer.setPlaybackParams(params);
            }
        });
    }

    @Override
    public PlaybackParams2 getPlaybackParams() {
        return runPlayerCallableBlocking(new Callable<PlaybackParams2>() {
            @Override
            public PlaybackParams2 call() throws Exception {
                return mPlayer.getPlaybackParams();
            }
        });
    }

    @Override
    public int getVideoWidth() {
        return runPlayerCallableBlocking(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mPlayer.getVideoWidth();
            }
        });
    }

    @Override
    public int getVideoHeight() {
        return runPlayerCallableBlocking(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mPlayer.getVideoHeight();
            }
        });
    }

    @Override
    public void setSurface(final Surface surface) {
        _setSurface(surface);
    }

    @Override
    public Object _setSurface(final Surface surface) {
        return addTask(new Task(CALL_COMPLETED_SET_SURFACE, false) {
            @Override
            void process() {
                mPlayer.setSurface(surface);
            }
        });
    }

    @Override
    public void setPlayerVolume(final float volume) {
        _setPlayerVolume(volume);
    }

    @Override
    public Object _setPlayerVolume(final float volume) {
        return addTask(new Task(CALL_COMPLETED_SET_PLAYER_VOLUME, false) {
            @Override
            void process() {
                mPlayer.setVolume(volume);
            }
        });
    }

    @Override
    public float getPlayerVolume() {
        return runPlayerCallableBlocking(new Callable<Float>() {
            @Override
            public Float call() throws Exception {
                return mPlayer.getVolume();
            }
        });
    }

    @Override
    public List<TrackInfo> getTrackInfo() {
        return runPlayerCallableBlocking(new Callable<List<TrackInfo>>() {
            @Override
            public List<TrackInfo> call() throws Exception {
                return mPlayer.getTrackInfo();
            }
        });
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PersistableBundle getMetrics() {
        if (Build.VERSION.SDK_INT < 21) {
            // TODO(b/80232248): Check what to do for pre-Android L builds.
            throw new UnsupportedOperationException();
        }
        return runPlayerCallableBlocking(new Callable<PersistableBundle>() {
            @Override
            public PersistableBundle call() throws Exception {
                return mPlayer.getMetricsV21();
            }
        });
    }

    @Override
    public void reset() {
        runPlayerCallableBlocking(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mPlayer.reset();
                return null;
            }
        });
    }

    @Override
    public void close() {
        synchronized (mLock) {
            mExecutorByPlayerEventCallback.clear();
            mExecutorAndEventCallback = null;
            runPlayerCallableBlocking(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    mPlayer.close();
                    return null;
                }
            });
        }
    }

    @Override
    public void skipToNext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _skipToNext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextMediaItem(MediaItem2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _setNextMediaItem(MediaItem2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getNextMediaItems(List<MediaItem2> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _setNextMediaItems(List<MediaItem2> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaTimestamp2 getTimestamp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAudioSessionId(int sessionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _setAudioSessionId(final int sessionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getAudioSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSelectedTrack(int trackType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void selectTrack(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _selectTrack(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deselectTrack(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object _deselectTrack(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearEventCallback() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDrmEventCallback(Executor executor,
            DrmEventCallback eventCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearDrmEventCallback() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DrmInfo getDrmInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void prepareDrm(UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releaseDrm() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaDrm.KeyRequest getDrmKeyRequest(byte[] keySetId, byte[] initData, String mimeType,
            int keyType, Map<String, String> optionalParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] provideDrmKeyResponse(byte[] keySetId, byte[] response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreDrmKeys(byte[] keySetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDrmPropertyString(String propertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDrmPropertyString(String propertyName, String value) {
        throw new UnsupportedOperationException();
    }

    // ExoPlayerWrapper.Listener implementation.

    @Override
    public void onPrepared(final MediaItem2 mediaItem2) {
        notifyMediaPlayer2Event(new ExoPlayerMediaPlayer2Impl.Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                MediaPlayer2 mediaPlayer2 = ExoPlayerMediaPlayer2Impl.this;
                callback.onInfo(
                        mediaPlayer2,
                        mediaItem2,
                        MEDIA_INFO_PREPARED,
                        /* extra= */ 0);
            }
        });
        notifyPlayerEvent(new ExoPlayerMediaPlayer2Impl.PlayerEventNotifier() {
            @Override
            public void notify(MediaPlayerConnector.PlayerEventCallback cb) {
                cb.onMediaPrepared(getMediaPlayerConnector(), mediaItem2);
            }
        });
        synchronized (mTaskLock) {
            if (mCurrentTask != null
                    && mCurrentTask.mMediaCallType == CALL_COMPLETED_PREPARE
                    && ObjectsCompat.equals(mCurrentTask.mDSD, mediaItem2)
                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                mCurrentTask = null;
                processPendingTask();
            }
        }
    }

    @Override
    public void onSeekCompleted(long positionMs) {
        synchronized (mTaskLock) {
            if (mCurrentTask != null
                    && mCurrentTask.mMediaCallType == CALL_COMPLETED_SEEK_TO
                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                mCurrentTask = null;
                processPendingTask();
            }
        }
        final long seekPositionMs = mPlayer.getCurrentPosition();
        notifyPlayerEvent(new ExoPlayerMediaPlayer2Impl.PlayerEventNotifier() {
            @Override
            public void notify(MediaPlayerConnector.PlayerEventCallback cb) {
                final MediaPlayerConnector connector;
                synchronized (mLock) {
                    connector = mMediaPlayerConnectorImpl;
                }
                cb.onSeekCompleted(connector, seekPositionMs);
            }
        });
    }

    @Override
    public void onVideoRenderingStart(final MediaItem2 mediaItem2) {
        notifyMediaPlayer2Event(new ExoPlayerMediaPlayer2Impl.Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                MediaPlayer2 mediaPlayer2 = ExoPlayerMediaPlayer2Impl.this;
                callback.onInfo(
                        mediaPlayer2,
                        mediaItem2,
                        MEDIA_INFO_VIDEO_RENDERING_START,
                        /* extra= */ 0);
            }
        });
    }

    @Override
    public void onVideoSizeChanged(final MediaItem2 mediaItem2, final int width, final int height) {
        notifyMediaPlayer2Event(new ExoPlayerMediaPlayer2Impl.Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                callback.onVideoSizeChanged(
                        ExoPlayerMediaPlayer2Impl.this, mediaItem2, width, height);
            }
        });
    }

    // Internal functionality.

    private void resetPlayer() {
        runPlayerCallableBlocking(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                mPlayer.reset();
                return null;
            }
        });
    }

    /**
     * Runs the specified callable on the player thread, blocking the calling thread until a result
     * is returned.
     *
     * <p>Note: ExoPlayer methods apart from {@link Player#release} are asynchronous, so calling
     * player methods will not block the caller thread for a substantial amount of time.
     */
    private <T> T runPlayerCallableBlocking(final Callable<T> callable) {
        final SettableFuture<T> future = SettableFuture.create();
        mTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    future.set(callable.call());
                } catch (Throwable e) {
                    future.setException(e);
                }
            }
        });
        try {
            T result;
            boolean wasInterrupted = false;
            while (true) {
                try {
                    result = future.get();
                    break;
                } catch (InterruptedException e) {
                    // We always wait for player calls to return.
                    wasInterrupted = true;
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
            return result;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            Log.e(TAG, "Internal player error", cause);
            throw new IllegalStateException(cause);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ExoPlayerMediaPlayerConnector extends MediaPlayerConnector {

        @Override
        public void prepare() {
            ExoPlayerMediaPlayer2Impl.this.prepare();
        }

        @Override
        public void play() {
            ExoPlayerMediaPlayer2Impl.this.play();
        }

        @Override
        public void pause() {
            ExoPlayerMediaPlayer2Impl.this.pause();
        }

        @Override
        public void reset() {
            ExoPlayerMediaPlayer2Impl.this.reset();
        }

        @Override
        public void skipToNext() {
            ExoPlayerMediaPlayer2Impl.this.skipToNext();
        }

        @Override
        public void seekTo(long pos) {
            ExoPlayerMediaPlayer2Impl.this.seekTo(pos);
        }

        @Override
        public void setAudioAttributes(AudioAttributesCompat attributes) {
            ExoPlayerMediaPlayer2Impl.this.setAudioAttributes(attributes);
        }

        @Override
        public AudioAttributesCompat getAudioAttributes() {
            return ExoPlayerMediaPlayer2Impl.this.getAudioAttributes();
        }

        @Override
        public void setMediaItem(MediaItem2 item) {
            ExoPlayerMediaPlayer2Impl.this.setMediaItem(item);
        }

        @Override
        public void setNextMediaItem(MediaItem2 item) {
            ExoPlayerMediaPlayer2Impl.this.setNextMediaItem(item);
        }

        @Override
        public void setNextMediaItems(List<MediaItem2> items) {
            ExoPlayerMediaPlayer2Impl.this.getNextMediaItems(items);
        }

        @Override
        public MediaItem2 getCurrentMediaItem() {
            return ExoPlayerMediaPlayer2Impl.this.getCurrentMediaItem();
        }

        @Override
        public void loopCurrent(boolean loop) {
            ExoPlayerMediaPlayer2Impl.this.loopCurrent(loop);
        }

        @Override
        public void setPlayerVolume(float volume) {
            ExoPlayerMediaPlayer2Impl.this.setPlayerVolume(volume);
        }

        @Override
        public float getPlayerVolume() {
            return ExoPlayerMediaPlayer2Impl.this.getPlayerVolume();
        }

        @Override
        public void close() {
            ExoPlayerMediaPlayer2Impl.this.close();
        }

        @Override
        public void registerPlayerEventCallback(Executor executor, PlayerEventCallback callback) {
            ExoPlayerMediaPlayer2Impl.this.registerPlayerEventCallback(executor, callback);
        }

        @Override
        public void unregisterPlayerEventCallback(PlayerEventCallback callback) {
            ExoPlayerMediaPlayer2Impl.this.unregisterPlayerEventCallback(callback);
        }

        @Override
        public int getPlayerState() {
            int state = getState();
            switch (state) {
                case MediaPlayer2.PLAYER_STATE_IDLE:
                    return PLAYER_STATE_IDLE;
                case MediaPlayer2.PLAYER_STATE_PREPARED:
                case MediaPlayer2.PLAYER_STATE_PAUSED:
                    return PLAYER_STATE_PAUSED;
                case MediaPlayer2.PLAYER_STATE_PLAYING:
                    return PLAYER_STATE_PLAYING;
                case MediaPlayer2.PLAYER_STATE_ERROR:
                    return PLAYER_STATE_ERROR;
                default:
                    throw new IllegalStateException();
            }
        }

        // TODO: Implement these methods:

        @Override
        public void setPlaybackSpeed(float speed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBufferingState() {
            return MediaPlayerConnector.BUFFERING_STATE_UNKNOWN;
        }

    }

    private interface Mp2EventNotifier {
        void notify(EventCallback callback);
    }

    private interface PlayerEventNotifier {
        void notify(MediaPlayerConnector.PlayerEventCallback callback);
    }

    private abstract class Task implements Runnable {
        final int mMediaCallType;
        final boolean mNeedToWaitForEventToComplete;

        MediaItem2 mDSD;
        boolean mSkip;

        Task(int mediaCallType, boolean needToWaitForEventToComplete) {
            mMediaCallType = mediaCallType;
            mNeedToWaitForEventToComplete = needToWaitForEventToComplete;
        }

        abstract void process() throws IOException, NoDrmSchemeException;

        @Override
        public void run() {
            int status = CALL_STATUS_NO_ERROR;
            boolean skip;
            synchronized (mTaskLock) {
                skip = mSkip;
            }
            if (!skip) {
                try {
                    if (mMediaCallType != CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED
                            && mPlayer.hasError()) {
                        status = CALL_STATUS_INVALID_OPERATION;
                    } else {
                        process();
                    }
                } catch (IllegalStateException e) {
                    status = CALL_STATUS_INVALID_OPERATION;
                } catch (IllegalArgumentException e) {
                    status = CALL_STATUS_BAD_VALUE;
                } catch (SecurityException e) {
                    status = CALL_STATUS_PERMISSION_DENIED;
                } catch (IOException e) {
                    status = CALL_STATUS_ERROR_IO;
                } catch (Exception e) {
                    status = CALL_STATUS_ERROR_UNKNOWN;
                }
            } else {
                status = CALL_STATUS_SKIPPED;
            }

            mDSD = mPlayer.getCurrentMediaItem();

            if (!mNeedToWaitForEventToComplete || status != CALL_STATUS_NO_ERROR || skip) {
                sendCompleteNotification(status);

                synchronized (mTaskLock) {
                    mCurrentTask = null;
                    processPendingTask();
                }
            }
        }

        void sendCompleteNotification(final int status) {
            if (mMediaCallType >= SEPARATE_CALL_COMPLETE_CALLBACK_START) {
                // These methods have a separate call complete callback and it should be already
                // called within process().
                return;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(EventCallback callback) {
                    callback.onCallCompleted(
                            ExoPlayerMediaPlayer2Impl.this, mDSD, mMediaCallType, status);
                }
            });
        }
    }

}
