/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (String controllerId, the "License");
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

import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

interface IRemoteMediaController2 {

    void create(String controllerId, in Bundle token, boolean waitForConnection);
    void runCustomTestCommands(String controllerId, int command, in Bundle args);

    // MediaController2 Methods
    void play(String controllerId);
    void pause(String controllerId);
    void reset(String controllerId);
    void prepare(String controllerId);
    void seekTo(String controllerId, long pos);
    void setPlaybackSpeed(String controllerId, float speed);
    void setPlaylist(String controllerId, in List<Bundle> list, in Bundle metadata);
    void updatePlaylistMetadata(String controllerId, in Bundle metadata);
    void addPlaylistItem(String controllerId, int index, in Bundle item);
    void removePlaylistItem(String controllerId, in Bundle item);
    void replacePlaylistItem(String controllerId, int index, in Bundle item);
    void skipToPreviousItem(String controllerId);
    void skipToNextItem(String controllerId);
    void skipToPlaylistItem(String controllerId, in Bundle item);
    void setShuffleMode(String controllerId, int shuffleMode);
    void setRepeatMode(String controllerId, int repeatMode);
    void setVolumeTo(String controllerId, int value, int flags);
    void adjustVolume(String controllerId, int direction, int flags);
    void sendCustomCommand(String controllerId, in Bundle command, in Bundle args,
            in ResultReceiver cb);
    void fastForward(String controllerId);
    void rewind(String controllerId);
    void playFromMediaId(String controllerId, String mediaId, in Bundle extras);
    void playFromSearch(String controllerId, String query, in Bundle extras);
    void playFromUri(String controllerId, in Uri uri, in Bundle extras);
    void prepareFromMediaId(String controllerId, String mediaId, in Bundle extras);
    void prepareFromSearch(String controllerId, String query, in Bundle extras);
    void prepareFromUri(String controllerId, in Uri uri, in Bundle extras);
    void setRating(String controllerId, String mediaId, in Bundle rating);
    void subscribeRoutesInfo(String controllerId);
    void unsubscribeRoutesInfo(String controllerId);
    void selectRoute(String controllerId, in Bundle route);
    void close(String controllerId);
}
