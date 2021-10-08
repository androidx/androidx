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

import static java.lang.Math.abs;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.FileTransformFactory;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A fragment that demos transform utilities.
 */
@SuppressLint("RestrictedAPI")
@OptIn(markerClass = TransformExperimental.class)
public final class TransformFragment extends Fragment {

    private static final String TAG = "TransformFragment";

    private static final int TILE_COUNT = 4;
    public static final RectF NORMALIZED_RECT = new RectF(-1, -1, 1, 1);

    private LifecycleCameraController mCameraController;
    private ExecutorService mExecutorService;
    private ToggleButton mMirror;
    private ToggleButton mCameraToggle;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    PreviewView mPreviewView;
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    OverlayView mOverlayView;

    // The following two variables should only be accessed from mExecutorService.
    // Synthetic access
    @Nullable
    @SuppressWarnings("WeakerAccess")
    OutputTransform mImageProxyTransform;
    // Synthetic access
    @Nullable
    @SuppressWarnings("WeakerAccess")
    RectF mBrightestTile;

    private FileTransformFactory mFileTransformFactoryWithoutExif;
    private FileTransformFactory mFileTransformFactoryWithExif;

    private final ImageAnalysis.Analyzer mAnalyzer = new ImageAnalysis.Analyzer() {

        private final ImageProxyTransformFactory mImageProxyTransformFactory =
                new ImageProxyTransformFactory();

        @Override
        @OptIn(markerClass = TransformExperimental.class)
        @SuppressWarnings("RestrictedApi")
        public void analyze(@NonNull ImageProxy imageProxy) {
            // Find the brightest tile to highlight.
            mBrightestTile = findBrightestTile(imageProxy);
            mImageProxyTransform =
                    mImageProxyTransformFactory.getOutputTransform(imageProxy);
            imageProxy.close();

            // Take a snapshot of the analyze result for thread safety.
            final RectF brightestTile = new RectF(mBrightestTile);
            final OutputTransform imageProxyTransform = mImageProxyTransform;

            // Calculate PreviewView transform on UI thread.
            mOverlayView.post(() -> {
                // Calculate the transform.
                RectF brightestTileInPreviewView = getBrightestTileInPreviewView(
                        imageProxyTransform, brightestTile);
                if (brightestTileInPreviewView == null) {
                    // PreviewView transform info is not ready. No-op.
                    return;
                }

                // Draw the tile on top of PreviewView.
                mOverlayView.setTileRect(brightestTileInPreviewView);
                mOverlayView.postInvalidate();
            });
        }
    };

    /**
     * Gets the transform matrix based on exif orientation.
     *
     * <p> A forked version of {@code TransformUtil#getExifTransform} to make the test app
     * self-contained.
     */
    @NonNull
    public static Matrix getExifTransform(int exifOrientation, int width, int height) {
        Matrix matrix = new Matrix();

        // Map the bitmap to a normalized space (-1, -1) - (1, 1) and perform transform in the
        // normalized space.
        RectF rect = new RectF(0, 0, width, height);
        matrix.setRectToRect(rect, NORMALIZED_RECT, Matrix.ScaleToFit.FILL);

        // A flag that check if the image has been rotated 90/270.
        boolean isWidthHeightSwapped = false;

        // Transform the normalized space based on exif orientation.
        switch (exifOrientation) {
            case android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1f, 1f);
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1f, -1f);
                break;
            case android.media.ExifInterface.ORIENTATION_TRANSPOSE:
                // Flipped about top-left <--> bottom-right axis, it can also be represented by
                // flip horizontally and then rotate 270 degree clockwise.
                matrix.postScale(-1f, 1f);
                matrix.postRotate(270);
                isWidthHeightSwapped = true;
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                isWidthHeightSwapped = true;
                break;
            case android.media.ExifInterface.ORIENTATION_TRANSVERSE:
                // Flipped about top-right <--> bottom-left axis, it can also be
                // represented by flip horizontally and then rotate 90 degree clockwise.
                matrix.postScale(-1f, 1f);
                matrix.postRotate(90);
                isWidthHeightSwapped = true;
                break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                isWidthHeightSwapped = true;
                break;
            case android.media.ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case android.media.ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                break;
        }

        // Map the normalized space back to the bitmap coordinates.
        RectF restoredRect = isWidthHeightSwapped ? new RectF(0, 0, height, width) : rect;
        Matrix restore = new Matrix();
        restore.setRectToRect(NORMALIZED_RECT, restoredRect, Matrix.ScaleToFit.FILL);
        matrix.postConcat(restore);
        return matrix;
    }

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
        int tileX;
        int tileY;
        for (int x = 0; x < cropRect.width(); x++) {
            for (int y = 0; y < cropRect.height(); y++) {
                tileX = Math.min(x / tileWidth, TILE_COUNT - 1);
                tileY = Math.min(y / tileHeight, TILE_COUNT - 1);
                tiles[tileX][tileY] +=
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
    @OptIn(markerClass = TransformExperimental.class)
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mFileTransformFactoryWithoutExif = new FileTransformFactory();
        mFileTransformFactoryWithExif = new FileTransformFactory();
        mFileTransformFactoryWithExif.setUsingExifOrientation(true);
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

        mCameraToggle = view.findViewById(R.id.toggle_camera);
        mCameraToggle.setOnCheckedChangeListener(
                (buttonView, isChecked) -> updateCameraOrientation());

        view.findViewById(R.id.capture).setOnClickListener(
                v -> saveHighlightedFilePreservingExif());

        view.findViewById(R.id.capture_and_transform).setOnClickListener(
                v -> saveHighlightedUriWithoutExif());

        updateMirrorState();
        updateCameraOrientation();
        return view;
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void showToast(String message) {
        requireActivity().runOnUiThread(
                () -> Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show());
    }

    private void createDefaultPictureFolderIfNotExist() {
        File pictureFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!pictureFolder.exists()) {
            if (!pictureFolder.mkdir()) {
                Log.e(TAG, "Failed to create directory: " + pictureFolder);
            }
        }
    }

    /**
     * Takes a picture, applies the exif info, highlights the brightest tile and saves it to
     * MediaStore without exif info.
     */
    private void saveHighlightedUriWithoutExif() {
        // Take a picture and save to MediaStore
        createDefaultPictureFolderIfNotExist();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();

        mCameraController.takePicture(outputFileOptions, mExecutorService,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        if (mImageProxyTransform == null || mBrightestTile == null) {
                            Logger.d(TAG, "ImageAnalysis result not ready.");
                            return;
                        }
                        Uri uri = outputFileResults.getSavedUri();
                        if (uri == null) {
                            showToast("Saved URI should not be null.");
                            return;
                        }
                        try {
                            RectF tileInUri = getBrightestTileInUriWithExif(
                                    uri,
                                    mImageProxyTransform,
                                    mBrightestTile);
                            Bitmap bitmap = loadBitmapWithExifApplied(uri);
                            drawRectOnBitmap(bitmap, tileInUri);
                            saveBitmapToUri(bitmap, uri);
                            showToast("Image saved.");
                        } catch (IOException e) {
                            showToast("Failed to draw on file. " + e);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        showToast("Failed to capture image. " + exception);
                    }
                });
    }

    /**
     * Loads {@link Bitmap} from the given {@link Uri} and applies exif info to the {@link Bitmap}.
     */
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @NonNull
    Bitmap loadBitmapWithExifApplied(Uri uri) throws IOException {
        // Loads bitmap.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap original;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            original = BitmapFactory.decodeStream(inputStream, /*outPadding*/ null, options);
        }

        // Reads exif orientation.
        int exifOrientation;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            exifOrientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        }
        Matrix matrix = getExifTransform(exifOrientation, original.getWidth(),
                original.getHeight());

        // Calculate the bitmap size with exif applied.
        float[] sizeVector = new float[]{original.getWidth(), original.getHeight()};
        matrix.mapVectors(sizeVector);

        // Create a new bitmap with exif applied.
        Bitmap bitmapWithExif = Bitmap.createBitmap(
                round(abs(sizeVector[0])),
                round(abs(sizeVector[1])),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapWithExif);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.drawBitmap(original, matrix, paint);
        return bitmapWithExif;
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void saveBitmapToUri(@NonNull Bitmap bitmap, @NonNull Uri uri) throws IOException {
        try (OutputStream outputStream =
                     requireContext().getContentResolver().openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        }
    }

    /**
     * Takes a picture, highlights the brightest tile and saves it to MediaStore preserving Exif
     * info.
     */
    private void saveHighlightedFilePreservingExif() {
        // Create an internal temp file for drawing an overlay.
        File tempFile;
        try {
            tempFile = File.createTempFile("camerax-view-test_transform-test", ".jpg");
            tempFile.deleteOnExit();
        } catch (IOException e) {
            showToast("Failed to create temp file. " + e);
            return;
        }

        // Take a picture.
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(tempFile).build();
        mCameraController.takePicture(outputFileOptions, mExecutorService,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        if (mImageProxyTransform == null || mBrightestTile == null) {
                            Logger.d(TAG, "ImageAnalysis result not ready.");
                            return;
                        }
                        try {
                            RectF tileInFile = getBrightestTileInFileWithoutExif(
                                    tempFile,
                                    mImageProxyTransform,
                                    mBrightestTile);
                            // Load a mutable Bitmap.
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inMutable = true;
                            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath(),
                                    options);
                            drawRectOnBitmap(bitmap, tileInFile);
                            saveBitmapToFilePreservingExif(tempFile, bitmap);
                            insertFileToMediaStore(tempFile);
                            showToast("Image saved.");
                        } catch (IOException e) {
                            showToast("Failed to draw on file. " + e);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        showToast("Failed to capture image. " + exception);
                    }
                });
    }

    private void updateMirrorState() {
        if (mMirror.isChecked()) {
            mPreviewView.setScaleX(-1);
        } else {
            mPreviewView.setScaleX(1);
        }
    }

    private void updateCameraOrientation() {
        if (mCameraToggle.isChecked()) {
            mCameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        } else {
            mCameraController.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA);
        }
    }

    /**
     * Saves the Bitmap to the given File while preserving the File's exif orientation.
     */
    void saveBitmapToFilePreservingExif(@NonNull File originalFile, @NonNull Bitmap bitmap)
            throws IOException {
        ExifInterface exifInterface = new ExifInterface(originalFile);
        int orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        try (OutputStream outputStream = new FileOutputStream(originalFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        }
        exifInterface = new ExifInterface(originalFile);
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientation));
        exifInterface.saveAttributes();
    }

    void insertFileToMediaStore(@NonNull File file) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
        try (OutputStream outputStream = contentResolver.openOutputStream(uri)) {
            try (InputStream inputStream = new FileInputStream(file)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            }
        }
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void drawRectOnBitmap(@NonNull Bitmap bitmap, @NonNull RectF rectF) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);

        Canvas canvas = new Canvas(bitmap);

        // Draw a rect with black stroke and white glow so it's always visible regardless of
        // background.
        paint.setStrokeWidth(20);
        paint.setColor(Color.WHITE);
        canvas.drawRect(rectF, paint);

        paint.setStrokeWidth(10);
        paint.setColor(Color.BLACK);
        canvas.drawRect(rectF, paint);
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @OptIn(markerClass = TransformExperimental.class)
    @NonNull
    RectF getBrightestTileInFileWithoutExif(@NonNull File file,
            @NonNull OutputTransform imageProxyTransform,
            @NonNull RectF imageProxyTile) throws IOException {
        return getBrightestTile(
                imageProxyTransform,
                mFileTransformFactoryWithoutExif.getOutputTransform(file),
                imageProxyTile);
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @OptIn(markerClass = TransformExperimental.class)
    @NonNull
    RectF getBrightestTileInUriWithExif(@NonNull Uri uri,
            @NonNull OutputTransform imageProxyTransform,
            @NonNull RectF imageProxyTile) throws IOException {
        OutputTransform uriTransform = mFileTransformFactoryWithExif.getOutputTransform(
                requireContext().getContentResolver(), uri);
        return getBrightestTile(imageProxyTransform, uriTransform, imageProxyTile);
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @OptIn(markerClass = TransformExperimental.class)
    @MainThread
    @Nullable
    RectF getBrightestTileInPreviewView(@NonNull OutputTransform imageProxyTransform,
            @NonNull RectF imageProxyTile) {
        OutputTransform previewViewTransform = mPreviewView.getOutputTransform();
        if (previewViewTransform == null) {
            // PreviewView transform info is not ready. No-op.
            return null;
        }
        return getBrightestTile(imageProxyTransform, previewViewTransform, imageProxyTile);
    }

    @OptIn(markerClass = TransformExperimental.class)
    @NonNull
    private RectF getBrightestTile(@NonNull OutputTransform source,
            @NonNull OutputTransform target,
            @NonNull RectF imageProxyTile) {
        CoordinateTransform transform = new CoordinateTransform(source, target);
        Matrix matrix = new Matrix();
        transform.transform(matrix);
        matrix.mapRect(imageProxyTile);
        return imageProxyTile;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
    }
}
