/* Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.media.session;
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IMediaSession {
  void sendCommand(String command, in android.os.Bundle args, in android.support.v4.media.session.MediaSessionCompat.ResultReceiverWrapper cb) = 0;
  boolean sendMediaButton(in android.view.KeyEvent mediaButton) = 1;
  void registerCallbackListener(in android.support.v4.media.session.IMediaControllerCallback cb) = 2;
  void unregisterCallbackListener(in android.support.v4.media.session.IMediaControllerCallback cb) = 3;
  boolean isTransportControlEnabled() = 4;
  String getPackageName() = 5;
  String getTag() = 6;
  android.app.PendingIntent getLaunchPendingIntent() = 7;
  long getFlags() = 8;
  android.support.v4.media.session.ParcelableVolumeInfo getVolumeAttributes() = 9;
  void adjustVolume(int direction, int flags, String packageName) = 10;
  void setVolumeTo(int value, int flags, String packageName) = 11;
  android.support.v4.media.MediaMetadataCompat getMetadata() = 26;
  android.support.v4.media.session.PlaybackStateCompat getPlaybackState() = 27;
  List<android.support.v4.media.session.MediaSessionCompat.QueueItem> getQueue() = 28;
  CharSequence getQueueTitle() = 29;
  android.os.Bundle getExtras() = 30;
  int getRatingType() = 31;
  boolean isCaptioningEnabled() = 44;
  int getRepeatMode() = 36;
  boolean isShuffleModeEnabledRemoved() = 37;
  int getShuffleMode() = 46;
  void addQueueItem(in android.support.v4.media.MediaDescriptionCompat description) = 40;
  void addQueueItemAt(in android.support.v4.media.MediaDescriptionCompat description, int index) = 41;
  void removeQueueItem(in android.support.v4.media.MediaDescriptionCompat description) = 42;
  void removeQueueItemAt(int index) = 43;
  android.os.Bundle getSessionInfo() = 49;
  void prepare() = 32;
  void prepareFromMediaId(String uri, in android.os.Bundle extras) = 33;
  void prepareFromSearch(String string, in android.os.Bundle extras) = 34;
  void prepareFromUri(in android.net.Uri uri, in android.os.Bundle extras) = 35;
  void play() = 12;
  void playFromMediaId(String uri, in android.os.Bundle extras) = 13;
  void playFromSearch(String string, in android.os.Bundle extras) = 14;
  void playFromUri(in android.net.Uri uri, in android.os.Bundle extras) = 15;
  void skipToQueueItem(long id) = 16;
  void pause() = 17;
  void stop() = 18;
  void next() = 19;
  void previous() = 20;
  void fastForward() = 21;
  void rewind() = 22;
  void seekTo(long pos) = 23;
  void rate(in android.support.v4.media.RatingCompat rating) = 24;
  void rateWithExtras(in android.support.v4.media.RatingCompat rating, in android.os.Bundle extras) = 50;
  void setPlaybackSpeed(float speed) = 48;
  void setCaptioningEnabled(boolean enabled) = 45;
  void setRepeatMode(int repeatMode) = 38;
  void setShuffleModeEnabledRemoved(boolean shuffleMode) = 39;
  void setShuffleMode(int shuffleMode) = 47;
  void sendCustomAction(String action, in android.os.Bundle args) = 25;
}
