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
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.concurrent.listenablefuture.ListenableFuture;
import androidx.concurrent.listenablefuture.SettableFuture;
import androidx.media.AudioAttributesCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @hide
 */
@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(LIBRARY)
public class XMediaPlayer extends SessionPlayer2 {
    private static final String TAG = "XMediaPlayer";

    /**
     * Unspecified media player error.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    /**
     * The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    /**
     * File or network related operation errors.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_IO = -1004;
    /**
     * Bitstream is not conforming to the related coding standard or file spec.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    /**
     * Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    /**
     * Some operation takes too long to complete, usually more than 3-5 seconds.
     * @see PlayerCallback#onError
     */
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    /** Unspecified low-level system error. This value originated from UNKNOWN_ERROR in
     * system/core/include/utils/Errors.h
     * @see PlayerCallback#onError
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_ERROR_SYSTEM = -2147483648;

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "MEDIA_ERROR",*/ value = {
            MEDIA_ERROR_UNKNOWN,
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
            MEDIA_ERROR_IO,
            MEDIA_ERROR_MALFORMED,
            MEDIA_ERROR_UNSUPPORTED,
            MEDIA_ERROR_TIMED_OUT,
            MEDIA_ERROR_SYSTEM
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface MediaError {}

    /**
     * Unspecified media player info.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_UNKNOWN = 1;

    /**
     * The player just started the playback of this data source.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_DATA_SOURCE_START = 2;

    /**
     * The player just pushed the very first video frame for rendering.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;

    /**
     * The player just rendered the very first audio sample.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_AUDIO_RENDERING_START = 4;

    /**
     * The player just completed the playback of this data source.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_DATA_SOURCE_END = 5;

    /**
     * The player just completed the playback of all the data sources set by {@link #setPlaylist}
     * and {@link #setMediaItem}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_DATA_SOURCE_LIST_END = 6;

    /**
     * The player just completed an iteration of playback loop. This event is sent only when
     * looping is enabled by {@link #loopCurrent}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_DATA_SOURCE_REPEAT = 7;

    /**
     * The player just prepared a data source.
     * @see #prepare()
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_PREPARED = 100;

    /**
     * The video is too complex for the decoder: it can't decode frames fast
     * enough. Possibly only the audio plays fine at this stage.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /**
     * The player is temporarily pausing playback internally in order to
     * buffer more data.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /**
     * The player is resuming playback after filling buffers.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /**
     * Estimated network bandwidth information (kbps) is available; currently this event fires
     * simultaneously as {@link #MEDIA_INFO_BUFFERING_START} and {@link #MEDIA_INFO_BUFFERING_END}
     * when playing network files.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;

    /**
     * Update status in buffering a media source received through progressive downloading.
     * The received buffering percentage indicates how much of the content has been buffered
     * or played. For example a buffering update of 80 percent when half the content
     * has already been played indicates that the next 30 percent of the
     * content to play has been buffered.
     *
     * The {@code extra} parameter in {@link PlayerCallback#onInfo} is the
     * percentage (0-100) of the content that has been buffered or played thus far.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_BUFFERING_UPDATE = 704;

    /**
     * Bad interleaving means that a media has been improperly interleaved or
     * not interleaved at all, e.g has all the video samples first then all the
     * audio ones. Video is playing but a lot of disk seeks may be happening.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

    /**
     * The media cannot be seeked (e.g live stream)
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

    /**
     * A new set of metadata is available.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /**
     * A new set of external-only metadata is available.  Used by
     * JAVA framework to avoid triggering track scanning.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_EXTERNAL_METADATA_UPDATE = 803;

    /**
     * Informs that audio is not playing. Note that playback of the video
     * is not interrupted.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_AUDIO_NOT_PLAYING = 804;

    /**
     * Informs that video is not playing. Note that playback of the audio
     * is not interrupted.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_VIDEO_NOT_PLAYING = 805;

    /** Failed to handle timed text track properly.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_TIMED_TEXT_ERROR = 900;

    /**
     * Subtitle track was not supported by the media framework.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;

    /**
     * Reading the subtitle track takes too long.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "MEDIA_INFO",*/ value = {
            MEDIA_INFO_UNKNOWN,
            MEDIA_INFO_DATA_SOURCE_START,
            MEDIA_INFO_VIDEO_RENDERING_START,
            MEDIA_INFO_AUDIO_RENDERING_START,
            MEDIA_INFO_DATA_SOURCE_END,
            MEDIA_INFO_DATA_SOURCE_LIST_END,
            MEDIA_INFO_DATA_SOURCE_REPEAT,
            MEDIA_INFO_PREPARED,
            MEDIA_INFO_VIDEO_TRACK_LAGGING,
            MEDIA_INFO_BUFFERING_START,
            MEDIA_INFO_BUFFERING_END,
            MEDIA_INFO_NETWORK_BANDWIDTH,
            MEDIA_INFO_BUFFERING_UPDATE,
            MEDIA_INFO_BAD_INTERLEAVING,
            MEDIA_INFO_NOT_SEEKABLE,
            MEDIA_INFO_METADATA_UPDATE,
            MEDIA_INFO_EXTERNAL_METADATA_UPDATE,
            MEDIA_INFO_AUDIO_NOT_PLAYING,
            MEDIA_INFO_VIDEO_NOT_PLAYING,
            MEDIA_INFO_TIMED_TEXT_ERROR,
            MEDIA_INFO_UNSUPPORTED_SUBTITLE,
            MEDIA_INFO_SUBTITLE_TIMED_OUT
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface MediaInfo {}

    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a sync (or key) frame associated with a data source that is located
     * right before or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_PREVIOUS_SYNC    = 0x00;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a sync (or key) frame associated with a data source that is located
     * right after or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_NEXT_SYNC        = 0x01;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a sync (or key) frame associated with a data source that is located
     * closest to (in time) or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_CLOSEST_SYNC     = 0x02;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a frame (not necessarily a key frame) associated with a data source that
     * is located closest to or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_CLOSEST          = 0x03;

    /** @hide */
    @IntDef(flag = false, /*prefix = "SEEK",*/ value = {
            SEEK_PREVIOUS_SYNC,
            SEEK_NEXT_SYNC,
            SEEK_CLOSEST_SYNC,
            SEEK_CLOSEST,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface SeekMode {}

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sResultCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sErrorCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sInfoCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sSeekModeMap;

    static {
        sResultCodeMap = new ArrayMap<>();
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_NO_ERROR, RESULT_CODE_NO_ERROR);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN, RESULT_CODE_ERROR_UNKNOWN);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_INVALID_OPERATION, RESULT_CODE_INVALID_OPERATION);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_BAD_VALUE, RESULT_CODE_BAD_VALUE);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_PERMISSION_DENIED, RESULT_CODE_PERMISSION_DENIED);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_IO, RESULT_CODE_ERROR_IO);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_SKIPPED, RESULT_CODE_SKIPPED);

        sErrorCodeMap = new ArrayMap<>();
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_UNKNOWN);
        sErrorCodeMap.put(
                MediaPlayer2.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
                MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_IO, MEDIA_ERROR_IO);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_MALFORMED, MEDIA_ERROR_MALFORMED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED, MEDIA_ERROR_UNSUPPORTED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_TIMED_OUT, MEDIA_ERROR_TIMED_OUT);

        sInfoCodeMap = new ArrayMap<>();
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_UNKNOWN, MEDIA_INFO_UNKNOWN);
        sInfoCodeMap.put(
                MediaPlayer2.MEDIA_INFO_VIDEO_RENDERING_START, MEDIA_INFO_VIDEO_RENDERING_START);
        sInfoCodeMap.put(
                MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING, MEDIA_INFO_VIDEO_TRACK_LAGGING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_BUFFERING_START, MEDIA_INFO_BUFFERING_START);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_BUFFERING_END, MEDIA_INFO_BUFFERING_END);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING, MEDIA_INFO_BAD_INTERLEAVING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE, MEDIA_INFO_NOT_SEEKABLE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE, MEDIA_INFO_METADATA_UPDATE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING, MEDIA_INFO_AUDIO_NOT_PLAYING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING, MEDIA_INFO_VIDEO_NOT_PLAYING);
        sInfoCodeMap.put(
                MediaPlayer2.MEDIA_INFO_UNSUPPORTED_SUBTITLE, MEDIA_INFO_UNSUPPORTED_SUBTITLE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_SUBTITLE_TIMED_OUT, MEDIA_INFO_SUBTITLE_TIMED_OUT);

        sSeekModeMap = new ArrayMap<>();
        sSeekModeMap.put(SEEK_PREVIOUS_SYNC, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        sSeekModeMap.put(SEEK_NEXT_SYNC, MediaPlayer2.SEEK_NEXT_SYNC);
        sSeekModeMap.put(SEEK_CLOSEST_SYNC, MediaPlayer2.SEEK_CLOSEST_SYNC);
        sSeekModeMap.put(SEEK_CLOSEST, MediaPlayer2.SEEK_CLOSEST);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaPlayer2 mPlayer;
    private ExecutorService mExecutor;

    /* A list for tracking the commands submitted to MediaPlayer2.*/
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayDeque<Pair<Integer, SettableFuture<CommandResult2>>>
            mCallTypeAndFutures = new ArrayDeque<>();

    private final Object mStateLock = new Object();
    @GuardedBy("mStateLock")
    private @PlayerState int mState;
    @GuardedBy("mStateLock")
    private Map<DataSourceDesc2, Integer> mDSDToBuffStateMap = new HashMap<>();

    public XMediaPlayer(Context context) {
        mState = PLAYER_STATE_IDLE;
        mPlayer = MediaPlayer2.create(context);
        mExecutor = Executors.newFixedThreadPool(1);
        mPlayer.setEventCallback(mExecutor, new Mp2Callback());
    }

    @Override
    public ListenableFuture<CommandResult2> play() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PLAY, future));
            mPlayer.play();
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> pause() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PAUSE, future));
            mPlayer.pause();
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> prepare() {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_PREPARE, future));
            mPlayer.prepare();
        }
        // TODO: Changing buffering state is not correct. Think about changing MP2 event APIs for
        // the initial buffering for prepare case.
        setBufferingState(mPlayer.getCurrentDataSource(), BUFFERING_STATE_BUFFERING_AND_STARVED);
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> seekTo(long position) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO, future));
            mPlayer.seekTo(position);
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaybackSpeed(float playbackSpeed) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(
                    MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS, future));
            mPlayer.setPlaybackParams(new PlaybackParams2.Builder(
                    mPlayer.getPlaybackParams().getPlaybackParams())
                    .setSpeed(playbackSpeed).build());
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> setAudioAttributes(AudioAttributesCompat attr) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(
                    MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES, future));
            mPlayer.setAudioAttributes(attr);
        }
        return future;
    }

    @Override
    public int getPlayerState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    @Override
    public long getCurrentPosition() {
        try {
            return mPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return UNKNOWN_TIME;
        }
    }

    @Override
    public long getDuration() {
        try {
            return mPlayer.getDuration();
        } catch (IllegalStateException e) {
            return UNKNOWN_TIME;
        }
    }

    @Override
    public long getBufferedPosition() {
        try {
            return mPlayer.getBufferedPosition();
        } catch (IllegalStateException e) {
            return UNKNOWN_TIME;
        }
    }

    @Override
    public int getBufferingState() {
        Integer buffState;
        synchronized (mStateLock) {
            buffState = mDSDToBuffStateMap.get(mPlayer.getCurrentDataSource());
        }
        return buffState == null ? BUFFERING_STATE_UNKNOWN : buffState;
    }

    @Override
    public float getPlaybackSpeed() {
        try {
            return mPlayer.getPlaybackParams().getSpeed();
        } catch (IllegalStateException e) {
            return 1.0f;
        }
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        try {
            return mPlayer.getAudioAttributes();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaylist(
            List<DataSourceDesc2> list, MediaMetadata2 metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> setMediaItem(DataSourceDesc2 item) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE, future));
            mPlayer.setDataSource(item);
        }
        return future;
    }

    @Override
    public ListenableFuture<CommandResult2> addPlaylistItem(int index, DataSourceDesc2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> removePlaylistItem(DataSourceDesc2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> replacePlaylistItem(int index, DataSourceDesc2 item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPreviousItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToNextItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPlaylistItem(DataSourceDesc2 desc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> updatePlaylistMetadata(MediaMetadata2 metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> setRepeatMode(int repeatMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListenableFuture<CommandResult2> setShuffleMode(int shuffleMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataSourceDesc2> getPlaylist() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRepeatMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getShuffleMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataSourceDesc2 getCurrentMediaItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws Exception {
        mPlayer.close();
        mExecutor.shutdown();
    }

    /**
     * Resets {@link XMediaPlayer} to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling {@link #prepare()}.
     */
    public void reset() {
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.clear();
            mPlayer.reset();
        }
        synchronized (mStateLock) {
            mState = PLAYER_STATE_IDLE;
            mDSDToBuffStateMap.clear();
        }
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of
     * the media.  Setting a
     * Surface will un-set any Surface or SurfaceHolder that was previously set.
     * A null surface will result in only the audio track being played.
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
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> setSurface(Surface surface) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_SURFACE, future));
            mPlayer.setSurface(surface);
        }
        return future;
    }

    /**
     * Sets the volume of the audio of the media to play, expressed as a linear multiplier
     * on the audio samples.
     * Note that this volume is specific to the player, and is separate from stream volume
     * used across the platform.<br>
     * A value of 0.0f indicates muting, a value of 1.0f is the nominal unattenuated and unamplified
     * gain. See {@link #getMaxPlayerVolume()} for the volume range supported by this player.
     *
     * @param volume a value between 0.0f and {@link #getMaxPlayerVolume()}.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> setPlayerVolume(float volume) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_PLAYER_VOLUME, future));
            mPlayer.setPlayerVolume(volume);
        }
        return future;
    }

    /**
     * @return the current volume of this player to this player. Note that it does not take into
     * account the associated stream volume.
     */
    public float getPlayerVolume() {
        return mPlayer.getPlayerVolume();
    }

    /**
     * @return the maximum volume that can be used in {@link #setPlayerVolume(float)}.
     */
    public float getMaxPlayerVolume() {
        return mPlayer.getMaxPlayerVolume();
    }

    /**
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The {@link PlayerCallback} can be registered via {@link #registerPlayerCallback} to
     * receive a notification {@link PlayerCallback#onVideoSizeChanged} when the width
     * is available.
     */
    public int getVideoWidth() {
        return mPlayer.getVideoWidth();
    }

    /**
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The {@link PlayerCallback} can be registered via {@link #registerPlayerCallback} to
     * receive a notification {@link PlayerCallback#onVideoSizeChanged} when the height is
     * available.
     */
    public int getVideoHeight() {
        return mPlayer.getVideoHeight();
    }

    /**
     * @return a {@link PersistableBundle} containing the set of attributes and values
     * available for the media being handled by this player instance.
     * The attributes are descibed in {@link MetricsConstants}.
     *
     * Additional vendor-specific fields may also be present in the return value.
     */
    public PersistableBundle getMetrics() {
        return mPlayer.getMetrics();
    }

    /**
     * Sets playback rate using {@link PlaybackParams2}. The player sets its internal
     * PlaybackParams2 to the given input. This does not change the player state. For example,
     * if this is called with the speed of 2.0f in {@link #PLAYER_STATE_PAUSED}, the player will
     * just update internal property and stay paused. Once the client calls {@link #play()}
     * afterwards, the player will start playback with the given speed. Calling this with zero
     * speed is not allowed.
     *
     * @param params the playback params.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> setPlaybackParams(@NonNull PlaybackParams2 params) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS, future));
            mPlayer.setPlaybackParams(params);
        }
        return future;
    }

    /**
     * Gets the playback params, containing the current playback rate.
     *
     * @return the playback params.
     */
    public PlaybackParams2 getPlaybackParams() {
        return mPlayer.getPlaybackParams();
    }

    /**
     * Moves the media to specified time position by considering the given mode.
     * <p>
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
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> seekTo(long msec, @SeekMode int mode) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        int mp2SeekMode = sSeekModeMap.getOrDefault(mode, SEEK_NEXT_SYNC);
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(new Pair<>(MediaPlayer2.CALL_COMPLETED_SEEK_TO, future));
            mPlayer.seekTo(msec, mp2SeekMode);
        }
        return future;
    }

    /**
     * Gets current playback position as a {@link MediaTimestamp2}.
     * <p>
     * The MediaTimestamp2 represents how the media time correlates to the system time in
     * a linear fashion using an anchor and a clock rate. During regular playback, the media
     * time moves fairly constantly (though the anchor frame may be rebased to a current
     * system time, the linear correlation stays steady). Therefore, this method does not
     * need to be called often.
     * <p>
     * To help users get current playback position, this method always anchors the timestamp
     * to the current {@link System#nanoTime system time}, so
     * {@link MediaTimestamp2#getAnchorMediaTimeUs} can be used as current playback position.
     *
     * @return a MediaTimestamp2 object if a timestamp is available, or {@code null} if no timestamp
     *         is available, e.g. because the media player has not been initialized.
     *
     * @see MediaTimestamp2
     */
    @Nullable
    public MediaTimestamp2 getTimestamp() {
        return mPlayer.getTimestamp();
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
     * <p>This method must be called before {@link #setMediaItem} and {@link #setPlaylist} methods.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> setAudioSessionId(int sessionId) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_AUDIO_SESSION_ID, future));
            mPlayer.setAudioSessionId(sessionId);
        }
        return future;
    }

    /**
     * Returns the audio session ID.
     *
     * @return the audio session ID. {@see #setAudioSessionId(int)}
     * Note that the audio session ID is 0 only if a problem occured when the MediaPlayer2 was
     * contructed.
     */
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
     * <p>This method must be called before {@link #setMediaItem} and {@link #setPlaylist} methods.
     * @param effectId system wide unique id of the effect to attach
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> attachAuxEffect(int effectId) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_ATTACH_AUX_EFFECT, future));
            mPlayer.attachAuxEffect(effectId);
        }
        return future;
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
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> setAuxEffectSendLevel(float level) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL, future));
            mPlayer.setAuxEffectSendLevel(level);
        }
        return future;
    }

    /**
     * Returns a List of track information.
     *
     * @return List of track info. The total number of tracks is the size of the list.
     */
    public List<TrackInfo> getTrackInfo() {
        List<MediaPlayer2.TrackInfo> list = mPlayer.getTrackInfo();
        List<TrackInfo> trackList = new ArrayList<>();
        for (MediaPlayer2.TrackInfo info : list) {
            trackList.add(new TrackInfo(info.getTrackType(), info.getFormat()));
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
    public int getSelectedTrack(int trackType) {
        return mPlayer.getSelectedTrack(trackType);
    }

    /**
     * Selects a track.
     * <p>
     * If the player is in invalid state, {@link #RESULT_CODE_INVALID_OPERATION} will be
     * reported with {@link CommandResult2}.
     * If a player is in <em>Playing</em> state, the selected track is presented immediately.
     * If a player is not in Playing state, it just marks the track to be played.
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
     * </p>
     * @param index the index of the track to be selected. The valid range of the index
     * is 0..total number of track - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     *
     * @see #getTrackInfo
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> selectTrack(int index) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_SELECT_TRACK, future));
            mPlayer.selectTrack(index);
        }
        return future;
    }

    /**
     * Deselects a track.
     * <p>
     * Currently, the track must be a timed text track and no audio or video tracks can be
     * deselected.
     * </p>
     * @param index the index of the track to be deselected. The valid range of the index
     * is 0..total number of tracks - 1. The total number of tracks as well as the type of
     * each individual track can be found by calling {@link #getTrackInfo()} method.
     *
     * @see #getTrackInfo
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link CommandResult2} will be delivered when the command completes.
     */
    public ListenableFuture<CommandResult2> deselectTrack(int index) {
        SettableFuture<CommandResult2> future = SettableFuture.create();
        synchronized (mCallTypeAndFutures) {
            mCallTypeAndFutures.add(
                    new Pair<>(MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK, future));
            mPlayer.deselectTrack(index);
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setState(@PlayerState final int state) {
        boolean needToNotify = false;
        synchronized (mStateLock) {
            if (mState != state) {
                mState = state;
                needToNotify = true;
            }
        }
        if (needToNotify) {
            notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                @Override
                public void callCallback(SessionPlayer2.PlayerCallback callback) {
                    callback.onPlayerStateChanged(XMediaPlayer.this, state);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setBufferingState(final DataSourceDesc2 dsd, @BuffState final int state) {
        Integer previousState;
        synchronized (mStateLock) {
            previousState = mDSDToBuffStateMap.put(dsd, state);
        }
        if (previousState == null || previousState.intValue() != state) {
            notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                @Override
                public void callCallback(SessionPlayer2.PlayerCallback callback) {
                    callback.onBufferingStateChanged(XMediaPlayer.this, dsd, state);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifySessionPlayerCallback(final SessionPlayerCallbackNotifier notifier) {
        Map<SessionPlayer2.PlayerCallback, Executor> map = getCallbacks();
        for (final SessionPlayer2.PlayerCallback callback : map.keySet()) {
            map.get(callback).execute(new Runnable() {
                @Override
                public void run() {
                    notifier.callCallback(callback);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyXMediaPlayerCallback(final XMediaPlayerCallbackNotifier notifier) {
        Map<SessionPlayer2.PlayerCallback, Executor> map = getCallbacks();
        for (final SessionPlayer2.PlayerCallback callback : map.keySet()) {
            if (callback instanceof PlayerCallback) {
                map.get(callback).execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.callCallback((PlayerCallback) callback);
                    }
                });
            }
        }
    }

    private interface SessionPlayerCallbackNotifier {
        void callCallback(SessionPlayer2.PlayerCallback callback);
    }

    private interface XMediaPlayerCallbackNotifier {
        void callCallback(PlayerCallback callback);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    class Mp2Callback extends MediaPlayer2.EventCallback {
        @Override
        public void onVideoSizeChanged(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final int width, final int height) {
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onVideoSizeChanged(XMediaPlayer.this, dsd, width, height);
                }
            });
        }

        @Override
        public void onTimedMetaDataAvailable(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final TimedMetaData2 data) {
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onTimedMetaDataAvailable(XMediaPlayer.this, dsd, data);
                }
            });
        }

        @Override
        public void onError(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final int what, final int extra) {
            setState(PLAYER_STATE_ERROR);
            setBufferingState(dsd, BUFFERING_STATE_UNKNOWN);
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onError(XMediaPlayer.this, dsd, what, extra);
                }
            });
        }

        @Override
        public void onInfo(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final int mp2What, final int extra) {
            switch (mp2What) {
                case MediaPlayer2.MEDIA_INFO_BUFFERING_START:
                    setBufferingState(dsd, BUFFERING_STATE_BUFFERING_AND_STARVED);
                    break;
                case MediaPlayer2.MEDIA_INFO_PREPARED:
                case MediaPlayer2.MEDIA_INFO_BUFFERING_END:
                    setBufferingState(dsd, BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                    break;
                case MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE:
                    if (extra /* percent */ >= 100) {
                        setBufferingState(dsd, BUFFERING_STATE_BUFFERING_COMPLETE);
                    }
                    break;
                case MediaPlayer2.MEDIA_INFO_DATA_SOURCE_LIST_END:
                    setState(PLAYER_STATE_PAUSED);
                    break;
            }
            final int what = sInfoCodeMap.getOrDefault(mp2What, MEDIA_INFO_UNKNOWN);
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onInfo(XMediaPlayer.this, dsd, what, extra);
                }
            });
        }

        @Override
        public void onCallCompleted(
                MediaPlayer2 mp, DataSourceDesc2 dsd, int what, int status) {
            Pair<Integer, SettableFuture<CommandResult2>> pair;
            synchronized (mCallTypeAndFutures) {
                pair = mCallTypeAndFutures.pollFirst();
            }
            if (what != pair.first) {
                Log.w(TAG, "Call type does not match. expeced:" + pair.first + " actual:" + what);
                status = MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN;
            }
            if (status == MediaPlayer2.CALL_STATUS_NO_ERROR) {
                switch (what) {
                    case MediaPlayer2.CALL_COMPLETED_PREPARE:
                    case MediaPlayer2.CALL_COMPLETED_PAUSE:
                        setState(PLAYER_STATE_PAUSED);
                        break;
                    case MediaPlayer2.CALL_COMPLETED_PLAY:
                        setState(PLAYER_STATE_PLAYING);
                        break;
                    case MediaPlayer2.CALL_COMPLETED_SEEK_TO:
                        final long pos = mPlayer.getCurrentPosition();
                        notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                            @Override
                            public void callCallback(
                                    SessionPlayer2.PlayerCallback callback) {
                                callback.onSeekCompleted(XMediaPlayer.this, pos);
                            }
                        });
                        break;
                    case MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS:
                        // TODO: Need to check if the speed value is really changed.
                        final float speed = mPlayer.getPlaybackParams().getSpeed();
                        notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                            @Override
                            public void callCallback(
                                    SessionPlayer2.PlayerCallback callback) {
                                callback.onPlaybackSpeedChanged(XMediaPlayer.this, speed);
                            }
                        });
                        break;
                }
            }
            // TODO: more handling on listenable future. e.g. Canceling.
            Integer resultCode = sResultCodeMap.get(status);
            pair.second.set(new CommandResult2(
                    resultCode == null ? RESULT_CODE_ERROR_UNKNOWN : resultCode, dsd));
        }

        @Override
        public void onMediaTimeDiscontinuity(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final MediaTimestamp2 timestamp) {
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onMediaTimeDiscontinuity(XMediaPlayer.this, dsd, timestamp);
                }
            });
        }

        @Override
        public void onCommandLabelReached(MediaPlayer2 mp, Object label) {
            // Ignore. XMediaPlayer does not use MediaPlayer2.notifyWhenCommandLabelReached().
        }

        @Override
        public void onSubtitleData(
                MediaPlayer2 mp, final DataSourceDesc2 dsd, final SubtitleData2 data) {
            notifyXMediaPlayerCallback(new XMediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onSubtitleData(XMediaPlayer.this, dsd, data);
                }
            });
        }
    }

    /**
     * Interface definition for callbacks to be invoked when the player has the corresponding
     * events.
     */
    public abstract static class PlayerCallback extends SessionPlayer2.PlayerCallback {
        /**
         * Called to indicate the video size
         *
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param mp the player associated with this callback
         * @param dsd the DataSourceDesc2 of this data source
         * @param width the width of the video
         * @param height the height of the video
         */
        public void onVideoSizeChanged(
                XMediaPlayer mp, DataSourceDesc2 dsd, int width, int height) { }

        /**
         * Called to indicate available timed metadata
         * <p>
         * This method will be called as timed metadata is extracted from the media,
         * in the same order as it occurs in the media. The timing of this event is
         * not controlled by the associated timestamp.
         * <p>
         * Currently only HTTP live streaming data URI's embedded with timed ID3 tags generates
         * {@link TimedMetaData2}.
         *
         * @see TimedMetaData2
         *
         * @param mp the player associated with this callback
         * @param dsd the DataSourceDesc2 of this data source
         * @param data the timed metadata sample associated with this event
         */
        public void onTimedMetaDataAvailable(
                XMediaPlayer mp, DataSourceDesc2 dsd, TimedMetaData2 data) { }

        /**
         * Called to indicate an error.
         *
         * @param mp the MediaPlayer2 the error pertains to
         * @param dsd the DataSourceDesc2 of this data source
         * @param what the type of error that has occurred.
         * @param extra an extra code, specific to the error. Typically
         * implementation dependent.
         */
        public void onError(
                XMediaPlayer mp, DataSourceDesc2 dsd, @MediaError int what, int extra) { }

        /**
         * Called to indicate an info or a warning.
         *
         * @param mp the player the info pertains to.
         * @param dsd the DataSourceDesc2 of this data source
         * @param what the type of info or warning.
         * @param extra an extra code, specific to the info. Typically
         * implementation dependent.
         */
        public void onInfo(XMediaPlayer mp, DataSourceDesc2 dsd, @MediaInfo int what, int extra) { }

        /**
         * Called when a discontinuity in the normal progression of the media time is detected.
         * The "normal progression" of media time is defined as the expected increase of the
         * playback position when playing media, relative to the playback speed (for instance every
         * second, media time increases by two seconds when playing at 2x).<br>
         * Discontinuities are encountered in the following cases:
         * <ul>
         * <li>when the player is starved for data and cannot play anymore</li>
         * <li>when the player encounters a playback error</li>
         * <li>when the a seek operation starts, and when it's completed</li>
         * <li>when the playback speed changes</li>
         * <li>when the playback state changes</li>
         * <li>when the player is reset</li>
         * </ul>
         *
         * @param mp the player the media time pertains to.
         * @param dsd the DataSourceDesc2 of this data source
         * @param timestamp the timestamp that correlates media time, system time and clock rate,
         *     or {@link MediaTimestamp2#TIMESTAMP_UNKNOWN} in an error case.
         */
        public void onMediaTimeDiscontinuity(
                XMediaPlayer mp, DataSourceDesc2 dsd, MediaTimestamp2 timestamp) { }

        /**
         * Called when when a player subtitle track has new subtitle data available.
         * @param mp the player that reports the new subtitle data
         * @param dsd the DataSourceDesc2 of this data source
         * @param data the subtitle data
         */
        public void onSubtitleData(
                XMediaPlayer mp, DataSourceDesc2 dsd, @NonNull SubtitleData2 data) { }
    }

    /**
     * Class for the player to return each audio/video/subtitle track's metadata.
     *
     * @see #getTrackInfo
     */
    public static final class TrackInfo {
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;

        private final int mTrackType;
        private final MediaFormat mFormat;

        /**
         * Gets the track type.
         * @return TrackType which indicates if the track is video, audio, timed text.
         */
        public int getTrackType() {
            return mTrackType;
        }

        /**
         * Gets the language code of the track.
         * @return a language code in either way of ISO-639-1 or ISO-639-2.
         * When the language is unknown or could not be determined,
         * ISO-639-2 language code, "und", is returned.
         */
        public String getLanguage() {
            String language = mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        /**
         * Gets the {@link MediaFormat} of the track.  If the format is
         * unknown or could not be determined, null is returned.
         */
        public MediaFormat getFormat() {
            if (mTrackType == MEDIA_TRACK_TYPE_TIMEDTEXT
                    || mTrackType == MEDIA_TRACK_TYPE_SUBTITLE) {
                return mFormat;
            }
            return null;
        }

        TrackInfo(int type, MediaFormat format) {
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
    }

    /**
     * Definitions for the metrics that are reported via the {@link #getMetrics} call.
     */
    public static final class MetricsConstants {
        private MetricsConstants() {}

        /**
         * Key to extract the MIME type of the video track
         * from the {@link #getMetrics} return value.
         * The value is a String.
         */
        public static final String MIME_TYPE_VIDEO = "android.media.mediaplayer.video.mime";

        /**
         * Key to extract the codec being used to decode the video track
         * from the {@link #getMetrics} return value.
         * The value is a String.
         */
        public static final String CODEC_VIDEO = "android.media.mediaplayer.video.codec";

        /**
         * Key to extract the width (in pixels) of the video track
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String WIDTH = "android.media.mediaplayer.width";

        /**
         * Key to extract the height (in pixels) of the video track
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String HEIGHT = "android.media.mediaplayer.height";

        /**
         * Key to extract the count of video frames played
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String FRAMES = "android.media.mediaplayer.frames";

        /**
         * Key to extract the count of video frames dropped
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String FRAMES_DROPPED = "android.media.mediaplayer.dropped";

        /**
         * Key to extract the MIME type of the audio track
         * from the {@link #getMetrics} return value.
         * The value is a String.
         */
        public static final String MIME_TYPE_AUDIO = "android.media.mediaplayer.audio.mime";

        /**
         * Key to extract the codec being used to decode the audio track
         * from the {@link #getMetrics} return value.
         * The value is a String.
         */
        public static final String CODEC_AUDIO = "android.media.mediaplayer.audio.codec";

        /**
         * Key to extract the duration (in milliseconds) of the
         * media being played
         * from the {@link #getMetrics} return value.
         * The value is a long.
         */
        public static final String DURATION = "android.media.mediaplayer.durationMs";

        /**
         * Key to extract the playing time (in milliseconds) of the
         * media being played
         * from the {@link #getMetrics} return value.
         * The value is a long.
         */
        public static final String PLAYING = "android.media.mediaplayer.playingMs";

        /**
         * Key to extract the count of errors encountered while
         * playing the media
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String ERRORS = "android.media.mediaplayer.err";

        /**
         * Key to extract an (optional) error code detected while
         * playing the media
         * from the {@link #getMetrics} return value.
         * The value is an integer.
         */
        public static final String ERROR_CODE = "android.media.mediaplayer.errcode";

    }
}
