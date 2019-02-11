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

package androidx.camera.app.cameraview;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.view.CameraView;
import androidx.camera.view.CameraView.CaptureMode;
import androidx.camera.view.CameraView.ScaleType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/** The main camera fragment. */
public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";

    // Possible values for this intent key are the name values of CameraX.LensFacing encoded as
    // strings (case-insensitive): "back", "front".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";

    // Possible values for this intent key are the name values of CameraView.CaptureMode encoded as
    // strings (case-insensitive): "image", "video", "mixed"
    private static final String INTENT_EXTRA_CAPTURE_MODE = "captureMode";

    private View cameraHolder;
    private CameraView cameraView;
    private View captureView;
    private CompoundButton modeButton;
    @Nullable
    private CompoundButton toggleCameraButton;
    private CompoundButton toggleCropButton;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        cameraHolder = view.findViewById(R.id.layout_camera);
        cameraView = view.findViewById(R.id.camera);
        toggleCameraButton = view.findViewById(R.id.toggle);
        toggleCropButton = view.findViewById(R.id.toggle_crop);
        captureView = cameraHolder.findViewById(R.id.capture);
        if (cameraHolder == null) {
            throw new IllegalStateException("No View found with id R.id.layout_camera");
        }
        if (cameraView == null) {
            throw new IllegalStateException("No CameraView found with id R.id.camera");
        }
        if (captureView == null) {
            throw new IllegalStateException("No CameraView found with id R.id.capture");
        }

        modeButton = cameraHolder.findViewById(R.id.mode);

        if (modeButton == null) {
            throw new IllegalStateException("No View found with id R.id.mode");
        }

        // Log the location of some views, so their locations can be used to perform some automated
        // clicks in tests.
        logCenterCoordinates(cameraView, "camera_view");
        logCenterCoordinates(captureView, "capture");
        logCenterCoordinates(toggleCameraButton, "toggle_camera");
        logCenterCoordinates(toggleCropButton, "toggle_crop");
        logCenterCoordinates(modeButton, "mode");

        // Get extra option for setting initial camera direction
        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            String cameraDirectionString = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            if (cameraDirectionString != null) {
                LensFacing lensFacing = LensFacing.valueOf(cameraDirectionString.toUpperCase());
                cameraView.setCameraByLensFacing(lensFacing);
            }

            String captureModeString = bundle.getString(INTENT_EXTRA_CAPTURE_MODE);
            if (captureModeString != null) {
                CaptureMode captureMode = CaptureMode.valueOf(captureModeString.toUpperCase());
                cameraView.setCaptureMode(captureMode);
            }
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        // Set the lifecycle that will be used to control the camera
        cameraView.bindToLifecycle(getActivity());

        cameraView.setPinchToZoomEnabled(true);
        captureView.setOnTouchListener(new CaptureViewOnTouchListener(cameraView));

        // Set clickable, Let the cameraView can be interacted by Voice Access
        cameraView.setClickable(true);

        if (toggleCameraButton != null) {
            toggleCameraButton.setVisibility(
                    (cameraView.hasCameraWithLensFacing(LensFacing.BACK)
                            && cameraView.hasCameraWithLensFacing(LensFacing.FRONT))
                            ? View.VISIBLE
                            : View.INVISIBLE);
            toggleCameraButton.setChecked(cameraView.getCameraLensFacing() == LensFacing.FRONT);
        }

        // Set listeners here, or else restoring state will trigger them.
        if (toggleCameraButton != null) {
            toggleCameraButton.setOnCheckedChangeListener(
                    (b, checked) ->
                            cameraView.setCameraByLensFacing(
                                    checked ? LensFacing.FRONT : LensFacing.BACK));
        }

        toggleCropButton.setChecked(cameraView.getScaleType() == ScaleType.CENTER_CROP);
        toggleCropButton.setOnCheckedChangeListener(
                (b, checked) -> {
                    if (checked) {
                        cameraView.setScaleType(ScaleType.CENTER_CROP);
                    } else {
                        cameraView.setScaleType(ScaleType.CENTER_INSIDE);
                    }
                });

        if (modeButton != null) {
            updateModeButtonIcon();

            modeButton.setOnClickListener(
                    view -> {
                        if (cameraView.isRecording()) {
                            Toast.makeText(
                                    getContext(),
                                    "Can not switch mode during video recording.",
                                    Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        if (cameraView.getCaptureMode() == CaptureMode.MIXED) {
                            cameraView.setCaptureMode(CaptureMode.IMAGE);
                        } else if (cameraView.getCaptureMode() == CaptureMode.IMAGE) {
                            cameraView.setCaptureMode(CaptureMode.VIDEO);
                        } else {
                            cameraView.setCaptureMode(CaptureMode.MIXED);
                        }

                        updateModeButtonIcon();
                    });
        }
    }

    private void updateModeButtonIcon() {
        if (cameraView.getCaptureMode() == CaptureMode.MIXED) {
            modeButton.setButtonDrawable(R.drawable.ic_photo_camera);
        } else if (cameraView.getCaptureMode() == CaptureMode.IMAGE) {
            modeButton.setButtonDrawable(R.drawable.ic_camera);
        } else {
            modeButton.setButtonDrawable(R.drawable.ic_videocam);
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
                                File externalDir = getActivity().getExternalFilesDir(null);
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
