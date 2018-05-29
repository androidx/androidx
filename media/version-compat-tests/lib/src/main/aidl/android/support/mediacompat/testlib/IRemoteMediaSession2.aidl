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

interface IRemoteMediaSession2 {

    void create(String sessionId);
    void runCustomTestCommands(String sessionId, int command, in Bundle args);

    // MediaSession2 Methods
    Bundle getToken(String sessionId);
    void sendCustomCommand(String sessionId, in Bundle command, in Bundle args);
    void sendCustomCommand2(String sessionId, in Bundle controller, in Bundle command,
            in Bundle args, in ResultReceiver receiver);
    void close(String sessionId);
    void notifyError(String sessionId, int errorCode, in Bundle extras);
    void setAllowedCommands(String sessionId, in Bundle controller, in Bundle commands);
    void notifyRoutesInfoChanged(String sessionId, in Bundle controller, in List<Bundle> routes);
    void setCustomLayout(String sessionId, in Bundle controller, in List<Bundle> layout);

    // MockPlayer Methods
    void setPlayerState(String sessionId, int state);
    void setCurrentPosition(String sessionId, long pos);
    void setBufferedPosition(String sessionId, long pos);
    void setDuration(String sessionId, long duration);
    void setPlaybackSpeed(String sessionId, float speed);
    void notifySeekCompleted(String sessionId, long pos);
    void notifyBufferingStateChanged(String sessionId, int itemIndex, int buffState);
    void notifyPlayerStateChanged(String sessionId, int state);
    void notifyPlaybackSpeedChanged(String sessionId, float speed);
    void notifyCurrentDataSourceChanged(String sessionId, int index);
    void notifyMediaPrepared(String sessionId, int index);

    // MockPlaylistAgent Methods
    void setPlaylist(String sessionId, in List<Bundle> playlist);
    void setPlaylistWithNewDsd(String sessionId, in List<Bundle> playlist);
    void setPlaylistMetadata(String sessionId, in Bundle metadata);
    void setShuffleMode(String sessionId, int shuffleMode);
    void setRepeatMode(String sessionId, int repeatMode);
    void setCurrentMediaItem(String sessionId, int index);
    void notifyPlaylistChanged(String sessionId);
    void notifyPlaylistMetadataChanged(String sessionId);
    void notifyShuffleModeChanged(String sessionId);
    void notifyRepeatModeChanged(String sessionId);
}
