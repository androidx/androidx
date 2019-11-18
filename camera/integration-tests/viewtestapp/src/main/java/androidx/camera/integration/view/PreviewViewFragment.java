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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.LifecycleCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;

/**
 * A Fragment that displays a PreviewView.
 */
public class PreviewViewFragment extends Fragment {

    private PreviewView mPreviewView;
    private FrameLayout mPreviewViewContainer;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPreviewView = view.findViewById(R.id.preview_view);

        mPreviewViewContainer = view.findViewById(R.id.container);

        view.findViewById(R.id.toggle_visibility).setOnClickListener(
                view1 -> {
                    // Toggle the existence of the PreviewView.
                    if (mPreviewViewContainer.getChildCount() == 0) {
                        mPreviewViewContainer.addView(mPreviewView);
                        bindPreview();
                    } else {
                        LifecycleCameraProvider.unbindAll();
                        mPreviewViewContainer.removeView(mPreviewView);
                    }
                });

        bindPreview();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

    }

    private void bindPreview() {
        Preview preview = new Preview.Builder()
                .setTargetName("Preview")
                .build();
        preview.setPreviewSurfaceCallback(mPreviewView.getPreviewSurfaceCallback());

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(LensFacing.BACK).build();
        LifecycleCameraProvider.bindToLifecycle(PreviewViewFragment.this, cameraSelector, preview);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview_view, container, false);
    }
}
