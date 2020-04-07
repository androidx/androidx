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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.CameraView;
import androidx.camera.view.CameraView.CaptureMode;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/** The main camera fragment. */
public class CameraViewFragment extends Fragment {
    private static final String TAG = "CameraViewFragment";

    // Possible values for this intent key are the name values of LensFacing encoded as
    // strings (case-insensitive): "back", "front".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";

    // Possible values for this intent key are the name values of CameraView.CaptureMode encoded as
    // strings (case-insensitive): "image", "video", "mixed"
    private static final String INTENT_EXTRA_CAPTURE_MODE = "captureMode";

    private View mCameraHolder;
    private CameraView mCameraView;
    private View mCaptureView;
    private CompoundButton mModeButton;
    @Nullable
    private CompoundButton mToggleCameraButton;
    private CompoundButton mToggleCropButton;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mCameraHolder = view.findViewById(R.id.layout_camera);
        mCameraView = view.findViewById(R.id.camera);
        mToggleCameraButton = view.findViewById(R.id.toggle);
        mToggleCropButton = view.findViewById(R.id.toggle_crop);
        mCaptureView = mCameraHolder.findViewById(R.id.capture);
        if (mCameraHolder == null) {
            throw new IllegalStateException("No View found with id R.id.layout_camera");
        }
        if (mCameraView == null) {
            throw new IllegalStateException("No CameraView found with id R.id.camera");
        }
        if (mCaptureView == null) {
            throw new IllegalStateException("No CameraView found with id R.id.capture");
        }

        mModeButton = mCameraHolder.findViewById(R.id.mode);

        if (mModeButton == null) {
            throw new IllegalStateException("No View found with id R.id.mode");
        }

        // Log the location of some views, so their locations can be used to perform some automated
        // clicks in tests.
        logCenterCoordinates(mCameraView, "camera_view");
        logCenterCoordinates(mCaptureView, "capture");
        logCenterCoordinates(mToggleCameraButton, "toggle_camera");
        logCenterCoordinates(mToggleCropButton, "toggle_crop");
        logCenterCoordinates(mModeButton, "mode");

        // Get extra option for setting initial camera direction
        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            final String cameraDirectionString = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            final boolean isCameraDirectionValid =
                    cameraDirectionString != null && (cameraDirectionString.equalsIgnoreCase("BACK")
                            || cameraDirectionString.equalsIgnoreCase("FRONT"));
            if (isCameraDirectionValid) {
                @CameraSelector.LensFacing int lensFacing = cameraDirectionString.equalsIgnoreCase(
                        "BACK")
                        ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
                mCameraView.setCameraLensFacing(lensFacing);
            }

            String captureModeString = bundle.getString(INTENT_EXTRA_CAPTURE_MODE);
            if (captureModeString != null) {
                CaptureMode captureMode = CaptureMode.valueOf(captureModeString.toUpperCase());
                mCameraView.setCaptureMode(captureMode);
            }
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("App has not been granted CAMERA permission");
        }

        // Set the lifecycle that will be used to control the camera
        mCameraView.bindToLifecycle(CameraViewFragment.this);

        mCameraView.setPinchToZoomEnabled(true);
        mCaptureView.setOnTouchListener(new CaptureViewOnTouchListener(mCameraView));

        // Set clickable, Let the cameraView can be interacted by Voice Access
        mCameraView.setClickable(true);

        if (mToggleCameraButton != null) {
            mToggleCameraButton.setVisibility(
                    (mCameraView.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)
                            && mCameraView.hasCameraWithLensFacing(
                            CameraSelector.LENS_FACING_FRONT))
                            ? View.VISIBLE
                            : View.INVISIBLE);
            mToggleCameraButton.setChecked(
                    mCameraView.getCameraLensFacing() == CameraSelector.LENS_FACING_FRONT);
        }

        // Set listeners here, or else restoring state will trigger them.
        if (mToggleCameraButton != null) {
            mToggleCameraButton.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton b, boolean checked) {
                            mCameraView.setCameraLensFacing(
                                    checked ? CameraSelector.LENS_FACING_FRONT
                                            : CameraSelector.LENS_FACING_BACK);
                        }
                    });
        }

        mToggleCropButton.setChecked(
                mCameraView.getScaleType() == PreviewView.ScaleType.FILL_CENTER);
        mToggleCropButton.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                mCameraView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
            } else {
                mCameraView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
            }
        });

        if (mModeButton != null) {
            updateModeButtonIcon();

            mModeButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mCameraView.isRecording()) {
                                Toast.makeText(
                                        CameraViewFragment.this.getContext(),
                                        "Can not switch mode during video recording.",
                                        Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            if (mCameraView.getCaptureMode() == CaptureMode.MIXED) {
                                mCameraView.setCaptureMode(CaptureMode.IMAGE);
                            } else if (mCameraView.getCaptureMode() == CaptureMode.IMAGE) {
                                mCameraView.setCaptureMode(CaptureMode.VIDEO);
                            } else {
                                mCameraView.setCaptureMode(CaptureMode.MIXED);
                            }

                            CameraViewFragment.this.updateModeButtonIcon();
                        }
                    });
        }
    }

    private void updateModeButtonIcon() {
        if (mCameraView.getCaptureMode() == CaptureMode.MIXED) {
            mModeButton.setButtonDrawable(R.drawable.ic_photo_camera);
        } else if (mCameraView.getCaptureMode() == CaptureMode.IMAGE) {
            mModeButton.setButtonDrawable(R.drawable.ic_camera);
        } else {
            mModeButton.setButtonDrawable(R.drawable.ic_videocam);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    private void logCenterCoordinates(View view, String name) {
        view.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Activity activity = getActivity();
                                if (activity == null) {
                                    // The fragment has been detached from the parent Activity.
                                    return;
                                }
                                Rect rect = new Rect();
                                view.getGlobalVisibleRect(rect);
                                Log.d(
                                        TAG,
                                        "View "
                                                + name
                                                + " Center "
                                                + rect.centerX()
                                                + " "
                                                + rect.centerY());
                                File externalDir = activity.getExternalFilesDir(null);
                                File logFile =
                                        new File(externalDir, name + "_button_coordinates.txt");
                                try (PrintStream stream = new PrintStream(logFile)) {
                                    stream.print(rect.centerX() + " " + rect.centerY());
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not save to " + logFile, e);
                                }
                            }
                        });
    }
}
