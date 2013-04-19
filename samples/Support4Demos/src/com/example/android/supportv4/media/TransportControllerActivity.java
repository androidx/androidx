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

//BEGIN_INCLUDE(complete)
import android.support.v4.media.TransportMediator;
import android.support.v4.media.TransportPerformer;
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

public class TransportControllerActivity extends Activity {

    /**
     * TODO: Set the path variable to a streaming video URL or a local media
     * file path.
     */
    private Content mContent;
    private TransportMediator mTransportMediator;
    private MediaController mMediaController;

    /**
     * Handle actions from on-screen media controls.  Most of these are simple re-directs
     * to the VideoView; some we need to capture to update our state.
     */
    TransportPerformer mTransportPerformer = new TransportPerformer() {
        @Override public void onStart() {
            mContent.start();
        }

        @Override public void onStop() {
            mContent.pause();
        }

        @Override public void onPause() {
            mContent.pause();
        }

        @Override public long onGetDuration() {
            return mContent.getDuration();
        }

        @Override public long onGetCurrentPosition() {
            return mContent.getCurrentPosition();
        }

        @Override public void onSeekTo(long pos) {
            mContent.seekTo((int)pos);
        }

        @Override public boolean onIsPlaying() {
            return mContent.isPlaying();
        }

        @Override public int onGetBufferPercentage() {
            return mContent.getBufferPercentage();
        }

        @Override public int onGetTransportControlFlags() {
            int flags = TransportMediator.FLAG_KEY_MEDIA_PLAY
                    | TransportMediator.FLAG_KEY_MEDIA_PLAY_PAUSE
                    | TransportMediator.FLAG_KEY_MEDIA_STOP;
            if (mContent.canPause()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_PAUSE;
            }
            if (mContent.canSeekBackward()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_REWIND;
            }
            if (mContent.canSeekForward()) {
                flags |= TransportMediator.FLAG_KEY_MEDIA_FAST_FORWARD;
            }
            return flags;
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
        TransportMediator mTransportMediator;
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

        public void init(Activity activity, TransportMediator transportMediator,
                MediaController mediaController) {
            // This called by the containing activity to supply the surrounding
            // state of the video player that it will interact with.
            mActivity = activity;
            mTransportMediator = transportMediator;
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
            mTransportMediator.pausePlaying();
            pause();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mTransportMediator.pausePlaying();
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

        // Create transport controller to control video, giving the callback
        // interface to receive actions from.
        mTransportMediator = new TransportMediator(this, mTransportPerformer);

        // Create and initialize the media control UI.
        mMediaController = (MediaController) findViewById(R.id.media_controller);
        mMediaController.setMediaPlayer(mTransportMediator);

        // We're just playing a built-in demo video.
        mContent.init(this, mTransportMediator, mMediaController);
        mContent.setVideoURI(Uri.parse("android.resource://" + getPackageName() +
                "/" + R.raw.videoviewdemo));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // We first dispatch keys to the transport controller -- we want it
        // to get to consume any media keys rather than letting whoever has focus
        // in the view hierarchy to potentially eat it.
        if (mTransportMediator.dispatchKeyEvent(event)) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
//END_INCLUDE(complete)
