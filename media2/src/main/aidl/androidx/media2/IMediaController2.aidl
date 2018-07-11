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
import android.os.ResultReceiver;

import androidx.media2.IMediaSession2;
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
    void onPlaylistChanged(in List<ParcelImpl> playlist, in Bundle metadata) = 4;
    void onPlaylistMetadataChanged(in Bundle metadata) = 5;
    void onPlaybackInfoChanged(in ParcelImpl playbackInfo) = 6;
    void onRepeatModeChanged(int repeatMode) = 7;
    void onShuffleModeChanged(int shuffleMode) = 8;
    void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) = 9;
    void onError(int errorCode, in Bundle extras) = 10;
    void onRoutesInfoChanged(in List<Bundle> routes) = 11;

    void onConnected(IMediaSession2 sessionBinder, in ParcelImpl commandGroup, int playerState,
        in ParcelImpl currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed,
        long bufferedPositionMs, in ParcelImpl playbackInfo, int repeatMode, int shuffleMode,
        in List<ParcelImpl> playlist, in PendingIntent sessionActivity) = 12;
    void onDisconnected() = 13;

    void onCustomLayoutChanged(in List<ParcelImpl> commandButtonlist) = 14;
    void onAllowedCommandsChanged(in ParcelImpl commandGroup) = 15;

    void onCustomCommand(in ParcelImpl command, in Bundle args, in ResultReceiver receiver) = 16;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Browser sepcific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void onGetLibraryRootDone(in Bundle rootHints, String rootMediaId, in Bundle rootExtra) = 17;
    void onGetItemDone(String mediaId, in ParcelImpl item) = 18;
    void onChildrenChanged(String parentId, int itemCount, in Bundle extras) = 19;
    void onGetChildrenDone(String parentId, int page, int pageSize, in List<ParcelImpl> itemList,
        in Bundle extras) = 20;
    void onSearchResultChanged(String query, int itemCount, in Bundle extras) = 21;
    void onGetSearchResultDone(String query, int page, int pageSize, in List<ParcelImpl> itemList,
        in Bundle extras) = 22;
    // Next Id : 23
}
