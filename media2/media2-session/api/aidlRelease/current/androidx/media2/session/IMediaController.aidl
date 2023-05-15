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
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package androidx.media2.session;
/* @hide */
interface IMediaController {
  oneway void onCurrentMediaItemChanged(int seq, in androidx.versionedparcelable.ParcelImpl item, int currentIdx, int previousIdx, int nextIdx) = 0;
  oneway void onPlayerStateChanged(int seq, long eventTimeMs, long positionMs, int state) = 1;
  oneway void onPlaybackSpeedChanged(int seq, long eventTimeMs, long positionMs, float speed) = 2;
  oneway void onBufferingStateChanged(int seq, in androidx.versionedparcelable.ParcelImpl item, int state, long bufferedPositionMs, long eventTimeMs, long positionMs) = 3;
  oneway void onPlaylistChanged(int seq, in androidx.media2.common.ParcelImplListSlice listSlice, in androidx.versionedparcelable.ParcelImpl metadata, int currentIdx, int previousIdx, int nextIdx) = 4;
  oneway void onPlaylistMetadataChanged(int seq, in androidx.versionedparcelable.ParcelImpl metadata) = 5;
  oneway void onPlaybackInfoChanged(int seq, in androidx.versionedparcelable.ParcelImpl playbackInfo) = 6;
  oneway void onRepeatModeChanged(int seq, int repeatMode, int currentIdx, int previousIdx, int nextIdx) = 7;
  oneway void onShuffleModeChanged(int seq, int shuffleMode, int currentIdx, int previousIdx, int nextIdx) = 8;
  oneway void onPlaybackCompleted(int seq) = 9;
  oneway void onSeekCompleted(int seq, long eventTimeMs, long positionMs, long seekPositionMs) = 10;
  oneway void onVideoSizeChanged(int seq, in androidx.versionedparcelable.ParcelImpl item, in androidx.versionedparcelable.ParcelImpl videoSize) = 20;
  oneway void onSubtitleData(int seq, in androidx.versionedparcelable.ParcelImpl item, in androidx.versionedparcelable.ParcelImpl track, in androidx.versionedparcelable.ParcelImpl data) = 24;
  oneway void onConnected(int seq, in androidx.versionedparcelable.ParcelImpl connectionResult) = 11;
  oneway void onDisconnected(int seq) = 12;
  oneway void onSetCustomLayout(int seq, in List<androidx.versionedparcelable.ParcelImpl> commandButtonlist) = 13;
  oneway void onAllowedCommandsChanged(int seq, in androidx.versionedparcelable.ParcelImpl commandGroup) = 14;
  oneway void onCustomCommand(int seq, in androidx.versionedparcelable.ParcelImpl command, in android.os.Bundle args) = 15;
  oneway void onSessionResult(int seq, in androidx.versionedparcelable.ParcelImpl sessionResult) = 16;
  oneway void onLibraryResult(int seq, in androidx.versionedparcelable.ParcelImpl libraryResult) = 17;
  oneway void onTrackInfoChanged(int seq, in List<androidx.versionedparcelable.ParcelImpl> trackInfos, in androidx.versionedparcelable.ParcelImpl selectedVideoTrack, in androidx.versionedparcelable.ParcelImpl selectedAudioTrack, in androidx.versionedparcelable.ParcelImpl selectedSubtitleTrack, in androidx.versionedparcelable.ParcelImpl selectedMetadataTrack) = 21;
  oneway void onTrackSelected(int seq, in androidx.versionedparcelable.ParcelImpl trackInfo) = 22;
  oneway void onTrackDeselected(int seq, in androidx.versionedparcelable.ParcelImpl trackInfo) = 23;
  oneway void onChildrenChanged(int seq, String parentId, int itemCount, in androidx.versionedparcelable.ParcelImpl libraryParams) = 18;
  oneway void onSearchResultChanged(int seq, String query, int itemCount, in androidx.versionedparcelable.ParcelImpl libraryParams) = 19;
}
