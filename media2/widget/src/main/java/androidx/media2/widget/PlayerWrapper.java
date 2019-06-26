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

package androidx.media2.widget;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.BaseResult;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Wrapper for MediaController and SessionPlayer
 */
class PlayerWrapper {
    final MediaController mController;
    final SessionPlayer mPlayer;

    private final Executor mCallbackExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final PlayerCallback mWrapperCallback;
    private final MediaControllerCallback mControllerCallback;
    private final SessionPlayerCallback mPlayerCallback;

    private boolean mCallbackAttached;

    // cached states
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mSavedPlayerState = SessionPlayer.PLAYER_STATE_IDLE;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SessionCommandGroup mAllowedCommands;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaMetadata mMediaMetadata;

    private final SessionCommandGroup mAllCommands;

    PlayerWrapper(@NonNull MediaController controller, @NonNull Executor executor,
            @NonNull PlayerCallback callback) {
        if (controller == null) throw new NullPointerException("controller must not be null");
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (callback == null) throw new NullPointerException("callback must not be null");
        mController = controller;
        mCallbackExecutor = executor;
        mWrapperCallback = callback;
        mControllerCallback = new MediaControllerCallback();

        mPlayer = null;
        mPlayerCallback = null;

        mAllCommands = null;
    }

    PlayerWrapper(@NonNull SessionPlayer player, @NonNull Executor executor,
            @NonNull PlayerCallback callback) {
        if (player == null) throw new NullPointerException("player must not be null");
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (callback == null) throw new NullPointerException("callback must not be null");
        mPlayer = player;
        mCallbackExecutor = executor;
        mWrapperCallback = callback;
        mPlayerCallback = new SessionPlayerCallback();

        mController = null;
        mControllerCallback = null;

        mAllCommands = new SessionCommandGroup.Builder()
                .addAllPredefinedCommands(SessionCommand.COMMAND_VERSION_1)
                .build();
    }

    void attachCallback() {
        if (mCallbackAttached) return;
        if (mController != null) {
            mController.registerExtraCallback(mCallbackExecutor, mControllerCallback);
        } else if (mPlayer != null) {
            mPlayer.registerPlayerCallback(mCallbackExecutor, mPlayerCallback);
        }
        updateAndNotifyCachedStates();
        mCallbackAttached = true;
    }

    void detachCallback() {
        if (!mCallbackAttached) return;
        if (mController != null) {
            mController.unregisterExtraCallback(mControllerCallback);
        } else if (mPlayer != null) {
            mPlayer.unregisterPlayerCallback(mPlayerCallback);
        }
        mCallbackAttached = false;
    }

    boolean isPlaying() {
        return mSavedPlayerState == SessionPlayer.PLAYER_STATE_PLAYING;
    }

    long getCurrentPosition() {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return 0;
        }
        long position = 0;
        if (mController != null) {
            position = mController.getCurrentPosition();
        } else if (mPlayer != null) {
            position = mPlayer.getCurrentPosition();
        }
        return (position < 0) ? 0 : position;
    }

    long getBufferPercentage() {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return 0;
        }
        long duration = getDurationMs();
        if (duration == 0) return 0;
        long bufferedPos = 0;
        if (mController != null) {
            bufferedPos = mController.getBufferedPosition();
        } else if (mPlayer != null) {
            bufferedPos = mPlayer.getBufferedPosition();
        }
        return (bufferedPos < 0) ? 0 : (bufferedPos * 100 / duration);
    }

    int getPlayerState() {
        if (mController != null) {
            return mController.getPlayerState();
        } else if (mPlayer != null) {
            return mPlayer.getPlayerState();
        }
        return SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean canPause() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_PAUSE);
    }

    boolean canSeekBackward() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_SESSION_REWIND);
    }

    boolean canSeekForward() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD);
    }

    boolean canSkipToNext() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM);
    }

    boolean canSkipToPrevious() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM);
    }

    boolean canSeekTo() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO);
    }

    boolean canSelectDeselectTrack() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_SELECT_TRACK)
                && mAllowedCommands.hasCommand(SessionCommand.COMMAND_CODE_PLAYER_DESELECT_TRACK);
    }

    void pause() {
        if (mController != null) {
            mController.pause();
        } else if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    void play() {
        if (mController != null) {
            mController.play();
        } else if (mPlayer != null) {
            mPlayer.play();
        }
    }

    void seekTo(long posMs) {
        if (mController != null) {
            mController.seekTo(posMs);
        } else if (mPlayer != null) {
            mPlayer.seekTo(posMs);
        }
    }

    void skipToNextItem() {
        if (mController != null) {
            mController.skipToNextPlaylistItem();
        } else if (mPlayer != null) {
            mPlayer.skipToNextPlaylistItem();
        }
    }

    void skipToPreviousItem() {
        if (mController != null) {
            mController.skipToPreviousPlaylistItem();
        } else if (mPlayer != null) {
            mPlayer.skipToPreviousPlaylistItem();
        }
    }

    private float getPlaybackSpeed() {
        if (mController != null) {
            return mController.getPlaybackSpeed();
        } else if (mPlayer != null) {
            return mPlayer.getPlaybackSpeed();
        }
        return 1f;
    }

    void setPlaybackSpeed(float speed) {
        if (mController != null) {
            mController.setPlaybackSpeed(speed);
        } else if (mPlayer != null) {
            mPlayer.setPlaybackSpeed(speed);
        }
    }

    void selectTrack(TrackInfo trackInfo) {
        if (mController != null) {
            mController.selectTrack(trackInfo);
        } else if (mPlayer != null) {
            mPlayer.selectTrackInternal(trackInfo);
        }
    }

    void deselectTrack(TrackInfo trackInfo) {
        if (mController != null) {
            mController.deselectTrack(trackInfo);
        } else if (mPlayer != null) {
            mPlayer.deselectTrackInternal(trackInfo);
        }
    }

    long getDurationMs() {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return 0;
        }
        long duration = 0;
        if (mController != null) {
            duration = mController.getDuration();
        } else if (mPlayer != null) {
            duration = mPlayer.getDuration();
        }
        return (duration < 0) ? 0 : duration;
    }

    CharSequence getTitle() {
        if (mMediaMetadata != null) {
            if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            }
        }
        return null;
    }

    CharSequence getArtistText() {
        if (mMediaMetadata != null) {
            if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST)) {
                return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            }
        }
        return null;
    }

    @Nullable
    MediaItem getCurrentMediaItem() {
        if (mController != null) {
            return mController.getCurrentMediaItem();
        } else if (mPlayer != null) {
            return mPlayer.getCurrentMediaItem();
        }
        return null;
    }

    @Nullable
    private SessionCommandGroup getAllowedCommands() {
        if (mController != null) {
            return mController.getAllowedCommands();
        } else if (mPlayer != null) {
            // We can assume direct players allow all commands since no MediaSession is involved.
            return mAllCommands;
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateAndNotifyCachedStates() {
        boolean playerStateChanged = false;
        int playerState = getPlayerState();
        if (mSavedPlayerState != playerState) {
            mSavedPlayerState = playerState;
            playerStateChanged = true;
        }

        boolean allowedCommandsChanged = false;
        SessionCommandGroup allowedCommands = getAllowedCommands();
        if (mAllowedCommands != allowedCommands) {
            mAllowedCommands = allowedCommands;
            allowedCommandsChanged = true;
        }

        MediaItem item = getCurrentMediaItem();
        mMediaMetadata = item == null ? null : item.getMetadata();

        if (playerStateChanged) {
            mWrapperCallback.onPlayerStateChanged(this, playerState);
        }
        if (allowedCommands != null && allowedCommandsChanged) {
            mWrapperCallback.onAllowedCommandsChanged(this, allowedCommands);
        }
        mWrapperCallback.onCurrentMediaItemChanged(this, item);

        // notify other non-cached states
        mWrapperCallback.onPlaybackSpeedChanged(this, getPlaybackSpeed());
        List<TrackInfo> trackInfos = getTrackInfo();
        if (trackInfos != null) {
            mWrapperCallback.onTrackInfoChanged(this, trackInfos);
        }
    }

    @NonNull
    VideoSize getVideoSize() {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return new VideoSize(0, 0);
        }
        if (mController != null) {
            return mController.getVideoSize();
        } else if (mPlayer != null) {
            return mPlayer.getVideoSizeInternal();
        }
        return new VideoSize(0, 0);
    }

    @NonNull
    List<TrackInfo> getTrackInfo() {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return null;
        }
        if (mController != null) {
            return mController.getTrackInfo();
        } else if (mPlayer != null) {
            return mPlayer.getTrackInfoInternal();
        }
        return Collections.emptyList();
    }

    @Nullable
    TrackInfo getSelectedTrack(int trackType) {
        if (mSavedPlayerState == SessionPlayer.PLAYER_STATE_IDLE) {
            return null;
        }
        if (mController != null) {
            return mController.getSelectedTrack(trackType);
        } else if (mPlayer != null) {
            return mPlayer.getSelectedTrackInternal(trackType);
        }
        return null;
    }

    ListenableFuture<? extends BaseResult> setSurface(Surface surface) {
        if (mController != null) {
            return mController.setSurface(surface);
        } else if (mPlayer != null) {
            return mPlayer.setSurfaceInternal(surface);
        }
        return null;
    }

    private class MediaControllerCallback extends MediaController.ControllerCallback {
        MediaControllerCallback() {
        }

        @Override
        public void onConnected(@NonNull MediaController controller,
                @NonNull SessionCommandGroup allowedCommands) {
            mWrapperCallback.onConnected(PlayerWrapper.this);
            updateAndNotifyCachedStates();
        }

        @Override
        public void onAllowedCommandsChanged(@NonNull MediaController controller,
                @NonNull SessionCommandGroup commands) {
            if (mAllowedCommands == commands) return;
            mAllowedCommands = commands;
            mWrapperCallback.onAllowedCommandsChanged(PlayerWrapper.this, commands);
        }

        @Override
        public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
            if (mSavedPlayerState == state) return;
            mSavedPlayerState = state;
            mWrapperCallback.onPlayerStateChanged(PlayerWrapper.this, state);
        }

        @Override
        public void onPlaybackSpeedChanged(@NonNull MediaController controller, float speed) {
            mWrapperCallback.onPlaybackSpeedChanged(PlayerWrapper.this, speed);
        }

        @Override
        public void onSeekCompleted(@NonNull MediaController controller, long position) {
            mWrapperCallback.onSeekCompleted(PlayerWrapper.this, position);
        }

        @Override
        public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                @Nullable MediaItem item) {
            mMediaMetadata = item == null ? null : item.getMetadata();
            mWrapperCallback.onCurrentMediaItemChanged(PlayerWrapper.this, item);
        }

        @Override
        public void onPlaybackCompleted(@NonNull MediaController controller) {
            mWrapperCallback.onPlaybackCompleted(PlayerWrapper.this);
        }

        @Override
        public void onVideoSizeChanged(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {
            mWrapperCallback.onVideoSizeChanged(PlayerWrapper.this, item, videoSize);
        }

        @Override
        public void onSubtitleData(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            mWrapperCallback.onSubtitleData(PlayerWrapper.this, item, track, data);
        }

        @Override
        public void onTrackInfoChanged(@NonNull MediaController controller,
                @NonNull List<TrackInfo> trackInfos) {
            mWrapperCallback.onTrackInfoChanged(PlayerWrapper.this, trackInfos);
        }

        @Override
        public void onTrackSelected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {
            mWrapperCallback.onTrackSelected(PlayerWrapper.this, trackInfo);
        }

        @Override
        public void onTrackDeselected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {
            mWrapperCallback.onTrackDeselected(PlayerWrapper.this, trackInfo);
        }
    }

    private class SessionPlayerCallback extends SessionPlayer.PlayerCallback {
        SessionPlayerCallback() {
        }

        @Override
        public void onPlayerStateChanged(@NonNull SessionPlayer player, int playerState) {
            if (mSavedPlayerState == playerState) return;
            mSavedPlayerState = playerState;
            mWrapperCallback.onPlayerStateChanged(PlayerWrapper.this, playerState);
        }

        @Override
        public void onPlaybackSpeedChanged(@NonNull SessionPlayer player, float playbackSpeed) {
            mWrapperCallback.onPlaybackSpeedChanged(PlayerWrapper.this, playbackSpeed);
        }

        @Override
        public void onSeekCompleted(@NonNull SessionPlayer player, long position) {
            mWrapperCallback.onSeekCompleted(PlayerWrapper.this, position);
        }

        @Override
        public void onCurrentMediaItemChanged(@NonNull SessionPlayer player,
                @NonNull MediaItem item) {
            mMediaMetadata = item == null ? null : item.getMetadata();
            mWrapperCallback.onCurrentMediaItemChanged(PlayerWrapper.this, item);
        }

        @Override
        public void onPlaybackCompleted(@NonNull SessionPlayer player) {
            mWrapperCallback.onPlaybackCompleted(PlayerWrapper.this);
        }

        @Override
        public void onVideoSizeChangedInternal(@NonNull SessionPlayer player,
                @NonNull MediaItem item, @NonNull VideoSize size) {
            mWrapperCallback.onVideoSizeChanged(PlayerWrapper.this, item, size);
        }

        @Override
        public void onSubtitleData(@NonNull SessionPlayer player, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            mWrapperCallback.onSubtitleData(PlayerWrapper.this, item, track, data);
        }

        @Override
        public void onTrackInfoChanged(@NonNull SessionPlayer player,
                @NonNull List<TrackInfo> trackInfos) {
            mWrapperCallback.onTrackInfoChanged(PlayerWrapper.this, trackInfos);
        }

        @Override
        public void onTrackSelected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
            mWrapperCallback.onTrackSelected(PlayerWrapper.this, trackInfo);
        }

        @Override
        public void onTrackDeselected(@NonNull SessionPlayer player, @NonNull TrackInfo trackInfo) {
            mWrapperCallback.onTrackDeselected(PlayerWrapper.this, trackInfo);
        }
    }

    abstract static class PlayerCallback {
        void onConnected(@NonNull PlayerWrapper player) {
        }
        void onAllowedCommandsChanged(@NonNull PlayerWrapper player,
                @NonNull SessionCommandGroup commands) {
        }
        void onCurrentMediaItemChanged(@NonNull PlayerWrapper player, @Nullable MediaItem item) {
        }
        void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
        }
        void onPlaybackSpeedChanged(@NonNull PlayerWrapper player, float speed) {
        }
        void onSeekCompleted(@NonNull PlayerWrapper player, long position) {
        }
        void onPlaybackCompleted(@NonNull PlayerWrapper player) {
        }
        void onVideoSizeChanged(@NonNull PlayerWrapper player, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {
        }
        void onTrackInfoChanged(@NonNull PlayerWrapper player,
                @NonNull List<TrackInfo> trackInfos) {
        }
        void onTrackSelected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
        }
        void onTrackDeselected(@NonNull PlayerWrapper player, @NonNull TrackInfo trackInfo) {
        }
        void onSubtitleData(@NonNull PlayerWrapper player, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
        }
    }
}
