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

package androidx.media2.player;

import android.content.Context;
import android.media.MediaDrm;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
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
 */
/* package */ final class ExoPlayerMediaPlayer2Impl extends MediaPlayer2
        implements ExoPlayerWrapper.Listener {

    private static final String TAG = "ExoPlayerMediaPlayer2";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ExoPlayerWrapper mPlayer;

    private final Handler mTaskHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mTaskLock")
    final ArrayDeque<Task> mPendingTasks;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mTaskLock;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mTaskLock")
    Task mCurrentTask;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mLock;
    @GuardedBy("mLock")
    private Pair<Executor, EventCallback> mExecutorAndEventCallback;
    @SuppressWarnings("unused")
    @GuardedBy("mLock")
    private Pair<Executor, DrmEventCallback> mExecutorAndDrmEventCallback;
    @GuardedBy("mLock")
    private HandlerThread mHandlerThread;

    /** Creates a new ExoPlayer wrapper using the specified context. */
    ExoPlayerMediaPlayer2Impl(@NonNull Context context) {
        mHandlerThread = new HandlerThread("ExoMediaPlayer2Thread");
        mHandlerThread.start();
        mPlayer = new ExoPlayerWrapper(
                context.getApplicationContext(),
                /* listener= */ this,
                mHandlerThread.getLooper());
        // Player callbacks will be called on the task handler thread.
        mTaskHandler = new Handler(mPlayer.getLooper());
        mPendingTasks = new ArrayDeque<>();
        mTaskLock = new Object();
        mLock = new Object();
        resetPlayer();
    }

    // Command queue and events implementation.

    @Override
    public Object notifyWhenCommandLabelReached(@NonNull final Object label) {
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

    @Override
    public void setEventCallback(@NonNull Executor executor, @NonNull EventCallback eventCallback) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(eventCallback);
        synchronized (mLock) {
            mExecutorAndEventCallback = Pair.create(executor, eventCallback);
        }
    }

    @Override
    public void clearEventCallback() {
        synchronized (mLock) {
            mExecutorAndEventCallback = null;
        }
    }

    @Override
    public void setDrmEventCallback(@NonNull Executor executor,
            @NonNull DrmEventCallback eventCallback) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(eventCallback);
        synchronized (mLock) {
            mExecutorAndDrmEventCallback = Pair.create(executor, eventCallback);
        }
    }

    @Override
    public void clearDrmEventCallback() {
        synchronized (mLock) {
            mExecutorAndDrmEventCallback = null;
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

    // Player implementation.

    @Override
    public Object setAudioSessionId(final int sessionId) {
        return addTask(new Task(CALL_COMPLETED_SET_AUDIO_SESSION_ID, false) {
            @Override
            void process() {
                mPlayer.setAudioSessionId(sessionId);
            }
        });
    }

    @Override
    public Object setMediaItem(@NonNull final MediaItem item) {
        return addTask(new Task(CALL_COMPLETED_SET_DATA_SOURCE, false) {
            @Override
            void process() {
                mPlayer.setMediaItem(item);
            }
        });
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        return runPlayerCallableBlocking(new Callable<MediaItem>() {
            @Override
            public MediaItem call() throws Exception {
                return mPlayer.getCurrentMediaItem();
            }
        });
    }

    @Override
    public Object prepare() {
        return addTask(new Task(CALL_COMPLETED_PREPARE, true) {
            @Override
            void process() {
                mPlayer.prepare();
            }
        });
    }

    @Override
    public Object play() {
        return addTask(new Task(CALL_COMPLETED_PLAY, false) {
            @Override
            void process() {
                mPlayer.play();
            }
        });
    }

    @Override
    public Object pause() {
        return addTask(new Task(CALL_COMPLETED_PAUSE, false) {
            @Override
            void process() {
                mPlayer.pause();
            }
        });
    }

    @Override
    public Object seekTo(final long msec, final int mode) {
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
    public Object loopCurrent(final boolean loop) {
        return addTask(new Task(CALL_COMPLETED_LOOP_CURRENT, false) {
            @Override
            void process() {
                mPlayer.loopCurrent(loop);
            }
        });
    }

    @Override
    public Object skipToNext() {
        return addTask(new Task(CALL_COMPLETED_SKIP_TO_NEXT, false) {
            @Override
            void process() {
                mPlayer.skipToNext();
            }
        });
    }

    @Override
    public Object setNextMediaItem(@NonNull final MediaItem item) {
        return addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCE, false) {
            @Override
            void process() {
                mPlayer.setNextMediaItem(item);
            }
        });
    }

    @Override
    public Object setNextMediaItems(@NonNull final List<MediaItem> items) {
        return addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCES, false) {
            @Override
            void process() {
                mPlayer.setNextMediaItems(items);
            }
        });
    }

    @Override
    public Object setAudioAttributes(@NonNull final AudioAttributesCompat attributes) {
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
    public int getAudioSessionId() {
        return runPlayerCallableBlocking(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return mPlayer.getAudioSessionId();
            }
        });
    }

    @Override
    public Object attachAuxEffect(final int effectId) {
        return addTask(new Task(CALL_COMPLETED_ATTACH_AUX_EFFECT, false) {
            @Override
            void process() {
                mPlayer.attachAuxEffect(effectId);
            }
        });
    }

    @Override
    public Object setAuxEffectSendLevel(final float auxEffectSendLevel) {
        return addTask(new Task(CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL, false) {
            @Override
            void process() {
                mPlayer.setAuxEffectSendLevel(auxEffectSendLevel);
            }
        });
    }

    @Override
    public Object setPlaybackParams(@NonNull final PlaybackParams params) {
        return addTask(new Task(CALL_COMPLETED_SET_PLAYBACK_PARAMS, false) {
            @Override
            void process() {
                mPlayer.setPlaybackParams(params);
            }
        });
    }

    @Override
    @NonNull
    public PlaybackParams getPlaybackParams() {
        return runPlayerCallableBlocking(new Callable<PlaybackParams>() {
            @Override
            public PlaybackParams call() throws Exception {
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
    public Object setSurface(final Surface surface) {
        return addTask(new Task(CALL_COMPLETED_SET_SURFACE, false) {
            @Override
            void process() {
                mPlayer.setSurface(surface);
            }
        });
    }

    @Override
    public Object setPlayerVolume(final float volume) {
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
    @NonNull
    public List<TrackInfo> getTracks() {
        return runPlayerCallableBlocking(new Callable<List<TrackInfo>>() {
            @Override
            public List<TrackInfo> call() throws Exception {
                return mPlayer.getTracks();
            }
        });
    }

    @Override
    @Nullable
    public TrackInfo getSelectedTrack(final int trackType) {
        return runPlayerCallableBlocking(new Callable<TrackInfo>() {
            @Override
            public TrackInfo call() {
                return mPlayer.getSelectedTrack(trackType);
            }
        });
    }

    @Override
    @NonNull
    public Object selectTrack(final int trackId) {
        return addTask(new Task(CALL_COMPLETED_SELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.selectTrack(trackId);
            }
        });
    }

    @Override
    @NonNull
    public Object deselectTrack(final int trackId) {
        return addTask(new Task(CALL_COMPLETED_DESELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.deselectTrack(trackId);
            }
        });
    }

    @Override
    @RequiresApi(21)
    public PersistableBundle getMetrics() {
        return runPlayerCallableBlocking(new Callable<PersistableBundle>() {
            @Override
            public PersistableBundle call() throws Exception {
                return mPlayer.getMetricsV21();
            }
        });
    }

    @Override
    public MediaTimestamp getTimestamp() {
        return runPlayerCallableBlocking(new Callable<MediaTimestamp>() {
            @Override
            public MediaTimestamp call() {
                return mPlayer.getTimestamp();
            }
        });
    }

    @Override
    public void reset() {
        clearPendingCommands();

        // Make sure that the current task finishes.
        Task currentTask;
        synchronized (mTaskLock) {
            currentTask = mCurrentTask;
        }
        if (currentTask != null) {
            synchronized (currentTask) {
                try {
                    while (!currentTask.mDone) {
                        currentTask.wait();
                    }
                } catch (InterruptedException e) {
                    // Suppress interruption.
                }
            }
        }
        runPlayerCallableBlocking(new Callable<Void>() {
            @Override
            public Void call() {
                mPlayer.reset();
                return null;
            }
        });
    }

    @Override
    public void close() {
        clearEventCallback();
        HandlerThread handlerThread;
        synchronized (mLock) {
            handlerThread = mHandlerThread;
            if (handlerThread == null) {
                return;
            }
            mHandlerThread = null;
        }
        final ResolvableFuture<Void> future = ResolvableFuture.create();
        mTaskHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mPlayer.close();
                    future.set(null);
                } catch (Throwable e) {
                    future.setException(e);
                }
            }
        });
        getPlayerFuture(future);
        handlerThread.quit();
    }

    @Override
    public void setOnDrmConfigHelper(OnDrmConfigHelper listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DrmInfo getDrmInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object prepareDrm(@NonNull final UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releaseDrm() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public MediaDrm.KeyRequest getDrmKeyRequest(byte[] keySetId, byte[] initData, String mimeType,
            int keyType, Map<String, String> optionalParameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] provideDrmKeyResponse(@Nullable byte[] keySetId, @NonNull byte[] response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreDrmKeys(@NonNull byte[] keySetId) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDrmPropertyString(@NonNull String propertyName, @NonNull String value) {
        throw new UnsupportedOperationException();
    }

    // ExoPlayerWrapper.Listener implementation.

    @Override
    public void onPrepared(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_PREPARED);
        synchronized (mTaskLock) {
            if (mCurrentTask != null
                    && mCurrentTask.mMediaCallType == CALL_COMPLETED_PREPARE
                    && ObjectsCompat.equals(mCurrentTask.mDSD, mediaItem)
                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                mCurrentTask = null;
                processPendingTask();
            }
        }
    }

    @Override
    public void onTracksChanged(@NonNull final List<TrackInfo> tracks) {
        notifyMediaPlayer2Event(cb -> cb.onTracksChanged(ExoPlayerMediaPlayer2Impl.this,
                tracks));
    }

    @Override
    public void onSeekCompleted() {
        synchronized (mTaskLock) {
            if (mCurrentTask != null
                    && mCurrentTask.mMediaCallType == CALL_COMPLETED_SEEK_TO
                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                mCurrentTask = null;
                processPendingTask();
            }
        }
    }

    @Override
    public void onBufferingStarted(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_BUFFERING_START);
    }

    @Override
    public void onBufferingEnded(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_BUFFERING_END);
    }

    @Override
    public void onBufferingUpdate(MediaItem mediaItem, int bufferingPercentage) {
        notifyOnInfo(mediaItem, MEDIA_INFO_BUFFERING_UPDATE, bufferingPercentage);
    }

    @Override
    public void onBandwidthSample(MediaItem mediaItem, int bitrateKbps) {
        notifyOnInfo(mediaItem, MEDIA_INFO_NETWORK_BANDWIDTH, bitrateKbps);
    }

    @Override
    public void onVideoRenderingStart(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_VIDEO_RENDERING_START);
    }

    @Override
    public void onVideoSizeChanged(final MediaItem mediaItem, final int width, final int height) {
        notifyMediaPlayer2Event(new ExoPlayerMediaPlayer2Impl.Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                callback.onVideoSizeChanged(
                        ExoPlayerMediaPlayer2Impl.this,
                        mediaItem,
                        width,
                        height);
            }
        });
    }

    @Override
    public void onSubtitleData(@NonNull final MediaItem mediaItem, @NonNull final TrackInfo track,
            @NonNull final SubtitleData subtitleData) {
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(EventCallback cb) {
                cb.onSubtitleData(
                        ExoPlayerMediaPlayer2Impl.this, mediaItem, track, subtitleData);
            }
        });
    }

    @Override
    public void onTimedMetadata(final MediaItem mediaItem, final TimedMetaData timedMetaData) {
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(EventCallback cb) {
                cb.onTimedMetaDataAvailable(
                        ExoPlayerMediaPlayer2Impl.this, mediaItem, timedMetaData);
            }
        });
    }

    @Override
    public void onMediaItemStartedAsNext(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_DATA_SOURCE_START);
    }

    @Override
    public void onMediaItemEnded(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_DATA_SOURCE_END);
    }

    @Override
    public void onLoop(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_DATA_SOURCE_REPEAT);
    }

    @Override
    public void onMediaTimeDiscontinuity(
            final MediaItem mediaItem, final MediaTimestamp mediaTimestamp) {
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(EventCallback cb) {
                cb.onMediaTimeDiscontinuity(
                        ExoPlayerMediaPlayer2Impl.this, mediaItem, mediaTimestamp);
            }
        });
    }

    @Override
    public void onPlaybackEnded(MediaItem mediaItem) {
        notifyOnInfo(mediaItem, MEDIA_INFO_DATA_SOURCE_LIST_END);
    }

    @Override
    public void onError(final MediaItem mediaItem, final int what) {
        synchronized (mTaskLock) {
            if (mCurrentTask != null
                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                mCurrentTask.sendCompleteNotification(CALL_STATUS_ERROR_UNKNOWN);
                mCurrentTask = null;
                processPendingTask();
            }
        }
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(EventCallback cb) {
                cb.onError(ExoPlayerMediaPlayer2Impl.this, mediaItem, what, /* extra= */ 0);
            }
        });
    }

    // Internal functionality.

    private void notifyOnInfo(MediaItem mediaItem, int what) {
        notifyOnInfo(mediaItem, what, /* extra= */ 0);
    }

    private void notifyOnInfo(final MediaItem mediaItem, final int what, final int extra) {
        notifyMediaPlayer2Event(new ExoPlayerMediaPlayer2Impl.Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2.EventCallback callback) {
                callback.onInfo(ExoPlayerMediaPlayer2Impl.this, mediaItem, what, extra);
            }
        });
    }

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
     * <p>Note: ExoPlayer methods do not block (except {@link Player#release}, which needs to
     * block until resources have been released) so the caller thread will not be blocked for a
     * substantial amount of time.
     */
    private <T> T runPlayerCallableBlocking(final Callable<T> callable) {
        final ResolvableFuture<T> future = ResolvableFuture.create();
        synchronized (mLock) {
            Preconditions.checkNotNull(mHandlerThread);
            boolean success = mTaskHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        future.set(callable.call());
                    } catch (Throwable e) {
                        future.setException(e);
                    }
                }
            });
            Preconditions.checkState(success);
        }
        return getPlayerFuture(future);
    }

    private static <T> T getPlayerFuture(ResolvableFuture<T> future) {
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
            Log.e(TAG, "Internal player error", new RuntimeException(cause));
            throw new IllegalStateException(cause);
        }
    }

    private interface Mp2EventNotifier {
        void notify(EventCallback callback);
    }

    private abstract class Task implements Runnable {
        final int mMediaCallType;
        final boolean mNeedToWaitForEventToComplete;

        MediaItem mDSD;
        @GuardedBy("this")
        boolean mDone;

        Task(int mediaCallType, boolean needToWaitForEventToComplete) {
            mMediaCallType = mediaCallType;
            mNeedToWaitForEventToComplete = needToWaitForEventToComplete;
        }

        abstract void process() throws IOException, NoDrmSchemeException;

        @Override
        public void run() {
            int status = CALL_STATUS_NO_ERROR;
            boolean skip = false;
            if (mMediaCallType == CALL_COMPLETED_SEEK_TO) {
                synchronized (mTaskLock) {
                    Task next = mPendingTasks.peekFirst();
                    if (next != null && next.mMediaCallType == CALL_COMPLETED_SEEK_TO) {
                        skip = true;
                    }
                }
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
            // reset() might be waiting for this task. Notify that the task is done.
            synchronized (this) {
                mDone = true;
                notifyAll();
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
