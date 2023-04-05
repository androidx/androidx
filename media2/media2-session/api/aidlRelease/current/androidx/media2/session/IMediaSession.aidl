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
interface IMediaSession {
  oneway void connect(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl connectionRequest) = 0;
  oneway void release(androidx.media2.session.IMediaController caller, int seq) = 1;
  oneway void setVolumeTo(androidx.media2.session.IMediaController caller, int seq, int value, int flags) = 2;
  oneway void adjustVolume(androidx.media2.session.IMediaController caller, int seq, int direction, int flags) = 3;
  oneway void play(androidx.media2.session.IMediaController caller, int seq) = 4;
  oneway void pause(androidx.media2.session.IMediaController caller, int seq) = 5;
  oneway void prepare(androidx.media2.session.IMediaController caller, int seq) = 6;
  oneway void fastForward(androidx.media2.session.IMediaController caller, int seq) = 7;
  oneway void rewind(androidx.media2.session.IMediaController caller, int seq) = 8;
  oneway void skipForward(androidx.media2.session.IMediaController caller, int seq) = 9;
  oneway void skipBackward(androidx.media2.session.IMediaController caller, int seq) = 10;
  oneway void seekTo(androidx.media2.session.IMediaController caller, int seq, long pos) = 11;
  oneway void onCustomCommand(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl sessionCommand, in android.os.Bundle args) = 12;
  oneway void setRating(androidx.media2.session.IMediaController caller, int seq, String mediaId, in androidx.versionedparcelable.ParcelImpl rating) = 19;
  oneway void setPlaybackSpeed(androidx.media2.session.IMediaController caller, int seq, float speed) = 20;
  oneway void setPlaylist(androidx.media2.session.IMediaController caller, int seq, in List<String> list, in androidx.versionedparcelable.ParcelImpl metadata) = 21;
  oneway void setMediaItem(androidx.media2.session.IMediaController caller, int seq, String mediaId) = 22;
  oneway void setMediaUri(androidx.media2.session.IMediaController caller, int seq, in android.net.Uri uri, in android.os.Bundle extras) = 44;
  oneway void updatePlaylistMetadata(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl metadata) = 23;
  oneway void addPlaylistItem(androidx.media2.session.IMediaController caller, int seq, int index, String mediaId) = 24;
  oneway void removePlaylistItem(androidx.media2.session.IMediaController caller, int seq, int index) = 25;
  oneway void replacePlaylistItem(androidx.media2.session.IMediaController caller, int seq, int index, String mediaId) = 26;
  oneway void movePlaylistItem(androidx.media2.session.IMediaController caller, int seq, int fromIndex, int toIndex) = 43;
  oneway void skipToPlaylistItem(androidx.media2.session.IMediaController caller, int seq, int index) = 27;
  oneway void skipToPreviousItem(androidx.media2.session.IMediaController caller, int seq) = 28;
  oneway void skipToNextItem(androidx.media2.session.IMediaController caller, int seq) = 29;
  oneway void setRepeatMode(androidx.media2.session.IMediaController caller, int seq, int repeatMode) = 30;
  oneway void setShuffleMode(androidx.media2.session.IMediaController caller, int seq, int shuffleMode) = 31;
  oneway void setSurface(androidx.media2.session.IMediaController caller, int seq, in android.view.Surface surface) = 40;
  oneway void selectTrack(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl trackInfo) = 41;
  oneway void deselectTrack(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl trackInfo) = 42;
  oneway void onControllerResult(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl controllerResult) = 32;
  oneway void getLibraryRoot(androidx.media2.session.IMediaController caller, int seq, in androidx.versionedparcelable.ParcelImpl libraryParams) = 33;
  oneway void getItem(androidx.media2.session.IMediaController caller, int seq, String mediaId) = 34;
  oneway void getChildren(androidx.media2.session.IMediaController caller, int seq, String parentId, int page, int pageSize, in androidx.versionedparcelable.ParcelImpl libraryParams) = 35;
  oneway void search(androidx.media2.session.IMediaController caller, int seq, String query, in androidx.versionedparcelable.ParcelImpl libraryParams) = 36;
  oneway void getSearchResult(androidx.media2.session.IMediaController caller, int seq, String query, int page, int pageSize, in androidx.versionedparcelable.ParcelImpl libraryParams) = 37;
  oneway void subscribe(androidx.media2.session.IMediaController caller, int seq, String parentId, in androidx.versionedparcelable.ParcelImpl libraryParams) = 38;
  oneway void unsubscribe(androidx.media2.session.IMediaController caller, int seq, String parentId) = 39;
}
