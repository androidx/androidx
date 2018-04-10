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

package androidx.media;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

class MediaInterface2 {
    private MediaInterface2() {
    }

    // TODO: relocate methods among different interfaces and classes.
    interface SessionPlaybackControl {
        void prepare();
        void play();
        void pause();
        void reset();

        void seekTo(long pos);

        int getPlayerState();
        long getCurrentPosition();
        long getDuration();

        long getBufferedPosition();
        int getBufferingState();

        float getPlaybackSpeed();
        void setPlaybackSpeed(float speed);
    }

    interface SessionPlaylistControl {
        void setOnDataSourceMissingHelper(MediaSession2.OnDataSourceMissingHelper helper);
        void clearOnDataSourceMissingHelper();

        List<MediaItem2> getPlaylist();
        MediaMetadata2 getPlaylistMetadata();
        void setPlaylist(List<MediaItem2> list, MediaMetadata2 metadata);
        void updatePlaylistMetadata(MediaMetadata2 metadata);

        MediaItem2 getCurrentMediaItem();
        void skipToPlaylistItem(MediaItem2 item);
        void skipToPreviousItem();
        void skipToNextItem();

        void addPlaylistItem(int index, MediaItem2 item);
        void removePlaylistItem(MediaItem2 item);
        void replacePlaylistItem(int index, MediaItem2 item);

        int getRepeatMode();
        void setRepeatMode(int repeatMode);
        int getShuffleMode();
        void setShuffleMode(int shuffleMode);
    }

    // Common interface for session2 and controller2
    // TODO: consider to add fastForward, rewind.
    abstract static class SessionPlayer implements SessionPlaybackControl, SessionPlaylistControl {
        abstract void skipForward();
        abstract void skipBackward();
        abstract void notifyError(@MediaSession2.ErrorCode int errorCode, @Nullable Bundle extras);
    }
}
