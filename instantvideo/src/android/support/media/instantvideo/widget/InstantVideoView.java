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
package android.support.media.instantvideo.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;

/**
 * Displays a video or an image. This class provides a high level interface for applications to play
 * a video. When the video isn't playing, it displays an image instead if set.
 *
 * <p>A developer can preload a video by calling
 * {@link android.support.media.instantvideo.preload.InstantVideoPreloadManager#preload}, so that
 * the video can play right after {@link #start} is called.
 *
 * @see android.support.media.instantvideo.preload.InstantVideoPreloadManager
 */
public class InstantVideoView extends FrameLayout {
    private static final String TAG = "InstantVideoView";

    private final VideoView mVideoView;
    private final ImageView mImageView;

    public InstantVideoView(Context context) {
        this(context, null, 0, 0);
    }

    public InstantVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public InstantVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public InstantVideoView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mVideoView = new VideoView(context, attrs, defStyleAttr, defStyleRes);
        mImageView = new ImageView(context, attrs, defStyleAttr, defStyleRes);
        addView(mVideoView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(mImageView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mVideoView.setVisibility(GONE);
    }

    /**
     * Sets a video URI.
     *
     * <p>This method is used to set the video URI to play in this view.
     *
     * @param uri the URI of the video.
     */
    public void setVideoUri(Uri uri) {
        mVideoView.setVideoURI(uri);
    }

    /**
     * Sets a drawable as the default image of this view.
     *
     * @param drawable the Drawable to set, or {@code null} to clear the content.
     *
     * @see ImageView#setImageDrawable
     */
    public void setImageDrawable(Drawable drawable) {
        mImageView.setImageDrawable(drawable);
    }

    private void reset() {
        mVideoView.setOnPreparedListener(null);
        mVideoView.stopPlayback();
        mVideoView.setVisibility(GONE);
        mImageView.setVisibility(VISIBLE);
    }

    /**
     * Starts the video playback.
     */
    public void start() {
        reset();
        mVideoView.setVisibility(VISIBLE);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mVideoView.start();
                mImageView.setVisibility(GONE);
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mVideoView.setVisibility(GONE);
                return false;
            }
        });
    }

    /**
     * Stops the video playback.
     */
    public void stop() {
        mVideoView.stopPlayback();
        mVideoView.setVisibility(GONE);
        mImageView.setVisibility(VISIBLE);
    }

    /**
     * Seeks to the given position.
     *
     * @param position The position of the video to seek to. It's the offset from the start of the
     * video.
     */
    public void seekTo(int position) {
        mVideoView.seekTo(position);
    }

    /**
     * Returns the current playback position which is the offset from the start of the video.
     */
    public int getCurrentPosition() {
        return mVideoView.getCurrentPosition();
    }
}
