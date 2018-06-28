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
import androidx.media2.BaseRemoteMediaPlayer;
import androidx.media2.DataSourceDesc2;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Mock implementation of {@link BaseRemoteMediaPlayer}.
 */
public class MockRemotePlayer extends BaseRemoteMediaPlayer {
    public final CountDownLatch mLatch = new CountDownLatch(1);
    public boolean mSetVolumeToCalled;
    public boolean mAdjustVolumeCalled;
    public int mControlType;
    public float mCurrentVolume;
    public float mMaxVolume;
    public int mDirection;
    public AudioAttributesCompat mAudioAttributes;

    public MockRemotePlayer(int controlType, float maxVolume, float currentVolume) {
        mControlType = controlType;
        mMaxVolume = maxVolume;
        mCurrentVolume = currentVolume;
    }

    @Override
    public void setPlayerVolume(float volume) {
        mSetVolumeToCalled = true;
        mCurrentVolume = volume;
        mLatch.countDown();
    }

    @Override
    public void adjustPlayerVolume(int direction) {
        mAdjustVolumeCalled = true;
        mDirection = direction;
        mLatch.countDown();
    }

    @Override
    public float getPlayerVolume() {
        return mCurrentVolume;
    }

    @Override
    public float getMaxPlayerVolume() {
        return mMaxVolume;
    }

    @Override
    public int getVolumeControlType() {
        return mControlType;
    }

    @Override
    public void play() {

    }

    @Override
    public void prepare() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void skipToNext() {

    }

    @Override
    public void seekTo(long pos) {

    }

    @Override
    public long getCurrentPosition() {
        return super.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return super.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return super.getBufferedPosition();
    }

    @Override
    public int getPlayerState() {
        return 0;
    }

    @Override
    public int getBufferingState() {
        return 0;
    }

    @Override
    public void setAudioAttributes(AudioAttributesCompat attributes) {
        mAudioAttributes = attributes;
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    public void setDataSource(DataSourceDesc2 dsd) {

    }

    @Override
    public void setNextDataSource(DataSourceDesc2 dsd) {

    }

    @Override
    public void setNextDataSources(List<DataSourceDesc2> dsds) {

    }

    @Override
    public DataSourceDesc2 getCurrentDataSource() {
        return null;
    }

    @Override
    public void loopCurrent(boolean loop) {

    }

    @Override
    public void setPlaybackSpeed(float speed) {

    }

    @Override
    public float getPlaybackSpeed() {
        return super.getPlaybackSpeed();
    }

    @Override
    public boolean isReversePlaybackSupported() {
        return super.isReversePlaybackSupported();
    }

    @Override
    public void registerPlayerEventCallback(Executor executor, PlayerEventCallback callback) {

    }

    @Override
    public void unregisterPlayerEventCallback(PlayerEventCallback callback) {

    }

    @Override
    public void close() throws Exception {

    }
}
