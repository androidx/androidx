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

import androidx.media2.SessionPlayer.PlayerResult;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

class MediaInterface {
    private MediaInterface() {
    }

    // TODO: relocate methods among different interfaces and classes.
    interface SessionPlaybackControl {
        ListenableFuture<PlayerResult> prepare();
        ListenableFuture<PlayerResult> play();
        ListenableFuture<PlayerResult> pause();

        ListenableFuture<PlayerResult> seekTo(long pos);

        int getPlayerState();
        long getCurrentPosition();
        long getDuration();

        long getBufferedPosition();
        int getBufferingState();

        float getPlaybackSpeed();
        ListenableFuture<PlayerResult> setPlaybackSpeed(float speed);
    }

    interface SessionPlaylistControl {
        List<MediaItem> getPlaylist();
        MediaMetadata getPlaylistMetadata();
        ListenableFuture<PlayerResult> setPlaylist(List<MediaItem> list, MediaMetadata metadata);
        ListenableFuture<PlayerResult> setMediaItem(MediaItem item);
        ListenableFuture<PlayerResult> updatePlaylistMetadata(MediaMetadata metadata);

        MediaItem getCurrentMediaItem();
        int getCurrentMediaItemIndex();
        int getPreviousMediaItemIndex();
        int getNextMediaItemIndex();
        ListenableFuture<PlayerResult> skipToPlaylistItem(int index);
        ListenableFuture<PlayerResult> skipToPreviousItem();
        ListenableFuture<PlayerResult> skipToNextItem();

        ListenableFuture<PlayerResult> addPlaylistItem(int index, MediaItem item);
        ListenableFuture<PlayerResult> removePlaylistItem(int index);
        ListenableFuture<PlayerResult> replacePlaylistItem(int index, MediaItem item);

        int getRepeatMode();
        ListenableFuture<PlayerResult> setRepeatMode(int repeatMode);
        int getShuffleMode();
        ListenableFuture<PlayerResult> setShuffleMode(int shuffleMode);
    }

    // Common interface for session and controller
    interface SessionPlayer extends SessionPlaybackControl, SessionPlaylistControl {
    }
}
