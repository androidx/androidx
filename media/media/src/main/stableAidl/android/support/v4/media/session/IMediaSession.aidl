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

package android.support.v4.media.session;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.ParcelableVolumeInfo;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;

import java.util.List;

/**
 * Interface to a MediaSessionCompat.
 * @hide
 */
interface IMediaSession {
    // Next ID: 50
    void sendCommand(String command, in Bundle args, in MediaSessionCompat.ResultReceiverWrapper cb) = 0;
    boolean sendMediaButton(in KeyEvent mediaButton) = 1;
    void registerCallbackListener(in IMediaControllerCallback cb) = 2;
    void unregisterCallbackListener(in IMediaControllerCallback cb) = 3;
    boolean isTransportControlEnabled() = 4;
    String getPackageName() = 5;
    String getTag() = 6;
    PendingIntent getLaunchPendingIntent() = 7;
    long getFlags() = 8;
    ParcelableVolumeInfo getVolumeAttributes() = 9;
    void adjustVolume(int direction, int flags, String packageName) = 10;
    void setVolumeTo(int value, int flags, String packageName) = 11;
    MediaMetadataCompat getMetadata() = 26;
    PlaybackStateCompat getPlaybackState() = 27;
    List<MediaSessionCompat.QueueItem> getQueue() = 28;
    CharSequence getQueueTitle() = 29;
    Bundle getExtras() = 30;
    int getRatingType() = 31;
    boolean isCaptioningEnabled() = 44;
    int getRepeatMode() = 36;
    boolean isShuffleModeEnabledRemoved() = 37;
    int getShuffleMode() = 46;
    void addQueueItem(in MediaDescriptionCompat description) = 40;
    void addQueueItemAt(in MediaDescriptionCompat description, int index) = 41;
    void removeQueueItem(in MediaDescriptionCompat description) = 42;
    void removeQueueItemAt(int index) = 43;
    Bundle getSessionInfo() = 49;

    // These commands are for the TransportControls
    void prepare() = 32;
    void prepareFromMediaId(String uri, in Bundle extras) = 33;
    void prepareFromSearch(String string, in Bundle extras) = 34;
    void prepareFromUri(in Uri uri, in Bundle extras) = 35;
    void play() = 12;
    void playFromMediaId(String uri, in Bundle extras) = 13;
    void playFromSearch(String string, in Bundle extras) = 14;
    void playFromUri(in Uri uri, in Bundle extras) = 15;
    void skipToQueueItem(long id) = 16;
    void pause() = 17;
    void stop() = 18;
    void next() = 19;
    void previous() = 20;
    void fastForward() = 21;
    void rewind() = 22;
    void seekTo(long pos) = 23;
    void rate(in RatingCompat rating) = 24;
    void rateWithExtras(in RatingCompat rating, in Bundle extras) = 50;
    void setPlaybackSpeed(float speed) = 48;
    void setCaptioningEnabled(boolean enabled) = 45;
    void setRepeatMode(int repeatMode) = 38;
    void setShuffleModeEnabledRemoved(boolean shuffleMode) = 39;
    void setShuffleMode(int shuffleMode) = 47;
    void sendCustomAction(String action, in Bundle args) = 25;
}
