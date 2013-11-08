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

package com.example.android.supportv7.media;

import com.example.android.supportv7.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.MediaRouteControllerDialog;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This class serves as an example on how to customize the media router control
 * dialog. It is derived from the standard MediaRouteControllerDialog with the
 * following overrides:
 *
 *   1. Shows thumbnail/snapshot of the current item
 *
 *   2. For variable volume routes, only allow volume control via Volume Up/Down
 *      keys (to prevent accidental tapping on the volume adjust seekbar that sets
 *      volume to maximum)
 *
 *   3. Provides transport control buttons (play/pause, stop)
 */
public class SampleMediaRouteControllerDialog extends MediaRouteControllerDialog {
    private static final String TAG = "SampleMediaRouteControllerDialog";
    private final SampleMediaRouterActivity mActivity;
    private final SessionManager mSessionManager;
    private final Player mPlayer;
    private ImageButton mPauseResumeButton;
    private ImageButton mStopButton;
    private ImageView mThumbnail;
    private TextView mTextView;
    private LinearLayout mInfoLayout;
    private LinearLayout mVolumeLayout;

    public SampleMediaRouteControllerDialog(Context context,
            SessionManager manager, Player player) {
        super(context);
        mActivity = (SampleMediaRouterActivity) context;
        mSessionManager = manager;
        mPlayer = player;
    }

    @Override
    public View onCreateMediaControlView(Bundle savedInstanceState) {
        // Thumbnail and Track info
        View v = getLayoutInflater().inflate(R.layout.sample_media_controller, null);
        mInfoLayout = (LinearLayout)v.findViewById(R.id.media_route_info);
        mTextView = (TextView)v.findViewById(R.id.track_info);
        mThumbnail = (ImageView)v.findViewById(R.id.snapshot);

        // Transport controls
        mPauseResumeButton = (ImageButton)v.findViewById(R.id.pause_resume_button);
        mPauseResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity != null) {
                    mActivity.handleMediaKey(new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                }
            }
        });

        mStopButton = (ImageButton)v.findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActivity != null) {
                    mActivity.handleMediaKey(new KeyEvent(KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_MEDIA_STOP));
                }
            }
        });

        // update session status (will callback to updateUi at the end)
        mSessionManager.updateStatus();
        return v;
    }

    public void updateUi() {
        String trackInfo = mPlayer.getDescription();
        Bitmap snapshot = mPlayer.getSnapshot();
        if (mPlayer.isRemotePlayback() && !trackInfo.isEmpty() && snapshot != null) {
            mInfoLayout.setVisibility(View.VISIBLE);
            mThumbnail.setImageBitmap(snapshot);
            mTextView.setText(trackInfo);
        } else {
            mInfoLayout.setVisibility(View.GONE);
        }
        // show pause or resume icon depending on current state
        mPauseResumeButton.setImageResource(mSessionManager.isPaused() ?
                R.drawable.ic_media_play : R.drawable.ic_media_pause);
    }
}
