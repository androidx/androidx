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

import com.example.android.supportv4.R;

import android.app.ActionBar;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import android.support.v4.media.TransportController;

public class TransportControllerActivity extends Activity {

    /**
     * TODO: Set the path variable to a streaming video URL or a local media
     * file path.
     */
    private Content mContent;
    private TransportController mTransportController;
    private MediaController mMediaController;

    /**
     * Handle media buttons to start/stop video playback.  Real implementations
     * will probably handle more buttons, like skip and fast-forward.
     */
    TransportController.Callbacks mTransportCallbacks = new TransportController.Callbacks() {
        public boolean onMediaButtonDown(int keyCode, KeyEvent event) {
            switch (keyCode) {
                case TransportController.KEYCODE_MEDIA_PLAY:
                    mMediaPlayerControl.start();
                    return true;
                case TransportController.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    mMediaPlayerControl.pause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (mContent.isPlaying()) {
                        mMediaPlayerControl.pause();
                    } else {
                        mMediaPlayerControl.start();
                    }
            }
            return true;
        }
    };

    /**
     * Handle actions from on-screen media controls.  Most of these are simple re-directs
     * to the VideoView; some we need to capture to update our state.
     */
    MediaController.MediaPlayerControl mMediaPlayerControl
            = new MediaController.MediaPlayerControl() {

        @Override
        public void start() {
            mTransportController.startPlaying();
            mContent.start();
        }

        @Override
        public void pause() {
            mTransportController.pausePlaying();
            mContent.pause();
        }

        @Override
        public int getDuration() {
            return mContent.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            return mContent.getCurrentPosition();
        }

        @Override
        public void seekTo(int pos) {
            mContent.seekTo(pos);
        }

        @Override
        public boolean isPlaying() {
            return mContent.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
            return mContent.getBufferPercentage();
        }

        @Override
        public boolean canPause() {
            return mContent.canPause();
        }

        @Override
        public boolean canSeekBackward() {
            return mContent.canSeekBackward();
        }

        @Override
        public boolean canSeekForward() {
            return mContent.canSeekForward();
        }
    };

    /**
     * This is the actual video player.  It is the top-level content of
     * the activity's view hierarchy, going under the status bar and nav
     * bar areas.
     */
    public static class Content extends VideoView implements
            View.OnSystemUiVisibilityChangeListener, View.OnClickListener,
            ActionBar.OnMenuVisibilityListener, MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        Activity mActivity;
        TransportController mTransportController;
        MediaController mMediaController;
        boolean mAddedMenuListener;
        boolean mMenusOpen;
        boolean mPaused;
        boolean mNavVisible;
        int mLastSystemUiVis;

        Runnable mNavHider = new Runnable() {
            @Override public void run() {
                setNavVisibility(false);
            }
        };

        Runnable mProgressUpdater = new Runnable() {
            @Override public void run() {
                mMediaController.updateProgress();
                getHandler().postDelayed(this, 1000);
            }
        };

        public Content(Context context, AttributeSet attrs) {
            super(context, attrs);
            setOnSystemUiVisibilityChangeListener(this);
            setOnClickListener(this);
            setOnPreparedListener(this);
            setOnCompletionListener(this);
            setOnErrorListener(this);
        }

        public void init(Activity activity, TransportController transportController,
                MediaController mediaController) {
            // This called by the containing activity to supply the surrounding
            // state of the video player that it will interact with.
            mActivity = activity;
            mTransportController = transportController;
            mMediaController = mediaController;
            pause();
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (mActivity != null) {
                mAddedMenuListener = true;
                mActivity.getActionBar().addOnMenuVisibilityListener(this);
            }
        }

        @Override protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (mAddedMenuListener) {
                mActivity.getActionBar().removeOnMenuVisibilityListener(this);
            }
            mNavVisible = false;
        }

        @Override public void onSystemUiVisibilityChange(int visibility) {
            // Detect when we go out of nav-hidden mode, to clear our state
            // back to having the full UI chrome up.  Only do this when
            // the state is changing and nav is no longer hidden.
            int diff = mLastSystemUiVis ^ visibility;
            mLastSystemUiVis = visibility;
            if ((diff&SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility&SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                setNavVisibility(true);
            }
        }

        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);

            // When we become visible or invisible, play is paused.
            pause();
        }

        @Override public void onClick(View v) {
            // Clicking anywhere makes the navigation visible.
            setNavVisibility(true);
        }

        @Override public void onMenuVisibilityChanged(boolean isVisible) {
            mMenusOpen = isVisible;
            setNavVisibility(true);
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mMediaController.setEnabled(true);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            mTransportController.pausePlaying();
            pause();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mTransportController.pausePlaying();
            pause();
            return false;
        }

        @Override public void start() {
            super.start();
            mPaused = false;
            setKeepScreenOn(true);
            setNavVisibility(true);
            mMediaController.refresh();
            scheduleProgressUpdater();
        }

        @Override public void pause() {
            super.pause();
            mPaused = true;
            setKeepScreenOn(false);
            setNavVisibility(true);
            mMediaController.refresh();
            scheduleProgressUpdater();
        }

        void scheduleProgressUpdater() {
            Handler h = getHandler();
            if (h != null) {
                if (mNavVisible && !mPaused) {
                    h.removeCallbacks(mProgressUpdater);
                    h.post(mProgressUpdater);
                } else {
                    h.removeCallbacks(mProgressUpdater);
                }
            }
        }

        void setNavVisibility(boolean visible) {
            int newVis = SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (!visible) {
                newVis |= SYSTEM_UI_FLAG_LOW_PROFILE | SYSTEM_UI_FLAG_FULLSCREEN
                        | SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // If we are now visible, schedule a timer for us to go invisible.
            if (visible) {
                Handler h = getHandler();
                if (h != null) {
                    h.removeCallbacks(mNavHider);
                    if (!mMenusOpen && !mPaused) {
                        // If the menus are open or play is paused, we will not auto-hide.
                        h.postDelayed(mNavHider, 3000);
                    }
                }
            }

            // Set the new desired visibility.
            setSystemUiVisibility(newVis);
            mNavVisible = visible;
            mMediaController.setVisibility(visible ? VISIBLE : INVISIBLE);
            scheduleProgressUpdater();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.videoview);

        // Find the video player in our UI.
        mContent = (Content) findViewById(R.id.content);

        // Create and initialize the media control UI.
        mMediaController = (MediaController) findViewById(R.id.media_controller);
        mMediaController.setMediaPlayer(mMediaPlayerControl);

        // Create transport controller to control video, giving the callback
        // interface to receive actions from.
        mTransportController = new TransportController(this, mTransportCallbacks);

        // We're just playing a built-in demo video.
        mContent.init(this, mTransportController, mMediaController);
        mContent.setVideoURI(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.videoviewdemo));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // We first dispatch keys to the transport controller -- we want it
        // to get to consume any media keys rather than letting whoever has focus
        // in the view hierarchy to potentially eat it.
        if (mTransportController.dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
