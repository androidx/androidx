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
import android.os.ResultReceiver;

import androidx.versionedparcelable.ParcelImpl;

interface IRemoteMediaController {

    void create(boolean isBrowser, String controllerId, in ParcelImpl token,
            boolean waitForConnection);

    // MediaController Methods
    ParcelImpl getConnectedSessionToken(String controllerId);
    void play(String controllerId);
    void pause(String controllerId);
    void prepare(String controllerId);
    void seekTo(String controllerId, long pos);
    void setPlaybackSpeed(String controllerId, float speed);
    void setPlaylist(String controllerId, in List<String> list, in ParcelImpl metadata);
    void createAndSetDummyPlaylist(String controllerId, int size, in ParcelImpl metadata);
    void setMediaItem(String controllerId, in String mediaId);
    void updatePlaylistMetadata(String controllerId, in ParcelImpl metadata);
    void addPlaylistItem(String controllerId, int index, String mediaId);
    void removePlaylistItem(String controllerId, int index);
    void replacePlaylistItem(String controllerId, int index, String mediaId);
    void skipToPreviousItem(String controllerId);
    void skipToNextItem(String controllerId);
    void skipToPlaylistItem(String controllerId, int index);
    void setShuffleMode(String controllerId, int shuffleMode);
    void setRepeatMode(String controllerId, int repeatMode);
    void setVolumeTo(String controllerId, int value, int flags);
    void adjustVolume(String controllerId, int direction, int flags);
    void sendCustomCommand(String controllerId, in ParcelImpl command, in Bundle args);
    void fastForward(String controllerId);
    void rewind(String controllerId);
    void skipForward(String controllerId);
    void skipBackward(String controllerId);
    void playFromMediaId(String controllerId, String mediaId, in Bundle extras);
    void playFromSearch(String controllerId, String query, in Bundle extras);
    void playFromUri(String controllerId, in Uri uri, in Bundle extras);
    void prepareFromMediaId(String controllerId, String mediaId, in Bundle extras);
    void prepareFromSearch(String controllerId, String query, in Bundle extras);
    void prepareFromUri(String controllerId, in Uri uri, in Bundle extras);
    void setRating(String controllerId, String mediaId, in ParcelImpl rating);
    void close(String controllerId);

    // MediaBrowser methods
    void getLibraryRoot(String controllerId, in ParcelImpl libraryParams);
    void subscribe(String controllerId, String parentId, in ParcelImpl libraryParams);
    void unsubscribe(String controllerId, String parentId);
    void getChildren(String controllerId, String parentId, int page, int pageSize,
            in ParcelImpl libraryParams);
    void getItem(String controllerId, String mediaId);
    void search(String controllerId, String query, in ParcelImpl libraryParams);
    void getSearchResult(String controllerId, String query, int page, int pageSize,
            in ParcelImpl libraryParams);
}
