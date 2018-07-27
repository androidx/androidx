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

import android.annotation.TargetApi;
import android.content.Context;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.media.AudioAttributesCompat;
import androidx.media2.DataSourceDesc2;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaTimestamp2;
import androidx.media2.PlaybackParams2;
import androidx.media2.UriDataSourceDesc2;
import androidx.media2.exoplayer.external.ExoPlayerFactory;
import androidx.media2.exoplayer.external.Player;
import androidx.media2.exoplayer.external.SimpleExoPlayer;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.source.ExtractorMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.trackselection.DefaultTrackSelector;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.DefaultDataSourceFactory;
import androidx.media2.exoplayer.external.util.Assertions;
import androidx.media2.exoplayer.external.util.Util;
import androidx.media2.exoplayer.external.video.VideoListener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * @hide
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
@RestrictTo(LIBRARY_GROUP)
public final class ExoPlayerMediaPlayer2Impl extends MediaPlayer2 {

    private static final String USER_AGENT_NAME = "MediaPlayer2";

    private final @NonNull DataSource.Factory mDataSourceFactory;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final @NonNull SimpleExoPlayer mPlayer;

    private @Nullable Executor mExecutor;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable EventCallback mEventCallback;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable DataSourceDesc2 mDataSourceDescription;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mVideoWidth;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mVideoHeight;

    // TODO(b/80232248): Implement command queue and make setters notify their callbacks.
    // TODO(b/80232248): Make all methods thread-safe.

    /** Creates a new ExoPlayer wrapper using the specified context. */
    public ExoPlayerMediaPlayer2Impl(@NonNull Context context) {
        context = Preconditions.checkNotNull(context).getApplicationContext();
        String userAgent = Util.getUserAgent(context, USER_AGENT_NAME);
        mDataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
        mPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
        mPlayer.addListener(new ComponentListener());
    }

    @Override
    public void setEventCallback(Executor executor, EventCallback eventCallback) {
        this.mExecutor = Assertions.checkNotNull(executor);
        this.mEventCallback = Assertions.checkNotNull(eventCallback);
    }

    @Override
    public void setDataSource(DataSourceDesc2 dsd) {
        mDataSourceDescription = dsd;
    }

    @Override
    public DataSourceDesc2 getCurrentDataSource() {
        return mDataSourceDescription;
    }

    @Override
    public void prepare() {
        MediaSource mediaSource;
        if (mDataSourceDescription.getType() == DataSourceDesc2.TYPE_URI) {
            // TODO(b/111150876): Add support for HLS streams.
            Uri uri = ((UriDataSourceDesc2) mDataSourceDescription).getUri();
            mediaSource =
                    new ExtractorMediaSource.Factory(mDataSourceFactory).createMediaSource(uri);
        } else {
            throw new UnsupportedOperationException();
        }
        mPlayer.prepare(mediaSource);
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mEventCallback.onCallCompleted(
                        ExoPlayerMediaPlayer2Impl.this,
                        mDataSourceDescription,
                        CALL_COMPLETED_PREPARE,
                        CALL_STATUS_NO_ERROR);
            }
        });
    }

    @Override
    public void play() {
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        mPlayer.setPlayWhenReady(false);
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
        return mPlayer.getBufferedPosition();
    }

    @Override
    public @MediaPlayer2.MediaPlayer2State int getState() {
        int state = mPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
                return PLAYER_STATE_IDLE;
            case Player.STATE_BUFFERING:
                return PLAYER_STATE_PAUSED;
            case Player.STATE_READY:
                return mPlayer.getPlayWhenReady() ? PLAYER_STATE_PLAYING : PLAYER_STATE_PAUSED;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void loopCurrent(boolean loop) {
        mPlayer.setRepeatMode(loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    @Override
    public MediaPlayerConnector getMediaPlayerConnector() {
        return new ExoPlayerMediaPlayerConnector();
    }

    @Override
    public void close() {
        mPlayer.release();
    }

    @Override
    public void skipToNext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAudioAttributes(AudioAttributesCompat attributes) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(attributes.getContentType())
                .setFlags(attributes.getFlags())
                .setUsage(attributes.getUsage())
                .build();
        mPlayer.setAudioAttributes(audioAttributes);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        AudioAttributes audioAttributes = mPlayer.getAudioAttributes();
        return new AudioAttributesCompat.Builder()
                .setContentType(audioAttributes.contentType)
                .setFlags(audioAttributes.flags)
                .setUsage(audioAttributes.usage)
                .build();
    }

    @Override
    public void setNextDataSource(DataSourceDesc2 dsd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNextDataSources(List<DataSourceDesc2> dsds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlayerVolume(float volume) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getPlayerVolume() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSurface(Surface surface) {
        mPlayer.setVideoSurface(surface);
    }

    @Override
    public void clearPendingCommands() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
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
    public void reset() {
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
    public List<TrackInfo> getTrackInfo() {
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
    public void releaseDrm() throws NoDrmSchemeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaDrm.KeyRequest getDrmKeyRequest(byte[] keySetId, byte[] initData, String mimeType,
            int keyType, Map<String, String> optionalParameters) throws NoDrmSchemeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] provideDrmKeyResponse(byte[] keySetId, byte[] response)
            throws NoDrmSchemeException, DeniedByServerException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restoreDrmKeys(byte[] keySetId) throws NoDrmSchemeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDrmPropertyString(String propertyName)
            throws NoDrmSchemeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDrmPropertyString(String propertyName, String value)
            throws NoDrmSchemeException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ComponentListener extends Player.DefaultEventListener implements
            VideoListener {

        @Override
        public void onVideoSizeChanged(
                int width,
                int height,
                int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {
            mVideoWidth = width;
            mVideoHeight = height;
        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {}

        @Override
        public void onSeekProcessed() {}

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int state) {}

        @Override
        public void onRenderedFirstFrame() {}
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final class ExoPlayerMediaPlayerConnector extends MediaPlayerConnector {

        @Override
        public void prepare() {
            ExoPlayerMediaPlayer2Impl.this.prepare();
        }

        @Override
        public void play() {
            mPlayer.setPlayWhenReady(true);
        }

        @Override
        public void pause() {
            mPlayer.setPlayWhenReady(false);
        }

        @Override
        public void reset() {
            mPlayer.stop();
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
        public void setDataSource(DataSourceDesc2 dsd) {
            ExoPlayerMediaPlayer2Impl.this.setDataSource(dsd);
        }

        @Override
        public void setNextDataSource(DataSourceDesc2 dsd) {
            ExoPlayerMediaPlayer2Impl.this.setNextDataSource(dsd);
        }

        @Override
        public void setNextDataSources(List<DataSourceDesc2> dsds) {
            ExoPlayerMediaPlayer2Impl.this.setNextDataSources(dsds);
        }

        @Override
        public DataSourceDesc2 getCurrentDataSource() {
            return ExoPlayerMediaPlayer2Impl.this.getCurrentDataSource();
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
        public void close() throws Exception {
            ExoPlayerMediaPlayer2Impl.this.close();
        }

        // TODO: Implement these methods:

        @Override
        public void registerPlayerEventCallback(Executor e, PlayerEventCallback cb) {}

        @Override
        public void unregisterPlayerEventCallback(PlayerEventCallback cb) {}

        @Override
        public void setPlaybackSpeed(float speed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPlayerState() {
            return MediaPlayerConnector.PLAYER_STATE_IDLE;
        }

        @Override
        public int getBufferingState() {
            return MediaPlayerConnector.BUFFERING_STATE_UNKNOWN;
        }

    }

}
