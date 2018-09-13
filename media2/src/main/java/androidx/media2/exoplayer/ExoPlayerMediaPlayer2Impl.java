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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaItem2;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaTimestamp2;
import androidx.media2.PlaybackParams2;
import androidx.media2.exoplayer.external.DefaultLoadControl;
import androidx.media2.exoplayer.external.DefaultRenderersFactory;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.Timeline;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoListener;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link MediaPlayer2} based on a repackaged version of ExoPlayer.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
public final class ExoPlayerMediaPlayer2Impl extends MediaPlayer2 {

    private static final String USER_AGENT_NAME = "MediaPlayer2";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull DataSource.Factory mDataSourceFactory;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull Object mPlayerLock;
    @GuardedBy("mPlayerLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull SimpleExoPlayer mPlayer;

    private final HandlerThread mHandlerThread;
    private final Handler mTaskHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mTaskLock;
    @GuardedBy("mTaskLock")
    private final ArrayDeque<Task> mPendingTasks;
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
    private MediaPlayerConnector mMediaPlayerConnectorImpl;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable MediaItem2 mMediaItem;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mVideoWidth;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mVideoHeight;
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mStartPlaybackTimeNs;
    // TODO(b/80232248): Store with the data source it relates to.
    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mPlayingTimeUs;

    // TODO(b/80232248): Implement command queue and make setters notify their callbacks.

    /** Creates a new ExoPlayer wrapper using the specified context. */
    public ExoPlayerMediaPlayer2Impl(@NonNull Context context) {
        context = Preconditions.checkNotNull(context).getApplicationContext();

        mHandlerThread = new HandlerThread("ExoMediaPlayer2TaskThread");
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        mTaskHandler = new Handler(looper);
        mPendingTasks = new ArrayDeque<>();
        mTaskLock = new Object();

        String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
        mDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
        mPlayer = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(context),
                new DefaultTrackSelector(),
                new DefaultLoadControl(),
                /* drmSessionManager= */ null,
                looper);
        ComponentListener listener = new ComponentListener();
        mPlayer.addListener(listener);
        mPlayer.addVideoListener(listener);
        mPlayerLock = new Object();

        mExecutorByPlayerEventCallback = new ArrayMap<>();
        mLock = new Object();
        mStartPlaybackTimeNs = -1;
    }

    // Command queue and events implementation.
    // TODO: Consider refactoring to share implementation with MediaPlayer2Impl.

    @Override
    public void notifyWhenCommandLabelReached(final Object label) {
        addTask(new Task(CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED, false) {
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

    private void addTask(Task task) {
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
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(eventCallback);
                }
            });
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
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(eventCallback);
                }
            });
        }
    }

    // Player implementation.

    @Override
    public void setMediaItem(final MediaItem2 item) {
        addTask(new Task(CALL_COMPLETED_SET_DATA_SOURCE, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(item);
                // TODO(b/80232248): Update the data source queue when it's implemented.
                synchronized (mLock) {
                    mMediaItem = item;
                }
            }
        });
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        synchronized (mLock) {
            return mMediaItem;
        }
    }

    @Override
    public void prepare() {
        addTask(new Task(CALL_COMPLETED_PREPARE, true) {
            @Override
            void process() {
                MediaItem2 item = getCurrentMediaItem();
                MediaSource mediaSource = ExoPlayerUtils.createMediaSource(
                        mDataSourceFactory, item);
                synchronized (mPlayerLock) {
                    mPlayer.prepare(mediaSource);
                }
            }
        });
    }

    @Override
    public void play() {
        addTask(new Task(CALL_COMPLETED_PLAY, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setPlayWhenReady(true);
                }
            }
        });
    }

    @Override
    public void pause() {
        addTask(new Task(CALL_COMPLETED_PAUSE, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setPlayWhenReady(false);
                }
            }
        });
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mPlayerLock) {
            return mPlayer.getCurrentPosition();
        }
    }

    @Override
    public long getDuration() {
        synchronized (mPlayerLock) {
            return mPlayer.getDuration();
        }
    }

    @Override
    public long getBufferedPosition() {
        synchronized (mPlayerLock) {
            return mPlayer.getBufferedPosition();
        }
    }

    @Override
    public @MediaPlayer2.MediaPlayer2State int getState() {
        int state;
        boolean playWhenReady;
        synchronized (mPlayerLock) {
            state = mPlayer.getPlaybackState();
            playWhenReady = mPlayer.getPlayWhenReady();
        }
        // TODO(b/80232248): Return PLAYER_STATE_PREPARED before playback when we have track groups.
        switch (state) {
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
                return PLAYER_STATE_IDLE;
            case Player.STATE_BUFFERING:
                return PLAYER_STATE_PAUSED;
            case Player.STATE_READY:
                return playWhenReady ? PLAYER_STATE_PLAYING : PLAYER_STATE_PAUSED;
            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @MediaPlayerConnector.PlayerState int getPlayerState() {
        int state = getState();
        switch (state) {
            case PLAYER_STATE_IDLE:
                return MediaPlayerConnector.PLAYER_STATE_IDLE;
            case PLAYER_STATE_PREPARED:
            case PLAYER_STATE_PAUSED:
                return MediaPlayerConnector.PLAYER_STATE_PAUSED;
            case PLAYER_STATE_PLAYING:
                return MediaPlayerConnector.PLAYER_STATE_PLAYING;
            case PLAYER_STATE_ERROR:
                return MediaPlayerConnector.PLAYER_STATE_ERROR;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void loopCurrent(final boolean loop) {
        addTask(new Task(CALL_COMPLETED_LOOP_CURRENT, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
                }
            }
        });
    }

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
    public void close() {
        synchronized (mPlayerLock) {
            mPlayer.release();
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
    }

    @Override
    public void setAudioAttributes(final AudioAttributesCompat attributes) {
        addTask(new Task(CALL_COMPLETED_SET_AUDIO_ATTRIBUTES, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setAudioAttributes(ExoPlayerUtils.getAudioAttributes(attributes));
                }
            }
        });
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        AudioAttributes exoPlayerAudioAttributes;
        synchronized (mPlayerLock) {
            exoPlayerAudioAttributes = mPlayer.getAudioAttributes();
        }
        return ExoPlayerUtils.getAudioAttributesCompat(exoPlayerAudioAttributes);
    }

    @Override
    public int getVideoWidth() {
        synchronized (mLock) {
            return mVideoWidth;
        }
    }

    @Override
    public int getVideoHeight() {
        synchronized (mLock) {
            return mVideoHeight;
        }
    }

    @Override
    public void setSurface(final Surface surface) {
        addTask(new Task(CALL_COMPLETED_SET_SURFACE, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setVideoSurface(surface);
                }
            }
        });
    }

    @Override
    public void setPlayerVolume(final float volume) {
        addTask(new Task(CALL_COMPLETED_SET_PLAYER_VOLUME, false) {
            @Override
            void process() {
                synchronized (mPlayerLock) {
                    mPlayer.setVolume(volume);
                }
            }
        });
    }

    @Override
    public float getPlayerVolume() {
        synchronized (mPlayerLock) {
            return mPlayer.getVolume();
        }
    }

    @Override
    public List<TrackInfo> getTrackInfo() {
        TrackGroupArray trackGroupArray;
        synchronized (mPlayerLock) {
            trackGroupArray = mPlayer.getCurrentTrackGroups();
        }
        return ExoPlayerUtils.getTrackInfo(trackGroupArray);
    }

    @Override
    public void reset() {
        synchronized (mLock) {
            mMediaItem = null;
            mVideoWidth = 0;
            mVideoHeight = 0;
            mStartPlaybackTimeNs = -1;
            mPlayingTimeUs = 0;
        }
        // TODO(b/80232248): Make mPlayer non-final and release here to make the call blocking.
        // TODO(b/80232248): ComponentListener callbacks on the task thread can happen at the same
        // time as resetting the player. Once the data source queue is implemented, check the
        // current data source description to suppress stale events before reset.
        synchronized (mPlayerLock) {
            mPlayer.stop(/* reset= */ true);
        }
    }

    @Override
    public void skipToNext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextMediaItem(MediaItem2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getNextMediaItems(List<MediaItem2> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PersistableBundle getMetrics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlaybackParams(PlaybackParams2 params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PlaybackParams2 getPlaybackParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void seekTo(long msec, int mode) {
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
    public int getAudioSessionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAuxEffect(int effectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuxEffectSendLevel(float level) {
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
    public void deselectTrack(int index) {
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

    // Internal functionality.

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void maybeUpdateTimerForPlaying() {
        synchronized (mLock) {
            if (mStartPlaybackTimeNs != -1) {
                return;
            }
            mStartPlaybackTimeNs = System.nanoTime();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void maybeUpdateTimerForStopped() {
        synchronized (mLock) {
            if (mStartPlaybackTimeNs == -1) {
                return;
            }
            long nowNs = System.nanoTime();
            mPlayingTimeUs += (nowNs - mStartPlaybackTimeNs + 500) / 1000;
            mStartPlaybackTimeNs = -1;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ComponentListener extends Player.DefaultEventListener implements
            VideoListener {

        @Override
        public void onVideoSizeChanged(
                final int width,
                final int height,
                int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {
            final MediaItem2 item;
            synchronized (mLock) {
                // TODO(b/80232248): get the active media item from a media item queue.
                item = mMediaItem;
                mVideoWidth = width;
                mVideoHeight = height;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(EventCallback callback) {
                    callback.onVideoSizeChanged(
                            ExoPlayerMediaPlayer2Impl.this, item, width, height);
                }
            });
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
            // TODO(b/80232248): Trigger onInfo with MEDIA_INFO_PREPARED for any item in the data
            // source queue for which the duration is now known, even if this is not the initial
            // preparation.
            if (reason != Player.TIMELINE_CHANGE_REASON_PREPARED) {
                return;
            }

            final MediaItem2 item;
            synchronized (mLock) {
                item = mMediaItem;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(EventCallback callback) {
                    MediaPlayer2 mediaPlayer2 = ExoPlayerMediaPlayer2Impl.this;
                    callback.onInfo(
                            mediaPlayer2,
                            item,
                            MEDIA_INFO_PREPARED,
                            /* extra= */ 0);
                }
            });
            notifyPlayerEvent(new PlayerEventNotifier() {
                @Override
                public void notify(MediaPlayerConnector.PlayerEventCallback cb) {
                    cb.onMediaPrepared(getMediaPlayerConnector(), item);
                }
            });
            synchronized (mTaskLock) {
                if (mCurrentTask != null
                        && mCurrentTask.mMediaCallType == CALL_COMPLETED_PREPARE
                        && ObjectsCompat.equals(mCurrentTask.mDSD, item)
                        && mCurrentTask.mNeedToWaitForEventToComplete) {
                    mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                    mCurrentTask = null;
                    processPendingTask();
                }
            }
        }

        @Override
        public void onRenderedFirstFrame() {
            final MediaItem2 item;
            synchronized (mLock) {
                // TODO(b/80232248): get the active data source from a data source queue.
                item = mMediaItem;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(EventCallback callback) {
                    MediaPlayer2 mediaPlayer2 = ExoPlayerMediaPlayer2Impl.this;
                    callback.onInfo(
                            mediaPlayer2,
                            item,
                            MEDIA_INFO_VIDEO_RENDERING_START,
                            /* extra= */ 0);
                }
            });
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            if (state == Player.STATE_READY && playWhenReady) {
                maybeUpdateTimerForPlaying();
            } else {
                maybeUpdateTimerForStopped();
            }
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {}

        @Override
        public void onSeekProcessed() {}

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
            return ExoPlayerMediaPlayer2Impl.this.getPlayerState();
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
                            && getState() == PLAYER_STATE_ERROR) {
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

            mDSD = getCurrentMediaItem();

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
