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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaPlayer2;
import androidx.media2.PlaybackParams2;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.DefaultLoadControl;
import androidx.media2.exoplayer.external.DefaultRenderersFactory;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.Renderer;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.audio.AudioCapabilities;
import androidx.media2.exoplayer.external.audio.AudioListener;
import androidx.media2.exoplayer.external.audio.AudioProcessor;
import androidx.media2.exoplayer.external.audio.AudioRendererEventListener;
import androidx.media2.exoplayer.external.audio.AuxEffectInfo;
import androidx.media2.exoplayer.external.audio.DefaultAudioSink;
import androidx.media2.exoplayer.external.audio.MediaCodecAudioRenderer;
import androidx.media2.exoplayer.external.drm.DrmSessionManager;
import androidx.media2.exoplayer.external.drm.FrameworkMediaCrypto;
import androidx.media2.exoplayer.external.mediacodec.MediaCodecSelector;
import androidx.media2.exoplayer.external.source.ConcatenatingMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoListener;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        void onPrepared(MediaItem2 mediaItem2);

        /** Called when a seek request has completed. */
        void onSeekCompleted(long positionMs);

        /** Called when video rendering of the specified media item has started. */
        void onVideoRenderingStart(MediaItem2 mediaItem2);

        /** Called when the video size of the specified media item has changed. */
        void onVideoSizeChanged(MediaItem2 mediaItem2, int width, int height);

        /** Called when playback transitions to the next media item. */
        void onMediaItem2StartedAsNext(MediaItem2 mediaItem2);

        /** Called when playback of a media item ends. */
        void onMediaItem2Ended(MediaItem2 mediaItem2);

        /** Called when playback of the specified item loops back to its start. */
        void onLoop(MediaItem2 mediaItem2);

        /** Called when playback of the item list has ended. */
        void onPlaybackEnded(MediaItem2 mediaItem2);

    }

    private static final String USER_AGENT_NAME = "MediaPlayer2";

    private final Context mContext;
    private final Listener mListener;
    private final Looper mLooper;

    private SimpleExoPlayer mPlayer;
    private DefaultAudioSink mAudioSink;
    private MediaItemQueue mMediaItemQueue;

    private int mAudioSessionId;
    private int mAuxEffectId;
    private float mAuxEffectSendLevel;
    private boolean mPrepared;
    private boolean mPendingSeek;
    private int mVideoWidth;
    private int mVideoHeight;
    private PlaybackParams2 mPlaybackParams2;

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

    public void setMediaItem(MediaItem2 mediaItem2) {
        mMediaItemQueue.setMediaItem2(Preconditions.checkNotNull(mediaItem2));
    }

    public MediaItem2 getCurrentMediaItem() {
        return mMediaItemQueue.getCurrentMediaItem();
    }

    public void prepare() {
        mMediaItemQueue.preparePlayer();
    }

    public void play() {
        mPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        mPlayer.setPlayWhenReady(false);
    }

    public void seekTo(long msec, @MediaPlayer2.SeekMode int mode) {
        mPlayer.setSeekParameters(ExoPlayerUtils.getSeekParameters(mode));
        mPlayer.seekTo(msec);
    }

    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    public long getDuration() {
        long duration = mPlayer.getDuration();
        return duration == C.TIME_UNSET ? -1 : duration;
    }

    public long getBufferedPosition() {
        return mPlayer.getBufferedPosition();
    }

    public @MediaPlayer2.MediaPlayer2State int getState() {
        int state = mPlayer.getPlaybackState();
        boolean playWhenReady = mPlayer.getPlayWhenReady();
        // TODO(b/80232248): Return PLAYER_STATE_PREPARED before playback when we have track
        // groups.
        switch (state) {
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
                return MediaPlayer2.PLAYER_STATE_IDLE;
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

    public void setNextMediaItem(MediaItem2 mediaItem2) {
        mMediaItemQueue.setNextMediaItem2s(Collections.singletonList(mediaItem2));
    }

    public void setNextMediaItems(List<MediaItem2> mediaItem2s) {
        mMediaItemQueue.setNextMediaItem2s(Preconditions.checkNotNull(mediaItem2s));
    }

    public void setAudioAttributes(AudioAttributesCompat audioAttributes) {
        mPlayer.setAudioAttributes(ExoPlayerUtils.getAudioAttributes(audioAttributes));
        // Reset the audio session ID, as it gets cleared by setting audio attributes.
        if (mAudioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            mAudioSink.setAudioSessionId(mAudioSessionId);
        }
    }

    public AudioAttributesCompat getAudioAttributes() {
        return ExoPlayerUtils.getAudioAttributesCompat(mPlayer.getAudioAttributes());
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

    public void setPlaybackParams(PlaybackParams2 playbackParams2) {
        // TODO(b/80232248): Decide how to handle fallback modes, which ExoPlayer doesn't support.
        mPlaybackParams2 = playbackParams2;
        mPlayer.setPlaybackParameters(ExoPlayerUtils.getPlaybackParameters(mPlaybackParams2));
    }

    public PlaybackParams2 getPlaybackParams() {
        return mPlaybackParams2;
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
        return ExoPlayerUtils.getTrackInfo(mPlayer.getCurrentTrackGroups());
    }

    @TargetApi(21)
    public PersistableBundle getMetricsV21() {
        TrackGroupArray trackGroupArray = mPlayer.getCurrentTrackGroups();
        long durationMs = mPlayer.getDuration();
        long playingTimeMs = mMediaItemQueue.getCurrentItemPlayingTimeMs();
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

    public void reset() {
        if (mPlayer != null) {
            mPlayer.release();
            mMediaItemQueue.clear();
        }
        mAudioSink = new DefaultAudioSink(
                AudioCapabilities.getCapabilities(mContext), new AudioProcessor[0]);
        mPlayer = ExoPlayerFactory.newSimpleInstance(
                mContext,
                new AudioSinkRenderersFactory(mContext, mAudioSink),
                new DefaultTrackSelector(),
                new DefaultLoadControl(),
                    /* drmSessionManager= */ null,
                mLooper);
        mMediaItemQueue = new MediaItemQueue(mContext, mPlayer, mListener);
        ComponentListener listener = new ComponentListener();
        mPlayer.addListener(listener);
        mPlayer.addVideoListener(listener);
        mVideoWidth = 0;
        mVideoHeight = 0;
        mPrepared = false;
        mAudioSessionId = C.AUDIO_SESSION_ID_UNSET;
        mAuxEffectId = AuxEffectInfo.NO_AUX_EFFECT_ID;
        mAuxEffectSendLevel = 0f;
        mPlaybackParams2 = new PlaybackParams2.Builder()
                .setSpeed(1f)
                .setPitch(1f)
                .setAudioFallbackMode(PlaybackParams2.AUDIO_FALLBACK_MODE_DEFAULT)
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
        if (state == Player.STATE_READY && playWhenReady) {
            maybeUpdateTimerForPlaying();
        } else {
            maybeUpdateTimerForStopped();
        }

        if (state == Player.STATE_ENDED) {
            mMediaItemQueue.onPlayerEnded();
        } else if (state == Player.STATE_READY) {
            maybeNotifyReadyEvents();
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
        mMediaItemQueue.onPositionDiscontinuity(
                reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    private void maybeUpdateTimerForPlaying() {
        mMediaItemQueue.onPlaying();
    }

    private void maybeUpdateTimerForStopped() {
        mMediaItemQueue.onStopped();
    }

    private void maybeNotifyReadyEvents() {
        MediaItem2 mediaItem2 = mMediaItemQueue.getCurrentMediaItem();
        boolean prepareComplete = !mPrepared;
        boolean seekComplete = mPendingSeek;
        if (prepareComplete) {
            mPrepared = true;
            mMediaItemQueue.onPositionDiscontinuity(/* isPeriodTransition= */ false);
            // TODO(b/80232248): Trigger onInfo with MEDIA_INFO_PREPARED for any item in the data
            // source queue for which the duration is now known, even if this is not the initial
            // preparation.
            mListener.onPrepared(mediaItem2);
        } else if (seekComplete) {
            // TODO(b/80232248): Suppress notification if this is an initial seek for a non-zero
            // start position.
            mPendingSeek = false;
            mListener.onSeekCompleted(getCurrentPosition());
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ComponentListener extends Player.DefaultEventListener
            implements VideoListener, AudioListener {

        // DefaultEventListener implementation.

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            handlePlayerStateChanged(playWhenReady, state);
        }

        @Override
        public void onSeekProcessed() {
            handleSeekProcessed();
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            handlePositionDiscontinuity(reason);
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

    }

    // TODO(b/80232248): Upstream a setter for the audio session ID then remove this.
    private static final class AudioSinkRenderersFactory extends DefaultRenderersFactory {

        private final DefaultAudioSink mAudioSink;

        AudioSinkRenderersFactory(Context context, DefaultAudioSink audioSink) {
            super(context);
            mAudioSink = audioSink;
        }

        @Override
        protected void buildAudioRenderers(Context context,
                @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                AudioProcessor[] audioProcessors, Handler eventHandler,
                AudioRendererEventListener eventListener, int extensionRendererMode,
                ArrayList<Renderer> out) {
            out.add(new MediaCodecAudioRenderer(
                    context,
                    MediaCodecSelector.DEFAULT,
                    drmSessionManager,
                    /* playClearSamplesWithoutKeys= */ false,
                    eventHandler,
                    eventListener,
                    mAudioSink));
        }

    }

    private static final class MediaItemInfo {

        final MediaItem2 mMediaItem;
        @Nullable
        final FileDescriptor mFileDescriptor;

        MediaItemInfo(MediaItem2 mediaItem) {
            this(mediaItem, null);
        }

        MediaItemInfo(MediaItem2 mediaItem, @Nullable FileDescriptor fileDescriptor) {
            mMediaItem = mediaItem;
            mFileDescriptor = fileDescriptor;
        }

        public void close() {
            if (mFileDescriptor != null) {
                try {
                    FileDescriptorUtil.close(mFileDescriptor);
                } catch (IOException e) {
                    Log.e(TAG, "Error closing file descriptor for " + mMediaItem, e);
                }
            }
        }
    }

    private static final class MediaItemQueue {

        private final Listener mListener;
        private final SimpleExoPlayer mPlayer;
        private final DataSource.Factory mDataSourceFactory;
        private final ConcatenatingMediaSource mConcatenatingMediaSource;
        private final ArrayDeque<MediaItemInfo> mMediaItemInfos;

        private long mStartPlayingTimeNs;
        private long mCurrentItemPlayingTimeUs;

        MediaItemQueue(Context context, SimpleExoPlayer player, Listener listener) {
            mPlayer = player;
            mListener = listener;
            String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
            mDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
            mConcatenatingMediaSource = new ConcatenatingMediaSource();
            mMediaItemInfos = new ArrayDeque<>();
            mStartPlayingTimeNs = -1;
        }

        public void clear() {
            while (!mMediaItemInfos.isEmpty()) {
                mMediaItemInfos.remove().close();
            }
        }

        public void setMediaItem2(MediaItem2 mediaItem2) {
            clear();
            mConcatenatingMediaSource.clear();
            setNextMediaItem2s(Collections.singletonList(mediaItem2));
        }

        public void setNextMediaItem2s(List<MediaItem2> mediaItem2s) {
            int size = mConcatenatingMediaSource.getSize();
            if (size > 1) {
                mConcatenatingMediaSource.removeMediaSourceRange(
                        /* fromIndex= */ 1, /* toIndex= */ size);
                while (mMediaItemInfos.size() > 1) {
                    mMediaItemInfos.removeLast().close();
                }
            }

            List<MediaSource> mediaSources = new ArrayList<>(mediaItem2s.size());
            for (MediaItem2 mediaItem2 : mediaItem2s) {
                final DataSource.Factory dataSourceFactory;
                MediaItemInfo mediaItemInfo;
                if (mediaItem2 instanceof FileMediaItem2) {
                    FileMediaItem2 fileMediaItem2 = (FileMediaItem2) mediaItem2;
                    FileDescriptor fileDescriptor;
                    try {
                        fileDescriptor =
                                FileDescriptorUtil.dup(fileMediaItem2.getFileDescriptor());
                    } catch (IOException e) {
                        // TODO(b/68398926): Surface as a source error.
                        Log.e(TAG, "Error duping file descriptor", e);
                        throw new IllegalStateException(e);
                    }
                    long offset = fileMediaItem2.getFileDescriptorOffset();
                    long length = fileMediaItem2.getFileDescriptorLength();
                    dataSourceFactory =
                            FileDescriptorDataSource.getFactory(fileDescriptor, offset, length);
                    mediaItemInfo = new MediaItemInfo(mediaItem2, fileDescriptor);
                } else {
                    dataSourceFactory = mDataSourceFactory;
                    mediaItemInfo = new MediaItemInfo(mediaItem2);
                }
                mediaSources.add(ExoPlayerUtils.createMediaSource(dataSourceFactory, mediaItem2));
                mMediaItemInfos.add(mediaItemInfo);
            }
            mConcatenatingMediaSource.addMediaSources(mediaSources);
        }

        public void preparePlayer() {
            mPlayer.prepare(mConcatenatingMediaSource);
        }

        @Nullable
        public MediaItem2 getCurrentMediaItem() {
            return mMediaItemInfos.isEmpty() ? null : mMediaItemInfos.peekFirst().mMediaItem;
        }

        public long getCurrentItemPlayingTimeMs() {
            return C.usToMs(mCurrentItemPlayingTimeUs);
        }

        public void skipToNext() {
            // TODO(b/68398926): Play the start position of the next media item.
            mMediaItemInfos.removeFirst().close();
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
            mCurrentItemPlayingTimeUs += (nowNs - mStartPlayingTimeNs + 500) / 1000;
            mStartPlayingTimeNs = -1;
        }

        public void onPlayerEnded() {
            MediaItem2 mediaItem = getCurrentMediaItem();
            mListener.onMediaItem2Ended(mediaItem);
            mListener.onPlaybackEnded(mediaItem);
        }

        public void onPositionDiscontinuity(boolean isPeriodTransition) {
            MediaItem2 currentMediaItem = getCurrentMediaItem();
            if (isPeriodTransition && mPlayer.getRepeatMode() != Player.REPEAT_MODE_OFF) {
                mListener.onLoop(currentMediaItem);
            }
            int windowIndex = mPlayer.getCurrentWindowIndex();
            if (windowIndex > 0) {
                // We're no longer playing the first item in the queue.
                if (isPeriodTransition) {
                    mListener.onMediaItem2Ended(getCurrentMediaItem());
                }
                for (int i = 0; i < windowIndex; i++) {
                    mMediaItemInfos.removeFirst().close();
                }
                if (isPeriodTransition) {
                    mListener.onMediaItem2StartedAsNext(getCurrentMediaItem());
                }
                mConcatenatingMediaSource.removeMediaSourceRange(0, windowIndex);
                mCurrentItemPlayingTimeUs = 0;
                mStartPlayingTimeNs = -1;
                if (mPlayer.getPlaybackState() == Player.STATE_READY) {
                    onPlaying();
                }
            }
        }

    }

}
