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

import android.os.Bundle;
import android.net.Uri;

import androidx.media2.IMediaController;
import androidx.media2.ParcelImplListSlice;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaController to MediaSession.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaSession {
    void connect(IMediaController caller, int seq, String callingPackage) = 0;
    void release(IMediaController caller, int seq) = 1;

    void setVolumeTo(IMediaController caller, int seq, int value, int flags) = 2;
    void adjustVolume(IMediaController caller, int seq, int direction, int flags) = 3;

    void play(IMediaController caller, int seq) = 4;
    void pause(IMediaController caller, int seq) = 5;
    void prepare(IMediaController caller, int seq) = 7;
    void fastForward(IMediaController caller, int seq) = 8;
    void rewind(IMediaController caller, int seq) = 9;
    void skipForward(IMediaController caller, int seq) = 10;
    void skipBackward(IMediaController caller, int seq) = 11;
    void seekTo(IMediaController caller, int seq, long pos) = 12;
    void onCustomCommand(IMediaController caller, int seq, in ParcelImpl sessionCommand,
            in Bundle args) = 13;
    void prepareFromUri(IMediaController caller, int seq, in Uri uri, in Bundle extras) = 14;
    void prepareFromSearch(IMediaController caller, int seq, String query, in Bundle extras) = 15;
    void prepareFromMediaId(IMediaController caller, int seq, String mediaId,
            in Bundle extras) = 16;
    void playFromUri(IMediaController caller, int seq, in Uri uri, in Bundle extras) = 17;
    void playFromSearch(IMediaController caller, int seq, String query, in Bundle extras) = 18;
    void playFromMediaId(IMediaController caller, int seq, String mediaId, in Bundle extras) = 19;
    void setRating(IMediaController caller, int seq, String mediaId, in ParcelImpl rating) = 20;
    void setPlaybackSpeed(IMediaController caller, int seq, float speed) = 21;

    void setPlaylist(IMediaController caller, int seq, in List<String> list,
            in ParcelImpl metadata) = 22;
    void setMediaItem(IMediaController caller, int seq, String mediaId) = 23;
    void updatePlaylistMetadata(IMediaController caller, int seq, in ParcelImpl metadata) = 24;
    void addPlaylistItem(IMediaController caller, int seq, int index, String mediaId) = 25;
    void removePlaylistItem(IMediaController caller, int seq, int index) = 26;
    void replacePlaylistItem(IMediaController caller, int seq, int index, String mediaId) = 27;
    void skipToPlaylistItem(IMediaController caller, int seq, int index) = 28;
    void skipToPreviousItem(IMediaController caller, int seq) = 29;
    void skipToNextItem(IMediaController caller, int seq) = 30;
    void setRepeatMode(IMediaController caller, int seq, int repeatMode) = 31;
    void setShuffleMode(IMediaController caller, int seq, int shuffleMode) = 32;

    void onControllerResult(IMediaController caller, int seq,
            in ParcelImpl controllerResult) = 46;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // library service specific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void getLibraryRoot(IMediaController caller, int seq, in ParcelImpl libraryParams) = 37;
    void getItem(IMediaController caller, int seq, String mediaId) = 38;
    void getChildren(IMediaController caller, int seq, String parentId, int page, int pageSize,
            in ParcelImpl libraryParams) = 39;
    void search(IMediaController caller, int seq, String query, in ParcelImpl libraryParams) = 40;
    void getSearchResult(IMediaController caller, int seq, String query, int page, int pageSize,
            in ParcelImpl libraryParams) = 41;
    void subscribe(IMediaController caller, int seq, String parentId,
            in ParcelImpl libraryParams) = 42;
    void unsubscribe(IMediaController caller, int seq, String parentId) = 43;
    // Next Id : 44
}
