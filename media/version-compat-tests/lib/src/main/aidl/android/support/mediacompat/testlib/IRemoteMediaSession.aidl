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

package android.support.mediacompat.testlib;

import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.versionedparcelable.ParcelImpl;

interface IRemoteMediaSession {

    void create(String sessionId);

    // MediaSession Methods
    ParcelImpl getToken(String sessionId);
    Bundle getCompatToken(String sessionId);
    void updatePlayer(String sessionId, in Bundle playerBundle);
    void broadcastCustomCommand(String sessionId, in ParcelImpl command, in Bundle args);
    void sendCustomCommand(String sessionId, in Bundle controller, in ParcelImpl command,
            in Bundle args);
    void close(String sessionId);
    void setAllowedCommands(String sessionId, in Bundle controller, in ParcelImpl commands);
    void setCustomLayout(String sessionId, in Bundle controller, in List<ParcelImpl> layout);

    // SessionPlayer Methods
    void setPlayerState(String sessionId, int state);
    void setCurrentPosition(String sessionId, long pos);
    void setBufferedPosition(String sessionId, long pos);
    void setDuration(String sessionId, long duration);
    void setPlaybackSpeed(String sessionId, float speed);
    void notifySeekCompleted(String sessionId, long pos);
    void notifyBufferingStateChanged(String sessionId, int itemIndex, int buffState);
    void notifyPlayerStateChanged(String sessionId, int state);
    void notifyPlaybackSpeedChanged(String sessionId, float speed);
    void notifyCurrentMediaItemChanged(String sessionId, int index);
    void notifyAudioAttributesChanged(String sessionId, in Bundle attrs);

    void setPlaylist(String sessionId, in List<ParcelImpl> playlist);
    void createAndSetDummyPlaylist(String sessionId, int size);
    void setPlaylistWithDummyItem(String sessionId, in List<ParcelImpl> playlist);
    void setPlaylistMetadata(String sessionId, in ParcelImpl metadata);
    void setPlaylistMetadataWithLargeBitmaps(String sessionId, int count, int width, int height);
    void setShuffleMode(String sessionId, int shuffleMode);
    void setRepeatMode(String sessionId, int repeatMode);
    void setCurrentMediaItem(String sessionId, int index);
    void notifyPlaylistChanged(String sessionId);
    void notifyPlaylistMetadataChanged(String sessionId);
    void notifyShuffleModeChanged(String sessionId);
    void notifyRepeatModeChanged(String sessionId);
    void notifyPlaybackCompleted(String sessionId);
}
