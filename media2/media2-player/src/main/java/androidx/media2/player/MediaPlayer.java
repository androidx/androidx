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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_BAD_VALUE;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_INVALID_STATE;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_IO;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_PERMISSION_DENIED;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_ERROR_UNKNOWN;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_INFO_SKIPPED;
import static androidx.media2.common.SessionPlayer.PlayerResult.RESULT_SUCCESS;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.MediaFormat;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.FloatRange;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.concurrent.futures.AbstractResolvableFuture;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.UriMediaItem;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * <h3 id="AudioFocusAndNoisyIntent">Audio focus and noisy intent</h3>
 * <p>
 * By default, {@link MediaPlayer} handles audio focus and noisy intent with
 * {@link AudioAttributesCompat} set to this player. You need to call
 * {@link #setAudioAttributes(AudioAttributesCompat)} set the audio attribute while in the
 * {@link #PLAYER_STATE_IDLE}.
 * <p>
 * Here's the table of automatic audio focus behavior with audio attributes.
 * <table summary="Audio focus handling overview">
 * <tr><th>Audio Attributes</th><th>Audio Focus Gain Type</th><th>Misc</th></tr>
 * <tr><td>{@link AudioAttributesCompat#USAGE_VOICE_COMMUNICATION_SIGNALLING}</td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_NONE}</td>
 *     <td></td></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_GAME}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_MEDIA}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_UNKNOWN}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN}</td>
 *     <td>Developers should specific a proper usage instead of
 *         {@link AudioAttributesCompat#USAGE_UNKNOWN}</td></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ALARM}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_VOICE_COMMUNICATION}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT}</td>
 *     <td></td></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ASSISTANCE_NAVIGATION_GUIDANCE}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_ASSISTANCE_SONIFICATION}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_DELAYED}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_INSTANT}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_COMMUNICATION_REQUEST}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_EVENT}</li>
 *             <li>{@link AudioAttributesCompat#USAGE_NOTIFICATION_RINGTONE}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}</td>
 *     <td></td></tr>
 * <tr><td><ul><li>{@link AudioAttributesCompat#USAGE_ASSISTANT}</li></ul></td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE}</td>
 *     <td></td></tr>
 * <tr><td>{@link AudioAttributesCompat#USAGE_ASSISTANCE_ACCESSIBILITY}</td>
 *     <td>{@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT} if
 *         {@link AudioAttributesCompat#CONTENT_TYPE_SPEECH},
 *         {@link android.media.AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK} otherwise</td>
 *     <td></td></tr>
 * <tr><td>{@code null}</td>
 *     <td>No audio focus handling, and sets the player volume to {@code 0}</td>
 *     <td>Only valid if your media contents don't have audio</td></tr>
 * <tr><td>Any other AudioAttributes</td>
 *     <td>No audio focus handling, and sets the player volume to {@code 0}</td>
 *     <td>This is to handle error</td></tr>
 * </table>
 * <p>
 * If an {@link AudioAttributesCompat} is not specified by {@link #setAudioAttributes},
 * {@link #getAudioAttributes} will return {@code null} and the default audio focus behavior will
 * follow the {@code null} case on the table above.
 * <p>
 * For more information about the audio focus, take a look at
 * <a href="{@docRoot}guide/topics/media-apps/audio-focus.html">Managing audio focus</a>
 * <p>
 */
public final class MediaPlayer extends SessionPlayer {
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

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "PLAYER_ERROR",*/ value = {
            PLAYER_ERROR_UNKNOWN,
            PLAYER_ERROR_IO,
            PLAYER_ERROR_MALFORMED,
            PLAYER_ERROR_UNSUPPORTED,
            PLAYER_ERROR_TIMED_OUT,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface MediaError {}

    /**
     * The player just started the playback of this media item.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_MEDIA_ITEM_END = 5;

    /**
     * The player just completed the playback of all the media items set by {@link #setPlaylist}
     * and {@link #setMediaItem}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_MEDIA_ITEM_LIST_END = 6;

    /**
     * The player just completed an iteration of playback loop. This event is sent only when
     * looping is enabled by {@link #setRepeatMode(int)}.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_MEDIA_ITEM_REPEAT = 7;

    /**
     * The player just finished preparing a media item for playback.
     * @see #prepare()
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /**
     * The player is resuming playback after filling buffers.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /**
     * Estimated network bandwidth information (kbps) is available; currently this event fires
     * simultaneously as {@link #MEDIA_INFO_BUFFERING_START} and {@link #MEDIA_INFO_BUFFERING_END}
     * when playing network files.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_NETWORK_BANDWIDTH = 703;

    /**
     * Update status in buffering a media source received through progressive downloading.
     * The received buffering percentage indicates how much of the content has been buffered
     * or played. For example a buffering update of 80 percent when half the content
     * has already been played indicates that the next 30 percent of the
     * content to play has been buffered.
     *
     * <p>The {@code extra} parameter in {@link PlayerCallback#onInfo} is the
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
     */
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /**
     * A new set of external-only metadata is available.  Used by
     * JAVA framework to avoid triggering track scanning.
     * @hide
     */
    @RestrictTo(LIBRARY)
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

    /**
     * Subtitle track was not supported by the media framework.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;

    /**
     * Reading the subtitle track takes too long.
     * @see PlayerCallback#onInfo
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    /**
     * @hide
     */
    @IntDef(flag = false, /*prefix = "MEDIA_INFO",*/ value = {
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
            MEDIA_INFO_UNSUPPORTED_SUBTITLE,
            MEDIA_INFO_SUBTITLE_TIMED_OUT
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public @interface SeekMode {}

    /**
     * The return value of {@link #getSelectedTrack(int)} when there is no selected track
     * for the given type.
     *
     * @see #getSelectedTrack(int)
     * @deprecated {@link #getSelectedTrack(int)} returns {@code null} instead of this value.
     */
    @Deprecated
    public static final int NO_TRACK_SELECTED = Integer.MIN_VALUE;

    static final PlaybackParams DEFAULT_PLAYBACK_PARAMS = new PlaybackParams.Builder()
            .setSpeed(1f)
            .setPitch(1f)
            .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
            .build();

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
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_NO_ERROR, RESULT_SUCCESS);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_UNKNOWN, RESULT_ERROR_UNKNOWN);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_INVALID_OPERATION, RESULT_ERROR_INVALID_STATE);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_BAD_VALUE, RESULT_ERROR_BAD_VALUE);
        sResultCodeMap.put(
                MediaPlayer2.CALL_STATUS_PERMISSION_DENIED, RESULT_ERROR_PERMISSION_DENIED);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_ERROR_IO, RESULT_ERROR_IO);
        sResultCodeMap.put(MediaPlayer2.CALL_STATUS_SKIPPED, RESULT_INFO_SKIPPED);

        sErrorCodeMap = new ArrayMap<>();
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNKNOWN, PLAYER_ERROR_UNKNOWN);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_IO, PLAYER_ERROR_IO);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_MALFORMED, PLAYER_ERROR_MALFORMED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_UNSUPPORTED, PLAYER_ERROR_UNSUPPORTED);
        sErrorCodeMap.put(MediaPlayer2.MEDIA_ERROR_TIMED_OUT, PLAYER_ERROR_TIMED_OUT);

        sInfoCodeMap = new ArrayMap<>();
        sInfoCodeMap.put(
                MediaPlayer2.MEDIA_INFO_VIDEO_RENDERING_START, MEDIA_INFO_VIDEO_RENDERING_START);
        sInfoCodeMap.put(
                MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING, MEDIA_INFO_VIDEO_TRACK_LAGGING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, MEDIA_INFO_BUFFERING_UPDATE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_BAD_INTERLEAVING, MEDIA_INFO_BAD_INTERLEAVING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE, MEDIA_INFO_NOT_SEEKABLE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_METADATA_UPDATE, MEDIA_INFO_METADATA_UPDATE);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_AUDIO_NOT_PLAYING, MEDIA_INFO_AUDIO_NOT_PLAYING);
        sInfoCodeMap.put(MediaPlayer2.MEDIA_INFO_VIDEO_NOT_PLAYING, MEDIA_INFO_VIDEO_NOT_PLAYING);

        sSeekModeMap = new ArrayMap<>();
        sSeekModeMap.put(SEEK_PREVIOUS_SYNC, MediaPlayer2.SEEK_PREVIOUS_SYNC);
        sSeekModeMap.put(SEEK_NEXT_SYNC, MediaPlayer2.SEEK_NEXT_SYNC);
        sSeekModeMap.put(SEEK_CLOSEST_SYNC, MediaPlayer2.SEEK_CLOSEST_SYNC);
        sSeekModeMap.put(SEEK_CLOSEST, MediaPlayer2.SEEK_CLOSEST);

        sPrepareDrmStatusMap = new ArrayMap<>();
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_SUCCESS,
                DrmResult.RESULT_SUCCESS);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR,
                DrmResult.RESULT_ERROR_PROVISIONING_NETWORK_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR,
                DrmResult.RESULT_ERROR_PREPARATION_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_PREPARATION_ERROR,
                DrmResult.RESULT_ERROR_PREPARATION_ERROR);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_UNSUPPORTED_SCHEME,
                DrmResult.RESULT_ERROR_UNSUPPORTED_SCHEME);
        sPrepareDrmStatusMap.put(MediaPlayer2.PREPARE_DRM_STATUS_RESOURCE_BUSY,
                DrmResult.RESULT_ERROR_RESOURCE_BUSY);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaPlayer2 mPlayer;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ExecutorService mExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final class PendingCommand {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @MediaPlayer2.CallCompleted final int mCallType;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final ResolvableFuture<? extends PlayerResult> mFuture;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final SessionPlayer.TrackInfo mTrackInfo;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PendingCommand(int callType, ResolvableFuture<? extends PlayerResult> future) {
            this(callType, future, null);
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PendingCommand(int callType, ResolvableFuture<? extends PlayerResult> future,
                SessionPlayer.TrackInfo trackInfo) {
            mCallType = callType;
            mFuture = future;
            mTrackInfo = trackInfo;
        }

        @SuppressWarnings("unchecked")
        <V extends PlayerResult> void setResult(V value) {
            ((ResolvableFuture<V>) mFuture).set(value);
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
                    if (resultCode != RESULT_SUCCESS && resultCode != RESULT_INFO_SKIPPED) {
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
            if (mFutures != null) {
                for (ResolvableFuture<V> future : mFutures) {
                    if (!future.isCancelled() && !future.isDone()) {
                        future.cancel(true);
                    }
                }
            }
        }
    }

    /* A list of pending operations within this MediaPlayer that will be executed sequentially. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @GuardedBy("mPendingFutures")
    final ArrayDeque<PendingFuture<? extends PlayerResult>> mPendingFutures = new ArrayDeque<>();

    private final Object mStateLock = new Object();
    @GuardedBy("mStateLock")
    @PlayerState
    private int mState;
    @GuardedBy("mStateLock")
    private Map<MediaItem, Integer> mMediaItemToBuffState = new HashMap<>();
    @GuardedBy("mStateLock")
    private boolean mClosed;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final AudioFocusHandler mAudioFocusHandler;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Object mPlaylistLock = new Object();
    @GuardedBy("mPlaylistLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaItemList mPlaylist = new MediaItemList();
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

    /**
     * Constructor to create a MediaPlayer instance.
     *
     * @param context A {@link Context} that will be used to resolve {@link UriMediaItem}.
     */
    public MediaPlayer(@NonNull Context context) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
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
            int callType, final ResolvableFuture<? extends PlayerResult> future,
            final Object token) {
        final PendingCommand pendingCommand = new PendingCommand(callType, future);
        mPendingCommands.add(pendingCommand);
        addFutureListener(pendingCommand, future, token);
    }

    @GuardedBy("mPendingCommands")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void addPendingCommandWithTrackInfoLocked(
            int callType, final ResolvableFuture<? extends PlayerResult> future,
            final SessionPlayer.TrackInfo trackInfo, final Object token) {
        final PendingCommand pendingCommand = new PendingCommand(callType, future, trackInfo);
        mPendingCommands.add(pendingCommand);
        addFutureListener(pendingCommand, future, token);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void addFutureListener(final PendingCommand pendingCommand,
            final ResolvableFuture<? extends PlayerResult> future, final Object token) {
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void addPendingFuture(final PendingFuture<? extends PlayerResult> pendingFuture) {
        synchronized (mPendingFutures) {
            mPendingFutures.add(pendingFuture);
            executePendingFutures();
        }
    }

    /**
     * Starts or resumes playback.
     * <p>
     * On success, this transfers the player state to {@link #PLAYER_STATE_PLAYING} and
     * a {@link SessionPlayer.PlayerResult} would be returned with the current media item when
     * the command was completed. If it is called in {@link #PLAYER_STATE_IDLE} or
     * {@link #PLAYER_STATE_ERROR}, it would be ignored and a {@link SessionPlayer.PlayerResult}
     * would be returned with {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> play() {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
                    future = createFutureForResultCode(RESULT_ERROR_UNKNOWN);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Pauses playback.
     * <p>
     * On success, this transfers the player state to {@link #PLAYER_STATE_PAUSED} and
     * a {@link SessionPlayer.PlayerResult} would be returned with the current media item when the
     * command was completed. If it is called in {@link #PLAYER_STATE_IDLE} or
     * {@link #PLAYER_STATE_ERROR}, it would be ignored and a {@link SessionPlayer.PlayerResult}
     * would be returned with {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> pause() {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
     * Prepares the media items for playback. Before calling this API, set media item(s) through
     * either {@link #setMediaItem} or {@link #setPlaylist}, and set a display surface through
     * {@link #setSurface} when needed.
     * <p>
     * On success, this transfers the player state from {@link #PLAYER_STATE_IDLE} to
     * {@link #PLAYER_STATE_PAUSED} and a {@link SessionPlayer.PlayerResult} would be returned with
     * the prepared media item when the command completed. If it's not called in
     * {@link #PLAYER_STATE_IDLE}, it would be ignored and {@link SessionPlayer.PlayerResult} would
     * be returned with {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> prepare() {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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

    /**
     * Seeks to the specified position.
     * <p>
     * The position is the relative position based on the {@link MediaItem#getStartPosition()}. So
     * calling {@link #seekTo(long)} with {@code 0} means the seek to the start position.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed. If it's called in {@link #PLAYER_STATE_IDLE}, it is ignored
     * and a {@link SessionPlayer.PlayerResult} would be returned with
     * {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE}.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @param position the new playback position in ms. The value would be in the range of start
     * and end positions defined in {@link MediaItem}.
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> seekTo(final long position) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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

    /**
     * Sets the playback speed. The default playback speed is {@code 1.0f}, and values less than
     * or equals to {@code 0.0f} is not allowed.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @param playbackSpeed the requested playback speed
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see #getPlaybackSpeed()
     * @see PlayerCallback#onPlaybackSpeedChanged(SessionPlayer, float)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setPlaybackSpeed(
            @FloatRange(from = 0.0f, to = Float.MAX_VALUE, fromInclusive = false)
            final float playbackSpeed) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                if (playbackSpeed <= 0.0f) {
                    return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
                }
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setPlaybackParams(new PlaybackParams.Builder(
                            mPlayer.getPlaybackParams())
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

    /**
     * Sets the {@link AudioAttributesCompat} to be used during the playback of the media.
     * <p>
     * You must call this method in {@link #PLAYER_STATE_IDLE} in order for the audio attributes to
     * become effective thereafter. Otherwise, the call would be ignored and
     * {@link SessionPlayer.PlayerResult} would be returned with
     * {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE}.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @param attributes non-null <code>AudioAttributes</code>.
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setAudioAttributes(
            @NonNull final AudioAttributesCompat attributes) {
        if (attributes == null) {
            throw new NullPointerException("attr shouldn't be null");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.setAudioAttributes(attributes);
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
    @PlayerState
    public int getPlayerState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    @Override
    public long getCurrentPosition() {
        synchronized (mStateLock) {
            if (mClosed) {
                return UNKNOWN_TIME;
            }
        }
        try {
            final long pos = mPlayer.getCurrentPosition();
            if (pos >= 0) {
                return pos;
            }
        } catch (IllegalStateException e) {
            // fall-through.
        }
        return UNKNOWN_TIME;
    }

    @Override
    public long getDuration() {
        synchronized (mStateLock) {
            if (mClosed) {
                return UNKNOWN_TIME;
            }
        }
        try {
            final long duration = mPlayer.getDuration();
            if (duration >= 0) {
                return duration;
            }
        } catch (IllegalStateException e) {
            // fall-through.
        }
        return UNKNOWN_TIME;
    }

    @Override
    public long getBufferedPosition() {
        synchronized (mStateLock) {
            if (mClosed) {
                return UNKNOWN_TIME;
            }
        }
        try {
            final long pos = mPlayer.getBufferedPosition();
            if (pos >= 0) {
                return pos;
            }
        } catch (IllegalStateException e) {
            // fall-through.
        }
        return UNKNOWN_TIME;
    }

    @Override
    @BuffState
    public int getBufferingState() {
        synchronized (mStateLock) {
            if (mClosed) {
                return BUFFERING_STATE_UNKNOWN;
            }
        }
        Integer buffState;
        synchronized (mStateLock) {
            buffState = mMediaItemToBuffState.get(mPlayer.getCurrentMediaItem());
        }
        return buffState == null ? BUFFERING_STATE_UNKNOWN : buffState;
    }

    @Override
    @FloatRange(from = 0.0f, to = Float.MAX_VALUE, fromInclusive = false)
    public float getPlaybackSpeed() {
        synchronized (mStateLock) {
            if (mClosed) {
                return 1.0f;
            }
        }
        try {
            return mPlayer.getPlaybackParams().getSpeed();
        } catch (IllegalStateException e) {
            return 1.0f;
        }
    }

    @Override
    @Nullable
    public AudioAttributesCompat getAudioAttributes() {
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
        try {
            return mPlayer.getAudioAttributes();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Sets a {@link MediaItem} for playback. Use this or {@link #setPlaylist} to specify which
     * items to play. If you want to change current item in the playlist, use one of
     * {@link #skipToPlaylistItem}, {@link #skipToNextPlaylistItem}, or
     * {@link #skipToPreviousPlaylistItem} instead of this method.
     * <p>
     * When this is called multiple times in any states other than {@link #PLAYER_STATE_ERROR}, it
     * would override previous {@link #setMediaItem} or {@link #setPlaylist} calls.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with {@code item} set.
     *
     * @param item the descriptor of media item you want to play
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see #setPlaylist
     * @see PlayerCallback#onPlaylistChanged
     * @see PlayerCallback#onCurrentMediaItemChanged
     * @throws IllegalArgumentException if the given item is {@code null}.
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setMediaItem(@NonNull final MediaItem item) {
        if (item == null) {
            throw new NullPointerException("item shouldn't be null");
        }
        if (item instanceof FileMediaItem) {
            if (((FileMediaItem) item).isClosed()) {
                throw new IllegalArgumentException("File descriptor is closed. " + item);
            }
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                synchronized (mPlaylistLock) {
                    mPlaylist.clear();
                    mPlaylistMetadata = null;
                    mShuffledList.clear();
                    mCurPlaylistItem = item;
                    mNextPlaylistItem = null;
                    mCurrentShuffleIdx = END_OF_PLAYLIST;
                }
                notifySessionPlayerCallback(
                        callback -> callback.onPlaylistChanged(MediaPlayer.this, null, null));
                futures.addAll(setMediaItemsInternal(item, null));
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Sets a list of {@link MediaItem} with metadata. Use this or {@link #setMediaItem} to specify
     * which items to play.
     * <p>
     * This can be called multiple times in any states other than {@link #PLAYER_STATE_ERROR}. This
     * would override previous {@link #setMediaItem} or {@link #setPlaylist} calls.
     * <p>
     * Ensure uniqueness of each {@link MediaItem} in the playlist so the session can uniquely
     * identity individual items. All {@link MediaItem}s wouldn't be {@code null} as well.
     * <p>
     * It's recommended to fill {@link MediaMetadata} in each {@link MediaItem} especially for the
     * duration information with the key {@link MediaMetadata#METADATA_KEY_DURATION}. Without the
     * duration information in the metadata, session will do extra work to get the duration and send
     * it to the controller.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the first media item
     * of the playlist when the command completed.
     *
     * @param list a list of {@link MediaItem} objects to set as a play list
     * @throws IllegalArgumentException if the given list is {@code null} or empty, or has
     *         duplicated media items.
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see #setMediaItem
     * @see PlayerCallback#onPlaylistChanged
     * @see PlayerCallback#onCurrentMediaItemChanged
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setPlaylist(
            @NonNull final List<MediaItem> list, @Nullable final MediaMetadata metadata) {
        if (list == null) {
            throw new NullPointerException("list shouldn't be null");
        } else if (list.isEmpty()) {
            throw new IllegalArgumentException("list shouldn't be empty");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        String errorString = null;
        for (MediaItem item : list) {
            if (item == null) {
                errorString = "list shouldn't contain null item";
                break;
            }
            if (item instanceof FileMediaItem) {
                if (((FileMediaItem) item).isClosed()) {
                    errorString = "File descriptor is closed. " + item;
                    break;
                }
            }
        }
        if (errorString != null) {
            // Close all the given FileMediaItems on error case.
            for (MediaItem item : list) {
                if (item instanceof FileMediaItem) {
                    ((FileMediaItem) item).increaseRefCount();
                    ((FileMediaItem) item).decreaseRefCount();
                }
            }
            throw new IllegalArgumentException(errorString);
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    mPlaylistMetadata = metadata;
                    mPlaylist.replaceAll(list);
                    applyShuffleModeLocked();
                    mCurrentShuffleIdx = 0;
                    updateAndGetCurrentNextItemIfNeededLocked();
                    curItem = mCurPlaylistItem;
                    nextItem = mNextPlaylistItem;
                }
                notifySessionPlayerCallback(callback -> {
                    callback.onPlaylistChanged(MediaPlayer.this, list, metadata);
                });
                if (curItem != null) {
                    return setMediaItemsInternal(curItem, nextItem);
                }
                return createFuturesForResultCode(RESULT_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Adds the media item to the playlist at the index. Index equals to or greater than
     * the current playlist size (e.g. {@link Integer#MAX_VALUE}) will add the item at the end of
     * the playlist.
     * <p>
     * If index is less than or equal to the current index of the playlist,
     * the current index of the playlist would be increased correspondingly.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with {@code item} added.
     *
     * @param index the index of the item you want to add in the playlist
     * @param item the media item you want to add
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> addPlaylistItem(
            final int index, @NonNull final MediaItem item) {
        if (item == null) {
            throw new NullPointerException("item shouldn't be null");
        }
        if (item instanceof FileMediaItem) {
            if (((FileMediaItem) item).isClosed()) {
                throw new IllegalArgumentException("File descriptor is closed. " + item);
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                Pair<MediaItem, MediaItem> updatedCurNextItem;
                synchronized (mPlaylistLock) {
                    if (mPlaylist.contains(item)) {
                        return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE, item);
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

                if (updatedCurNextItem == null || updatedCurNextItem.second == null) {
                    return createFuturesForResultCode(RESULT_SUCCESS);
                }
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                futures.add(setNextMediaItemInternal(updatedCurNextItem.second));
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Removes the media item from the playlist
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with {@code item} removed.
     *
     * @param index the index of the item you want to remove in the playlist
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> removePlaylistItem(@IntRange(from = 0) final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
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
                        return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
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
                if (curItem == null) {
                    resetInternal();
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(SessionPlayer.PlayerCallback callback) {
                            callback.onPlayerStateChanged(MediaPlayer.this, PLAYER_STATE_IDLE);
                        }
                    });
                    futures.add(createFutureForResultCode(RESULT_SUCCESS));
                } else if (updatedCurNextItem != null) {
                    if (updatedCurNextItem.first != null) {
                        futures.addAll(setMediaItemsInternal(curItem, nextItem));
                    } else if (updatedCurNextItem.second != null) {
                        futures.add(setNextMediaItemInternal(nextItem));
                    }
                } else {
                    futures.add(createFutureForResultCode(RESULT_SUCCESS));
                }
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Replaces the media item at index in the playlist. This can be also used to update metadata of
     * an item.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with {@code item} set.
     *
     * @param index the index of the item to replace in the playlist
     * @param item the new item
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> replacePlaylistItem(
            final int index, @NonNull final MediaItem item) {
        if (item == null) {
            throw new NullPointerException("item shouldn't be null");
        }
        if (item instanceof FileMediaItem) {
            if (((FileMediaItem) item).isClosed()) {
                throw new IllegalArgumentException("File descriptor is closed. " + item);
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                Pair<MediaItem, MediaItem> updatedCurNextItem = null;
                synchronized (mPlaylistLock) {
                    if (index >= mPlaylist.size() || mPlaylist.contains(item)) {
                        return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE, item);
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
                    futures.add(createFutureForResultCode(RESULT_SUCCESS));
                }
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Moves the media item at {@code fromIdx} to {@code toIdx} in the playlist.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with {@code item} set.
     *
     * @param fromIndex the media item's initial index in the playlist
     * @param toIndex the media item's target index in the playlist
     * @see PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> movePlaylistItem(final int fromIndex, final int toIndex) {
        if (fromIndex < 0 || toIndex < 0) {
            throw new IllegalArgumentException("indices shouldn't be negative");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                Pair<MediaItem, MediaItem> updatedCurNextItem = null;
                synchronized (mPlaylistLock) {
                    if (fromIndex >= mPlaylist.size() || toIndex >= mPlaylist.size()) {
                        return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
                    }

                    MediaItem item = mPlaylist.remove(fromIndex);
                    mPlaylist.add(toIndex, item);
                    if (mShuffleMode == SessionPlayer.SHUFFLE_MODE_NONE) {
                        mShuffledList.remove(fromIndex);
                        mShuffledList.add(toIndex, item);
                        if (item == mCurPlaylistItem) {
                            mCurrentShuffleIdx = toIndex;
                        }
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
                    futures.add(createFutureForResultCode(RESULT_SUCCESS));
                }
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Skips to the previous item in the playlist.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPreviousPlaylistItem() {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    if (mCurrentShuffleIdx < 0) {
                        return createFuturesForResultCode(RESULT_ERROR_INVALID_STATE);
                    }
                    int prevShuffleIdx = mCurrentShuffleIdx - 1;
                    if (prevShuffleIdx < 0) {
                        if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                            prevShuffleIdx = mShuffledList.size() - 1;
                        } else {
                            return createFuturesForResultCode(RESULT_ERROR_INVALID_STATE);
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

    /**
     * Skips to the next item in the playlist.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToNextPlaylistItem() {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    if (mCurrentShuffleIdx < 0) {
                        return createFuturesForResultCode(RESULT_ERROR_INVALID_STATE);
                    }
                    int nextShuffleIdx = mCurrentShuffleIdx + 1;
                    if (nextShuffleIdx >= mShuffledList.size()) {
                        if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                            nextShuffleIdx = 0;
                        } else {
                            return createFuturesForResultCode(RESULT_ERROR_INVALID_STATE);
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

    /**
     * Skips to the item in the playlist at the index.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @param index the index of the item you want to play in the playlist
     * @see PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPlaylistItem(@IntRange(from = 0) final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index shouldn't be negative");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }

        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                MediaItem curItem;
                MediaItem nextItem;
                synchronized (mPlaylistLock) {
                    if (index >= mPlaylist.size()) {
                        return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
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

    /**
     * Updates the playlist metadata while keeping the playlist as-is.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} swuld be returned with the current media
     * item when the command completed.
     *
     * @param metadata metadata of the playlist
     * @see PlayerCallback#onPlaylistMetadataChanged(SessionPlayer, MediaMetadata)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(
            @Nullable final MediaMetadata metadata) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
                return createFuturesForResultCode(RESULT_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Sets the repeat mode.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @param repeatMode repeat mode
     * @see #REPEAT_MODE_NONE
     * @see #REPEAT_MODE_ONE
     * @see #REPEAT_MODE_ALL
     * @see #REPEAT_MODE_GROUP
     * @see PlayerCallback#onRepeatModeChanged(SessionPlayer, int)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setRepeatMode(final int repeatMode) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                if (repeatMode < SessionPlayer.REPEAT_MODE_NONE
                        || repeatMode > SessionPlayer.REPEAT_MODE_GROUP) {
                    return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
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
                return createFuturesForResultCode(RESULT_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Sets the shuffle mode.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} would be returned with the current media
     * item when the command completed.
     *
     * @param shuffleMode the shuffle mode
     * @return a {@link ListenableFuture} representing the pending completion of the command
     * @see #SHUFFLE_MODE_NONE
     * @see #SHUFFLE_MODE_ALL
     * @see #SHUFFLE_MODE_GROUP
     * @see PlayerCallback#onShuffleModeChanged(SessionPlayer, int)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setShuffleMode(final int shuffleMode) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                if (shuffleMode < SessionPlayer.SHUFFLE_MODE_NONE
                        || shuffleMode > SessionPlayer.SHUFFLE_MODE_GROUP) {
                    return createFuturesForResultCode(RESULT_ERROR_BAD_VALUE);
                }

                boolean changed;
                synchronized (mPlaylistLock) {
                    changed = mShuffleMode != shuffleMode;
                    mShuffleMode = shuffleMode;
                    applyShuffleModeLocked();
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
                return createFuturesForResultCode(RESULT_SUCCESS);
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    @Override
    @Nullable
    public List<MediaItem> getPlaylist() {
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
        synchronized (mPlaylistLock) {
            return mPlaylist.isEmpty() ? null : new ArrayList<>(mPlaylist.getCollection());
        }
    }

    @Override
    @Nullable
    public MediaMetadata getPlaylistMetadata() {
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
        synchronized (mPlaylistLock) {
            return mPlaylistMetadata;
        }
    }

    @Override
    public int getRepeatMode() {
        synchronized (mStateLock) {
            if (mClosed) {
                return REPEAT_MODE_NONE;
            }
        }
        synchronized (mPlaylistLock) {
            return mRepeatMode;
        }
    }

    @Override
    public int getShuffleMode() {
        synchronized (mStateLock) {
            if (mClosed) {
                return SHUFFLE_MODE_NONE;
            }
        }
        synchronized (mPlaylistLock) {
            return mShuffleMode;
        }
    }

    @Override
    @Nullable
    public MediaItem getCurrentMediaItem() {
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
        return mPlayer.getCurrentMediaItem();
    }

    /**
     * Gets the index of current media item in playlist. This value would be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of current media item. Can be {@link #INVALID_ITEM_INDEX} when current
     *         media item is null or not in the playlist, and when the playlist hasn't been set.
     */
    @Override
    public int getCurrentMediaItemIndex() {
        synchronized (mStateLock) {
            if (mClosed) {
                return END_OF_PLAYLIST;
            }
        }
        synchronized (mPlaylistLock) {
            if (mCurrentShuffleIdx < 0) {
                return END_OF_PLAYLIST;
            }
            return mPlaylist.indexOf(mShuffledList.get(mCurrentShuffleIdx));
        }
    }

    /**
     * Gets the previous item index in the playlist. This value would be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of previous media item. Can be {@link #INVALID_ITEM_INDEX} only when
     *         previous media item does not exist or playlist hasn't been set.
     */
    @Override
    public int getPreviousMediaItemIndex() {
        synchronized (mStateLock) {
            if (mClosed) {
                return END_OF_PLAYLIST;
            }
        }
        synchronized (mPlaylistLock) {
            if (mCurrentShuffleIdx < 0) {
                return END_OF_PLAYLIST;
            }
            int prevShuffleIdx = mCurrentShuffleIdx - 1;
            if (prevShuffleIdx < 0) {
                if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                    return mPlaylist.indexOf(mShuffledList.get(mShuffledList.size() - 1));
                } else {
                    return END_OF_PLAYLIST;
                }
            }
            return mPlaylist.indexOf(mShuffledList.get(prevShuffleIdx));
        }
    }

    /**
     * Gets the next item index in the playlist. This value would be updated when
     * {@link PlayerCallback#onCurrentMediaItemChanged(SessionPlayer, MediaItem)} or
     * {@link PlayerCallback#onPlaylistChanged(SessionPlayer, List, MediaMetadata)} is called.
     *
     * @return the index of next media item. Can be {@link #INVALID_ITEM_INDEX} only when next media
     *         item does not exist or playlist hasn't been set.
     */
    @Override
    public int getNextMediaItemIndex() {
        synchronized (mStateLock) {
            if (mClosed) {
                return END_OF_PLAYLIST;
            }
        }
        synchronized (mPlaylistLock) {
            if (mCurrentShuffleIdx < 0) {
                return END_OF_PLAYLIST;
            }
            int nextShuffleIdx = mCurrentShuffleIdx + 1;
            if (nextShuffleIdx >= mShuffledList.size()) {
                if (mRepeatMode == REPEAT_MODE_ALL || mRepeatMode == REPEAT_MODE_GROUP) {
                    return mPlaylist.indexOf(mShuffledList.get(0));
                } else {
                    return END_OF_PLAYLIST;
                }
            }
            return mPlaylist.indexOf(mShuffledList.get(nextShuffleIdx));
        }
    }

    /**
     * Closes the player and relinquish underlying resources.
     */
    @Override
    public void close() {
        super.close();

        synchronized (mStateLock) {
            if (!mClosed) {
                mClosed = true;
                reset();
                mAudioFocusHandler.close();
                mPlayer.close();
                mExecutor.shutdown();
            }
        }
    }

    /**
     * Resets {@link MediaPlayer} to its uninitialized state if not closed. After calling
     * this method, you will have to initialize it again by setting the media item and
     * calling {@link #prepare()}.
     * <p> Note that if the player is closed, there is no way to reuse the instance.
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
            for (PendingFuture<? extends PlayerResult> f : mPendingFutures) {
                if (f.mExecuteCalled && !f.isDone() && !f.isCancelled()) {
                    f.cancel(true);
                }
            }
            mPendingFutures.clear();
        }
        resetInternal();
    }

    /**
     * Sets the {@link Surface} to be used as the sink for the video portion of
     * the media.
     * <p>
     * A null surface will result in only the audio track being played.
     * <p>
     * If the Surface sends frames to a {@link SurfaceTexture}, the timestamps
     * returned from {@link SurfaceTexture#getTimestamp()} will have an
     * unspecified zero point.  These timestamps cannot be directly compared
     * between different media sources, different instances of the same media
     * source, or multiple runs of the same program.  The timestamp is normally
     * monotonically increasing and is unaffected by time-of-day adjustments,
     * but it is reset when the position is set.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param surface The {@link Surface} to be used for the video portion of
     * the media.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     */
    @NonNull
    @Override
    public ListenableFuture<PlayerResult> setSurface(@Nullable final Surface surface) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
     * <p>
     * Note that this volume is specific to the player, and is separate from stream volume
     * used across the platform.
     * <p>
     * A value of 0.0f indicates muting, a value of 1.0f is the nominal unattenuated and unamplified
     * gain. See {@link #getMaxPlayerVolume()} for the volume range supported by this player.
     * <p>
     * The default player volume is 1.0f.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param volume a value between 0.0f and {@link #getMaxPlayerVolume()}.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setPlayerVolume(
            @FloatRange(from = 0, to = 1) final float volume) {
        if (volume < 0 || volume > 1) {
            throw new IllegalArgumentException("volume should be between 0.0 and 1.0");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
        synchronized (mStateLock) {
            if (mClosed) {
                return 1.0f;
            }
        }
        return mPlayer.getPlayerVolume();
    }

    /**
     * @return the maximum volume that can be used in {@link #setPlayerVolume(float)}.
     */
    public float getMaxPlayerVolume() {
        synchronized (mStateLock) {
            if (mClosed) {
                return 1.0f;
            }
        }
        return mPlayer.getMaxPlayerVolume();
    }

    /**
     * Returns the size of the video.
     *
     * @return the size of the video. The width and height of size could be 0 if there is no video
     * or the size has not been determined yet.
     * The {@link PlayerCallback} can be registered via {@link #registerPlayerCallback} to
     * receive a notification {@link PlayerCallback#onVideoSizeChanged} when the size
     * is available.
     */
    @Override
    @NonNull
    public VideoSize getVideoSize() {
        synchronized (mStateLock) {
            if (mClosed) {
                return new VideoSize(0, 0);
            }
        }
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
    @RestrictTo(LIBRARY)
    @RequiresApi(21)
    public PersistableBundle getMetrics() {
        return mPlayer.getMetrics();
    }

    /**
     * Sets playback params using {@link PlaybackParams}.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param params the playback params.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setPlaybackParams(@NonNull final PlaybackParams params) {
        if (params == null) {
            throw new NullPointerException("params shouldn't be null");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
        synchronized (mStateLock) {
            if (mClosed) {
                return DEFAULT_PLAYBACK_PARAMS;
            }
        }
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
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param position the offset in milliseconds from the start to seek to.
     * When seeking to the given time position, there is no guarantee that the media item
     * has a frame located at the position. When this happens, a frame nearby will be rendered.
     * The value should be in the range of start and end positions defined in {@link MediaItem}.
     * @param mode the mode indicating where exactly to seek to.
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     */
    @NonNull
    public ListenableFuture<PlayerResult> seekTo(final long position, @SeekMode final int mode) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture =
                new PendingFuture<PlayerResult>(mExecutor, true) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                int mp2SeekMode = sSeekModeMap.containsKey(mode)
                        ? sSeekModeMap.get(mode) : MediaPlayer2.SEEK_NEXT_SYNC;
                synchronized (mPendingCommands) {
                    Object token = mPlayer.seekTo(position, mp2SeekMode);
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
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
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
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @see AudioManager#generateAudioSessionId
     */
    @NonNull
    public ListenableFuture<PlayerResult> setAudioSessionId(final int sessionId) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
     * @return the audio session ID. See {@link #setAudioSessionId(int)}. Note that the audio
     *     session ID is 0 if a problem occurred when the MediaPlayer was constructed or it is
     *     closed.
     */
    public int getAudioSessionId() {
        synchronized (mStateLock) {
            if (mClosed) {
                return 0;
            }
        }
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
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     */
    @NonNull
    public ListenableFuture<PlayerResult> attachAuxEffect(final int effectId) {
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
     * logarithmically: the gain applied by audio framework ranges from -72dB to 0dB, so an
     * appropriate conversion from linear UI input x to level is: x == 0 -&gt; level = 0, 0 &lt; x
     * &lt;= R -&gt; level = 10^(72*(x-R)/20/R)
     * <p>
     * On success, a {@link SessionPlayer.PlayerResult} is returned with
     * the current media item when the command completed.
     *
     * @param level send level scalar
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command
     * completed.
     */
    @NonNull
    public ListenableFuture<PlayerResult> setAuxEffectSendLevel(
            @FloatRange(from = 0, to = 1) final float level) {
        if (level < 0 || level > 1) {
            // Returns ListenableFuture instead of throwing exception, not to newly throw an
            // exception in existing code.
            return createFutureForResultCode(RESULT_ERROR_BAD_VALUE);
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
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
     * Gets the full list of selected and unselected tracks that the media contains. The order of
     * the list is irrelevant as different players expose tracks in different ways, but the tracks
     * will generally be ordered based on track type.
     *
     * @return list of tracks. The total number of tracks is the size of the list. If empty,
     *         an empty list would be returned.
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     */
    @Override
    @NonNull
    public List<SessionPlayer.TrackInfo> getTracks() {
        synchronized (mStateLock) {
            if (mClosed) {
                return Collections.emptyList();
            }
        }
        return mPlayer.getTracks();
    }


    /**
     * @deprecated Use {@link #getTracks()} instead.
     */
    @Deprecated
    @NonNull
    public List<TrackInfo> getTrackInfo() {
        List<SessionPlayer.TrackInfo> infoInternals = getTracks();
        List<TrackInfo> infos = new ArrayList<>();
        for (SessionPlayer.TrackInfo infoInternal : infoInternals) {
            infos.add(new TrackInfo(infoInternal));
        }
        return infos;
    }

    /**
     * Returns the selected track for the given track type.
     * The return value is an element in the list returned by {@link #getTracks()}.
     *
     * @param trackType should be one of {@link TrackInfo#MEDIA_TRACK_TYPE_VIDEO},
     * {@link TrackInfo#MEDIA_TRACK_TYPE_AUDIO}, {@link TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE},
     * or {@link TrackInfo#MEDIA_TRACK_TYPE_METADATA}.
     * @return metadata corresponding to the  track currently selected for
     * playback; {@code null} is returned when there is no selected track for {@code trackType} or
     * when {@code trackType} is not one of audio or video.
     * @throws IllegalStateException if called after {@link #close()}
     *
     * @see #getTracks()
     */
    @Override
    @Nullable
    public TrackInfo getSelectedTrack(@TrackInfo.MediaTrackType int trackType) {
        synchronized (mStateLock) {
            if (mClosed) {
                return null;
            }
        }
        SessionPlayer.TrackInfo infoInternal = mPlayer.getSelectedTrack(trackType);
        return infoInternal == null ? null : new TrackInfo(infoInternal);
    }

    /**
     * Selects the {@link TrackInfo} for the current media item.
     * <p>
     * If the player is in invalid state,
     * {@link SessionPlayer.PlayerResult#RESULT_ERROR_INVALID_STATE} will be reported with
     * {@link SessionPlayer.PlayerResult}.
     * If a player is in <em>Playing</em> state, the selected track is presented immediately.
     * If a player is not in Playing state, it just marks the track to be played.
     * <p>
     * In any valid state, if it is called multiple times on the same type of track (ie. Video,
     * Audio), the most recent one will be chosen.
     * <p>
     * The first audio and video tracks are selected by default if available, even though
     * this method is not called.
     * <p>
     * Currently, tracks that return true for {@link TrackInfo#isSelectable()} can be selected via
     * this method.
     *
     * @param trackInfo metadata corresponding to the track to be selected. A {@code trackInfo}
     * object can be obtained from {@link #getTracks()}.
     *
     * @see #getTracks
     * @return a {@link ListenableFuture} which represents the pending completion of the command.
     * {@link SessionPlayer.PlayerResult} will be delivered when the command completed.
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> selectTrack(
            @NonNull final SessionPlayer.TrackInfo trackInfo) {
        if (trackInfo == null) {
            throw new NullPointerException("trackInfo shouldn't be null");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.selectTrack(trackInfo.getId());
                    addPendingCommandWithTrackInfoLocked(MediaPlayer2.CALL_COMPLETED_SELECT_TRACK,
                            future, trackInfo, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * @deprecated Use {@link #selectTrack(SessionPlayer.TrackInfo)} instead.
     */
    @Deprecated
    @NonNull
    public ListenableFuture<PlayerResult> selectTrack(@NonNull final TrackInfo trackInfo) {
        return selectTrack((SessionPlayer.TrackInfo) trackInfo);
    }

    /**
     * Deselects the {@link TrackInfo} for the current media item.
     * <p>
     * The track must be a subtitle track, and no audio or video tracks can be deselected.
     * <p>
     * Note: {@link #getSelectedTrack(int)} returns the currently selected track per track type that
     * can be deselected, but the list may be invalidated when
     * {@link PlayerCallback#onTracksChanged(SessionPlayer, List)} is called.
     *
     * @param trackInfo the track to be selected
     * @return a {@link ListenableFuture} which represents the pending completion of the command
     * @see TrackInfo#MEDIA_TRACK_TYPE_VIDEO
     * @see TrackInfo#MEDIA_TRACK_TYPE_AUDIO
     * @see TrackInfo#MEDIA_TRACK_TYPE_SUBTITLE
     * @see TrackInfo#MEDIA_TRACK_TYPE_METADATA
     * @see PlayerCallback#onTrackDeselected(SessionPlayer, TrackInfo)
     */
    @Override
    @NonNull
    public ListenableFuture<PlayerResult> deselectTrack(
            @NonNull SessionPlayer.TrackInfo trackInfo) {
        if (trackInfo == null) {
            throw new NullPointerException("trackInfo shouldn't be null");
        }
        synchronized (mStateLock) {
            if (mClosed) {
                return createFutureForClosed();
            }
        }
        PendingFuture<PlayerResult> pendingFuture = new PendingFuture<PlayerResult>(mExecutor) {
            @Override
            List<ResolvableFuture<PlayerResult>> onExecute() {
                ArrayList<ResolvableFuture<PlayerResult>> futures = new ArrayList<>();
                ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
                synchronized (mPendingCommands) {
                    Object token = mPlayer.deselectTrack(trackInfo.getId());
                    addPendingCommandWithTrackInfoLocked(MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK,
                            future, trackInfo, token);
                }
                futures.add(future);
                return futures;
            }
        };
        addPendingFuture(pendingFuture);
        return pendingFuture;
    }

    /**
     * Register {@link PlayerCallback} to listen changes.
     *
     * @param executor a callback Executor
     * @param callback a PlayerCallback
     * @throws IllegalArgumentException if executor or callback is {@code null}.
     */
    public void registerPlayerCallback(
            @NonNull /*@CallbackExecutor*/ Executor executor,
            @NonNull PlayerCallback callback) {
        super.registerPlayerCallback(executor, callback);
    }

    /**
     * Unregister the previously registered {@link PlayerCallback}.
     *
     * @param callback the callback to be removed
     * @throws IllegalArgumentException if the callback is {@code null}.
     */
    public void unregisterPlayerCallback(@NonNull PlayerCallback callback) {
        super.unregisterPlayerCallback(callback);
    }

    /**
     * Retrieves the DRM Info associated with the current media item.
     *
     * @throws IllegalStateException if called before being prepared
     * @hide
     */
    @Nullable
    @RestrictTo(LIBRARY)
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
     * {@link DrmResult} will be delivered when the command completed.
     * @hide
     */
    @RestrictTo(LIBRARY)
    // This is an asynchronous call.
    @NonNull
    public ListenableFuture<DrmResult> prepareDrm(@NonNull final UUID uuid) {
        if (uuid == null) {
            throw new NullPointerException("uuid shouldn't be null");
        }
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
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
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
     * <p>
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
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public void restoreDrmKeys(@NonNull byte[] keySetId) throws NoDrmSchemeException {
        if (keySetId == null) {
            throw new NullPointerException("keySetId shouldn't be null");
        }
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
    @RestrictTo(LIBRARY)
    @NonNull
    public String getDrmPropertyString(@NonNull String propertyName) throws NoDrmSchemeException {
        if (propertyName == null) {
            throw new NullPointerException("propertyName shouldn't be null");
        }
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
    @RestrictTo(LIBRARY)
    public void setDrmPropertyString(@NonNull String propertyName, @NonNull String value)
            throws NoDrmSchemeException {
        if (propertyName == null) {
            throw new NullPointerException("propertyName shouldn't be null");
        }
        if (value == null) {
            throw new NullPointerException("value shouldn't be null");
        }
        try {
            mPlayer.setDrmPropertyString(propertyName, value);
        } catch (MediaPlayer2.NoDrmSchemeException e) {
            throw new NoDrmSchemeException(e.getMessage());
        }
    }

    /**
     * Register a callback to be invoked for configuration of the DRM object before
     * the session is created.
     * <p>
     * The callback will be invoked synchronously during the execution
     * of {@link #prepareDrm(UUID uuid)}.
     *
     * @param listener the callback that will be run
     * @hide
     */
    @RestrictTo(LIBRARY)
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
        synchronized (mStateLock) {
            if (mClosed) {
                return;
            }
        }
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
        synchronized (mStateLock) {
            if (mClosed) {
                return;
            }
        }
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
        if (curItem == null) {
            throw new NullPointerException("curItem shouldn't be null");
        }
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void resetInternal() {
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
    ResolvableFuture<PlayerResult> createFutureForClosed() {
        ResolvableFuture<PlayerResult> future = ResolvableFuture.create();
        future.set(new PlayerResult(RESULT_ERROR_INVALID_STATE, null));
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
        mShuffledList.addAll(mPlaylist.getCollection());
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
        if (mCurrentShuffleIdx < 0 || mPlaylist.isEmpty()) {
            if (mCurPlaylistItem == null && mNextPlaylistItem == null) {
                return null;
            }
            mCurPlaylistItem = null;
            mNextPlaylistItem = null;
            return new Pair<>(null, null);
        }
        if (!ObjectsCompat.equals(mCurPlaylistItem, mShuffledList.get(mCurrentShuffleIdx))) {
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
        } else if (!ObjectsCompat.equals(mNextPlaylistItem, mShuffledList.get(nextShuffleIdx))) {
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
            Log.w(TAG, "Call type does not match. expected:" + expected.mCallType
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
                    final long pos = getCurrentPosition();
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(
                                SessionPlayer.PlayerCallback callback) {
                            callback.onSeekCompleted(MediaPlayer.this, pos);
                        }
                    });
                    break;
                case MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE:
                case MediaPlayer2.CALL_COMPLETED_SKIP_TO_NEXT:
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
                case MediaPlayer2.CALL_COMPLETED_SELECT_TRACK:
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(SessionPlayer.PlayerCallback callback) {
                            callback.onTrackSelected(MediaPlayer.this, expected.mTrackInfo);
                        }
                    });
                    break;
                case MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK:
                    notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                        @Override
                        public void callCallback(SessionPlayer.PlayerCallback callback) {
                            callback.onTrackDeselected(MediaPlayer.this, expected.mTrackInfo);
                        }
                    });
                    break;
            }
        }
        if (what != MediaPlayer2.CALL_COMPLETED_PREPARE_DRM) {
            Integer resultCode = sResultCodeMap.containsKey(status)
                    ? sResultCodeMap.get(status) : RESULT_ERROR_UNKNOWN;
            expected.setResult(new PlayerResult(resultCode, item));
        } else {
            Integer resultCode = sPrepareDrmStatusMap.containsKey(status)
                    ? sPrepareDrmStatusMap.get(status) : DrmResult.RESULT_ERROR_PREPARATION_ERROR;
            expected.setResult(new DrmResult(resultCode, item));
        }
        executePendingFutures();
    }

    private void executePendingFutures() {
        synchronized (mPendingFutures) {
            Iterator<PendingFuture<? extends PlayerResult>> it = mPendingFutures.iterator();
            while (it.hasNext()) {
                PendingFuture<? extends PlayerResult> f = it.next();
                if (f.isCancelled() || f.execute()) {
                    mPendingFutures.removeFirst();
                } else {
                    break;
                }
            }
            // Execute skip futures earlier for making them be skipped.
            while (it.hasNext()) {
                PendingFuture<? extends PlayerResult> f = it.next();
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
            MediaItem currentItem = getCurrentMediaItem();
            if (currentItem != null && currentItem == item) {
                final androidx.media2.common.VideoSize commonSize =
                        new androidx.media2.common.VideoSize(width, height);
                notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(SessionPlayer.PlayerCallback callback) {
                        callback.onVideoSizeChanged(MediaPlayer.this, commonSize);
                    }
                });
            }
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
                case MediaPlayer2.MEDIA_INFO_DATA_SOURCE_START:
                    boolean shouldNotifyCurrentMediaItemChanged;
                    MediaItem nextPlaylistItem;
                    synchronized (mPlaylistLock) {
                        if (mCurPlaylistItem == item) {
                            // Playback is started for the media item that the MediaPlayer has set
                            // as the current media item via MediaPlayer2.setMediaItem() or
                            // MediaPlayer2.skipToNext(). In that case, the current media item is
                            // already notified in the MediaPlayer2.EventCallback#onCallCompleted(),
                            // so don't need to notify again.
                            shouldNotifyCurrentMediaItemChanged = false;
                            nextPlaylistItem = null;
                        } else {
                            // Playback is advanced to the next item by MediaPlayer2 itself after
                            // the playback of the mCurPlaylistItem is completed.
                            // In that case, update the mCurPlaylistItem and also notify about the
                            // current media item changes.
                            shouldNotifyCurrentMediaItemChanged = true;
                            mCurrentShuffleIdx = mShuffledList.indexOf(item);
                            updateAndGetCurrentNextItemIfNeededLocked();
                            nextPlaylistItem = mNextPlaylistItem;
                        }
                    }
                    if (shouldNotifyCurrentMediaItemChanged) {
                        notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                            @Override
                            public void callCallback(SessionPlayer.PlayerCallback callback) {
                                callback.onCurrentMediaItemChanged(MediaPlayer.this, item);
                            }
                        });
                        // If the playback is advanced to the next item by itself, then the next
                        // media item may be emptied. If so, sets the next media item so the
                        // playback continues.
                        if (nextPlaylistItem != null) {
                            PendingFuture<PlayerResult> pendingFuture =
                                    new PendingFuture<PlayerResult>(mExecutor) {
                                        @Override
                                        List<ResolvableFuture<PlayerResult>> onExecute() {
                                            ArrayList<ResolvableFuture<PlayerResult>> futures =
                                                    new ArrayList<>();
                                            futures.add(setNextMediaItemInternal(nextPlaylistItem));
                                            return futures;
                                        }
                                    };
                            addPendingFuture(pendingFuture);
                        }
                    }
                    break;
                case MediaPlayer2.MEDIA_INFO_DATA_SOURCE_LIST_END:
                    MediaItem nextItemToPlay;
                    synchronized (mPlaylistLock) {
                        mCurrentShuffleIdx = mShuffledList.indexOf(item);
                        nextItemToPlay = mNextPlaylistItem;
                    }
                    if (nextItemToPlay != null) {
                        // Although the MediaPlayer2's playback is completed, but there's still
                        // remaining items to play in the playlist. It happens if the MediaPlayer2's
                        // playback is completed before the MediaPlayer has set the next item to
                        // play.
                        // Forcefully call skipToNextPlaylistItem to resume playback.
                        ListenableFuture<PlayerResult> future = skipToNextPlaylistItem();
                        if (future == null) {
                            Log.e(TAG, "Cannot play next media item", new IllegalStateException());
                            setState(PLAYER_STATE_ERROR);
                        }
                    } else {
                        // The playback for the playlist is completed for real. Notify accordingly.
                        setState(PLAYER_STATE_PAUSED);
                        notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                            @Override
                            public void callCallback(SessionPlayer.PlayerCallback callback) {
                                callback.onPlaybackCompleted(MediaPlayer.this);
                            }
                        });
                    }
                    break;
            }
            if (sInfoCodeMap.containsKey(mp2What)) {
                final int what = sInfoCodeMap.get(mp2What);
                notifyMediaPlayerCallback(new MediaPlayerCallbackNotifier() {
                    @Override
                    public void callCallback(PlayerCallback callback) {
                        callback.onInfo(MediaPlayer.this, item, what, extra);
                    }
                });
            }
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
        public void onSubtitleData(@NonNull MediaPlayer2 mp, @NonNull final MediaItem item,
                @NonNull final SessionPlayer.TrackInfo track, @NonNull final SubtitleData data) {
            notifySessionPlayerCallback(new SessionPlayerCallbackNotifier() {
                @Override
                public void callCallback(SessionPlayer.PlayerCallback callback) {
                    callback.onSubtitleData(MediaPlayer.this, item, track, data);
                }
            });
        }

        @Override
        public void onTracksChanged(@NonNull MediaPlayer2 mp,
                @NonNull List<SessionPlayer.TrackInfo> tracks) {
            notifySessionPlayerCallback(callback -> callback.onTracksChanged(MediaPlayer.this,
                    tracks));
        }
    }

    /**
     * Interface definition for callbacks to be invoked when the player has the corresponding
     * events.
     */
    public abstract static class PlayerCallback extends SessionPlayer.PlayerCallback {
        /**
         * @deprecated Use
         * {@link #onVideoSizeChanged(SessionPlayer,androidx.media2.common.VideoSize)} instead.
         */
        @Deprecated
        public void onVideoSizeChanged(
                @NonNull MediaPlayer mp, @NonNull MediaItem item, @NonNull VideoSize size) { }

        /**
         * Called to indicate the video size
         * <p>
         * The video size (width and height) could be 0 if there was no video,
         * no display surface was set, or the value was not determined yet.
         *
         * @param player the player associated with this callback
         * @param size the size of the video
         */
        @Override
        public void onVideoSizeChanged(@NonNull SessionPlayer player,
                @NonNull androidx.media2.common.VideoSize size) {
            if (!(player instanceof MediaPlayer)) {
                throw new IllegalArgumentException("player must be MediaPlayer");
            }
            onVideoSizeChanged((MediaPlayer) player, player.getCurrentMediaItem(),
                    new VideoSize(size));
        }

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
         * @param extra an extra code, specific to the error. Typically implementation dependent.
         */
        public void onError(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @MediaError int what, int extra) { }

        /**
         * Called to indicate an info or a warning.
         *
         * @param mp the player the info pertains to.
         * @param item the MediaItem of this media item
         * @param what the type of info or warning.
         * @param extra an extra code, specific to the info. Typically implementation dependent.
         */
        public void onInfo(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @MediaInfo int what, int extra) { }

        /**
         * Called when a discontinuity in the normal progression of the media time is detected.
         * <p>
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
         * Called to indicate DRM info is available
         *
         * @param mp the {@code MediaPlayer2} associated with this callback
         * @param item the MediaItem of this media item
         * @param drmInfo DRM info of the source including PSSH, and subset
         *                of crypto schemes supported by this device
         * @hide
         */
        @RestrictTo(LIBRARY)
        public void onDrmInfo(@NonNull MediaPlayer mp,
                @NonNull MediaItem item, @NonNull DrmInfo drmInfo) { }
    }

    /**
     * Class for the player to return each audio/video/subtitle track's metadata.
     *
     * @see #getTracks
     */
    public static final class TrackInfo extends SessionPlayer.TrackInfo {
        TrackInfo(SessionPlayer.TrackInfo infoInternal) {
            super(infoInternal.getId(), infoInternal.getTrackType(), infoInternal.getFormat(),
                    (infoInternal.getTrackType() != MEDIA_TRACK_TYPE_VIDEO));
        }

        @Nullable
        @Override
        public MediaFormat getFormat() {
            if (getTrackType() == MEDIA_TRACK_TYPE_SUBTITLE) {
                return super.getFormat();
            }
            return null;
        }
    }

    /**
     * Encapsulates the DRM properties of the source.
     * @hide
     */
    @RestrictTo(LIBRARY)
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
    }

    /**
     * Interface definition of a callback to be invoked when the app
     * can do DRM configuration (get/set properties) before the session
     * is opened. This facilitates configuration of the properties, like
     * 'securityLevel', which has to be set after DRM scheme creation but
     * before the DRM session is opened.
     * <p>
     * The only allowed DRM calls in this listener are {@link #getDrmPropertyString}
     * and {@link #setDrmPropertyString}.
     * @hide
     */
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public static class NoDrmSchemeException extends Exception {
        public NoDrmSchemeException(@Nullable String detailMessage) {
            super(detailMessage);
        }
    }

    /**
     * Definitions for the metrics that are reported via the {@link #getMetrics} call.
     * @hide
     */
    @RestrictTo(LIBRARY)
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
    @RestrictTo(LIBRARY)
    public static class DrmResult extends PlayerResult {
        /**
         * The device required DRM provisioning but couldn't reach the provisioning server.
         */
        public static final int RESULT_ERROR_PROVISIONING_NETWORK_ERROR = -1001;

        /**
         * The device required DRM provisioning but the provisioning server denied the request.
         */
        public static final int RESULT_ERROR_PROVISIONING_SERVER_ERROR = -1002;

        /**
         * The DRM preparation has failed.
         */
        public static final int RESULT_ERROR_PREPARATION_ERROR = -1003;

        /**
         * The crypto scheme UUID that is not supported by the device.
         */
        public static final int RESULT_ERROR_UNSUPPORTED_SCHEME = -1004;

        /**
         * The hardware resources are not available, due to being in use.
         */
        public static final int RESULT_ERROR_RESOURCE_BUSY = -1005;

        /** @hide */
        @IntDef(flag = false, /*prefix = "PREPARE_DRM_STATUS",*/ value = {
                RESULT_SUCCESS,
                RESULT_ERROR_PROVISIONING_NETWORK_ERROR,
                RESULT_ERROR_PROVISIONING_SERVER_ERROR,
                RESULT_ERROR_PREPARATION_ERROR,
                RESULT_ERROR_UNSUPPORTED_SCHEME,
                RESULT_ERROR_RESOURCE_BUSY,
        })
        @Retention(RetentionPolicy.SOURCE)
        @RestrictTo(LIBRARY)
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

    /**
     * List for {@link MediaItem} which manages the resource life cycle of
     * {@link android.os.ParcelFileDescriptor} in {@link FileMediaItem}.
     */
    static class MediaItemList {
        private ArrayList<MediaItem> mList = new ArrayList<>();

        void add(int index, MediaItem item) {
            if (item instanceof FileMediaItem) {
                ((FileMediaItem) item).increaseRefCount();
            }
            mList.add(index, item);
        }

        boolean replaceAll(Collection<MediaItem> c) {
            for (MediaItem item : c) {
                if (item instanceof FileMediaItem) {
                    ((FileMediaItem) item).increaseRefCount();
                }
            }
            clear();
            return mList.addAll(c);
        }

        MediaItem remove(int index) {
            MediaItem item = mList.remove(index);
            if (item instanceof FileMediaItem) {
                ((FileMediaItem) item).decreaseRefCount();
            }
            return item;
        }

        MediaItem get(int index) {
            return mList.get(index);
        }

        MediaItem set(int index, MediaItem item) {
            if (item instanceof FileMediaItem) {
                ((FileMediaItem) item).increaseRefCount();
            }
            MediaItem removed = mList.set(index, item);
            if (removed instanceof FileMediaItem) {
                ((FileMediaItem) removed).decreaseRefCount();
            }
            return removed;
        }

        void clear() {
            for (MediaItem item : mList) {
                if (item instanceof FileMediaItem) {
                    ((FileMediaItem) item).decreaseRefCount();
                }
            }
            mList.clear();
        }

        int size() {
            return mList.size();
        }

        boolean contains(Object o) {
            return mList.contains(o);
        }

        int indexOf(Object o) {
            return mList.indexOf(o);
        }

        boolean isEmpty() {
            return mList.isEmpty();
        }

        Collection<MediaItem> getCollection() {
            return mList;
        }
    }
}
