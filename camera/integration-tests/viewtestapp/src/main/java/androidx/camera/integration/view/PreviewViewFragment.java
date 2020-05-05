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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CountDownLatch;

/** A Fragment that displays a {@link PreviewView}. */
public class PreviewViewFragment extends Fragment {

    private static final String TAG = "PreviewViewFragment";

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    @SuppressWarnings("WeakerAccess")
    PreviewView mPreviewView;
    @SuppressWarnings("WeakerAccess")
    int mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;

    public PreviewViewFragment() {
        super(R.layout.fragment_preview_view);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreviewView = view.findViewById(R.id.preview_view);
        Futures.addCallback(mCameraProviderFuture, new FutureCallback<ProcessCameraProvider>() {
            @Override
            public void onSuccess(@Nullable ProcessCameraProvider cameraProvider) {
                Preconditions.checkNotNull(cameraProvider);

                if (!areFrontOrBackCameraAvailable(cameraProvider)) {
                    return;
                }

                setUpToggleVisibility(cameraProvider, view);
                setUpToggleCamera(cameraProvider, view);
                setUpScaleTypeSelect(view);
                bindPreview(cameraProvider);
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to retrieve camera provider from CameraX. Is CameraX "
                        + "initialized?", throwable);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
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

    @SuppressWarnings("WeakerAccess")
    void setUpToggleVisibility(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        final ViewGroup previewViewContainer = rootView.findViewById(R.id.container);
        final Button toggleVisibilityButton = rootView.findViewById(R.id.toggle_visibility);
        toggleVisibilityButton.setEnabled(true);
        toggleVisibilityButton.setOnClickListener(view -> {
            if (previewViewContainer.getChildCount() == 0) {
                previewViewContainer.addView(mPreviewView);
                bindPreview(cameraProvider);
            } else {
                cameraProvider.unbindAll();
                previewViewContainer.removeView(mPreviewView);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void setUpToggleCamera(@NonNull final ProcessCameraProvider cameraProvider,
            @NonNull final View rootView) {
        final Button toggleCameraButton = rootView.findViewById(R.id.toggle_camera);
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    toggleCameraButton.setEnabled(true);
                }
            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                throw new IllegalStateException("Front and back cameras are unavailable.");
            }
            toggleCameraButton.setOnClickListener(view -> switchCamera(cameraProvider));
        } catch (CameraInfoUnavailableException exception) {
            toggleCameraButton.setEnabled(false);
        }
    }

    @SuppressWarnings("WeakerAccess")
    void setUpScaleTypeSelect(@NonNull final View rootView) {
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        final Preview preview = new Preview.Builder()
                .setTargetName("Preview")
                .build();
        cameraProvider.bindToLifecycle(this, getCurrentCameraSelector(), preview);
        mPreviewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW);
        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
    }

    @SuppressWarnings("WeakerAccess")
    void switchCamera(@NonNull final ProcessCameraProvider cameraProvider) {
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
    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = () -> {
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
    // end region
}
