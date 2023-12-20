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
import androidx.core.util.ObjectsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Wrapper for androidx.media2.session.MediaController and androidx.media2.common.SessionPlayer */
@SuppressWarnings("deprecation")
class PlayerWrapper {
    final androidx.media2.session.MediaController mController;
    final androidx.media2.common.SessionPlayer mPlayer;

    private final Executor mCallbackExecutor;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final PlayerCallback mWrapperCallback;
    private final MediaControllerCallback mControllerCallback;
    private final SessionPlayerCallback mPlayerCallback;

    private boolean mCallbackAttached;

    // cached states
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mSavedPlayerState = androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    androidx.media2.session.SessionCommandGroup mAllowedCommands;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    androidx.media2.common.MediaMetadata mMediaMetadata;

    private final androidx.media2.session.SessionCommandGroup mAllCommands;

    PlayerWrapper(
            @NonNull androidx.media2.session.MediaController controller,
            @NonNull Executor executor,
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

    PlayerWrapper(
            @NonNull androidx.media2.common.SessionPlayer player,
            @NonNull Executor executor,
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

        mAllCommands =
                new androidx.media2.session.SessionCommandGroup.Builder()
                        .addAllPredefinedCommands(
                                androidx.media2.session.SessionCommand.COMMAND_VERSION_1)
                        .build();
    }

    boolean hasDisconnectedController() {
        return mController != null && !mController.isConnected();
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
        return mSavedPlayerState == androidx.media2.common.SessionPlayer.PLAYER_STATE_PLAYING;
    }

    long getCurrentPosition() {
        if (mSavedPlayerState == androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE) {
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
        if (mSavedPlayerState == androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE) {
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
        return androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean canPause() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_PAUSE);
    }

    boolean canSeekBackward() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_REWIND);
    }

    boolean canSeekForward() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD);
    }

    boolean canSkipToNext() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand
                                .COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM);
    }

    boolean canSkipToPrevious() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand
                                .COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM);
    }

    boolean canSeekTo() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO);
    }

    boolean canSelectDeselectTrack() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SELECT_TRACK)
                && mAllowedCommands.hasCommand(
                        androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_DESELECT_TRACK);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void pause() {
        if (mController != null) {
            mController.pause();
        } else if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void play() {
        if (mController != null) {
            mController.play();
        } else if (mPlayer != null) {
            mPlayer.play();
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void seekTo(long posMs) {
        if (mController != null) {
            mController.seekTo(posMs);
        } else if (mPlayer != null) {
            mPlayer.seekTo(posMs);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void skipToNextItem() {
        if (mController != null) {
            mController.skipToNextPlaylistItem();
        } else if (mPlayer != null) {
            mPlayer.skipToNextPlaylistItem();
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
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

    @SuppressWarnings("FutureReturnValueIgnored")
    void setPlaybackSpeed(float speed) {
        if (mController != null) {
            mController.setPlaybackSpeed(speed);
        } else if (mPlayer != null) {
            mPlayer.setPlaybackSpeed(speed);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void selectTrack(androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
        if (mController != null) {
            mController.selectTrack(trackInfo);
        } else if (mPlayer != null) {
            mPlayer.selectTrack(trackInfo);
        }
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    void deselectTrack(androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
        if (mController != null) {
            mController.deselectTrack(trackInfo);
        } else if (mPlayer != null) {
            mPlayer.deselectTrack(trackInfo);
        }
    }

    long getDurationMs() {
        if (mSavedPlayerState == androidx.media2.common.SessionPlayer.PLAYER_STATE_IDLE) {
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
            if (mMediaMetadata.containsKey(
                    androidx.media2.common.MediaMetadata.METADATA_KEY_TITLE)) {
                return mMediaMetadata.getText(
                        androidx.media2.common.MediaMetadata.METADATA_KEY_TITLE);
            }
        }
        return null;
    }

    CharSequence getArtistText() {
        if (mMediaMetadata != null) {
            if (mMediaMetadata.containsKey(
                    androidx.media2.common.MediaMetadata.METADATA_KEY_ARTIST)) {
                return mMediaMetadata.getText(
                        androidx.media2.common.MediaMetadata.METADATA_KEY_ARTIST);
            }
        }
        return null;
    }

    @Nullable
    androidx.media2.common.MediaItem getCurrentMediaItem() {
        if (mController != null) {
            return mController.getCurrentMediaItem();
        } else if (mPlayer != null) {
            return mPlayer.getCurrentMediaItem();
        }
        return null;
    }

    @Nullable
    private androidx.media2.session.SessionCommandGroup getAllowedCommands() {
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
        androidx.media2.session.SessionCommandGroup allowedCommands = getAllowedCommands();
        if (!ObjectsCompat.equals(mAllowedCommands, allowedCommands)) {
            mAllowedCommands = allowedCommands;
            allowedCommandsChanged = true;
        }

        androidx.media2.common.MediaItem item = getCurrentMediaItem();
        mMediaMetadata = item == null ? null : item.getMetadata();

        if (playerStateChanged) {
            mWrapperCallback.onPlayerStateChanged(this, playerState);
        }
        if (allowedCommands != null && allowedCommandsChanged) {
            mWrapperCallback.onAllowedCommandsChanged(this, allowedCommands);
        }
        mWrapperCallback.onCurrentMediaItemChanged(this, item);
        notifyNonCachedStates();
    }

    @NonNull
    androidx.media2.common.VideoSize getVideoSize() {
        if (mController != null) {
            return mController.getVideoSize();
        } else if (mPlayer != null) {
            return mPlayer.getVideoSize();
        }
        return new androidx.media2.common.VideoSize(0, 0);
    }

    @NonNull
    List<androidx.media2.common.SessionPlayer.TrackInfo> getTracks() {
        if (mController != null) {
            return mController.getTracks();
        } else if (mPlayer != null) {
            return mPlayer.getTracks();
        }
        return Collections.emptyList();
    }

    @Nullable
    androidx.media2.common.SessionPlayer.TrackInfo getSelectedTrack(int trackType) {
        if (mController != null) {
            return mController.getSelectedTrack(trackType);
        } else if (mPlayer != null) {
            return mPlayer.getSelectedTrack(trackType);
        }
        return null;
    }

    ListenableFuture<? extends androidx.media2.common.BaseResult> setSurface(Surface surface) {
        if (mController != null) {
            return mController.setSurface(surface);
        } else if (mPlayer != null) {
            return mPlayer.setSurface(surface);
        }
        return null;
    }

    int getCurrentMediaItemIndex() {
        if (mController != null) {
            return mController.getCurrentMediaItemIndex();
        } else if (mPlayer != null) {
            return mPlayer.getCurrentMediaItemIndex();
        }
        return androidx.media2.common.SessionPlayer.INVALID_ITEM_INDEX;
    }

    int getPreviousMediaItemIndex() {
        if (mController != null) {
            return mController.getPreviousMediaItemIndex();
        } else if (mPlayer != null) {
            return mPlayer.getPreviousMediaItemIndex();
        }
        return androidx.media2.common.SessionPlayer.INVALID_ITEM_INDEX;
    }

    int getNextMediaItemIndex() {
        if (mController != null) {
            return mController.getNextMediaItemIndex();
        } else if (mPlayer != null) {
            return mPlayer.getNextMediaItemIndex();
        }
        return androidx.media2.common.SessionPlayer.INVALID_ITEM_INDEX;
    }

    private class MediaControllerCallback
            extends androidx.media2.session.MediaController.ControllerCallback {
        MediaControllerCallback() {
        }

        @Override
        public void onConnected(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.session.SessionCommandGroup allowedCommands) {
            mWrapperCallback.onConnected(PlayerWrapper.this);
            updateAndNotifyCachedStates();
        }

        @Override
        public void onAllowedCommandsChanged(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.session.SessionCommandGroup commands) {
            if (ObjectsCompat.equals(mAllowedCommands, commands)) return;
            mAllowedCommands = commands;
            mWrapperCallback.onAllowedCommandsChanged(PlayerWrapper.this, commands);
        }

        @Override
        public void onPlayerStateChanged(
                @NonNull androidx.media2.session.MediaController controller, int state) {
            if (mSavedPlayerState == state) return;
            mSavedPlayerState = state;
            mWrapperCallback.onPlayerStateChanged(PlayerWrapper.this, state);
        }

        @Override
        public void onPlaybackSpeedChanged(
                @NonNull androidx.media2.session.MediaController controller, float speed) {
            mWrapperCallback.onPlaybackSpeedChanged(PlayerWrapper.this, speed);
        }

        @Override
        public void onSeekCompleted(
                @NonNull androidx.media2.session.MediaController controller, long position) {
            mWrapperCallback.onSeekCompleted(PlayerWrapper.this, position);
        }

        @Override
        public void onCurrentMediaItemChanged(
                @NonNull androidx.media2.session.MediaController controller,
                @Nullable androidx.media2.common.MediaItem item) {
            mMediaMetadata = item == null ? null : item.getMetadata();
            mWrapperCallback.onCurrentMediaItemChanged(PlayerWrapper.this, item);
        }

        @Override
        public void onPlaylistChanged(
                @NonNull androidx.media2.session.MediaController controller,
                @Nullable List<androidx.media2.common.MediaItem> list,
                @Nullable androidx.media2.common.MediaMetadata metadata) {
            mWrapperCallback.onPlaylistChanged(PlayerWrapper.this, list, metadata);
        }

        @Override
        public void onPlaybackCompleted(
                @NonNull androidx.media2.session.MediaController controller) {
            mWrapperCallback.onPlaybackCompleted(PlayerWrapper.this);
        }

        @Override
        public void onVideoSizeChanged(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.common.VideoSize videoSize) {
            mWrapperCallback.onVideoSizeChanged(PlayerWrapper.this, videoSize);
        }

        @Override
        public void onSubtitleData(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.common.MediaItem item,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo track,
                @NonNull androidx.media2.common.SubtitleData data) {
            mWrapperCallback.onSubtitleData(PlayerWrapper.this, item, track, data);
        }

        @Override
        public void onTracksChanged(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull List<androidx.media2.common.SessionPlayer.TrackInfo> tracks) {
            mWrapperCallback.onTracksChanged(PlayerWrapper.this, tracks);
        }

        @Override
        public void onTrackSelected(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
            mWrapperCallback.onTrackSelected(PlayerWrapper.this, trackInfo);
        }

        @Override
        public void onTrackDeselected(
                @NonNull androidx.media2.session.MediaController controller,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
            mWrapperCallback.onTrackDeselected(PlayerWrapper.this, trackInfo);
        }
    }

    private void notifyNonCachedStates() {
        mWrapperCallback.onPlaybackSpeedChanged(this, getPlaybackSpeed());

        List<androidx.media2.common.SessionPlayer.TrackInfo> trackInfos = getTracks();
        if (trackInfos != null) {
            mWrapperCallback.onTracksChanged(PlayerWrapper.this, trackInfos);
        }
        androidx.media2.common.MediaItem item = getCurrentMediaItem();
        if (item != null) {
            mWrapperCallback.onVideoSizeChanged(PlayerWrapper.this, getVideoSize());
        }
    }

    private class SessionPlayerCallback
            extends androidx.media2.common.SessionPlayer.PlayerCallback {
        SessionPlayerCallback() {
        }

        @Override
        public void onPlayerStateChanged(
                @NonNull androidx.media2.common.SessionPlayer player, int playerState) {
            if (mSavedPlayerState == playerState) return;
            mSavedPlayerState = playerState;
            mWrapperCallback.onPlayerStateChanged(PlayerWrapper.this, playerState);
        }

        @Override
        public void onPlaybackSpeedChanged(
                @NonNull androidx.media2.common.SessionPlayer player, float playbackSpeed) {
            mWrapperCallback.onPlaybackSpeedChanged(PlayerWrapper.this, playbackSpeed);
        }

        @Override
        public void onSeekCompleted(
                @NonNull androidx.media2.common.SessionPlayer player, long position) {
            mWrapperCallback.onSeekCompleted(PlayerWrapper.this, position);
        }

        @Override
        public void onCurrentMediaItemChanged(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull androidx.media2.common.MediaItem item) {
            mMediaMetadata = item == null ? null : item.getMetadata();
            mWrapperCallback.onCurrentMediaItemChanged(PlayerWrapper.this, item);
        }

        @Override
        public void onPlaylistChanged(
                @NonNull androidx.media2.common.SessionPlayer player,
                @Nullable List<androidx.media2.common.MediaItem> list,
                @Nullable androidx.media2.common.MediaMetadata metadata) {
            mWrapperCallback.onPlaylistChanged(PlayerWrapper.this, list, metadata);
        }

        @Override
        public void onPlaybackCompleted(@NonNull androidx.media2.common.SessionPlayer player) {
            mWrapperCallback.onPlaybackCompleted(PlayerWrapper.this);
        }

        @Override
        public void onVideoSizeChanged(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull androidx.media2.common.VideoSize size) {
            mWrapperCallback.onVideoSizeChanged(PlayerWrapper.this, size);
        }

        @Override
        public void onSubtitleData(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull androidx.media2.common.MediaItem item,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo track,
                @NonNull androidx.media2.common.SubtitleData data) {
            mWrapperCallback.onSubtitleData(PlayerWrapper.this, item, track, data);
        }

        @Override
        public void onTracksChanged(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull List<androidx.media2.common.SessionPlayer.TrackInfo> tracks) {
            mWrapperCallback.onTracksChanged(PlayerWrapper.this, tracks);
        }

        @Override
        public void onTrackSelected(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
            mWrapperCallback.onTrackSelected(PlayerWrapper.this, trackInfo);
        }

        @Override
        public void onTrackDeselected(
                @NonNull androidx.media2.common.SessionPlayer player,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {
            mWrapperCallback.onTrackDeselected(PlayerWrapper.this, trackInfo);
        }
    }

    abstract static class PlayerCallback {
        void onConnected(@NonNull PlayerWrapper player) {
        }

        void onAllowedCommandsChanged(
                @NonNull PlayerWrapper player,
                @NonNull androidx.media2.session.SessionCommandGroup commands) {}

        void onCurrentMediaItemChanged(
                @NonNull PlayerWrapper player, @Nullable androidx.media2.common.MediaItem item) {}

        void onPlaylistChanged(
                @NonNull PlayerWrapper player,
                @Nullable List<androidx.media2.common.MediaItem> list,
                @Nullable androidx.media2.common.MediaMetadata metadata) {}

        void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
        }
        void onPlaybackSpeedChanged(@NonNull PlayerWrapper player, float speed) {
        }
        void onSeekCompleted(@NonNull PlayerWrapper player, long position) {
        }
        void onPlaybackCompleted(@NonNull PlayerWrapper player) {
        }

        void onVideoSizeChanged(
                @NonNull PlayerWrapper player,
                @NonNull androidx.media2.common.VideoSize videoSize) {}

        void onTracksChanged(
                @NonNull PlayerWrapper player,
                @NonNull List<androidx.media2.common.SessionPlayer.TrackInfo> tracks) {}

        void onTrackSelected(
                @NonNull PlayerWrapper player,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {}

        void onTrackDeselected(
                @NonNull PlayerWrapper player,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo trackInfo) {}

        void onSubtitleData(
                @NonNull PlayerWrapper player,
                @NonNull androidx.media2.common.MediaItem item,
                @NonNull androidx.media2.common.SessionPlayer.TrackInfo track,
                @NonNull androidx.media2.common.SubtitleData data) {}
    }
}
