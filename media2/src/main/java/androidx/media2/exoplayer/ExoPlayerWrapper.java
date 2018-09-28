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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
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
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoListener;

import java.util.ArrayList;
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

    }

    private static final String USER_AGENT_NAME = "MediaPlayer2";

    private final Context mContext;
    private final Looper mLooper;
    private final DataSource.Factory mDataSourceFactory;
    private final Listener mListener;

    private HandlerThread mHandlerThread;
    private SimpleExoPlayer mPlayer;
    private DefaultAudioSink mAudioSink;

    // TODO(b/80232248): Store with the media item it relates to.
    private long mStartPlaybackTimeNs;
    private long mPlayingTimeUs;

    private int mAudioSessionId;
    private int mAuxEffectId;
    private float mAuxEffectSendLevel;
    private boolean mPrepared;
    private boolean mPendingSeek;
    private MediaItem2 mMediaItem;
    private int mVideoWidth;
    private int mVideoHeight;
    private PlaybackParams2 mPlaybackParams2;

    /**
     * Creates a new ExoPlayer wrapper.
     *
     * @param context The context for accessing system components.
     * @param listener A listener for player wrapper events.
     */
    ExoPlayerWrapper(Context context, Listener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mHandlerThread = new HandlerThread("ExoMediaPlayer2Thread");
        mHandlerThread.start();
        mLooper = mHandlerThread.getLooper();
        String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
        mDataSourceFactory = new DefaultDataSourceFactory(mContext, userAgent);
    }

    public Looper getLooper() {
        return mLooper;
    }

    public void setMediaItem(MediaItem2 mediaItem2) {
        // TODO(b/80232248): Update the data source queue when it's implemented.
        mMediaItem = Preconditions.checkNotNull(mediaItem2);
    }

    public MediaItem2 getCurrentMediaItem() {
        return mMediaItem;
    }

    public void prepare() {
        MediaSource mediaSource = ExoPlayerUtils.createMediaSource(mDataSourceFactory, mMediaItem);
        mPlayer.prepare(mediaSource);
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
        long playingTimeMs = C.usToMs(mPlayingTimeUs);
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
        ComponentListener listener = new ComponentListener();
        mPlayer.addListener(listener);
        mPlayer.addVideoListener(listener);
        mMediaItem = null;
        mVideoWidth = 0;
        mVideoHeight = 0;
        mPrepared = false;
        mPlayingTimeUs = 0;
        mStartPlaybackTimeNs = -1;
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
        if (mHandlerThread != null) {
            mPlayer.release();
            mPlayer = null;
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    public boolean hasError() {
        return mPlayer.getPlaybackError() != null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleVideoSizeChanged(int width, int height) {
        // TODO(b/80232248): Get the active media item from the media item queue.
        mVideoWidth = width;
        mVideoHeight = height;
        mListener.onVideoSizeChanged(mMediaItem, width, height);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handleRenderedFirstFrame() {
        // TODO(b/80232248): Get the active media item from the media item queue.
        mListener.onVideoRenderingStart(mMediaItem);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void handlePlayerStateChanged(boolean playWhenReady, int state) {
        if (state == Player.STATE_READY && playWhenReady) {
            maybeUpdateTimerForPlaying();
        } else {
            maybeUpdateTimerForStopped();
        }

        if (state != Player.STATE_READY) {
            return;
        }

        maybeNotifyReadyEvents();
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
    void handleAudioSessionId(int audioSessionId) {
        mAudioSessionId = audioSessionId;
    }

    private void maybeUpdateTimerForPlaying() {
        if (mStartPlaybackTimeNs != -1) {
            return;
        }
        mStartPlaybackTimeNs = System.nanoTime();
    }

    private void maybeUpdateTimerForStopped() {
        if (mStartPlaybackTimeNs == -1) {
            return;
        }
        long nowNs = System.nanoTime();
        mPlayingTimeUs += (nowNs - mStartPlaybackTimeNs + 500) / 1000;
        mStartPlaybackTimeNs = -1;
    }

    private void maybeNotifyReadyEvents() {
        MediaItem2 mediaItem2 = mMediaItem;
        boolean prepareComplete = !mPrepared;
        boolean seekComplete = mPendingSeek;
        if (prepareComplete) {
            mPrepared = true;
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
        public void onPlayerStateChanged(boolean playWhenReady, int state) {
            handlePlayerStateChanged(playWhenReady, state);
        }

        @Override
        public void onSeekProcessed() {
            handleSeekProcessed();
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

}
