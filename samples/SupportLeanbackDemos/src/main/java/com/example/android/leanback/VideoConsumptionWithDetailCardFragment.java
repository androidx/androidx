/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.leanback;

import android.net.Uri;
import android.os.Bundle;

import androidx.leanback.app.VideoFragment;
import androidx.leanback.app.VideoFragmentGlueHost;
import androidx.leanback.media.MediaPlayerAdapter;
import androidx.leanback.media.PlaybackBannerControlGlue;
import androidx.leanback.media.PlaybackGlue;

/**
 * Fragment used as Control Glue's host
 */
public class VideoConsumptionWithDetailCardFragment extends VideoFragment {

    public static final String TAG = "VideoConsumptionWithDetailCardFragment";
    // A valid video URL to play video. So the progress bar can be seen to reproduce the bug.
    private static final String VIDEO_URL =
            "https://storage.googleapis.com/android-tv/Sample videos/"
                    + "April Fool's 2013/Explore Treasure Mode with Google Maps.mp4";
    public static final String TITLE = "Diving with Sharks";
    public static final String SUBTITLE = "A Googler";

    private PlaybackBannerControlGlue<MediaPlayerAdapter> mMediaPlayerGlue;
    final VideoFragmentGlueHost mHost = new VideoFragmentGlueHost(this);

    /**
     * helper function for playBackGlue to add/ remove callbacks
     *
     * @param glue The playback glue attached to this fragment
     */
    private static void playWhenReady(PlaybackGlue glue) {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int[] defuatSpeed = new int[]{1};
        mMediaPlayerGlue = new PlaybackBannerControlGlue<>(getActivity(), defuatSpeed,
                new MediaPlayerAdapter(getActivity()));
        // attach player glue to current host
        mMediaPlayerGlue.setHost(mHost);

        // add image resource to the PlaybackControlGlue
        mMediaPlayerGlue.setArt(getActivity().getDrawable(R.drawable.google_map));

        // meta information for video player
        mMediaPlayerGlue.setTitle(TITLE);
        mMediaPlayerGlue.setSubtitle(SUBTITLE);
        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(VIDEO_URL));
        playWhenReady(mMediaPlayerGlue);
        setBackgroundType(BG_LIGHT);
    }

    @Override
    public void onPause() {
        if (mMediaPlayerGlue != null) {
            mMediaPlayerGlue.pause();
        }
        super.onPause();
    }
}

