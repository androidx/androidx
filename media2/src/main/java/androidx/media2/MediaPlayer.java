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
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_IO_ERROR;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_PERMISSION_DENIED;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_SKIPPED;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_SUCCESS;
import static androidx.media2.SessionPlayer.PlayerResult.RESULT_CODE_UNKNOWN_ERROR;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.MediaFormat;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.AbstractResolvableFuture;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A media player which plays {@link MediaItem}s. The details on playback control and player states
 * can be found in the documentation of the base class, {@link SessionPlayer}.
 * <p>
 * Topic covered here:
 * <ol>
 * <li><a href="#AudioFocusAndNoisyIntent">Audio focus and noisy intent</a>
 * </ol>
 * <a name="AudioFocusAndNoisyIntent"></a>
 * <h3>Audio focus and noisy intent</h3>
 * <p>
 * By default, {@link MediaPlayer} handles audio focus and noisy intent with
 * {@link AudioAttributesCompat} set to this player. You need to call
 * {@link #setAudioAttributes(AudioAttributesCompat)} set the audio attribute while in the
 * {@link #PLAYER_STATE_IDLE}.
 * <p>
 * Here's the table of automatic audio focus behavior with audio attributes.
 * <table>
 * <tr><th>Audio Attributes</th><th>Audio Focus Gain Type</th><th>Misc</th></tr>
 * <tr><td>{@link AudioAttributesCompat#USAGE_VOICE_COMMUNICATION_SIGNALLING}</td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_NONE}</td>
 *     <td /></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_GAME}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_MEDIA}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_UNKNOWN}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN}</td>
 *     <td>Developers should specific a proper usage instead of
 *         {@link AudioAttributesCompat#USAGE_UNKNOWN}</td></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ALARM}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_VOICE_COMMUNICATION}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT}</td>
 *     <td /></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ASSISTANCE_NAVIGATION_GUIDANCE}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_ASSISTANCE_SONIFICATION}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_DELAYED}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_INSTANT}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_REQUEST}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_EVENT}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_RINGTONE}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}</td>
 *     <td /></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ASSISTANT}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}</td>
 *     <td /></tr>
 * <tr><td>{@link AudioAttributesCompat#USAGE_ASSISTANCE_ACCESSIBILITY}</td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT} if
 *         {@link AudioAttributesCompat#CONTENT_TYPE_SPEECH},
 *         {@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK} otherwise</td>
 *     <td /></tr>
 * <tr><td>{@code null}</td>
 *     <td>No audio focus handling, and sets the player volume to {@code 0}</td>
 *     <td>Only valid if your media contents don't have audio</td></tr>
 * <tr><td>Any other AudioAttributes</td>
 *     <td>No audio focus handling, and sets the player volume to {@code 0}</td>
 *     <td>This is to handle error</td></tr>
 * </table>
 * <p>
 * For more information about the audio focus, take a look at
 * <a href="{@docRoot}guide/topics/media-apps/audio-focus.html">Managing audio focus</a>
 * <p>
 */
@TargetApi(Build.VERSION_CODES.P)
public class MediaPlayer extends SessionPlayer {
    private static final String TAG = "MediaPlayer";

    /**
     * Unspecified player error.
     * @see PlayerCallback#onError
     */
    public static final int PLAYER_ERROR_UNKNOWN = 1;
    /**
     * File or network related operation errors.
     * @see PlayerCallback#onError
     */
    public static final int PLAYER_ERROR_IO = -1004;
    /**
     * Bitstream is not conforming to the related coding standard or file spec.
     * @see PlayerCallback#onError
     */
    public static final int PLAYER_ERROR_MALFORMED = -1007;
    /**
     * Bitstream is conforming to the related coding standard or file spec, but
     * the media framework does not support the feature.
     * @see PlayerCallback#onError
     */
    public static final int PLAYER_ERROR_UNSUPPORTED = -1010;
    /**
     * Some operation takes too long to complete, usually more than 3-5 seconds.
     * @see PlayerCallback#onError
     */
    public static final int PLAYER_ERROR_TIMED_OUT = -110;
    /** Unspecified low-level system error. This value originated from UNKNOWN_ERROR in
     * system/core/include/utils/Errors.h
     * @see PlayerCallback#onError
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int PLAYER_ERROR_SYSTEM = -2147483648;

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "PLAYER_ERROR",*/ value = {
            PLAYER_ERROR_UNKNOWN,
            PLAYER_ERROR_IO,
            PLAYER_ERROR_MALFORMED,
            PLAYER_ERROR_UNSUPPORTED,
            PLAYER_ERROR_TIMED_OUT,
            PLAYER_ERROR_SYSTEM
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
     * The player just started the playback of this media item.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_MEDIA_ITEM_START = 2;

    /**
     * The player just pushed the very first video frame for rendering.
     * @see PlayerCallback#onInfo
     */
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;

    /**
     * The player just completed the playback of this media item.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_MEDIA_ITEM_END = 5;

    /**
     * The player just completed the playback of all the media items set by {@link #setPlaylist}
     * and {@link #setMediaItem}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_MEDIA_ITEM_LIST_END = 6;

    /**
     * The player just completed an iteration of playback loop. This event is sent only when
     * looping is enabled by {@link #setRepeatMode(int)}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int MEDIA_INFO_MEDIA_ITEM_REPEAT = 7;

    /**
     * The player just finished preparing a media item for playback.
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
            MEDIA_INFO_MEDIA_ITEM_START,
            MEDIA_INFO_VIDEO_RENDERING_START,
            MEDIA_INFO_MEDIA_ITEM_END,
            MEDIA_INFO_MEDIA_ITEM_LIST_END,
            MEDIA_INFO_MEDIA_ITEM_REPEAT,
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
     * a sync (or key) frame associated with a media item that is located
     * right before or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_PREVIOUS_SYNC    = 0x00;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a sync (or key) frame associated with a media item that is located
     * right after or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_NEXT_SYNC        = 0x01;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a sync (or key) frame associated with a media item that is located
     * closest to (in time) or at the given time.
     *
     * @see #seekTo(long, int)
     */
    public static final int SEEK_CLOSEST_SYNC     = 0x02;
    /**
     * This mode is used with {@link #seekTo(long, int)} to move media position to
     * a frame (not necessarily a key frame) associated with a media item that
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

    private static final int CALL_COMPLETE_PLAYLIST_BASE = -1000;
    private static final int END_OF_PLAYLIST = -1;
    private static final int NO_MEDIA_ITEM = -2;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sResultCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sErrorCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sInfoCodeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sSeekModeMap;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayMap<Integer, Integer> sPrepareDrmStatusMap;

    static {
        sResultCodeMap = new ArrayMap<>();
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_NO_ERROR, RESULT_CODE_SUCCESS);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN, RESULT_CODE_UNKNOWN_ERROR);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_INVALID_OPERATION, RESULT_CODE_INVALID_STATE);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_BAD_VALUE, RESULT_CODE_BAD_VALUE);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_PERMISSION_DENIED, RESULT_CODE_PERMISSION_DENIED);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_IO, RESULT_CODE_IO_ERROR);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_SKIPPED, RESULT_CODE_SKIPPED);

        sErrorCodeMap = new ArrayMap<>();
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNKNOWN, PLAYER_ERROR_UNKNOWN);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_IO, PLAYER_ERROR_IO);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_MALFORMED, PLAYER_ERROR_MALFORMED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED, PLAYER_ERROR_UNSUPPORTED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_TIMED_OUT, PLAYER_ERROR_TIMED_OUT);

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

        sPrepareDrmStatusMap = new ArrayMap<>();
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_SUCCESS,
                DrmResult.RESULT_CODE_SUCCESS);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR,
                DrmResult.RESULT_CODE_PROVISIONING_NETWORK_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR,
                DrmResult.RESULT_CODE_PREPARATION_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PREPARATION_ERROR,
                DrmResult.RESULT_CODE_PREPARATION_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_UNSUPPORTED_SCHEME,
                DrmResult.RESULT_CODE_UNSUPPORTED_SCHEME);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_RESOURCE_BUSY,
                DrmResult.RESULT_CODE_RESOURCE_BUSY);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaPlayer2 mPlayer;
    private ExecutorService mExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final class PendingCommand {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @MediaPlayer2.CallCompleted final int mCallType;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final ResolvableFuture mFuture;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PendingCommand(int callType, ResolvableFuture future) {
            mCallType = callType;
            mFuture = future;
        }
    }

    /* A list for tracking the commands submitted to MediaPlayer2.*/
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mPendingCommands")
    final ArrayDeque<PendingCommand> mPendingCommands = new ArrayDeque<>();

    /**
     * PendingFuture is a future for the result of execution which will be executed later via
     * the onExecute() method.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    abstract static class PendingFuture<V extends PlayerResult>
            extends AbstractResolvableFuture<V> {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final boolean mIsSeekTo;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        boolean mExecuteCalled = false;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        List<ResolvableFuture<V>> mFutures;

        PendingFuture(Executor executor) {
            this(executor, false);
        }

        PendingFuture(Executor executor, boolean isSeekTo) {
            mIsSeekTo = isSeekTo;
            addListener(new Runnable() {
                @Override
                public void run() {
                    if (isCancelled() && mExecuteCalled) {
                        cancelFutures();
                    }
                }
            }, executor);
        }

        @Override
        public boolean set(@Nullable V value) {
            return super.set(value);
        }

        @Override
        public boolean setException(Throwable throwable) {
            return super.setException(throwable);
        }

        public boolean execute() {
            if (!mExecuteCalled && !isCancelled()) {
                mExecuteCalled = true;
                mFutures = onExecute();
            }
            if (!isCancelled() && !isDone()) {
                setResultIfFinished();
            }
            return isCancelled() || isDone();
        }

        private void setResultIfFinished() {
            V result = null;
            for (int i = 0; i < mFutures.size(); ++i) {
                ResolvableFuture<V> future = mFutures.get(i);
                if (!future.isDone() && !future.isCancelled()) {
                    return;
                }
                try {
                    result = future.get();
                    int resultCode = result.getResultCode();
                    if (resultCode != RESULT_CODE_SUCCESS && resultCode != RESULT_CODE_SKIPPED) {
                        cancelFutures();
                        set(result);
                        return;
                    }
                } catch (Exception e) {
                    cancelFutures();
                    setException(e);
                    return;
                }
            }
            try {
                set(result);
            } catch (Exception e) {
                setException(e);
            }
        }

        abstract List<ResolvableFuture<V>> onExecute();

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        void cancelFutures() {
            for (ResolvableFuture<V> future : mFutures) {
                if (!future.isCancelled() && !future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }

    /* A list of pending operations within this MediaPlayer that will be executed sequentially. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mPendingFutures")
    final ArrayDeque<PendingFuture<? super PlayerResult>> mPendingFutures = new ArrayDeque<>();

    private final Object mStateLock = new Object();
    @GuardedBy("mStateLock")
    private @PlayerState int mState;
    @GuardedBy("mStateLock")
    private Map<MediaItem, Integer> mMediaItemToBuffState = new HashMap<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AudioFocusHandler mAudioFocusHandler;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mPlaylistLock = new Object();
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ArrayList<MediaItem> mPlaylist = new ArrayList<>();
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ArrayList<MediaItem> mShuffledList = new ArrayList<>();
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaMetadata mPlaylistMetadata;
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRepeatMode;
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mShuffleMode;
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mCurrentShuffleIdx;
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaItem mCurPlaylistItem;
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
            MediaItem mNextPlaylistItem;
    @GuardedBy("mPlaylistLock")
    private boolean mSetMediaItemCalled;

    public MediaPlayer(@NonNull Context context) {
        mState = PLAYER_STATE_IDLE;
        mPlayer = MediaPlayer2.create(context);
        mExecutor = Executors.newFixedThreadPool(1);
        mPlayer.setEventCallback(mExecutor, new Mp2Callback());
        mPlayer.setDrmEventCallback(mExecutor, new Mp2DrmCallback());
        mCurrentShuffleIdx = NO_MEDIA_ITEM;
        mAudioFocusHandler = new AudioFocusHandler(context, this);
    }

    @GuardedBy("mPendingCommands")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void addPendingCommandLocked(
            int callType, final ResolvableFuture future, final Object token) {
        final PendingCommand pendingCommand = new PendingCommand(callType, future);
        mPendingCommands.add(pendingCommand);
        future.addListener(new Runnable() {
            @Override
            public void run() {
                // Propagate the cancellation to the MediaPlayer2 implementation.
                if (future.isCancelled()) {
                    synchronized (mPendingCommands) {
                        if (mPlayer.cancel(token)) {
                            mPendingCommands.remove(pendingCommand);
                        }
                    }
                }
            }
        }, mExecutor);
    }

    private void addPendingFuture(final PendingFuture pendingFuture) {
        synchronized (mPendingFutures) {
            mPendingFutures.add(pendingFuture);
            executePendingFutures();
        }
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> play() {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                final ResolvableFuture<PlayerResult> future;
                if (mAudioFocusHandler.onPlay()) {
                    if (mPlayer.getAudioAttributes() == null) {
                        futures.add(setPlayerVolumeInternal(0f));
                    }
                    future = ResolvableFuture.create();
                    synchronized (mPendingCommands) {
                        Object token = mPlayer.play();
                        addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_PLAY, future, token);
                    }
                } else {
                    future = createFutureForResultCode(RESULT_CODE_UNKNOWN_ERROR);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> pause() {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                mAudioFocusHandler.onPause();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.pause();
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_PAUSE, future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Prepares the media items for playback.
     *
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> prepare() {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.prepare();
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_PREPARE, future, token);
                }
                // TODO: Changing buffering state is not correct. Think about changing MP2 event
                // APIs for the initial buffering for prepare case.
                setBufferingState(mPlayer.getCurrentMediaItem(),
                        BUFFERING_STATE_BUFFERING_AND_STARVED);
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> seekTo(final long position) {
        PendingFuture<PlayerResult> pendingFuture =
                new PendingFuture<PlayerResult>(mExecutor, true) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.seekTo(position);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SEEK_TO, future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setPlaybackSpeed(final float playbackSpeed) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setPlaybackParams(new PlaybackParams.Builder(
                            mPlayer.getPlaybackParams().getPlaybackParams())
                            .setSpeed(playbackSpeed).build());
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> setAudioAttributes(
            @NonNull final AudioAttributesCompat attr) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setAudioAttributes(attr);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
            buffState = mMediaItemToBuffState.get(mPlayer.getCurrentMediaItem());
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
    @Nullable
    public AudioAttributesCompat getAudioAttributes() {
        try {
            return mPlayer.getAudioAttributes();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setMediaItem(@NonNull final MediaItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                synchronized (mPlaylistLock) {
                    mPlaylist.clear();
                    mShuffledList.clear();
                    mCurPlaylistItem = item;
                    mNextPlaylistItem = null;
                    mCurrentShuffleIdx = END_OF_PLAYLIST;
                }
                futures.addAll(setMediaItemsInternal(item, null));
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> setPlaylist(
            @NonNull final List<MediaItem> playlist, @Nullable final MediaMetadata metadata) {
        if (playlist == null || playlist.isEmpty()) {
            throw new IllegalArgumentException("playlist shouldn't be null or empty");
        }
        for (MediaItem item : playlist) {
            if (item == null) {
                throw new IllegalArgumentException("playlist shouldn't contain null item");
            }
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    mPlaylistMetadata = metadata;
                    mPlaylist.clear();
                    mShuffledList.clear();
                    mPlaylist.addAll(playlist);
                    applyShuffleModeLocked();
                    mCurrentShuffleIdx = 0;
                    updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(
                            SessionPlayer.PlayerCallback callback) {
                        callback.onPlaylistChanged(MediaPlayer.this, playlist, metadata);
                    }
                });
                if (curItem != null) {
                    return setMediaItemsInternal(curItem, nextItem);
                }
                return createFuturesForResultCode(RESULT_CODE_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> addPlaylistItem(
            final int index, @NonNull final MediaItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                Pair<MediaItem, MediaItem> updatedCurNextItem;
                synchronized (mPlaylistLock) {
                    if (mPlaylist.contains(item)) {
                        return createFuturesForResultCode(RESULT_CODE_BAD_VALUE, item);
                    }
                    int clampedIndex = clamp(index, mPlaylist.size());
                    int addedShuffleIdx = clampedIndex;
                    mPlaylist.add(clampedIndex, item);
                    if (mShuffleMode == SessionPlayer.SHUFFLE_MODE_NONE) {
                        mShuffledList.add(clampedIndex, item);
                    } else {
                        // Add the item in random position of mShuffledList.
                        addedShuffleIdx = (int) (Math.random() * (mShuffledList.size() + 1));
                        mShuffledList.add(addedShuffleIdx, item);
                    }
                    if (addedShuffleIdx <= mCurrentShuffleIdx) {
                        mCurrentShuffleIdx++;
                    }
                    updatedCurNextItem = updateAndGetCurrentNextItemIfNeededLocked();
                }
                final List<MediaItem> playlist = getPlaylist();
                final MediaMetadata metadata = getPlaylistMetadata();
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(
                            SessionPlayer.PlayerCallback callback) {
                        callback.onPlaylistChanged(MediaPlayer.this, playlist, metadata);
                    }
                });

                if (updatedCurNextItem.second == null) {
                    return createFuturesForResultCode(RESULT_CODE_SUCCESS);
                }
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                futures.add(setNextMediaItemInternal(updatedCurNextItem.second));
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> removePlaylistItem(@IntRange(from = 0) final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                int removedItemShuffleIdx;
                MediaItem curItem;
                MediaItem nextItem;
                Pair<MediaItem, MediaItem> updatedCurNextItem = null;
                synchronized (mPlaylistLock) {
                    if (index >= mPlaylist.size()) {
                        return createFuturesForResultCode(RESULT_CODE_BAD_VALUE);
                    }
                    MediaItem item = mPlaylist.remove(index);
                    removedItemShuffleIdx = mShuffledList.indexOf(item);
                    mShuffledList.remove(removedItemShuffleIdx);
                    if (removedItemShuffleIdx < mCurrentShuffleIdx) {
                        mCurrentShuffleIdx--;
                    }
                    updatedCurNextItem = updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                final List<MediaItem> playlist = getPlaylist();
                final MediaMetadata metadata = getPlaylistMetadata();
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(
                            SessionPlayer.PlayerCallback callback) {
                        callback.onPlaylistChanged(MediaPlayer.this, playlist, metadata);
                    }
                });

                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                if (updatedCurNextItem != null) {
                    if (updatedCurNextItem.first != null) {
                        futures.addAll(setMediaItemsInternal(curItem, nextItem));
                    } else if (updatedCurNextItem.second != null) {
                        futures.add(setNextMediaItemInternal(nextItem));
                    }
                } else {
                    futures.add(createFutureForResultCode(RESULT_CODE_SUCCESS));
                }
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> replacePlaylistItem(
            final int index, @NonNull final MediaItem item) {
        if (item == null) {
            throw new IllegalArgumentException("item shouldn't be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                Pair<MediaItem, MediaItem> updatedCurNextItem = null;
                synchronized (mPlaylistLock) {
                    if (index >= mPlaylist.size() || mPlaylist.contains(item)) {
                        return createFuturesForResultCode(RESULT_CODE_BAD_VALUE, item);
                    }

                    int shuffleIdx = mShuffledList.indexOf(mPlaylist.get(index));
                    mShuffledList.set(shuffleIdx, item);
                    mPlaylist.set(index, item);
                    updatedCurNextItem = updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                // TODO: Should we notify current media item changed if it is replaced?
                final List<MediaItem> playlist = getPlaylist();
                final MediaMetadata metadata = getPlaylistMetadata();
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(
                            SessionPlayer.PlayerCallback callback) {
                        callback.onPlaylistChanged(MediaPlayer.this, playlist, metadata);
                    }
                });

                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                if (updatedCurNextItem != null) {
                    if (updatedCurNextItem.first != null) {
                        futures.addAll(setMediaItemsInternal(curItem, nextItem));
                    } else if (updatedCurNextItem.second != null) {
                        futures.add(setNextMediaItemInternal(nextItem));
                    }
                } else {
                    futures.add(createFutureForResultCode(RESULT_CODE_SUCCESS));
                }
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPreviousPlaylistItem() {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    int prevShuffleIdx = mCurrentShuffleIdx - 1;
                    if (prevShuffleIdx < 0) {
                        if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                            prevShuffleIdx = mShuffledList.size() - 1;
                        } else {
                            return createFuturesForResultCode(RESULT_CODE_INVALID_STATE);
                        }
                    }
                    mCurrentShuffleIdx = prevShuffleIdx;
                    updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                return setMediaItemsInternal(curItem, nextItem);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToNextPlaylistItem() {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    int nextShuffleIdx = mCurrentShuffleIdx + 1;
                    if (nextShuffleIdx >= mShuffledList.size()) {
                        if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                            nextShuffleIdx = 0;
                        } else {
                            return createFuturesForResultCode(RESULT_CODE_INVALID_STATE);
                        }
                    }
                    mCurrentShuffleIdx = nextShuffleIdx;
                    updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                if (curItem != null) {
                    return setMediaItemsInternal(curItem, nextItem);
                }
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                futures.add(skipToNextInternal());
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPlaylistItem(@IntRange(from = 0) final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    if (index >= mPlaylist.size()) {
                        return createFuturesForResultCode(RESULT_CODE_BAD_VALUE);
                    }
                    mCurrentShuffleIdx = mShuffledList.indexOf(mPlaylist.get(index));
                    updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                return setMediaItemsInternal(curItem, nextItem);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @NonNull
    @Override
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(
            @Nullable final MediaMetadata metadata) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                synchronized (mPlaylistLock) {
                    mPlaylistMetadata = metadata;
                }
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(
                            SessionPlayer.PlayerCallback callback) {
                        callback.onPlaylistMetadataChanged(MediaPlayer.this, metadata);
                    }
                });
                return createFuturesForResultCode(RESULT_CODE_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setRepeatMode(final int repeatMode) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                if (repeatMode < SessionPlayer.REPEAT_MODE_NONE
                        || repeatMode > SessionPlayer.REPEAT_MODE_GROUP) {
                    return createFuturesForResultCode(RESULT_CODE_BAD_VALUE);
                }

                boolean changed;
                synchronized (mPlaylistLock) {
                    changed = mRepeatMode != repeatMode;
                    mRepeatMode = repeatMode;
                }
                if (changed) {
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(
                                SessionPlayer.PlayerCallback callback) {
                            callback.onRepeatModeChanged(MediaPlayer.this, repeatMode);
                        }
                    });
                }
                return createFuturesForResultCode(RESULT_CODE_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setShuffleMode(final int shuffleMode) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                if (shuffleMode < SessionPlayer.SHUFFLE_MODE_NONE
                        || shuffleMode > SessionPlayer.SHUFFLE_MODE_GROUP) {
                    return createFuturesForResultCode(RESULT_CODE_BAD_VALUE);
                }

                boolean changed;
                synchronized (mPlaylistLock) {
                    changed = mShuffleMode != shuffleMode;
                    mShuffleMode = shuffleMode;
                }
                if (changed) {
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(
                                SessionPlayer.PlayerCallback callback) {
                            callback.onShuffleModeChanged(MediaPlayer.this, shuffleMode);
                        }
                    });
                }
                return createFuturesForResultCode(RESULT_CODE_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @Nullable
    public List<MediaItem> getPlaylist() {
        synchronized (mPlaylistLock) {
            return mPlaylist.isEmpty() ? null : new ArrayList<>(mPlaylist);
        }
    }

    @Override
    @Nullable
    public MediaMetadata getPlaylistMetadata() {
        synchronized (mPlaylistLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public int getRepeatMode() {
        synchronized (mPlaylistLock) {
            return mRepeatMode;
        }
    }

    @Override
    public int getShuffleMode() {
        synchronized (mPlaylistLock) {
            return mShuffleMode;
        }
    }

    @Override
    @Nullable
    public MediaItem getCurrentMediaItem() {
        return mPlayer.getCurrentMediaItem();
    }

    @Override
    public void close() throws Exception {
        mAudioFocusHandler.close();
        mPlayer.close();
        mExecutor.shutdown();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public AudioFocusHandler getAudioFocusHandler() {
        return mAudioFocusHandler;
    }

    /**
     * Resets {@link MediaPlayer} to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * media item and calling {@link #prepare()}.
     */
    public void reset() {
        // Cancel the pending commands.
        synchronized (mPendingCommands) {
            for (PendingCommand c : mPendingCommands) {
                c.mFuture.cancel(true);
            }
            mPendingCommands.clear();
        }
        // Cancel the pending futures.
        synchronized (mPendingFutures) {
            for (PendingFuture f : mPendingFutures) {
                if (f.mExecuteCalled && !f.isDone() && !f.isCancelled()) {
                    f.cancel(true);
                }
            }
            mPendingFutures.clear();
        }
        synchronized (mStateLock) {
            mState = PLAYER_STATE_IDLE;
            mMediaItemToBuffState.clear();
        }
        synchronized (mPlaylistLock) {
            mPlaylist.clear();
            mShuffledList.clear();
            mCurPlaylistItem = null;
            mNextPlaylistItem = null;
            mCurrentShuffleIdx = END_OF_PLAYLIST;
            mSetMediaItemCalled = false;
        }
        mAudioFocusHandler.onReset();
        mPlayer.reset();
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setSurface(@Nullable final Surface surface) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setSurface(surface);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_SURFACE, future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setPlayerVolume(final float volume) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                futures.add(setPlayerVolumeInternal(volume));
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
     * Returns the size of the video.
     *
     * @return the size of the video. The width and height of size could be 0 if there is no video,
     * no display surface was set, or the size has not been determined yet.
     * The {@link PlayerCallback} can be registered via {@link #registerPlayerCallback} to
     * receive a notification {@link PlayerCallback#onVideoSizeChanged} when the size
     * is available.
     */
    public @NonNull VideoSize getVideoSize() {
        return new VideoSize(mPlayer.getVideoWidth(), mPlayer.getVideoHeight());
    }

    /**
     * @return a {@link PersistableBundle} containing the set of attributes and values
     * available for the media being handled by this player instance.
     * The attributes are described in {@link MetricsConstants}.
     *
     * Additional vendor-specific fields may also be present in the return value.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public PersistableBundle getMetrics() {
        return mPlayer.getMetrics();
    }

    /**
     * Sets playback rate using {@link PlaybackParams}. The player sets its internal
     * PlaybackParams to the given input. This does not change the player state. For example,
     * if this is called with the speed of 2.0f in {@link #PLAYER_STATE_PAUSED}, the player will
     * just update internal property and stay paused. Once the client calls {@link #play()}
     * afterwards, the player will start playback with the given speed. Calling this with zero
     * speed is not allowed.
     *
     * @param params the playback params.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setPlaybackParams(@NonNull final PlaybackParams params) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setPlaybackParams(params);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Gets the playback params, containing the current playback rate.
     *
     * @return the playback params.
     */
    @NonNull
    public PlaybackParams getPlaybackParams() {
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
     * When seeking to the given time position, there is no guarantee that the media item
     * has a frame located at the position. When this happens, a frame nearby will be rendered.
     * The value should be in the range of start and end positions defined in {@link MediaItem}.
     * @param mode the mode indicating where exactly to seek to.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> seekTo(final long msec, @SeekMode final int mode) {
        PendingFuture<PlayerResult> pendingFuture =
                new PendingFuture<PlayerResult>(mExecutor, true) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                int mp2SeekMode = sSeekModeMap.getOrDefault(mode, SEEK_NEXT_SYNC);
                synchronized (mPendingCommands) {
                    Object token = mPlayer.seekTo(msec, mp2SeekMode);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SEEK_TO, future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Gets current playback position as a {@link MediaTimestamp}.
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
     *         is available, e.g. because the media player has not been initialized.
     *
     * @see MediaTimestamp
     */
    @Nullable
    public MediaTimestamp getTimestamp() {
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setAudioSessionId(final int sessionId) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setAudioSessionId(sessionId);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_AUDIO_SESSION_ID,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> attachAuxEffect(final int effectId) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.attachAuxEffect(effectId);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_ATTACH_AUX_EFFECT,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setAuxEffectSendLevel(final float level) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setAuxEffectSendLevel(level);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Returns a List of track information.
     *
     * @return List of track info. The total number of tracks is the size of the list.
     */
    @NonNull
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
     * If the player is in invalid state, {@link PlayerResult#RESULT_CODE_INVALID_STATE} will be
     * reported with {@link PlayerResult}.
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> selectTrack(final int index) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.selectTrack(index);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SELECT_TRACK,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
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
     * {@link PlayerResult} will be delivered when the command completes.
     */
    @NonNull
    public ListenableFuture<PlayerResult> deselectTrack(final int index) {
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.deselectTrack(index);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK,
                            future, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Retrieves the DRM Info associated with the current media item.
     *
     * @throws IllegalStateException if called before being prepared
     * @hide
     */
    @Nullable
    @RestrictTo(LIBRARY_GROUP)
    public DrmInfo getDrmInfo() {
        MediaPlayer2.DrmInfo info = mPlayer.getDrmInfo();
        return info == null ? null : new DrmInfo(info);
    }

    /**
     * Prepares the DRM for the current media item.
     * <p>
     * If {@link OnDrmConfigHelper} is registered, it will be called during
     * preparation to allow configuration of the DRM properties before opening the
     * DRM session. Note that the callback is called synchronously in the thread that called
     * {@link #prepareDrm}. It should be used only for a series of {@code getDrmPropertyString}
     * and {@code setDrmPropertyString} calls and refrain from any lengthy operation.
     * <p>
     * If the device has not been provisioned before, this call also provisions the device
     * which involves accessing the provisioning server and can take a variable time to
     * complete depending on the network connectivity.
     * prepareDrm() runs in non-blocking mode by launching the provisioning in the background and
     * returning. The application should check the {@link DrmResult#getResultCode()} returned with
     * {@link ListenableFuture} to proceed.
     * <p>
     *
     * @param uuid The UUID of the crypto scheme. If not known beforehand, it can be retrieved
     * from the source through {#link getDrmInfo} or registering
     * {@link PlayerCallback#onDrmInfo}.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link DrmResult} will be delivered when the command completes.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    // This is an asynchronous call.
    @NonNull
    public ListenableFuture<DrmResult> prepareDrm(@NonNull final UUID uuid) {
        PendingFuture<DrmResult> pendingFuture = new PendingFuture<DrmResult>(mExecutor) {
            @Override
            List<ResolvableFuture<DrmResult>> onExecute() {
                ArrayList<ResolvableFuture<DrmResult>> futures = new ArrayList<>();
                ResolvableFuture<DrmResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.prepareDrm(uuid);
                    addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_PREPARE_DRM, future, token);
                }
                futures.add(future);
                return futures;
            }
        };

        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Releases the DRM session
     * <p>
     * The player has to have an active DRM session and be in stopped, or prepared
     * state before this call is made.
     * A {@code reset()} call will release the DRM session implicitly.
     *
     * @throws NoDrmSchemeException if there is no active DRM session to release
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void releaseDrm() throws NoDrmSchemeException {
        try {
            mPlayer.releaseDrm();
        } catch (MediaPlayer2.NoDrmSchemeException e) {
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
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public MediaDrm.KeyRequest getDrmKeyRequest(
            @Nullable byte[] keySetId, @Nullable byte[] initData,
            @Nullable String mimeType, int keyType,
            @Nullable Map<String, String> optionalParameters)
            throws NoDrmSchemeException {
        try {
            return mPlayer.getDrmKeyRequest(
                    keySetId, initData, mimeType, keyType, optionalParameters);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * A key response is received from the license server by the app, then it is
     * provided to the DRM engine plugin using provideDrmKeyResponse. When the
     * response is for an offline key request, a key-set identifier is returned that
     * can be used to later restore the keys to a new session with the method
     * {@link #restoreDrmKeys}.
     * When the response is for a streaming or release request, null is returned.
     *
     * @param keySetId When the response is for a release request, keySetId identifies
     * the saved key associated with the release request (i.e., the same keySetId
     * passed to the earlier {@link #getDrmKeyRequest} call. It MUST be null when the
     * response is for either streaming or offline key requests.
     *
     * @param response the byte array response from the server
     *
     * @throws NoDrmSchemeException if there is no active DRM session
     * @throws DeniedByServerException if the response indicates that the
     * server rejected the request
     * @hide
     */
    @Nullable
    @RestrictTo(LIBRARY_GROUP)
    public byte[] provideDrmKeyResponse(
            @Nullable byte[] keySetId, @NonNull byte[] response)
            throws NoDrmSchemeException, DeniedByServerException {
        try {
            return mPlayer.provideDrmKeyResponse(keySetId, response);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * Restore persisted offline keys into a new session.  keySetId identifies the
     * keys to load, obtained from a prior call to {@link #provideDrmKeyResponse}.
     *
     * @param keySetId identifies the saved key set to restore
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void restoreDrmKeys(@NonNull byte[] keySetId) throws NoDrmSchemeException {
        try {
            mPlayer.restoreDrmKeys(keySetId);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * Read a DRM engine plugin String property value, given the property name string.
     * <p>
     * @param propertyName the property name
     *
     * Standard fields names are:
     * {@link MediaDrm#PROPERTY_VENDOR}, {@link MediaDrm#PROPERTY_VERSION},
     * {@link MediaDrm#PROPERTY_DESCRIPTION}, {@link MediaDrm#PROPERTY_ALGORITHMS}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName) throws NoDrmSchemeException {
        try {
            return mPlayer.getDrmPropertyString(propertyName);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * Set a DRM engine plugin String property value.
     * <p>
     * @param propertyName the property name
     * @param value the property value
     *
     * Standard fields names are:
     * {@link MediaDrm#PROPERTY_VENDOR}, {@link MediaDrm#PROPERTY_VERSION},
     * {@link MediaDrm#PROPERTY_DESCRIPTION}, {@link MediaDrm#PROPERTY_ALGORITHMS}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setDrmPropertyString(@NonNull String propertyName, @NonNull String value)
            throws NoDrmSchemeException {
        try {
            mPlayer.setDrmPropertyString(propertyName, value);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * Register a callback to be invoked for configuration of the DRM object before
     * the session is created.
     * The callback will be invoked synchronously during the execution
     * of {@link #prepareDrm(UUID uuid)}.
     *
     * @param listener the callback that will be run
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void setOnDrmConfigHelper(@Nullable final OnDrmConfigHelper listener) {
        mPlayer.setOnDrmConfigHelper(listener == null ? null :
                new MediaPlayer2.OnDrmConfigHelper() {
                    @Override
                    public void onDrmConfig(MediaPlayer2 mp, MediaItem item) {
                        listener.onDrmConfig(MediaPlayer.this, item);
                    }
                });
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
                public void callCallback(SessionPlayer.PlayerCallback callback) {
                    callback.onPlayerStateChanged(MediaPlayer.this, state);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void setBufferingState(final MediaItem item, @BuffState final int state) {
        Integer previousState;
        synchronized (mStateLock) {
            previousState = mMediaItemToBuffState.put(item, state);
        }
        if (previousState == null || previousState.intValue() != state) {
            notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                @Override
                public void callCallback(SessionPlayer.PlayerCallback callback) {
                    callback.onBufferingStateChanged(MediaPlayer.this, item, state);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifySessionPlayerCallback(final SessionPlayerCallbackNotifier notifier) {
        List<Pair<SessionPlayer.PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<SessionPlayer.PlayerCallback, Executor> pair : callbacks) {
            final SessionPlayer.PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    notifier.callCallback(callback);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void notifyMediaPlayerCallback(final MediaPlayerCallbackNotifier notifier) {
        List<Pair<SessionPlayer.PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<SessionPlayer.PlayerCallback, Executor> pair : callbacks) {
            if (pair.first instanceof PlayerCallback) {
                final PlayerCallback callback = (PlayerCallback) pair.first;
                pair.second.execute(new Runnable() {
                    @Override
                    public void run() {
                        notifier.callCallback(callback);
                    }
                });
            }
        }
    }

    private interface SessionPlayerCallbackNotifier {
        void callCallback(SessionPlayer.PlayerCallback callback);
    }

    private interface MediaPlayerCallbackNotifier {
        void callCallback(PlayerCallback callback);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<ResolvableFuture<PlayerResult>> setMediaItemsInternal(
            @NonNull MediaItem curItem, @Nullable MediaItem nextItem) {
        boolean setMediaItemCalled;
        synchronized (mPlaylistLock) {
            setMediaItemCalled = mSetMediaItemCalled;
        }

        ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
        if (setMediaItemCalled) {
            futures.add(setNextMediaItemInternal(curItem));
            futures.add(skipToNextInternal());
        } else {
            futures.add(setMediaItemInternal(curItem));
        }

        if (nextItem != null) {
            futures.add(setNextMediaItemInternal(nextItem));
        }
        return futures;
    }

    private ResolvableFuture<PlayerResult> setMediaItemInternal(MediaItem item) {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        synchronized (mPendingCommands) {
            Object token = mPlayer.setMediaItem(item);
            addPendingCommandLocked(MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE, future, token);
        }
        synchronized (mPlaylistLock) {
            mSetMediaItemCalled = true;
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ResolvableFuture<PlayerResult> setNextMediaItemInternal(MediaItem item) {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        synchronized (mPendingCommands) {
            Object token = mPlayer.setNextMediaItem(item);
            addPendingCommandLocked(
                    MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE, future, token);
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ResolvableFuture<PlayerResult> skipToNextInternal() {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        synchronized (mPendingCommands) {
            Object token = mPlayer.skipToNext();
            addPendingCommandLocked(
                    MediaPlayer2.CALL_COMPLETED_SKIP_TO_NEXT, future, token);
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ResolvableFuture<PlayerResult> setPlayerVolumeInternal(float volume) {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        synchronized (mPendingCommands) {
            Object token = mPlayer.setPlayerVolume(volume);
            addPendingCommandLocked(
                    MediaPlayer2.CALL_COMPLETED_SET_PLAYER_VOLUME, future, token);
        }
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ResolvableFuture<PlayerResult> createFutureForResultCode(int resultCode) {
        return createFutureForResultCode(resultCode, null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ResolvableFuture<PlayerResult> createFutureForResultCode(int resultCode, MediaItem item) {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        future.set(new PlayerResult(resultCode,
                item == null ? mPlayer.getCurrentMediaItem() : item));
        return future;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<ResolvableFuture<PlayerResult>> createFuturesForResultCode(int resultCode) {
        return createFuturesForResultCode(resultCode, null);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<ResolvableFuture<PlayerResult>> createFuturesForResultCode(int resultCode,
            MediaItem item) {
        ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
        futures.add(createFutureForResultCode(resultCode, item));
        return futures;
    }

    @SuppressWarnings({"GuardedBy", "WeakerAccess"}) /* synthetic access */
    void applyShuffleModeLocked() {
        mShuffledList.clear();
        mShuffledList.addAll(mPlaylist);
        if (mShuffleMode == SessionPlayer.SHUFFLE_MODE_ALL
                || mShuffleMode == SessionPlayer.SHUFFLE_MODE_GROUP) {
            Collections.shuffle(mShuffledList);
        }
    }

    /**
     * Update mCurPlaylistItem and mNextPlaylistItem based on mCurrentShuffleIdx value.
     *
     * @return A pair contains the changed current item and next item. If current item or next item
     * is not changed, Pair.first or Pair.second will be null. If current item and next item are the
     * same, it will return null Pair. If non null Pair which contains two nulls, that means one of
     * current and next item or both are changed to null.
     */
    @SuppressWarnings({"GuardedBy", "WeakerAccess"}) /* synthetic access */
    Pair<MediaItem, MediaItem> updateAndGetCurrentNextItemIfNeededLocked() {
        MediaItem changedCurItem = null;
        MediaItem changedNextItem = null;
        if (mCurrentShuffleIdx < 0) {
            if (mCurPlaylistItem == null && mNextPlaylistItem == null) {
                return null;
            }
            mCurPlaylistItem = null;
            mNextPlaylistItem = null;
            return new Pair<>(null, null);
        }
        if (!Objects.equals(mCurPlaylistItem, mShuffledList.get(mCurrentShuffleIdx))) {
            changedCurItem = mCurPlaylistItem = mShuffledList.get(mCurrentShuffleIdx);
        }
        int nextShuffleIdx = mCurrentShuffleIdx + 1;
        if (nextShuffleIdx >= mShuffledList.size()) {
            if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                nextShuffleIdx = 0;
            } else {
                nextShuffleIdx = END_OF_PLAYLIST;
            }
        }

        if (nextShuffleIdx == END_OF_PLAYLIST) {
            mNextPlaylistItem = null;
        } else if (!Objects.equals(mNextPlaylistItem, mShuffledList.get(nextShuffleIdx))) {
            changedNextItem = mNextPlaylistItem = mShuffledList.get(nextShuffleIdx);
        }

        return (changedCurItem == null && changedNextItem == null)
                ? null : new Pair<>(changedCurItem, changedNextItem);
    }

    // Clamps value to [0, maxValue]
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static int clamp(int value, int maxValue) {
        if (value < 0) {
            return 0;
        }
        return (value > maxValue) ? maxValue : value;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleCallComplete(MediaPlayer2 mp, final MediaItem item, int what, int status) {
        PendingCommand expected;
        synchronized (mPendingCommands) {
            expected = mPendingCommands.pollFirst();
        }
        if (expected == null) {
            Log.i(TAG, "No matching call type for " + what + ". Possibly because of reset().");
            return;
        }

        if (what != expected.mCallType) {
            Log.w(TAG, "Call type does not match. expeced:" + expected.mCallType
                    + " actual:" + what);
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
                                SessionPlayer.PlayerCallback callback) {
                            callback.onSeekCompleted(MediaPlayer.this, pos);
                        }
                    });
                    break;
                case MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE:
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(
                                SessionPlayer.PlayerCallback callback) {
                            callback.onCurrentMediaItemChanged(MediaPlayer.this, item);
                        }
                    });
                    break;
                case MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS:
                    // TODO: Need to check if the speed value is really changed.
                    final float speed = mPlayer.getPlaybackParams().getSpeed();
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(
                                SessionPlayer.PlayerCallback callback) {
                            callback.onPlaybackSpeedChanged(MediaPlayer.this, speed);
                        }
                    });
                    break;
                case MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES:
                    final AudioAttributesCompat attr = mPlayer.getAudioAttributes();
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(SessionPlayer.PlayerCallback callback) {
                            callback.onAudioAttributesChanged(MediaPlayer.this, attr);
                        }
                    });
                    break;
            }
        }
        if (what != MediaPlayer2.CALL_COMPLETED_PREPARE_DRM) {
            Integer resultCode = sResultCodeMap.getOrDefault(status, RESULT_CODE_UNKNOWN_ERROR);
            expected.mFuture.set(new PlayerResult(resultCode, item));
        } else {
            Integer resultCode = sPrepareDrmStatusMap.getOrDefault(
                    status, DrmResult.RESULT_CODE_PREPARATION_ERROR);
            expected.mFuture.set(new DrmResult(resultCode, item));
        }
        executePendingFutures();
    }

    private void executePendingFutures() {
        synchronized (mPendingFutures) {
            Iterator<PendingFuture<? super PlayerResult>> it = mPendingFutures.iterator();
            while (it.hasNext()) {
                PendingFuture f = it.next();
                if (f.isCancelled() || f.execute()) {
                    mPendingFutures.removeFirst();
                } else {
                    break;
                }
            }
            // Execute skip futures earlier for making them be skipped.
            while (it.hasNext()) {
                PendingFuture f = it.next();
                if (!f.mIsSeekTo) {
                    break;
                }
                f.execute();
            }
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    class Mp2DrmCallback extends MediaPlayer2.DrmEventCallback {
        @Override
        public void onDrmInfo(
                MediaPlayer2 mp, final MediaItem item, final MediaPlayer2.DrmInfo drmInfo) {
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onDrmInfo(MediaPlayer.this, item,
                            drmInfo == null ? null : new DrmInfo(drmInfo));
                }
            });
        }

        @Override
        public void onDrmPrepared(MediaPlayer2 mp, final MediaItem item, final int status) {
            handleCallComplete(mp, item, MediaPlayer2.CALL_COMPLETED_PREPARE_DRM, status);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    class Mp2Callback extends MediaPlayer2.EventCallback {
        @Override
        public void onVideoSizeChanged(
                MediaPlayer2 mp, final MediaItem item, final int width, final int height) {
            final VideoSize size = new VideoSize(width, height);
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onVideoSizeChanged(MediaPlayer.this, item, size);
                }
            });
        }

        @Override
        public void onTimedMetaDataAvailable(
                MediaPlayer2 mp, final MediaItem item, final TimedMetaData data) {
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onTimedMetaDataAvailable(MediaPlayer.this, item, data);
                }
            });
        }

        @Override
        public void onError(
                MediaPlayer2 mp, final MediaItem item, final int what, final int extra) {
            setState(PLAYER_STATE_ERROR);
            setBufferingState(item, BUFFERING_STATE_UNKNOWN);
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onError(MediaPlayer.this, item, what, extra);
                }
            });
        }

        @Override
        public void onInfo(
                MediaPlayer2 mp, final MediaItem item, final int mp2What, final int extra) {
            switch (mp2What) {
                case MediaPlayer2.MEDIA_INFO_BUFFERING_START:
                    setBufferingState(item, BUFFERING_STATE_BUFFERING_AND_STARVED);
                    break;
                case MediaPlayer2.MEDIA_INFO_PREPARED:
                case MediaPlayer2.MEDIA_INFO_BUFFERING_END:
                    setBufferingState(item, BUFFERING_STATE_BUFFERING_AND_PLAYABLE);
                    break;
                case MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE:
                    if (extra /* percent */ >= 100) {
                        setBufferingState(item, BUFFERING_STATE_COMPLETE);
                    }
                    break;
                case MediaPlayer2.MEDIA_INFO_DATA_SOURCE_LIST_END:
                    setState(PLAYER_STATE_PAUSED);
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(SessionPlayer.PlayerCallback callback) {
                            callback.onPlaybackCompleted(MediaPlayer.this);
                        }
                    });
                    break;
            }
            final int what = sInfoCodeMap.getOrDefault(mp2What, MEDIA_INFO_UNKNOWN);
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onInfo(MediaPlayer.this, item, what, extra);
                }
            });
        }

        @Override
        public void onCallCompleted(
                MediaPlayer2 mp, final MediaItem item, int what, int status) {
            handleCallComplete(mp, item, what, status);
        }

        @Override
        public void onMediaTimeDiscontinuity(
                MediaPlayer2 mp, final MediaItem item, final MediaTimestamp timestamp) {
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onMediaTimeDiscontinuity(MediaPlayer.this, item, timestamp);
                }
            });
        }

        @Override
        public void onCommandLabelReached(MediaPlayer2 mp, Object label) {
            // Ignore. MediaPlayer does not use MediaPlayer2.notifyWhenCommandLabelReached().
        }

        @Override
        public void onSubtitleData(
                MediaPlayer2 mp, final MediaItem item, final SubtitleData data) {
            notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                @Override
                public void callCallback(PlayerCallback callback) {
                    callback.onSubtitleData(MediaPlayer.this, item, data);
                }
            });
        }
    }

    /**
     * Interface definition for callbacks to be invoked when the player has the corresponding
     * events.
     */
    public abstract static class PlayerCallback extends SessionPlayer.PlayerCallback {
        /**
         * Called to indicate the video size
         *
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param mp the player associated with this callback
         * @param item the MediaItem of this media item
         * @param size the size of the video
         */
        public void onVideoSizeChanged(
                @NonNull MediaPlayer mp, @NonNull MediaItem item, @NonNull VideoSize size) { }

        /**
         * Called to indicate available timed metadata
         * <p>
         * This method will be called as timed metadata is extracted from the media,
         * in the same order as it occurs in the media. The timing of this event is
         * not controlled by the associated timestamp.
         * <p>
         * Currently only HTTP live streaming data URI's embedded with timed ID3 tags generates
         * {@link TimedMetaData}.
         *
         * @see TimedMetaData
         *
         * @param mp the player associated with this callback
         * @param item the MediaItem of this media item
         * @param data the timed metadata sample associated with this event
         */
        public void onTimedMetaDataAvailable(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @NonNull TimedMetaData data) { }

        /**
         * Called to indicate an error.
         *
         * @param mp the MediaPlayer2 the error pertains to
         * @param item the MediaItem of this media item
         * @param what the type of error that has occurred.
         * @param extra an extra code, specific to the error. Typically
         * implementation dependent.
         */
        public void onError(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @MediaError int what, int extra) { }

        /**
         * Called to indicate an info or a warning.
         *
         * @param mp the player the info pertains to.
         * @param item the MediaItem of this media item
         * @param what the type of info or warning.
         * @param extra an extra code, specific to the info. Typically
         * implementation dependent.
         */
        public void onInfo(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @MediaInfo int what, int extra) { }

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
         * @param item the MediaItem of this media item
         * @param timestamp the timestamp that correlates media time, system time and clock rate,
         *     or {@link MediaTimestamp#TIMESTAMP_UNKNOWN} in an error case.
         */
        public void onMediaTimeDiscontinuity(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @NonNull MediaTimestamp timestamp) { }

        /**
         * Called when when a player subtitle track has new subtitle data available.
         * @param mp the player that reports the new subtitle data
         * @param item the MediaItem of this media item
         * @param data the subtitle data
         */
        public void onSubtitleData(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @NonNull SubtitleData data) { }

        /**
         * Called to indicate DRM info is available
         *
         * @param mp the {@code MediaPlayer2} associated with this callback
         * @param item the MediaItem of this media item
         * @param drmInfo DRM info of the source including PSSH, and subset
         *                of crypto schemes supported by this device
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public void onDrmInfo(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @NonNull DrmInfo drmInfo) { }
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
        /** @hide */
        @RestrictTo(LIBRARY_GROUP)
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
        @NonNull
        public String getLanguage() {
            String language = mFormat.getString(MediaFormat.KEY_LANGUAGE);
            return language == null ? "und" : language;
        }

        /**
         * Gets the {@link MediaFormat} of the track.  If the format is
         * unknown or could not be determined, null is returned.
         */
        @Nullable
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
     * Encapsulates the DRM properties of the source.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final class DrmInfo {
        private final MediaPlayer2.DrmInfo mMp2DrmInfo;

        /**
         * Returns the PSSH info of the media item for each supported DRM scheme.
         */
        @NonNull
        public Map<UUID, byte[]> getPssh() {
            return mMp2DrmInfo.getPssh();
        }

        /**
         * Returns the intersection of the media item and the device DRM schemes.
         * It effectively identifies the subset of the source's DRM schemes which
         * are supported by the device too.
         */
        @NonNull
        public List<UUID> getSupportedSchemes() {
            return mMp2DrmInfo.getSupportedSchemes();
        }

        DrmInfo(MediaPlayer2.DrmInfo info) {
            mMp2DrmInfo = info;
        }
    };

    /**
     * Interface definition of a callback to be invoked when the app
     * can do DRM configuration (get/set properties) before the session
     * is opened. This facilitates configuration of the properties, like
     * 'securityLevel', which has to be set after DRM scheme creation but
     * before the DRM session is opened.
     *
     * The only allowed DRM calls in this listener are {@link #getDrmPropertyString}
     * and {@link #setDrmPropertyString}.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnDrmConfigHelper {
        /**
         * Called to give the app the opportunity to configure DRM before the session is created
         *
         * @param mp the {@code MediaPlayer} associated with this callback
         * @param item the MediaItem of this media item
         */
        void onDrmConfig(@NonNull MediaPlayer mp, @NonNull MediaItem item);
    }

    /**
     * Thrown when a DRM method is called before preparing a DRM scheme through prepareDrm().
     * Extends MediaDrm.MediaDrmException
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class NoDrmSchemeException extends MediaDrmException {
        public NoDrmSchemeException(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Definitions for the metrics that are reported via the {@link #getMetrics} call.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
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

    /**
     * Result class of the asynchronous DRM APIs.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class DrmResult extends PlayerResult {
        /**
         * The device required DRM provisioning but couldn't reach the provisioning server.
         */
        public static final int RESULT_CODE_PROVISIONING_NETWORK_ERROR = -1001;

        /**
         * The device required DRM provisioning but the provisioning server denied the request.
         */
        public static final int RESULT_CODE_PROVISIONING_SERVER_ERROR = -1002;

        /**
         * The DRM preparation has failed.
         */
        public static final int RESULT_CODE_PREPARATION_ERROR = -1003;

        /**
         * The crypto scheme UUID that is not supported by the device.
         */
        public static final int RESULT_CODE_UNSUPPORTED_SCHEME = -1004;

        /**
         * The hardware resources are not available, due to being in use.
         */
        public static final int RESULT_CODE_RESOURCE_BUSY = -1005;

        /** @hide */
        @IntDef(flag = false, /*prefix = "PREPARE_DRM_STATUS",*/ value = {
                RESULT_CODE_SUCCESS,
                RESULT_CODE_PROVISIONING_NETWORK_ERROR,
                RESULT_CODE_PROVISIONING_SERVER_ERROR,
                RESULT_CODE_PREPARATION_ERROR,
                RESULT_CODE_UNSUPPORTED_SCHEME,
                RESULT_CODE_RESOURCE_BUSY,
        })
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY_GROUP)
        public @interface DrmResultCode {}

        /**
         * Constructor that uses the current system clock as the completion time.
         *
         * @param resultCode result code. Recommends to use the standard code defined here.
         * @param item media item when the operation is completed
         */
        public DrmResult(@DrmResultCode int resultCode, @NonNull MediaItem item) {
            super(resultCode, item);
        }

        /**
         * Gets the result code.
         *
         * @return result code.
         */
        @Override
        @DrmResultCode
        public int getResultCode() {
            return super.getResultCode();
        }
    }
}
