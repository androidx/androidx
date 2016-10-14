/* This file is auto-generated from VideoFragment.java.  DO NOT MODIFY. */

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
package android.support.v17.leanback.app;

import android.os.Bundle;
import android.support.v17.leanback.R;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Subclass of {@link PlaybackSupportFragment} that is responsible for providing a {@link SurfaceView}
 * and rendering video.
 */
public class VideoSupportFragment extends PlaybackSupportFragment {
    private SurfaceView mVideoSurface;
    private SurfaceHolder.Callback mMediaPlaybackCallback;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        mVideoSurface = (SurfaceView) inflater.inflate(R.layout.lb_video_surface, container, false);
        ((ViewGroup) root.findViewById(R.id.playback_fragment_root)).addView(mVideoSurface, 0);
        mVideoSurface.getHolder().addCallback(mMediaPlaybackCallback);
        setBackgroundType(PlaybackSupportFragment.BG_LIGHT);
        return root;
    }

    /**
     * Adds {@link SurfaceHolder.Callback} to {@link android.view.SurfaceView}.
     */
    public void setSurfaceHolderCallback(SurfaceHolder.Callback callback) {
        if (mVideoSurface != null && mMediaPlaybackCallback != null) {
            mVideoSurface.getHolder().removeCallback(mMediaPlaybackCallback);
        }

        mMediaPlaybackCallback = callback;
        if (mVideoSurface != null && mMediaPlaybackCallback != null) {
            mVideoSurface.getHolder().addCallback(mMediaPlaybackCallback);
        }
    }
}
