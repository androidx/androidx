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

import android.app.PendingIntent;
import android.os.Bundle;

import androidx.media2.IMediaSession2;
import androidx.media2.ParcelImplListSlice;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaSession2 to MediaController2.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaController2 {
    void onCurrentMediaItemChanged(in ParcelImpl item) = 0;
    void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) = 1;
    void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) = 2;
    void onBufferingStateChanged(in ParcelImpl item, int state, long bufferedPositionMs) = 3;
    void onPlaylistChanged(in ParcelImplListSlice listSlice, in ParcelImpl metadata) = 4;
    void onPlaylistMetadataChanged(in ParcelImpl metadata) = 5;
    void onPlaybackInfoChanged(in ParcelImpl playbackInfo) = 6;
    void onRepeatModeChanged(int repeatMode) = 7;
    void onShuffleModeChanged(int shuffleMode) = 8;
    void onPlaybackCompleted() = 23;
    void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) = 9;

    void onConnected(IMediaSession2 sessionBinder, in ParcelImpl commandGroup, int playerState,
        in ParcelImpl currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed,
        long bufferedPositionMs, in ParcelImpl playbackInfo, int repeatMode, int shuffleMode,
        in ParcelImplListSlice listSlice, in PendingIntent sessionActivity) = 12;
    void onDisconnected() = 13;

    void onSetCustomLayout(int seq, in List<ParcelImpl> commandButtonlist) = 14;
    void onAllowedCommandsChanged(in ParcelImpl commandGroup) = 15;
    void onCustomCommand(int seq, in ParcelImpl command, in Bundle args) = 16;

    void onSessionResult(int seq, in ParcelImpl sessionResult) = 24;
    void onLibraryResult(int seq, in ParcelImpl libraryResult) = 25;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Browser sepcific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void onChildrenChanged(String parentId, int itemCount, in ParcelImpl libraryParams) = 19;
    void onSearchResultChanged(String query, int itemCount, in ParcelImpl libraryParams) = 21;
    // Next Id : 26
}
