/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.previewview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.previewview.surface.PreviewSurfaceRequest;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Custom View that displays the camera feed for Camera2 and CameraX.
 *
 * <p> This class manages the preview {@link Surface}'s lifecycle. It internally uses either a
 * {@link TextureView} or {@link SurfaceView} to display the camera feed, and applies required
 * transformations on them to correctly display the preview, this involves correcting their
 * aspect ratio, scale and rotation.
 */
@RequiresApi(21)
public final class CameraPreviewView extends FrameLayout {

    private static final String TAG = "PreviewView";

    @ColorRes
    static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.PERFORMANCE;

    @NonNull
    ImplementationMode mImplementationMode = DEFAULT_IMPL_MODE;

    @Nullable
    PreviewViewImplementation mImplementation;

    @NonNull
    final PreviewTransformation mPreviewTransform = new PreviewTransformation();

    @UiThread
    public CameraPreviewView(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public CameraPreviewView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public CameraPreviewView(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public CameraPreviewView(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.PreviewView, defStyleAttr, defStyleRes);
        ViewCompat.saveAttributeDataForStyleable(this, context, R.styleable.PreviewView, attrs,
                attributes, defStyleAttr, defStyleRes);

        try {
            final int scaleTypeId = attributes.getInteger(
                    R.styleable.PreviewView_scaleType,
                    mPreviewTransform.getScaleType().getId());
            setScaleType(ScaleType.fromId(scaleTypeId));

            int implementationModeId =
                    attributes.getInteger(R.styleable.PreviewView_implementationMode,
                            DEFAULT_IMPL_MODE.getId());
            setImplementationMode(ImplementationMode.fromId(implementationModeId));
        } finally {
            attributes.recycle();
        }

        // Set background only if it wasn't already set. A default background prevents the content
        // behind the PreviewView from being visible before the preview starts streaming.
        if (getBackground() == null) {
            setBackgroundColor(ContextCompat.getColor(getContext(), DEFAULT_BACKGROUND_COLOR));
        }
    }

    /**
     * Sets the {@link ImplementationMode} for the {@link CameraPreviewView}.
     *
     * <p> {@link CameraPreviewView} displays the preview with a {@link TextureView} when the
     * mode is {@link ImplementationMode#COMPATIBLE}, and tries to use a {@link SurfaceView} if
     * it is {@link ImplementationMode#PERFORMANCE} when possible, which depends on the device's
     * attributes (e.g. API level). If not set, the default mode is
     * {@link ImplementationMode#PERFORMANCE}.
     */
    @UiThread
    public void setImplementationMode(@NonNull final ImplementationMode implementationMode) {
        mImplementationMode = implementationMode;
    }

    /**
     * Returns the {@link ImplementationMode}.
     *
     * <p> If nothing is set via {@link #setImplementationMode}, the default
     * value is {@link ImplementationMode#PERFORMANCE}.
     *
     * @return The {@link ImplementationMode} for {@link CameraPreviewView}.
     */
    @UiThread
    @NonNull
    public ImplementationMode getImplementationMode() {
        return mImplementationMode;
    }

    /**
     * Applies a {@link ScaleType} to the preview.
     *
     * <p> This value can also be set in the layout XML file via the {@code app:scaleType}
     * attribute.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @param scaleType A {@link ScaleType} to apply to the preview.
     * @attr name app:scaleType
     */
    @UiThread
    public void setScaleType(@NonNull final ScaleType scaleType) {
        mPreviewTransform.setScaleType(scaleType);
        redrawPreview();
    }

    /**
     * Returns the {@link ScaleType} currently applied to the preview.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @return The {@link ScaleType} currently applied to the preview.
     */
    @UiThread
    @NonNull
    public ScaleType getScaleType() {
        return mPreviewTransform.getScaleType();
    }

    /**
     * Request surface asynchronously.
     *
     * @param surfaceRequest surface request.
     * @return surface.
     */
    @UiThread
    @NonNull
    public ListenableFuture<Surface> requestSurfaceAsync(
            @NonNull PreviewSurfaceRequest surfaceRequest) {
        // TODO: API implementation
        return surfaceRequest.getPreviewSurface().getSurface();
    }

    private void redrawPreview() {
        if (mImplementation != null) {
            mImplementation.redrawPreview();
        }
    }

    /**
     * The implementation mode of a {@link CameraPreviewView}.
     *
     * <p> User preference on how the {@link CameraPreviewView} should render the preview.
     * {@link CameraPreviewView} displays the preview with either a {@link SurfaceView} or a
     * {@link TextureView}. A {@link SurfaceView} is generally better than a {@link TextureView}
     * when it comes to certain key metrics, including power and latency. On the other hand,
     * {@link TextureView} is better supported by a wider range of devices. The option is used by
     * {@link CameraPreviewView} to decide what is the best internal implementation given the device
     * capabilities and user configurations.
     */
    @RequiresApi(21)
    public enum ImplementationMode {

        /**
         * Use a {@link SurfaceView} for the preview when possible. If the device
         * doesn't support {@link SurfaceView}, {@link CameraPreviewView} will fall back to use a
         * {@link TextureView} instead.
         *
         * <p>{@link CameraPreviewView} falls back to {@link TextureView} when the API level is 24.
         */
        PERFORMANCE(0),

        /**
         * Use a {@link TextureView} for the preview.
         */
        COMPATIBLE(1);

        private final int mId;

        ImplementationMode(int id) {
            mId = id;
        }

        int getId() {
            return mId;
        }

        static ImplementationMode fromId(int id) {
            for (ImplementationMode implementationMode : values()) {
                if (implementationMode.mId == id) {
                    return implementationMode;
                }
            }
            throw new IllegalArgumentException("Unknown implementation mode id " + id);
        }
    }

    /** Options for scaling the preview vis-à-vis its container {@link CameraPreviewView}. */
    @RequiresApi(21)
    public enum ScaleType {
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraPreviewView}, and align it to the start of the view, which is the top left
         * corner in a left-to-right (LTR) layout, or the top right corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link CameraPreviewView}.
         */
        FILL_START(0),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraPreviewView}, and center it in the view.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link CameraPreviewView}.
         */
        FILL_CENTER(1),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it fills the entire
         * {@link CameraPreviewView}, and align it to the end of the view, which is the bottom right
         * corner in a left-to-right (LTR) layout, or the bottom left corner in a right-to-left
         * (RTL) layout.
         * <p>
         * This may cause the preview to be cropped if the camera preview aspect ratio does not
         * match that of its container {@link CameraPreviewView}.
         */
        FILL_END(2),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraPreviewView}, and align it to the start of the view, which is the
         * top left corner in a left-to-right (LTR) layout, or the top right corner in a
         * right-to-left (RTL) layout. The background area not covered by the preview stream
         * will be black or the background of the {@link CameraPreviewView}
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link CameraPreviewView}.
         */
        FIT_START(3),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraPreviewView}, and center it inside the view. The background
         * area not covered by the preview stream will be black or the background of the
         * {@link CameraPreviewView}.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link CameraPreviewView}.
         */
        FIT_CENTER(4),
        /**
         * Scale the preview, maintaining the source aspect ratio, so it is entirely contained
         * within the {@link CameraPreviewView}, and align it to the end of the view, which is the
         * bottom right corner in a left-to-right (LTR) layout, or the bottom left corner in a
         * right-to-left (RTL) layout. The background area not covered by the preview stream
         * will be black or the background of the {@link CameraPreviewView}.
         * <p>
         * Both dimensions of the preview will be equal or less than the corresponding dimensions
         * of its container {@link CameraPreviewView}.
         */
        FIT_END(5);

        private final int mId;

        ScaleType(int id) {
            mId = id;
        }

        int getId() {
            return mId;
        }

        static ScaleType fromId(int id) {
            for (ScaleType scaleType : values()) {
                if (scaleType.mId == id) {
                    return scaleType;
                }
            }
            throw new IllegalArgumentException("Unknown scale type id " + id);
        }
    }
}
