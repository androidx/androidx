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

package androidx.media.test.service;

import androidx.media.AudioAttributesCompat;
import androidx.media2.CommandResult2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.RemoteSessionPlayer2;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Mock implementation of {@link RemoteSessionPlayer2}.
 */
public class MockRemotePlayer extends RemoteSessionPlayer2 {
    public final CountDownLatch mLatch = new CountDownLatch(1);
    public boolean mSetVolumeToCalled;
    public boolean mAdjustVolumeCalled;
    public @VolumeControlType int mControlType;
    public int mCurrentVolume;
    public int mMaxVolume;
    public int mDirection;
    public AudioAttributesCompat mAttributes;

    public MockRemotePlayer(int controlType, int maxVolume, int currentVolume) {
        mControlType = controlType;
        mMaxVolume = maxVolume;
        mCurrentVolume = currentVolume;
    }

    @Override
    public ListenableFuture<CommandResult2> setVolume(int volume) {
        mSetVolumeToCalled = true;
        mCurrentVolume = volume;
        mLatch.countDown();
        return new SyncListenableFuture(null);
    }

    @Override
    public ListenableFuture<CommandResult2> adjustVolume(int direction) {
        mAdjustVolumeCalled = true;
        mDirection = direction;
        mLatch.countDown();
        return new SyncListenableFuture(null);
    }

    @Override
    public int getVolume() {
        return mCurrentVolume;
    }

    @Override
    public int getMaxVolume() {
        return mMaxVolume;
    }

    @Override
    public int getVolumeControlType() {
        return mControlType;
    }

    @Override
    public ListenableFuture<CommandResult2> play() {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> pause() {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> prepare() {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> seekTo(long position) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaybackSpeed(float playbackSpeed) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> setAudioAttributes(AudioAttributesCompat attributes) {
        mAttributes = attributes;
        return new SyncListenableFuture(null);
    }

    @Override
    public int getPlayerState() {
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public long getBufferedPosition() {
        return 0;
    }

    @Override
    public int getBufferingState() {
        return 0;
    }

    @Override
    public float getPlaybackSpeed() {
        return 0;
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaylist(List<MediaItem2> list,
            MediaMetadata2 metadata) {
        return null;
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAttributes;
    }

    @Override
    public ListenableFuture<CommandResult2> setMediaItem(MediaItem2 item) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> addPlaylistItem(int index, MediaItem2 item) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> removePlaylistItem(MediaItem2 item) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> replacePlaylistItem(int index, MediaItem2 item) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPreviousItem() {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> skipToNextItem() {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPlaylistItem(MediaItem2 item) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> updatePlaylistMetadata(MediaMetadata2 metadata) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> setRepeatMode(int repeatMode) {
        return null;
    }

    @Override
    public ListenableFuture<CommandResult2> setShuffleMode(int shuffleMode) {
        return null;
    }

    @Override
    public List<MediaItem2> getPlaylist() {
        return null;
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return null;
    }

    @Override
    public int getRepeatMode() {
        return 0;
    }

    @Override
    public int getShuffleMode() {
        return 0;
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return null;
    }

    @Override
    public void close() throws Exception {
    }
}
