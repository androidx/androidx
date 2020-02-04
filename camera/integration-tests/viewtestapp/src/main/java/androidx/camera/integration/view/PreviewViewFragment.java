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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

/**
 * A Fragment that displays a PreviewView.
 */
public class PreviewViewFragment extends Fragment {
    private static final String TAG = "PreviewViewFragment";
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            PreviewView mPreviewView;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private int mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPreviewView = view.findViewById(R.id.preview_view);

        ViewGroup previewViewContainer = view.findViewById(R.id.container);

        final Button toggleVisibilityButton = view.findViewById(R.id.toggle_visibility);
        final Button toggleCameraButton = view.findViewById(R.id.toggle_camera);

        Futures.addCallback(mCameraProviderFuture, new FutureCallback<ProcessCameraProvider>() {
            @Override
            public void onSuccess(
                    @Nullable ProcessCameraProvider cameraProvider) {
                Preconditions.checkNotNull(cameraProvider);
                toggleVisibilityButton.setEnabled(true);
                toggleVisibilityButton.setOnClickListener(
                        view1 -> {
                            // Toggle the existence of the PreviewView.
                            if (previewViewContainer.getChildCount() == 0) {
                                previewViewContainer.addView(mPreviewView);
                                bindPreview(cameraProvider);
                            } else {
                                cameraProvider.unbindAll();
                                previewViewContainer.removeView(mPreviewView);
                            }
                        });

                try {
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            toggleCameraButton.setEnabled(true);
                        }
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
                    } else { // no cameras at all
                        return;
                    }

                    toggleCameraButton.setOnClickListener(v -> switchCamera(cameraProvider));
                } catch (CameraInfoUnavailableException e) {
                    toggleCameraButton.setEnabled(false);
                }

                bindPreview(cameraProvider);
            }

            @Override
            public void onFailure(@NonNull Throwable throwable) {
                Log.e(TAG, "Failed to retrieve camera provider from CameraX. Is CameraX "
                        + "initialized?", throwable);
            }
        }, ContextCompat.getMainExecutor(Preconditions.checkNotNull(getContext())));
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetName("Preview")
                .build();

        preview.setSurfaceProvider(mPreviewView.getPreviewSurfaceProvider());

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(mCurrentLensFacing).build();
        cameraProvider.bindToLifecycle(PreviewViewFragment.this, cameraSelector, preview);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview_view, container, false);
    }

    private void switchCamera(@NonNull final ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        if (mCurrentLensFacing == CameraSelector.LENS_FACING_BACK) {
            mCurrentLensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            mCurrentLensFacing = CameraSelector.LENS_FACING_BACK;
        }
        bindPreview(cameraProvider);
    }
}
