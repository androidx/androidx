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

package androidx.media2.widget;

import static androidx.media2.widget.VideoView.VIEW_TYPE_TEXTUREVIEW;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import androidx.core.content.ContextCompat;

class VideoTextureView extends TextureView
        implements VideoViewInterface, TextureView.SurfaceTextureListener {
    private static final String TAG = "VideoTextureView";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private Surface mSurface;
    SurfaceListener mSurfaceListener;
    private PlayerWrapper mPlayer;
    // A flag to indicate taking over other view should be proceed.
    private boolean mIsTakingOverOldView;

    VideoTextureView(Context context) {
        super(context, null);
        setSurfaceTextureListener(this);
    }

    ////////////////////////////////////////////////////
    // implements VideoViewInterface
    ////////////////////////////////////////////////////

    @Override
    public boolean assignSurfaceToPlayerWrapper(PlayerWrapper player) {
        if (player == null || !hasAvailableSurface()) {
            // Surface is not ready.
            return false;
        }
        player.setSurface(mSurface).addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mSurfaceListener != null) {
                            mSurfaceListener.onSurfaceTakeOverDone(VideoTextureView.this);
                        }
                    }
                }, ContextCompat.getMainExecutor(getContext())
        );
        return true;
    }

    @Override
    public void setSurfaceListener(SurfaceListener l) {
        mSurfaceListener = l;
    }

    @Override
    public int getViewType() {
        return VIEW_TYPE_TEXTUREVIEW;
    }

    @Override
    public void setPlayerWrapper(PlayerWrapper player) {
        mPlayer = player;
        if (mIsTakingOverOldView) {
            mIsTakingOverOldView = !assignSurfaceToPlayerWrapper(mPlayer);
        }
    }

    @Override
    public void takeOver() {
        mIsTakingOverOldView = !assignSurfaceToPlayerWrapper(mPlayer);
    }

    @Override
    public boolean hasAvailableSurface() {
        return mSurface != null && mSurface.isValid();
    }

    ////////////////////////////////////////////////////
    // implements TextureView.SurfaceTextureListener
    ////////////////////////////////////////////////////

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mSurface = new Surface(surfaceTexture);
        if (mSurfaceListener != null) {
            mSurfaceListener.onSurfaceCreated(this, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceListener != null) {
            mSurfaceListener.onSurfaceChanged(this, width, height);
        }
        // requestLayout();  // TODO: figure out if it should be called here?
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // no-op
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mSurfaceListener != null) {
            mSurfaceListener.onSurfaceDestroyed(this);
        }
        mSurface = null;
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int videoWidth = mPlayer != null ? mPlayer.getVideoSize().getWidth() : 0;
        final int videoHeight = mPlayer != null ? mPlayer.getVideoSize().getHeight() : 0;

        int width;
        int height;

        if (videoWidth == 0 || videoHeight == 0) {
            width = getDefaultSize(videoWidth, widthMeasureSpec);
            height = getDefaultSize(videoHeight, heightMeasureSpec);
            setMeasuredDimension(width, height);
            return;
        }

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
            // the size is fixed
            width = widthSpecSize;
            height = heightSpecSize;

            // for compatibility, we adjust size based on aspect ratio
            if (videoWidth * height  < width * videoHeight) {
                width = height * videoWidth / videoHeight;
            } else if (videoWidth * height  > width * videoHeight) {
                height = width * videoHeight / videoWidth;
            }
        } else if (widthSpecMode == MeasureSpec.EXACTLY) {
            // only the width is fixed, adjust the height to match aspect ratio if possible
            width = widthSpecSize;
            height = width * videoHeight / videoWidth;
            if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                // couldn't match aspect ratio within the constraints
                height = heightSpecSize | MEASURED_STATE_TOO_SMALL;
            }
        } else if (heightSpecMode == MeasureSpec.EXACTLY) {
            // only the height is fixed, adjust the width to match aspect ratio if possible
            height = heightSpecSize;
            width = height * videoWidth / videoHeight;
            if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                // couldn't match aspect ratio within the constraints
                width = widthSpecSize | MEASURED_STATE_TOO_SMALL;
            }
        } else {
            // neither the width nor the height are fixed, try to use actual video size
            width = videoWidth;
            height = videoHeight;
            if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                // too tall, decrease both width and height
                height = heightSpecSize;
                width = height * videoWidth / videoHeight;
            }
            if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                // too wide, decrease both width and height
                width = widthSpecSize;
                height = width * videoHeight / videoWidth;
            }
        }

        setMeasuredDimension(width, height);
    }
}
