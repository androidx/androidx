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

import android.os.Handler;

class PlayerAdapter extends
        androidx.leanback.media.PlayerAdapter {

    static final int FAUX_DURATION = 33 * 1000;

    private boolean mIsPlaying;
    private long mStartTime;
    private long mStartPosition = 0;
    private Handler mHandler = new Handler();
    private final Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            getCallback().onCurrentPositionChanged(com.example.android.leanback.PlayerAdapter.this);
            mHandler.postDelayed(this, 16);
        }
    };

    @Override
    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public long getDuration() {
        return FAUX_DURATION;
    }

    @Override
    public void seekTo(long position) {
        mStartPosition = position;
        mStartTime = System.currentTimeMillis();
        getCallback().onCurrentPositionChanged(com.example.android.leanback.PlayerAdapter.this);
    }

    @Override
    public long getCurrentPosition() {
        int speed;
        if (!mIsPlaying) {
            speed = 0;
        } else {
            speed = 1;
        }
        long position = mStartPosition + (System.currentTimeMillis() - mStartTime) * speed;
        if (position > getDuration()) {
            position = getDuration();
            onPlaybackComplete(true);
        } else if (position < 0) {
            position = 0;
            onPlaybackComplete(false);
        }
        return (int) position;
    }

    void onPlaybackComplete(final boolean ended) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mStartPosition = 0;
                mIsPlaying = false;
                getCallback().onPlayStateChanged(com.example.android.leanback.PlayerAdapter.this);
                if (ended) {
                    getCallback().onPlayCompleted(com.example.android.leanback.PlayerAdapter.this);
                }
            }
        });
    }

    @Override
    public void play() {
        if (mIsPlaying) {
            return;
        }
        mStartPosition = getCurrentPosition();
        mIsPlaying = true;
        mStartTime = System.currentTimeMillis();
        getCallback().onPlayStateChanged(com.example.android.leanback.PlayerAdapter.this);
    }

    @Override
    public void pause() {
        if (!mIsPlaying) {
            return;
        }
        mStartPosition = getCurrentPosition();
        mIsPlaying = false;
        getCallback().onPlayStateChanged(com.example.android.leanback.PlayerAdapter.this);
    }

    @Override
    public void setProgressUpdatingEnabled(boolean enable) {
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) {
            mUpdateProgressRunnable.run();
        }
    }
}
