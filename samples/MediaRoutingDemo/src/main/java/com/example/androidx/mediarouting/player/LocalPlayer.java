/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.player;

import android.app.Activity;
import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import com.example.androidx.mediarouting.OverlayDisplayWindow;
import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.data.PlaylistItem;

import java.io.IOException;

/**
 * Handles playback of a single media item using MediaPlayer.
 */
public abstract class LocalPlayer extends Player implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener {
    private static final String TAG = "LocalPlayer";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final Handler mUpdateSurfaceHandler = new Handler(mHandler.getLooper());
    private MediaPlayer mMediaPlayer;
    private int mState = STATE_IDLE;
    private int mSeekToPos;
    private int mVideoWidth;
    private int mVideoHeight;
    private Surface mSurface;
    private SurfaceHolder mSurfaceHolder;

    public LocalPlayer(@NonNull Context context) {
        mContext = context;

        // reset media player
        reset();
    }

    @Override
    public boolean isRemotePlayback() {
        return false;
    }

    @Override
    public boolean isQueuingSupported() {
        return false;
    }

    @Override
    public void connect(@NonNull RouteInfo route) {
        if (DEBUG) {
            Log.d(TAG, "connecting to: " + route);
        }
    }

    @Override
    public void release() {
        if (DEBUG) {
            Log.d(TAG, "releasing");
        }
        // release media player
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        super.release();
    }

    // Player
    @Override
    public void play(@NonNull final PlaylistItem item) {
        if (DEBUG) {
            Log.d(TAG, "play: item=" + item);
        }
        reset();
        mSeekToPos = (int) item.getPosition();
        try {
            mMediaPlayer.setDataSource(mContext, item.getUri());
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer throws IllegalStateException, uri=" + item.getUri());
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer throws IOException, uri=" + item.getUri());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "MediaPlayer throws IllegalArgumentException, uri=" + item.getUri());
        } catch (SecurityException e) {
            Log.e(TAG, "MediaPlayer throws SecurityException, uri=" + item.getUri());
        }
        if (item.getState() == MediaItemStatus.PLAYBACK_STATE_PLAYING) {
            resume();
        } else {
            pause();
        }
    }

    @Override
    public void seek(@NonNull final PlaylistItem item) {
        if (DEBUG) {
            Log.d(TAG, "seek: item=" + item);
        }
        int pos = (int) item.getPosition();
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            mMediaPlayer.seekTo(pos);
            mSeekToPos = pos;
        } else if (mState == STATE_IDLE || mState == STATE_PREPARING_FOR_PLAY
                || mState == STATE_PREPARING_FOR_PAUSE) {
            // Seek before onPrepared() arrives,
            // need to performed delayed seek in onPrepared()
            mSeekToPos = pos;
        }
    }

    @Override
    public void getPlaylistItemStatus(
            @NonNull final PlaylistItem item, final boolean shouldUpdate) {
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            item.setDuration(mMediaPlayer.getDuration());
            item.setPosition(getCurrentPosition());
            item.setTimestamp(SystemClock.elapsedRealtime());
        }
        if (shouldUpdate && mCallback != null) {
            mCallback.onPlaylistReady();
        }
    }

    private int getCurrentPosition() {
        // Use mSeekToPos if we're currently seeking (mSeekToPos is reset when seeking is completed)
        return mSeekToPos > 0 ? mSeekToPos : mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void pause() {
        if (DEBUG) {
            Log.d(TAG, "pause");
        }
        if (mState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mState = STATE_PAUSED;
        } else if (mState == STATE_PREPARING_FOR_PLAY) {
            mState = STATE_PREPARING_FOR_PAUSE;
        }
    }

    @Override
    public void resume() {
        if (DEBUG) {
            Log.d(TAG, "resume");
        }
        if (mState == STATE_READY || mState == STATE_PAUSED) {
            mMediaPlayer.start();
            mState = STATE_PLAYING;
        } else if (mState == STATE_IDLE || mState == STATE_PREPARING_FOR_PAUSE) {
            mState = STATE_PREPARING_FOR_PLAY;
        }
    }

    @Override
    public void stop() {
        if (DEBUG) {
            Log.d(TAG, "stop");
        }
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            mMediaPlayer.stop();
            mState = STATE_IDLE;
        }
    }

    @Override
    public void enqueue(@NonNull final PlaylistItem item) {
        throw new UnsupportedOperationException("LocalPlayer doesn't support enqueue!");
    }

    @NonNull
    @Override
    public PlaylistItem remove(@NonNull String iid) {
        throw new UnsupportedOperationException("LocalPlayer doesn't support remove!");
    }

    //MediaPlayer Listeners
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG, "onPrepared");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mState == STATE_IDLE) {
                    mState = STATE_READY;
                    updateVideoRect();
                } else if (mState == STATE_PREPARING_FOR_PLAY
                        || mState == STATE_PREPARING_FOR_PAUSE) {
                    int prevState = mState;
                    mState = mState == STATE_PREPARING_FOR_PLAY ? STATE_PLAYING : STATE_PAUSED;
                    updateVideoRect();
                    if (mSeekToPos > 0) {
                        if (DEBUG) {
                            Log.d(TAG, "seek to initial pos: " + mSeekToPos);
                        }
                        mMediaPlayer.seekTo(mSeekToPos);
                    }
                    if (prevState == STATE_PREPARING_FOR_PLAY) {
                        mMediaPlayer.start();
                    }
                }
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG, "onCompletion");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onCompletion();
                }
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (DEBUG) {
            Log.d(TAG, "onError");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onError();
                }
            }
        });
        // return true so that onCompletion is not called
        return true;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG, "onSeekComplete");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSeekToPos = 0;
                if (mCallback != null) {
                    mCallback.onPlaylistChanged();
                }
            }
        });
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    @NonNull
    protected MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    protected int getVideoWidth() {
        return mVideoWidth;
    }

    protected int getVideoHeight() {
        return mVideoHeight;
    }

    protected int getState() {
        return mState;
    }

    protected void setSurface(@NonNull Surface surface) {
        mSurface = surface;
        mSurfaceHolder = null;
        updateSurface();
    }

    protected void setSurface(@Nullable SurfaceHolder surfaceHolder) {
        mSurface = null;
        mSurfaceHolder = surfaceHolder;
        updateSurface();
    }

    protected void removeSurface(@NonNull SurfaceHolder surfaceHolder) {
        if (surfaceHolder == mSurfaceHolder) {
            setSurface((SurfaceHolder) null);
        }
    }

    protected void updateSurface() {
        mUpdateSurfaceHandler.removeCallbacksAndMessages(null);
        mUpdateSurfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer == null) {
                    // just return if media player is already gone
                    return;
                }
                if (mSurface != null) {
                    ICSMediaPlayer.setSurface(mMediaPlayer, mSurface);
                } else if (mSurfaceHolder != null) {
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                } else {
                    mMediaPlayer.setDisplay(null);
                }
            }
        });
    }

    protected abstract void updateSize();

    private void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        updateSurface();
        mState = STATE_IDLE;
        mSeekToPos = 0;
    }

    private void updateVideoRect() {
        if (mState != STATE_IDLE && mState != STATE_PREPARING_FOR_PLAY
                && mState != STATE_PREPARING_FOR_PAUSE) {
            int width = mMediaPlayer.getVideoWidth();
            int height = mMediaPlayer.getVideoHeight();
            if (width > 0 && height > 0) {
                mVideoWidth = width;
                mVideoHeight = height;
                updateSize();
            } else {
                Log.e(TAG, "video rect is 0x0!");
                mVideoWidth = mVideoHeight = 0;
            }
        }
    }

    private static final class ICSMediaPlayer {
        public static void setSurface(MediaPlayer player, Surface surface) {
            player.setSurface(surface);
        }
    }

    /**
     * Handles playback of a single media item using MediaPlayer in SurfaceView
     */
    public static class SurfaceViewPlayer extends LocalPlayer implements SurfaceHolder.Callback {
        private static final String TAG = "SurfaceViewPlayer";
        private RouteInfo mRoute;
        private final SurfaceView mSurfaceView;
        private final FrameLayout mLayout;
        private DemoPresentation mPresentation;

        public SurfaceViewPlayer(@NonNull Context context) {
            super(context);

            mLayout = (FrameLayout) ((Activity) context).findViewById(R.id.player);
            mSurfaceView = (SurfaceView) ((Activity) context).findViewById(R.id.surface_view);

            // add surface holder callback
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.addCallback(this);
        }

        @Override
        public void connect(@NonNull RouteInfo route) {
            super.connect(route);
            mRoute = route;
        }

        @Override
        public void release() {
            releasePresentation();

            // remove surface holder callback
            SurfaceHolder holder = mSurfaceView.getHolder();
            holder.removeCallback(this);

            // hide the surface view when SurfaceViewPlayer is destroyed
            mSurfaceView.setVisibility(View.GONE);
            mLayout.setVisibility(View.GONE);

            super.release();
        }

        @Override
        public void updatePresentation() {
            // Get the current route and its presentation display.
            Display presentationDisplay = mRoute != null ? mRoute.getPresentationDisplay() : null;

            // Dismiss the current presentation if the display has changed.
            if (mPresentation != null && mPresentation.getDisplay() != presentationDisplay) {
                Log.i(TAG, "Dismissing presentation because the current route no longer "
                        + "has a presentation display.");
                mPresentation.dismiss();
                mPresentation = null;
            }

            // Show a new presentation if needed.
            if (mPresentation == null && presentationDisplay != null) {
                Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
                mPresentation = new DemoPresentation(getContext(), presentationDisplay);
                mPresentation.setOnDismissListener(mOnDismissListener);
                try {
                    mPresentation.show();
                } catch (WindowManager.InvalidDisplayException ex) {
                    Log.w(TAG, "Couldn't show presentation!  Display was removed in "
                            + "the meantime.", ex);
                    mPresentation = null;
                }
            }

            updateContents();
        }

        // SurfaceHolder.Callback
        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                int height) {
            if (DEBUG) {
                Log.d(TAG, "surfaceChanged: " + width + "x" + height);
            }
            setSurface(holder);
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            if (DEBUG) {
                Log.d(TAG, "surfaceCreated");
            }
            setSurface(holder);
            updateSize();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            if (DEBUG) {
                Log.d(TAG, "surfaceDestroyed");
            }
            removeSurface(holder);
        }

        @Override
        protected void updateSize() {
            int width = getVideoWidth();
            int height = getVideoHeight();
            if (width > 0 && height > 0) {
                if (mPresentation != null) {
                    mPresentation.updateSize(width, height);
                } else {
                    int surfaceWidth = mLayout.getWidth();
                    int surfaceHeight = mLayout.getHeight();

                    // Calculate the new size of mSurfaceView, so that video is centered
                    // inside the framelayout with proper letterboxing/pillarboxing
                    ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                    if (surfaceWidth * height < surfaceHeight * width) {
                        // Black bars on top&bottom, mSurfaceView has full layout width,
                        // while height is derived from video's aspect ratio
                        lp.width = surfaceWidth;
                        lp.height = surfaceWidth * height / width;
                    } else {
                        // Black bars on left&right, mSurfaceView has full layout height,
                        // while width is derived from video's aspect ratio
                        lp.width = surfaceHeight * width / height;
                        lp.height = surfaceHeight;
                    }
                    Log.i(TAG, "video rect is " + lp.width + "x" + lp.height);
                    mSurfaceView.setLayoutParams(lp);
                }
            }
        }

        private void updateContents() {
            // Show either the content in the main activity or the content in the presentation
            if (mPresentation != null) {
                mLayout.setVisibility(View.GONE);
                mSurfaceView.setVisibility(View.GONE);
            } else {
                mLayout.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        }

        // Listens for when presentations are dismissed.
        private final DialogInterface.OnDismissListener mOnDismissListener =
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (dialog == mPresentation) {
                            Log.i(TAG, "Presentation dismissed.");
                            mPresentation = null;
                            updateContents();
                        }
                    }
                };

        private void releasePresentation() {
            // dismiss presentation display
            if (mPresentation != null) {
                Log.i(TAG, "Dismissing presentation because the activity is no longer visible.");
                mPresentation.dismiss();
                mPresentation = null;
            }
        }

        // Presentation
        private final class DemoPresentation extends Presentation {
            private SurfaceView mPresentationSurfaceView;

            DemoPresentation(Context context, Display display) {
                super(context, display);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                // Be sure to call the super class.
                super.onCreate(savedInstanceState);

                // Inflate the layout.
                setContentView(R.layout.sample_media_router_presentation);

                // Set up the surface view.
                mPresentationSurfaceView = findViewById(R.id.surface_view);
                SurfaceHolder holder = mPresentationSurfaceView.getHolder();
                holder.addCallback(SurfaceViewPlayer.this);
                Log.i(TAG, "Presentation created");
            }

            public void updateSize(int width, int height) {
                int surfaceHeight = getWindow().getDecorView().getHeight();
                int surfaceWidth = getWindow().getDecorView().getWidth();
                ViewGroup.LayoutParams lp = mPresentationSurfaceView.getLayoutParams();
                if (surfaceWidth * height < surfaceHeight * width) {
                    lp.width = surfaceWidth;
                    lp.height = surfaceWidth * height / width;
                } else {
                    lp.width = surfaceHeight * width / height;
                    lp.height = surfaceHeight;
                }
                Log.i(TAG, "Presentation video rect is " + lp.width + "x" + lp.height);
                mPresentationSurfaceView.setLayoutParams(lp);
            }
        }
    }

    /**
     * Handles playback of a single media item using MediaPlayer in
     * OverlayDisplayWindow.
     */
    public static class OverlayPlayer extends LocalPlayer implements
            OverlayDisplayWindow.OverlayWindowListener {
        private static final String TAG = "OverlayPlayer";
        private final OverlayDisplayWindow mOverlay;

        public OverlayPlayer(@NonNull Context context) {
            super(context);

            mOverlay = OverlayDisplayWindow.create(getContext(),
                    getContext().getResources().getString(
                            R.string.sample_media_route_provider_remote), 1024, 768,
                    Gravity.CENTER);

            mOverlay.setOverlayWindowListener(this);
        }

        @Override
        public void connect(@NonNull RouteInfo route) {
            super.connect(route);
            mOverlay.show();
        }

        @Override
        public void release() {
            mOverlay.dismiss();
            super.release();
        }

        @Override
        protected void updateSize() {
            int width = getVideoWidth();
            int height = getVideoHeight();
            if (width > 0 && height > 0) {
                mOverlay.updateAspectRatio(width, height);
            }
        }

        // OverlayDisplayWindow.OverlayWindowListener
        @Override
        public void onWindowCreated(@NonNull Surface surface) {
            setSurface(surface);
        }

        @Override
        public void onWindowCreated(@NonNull SurfaceHolder surfaceHolder) {
            setSurface(surfaceHolder);
        }

        @Override
        public void onWindowDestroyed() {
            setSurface((SurfaceHolder) null);
        }

        @Nullable
        @Override
        public Bitmap getSnapshot() {
            if (getState() == STATE_PLAYING || getState() == STATE_PAUSED) {
                return mOverlay.getSnapshot();
            }
            return null;
        }
    }
}
