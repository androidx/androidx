/* This file is auto-generated from OnboardingDemoFragment.java.  DO NOT MODIFY. */

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

import android.os.Bundle;
import android.support.v17.leanback.app.VideoFragmentGlueHost;
import android.support.v17.leanback.media.MediaPlayerGlue;
import android.support.v17.leanback.media.PlaybackGlue;

/**
 * Fragment demonstrating the use of {@link android.support.v17.leanback.app.VideoFragment} to
 * render video with playback controls.
 */
public class SampleSupportVideoFragment extends android.support.v17.leanback.app.VideoFragment {
    private MediaPlayerGlue mMediaPlayerGlue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlayerGlue = new MediaPlayerGlue(getActivity());
        mMediaPlayerGlue.setHost(new VideoFragmentGlueHost(this));
        mMediaPlayerGlue.setMode(MediaPlayerGlue.REPEAT_ALL);
    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaPlayerGlue.setArtist("A Googleer");
        mMediaPlayerGlue.setTitle("Diving with Sharks");
        mMediaPlayerGlue.setVideoUrl("http://techslides.com/demos/sample-videos/small.mp4");
        mMediaPlayerGlue.setPlayerCallback(new PlaybackGlue.PlayerCallback() {
            @Override
            public void onReadyForPlayback() {
                mMediaPlayerGlue.play();
            }
        });
    }
}
