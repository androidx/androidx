/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.integration.view;

import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture.OnImageSavedListener;
import androidx.camera.core.ImageCapture.UseCaseError;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCapture.OnVideoSavedListener;
import androidx.camera.view.CameraView;
import androidx.camera.view.CameraView.CaptureMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * A {@link View.OnTouchListener} which converts a view's touches into camera actions.
 *
 * <p>The listener converts touches on a {@link View}, such as a button, into appropriate photo
 * taking or video recording actions through a {@link CameraView}. A click is interpreted as a
 * take-photo signal, while a long-press is interpreted as a record-video signal.
 */
class CaptureViewOnTouchListener
        implements View.OnTouchListener, OnImageSavedListener, OnVideoSavedListener {
    private static final String TAG = "ViewOnTouchListener";

    private static final String FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final String PHOTO_EXTENSION = ".jpg";
    private static final String VIDEO_EXTENSION = ".mp4";

    private static final int TAP = 1;
    private static final int HOLD = 2;
    private static final int RELEASE = 3;

    private final long mLongPress = ViewConfiguration.getLongPressTimeout();
    private final CameraView mCameraView;

    // TODO: Use a Handler for a background thread, rather than running on the current (main)
    // thread.
    private final Handler mHandler =
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case TAP:
                            onTap();
                            break;
                        case HOLD:
                            onHold();
                            if (mCameraView.getMaxVideoDuration() > 0) {
                                sendEmptyMessageDelayed(RELEASE, mCameraView.getMaxVideoDuration());
                            }
                            break;
                        case RELEASE:
                            onRelease();
                            break;
                        default:
                            // No op
                    }
                }
            };

    private long mDownEventTimestamp;
    private Rect mViewBoundsRect;

    /** Creates a new listener which links to the given {@link CameraView}. */
    CaptureViewOnTouchListener(CameraView cameraView) {
        mCameraView = cameraView;
    }

    /** Called when the user taps. */
    void onTap() {
        if (mCameraView.getCaptureMode() == CaptureMode.IMAGE
                || mCameraView.getCaptureMode() == CaptureMode.MIXED) {
            mCameraView.takePicture(createNewFile(PHOTO_EXTENSION), this);
        }
    }

    /** Called when the user holds (long presses). */
    void onHold() {
        if (mCameraView.getCaptureMode() == CaptureMode.VIDEO
                || mCameraView.getCaptureMode() == CaptureMode.MIXED) {
            mCameraView.startRecording(createNewFile(VIDEO_EXTENSION), this);
        }
    }

    /** Called when the user releases. */
    void onRelease() {
        if (mCameraView.getCaptureMode() == CaptureMode.VIDEO
                || mCameraView.getCaptureMode() == CaptureMode.MIXED) {
            mCameraView.stopRecording();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownEventTimestamp = System.currentTimeMillis();
                mViewBoundsRect =
                        new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                mHandler.sendEmptyMessageDelayed(HOLD, mLongPress);
                view.setPressed(true);
                break;
            case MotionEvent.ACTION_MOVE:
                // If the user moves their finger off the button, trigger RELEASE
                if (mViewBoundsRect.contains(
                        view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())) {
                    break;
                }
                // Fall-through
            case MotionEvent.ACTION_CANCEL:
                clearHandler();
                if (deltaSinceDownEvent() > mLongPress
                        && (mCameraView.getMaxVideoDuration() <= 0
                        || deltaSinceDownEvent() < mCameraView.getMaxVideoDuration())) {
                    mHandler.sendEmptyMessage(RELEASE);
                }
                view.setPressed(false);
                break;
            case MotionEvent.ACTION_UP:
                clearHandler();
                if (deltaSinceDownEvent() < mLongPress) {
                    mHandler.sendEmptyMessage(TAP);
                } else if ((mCameraView.getMaxVideoDuration() <= 0
                        || deltaSinceDownEvent() < mCameraView.getMaxVideoDuration())) {
                    mHandler.sendEmptyMessage(RELEASE);
                }
                view.setPressed(false);
                break;
            default:
                // No op
        }
        return true;
    }

    private long deltaSinceDownEvent() {
        return System.currentTimeMillis() - mDownEventTimestamp;
    }

    private void clearHandler() {
        mHandler.removeMessages(TAP);
        mHandler.removeMessages(HOLD);
        mHandler.removeMessages(RELEASE);
    }

    private File createNewFile(String extension) {
        // Use Locale.US to ensure we get ASCII digits
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                new SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis())
                        + extension);
    }

    @Override
    public void onImageSaved(File file) {
        report("Picture saved to " + file.getAbsolutePath());

        // Print out metadata about the picture
        // TODO: Print out metadata to log once metadata is implemented

        broadcastPicture(file);
    }

    @Override
    public void onVideoSaved(File file) {
        report("Video saved to " + file.getAbsolutePath());
        broadcastVideo(file);
    }

    @Override
    public void onError(UseCaseError useCaseError, String message, @Nullable Throwable cause) {
        report("Failure");
    }

    @Override
    public void onError(
            VideoCapture.UseCaseError useCaseError,
            String message,
            @Nullable Throwable cause) {
        report("Failure");
    }

    private void report(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(mCameraView.getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void broadcastPicture(File file) {
        if (Build.VERSION.SDK_INT < 24) {
            Intent intent = new Intent(Camera.ACTION_NEW_PICTURE);
            intent.setData(Uri.fromFile(file));
            mCameraView.getContext().sendBroadcast(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            mCameraView.getContext().sendBroadcast(intent);
        }
    }

    private void broadcastVideo(File file) {
        if (Build.VERSION.SDK_INT < 24) {
            Intent intent = new Intent(Camera.ACTION_NEW_VIDEO);
            intent.setData(Uri.fromFile(file));
            mCameraView.getContext().sendBroadcast(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(file));
            mCameraView.getContext().sendBroadcast(intent);
        }
    }
}
