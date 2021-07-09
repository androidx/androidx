/*
 * Copyright 2019 The Android Open Source Project
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

import static androidx.camera.view.PreviewView.StreamState.IDLE;
import static androidx.camera.view.PreviewView.StreamState.STREAMING;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalUseCaseGroup;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/** A Fragment that displays a {@link PreviewView}. */
public class PreviewViewFragment extends Fragment {

    /** Scale types of ImageView that map to the PreviewView scale types. */
    // Synthetic access
    static final ImageView.ScaleType[] IMAGE_VIEW_SCALE_TYPES =
            {ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_CENTER,
                    ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_START,
                    ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_END};

    private static final String TAG = "PreviewViewFragment";

    // Possible values for this intent key are the name values of LensFacing encoded as
    // strings (case-insensitive): "back", "front".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";
    private static final String CAMERA_DIRECTION_BACK = "back";
    private static final String CAMERA_DIRECTION_FRONT = "front";

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    @SuppressWarnings("WeakerAccess")
    PreviewView mPreviewView;
    @SuppressWarnings("WeakerAccess")
    int mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
    private BlurBitmap mBlurBitmap;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    Preview mPreview;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    ProcessCameraProvider mCameraProvider;

    public PreviewViewFragment() {
        super(R.layout.fragment_preview_view);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewView = view.findViewById(R.id.preview_view);
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mPreviewView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> {
                    if (mCameraProvider != null) {
                        bindPreview(mCameraProvider);
                    }
                });
        mBlurBitmap = new BlurBitmap(requireContext());
        Futures.addCallback(mCameraProviderFuture, new FutureCallback<ProcessCameraProvider>() {
            @Override
            public void onSuccess(@Nullable ProcessCameraProvider cameraProvider) {
                Preconditions.checkNotNull(cameraProvider);
                mCameraProvider = cameraProvider;
                mPreview = new Preview.Builder()
                        .setTargetRotation(view.getDisplay().getRotation())
                        .setTargetName("Preview")
                        .build();
                mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
                if (!areFrontOrBackCameraAvailable(cameraProvider)) {
                    return;
                }

                setUpToggleVisibility(cameraProvider, view);
                setUpCameraLensFacing(cameraProvider);
                setUpToggleCamera(cameraProvider, view);
                setUpScaleTypeSelect(cameraProvider, view);
                setUpTargetRotationButton(cameraProvider, view);
                bindPreview(cameraProvider);
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to retrieve camera provider from CameraX. Is CameraX "
                        + "initialized?", throwable);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBlurBitmap.clear();
    }

    @SuppressWarnings("WeakerAccess")
    boolean areFrontOrBackCameraAvailable(@NonNull final ProcessCameraProvider cameraProvider) {
        try {
            return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    || cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
        } catch (CameraInfoUnavailableException exception) {
            return false;
        }
    }

    @OptIn(markerClass = ExperimentalUseCaseGroup.class)
    void setUpTargetRotationButton(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        Button button = rootView.findViewById(R.id.target_rotation);
        updateTargetRotationButtonText(button);
        button.setOnClickListener(view -> {
            switch (mPreview.getTargetRotation()) {
                case Surface.ROTATION_0:
                    mPreview.setTargetRotation(Surface.ROTATION_90);
                    break;
                case Surface.ROTATION_90:
                    mPreview.setTargetRotation(Surface.ROTATION_180);
                    break;
                case Surface.ROTATION_180:
                    mPreview.setTargetRotation(Surface.ROTATION_270);
                    break;
                case Surface.ROTATION_270:
                    mPreview.setTargetRotation(Surface.ROTATION_0);
                    break;
                default:
                    throw new RuntimeException(
                            "Unexpected rotation value: " + mPreview.getTargetRotation());
            }
            updateTargetRotationButtonText(button);
            bindPreview(cameraProvider);
        });
    }

    @SuppressLint("SetTextI18n")
    void updateTargetRotationButtonText(final @NonNull Button rotationButton) {
        switch (mPreview.getTargetRotation()) {
            case Surface.ROTATION_0:
                rotationButton.setText("ROTATION_0");
                break;
            case Surface.ROTATION_90:
                rotationButton.setText("ROTATION_90");
                break;
            case Surface.ROTATION_180:
                rotationButton.setText("ROTATION_180");
                break;
            case Surface.ROTATION_270:
                rotationButton.setText("ROTATION_270");
                break;
            default:
                throw new RuntimeException(
                        "Unexpected rotation value: " + mPreview.getTargetRotation());
        }
    }

    @SuppressWarnings("WeakerAccess")
    void setUpToggleVisibility(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        final ViewGroup previewViewContainer = rootView.findViewById(R.id.container);
        final Button toggleVisibilityButton = rootView.findViewById(R.id.toggle_visibility);
        toggleVisibilityButton.setEnabled(true);
        toggleVisibilityButton.setOnClickListener(view -> {
            if (previewViewContainer.indexOfChild(mPreviewView) == -1) {
                previewViewContainer.addView(mPreviewView, 0);
                bindPreview(cameraProvider);
            } else {
                cameraProvider.unbindAll();
                previewViewContainer.removeView(mPreviewView);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void setUpCameraLensFacing(@NonNull final ProcessCameraProvider cameraProvider) {
        try {
            // Get extra option for setting initial camera direction
            boolean isCameraDirectionValid = false;
            String cameraDirectionString = null;
            Bundle bundle = requireActivity().getIntent().getExtras();
            if (bundle != null) {
                cameraDirectionString = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
                isCameraDirectionValid =
                        cameraDirectionString != null && (cameraDirectionString.equalsIgnoreCase(
                                CAMERA_DIRECTION_BACK) || cameraDirectionString.equalsIgnoreCase(
                                CAMERA_DIRECTION_FRONT));
            }
            if (isCameraDirectionValid) {
                if (cameraDirectionString.equalsIgnoreCase(CAMERA_DIRECTION_BACK)
                        && cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
                } else if (cameraDirectionString.equalsIgnoreCase(CAMERA_DIRECTION_FRONT)
                        && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    throw new IllegalStateException(
                            "The camera " + cameraDirectionString + " is unavailable.");
                }
            } else {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    throw new IllegalStateException("Front and back cameras are unavailable.");
                }
            }
        } catch (CameraInfoUnavailableException e) {
            Log.e(TAG, "Unable to access camera: " + e.getMessage());
        }
    }

    @SuppressWarnings("WeakerAccess")
    void setUpToggleCamera(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        final Button toggleCameraButton = rootView.findViewById(R.id.toggle_camera);
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                toggleCameraButton.setEnabled(true);
            }
            toggleCameraButton.setOnClickListener(view -> {
                animateToggleCamera(rootView);
                toggleCamera(cameraProvider);
            });
        } catch (CameraInfoUnavailableException exception) {
            toggleCameraButton.setEnabled(false);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void setUpScaleTypeSelect(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                PreviewViewScaleTypePresenter.getScaleTypesLiterals());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner scaleTypeSpinner = rootView.findViewById(R.id.scale_type);
        scaleTypeSpinner.setAdapter(adapter);

        // Default value
        final PreviewView.ScaleType currentScaleType = mPreviewView.getScaleType();
        final String currentScaleTypeLiteral =
                PreviewViewScaleTypePresenter.getLiteralForScaleType(currentScaleType);
        final int defaultSelection = adapter.getPosition(currentScaleTypeLiteral);
        scaleTypeSpinner.setSelection(defaultSelection, false);

        scaleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final PreviewView.ScaleType scaleType = PreviewView.ScaleType.values()[position];
                mPreviewView.setScaleType(scaleType);

                // Update the preview snapshot ImageView to have a scaleType matching that of the
                // PreviewView.
                final ImageView previewSnapshot = rootView.findViewById(R.id.preview_snapshot);
                previewSnapshot.setScaleType(IMAGE_VIEW_SCALE_TYPES[position]);

                bindPreview(cameraProvider);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    // TODO(b/185869869) Remove the UnsafeOptInUsageError once view's version matches core's.
    @SuppressLint("UnsafeOptInUsageError")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        if (mPreview == null) {
            return;
        }
        ViewPort viewPort = mPreviewView.getViewPort(mPreview.getTargetRotation());
        if (viewPort == null) {
            return;
        }
        final Camera camera = cameraProvider.bindToLifecycle(this, getCurrentCameraSelector(),
                new UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(mPreview).build());
        setUpFocusAndMetering(camera);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpFocusAndMetering(@NonNull final Camera camera) {
        mPreviewView.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    final MeteringPointFactory factory = mPreviewView.getMeteringPointFactory();
                    final MeteringPoint point = factory.createPoint(motionEvent.getX(),
                            motionEvent.getY());
                    final FocusMeteringAction action = new FocusMeteringAction.Builder(
                            point).build();
                    Futures.addCallback(
                            camera.getCameraControl().startFocusAndMetering(action),
                            new FutureCallback<FocusMeteringResult>() {
                                @Override
                                public void onSuccess(FocusMeteringResult result) {
                                    Log.d(TAG, "Focus and metering succeeded");
                                }

                                @Override
                                public void onFailure(@NonNull Throwable throwable) {
                                    Log.e(TAG, "Focus and metering failed", throwable);
                                }
                            }, ContextCompat.getMainExecutor(requireContext()));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void animateToggleCamera(@NonNull final View rootView) {
        final Bitmap snapshot = mPreviewView.getBitmap();
        if (snapshot == null) {
            return;
        }

        final ImageView previewSnapshot = rootView.findViewById(R.id.preview_snapshot);
        mBlurBitmap.blur(snapshot);
        previewSnapshot.setImageBitmap(snapshot);

        final AtomicBoolean isPreviewIdle = new AtomicBoolean(false);
        mPreviewView.getPreviewStreamState().observe(getViewLifecycleOwner(),
                new Observer<PreviewView.StreamState>() {
                    @Override
                    public void onChanged(PreviewView.StreamState streamState) {
                        if (streamState == IDLE) {
                            // The current preview stream is idle
                            isPreviewIdle.set(true);
                        } else if (isPreviewIdle.get() && streamState == STREAMING) {
                            // A new preview stream is starting
                            previewSnapshot.setImageBitmap(null);
                            mPreviewView.getPreviewStreamState().removeObserver(this);
                        }
                    }
                });
    }

    private void toggleCamera(@NonNull final ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        if (mCurrentLensFacing == CameraSelector.LENS_FACING_BACK) {
            mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
        }
        bindPreview(cameraProvider);
    }

    private CameraSelector getCurrentCameraSelector() {
        return new CameraSelector.Builder()
                .requireLensFacing(mCurrentLensFacing)
                .build();
    }

    // region For preview updates testing
    @Nullable
    private CountDownLatch mPreviewUpdatingLatch;

    // Here we use OnPreDrawListener in ViewTreeObserver to detect if view is being updating.
    // If yes, preview should be working to update the view. We could use more precise approach
    // like TextureView.SurfaceTextureListener#onSurfaceTextureUpdated but it will require to add
    // API in PreviewView which is not a good idea. And we use OnPreDrawListener instead of
    // OnDrawListener because OnDrawListener is not invoked on some low API level devices.
    private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = () -> {
        if (mPreviewUpdatingLatch != null) {
            mPreviewUpdatingLatch.countDown();
        }
        return true;
    };

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(
                mOnPreDrawListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(
                mOnPreDrawListener);
    }

    @VisibleForTesting
    void setPreviewUpdatingLatch(@NonNull CountDownLatch previewUpdatingLatch) {
        mPreviewUpdatingLatch = previewUpdatingLatch;
    }
    // endregion
}
