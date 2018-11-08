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
import static androidx.media2.MediaPlayer2.MEDIA_ERROR_UNKNOWN;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.CallbackMediaItem;
import androidx.media2.FileMediaItem;
import androidx.media2.MediaItem;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaTimestamp;
import androidx.media2.PlaybackParams;
import androidx.media2.SubtitleData;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.DefaultLoadControl;
import androidx.media2.exoplayer.external.ExoPlaybackException;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.audio.AudioCapabilities;
import androidx.media2.exoplayer.external.audio.AudioListener;
import androidx.media2.exoplayer.external.audio.AudioProcessor;
import androidx.media2.exoplayer.external.audio.AuxEffectInfo;
import androidx.media2.exoplayer.external.audio.DefaultAudioSink;
import androidx.media2.exoplayer.external.source.ClippingMediaSource;
import androidx.media2.exoplayer.external.source.ConcatenatingMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.TrackSelectionArray;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoListener;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps an ExoPlayer instance and provides methods and notifies events like those in the
 * {@link MediaPlayer2} API. {@link #getLooper()} returns the looper on which all other method calls
 * must be made.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ final class ExoPlayerWrapper {

    private static final String TAG = "ExoPlayerWrapper";

    /** Listener for player wrapper events. */
    public interface Listener {

        /** Called when the player is prepared. */
        void onPrepared(MediaItem mediaItem);

        /** Called when metadata (e.g., the set of available tracks) changes. */
        void onMetadataChanged(MediaItem mediaItem);

        /** Called when a seek request has completed. */
        void onSeekCompleted(long positionMs);

        /** Called when the player rebuffers. */
        void onBufferingStarted(MediaItem mediaItem);

        /** Called when the player becomes ready again after rebuffering. */
        void onBufferingEnded(MediaItem mediaItem);

        /** Called when video rendering of the specified media item has started. */
        void onVideoRenderingStart(MediaItem mediaItem);

        /** Called when the video size of the specified media item has changed. */
        void onVideoSizeChanged(MediaItem mediaItem, int width, int height);

        /** Called when subtitle data is handled. */
        void onSubtitleData(MediaItem mediaItem, SubtitleData subtitleData);

        /** Called when playback transitions to the next media item. */
        void onMediaItemStartedAsNext(MediaItem mediaItem);

        /** Called when playback of a media item ends. */
        void onMediaItemEnded(MediaItem mediaItem);

        /** Called when playback of the specified item loops back to its start. */
        void onLoop(MediaItem mediaItem);

        /** Called when a change in the progression of media time is detected. */
        void onMediaTimeDiscontinuity(MediaItem mediaItem, MediaTimestamp mediaTimestamp);

        /** Called when playback of the item list has ended. */
        void onPlaybackEnded(MediaItem mediaItem);

        /** Called when the player encounters an error. */
        void onError(MediaItem mediaItem, int what);

    }

    private static final String USER_AGENT_NAME = "MediaPlayer2";

    private final Context mContext;
    private final Listener mListener;
    private final Looper mLooper;

    private SimpleExoPlayer mPlayer;
    private DefaultAudioSink mAudioSink;
    private TrackSelector mTrackSelector;
    private MediaItemQueue mMediaItemQueue;

    private boolean mHasAudioAttributes;
    private int mAudioSessionId;
    private int mAuxEffectId;
    private float mAuxEffectSendLevel;
    private boolean mPrepared;
    private boolean mNewlyPrepared;
    private boolean mRebuffering;
    private boolean mPendingSeek;
    private int mVideoWidth;
    private int mVideoHeight;
    private PlaybackParams mPlaybackParams;

    /**
     * Creates a new ExoPlayer wrapper.
     *
     * @param context The context for accessing system components.
     * @param listener A listener for player wrapper events.
     * @param looper The looper that will be used for player events.
     */
    ExoPlayerWrapper(Context context, Listener listener, Looper looper) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mLooper = looper;
    }

    public Looper getLooper() {
        return mLooper;
    }

    public void setMediaItem(MediaItem mediaItem) {
        mMediaItemQueue.setMediaItem(Preconditions.checkNotNull(mediaItem));
    }

    public MediaItem getCurrentMediaItem() {
        return mMediaItemQueue.getCurrentMediaItem();
    }

    public void prepare() {
        Preconditions.checkState(!mPrepared);
        mMediaItemQueue.preparePlayer();
    }

    public void play() {
        mNewlyPrepared = false;
        if (mPlayer.getPlaybackState() == Player.STATE_ENDED) {
            mPlayer.seekTo(0);
        }
        mPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        mNewlyPrepared = false;
        mPlayer.setPlayWhenReady(false);
    }

    public void seekTo(long position, @MediaPlayer2.SeekMode int mode) {
        mPlayer.setSeekParameters(ExoPlayerUtils.getSeekParameters(mode));
        MediaItem mediaItem = mMediaItemQueue.getCurrentMediaItem();
        if (mediaItem != null) {
            Preconditions.checkArgument(
                    mediaItem.getStartPosition() <= position
                            && mediaItem.getEndPosition() >= position,
                    "Requested seek position is out of range : " + position);
            position -= mediaItem.getStartPosition();
        }
        mPlayer.seekTo(position);
    }

    public long getCurrentPosition() {
        long position = mPlayer.getCurrentPosition();
        MediaItem mediaItem = mMediaItemQueue.getCurrentMediaItem();
        if (mediaItem != null) {
            position += mediaItem.getStartPosition();
        }
        return position;
    }

    public long getDuration() {
        long duration = mMediaItemQueue.getCurrentMediaItemDuration();
        return duration == C.TIME_UNSET ? -1 : duration;
    }

    public long getBufferedPosition() {
        long position = mPlayer.getBufferedPosition();
        MediaItem mediaItem = mMediaItemQueue.getCurrentMediaItem();
        if (mediaItem != null) {
            position += mediaItem.getStartPosition();
        }
        return position;
    }

    public @MediaPlayer2.MediaPlayer2State int getState() {
        if (hasError()) {
            return MediaPlayer2.PLAYER_STATE_ERROR;
        }
        if (mNewlyPrepared) {
            return MediaPlayer2.PLAYER_STATE_PREPARED;
        }
        int state = mPlayer.getPlaybackState();
        boolean playWhenReady = mPlayer.getPlayWhenReady();
        // TODO(b/80232248): Return PLAYER_STATE_PREPARED before playback when we have track
        // groups.
        switch (state) {
            case Player.STATE_IDLE:
                return MediaPlayer2.PLAYER_STATE_IDLE;
            case Player.STATE_ENDED:
            case Player.STATE_BUFFERING:
                return MediaPlayer2.PLAYER_STATE_PAUSED;
            case Player.STATE_READY:
                return playWhenReady ? MediaPlayer2.PLAYER_STATE_PLAYING
                        : MediaPlayer2.PLAYER_STATE_PAUSED;
            default:
                throw new IllegalStateException();
        }
    }

    public void loopCurrent(boolean loop) {
        mPlayer.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    public void skipToNext() {
        mMediaItemQueue.skipToNext();
    }

    public void setNextMediaItem(MediaItem mediaItem) {
        Preconditions.checkState(!mMediaItemQueue.isEmpty());
        mMediaItemQueue.setNextMediaItems(Collections.singletonList(mediaItem));
    }

    public void setNextMediaItems(List<MediaItem> mediaItems) {
        Preconditions.checkState(!mMediaItemQueue.isEmpty());
        mMediaItemQueue.setNextMediaItems(Preconditions.checkNotNull(mediaItems));
    }

    public void setAudioAttributes(AudioAttributesCompat audioAttributes) {
        mHasAudioAttributes = true;
        mPlayer.setAudioAttributes(ExoPlayerUtils.getAudioAttributes(audioAttributes));
        // Reset the audio session ID, as it gets cleared by setting audio attributes.
        if (mAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            mAudioSink.setAudioSessionId(mAudioSessionId);
        }
    }

    public AudioAttributesCompat getAudioAttributes() {
        return mHasAudioAttributes
                ? ExoPlayerUtils.getAudioAttributesCompat(mPlayer.getAudioAttributes()) : null;
    }

    public void setAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
        mAudioSink.setAudioSessionId(mAudioSessionId);
    }

    public int getAudioSessionId() {
        if (Build.VERSION.SDK_INT >= 21 && mAudioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            setAudioSessionId(C.generateAudioSessionIdV21(mContext));
        }
        return mAudioSessionId == C.AUDIO_SESSION_ID_UNSET ? 0 : mAudioSessionId;
    }

    public void attachAuxEffect(int auxEffectId) {
        mAuxEffectId = auxEffectId;
        mPlayer.setAuxEffectInfo(new AuxEffectInfo(auxEffectId, mAuxEffectSendLevel));
    }

    public void setAuxEffectSendLevel(float auxEffectSendLevel) {
        mAuxEffectSendLevel = auxEffectSendLevel;
        mPlayer.setAuxEffectInfo(new AuxEffectInfo(mAuxEffectId, auxEffectSendLevel));
    }

    public void setPlaybackParams(PlaybackParams playbackParams2) {
        // TODO(b/80232248): Decide how to handle fallback modes, which ExoPlayer doesn't support.
        mPlaybackParams = playbackParams2;
        mPlayer.setPlaybackParameters(ExoPlayerUtils.getPlaybackParameters(mPlaybackParams));
        mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
    }

    public PlaybackParams getPlaybackParams() {
        return mPlaybackParams;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public void setSurface(Surface surface) {
        mPlayer.setVideoSurface(surface);
    }

    public void setVolume(float volume) {
        mPlayer.setVolume(volume);
    }

    public float getVolume() {
        return mPlayer.getVolume();
    }

    public List<MediaPlayer2.TrackInfo> getTrackInfo() {
        return mTrackSelector.getTrackInfos();
    }

    public int getSelectedTrack(int trackType) {
        return mTrackSelector.getSelectedTrack(trackType);
    }

    public void selectTrack(int index) {
        mTrackSelector.selectTrack(index);
    }

    public void deselectTrack(int index) {
        mTrackSelector.deselectTrack(index);
    }

    @TargetApi(21)
    public PersistableBundle getMetricsV21() {
        TrackGroupArray trackGroupArray = mPlayer.getCurrentTrackGroups();
        long durationMs = mPlayer.getDuration();
        long playingTimeMs = mMediaItemQueue.getCurrentMediaItemPlayingTimeMs();
        @Nullable String primaryAudioMimeType = null;
        @Nullable String primaryVideoMimeType = null;
        for (int i = 0; i < trackGroupArray.length; i++) {
            TrackGroup trackGroup = trackGroupArray.get(i);
            String mimeType = trackGroup.getFormat(0).sampleMimeType;
            if (primaryVideoMimeType == null && MimeTypes.isVideo(mimeType)) {
                primaryVideoMimeType = mimeType;
            } else if (primaryAudioMimeType == null && MimeTypes.isAudio(mimeType)) {
                primaryAudioMimeType = mimeType;
            }
        }
        PersistableBundle bundle = new PersistableBundle();
        if (primaryVideoMimeType != null) {
            bundle.putString(MediaPlayer2.MetricsConstants.MIME_TYPE_VIDEO, primaryVideoMimeType);
        }
        if (primaryAudioMimeType != null) {
            bundle.putString(MediaPlayer2.MetricsConstants.MIME_TYPE_AUDIO, primaryAudioMimeType);
        }
        bundle.putLong(MediaPlayer2.MetricsConstants.DURATION,
                durationMs == C.TIME_UNSET ? -1 : durationMs);
        bundle.putLong(MediaPlayer2.MetricsConstants.PLAYING, playingTimeMs);
        return bundle;
    }

    public MediaTimestamp getTimestamp() {
        boolean isPlaying =
                mPlayer.getPlaybackState() == Player.STATE_READY && mPlayer.getPlayWhenReady();
        float speed = isPlaying ? mPlaybackParams.getSpeed() : 0f;
        return new MediaTimestamp(C.msToUs(getCurrentPosition()), System.nanoTime(), speed);
    }

    public void reset() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
            mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
            mPlayer.release();
            mMediaItemQueue.clear();
        }
        ComponentListener listener = new ComponentListener();
        mAudioSink = new DefaultAudioSink(
                AudioCapabilities.getCapabilities(mContext), new AudioProcessor[0]);
        TextRenderer textRenderer = new TextRenderer(listener);
        mTrackSelector = new TrackSelector(textRenderer);
        mPlayer = ExoPlayerFactory.newSimpleInstance(
                mContext,
                new RenderersFactory(mContext, mAudioSink, textRenderer),
                mTrackSelector.getPlayerTrackSelector(),
                new DefaultLoadControl(),
                /* drmSessionManager= */ null,
                mLooper);
        mMediaItemQueue = new MediaItemQueue(mContext, mPlayer, mListener);
        mPlayer.addListener(listener);
        mPlayer.addVideoListener(listener);
        mVideoWidth = 0;
        mVideoHeight = 0;
        mPrepared = false;
        mNewlyPrepared = false;
        mRebuffering = false;
        mPendingSeek = false;
        mHasAudioAttributes = false;
        mAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
        mAuxEffectId = AuxEffectInfo.NO_AUX_EFFECT_ID;
        mAuxEffectSendLevel = 0f;
        mPlaybackParams = new PlaybackParams.Builder()
                .setSpeed(1f)
                .setPitch(1f)
                .setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
                .build();
    }

    public void close() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mMediaItemQueue.clear();
        }
    }

    public boolean hasError() {
        return mPlayer.getPlaybackError() != null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleVideoSizeChanged(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        mListener.onVideoSizeChanged(mMediaItemQueue.getCurrentMediaItem(), width, height);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleRenderedFirstFrame() {
        mListener.onVideoRenderingStart(mMediaItemQueue.getCurrentMediaItem());
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handlePlayerStateChanged(boolean playWhenReady, int state) {
        mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());

        if (state == Player.STATE_READY && playWhenReady) {
            maybeUpdateTimerForPlaying();
        } else {
            maybeUpdateTimerForStopped();
        }

        switch (state) {
            case Player.STATE_BUFFERING:
                maybeNotifyBufferingEvents();
                break;
            case Player.STATE_READY:
                maybeNotifyReadyEvents();
                break;
            case Player.STATE_ENDED:
                mMediaItemQueue.onPlayerEnded();
                break;
            case Player.STATE_IDLE:
            default:
                // Do nothing.
                break;
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleTextRendererChannelAvailable(int type, int channel) {
        mTrackSelector.handleTextRendererChannelAvailable(type, channel);
        if (mTrackSelector.hasPendingMetadataUpdate()) {
            mListener.onMetadataChanged(getCurrentMediaItem());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handlePlayerTracksChanged() {
        mTrackSelector.handlePlayerTracksChanged(mPlayer);
        if (mTrackSelector.hasPendingMetadataUpdate()) {
            mListener.onMetadataChanged(getCurrentMediaItem());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleSeekProcessed() {
        mPendingSeek = true;
        if (mPlayer.getPlaybackState() == Player.STATE_READY) {
            // The player doesn't need to buffer to seek, so handle being ready now.
            maybeNotifyReadyEvents();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handlePositionDiscontinuity(@Player.DiscontinuityReason int reason) {
        mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
        mMediaItemQueue.onPositionDiscontinuity(
                reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handlePlayerError(ExoPlaybackException exception) {
        mListener.onError(getCurrentMediaItem(), ExoPlayerUtils.getError(exception));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleSubtitleData(byte[] data, long timeUs) {
        int trackIndex = mTrackSelector.getSelectedTrack(MEDIA_TRACK_TYPE_SUBTITLE);
        mListener.onSubtitleData(getCurrentMediaItem(),
                new SubtitleData(trackIndex, timeUs, /* durationUs= */ 0L, data));
    }

    private void maybeUpdateTimerForPlaying() {
        mMediaItemQueue.onPlaying();
    }

    private void maybeUpdateTimerForStopped() {
        mMediaItemQueue.onStopped();
    }

    private void maybeNotifyBufferingEvents() {
        if (mPrepared && !mRebuffering) {
            mRebuffering = true;
            mListener.onBufferingStarted(getCurrentMediaItem());
        }
    }

    private void maybeNotifyReadyEvents() {
        MediaItem mediaItem = mMediaItemQueue.getCurrentMediaItem();
        boolean prepareComplete = !mPrepared;
        boolean seekComplete = mPendingSeek;
        if (prepareComplete) {
            mPrepared = true;
            mNewlyPrepared = true;
            mMediaItemQueue.onPositionDiscontinuity(/* isPeriodTransition= */ false);
            // TODO(b/80232248): Trigger onInfo with MEDIA_INFO_PREPARED for any item in the data
            // source queue for which the duration is now known, even if this is not the initial
            // preparation.
            mListener.onPrepared(mediaItem);
        } else if (seekComplete) {
            // TODO(b/80232248): Suppress notification if this is an initial seek for a non-zero
            // start position.
            mPendingSeek = false;
            mListener.onSeekCompleted(getCurrentPosition());
        } else if (mRebuffering) {
            mRebuffering = false;
            mListener.onBufferingEnded(getCurrentMediaItem());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ComponentListener extends Player.DefaultEventListener
            implements VideoListener, AudioListener, TextRenderer.Output {

        // DefaultEventListener implementation.

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            handlePlayerStateChanged(playWhenReady, state);
        }

        @Override
        public void onTracksChanged(
                TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            handlePlayerTracksChanged();
        }

        @Override
        public void onSeekProcessed() {
            handleSeekProcessed();
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            handlePositionDiscontinuity(reason);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            handlePlayerError(error);
        }

        // VideoListener implementation.

        @Override
        public void onVideoSizeChanged(
                final int width,
                final int height,
                int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {
            handleVideoSizeChanged(width, height);
        }

        @Override
        public void onRenderedFirstFrame() {
            handleRenderedFirstFrame();
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {}

        // AudioListener implementation.

        @Override
        public void onAudioSessionId(int audioSessionId) {
            handleAudioSessionId(audioSessionId);
        }

        @Override
        public void onAudioAttributesChanged(AudioAttributes audioAttributes) {}

        @Override
        public void onVolumeChanged(float volume) {}

        // TextRenderer.Output implementation.

        @Override
        public void onCcData(byte[] data, long timeUs) {
            handleSubtitleData(data, timeUs);
        }

        @Override
        public void onChannelAvailable(int type, int channel) {
            handleTextRendererChannelAvailable(type, channel);
        }

    }

    private static final class MediaItemInfo {

        final MediaItem mMediaItem;
        @Nullable
        final DurationProvidingMediaSource mDurationProvidingMediaSource;
        @Nullable
        final FileDescriptor mFileDescriptor;

        MediaItemInfo(
                MediaItem mediaItem,
                @Nullable DurationProvidingMediaSource durationProvidingMediaSource,
                @Nullable FileDescriptor fileDescriptor) {
            mMediaItem = mediaItem;
            mDurationProvidingMediaSource = durationProvidingMediaSource;
            mFileDescriptor = fileDescriptor;
        }

    }

    private static final class FileDescriptorRegistry {

        private static final class Entry {
            public final Object mLock;

            public int mMediaItemCount;

            Entry() {
                mLock = new Object();
            }
        }

        private final Map<FileDescriptor, Entry> mEntries;

        FileDescriptorRegistry() {
            mEntries = new HashMap<>();
        }

        public Object registerMediaItemAndGetLock(FileDescriptor fileDescriptor) {
            if (!mEntries.containsKey(fileDescriptor)) {
                mEntries.put(fileDescriptor, new Entry());
            }
            Entry entry = Preconditions.checkNotNull(mEntries.get(fileDescriptor));
            entry.mMediaItemCount++;
            return entry.mLock;
        }

        public void unregisterMediaItem(FileDescriptor fileDescriptor) {
            Entry entry = Preconditions.checkNotNull(mEntries.get(fileDescriptor));
            if (--entry.mMediaItemCount == 0) {
                mEntries.remove(fileDescriptor);
            }
        }

    }

    private static final class MediaItemQueue {

        private final Listener mListener;
        private final SimpleExoPlayer mPlayer;
        private final DataSource.Factory mDataSourceFactory;
        private final ConcatenatingMediaSource mConcatenatingMediaSource;
        private final ArrayDeque<MediaItemInfo> mMediaItemInfos;
        private final FileDescriptorRegistry mFileDescriptorRegistry;

        private long mStartPlayingTimeNs;
        private long mCurrentMediaItemPlayingTimeUs;

        MediaItemQueue(Context context, SimpleExoPlayer player, Listener listener) {
            mPlayer = player;
            mListener = listener;
            String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
            mDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
            mConcatenatingMediaSource = new ConcatenatingMediaSource();
            mMediaItemInfos = new ArrayDeque<>();
            mFileDescriptorRegistry = new FileDescriptorRegistry();
            mStartPlayingTimeNs = -1;
        }

        public void clear() {
            while (!mMediaItemInfos.isEmpty()) {
                releaseMediaItem(mMediaItemInfos.remove());
            }
        }

        public boolean isEmpty() {
            return mConcatenatingMediaSource.getSize() == 0;
        }

        public void setMediaItem(MediaItem mediaItem) {
            clear();
            mConcatenatingMediaSource.clear();
            setNextMediaItems(Collections.singletonList(mediaItem));
        }

        public void setNextMediaItems(List<MediaItem> mediaItems) {
            int size = mConcatenatingMediaSource.getSize();
            if (size > 1) {
                mConcatenatingMediaSource.removeMediaSourceRange(
                        /* fromIndex= */ 1, /* toIndex= */ size);
                while (mMediaItemInfos.size() > 1) {
                    releaseMediaItem(mMediaItemInfos.removeLast());
                }
            }

            List<MediaSource> mediaSources = new ArrayList<>(mediaItems.size());
            for (MediaItem mediaItem : mediaItems) {
                if (mediaItem == null) {
                    mListener.onError(/* mediaItem= */ null, MEDIA_ERROR_UNKNOWN);
                    return;
                }
                try {
                    appendMediaItem(
                            mediaItem,
                            mMediaItemInfos,
                            mediaSources);
                } catch (IOException e) {
                    mListener.onError(mediaItem, MEDIA_ERROR_UNKNOWN);
                }
            }
            mConcatenatingMediaSource.addMediaSources(mediaSources);
        }

        public void preparePlayer() {
            mPlayer.prepare(mConcatenatingMediaSource);
        }

        @Nullable
        public MediaItem getCurrentMediaItem() {
            return mMediaItemInfos.isEmpty() ? null : mMediaItemInfos.peekFirst().mMediaItem;
        }

        public long getCurrentMediaItemDuration() {
            DurationProvidingMediaSource durationProvidingMediaSource =
                    mMediaItemInfos.peekFirst().mDurationProvidingMediaSource;
            if (durationProvidingMediaSource != null) {
                return durationProvidingMediaSource.getDurationMs();
            } else {
                return mPlayer.getDuration();
            }
        }

        public long getCurrentMediaItemPlayingTimeMs() {
            return C.usToMs(mCurrentMediaItemPlayingTimeUs);
        }

        public void skipToNext() {
            // TODO(b/68398926): Play the start position of the next media item.
            releaseMediaItem(mMediaItemInfos.removeFirst());
            mConcatenatingMediaSource.removeMediaSource(0);
        }

        public void onPlaying() {
            if (mStartPlayingTimeNs != -1) {
                return;
            }
            mStartPlayingTimeNs = System.nanoTime();
        }

        public void onStopped() {
            if (mStartPlayingTimeNs == -1) {
                return;
            }
            long nowNs = System.nanoTime();
            mCurrentMediaItemPlayingTimeUs += (nowNs - mStartPlayingTimeNs + 500) / 1000;
            mStartPlayingTimeNs = -1;
        }

        public void onPlayerEnded() {
            MediaItem mediaItem = getCurrentMediaItem();
            mListener.onMediaItemEnded(mediaItem);
            mListener.onPlaybackEnded(mediaItem);
        }

        public void onPositionDiscontinuity(boolean isPeriodTransition) {
            MediaItem currentMediaItem = getCurrentMediaItem();
            if (isPeriodTransition && mPlayer.getRepeatMode() != Player.REPEAT_MODE_OFF) {
                mListener.onLoop(currentMediaItem);
            }
            int windowIndex = mPlayer.getCurrentWindowIndex();
            if (windowIndex > 0) {
                // We're no longer playing the first item in the queue.
                if (isPeriodTransition) {
                    mListener.onMediaItemEnded(getCurrentMediaItem());
                }
                for (int i = 0; i < windowIndex; i++) {
                    releaseMediaItem(mMediaItemInfos.removeFirst());
                }
                if (isPeriodTransition) {
                    mListener.onMediaItemStartedAsNext(getCurrentMediaItem());
                }
                mConcatenatingMediaSource.removeMediaSourceRange(0, windowIndex);
                mCurrentMediaItemPlayingTimeUs = 0;
                mStartPlayingTimeNs = -1;
                if (mPlayer.getPlaybackState() == Player.STATE_READY) {
                    onPlaying();
                }
            }
        }

        /**
         * Appends a media source and associated information for the given media item to the
         * collections provided.
         */
        private void appendMediaItem(
                MediaItem mediaItem,
                Collection<MediaItemInfo> mediaItemInfos,
                Collection<MediaSource> mediaSources) throws IOException {
            DataSource.Factory dataSourceFactory = mDataSourceFactory;
            // Create a data source for reading from the file descriptor, if needed.
            FileDescriptor fileDescriptor = null;
            if (mediaItem instanceof FileMediaItem) {
                FileMediaItem fileMediaItem = (FileMediaItem) mediaItem;
                // TODO(b/68398926): Remove dup'ing the file descriptor once FileMediaItem does it.
                Object lock = mFileDescriptorRegistry.registerMediaItemAndGetLock(
                        fileMediaItem.getFileDescriptor());
                fileDescriptor = FileDescriptorUtil.dup(fileMediaItem.getFileDescriptor());
                long offset = fileMediaItem.getFileDescriptorOffset();
                long length = fileMediaItem.getFileDescriptorLength();
                dataSourceFactory =
                        FileDescriptorDataSource.getFactory(fileDescriptor, offset, length, lock);
            }

            // Create a source for the item.
            MediaSource mediaSource =
                    ExoPlayerUtils.createUnclippedMediaSource(dataSourceFactory, mediaItem);

            // Apply clipping if needed. Because ExoPlayer doesn't expose the unclipped duration, we
            // wrap the child source in an intermediate source that lets us access its duration.
            DurationProvidingMediaSource durationProvidingMediaSource = null;
            long startPosition = mediaItem.getStartPosition();
            long endPosition = mediaItem.getEndPosition();
            if (startPosition != 0L || endPosition != MediaItem.POSITION_UNKNOWN) {
                durationProvidingMediaSource = new DurationProvidingMediaSource(mediaSource);
                // Disable the initial discontinuity to give seamless transitions to clips.
                mediaSource = new ClippingMediaSource(
                        durationProvidingMediaSource,
                        C.msToUs(startPosition),
                        C.msToUs(endPosition),
                        /* enableInitialDiscontinuity= */ false,
                        /* allowDynamicClippingUpdates= */ false,
                        /* relativeToDefaultPosition= */ true);
            }

            mediaSources.add(mediaSource);
            mediaItemInfos.add(
                    new MediaItemInfo(mediaItem, durationProvidingMediaSource, fileDescriptor));
        }

        private void releaseMediaItem(MediaItemInfo mediaItemInfo) {
            MediaItem mediaItem = mediaItemInfo.mMediaItem;
            try {
                if (mediaItem instanceof FileMediaItem) {
                    FileDescriptorUtil.close(mediaItemInfo.mFileDescriptor);
                    // TODO(b/68398926): Remove separate file descriptors once FileMediaItem dup's.
                    FileDescriptor fileDescriptor =
                            ((FileMediaItem) mediaItem).getFileDescriptor();
                    mFileDescriptorRegistry.unregisterMediaItem(fileDescriptor);
                } else if (mediaItem instanceof CallbackMediaItem) {
                    ((CallbackMediaItem) mediaItemInfo.mMediaItem)
                            .getDataSourceCallback().close();
                }
            } catch (IOException e) {
                Log.w(TAG, "Error releasing media item " + mediaItem, e);
            }
        }

    }

}
