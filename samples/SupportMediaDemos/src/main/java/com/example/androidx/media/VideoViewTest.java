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

package com.example.androidx.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.media.widget.MediaControlView2;
import androidx.media.widget.VideoView2;

/**
 * Test application for VideoView2/MediaControlView2
 */
@SuppressLint("NewApi")
public class VideoViewTest extends FragmentActivity {
    public static final String LOOPING_EXTRA_NAME =
            "com.example.androidx.media.VideoViewTest.IsLooping";
    public static final String USE_TEXTURE_VIEW_EXTRA_NAME =
            "com.example.androidx.media.VideoViewTest.UseTextureView";
    public static final String MEDIA_TYPE_ADVERTISEMENT =
            "com.example.androidx.media.VideoViewTest.MediaTypeAdvertisement";
    private static final String TAG = "VideoViewTest";

    private MyVideoView mVideoView = null;
    private float mSpeed = 1.0f;

    private MediaControlView2 mMediaControlView = null;

    private boolean mUseTextureView = false;
    private int mPrevWidth;
    private int mPrevHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.video_activity);

        mVideoView = findViewById(R.id.video_view);
        mVideoView.setActivity(this);

        String errorString = null;
        Intent intent = getIntent();
        Uri contentUri;
        if (intent == null || (contentUri = intent.getData()) == null || !contentUri.isAbsolute()) {
            errorString = "Invalid intent";
        } else {
            mUseTextureView = intent.getBooleanExtra(USE_TEXTURE_VIEW_EXTRA_NAME, false);
            if (mUseTextureView) {
                mVideoView.setViewType(VideoView2.VIEW_TYPE_TEXTUREVIEW);
            }
            mVideoView.setVideoUri(contentUri);

            mMediaControlView = new MediaControlView2(this);
            mVideoView.setMediaControlView2(mMediaControlView, 2000);
            mMediaControlView.setOnFullScreenListener(new FullScreenListener());
        }
        if (errorString != null) {
            showErrorDialog(errorString);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mVideoView.isAttachedToWindow()) {
            mVideoView.getMediaController().getTransportControls().play();
            mVideoView.getMediaController().registerCallback(mMediaControllerCallback);
        } else {
            mVideoView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    mVideoView.getMediaController().getTransportControls().play();
                    mVideoView.getMediaController().registerCallback(mMediaControllerCallback);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    // No need to remove callback here since MediaSession has already been
                    // destroyed.
                }
            });
        }
        setTitle(getViewTypeString(mVideoView));
    }

    @Override
    protected void onPause() {
        mVideoView.getMediaController().getTransportControls().pause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mVideoView.getMediaController().unregisterCallback(mMediaControllerCallback);
        mVideoView.getMediaController().getTransportControls().stop();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            if (ev.getRawX() < (screenWidth / 2.0f)) {
                // TODO: getSpeed() not needed?
                mSpeed -= 0.1f;
            } else {
                mSpeed += 0.1f;
            }
            mVideoView.setSpeed(mSpeed);
            Toast.makeText(this, "speed rate: " + String.format("%.2f", mSpeed), Toast.LENGTH_SHORT)
                    .show();
        }
        return super.onTouchEvent(ev);
    }

    private void showErrorDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playback error")
                .setMessage(errorMessage)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).show();
    }

    MediaControllerCompat.Callback mMediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_STOPPED:
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                case PlaybackStateCompat.STATE_ERROR:
                    showErrorDialog("Error: (" + state.getErrorMessage() + ")");
                    break;
            }
        }
    };

    private class FullScreenListener
            implements MediaControlView2.OnFullScreenListener {
        @Override
        public void onFullScreen(View view, boolean fullScreen) {
            // TODO: Remove bottom controls after adding back button functionality.
            if (mPrevHeight == 0 && mPrevWidth == 0) {
                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                mPrevWidth = params.width;
                mPrevHeight = params.height;
            }

            if (fullScreen) {
                // Remove notification bar
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mVideoView.setLayoutParams(params);
            } else {
                // Restore notification bar
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                params.width = mPrevWidth;
                params.height = mPrevHeight;
                mVideoView.setLayoutParams(params);
            }
            mVideoView.requestLayout();
            mVideoView.invalidate();
        }
    }

    /**
     * Extension of the stock android video view used to hook and override
     * keypress behavior.  Mainly used to make sure that certain keystrokes
     * don't automatically bring up the andriod MediaController widget (which
     * then steals focus)
     *
     * @author johngro@google.com (John Grossman)
     */
    public static class MyVideoView extends VideoView2 {
        private float mDX;
        private float mDY;
        private Activity mActivity;

        public MyVideoView(Context context) {
            super(context);
        }

        public MyVideoView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDX = ev.getRawX() - getX();
                    mDY = ev.getRawY() - getY();
                    super.onTouchEvent(ev);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    animate()
                            .x(ev.getRawX() - mDX)
                            .y(ev.getRawY() - mDY)
                            .setDuration(0)
                            .start();
                    super.onTouchEvent(ev);
                    return true;
            }
            return super.onTouchEvent(ev);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event)  {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                mActivity.finish();
            }
            return true;
        }

        public void setActivity(Activity activity) {
            mActivity = activity;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mVideoView.getViewType() == VideoView2.VIEW_TYPE_SURFACEVIEW) {
            mVideoView.setViewType(VideoView2.VIEW_TYPE_TEXTUREVIEW);
            Toast.makeText(this, "switch to TextureView", Toast.LENGTH_SHORT).show();
            setTitle(getViewTypeString(mVideoView));
        } else if (mVideoView.getViewType() == VideoView2.VIEW_TYPE_TEXTUREVIEW) {
            mVideoView.setViewType(VideoView2.VIEW_TYPE_SURFACEVIEW);
            Toast.makeText(this, "switch to SurfaceView", Toast.LENGTH_SHORT).show();
            setTitle(getViewTypeString(mVideoView));
        }
    }

    private String getViewTypeString(VideoView2 videoView) {
        if (videoView == null) {
            return "Unknown";
        }
        int type = videoView.getViewType();
        if (type == VideoView2.VIEW_TYPE_SURFACEVIEW) {
            return "SurfaceView";
        } else if (type == VideoView2.VIEW_TYPE_TEXTUREVIEW) {
            return "TextureView";
        }
        return "Unknown";
    }
}
