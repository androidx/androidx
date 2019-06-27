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

package androidx.media2.player.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.media2.player.MediaPlayer2.MEDIA_ERROR_UNKNOWN;
import static androidx.media2.player.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.CallbackMediaItem;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.UriMediaItem;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.DefaultLoadControl;
import androidx.media2.exoplayer.external.ExoPlaybackException;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.analytics.AnalyticsCollector;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.audio.AudioCapabilities;
import androidx.media2.exoplayer.external.audio.AudioListener;
import androidx.media2.exoplayer.external.audio.AudioProcessor;
import androidx.media2.exoplayer.external.audio.AuxEffectInfo;
import androidx.media2.exoplayer.external.audio.DefaultAudioSink;
import androidx.media2.exoplayer.external.decoder.DecoderCounters;
import androidx.media2.exoplayer.external.metadata.Metadata;
import androidx.media2.exoplayer.external.metadata.MetadataOutput;
import androidx.media2.exoplayer.external.source.ClippingMediaSource;
import androidx.media2.exoplayer.external.source.ConcatenatingMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.TrackSelectionArray;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultBandwidthMeter;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoRendererEventListener;
import androidx.media2.player.MediaPlayer2;
import androidx.media2.player.MediaTimestamp;
import androidx.media2.player.PlaybackParams;
import androidx.media2.player.TimedMetaData;

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
@RestrictTo(LIBRARY_GROUP_PREFIX)
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
        void onSeekCompleted();

        /** Called when the player rebuffers. */
        void onBufferingStarted(MediaItem mediaItem);

        /** Called when the player becomes ready again after rebuffering. */
        void onBufferingEnded(MediaItem mediaItem);

        /** Called periodically with the player's buffered position as a percentage. */
        void onBufferingUpdate(MediaItem mediaItem, int bufferingPercentage);

        /** Called when a sample of the available bandwidth is known. */
        void onBandwidthSample(MediaItem mediaItem2, int bitrateKbps);

        /** Called when video rendering of the specified media item has started. */
        void onVideoRenderingStart(MediaItem mediaItem);

        /** Called when the video size of the specified media item has changed. */
        void onVideoSizeChanged(MediaItem mediaItem, int width, int height);

        /** Called when subtitle data is handled. */
        void onSubtitleData(MediaItem mediaItem, int trackIndex, SubtitleData subtitleData);

        /** Called when timed metadata is handled. */
        void onTimedMetadata(MediaItem mediaItem, TimedMetaData timedMetaData);

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

    private static final int POLL_BUFFER_INTERVAL_MS = 1000;

    private final Context mContext;
    private final Listener mListener;
    private final Looper mLooper;
    private final Handler mHandler;
    private final DefaultBandwidthMeter mBandwidthMeter;
    private final Runnable mPollBufferRunnable;

    private SimpleExoPlayer mPlayer;
    private Handler mPlayerHandler;
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
        mHandler = new Handler(looper);
        // Use the same bandwidth meter for all playbacks via this wrapper.
        mBandwidthMeter = new DefaultBandwidthMeter();
        mPollBufferRunnable = new PollBufferRunnable();
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
        Preconditions.checkState(getState() != MediaPlayer2.PLAYER_STATE_IDLE);
        long position = Math.max(0, mPlayer.getCurrentPosition());
        MediaItem mediaItem = mMediaItemQueue.getCurrentMediaItem();
        if (mediaItem != null) {
            position += mediaItem.getStartPosition();
        }
        return position;
    }

    public long getDuration() {
        Preconditions.checkState(getState() != MediaPlayer2.PLAYER_STATE_IDLE);
        long duration = mMediaItemQueue.getCurrentMediaItemDuration();
        return duration == C.TIME_UNSET ? -1 : duration;
    }

    public long getBufferedPosition() {
        Preconditions.checkState(getState() != MediaPlayer2.PLAYER_STATE_IDLE);
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
        if (mMediaItemQueue.isEmpty()) {
            if (mediaItem instanceof FileMediaItem) {
                ((FileMediaItem) mediaItem).increaseRefCount();
                ((FileMediaItem) mediaItem).decreaseRefCount();
            }
            throw new IllegalStateException();
        }
        mMediaItemQueue.setNextMediaItems(Collections.singletonList(mediaItem));
    }

    public void setNextMediaItems(List<MediaItem> mediaItems) {
        if (mMediaItemQueue.isEmpty()) {
            for (MediaItem item: mediaItems) {
                ((FileMediaItem) item).increaseRefCount();
                ((FileMediaItem) item).decreaseRefCount();
            }
            throw new IllegalStateException();
        }
        mMediaItemQueue.setNextMediaItems(Preconditions.checkNotNull(mediaItems));
    }

    public void setAudioAttributes(AudioAttributesCompat audioAttributes) {
        mHasAudioAttributes = true;
        mPlayer.setAudioAttributes(ExoPlayerUtils.getAudioAttributes(audioAttributes));

        // Reset the audio session ID, as it gets cleared by setting audio attributes.
        if (mAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            updatePlayerAudioSessionId(mPlayerHandler, mAudioSink, mAudioSessionId);
        }
    }

    public AudioAttributesCompat getAudioAttributes() {
        return mHasAudioAttributes
                ? ExoPlayerUtils.getAudioAttributesCompat(mPlayer.getAudioAttributes()) : null;
    }

    public void setAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
        if (mPlayer != null) {
            updatePlayerAudioSessionId(mPlayerHandler, mAudioSink, mAudioSessionId);
        }
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
        if (getState() == MediaPlayer2.PLAYER_STATE_PLAYING) {
            mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
        }
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

    @RequiresApi(21)
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
        long positionUs = mPlayer.getPlaybackState() == Player.STATE_IDLE
                ? 0L : C.msToUs(getCurrentPosition());
        float speed = mPlayer.getPlaybackState() == Player.STATE_READY && mPlayer.getPlayWhenReady()
                ? mPlaybackParams.getSpeed() : 0f;
        return new MediaTimestamp(positionUs, System.nanoTime(), speed);
    }

    public void reset() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
            if (getState() != MediaPlayer2.PLAYER_STATE_IDLE) {
                mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
            }
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
                mBandwidthMeter,
                new AnalyticsCollector.Factory(),
                mLooper);
        mPlayerHandler = new Handler(mPlayer.getPlaybackLooper());
        mMediaItemQueue = new MediaItemQueue(mContext, mPlayer, mListener);
        mPlayer.addListener(listener);
        // TODO(b/80232248): Switch to AnalyticsListener once default methods work.
        mPlayer.setVideoDebugListener(listener);
        mPlayer.addMetadataOutput(listener);
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
            mHandler.removeCallbacks(mPollBufferRunnable);
            mPlayer.release();
            mPlayer = null;
            mMediaItemQueue.clear();
            mHasAudioAttributes = false;
        }
    }

    public boolean hasError() {
        return mPlayer.getPlaybackError() != null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        if (pixelWidthHeightRatio != 1f) {
            mVideoWidth = (int) (pixelWidthHeightRatio * width);
        } else {
            mVideoWidth = width;
        }
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

        if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
            mHandler.post(mPollBufferRunnable);
        } else {
            mHandler.removeCallbacks(mPollBufferRunnable);
        }

        switch (state) {
            case Player.STATE_BUFFERING:
                maybeNotifyBufferingEvents();
                break;
            case Player.STATE_READY:
                maybeNotifyReadyEvents();
                break;
            case Player.STATE_ENDED:
                maybeNotifyEndedEvents();
                break;
            case Player.STATE_IDLE:
                // Do nothing.
                break;
            default:
                throw new IllegalStateException();
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
        if (getCurrentMediaItem() == null) {
            mListener.onSeekCompleted();
            return;
        }
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
        mListener.onMediaTimeDiscontinuity(getCurrentMediaItem(), getTimestamp());
        mListener.onError(getCurrentMediaItem(), ExoPlayerUtils.getError(exception));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleSubtitleData(byte[] data, long timeUs) {
        int trackIndex = mTrackSelector.getSelectedTrack(MEDIA_TRACK_TYPE_SUBTITLE);
        final MediaItem currentMediaItem = getCurrentMediaItem();
        mListener.onSubtitleData(currentMediaItem, trackIndex,
                new SubtitleData(timeUs, /* durationUs= */ 0L, data));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleMetadata(Metadata metadata) {
        int length = metadata.length();
        for (int i = 0; i < length; i++) {
            ByteArrayFrame byteArrayFrame = (ByteArrayFrame) metadata.get(i);
            mListener.onTimedMetadata(
                    getCurrentMediaItem(),
                    new TimedMetaData(byteArrayFrame.mTimestamp, byteArrayFrame.mData));
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateBufferingAndScheduleNextPollBuffer() {
        if (mMediaItemQueue.getCurrentMediaItemIsRemote()) {
            mListener.onBufferingUpdate(getCurrentMediaItem(), mPlayer.getBufferedPercentage());
        }
        mHandler.removeCallbacks(mPollBufferRunnable);
        mHandler.postDelayed(mPollBufferRunnable, POLL_BUFFER_INTERVAL_MS);
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
            if (mMediaItemQueue.getCurrentMediaItemIsRemote()) {
                mListener.onBandwidthSample(
                        getCurrentMediaItem(), (int) (mBandwidthMeter.getBitrateEstimate() / 1000));
            }
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
            mListener.onSeekCompleted();
        }
        if (mRebuffering) {
            mRebuffering = false;
            if (mMediaItemQueue.getCurrentMediaItemIsRemote()) {
                mListener.onBandwidthSample(
                        getCurrentMediaItem(), (int) (mBandwidthMeter.getBitrateEstimate() / 1000));
            }
            mListener.onBufferingEnded(getCurrentMediaItem());
        }
    }

    private void maybeNotifyEndedEvents() {
        if (mPendingSeek) {
            // The seek operation resulted in transitioning to the ended state.
            mPendingSeek = false;
            mListener.onSeekCompleted();
        }
        if (mPlayer.getPlayWhenReady()) {
            mMediaItemQueue.onPlayerEnded();
            mPlayer.setPlayWhenReady(false);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ComponentListener extends Player.DefaultEventListener
            implements VideoRendererEventListener, AudioListener,
            TextRenderer.Output, MetadataOutput {

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

        // VideoRendererEventListener implementation.

        @Override
        public void onVideoSizeChanged(
                final int width,
                final int height,
                int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {
            handleVideoSizeChanged(width, height, pixelWidthHeightRatio);
        }

        @Override
        public void onVideoInputFormatChanged(Format format) {
            if (MimeTypes.isVideo(format.sampleMimeType)) {
                handleVideoSizeChanged(format.width, format.height, format.pixelWidthHeightRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame(@Nullable Surface surface) {
            handleRenderedFirstFrame();
        }

        @Override
        public void onVideoEnabled(DecoderCounters counters) {}

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs,
                long initializationDurationMs) {}

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {}

        @Override
        public void onVideoDisabled(DecoderCounters counters) {}

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

        // MetadataOutput implementation.

        @Override
        public void onMetadata(Metadata metadata) {
            handleMetadata(metadata);
        }

    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class PollBufferRunnable implements Runnable {
        @Override
        public void run() {
            updateBufferingAndScheduleNextPollBuffer();
        }
    }

    private static void updatePlayerAudioSessionId(
            Handler playerHandler,
            final DefaultAudioSink audioSink,
            final int audioSessionId) {
        // DefaultAudioSink is not thread-safe, so post the update to the playback thread.
        playerHandler.post(new Runnable() {
            @Override
            public void run() {
                audioSink.setAudioSessionId(audioSessionId);
            }
        });
    }

    private static final class MediaItemInfo {

        final MediaItem mMediaItem;
        @Nullable
        final DurationProvidingMediaSource mDurationProvidingMediaSource;
        final boolean mIsRemote;

        MediaItemInfo(
                MediaItem mediaItem,
                @Nullable DurationProvidingMediaSource durationProvidingMediaSource,
                boolean isRemote) {
            mMediaItem = mediaItem;
            mDurationProvidingMediaSource = durationProvidingMediaSource;
            mIsRemote = isRemote;
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

        private final Context mContext;
        private final Listener mListener;
        private final SimpleExoPlayer mPlayer;
        private final DataSource.Factory mDataSourceFactory;
        private final ConcatenatingMediaSource mConcatenatingMediaSource;
        private final ArrayDeque<MediaItemInfo> mMediaItemInfos;
        private final FileDescriptorRegistry mFileDescriptorRegistry;

        private long mStartPlayingTimeNs;
        private long mCurrentMediaItemPlayingTimeUs;

        MediaItemQueue(Context context, SimpleExoPlayer player, Listener listener) {
            mContext = context;
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
                appendMediaItem(
                        mediaItem,
                        mMediaItemInfos,
                        mediaSources);
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
            if (mMediaItemInfos.isEmpty()) {
                return C.TIME_UNSET;
            }
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

        public boolean getCurrentMediaItemIsRemote() {
            return !mMediaItemInfos.isEmpty() && mMediaItemInfos.peekFirst().mIsRemote;
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
                Collection<MediaSource> mediaSources) {
            DataSource.Factory dataSourceFactory = mDataSourceFactory;
            // Create a data source for reading from the file descriptor, if needed.
            if (mediaItem instanceof FileMediaItem) {
                FileMediaItem fileMediaItem = (FileMediaItem) mediaItem;
                fileMediaItem.increaseRefCount();
                FileDescriptor fileDescriptor =
                        fileMediaItem.getParcelFileDescriptor().getFileDescriptor();
                long offset = fileMediaItem.getFileDescriptorOffset();
                long length = fileMediaItem.getFileDescriptorLength();
                Object lock = mFileDescriptorRegistry.registerMediaItemAndGetLock(fileDescriptor);
                dataSourceFactory =
                        FileDescriptorDataSource.getFactory(fileDescriptor, offset, length, lock);
            }

            // Create a source for the item.
            MediaSource mediaSource = ExoPlayerUtils.createUnclippedMediaSource(
                    mContext, dataSourceFactory, mediaItem);

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

            boolean isRemote = mediaItem instanceof UriMediaItem
                    && !Util.isLocalFileUri(((UriMediaItem) mediaItem).getUri());
            mediaSources.add(mediaSource);
            mediaItemInfos.add(
                    new MediaItemInfo(mediaItem, durationProvidingMediaSource, isRemote));
        }

        private void releaseMediaItem(MediaItemInfo mediaItemInfo) {
            MediaItem mediaItem = mediaItemInfo.mMediaItem;
            try {
                if (mediaItem instanceof FileMediaItem) {
                    FileDescriptor fileDescriptor =
                            ((FileMediaItem) mediaItem).getParcelFileDescriptor()
                                    .getFileDescriptor();
                    mFileDescriptorRegistry.unregisterMediaItem(fileDescriptor);
                    ((FileMediaItem) mediaItem).decreaseRefCount();
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
