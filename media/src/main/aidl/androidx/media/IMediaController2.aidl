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

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.media.IMediaSession2;

/**
 * Interface from MediaSession2 to MediaController2.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaController2 {
    void onCurrentMediaItemChanged(in Bundle item);
    void onPlayerStateChanged(long eventTimeMs, long positionMs, int state);
    void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed);
    void onBufferingStateChanged(in Bundle item, int state, long bufferedPositionMs);
    void onPlaylistChanged(in List<Bundle> playlist, in Bundle metadata);
    void onPlaylistMetadataChanged(in Bundle metadata);
    void onPlaybackInfoChanged(in Bundle playbackInfo);
    void onRepeatModeChanged(int repeatMode);
    void onShuffleModeChanged(int shuffleMode);
    void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs);
    void onError(int errorCode, in Bundle extras);
    void onRoutesInfoChanged(in List<Bundle> routes);

    void onConnected(IMediaSession2 sessionBinder, in Bundle commandGroup, int playerState,
        in Bundle currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed,
        long bufferedPositionMs, in Bundle playbackInfo, int repeatMode, int shuffleMode,
        in List<Bundle> playlist, in PendingIntent sessionActivity);
    void onDisconnected();

    void onCustomLayoutChanged(in List<Bundle> commandButtonlist);
    void onAllowedCommandsChanged(in Bundle commands);

    void onCustomCommand(in Bundle command, in Bundle args, in ResultReceiver receiver);

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Browser sepcific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void onGetLibraryRootDone(in Bundle rootHints, String rootMediaId, in Bundle rootExtra);
    void onGetItemDone(String mediaId, in Bundle result);
    void onChildrenChanged(String parentId, int itemCount, in Bundle extras);
    void onGetChildrenDone(String parentId, int page, int pageSize, in List<Bundle> result,
        in Bundle extras);
    void onSearchResultChanged(String query, int itemCount, in Bundle extras);
    void onGetSearchResultDone(String query, int page, int pageSize, in List<Bundle> result,
        in Bundle extras);
}
