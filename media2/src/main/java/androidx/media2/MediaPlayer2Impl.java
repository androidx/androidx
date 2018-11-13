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
import android.media.AudioAttributes;
import android.media.DeniedByServerException;
import android.media.MediaDataSource;
import android.media.MediaDrm;
import android.media.MediaPlayer;
import android.media.ResourceBusyException;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.util.Log;
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
import androidx.media2.SessionPlayer.BuffState;
import androidx.media2.SessionPlayer.PlayerState;
import androidx.media2.common.TrackInfoImpl;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MediaPlayer2 implementation for platform version P (28).
 * @hide
 */
@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(LIBRARY_GROUP)
public final class MediaPlayer2Impl extends MediaPlayer2 {

    private static final String TAG = "MediaPlayer2Impl";

    private static final int SOURCE_STATE_ERROR = -1;
    private static final int SOURCE_STATE_INIT = 0;
    private static final int SOURCE_STATE_PREPARING = 1;
    private static final int SOURCE_STATE_PREPARED = 2;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final android.media.PlaybackParams DEFAULT_PLAYBACK_PARAMS =
            new android.media.PlaybackParams().allowDefaults();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sInfoEventMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sErrorEventExtraMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sStateMap;

    static {
        sInfoEventMap = new ArrayMap<>();
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_UNKNOWN, MEDIA_INFO_UNKNOWN);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT, MEDIA_INFO_UNKNOWN);
        sInfoEventMap.put(
                MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START, MEDIA_INFO_VIDEO_RENDERING_START);
        sInfoEventMap.put(
                MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING, MEDIA_INFO_VIDEO_TRACK_LAGGING);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_BUFFERING_START, MEDIA_INFO_BUFFERING_START);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_BUFFERING_END, MEDIA_INFO_BUFFERING_END);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING, MEDIA_INFO_BAD_INTERLEAVING);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_NOT_SEEKABLE, MEDIA_INFO_NOT_SEEKABLE);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_METADATA_UPDATE, MEDIA_INFO_METADATA_UPDATE);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_AUDIO_NOT_PLAYING, MEDIA_INFO_AUDIO_NOT_PLAYING);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_VIDEO_NOT_PLAYING, MEDIA_INFO_VIDEO_NOT_PLAYING);
        sInfoEventMap.put(
                MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE, MEDIA_INFO_UNSUPPORTED_SUBTITLE);
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT, MEDIA_INFO_SUBTITLE_TIMED_OUT);

        sErrorEventExtraMap = new ArrayMap<>();
        sErrorEventExtraMap.put(MediaPlayer.MEDIA_ERROR_IO, MEDIA_ERROR_IO);
        sErrorEventExtraMap.put(MediaPlayer.MEDIA_ERROR_MALFORMED, MEDIA_ERROR_MALFORMED);
        sErrorEventExtraMap.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, MEDIA_ERROR_UNSUPPORTED);
        sErrorEventExtraMap.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, MEDIA_ERROR_TIMED_OUT);

        sStateMap = new ArrayMap<>();
        sStateMap.put(PLAYER_STATE_IDLE, SessionPlayer.PLAYER_STATE_IDLE);
        sStateMap.put(PLAYER_STATE_PREPARED, SessionPlayer.PLAYER_STATE_PAUSED);
        sStateMap.put(PLAYER_STATE_PAUSED, SessionPlayer.PLAYER_STATE_PAUSED);
        sStateMap.put(PLAYER_STATE_PLAYING, SessionPlayer.PLAYER_STATE_PLAYING);
        sStateMap.put(PLAYER_STATE_ERROR, SessionPlayer.PLAYER_STATE_ERROR);
    }

    MediaPlayerSourceQueue mPlayer;

    private HandlerThread mHandlerThread;
    private final Handler mEndPositionHandler;
    private final Handler mTaskHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mTaskLock = new Object();
    @GuardedBy("mTaskLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayDeque<Task> mPendingTasks = new ArrayDeque<>();
    @GuardedBy("mTaskLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Task mCurrentTask;

    private final Object mLock = new Object();
    //--- guarded by |mLock| start
    private Pair<Executor, EventCallback> mMp2EventCallbackRecord;
    private Pair<Executor, DrmEventCallback> mDrmEventCallbackRecord;
    //--- guarded by |mLock| end

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleDataSourceError(final DataSourceError err) {
        if (err == null) {
            return;
        }
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(EventCallback callback) {
                callback.onError(MediaPlayer2Impl.this, err.mDSD, err.mWhat, err.mExtra);
            }
        });
    }

    /**
     * Default constructor.
     * <p>When done with the MediaPlayer2Impl, you should call  {@link #close()},
     * to free the resources. If not released, too many MediaPlayer2Impl instances may
     * result in an exception.</p>
     */
    public MediaPlayer2Impl() {
        mHandlerThread = new HandlerThread("MediaPlayer2TaskThread");
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        mEndPositionHandler = new Handler(looper);
        mTaskHandler = new Handler(looper);

        // TODO: To make sure MediaPlayer1 listeners work, the caller thread should have a looper.
        // Fix the framework or document this behavior.
        mPlayer = new MediaPlayerSourceQueue();
    }

    /**
     * Releases the resources held by this {@code MediaPlayer2} object.
     *
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer2. In particular, whenever an Activity
     * of an application is paused (its onPause() method is called),
     * or stopped (its onStop() method is called), this method should be
     * invoked to release the MediaPlayer2 object, unless the application
     * has a special need to keep the object around. In addition to
     * unnecessary resources (such as memory and instances of codecs)
     * being held, failure to call this method immediately if a
     * MediaPlayer2 object is no longer needed may also lead to
     * continuous battery consumption for mobile devices, and playback
     * failure for other applications if no multiple instances of the
     * same codec are supported on a device. Even if multiple instances
     * of the same codec are supported, some performance degradation
     * may be expected when unnecessary multiple instances are used
     * at the same time.
     *
     * {@code close()} may be safely called after a prior {@code close()}.
     * This class implements the Java {@code AutoCloseable} interface and
     * may be used with try-with-resources.
     */
    @Override
    public boolean cancel(Object token) {
        synchronized (mTaskLock) {
            return mPendingTasks.remove(token);
        }
    }

    @Override
    public void close() {
        clearEventCallback();
        clearDrmEventCallback();
        mPlayer.release();
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
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
    public Object prepare() {
        return addTask(new Task(CALL_COMPLETED_PREPARE, true) {
            @Override
            void process() throws IOException {
                mPlayer.prepareAsync();
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
    public Object skipToNext() {
        return addTask(new Task(CALL_COMPLETED_SKIP_TO_NEXT, false) {
            @Override
            void process() {
                mPlayer.skipToNext();
            }
        });
    }

    @Override
    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        // Use cached buffered percent for now.
        return mPlayer.getBufferedPosition();
    }

    @Override
    public @MediaPlayer2State int getState() {
        return mPlayer.getMediaPlayer2State();
    }

    @SessionPlayer.PlayerState int getPlayerState() {
        return mPlayer.getPlayerState();
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     */
    @SessionPlayer.BuffState int getBufferingState() {
        return  mPlayer.getBufferingState();
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
    public @Nullable AudioAttributesCompat getAudioAttributes() {
        return mPlayer.getAudioAttributes();
    }

    @Override
    public Object setMediaItem(@NonNull final MediaItem item) {
        return addTask(new Task(CALL_COMPLETED_SET_DATA_SOURCE, false) {
            @Override
            void process() {
                Preconditions.checkArgument(item != null, "the MediaItem cannot be null");
                // TODO: setMediaItem could update exist media item
                try {
                    mPlayer.setFirst(item);
                } catch (IOException e) {
                    Log.e(TAG, "process: setMediaItem", e);
                }
            }
        });
    }

    @Override
    public Object setNextMediaItem(@NonNull final MediaItem item) {
        return addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCE, false) {
            @Override
            void process() {
                Preconditions.checkArgument(item != null, "the MediaItem cannot be null");
                handleDataSourceError(mPlayer.setNext(item));
            }
        });
    }

    @Override
    public Object setNextMediaItems(@NonNull final List<MediaItem> items) {
        return addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCES, false) {
            @Override
            void process() {
                if (items == null || items.size() == 0) {
                    throw new IllegalArgumentException("media item list cannot be null or empty.");
                }
                for (MediaItem item : items) {
                    if (item == null) {
                        throw new IllegalArgumentException(
                                "MediaItem in the source list cannot be null.");
                    }
                }
                handleDataSourceError(mPlayer.setNextMultiple(items));
            }
        });
    }

    @Override
    public @Nullable MediaItem getCurrentMediaItem() {
        return mPlayer.getFirst().getDSD();
    }

    @Override
    public Object loopCurrent(final boolean loop) {
        return addTask(new Task(CALL_COMPLETED_LOOP_CURRENT, false) {
            @Override
            void process() {
                mPlayer.setLooping(loop);
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
        return mPlayer.getVolume();
    }

    @Override
    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    @Override
    public Object notifyWhenCommandLabelReached(final Object label) {
        return addTask(new Task(CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED, false) {
            @Override
            void process() {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onCommandLabelReached(MediaPlayer2Impl.this, label);
                    }
                });
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
    public void clearPendingCommands() {
        synchronized (mTaskLock) {
            mPendingTasks.clear();
        }
    }

    private Object addTask(Task task) {
        synchronized (mTaskLock) {
            mPendingTasks.add(task);
            processPendingTask_l();
        }
        return task;
    }

    @GuardedBy("mTaskLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void processPendingTask_l() {
        if (mCurrentTask != null) {
            return;
        }
        if (!mPendingTasks.isEmpty()) {
            Task task = mPendingTasks.removeFirst();
            mCurrentTask = task;
            mTaskHandler.post(task);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void handleDataSource(MediaPlayerSource src)
            throws IOException {
        final MediaItem item = src.getDSD();
        Preconditions.checkArgument(item != null, "the MediaItem cannot be null");

        MediaPlayer player = src.getPlayer();
        if (item instanceof CallbackMediaItem) {
            player.setDataSource(new MediaDataSource() {
                DataSourceCallback mDataSource =
                        ((CallbackMediaItem) item).getDataSourceCallback();

                @Override
                public int readAt(long position, byte[] buffer, int offset, int size)
                        throws IOException {
                    return mDataSource.readAt(position, buffer, offset, size);
                }

                @Override
                public long getSize() throws IOException {
                    return mDataSource.getSize();
                }

                @Override
                public void close() throws IOException {
                    mDataSource.close();
                }
            });
        } else if (item instanceof FileMediaItem) {
            FileMediaItem fitem = (FileMediaItem) item;
            player.setDataSource(
                    fitem.getFileDescriptor(),
                    fitem.getFileDescriptorOffset(),
                    fitem.getFileDescriptorLength());
        } else if (item instanceof UriMediaItem) {
            UriMediaItem uitem = (UriMediaItem) item;
            player.setDataSource(
                    uitem.getUriContext(),
                    uitem.getUri(),
                    uitem.getUriHeaders(),
                    uitem.getUriCookies());
        } else {
            throw new IllegalArgumentException(
                    "Unsupported media item description. " + item.toString());
        }
    }

    @Override
    public int getVideoWidth() {
        return mPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mPlayer.getVideoHeight();
    }

    @Override
    public PersistableBundle getMetrics() {
        return mPlayer.getMetrics();
    }

    @Override
    public Object setPlaybackParams(@NonNull final PlaybackParams params) {
        return addTask(new Task(CALL_COMPLETED_SET_PLAYBACK_PARAMS, false) {
            @Override
            void process() {
                mPlayer.setPlaybackParams(params.getPlaybackParams());
            }
        });
    }

    @Override
    @NonNull
    public PlaybackParams getPlaybackParams() {
        return new PlaybackParams.Builder(mPlayer.getPlaybackParams()).build();
    }

    @Override
    public Object seekTo(final long msec, @SeekMode final int mode) {
        return addTask(new Task(CALL_COMPLETED_SEEK_TO, true) {
            @Override
            void process() {
                mPlayer.seekTo(msec, mode);
            }
        });
    }

    @Override
    @Nullable
    public MediaTimestamp getTimestamp() {
        return mPlayer.getTimestamp();
    }

    /**
     * Resets the MediaPlayer2 to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * media item and calling prepare().
     */
    @Override
    public void reset() {
        clearPendingCommands();
        mEndPositionHandler.removeCallbacksAndMessages(null);
        mTaskHandler.removeCallbacksAndMessages(null);

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
                }
            }
        }
        mPlayer.reset();
    }

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
    public int getAudioSessionId() {
        return mPlayer.getAudioSessionId();
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
    public Object setAuxEffectSendLevel(final float level) {
        return addTask(new Task(CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL, false) {
            @Override
            void process() {
                mPlayer.setAuxEffectSendLevel(level);
            }
        });
    }

    @Override
    public List<TrackInfo> getTrackInfo() {
        MediaPlayer.TrackInfo[] list = mPlayer.getTrackInfo();
        List<TrackInfo> trackList = new ArrayList<>();
        for (MediaPlayer.TrackInfo info : list) {
            trackList.add(new TrackInfoImpl(info.getTrackType(), info.getFormat()));
        }
        return trackList;
    }

    @Override
    public int getSelectedTrack(int trackType) {
        return mPlayer.getSelectedTrack(trackType);
    }

    @Override
    public Object selectTrack(final int index) {
        return addTask(new Task(CALL_COMPLETED_SELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.selectTrack(index);
            }
        });
    }

    @Override
    public Object deselectTrack(final int index) {
        return addTask(new Task(CALL_COMPLETED_DESELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.deselectTrack(index);
            }
        });
    }

    @Override
    public void setEventCallback(@NonNull Executor executor,
            @NonNull EventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Illegal null Executor for the EventCallback");
        }
        synchronized (mLock) {
            mMp2EventCallbackRecord = new Pair(executor, eventCallback);
        }
    }

    @Override
    public void clearEventCallback() {
        synchronized (mLock) {
            mMp2EventCallbackRecord = null;
        }
    }

    // Modular DRM begin

    @Override
    public void setOnDrmConfigHelper(final OnDrmConfigHelper listener) {
        mPlayer.setOnDrmConfigHelper(new MediaPlayer.OnDrmConfigHelper() {
            @Override
            public void onDrmConfig(MediaPlayer mp) {
                MediaPlayerSource src = mPlayer.getSourceForPlayer(mp);
                MediaItem item = src == null ? null : src.getDSD();
                listener.onDrmConfig(MediaPlayer2Impl.this, item);
            }
        });
    }

    @Override
    public void setDrmEventCallback(@NonNull Executor executor,
                                    @NonNull DrmEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Illegal null Executor for the EventCallback");
        }
        synchronized (mLock) {
            mDrmEventCallbackRecord = new Pair(executor, eventCallback);
        }
    }

    @Override
    public void clearDrmEventCallback() {
        synchronized (mLock) {
            mDrmEventCallbackRecord = null;
        }
    }


    @Override
    public DrmInfo getDrmInfo() {
        MediaPlayer.DrmInfo info = mPlayer.getDrmInfo();
        return info == null ? null : new DrmInfoImpl(info.getPssh(), info.getSupportedSchemes());
    }


    @Override
    public Object prepareDrm(@NonNull final UUID uuid) {
        return addTask(new Task(CALL_COMPLETED_PREPARE_DRM, false) {
            @Override
            void process() {
                int status = PREPARE_DRM_STATUS_SUCCESS;
                try {
                    mPlayer.prepareDrm(uuid);
                } catch (ResourceBusyException e) {
                    status = PREPARE_DRM_STATUS_RESOURCE_BUSY;
                } catch (MediaPlayer.ProvisioningServerErrorException e) {
                    status = PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR;
                } catch (MediaPlayer.ProvisioningNetworkErrorException e) {
                    status = PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR;
                } catch (UnsupportedSchemeException e) {
                    status = PREPARE_DRM_STATUS_UNSUPPORTED_SCHEME;
                } catch (Exception e) {
                    status = PREPARE_DRM_STATUS_PREPARATION_ERROR;
                }
                final int prepareDrmStatus = status;
                notifyDrmEvent(new DrmEventNotifier() {
                    @Override
                    public void notify(DrmEventCallback cb) {
                        cb.onDrmPrepared(MediaPlayer2Impl.this, mDSD, prepareDrmStatus);
                    }
                });
            }
        });
    }

    @Override
    public void releaseDrm() throws NoDrmSchemeException {
        try {
            mPlayer.releaseDrm();
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    @Override
    @NonNull
    public MediaDrm.KeyRequest getDrmKeyRequest(@Nullable byte[] keySetId,
            @Nullable byte[] initData, @Nullable String mimeType, int keyType,
            @Nullable Map<String, String> optionalParameters)
            throws NoDrmSchemeException {
        try {
            return mPlayer.getKeyRequest(keySetId, initData, mimeType, keyType, optionalParameters);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    @Override
    public byte[] provideDrmKeyResponse(@Nullable byte[] keySetId, @NonNull byte[] response)
            throws NoDrmSchemeException, DeniedByServerException {
        try {
            return mPlayer.provideKeyResponse(keySetId, response);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    @Override
    public void restoreDrmKeys(@NonNull final byte[] keySetId)
            throws NoDrmSchemeException {
        try {
            mPlayer.restoreKeys(keySetId);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    @Override
    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName)
            throws NoDrmSchemeException {
        try {
            return mPlayer.getDrmPropertyString(propertyName);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    @Override
    public void setDrmPropertyString(@NonNull String propertyName,
                                     @NonNull String value)
            throws NoDrmSchemeException {
        try {
            mPlayer.setDrmPropertyString(propertyName, value);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyMediaPlayer2Event(final Mp2EventNotifier notifier) {
        final Pair<Executor, EventCallback> record;
        synchronized (mLock) {
            record = mMp2EventCallbackRecord;
        }
        if (record != null) {
            try {
                record.first.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.notify(record.second);
                    }
                });
            } catch (RejectedExecutionException e) {
                // The given executor is shutting down.
                Log.w(TAG, "The given executor is shutting down. Ignoring the player event.");
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyDrmEvent(final DrmEventNotifier notifier) {
        final Pair<Executor, DrmEventCallback> record;
        synchronized (mLock) {
            record = mDrmEventCallbackRecord;
        }
        if (record != null) {
            try {
                record.first.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.notify(record.second);
                    }
                });
            } catch (RejectedExecutionException e) {
                // The given executor is shutting down.
                Log.w(TAG, "The given executor is shutting down. Ignoring the player event.");
            }
        }
    }

    private interface Mp2EventNotifier {
        void notify(EventCallback callback);
    }

    private interface DrmEventNotifier {
        void notify(DrmEventCallback callback);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setEndPositionTimerIfNeeded(
            final MediaPlayer.OnCompletionListener completionListener,
            final MediaPlayerSource src, android.media.MediaTimestamp timeitem) {
        if (src == mPlayer.getFirst()) {
            mEndPositionHandler.removeCallbacksAndMessages(null);
            MediaItem item = src.getDSD();
            if (item.getEndPosition() != MediaItem.POSITION_UNKNOWN) {
                if (timeitem.getMediaClockRate() > 0.0f) {
                    long nowNs = System.nanoTime();
                    long elapsedTimeUs = (nowNs - timeitem.getAnchorSytemNanoTime()) / 1000;
                    long nowMediaMs = (timeitem.getAnchorMediaTimeUs() + elapsedTimeUs) / 1000;
                    long timeLeftMs = (long) ((item.getEndPosition() - nowMediaMs)
                            / timeitem.getMediaClockRate());
                    mEndPositionHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mPlayer.getFirst() != src) {
                                return;
                            }
                            mPlayer.pause();
                            completionListener.onCompletion(src.getPlayer());
                        }
                    }, timeLeftMs < 0 ? 0 : timeLeftMs);
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void clearListeners(final MediaPlayerSource src) {
        MediaPlayer p = src.getPlayer();
        p.setOnPreparedListener(null);
        p.setOnVideoSizeChangedListener(null);
        p.setOnInfoListener(null);
        p.setOnCompletionListener(null);
        p.setOnErrorListener(null);
        p.setOnSeekCompleteListener(null);
        p.setOnTimedMetaDataAvailableListener(null);
        p.setOnBufferingUpdateListener(null);
        p.clearOnMediaTimeDiscontinuityListener();
        p.clearOnSubtitleDataListener();
        p.setOnDrmInfoListener(null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setUpListeners(final MediaPlayerSource src) {
        MediaPlayer p = src.getPlayer();
        final MediaPlayer.OnPreparedListener preparedListener =
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        handleDataSourceError(mPlayer.onPrepared(mp));
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(EventCallback callback) {
                                MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                                MediaItem item = src.getDSD();
                                callback.onInfo(mp2, item, MEDIA_INFO_PREPARED, 0);
                            }
                        });
                        synchronized (mTaskLock) {
                            if (mCurrentTask != null
                                    && mCurrentTask.mMediaCallType == CALL_COMPLETED_PREPARE
                                    && ObjectsCompat.equals(mCurrentTask.mDSD, src.getDSD())
                                    && mCurrentTask.mNeedToWaitForEventToComplete) {
                                mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                                mCurrentTask = null;
                                processPendingTask_l();
                            }
                        }
                    }
                };
        p.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (src.getPlayer().getDuration() >= 0) {
                    // Seeks to the start position. We call seek operation even when the start
                    // position is 0 in order to show the preview.
                    src.getPlayer().seekTo((int) src.getDSD().getStartPosition(),
                            MediaPlayer.SEEK_CLOSEST);
                    // In this case, PREPARED notification will be sent when seek is done.
                } else {
                    // The content is not seekable. e.g. live contents.
                    preparedListener.onPrepared(mp);
                }
            }
        });
        p.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onVideoSizeChanged(MediaPlayer2Impl.this, src.getDSD(), width, height);
                    }
                });
            }
        });
        p.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        mPlayer.setBufferingState(
                                mp, SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        mPlayer.setBufferingState(
                                mp, SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                        break;
                }
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        if (what == MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT) {
                            mPlayer.onStartedAsNext(mp);
                            return;
                        }
                        int w = sInfoEventMap.getOrDefault(what, MEDIA_INFO_UNKNOWN);
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), w, extra);
                    }
                });
                return true;
            }
        });
        final MediaPlayer.OnCompletionListener completionListener =
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        handleDataSourceError(mPlayer.onCompletion(mp));
                    }
                };
        p.setOnCompletionListener(completionListener);
        p.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, final int what, final int extra) {
                mPlayer.onError(mp);
                synchronized (mTaskLock) {
                    if (mCurrentTask != null
                            && mCurrentTask.mNeedToWaitForEventToComplete) {
                        mCurrentTask.sendCompleteNotification(CALL_STATUS_ERROR_UNKNOWN);
                        mCurrentTask = null;
                        processPendingTask_l();
                    }
                }
                final int w  = (what == MediaPlayer.MEDIA_ERROR_UNKNOWN)
                        ? sErrorEventExtraMap.getOrDefault(extra, MEDIA_ERROR_UNKNOWN)
                        : MEDIA_ERROR_UNKNOWN;
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onError(MediaPlayer2Impl.this, src.getDSD(), w, 0);
                    }
                });
                return true;
            }
        });
        p.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (src.mMp2State == PLAYER_STATE_IDLE) {
                    // This seek request was for handling start position. Notify client that it's
                    // ready to start playback.
                    preparedListener.onPrepared(mp);
                    return;
                }
                synchronized (mTaskLock) {
                    if (mCurrentTask != null
                            && mCurrentTask.mMediaCallType == CALL_COMPLETED_SEEK_TO
                            && mCurrentTask.mNeedToWaitForEventToComplete) {
                        mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                        mCurrentTask = null;
                        processPendingTask_l();
                    }
                }
            }
        });
        p.setOnTimedMetaDataAvailableListener(
                new MediaPlayer.OnTimedMetaDataAvailableListener() {
                    @Override
                    public void onTimedMetaDataAvailable(MediaPlayer mp, final android.media
                            .TimedMetaData data) {
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(EventCallback cb) {
                                cb.onTimedMetaDataAvailable(MediaPlayer2Impl.this, src.getDSD(),
                                        new TimedMetaData(data));
                            }
                        });
                    }
                });
        p.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, final int percent) {
                if (percent >= 100) {
                    mPlayer.setBufferingState(
                            mp, SessionPlayer.BUFFERING_STATE_COMPLETE);
                }
                src.mBufferedPercentage.set(percent);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                MEDIA_INFO_BUFFERING_UPDATE, percent);
                    }
                });
            }
        });
        p.setOnMediaTimeDiscontinuityListener(
                new MediaPlayer.OnMediaTimeDiscontinuityListener() {
                    @Override
                    public void onMediaTimeDiscontinuity(
                            MediaPlayer mp, final android.media.MediaTimestamp timestamp) {
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(EventCallback cb) {
                                cb.onMediaTimeDiscontinuity(
                                        MediaPlayer2Impl.this, src.getDSD(),
                                        new MediaTimestamp(timestamp));
                            }
                        });
                        setEndPositionTimerIfNeeded(completionListener, src, timestamp);
                    }
                });
        p.setOnSubtitleDataListener(new MediaPlayer.OnSubtitleDataListener() {
            @Override
            public  void onSubtitleData(MediaPlayer mp, final android.media.SubtitleData data) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        cb.onSubtitleData(
                                MediaPlayer2Impl.this, src.getDSD(), new SubtitleData(data));
                    }
                });
            }
        });
        p.setOnDrmInfoListener(new MediaPlayer.OnDrmInfoListener() {
            @Override
            public void onDrmInfo(MediaPlayer mp, final MediaPlayer.DrmInfo drmInfo) {
                notifyDrmEvent(new DrmEventNotifier() {
                    @Override
                    public void notify(DrmEventCallback cb) {
                        cb.onDrmInfo(MediaPlayer2Impl.this, src.getDSD(),
                                new DrmInfoImpl(drmInfo.getPssh(), drmInfo.getSupportedSchemes()));
                    }
                });
            }
        });
    }

    /**
     * Encapsulates the DRM properties of the source.
     */
    public static final class DrmInfoImpl extends DrmInfo {
        private Map<UUID, byte[]> mMapPssh;
        private UUID[] mSupportedSchemes;

        /**
         * Returns the PSSH info of the media item for each supported DRM scheme.
         */
        @Override
        public Map<UUID, byte[]> getPssh() {
            return mMapPssh;
        }

        /**
         * Returns the intersection of the media item and the device DRM schemes.
         * It effectively identifies the subset of the source's DRM schemes which
         * are supported by the device too.
         */
        @Override
        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(mSupportedSchemes);
        }

        DrmInfoImpl(Map<UUID, byte[]> pssh, UUID[] supportedSchemes) {
            mMapPssh = pssh;
            mSupportedSchemes = supportedSchemes;
        }

        private DrmInfoImpl(Parcel parcel) {
            Log.v(TAG, "DrmInfoImpl(" + parcel + ") size " + parcel.dataSize());

            int psshsize = parcel.readInt();
            byte[] pssh = new byte[psshsize];
            parcel.readByteArray(pssh);

            Log.v(TAG, "DrmInfoImpl() PSSH: " + arrToHex(pssh));
            mMapPssh = parsePSSH(pssh, psshsize);
            Log.v(TAG, "DrmInfoImpl() PSSH: " + mMapPssh);

            int supportedDRMsCount = parcel.readInt();
            mSupportedSchemes = new UUID[supportedDRMsCount];
            for (int i = 0; i < supportedDRMsCount; i++) {
                byte[] uuid = new byte[16];
                parcel.readByteArray(uuid);

                mSupportedSchemes[i] = bytesToUUID(uuid);

                Log.v(TAG, "DrmInfoImpl() supportedScheme[" + i + "]: "
                        + mSupportedSchemes[i]);
            }

            Log.v(TAG, "DrmInfoImpl() Parcel psshsize: " + psshsize
                    + " supportedDRMsCount: " + supportedDRMsCount);
        }

        private DrmInfoImpl makeCopy() {
            return new DrmInfoImpl(this.mMapPssh, this.mSupportedSchemes);
        }

        private String arrToHex(byte[] bytes) {
            String out = "0x";
            for (int i = 0; i < bytes.length; i++) {
                out += String.format("%02x", bytes[i]);
            }

            return out;
        }

        private UUID bytesToUUID(byte[] uuid) {
            long msb = 0, lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb |= (((long) uuid[i] & 0xff) << (8 * (7 - i)));
                lsb |= (((long) uuid[i + 8] & 0xff) << (8 * (7 - i)));
            }

            return new UUID(msb, lsb);
        }

        private Map<UUID, byte[]> parsePSSH(byte[] pssh, int psshsize) {
            Map<UUID, byte[]> result = new HashMap<UUID, byte[]>();

            final int uuidSize = 16;
            final int dataLenSize = 4;

            int len = psshsize;
            int numentries = 0;
            int i = 0;

            while (len > 0) {
                if (len < uuidSize) {
                    Log.w(TAG, String.format("parsePSSH: len is too short to parse "
                            + "UUID: (%d < 16) pssh: %d", len, psshsize));
                    return null;
                }

                byte[] subset = Arrays.copyOfRange(pssh, i, i + uuidSize);
                UUID uuid = bytesToUUID(subset);
                i += uuidSize;
                len -= uuidSize;

                // get data length
                if (len < 4) {
                    Log.w(TAG, String.format("parsePSSH: len is too short to parse "
                            + "datalen: (%d < 4) pssh: %d", len, psshsize));
                    return null;
                }

                subset = Arrays.copyOfRange(pssh, i, i + dataLenSize);
                int datalen = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
                        ? ((subset[3] & 0xff) << 24) | ((subset[2] & 0xff) << 16)
                        | ((subset[1] & 0xff) << 8) | (subset[0] & 0xff)
                        : ((subset[0] & 0xff) << 24) | ((subset[1] & 0xff) << 16)
                                | ((subset[2] & 0xff) << 8) | (subset[3] & 0xff);
                i += dataLenSize;
                len -= dataLenSize;

                if (len < datalen) {
                    Log.w(TAG, String.format("parsePSSH: len is too short to parse "
                            + "data: (%d < %d) pssh: %d", len, datalen, psshsize));
                    return null;
                }

                byte[] data = Arrays.copyOfRange(pssh, i, i + datalen);

                // skip the data
                i += datalen;
                len -= datalen;

                Log.v(TAG, String.format("parsePSSH[%d]: <%s, %s> pssh: %d",
                        numentries, uuid, arrToHex(data), psshsize));
                numentries++;
                result.put(uuid, data);
            }

            return result;
        }

    };  // DrmInfoImpl

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
                    processPendingTask_l();
                }
            }
            // reset() might be waiting for this task. Notify that the task is done.
            synchronized (this) {
                mDone = true;
                this.notifyAll();
            }
        }

        void sendCompleteNotification(final int status) {
            if (mMediaCallType >= SEPARATE_CALL_COMPLETE_CALLBACK_START) {
                // These methods have a separate call complete callback and it should be already
                // called within {@link #processs()}.
                return;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(EventCallback cb) {
                    cb.onCallCompleted(
                            MediaPlayer2Impl.this, mDSD, mMediaCallType, status);
                }
            });
        }
    };

    private static class DataSourceError {
        final MediaItem mDSD;
        final int mWhat;

        final int mExtra;
        DataSourceError(MediaItem item, int what, int extra) {
            mDSD = item;
            mWhat = what;
            mExtra = extra;
        }

    }

    private class MediaPlayerSource {

        volatile MediaItem mDSD;
        MediaPlayer mPlayer;
        final AtomicInteger mBufferedPercentage = new AtomicInteger(0);
        int mSourceState = SOURCE_STATE_INIT;
        @MediaPlayer2State int mMp2State = PLAYER_STATE_IDLE;
        @BuffState int mBufferingState = SessionPlayer.BUFFERING_STATE_UNKNOWN;
        @PlayerState int mPlayerState = SessionPlayer.PLAYER_STATE_IDLE;
        boolean mPlayPending;
        boolean mSetAsNextPlayer;

        MediaPlayerSource(final MediaItem item) {
            mDSD = item;
            setUpListeners(this);
        }

        MediaItem getDSD() {
            return mDSD;
        }

        synchronized MediaPlayer getPlayer() {
            if (mPlayer == null) {
                mPlayer = new MediaPlayer();
            }
            return mPlayer;
        }

        void release() {
            clearListeners(this);
            mPlayer.release();
        }
    }

    private class MediaPlayerSourceQueue {
        List<MediaPlayerSource> mQueue = new ArrayList<>();
        Float mVolume = 1.0f;
        Surface mSurface;
        Integer mAuxEffect;
        Float mAuxEffectSendLevel;
        AudioAttributesCompat mAudioAttributes;
        Integer mAudioSessionId;
        android.media.PlaybackParams mPlaybackParams = DEFAULT_PLAYBACK_PARAMS;
        android.media.PlaybackParams mPlaybackParamsToSetWhenStarting;
        boolean mLooping;

        MediaPlayerSourceQueue() {
            mQueue.add(new MediaPlayerSource(null));
        }

        synchronized MediaPlayer getCurrentPlayer() {
            return mQueue.get(0).getPlayer();
        }

        synchronized MediaPlayerSource getFirst() {
            return mQueue.get(0);
        }

        synchronized void setFirst(MediaItem item) throws IOException {
            if (mQueue.isEmpty()) {
                mQueue.add(0, new MediaPlayerSource(item));
            } else {
                mQueue.get(0).mDSD = item;
                setUpListeners(mQueue.get(0));
            }
            handleDataSource(mQueue.get(0));
        }

        synchronized DataSourceError setNext(MediaItem item) {
            if (mQueue.isEmpty() || getFirst().getDSD() == null) {
                throw new IllegalStateException();
            }
            // Clear next media items if any.
            while (mQueue.size() >= 2) {
                MediaPlayerSource src = mQueue.remove(1);
                src.release();
            }
            MediaPlayerSource src = new MediaPlayerSource(item);
            mQueue.add(1, src);
            return prepareAt(1);
        }

        synchronized DataSourceError setNextMultiple(List<MediaItem> descs) {
            if (mQueue.isEmpty() || getFirst().getDSD() == null) {
                throw new IllegalStateException();
            }
            // Clear next media items if any.
            while (mQueue.size() >= 2) {
                MediaPlayerSource src = mQueue.remove(1);
                src.release();
            }
            List<MediaPlayerSource> sources = new ArrayList<>();
            for (MediaItem item: descs) {
                sources.add(new MediaPlayerSource(item));
            }
            mQueue.addAll(1, sources);
            return prepareAt(1);
        }

        synchronized void play() {
            final MediaPlayerSource src = mQueue.get(0);
            if (src.mSourceState == SOURCE_STATE_PREPARED) {
                if (mPlaybackParamsToSetWhenStarting != null) {
                    src.getPlayer().setPlaybackParams(mPlaybackParamsToSetWhenStarting);
                    mPlaybackParamsToSetWhenStarting = null;
                }
                src.getPlayer().start();
                setMp2State(src.getPlayer(), PLAYER_STATE_PLAYING);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                MEDIA_INFO_DATA_SOURCE_START, 0);
                    }
                });
            } else {
                throw new IllegalStateException();
            }
        }

        synchronized void release() {
            getCurrentPlayer().release();
        }

        synchronized void prepareAsync() {
            MediaPlayer mp = getCurrentPlayer();
            mp.prepareAsync();
            setBufferingState(mp, SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED);
        }

        synchronized void pause() {
            MediaPlayerSource current = getFirst();
            if (current.mMp2State == PLAYER_STATE_PREPARED) {
                // MediaPlayer1 does not allow pause() in the prepared state. To workaround, call
                // start() here right before calling pause().
                current.mPlayer.start();
            }
            current.mPlayer.pause();
            setMp2State(current.mPlayer, PLAYER_STATE_PAUSED);
        }

        synchronized long getCurrentPosition() {
            // Throws an ISE here rather than relying on MediaPlayer1 implementation which returns
            // a garbage value in the IDLE state.
            if (getFirst().mMp2State == PLAYER_STATE_IDLE) {
                throw new IllegalStateException();
            }
            return getCurrentPlayer().getCurrentPosition();
        }

        synchronized long getDuration() {
            // Throws an ISE here rather than relying on MediaPlayer1 implementation which returns
            // a garbage value in the IDLE state.
            if (getFirst().mMp2State == PLAYER_STATE_IDLE) {
                throw new IllegalStateException();
            }
            return getCurrentPlayer().getDuration();
        }

        synchronized long getBufferedPosition() {
            // Throws an ISE here rather than relying on MediaPlayer1 implementation which returns
            // a garbage value in the IDLE state.
            if (getFirst().mMp2State == PLAYER_STATE_IDLE) {
                throw new IllegalStateException();
            }
            MediaPlayerSource src = mQueue.get(0);
            return (long) src.getPlayer().getDuration() * src.mBufferedPercentage.get() / 100;
        }

        synchronized void setAudioAttributes(AudioAttributesCompat attributes) {
            mAudioAttributes = attributes;
            AudioAttributes attr = mAudioAttributes == null
                    ? null : (AudioAttributes) mAudioAttributes.unwrap();
            getCurrentPlayer().setAudioAttributes(attr);
        }

        synchronized AudioAttributesCompat getAudioAttributes() {
            return mAudioAttributes;
        }

        synchronized DataSourceError onPrepared(MediaPlayer mp) {
            for (int i = 0; i < mQueue.size(); i++) {
                MediaPlayerSource src = mQueue.get(i);
                if (mp == src.getPlayer()) {
                    if (i == 0) {
                        if (src.mPlayPending) {
                            src.mPlayPending = false;
                            src.getPlayer().start();
                            setMp2State(src.getPlayer(), PLAYER_STATE_PLAYING);
                        } else {
                            setMp2State(src.getPlayer(), PLAYER_STATE_PREPARED);
                        }
                    }
                    src.mSourceState = SOURCE_STATE_PREPARED;
                    setBufferingState(src.getPlayer(),
                            SessionPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                    if (i == 1) {
                        boolean hasVideo = false;
                        for (MediaPlayer.TrackInfo info : mp.getTrackInfo()) {
                            if (info.getTrackType()
                                    == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO) {
                                hasVideo = true;
                                break;
                            }
                        }
                        // setNextMediaPlayer() does not pass surface to next player. Use it only
                        // for the audio-only media item.
                        if (!hasVideo) {
                            getCurrentPlayer().setNextMediaPlayer(src.mPlayer);
                            src.mSetAsNextPlayer = true;
                        }
                    }
                    return prepareAt(i + 1);
                }
            }
            return null;
        }

        synchronized DataSourceError onCompletion(MediaPlayer mp) {
            final MediaPlayerSource src = getFirst();
            if (mLooping && mp == src.mPlayer) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        MediaItem item = src.getDSD();
                        cb.onInfo(mp2, item, MEDIA_INFO_DATA_SOURCE_REPEAT, 0);
                    }
                });
                src.mPlayer.seekTo((int) src.getDSD().getStartPosition());
                src.mPlayer.start();
                setMp2State(mp, PLAYER_STATE_PLAYING);
                return null;
            }
            if (mp == src.mPlayer) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback cb) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        MediaItem item = src.getDSD();
                        cb.onInfo(mp2, item, MEDIA_INFO_DATA_SOURCE_END, 0);
                    }
                });
            } else {
                Log.w(TAG, "Playback complete event from next player. Ignoring.");
            }
            if (!mQueue.isEmpty() && mp == src.mPlayer) {
                if (mQueue.size() == 1) {
                    setMp2State(mp, PLAYER_STATE_PAUSED);

                    final MediaItem item = mQueue.get(0).getDSD();
                    notifyMediaPlayer2Event(new Mp2EventNotifier() {
                        @Override
                        public void notify(EventCallback callback) {
                            callback.onInfo(MediaPlayer2Impl.this, item,
                                    MEDIA_INFO_DATA_SOURCE_LIST_END, 0);
                        }
                    });
                    return null;
                } else {
                    if (mQueue.get(1).mSetAsNextPlayer) {
                        // Transition to next player will be handled by MEDIA_INFO_STARTED_AS_NEXT
                        // event later.
                        return null;
                    }
                    moveToNext();
                    return playCurrent();
                }
            } else {
                Log.w(TAG, "Invalid playback complete callback from " + mp.toString());
                return null;
            }
        }

        synchronized void onStartedAsNext(MediaPlayer mp) {
            if (mQueue.size() >= 2 && mQueue.get(1).mPlayer == mp) {
                moveToNext();
                final MediaPlayerSource src = getFirst();
                setMp2State(src.getPlayer(), PLAYER_STATE_PLAYING);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                MEDIA_INFO_DATA_SOURCE_START, 0);
                    }
                });
                prepareAt(1);
                applyProperties();
            }
        }

        synchronized void moveToNext() {
            final MediaPlayerSource src1 = mQueue.remove(0);
            src1.release();
            if (mQueue.isEmpty()) {
                throw new IllegalStateException("player/source queue emptied");
            }
        }

        synchronized DataSourceError playCurrent() {
            DataSourceError err = null;
            applyProperties();

            final MediaPlayerSource src = mQueue.get(0);
            if (src.mSourceState == SOURCE_STATE_PREPARED) {
                // start next source only when it's in prepared state.
                src.getPlayer().start();
                setMp2State(src.getPlayer(), PLAYER_STATE_PLAYING);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                MEDIA_INFO_DATA_SOURCE_START, 0);
                    }
                });
                prepareAt(1);

            } else {
                if (src.mSourceState == SOURCE_STATE_INIT) {
                    err = prepareAt(0);
                }
                src.mPlayPending = true;
            }
            return err;
        }

        synchronized void applyProperties() {
            final MediaPlayerSource src = mQueue.get(0);
            if (mSurface != null) {
                src.getPlayer().setSurface(mSurface);
            }
            if (mVolume != null) {
                src.getPlayer().setVolume(mVolume, mVolume);
            }
            if (mAudioAttributes != null) {
                src.getPlayer().setAudioAttributes((AudioAttributes) mAudioAttributes.unwrap());
            }
            if (mAuxEffect != null) {
                src.getPlayer().attachAuxEffect(mAuxEffect);
            }
            if (mAuxEffectSendLevel != null) {
                src.getPlayer().setAuxEffectSendLevel(mAuxEffectSendLevel);
            }
            if (mPlaybackParams != DEFAULT_PLAYBACK_PARAMS) {
                src.getPlayer().setPlaybackParams(mPlaybackParams);
            }
        }

        synchronized void onError(MediaPlayer mp) {
            setMp2State(mp, PLAYER_STATE_ERROR);
            setBufferingState(mp, SessionPlayer.BUFFERING_STATE_UNKNOWN);
        }

        synchronized DataSourceError prepareAt(int n) {
            if (n >= Math.min(2, mQueue.size())
                    || mQueue.get(n).mSourceState != SOURCE_STATE_INIT
                    || (n != 0 && getPlayerState() == SessionPlayer.PLAYER_STATE_IDLE)) {
                // There is no next source or it's in preparing or prepared state.
                return null;
            }

            MediaPlayerSource src = mQueue.get(n);
            try {
                // Apply audio session ID before calling setDataSource().
                if (mAudioSessionId != null) {
                    src.getPlayer().setAudioSessionId(mAudioSessionId);
                }
                src.mSourceState = SOURCE_STATE_PREPARING;
                handleDataSource(src);
                src.getPlayer().prepareAsync();
                return null;
            } catch (Exception e) {
                MediaItem item = src.getDSD();
                setMp2State(src.getPlayer(), PLAYER_STATE_ERROR);
                return new DataSourceError(item, MEDIA_ERROR_UNKNOWN, 0);
            }

        }

        synchronized void skipToNext() {
            if (mQueue.size() <= 1) {
                throw new IllegalStateException("No next source available");
            }
            final MediaPlayerSource src = mQueue.get(0);
            moveToNext();
            if (src.mPlayerState == SessionPlayer.PLAYER_STATE_PLAYING || src.mPlayPending) {
                playCurrent();
            }
        }

        synchronized void setLooping(boolean loop) {
            mLooping = loop;
        }

        synchronized void setPlaybackParams(final android.media.PlaybackParams params) {
            if (params == null || params.getSpeed() == 0f) {
                throw new IllegalArgumentException();
            }
            android.media.PlaybackParams current = getPlaybackParams();
            MediaPlayerSource firstPlayer = mPlayer.getFirst();
            if (firstPlayer.mMp2State != PLAYER_STATE_PLAYING) {
                // MediaPlayer1 may start the playback on setPlaybackParams. Store the value here
                // so that it can be applied later when starting the playback.
                mPlaybackParamsToSetWhenStarting = params;
            } else {
                firstPlayer.mPlayer.setPlaybackParams(params);
                mPlaybackParamsToSetWhenStarting = null;
            }

            mPlaybackParams = params;
        }

        synchronized float getVolume() {
            return mVolume;
        }

        synchronized void setVolume(float volume) {
            mVolume = volume;
            getCurrentPlayer().setVolume(volume, volume);
        }

        synchronized void setSurface(Surface surface) {
            mSurface = surface;
            getCurrentPlayer().setSurface(surface);
        }

        synchronized int getVideoWidth() {
            try {
                return getCurrentPlayer().getVideoWidth();
            } catch (IllegalStateException e) {
                return 0;
            }
        }

        synchronized int getVideoHeight() {
            try {
                return getCurrentPlayer().getVideoHeight();
            } catch (IllegalStateException e) {
                return 0;
            }
        }

        synchronized PersistableBundle getMetrics() {
            return getCurrentPlayer().getMetrics();
        }

        synchronized android.media.PlaybackParams getPlaybackParams() {
            // PlaybackParams is mutable. Make a copy of mPlaybackParams and return.
            Parcel parcel = Parcel.obtain();
            mPlaybackParams.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            android.media.PlaybackParams ret =
                    android.media.PlaybackParams.CREATOR.createFromParcel(parcel);
            parcel.recycle();
            return ret;
        }

        synchronized void seekTo(long msec, int mode) {
            MediaItem current = getFirst().getDSD();
            Preconditions.checkArgument(
                    current.getStartPosition() <= msec && current.getEndPosition() >= msec,
                    "Requested seek position is out of range : " + msec);
            getCurrentPlayer().seekTo(msec, mode);
        }

        synchronized void reset() {
            mVolume = 1.0f;
            mSurface = null;
            mAuxEffect = null;
            mAuxEffectSendLevel = null;
            mAudioAttributes = null;
            mAudioSessionId = null;
            mPlaybackParams = DEFAULT_PLAYBACK_PARAMS;
            mPlaybackParamsToSetWhenStarting = null;
            mLooping = false;

            MediaPlayerSource first = mQueue.get(0);
            setMp2State(first.getPlayer(), PLAYER_STATE_IDLE);
            setBufferingState(first.getPlayer(), SessionPlayer.BUFFERING_STATE_UNKNOWN);

            for (MediaPlayerSource src : mQueue) {
                src.release();
            }
            mQueue.clear();
            mQueue.add(new MediaPlayerSource(null));
        }

        synchronized MediaTimestamp getTimestamp() {
            android.media.MediaTimestamp t = getCurrentPlayer().getTimestamp();
            return (t == null) ? null : new MediaTimestamp(t);
        }

        synchronized void setAudioSessionId(int sessionId) {
            getCurrentPlayer().setAudioSessionId(sessionId);
            mAudioSessionId = Integer.valueOf(sessionId);
        }

        synchronized int getAudioSessionId() {
            return getCurrentPlayer().getAudioSessionId();
        }

        synchronized void attachAuxEffect(int effectId) {
            getCurrentPlayer().attachAuxEffect(effectId);
            mAuxEffect = Integer.valueOf(effectId);
        }

        synchronized void setAuxEffectSendLevel(float level) {
            getCurrentPlayer().setAuxEffectSendLevel(level);
            mAuxEffectSendLevel = Float.valueOf(level);
        }

        synchronized MediaPlayer.TrackInfo[] getTrackInfo() {
            return getCurrentPlayer().getTrackInfo();
        }

        synchronized int getSelectedTrack(int trackType) {
            return getCurrentPlayer().getSelectedTrack(trackType);
        }

        synchronized void selectTrack(int index) {
            getCurrentPlayer().selectTrack(index);
        }

        synchronized void deselectTrack(int index) {
            getCurrentPlayer().deselectTrack(index);
        }

        synchronized MediaPlayer.DrmInfo getDrmInfo() {
            return getCurrentPlayer().getDrmInfo();
        }

        synchronized void prepareDrm(UUID uuid)
                throws ResourceBusyException, MediaPlayer.ProvisioningServerErrorException,
                MediaPlayer.ProvisioningNetworkErrorException, UnsupportedSchemeException {
            getCurrentPlayer().prepareDrm(uuid);
        }

        synchronized void releaseDrm() throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().stop();
            getCurrentPlayer().releaseDrm();
        }

        synchronized byte[] provideKeyResponse(byte[] keySetId, byte[] response)
                throws DeniedByServerException, MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().provideKeyResponse(keySetId, response);
        }

        synchronized void restoreKeys(byte[] keySetId) throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().restoreKeys(keySetId);
        }

        synchronized String getDrmPropertyString(String propertyName)
                throws MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().getDrmPropertyString(propertyName);
        }

        synchronized void setDrmPropertyString(String propertyName, String value)
                throws MediaPlayer.NoDrmSchemeException {
            getCurrentPlayer().setDrmPropertyString(propertyName, value);
        }

        synchronized void setOnDrmConfigHelper(MediaPlayer.OnDrmConfigHelper onDrmConfigHelper) {
            getCurrentPlayer().setOnDrmConfigHelper(onDrmConfigHelper);
        }

        synchronized MediaDrm.KeyRequest getKeyRequest(byte[] keySetId, byte[] initData,
                String mimeType,
                int keyType, Map<String, String> optionalParameters)
                throws MediaPlayer.NoDrmSchemeException {
            return getCurrentPlayer().getKeyRequest(keySetId, initData, mimeType, keyType,
                    optionalParameters);
        }

        synchronized void setMp2State(MediaPlayer mp, @MediaPlayer2State int mp2State) {
            for (final MediaPlayerSource src: mQueue) {
                if (src.getPlayer() != mp) {
                    continue;
                }
                if (src.mMp2State == mp2State) {
                    return;
                }
                src.mMp2State = mp2State;

                final int playerState = sStateMap.get(mp2State);
                if (src.mPlayerState == playerState) {
                    return;
                }
                src.mPlayerState = playerState;
                return;
            }
        }

        synchronized void setBufferingState(MediaPlayer mp, @BuffState final int state) {
            for (final MediaPlayerSource src: mQueue) {
                if (src.getPlayer() != mp) {
                    continue;
                }
                if (src.mBufferingState == state) {
                    return;
                }
                src.mBufferingState = state;
                return;
            }
        }

        synchronized @MediaPlayer2State int getMediaPlayer2State() {
            return mQueue.get(0).mMp2State;
        }

        synchronized @BuffState int getBufferingState() {
            return mQueue.get(0).mBufferingState;
        }

        synchronized @PlayerState int getPlayerState() {
            return mQueue.get(0).mPlayerState;
        }

        synchronized MediaPlayerSource getSourceForPlayer(MediaPlayer mp) {
            for (MediaPlayerSource src: mQueue) {
                if (src.getPlayer() == mp) {
                    return src;
                }
            }
            return null;
        }
    }
}
