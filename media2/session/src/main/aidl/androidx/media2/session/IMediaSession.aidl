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

package androidx.media2.session;

import android.os.Bundle;
import android.net.Uri;
import android.view.Surface;

import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.session.IMediaController;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaController to MediaSession.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaSession {
    void connect(IMediaController caller, int seq, in ParcelImpl connectionRequest) = 0;
    void release(IMediaController caller, int seq) = 1;

    void setVolumeTo(IMediaController caller, int seq, int value, int flags) = 2;
    void adjustVolume(IMediaController caller, int seq, int direction, int flags) = 3;

    void play(IMediaController caller, int seq) = 4;
    void pause(IMediaController caller, int seq) = 5;
    void prepare(IMediaController caller, int seq) = 6;
    void fastForward(IMediaController caller, int seq) = 7;
    void rewind(IMediaController caller, int seq) = 8;
    void skipForward(IMediaController caller, int seq) = 9;
    void skipBackward(IMediaController caller, int seq) = 10;
    void seekTo(IMediaController caller, int seq, long pos) = 11;
    void onCustomCommand(IMediaController caller, int seq, in ParcelImpl sessionCommand,
            in Bundle args) = 12;
    // 13~18: removed
    void setRating(IMediaController caller, int seq, String mediaId, in ParcelImpl rating) = 19;
    void setPlaybackSpeed(IMediaController caller, int seq, float speed) = 20;

    void setPlaylist(IMediaController caller, int seq, in List<String> list,
            in ParcelImpl metadata) = 21;
    void setMediaItem(IMediaController caller, int seq, String mediaId) = 22;
    void setMediaUri(IMediaController caller, int seq, in Uri uri, in Bundle extras) = 44;
    void updatePlaylistMetadata(IMediaController caller, int seq, in ParcelImpl metadata) = 23;
    void addPlaylistItem(IMediaController caller, int seq, int index, String mediaId) = 24;
    void removePlaylistItem(IMediaController caller, int seq, int index) = 25;
    void replacePlaylistItem(IMediaController caller, int seq, int index, String mediaId) = 26;
    void movePlaylistItem(IMediaController caller, int seq, int fromIndex, int toIndex) = 43;
    void skipToPlaylistItem(IMediaController caller, int seq, int index) = 27;
    void skipToPreviousItem(IMediaController caller, int seq) = 28;
    void skipToNextItem(IMediaController caller, int seq) = 29;
    void setRepeatMode(IMediaController caller, int seq, int repeatMode) = 30;
    void setShuffleMode(IMediaController caller, int seq, int shuffleMode) = 31;
    void setSurface(IMediaController caller, int seq, in Surface surface) = 40;
    void selectTrack(IMediaController caller, int seq, in ParcelImpl trackInfo) = 41;
    void deselectTrack(IMediaController caller, int seq, in ParcelImpl trackInfo) = 42;

    void onControllerResult(IMediaController caller, int seq,
            in ParcelImpl controllerResult) = 32;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // library service specific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void getLibraryRoot(IMediaController caller, int seq, in ParcelImpl libraryParams) = 33;
    void getItem(IMediaController caller, int seq, String mediaId) = 34;
    void getChildren(IMediaController caller, int seq, String parentId, int page, int pageSize,
            in ParcelImpl libraryParams) = 35;
    void search(IMediaController caller, int seq, String query, in ParcelImpl libraryParams) = 36;
    void getSearchResult(IMediaController caller, int seq, String query, int page, int pageSize,
            in ParcelImpl libraryParams) = 37;
    void subscribe(IMediaController caller, int seq, String parentId,
            in ParcelImpl libraryParams) = 38;
    void unsubscribe(IMediaController caller, int seq, String parentId) = 39;
    // Next Id : 45
}
