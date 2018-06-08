/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.leanback.app.VideoFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackGlue;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.PlaybackControlsRow;

/**
 * Fragment demonstrating the use of {@link androidx.leanback.app.VideoFragment} to
 * render video with playback controls. And demonstrates video seeking with thumbnails.
 *
 * Generate 1 frame per second thumbnail bitmaps and put on sdcard:
 * <pre>
 * sudo apt-get install libav-tools
 * avconv -i input.mp4 -s 240x135 -vsync 1 -r 1 -an -y -qscale 8 frame_%04d.jpg
 * adb shell mkdir /sdcard/seek
 * adb push frame_*.jpg /sdcard/seek/
 * </pre>
 * Change to 1 frame per minute: use "-r 1/60".
 * For more options, see https://wiki.libav.org/Snippets/avconv
 *
 * <p>
 * Showcase:
 * </p>
 * <li>Auto play when ready</li>
 * <li>Set seek provider</li>
 * <li>switch MediaSource</li>
 * <li>switch PlaybackGlue</li>
 */
public class SampleVideoFragment extends androidx.leanback.app.VideoFragment {

    // Media Session Token
    private static final String MEDIA_SESSION_COMPAT_TOKEN = "media session support video";

    private PlaybackTransportControlGlueSample<MediaPlayerAdapter> mMediaPlayerGlue;

    private MediaSessionCompat mMediaSessionCompat;

    final VideoFragmentGlueHost mHost = new VideoFragmentGlueHost(SampleVideoFragment.this);

    static void playWhenReady(PlaybackGlue glue) {
        if (glue.isPrepared()) {
            glue.play();
        } else {
            glue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    if (glue.isPrepared()) {
                        glue.removePlayerCallback(this);
                        glue.play();
                    }
                }
            });
        }
    }

    static void loadSeekData(final PlaybackTransportControlGlue glue) {
        if (glue.isPrepared()) {
            glue.setSeekProvider(new PlaybackSeekDiskDataProvider(
                    glue.getDuration(),
                    1000,
                    "/sdcard/seek/frame_%04d.jpg"));
        } else {
            glue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
                @Override
                public void onPreparedStateChanged(PlaybackGlue glue) {
                    if (glue.isPrepared()) {
                        glue.removePlayerCallback(this);
                        PlaybackTransportControlGlue transportControlGlue =
                                (PlaybackTransportControlGlue) glue;
                        transportControlGlue.setSeekProvider(new PlaybackSeekDiskDataProvider(
                                transportControlGlue.getDuration(),
                                1000,
                                "/sdcard/seek/frame_%04d.jpg"));
                    }
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlayerGlue = new PlaybackTransportControlGlueSample(getActivity(),
                new MediaPlayerAdapter(getActivity()));

        // create a media session inside of a fragment, and app developer can determine if connect
        // this media session to glue or not
        // as requested in b/64935838
        mMediaSessionCompat = new MediaSessionCompat(getActivity(), MEDIA_SESSION_COMPAT_TOKEN);
        mMediaPlayerGlue.connectToMediaSession(mMediaSessionCompat);

        mMediaPlayerGlue.setHost(mHost);
        mMediaPlayerGlue.setMode(PlaybackControlsRow.RepeatAction.INDEX_NONE);
        mMediaPlayerGlue.addPlayerCallback(new PlaybackGlue.PlayerCallback() {
            boolean mSecondCompleted = false;
            @Override
            public void onPlayCompleted(PlaybackGlue glue) {
                if (!mSecondCompleted) {
                    mSecondCompleted = true;
                    mMediaPlayerGlue.setSubtitle("Leanback artist Changed!");
                    mMediaPlayerGlue.setTitle("Leanback team at work");
                    String uriPath = "https://storage.googleapis.com/android-tv/Sample videos/"
                            + "April Fool's 2013/Explore Treasure Mode with Google Maps.mp4";
                    mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(uriPath));
                    loadSeekData(mMediaPlayerGlue);
                    playWhenReady(mMediaPlayerGlue);
                } else {
                    mMediaPlayerGlue.removePlayerCallback(this);
                    switchAnotherGlue();
                }
            }
        });
        mMediaPlayerGlue.setSubtitle("Leanback artist");
        mMediaPlayerGlue.setTitle("Leanback team at work");
        String uriPath = "https://storage.googleapis.com/android-tv/Sample videos/"
                + "April Fool's 2013/Explore Treasure Mode with Google Maps.mp4";
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(uriPath));
        loadSeekData(mMediaPlayerGlue);
        playWhenReady(mMediaPlayerGlue);
    }

    @Override
    public void onPause() {
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayerGlue.disconnectToMediaSession();
    }

    void switchAnotherGlue() {
        mMediaPlayerGlue = new PlaybackTransportControlGlueSample(getActivity(),
                new MediaPlayerAdapter(getActivity()));

        // If the glue is switched, re-register the media session
        mMediaPlayerGlue.connectToMediaSession(mMediaSessionCompat);

        mMediaPlayerGlue.setMode(PlaybackControlsRow.RepeatAction.INDEX_ONE);
        mMediaPlayerGlue.setSubtitle("A Googler");
        mMediaPlayerGlue.setTitle("Swimming with the fishes");
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(
                Uri.parse("http://techslides.com/demos/sample-videos/small.mp4"));
        mMediaPlayerGlue.setHost(mHost);
        loadSeekData(mMediaPlayerGlue);
        playWhenReady(mMediaPlayerGlue);
    }
}
