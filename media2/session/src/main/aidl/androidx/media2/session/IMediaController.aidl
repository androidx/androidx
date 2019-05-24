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

import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.session.IMediaSession;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaSession to MediaController.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaController {
    void onCurrentMediaItemChanged(int seq, in ParcelImpl item, int currentIdx, int previousIdx,
            int nextIdx) = 0;
    void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs, int state) = 1;
    void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs, float speed) = 2;
    void onBufferingStateChanged(int seq, in ParcelImpl item, int state,
            long bufferedPositionMs, long eventTimeMs, long positionMs) = 3;
    void onPlaylistChanged(int seq, in ParcelImplListSlice listSlice, in ParcelImpl metadata,
            int currentIdx, int previousIdx, int nextIdx) = 4;
    void onPlaylistMetadataChanged(int seq, in ParcelImpl metadata) = 5;
    void onPlaybackInfoChanged(int seq, in ParcelImpl playbackInfo) = 6;
    void onRepeatModeChanged(int seq, int repeatMode, int currentIdx, int previousIdx,
            int nextIdx) = 7;
    void onShuffleModeChanged(int seq, int shuffleMode, int currentIdx, int previousIdx,
            int nextIdx) = 8;
    void onPlaybackCompleted(int seq) = 9;
    void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long seekPositionMs) = 10;
    void onVideoSizeChanged(int seq, in ParcelImpl item, in ParcelImpl videoSize) = 20;
    void onSubtitleData(int seq, in ParcelImpl item, in ParcelImpl track, in ParcelImpl data) = 24;

    void onConnected(int seq, in ParcelImpl connectionResult) = 11;
    void onDisconnected(int seq) = 12;

    void onSetCustomLayout(int seq, in List<ParcelImpl> commandButtonlist) = 13;
    void onAllowedCommandsChanged(int seq, in ParcelImpl commandGroup) = 14;
    void onCustomCommand(int seq, in ParcelImpl command, in Bundle args) = 15;

    void onSessionResult(int seq, in ParcelImpl sessionResult) = 16;
    void onLibraryResult(int seq, in ParcelImpl libraryResult) = 17;

    void onTrackInfoChanged(int seq, in List<ParcelImpl> trackInfos,
            in ParcelImpl selectedVideoTrack, in ParcelImpl selectedAudioTrack,
            in ParcelImpl selectedSubtitleTrack, in ParcelImpl selectedMetadataTrack) = 21;
    void onTrackSelected(int seq, in ParcelImpl trackInfo) = 22;
    void onTrackDeselected(int seq, in ParcelImpl trackInfo) = 23;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Browser sepcific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void onChildrenChanged(int seq, String parentId, int itemCount,
            in ParcelImpl libraryParams) = 18;
    void onSearchResultChanged(int seq, String query, int itemCount,
            in ParcelImpl libraryParams) = 19;
    // Next Id : 25
}
