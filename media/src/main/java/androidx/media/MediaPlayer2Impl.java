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
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.DeniedByServerException;
import android.media.MediaDataSource;
import android.media.MediaDrm;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.media.ResourceBusyException;
import android.media.SubtitleData;
import android.media.SyncParams;
import android.media.TimedMetaData;
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
import androidx.core.util.Preconditions;
import androidx.media.MediaPlayerInterface.BuffState;
import androidx.media.MediaPlayerInterface.PlayerEventCallback;
import androidx.media.MediaPlayerInterface.PlayerState;

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
import java.util.concurrent.atomic.AtomicInteger;

/**
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

    private static ArrayMap<Integer, Integer> sInfoEventMap;
    private static ArrayMap<Integer, Integer> sErrorEventMap;
    private static ArrayMap<Integer, Integer> sPrepareDrmStatusMap;
    private static ArrayMap<Integer, Integer> sStateMap;

    static {
        sInfoEventMap = new ArrayMap<>();
        sInfoEventMap.put(MediaPlayer.MEDIA_INFO_UNKNOWN, MEDIA_INFO_UNKNOWN);
        sInfoEventMap.put(2 /*MediaPlayer.MEDIA_INFO_STARTED_AS_NEXT*/, MEDIA_INFO_STARTED_AS_NEXT);
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

        sErrorEventMap = new ArrayMap<>();
        sErrorEventMap.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_UNKNOWN);
        sErrorEventMap.put(
                MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
                MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK);
        sErrorEventMap.put(MediaPlayer.MEDIA_ERROR_IO, MEDIA_ERROR_IO);
        sErrorEventMap.put(MediaPlayer.MEDIA_ERROR_MALFORMED, MEDIA_ERROR_MALFORMED);
        sErrorEventMap.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, MEDIA_ERROR_UNSUPPORTED);
        sErrorEventMap.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, MEDIA_ERROR_TIMED_OUT);

        sPrepareDrmStatusMap = new ArrayMap<>();
        sPrepareDrmStatusMap.put(
                MediaPlayer.PREPARE_DRM_STATUS_SUCCESS, PREPARE_DRM_STATUS_SUCCESS);
        sPrepareDrmStatusMap.put(
                MediaPlayer.PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR,
                PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR);
        sPrepareDrmStatusMap.put(
                MediaPlayer.PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR,
                PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR);
        sPrepareDrmStatusMap.put(
                MediaPlayer.PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR,
                PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR);

        sStateMap = new ArrayMap<>();
        sStateMap.put(MEDIAPLAYER2_STATE_IDLE, MediaPlayerInterface.PLAYER_STATE_IDLE);
        sStateMap.put(MEDIAPLAYER2_STATE_PREPARED, MediaPlayerInterface.PLAYER_STATE_PAUSED);
        sStateMap.put(MEDIAPLAYER2_STATE_PAUSED, MediaPlayerInterface.PLAYER_STATE_PAUSED);
        sStateMap.put(MEDIAPLAYER2_STATE_PLAYING, MediaPlayerInterface.PLAYER_STATE_PLAYING);
        sStateMap.put(MEDIAPLAYER2_STATE_ERROR, MediaPlayerInterface.PLAYER_STATE_ERROR);
    }

    private MediaPlayerSourceQueue mPlayer;

    private HandlerThread mHandlerThread;
    private final Handler mTaskHandler;
    private final Object mTaskLock = new Object();
    @GuardedBy("mTaskLock")
    private final ArrayDeque<Task> mPendingTasks = new ArrayDeque<>();
    @GuardedBy("mTaskLock")
    private Task mCurrentTask;

    private final Object mLock = new Object();
    //--- guarded by |mLock| start
    private AudioAttributesCompat mAudioAttributes;
    private ArrayList<Pair<Executor, MediaPlayer2EventCallback>> mMp2EventCallbackRecords =
            new ArrayList<>();
    private ArrayMap<PlayerEventCallback, Executor> mPlayerEventCallbackMap =
            new ArrayMap<>();
    private ArrayList<Pair<Executor, DrmEventCallback>> mDrmEventCallbackRecords =
            new ArrayList<>();
    private MediaPlayerInterfaceImpl mMediaPlayerInterfaceImpl;
    //--- guarded by |mLock| end

    private void handleDataSourceError(final DataSourceError err) {
        if (err == null) {
            return;
        }
        notifyMediaPlayer2Event(new Mp2EventNotifier() {
            @Override
            public void notify(MediaPlayer2EventCallback callback) {
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
        mTaskHandler = new Handler(looper);

        // TODO: To make sure MediaPlayer1 listeners work, the caller thread should have a looper.
        // Fix the framework or document this behavior.
        mPlayer = new MediaPlayerSourceQueue();
    }

    /**
     * Returns a {@link MediaPlayerInterface} implementation which runs based on
     * this MediaPlayer2 instance.
     */
    @Override
    public MediaPlayerInterface getMediaPlayerInterface() {
        synchronized (mLock) {
            if (mMediaPlayerInterfaceImpl == null) {
                mMediaPlayerInterfaceImpl = new MediaPlayerInterfaceImpl();
            }
            return mMediaPlayerInterfaceImpl;
        }
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
    public void close() {
        mPlayer.release();
    }

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    @Override
    public void play() {
        addTask(new Task(CALL_COMPLETED_PLAY, false) {
            @Override
            void process() {
                mPlayer.play();
            }
        });
    }

    /**
     * Prepares the player for playback, asynchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare(). For streams, you should call prepare(),
     * which returns immediately, rather than blocking until enough data has been
     * buffered.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    @Override
    public void prepare() {
        addTask(new Task(CALL_COMPLETED_PREPARE, true) {
            @Override
            void process() throws IOException {
                mPlayer.prepareAsync();
            }
        });
    }

    /**
     * Pauses playback. Call play() to resume.
     *
     * @throws IllegalStateException if the internal player engine has not been initialized.
     */
    @Override
    public void pause() {
        addTask(new Task(CALL_COMPLETED_PAUSE, false) {
            @Override
            void process() {
                mPlayer.pause();
            }
        });
    }

    /**
     * Tries to play next data source if applicable.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    @Override
    public void skipToNext() {
        addTask(new Task(CALL_COMPLETED_SKIP_TO_NEXT, false) {
            @Override
            void process() {
                mPlayer.skipToNext();
            }
        });
    }

    /**
     * Gets the current playback position.
     *
     * @return the current position in milliseconds
     */
    @Override
    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    /**
     * Gets the duration of the file.
     *
     * @return the duration in milliseconds, if no duration is available
     * (for example, if streaming live content), -1 is returned.
     */
    @Override
    public long getDuration() {
        return mPlayer.getDuration();
    }

    /**
     * Gets the current buffered media source position received through progressive downloading.
     * The received buffering percentage indicates how much of the content has been buffered
     * or played. For example a buffering update of 80 percent when half the content
     * has already been played indicates that the next 30 percent of the
     * content to play has been buffered.
     *
     * @return the current buffered media source position in milliseconds
     */
    @Override
    public long getBufferedPosition() {
        // Use cached buffered percent for now.
        return mPlayer.getBufferedPosition();
    }

    /**
     * Gets the current MediaPlayer2 state.
     *
     * @return the current MediaPlayer2 state.
     */
    @Override
    public @MediaPlayer2State int getMediaPlayer2State() {
        return mPlayer.getMediaPlayer2State();
    }

    private @MediaPlayerInterface.PlayerState int getPlayerState() {
        return mPlayer.getPlayerState();
    }

    /**
     * Gets the current buffering state of the player.
     * During buffering, see {@link #getBufferedPosition()} for the quantifying the amount already
     * buffered.
     */
    private @MediaPlayerInterface.BuffState int getBufferingState() {
        return  mPlayer.getBufferingState();
    }

    /**
     * Sets the audio attributes for this MediaPlayer2.
     * See {@link AudioAttributes} for how to build and configure an instance of this class.
     * You must call this method before {@link #prepare()} in order
     * for the audio attributes to become effective thereafter.
     * @param attributes a non-null set of audio attributes
     * @throws IllegalArgumentException if the attributes are null or invalid.
     */
    @Override
    public void setAudioAttributes(@NonNull final AudioAttributesCompat attributes) {
        addTask(new Task(CALL_COMPLETED_SET_AUDIO_ATTRIBUTES, false) {
            @Override
            void process() {
                AudioAttributes attr;
                synchronized (mLock) {
                    mAudioAttributes = attributes;
                    attr = (AudioAttributes) mAudioAttributes.unwrap();
                }
                mPlayer.setAudioAttributes(attr);
            }
        });
    }

    @Override
    public @NonNull AudioAttributesCompat getAudioAttributes() {
        synchronized (mLock) {
            return mAudioAttributes;
        }
    }

    /**
     * Sets the data source as described by a DataSourceDesc.
     *
     * @param dsd the descriptor of data source you want to play
     * @throws IllegalStateException if it is called in an invalid state
     * @throws NullPointerException if dsd is null
     */
    @Override
    public void setDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(CALL_COMPLETED_SET_DATA_SOURCE, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                // TODO: setDataSource could update exist data source
                try {
                    mPlayer.setFirst(dsd);
                } catch (IOException e) {
                    Log.e(TAG, "process: setDataSource", e);
                }
            }
        });
    }

    /**
     * Sets a single data source as described by a DataSourceDesc which will be played
     * after current data source is finished.
     *
     * @param dsd the descriptor of data source you want to play after current one
     * @throws IllegalStateException if it is called in an invalid state
     * @throws NullPointerException if dsd is null
     */
    @Override
    public void setNextDataSource(@NonNull final DataSourceDesc dsd) {
        addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCE, false) {
            @Override
            void process() {
                Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");
                handleDataSourceError(mPlayer.setNext(dsd));
            }
        });
    }

    /**
     * Sets a list of data sources to be played sequentially after current data source is done.
     *
     * @param dsds the list of data sources you want to play after current one
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if dsds is null or empty, or contains null DataSourceDesc
     */
    @Override
    public void setNextDataSources(@NonNull final List<DataSourceDesc> dsds) {
        addTask(new Task(CALL_COMPLETED_SET_NEXT_DATA_SOURCES, false) {
            @Override
            void process() {
                if (dsds == null || dsds.size() == 0) {
                    throw new IllegalArgumentException("data source list cannot be null or empty.");
                }
                for (DataSourceDesc dsd : dsds) {
                    if (dsd == null) {
                        throw new IllegalArgumentException(
                                "DataSourceDesc in the source list cannot be null.");
                    }
                }
                handleDataSourceError(mPlayer.setNextMultiple(dsds));
            }
        });
    }

    @Override
    public @NonNull DataSourceDesc getCurrentDataSource() {
        return mPlayer.getFirst().getDSD();
    }

    /**
     * Configures the player to loop on the current data source.
     * @param loop true if the current data source is meant to loop.
     */
    @Override
    public void loopCurrent(final boolean loop) {
        addTask(new Task(CALL_COMPLETED_LOOP_CURRENT, false) {
            @Override
            void process() {
                mPlayer.setLooping(loop);
            }
        });
    }

    /**
     * Sets the playback speed.
     * A value of 1.0f is the default playback value.
     * A negative value indicates reverse playback, check {@link #isReversePlaybackSupported()}
     * before using negative values.<br>
     * After changing the playback speed, it is recommended to query the actual speed supported
     * by the player, see {@link #getPlaybackSpeed()}.
     * @param speed the desired playback speed
     */
    @Override
    public void setPlaybackSpeed(final float speed) {
        addTask(new Task(CALL_COMPLETED_SET_PLAYBACK_SPEED, false) {
            @Override
            void process() {
                setPlaybackParamsInternal(getPlaybackParams().setSpeed(speed));
            }
        });
    }

    /**
     * Returns the actual playback speed to be used by the player when playing.
     * Note that it may differ from the speed set in {@link #setPlaybackSpeed(float)}.
     * @return the actual playback speed
     */
    @Override
    public float getPlaybackSpeed() {
        return getPlaybackParams().getSpeed();
    }

    /**
     * Indicates whether reverse playback is supported.
     * Reverse playback is indicated by negative playback speeds, see
     * {@link #setPlaybackSpeed(float)}.
     * @return true if reverse playback is supported.
     */
    @Override
    public boolean isReversePlaybackSupported() {
        return false;
    }

    /**
     * Sets the volume of the audio of the media to play, expressed as a linear multiplier
     * on the audio samples.
     * Note that this volume is specific to the player, and is separate from stream volume
     * used across the platform.<br>
     * A value of 0.0f indicates muting, a value of 1.0f is the nominal unattenuated and unamplified
     * gain. See {@link #getMaxPlayerVolume()} for the volume range supported by this player.
     * @param volume a value between 0.0f and {@link #getMaxPlayerVolume()}.
     */
    @Override
    public void setPlayerVolume(final float volume) {
        addTask(new Task(CALL_COMPLETED_SET_PLAYER_VOLUME, false) {
            @Override
            void process() {
                mPlayer.setVolume(volume);
            }
        });
    }

    /**
     * Returns the current volume of this player to this player.
     * Note that it does not take into account the associated stream volume.
     * @return the player volume.
     */
    @Override
    public float getPlayerVolume() {
        return mPlayer.getVolume();
    }

    /**
     * @return the maximum volume that can be used in {@link #setPlayerVolume(float)}.
     */
    @Override
    public float getMaxPlayerVolume() {
        return 1.0f;
    }

    /**
     * Adds a callback to be notified of events for this player.
     * @param e the {@link Executor} to be used for the events.
     * @param cb the callback to receive the events.
     */
    private void registerPlayerEventCallback(@NonNull Executor e,
            @NonNull PlayerEventCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null PlayerEventCallback");
        }
        if (e == null) {
            throw new IllegalArgumentException(
                    "Illegal null Executor for the PlayerEventCallback");
        }
        synchronized (mLock) {
            mPlayerEventCallbackMap.put(cb, e);
        }
    }

    /**
     * Removes a previously registered callback for player events
     * @param cb the callback to remove
     */
    private void unregisterPlayerEventCallback(@NonNull PlayerEventCallback cb) {
        if (cb == null) {
            throw new IllegalArgumentException("Illegal null PlayerEventCallback");
        }
        synchronized (mLock) {
            mPlayerEventCallbackMap.remove(cb);
        }
    }

    @Override
    public void notifyWhenCommandLabelReached(final Object label) {
        addTask(new Task(CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED, false) {
            @Override
            void process() {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        cb.onCommandLabelReached(MediaPlayer2Impl.this, label);
                    }
                });
            }
        });
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of
     * the media. Setting a Surface will un-set any Surface or SurfaceHolder that
     * was previously set. A null surface will result in only the audio track
     * being played.
     *
     * If the Surface sends frames to a {@link SurfaceTexture}, the timestamps
     * returned from {@link SurfaceTexture#getTimestamp()} will have an
     * unspecified zero point.  These timestamps cannot be directly compared
     * between different media sources, different instances of the same media
     * source, or multiple runs of the same program.  The timestamp is normally
     * monotonically increasing and is unaffected by time-of-day adjustments,
     * but it is reset when the position is set.
     *
     * @param surface The {@link Surface} to be used for the video portion of
     * the media.
     * @throws IllegalStateException if the internal player engine has not been
     * initialized or has been released.
     */
    @Override
    public void setSurface(final Surface surface) {
        addTask(new Task(CALL_COMPLETED_SET_SURFACE, false) {
            @Override
            void process() {
                mPlayer.setSurface(surface);
            }
        });
    }

    /**
     * Discards all pending commands.
     */
    @Override
    public void clearPendingCommands() {
        synchronized (mTaskLock) {
            mPendingTasks.clear();
        }
    }

    private void addTask(Task task) {
        synchronized (mTaskLock) {
            mPendingTasks.add(task);
            processPendingTask_l();
        }
    }

    @GuardedBy("mTaskLock")
    private void processPendingTask_l() {
        if (mCurrentTask != null) {
            return;
        }
        if (!mPendingTasks.isEmpty()) {
            Task task = mPendingTasks.removeFirst();
            mCurrentTask = task;
            mTaskHandler.post(task);
        }
    }

    private static void handleDataSource(MediaPlayerSource src)
            throws IOException {
        final DataSourceDesc dsd = src.getDSD();
        Preconditions.checkNotNull(dsd, "the DataSourceDesc cannot be null");

        MediaPlayer player = src.mPlayer;
        switch (dsd.getType()) {
            case DataSourceDesc.TYPE_CALLBACK:
                player.setDataSource(new MediaDataSource() {
                    Media2DataSource mDataSource = dsd.getMedia2DataSource();
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
                break;

            case DataSourceDesc.TYPE_FD:
                player.setDataSource(
                        dsd.getFileDescriptor(),
                        dsd.getFileDescriptorOffset(),
                        dsd.getFileDescriptorLength());
                break;

            case DataSourceDesc.TYPE_URI:
                player.setDataSource(
                        dsd.getUriContext(),
                        dsd.getUri(),
                        dsd.getUriHeaders(),
                        dsd.getUriCookies());
                break;

            default:
                break;
        }
    }

    /**
     * Returns the width of the video.
     *
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The {@code MediaPlayer2EventCallback} can be registered via
     * {@link #setMediaPlayer2EventCallback(Executor, MediaPlayer2EventCallback)} to provide a
     * notification {@code MediaPlayer2EventCallback.onVideoSizeChanged} when the width
     * is available.
     */
    @Override
    public int getVideoWidth() {
        return mPlayer.getVideoWidth();
    }

    /**
     * Returns the height of the video.
     *
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The {@code MediaPlayer2EventCallback} can be registered via
     * {@link #setMediaPlayer2EventCallback(Executor, MediaPlayer2EventCallback)} to provide a
     * notification {@code MediaPlayer2EventCallback.onVideoSizeChanged} when the height
     * is available.
     */
    @Override
    public int getVideoHeight() {
        return mPlayer.getVideoHeight();
    }

    @Override
    public PersistableBundle getMetrics() {
        return mPlayer.getMetrics();
    }

    /**
     * Sets playback rate using {@link PlaybackParams}. The object sets its internal
     * PlaybackParams to the input, except that the object remembers previous speed
     * when input speed is zero. This allows the object to resume at previous speed
     * when play() is called. Calling it before the object is prepared does not change
     * the object state. After the object is prepared, calling it with zero speed is
     * equivalent to calling pause(). After the object is prepared, calling it with
     * non-zero speed is equivalent to calling play().
     *
     * @param params the playback params.
     * @throws IllegalStateException if the internal player engine has not been
     * initialized or has been released.
     * @throws IllegalArgumentException if params is not supported.
     */
    @Override
    public void setPlaybackParams(@NonNull final PlaybackParams params) {
        addTask(new Task(CALL_COMPLETED_SET_PLAYBACK_PARAMS, false) {
            @Override
            void process() {
                setPlaybackParamsInternal(params);
            }
        });
    }

    /**
     * Gets the playback params, containing the current playback rate.
     *
     * @return the playback params.
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    @Override
    @NonNull
    public PlaybackParams getPlaybackParams() {
        return mPlayer.getPlaybackParams();
    }

    /**
     * Sets A/V sync mode.
     *
     * @param params the A/V sync params to apply
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     * @throws IllegalArgumentException if params are not supported.
     */
    @Override
    public void setSyncParams(@NonNull final SyncParams params) {
        addTask(new Task(CALL_COMPLETED_SET_SYNC_PARAMS, false) {
            @Override
            void process() {
                mPlayer.setSyncParams(params);
            }
        });
    }

    /**
     * Gets the A/V sync mode.
     *
     * @return the A/V sync params
     * @throws IllegalStateException if the internal player engine has not been
     *                               initialized.
     */
    @Override
    @NonNull
    public SyncParams getSyncParams() {
        return mPlayer.getSyncParams();
    }

    /**
     * Moves the media to specified time position by considering the given mode.
     * <p>
     * When seekTo is finished, the user will be notified via OnSeekComplete supplied by the user.
     * There is at most one active seekTo processed at any time. If there is a to-be-completed
     * seekTo, new seekTo requests will be queued in such a way that only the last request
     * is kept. When current seekTo is completed, the queued request will be processed if
     * that request is different from just-finished seekTo operation, i.e., the requested
     * position or mode is different.
     *
     * @param msec the offset in milliseconds from the start to seek to.
     * When seeking to the given time position, there is no guarantee that the data source
     * has a frame located at the position. When this happens, a frame nearby will be rendered.
     * If msec is negative, time position zero will be used.
     * If msec is larger than duration, duration will be used.
     * @param mode the mode indicating where exactly to seek to.
     * Use {@link #SEEK_PREVIOUS_SYNC} if one wants to seek to a sync frame
     * that has a timestamp earlier than or the same as msec. Use
     * {@link #SEEK_NEXT_SYNC} if one wants to seek to a sync frame
     * that has a timestamp later than or the same as msec. Use
     * {@link #SEEK_CLOSEST_SYNC} if one wants to seek to a sync frame
     * that has a timestamp closest to or the same as msec. Use
     * {@link #SEEK_CLOSEST} if one wants to seek to a frame that may
     * or may not be a sync frame but is closest to or the same as msec.
     * {@link #SEEK_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at msec.
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     * @throws IllegalArgumentException if the mode is invalid.
     */
    @Override
    public void seekTo(final long msec, @SeekMode final int mode) {
        addTask(new Task(CALL_COMPLETED_SEEK_TO, true) {
            @Override
            void process() {
                mPlayer.seekTo(msec, mode);
            }
        });
    }

    /**
     * Get current playback position as a {@link MediaTimestamp}.
     * <p>
     * The MediaTimestamp represents how the media time correlates to the system time in
     * a linear fashion using an anchor and a clock rate. During regular playback, the media
     * time moves fairly constantly (though the anchor frame may be rebased to a current
     * system time, the linear correlation stays steady). Therefore, this method does not
     * need to be called often.
     * <p>
     * To help users get current playback position, this method always anchors the timestamp
     * to the current {@link System#nanoTime system time}, so
     * {@link MediaTimestamp#getAnchorMediaTimeUs} can be used as current playback position.
     *
     * @return a MediaTimestamp object if a timestamp is available, or {@code null} if no timestamp
     * is available, e.g. because the media player has not been initialized.
     * @see MediaTimestamp
     */
    @Override
    @Nullable
    public MediaTimestamp getTimestamp() {
        return mPlayer.getTimestamp();
    }

    /**
     * Resets the MediaPlayer2 to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     */
    @Override
    public void reset() {
        mPlayer.reset();
        synchronized (mLock) {
            mAudioAttributes = null;
            mMp2EventCallbackRecords.clear();
            mPlayerEventCallbackMap.clear();
            mDrmEventCallbackRecords.clear();
        }
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId the audio session ID.
     * The audio session ID is a system wide unique identifier for the audio stream played by
     * this MediaPlayer2 instance.
     * The primary use of the audio session ID  is to associate audio effects to a particular
     * instance of MediaPlayer2: if an audio session ID is provided when creating an audio effect,
     * this effect will be applied only to the audio content of media players within the same
     * audio session and not to the output mix.
     * When created, a MediaPlayer2 instance automatically generates its own audio session ID.
     * However, it is possible to force this player to be part of an already existing audio session
     * by calling this method.
     * This method must be called before one of the overloaded <code> setDataSource </code> methods.
     * @throws IllegalStateException if it is called in an invalid state
     * @throws IllegalArgumentException if the sessionId is invalid.
     */
    @Override
    public void setAudioSessionId(final int sessionId) {
        addTask(new Task(CALL_COMPLETED_SET_AUDIO_SESSION_ID, false) {
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

    /**
     * Attaches an auxiliary effect to the player. A typical auxiliary effect is a reverberation
     * effect which can be applied on any sound source that directs a certain amount of its
     * energy to this effect. This amount is defined by setAuxEffectSendLevel().
     * See {@link #setAuxEffectSendLevel(float)}.
     * <p>After creating an auxiliary effect (e.g.
     * {@link android.media.audiofx.EnvironmentalReverb}), retrieve its ID with
     * {@link android.media.audiofx.AudioEffect#getId()} and use it when calling this method
     * to attach the player to the effect.
     * <p>To detach the effect from the player, call this method with a null effect id.
     * <p>This method must be called after one of the overloaded <code> setDataSource </code>
     * methods.
     * @param effectId system wide unique id of the effect to attach
     */
    @Override
    public void attachAuxEffect(final int effectId) {
        addTask(new Task(CALL_COMPLETED_ATTACH_AUX_EFFECT, false) {
            @Override
            void process() {
                mPlayer.attachAuxEffect(effectId);
            }
        });
    }

    /**
     * Sets the send level of the player to the attached auxiliary effect.
     * See {@link #attachAuxEffect(int)}. The level value range is 0 to 1.0.
     * <p>By default the send level is 0, so even if an effect is attached to the player
     * this method must be called for the effect to be applied.
     * <p>Note that the passed level value is a raw scalar. UI controls should be scaled
     * logarithmically: the gain applied by audio framework ranges from -72dB to 0dB,
     * so an appropriate conversion from linear UI input x to level is:
     * x == 0 -> level = 0
     * 0 < x <= R -> level = 10^(72*(x-R)/20/R)
     * @param level send level scalar
     */
    @Override
    public void setAuxEffectSendLevel(final float level) {
        addTask(new Task(CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL, false) {
            @Override
            void process() {
                mPlayer.setAuxEffectSendLevel(level);
            }
        });
    }

    /**
     * Class for MediaPlayer2 to return each audio/video/subtitle track's metadata.
     *
     * @see MediaPlayer2#getTrackInfo
     */
    public static final class TrackInfoImpl extends TrackInfo {
        final int mTrackType;
        final MediaFormat mFormat;

        /**
         * Gets the track type.
         * @return TrackType which indicates if the track is video, audio, timed text.
         */
        @Override
        public int getTrackType() {
            return mTrackType;
        }

        /**
         * Gets the language code of the track.
         * @return a language code in either way of ISO-639-1 or ISO-639-2.
         * When the language is unknown or could not be determined,
         * ISO-639-2 language code, "und", is returned.
         */
        @Override
        public String getLanguage() {
            String language = mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        /**
         * Gets the {@link MediaFormat} of the track.  If the format is
         * unknown or could not be determined, null is returned.
         */
        @Override
        public MediaFormat getFormat() {
            if (mTrackType == MEDIA_TRACK_TYPE_TIMEDTEXT
                    || mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
                return mFormat;
            }
            return null;
        }

        TrackInfoImpl(int type, MediaFormat format) {
            mTrackType = type;
            mFormat = format;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder(128);
            out.append(getClass().getName());
            out.append('{');
            switch (mTrackType) {
                case MEDIA_TRACK_TYPE_VIDEO:
                    out.append("VIDEO");
                    break;
                case MEDIA_TRACK_TYPE_AUDIO:
                    out.append("AUDIO");
                    break;
                case MEDIA_TRACK_TYPE_TIMEDTEXT:
                    out.append("TIMEDTEXT");
                    break;
                case MEDIA_TRACK_TYPE_SUBTITLE:
                    out.append("SUBTITLE");
                    break;
                default:
                    out.append("UNKNOWN");
                    break;
            }
            out.append(", " + mFormat.toString());
            out.append("}");
            return out.toString();
        }
    };

    /**
     * Returns a List of track information.
     *
     * @return List of track info. The total number of tracks is the array length.
     * Must be called again if an external timed text source has been added after
     * addTimedTextSource method is called.
     * @throws IllegalStateException if it is called in an invalid state.
     */
    @Override
    public List<TrackInfo> getTrackInfo() {
        MediaPlayer.TrackInfo[] list = mPlayer.getTrackInfo();
        List<TrackInfo> trackList = new ArrayList<>();
        for (MediaPlayer.TrackInfo info : list) {
            trackList.add(new TrackInfoImpl(info.getTrackType(), info.getFormat()));
        }
        return trackList;
    }

    /**
     * Returns the index of the audio, video, or subtitle track currently selected for playback,
     * The return value is an index into the array returned by {@link #getTrackInfo()}, and can
     * be used in calls to {@link #selectTrack(int)} or {@link #deselectTrack(int)}.
     *
     * @param trackType should be one of {@link TrackInfo#MEDIA_TRACK_TYPE_VIDEO},
     * {@link TrackInfo#MEDIA_TRACK_TYPE_AUDIO}, or
     * {@link TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE}
     * @return index of the audio, video, or subtitle track currently selected for playback;
     * a negative integer is returned when there is no selected track for {@code trackType} or
     * when {@code trackType} is not one of audio, video, or subtitle.
     * @throws IllegalStateException if called after {@link #close()}
     *
     * @see #getTrackInfo()
     * @see #selectTrack(int)
     * @see #deselectTrack(int)
     */
    @Override
    public int getSelectedTrack(int trackType) {
        return mPlayer.getSelectedTrack(trackType);
    }

    /**
     * Selects a track.
     * <p>
     * If a MediaPlayer2 is in invalid state, it throws an IllegalStateException exception.
     * If a MediaPlayer2 is in <em>Started</em> state, the selected track is presented immediately.
     * If a MediaPlayer2 is not in Started state, it just marks the track to be played.
     * </p>
     * <p>
     * In any valid state, if it is called multiple times on the same type of track (ie. Video,
     * Audio, Timed Text), the most recent one will be chosen.
     * </p>
     * <p>
     * The first audio and video tracks are selected by default if available, even though
     * this method is not called. However, no timed text track will be selected until
     * this function is called.
     * </p>
     * <p>
     * Currently, only timed text tracks or audio tracks can be selected via this method.
     * In addition, the support for selecting an audio track at runtime is pretty limited
     * in that an audio track can only be selected in the <em>Prepared</em> state.
     * </p>
     *
     * @param index the index of the track to be selected. The valid range of the index
     * is 0..total number of track - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     * @throws IllegalStateException if called in an invalid state.
     * @see MediaPlayer2#getTrackInfo
     */
    @Override
    public void selectTrack(final int index) {
        addTask(new Task(CALL_COMPLETED_SELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.selectTrack(index);
            }
        });
    }

    /**
     * Deselect a track.
     * <p>
     * Currently, the track must be a timed text track and no audio or video tracks can be
     * deselected. If the timed text track identified by index has not been
     * selected before, it throws an exception.
     * </p>
     *
     * @param index the index of the track to be deselected. The valid range of the index
     * is 0..total number of tracks - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     * @throws IllegalStateException if called in an invalid state.
     * @see MediaPlayer2#getTrackInfo
     */
    @Override
    public void deselectTrack(final int index) {
        addTask(new Task(CALL_COMPLETED_DESELECT_TRACK, false) {
            @Override
            void process() {
                mPlayer.deselectTrack(index);
            }
        });
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param eventCallback the callback that will be run
     * @param executor the executor through which the callback should be invoked
     */
    @Override
    public void setMediaPlayer2EventCallback(@NonNull Executor executor,
            @NonNull MediaPlayer2EventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Illegal null Executor for the MediaPlayer2EventCallback");
        }
        synchronized (mLock) {
            mMp2EventCallbackRecords.add(new Pair(executor, eventCallback));
        }
    }

    /**
     * Clears the {@link MediaPlayer2EventCallback}.
     */
    @Override
    public void clearMediaPlayer2EventCallback() {
        synchronized (mLock) {
            mMp2EventCallbackRecords.clear();
        }
    }

    // Modular DRM begin

    /**
     * Register a callback to be invoked for configuration of the DRM object before
     * the session is created.
     * The callback will be invoked synchronously during the execution
     * of {@link #prepareDrm(UUID uuid)}.
     *
     * @param listener the callback that will be run
     */
    @Override
    public void setOnDrmConfigHelper(final OnDrmConfigHelper listener) {
        mPlayer.setOnDrmConfigHelper(new MediaPlayer.OnDrmConfigHelper() {
            @Override
            public void onDrmConfig(MediaPlayer mp) {
                MediaPlayerSource src = mPlayer.getSourceForPlayer(mp);
                DataSourceDesc dsd = src == null ? null : src.getDSD();
                listener.onDrmConfig(MediaPlayer2Impl.this, dsd);
            }
        });
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param eventCallback the callback that will be run
     * @param executor the executor through which the callback should be invoked
     */
    @Override
    public void setDrmEventCallback(@NonNull Executor executor,
                                    @NonNull DrmEventCallback eventCallback) {
        if (eventCallback == null) {
            throw new IllegalArgumentException("Illegal null MediaPlayer2EventCallback");
        }
        if (executor == null) {
            throw new IllegalArgumentException(
                    "Illegal null Executor for the MediaPlayer2EventCallback");
        }
        synchronized (mLock) {
            mDrmEventCallbackRecords.add(new Pair(executor, eventCallback));
        }
    }

    /**
     * Clears the {@link DrmEventCallback}.
     */
    @Override
    public void clearDrmEventCallback() {
        synchronized (mLock) {
            mDrmEventCallbackRecords.clear();
        }
    }


    /**
     * Retrieves the DRM Info associated with the current source
     *
     * @throws IllegalStateException if called before prepare()
     */
    @Override
    public DrmInfo getDrmInfo() {
        MediaPlayer.DrmInfo info = mPlayer.getDrmInfo();
        return info == null ? null : new DrmInfoImpl(info.getPssh(), info.getSupportedSchemes());
    }


    /**
     * Prepares the DRM for the current source
     * <p>
     * If {@code OnDrmConfigHelper} is registered, it will be called during
     * preparation to allow configuration of the DRM properties before opening the
     * DRM session. Note that the callback is called synchronously in the thread that called
     * {@code prepareDrm}. It should be used only for a series of {@code getDrmPropertyString}
     * and {@code setDrmPropertyString} calls and refrain from any lengthy operation.
     * <p>
     * If the device has not been provisioned before, this call also provisions the device
     * which involves accessing the provisioning server and can take a variable time to
     * complete depending on the network connectivity.
     * If {@code OnDrmPreparedListener} is registered, prepareDrm() runs in non-blocking
     * mode by launching the provisioning in the background and returning. The listener
     * will be called when provisioning and preparation has finished. If a
     * {@code OnDrmPreparedListener} is not registered, prepareDrm() waits till provisioning
     * and preparation has finished, i.e., runs in blocking mode.
     * <p>
     * If {@code OnDrmPreparedListener} is registered, it is called to indicate the DRM
     * session being ready. The application should not make any assumption about its call
     * sequence (e.g., before or after prepareDrm returns), or the thread context that will
     * execute the listener (unless the listener is registered with a handler thread).
     * <p>
     *
     * @param uuid The UUID of the crypto scheme. If not known beforehand, it can be retrieved
     * from the source through {@code getDrmInfo} or registering a {@code onDrmInfoListener}.
     * @throws IllegalStateException             if called before prepare(), or the DRM was
     *                                           prepared already
     * @throws UnsupportedSchemeException        if the crypto scheme is not supported
     * @throws ResourceBusyException             if required DRM resources are in use
     * @throws ProvisioningNetworkErrorException if provisioning is required but failed due to a
     *                                           network error
     * @throws ProvisioningServerErrorException  if provisioning is required but failed due to
     *                                           the request denied by the provisioning server
     */
    @Override
    public void prepareDrm(@NonNull UUID uuid)
            throws UnsupportedSchemeException, ResourceBusyException,
            ProvisioningNetworkErrorException, ProvisioningServerErrorException {
        try {
            mPlayer.prepareDrm(uuid);
        } catch (MediaPlayer.ProvisioningNetworkErrorException e) {
            throw new ProvisioningNetworkErrorException(e.getMessage());
        } catch (MediaPlayer.ProvisioningServerErrorException e) {
            throw new ProvisioningServerErrorException(e.getMessage());
        }
    }

    /**
     * Releases the DRM session
     * <p>
     * The player has to have an active DRM session and be in stopped, or prepared
     * state before this call is made.
     * A {@code reset()} call will release the DRM session implicitly.
     *
     * @throws NoDrmSchemeException if there is no active DRM session to release
     */
    @Override
    public void releaseDrm() throws NoDrmSchemeException {
        try {
            mPlayer.releaseDrm();
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    /**
     * A key request/response exchange occurs between the app and a license server
     * to obtain or release keys used to decrypt encrypted content.
     * <p>
     * getDrmKeyRequest() is used to obtain an opaque key request byte array that is
     * delivered to the license server.  The opaque key request byte array is returned
     * in KeyRequest.data.  The recommended URL to deliver the key request to is
     * returned in KeyRequest.defaultUrl.
     * <p>
     * After the app has received the key request response from the server,
     * it should deliver to the response to the DRM engine plugin using the method
     * {@link #provideDrmKeyResponse}.
     *
     * @param keySetId is the key-set identifier of the offline keys being released when keyType is
     * {@link MediaDrm#KEY_TYPE_RELEASE}. It should be set to null for other key requests, when
     * keyType is {@link MediaDrm#KEY_TYPE_STREAMING} or {@link MediaDrm#KEY_TYPE_OFFLINE}.
     *
     * @param initData is the container-specific initialization data when the keyType is
     * {@link MediaDrm#KEY_TYPE_STREAMING} or {@link MediaDrm#KEY_TYPE_OFFLINE}. Its meaning is
     * interpreted based on the mime type provided in the mimeType parameter.  It could
     * contain, for example, the content ID, key ID or other data obtained from the content
     * metadata that is required in generating the key request.
     * When the keyType is {@link MediaDrm#KEY_TYPE_RELEASE}, it should be set to null.
     *
     * @param mimeType identifies the mime type of the content
     *
     * @param keyType specifies the type of the request. The request may be to acquire
     * keys for streaming, {@link MediaDrm#KEY_TYPE_STREAMING}, or for offline content
     * {@link MediaDrm#KEY_TYPE_OFFLINE}, or to release previously acquired
     * keys ({@link MediaDrm#KEY_TYPE_RELEASE}), which are identified by a keySetId.
     *
     * @param optionalParameters are included in the key request message to
     * allow a client application to provide additional message parameters to the server.
     * This may be {@code null} if no additional parameters are to be sent.
     *
     * @throws NoDrmSchemeException if there is no active DRM session
     */
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


    /**
     * A key response is received from the license server by the app, then it is
     * provided to the DRM engine plugin using provideDrmKeyResponse. When the
     * response is for an offline key request, a key-set identifier is returned that
     * can be used to later restore the keys to a new session with the method
     * {@ link # restoreDrmKeys}.
     * When the response is for a streaming or release request, null is returned.
     *
     * @param keySetId When the response is for a release request, keySetId identifies
     * the saved key associated with the release request (i.e., the same keySetId
     * passed to the earlier {@ link #getDrmKeyRequest} call. It MUST be null when the
     * response is for either streaming or offline key requests.
     *
     * @param response the byte array response from the server
     *
     * @throws NoDrmSchemeException if there is no active DRM session
     * @throws DeniedByServerException if the response indicates that the
     * server rejected the request
     */
    @Override
    public byte[] provideDrmKeyResponse(@Nullable byte[] keySetId, @NonNull byte[] response)
            throws NoDrmSchemeException, DeniedByServerException {
        try {
            return mPlayer.provideKeyResponse(keySetId, response);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    /**
     * Restore persisted offline keys into a new session.  keySetId identifies the
     * keys to load, obtained from a prior call to {@link #provideDrmKeyResponse}.
     *
     * @param keySetId identifies the saved key set to restore
     */
    @Override
    public void restoreDrmKeys(@NonNull final byte[] keySetId)
            throws NoDrmSchemeException {
        try {
            mPlayer.restoreKeys(keySetId);
        } catch (MediaPlayer.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }


    /**
     * Read a DRM engine plugin String property value, given the property name string.
     * <p>
     *

     * @param propertyName the property name
     *
     * Standard fields names are:
     * {@link MediaDrm#PROPERTY_VENDOR}, {@link MediaDrm#PROPERTY_VERSION},
     * {@link MediaDrm#PROPERTY_DESCRIPTION}, {@link MediaDrm#PROPERTY_ALGORITHMS}
     */
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


    /**
     * Set a DRM engine plugin String property value.
     * <p>
     *
     * @param propertyName the property name
     * @param value the property value
     *
     * Standard fields names are:
     * {@link MediaDrm#PROPERTY_VENDOR}, {@link MediaDrm#PROPERTY_VERSION},
     * {@link MediaDrm#PROPERTY_DESCRIPTION}, {@link MediaDrm#PROPERTY_ALGORITHMS}
     */
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

    private void setPlaybackParamsInternal(final PlaybackParams params) {
        PlaybackParams current = mPlayer.getPlaybackParams();
        mPlayer.setPlaybackParams(params);
        if (current.getSpeed() != params.getSpeed()) {
            notifyPlayerEvent(new PlayerEventNotifier() {
                @Override
                public void notify(PlayerEventCallback cb) {
                    cb.onPlaybackSpeedChanged(mMediaPlayerInterfaceImpl, params.getSpeed());
                }
            });
        }
    }

    private void notifyMediaPlayer2Event(final Mp2EventNotifier notifier) {
        List<Pair<Executor, MediaPlayer2EventCallback>> records;
        synchronized (mLock) {
            records = new ArrayList<>(mMp2EventCallbackRecords);
        }
        for (final Pair<Executor, MediaPlayer2EventCallback> record : records) {
            record.first.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(record.second);
                }
            });
        }
    }

    private void notifyPlayerEvent(final PlayerEventNotifier notifier) {
        ArrayMap<PlayerEventCallback, Executor> map;
        synchronized (mLock) {
            map = new ArrayMap<>(mPlayerEventCallbackMap);
        }
        final int callbackCount = map.size();
        for (int i = 0; i < callbackCount; i++) {
            final Executor executor = map.valueAt(i);
            final PlayerEventCallback cb = map.keyAt(i);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(cb);
                }
            });
        }
    }

    private void notifyDrmEvent(final DrmEventNotifier notifier) {
        List<Pair<Executor, DrmEventCallback>> records;
        synchronized (mLock) {
            records = new ArrayList<>(mDrmEventCallbackRecords);
        }
        for (final Pair<Executor, DrmEventCallback> record : records) {
            record.first.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.notify(record.second);
                }
            });
        }
    }

    private interface Mp2EventNotifier {
        void notify(MediaPlayer2EventCallback callback);
    }

    private interface PlayerEventNotifier {
        void notify(PlayerEventCallback callback);
    }

    private interface DrmEventNotifier {
        void notify(DrmEventCallback callback);
    }

    private void setUpListeners(final MediaPlayerSource src) {
        MediaPlayer p = src.mPlayer;
        p.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                handleDataSourceError(mPlayer.onPrepared(mp));
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback callback) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        DataSourceDesc dsd = src.getDSD();
                        callback.onInfo(mp2, dsd, MEDIA_INFO_PREPARED, 0);
                    }
                });
                notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(PlayerEventCallback cb) {
                        cb.onMediaPrepared(mMediaPlayerInterfaceImpl, src.getDSD());
                    }
                });
                synchronized (mTaskLock) {
                    if (mCurrentTask != null
                            && mCurrentTask.mMediaCallType == CALL_COMPLETED_PREPARE
                            && mCurrentTask.mDSD == src.getDSD()
                            && mCurrentTask.mNeedToWaitForEventToComplete) {
                        mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                        mCurrentTask = null;
                        processPendingTask_l();
                    }
                }
            }
        });
        p.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, final int width, final int height) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        cb.onVideoSizeChanged(MediaPlayer2Impl.this, src.getDSD(), width, height);
                    }
                });
            }
        });
        p.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(MediaPlayer2EventCallback cb) {
                                cb.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                        MEDIA_INFO_VIDEO_RENDERING_START, 0);
                            }
                        });
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        mPlayer.setBufferingState(
                                mp, MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_STARVED);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        mPlayer.setBufferingState(
                                mp, MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                        break;
                }
                return false;
            }
        });
        p.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                handleDataSourceError(mPlayer.onCompletion(mp));
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        MediaPlayer2Impl mp2 = MediaPlayer2Impl.this;
                        DataSourceDesc dsd = src.getDSD();
                        cb.onInfo(mp2, dsd, MEDIA_INFO_PLAYBACK_COMPLETE, 0);
                    }
                });
            }
        });
        p.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, final int what, final int extra) {
                mPlayer.onError(mp);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        int w = sErrorEventMap.getOrDefault(what, MEDIA_ERROR_UNKNOWN);
                        cb.onError(MediaPlayer2Impl.this, src.getDSD(), w, extra);
                    }
                });
                return true;
            }
        });
        p.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                synchronized (mTaskLock) {
                    if (mCurrentTask != null
                            && mCurrentTask.mMediaCallType == CALL_COMPLETED_SEEK_TO
                            && mCurrentTask.mNeedToWaitForEventToComplete) {
                        mCurrentTask.sendCompleteNotification(CALL_STATUS_NO_ERROR);
                        mCurrentTask = null;
                        processPendingTask_l();
                    }
                }
                final long seekPos = getCurrentPosition();
                notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(PlayerEventCallback cb) {
                        // TODO: The actual seeked position might be different from the
                        // requested position. Clarify which one is expected here.
                        cb.onSeekCompleted(mMediaPlayerInterfaceImpl, seekPos);
                    }
                });
            }
        });
        p.setOnTimedMetaDataAvailableListener(
                new MediaPlayer.OnTimedMetaDataAvailableListener() {
                    @Override
                    public void onTimedMetaDataAvailable(MediaPlayer mp, final TimedMetaData data) {
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(MediaPlayer2EventCallback cb) {
                                cb.onTimedMetaDataAvailable(
                                        MediaPlayer2Impl.this, src.getDSD(), data);
                            }
                        });
                    }
                });
        p.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, final int what, final int extra) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        int w = sInfoEventMap.getOrDefault(what, MEDIA_INFO_UNKNOWN);
                        cb.onInfo(MediaPlayer2Impl.this, src.getDSD(), w, extra);
                    }
                });
                return true;
            }
        });
        p.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, final int percent) {
                if (percent >= 100) {
                    mPlayer.setBufferingState(
                            mp, MediaPlayerInterface.BUFFERING_STATE_BUFFERING_COMPLETE);
                }
                src.mBufferedPercentage.set(percent);
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
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
                            MediaPlayer mp, final MediaTimestamp timestamp) {
                        notifyMediaPlayer2Event(new Mp2EventNotifier() {
                            @Override
                            public void notify(MediaPlayer2EventCallback cb) {
                                cb.onMediaTimeDiscontinuity(
                                        MediaPlayer2Impl.this, src.getDSD(), timestamp);
                            }
                        });
                    }
                });
        p.setOnSubtitleDataListener(new MediaPlayer.OnSubtitleDataListener() {
            @Override
            public  void onSubtitleData(MediaPlayer mp, final SubtitleData data) {
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback cb) {
                        cb.onSubtitleData(MediaPlayer2Impl.this, src.getDSD(), data);
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
        p.setOnDrmPreparedListener(new MediaPlayer.OnDrmPreparedListener() {
            @Override
            public void onDrmPrepared(MediaPlayer mp, final int status) {
                notifyDrmEvent(new DrmEventNotifier() {
                    @Override
                    public void notify(DrmEventCallback cb) {
                        int s = sPrepareDrmStatusMap.getOrDefault(
                                status, PREPARE_DRM_STATUS_PREPARATION_ERROR);
                        cb.onDrmPrepared(MediaPlayer2Impl.this, src.getDSD(), s);
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
         * Returns the PSSH info of the data source for each supported DRM scheme.
         */
        @Override
        public Map<UUID, byte[]> getPssh() {
            return mMapPssh;
        }

        /**
         * Returns the intersection of the data source and the device DRM schemes.
         * It effectively identifies the subset of the source's DRM schemes which
         * are supported by the device too.
         */
        @Override
        public List<UUID> getSupportedSchemes() {
            return Arrays.asList(mSupportedSchemes);
        }

        private DrmInfoImpl(Map<UUID, byte[]> pssh, UUID[] supportedSchemes) {
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

    /**
     * Thrown when a DRM method is called before preparing a DRM scheme through prepareDrm().
     * Extends MediaDrm.MediaDrmException
     */
    public static final class NoDrmSchemeExceptionImpl extends NoDrmSchemeException {
        public NoDrmSchemeExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Thrown when the device requires DRM provisioning but the provisioning attempt has
     * failed due to a network error (Internet reachability, timeout, etc.).
     * Extends MediaDrm.MediaDrmException
     */
    public static final class ProvisioningNetworkErrorExceptionImpl
            extends ProvisioningNetworkErrorException {
        public ProvisioningNetworkErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Thrown when the device requires DRM provisioning but the provisioning attempt has
     * failed due to the provisioning server denying the request.
     * Extends MediaDrm.MediaDrmException
     */
    public static final class ProvisioningServerErrorExceptionImpl
            extends ProvisioningServerErrorException {
        public ProvisioningServerErrorExceptionImpl(String detailMessage) {
            super(detailMessage);
        }
    }

    private abstract class Task implements Runnable {
        private final int mMediaCallType;
        private final boolean mNeedToWaitForEventToComplete;
        private DataSourceDesc mDSD;

        Task(int mediaCallType, boolean needToWaitForEventToComplete) {
            mMediaCallType = mediaCallType;
            mNeedToWaitForEventToComplete = needToWaitForEventToComplete;
        }

        abstract void process() throws IOException, NoDrmSchemeException;

        @Override
        public void run() {
            int status = CALL_STATUS_NO_ERROR;
            try {
                process();
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
            mDSD = getCurrentDataSource();

            if (!mNeedToWaitForEventToComplete || status != CALL_STATUS_NO_ERROR) {

                sendCompleteNotification(status);

                synchronized (mTaskLock) {
                    mCurrentTask = null;
                    processPendingTask_l();
                }
            }
        }

        private void sendCompleteNotification(final int status) {
            // In {@link #notifyWhenCommandLabelReached} case, a separate callback
            // {#link #onCommandLabelReached} is already called in {@code process()}.
            if (mMediaCallType == CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED) {
                return;
            }
            notifyMediaPlayer2Event(new Mp2EventNotifier() {
                @Override
                public void notify(MediaPlayer2EventCallback cb) {
                    cb.onCallCompleted(
                            MediaPlayer2Impl.this, mDSD, mMediaCallType, status);
                }
            });
        }
    };

    private static class DataSourceError {
        final DataSourceDesc mDSD;
        final int mWhat;

        final int mExtra;
        DataSourceError(DataSourceDesc dsd, int what, int extra) {
            mDSD = dsd;
            mWhat = what;
            mExtra = extra;
        }

    }

    private class MediaPlayerSource {

        volatile DataSourceDesc mDSD;
        final MediaPlayer mPlayer = new MediaPlayer();
        final AtomicInteger mBufferedPercentage = new AtomicInteger(0);
        int mSourceState = SOURCE_STATE_INIT;
        @MediaPlayer2State int mMp2State = MEDIAPLAYER2_STATE_IDLE;
        @BuffState int mBufferingState = MediaPlayerInterface.BUFFERING_STATE_UNKNOWN;
        @PlayerState int mPlayerState = MediaPlayerInterface.PLAYER_STATE_IDLE;
        boolean mPlayPending;

        MediaPlayerSource(final DataSourceDesc dsd) {
            mDSD = dsd;
            setUpListeners(this);
        }

        DataSourceDesc getDSD() {
            return mDSD;
        }

    }

    private class MediaPlayerSourceQueue {

        List<MediaPlayerSource> mQueue = new ArrayList<>();
        float mVolume = 1.0f;
        Surface mSurface;

        MediaPlayerSourceQueue() {
            mQueue.add(new MediaPlayerSource(null));
        }

        synchronized MediaPlayer getCurrentPlayer() {
            return mQueue.get(0).mPlayer;
        }

        synchronized MediaPlayerSource getFirst() {
            return mQueue.get(0);
        }

        synchronized void setFirst(DataSourceDesc dsd) throws IOException {
            if (mQueue.isEmpty()) {
                mQueue.add(0, new MediaPlayerSource(dsd));
            } else {
                mQueue.get(0).mDSD = dsd;
                setUpListeners(mQueue.get(0));
            }
            handleDataSource(mQueue.get(0));
        }

        synchronized DataSourceError setNext(DataSourceDesc dsd) {
            MediaPlayerSource src = new MediaPlayerSource(dsd);
            if (mQueue.isEmpty()) {
                mQueue.add(src);
                return prepareAt(0);
            } else {
                mQueue.add(1, src);
                return prepareAt(1);
            }
        }

        synchronized DataSourceError setNextMultiple(List<DataSourceDesc> descs) {
            List<MediaPlayerSource> sources = new ArrayList<>();
            for (DataSourceDesc dsd: descs) {
                sources.add(new MediaPlayerSource(dsd));
            }
            if (mQueue.isEmpty()) {
                mQueue.addAll(sources);
                return prepareAt(0);
            } else {
                mQueue.addAll(1, sources);
                return prepareAt(1);
            }
        }

        synchronized void play() {
            MediaPlayerSource src = mQueue.get(0);
            if (src.mSourceState == SOURCE_STATE_PREPARED) {
                src.mPlayer.start();
                setMp2State(src.mPlayer, MEDIAPLAYER2_STATE_PLAYING);
            }
        }

        synchronized void prepare() {
            getCurrentPlayer().prepareAsync();
        }

        synchronized void release() {
            getCurrentPlayer().release();
        }

        synchronized void prepareAsync() {
            MediaPlayer mp = getCurrentPlayer();
            mp.prepareAsync();
            setBufferingState(mp, MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_STARVED);
        }

        synchronized void pause() {
            MediaPlayer mp = getCurrentPlayer();
            mp.pause();
            setMp2State(mp, MEDIAPLAYER2_STATE_PAUSED);
        }

        synchronized long getCurrentPosition() {
            return getCurrentPlayer().getCurrentPosition();
        }

        synchronized long getDuration() {
            return getCurrentPlayer().getDuration();
        }

        synchronized long getBufferedPosition() {
            MediaPlayerSource src = mQueue.get(0);
            return (long) src.mPlayer.getDuration() * src.mBufferedPercentage.get() / 100;
        }

        synchronized void setAudioAttributes(AudioAttributes attributes) {
            getCurrentPlayer().setAudioAttributes(attributes);
        }

        synchronized DataSourceError onPrepared(MediaPlayer mp) {
            for (int i = 0; i < mQueue.size(); i++) {
                MediaPlayerSource src = mQueue.get(i);
                if (mp == src.mPlayer) {
                    if (i == 0) {
                        if (src.mPlayPending) {
                            src.mPlayPending = false;
                            src.mPlayer.start();
                            setMp2State(src.mPlayer, MEDIAPLAYER2_STATE_PLAYING);
                        } else {
                            setMp2State(src.mPlayer, MEDIAPLAYER2_STATE_PREPARED);
                        }
                    }
                    src.mSourceState = SOURCE_STATE_PREPARED;
                    setBufferingState(src.mPlayer,
                            MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                    return prepareAt(i + 1);
                }
            }
            return null;
        }

        synchronized DataSourceError onCompletion(MediaPlayer mp) {
            if (!mQueue.isEmpty() && mp == getCurrentPlayer()) {
                if (mQueue.size() == 1) {
                    setMp2State(mp, MEDIAPLAYER2_STATE_PAUSED);
                    return null;
                }
                moveToNext();
            }
            return playCurrent();
        }

        synchronized void moveToNext() {
            final MediaPlayerSource src1 = mQueue.remove(0);
            src1.mPlayer.release();
            if (mQueue.isEmpty()) {
                throw new IllegalStateException("player/source queue emptied");
            }
            final MediaPlayerSource src2 = mQueue.get(0);
            if (src1.mPlayerState != src2.mPlayerState) {
                notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(PlayerEventCallback cb) {
                        cb.onPlayerStateChanged(mMediaPlayerInterfaceImpl, src2.mPlayerState);
                    }
                });
            }
            notifyPlayerEvent(new PlayerEventNotifier() {
                @Override
                public void notify(PlayerEventCallback cb) {
                    cb.onCurrentDataSourceChanged(mMediaPlayerInterfaceImpl, src2.mDSD);
                }
            });
        }

        synchronized DataSourceError playCurrent() {
            DataSourceError err = null;
            final MediaPlayerSource src = mQueue.get(0);
            src.mPlayer.setSurface(mSurface);
            src.mPlayer.setVolume(mVolume, mVolume);
            if (src.mSourceState == SOURCE_STATE_PREPARED) {
                // start next source only when it's in prepared state.
                src.mPlayer.start();
                notifyMediaPlayer2Event(new Mp2EventNotifier() {
                    @Override
                    public void notify(MediaPlayer2EventCallback callback) {
                        callback.onInfo(MediaPlayer2Impl.this, src.getDSD(),
                                MEDIA_INFO_STARTED_AS_NEXT, 0);
                    }
                });

            } else {
                if (src.mSourceState == SOURCE_STATE_INIT) {
                    err = prepareAt(0);
                }
                src.mPlayPending = true;
            }
            return err;
        }

        synchronized void onError(MediaPlayer mp) {
            setMp2State(mp, MEDIAPLAYER2_STATE_ERROR);
            setBufferingState(mp, MediaPlayerInterface.BUFFERING_STATE_UNKNOWN);
        }

        synchronized DataSourceError prepareAt(int n) {
            if (n >= mQueue.size()
                    || mQueue.get(n).mSourceState != SOURCE_STATE_INIT
                    || getPlayerState() == MediaPlayerInterface.PLAYER_STATE_IDLE) {
                // There is no next source or it's in preparing or prepared state.
                return null;
            }

            MediaPlayerSource src = mQueue.get(n);
            try {
                src.mSourceState = SOURCE_STATE_PREPARING;
                handleDataSource(src);
                src.mPlayer.prepareAsync();
                return null;
            } catch (Exception e) {
                DataSourceDesc dsd = src.getDSD();
                setMp2State(src.mPlayer, MEDIAPLAYER2_STATE_ERROR);
                return new DataSourceError(dsd, MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_UNSUPPORTED);
            }

        }

        synchronized void skipToNext() {
            if (mQueue.size() <= 1) {
                throw new IllegalStateException("No next source available");
            }
            final MediaPlayerSource src = mQueue.get(0);
            moveToNext();
            if (src.mPlayerState == MediaPlayerInterface.PLAYER_STATE_PLAYING) {
                playCurrent();
            }
        }

        synchronized void setLooping(boolean loop) {
            getCurrentPlayer().setLooping(loop);
        }

        synchronized void setPlaybackParams(PlaybackParams playbackParams) {
            getCurrentPlayer().setPlaybackParams(playbackParams);
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
            return getCurrentPlayer().getVideoWidth();
        }

        synchronized int getVideoHeight() {
            return getCurrentPlayer().getVideoHeight();
        }

        synchronized PersistableBundle getMetrics() {
            return getCurrentPlayer().getMetrics();
        }

        synchronized PlaybackParams getPlaybackParams() {
            return getCurrentPlayer().getPlaybackParams();
        }

        synchronized void setSyncParams(SyncParams params) {
            getCurrentPlayer().setSyncParams(params);
        }

        synchronized SyncParams getSyncParams() {
            return getCurrentPlayer().getSyncParams();
        }

        synchronized void seekTo(long msec, int mode) {
            getCurrentPlayer().seekTo(msec, mode);
        }

        synchronized void reset() {
            MediaPlayerSource src = mQueue.get(0);
            src.mPlayer.reset();
            src.mBufferedPercentage.set(0);
            mVolume = 1.0f;
            setMp2State(src.mPlayer, MEDIAPLAYER2_STATE_IDLE);
            setBufferingState(src.mPlayer, MediaPlayerInterface.BUFFERING_STATE_UNKNOWN);
        }

        synchronized MediaTimestamp getTimestamp() {
            return getCurrentPlayer().getTimestamp();
        }

        synchronized void setAudioSessionId(int sessionId) {
            getCurrentPlayer().setAudioSessionId(sessionId);
        }

        synchronized int getAudioSessionId() {
            return getCurrentPlayer().getAudioSessionId();
        }

        synchronized void attachAuxEffect(int effectId) {
            getCurrentPlayer().attachAuxEffect(effectId);
        }

        synchronized void setAuxEffectSendLevel(float level) {
            getCurrentPlayer().setAuxEffectSendLevel(level);
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
                if (src.mPlayer != mp) {
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
                notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(PlayerEventCallback cb) {
                        cb.onPlayerStateChanged(mMediaPlayerInterfaceImpl, playerState);
                    }
                });
                return;
            }
        }

        synchronized void setBufferingState(MediaPlayer mp, @BuffState final int state) {
            for (final MediaPlayerSource src: mQueue) {
                if (src.mPlayer != mp) {
                    continue;
                }
                if (src.mBufferingState == state) {
                    return;
                }
                src.mBufferingState = state;
                notifyPlayerEvent(new PlayerEventNotifier() {
                    @Override
                    public void notify(PlayerEventCallback cb) {
                        DataSourceDesc dsd = src.getDSD();
                        cb.onBufferingStateChanged(mMediaPlayerInterfaceImpl, dsd, state);
                    }
                });
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
                if (src.mPlayer == mp) {
                    return src;
                }
            }
            return null;
        }
    }

    private class MediaPlayerInterfaceImpl extends MediaPlayerInterface {
        @Override
        public void play() {
            MediaPlayer2Impl.this.play();
        }

        @Override
        public void prepare() {
            MediaPlayer2Impl.this.prepare();
        }

        @Override
        public void pause() {
            MediaPlayer2Impl.this.pause();
        }

        @Override
        public void reset() {
            MediaPlayer2Impl.this.reset();
        }

        @Override
        public void skipToNext() {
            MediaPlayer2Impl.this.skipToNext();
        }

        @Override
        public void seekTo(long pos) {
            MediaPlayer2Impl.this.seekTo(pos);
        }

        @Override
        public long getCurrentPosition() {
            return MediaPlayer2Impl.this.getCurrentPosition();
        }

        @Override
        public long getDuration() {
            return MediaPlayer2Impl.this.getDuration();
        }

        @Override
        public long getBufferedPosition() {
            return MediaPlayer2Impl.this.getBufferedPosition();
        }

        @Override
        public int getPlayerState() {
            return MediaPlayer2Impl.this.getPlayerState();
        }

        @Override
        public int getBufferingState() {
            return MediaPlayer2Impl.this.getBufferingState();
        }

        @Override
        public void setAudioAttributes(AudioAttributesCompat attributes) {
            MediaPlayer2Impl.this.setAudioAttributes(attributes);
        }

        @Override
        public AudioAttributesCompat getAudioAttributes() {
            return MediaPlayer2Impl.this.getAudioAttributes();
        }

        @Override
        public void setDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setDataSource(dsd);
        }

        @Override
        public void setNextDataSource(DataSourceDesc dsd) {
            MediaPlayer2Impl.this.setNextDataSource(dsd);
        }

        @Override
        public void setNextDataSources(List<DataSourceDesc> dsds) {
            MediaPlayer2Impl.this.setNextDataSources(dsds);
        }

        @Override
        public DataSourceDesc getCurrentDataSource() {
            return MediaPlayer2Impl.this.getCurrentDataSource();
        }

        @Override
        public void loopCurrent(boolean loop) {
            MediaPlayer2Impl.this.loopCurrent(loop);
        }

        @Override
        public void setPlaybackSpeed(float speed) {
            MediaPlayer2Impl.this.setPlaybackSpeed(speed);
        }

        @Override
        public float getPlaybackSpeed() {
            return MediaPlayer2Impl.this.getPlaybackSpeed();
        }

        @Override
        public void setPlayerVolume(float volume) {
            MediaPlayer2Impl.this.setPlayerVolume(volume);
        }

        @Override
        public float getPlayerVolume() {
            return MediaPlayer2Impl.this.getPlayerVolume();
        }

        @Override
        public void registerPlayerEventCallback(Executor e, final PlayerEventCallback cb) {
            MediaPlayer2Impl.this.registerPlayerEventCallback(e, cb);
        }

        @Override
        public void unregisterPlayerEventCallback(PlayerEventCallback cb) {
            MediaPlayer2Impl.this.unregisterPlayerEventCallback(cb);
        }

        @Override
        public void close() throws Exception {
            MediaPlayer2Impl.this.close();
        }
    }
}
