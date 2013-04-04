/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.media;

import android.view.KeyEvent;
import com.example.android.supportv4.R;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import android.support.v4.media.TransportController;

public class TransportControllerActivity extends Activity {

    /**
     * TODO: Set the path variable to a streaming video URL or a local media
     * file path.
     */
    private VideoView mVideoView;
    private TransportController mTransportController;

    /**
     * Handle media buttons to start/stop video playback.  Real implementations
     * will probably handle more buttons, like skip and fast-forward.
     */
    public class PlayerControlCallbacks extends TransportController.Callbacks {
        public boolean onMediaButtonDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case TransportController.KEYCODE_MEDIA_PLAY:
                    mVideoView.start();
                    return true;
                case TransportController.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    mVideoView.pause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (mVideoView.isPlaying()) {
                        mVideoView.pause();
                    } else {
                        mVideoView.start();
                    }
            }
            return true;
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.videoview);

        // Find the video player in our UI.
        mVideoView = (VideoView) findViewById(R.id.surface_view);

        // Create transport controller to control video; use the standard
        // control callbacks that knows how to talk to a MediaPlayerControl.
        mTransportController = new TransportController(this, new PlayerControlCallbacks());

        // We're just playing a built-in demo video.
        mVideoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.videoviewdemo));
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }

        // If the UI didn't handle the key, give the transport controller
        // a crack at it.
        return mTransportController.dispatchKeyEvent(event);
    }
}
