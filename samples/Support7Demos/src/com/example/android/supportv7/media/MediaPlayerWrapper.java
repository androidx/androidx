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

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.io.IOException;

/**
 * MediaPlayerWrapper handles playback of a single media item, and is used for
 * both local and remote playback.
 */
public class MediaPlayerWrapper implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaSessionManager.Callback {
    private static final String TAG = "MediaPlayerWrapper";
    private static final boolean DEBUG = false;

    private static final int STATE_IDLE = 0;
    private static final int STATE_PLAY_PENDING = 1;
    private static final int STATE_READY = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;

    private final Context mContext;
    private final Handler mHandler = new Handler();
    private MediaPlayer mMediaPlayer;
    private int mState = STATE_IDLE;
    private Callback mCallback;
    private Surface mSurface;
    private SurfaceHolder mSurfaceHolder;
    private int mSeekToPos;

    public MediaPlayerWrapper(Context context) {
        mContext = context;
        reset();
    }

    public void release() {
        onStop();
        mMediaPlayer.release();
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    // MediaSessionManager.Callback
    @Override
    public void onNewItem(Uri uri) {
        reset();
        try {
            mMediaPlayer.setDataSource(mContext, uri);
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MediaPlayer throws IllegalStateException, uri=" + uri);
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer throws IOException, uri=" + uri);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "MediaPlayer throws IllegalArgumentException, uri=" + uri);
        } catch (SecurityException e) {
            Log.e(TAG, "MediaPlayer throws SecurityException, uri=" + uri);
        }
    }

    @Override
    public void onStart() {
        if (mState == STATE_READY || mState == STATE_PAUSED) {
            mMediaPlayer.start();
            mState = STATE_PLAYING;
        } else if (mState == STATE_IDLE){
            mState = STATE_PLAY_PENDING;
        }
    }

    @Override
    public void onStop() {
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            mMediaPlayer.stop();
            mState = STATE_IDLE;
        }
    }

    @Override
    public void onPause() {
        if (mState == STATE_PLAYING) {
            mMediaPlayer.pause();
            mState = STATE_PAUSED;
        }
    }

    @Override
    public void onSeek(long pos) {
        if (DEBUG) {
            Log.d(TAG, "onSeek: pos=" + pos);
        }
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            mMediaPlayer.seekTo((int)pos);
            mSeekToPos = (int)pos;
        } else if (mState == STATE_IDLE || mState == STATE_PLAY_PENDING) {
            // Seek before onPrepared() arrives,
            // need to performed delayed seek in onPrepared()
            mSeekToPos = (int)pos;
        }
    }

    @Override
    public void onGetStatus(MediaQueueItem item) {
        if (mState == STATE_PLAYING || mState == STATE_PAUSED) {
            // use mSeekToPos if we're currently seeking (mSeekToPos is reset
            // when seeking is completed)
            item.setContentDuration(mMediaPlayer.getDuration());
            item.setContentPosition(mSeekToPos > 0 ?
                    mSeekToPos : mMediaPlayer.getCurrentPosition());
        }
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
        mSurfaceHolder = null;
        updateSurface();
    }

    public void setSurface(SurfaceHolder surfaceHolder) {
        mSurface = null;
        mSurfaceHolder = surfaceHolder;
        updateSurface();
    }

    //MediaPlayer Listeners
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG,"onPrepared");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mState == STATE_IDLE) {
                    mState = STATE_READY;
                    updateVideoRect();
                } else if (mState == STATE_PLAY_PENDING) {
                    mState = STATE_PLAYING;
                    updateVideoRect();
                    if (mSeekToPos > 0) {
                        Log.d(TAG, "Seeking to initial pos " + mSeekToPos);
                        mMediaPlayer.seekTo((int)mSeekToPos);
                    }
                    mMediaPlayer.start();
                }
                if (mCallback != null) {
                    mCallback.onStatusChanged();
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (DEBUG) {
            Log.d(TAG,"onCompletion");
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
            Log.d(TAG,"onError");
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
                    mCallback.onStatusChanged();
                }
            }
        });
    }

    public void reset() {
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
        if (mState != STATE_IDLE && mState != STATE_PLAY_PENDING) {
            int videoWidth = mMediaPlayer.getVideoWidth();
            int videoHeight = mMediaPlayer.getVideoHeight();
            if (videoWidth > 0 && videoHeight > 0) {
                if (mCallback != null) {
                    mCallback.onSizeChanged(videoWidth, videoHeight);
                }
            } else {
                Log.e(TAG, "video rect is 0x0!");
            }
        }
    }

    private void updateSurface() {
        if (mSurface != null) {
            // The setSurface API does not exist until V14+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                ICSMediaPlayer.setSurface(mMediaPlayer, mSurface);
            } else {
                throw new UnsupportedOperationException("MediaPlayer does not support "
                        + "setSurface() on this version of the platform.");
            }
        } else if (mSurfaceHolder != null) {
            mMediaPlayer.setDisplay(mSurfaceHolder);
        } else {
            mMediaPlayer.setDisplay(null);
        }
    }

    public static abstract class Callback {
        public void onError() {}
        public void onCompletion() {}
        public void onStatusChanged() {}
        public void onSizeChanged(int width, int height) {}
    }

    private static final class ICSMediaPlayer {
        public static final void setSurface(MediaPlayer player, Surface surface) {
            player.setSurface(surface);
        }
    }
}
