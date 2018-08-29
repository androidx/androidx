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

package androidx.media.widget;

import androidx.media.AudioAttributesCompat;
import androidx.media2.DataSourceDesc2;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaPlayerConnector;

import java.util.List;
import java.util.concurrent.Executor;

class VideoView2Player extends MediaPlayerConnector {
    private MediaPlayer2 mMediaPlayer2;
    private MediaPlayerConnector mMediaPlayerConnector;

    VideoView2Player(MediaPlayer2 mediaPlayer2) {
        mMediaPlayer2 = mediaPlayer2;
        mMediaPlayerConnector = mMediaPlayer2.getMediaPlayerConnector();
    }

    @Override
    public void play() {
        mMediaPlayer2.play();
    }

    @Override
    public void prepare() {
        mMediaPlayer2.prepare();
    }

    @Override
    public void pause() {
        mMediaPlayer2.pause();
    }

    @Override
    public void reset() {
        mMediaPlayer2.reset();
    }

    @Override
    public void skipToNext() {
        mMediaPlayer2.skipToNext();
    }

    @Override
    public void seekTo(long pos) {
        mMediaPlayer2.seekTo(pos, MediaPlayer2.SEEK_CLOSEST);
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer2.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mMediaPlayer2.getDuration();
    }

    @Override
    public long getBufferedPosition() {
        return mMediaPlayer2.getBufferedPosition();
    }

    @Override
    public int getPlayerState() {
        return mMediaPlayerConnector.getPlayerState();
    }

    @Override
    public int getBufferingState() {
        return mMediaPlayerConnector.getBufferingState();
    }

    @Override
    public void setAudioAttributes(AudioAttributesCompat attributes) {
        mMediaPlayer2.setAudioAttributes(attributes);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mMediaPlayer2.getAudioAttributes();
    }

    @Override
    public void setDataSource(DataSourceDesc2 dsd) {
        mMediaPlayer2.setDataSource(dsd);
    }

    @Override
    public void setNextDataSource(DataSourceDesc2 dsd) {
        mMediaPlayer2.setNextDataSource(dsd);
    }

    @Override
    public void setNextDataSources(List<DataSourceDesc2> dsds) {
        mMediaPlayer2.setNextDataSources(dsds);
    }

    @Override
    public DataSourceDesc2 getCurrentDataSource() {
        return mMediaPlayer2.getCurrentDataSource();
    }

    @Override
    public void loopCurrent(boolean loop) {
        mMediaPlayer2.loopCurrent(loop);
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        mMediaPlayerConnector.setPlaybackSpeed(speed);
    }

    @Override
    public void setPlayerVolume(float volume) {
        mMediaPlayer2.setPlayerVolume(volume);
    }

    @Override
    public float getPlayerVolume() {
        return mMediaPlayer2.getPlayerVolume();
    }

    @Override
    public void registerPlayerEventCallback(Executor e,
            PlayerEventCallback cb) {
        mMediaPlayerConnector.registerPlayerEventCallback(e, cb);
    }

    @Override
    public void unregisterPlayerEventCallback(PlayerEventCallback cb) {
        mMediaPlayerConnector.unregisterPlayerEventCallback(cb);
    }

    @Override
    public void close() {
        mMediaPlayer2.close();
    }
}
