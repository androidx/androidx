/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A fragment that demos transform utilities.
 */
public final class TransformFragment extends Fragment {

    private static final int TILE_COUNT = 4;

    private LifecycleCameraController mCameraController;
    private ExecutorService mExecutorService;
    private ToggleButton mMirror;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    PreviewView mPreviewView;
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    OverlayView mOverlayView;

    private final ImageAnalysis.Analyzer mAnalyzer = new ImageAnalysis.Analyzer() {

        private final ImageProxyTransformFactory mImageProxyTransformFactory =
                new ImageProxyTransformFactory.Builder().build();

        @Override
        @UseExperimental(markerClass = TransformExperimental.class)
        @SuppressWarnings("RestrictedApi")
        public void analyze(@NonNull ImageProxy imageProxy) {
            // Find the tile to highlight.
            RectF brightestTile = findBrightestTile(imageProxy);

            // Calculate PreviewView transform on UI thread.
            mOverlayView.post(() -> {
                // Calculate the transform.
                try (ImageProxy imageToClose = imageProxy)  {
                    OutputTransform previewViewTransform = mPreviewView.getOutputTransform();
                    if (previewViewTransform == null) {
                        // PreviewView transform info is not ready. No-op.
                        return;
                    }
                    CoordinateTransform transform = new CoordinateTransform(
                            mImageProxyTransformFactory.getOutputTransform(imageToClose),
                            previewViewTransform);
                    Matrix analysisToPreview = new Matrix();
                    transform.getTransform(analysisToPreview);

                    // Map the tile to PreviewView coordinates.
                    analysisToPreview.mapRect(brightestTile);
                    // Draw the tile on top of PreviewView.
                    mOverlayView.setTileRect(brightestTile);
                    mOverlayView.postInvalidate();
                }
            });
        }
    };

    /**
     * Finds the brightest tile in the given {@link ImageProxy}.
     *
     * <p> Divides the crop rect of the image into a 4x4 grid, and find the brightest tile
     * among the 16 tiles.
     *
     * @return the box of the brightest tile.
     */
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    RectF findBrightestTile(ImageProxy image) {
        // Divide the crop rect in to 4x4 tiles.
        Rect cropRect = image.getCropRect();
        int[][] tiles = new int[TILE_COUNT][TILE_COUNT];
        int tileWidth = cropRect.width() / TILE_COUNT;
        int tileHeight = cropRect.height() / TILE_COUNT;

        // Loop through the y plane and get the sum of the luminance for each tile.
        byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
        image.getPlanes()[0].getBuffer().get(bytes);
        for (int x = 0; x < cropRect.width(); x++) {
            for (int y = 0; y < cropRect.height(); y++) {
                tiles[x / tileWidth][y / tileHeight] +=
                        bytes[(y + cropRect.top) * image.getWidth() + cropRect.left + x] & 0xFF;
            }
        }

        // Find the brightest tile among the 16 tiles.
        float maxLuminance = 0;
        int brightestTileX = 0;
        int brightestTileY = 0;
        for (int i = 0; i < TILE_COUNT; i++) {
            for (int j = 0; j < TILE_COUNT; j++) {
                if (tiles[i][j] > maxLuminance) {
                    maxLuminance = tiles[i][j];
                    brightestTileX = i;
                    brightestTileY = j;
                }
            }
        }

        // Return the rectangle of the tile.
        return new RectF(brightestTileX * tileWidth + cropRect.left,
                brightestTileY * tileHeight + cropRect.top,
                (brightestTileX + 1) * tileWidth + cropRect.left,
                (brightestTileY + 1) * tileHeight + cropRect.top);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mExecutorService = Executors.newSingleThreadExecutor();
        mCameraController = new LifecycleCameraController(requireContext());
        mCameraController.bindToLifecycle(getViewLifecycleOwner());

        View view = inflater.inflate(R.layout.transform_view, container, false);

        mPreviewView = view.findViewById(R.id.preview_view);
        // Set to compatible so the custom transform (e.g. mirroring) would work.
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mPreviewView.setController(mCameraController);

        mCameraController.setImageAnalysisAnalyzer(mExecutorService, mAnalyzer);

        mOverlayView = view.findViewById(R.id.overlay_view);
        mMirror = view.findViewById(R.id.mirror_preview);
        mMirror.setOnCheckedChangeListener((buttonView, isChecked) -> updateMirrorState());

        updateMirrorState();
        return view;
    }

    private void updateMirrorState() {
        if (mMirror.isChecked()) {
            mPreviewView.setScaleX(-1);
        } else {
            mPreviewView.setScaleX(1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
    }
}
