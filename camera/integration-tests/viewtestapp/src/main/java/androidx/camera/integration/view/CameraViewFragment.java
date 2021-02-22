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
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.CameraSelector;
import androidx.camera.view.CameraView;
import androidx.camera.view.CameraView.CaptureMode;
import androidx.camera.view.PreviewView;
import androidx.camera.view.video.ExperimentalVideo;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

/** The main camera fragment. */
public class CameraViewFragment extends Fragment {
    private static final String TAG = "CameraViewFragment";

    // Possible values for this intent key are the name values of LensFacing encoded as
    // strings (case-insensitive): "back", "front".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";

    // Possible values for this intent key are the name values of CameraView.CaptureMode encoded as
    // strings (case-insensitive): "image", "video", "mixed"
    private static final String INTENT_EXTRA_CAPTURE_MODE = "captureMode";

    // Argument key which determines the lifecycle used to control the camera of CameraView.
    // Possible values for this argument key are LIFECYCLE_TYPE_ACTIVITY, LIFECYCLE_TYPE_FRAGMENT,
    // LIFECYCLE_TYPE_FRAGMENT_VIEW, LIFECYCLE_TYPE_CUSTOM. If using LIFECYCLE_TYPE_DEBUG, then
    // a lifecycle must be provided via setDebugLifecycleOwner().
    static final String ARG_LIFECYCLE_TYPE = "lifecycle_type";

    // Fragment's Activity lifecycle
    static final String LIFECYCLE_TYPE_ACTIVITY = "activity";
    // Fragment lifecycle (this). This is the default lifecycle used by this fragment
    static final String LIFECYCLE_TYPE_FRAGMENT = "fragment";
    // Fragment's View lifecycle (getViewLifecycleOwner())
    static final String LIFECYCLE_TYPE_FRAGMENT_VIEW = "fragment_view";
    // Lifecycle provided by setDebugLifecycleOwner
    static final String LIFECYCLE_TYPE_DEBUG = "debug";


    private View mCameraHolder;
    CameraView mCameraView;
    private View mCaptureView;
    private CompoundButton mModeButton;
    @Nullable
    private CompoundButton mToggleCameraButton;
    private CompoundButton mToggleCropButton;
    @Nullable
    private LifecycleOwner mDebugLifecycleOwner;

    /**
     * Sets the debug lifecycle used by this fragment IF the fragment has argument
     * {@link #ARG_LIFECYCLE_TYPE} set to {@link #LIFECYCLE_TYPE_DEBUG}.
     *
     * <p>This lifecycle must be set before the fragment lifecycle reaches
     * {@link #onViewStateRestored(Bundle)}, or it will be ignored.
     *
     * <p>This value set here is not retained across fragment recreation, so it is only safe to
     * use for debugging/testing purposes.
     */
    public void setDebugLifecycleOwner(@NonNull LifecycleOwner owner) {
        mDebugLifecycleOwner = owner;
    }

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
    @UseExperimental(markerClass = ExperimentalVideo.class)
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("App has not been granted CAMERA permission");
        }

        // Set the lifecycle that will be used to control the camera
        Bundle args = getArguments();
        String lifecycleType = args == null ? LIFECYCLE_TYPE_FRAGMENT :
                args.getString(ARG_LIFECYCLE_TYPE, LIFECYCLE_TYPE_FRAGMENT);
        Log.d(TAG, "Attempting bindToLifecycle with lifecycle type: " + lifecycleType);
        switch (lifecycleType) {
            case LIFECYCLE_TYPE_ACTIVITY:
                mCameraView.bindToLifecycle(requireActivity());
                break;
            case LIFECYCLE_TYPE_FRAGMENT:
                mCameraView.bindToLifecycle(CameraViewFragment.this);
                break;
            case LIFECYCLE_TYPE_FRAGMENT_VIEW:
                mCameraView.bindToLifecycle(Objects.requireNonNull(getViewLifecycleOwner()));
                break;
            case LIFECYCLE_TYPE_DEBUG:
                if (mDebugLifecycleOwner == null) {
                    throw new IllegalStateException("Lifecycle type set to DEBUG, but no debug "
                            + "lifecycle exists. setDebugLifecycleOwner() must be called before "
                            + "onViewStateRestored()");
                }
                mCameraView.bindToLifecycle(mDebugLifecycleOwner);
                break;
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Invalid lifecycle "
                                + "type: %s. Valid options are %s, %s, %s, and %s.", lifecycleType,
                        LIFECYCLE_TYPE_ACTIVITY, LIFECYCLE_TYPE_FRAGMENT,
                        LIFECYCLE_TYPE_FRAGMENT_VIEW, LIFECYCLE_TYPE_DEBUG));
        }

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

    @UseExperimental(markerClass = ExperimentalVideo.class)
    void updateModeButtonIcon() {
        if (mCameraView.getCaptureMode() == CaptureMode.MIXED) {
            mModeButton.setButtonDrawable(R.drawable.ic_photo_camera);
        } else if (mCameraView.getCaptureMode() == CaptureMode.IMAGE) {
            mModeButton.setButtonDrawable(R.drawable.ic_camera);
        } else {
            mModeButton.setButtonDrawable(R.drawable.ic_videocam);
        }
    }

    @Override
    @NonNull
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
